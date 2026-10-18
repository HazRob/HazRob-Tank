# Imported legacy research reference for HazRob Tank
Original: sohpany/HazRob-Lab/references/extracted_analytical_equations_batch01_2026-10-18.md
Imported 2026-10-18. Research evidence only; no production integration. Validation claims retain original limitations.

# HazRob ChemCore — Extracted analytical equations, batch 01
Date: 2026-10-18
Research status: extracted source-code coefficients; NOT independent calibration and NOT approved for production.

Model C_ppm = a * (Rs/R0)^b. Interpret gas-specific Rs/R0 only with matching calibration convention and operating conditions.

| Sensor | Label as written | a | b | Primary exact source | Status |
|---|---|---:|---:|---|---|
| MQ3 | Alcohol | 0.3934 | -1.504 | https://github.com/miguel5612/MQSensorsLib/blob/master/examples/MQ-3/MQ-3.ino | source-code model |
| MQ3 | Benzene | 4.8387 | -2.68 | https://github.com/miguel5612/MQSensorsLib/blob/master/examples/MQ-3/MQ-3.ino | source-code model |
| MQ3 | LPG | 44771 | -3.245 | same | source-code model |
| MQ3 | CO | 521853 | -3.821 | same | source-code model |
| MQ3 | Hexane | 7585.3 | -2.849 | same | source-code model |
| MQ4 | CH4 | 1012.7 | -2.786 | https://github.com/miguel5612/MQSensorsLib/blob/master/README.md | source-code model |
| MQ135 | CO | 605.18 | -3.937 | https://github.com/miguel5612/MQSensorsLib/blob/master/examples/MQ-135-ALL/MQ-135-ALL.ino | source-code model |
| MQ135 | Alcohol | 77.255 | -3.18 | same | source-code model |
| MQ135 | CO2 | 110.47 | -2.862 | same | source-code model; NOT selective CO2 quantification |
| MQ135 | Toluene | 44.947 | -3.445 | same | source-code model |
| MQ135 | NH4 (source's label) | 102.2 | -2.473 | same | ambiguous label; do not automatically map to NH3 |
| MQ135 | Acetone | 34.668 | -3.369 | same | source-code model |

## Dataset schema ready for extraction
UCI 1081 https://archive.ics.uci.edu/dataset/1081/gas+sensor+array+low-concentration : 90 samples x 9000 points; 10 sensors x 900 points; MQ135 = sensor index 4 (0-based), 3600:4500 in the concatenated 9000-point vector. Six gases: ethanol, acetone, toluene, ethyl acetate, isopropanol, n-hexane; 50,100,200 ppb, five samples each. 1 Hz; baseline 300 seconds and exposure 600 seconds. Thus MQ135 time-series alone is directly extractable. The 900 points exclude cleaning phase.
UCI license CC BY 4.0. No numerical traces extracted in this batch.

## Critical provenance rules
- Do not confuse MQSensorsLib datasheet regression with experimental validation.
- Coefficients may differ between code revisions and even files in the same repository; pin SHA before integration.
- Source labels such as NH4 may be scientifically ambiguous. Validate against original sensor manufacturer datasheet and test gas.
- Inverse ppm model does not identify a gas on its own; gas hypothesis must come from independent discrimination evidence.
