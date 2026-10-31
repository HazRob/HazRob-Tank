"""Synthetic G2 declared group connectivity tests; NOT physical experiments."""
import unittest

from g2_declared_provenance_overlap_audit import audit_declared_overlap


COLUMNS = ['group_id', 'session_id', 'exposure_batch_id', 'device_id']


def record(group, session, batch, device='device-1'):
    return dict(group_id=group, session_id=session,
                exposure_batch_id=batch, device_id=device)


class DeclaredGroupConnectivityTests(unittest.TestCase):
    def connectivity(self, rows, columns=COLUMNS):
        return audit_declared_overlap(rows, columns)['declared_session_batch_connectivity']

    def test_transitive_session_and_batch_chain_is_one_component(self):
        # g1 and g3 do not share any ID directly; g2 connects them.
        rows = [record('g1', 's12', 'b1'),
                record('g2', 's12', 'b23'),
                record('g3', 's3', 'b23')]
        out = self.connectivity(rows)
        self.assertEqual(out['components'], [['g1', 'g2', 'g3']])
        self.assertEqual(out['component_count'], 1)
        self.assertEqual(out['multi_group_component_count'], 1)
        self.assertEqual(out['largest_component_groups'], 3)
        self.assertTrue(out['requires_holdout_split_protocol_review'])

    def test_device_reuse_does_not_link_groups(self):
        out = self.connectivity([record('g1', 's1', 'b1', 'same-device'),
                                 record('g2', 's2', 'b2', 'same-device')])
        self.assertEqual(out['components'], [['g1'], ['g2']])
        self.assertFalse(out['requires_holdout_split_protocol_review'])
        self.assertTrue(out['device_reuse_does_not_create_edges'])

    def test_multiple_components_and_singleton(self):
        out = self.connectivity([record('g3', 's3', 'b3'),
                                 record('g2', 's12', 'b2'),
                                 record('g1', 's12', 'b1'),
                                 record('g5', 's45', 'b5'),
                                 record('g4', 's45', 'b4')])
        self.assertEqual(out['components'], [['g1', 'g2'], ['g3'], ['g4', 'g5']])
        self.assertEqual(out['component_count'], 3)
        self.assertEqual(out['multi_group_component_count'], 2)
        self.assertEqual(out['largest_component_groups'], 2)

    def test_exact_duplicate_rows_do_not_change_connectivity(self):
        rows = [record('g1', 's1', 'b1'), record('g2', 's1', 'b2')]
        a = self.connectivity(rows)
        b = self.connectivity(rows + [rows[0].copy(), rows[1].copy()])
        self.assertEqual(a, b)

    def test_row_permutation_and_transitive_union_are_deterministic(self):
        rows = [record('g1', 's12', 'b1'), record('g2', 's12', 'b23'),
                record('g3', 's3', 'b23'), record('g4', 's4', 'b4')]
        self.assertEqual(self.connectivity(rows),
                         self.connectivity(list(reversed(rows))))

    def test_missing_session_column_is_explicitly_unverified(self):
        columns = ['group_id', 'exposure_batch_id', 'device_id']
        rows = [dict(group_id='g1', exposure_batch_id='b1', device_id='d1')]
        out = self.connectivity(rows, columns)
        self.assertFalse(out['checked'])
        self.assertIsNone(out['components'])
        self.assertIsNone(out['component_count'])
        self.assertIsNone(out['requires_holdout_split_protocol_review'])

    def test_missing_batch_column_is_explicitly_unverified(self):
        columns = ['group_id', 'session_id', 'device_id']
        rows = [dict(group_id='g1', session_id='s1', device_id='d1')]
        out = self.connectivity(rows, columns)
        self.assertFalse(out['checked'])
        self.assertIsNone(out['multi_group_component_count'])

    def test_arabic_identifiers_preserve_distinct_groups(self):
        rows = [record('مجموعة-١', 'جلسة-١', 'دفعة-١'),
                record('مجموعة-٢', 'جلسة-١', 'دفعة-٢'),
                record('مجموعة-٣', 'جلسة-٣', 'دفعة-٣')]
        out = self.connectivity(rows)
        self.assertEqual(out['components'],
                         [['مجموعة-١', 'مجموعة-٢'], ['مجموعة-٣']])
        self.assertFalse(out['not_physical_independence_evidence'] is False)

    def test_single_declared_group_is_not_independent_evidence(self):
        out = self.connectivity([record('g1', 's1', 'b1')])
        self.assertEqual(out['components'], [['g1']])
        self.assertEqual(out['component_count'], 1)
        self.assertEqual(out['multi_group_component_count'], 0)
        self.assertTrue(out['not_physical_independence_evidence'])

    def test_missing_columns_cannot_be_called_clean(self):
        columns = ['group_id', 'device_id']
        out = self.connectivity([dict(group_id='g1', device_id='d1'),
                                 dict(group_id='g2', device_id='d1')], columns)
        self.assertFalse(out['checked'])
        self.assertIsNone(out['requires_holdout_split_protocol_review'])
        self.assertTrue(out['not_physical_independence_evidence'])


if __name__ == '__main__':
    unittest.main()
