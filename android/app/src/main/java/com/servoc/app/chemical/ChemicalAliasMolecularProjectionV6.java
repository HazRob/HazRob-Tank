package com.servoc.app.chemical;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.servoc.app.data.ExperimentsDb;

/**
 * V6 strictly source-qualified reconciliation of missing molecular weight on
 * a historical chemical alias. A NIOSH CAS-verified identity crosswalk proves
 * the *same chemical* as a canonical row; only the canonical row's A_SOURCE
 * PubChem molecular-weight observation may fill an empty historical value.
 *
 * No historical chemical UID, original R0/Rs, sessions, exposure limits,
 * chemical classes or calibrated gas identity can be changed by this process.
 * This deliberately does NOT infer molecular mass from a formula.
 */
public final class ChemicalAliasMolecularProjectionV6 {
    public static final String VERSION="v6-niosh-cas-pubchem-identity-mass/1";
    private ChemicalAliasMolecularProjectionV6(){}

    public static final class Result{
        public final int eligibleBefore,eligibleAfter,filled,totalWithMass;
        Result(int eligibleBefore,int eligibleAfter,int filled,int totalWithMass){
            this.eligibleBefore=eligibleBefore;
            this.eligibleAfter=eligibleAfter;
            this.filled=filled;
            this.totalWithMass=totalWithMass;
        }
        public String summary(){
            return VERSION+":eligible_before="+eligibleBefore+
                ":eligible_after="+eligibleAfter+":filled="+filled+
                ":master_with_mass="+totalWithMass;
        }
    }

    private static final String QUALIFIED_JOIN=
        " FROM chemical_identity_equivalence_v6 x "+
        "JOIN chemicals canonical ON canonical.uid=x.canonical_uid "+
        "JOIN chemicals historic ON historic.uid=x.alias_uid "+
        "JOIN chemcore_properties p ON p.chemical_uid=canonical.uid "+
        "WHERE x.evidence_status='NIOSH_SYNONYM_VERIFIED' "+
        "AND canonical.cas=x.source_cas AND canonical.active=1 "+
        "AND historic.active=1 AND historic.uid<>canonical.uid "+
        "AND (historic.molar_mass IS NULL OR historic.molar_mass<=0) "+
        "AND p.property_key='MolecularWeight' "+
        "AND p.source_id='PUBCHEM' AND p.quality_tier='A_SOURCE' "+
        "AND p.value_numeric>0 AND p.value_numeric<100000";

    private static int scalar(SQLiteDatabase db,String q){
        try(Cursor c=db.rawQuery(q,null)){return c.moveToFirst()?c.getInt(0):0;}
    }
    private static boolean hasTable(SQLiteDatabase db,String table){
        try(Cursor c=db.rawQuery(
                "SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",
                new String[]{table})){return c.moveToFirst();}
    }
    public static Result ensure(ExperimentsDb source){
        if(source==null)throw new IllegalArgumentException("database required");
        SQLiteDatabase db=source.getWritableDatabase();
        if(!hasTable(db,"chemical_identity_equivalence_v6")||
                !hasTable(db,"chemcore_properties")){
            int has=scalar(db,"SELECT COUNT(*) FROM chemicals WHERE molar_mass>0");
            return new Result(0,0,0,has);
        }
        // Distinct per historic UID; never count multiple PubChem replicates
        // as independent compounds or fabricate a mass when evidence absent.
        int eligibleBefore=scalar(db,"SELECT COUNT(DISTINCT x.alias_uid)"+QUALIFIED_JOIN);
        db.beginTransaction();
        try{
            db.execSQL("UPDATE chemicals SET molar_mass=("+
                "SELECT p.value_numeric FROM chemical_identity_equivalence_v6 x "+
                "JOIN chemicals canonical ON canonical.uid=x.canonical_uid "+
                "JOIN chemcore_properties p ON p.chemical_uid=canonical.uid "+
                "WHERE x.alias_uid=chemicals.uid "+
                "AND x.evidence_status='NIOSH_SYNONYM_VERIFIED' "+
                "AND canonical.cas=x.source_cas AND canonical.active=1 "+
                "AND p.property_key='MolecularWeight' "+
                "AND p.source_id='PUBCHEM' AND p.quality_tier='A_SOURCE' "+
                "AND p.value_numeric>0 AND p.value_numeric<100000 "+
                "ORDER BY p.id DESC LIMIT 1) "+
                "WHERE (molar_mass IS NULL OR molar_mass<=0) "+
                "AND uid IN (SELECT DISTINCT x.alias_uid"+QUALIFIED_JOIN+")");
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
        int eligibleAfter=scalar(db,"SELECT COUNT(DISTINCT x.alias_uid)"+QUALIFIED_JOIN);
        int afterMass=scalar(db,"SELECT COUNT(*) FROM chemicals WHERE molar_mass>0");
        int filled=eligibleBefore-eligibleAfter;
        if(eligibleAfter>eligibleBefore||filled<0)
            throw new IllegalStateException("PubChem alias mass projection violated monotonicity");
        return new Result(eligibleBefore,eligibleAfter,filled,afterMass);
    }
}
