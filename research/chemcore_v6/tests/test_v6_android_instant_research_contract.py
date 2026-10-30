#!/usr/bin/env python3
"""Structural fail-closed N06 source boundary checks (NOT Android runtime tests).

The actual Android :app:compileDebugJavaWithJavac is a separate CI step.
These tests establish new mode call-site and report branch invariants only.
"""
import pathlib
import re
import unittest

JAVA=pathlib.Path("android/app/src/main/java")
ROOT=JAVA/"com/servoc/app"
ADAPTER=JAVA/"com/hazrob/tank/v6integration/TankV6TelemetryAdapter.java"
INSTANT=ROOT/"chemcore/TankInstantAnalyzer.java"
UI=ROOT/"ui/MeasurementsTab.java"
PDF=ROOT/"report/TankInstantReportPdf.java"
SRC_COORDINATOR=JAVA/"com/hazrob/tank/chemcore/v6/ChemCoreTankV6.java"

class N06AndroidV6GasReportSafety(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.a=ADAPTER.read_text(encoding="utf-8")
        cls.i=INSTANT.read_text(encoding="utf-8")
        cls.u=UI.read_text(encoding="utf-8")
        cls.p=PDF.read_text(encoding="utf-8")
        cls.c=SRC_COORDINATOR.read_text(encoding="utf-8")

    def test_exact_new_coordinator_used(self):
        self.assertIn("ChemCoreTankV6.analyze(new ChemCoreTankV6.Request(",self.a)
        self.assertIn("TankV6TelemetryAdapter.analyzeSnapshot(frozen)",self.i)
        self.assertIn("TankV6TelemetryAdapter.toJson(analyzed)",self.i)
        self.assertIn("TankInstantAnalyzer.analyze(c,host.database(),frozen,now)",self.u)

    def test_no_instant_legacy_fallback(self):
        for banned in ("ChemCoreInferenceEngine", "AnalyticsSnapshot",
                       "ChemCoreRuntimeCoordinator", "TankStableRuntimeCatalog",
                       "TankCandidateHazardBadges", "FoodFreshness"):
            self.assertNotIn("import "+banned,self.i)
            self.assertNotIn(banned+"(",self.i)
        self.assertIn('out.put("legacy_inference_fallback_used",false)',self.i)
        self.assertNotIn("catch(Exception",self.i)

    def test_no_unpowered_or_invalid_channels_promoted(self):
        self.assertIn('!value.optBoolean("ok",value.optBoolean("valid",false))',self.a)
        self.assertIn('value.has("powered")',self.a)
        self.assertIn('!Double.isFinite(rs)||rs<=0',self.a)
        self.assertIn('root.put("original_mq_channel_evidence",rawChannels)',self.a)
        self.assertIn('row.put("r0_as_received",numberOrNull(raw.r0))',self.a)

    def test_missing_r0_kept_without_synthetic_ratio(self):
        self.assertIn('new Engine.Channel(model,model+"-tank-current-snapshot",rs,r0)',self.a)
        self.assertIn('out.put("reported_identity","UNKNOWN")',self.i)
        self.assertIn('out.put("ppm_reportable",false)',self.i)
        self.assertIn('out.put("gas_food_isolated",true)',self.i)
        self.assertIn('root.put("reportable_ppm",false)',self.a)
        self.assertIn('rawResistanceValid()&&positiveFinite(r0)',self.c.replace(
            "chemcore", "chemcore") if "rawResistanceValid()&&positiveFinite(r0)" in self.c else
            (JAVA/"com/hazrob/tank/chemcore/v6/Engine.java").read_text(encoding="utf-8"))

    def test_v6_result_renders_before_legacy_posterior(self):
        v6=self.u.index("if(isResearchOnlyV6(p))")
        old=self.u.index('String chemical=p.optString("chemical"')
        self.assertLess(v6,old)
        self.assertIn("renderResearchOnlyV6(p)",self.u)
        helper=self.u[self.u.index("private void renderResearchOnlyV6("):v6]
        # The user can manually select a research candidate from the unified
        # master catalog for exploratory gradient tracking, but the V6
        # engine must never select a target or start driving automatically.
        self.assertIn("startTracking.setEnabled(selectedCandidateId>=0)",helper)
        self.assertIn('p.optJSONArray("candidates")',helper)
        self.assertIn("selectedCandidateId=(int)uid;",helper)
        self.assertNotIn("setUnifiedTrackingTarget(",helper)
        self.assertNotIn("host.setTrackingTarget(",helper)
        self.assertNotIn("TankPpmEstimate.from",helper)
        self.assertNotIn("screening_fit_ppm",helper)
        self.assertNotIn("TankCandidateHazardBadges",helper)
        self.assertIn("الهوية: UNKNOWN",helper)
        self.assertIn('p.optJSONArray("source_curve_hypotheses")',helper)

    def test_v6_pdf_branch_before_legacy_document(self):
        v6=self.p.index('"hazrob-tank-v6-android-telemetry-bridge/1".equals')
        old=self.p.index("PdfDocument doc=new PdfDocument();",v6)
        self.assertLess(v6,old)
        self.assertIn("writeResearchV6(out,telemetry,chemcore,id,timestamp)",self.p)
        method=self.p[self.p.index("private static void writeResearchV6("):
                      self.p.index("public static void write(OutputStream")]
        self.assertIn("الهوية: UNKNOWN",method)
        self.assertIn("تركيز ppm معتمد: غير متاح",method)
        self.assertNotIn("candidate_score_pct",method)
        self.assertNotIn("screening_fit_ppm",method)
        self.assertNotIn("estimated_ppm",method)
        self.assertIn('v6.optJSONArray("source_curve_hypotheses")',method)
        self.assertIn('v6.optInt("raw_only_mq_channels",0)',method)

    def test_gas_research_does_not_upgrade_physical_g3(self):
        self.assertIn('physicalCalibrationProven=false;reportablePpm=false;foodModelAttached=false',self.c)
        self.assertNotIn("import com.servoc",self.c)
        self.assertNotIn("import android.",self.c)
        self.assertIn("single_snapshot_no_temporal_series",self.i)

if __name__=="__main__":
    unittest.main()
