# 04.9 Physics + ML — HazRob Tank research prototype (feature engineering only)

This **PARTIAL_PROTOTYPE** implements candidate-conditioned, physics-inspired feature augmentation from documented MQ Rs/R0 versus ppm reference curves: log10 observed response, log-log interpolated reference response, per-channel residuals, missing/out-of-range masks and residual-based candidate ranking. **No machine-learning model is fitted in this version**. The next integration must use candidate-specific features in a leakage-safe ranking/classification pipeline; never select a candidate's feature vector using the true gas label of a test sample.

**Known independently measured concentration is mandatory.** This prototype cannot analyze unknown-concentration field samples, infer chemical identity or identify mixtures. No extrapolation beyond reference curve ranges. Manufacturer curves require explicit provenance and calibration details; reference curves in tests are synthetic, not MQ manufacturer data. Heater cycle, R0, humidity, temperature, gas mixtures and independent concentration measurement matter.

Fair evaluation must compare physics-only, data-only and combined pipelines using identical grouped session/device splits, fit all learned transformations on training folds only, and report coverage and abstention. Candidate residuals with different valid-channel counts are not directly comparable. Never use this as an operational alarm.

16 synthetic tests authored; clean-checkout execution and real Tank validation pending. Run `cd research/techniques/04.9 && python -m unittest -v`. No production modifications.
