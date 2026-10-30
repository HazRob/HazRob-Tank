"""V6 authoritative ontology guard: mixtures and reporting indices are not pure CAS molecules."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[3]
SRC=(ROOT/"android/app/src/main/java/com/servoc/app/chemical/ChemicalIdentityCategoryV6.java").read_text()
BOOT=(ROOT/"android/app/src/main/java/com/servoc/app/chemical/ChemicalSeedBootstrap.java").read_text()
EMULATOR=(ROOT/"research/chemcore_v6/tests/android/V6ScientificRiskDbFixtureActivity.java").read_text()

class NineNonsingleMaterialLabels(unittest.TestCase):
    def test_names_are_typed_without_chemistry_fabrication(self):
        types=[
            ('136','LPG','MIXTURE_FUEL'),('137','CNG','MIXTURE_FUEL'),
            ('138','Natural Gas','MIXTURE_FUEL'),('139','Town Gas','MIXTURE_FUEL'),
            ('140','Smoke','MIXTURE_COMPLEX_AEROSOL'),('141','TVOC','AGGREGATE_VOC_METRIC'),
            ('142','NOx','AGGREGATE_REACTIVE_GAS_FAMILY'),
            ('143','LEL','MEASUREMENT_INDEX_NOT_CHEMICAL'),
            ('144','R410A','MIXTURE_REFRIGERANT')
        ]
        for uid,name,kind in types:
            self.assertIn(f'{{"{uid}","{name}","{kind}"}}',SRC)
        self.assertIn("Integer.parseInt(uid)-1",SRC)
        self.assertIn("legacy_id=?",SRC)
        self.assertIn("cas",SRC)
        self.assertIn("sql.beginTransaction()",SRC)
        self.assertIn("sql.setTransactionSuccessful()",SRC)
        self.assertNotIn('v.put("cas"',SRC)
        self.assertNotIn('v.put("formula"',SRC)
    def test_non_overwriting_and_android_fixture(self):
        self.assertIn("TRIM(chemical_class)=''",SRC)
        self.assertIn("ChemicalIdentityCategoryV6.ensure(db)",BOOT)
        self.assertIn("categoryAudit.already==9",EMULATOR)
        self.assertIn("categoryAudit.refused==0",EMULATOR)
        self.assertIn("cas IS NOT NULL AND TRIM(cas)<>''",EMULATOR)
if __name__=="__main__": unittest.main()
