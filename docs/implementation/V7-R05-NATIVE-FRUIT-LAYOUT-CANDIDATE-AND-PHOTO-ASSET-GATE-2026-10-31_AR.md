# HazRob Tank V7 — R05 Native Fruit PDF candidate and photo asset gate

**Date:** 2026-10-31 | **Branch:** `feature/tank-v7-reports`  
**State:** `NATIVE_LAYOUT_CANDIDATE_IMPLEMENTED_NOT_YET_PRODUCTION_WIRED / IMAGE_ASSETS_PENDING`

## Implemented native Android layout (real code)
`android/app/src/main/java/com/hazrob/tank/foodfreshness/V7FruitReportPdfWriter.java` is a full printable Android `PdfDocument` candidate preserving the existing independent food-engine result object. Unlike manually built mockups it is executable and rendering-tested through an Android emulator fixture.

- Always displays the **18 selectable source families** in two portrait A4 **main matrix pages**, nine per page, each with source-family English and Arabic labels.
- Right-to-left first column reserves a **real reference fruit or composite mixture photograph**. The lookup contract is `android/app/src/main/assets/fruit_photos/<normalized_family>.webp`. Each asset will require accurate subject, rights/license/provenance and a print QA.
- **THE PHOTOS HAVE NOT BEEN PACKAGED YET.** Missing picture cells explicitly display `-`, not false photographs, emoji or images of inspected food; this remains a release-blocking deficiency for the owner's R05 photo requirement.
- Selected family alone may receive actual `result.rankedTemplates` values; all other family rows show `-` even when chart placeholders or reference labels exist. If `foodSelection` is empty/all, rankings for multiple families can be displayed only when the independent food engine actually returned them.
- Annex emits **all source matches** across dynamically added A4 pages, eight source records per page, with full source label, study day, descriptive similarity index, shape distance and overlap; **no Math.min(7)** or scientific results removed.
- Main pages reserve two different blank chart regions: an **8-MQ headspace fingerprint** and the **external study-day D1–D5 reference comparison**. Time-series was never passed into PDF, so source-less chart areas remain blank.
- All text uses `V7ReportLayoutKit` (RTL shaping, 12pt, official black/amber footer/header); missing numbers `-`, no gas ChemCore, GHS, ppm or edibility classification.
- The existing production `FoodFreshnessPdf.save` remains unchanged until 18 actual photos and visual QA are complete. This prevents misrepresenting the current reference image slots as a finished user-approved report.

## Android QA
- The existing Android fixture now calls `V7FruitReportPdfWriter.write` directly (not the original FoodFreshnessPdf.save) for an explicitly synthetic **Banana D3** example; requires two full-family main pages and a source-evidence annex.
- A **zero-readings** Banana run must still create the two family matrix pages with `-` in result cells, no simulated signal traces.
- The CI script extracts both `V7_R05_Fruit_18_Families_No_Photos_Yet.pdf` and `V7_R05_Fruit_No_Data_18_Families.pdf`.
- GitHub Actions checks all 18 English source families, A4 portrait, text >=12pt and generates PNG images/manifest. State becomes PASS only on successful exact commit run.

## Remaining release gates
1. Supply **18** accurately labeled, publishable reference photographs including suitable composite pictures for the eight mixture families, with usage licensing and provenance.
2. Review printed crop size `22×14mm` image in `25×18mm` photo cells, Arabic/English text and 18 family rows; ensure no overlap at 12pt.
3. Switch stable `FoodFreshnessPdf.save` to V7 native writer **only after image completeness** and actual Android rendering QA.
4. Verify selected one-family and all-family modes, incomplete MQ data, full evidence source appendix, and case-insensitive identifier normalization; further test studies' day labels are reference dates, not current Tank longitudinal observations.
5. No change to gas engine, fruit classification, food safety or microbial claims.

**Owner visual requirements:** approved 2026-10-30 23:16 UTC; dynamic page count flexibility approved later 23:36 UTC. Product release and visual signature are still pending.
