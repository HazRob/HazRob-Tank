# G2 independent evidence intake and blinded evaluation protocol (draft)

Status: **BLOCKED — no independent physical evidence registered**. This document is a prospective protocol, not a claim of qualification.

## Evidence acquisition

1. Freeze the research candidate commit SHA, configuration, sensor channel mapping, preprocessing, calibration/R0 policy, and reference library hashes **before** collecting the holdout. Keep ChemCore Stable untouched.
2. Independently prepare reference-verified substances, concentrations, and blanks with recorded preparation method, uncertainty, ambient temperature/humidity, exposure duration, recovery time, instrument serials, sensor age, and acquisition timestamps. Document traceable reference instrument calibration and chain of custody. Never infer reference concentrations from the MQ array under test.
3. Randomize acquisition order, interleave blanks and repeat sessions, and include distinct days, devices, operators, concentration bands, mixtures, interference conditions, and out-of-distribution compounds. Record failed runs and exclusions rather than silently dropping them.
4. Preserve **unaltered original raw bytes** and calculate SHA-256 before any parsing or cleaning. Enter each physical record in `G2_INDEPENDENT_EVIDENCE.json` with the required fields: `source_id`, `raw_data_path`, `raw_sha256`, `measurement_date`, `instrument_id`, `calibration_reference`, `holdout_split_id`. Store original files under the manifest's configured raw root. The integrity gate does not authenticate the scientific truth of the measurements.
5. Lock group-based train/validation/test assignments at the source-session/device/day level before model selection. Prevent repeated windows from the same exposure leaking across splits. An independent custodian should retain blinded reference labels until predictions are finalized and hashed.

## Reproducible evaluation

- Run the frozen 80-technique implementation and prespecified ablations against the locked holdout, recording exact source SHA, environment/dependency versions, random seeds, feature transformations, sample inclusion counts, and output hashes.
- Report each technique's eligibility and coverage, source attribution, unknown rejection, false-positive/false-negative rates, confusion matrix, calibration, and abstention; quantify concentration error only for analytes with traceable quantitative ground truth. Include uncertainty intervals and per-device/day/concentration strata. Report computational failures explicitly.
- Compare with fixed baselines under identical partitions and input channels. Distinguish engineering CI success, raw-file integrity, analytical performance, and independent physical validation as separate gates.
- Do not use the holdout for threshold tuning. If any threshold is changed after inspecting results, invalidate the prior blind test and collect a new holdout.

## Release gates

**Integrity:** raw byte digests and canonical metadata pass CI. **Provenance:** a reviewer independently confirms source instrument, calibration, labels, custody, and split independence. **Scientific:** prespecified acceptance criteria and uncertainty bounds pass on a never-used independent physical holdout. No scientific approval, accuracy claim, or promotion to Stable follows from an integrity-only pass.

## Current blocker

The checked-in `G2_INDEPENDENT_EVIDENCE.json` contains `records: []`. Independent physical samples, blinded reference labels, and traceable calibration records must be supplied by the measurement team; synthetic CI fixtures cannot replace them.
