# 01.6 Signal quality — HazRob Tank research prototype

Provides window-level diagnostic flags for missing readings, ADC out-of-range values, ADC rail saturation, insufficient samples and high median absolute deviation (MAD) relative to the median. Returns full diagnostics and a boolean quality flag. It does **not** interpret a quiet signal as chemically accurate, or substitute for an alarm.

The default ADC range 0..4095 is illustrative only. Configure the real ADS1115 or ESP32 ADC range, gain, input mode and clipping behavior before using with Tank data. Sample rate, heater phase, time stamps, baseline drift, quantization, autocorrelation and transient response must be addressed in later validation. Defaults (10% missing, 2% rail, 5% relative MAD, 5 valid samples) are research placeholders, not manufacturer or regulatory limits.

Tests: 13 cases supplied in test_signal_quality_v1.py; run `cd research/techniques/01.6 && python -m unittest -v`. No physical validation or production integration. Status PROTOTYPE.
