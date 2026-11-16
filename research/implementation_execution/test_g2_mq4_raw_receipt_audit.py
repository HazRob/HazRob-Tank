"""Offline, deterministic tests; original Minnesota MQ4 ZIP is not in CI."""
import copy
import json
import unittest
from pathlib import Path

import g2_mq4_raw_receipt_audit as mod


class PinnedRawReceiptAuditTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.raw = mod.RECEIPT.read_bytes()
        cls.original = mod.strict_loads(cls.raw)

    def changed(self):
        return copy.deepcopy(self.original)

    def rejects(self, obj, code):
        with self.assertRaisesRegex(ValueError, 'G2_MQ4_RAW_RECEIPT_' + code):
            mod.derive(obj)

    def test_pinned_source_receipt_audits(self):
        result = mod.audit_bytes(self.raw)
        self.assertEqual(result['source_scope']['rows'], 14186)
        self.assertEqual(result['source_scope']['calendar_days'], 11)
        self.assertEqual(result['correlation_sign_agreement_days'], 11)
        self.assertEqual(result['all_sensor_positive_correlation_days'],
                         ['2021-07-22', '2021-07-24'])
        self.assertEqual(result['reference_4ppm_range_support']
                         ['guaranteed_no_positive_days'],
                         ['2021-07-26', '2021-07-31'])
        self.assertFalse(result['g2_qualified'])
        self.assertFalse(result['g3_qualified'])

    def test_immutable_git_blob_bytes(self):
        self.assertEqual(mod.git_blob_sha1(self.raw), mod.PINNED_GIT_BLOB_SHA1)
        with self.assertRaisesRegex(ValueError, 'BYTE_PIN'):
            mod.audit_bytes(self.raw + b' ')

    def test_duplicate_json_key_rejected(self):
        with self.assertRaisesRegex(ValueError, 'DUPLICATE_JSON_KEY'):
            mod.strict_loads(b'{"x":1,"x":2}')

    def test_nonfinite_json_rejected(self):
        with self.assertRaisesRegex(ValueError, 'NONFINITE_JSON'):
            mod.strict_loads(b'{"x":NaN}')

    def test_scientific_promotion_rejected(self):
        obj = self.changed()
        obj['g2_qualified'] = True
        self.rejects(obj, 'UNSUPPORTED_CLAIM_G2_QUALIFIED')

    def test_source_digest_mutation_rejected(self):
        obj = self.changed()
        obj['archive_sha256'] = '0' * 64
        self.rejects(obj, 'SOURCE_PINS')

    def test_extra_day_rejected(self):
        obj = self.changed()
        obj['daily_compact']['2021-08-01'] = copy.deepcopy(
            obj['daily_compact']['2021-07-31'])
        self.rejects(obj, 'DAY_SCOPE')

    def test_day_row_count_mutation_rejected(self):
        obj = self.changed()
        obj['daily_compact']['2021-07-21']['n'] += 1
        self.rejects(obj, 'ROW_SUM')

    def test_sensor_cardinality_mutation_rejected(self):
        obj = self.changed()
        obj['daily_compact']['2021-07-21']['mq4']['MQ4_3'][0] = 100000
        self.rejects(obj, 'CARDINALITY')

    def test_sensor_pearson_out_of_range_rejected(self):
        obj = self.changed()
        obj['daily_compact']['2021-07-21']['mq4']['MQ4_1'][1] = 1.1
        self.rejects(obj, 'PEARSON')

    def test_corrupted_summary_sign_rejected(self):
        obj = self.changed()
        obj['summary']['MQ4_2']['days_positive_pearson'].append('2021-07-21')
        self.rejects(obj, 'SIGN_LINEAGE')

    def test_corrupted_summary_fraction_rejected(self):
        obj = self.changed()
        obj['summary']['MQ4_3']['max_day_distinct_fraction'] = 0.8
        self.rejects(obj, 'SUMMARY_LINEAGE_MAX_DAY_DISTINCT_FRACTION')

    def test_invented_reference_threshold_rejected(self):
        obj = self.changed()
        obj['daily_compact']['2021-07-31']['ref_max_ppm'] = -2
        self.rejects(obj, 'REFERENCE_RANGE')

    def test_boolean_cardinality_rejected(self):
        obj = self.changed()
        obj['daily_compact']['2021-07-21']['mq4']['MQ4_1'][0] = True
        self.rejects(obj, 'CARDINALITY')

    def test_committed_derived_receipt_matches(self):
        expected = mod.audit_bytes(self.raw)
        p = Path(__file__).with_name('G2_MQ4_RAW_RECEIPT_AUDIT_2026-11-02.json')
        saved = mod.strict_loads(p.read_bytes())
        self.assertEqual(saved, expected)
        self.assertIs(saved['independent_tank_exposures'], False)
        self.assertIs(saved['traceable_calibration'], False)


if __name__ == '__main__':
    unittest.main()
