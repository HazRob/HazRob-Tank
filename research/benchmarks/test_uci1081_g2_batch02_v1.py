"""Synthetic invariant tests. Real-data integration runs separately on UCI1081."""
import unittest
import uci1081_g2_batch02_v1 as b
from uci1081_actual_modules_v1 import SENSORS,GASES,PPB

def synthetic():
    rows=[]
    for gi,g in enumerate(GASES):
        for c in PPB:
            for rep in range(5):
                rows.append({"gas":g,"ppb":c,"replicate_ordinal":rep,
                             "source_row":len(rows)})
    late=[[gi*3.+c/100.+rep*.04+j*.2+(gi*j*.01) for j in range(10)]
          for gi,g in enumerate(GASES) for c in PPB for rep in range(5)]
    delta=[[v*(1.+phase*.01) for phase in (0,1) for v in a] for a in late]
    return rows,late,delta

class G2Batch02Tests(unittest.TestCase):
    def test_fixed_target_ten(self):
        self.assertEqual(len(b.IDS),10)
        self.assertEqual(len(set(b.IDS)),10)
    def test_source_schema_not_tank(self):
        r=b.external_sensor_schema_gate()
        self.assertEqual(r["status"],"BLOCKED_INCOMPATIBLE_SENSOR_SCHEMA")
        self.assertEqual(r["tested_real_MQ8_vectors"],0)
    def test_logit_order(self):
        model={"features":2,"mean":[1,1],"scale":[2,4],
               "weights":[[1.,1.,0.],[-1.,0.,2.]]}
        self.assertEqual(b.fit_softmax_logits(model,[[3,5]]),[[2.,1.]])
    def test_logit_wrong_dimension(self):
        with self.assertRaises(ValueError):
            b.fit_softmax_logits({"features":2,"mean":[0,0],"scale":[1,1],
                                 "weights":[[1.,0.,0.]]},[[3.]])
    def test_synthetic_fold_diagnostics_and_benchmark(self):
        rows,late,delta=synthetic()
        output=b.train_only_subset_and_overlap(rows,late,delta)
        self.assertEqual(len(output["03.3_03.6_03.8_fold_diagnostics"]),3)
        for item in output["03.3_03.6_03.8_fold_diagnostics"]:
            self.assertEqual(item["03.6_n_subset_candidates_train_only"],55)
            self.assertEqual(item["n_test_exposures"],30)
        self.assertEqual(
            output["04.10_nested_subset_vs_full_sensor_metrics"]["all10_knn3"]["n"],90)
        self.assertEqual(output["07.3_holdout_test_counts"],[30,30,30])
    def test_synthetic_temperature_calibration_splits(self):
        rows,late,delta=synthetic()
        result=b.heldout_logit_calibration(rows,delta)
        self.assertEqual(len(result),3)
        self.assertTrue(all(r["n_model_train"]==48 and
                            r["n_temperature_calibration"]==12 and
                            r["n_unseen_ppb_test"]==30 for r in result))
        self.assertTrue(all(r["selected_temperature"]>0 for r in result))
    def test_synthetic_unknown_gas_splits(self):
        rows,late,delta=synthetic()
        result=b.leave_one_gas_unknown(rows,delta)
        self.assertEqual(len(result),6)
        self.assertEqual({x["gas_left_as_unknown"] for x in result},set(GASES))
        self.assertTrue(all(x["known_train_n"]==60 and x["heldout_known_n"]==15 and
                            x["heldout_unknown_n"]==15 for x in result))
    def test_ppb_to_ppm(self):
        rows,late,delta=synthetic()
        self.assertEqual(sorted({r["ppb"]/1000 for r in rows}),[.05,.1,.2])

if __name__=="__main__":
    unittest.main()
