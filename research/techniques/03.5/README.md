# 03.5 Sensor importance — HazRob Tank research prototype

Training-only univariate between/within-class Fisher scores for aligned sensor features. Ranks sensor channels, flags constant channels, and projects held-out records using the frozen selected sensor columns. This is a simple **marginal statistical association**, not sensor causality, chemical selectivity, permutation importance, SHAP, sensor ablation, or proof that a sensor can safely be removed.

Use one comparable scalar feature per sensor and a documented sensor/channel mapping. Fit preprocessing and importance only within training folds; evaluate sensor removal via grouped held-out sessions, concentrations, devices, temperature/humidity and heater cycles. Correlated sensors may each appear important even when redundant; nonlinear and interaction effects are not captured. MQ7/MQ9 heater phase and exposure window alignment must be respected. The score can be infinite on perfectly separated training groups, without guaranteeing generalization.

14 synthetic test cases authored; clean-checkout execution and real Tank validation pending. Run `cd research/techniques/03.5 && python -m unittest -v`. Research only; no production changes. Status PROTOTYPE.
