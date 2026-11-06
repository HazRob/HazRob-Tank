package com.servoc.app.chemcore.nextintegration;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.hazrob.tank.chemcore.next.ChemCoreNext;
import com.hazrob.tank.chemcore.next.CoreContracts;
import com.hazrob.tank.chemcore.next.NextReplayCodec;
import com.hazrob.tank.chemcore.next.NextAnalysisTrace;
import java.util.List;
import com.servoc.app.data.ExperimentsDb;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

/**
 * V8 P07.2: atomic append/read-back of an actual ChemCore Next decision.
 * Stored snapshot + output are byte-faithful versioned replay envelopes.
 * The DB rejects UPDATE/DELETE, duplicate decisions and certified ppm/G3.
 * No old V7 chemcore_decisions, measurement_sessions or telemetry rows altered.
 */
public final class NextDecisionStore {
  private final ExperimentsDb db;
  public NextDecisionStore(ExperimentsDb db) {
    if(db==null)throw new IllegalArgumentException("SQLite database required");
    this.db=db;
  }
  public static final class Receipt {
    public final String decisionUid,sessionId,snapshotUid,snapshotSha256,engineVersion;
    public final CoreContracts.DecisionStatus status;
    public final long createdUtcMs;
    public final CoreContracts.Snapshot sourceSnapshot;
    public final CoreContracts.Result originalDecision;
    /** R3 trace is null only for historical V15 rows created before trace archiving. */
    public final List<NextAnalysisTrace.Event> executionTrace;
    public final String traceSha256,traceAvailability;
    private Receipt(String uid,String session,String snapshot,String hash,String engine,
                    CoreContracts.DecisionStatus status,long time,
                    CoreContracts.Snapshot captured,CoreContracts.Result output,
                    NextResearchTraceStore.Archive archive){
      decisionUid=uid;sessionId=session;snapshotUid=snapshot;
      snapshotSha256=hash;engineVersion=engine;this.status=status;createdUtcMs=time;
      sourceSnapshot=captured;originalDecision=output;
      executionTrace=archive==null?null:archive.events;
      traceSha256=archive==null?null:archive.sha256;
      traceAvailability=archive==null?"NOT_ARCHIVED_BEFORE_V9_R3":
          "VERIFIED_R3_IMMUTABLE_TRACE";
    }
  }
  private static String require(String value,String field){
    if(value==null||value.isBlank())throw new IllegalArgumentException(field+" required");
    return value.trim();
  }
  private static String digest(String content){
    try {
      MessageDigest md=MessageDigest.getInstance("SHA-256");
      byte[] bytes=md.digest(content.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb=new StringBuilder();
      for(byte b:bytes)sb.append(String.format(Locale.ROOT,"%02x",b&255));
      return sb.toString();
    }catch(Exception e){throw new IllegalStateException("SHA-256 unavailable",e);}
  }
  /** Legacy V15 API remains compatible; it must NOT fabricate an R3 trace. */
  public Receipt append(String originalSessionId,long utcMs,
                        CoreContracts.Snapshot capture,CoreContracts.Result decision) {
    return appendInternal(originalSessionId,utcMs,capture,decision,null);
  }
  /** R3 decision and every execution-stage event commit in ONE SQLite transaction. */
  public Receipt appendWithTrace(String originalSessionId,long utcMs,
                        CoreContracts.Snapshot capture,CoreContracts.Result decision,
                        List<NextAnalysisTrace.Event> executionTrace) {
    if(executionTrace==null||executionTrace.isEmpty())
      throw new IllegalArgumentException("R3 original execution trace required");
    return appendInternal(originalSessionId,utcMs,capture,decision,executionTrace);
  }
  private Receipt appendInternal(String originalSessionId,long utcMs,
                        CoreContracts.Snapshot capture,CoreContracts.Result decision,
                        List<NextAnalysisTrace.Event> executionTrace) {
    String session=require(originalSessionId,"measurement session");
    if(utcMs<0||capture==null||decision==null)
      throw new IllegalArgumentException("physical session, time, capture and Next output required");
    String expectedHash=ChemCoreNext.snapshotHash(capture);
    if(!decision.snapshotUid.equals(capture.uid)||
       !expectedHash.equals(decision.snapshotHash)||
       !decision.stableManifestHash.equals(capture.stableManifestHash)||
       decision.reportablePpm||decision.physicallyValidatedG3)
      throw new IllegalArgumentException("result/snapshot mismatch or unsupported chemical reportability");
    if(!capture.stableManifestHash.matches("[0-9a-f]{64}"))
      throw new IllegalArgumentException("actual verified Stable SHA required");
    final String frozenSnapshot=NextReplayCodec.encodeSnapshot(capture);
    final String frozenDecision=NextReplayCodec.encodeResult(decision);
    // Replay both before committing any record. The codec verifies checksum,
    // stable snapshot, raw readings and canonical record structure.
    var sourceReplayed=NextReplayCodec.decodeSnapshot(frozenSnapshot);
    var decisionReplayed=NextReplayCodec.decodeResult(frozenDecision,sourceReplayed);
    if(decisionReplayed.status!=decision.status||
       decisionReplayed.rankedEligibleCandidates.size()!=decision.rankedEligibleCandidates.size())
      throw new IllegalStateException("original Next result did not survive lossless replay");
    final String uid="NEXT-"+digest(session+"|"+capture.uid+"|"+
      expectedHash+"|"+decision.engineVersion);
    SQLiteDatabase w=db.getWritableDatabase();
    w.beginTransaction();
    try{
      Receipt receipt=readBack(w,uid);
      if(receipt==null){
        ContentValues v=new ContentValues();
        v.put("decision_uid",uid);v.put("session_id",session);
        v.put("snapshot_uid",capture.uid);v.put("snapshot_sha256",expectedHash);
        v.put("stable_sha256",capture.stableManifestHash);
        v.put("engine_version",decision.engineVersion);v.put("decision_status",decision.status.name());
        v.put("snapshot_replay",frozenSnapshot);v.put("result_replay",frozenDecision);
        v.put("created_utc_ms",utcMs);
        v.put("candidate_count",decision.rankedEligibleCandidates.size());
        v.put("research_only",1);v.put("ppm_reportable",0);v.put("g3_physical",0);
        w.insertOrThrow("next_research_decisions",null,v);
        receipt=readBack(w,uid);
      }
      // Both writes are within the same transaction: a trace failure rolls back
      // the newly created decision as well (no half-persisted R3 decision).
      if(executionTrace!=null){
        NextResearchTraceStore.append(w,uid,expectedHash,decision.engineVersion,
            executionTrace,utcMs);
        receipt=readBack(w,uid);
        if(receipt==null||receipt.executionTrace==null||
           receipt.executionTrace.size()!=executionTrace.size()||
           receipt.traceSha256==null)
          throw new IllegalStateException("NEXT_R3_ATOMIC_TRACE_READBACK_FAILED");
      }
      if(receipt==null||!receipt.sessionId.equals(session)||
         !receipt.snapshotSha256.equals(expectedHash)||
         !receipt.engineVersion.equals(decision.engineVersion)||
         !receipt.status.equals(decision.status)||
         !NextReplayCodec.encodeSnapshot(receipt.sourceSnapshot).equals(frozenSnapshot)||
         !NextReplayCodec.encodeResult(receipt.originalDecision).equals(frozenDecision))
        throw new IllegalStateException("read-after-write research decision evidence mismatch");
      w.setTransactionSuccessful();
      return receipt;
    }finally{w.endTransaction();}
  }
  /** Previous session evidence stays read-only and can be reopened after restart. */
  public Receipt latestForSession(String originalSessionId) {
    String session=require(originalSessionId,"session");
    SQLiteDatabase r=db.getReadableDatabase();
    try(Cursor c=r.rawQuery("SELECT decision_uid FROM next_research_decisions "+
         "WHERE session_id=? ORDER BY created_utc_ms DESC,"+
         "CASE WHEN engine_version='"+ChemCoreNext.VERSION+"' THEN 1 ELSE 0 END DESC,"+
         "decision_uid DESC LIMIT 1",
         new String[]{session})){
       return c.moveToFirst()?readBack(r,c.getString(0)):null;
    }
  }
  public Receipt byId(String originalDecisionUid){
    return readBack(db.getReadableDatabase(),require(originalDecisionUid,"decision UID"));
  }
  private static Receipt readBack(SQLiteDatabase db,String id) {
    try(Cursor c=db.rawQuery("SELECT decision_uid,session_id,snapshot_uid,snapshot_sha256,"+
        "stable_sha256,engine_version,decision_status,snapshot_replay,result_replay,"+
        "created_utc_ms,candidate_count,research_only,ppm_reportable,g3_physical "+
        "FROM next_research_decisions WHERE decision_uid=?",new String[]{id})){
      if(!c.moveToFirst())return null;
      if(c.getInt(11)!=1||c.getInt(12)!=0||c.getInt(13)!=0)
        throw new IllegalStateException("not a research-only Next record");
      CoreContracts.Snapshot snapshot=NextReplayCodec.decodeSnapshot(c.getString(7));
      CoreContracts.Result result=NextReplayCodec.decodeResult(c.getString(8),snapshot);
      if(!c.getString(2).equals(snapshot.uid)||
         !c.getString(3).equals(ChemCoreNext.snapshotHash(snapshot))||
         !c.getString(4).equals(snapshot.stableManifestHash)||
         !c.getString(5).equals(result.engineVersion)||
         !c.getString(6).equals(result.status.name())||
         c.getInt(10)!=result.rankedEligibleCandidates.size())
        throw new IllegalStateException("persisted Next evidence metadata differs from immutable replay");
      NextResearchTraceStore.Archive trace=NextResearchTraceStore.read(
          db,c.getString(0),c.getString(3),c.getString(5));
      return new Receipt(c.getString(0),c.getString(1),c.getString(2),c.getString(3),
        c.getString(5),result.status,c.getLong(9),snapshot,result,trace);
    }
  }
}
