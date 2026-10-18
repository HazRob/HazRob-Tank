# 01.1 ADC to Rs — research prototype
Date: 2026-10-18. HazRob Tank ONLY.

Model: MQ sensing resistor Rs above load resistor RL to ground; V_RL = Vc * RL/(Rs+RL), hence Rs = RL*(Vc/V_RL - 1). Units: V, ohms. ADC conversion is an ideal linear approximation with explicit divider gain and calibrated full-scale. Reject rail, nonfinite and invalid readings.

Run: cd research/techniques/01.1 && python -m unittest -v

Result: 11/11 unit tests passed locally on 2026-10-18. **PROTOTYPE, not validated against actual Tank ADC, ADS1115 PGA, hardware resistor RL, module topology or voltage-divider calibration.** Do not use for ppm or safety conclusions. Verify heater cycles and hardware schematics before field use. No production changes.
