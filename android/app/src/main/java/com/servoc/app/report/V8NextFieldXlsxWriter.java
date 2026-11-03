package com.servoc.app.report;

import com.hazrob.tank.chemcore.next.CoreContracts;
import com.servoc.app.chemcore.nextintegration.NextDecisionStore;
import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.data.SessionSeries;
import com.servoc.app.hardware.HazRobHardwareProfile;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Approved SmartXlsxPackage V7 visual system, but ALL analytical content
 * comes from the ONE immutable ChemCore Next source receipt.
 * Raw environment, ADS, MQ and motion stay exactly as archived in SQLite.
 * No legacy 144/posterior/M4/V6 decision is mixed into a Next workbook.
 */
final class V8NextFieldXlsxWriter {
    private V8NextFieldXlsxWriter(){}
    private static SmartXlsxPackage.Grid grid(double... widths){
        return new SmartXlsxPackage.Grid(widths);
    }
    private static SmartXlsxPackage.Grid begin(
            FieldSessionReportExporter.Bundle b,String title,double... widths){
        SmartXlsxPackage.Grid g=grid(widths);
        SmartFieldSessionXlsxWriter.identity(g,b,title);
        g.blank();return g;
    }
    static void write(OutputStream out,ExperimentsDb db,
                      FieldSessionReportExporter.Bundle b,
                      NextDecisionStore.Receipt next)throws Exception{
        if(out==null||db==null||b==null||next==null)
            throw new IllegalArgumentException("Next Excel archived source required");
        V8NextChartRenderer.requireSameSession(next,b.primaryMeasurementSessionId);
        if(b.measurementSessionIds.size()!=1)
            throw new IllegalStateException("V8 field XLSX requires one approved measurement");
        // Read once from SQLite and verify decision+archive+source math before any bytes are written.
        V8NextDualReferenceXlsxLedger dual=V8NextDualReferenceXlsxLedger.load(
            db,b.primaryMeasurementSessionId,next);
        List<SmartXlsxPackage.Sheet> sheets=new ArrayList<>();
        // Exactly 16 approved base/research worksheet slots, plus 4 original
        // per-timepoint raw worksheets: existing workbook style is unchanged.
        sheets.add(new SmartXlsxPackage.Sheet("الملخص التنفيذي",executive(b,next)));
        sheets.add(new SmartXlsxPackage.Sheet("بيانات الجلسة",
            SmartFieldSessionXlsxWriter.sessionMetadata(b)));
        sheets.add(new SmartXlsxPackage.Sheet("دورات القياس",
            SmartFieldSessionXlsxWriter.cycleLedger(db,b)));
        sheets.add(new SmartXlsxPackage.Sheet("المرشحون Next",candidates(b,next)));
        sheets.add(new SmartXlsxPackage.Sheet("مصادر Next المستقلة",roots(b,next)));
        sheets.add(new SmartXlsxPackage.Sheet("الأدلة التحليلية",evidence(b,next)));
        sheets.add(new SmartXlsxPackage.Sheet("حدود التأهيل",qualification(b,next,dual)));
        sheets.add(new SmartXlsxPackage.Sheet("الجودة وعدم اليقين",quality(b,next)));
        sheets.add(new SmartXlsxPackage.Sheet("السلسلة الزمنية",timeline(b,db)));
        sheets.add(new SmartXlsxPackage.Sheet("MQ الثمانية الخام",
            SmartFieldSessionXlsxWriter.rawMqWide(db,b)));
        sheets.add(new SmartXlsxPackage.Sheet("MQ الأصل لكل حساس",
            SmartFieldSessionXlsxWriter.rawMqLong(db,b)));
        sheets.add(new SmartXlsxPackage.Sheet("القياسات الفنية",
            SmartFieldSessionXlsxWriter.measurementRecords(b)));
        sheets.add(new SmartXlsxPackage.Sheet("عقد Next",contract(b,next)));
        sheets.add(new SmartXlsxPackage.Sheet("المنهجية والمصدر",provenance(b,next,dual)));
        sheets.add(new SmartXlsxPackage.Sheet("تشخيص الخبراء",diagnostics(b,next,dual)));
        sheets.add(new SmartXlsxPackage.Sheet("خريطة الأدلة Next",map(b,next)));
        V8FieldFullTelemetryXlsx.append(sheets,db,b);
        if(sheets.size()!=20)throw new IllegalStateException("20 source-defined Next sheets required");
        SmartXlsxPackage.write(out,sheets);
    }
    private static String executive(FieldSessionReportExporter.Bundle b,
                                    NextDecisionStore.Receipt next){
        SmartXlsxPackage.Grid g=begin(b,"HazRob Tank V8 — الملخص التنفيذي ChemCore Next",33,49,75);
        g.section("القرار البحثي المؤرشف");g.header("المؤشر","القيمة","المنهجية");
        var decision=next.originalDecision;
        g.row("Field session",b.fieldSession.name,"قياس معتمد واحد للجلسة");
        g.row("Measurement session",next.sessionId,"المصدر نفسه في PDF و Excel");
        g.row("Decision",next.status.name(),"قرار ChemCore Next المؤرشف");
        g.row("Top research UID",firstUid(next),"مرشح قائم على أدلة مصادر؛ ليس هوية مؤكدة");
        g.row("Research candidate count",decision.rankedEligibleCandidates.size(),
            "قائمة مرشحين مبنية على مصادر مستقلة");
        g.row("Evidence nodes",decision.evidence.size(),"ليست أصواتًا مستقلة إن اتحد الجذر");
        g.row("Independent source roots",allRoots(next).size(),
            "كل جذر يُحصى مرة واحدة");
        g.row("Actual MQ readings",next.sourceSnapshot.readings.size(),
            "عدد قنوات اللقطة الأصلية؛ انظر حالة الجودة لكل قناة");
        g.row("Valid MQ channels",validCount(next),"VALID only");
        g.row("PPM reportable",decision.reportablePpm,"G3/calibration not established");
        g.row("Physical G3 verified",decision.physicallyValidatedG3,
            "الاختبار البرمجي لا يكافئ تأهيل الجهاز");
        g.row("Stable SHA256",decision.stableManifestHash,
            "بصمة مكتبة Stable المرتبطة بالقرار");
        g.row("Snapshot SHA256",next.snapshotSha256,"تحقق إسناد القرار إلى اللقطة");
        g.blank();g.section("حدود التفسير");
        g.rowStyled(SmartXlsxPackage.WARN,"لا توجد شهادة هوية أو امتثال",
            "المرشح البحثي لا يثبت جزيئًا أو تركيزًا معتمدًا ولا يُنتج إنذارًا مهنيًا.");
        for(String problem:decision.problems)g.rowStyled(SmartXlsxPackage.WARN,
            "Scientific limitation",problem);
        return g.finish();
    }
    private static String candidates(FieldSessionReportExporter.Bundle b,
                                     NextDecisionStore.Receipt next){
        SmartXlsxPackage.Grid g=begin(b,"مرشحو ChemCore Next — لا هويات مؤكدة",
            8,25,58,20,52,80);
        g.header("Rank","Canonical UID","Independent source roots","Roots #",
            "Evidence summary","Decision SHA256");
        int i=1;
        for(CoreContracts.Candidate candidate:next.originalDecision.rankedEligibleCandidates){
            Set<String> roots=new LinkedHashSet<>(candidate.sourceRoots);
            g.row(i++,candidate.canonicalChemicalUid,String.join("; ",roots),
                roots.size(),candidate.evidenceSummary,next.snapshotSha256);
        }
        if(i==1)g.rowStyled(SmartXlsxPackage.WARN,"No ranked candidate",
            "ChemCore Next abstained or no supported source candidate");
        g.rowStyled(SmartXlsxPackage.WARN,"الترتيب",
            "لا Posterior، لا CAS مستنتج، لا نسبة ثقة ولا PPM غير موجودة في أصل القرار.");
        return g.finish();
    }
    private static Map<String,Set<String>> sourceSensors(NextDecisionStore.Receipt next){
        Map<String,Set<String>> roots=new LinkedHashMap<>();
        for(CoreContracts.EvidenceNode e:next.originalDecision.evidence)
            roots.computeIfAbsent(e.sourceRootId,k->new LinkedHashSet<>()).add(e.sensorUid);
        return roots;
    }
    private static Set<String> allRoots(NextDecisionStore.Receipt next){
        Set<String> roots=new LinkedHashSet<>();
        for(CoreContracts.EvidenceNode e:next.originalDecision.evidence)
            roots.add(e.sourceRootId);
        return roots;
    }
    private static String roots(FieldSessionReportExporter.Bundle b,
                                NextDecisionStore.Receipt next){
        SmartXlsxPackage.Grid g=begin(b,"جذور المصادر المستقلة — ChemCore Next",
            72,16,88,33);
        g.header("Independent source root","Physical channels #",
            "Actual sensor UIDs","Frozen decision UID");
        var map=sourceSensors(next);
        for(var entry:map.entrySet())g.row(entry.getKey(),
            entry.getValue().size(),String.join("; ",entry.getValue()),next.decisionUid);
        if(map.isEmpty())g.row("NO_SOURCE_EVIDENCE",0,"");
        g.rowStyled(SmartXlsxPackage.WARN,"قاعدة المصدر",
            "تكرار الحساس داخل الدراسة لا ينشئ مصدرًا علميًا مستقلًا.");
        return g.finish();
    }
    private static String evidence(FieldSessionReportExporter.Bundle b,
                                   NextDecisionStore.Receipt next){
        SmartXlsxPackage.Grid g=begin(b,"الأدلة المحفوظة من محرك Next",
            10,65,28,49,27,28,70,70);
        g.header("#","Source root","Expert","Physical sensor UID",
            "Canonical UID","Polarity","Evidence note","Snapshot SHA256");
        int i=1;
        for(CoreContracts.EvidenceNode e:next.originalDecision.evidence)
            g.row(i++,e.sourceRootId,e.expertId,e.sensorUid,
                e.canonicalChemicalUid,e.polarity.name(),e.note,next.snapshotSha256);
        if(i==1)g.rowStyled(SmartXlsxPackage.WARN,"No source evidence",
            "No models or qualified sensor observations passed the Next source condition");
        return g.finish();
    }
    private static String qualification(FieldSessionReportExporter.Bundle b,
                                        NextDecisionStore.Receipt next,
                                        V8NextDualReferenceXlsxLedger dual){
        SmartXlsxPackage.Grid g=begin(b,"معايرة القنوات وصلاحية الاستنتاج",
            54,43,54);
        g.header("Instrument/qualification item","Archived value","Meaning");
        g.row("Next decision state",next.status.name(),"Source-only");
        g.row("R_air reference",next.sourceSnapshot.readings.stream()
            .filter(q->q.quality==CoreContracts.SampleQuality.VALID).count(),
            "Valid sensor reference count");
        g.row("Stable SHA256",next.sourceSnapshot.stableManifestHash,"verified source snapshot");
        g.row("Reportable PPM",next.originalDecision.reportablePpm,
            "No numerical values manufactured at export");
        g.row("Molecular identity validated",false,"No source-only inference equals identity");
        g.row("G3 physical qualification",next.originalDecision.physicallyValidatedG3,
            "Physical Tank test required");
        g.row("Dual R0 scientific ledger",dual.status,
            "Read from SQLite, bound to original Next decision and source archive");
        g.row("Derived R0 source rows",dual.available?13:0,
            "R_air stays physical clean-air mean; R0_source=R_air/K for MQSL only");
        g.row("Conditional source ppm calculations",dual.computedCount,
            "Source examples only; NOT certified concentration, exposure or gas identity");
        g.row("Occupational exposure limit",null,"No evaluated occupational comparison from Next");
        g.row("LFL/UEL compliance",null,"No certified flammable gas concentration");
        g.blank();
        g.rowStyled(SmartXlsxPackage.WARN,"مصادر المصنع والأبحاث",
            "محتوى المنحنيات والفرضيات يبقى ظاهرًا في جذور المصدر والأدلة؛ لا يُحجب دون توثيق، "+
            "لكن صلاحية المقارنة المهنية تعتمد على شروط المعايرة الفعلية.");
        return g.finish();
    }
    private static int validCount(NextDecisionStore.Receipt next){
        int n=0;for(CoreContracts.Reading r:next.sourceSnapshot.readings)
            if(r.quality==CoreContracts.SampleQuality.VALID)n++;return n;
    }
    private static String quality(FieldSessionReportExporter.Bundle b,
                                  NextDecisionStore.Receipt next){
        SmartXlsxPackage.Grid g=begin(b,"جودة الحساسات والأدلة الفعلية",
            40,20,18,18,20,24,22,28,43,30,58);
        g.header("Physical sensor UID","Raw ADC","Voltage V","Rs kΩ","R_air kΩ",
            "Rs/R_air","R0 source kΩ","Reference definition","Heater phase","Quality",
            "Source decision SHA");
        for(CoreContracts.Reading r:next.sourceSnapshot.readings){
            double ratio=r.airRatioOrNaN();
            g.row(r.sensorUid,r.rawAdc,r.voltageV,r.rsKohm,r.rAirKohm,
                Double.isFinite(ratio)?ratio:null,r.r0SourceKohm,
                r.r0Definition.name(),r.heaterPhase,r.quality.name(),next.snapshotSha256);
        }
        g.rowStyled(SmartXlsxPackage.WARN,"Missing/invalid",
            "القيم غير الموجودة تظل فارغة؛ يمنع استبدالها بصفر أو بمنحنى افتراضي.");
        return g.finish();
    }
    private static Object value(JSONObject q,String key){
        if(q==null||!q.has(key)||q.isNull(key))return null;
        double d=q.optDouble(key,Double.NaN);
        return Double.isFinite(d)?d:null;
    }
    private static String timeline(FieldSessionReportExporter.Bundle b,ExperimentsDb db){
        SmartXlsxPackage.Grid g=begin(b,"التتابع الزمني الأصلي MQ + البيئة",
            32,18,16,18,18,18,18,18,18,18,18,18,18);
        g.rowStyled(SmartXlsxPackage.WARN,"تعريف Rs",
            "قيمة Rs الأصلية للوحدة المبلّغ عنها؛ لا يعاد توليد Rs/R0 أو ppm من بيانات لم تُقَس.");
        g.header("Measurement session ID","t_ms","Temperature °C","Humidity %RH",
            "Pressure hPa","MQ2 Rs","MQ3 Rs","MQ4 Rs","MQ5 Rs","MQ7 Rs","MQ8 Rs",
            "MQ9 Rs","MQ135 Rs");
        SessionSeries.Series series=SessionSeries.byId(db,b.primaryMeasurementSessionId);
        if(series!=null)for(SessionSeries.Sample sample:series.samples){
            Object[] row=new Object[13];row[0]=b.primaryMeasurementSessionId;row[1]=sample.tMs;
            row[2]=value(sample.env,"temperature");row[3]=value(sample.env,"humidity");
            row[4]=value(sample.env,"pressure");
            int j=5;
            for(HazRobHardwareProfile.Channel hw:HazRobHardwareProfile.MQ){
                JSONObject q=SessionSeries.sensor(sample,hw.key);
                row[j++]=value(q,"rs");
            }
            g.row(row);
        }
        return g.finish();
    }
    private static String contract(FieldSessionReportExporter.Bundle b,
                                   NextDecisionStore.Receipt next){
        SmartXlsxPackage.Grid g=begin(b,"عقد تحليل ChemCore Next المؤرشف",
            45,83,68);
        g.header("Key","Frozen value","Data lineage");
        g.row("Engine version",next.engineVersion,"Source-replay receipt");
        g.row("Decision UID",next.decisionUid,"Not derived from latest diagnostics");
        g.row("Snapshot UID",next.snapshotUid,"Frozen within ONE measurement");
        g.row("Original measurement",next.sessionId,"FieldSessionReportExporter");
        g.row("Snapshot SHA256",next.snapshotSha256,"Source integrity gate");
        g.row("Stable SHA256",next.sourceSnapshot.stableManifestHash,"Fixed Stable data");
        g.row("Decision status",next.status.name(),"ChemCore Next authoritative");
        g.row("Model candidates",next.originalDecision.rankedEligibleCandidates.size(),"All eligible UID entries");
        g.row("Evidence nodes",next.originalDecision.evidence.size(),"Each source node separately");
        g.row("Source roots",allRoots(next).size(),"Deduplicated by root ID");
        g.row("Source timestamp UTC ms",next.createdUtcMs,"Stored at analysis time");
        g.row("Physical G3",next.originalDecision.physicallyValidatedG3,"Not promoted by PDF/XLSX");
        return g.finish();
    }
    private static String provenance(FieldSessionReportExporter.Bundle b,
                                     NextDecisionStore.Receipt next,
                                     V8NextDualReferenceXlsxLedger dual){
        SmartXlsxPackage.Grid g=begin(b,"المنهجية ومرجع ChemCore Next",
            55,112);
        g.header("Metadata","Original value");
        g.row("Report family","V8 Field one-approved-measurement Excel");
        g.row("Analysis source","ChemCore Next immutable replay, NOT legacy ChemCore");
        g.row("Snapshot SHA256",next.snapshotSha256);
        g.row("Decision UID",next.decisionUid);
        g.row("Engine version",next.engineVersion);
        g.row("Stable SHA256",next.sourceSnapshot.stableManifestHash);
        g.row("Site",b.site.name);
        g.row("Field session",b.fieldSession.name);
        g.row("Measurement session",next.sessionId);
        g.row("Dual R0 research ledger",dual.status);
        g.row("Source archive SHA256",dual.available?dual.sourceArchiveSha256:"NOT_CAPTURED");
        g.row("P11.3 source models",dual.available?dual.rows.length():0);
        g.row("R0 source formula","R0_source=R_air/K; ppm=A*(K*Rs/R_air)^B");
        g.row("Derived source K","MQSensorsLib pinned original example coefficients, source-specific");
        g.row("Research safety policy","No G3, no operator gas identity, no certified ppm or alarm");
        g.row("Allowed source count",b.measurementSessionIds.size());
        g.row("Time-series","Archived telemetry_samples only; preserved IDs/timestamps");
        g.row("Technology","Multiple source experts, source-root deduplication and open-world abstention");
        g.row("Uncertainty","Next decision state; no invented probabilities");
        g.row("Emissions IFR/IFC","Not part of the Field Session chemical identification report");
        g.row("Workbook schema","hazrob-tank-v8-next-field-xlsx/1");
        g.rowStyled(SmartXlsxPackage.WARN,"Safety",
            "No extrapolated occupational PPM / calibrated molecular identity / G3 qualification.");
        return g.finish();
    }
    /**
     * P11.3: retain the original expert/problem records, then append every
     * saved source equation (49 digitized + 13 MQSensorsLib). Original source
     * K, the untouched R_air and the derived R0_source remain visibly
     * separate. No output is recomputed at Excel-export time.
     */
    private static String diagnostics(FieldSessionReportExporter.Bundle b,
                                      NextDecisionStore.Receipt next,
                                      V8NextDualReferenceXlsxLedger dual){
        SmartXlsxPackage.Grid g=begin(b,
            "أسباب الامتناع ومراجع R0 المزدوجة — ChemCore Next",
            21,34,96,46,54,20,20,20,24,24,24,24,
            24,30,55,23,15,15,52,69,69,90);
        g.header("#","Type","Exact original note");
        int i=1;
        for(String p:next.originalDecision.problems)g.row(i++,"NEXT_PROBLEM",p);
        for(CoreContracts.Reading q:next.sourceSnapshot.readings)
            if(q.quality!=CoreContracts.SampleQuality.VALID)
                g.row(i++,"SENSOR_"+q.quality.name(),q.sensorUid+" / "+q.heaterPhase);
        if(i==1)g.row("INFO","No recorded issues",
            "No guarantee of physical calibration");
        g.blank();g.section("P11.3 — أصول معادلات المصادر والمرجع المزدوج البحثي");
        g.rowStyled(SmartXlsxPackage.WARN,"Scope",
            "R_air أصلي ومحفوظ؛ R0_source مشتق وفق المصدر. PPM حساب بحثي لا يثبت هوية الغاز أو حدود التعرض أو G3.");
        g.row("Ledger",dual.status);
        g.row("Decision UID",next.decisionUid);
        g.row("Source snapshot SHA256",next.snapshotSha256);
        g.row("Source archive SHA256",
            dual.available?dual.sourceArchiveSha256:"NOT_CAPTURED_PRE_P11");
        if(!dual.available){
            g.rowStyled(SmartXlsxPackage.WARN,"No dual-reference source ledger",
                "Historical Next decision retained; no replacement or recalculation from newer MQ telemetry.");
        }else{
            g.header("Curve ID","Sensor","Source gas/class","Source root",
                "Original equation","A","B","K source clean-air","Rs kΩ",
                "Device R_air kΩ","R0 source derived kΩ","Rs/R_air",
                "Rs/R0 source","Source ppm RESEARCH","Source status",
                "Source range verified","Physical G3","Safety alarm",
                "Source blob","Source snapshot SHA256","Archive SHA256",
                "Caveat / source limitations");
            for(int k=0;k<dual.rows.length();k++){
                JSONObject model=dual.rows.optJSONObject(k);
                if(model==null)continue; // already rejected by strict reader
                g.row(model.optString("source_curve_id",""),
                    model.optString("sensor_model",""),
                    model.optString("gas_or_class",""),
                    model.optString("source_root",""),
                    model.optString("source_equation",""),
                    scientificNumber(model,"a"),scientificNumber(model,"b"),
                    scientificNumber(model,"source_example_clean_air_K"),
                    scientificNumber(model,"rs_kohm"),
                    scientificNumber(model,"device_air_reference_kohm"),
                    scientificNumber(model,"source_r0_research_kohm"),
                    scientificNumber(model,"observed_rs_over_air"),
                    scientificNumber(model,"observed_rs_r0"),
                    scientificNumber(model,"estimated_ppm"),
                    model.optString("status","NOT_EVALUABLE"),
                    model.optBoolean("source_domain_primary_verified",false),
                    model.optBoolean("physical_g3_validated",false),
                    model.optBoolean("safety_alarm_authorized",false),
                    model.optString("source_blob",""),
                    next.snapshotSha256,dual.sourceArchiveSha256,
                    model.optString("caveat",
                        model.optString("source_scale_origin",
                            "Original source R0 / source-heater match not independently established")));
            }
            g.rowStyled(SmartXlsxPackage.WARN,"MQSensorsLib K",
                "K ليس قيمة موحّدة لكل المنحنيات؛ لا يُطبّق على منحنيات MQDataScience أو قيم R_air المخزنة.");
        }
        return g.finish();
    }
    private static Object scientificNumber(JSONObject model,String field){
        if(model==null||!model.has(field)||model.isNull(field))return null;
        double n=model.optDouble(field,Double.NaN);
        return Double.isFinite(n)?n:null;
    }
    private static String map(FieldSessionReportExporter.Bundle b,
                              NextDecisionStore.Receipt next){
        SmartXlsxPackage.Grid g=begin(b,"جذور المصادر ↔ UID ↔ نماذج الخبراء",
            76,26,33,49,45,26);
        g.header("Source root","Canonical UID","Expert","Physical sensor",
            "Evidence polarity","Decision");
        int count=0;
        for(CoreContracts.EvidenceNode e:next.originalDecision.evidence){
            g.row(e.sourceRootId,e.canonicalChemicalUid,e.expertId,e.sensorUid,
                e.polarity.name(),next.status.name());
            count++;
        }
        if(count==0)g.row("NO_INDEPENDENT_EVIDENCE",null,null,null,null,
            next.status.name());
        return g.finish();
    }
    private static String firstUid(NextDecisionStore.Receipt next){
        return next.originalDecision.rankedEligibleCandidates.isEmpty()?"UNKNOWN":
            "UID "+next.originalDecision.rankedEligibleCandidates.get(0).canonicalChemicalUid;
    }
}
