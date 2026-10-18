# 04.1 MQ curve matching — HazRob Tank research prototype

Uses externally supplied per-gas/per-sensor **positive** reference points (ppm, Rs/R0) with piecewise **log-log interpolation**. At a **known, independently controlled concentration**, compares observed aligned Rs/R0 readings to reference curves using root-mean-square log10 ratio residual and ranks eligible candidates. Requires at least two matching sensors by default; no extrapolation outside any curve's documented concentration range.

**No factory curves or measured gas readings are bundled.** Do not treat synthetic examples as manufacturer data. Curve provenance, MQ model, R0 calibration gas/procedure, heater phase (especially MQ7/MQ9), exposure time, temperature/humidity, sensor uncertainty, curve digitization accuracy and concentration units must be recorded before application. This baseline does **not** infer unknown concentration, identify mixtures, calculate calibrated probability or guarantee gas identity. Candidate rankings with unequal channel coverage are not directly comparable; enforce matched channel sets in a later iteration.

16 synthetic tests authored; execution from clean checkout and real Tank validation pending. Run `cd research/techniques/04.1 && python -m unittest -v`. Research only; no production changes. Status PROTOTYPE.
