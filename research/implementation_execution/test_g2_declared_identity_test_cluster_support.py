"""Synthetic research-only regressions for effective declared test clusters."""
import unittest

from g2_declared_holdout_partition_audit import audit_declared_holdout

COLUMNS = ('group_id', 'session_id', 'exposure_batch_id', 'device_id',
           'partition', 'exposure_id', 'physical_sample_id')


def row(group, partition, device='shared'):
    return {
        'group_id': group, 'session_id': 'session-' + group,
        'exposure_batch_id': 'batch-' + group, 'device_id': device,
        'partition': partition, 'exposure_id': 'exposure-' + group,
        'physical_sample_id': 'sample-' + group,
    }


class DeclaredIdentityTestClusterTests(unittest.TestCase):
    def base(self):
        return [row('train', 'train'), row('test1', 'test'),
                row('test2', 'test')]

    def audit(self, rows, columns=COLUMNS):
        return audit_declared_holdout(rows, columns)

    def test_two_separate_declared_test_components(self):
        result = self.audit(self.base())
        self.assertEqual(result['declared_identity_test_component_count'], 2)
        self.assertEqual(result['declared_identity_test_only_component_count'], 2)
        self.assertTrue(
            result['test_has_two_disconnected_test_only_declared_identity_components'])
        self.assertFalse(result['independent_physical_exposures_verified'])

    def test_shared_test_sample_collapses_declared_components(self):
        rows = self.base()
        rows[2]['physical_sample_id'] = rows[1]['physical_sample_id']
        result = self.audit(rows)
        self.assertTrue(result['test_has_two_declared_groups_for_cluster_uncertainty'])
        self.assertEqual(result['declared_identity_test_component_count'], 1)
        self.assertFalse(
            result['test_has_two_disconnected_test_only_declared_identity_components'])

    def test_test_groups_joined_through_training_identity(self):
        rows = self.base()
        rows[1]['physical_sample_id'] = rows[0]['physical_sample_id']
        rows[2]['exposure_id'] = rows[0]['exposure_id']
        result = self.audit(rows)
        self.assertEqual(result['declared_identity_test_component_count'], 1)
        self.assertEqual(result['declared_identity_test_only_component_count'], 0)
        self.assertEqual(len(result['declared_identity_test_cross_partition_components']), 1)
        self.assertEqual(
            result['declared_identity_test_cross_partition_components'][0]['partitions'],
            ['test', 'train'])

    def test_shared_device_alone_does_not_collapse_components(self):
        result = self.audit(self.base())
        self.assertEqual(result['declared_identity_test_only_component_count'], 2)

    def test_test_group_linked_to_calibration_excluded_from_clean_support(self):
        rows = self.base() + [row('cal', 'calibration')]
        rows[1]['exposure_batch_id'] = rows[3]['exposure_batch_id']
        result = self.audit(rows)
        self.assertEqual(result['declared_identity_test_component_count'], 2)
        self.assertEqual(result['declared_identity_test_only_component_count'], 1)
        self.assertFalse(
            result['test_has_two_disconnected_test_only_declared_identity_components'])

    def test_optional_identity_columns_absent_still_audits_sessions(self):
        columns = [c for c in COLUMNS
                   if c not in ('exposure_id', 'physical_sample_id')]
        rows = [{k: v for k, v in r.items() if k in columns} for r in self.base()]
        result = self.audit(rows, columns)
        self.assertEqual(result['declared_identity_test_only_component_count'], 2)
        self.assertEqual(result['declared_identity_fields_used'],
                         ['session_id', 'exposure_batch_id'])

    def test_session_transitive_test_connection(self):
        rows = self.base()
        rows[2]['session_id'] = rows[1]['session_id']
        self.assertEqual(
            self.audit(rows)['declared_identity_test_component_count'], 1)

    def test_reordering_rows_preserves_diagnostics(self):
        rows = self.base()
        rows[2]['exposure_id'] = rows[1]['exposure_id']
        self.assertEqual(self.audit(rows), self.audit(list(reversed(rows))))

    def test_missing_partition_is_unverified_not_zero_components(self):
        columns = [c for c in COLUMNS if c != 'partition']
        rows = [{k: v for k, v in r.items() if k in columns} for r in self.base()]
        result = self.audit(rows, columns)
        self.assertFalse(result['checked'])
        self.assertIsNone(result['declared_identity_test_component_count'])

    def test_missing_batch_is_unverified_not_zero_components(self):
        columns = [c for c in COLUMNS if c != 'exposure_batch_id']
        rows = [{k: v for k, v in r.items() if k in columns} for r in self.base()]
        result = self.audit(rows, columns)
        self.assertFalse(result['checked'])
        self.assertIsNone(result['declared_identity_test_only_component_count'])

    def test_one_test_component_connected_to_train_and_two_clean(self):
        rows = self.base() + [row('test3', 'test')]
        rows[1]['session_id'] = rows[0]['session_id']
        result = self.audit(rows)
        self.assertEqual(result['declared_identity_test_component_count'], 3)
        self.assertEqual(result['declared_identity_test_only_component_count'], 2)
        self.assertTrue(
            result['test_has_two_disconnected_test_only_declared_identity_components'])

    def test_single_group_reused_train_test_is_cross_partition(self):
        rows = self.base()
        rows[1]['group_id'] = 'train'
        result = self.audit(rows)
        self.assertEqual(result['declared_identity_test_only_component_count'], 1)
        self.assertTrue(result['potential_partition_overlap'])

    def test_declared_test_clusters_never_grant_qualification(self):
        result = self.audit(self.base())
        self.assertFalse(result['g2_qualified'])
        self.assertFalse(result['g3_qualified'])
        self.assertTrue(
            result['declared_identity_components_are_not_authenticated_experiments'])


if __name__ == '__main__':
    unittest.main()
