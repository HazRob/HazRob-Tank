"""Deterministic synthetic tests; real source archive is external to CI."""
import csv
import hashlib
import io
import json
import sys
import unittest
import zipfile
from datetime import date, timedelta
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from g2_mq4_neighbor_day_embargo_probe import evaluate, evaluate_zip, MEMBER


def fixture(n_days=7, n_per_day=5):
    buf = io.StringIO()
    writer = csv.writer(buf)
    writer.writerow(['datetime', 'licor_ch4', 'MQ4_1', 'MQ4_2', 'MQ4_3'])
    start = date(2021, 7, 1)
    for day in range(n_days):
        d = start + timedelta(days=day)
        for minute in range(n_per_day):
            writer.writerow([f'{d.isoformat()} 00:{minute:02d}:00', 100 + 4*day + minute,
                             20 + 2*day + minute, 30 + day - minute, 40 + day + 2*minute])
    return buf.getvalue().encode('utf-8')


class NeighborEmbargoTests(unittest.TestCase):
    def test_excludes_holdout_and_calendar_neighbors(self):
        r = evaluate(fixture(), (0, 1, 2))
        fold = r['channels']['MQ4_1']['1']['folds']['2021-07-04']
        self.assertEqual(fold['excluded_days'], ['2021-07-03', '2021-07-04', '2021-07-05'])
        self.assertEqual(fold['train_rows'], 20)
        self.assertEqual(fold['test_rows'], 5)
        self.assertEqual(len(r['channels']['MQ4_1']['2']['folds']), 7)
        self.assertFalse(r['scientific_state']['g2_qualified'])

    def test_edge_day_and_calendar_gap(self):
        r = evaluate(fixture(), (1,))
        self.assertEqual(r['channels']['MQ4_2']['1']['folds']['2021-07-01']['excluded_days'],
                         ['2021-07-01', '2021-07-02'])
        self.assertEqual(r['channels']['MQ4_2']['1']['folds']['2021-07-01']['train_rows'], 25)

    def test_deterministic_and_finite(self):
        a, b = evaluate(fixture()), evaluate(fixture())
        self.assertEqual(json.dumps(a, sort_keys=True), json.dumps(b, sort_keys=True))
        self.assertGreater(a['channels']['MQ4_1']['0']['pooled_rmse_baseline_ppb'], 0)

    def test_reject_invalid_width_and_insufficient_train(self):
        for bad in ((-1,), (0, 0), (True,), (0, 31)):
            with self.subTest(bad=bad), self.assertRaisesRegex(ValueError, 'EMBARGO_WIDTHS'):
                evaluate(fixture(), bad)
        with self.assertRaisesRegex(ValueError, 'INSUFFICIENT_TRAIN_DAYS'):
            evaluate(fixture(n_days=5), (2,))

    def test_reject_duplicate_timestamp(self):
        raw = fixture().replace(b'2021-07-02 00:01:00', b'2021-07-02 00:00:00')
        with self.assertRaisesRegex(ValueError, 'TIME_ORDER'):
            evaluate(raw)

    def test_reject_nan_and_member_hash_mismatch(self):
        with self.assertRaisesRegex(ValueError, 'FINITE'):
            evaluate(fixture().replace(b',100,', b',nan,'))
        with self.assertRaisesRegex(ValueError, 'MEMBER_SHA256'):
            evaluate(fixture(), expected_member_sha='0'*64)

    def test_pinned_zip_and_tamper(self):
        raw = fixture()
        out = io.BytesIO()
        with zipfile.ZipFile(out, 'w') as z:
            z.writestr(MEMBER, raw)
        blob = out.getvalue()
        r = evaluate_zip(blob, hashlib.sha256(blob).hexdigest(), hashlib.sha256(raw).hexdigest())
        self.assertEqual(len(r['channels']), 3)
        with self.assertRaisesRegex(ValueError, 'ARCHIVE_SHA256'):
            evaluate_zip(blob+b'x', hashlib.sha256(blob).hexdigest(), hashlib.sha256(raw).hexdigest())

    def test_pinned_real_summary_is_fail_closed(self):
        path = Path(__file__).resolve().parent / 'G2_MQ4_NEIGHBOR_DAY_EMBARGO_SUMMARY_2026-10-31.json'
        pinned = json.loads(path.read_text(encoding='utf-8'))
        self.assertEqual(pinned['raw_member_sha256'],
                         '3c27c4f8b8e8aab54b6433acfa41de00890a9eefd19376259a2371f22a48ff2d')
        self.assertEqual(pinned['rows'], 14186)
        self.assertEqual(len(pinned['days']), 11)
        self.assertFalse(pinned['scientific_state']['g2_qualified'])
        self.assertFalse(pinned['scientific_state']['reference_calibration_verified'])
        self.assertEqual(sorted(pinned['channels']['MQ4_1']), ['0', '1', '2'])


if __name__ == '__main__':
    unittest.main()
