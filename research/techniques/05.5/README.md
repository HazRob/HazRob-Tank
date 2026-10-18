# 05.5 PLS Regression — HazRob Tank

Research **PROTOTYPE**: PLS1 regression for one independently identified gas, mapping aligned log10(Rs/R0) values of MQ sensors to log10(ppm). Uses training-only per-sensor standardization, centered target, sequential NIPALS-style latent components and deflation. The number of extracted components may be fewer than requested when residual covariance is degenerate. No external ML dependency.

PLS addresses correlated channels by projecting sensor responses onto supervised latent components, but this does not guarantee better generalization than ridge (05.4). Select number of components using training-only grouped cross-validation; hold out concentration levels, sessions and devices. Beware ambient conditions, drift, heater phases, mixture cross-sensitivity, class identity uncertainty and data leakage. No extrapolation of predicted ppm beyond training concentration range, though feature-space OOD remains unhandled. No uncertainty intervals, identity inference or safety certification.

16 synthetic tests authored; clean-checkout execution and real Tank validation pending. Run `cd research/techniques/05.5 && python -m unittest -v`. No production modifications.
