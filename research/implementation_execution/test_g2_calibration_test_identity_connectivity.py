"""Synthetic-only G2 calibration/test declared identity connectivity regressions.

These checks diagnose declared metadata. They do not authenticate physical
exposures, concentration references, or scientific G2/G3 qualification.
"""
import unittest

from g2_declared_holdout_partition_audit import audit_declared_holdout

FIELDS = ('group_id', 'session_id', 'exposure_batch_id', 'device_id',
          'partition', 'exposure_id', 'physical_sample_id')


def row(group, partition):
    return {
        'group_id': group,
        'session_id': 'session-' + group,
        'exposure_batch_id': 'batch-' + group,
        'device_id': 'device-shared',
        'partition': partition,
        'exposure_id': 'exposure-' + group,
        'physical_sample_id': 'sample-' + group,
    }


def run(rows, omitted=()):
    columns = [field for field in FIELDS if field not in omitted]
    return audit_declared_holdout(
        [{key: record[key] for key in columns} for record in rows], columns)


class CalibrationTestIdentityConnectivityTests(unittest.TestCase):
    def base(self):
        return [row('train', 'train'), row('cal', 'calibration'),
                row('test-a', 'test'), row('test-b', 'test')]

    def test_complete_disconnected_calibration_test_is_false(self):
        result = run(self.base())
        self.assertIs(
            result['calibration_test_declared_identity_component_overlap'],
            False)
        self.assertFalse(result['independent_physical_exposures_verified'])
        self.assertFalse(result['g2_qualified'])
        self.assertFalse(result['g3_qualified'])

    def test_direct_session_overlap_is_true(self):
        rows = self.base()
        rows[2]['session_id'] = rows[1]['session_id']
        self.assertIs(
            run(rows)['calibration_test_declared_identity_component_overlap'],
            True)

    def test_direct_sample_overlap_is_true(self):
        rows = self.base()
        rows[3]['physical_sample_id'] = rows[1]['physical_sample_id']
        self.assertIs(
            run(rows)['calibration_test_declared_identity_component_overlap'],
            True)

    def test_transitive_calibration_train_test_is_true(self):
        rows = self.base()
        rows[0]['exposure_id'] = rows[1]['exposure_id']
        rows[2]['physical_sample_id'] = rows[0]['physical_sample_id']
        result = run(rows)
        self.assertIs(
            result['calibration_test_declared_identity_component_overlap'],
            True)
        self.assertTrue(result['potential_partition_overlap'])

    def test_device_reuse_alone_is_not_overlap(self):
        rows = self.base()
        self.assertEqual(len({r['device_id'] for r in rows}), 1)
        self.assertIs(
            run(rows)['calibration_test_declared_identity_component_overlap'],
            False)

    def test_missing_exposure_id_returns_unknown_not_false(self):
        result = run(self.base(), omitted=('exposure_id',))
        self.assertIsNone(
            result['calibration_test_declared_identity_component_overlap'])

    def test_missing_sample_id_but_known_overlap_remains_true(self):
        rows = self.base()
        rows[2]['session_id'] = rows[1]['session_id']
        result = run(rows, omitted=('physical_sample_id',))
        self.assertIs(
            result['calibration_test_declared_identity_component_overlap'],
            True)

    def test_absent_calibration_partition_returns_unknown(self):
        rows = [row('train', 'train'), row('test-a', 'test'),
                row('test-b', 'test')]
        result = run(rows)
        self.assertIsNone(
            result['calibration_test_declared_identity_component_overlap'])
        self.assertTrue(
            result['calibration_partition_absent_requires_g2_protocol_review'])

    def test_missing_required_batch_returns_unknown(self):
        result = run(self.base(), omitted=('exposure_batch_id',))
        self.assertFalse(result['checked'])
        self.assertIsNone(
            result['calibration_test_declared_identity_component_overlap'])

    def test_row_order_invariance(self):
        rows = self.base()
        rows[2]['session_id'] = rows[1]['session_id']
        self.assertEqual(run(rows), run(list(reversed(rows))))


if __name__ == '__main__':
    unittest.main()
