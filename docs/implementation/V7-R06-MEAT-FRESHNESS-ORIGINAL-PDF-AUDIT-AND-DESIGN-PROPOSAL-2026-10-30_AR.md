# HazRob Tank V7 — R06 | تدقيق التقرير الأصلي للحوم والتصميم البديل المقترح
**Document ID:** `V7-R06-MEAT-REPORT-REVIEW-001`  
**Date:** `2026-10-30 / user review 23:20 UTC`  
**Status:** `SOURCE_AND_ORIGINAL_PDF_AUDITED; DESIGN_PROPOSAL_PENDING_OWNER`  
**Repo/branch:** `HazRob/HazRob-Tank @ feature/tank-v7-reports`.  
**Governance:** `HZT-V7-RPT-STD-001 rev1.2 / G01–G18`; `HZT-V7-RPT-LAYOUT-001` (DESIGN-FIRST, `-`, empty plots). **No production changes to Android/Meat Engine; do not imply APK release.**

## 1. Evidence and boundaries
- Actual original Android V6 sample file: `06_V6_Meat_Freshness_Original.pdf`.
- `SHA256 = 053d78db6133e2b519146ddd7ad85335d39c74a202fccbc47494d246319d2366`; `size=213267 bytes`; PDF v1.4 Skia, `1` page, A4 Portrait `595×842pt`, no embedded raster images, vector shapes.
- Reviewed using `pdfinfo`, `pdftotext -layout`, PyMuPDF extraction, `pdftoppm -r110` page render and manual view. The sample is **synthetic CI test fixture** `CI-V6-MEAT`, not actual Tank validation or an identified food specimen.
- Font measures: `54 nonblank spans`, `48/54 (<12pt) = 88.9%`, minimum `8pt`; font histogram `8pt:4, 9pt:11, 10pt:10, 11pt:23, 16pt:6`. A4 single page without official mark/footer.
- **Actual source writer:** `android/app/src/main/java/com/hazrob/tank/foodfreshness/meat/BeefResearchPdf.java` (`BeefResearchPdf.save`), source Android `Canvas.drawText` at fixed pixel positions and fixed 595×842 page.
- **Actual independent algorithm:** `android/app/src/main/java/com/hazrob/tank/foodfreshness/meat/BeefResearchEngine.java`; catalog `BeefResearchCatalog.java`; parsed research prototype `research/food_freshness/meat/wijaya2018_5series_7mq_class_prototypes.json`, source manifest.
- **Data source external verification:** [Mendeley Data version 3](https://data.mendeley.com/datasets/mwmhh766fc/3), `DOI 10.17632/mwmhh766fc.3`; study article [PubMed PMID 30547068](https://pubmed.ncbi.nlm.nih.gov/30547068/), correct `DOI 10.1016/j.dib.2018.11.091`. Data license `CC BY 4.0`.
- This is a **food-meat research-only branch** distinct from Fruit R05 and gas `ChemCore`; the food source is **beef only**, not generalized to lamb, chicken or other meats.

## 2. Original page inventory (observed)
| Original area | What is genuinely shown in the original | Design problem |
| --- | --- | --- |
| Title/top-left | English `HAZROB TANK | BEEF HEADSPACE RESEARCH`, subordinate study note | No owner geometric logo, no Arabic, no black header, no authentic capture timestamp |
| Acquisition facts | Report ID, 180.0s/179 snapshots synthetic, source split TS1–TS4 training/TS5 holdout, selected 7 MQ, T/RH | Long English sentences at `9–10pt`, no labeled cards/timestamps/data trace |
| Seven MQ readings | Raw values MQ2, MQ3, MQ4, MQ5, MQ8, MQ9, MQ135 | Plain list, no unit or condition column, no reference fingerprint, no normalized vs raw distinction |
| Class comparison | Four study class names `excellent`, `good`, `acceptable`, `spoiled`; rows count 1320/2280/1680/3360, distance, similarity/100 | Labels can be misconstrued as proven food safety grades; score has no precision/uncertainty chart, labels truncated elsewhere |
| One plot | Four blue bars `Research similarity chart`; labels clipped `excell, accept, spoile` | Plots same values as rows; no source-prototype overlay, no current 180s MQ plot; axes and semantics under-explained |
| Source limitations/footer | Heldout acc 43.9%, balanced acc 38.9%; 3-minute scan vs study 36-hour series; no actual TVC, DOI | Important scientific caveats condensed into `8–9pt`; no footer, provenance link or distinction source vs observed |
| Printed identity | White background + blue line/bar | Violates HazRob black/amber/gray/white palette, table borders, bilingual, pagination |

## 3. Engineering and scientific traceability findings
**A. Different observation periods:** The Mendeley repository has `5 time series × 2160 min`, corresponding to five beef cuts studied over `36 h` each. The Tank operation is `180s` and records a fingerprint at one session. If a plot shows the `36h` reference timeline, it must be explicitly labelled **external historical study series**, never a measurement of present Tank beef.

**B. Feature scope:** Source and Android engine intersect at **7** sensor models `MQ2, MQ3, MQ4, MQ5, MQ8, MQ9, MQ135` (Tank MQ7 is **not in the seven sensor reference overlap**), uses `centered log10 Rs` shape. Reference resistances are in `Ω`; label axes/units and whether a plotted series is `raw Rs` vs `normalized centered-log`; source-on-device transfer is unvalidated.

**C. Class labels:** Four `excellent/good/acceptable/spoiled` categories are **study labels** assigned using original data including TVC; current Tank has **no TVC assay or food-safety certification**. Class similarity `100/(1+distance)` is a descriptive similarity index **not probability of edibility**.

**D. Validation honesty:** The project's own split `TS1–TS4 training = 8640 min records` and `TS5 holdout = 2160 min records` reports `accuracy 0.43935185` and `balanced accuracy 0.38878968` — these are **research one-cut holdout performance**, not 180-second performance on independent field food samples, and are inadequate to assert any certified quality decision.

**E. Original source/reference DOI defect (verified externally):**
- Source Android `BeefResearchCatalog.STUDY_PAPER` and `research/food_freshness/meat/wijaya2018_5series_7mq_class_prototypes.json.provenance.original_paper_doi` incorrectly contain `10.1016/j.dib.2018.11.075`.
- Authoritative article DOI is `10.1016/j.dib.2018.11.091` (PubMed PMID 30547068). The erroneous `.075` DOI belongs to an unrelated bloodstain-pattern article (PubMed PMID 30815519).
- **Document only now.** Correct catalog/JSON source metadata in later authorized functional/data integration batch, not during layout review. The dataset DOI `10.17632/mwmhh766fc.3` in current PDF is **correct**.

**F. Data already in UI but not supplied to PDF:** `FoodFreshnessTab` retains `signalHistory` per MQ during 180s, but `BeefResearchPdf.save` takes **only** averaged channel readings, sample counts/duration, result, T/RH and report ID. Any future 180s plot must be bound from authentic captured series **later**, not synthesized from averages.

**G. No gas-family crossover:** No GHS/OSHA occupational limit/chemical ppm, and no 80-technique ChemCore flow should be inserted into a food-beef research similarity report. The source data has TVC **as reference-study labels**, not a field TVC measurement.

## 4. G01–G18 assessment against ORIGINAL V6 PDF, not V7 mockups
| G | State | Reason |
| --- | --- | --- |
| G01 | FAIL | Original official owner hex mark absent |
| G02 | FAIL | Navy-blue chart theme instead of approved four-color set |
| G03 | FAIL | 48/54 text spans below 12pt; missing proper Arabic RTL |
| G04 | NOT_APPLICABLE | GHS/OEL refers to gas chemical hazards, not food class similarity |
| G05 | PARTIAL | Class table and class bar chart duplicate meaning; could be complementary with interpretation but currently add little |
| G06 | NOT_APPLICABLE | Physical threshold gauges are not a requirement for food-beef reference similarity; T/RH metadata is appropriate |
| G07 | PARTIAL | Source classes and validity caveats appear but are not clearly visually distinguished from actual food-grade verdict |
| G08 | PARTIAL | One legitimate bar chart, but no comparative sensor fingerprint or actual 180s MQ signal graph |
| G09 | NOT_APPLICABLE | Standalone meat FoodFreshness model is not ChemCore gas and should not claim executed gas techniques |
| G10 | FAIL | Entire PDF English-only |
| G11 | FAIL | One fixed page, dense unsized sections, no adaptive handling if extra source material is displayed |
| G12 | FAIL | MQ and four classes are plain lines, not amber header, black bordered tables |
| G13 | FAIL | Bars not distinguished from probability/food-safety verdict; no nuanced plots with source-vs-observed status |
| G14 | PASS_BASELINE | PDF does not compute new class, microbiology or threshold; only renders engine output and numeric bar lengths |
| G15 | PARTIAL | White body, but nonconforming header, text/plot colors |
| G16 | FAIL | Report ID generated at completion is not authentic capture start-to-end metadata |
| G17 | FAIL | No black standardized header/logo right/AR-EN amber title centered/time left |
| G18 | FAIL | No pale-gray footer, 14pt Arabic page number left / wordmark right |

**Counts:** `10 FAIL / 4 PARTIAL / 3 NOT_APPLICABLE / 1 PASS_BASELINE`. No implication of engine precision or device validation.

## 5. Proposed report redesign — TWO portrait A4 design pages + expandable evidence annex (not yet approved)
**Reference style:** `210×297mm` page, `5mm` margins, `18mm` black Header `y=5..23`, `11mm` gray Footer `y=281..292`. Body `y=27..277`. All text including labels ≥`12pt`; AR-first bilingual; official geometrical mark upper right, headline `فحص بصمة اللحوم — Beef Research Headspace` middle `15pt Bold #FFBE00`, authentic capture start-end/placeholder `-` upper left. Table headings amber `#FFBE00`, black bold with fully black borders, `2mm ×1mm` cell padding, 1.15 line spacing.

### Page B-01 — identity, seven MQ, four published source labels
| Slot | Target y-range (mm) | The reserved design elements |
| --- | --- | --- |
| Header | `5–23` | Owner logo, title, measurement time location |
| Acquisition card | `27–60` | Beef sample label, process ID, start/end `-`, actual duration `-`, number of frames `-`, 7 MQ coverage `-`, temperature `-`, humidity `-`, device/model source |
| 7 sensor table | `65–165` | MQ2/MQ3/MQ4/MQ5/MQ8/MQ9/MQ135 each with sensor ID, raw Rs + unit, normalized shape if engine provides, data-quality/source flag. No data fields populated in design mockup |
| Four-class reference table | `170–267` | **4 rows only as actual study reference classes**, Arabic/English `ممتازة / جيدة / مقبولة / فاسدة (فئة الدراسة)`, research distance, descriptive similarity index, training reference rows, source overlap and row-level status (where provided). An optional mini horizontal blank bar region for class index. No colored edibility verdict |
| Interpretation line | `267–277` | concise `These are research source labels, not a Tank microbiological test` (Arabic first, wrap if needed) |
| Footer | `281–292` | `صفحة 1 من 2` and two-tone brand |

The page **does not list four different meats**: there is ONE food type, beef; the four rows are four **research classes**, not four measured specimens. If owner prefers images, reserve **one illustrative beef-sample photo** in the acquisition card, clearly labelled as generic reference—not one fabricated photograph per class.

### Page B-02 — three complementary chart slots + study provenance
| Slot | Target y-range (mm) | Chart/source explanation |
| --- | --- | --- |
| Sensor fingerprint overlay | `27–104` | **Observed vs Source Class Fingerprint** on 7 MQ channels in the **same centered-log shape feature space**, plot area currently empty |
| Actual 180-second sensor response | `108–185` | Real Tank MQ sensor response over `180s` on the x-axis (not study 36 h), currently only the **reserved chart grid**; source history is not available to writer yet |
| Four-class similarity comparison | `189–242` | **Horizontal Lollipop/Bullet or Dot Plot** for descriptive similarity across four reference classes; all four class names untruncated. Empty plot with legend and units now |
| Source and methodological caveats | `246–277` | Model origin, train/holdout split, evidence status, benchmark accuracy fields `-` in blank mockup, original DOI plus correct article DOI; caveat that 3-min scan ≠ 36h, measured TVC `-`, no certified edibility |
| Footer | `281–292` | `صفحة 2 من 2` right company mark and left page text |

**Layout caveat:** 12pt bilingual text inside crowded chart areas may require expanding the figure across a new page. Two pages are the **recommended basic design target**, not a scientific data or print page cap. Full `source class confusion matrix (4×4)`, source 36-hour time-series (clearly referenced as external), and additional executed measurements could expand into annexes **only after owner design approval**.

### Why three distinct graphics?
1. **Seven-MQ reference overlay:** differences in **multisensor fingerprint**; distinct from the table with raw values.
2. **180s MQ temporal response:** stability and actual response during the robot measurement; distinct from source 36-hour timeline.
3. **Four-class ranked comparison:** closeness of the single Tank observation to source class templates **not a freshness % or probabilistic safety output**.
4. **Optional source research comparison:** a study-only `TVC × source class` if the owner wants to show reference intervals in a separately titled area, never as a measured current-sample TVC.

## 6. No-data design rules
- **All absent value cells `-`**, including T/RH, duration, number of sensors, similarity, raw Rs, model benchmark when not populated, and capture timestamps. `0` is reserved for an actual documented zero.
- **All plotted data regions remain empty** with only labels/axes/grid/legend placeholders. Do not draw sample bars for four classes, colored spoilage levels, a 36-hour timeline for Tank, fake TVC, gas ppm or limits.
- **Four research class labels may appear** as fixed category names, but all comparison results remain `-` until data hookup. No graph/status color that implies a food-safety decision before engine evidence.
- Static data-source citations and method identity may be displayed as **bibliographic text**, clearly separated from measurement fields; publication DOI is static, not a fabricated measurement.
- No arbitrary report truncation of source outputs when functional data integration begins; use expandable pages rather than <12pt or shortened trace IDs.

## 7. Later functional tasks — recorded but NOT authorized
- Shared `ReportThemeV7`, RTL table renderer, real measurement time source, traceable report record and page counters.
- Add `signalHistory` to the **document input/view model** only if backed by authentic readings; preserve seven MQ sensor coverage.
- Show actual engine `rankedClasses`, all four classes and the reference overlay; never calculate new food quality gates or TVC in PDF.
- Correct erroneous `STUDY_PAPER` and JSON provenance DOI to `.091`, preserving original dataset DOI `10.17632/mwmhh766fc.3` and provenance history.
- QA of Arabic/English text ≥12pt, no clipping, 7 MQ rows, four classes, missing numbers `-`, independent beef-only mode, no red/green fabricated edibility, old Android writer unchanged until approval.

## 8. R06 design choices needing owner approval
- **R06-D01:** Two A4 portrait pages baseline as §5: (1) test + seven MQ + four study classes, (2) graphs + research limitations; auto-expand only when needed?
- **R06-D02:** Separate three diagrams `7MQ Observed-v-Reference`, `180s Actual MQ Temporal`, `4-Class Similarity Ranking`, all blank during design?
- **R06-D03:** All four class rows always visible, no conditional omission; selected/closest class visually outlined amber **but not green/red safety verdict**, and each row has a small reserved similarity indicator?
- **R06-D04:** Reserve **one generic meat sample image** for identification (optional), rather than four condition images that falsely imply visual diagnosis?
- **R06-D05:** Strong scientific provenance box with DOI `10.17632/mwmhh766fc.3` and article DOI `10.1016/j.dib.2018.11.091`, protocol mismatch, source accuracy 43.9/38.9 field slots, and no Tank TVC?
- **R06-D06:** Use the universal V7 no-readings protocol `-`/blank chart areas, preserve full AR-first bilingual theme?
- **R06-D07:** Should an **additional appendix page** reserve a `4×4` study held-out confusion matrix and a **study-only 36h timeline**? We propose an appendix only if owner wants research-depth reporting, without mistaking source values for Tank data.

**State:** `REVIEW_AND_DESIGN_PROPOSAL_ONLY`; do not mark owner approved and do not modify `BeefResearchPdf.java`, `BeefResearchEngine.java`, `BeefResearchCatalog.java`, or APK.
