package com.servoc.app.chemical;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.servoc.app.data.ExperimentsDb;

/**
 * V6: source-qualified NON-DESTRUCTIVE reconciliation of historical HazRob
 * material aliases with canonical NIOSH names/CAS on the same master database.
 * Preserve every original UID, historical capture and FK; do not invent CAS
 * on legacy rows or conflate mixtures/families with a single CAS substance.
 *
 * A crosswalk is an IDENTITY statement, not a gas measurement or an
 * occupational limit. Final de-duplicated UI migration remains separately
 * gated until consumers of historical legacy UIDs have been regression tested.
 */
public final class ChemicalIdentityEquivalenceV6 {
    public static final String SCHEMA="hazrob-tank-v6-niosh-alias-equivalence/1";
    private ChemicalIdentityEquivalenceV6(){}

    // Curated name equivalences: old UID, exact old name, canonical NIOSH
    // primary label, CAS of the source-qualified NIOSH row.
    // Each canonical record MUST already be present from the original NIOSH
    // authority seed and carry its original cdc.gov source URL.
    private static final String[][] ALIASES={
        {"9","Germane","Germanium tetrahydride","7782-65-2"},
        {"63","Chloromethane","Methyl chloride","74-87-3"},
        {"64","Dichloromethane","Methylene chloride","75-09-2"},
        {"67","1,2-Dichloroethane","Ethylene dichloride","107-06-2"},
        {"71","Chloroethane","Ethyl chloride","75-00-3"},
        {"97","Ethylbenzene","Ethyl benzene","100-41-4"},
        {"103","Methanol","Methyl alcohol","67-56-1"},
        {"104","Ethanol","Ethyl alcohol","64-17-5"},
        {"105","1-Propanol","n-Propyl alcohol","71-23-8"},
        {"106","Isopropanol","Isopropyl alcohol","67-63-0"},
        {"107","1-Butanol","n-Butyl alcohol","71-36-3"},
        {"108","2-Butanol","sec-Butyl alcohol","78-92-2"},
        {"109","Isobutanol","Isobutyl alcohol","78-83-1"},
        {"110","tert-Butanol","tert-Butyl alcohol","75-65-0"},
        {"123","1,4-Dioxane","Dioxane","123-91-1"},
        {"39","Methanethiol","Methyl mercaptan","74-93-1"},
        {"40","Ethanethiol","Ethyl mercaptan","75-08-1"},
        {"70","1,2-Dichloropropane","Propylene dichloride","78-87-5"},
        {"112","2-Butanone / MEK","2-Butanone","78-93-3"},
        {"120","Diethyl Ether","Ethyl ether","60-29-7"},
        {"125","Chlorodifluoromethane / R22","Chlorodifluoromethane","75-45-6"}
    };
    public static final class Result {
        public final int proposed,verified,inserted,rejected;
        Result(int p,int v,int i,int r){
            proposed=p;verified=v;inserted=i;rejected=r;
        }
        public String summary(){
            return SCHEMA+":proposed="+proposed+":verified="+verified+
                ":inserted="+inserted+":rejected="+rejected;
        }
    }
    private static boolean casValid(String cas){
        if(cas==null||!cas.matches("[0-9]{2,7}-[0-9]{2}-[0-9]"))return false;
        String s=cas.replace("-","");
        int sum=0,weight=1;
        for(int i=s.length()-2;i>=0;i--)sum+=(s.charAt(i)-'0')*weight++;
        return sum%10==s.charAt(s.length()-1)-'0';
    }
    private static String value(SQLiteDatabase db,String sql,String[] args){
        try(Cursor c=db.rawQuery(sql,args)){
            return c.moveToFirst()&&!c.isNull(0)?c.getString(0):null;
        }
    }
    private static long canonicalUid(SQLiteDatabase db,String cas,String expectedName){
        try(Cursor c=db.rawQuery(
                "SELECT uid,name_en,record_source FROM chemicals "+
                "WHERE cas=? AND active=1",new String[]{cas})){
            long found=0;
            while(c.moveToNext()){
                if(found!=0)return 0; // Ambiguous CAS, fail closed.
                if(!expectedName.equalsIgnoreCase(c.getString(1)))return 0;
                // Only genuine NIOSH-imported master identities are eligible.
                String provenance=c.isNull(2)?"":c.getString(2);
                // Later OSHA/NIOSH limit importers append authority names to
                // record_source; the NIOSH identity must remain the FIRST source
                // and an original chemical_sources NIOSH URL is required below.
                if(!provenance.startsWith("NIOSH_NPG"))return 0;
                found=c.getLong(0);
            }
            return found;
        }
    }
    public static Result ensure(ExperimentsDb source){
        if(source==null)throw new IllegalArgumentException("SQLite context missing");
        SQLiteDatabase db=source.getWritableDatabase();
        db.execSQL("CREATE TABLE IF NOT EXISTS chemical_identity_equivalence_v6("+
            "alias_uid INTEGER PRIMARY KEY, canonical_uid INTEGER NOT NULL,"+
            "source_cas TEXT NOT NULL,source_id TEXT NOT NULL,"+
            "source_url TEXT NOT NULL,evidence_status TEXT NOT NULL,"+
            "FOREIGN KEY(alias_uid) REFERENCES chemicals(uid),"+
            "FOREIGN KEY(canonical_uid) REFERENCES chemicals(uid),"+
            "CHECK(alias_uid<>canonical_uid),"+
            "CHECK(evidence_status='NIOSH_SYNONYM_VERIFIED'))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_v6_chemical_alias_canonical "+
            "ON chemical_identity_equivalence_v6(canonical_uid)");
        int valid=0,inserted=0,rejected=0;
        db.beginTransaction();
        try{
            for(String[] x:ALIASES){
                String oldId=x[0],oldName=x[1],newName=x[2],cas=x[3];
                if(!casValid(cas)){rejected++;continue;}
                String actualOldName=value(db,
                    "SELECT name_en FROM chemicals WHERE uid=? AND active=1",
                    new String[]{oldId});
                String actualOldCas=value(db,
                    "SELECT cas FROM chemicals WHERE uid=?",new String[]{oldId});
                if(actualOldName==null||!oldName.equalsIgnoreCase(actualOldName)||
                    actualOldCas!=null&&!actualOldCas.trim().isEmpty()){
                    rejected++;continue;
                }
                long primary=canonicalUid(db,cas,newName);
                if(primary<=0||primary==Long.parseLong(oldId)){
                    rejected++;continue;
                }
                String src=value(db,
                    "SELECT cs.record_url FROM chemical_sources cs "+
                    "WHERE cs.chemical_uid=? AND cs.source_id='NIOSH_NPG' "+
                    "AND cs.record_url LIKE 'https://www.cdc.gov/niosh/%' "+
                    "ORDER BY cs.id LIMIT 1",
                    new String[]{Long.toString(primary)});
                if(src==null){rejected++;continue;}
                valid++;
                ContentValues v=new ContentValues();
                v.put("alias_uid",Long.parseLong(oldId));
                v.put("canonical_uid",primary);
                v.put("source_cas",cas);
                v.put("source_id","NIOSH_NPG");
                v.put("source_url",src);
                v.put("evidence_status","NIOSH_SYNONYM_VERIFIED");
                if(db.insertWithOnConflict("chemical_identity_equivalence_v6",
                        null,v,SQLiteDatabase.CONFLICT_IGNORE)>=0)inserted++;
            }
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
        return new Result(ALIASES.length,valid,inserted,rejected);
    }
}
