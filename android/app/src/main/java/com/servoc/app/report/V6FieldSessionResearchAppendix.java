package com.servoc.app.report;

import android.database.Cursor;
import com.hazrob.tank.v6integration.TankV6TelemetryAdapter;
import com.hazrob.tank.chemcore.v6.ChemCoreTankV6;
import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.field.FieldCycleRepository;
import com.servoc.app.hardware.HazRobHardwareProfile;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * N06-B READ-ONLY V6 research appendix for verified APPROVED Field Sessions.
 *
 * Caller MUST first resolve FieldSessionReportExporter.Bundle, which verifies
 * every approved cycle evidence SHA. Never call on unapproved session.
 * The V5 legacy ChemCore decision + telemetry and approval SHA are untouched.
 *
 * For each archived physical sensor instance, select an ACTUALLY STORED
 * central unique scan-cycle observation, NOT a synthetic Rs/R0 median.
 * Feed each source channel separately to the independent V6 coordinator;
 * timestamps from different sensors are never fabricated as simultaneous.
 *
 * Source evidence insufficient? Return UNKNOWN and fail the research layer
 * closed, never run old gas inference in place of V6.
 */
public final class V6FieldSessionResearchAppendix {
    public static final String SCHEMA="hazrob-tank-v6-approved-field-readonly/1";
    private V6FieldSessionResearchAppendix(){}

    private static final class Sample {
        final long timestamp,scanCycle;
        final JSONObject observed,environment;
        Sample(long t,long scan,JSONObject q,JSONObject env) throws org.json.JSONException{
            timestamp=t;scanCycle=scan;
            observed=new JSONObject(q.toString());
            environment=env==null?new JSONObject():new JSONObject(env.toString());
        }
    }
    private static final class Accumulator {
        final LinkedHashMap<Long,Sample> distinct=new LinkedHashMap<>();
        int encountered=0,accepted=0;
    }
    private static String hex(byte[] data){
        StringBuilder b=new StringBuilder(data.length*2);
        for(byte v:data)b.append(String.format(Locale.ROOT,"%02x",v&255));
        return b.toString();
    }
    private static String sourceHash(String sessionId,List<String> orderedRecords)throws Exception{
        MessageDigest md=MessageDigest.getInstance("SHA-256");
        md.update(("HAZROB_TANK_V6_READONLY_SOURCE/1\n"+sessionId+"\n")
            .getBytes(StandardCharsets.UTF_8));
        for(String row:orderedRecords){
            byte[] b=row.getBytes(StandardCharsets.UTF_8);
            md.update((b.length+":").getBytes(StandardCharsets.UTF_8));
            md.update(b);
        }
        return hex(md.digest());
    }
    private static boolean eligible(JSONObject q){
        return q!=null&&q.optBoolean("ok",q.optBoolean("valid",false))
            &&q.optBoolean("powered",false)&&q.has("scan_cycle")
            &&q.optLong("scan_cycle",-1)>=0
            &&Double.isFinite(q.optDouble("rs",Double.NaN))
            &&q.optDouble("rs",Double.NaN)>0;
    }

    /** Must be invoked ONLY after the approved-cycle authoritative evidence
      * check in FieldSessionReportExporter.resolve has succeeded. */
    public static JSONObject analyzeVerified(ExperimentsDb db,
                        FieldSessionReportExporter.Bundle approved)throws Exception{
        if(db==null||approved==null||
            !approved.fieldSessionId.equals(approved.fieldSession.id)||
            !approved.fieldSession.approved())
            throw new IllegalArgumentException("Verified approved field bundle required");
        JSONObject root=new JSONObject();
        root.put("schema",SCHEMA);
        root.put("field_session_id",approved.fieldSessionId);
        root.put("source","READ_ONLY_VERIFIED_ARCHIVED_TELEMETRY_SAMPLES");
        root.put("source_database_table","telemetry_samples");
        root.put("no_db_write",true);
        root.put("v5_approval_and_decisions_unchanged",true);
        root.put("reported_identity","UNKNOWN");
        root.put("reportable_ppm",false);
        root.put("physical_tank_g3_validated",false);
        root.put("food_freshness_included",false);
        root.put("legacy_fallback_for_v6",false);
        root.put("method",
            "ONE_REAL_RECORDED_MEDIAN_INDEX_SCAN_PER_MQ_MODEL;NOT_SIMULTANEOUS;NO_FAKE_MEDIAN_RS_R0");
        JSONArray entries=new JSONArray();
        // Scan only approved gas-including acquisitions already verified by
        // the report resolver; never include failed, deleted or superseded cycles.
        int eligibleCycles=0;
        FieldCycleRepository verifier=new FieldCycleRepository(db);
        for(FieldCycleRepository.Cycle cycle:approved.cycleLedger){
            if(!cycle.approved()||!cycle.modes.gas)continue;
            String sid=cycle.measurementId;
            if(!approved.measurementSessionIds.contains(sid))
                throw new IllegalStateException("Approved gas cycle missing from bundle");
            // A previously resolved Bundle is not a permanent authorization:
            // guard against a concurrent DB mutation between approval/resolve
            // and this second read of raw archived telemetry.
            if(!verifier.verifyApprovedEvidence(cycle))
                throw new IllegalStateException("Archived approved source changed before V6 replay");
            JSONObject derived=analyzeOneVerifiedCycle(db,sid,cycle.id);
            if(!verifier.verifyApprovedEvidence(cycle))
                throw new IllegalStateException("Archived approved source changed during V6 replay");
            entries.put(derived);
            eligibleCycles++;
        }
        root.put("approved_gas_cycles",eligibleCycles);
        root.put("cycles",entries);
        return root;
    }

    private static JSONObject analyzeOneVerifiedCycle(ExperimentsDb db,String sid,
                                                      String cycleId)throws Exception{
        Map<String,Accumulator> perSensor=new LinkedHashMap<>();
        for(HazRobHardwareProfile.Channel model:HazRobHardwareProfile.MQ)
            perSensor.put(model.key,new Accumulator());
        List<String> originalRows=new ArrayList<>();
        int readRows=0,malformedRows=0;
        // This is a read, not a measurement: original frozen payloads only.
        try(Cursor c=db.getReadableDatabase().rawQuery(
            "SELECT t_ms,mq_payload,env_payload FROM telemetry_samples "+
            "WHERE session_id=? ORDER BY t_ms ASC,id ASC",new String[]{sid})){
            while(c.moveToNext()){
                long t=c.getLong(0);
                String mqText=c.isNull(1)?"":c.getString(1);
                String envText=c.isNull(2)?"":c.getString(2);
                readRows++;
                originalRows.add(t+"\n"+mqText+"\n"+envText);
                JSONObject mq,env;
                try{
                    mq=new JSONObject(mqText);
                    env=envText.isEmpty()?new JSONObject():new JSONObject(envText);
                }catch(Exception bad){
                    malformedRows++;continue;
                }
                for(Map.Entry<String,Accumulator> entry:perSensor.entrySet()){
                    JSONObject q=mq.optJSONObject(entry.getKey());
                    if(q==null)continue;
                    Accumulator a=entry.getValue();
                    a.encountered++;
                    if(!eligible(q))continue;
                    a.accepted++;
                    long unique=q.optLong("scan_cycle",-1);
                    a.distinct.putIfAbsent(unique,new Sample(t,unique,q,env));
                }
            }
        }
        JSONObject cycle=new JSONObject();
        cycle.put("measurement_session_id",sid);
        cycle.put("cycle_id",cycleId);
        cycle.put("original_archived_telemetry_rows",readRows);
        cycle.put("malformed_archived_rows",malformedRows);
        cycle.put("research_source_sha256",sourceHash(sid,originalRows));
        cycle.put("reported_identity","UNKNOWN");
        cycle.put("reportable_ppm",false);
        cycle.put("independent_tank_reference_calibration",false);
        cycle.put("source_replay_only",true);
        JSONArray sensors=new JSONArray();
        int evaluated=0;
        for(Map.Entry<String,Accumulator> e:perSensor.entrySet()){
            String key=e.getKey();Accumulator a=e.getValue();
            JSONObject evidence=new JSONObject();
            evidence.put("sensor_model",key.toUpperCase(Locale.ROOT));
            evidence.put("original_rows_with_channel",a.encountered);
            evidence.put("eligible_original_channel_rows",a.accepted);
            evidence.put("distinct_original_scan_cycles",a.distinct.size());
            evidence.put("minimum_scan_cycles",10);
            // Not every approved gas cycle has enough evidence for each individual
            // MQ channel. Do not artificially extend sparse channels.
            if(a.distinct.size()<10){
                evidence.put("state","INSUFFICIENT_UNIQUE_SOURCE_SCANS");
                evidence.put("ratio_ready",false);
                sensors.put(evidence);continue;
            }
            List<Sample> uniqueSamples=new ArrayList<>(a.distinct.values());
            // Select an EXACT original observation, not averaged physically
            // incompatible R0 references across scans.
            Sample original=uniqueSamples.get(uniqueSamples.size()/2);
            JSONObject one=new JSONObject(),mq=new JSONObject();
            mq.put(key,new JSONObject(original.observed.toString()));
            one.put("mq",mq);
            one.put("env",new JSONObject(original.environment.toString()));
            ChemCoreTankV6.Result result=
                TankV6TelemetryAdapter.analyzeSnapshot(one);
            JSONObject computed=TankV6TelemetryAdapter.toJson(result);
            if(computed.optBoolean("reportable_ppm",true)||
                computed.optBoolean("physical_calibration_proven",true)||
                computed.optBoolean("food_models_included",true))
                throw new IllegalStateException("V6 research data incorrectly promoted");
            evidence.put("state","SOURCE_ONLY_RESEARCH_UNVALIDATED");
            evidence.put("selected_original_scan_cycle",original.scanCycle);
            evidence.put("selected_original_t_ms",original.timestamp);
            evidence.put("selected_original_rs",original.observed.optDouble("rs",Double.NaN));
            evidence.put("selected_original_r0",
                Double.isFinite(original.observed.optDouble("r0",Double.NaN))?
                    original.observed.optDouble("r0",Double.NaN):JSONObject.NULL);
            evidence.put("observed_mq_channels",computed.optInt("observed_mq_channels",0));
            evidence.put("raw_only_mq_channels",computed.optInt("raw_only_mq_channels",0));
            evidence.put("ratio_ready_channels",computed.optInt("ratio_ready_mq_channels",0));
            evidence.put("source_curve_hypotheses",
                computed.optJSONArray("source_curve_hypotheses"));
            evidence.put("source_curve_disagreements",
                computed.optJSONArray("source_curve_disagreements"));
            evidence.put("chemical_evidence_ledger",
                computed.optJSONArray("chemical_evidence_ledger"));
            evidence.put("reported_identity","UNKNOWN");
            evidence.put("reportable_ppm",false);
            evaluated++;
            sensors.put(evidence);
        }
        cycle.put("independently_evaluated_sensor_models",evaluated);
        cycle.put("sensors",sensors);
        cycle.put("scientific_status",evaluated==0?
            "NOT_EVALUABLE_RAW_SOURCE_ONLY":"SOURCE_RESEARCH_UNVALIDATED");
        return cycle;
    }
}
