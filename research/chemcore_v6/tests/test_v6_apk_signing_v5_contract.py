"""Release signature preflight: V4 signing secret must not be mislabeled as V5-upgrade safe."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[3]
FLOW=(ROOT/".github/workflows/build-tank-android-v6.yml").read_text(encoding="utf-8")
ACCEPT=(ROOT/"docs/implementation/V6-ANDROID-SOFTWARE-ACCEPTANCE.json").read_text(encoding="utf-8")
V5_CERT="97F0E315449BB369D1C2042CD092E73E422CE9718406D93F8CCB1E40C6921BD1"
V4_EXPECTED="799BED750710DC02EE09B3976E07EEE407776CEA225F7BF438E92DB6428B0DF6"
class V5SignerGuard(unittest.TestCase):
    def test_signer_evidence_does_not_pretend_v4_key_updates_v5(self):
        self.assertIn(V5_CERT,FLOW)
        self.assertIn(V4_EXPECTED,FLOW)
        self.assertNotEqual(V5_CERT,V4_EXPECTED)
        self.assertIn('echo "v5_upgrade_compatible=false" >> "$GITHUB_OUTPUT"',FLOW)
        self.assertIn("V4 ONLY",FLOW)
        self.assertIn("V5 in-place upgrade supported by this build",FLOW)
        self.assertIn("private key",FLOW)
    def test_apk_build_still_gated_by_owner(self):
        self.assertIn("workflow_dispatch:",FLOW)
        self.assertNotIn("\n  push:",FLOW)
        self.assertIn("doc.get('owner_approved_apk_build') is True",FLOW)
        self.assertIn('"owner_approved_apk_build": false',ACCEPT)
        self.assertIn('"release_authorization": "NO_RELEASE"',ACCEPT)
if __name__=="__main__":unittest.main()
