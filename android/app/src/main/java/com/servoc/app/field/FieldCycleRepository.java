package com.servoc.app.field;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.servoc.app.data.ExperimentsDb;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

/**
 * V5 documented field cycles. Site/session is a container; every user-authorized
 * measurement attempt is immutable after approval. Never records INSTANT results.
 *
 * Contract: all calls perform SQLite work; callers MUST use an IO executor.
 * The companion legacy field_session_measurements table remains for V4 reports.
 */
public final class FieldCycleRepository {
    public static final String ARMED="ARMED",WARMUP="WARMUP",SAMPLING="SAMPLING";
    public static final String REVIEW="REVIEW_READY",APPROVED="APPROVED";
    public static final String FAILED="FAILED",CANCELLED="CANCELLED",SUPERSEDED="SUPERSEDED",DELETED="DELETED";

    public static final class Options {
        public final boolean gas,particles,physical;
        public Options(boolean gas,boolean particles,boolean physical){
            if(!gas&&!particles&&!physical)throw new IllegalArgumentException("Select at least one measurement family");
            this.gas=gas;this.particles=particles;this.physical=physical;
        }
        public static Options defaults(){return new Options(true,false,true);}
    }

    public static final class Cycle {
        public final String id,fieldSessionId,measurementId,state,started,completed,approved,supersedes,deviceId,evidenceSha,failReason;
        public final int sequence,attempt;
        public final Options modes;
        Cycle(Cursor c){
            id=c.getString(0);fieldSessionId=c.getString(1);sequence=c.getInt(2);
            attempt=c.getInt(3);supersedes=nz(c.getString(4));measurementId=nz(c.getString(5));
            modes=new Options(c.getInt(6)!=0,c.getInt(7)!=0,c.getInt(8)!=0);
            state=c.getString(9);started=c.getString(10);completed=nz(c.getString(11));approved=nz(c.getString(12));deviceId=nz(c.getString(13));evidenceSha=nz(c.getString(14));failReason=nz(c.getString(15));
        }
        public boolean reviewReady(){return REVIEW.equals(state);}
        public boolean approved(){return APPROVED.equals(state)||"LEGACY_APPROVED".equals(state);}
    }

    private static final String SELECT="SELECT cycle_id,field_session_id,sequence_no,attempt_no,"+
        "supersedes_cycle_id,measurement_session_id,include_gas,include_particles,include_physical,"+
        "state,started_ts,completed_ts,approved_ts,device_id,evidence_sha256,fail_reason FROM field_measurement_cycles";
    private final ExperimentsDb db;
    public FieldCycleRepository(ExperimentsDb db){
        if(db==null)throw new IllegalArgumentException("Database required");
        this.db=db;
    }

    /** Reconcile dead-process acquisitions only when the recorder has no owner. */
    public int recoverAbandoned(){
        if(com.servoc.app.measurement.MeasurementSessionRegistry.fieldCaptureRequested()||
            !com.servoc.app.measurement.MeasurementSessionRegistry.activeSessionId().isEmpty())return 0;
        SQLiteDatabase w=db.getWritableDatabase();w.beginTransaction();
        try{
            String ts=utc();
            // Do not leave a permanently open technical measurement when Android
            // was killed. End it as interrupted; never call it successful.
            ContentValues ended=new ContentValues();ended.put("ended_ts",ts);
            w.update("measurement_sessions",ended,
                "session_id IN (SELECT measurement_session_id FROM field_measurement_cycles "+
                "WHERE state IN ('ARMED','WARMUP','SAMPLING') AND measurement_session_id IS NOT NULL) "+
                "AND (ended_ts IS NULL OR TRIM(ended_ts)='')",null);
            ContentValues v=new ContentValues();v.put("state",FAILED);
            v.put("fail_reason","PROCESS_INTERRUPTED_BEFORE_APPROVAL");v.put("updated_ts",ts);
            int n=w.update("field_measurement_cycles",v,
                    "state IN ('ARMED','WARMUP','SAMPLING')",null);
            w.setTransactionSuccessful();
            return n;
        }finally{w.endTransaction();}
    }

    public Cycle byId(String cycleId){
        try(Cursor c=db.getReadableDatabase().rawQuery(SELECT+" WHERE cycle_id=?",
                new String[]{required(cycleId)})){return c.moveToFirst()?new Cycle(c):null;}
    }
    public List<Cycle> forSession(String fieldId){
        List<Cycle> rows=new ArrayList<>();
        try(Cursor c=db.getReadableDatabase().rawQuery(SELECT+
                " WHERE field_session_id=? ORDER BY sequence_no ASC",new String[]{required(fieldId)})){
            while(c.moveToNext())rows.add(new Cycle(c));
        }
        return rows;
    }

    /** Reserve a unique sequence under one database transaction; do not power the relay here. */
    public Cycle begin(String fieldSessionId,Options options){
        return beginInternal(fieldSessionId,options,null);
    }
    private Cycle beginInternal(String fieldSessionId,Options options,Cycle predecessor){
        String id=required(fieldSessionId);
        if(options==null)throw new IllegalArgumentException("Options required");
        SQLiteDatabase w=db.getWritableDatabase();
        w.beginTransaction();
        String cycleId="FC-"+UUID.randomUUID().toString();
        try{
            try(Cursor c=w.rawQuery("SELECT status FROM field_sessions WHERE field_session_id=?",
                    new String[]{id})){
                if(!c.moveToFirst()||!FieldSessionRepository.STATUS_ACTIVE.equals(c.getString(0)))
                    throw new IllegalStateException("Field session must be active");
            }
            // Only one field cycle can own the physical acquisition system at any time.
            try(Cursor c=w.rawQuery("SELECT 1 FROM field_measurement_cycles "+
                    "WHERE state IN ('ARMED','WARMUP','SAMPLING') LIMIT 1",null)){
                if(c.moveToFirst())throw new IllegalStateException("Another field cycle is active");
            }
            int next;
            try(Cursor c=w.rawQuery("SELECT COALESCE(MAX(sequence_no),0)+1 FROM field_measurement_cycles "+
                    "WHERE field_session_id=?",new String[]{id})){
                if(!c.moveToFirst())throw new IllegalStateException("Cannot allocate sequence");
                next=c.getInt(0);
            }
            String now=utc();
            ContentValues v=new ContentValues();
            v.put("cycle_id",cycleId);v.put("field_session_id",id);
            v.put("sequence_no",next);v.put("attempt_no",predecessor==null?1:predecessor.attempt+1);
            if(predecessor!=null)v.put("supersedes_cycle_id",predecessor.id);
            v.put("include_gas",options.gas?1:0);v.put("include_particles",options.particles?1:0);
            v.put("include_physical",options.physical?1:0);v.put("state",ARMED);
            v.put("started_ts",now);v.put("updated_ts",now);
            w.insertOrThrow("field_measurement_cycles",null,v);
            w.setTransactionSuccessful();
        }finally{w.endTransaction();}
        return byId(cycleId);
    }

    /** The relay was actually observed ON (not just requested by Android). */
    public boolean relayConfirmed(String cycleId,long monotonicMs,String deviceId){
        if(monotonicMs<=0||deviceId==null||deviceId.trim().isEmpty())return false;
        ContentValues v=new ContentValues();v.put("state",WARMUP);
        v.put("warmup_started_ms",monotonicMs);v.put("device_id",deviceId.trim());v.put("updated_ts",utc());
        return db.getWritableDatabase().update("field_measurement_cycles",v,
                "cycle_id=? AND state=? AND include_gas=1",
                new String[]{required(cycleId),ARMED})==1;
    }
    /** Non-gas operation needs no MQ relay; its timing is still documented. */
    public boolean startedWithoutGas(String cycleId,long monotonicMs,String deviceId){
        if(monotonicMs<=0||deviceId==null||deviceId.trim().isEmpty())return false;
        ContentValues v=new ContentValues();v.put("state",WARMUP);
        v.put("warmup_started_ms",monotonicMs);v.put("device_id",deviceId.trim());v.put("updated_ts",utc());
        return db.getWritableDatabase().update("field_measurement_cycles",v,
                "cycle_id=? AND state=? AND include_gas=0",
                new String[]{required(cycleId),ARMED})==1;
    }
    public boolean samplingStarted(String cycleId,long monotonicMs){
        Cycle cycle=byId(cycleId);
        if(cycle==null||!WARMUP.equals(cycle.state))return false;
        ContentValues v=new ContentValues();v.put("state",SAMPLING);
        v.put("sampling_started_ms",monotonicMs);v.put("updated_ts",utc());
        return db.getWritableDatabase().update("field_measurement_cycles",v,
                "cycle_id=? AND state=? AND warmup_started_ms>0 AND ?-warmup_started_ms>=60000",
                new String[]{required(cycleId),WARMUP,Long.toString(monotonicMs)})==1;
    }

    public static final class Timing {
        public final String state;public final long warmupStart,sampleStart;
        Timing(String state,long warmupStart,long sampleStart){
            this.state=state;this.warmupStart=warmupStart;this.sampleStart=sampleStart;
        }
    }
    public Timing timing(String id){
        try(Cursor c=db.getReadableDatabase().rawQuery(
                "SELECT state,warmup_started_ms,sampling_started_ms FROM field_measurement_cycles "+
                "WHERE cycle_id=?",new String[]{required(id)})){
            return c.moveToFirst()?new Timing(c.getString(0),c.isNull(1)?0:c.getLong(1),
                    c.isNull(2)?0:c.getLong(2)):null;
        }
    }

    /** Finalized telemetry must be a REAL, fresh, successful user acquisition. */
    public boolean complete(String cycleId,String deviceId){
        String cid=required(cycleId);SQLiteDatabase w=db.getWritableDatabase();w.beginTransaction();
        try{
            try(Cursor c=w.rawQuery(
                "SELECT c.measurement_session_id,c.device_id,m.ended_ts,m.operator_role,m.kind,m.analytics,m.payload,"+
                "(SELECT COUNT(*) FROM telemetry_samples t WHERE t.session_id=c.measurement_session_id),c.sampling_started_ms,"+
                "c.include_gas,c.include_physical,c.include_particles "+
                "FROM field_measurement_cycles c LEFT JOIN measurement_sessions m "+
                "ON m.session_id=c.measurement_session_id WHERE c.cycle_id=? AND c.state=?",
                new String[]{cid,SAMPLING})){
                if(!c.moveToFirst())return false;
                String ms=nz(c.getString(0)),owner=nz(c.getString(1));
                if(ms.isEmpty()||owner.isEmpty()||!owner.equals(deviceId)||
                    nz(c.getString(2)).isEmpty()||!"USER".equals(c.getString(3))||
                    !nz(c.getString(4)).startsWith("field_")||c.getInt(7)<1||
                    !FieldSessionApprovalPolicy.completedSuccessfully(nz(c.getString(5)),nz(c.getString(6)))||
                    c.isNull(8)||c.getLong(8)<=0||
                    android.os.SystemClock.elapsedRealtime()-c.getLong(8)<60000L ||
                    !hasMeasuredEvidence(w,ms,c.getInt(9)!=0,c.getInt(10)!=0,c.getInt(11)!=0)||
                    !hasRequiredChemCoreDecision(ms,c.getInt(9)!=0))
                    return false;
                String fingerprint=evidenceDigest(w,ms,deviceId,c.getString(5),c.getString(6));
                ContentValues v=new ContentValues();v.put("state",REVIEW);
                v.put("evidence_sha256",fingerprint);v.put("completed_ts",utc());v.put("updated_ts",utc());
                int updated=w.update("field_measurement_cycles",v,"cycle_id=? AND state=?",
                        new String[]{cid,SAMPLING});
                if(updated==1){w.setTransactionSuccessful();return true;}
            }
            return false;
        }finally{w.endTransaction();}
    }
    /** Review-to-approval is atomic and re-hashes the entire evidence sequence. */
    public boolean approve(String cycleId){
        String id=required(cycleId);SQLiteDatabase w=db.getWritableDatabase();w.beginTransaction();
        try{
            String ms,device,hash,metadata,payload;
            try(Cursor c=w.rawQuery(
                "SELECT c.measurement_session_id,c.device_id,c.evidence_sha256,"+
                "m.analytics,m.payload FROM field_measurement_cycles c "+
                "JOIN measurement_sessions m ON m.session_id=c.measurement_session_id "+
                "WHERE c.cycle_id=? AND c.state=?",
                new String[]{id,REVIEW})){
                if(!c.moveToFirst())return false;
                ms=nz(c.getString(0));device=nz(c.getString(1));hash=nz(c.getString(2));
                metadata=nz(c.getString(3));payload=nz(c.getString(4));
            }
            if(ms.isEmpty()||device.isEmpty()||hash.length()!=64||
                !FieldSessionApprovalPolicy.completedSuccessfully(metadata,payload)||
                !hash.equalsIgnoreCase(evidenceDigest(w,ms,device,metadata,payload)))return false;
            ContentValues v=new ContentValues();v.put("state",APPROVED);
            v.put("approved_ts",utc());v.put("updated_ts",utc());
            int n=w.update("field_measurement_cycles",v,
                "cycle_id=? AND state=? AND measurement_session_id=?",
                new String[]{id,REVIEW,ms});
            if(n==1)w.setTransactionSuccessful();
            return n==1;
        }finally{w.endTransaction();}
    }

    /**
     * Retry and supersession are a single transaction. Never leave a closed
     * attempt superseded if the replacement cannot be inserted.
     */
    public Cycle retry(String cycleId){
        Cycle old=byId(cycleId);
        if(old==null||!(REVIEW.equals(old.state)||FAILED.equals(old.state)||CANCELLED.equals(old.state)))
            throw new IllegalStateException("Only review/failed/cancelled attempts may be retried");
        SQLiteDatabase w=db.getWritableDatabase();w.beginTransaction();
        String nextId="FC-"+UUID.randomUUID().toString();
        try{
            try(Cursor c=w.rawQuery("SELECT status FROM field_sessions WHERE field_session_id=?",
                    new String[]{old.fieldSessionId})){
                if(!c.moveToFirst()||!FieldSessionRepository.STATUS_ACTIVE.equals(c.getString(0)))
                    throw new IllegalStateException("Field session must be active for retry");
            }
            try(Cursor c=w.rawQuery("SELECT 1 FROM field_measurement_cycles "+
                    "WHERE state IN ('ARMED','WARMUP','SAMPLING') LIMIT 1",null)){
                if(c.moveToFirst())throw new IllegalStateException("Physical acquisition already reserved");
            }
            int next;
            try(Cursor c=w.rawQuery("SELECT COALESCE(MAX(sequence_no),0)+1 FROM field_measurement_cycles "+
                    "WHERE field_session_id=?",new String[]{old.fieldSessionId})){
                if(!c.moveToFirst())throw new IllegalStateException("Sequence allocation failed");
                next=c.getInt(0);
            }
            ContentValues replacement=new ContentValues();String now=utc();
            replacement.put("cycle_id",nextId);replacement.put("field_session_id",old.fieldSessionId);
            replacement.put("sequence_no",next);replacement.put("attempt_no",old.attempt+1);
            replacement.put("supersedes_cycle_id",old.id);replacement.put("include_gas",old.modes.gas?1:0);
            replacement.put("include_particles",old.modes.particles?1:0);
            replacement.put("include_physical",old.modes.physical?1:0);
            replacement.put("state",ARMED);replacement.put("started_ts",now);replacement.put("updated_ts",now);
            w.insertOrThrow("field_measurement_cycles",null,replacement);
            ContentValues predecessor=new ContentValues();predecessor.put("state",SUPERSEDED);
            predecessor.put("updated_ts",now);
            if(w.update("field_measurement_cycles",predecessor,"cycle_id=? AND state=?",
                    new String[]{old.id,old.state})!=1)
                throw new IllegalStateException("Concurrent change to the predecessor attempt");
            w.setTransactionSuccessful();
        }finally{w.endTransaction();}
        return byId(nextId);
    }
    /**
     * User delete of an unapproved, terminal attempt. Preserve its sequence and
     * supersession tombstone, while purging only the attached USER telemetry.
     * An approved cycle and any scientific/reference capture are untouchable.
     */
    public boolean deleteUnapproved(String cycleId){
        String id=required(cycleId);SQLiteDatabase w=db.getWritableDatabase();w.beginTransaction();
        try{
            String ms,kind="",role="";
            try(Cursor c=w.rawQuery(
                "SELECT c.measurement_session_id,m.kind,m.operator_role "+
                "FROM field_measurement_cycles c LEFT JOIN measurement_sessions m "+
                "ON m.session_id=c.measurement_session_id "+
                "WHERE c.cycle_id=? AND c.state IN ('FAILED','CANCELLED','SUPERSEDED','REVIEW_READY')",
                new String[]{id})){
                if(!c.moveToFirst())return false;
                ms=nz(c.getString(0));kind=nz(c.getString(1));role=nz(c.getString(2));
            }
            if(!ms.isEmpty()){
                if(!"USER".equals(role)||!kind.startsWith("field_"))
                    throw new IllegalStateException("Refusing non-user measurement deletion");
                try(Cursor c=w.rawQuery(
                    "SELECT 1 FROM experiments e LEFT JOIN reference_queue q ON q.experiment_id=e.id "+
                    "WHERE e.session_id=? AND (e.approved<>0 OR e.approved_ts IS NOT NULL OR "+
                    "e.reference_kind IS NOT NULL OR q.experiment_id IS NOT NULL) LIMIT 1",
                    new String[]{ms})){if(c.moveToFirst())
                        throw new IllegalStateException("Scientific reference exists");}
                try(Cursor c=w.rawQuery("SELECT 1 FROM captures WHERE session_id=? LIMIT 1",
                    new String[]{ms})){if(c.moveToFirst())
                        throw new IllegalStateException("Scientific capture exists");}
                w.delete("field_session_measurements","measurement_session_id=?",new String[]{ms});
                for(String table:new String[]{"telemetry_samples","events","chemcore_decisions",
                                             "media_assets","chemcore_channel_timing"}){
                    w.delete(table,"session_id=?",new String[]{ms});
                }
                w.delete("experiments","session_id=?",new String[]{ms});
                w.delete("measurement_sessions","session_id=?",new String[]{ms});
            }
            ContentValues tomb=new ContentValues();
            tomb.put("state",DELETED);
            tomb.putNull("measurement_session_id");
            tomb.putNull("evidence_sha256");
            tomb.put("fail_reason","DELETED_BY_USER_AT_"+utc());
            tomb.put("updated_ts",utc());
            int count=w.update("field_measurement_cycles",tomb,
                "cycle_id=? AND state IN ('FAILED','CANCELLED','SUPERSEDED','REVIEW_READY')",
                new String[]{id});
            if(count==1)w.setTransactionSuccessful();
            return count==1;
        }finally{w.endTransaction();}
    }

    public boolean fail(String cycleId,String reason){
        String cid=required(cycleId);String message=reason==null?"FAILED":reason.trim();
        ContentValues v=new ContentValues();v.put("state",FAILED);v.put("fail_reason",message);
        v.put("updated_ts",utc());
        return db.getWritableDatabase().update("field_measurement_cycles",v,
            "cycle_id=? AND state IN ('ARMED','WARMUP','SAMPLING')",new String[]{cid})==1;
    }
    public boolean cancel(String cycleId){
        String cid=required(cycleId);ContentValues v=new ContentValues();v.put("state",CANCELLED);v.put("updated_ts",utc());
        return db.getWritableDatabase().update("field_measurement_cycles",v,
            "cycle_id=? AND state IN ('ARMED','WARMUP','SAMPLING','REVIEW_READY')",new String[]{cid})==1;
    }

    /**
     * Require genuine stored sensor observations; a full timer with stale,
     * empty or fabricated zero-value snapshots is not an accepted field result.
     * Minimum 10 independent poll records per selected measurement family.
     */
    /** A documented gas cycle must carry a persisted ChemCore screening decision
     *  (including INSUFFICIENT_EVIDENCE), never merely a timed raw sample sequence.
     *  Physical/particle-only cycles do not manufacture a gas identification.
     */
    private boolean hasRequiredChemCoreDecision(String measurementId,boolean gas){
        if(!gas)return true;
        try{
            return new com.servoc.app.chemcore.ChemCoreDecisionRepository(db)
                .latestPayloadForSession(measurementId)!=null;
        }catch(Exception ex){return false;}
    }

    private static boolean hasMeasuredEvidence(SQLiteDatabase w,String measurementId,
                                               boolean gas,boolean physical,boolean particles){
        int physicalN=0,particleN=0;
        java.util.Set<Long> gasScanCycles=new java.util.HashSet<>();
        try(Cursor c=w.rawQuery("SELECT mq_payload,env_payload,motion_payload,t_ms "+
                "FROM telemetry_samples WHERE session_id=? ORDER BY t_ms",
                new String[]{measurementId})){
            while(c.moveToNext()){
                try{
                    if(gas){
                        JSONObject mq=new JSONObject(nz(c.getString(0)));
                        java.util.Iterator<String> keys=mq.keys();
                        while(keys.hasNext()){
                            JSONObject v=mq.optJSONObject(keys.next());
                            if(v!=null&&v.optBoolean("ok",false)&&
                                v.optBoolean("powered",false)&&
                                Double.isFinite(v.optDouble("rs",Double.NaN))&&
                                v.optDouble("rs",0)>0&&v.has("scan_cycle")&&
                                v.optLong("scan_cycle",-1)>=0){
                                gasScanCycles.add(v.optLong("scan_cycle",-1));break;
                            }
                        }
                    }
                    if(physical){
                        JSONObject env=new JSONObject(nz(c.getString(1)));
                        if(env.optBoolean("ok",false)&&
                            Double.isFinite(env.optDouble("temperature",Double.NaN))&&
                            env.optLong("last_ok_age_ms",Long.MAX_VALUE)<=5000L)physicalN++;
                    }
                    if(particles){
                        JSONObject aux=new JSONObject(nz(c.getString(2)));
                        JSONObject pm=aux.optJSONObject("particles");
                        if(pm!=null&&pm.optBoolean("ok",false))particleN++;
                    }
                }catch(Exception ignored){/* malformed samples count as invalid */ }
            }
        }
        return (!gas||gasScanCycles.size()>=10)&&(!physical||physicalN>=10)&&
               (!particles||particleN>=10);
    }

    private static String required(String id){if(id==null||id.trim().isEmpty())throw new IllegalArgumentException("ID required");return id.trim();}
    private static String nz(String x){return x==null?"":x;}
    private static String utc(){SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",Locale.US);f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(new Date());}
    /**
     * Bind review evidence to the actual stored telemetry rows, not just the
     * final instantaneous snapshot. Stable canonical ordering is by t_ms/id.
     */
    private static String evidenceDigest(SQLiteDatabase w,String measurementId,
                                         String deviceId,String analytics,String payload){
        try{
            MessageDigest d=MessageDigest.getInstance("SHA-256");
            add(d,measurementId);add(d,deviceId);add(d,analytics);add(d,payload);
            try(Cursor c=w.rawQuery(
                "SELECT t_ms,env_payload,mq_payload,motion_payload FROM telemetry_samples "+
                "WHERE session_id=? ORDER BY t_ms,id",new String[]{measurementId})){
                while(c.moveToNext()){
                    add(d,Long.toString(c.getLong(0)));
                    for(int i=1;i<4;i++)add(d,nz(c.getString(i)));
                }
            }
            // The associated ChemCore decision is part of the scientific
            // evidence consumed by session and site reports. Freeze it together
            // with archived telemetry so later edits fail the approval hash.
            try(Cursor decisions=w.rawQuery(
                    "SELECT id,t_ms,payload FROM chemcore_decisions "+
                    "WHERE session_id=? ORDER BY t_ms,id",
                    new String[]{measurementId})){
                while(decisions.moveToNext()){
                    add(d,Long.toString(decisions.getLong(0)));
                    add(d,Long.toString(decisions.getLong(1)));
                    add(d,nz(decisions.getString(2)));
                }
            }
            StringBuilder out=new StringBuilder();
            for(byte b:d.digest())out.append(String.format(Locale.US,"%02x",b&255));
            return out.toString();
        }catch(Exception ex){throw new IllegalStateException("Cannot hash field evidence",ex);}
    }
    private static void add(MessageDigest d,String value){
        byte[] b=(value==null?"":value).getBytes(StandardCharsets.UTF_8);
        d.update((byte)((b.length>>>24)&255));
        d.update((byte)((b.length>>>16)&255));
        d.update((byte)((b.length>>>8)&255));
        d.update((byte)(b.length&255));
        d.update(b);
    }

    private static String sha256(String data){
        try{
            byte[] bytes=MessageDigest.getInstance("SHA-256").digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder out=new StringBuilder();for(byte b:bytes)out.append(String.format(Locale.US,"%02x",b&255));
            return out.toString();
        }catch(Exception ex){throw new IllegalStateException("SHA-256 unavailable",ex);}
    }
}
