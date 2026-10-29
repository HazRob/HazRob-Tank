# HazRob Tank V6 — change register

**Created:** 2026-10-29
**Branch:** `feature/tank-v6-implementation`
**Baseline:** `c132fc2f8eb7a54d5b3c96b4fdcd6090096d1a5b`
**Status:** `OPEN_FOR_OWNER_REQUIREMENTS`

This register is reserved for **new owner-approved V6 changes**. No V6 functionality changes have been approved or implemented just by creating the branch.

| ID | Requirement / defect | Priority | Owner approval | Target component | Test criteria | State |
| --- | --- | --- | --- | --- | --- | --- |
| V6-001 | *Reserved for the first specific owner change* | TBD | PENDING | TBD | TBD | NOT_STARTED |

### Intake rules

- Replace the reserved row when the first concrete change is supplied; allocate `V6-002` onward in sequence.
- Identify whether each item is a fix, enhancement, or investigation, and record observed behavior separately from suspected root cause.
- Capture API/firmware, Android/SQLite, ChemCore/Stable, physical hardware, report, safety and signing dependencies.
- Acceptance tests and change impact must be agreed before an item is marked complete; static source checks do not substitute for device testing.
- If real connectivity is a V6 requirement, first reproduce and capture logs from Android and ESP32 `/`/`/diag`, Wi-Fi state and firmware revision; avoid disabling safety gates to 'fix' a connection.
