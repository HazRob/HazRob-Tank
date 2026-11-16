# G2 independent physical evidence acquisition protocol (research only)

Status: **BLOCKED — no independent physical records in G2_INDEPENDENT_EVIDENCE.json** (checked 2026-11-12). This protocol is a reproducibility plan, **not** empirical validation or an authorization to promote ChemCore Stable.

## Preregister before collecting data
1. Freeze a Git commit for algorithms, sensor mapping, preprocessing, R0 policy, thresholds, candidate universe, and evaluation scripts. Record commit SHA, firmware/app versions, instrument serial IDs and calibration certificates.
2. Define the intended analytes and concentration ranges from the central reference library. Distinguish **manufacturer response curves**, **published third-party measurements**, and **new physical measurements**; never treat simulated or digitized curves as independent robot measurements.
3. Assign holdout groups **before** data capture: separate acquisition sessions, days, gas cylinders/batches and preferably devices. No replicate, adjacent temporal window or derived feature from the same exposure may cross train/validation/test boundaries.
4. Record the independent reference instrument, traceable calibration, concentration uncertainty, environmental conditions (temperature, humidity, pressure), sampling flow, exposure timing, blank/purge intervals, gas identity, and mixture preparation. Document cross-sensitivity and safety controls.

## Capture and provenance
- Store immutable raw time-series and acquisition metadata; compute SHA-256 on original bytes before preprocessing.
- Create a manifest record for each distinct raw file with source_id, raw_data_path, raw_sha256, measurement_date, instrument_id, calibration_reference, holdout_split_id.
- Use unique source IDs and file hashes; retain chain of custody, reference-instrument readings, and negative/clean-air controls.
- Verify actual file existence and byte-level SHA-256 in a separate evidence-integrity stage. The manifest audit alone checks metadata syntax and **does not verify physical files, independence, measurement accuracy, or traceability**.
- Never include personal identifiers or confidential laboratory information in public repository artifacts.

## Reproducible evaluation
1. Freeze all hyperparameters and decision thresholds before touching the sealed test split; run preprocessing fit only on training data.
2. Evaluate all 80 methods individually and in the integrated pipeline, including ablation, feature leakage checks, missing-sensor tests, R0 factory-vs-field sensitivity, humidity/temperature drift, device/day transfer, mixture ambiguity, unknown/open-set abstention and false alarms.
3. Publish confusion matrices and per-class precision/recall/F1, balanced accuracy, macro averages, coverage vs selective risk, false-positive rate on blanks, quantitative MAE/RMSE/bias with units and confidence intervals; include sample counts and grouped uncertainty estimates. Report failures and excluded cases.
4. Use grouped bootstrap or another justified uncertainty method at the **independent session/device** level, not naive window-level resampling. Compare with simple baselines under identical splits.
5. Archive a machine-readable run manifest (commit SHA, data hashes, split assignments, Python/environment versions, deterministic seeds, commands, workflow run URLs) and CI artifacts. CI green means **software checks passed**, not scientific qualification.

## Release gate
Keep `scientifically_validated=false` until independent traceable measurements exist, the sealed holdout is evaluated without leakage, quantitative acceptance criteria are preregistered and met, and an independent reviewer verifies evidence and uncertainty. The present `PROVENANCE_REVIEW_REQUIRED` status is **not** approval. No changes to ChemCore Stable, application or firmware are permitted under this research protocol.
