"""V6 pre-APK source contracts for the ChemCore Stable offline library and GitHub update channel."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[3]
STABLE=(ROOT/"tools/prepare_v6_stable_source.py").read_text(encoding="utf-8")
WORKFLOW=(ROOT/".github/workflows/build-tank-android-v6.yml").read_text(encoding="utf-8")
CI=(ROOT/".github/workflows/chemcore-v6-embedded-stable-source-ci.yml").read_text(encoding="utf-8")
REPOSITORY=(ROOT/"android/app/src/main/java/com/servoc/app/chemical/ChemicalRepository.java").read_text(encoding="utf-8")
MANAGER=(ROOT/"android/app/src/main/java/com/servoc/app/chemcore/library/ChemCoreLibraryManager.java").read_text(encoding="utf-8")
CLIENT=(ROOT/"android/app/src/main/java/com/servoc/app/chemcore/library/ChemCoreLibraryUpdateClient.java").read_text(encoding="utf-8")
RELEASE=(ROOT/"android/app/src/main/java/com/servoc/app/ReleaseAudit.java").read_text(encoding="utf-8")

class StableV6UpdateContract(unittest.TestCase):
    def test_01_embedded_unchanged_hr069_stable_and_v6_compatibility(self):
        self.assertIn('public static final String VERSION="6.0"',RELEASE)
        self.assertIn('1.0.0-hr069r1',STABLE)
        self.assertIn('652 identities',STABLE)
        self.assertIn('49 research priors',STABLE)
        self.assertIn('"calibration_count":0',STABLE)
        self.assertIn('hashlib.sha256(raw).hexdigest()',STABLE)
        self.assertIn('prepare_v6_stable_source.py',WORKFLOW)
        self.assertNotIn("sed -i 's/5\\.0/6.0/g'",WORKFLOW)

    def test_02_github_only_channel_and_verified_download(self):
        self.assertIn('https://raw.githubusercontent.com/HazRob/HazRob-Tank/main/updates/chemcore-stable/manifest.json',WORKFLOW)
        self.assertIn('CHEMCORE_LIBRARY_MANIFEST_URL',WORKFLOW)
        self.assertIn('validateGitHubManifestUrl(source)',MANAGER)
        self.assertIn('verifyManifestOnly',MANAGER)
        self.assertIn('installVerifiedSnapshot',MANAGER)
        self.assertIn('downloadPayload(',MANAGER)
        self.assertIn('requireSameApprovedGithubDirectory',CLIENT)
        self.assertIn('MAX_PAYLOAD_BYTES',CLIENT)

    def test_03_unified_catalog_uid_only(self):
        self.assertIn('v14_unified_chemical_catalog',REPOSITORY)
        self.assertIn('external_mq_r0_rs_sensor_studies',REPOSITORY)
        self.assertIn('usable_as_tank_r0",false',REPOSITORY)
        self.assertIn('external_r0_rs_not_material_constants',REPOSITORY)

    def test_04_no_apk_before_full_plan_acceptance(self):
        self.assertIn('workflow_dispatch:',WORKFLOW)
        self.assertNotIn('  push:',WORKFLOW)
        self.assertIn('V6-ANDROID-SOFTWARE-ACCEPTANCE.json',WORKFLOW)
        self.assertIn("doc.get('owner_approved_apk_build') is True",WORKFLOW)
        self.assertIn('apk_produced',CI)
        self.assertIn("'apk_produced':False",CI)
        self.assertIn("manifest['model_count']==manifest['calibration_count']==0",CI)

if __name__=="__main__":unittest.main()
