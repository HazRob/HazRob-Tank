package com.servoc.app.chemical;

import android.content.Context;

import com.servoc.app.chemcore.ChemCoreResponseModel;
import com.servoc.app.chemcore.InverseMolecularSolver;
import com.servoc.app.chemcore.TankStableScreeningModelRegistry;
import com.servoc.app.data.ExperimentsDb;

import java.util.Collections;
import java.util.Locale;

/**
 * V5 verified runtime capability inventory for an individual chemical.
 * Distinguishes local SQLite evidence from actually activated, SHA-256-verified
 * ChemCore Stable priors. The absence of a local curve does not imply an
 * unavailable Stable curve; a catalog identity is not a sensor model.
 *
 * Performs SQLite and file I/O: call only from a background executor.
 */
public final class TankChemicalCapabilityReport {
    private TankChemicalCapabilityReport(){}

    public static String describe(Context context,ExperimentsDb db,ChemicalRepository.Chemical chemical){
        if(context==null||db==null||chemical==null)
            throw new IllegalArgumentException("Context, database and chemical required");
        TankChemicalEvidence.Flags local=TankChemicalEvidence.inspect(db,chemical);
        TankStableScreeningModelRegistry.LoadResult stable=
            TankStableScreeningModelRegistry.load(context.getApplicationContext(),db,Collections.emptyList());
        StringBuilder out=new StringBuilder();
        out.append("أدلة هذا الجهاز (SQLite):\n").append(local.describe());
        out.append("\n\nمكتبة ChemCore Stable النشطة:");
        out.append("\nالحالة: ").append(stable.status);
        out.append("\nالإصدار: ").append(stable.version.isEmpty()?"غير متاح":stable.version);
        out.append("\nSHA-256: ").append(shortHash(stable.sha256));
        if(!stable.status.startsWith("ACTIVE_STABLE")){
            out.append("\nتعذر التحقق من نماذج Stable النشطة. لا يعني ذلك خلو المادة من النماذج في المصادر الأخرى.");
            return out.toString();
        }
        int count=0;
        for(InverseMolecularSolver.Candidate candidate:stable.candidates){
            if(candidate==null||candidate.chemicalUid!=chemical.uid)continue;
            for(ChemCoreResponseModel model:candidate.models){
                if(model==null)continue;
                if(count++==0)out.append("\nنماذج المصدر القابلة للحساب (ليست معايرة جهاز):");
                if(count<=24){
                    out.append("\n• ").append(model.sensorKey().toUpperCase(Locale.US))
                       .append(" • ").append(model.evidenceTier())
                       .append(" • ").append(model.sourceId())
                       .append(" • ").append(model.version())
                       .append(" • ").append(rounded(model.minPpm())).append("–")
                       .append(rounded(model.maxPpm())).append(" ppm");
                }
            }
        }
        if(count==0){
            out.append("\nلا يوجد منحنى استجابة نشط لهذه المادة في Stable المحمّلة؛ "+
                "قد تبقى طرق أخرى في ChemCore متاحة للاستدلال.");
        }else{
            out.append("\nالإجمالي: ").append(count).append(" نموذجًا • "+
                "النطاقات المبيّنة هي حدود رياضية للنموذج، لا شهادة دقة أو تحقق PPM.");
        }
        return out.toString();
    }

    private static String rounded(double x){
        return Double.isFinite(x)?String.format(Locale.US,"%.4g",x):"—";
    }
    private static String shortHash(String s){
        return s==null||s.trim().isEmpty()?"غير متاح":s.substring(0,Math.min(16,s.length()))+"…";
    }
}
