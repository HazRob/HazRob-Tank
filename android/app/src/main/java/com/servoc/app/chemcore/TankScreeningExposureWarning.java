package com.servoc.app.chemcore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

/**
 * Immediate, explicitly CONDITIONAL screening warnings from estimated one-shot MQ ppm.
 * No M4 reference-gas calibration is needed merely to display an appropriately labelled
 * comparison. This is NOT an occupational measurement, compliance verdict, or safety clearance.
 *
 * TWA/STEL: current ppm >= concentration limit warns about a hypothetical sustained exposure;
 * it is NOT evidence that a time-weighted average has exceeded a regulatory limit.
 * LFL/LEL, ceiling, IDLH: concentration-based comparisons are immediate; uncertain gas identity,
 * mixture effects, cross-sensitivity and unvalidated ppm remain prominently disclosed.
 */
public final class TankScreeningExposureWarning {
    public static final String KEY="screening_exposure_warnings";
    public static final String SCHEMA="tank-instant-exposure-screening/1";
    private TankScreeningExposureWarning(){}

    public static JSONObject evaluate(JSONObject analysis){
        JSONObject out=new JSONObject();
        try{
            out.put("schema",SCHEMA);
            out.put("method","candidate_screening_fit_vs_reference");
            out.put("m4_required_for_screening_warning",false);
            out.put("unvalidated_ppm",true);
            out.put("identity_unconfirmed",true);
            out.put("occupational_compliance_determination","NOT_MADE");
            out.put("time_weighted_average_evaluated",false);
            out.put("no_safety_clearance",true);
            JSONArray warnings=new JSONArray();out.put("warnings",warnings);
            if(analysis==null){out.put("status","NO_ANALYSIS");return out;}
            JSONObject catalog=analysis.optJSONObject(TankExposureReferenceCatalog.KEY);
            JSONArray stored=catalog==null?null:catalog.optJSONArray("candidates");
            JSONArray candidates=analysis.optJSONArray("candidates");
            if(candidates==null||stored==null){out.put("status","MISSING_CANDIDATES_OR_REFERENCES");return out;}
            int validFits=0,compared=0;
            for(int i=0;i<candidates.length()&&i<8;i++){
                JSONObject candidate=candidates.optJSONObject(i);if(candidate==null)continue;
                long uid=candidate.optLong("chemical_uid",-1);
                double ppm=candidate.optDouble("screening_fit_ppm",candidate.optDouble("best_ppm",Double.NaN));
                if(uid<0||!Double.isFinite(ppm)||ppm<0)continue;
                validFits++;
                JSONObject matching=byUid(stored,uid);
                if(matching==null)continue;
                JSONArray limits=matching.optJSONArray("limits");if(limits==null)continue;
                for(int j=0;j<limits.length();j++){
                    JSONObject limit=limits.optJSONObject(j);if(limit==null)continue;
                    double reference=limit.optDouble("value",Double.NaN);
                    if(!Double.isFinite(reference)||reference<=0)continue;
                    String type=limit.optString("limit_type","").toUpperCase(Locale.US);
                    String unit=limit.optString("unit","").trim().toLowerCase(Locale.US).replace(" ","");
                    String role=role(type),authority=limit.optString("authority",""),source=limit.optString("source_id","");
                    double referencePpm;
                    if("LFL".equals(role)){
                        if(!("vol%".equals(unit)||"%vol".equals(unit)||"volume%".equals(unit)||"%v/v".equals(unit)||"vol.%".equals(unit)))continue;
                        referencePpm=reference*10000.0;
                    }else{
                        // Do not silently translate mg/m3 without trustworthy molecular mass, T and P.
                        if(!("ppm".equals(unit)||"ppmv".equals(unit)))continue;
                        referencePpm=reference;
                    }
                    if(!Double.isFinite(referencePpm)||referencePpm<=0)continue;
                    compared++;
                    double fraction=ppm/referencePpm;
                    // 10% LFL is an early precaution, NOT proof of ignition at that fraction.
                    if(fraction<("LFL".equals(role)?0.10:1.0))continue;
                    JSONObject hit=new JSONObject();
                    hit.put("chemical_uid",uid);
                    hit.put("label",candidate.optString("label",matching.optString("label","—")));
                    hit.put("candidate_rank",candidate.optInt("rank",i+1));
                    hit.put("estimated_ppm",ppm);
                    hit.put("reference_value",reference);
                    hit.put("reference_unit",limit.optString("unit",""));
                    hit.put("reference_ppm",referencePpm);
                    hit.put("ratio_to_reference",fraction);
                    hit.put("authority",authority);
                    hit.put("reference_type",limit.optString("limit_type",""));
                    hit.put("reference_role",role);
                    hit.put("source_id",source);
                    hit.put("conditional",true);
                    hit.put("source_estimate_quality","UNVALIDATED_CANDIDATE_SCREENING");
                    hit.put("regulatory_exceedance_proven",false);
                    if("TWA".equals(role)||"STEL".equals(role)){
                        double period=limit.optDouble("duration_min",Double.NaN);
                        if(!Double.isFinite(period)||period<=0)period="TWA".equals(role)?480.0:15.0;
                        hit.put("reference_duration_min",period);
                        hit.put("warning_kind","SUSTAINED_EXPOSURE_HYPOTHETICAL");
                    }else if("LFL".equals(role)){
                        hit.put("percent_lfl",fraction*100.0);
                        hit.put("warning_kind",fraction>=1.0?"POSSIBLE_LFL_REACHED":"PRECAUTIONARY_LFL_APPROACH");
                    }else{
                        hit.put("warning_kind","INSTANTANEOUS_REFERENCE_SCREENING");
                    }
                    warnings.put(hit);
                }
            }
            out.put("numeric_candidates",validFits);
            out.put("compared_references",compared);
            out.put("warning_count",warnings.length());
            out.put("status",warnings.length()>0?"CONDITIONAL_WARNING":
                validFits==0?"NO_VALID_PPM_FIT":compared==0?"NO_COMPARABLE_REFERENCE":"NO_SCREENING_THRESHOLD_HIT");
        }catch(Exception ignored){try{out.put("status","EVALUATION_ERROR");}catch(Exception ignoredAgain){}}
        return out;
    }

    private static JSONObject byUid(JSONArray entries,long uid){
        for(int i=0;i<entries.length();i++){
            JSONObject x=entries.optJSONObject(i);
            if(x!=null&&x.optLong("chemical_uid",-1)==uid)return x;
        }
        return null;
    }
    private static String role(String type){
        if(type.contains("LFL")||type.contains("LEL")||type.contains("LOWER_EXPLOSIVE")||
           type.contains("LOWER_FLAMMABLE"))return "LFL";
        if(type.contains("IDLH"))return "IDLH";
        if(type.contains("CEILING")||type.trim().equals("C"))return "CEILING";
        if(type.contains("STEL")||type.contains("SHORT_TERM"))return "STEL";
        if(type.contains("TWA")||type.contains("PEL")||type.contains("REL"))return "TWA";
        return "OTHER";
    }

    public static String summary(JSONObject analysis,int maxWarnings){
        JSONObject result=analysis==null?null:analysis.optJSONObject(KEY);
        if(result==null)return "تنبيهات التعرض التقديرية غير متاحة لهذه العملية.";
        JSONArray entries=result.optJSONArray("warnings");
        if(entries==null||entries.length()==0){
            return "تنبيهات PPM التقديرية: لا توجد عتبة قابلة للمقارنة تجاوزها التقدير الحالي. "
                +"هذا لا يثبت خلو الهواء من الخطر أو دقة تحديد الغاز. الحالة: "
                +result.optString("status","—");
        }
        StringBuilder text=new StringBuilder("⚠ تحذير مبكر استناداً إلى PPM تقديري — هوية الغاز والتركيز غير مؤكدين");
        text.append("\nهذه مقارنة استدلالية مشروطة، وليست قياس تعرض معتمداً أو إثبات امتثال.");
        for(int i=0;i<entries.length()&&i<Math.max(1,maxWarnings);i++){
            JSONObject w=entries.optJSONObject(i);if(w==null)continue;
            String role=w.optString("reference_role","OTHER"),label=w.optString("label","—");
            double ppm=w.optDouble("estimated_ppm",Double.NaN),r=w.optDouble("reference_value",Double.NaN);
            text.append("\n\n").append(label).append(" • ")
                .append(w.optString("authority","")).append(" ")
                .append(w.optString("reference_type",role)).append(" • ")
                .append(number(ppm)).append(" ppm تقديري / ")
                .append(number(r)).append(" ").append(w.optString("reference_unit",""));
            if("TWA".equals(role)||"STEL".equals(role)){
                double mins=w.optDouble("reference_duration_min",Double.NaN);
                text.append("\n☠ مقارنة تقديرية افتراضية — لو استمر هذا التركيز نحو ").append(number(mins))
                    .append(" دقيقة فقد يتجاوز متوسط التعرض الحد المرجعي؛ لا يوجد متوسط زمني مقاس.");
            }else if("LFL".equals(role)){
                boolean atLfl=w.optDouble("ratio_to_reference",0)>=1.0;
                text.append(atLfl?"\n🔥 LFL تقديري — ":"\n⚠ اقتراب من LFL — ")
                    .append(number(w.optDouble("percent_lfl",Double.NaN)))
                    .append("% من LFL المقدّر • مقارنة فورية لا تنتظر مدة تعرض.")
                    .append(" الخليط وعدم اليقين قد يغيران الحد الفعلي.");
            }else{
                text.append("\nمقارنة تركيز لحظية مرجعية؛ يلزم تأكيد مستقل للغاز والتركيز.");
            }
        }
        return text.toString();
    }
    private static String number(double n){
        return Double.isFinite(n)?String.format(Locale.US,"%.3g",n):"—";
    }
}
