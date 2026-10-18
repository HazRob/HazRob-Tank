# 04.4 Logistic regression (multiclass softmax) — HazRob Tank research prototype

Implements a transparent full-batch gradient-descent multiclass softmax classifier with L2 regularization (unpenalized intercept), fitted training-only feature mean and standard deviation, and deterministic class ordering. Predicts a label and softmax scores. **Softmax scores are not independently calibrated probabilities of gas identity.** No production alarm or chemical safety use.

Fit preprocessing, L2 strength, learning rate and training epochs within training folds; validate on untouched **session/device grouped** holdouts with known concentrations and ambient conditions. Class imbalance, MQ heater cycles, mixtures, open-set unknowns, feature drift and mislabeling require explicit analysis. Gradient descent has no convergence diagnostics or guaranteed optimum and is not a replacement for a vetted numerical solver. Calibrate predictive probabilities only using separate validation data, after model selection, and verify reliability on held-out data.

16 synthetic unit tests authored; clean checkout execution and real Tank validation pending. Run `cd research/techniques/04.4 && python -m unittest -v`. Research only; no production changes. Status PROTOTYPE.
