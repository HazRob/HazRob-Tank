# HazRob Tank V8 — MQDataScience / MQSpaceData 7.0.0 العلمي، تأسيس ChemCore Next
**التاريخ:** 2026-11-02 (Asia/Aden)  
**النطاق:** تدقيق أولي عميق للشيفرة المصدرية ذات الصلة بالاستدلال والمعايرة والتنبؤ؛ `AUDIT_IN_PROGRESS` وليس تصديقًا لكل ملفات المستودع أو بياناته.  
**قرار المالك:** فحص MQ Data Science أولًا، ثم إضافة الفجوات المستحقة إلى الجيل الجديد `ChemCore Next` المبني على ChemCore V6، دون الرجوع إلى V5.  
**فرع التنفيذ:** `feature/tank-v8-implementation`؛ الفروع الأصلية V5/V6/V7 لا تُعدّل.  
**المصدر الأصلي:** https://github.com/abcdaaaaaaaaa/MQDataScience  
**نسخة المصدر:** `library.properties version=7.0.0` على `main` وقت الفحص؛ محفوظ تاريخيًا `v6.0.0`. الترخيص MIT في مستودع المصدر؛ يلزم الحفاظ على إشعار الترخيص ونسبة المصدر عند إعادة استخدام ملفات.  
**النسخة الأصلية المؤرشفة في HazRob Tank:** `research/chemcore_v7/mqspacedata_v7_upstream/` عند `db0efc3e4c6736c1983fc82b5304095ebd44737f`، ولا تُفترض مطابقتها للمصدر 7.0.0.

## 1. الملفات والإصدارات التي فُحصت على المستوى المصدري
| الأصل | النوع | Git blob في المصدر 7.0.0 |
| --- | --- | --- |
| `DataScience/Requirements/PredictData.py` | 76 نموذجًا، اختيار CV، إنتاج 4D | `bacf95948714dbe76f09c57e3db14cbc60616580` |
| `DataScience/Requirements/MQInfo.py` | 21 نوع/وضع MQ، معاملات المصدر وأنماط المعايرة | `c9519bcebd22c79fadeaa6b21d22d4fb821ba290` |
| `DataScience/3D_Surface/3DSurface.py` | سطح التصحيح الحراري والرطوبي ومنحنيات PPM المصدرية | `0f55b726e07730d2cfa4f33f72da46b65d0a31e6` |
| `DataScience/4D_Curve/4DCurve.py` | وقت/نسبة حساس/حرارة/رطوبة وإسقاط 4D | `cc620184f38461fa02ae1c8a52033d04e91c176d` |
| `DataScience/4D_Curve/readppm.py` | تحويل الخصائص إلى قيم منحنيات ppm بحثية | `be1b5d98ac4b330d9594e065e69c08a91180a1b7` |
| `src/GasSensor.cpp` | تحويل ADC/نسبة وعكس المعادلات | `b7a1d9aaaae0ed27bcbbdb5142b62ba3b448a95f` |
| `src/Correction.cpp` | معاملات تصحيح T/RH للمستشعر | `a4aafa662ae94b66bb8b14f32ec1fb8e54943f56` |
| `src/AirQuality.cpp` | الاستيفاء غير الخطي / نطاق الهواء | `1fdf5fcbe1929bea370ca2050f6762174af6e8f0` |
| `DataScript/data.js` | محاكاة/عرض/تحويلات المستشعر | `b4cefac3ae756c1aae824a9895ce1dd40873121e` |
| `DataScience/Models/ModelGases/ModelCurve.py` | نماذج منحنيات المادة المصدرية | `4b57abe6300b59ba19cf047a98061b0593837d28` |
| `DataScience/Models/ModelSensor/SensorCurve.py` | نمذجة منحنى الحساس | `d30d04c4b86542d36d3e13b564b57949964575b8` |

المصدر الرئيسي يتضمن أيضًا `Regression/`, `examples/`, `DataScript/`, `src/`, وملفات بيانات. لم يُثبت بعد فحص كل ملف في هذه المجلدات ولا اختبارات تشغيل المصدر الكامل. لذلك لا يجوز وصف هذه المرحلة بأنها `FULL_REPOSITORY_AUDIT_COMPLETE`.

## 2. مقارنة v6.0.0 → v7.0.0
المقارنة المصدرية لـ`v6.0.0...main` أظهرت نحو 40 commit وعدد 23 ملفًا في قائمة الفروق. التغيير الحسابي الأبرز: انتقال معايرة النطاق من الاستيفاء الخطي
`(C-Cmin)/(Cmax-Cmin)`
إلى الاستيفاء اللوغاريتمي العكسي
`(log10(C)-log10(Cmin))/(log10(Cmax)-log10(Cmin))`
داخل `GasSensor.cpp` و`3DSurface.py` و`4DCurve.py` و`readppm.py` و`data.js`. وهذا يغيّر نتيجة المعايرة حتى عندما لا تتغير معاملات منحنى الغاز. لا يجوز نقل هذا التغيير مباشرة إلى R0 الخاص بـTank دون تحديد المقصود بنسبة ADC وقيمة `CalValue` و`RL` و`Vc` و`R0` واتجاه `Rs/R0`.
- `MQInfo.py` تغيرت أربعة `calvalue` مصدرية: MQ306A/Ethanol: 0.873876→0.976666، MQ307A/H2: 0.999619→0.999572، MQ309A/alcohol وCO: 0.83393→0.988237.
- فهرس 76 نموذجًا في `PredictData.py` لم يظهر في قائمة الملفات التي غُيّرت بين المرجعين؛ لا يعني ذلك نجاح هذه النماذج أو تكافؤها في Tank.

## 3. نتائج تدقيق الوظائف
### 3.1 فيزياء مدخل المستشعر
تستخدم طبقة المصدر `SensorValue` / `Per` المشتقة من ADC/النسبة المئوية، وتحويلًا كهربائيًا يتضمن `Air`, `CalValue`, `SensorRLCalRL`, `RL`، قبل حساب تركيز على معادلة `Rs/R0=a*(ppm/scale)^b`. `Per/100` لا يساوي `Rs/R0` رياضيًا، ولا يجوز نقل شكل 3D/4D باعتباره ميزة عينات MQ المقاومة الخاصة بالدبابة.
تحتاج واجهة Next إلى `RawElectricalSample{adsRaw, fullScaleV, Vc, RL, circuitTopology, channelId, timestamp, heaterPhase}` و`NormalizedResistance{Rs, R0, ratioDirection, definition, provenance, uncertainty}`، مع سجل إن كانت القيم مقدرة أو فعلية. المصدر يعرض نماذج لأجهزة غير مركبة على الدبابة، ولا يفترض توفرها آنيًا.

### 3.2 3D
`3DSurface.py` يرسم ppm في دالة لدرجة الحرارة والرطوبة وبمعاملات حساسية مصدرية؛ يستعمل `a(RH)+c(RH)*exp(b(RH)*T)` مع الاستيفاء بين حالات الرطوبة، ويعرض السطح باستخدام Plotly. هذا مفيد لنقل معادلة مصحّحة مشروطة بالمستشعر وللمحاكاة، **وليس نموذجًا مثبتًا لتحديد هوية غاز في خليط**. يجب تحقق مجال `T/RH` والمجال المرجعي للغاز وعدم إخفاء القيم الخارجة باستخدام `clip` في قرار السلامة.

### 3.3 4D
`4DCurve.py` و`readppm.py` تقرآن الأعمدة `Time,Per,Temp,Rh`، وتطلبان التنبؤ بكل سلسلة باستدعاء `get_best_fit` ثلاث مرات. إسقاط القيم المستقبلية للحرارة والرطوبة يتم بالاستيفاء/الاستقراء من السلسلة المشاهدة؛ وبذلك لا تكون عوامل الطقس المستقبلية مقاسة. التنبؤ بقيمة `Per` ليس دليلًا على هوية الغاز، والتنبؤ بتركيز مستنبط من فرضية غاز ليس تركيزًا مقاسًا.
 
### 3.4 76 نموذجًا
`PredictData.py` يسجل 76 نموذج انحدار/تنبؤ، وليس 76 مصنّف هوية. بحسب مصفوفة V6 المثبتة: **11 نماذج الاستجابة البيئية، 38 الانحدار/المنحنيات، 14 ديناميات زمنية، 13 تعلم آلي**. تاريخيًا: **14 منفذة في Android بصورة مسجلة (12 توافق مصدر موثق، 2 تقريب خوارزمي)، و62 غير منقولة**. هذه أرقام مصفوفة V6 ولا تثبت ربط الـ14 بوظائف ChemCore V6 المستقل ولا اجتياز G2 أو G3 في V8.
الملف المفصل: `docs/research/CHEMCORE-NEXT-MQDATA-76-MODEL-GAP-2026-11-02.csv`.
  
### 3.5 مشكلات التقييم التي يجب إصلاحها قبل نقل 4D
- `ModelRunner.run()` يستخدم `KFold(n_splits=2, shuffle=True, random_state=42)` لمعظم النماذج، و`TimeSeriesSplit(n_splits=2)` للنماذج الزمنية. تقسيم صفوف مسار زمني عشوائي قد يسرّب معلومات الجوار الزمني والتعرض نفسه إلى التحقق.
- اختيار أفضل `R²` من **76 مرشحًا** على نفس CV مصدر لتحيز اختيار/تفاؤل الأداء؛ لا يوجد اختبار خارجي untouched مستقل في هذه الدالة.
- `SklearnModel.fit()` يستخدم داخليًا `GridSearchCV(..., cv=2, scoring='r2')` دون تصنيف مجموعات التعرض/الجهاز. حتى وجود CV متداخل لا يغني عن holdout منضبط وتجارب أيام/أجهزة.
- المتغيرات البيئية المستقبلية تُستنتج عبر `interp1d(..., fill_value='extrapolate')`، ولذلك يجب إخراج فرق واضح بين قيم مقاسة وقيم متنبأ بها وقيم مفترضة.
- `clip/limit` في 3D/4D قد يخفي تجاوز مجال المصدر؛ يُعرض سبب التجاوز ولا يُسوّى مع عينة مضمونة.
- جميع هذه المخرجات نماذج مصدرية بحثية؛ نموذج انحدار مناسب لـPer لا يثبت دقة PPM ولا التعرّف على مركّب.
 
## 4. فجوات ومخاطر ChemCore V6 المؤكدة من الشيفرة
1. `Engine.analyze()` يحسب 49 منحنى مصدر ويُرتب المرشحين أولاً على أساس عدد الحساسات المتوفرة ثم RMSE؛ هذا ليس ترتيب احتمال هوية. قد تكون بواقي ملاءمة منحنى واحد صفراً أو شبه صفر دون دلالة تمييزية؛ يلزم اختبار identifiability وmulti-gas cross-sensitivity وUNKNOWN.
2. `ChemCoreTankV6.analyze()` يستدعي أساسًا `Engine` و`FingerprintEngineV6` و`EvidenceConflictV6`. لا يظهر من الكود أن وحدات `ClassicalLearningV6`, `TemporalSpatialResearchV6`, `SourceValidationV6`, `OpenSetResearchV6`, `TemporalNeuralResearchV6` تُنفّذ في ذلك المسار القياسي. وجود class لا يساوي تنشيطًا.
3. مسار القياس الآني في `TankInstantAnalyzer` يبني قائمة المرشحين من `source_curve_hypotheses` بترتيب `LinkedHashMap` و`rank=distinct.size()+1`، وليس من `Engine.Result.rankedCandidates`. وهذا يفسر ثبات ترتيب المرشحين المعروض دون إثبات ثبات تقديرات المحرك.
4. `CompletedSessionChemCoreAnalyzer` لا يزال يستدعي `ChemCoreInferenceEngine.analyze` في مسار الجلسات؛ يلزم توحيد استدلال الجلسات الجديدة مع Next دون إعادة كتابة مخرجات الجلسات التاريخية.
5. `TankV6TelemetryAdapter.analyzeSnapshot` لا يحول snapshot أحاديًا إلى بصمة مصدرية أو سلسلة UCI مصطنعة، وهذا قرار سليم؛ لكن الاستفادة من السمات الزمنية تحتاج حفظ سلسلة المصدر كاملة وتوليدها بواجهات ظاهرة مع provenance.
6. `CleanAirResponseGateV6.DEFAULT_DROP_FRACTION=0.10` عتبة تجريبية غير مؤهلة بالملاحظة، وظهور فرضيات الغاز مشروط بنوعين مختلفين مستجيبين. لا يصح استعمالها لإثبات خلو الجو أو منح شهادة إنذار.
7. تغطية 80 تقنية: أحدث سجل تغطية V6 = `78 PARTIAL / 2 NOT_PORTED (01.4,09.6) / 0 G3`. أرقام سابقة `34/80` في tracker's checkpoint أولي وليست أحدث لقطة تنفيذ؛ التفعيل التشغيلي أقل من عدد الأصناف الجزئية.

## 5. المعايير العلمية والتجريبية الحاكمة
- قياسات الغاز المرجعية والمقارنة المستقلة وفق NIOSH `Components for Evaluation of Direct-Reading Monitors for Gases and Vapors` (DHHS/NIOSH 2012-162): response time, bias, precision, repeatability, drift, interference, environmental effects and uncertainty. https://www.cdc.gov/niosh/docs/2012-162/default.html
- متطلبات اختبار الأجهزة ومراجعة المعايرة بحسب إرشاد OSHA للأجهزة المباشرة https://www.osha.gov/publications/shib093013 ؛ ليس معنى ذلك أن الروبوت أصبح compliant أو مصدقًا.
- MQ-7 يعتمد في مواصفات Hanwei على تسخين دوري في نماذج التشغيل القياسية؛ يلزم مطابقة إصدار القطعة الفعلية ودورة السخان قبل استخدام منحنى CO باعتباره صالحًا لقياس Tank. https://www.sgbotic.com/products/datasheets/sensors/MQ7-datasheet.pdf
- `G1`: المطابقة العددية والوحدات والحدود. `G2`: اختبارات grouped holdout مستقلة بحسب التعرض/الجلسة/اليوم/الجهاز، ومقارنة baselines، ومؤشر UNKNOWN. `G3`: تعريض Tank فعليًا لهويات وتركيزات موثقة بأجهزة مرجعية ومعايرات صالحة.

## 6. أولويات ChemCore Next
| الأولوية | المكوّن | النتيجة المطلوبة |
| --- | --- | --- |
| P0 | عقد المدخلات الكهربائية وR0 وheater phase | توحيد نسبة المصدر والتحقق من مجال المعادلة |
| P0 | 3D per-sensor T/RH correction | مطابقة قيم المصدر واختبار عدم تحسين زائف |
| P0 | إصلاح عرض المرشحين وفصل المصدر عن القرار | تغير مرشحين فعلي/UNKNOWN بدون ترتيب كتالوج |
| P0 | توحيد جلسات جديدة وقياس آني | نتيجة واحدة قابلة للتتبع دون تغيير التاريخ |
| P1 | 4D causal temporal features + forecasting | تجارب متعاقبة واختبار baseline وCI |
| P1 | model-selection workbench للـ76 | اختيار اختياري في بيئة البحث، لا حزم 76 نموذجًا جميعها في APK |
| P1 | معايرة احتمالية/رفض المجهول | G2 مستقل قبل دمج قرارات المستخدم |
| P2 | 3D/4D visualization | مخططات تفسيرية تفصل القياس عن التنبؤ |
| P2 | مصادر حساسات MQ أخرى في المكتبة | مراجع غير فعّالة حتى تركيب حساس فعلي وتوثيقه |

## 7. الحالة الحالية ونطاق الادعاء
- `AUDIT_IN_PROGRESS`: فُحصت الملفات الرياضية المحورية وفرق v6.0→v7.0 ومصفوفة 76؛ يلزم فحص بقية البيانات والأمثلة و`Regression/` و`DataScript/` و`examples/` واستخراج البيانات الخام وفحص hash/تبعيات النقل.
- `NEXT_ENGINE_CODE_NOT_PROMOTED`: لا تغيير إلى محرك الاستدلال التشغيلي قبل بناء نموذج Next واختباره منفصلًا.
- `UPSTREAM_EXECUTION_NOT_RUN`: لم تُنفّذ حزمه Python الأصلية ولا benchmark جديد في هذه الدفعة؛ نتائج مراجعة مصدرية لا تمثل Pass تجريبيًا.
- `TANK_G3=0/80`: لا تُستخدم التقديرات لاتخاذ قرار تعرض أو اشتعال أو اعتماد PPM/هوية.
