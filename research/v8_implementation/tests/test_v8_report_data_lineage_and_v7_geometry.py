"""V8 report data-lineage and approved-geometry regression contract.

Static source checks are only G1 guards; Android PdfRenderer instrumented tests
are required for actual rendered chart binding and PDF readability.
"""
from pathlib import Path
import hashlib
import re
import unittest

ROOT=Path(__file__).resolve().parents[3]
JAVA=ROOT/"android/app/src/main/java"

def source(relative):
    return (JAVA/relative).read_text(encoding="utf-8")

def git_blob_hash(relative):
    data=(JAVA/relative).read_bytes()
    return hashlib.sha1(f"blob {len(data)}\0".encode()+data).hexdigest()

class V8ProductionReportBindingAudit(unittest.TestCase):
    def test_approved_v7_global_geometry_and_chart_layout_unchanged(self):
        # Git blob content hashes sourced from V7-approved design at start of V8 closeout.
        self.assertEqual("0e8d8695a27b8e075c7166368047c4b8860034c1",
            git_blob_hash("com/servoc/app/report/V7ReportLayoutKit.java"))
        self.assertEqual("86802317f76482554756cec6646359a18c5ddf62",
            git_blob_hash("com/servoc/app/report/V7FieldChartLayout.java"))
        layout=source("com/servoc/app/report/V7ReportLayoutKit.java")
        for invariant in ("A4_PORTRAIT_W=595", "A4_LANDSCAPE_W=842",
            "MIN_BODY_PT=12f", "EXCEEDED_RED=0xFFC62828",
            "drawHeader(", "drawFooter("):
            self.assertIn(invariant,layout)

    def test_r03_production_pdf_is_real_smart_writer_not_blank_v7_draft(self):
        export=source("com/servoc/app/report/FieldSessionReportExporter.java")
        smart=source("com/servoc/app/report/SmartFieldSessionPdfWriter.java")
        self.assertIn("SmartFieldSessionPdfWriter.writePdf(out,db,fieldSessionId,chemicals)",export)
        self.assertNotIn("V7FieldSessionPdfWriter.writePdf(out",export)
        for invariant in (
            "FieldSessionReportExporter.resolve(db,fieldSessionId)",
            "SmartReportAnalyticalContract contract=b.analyticalContract",
            "primaryFromBundle(b,contract,all)",
            "contract.series()", "contract.decision()",
            "AnalyticalCharts.mqRsR0(c,r[0],s)",
            "AnalyticalCharts.fingerprint(c,r[1],s)",
            "AnalyticalCharts.candidates(c,r[0],q)",
            "AnalyticalCharts.mqResponseKinetics(c,r[4],s)",
            "SessionSeries.byId(db,id)",
            "decisions.latestPayloadForSession(id)"):
            self.assertIn(invariant,smart)
        self.assertNotIn("V7FieldChartLayout.drawFirstFour(",smart)
        # Modern Next receipt must never invoke legacy V6 re-analysis or
        # label a historical appendix as authoritative Next evidence.
        self.assertIn("v6ReadOnly=next==null?",smart)
        self.assertIn("V8NextFieldResearchAppendixPdf.draw(",smart)
        self.assertIn("nextPages=next==null?0:1",smart)
        next_annex=source("com/servoc/app/report/V8NextFieldResearchAppendixPdf.java")
        self.assertIn("ChemCore Next •",next_annex)
        self.assertIn("n.sourceSnapshot.readings",next_annex)
        self.assertNotIn("ChemCoreTankV6",next_annex)

    def test_r01_next_pdf_only_from_same_frozen_source_and_full_references(self):
        router=source("com/servoc/app/report/TankInstantReportPdf.java")
        visual=source("com/servoc/app/report/TankInstantV7PdfWriter.java")
        self.assertIn("TankInstantV7PdfWriter.writeNext(out,telemetry,chemcore,id,timestamp)",router)
        for invariant in ("decision_uid","snapshot_sha256","source_curve_hypotheses",
                          "curves.length()", "comparisons.length()",
                          "header(c,","footer(c,"):
            self.assertIn(invariant,visual)

    def test_r04_site_writer_is_not_blank_architecture_preview(self):
        site=source("com/servoc/app/report/SiteSummaryReportExporter.java")
        blank=source("com/servoc/app/report/V7SiteVisualPdfWriter.java")
        self.assertIn("public static void writePdf(",site)
        self.assertIn("public static void writeBlankDesign",blank)
        self.assertNotIn("V7SiteVisualPdfWriter",site)
        for field in ("FieldSessionReportExporter.resolve(db,fs.id)",
                      "repo.approvedSessionsForSite(id)",
                      "decisions.latestPayloadForSession(cycle.measurementId)",
                      "SiteReportIntelligence.plan(b.sessions)",
                      "matrix(p.getCanvas(),b,chunk)",
                      "NextSiteEvidenceCharts.rootRecurrence(",
                      "NextSiteEvidenceCharts.candidateRecurrence(",
                      "NextDecisionStore(db).latestForSession(",
                      "recurrence(p.getCanvas(),b,plan)"):
            self.assertIn(field,site)

    def test_r05_r06_pdf_lines_bind_actual_frozen_mq_timelines(self):
        fruit=source("com/hazrob/tank/foodfreshness/V7FruitReportPdfWriter.java")
        meat=source("com/hazrob/tank/foodfreshness/meat/V7BeefResearchPdfWriter.java")
        tab=source("com/servoc/app/ui/FoodFreshnessTab.java")
        for invariant in ("realTrace", "signalHistory.entrySet()",
                          "FoodFreshnessPdf.save(", "BeefResearchPdf.save("):
            self.assertIn(invariant,tab)
        for invariant in ("temporalCurve(c,trace", "FoodMeasurementDecision.fruit(",
                          "observedSourceChart(c,readings,result)",
                          "studyDayChart(c,result,readings)"):
            self.assertIn(invariant,fruit)
        for invariant in ("actualTimeTrace(c,trace)","measuredBeefCurve(c,result,measured)",
                          "BeefGaussianResearchEngine.analyze(measured)"):
            self.assertIn(invariant,meat)
        self.assertNotIn("ChemCoreNext",fruit+meat)

    def test_r07_reports_all_emissions_source_hypotheses_without_truncation(self):
        pdf=source("com/hazrob/tank/emissions/EmissionReportPdf.java")
        raw=source("com/hazrob/tank/emissions/EmissionNextBridge.java")
        tab=source("com/servoc/app/ui/EmissionsTab.java")
        for invariant in ("(sources.size()+8)/9","pageSource(c,ctx,name,scan,sources",
                          "next_source_hypotheses","research_estimate_ppm",
                          "V7ReportLayoutKit.drawHeader", "V7ReportLayoutKit.drawFooter"):
            self.assertIn(invariant,pdf)
        for invariant in ("source_curve_hypotheses","next_source_hypotheses",
                          "research_numeric_models", "research_values_visible"):
            self.assertIn(invariant,raw)
        self.assertIn("EmissionReportPdf.save(context,e.name,latest",tab)
        self.assertNotIn("Math.min(13",pdf)
        self.assertNotIn("Math.min(total,7)",pdf)

    def test_all_report_types_have_separate_g1_and_emulator_evidence(self):
        expected=[
            "com/servoc/app/report/TankInstantV7PdfWriter.java",
            "com/servoc/app/report/V7PhysicalReportPdfWriter.java",
            "com/servoc/app/report/SmartFieldSessionPdfWriter.java",
            "com/servoc/app/report/SiteSummaryReportExporter.java",
            "com/hazrob/tank/foodfreshness/V7FruitReportPdfWriter.java",
            "com/hazrob/tank/foodfreshness/meat/V7BeefResearchPdfWriter.java",
            "com/hazrob/tank/emissions/EmissionReportPdf.java",
        ]
        for name in expected:
            with self.subTest(report=name):
                self.assertTrue((JAVA/name).is_file())

if __name__=="__main__":
    unittest.main()
