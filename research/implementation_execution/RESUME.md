# PRIMARY RECOVERY CHECKPOINT — V4 BATCH01 G1 ENGINEERING COMPLETE (2026-10-21)

**Next task:** start **V4 batch02**, ten IDs `02.4,02.5,02.6,02.7,02.8,03.1,03.2,03.3,03.4,03.5`; first work 02.4 dynamic segmental average differentiation vs existing temporal features and 02.5 source-qualified pure/complex fingerprints. **No production changes.**

- **Snapshot:** V4 source analysis `PLAN_80_V4.json` and [PLAN_80_V4_AR.md](PLAN_80_V4_AR.md) with 80 detailed work orders; source catalogue pinned blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f`.
- **Batch01:** IDs `01.1–01.7,02.1–02.3` **10/10 newly developed at software engineering/G1 level**, with [336/336 Python tests pass, Actions #39545362103](https://github.com/HazRob/HazRob-Tank/actions/runs/39545362103), tested commit `420908b38fee01e3c7c4ffc1a167b6254f86fff2`.
- **Authoritative ledger:** `EXECUTION_LEDGER_2026-10-21.json`; **audited per-ID table and known defects:** `BATCH01_EVIDENCE_AR.md`.
- **G2 scientific real dataset benchmarks newly added:** no claim at batch01. Historical G2 source-eligibility 80/80 remains intact; per-ID G2-V4 source data evaluation and reproducible holdings are still required. **Tank physical G3 0/80**. This is not a gas/ppm safety certification.
- **Parallel V2 research implementations:** 01.2, 01.3, 02.2 have alternative source-aware versions present in research branch; harmonize interfaces and test scientific equivalence before using either from actual ChemCore. Do not promote any version to HR069, Android Tank or MCU firmware without separate request and measured standards.
- **Required continuity steps:** (1) inspect current research branch HEAD and PR #6; (2) validate updated ledger with `python research/implementation_execution/validate_plan_v4.py`; (3) inspect batch01 CI and failed attempts for audit; (4) read V4 exact 02.4 work order and central sources `CN-RSCH-01,SYS-17,JP-FIG-02,MQ-EXT-10`; (5) create 02.4 new research code/tests, verify CI and write `BATCH02_EVIDENCE_AR.md`.
- **Scientific blocker:** No Tank-specific true reference gas exposures with measured RL/R0/heater/BME and separate sessions. No certified ppm, toxic gas sensing or G3 claims.
- **User reporting cadence:** summarize progress for user on completion of *each ten-technique batch*; preserve all per-ID commits and unresolved data requirements continuously.

---

# LATEST RESTART POINT — V4 BATCH01 RESEARCH IMPLEMENTATION (2026-10-21)

- **HEAD verified CI:** `18dbc262afa28aa2005dd03b190c12ca354126c1`, [GitHub Actions #39544492918](https://github.com/HazRob/HazRob-Tank/actions/runs/39544492918): **207/207 unit tests passed across IDs 01.1–02.3**.
- **New source-aware engineering prototypes:** IDs `01.1`, `01.2`, `01.3` — **3/10 newly developed in batch01**. `01.4`, `01.5`, `01.6`, `01.7`, `02.1`, `02.2`, `02.3` have only historical software prototypes despite passing historical tests.
- **Ledger source:** `research/implementation_execution/EXECUTION_LEDGER_2026-10-21.json`; **batch evidence:** `research/implementation_execution/BATCH01_EVIDENCE_AR.md`. Follow their updated statuses; an older paragraph below recording 01.1/01.2 as CODE_COMMITTED_CI_PENDING is now historical, not the current status.
- **Main G1/G2/G3 counts remain:** 80/80 historical G1; 80/80 external-source G2 eligibility; **0/80 Tank real-gas G3**, and no activated ChemCore/Android/ESP32 changes.
- **Next task:** technical scientific contracts and implemented evidence/test cases for **01.4 heater-state metadata**, then `01.5` environmental reference calibration, `01.6` chamber signal quality/flow and filter-state; proceed through remaining IDs to complete the first batch of 10, preserving 10-per-batch reporting.
- **Known merge concern:** alternative V2 prototypes of 01.2/01.3 are currently present; choose one audited canonical module per technique before any integration. Do not use source metadata SHA field as proof actual bytes were hashed.
- **Immutable scientific reference source:** pinned central catalogue blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` used in V4 plan; refresh bibliography only with explicit source revision record. PR #6 is a research review, not an implicit production merge.

---

# HazRob Tank ChemCore V4 — recovery checkpoint (2026-10-21)

**Branch:** `research/chemcore80-central-evidence-gap-analysis-20261021` (**research-only**); canonical `main` untouched until PR review.

## Stable and immutable evidence
- `research/implementation_execution/PLAN_80_V4.json`: 80 named technique work orders, immutable baseline source catalog Git blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f`, eight original batches of 10, per-ID sources, testing and G3 criteria.
- `research/implementation_execution/PLAN_80_V4_AR.md`: full Arabic procedures for all 80.
- `research/systems_analysis/CHEMCORE80_REFERENCE_COMPARATIVE_SYSTEMS_ANALYSIS_2026-10-21_AR.md` and `TECH80_CENTRAL_REFERENCE_CROSSWALK_2026-10-21.json`: source-to-technique rationale, 23 redesign / 52 upgrades / 5 retained.
- `research/implementation_execution/EXECUTION_LEDGER_2026-10-21.json`: latest authoritative V4 execution statuses (NOT historical G1 or G2 record).

## Historical and new gates
Historical `research/PROGRESS_80.json`: 80/80 original G1 software prototype & G2 reference-eligibility reviewed (49 external data operations, 29 blocked, 2 theoretical), **Tank real MQ8 physical G3 = 0/80**. No operation in this research branch may upgrade G3 absent traceable actual Tank reference exposures. No APK, ESP32/ESP32-CAM, original HazRob, production ChemCore edits.

## Run order after interruption
1. Fetch newest branch HEAD and `EXECUTION_LEDGER_2026-10-21.json`; inspect PR and CI; do not assume any unfinished operation committed.
2. Review `PLAN_80_V4.json` and `PLAN_80_V4_AR.md`, then source catalog pinned SHA.
3. Continue earliest batch-01 ID with `PLANNED_NOT_EXECUTED` / `IMPLEMENTED_TESTS_PENDING`; do not reimplement committed validated code. Validate each stage with CI, record run URL/commit.
4. Update `BATCH01_EVIDENCE_AR.md` after each work item; publish cumulative recap only when all 10 complete, preserve blocked/negative findings.
5. Keep original historical `research/PROGRESS_80.json` and main untouched; this research branch is a proposal until PR merges.

## Physical blockers
- Measured Tank RL/Vc/ADC divider mapping, physical MQ instance IDs, heater HIGH/LOW cycle, R0 protocol, controlled blank and certified gas references, BME280 and actual session groups are required for G3.
- Publisher curves / cross-device R0 are priors not calibrations. Mock tests and Mendeley / UCI external arrays may help G1/G2 only.
