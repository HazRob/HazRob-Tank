# HazRob Tank V6 ChemCore — checkpoint 03: mathematical source-curve kernel

**تاريخ التوثيق:** 2026-10-29 · **الحزمة:** `V6-002 / P06 (PARTIAL)` · **الفرع:** `feature/tank-v6-implementation`.  
**آخر تنفيذ مثبت:** GitHub Actions [40917593589](https://github.com/HazRob/HazRob-Tank/actions/runs/40917593589)، commit `1e52e2f7605b2ef7de5555ffea78ca402ed101cc`، `conclusion=success`.

## 1. التنفيذ الجديد

- `android/app/src/main/java/com/servoc/app/chemcore/TankV6CurveKernel.java` — commit `85ee3934b620dc58a737a485316d3ae17d74699d`.
- `research/chemcore_v6/tests/java/TankV6CurveKernelTest.java` — commit `ec67ee5283dadce154bc535ebb374cfed642e940`.
- Workflow scientific CI updated in `1e52e2f7605b2ef7de5555ffea78ca402ed101cc`.

**وظيفة الوحدة:** تنفيذ حساب منحنيات `Rs/R0 = a (C/outputScale)^b` وعكسها في Java بدلالة المجال المحدد والمصدر وتعريف R0 ونوع الحساس ونطاق الجهاز/الحساس. ترفض القيم خارج المجال، والمصدر غير المؤهل، ومعاملات الصفر وغير المنتهية، وتدقق المصدر بجزء `TankV6SourceCompatibility`. كلها تُرجع `RESEARCH_SCREENING` ولا تتضمن مسارًا يسمح بـ M4 أو `reportablePpm` من مجرد نوع source.

**التحقق العددي:** 15 تحقق Java جديدًا، يتضمن:
1. `outputScale=50` لنموذج MQ3: عند `a=2,b=-0.5` و`C=200 ppm` كمدخل افتراضي للدالة تعطي النسبة 1، ثم يعيد الحساب العكسي القيمة نفسها.
2. رفض اختلاف تعريف `R0` بين العينة والمنحنى، وخروج التركيز المحسوب عن `ppm_min/max`.
3. رفض نقل نموذج مقيد بجهاز محدد إلى جهاز آخر ولو تطابقت أسماء الطرازات.
4. دعم الاتجاه `Rs/R0` أو `R0/Rs` فقط إذا عرّف المصدر اتجاه منحناه مسبقًا.
5. رفض معاملات (a=0)، وميل (b=0)، وقيم NaN، وإبقاء التقرير **research-only** حتى مع تمويه metadata للمصدر بأنه `deviceValidated`.

المعاملات `a=2,b=-0.5` في الاختبار **fixture افتراضي للتأكد من الجبر، وليس منحنى منشورًا أو معايرة Tank**. تستمر اختبارات 43 المنحنيات الحقيقية من المصدر المحفوظ في Python (387 roundtrips).

## 2. حالة CI المجمعة

| المجال | نتيجة مثبتة |
|---|---:|
| Python source + G2 + SIM-B | **50/50 PASS** |
| Java R0 compatibility | **31 assertion PASS** |
| Java V6 evidence consensus | **33 assertion PASS** |
| Java bounded curve kernel | **15 assertion PASS** |
| إجمالي تحققات Java | **79 PASS** |
| عدد منحنيات المصدر المتحقق رياضيًا | 43 منحنى، 387 نقطة |
| اختبارات R0 افتراضية | 172 حالة، 94 خارج المجال |
| SIM-B الزمني والبيئي الافتراضي | 8 سيناريوهات |
| سجل الباحثين التاريخي | 4 ملفات G2 أصلية مثبتة بصمات Git، ليس raw re-execution |
| G3 Tank الفيزيائي | **0/80** |

**ملف نتائج GitHub Actions:** `chemcore-v6-source-research-audit`، artifact ID `12916292465`.

## 3. ماذا تم وما لم يتم؟

تم إنجاز النواة الرقمية للمصدر والمنحنيات وعقد R0 والدمج التشخيصي المستقل، إضافة إلى محاكاة ومراجعة بيانات الباحثين وسجلات CI؛ لا تعتمد أي نتائج غاز تشغيلي على هذه الوحدات بعد.

**لم يكتمل:** التوافق الفيزيائي RL/VC/ADC/سخانات MQ على Tank، تدقيق المعنى العلمي لــ43 مصدرًا على جهازنا، تشغيل UCI270/308/309 من RAW في V6، تدريب/تأهيل نموذج أجهزة متعددة، توصيل `TankV6EvidenceConsensus` بمخرجات `TankInstantAnalyzer` والجلسات، Gradle/Android full suite، إصدار APK V6 أو اختبار هاتف فعلي. لا تُبدل نتائج V5 حتى تجتاز تلك المتطلبات.

## 4. الترتيب التنفيذي التالي بعد الاستئناف

1. **P06:** إعداد `SourceCurveRegistryV6` يستورد فقط مصادر مستقلة وموثقة ويربط `source_id,root_id,r0_definition,ratio_direction,source_SHA,ppm_unit,domain,heater_phase`. لا تدمج `43` و`49` و`39` بأعداد خام دون dedup.
2. **P03/P08:** تحويل `ChemCoreEvidenceObservation` إلى مدخل `TankV6EvidenceConsensus.Evidence` كتشخيص غير مؤثر على المخرجات التشغيلية، مع اختبار serialization وحالة الأصول المفقودة. لا تقبل المطابقة الاسمية للحساس على أنها instance-level source validation.
3. **P05:** جلب ملفات UCI270/308/309 الخام من المصدر إذا أتاح النظام ذلك، ومقارنة hashes وسجل التجارب الأصلية ومنع leakage قبل أي claim أداء جديد.
4. **P09:** اختبار Android CI جامع، وعمليات القياس الآني والجلسات والتقارير وR0 وSQLite والانقطاع عن الاتصال والمخارج.
5. **P10:** لا يقبل G3 حتى تصل تعرّضات Tank بغازات مرجعية حقيقية مستقلة.

**رابط المتتبع:** `docs/implementation/V6-CHEMCORE-EXECUTION-TRACKER.json`. **حالة الاستئناف:** `P06_SOURCE_CURVE_JAVA_MATH_PASS_NOT_INTEGRATED; P08_CONSENSUS_JAVA_PASS_NOT_INTEGRATED; CI_50_PYTHON_AND_79_JAVA; G3_0/80`.
