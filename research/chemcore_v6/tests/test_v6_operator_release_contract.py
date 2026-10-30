"""Tank V6 operator preflight: prevent V5 gas-alarm/identity regression.

These tests are source-contract checks, not physical clean-air or gas calibration.
Android compilation and actual emulator tests remain separate CI stages.
"""
from pathlib import Path
import re
import unittest

ROOT=Path(__file__).resolve().parents[3]
ANDROID=ROOT/"android/app/src/main/java/com/servoc/app"
FW=ROOT/"firmware/ESP32-Tank"

class V6OperatorReleaseContract(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.alarm=(ANDROID/"measurement/GasAlertCoordinator.java").read_text()
        cls.main=(ANDROID/"MainActivity.java").read_text()
        cls.cal=(ANDROID/"ui/DeveloperR0Tab.java").read_text()
        cls.r0=(ANDROID/"calibration/FirmwareR0Contract.java").read_text()
        cls.firmware=(FW/"Hardware.cpp").read_text()
        cls.fwapi=(FW/"ESP32-Tank.ino").read_text()
        cls.gateway=(ANDROID/"auth/TankFirmwareCompatibility.java").read_text()
        cls.release=(ANDROID/"ReleaseAudit.java").read_text()
        cls.control=(ANDROID/"ui/ControlTab.java").read_text()
        cls.measurements=(ANDROID/"ui/MeasurementsTab.java").read_text()

    def test_user_calibration_exact_180_plus_60(self):
        self.assertIn("calibrationStart(180,60)",self.cal)
        self.assertIn("180 ثانية تسخين و60 ثانية قياس",self.cal)
        self.assertIn("warmSeconds==180&&sampleSeconds==60",self.firmware)
        self.assertIn("calV6SingleWindow",self.firmware)
        self.assertIn("R0_MAGIC_V6_QUICK",self.firmware)
        self.assertIn('r0_v6q_a',self.firmware)
        self.assertIn("V6_180S_WARM_60S_SINGLE_RS_AIR",self.firmware)
        self.assertIn('V6_QUICK_SCHEMA="tank-r0-fw-v6/2"',self.r0)
        self.assertIn('window_count',self.r0)

    def test_android_connection_has_no_build_or_api_version_lock(self):
        self.assertNotIn("diag.optInt(\"api\",-1)!=SUPPORTED_API",self.gateway)
        self.assertNotIn("!shown.equals(reported)",self.gateway)
        self.assertIn("looksLikeTank",self.gateway)
        self.assertIn("DRIVE_DRIVER",self.gateway)
        self.assertIn("CONTROL_COMMAND_TIMEOUT_MS=500", (FW/"Config.h").read_text())

    def test_gas_findings_are_not_produced_by_unqualified_legacy_alarm(self):
        self.assertIn("return NONE;",self.alarm)
        self.assertNotIn("chemCoreLive.refineIdentity",self.alarm)
        self.assertNotIn("chemCoreLive.updateBroad",self.alarm)
        self.assertIn('diagnostic.put("reported_identity","UNKNOWN")',self.alarm)
        self.assertIn('diagnostic.put("reportable_ppm",false)',self.alarm)
        self.assertIn('diagnostic.put("safety_grade_gas_alarm",false)',self.alarm)
        self.assertIn('Math.max(level,hazard?GasAlertCoordinator.WARNING',self.main)
        self.assertIn('if(!isOffline())',self.main)

    def test_operator_ui_does_not_call_unverified_gas_normal_or_danger(self):
        self.assertNotIn("غازات ● طبيعي",self.control)
        self.assertNotIn("⛔ استجابة غاز قوية",self.control)
        self.assertIn("إشارات MQ",self.control)
        self.assertIn("غير معتمد",self.control)
        self.assertIn("استجابة إشارات MQ (بحثية فقط)",self.measurements)
        self.assertIn("boolean gasNormal=false;",self.measurements)
        self.assertIn("بلا تغير ملحوظ (غير معتمد)",self.measurements)

    def test_v6_android_independence_and_database(self):
        self.assertIn('VERSION="6.0"',self.release)
        self.assertIn("DATABASE_SCHEMA=14",self.release)
        self.assertIn('single clean-air Rs capture',self.release)

if __name__=="__main__":
    unittest.main()
