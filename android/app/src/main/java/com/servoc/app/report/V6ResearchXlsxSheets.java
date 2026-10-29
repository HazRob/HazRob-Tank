package com.servoc.app.report;

import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * N06-C: pure-Java XLSX renderer of the two source-only V6 appendix sheets.
 *
 * Shares the EXACT approved-session sheet implementation with Android. No DB
 * or Android graphics dependencies. Called only after
 * FieldSessionReportExporter.resolve -> approved original SHA validation.
 * This class never asserts gas identity, reference-gas ppm or G3.
 */
final class V6ResearchXlsxSheets {
    private V6ResearchXlsxSheets(){}

    static int append(List<SmartXlsxPackage.Sheet> sheets,
                      String siteName,String siteId,
                      String sessionName,String fieldSessionId,
                      JSONObject v6) {
        if(sheets==null||v6==null)throw new IllegalArgumentException("V6 sheets require verified evidence");
        if(!V6FieldSessionResearchAppendix.SCHEMA.equals(v6.optString("schema",""))){
            throw new IllegalArgumentException("Unexpected V6 approved report research source");
        }
        if(!"UNKNOWN".equals(v6.optString("reported_identity",""))||
           v6.optBoolean("reportable_ppm",true)||
           v6.optBoolean("physical_tank_g3_validated",true)||
           v6.optBoolean("food_freshness_included",true)||
           !v6.optBoolean("no_db_write",false)||
           !v6.optBoolean("v5_approval_and_decisions_unchanged",false)){
            throw new IllegalArgumentException("V6 annex may not certify gas identity/ppm or modify approval");
        }
        if(!fieldSessionId.equals(v6.optString("field_session_id","")))
            throw new IllegalArgumentException("V6 annex field ID does not match approved bundle");
        JSONArray cycles=v6.optJSONArray("cycles");
        if(cycles==null||cycles.length()!=v6.optInt("approved_gas_cycles",-1))
            throw new IllegalArgumentException("V6 gas cycle count mismatch");
        if(cycles.length()==0)return 0;
        sheets.add(new SmartXlsxPackage.Sheet("V6 بحثي غازات",
            measurements(siteName,siteId,sessionName,fieldSessionId,cycles)));
        sheets.add(new SmartXlsxPackage.Sheet("V6 مصادر وتعارضات",
            sourceHypotheses(siteName,siteId,sessionName,fieldSessionId,cycles)));
        return 2;
    }

    private static void identity(SmartXlsxPackage.Grid g,
                        String site,String siteId,String session,String sessionId,
                        String title){
        g.title(title);
        g.header("Site","Site ID","Field Session","Field Session ID");
        g.row(site,siteId,session,sessionId);
    }
    private static Object finiteNumber(JSONObject q,String field){
        if(q==null||!q.has(field)||q.isNull(field))return null;
        double n=q.optDouble(field,Double.NaN);
        return Double.isFinite(n)?n:null;
    }
    private static String measurements(
                        String site,String siteId,String session,String sessionId,
                        JSONArray cycles){
        SmartXlsxPackage.Grid g=new SmartXlsxPackage.Grid(
            25,24,20,23,17,18,18,20,20,24,16,32,40);
        identity(g,site,siteId,session,sessionId,
            "ChemCore Tank V6 — بحثي فقط / UNKNOWN");
        g.blank();g.section("ضوابط قبول المصدر");
        g.header("البند","القيمة","معنى المؤشر");
        g.row("Schema",V6FieldSessionResearchAppendix.SCHEMA,"READ ONLY");
        g.row("Identity","UNKNOWN","لا يوجد غاز مؤهل G3");
        g.row("PPM reportable",false,"لا تستخدم أرقام المصادر لحساب تعرض مهني");
        g.row("Approved gas cycles",cycles.length(),"SHA الرسمي يتحقق قبل هذا الملحق");
        g.row("Source","telemetry_samples","قراءات Rs/R0 الأصلية المحفوظة لكل scan_cycle");
        g.row("Selection","recorded middle scan by index","لا نخلط قيم أجهزة/أزمنة مختلفة");
        g.blank();g.section("نتائج V6 المشتقة لكل دورة وحساس — أدلة بحثية غير معتمدة");
        g.header("Measurement ID","Cycle ID","Sensor","State",
                 "Distinct source scans","Eligible raw rows","Source t_ms",
                 "Source scan","Rs as stored","R0 as stored","Ratio ready",
                 "Curve hypotheses","Archive SHA256");
        for(int i=0;i<cycles.length();i++){
            JSONObject cycle=cycles.optJSONObject(i);
            if(cycle==null)throw new IllegalArgumentException("Non-object V6 source cycle");
            JSONArray sensors=cycle.optJSONArray("sensors");
            if(sensors==null)throw new IllegalArgumentException("Missing per-cycle sensor source data");
            for(int j=0;j<sensors.length();j++){
                JSONObject s=sensors.optJSONObject(j);
                if(s==null)continue;
                JSONArray curves=s.optJSONArray("source_curve_hypotheses");
                g.row(cycle.optString("measurement_session_id",""),
                    cycle.optString("cycle_id",""),
                    s.optString("sensor_model",""),
                    s.optString("state",""),
                    s.optInt("distinct_original_scan_cycles",0),
                    s.optInt("eligible_original_channel_rows",0),
                    s.has("selected_original_t_ms")?s.optLong("selected_original_t_ms",0):null,
                    s.has("selected_original_scan_cycle")?s.optLong("selected_original_scan_cycle",0):null,
                    finiteNumber(s,"selected_original_rs"),
                    finiteNumber(s,"selected_original_r0"),
                    s.optInt("ratio_ready_channels",0),
                    curves==null?0:curves.length(),
                    cycle.optString("research_source_sha256",""));
            }
        }
        g.blank();
        g.rowStyled(SmartXlsxPackage.WARN,
            "UNKNOWN","نتائج هذا الملحق بحثية؛ G3=0 ولا ppm معتمد.");
        g.rowStyled(SmartXlsxPackage.WARN,
            "Integrity","ملحق للقراءة فقط؛ لا يغير بصمة اعتماد القياس الأصلية.");
        return g.finish();
    }

    private static String sourceHypotheses(
                        String site,String siteId,String session,String sessionId,
                        JSONArray cycles){
        SmartXlsxPackage.Grid g=new SmartXlsxPackage.Grid(
            28,30,16,32,25,22,24,21,17,17,20);
        identity(g,site,siteId,session,sessionId,
            "ChemCore V6 — جميع فرضيات المصادر والتعارضات");
        g.blank();
        g.rowStyled(SmartXlsxPackage.WARN,"UNKNOWN",
            "فرضيات بحثية فقط؛ لا معايرة Tank G3 ولا تركيز ppm مهني معتمد");
        g.header("Measurement ID","Cycle ID","Sensor","Reference curve",
            "Source root","Chemical source ID","Gas or class",
            "Research-only curve ppm","Outside source range",
            "Contradictions count","Original scan cycle");
        for(int i=0;i<cycles.length();i++){
            JSONObject cycle=cycles.optJSONObject(i);
            if(cycle==null)throw new IllegalArgumentException("Non-object V6 source cycle");
            JSONArray sensors=cycle.optJSONArray("sensors");
            if(sensors==null)throw new IllegalArgumentException("Missing V6 sensor evidence");
            for(int j=0;j<sensors.length();j++){
                JSONObject sensor=sensors.optJSONObject(j);
                if(sensor==null)continue;
                JSONArray fits=sensor.optJSONArray("source_curve_hypotheses");
                JSONArray conflicts=sensor.optJSONArray("source_curve_disagreements");
                if(fits==null)continue;
                for(int k=0;k<fits.length();k++){
                    JSONObject fit=fits.optJSONObject(k);
                    if(fit==null)continue;
                    g.row(cycle.optString("measurement_session_id",""),
                        cycle.optString("cycle_id",""),
                        sensor.optString("sensor_model",""),
                        fit.optString("source_curve_id",""),
                        fit.optString("source_root",""),
                        fit.optString("chemical_reference_id",""),
                        fit.optString("gas_or_class",""),
                        finiteNumber(fit,"source_estimate_ppm_research"),
                        fit.optBoolean("outside_original_reference_domain",false),
                        conflicts==null?0:conflicts.length(),
                        sensor.optLong("selected_original_scan_cycle",-1));
                }
            }
        }
        return g.finish();
    }
}
