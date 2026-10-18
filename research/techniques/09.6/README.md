# 09.6 — HazRob Tank research

**PARTIAL_PROTOTYPE.** Conservative advisory gate for obstacle, cliff, gas alarm, data validity, connectivity and localization. No motor command generated.

Minimal research-only algorithm with synthetic smoke tests authored but not executed from a clean checkout. Sensor fusion and robot motion require synchronized telemetry, validated physical safety interlocks, independent operator authorization and field testing. This prototype never authorizes movement or certifies safe conditions. No production ChemCore, APK or ESP32 firmware changes.

Run `cd research/techniques/09.6 && python -m unittest -v`.
