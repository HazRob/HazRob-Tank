package com.servoc.app.chemcore.nextintegration;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.hazrob.tank.chemcore.next.NextAnalysisTrace;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Immutable, lossless source-execution evidence from the SAME R3 analysis
 * operation. The caller holds the original NextDecisionStore transaction.
 * No recomputation, no source modification and no fake G3 qualification.
 */
public final class NextResearchTraceStore {
  public static final String SCHEMA="hazrob.tank.v9.chemcore_next_r3.trace/1";
  private NextResearchTraceStore(){}
  public static final class Archive {
    public final String decisionUid,snapshotSha256,engineVersion,sha256;
    public final List<NextAnalysisTrace.Event> events;
    private Archive(String uid,String hash,String engine,String digest,
                    List<NextAnalysisTrace.Event> events){
      decisionUid=uid;snapshotSha256=hash;engineVersion=engine;sha256=digest;
      this.events=List.copyOf(events);
    }
  }
  private static String sha256(String value) {
    try {
      byte[] bytes=MessageDigest.getInstance("SHA-256").digest(
          value.getBytes(StandardCharsets.UTF_8));
      StringBuilder b=new StringBuilder(64);
      for(byte x:bytes)b.append(String.format(Locale.ROOT,"%02x",x&255));
      return b.toString();
    }catch(Exception e){throw new IllegalStateException("SHA256 unavailable",e);}
  }
  private static String encode(String uid,String snapshot,String engine,
                               List<NextAnalysisTrace.Event> events) {
    if(events==null||events.isEmpty()||
        !"INPUT".equals(events.get(0).stage)||
        !"FINAL".equals(events.get(events.size()-1).stage))
      throw new IllegalArgumentException("complete original R3 input-to-final trace required");
    try{
      JSONObject envelope=new JSONObject();
      envelope.put("schema",SCHEMA);
      envelope.put("decision_uid",uid);
      envelope.put("snapshot_sha256",snapshot);
      envelope.put("engine_version",engine);
      envelope.put("research_only",true);
      envelope.put("event_count",events.size());
      JSONArray array=new JSONArray();
      for(NextAnalysisTrace.Event event:NextAnalysisTrace.immutable(events)){
        JSONObject row=new JSONObject();
        row.put("stage",event.stage);
        row.put("asset_id",event.sourceAssetId);
        row.put("source_root",event.sourceRoot);
        row.put("sensor_uid",event.sensorUid);
        row.put("chemical_uid",event.chemicalUid==null?JSONObject.NULL:event.chemicalUid);
        row.put("status",event.status);
        row.put("reason",event.reason);
        row.put("source_diagnostic_value",event.sourceOnlyDiagnosticValue==null?
            JSONObject.NULL:event.sourceOnlyDiagnosticValue);
        array.put(row);
      }
      envelope.put("events",array);
      return envelope.toString();
    }catch(JSONException e){throw new IllegalStateException("trace encoding failed",e);}
  }
  private static List<NextAnalysisTrace.Event> decode(String raw,String uid,
                                                       String snapshot,String engine,
                                                       int expectedCount) {
    try{
      JSONObject obj=new JSONObject(raw);
      if(!SCHEMA.equals(obj.getString("schema"))||!uid.equals(obj.getString("decision_uid"))||
         !snapshot.equals(obj.getString("snapshot_sha256"))||
         !engine.equals(obj.getString("engine_version"))||
         !obj.getBoolean("research_only")||obj.getInt("event_count")!=expectedCount)
        throw new IllegalStateException("NEXT_R3_TRACE_PROVENANCE_MISMATCH");
      JSONArray a=obj.getJSONArray("events");
      if(a.length()!=expectedCount||expectedCount<2)
        throw new IllegalStateException("NEXT_R3_TRACE_LENGTH_MISMATCH");
      var items=new ArrayList<NextAnalysisTrace.Event>();
      for(int i=0;i<a.length();i++){
        JSONObject row=a.getJSONObject(i);
        Long chemical=row.isNull("chemical_uid")?null:row.getLong("chemical_uid");
        Double value=row.isNull("source_diagnostic_value")?null:
            row.getDouble("source_diagnostic_value");
        items.add(new NextAnalysisTrace.Event(row.getString("stage"),
            row.getString("asset_id"),row.getString("source_root"),
            row.getString("sensor_uid"),chemical,row.getString("status"),
            row.getString("reason"),value));
      }
      if(!"INPUT".equals(items.get(0).stage)||
         !"FINAL".equals(items.get(items.size()-1).stage))
        throw new IllegalStateException("NEXT_R3_TRACE_UNSEALED");
      return List.copyOf(items);
    }catch(JSONException e){
      throw new IllegalStateException("NEXT_R3_TRACE_JSON_CORRUPT",e);
    }
  }
  /** Required inside decision transaction to guarantee atomic persistence. */
  public static Archive append(SQLiteDatabase w,String decisionUid,
                               String snapshotSha256,String engineVersion,
                               List<NextAnalysisTrace.Event> originalEvents,long utcMs) {
    if(w==null||decisionUid==null||!decisionUid.startsWith("NEXT-")||
       snapshotSha256==null||!snapshotSha256.matches("[0-9a-f]{64}")||
       engineVersion==null||engineVersion.isBlank()||utcMs<0)
      throw new IllegalArgumentException("complete original trace parent identity required");
    String payload=encode(decisionUid,snapshotSha256,engineVersion,originalEvents);
    String digest=sha256(payload);
    Archive stored=read(w,decisionUid,snapshotSha256,engineVersion);
    if(stored==null){
      ContentValues v=new ContentValues();
      v.put("decision_uid",decisionUid);
      v.put("snapshot_sha256",snapshotSha256);
      v.put("engine_version",engineVersion);
      v.put("trace_schema",SCHEMA);
      v.put("trace_sha256",digest);
      v.put("trace_json",payload);
      v.put("event_count",originalEvents.size());
      v.put("created_utc_ms",utcMs);
      w.insertOrThrow("next_research_trace_v16",null,v);
      stored=read(w,decisionUid,snapshotSha256,engineVersion);
    }
    if(stored==null||!digest.equals(stored.sha256)||
       stored.events.size()!=originalEvents.size())
      throw new IllegalStateException("NEXT_R3_TRACE_IDEMPOTENT_READBACK_FAILED");
    return stored;
  }
  /** A V8/V15 archived decision is distinguishable from an R3 archived trace. */
  public static Archive read(SQLiteDatabase db,String decisionUid,
                             String snapshotSha256,String engineVersion) {
    if(db==null||decisionUid==null||snapshotSha256==null||engineVersion==null)
      throw new IllegalArgumentException("complete trace lookup required");
    try(Cursor c=db.rawQuery("SELECT snapshot_sha256,engine_version,trace_schema,"+
        "trace_sha256,trace_json,event_count FROM next_research_trace_v16 "+
        "WHERE decision_uid=?",new String[]{decisionUid})){
      if(!c.moveToFirst())return null;
      String actualSnapshot=c.getString(0),actualEngine=c.getString(1);
      if(!snapshotSha256.equals(actualSnapshot)||!engineVersion.equals(actualEngine)||
         !SCHEMA.equals(c.getString(2)))
        throw new IllegalStateException("NEXT_R3_TRACE_ORPHAN_OR_PARENT_MISMATCH");
      String raw=c.getString(4),sum=c.getString(3);
      if(!sha256(raw).equals(sum))
        throw new IllegalStateException("NEXT_R3_TRACE_SHA256_MISMATCH");
      return new Archive(decisionUid,snapshotSha256,engineVersion,sum,
          decode(raw,decisionUid,snapshotSha256,engineVersion,c.getInt(5)));
    }
  }
  public static JSONArray asJson(List<NextAnalysisTrace.Event> events) {
    JSONArray result=new JSONArray();
    if(events==null)return result;
    for(NextAnalysisTrace.Event e:events){
      JSONObject row=new JSONObject();
      try{
        row.put("stage",e.stage);row.put("source_asset",e.sourceAssetId);
        row.put("source_root",e.sourceRoot);row.put("sensor_uid",e.sensorUid);
        row.put("chemical_uid",e.chemicalUid==null?JSONObject.NULL:e.chemicalUid);
        row.put("status",e.status);row.put("reason",e.reason);
        row.put("source_diagnostic_value",e.sourceOnlyDiagnosticValue==null?
            JSONObject.NULL:e.sourceOnlyDiagnosticValue);
      }catch(JSONException exception){throw new IllegalStateException(exception);}
      result.put(row);
    }
    return result;
  }
}
