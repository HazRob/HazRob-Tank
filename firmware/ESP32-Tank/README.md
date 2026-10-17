# ESP32-Tank

Firmware 0.1.1-dev for ESP32 DOIT DevKit V1. Dual continuous-rotation rear servos: GPIO16 left, GPIO17 right. BME280 direct I2C GPIO13/14; ADS1115 I2C GPIO21/22. IR removed; Echo warns only. No motor-driver L298N.

Continuous-servo direction and neutral require bench calibration. Build target: Arduino-ESP32 2.x (LEDC channel API). Not compiled or hardware tested. See CHANGELOG.md and docs/firmware/TANK-FW-001-CHECKPOINT.md.
