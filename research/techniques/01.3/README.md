# 01.3 MQ sensitivity curves — research prototype

Scope: independent HazRob Tank ChemCore only. No firmware, Android or production code changes.

## Scientific model
Digitized manufacturer **gas-specific** sensitivity curve points (concentration ppm, Rs/R0) are input, in strictly ascending concentration order. The model interpolates linearly in log(concentration)-log(Rs/R0) between adjacent points. It can invert strictly monotonic curves, but does not identify gas, resolve mixtures, or establish measurement uncertainty. By default extrapolation beyond the source curve is prohibited.

No actual MQ2/MQ3/MQ4/MQ5/MQ7/MQ8/MQ9/MQ135 datasheet curves are embedded or claimed validated. Values in unit tests are synthetic and must never be interpreted as manufacturer calibration. Any curve must carry provenance: manufacturer, datasheet revision, target gas, reference conditions, R0 definition, heater phase, digitization method and error.

## Tests
Run: `cd research/techniques/01.3 && python -m unittest -v`. 13 tests are supplied; clean-checkout execution must be verified. Inversion requires monotonic curve; real datasheet nonmonotonic regions must be segmented and treated separately.

## Acceptance gate
Verify real module RL and Rs/R0, digitize sensor-specific curves from traceable manuals, test curve residuals on withheld experimental concentrations, account for humidity/temperature and MQ7/MQ9 heater phase, and quantify uncertainty. Status: PROTOTYPE, not chemical validation or safety-grade concentration measurement.
