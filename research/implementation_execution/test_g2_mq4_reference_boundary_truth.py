"""Offline regressions: external reference labels, not Tank scientific evidence."""
import copy
import csv
import io
import unittest
from decimal import Decimal
from g2_mq4_reference_boundary_truth import (
    RECEIPT, FORWARD, COLUMNS, analyze_csv, strict_json,
    validate_payload, audit, RECEIPT_SHA,
)


class MQ4ReferenceTruthTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.r = strict_json(RECEIPT.read_bytes())
        cls.f = strict_json(FORWARD.read_bytes())

    def test_actual_pinned_receipt(self):
        result = audit()
        self.assertEqual((result['source_rows'],result['source_positives'],
                          result['test_rows'],result['test_positives']),
                         (14186,2346,9325,1425))
        self.assertFalse(result['zip_replayed_in_ci'])
        self.assertFalse(result['g2_qualified'])

    def test_pin_format(self):
        self.assertEqual(len(bytes.fromhex(RECEIPT_SHA)), 32)

    def test_per_day_truth_reconciled(self):
        for day, positives in self.f['positive_by_test_day'].items():
            self.assertEqual(self.r['day_rows_positive_boundary_pairs'][day][1], positives)

    def test_boundary_counts_monotonic(self):
        for rows, pos, bands in self.r['day_rows_positive_boundary_pairs'].values():
            self.assertEqual([b[0] for b in bands], sorted(b[0] for b in bands))
            self.assertEqual([b[1] for b in bands], sorted(b[1] for b in bands))

    def test_zero_positive_days(self):
        for day in ('2021-07-26','2021-07-31'):
            rows,pos,bands = self.r['day_rows_positive_boundary_pairs'][day]
            self.assertEqual(pos, 0)
            self.assertEqual(bands, [[0,0],[0,0],[0,0]])

    def mutate(self, edit, pattern):
        r,f = copy.deepcopy(self.r),copy.deepcopy(self.f)
        edit(r,f)
        with self.assertRaisesRegex(ValueError, pattern):
            validate_payload(r,f)

    def test_wrong_day_truth(self):
        self.mutate(lambda r,f:f['positive_by_test_day'].__setitem__('2021-07-29',460),'FORWARD_TRUTH')

    def test_false_g2_promotion(self):
        self.mutate(lambda r,f:r.__setitem__('g2_qualified',True),'NO_PROMOTION')

    def test_boolean_count(self):
        self.mutate(lambda r,f:r['day_rows_positive_boundary_pairs']['2021-07-25'].__setitem__(1,True),'DAY_SHAPE')

    def test_bad_boundary_count(self):
        self.mutate(lambda r,f:r['day_rows_positive_boundary_pairs']['2021-07-25'][2][1].__setitem__(1,999),'BAND_COUNTS')

    def test_reversed_band(self):
        self.mutate(lambda r,f:r['day_rows_positive_boundary_pairs']['2021-07-25'][2][2].__setitem__(0,0),'BAND_COUNTS')

    def test_duplicate_json_key(self):
        with self.assertRaisesRegex(ValueError,'DUPLICATE_KEY'):
            strict_json(b'{"a":1,"a":2}')

    def test_nonfinite_json(self):
        for b in (b'{"a":NaN}',b'{"a":1e999}'):
            with self.assertRaisesRegex(ValueError,'NONFINITE'):
                strict_json(b)

    @staticmethod
    def fixture(samples):
        out = io.StringIO(newline='')
        w = csv.writer(out)
        w.writerow(COLUMNS)
        for stamp, ppm in samples:
            row = {k:'1' for k in COLUMNS}
            row['datetime']=stamp
            row['licor_ch4']=str(Decimal(str(ppm))*1000)
            w.writerow([row[k] for k in COLUMNS])
        return out.getvalue().encode()

    def test_exact_threshold_endpoints(self):
        raw=self.fixture([('2021-07-21 00:00:00','3.90'),
                          ('2021-07-21 00:01:00','4.00'),
                          ('2021-07-21 00:02:00','4.10'),
                          ('2021-07-21 00:03:00','4.11')])
        self.assertEqual(analyze_csv(raw,pinned=False)['2021-07-21'],
                         [4,3,[[0,1],[1,1],[1,3]]])

    def test_duplicate_timestamp(self):
        raw=self.fixture([('2021-07-21 00:00:00',3),('2021-07-21 00:00:00',4)])
        with self.assertRaisesRegex(ValueError,'TIME'):
            analyze_csv(raw,pinned=False)

    def test_nonpositive_reference(self):
        with self.assertRaisesRegex(ValueError,'REFERENCE'):
            analyze_csv(self.fixture([('2021-07-21 00:00:00',-1)]),pinned=False)

    def test_unpinned_csv_rejected(self):
        with self.assertRaisesRegex(ValueError,'CSV_SHA'):
            analyze_csv(self.fixture([('2021-07-21 00:00:00',3)]))


if __name__=='__main__':
    unittest.main()
