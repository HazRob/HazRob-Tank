# CURRENT CHECKPOINT — 2026-10-19: G2-04 TEN-TECHNIQUE UCI270 CHRONOLOGICAL DRIFT EVIDENCE COMPLETE

**SUMMARY AND TRUTH CONDITIONS:** Initial G1 remains 80/80 unit gates PASS (1,261 original software cases); G2 has now reviewed **40 distinct technique IDs** across FOUR ten-ID batches. Of these, **28** had an operation on authentic external-source experimental records or their metadata (G2-04: seven numeric sensor-derived operations plus 07.1 source temporal metadata split), **11** cannot be scientifically exercised due absent physical/source fields or multiple devices (G2-04: 07.2), and **1** is theoretical resource arithmetic only (G2-04: 07.10). This G2 reviewed count is NOT 40 validated operational algorithms. **Tank MQ8 G3 independently calibrated scientific validation = 0/80.** No APK/ESP32 firmware/production ChemCore modifications.

**SOURCE AND DEDUP:** UCI270 DOI 10.24432/C5MK6M, SHA256 98fe3a30981a222dd4518fbcc3dddd45d5c0ce9b03ef6dc6fe5cf7a04cfbff5e, 13,910 authentic exposure rows across 10 chronological batches, 128 PRECOMPUTED attributes from 16 sensors, NOT 128 sensors or raw trace data. Original concentration ppbv? NO — source uses ppmv; UCI1081 uses ppb. UCI224 duplicates these 13,910 rows and MUST NOT be added. Source UCI270 current CC BY 4.0 metadata conflicts with description of research-only/no commercial; research use only, raw archive not committed. Two distinct sources tracked so far: UCI1081 90 + UCI270 13,910 = 14,000 exposure rows (not Tank).

**G2-04 VERIFIED RUN:** https://github.com/HazRob/HazRob-Tank/actions/runs/39201607716, SHA 8e065677c3c5653185b99847759f94baae223583, 12/12 protocol/invariant tests PASS, canonical dataset downloaded+SHA verified, full benchmark completed. Archived output https://github.com/HazRob/HazRob-Tank/actions/runs/39201607716/artifacts/12163638882; durable compact JSON research/benchmarks/results/uci270_g2_batch04_2026-10-19.json and Arabic methods/results research/G2_REAL_DATA_BATCH04_AR.md. Prior failed #39201362612 was a small synthetic-fixture/source-count assertion, fixed without relaxing full original-data inventory validation; final run succeeded.

**TEN IDS:** 03.4 LDA/PLS1DA binary only, tested on held-out batch10 619/1200 (51.6%) vs 332/1200 (27.7%); 07.1 source ten-batch folds (NOT independent sessions; metadata-only); 07.2 BLOCKED only one physical source device; 07.4 matched acetone200ppm batch shift flags 80% on 50 reference heldouts (does not prove cause); 07.5 matching offset reduces mean centroid distance 6.7065→4.8057 in 50 separate target reference holdouts, NOT classification transfer; 07.6 unsupervised matching centroid gap .8421→.5593 (not classification); 07.7 labelled head with 60 target samples improves 923/3540→978/3540 (26.1→27.6%), not neural fine tuning; 07.8 ranks 300 externally measured candidates with UNCALIBRATED distance pseudo-probabilities, no experimental active learning; 07.9 sequential update labels used only AFTER temporal batch evaluation, frozen 4327/13465 vs updated 4980/13465 (not monotonic by batch), 565 audit updates; 07.10 448-byte theoretical head only (no MCU profile).

**REAL SCIENTIFIC RESTRICTIONS:** Do not equate batch with device/session; fit transforms on source/calibration not future holdout; changing gas and concentration distributions confound drift estimates; all 80 G3=NOT_VALIDATED; research-only. G1 software prototype success does not imply trained CNN/GNN or functional detection in hazardous environments.

**NEXT G2-05 TEN UNREVIEWED IDS PROPOSED:** 02.6, 02.8, 05.4, 05.5, 05.6, 05.7, 08.8, 08.9, 08.10, 08.11. FIRST assess UCI308 mixture raw/features and UCI309 turbulent gas mixtures, archive hashes/licensing and true concentration labels. UCI308 target vol.% are dilutions in water, NOT certified headspace ppm; UCI309 gas source release settings are NOT necessarily measured ambient ppm. Properly BLOCK techniques requiring absent mixture ppm or Tank MQ8 R0/BME rather than invent. Use 10-ID batch report, verify current PROGRESS_80.json for ID dedup, and run code with source-specific grouping. Do not modify production.

**RECOVERY ORDER:** research/RESUME.md top → research/PROGRESS_80.json → research/G2_REAL_DATA_BATCH04_AR.md → research/benchmarks/results/uci270_g2_batch04_2026-10-19.json → CI #39201607716 → earlier G2/technical logs. Keep failed run and negative findings, never overwrite earlier checkpoint history.

---

# CURRENT CHECKPOINT — 2026-10-19: G2-03 TEN-TECHNIQUE PHYSICAL EVIDENCE AUDIT COMPLETE (1 MEASURED, 9 BLOCKED)

**VERIFIED CURRENT STATE:** G1 baseline continues at 80/80 software prototypes, 1261 passing unit cases; new G2 external-source three successive batches of ten = **30 distinct technique IDs reviewed**, **20 executed against actual measured external-array values**, **10 blocked on missing device-dependent physical input data**. All measured data is UCI1081, 90 distinct exposure records, not 30×90 new experiments. G3 Tank MQ8 scientific instrument validation **0/80**; no APK/firmware/production ChemCore modifications. Canonical machine counts in research/PROGRESS_80.json.

**BATCH G2-03:** 01.1–01.7,02.1,02.3,05.1; CI https://github.com/HazRob/HazRob-Tank/actions/runs/39184775420 tested SHA 51b3973e0cad6c6dd85b6d155330c64a2c4f04dc = SUCCESS; 13/13 eligibility/unit tests PASS, actual UCI1081 download+SHA VERIFIED, 02.1 generic numeric three causal filters run on ALL 900 measured sensor-exposure traces (90×10; 810000 points). Nine physical MQ techniques BLOCKED from experimental scoring due missing verified ADC codes and divider rail/load, measured Rs/R0, heater phase and/or BME; source raw signal units unknown. No invented resistances, voltages, gas calibration or ppm predictions. Full Arabic scientific results: research/G2_REAL_DATA_BATCH03_AR.md; permanent machine results: research/benchmarks/results/uci1081_g2_batch03_2026-10-19.json; workflow script research/benchmarks/uci1081_g2_batch03_v1.py, 13 tests research/benchmarks/test_uci1081_g2_batch03_v1.py. GitHub artifact https://github.com/HazRob/HazRob-Tank/actions/runs/39184775420/artifacts/12157448158.

**REAL NUMERIC EVIDENCE 02.1:** median baseline MAD source units raw .0025000; causal median5 .0020000 (20% smaller), mean5 .0017500 (30% smaller), EMA(alpha .25) .0015967178 (36.1% smaller). Median halfstep lag relative unfiltered: 3s/3s/5s (869/876/876 valid channel-window pairs). This is generic source signal smoothing only; NOT HazRob Tank chemical sensitivity, latency guarantee or safety improvement.

**NEXT PRIORITY:** Acquire genuine Tank source sessions with MQ8 channel mapping and measured electrical quantities (RL, Vc, ADC code calibration, Rs/R0 per sensor, gas standards, heater voltage-phase especially MQ7/MQ9, BME temperature/RH/pressure and exact acquisition timestamps); then re-open nine blocked techniques. In parallel source/inspect UCI270/224 drift and UCI308 mixtures; verify provenance, licensure, deduplicate overlaps and check independent batch/device/session grouping. If doing next 10-technique scientific batch, choose G2-04 ten *unreviewed* eligible IDs and explicitly separate executed external-sensor evidence from blocked Tank-specific techniques. Report in groups of exactly ten; preserve G3=0 until actual validated controlled Tank reference-gas measurements.

**RECOVERY ORDER:** this top checkpoint → research/PROGRESS_80.json → research/G2_REAL_DATA_BATCH03_AR.md → research/benchmarks/results/uci1081_g2_batch03_2026-10-19.json → CI run above and source SHA. Prior G2-02 and G2-01 checkpoint histories follow below; avoid overwriting past negative findings.

---

# CURRENT CHECKPOINT — 2026-10-19: G2-02 TEN-TECHNIQUE REFERENCE BATCH COMPLETE (9 EXECUTED, 1 BLOCKED)

**CANONICAL RECORD:** research/PROGRESS_80.json (G1 80/80, 1261 old unit tests unchanged; G2 2 batches reviewed 20 IDs; 19 distinct techniques exercised to at least external-data metric/algorithm level; 02.5 BLOCKED; G3 Tank device scientific validations 0/80). Detailed Arabic batch report research/G2_REAL_DATA_BATCH02_AR.md, committed numeric JSON research/benchmarks/results/uci1081_g2_batch02_2026-10-19.json. Preserve prior G2-01 in research/G2_REAL_DATA_BATCH01_AR.md.

**MEASURED DATA PROVENANCE:** Original authentic UCI1081 external ten-sensor gas arrays: 90 independent labelled EXPOSURE RECORDS, not 810000 independent experiments; original SHA256 813ebb7d00ed9172114b68d27ab3213170d6c220b8b23907dce91e9838cd48ab; 6 gases, 50/100/200 ppb (convert to 0.05/0.1/0.2 ppm for code requiring ppm); 5 replicates per gas×level; no verifiable independent days/devices or original Tank MQ8 Rs/R0/BME. CC BY 4.0, DOI 10.24432/C5CK6F.

**CI EVIDENCE:** Final successful GitHub Actions https://github.com/HazRob/HazRob-Tank/actions/runs/39182059102 on SHA 9d50bc34813fe868b7d3e2eecfd3c39459d782fd; source archive SHA confirmed, eight synthetic protocol/integration tests PASS (8/8); actual module evaluation on original measured data PASS; full artifact https://github.com/HazRob/HazRob-Tank/actions/runs/39182059102/artifacts/12156686219. Modules called with real UCI data: 03.3 class-overlap, 03.6 subset selection, 03.7 within-gas concentration correlation, 03.8 identifiability/refusal, 04.10 grouped by concentration *proxy* plus common metrics, 05.9 held-out ppm split & concentration-BLIND geometric mean benchmark (NO ppm predictor), 06.3 separate 48 model / 12 calibration / 30 test temperature scaling, 06.5 leave-one-gas-out open-set, 07.3 held-out full concentration levels. 02.5 refused external array because requires exact eight measured MQ channels; properly BLOCKED, not given fabricated data.

**KEY NEGATIVE NUMERIC EVIDENCE:** 03.6 selects 55 single/two-sensor subsets within each of three training folds (not 175 arbitrary test-picked sensor combinations); KNN accuracy selected subset 48/90 vs all 10 channels 59/90. 03.8 threshold 0.5 standardized distance yields 87/90 INSUFFICIENT_EVIDENCE and 3/90 SINGLE_REFERENCE_CLASS, 0 ambiguous; this is arbitrary coverage and not a physical identification limit. 06.3 heldout 50/100/200 ppb NLL before→after temperature: 1.3594→2.8116, 0.4828→0.6756, 0.1888→0.1888; NO overall improvement. 06.5 rejects only 11/90 gas-class-withheld unknown exposures = 12.22%, not reliable open-set recognition. 05.9 zero error at middle 0.10 ppm arises trivially from geometric mean of training levels (0.05 and 0.20), NOT ppm inference; endpoints fail. All results exploratory, no Tank G3 validation or safety claims.

**RECOVERY ORDER:** this checkpoint → research/PROGRESS_80.json → research/G2_REAL_DATA_BATCH02_AR.md → research/benchmarks/results/uci1081_g2_batch02_2026-10-19.json → GitHub CI run (confirm commit SHA). The two benchmark scripts and tests are under research/benchmarks. G1 original 1261 unit tests and 80/80 completion remains unchanged, do NOT conflate with these extra eight integration tests.

**NEXT RESEARCH BATCH G2-03 (10 candidate technique IDs, pending data feasibility):** 01.1,01.2,01.3,01.4,01.5,01.6,01.7,02.1,02.3,05.1. Audit each against source data eligibility and hardware physics first; where source does not contain ADC topology/RL/R0/heater/BME, mark BLOCKED_NEEDS_TANK_REFERENCE and do not silently transform arbitrary analog voltage into MQ Rs or ppm. For independent real-data reinforcement, investigate UCI270 drift dataset and UCI308 mixture dataset with exact source/license, dedupe UCI224 overlap, session/device groups and sensor compatibility. Keep ten-item report policy: one user summary after complete audit of ten, with tested vs blocked counts, and retain negative findings.

**SCOPE GUARD:** no Android app APK, ESP32 firmware, production ChemCore, original HazRob code, chemical hazard alarm, instrument calibration or deployment approval modified.

---

# CURRENT CHECKPOINT — 2026-10-19: G2 REAL MEASUREMENT RESEARCH BATCH 01 COMPLETE (10 MODULES)

**COMPLETED:** Original G1 record remains 80/80 software-unit gates PASS (1261 tests). NEW G2-01 measured external-array evidence successfully executes **ten actual research modules**: 02.2, 02.4, 02.7, 03.1, 03.2, 03.5, 04.2, 04.3, 04.4, 04.6. Data UCI1081 90 authentic gas exposures, 810000 sensor signal scalars, 6 gas identities, 50/100/200 ppb, 10 external sensor types ×900 s at 1Hz, split 300s baseline +600s exposure; official CC BY 4.0 DOI 10.24432/C5CK6F; original archive SHA256 813ebb7d00ed9172114b68d27ab3213170d6c220b8b23907dce91e9838cd48ab.

**PROOF:** classification models (actual technique 02.2/04.2/04.3/04.4/04.6) GitHub Actions https://github.com/HazRob/HazRob-Tank/actions/runs/39180196732, commit 94a04c5365ee833f08d78e27e1da66d3588acccc, 13/13 adapter tests PASS, valid canonical source ZIP download and SHA, 20 evaluated configurations using 2 fixed signal representations × 2 fold protocols × 5 classifiers. Concentration-holdout at 50/100/200 ppb: on early+late delta20, 04.4 softmax 73/90 (81.11%); 04.2 reference fingerprint 59/90; 04.3 KNN 48/90; 04.6 forest 36/90; baseline 15/90. This is source-specific exploratory evidence, NOT Tank MQ8 validation.

Diagnostics (actual techniques 02.2/02.4/02.7/03.1/03.2/03.5) GitHub Actions https://github.com/HazRob/HazRob-Tank/actions/runs/39180489713 on commit d3ca81ca61571e47e7067bd1d034a7b41dfacd23; 900/900 temporal sensor-exposure windows executed, 510 undefined recoveries; feature selection, sensor rankings, PCA and heldout similarity computed with train-only fits; 03.2 correct 14/30,21/30,24/30 across three heldout ppb levels. See research/G2_REAL_DATA_BATCH01_AR.md and research/benchmarks/results/*.json (two permanent summary files).

**STRICT STATUS:** G2 external sensor data evidence exists for 10 techniques; G3 Tank-device independently calibrated scientific validation remains 0/80. Source sensor schema includes only MQ135 overlap, no independently verified source session/device IDs, no actual MQ8 R0/BME/heater metadata. No APK, firmware or production ChemCore modification. Earlier scripts and benchmark cases remain PROTOTYPE/PARTIAL_PROTOTYPE, no safety claims. Machine register research/PROGRESS_80.json contains per-technique reference evidence without overwriting empirical_gate.

**NEXT TARGET (G2-02 — 10 candidate techniques):** 02.5,03.3,03.6,03.7,03.8,04.10,05.9,06.3,06.5,07.3. First verify each technique's semantics, data/sensor compatibility and source group labels. Run reproducible grouped or concentration-level holds on real external records; for 02.5 Tank-only MQ8 fingerprints require Tank measurements (mark blocked rather than fabricate channels). For 05.9/06.3/06.5 assess legitimate calibration and OOD data requirements; do not assign a completed empirical gate without independent hardware data. Continue acquisition/inventory for UCI270 drift and UCI308 mixtures subject to research-only licenses. Document and report only once 10-topic batch is finished.

**RECOVERY ORDER:** read this top checkpoint → research/PROGRESS_80.json → research/G2_REAL_DATA_BATCH01_AR.md → research/REFERENCE_BENCHMARK_INVENTORY.md → research/EXECUTION_PLAN_80_AR.md. Check latest CI SHA before amendments. Preserve previous checkpoints and failures.

---

# CURRENT CHECKPOINT 2026-10-19 — ALL 80 RESEARCH PROTOTYPES G1 PASS; G2/G3 NOT COMPLETE

Verified 8×10 techniques on independent, clean GitHub Actions jobs per technique: **80/80 G1 software-unit gates**, exactly **1261 passing unit cases**, source refs in research/PROGRESS_80.json and extensive research/BATCH_RESULTS_80.md. Last batch08 [08.10,08.11,08.12,09.1–09.6,10.1] final run https://github.com/HazRob/HazRob-Tank/actions/runs/39179061733 at commit a2a52c7ca2d42f4a770d3a0a742728b08cadb53e; 10/10 jobs success, 172/172 tests. Fixed 08.10,08.12,09.2,09.5,09.6,10.1 input validation and added substantive tests (10.1 now 25 tests). No Android, ESP32, production ChemCore modifications.

**CRUCIAL: G1 completion is NOT completion of 80 scientifically operational techniques.** Many Stage08/09 modules are toy primitives, not trained neural/robotic models. **G3 real-gas Tank-device validation = 0/80.** UCI1081 surrogate 90 exposures with source sensor arrays cannot validate HazRob MQ8 hardware, and lacks BME, R0 and device/group metadata; no certification or hazard clearance. Science statuses (PROTOTYPE/PARTIAL_PROTOTYPE) intentionally unchanged.

NEXT PHASE: G2/G3 research work, not another G1-only loop. (1) Re-audit source provenance/licenses and actual raw fingerprints, calibrations, manufacturer curves, MQ8 R0 BME heater-phase; (2) create a common evidence adapter with sensor IDs, units, group/session/device IDs, known-gas labels/concentrations and source SHA; (3) implement leak-proof train/test splitting and independent surrogate real-data baselines for compatible techniques, mark unsupported appropriately; (4) progress module integration and scientifically replace toy stubs for MLP/CNN/TCN/GNN/mixtures with trained baselines only where sufficient real labels; (5) measure false identity/ppm/open-set errors and uncertainty per gas; (6) gain true Tank hardware measurements before promoting RESEARCH_VALIDATED. Read EXECUTION_PLAN_80_AR.md, PROGRESS_80.json, BATCH_RESULTS_80.md and STATUS_LEDGER.md before work. No production edits without explicit separate approval.

---

# CURRENT CHECKPOINT 2026-10-19 — BATCH 07 G1 PASS (70/80)

Verified 70/80 research software G1 gates and 1089 actual unit assertions/cases across seven independently linked CI batch runs, ZERO G3 independent Tank gas reference validation. Batch07 07.10 and 08.1–08.9, 10 isolated jobs, 162/162 test cases PASS on GitHub Actions https://github.com/HazRob/HazRob-Tank/actions/runs/39178553937 commit 3868943da762d42f623669b0997db1eff7b0917e. Added behavioral regression tests to all ten (prior smoke-only) and repaired validation faults 07.10,08.3,08.8 and nondivisor endmember omission/compute budget in 08.9. Note 08.1–08.9 are untrained primitives/synthetic models, NOT complete trained CNN/TCN/LSTM/GNN or validated unmixers. Source details research/BATCH_RESULTS_80.md and research/PROGRESS_80.json.

NEXT batch08 FINAL: 08.10,08.11,08.12,09.1,09.2,09.3,09.4,09.5,09.6,10.1. Existing 10.1 already has synthetic tests; inspect actual smoke tests for 08.10–09.6 and augment. Require 10 isolated real CI jobs, preserve G1 vs G3, append reports/progress/ledger/checkpoint. Then prioritize architecture integration and grouped real dataset evaluation, NOT claim eighty full scientific techniques are built. No production changes.

---

# CURRENT CHECKPOINT 2026-10-19 — BATCH 06 G1 VERIFIED COMPLETE (60/80)

Canonical plan: research/EXECUTION_PLAN_80_AR.md; progress: research/PROGRESS_80.json; per-technique evidence: research/BATCH_RESULTS_80.md; statuses: research/STATUS_LEDGER.md. Batch06 IDs 06.9,07.1–07.9, CI https://github.com/HazRob/HazRob-Tank/actions/runs/39178053177 on exact SHA 3d2aa344016408fb03714b5f39e000f08e8df488: 10/10 jobs SUCCESS, 171/171 behavioral unit tests from individual CI logs. Previously superficial test suites received new substantive cases; repaired bool count misuse (07.1/07.2), fitted-model validation (07.4/07.5/07.6), boolean concentration/weight misuse (07.8). Cumulative G1 60/80, 927 actual tests. G3 chemically validated 0/80. No production changes.

NEXT batch07 IDs: 07.10,08.1,08.2,08.3,08.4,08.5,08.6,08.7,08.8,08.9. Inspect actual implementation, not just names: 08.* files may be toy primitives, NOT trained neural CNN/TCN/GRU/GNN, NOT credible mixture deconvolution without proper reference data. Replace smoke-only tests with substantive behavioral suites, fix demonstrable bugs, run all ten in independent CI jobs, record exact job count and success, then update four canonical documents above. Do not claim G3 success or change production firmware/APK/ChemCore.

---

# CURRENT CHECKPOINT 2026-10-19 — BATCH 05 G1 COMPLETE (50/80)

Plan research/EXECUTION_PLAN_80_AR.md; machine state research/PROGRESS_80.json; full evidence research/BATCH_RESULTS_80.md. 50/80 G1 verified research software prototypes, 756 actual passing unit cases across completed batch runs. Batch05 05.8/05.9/06.1–06.8 final CI https://github.com/HazRob/HazRob-Tank/actions/runs/39177070616 commit 1bfd90c6aaa7f53c094f1bae2702d990cb7e0234, 10 jobs, 158/158 tests. 06.6/06.7/06.8 had previous superficial 3/2/2 export-only smoke tests; replaced with meaningful 15/16/15 cases; corrected malformed detection-reference validation and SHAP parameter validation. ZERO G3 independently validated. No production alterations.

NEXT batch06 = 06.9,07.1,07.2,07.3,07.4,07.5,07.6,07.7,07.8,07.9. Its existing tests may be smoke/export-only: inspect and add boundary/behavior regression tests, do NOT mark a smoke-only CI success as robust G1. Check test coverage + data/sensor constraints; run 10 isolated CI and record result, then update all registers. Stage07 cross-device/drift validation requires independent devices/sessions and BME/ambient logs not yet available.

---

# CURRENT CHECKPOINT 2026-10-19 — BATCH 04 G1 COMPLETE (40/80)

40/80 research techniques CI-verified software unit PASS; 598 executed tests; none G3 scientifically validated. Batch04 04.8/04.9/04.10/05.1–05.7 first CI https://github.com/HazRob/HazRob-Tank/actions/runs/39176503631 FAILED on 04.9 invalid curve, fixed malformed-reference rejection vs legitimate out-of-range masking. Earlier 04.9 syntax error fixed before first CI. Final CI https://github.com/HazRob/HazRob-Tank/actions/runs/39176571203 on b86a045775047a437de1547e1fbed6f81b7eed60 success (10/10 jobs ×16 tests =160). Source, data/empirical gaps: research/BATCH_RESULTS_80.md; machine register: research/PROGRESS_80.json; Arabic gates/plan: research/EXECUTION_PLAN_80_AR.md. No production modifications.

NEXT BATCH 05 IDs 05.8,05.9,06.1,06.2,06.3,06.4,06.5,06.6,06.7,06.8. Confirm partial model scope, inspect source/tests, run 10 clean CI jobs, fix real failures, archive accurate counts. G2/G3 remains separate, 05 ppm calibration requires real known-gas standards.

---

# CURRENT CHECKPOINT 2026-10-19 — BATCH 03 G1 COMPLETE

30/80 techniques pass reproducible isolated G1 software tests; 438 unit tests total; ZERO G3 real-gas/device validations. Batch03 03.6–03.8,04.1–04.7 initially FAILED CI #39176204448 due a preexisting missing closing parenthesis in 04.1; fixed in commit fa693d4d3678caf50c1b39fda7a14f4176909b95. Final #39176274539 PASS 10 jobs/160 tests. Evidence and science/data limitations: research/BATCH_RESULTS_80.md; canonical progress: research/PROGRESS_80.json; plan: research/EXECUTION_PLAN_80_AR.md.

NEXT batch04: 04.8,04.9,04.10,05.1,05.2,05.3,05.4,05.5,05.6,05.7. Audit source and test method, execute isolated G1 per technique, document pending G2/G3, never describe simplified 04.7 stumps as real XGBoost. No production changes.

---

# CHECKPOINT CURRENT — 2026-10-19: BATCH 02 G1 COMPLETE (20 of 80 verified)

See research/EXECUTION_PLAN_80_AR.md and research/PROGRESS_80.json (canonical machine progress), research/BATCH_RESULTS_80.md (per-technique evidence). Batch02 02.4,02.5,02.6,02.7,02.8,03.1,03.2,03.3,03.4,03.5: CI https://github.com/HazRob/HazRob-Tank/actions/runs/39175957251 tested commit faa4e3f4a41ea0ab149096b46925e9c7532d3f02; 10/10 independently isolated jobs PASS; exact logs 16+14+16+14+14+14+16+16+14+14=148 tests PASS. Cumulative first two batches: 20/80 technique G1 gates, 278 tests. G3 empirical validation remains 0/80; 03.4 LDA/PLS-DA remains binary-only PARTIAL_PROTOTYPE. Dataset UCI1081 is not Tank MQ8 and lacks verified sessions, R0, timebase and BME; no calibration or safety assertions. No production modifications.

NEXT BATCH 03: 03.6,03.7,03.8,04.1,04.2,04.3,04.4,04.5,04.6,04.7. Inspect source/test/README and mathematically identifiable weaknesses; fix actual bugs with regressions; run ten isolated jobs and obtain test counts from GitHub CI; update progress, ledger, batch results and this RESUME. Keep G1/G2/G3 separate.

---

# CHECKPOINT CURRENT — 2026-10-19 (Yemen): 80-technique scientific plan / batch 01 G1 COMPLETE

Canonical plan: research/EXECUTION_PLAN_80_AR.md (8×10 batches, G0 design / G1 executable unit tests / G2 data / G3 independent scientific validation). Machine progress: research/PROGRESS_80.json. Accumulated evidence: research/BATCH_RESULTS_80.md. Original 80-technique IDs validated against STATUS_LEDGER.md; MASTER_PLAN.md reconciled to 80.

BATCH 01 (01.1–01.7, 02.1–02.3) independently verified in GitHub Actions https://github.com/HazRob/HazRob-Tank/actions/runs/39175394866 (tested commit b33230ce56a75a93c25a81c124b3757ea801fe32): all 10 jobs and their real test steps SUCCESS; precise log counts 11+11+17+12+12+13+12+15+13+14 = 130 PASS, 0 FAIL. Fixed actual 01.3 inverse extrapolation wrong-end segment and added four regression cases; default extrapolation remains forbidden.

G2 local read-only check of user-provided UCI1081 archive: 90 labeled exposures, 810000 finite scalars, 90 unique records, 18 gas×concentration categories with 5 reps, 0 nonnumeric/NaN/Inf, file SHA256 813ebb7d00ed9172114b68d27ab3213170d6c220b8b23907dce91e9838cd48ab. Script research/benchmarks/uci1081_source_qc_v1.py. Missing calibration R0/RL/BME/heater metadata prevents full real-data validation for first ten. ZERO techniques have completed G3 in this program.

NEXT: BATCH 02 IDs 02.4, 02.5, 02.6, 02.7, 02.8, 03.1, 03.2, 03.3, 03.4, 03.5. Read source/test/README; repair mathematically demonstrable bugs with regression tests; run all 10 in independent CI jobs, examine per-job counts; update ledger, progress JSON, batch result log and this checkpoint. Do NOT alter production ChemCore, ESP32 firmware, APK, or HazRob original. Do not claim real validation solely from tests.

---

# Verified CI batch 01 — 2026-10-18

Techniques 01.1–01.5: GitHub Actions run https://github.com/HazRob/HazRob-Tank/actions/runs/39135356937 on main SHA 5a6e4382f4612215390bae9dccfe446c486b2e0e completed SUCCESS. All five independent jobs succeeded, including their 'Execute technique tests' step. This establishes clean-checkout software-unit-test PASS for these five only, NOT Tank hardware, gas calibration, or external-reference benchmark validation. Next batch 01.6, 01.7, 02.1, 02.2, 02.3. No production changes.

# Checkpoint 2026-10-18: 09.5 spatiotemporal fusion and 09.6 motion decisions PARTIAL_PROTOTYPE minimal research baselines committed, README and smoke tests authored, not verified executed. 09.6 returns operator-review intent only, never motor commands. Roadmap through 09.6 has research artifacts, but NOT completed scientific validation. NEXT audit full 79-technique status ledger, run clean-checkout tests, assess evidence/quality gaps, then controlled real-world validation. No production changes.

# Checkpoint 2026-10-18: Batch 08.12–09.4 PARTIAL_PROTOTYPE minimal research baselines committed: complex product similarity, plume encounters, gas grid mapping, spatial response centroid and waypoint ranking. All five have README and synthetic smoke tests authored, not verified run. No validated chemical composition, source location, safe autonomous navigation or production changes. NEXT 09.5 spatiotemporal fusion.

# Checkpoint 2026-10-18: Batch 08.12–09.4 PARTIAL_PROTOTYPE research-only minimal baselines committed: complex product fingerprint similarity, odor encounter intervals, spatial gas grid, signal-weighted spatial centroid, and uncertainty/risk waypoint ranking (NO motion control). README and synthetic smoke tests authored, not verified executed. NEXT 09.5 spatiotemporal fusion. No production changes.

# Checkpoint 2026-10-18: Batch 08.7–08.11 PARTIAL_PROTOTYPE minimal research baselines committed: sim/real feature mean gap, single-signature residual, nonnegative two-component unmixing, multianalyte coefficient optimization, and paired interferent effect. README and synthetic smoke tests authored, not verified executed. No real Tank validation, no concentration accuracy claims. NEXT 08.12 complex products. No production changes.

# Checkpoint 2026-10-18: Batch 08.2–08.6 PARTIAL_PROTOTYPE research-only primitives committed: causal dilated convolution, scalar GRU forward step (no LSTM), GNN neighborhood aggregation, weighted temporal ensemble, and toy first-order sensor simulation. Each has README and synthetic smoke tests authored, not clean-checkout executed. NEXT 08.7 sim-to-real. No production changes.

# Checkpoint 2026-10-18: Batch 07.7–08.1 PARTIAL_PROTOTYPE research-only minimal baselines committed: frozen-feature target head, uncertainty-ranked DoE, approved incremental centroids, analytical resource estimate, and 1D convolution primitives (NOT a trained CNN). Each has README and synthetic smoke tests authored, not verified executed. NEXT 08.2 TCN. No production changes.

# Checkpoint 2026-10-18: Batch 07.2–07.6 PARTIAL_PROTOTYPE minimal research baselines committed: device-disjoint splits, concentration-level holdouts, baseline shift flags, reference offset correction, and featurewise domain alignment. Each has README and synthetic smoke tests authored, not clean-checkout executed. Full validation, drift/chemical-change disambiguation and negative-transfer checks pending. NEXT 07.7 transfer learning. No production changes.

# Checkpoint 2026-10-18: Batch 06.6, 06.7, 06.8, 06.9, 07.1 PARTIAL_PROTOTYPE research-only minimal baselines committed with README and synthetic smoke tests (not comprehensive 16-test suites; not executed from clean checkout). Negative evidence 06.7 uses externally supplied gas-specific detection power; never chemical-family hard exclusion. NEXT 07.2 cross-device tests. No production changes.

# Checkpoint 2026-10-18: 06.5 open-set recognition PARTIAL_PROTOTYPE committed. Nearest-class-centroid rejection with training-distance quantile threshold (optimistically biased), unknown-or-unsupported output and held-out descriptive rates; 16 synthetic tests authored, clean-checkout execution and real Tank validation pending. NEXT 06.6 OOD detection. No production changes.

# Checkpoint 2026-10-18: 06.4 split conformal prediction PARTIAL_PROTOTYPE committed. Closed-world candidate sets using held-out nonconformity quantiles; coverage only under exchangeability, not classwise or OOD guaranteed. 16 synthetic tests authored; clean-checkout execution and real Tank validation pending. NEXT 06.5 open-set recognition. Negative-evidence proposal retained for 06.7. No production changes.

# Checkpoint 2026-10-18: User-requested Negative Evidence & Sensor-Guided Exclusion proposal documented at research/NEGATIVE_EVIDENCE_PROPOSAL.md (PROPOSED, not implemented). 06.3 probability calibration PARTIAL_PROTOTYPE committed: multiclass logit temperature scaling and descriptive ECE; 16 synthetic tests authored, clean-checkout execution and real Tank validation pending. NEXT 06.4 conformal prediction. No production changes.

# Checkpoint 2026-10-18: 06.2 Bayesian inference PARTIAL_PROTOTYPE committed. Model-conditional Bayes update for supplied priors and class-conditional likelihoods; independence is caller-declared and not verified. 16 synthetic tests authored; clean-checkout execution and real Tank validation pending. NEXT 06.3 probability calibration. No production changes.

# Checkpoint 2026-10-18: 06.1 evidence fusion PARTIAL_PROTOTYPE committed. Dependence-group-capped log-opinion pooling over supplied candidate distributions; fused scores are not calibrated probabilities; grouping does not establish independence. 16 synthetic tests authored, clean-checkout execution and real Tank validation pending. NEXT 06.2 Bayesian inference. No production changes.

# Checkpoint 2026-10-18: 05.9 unseen concentration evaluation PROTOTYPE committed. Model-agnostic holdout of complete concentration levels; separates interpolation/extrapolation; computes error and abstention/coverage. No models trained or compared yet. 16 synthetic tests authored; clean-checkout execution and real Tank validation pending. Stage 05 research modules 05.1–05.9 drafted. NEXT 06.1 evidence fusion. No production changes.

# Checkpoint 2026-10-18: 05.8 LOD/LOQ PARTIAL_PROTOTYPE committed. One-sided blank-sigma signal thresholds (3x/10x) only, not validated analytical detection or quantitation limits; quantifiable always false. 16 synthetic tests authored, clean-checkout execution and real Tank validation pending. NEXT 05.9 unseen concentrations. No production changes.

# Checkpoint 2026-10-18: 05.7 identity-conditional concentration gate PROTOTYPE committed. External gas identity probabilities and confidence/margin checks route to known-gas calibrator, explicit abstention on uncertainty or invalid/out-of-range ppm. Probabilities not calibrated by this module; 16 synthetic tests authored, clean-checkout execution and real Tank validation pending. NEXT 05.8 LOD/LOQ. No production changes.

# Checkpoint 2026-10-18: 05.6 tree regression PROTOTYPE committed. Known-gas bagged CART regression on multichannel log10(Rs/R0), mean log10(ppm), tree dispersion (not calibrated uncertainty); 16 synthetic tests authored, clean-checkout execution and real Tank validation pending. NEXT 05.7 identity-conditional concentration estimates. No production changes.

# Checkpoint 2026-10-18: 05.5 PLS regression PROTOTYPE committed. Known-gas PLS1 with train-only standardization and latent components; 16 synthetic tests authored, clean-checkout execution and real Tank validation pending. NEXT 05.6 tree regression. No production changes.

# Checkpoint 2026-10-18: 05.4 multisensor regression PROTOTYPE committed. Known gas, aligned MQ Rs/R0 log-features, ridge linear regression of log10(ppm), training-only standardization, range abstention; 16 synthetic tests authored, clean-checkout execution and real Tank validation pending. NEXT 05.5 PLS regression. No production changes.

# Checkpoint 2026-10-18: 05.3 nonlinear regression PROTOTYPE committed. Quadratic log-log response fit for independently known gas/ppm with monotonic inverse and range abstention; 16 synthetic tests authored, clean-checkout execution and real Tank validation pending. NEXT 05.4 multisensor regression. No production changes.

# Checkpoint 2026-10-18: 05.2 multipoint calibration PROTOTYPE committed. Known gas/sensor, >=3 independent calibration levels, monotone piecewise log-log forward/inverse interpolation, no extrapolation. 16 synthetic tests authored; clean-checkout execution and real Tank calibration validation pending. NEXT 05.3 nonlinear regression. No production changes.

# Checkpoint 2026-10-18: 05.1 single-sensor calibration PROTOTYPE committed. Known gas, one MQ sensor, >=3 distinct independently known ppm standards; log-log power-law OLS; no extrapolation; R0/Rs ohms. 16 synthetic tests authored, clean-checkout execution and real Tank validation pending. NEXT 05.2 multipoint calibration. No production changes.

# Checkpoint 2026-10-18: 04.10 benchmarking PROTOTYPE committed. Grouped holdout partitioning and descriptive accuracy/coverage/selective accuracy/confusion/per-class metrics for externally supplied predictions; no real benchmark performed. 16 synthetic tests authored, clean checkout and real Tank evaluation pending. Stage 04 research code 04.1–04.10 present, with 04.5/04.7/04.9 PARTIAL_PROTOTYPE; not validated or production-ready. NEXT 05.1 single-sensor calibration. No production changes.

# Checkpoint 2026-10-18: 04.9 Physics + ML PARTIAL_PROTOTYPE committed. Candidate-specific MQ reference residual features at independently known concentration, 16 synthetic tests authored. ML model integration, unknown-concentration handling, clean-checkout test execution and real Tank validation pending. NEXT 04.10 benchmarking. No production changes.

# Checkpoint 2026-10-18: 04.8 MLP PROTOTYPE committed. Single-hidden-layer tanh/softmax network, full-batch backpropagation, train-only standardization, L2, 16 synthetic tests authored. Clean-checkout execution and real Tank validation pending. NEXT 04.9 physics + ML. No production changes.

# Checkpoint 2026-10-18: 04.7 XGBoost investigation PARTIAL_PROTOTYPE committed. Simplified multiclass gradient-boosted decision stumps, explicitly NOT XGBoost; 16 synthetic tests authored, clean checkout and real Tank evaluation pending. True XGBoost objective, Hessian-based splits, trees and validation remain outstanding. NEXT 04.8 MLP. No production changes.

# Checkpoint 2026-10-18: 04.6 Random Forest PROTOTYPE committed. Bootstrapped Gini decision trees with random feature subsets and deterministic seed; 16 synthetic tests authored, clean checkout and real Tank grouped holdout validation pending. NEXT 04.7 XGBoost. No production changes.

# Checkpoint 2026-10-18: 04.5 SVM PARTIAL_PROTOTYPE committed. Linear one-vs-rest hinge-loss subgradient baseline, 16 synthetic tests authored; kernel/RBF SVM and vetted solver comparison pending. Clean checkout and real Tank validation pending. NEXT 04.6 Random Forest. No production changes.

# Checkpoint 2026-10-18: 04.4 logistic regression PROTOTYPE committed. Multiclass softmax full-batch gradient descent, L2 regularization, train-only scaling, 16 synthetic tests authored; clean checkout and real Tank validation pending. Scores uncalibrated; convergence not independently verified. NEXT 04.5 SVM. No production changes.

# Checkpoint 2026-10-18: 04.3 KNN PROTOTYPE committed. Frozen training fingerprints, uniform and inverse-distance voting, deterministic tie flag, 16 synthetic tests authored; clean checkout and Tank grouped holdout evaluation pending. NEXT 04.4 logistic regression. No production changes.

# Checkpoint 2026-10-18: 04.2 fingerprint matching PROTOTYPE committed. Training-only multichannel exemplar library, scaled Euclidean nearest-reference per class and optional abstention threshold; 16 synthetic tests authored. Clean-checkout execution and real Tank held-out evaluation pending. NEXT 04.3 KNN. No production changes.

# Checkpoint 2026-10-18: 04.1 curve matching PROTOTYPE committed. Log-log piecewise interpolation of externally supplied MQ Rs/R0 curves and candidate ranking at independently known concentration; 16 synthetic tests authored, clean checkout pending. No real factory curves or measured Tank datasets bundled; unknown concentration/mixtures not handled. NEXT 04.2 fingerprint matching. No production changes.

# Checkpoint 2026-10-18: 03.8 identifiability limits PROTOTYPE committed. Empirical cross-class near-collision analysis and conservative query screening with uncalibrated distance threshold; 16 synthetic tests authored, clean checkout pending. Stage 03 has eight research implementations (03.4 PARTIAL_PROTOTYPE), no real Tank validation. NEXT 04.1 curve matching. No production changes.

# Checkpoint 2026-10-18: 03.7 concentration confounding PROTOTYPE committed. Within-gas Pearson correlation with log10 independently known ppm and insufficient-data flags; 16 synthetic tests authored, clean checkout pending. Does not establish causality, classification or concentration estimation. NEXT 03.8 identifiability limits. No production changes.

# Checkpoint 2026-10-18: 03.6 sensor subsets PROTOTYPE committed. Exhaustive nonempty sensor combinations and leave-one-out descriptive ambiguity, 16 synthetic tests authored. Selection bias warning; nested grouped CV and real Tank validation pending. NEXT 03.7 concentration confounding. No production changes.

# Checkpoint 2026-10-18: 03.5 sensor importance PROTOTYPE committed. Per-sensor univariate Fisher score, constant flag, train-only ranking and held-out projection; 14 synthetic tests authored, not verified from clean checkout. Requires Tank cross-session/device/concentration validation and ablation studies. NEXT 03.6 sensor subsets. No production changes.

# Checkpoint 2026-10-18: 03.4 LDA/PLS-DA PARTIAL_PROTOTYPE committed: binary ridge LDA and one-component binary PLS1-DA, 14 synthetic tests authored; no clean checkout verification. Full multiclass LDA and multicomponent PLS-DA, reference comparison and real Tank data validation pending. NEXT 03.5 sensor importance; return to complete 03.4 before any final scientific approval. No production changes.

# Checkpoint 2026-10-18: 03.3 class overlap PROTOTYPE committed. Descriptive leave-one-out nearest same/other class ambiguity and margins, 16 tests authored. Clean-checkout execution and controlled Tank cross-session data validation pending. NEXT 03.4 LDA/PLS-DA. No production changes.

# Checkpoint 2026-10-18: 03.2 similarity analysis PROTOTYPE committed. Cosine, Euclidean, Manhattan, scaled Euclidean and nearest reference. 16/16 local synthetic unit tests passed. Clean GitHub checkout and real Tank cross-session/concentration validation pending. NEXT 03.3 class overlap. No production changes.

# Checkpoint 2026-10-18: 03.1 PCA PROTOTYPE committed. Pure Python symmetric Jacobi covariance eigensolver, explained variance and projection, 14 tests authored; clean checkout and numerical reference verification pending. NEXT 03.2 similarity analysis. No production changes.

# Checkpoint 2026-10-18: 02.8 provenance schema PROTOTYPE committed. Metadata validation, UTC, calibration/device/session/sensor and canonical SHA-256 digest; 14 tests authored. Stage 02 now has eight research prototypes; clean checkout and real Tank validation pending. NEXT 03.1 PCA. No production changes.

# Checkpoint 2026-10-18: 02.7 feature selection PROTOTYPE committed. Training-only Fisher-like univariate ranking, deterministic indices, 14 synthetic tests authored; clean checkout and real-data validation pending. NEXT 02.8 provenance schema. No production changes.

# Checkpoint 2026-10-18: 02.6 3D/4D time-aligned response trajectories PROTOTYPE committed; 16 tests authored, clean checkout and real-data verification pending. This is not a reproduction of MQDataScience methods. NEXT: 02.7 feature selection. No production changes.

# Checkpoint 2026-10-18: 02.5 fingerprints PROTOTYPE committed. Ordered 8-sensor log-fold Rs response vector and cosine baseline, 14 test cases authored. Legacy fingerprint_v1 parity NOT verified; real Tank sensor order and heater-phase data pending. NEXT: 02.6 3D/4D features. No production changes.

# Checkpoint 2026-10-18: 02.4 temporal features PROTOTYPE committed, 16/16 synthetic local tests passed. Features include observed-peak rise and recovery times, slope and area. Actual Tank exposure timing and gas validation pending. NEXT: 02.5 fingerprints; inspect existing legacy fingerprint research before writing duplicate algorithms. No production changes.

# Checkpoint 2026-10-18: 02.3 static feature extraction PROTOTYPE committed; baseline/exposure Rs summary, relative and log-fold changes, 14 test cases authored. Clean-checkout test run and real Tank validation pending. NEXT: 02.4 temporal features. No production changes.

# Checkpoint 2026-10-18: 02.2 scaling PROTOTYPE committed under research/techniques/02.2/. Standard and robust scalers fit on training data only; 13 test cases authored but clean-checkout run not verified. Next sequential item 02.3 static features. Tank research only, no production changes.

# Checkpoint 2026-10-18: 02.1 filtering PROTOTYPE committed. Three causal filters (median, moving average, EMA), 15/15 local tests passed. Need real Tank time-series and clean checkout validation. Next 02.2 scaling. Tank research only; no production changes.

# Checkpoint 2026-10-18: 01.7 baseline drift monitoring prototype committed (robust median and log-change, stability gate; no automatic R0 changes). Twelve test cases authored, clean-checkout execution pending. Stage 01 research prototypes 01.1–01.7 now recorded; none chemically or hardware validated. NEXT sequential item 02.1 signal filtering. No production changes.

# Checkpoint 2026-10-18: 01.6 signal-quality prototype committed (missing data, saturation, out-of-range, insufficient data, MAD diagnostics). 13 test cases authored, not yet rerun in clean checkout. Hardware ADC configuration and real Tank data validation pending. NEXT sequential item 01.7 drift monitoring. No production changes.

# Checkpoint 2026-10-18: 01.5 environmental correction research prototype committed under research/techniques/01.5/. BME280 temperature, RH, pressure conditional normalization; coefficients must be fit from controlled data. Twelve test cases authored but clean checkout not yet run. No field validation or production changes. NEXT: 01.6 signal quality.

# Checkpoint 2026-10-18: 01.4 heater-phase segmentation prototype committed with 12 software test cases; test execution and physical heater profiles remain unverified. NEXT: 01.5 environmental correction (temperature/humidity/pressure) with BME280 and real calibration data; avoid unvalidated compensation coefficients. Tank only; no production changes.

# Research checkpoint 2026-10-18
01.1 ADC-to-Rs, 01.2 R0 and 01.3 sensitivity curves have research prototypes committed to Tank. 01.2 hardware calibration and 01.3 manufacturer-curve validation remain pending. Next sequential item: 01.4 heater cycles, especially MQ7 and MQ9. No production changes.

# Checkpoint update 2026-10-18
01.1 ADC-to-Rs: PROTOTYPE. Source, 11 unit tests, and README committed under research/techniques/01.1/. Actual Tank hardware topology/ADS1115 gain/RL not verified. Next sequential item: 01.2 R0 calibration audit; first inspect legacy code and verify sensor heater cycles, R0 units, calibration gas and actual data. No production changes.

# EXCLUSIVE SCOPE: HazRob Tank ChemCore
Never resume this roadmap in HazRob-Lab or the original HazRob repository. Current owner: HazRob/HazRob-Tank. Historical evidence links in HazRob-Lab are references only, not Tank implementation.

# ChemCore — resume checkpoint
Date 2026-10-18 | Roadmap 1.0 | Repo HazRob/HazRob-Tank

## NEXT
01.1 completed as research PROTOTYPE (not hardware validated); next 01.2 R0 calibration audit. Legacy results exist but have not been independently audited for these gates.

## Exact next actions
1. Read research/MASTER_PLAN.md and research/STATUS_LEDGER.md.
2. Inspect latest connected firmware and ChemCore measurement code for ADS1115 and ESP32 ADC scaling, RL, R0, reference voltages, attenuations, divider topology, units, clipping.
3. Verify real MQ board wiring and sensor-specific heater modes; derive and test Rs equations.
4. Create dated experiment log, code tests, decision, update ledger and this checkpoint with last commit and unresolved issues.

## Legacy pointers
references/tech12_nested_subset_validation_2026-10-18.md; references/tech13_evidence_fusion_prototype_2026-10-18.md; references/tech14_real_data_generalization_audit_2026-10-18.md; references/extracted_analytical_equations_batch01_2026-10-18.md.

## Restrictions
No production ChemCore/APK/firmware modifications. No claim of empirical validation from unit tests alone. Do not merge HazRob Tank with original HazRob.

## Prompt for a new conversation
استأنف خارطة ChemCore البحثية من HazRob/HazRob-Tank. اقرأ research/RESUME.md ثم research/MASTER_PLAN.md وresearch/STATUS_LEDGER.md. نفذ البند التالي بعد مراجعة الأدلة السابقة، ووثق الشيفرة والاختبارات والقرار وحدّث نقطة الاستئناف. لا تعدل الإصدارات الإنتاجية.
