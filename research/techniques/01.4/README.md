# 01.4 Heater-cycle segmentation — HazRob Tank research prototype

**Purpose:** Tag measurements with heater HIGH/LOW phase and exclude configurable settling windows before R0 calibration or sensitivity analysis. This is a software segmentation tool; it **does not switch or regulate heater power**.

## Safety and scope
MQ7 and MQ9 heater voltage profiles are model/manufacturer-specific and require an actual controllable heater supply, verified timing and circuit characterization. The 60/90 s timing in tests is **synthetic** and NOT a datasheet recommendation. The function does not set voltages or claim valid sampling phases for either sensor. Verify manufacturer instructions and actual Tank heater wiring before any field experiment. Continuous-heater sensors should not be forced into this cycling profile.

## Limitations
Clock starts at a caller-defined phase origin and must be synchronized with actual heater transitions; missed cycles and reset recovery are not detected. No thermal model, warmup certification, gas identification or ppm calculation. Settling time is user-defined research parameter. Real samples need timestamp, heater voltage/current, sensor ID, ambient temperature/humidity and session provenance.

## Tests
12 software unit cases in test_heater_cycles_v1.py; run `cd research/techniques/01.4 && python -m unittest -v`. Research-only; not production qualified.
