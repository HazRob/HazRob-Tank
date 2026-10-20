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

## Source-file and schema verification — 2026-10-18

| Source | Directly checked | Result | Benchmark eligibility |
|---|---|---|---|
| TakMashhido/Gas-Sensors-Measurements-Dataset | GitHub CSV file view, 6401 lines (header + 6400 observations), 252 KB | Headers: Serial Number, MQ2, MQ3, MQ5, MQ6, MQ7, MQ8, MQ135, Gas. No concentration field. | Gas-class-only (gas labels include Smoke, Perfume, Mixture, NoGas; not necessarily single chemical identities). NOT eligible for known-analyte-known-ppm test. |
| camberbatch/Breath_analysis | Repository README and named raw CSV files | MS_1_calibration_data.csv n-heptane, propan-1-ol, ethylbenzene; Chapter 4 two-system calibration-transfer files for n-heptane, propan-1-ol, o-xylene; Chapter 3 breath-study patient labels (118 participants). | Calibration/mixed VOC files are CANDIDATES pending raw concentration/units verification. Medical-study rows NOT known-analyte-known-concentration samples. |
| anemo-labs/SmellDB | Repository README | 3000 labelled samples, 32 sensor channels, 12 semantic odor classes, 115 s/sample, API-first download. | Odor classification only unless molecular identities and concentration labels separately documented; NOT known-ppm benchmark. |
| mismiklik-iff/E-Nose-Dataset-for-Multi-Component-Liquid-Condiment-Concentration-Prediction | Raw files could not be accessed in this inspection | Reported liquid composition dataset, gas ppm not established. | Pending raw access; exclude from quantified gas-concentration benchmark. |

**Verification boundary:** GitHub file page and README inspection only for external sources. This pass did NOT download the 6400-row MQ CSV or the breath-analysis CSVs into the local benchmark, and did NOT execute numerical ChemCore tests. A source that has no concentration field is never assigned invented ppm. No source is yet certified as a HazRob Tank device-equivalent calibration source.

## Local starter bundle constructed — 2026-10-18

Read actual `HazRob_ChemCore_Central_Library_HR069_2026-10-04.xlsx` with artifact_tool. Verified 43 research sensor/analyte curve rows; 16 fingerprint **catalog descriptions** (not 16 independently measured fingerprint vectors); 24 stable-material status rows; and the separately preserved UCI1081 90 measured exposures (810,000 scalar readings). Built `HazRob_Tank_Benchmark_Starter_2026-10-18.zip` locally with UCI1081 raw CSV, sample index, curve/catalog/status extracts, manifest and evidence separation. The source workbook is a catalog/synthesis, not a substitute for original organization JSON fingerprint payloads. Report 000004 has 8 MQ summary readings but explicitly says 'No matching recorded time-series'; no verified known analyte/concentration, so not added to the known-identity benchmark. No numeric ChemCore evaluation run in this step.

## UCI1081 primary-source audit and real external-module execution — 2026-10-19

The original UCI 1081 release confirms: (1) CC BY 4.0 attribution license; (2) complete sensor order **TGS2603, TGS2630, TGS813, TGS822, MQ135, MQ137, MQ138, 2M012, VOCS-P, 2SH12**; (3) strictly sensor-major 900 samples/channel, 1 Hz, baseline 300 s then injection 600 s; (4) 90 exposure rows, 6 gases × 3 levels **50/100/200 ppb** × 5 repeats. Primary: https://archive.ics.uci.edu/dataset/1081/gas+sensor+array+low-concentration, DOI https://doi.org/10.24432/C5CK6F, cite original creators Tian, Zhao & Deng (2023). User-provided original archive SHA256 **813ebb7d00ed9172114b68d27ab3213170d6c220b8b23907dce91e9838cd48ab** checked against separately downloaded official ZIP in GitHub Actions.

Actual research modules 02.2,02.4,02.7,03.1,03.2,03.5,04.2,04.3,04.4,04.6 executed on this external corpus across two successful immutable GitHub Actions runs. See [detailed Arabic report](G2_REAL_DATA_BATCH01_AR.md), [classification run](https://github.com/HazRob/HazRob-Tank/actions/runs/39180196732), [temporal/feature diagnostics](https://github.com/HazRob/HazRob-Tank/actions/runs/39180489713). Source CSV not committed to Tank repo by these runs. Original **response measurement units not conclusively documented** in the distributed CSV; never assume Rs, R0, voltage or ADC. Replicate ranks are NOT demonstrated independent acquisition days/sessions. External reference data do **not** establish Tank MQ8 transfer accuracy or G3 qualification. Existing older inventory notes that license/source verification was pending are superseded for UCI1081 specifically; other dataset licenses remain separate.

## UCI270 ORIGINAL SOURCE INGESTION VERIFIED (2026-10-19)

- Primary <https://archive.ics.uci.edu/dataset/270/gas+sensor+array+drift+dataset+at+different+concentrations>; DOI 10.24432/C5MK6M. Downloaded original `gas+sensor+array+drift+dataset+at+different+concentrations.zip` directly in GitHub Actions and verified SHA256 **98fe3a30981a222dd4518fbcc3dddd45d5c0ce9b03ef6dc6fe5cf7a04cfbff5e**.
- Exactly **13,910 experimental exposure records in 10 .dat files** with source batch counts **445,1244,1586,161,197,2300,3613,294,470,3600**; six gas class labels plus independently provided known concentration in ppmv; **128 engineered features per record**, representing **16 physical source sensors × 8 derived signal descriptors**, not 128 individual sensors and not raw time series.
- Original source text defines 1=Ethanol;2=Ethylene;3=Ammonia;4=Acetaldehyde;5=Acetone;6=Toluene. Preserve raw class-coded labels and concentration; no renaming of batch IDs as devices. GitHub Actions printed class counts by batch and verified original source SHA.
- **Do not double count UCI224**: it describes the same original 13,910 experiments, and UCI270 adds concentration labels. Earlier UCI270 listing as “metadata only, not ingested” is now superseded for this source by the actual SHA-pinned measured research runs.
- Scientific G2-04: eight original research techniques executed on measured source values; 07.2 blocked because only one physical source device; 07.10 run analytically but NOT physically profiled. Full methods and metrics: [G2_REAL_DATA_BATCH04_AR.md](G2_REAL_DATA_BATCH04_AR.md), raw-run evidence [#39201607716](https://github.com/HazRob/HazRob-Tank/actions/runs/39201607716), compact [JSON](benchmarks/results/uci270_g2_batch04_2026-10-19.json).
- **Licensing warning:** UCI webpage now claims CC BY 4.0, while donor description still says research-only/no commercial. Perform research only; verify rights before redistribution or commercialization. Do not commit source archive raw bytes.
- Across distinct UCI1081 and UCI270 original sources now **90 + 13,910 = 14,000 measured exposure records**, BUT sensor geometries/calibrations differ and NONE prove HazRob Tank 8×MQ gas identification, sensitivity or safety. G3 remains zero.

## Verified third external source — UCI308 flow-modulated acetone/ethanol mixtures (2026-10-20)

- Primary original https://archive.ics.uci.edu/dataset/308/gas+sensor+array+under+flow+modulation ; DOI **10.24432/C5BG7G**; dataset by Ziyatdinov & Fonollosa, CC BY 4.0.
- Exact official ZIP SHA256 **7b062960dbef1a9e8aefc62bc7dbac09bdadb8cbb40793cf2b8e122da1862f90**, verified twice on GitHub Actions.
- ZIP members: `features.csv` (58 exposure rows, 439 columns = 7 metadata and 432 derived sensor features); `rawdata.csv.gz` (928 sensor rows, 7509 columns = 9 metadata and 7500 time values); **58 distinct laboratory exposures** × 16 sensors × 7500 samples at 25 Hz = **6,960,000 raw scalar readings**, NOT millions of independent exposures. Five day/time measurement batches, not five devices. Twelve source mixture/pure/air classes.
- Critically, `ace_conc` and `eth_conc` are **liquid mixture prep vol.% (dilutions in water)**, NOT headspace gas concentration ppm; all 05.4/05.5/05.6/05.7 true gas ppm regression claims are blocked without external gas standards.
- `dR` is a measured source response, not a verified Tank MQ `log(Rs/R0)` ratio. The G2-05 02.6 3D trajectory computation uses these real numbers in a numeric-only way and is explicitly ineligible for chemical validation.
- Five source-batch-held-out runs of 08.8/08.9/08.10 use true pure and mixture exposure labels, with ALL feature scaling and pure endmember signatures fit within training batches. **Poor negative result:** 08.8 at untuned fixed 0.5 residual threshold: **6/20 mixture flags (30% sensitivity)** and **16/30 pure false alarms (46.7% specificity)**; 08.9/08.10 produced 20 numerical fits each with coefficients lacking validated gas mixture proportions/ppm.
- [UCI308 source schema inspection](https://github.com/HazRob/HazRob-Tank/actions/runs/39435016417); [G2-05 run](https://github.com/HazRob/HazRob-Tank/actions/runs/39435979553); [full report](G2_REAL_DATA_BATCH05_AR.md). Original raw archive not committed to repository; SHA and scripts enable reproducibility.
- **Three nonoverlapping external source collections now reviewed:** UCI1081 (90), UCI270 (13910), UCI308 (58) = **14,058 distinct source exposure records**; do not recount the original UCI224 copy of UCI270. These are NOT 14,058 Tank 8-MQ exposures.

