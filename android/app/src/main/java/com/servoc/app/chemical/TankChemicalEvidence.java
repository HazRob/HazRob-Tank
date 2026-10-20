package com.servoc.app.chemical;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.servoc.app.calibration.CalibrationValidationMarker;
import com.servoc.app.data.ExperimentsDb;

/**
 * Conservative provenance-only chemical search badges.
 * A molecular identifier is not a QSPR model; an active curve is not a validated ppm result.
 */
public final class TankChemicalEvidence {
    private TankChemicalEvidence(){}
    public static final class Flags {
        public final boolean localFingerprint,storedCurve,referenceAcceptedCurve,molecularIdentifier;
        Flags(boolean fp,boolean c,boolean r,boolean m){
            localFingerprint=fp;storedCurve=c;referenceAcceptedCurve=r;molecularIdentifier=m;
        }
        public String describe(){
            return "البصمة المحلية: "+(localFingerprint?"محفوظة":"لا توجد بصمة محلية مثبتة")
                +"\nمنحنى المعايرة: "+(storedCurve?"محفوظ (الصلاحية تعتمد على الجودة)":"غير موثق")
                +"\nدليل PPM مرجعي: "+(referenceAcceptedCurve?"توجد معايرة مرجعية مقبولة؛ الصلاحية الفعلية مشروطة بالنطاق والجودة":"غير مثبت")
                +"\nالتعريف الجزيئي: "+(molecularIdentifier?"معرف PubChem متاح؛ لا يثبت وجود نموذج QSPR":"لم يثبت توفر معرف PubChem")
                +"\nالتتبع اللحظي لا يعني التعرف الكمي على المادة.";
        }
        public String concise(){
            return (localFingerprint?"🧬 بصمة":"○ بصمة")+
                (storedCurve?" | 📈 منحنى":" | ○ منحنى")+
                (referenceAcceptedCurve?" | ✓ PPM مرجعي":" | ○ PPM غير مثبت")+
                (molecularIdentifier?" | ◇ PubChem":" | ○ جزيئي");
        }
    }

    public static Flags inspect(ExperimentsDb db,ChemicalRepository.Chemical chemical){
        if(db==null||chemical==null)return new Flags(false,false,false,false);
        boolean fingerprint=false,curve=false,accepted=false,identifier=chemical.pubchemCid!=null&&chemical.pubchemCid>0;
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
        return new Flags(fingerprint,curve,accepted,identifier);
    }
}
