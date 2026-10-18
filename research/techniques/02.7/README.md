# 02.7 Feature selection — HazRob Tank research prototype

Training-only univariate Fisher-like score: between-class sum of squares divided by within-class sum of squares. Returns deterministic ranked feature names and indices, and projects held-out matrices using the *unchanged* selected indices. Constant features score zero; perfectly separated training groups score infinity, **not** a claim of out-of-sample accuracy.

**Scientific protocol:** split complete sessions/devices/concentrations before feature selection. Perform feature selection within each cross-validation training fold (nested CV for selecting k/hyperparameters), then evaluate once on held-out data. Never use held-out labels to rank features. Correlated and redundant features can score highly together; multivariate redundancy, sensor cost and stability must be assessed in later stages. This ranking does not identify gases, optimize sensor subsets or validate chemical selectivity.

14 synthetic unit cases authored; clean-checkout execution and real Tank data validation pending. Run `cd research/techniques/02.7 && python -m unittest -v`. No production changes. Status PROTOTYPE.
