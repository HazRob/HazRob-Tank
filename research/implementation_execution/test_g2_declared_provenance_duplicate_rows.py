"""Synthetic G2 duplicate-declaration diagnostics; no physical evidence."""
import tempfile
import unittest
from pathlib import Path

from g2_declared_provenance_overlap_audit import audit_csv, audit_declared_overlap


class DeclaredProvenanceDuplicateRowsTests(unittest.TestCase):
    def setUp(self):
        self.columns = ['group_id', 'session_id', 'exposure_batch_id', 'device_id']
        self.a = dict(group_id='g1', session_id='s1', exposure_batch_id='b1', device_id='d1')
        self.b = dict(group_id='g2', session_id='s2', exposure_batch_id='b2', device_id='d1')

    def test_exact_repetition_is_not_independent_sample_evidence(self):
        out = audit_declared_overlap([self.a, self.a.copy(), self.b], self.columns)
        self.assertEqual(out['rows'], 3)
        self.assertEqual(out['unique_declared_rows'], 2)
        self.assertEqual(out['exact_duplicate_declared_rows'], 1)
        self.assertEqual(out['repeated_declared_row_patterns'], 1)
        self.assertEqual(out['declared_rows_per_group'], {'g1': 2, 'g2': 1})
        self.assertTrue(out['duplicate_metadata_is_not_independent_evidence'])
        self.assertFalse(out['g2_qualified'])
        self.assertFalse(out['independent_physical_exposures_verified'])

    def test_repetition_does_not_imply_cross_group_overlap(self):
        out = audit_declared_overlap([self.a, self.a.copy(), self.b], self.columns)
        self.assertFalse(out['potential_session_or_batch_group_overlap'])

    def test_triplicate_counts_extra_rows_not_all_rows(self):
        out = audit_declared_overlap([self.a] * 3 + [self.b], self.columns)
        self.assertEqual(out['exact_duplicate_declared_rows'], 2)
        self.assertEqual(out['repeated_declared_row_patterns'], 1)
        self.assertEqual(out['unique_declared_rows'], 2)

    def test_two_distinct_repeated_patterns(self):
        out = audit_declared_overlap([self.a, self.b, self.a.copy(), self.b.copy()],
                                     self.columns)
        self.assertEqual(out['exact_duplicate_declared_rows'], 2)
        self.assertEqual(out['repeated_declared_row_patterns'], 2)
        self.assertEqual(out['declared_rows_per_group'], {'g1': 2, 'g2': 2})

    def test_noncritical_column_distinguishes_full_declared_records(self):
        cols = self.columns + ['declared_trial']
        a1 = dict(self.a, declared_trial='trial-1')
        a2 = dict(self.a, declared_trial='trial-2')
        out = audit_declared_overlap([a1, a2, dict(self.b, declared_trial='trial-3')], cols)
        self.assertEqual(out['exact_duplicate_declared_rows'], 0)
        self.assertEqual(out['unique_declared_rows'], 3)
        self.assertFalse(out['g3_qualified'])

    def test_order_invariant_and_arabic_identifiers(self):
        rows = [dict(self.a, session_id='جلسة-١'), dict(self.b, session_id='جلسة-٢'),
                dict(self.a, session_id='جلسة-١')]
        left = audit_declared_overlap(rows, self.columns)
        right = audit_declared_overlap(list(reversed(rows)), self.columns)
        self.assertEqual(left, right)
        self.assertEqual(left['exact_duplicate_declared_rows'], 1)

    def test_csv_quoted_fields_and_repeated_rows(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / 'declared.csv'
            path.write_text('group_id,session_id,exposure_batch_id,device_id,tag\n'
                            'g1,s1,b1,d1,"alpha,beta"\n'
                            'g1,s1,b1,d1,"alpha,beta"\n'
                            'g2,s2,b2,d1,"alpha,gamma"\n',
                            encoding='utf-8')
            out = audit_csv(path)
            self.assertEqual(out['rows'], 3)
            self.assertEqual(out['exact_duplicate_declared_rows'], 1)
            self.assertEqual(out['repeated_declared_row_patterns'], 1)


if __name__ == '__main__':
    unittest.main()
