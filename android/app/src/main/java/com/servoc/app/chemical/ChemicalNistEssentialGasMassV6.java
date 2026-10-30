package com.servoc.app.chemical;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.servoc.app.data.ExperimentsDb;

/**
 * Verified NIST SRD 69 identity properties of two essential atmospheric gases.
 * These are molecular weights only. They are NOT normal concentration ranges,
 * O2 safety alarms, measured gas concentration, MQ R0 or Tank G3 calibration.
 */
public final class ChemicalNistEssentialGasMassV6 {
    public static final String VERSION="hazrob-tank-v6-nist-essential-gas-mass/1";
    private ChemicalNistEssentialGasMassV6(){}
    // Historical immutable UID, exact name, molecular formula, CAS, NIST SRD 69 mass g/mol.
    private static final String[][] SOURCE={
        {"23","Oxygen","O2","7782-44-7","31.9988"},
        {"77","Methane","CH4","74-82-8","16.0425"}
    };
    public static final class Result{
        public final int verified,filled,retained,rejected;
        Result(int v,int f,int r,int x){
            verified=v;filled=f;retained=r;rejected=x;
        }
        public String summary(){
            return VERSION+":verified="+verified+":filled="+filled+
                ":retained="+retained+":rejected="+rejected;
        }
    }
    public static Result ensure(ExperimentsDb database){
        if(database==null)throw new IllegalArgumentException("database required");
        SQLiteDatabase db=database.getWritableDatabase();
        ChemicalIdentitySchema.ensure(db);
        int verified=0,filled=0,retained=0,rejected=0;
        db.beginTransaction();
        try{
            for(String[] entry:SOURCE){
                final String uid=entry[0],name=entry[1],formula=entry[2],
                    cas=entry[3];
                final double molecularWeight=Double.parseDouble(entry[4]);
                try(Cursor row=db.rawQuery(
                    "SELECT name_en,formula,cas,legacy_id,molar_mass FROM chemicals "+
                    "WHERE uid=? AND active=1",new String[]{uid})){
                    if(!row.moveToFirst()||
                        !name.equals(row.getString(0))||
                        !formula.equals(row.getString(1))||
                        !cas.equals(row.getString(2))||
                        row.isNull(3)||row.getInt(3)!=Integer.parseInt(uid)-1){
                        rejected++;continue;
                    }
                    // Never replace a conflicting curated value. A later
                    // source review is required if the reported weight differs.
                    if(!row.isNull(4)&&row.getDouble(4)>0){
                        if(Math.abs(row.getDouble(4)-molecularWeight)>0.02){
                            rejected++;continue;
                        }
                        retained++;
                    }else{
                        ContentValues update=new ContentValues();
                        update.put("molar_mass",molecularWeight);
                        if(db.update("chemicals",update,
                            "uid=? AND (molar_mass IS NULL OR molar_mass<=0)",
                            new String[]{uid})!=1)
                            throw new IllegalStateException("NIST mass update race "+uid);
                        filled++;
                    }
                }
                // Preserve permanent source URL for provenance and repeatable
                // original NIST claim. Do not create concentration thresholds.
                String url="https://webbook.nist.gov/cgi/cbook.cgi?ID="+cas;
                ContentValues evidence=new ContentValues();
                evidence.put("chemical_uid",Long.parseLong(uid));
                evidence.put("source_id","NIST_WEBBOOK");
                evidence.put("source_key","MASS_CAS:"+cas);
                evidence.put("record_url",url);
                db.insertWithOnConflict("chemical_sources",null,evidence,
                    SQLiteDatabase.CONFLICT_IGNORE);
                verified++;
            }
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
        return new Result(verified,filled,retained,rejected);
    }
}
