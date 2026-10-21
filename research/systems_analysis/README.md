# ChemCore80 — source-driven systems-analysis and implementation workspace

**Scope:** Independent HazRob Tank *research code and documentation only*. Never treat these files as an activated ChemCore Stable or production app/firmware. **G3 on physical Tank = 0/80.**

## Read in this order
1. [Durable restart checkpoint](RESUME_IMPLEMENTATION_80_2026-10-21.md)
2. [Arabic per-technique 80-ID implementation plan](IMPLEMENTATION_PLAN_80_2026-10-21_AR.md)
3. [Machine-readable plan, eight batches ×10](IMPLEMENTATION_PLAN_80_2026-10-21.json)
4. [Source-to-technique comparison: why redesign?](CHEMCORE80_REFERENCE_COMPARISON_2026-10-21_AR.md)
5. [80 × references machine matrix](CHEMCORE80_REFERENCE_GAP_MATRIX_2026-10-21.json)
6. [Live implementation status and CI provenance](IMPLEMENTATION_STATUS_80_2026-10-21.json)
7. [Batch 01 worklog: 10 foundational research contracts](batches/BATCH_01_EXECUTION_2026-10-21_AR.md)

## Research tools / tests

- [Guarded measurement/reference contract V2](measurement_reference_contract_v2.py): ADC/RL, R0, source curve, heater state, flow metadata, source-label fingerprint, SHA256 actual bytes, whole-session split, reference-only calibration and blank signal screening.
- [Synthetic plus negative-path test suite](test_measurement_reference_contract_v2.py).
- [Plan and status consistency validator](validate_implementation_plan_v1.py).
- [CI workflow](../../.github/workflows/chemcore80-systems-batch01.yml). Last verified shared research suite [35/35 PASS](https://github.com/HazRob/HazRob-Tank/actions/runs/39527014531); verify a new SHA after any change.

## Source authority
The **single global bibliography** belongs to [HazRob/build-harness reference-bank](https://github.com/HazRob/build-harness/tree/main/library/reference-bank). At analysis freeze, 80 bibliographic records; different from the 80 technique IDs and not 80 independent physical datasets. [Tank research master progress](../PROGRESS_80.json) is the current G1/G2/G3 historical ledger and remains untouched by these research contracts.

Research statuses: **26 REDESIGN / 33 ENHANCE / 14 RETAIN / 7 DEFER**; the first 10 technique IDs have a shared G1 reference-interface only, **not ten fully revalidated scientific implementations**. Keep all physical gas concentration claims blocked pending source/reference validation.
