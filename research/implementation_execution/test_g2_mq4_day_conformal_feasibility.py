"""Synthetic fail-closed tests; original external source ZIP is absent from CI."""
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
from g2_mq4_day_conformal_feasibility import (
    MEMBER, analyze, analyze_zip, read_days, required_groups,
)


def fixture(days=11):
    stream = io.StringIO()
    writer = csv.writer(stream)
    writer.writerow(['datetime', 'licor_ch4', 'MQ4_1'])
    for i in range(days):
        d = date(2021, 7, 21) + timedelta(days=i)
        for minute in range(3):
            writer.writerow([f'{d.isoformat()} 00:0{minute}:00', i+minute, 10+i])
    return stream.getvalue().encode()


class MQ4DayConformalFeasibilityTests(unittest.TestCase):
    def test_required_group_rank_boundary(self):
        self.assertEqual(required_groups(0.9), 9)
        self.assertEqual(required_groups(0.95), 19)
        self.assertEqual(required_groups(0.8), 4)
        self.assertEqual(required_groups(0.5), 1)

    def test_six_forward_days_three_embargoes(self):
        r = analyze(read_days(fixture()))
        self.assertEqual(r['source_rows'], 33)
        self.assertEqual(r['test_days'], [f'2021-07-{i:02d}' for i in range(26, 32)])
        self.assertEqual([r['gap_results'][str(g)]['folds']['2021-07-26']['maximum_calendar_calibration_days']
                          for g in (0, 1, 2)], [2, 1, 0])
        self.assertEqual([r['gap_results'][str(g)]['folds']['2021-07-31']['maximum_calendar_calibration_days']
                          for g in (0, 1, 2)], [7, 6, 5])
        self.assertTrue(all(not v for g in r['gap_results'].values()
                            for v in g['all_test_days_finite_nominal_feasible'].values()))
        self.assertEqual([r['gap_results'][str(g)]['locked_holdout_block_calibration_capacity']
                          ['maximum_calendar_calibration_days'] for g in (0, 1, 2)], [2, 1, 0])

    def test_maximum_rank_fraction_is_not_coverage_claim(self):
        r = analyze(read_days(fixture()))
        self.assertEqual(r['gap_results']['0']['folds']['2021-07-31']
                         ['largest_finite_rank_fraction_if_exchangeable'], 7/8)
        self.assertTrue(all(v is False for v in r['scientific_state'].values()))
        self.assertIn('not actual', r['limitations'])

    def test_past_only_and_calendar_gap(self):
        r = analyze(read_days(fixture()))
        for gap in (0, 1, 2):
            for day, fold in r['gap_results'][str(gap)]['folds'].items():
                earlier = [d for d in r['source_days']
                           if date.fromisoformat(d) < date.fromisoformat(day)-timedelta(days=gap)]
                self.assertEqual(fold['eligible_past_days'], len(earlier))

    def test_reproducibility(self):
        counts = read_days(fixture())
        self.assertEqual(json.dumps(analyze(counts), sort_keys=True),
                         json.dumps(analyze(counts), sort_keys=True))

    def test_reject_invalid_nominal_or_groups(self):
        for p in (0, 1, -1, True, float('nan'), float('inf')):
            with self.subTest(p=p), self.assertRaisesRegex(ValueError, 'NOMINAL'):
                required_groups(p)
        with self.assertRaisesRegex(ValueError, 'GAPS'):
            analyze(read_days(fixture()), gaps=(0, 0))
        with self.assertRaisesRegex(ValueError, 'HOLDOUT_DAYS'):
            analyze(read_days(fixture()), holdout_days=True)
        with self.assertRaisesRegex(ValueError, 'INSUFFICIENT_PAST_DAYS'):
            analyze(read_days(fixture()), min_train_days=6)

    def test_reject_bad_csv_shape_order_and_hash(self):
        with self.assertRaisesRegex(ValueError, 'MEMBER_SHA256'):
            read_days(fixture(), '0'*64)
        with self.assertRaisesRegex(ValueError, 'TIME_ORDER'):
            read_days(fixture().replace(b'2021-07-22 00:01:00', b'2021-07-22 00:00:00'))
        with self.assertRaisesRegex(ValueError, 'ROW_SHAPE'):
            read_days(fixture()+b'2021-08-01 00:00:00\n')

    def test_zip_member_integrity(self):
        raw = fixture()
        buf = io.BytesIO()
        with zipfile.ZipFile(buf, 'w') as z:
            z.writestr(MEMBER, raw)
        data = buf.getvalue()
        r = analyze_zip(data, hashlib.sha256(data).hexdigest(),
                        hashlib.sha256(raw).hexdigest())
        self.assertEqual(r['source_rows'], 33)
        with self.assertRaisesRegex(ValueError, 'ARCHIVE_SHA256'):
            analyze_zip(data+b'x', hashlib.sha256(data).hexdigest(), hashlib.sha256(raw).hexdigest())
        with self.assertRaisesRegex(ValueError, 'INVALID_ZIP'):
            analyze_zip(b'bad', hashlib.sha256(b'bad').hexdigest(), hashlib.sha256(raw).hexdigest())
        buf = io.BytesIO()
        with zipfile.ZipFile(buf, 'w') as z:
            z.writestr(MEMBER, raw)
            z.writestr(MEMBER, raw)
        with self.assertRaisesRegex(ValueError, 'ZIP_MEMBER'):
            duplicate = buf.getvalue()
            analyze_zip(duplicate, hashlib.sha256(duplicate).hexdigest(), hashlib.sha256(raw).hexdigest())

    def test_pinned_original_source_summary(self):
        p = Path(__file__).with_name('G2_MQ4_DAY_CONFORMAL_FEASIBILITY_2026-10-31.json')
        r = json.loads(p.read_text(encoding='utf-8'))
        self.assertEqual(r['raw_member_sha256'],
                         '3c27c4f8b8e8aab54b6433acfa41de00890a9eefd19376259a2371f22a48ff2d')
        self.assertEqual(r['source_rows'], 14186)
        self.assertEqual(len(r['source_days']), 11)
        self.assertEqual(r['minimum_exchangeable_calibration_groups_for_finite_threshold'],
                         {'0.9': 9, '0.95': 19})
        self.assertFalse(r['gap_results']['0']['all_test_days_finite_nominal_feasible']['0.9'])
        self.assertFalse(r['scientific_state']['g2_qualified'])


if __name__ == '__main__':
    unittest.main()
