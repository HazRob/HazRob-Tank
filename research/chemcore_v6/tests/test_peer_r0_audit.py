"""Provenance and arithmetic tests for external reference R0 studies."""
import json
import shutil
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
import peer_r0_audit as p
from source_audit import EvidenceError

class PublishedR0Tests(unittest.TestCase):
    def test_01_three_study_json_sources_sha_locked(self):
        self.assertEqual(len(p.sources()),3)

    def test_02_original_2018_nine_rows_eight_Tank_models(self):
        r=p.report()
        self.assertEqual(r["paper_2018_sensor_rows"],9)
        self.assertEqual(r["paper_2018_Tank_sensor_models"],8)

    def test_03_mq7_mq8_arithmetic_disagreement(self):
        self.assertEqual(p.report()["paper_arithmetic_issue_sensors"],["MQ7","MQ8"])

    def test_04_mq3_not_using_same_RL_as_MQ2(self):
        rows=p.report()["study_specific_RL_ohm"]
        self.assertEqual(rows["MQ3"],200000)
        self.assertEqual(rows["MQ2"],5000)
        self.assertNotEqual(rows["MQ3"],rows["MQ2"])

    def test_05_published_reference_R0_is_not_Tank_R0(self):
        r=p.report()
        self.assertIs(r["device_Tank_R0_authority"],False)
        self.assertTrue(all(row["peer_device_only_not_Tank_R0"] for row in r["paper_rows"]))

    def test_06_source_tampering_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            d=Path(tmp)/"peers"
            shutil.copytree(p.ROOT,d)
            f=d/"MQ_R0_RS_CROSS_STUDY_2026-10-21.json"
            f.write_bytes(f.read_bytes()+b"\n")
            with self.assertRaisesRegex(EvidenceError,"source SHA modified"):
                p.sources(d)

    def test_07_additional_studies_not_normative(self):
        r=p.report()
        self.assertEqual(r["historical_additional_source_count"],5)
        self.assertTrue(r["research_only"])

if __name__=="__main__":
    unittest.main()
