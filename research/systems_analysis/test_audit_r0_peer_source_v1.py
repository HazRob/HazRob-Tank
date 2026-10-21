"""Real published R0 source-table arithmetic test; never physical robot validation."""
import hashlib
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0,str(Path(__file__).resolve().parent))
import audit_r0_peer_source_v1 as r0


class PublishedR0ReferenceAuditTest(unittest.TestCase):
    def test_01_source_blob_matches_central_original(self):
        payload = r0.SOURCE_PATH.read_bytes()
        digest = hashlib.sha1(b"blob "+str(len(payload)).encode()+b"\0"+payload)
        self.assertEqual(digest.hexdigest(),r0.PINNED_ORIGINAL_GIT_BLOB)

    def test_02_exactly_eight_studies_and_nine_source_models(self):
        audit = r0.audit()
        self.assertEqual(audit["n_studies_documented"],8)
        self.assertEqual(audit["baseline_sensor_models"],9)
        self.assertEqual(audit["independent_Tank_exposures"],0)

    def test_03_table_2018_mq7_and_mq8_are_arithmetically_inconsistent(self):
        report=r0.audit()
        self.assertEqual(set(report["inconsistent_models"]),{"MQ7","MQ8"})
        model={x["sensor"]:x for x in report["baseline_rows"]}
        self.assertAlmostEqual(model["MQ7"]["r0_recomputed_kohm"],2.94383,places=3)
        self.assertAlmostEqual(model["MQ8"]["r0_recomputed_kohm"],0.64078,places=3)
        self.assertGreater(abs(model["MQ7"]["r0_recompute_disagreement_percent"]),50)
        self.assertGreater(abs(model["MQ8"]["r0_recompute_disagreement_percent"]),50)

    def test_04_other_seven_rows_are_internally_consistent(self):
        rows=r0.audit()["baseline_rows"]
        others=[x for x in rows if x["sensor"] not in ("MQ7","MQ8")]
        self.assertEqual(len(others),7)
        self.assertTrue(all(not x["table_internal_conflict"] for x in others))

    def test_05_mq9_cross_study_numeric_similarity_not_standard(self):
        result=r0.audit()
        self.assertAlmostEqual(result["mq9_cross_source_difference_percent"],1.836,delta=.01)
        self.assertIn("NOT_A_REFERENCE_R0_STANDARD",result["mq9_near_match_interpretation"])

    def test_06_no_safety_ppm_calibration_claim(self):
        r=r0.audit()
        self.assertEqual(r["new_empirically_validated_Tank_methods"],0)
        self.assertEqual(r["scientific_scope"],"SOURCE_TABLE_ARITHMETIC_NOT_TANK_CALIBRATION")

    def test_07_tampered_original_bytes_rejected(self):
        with tempfile.TemporaryDirectory() as td:
            test=Path(td)/"tampered.json"
            test.write_bytes(r0.SOURCE_PATH.read_bytes()+b"\n")
            with patch.object(r0,"SOURCE_PATH",test),self.assertRaisesRegex(ValueError,"source changed"):
                r0.load_original()


if __name__=="__main__":
    unittest.main()
