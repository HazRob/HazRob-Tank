"""Synthetic G2 train/validation/test declared-identity connectivity tests.

No physical independence, reference concentration or G2/G3 promotion implied.
"""
import unittest

from g2_declared_holdout_partition_audit import audit_declared_holdout

FIELDS = ('group_id', 'session_id', 'exposure_batch_id', 'device_id',
          'partition', 'exposure_id', 'physical_sample_id')


def record(group, partition):
    return {
        'group_id': group, 'partition': partition,
        'session_id': 'session-' + group,
        'exposure_batch_id': 'batch-' + group,
        'exposure_id': 'exposure-' + group,
        'physical_sample_id': 'sample-' + group,
        'device_id': 'device-shared',
    }


def inspect(rows, omitted=()):
    columns = [key for key in FIELDS if key not in omitted]
    return audit_declared_holdout(
        [{key: r[key] for key in columns} for r in rows], columns)


class TestPartitionConnectivity(unittest.TestCase):
    def base(self):
        return [record('train', 'train'), record('val', 'validation'),
                record('cal', 'calibration'), record('test-a', 'test'),
                record('test-b', 'test')]

    def test_disconnected_full_ids_all_negative(self):
        result = inspect(self.base())
        for key in ('train_test', 'validation_test', 'calibration_test',
                    'test_non_test'):
            self.assertIs(result[key + '_declared_identity_component_overlap'],
                          False)

    def test_direct_train_test_session_overlap(self):
        rows = self.base()
        rows[3]['session_id'] = rows[0]['session_id']
        result = inspect(rows)
        self.assertIs(result['train_test_declared_identity_component_overlap'],
                      True)
        self.assertIs(result['test_non_test_declared_identity_component_overlap'],
                      True)
        self.assertIs(result['validation_test_declared_identity_component_overlap'],
                      False)

    def test_direct_validation_test_sample_overlap(self):
        rows = self.base()
        rows[4]['physical_sample_id'] = rows[1]['physical_sample_id']
        result = inspect(rows)
        self.assertIs(result['validation_test_declared_identity_component_overlap'],
                      True)
        self.assertIs(result['test_non_test_declared_identity_component_overlap'],
                      True)

    def test_transitive_train_via_validation_to_test(self):
        rows = self.base()
        rows[1]['exposure_id'] = rows[0]['exposure_id']
        rows[3]['physical_sample_id'] = rows[1]['physical_sample_id']
        result = inspect(rows)
        self.assertIs(result['train_test_declared_identity_component_overlap'],
                      True)
        self.assertIs(result['validation_test_declared_identity_component_overlap'],
                      True)
        self.assertIs(result['calibration_test_declared_identity_component_overlap'],
                      False)

    def test_missing_optional_id_unknown_when_disconnected(self):
        result = inspect(self.base(), omitted=('physical_sample_id',))
        for key in ('train_test', 'validation_test', 'calibration_test',
                    'test_non_test'):
            self.assertIsNone(
                result[key + '_declared_identity_component_overlap'])

    def test_missing_optional_id_detected_overlap_still_true(self):
        rows = self.base()
        rows[3]['exposure_batch_id'] = rows[0]['exposure_batch_id']
        result = inspect(rows, omitted=('physical_sample_id',))
        self.assertIs(result['train_test_declared_identity_component_overlap'],
                      True)
        self.assertIs(result['test_non_test_declared_identity_component_overlap'],
                      True)
        self.assertIsNone(
            result['validation_test_declared_identity_component_overlap'])

    def test_absent_validation_returns_unknown(self):
        rows = [r for r in self.base() if r['partition'] != 'validation']
        result = inspect(rows)
        self.assertIsNone(
            result['validation_test_declared_identity_component_overlap'])
        self.assertIs(result['train_test_declared_identity_component_overlap'],
                      False)

    def test_shared_device_does_not_create_identity_overlap(self):
        rows = self.base()
        self.assertEqual(len({r['device_id'] for r in rows}), 1)
        self.assertIs(
            inspect(rows)['test_non_test_declared_identity_component_overlap'],
            False)

    def test_required_session_missing_returns_unknown(self):
        result = inspect(self.base(), omitted=('session_id',))
        self.assertFalse(result['checked'])
        self.assertIsNone(
            result['train_test_declared_identity_component_overlap'])
        self.assertIsNone(
            result['test_non_test_declared_identity_component_overlap'])

    def test_row_order_invariant(self):
        rows = self.base()
        rows[3]['exposure_id'] = rows[1]['exposure_id']
        self.assertEqual(inspect(rows), inspect(list(reversed(rows))))


if __name__ == '__main__':
    unittest.main()
