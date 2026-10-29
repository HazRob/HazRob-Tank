# قرار علمي وتنفيذي: سياسة الاستفادة الشاملة من البيانات في ChemCore Tank V6

**التاريخ:** 2026-10-29 · **الفرع:** `feature/tank-v6-implementation` · **النطاق:** `V6-002 / P06 / P08` · **القرار:** `INCLUSIVE_RESEARCH_ANALYTICS`.

## 1. التوجيه المعتمد

بناءً على توجيه المستخدم بتاريخ 29 أكتوبر 2026: **لا استبعاد صارم من الحسابات البحثية إلا للمدخلات الرقمية الفاسدة أو العمليات غير المعرفة رياضيًا**. يُفضّل إتاحة كل منحنى أو بصمة أو نموذج أو فرضية ومقارنة النتائج على حجبها لنقص R0 أو هوية السخان أو التوثيق، وإظهار ملاحظات المصدر مع النتيجة.

لا يعني ذلك تحويل الاستنتاج البحثي إلى قياس معياري أو شهادة سلامة. التمييز بين معنى الرقم ومستوى الإثبات **وصف للمخرجات وليس مرشحًا يحذف البيانات**.

## 2. طريقة التطبيق

- **جميع المنحنيات الـ43:** مفعلة في `SourceCurveRegistryV6` كفرضيات رقمية بحثية؛ المتغيرات `source_r0_definition_verified=false`، `source_heater_profile_verified=false` و`output_scale_verified=false` تؤدي إلى `risk_flags` فقط، لا `HOLD`.
- **كل المجالات:** الملاءمة خارج مجال التركيز المنشور تظل ممكنة عندما تعطي معادلة المصدر قيمة منتهية وموجبة؛ تُوسم `EXTRAPOLATED_BEYOND_SOURCE_RANGE`، لا تُحذف.
- **الغازات والفئات:** تبقى تسميات `LPG`, `smoke`, `alcohol`, `NH4/NH3` متاحة للتحليل والترتيب مع توضيح أنها ليست إثبات هوية جزيئية مفردة.
- **مركبات ومصادر متعارضة:** تحتفظ النتيجة بكل المرشحين وتعرض `conflictingSourceModels` كخاصية تفسيرية. الحساس الفيزيائي لا يصبح حساسين لمجرد وجود مصدرين؛ يمنع احتساب دليل مكرر مرتين في المتوسط دون حجب أي منحنى من سجل التحليل.
- **حساس واحد أو مساهمة ضعيفة:** لا يُخفى المرشح بسبب قلة الحساسات أو انخفاض درجة التطابق أو تقارب الدرجات؛ تُعرض نتائج التصنيف البحثي كلها.
- **نقص Source ID:** تستخدم مجموعة `source_unattributed` للحفاظ على الدليل دون اختلاق استقلالية مصادر مفقودة.
- **الاستبعاد الحسابي الوحيد:** `NaN`, `Infinity`, `Rs<=0`, `R0<=0`, معاملات `a<=0` أو `outputScale<=0` أو ميل صفري في معادلة الأس، ومعادلة يفيض ناتجها أو تصبح غير معرفة. يظل الصف الأصلي قابلاً للتوثيق، لكن لا يُقدّم رقم PPM مختلق.
- **التقدير النسبي:** `ppmResearch` قراءة حسابية مستنتجة من نموذج مصدر، وليس `reportable calibrated ppm` ما لم يوجد دليل قياسي مستقل. هذا بيان عن حقيقة القياس، وليس حرمانًا للمستخدم من الحساب.

## 3. المكونات المنفذة

1. `research/chemcore_v6/manifests/source_curve_registry_43.json`: النسخة 2، كل 43 سجل `USABLE_RESEARCH_WITH_CAVEATS`.
2. `research/chemcore_v6/curve_registry_audit.py`: يقارن معاملات كل سجل بالأصل Git blob `bd597ffe9ecaee6a0ccfddc5ec1b89c1cccc0ba1` ويكشف تحويرات البيانات غير الصالحة.
3. `android/app/src/main/java/com/servoc/app/chemcore/SourceCurveRegistryV6.java`: سجل ثابت لجميع المعاملات والغازات، يوفر `all`, `forSensor`, `explore`, `exploreSensor` مع تنبيهات غير مانعة.
4. `android/app/src/main/java/com/servoc/app/chemcore/TankV6EvidenceConsensus.java`: إصدار schema/2، لا تفرض `MIN_MATCH/MIN_MARGIN/MIN_SENSOR_BREADTH` رفضًا للمرشحين. تعرض التعارض، ومجهولية المصدر، والخروج عن المجال كأوصاف.
5. `research/chemcore_v6/tests/test_curve_registry_audit.py`, `research/chemcore_v6/tests/java/SourceCurveRegistryV6Test.java`, `research/chemcore_v6/tests/java/TankV6EvidenceConsensusTest.java`: اختبارات السياسة والحساب.

**لم يتغير مسار V5 التشغيلي** أو فيرموير ESP32، ولم يُبنَ APK V6 في هذه الدفعة. ما يزال ربط سجل المنحنيات بوحدة الإدخال ومخرجات التقارير ضمن P03/P09.

## 4. نتائج الاختبار الفعلية

**GitHub Actions:** [40923146562](https://github.com/HazRob/HazRob-Tank/actions/runs/40923146562)، head commit `1417beecfbcd9ec9a08b2b884f06ab22791d34f4`، `success`.

| الاختبارات | النتيجة |
|---|---:|
| اختبارات Python | **57/57 PASS** |
| عقود R0 في Java | **31 assertion PASS** |
| دمج الأدلة في Java بعد إزالة العتبات المانعة | **40 assertion PASS** |
| محرك حساب المنحنيات في Java | **15 assertion PASS** |
| سجل المنحنيات الـ43 في Java | **328 assertion PASS** |
| مجموع تحققات Java | **414 PASS** |
| المنحنيات المفتوحة للبحث | **43/43** |
| المنحنيات المؤهلة ماديًا لجهاز Tank | **0** |
| اختبار Tank G3 على الجهاز | **0/80** |

**CI artifact:** `chemcore-v6-source-research-audit`, ID `12919626294`. يتضمن `curve_registry_inclusive.json` ونتائج المحاكاة التاريخية/العددية، وليس قياسات جديدة من الجهاز.

### متابعة التنفيذ

استكمال المصدر والتوقيت في P03، وتجارب مقارنة فعليّة إضافية في P05، وربط `SourceCurveRegistryV6` و`TankV6EvidenceConsensus` بمسار تحليل Android **بوضع بحثي** أولًا، مع مقارنة نتائج V5 وتسجيل سبب كل اختلاف. تظل تقنية الإحماء الجديدة V6-003 **مؤجلة** بقرار المستخدم.

**للاستئناف بعد الانقطاع:** اقرأ هذا الملف وسجل التغييرات و`docs/implementation/V6-CHEMCORE-EXECUTION-TRACKER.json`، ثم ابدأ من الالتزام الناجح للاختبارات المبينة أعلاه ولا تعد تنفيذ السجلات التاريخية دون حاجة.
