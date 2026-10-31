# HazRob Tank V7 — R06 native beef PDF implementation and verification gate

**Date:** 2026-10-31; **Branch:** `feature/tank-v7-reports`.  
**Stage:** Source writer integrated, Android CI render pending. This is not an assertion of model accuracy or food safety.

## Implemented
- `android/app/src/main/java/com/hazrob/tank/foodfreshness/meat/V7BeefResearchPdfWriter.java` draws the R06 report using **Android PdfDocument**, shared official V7 logo/header/footer, RTL StaticLayout, black/amber/gray tables and 12pt minimum text. Its baseline is three A4 portrait pages, but overall owner policy permits more or fewer pages with the same mandatory scientific content.
- `android/app/src/main/java/com/hazrob/tank/foodfreshness/meat/BeefResearchPdf.java` retains the **original `save` method signature**, original `MediaStore Downloads/HazRobTank` destination and name, and delegates to the new writer. If output fails, the incomplete MediaStore URI is removed.
- **Page 1:** beef study only, real `result.validChannels`, actual passed sample count/duration and T/RH, 7 channel `Rs` readings from the passed map, all four reference-study labels with result distances and descriptive similarities. No microbiological or food-safety certification.
- **Page 2:** chart spaces for 7-MQ observed/source overlay and true 180-second sensor response stay **empty** since the `save` API does not carry compatible normalized vectors or time-series history; the four-class similarity display draws **only** the precomputed values in `BeefResearchEngine.Result`, with no own classification or threshold.
- **Page 3:** real **external TS5 research confusion matrix 4×4** faithfully transcribed from `research/food_freshness/meat/wijaya2018_5series_7mq_class_prototypes.json.holdout_results.confusion`: rows `excellent 43/317/0/0`, `good 0/540/0/0`, `acceptable 0/420/0/0`, `spoiled 0/115/359/366` (total 2160 reference records). Actual-source accuracy `43.9%`, balanced accuracy `38.9%` displayed as source provenance, **not Tank field validation**. Reserve external 36-hour study chart as blank since source time-series is not part of current PDF input.
- Article DOI displayed correctly `10.1016/j.dib.2018.11.091`; dataset DOI `10.17632/mwmhh766fc.3`. Existing `BeefResearchCatalog.STUDY_PAPER` wrong metadata remains a tracked technical debt until separate research reference correction, and not used by new report's citation text.
- No change to independent `BeefResearchEngine` chemical inference or food model; no gas ChemCore, no GHS/PPM, no calculated Tank TVC.

## Android QA added
- Fixture `research/chemcore_v6/tests/android/V7R01PdfFixtureActivity.java` uses the same `BeefResearchPdf.save` production facade, extracts a PDF from phone MediaStore and requires a separate source-research page. Synthetic inputs clearly identified in `status.json`.
- `research/chemcore_v6/tests/android/run_v7_r01_pdf_qa.sh` exports `V7_R06_Actual_Android_Report.pdf`.
- `.github/workflows/v7-r01-android-pdf-qa.yml` checks source labels seven MQ, class labels, DOI and TS5 cells, all 3 pages A4 portrait with text ≥12pt, renders per-page PNG and writes R06 QA manifest.
- **Do not declare QA PASS until latest execution at precise commit completes**. Former R01/R02 Android production QA passed, but R06 is a separate gate.

## Further actions
1. Inspect latest GitHub Actions job build and Python QA, fix failures, inspect rendered page images for overlapping figures/footers.
2. Test null readings result: `-` placeholders remain and bars are not drawn for missing class results.
3. Integrate genuine normalized overlay and 180-second raw series only when the real upstream output contains their source-provenanced data.
4. R03 and R04 still require full native PDF redesign; R05 needs 18 appropriate static images (including mixes) and native PDF redesign.
5. Owner visual signoff separate from CI QA; APK release not authorized as complete on this basis.

**Scientific disclaimer:** `excellent/good/acceptable/spoiled` are categories from a published beef dataset, not verified edibility of a Tank-tested sample.
