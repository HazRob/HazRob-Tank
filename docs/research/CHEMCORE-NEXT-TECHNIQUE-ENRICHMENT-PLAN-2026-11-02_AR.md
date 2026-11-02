# ChemCore Next — خطة البحث العلمي وإثراء التقنيات (V8)
**الإصدار:** 1.0-proposed | **التاريخ:** 2026-11-02 | **الحالة:** PROPOSED_FOR_OWNER_APPROVAL / NOT_STARTED_NEW_TECHNIQUE_TESTS.
**المرجعية الحاكمة:** docs/architecture/CHEMCORE-NEXT-HEDAF-SCIENTIFIC-METHODOLOGY-2026-11-02_AR.md.
**سجل الأفكار S01–S20:** docs/research/CHEMCORE-NEXT-SCIENTIFIC-DISCUSSION-REGISTER-2026-11-02_AR.md.

## 1. الجرد الأساسي والفصل بين الأعداد
- دفتر V6 الأصلي: docs/implementation/V6-CHEMCORE-80-TECHNIQUE-NEW-ENGINE-COVERAGE-2026-10-29.json: **80** تقنية، 78 مع شيفرة جزئية واثنتان لم تنقلا (01.4 heater cycles و09.6 motion decisions)؛ **0/80 مؤهلة G3 على Tank**.
- أصول V6 المرجعية: سجل سابق 49 منحنى و24 fingerprint prototype؛ لا يعني العدد 49 أو 24 عينات تعرّف غاز فعلية.
- MQDataScience 7: 89 نموذج علاقة gas×sensor عبر 21 sensor types، 43 علاقة لحساسات الدبابة الثمانية؛ 76 طريقة regression/forecast وليست 76 مصنف غازات مستقلة؛ من سجل V6 التاريخي 14 port مسجلة (2 approximation) و62 غير منقولة، لكن port≠runtime-activated≠validated.
- الاستيعاب من المصادر المتعددة قد يصنع duplicates من نفس manufacturer curve؛ source independence mandatory.
- **المنهج:** Keep 80 original IDs immutable. Add NEXT-EXT-01... as separate research proposals; not deceptively report “80+X qualified”. Build crosswalk original↔external↔new with status gates.

## 2. سجل الأبحاث والمشاريع — STREAM-LIT
### LIT-01 بروتوكول البحث القابل لإعادة التنفيذ
تعريف السؤال (نوع الغاز/الحساس/المدى/التعرف/ppm/خليط/تداخل) ونطاق البحث، قواعد البيانات (Crossref, PubMed where relevant, IEEE Xplore, Scopus if licensed, Google Scholar discovery, institutional repositories, manufacturer, GitHub/UCI/Zenodo), query log exact query/date/site/language, dedup and inclusion/exclusion, PRISMA-compatible flow. لا يدّعى استعراض كل أوراق MQ على الإنترنت حرفيًا.
بحث مبدئي مثال: (MQ-2 OR MQ-3 OR MQ-4 OR MQ-5 OR MQ-7 OR MQ-8 OR MQ-9 OR MQ-135 OR metal oxide gas sensor array) AND (gas classification OR regression OR calibration OR mixture OR drift OR humidity OR cross sensitivity). إضافة بحث عن sensors to be installed مستقبلًا.
### LIT-02 نموذج الاستخراج
Ref id, DOI/URL, publication date, authors, license, repository snapshot/hash, manufacturer vs peer study vs simulated, actual hardware/model/batch, gas species and source reference, ppm/uncertainty, heater/voltage/RL/ADC, chamber/flow/humidity/temp/warmup/sampling/ground truth, replicate/day/device, train/validation/test design, statistics and reported failures, code/data availability, bias/limits. **source-quality tier** SOURCE_ONLY / DATA_RAW / INDEPENDENT_REPEAT / TANK_G3.
### LIT-03 سياسة الاستيعاب
1) Primary original datasheets; 2) papers with raw concentration-known physical data; 3) long-term drift, mixture and interference studies; 4) validated models source code; 5) simulations/digitized manufacturer graphs as theoretical features, never independently counted as experiments.
**Outputs:** research/chemcore_next/literature/search_log.csv, bibliography_registry.csv, paper_method_extraction.csv, reuse_license_review.csv, provenance_source_graph.json, literature_gaps.md.

## 3. سجل الأصول التحليلية — STREAM-ASSET
لكل أصل: asset_id, chemical_uid, sensor_model, source_root, origin blob, role [qualitative, quantitative, dynamic], equation canonical and original, ppm scale/range, Rs/R0 orientation, R_air/K_source reference, T/RH/time, heating phase, units, extraction mode [measured/manufacturer/digitized/computed/simulated], licensing, uncertainty, tests, allowed uses. Distinguish usable **research input** from runtime-approved evidence and safety-level claims.
Check manufacturer digitized regressions and alternative coefficients via MQSensorsLib_Docs workbook, MQDataScience source 89 and 76, V6 49 curves/24 prototype fingerprints, stable assets. All source errors logged even when method is attractive.
**Outputs:** research/chemcore_next/evidence_assets.csv, source_lineage.json, equation_registry.json, unresolved_source_units.csv.

## 4. العشر مجموعات الأساسية لخطة إثراء 80 تقنية
| Workstream | Original IDs | Scientific development focus | Mandatory gates |
| --- | --- | --- | --- |
| T01 — الفيزياء والمعايرة | 01.1..01.7 | ADC→Rs and RL/Vc, source-correct R_air/R0, MQ7/MQ9 heater (01.4 unported), T/RH applicability, dynamic drift, failure classes | Source equation G1; device electrical/phase G3 |
| T02 — استخراج الخصائص | 02.1..02.8 | robust filtering, scaling independent of source calibration, steady/transient features, theoretical vs measured fingerprints, 3D/4D, feature selection and provenance | G1/time no leakage; G2 ablation |
| T03 — التمييز والهوية | 03.1..03.8 | PCA/linear models vs controlled heldout, sensor subsets, identifiability and concentration confounding, classes overlap | G2 grouped identity metrics, unknown class |
| T04 — نماذج التحديد | 04.1..04.10 | curve and fingerprint match; baseline KNN, logistic, true-vs-prototype SVM/RF/XGBoost/MLP and physically grounded ML, fair benchmark | G1 correct named method; G2 source matched |
| T05 — تقدير الكمية | 05.1..05.9 | multipoint calibration, per-gas source inverse/PLS/regression, LOD/LOQ per device, concentration extrapolation flagged and quantified uncertainty | G2 known-concentration; G3 reference analyzer |
| T06 — الاستدلال المركب | 06.1..06.9 | sensor speciality, evidence lineage, Bayesian *only when calibrated*, conformal only if exchangeability supported, contradiction, open-set, abstention, explanations | no false probability, OOD/G2 calibration |
| T07 — التعميم والتعلم | 07.1..07.10 | split by exposure/day/device/batch, drift compensation, domain adaptation, active DoE, retraining audit and resource | blind grouped holdout, versioned stable |
| T08 — الزمن والمخاليط | 08.1..08.12 | 1D-CNN/TCN/LSTM only data-justified, source 3D/4D time forecasts, physical synthetic vs real, independent mixture/nonlinear unmixing and interference | no synthetic truth as physical G2, G3 mixtures |
| T09 — القياس الروبوتي | 09.1..09.6 | robotic olfaction, geolocated sessions, time-space mapping, plume-source hypotheses, active sampling, motion decision (09.6 unported) | isolate safety, actual robot bench |
| T10 — الأدلة السلبية | 10.1 | conditional sensor guided exclusion, sensor-specific no-response with LOD/heater/expected concentration; separation NOT_EVALUABLE vs ABSENT | edge cases negative evidence, cross sensitivity, G2/G3 |

**Implementation truth rules:** Partial status means a portion of method/code exists, not that a full RF or PLS-DA or XGBoost algorithm is implemented. Each ID gets source method description, code line, input/output schema, active consumer, numerical oracle, test case, G1/G2/G3 status, release decision.

## 5. إضافات البحث العلمي NEXT-EXT (ليست جزءًا من 80 الأصلية)
| ID | Area | Mechanism | Prerequisite |
| --- | --- | --- | --- |
| NEXT-EXT-01 | Sensor Selectivity Matrix | sensor×chemical×range×conditions validated positive/negative evidence | actual manufacturer/bench cross-sensitivity |
| NEXT-EXT-02 | Sensor independence & topology | sensor physical instance and source DAG, shared observables not double-counted | canonical model and source ids |
| NEXT-EXT-03 | Forward/Inverse falsifiability | reconstruct sensor vector/time using proposed gas/candidate mixture; abstain if not identifiable | experimentally suitable response F |
| NEXT-EXT-04 | Dynamic independent experts | source curve expert, fingerprint expert, T/RH surface expert, temporal expert, ML expert with suitability flags | G1+G2 per expert |
| NEXT-EXT-05 | Calibrated negative evidence | nonresponse conditional on valid LOD/time/phase/expected response | G2 and G3 with negative controls |
| NEXT-EXT-06 | Confidence and abstention | calibrated probabilities *only when supported*; reason coded UNKNOWN | heldout Brier/ECE and rejection analysis |
| NEXT-EXT-07 | Prior/context provenance | site materials, expert background, conflict tracking without overwriting physical evidence | user review schema |
| NEXT-EXT-08 | Central feedback triage | quarantine, provenance, consent, independent replication, ML training and promotion | audit controls and lab capacity |
| NEXT-EXT-09 | Community contributions | validation and quarantine, authenticity/repeatability, license/privacy | moderation and stable gate |
| NEXT-EXT-10 | Unified eligibility and rank | all ~734 material UIDs with actual evidence only; no top-10 filler | chemical uid completeness and G1/G2 |
| NEXT-EXT-11 | Bidirectional lab ↔ inference | stable data predicts samples; measured samples challenge model, error diagnostics | forward model qualified and feedback refs |
| NEXT-EXT-12 | Incremental sensor extension | dynamic sensor capability registration for future new MOX/O2/PM/electrochemical | architecture and hardware characterization |
| NEXT-EXT-13 | Clean air/session parity | R_air state consistent across instant/field/tracking, no hidden independent gates | actual replay tests |
| NEXT-EXT-14 | Separate safety evidence | independent qualified safety threshold/time accumulation and alarms | IEC/NIOSH use-case tests |

## 6. تجارب التطوير والاختبارات المشتركة
- **RESEARCH-CASE-01**: blank clean ambient, no nuisance candidates and no overclaim “safe”; baseline noise and T/RH variability.
- **02**: only one specialized response valid (current V6 gate demands two), display single-source evidence with explicit research tier.
- **03**: each sensor invalid/saturated/missing R0/heater wrong phase; no fabricated negative evidence.
- **04**: gas X with model, Y absent model, Z different correlation; no deducting Y absence as negative observation.
- **05**: pure known gas at 3+ reference concentrations and recorded environments, compare forward/inverse and ppm bias; hold out exposures/days.
- **06**: two- and three-gas mixtures with physically referenced composition; do not assume linear additivity.
- **07**: environment drift/long-term sensor aging, transfer across batches, compare models with/without correction.
- **08**: simulated curves vs digitized datasheet vs actual lab fingerprints reported in separate quality tiers; test source leakage.
- **09**: contradiction from user context (ethanol inventory vs methanol candidate), retain original ranking and flag dispute.
- **10**: nonlegacy UIDs with qualified evidence, cases 0/1/5/10/>10; no quota fill.
- **11**: reproduce same raw data through instantaneous and session analyzers; outcome differences only from documented temporal info.
- **12**: operational safety negative cases, no silent safety alarm suppression by user feedback.
**Outputs:** protocol preregistration, raw references, origin manifest, loss/error logs, G1-G2-G3 report plus responsible reviewer.

## 7. دفعات العمل وقواعد الأولوية
Batch 0: source inventory + reproducibility + full original-80 wiring/candidate mapping.
Batch 1: T01/T10 + NEXT-EXT-01/02/05/13 (quality, baseline, specialty, negative evidence).
Batch 2: T02/T03/T04 + NEXT-EXT-04/10 (curves/fingerprints/eligible candidate ranking).
Batch 3: T05/T06 + NEXT-EXT-03/06/11/14 (forward/inverse, uncertainty, UNKNOWN, PPM gate).
Batch 4: T07/T08 + NEXT-EXT-07/08/09 (central lab, drift, ML, community, mixtures).
Batch 5: T09 + NEXT-EXT-12 (robotic mapping and dynamic sensors), after hardware capabilities.
**Each batch**: evidence source→design hypothesis→minimal prototype→G1→G2 blind holdout→G3 if physical claim→accept/revise/reject→stable update. Parallel scientific research may continue, but unqualified code remains behind feature flag. No user data automatically trained.

## 8. Decision and measurable work criteria
Priority computed with publicly documented rubric: evidence quality (0–5), scientific effect/added independent information (0–5), safety-risk reduction (0–5), Android feasibility (0–5), cost/complexity (0–5); weighted plan is a management tool, not the probability a chemical exists. Require preregistered expected metric gain, correct experimental split and rollback.
G1: 100% mandatory numerical tests and source identity provenance; G2: task-specific predefined confidence intervals and performance vs simple/V6 baseline without sacrificing safety false negatives; G3: traceable actual Tank tests, minimum sample plan per analyte, no inferred qualification. Missing data = NOT_TESTED, not failed or passed. Source PDF alone never qualifies real ppm.
**Technique ledger columns:** id, section, owner_requirement, V6_status, source, duplicate_root, class/method, data_input, data_output, files, consumer, tests_g1, holdout_g2, bench_g3, gain, failure, proposed_change, decision, commit, actions_run.
**Candidate benchmarks:** offline evaluation with correct gas availability and combinations, unknown/mixtures, per-method attribution, FPR clean air, reportable ppm error only if matched known gas.

## 9. مخرجات التعهد النهائي
1. Literature and sources registry with factual extraction.
2. Original 80 × MQDataScience 76 crosswalk, equations, unit calibration differences, 14 legacy ports verification.
3. Independent 80-technique activation and qualification ledger.
4. Additional NEXT-EXT technique specification and ablation tests.
5. Immutable dataset partitions and scientific G1/G2/G3 evidence.
6. Stable promotion queue (NOT_PROMOTED until complete) and rollback manifests.
7. Summary of accepted/rejected ideas and why, retaining owner proposals 1..20.
**Status upon writing:** plans only; research existing partial; no new tests run.


## 10. ملحق تدقيق وتنفيذ مستودعي المصدر — 2026-11-02
**قرار تطبيقي داخل خطة الإثراء (لا يعني تأهيل التشغيل):** تم تحديد **23 قدرة تقنية ذات فائدة**، موزعة 11 من MQDataScience و12 من MQSensorsLib، لكل منها مصدر، بصمة SHA/URL، تقنيات V6 مرتبطة، مهمة Next مستهدفة، وأولوية نقل. ليست 23 مصنفًا مستقلًا جديدًا؛ بعضها إعادة صياغة أو تدقيق أو تصحيح لمكونات كانت جزئية في V6.
**مصفوفة النقل التفصيلية:** research/chemcore_next/mqdata_mqsensorslib_transfer_matrix_23_2026-11-02.csv.
**P0 (أولوية للمرحلة الأساسية):** معاملات Rs الفيزيائية (SRC-MQSL-01)، الفصل بين R_air/R0_source وK_clean (SRC-MQSL-02)، فحص ADS1115 وحساسية الـADC (SRC-MQSL-03)، منع معايرة جميع القنوات عبر MQ2 (SRC-MQSL-08)، فحص طور سخان MQ7/MQ9 (SRC-MQSL-09)، عدم تقديم تشبع المقسم الكهربائي كمعلومة صالحة (SRC-MQSL-10)، معاملات كل حساس (SRC-MQSL-11)، مطابقة gas source curves والهوية الأصلية (SRC-DATA-01)، صحة علاقات إشارة MQDataScience المحوسبة مقابل قياس Tank (SRC-DATA-02).
**P1 (بعد سلامة الطبقة الفيزيائية):** استيفاء CalValue اللوغاريتمي مع اختبارات source version (SRC-DATA-03)، تصحيح T/RH الموثق (SRC-DATA-04)، انحدار بيانات منحنيات المصنع مع تتبع النقاط (SRC-DATA-09)، توحيد صيغ المنحنى الأسية واللوغاريتمية (SRC-MQSL-04/05)، دمج MQ135 البدائل دون وزن مستقل زائد (SRC-MQSL-06)، ثم إعادة فحص دفاتر المصدر الأصلي (SRC-MQSL-07).
**P2 (بحث وتنبؤ وتعلم بعد استكمال raw series وتراكيز/مصادر):** 3D/4D correction & forecasts (SRC-DATA-05/06)، 76 model workbench الخاضع لتقسيم مستقل (SRC-DATA-07)، نماذج المعايرة البيئية المتعددة (SRC-DATA-08)، دعم الحساسات المستقبلية (SRC-DATA-10)، feature combinations environment/time (SRC-DATA-11)، وفحص الصيغة البديلة للتعويض (SRC-MQSL-12).

**ترتيب التنفيذ:** لا ينقل source code إلى Android قبل اجتياز G1 على الصيغة والمجال والتوافق. اختبار G2 منفصل عند وجود بيانات متطابقة، وتحفظ جميع الأصول الغير مفعلة بوسم RESEARCH_ONLY. تقنيات 01.4 و09.6 تبقيان غير منقولتين في V6؛ يجري تطويرهما داخل Next عندما تسمح دارة الجهاز وخطة الحركة. كل تقنية يستدعى تنفيذها ضمن معرفاتها الأصلية P02–P06، ولا تنفذ 23 خاصية دفعة واحدة.

**النتيجة الراهنة من جرد P00.2:**
- 80 سطرًا محل فحص ثابت؛ 11 مرتبطة بملفات ذات استدعاء مرصود على مستوى الفئة في سلسلة الدخول المختارة (ولا يثبت ذلك أن تقنية بعينها فعالة)، 67 لها ملفات كود دون استدعاء مثبت في تلك العينة، و02 غير منقولتين. لا يعد ذلك جرد تشغيل شاملًا ولا اختبارًا فعليًا G1/G2/G3.
- راجع research/v8_implementation/results/next_p00_80_activation.csv وnext_p00_2_static_wiring_evidence.json.
- اكتشاف ازدواج مسار V8: TankInstantAnalyzer يستخدم source_curve_hypotheses وتقيد legacyId < 144، بينما CompletedSessionChemCoreAnalyzer يحتوي مسارًا يستدعي ChemCoreInferenceEngine.analyze التاريخي. إصلاحهما ضمن P08/P09 بعد تأهيل Next.
