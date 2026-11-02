#!/usr/bin/env python3
"""P07.3 + P08.2 V8 Next instantaneous entrypoint source contract.
Static CI guard only; never labeled as Android runtime or G3 gas validation.
"""
import pathlib
import re
import unittest

ROOT=pathlib.Path(__file__).resolve().parents[3]
A=ROOT/"android/app/src/main/java/com/servoc/app/chemcore/TankInstantAnalyzer.java"
B=ROOT/"android/app/src/main/java/com/servoc/app/chemcore/nextintegration/NextEngineRouter.java"
C=ROOT/"android/app/src/main/java/com/servoc/app/ui/MeasurementsTab.java"
D=ROOT/"android/app/src/main/java/com/servoc/app/ui/SettingsTab.java"
E=ROOT/"android/app/src/main/java/com/servoc/app/chemcore/nextintegration/NextDecisionEnvelope.java"
class SourceRouting(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.a=A.read_text(encoding="utf-8")
        cls.b=B.read_text(encoding="utf-8")
        cls.c=C.read_text(encoding="utf-8")
        cls.d=D.read_text(encoding="utf-8")
        cls.e=E.read_text(encoding="utf-8")
        assert "public static Result analyzeV6Comparison(" in cls.a
        cls.next_method=cls.a.split("public static Result analyze(android.content.Context androidContext",1)[1].split("public static Result analyzeV6Comparison",1)[0]
    def test_v8_live_invokes_next_core_and_stores_once(self):
        self.assertIn("NextEngineRouter.analyzeAndPersist(",self.next_method)
        self.assertIn("NextDecisionEnvelope",self.next_method)
        self.assertIn(".fromReceipt(next.receipt)",self.next_method)
        self.assertIn("candidate_order_source","%s"%self.next_method)
        self.assertIn("PERSISTED_NEXT_DECISION_ONLY",self.next_method)
        self.assertIn("legacy_inference_fallback_used\",false",self.next_method)
    def test_no_synthetic_legacy_144_or_source_curve_candidate_promotion_in_active_path(self):
        for banned in ("byLegacyId(","<144","source_curve_hypotheses\");","TankV6TelemetryAdapter.analyzeSnapshot","ChemCoreTankV6.Result analyzed"):
            if banned=='source_curve_hypotheses\");':continue
            self.assertNotIn(banned,self.next_method)
        self.assertIn('out.put("source_curve_hypotheses",new JSONArray())',self.next_method)
        self.assertIn("List.of(),java.util.List.of(),java.util.List.of(),12)",self.next_method)
    def test_v6_comparison_explicit_not_inactive_fallback(self):
        v6=self.a.split("public static Result analyzeV6Comparison(",1)[1]
        self.assertIn("TankV6TelemetryAdapter.analyzeSnapshot",v6)
        self.assertIn("byLegacyId",v6)
        self.assertNotIn("TankV6TelemetryAdapter",self.next_method)
        self.assertIn("NEXT_NOT_AUTHORIZED_OR_COMPARISON_MODE_NO_SILENT_V6",
                      (ROOT/"android/app/src/main/java/com/hazrob/tank/chemcore/next/NextEngineRoutingPolicy.java").read_text())
    def test_source_sha_matches_stable_payload_bytes_not_preference(self):
        for required in ("verifiedInstalledStableSha","FileInputStream","MessageDigest.getInstance(\"SHA-256\")",
                         "STABLE_PAYLOAD_CONTENT_HASH_MISMATCH","ChemCoreNext.analyze"):
            self.assertIn(required,self.b)
        self.assertIn("flag",self.b.lower())
    def test_next_UI_research_gate_and_settings_do_not_claim_ppm(self):
        self.assertIn("hazrob.tank.v8.next.decision.presentation/1",self.c)
        self.assertIn("NextEngineRouter.FLAG",self.d)
        self.assertIn("لا بديل تلقائي",self.d)
        for s in ('"ppm_reportable",false','"physical_g3_validated",false',
                  '"occupational_gas_alarm_authorized",false'):
            self.assertIn(s,self.e)
    def test_old_candidate_math_not_upgraded_to_gas_posterior(self):
        self.assertIn("candidate_count",self.e)
        self.assertIn('candidate.put("chemical_uid",c.canonicalUid)',self.e)
        self.assertNotIn('"posterior"',self.e)
        self.assertNotIn('"screening_fit_ppm"',self.e)
if __name__=="__main__":
    unittest.main(verbosity=2)
