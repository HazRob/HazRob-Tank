# HazRob Reference Benchmark — exhaustive inclusion policy

Status: **SPECIFICATION ONLY** (2026-10-18). No source datasets have been enumerated or evaluated yet.

## Scope

Include **every available measurement** from the reference library when it has (1) an identifiable chemical or documented mixture identity, (2) documented concentration(s) and units, and (3) a usable sensor response fingerprint. Include all replicates, concentrations, sessions, devices, temperatures/humidity/pressure metadata, and R0 metadata where present. Do not limit to five databases or sample a convenient subset. Missing BME or R0 is not an automatic exclusion; retain a missingness flag and restrict relevant evaluations accordingly.

## Required manifest per source/record

Source citation, dataset ID/version/license, file and row/trace identifier, compound identifiers and mixture constituents, concentration values and units, instrument/sensor types and channel mapping, raw or normalized response, R0 values with units and calibration protocol if present, timestamps, session/device/experiment ID, environmental readings (BME if available), measurement quality flags, source provenance, and transformations.

## Eligibility and exclusions

Only known-identity + known-concentration + usable-fingerprint records enter the **labeled concentration benchmark**. Preserve excluded or incomplete records in a separate audit manifest with reasons. Never fabricate missing channels, R0, environmental measurements or concentrations. Keep all original records immutable; retain duplicate readings but flag true duplicated exports and avoid double-counting. Treat source-level provenance and license restrictions as binding.

## Evaluation design

Prevent leakage with group-separated training/validation/test by experiment, session, device and original source; do not randomly split adjacent timepoints of the same exposure. Fit normalization, R0-derived transforms and model selection on training folds only. Report results stratified by gas, mixture, concentration range, sensor subset, R0 availability, environmental metadata, device and source. Use leave-one-source/device/session/concentration-out when supported. A gas absent from training must be tested under open-set rules, not forced into closed-set classification. Distinguish true measured records from derived synthetic augmentation. Quantify coverage and missingness; disclose unavailable evaluation subsets.

## Reporting

Full eligible sample count and total source count; all included/excluded counts and reasons; performance metrics with group-aware uncertainty; class and concentration distribution; mixture and environmental subsets; reproducibility hashes and scripts; explicit NOT_TESTED status until datasets are ingested and tests executed. These reference results do not establish HazRob Tank hardware accuracy or safety certification.

## Non-goals

No production ChemCore, Android APK, ESP32 firmware, or original HazRob project modifications.
