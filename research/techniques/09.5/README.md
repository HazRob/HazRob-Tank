# 09.5 — HazRob Tank research

**PARTIAL_PROTOTYPE.** Causal timestamped smoothing of response and pose, rejecting stale or future pose. Not wind/sensor-lag compensated.

Minimal research-only algorithm with synthetic smoke tests authored but not executed from a clean checkout. Sensor fusion and robot motion require synchronized telemetry, validated physical safety interlocks, independent operator authorization and field testing. This prototype never authorizes movement or certifies safe conditions. No production ChemCore, APK or ESP32 firmware changes.

Run `cd research/techniques/09.5 && python -m unittest -v`.
