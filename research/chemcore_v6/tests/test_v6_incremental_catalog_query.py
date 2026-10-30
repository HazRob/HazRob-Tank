"""V6 record-based ChemCore: central publication approval is authoritative.

No full Stable replacement, no post-download scientific requalification.
A transport SHA-256 and immutable UID/CAS guard do remain mandatory.
"""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[3]
CHEM=(ROOT/"android/app/src/main/java/com/servoc/app/chemical/ChemicalReferenceSyncV6.java").read_text()
STATUS=(ROOT/"android/app/src/main/java/com/servoc/app/chemical/ChemicalExperimentStatusRepository.java").read_text()
MATRIX=(ROOT/"android/app/src/main/java/com/servoc/app/ui/ChemicalExperimentMatrixDialog.java").read_text()
SETTINGS=(ROOT/"android/app/src/main/java/com/servoc/app/ui/SettingsTab.java").read_text()
GLOBAL=(ROOT/"android/app/src/main/java/com/servoc/app/ui/StatusTab.java").read_text()
LAB=(ROOT/"android/app/src/main/java/com/servoc/app/ui/DeveloperExperimentsTab.java").read_text()
DASHBOARD=(ROOT/"android/app/src/main/java/com/servoc/app/ui/CentralLibraryDashboardTab.java").read_text()
EMULATOR=(ROOT/"research/chemcore_v6/tests/android/V6ScientificRiskDbFixtureActivity.java").read_text()
INDEX=(ROOT/"updates/chemcore-records/v6/index.json").read_text()
class CentralLibraryRecordQueries(unittest.TestCase):
    def test_small_changes_not_stable_file(self):
        self.assertIn('main/updates/chemcore-records/v6/index.json',CHEM)
        self.assertIn('private static final int MAX_BATCH=25;',CHEM)
        self.assertIn('chemical_reference_updates_v6',CHEM)
        self.assertIn('chemical_reference_sync_receipts_v6',CHEM)
        self.assertIn('currentSha(db,uid)',CHEM)
        self.assertLess(CHEM.index("if(digest.equals(currentSha(db,uid)))"),
                        CHEM.index("if(candidates>=maxChanges)break"))
        self.assertIn('if(requestedUid!=0&&uid!=requestedUid)continue;',CHEM)
        self.assertIn('"records": []',INDEX)
    def test_transport_integrity_but_no_extra_scientific_approval(self):
        self.assertIn('digest.equals(sha256(bytes))',CHEM)
        self.assertIn('!cas.equals(localCas(db,uid))',CHEM)
        self.assertNotIn('research_only',CHEM)
        self.assertNotIn('unreviewed authority code',CHEM)
        self.assertIn('PUBLISHED_LIBRARY_',CHEM)
        self.assertIn('library_status',CHEM)
        self.assertIn('db.delete("chemical_reference_updates_v6","chemical_uid=?"',CHEM)
        self.assertIn('db.setTransactionSuccessful()',CHEM)
    def test_all_four_capabilities_mirrored_in_existing_lists(self):
        for k in ('fingerprint_reference','mq_curve_reference','ppm_reference','m4_reference'):
            self.assertIn(k,CHEM)
            self.assertIn(k,STATUS)
        self.assertIn('publishedM4Qualified',STATUS)
        self.assertIn('publishedFingerprintQualified',STATUS)
        self.assertIn('publishedCurveQualified',STATUS)
        self.assertIn('publishedPpmQualified',STATUS)
        self.assertIn('✓ مكتبة',STATUS)
        self.assertIn('✓ جهاز',STATUS)
        self.assertIn('Button curve=header(context,"منحنى"',MATRIX)
        self.assertIn('s.capabilityLine()',LAB)
        self.assertIn('countRemoteResearchM4Materials()',DASHBOARD)
    def test_update_controls_do_not_download_whole_stable(self):
        self.assertIn('ChemicalReferenceSyncV6.refresh(',SETTINGS)
        self.assertIn('ChemicalReferenceSyncV6.refresh(',GLOBAL)
        self.assertIn('host.refreshAllTabs()',SETTINGS)
        self.assertIn('host.refreshAllTabs()',GLOBAL)
        self.assertNotIn('libraryUpdater.install(checked',GLOBAL)
        self.assertNotIn('libraryManager.install(c,ReleaseAudit.VERSION',SETTINGS)
    def test_real_sqlite_qa_covers_publication_and_withdrawal(self):
        self.assertIn('central_published_capability_sync_e2e',EMULATOR)
        self.assertIn('withdrawn.referenceFingerprint',EMULATOR)
        self.assertIn('withdrawn.publishedM4Qualified',EMULATOR)
        self.assertIn('ChemicalReferenceSyncV6.applyVerifiedRecord(',EMULATOR)
if __name__=='__main__':unittest.main()
