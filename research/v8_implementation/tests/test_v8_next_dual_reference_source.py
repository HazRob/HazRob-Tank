#!/usr/bin/env python3
"""P11.2 source-code contracts and pinned 109-formula provenance reconciliation.

Static G1 source check: Android runtime, Gas ppm accuracy and physical G3 are NOT tested.
"""
import csv
import pathlib
import re
import unittest

ROOT=pathlib.Path(__file__).resolve().parents[3]
CORE=ROOT/"android/app/src/main/java/com/hazrob/tank/chemcore/next"
LINK=ROOT/"android/app/src/main/java/com/servoc/app/chemcore/nextintegration"
REGISTER=ROOT/"research/chemcore_next/source_curves/chemcore_next_p03_2_unified_109_formula_register.csv"

class DualReferenceSource(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.catalog=(CORE/"MQSensorsLibExampleCatalog.java").read_text()
        cls.reference=(CORE/"R0ReferenceMath.java").read_text()
        cls.bridge=(LINK/"NextResearchSourceBindings.java").read_text()
        cls.diagnostics=(LINK/"NextScientificDiagnostics.java").read_text()
        cls.adapter=(LINK/"NextAndroidAdapter.java").read_text()
        cls.mapper=(CORE/"NextTelemetryMapper.java").read_text()
        cls.field=(LINK/"NextFieldArchiveBridge.java").read_text()
        with REGISTER.open(newline="") as fp:
            cls.original=[x for x in csv.DictReader(fp)
                          if x["source_curve_uid"].startswith("MQSL-EX-")]
        cls.example_rows=re.findall(
            r'mk\((\d+),"([^"]+)","([^"]+)",([\deE.+-]+),([\deE.+-]+),'
            r'([\deE.+-]+),(true|false),(\d+),(true|false)\)',cls.catalog)
    def test_all_14_original_example_coefficients_and_source_lines_match_register(self):
        self.assertEqual(len(self.original),14)
        self.assertEqual(len(self.example_rows),14)
        indexed={x["source_curve_uid"]:x for x in self.original}
        for num,model,label,a,b,k,installed,line,is135 in self.example_rows:
            uid=f"MQSL-EX-{int(num):03d}"
            src=indexed[uid]
            self.assertEqual(model,src["sensor_model"])
            self.assertEqual(label,src["source_gas_or_class_label"])
            self.assertAlmostEqual(float(a),float(src["coefficient_a"]))
            self.assertAlmostEqual(float(b),float(src["coefficient_b"]))
            self.assertEqual(int(line),int(src["source_line"]))
            self.assertEqual("YES" if installed=="true" else "NO",src["physical_sensor_present_on_tank"])
            self.assertAlmostEqual(float(k),{"MQ2":9.83,"MQ3":60,"MQ4":4.4,
                "MQ5":6.5,"MQ6":10,"MQ7":27.5,"MQ8":70,"MQ9":9.6,
                "MQ135":3.6}[model])
            self.assertEqual(is135=="true",src["source_file"].startswith("examples/MQ-135-ALL/"))
    def test_included_only_13_models_and_source_evidence_not_independent(self):
        installed=[x for x in self.example_rows if x[6]=="true"]
        self.assertEqual(len(installed),13)
        self.assertEqual({x[1] for x in installed},
            {"MQ2","MQ3","MQ4","MQ5","MQ7","MQ8","MQ9","MQ135"})
        self.assertEqual([x[1] for x in self.example_rows if x[6]=="false"],["MQ6"])
        for x in ("public static List<Model> installedModels()", "ROOT_MQ135", "ROOT_BOARD",
                  "MQ-Board", "CO2 + 400", "MQ2.calibrate()"):
            self.assertIn(x,self.catalog)
    def test_air_storage_is_never_overwritten_and_qualifications_are_separate(self):
        for token in ("public static Comparison researchTransform(", "ReferenceKind.DEVICE_CLEAN_AIR",
                      "sourceR0=physical.rAirKohm/source.cleanAirRatioK",
                      "qSource=qAir*source.cleanAirRatioK","false,root,",
                      "SOURCE_MODEL_MISMATCH"):
            self.assertIn(token,self.reference)
        self.assertIn("SOURCE_HEATER_MISMATCH",self.reference)
        self.assertIn("MQ_FACTOR_SCALED_R0_NOT_AIR_RS",self.reference)
        self.assertIn("DEVICE_CLEAN_AIR_RS_MEAN",self.mapper)
        self.assertNotIn('sourceR0=physical.rAirKohm',self.mapper)
        self.assertIn("ManualR0Calibration.get(hw.key)",self.adapter)
        self.assertIn("ManualR0Calibration.getDefinition(hw.key)",self.adapter)
        self.assertNotIn('finite(x,"r0")',self.adapter)
    def test_next_bindings_keep_distinct_equation_and_abstention(self):
        for marker in ("SourceCatalog.allCurves()",
                       "MQSensorsLibExampleCatalog.installedModels()",
                       "PPM_EQUALS_A_TIMES_RATIO_POWER_B",
                       "RATIO_EQUALS_A_TIMES_PPM_OVER_SCALE_POWER_B",
                       "SourceCurveExpert.Qualification.unverified()",
                       "actualDeviceId+\":\"+example.model"):
            self.assertIn(marker,self.bridge)
        self.assertIn("NextResearchSourceBindings.abstentionOnlyCurves(",self.field)
    def test_per_source_diagnostics_use_frozen_snapshot_and_no_raw_firmware_r0(self):
        for marker in ("SCIENTIFIC_DEVICE_ID_SNAPSHOT_MISMATCH",
                       "frozen.mqRelayOn", "SampleQuality.VALID",
                       "CalibrationReference.AIR_RS_MEAN",
                       "reading.rAirKohm",
                       "R0ReferenceMath.researchTransform(device,rs,k)",
                       "MQSensorsLibExampleCatalog.installedModels()",
                       "source_r0_research_kohm",
                       "observed_rs_over_air","source_example_clean_air_K",
                       "source_heater_phase_verified",
                       "source_r0_equivalence_verified",
                       "source_reference_transform_research_only",
                       "source_example_lab_reference_validated_on_tank",
                       "RESEARCH_ONLY_SOURCE_DOMAIN_AND_HEATER_UNVERIFIED",
                       "physical_g3_validated","safety_alarm_authorized",
                       '"chemical_uid",JSONObject.NULL',
                       "source_co2_example_prints_400ppm_offset_not_applied"):
            self.assertIn(marker,self.diagnostics)
        self.assertNotIn('reading.optDouble("r0"',self.diagnostics)
        self.assertNotIn('original.optJSONObject("mq").optDouble("r0"',self.diagnostics)
    def test_field_session_archive_preserves_dual_reference_replay(self):
        for marker in (
            'NextScientificDiagnostics.compute(',
            '"v8_field_next_dual_reference_"+measurementId',
            '"source_archive_sha256",source.sourceSha256',
            '"next_decision_uid",next.receipt.decisionUid',
            '"scientific_source_diagnostics",dualReference',
            '"research_only_no_gas_identity_or_safety_alarm",true',
        ):
            self.assertIn(marker,self.field)
        self.assertIn("FIELD_MEASUREMENT_DURATION_BELOW_60S",self.field)
        self.assertIn("NextResearchSourceBindings.abstentionOnlyCurves(",self.field)

    def test_research_outputs_are_not_promoted_to_alarm_or_identity(self):
        self.assertIn("SourceCurveExpert.Qualification.unverified()",self.bridge)
        self.assertIn("source_example_lab_reference_validated_on_tank",self.diagnostics)
        self.assertIn("source_independent_gas_identity_evidence",self.diagnostics)
        self.assertIn("MQSL_MQ_BOARD_8_MODEL_EXAMPLE",self.catalog)
        self.assertIn("MQSL_MQ135_EXAMPLE_SAME_AS_ADS1115_EXAMPLE",self.catalog)
        self.assertNotIn("ManualR0Calibration.save",self.diagnostics)
        self.assertNotIn("relaySetMask",self.diagnostics)

if __name__=="__main__":
    unittest.main()
