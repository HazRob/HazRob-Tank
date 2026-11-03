package com.servoc.app.chemcore.nextintegration;

import android.content.Context;
import android.database.Cursor;
import com.hazrob.tank.chemcore.next.*;
import com.servoc.app.chemical.ChemicalRepository;
import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.hardware.HazRobHardwareProfile;
import java.util.*;
import org.json.*;

/**
 * V8 P09.2/P09.3: field gas capture comes from persisted REAL 60-second
 * telemetry rows. A terminal /diag snapshot alone NEVER substitutes for an
 * original field archive. This bridge produces a source-provenance median
 * fingerprint, not a certified source-matched gas identity or ppm.
 */
public final class NextFieldArchiveBridge {
  private NextFieldArchiveBridge(){}
  public static final String SCHEMA="hazrob.tank.v8.field.next_archive_adapter/1";

  public static final class Outcome {
    public final NextFieldArchive.Summary source;
    public final NextEngineRouter.Outcome decision;
    private Outcome(NextFieldArchive.Summary raw,NextEngineRouter.Outcome outcome){
      source=raw;decision=outcome;
    }
  }
  private static boolean included(String model){
    return NextFieldArchiveModelKeys.isPhysicalInstalled(model);
  }
  private static long age(JSONObject x){
    return x.has("last_ok_age_ms")?x.optLong("last_ok_age_ms",-1L):-1L;
  }
  private static Map<String,NextFieldArchive.Scan> parseScans(String mqRaw){
    Map<String,NextFieldArchive.Scan> sensors=new LinkedHashMap<>();
    try{
      JSONObject mq=new JSONObject(mqRaw);
      for(HazRobHardwareProfile.Channel ch:HazRobHardwareProfile.MQ){
        if(!included(ch.key))continue;
        JSONObject src=mq.optJSONObject(ch.key);
        if(src==null||!src.optBoolean("ok",src.optBoolean("valid",false))||
           !src.optBoolean("powered",false)||!src.has("scan_cycle"))continue;
        long cycle=src.optLong("scan_cycle",-1);
        double rs=src.optDouble("rs",Double.NaN);
        long freshness=age(src);
        if(cycle<0||!Double.isFinite(rs)||rs<=0||(freshness>=0&&freshness>6000L))
          continue;
        sensors.put(ch.key,new NextFieldArchive.Scan(cycle,rs,freshness));
      }
    }catch(JSONException corrupt){
      throw new IllegalStateException("FIELD_ARCHIVE_CORRUPT_MQ_JSON",corrupt);
    }
    return sensors;
  }
  /**
   * Session ownership is read from the immutable Cycle row. Gas=false
   * profiles never fabricate a gas decision.
   */
  public static boolean requiresGasDecision(ExperimentsDb db,String measurementId){
    if(db==null||measurementId==null||measurementId.isBlank())
      throw new IllegalArgumentException("original field session required");
    try(Cursor c=db.getReadableDatabase().rawQuery(
       "SELECT include_gas FROM field_measurement_cycles WHERE measurement_session_id=? "+
       "AND state='SAMPLING' ORDER BY sequence_no DESC LIMIT 1",
       new String[]{measurementId})){
      if(!c.moveToFirst())throw new IllegalStateException("FIELD_CYCLE_NOT_LINKED_OR_NOT_SAMPLING");
      return c.getInt(0)!=0;
    }
  }
  public static NextFieldArchive.Summary fromStoredSamples(
      ExperimentsDb db,String measurementId) {
    if(db==null||measurementId==null||measurementId.isBlank())
      throw new IllegalArgumentException("recorded measurement required");
    List<NextFieldArchive.Row> originals=new ArrayList<>();
    try(Cursor c=db.getReadableDatabase().rawQuery(
       "SELECT t_ms,env_payload,mq_payload,motion_payload "+
       "FROM telemetry_samples WHERE session_id=? ORDER BY t_ms,id",
       new String[]{measurementId})){
      while(c.moveToNext()){
        if(originals.size()>=1200)
          throw new IllegalStateException("FIELD_ARCHIVE_EXCESS_ROWS_AUDIT_REQUIRED");
        String env=c.isNull(1)?"":c.getString(1);
        String mq=c.isNull(2)?"":c.getString(2);
        String motion=c.isNull(3)?"":c.getString(3);
        if(mq.isEmpty()||motion.isEmpty()||env.isEmpty())
          throw new IllegalStateException("FIELD_ARCHIVE_INCOMPLETE_ROW");
        originals.add(new NextFieldArchive.Row(c.getLong(0),env,mq,motion,parseScans(mq)));
      }
    }
    return NextFieldArchive.summarize(originals);
  }
  /** Do not overwrite hardware identity, original relay or firmware status. */
  private static JSONObject aggregateSource(JSONObject terminal,
      NextFieldArchive.Summary archive)throws JSONException {
    if(terminal==null||terminal.optJSONObject("mq")==null)
      throw new IllegalStateException("FIELD_TERMINAL_MQ_MISSING");
    JSONObject safe=new JSONObject(terminal.toString());
    if(safe.optString("device_id","").isBlank())
      throw new IllegalStateException("FIELD_TERMINAL_DEVICE_ID_MISSING");
    if(!NextAndroidAdapter.relayConfirmedOn(safe))
      throw new IllegalStateException("FIELD_RELAY_NOT_ON_OR_INCONSISTENT_AT_CAPTURE_SEAL");
    JSONObject values=new JSONObject();
    for(var ch:archive.qualifiedChannels.values()){
      JSONObject v=new JSONObject();
      v.put("rs",ch.medianRsKohm);
      v.put("ok",true);v.put("powered",true);
      v.put("last_ok_age_ms",ch.lastAgeMs);
      v.put("status","ARCHIVE_SOURCE_MEDIAN_NO_G3");
      v.put("source_distinct_scan_cycles",ch.distinctCycles);
      // Deliberately NO invented raw ADC, no source manufacturer R0 or ppm.
      values.put(ch.model,v);
    }
    safe.put("mq",values);
    JSONObject meta=new JSONObject();
    meta.put("schema",SCHEMA);
    meta.put("source_archive_sha256",archive.sourceSha256);
    meta.put("source_rows",archive.rawRows);
    meta.put("source_coverage_ms",archive.coverageMs);
    meta.put("distinct_scan_cycles",archive.distinctGlobalGasScanCycles);
    meta.put("source_status",archive.status);
    meta.put("feature_method","MEDIAN_DISTINCT_SOURCE_SCAN_CYCLES_NOT_SINGLE_TERMINAL");
    meta.put("raw_source_persisted_in_telemetry_samples",true);
    meta.put("source_reference_ppm_available",false);
    safe.put("_next_field_archive",meta);
    return safe;
  }
  /** Call only after actual 60 second capture has terminal READY persisted. */
  public static Outcome analyzeAndPersist(Context app,ExperimentsDb db,String measurementId,
      JSONObject terminal,long recordedElapsedMs,long captureEndedUtcMs)throws Exception{
    if(app==null||db==null||measurementId==null||measurementId.isBlank())
      throw new IllegalArgumentException("real Android field context required");
    if(recordedElapsedMs<60000L)
      throw new IllegalStateException("FIELD_MEASUREMENT_DURATION_BELOW_60S");
    NextFieldArchive.Summary source=fromStoredSamples(db,measurementId);
    if(!source.hasMeasuredEvidence())
      throw new IllegalStateException("FIELD_ARCHIVE_SOURCE_INVALID:"+source.status+
        " scan_cycles="+source.distinctGlobalGasScanCycles+
        " coverage_ms="+source.coverageMs);
    JSONObject combined=aggregateSource(terminal,source);
    List<ResearchCandidateGenerator.Material> catalog=new ArrayList<>();
    ChemicalRepository master=new ChemicalRepository(db);
    for(ChemicalRepository.Chemical item:master.all(5000)){
      String cls=item.chemicalClass==null?"":item.chemicalClass.toLowerCase(Locale.ROOT);
      boolean generic=cls.contains("mixture")||cls.contains("generic")||cls.contains("general");
      catalog.add(new ResearchCandidateGenerator.Material(item.uid,item.label(),generic?
          ResearchCandidateGenerator.MaterialKind.GENERAL_CLASS:
          ResearchCandidateGenerator.MaterialKind.MOLECULAR_CHEMICAL));
    }
    // Every archived scientific curve is evaluated only for provenance and
    // explicit abstention; no source R0 equivalence, G3 or candidate identity.
    var researchCurves=NextResearchSourceBindings.abstentionOnlyCurves(
        master,combined.optString("device_id","").trim());
    String captureUid="FIELD-ARCHIVE-"+source.sourceSha256;
    var next=NextEngineRouter.analyzeAndPersist(app,db,combined,measurementId,
         captureUid,captureEndedUtcMs,CoreContracts.Mode.FIELD_SESSION,catalog,
         researchCurves,List.of(),List.of(),12);
    if(next.receipt==null||next.receipt.originalDecision.physicallyValidatedG3)
      throw new IllegalStateException("NEXT_RESEARCH_ONLY_DECISION_RECEIPT_FAILED");
    if(!next.receipt.snapshotUid.equals(captureUid))
      throw new IllegalStateException("NEXT_SOURCE_ARCHIVE_HASH_MISMATCH");
    // Source-specific R0 is derived AFTER the original 60-second archive
    // is frozen. Persist this ledger separately from the immutable Next
    // decision: it is NOT evidence of detected chemical identity or ppm.
    // Preserve both independent denominators and the source archive digest
    // so later field exports can reproduce every conditional calculation.
    JSONObject dualReference=NextScientificDiagnostics.compute(
        combined,master,next.receipt.sourceSnapshot);
    JSONObject provenance=new JSONObject();
    provenance.put("schema","hazrob.tank.v8.field.next_dual_reference/1");
    provenance.put("measurement_session_id",measurementId);
    provenance.put("source_archive_sha256",source.sourceSha256);
    provenance.put("next_decision_uid",next.receipt.decisionUid);
    provenance.put("scientific_source_diagnostics",dualReference);
    provenance.put("research_only_no_gas_identity_or_safety_alarm",true);
    db.putMeta("v8_field_next_dual_reference_"+measurementId,provenance.toString());
    return new Outcome(source,next);
  }

  /** Only the known eight physical Tank MQ models, not source device extras. */
  static final class NextFieldArchiveModelKeys {
    static boolean isPhysicalInstalled(String s){
      return NextTelemetryMapper.CHANNELS.contains(s);
    }
  }
}
