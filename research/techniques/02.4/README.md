# 02.4 Temporal features — HazRob Tank ChemCore research prototype
Features: observed peak, peak time, interpolated t10/t90, 10–90% rise time, maximum pre-peak slope, signed baseline-relative trapezoidal area, and time from peak to 50% recovery. Supports upward and downward responses. Missing recovery is represented by null/None, not zero. Times in seconds; response in caller's units. Peak-based rise time is not a manufacturer's steady-state t90 specification.

16 synthetic software unit tests passed locally 2026-10-18. Real Tank recordings, time synchronization, sensor-specific heater phase, sampling frequency, controlled exposure start, chamber transport delay and actual recovery must be checked before validation. Do not compare sensors without synchronized sampling or use overlapping train/test session windows. No gas identification or concentration claim; no production integration.

Run: `cd research/techniques/02.4 && python -m unittest -v`. Status PROTOTYPE.
