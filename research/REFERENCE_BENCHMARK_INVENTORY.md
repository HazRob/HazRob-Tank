# Reference Benchmark — verified inventory (2026-10-18)

**Status: PARTIAL INVENTORY.** The policy is exhaustive inclusion; this document is not a claim that all library sources have been discovered or ingested.

## Verified local raw source: UCI 1081

- Local user-provided archive: `gas+sensor+array+low-concentration.zip`; member `gsalc.csv`.
- Direct CSV parse: **90 rows**, each **9,002 columns** = chemical label + concentration label + 9,000 sensor-time values.
- Labels: acetone, ethanol, ethyl acetate, hexane, isopropanol, toluene.
- Concentrations: 50 ppb, 100 ppb, 200 ppb.
- Complete factorial grid: **6 substances × 3 concentrations × 5 repetitions = 90 labeled traces**.
- If 10 sensors × 900 time points per trace as specified in the pre-existing local benchmark report, then **810,000 scalar response readings**, **not 810,000 independent experiments**.
- Initial eligibility: all 90 traces qualify on known identity, known concentration and usable fingerprint format, subject to per-value quality control and source license verification. Source-level independence and device/session IDs are not established from this CSV alone.
- MQ135 is a sensor overlap, but other sensors in this dataset are not equivalent to HazRob Tank MQ inventory. Do not map incompatible sensors as if interchangeable.
- Prior local artifact `HazRob_UCI1081_Real_Benchmark.zip` contains existing subset benchmark results; its report warns that best-of-175 feature-subset selection on the same CV causes optimistic bias. Treat those scores as exploratory, **not** final held-out benchmark scores.

## Additional files found locally — pending data-level inventory

- `HazRob_ChemCore_Central_Library_HR069_2026-10-04.xlsx`: library workbook; determine which records are experimental labeled measurements versus reference curves.
- `HazRob_Report_000004_20261001_232420_١١٢٧٥٦.xlsx`: experiment report; inspect gas ground-truth identity and concentration before admitting.
- `HazRob_Dataset_Acquisition_2026-10-18.zip`: dataset acquisition manifest, not raw measurements. Lists MQ7_6400, UCI1081, UCI308, UCI360, Zenodo mixture and drift resources, among others; listed does not mean downloaded or eligible.
- `HazRob_Tank_ChemCore_Legacy_Research_2026-10-18.zip` and other research archives: assess source provenance and avoid treating synthetic tests as independently measured datasets.

## Next required steps

1. Inspect all available reference datasets and enumerate **all** eligible records, preserving all repetitions and concentrations; document unavailable downloads.
2. Verify channel ordering, response units, per-source R0/BME availability, labels and measurement quality.
3. Build immutable source manifests and a reproducible loader; log excluded rows with explicit reasons.
4. Implement grouped holdouts by source/session/device/experiment; no temporal or repeated-exposure leakage.
5. Run all compatible technique evaluations and publish per-technique metrics and limitations. No production changes.

**No comprehensive evaluation has yet been run in this inventory phase.**

## External primary-source discovery (2026-10-18; metadata only, NOT ingested)

- UCI 270: Gas Sensor Array Drift at Different Concentrations — **13,910 labeled measurements**, 16 sensors, 6 gases, variable known concentrations; DOI 10.24432/C5MK6M; https://archive.ics.uci.edu/dataset/270 . Note: overlaps UCI 224; deduplicate original experiments across dataset releases.
- UCI 322: Gas sensor array under dynamic gas mixtures — **4,178,504 time-stamped records**, 16 TGS sensors, ethylene/methane and ethylene/CO mixtures, concentration set points, ~12h per mixture; DOI 10.24432/C5WP4C; https://archive.ics.uci.edu/dataset/322 . These are NOT 4.18 million independent experiments. Different TGS types from HazRob MQ sensors.
- UCI 308: Gas sensor array under flow modulation — **58 time series**, 16 sensors, acetone/ethanol binary mixtures with concentrations; DOI 10.24432/C5BG7G; https://archive.ics.uci.edu/dataset/308 .
- UCI 224: Gas Sensor Array Drift Dataset — 13,910 records and 16 sensors; DOI 10.24432/C5RP6W; https://archive.ics.uci.edu/dataset/224 . Same original collection as UCI 270; **do not double count**.

Metadata counts and source links confirmed from UCI repository pages, not downloaded or quality-controlled in this turn. Dataset licensing and original paper citation obligations must be checked before redistribution. The GitHub organization repository search exposed HazRob-Tank but did not locate a distinct accessible reference-library repository; repository availability/access remains unresolved. Do not claim exhaustive inventory yet.

## External GitHub repository discovery — 2026-10-18 (not yet ingested)

- https://github.com/mismiklik-iff/E-Nose-Dataset-for-Multi-Component-Liquid-Condiment-Concentration-Prediction — reports 9,200 8-MOS sensor trials, 1,550 single-component and 7,650 mixtures. Labels are **liquid condiment mixture proportions**, NOT calibrated airborne gas ppm; eligible for composition/temporal research only, not gas-ppm benchmark without gas-phase ground truth.
- https://github.com/TakMashhido/Gas-Sensors-Measurements-Dataset — CSV reported 6,400 records, MQ2/3/5/6/7/8/135 and gas labels. **No verified per-record concentration field**; keep OUT of labeled-concentration benchmark until checked, but consider separate gas-classification benchmark.
- https://github.com/camberbatch/Breath_analysis — calibration and multi-component VOC measurements across two instruments; verify analyte concentration units, calibration and sample IDs before admission. Medical breath class labels alone do not establish chemical identity/concentration.
- https://github.com/anemo-labs/SmellDB — 32-channel odor classification resource with API-key-mediated download; verify concentration labels and access/licensing before admission.
- https://github.com/infochemistry-ai/digital-nose — README advertises sensor mixture data but download link is placeholder; **not a verified accessible dataset**.
- https://github.com/RonishNadar/electronic-nose — MQ acquisition and preprocessing code; do not count as labeled measurement corpus without verified released raw files.

**Strict counting:** A CSV row or timestamp is not automatically an independent experimental exposure. Duplicate releases and repeated exports must be cross-referenced. External dataset discovery does not imply download, licensing clearance, validation or measured compatibility with HazRob MQ hardware.
