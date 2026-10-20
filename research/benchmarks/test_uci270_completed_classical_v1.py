import unittest
from uci270_completed_classical_v1 import prepare,score,SCENARIOS,prepare
from uci1081_completed_classical_v1 import METHODS

class UCI270ChronologicalTests(unittest.TestCase):
    def setUp(self):
        self.rows=[
            {"batch":b,"source_row":i,"gas":g,"features":[0.]*128}
            for i,(b,g) in enumerate(((1,"ethanol"),(2,"toluene"),(5,"ethanol"),
                (10,"ethanol"),(10,"toluene")))]
    def test_fixed_comparators(self):
        self.assertEqual(len(METHODS),7)
    def test_two_chronological_scenarios(self):
        self.assertEqual(SCENARIOS,((1,2),(1,2,3,4,5)))
    def test_split_train_two_target(self):
        tr,te=prepare(self.rows,(1,2))
        self.assertEqual([r["batch"] for r in tr],[1,2])
        self.assertEqual(len(te),2)
    def test_no_target_leakage(self):
        tr,te=prepare(self.rows,(1,2,5))
        self.assertFalse({r["source_row"] for r in tr}&{r["source_row"] for r in te})
    def test_target_in_training_rejected(self):
        with self.assertRaises(ValueError):
            prepare(self.rows,(1,10))
    def test_empty_source_rejected(self):
        with self.assertRaises(ValueError):
            prepare(self.rows,(7,))
    def test_empty_target_rejected(self):
        with self.assertRaises(ValueError):
            prepare(self.rows,(1,2),target_batch=8)
    def test_metric_perfect(self):
        s=score(["ethanol","toluene"],["ethanol","toluene"])
        self.assertAlmostEqual(s["accuracy"],1)
    def test_metric_wrong(self):
        s=score(["ethanol","toluene"],["toluene","ethanol"])
        self.assertAlmostEqual(s["accuracy"],0)
    def test_metric_n(self):
        s=score(["ethanol","toluene"],["ethanol","ethanol"])
        self.assertEqual(s["n_test"],2)

if __name__=="__main__":unittest.main()
