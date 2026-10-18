# 04.6 Random Forest — HazRob Tank research prototype

Implements a small **bagged decision-tree ensemble** with bootstrap sampling, randomized feature subsets at each split, Gini impurity, maximum depth, minimum leaf size and reproducible seed. Returns majority-vote class and vote fractions. **Vote fractions are not calibrated gas identity probabilities.** This is a minimal pure-Python research baseline, not a vetted/scalable production implementation.

Select tree count, max depth, min leaf and feature subset size using training-only grouped cross-validation. Evaluate on held-out **sessions/devices/concentrations**, avoiding correlated readings leaking between folds. Consider MQ heater phases, environmental confounding, imbalanced labels, unknown gases and mixtures. No missing-data imputation, out-of-bag evaluation, feature importance, uncertainty calibration or class-balanced bootstrap yet. Large datasets may be slow because all candidate split thresholds are enumerated. Do not use for operational hazard alarms.

16 synthetic unit tests authored; clean-checkout execution and real Tank validation pending. Run `cd research/techniques/04.6 && python -m unittest -v`. No production changes. Status PROTOTYPE.
