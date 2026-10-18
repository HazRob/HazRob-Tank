# 05.1 Single-sensor calibration — HazRob Tank

Research **PROTOTYPE**: ordinary least-squares calibration of log10(Rs/R0) versus log10(concentration ppm) for **one known gas** and **one MQ sensor**, with at least three distinct independently verified positive reference concentrations. Fits slope/intercept and in-sample log-space RMSE; inversely estimates ppm only inside the observed concentration range, otherwise explicitly abstains.

**R0 is not a universal constant** and must be measured/documented under sensor-specific reference conditions (heater stabilization, reference gas/air, temperature, humidity and aging). This implementation accepts R0 in **ohms** (convert from kΩ by multiplying 1000). The Rs readings and R0 must use identical resistance units. Manufacturer sensitivity curves cannot replace an instrument-traceable multipoint calibration. Cross-sensitivity, mixtures, drift, saturation and nonlinear segments can invalidate this power-law model. Gas identity is assumed independently established: it is not inferred here.

A low in-sample RMSE is not independent accuracy. Evaluate held-out concentration levels, sessions, devices, environmental conditions and uncertainty before any quantitative or safety claims. No LOD/LOQ, confidence intervals, drift correction or uncertainty propagation yet.

16 synthetic tests authored; clean-checkout execution and real Tank validation pending. Run `cd research/techniques/05.1 && python -m unittest -v`. No production changes.
