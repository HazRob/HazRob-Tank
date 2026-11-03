#!/usr/bin/env python3
"""Static V8 P11.5 Android source contracts. NOT a physical Tank G3 test."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[3]
APP=ROOT/"android/app/src/main/java/com/servoc/app"
UI=(APP/"ui/MeasurementsTab.java").read_text()
DIAG=(APP/"chemcore/nextintegration/NextScientificDiagnostics.java").read_text()
VIEW=(APP/"chemcore/nextintegration/NextResearchOperatorText.java").read_text()
CAL=(APP/"calibration/ManualR0Calibration.java").read_text()
BIND=(APP/"chemcore/nextintegration/NextResearchSourceBindings.java").read_text()

class TestP115Operator(unittest.TestCase):
    def test_ui_bound_to_actual_next_frozen_source(self):
        for x in ('NextResearchOperatorText.summary(quantitative)',
                  'NextResearchOperatorText.curveLine(m)',
                  'p.optJSONObject("scientific_source_diagnostics")',
                  'dualReferenceCard.setTextIsSelectable(true)',
                  'dualReferenceCard.setVisibility(View.VISIBLE)',
                  'dualReferenceCard.setVisibility(View.GONE)'):
            self.assertIn(x,UI)
        self.assertEqual(UI.count("NextResearchOperatorText.summary("),1)
    def test_research_cannot_be_styled_as_safety_alarm(self):
        for x in ('مقارنة فرضيات المصدر بالحدود المرجعية (ليست إنذارًا)',
                  'screeningWarnings.setBackgroundColor(CARD)',
                  'screeningWarnings.setTextColor(TEXT)',
                  'لا يوجد تأكيد لوجود الغاز',
                  'لا قرار سلامة من هذه المقارنات'):
            self.assertIn(x,UI)
        self.assertIn('SourceCurveExpert.Qualification.unverified()',BIND)
        self.assertNotIn('row.put("safety_alarm_authorized",true)',DIAG)
    def test_adc_raw_and_voltage_are_not_confused(self):
        for x in ('num(q,"adc",3)+" (الوحدة غير مثبتة)"',
                  'q.optDouble("adc_voltage_v",Double.NaN)',
                  'q.optDouble("voltage_v",Double.NaN)',
                  'ManualR0Calibration.getDefinition(hw.key)',
                  'sourceChannelText(q)'):
            self.assertIn(x,UI)
        self.assertNotIn('num(q,"adc",3)+" V',UI)
        self.assertNotIn('num(q,"ao",3)+" V',UI)
        self.assertIn('LEGACY_MQ_FACTOR_SCALED_R0_NOT_AIR_RS',CAL)
    def test_exact_failure_reasons_and_per_sensor_provenance(self):
        for code in ("MISSING_FROZEN_SNAPSHOT","MQ_RELAY_OFF_IN_FROZEN_SNAPSHOT",
                     "MISSING_INSTALLED_MQ_SAMPLE","SENSOR_QUALITY_",
                     "WRONG_R0_DEFINITION","MISSING_AIR_RS_MEAN",
                     "MISSING_POSITIVE_RS","INVALID_SOURCE_EQUATION",
                     "INVALID_SOURCE_REFERENCE_TRANSFORM","NONFINITE_SOURCE_RESULT"):
            self.assertIn(code,DIAG)
            self.assertIn(code,VIEW)
        self.assertIn('missingSourceReason(frozen,reading)',DIAG)
        self.assertIn('missingSourceReason(frozen,observed)',DIAG)
    def test_display_is_render_only(self):
        for x in ('كل ppm ناتج بحثي مشروط',
                  'R0 المصدر = R_air ÷ K',
                  'K وطور السخان غير متحققين',
                  'R_air للجهاز غير مثبت التطابق مع R0 البحثي'):
            self.assertIn(x,VIEW)
        for bad in ("Math.pow","ManualR0Calibration.save","setAlarm"):
            self.assertNotIn(bad,VIEW)
    def test_all_models_shown(self):
        self.assertIn("for(int i=0;i<models.length();i++)",VIEW)
        self.assertIn("for(int ix=0;ix<models.length();ix++)",UI)
        self.assertIn('row.optString("source_curve_id","—")',VIEW)

if __name__=="__main__":
    unittest.main()
