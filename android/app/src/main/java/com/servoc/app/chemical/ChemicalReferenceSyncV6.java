package com.servoc.app.chemical;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.servoc.app.data.ExperimentsDb;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * HazRob Tank V6 scientific reference RECORD SYNC (not replacement of Stable).
 *
 * A small GitHub index lists only UID/CAS/checksum of changed reference records.
 * Per-record HTTPS requests and SHA-256 authorize only a source-labelled,
 * queryable SQLite research overlay. Neither a synced record nor a claimed
 * reference URL can upgrade Tank G3, R0, sensor curves, gas alarms, or ppm.
 *
 * The app's offline 652-identity Stable package stays intact. Historical UIDs,
 * experiment FKs, scientific authority tables and user sessions are not edited.
 */
public final class ChemicalReferenceSyncV6 {
    public static final String SCHEMA="hazrob-tank-reference-delta-v6/1";
    public static final String DEFAULT_INDEX=
        "https://raw.githubusercontent.com/HazRob/HazRob-Tank/main/updates/chemcore-records/v6/index.json";
    private static final int INDEX_LIMIT=256*1024;
    private static final int RECORD_LIMIT=16*1024;
    private static final int MAX_BATCH=25;
    private ChemicalReferenceSyncV6(){}

    public static final class Result {
        public final int candidates,unchanged,applied;
        public final String revision;
        public Result(int c,int u,int a,String r){candidates=c;unchanged=u;applied=a;revision=r;}
        public String summary(){return SCHEMA+":revision="+revision+
            ":candidates="+candidates+":unchanged="+unchanged+":updated="+applied;}
    }

    public static void ensure(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS chemical_reference_updates_v6 ("+
            "chemical_uid INTEGER NOT NULL,cas TEXT NOT NULL,"+
            "property_key TEXT NOT NULL,source_id TEXT NOT NULL,"+
            "source_url TEXT NOT NULL,unit TEXT,"+
            "value_text TEXT,value_numeric REAL,"+
            "record_sha256 TEXT NOT NULL,publication_revision TEXT NOT NULL,"+
            "evidence_status TEXT NOT NULL,"+
            "PRIMARY KEY(chemical_uid,property_key,source_id),"+
            "FOREIGN KEY(chemical_uid) REFERENCES chemicals(uid))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_v6_remote_facts_uid "+
            "ON chemical_reference_updates_v6(chemical_uid)");
        db.execSQL("CREATE TABLE IF NOT EXISTS chemical_reference_sync_receipts_v6 ("+
            "chemical_uid INTEGER PRIMARY KEY,cas TEXT NOT NULL,"+
            "sha256 TEXT NOT NULL,publication_revision TEXT NOT NULL)");
    }

    private static String sha256(byte[] b)throws Exception {
        byte[] digest=MessageDigest.getInstance("SHA-256").digest(b);
        StringBuilder out=new StringBuilder(64);
        for(byte v:digest)out.append(String.format(Locale.US,"%02x",v&0xff));
        return out.toString();
    }
    private static byte[] fetch(URL u,int limit)throws Exception {
        if(!"https".equalsIgnoreCase(u.getProtocol())||
            !"raw.githubusercontent.com".equalsIgnoreCase(u.getHost())||
            u.getPort()!=-1||u.getUserInfo()!=null||u.getQuery()!=null||u.getRef()!=null||
            !u.getPath().startsWith("/HazRob/HazRob-Tank/")||
            u.getPath().contains("..")||u.getPath().contains("%")||
            u.getPath().contains("//"))
            throw new IllegalArgumentException("reference queries require exact HazRob GitHub raw HTTPS");
        HttpURLConnection c=(HttpURLConnection)u.openConnection();
        c.setConnectTimeout(7000);c.setReadTimeout(16000);
        c.setInstanceFollowRedirects(false);
        c.setUseCaches(false);
        try{
            if(c.getResponseCode()!=200)throw new IllegalStateException("GitHub record HTTP "+c.getResponseCode());
            if(c.getContentLengthLong()>limit)throw new IllegalStateException("record exceeds limit");
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            try(InputStream in=c.getInputStream()){
                byte[] buf=new byte[4096];int n;
                while((n=in.read(buf))!=-1){
                    if(n==0)continue;
                    if(bytes.size()+n>limit)throw new IllegalStateException("reference too large");
                    bytes.write(buf,0,n);
                }
            }
            return bytes.toByteArray();
        }finally{c.disconnect();}
    }
    private static boolean casValid(String cas){
        if(cas==null||!cas.matches("[0-9]{2,7}-[0-9]{2}-[0-9]"))return false;
        String digits=cas.replace("-","");
        int sum=0,m=1;
        for(int i=digits.length()-2;i>=0;i--)sum+=(digits.charAt(i)-'0')*m++;
        return sum%10==digits.charAt(digits.length()-1)-'0';
    }
    private static String localCas(SQLiteDatabase db,long uid) {
        try(Cursor c=db.rawQuery("SELECT cas FROM chemicals WHERE uid=? AND active=1",
                new String[]{Long.toString(uid)})){
            return c.moveToFirst()&&!c.isNull(0)?c.getString(0):"";
        }
    }
    /** Only the preverified NIOSH alias table may resolve a historical UID. */
    private static long canonicalForQuery(SQLiteDatabase db,long uid){
        if(uid<=0)return uid;
        try(Cursor c=db.rawQuery(
            "SELECT canonical_uid FROM chemical_identity_equivalence_v6 "+
            "WHERE alias_uid=? AND evidence_status='NIOSH_SYNONYM_VERIFIED' LIMIT 1",
            new String[]{Long.toString(uid)})){
            return c.moveToFirst()?c.getLong(0):uid;
        }catch(Exception ignored){return uid;}
    }

    private static String currentSha(SQLiteDatabase db,long uid){
        try(Cursor c=db.rawQuery(
                "SELECT sha256 FROM chemical_reference_sync_receipts_v6 WHERE chemical_uid=?",
                new String[]{Long.toString(uid)})){
            return c.moveToFirst()?c.getString(0):"";
        }
    }
    private static String required(JSONObject j,String key,int max){
        String s=j.optString(key,"").trim();
        if(s.isEmpty()||s.length()>max)throw new IllegalArgumentException("invalid "+key);
        return s;
    }
    private static void applyVerified(SQLiteDatabase db,JSONObject doc,
                                      long uid,String cas,String digest,String revision)throws Exception{
        if(doc.optLong("uid",-1)!=uid||!cas.equals(doc.optString("cas")))
            throw new IllegalArgumentException("GitHub UID/CAS identity drift");
        // Central library has ALREADY completed scientific qualification before
        // publication. Mobile performs only record-identity/hash/schema checks.
        // A complete per-UID record replaces the previous cached capabilities,
        // including a source's removal or withdrawal of an old capability.
        db.delete("chemical_reference_updates_v6","chemical_uid=?",
            new String[]{Long.toString(uid)});
        JSONArray facts=doc.optJSONArray("facts");
        if(facts==null||facts.length()>15)
            throw new IllegalArgumentException("invalid source fact count");
        for(int i=0;i<facts.length();i++){
            JSONObject fact=facts.getJSONObject(i);
            String key=required(fact,"key",40);
            if(!("molar_mass".equals(key)||"formula".equals(key)||
                 "reference_note".equals(key)||
                 "fingerprint_reference".equals(key)||
                 "mq_curve_reference".equals(key)||
                 "ppm_reference".equals(key)||
                 "m4_reference".equals(key)))
                throw new IllegalArgumentException("unsafe scientific field: "+key);
            String authority=required(fact,"source_id",50);
            // Central ChemCore has already qualified each publication.
            // Source ID is metadata, NOT an Android authority allow-list.
            // New source organizations or methods need no APK upgrade.
            String sourceUrl=required(fact,"source_url",300);
            // Only transport URL hygiene. The scientifically approved
            // provenance is decided by the library before publication.
            java.net.URL cited=new java.net.URL(sourceUrl);
            if(!"https".equalsIgnoreCase(cited.getProtocol())||
                cited.getUserInfo()!=null)
                throw new IllegalArgumentException("invalid HTTPS source metadata");
            double number=Double.NaN;
            String text=null;
            String unit="";
            // No second peer review, curve qualification, M4 approval or
            // concentration calibration is run by the Android sync client.
            // The central library publishes the capability's authoritative
            // availability state; a phone only mirrors that state.
            String libraryStatus=fact.optString("library_status","AVAILABLE");
            if(!"AVAILABLE".equals(libraryStatus)&&
               !"QUALIFIED".equals(libraryStatus))
                throw new IllegalArgumentException("unknown published capability state");
            if("molar_mass".equals(key)){
                number=fact.optDouble("numeric",Double.NaN);
                if(!Double.isFinite(number)||number<=0||number>=100000)
                    throw new IllegalArgumentException("molecular weight missing or invalid");
                unit=required(fact,"unit",16);
                if(!"g/mol".equals(unit))throw new IllegalArgumentException("invalid mass unit");
            }else{
                text=required(fact,"text",250);
            }
            ContentValues row=new ContentValues();
            row.put("chemical_uid",uid);row.put("cas",cas);
            row.put("property_key",key);row.put("source_id",authority);
            row.put("source_url",sourceUrl);row.put("unit",unit);
            if(Double.isFinite(number))row.put("value_numeric",number);else row.putNull("value_numeric");
            if(text!=null)row.put("value_text",text);else row.putNull("value_text");
            row.put("record_sha256",digest);row.put("publication_revision",revision);
            row.put("evidence_status","PUBLISHED_LIBRARY_"+libraryStatus);
            db.insertWithOnConflict("chemical_reference_updates_v6",null,row,
                SQLiteDatabase.CONFLICT_REPLACE);
        }
        ContentValues receipt=new ContentValues();
        receipt.put("chemical_uid",uid);receipt.put("cas",cas);
        receipt.put("sha256",digest);receipt.put("publication_revision",revision);
        db.insertWithOnConflict("chemical_reference_sync_receipts_v6",null,receipt,
            SQLiteDatabase.CONFLICT_REPLACE);
    }

    /** Trigger from a worker thread. Small per-UID lookups; never download Stable. */
    public static Result refresh(ExperimentsDb repository,long requestedUid,int maxChanges,
                                 boolean analysisActive)throws Exception {
        if(repository==null||requestedUid<0||maxChanges<1||maxChanges>MAX_BATCH)
            throw new IllegalArgumentException("invalid synchronization request");
        if(analysisActive)throw new IllegalStateException("synchronization paused during analysis");
        SQLiteDatabase db=repository.getWritableDatabase();
        ensure(db);
        // A legacy UID may use a proven NIOSH synonym of a canonical CAS-
        // bearing UID. Query the canonical record without rewriting sessions.
        requestedUid=canonicalForQuery(db,requestedUid);
        URL indexUrl=new URL(DEFAULT_INDEX);
        JSONObject index=new JSONObject(new String(fetch(indexUrl,INDEX_LIMIT),StandardCharsets.UTF_8));
        if(!SCHEMA.equals(index.optString("schema")))throw new IllegalArgumentException("unknown index schema");
        String revision=required(index,"revision",80);
        JSONArray entries=index.optJSONArray("records");
        if(entries==null||entries.length()>1000)
            throw new IllegalArgumentException("unbounded reference index");
        Set<Long> seen=new HashSet<>();
        int candidates=0,unchanged=0,applied=0;
        for(int i=0;i<entries.length();i++){
            JSONObject entry=entries.getJSONObject(i);
            long uid=entry.optLong("uid",-1);
            if(uid<=0||!seen.add(uid))throw new IllegalArgumentException("duplicate/invalid reference UID");
            if(requestedUid!=0&&uid!=requestedUid)continue;
            String cas=required(entry,"cas",40);
            String digest=required(entry,"sha256",64);
            String file=required(entry,"file",64);
            if(!casValid(cas)||!digest.matches("[0-9a-f]{64}")||
                !file.matches("[0-9]{1,8}\\.json")||!file.equals(uid+".json"))
                throw new IllegalArgumentException("invalid reference CAS/hash/path");
            // Only source-matched, already known individual chemical records.
            if(!cas.equals(localCas(db,uid)))continue;
            // Skip already-current UIDs BEFORE applying the batch budget.
            // Otherwise a 734-record index with the first 25 unchanged would
            // permanently starve later changed compounds.
            if(digest.equals(currentSha(db,uid))){unchanged++;continue;}
            if(candidates>=maxChanges)break;
            candidates++;
            String root=indexUrl.toString().substring(0,indexUrl.toString().lastIndexOf('/')+1);
            byte[] bytes=fetch(new URL(root+"records/"+file),RECORD_LIMIT);
            if(!digest.equals(sha256(bytes)))throw new IllegalStateException("GitHub record SHA256 mismatch");
            JSONObject doc=new JSONObject(new String(bytes,StandardCharsets.UTF_8));
            db.beginTransaction();
            try{
                applyVerified(db,doc,uid,cas,digest,revision);
                db.setTransactionSuccessful();
            }finally{db.endTransaction();}
            applied++;
        }
        if(applied>0)repository.putMeta("v6_reference_query_revision",revision);
        return new Result(candidates,unchanged,applied,revision);
    }

    /**
     * Fully offline, SHA-checked reference-delta test/import gate, used by the
     * Android emulator and by future independent record reconciliation.
     * No actual claimed QA status is inferred from SHA verification.
     */
    public static void applyVerifiedRecord(ExperimentsDb repository,byte[] record,
                                           String expectedSha,String revision)throws Exception{
        if(repository==null||record==null||record.length==0||
            record.length>RECORD_LIMIT||expectedSha==null||
            !expectedSha.matches("[0-9a-f]{64}")||
            !expectedSha.equals(sha256(record)))
            throw new IllegalArgumentException("record integrity not verified");
        SQLiteDatabase db=repository.getWritableDatabase();
        ensure(db);
        JSONObject doc=new JSONObject(new String(record,StandardCharsets.UTF_8));
        long uid=doc.optLong("uid",-1);
        String cas=required(doc,"cas",40);
        if(uid<=0||!casValid(cas)||!cas.equals(localCas(db,uid)))
            throw new IllegalArgumentException("local UID/CAS mismatch");
        db.beginTransaction();
        try{
            applyVerified(db,doc,uid,cas,expectedSha,required(
                new JSONObject().put("revision",revision),"revision",80));
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
        repository.putMeta("v6_reference_query_revision",revision);
    }

    /** Available to the existing compound detail query without network access. */
    public static JSONArray facts(ExperimentsDb source,long uid){
        JSONArray out=new JSONArray();
        if(source==null||uid<=0)return out;
        SQLiteDatabase db=source.getReadableDatabase();
        try{
            ensure(db);
            long canonical=canonicalForQuery(db,uid);
            try(Cursor c=db.rawQuery(
                    "SELECT property_key,source_id,source_url,unit,value_text,value_numeric,"+
                    "publication_revision,evidence_status,chemical_uid "+
                    "FROM chemical_reference_updates_v6 "+
                    "WHERE chemical_uid=? OR chemical_uid=? "+
                    "ORDER BY property_key,source_id LIMIT 30",
                    new String[]{Long.toString(uid),Long.toString(canonical)})){
                while(c.moveToNext()){
                    JSONObject f=new JSONObject();
                    f.put("key",c.getString(0));f.put("source_id",c.getString(1));
                    f.put("source_url",c.getString(2));f.put("unit",c.getString(3));
                    f.put("text",c.isNull(4)?JSONObject.NULL:c.getString(4));
                    f.put("numeric",c.isNull(5)?JSONObject.NULL:c.getDouble(5));
                    f.put("revision",c.getString(6));f.put("evidence_status",c.getString(7));
                    f.put("published_for_uid",c.getLong(8));
                    f.put("displayed_via_verified_alias",c.getLong(8)!=uid);
                    out.put(f);
                }
            }
        }catch(Exception ignored){/* old databases stay readable and offline */}
        return out;
    }
}
