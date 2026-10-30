"""V6 scientific-source contract: enrich canonical materials, never invent MQ/air data.

These checks are static; actual SQLite and Android tests run separately.
"""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[3]
PROJECTION=(ROOT/"android/app/src/main/java/com/servoc/app/chemical/ChemicalMolecularProjectionV6.java").read_text()
BOOTSTRAP=(ROOT/"android/app/src/main/java/com/servoc/app/chemical/ChemicalSeedBootstrap.java").read_text()
REPOSITORY=(ROOT/"android/app/src/main/java/com/servoc/app/chemical/ChemicalRepository.java").read_text()
EMULATOR=(ROOT/"research/chemcore_v6/tests/android/V6ScientificRiskDbFixtureActivity.java").read_text()
CI=(ROOT/"research/chemcore_v6/tests/android/run_n09_scientific_risk_db.sh").read_text()

class VerifiedMolecularProjection(unittest.TestCase):
    def test_only_existing_pubchem_cas_matched_source_records(self):
        self.assertIn("chemcore_properties",PROJECTION)
        self.assertIn("source_id='PUBCHEM'",PROJECTION)
        self.assertIn("quality_tier='A_SOURCE'",PROJECTION)
        self.assertIn("MolecularFormula",PROJECTION)
        self.assertIn("MolecularWeight",PROJECTION)
        self.assertIn("SELECT p.value_text",PROJECTION)
        self.assertIn("SELECT p.value_numeric",PROJECTION)
        self.assertIn("formula IS NULL OR TRIM(formula)=''",PROJECTION)
        self.assertIn("molar_mass IS NULL OR molar_mass<=0",PROJECTION)
        self.assertIn("sql.beginTransaction()",PROJECTION)
        self.assertIn("sql.setTransactionSuccessful()",PROJECTION)
        self.assertNotIn("UPDATE mq_",PROJECTION)
        self.assertNotIn("INSERT INTO chemicals",PROJECTION)

    def test_import_after_authoritative_molecular_seed(self):
        importer=BOOTSTRAP.index("ChemCoreMolecularSeedImporter.importBestAvailable")
        projection=BOOTSTRAP.index("ChemicalMolecularProjectionV6.ensure(db)")
        qspr=BOOTSTRAP.index("ChemCoreQsprRuntimeRegistry.rebuild(db)")
        self.assertLess(importer,projection)
        self.assertLess(projection,qspr)

    def test_one_uid_and_per_field_source_disclosure(self):
        self.assertIn("v14_unified_chemical_catalog",REPOSITORY)
        self.assertIn("molecular_identity_source_facts",REPOSITORY)
        self.assertIn("is_gas_detection_evidence",REPOSITORY)
        self.assertIn("source_id='PUBCHEM'",REPOSITORY)

    def test_tested_real_sqlite_coverage_and_no_fake_completion(self):
        self.assertIn("v6_chemical_catalog_coverage.csv",EMULATOR)
        self.assertIn("writeCoverageAudit(sql)",EMULATOR)
        self.assertIn("ChemicalMolecularProjectionV6.ensure(db)",EMULATOR)
        self.assertIn('scientific_fields_exhaustive",false',EMULATOR)
        self.assertIn("material_field_coverage_audit",CI)
        self.assertIn("len(records)==734",CI)
        self.assertIn("NULL_NOT_ZERO_NO_SAFETY_INFERENCE",CI)

if __name__=="__main__": unittest.main()
