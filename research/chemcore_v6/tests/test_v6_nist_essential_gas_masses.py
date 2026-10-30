"""NIST SRD69 essential molecular properties: not ambient gas safety claims."""
from pathlib import Path
import re
import unittest

ROOT=Path(__file__).resolve().parents[3]
SOURCE=(ROOT/"android/app/src/main/java/com/servoc/app/chemical/ChemicalNistEssentialGasMassV6.java").read_text()
BOOT=(ROOT/"android/app/src/main/java/com/servoc/app/chemical/ChemicalSeedBootstrap.java").read_text()
ACTIVITY=(ROOT/"research/chemcore_v6/tests/android/V6ScientificRiskDbFixtureActivity.java").read_text()
REPORT=(ROOT/"android/app/src/main/java/com/servoc/app/chemical/ChemicalRepository.java").read_text()

class EssentialGasNist(unittest.TestCase):
    def test_exact_nist_cas_formula_and_molecular_weight(self):
        self.assertIn('{"23","Oxygen","O2","7782-44-7","31.9988"}',SOURCE)
        self.assertIn('{"77","Methane","CH4","74-82-8","16.0425"}',SOURCE)
        self.assertIn("https://webbook.nist.gov/cgi/cbook.cgi?ID=",SOURCE)
        self.assertIn('"NIST_WEBBOOK"',SOURCE)
        self.assertIn("if(Math.abs(row.getDouble(4)-molecularWeight)>0.02)",SOURCE)
        self.assertIn('molar_mass IS NULL OR molar_mass<=0',SOURCE)
        self.assertIn("db.beginTransaction()",SOURCE)
        self.assertIn("db.setTransactionSuccessful()",SOURCE)
        self.assertNotIn('put("r0"',SOURCE)
        self.assertNotIn('put("ppm"',SOURCE)
        self.assertNotIn('put("natural_',SOURCE)
        self.assertNotIn("chemical_hazard_boundaries",SOURCE)
    def test_provenance_and_idempotence_verified_by_real_android(self):
        self.assertIn("ChemicalNistEssentialGasMassV6.ensure(db)",BOOT)
        self.assertIn("essentialGasReplay.filled==0",ACTIVITY)
        self.assertIn("essentialGasReplay.verified==2",ACTIVITY)
        self.assertIn("31.9988",ACTIVITY)
        self.assertIn("16.0425",ACTIVITY)
        self.assertIn('"nist_direct_cas_source"',REPORT)
if __name__=="__main__":unittest.main()
