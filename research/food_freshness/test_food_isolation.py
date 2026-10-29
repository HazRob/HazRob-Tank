"""Verify food freshness reference data never becomes gas ChemCore input."""
import hashlib
import json
import math
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "research/food_freshness/source_snapshots/food_freshness_headspace_fingerprints_v1.json"
ANDROID = ROOT / "android/app/src/main/assets/food_freshness/reference/food_freshness_headspace_fingerprints_v1.json"
FOOD_MANIFEST = ROOT / "research/food_freshness/source_manifest.json"
GAS_MANIFEST = ROOT / "research/chemcore_v6/source_manifest.json"
GAS_SNAPSHOT = ROOT / "research/chemcore_v6/source_snapshots/hr069"
GAS_ASSETS = ROOT / "android/app/src/main/assets/chemcore_v6/reference"
GAS_JAVA = ROOT / "android/app/src/main/java/com/hazrob/tank/chemcore/v6"
ORIGINAL_BLOB = "959aae7574307a0030d218d4b86eb6618a6f72b0"
FILENAME = "food_freshness_headspace_fingerprints_v1.json"

def git_blob_sha(payload):
    return hashlib.sha1(b"blob " + str(len(payload)).encode("ascii") + b"\0" + payload).hexdigest()

class TestFoodResearchIsolation(unittest.TestCase):
    def test_original_content_and_android_copy_identical(self):
        source_bytes = SOURCE.read_bytes()
        app_bytes = ANDROID.read_bytes()
        self.assertEqual(source_bytes, app_bytes)
        self.assertEqual(git_blob_sha(source_bytes), ORIGINAL_BLOB)

    def test_fifty_nine_food_profiles_not_gas_ppm(self):
        j = json.loads(SOURCE.read_text(encoding="utf-8"))
        self.assertEqual(j["session_count"], 59)
        self.assertEqual(len(j["sessions"]), 59)
        self.assertEqual(len(j["sensor_order"]), 8)
        self.assertFalse(j["quantitative_ppm"])
        self.assertFalse(j["pure_chemical_identity"])
        for label, vector in j["sessions"]:
            self.assertTrue(isinstance(label, str) and bool(label))
            self.assertEqual(len(vector), 8)
            self.assertTrue(all(isinstance(v, (int,float)) and math.isfinite(v) and v > 0 for v in vector))

    def test_gas_manifest_and_gas_asset_exclude_food(self):
        gas = json.loads(GAS_MANIFEST.read_text(encoding="utf-8"))
        filenames = {row["filename"] for row in gas["research_sources"]["copies"]}
        self.assertEqual(len(filenames), 13)
        self.assertNotIn(FILENAME, filenames)
        self.assertFalse((GAS_SNAPSHOT / FILENAME).exists())
        self.assertFalse((GAS_ASSETS / FILENAME).exists())

    def test_independent_food_manifest(self):
        m = json.loads(FOOD_MANIFEST.read_text(encoding="utf-8"))
        self.assertTrue(m["no_gas_chemcore_import"])
        self.assertTrue(m["no_molecular_gas_identity"])
        self.assertTrue(m["no_calibrated_ppm"])
        self.assertEqual(m["original_session_count"], 59)
        self.assertEqual(m["original_source"]["git_blob_sha1"], ORIGINAL_BLOB)

    def test_gas_source_and_Java_engine_have_no_food_import(self):
        audit = (ROOT / "research/chemcore_v6/source_audit.py").read_text()
        self.assertNotIn("food_freshness", audit)
        self.assertNotIn("food_headspace_session_count", audit)
        for java_file in GAS_JAVA.glob("*.java"):
            code = java_file.read_text()
            self.assertNotIn("food_freshness", code.lower(), java_file.name)
            self.assertNotIn("foodfreshness", code.lower(), java_file.name)

if __name__ == "__main__":
    unittest.main()
