"""Synthetic negative controls; original raw archive is verified locally, not in CI."""
import io
import json
import unittest
import zipfile
from g2_mq4_benchmark_claim_audit import audit, MEMBER


def fixture(ch4=(2000, 4500, 3500, 6000), days=(1, 1, 2, 2)):
    header = 'datetime,licor_ch4,MQ4_1,MQ4_2,MQ4_3\n'
    rows = ''.join(f'2021-07-{d:02d} 11:{i:02d}:00,{v},100,200,300\n'
                   for i, (v, d) in enumerate(zip(ch4, days)))
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, 'w') as z:
        z.writestr(MEMBER, header + rows)
    model = {'model': 'HGB_MQ4_3_T_RH', 'original_source_test_predictions': 4,
             'test_days': 2, 'pooled_mae_ppm': 0.5, 'pooled_interval_coverage': 0.75,
             'total_4ppm_true_positive': 1, 'total_4ppm_false_negative': 1,
             'total_4ppm_false_positive': 1, 'total_4ppm_true_negative': 1}
    summary = {'source_rows': 4, 'days': 2, 'sensor_instances': 3,
               'distinct_gases': 1,
               'role': 'EXTERNAL_SOURCE_RESEARCH_PIPELINE_ONLY_NOT_JAVA_ANDROID_RUNTIME',
               'evidence_policy': {'tank_g3_validated': 0},
               'four_ppm_diagnostic': {'not_safety_threshold': True,
                   'source_reference_positive': 2,
                   'hgb_true_positive': 1, 'hgb_false_negative': 1,
                   'hgb_false_positive': 1, 'hgb_true_negative': 1,
                   'hgb_sensitivity': 0.5, 'hgb_false_positive_rate': 0.5},
               'models': [model]}
    return buf.getvalue(), summary


def run(archive, summary):
    return audit(archive, json.dumps(summary).encode('utf-8'), enforce_pins=False)


class TestExternalMQ4ClaimReconciliation(unittest.TestCase):
    def test_positive_reconciliation_no_promotion(self):
        a, s = fixture()
        out = run(a, s)
        self.assertEqual((out['source_rows'], out['source_reference_positive_4ppm']), (4, 2))
        self.assertEqual(out['model_claim_arithmetic'][0]['sensitivity_4ppm'], 0.5)
        self.assertTrue(out['reference_labels_independently_recounted_from_raw'])
        self.assertFalse(out['model_predictions_independently_replayed'])
        self.assertFalse(out['tank_g2_qualified'])
        self.assertFalse(out['tank_g3_qualified'])

    def test_archive_and_summary_sha_required_in_real_mode(self):
        a, s = fixture()
        with self.assertRaisesRegex(ValueError, 'PINNED_INPUT_MISMATCH'):
            audit(a, json.dumps(s).encode())

    def test_mislabeled_ppb_ppm_source_positive_rejected(self):
        a, s = fixture()
        s['four_ppm_diagnostic']['source_reference_positive'] = 4
        with self.assertRaisesRegex(ValueError, 'REFERENCE_POSITIVE_MISMATCH'):
            run(a, s)

    def test_confusion_matrix_false_negative_tamper_rejected(self):
        a, s = fixture()
        s['models'][0]['total_4ppm_false_negative'] = 0
        with self.assertRaisesRegex(ValueError, 'CONFUSION_REFERENCE_MISMATCH'):
            run(a, s)

    def test_confusion_matrix_false_positive_tamper_rejected(self):
        a, s = fixture()
        s['models'][0]['total_4ppm_false_positive'] = 0
        with self.assertRaisesRegex(ValueError, 'CONFUSION_REFERENCE_MISMATCH'):
            run(a, s)

    def test_reported_hgb_rate_tamper_rejected(self):
        a, s = fixture()
        s['four_ppm_diagnostic']['hgb_sensitivity'] = 0.9
        with self.assertRaisesRegex(ValueError, 'HGB_RATE_MISMATCH'):
            run(a, s)

    def test_reported_coverage_out_of_range_rejected(self):
        a, s = fixture()
        s['models'][0]['pooled_interval_coverage'] = 1.1
        with self.assertRaisesRegex(ValueError, 'NUMBER_RANGE'):
            run(a, s)

    def test_missing_source_days_rejected(self):
        a, s = fixture()
        s['days'] = 3
        with self.assertRaisesRegex(ValueError, 'SOURCE_SCOPE_MISMATCH'):
            run(a, s)

    def test_nonmonotonic_source_time_rejected(self):
        a, s = fixture(days=(1, 2, 1, 2))
        with self.assertRaisesRegex(ValueError, 'TIMESTAMP_NONMONOTONIC'):
            run(a, s)

    def test_duplicate_summary_key_rejected(self):
        a, s = fixture()
        raw = json.dumps(s).replace('"source_rows": 4', '"source_rows": 4, "source_rows": 4')
        with self.assertRaisesRegex(ValueError, 'SUMMARY_DUPLICATE_KEY'):
            audit(a, raw.encode(), enforce_pins=False)

    def test_duplicate_model_rejected(self):
        a, s = fixture()
        s['models'].append(dict(s['models'][0]))
        with self.assertRaisesRegex(ValueError, 'MODEL_NAME_INVALID'):
            run(a, s)

    def test_missing_scientific_boundary_rejected(self):
        a, s = fixture()
        s['four_ppm_diagnostic']['not_safety_threshold'] = False
        with self.assertRaisesRegex(ValueError, 'SCIENTIFIC_BOUNDARY_INVALID'):
            run(a, s)


if __name__ == '__main__':
    unittest.main()
