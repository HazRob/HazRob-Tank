# 04.7 Gradient boosting / XGBoost investigation — HazRob Tank

**PARTIAL_PROTOTYPE — NOT XGBoost.** This is a transparent multiclass additive ensemble of regression decision stumps fitted to negative softmax gradients (residuals), with shrinkage and regularized leaf means. It illustrates sequential error correction and permits a first comparison with 04.6 Random Forest. It does NOT implement XGBoost's second-order Newton gain, exact regularized objective, depth-limited trees, row/column subsampling, missing-value routing, pruning, monotonic constraints or optimized training. Do not label results as XGBoost performance.

Choose training settings within training-only grouped folds; evaluate on held-out sessions, devices, concentrations and ambient conditions using the same split as 04.3–04.6. Include class imbalance, correlated time-series, MQ heater phase and unknown mixtures. Scores are **not calibrated probabilities**. This prototype has no operational gas identification or hazard alarm role.

16 synthetic unit tests authored; clean-checkout execution and real Tank validation pending. Run `cd research/techniques/04.7 && python -m unittest -v`. No production modifications.
