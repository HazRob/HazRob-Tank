"""Synthetic/fail-closed regression tests; original external archive is not in CI."""
import csv
import hashlib
import io
import json
import math
import sys
import unittest
import zipfile
from datetime import date, timedelta
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from g2_mq4_multichannel_forward_control import (MEMBER, evaluate, evaluate_zip,
                                                   compact, fit, predict, rotated, solve3)


def fixture(days=9, flat=False):
    out = io.StringIO()
    w = csv.writer(out)
    w.writerow(['datetime', 'licor_ch4', 'MQ4_1', 'MQ4_2', 'MQ4_3'])
    for i in range(days):
        d = date(2021, 7, 1)+timedelta(days=i)
        for j in range(6):
            w.writerow([f'{d.isoformat()} 00:{j:02d}:00', 120+2*i+3*j,
                        5 if flat else 10+i+j, 5 if flat else 20+i-j,
                        5 if flat else 30+i+2*j])
    return out.getvalue().encode()


class MQ4MultichannelForwardTests(unittest.TestCase):
    def test_solver_and_ridge_finite(self):
        self.assertEqual(solve3([[2, 0, 0], [0, 3, 0], [0, 0, 4]], [4, 9, 16]), [2, 3, 4])
        m = fit([(1, 5, 5, 5), (2, 5, 5, 5), (3, 5, 5, 5)], 0.1)
        self.assertEqual(predict(m, (0, 5, 5, 5))[0], 2)

    def test_common_test_days_and_train_past_only(self):
        r = evaluate(fixture())
        self.assertEqual(r['common_test_days'], ['2021-07-06', '2021-07-07',
                                                '2021-07-08', '2021-07-09'])
        self.assertEqual(r['gaps_results']['0']['total_test_rows'], 24)
        for gap, data in r['gaps_results'].items():
            self.assertEqual(set(data['folds']), set(r['common_test_days']))
            for d, f in data['folds'].items():
                self.assertTrue(all((date.fromisoformat(d)-date.fromisoformat(t)).days > int(gap)
                                    for t in f['train_days']))
                self.assertEqual(f['test_rows'], 6)

    def test_reproducible_and_no_promotion(self):
        a, b = evaluate(fixture()), evaluate(fixture())
        self.assertEqual(json.dumps(a, sort_keys=True), json.dumps(b, sort_keys=True))
        self.assertTrue(all(v is False for v in a['scientific_state'].values()))
        self.assertEqual(a['models'], ['ridge3', 'MQ4_1', 'MQ4_2', 'MQ4_3',
                                       'training_mean', 'day_rotation_placebo_ridge3'])

    def test_future_label_does_not_change_earlier_fold(self):
        raw = fixture()
        changed = raw.replace(b'2021-07-09 00:00:00,136,', b'2021-07-09 00:00:00,999,')
        self.assertNotEqual(raw, changed)
        a, b = evaluate(raw), evaluate(changed)
        self.assertEqual(a['gaps_results']['0']['folds']['2021-07-06'],
                         b['gaps_results']['0']['folds']['2021-07-06'])
        self.assertNotEqual(a['gaps_results']['0']['folds']['2021-07-09'],
                            b['gaps_results']['0']['folds']['2021-07-09'])

    def test_rotation_preserves_day_label_distribution(self):
        groups = {date(2021, 7, 1): [(i, i+10, i+20, i+30) for i in range(7)]}
        shifted = rotated(groups)
        self.assertEqual(sorted(r[0] for r in shifted[next(iter(groups))]), list(range(7)))
        self.assertNotEqual(groups, shifted)
        self.assertEqual([r[1:] for r in groups[next(iter(groups))]],
                         [r[1:] for r in shifted[next(iter(groups))]])

    def test_flat_channels_fallback_and_placebo(self):
        r = evaluate(fixture(flat=True))
        for x in r['gaps_results'].values():
            self.assertAlmostEqual(x['pooled_rmse_ppb']['ridge3'],
                                   x['pooled_rmse_ppb']['training_mean'])
            self.assertAlmostEqual(x['pooled_rmse_ppb']['day_rotation_placebo_ridge3'],
                                   x['pooled_rmse_ppb']['training_mean'])

    def test_reject_invalid_alpha_gaps_and_train_min(self):
        for bad in (0, -1, True, float('nan'), float('inf'), 101):
            with self.subTest(alpha=bad), self.assertRaisesRegex(ValueError, 'ALPHA'):
                evaluate(fixture(), alpha=bad)
        for bad in ((0, 0), (-1,), (True,), (31,), ()):
            with self.subTest(gaps=bad), self.assertRaisesRegex(ValueError, 'GAPS'):
                evaluate(fixture(), gaps=bad)
        with self.assertRaisesRegex(ValueError, 'MIN_TRAIN_DAYS'):
            evaluate(fixture(), min_train_days=1)
        with self.assertRaisesRegex(ValueError, 'INSUFFICIENT_FOLDS'):
            evaluate(fixture(days=5))

    def test_reject_tampered_member_and_time_order(self):
        with self.assertRaisesRegex(ValueError, 'MEMBER_SHA256'):
            evaluate(fixture(), expected_member_sha='0'*64)
        with self.assertRaisesRegex(ValueError, 'TIME_ORDER'):
            evaluate(fixture().replace(b'2021-07-02 00:01:00', b'2021-07-02 00:00:00'))
        with self.assertRaisesRegex(ValueError, 'NONFINITE'):
            evaluate(fixture().replace(b'2021-07-02 00:01:00,125,', b'2021-07-02 00:01:00,nan,'))

    def test_zip_hash_duplicate_member_and_invalid_zip(self):
        raw = fixture()
        buf = io.BytesIO()
        with zipfile.ZipFile(buf, 'w') as z:
            z.writestr(MEMBER, raw)
        data = buf.getvalue()
        r = evaluate_zip(data, hashlib.sha256(data).hexdigest(), hashlib.sha256(raw).hexdigest())
        self.assertEqual(len(r['gaps_results']), 3)
        with self.assertRaisesRegex(ValueError, 'ARCHIVE_SHA256'):
            evaluate_zip(data+b'x', hashlib.sha256(data).hexdigest(), hashlib.sha256(raw).hexdigest())
        with self.assertRaisesRegex(ValueError, 'INVALID_ZIP'):
            evaluate_zip(b'not zip', hashlib.sha256(b'not zip').hexdigest(), hashlib.sha256(raw).hexdigest())
        buf = io.BytesIO()
        with zipfile.ZipFile(buf, 'w') as z:
            z.writestr(MEMBER, raw)
            z.writestr(MEMBER, raw)
        duplicate = buf.getvalue()
        with self.assertRaisesRegex(ValueError, 'ZIP_MEMBER'):
            evaluate_zip(duplicate, hashlib.sha256(duplicate).hexdigest(), hashlib.sha256(raw).hexdigest())

    def test_compact_retains_metrics(self):
        r = evaluate(fixture())
        c = compact(r)
        self.assertNotIn('folds', c['gaps_results']['0'])
        self.assertEqual(c['gaps_results']['0']['pooled_rmse_ppb'],
                         r['gaps_results']['0']['pooled_rmse_ppb'])

    def test_pinned_source_summary_and_previous_univariate_replay(self):
        p = Path(__file__).with_name('G2_MQ4_MULTICHANNEL_FORWARD_CONTROL_2026-10-31.json')
        r = json.loads(p.read_text(encoding='utf-8'))
        self.assertEqual(r['raw_member_sha256'],
                         '3c27c4f8b8e8aab54b6433acfa41de00890a9eefd19376259a2371f22a48ff2d')
        self.assertEqual(r['rows'], 14186)
        self.assertEqual(len(r['common_test_days']), 6)
        self.assertEqual(r['gaps_results']['0']['total_test_rows'], 7889)
        self.assertAlmostEqual(r['gaps_results']['0']['pooled_rmse_ppb']['MQ4_1'],
                               1123.8993687497407, places=7)
        self.assertAlmostEqual(r['gaps_results']['2']['pooled_rmse_ppb']['MQ4_2'],
                               1173.435447246299, places=7)
        self.assertLess(r['gaps_results']['0']['pooled_rmse_ppb']['ridge3'],
                        r['gaps_results']['0']['pooled_rmse_ppb']['MQ4_1'])
        self.assertGreater(r['gaps_results']['2']['pooled_rmse_ppb']['ridge3'],
                           r['gaps_results']['2']['pooled_rmse_ppb']['MQ4_1'])
        self.assertFalse(r['scientific_state']['g2_qualified'])


if __name__ == '__main__':
    unittest.main()
