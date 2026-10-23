# HazRob Tank V4 — rolling update backlog

**Created:** 2026-10-24  
**Project:** HazRob Tank only (`HazRob/HazRob-Tank`), fully independent of the original HazRob  
**Status:** REQUIREMENTS COLLECTION — owner will submit additional fixes and changes sequentially  
**Baseline:** Preserve the last verified V3 app/controller/camera sources and fix only V4-approved requirements. Do not conflate completed CI checks with device approval.

## V4 requirements register

| ID | Priority | Component | Owner request | Acceptance criteria | Status |
| --- | --- | --- | --- | --- | --- |
| T4-001 | P1 | Release packaging — main ESP32 and ESP32-CAM | Deliver the firmware as **two separate, directly downloadable Arduino IDE project ZIP files**, one for `ESP32-Tank` and one for `Cam-tank` (ESP32-CAM), as normally supplied. | (1) `HazRob-Tank-V4-ESP32-Arduino.zip` contains a top-level `ESP32-Tank/` folder with `ESP32-Tank.ino` and all required local `.cpp`/`.h` files. (2) `HazRob-Tank-V4-ESP32-CAM-Arduino.zip` contains a top-level `Cam-tank/` folder with `Cam-tank.ino`, `RemoteVideo.cpp`, `RemoteVideo.h`, config and other required sources. (3) Unzip each package and open the corresponding `.ino` directly in Arduino IDE; select the board/core appropriate to **each distinct controller**. They are **Arduino sketch projects**, not Arduino ZIP libraries; do not try to use “Add .ZIP Library.” (4) Compile each sketch independently, inspect ZIP paths/source completeness and record SHA-256. (5) No original HazRob/upstream firmware, private device credentials, compiled binaries replacing sketch sources, or an outer ZIP containing both projects. (6) Keep Android `.apk` separate and directly downloadable, not compressed into the firmware ZIP. (7) Document versions and project branch; distinguish CI-built candidate from physical hardware approval. | **REGISTERED — pending V4 implementation / test / delivery** |

## Decisions and change control

- **2026-10-24:** Owner explicitly requested capturing the independent `ESP32-Tank` and `ESP32-CAM` Arduino ZIP delivery format for **V4**, and will provide additional repairs and enhancements in subsequent messages.
- This document is the canonical accumulating **V4 requirements list**, not an authorization to republish/overwrite V3, merge branches, flash motors, or claim a completed production release.
- Number new requirements sequentially `T4-002`, `T4-003`, and so forth; record each requirement's scope, acceptance criterion and status. Avoid duplicate IDs and silently changing approved requirements.
- Leave V3 source and hardware unchanged while collecting V4 amendments. Implementation, integration, CI and physical validation will be tracked separately when the owner directs V4 execution.

## Sources referenced

- Controller project: `firmware/ESP32-Tank/ESP32-Tank.ino`
- Camera project: `firmware/Cam-tank/Cam-tank.ino`
- V3 planning reference: `docs/requirements/V3-ANDROID-BACKLOG-2026-10-20.md`
