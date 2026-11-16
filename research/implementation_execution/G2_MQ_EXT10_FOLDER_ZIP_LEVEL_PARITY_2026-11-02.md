# G2 — parity of original-folder and ZIP liquid-level completeness (2026-11-02)

**Scope:** MQ-EXT-10 published ethanol source metadata, not Tank calibration or gas ppm certification.

## Defect and fix
The ZIP intake already rejected 65 measurements restricted to one liquid concentration level, whereas the loose-folder preflight accepted 65 distinct files as source-count-complete without checking C1–C6 coverage. The two ingestion paths could therefore produce contradictory source-completeness statuses from equivalent input sets.

A shared `liquid_level_coverage(manifest)` now computes per-code counts and six-level presence, reused by both intake paths. Loose-folder output reports `concentration_code_counts`, `all_six_published_liquid_levels_present`, and `reported_ethanol_count_complete`. A 65-file single-level folder is flagged `REPORTED_COUNT_MISSING_CONCENTRATION_LEVELS` and receives the explicit blocker `MISSING_PUBLISHED_LIQUID_CONCENTRATION_LEVELS`.

## Reproducibility
`python -m unittest discover -s research/implementation_execution -p 'test_g2_mq_ext10*.py' -v`

Three new synthetic folder cases use unique fake filenames and deterministic stubbed inspection metadata: 65 C1-only samples, six files covering six levels, and 65 files covering all six levels. Existing ZIP regression cases independently exercise corresponding outcomes. No synthetic values represent measured exposures; the original archive remains absent. No publisher checksum authentication or independent physical grouping is inferred.

## Qualification boundary
The concentration codes describe ethanol **liquid solution % v/v**, not vapor ppm. Source completeness is a necessary input QA condition only; neither this check nor green CI establishes G2 or G3. No Stable, Android, firmware, or main changes.
