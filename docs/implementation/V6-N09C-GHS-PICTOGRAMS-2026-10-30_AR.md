# HazRob Tank V6 / N09C — رموز GHS للمكتبة الموحدة
**التاريخ:** 2026-10-30  
**المستودع:** HazRob/HazRob-Tank — فرع feature/tank-v6-implementation  
**المخطط:** SQLite V13، العرض الموحد `v11_unified_chemical_catalog` (734 مادة فريدة)

## آلية العمل
- قائمة المواد الـ734 مرجعية موحدة؛ جميع الصفوف تشير إلى `chemicals.uid`، ولا ينشئ أي رمز GHS هوية جديدة.
- تحتفظ `chemical_hazard_classifications.ghs_pictogram` بترميز GHS الموثق، مع `source_id` و`classification_system` و`evidence_status`.
- مصدر الصور هو VectorDrawable محلي مستقل `android/app/src/main/res/drawable/ghs_01.xml` إلى `ghs_09.xml`، ومنسوخ مع حزمة Android. لا توجد اتصالات الشبكة عند العرض أو توليد PDF.
- `GhsPictograms.allQualified(db)` ينفذ مسحًا واحدًا لرموز المصادر الموثقة لكل المواد بدل 734 استعلامًا داخل قائمة البحث، ويزيل التكرارات.
- تبويب البحث/التتبع: صور في صفوف القائمة وفي مربع تفاصيل المادة؛ تبويب المرشحين ChemCore V6: رموز بجانب الفرضية البحثية مع التنويه بأن الهوية غير مثبتة.
- `TankInstantAnalyzer` يحتفظ بحقول `ghs_pictograms` و`ghs_pictogram_scope=CLASSIFICATION_IF_IDENTITY_CONFIRMED` و`ghs_is_detection_evidence=false` بالمرشح الموحد، دون إضافة أي دليل على الاستشعار أو تغيير الترتيب.
- PDF القياسات الآنية للغازات V6: ملحق ثانٍ **مشروط** يُدرج فقط إن كانت هناك رموز GHS مرتبطة بمرشحين موحدين من مصادر معتمدة، ويعرض رمز الخطر واسم المادة مع تحذير صريح بأن الغاز لم يثبت رصده. ملف PDF يُنتج من صور SVG محوّلة إلى VectorDrawable داخل التطبيق.
- إنذارات الكشف/الخطر: `GhsPictograms.forQualifiedAlarm` يشترط **هوية غاز محققة، وتركيزًا محققًا، وصلاحية إنذار مستقلة**؛ وبما أن G3=0، فلا تصبح الأيقونة وحدها إنذارًا بالغاز.

## الرموز
| الترميز | الرمز | الدلالة |
|---|---|---|
| GHS01 | Exploding bomb | متفجر |
| GHS02 | Flame | قابل للاشتعال |
| GHS03 | Flame over circle | مؤكسد |
| GHS04 | Gas cylinder | غاز تحت ضغط |
| GHS05 | Corrosion | أكال |
| GHS06 | Skull and crossbones | سمية حادة شديدة |
| GHS07 | Exclamation mark | ضار / مهيج |
| GHS08 | Health hazard | خطر صحي جسيم (بما في ذلك السرطنة) |
| GHS09 | Environment | خطر على البيئة المائية — ليس إلزامياً بموجب OSHA HCS |

## حدود التصنيف
- لا توجد أيقونة GHS إشعاعية؛ النشاط الإشعاعي للرادون يحتاج فئة مستقلة وجهاز قياس مناسب. الخانق البسيط لا يملك أيقونة GHS مستقلة أيضًا.
- غياب رمز لا يعني مادة آمنة: يظل مصدر التصنيف مجهولًا حتى اكتمال التوثيق.
- وضع 90% من حد التعرض أو 10% LFL لا ينشئ تلقائيًا تصنيف GHS؛ درجة خطورة المادة تأتي من SDS/تصنيفات مصدرية وتظل مستقلة عن قياس روبوت Tank.
- الأيقونات في القوائم تصف مخاطر المادة إن كانت موجودة، ولا تثبت أن الروبوت رصدها في الهواء.

## المصادر والرخصة
- [UNECE — GHS pictograms](https://unece.org/transport/dangerous-goods/ghs-pictograms)
- [OSHA — Hazard Communication Pictograms](https://www.osha.gov/hazcom/pictograms)
- [OSHA 3491 Pictogram Quick Card](https://www.osha.gov/sites/default/files/publications/OSHA3491QUICKCARDPICTOGRAM.pdf)
- ملفات SVG المرجعية: [karlnorling/ghs-hazard-pictograms](https://github.com/karlnorling/ghs-hazard-pictograms)؛ نسخة MIT attribution محفوظة في `docs/licenses/GHS-VECTOR-SOURCE-LICENSE.md`.

## التحقق
- تجميع التطبيق مع الموارد المتجهية: GitHub Actions `chemcore-v6-android-compile.yml`.
- الاختبار الميداني G3 لم يتغير: 0/80.
- اختبار `research/chemcore_v6/tests/android/V6ScientificRiskDbFixtureActivity.java` على محاكي API31 يفحص: 
  1. GHS01…GHS09: كل رمز موجود ومرسوم بالمعين الأحمر والرسمة السوداء،
  2. مصدريًا: البنزين GHS02/GHS08 والأكسجين GHS03 دون GHS02،
  3. ظهور صورتين على الأقل في ملحق PDF الصفحة الثانية عبر PdfRenderer،
  4. الحجب عن تنبيه غاز غير مؤهل G3،
  5. سلامة عدد المواد 734 والمخطط V13.
- تقرير الإخراج: `n09_ghs_report.pdf` مرفق بأثر CI عند نجاح الاختبار.

## نتيجة تنفيذ موثقة بتاريخ 2026-10-30
- **PASS**: [Android API31 / GHS vector + PDF actual raster integration](https://github.com/HazRob/HazRob-Tank/actions/runs/40996429712).
- **78 assertion checks** passed; bundled vector resources = **9/9**, PDF page count = **2**, GHS actual rasterized red pixels = **656**, one-to-one chemical view = **734/734**.
- Android source build successful. No physical robot G3 certification, no proven gas detector alarm.
