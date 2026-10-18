# 02.3 Static features — HazRob Tank research prototype

Per-sensor summary of separate baseline and exposure resistance windows: median, mean, min, max, dynamic range, signed difference, relative change and log-fold change. All resistances in ohms; no identification or ppm inferred.

**Scientific constraints:** windows must be aligned to actual heater phase, sampling session, and gas-exposure protocol; exclude invalid ADC and unstable baseline using 01.6 and 01.7 gates. Cross-sensor features require synchronized measurements and sensor ID ordering. A relative change is not a gas concentration and can be confounded by ambient changes. Use Rs or Rs/R0 consistently; this prototype takes Rs only. Baseline and exposure windows must not overlap, and splitting train/test must occur by complete sessions.

14 unit-test cases authored, not yet rerun from clean GitHub checkout. Run `cd research/techniques/02.3 && python -m unittest -v`. No production changes; status PROTOTYPE.
