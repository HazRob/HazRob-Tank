# 03.4 LDA / PLS-DA — HazRob Tank research prototype (partial)

Two transparent **binary-only** supervised projection baselines: ridge-regularized Fisher LDA direction and one-component PLS1-DA covariance direction. These are **not full multiclass LDA or multicomponent PLS-DA**, not trained classification systems, and not calibrated gas identity probabilities. A full multiclass/multicomponent implementation and numerical reference comparisons remain open tasks before 03.4 can be considered scientifically complete.

Fit models only on training sessions, including preprocessing/scaling. Apply fixed projection to held-out sessions and compare with unsupervised PCA (03.1) and class overlap (03.3). PLS1-DA uses ±1 training targets, LDA uses pooled within-class scatter and a positive ridge; choose regularization only within training folds. Never interpret training separation as held-out accuracy. Control concentration, humidity, device and MQ heater phase; check label integrity and avoid session leakage.

14 synthetic test cases authored; clean checkout and real Tank validation pending. Run `cd research/techniques/03.4 && python -m unittest -v`. Research only; no production integration. Status: PARTIAL PROTOTYPE.
