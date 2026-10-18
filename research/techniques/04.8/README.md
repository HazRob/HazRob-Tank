# 04.8 Multilayer Perceptron — HazRob Tank research prototype

A minimal, deterministic **single-hidden-layer** feed-forward neural network with tanh activation, multiclass softmax output, full-batch backpropagation, L2 weight decay and training-only standardization. Architecture: D input features → configurable H hidden neurons → K gas-label outputs. Uses only Python standard library.

This is a research baseline, **not** a calibrated gas detector, proof of chemical specificity or validated AI model. Softmax scores are not reliable posterior probabilities. There is no early stopping, mini-batching, dropout, convergence report, hyperparameter search or temporal modeling. Train/tune with session-grouped splits and assess independent devices, gas concentrations, ambient conditions, mixtures, unknown gases and drift. Control class imbalance and avoid correlated measurement leakage. Compare fairly to 04.3–04.7 on identical splits.

16 synthetic tests authored; clean-checkout execution and real HazRob Tank validation pending. Run `cd research/techniques/04.8 && python -m unittest -v`. No APK, firmware, production ChemCore or original HazRob changes. Status PROTOTYPE.
