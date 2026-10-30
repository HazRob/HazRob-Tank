"""Operator UI contract: all five measurement routes reachable, and CAS aliases source-labeled."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[3]
UI=(ROOT/"android/app/src/main/java/com/servoc/app/ui/MeasurementsTab.java").read_text(encoding="utf8")
REPO=(ROOT/"android/app/src/main/java/com/servoc/app/chemical/ChemicalRepository.java").read_text(encoding="utf8")
FOOD=(ROOT/"android/app/src/main/java/com/servoc/app/ui/FoodFreshnessTab.java").read_text(encoding="utf8")
EMULATOR=(ROOT/"research/chemcore_v6/tests/android/V6ScientificRiskDbFixtureActivity.java").read_text(encoding="utf8")
class OperatorRoutability(unittest.TestCase):
    def test_food_button_is_accessible(self):
        self.assertIn('new Button[5]',UI)
        self.assertIn('String[] names={"🔎 البحث والتتبع","📊 التحليل","🌡 الفيزيائية","◌ الجزيئات","🍎 نضارة الأغذية"}',UI)
        self.assertIn('for(int i=0;i<names.length;i++)',UI)
        self.assertIn('new View[]{searchView,analysisShell,physical.view,particlesView,foodFreshness.view()}',UI)
        self.assertIn('selected=Math.max(0,Math.min(4,index))',UI)
        self.assertNotIn('for(int i=0;i<4;i++){\n            final int index=i;tabs[i]=UiKit.button(c,names[i])',UI)
        self.assertIn('new String[]{"🥩 اللحوم","🍎 الفواكه"}',FOOD)
        self.assertIn('operationMode=index==0?"MEAT":"FRUIT"',FOOD)
    def test_cas_source_visible_not_invented(self):
        self.assertIn('LEFT JOIN chemical_identity_equivalence_v6 e',REPO)
        self.assertIn('OR e.source_cas LIKE ? OR ni.name_en LIKE ?',REPO)
        self.assertIn('src.record_url=e.source_url',REPO)
        self.assertIn('catalogCasLabel(Chemical chemical)',REPO)
        self.assertIn('"CAS مرجعي NIOSH: "+qualified',REPO)
        self.assertIn('repo.catalogCasLabel(chemical)',UI)
        self.assertIn('pickerRepo.catalogCasLabel(m)',UI)
        self.assertIn('search("64-17-5",100)',EMULATOR)
        self.assertIn('search("Ethyl alcohol",100)',EMULATOR)
    def test_identity_crosswalk_does_not_rewrite_gas_data(self):
        self.assertIn('sourceLinkedCas',REPO)
        self.assertIn('chemical_identity_equivalence_v6',REPO)
        self.assertNotIn('INSERT INTO calibration_curves',REPO)
if __name__=="__main__":
    unittest.main()
