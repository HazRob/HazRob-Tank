# 03.3 Class overlap — HazRob Tank research prototype

Descriptive leave-one-out nearest-neighbor overlap diagnostic. For each sample, compare nearest *other sample of same class* with nearest sample of a different class using Euclidean distance with explicit feature scales. Flag ambiguity when the other-class neighbor is equally near or nearer; report overall and per-class ambiguity fractions and signed distance margins. This is **not** a calibrated probability, classification accuracy, or evidence of gas identity.

Critical caveats: samples from the same recording are correlated; apparent separation can reflect concentration, session, sensor device, humidity or calibration rather than chemical selectivity. Split by whole independent sessions and compare cross-session results. Fit any feature scales and preprocessing on training data only. Assess sensitivity to concentration, noise and class imbalance. Use controlled real Tank recordings before interpretation.

16 synthetic unit tests authored; execution from clean checkout pending. Run `cd research/techniques/03.3 && python -m unittest -v`. No production changes. Status PROTOTYPE.
