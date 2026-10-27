# HazRob Tank V4 — owner-authorized diagnostic build request

Date: 2026-10-27

The owner originally directed NO Android build until additional changes. After these
changes, the owner explicitly requested a final source/UI/menus/charts review and
to proceed to a build if no outstanding SOURCE blocker remained.

This file deliberately triggers **one** Android GitHub Actions diagnostic build
through the narrowly scoped `paths` filter in
`.github/workflows/build-tank-android-v4.yml`.

Scope: compile and run Gradle unit/contract tests, verify off-line ChemCore Stable
asset contents and hashes, and create a clearly marked **UNRELEASED** APK if CI passes.

This request does **not** authorize claiming gas-identification accuracy, measured
ppm, occupational exposure certification, hardware NVS power-loss validation,
MQ7/MQ9 heater qualification, or device/UI field acceptance.

Source/UI pre-build review: operator and developer screens, layout/text contrast,
all five developer destinations, report PDF/XLSX selection, historical ChemCore,
firmware R0 identity and sample counts, and source-only Stable 4.0 manifest
compatibility. Newly added Java/JUnit test sources have not previously been run;
a failing CI is a blocker requiring repairs, not permission to ignore tests.

Do not modify this request file for ordinary code changes. Further build requests
must be explicitly authorized and use a new controlled request.

## Explicit second build authorization — 2026-10-27 17:26 UTC (20:26 Asia/Aden)
Owner requested generation of standalone Tank V4 APK and independently downloadable
ESP32 Arduino ZIP. This marker authorizes GitHub Actions Android Gradle diagnostics,
test compilation and APK assembly from the current canonical V4 branch, not
automatic scientific certification or hardware deployment.

Unlike source-only phases, the owner has expressly lifted the build hold.
Do not erase user data, silently ignore failed tests, publish a stable signed
release without verifying the signing identity, or claim a produced APK when
its CI did not complete. Record this run and its SHA in the V4 ledger.

### Authorized diagnostic retry (2026-10-27 UTC)
First authorized run 40550809159 failed at the static prebuild gate, because its assertion
referenced an obsolete synchronous selected-site query. Source test corrected in
b3d9ad2 to assert the new worker-thread `approvedSessionsForSite(effectiveSite)`.
The owner's existing permission to build V4 includes this diagnostic retry;
keep meaningful tests enabled and record any fresh failure before an APK claim.

### Authorized V4 test-suite remediation retry
Run 40550963771 compiled the Tank V4 main Java successfully and executed **711 JUnit tests: 696 passed, 15 failed**. Failure logs identify legacy HR069 car/version/repository assertions plus obsolete synchronous-SQLite UI test expectations and a firmware R0 provenance fixture missing the now-mandatory sample count. The 15 named failures were individually reviewed and corresponding Tank V4 test overlays committed, preserving safety tests and approved-only report/database semantics. This repeat run is explicitly covered by the owner's 27 October permission to produce the new diagnostic APK, contingent on all tests being green.

### Authorized final contract-text retry
Run 40552169227 completed 711 Java tests with one failing inherited review-staging wording assertion (HR069 source string). No production code was weakened; commit d018a4d updates the test to assert the actual Tank-specific staging action. Owner's explicit authorization extends to this remedial diagnostic retry. APK publication remains conditioned on a green test suite and artifact verification.
