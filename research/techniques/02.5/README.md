# 02.5 Fingerprints — HazRob Tank research prototype

A fixed, explicitly ordered eight-sensor vector: MQ2, MQ3, MQ4, MQ5, MQ7, MQ8, MQ9, MQ135. For each sensor use log(Rs_exposure/Rs_baseline), retaining signed changes and avoiding division by a potentially unstable zero delta. Signed cosine similarity is provided only as a baseline comparison, NOT calibrated classification probability.

**Provenance/limitations:** prior HazRob-Lab fingerprint_v1.py was identified in local legacy inventory, but no exact matching GitHub file was found during this step; this is a new Tank-specific minimal prototype, not a verified port of the legacy algorithm. Must compare to legacy implementation and reconcile features, weighting and evaluation before claiming equivalent behavior.

Sensor order and data acquisition timing must match the actual Tank wiring; this order is a provisional schema. MQ7/MQ9 heater phase, per-sensor baseline, environment and valid windows must be verified. Cross-sensitivity means fingerprint similarity does not establish gas identity. Concentration can alter fingerprints; mixtures and unknown gases are not addressed. Evaluate with held-out sessions/concentrations and calibrated abstention.

14 unit cases authored, clean checkout execution pending. Run `cd research/techniques/02.5 && python -m unittest -v`. No production changes. Status PROTOTYPE.
