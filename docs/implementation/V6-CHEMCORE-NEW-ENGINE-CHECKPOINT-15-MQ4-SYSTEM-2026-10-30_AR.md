# ChemCore Tank V6 — Checkpoint 15: MQ4 external reference whole-system research validation

**Date:** 2026-10-30 (Asia/Aden)  
**Branch:** `feature/tank-v6-implementation`  
**User task:** «ابدأ التجارب بهذه القياسات على مستوى المنظومة ككل»

## 1. Scope and defensible meaning of "whole system"

A **complete external-source research processing chain** was run, from byte-integrity checks to original sensor schema QC, reference ppm conversion, separated training/calibration/held-out day splitting, source regression, T/RH compensation, model comparison, uncertainty calibration, heldout sensor-instance transfer, concentration-elevation stress, missing data/fault gates, and evidence/reproducible outputs.

**THIS IS NOT:** direct execution of all 80 techniques, Android `ChemCoreTankV6` and session/PDF/Excel entry points, all eight installed MQ models, a multi-gas identity/open-set experiment, field safety validation, or `G3`. Python/scikit-learn models are an **independent scientific assessment harness** and are not silently renamed production ChemCore engines.

### Actual original source data

- `10ppm_data.csv` from [Furuta et al. Minnesota DRUM](https://hdl.handle.net/11299/226714) (source citation DOI `10.13020/13xf-nw93`). Original SHA256 `3c27c4f8b8e8aab54b6433acfa41de00890a9eefd19376259a2371f22a48ff2d`.
- **14,186 consecutive one-minute source readings, 11 days (2021-07-21 through 2021-07-31), three physically separate MQ4 units**, raw MQ4 resistance Ω, independent Li-Cor measured CH4 ppb, T °C and RH %. The physical reference interval is approximately **1.9455–9.9979 ppm** by dividing recorded ppb by 1000. Source also has Figaro channels; these were deliberately excluded from the MQ-only feature set.
- Supplemental [NohPei/source sensor_log.csv](https://github.com/NohPei/mobile-AQ-documentation/blob/main/sensor_log.csv) SHA256 `9ff45148718003878bca102c28d53df2708b87c7371e7d4ba986ddfd0ba98dc8`, **114 short-interval voltage measurements of MQ3/MQ4/MQ8** and separate environmental measurements. No defined gas exposures or reference gas ppm; negative `-1` sentinels are missing, not valid zero.

Original files and README/source license are archived in personal Library `/HazRob/MQ_Reference_Measurements_2026/HazRob_MQ_Reference_Real_Measurements_2026-10-30.zip`, ID `libfile_28f13e2a92f48191a2ad462be5814f75`.

## 2. Reproducible experiment code and CI

- Runnable Python: [`research/chemcore_v6/benchmarks/mq4_full_system_external_source_benchmark.py`](../../research/chemcore_v6/benchmarks/mq4_full_system_external_source_benchmark.py), commit `43fa6b23b73cfc00da69055d03cf4529a27e7603`.
- GitHub Actions: `.github/workflows/chemcore-v6-mq4-system-reference-ci.yml`. Downloads *original* Minnesota and NohPei CSV over official/public HTTPS and rejects SHA changes. Source code uses numpy/pandas/scikit-learn fixed versions; no original Tank firmware altered.
- CI original research run [40966759549](https://github.com/HazRob/HazRob-Tank/actions/runs/40966759549) SUCCESS; updated source fault injection [40967142397](https://github.com/HazRob/HazRob-Tank/actions/runs/40967142397) SUCCESS; final strict fault CI run indicated in link from latest workflow execution.
- Actual source research results pinned `research/chemcore_v6/results/mq4_external_whole_system_day_unit_holdout_2026-10-30.json`, commit `f5cc16f46b0c3bc19a92060a4eac1397756094b1`.
- Complete independently generated results ZIP and the original Python script saved into personal Library `/HazRob/MQ_Reference_Measurements_2026/HazRob_Tank_V6_MQ4_Whole_System_Research_2026-10-30.zip`. Includes `mq4_day_blocked_predictions.csv` with 70,930 rows, 55 fold records, model summaries, 33 unseen unit-day transfers, fault injection and mobile source QC records.

## 3. Holdout protocol

Each **whole source collection day** (11 unique dates) is held out as TEST one at a time. The PREVIOUS other whole day is CALIBRATION ONLY (split conformal residual). All remaining nine different days are TRAIN. No source row from test/calibration day can be used to train model weights or preprocessing scalers.

Five **preregistered source research models** for one continuous CH4 concentration:

1. TRAIN_MEDIAN_REFERENCE: median Li-Cor CH4 training labels; no MQ readings (naïve strong baseline).
2. RIDGE_MQ4_1: logarithm of a single raw MQ4 resistance in Ω, training-only standardization.
3. RIDGE_MQ4_3: three independent MQ4 instances, log Ω and training-only scaler.
4. RIDGE_MQ4_3_T_RH: MQ4 trio with measured T and RH as covariates.
5. HGB_MQ4_3_T_RH: fixed shallow histogram gradient boosting with three MQ4 readings plus T/RH.

The temperature/RH features are SOURCE measurements, not sensor substitute reference methane labels. The three same-model MQ4s are NOT claimed equivalent to the eight different Tank MQ model types.

For each fold compute absolute error MAE, RMSE, bias, day-group error, and calibration-day-sourced nominal 90% prediction interval halfwidth; evaluate true held-out day interval coverage. No model or cutoff selected using any heldout test label.

## 4. Source reference performance (11 day holds, 14,186 heldout source exposures per model)

| Model | Pooled MAE ppm | Pooled RMSE ppm | Empirical nominal 90% interval coverage |
|---|---:|---:|---:|
| TRAIN_MEDIAN_REFERENCE | 0.8767 | 1.4774 | 79.59% |
| RIDGE_MQ4_1 | 0.8601 | 1.2675 | 77.29% |
| RIDGE_MQ4_3 | 0.8475 | 1.2637 | 81.90% |
| RIDGE_MQ4_3_T_RH | 0.8415 | **1.0829** | 81.94% |
| HGB_MQ4_3_T_RH | **0.8021** | 1.2148 | **82.62%** |

Total **55 source model/day evaluations** and **70,930 source heldout predictions**. The best raw MAE model was HGB but the 90% nominal interval actually covered only 82.62% of independent heldout source rows, and may vary severely by day. We cannot promote that uncertainty as 90% reliable.

**Important confounder:** MQ4 raw resistance is strongly affected by ambient source temperature; observed correlation with source T about **-0.87 to -0.89**, so environmental compensation and sensor-specific calibration are mandatory to evaluate in Tank. Source LiCor results cannot be assumed to apply to Tank R0/RL.

## 5. Device/domain transfer: hold out SOURCE SENSOR INSTANCE and DAY simultaneously

Additional 33 folds = 11 test days × 3 withheld MQ4 instances. Fit on OTHER TWO physical MQ4 units, only the OTHER NINE training days. An independent calibration day is reserved. Evaluate the previously unseen MQ4 unit on the independently withheld test day. Unit is not in training data, and test-day timepoints are absent from fitting.

Mean of 33 heldout source unit/day MAE values: **1.1561 ppm** (pooled average across folds, not on Tank). Mean coverage of nominal 90% intervals: **83.18%**. Average MAE by source heldout MQ4 instance:
- MQ4_1: **1.6098 ppm**
- MQ4_2: **1.0723 ppm**
- MQ4_3: **0.7862 ppm**

**Conclusion:** cross-unit calibration transfer is inconsistent even between nominally the SAME MQ4 model. Reusing literature R0 or scaling from another physical unit cannot be accepted without measured transfer evidence.

## 6. Detection stress diagnostic, NOT an alarm

Research diagnostic: reference Li-Cor CH4 at or above 4 ppm vs source regression estimate at or above 4 ppm. This is NOT a regulatory, safety, or factory alarm threshold.

For the best pooled-MAE HGB model:
- true positives **533**; false negatives **1813** (sensitivity **22.72%**);
- false positives **1044**, true negatives **10796** (non-high false positive rate **8.82%**).
- 2346 of 14186 Li-Cor source rows meet the >=4ppm diagnostic rule; only 533 were classified elevated.

The RIDGE_MQ4_3_T_RH model detected **1302/2346** source highs with **787** false-positive non-highs — better sensitivity than HGB but still not suitable for safety alarms. Low MAE alone is a dangerous sole acceptance target.

## 7. Sensor schema, missing values and fault injection

- All three MQ4 source columns were required positive finite original Ω before downstream processing.
- Code **actually executed** missing-channel `MQ4_2` and nonfinite-`MQ4_3` synthetic faults and verified that each produces input rejection. It does not establish Android's missing-connection handling or physical failure mode and must not be represented as an ESP32 firmware test.
- NohPei logged 114 timestamps and all three voltage channels. Sampling median gap **0.133 seconds**; only **4 rows** carry valid temperature/RH/other-device CO2 fields; other `-1` values are missing. **No chemical gas identity, multi-gas fingerprint or ppm** inferred from this record.

## 8. What remains blocked (G3 and full software)

- **Only one known gas (CH4)**. No proof of discrimination against ethanol, acetone, toluene, CO, H2, LPG etc. Unknown-gas Open-set remains UNVALIDATED, prior N05-H source rejection failed.
- **Only three MQ4 instances**. The Tank physical matrix MQ2/MQ3/MQ4/MQ5/MQ7/MQ8/MQ9/MQ135 was NOT physically exercised.
- **Independent source research harness only**, using scikit-learn. This is NOT execution of all 80 methods in the Android Java `ChemCoreTankV6` application; no APK/session/PDF/report screens or Bluetooth/Wi-Fi firmware link tested.
- No device R0 measured on Tank, no per-sensor R0 acceptance, no field chemical family exclusions, no certified ppm, no occupational hazard determination.
- The MQ3/MQ4/MQ8 mobile trace is unlabeled ambient response and cannot be used as supervised concentrations.

**Formal disposition: EXTERNAL_SOURCE_RESEARCH_CODE_VALIDATED / SYSTEM_G3_HOLD / 78_PARTIAL_2_UNPORTED / 0_TANK_G3 / NOT_PRODUCTION_READY / NO_NEW_APK.**

## 9. Next gated integration

1. Add a **versioned source data adapter** to validate unit conversion (source MQ4 Ω, independent reference CH4 ppb) and forbid mapping three MQ4 copies into disparate Tank MQ channels. Preserve immutable original timestamps, reference and device instance IDs.
2. Validate actual `ChemCoreTankV6` gas-only output provenance/consensus/UNKNOWN flags using original source records without treating source-only HGB as production model, and confirm **NO legacy inference fallback** in Android.
3. Expand heldout original MQ data to other pure gases and MQ sensor model types with source-qualified concentration reference and per-unit R0. Only this makes multi-gas identity and 80-method coverage measurable.
4. Conduct actual Tank G3 sensor/DAQ experiments and Android integration/runtime qualification before app release or safety deployment.

**Resume:** `CHECKPOINT15_MQ4_EXTERNAL_WHOLE_SYSTEM / MQ4_14186_11DAYS_3UNITS_LICOR / FOLDS_55 / PREDS_70930 / INSTANCE_DAY_TRANSFER_33 / BEST_MAE_0.8021ppm / BEST_COVERAGE90_82.62pct / DIAGNOSTIC_4PPM_FN_1813_OF_2346 / TANK_G3_0_80 / ANDROID_RUNTIME_NOT_TESTED / FOOD_SEPARATE`.
