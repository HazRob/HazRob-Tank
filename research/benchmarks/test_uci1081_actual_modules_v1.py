"""Local, source-independent guards for the UCI1081 reproducible benchmark."""
import unittest
import uci1081_actual_modules_v1 as m

class ProtocolTests(unittest.TestCase):
    def synthetic_rows(self):
        return [
            {"gas":g,"ppb":c,"replicate_ordinal":k}
            for g in m.GASES for c in m.PPB for k in range(5)
        ]

    def test_90(self):
        self.assertEqual(len(self.synthetic_rows()),90)

    def test_replicate_heldouts(self):
        s=m.splits(self.synthetic_rows(),"replicate_ordinal_proxy")
        self.assertEqual(len(s),5)
        self.assertEqual([len(v["test"]) for v in s],[18]*5)

    def test_concentration_heldouts(self):
        s=m.splits(self.synthetic_rows(),"leave_one_concentration_ppb")
        self.assertEqual(len(s),3)
        self.assertEqual([len(v["test"]) for v in s],[30]*3)

    def test_train_test_no_overlap(self):
        for mode in ("replicate_ordinal_proxy","leave_one_concentration_ppb"):
            for split in m.splits(self.synthetic_rows(),mode):
                self.assertFalse(set(split["train"]) & set(split["test"]))

    def test_train_all_six_classes(self):
        r=self.synthetic_rows()
        for split in m.splits(r,"leave_one_concentration_ppb"):
            self.assertEqual({r[i]["gas"] for i in split["train"]},set(m.GASES))

    def test_unknown_mode(self):
        with self.assertRaises(ValueError):
            m.splits(self.synthetic_rows(),"random_casual")

    def test_late_features(self):
        traces=tuple(tuple([float(j)]*900) for j in range(10))
        self.assertEqual(m.features(traces,"late10"),[float(j) for j in range(10)])

    def test_delta_features(self):
        traces=tuple(tuple([1.]*300+[3.]*600) for _ in range(10))
        self.assertEqual(m.features(traces,"early_late_delta20"),[2.]*20)

    def test_nonconforming_time(self):
        with self.assertRaises(ValueError):
            m.features([tuple([1.]*899)]*10,"late10")

    def test_unrecognized_feature(self):
        with self.assertRaises(ValueError):
            m.features([tuple([1.]*900)]*10,"invalid")

    def test_all_correct(self):
        data=[(g,g,50.0,0) for g in m.GASES]
        v=m.summarize(data)
        self.assertEqual(v["accuracy"],1)
        self.assertEqual(v["macro_recall"],1)

    def test_all_wrong(self):
        data=[(g,m.GASES[(i+1)%6],50.0,0) for i,g in enumerate(m.GASES)]
        v=m.summarize(data)
        self.assertEqual(v["accuracy"],0)
        self.assertEqual(v["macro_f1"],0)

    def test_macro_f1_imbalanced(self):
        data=[("ethanol","ethanol",50,0),("ethanol","ethanol",50,0),
              ("acetone","ethanol",50,0)]
        v=m.summarize(data)
        self.assertLess(v["macro_f1"],1.0)

if __name__=="__main__":
    unittest.main()
