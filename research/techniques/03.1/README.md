# 03.1 PCA — HazRob Tank research prototype

Unsupervised principal component analysis on a training-only numeric matrix, returning principal directions, explained variance ratios and transformed scores. Implements a small pure-Python symmetric Jacobi eigensolver for transparent research inspection, not optimized production numerical linear algebra. Input features must first be selected and scaled appropriately (02.2), using training-only fit; the PCA fit itself must not see held-out sessions or devices.

PCA maximizes variance, **not class separation**, so visual separation cannot prove chemical identification, detection limits or predictive accuracy. Label overlays must be held-out and reviewed for concentration, session, device and humidity confounding. Verify with numerical reference libraries and real Tank data before using any conclusions. The eigensolver has no residual error report and uses absolute tolerance; further numerical QA is needed for badly conditioned matrices.

14 unit cases authored; clean-checkout execution pending. Run `cd research/techniques/03.1 && python -m unittest -v`. No production changes. Status PROTOTYPE.
