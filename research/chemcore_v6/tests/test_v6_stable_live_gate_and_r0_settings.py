"""Regression contract: Stable activation cannot rely on a stale pre-download 'false'.

The operator-facing Settings R0 help must match the exact V6 single-window
180+60 s firmware calibration. General sampling timers are distinct.
"""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[3]
SETTINGS=(ROOT/"android/app/src/main/java/com/servoc/app/ui/SettingsTab.java").read_text(encoding="utf-8")
MANAGER=(ROOT/"android/app/src/main/java/com/servoc/app/chemcore/library/ChemCoreLibraryManager.java").read_text(encoding="utf-8")
class StableLiveGateAndSettings(unittest.TestCase):
    def test_user_r0_says_exact_firmware_protocol_not_old_versions(self):
        self.assertIn("180 ثانية تسخين ثم 60 ثانية قراءة فعلية في نافذة واحدة",SETTINGS)
        self.assertNotIn("300 ثانية إحماء ثم 60 ثانية قياس",SETTINGS)
        self.assertNotIn("الحفظ الحالي في فيرموير V4",SETTINGS)
        self.assertIn("معايرة R0 مستقلة وثابتة: 180s إحماء + 60s قياس",SETTINGS)
        self.assertIn("التحديث اليدوي لا يستعير إثبات معايرة الفيرموير",SETTINGS)

    def test_live_measurement_status_rechecked_after_download(self):
        self.assertIn("java.util.function.BooleanSupplier liveMeasurementActive",MANAGER)
        self.assertIn("return install(checked,appVersion,()->measurementActive)",MANAGER)
        self.assertIn("boolean activeNow=liveMeasurementActive.getAsBoolean()",MANAGER)
        self.assertIn("measurementOrAnalysisActive(null)",MANAGER)
        self.assertLess(MANAGER.index("client.downloadPayload(checked.remote, staging)"),
                        MANAGER.index("boolean activeNow=liveMeasurementActive.getAsBoolean()"))
        self.assertIn("appVersion, activeNow, false",MANAGER)
        self.assertIn("()->ChemCoreLibraryManager.measurementOrAnalysisActive(host.latestDiagnostics())",SETTINGS)
        self.assertNotIn("libraryManager.install(c,ReleaseAudit.VERSION,false)",SETTINGS)

if __name__=="__main__":
    unittest.main()
