"""Synthetic mutation tests of historical MQ4 cross-report provenance only."""
import copy
import json
import unittest
from pathlib import Path

from g2_mq4_pairwise_frechet_bounds import EVIDENCE, SOURCE
from g2_mq4_three_way_source_reconciliation import REPLAY, PROTOCOL, reconcile

REPLAY_RAW = REPLAY.read_bytes()
PROTOCOL_RAW = PROTOCOL.read_bytes()
SOURCE_RAW = SOURCE.read_bytes()
EVIDENCE_RAW = EVIDENCE.read_bytes()
PINNED = Path(__file__).resolve().parent / "G2_MQ4_THREE_WAY_SOURCE_RECONCILIATION_2026-11-01.json"


def check(replay=None, protocol=None, source=SOURCE_RAW, evidence=EVIDENCE_RAW):
    rr = REPLAY_RAW if replay is None else json.dumps(replay, sort_keys=True).encode()
    pr = PROTOCOL_RAW if protocol is None else json.dumps(protocol, sort_keys=True).encode()
    return reconcile(rr, pr, source, evidence)


class SourceReconciliationTests(unittest.TestCase):
    def test_pinned_historical_reports_match_expected_output(self):
        self.assertEqual(check(), json.loads(PINNED.read_text(encoding="utf-8")))
        self.assertFalse(check()["raw_zip_replayed_in_current_ci"])
        self.assertFalse(check()["g2_qualified"])

    def test_changed_archive_sha_rejected(self):
        obj = json.loads(REPLAY_RAW)
        obj["archive_sha256"] = "0" * 64
        with self.assertRaisesRegex(ValueError, "RAW_REPLAY_LINEAGE_MISMATCH"):
            check(replay=obj)

    def test_reallocated_daily_rows_same_total_rejected(self):
        obj = json.loads(REPLAY_RAW)
        obj["derived"]["daily_rows"]["2021-07-21"] += 1
        obj["derived"]["daily_rows"]["2021-07-22"] -= 1
        with self.assertRaisesRegex(ValueError, "RAW_REPLAY_DAY_COUNT_MISMATCH"):
            check(replay=obj)

    def test_mutated_lag_context_rejected(self):
        obj = json.loads(REPLAY_RAW)
        obj["derived"]["lags"]["120"]["cross_day_context_rows"] += 1
        with self.assertRaisesRegex(ValueError, "RAW_REPLAY_LAG_CONTEXT_MISMATCH"):
            check(replay=obj)

    def test_unverified_replay_promotion_rejected(self):
        obj = json.loads(REPLAY_RAW)
        obj["g2_qualified"] = True
        with self.assertRaisesRegex(ValueError, "RAW_REPLAY_UNSUPPORTED_QUALIFICATION"):
            check(replay=obj)

    def test_changed_library_script_hash_rejected(self):
        obj = json.loads(PROTOCOL_RAW)
        obj["source_library_benchmark_script_sha256"] = "0" * 64
        with self.assertRaisesRegex(ValueError, "LIBRARY_PROTOCOL_LINEAGE_MISMATCH"):
            check(protocol=obj)

    def test_changed_fold_calibration_rejected(self):
        obj = json.loads(PROTOCOL_RAW)
        obj["folds"][0]["calibration_day"] = "2021-07-22"
        with self.assertRaisesRegex(ValueError, "LIBRARY_PROTOCOL_TEMPORAL_FLAG_MISMATCH"):
            check(protocol=obj)

    def test_changed_future_training_flag_rejected(self):
        obj = json.loads(PROTOCOL_RAW)
        obj["folds"][0]["future_training_days"] = []
        with self.assertRaisesRegex(ValueError, "LIBRARY_PROTOCOL_TEMPORAL_FLAG_MISMATCH"):
            check(protocol=obj)

    def test_changed_training_partition_rejected(self):
        obj = json.loads(PROTOCOL_RAW)
        obj["folds"][0]["training_days"][-1] = "2021-07-31"
        with self.assertRaisesRegex(ValueError, "LIBRARY_PROTOCOL_FOLD_MISMATCH"):
            check(protocol=obj)

    def test_segment_reallocation_preserving_total_rejected(self):
        obj = json.loads(REPLAY_RAW)
        obj["derived"]["segment_lengths"][0] += 1
        obj["derived"]["segment_lengths"][1] -= 1
        with self.assertRaisesRegex(ValueError, "RAW_REPLAY_SEGMENT_MISMATCH"):
            check(replay=obj)

    def test_segment_reordering_rejected(self):
        obj = json.loads(REPLAY_RAW)
        segments = obj["derived"]["segment_lengths"]
        segments[0], segments[1] = segments[1], segments[0]
        with self.assertRaisesRegex(ValueError, "RAW_REPLAY_SEGMENT_MISMATCH"):
            check(replay=obj)

    def test_short_lag_mutation_rejected(self):
        obj = json.loads(REPLAY_RAW)
        obj["derived"]["lags"]["5"]["contiguous_lag_pairs"] += 1
        with self.assertRaisesRegex(ValueError, "RAW_REPLAY_LAG_CONTEXT_MISMATCH"):
            check(replay=obj)

    def test_lag60_cross_day_distribution_rejected(self):
        obj = json.loads(REPLAY_RAW)
        obj["derived"]["lags"]["60"]["cross_day_by_later_date"]["2021-07-22"] -= 1
        with self.assertRaisesRegex(ValueError, "RAW_REPLAY_LAG_CONTEXT_MISMATCH"):
            check(replay=obj)

    def test_missing_lag_scale_rejected(self):
        obj = json.loads(REPLAY_RAW)
        del obj["derived"]["lags"]["15"]
        with self.assertRaisesRegex(ValueError, "RAW_REPLAY_LAG_CONTEXT_MISMATCH"):
            check(replay=obj)

    def test_boolean_lag_count_rejected(self):
        obj = json.loads(REPLAY_RAW)
        obj["derived"]["lags"]["1"]["unavailable_rows"] = True
        with self.assertRaisesRegex(ValueError, "RAW_REPLAY_LAG_CONTEXT_MISMATCH"):
            check(replay=obj)

    def test_max_lag_day_eligibility_rejected(self):
        obj = json.loads(REPLAY_RAW)
        obj["derived"]["max_lag_eligible_by_day"]["2021-07-26"] -= 1
        with self.assertRaisesRegex(ValueError, "RAW_REPLAY_MAX_LAG_ELIGIBILITY_MISMATCH"):
            check(replay=obj)

    def test_timestamp_mutation_rejected(self):
        obj = json.loads(REPLAY_RAW)
        obj["derived"]["first_timestamp"] = "2021-07-21 11:07:00"
        with self.assertRaisesRegex(ValueError, "RAW_REPLAY_TIMELINE_MISMATCH"):
            check(replay=obj)

    def test_raw_member_size_mutation_rejected(self):
        obj = json.loads(REPLAY_RAW)
        obj["raw_member_bytes"] += 1
        with self.assertRaisesRegex(ValueError, "RAW_REPLAY_BYTES_MISMATCH"):
            check(replay=obj)

    def test_duplicate_json_keys_rejected(self):
        raw = REPLAY_RAW.replace(b'"source_id":', b'"source_id": "fake", "source_id":', 1)
        with self.assertRaisesRegex(ValueError, "DUPLICATE_JSON_KEY"):
            reconcile(raw, PROTOCOL_RAW, SOURCE_RAW, EVIDENCE_RAW)

    def test_mutated_pinned_source_summary_rejected(self):
        with self.assertRaisesRegex(ValueError, "SOURCE_SHA_MISMATCH"):
            check(source=SOURCE_RAW + b" ")

    def test_nonempty_independent_evidence_rejected(self):
        with self.assertRaisesRegex(ValueError, "UNVERIFIED_INDEPENDENT_EVIDENCE"):
            check(evidence=b'{"schema":"hazrob.g2.independent-evidence.v1","records":[{"unverified":true}]}')
