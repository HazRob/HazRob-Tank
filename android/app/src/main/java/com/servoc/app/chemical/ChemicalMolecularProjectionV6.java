package com.servoc.app.chemical;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.servoc.app.data.ExperimentsDb;

/**
 * V6 source-preserving enrichment of the ONE master chemicals table.
 *
 * Existing PubChem observations are imported by ChemCoreMolecularSeedImporter
 * only after a resolved CAS-to-UID match. This projection fills missing
 * formula/molar-mass identity fields from A_SOURCE observations; it does NOT
 * create chemical identities, gas curves, R0 values, exposure limits or G3
 * certifications. The original PubChem property and source metadata remain in
 * chemcore_properties for per-field traceability.
 */
public final class ChemicalMolecularProjectionV6 {
    public static final String VERSION="v6-pubchem-identity-projection/1";
    private ChemicalMolecularProjectionV6(){}

    public static final class Result {
        public final int beforeFormula,afterFormula,beforeMass,afterMass;
        public final String state;
        Result(int f0,int f1,int m0,int m1,String status){
            beforeFormula=f0;afterFormula=f1;beforeMass=m0;afterMass=m1;state=status;
        }
        public String summary(){
            return VERSION+":"+state+":formula="+beforeFormula+"->"+afterFormula+
                   ":molar_mass="+beforeMass+"->"+afterMass;
        }
    }
    private static int count(SQLiteDatabase db,String expression){
        try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM chemicals WHERE "+expression,null)){
            return c.moveToFirst()?c.getInt(0):0;
        }
    }
    private static boolean hasTable(SQLiteDatabase db,String name){
        try(Cursor c=db.rawQuery(
            "SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",
            new String[]{name})){return c.moveToFirst();}
    }
    public static Result ensure(ExperimentsDb db){
        if(db==null)throw new IllegalArgumentException("database required");
        SQLiteDatabase sql=db.getWritableDatabase();
        int f0=count(sql,"formula IS NOT NULL AND TRIM(formula)<>''");
        int m0=count(sql,"molar_mass IS NOT NULL AND molar_mass>0");
        if(!hasTable(sql,"chemcore_properties"))
            return new Result(f0,f0,m0,m0,"NO_PUBCHEM_PROPERTIES_TABLE");
        // Apply no speculative mass estimation from formula, and never update
        // a legacy or manually curated value. Source evidence remains queryable.
        sql.beginTransaction();
        try{
            sql.execSQL("UPDATE chemicals SET formula=("+ 
                "SELECT p.value_text FROM chemcore_properties p "+
                "WHERE p.chemical_uid=chemicals.uid AND "+
                "p.property_key='MolecularFormula' AND p.source_id='PUBCHEM' "+
                "AND p.quality_tier='A_SOURCE' AND p.value_text IS NOT NULL "+
                "AND TRIM(p.value_text)<>'' AND LENGTH(p.value_text)<=160 "+
                "ORDER BY p.id DESC LIMIT 1) "+
                "WHERE (formula IS NULL OR TRIM(formula)='') "+
                "AND EXISTS (SELECT 1 FROM chemcore_properties p "+
                "WHERE p.chemical_uid=chemicals.uid AND "+
                "p.property_key='MolecularFormula' AND p.source_id='PUBCHEM' "+
                "AND p.quality_tier='A_SOURCE' AND p.value_text IS NOT NULL "+
                "AND TRIM(p.value_text)<>'' AND LENGTH(p.value_text)<=160)");
            sql.execSQL("UPDATE chemicals SET molar_mass=("+ 
                "SELECT p.value_numeric FROM chemcore_properties p "+
                "WHERE p.chemical_uid=chemicals.uid AND "+
                "p.property_key='MolecularWeight' AND p.source_id='PUBCHEM' "+
                "AND p.quality_tier='A_SOURCE' AND p.value_numeric>0 "+
                "AND p.value_numeric<100000 "+
                "ORDER BY p.id DESC LIMIT 1) "+
                "WHERE (molar_mass IS NULL OR molar_mass<=0) "+
                "AND EXISTS (SELECT 1 FROM chemcore_properties p "+
                "WHERE p.chemical_uid=chemicals.uid AND "+
                "p.property_key='MolecularWeight' AND p.source_id='PUBCHEM' "+
                "AND p.quality_tier='A_SOURCE' AND p.value_numeric>0 "+
                "AND p.value_numeric<100000)");
            sql.setTransactionSuccessful();
        }finally{sql.endTransaction();}
        int f1=count(sql,"formula IS NOT NULL AND TRIM(formula)<>''");
        int m1=count(sql,"molar_mass IS NOT NULL AND molar_mass>0");
        if(f1<f0||m1<m0)throw new IllegalStateException(
            "non-destructive PubChem projection decreased chemical coverage");
        return new Result(f0,f1,m0,m1,"SOURCE_QUALIFIED_IDENTITY_ONLY");
    }
}
