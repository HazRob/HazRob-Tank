# HazRob Tank — Android Phase 1: Measurement and Field Sessions
Status: APPROVED by project owner, 2026-10-17. Implementation baseline: Tank Android 0.1 on `develop`.

## Scope and UI architecture
Main **القياس** tab has four RTL mobile sub-tabs:
1. **البحث والتتبع** — continuously inspect live MQ telemetry, chemical-family chips, name/CAS search, chemical capabilities table (PPM / fingerprint / curves / molecular: actual library availability, never inferred capability), selected target prominence, other candidate gases beneath. No measurement session required. Source-direction arrow is displayed ONLY in قيادة and must show unknown when direction evidence is insufficient.
2. **التحليل** — time-bounded *instant* acquisition with warmup/measurement durations from settings, progress and locked start button, automatic completion, ChemCore top ten ranked candidates with user-selected Track action, charts, and smart instant report. The selected candidate enters target tracking, not automatically the highest-ranked one.
3. **الفيزيائية** — live physical readings; include temperature, humidity, pressure and other available environmental readings in ChemCore gas analysis inputs; standalone PDF captures the values visible at the instant of the user's request, with units and timestamp.
4. **الجزيئات** — modern live PM UI and future report integration; unavailable until a real PM module is connected. Never synthesize readings or assume PM5/PM20/PM50 hardware support.

Main **الجلسات** tab has two RTL mobile sub-tabs:
1. **الإدارة** — site dropdown; dependent session table with exactly two visible columns: session name, status. Shared add/delete actions contextual to site/session, delete confirmation. A site can be deleted only after its sessions are deleted. Each session represents one physical measurement point and permits one approved acquisition only. Visible statuses exactly **جديدة** or **معتمدة**; cancelled, unmeasured and completed-but-unapproved all show **جديدة**. A new session can be measured again, replacing unapproved attempts. Warmup + timed acquisition end automatically. Start disabled while running, approval/cancellation controls become available only on completion. Do not expose final charts/reports before approval.
2. **التقارير** — site dropdown and dependent approved-session dropdown; selected session smart report, or site smart report aggregating approved sessions only. Deletions must immediately disappear. Detailed smart report content/templates are deferred to a separate design discussion.

## Distinct acquisition lifecycles
**Live sensing**: MQ power-on from قيادة immediately enables per-sensor out-of-normal-range alerts without a measurement session, ChemCore identification, or candidate ranking. Do not equate generic MQ warnings to verified chemical identification.
**Target tracking**: can start from direct chemical lookup or from user selection among top ten candidates after an instant analysis; runs from live telemetry without creating a documented field session.
**Instant analysis**: each press starts a new bounded acquisition and replaces the previous instant acquisition context; data are held for that acquisition's smart report. Not attached to sites or field sessions.
**Documented acquisition**: requires site + one named session, automatically completes after configured duration, can be approved once, then appears in reports. Physical readings and future PM channels are included when available.

## Safety, data and implementation invariants
- ChemCore identity and concentration are probabilistic/model-dependent; an absent H2S candidate must never be shown as proof of confined-space safety. Avoid inventing PPM from noncalibrated MQ curves.
- Preserve current Tank servo, Echo warning-only, emergency-stop, network, developer/reference session isolation, ChemCore libraries, and R0 calibration.
- Do not modify `sohpany/SERVOC-Android`; Tank changes are overlays on `HazRob/HazRob-Tank` `develop`.
- Never persist instant acquisitions as user field sessions by accident. Do not generate approved field reports from incomplete or unapproved captures.
- For release, run existing Gradle unit tests, add regression tests for session policy and UI state, build signed debug APK in existing GitHub Actions workflow, then test on hardware.

## Work breakdown
1. Extract reusable acquisition lifecycle/state model and tests.
2. Implement modern four-pane measurement UI and target picker, preserving live alarms.
3. Implement single-measurement-per-session field repository migration and approval lifecycle, with tests.
4. Implement two-pane field session UI and approved-only report selectors.
5. Integrate physical snapshot and PM placeholder, regression test, and build APK.
6. Detailed smart report templates are a separate approval gate.
