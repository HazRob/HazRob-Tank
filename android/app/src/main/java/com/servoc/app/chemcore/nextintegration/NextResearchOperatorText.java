package com.servoc.app.chemcore.nextintegration;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;

/**
 * P11.5 read-only Arabic operator presentation of the archived ChemCore Next
 * scientific source ledger. This class NEVER evaluates a new gas fit,
 * mutates R_air/R0, derives a chemical UID, sets an alarm, or ranks candidates.
 *
 * A source numerical ppm is labeled CONDITIONAL RESEARCH even when finite.
 * Missing data is a visible refusal with a physical reason, not zero ppm.
 */
public final class NextResearchOperatorText {
    private NextResearchOperatorText(){}
    public static final String EXPECTED_SCHEMA=
        "hazrob.tank.v8.scientific_source_diagnostics/2";

    private static boolean sourceLedger(JSONObject q){
        return q!=null&&EXPECTED_SCHEMA.equals(q.optString("schema",""));
    }
    private static Double positive(JSONObject q,String key){
        if(q==null||!q.has(key)||q.isNull(key))return null;
        double v=q.optDouble(key,Double.NaN);
        return Double.isFinite(v)&&v>0?v:null;
    }
    private static String numeric(JSONObject q,String key){
        Double v=positive(q,key);
        return v==null?"غير متاح":String.format(Locale.ROOT,"%.6g",v);
    }

    /** No evidence is automatically extrapolated from a missing/stale ledger. */
    public static String summary(JSONObject diagnostic){
        if(!sourceLedger(diagnostic))
            return "المرجع المزدوج • لا يوجد سجل علمي مرتبط بلقطة Next المحفوظة" +
                "\nلا يمكن حساب ppm من بيانات حية لاحقة أو معايرة مجهولة.";
        JSONArray models=diagnostic.optJSONArray("all_reference_models");
        if(models==null)return "المرجع المزدوج • سجل المنحنيات غير متاح";
        int evaluated=0,missing=0,mqsl=0,derived=0,unsupported=0;
        for(int i=0;i<models.length();i++){
            JSONObject row=models.optJSONObject(i);
            if(row==null){missing++;continue;}
            String status=row.optString("status","");
            if(status.startsWith("NOT_EVALUABLE")||positive(row,"estimated_ppm")==null)
                missing++;
            else evaluated++;
            if("MQSENSORSLIB_PINNED_LIBRARY_EXAMPLE".equals(
                    row.optString("source_class",""))){
                mqsl++;
                if(positive(row,"source_r0_research_kohm")!=null)derived++;
                if(!row.optBoolean("source_heater_phase_verified",false))unsupported++;
            }
        }
        return "المراجع العلمية • "+models.length()+" معادلة مصدرية"+
            "\nقابلة للحساب رياضيًا: "+evaluated+
            " • غير قابلة للحساب: "+missing+
            "\nMQSensorsLib: "+mqsl+" معادلة، مرجع R0 مشتق في "+derived+
            "\nR_air هو مرجع الجهاز؛ R0 المصدر = R_air ÷ K الخاص بالمنحنى"+
            "\nطور السخان/المرجع غير مؤهلين في "+unsupported+
            " من معادلات MQSensorsLib"+
            "\nكل ppm ناتج بحثي مشروط، لا يثبت وجود غاز أو سلامة الهواء أو تجاوز التعرض.";
    }

    public static String cause(String reason){
        if("MISSING_FROZEN_SNAPSHOT".equals(reason))
            return "لا توجد لقطة قياس Next مجمّدة";
        if("MQ_RELAY_OFF_IN_FROZEN_SNAPSHOT".equals(reason))
            return "الريلاي لم يكن في حالة تشغيل داخل لقطة القياس";
        if("MISSING_INSTALLED_MQ_SAMPLE".equals(reason))
            return "لم تُحفظ قراءة لهذا الحساس في لقطة القياس";
        if(reason.startsWith("SENSOR_QUALITY_"))
            return "جودة قراءة الحساس غير صالحة للتحليل: "+
                reason.substring("SENSOR_QUALITY_".length());
        if("WRONG_R0_DEFINITION".equals(reason))
            return "مرجع المعايرة لا يثبت أنه متوسط Rs للهواء النظيف";
        if("MISSING_AIR_RS_MEAN".equals(reason))
            return "مرجع R_air للهواء النظيف مفقود أو غير موجب";
        if("MISSING_POSITIVE_RS".equals(reason))
            return "قراءة Rs غير متاحة أو غير موجبة";
        if("INVALID_SOURCE_EQUATION".equals(reason))
            return "معاملات المعادلة المصدرية غير قابلة للتقييم";
        if("INVALID_SOURCE_REFERENCE_TRANSFORM".equals(reason))
            return "تعذّر تحويل مرجع R0 رياضيًا لهذه المعادلة";
        if("NONFINITE_SOURCE_RESULT".equals(reason))
            return "الحساب أعطى رقمًا غير منتهٍ أو غير صالح";
        return reason.isEmpty()?"لم يُسجل سبب تفصيلي ضمن المصدر":"رمز السبب: "+reason;
    }
    public static String missingReason(JSONObject row){
        if(row==null)return "لا توجد بيانات منحنى";
        String status=row.optString("status","");
        String code=row.optString("failure_reason_code","");
        if(!code.isEmpty())return cause(code);
        if(status.startsWith("NOT_EVALUABLE_NO_MATCHED"))
            return "القراءة أو R_air أو حالة تشغيل الحساس لا تستوفي شروط اللقطة المجمدة";
        if(status.contains("INVALID_SOURCE_EQUATION"))
            return cause("INVALID_SOURCE_EQUATION");
        if(status.contains("NONFINITE"))return cause("NONFINITE_SOURCE_RESULT");
        if(status.contains("SOURCE_REFERENCE_TRANSFORM"))
            return cause("INVALID_SOURCE_REFERENCE_TRANSFORM");
        return "لم يكتمل تقييم العلاقة المصدرية: "+status;
    }

    /** Per model, show each denominator with its own UNCHANGED scientific meaning. */
    public static String curveLine(JSONObject row){
        if(row==null)return "معادلة غير متاحة";
        String id=row.optString("source_curve_id","—");
        String sensor=row.optString("sensor_model","—");
        String label=row.optString("gas_or_class","—");
        String status=row.optString("status","NOT_EVALUABLE_UNKNOWN");
        boolean sourceExample="MQSENSORSLIB_PINNED_LIBRARY_EXAMPLE".equals(
            row.optString("source_class",""));
        StringBuilder s=new StringBuilder("[").append(id).append("] ")
            .append(sensor).append(" • ").append(label)
            .append("\nالحالة: ").append(status);
        Double ppm=positive(row,"estimated_ppm");
        if(status.startsWith("NOT_EVALUABLE")||ppm==null)
            s.append("\nلم تُحسب قيمة ppm: ").append(missingReason(row));
        else
            s.append("\n≈").append(numeric(row,"estimated_ppm"))
                .append(" ppm • نتيجة مصدرية بحثية غير معتمدة");
        s.append("\nRs = ").append(numeric(row,"rs_kohm")).append(" kΩ")
            .append(" • R_air = ")
            .append(numeric(row,"device_air_reference_kohm")).append(" kΩ");
        if(sourceExample){
            s.append("\nMQSensorsLib: K المصدر = ")
                .append(numeric(row,"source_example_clean_air_K"))
                .append(" • R0 المصدر المشتق = ")
                .append(numeric(row,"source_r0_research_kohm")).append(" kΩ")
                .append("\nRs/R_air = ")
                .append(numeric(row,"observed_rs_over_air"))
                .append(" • Rs/R0_source = ")
                .append(numeric(row,"observed_rs_r0"))
                .append("\nK وطور السخان غير متحققين على الدبابة؛ لا إثبات لهوية المادة.");
        }else{
            s.append("\nمنحنى مستقل؛ R_air للجهاز غير مثبت التطابق مع R0 البحثي")
                .append("\nلا يُطبّق معامل K الخاص بـMQSensorsLib على هذا المنحنى.");
        }
        s.append("\n").append(row.optString("source_equation",""))
            .append(" • a=").append(numeric(row,"a"))
            .append(" • b=").append(row.optDouble("b",Double.NaN))
            .append(" • أصل المصدر ").append(row.optString("source_root","—"));
        return s.toString();
    }
}
