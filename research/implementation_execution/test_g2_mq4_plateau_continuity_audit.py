"""Synthetic regressions for gap-aware MQ4 plateau audit (not physical validation)."""
import copy
import hashlib
import json
import unittest
from pathlib import Path
from g2_mq4_plateau_continuity_audit import audit_plateaus, REQUIRED


class MQ4PlateauContinuityTests(unittest.TestCase):
    def setUp(self):
        self.rows = [
            {'datetime': '2021-07-21 23:58:00', 'licor_ch4': '2000', 'MQ4_1': '10', 'MQ4_2': '20', 'MQ4_3': '30', 't': '20', 'rh': '50'},
            {'datetime': '2021-07-21 23:59:00', 'licor_ch4': '2060', 'MQ4_1': '11', 'MQ4_2': '20', 'MQ4_3': '30', 't': '20', 'rh': '50'},
            {'datetime': '2021-07-22 00:00:00', 'licor_ch4': '2110', 'MQ4_1': '12', 'MQ4_2': '20', 'MQ4_3': '30', 't': '20', 'rh': '50'},
            {'datetime': '2021-07-22 00:03:00', 'licor_ch4': '2500', 'MQ4_1': '13', 'MQ4_2': '20', 'MQ4_3': '30', 't': '20', 'rh': '50'},
            {'datetime': '2021-07-22 00:04:00', 'licor_ch4': '2700', 'MQ4_1': '14', 'MQ4_2': '20', 'MQ4_3': '30', 't': '20', 'rh': '50'},
        ]

    def test_gap_excludes_false_adjacent_pair_and_resets_run(self):
        r = audit_plateaus(self.rows, REQUIRED)
        c = r['channels']['MQ4_3']
        self.assertEqual((r['non_60_second_gaps'], r['strict_minute_adjacent_pairs']), (1, 3))
        self.assertEqual((c['strict_minute_equal_pairs'], c['noncontiguous_equal_pairs_excluded']), (3, 1))
        self.assertEqual((c['constant_runs_at_least_two'], c['longest_strict_minute_run']), (2, 3))

    def test_cross_midnight_pair_assigned_to_later_day(self):
        r = audit_plateaus(self.rows, REQUIRED)
        self.assertEqual(r['channels']['MQ4_3']['strict_equal_pairs_by_later_row_day'],
                         {'2021-07-21': 1, '2021-07-22': 2})

    def test_reference_variation_descriptive_not_calibration(self):
        r = audit_plateaus(self.rows, REQUIRED)
        c = r['channels']['MQ4_3']
        self.assertEqual(c['constant_runs_with_reference_span_at_least_100ppb'], 2)
        self.assertEqual(c['max_reference_span_ppb_in_constant_run'], 200)
        self.assertFalse(r['source_calibration_traceability_verified'])
        self.assertFalse(r['g2_qualified'])
        self.assertFalse(r['g3_tank_qualified'])

    def test_nonfinite_and_invalid_timestamp_fail_closed(self):
        for field, value, code in (('MQ4_3', 'NaN', 'NONFINITE'),
                                   ('datetime', '2021-07-21 23:58:00', 'DUPLICATE')):
            rows = copy.deepcopy(self.rows)
            rows[1][field] = value
            with self.subTest(field=field), self.assertRaisesRegex(ValueError, code):
                audit_plateaus(rows, REQUIRED)

    def test_no_repeated_values_and_single_row(self):
        r = audit_plateaus(self.rows[:1], REQUIRED)
        self.assertEqual(r['strict_minute_adjacent_pairs'], 0)
        self.assertEqual(r['channels']['MQ4_3']['constant_runs_at_least_two'], 0)
        self.assertEqual(r['channels']['MQ4_3']['longest_strict_minute_run'], 1)

    def test_pinned_raw_replay_is_byte_exact_and_nonpromoting(self):
        p = Path(__file__).with_name('G2_MQ4_GAP_AWARE_PLATEAU_RAW_REPLAY_2026-10-30.json')
        data = p.read_bytes()
        self.assertEqual(hashlib.sha256(data).hexdigest(),
                         '563c308f898cea911d8165de7b8fe96060902df88e76ac7fb2dbcb1021789f82')
        r = json.loads(data)
        self.assertEqual(r['row_count'], 14186)
        self.assertEqual(r['channels']['MQ4_3']['strict_minute_equal_pairs'], 5906)
        self.assertEqual(r['channels']['MQ4_3']['constant_runs_with_reference_span_at_least_100ppb'], 25)
        self.assertFalse(r['g2_qualified'])


if __name__ == '__main__':
    unittest.main()
