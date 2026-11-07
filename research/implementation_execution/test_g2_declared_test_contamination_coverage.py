"""Synthetic research-only G2 all-test contamination coverage regressions."""
import unittest
from g2_declared_holdout_partition_audit import audit_declared_holdout

COLUMNS = ('group_id', 'session_id', 'exposure_batch_id', 'device_id',
           'partition', 'exposure_id', 'physical_sample_id')

def row(group, partition):
    return dict(group_id=group, partition=partition,
                session_id='session-' + group,
                exposure_batch_id='batch-' + group,
                exposure_id='exposure-' + group,
                physical_sample_id='sample-' + group, device_id='device-shared')

def audit(rows, omitted=()):
    columns = [k for k in COLUMNS if k not in omitted]
    return audit_declared_holdout([{k: r[k] for k in columns} for r in rows], columns)

class TestAllTestCoverage(unittest.TestCase):
    def base(self):
        return [row('tr','train'), row('va','validation'), row('ca','calibration'),
                row('ta','test'), row('tb','test')]

    def test_two_clean_components_all_test_clean(self):
        x = audit(self.base())
        self.assertIs(x['test_all_declared_identity_components_test_only'], True)
        self.assertIs(x['test_has_two_clean_declared_components_without_any_test_contamination'], True)
        self.assertEqual(x['declared_identity_test_cross_partition_test_group_count'], 0)
        self.assertFalse(x['independent_physical_exposures_verified'])

    def test_two_clean_plus_contaminated_test_fails_strict_gate(self):
        rows = self.base() + [row('tc','test')]
        rows[-1]['session_id'] = rows[0]['session_id']
        x = audit(rows)
        self.assertTrue(x['test_has_two_disconnected_test_only_declared_identity_components'])
        self.assertIs(x['test_all_declared_identity_components_test_only'], False)
        self.assertIs(x['test_has_two_clean_declared_components_without_any_test_contamination'], False)
        self.assertEqual(x['declared_identity_test_cross_partition_test_group_count'], 1)

    def test_transitive_contamination_through_validation(self):
        rows = self.base()
        rows[1]['exposure_id'] = rows[0]['exposure_id']
        rows[3]['physical_sample_id'] = rows[1]['physical_sample_id']
        x = audit(rows)
        self.assertIs(x['test_all_declared_identity_components_test_only'], False)
        self.assertEqual(x['declared_identity_test_cross_partition_test_group_count'], 1)

    def test_two_contaminated_test_groups_count_once_each(self):
        rows = self.base()
        rows[3]['session_id'] = rows[0]['session_id']
        rows[4]['exposure_batch_id'] = rows[3]['exposure_batch_id']
        x = audit(rows)
        self.assertEqual(x['declared_identity_test_cross_partition_test_group_count'], 2)
        self.assertIs(x['test_has_two_clean_declared_components_without_any_test_contamination'], False)

    def test_missing_optional_identity_unknown_when_no_link(self):
        x = audit(self.base(), omitted=('physical_sample_id',))
        self.assertIsNone(x['test_all_declared_identity_components_test_only'])
        self.assertIsNone(x['test_has_two_clean_declared_components_without_any_test_contamination'])

    def test_missing_optional_identity_known_overlap_negative(self):
        rows = self.base()
        rows[3]['exposure_batch_id'] = rows[0]['exposure_batch_id']
        x = audit(rows, omitted=('physical_sample_id',))
        self.assertIs(x['test_all_declared_identity_components_test_only'], False)
        self.assertIs(x['test_has_two_clean_declared_components_without_any_test_contamination'], False)

    def test_missing_optional_one_test_cannot_support_two(self):
        x = audit([row('tr','train'),row('ta','test')], omitted=('physical_sample_id',))
        self.assertIsNone(x['test_all_declared_identity_components_test_only'])
        self.assertIs(x['test_has_two_clean_declared_components_without_any_test_contamination'], False)

    def test_one_clean_test_complete_identity(self):
        x = audit([row('tr','train'),row('ta','test')])
        self.assertIs(x['test_all_declared_identity_components_test_only'], True)
        self.assertIs(x['test_has_two_clean_declared_components_without_any_test_contamination'], False)

    def test_device_reuse_does_not_link_groups(self):
        rows = self.base()
        self.assertEqual(len({r['device_id'] for r in rows}), 1)
        self.assertIs(audit(rows)['test_all_declared_identity_components_test_only'], True)

    def test_row_order_invariant(self):
        rows = self.base() + [row('tc','test')]
        rows[-1]['physical_sample_id'] = rows[0]['physical_sample_id']
        self.assertEqual(audit(rows), audit(list(reversed(rows))))

    def test_missing_test_partition_unverified(self):
        x = audit([row('tr','train'),row('va','validation')])
        self.assertFalse(x['checked'])
        self.assertIsNone(x['test_all_declared_identity_components_test_only'])
        self.assertIsNone(x['test_has_two_clean_declared_components_without_any_test_contamination'])

    def test_no_scientific_promotion(self):
        x = audit(self.base())
        for flag in ('source_provenance_authenticated','independent_physical_exposures_verified',
                     'g2_qualified','g3_qualified'):
            self.assertFalse(x[flag])

if __name__ == '__main__':
    unittest.main()
