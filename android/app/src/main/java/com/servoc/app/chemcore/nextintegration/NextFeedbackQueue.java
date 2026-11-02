package com.servoc.app.chemcore.nextintegration;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.hazrob.tank.chemcore.next.CoreContracts;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * V8 P11.3/P11.4: isolated append-only local Feedback outbox.
 *
 * Feedback never changes original Next decisions, never promotes scientific
 * results, and never uploads without explicit PUBLIC repository consent.
 * Status PENDING_AUTH means upload is awaiting authorized GitHub writing
 * credentials: public repository visibility alone cannot grant write access.
 */
public final class NextFeedbackQueue extends SQLiteOpenHelper {
  public static final String SCHEMA="hazrob.tank.v8.feedback/1";
  public static final String LOCAL_ONLY="LOCAL_ONLY";
  public static final String PENDING_AUTH="PENDING_AUTH";
  public static final String SENT="SENT";
  public static final int VERSION=1;
  public NextFeedbackQueue(Context c){
    super(c.getApplicationContext(),"hazrob_feedback_outbox.db",null,VERSION);
  }
  @Override public void onCreate(SQLiteDatabase db){
    db.execSQL("CREATE TABLE feedback_outbox ("+
      "feedback_id TEXT PRIMARY KEY NOT NULL,"+
      "decision_uid TEXT NOT NULL, snapshot_sha256 TEXT NOT NULL,"+
      "payload_json TEXT NOT NULL, payload_sha256 TEXT NOT NULL,"+
      "status TEXT NOT NULL CHECK(status IN ('LOCAL_ONLY','PENDING_AUTH','SENT')),"+
      "public_share_consent INTEGER NOT NULL CHECK(public_share_consent IN (0,1)),"+
      "created_utc_ms INTEGER NOT NULL, uploaded_utc_ms INTEGER,"+
      "receipt_url TEXT, last_failure TEXT, attempts INTEGER NOT NULL DEFAULT 0)");
    db.execSQL("CREATE INDEX feedback_outbox_status_created ON "+
      "feedback_outbox(status,created_utc_ms)");
    db.execSQL("CREATE TRIGGER feedback_original_immutable BEFORE UPDATE OF "+
      "feedback_id,decision_uid,snapshot_sha256,payload_json,payload_sha256,"+
      "public_share_consent,created_utc_ms ON feedback_outbox "+
      "BEGIN SELECT RAISE(ABORT,'feedback evidence is immutable'); END");
    db.execSQL("CREATE TRIGGER feedback_no_delete BEFORE DELETE ON feedback_outbox "+
      "BEGIN SELECT RAISE(ABORT,'feedback evidence is immutable'); END");
  }
  @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){
    throw new IllegalStateException("Unexpected Feedback queue upgrade; explicit audited migration required");
  }
  private static String sha(String value){
    try{
      byte[] bytes=MessageDigest.getInstance("SHA-256")
        .digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder hex=new StringBuilder();
      for(byte b:bytes)hex.append(String.format(Locale.ROOT,"%02x",b&255));
      return hex.toString();
    }catch(Exception e){throw new IllegalStateException("SHA256 unavailable",e);}
  }
  private static JSONObject numberOrNull(Double value){
    JSONObject out=new JSONObject();
    // Not used as a canonical object: put values only after finite check.
    return out;
  }
  private static Object finiteValue(Double value){
    return value!=null&&Double.isFinite(value)?value:JSONObject.NULL;
  }
  public static final class Record {
    public final String id,decisionUid,snapshotSha256,payloadJson,payloadSha256,status,receiptUrl;
    public final boolean publicConsent;
    Record(String id,String uid,String snap,String payload,String hash,String status,
           String url,boolean consent){
      this.id=id;decisionUid=uid;snapshotSha256=snap;payloadJson=payload;
      payloadSha256=hash;this.status=status;receiptUrl=url;publicConsent=consent;
    }
  }
  /** Persist, only from a verified replayed original Next decision. */
  public Record add(Context context, NextDecisionStore.Receipt receipt,String userNote,
                    boolean publicShareConsent) throws Exception {
    if(context==null||receipt==null||userNote==null||userNote.trim().isEmpty())
      throw new IllegalArgumentException("Verified decision and nonempty user opinion required");
    String note=userNote.trim();
    if(note.length()>4000)throw new IllegalArgumentException("Feedback maximum 4000 characters");
    JSONObject original=NextDecisionEnvelope.fromReceipt(receipt);
    JSONObject message=new JSONObject();
    String feedbackId="FB-"+UUID.randomUUID();
    message.put("schema",SCHEMA);
    message.put("feedback_id",feedbackId);
    message.put("created_utc_ms",System.currentTimeMillis());
    message.put("decision_uid",receipt.decisionUid);
    message.put("session_id",receipt.sessionId);
    message.put("snapshot_uid",receipt.snapshotUid);
    message.put("snapshot_sha256",receipt.snapshotSha256);
    message.put("model_version",receipt.engineVersion);
    message.put("decision_status",receipt.status.name());
    message.put("stable_sha256",receipt.sourceSnapshot.stableManifestHash);
    String appVersion;
    long versionCode;
    try{
      android.content.pm.PackageInfo info=context.getPackageManager()
        .getPackageInfo(context.getPackageName(),0);
      appVersion=info.versionName==null?"UNSET":info.versionName;
      versionCode=android.os.Build.VERSION.SDK_INT>=28?
        info.getLongVersionCode():info.versionCode;
    }catch(Exception e){
      appVersion="UNKNOWN_BUILD_NOT_CERTIFIED";versionCode=-1;
    }
    message.put("application_package",context.getPackageName());
    message.put("app_version",appVersion);
    message.put("app_version_code",versionCode);
    message.put("user_note",note);
    message.put("operator_classification","UNVERIFIED_PERSONAL_OBSERVATION");
    message.put("public_share_consent",publicShareConsent);
    message.put("engine_result_research_only",true);
    message.put("original_analysis",original);
    JSONArray sensors=new JSONArray();
    for(CoreContracts.Reading r:receipt.sourceSnapshot.readings){
      JSONObject sensor=new JSONObject();
      sensor.put("uid",r.sensorUid);
      sensor.put("raw_adc",r.rawAdc==null?JSONObject.NULL:r.rawAdc);
      sensor.put("rs_kohm",finiteValue(r.rsKohm));
      sensor.put("device_air_reference_kohm",finiteValue(r.rAirKohm));
      sensor.put("r0_source_kohm",finiteValue(r.r0SourceKohm));
      sensor.put("quality",r.quality.name());
      sensor.put("reference_type",r.r0Definition.name());
      sensors.put(sensor);
    }
    message.put("original_mq_measurements",sensors);
    message.put("temperature_c",finiteValue(receipt.sourceSnapshot.temperatureC));
    message.put("relative_humidity_pct",finiteValue(
      receipt.sourceSnapshot.relativeHumidityPercent));
    message.put("ppm_certified",false);
    message.put("physical_g3_qualified",false);
    message.put("training_source_promotion_authorized",false);
    String payload=message.toString();
    String hash=sha(payload);
    ContentValues v=new ContentValues();
    v.put("feedback_id",feedbackId);
    v.put("decision_uid",receipt.decisionUid);
    v.put("snapshot_sha256",receipt.snapshotSha256);
    v.put("payload_json",payload);
    v.put("payload_sha256",hash);
    v.put("status",publicShareConsent?PENDING_AUTH:LOCAL_ONLY);
    v.put("public_share_consent",publicShareConsent?1:0);
    v.put("created_utc_ms",System.currentTimeMillis());
    SQLiteDatabase db=getWritableDatabase();
    db.beginTransaction();
    try{
      db.insertOrThrow("feedback_outbox",null,v);
      db.setTransactionSuccessful();
    }finally{db.endTransaction();}
    return get(feedbackId);
  }
  public Record get(String feedbackId){
    try(Cursor c=getReadableDatabase().rawQuery("SELECT feedback_id,decision_uid,"+
      "snapshot_sha256,payload_json,payload_sha256,status,receipt_url,public_share_consent "+
      "FROM feedback_outbox WHERE feedback_id=?",new String[]{feedbackId})){
      return c.moveToFirst()?fromCursor(c):null;
    }
  }
  private static Record fromCursor(Cursor c){
    String payload=c.getString(3);
    if(!sha(payload).equals(c.getString(4)))
      throw new IllegalStateException("Feedback outbox payload tamper detected");
    return new Record(c.getString(0),c.getString(1),c.getString(2),
      payload,c.getString(4),c.getString(5),c.getString(6),c.getInt(7)!=0);
  }
  public List<Record> pending(int limit){
    List<Record> out=new ArrayList<>();
    if(limit<1||limit>100)throw new IllegalArgumentException("1..100 batch");
    try(Cursor c=getReadableDatabase().rawQuery("SELECT feedback_id,decision_uid,"+
      "snapshot_sha256,payload_json,payload_sha256,status,receipt_url,public_share_consent "+
      "FROM feedback_outbox WHERE status='PENDING_AUTH' AND public_share_consent=1 "+
      "ORDER BY created_utc_ms,feedback_id LIMIT "+limit,null)){
      while(c.moveToNext())out.add(fromCursor(c));
    }
    return out;
  }
  public void recordFailure(String feedbackId,String reason){
    ContentValues v=new ContentValues();
    v.put("last_failure",reason==null?"UPLOAD_UNAVAILABLE":reason.substring(0,Math.min(reason.length(),300)));
    getWritableDatabase().update("feedback_outbox",v,
      "feedback_id=? AND status='PENDING_AUTH'",new String[]{feedbackId});
  }
  /** Only a real successful authenticated GitHub write result can call this. */
  public boolean confirmRemoteReceipt(String id,String remoteReceipt,String expectedPayloadSha){
    Record found=get(id);
    if(found==null||!found.publicConsent||!PENDING_AUTH.equals(found.status)||
        !found.payloadSha256.equals(expectedPayloadSha)||
        remoteReceipt==null||!remoteReceipt.startsWith(
          "https://github.com/HazRob/HazRob-Tank/blob/"))
      return false;
    ContentValues v=new ContentValues();
    v.put("status",SENT);v.put("receipt_url",remoteReceipt);
    v.put("uploaded_utc_ms",System.currentTimeMillis());
    v.putNull("last_failure");
    return getWritableDatabase().update("feedback_outbox",v,
      "feedback_id=? AND status='PENDING_AUTH' AND public_share_consent=1",
      new String[]{id})==1;
  }
}
