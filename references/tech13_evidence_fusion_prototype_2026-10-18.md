# Imported legacy research reference for HazRob Tank
Original: sohpany/HazRob-Lab/references/tech13_evidence_fusion_prototype_2026-10-18.md
Imported 2026-10-18. Research evidence only; no production integration. Validation claims retain original limitations.

# Technique 13 — Evidence Fusion prototype (2026-10-18)

Status: research-only prototype; production ChemCore/APK/firmware unchanged.

Implemented in local `evidence_fusion_v1.py`: explicit provenance groups, averaging of correlated methods within a group, equal group contribution, conservative conflict detection, margin/support thresholds, and abstention without independent calibration. `test_evidence_fusion_v1.py`: **7/7 unit tests passed**.

**Limits:** group independence is a caller assumption; scores are not calibrated probabilities; equal weights are heuristic; no real-gas fusion benchmark or unknown-class held-out test yet. `independently_calibrated=True` is an external declaration, not independent proof. `ACCEPT_SCREENING` never means safe atmosphere.

Next: compare out-of-fold predictions from genuinely distinct evidence families on UCI 1081, calibration strictly within training folds, leave-one-gas-out rejection, coverage-risk curves and held-out sessions/devices. Full local source/test/report bundle `HazRob_Tech13_Evidence_Fusion_v1.zip` generated in conversation.