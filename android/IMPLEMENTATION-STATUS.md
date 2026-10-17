# HazRob Tank Android — implementation status (2026-10-17)

Source pin: `sohpany/SERVOC-Android@6281d3eb7c1c961ce63b0b5e4ae0792874626720`.

## Changes staged on develop

- `ControlTab.java`: remove IR badge and IR state rendering; retain Echo distance and manual/latched E-STOP.
- `MeasurementsTab.java`: remove IR table row and associated updates.
- `StatusTab.java`: remove IR diagnostics and legacy L298N label; show servo GPIO16/17.
- `SettingsTab.java`: remove obstacle auto-stop toggle and related remote/local requests; replace L298N wiring note with servo GPIO16/17.
- `ExperimentsTab.java`: pinned source copy staged, no functional changes.
- `HazRobHardwareProfile.java`: replace L298N pins with two servo pins; remove IR hardware declaration; retain MQ/ADS/BME/Echo mapping.
- `RobotProtocolV4.java`: remove fabricated IR object from normalized snapshot; omit obstacle auto-stop output.
- `RobotApi.java`: remove obsolete auto-stop endpoint methods; retain HTTP direct-drive and E-STOP.

## Not yet complete

Only the eight files listed above have been imported, not the complete Android project. **No Android build or tests have run; no APK exists.** Other Android source files/resources may still reference IR or obstacle auto-stop. Further full-source import, cross-file reference audit, compile/test, and firmware-device validation are mandatory. Do not mark this as a production-ready release.

## Firmware

Tank firmware already uses servo GPIO16/17 and Echo TRIG23/ECHO39 for warning/telemetry only, with no installed IR. Operator STOP, E-STOP, and the 500ms command deadman are preserved. The automatic reverse-on-link-loss mechanism remains a separate firmware risk to evaluate; do not conflate it with sensor obstacle auto-stop.
