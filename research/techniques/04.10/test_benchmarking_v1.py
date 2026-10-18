import unittest
from benchmarking_v1 import grouped_split,classification_metrics,compare_predictions
class BenchmarkTests(unittest.TestCase):
    def test_split(self):
        a,b=grouped_split([{"group":"s1","label":"A"},{"group":"s2","label":"B"}],holdout_groups=["s2"])
        self.assertEqual((len(a),len(b)),(1,1))
    def test_no_holdout(self):
        with self.assertRaises(ValueError):grouped_split([{"group":"s1","label":"A"}],holdout_groups=["s2"])
    def test_no_train(self):
        with self.assertRaises(ValueError):grouped_split([{"group":"s1","label":"A"}],holdout_groups=["s1"])
    def test_duplicates(self):
        with self.assertRaises(ValueError):grouped_split([{"group":"s1","label":"A"},{"group":"s2","label":"B"}],holdout_groups=["s2","s2"])
    def test_accuracy(self):self.assertEqual(classification_metrics(["A","B"],["A","B"])["accuracy_with_abstention_as_error"],1)
    def test_mistake(self):self.assertEqual(classification_metrics(["A","B"],["B","A"])["accuracy_with_abstention_as_error"],0)
    def test_coverage(self):self.assertEqual(classification_metrics(["A","B"],["A","ABSTAIN"])["coverage"],.5)
    def test_selective(self):self.assertEqual(classification_metrics(["A","B"],["A","ABSTAIN"])["selective_accuracy"],1)
    def test_all_abstain(self):self.assertIsNone(classification_metrics(["A","B"],["ABSTAIN","ABSTAIN"])["selective_accuracy"])
    def test_recall(self):self.assertEqual(classification_metrics(["A","A","B"],["A","B","B"])["per_class"]["A"]["recall"],.5)
    def test_precision(self):self.assertEqual(classification_metrics(["A","B"],["A","A"])["per_class"]["A"]["precision"],.5)
    def test_balanced(self):self.assertEqual(classification_metrics(["A","B"],["A","B"])["balanced_recall"],1)
    def test_confusion(self):self.assertEqual(classification_metrics(["A","B"],["A","B"])["confusion"]["A"]["A"],1)
    def test_compare(self):self.assertEqual(len(compare_predictions(["A"],{"m1":["A"],"m2":["B"]})),2)
    def test_bad_lengths(self):
        with self.assertRaises(ValueError):classification_metrics(["A"],["A","B"])
    def test_empty(self):
        with self.assertRaises(ValueError):classification_metrics([],[])
if __name__=="__main__":unittest.main()
