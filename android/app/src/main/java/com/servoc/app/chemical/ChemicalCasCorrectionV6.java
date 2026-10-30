package com.servoc.app.chemical;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.servoc.app.data.ExperimentsDb;

/**
 * Source-audited correction of TWO malformed CAS strings in the V6 imports.
 *
 * 715 Benzonitrile: literal "null" is NOT a CAS (NIST SRD69 100-47-0).
 * 724 Coal tar pitch volatiles: 65966-93-2 checksum fails;
 *     actual NIOSH/OSHA scope-bound mixture CAS is 65996-93-2.
 *
 * Original invalid strings remain in a separate audit table, not in
 * searchable chemical_identifiers. Never infer gas identity or an alarm.
 */
public final class ChemicalCasCorrectionV6 {
    public static final String VERSION="hazrob-tank-v6-audited-cas-repairs/1";
    private ChemicalCasCorrectionV6(){}

    private static final String COAL_NAME=
        "Coal tar pitch volatiles (benzene soluble fraction), anthracene, BaP, phenanthrene, acridine, chrysene, pyrene)";
    private static final String[][] RECORDS={
        {"715","Benzonitrile","null","100-47-0","NIST_WEBBOOK",
         "https://webbook.nist.gov/cgi/cbook.cgi?ID=100-47-0",
         "C7H5N","103.1213"},
        {"724",COAL_NAME,"65966-93-2","65996-93-2","NIOSH_NPG",
         "https://www.cdc.gov/niosh/npg/npgd0145.html",
         "",""}
    };
    public static final class Result {
        public final int inspected,corrected,confirmed,rejected;
        public final String rejectedUids;
        Result(int i,int c,int v,int r,String ids){
            inspected=i;corrected=c;confirmed=v;rejected=r;rejectedUids=ids;
        }
        public String summary(){
            return VERSION+":inspected="+inspected+":corrected="+corrected+
                ":confirmed="+confirmed+":rejected="+rejected+
                ":rejected_uids="+rejectedUids;
        }
    }
    private static boolean validCas(String cas){
        if(cas==null||!cas.matches("[0-9]{2,7}-[0-9]{2}-[0-9]"))return false;
        String s=cas.replace("-","");
        int sum=0;
        for(int i=s.length()-2,w=1;i>=0;i--,w++)
            sum+=(s.charAt(i)-'0')*w;
        return sum%10==(s.charAt(s.length()-1)-'0');
    }
    public static Result ensure(ExperimentsDb source){
        if(source==null)throw new IllegalArgumentException("database required");
        SQLiteDatabase db=source.getWritableDatabase();
        ChemicalIdentitySchema.ensure(db);
        db.execSQL("CREATE TABLE IF NOT EXISTS chemical_cas_correction_audit_v6("+
            "chemical_uid INTEGER PRIMARY KEY, original_invalid_cas TEXT NOT NULL,"+
            "corrected_cas TEXT NOT NULL, source_id TEXT NOT NULL,"+
            "source_url TEXT NOT NULL, status TEXT NOT NULL,"+
            "FOREIGN KEY(chemical_uid) REFERENCES chemicals(uid),"+
            "CHECK(status='SOURCE_VERIFIED_INVALID_CAS_REPAIR'))");
        int corrected=0,confirmed=0,rejected=0;
        StringBuilder reasons=new StringBuilder();
        db.beginTransaction();
        try{
            for(String[] r:RECORDS){
                String uid=r[0],name=r[1],bad=r[2],cas=r[3],
                    authority=r[4],url=r[5],formula=r[6],mw=r[7];
                if(!validCas(cas)){
                    rejected++;reasons.append(uid).append(":source_invalid;");
                    continue;
                }
                try(Cursor c=db.rawQuery(
                        "SELECT name_en,cas,formula,molar_mass FROM chemicals "+
                        "WHERE uid=? AND active=1",new String[]{uid})){
                    if(!c.moveToFirst()||!name.equals(c.getString(0))){
                        rejected++;reasons.append(uid).append(":identity_changed;");
                        continue;
                    }
                    String existing=c.isNull(1)?"":c.getString(1).trim();
                    boolean initial=bad.equals(existing);
                    boolean replay=cas.equals(existing);
                    if(!initial&&!replay){
                        rejected++;reasons.append(uid).append(":cas_conflict;");
                        continue;
                    }
                    String prior=dbValue(db,"SELECT corrected_cas FROM "+
                        "chemical_cas_correction_audit_v6 WHERE chemical_uid=?",
                        uid);
                    if(replay&&(!cas.equals(prior)||
                        !sourceEvidenceExists(db,uid,authority,url,cas))){
                        rejected++;reasons.append(uid).append(":missing_provenance;");
                        continue;
                    }
                    if(!formula.isEmpty()&&!c.isNull(2)&&
                        !formula.equals(c.getString(2))){
                        rejected++;reasons.append(uid).append(":formula_conflict;");
                        continue;
                    }
                    if(!mw.isEmpty()&&!c.isNull(3)&&c.getDouble(3)>0&&
                        Math.abs(c.getDouble(3)-Double.parseDouble(mw))>0.05){
                        rejected++;reasons.append(uid).append(":mass_conflict;");
                        continue;
                    }
                    // Quarantine only the exact two known-invalid CAS
                    // identifiers from the user-facing secondary search
                    // index; original imported tokens remain in the audit
                    // table and are never interpreted as valid identities.
                    db.delete("chemical_identifiers",
                        "chemical_uid=? AND scheme='CAS' AND value=?",
                        new String[]{uid,bad});
                    if(initial){
                        ContentValues cv=new ContentValues();
                        cv.put("cas",cas);
                        if(!formula.isEmpty()&&c.isNull(2))
                            cv.put("formula",formula);
                        if(!mw.isEmpty()&&(c.isNull(3)||c.getDouble(3)<=0))
                            cv.put("molar_mass",Double.parseDouble(mw));
                        if(db.update("chemicals",cv,"uid=?",new String[]{uid})!=1)
                            throw new IllegalStateException("CAS correction failed uid="+uid);
                        ContentValues a=new ContentValues();
                        a.put("chemical_uid",Long.parseLong(uid));
                        a.put("original_invalid_cas",bad);
                        a.put("corrected_cas",cas);
                        a.put("source_id",authority);
                        a.put("source_url",url);
                        a.put("status","SOURCE_VERIFIED_INVALID_CAS_REPAIR");
                        db.insertOrThrow("chemical_cas_correction_audit_v6",null,a);
                        ContentValues sourceFact=new ContentValues();
                        sourceFact.put("chemical_uid",Long.parseLong(uid));
                        sourceFact.put("source_id",authority);
                        sourceFact.put("source_key","V6_INVALID_CAS_REPAIR_UID:"+uid);
                        sourceFact.put("record_url",url);
                        db.insertWithOnConflict("chemical_sources",null,sourceFact,
                            SQLiteDatabase.CONFLICT_IGNORE);
                        ContentValues canonical=new ContentValues();
                        canonical.put("chemical_uid",Long.parseLong(uid));
                        canonical.put("scheme","CAS");
                        canonical.put("value",cas);
                        canonical.put("source_id",authority);
                        db.insertWithOnConflict("chemical_identifiers",null,canonical,
                            SQLiteDatabase.CONFLICT_IGNORE);
                        corrected++;
                    }else confirmed++;
                }
            }
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
        return new Result(RECORDS.length,corrected,confirmed,rejected,reasons.toString());
    }
    private static String dbValue(SQLiteDatabase db,String q,String uid){
        try(Cursor c=db.rawQuery(q,new String[]{uid})){
            return c.moveToFirst()&&!c.isNull(0)?c.getString(0):null;
        }
    }
    private static boolean sourceEvidenceExists(
        SQLiteDatabase db,String uid,String authority,String url,String cas){
        try(Cursor c=db.rawQuery(
            "SELECT 1 FROM chemical_sources s JOIN chemical_identifiers i "+
            "ON i.chemical_uid=s.chemical_uid AND i.source_id=s.source_id "+
            "AND i.scheme='CAS' AND i.value=? "+
            "WHERE s.chemical_uid=? AND s.source_id=? "+
            "AND s.source_key=? AND s.record_url=? LIMIT 1",
            new String[]{cas,uid,authority,"V6_INVALID_CAS_REPAIR_UID:"+uid,url})){
            return c.moveToFirst();
        }
    }
}
