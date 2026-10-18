# 06.5 Open-Set Recognition — HazRob Tank

**PARTIAL_PROTOTYPE:** Euclidean nearest-class-centroid known-gas baseline and conservative unknown-or-unsupported rejection when distance exceeds a threshold. Prototype centroids and a simple training-distance quantile threshold are fitted from known examples; this is **optimistically biased** and NOT validated or production-suitable. Returns the nearest known class even for rejected observations solely for audit, never as verified identity. Descriptive known acceptance and unknown rejection rates can be evaluated on external held-out test vectors.

Scientific next steps: fit preprocessing only on training sessions; calibrate threshold on **disjoint** known validation and representative held-out unknown classes, preserve independent final tests, compare Mahalanobis/EVT/open-max/energy baselines, characterize sensitivity to concentration, drift, temperature/humidity, device shifts, interferents and mixtures. Unknown rejection does not establish a new chemical identity: faults, uncalibrated levels, sensor saturation, and out-of-domain operation may look unknown. Distinguish UNKNOWN_OR_UNSUPPORTED from gas detection. No safety decisions.

16 synthetic tests authored, clean-checkout execution and real Tank validation pending. Run `cd research/techniques/06.5 && python -m unittest -v`. No production changes.
