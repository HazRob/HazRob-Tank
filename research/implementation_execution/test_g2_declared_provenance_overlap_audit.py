"""Synthetic G2 provenance overlap regressions, not independent gas trials."""
import tempfile
import unittest
from pathlib import Path
from g2_declared_provenance_overlap_audit import audit_declared_overlap, audit_csv


class DeclaredProvenanceOverlapTests(unittest.TestCase):
    def setUp(self):
        self.columns = ['group_id', 'session_id', 'exposure_batch_id', 'device_id']
        self.rows = [
            dict(group_id='g1', session_id='s1', exposure_batch_id='b1', device_id='d1'),
            dict(group_id='g1', session_id='s1', exposure_batch_id='b1', device_id='d1'),
            dict(group_id='g2', session_id='s2', exposure_batch_id='b2', device_id='d1'),
        ]

    def test_device_reuse_is_not_mislabeled_session_leakage(self):
        result = audit_declared_overlap(self.rows, self.columns)
        self.assertFalse(result['potential_session_or_batch_group_overlap'])
        self.assertEqual(result['by_provenance_field']['device_id']
                         ['ids_spanning_multiple_declared_groups'], ['d1'])
        self.assertFalse(result['g2_qualified'])

    def test_cross_group_session_overlap_is_reported_not_rejected(self):
        self.rows[2]['session_id'] = 's1'
        result = audit_declared_overlap(self.rows, self.columns)
        self.assertTrue(result['potential_session_or_batch_group_overlap'])
        self.assertEqual(result['by_provenance_field']['session_id']
                         ['ids_spanning_multiple_declared_groups'], ['s1'])

    def test_cross_group_batch_overlap_is_reported(self):
        self.rows[2]['exposure_batch_id'] = 'b1'
        result = audit_declared_overlap(self.rows, self.columns)
        self.assertTrue(result['potential_session_or_batch_group_overlap'])
        self.assertEqual(result['by_provenance_field']['exposure_batch_id']
                         ['cross_group_id_count'], 1)

    def test_missing_session_is_unverified_not_clean(self):
        for row in self.rows:
            del row['session_id']
        result = audit_declared_overlap(self.rows,
                                        [c for c in self.columns if c != 'session_id'])
        self.assertIsNone(result['potential_session_or_batch_group_overlap'])
        self.assertFalse(result['by_provenance_field']['session_id']['checked'])

    def test_invalid_optional_id_fails_closed(self):
        self.rows[0]['session_id'] = ' s1'
        with self.assertRaisesRegex(ValueError, 'G2_PROVENANCE_ID_INVALID'):
            audit_declared_overlap(self.rows, self.columns)

    def test_row_order_invariance_with_unicode(self):
        self.rows[0]['session_id'] = 'جلسة-١'
        self.rows[1]['session_id'] = 'جلسة-١'
        a = audit_declared_overlap(self.rows, self.columns)
        b = audit_declared_overlap(list(reversed(self.rows)), self.columns)
        self.assertEqual(a, b)

    def test_csv_roundtrip_and_duplicate_header(self):
        with tempfile.TemporaryDirectory() as tmp:
            p = Path(tmp) / 'rows.csv'
            p.write_text('group_id,session_id,exposure_batch_id,device_id\n'
                         'g1,s1,b1,d1\ng2,s2,b2,d1\n', encoding='utf-8')
            self.assertFalse(audit_csv(p)['potential_session_or_batch_group_overlap'])
            p.write_text('group_id,group_id\ng1,g1\n', encoding='utf-8')
            with self.assertRaisesRegex(ValueError, 'G2_PROVENANCE_HEADER_INVALID'):
                audit_csv(p)

    def test_empty_and_noncanonical_ids_fail(self):
        with self.assertRaisesRegex(ValueError, 'G2_PROVENANCE_EMPTY'):
            audit_declared_overlap([], self.columns)
        self.rows[0]['group_id'] = 'g1\n'
        with self.assertRaisesRegex(ValueError, 'G2_PROVENANCE_ROW_INVALID'):
            audit_declared_overlap(self.rows, self.columns)


if __name__ == '__main__':
    unittest.main()
