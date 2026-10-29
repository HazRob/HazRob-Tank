"""G2 archive-report lineage tests. They do not replay original raw UCI exposures."""
import shutil
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
import g2_portfolio_audit as p
from source_audit import EvidenceError

class G2PortfolioTests(unittest.TestCase):
    def test_01_four_historical_source_result_blobs(self):
        self.assertEqual(len(p.load_reports()),4)

    def test_02_counts_are_independent_exposures_not_timepoints(self):
        report=p.report()
        self.assertEqual(report["distinct_external_exposure_counts"],
                         {"uci1081":90,"uci270":13910,"uci308":58,"uci309":180})
        self.assertEqual(report["total_distinct_external_source_exposures"],14238)

    def test_03_duplicate_uci224_is_not_recounted(self):
        self.assertTrue(p.report()["uci270_UCI224_same_dataset_mirror"])

    def test_04_no_new_raw_archive_claim(self):
        self.assertEqual(set(p.report()["unverified_raw_source_bytes_in_current_V6_task"]),
                         {"uci270","uci308","uci309"})
        self.assertTrue(p.report()["record_type"].startswith("VERIFIED_HISTORY"))

    def test_05_source_sensor_family_is_not_Tank_validation(self):
        record=p.report()
        self.assertTrue(record["source_sensor_platforms_not_Tank_MQ_array"])
        self.assertFalse(record["M4_quantitative_claim"])
        self.assertEqual(record["Tank_G3_measurement_promotions"],0)

    def test_06_mixture_ppm_time_semantics(self):
        self.assertTrue(p.report()["uci309_mean_GCMS_ppm_is_not_per_timestamp_ppm"])
        self.assertTrue(p.report()["flow_modulation_liquid_preparation_is_not_gas_phase_ppm"])

    def test_07_modified_history_fail_closed(self):
        with tempfile.TemporaryDirectory() as temp:
            d=Path(temp)
            shutil.copytree(p.ROOT,d/"source")
            q=d/"source"/"uci309_g2_batch06_2026-10-20.json"
            q.write_bytes(q.read_bytes()+b"\n")
            with self.assertRaisesRegex(EvidenceError,"SHA_MISMATCH"):
                p.load_reports(d/"source")

    def test_08_additional_unknown_report_rejected(self):
        with tempfile.TemporaryDirectory() as temp:
            d=Path(temp)/"source"
            shutil.copytree(p.ROOT,d)
            (d/"untrusted.json").write_text("{}")
            with self.assertRaises(EvidenceError):
                p.load_reports(d)

if __name__=="__main__":
    unittest.main()
