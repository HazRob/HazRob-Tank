package com.servoc.app.report;

import android.database.Cursor;
import com.servoc.app.ReleaseAudit;
import com.servoc.app.chemical.ChemicalRepository;
import com.servoc.app.data.ExperimentQueries;
import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.data.SessionSeries;
import com.servoc.app.field.FieldCycleRepository;
import com.servoc.app.hardware.HazRobHardwareProfile;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Detailed analytical workbook for one Field Session.
 *
 * Design rule: PDF is the concise decision report; XLSX is the detailed auditable analytical companion.
 * Every sheet carries the parent Site identity so an exported worksheet cannot lose its operational context.
 */
final class SmartFieldSessionXlsxWriter {
    private SmartFieldSessionXlsxWriter(){}

    static void write(OutputStream out,ExperimentsDb db,FieldSessionReportExporter.Bundle b,ChemicalRepository chemicals)throws Exception{
        if(out==null||db==null||b==null)throw new IllegalArgumentException("output/db/session required");
        if(b.analyticalContract==null)throw new IllegalStateException("SmartReport analytical contract missing");
        SmartReportAnalyticalContract contract=b.analyticalContract;
        JSONObject cc=contract.decision(),hazard=CandidateHazardContext.evaluate(db,cc);
        ReportIntelligence.ReportPlan plan=ReportIntelligence.plan(contract.series(),cc,b.measurementSessionIds.size());
        List<SmartXlsxPackage.Sheet> sheets=new ArrayList<>();
        sheets.add(new SmartXlsxPackage.Sheet("الملخص التنفيذي",executive(b,plan,cc,hazard)));
        sheets.add(new SmartXlsxPackage.Sheet("بيانات الجلسة",sessionMetadata(b)));
        sheets.add(new SmartXlsxPackage.Sheet("دورات القياس",cycleLedger(db,b)));
        sheets.add(new SmartXlsxPackage.Sheet("المرشحون والمخاطر",candidateHazards(b,hazard)));
        sheets.add(new SmartXlsxPackage.Sheet("مصفوفة 144",panel144(b,cc)));
        sheets.add(new SmartXlsxPackage.Sheet("الأدلة التحليلية",candidateEvidence(b,cc)));
        sheets.add(new SmartXlsxPackage.Sheet("حدود التعرض",exposureReferences(b,hazard)));
        sheets.add(new SmartXlsxPackage.Sheet("الجودة وعدم اليقين",qualityUncertainty(b,plan,cc)));
        sheets.add(new SmartXlsxPackage.Sheet("السلسلة الزمنية",timeSeries(db,b)));
        // V7 research hand-off: immutable stored MQ JSON is copied directly
        // from telemetry_samples, not recomputed by SessionSeries.normalizeMq.
        sheets.add(new SmartXlsxPackage.Sheet("MQ الثمانية الخام",rawMqWide(db,b)));
        sheets.add(new SmartXlsxPackage.Sheet("MQ الأصل لكل حساس",rawMqLong(db,b)));
        sheets.add(new SmartXlsxPackage.Sheet("القياسات الفنية",measurementRecords(b)));
        sheets.add(new SmartXlsxPackage.Sheet("عقد التحليل",contractMetadata(b)));
        sheets.add(new SmartXlsxPackage.Sheet("المنهجية والمصدر",provenance(b,plan,cc)));
        // READ-ONLY V6 research appendix. The authoritative V5 approved
        // decision and original raw evidence remain unchanged and SHA-checked.
        JSONObject v6Field=V6FieldSessionResearchAppendix.analyzeVerified(db,b);
        V6ResearchXlsxSheets.append(sheets,b.site.name,b.site.id,
            b.fieldSession.name,b.fieldSession.id,v6Field);
        SmartXlsxPackage.write(out,sheets);
    }

    private static String executive(FieldSessionReportExporter.Bundle b,ReportIntelligence.ReportPlan plan,JSONObject cc,JSONObject hazard){
        SmartXlsxPackage.Grid g=grid();identity(g,b,"HazRob HR069 — التقرير الذكي للجلسة");g.blank();g.section("القرار التنفيذي");
        g.header("البند","القيمة","الدلالة");
        g.row("نمط التقرير",plan.modeAr,plan.executiveTitleAr);g.row("الهوية/المرشح",cc==null?"—":cc.optString("chemical","—"),cc==null?"—":cc.optString("evidence_tier","—"));
        g.row("الدعم التحليلي %",finite(plan.analyticalConfidencePct),"مؤشر دعم إيجابي وليس احتمال وجود معاير");g.row("Top ranking weight",finite(plan.topPosterior),"وزن ترتيب نسبي");g.row("Separation margin",finite(plan.separationMargin),"فصل المرشح الأول عن التالي");g.row("OOD",finite(plan.oodScore),"مؤشر خروج عن المجال");g.row("Normalized entropy",finite(plan.entropy),"عدم يقين/التباس");
        g.row("مصفوفة 144",plan.panelCandidateSupported+" مواد بدليل جلسة",plan.panelAcceptedIdentities+" هويات مقبولة • "+plan.panelEvaluated+"/"+plan.panelTarget+" مقيّمة");
        boolean ppm=plan.ppmReportable&&cc!=null&&Double.isFinite(cc.optDouble("ppm",Double.NaN));g.row("PPM القابل للتقرير",ppm?cc.optDouble("ppm",Double.NaN):"محجوب",ppm?"M4 reportable":"لا يستخدم كقياس تعرض مهني");
        g.blank();g.section("الملاحظات الآلية");g.header("الشدة","الملاحظة","التفصيل");if(plan.findings.isEmpty())g.row("INFO","لا توجد ملاحظات منهجية حرجة","—");else for(ReportIntelligence.Finding f:plan.findings)g.row(f.severity.name(),f.titleAr,f.detailAr);
        g.blank();g.section("أعلى سياقات مخاطر المرشحين");g.header("الرتبة","المادة","أساس التركيز","ppm","الرموز","السمية","% من المرجع","الاشتعال","%LFL","الأولوية");JSONArray a=hazard==null?null:hazard.optJSONArray("candidates");if(a!=null)for(int i=0;i<Math.min(10,a.length());i++){JSONObject q=a.optJSONObject(i);if(q==null)continue;JSONObject t=q.optJSONObject("toxicity"),f=q.optJSONObject("flammability");g.row(q.optInt("rank",i+1),label(q),q.optString("concentration_basis",""),num(q,"concentration_ppm"),q.optString("hazard_symbols",""),t==null?"":t.optString("state",""),t==null?null:num(t,"max_context_percent"),f==null?"":f.optString("state",""),f==null?null:num(f,"percent_lfl_context"),q.optString("priority_class",""));}
        g.rowStyled(SmartXlsxPackage.WARN,"تنبيه","EST/screening-fit ppm هو سياق أولوية فقط ولا ينشئ TWA/STEL/LFL alarm أو حكم امتثال.");return g.finish();
    }


    private static String sessionMetadata(FieldSessionReportExporter.Bundle b){SmartXlsxPackage.Grid g=grid();identity(g,b,"هوية الموقع والجلسة");g.blank();g.section("بيانات Field Session");g.header("الحقل","القيمة");g.row("Site",b.site.name);g.row("Site ID",b.site.id);g.row("Field Session",b.fieldSession.name);g.row("Field Session ID",b.fieldSession.id);g.row("Status",b.fieldSession.status);g.row("Started",b.fieldSession.startedTs);g.row("Ended",nz(b.fieldSession.endedTs).isEmpty()?"ACTIVE":b.fieldSession.endedTs);g.row("Measurement Session count",b.measurementSessionIds.size());g.row("Derived experiment records",b.records.size());g.row("Primary analytical measurement",nz(b.primaryMeasurementSessionId));g.blank();g.section("Measurement Sessions");g.header("#","Measurement Session ID");for(int i=0;i<b.measurementSessionIds.size();i++)g.row(i+1,b.measurementSessionIds.get(i));return g.finish();}

    /** One row per documented attempt, including failed/superseded history. */
    private static String cycleLedger(ExperimentsDb db,FieldSessionReportExporter.Bundle b){
        SmartXlsxPackage.Grid g=new SmartXlsxPackage.Grid(
            18,18,16,34,23,20,22,25,22,22,44,25,34);
        identity(g,b,"سجل دورات الجلسة الموثقة • الاعتماد والتتبع");
        g.blank();
        g.header("Sequence","Attempt","Status","Measurement ID","Gas","PM","Physical",
                 "Start UTC","Complete UTC","Approve UTC","Device ID","Evidence SHA-256","Reason");
        for(FieldCycleRepository.Cycle c:b.cycleLedger){
            g.row(c.sequence,c.attempt,c.state,c.measurementId,
                c.modes.gas?"YES":"NO",c.modes.particles?"YES":"NO",
                c.modes.physical?"YES":"NO",c.started,c.completed,c.approved,
                c.deviceId,c.evidenceSha,c.failReason);
        }
        g.blank();
        g.rowStyled(SmartXlsxPackage.WARN,"ملاحظة",
            "القياسات المعتمدة فقط تدخل التحليل/الرسوم الرئيسية؛ المحاولات الفاشلة "+
            "أو المعادة تبقى هنا للتدقيق، دون اعتبارها قياسات صالحة.");
        return g.finish();
    }

    private static String candidateHazards(FieldSessionReportExporter.Bundle b,JSONObject hazard){SmartXlsxPackage.Grid g=wide();identity(g,b,"المرشحون وسياق المخاطر");g.blank();g.header("Rank","المادة","CAS","الصيغة","الفئة","Evidence tier","Ranking weight","Support %","أساس التركيز","ppm","☠ السمية","مرجع السمية المسيطر","% من المرجع","🔥 الاشتعال","LFL %vol","%LFL","الأولوية","مسموح تفسير مهني؟");JSONArray a=hazard==null?null:hazard.optJSONArray("candidates");if(a!=null)for(int i=0;i<a.length();i++){JSONObject q=a.optJSONObject(i);if(q==null)continue;JSONObject t=q.optJSONObject("toxicity"),f=q.optJSONObject("flammability"),tr=t==null?null:t.optJSONObject("controlling_reference");g.row(q.optInt("rank",i+1),label(q),q.optString("cas",""),q.optString("formula",""),q.optString("chemical_class",""),q.optString("evidence_tier",""),num(q,"ranking_weight"),num(q,"analytical_support_index_pct"),q.optString("concentration_basis",""),num(q,"concentration_ppm"),t==null?"":t.optString("state",""),tr==null?"":tr.optString("authority","")+" "+tr.optString("limit_type","")+" "+valueUnit(tr),t==null?null:num(t,"max_context_percent"),f==null?"":f.optString("state",""),f==null?null:num(f,"lfl_vol_pct"),f==null?null:num(f,"percent_lfl_context"),q.optString("priority_class",""),q.optBoolean("occupational_interpretation_permitted",false));}g.rowStyled(SmartXlsxPackage.WARN,"ملاحظة","المقارنات الخاصة بالمرشحين غير M4 هي Hazard Context فقط، وليست قياس تعرض أو حكم امتثال.");return g.finish();}

    private static String panel144(FieldSessionReportExporter.Bundle b,JSONObject cc){SmartXlsxPackage.Grid g=wide();identity(g,b,"مصفوفة 144 مادة — دليل الجلسة");g.blank();g.header("Index","المادة","CAS","الحالة","Array support","Session candidate evidence","Accepted identity","Candidate rank","Session support %","Sensor breadth","Evidence groups","Evidence quality %","Residual/Fit","ملاحظة");JSONObject p=cc==null?null:cc.optJSONObject("screening_144");JSONArray rows=p==null?null:p.optJSONArray("rows");if(rows!=null)for(int i=0;i<rows.length();i++){JSONObject q=rows.optJSONObject(i);if(q==null)continue;g.row(q.optInt("index",i)+1,q.optString("name",""),q.optString("cas",""),q.optString("status",""),q.optBoolean("catalogue_array_supported",!"UNSUPPORTED_BY_CURRENT_ARRAY".equals(q.optString("status",""))),q.optBoolean("session_candidate_evidence",false),q.optBoolean("detected",false),q.has("candidate_rank")?q.optInt("candidate_rank",0):null,num(q,"session_support_index_pct"),q.has("evidence_sensor_breadth")?q.optInt("evidence_sensor_breadth",0):null,q.has("evidence_group_count")?q.optInt("evidence_group_count",0):null,num(q,"evidence_quality_pct"),firstFinite(q,"residual","fit_error","distance"),q.optString("session_evidence_note",q.optString("note","")));}g.rowStyled(SmartXlsxPackage.WARN,"قاعدة تفسير","غياب دليل مرشح في الجلسة لا يعني نفي وجود المادة، خصوصًا إذا كانت قدرة المصفوفة محدودة.");return g.finish();}

    private static String candidateEvidence(FieldSessionReportExporter.Bundle b,JSONObject cc){SmartXlsxPackage.Grid g=wide();identity(g,b,"تفاصيل الأدلة التحليلية للمرشحين");g.blank();g.header("Rank","المرشح","Posterior/weight","Support %","Coverage %","Agreement %","Evidence quality %","Evidence groups","Sensor breadth","screening-fit ppm","Residual","Evidence tier","Evidence types","QSPR obs","QSPR in-domain","QSPR mean support","QSPR lineage");JSONArray a=cc==null?null:cc.optJSONArray("candidates");if(a!=null)for(int i=0;i<a.length();i++){JSONObject q=a.optJSONObject(i);if(q==null)continue;g.row(q.optInt("rank",i+1),q.optString("label",""),num(q,"posterior"),num(q,"analytical_confidence_pct"),num(q,"coverage_pct"),num(q,"agreement_pct"),num(q,"evidence_quality_pct"),q.optInt("evidence_group_count",0),q.optInt("evidence_sensor_breadth",0),firstFinite(q,"screening_fit_ppm","best_ppm"),firstFinite(q,"residual","fit_error","distance"),q.optString("evidence_tier",""),evidenceTypes(q),qsprCount(q,false),qsprCount(q,true),qsprMeanSupport(q),qsprLineage(q));}g.rowStyled(SmartXlsxPackage.WARN,"QSPR/PubChem","يتم عرض evidence_observations المحفوظة فقط. QSPR دعم جزيئي M1 ولا يشغّل تحليلًا جديدًا وقت التصدير ولا يفتح PPM تقريرية.");
        g.blank();g.section("تفاصيل Stage17 common-C / منحنيات الاستجابة المحفوظة");g.header("Rank","المرشح","Sensor keys","Evidence type","Model","Source","Inside domain","Observed Rs/R0","Predicted log10 Rs/R0","Residual log10","Standardized residual","C* fitted ppm","Support weight");if(a!=null)for(int i=0;i<Math.min(10,a.length());i++){JSONObject q=a.optJSONObject(i);if(q==null)continue;JSONArray ev=q.optJSONArray("evidence_observations");if(ev==null)continue;for(int j=0;j<ev.length();j++){JSONObject e=ev.optJSONObject(j);if(e==null)continue;g.row(q.optInt("rank",i+1),q.optString("label",""),sensorKeys(e),e.optString("type",""),e.optString("model_id",""),e.optString("source_id",""),e.optBoolean("inside_domain",false),num(e,"observed_ratio"),num(e,"predicted_log10_ratio"),num(e,"residual_log10"),num(e,"standardized_residual"),num(e,"fitted_ppm"),num(e,"support_weight"));}}g.rowStyled(SmartXlsxPackage.WARN,"Stage17","هذه صفوف evidence_observations المحفوظة التي تغذي فحص اتساق المنحنيات عند C*. لا يعاد تشغيل solver أثناء التصدير، وC* screening ليست PPM تقريرية إلا بعد M4.");return g.finish();}

    private static String exposureReferences(FieldSessionReportExporter.Bundle b,JSONObject hazard){SmartXlsxPackage.Grid g=wide();identity(g,b,"حدود التعرض والاشتعال المرتبطة بالمرشحين");g.blank();g.header("Rank","المادة","نوع المرجع","Authority","Jurisdiction","Limit type","Value","Unit","Duration min","Notation","Source","Comparable to candidate ppm","Ratio","% reference","Interpretation");JSONArray a=hazard==null?null:hazard.optJSONArray("candidates");if(a!=null)for(int i=0;i<a.length();i++){JSONObject q=a.optJSONObject(i);if(q==null)continue;int rank=q.optInt("rank",i+1);String name=label(q);JSONObject t=q.optJSONObject("toxicity"),f=q.optJSONObject("flammability");appendRefs(g,rank,name,"TOXICITY",t==null?null:t.optJSONArray("references"));appendRefs(g,rank,name,"FLAMMABILITY",f==null?null:f.optJSONArray("references"));}g.rowStyled(SmartXlsxPackage.WARN,"قاعدة تفسير","الحدود الزمنية TWA/STEL/Ceiling لا تتحول إلى تجاوز مهني من screening-fit ppm؛ التفسير المهني يتطلب بوابة M4 والسلسلة الزمنية المناسبة.");return g.finish();}

    private static String qualityUncertainty(FieldSessionReportExporter.Bundle b,ReportIntelligence.ReportPlan plan,JSONObject cc){SmartXlsxPackage.Grid g=grid();identity(g,b,"الجودة وعدم اليقين");g.blank();g.header("المؤشر","القيمة","المعنى");g.row("Report mode",plan.mode.name(),plan.modeAr);g.row("Analytical support %",finite(plan.analyticalConfidencePct),"Positive-evidence index");g.row("Top ranking weight",finite(plan.topPosterior),"Relative model ranking");g.row("Separation margin",finite(plan.separationMargin),"Top minus runner-up");g.row("OOD score",finite(plan.oodScore),"Open-set distance");g.row("Normalized entropy",finite(plan.entropy),"Ambiguity");g.row("PPM reportable",plan.ppmReportable,"Requires accepted quantitative gate");g.row("Safety interpretable",plan.safetyInterpretable,"Does not itself create legal compliance conclusion");if(cc!=null){g.row("Outcome",cc.optString("outcome",""),"");g.row("Evidence tier",cc.optString("evidence_tier",""),"");g.row("Prediction low",num(cc,"prediction_low"),"");g.row("Prediction high",num(cc,"prediction_high"),"");g.row("Quantitative occupational interpretation",cc.optBoolean("quantitative_occupational_interpretation_permitted",false),"");}g.blank();g.section("Planner findings");g.header("Severity","Title","Detail");for(ReportIntelligence.Finding f:plan.findings)g.row(f.severity.name(),f.titleAr,f.detailAr);return g.finish();}

    private static String timeSeries(ExperimentsDb db,FieldSessionReportExporter.Bundle b){SmartXlsxPackage.Grid g=new SmartXlsxPackage.Grid(28,12,15,15,15,14,14,14,14,14,14,14,14,14,14,14,14,14,14);identity(g,b,"السلاسل الزمنية — لا يتم دمج الجلسات الفنية في منحنى مصطنع");g.blank();List<String> h=new ArrayList<>();h.add("Measurement Session");h.add("t_ms");h.add("Temperature °C");h.add("Humidity %RH");h.add("Pressure hPa");for(HazRobHardwareProfile.Channel q:HazRobHardwareProfile.MQ){h.add(q.key+" Rs/R0");h.add(q.key+" Rs");}g.header(h.toArray(new String[0]));for(String sid:b.measurementSessionIds){SessionSeries.Series s=SessionSeries.byId(db,sid);if(s==null)continue;for(SessionSeries.Sample sample:s.samples){List<Object> r=new ArrayList<>();r.add(sid);r.add(sample.tMs);r.add(finiteObj(sample.env,"temperature"));r.add(finiteObj(sample.env,"humidity"));r.add(finiteObj(sample.env,"pressure"));for(HazRobHardwareProfile.Channel hw:HazRobHardwareProfile.MQ){JSONObject q=SessionSeries.sensor(sample,hw.key);r.add(finiteObj(q,"ratio"));r.add(finiteObj(q,"rs"));}g.row(r.toArray(new Object[0]));}}return g.finish();}

    /**
     * SOURCE-EXACT V7 simulation handoff.
     * One source database row per captured Android telemetry sample, displaying
     * all eight MQ channels together. Values originate from the saved mq_payload,
     * without the historical-ratio recalculation used by analytical charts.
     * "ADC" is blank if the ESP32 did not record per-channel ADC in mq_payload.
     */
    private static String rawMqWide(ExperimentsDb db,FieldSessionReportExporter.Bundle b){
        int[] widths=new int[5+HazRobHardwareProfile.MQ.length*4];
        for(int i=0;i<widths.length;i++)widths[i]=i==0?33:i==1?16:20;
        SmartXlsxPackage.Grid g=new SmartXlsxPackage.Grid(widths);
        identity(g,b,"MQ — source-exact 8-channel timestamped capture");
        g.blank();g.rowStyled(SmartXlsxPackage.WARN,"SOURCE CONTRACT",
            "Rs/R0/ADC here are only the original archived mq_payload fields. "+
            "Blank means absent. No current R0 or calculated ratios are substituted.");
        List<String> headings=new ArrayList<>();
        headings.add("Measurement Session ID");headings.add("Telemetry row ID");
        headings.add("t_ms");headings.add("sample UTC ms");headings.add("Scan record / state");
        for(HazRobHardwareProfile.Channel hw:HazRobHardwareProfile.MQ){
            headings.add(hw.key+" Rs as stored");headings.add(hw.key+" R0 as stored");
            headings.add(hw.key+" ratio as stored");headings.add(hw.key+" ADC if recorded");
        }
        g.header(headings.toArray(new String[0]));
        for(String sid:b.measurementSessionIds){
            try(Cursor c=db.getReadableDatabase().rawQuery(
                "SELECT id,t_ms,mq_payload,motion_payload FROM telemetry_samples "+
                "WHERE session_id=? ORDER BY t_ms,id",new String[]{sid})){
                while(c.moveToNext()){
                    JSONObject mq=parseRaw(c.getString(2)),state=parseRaw(c.getString(3));
                    List<Object> row=new ArrayList<>();
                    row.add(sid);row.add(c.getLong(0));row.add(c.getLong(1));
                    row.add(exactNumber(state,"sample_utc_ms"));
                    row.add(exactString(state,"measurement_state"));
                    for(HazRobHardwareProfile.Channel hw:HazRobHardwareProfile.MQ){
                        JSONObject q=mq.optJSONObject(hw.key);
                        row.add(exactNumber(q,"rs"));
                        row.add(exactNumber(q,"r0"));
                        row.add(exactNumber(q,"ratio"));
                        row.add(adcFromMq(q));
                    }
                    g.row(row.toArray(new Object[0]));
                }
            }
        }
        return g.finish();
    }

    /**
     * One row per actual sensor/scan sample: complete per-sensor MQ JSON is
     * retained verbatim as an independent cell, covering device-specific
     * timing, ADC, heater, R0 provenance and quality fields when present.
     * This does NOT certify that absent hardware fields were measured.
     */
    private static String rawMqLong(ExperimentsDb db,FieldSessionReportExporter.Bundle b){
        SmartXlsxPackage.Grid g=new SmartXlsxPackage.Grid(
            36,18,16,20,18,16,22,20,20,20,22,20,20,18,18,26,20,90);
        identity(g,b,"MQ — original stored per-sensor source payload");
        g.blank();g.rowStyled(SmartXlsxPackage.WARN,"PROVENANCE",
            "Every row corresponds to an actually stored source sample and MQ channel. "+
            "Original JSON is not normalized; do not infer missing ADC, heater, ppm or R0.");
        g.header("Measurement Session ID","Telemetry ID","t_ms",
            "Sample UTC ms","MQ model","State","Rs original","R0 original",
            "Ratio original","ADC when present","R0 source","R0 UTC",
            "R0 session","MQ scan cycle","Channel offset us",
            "T °C","RH %","Original MQ channel JSON");
        for(String sid:b.measurementSessionIds){
            try(Cursor c=db.getReadableDatabase().rawQuery(
                "SELECT id,t_ms,env_payload,mq_payload,motion_payload "+
                "FROM telemetry_samples WHERE session_id=? ORDER BY t_ms,id",
                new String[]{sid})){
                while(c.moveToNext()){
                    JSONObject env=parseRaw(c.getString(2));
                    JSONObject mq=parseRaw(c.getString(3));
                    JSONObject state=parseRaw(c.getString(4));
                    for(HazRobHardwareProfile.Channel hw:HazRobHardwareProfile.MQ){
                        JSONObject q=mq.optJSONObject(hw.key);
                        g.row(sid,c.getLong(0),c.getLong(1),
                            exactNumber(state,"sample_utc_ms"),hw.key,
                            exactString(q,"state"),
                            exactNumber(q,"rs"),exactNumber(q,"r0"),
                            exactNumber(q,"ratio"),adcFromMq(q),
                            exactString(q,"r0_source"),exactString(q,"r0_utc"),
                            exactString(q,"r0_session"),exactNumber(q,"scan_cycle"),
                            exactNumber(q,"channel_offset_us"),
                            exactNumber(env,"temperature"),exactNumber(env,"humidity"),
                            q==null?null:q.toString());
                    }
                }
            }
        }
        return g.finish();
    }

    private static JSONObject parseRaw(String value){
        try{return value==null?new JSONObject():new JSONObject(value);}
        catch(org.json.JSONException invalid){
            // Never silently synthesize a scientific reading when source JSON
            // cannot be parsed. The immutable database source is untouched.
            throw new IllegalStateException("Corrupt archived MQ telemetry JSON",invalid);
        }
    }
    private static Object exactNumber(JSONObject o,String key){
        if(o==null||!o.has(key)||o.isNull(key))return null;
        Object raw=o.opt(key);
        if(raw instanceof Number){
            double n=((Number)raw).doubleValue();
            return Double.isFinite(n)?raw:null;
        }
        return raw instanceof String?raw:null; // preserve raw string; no forced conversion
    }
    private static String exactString(JSONObject o,String key){
        if(o==null||!o.has(key)||o.isNull(key))return "";
        return String.valueOf(o.opt(key));
    }
    private static Object adcFromMq(JSONObject sensor){
        if(sensor==null)return null;
        // Some ESP32 firmware revisions publish adc_raw, others adc.
        // These are per-channel ONLY, never the aggregate snapshot adc block.
        for(String key:new String[]{"adc_raw","raw_adc","adc","adc_code"}){
            Object v=exactNumber(sensor,key);
            if(v!=null)return v;
        }
        return null;
    }

    private static String measurementRecords(FieldSessionReportExporter.Bundle b){SmartXlsxPackage.Grid g=wide();identity(g,b,"سجل القياسات الفنية والاشتقاقات");g.blank();g.header("Experiment ID","Measurement Session ID","UTC","Protocol","Capture ID","Chemical UID","Reference kind","Known ppm","Approved","Reference name");for(ExperimentQueries.Record r:b.records)g.row(r.id,nz(r.sessionId),nz(r.ts),nz(r.protocol),r.captureId,r.chemicalUid,nz(r.referenceKind),r.knownPpm,r.approved,nz(r.referenceName));return g.finish();}

    private static String contractMetadata(FieldSessionReportExporter.Bundle b){
        SmartXlsxPackage.Grid g=grid();identity(g,b,"عقد ChemCore ↔ SmartReport");g.blank();
        JSONObject m=b.analyticalContract.metadata();
        g.header("العنصر","القيمة","الدلالة");
        g.row("SmartReport contract",m.optString("smartreport_contract_version","—"),"نسخة عقد الاستهلاك في التقارير");
        g.row("SmartReport revision",m.optInt("smartreport_contract_revision",0),"Schema revision");
        g.row("ChemCore contract",m.optString("chemcore_report_contract_version","—"),"نسخة عقد المخرجات العلمية");
        g.row("ChemCore revision",m.optInt("chemcore_report_contract_revision",0),"Schema revision");
        g.row("Ranking semantics",m.optString("ranking_semantics","—"),"الوزن النسبي ليس calibrated probability");
        g.blank();g.section("تغطية القدرات في هذه الجلسة");g.header("Capability","Available","Reason");
        JSONObject caps=m.optJSONObject("capabilities");
        for(SmartReportAnalyticalContract.Capability c:SmartReportAnalyticalContract.Capability.values()){
            JSONObject q=caps==null?null:caps.optJSONObject(c.name());
            g.row(c.name(),q!=null&&q.optBoolean("available",false),q==null?"not_evaluated":q.optString("reason",""));
        }
        return g.finish();
    }

    private static String provenance(FieldSessionReportExporter.Bundle b,ReportIntelligence.ReportPlan plan,JSONObject cc){SmartXlsxPackage.Grid g=grid();identity(g,b,"المنهجية ومصدر القرار");g.blank();JSONObject lib=cc==null?null:cc.optJSONObject("library");g.header("العنصر","القيمة");g.row("HazRob version",ReleaseAudit.VERSION);g.row("Hardware revision","HR069");g.row("SmartReport contract",SmartReportAnalyticalContract.VERSION);g.row("ChemCore report contract",com.servoc.app.chemcore.ChemCoreReportContract.VERSION);g.row("Report mode",plan.mode.name());g.row("Primary analytical measurement",b.primaryMeasurementSessionId);g.row("Result schema",cc==null?"—":cc.optString("schema","—"));g.row("Model version",cc==null?"—":cc.optString("model_version","—"));g.row("Feature schema",cc==null?"—":cc.optString("feature_schema","—"));g.row("Sampling profile",cc==null?"—":cc.optString("sampling_profile_id","—"));g.row("Evidence tier",cc==null?"—":cc.optString("evidence_tier","—"));g.row("Library version",lib==null?"—":lib.optString("version","—"));g.row("Library SHA-256",lib==null?"—":lib.optString("sha256","—"));g.row("Library source commit",lib==null?"—":lib.optString("source_commit","—"));g.blank();g.section("قواعد التفسير");g.row("1","Posterior/ranking weight ليس احتمالًا معايرًا لوجود المادة.");g.row("2","Session support index دليل إيجابي موزون وليس probability.");g.row("3","Screening-fit ppm سياق تحليلي/خطر فقط حتى اجتياز بوابة M4.");g.row("4","لا يتم متوسط PPM بين جلسات مستقلة أو دمج سلاسلها زمنيًا.");g.row("5","Tracking وظيفة تشغيلية وغير مدرجة في التقرير.");return g.finish();}

    private static SmartXlsxPackage.Grid grid(){return new SmartXlsxPackage.Grid(30,44,58);}
    private static SmartXlsxPackage.Grid wide(){return new SmartXlsxPackage.Grid(10,28,18,16,24,22,16,16,20,14,28,30,16,28,16,14,24,20);}
    private static void identity(SmartXlsxPackage.Grid g,FieldSessionReportExporter.Bundle b,String title){g.title(title);g.header("Site","Site ID","Field Session","Field Session ID");g.row(b.site.name,b.site.id,b.fieldSession.name,b.fieldSession.id);}
    private static void appendRefs(SmartXlsxPackage.Grid g,int rank,String name,String type,JSONArray refs){if(refs==null)return;for(int i=0;i<refs.length();i++){JSONObject r=refs.optJSONObject(i);if(r==null)continue;g.row(rank,name,type,r.optString("authority",""),r.optString("jurisdiction",""),r.optString("limit_type",""),num(r,"value"),r.optString("unit",""),num(r,"duration_min"),r.optString("notation",""),r.optString("source_id",""),r.optBoolean("comparable_to_candidate_ppm",false),firstFinite(r,"ratio_to_reference","ratio_to_boundary"),firstFinite(r,"percent_of_reference","percent_of_boundary"),r.optString("interpretation",""));}}
    private static String evidenceTypes(JSONObject q){JSONArray a=q==null?null:q.optJSONArray("evidence_observations");List<String> types=new ArrayList<>();if(a!=null)for(int i=0;i<a.length();i++){JSONObject e=a.optJSONObject(i);if(e==null)continue;String t=e.optString("type","UNKNOWN");if(!types.contains(t))types.add(t);}StringBuilder s=new StringBuilder();for(String t:types){if(s.length()>0)s.append(", ");s.append(t);}return s.toString();}
    private static String sensorKeys(JSONObject e){JSONArray a=e==null?null:e.optJSONArray("sensor_keys");if(a==null)return"";StringBuilder s=new StringBuilder();for(int i=0;i<a.length();i++){String k=a.optString(i,"");if(k.isEmpty())continue;if(s.length()>0)s.append(", ");s.append(k);}return s.toString();}
    private static boolean isQspr(JSONObject e){if(e==null)return false;String t=e.optString("type","");String f=e.optString("family_id","");return "QSPR".equalsIgnoreCase(t)||f.toUpperCase(Locale.US).contains("PUBCHEM_QSPR");}
    private static int qsprCount(JSONObject q,boolean insideOnly){JSONArray a=q==null?null:q.optJSONArray("evidence_observations");int n=0;if(a!=null)for(int i=0;i<a.length();i++){JSONObject e=a.optJSONObject(i);if(isQspr(e)&&(!insideOnly||e.optBoolean("inside_domain",false)))n++;}return n;}
    private static Object qsprMeanSupport(JSONObject q){JSONArray a=q==null?null:q.optJSONArray("evidence_observations");double sum=0;int n=0;if(a!=null)for(int i=0;i<a.length();i++){JSONObject e=a.optJSONObject(i);if(!isQspr(e))continue;double w=e.optDouble("support_weight",Double.NaN);if(Double.isFinite(w)){sum+=w;n++;}}return n>0?sum/n:null;}
    private static String qsprLineage(JSONObject q){JSONArray a=q==null?null:q.optJSONArray("evidence_observations");if(a==null)return"";for(int i=0;i<a.length();i++){JSONObject e=a.optJSONObject(i);if(!isQspr(e))continue;String f=e.optString("family_id",""),s=e.optString("source_id","");return f+(f.isEmpty()||s.isEmpty()?"":" • ")+s;}return"";}
    private static String label(JSONObject q){String ar=q.optString("name_ar","").trim(),en=q.optString("name_en",q.optString("label","")).trim();return ar.isEmpty()?en:(en.isEmpty()?ar:ar+" / "+en);}
    private static String valueUnit(JSONObject q){Object v=num(q,"value");return (v==null?"—":String.valueOf(v))+" "+q.optString("unit","");}
    private static Object firstFinite(JSONObject q,String... keys){if(q==null)return null;for(String k:keys){double v=q.optDouble(k,Double.NaN);if(Double.isFinite(v))return v;}return null;}
    private static Object num(JSONObject q,String k){if(q==null||q.isNull(k))return null;double v=q.optDouble(k,Double.NaN);return Double.isFinite(v)?v:null;}
    private static Object finiteObj(JSONObject q,String k){return num(q,k);}
    private static Object finite(double v){return Double.isFinite(v)?v:null;}
    private static String nz(String s){return s==null?"":s;}
}