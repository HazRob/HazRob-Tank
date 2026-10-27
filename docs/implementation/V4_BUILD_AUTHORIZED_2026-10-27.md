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
