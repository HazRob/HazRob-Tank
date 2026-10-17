# Phase 1 implementation log — HazRob Tank
Last updated: 2026-10-17 23:56 UTC. Work branch: `develop`. Original SERVOC-Android is untouched.

## Checkpoint P1-01 — requirements APPROVED
- Commit `7253eb6a5fe2df47a44da3a3e29b7690b9bb10ac`
- `docs/requirements/PHASE-1-ANDROID-MEASUREMENT-SESSIONS.md`
- Defines instant vs documented acquisition, four measurement panes, two field-session panes, one approved measurement per field session, generic live MQ warnings, physical data integration, PM placeholder, smart report boundaries.

## Checkpoint P1-02 — lifecycle policy and tests COMMITTED (NOT YET CI-VERIFIED)
- Policy commit `e2d9e4a082708cf64a01cbdf74f4f8145cdd0bb3`
- Test commit `8a5c8ec1f96a7434abc7e08cc9ef3c005137412b`
- `TankAcquisitionPolicy.java` models NEW/WARMUP/MEASURING/COMPLETED/CANCELLED/APPROVED; only approved is reportable; user-visible status collapses to جديدة/معتمدة.
- `TankAcquisitionPolicyTest.java` covers visibility, approval, retries, deletion precondition, generic warnings.
- These are initial policy contracts, not yet wired into database and Android UI.

## Checkpoint P1-03 — measurement navigation shell COMMITTED (NOT YET CI-VERIFIED)
- Commit `ef1e6bfe26d3bc9087730a644905d9f8b384f403`
- `android/app/src/main/java/com/servoc/app/ui/MeasurementsTab.java`
- Four mobile RTL panes: search/tracking landing, analysis (existing live/analysis/results reachable through nested buttons), existing physical, PM placeholder.
- Existing ChemCore candidate controls and live telemetry remain reachable.
- Search landing is intentionally NOT yet a functional chemical search; no simulated gas values.
- Particle pane explicitly marks hardware unavailable; no fake measurements.
- UI shell must be compiled and device-tested before calling this completed.

## Next implementation order
1. Trigger existing GitHub Actions workflow on `main` to checkout `develop` and run Gradle tests/build; fix any compile or test regressions.
2. Implement functional chemical-family + name/CAS search using actual ChemicalRepository and availability flags; connect target selection to existing tracking workflow and top-ten candidate selection.
3. Implement field session 1:1 acquisition + approval persistence and migration; unit tests; distinguish instant from documented recorder sessions.
4. Replace FieldSessionsTab with إدارة/التقارير, dynamic dropdowns, approved-only reporting, confirmation dialogs, proper result checking.
5. Physical instantaneous PDF snapshot, ChemCore environmental input verification, PM placeholders and future interface contract.
6. Review smart report templates separately with owner, end-to-end regression and APK build.

## Important limits
- No new APK has been built for these commits yet.
- No device/hardware integration testing has been performed for this phase.
- Do not claim search, session approval, smart reports or source direction are complete based on the shell.
