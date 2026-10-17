# HazRob Tank — SYS-SPEC-001

Version: 0.1.0-Draft | Date: 2026-10-17

## Purpose
Independent tracked mobile platform for exploratory hazardous-gas sensing, environmental measurements, remote inspection and research analysis.

## Preliminary architecture
- Tracked drive: six wheels (three per side); new 65 mm sprocket and 20 mm GT2 belt design under development.
- Two continuous-rotation drive servos: model and torque to be verified.
- Gas array: eight MQ sensors; two ADS1115 ADCs; BME280; obstacle and edge sensing.
- Visible camera, night vision and thermal imaging are requirements under feasibility review, not confirmed integrations.
- Candidate battery pack: eight matched 18650 cells in 1S8P, pending protection, charging and load validation.
- Independent Android, firmware, ChemCore and communications modules.

## Safety and validation
MQ sensors and the prototype are not certified for occupational exposure decisions or explosive atmospheres. Electrical, thermal, functional and analytical validation are required before deployment.

## Open decisions
Controller, camera interfaces, final dimensions, actual motor specifications, power electronics, remote communications and analytical calibration acceptance criteria.
