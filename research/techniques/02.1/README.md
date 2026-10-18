# 02.1 Signal filtering — HazRob Tank research prototype

Implements trailing median, trailing moving average and causal exponential moving average. Each preserves signal length and uses no future samples. A local execution of the equivalent source/test suite passed 15/15 tests on 2026-10-18. Repository clean-checkout test rerun is pending.

Causal filtering is appropriate for live streaming, but median/averaging can suppress short peaks and introduce detection delay. Test response rise time, overshoot, transient peak retention, phase separation (MQ7/MQ9), sampling interval and false alarms using real Tank records. Do not filter away hazard alarms or apply filters across heater phase transitions without validation.

Missing/nonfinite data are rejected by this research version; quality checks and gap-handling policy must precede integration. Window sizes and EMA alpha are research examples, not optimized parameters. No real-data chemical validation and no production integration.

Run: `cd research/techniques/02.1 && python -m unittest -v`.
Status: PROTOTYPE.
