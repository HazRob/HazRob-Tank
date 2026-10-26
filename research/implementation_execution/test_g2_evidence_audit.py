import unittest
from g2_evidence_audit import audit_record, audit_all

class G2AuditTests(unittest.TestCase):
 def test_missing_source_fails_closed(self):
  self.assertEqual(audit_record({"id":"01.1"})["status"],"G2_BLOCKED")
 def test_synthetic_source_rejected(self):
  self.assertIn("NOT_INDEPENDENT_MEASURED_SOURCE",audit_record({"id":"01.1","source_kind":"SYNTHETIC"})["issues"])
 def test_group_overlap_rejected(self):
  self.assertIn("GROUP_LEAKAGE",audit_record({"id":"01.1","train_group_ids":["session1"],"test_group_ids":["session1"]})["issues"])
 def test_no_test_tuning(self):
  self.assertIn("TEST_SET_SELECTION_LEAKAGE_OR_UNKNOWN",audit_record({"id":"01.1","hyperparameters_selected_on_test":True})["issues"])
 def test_80_missing_sources_all_blocked(self):
  ids=[str(i) for i in range(80)]
  a=audit_all(ids,[])
  self.assertEqual(len(a),80)
  self.assertTrue(all(x["status"]=="G2_BLOCKED" for x in a))
 def test_complete_source_candidate_not_automatic_approval(self):
  from g2_evidence_audit import REQUIRED
  item={k:"sha-or-value" for k in REQUIRED}
  item.update(id="01.1",source_kind="INDEPENDENT_MEASURED",train_group_ids=["a"],test_group_ids=["b"],hyperparameters_selected_on_test=False,blind_holdout=True,measurement_units="Rs/R0",confidence_interval=[.1,.2],source_raw_data_accessible=True)
  self.assertEqual(audit_record(item)["status"],"G2_PASS_CANDIDATE_REQUIRES_REVIEW")
if __name__=="__main__": unittest.main()
