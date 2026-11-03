"""V8 food acquisition clock: static invariants, not physical 180s acceptance.
Android12 runtime remains separate. Prevents losing the session clock on tab switch.
"""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[2]
UI=(ROOT/"android/app/src/main/java/com/servoc/app/ui/FoodFreshnessTab.java").read_text()
class FoodTabCaptureClock(unittest.TestCase):
    def test_capture_clock_independent_of_selected_measurement_tab(self):
        for s in ("private final Handler captureHandler=new Handler(Looper.getMainLooper())",
                  "private final Runnable captureClock=new Runnable()",
                  "if(capturing)captureHandler.postDelayed(this,1000)",
                  "captureHandler.postDelayed(captureClock,1000)",
                  "captureHandler.removeCallbacks(captureClock)",
                  "SystemClock.elapsedRealtime()-started",
                  "if(elapsed>=STUDY_CAPTURE_MS)complete()"):
            self.assertIn(s,UI)
    def test_same_firmware_cycle_not_doubled_by_two_refresh_callers(self):
        for s in ("lastCycle.get(sensor)", "if(last!=null&&cycle==last)continue;",
                  "lastCycle.put(sensor,cycle)",
                  "MIN_ACCEPT_INTERVAL_MS=950",
                  "if(snapshots<30||modelChannels<6)"):
            self.assertIn(s,UI)
    def test_food_does_not_invoke_gas_engine(self):
        self.assertNotIn("ChemCoreNext.analyze",UI)
        self.assertNotIn("NextEngineRouter.analyze",UI)
if __name__=="__main__":unittest.main()
