#!/usr/bin/env python3
"""G1 source contract for P11.6: clean-air R0 meaning and source PDF reasons.

This is a structural guard; physical Tank G3 and device UI interaction remain untested.
"""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[3]
APP=ROOT/"android/app/src/main/java/com/servoc/app"
UI=(APP/"ui/DeveloperR0Tab.java").read_text()
FW=(APP/"calibration/FirmwareR0Contract.java").read_text()
STORE=(APP/"calibration/ManualR0Calibration.java").read_text()
PDF=(APP/"report/TankInstantV7PdfWriter.java").read_text()
FIELD=(APP/"report/V8NextFieldResearchAppendixPdf.java").read_text()
DIAG=(APP/"chemcore/nextintegration/NextScientificDiagnostics.java").read_text()

class TestP116CalibrationPdf(unittest.TestCase):
    def test_manual_auto_legacy_display_distinction(self):
        for token in (
            "R_air تشغيلي ميداني",
            "180 ثانية تسخين و60 ثانية",
            "R0 المشتقة لكل منحنى MQSensorsLib = R_air/K",
            '"DEVICE_CLEAN_AIR_RS_MEAN".equals(definition)',
            '"LEGACY_MQ_FACTOR_SCALED_R0_NOT_AIR_RS".equals(definition)',
            "مرجع يدوي/غير مؤهل",
            "R0 المصدرية = R_air/K_source",
            "ManualR0Calibration.getDefinition(k)"
        ):self.assertIn(token,UI)
        self.assertIn('getDefinition(String key)',STORE)
        self.assertIn('V6_QUICK_SCHEMA',FW)
        self.assertIn('p.r0Definition',UI)
        self.assertIn('p.protocol',UI)
    def test_no_source_r0_writeback_and_no_changed_calibration_duration(self):
        for bad in ("saveSourceR0(", "sourceR0Kohm", "saveDerivedR0("):
            self.assertNotIn(bad,UI)
        self.assertIn("p.warmupMs",UI)
        self.assertIn("p.durationMs",UI)
        self.assertIn("ManualR0Calibration.saveFirmware(",UI)
        self.assertIn("NVS R0 readback mismatch",UI)
    def test_source_refusal_uses_frozen_diagnostic_in_same_pdf_row(self):
        self.assertIn("NextResearchOperatorText.cause(reason)",PDF)
        self.assertIn('o.optString("failure_reason_code","")',PDF)
        self.assertIn('سبب عدم الحساب [',PDF)
        self.assertIn('rowY+24,w-mm(4),7.1f',PDF)
        self.assertIn("float rowY=y+32+i*37",PDF)
        self.assertIn("int curvePages=(nc+14)/15",PDF)
        self.assertIn("for(int offset=0;offset<nc;offset+=15)",PDF)
        self.assertIn("new PdfDocument.PageInfo.Builder(W,H,pageNo)",PDF)
        self.assertIn("source_r0_research_kohm",PDF)
        self.assertIn("source_example_clean_air_K",PDF)
        self.assertIn("source_only_estimate_not_a_verified_concentration",DIAG)
        self.assertNotIn("NextScientificDiagnostics.compute(",PDF)
        self.assertNotIn("ManualR0Calibration.save",PDF)
    def test_field_appendix_discloses_invalid_baseline_without_new_page(self):
        for key in ("R_air absent ","MQ quality not VALID ","Relay ",
                    "Source R0/K not G3-qualified","q.rAirKohm==null",
                    "q.quality!=CoreContracts.SampleQuality.VALID"):
            self.assertIn(key,FIELD)
        self.assertIn('42,354,8,MUTED,false',FIELD)
        self.assertIn('42,374,9,INK,true',FIELD)
        self.assertIn('static void draw(Canvas c,NextDecisionStore.Receipt receipt',FIELD)
        self.assertNotIn("startPage(",FIELD)
        self.assertNotIn("NextScientificDiagnostics.compute(",FIELD)

if __name__=="__main__":
    unittest.main()
