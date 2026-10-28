package com.servoc.app.chemical;

import android.content.Context;

import com.servoc.app.chemcore.ChemCoreResponseModel;
import com.servoc.app.chemcore.InverseMolecularSolver;
import com.servoc.app.chemcore.TankStableScreeningModelRegistry;
import com.servoc.app.chemcore.TankStableRuntimeCatalog;
import com.servoc.app.chemcore.TankMultiSourceResponseModel;
import com.servoc.app.data.ExperimentsDb;

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
            TankStableRuntimeCatalog.responseModels(context.getApplicationContext(),db);
        StringBuilder out=new StringBuilder();
        out.append("أدلة هذا الجهاز (SQLite):\n").append(local.describe());
        out.append("\n\nمكتبة ChemCore Stable النشطة:");
        out.append("\nالحالة: ").append(stable.status);
        out.append("\nالإصدار: ").append(stable.version.isEmpty()?"غير متاح":stable.version);
        out.append("\nSHA-256: ").append(shortHash(stable.sha256));
        boolean verifiedStable=stable.status.startsWith("ACTIVE_STABLE");
        if(!verifiedStable)
            out.append("\nStable غير مثبتة/غير متحققة؛ سيستخدم المحرك المنحنيات "+
                       "المصدرية الموجودة دون ادعاء أن Stable نشطة.");
        out.append("\n\nالقدرات الحسابية الفعلية المتوفرة لهذه المادة:");
        int stableCount=0,otherCount=0,listed=0;
        for(InverseMolecularSolver.Candidate candidate:stable.candidates){
            if(candidate==null||candidate.chemicalUid!=chemical.uid)continue;
            for(ChemCoreResponseModel merged:candidate.models){
                if(merged==null)continue;
                java.util.List<ChemCoreResponseModel> alternatives=
                    merged instanceof TankMultiSourceResponseModel?
                    ((TankMultiSourceResponseModel)merged).sources():
                    java.util.Collections.singletonList(merged);
                for(ChemCoreResponseModel model:alternatives){
                    boolean fromStable=verifiedStable&&
                        model.version().startsWith("tank-stable-reference/1:");
                    if(fromStable)stableCount++;else otherCount++;
                    listed++;
                    out.append("\n• ").append(model.sensorKey().toUpperCase(Locale.US))
                       .append(fromStable?" • Stable LOG10":" • نموذج مصدري/QSPR")
                       .append(" • ").append(model.evidenceTier())
                       .append(" • ").append(model.sourceId())
                       .append(" • ").append(model.version())
                       .append(" • ").append(rounded(model.minPpm()))
                       .append("–").append(rounded(model.maxPpm())).append(" ppm");
                }
            }
        }
        if(listed==0)
            out.append("\nلا يوجد نموذج استجابة مؤهل للاستخدام الحسابي لهذه المادة في "+
                "المصادر الفعالة الحالية. وجود اسم المادة في قائمة 652 لا يثبت وجود بصمة قابلة للقياس.");
        else
            out.append("\nالنماذج: Stable ").append(stableCount)
                .append(" • مصدرية/QSPR ").append(otherCount)
                .append(" • الإجمالي ").append(listed)
                .append("\nالمنحنيات اللوغاريتمية تستخدم المجالات الموثقة لكل نموذج "+
                    "ولا تُعد معايرة PPM على هذا الجهاز.");
        out.append("\n\nالبصمات والإشارات:");
        out.append("\nبصمات SQLite: ").append(local.localFingerprint?"موجودة":"لا توجد بصمة محلية");
        out.append("\nنماذج المصدر: ").append(otherCount>0?"قابلة للفحص":"لا تتوفر مساهمات مؤهلة");
        out.append("\nالتعريف الجزيئي: ").append(local.molecularIdentifier?
            "موجود (قرينة وصفية وليست بصمة استجابة غاز)":"غير متاح");
        out.append("\nهذه القائمة جرد للطرق المتاحة؛ لا تعني أن كل طريقة قادرة على "+
            "تمييز مادة واحدة من خليط أو أن النتيجة معتمدة مهنيًا.");
        return out.toString();
    }

    private static String rounded(double x){
        return Double.isFinite(x)?String.format(Locale.US,"%.4g",x):"—";
    }
    private static String shortHash(String s){
        return s==null||s.trim().isEmpty()?"غير متاح":s.substring(0,Math.min(16,s.length()))+"…";
    }
}
