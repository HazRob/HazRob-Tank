"""Regression tests for strict, four-stage external MQ4 receipt integrity."""
import copy
import json
import tempfile
import unittest
from pathlib import Path

from g2_mq4_receipt_lineage_ci import ROOT, FILENAMES, audit, strict_json


class ReceiptLineageTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.original = {key: (ROOT / name).read_bytes()
                        for key, name in FILENAMES.items()}

    def check(self, changes=None):
        with tempfile.TemporaryDirectory() as folder:
            paths = {}
            for key, raw in self.original.items():
                path = Path(folder) / FILENAMES[key]
                path.write_bytes((changes or {}).get(key, raw))
                paths[key] = path
            return audit(paths)

    def changed(self, name, edit):
        obj = json.loads(self.original[name])
        edit(obj)
        return {name: json.dumps(obj, ensure_ascii=False).encode("utf-8")}

    def test_real_committed_receipts_recompute(self):
        out = self.check()
        self.assertEqual(out["test_rows"], 9325)
        self.assertEqual(out["reference_positives"], 1425)
        self.assertEqual(out["calendar_days"], 7)
        self.assertEqual(len(out["receipt_sha256"]), 4)
        self.assertFalse(out["original_source_zip_replayed_in_ci"])

    def test_reproducible_manifest(self):
        self.assertEqual(self.check(), self.check())

    def test_duplicate_top_level_json_key_rejected(self):
        raw = self.original["source"].replace(b'"schema":',
                                               b'"schema":"fake","schema":', 1)
        with self.assertRaisesRegex(ValueError, "DUPLICATE_KEY"):
            self.check({"source": raw})

    def test_duplicate_nested_json_key_rejected(self):
        raw = self.original["source"].replace(b'"models":',
                                               b'"models":{},"models":', 1)
        with self.assertRaisesRegex(ValueError, "DUPLICATE_KEY"):
            self.check({"source": raw})

    def test_nonfinite_number_rejected(self):
        raw = self.changed("source", lambda x: x.update({"invalid_number": float("nan")}))
        with self.assertRaisesRegex(ValueError, "NONFINITE"):
            self.check(raw)

    def test_source_prediction_mutation_rejected(self):
        def edit(x):
            x["models"]["RIDGE_MQ4_1"]["days"][0]["mae"] += 0.01
        with self.assertRaises(ValueError):
            self.check(self.changed("source", edit))

    def test_stability_mutation_rejected(self):
        def edit(x):
            x["models"]["RIDGE_MQ4_1"]["pooled"]["specificity"] = 0.5
        with self.assertRaisesRegex(ValueError, "STABILITY_LINEAGE_MISMATCH"):
            self.check(self.changed("stability", edit))

    def test_ranking_mutation_rejected(self):
        with self.assertRaisesRegex(ValueError, "RANKING_LINEAGE_MISMATCH"):
            self.check(self.changed("ranking",
                                    lambda x: x.update({"pairwise_reversals": 0})))

    def test_support_mutation_rejected(self):
        with self.assertRaisesRegex(ValueError, "SUPPORT_LINEAGE_MISMATCH"):
            self.check(self.changed("support",
                                    lambda x: x.update({"baseline": "FORGED"})))

    def test_promotion_claim_rejected(self):
        with self.assertRaisesRegex(ValueError, "UNSUPPORTED_PROMOTION"):
            self.check(self.changed("support",
                                    lambda x: x.update({"g2_qualified": True})))

    def test_safety_threshold_claim_rejected(self):
        with self.assertRaisesRegex(ValueError, "SAFETY_THRESHOLD_CLAIM"):
            self.check(self.changed("source",
                                    lambda x: x.update({"threshold_4ppm_is_safety_limit": True})))

    def test_file_scope_and_missing_receipt_rejected(self):
        with self.assertRaisesRegex(ValueError, "FILE_SET_MISMATCH"):
            audit({"source": ROOT / FILENAMES["source"]})
        with tempfile.TemporaryDirectory() as folder:
            paths = {key: Path(folder) / name for key, name in FILENAMES.items()}
            with self.assertRaises(FileNotFoundError):
                audit(paths)


if __name__ == "__main__":
    unittest.main()
