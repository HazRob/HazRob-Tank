"""G2 synthetic strict-width CSV regressions; never physical evidence."""
import tempfile
import unittest
from pathlib import Path

from g2_declared_provenance_overlap_audit import audit_csv


class DeclaredProvenanceExactWidthCsvTests(unittest.TestCase):
    HEADER = 'group_id,session_id,exposure_batch_id,device_id,comment\n'

    def audit(self, data):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / 'declared.csv'
            path.write_bytes(data if isinstance(data, bytes) else data.encode('utf-8'))
            return audit_csv(path)

    def test_missing_noncritical_trailing_cell_rejected(self):
        with self.assertRaisesRegex(ValueError, 'G2_PROVENANCE_CSV_ROW_WIDTH_INVALID'):
            self.audit(self.HEADER + 'g1,s1,b1,d1\n')

    def test_surplus_cell_rejected(self):
        with self.assertRaisesRegex(ValueError, 'G2_PROVENANCE_CSV_ROW_WIDTH_INVALID'):
            self.audit(self.HEADER + 'g1,s1,b1,d1,ok,extra\n')

    def test_missing_middle_cell_rejected_even_if_last_field_absent(self):
        with self.assertRaisesRegex(ValueError, 'G2_PROVENANCE_CSV_ROW_WIDTH_INVALID'):
            self.audit(self.HEADER + 'g1,s1,d1,ok\n')

    def test_error_reports_record_number(self):
        with self.assertRaisesRegex(ValueError, 'record=3:expected=5:actual=4'):
            self.audit(self.HEADER + 'g1,s1,b1,d1,ok\ng2,s2,b2,d2\n')

    def test_empty_file_rejected(self):
        with self.assertRaisesRegex(ValueError, 'G2_PROVENANCE_HEADER_INVALID'):
            self.audit(b'')

    def test_utf8_bom_accepted_without_identity_change(self):
        csv_text = self.HEADER + 'g1,s1,b1,d1,ok\n'
        a = self.audit(csv_text)
        b = self.audit(b'\xef\xbb\xbf' + csv_text.encode('utf-8'))
        self.assertEqual(a, b)

    def test_quoted_comma_in_noncritical_cell_is_valid(self):
        x = self.audit(self.HEADER + 'g1,s1,b1,d1,"note, verified"\n')
        self.assertEqual(x['rows'], 1)
        self.assertFalse(x['source_provenance_authenticated'])
        self.assertFalse(x['g2_qualified'])

    def test_multiline_quoted_noncritical_cell_is_valid(self):
        x = self.audit(self.HEADER + 'g1,s1,b1,d1,"first\nsecond"\n')
        self.assertEqual(x['rows'], 1)

    def test_semantic_digest_row_permutation_invariant(self):
        row1 = 'g1,s1,b1,d1,first\n'
        row2 = 'g2,s2,b2,d1,second\n'
        a = self.audit(self.HEADER + row1 + row2)
        b = self.audit(self.HEADER + row2 + row1)
        self.assertEqual(a, b)
        self.assertFalse(a['potential_session_or_batch_group_overlap'])

    def test_invalid_utf8_rejected(self):
        with self.assertRaisesRegex(ValueError, 'G2_PROVENANCE_CSV_INVALID'):
            self.audit(self.HEADER.encode('utf-8') + b'g1,s1,b1,d1,\xff\n')

    def test_unclosed_quote_rejected(self):
        with self.assertRaisesRegex(ValueError, 'G2_PROVENANCE_CSV_INVALID'):
            self.audit(self.HEADER + 'g1,s1,b1,d1,"unfinished\n')


if __name__ == '__main__':
    unittest.main()
