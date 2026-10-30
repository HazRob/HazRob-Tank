"""Synthetic regression for declared calibration holdout coverage (not G2 evidence)."""
import unittest

from g2_declared_holdout_partition_audit import audit_declared_holdout


COLUMNS = [
    'group_id', 'session_id', 'exposure_batch_id', 'device_id',
    'partition', 'exposure_id', 'physical_sample_id',
]


def row(group, session, batch, partition, exposure, sample, device='shared'):
    return dict(group_id=group, session_id=session,
                exposure_batch_id=batch, device_id=device,
                partition=partition, exposure_id=exposure,
                physical_sample_id=sample)


class CalibrationPartitionAuditTests(unittest.TestCase):
    def dataset(self):
        return [
            row('g1', 's1', 'b1', 'train', 'e1', 'p1'),
            row('g2', 's2', 'b2', 'calibration', 'e2', 'p2'),
            row('g3', 's3', 'b3', 'test', 'e3', 'p3'),
            row('g4', 's4', 'b4', 'test', 'e4', 'p4'),
        ]

    def audit(self, rows):
        return audit_declared_holdout(rows, COLUMNS)

    def test_calibration_partition_is_supported_not_unknown(self):
        result = self.audit(self.dataset())
        self.assertTrue(result['checked'])
        self.assertEqual(result['partition_labels'],
                         ['calibration', 'test', 'train'])
        self.assertTrue(result['calibration_partition_declared'])
        self.assertFalse(
            result['calibration_partition_absent_requires_g2_protocol_review'])
        self.assertFalse(result['potential_partition_overlap'])

    def test_declared_group_counts_and_test_support(self):
        result = self.audit(self.dataset())
        self.assertEqual(result['declared_rows_per_partition'],
                         {'calibration': 1, 'test': 2, 'train': 1})
        self.assertEqual(result['declared_groups_per_partition'],
                         {'calibration': 1, 'test': 2, 'train': 1})
        self.assertTrue(
            result['test_has_two_declared_groups_for_cluster_uncertainty'])
        self.assertTrue(
            result['declared_group_counts_do_not_verify_physical_independence'])
        self.assertFalse(result['independent_physical_exposures_verified'])
        self.assertFalse(result['g2_qualified'])

    def test_calibration_reusing_training_session_detected(self):
        rows = self.dataset()
        rows[1]['session_id'] = 's1'
        result = self.audit(rows)
        self.assertTrue(result['potential_partition_overlap'])
        self.assertEqual(result['cross_partition_ids']['session_id'], ['s1'])
        self.assertEqual(result['cross_partition_components'], [{
            'groups': ['g1', 'g2'], 'partitions': ['calibration', 'train'],
        }])

    def test_calibration_reusing_test_sample_detected(self):
        rows = self.dataset()
        rows[1]['physical_sample_id'] = 'p3'
        result = self.audit(rows)
        self.assertTrue(result['potential_partition_overlap'])
        self.assertEqual(
            result['cross_partition_ids']['physical_sample_id'], ['p3'])

    def test_absent_calibration_flagged_but_overlap_diagnostic_still_runs(self):
        rows = [r for r in self.dataset()
                if r['partition'] != 'calibration']
        result = self.audit(rows)
        self.assertTrue(result['checked'])
        self.assertFalse(result['calibration_partition_declared'])
        self.assertTrue(
            result['calibration_partition_absent_requires_g2_protocol_review'])
        self.assertFalse(result['g2_qualified'])

    def test_single_declared_test_group_is_not_cluster_support(self):
        rows = self.dataset()[:-1]
        result = self.audit(rows)
        self.assertTrue(result['checked'])
        self.assertEqual(result['declared_groups_per_partition']['test'], 1)
        self.assertFalse(
            result['test_has_two_declared_groups_for_cluster_uncertainty'])

    def test_calibration_and_test_without_train_remains_unverified(self):
        rows = [r for r in self.dataset() if r['partition'] != 'train']
        result = self.audit(rows)
        self.assertFalse(result['checked'])
        self.assertEqual(result['reason'], 'TRAIN_TEST_NOT_BOTH_DECLARED')
        self.assertTrue(result['calibration_partition_declared'])

    def test_reordering_rows_does_not_change_partition_diagnostics(self):
        rows = self.dataset()
        self.assertEqual(self.audit(rows), self.audit(list(reversed(rows))))

    def test_validation_and_calibration_are_distinct_partitions(self):
        rows = self.dataset()
        rows.append(row('g5', 's5', 'b5', 'validation', 'e5', 'p5'))
        result = self.audit(rows)
        self.assertEqual(result['partition_labels'],
                         ['calibration', 'test', 'train', 'validation'])
        self.assertEqual(result['declared_groups_per_partition']['validation'], 1)
        self.assertFalse(result['potential_partition_overlap'])


if __name__ == '__main__':
    unittest.main()
