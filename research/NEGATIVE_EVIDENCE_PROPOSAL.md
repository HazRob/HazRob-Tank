# Research decision: Negative Evidence & Sensor-Guided Exclusion

**Status: PROPOSED / NOT IMPLEMENTED; no production filtering.** User proposal accepted into HazRob Tank research roadmap on 2026-10-18, without changing the original 79-item numbering. Integrate as an explicit sub-study spanning 06.1 evidence fusion, 06.2 Bayesian inference, 06.7 evidence conflict and 06.9 abstention, with experimental dependencies on 05.8 detection limits and 08.11 interference.

## Chemical correction
An H2 sensor measures molecular H2 under its response mechanism; a negative signal does NOT rule out H-containing molecules (e.g. methane, ethanol, ammonia). Similarly CO/CO2 negativity does NOT eliminate carbon compounds; NOx negativity does NOT eliminate all nitrogen compounds or nitrate functional groups; SO2/SOx negativity does NOT eliminate sulfur-containing molecules or sulfates. Many MQ sensors are cross-sensitive and non-specific.

## Research specification
Build a gas x sensor x concentration x environment response matrix, with per-cell source/provenance, expected response direction, calibration/blank detection limit, sensitivity/false-negative rate, uncertainty, warmup, response time, temperature, humidity, drift and interference. Distinguish NOT_DETECTED from NOT_MEASURED, FAULT, OUT_OF_RANGE and UNKNOWN_RESPONSE. A negative observation should adjust gas candidate ranking only when the gas-specific sensor sensitivity at the hypothesized concentration is validated. Prefer soft exclusion via empirically justified P(negative | gas, concentration, environment) in a properly specified likelihood model; do not multiply correlated sensor evidence as independent. Hard exclusion is prohibited by default and requires independent evidence of near-zero false-negative risk under the exact operating conditions; even then safety decisions need separate validation.

## Acceptance criteria
- Per-sensor/compound negative-response matrix with traceable experimental provenance.
- Explicit false-negative and LOD/LOQ characterization, cross-sensitivity and mixtures.
- Dependence-aware combination; ablation on real held-out sessions, devices, concentrations, humidity/temperature and unknown gases.
- Show ranking change and abstention reasons; avoid silent elimination.
- Demonstrate improved selective accuracy and no increase in dangerous false exclusions before production consideration.

Next implementation in original sequence: 06.3 probability calibration. Later 06.7 will incorporate explicit negative-evidence contradiction study. No APK, firmware, or production ChemCore changes.
