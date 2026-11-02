import pathlib
import unittest
ROOT=pathlib.Path(__file__).resolve().parents[3]
UI=(ROOT/"android/app/src/main/java/com/servoc/app/ui/MeasurementsTab.java").read_text()
PDF=(ROOT/"android/app/src/main/java/com/servoc/app/report/TankInstantReportPdf.java").read_text()
ENVELOPE=(ROOT/"android/app/src/main/java/com/servoc/app/chemcore/nextintegration/NextDecisionEnvelope.java").read_text()

class NextReadOnlyParityContract(unittest.TestCase):
    """Structural check; PDF opening and Arabic RTL require Android device tests."""
    def test_ui_uses_distinct_next_read_only_path(self):
        self.assertIn("isResearchOnlyNext(p)",UI)
        self.assertIn("renderResearchOnlyNext(p)",UI)
        self.assertIn('p.optString("decision_uid"',UI)
        self.assertIn('q.optLong("chemical_uid"',UI)
        self.assertIn("catalog.byUid(uid)",UI)
        self.assertIn('q.optInt("rank",i+1)',UI)
    def test_pdf_uses_canonical_stored_candidates_and_all_pages(self):
        self.assertIn("writeNext(out,telemetry,chemcore,id,timestamp)",PDF)
        self.assertIn('next.optString("decision_uid"',PDF)
        self.assertIn('next.optString("snapshot_sha256"',PDF)
        self.assertIn('next.optJSONArray("candidates")',PDF)
        self.assertIn("for(int offset=0;offset<count;offset+=perPage)",PDF)
        self.assertIn("rank!=i+1",PDF)
        self.assertIn('row.optLong("chemical_uid"',PDF)
        self.assertIn('capture.optInt("distinct_scan_cycles"',PDF)
    def test_no_display_inference(self):
        self.assertIn('candidate.put("ppm",JSONObject.NULL)',ENVELOPE)
        self.assertIn('out.put("gas_identity_confirmed",false)',ENVELOPE)
        self.assertIn("لا تعرض قيم ppm أو إنذارات",PDF)

if __name__=="__main__":unittest.main()
