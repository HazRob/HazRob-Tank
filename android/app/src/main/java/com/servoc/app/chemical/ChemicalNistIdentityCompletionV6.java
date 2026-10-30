package com.servoc.app.chemical;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.servoc.app.data.ExperimentsDb;

/**
 * Evidence-qualified NIST Chemistry WebBook SRD69 completion for 27 historical
 * pure-substance rows without direct CAS. Identity and molecular weight only:
 * NO inferred gas concentration, sensor calibration, clean-air rule, GHS or G3.
 *
 * The CAS/formula pairs and masses were independently reviewed against the
 * official NIST WebBook species identity page for each listed CAS.
 * Every source URL is durable and queryable from chemical_sources.
 */
public final class ChemicalNistIdentityCompletionV6 {
    public static final String VERSION="hazrob-tank-v6-nist-srd69-cas-identity/1";
    private ChemicalNistIdentityCompletionV6(){}

    // legacy master UID, EXACT legacy name, exact existing formula, verified
    // NIST SRD69 CAS, verified NIST molecular weight in g/mol.
    private static final String[][] SOURCE={
        {"38","Carbonyl Sulfide","COS","463-58-1","60.075"},
        {"41","Dimethyl Sulfide","C2H6S","75-18-3","62.134"},
        {"42","Dimethyl Disulfide","C2H6S2","624-92-0","94.199"},
        {"45","Tungsten Hexafluoride","WF6","7783-82-6","297.83"},
        {"46","Silicon Tetrafluoride","SiF4","7783-61-1","104.0791"},
        {"47","Titanium Tetrachloride","TiCl4","7550-45-0","189.679"},
        {"53","Aniline","C6H7N","62-53-3","93.1265"},
        {"57","Propionaldehyde","C3H6O","123-38-6","58.0791"},
        {"58","Butyraldehyde","C4H8O","123-72-8","72.1057"},
        {"76","Hydrogen","H2","1333-74-0","2.01588"},
        {"78","Ethane","C2H6","74-84-0","30.0690"},
        {"83","Isopentane","C5H12","78-78-4","72.1488"},
        {"84","Neopentane","C5H12","463-82-1","72.1488"},
        {"89","n-Decane","C10H22","124-18-5","142.2817"},
        {"90","Ethylene","C2H4","74-85-1","28.0532"},
        {"91","Propylene","C3H6","115-07-1","42.0797"},
        {"93","Cyclopropane","C3H6","75-19-4","42.0797"},
        {"121","MTBE","C5H12O","1634-04-4","88.1482"},
        {"124","Difluoromethane / R32","CH2F2","75-10-5","52.0234"},
        {"126","R134a","C2H2F4","811-97-2","102.0309"},
        {"127","R125","C2HF5","354-33-6","120.0214"},
        {"128","R143a","C2H3F3","420-46-2","84.0404"},
        {"129","R152a","C2H4F2","75-37-6","66.0500"},
        {"131","R1234yf","C3H2F4","754-12-1","114.0416"},
        {"132","R1234ze(E)","C3H2F4","29118-24-9","114.0416"},
        {"133","Carbon Tetrafluoride","CF4","75-73-0","88.0043"},
        {"134","Hexafluoroethane","C2F6","76-16-4","138.0118"}
    };
    public static final class Result {
        public final int proposed,matched,casFilled,massFilled,rejected;
        Result(int p,int m,int c,int w,int r){
            proposed=p;matched=m;casFilled=c;massFilled=w;rejected=r;
        }
        public String summary(){
            return VERSION+":proposed="+proposed+":matched="+matched+
                ":cas_filled="+casFilled+":molecular_mass_filled="+massFilled+
                ":rejected="+rejected;
        }
    }
    private static boolean validCas(String cas){
        if(cas==null||!cas.matches("[0-9]{2,7}-[0-9]{2}-[0-9]"))return false;
        String digits=cas.replace("-","");
        int sum=0;
        for(int i=digits.length()-2,weight=1;i>=0;i--,weight++)
            sum+=(digits.charAt(i)-'0')*weight;
        return sum%10==(digits.charAt(digits.length()-1)-'0');
    }
    public static Result ensure(ExperimentsDb repository){
        if(repository==null)throw new IllegalArgumentException("database required");
        SQLiteDatabase db=repository.getWritableDatabase();
        ChemicalIdentitySchema.ensure(db);
        int matched=0,filledCas=0,filledMass=0,rejected=0;
        db.beginTransaction();
        try{
            for(String[] record:SOURCE){
                String uid=record[0],name=record[1],formula=record[2],
                       cas=record[3],mw=record[4];
                double mass=Double.parseDouble(mw);
                if(!validCas(cas)||!Double.isFinite(mass)||mass<=0){
                    rejected++;continue;
                }
                try(Cursor c=db.rawQuery(
                        "SELECT name_en,formula,cas,molar_mass,legacy_id,record_source "+
                        "FROM chemicals WHERE uid=? AND active=1",
                        new String[]{uid})){
                    if(!c.moveToFirst()||
                        !name.equals(c.getString(0))||
                        !formula.equals(c.getString(1))||
                        c.isNull(4)||c.getInt(4)!=Integer.parseInt(uid)-1){
                        rejected++;continue;
                    }
                    String oldCas=c.isNull(2)?"":c.getString(2).trim();
                    boolean isOriginalLegacy=c.getString(5)!=null&&
                        c.getString(5).startsWith("HazRob legacy");
                    // The OSHA/NIOSH imports run AFTER this NIST completion
                    // and may augment record_source. On the second startup,
                    // preserve a verified NIST direct identity even when the
                    // record_source textual summary has changed.
                    boolean committedNist=false;
                    if(cas.equals(oldCas)){
                        try(Cursor prior=db.rawQuery(
                                "SELECT 1 FROM chemical_sources WHERE "+
                                "chemical_uid=? AND source_id='NIST_WEBBOOK' "+
                                "AND source_key=? AND record_url=? LIMIT 1",
                                new String[]{uid,"CAS:"+cas,
                                    "https://webbook.nist.gov/cgi/cbook.cgi?ID="+cas})){
                            committedNist=prior.moveToFirst();
                        }
                    }
                    if(!isOriginalLegacy&&!committedNist){
                        rejected++;continue;
                    }
                    if(!oldCas.isEmpty()&&!cas.equals(oldCas)){
                        rejected++;continue;
                    }
                    // Strictly idempotent replay: if this EXACT UID/name/
                    // formula already has the SAME CAS, SAME NIST source
                    // URL and SAME source mass, it is a trusted no-op.
                    // Do not re-evaluate post-import duplicate CAS rows:
                    // other scope-bound authority records may acquire
                    // the same CAS later from independent OSHA/NIOSH imports.
                    // Their provenance is preserved and never merged here.
                    if(committedNist){
                        if(c.isNull(3)||Math.abs(c.getDouble(3)-mass)>0.000001){
                            rejected++;continue;
                        }
                        matched++;continue;
                    }
                    // Never collapse distinct scientific UIDs into the
                    // same CAS without an independently qualified crosswalk.
                    try(Cursor other=db.rawQuery(
                            "SELECT uid,name_en,record_source FROM chemicals "+
                            "WHERE cas=? AND uid<>?",new String[]{cas,uid})){
                        if(other.moveToFirst()){
                            // NIOSH's broader "Aniline (and homologs)"
                            // occupational reference uses the CAS of its
                            // Aniline index substance, while the historical
                            // pure Aniline row deserves that same NIST CAS.
                            // Retain both UIDs: one is a scope-bound group
                            // reference, not proof that homologs are one CAS.
                            boolean sharedAnilineIndex="53".equals(uid)&&
                                "62-53-3".equals(cas)&&
                                other.getLong(0)==486L&&
                                "Aniline (and homologs)".equals(other.getString(1))&&
                                other.getString(2)!=null&&
                                other.getString(2).startsWith("NIOSH_NPG")&&
                                !other.moveToNext();
                            if(!sharedAnilineIndex){rejected++;continue;}
                        }
                    }
                    matched++;
                    ContentValues values=new ContentValues();
                    if(oldCas.isEmpty()){
                        values.put("cas",cas);
                        filledCas++;
                    }
                    if(c.isNull(3)||c.getDouble(3)<=0){
                        values.put("molar_mass",mass);
                        filledMass++;
                    }
                    if(values.size()>0){
                        if(db.update("chemicals",values,"uid=?",
                                new String[]{uid})!=1)
                            throw new IllegalStateException("CAS update failed for "+uid);
                    }
                }
                String url="https://webbook.nist.gov/cgi/cbook.cgi?ID="+cas;
                ContentValues evidence=new ContentValues();
                evidence.put("chemical_uid",Long.parseLong(uid));
                evidence.put("source_id","NIST_WEBBOOK");
                evidence.put("source_key","CAS:"+cas);
                evidence.put("record_url",url);
                db.insertWithOnConflict("chemical_sources",null,evidence,
                    SQLiteDatabase.CONFLICT_IGNORE);
                ContentValues identifier=new ContentValues();
                identifier.put("chemical_uid",Long.parseLong(uid));
                identifier.put("scheme","CAS");
                identifier.put("value",cas);
                identifier.put("source_id","NIST_WEBBOOK");
                db.insertWithOnConflict("chemical_identifiers",null,identifier,
                    SQLiteDatabase.CONFLICT_IGNORE);
            }
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
        return new Result(SOURCE.length,matched,filledCas,filledMass,rejected);
    }
}
