"""Synthetic compatibility-alias regressions; not independent physical evidence."""
import unittest

from g2_declared_provenance_overlap_audit import audit_declared_overlap


class DeclaredProvenanceNfkcAliasTests(unittest.TestCase):
    def setUp(self):
        self.columns = ['group_id', 'session_id', 'exposure_batch_id', 'device_id']
        self.rows = [
            dict(group_id='g1', session_id='s1', exposure_batch_id='b1', device_id='d1'),
            dict(group_id='g2', session_id='s2', exposure_batch_id='b2', device_id='d2'),
        ]

    def test_compatible_group_names_fail_closed(self):
        self.rows[1]['group_id'] = 'g１'
        self.rows[0]['group_id'] = 'g1'
        with self.assertRaisesRegex(ValueError, 'G2_PROVENANCE_ID_ALIAS_NFKC:group_id'):
            audit_declared_overlap(self.rows, self.columns)

    def test_compatible_session_names_fail_closed(self):
        self.rows[1]['session_id'] = 'ｓ1'
        self.rows[0]['session_id'] = 's1'
        with self.assertRaisesRegex(ValueError, 'G2_PROVENANCE_ID_ALIAS_NFKC:session_id'):
            audit_declared_overlap(self.rows, self.columns)

    def test_compatible_batch_names_fail_closed(self):
        self.rows[1]['exposure_batch_id'] = 'ｂ1'
        self.rows[0]['exposure_batch_id'] = 'b1'
        with self.assertRaisesRegex(ValueError, 'G2_PROVENANCE_ID_ALIAS_NFKC:exposure_batch_id'):
            audit_declared_overlap(self.rows, self.columns)

    def test_compatible_device_names_fail_closed(self):
        self.rows[1]['device_id'] = 'ｄ1'
        self.rows[0]['device_id'] = 'd1'
        with self.assertRaisesRegex(ValueError, 'G2_PROVENANCE_ID_ALIAS_NFKC:device_id'):
            audit_declared_overlap(self.rows, self.columns)

    def test_critical_header_alias_cannot_hide_session_metadata(self):
        for row in self.rows:
            row['ｓｅｓｓｉｏｎ＿ｉｄ'] = row.pop('session_id')
        columns = [c for c in self.columns if c != 'session_id'] + ['ｓｅｓｓｉｏｎ＿ｉｄ']
        with self.assertRaisesRegex(ValueError, 'G2_PROVENANCE_HEADER_ALIAS_NFKC'):
            audit_declared_overlap(self.rows, columns)

    def test_compatible_noncritical_headers_cannot_coexist(self):
        for row in self.rows:
            row['tag'] = 'a'
            row['ｔａｇ'] = 'b'
        with self.assertRaisesRegex(ValueError, 'G2_PROVENANCE_HEADER_ALIAS_NFKC'):
            audit_declared_overlap(self.rows, self.columns + ['tag', 'ｔａｇ'])

    def test_distinct_canonical_arabic_ids_are_preserved(self):
        self.rows[0]['session_id'] = 'جلسة-١'
        self.rows[1]['session_id'] = 'جلسة-٢'
        result = audit_declared_overlap(self.rows, self.columns)
        self.assertFalse(result['potential_session_or_batch_group_overlap'])
        self.assertFalse(result['source_provenance_authenticated'])
        self.assertFalse(result['g2_qualified'])

    def test_row_order_invariant_without_compatibility_alias(self):
        a = audit_declared_overlap(self.rows, self.columns)
        b = audit_declared_overlap(list(reversed(self.rows)), self.columns)
        self.assertEqual(a, b)


if __name__ == '__main__':
    unittest.main()
