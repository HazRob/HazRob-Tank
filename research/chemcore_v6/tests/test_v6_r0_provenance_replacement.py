"""V6 R0 calibration provenance regression (source-level, not a device test).

Replacing a firmware-calibrated channel with a manual or historical phone-side
value must not retain the old firmware's definition/window-count metadata.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[3]
SOURCE = (ROOT / "android/app/src/main/java/com/servoc/app/calibration/ManualR0Calibration.java").read_text(encoding="utf-8")


def method(start: str, end: str) -> str:
    assert SOURCE.count(start) == 1, start
    pos = SOURCE.index(start)
    nxt = SOURCE.find(end, pos + len(start))
    assert nxt > pos, end
    return SOURCE[pos:nxt]


class CalibrationProvenanceReplacementTest(unittest.TestCase):
    def test_loading_historical_manual_values_ignores_stale_firmware_attestations(self):
        s = method("public static synchronized void load(", "public static synchronized boolean isLoaded()")
        self.assertIn('String source=p.getString("manual_r0_source_"+k,"manual_device_r0")', s)
        self.assertIn('if("firmware_clean_air_r0".equals(source))', s)
        block = s.split('if("firmware_clean_air_r0".equals(source))', 1)[1]
        self.assertIn('r0Definitions.put(k,definition)', block)
        self.assertIn('r0WindowCounts.put(k,w)', block)
        self.assertIn('sampleCounts.put(k,n)', block)

    def test_manual_single_clears_both_persistent_and_in_memory_protocol_metadata(self):
        s = method("public static synchronized boolean saveForDevice(", "public static synchronized boolean saveAll(")
        self.assertIn('edit.remove("manual_r0_samples_"+key)', s)
        self.assertIn('.remove("manual_r0_definition_"+key)', s)
        self.assertIn('.remove("manual_r0_windows_"+key)', s)
        self.assertIn("if(!edit.commit())return false;", s)
        self.assertGreater(s.index("r0Definitions.remove(key);"), s.index("if(!edit.commit())return false;"))
        self.assertGreater(s.index("r0WindowCounts.remove(key);"), s.index("if(!edit.commit())return false;"))

    def test_manual_batch_is_atomic_and_purges_all_old_firmware_metadata(self):
        s = method("public static synchronized boolean saveAllForDevice(", "public static synchronized boolean saveAutomatic(")
        self.assertIn("updated.size()!=KEYS.length", s)
        self.assertIn('e.remove("manual_r0_definition_"+k)', s)
        self.assertIn('e.remove("manual_r0_windows_"+k)', s)
        self.assertIn("if(!e.commit())return false;", s)
        self.assertGreater(s.index("r0Definitions.remove(k);"), s.index("if(!e.commit())return false;"))
        self.assertGreater(s.index("r0WindowCounts.remove(k);"), s.index("if(!e.commit())return false;"))

    def test_phone_side_automatic_cannot_inherit_firmware_definition(self):
        s = method("public static synchronized boolean saveAutomatic(", "public static synchronized boolean saveFirmware(")
        self.assertIn('e.remove("manual_r0_definition_"+k)', s)
        self.assertIn('e.remove("manual_r0_windows_"+k)', s)
        self.assertIn("if(!e.commit())return false;", s)
        self.assertGreater(s.index("r0Definitions.remove(k);"), s.index("if(!e.commit())return false;"))
        self.assertGreater(s.index("r0WindowCounts.remove(k);"), s.index("if(!e.commit())return false;"))

    def test_firmware_commit_still_retains_attested_definition_and_window(self):
        s = method("public static synchronized boolean saveFirmware(SharedPreferences p,Map<String,Double> proposal,\n                    String deviceId,Map<String,Integer> samples,String definition,int windowCount)", "public static String firmwareDevice(")
        self.assertIn('putString("manual_r0_definition_"+key,definition)', s)
        self.assertIn('putString("manual_r0_windows_"+key,Integer.toString(windowCount))', s)
        self.assertIn("if(!edit.commit())return false;", s)
        self.assertGreater(s.index("r0Definitions.put(key,definition);"), s.index("if(!edit.commit())return false;"))


if __name__ == "__main__":
    unittest.main()
