# 01.7 Sensor baseline drift monitoring — HazRob Tank research prototype

Compares two baseline windows in resistance units (ohms) using robust medians, a log resistance shift and relative median absolute deviation (MAD) quality gate. Reports changes in both directions and refuses to label noisy windows as verified drift.

**Critical limitation:** Baseline differences can arise from ambient gas composition, humidity, temperature, heater state, supply voltage, sensor aging and R0 calibration. This algorithm cannot distinguish those causes without controlled conditions, metadata and repeated reference exposures. The 0.10 log-change and 0.05 relative MAD thresholds are illustrative research choices, not manufacturer drift specifications or alarm thresholds. Never automatically modify R0 in response to this detector.

Data required: sensor ID, stable reference atmosphere, chamber airflow, timestamp, heater phase/voltage, temperature/humidity, supply, calibration record, and session/device ID. Compare same operating conditions or validated compensated values. Longitudinal testing and change-point methods are later research tasks.

Twelve test cases authored; run `cd research/techniques/01.7 && python -m unittest -v`. Status PROTOTYPE. No production integration.
