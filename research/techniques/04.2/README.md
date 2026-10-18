# 04.2 Fingerprint matching — HazRob Tank research prototype

Builds a labelled reference-exemplar library from **training-only** aligned multichannel numeric sensor features, then ranks classes by their nearest exemplar's scaled Euclidean distance. A fixed externally supplied distance threshold optionally produces ABSTAIN_DISTANCE. A reference match is **not a verified gas identification**, confidence score or concentration estimate.

The fingerprint must have a documented consistent feature schema: MQ channel IDs and order, Rs/R0 vs ADC units, R0 method, heater phase, acquisition window, environmental conditions and firmware version. Normalize/scale using training data only. Training exemplars may contain repeated correlated samples; group by independent sessions and evaluate on held-out sessions, concentrations, devices and mixtures. Nearest-exemplar ranking can favor densely sampled classes; benchmark balanced alternatives. Unknown gases and mixtures are not solved. Threshold selection requires independent calibration; no operational alarm/decision use.

16 synthetic unit tests authored; clean-checkout execution and real Tank validation pending. Run `cd research/techniques/04.2 && python -m unittest -v`. Research only; no production changes. Status PROTOTYPE.
