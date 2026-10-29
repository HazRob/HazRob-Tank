# ChemCore Tank V6 — Checkpoint 14: Real MQ sensor measurement re-evaluation

**Date:** 2026-10-29 (Asia/Aden)  
**Branch:** `feature/tank-v6-implementation`  
**Priority change:** User requested a repeat of V6 tests using **known physically recorded MQ sensor values for different substances and different concentrations** rather than silently substituting the UCI ten-sensor non-MQ array.

## 1. Verified implementation and reproducibility

- New JDK Java **real-source MQ study** in `research/chemcore_v6/benchmarks/RealMqSourceV6Benchmark.java` (final code commit `19f1c72fb0bbaedac61e46c639670d882880bbbe`).
- CI pipeline `.github/workflows/chemcore-v6-real-mq-multi-gas-concentrations.yml`, initially committed `ac499f005b84fddce4e016935800d3f1c37596ef`, fixed original MQ serial counter reset and unit-aware scientific notation; successful [GitHub Actions 40962025285](https://github.com/HazRob/HazRob-Tank/actions/runs/40962025285).
- SHA256 of 6,400 original-source MQ ADC rows pinned as `13c2c85cfbb23357a71555b9bc2cbe7dd68e931ffc94df30389c1a4631203745`. UCI1081 source archive original SHA256 `813ebb7d00ed9172114b68d27ab3213170d6c220b8b23907dce91e9838cd48ab` and underlying `gsalc.csv` `d79afe6494903b988de68d4a8ab133167151a1bb620ba413e5cefd15003d07b3` all verified before computing.
- Actual executable independent V6 Java methods: `ClassicalLearningV6.fitSoftmax`, `SourceValidationV6.benchmark`, `SourceNumericsV6.estimateUnseen`. No old gas ChemCore invoked by benchmark, no food freshness data, no imputed MQ4/MQ9 channels.
- Full results and provenance JSON: `research/chemcore_v6/results/real_mq_gas_identification_concentration_holdout_2026-10-29.json` (commit `8ac7b62802a3cfbc58de2c327a230c7ad4f00f6f`).
- Artifact 12936685657, SHA256 ZIP `7c1b0303e6c3919f70a2b18736e21daf4ba3b6685814570ac88dd3d3fb527ac5`, original test predictions and MAE calculations `ChemCore_V6_Real_MQ_Gases_Concentrations_2026-10-29.zip`, backed up to personal Library ID `libfile_3db75bf3369481919ebd181c3f9ec59f`.

## 2. Test A: measured source ADC from multiple MQ models, 6,400 rows

**Scientific source:** Narkhede et al. `MultimodalGasData: Multimodal Dataset for Gas Detection and Classification`, DOI `10.17632/zkwgkjkjn9.1`; repository copy `research/chemcore_v6/source_snapshots/external/mq_ext_03_multimodal_6400_raw.csv`.

The source contains original ADC readings for **MQ2, MQ3, MQ5, MQ6, MQ7, MQ8, MQ135**, with four **class labels**, each with 1,600 contiguous records: `NoGas`, `Perfume`, `Smoke`, `Mixture`. The source's "Serial Number" **resets from 0 to 1599 for each class**; global zero-based row offsets identify unique source rows. **No source concentration ppm** and none of these are an unambiguous pure chemical gas identity, so the test is CATEGORY classification only, never quantitative calibration.

Six sensor models overlap with installed Tank: `MQ2, MQ3, MQ5, MQ7, MQ8, MQ135`. Original MQ6 is absent from the Tank; Tank MQ4 and MQ9 are missing in this source and were NEVER filled with synthetic values.

For each source class, partition its ordinal 1,600 rows into five **contiguous 320-row blocks**. Leave one block out, exclude twenty immediately neighboring rows on both sides from training (when present), fit V6 Softmax from other source blocks using training-only scaling, and score the full 320 original rows. Five folds × four source classes = 6,400 held-out rows **per feature configuration**; three configurations yield 19,200 recorded predictions. Source block holdouts are not independent sessions or different devices.

| Features | Five-fold source balanced accuracy | Source macro-F1 |
|---|---:|---:|
| MQ135 only | **66.84%** | 65.99% |
| Six MQ models shared with Tank | **74.66%** | 71.65% |
| All seven MQ models in original source | **74.92%** | 72.02% |

**Important confusion analysis for the six-model Tank-overlap feature subset:** Smoke correctly recognized **1,600/1,600** source readings. Mixture correctly recognized **1,588/1,600**. NoGas only **804/1,600** (796 wrongly called Perfume), and Perfume only **786/1,600** (814 wrongly called NoGas). Therefore overall 74.66% is mainly driven by two easy source-class pairs; it is **not** comparable to six pure-gas identity accuracy or an open-set chemical-safety result.

**Meaningful scientific limitation:** no actual independent session/device/day IDs or reference gas concentration in this particular CSV. Original temporal rows likely correlated. Treat counts as source ADC rows, not independently replicated experiments. Source ADC chain and `R0` values not interchangeable with HazRob Tank.

## 3. Test B: one real MQ135 source sensor; six pure analytes × three concentration levels

**Scientific source:** UCI1081 Gas sensor array low-concentration. Ten source channels exist but this evaluation **uses only MQ135** (index 4) instead of claiming that the remaining channels are MQ2/MQ3/MQ4/MQ5/MQ7/MQ8/MQ9.

Six pure source gases: ethanol, acetone, toluene, ethyl acetate, isopropanol, hexane. Each source gas has five original exposure samples at each **50, 100, 200 ppb**, giving **90 original source exposures** (not an independent sensor-array Tank dataset). Each heldout test level removes all 30 original source gas exposures at that concentration; train at other two levels (60 original source exposures), V6 Softmax on three MQ135 original source temporal summary ratios.

The quantitative inverse is deliberately **best-case conditioned on TRUE known source gas identity** to isolate calibration failure: from the two OTHER levels' five measured MQ135 endpoint/source-baseline ratios per gas, fit a log-ratio versus log-ppb two-point source anchor, then invert for every sample at the heldout level. The concentration label is not reused to fit and no test exposure enters the standardizer. This does **not** give end-to-end gas-ID-then-quantification performance.

| All 6 pure gases at a completely held-out source ppb level | Gas-ID balanced accuracy | Median absolute concentration error using TRUE gas identity | Out-of-reference response |
|---|---:|---:|---:|
| 50 ppb | 23.33% | 28.12 ppb | 28/30 |
| 100 ppb | 50.00% | 33.54 ppb | 10/30 |
| 200 ppb | 43.33% | 120.15 ppb | 22/30 |

All six source gases combined: gas ID **38.89% mean balanced accuracy**; concentration inversion returned 90 positive floating-point outputs but **60/90 were extrapolations**, only 30/90 were within training sensor-response bracket. Overall absolute error median **54.99 ppb**, mean **3,075 ppb** (large extreme outliers), predicted level range **5.66e-12–237,750 ppb**. These values demonstrate **poor model stability**; a finite number from extrapolation is not a valid gas concentration measurement. The original source's low ppb concentrations are described as **below sensor detection limit**, so even the good-looking predictions must not be called accurate or reference-grade.

### Median absolute error in ppb by source gas and heldout true concentration

| Pure source gas | Heldout 50 ppb | Heldout 100 ppb | Heldout 200 ppb |
|---|---:|---:|---:|
| Acetone | 21.87 | 23.59 | 193.17 |
| Ethanol | 11.10 | 56.78 | **4306.79** |
| Ethyl acetate | 5.77 | 20.77 | 74.77 |
| Hexane | 50.00 | 191.30 | 112.97 |
| Isopropanol | 19.19 | 10.91 | 108.00 |
| Toluene | **739.51** | 74.42 | 120.04 |

**Source gas discrimination from MQ135 alone:** 14/15 toluene exposures correct (93.33%), but only 2/15 acetone (13.33%) and 3/15 isopropanol (20%). This heterogeneity prohibits a global reliability claim based solely on mean accuracy.

## 4. What WAS NOT used or claimed

- Mendeley `Advanced Gas Detection and Classification using MQ Series Sensors`, DOI 10.17632/jmhr42p7sf.1, is excluded from this physically measured source benchmark because its public description states Proteus sensor simulation.
- Peer-reviewed papers WANG_SNB_2022_MTL12 (10–100 ppm across 12 analytes) and MEI_SENSORS_2025_BINARY_VOC (0–100 ppm pure/mixture) identify strong potential **real multi-MQ concentration** datasets, but `mq_experiment_source_index_v1.json` explicitly marks `raw_numeric_local=false`. Without their original numerical matrices and per-exposure independent metadata, we **do not invent input rows or reproduce their reported paper metrics as if run by our V6 Java engine**.
- Manufacturer `mq_research_curves_43.json` and cross-study R0/Rs examples are valuable reference curves, but substituting values computed from their power-law formulas is **simulation** rather than independent gas measurements. Such results are NOT pooled into the real-source benchmark.
- Tank has no measured G3 reference gases in this study. No hardware-specific `R0`, firmware heater protocol or physical test/calibration was changed.
- FOOD freshness is kept in its independent module. No food-headspace reference enters gas ChemCore.

## 5. Implications for ChemCore V6 and G3 gating

- Physical-class categorization from actual multi-MQ source ADC is feasible at exploratory level, but same source **cannot** support pure gas ID/ppm or real Tank generalization.
- Source MQ135 known pure-gas at 50–200 ppb is insufficient to demonstrate stable quantification across withheld concentrations and produces unacceptable extrapolation, even with gas identity supplied as an oracle.
- Keep all source curve hypotheses and contradictions visible, but `reported_identity=UNKNOWN` and `reportable_ppm=false` in device software until independent Tank G3.
- Requires **new multi-gas, multi-concentration original Rs/R0 or ADC data** across installed MQ model instances, with documented gas flow/reference ppm, device IDs, session groups, acquisition order, temperature/humidity and repeats. Prefer data measured across separate days/devices; use exact source certificates and documented ADC/RL/heater setup.
- V6 N05-H original open-set source test already failed safety promotion; this new MQ-only test does not change that hold.

**Next resume token:** `V6_CHECKPOINT14_REAL_MQ_SOURCE_TESTS / CI_40962025285_PASS / MQ6400_5BLOCKS_3CONFIGS_19200_PREDS / SIX_TANK_OVERLAP_BALACC_74.66_PCT_CLASS_ONLY / MQ135_6GASES_3PPB_LEVELS_90_EXPOSURES / MQ135_BALACC_38.89_PCT / CONC_60_OUT_OF_90_EXTRAPOLATIONS / CONC_MEDIAN_AE_54.99_PPB_ORACLE_GAS / G3_0_80 / NO_NEW_V6_APK / FOOD_ISOLATED`.
