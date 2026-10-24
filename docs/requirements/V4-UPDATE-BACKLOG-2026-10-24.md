# HazRob Tank — V4 Update & Fix Backlog

**Created:** 2026-10-24  
**Status:** REQUIREMENTS COLLECTION — NO V4 IMPLEMENTATION AUTHORIZED YET  
**Target:** Independent HazRob Tank Android + ESP32-Tank + ESP32-CAM (Cam-tank). This list is for **V4** and must not silently change the V3 candidate or the legacy original HazRob project.

## Approved incoming requests

| ID | Area | Requirement | Acceptance checks | Status |
| --- | --- | --- | --- | --- |
| V4-001 | Firmware delivery / Arduino IDE | Deliver **two independent Arduino sketch ZIP packages**, one for **ESP32-Tank** and a second for **ESP32-CAM (Cam-tank)**, each with a self-contained, correctly named sketch directory, corresponding `.ino`, all essential local `.h`/`.cpp` files and safe example configurations. Each must be downloadable independently and extracted/opened in Arduino IDE as a sketch project without sourcing unrelated legacy HazRob project files. **Do not substitute compiled `.bin` files for Arduino source ZIP.** Android **APK must remain a separate directly downloadable `.apk` file, not inside a ZIP**. | (1) Both ZIP archives exist and are separately downloadable; (2) root folder/INO filenames match exactly: `ESP32-Tank/ESP32-Tank.ino` and `Cam-tank/Cam-tank.ino`; (3) all referenced companion source files included, no actual passwords/tokens/keys; (4) Arduino IDE can extract/open each sketch with documented matching board/core (ESP32 DevKit vs AI Thinker ESP32-CAM), and CI compiles both; (5) SHA-256 and version noted; (6) standalone APK can be downloaded separately; (7) clear distinction between build-verified and physical-hardware-tested. A sketch ZIP is NOT an Arduino library install package: open extracted `.ino` through File > Open (or Sketchbook), not Sketch > Include Library > Add .ZIP Library. | RECORDED / NOT IMPLEMENTED IN V4 |

| V4-002 | Android UI / live measurement tracking candidate selection | **Fix white-on-white text in the chemical candidate selection list used to choose a substance to track during live measurement.** The substance names and list-item labels must use **black text (`#FF000000`) on the current white or very-light list background**, both when first displaying the list and after filtering, scrolling, or selecting. Scope: `القياسات` → `البحث والتتبع` → `اختيار غاز من المكتبة` → `حدد المادة` (`MeasurementsTab.chooseTrackingChemical()` displays search matches using `AlertDialog.Builder.setItems`), and any equivalent light-background candidate-list presentation. Preserve current list contents, ranking, target IDs, selection/tracking logic and existing appropriate contrasting text on colored/dark candidate cards; do **not** force black text on dark red/blue backgrounds. | (1) Open search-and-tracking in a real Android light-themed Tank V4 build; (2) type an Arabic name or CAS; (3) every item in white/light results list is rendered clearly as black text, readable at normal brightness; (4) scrolling and selecting an item maintains legibility and starts the correct target; (5) empty-results/cancel behavior unchanged; (6) a UI/regression assertion guards explicit contrasting list-item color, without introducing text/background regressions in other dialogs or ranked cards. | RECORDED / NOT IMPLEMENTED IN V4 |

## Future additions
- Continue numbering subsequent user-requested changes **V4-002, V4-003, ...**, updating this document after each decision.
- Do not infer approval of requirements not supplied. Log request, affected module, exact expected behavior, test/acceptance criteria, and implementation status.
- Do not treat V3's prior diagnostic firmware ZIP/APK packaging as proof that V4 is finished.
- **No V4 code change or release is authorized by the current request:** collect owner-supplied modifications and fixes first. Maintain V3 branch and historical releases independently.

## Reference and source locations
- ESP32 controller sketch: `firmware/ESP32-Tank/ESP32-Tank.ino`
- Camera controller sketch: `firmware/Cam-tank/Cam-tank.ino`
- V3 requirements: `docs/requirements/V3-ANDROID-BACKLOG-2026-10-20.md`
- V3 implementation record (separate branch): `feature/tank-v3-implementation/docs/implementation/V3-EXECUTION-LOG.md`

## How to resume
Read this file on `develop`; locate the latest numbered V4 task; append new requirements with the next ID without overwriting existing V4 decisions. V4 remains in planning until the owner explicitly requests implementation.
