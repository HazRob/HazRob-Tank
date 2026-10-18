# 03.2 Similarity analysis — HazRob Tank research prototype

Compares aligned numeric fingerprint vectors using cosine similarity, Euclidean, Manhattan, and scaled Euclidean distance. Nearest-reference retrieval uses scaled Euclidean distance and deterministic tie-breaking. Cosine is undefined for zero-norm vectors and returns None. Scaling must be fitted on training data only and frozen for held-out sessions.

**Limits:** similarity is not calibrated probability or gas identity. Distance thresholds must be estimated from training data and validated across held-out sessions, devices, concentrations, humidity and mixtures. Cosine can be high even when signal magnitude differs strongly; Euclidean may be dominated by large-scale features. Sensor order, units, MQ7/MQ9 heater phases, baseline quality and environmental conditions must match before comparison.

16 local synthetic unit tests passed on 2026-10-18. GitHub clean-checkout and real Tank data validation pending. Run `cd research/techniques/03.2 && python -m unittest -v`. Research only; no production integration. Status PROTOTYPE.
