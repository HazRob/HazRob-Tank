# مكمّل الخطة الحاكمة: استثمار مكتبة MQUnifiedsensor ومكتبات R0 والمنحنيات والبصمات في ChemCore Tank V6

**التاريخ:** 2026-10-29 · **المعرف:** `V6-002-SOURCES-ADDENDUM-01` · **الحالة:** `INVENTORY_AND_PROPOSED_ADAPTERS; NO_V6_CHEMCORE_EXECUTABLE_PATCH; NOT_DEVICE_VALIDATED`
**الفرع:** `feature/tank-v6-implementation`، دون تعديل HazRob رباعي العجلات.
**المرجع الرئيسي:** `../implementation/V6-CHEMCORE-SCIENTIFIC-MASTER-PLAN-2026-10-29_AR.md`.
**مصفوفات التتبع المستخرجة فعليًا من GitHub:**
- `V6-CHEMCORE-MQUNIFIED-R0-RATIO-AUDIT.csv` — 8 طرازات R0 clean-air ratios ومعايرة دراسة 2018.
- `V6-CHEMCORE-MQDATA-CURVES-43-COMPATIBILITY.csv` — 43 منحنى مصدر أصلي ومعاملاته وحدوده وoutput_scale ومخاطر تحويله.
- `V6-CHEMCORE-HISTORICAL-LIBRARY-14-ASSET-MANIFEST.csv` — 14 أصلًا تاريخيًا في `HazRob/build-harness/library/research/hr069/` مكررة من الأصل بصورة byte-identical.

## A. مصادر الحقيقة ومجالات الاستخدام المسموحة

| المصدر | ما يحتفظ به فعليًا | مجال استخدامه في V6 | حدود علمية |
|---|---|---|---|
| `miguel5612/MQSensorsLib` (`MQUnifiedsensor.h/.cpp`) | `setRL,setR0,calibrate,externalADCUpdate,setRegressionMethod,readSensor,readSensorR0Rs`، أمثلة منحنيات `a,b` | خط أساس رقمي ومحاكاة R0/PPM والتحقق من تحويل الجهد، ومعاملات كمنحنيات بحثية فقط | أمثلة `ratioCleanAir` معيار dimensionless؛ الكود يصف RL/R0 بـ kΩ، والاختبار البرمجي ليس معايرة عتاد Tank |
| Hanwei/Winsen، المراجعة المحددة لكل طراز | رسوم الحساسية `Rs/R0` مقابل غاز/تركيز، جهد التسخين/الظروف، مجالات وقيود التشغيل | رسم مرجعي أولي ونقاط تحقُّق للمنحنيات والبصمات | الرسم نموذج typical مشروط، وليس بصمة غاز معتمدة لجهاز Tank |
| `HazRob/build-harness/library/reference-bank/external-research/calibration/` | سجلات R0/Rs من دراسات ومشاريع ومقارنات عبر عدة أجهزة | تحليل التشتت ومصادر الخطأ والتحقق من تعريف R0، لا استبدال معايرة Tank | لا R0 مطلقة قياسية؛ أخطاء منشورة في MQ7/MQ8، ومشاكل Ω/kΩ |
| `HazRob/build-harness/library/research/hr069/mq_research_curves_43.json` | 43 علاقة `ratio_a,ratio_b,min_ppm,max_ppm,output_scale` على ثمانية أنواع MQ | مصادر مرشحين/نقاط محاكاة ومقارنة عكسية بعد توحيد R0 والسخان | بحثية، `m4=false`، أصلها MQDataScience v6.0.0، 6 علاقات MQ3 لها `output_scale=50` يجب تدقيق وحداته |
| `GasEngine` في المصدر المرجعي الثابت `sohpany/SERVOC-Android@6281d3e` | 39 علاقة power-law موروثة بحسب سجل التدقيق | comparator وsource-backed screening bounded | لا تكرر منحنى من نفسه كمصدر مستقل، ولا ترفع إلى M4 |
| `TankStableScreeningModelRegistry` وStable | 49 prior response model (43 research + 6 references) مع `LOG10_LINEAR`، المصدر وSHA | تشكل خط أساس B0 ومرجع تفعيل V6 بعد التأكد من source/ref/R0 | `sigma_log10` البحثي ليس CI، لا معايرات Tank مادية مفعّلة |
| `identification_fingerprint_priors_v1.json` | 11 analytes ذات priors رقمية + فئتان تشغيليتان (LPG/smoke) = 13 fingerprint families، شبكة تركيز 11 نقطة | مقارنة fingerprint ومصدر مرشح بحثي بوسم provenance | بصمات مولدة من علاقات المصدر ليست 13 تجربة غازية مستقلة ولا 13 مراجع جهاز |
| `food_freshness_headspace_fingerprints_v1.json` | 59 جلسة headspace مجمعة حسب المادة الغذائية | class/odor screening؛ تمييز morphology مع masks | `pure_chemical_identity=false`, `quantitative_ppm=false`; لا تحول "جودة الطعام" إلى CAS غاز مفرد |
| `multimodal_mq_transfer_fingerprints_v1.json` | 6400 **ADC rows** عبر 4 فئات (NoGas,Smoke,Perfume,Mixture)، overlap ستة MQ وMQ4/MQ9 ناقصان | نقل بصمة فئة مع قناع غياب القنوات؛ class-level، resilience | 6400 صف ليست 6400 حقن مستقلة؛ لا تُخلق استجابات MQ4/MQ9 |
| `uci_mq135_lowconcentration_transfer_fingerprints_v1.json` | 18 vector aggregate = ستة غازات × 3 مستويات؛ بيانات المصدر 90 تعرضًا وMQ135 قناة مشتركة واحدة | اختبار single-sensor/one-channel ablation، low concentrations، temporal | 50/100/200 ppb = 0.05/0.10/0.20 ppm؛ لا تحويل إلى معايرة Tank أو مصفوفة MQ8 كاملة |
| `same_sensor_peer_reviewed_identity_evidence_v1.json` | 4 دراسات منشورة، منها Wang et al. 2022 لسبعة طرازات MQ متداخلة، والتباسات isomer/xylene | أولويات الحصول على ملفات أصلية، وكيانات/تطابق جزيئي | دقة الدراسة المنشورة ليست دقة Tank؛ `runtime_numeric_ready=false` عند نقص raw |
| `mq_experiment_source_index_v1.json` | 9 معرفات دراسات مُصنفة | سجل provenance وطلبات raw إضافية | فهرس استشهادي لا executable model |
| `mq_models_76.json` | 76 نموذج MQDataScience: 14 Android native و62 `PORT PENDING` | تخطيط تدريب ML/انحدار؛ ترشيح المنفذ إلى Android | `research_integrated=true` لا يعني trained/validated على كل بيانات |
| `PubChem`, QSPR, 기타 original HR069 assets | تعريف CAS/صيغة/خواص، transfer/olfaction context | identity curation, prior feasibility, report hazard metadata | لا يمكن تحويل الصيغ الجزيئية إلى استجابة MOS مُقاسة دون معايرة أو نموذج متحقق |

**حظر الجمع الساذج:** 43 منحنى قد تكون ضمن 49 Stable، وملفات fingerpint المشتقة من نفس المنحنيات، و14 أصلًا أرشيفيًا يكررون طبقات المركز. لا تُجمع هذه الأعداد ضمن "أدلة مستقلة" أو "عدد التجارب". `current-manifest.json` مصدر 14 أصلًا من `33ee17dbbd291ded7154f5030612d90f6e79f81b`، وGit blob الخاص بنماذج المنحنيات `bd597ffe9ecaee6a0ccfddc5ec1b89c1cccc0ba1`.

## B. R0 في MQUnifiedsensor — نقاط يجب أن يثبتها التنفيذ

| Sensor | `RatioCleanAir` من أمثلة المكتبة | النسبة |
|---|---:|---|
| MQ2 | 9.83 | `Rs_air/R0` |
| MQ3 | 60 | `Rs_air/R0` |
| MQ4 | 4.4 | `Rs_air/R0` |
| MQ5 | 6.5 | `Rs_air/R0` |
| MQ7 | 27.5 | `Rs_air/R0` |
| MQ8 | 70 | `Rs_air/R0` |
| MQ9 | 9.6 | `Rs_air/R0` |
| MQ135 | 3.6 | `Rs_air/R0` |

هذه معاملات **بلا وحدة**، حتى لو حمل تعليق مثال المكتبة كلمة ppm. مكتبة `MQUnifiedsensor` تتعامل مع `R0` و`RL` في كيلوأوم؛ يطبّق محرك V6 الصيغة الأساسية بالأوم ويتحقق من تحويل kΩ×1000 أولًا. 

```text
V_RL = (V_adc - ADC_offset) * ADC_divider_gain (if wired)
Rs_ohm = RL_ohm * (V_c_V / V_RL_V - 1)
R0_ohm = Rs_clean_air_ohm / ratio_clean_air (only where definition compatible)
ratio = Rs_ohm / R0_ohm
log10(ratio) = m*log10(C_ppm) + c
C_ppm = a*pow(ratio,b)   # only if same curve definition and calibration
```

**ملاحظة مصدرية مهمة:** `readSensorR0Rs` في `MQUnifiedsensor.cpp` يعكس النسبة إلى `R0/Rs`، خلاف `readSensor`. يجب أن تحتوي `curve-evidence/1` على `ratio_direction=RS_OVER_R0|R0_OVER_RS`؛ والتحويل (1/r) مشروط بوضوح مصدر المعادلة واتجاه الميل، دون عكس البيانات مرتين. لا تعتبر مجرد التسمية `R0` كافية لتحديد تعريفها.

**اختبار التعاريف:**
- `R0_CLEAN_AIR_RATIO`, `R0_REFERENCE_GAS_AT_PPM`, `RS_AIR_DIRECT_BASELINE` **أنواع مختلفة**؛ أي محاولة استخدام مرجع بغير تعريفه تنتج `INCOMPATIBLE_R0_REFERENCE`.
- `RL` في دراسة 2018 يختلف كثيرًا بين الحساسات؛ افتراض 10kΩ للجميع في Tank فرض عتادي غير مثبت لا تصححه زيادة نماذج ML.
- سجل الدراسة 2018 يحتوي خطأ قسمة في MQ7 وMQ8: احفظ القيمة المنشورة والمحسوبة منفصلتين، ولا ترقي أياً منهما إلى مرجع Tank حتى يُراجع المصدر/جهاز الدراسة.
- `MQ3 output_scale=50` في **ستة منحنيات بحثية**، ويجب اختبار مصدر هذا القياس وإثبات الوحدات قبل تمريره إلى المعكوس أو دمجه مع `MQ3` المنحنيات الأخرى.
- تقارب MQ9 في دراستين (≈8.06 و8.21 kΩ) لا يثبت وجود R0 قياسية، ولا يعوض معايرة حساس مركب فعليًا.
- إذا استُخدمت قيمة R0 محلية من الروبوت، يجب تأكيد `CAL_PENDING → accept → NVS`، وتاريخ/فولتية/سخان/تقرير معايرة محفوظ؛ لا استبدالها بمتوسط قيم منشورة.
- عند وضع `R0_reference_definition=UNKNOWN` لا تُملأ `reportable_ppm`، ويمكن استخدام المنحنى كفرضية بحثية موضحة العجز فقط.

## C. خطة المعالجة الهندسية (لا تعني تنفيذًا في V6 بعد)

### C1 — تقصّي المصدر والتكرار
اقرأ `14 legacy asset manifest`, `43 curves`, `39 legacy equations`, `49 Stable priors`, وكل ملف محلي/مركزي يشير إلى curve/fingerprint. عرّف مفتاح إزالة الازدواج `(source_sha, sensor_model, physical_source_dataset, analyte_UID, model_form, domain, r0_definition)`. `dataset mirror` أو `digitized same graph` ليس مصادر جديدة مستقلة.

### C2 — استيراد منحنيات المصنع والنماذج بدقة
- استخرج نقاط الرسم الأصلي من PDF/مخطط المصنع عندما يتوفر ويجوز استخدامها؛ خزّن نقاط `ppm, Rs/R0` مع نسخة الداتا شيت والمخطط وظروفه وأداة استخلاص النقاط وخطأ digitization.
- احسب نموذج `power-law`, `piecewise log interpolation` و`log10 linear` فقط عندما يكون المجال صالحًا؛ عقد يمنع الاستقراء ويعرض residual لكل خيار.
- قارن بالـ43 منحنى MQDataScience والـ39 legacy وعلاقات Stable 49، واعرض اختلاف الميول/المجالات والصفوف ذات `output_scale`.
- نموذج `manufacturer typical` يظل `M1/M2 RESEARCH` ولا يمنح M4.

### C3 — إعادة استعمال البصمات القائمة
- أنشئ `FingerprintAdapter` لكل من `numeric_source_backed_analytes`، `food_headspace`، `multimodal four classes`، `UCI1081 MQ135 aggregates`، ودراسات تطابق الطراز المنشورة.
- احتفظ بالفضاء الأصلي للبصمة (log ratio vs geomean vs ADC baseline)؛ لا تدمج متجهين من فضاءين مختلفين بحساب cosine مباشر.
- `channel mask` لكل مرجع، `source-array-order`، `feature_schema`، `calibration_version`. غياب MQ4 وMQ9 في بيانات `multimodal` لا يملأ صفرًا أو قيمة مصطنعة؛ استخدم subspace مستقل.
- `class-based headspace` يجب أن يبقى class-only؛ `source-based molecular fingerprints` hypothesis-only حتى يأتي مرجع Tank.
- لا تعتمد على `same_sensor` peer-paper من دون raw متاح لبناء مقياس إحصائي جديد.

### C4 — محاكاة مقارنة R0 والمنحنيات والبصمات
**Crossed simulation with frozen source IDs:** 
1. `R0_LOCAL` (قياس فعلي محفوظ إن وجد) ضد `R0_PEER_SOURCE` (لتحليل الحساسية فقط) ضد `Rs_AIR_BASELINE` و`R0_REFERENCE_GAS`; الحالة الأخيرة مقبولة فقط إذا كانت المعرفة كاملة. لا تعيّن قيمًا مفقودة.
2. `curve=manufacturer typical` مقابل `MQDataScience43` مقابل `GasEngine39` مقابل `Stable49`، مع محاكاة بيانات forward من مصدر واختبار inverse بمصدر آخر **بوصفه انتقالًا بحثيًا، لا دقة مستقلة**.
3. `fingerprint=static` ضد `temporal` ضد `subspace6` ضد `MQ135_single` على مصادر الباحثين التي تتوافر لها labels/sets ومستويات فعالة.
4. `identity` ethanol/acetone/toluene/benzene/CO/methane/H2 ونماذج `Alcohol`, `LPG`, `Smoke` كفئات متقاطعة، مع الامتناع عندما لا تتوفر أدلة انتقائية.
5. `reference curve train` و`peer curve evaluation` مرتبطان بمصدر ورسم واحد؟ إذا نعم لا تعتبر النتيجة اختبارًا مستقلًا. أعد validation على مصدر جديد في phase P05.

**مخرجات SIM-R0:** `r0_cross_source_compatibility.csv`, `curve_unit_roundtrip.json`, `response_prior_dependent_fingerprints.jsonl`, `fingerprint_overlap_mask_test.json`, `ppm_sensitivity_to_r0.csv`, `candidate_shift_r0_source.csv`, `duplicate_lineage_report.json`.

### C5 — بوابات قبول نقل الأصول إلى V6
- S0: source SHA/revision/licence and unit verified.
- S1: exact chemical UID/class semantics validated (no hidden synonym mapping).
- S2: RL/Vc/ADC/r0 definition/ratio direction and heater source compatible or `SCREENING_ONLY`.
- S3: forward/inverse round-trip + independent source residuals + domain limits + channel masks.
- S4: training and threshold never use sealed test; grouped by independent exposures.
- S5: run ablation to show incremental value relative to V5 on real external datasets.
- S6: Android model port only if memory/latency contracts and mathematical parity pass; otherwise PINNED_LAB.
- S7: gas ppm M4 requires device-validated physical reference gas, never library-model approval alone.

## D. إدراج في المخطط الرئيسي P00–P11

| المرحلة | إضافة إلزامية |
|---|---|
| P01 | فهرسة 14 legacy asset، 43 curve، 39 equation، 49 Stable؛ لكل source canonical SHA وindependence group |
| P02 | عقود R0 وRL/Vc و`ratio_direction` وMQ7/MQ9 heater مع سجل مصادر R0 المنشورة |
| P04 | SIM-R0/sources والأخطاء ±/×؛ outputScale=50؛ وmismatch `RS_OVER_R0`/`R0_OVER_RS` |
| P05 | أحجام عينات حقيقية للحزم `food-headspace`, `multimodal`, `UCI1081` وpeer MQ studies مع group-aware splits |
| P06 | `SourceCurveRegistryV6` ومرجع `R0CurveCompatibilityGate`، حفظ جميع البدائل مع تجنب voting double count |
| P07 | `FingerprintAdapterV6` في فضاء الميزات الصحيح وإزالة التبعية من استنتاجات الفئات |
| P08 | توجيه `incompatible R0` و`source conflict` إلى رفض/تفسير، وليس إلى رفع الرتبة |
| P09 | تكامل المصادر في شاشة المختبر والقياس الآني وتقارير PDF/Excel مع حقول المصدر والنسخة |
| P10 | لا يرقي أي مرجع خارجي بدل تقرير G3 Tank الفعلي |

**قاعدة القرار:** `REUSE_ALL_RELEVANT_REFERENCES / NEVER_ASSUME_ALL_ARE_INDEPENDENT_OR_DEVICE_VALIDATED`.

## E. ملفات ومصادر مرجعية ثابته

- `HazRob/build-harness:main/library/research/hr069/current-manifest.json`, `mq_research_curves_43.json`, `identification_fingerprint_priors_v1.json`, `food_freshness_headspace_fingerprints_v1.json`, `multimodal_mq_transfer_fingerprints_v1.json`, `uci_mq135_lowconcentration_transfer_fingerprints_v1.json`, `same_sensor_peer_reviewed_identity_evidence_v1.json`.
- `HazRob/build-harness:main/library/reference-bank/external-research/calibration/MQ_R0_RS_CROSS_STUDY_2026-10-21.json` blob `03198302d478c9f54cf84a79b5bdec1b600612ca`; `MQ_R0_ADDITIONAL_5_SOURCES_2026-10-21.json` blob `34b3cbc64cbc881444fbeefa54fda92448cbedfb`; `calibration_audits/mq_r0_rs_peer_sources_2026-10-21.json` blob `8565d6f2d9c27bf459d3f63f4350b4b19d9ecf3b`.
- `MQSensorsLib` code: https://github.com/miguel5612/MQSensorsLib/blob/master/src/MQUnifiedsensor.cpp — `calibrate(),readSensor(),readSensorR0Rs()`; https://github.com/miguel5612/MQSensorsLib/blob/master/examples/Calibration/Calibration.ino .
- No manufacturer datasheet revision or PDF has been independently digitized in the present update; do this within P01/P06 and preserve source graph uncertainty.

**ملاحظة:** بيانات V5/Stable لا تتغير بإضافة هذه الخطة، ولا صُنع APK أو تعديل فيرموير أو تشغيل G3 ضمن هذه الإضافة.
