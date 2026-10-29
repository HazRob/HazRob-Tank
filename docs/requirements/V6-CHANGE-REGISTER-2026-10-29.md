# HazRob Tank V6 — change register

**Created:** 2026-10-29
**Branch:** `feature/tank-v6-implementation`
**Baseline:** `c132fc2f8eb7a54d5b3c96b4fdcd6090096d1a5b`
**Status:** `OPEN_FOR_OWNER_REQUIREMENTS`

This register is reserved for **new owner-approved V6 changes**. No V6 functionality changes have been approved or implemented just by creating the branch.

| ID | Requirement / defect | Priority | Owner approval | Target component | Test criteria | State |
| --- | --- | --- | --- | --- | --- | --- |
| V6-001 | Direct entry rejects running ESP32-Tank firmware 0.1.4-v4-dev because Android version 5.0 is compared with firmware banner; remove release-version matching, do not add app↔firmware authentication | P0 | APPROVED 2026-10-29 | ConnectionGate / direct Wi-Fi bootstrap / local telemetry | Existing `ESP32-Tank 0.1.4-v4-dev` and V5/V6 firmware may enter without version equality; no fake camera prerequisite; unknown device rejected; never bypass E-STOP and motor watchdog | SOURCE_FIXED / JVM_PENDING / DEVICE_PENDING |
| V6-002 | Investigate manufacturer/MQUnifiedsensor R0 calibration, published source curves, fingerprint/logarithmic gas inference and ppm versus the inherited ChemCore, prioritize scientifically traceable improvements | P0 research | APPROVED_RESEARCH 2026-10-29 / ALGORITHM_CHANGE_NOT_YET_DEFINED | ChemCore / Stable / 8 MQ ESP32 electrical & heater contracts / central research bank | Source/curve-to-R0 provenance matrix, quantified algorithmic gap report, staged acceptance cases, no research-as-M4 promotion | RESEARCH_AUDIT_DOCUMENTED / IMPLEMENTATION_NOT_STARTED / HARDWARE_PENDING |

### Intake rules

- Replace the reserved row when the first concrete change is supplied; allocate `V6-002` onward in sequence.
- Identify whether each item is a fix, enhancement, or investigation, and record observed behavior separately from suspected root cause.
- Capture API/firmware, Android/SQLite, ChemCore/Stable, physical hardware, report, safety and signing dependencies.
- Acceptance tests and change impact must be agreed before an item is marked complete; static source checks do not substitute for device testing.
- If real connectivity is a V6 requirement, first reproduce and capture logs from Android and ESP32 `/`/`/diag`, Wi-Fi state and firmware revision; avoid disabling safety gates to 'fix' a connection.

## V6-001 — field observation, root cause and limits

**Reported screen:** Android reached `192.168.4.1` and received `ESP32-Tank 0.1.4-v4-dev`, but a red recovery page blocked Direct mode. Earlier login used inherited `LocalNetworkContract.isExpectedFirmwareBanner(banner,ReleaseAudit.VERSION)` against Android V5 `ReleaseAudit.VERSION="5.0"`. This function requires the unrelated prefix `HazRob 5.0`, and thus rejects the actual Tank firmware even though the host replied.

**Accepted resolution:** Read the `/` banner and treat an `ESP32-Tank <nonempty firmware tag>` response only as evidence of reachable main Tank controller, not cryptographic authentication, Android version matching, gas calibration, or authorization. Do not make connection success depend on `/remote/status`: on unreadable status do not attempt switching profiles, while an explicitly reported `remote` may be switched to Direct. ESP32-CAM remains optional, and the operator can enter the app without camera preflight. Preserve host/subnet routing, firmware E-STOP, watchdog, application background neutral behavior and independent operation-specific capability checks.

**Code:** `android/app/src/main/java/com/servoc/app/auth/ConnectionGate.java`. **Regression test source:** `android/app/src/test/java/com/servoc/app/auth/TankV6DirectVersionIndependentEntryTest.java`.

**Acceptance not yet met:** perform Android physical-device test against actual ESP32 `0.1.4-v4-dev`, confirm read-only telemetry, confirm stop/E-STOP before drive qualification, and record `/`, `/diag`, `/remote/status` behavior and Logcat. This bug is **app-side**; no firmware change is needed solely to drop the release-version check. Any later firmware API or physical hardware mismatch is a separate compatibility question.

**Explicit exclusions:** Do NOT remove remote service developer authorization, command watchdog/E-STOP, device-bound R0 provenance or protections on write operations. Version-independent Direct entry must not be misrepresented as firmware capability, authenticated identity or safety-grade operation.

## V6-002 — scientific comparison checkpoint (2026-10-29)

The requested research audit is stored at:
`docs/research/V6-002-CHEMCORE-MANUFACTURER-R0-PPM-FINGERPRINT-GAP-AUDIT-2026-10-29.md`.

Reviewed **live** Tank V6/V5 and pinned SERVOC analytical source, Tank ESP32 `Hardware.cpp`, manufacturers Winsen/Hanwei references, library `MQSensorsLib/MQUnifiedsensor`, `MQDataScience`, and organization's central `reference-bank` index and audits. Catalog contains 80 *bibliographic references*, not 80 Tank G3 measurements. Existing ChemCore implements joint inverse fit, source selection, provisional ppm, independent fingerprint and open-set/mix/specialist decisions; source-defined uncertainties are research proxies, not calibrated prediction intervals.

**P0 investigation themes:** module-specific RL/Vc and ADS calibration; per-source R0 conventions; MQ7/MQ9 heater-profile identity; ethanol vs generic alcohol; stable model provenance and source switching; one-sensor perfect-fit bias; humidity drift and nonlinear mixture/unknown gas; actual device G3. See `G01..G13` in the report and `V6-C002-T01..T12` test proposals.

**Acceptance states:** `SOURCE_REVIEW=COMPLETE_FOR_INSPECTED_SET`; `ALL_RAW_SOURCES_VERIFIED=NO`; `NEW_ALGORITHM=NOT_IMPLEMENTED`; `G3_TANK=NOT_VALIDATED`; `V6_APK=NOT_BUILT`. No file in firmware, ChemCore engine or Stable release was modified for this research request. Owner's next concrete change instructions may select which gaps are implemented.
