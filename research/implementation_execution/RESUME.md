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
