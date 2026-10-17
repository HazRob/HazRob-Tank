# ESP32-Tank changelog

## 0.1.1-dev — 2026-10-17
- Replaced L298N with independent rear-track continuous-servo control on GPIO16/17, short nonblocking ramps, reversal dwell and link-loss deceleration.
- Removed IR sensor and collision/cliff movement interlocks; Echo is warning-only.
- Preserved direct GPIO13/14 BME I2C and 8-MQ acquisition.
- Source review only; build and physical polarity/neutral tests pending.


## 0.1.0 — 2026-10-17
- Imported original HR069 ESP32 source without modifying code contents.
- Renamed Arduino entry sketch `HazRob.ino` to `ESP32-Tank.ino` for matching folder convention.
- Build, hardware and integration verification: **not yet performed**.
