# 10.1 Negative Evidence & Sensor-Guided Exclusion — PARTIAL_PROTOTYPE

Research-only calibrated Bayesian non-detection likelihood update. Uses supplied sensor-specific P(detection | gas, concentration, time window, environment, sensor health) and gas priors. Missing or invalid quality channels contribute no evidence. Probability floor prevents hard elimination. Multiple sensors are treated conditionally independent by this simple baseline: this assumption MUST be tested; correlated MQ sensors may cause severe overconfidence. No empirical detection-power calibration exists in this module.

8/8 local synthetic unit tests executed successfully on 2026-10-18 using Python unittest. To rerun: `cd research/techniques/10.1 && python -m unittest -v`.

**Not production-ready:** Need independent reference gas exposures, blanks, concentration strata, sensor response latency, drift/environment tests, dependence correction, held-out calibration and abstention validation. Never use for safety alarms, hard chemical exclusion, or ppm reporting. No Android, ESP32 or production ChemCore changes.
