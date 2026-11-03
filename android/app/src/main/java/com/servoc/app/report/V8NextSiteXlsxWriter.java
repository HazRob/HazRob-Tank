package com.servoc.app.report;

import com.hazrob.tank.chemcore.next.CoreContracts;
import com.servoc.app.chemcore.nextintegration.NextDecisionStore;
import com.servoc.app.field.FieldCycleRepository;
import org.json.JSONObject;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Six approved-format XLSX worksheets for a multi-session SITE where at
 * least one session has ChemCore Next. Each source-root and UID counts at
 * most ONCE per independent approved session; no pooled gas concentration,
 * old 144 probability, guessed M4 or retrospective re-analysis.
 *
 * Historical rows are labelled LEGACY_ARCHIVE without claiming Next lineage.
 */
final class V8NextSiteXlsxWriter {
    private V8NextSiteXlsxWriter(){}
    private static SmartXlsxPackage.Grid sheet(SiteSummaryReportExporter.SiteBundle b,
                                               String title,double... widths){
        SmartXlsxPackage.Grid g=new SmartXlsxPackage.Grid(widths);
        g.title(title);
        g.header("Site","Site ID","Approved field sessions");
        g.row(b.site.name,b.site.id,b.sessions.size());
        g.blank();return g;
    }
    static void write(OutputStream out,SiteSummaryReportExporter.SiteBundle b)throws Exception {
        if(out==null||b==null)throw new IllegalArgumentException("site Next XLSX required");
        if(!NextSiteEvidenceCharts.hasAnyNext(b.sessions))
            throw new IllegalArgumentException("Next site workbook requires a real Next receipt");
        // Each measurement's P11 ledger is verified against ITS OWN immutable
        // Next snapshot and archive. Never average/merge ppm between sessions.
        Map<String,V8NextDualReferenceXlsxLedger> dualByMeasurement=
            new LinkedHashMap<>();
        for(var session:b.sessions)if(session.hasNext){
            String measurement=session.bundle.primaryMeasurementSessionId;
            if(dualByMeasurement.put(measurement,
                V8NextDualReferenceXlsxLedger.load(
                    b.sourceDatabase,measurement,session.next))!=null)
                throw new IllegalStateException("P11_SITE_DUPLICATE_MEASUREMENT");
        }
        List<SmartXlsxPackage.Sheet> sheets=new ArrayList<>();
        sheets.add(new SmartXlsxPackage.Sheet("الملخص التنفيذي",summary(b,dualByMeasurement)));
        sheets.add(new SmartXlsxPackage.Sheet("الجلسات",sessions(b)));
        sheets.add(new SmartXlsxPackage.Sheet("دورات الموقع",cycles(b)));
        sheets.add(new SmartXlsxPackage.Sheet("تكرار مصادر Next",roots(b)));
        sheets.add(new SmartXlsxPackage.Sheet("أدلة Next والجودة",evidence(b,dualByMeasurement)));
        sheets.add(new SmartXlsxPackage.Sheet("المنهجية والمصدر",methodology(b,dualByMeasurement)));
        if(sheets.size()!=6)throw new IllegalStateException("site six-sheet design changed");
        SmartXlsxPackage.write(out,sheets);
    }
    private static int nextCount(SiteSummaryReportExporter.SiteBundle b){
        int c=0;for(var s:b.sessions)if(s.hasNext)c++;return c;
    }
    private static int sources(SiteSummaryReportExporter.SiteBundle b){
        Set<String> roots=new LinkedHashSet<>();
        for(var s:b.sessions)if(s.hasNext)
            for(var e:s.next.originalDecision.evidence)roots.add(e.sourceRootId);
        return roots.size();
    }
    private static String summary(SiteSummaryReportExporter.SiteBundle b,
        Map<String,V8NextDualReferenceXlsxLedger> dualByMeasurement){
        SmartXlsxPackage.Grid g=sheet(b,"HazRob Tank V8 — ملخص الموقع / ChemCore Next",
            39,32,80);
        g.section("مؤشرات القرار المؤرشف");g.header("Metric","Value","Interpretation");
        g.row("Independent approved sessions",b.sessions.size(),
            "Several Field Sessions, ONE accepted measurement per Field Session");
        g.row("ChemCore Next sessions",nextCount(b),
            "No inference is imputed to historical legacy sessions");
        g.row("Legacy archive sessions",b.sessions.size()-nextCount(b),
            "Historical analysis is not converted into Next evidence");
        g.row("Independent source roots across site",sources(b),
            "Each original source root is counted once");
        int n=0;
        for(var s:b.sessions)if(s.hasNext)
            n+=s.next.originalDecision.rankedEligibleCandidates.size();
        g.row("Next research UID entries across sessions",n,
            "Occurrences are session-specific, NOT chemical identification");
        g.row("PPM pooled",null,"Not calculated between different field sessions");
        g.row("G3 qualification",false,"Cloud Android tests do not certify sensor calibration");
        long sourceBound=dualByMeasurement.values().stream().filter(x->x.available).count();
        g.row("Sessions with archived dual R0 source ledger",sourceBound,
            "Per approved measurement, no pooled gas identity or PPM");
        g.row("Sessions without P11 ledger",dualByMeasurement.size()-sourceBound,
            "Historical Next data stays valid but lacks source-specific R0");
        g.blank();g.section("Recorded source and scientific limits");
        int i=1;for(var s:b.sessions){
            if(!s.hasNext)continue;
            g.row("Next #"+i++,s.next.decisionUid,s.next.status.name()+
                " / "+s.next.snapshotSha256);
        }
        g.rowStyled(SmartXlsxPackage.WARN,"ملاحظة",
            "لا يجوز دمج تركيزات جلسات منفصلة أو اعتبار أدلة البحث هوية مؤكدة.");
        return g.finish();
    }
    private static String sessions(SiteSummaryReportExporter.SiteBundle b){
        SmartXlsxPackage.Grid g=sheet(b,"صف لكل جلسة مستقلة — Next أو إرث موثق",
            8,39,52,24,28,20,26,37,25,24,24,65,80);
        g.header("#","Field session","Field ID","Status","Source route",
            "Decision","Top research UID","Next decision UID",
            "Evidence roots","Next evidence nodes","PPM certifiable",
            "Original measurement ID","Snapshot SHA256");
        int index=1;
        for(var s:b.sessions){
            Set<String> roots=new LinkedHashSet<>();
            if(s.hasNext)for(var e:s.next.originalDecision.evidence)roots.add(e.sourceRootId);
            g.row(index++,s.bundle.fieldSession.name,s.bundle.fieldSession.id,
                s.bundle.fieldSession.status,
                s.hasNext?"CHEMCORE_NEXT":"LEGACY_ARCHIVE",
                s.hasNext?s.next.status.name():s.outcome,
                s.hasNext?NextSiteEvidenceCharts.topUid(s.next):"LEGACY_ARCHIVE",
                s.hasNext?s.next.decisionUid:null,
                s.hasNext?roots.size():null,
                s.hasNext?s.next.originalDecision.evidence.size():null,
                s.hasNext?s.next.originalDecision.reportablePpm:null,
                s.bundle.primaryMeasurementSessionId,
                s.hasNext?s.next.snapshotSha256:null);
        }
        g.rowStyled(SmartXlsxPackage.WARN,"Interpretation",
            "A historical session is clearly marked; old chemical/PPM cannot be attributed to Next.");
        return g.finish();
    }
    private static String cycles(SiteSummaryReportExporter.SiteBundle b){
        SmartXlsxPackage.Grid g=sheet(b,"سجل جميع المحاولات • قياس معتمد واحد لكل جلسة",
            39,50,16,16,18,38,23,26,28,28,25,33,75,27);
        g.header("Field session","Session ID","Sequence","Attempt",
            "Cycle state","Measurement ID","Approved?","Started UTC",
            "Completed UTC","Approved UTC","Device","Next decision",
            "Evidence SHA256","Failure/retry reason");
        for(var s:b.sessions)for(var c:s.cycles){
            FieldCycleRepository.Cycle cycle=c.cycle;
            g.row(s.bundle.fieldSession.name,s.bundle.fieldSession.id,
                cycle.sequence,cycle.attempt,cycle.state,cycle.measurementId,
                cycle.approved(),cycle.started,cycle.completed,cycle.approved,
                cycle.deviceId,c.next==null?null:c.next.decisionUid,
                cycle.evidenceSha,cycle.failReason);
        }
        g.rowStyled(SmartXlsxPackage.WARN,"Audit trail",
            "Failed/superseded attempts remain in the audit only; they are not new certified measurements.");
        return g.finish();
    }
    private static String roots(SiteSummaryReportExporter.SiteBundle b){
        SmartXlsxPackage.Grid g=sheet(b,"تكرار جذور مصادر ChemCore Next المستقلة",
            76,21,77,33);
        g.header("Original source-root ID","Independent sessions",
            "Original field session IDs","Root interpretation");
        Map<String,Set<String>> map=new LinkedHashMap<>();
        for(var s:b.sessions){
            if(!s.hasNext)continue;
            Set<String> unique=new HashSet<>();
            for(var e:s.next.originalDecision.evidence)unique.add(e.sourceRootId);
            for(String root:unique)
                map.computeIfAbsent(root,k->new LinkedHashSet<>())
                    .add(s.bundle.fieldSession.id);
        }
        for(var e:map.entrySet())
            g.row(e.getKey(),e.getValue().size(),String.join("; ",e.getValue()),
                "Research provenance, not gas identity");
        if(map.isEmpty())g.row("NO_NEXT_ROOTS",0,null,"No positive model");
        g.rowStyled(SmartXlsxPackage.WARN,"Root independence",
            "Repeated sensors and model curves from one study are one source per session.");
        return g.finish();
    }
    private static String evidence(SiteSummaryReportExporter.SiteBundle b,
        Map<String,V8NextDualReferenceXlsxLedger> dualByMeasurement){
        SmartXlsxPackage.Grid g=sheet(b,"كل أدلة Next الأصلية وحالات القرار",
            38,38,35,53,24,54,30,22,83);
        g.header("Field session ID","Decision UID","Decision state",
            "Source root","Expert","Physical sensor UID",
            "Canonical chemical UID","Polarity","Original evidence note");
        for(var s:b.sessions){
            if(!s.hasNext){
                g.row(s.bundle.fieldSession.id,null,"LEGACY_ARCHIVE",null,null,
                    null,null,null,"Historical data not converted to Next");
                continue;
            }
            if(s.next.originalDecision.evidence.isEmpty())
                g.row(s.bundle.fieldSession.id,s.next.decisionUid,
                    s.next.status.name(),null,null,null,null,null,"No eligible source evidence");
            for(CoreContracts.EvidenceNode e:s.next.originalDecision.evidence)
                g.row(s.bundle.fieldSession.id,s.next.decisionUid,
                    s.next.status.name(),e.sourceRootId,e.expertId,
                    e.sensorUid,e.canonicalChemicalUid,e.polarity.name(),e.note);
        }
        // P11.4: source math remains a PER-SESSION diagnostic, never
        // aggregated into an occupational PPM, LFL, gas identity, or
        // cross-session estimate. Retain all 62 source records per P11 session.
        g.blank();g.section("P11.4 — المرجع المزدوج لكل قياس معتمد مستقل");
        g.header("Field ID","Measurement ID","Next decision UID",
            "Dual R0 status","Original source SHA256",
            "Source curve ID","Physical MQ model","Source R0 K",
            "Device R_air kΩ","Rs kΩ","Source R0 kΩ derived",
            "Rs/R_air","Rs/R0 source","ppm RESEARCH ONLY",
            "Source equation","Source root","Research status",
            "Source blob","Physical G3 verified",
            "Safety alarm authorized","Snapshot SHA256","Source caution");
        for(var session:b.sessions){
            if(!session.hasNext)continue;
            String measurement=session.bundle.primaryMeasurementSessionId;
            V8NextDualReferenceXlsxLedger dual=dualByMeasurement.get(measurement);
            if(dual==null)
                throw new IllegalStateException("P11_SITE_DUAL_R0_LEDGER_RESOLUTION_MISSING");
            if(!dual.available){
                g.row(session.bundle.fieldSession.id,measurement,session.next.decisionUid,
                    dual.status,"NOT_CAPTURED_PRE_P11");
                continue;
            }
            for(int i=0;i<dual.rows.length();i++){
                JSONObject model=dual.rows.optJSONObject(i);
                if(model==null)
                    throw new IllegalStateException("P11_SITE_DUAL_R0_MODEL_ROW_MISSING");
                g.row(session.bundle.fieldSession.id,measurement,session.next.decisionUid,
                    dual.status,dual.sourceArchiveSha256,
                    model.optString("source_curve_id",""),
                    model.optString("sensor_model",""),
                    scientificNumber(model,"source_example_clean_air_K"),
                    scientificNumber(model,"device_air_reference_kohm"),
                    scientificNumber(model,"rs_kohm"),
                    scientificNumber(model,"source_r0_research_kohm"),
                    scientificNumber(model,"observed_rs_over_air"),
                    scientificNumber(model,"observed_rs_r0"),
                    scientificNumber(model,"estimated_ppm"),
                    model.optString("source_equation",""),
                    model.optString("source_root",""),
                    model.optString("status","NOT_EVALUABLE"),
                    model.optString("source_blob",""),
                    model.optBoolean("physical_g3_validated",false),
                    model.optBoolean("safety_alarm_authorized",false),
                    session.next.snapshotSha256,
                    model.optString("caveat","SOURCE_R0_NOT_VERIFIED_FOR_TANK"));
            }
        }
        g.rowStyled(SmartXlsxPackage.WARN,"P11 site interpretation",
            "Each original Field Session is independent: no mean PPM across sessions; source model calculations are not gas detection or an occupational safety alarm.");
        return g.finish();
    }
    private static Object scientificNumber(JSONObject q,String key){
        if(q==null||!q.has(key)||q.isNull(key))return null;
        double n=q.optDouble(key,Double.NaN);
        return Double.isFinite(n)?n:null;
    }
    private static String methodology(SiteSummaryReportExporter.SiteBundle b,
        Map<String,V8NextDualReferenceXlsxLedger> dualByMeasurement){
        SmartXlsxPackage.Grid g=sheet(b,"مصادر العمل وحدود التفسير — ChemCore Next",
            53,120);
        g.header("Rule","Definition");
        g.row("Workbook", "hazrob-tank-v8-next-site-xlsx/1");
        g.row("Report visual", "Original SmartXlsxPackage V7 theme; RTL, dark headers, frozen first row");
        g.row("Site",b.site.id);
        g.row("Independent field sessions",b.sessions.size());
        g.row("Next source session count",nextCount(b));
        g.row("Source of Next","NextDecisionStore.Receipt from approved measurement ID, immutable replay");
        g.row("Site recurrence","One vote per actual source root / physical session, no pooled telemetry");
        g.row("Historical source","Old archive retained and labelled, never represented as ChemCore Next");
        g.row("Concentration","None pooled or averaged across sessions");
        g.row("Dual R0 reference method","Source-specific R0=R_air/K when source definition requires it; device R_air is never overwritten");
        g.row("P11 dual R0 per-session source",
            "Separate 62 research models per accepted measurement, source-hash/decision-bound; no added library of chemicals");
        g.row("P11 source K qualification",
            "MQSensorsLib model K and heater protocol remain unqualified on Tank; no certified source ppm");
        g.row("Occupational compliance","NOT_CERTIFIED");
        g.row("G3","NOT_CERTIFIED");
        g.row("Interpretation","Canonical UIDs are research candidates, not validated molecular identities");
        for(var s:b.sessions)if(s.hasNext){
            g.row("Session "+s.bundle.fieldSession.id,
                s.next.engineVersion+" / Stable SHA256 "+s.next.sourceSnapshot.stableManifestHash+
                " / Decision "+s.next.decisionUid);
        }
        g.rowStyled(SmartXlsxPackage.WARN,"Integrity",
            "A damaged Next source decision stops the export; no fallback to an unrelated live record.");
        return g.finish();
    }
}
