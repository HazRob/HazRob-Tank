import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[3]
GATE = ROOT / "android/app/src/main/java/com/servoc/app/measurement/InstantCaptureGate.java"
UI = ROOT / "android/app/src/main/java/com/servoc/app/ui/MeasurementsTab.java"

class InstantSourceContract(unittest.TestCase):
    """Static contract only; real Android12 and Tank source timing still required."""
    def test_monotonic_time_and_two_independent_windows(self):
        text = GATE.read_text()
        self.assertIn("WARMUP_MS=60_000L, SAMPLING_MS=60_000L", text)
        self.assertIn("MIN_SOURCE_SPAN_MS=58_000L", text)
        self.assertIn("MIN_DISTINCT_CYCLES=10", text)
        self.assertIn("nowMs>=beginMs+WARMUP_MS+SAMPLING_MS", text)
    def test_repeated_frame_not_new_cycle(self):
        text = GATE.read_text()
        self.assertIn('q.has("scan_cycle")', text)
        self.assertIn("observedCycles.contains(cycle)", text)
        self.assertIn("if(fresh.isEmpty())return", text)
        self.assertIn("relayConfirmed(source)", text)
    def test_ui_never_finishes_without_original_capture(self):
        ui = UI.read_text()
        self.assertIn("captureGate.isReady(mono)", ui)
        self.assertIn("captureGate.verifiedEvidence(mono)", ui)
        self.assertIn('frozen.put("v8_instant_source_capture"', ui)
        self.assertIn("captureGate.observe(at,d)", ui)
        self.assertIn("captureGate.hasFailure()", ui)
        self.assertIn("TankInstantAnalyzer.analyze(c,host.database(),frozen,now)", ui)

if __name__ == "__main__":
    unittest.main()
