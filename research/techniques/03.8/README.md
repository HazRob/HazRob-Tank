# 03.8 Identifiability limits — HazRob Tank research prototype

Empirical *near-collision* diagnostic for pairs of different gas labels and conservative query screening against labelled reference fingerprints. Uses a fixed, externally supplied scaled-Euclidean threshold. Cross-class pairs within threshold are flagged; a query close to multiple reference classes is AMBIGUOUS, one class is SINGLE_REFERENCE_CLASS (explicitly **not** a verified identification), and none is INSUFFICIENT_EVIDENCE.

This is **not** a proof of physical identifiability, chemical specificity, detection limit, calibrated confidence or gas absence. Threshold must be fitted on training sessions only and validated on independent controlled concentrations, devices and environmental conditions. Correlated repeated readings can falsely exaggerate certainty. Open-set unknown gases, mixtures, sensor drift and background interference remain unsolved. This is an exploratory abstention *candidate*, not an operational safety gate.

16 synthetic unit tests authored; clean checkout and real Tank data validation pending. Run `cd research/techniques/03.8 && python -m unittest -v`. No production changes. Status PROTOTYPE.
