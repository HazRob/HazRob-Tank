# HazRob Tank V6 — Checkpoint 22 / N06-C: تحسين قابلية قراءة PDF والتحقق من أربع صفحات Android

**التاريخ:** 30 أكتوبر 2026  
**الفرع:** `feature/tank-v6-implementation`  
**المرجع السابق:** [Checkpoint 21 — نجاح PdfRenderer الأول](V6-CHEMCORE-NEW-ENGINE-CHECKPOINT-21-N06C-ANDROID-PDF-RENDERER-2026-10-30_AR.md)، [Checkpoint 20 — ترحيل V9/V10](V6-CHEMCORE-NEW-ENGINE-CHECKPOINT-20-N06D-SQLITE-MIGRATION-2026-10-30_AR.md).

## 1. المشكلة المثبتة قبل الإصلاح

في الحزمة الأصلية لاختبار Android PDF الناجح [40978812570](https://github.com/HazRob/HazRob-Tank/actions/runs/40978812570) ظهرت **ثلاث دورات غاز في الصفحة** وخطوط MQ الفردية **6.9 pt**. بعد تنزيل artifact وفتح `v6_pdf_fixture.pdf` وPNG فعليًا، ثبت أن الحقول المختلطة `Rs` و`R0` كانت متلاصقة بصريًا وفي استخراج النص. كان محتوى PDF موجودًا وغير فارغ، لكن كثافته لم تحقق معيار القراءة المناسب.

## 2. التعديل الإنتاجي

عُدّل المسار الأصلي `android/app/src/main/java/com/servoc/app/report/SmartFieldSessionPdfWriter.java` في commit `709a533d07f6ba0058af819cacbe35bdd3d17185`:

- الانتقال من **ثلاث** دورات إلى **دورتين** من دورات القياس المعتمدة للغازات لكل صفحة ملحق PDF من A4 أفقي، دون حذف أي دورة أو تغيير صفحات V5 السابقة أو بصمات اعتمادها.
- فصل بيانات كل حساس في أعمدة ثابتة لإزالة الالتباس عند عرض العربية والرموز اللاتينية:
  `MQ x=43 pt`، `Scans x=116`، `Rs x=222`، `R0 x=347`، `State x=500`.
- رفع خط أسماء حساسات MQ وقيم Rs/R0 إلى **9 pt**، وتوضيح `SOURCE ONLY` مقابل `INSUFFICIENT SCANS`، مع `UNKNOWN` و`G3=0` و`PPM NOT REPORTABLE`.
- أبقيت البيانات المعروضة **من قراءة محفوظة فعلية** لكل حساس، لا قيمة مولدة أو متوسط مصنوع. يبقى عدم اعتماد الهوية والتركيز الفيزيائي جزءًا من التقرير.

عُدّل نشاط الاختبار المؤقت في `research/chemcore_v6/tests/android/V6ResearchPdfSmokeFixtureActivity.java` commit `50aeeabf66bf1d8c81448951fe2fa7e238ef5fdd` لتوقع **4 صفحات** من سبع دورات اختبارية (2+2+2+1)، وحدث سكربت جمع الأدلة `run_v6_pdf_emulator_smoke.sh` (commit `cd9a2f52bf5d6e175d480be605ee5e9b6cf4662b`) والـworkflow (commit `a71a84ae5efcbad317afe1792c539ade902cc531`). لم تُضف Activity الاختبار إلى كود التطبيق الإنتاجي؛ حقنها في مساحة CI المؤقتة فقط، وتوقيع APK الخاص بالمحاكي مؤقت وغير صالح لتحديث تطبيق المستخدم.

## 3. أدلة القبول الجديدة

**[تشغيل Android PDF النهائي 40981025159](https://github.com/HazRob/HazRob-Tank/actions/runs/40981025159) — SUCCESS.** شغّل مولد الصفحات الأصلي عبر `PdfDocument` ثم أعاد فتحه على Android بـ`PdfRenderer`، واستخرج PDF و4 PNG من المحاكي.

بعد تنزيل artifact تحقّقنا مستقلًا بواسطة ZIP/CRC و`pdfinfo`/PyMuPDF وفحص صور PNG:
- **4 صفحات A4 أفقي** بحجم 842×595 pt، حجم ملف PDF **514,531 بايت**.
- **56 صفًا لحساسات MQ** في سبع دورات اختبارية؛ 16 صفًا للصفحات الثلاث الأولى، وثمانية صفوف للرابعة.
- أصغر حجم خط لـMQ وRs/R0 **9 pt**، بدل 6.9 pt.
- مواقع الأعمدة ثابتة في الصفحات الأربع وفق `x=43/116/222/347/500 pt`، وكل مساحات النص ضمن حدود الصفحة.
- أعادت الملفات كافة حالة `UNKNOWN` و`G3=0`، ولم يتم إنتاج غاز مثبت أو ppm ميداني.

**SHA256 للملفات:**
- `v6_pdf_fixture.pdf` = `bf0bf4900b7b9d5f2c15a7ae310b63ef34c7a20ebbd2b2272536df97068c3cbd`.
- حزمة Artifact ZIP (ID `12944628278`) = `572e77a948149cad74e09f63ef05c95cd804f6ed2af9f0360fd7cd7d13202d77`.

**الحفظ المركزي:** `/HazRob/MQ_Reference_Measurements_2026/HazRob_Tank_V6_N06C_Android_PDF_Legible_4Pages_2026-10-30.zip`، Library ID `libfile_9ba0b7472a1481918cce7cd728f2936a`.

**تقرير النتائج الآلي المنظم:** `research/chemcore_v6/results/n06c_android_pdf_legible_4page_verified_2026-10-30.json` (commit `64833f4c485f2fa3559010435a96f6761c17ac0c`). ويوجد ملف اختبار تدقيق الخطوط `research/chemcore_v6/tests/test_v6_pdf_legibility.py` بمتطلب اختياري `PyMuPDF` عند تحليل ملف PDF فعلي؛ لا يُحمّل `fitz` في اختبارات المصدر الافتراضية. أُصلح تعارض الاستيراد في commit `0388b27abc8e241f6447ed4eda05ba038f19091c`، ونجح [source CI 40981353623](https://github.com/HazRob/HazRob-Tank/actions/runs/40981353623). ونجح كذلك [Android compile CI 40981051647](https://github.com/HazRob/HazRob-Tank/actions/runs/40981051647).

## 4. سلامة SQLite

من [Checkpoint 20](V6-CHEMCORE-NEW-ENGINE-CHECKPOINT-20-N06D-SQLITE-MIGRATION-2026-10-30_AR.md): اختبار SQL المستخرج من `ExperimentsDb.java` في [CI 40980388185](https://github.com/HazRob/HazRob-Tank/actions/runs/40980388185) **SUCCESS**. حافظ على ست جلسات قياس تاريخية و84 قراءة و4 دورات `LEGACY_APPROVED` ودورتين `LEGACY_IMPORTED` مع ترك بصماتها التاريخية المفقودة `NULL`، وبدون أي تكرار عند إعادة تشغيل الاستيراد. الملفات الثنائية V9 وV10 والـmanifest محفوظة في Library `libfile_18762e7609988191b9e008abdc322d04`، وقد فُتحت واختبرت في البيئة المحلية.

تمنع إضافة `V6FieldSessionResearchAppendix` الجديدة تحليل دورات `LEGACY_APPROVED` غير ذات SHA داخل محرك الغازات البحثي الجديد؛ تظل ملفات V5 التاريخية مستقلة.

## 5. ما لم يُنجز

- الاختبار أعلاه **ملحق بحثي من سبع دورات اختبارية مصطنعة**؛ ليس توليد تقرير جلسة ميدانية من `FieldSessionReportExporter.resolve` ثم عرض جميع صفحات V5 وملحق V6 على هاتف المستخدم.
- لم تُختبر قاعدة بيانات المستخدم الواقعية أو عملية ترحيل `SQLiteOpenHelper.onUpgrade` على هاتف Android. اختبار SQLite نفّذ المصدر SQL نفسه تحت Python.
- لم تُصدر نسخة Android V6 إنتاجية قابلة للترقية ولا APK موقّع بمفتاح المستخدم. ملفات APK المستخدمة بالمحاكي مؤقتة ولا يجوز توزيعها كتحديث.
- لا نتائج مختبرية جديدة لغازات مرجعية على عتاد Tank نفسه، ولا معايرة R0 جديدة أو حكم سلامة / ppm.
- محرك نضارة الأغذية مستقل؛ لا تغيير لبرنامج تسخين MQ أو حركة الروبوت.

**حالة التقنيات:** 78/80 ذات تنفيذ بحثي جزئي، وتقنيتان غير منقولتين، **G3 = 0/80**.

## 6. الخطوة المنطقية التالية N06-E

اختبار متكامل `FieldSessionReportExporter.resolve` مع قاعدة **V10 معتمدة وذات SHA أصلي صحيح** على Android/محاكي، وإصدار تقرير PDF وXLSX كاملين، مع مقارنة بصمات الأدلة قبل وبعد؛ وتمييز حالات `LEGACY_APPROVED` و`APPROVED` و`FAILED` و`SUPERSEDED` وعدم إحداث أي تغيير في المصدر أو الرجوع إلى محرك V5 داخل ناتج V6.

**Resume token:** `V6_CHECKPOINT22_N06C_PDF_4PAGE_LEGIBLE / CI40981025159_SUCCESS / 56_MQ_ROWS_7_FIXTURE_CYCLES / 9PT_RS_R0_FIXED_COLUMNS / G3_0 / N06D_SQLITE_SOURCE_MIGRATION_SUCCESS_40980388185 / V9_NO_SHA_RESEARCH_REPLAY_EXCLUDED / NO_FULL_FIELD_SESSION_EXPORT / NO_RELEASE_APK`.
