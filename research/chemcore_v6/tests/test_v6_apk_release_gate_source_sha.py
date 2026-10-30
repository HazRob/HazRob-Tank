"""V6 delivery must be owner-approved and exactly cover the code tested in CI.

An approval JSON file cannot contain its own commit hash, so the gate permits
ONLY a later change to this acceptance metadata, never a changed APK/firmware.
"""
from pathlib import Path
import json,re,unittest

ROOT=Path(__file__).resolve().parents[3]
FLOW=(ROOT/".github/workflows/build-tank-android-v6.yml").read_text(encoding="utf-8")
ACCEPT=json.loads((ROOT/"docs/implementation/V6-ANDROID-SOFTWARE-ACCEPTANCE.json").read_text())

class ReleaseGate(unittest.TestCase):
    def test_all_release_decisions_fail_closed_until_owner_authorizes(self):
        self.assertIn("workflow_dispatch:",FLOW)
        self.assertNotIn("\n  push:",FLOW)
        self.assertIn("'OWNER_APPROVED_RELEASE'",FLOW)
        self.assertIn("'owner_approved_apk_build'",FLOW)
        self.assertIn("'all_software_acceptance_tests_passed'",FLOW)
        self.assertIn("'all_software_modifications_complete'",FLOW)
        self.assertIs(ACCEPT['owner_approved_apk_build'],False)
        self.assertEqual(ACCEPT['release_authorization'],'NO_RELEASE')
    def test_pinned_tested_sha_and_approval_metadata_are_only_difference(self):
        self.assertIn("re.fullmatch('[0-9a-f]{40}',verified)",FLOW)
        self.assertIn("'merge-base','--is-ancestor',verified,head",FLOW)
        self.assertIn("'diff','--name-only',verified,head",FLOW)
        self.assertIn("'docs/implementation/V6-ANDROID-SOFTWARE-ACCEPTANCE.json'",FLOW)
        self.assertIn("changed<=allowed",FLOW)
        self.assertIn("unaudited changes since tested commit",FLOW)
        self.assertLess(FLOW.index("tested commit is not ancestor"),FLOW.index("Package V6 ESP32"))
    def test_signing_cannot_pretend_v4_certificate_updates_v5(self):
        self.assertIn("v5_upgrade_compatible=false",FLOW)
        self.assertIn("97F0E315449BB369D1C2042CD092E73E422CE9718406D93F8CCB1E40C6921BD1",FLOW)
if __name__=='__main__':unittest.main()
