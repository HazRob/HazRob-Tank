# HazRob Tank — approved Android/firmware decisions (2026-10-17)

## Baseline and scope
- Android source pinned to `sohpany/SERVOC-Android@6281d3eb7c1c961ce63b0b5e4ae0792874626720` (HazRob 0.10.2 / HR069-R1 / RC2). Tank is a **separate app** in `HazRob/HazRob-Tank`; original repo must not be modified.
- Tank main firmware `firmware/ESP32-Tank`, ESP32-CAM firmware separate. Preserve Direct/Remote architecture and camera settings unless user requests changes.

## Decisions approved by user
1. **No IR sensor** in Tank: remove IR status, badges, controls, warnings, telemetry fabrication and references from all Tank Android tabs and diagnostic screens. Do not represent GPIO35 as IR.
2. **No obstacle-triggered automatic stop**: Echo is for distance readings and proximity warning only, never for stopping movement. Remove the obstacle auto-stop setting, status and commands from Tank app. No sensor-triggered drive veto in firmware.
3. **Operator safety remains**: preserve manual STOP, latched E-STOP, release-to-stop, 500ms stale-command deadman and stop on loss of operator control. These are not obstacle avoidance.
4. **Drive hardware**: two continuous-rotation servos, LEFT GPIO16, RIGHT GPIO17, instead of L298N and its former drive pins. Differential track commands remain signed -100..100. Servo direction/polarity, neutral and range require physical validation.
5. **Echo**: TRIG GPIO23, ECHO GPIO39; 10 cm proximity warning is informative only, no 5 cm stop threshold. Existing firmware's ultrasonic warning may drive alert indicators but not motion interlock.
6. **Other sensors**: preserve eight MQ with ADS1115 pair, BME environment, microphone, LDR and battery mapping; no unapproved remapping.
7. **Steering and pivot settings retained**: inner-track turn speed default 40%, pivot-in-place default 60%, joystick translation deadzone 8%. These settings control differential track mixing, remain configurable, and were **not changed** in this iteration. Direction-reversal settings retained. Confirm actual servo response under load.
8. **First five app tabs in scope**: Control, Measurements, Status/Diagnostics, Settings, Experiments. The Experiments source was copied without behavior change. ChemCore, Reports, and developer interfaces still require a full audit.

## Code changes staged (not released)
- `android/app/src/main/java/com/servoc/app/ui/{ControlTab,MeasurementsTab,StatusTab,SettingsTab,ExperimentsTab}.java`
- `android/app/src/main/java/com/servoc/app/hardware/HazRobHardwareProfile.java`
- `android/app/src/main/java/com/servoc/app/communication/{RobotProtocolV4,RobotApi}.java`
- Earlier decision record: `android/TANK-APP-CHANGE-001.md`; implementation record: `android/IMPLEMENTATION-STATUS.md`.

## Verification and outstanding work
- **Android source import is incomplete**. Only a subset of Java files has been copied. No Gradle build, tests, signed/unsigned Tank APK, device testing, or full cross-reference search has been completed. Do not label any of these complete.
- Audit remaining UI, resources, telemetry, remote transport and tests for IR, auto-stop and L298N; reconcile all references and compile errors. Confirm Direct HTTP /drive and Remote MQTT behavior against Tank firmware. Validate ChemCore, sessions, reports, settings and camera on hardware.
- Firmware's link-loss recovery/reverse logic requires a separate safety decision and hardware test; no unapproved changes to it were made.

## Next phase
Continue new user-requested modifications from this documented baseline; retain these decisions unless explicitly superseded.
