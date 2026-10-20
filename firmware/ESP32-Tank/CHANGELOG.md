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
