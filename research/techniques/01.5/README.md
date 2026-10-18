# 01.5 Environmental correction — research prototype

Scope: independent HazRob Tank ChemCore only. No changes to firmware, app or production library.

## Model
BME280 temperature, relative humidity and barometric pressure are used as covariates. For one **fixed sensor, gas, concentration and heater phase**, the optional model is log(Rs_observed)=log(Rs_reference)+beta_T*(T-Tref)+beta_RH*(RH-RHref)+beta_P*(P-Pref). Coefficients are NOT known in advance: fit only on independently controlled calibration measurements with a full-rank environmental design, then evaluate on held-out sessions and conditions. The zero-coefficient default performs no adjustment; it is not evidence of environmental invariance.

Pressure is included as a candidate covariate, not assumed physically relevant. Temperature measured by BME280 near the sensor chamber is not necessarily sensing-element temperature. Avoid confounding gas concentration, heater phase, aging and environmental conditions. No compensation should be applied if calibration evidence is insufficient.

## Software
`environment_correction_v1.py` provides a conditional normalization and an OLS fitting helper (numpy required for fitting). Twelve test cases include synthetic coefficient recovery, validity and rank-deficiency checks. Run `cd research/techniques/01.5 && python -m unittest -v`.

## Limitations
Synthetic test parameters are not manufacturer compensation coefficients. Need real paired controlled exposures, uncertainty intervals, out-of-session testing, and verified BME280 synchronization before research acceptance. Status: PROTOTYPE.
