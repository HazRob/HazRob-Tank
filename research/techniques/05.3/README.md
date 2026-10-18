# 05.3 Nonlinear Regression — HazRob Tank

**PROTOTYPE:** fit a quadratic polynomial in log10(ppm) to log10(Rs/R0), using ordinary/ridge-regularized least squares and standardized log-concentration. Requires >=4 distinct independently known concentrations for one identified gas and one MQ sensor, and measured Rs and R0 in ohms. Rejects a nonmonotone/flat fitted curve across the calibrated range because inverse concentration is ambiguous. Inverse estimation uses bisection and abstains outside the fitted endpoint response range; no extrapolation.

Quadratic log-log fit is a flexible empirical model, **not a physical law** and not a certified sensor calibration. Monotonicity of fitted curve does not establish monotonicity of raw noisy observations. Ridge regularization is applied to linear/quadratic coefficients, not intercept. In-sample RMSE is not independent predictive accuracy. Model selection (05.1 linear, 05.2 piecewise, 05.3 quadratic) requires identical held-out concentration levels, sessions and devices, with hyperparameters chosen using training folds only. No confidence intervals, uncertainty propagation, heater/humidity correction, drift, mixtures, unknown gas identity or safety alarms.

16 synthetic tests authored; clean-checkout execution and real Tank validation pending. Run `cd research/techniques/05.3 && python -m unittest -v`. No production changes.
