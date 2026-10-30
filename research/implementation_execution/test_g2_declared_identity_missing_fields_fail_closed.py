"""Synthetic G2 regressions: incomplete declared identity must not imply test independence."""
import unittest

from g2_declared_holdout_partition_audit import audit_declared_holdout

ALL = ('group_id', 'session_id', 'exposure_batch_id', 'device_id',
       'partition', 'exposure_id', 'physical_sample_id')


def sample(group, partition):
    return {
        'group_id': group,
        'session_id': 'session-' + group,
        'exposure_batch_id': 'batch-' + group,
        'device_id': 'same-device',
        'partition': partition,
        'exposure_id': 'exposure-' + group,
        'physical_sample_id': 'sample-' + group,
    }


def run(omitted=(), rows=None):
    columns = [c for c in ALL if c not in omitted]
    raw = rows or [sample('train', 'train'), sample('test-a', 'test'),
                   sample('test-b', 'test')]
    return audit_declared_holdout(
        [{k: row[k] for k in columns} for row in raw], columns)


class MissingIdentityFailClosedTests(unittest.TestCase):
    def test_complete_identity_support_is_descriptive_only(self):
        result = run()
        self.assertTrue(result['declared_identity_test_support_complete'])
        self.assertEqual(result['declared_identity_fields_missing'], [])
        self.assertEqual(result['declared_identity_test_only_component_count'], 2)
        self.assertTrue(
            result['test_has_two_disconnected_test_only_declared_identity_components'])
        self.assertFalse(result['independent_physical_exposures_verified'])

    def test_missing_exposure_id_cannot_assert_two_clusters(self):
        result = run(('exposure_id',))
        self.assertIsNone(
            result['test_has_two_disconnected_test_only_declared_identity_components'])
        self.assertEqual(result['declared_identity_fields_missing'], ['exposure_id'])
        self.assertTrue(
            result['declared_identity_test_component_counts_are_upper_bounds_when_incomplete'])

    def test_missing_sample_id_cannot_assert_two_clusters(self):
        result = run(('physical_sample_id',))
        self.assertIsNone(
            result['test_has_two_disconnected_test_only_declared_identity_components'])
        self.assertEqual(result['declared_identity_fields_missing'],
                         ['physical_sample_id'])

    def test_both_optional_ids_missing_still_count_declared_upper_bound(self):
        result = run(('exposure_id', 'physical_sample_id'))
        self.assertEqual(result['declared_identity_test_only_component_count'], 2)
        self.assertEqual(result['declared_identity_fields_missing'],
                         ['exposure_id', 'physical_sample_id'])
        self.assertFalse(result['declared_identity_test_support_complete'])
        self.assertIsNone(
            result['test_has_two_disconnected_test_only_declared_identity_components'])

    def test_complete_one_test_component_returns_false(self):
        result = run(rows=[sample('train', 'train'), sample('test-a', 'test')])
        self.assertFalse(
            result['test_has_two_disconnected_test_only_declared_identity_components'])

    def test_complete_transitive_test_connection_returns_false(self):
        rows = [sample('train', 'train'), sample('test-a', 'test'),
                sample('test-b', 'test')]
        rows[2]['physical_sample_id'] = rows[1]['physical_sample_id']
        result = run(rows=rows)
        self.assertFalse(
            result['test_has_two_disconnected_test_only_declared_identity_components'])
        self.assertEqual(result['declared_identity_test_only_component_count'], 1)

    def test_incomplete_provenance_stays_unverified(self):
        result = run(('exposure_batch_id',))
        self.assertFalse(result['checked'])
        self.assertIsNone(result['declared_identity_test_support_complete'])
        self.assertIsNone(
            result['test_has_two_disconnected_test_only_declared_identity_components'])

    def test_permutation_determinism_and_no_promotion(self):
        rows = [sample('train', 'train'), sample('test-a', 'test'),
                sample('test-b', 'test')]
        columns = [c for c in ALL if c != 'physical_sample_id']
        def check(data):
            return audit_declared_holdout(
                [{k: row[k] for k in columns} for row in data], columns)
        first, second = check(rows), check(list(reversed(rows)))
        self.assertEqual(first, second)
        self.assertFalse(first['g2_qualified'])
        self.assertFalse(first['g3_qualified'])


if __name__ == '__main__':
    unittest.main()
