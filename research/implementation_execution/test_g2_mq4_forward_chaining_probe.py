"""Synthetic fail-closed regressions for chronological MQ4 research stress probe."""
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
from g2_mq4_forward_chaining_probe import evaluate, evaluate_zip, MEMBER


def fixture(n_days=9, flat=False):
    buffer = io.StringIO()
    writer = csv.writer(buffer)
    writer.writerow(['datetime', 'licor_ch4', 'MQ4_1', 'MQ4_2', 'MQ4_3'])
    for i in range(n_days):
        day = date(2021, 7, 1) + timedelta(days=i)
        for minute in range(5):
            writer.writerow([f'{day.isoformat()} 00:{minute:02d}:00',
                             100 + i*4 + minute, 5 if flat else 10+i+minute,
                             20+i-minute, 30+i+minute*2])
    return buffer.getvalue().encode('utf-8')


class ForwardChainTests(unittest.TestCase):
    def test_common_holdout_days_and_no_future_train(self):
        r = evaluate(fixture())
        self.assertEqual(r['common_test_days'], ['2021-07-06', '2021-07-07',
                                                '2021-07-08', '2021-07-09'])
        for channel in r['channels'].values():
            self.assertEqual(set(channel['0']['folds']), set(channel['2']['folds']))
            for width, result in channel.items():
                for day, fold in result['folds'].items():
                    self.assertTrue(all(d < day for d in fold['train_days']))
                    self.assertTrue(all((date.fromisoformat(day)-date.fromisoformat(d)).days > int(width)
                                        for d in fold['train_days']))
                    self.assertEqual(fold['test_rows'], 5)

    def test_past_only_first_fold(self):
        r = evaluate(fixture())
        fold = r['channels']['MQ4_1']['2']['folds']['2021-07-06']
        self.assertEqual(fold['train_days'], ['2021-07-01', '2021-07-02', '2021-07-03'])
        self.assertEqual(fold['train_rows'], 15)
        self.assertEqual(r['channels']['MQ4_1']['2']['total_test_rows'], 20)

    def test_determinism_and_fail_closed(self):
        a, b = evaluate(fixture()), evaluate(fixture())
        self.assertEqual(json.dumps(a, sort_keys=True), json.dumps(b, sort_keys=True))
        self.assertFalse(a['scientific_state']['g2_qualified'])
        self.assertFalse(a['scientific_state']['reference_calibration_verified'])

    def test_future_label_does_not_change_earlier_fold(self):
        raw = fixture()
        changed = raw.replace(b'2021-07-09 00:00:00,132,',
                              b'2021-07-09 00:00:00,999,')
        self.assertNotEqual(raw, changed)
        a, b = evaluate(raw), evaluate(changed)
        self.assertEqual(a['channels']['MQ4_1']['0']['folds']['2021-07-06'],
                         b['channels']['MQ4_1']['0']['folds']['2021-07-06'])
        self.assertNotEqual(a['channels']['MQ4_1']['0']['folds']['2021-07-09'],
                            b['channels']['MQ4_1']['0']['folds']['2021-07-09'])

    def test_zero_variance_predictor(self):
        r = evaluate(fixture(flat=True))
        self.assertEqual(r['channels']['MQ4_1']['0']['folds']['2021-07-06']
                         ['slope_ppb_per_raw_channel_unit'], 0.0)

    def test_invalid_gaps_and_minimum_days(self):
        for bad in ((-1,), (True,), (0, 0), (31,)):
            with self.subTest(gaps=bad), self.assertRaisesRegex(ValueError, 'GAPS'):
                evaluate(fixture(), gaps=bad)
        with self.assertRaisesRegex(ValueError, 'MIN_TRAIN_DAYS'):
            evaluate(fixture(), min_train_days=1)
        with self.assertRaisesRegex(ValueError, 'INSUFFICIENT_FORWARD_FOLDS'):
            evaluate(fixture(n_days=5))

    def test_reject_bad_source_bytes(self):
        with self.assertRaisesRegex(ValueError, 'MEMBER_SHA256'):
            evaluate(fixture(), expected_member_sha='0'*64)
        with self.assertRaisesRegex(ValueError, 'TIME_ORDER'):
            evaluate(fixture().replace(b'2021-07-02 00:01:00', b'2021-07-02 00:00:00'))

    def test_zip_hash_and_member_validation(self):
        raw = fixture()
        buf = io.BytesIO()
        with zipfile.ZipFile(buf, 'w') as z:
            z.writestr(MEMBER, raw)
        data = buf.getvalue()
        r = evaluate_zip(data, hashlib.sha256(data).hexdigest(), hashlib.sha256(raw).hexdigest())
        self.assertEqual(len(r['channels']), 3)
        with self.assertRaisesRegex(ValueError, 'ARCHIVE_SHA256'):
            evaluate_zip(data+b'x', hashlib.sha256(data).hexdigest(), hashlib.sha256(raw).hexdigest())

    def test_pinned_source_summary_remains_unqualified(self):
        p = Path(__file__).resolve().parent / 'G2_MQ4_FORWARD_CHAIN_SUMMARY_2026-10-31.json'
        r = json.loads(p.read_text(encoding='utf-8'))
        self.assertEqual(r['raw_member_sha256'],
                         '3c27c4f8b8e8aab54b6433acfa41de00890a9eefd19376259a2371f22a48ff2d')
        self.assertEqual(r['rows'], 14186)
        self.assertEqual(len(r['days']), 11)
        self.assertEqual(len(r['common_test_days']), 6)
        self.assertEqual(sorted(r['channels']['MQ4_1']), ['0', '1', '2'])
        self.assertFalse(r['scientific_state']['g2_qualified'])


if __name__ == '__main__':
    unittest.main()
