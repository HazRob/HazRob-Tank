"""Source qualification for non-destructive historical NIOSH alias molecular masses."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[3]
IMPL=(ROOT/"android/app/src/main/java/com/servoc/app/chemical/ChemicalAliasMolecularProjectionV6.java").read_text()
BOOT=(ROOT/"android/app/src/main/java/com/servoc/app/chemical/ChemicalSeedBootstrap.java").read_text()
PROFILE=(ROOT/"android/app/src/main/java/com/servoc/app/chemical/ChemicalRepository.java").read_text()
EMULATOR=(ROOT/"research/chemcore_v6/tests/android/V6ScientificRiskDbFixtureActivity.java").read_text()
class AliasMassProvenance(unittest.TestCase):
    def test_niosh_cas_qualified_pubchem_and_missing_only(self):
        for token in ("x.evidence_status='NIOSH_SYNONYM_VERIFIED'",
                      "canonical.cas=x.source_cas",
                      "p.property_key='MolecularWeight'",
                      "p.source_id='PUBCHEM'",
                      "p.quality_tier='A_SOURCE'",
                      "p.value_numeric>0",
                      "molar_mass IS NULL OR molar_mass<=0",
                      "db.beginTransaction()",
                      "db.setTransactionSuccessful()"):
            self.assertIn(token,IMPL)
        self.assertNotIn("UPDATE chemical_identity_equivalence_v6",IMPL)
        self.assertNotIn("DELETE FROM chemicals",IMPL)
        self.assertNotIn('SET cas=',IMPL)
        self.assertNotIn('SET r0=',IMPL)
        self.assertNotIn("INSERT INTO calibration",IMPL)
    def test_bootstrap_sequence(self):
        a=BOOT.index("ChemicalIdentityEquivalenceV6.ensure(db)")
        b=BOOT.index("ChemCoreMolecularSeedImporter.importBestAvailable")
        c=BOOT.index("ChemicalMolecularProjectionV6.ensure(db)")
        d=BOOT.index("ChemicalAliasMolecularProjectionV6.ensure(db)")
        e=BOOT.index("ChemCoreQsprRuntimeRegistry.rebuild(db)")
        self.assertLess(a,b)
        self.assertLess(b,c)
        self.assertLess(c,d)
        self.assertLess(d,e)
    def test_scientific_profile_source_retained_without_alarm_promotion(self):
        for token in ("niosh_alias_pubchem_mass_sources","niosh_cas",
                      "niosh_source_url","IDENTICAL_SUBSTANCE_ONLY_NOT_TANK_GAS_DETECTION"):
            self.assertIn(token,PROFILE)
        for token in ("aliasMassReplay.filled==0","aliasMassReplay.eligibleAfter==0",
                      "niosh_alias_pubchem_mass_sources","totalMassBeforeAliasReplay"):
            self.assertIn(token,EMULATOR)
if __name__=="__main__":unittest.main()
