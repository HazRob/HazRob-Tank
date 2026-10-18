# 02.2 Scaling — HazRob Tank research prototype

Implements per-feature standard scaling (population standard deviation) and robust scaling (median and interpolated IQR). Fitted parameters must be derived from the training split only, then applied unchanged to validation/test and deployment records. Do not recompute scaler parameters on held-out data. Constant features are flagged and mapped to zero for their training value; out-of-distribution deviations remain visible.

The feature matrix must preserve an explicit ordered sensor/feature schema, measurement units, heater phase, environmental context, and calibration version. Never mix raw ADC, Rs, Rs/R0 or log ratios as if they were interchangeable. In time-series and multi-session benchmarks, split by entire sessions/devices/concentrations before fitting the scaler; avoid per-window leakage.

13 unit tests authored, run: `cd research/techniques/02.2 && python -m unittest -v`. Real Tank measurements and clean-checkout test run remain pending. No production code changes. Status: PROTOTYPE.
