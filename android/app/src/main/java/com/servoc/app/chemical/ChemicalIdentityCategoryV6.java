package com.servoc.app.chemical;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.servoc.app.data.ExperimentsDb;

/**
 * V6 semantic type of nine historical *labels* that are not unique
 * CAS-qualified single molecular substances. This is an ontology hint,
 * not a measurement, concentration, health classification or verified gas.
 *
 * Does NOT write a synthetic CAS, a molecular formula or any hazard value.
 * Existing curated class names are never overwritten.
 */
public final class ChemicalIdentityCategoryV6 {
    public static final String VERSION="hazrob-tank-v6-non-single-substance-types/1";
    private ChemicalIdentityCategoryV6(){}

    private static final String[][] TERMS={
        {"136","LPG","MIXTURE_FUEL"},
        {"137","CNG","MIXTURE_FUEL"},
        {"138","Natural Gas","MIXTURE_FUEL"},
        {"139","Town Gas","MIXTURE_FUEL"},
        {"140","Smoke","MIXTURE_COMPLEX_AEROSOL"},
        {"141","TVOC","AGGREGATE_VOC_METRIC"},
        {"142","NOx","AGGREGATE_REACTIVE_GAS_FAMILY"},
        {"143","LEL","MEASUREMENT_INDEX_NOT_CHEMICAL"},
        {"144","R410A","MIXTURE_REFRIGERANT"}
    };
    public static final class Result{
        public final int proposed,tagged,already,refused;
        Result(int proposed,int tagged,int already,int refused){
            this.proposed=proposed;this.tagged=tagged;this.already=already;
            this.refused=refused;
        }
        public String summary(){
            return VERSION+":proposed="+proposed+":tagged="+tagged+
                ":already="+already+":refused="+refused;
        }
    }
    public static Result ensure(ExperimentsDb db){
        if(db==null)throw new IllegalArgumentException("DB required");
        SQLiteDatabase sql=db.getWritableDatabase();
        int tagged=0,already=0,refused=0;
        sql.beginTransaction();
        try{
            for(String[] term:TERMS){
                String uid=term[0],name=term[1],type=term[2];
                try(Cursor c=sql.rawQuery(
                        "SELECT name_en,cas,chemical_class FROM chemicals "+
                        "WHERE uid=? AND legacy_id=? AND active=1",
                        new String[]{uid,uid})){
                    if(!c.moveToFirst()||!name.equals(c.getString(0))||
                        !c.isNull(1)&&!c.getString(1).trim().isEmpty()){
                        refused++;continue;
                    }
                    String existing=c.isNull(2)?"":c.getString(2).trim();
                    if(type.equals(existing)){already++;continue;}
                    if(!existing.isEmpty()){refused++;continue;}
                }
                ContentValues v=new ContentValues();
                v.put("chemical_class",type);
                int n=sql.update("chemicals",v,
                    "uid=? AND legacy_id=? AND (chemical_class IS NULL "+
                    "OR TRIM(chemical_class)='')",new String[]{uid,uid});
                if(n==1)tagged++;else refused++;
            }
            sql.setTransactionSuccessful();
        }finally{sql.endTransaction();}
        return new Result(TERMS.length,tagged,already,refused);
    }
}
