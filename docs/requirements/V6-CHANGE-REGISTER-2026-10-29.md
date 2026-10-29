# HazRob Tank V6 — change register

**Created:** 2026-10-29
**Branch:** `feature/tank-v6-implementation`
**Baseline:** `c132fc2f8eb7a54d5b3c96b4fdcd6090096d1a5b`
**Status:** `OPEN_FOR_OWNER_REQUIREMENTS`

This register is reserved for **new owner-approved V6 changes**. No V6 functionality changes have been approved or implemented just by creating the branch.

| ID | Requirement / defect | Priority | Owner approval | Target component | Test criteria | State |
| --- | --- | --- | --- | --- | --- | --- |
| V6-001 | Direct entry rejects running ESP32-Tank firmware 0.1.4-v4-dev because Android version 5.0 is compared with firmware banner; remove release-version matching, do not add app↔firmware authentication | P0 | APPROVED 2026-10-29 | ConnectionGate / direct Wi-Fi bootstrap / local telemetry | Existing `ESP32-Tank 0.1.4-v4-dev` and V5/V6 firmware may enter without version equality; no fake camera prerequisite; unknown device rejected; never bypass E-STOP and motor watchdog | SOURCE_FIXED / JVM_PENDING / DEVICE_PENDING |

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
