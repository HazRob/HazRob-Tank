# 04.5 Support Vector Machine — HazRob Tank research prototype (linear only)

Implements a **linear** one-vs-rest SVM research baseline using primal hinge-loss subgradient descent, L2 regularization of weights, unregularized intercept and training-only standardization. Produces class decision scores, **not probabilities**. Supports multiclass via independent binary classifiers. Does **not** implement nonlinear/RBF kernels, SMO optimization, support-vector diagnostics or calibrated margins; therefore this is a **PARTIAL_PROTOTYPE** of the broader SVM family.

Training convergence and numerical equivalence to a vetted solver are unverified. Select C and preprocessing with grouped training folds, and assess held-out sessions/devices, concentration ranges, humidity, MQ heater cycles and unknown gas mixtures. Compare fairly with KNN/softmax using identical data splits. Do not use as a production gas alarm or identity decision.

16 synthetic unit tests authored; clean-checkout execution and real Tank validation pending. Run `cd research/techniques/04.5 && python -m unittest -v`. No production changes.
