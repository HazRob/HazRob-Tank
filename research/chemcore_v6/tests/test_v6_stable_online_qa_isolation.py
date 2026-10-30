"""N16 safety contract: verify real HTTPS without publishing test knowledge to main."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[3]
FLOW=(ROOT/'.github/workflows/chemcore-v6-n16-stable-https-qa-android12.yml').read_text(encoding='utf-8')
ACTIVITY=(ROOT/'research/chemcore_v6/tests/android/V6StableOnlineGithubActivity.java').read_text(encoding='utf-8')
RUNNER=(ROOT/'research/chemcore_v6/tests/android/run_n16_stable_real_github_https.sh').read_text(encoding='utf-8')

class RealGitHubQaOnly(unittest.TestCase):
    def test_qa_is_not_production_publication(self):
        self.assertIn('qa/v6-stable-online-$GITHUB_RUN_ID',FLOW)
        self.assertIn('git -C tank switch -c "$qa"',FLOW)
        self.assertIn('git -C tank push origin "HEAD:refs/heads/$qa"',FLOW)
        self.assertIn('git -C tank push origin --delete "$qa"',FLOW)
        self.assertIn('if: always()',FLOW)
        self.assertNotIn('refs/heads/main',FLOW)
        self.assertIn('no main',FLOW.lower())
        self.assertIn('release_apk_delivered',ACTIVITY)
        self.assertIn('production_main_modified',ACTIVITY)
    def test_transport_and_integrity_are_real(self):
        self.assertIn('ChemCoreLibraryUpdateClient client=new ChemCoreLibraryUpdateClient()',ACTIVITY)
        self.assertIn('ChemCoreLibraryManager.CheckSummary available=manager.check(appVersion)',ACTIVITY)
        self.assertIn('manager.install(available,appVersion',ACTIVITY)
        self.assertIn('ChemCoreLibraryVerifier.verifyPayload(store.activePayload()',ACTIVITY)
        self.assertIn("assert r['actual_https_download'] is True",RUNNER)
        self.assertIn("assert r['production_main_modified'] is False",RUNNER)
        self.assertIn("assert r['release_apk_delivered'] is False",RUNNER)
        self.assertIn('assert m[\'payload_sha256\']==hashlib.sha256(content).hexdigest()',FLOW)
        self.assertIn('assert m[\'calibration_count\']==0',FLOW)
    def test_nonproduction_branch_never_presumes_qualified_g3(self):
        self.assertIn('real_tank_g3_qualified',ACTIVITY)
        self.assertIn("assert r['real_tank_g3_qualified'] is False",RUNNER)

if __name__=='__main__':unittest.main()
