package com.servoc.app.field;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.servoc.app.data.ExperimentsDb;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

/**
 * User-domain repository for sites and field sessions.
 *
 * This class intentionally has no API for experiments, calibration, fingerprints,
 * reference artifacts, models, or other developer/scientific-library records.
 */
public final class FieldSessionRepository {
    public static final String STATUS_ACTIVE="ACTIVE";
    public static final String STATUS_CLOSED="CLOSED";
    public static final String STATUS_APPROVED="APPROVED";

    public static final class Site {
        public final String id,name,notes,createdTs,updatedTs;
        Site(String id,String name,String notes,String createdTs,String updatedTs){
            this.id=id;this.name=name;this.notes=notes;this.createdTs=createdTs;this.updatedTs=updatedTs;
        }
    }

    public static final class FieldSession {
        public final String id,siteId,name,startedTs,endedTs,status;
        FieldSession(String id,String siteId,String name,String startedTs,String endedTs,String status){
            this.id=id;this.siteId=siteId;this.name=name;this.startedTs=startedTs;this.endedTs=endedTs;this.status=status;
        }
        public boolean active(){return STATUS_ACTIVE.equals(status);}
        public boolean approved(){return STATUS_APPROVED.equals(status);}
        public String displayStatus(){return approved()?"معتمدة":"جديدة";}
    }

    private final ExperimentsDb db;
    public FieldSessionRepository(ExperimentsDb db){if(db==null)throw new IllegalArgumentException("db required");this.db=db;}

    public String createSite(String displayName,String notes){
        String name=requiredName(displayName,"site name");
        String id="SITE-"+UUID.randomUUID().toString();
        String now=isoNow();
        ContentValues v=new ContentValues();v.put("site_id",id);v.put("name",name);v.put("notes",notes==null?"":notes.trim());v.put("created_ts",now);v.put("updated_ts",now);
        db.getWritableDatabase().insertOrThrow("sites",null,v);return id;
    }

    public boolean renameSite(String siteId,String displayName){
        ContentValues v=new ContentValues();v.put("name",requiredName(displayName,"site name"));v.put("updated_ts",isoNow());
        return db.getWritableDatabase().update("sites",v,"site_id=?",new String[]{requiredId(siteId)})==1;
    }

    public List<Site> sites(){
        List<Site> out=new ArrayList<>();
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT site_id,name,notes,created_ts,updated_ts FROM sites ORDER BY updated_ts DESC,name COLLATE NOCASE",null)){
            while(c.moveToNext())out.add(new Site(c.getString(0),c.getString(1),nz(c.getString(2)),c.getString(3),c.getString(4)));
        }
        return out;
    }

    public String startFieldSession(String siteId,String displayName){
        String sid=requiredId(siteId);if(!siteExists(sid))throw new IllegalArgumentException("site not found");
        SQLiteDatabase w=db.getWritableDatabase();w.beginTransaction();
        try{
            try(Cursor c=w.rawQuery("SELECT 1 FROM field_sessions WHERE status=? LIMIT 1",new String[]{STATUS_ACTIVE})){
                if(c.moveToFirst())throw new IllegalStateException("another field session is already active");
            }
            String id="FS-"+UUID.randomUUID().toString();String now=isoNow();
            ContentValues v=new ContentValues();v.put("field_session_id",id);v.put("site_id",sid);v.put("name",requiredName(displayName,"field session name"));v.put("started_ts",now);v.put("status",STATUS_ACTIVE);v.put("created_ts",now);v.put("updated_ts",now);
            w.insertOrThrow("field_sessions",null,v);w.setTransactionSuccessful();return id;
        }finally{w.endTransaction();}
    }

    /**
     * Closes only an ACTIVE field session whose explicitly linked measurement sessions have all
     * been finalized. A field container must never close around a still-running acquisition.
     */
    public boolean closeFieldSession(String fieldSessionId){
        String id=requiredId(fieldSessionId);SQLiteDatabase w=db.getWritableDatabase();w.beginTransaction();
        try{
            try(Cursor c=w.rawQuery("SELECT 1 FROM field_sessions WHERE field_session_id=? AND status=? LIMIT 1",new String[]{id,STATUS_ACTIVE})){if(!c.moveToFirst())return false;}
            String openSql="SELECT 1 FROM field_session_measurements fsm JOIN measurement_sessions ms ON ms.session_id=fsm.measurement_session_id WHERE fsm.field_session_id=? AND (ms.ended_ts IS NULL OR TRIM(ms.ended_ts)='') LIMIT 1";
            try(Cursor c=w.rawQuery(openSql,new String[]{id})){if(c.moveToFirst())return false;}
            // A closed Field Session cannot contain an unreviewed V5 measurement.
            try(Cursor c=w.rawQuery("SELECT 1 FROM field_measurement_cycles "+
                    "WHERE field_session_id=? AND state IN ('ARMED','WARMUP','SAMPLING','REVIEW_READY') LIMIT 1",
                    new String[]{id})){if(c.moveToFirst())return false;}
            ContentValues v=new ContentValues();String now=isoNow();v.put("ended_ts",now);v.put("status",STATUS_CLOSED);v.put("updated_ts",now);
            int n=w.update("field_sessions",v,"field_session_id=? AND status=?",new String[]{id,STATUS_ACTIVE});w.setTransactionSuccessful();return n==1;
        }finally{w.endTransaction();}
    }

    /** Approve a CLOSED field session with exactly one complete, actually sampled user acquisition. */
    public boolean approveFieldSession(String fieldSessionId){
        String id=requiredId(fieldSessionId);
        SQLiteDatabase w=db.getWritableDatabase();w.beginTransaction();
        try{
            try(Cursor c=w.rawQuery("SELECT status FROM field_sessions WHERE field_session_id=? LIMIT 1",new String[]{id})){
                if(!c.moveToFirst()||!STATUS_CLOSED.equals(c.getString(0)))return false;
            }
            int complete=0;
            try(Cursor c=w.rawQuery(
                "SELECT ms.session_id,ms.ended_ts,ms.operator_role,ms.kind,ms.analytics,ms.payload FROM field_session_measurements f "+
                "JOIN measurement_sessions ms ON ms.session_id=f.measurement_session_id WHERE f.field_session_id=?",
                new String[]{id})){
                while(c.moveToNext()){
                    complete++;
                    String sid=c.getString(0),ended=nz(c.getString(1)),role=nz(c.getString(2)),kind=nz(c.getString(3));
                    if(!"USER".equals(role)||!kind.startsWith("field_")||ended.trim().isEmpty())return false;
                    if(!FieldSessionApprovalPolicy.completedSuccessfully(nz(c.getString(4)),nz(c.getString(5))))return false;
                    String cycleState="";
                    try(Cursor matching=w.rawQuery(
                            "SELECT state FROM field_measurement_cycles WHERE field_session_id=? AND measurement_session_id=?",
                            new String[]{id,sid})){
                        if(!matching.moveToFirst())return false;
                        cycleState=nz(matching.getString(0));
                    }
                    if("LEGACY_IMPORTED".equals(cycleState)){
                        // Historical V9 records were complete and have the old approval
                        // evidence. Preserve their old semantics without promoting V5 drafts.
                        ContentValues imported=new ContentValues();imported.put("state","LEGACY_APPROVED");imported.put("approved_ts",isoNow());
                        if(w.update("field_measurement_cycles",imported,
                                "field_session_id=? AND measurement_session_id=? AND state='LEGACY_IMPORTED'",
                                new String[]{id,sid})!=1)return false;
                    }else if(!FieldCycleRepository.APPROVED.equals(cycleState)&&!"LEGACY_APPROVED".equals(cycleState))
                        return false;
                    try(Cursor samples=w.rawQuery("SELECT 1 FROM telemetry_samples WHERE session_id=? LIMIT 1",new String[]{sid})){
                        if(!samples.moveToFirst())return false;
                    }
                }
            }
            if(complete<1)return false;
            ContentValues values=new ContentValues();values.put("status",STATUS_APPROVED);values.put("updated_ts",isoNow());
            int n=w.update("field_sessions",values,"field_session_id=? AND status=?",new String[]{id,STATUS_CLOSED});
            if(n==1)w.setTransactionSuccessful();
            return n==1;
        }finally{w.endTransaction();}
    }
    public boolean isApproved(String fieldSessionId){
        FieldSession f=fieldSessionById(fieldSessionId);
        return f!=null&&f.approved();
    }
    public List<FieldSession> approvedSessionsForSite(String siteId){
        List<FieldSession> out=new ArrayList<>();
        try(Cursor c=db.getReadableDatabase().rawQuery(
            "SELECT field_session_id,site_id,name,started_ts,ended_ts,status FROM field_sessions WHERE site_id=? AND status=? ORDER BY started_ts DESC,field_session_id DESC",
            new String[]{requiredId(siteId),STATUS_APPROVED})){
            while(c.moveToNext())out.add(fieldSession(c));
        }
        return out;
    }

    public FieldSession activeFieldSession(){
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT field_session_id,site_id,name,started_ts,ended_ts,status FROM field_sessions WHERE status=? ORDER BY started_ts DESC LIMIT 1",new String[]{STATUS_ACTIVE})){
            return c.moveToFirst()?fieldSession(c):null;
        }
    }

    /** Lightweight identity lookup; report bundles must be resolved OFF the UI thread. */
    public FieldSession fieldSessionById(String fieldSessionId){
        String id=requiredId(fieldSessionId);
        try(Cursor c=db.getReadableDatabase().rawQuery(
            "SELECT field_session_id,site_id,name,started_ts,ended_ts,status FROM field_sessions WHERE field_session_id=? LIMIT 1",
            new String[]{id})){return c.moveToFirst()?fieldSession(c):null;}
    }

    public List<FieldSession> sessionsForSite(String siteId){
        List<FieldSession> out=new ArrayList<>();
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT field_session_id,site_id,name,started_ts,ended_ts,status FROM field_sessions WHERE site_id=? ORDER BY started_ts DESC,field_session_id DESC",new String[]{requiredId(siteId)})){
            while(c.moveToNext())out.add(fieldSession(c));
        }
        return out;
    }

    /**
     * Links only USER/FIELD technical acquisitions. Developer sessions are rejected
     * even if a caller accidentally passes their ID.
     */
    public boolean linkMeasurement(String fieldSessionId,String measurementSessionId){
        String fs=requiredId(fieldSessionId),ms=requiredId(measurementSessionId);
        SQLiteDatabase w=db.getWritableDatabase();w.beginTransaction();
        try{
            try(Cursor c=w.rawQuery("SELECT status FROM field_sessions WHERE field_session_id=?",new String[]{fs})){
                if(!c.moveToFirst()||!STATUS_ACTIVE.equals(c.getString(0)))return false;
            }
            try(Cursor c=w.rawQuery("SELECT kind,operator_role FROM measurement_sessions WHERE session_id=?",new String[]{ms})){
                if(!c.moveToFirst())return false;
                String kind=nz(c.getString(0)),role=nz(c.getString(1));
                if(!FieldSessionPolicy.mayAttachMeasurement(kind,role))return false;
            }
            // V5 forbids implicit field capture: only a user-armed cycle can own it.
            String cycleId="";
            try(Cursor reserved=w.rawQuery(
                    "SELECT cycle_id FROM field_measurement_cycles WHERE field_session_id=? "+
                    "AND measurement_session_id IS NULL AND state IN ('ARMED','WARMUP','SAMPLING') "+
                    "ORDER BY sequence_no DESC LIMIT 1",new String[]{fs})){
                if(!reserved.moveToFirst())return false;
                cycleId=reserved.getString(0);
            }
            ContentValues v=new ContentValues();v.put("field_session_id",fs);v.put("measurement_session_id",ms);v.put("linked_ts",isoNow());
            long row=w.insertWithOnConflict("field_session_measurements",null,v,SQLiteDatabase.CONFLICT_IGNORE);
            if(row<0)return false;
            ContentValues owned=new ContentValues();owned.put("measurement_session_id",ms);owned.put("updated_ts",isoNow());
            if(w.update("field_measurement_cycles",owned,
                    "cycle_id=? AND measurement_session_id IS NULL",new String[]{cycleId})!=1)return false;
            w.setTransactionSuccessful();return true;
        }finally{w.endTransaction();}
    }

    /** Single bounded query for all site child counts, not N expensive calls from a UI redraw. */
    public java.util.Map<String,Integer> measurementCountsForSite(String siteId){
        java.util.Map<String,Integer> results=new java.util.HashMap<>();
        String sql="SELECT f.field_session_id,COUNT(m.measurement_session_id) "+
            "FROM field_sessions f LEFT JOIN field_session_measurements m "+
            "ON m.field_session_id=f.field_session_id "+
            "WHERE f.site_id=? GROUP BY f.field_session_id";
        try(Cursor c=db.getReadableDatabase().rawQuery(sql,new String[]{requiredId(siteId)})){
            while(c.moveToNext())results.put(c.getString(0),c.getInt(1));
        }
        return results;
    }

    /** Gives a readable error if a historical or damaged DB lacks the required v9 tables. */
    public void verifySessionSchema(){
        String[] required={"sites","field_sessions","field_session_measurements","field_measurement_cycles","measurement_sessions","telemetry_samples"};
        java.util.HashSet<String> found=new java.util.HashSet<>();
        try(Cursor c=db.getReadableDatabase().rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name IN ('sites','field_sessions','field_session_measurements','field_measurement_cycles','measurement_sessions','telemetry_samples')",null)){
            while(c.moveToNext())found.add(c.getString(0));
        }
        for(String table:required)if(!found.contains(table))
            throw new IllegalStateException("Field-session database migration incomplete: "+table);
    }

    public List<String> measurementSessionIds(String fieldSessionId){
        List<String> out=new ArrayList<>();
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT measurement_session_id FROM field_session_measurements WHERE field_session_id=? ORDER BY linked_ts,measurement_session_id",new String[]{requiredId(fieldSessionId)})){
            while(c.moveToNext())out.add(c.getString(0));
        }
        return out;
    }

    /** Exact reverse lookup used by SmartReport UI routing; empty means the measurement is not in a user Field Session. */
    public String fieldSessionIdForMeasurement(String measurementSessionId){
        String ms=measurementSessionId==null?"":measurementSessionId.trim();if(ms.isEmpty())return "";
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT field_session_id FROM field_session_measurements WHERE measurement_session_id=? ORDER BY linked_ts DESC,field_session_id DESC LIMIT 1",new String[]{ms})){
            return c.moveToFirst()?nz(c.getString(0)):"";
        }
    }

    /**
     * Permanently deletes a CLOSED user's Field Session and its own measurement data.
     * Developer/reference/scientific libraries, calibrations, and other sessions are excluded.
     * This is atomic in SQLite; caller separately purges portable SAF archives.
     */
    public boolean deleteFieldSession(String fieldSessionId){
        String id=requiredId(fieldSessionId);SQLiteDatabase w=db.getWritableDatabase();w.beginTransaction();
        try{
            try(Cursor c=w.rawQuery("SELECT status FROM field_sessions WHERE field_session_id=? LIMIT 1",new String[]{id})){
                if(!c.moveToFirst()||!STATUS_CLOSED.equals(c.getString(0)))return false;
            }
            java.util.List<String> ids=new java.util.ArrayList<>();
            try(Cursor c=w.rawQuery("SELECT m.session_id,m.kind,m.operator_role FROM measurement_sessions m JOIN field_session_measurements f ON f.measurement_session_id=m.session_id WHERE f.field_session_id=?",new String[]{id})){
                while(c.moveToNext()){
                    String ms=c.getString(0),kind=c.getString(1),role=c.getString(2);
                    if(!"USER".equals(role)||kind==null||!kind.startsWith("field_"))
                        throw new IllegalStateException("Refusing deletion: non-user/reference measurement linked to Field Session");
                    ids.add(ms);
                }
            }
            // Do not permit any referenced/approved capture to be removed through a user UI action.
            for(String ms:ids){
                try(Cursor c=w.rawQuery("SELECT 1 FROM experiments e LEFT JOIN reference_queue q ON q.experiment_id=e.id WHERE e.session_id=? AND (e.approved<>0 OR e.approved_ts IS NOT NULL OR e.reference_kind IS NOT NULL OR q.experiment_id IS NOT NULL) LIMIT 1",new String[]{ms})){
                    if(c.moveToFirst())throw new IllegalStateException("Refusing deletion: published or controlled reference");
                }
                try(Cursor c=w.rawQuery("SELECT 1 FROM captures WHERE session_id=? LIMIT 1",new String[]{ms})){
                    if(c.moveToFirst())throw new IllegalStateException("Refusing deletion: scientific capture uses this identity");
                }
            }
            // Delete V5 cycle references first (including unbound/rejected attempts).
            w.delete("field_measurement_cycles","field_session_id=?",new String[]{id});
            w.delete("field_session_measurements","field_session_id=?",new String[]{id});
            for(String ms:ids){
                String[] arg={ms};
                w.delete("experiments","session_id=?",arg);
                w.delete("events","session_id=?",arg);
                w.delete("chemcore_decisions","session_id=?",arg);
                w.delete("telemetry_samples","session_id=?",arg);
                w.delete("media_assets","session_id=?",arg);
                w.delete("measurement_sessions","session_id=?",arg);
            }
            int n=w.delete("field_sessions","field_session_id=?",new String[]{id});
            if(n!=1)throw new IllegalStateException("Field Session disappeared during deletion");
            ContentValues tombstone=new ContentValues();tombstone.put("k","deleted_field_session_"+id);tombstone.put("v",isoNow());
            w.insertWithOnConflict("meta",null,tombstone,SQLiteDatabase.CONFLICT_REPLACE);
            w.setTransactionSuccessful();return true;
        }finally{w.endTransaction();}
    }

    /** Delete a site only after all of its field sessions have been removed. */
    public boolean deleteEmptySite(String siteId){
        String id=requiredId(siteId);SQLiteDatabase w=db.getWritableDatabase();w.beginTransaction();
        try{
            try(Cursor c=w.rawQuery("SELECT 1 FROM field_sessions WHERE site_id=? LIMIT 1",new String[]{id})){
                if(c.moveToFirst())return false;
            }
            int n=w.delete("sites","site_id=?",new String[]{id});
            if(n!=1)return false;
            ContentValues tombstone=new ContentValues();tombstone.put("k","deleted_site_"+id);tombstone.put("v",isoNow());
            w.insertWithOnConflict("meta",null,tombstone,SQLiteDatabase.CONFLICT_REPLACE);
            w.setTransactionSuccessful();return true;
        }finally{w.endTransaction();}
    }

    private boolean siteExists(String id){try(Cursor c=db.getReadableDatabase().rawQuery("SELECT 1 FROM sites WHERE site_id=? LIMIT 1",new String[]{id})){return c.moveToFirst();}}
    private static FieldSession fieldSession(Cursor c){return new FieldSession(c.getString(0),c.getString(1),c.getString(2),c.getString(3),nz(c.getString(4)),c.getString(5));}
    private static String requiredId(String s){String v=s==null?"":s.trim();if(v.isEmpty())throw new IllegalArgumentException("id required");return v;}
    private static String requiredName(String s,String what){String v=s==null?"":s.trim();if(v.isEmpty())throw new IllegalArgumentException(what+" required");return v;}
    private static String nz(String s){return s==null?"":s;}
    private static String isoNow(){SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",Locale.US);f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(new Date());}
}
