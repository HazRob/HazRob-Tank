package com.servoc.app.chemical;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.servoc.app.calibration.CalibrationValidationMarker;
import com.servoc.app.data.ExperimentsDb;

/** Observational inventory of available paths. Never acts as a calculation gate. */
public final class TankChemicalEvidence {
    private TankChemicalEvidence(){}
    public static final class Flags {
        public final boolean localFingerprint,storedCurve,referenceAcceptedCurve,molecularIdentifier,algorithmEvidence;
        Flags(boolean fp,boolean c,boolean r,boolean m){this(fp,c,r,m,false);}
        Flags(boolean fp,boolean c,boolean r,boolean m,boolean algorithm){
            localFingerprint=fp;storedCurve=c;referenceAcceptedCurve=r;molecularIdentifier=m;algorithmEvidence=algorithm;
        }
        public boolean hasLocalMethod(){return localFingerprint||storedCurve||algorithmEvidence;}
        public String describe(){
            return "البصمة: "+(localFingerprint?"متاحة":"لا توجد بصمة محلية")
                +"\nمنحنى المعايرة: "+(storedCurve?"متاح للحساب":"لا يوجد منحنى محلي")
                +"\nمعادلة أو نموذج حسابي: "+(algorithmEvidence?"متاح":"لا توجد طريقة محلية مفهرسة")
                +"\nمرجعية المعايرة: "+(referenceAcceptedCurve?"يوجد مرجع مقبول":"غير موثقة مرجعيًا؛ لا يمنع الحساب")
                +"\nالتعريف الجزيئي: "+(molecularIdentifier?"يوجد معرف PubChem":"غير متاح")
                +"\nكل طرق الحساب الموجودة في ChemCore تبقى فعالة؛ مستوى الثقة والتقرير منفصل عن تشغيل الحساب.";
        }
        public String concise(){
            return (localFingerprint?"🧬 بصمة":"○ بصمة")+
                (storedCurve?" | 📈 منحنى":" | ○ منحنى")+
                (algorithmEvidence?" | ∑ معادلة/نموذج":" | ○ نموذج")+
                (referenceAcceptedCurve?" | مرجع مقبول":" | استدلالي");
        }
    }

    public static Flags inspect(ExperimentsDb db,ChemicalRepository.Chemical chemical){
        if(db==null||chemical==null)return new Flags(false,false,false,false);
        boolean fingerprint=false,curve=false,accepted=false,algorithm=false,identifier=chemical.pubchemCid!=null&&chemical.pubchemCid>0;
        SQLiteDatabase reader=db.getReadableDatabase();
        if(chemical.legacyId!=null&&chemical.legacyId>=0){
            String id=Integer.toString(chemical.legacyId);
            try(Cursor c=reader.rawQuery("SELECT 1 FROM fingerprints WHERE chemical_id=? LIMIT 1",new String[]{id})){
                fingerprint=c.moveToFirst();
            }
            // Only active physical calibration curves with >=2 persisted ppm points are recognized.
            try(Cursor c=reader.rawQuery(
                    "SELECT cc.reference_gas,cc.source_note,COUNT(cp.id) FROM calibration_curves cc "+
                    "JOIN calibration_points cp ON cp.curve_id=cc.id WHERE cc.chemical_id=? AND cc.active=1 "+
                    "GROUP BY cc.id,cc.reference_gas,cc.source_note HAVING COUNT(cp.id)>=2",
                    new String[]{id})){
                while(c.moveToNext()){
                    curve=true;
                    String gas=c.isNull(0)?"":c.getString(0);
                    String provenance=c.isNull(1)?"":c.getString(1);
                    if(!gas.trim().isEmpty()&&CalibrationValidationMarker.accepted(provenance))accepted=true;
                }
            }
        }
        // Algebraic/logarithmic curve or algorithm evidence is a distinct eligible analytical path.
        // These inventory flags NEVER disable, withhold, or alter any ChemCore calculation.
        if(chemical.legacyId!=null&&chemical.legacyId>=0){
            String id=Integer.toString(chemical.legacyId);
            try(Cursor c=reader.rawQuery("SELECT 1 FROM models WHERE chemical_id=? AND active=1 LIMIT 1",new String[]{id})){
                algorithm=c.moveToFirst();
            }
        }
        if(!algorithm){
            try(Cursor c=reader.rawQuery("SELECT 1 FROM reference_artifacts WHERE chemical_uid=? AND active=1 LIMIT 1",
                    new String[]{Long.toString(chemical.uid)})){
                algorithm=c.moveToFirst();
            }
        }
        if(!algorithm){
            try(Cursor c=reader.rawQuery(
                    "SELECT 1 FROM sensor_evidence WHERE chemical_uid=? AND ((equation IS NOT NULL AND TRIM(equation)<>'') "+
                    "OR (coefficients IS NOT NULL AND TRIM(coefficients)<>'')) LIMIT 1",
                    new String[]{Long.toString(chemical.uid)})){
                algorithm=c.moveToFirst();
            }
        }
        return new Flags(fingerprint,curve,accepted,identifier,algorithm);
    }
}
