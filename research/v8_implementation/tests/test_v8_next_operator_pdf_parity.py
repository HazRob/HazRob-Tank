import pathlib
import unittest
ROOT=pathlib.Path(__file__).resolve().parents[3]
UI=(ROOT/"android/app/src/main/java/com/servoc/app/ui/MeasurementsTab.java").read_text()
PDF=(ROOT/"android/app/src/main/java/com/servoc/app/report/TankInstantReportPdf.java").read_text()
BRANDED=(ROOT/"android/app/src/main/java/com/servoc/app/report/TankInstantV7PdfWriter.java").read_text()
ENVELOPE=(ROOT/"android/app/src/main/java/com/servoc/app/chemcore/nextintegration/NextDecisionEnvelope.java").read_text()

class NextReadOnlyParityContract(unittest.TestCase):
    """Static source contract; actual PDF opening and RTL checked separately."""
    def test_ui_preserves_original_decision_and_reference_supplements(self):
        for key in ('isResearchOnlyNext(p)', 'renderResearchOnlyNext(p)',
                    'p.optString("decision_uid"', 'q.optLong("chemical_uid"',
                    'catalog.byUid(uid)', 'p.optJSONObject("scientific_reference_screening")',
                    'p.optJSONObject("scientific_source_diagnostics")'):
            self.assertIn(key,UI)
    def test_original_pdf_identity_and_branding(self):
        self.assertIn('TankInstantV7PdfWriter.writeNext(out,telemetry,chemcore,id,timestamp)',PDF)
        for key in ('drawOfficialLogo', 'mainPage(pdf,telemetry,presentation',
                    'detailPage(pdf,curves', 'GhsPictograms.drawOnPdf',
                    'risk.optJSONArray("comparisons")', 'header(c,', 'footer(c,'):
            self.assertIn(key,BRANDED)
    def test_all_reference_math_not_suppressed_in_report(self):
        self.assertIn('o.optDouble("estimated_ppm"',BRANDED)
        self.assertIn('q.optDouble("percent_of_reference"',BRANDED)
        self.assertIn('curves.length()',BRANDED)
        self.assertIn('comparisons.length()',BRANDED)
        self.assertIn('ghss.length()',BRANDED)
    def test_next_does_not_claim_certified_ppm(self):
        self.assertIn('out.put("gas_identity_confirmed",false)',ENVELOPE)
        self.assertIn('candidate.put("ppm",JSONObject.NULL)',ENVELOPE)

if __name__=="__main__":
    unittest.main()
