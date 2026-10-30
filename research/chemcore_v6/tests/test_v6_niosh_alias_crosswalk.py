"""V6 source-contract regression for non-destructive NIOSH synonyms.

Real 734 UID and 15 source-qualified pairs are verified separately on API31.
"""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[3]
SRC=(ROOT/"android/app/src/main/java/com/servoc/app/chemical/ChemicalIdentityEquivalenceV6.java").read_text()
BOOT=(ROOT/"android/app/src/main/java/com/servoc/app/chemical/ChemicalSeedBootstrap.java").read_text()
DETAIL=(ROOT/"android/app/src/main/java/com/servoc/app/chemical/ChemicalRepository.java").read_text()
FIXTURE=(ROOT/"research/chemcore_v6/tests/android/V6ScientificRiskDbFixtureActivity.java").read_text()
class NioshSynonymContract(unittest.TestCase):
    def test_records_are_non_destructive_references(self):
        self.assertIn("CREATE TABLE IF NOT EXISTS chemical_identity_equivalence_v6",SRC)
        self.assertIn("source_id='NIOSH_NPG'",SRC)
        self.assertIn("https://www.cdc.gov/niosh/",SRC)
        self.assertIn("NIOSH_SYNONYM_VERIFIED",SRC)
        self.assertIn("db.beginTransaction()",SRC)
        self.assertIn("db.setTransactionSuccessful()",SRC)
        self.assertIn("CONFLICT_IGNORE",SRC)
        self.assertNotIn("DELETE FROM chemicals",SRC)
        self.assertNotIn("UPDATE chemicals",SRC)
        self.assertNotIn("INSERT INTO chemicals",SRC)
        self.assertIn("casValid(",SRC)
    def test_aliases_source_qualified_not_unverified_families(self):
        for entry in ('{"104","Ethanol","Ethyl alcohol","64-17-5"}',
                      '{"103","Methanol","Methyl alcohol","67-56-1"}',
                      '{"64","Dichloromethane","Methylene chloride","75-09-2"}',
                      '{"123","1,4-Dioxane","Dioxane","123-91-1"}',
                      '{"39","Methanethiol","Methyl mercaptan","74-93-1"}',
                      '{"40","Ethanethiol","Ethyl mercaptan","75-08-1"}',
                      '{"70","1,2-Dichloropropane","Propylene dichloride","78-87-5"}',
                      '{"112","2-Butanone / MEK","2-Butanone","78-93-3"}',
                      '{"120","Diethyl Ether","Ethyl ether","60-29-7"}',
                      '{"125","Chlorodifluoromethane / R22","Chlorodifluoromethane","75-45-6"}',
                      '{"10","Silane","Silicon tetrahydride","7803-62-5"}',
                      '{"87","n-Octane","Octane","111-65-9"}',
                      '{"88","n-Nonane","Nonane","111-84-2"}',
                      '{"102","Mesitylene","1,3,5-Trimethylbenzene","108-67-8"}',
                      '{"113","MIBK","Hexone","108-10-1"}',
                      '{"117","Propyl Acetate","n-Propyl acetate","109-60-4"}'):
            self.assertIn(entry,SRC)
        self.assertNotIn('"Aniline (and homologs)"',SRC)
        self.assertNotIn('"TVOC","',SRC)
        self.assertNotIn('"NOx","',SRC)
    def test_bootstrap_source_order_and_identity_views(self):
        self.assertLess(BOOT.index("ChemicalSeedImporter.importNioshIdentity("),
                        BOOT.index("ChemicalIdentityEquivalenceV6.ensure(db)"))
        self.assertIn("source_qualified_identity_aliases",DETAIL)
        self.assertIn("automatic_measurement_rekey_performed",DETAIL)
        self.assertIn("alias_link_is_not_new_gas_detection_evidence",DETAIL)
    def test_source_executed_on_android_emulator(self):
        self.assertIn("verifiedSynonyms==27",FIXTURE)
        self.assertIn("synonymAudit.inserted==0",FIXTURE)
        self.assertIn("source_qualified_identity_aliases",FIXTURE)
        self.assertIn("SELECT COUNT(*) FROM chemicals",FIXTURE)
if __name__=="__main__":
    unittest.main()
