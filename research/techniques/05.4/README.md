# 05.4 Multisensor Regression — HazRob Tank

Research **PROTOTYPE**: regularized multivariate linear regression (ridge) mapping aligned log10(Rs/R0) measurements from D MQ sensors to log10(ppm), conditional on **independently established gas identity**. Each sensor's mean and standard deviation are fitted on training data only. Uses positive ridge penalty on slopes, not intercept, to mitigate collinearity. Requires at least max(5,D+2) rows and >=3 known concentration levels. Refuses predicted concentrations outside the training concentration interval; note this does **not** establish feature-space in-distribution status.

Sensors are not chemically selective, and a multichannel regression can exploit spurious ambient/session effects. Require independently measured concentration standards, aligned sensor ordering, sensor-specific R0 and consistent heater conditions. Compare to 05.1–05.3 with grouped held-out concentrations, sessions and devices; account for humidity/temperature, drift, mixtures and uncertainty. No inference of unknown gas identity, no calibrated intervals, no target-device resource benchmark, no safety-critical use. A lower training RMSE is not proof of higher accuracy.

16 synthetic unit tests authored; clean-checkout execution and real Tank validation pending. Run `cd research/techniques/05.4 && python -m unittest -v`. No production modifications.
