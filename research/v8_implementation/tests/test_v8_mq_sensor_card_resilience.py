"""V8 F01 MQ sensor-card regression checks (source guard, not device qualification)."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[3]
CARD = ROOT / "android/app/src/main/java/com/servoc/app/ui/MeasurementsTab.java"


class MqSensorCardResilienceTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.source = CARD.read_text(encoding="utf-8")
        cls.sensor = cls.source.split("private final class SensorCard {", 1)[1].split(
            "private final class AnalysisPane {", 1
        )[0]

    def test_data_readable_on_light_card(self):
        self.assertIn("root=UiKit.card(c)", self.sensor)
        self.assertIn("data.setTextColor(0xFF37474F)", self.sensor)
        self.assertNotIn("data.setTextColor(0xFFCFD8DC)", self.sensor)

    def test_missing_baseline_not_described_as_ready_or_embedded(self):
        self.assertIn('baseline?" • جاهز":" • خط الأساس غير متحقق"', self.sensor)
        self.assertNotIn('" • baseline مدمج"', self.sensor)

    def test_fallback_baseline_cannot_become_a_colored_gas_alarm(self):
        self.assertIn("&&!warn.usedFallbackBaseline", self.sensor)
        self.assertIn("else if(!baseline){head.setBackgroundColor(0xFF546E7A)", self.sensor)
        self.assertIn("لا إنذار غاز", self.sensor)
        self.assertLess(
            self.sensor.index("else if(!baseline)"),
            self.sensor.index("else if(warn.level==InstantMqWarningEngine.Level.HIGH)")
        )
        self.assertIn("String delta=baseline&&Double.isFinite(warn.delta)?", self.sensor)

    def test_unavailable_diagnostic_does_not_dereference_null_health(self):
        self.assertIn("SensorHealthEngine.Snapshot health=host.sensorHealth(hw.key)", self.sensor)
        self.assertRegex(
            self.sensor,
            re.compile(r'health\s*==\s*null\s*\?\s*"تشخيص الحساس غير متاح"\s*:\s*'
                       r'SensorHealthEngine\.stateAr\(health\.state\)')
        )
        self.assertNotIn('+" • "+SensorHealthEngine.stateAr(health.state)', self.sensor)


if __name__ == "__main__":
    unittest.main()
