"""Official NIST/NIOSH correction of invalid literal and invalid CAS checksum."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[3]
SOURCE=(ROOT/"android/app/src/main/java/com/servoc/app/chemical/ChemicalCasCorrectionV6.java").read_text()
BOOT=(ROOT/"android/app/src/main/java/com/servoc/app/chemical/ChemicalSeedBootstrap.java").read_text()
EMULATOR=(ROOT/"research/chemcore_v6/tests/android/V6ScientificRiskDbFixtureActivity.java").read_text()
RUNNER=(ROOT/"research/chemcore_v6/tests/android/run_n09_scientific_risk_db.sh").read_text()
class SourceQualifiedCasRepair(unittest.TestCase):
    def test_authority_and_immutable_invalid_original(self):
        self.assertIn('{"715","Benzonitrile","null","100-47-0","NIST_WEBBOOK"',SOURCE)
        self.assertIn('"C7H5N","103.1213"',SOURCE)
        self.assertIn('"65966-93-2","65996-93-2","NIOSH_NPG"',SOURCE)
        self.assertIn('https://www.cdc.gov/niosh/npg/npgd0145.html',SOURCE)
        self.assertIn("chemical_cas_correction_audit_v6",SOURCE)
        self.assertIn("original_invalid_cas",SOURCE)
        self.assertIn('if(!initial&&!replay)',SOURCE)
        self.assertIn("sourceEvidenceExists",SOURCE)
        self.assertIn("db.beginTransaction()",SOURCE)
        self.assertIn("db.setTransactionSuccessful()",SOURCE)
        self.assertNotIn("DELETE FROM chemicals",SOURCE)
        self.assertNotIn("calibration_curves",SOURCE)
    def test_offline_boot_and_real_android_evidence(self):
        self.assertIn("ChemicalCasCorrectionV6.ensure(db)",BOOT)
        self.assertLess(BOOT.index("ChemicalNistIdentityCompletionV6.ensure(db)"),
                        BOOT.index("ChemicalCasCorrectionV6.ensure(db)"))
        self.assertIn("casChecksumValid(",EMULATOR)
        self.assertIn("invalidCasRows==0",EMULATOR)
        self.assertIn("cas_correction_idempotence",EMULATOR)
        self.assertIn("cas_checksum_invalid_direct_rows",RUNNER)
        self.assertIn("coverage['materials_with_formula']==668",RUNNER)
        self.assertIn("637<=coverage['materials_with_molar_mass']",RUNNER)
if __name__=='__main__':unittest.main()
