# ADR — ChemCore Next (HazRob Tank V8)
**التاريخ:** 2026-11-02 · **الحالة:** APPROVED_DIRECTION / SCIENTIFIC_AUDIT_AND_DESIGN  
**الاسم الجديد:** ChemCore Next  
**الأصل المستمر:** محرك `com.hazrob.tank.chemcore.v6` المستقل، لا محرك V5 أو HR069.  
**قرار المالك:** أولوية كاملة للتدقيق العلمي لمستودع MQDataScience ومطابقة تقنياته ونقل المفقود أو الناقص وفق اختبارات مستقلة، قبل تفعيل المحرك الجديد في V8.

## الحدود
1. يُحفظ V6 كما هو لإعادة التشغيل والمقارنة؛ Next امتداد/إعادة هندسة مدروسة على هيئة namespace جديد `com.hazrob.tank.chemcore.next` وإصدار تحليلي `next/0.1.0-research`، منفصل عن versionName الخاص بتطبيق V8. لا تُنسخ البنى القديمة بوصفها حقيقة مرجعية.
2. يُستقبل `MeasurementEvidence` مع الهوية المادية للقناة، وحدة القياس، `ADC raw`، الجهد الفعلي، مقاومة الحمل، `Rs`, `R0`, `R0 definition`, الطابع الزمني المتزامن، الحرارة والرطوبة ومرحلة السخان، والمصدر/calibration. تخضع مدخلات المنحنى لاختبارات ratio_direction وsource compatibility قبل تفسير PPM.
3. المنحنيات والبصمات والنماذج تبقى مكتبات أبحاث ذات مصادر. لا تتحول أي بيانات مصدرية رقمية أو GHS/PEL إلى هوية غاز تشغيلية أو إنذار معتمد بسبب عدد الخوارزميات.
4. يُعرض `UNKNOWN` عندما لا توجد أدلة مؤهلة للهوية؛ يمكن إظهار كل الفرضيات الصالحة حسابيًا في مرفق بحثي مع تضارباتها؛ لا يجوز استخدام الأرقام البحثية لاتخاذ قرار سلامة.
5. تُمنع أدلة الأغذية من الدخول إلى ChemCore Next المخصص للغازات، وتبقى كما هي في وحدة نضارة الأغذية المستقلة.

## البنية المقترحة
```text
Tank ESP32 telemetry / archived field samples
  -> Immutable SignalSnapshot + calibration/quality provenance
  -> ElectricalNormalization (as-built Vc/RL/ADS/r0_direction/heater_phase)
  -> SourceEquationRegistry [V6 49 curves + MQDataScience v7 89 source rows staged]
  -> EnvironmentalCompensation3D [source-qualified T/RH only]
  -> SignalFeatures [static + temporal + drifts]
  -> Parallel research branches:
       SourceCurveFits / CompatibleFingerprintMatches
       4D ForecastLab / 76-model offline selection (not blanket Android)
       Mixture/Disagreement Diagnostics / known-vs-unknown research
  -> EvidenceGraph (one physical sensor counted once, provenance)
  -> DecisionGate [UNKNOWN or research-ranked; G2/G3 qualified claims only]
  -> One immutable ChemCoreNextResult {schema, version, sha, evidence, rank,
                                      abstention, independent_counts, reasons}
  -> V8 UI + PDF + XLSX + session record (exact same result)
```

## التطوير على ثلاث مستويات
**Level A — استيعاب مرجعي:** فهرسة جميع 89 نموذج معادلات غاز داخل `MQInfo.py` (43 لمجموعة حساسات Tank الثمانية و46 لحساسات أخرى؛ إلى جانب 6 منحنيات MQ135 بديلة في V6). تُوسم حساسات غير مركبة `LIBRARY_ONLY_NOT_RUNTIME` ولا تُنشأ قراءات لها. جدول البداية:
`research/chemcore_next/mqdata_v7_source_gas_curve_register_89.csv`.

**Level B — تطبيق رياضي اختياري:** نقل تصحيح الحرارة والرطوبة وفق المصدر وتحت مصدرية ثابتة، ثم تقنيات 3D/4D وتوقع السلاسل، مع اختبار G1 واختيار نماذج G2 مستقل. لا يُقحم 76 نموذجًا كاملًا في APK دون ضرورة مثبتة وقياس CPU/RAM.

**Level C — تشغيل Next:** ربط محرك Next فقط بعد إثبات ranking الصحيح وتحكم UNKNOWN وتطابق قرار القياس الآني/الجلسات والتقارير؛ توجد بوابة `FeatureFlag` للرجوع إلى V6 على مستوى التطبيق. لا رجوع إلى محرك V5 للاستخدام الحي، ولا تعديل بيانات V5/V6 التاريخية.

## بروتوكولات التحقق
- **Source audit:** تثبيت URL/tag/sha/blob/license لكل ملف مصدر، ومجالات ppm وRH وT وADC والاتجاه والمستشعر.
- **G1:** مطابقة معاملات وعكس المنحنيات و3D/4D وتحويلات V6→V7؛ اختبارات حدود الصفر والسالب وNaN والتشبع والتعارضات، حساسات MQ7/9 وتجهيز R0.
- **G2:** تقسيم فعلي `GROUPED` على مستوى التعرض/الجهاز/اليوم/الدورة، حجز test غير مستخدم للاختيار، والالتزام بمرجع simple baseline أو last-value. يمنع اختيار الأفضل من نفس test بعد 76 تجربة.
- **G3:** Tank مادي بمعايرات مصدر معلوم وأجهزة قياس مرجعية، قياسات وبيانات خام ومقارنة انحياز ودقة وزمن استجابة وتداخلات حسب NIOSH.
- **UI contract:** `rank, UID, support, residual, source, unknown_reason` متطابقة في شاشة قياس الغاز وتقرير اللحظة/الجلسة/الموقع؛ عدم عرض مصدرية المنحنى بوصفها ترتيب كشف.

## سجل الترحيل الأولي
1. `NEXT-00`: مراجعة المصدر v7.0 وتثبيت كل المصادر — **IN_PROGRESS**.
2. `NEXT-01`: مقارنة النسخة v7 مع 49 منحنى/24 بصمة و80 تقنية و76 نموذج — **IN_PROGRESS**.
3. `NEXT-02`: بناء G1 equation oracle و3D environment — **NOT_STARTED**.
4. `NEXT-03`: 4D causal model lab وتصحيح 76 CV — **NOT_STARTED**.
5. `NEXT-04`: معمارية الأدلة/UNKNOWN والمخاليط واختبارات G2 — **NOT_STARTED**.
6. `NEXT-05`: التكامل التشغيلي V8 وتوحيد UI/التقارير — **NOT_STARTED**.
7. `NEXT-06`: G3 وتأهيل الاعتمادية — **NOT_STARTED**.

**سجل التدقيق:** `docs/research/CHEMCORE-NEXT-MQDATASCIENCE-SCIENTIFIC-AUDIT-2026-11-02_AR.md`  
**مصفوفة الـ76:** `docs/research/CHEMCORE-NEXT-MQDATA-76-MODEL-GAP-2026-11-02.csv`  
**المرجع العلمي للتأهيل:** https://www.cdc.gov/niosh/docs/2012-162/default.html

لا يشير هذا ADR إلى وجود محرك Next مدمج أو APK V8 أو G2/G3 ناجح؛ هو قرار معماري ومسار اختبار.
