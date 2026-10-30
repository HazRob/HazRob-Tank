"""Meat/fruit must not be reported from an incomplete or stale MQ capture."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[3]
S=(ROOT/"android/app/src/main/java/com/servoc/app/ui/FoodFreshnessTab.java").read_text(encoding="utf-8")
UI=(ROOT/"android/app/src/main/java/com/servoc/app/ui/MeasurementsTab.java").read_text(encoding="utf-8")
class FoodCaptureQuality(unittest.TestCase):
    def test_independent_meat_fruit_ui_and_180s_minimum(self):
        self.assertIn('STUDY_CAPTURE_MS=180000',S)
        self.assertIn('finish.setOnClickListener(v->finishOrCancel())',S)
        self.assertIn('if(SystemClock.elapsedRealtime()-started>=STUDY_CAPTURE_MS)',S)
        self.assertIn('if(frozenElapsed<STUDY_CAPTURE_MS)',S)
        self.assertIn('capturing=false;completed=null;meatCompleted=null;',S)
        self.assertIn('frozenReadings=null;reportId=""',S)
        self.assertIn('export.setEnabled(false)',S)
        self.assertIn('if(snapshots<30||supportedSensors<5)',S)
        self.assertIn('count.getValue()>=10',S)
        self.assertIn('q.optLong("last_ok_age_ms",0)>3000',S)
        self.assertIn('!q.optBoolean("powered",true)',S)
    def test_no_cross_gas_engine(self):
        self.assertIn('FoodFreshnessEngine.analyzeFruit(',S)
        self.assertIn('BeefResearchEngine.analyze(',S)
        self.assertNotIn('TankInstantAnalyzer',S)
        self.assertIn('new Button[5]',UI)
        self.assertIn('for(int i=0;i<names.length;i++)',UI)
if __name__=='__main__':unittest.main()
