"""Synthetic regression tests for declared G2 holdout partitions; no gas evidence."""
import tempfile
import unittest
from pathlib import Path

from g2_declared_holdout_partition_audit import (
    audit_declared_holdout, audit_csv,
)

COLUMNS = ['group_id', 'session_id', 'exposure_batch_id', 'device_id',
           'partition', 'exposure_id', 'physical_sample_id']


def row(group, session, batch, partition, exposure, sample, device='d1'):
    return dict(group_id=group, session_id=session,
                exposure_batch_id=batch, device_id=device,
                partition=partition, exposure_id=exposure,
                physical_sample_id=sample)


class HoldoutPartitionTests(unittest.TestCase):
    def audit(self, rows, columns=COLUMNS):
        return audit_declared_holdout(rows, columns)

    def clean(self):
        return [row('g1', 's1', 'b1', 'train', 'e1', 'p1'),
                row('g2', 's2', 'b2', 'test', 'e2', 'p2')]

    def test_disjoint_declarations_do_not_prove_physical_independence(self):
        result = self.audit(self.clean())
        self.assertTrue(result['checked'])
        self.assertFalse(result['potential_partition_overlap'])
        self.assertFalse(result['independent_physical_exposures_verified'])
        self.assertFalse(result['g2_qualified'])
        self.assertTrue(result['no_detected_overlap_is_not_physical_independence_evidence'])

    def test_same_session_crosses_train_and_test(self):
        rows = self.clean()
        rows[1]['session_id'] = 's1'
        result = self.audit(rows)
        self.assertTrue(result['potential_partition_overlap'])
        self.assertEqual(result['cross_partition_ids']['session_id'], ['s1'])

    def test_same_batch_crosses_train_and_test(self):
        rows = self.clean()
        rows[1]['exposure_batch_id'] = 'b1'
        result = self.audit(rows)
        self.assertEqual(result['cross_partition_ids']['exposure_batch_id'], ['b1'])
        self.assertTrue(result['potential_partition_overlap'])

    def test_same_group_in_two_partitions(self):
        rows = self.clean()
        rows[1]['group_id'] = 'g1'
        result = self.audit(rows)
        self.assertEqual(result['cross_partition_ids']['group_id'], ['g1'])
        self.assertEqual(len(result['cross_partition_components']), 1)

    def test_transitive_session_batch_chain_crosses_holdout(self):
        rows = [row('g1', 's12', 'b1', 'train', 'e1', 'p1'),
                row('g2', 's12', 'b23', 'train', 'e2', 'p2'),
                row('g3', 's3', 'b23', 'test', 'e3', 'p3')]
        result = self.audit(rows)
        self.assertTrue(result['potential_partition_overlap'])
        self.assertEqual(result['cross_partition_components'], [{
            'groups': ['g1', 'g2', 'g3'],
            'partitions': ['test', 'train'],
        }])

    def test_device_reuse_alone_is_not_leakage(self):
        result = self.audit(self.clean())
        self.assertEqual(result['cross_partition_ids']['device_id'], ['d1'])
        self.assertFalse(result['potential_partition_overlap'])
        self.assertTrue(result['device_reuse_is_not_automatic_leakage'])

    def test_exposure_id_cross_partition_detected(self):
        rows = self.clean()
        rows[1]['exposure_id'] = 'e1'
        self.assertEqual(self.audit(rows)['cross_partition_ids']['exposure_id'],
                         ['e1'])

    def test_sample_id_cross_partition_detected(self):
        rows = self.clean()
        rows[1]['physical_sample_id'] = 'p1'
        self.assertTrue(self.audit(rows)['potential_partition_overlap'])
        self.assertEqual(self.audit(rows)['cross_partition_ids']['physical_sample_id'],
                         ['p1'])

    def test_missing_partition_is_unverified(self):
        columns = [c for c in COLUMNS if c != 'partition']
        rows = [{k: v for k, v in r.items() if k != 'partition'}
                for r in self.clean()]
        result = self.audit(rows, columns)
        self.assertFalse(result['checked'])
        self.assertIsNone(result['potential_partition_overlap'])
        self.assertEqual(result['reason'], 'PARTITION_COLUMN_ABSENT')

    def test_missing_session_is_unverified(self):
        columns = [c for c in COLUMNS if c != 'session_id']
        rows = [{k: v for k, v in r.items() if k != 'session_id'}
                for r in self.clean()]
        self.assertEqual(self.audit(rows, columns)['reason'],
                         'SESSION_OR_BATCH_PROVENANCE_ABSENT')

    def test_train_only_is_unverified(self):
        rows = self.clean()
        rows[1]['partition'] = 'train'
        result = self.audit(rows)
        self.assertFalse(result['checked'])
        self.assertEqual(result['reason'], 'TRAIN_TEST_NOT_BOTH_DECLARED')

    def test_unknown_partition_rejected(self):
        rows = self.clean()
        rows[1]['partition'] = 'holdout'
        with self.assertRaisesRegex(ValueError, 'G2_HOLDOUT_PARTITION_LABEL_INVALID'):
            self.audit(rows)

    def test_nfkc_partition_header_alias_rejected(self):
        columns = COLUMNS + ['ｐａｒｔｉｔｉｏｎ']
        rows = [dict(r, **{'ｐａｒｔｉｔｉｏｎ': r['partition']})
                for r in self.clean()]
        with self.assertRaisesRegex(ValueError, 'G2_HOLDOUT_CRITICAL_HEADER_ALIAS'):
            self.audit(rows, columns)

    def test_nfkc_optional_header_alias_rejected(self):
        columns = [c if c != 'exposure_id' else 'ｅｘｐｏｓｕｒｅ＿ｉｄ'
                   for c in COLUMNS]
        rows = [{(k if k != 'exposure_id' else 'ｅｘｐｏｓｕｒｅ＿ｉｄ'): v
                 for k, v in r.items()} for r in self.clean()]
        with self.assertRaisesRegex(ValueError, 'G2_HOLDOUT_CRITICAL_HEADER_ALIAS'):
            self.audit(rows, columns)

    def test_row_permutation_preserves_diagnostic(self):
        rows = self.clean()
        self.assertEqual(self.audit(rows), self.audit(list(reversed(rows))))

    def test_arabic_ids_are_distinct(self):
        rows = [row('مجموعة١', 'جلسة١', 'دفعة١', 'train', 'تعرض١', 'عينة١'),
                row('مجموعة٢', 'جلسة٢', 'دفعة٢', 'test', 'تعرض٢', 'عينة٢')]
        self.assertFalse(self.audit(rows)['potential_partition_overlap'])

    def test_optional_identity_absent_is_explicit(self):
        columns = [c for c in COLUMNS
                   if c not in ('exposure_id', 'physical_sample_id')]
        rows = [{k: v for k, v in r.items() if k in columns}
                for r in self.clean()]
        result = self.audit(rows, columns)
        self.assertTrue(result['checked'])
        self.assertIsNone(result['cross_partition_ids']['exposure_id'])
        self.assertFalse(result['optional_physical_sample_id_checked'])

    def test_empty_optional_exposure_id_rejected(self):
        rows = self.clean()
        rows[1]['exposure_id'] = ''
        with self.assertRaisesRegex(ValueError, 'G2_HOLDOUT_OPTIONAL_ID_INVALID'):
            self.audit(rows)

    def test_nfkc_alias_optional_exposure_id_rejected(self):
        rows = self.clean()
        rows[1]['exposure_id'] = 'ｅ1'
        rows[0]['exposure_id'] = 'e1'
        with self.assertRaisesRegex(ValueError, 'G2_HOLDOUT_OPTIONAL_ID_ALIAS_NFKC'):
            self.audit(rows)

    def test_nfkc_alias_optional_sample_id_rejected(self):
        rows = self.clean()
        rows[1]['physical_sample_id'] = 'ｐ1'
        with self.assertRaisesRegex(ValueError, 'G2_HOLDOUT_OPTIONAL_ID_ALIAS_NFKC'):
            self.audit(rows)

    def test_csv_strict_width_and_roundtrip(self):
        header = ','.join(COLUMNS) + '\n'
        valid = header + 'g1,s1,b1,d1,train,e1,p1\ng2,s2,b2,d1,test,e2,p2\n'
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / 'declared.csv'
            path.write_text(valid, encoding='utf-8')
            self.assertFalse(audit_csv(path)['potential_partition_overlap'])
            path.write_text(header + 'g1,s1,b1,d1,train,e1\n',
                            encoding='utf-8')
            with self.assertRaisesRegex(ValueError, 'G2_HOLDOUT_CSV_WIDTH_INVALID'):
                audit_csv(path)


if __name__ == '__main__':
    unittest.main()
