# HazRob Tank V6-002 — تدقيق تطابق ChemCore مع المصنع وMQUnifiedsensor ومكتبات MQ والبصمات/PPM

**Date:** 2026-10-29 (Asia/Aden)  
**Status:** `RESEARCH_AND_SOURCE_AUDIT_COMPLETE_FOR_INSPECTED_REFERENCES / IMPLEMENTATION_NOT_AUTHORIZED / G3_NOT_VALIDATED`  
**Scope:** Tank V6 Android ChemCore, inherited Tank V5 ESP32 source and frozen baseline `sohpany/SERVOC-Android@6281d3eb7c1c961ce63b0b5e4ae0792874626720`. All modifications stay inside `HazRob/HazRob-Tank`. No ChemCore algorithm or firmware has been modified in V6-002.  
**Evidence classification:** `CODE_INSPECTED`, `PUBLISHED_MANUFACTURER`, `PUBLIC_LIBRARY_CODE`, `CENTRAL_REFERENCE_CATALOG`, `PROPOSED_EXPERIMENT`; these types must never be merged into a single performance score.

## Executive finding

The current engine is **not** a naive MQ->ppm-only calculator: it normalizes actual Rs/R0, fits a common gas concentration against available source-backed relations across the array, computes standardized log-residuals and relative candidate weights, applies breadth/mixture/specialist/open-set rules, retains research fingerprints, and distinguishes provisional PPM from device-validated M4. These are valuable existing components. Yet its ability to identify gases and quantify concentrations with physical accuracy is limited by reference-definition mismatch, as-built RL/heater/ADC uncertainty, generic "alcohol" labels and research-curve provenance, heuristic likelihood/ranking thresholds, absent Tank G3 per-gas validation, and inadequate mixture/unknown-gas controls.

## 1. Direct source inventory and authority

1. Manufacturer: Winsen MQ2, MQ4, MQ5, MQ8, MQ135 product specifications and MQ7B/MQ9B *variant-specific* heater conditions; Hanwei original-language MQ datasheets cataloged centrally. Datasheet sensitivity curves represent typical measurements in defined atmosphere; no universal physical R0 for all sensors of a type.
2. `miguel5612/MQSensorsLib` (`MQUnifiedsensor.h/.cpp`, `Calibration.ino`, MQ2/MQ3 examples). Code formula `Rs = RL*(Vc/Vrl - 1)`, `R0 = Rs_air/RatioCleanAir`; `setRegressionMethod(1)` implements `ppm = a * (Rs/R0)^b` and other supported regression mode implements `ppm=10^((log10(ratio)-b)/a)`. Its `externalADCUpdate(volt)` supports an ADS1115 measurement supplied after correct electrical conversion. Common illustrative clean-air ratios are **unitless**, not kΩ or PPM: MQ2=9.83, MQ3=60, MQ4=4.4, MQ5=6.5, MQ7=27.5, MQ8=70, MQ9=9.6, MQ135=3.6. Code example coefficients are not Tank-certified calibration coefficients. See https://github.com/miguel5612/MQSensorsLib/blob/master/src/MQUnifiedsensor.cpp and https://github.com/miguel5612/MQSensorsLib/blob/master/examples/Calibration/Calibration.ino .
3. `abcdaaaaaaaaa/MQDataScience`: code/data-science tools for MQ curve regression, temperature/humidity corrections, 3D and time-dependent 4D simulations; central catalog cites `MQSpaceData v7.0.0` at 2026-10-21 and V5 audit inspected `MQDataScience v6.0.0`; **pin exact version/commit/curve origins** before comparison. A model fit to extracted graph points or R² against simulated points is not independent concentration accuracy. https://github.com/abcdaaaaaaaaa/MQDataScience .
4. Canonical central HazRob reference bank: `HazRob/build-harness/library/reference-bank/INDEX.md` and `external-research/master_catalog.json`, **80 bibliographic entries**, mixed levels of provenance; not 80 Tank G3 experiments. `MQ_R0_RS_PEER_COMPARISON_2026-10-21_AR.md`, `MQDATASCIENCE_AND_EXPANDED_DATA_2026-10-21_AR.md`, `PUBCHEM_QSPR_INVERSE_MOLECULAR_2026-10-21_AR.md`, `ANALYTICAL_SYSTEMS_MIXTURE_UNMIXING_QUANT_FILTERS_2026-10-21_AR.md` have been read as catalogs/reviews. The old `HazRob-Lab` is explicitly frozen/historical, not an active production source.
5. Operational legacy source: `sohpany/SERVOC-Android@6281d3e` includes `research_data/hr069_analytical_library/mq_research_curves_43.json`, `GasEngine`, `LiteratureCurveRegistry`, `ResearchCurveRegistry`, `ChemCoreInferenceEngine`, `ChemCoreUncertaintyPolicy`, `AcceptedFingerprintIdentityEngine`, and open-set/mixture/breadth rules.
6. Actual Tank V5/V6 source bridge: `AcceptedReferenceResponseModel`, `TankStableScreeningModelRegistry`, `TankMultiSourceResponseModel`, `InverseMolecularSolver`, `TankStableRuntimeCatalog`, `TankInstantAnalyzer`, `TankFingerprintScreening`, `AcceptedR0Normalizer`, `TelemetryRecorder` and `firmware/ESP32-Tank/Hardware.cpp`. These are source paths, **not evidence of an installed V6 build**.
7. Literature used for analytical methodology: Mei et al. 2024 on cross-sensitivity and pattern recognition; Han et al. 2025 on MOS arrays and ML; electronic-nose data-processing review 2024; review of drift compensation 2026; UCI224/UCI270 temporal-drift benchmark (13,910 records, 16 Figaro/TGS-type sensors; not Tank MQ channels); UCI1081 single-MQ135 channel/other array under low concentrations. All are cross-platform methodology unless exact hardware, sampling and physical gas reference match.

## 2. What the present Tank actually computes

`Hardware.cpp:readMQ` averages ADS1115 codes per channel, computes `Vrl` as code*6.144/32768 and `Rs = RL*(5.0-Vrl)/Vrl`, rejects absent/power-off/invalid measurements, and divides by device-specific stored active R0 when available. Firmware assumes **all eight** channels use `RL=10 kΩ`, `MQ_VCC=5.0 V`, `ADS_FULL_SCALE_V=6.144 V`; ADS1115 supply and ADC input/common-ground must be verified physically. Current V5 staged calibration uses the eight library clean-air ratios in firmware order `mq2,mq4,mq5,mq8,mq3,mq7,mq9,mq135` and calculates `R0=mean(Rs_air)/ratioCleanAir` after 300s+60s, pending explicit accept to NVS. These constants are **not** a published universal physical R0, and calibrated air cleanliness is an assumption requiring context. V5 staged protocol differs from older three-repeat calibration discussions; reconcile method before a firmware change.

`AcceptedR0Normalizer` prioritizes accepted local device-specific R0, rejects nonphysical measurements, uses archived R0 for previously captured samples, and prevents recomputation of history with today's baseline. Important preserved functionality.

`TankStableScreeningModelRegistry.StableModel.predict` computes `log10(Rs/R0)=intercept+slope*log10(ppm)` with explicit finite/positive/domain tests and verifies active Stable SHA. `AcceptedReferenceResponseModel` attempts published power/linear references, manufacturer matrix/simulated relation, and research relations. `TankMultiSourceResponseModel` retains multiple candidate relations for a single sensor but chooses **one source relation per sensor per tested ppm** (correctly preventing duplicate physical observations). `InverseMolecularSolver` scans a grid of 181 log-concentrations, locally refines, minimizes reliability-weighted standardized residual mean, ranks candidates via softmax-like **relative model weight** and saves source/evidence/residuals. The legacy `ChemCoreInferenceEngine` additionally applies breadth weighting, reference/specialist contradictions, potential-mixture and OOD/open-set rules. `TankFingerprintScreening` independently exposes source-backed pattern matching as screening, not validated identification; `ChemCoreDecision` reserves reportable occupational concentration for M4/QUANTIFIED.

Historical generated Stable: 652 chemical **identities**, 49 `response_priors` (=43 MQDataScience-derived research curves + 6 other MQ135 references), 0 physical Tank calibration models, 0 M4. The 49/49 forward/inverse numerical consistency test recorded in V5 proves **algebraic consistency only**; it does not establish recognition accuracy, ppm error or absence of cross-sensitivity.

## 3. Manufacturer operating conditions versus current assumptions

| Sensor | Firmware clean-air divisor | Manufacturer/chemical issue | V6 research disposition |
|---|---:|---|---|
| MQ2 | 9.83 | Flammable/propane/smoke broad response; no chemical selectivity implied | Screen as combustible-family and verify source curve & RL |
| MQ3 | 60 | Alcohol-sensitive, cross-responds to benzene and other solvents; generic `Alcohol` is not necessarily pure ethanol with documented ppm unit | Separate ethanol-certified species evidence from generic alcohol curve |
| MQ4 | 4.4 | Manufacturer primary CH4 range about 300–10000 ppm under specified operating conditions | Respect exact sensor/module and domain |
| MQ5 | 6.5 | LPG, CH4, C3H8 interference; mixtures are not a single pure compound | Family/single-gas hypothesis only until mixtures validated |
| MQ7 | 27.5 | Check exact MQ7 vs MQ7B hardware; Winsen MQ7B CO protocol has heater 5.0V 60s and 1.5V 90s | With fixed heater, disable manufacturer-validated CO claims; retain research-only observations |
| MQ8 | 70 | Winsen H2 100–1000 ppm nominal range; 48h+ preheat, T/RH/O2 caveats | No exclusive H2 negative-evidence gate without sensitivity validation |
| MQ9 | 9.6 | Exact variant matters; Winsen MQ9B CO+CH4 5.0V/1.5V 60s/90s | Do not infer CO ppm from constant 5V heater by importing MQ9B relation |
| MQ135 | 3.6 | Ammonia/sulfide/aromatics and H2 cross-response; not a direct selective CO2 meter | Keep CO2 and alcohol hypotheses labeled indirect and unvalidated |

**Critical distinction:** The MQ7B/MQ9B time/voltage figures are conditional on the **B-model manufacturer sheet**. No visual or electrical proof has been collected that Tank's installed modules are this exact variant. Identify exact purchase labels/board circuits before any forced firmware heater redesign.

Winsen MQ135 page gives typical T≈20±2°C, RH≈55±5%, O2 importance and >48h initial preheat. A source curve from a reference lab cannot be treated as an as-installed transfer function when Tank temperature, humidity, RL, clean-air composition, sensor aging and heater protocol differ.

## 4. Prioritized source-based gaps

| ID / priority | Verified finding | Scientific consequence | V6 corrective design / gate |
|---|---|---|---|
| `G01 P0` | `Hardware.cpp` hardcodes `Cfg::RL=10kΩ` & Vc=5V for all modules; no as-built eight-channel resistance certificate | All Rs/R0 and ppm become systematically biased if RL/Vc differ | Measure circuit per module with DMM, ADS rail and divider; store per-sensor calibration contract and unit tests |
| `G02 P0` | `R0 = Rs_air / cleanAirRatio` ratios borrowed from library; multiple sources define R0 differently | Curve-specific mismatch can shift inferred gas concentration by orders of magnitude | Add immutable `r0_definition_id`, `reference_gas`, ratio reference, electronics, heater phase; algebraic transform only when justified; otherwise incompatible |
| `G03 P0` | MQ7/MQ9 heater is fixed relay 5V, but B-model references require thermal cycles | CO-specific manufacturer transfer model unqualified, and 2 sensors could bias array rankings | Physical part-number audit; mark affected channels as research/conditional until actual heater verification or change |
| `G04 P0` | `ResearchCurveRegistry` MQ3 generic alcohol uses `outputScale=50` and legacy `chemical=103` ethanol mapping; `LiteratureCurveRegistry` includes “alcohol / ethanol mapping” | Unit/species mismatch and false selective identity risk | Provenance and unit reconstruction for every curve; split `ALCOHOL_CLASS` from `ETHANOL_CAS_64-17-5`, prevent unqualified renaming |
| `G05 P0` | `TankMultiSourceResponseModel` changes selected reference by concentration based on tier/sigma, while `AcceptedReferenceResponseModel` returns first applicable source in fixed order | Piecewise discontinuities/selection bias; model uncertainty and inter-source conflict are not quantified | Retain separate per-source fit diagnostics, no hidden source switch; benchmark robust multi-source uncertainty without double counting sensor |
| `G06 P0` | `StableModel.predict` takes T/RH but currently uses only intercept/slope; generic curve path notes reference environment but no empirically validated compensation | Environmental shift could mimic a different chemical or concentration | Context-conditioned correction only if trained on independent data; report out-of-environment/uncertainty otherwise |
| `G07 P0` | One valid sensor can fit the one concentration parameter nearly perfectly; V5 minimumContributors=1 and breadth policy is a heuristic | Single-sensor low-residual hypothesis can outrank better-supported alternatives | Candidate-specific observability and identifiability metrics, leave-one-sensor-out, minimum breadth/coverage for identity and calibrated ranks |
| `G08 P0` | Source `sigma_log10` is explicitly a research proxy (0.20–0.32 typical), not a prediction interval; softmax ranks are not calibrated presence probabilities | Relative ranking and `confidence%` cannot be read as physical accuracy | Held-out calibration curves, empirical error/confusion, calibration error, uncertainty coverage and selective risk by device/gas |
| `G09 P0` | Multiple source curves and gas families overlap, and `MixtureResidualGuard` is heuristic | A mixture can resemble a single gas, and false identity absence can be inferred | Unknown/mixture abstention, external binary/multi-gas concentration labels, nonlinear mixture fitting only after training |
| `G10 P1` | Fingerprint match based on static array ratios, source-based thresholds; active Stable reports 0 approved open-set artifacts | Heuristic fingerprint screening ≠ independently validated class identity | Add baseline/peak/recovery/derivative/EMA features on labeled full time series; group-level held-out open-set threshold |
| `G11 P1` | Temporal field path uses 60s distinct-cycle median, useful but lacks independent flow/purge/recovery modeling | Heater/gas lag and chamber hysteresis can change fingerprint and generalization | Fix sampling protocol, annotate purge, latency, rise and recovery times, window age |
| `G12 P1` | 43+6 references have varying provenance, QSPR and UCI from different sensor families; sources not all raw-audited | Source leakage, wrong device/units, pseudoreplication | Model cards: file SHA, DOI/version, measured vs digitized vs simulated, RL/R0, sensor variant, exact gas, domains |
| `G13 P1` | No physically referenced Tank G3 validation in central checkpoint (0/80) | Cannot certify ppm or safety-grade identity | Independent gas standards, grouped holdout by run/day/device/concentration, device G3 gate before promotion |

Quantitative sensitivity illustration (**hypothetical, not measured Tank error**): with `ppm=a*(Rs/R0)^b`, doubling R0 while holding Rs fixed changes inferred PPM by factor `2^(-b)`. For library example MQ3 alcohol `b=-1.504`, factor ≈2.84; MQ135 alcohol example `b=-3.18`, factor ≈9.06. This shows why reference-definition reconciliation precedes ranking.

## 5. Mathematical correspondence and limitations

1. **Electrical layer**: `Rs=RL*(Vc/Vrl-1)`. Vrl must represent actual analog load-resistor voltage, not uncorrected pin voltage if scaling/offset exists; low/high saturation and ADS supply constraints are device-health failures.
2. **Calibration layer**: `R0=Rs_air/k_air` *only if* k_air is defined by the *same source/datasheet convention* used by fitted curve. An alternate model using R0 at reference-gas concentration cannot be used unchanged with a clean-air-derived R0.
3. **Curve layer**: `log10(ratio)=A+B log10(C)`. For `C=a*ratio^b`, equivalent `B=1/b` and `A=-log10(a)/b`; when C has `output_scale`, convert concentration *units* first. Pin actual valid range; do not fit outside it or silently reuse a surrogate gas.
4. **Single-component inference**: For gas g, find `C_g` minimizing standardized residuals across available sensors; do not count several curves from the same physical MQ as independent votes. Estimate ranking conditional on library coverage, not proof of presence.
5. **Fingerprint and identity**: compare the entire registered array signature including missing channels, covariance and confounding humidity. A low-residual match is not a validated identity. Require held-out open-set negative controls.
6. **Mixtures**: `Rs/R0` is a nonlinear mixture response; summing pure-gas ppm or individual resistances is generally invalid. Model binary mixtures with explicit gas standards and referenced concentrations, or output `POSSIBLE_MIXTURE_OR_INTERFERENCE`.
7. **Quantification**: use separate `estimated_ppm_research`, `calibrated_ppm`, and `unavailable`. OSHA/NIOSH limits are context/duration-specific and never convert a research fit into a certified exposure measurement.

## 6. External dataset and literature applicability

- **UCI224/UCI270:** 13,910 rows / 16 TGS-type sensors / six gases / ten batches over 36 months, rich steady and transient features; excellent for algorithmic drift, temporal split and open-set method benchmarking **but not a Tank MQ8 ppm calibration**. Never randomly split neighboring readings of the same exposure across train/test.
- **UCI1081:** one actual MQ135 channel among ten sensor channels and low concentrations (ppb), useful to test *that* channel's data processing and sensor ablation under independent sample grouping; not a full Tank array.
- **MQ-EXT-03** 6400 ADC records of NoGas/Perfume/Smoke/Mixture: operating classes only, not four pure molecules with reference ppm. **MQ-EXT-10** liquid-solution ethanol concentration series is not measured vapor ppm. **MQ-EXT-02** food spoilage Rs records not gas-reference ppm.
- **MOS mixture and QSPR studies:** can inform dimensionality reduction, nonlinear regression, drift and physics, but sensor-specific response functions must be learned or measured on installed MQ hardware.

Sources:
- https://www.winsen-sensor.com/manual/mq135.html
- https://www.winsen-sensor.com/sensors/h2-sensor/mq8.html
- https://www.winsen-sensor.com/sensors/combustible-sensor/mq4.html
- https://www.winsen-sensor.com/sensors/combustible-sensor/mq5.html
- https://www.winsen-sensor.com/sensors/co-sensor/mq-7b.html
- https://www.winsen-sensor.com/sensors/combustible-sensor/mq9b.html
- https://github.com/miguel5612/MQSensorsLib
- https://github.com/abcdaaaaaaaaa/MQDataScience
- https://pubs.rsc.org/en/content/articlehtml/2025/tc/d4tc05220j
- https://link.springer.com/article/10.1007/s40820-024-01489-z
- https://pmc.ncbi.nlm.nih.gov/articles/PMC11314697/
- https://pubmed.ncbi.nlm.nih.gov/39552302/
- https://archive.ics.uci.edu/dataset/270/gas+sensor+array+drift+dataset+at+different+concentrations
- https://archive.ics.uci.edu/dataset/1081/gas+sensor+array+low-concentration

## 7. Recommended V6 implementation strategy (DESIGN ONLY)

1. `V6-002/A` **Model-contract audit**: build exact variant/source/revision matrix, resolve eight RLs/Vc, R0 definitions, heater schedules, units and all source coefficients; produce `source_inventory.csv` and `r0_curve_compatibility.csv`. No firmware changes before actual model identification.
2. `V6-002/B` **Digital analytical sandbox**: run source-backed mathematical forward/inverse roundtrips, leave-one-sensor-out, competing-source selection, different R0 definitions, humidity sweeps and counterfactual gas cross-response, explicitly separate self-generated from physically labeled data.
3. `V6-002/C` **Inference & fingerprint architecture**: replace silent per-concentration source switching with traceable alternatives; distinct one-gas vs mixture vs unknown models; temporal signatures, missing-channel/quality-aware fusion and calibrated abstention metrics. Preserve V5 runtime endpoints, data migrations and archived R0 provenance.
4. `V6-002/D` **Independent benchmarks**: compare `V5-base` against alternative inference approaches on source-qualified UCI/MQ holdouts with grouping, label/units, licence and SHA. Metrics: top1/top3, macro-F1, FPR of unknown, selective risk/coverage, ppm MAE/MAPE only with known reference ppm and valid nonzero denominators, coverage of predicted intervals, robustness to drift/temperature/sensor dropout.
5. `V6-002/E` **Real Tank G3**: physically reference ethanol + at least two distinguishable targets and negatives/interferents (exact sample plan follows approved Tank G3 gate); document same reference-gas exposure for all sensors, clean-air and recovery, temperature/humidity, sample group and repeatability; don't claim safety until independent device acceptance.
6. `V6-002/F` **Release gates**: scientific model card + compiled JUnit/Gradle + Android integration + verified Stable checksum + actual 8-channel hardware tests + reports + regression. Accept a research screening engine even when no M4; do not present uncalibrated ppm as certified.

## 8. Explicit tests to implement

| Test | Meaning |
|---|---|
| `V6-C002-T01` | Electrical Rs matches real DMM/ADS/Vc/RL for all eight channels within independently declared tolerance |
| `V6-C002-T02` | R0 formula and model-reference definition compatible per source; incompatible pairs fail closed for quantitative inference |
| `V6-C002-T03` | V4↔V5 saved R0 histories replay identically and remain device bound |
| `V6-C002-T04` | MQ7/MQ9 heater cycle validity reported per exact variant; no false validated CO model on fixed heater |
| `V6-C002-T05` | Each original/source curve forward/inverse roundtrips inside own domain with source units explicit |
| `V6-C002-T06` | Generic alcohol, CO2 cross-response, classes of smoke/LPG cannot become confirmed pure-gas identity without separate evidence |
| `V6-C002-T07` | Competing source fits preserve each model provenance; one physical MQ contributes once; result insensitive to source array ordering unless documented |
| `V6-C002-T08` | One-channel perfect-fit does not silently outrank qualified 6–8-sensor evidence under comparable condition |
| `V6-C002-T09` | Out-of-domain, missing MQ, saturated ADC, stale sample, heater invalid, power-off, no R0 and unknown gas produce honest statuses |
| `V6-C002-T10` | Pure gas vs mixture with grouped holdouts; no fictitious separate ppm from a shared class label |
| `V6-C002-T11` | Source-derived numerical self-tests clearly separate from independent accuracy; confidence/ranking probabilities calibrated only with references |
| `V6-C002-T12` | Live Android instant/field/lab use same vetted Stable model and preserve raw sample, R0, SHA, gas UID and source into PDF/XLSX |

## 9. Open questions / release state

- Exact installed MQ7/MQ9 variant and heater circuits **not confirmed**.
- Exact eight module RL resistances and voltage rails **not physically measured in this study**.
- MQDataScience tag v6/v7 and exact imported source coefficients **not byte-by-byte rechecked from an immutable pinned tag in this audit**; source registry has an older recorded commit.
- Full 80 reference entries have been **indexed**, not all 80 raw archives individually downloaded/inspected in this conversation.
- No recorded Tank G3 physical gas validation, no new runtime model training, no live Android comparison, no V6 APK or firmware compile.
- Legacy research `ChemCoreInferenceEngine` and `AcceptedFingerprintIdentityEngine` are fetched from the pinned baseline and staged into Android at build time, so a V6 fix affecting them must be implemented as **Tank-local overlays**; never edit the original four-wheel repository.

`V6_002_STATE=SOURCE_AND_LITERATURE_GAP_AUDIT_DOCUMENTED / NO_ALGORITHM_PATCH / NO_FIRMWARE_CHANGE / NO_PPM_CERTIFICATION`.
