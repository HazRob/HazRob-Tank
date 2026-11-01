"""Synthetic-only regressions; original source ZIP is not bundled in CI."""
import csv
import hashlib
import io
import json
import unittest
from datetime import datetime, timedelta
from unittest.mock import patch

import g2_mq4_raw_day_diagnostics as mod


class RawDayDiagnosticTests(unittest.TestCase):
    def synthetic(self):
        s = io.StringIO(newline='')
        w = csv.writer(s)
        w.writerow(mod.COLUMNS)
        base = datetime(2021, 7, 21)
        for d in range(11):
            n = 1296 if d == 10 else 1289
            for i in range(n):
                # Not original observations: exact deterministic fixture.
                t = base + timedelta(days=d, minutes=i)
                y = 2000 + 100 * (i % 7)
                row = [t.strftime('%Y-%m-%d %H:%M:%S'), y]
                row += [1.0] * (len(mod.COLUMNS) - 5)
                row += [y*2, y*3, y*4]
                w.writerow(row)
        return s.getvalue().encode('utf-8')

    def test_raw_pins_are_expected(self):
        self.assertEqual(len(mod.ARCHIVE_SHA256), 64)
        self.assertEqual(len(mod.MEMBER_SHA256), 64)
        self.assertEqual(mod.MEMBER, 'raw/MQ-ACQ-2026-06_umn_10ppm_data_csv.csv')

    def test_simple_pearson_signs(self):
        self.assertAlmostEqual(mod.pearson([1, 2, 3], [3, 2, 1]), -1)
        self.assertAlmostEqual(mod.pearson([1, 2, 3], [2, 4, 6]), 1)

    def test_constant_pearson_rejected(self):
        with self.assertRaisesRegex(ValueError, 'CONSTANT_SIGNAL'):
            mod.pearson([1, 1, 1], [2, 3, 4])

    def test_synthetic_full_scope_reproducible(self):
        raw = self.synthetic()
        with patch.object(mod, 'MEMBER_SHA256', hashlib.sha256(raw).hexdigest()):
            a, b = mod.analyze_csv(raw), mod.analyze_csv(raw)
        self.assertEqual(a, b)
        self.assertEqual(a['rows'], 14186)
        self.assertEqual(a['calendar_days'], 11)
        self.assertEqual(len(a['summary']['MQ4_3']['days_positive_pearson']), 11)
        self.assertFalse(a['g2_qualified'])
        self.assertFalse(a['g3_qualified'])

    def test_changed_source_bytes_rejected(self):
        with self.assertRaisesRegex(ValueError, 'RAW_MEMBER_SHA256'):
            mod.analyze_csv(b'not the pinned source')

    def test_wrong_archive_digest_rejected(self):
        with self.assertRaisesRegex(ValueError, 'ARCHIVE_SHA256'):
            mod.analyze_archive(b'fake archive')

    def test_header_ambiguity_rejected(self):
        raw = b'datetime,licor_ch4,MQ4_1,MQ4_2,MQ4_3\n'
        with patch.object(mod, 'MEMBER_SHA256', hashlib.sha256(raw).hexdigest()):
            with self.assertRaisesRegex(ValueError, 'HEADER'):
                mod.analyze_csv(raw)

    def test_nonfinite_rejected(self):
        s = io.StringIO(newline='')
        w = csv.writer(s); w.writerow(mod.COLUMNS)
        w.writerow(['2021-07-21 00:00:00', 'NaN'] + ['1'] * (len(mod.COLUMNS)-2))
        raw = s.getvalue().encode()
        with patch.object(mod, 'MEMBER_SHA256', hashlib.sha256(raw).hexdigest()):
            with self.assertRaisesRegex(ValueError, 'FINITE'):
                mod.analyze_csv(raw)

    def test_duplicate_timestamp_rejected(self):
        s = io.StringIO(newline='')
        w = csv.writer(s); w.writerow(mod.COLUMNS)
        row = ['2021-07-21 00:00:00'] + ['1']*(len(mod.COLUMNS)-1)
        w.writerow(row); w.writerow(row)
        raw = s.getvalue().encode()
        with patch.object(mod, 'MEMBER_SHA256', hashlib.sha256(raw).hexdigest()):
            with self.assertRaisesRegex(ValueError, 'TIME_ORDER'):
                mod.analyze_csv(raw)

    def test_committed_receipt_has_no_scientific_promotion(self):
        from pathlib import Path
        p = Path(__file__).with_name('G2_MQ4_RAW_DAY_DIAGNOSTICS_2026-11-02.json')
        obj = json.loads(p.read_text(encoding='utf-8'))
        self.assertEqual(obj['rows'], 14186)
        self.assertEqual(obj['calendar_days'], 11)
        self.assertEqual(obj['archive_sha256'], mod.ARCHIVE_SHA256)
        self.assertEqual(obj['member_sha256'], mod.MEMBER_SHA256)
        for flag in ('g2_qualified','g3_qualified','independent_tank_exposures',
                     'traceable_calibration','blinded_prospective'):
            self.assertIs(obj[flag], False)
        for sensor in mod.SENSORS:
            self.assertEqual(obj['summary'][sensor]['days_positive_pearson'],
                             ['2021-07-22', '2021-07-24'])
            self.assertEqual(len(obj['summary'][sensor]['days_negative_pearson']), 9)
        self.assertLess(obj['summary']['MQ4_3']['max_day_distinct_fraction'], 0.07)
        self.assertGreater(obj['summary']['MQ4_1']['min_day_distinct_fraction'], 0.85)


if __name__ == '__main__':
    unittest.main()
