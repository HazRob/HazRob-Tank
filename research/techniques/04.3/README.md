# 04.3 K-nearest neighbors — HazRob Tank research prototype

Supervised KNN classification of aligned numeric multichannel fingerprints, using fixed positive feature scales, Euclidean distance, and uniform or inverse-distance voting. Returns label, neighbor distances, vote totals and tie flag. Deterministic alphabetical label tie-break is an implementation convention, **not** evidence of gas identity. With inverse-distance voting, exact matches override nonzero-distance neighbors.

Choose K, feature scales and any preprocessing using training folds only; then test on untouched **grouped sessions** and devices. Prevent duplicate/temporally correlated exposure samples from leaking across train/test. Study class imbalance, concentration ranges, sensor drift, temperature/humidity, MQ7/MQ9 heater phases, and unknown mixtures. Votes are **not calibrated probabilities**; production alarms must not rely on this baseline.

16 synthetic unit tests authored; clean checkout execution and real Tank validation pending. Run `cd research/techniques/04.3 && python -m unittest -v`. No production changes. Status PROTOTYPE.
