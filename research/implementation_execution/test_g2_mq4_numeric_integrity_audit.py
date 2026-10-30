"""Synthetic numeric-integrity regressions; NOT physical gas validation."""
import copy
import unittest
from g2_mq4_numeric_integrity_audit import REQUIRED, audit_rows


class MQ4NumericIntegrityTests(unittest.TestCase):
    def setUp(self):
        self.rows = [
            {'datetime': '2021-07-21 23:59:00', 'licor_ch4': '2000',
             'MQ4_1': '10', 'MQ4_2': '20', 'MQ4_3': '30', 't': '21', 'rh': '50'},
            {'datetime': '2021-07-22 00:00:00', 'licor_ch4': '4000',
             'MQ4_1': '11', 'MQ4_2': '21', 'MQ4_3': '31', 't': '-2', 'rh': '100'},
        ]

    def run_rows(self, rows=None, headers=None):
        return audit_rows(self.rows if rows is None else rows, REQUIRED if headers is None else headers)

    def test_valid_ppb_ohm_and_negative_temperature(self):
        r = self.run_rows()
        self.assertEqual(r['row_count'], 2)
        self.assertEqual((r['reference_methane_ppm_min'], r['reference_methane_ppm_max']), (2.0, 4.0))
        self.assertEqual(r['numeric_fields']['MQ4_1']['distinct_values'], 2)
        self.assertEqual(r['numeric_fields']['MQ4_1']['consecutive_equal_pairs'], 0)
        self.assertEqual(r['numeric_fields']['MQ4_1']['longest_identical_run'], 1)
        self.assertTrue(r['source_numeric_integrity_verified'])
        self.assertFalse(r['source_calibration_traceability_verified'])
        self.assertFalse(r['g2_qualified'])

    def test_quantized_plateau_is_reported_without_false_calibration_claim(self):
        rows = copy.deepcopy(self.rows)
        rows.extend(copy.deepcopy(self.rows[-1]) for _ in range(2))
        rows[2]['datetime'] = '2021-07-22 00:01:00'
        rows[3]['datetime'] = '2021-07-22 00:02:00'
        r = self.run_rows(rows)
        self.assertEqual(r['numeric_fields']['MQ4_3']['consecutive_equal_pairs'], 2)
        self.assertEqual(r['numeric_fields']['MQ4_3']['longest_identical_run'], 3)
        self.assertFalse(r['independent_exposure_groups_verified'])

    def test_empty_source_and_missing_header_fail_closed(self):
        with self.assertRaisesRegex(ValueError, 'EMPTY_SOURCE'):
            self.run_rows([])
        with self.assertRaisesRegex(ValueError, 'HEADER_INVALID'):
            self.run_rows(headers=('datetime', 'licor_ch4'))
        with self.assertRaisesRegex(ValueError, 'HEADER_INVALID'):
            self.run_rows(headers=REQUIRED + ('MQ4_1',))

    def test_missing_nonnumeric_and_nonfinite_fail_closed(self):
        for value, code in (('', 'MISSING'), ('oops', 'NONNUMBER'), ('NaN', 'NONFINITE'), ('inf', 'NONFINITE')):
            rows = copy.deepcopy(self.rows)
            rows[1]['MQ4_2'] = value
            with self.subTest(value=value), self.assertRaisesRegex(ValueError, code):
                self.run_rows(rows)

    def test_nonpositive_mq4_resistance_fail_closed(self):
        for value in ('0', '-0.1'):
            rows = copy.deepcopy(self.rows)
            rows[0]['MQ4_3'] = value
            with self.subTest(value=value), self.assertRaisesRegex(ValueError, 'RESISTANCE_NONPOSITIVE'):
                self.run_rows(rows)

    def test_negative_reference_and_invalid_rh_fail_closed(self):
        for field, value, code in (('licor_ch4', '-1', 'METHANE_NEGATIVE'),
                                   ('rh', '-0.01', 'RH_OUT_OF_RANGE'),
                                   ('rh', '100.01', 'RH_OUT_OF_RANGE')):
            rows = copy.deepcopy(self.rows)
            rows[0][field] = value
            with self.subTest(field=field, value=value), self.assertRaisesRegex(ValueError, code):
                self.run_rows(rows)

    def test_duplicate_and_out_of_order_timestamps_fail_closed(self):
        for timestamp in ('2021-07-21 23:59:00', '2021-07-21 23:58:00'):
            rows = copy.deepcopy(self.rows)
            rows[1]['datetime'] = timestamp
            with self.subTest(timestamp=timestamp), self.assertRaisesRegex(ValueError, 'DUPLICATE'):
                self.run_rows(rows)

    def test_bad_row_shape_and_timestamp_fail_closed(self):
        rows = copy.deepcopy(self.rows)
        rows[0]['MQ4_1'] = None
        with self.assertRaisesRegex(ValueError, 'ROW_SHAPE_INVALID'):
            self.run_rows(rows)
        rows = copy.deepcopy(self.rows)
        rows[0]['datetime'] = '2021-7-21 23:59:00'
        with self.assertRaisesRegex(ValueError, 'TIMESTAMP_NONCANONICAL'):
            self.run_rows(rows)


if __name__ == '__main__':
    unittest.main()
