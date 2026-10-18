# 09.1 — HazRob Tank research

**PARTIAL_PROTOTYPE.** Detects threshold plume encounter intervals; no navigation control.

Minimal baseline and synthetic smoke tests authored; not verified run from a clean checkout. Spatial sensing requires synchronized robot pose, gas transport delay, airflow, sensor response/recovery, calibration and obstacle handling. All safety-critical motion remains outside this prototype. No real Tank validation or production ChemCore, Android, firmware modifications.

Run `cd research/techniques/09.1 && python -m unittest -v`.
