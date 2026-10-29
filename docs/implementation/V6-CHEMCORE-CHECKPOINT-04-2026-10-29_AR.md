# HazRob Tank ChemCore V6 — checkpoint 04: الاستفادة الشاملة وربط المنحنيات بالقياس الآني

**التاريخ:** 2026-10-29 · **المستودع:** `HazRob/HazRob-Tank` · **الفرع:** `feature/tank-v6-implementation`.  
**آخر تحقق ناجح:** [Scientific run 40924315634](https://github.com/HazRob/HazRob-Tank/actions/runs/40924315634) · [Android compile 40924139634](https://github.com/HazRob/HazRob-Tank/actions/runs/40924139634).  
**القرار العلمي الحاكم:** `docs/research/V6-002-INCLUSIVE-ANALYTICS-POLICY-2026-10-29_AR.md`.

## 1. التنفيذ المتحقق

- **P06 — سجل المنحنيات:** استخدمنا `research/chemcore_v6/source_snapshots/hr069/mq_research_curves_43.json` بالـGit blob `bd597ffe9ecaee6a0ccfddc5ec1b89c1cccc0ba1`. نُقلت جميع المعاملات الـ43 إلى `SourceCurveRegistryV6.java`، commit `e5e576b93b9bbde6c24bdbe6dc31d619c2c7db52`، مع نسخة manifest بحثية `source_curve_registry_43.json`، commit `4ba4e691cae421304299c498e605aa2e2203eed3`.
- **P06 — السياسة:** كل علاقة تُحل رياضيًا تُعرض، حتى لو كان تعريف R0 في المصدر غير محقق أو مجال تركيزها محدودًا؛ المنحنيات الستة لـMQ3 بمعامل `output_scale=50` محفوظة بالكامل، مع التنبيه عند الاستقراء. لا تُشترط الهوية الجزيئية أو heater profile لعرض الحساب البحثي.
- **P08 — دمج الأدلة:** `TankV6EvidenceConsensus.java` (commit `7b6f7355174b391ba079240da3d794a46f22fcae`) لا يحجب المرشحين ذوي الحساس الواحد أو النتيجة المنخفضة أو التعارض أو نقص اسم المصدر؛ الاحتفاظ بكامل الترتيب والملاحظات، ولا تضخيم مساهمة الحساس الفيزيائي المكرر.
- **P03/P09 — ربط بحثي آني:** `TankV6ResearchCurveOverlay.java` (commit `0db9470329466490653cd1c1ab13f2fbbf8da038`) يأخذ نسب `Rs/R0` من الحساسات المتاحة ويُقيّم كل منحنى ذي طراز مطابق، دون فرض عتبات لتراكيز البحث. أُضيف استدعاؤه إلى `TankInstantAnalyzer.java` commit `79132d0eb7bc6df61ba0c4218157ce3a501a915a`، ويخزن نتائج مستقلة تحت المفتاح `v6_all_reference_curves_research` داخل JSON التحليل.
- **محتوى النتيجة الجديدة:** `source_curve_id, source_root, sensor_model, gas_or_class_label, legacy_chemical_id, numeric_fit_available, source_domain_extrapolated, observed_rs_over_r0, source_fit_ppm_research, advisories`. لم تتغير قائمة `candidates` القديمة ولا محرك V5 ولا إنذارات السلامة أو التحكم أو المعايرة.
- **البيانات:** كل العلاقات الـ43 مصدرها البحثي واحد `MQDATASCIENCE_6_0_0`، لا تعني 43 تجربة/مرجعًا مستقلًا. وPPM هو نتيجة حسابية من منحنى المصدر، **ليس تركيزًا تم التحقق منه بمقياس غاز مستقل**.

## 2. نتائج الاختبارات المنفذة

| مجال الاختبار | النتيجة |
|---|---:|
| GitHub CI Scientific `40924315634` | **success** |
| Python unit tests (بما فيها مقارنة Java بـ43 مصدرًا حقيقيًا) | **58/58 PASS** |
| R0 Java checks | 31 |
| Evidence consensus Java checks | 40 |
| Research curve math Java checks | 15 |
| All-43 source registry Java checks | 328 |
| One-shot 43-curve overlay Java checks | 184 |
| **الإجمالي Java assertions** | **598 PASS** |
| PPM forward-inverse original 43×9 | 387 حالة |
| R0 synthetic perturbations | 172 حالة، 94 خارج المجال |
| Java/Android merged application compile | **success**، run `40924139634` |
| المرجع الفيزيائي Tank G3 | **0/80** |

**CI source:** `c85a33fad4aafbcf6187a9fc6fecb2f0960bc674`. **Source artifact ID:** `12919122845`.  
**Android compile:** `5edb3d4c6e9bea46935bc128377e884855761740`، تحقق `gradle --no-daemon :app:compileDebugJavaWithJavac` ضد `sohpany/SERVOC-Android@6281d3eb7c1c961ce63b0b5e4ae0792874626720`. **لا APK** ولا اختبار على هاتف أو ESP32 ضمن هذا التشغيل.

## 3. الحدود غير المغلقة/المطلوب الاستمرار فيه

- **P03/P09:** تم تجميع الطبقة وربطها بمدخل القياس الآني، لكن تبويب UI والتقارير PDF/Excel ومسار جلسات الموقع لا يعرضون بعد كل صفوف هذه الطبقة بصورة مخصصة، ولم تُجرَ اختبارات فعلية على هاتف Android 12.
- **P05:** لم تُعاد تجارب UCI270/308/309 الخام؛ موجودة فقط سجلات G2 المثبتة ومقارنة UCI1081 الحقيقية السابقة.
- **P06:** 43 منحنى + 49 Stable + منحنيات المصنع والبصمات الأخرى يجب استكمال جمعها أو تشغيلها والمقارنة بينها؛ تأكد من تفادي تضخيم الأصول المتكررة **من دون حجب أي مصدر حسابي**.
- **P08:** لم تُجرَ اختبارات G2 جديدة بمقارنة الترتيب الشامل مقابل المحرك القديم على مجموعات مستقلة؛ التقديرات الحالية بحثية.
- **P10:** لم تُجرِ تجربة غاز مرجعي على Tank؛ `G3=0/80`.
- **V6-003:** تحليل إحماء MQ وبوش مؤجل بقرار المستخدم. لا تغيير ESP32.
- **P11:** لا APK V6؛ نجاح compiler لا يعني نجاح الواجهات والجلسات والتقارير أو التوافق مع شهادة التوقيع.

## 4. نقطة الاستئناف الدقيقة

1. اقرأ هذا التقرير ثم `docs/implementation/V6-CHEMCORE-EXECUTION-TRACKER.json`، ثم سياسة البحث المفتوح. تحقق من آخر GitHub workflow الناجح.
2. عند إعادة الاختبارات: `python -m unittest discover -s research/chemcore_v6/tests -v`، ثم `SourceCurveRegistryV6Test` و`TankV6ResearchCurveOverlayTest` ضمن `chemcore-v6-scientific-ci.yml`. تحقق من `chemcore-v6-android-compile.yml`.
3. **الخطوة التالية:** ربط النتيجة الجديدة بعرض مستقل في تبويب المختبر والقياسات الآنية، ثم إجراءات الجلسات وتقاريرها. تنفيذ مقارنة حقيقية ضد baseline V5 دون تعديل المخرجات السابقة الصادرة والموافَق عليها.
4. لا تستحدث تقنية الإحماء مؤقتًا ولا تغيّر firmware. كل نتيجة غاز بحثية متاحة للحساب؛ لا تحذف لغياب R0 المصدر، فقط سمِّ حقيقتها.

**State:** `P06_ALL_43_AVAILABLE / P08_INCLUSIVE_RANKING / P03_P09_INSTANT_ONE_SHOT_JSON_WIRED_AND_ANDROID_COMPILE_PASS / PHYSICAL_G3=0/80 / APK_NOT_BUILT`.
