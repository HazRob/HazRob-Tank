# 06.3 Probability Calibration — HazRob Tank

**PARTIAL_PROTOTYPE.** Multiclass temperature scaling from externally supplied pre-softmax logits and true integer class labels on a **disjoint held-out calibration set**. Grid search minimizes mean multiclass negative log likelihood. Apply the learned positive temperature to later logits with the exact same class order. Includes descriptive top-label expected calibration error (ECE) with fixed-width confidence bins. ECE is sample-size/bin sensitive and can conceal poor classwise calibration. Temperature scaling cannot fix incorrect classification rankings, class shifts, unknown gases, or mixtures.

Never fit temperature on training or final test observations. Split by session/device and concentration to avoid leakage, preserve an untouched final evaluation set, and check NLL, Brier score, reliability curves, per-class calibration and confidence-conditioned error. The discrete candidate grid is not an optimizer guarantee. A probability calibrated on one distribution can be unreliable under drift or OOD. Output is **not certified chemical identity confidence** and not an operational safety decision.

16 synthetic tests authored; clean-checkout execution and real Tank validation pending. Run `cd research/techniques/06.3 && python -m unittest -v`. No production modifications.
