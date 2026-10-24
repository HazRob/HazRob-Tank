## 0.1.4-v4-dev — 2026-10-24

- Echo HC-SR04: reject pulse widths outside 110–23500 µs; nonblocking GPIO39 interrupt capture and duplicate-rise guard; three-valid-reading median for the published distance to suppress isolated jumps (e.g., 20 cm to 90 cm).
- Expose both immediate `raw_cm` and filtered `cm` alongside status, timeout, invalid-pulse and no-edge counters in proximity telemetry. Invalid/stuck-high/timeout does not masquerade as a measured distance; close *valid raw* echo still raises a warning without waiting for three median samples.
- **Warning-only** on HazRob Tank: this Echo logic does not brake or steer, replace the operator, or override the independent 500 ms motor-command watchdog/E-STOP; true distance and hazard response still require a physical bench test.
- Android V4 computes active Rs/R0 from the latest durably saved per-sensor phone calibration; firmware's older embedded `Cfg::mq[].r0` stays an independent diagnostic/legacy field and must not be treated as the phone's current accepted R0.
- **Validation state:** source edit and static contracts only. Arduino-ESP32 compile, safe 5 V→3.3 V Echo level, servo-interference test, actual 20/40/90 cm ruler measurement and close-object warning remain PENDING.

## 0.1.3-v3-dev — 2026-10-20
- SAFETY CHANGE: automatic reverse-path/history replay after lost operator connection is **disabled**; its 5-second waiting and reverse movement are removed from Tank V3 firmware.
- Connection loss or **500 ms without a fresh motor command** brings both continuous-rotation servos to their neutral pulse immediately, marks `stale_stop=true`, and requires a fresh operator command to resume.
- Explicit disarming also neutralizes both servos; E-STOP continues to latch independently.
- Source update is not proof of hardware safety: ESP32 compile, servo-neutral/polarity test with tracks raised, loss-of-link and physical watchdog tests required before deployment.

# ESP32-Tank changelog

## 0.1.2-dev — 2026-10-17
- Added 250-sample rolling record of the last 5 seconds of individual track commands (20 ms resolution).
- On stale drive command: brake, wait 5 seconds for control recovery, then play the history backward at 50% command speed and double sample duration (up to 10 seconds), once.
- Explicit stop/E-stop clears recovery; heartbeat cancels replay. No encoder feedback; not an exact return-to-origin system.
- IMPORTANT: currently triggered by drive-command watchdog; true Wi-Fi/MQTT link-state verification and reconnection logic remain to be integrated and tested. Not compiled or hardware-tested.


## 0.1.1-dev — 2026-10-17
- Replaced L298N with independent rear-track continuous-servo control on GPIO16/17, short nonblocking ramps, reversal dwell and link-loss deceleration.
- Removed IR sensor and collision/cliff movement interlocks; Echo is warning-only.
- Preserved direct GPIO13/14 BME I2C and 8-MQ acquisition.
- Source review only; build and physical polarity/neutral tests pending.


## 0.1.0 — 2026-10-17
- Imported original HR069 ESP32 source without modifying code contents.
- Renamed Arduino entry sketch `HazRob.ino` to `ESP32-Tank.ino` for matching folder convention.
- Build, hardware and integration verification: **not yet performed**.
