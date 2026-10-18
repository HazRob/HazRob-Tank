# 01.2 R0 calibration — research prototype (HazRob Tank only)

## Definition
For a specified MQ sensor, **documented calibration gas and concentration**, temperature/humidity, heater voltage/phase, and a verified datasheet ratio k = Rs/R0, derive R0 = Rs/k. R0 is a resistance (ohms; displayed as kOhms after dividing by 1000). Never treat arbitrary clean-air Rs as R0 or assume a common k for MQ2/MQ3/MQ4/MQ5/MQ7/MQ8/MQ9/MQ135.

## Implemented
Independent robust median of an Rs sample window, relative median absolute deviation gate, invalid-data and minimum-count rejection. The default MAD threshold and five-sample minimum are **research parameters**, not manufacturer specifications. Caller must supply a verified sensor-specific k. Calibration metadata (sensor model, manufacturer, source, gas, concentration, heater phase, temperature, humidity, session ID, hardware topology, timestamp) must be stored alongside each run by future integration; this prototype does not yet enforce that schema.

## Verification status
Software prototype only. No validated Tank raw calibration samples, exact module RL, ADS1115 gain/ADC conversion, or manufacturer k for each sensor were available to this implementation. No physical R0 calibration is claimed. MQ7 and MQ9 require heater-cycle phase-aware treatment before real-world use. Unit tests provided; run `cd research/techniques/01.2 && python -m unittest -v`. No changes to production firmware, APK or ChemCore.

## Acceptance gate
Obtain verified manufacturer curves and actual Tank module schematics, collect stable heater-phase-tagged calibration samples with environmental readings and known exposure, then compare independently measured R0 and research references. Report uncertainty, outliers, and session drift.
