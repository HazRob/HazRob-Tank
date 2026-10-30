"""Pre-release source contract for V6 R0 interrupted NVS approval.

This catches an Android UI regression where a lost /r0 accept HTTP response
causes a second accept (or phone-side persistence of the wrong device/protocol).
It is not a physical ESP32 or Android UI automation test.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[3]
UI = (ROOT/"android/app/src/main/java/com/servoc/app/ui/DeveloperR0Tab.java").read_text(encoding="utf-8")
CONTRACT = (ROOT/"android/app/src/main/java/com/servoc/app/calibration/FirmwareR0Contract.java").read_text(encoding="utf-8")


class V6R0InterruptedApprovalContract(unittest.TestCase):
    def test_reconcile_from_esp32_before_accepting_a_pending_candidate(self):
        begin = UI.index("private void acceptFirmwareResult()")
        end = UI.index("private void retryAutomatic()", begin)
        s = UI[begin:end]
        self.assertIn("JSONObject observed=host.robotApi().calibrationStatus();", s)
        self.assertIn("FirmwareR0Contract.progress(observed)", s)
        self.assertIn('if("ready".equals(observedProgress.state)&&observedProgress.committed)', s)
        self.assertIn("committed=observed;", s)
        self.assertIn('else if("pending".equals(observedProgress.state)&&observedProgress.pending)', s)
        self.assertIn("committed=host.robotApi().calibrationAccept();", s)
        self.assertGreater(s.index("calibrationAccept();"), s.index("calibrationStatus();"))
        self.assertIn('throw new IllegalStateException("ESP32 calibration is neither staged nor verified committed")', s)

    def test_fail_closed_on_device_protocol_value_or_nvs_inconsistency(self):
        begin=UI.index("private void acceptFirmwareResult()")
        end=UI.index("private void retryAutomatic()", begin)
        s=UI[begin:end]
        self.assertIn("if(!expected.equals(observed.optString(\"device_id\",\"\").trim()))", s)
        self.assertIn("FirmwareR0Contract.committedValues(committed)", s)
        self.assertIn("if(!expected.equals(deviceId))", s)
        self.assertIn("proposal.windowCount!=actualProgress.windowCount", s)
        self.assertIn("!proposal.r0Definition.equals(actualProgress.r0Definition)", s)
        self.assertIn('throw new IllegalStateException("NVS R0 readback mismatch: "+sensor)', s)
        self.assertGreater(s.index("ManualR0Calibration.saveFirmware("), s.index("FirmwareR0Contract.committedValues(committed)"))


    def test_exact_quick_protocol_is_checked_from_firmware_state_not_label_only(self):
        self.assertIn('response.optInt("warmup_ms",-1)!=180000', CONTRACT)
        self.assertIn('"warmup".equals(state)&&stageDuration!=180000', CONTRACT)
        self.assertIn('&&stageDuration!=60000', CONTRACT)
        self.assertIn("wrongQuickDuration", CONTRACT)
        self.assertIn("V6_QUICK_SCHEMA", CONTRACT)

if __name__=="__main__":
    unittest.main()
