# HazRob Tank V6 — Checkpoint 23 / N06-E: تشغيل تصدير جلسة V10 معتمدة من SQLite على محاكي Android

**التاريخ:** 2026-10-30 | **الفرع:** `feature/tank-v6-implementation`  
**المرجع السابق:** [Checkpoint 22 — تحسين PDF N06-C وSQLite N06-D](V6-CHEMCORE-NEW-ENGINE-CHECKPOINT-22-N06C-PDF-LEGIBILITY-2026-10-30_AR.md).

## قرار الأولويات

بناءً على طلب المستخدم، **أُجِّلت تجربة الانحراف بين ChemCore V6 ومنحنيات/بصمات المصنع**. لا تُستخدم نتيجة أي مقارنة أولية لمعايرة R0 أو اختيار الغاز أو ترقية G3. المسار المنفّذ بعد تأجيلها هو **N06-E** فقط، مع بقاء محرك نضارة الأغذية منفصلًا.

## ماذا تم اختباره فعلًا؟

اختبار Android جديد لا يستعمل نسخًا من مولد التقرير، بل يستدعي المسار الإنتاجي:

`ExperimentsDb (SQLite V10) → FieldCycleRepository.verifyApprovedEvidence → FieldSessionReportExporter.resolve → V6FieldSessionResearchAppendix.analyzeVerified → FieldSessionReportExporter.writePdf + writeXlsx → Android PdfRenderer + ZIP/OOXML validation`.

**شيفرة اختبارية مستقلة عن تطبيق الإنتاج:** `research/chemcore_v6/tests/android/V6ApprovedFieldE2EFixtureActivity.java` commit أول `3166f89e23896a2db6642e25395ddd582c42fb1e`، وسكربت المحاكي `research/chemcore_v6/tests/android/run_n06e_full_approved_field_export.sh` commit `6d36e5f6121781dcd381e03489596737768d2b58`، و[workflow](../../.github/workflows/chemcore-v6-n06e-approved-field-e2e-ci.yml) commit `59b03292d5d09fde42ce3d03b7a68cc87edab7e4`.

تُنشأ قاعدة **V10 اختبارية جديدة مؤقتة** على المحاكي (ولا تصل إطلاقًا إلى بيانات المستخدم): موقع وجلسة `APPROVED`، و16 صف قياس مؤرشف لـ**8 طرازات MQ**، ودورة `FAILED` وجلسة مستقلة `ACTIVE` للتحقق من الاستبعاد، و`R0` مفقود في بعض القراءات. لا تُستخدم أي غازات حقيقية أو حساسات Tank. تُحسب بصمة SHA الرسمية من **التطبيق الإنتاجي نفسه** عبر `FieldCycleRepository.evidenceDigest` بعد تهيئة جدول `ChemCoreDecisionRepository` الحقيقي، ثم تحفظ دورة اعتماد `APPROVED`. لا يتم خلق قرار كيميائي قديم أو تركيز معتمد لأغراض الاختبار.

يختبر هذا **تصدير تقرير جلسة معتمدة ذات SHA صحيح**؛ لا يثبت مسار القياس الفيزيائي 60 ثانية الفعلي أو تنفيذ UI لاعتمادها من المستخدم، ولا ترحيل قاعدة هاتف حقيقية.

## نجاح اختبار N06-E الأساسي

[GitHub Actions 40985046869](https://github.com/HazRob/HazRob-Tank/actions/runs/40985046869) — **SUCCESS**: شغّل تطبيق Android مؤقتًا عبر محاكي API33 AOSP ATD وأصدر:
- ملف PDF كاملًا من **6 صفحات**: 4 صفحات تقرير V5 التحليلي التاريخي، صفحة سجل الدورات، صفحة بحثية V6.
- ملف XLSX كاملًا من **14 ورقة**: 12 ورقة أصلية وورقتان مستقلتان لقياسات V6 والفرضيات/التعارضات.
- **67 تأكيدًا** داخل تطبيق Android، بما فيها التحقق من الدورة الفاشلة، رفض الجلسة غير المعتمدة، إعادة فتح صفحات PDF الست عبر `PdfRenderer`، ووجود ورقتَي V6 داخل XLSX.
- تحقّق SHA-256 من **صفوف الجداول الأصلية قبل وبعد التصدير، متطابقًا حرفيًا:** `a0d3941f18f86d7715e8a7342b7eafc59c9a2316235ee02949006fd9487eeb92`.
- تأكَّد أن `FieldCycleRepository.verifyApprovedEvidence` ما زال صالحًا بعد التصدير، ولم يتغير قرار V5 أو `Rs/R0` أو الموافقة. `UNKNOWN`، `reportable_ppm=false`، G3=0.

**الوثائق الرقمية الحقيقية من هذا الاختبار:**
- `n06e_full_approved_field.pdf` — **1,177,373 bytes**، SHA256 `2c63349c52f8c0a79fb95ca2499d039137765958093ca294aa69375175494d5f`.
- `n06e_full_approved_field.xlsx` — **22,559 bytes**، SHA256 `e1d50f954229a8a89d6e1fbe4e9744109079fd1a863509d08444c36bbcbc3230`.
- `n06e_status.json`، `n06e_verified_manifest.json`.
- Artifact ID `12946766119`، ZIP SHA256 `bcf1ca205cbaac41dad70511acc128e0a963ff176309df0c8242cb4a99c779bf`.
- مكتبة المراجع الشخصية: `/HazRob/MQ_Reference_Measurements_2026/HazRob_Tank_V6_N06E_Full_Approved_PDF_XLSX_2026-10-30.zip`، Library ID `libfile_9d0cd28ec3848191943d1240959ee16e`.

مراجعة مستقلة للملفات بعد التنزيل أكدت **PDF=6 صفحات**، وفتح ملف XLSX كـZIP مع **14 ورقة** وصلاحية CRC. رُسِمت الصفحات الست لصور وفُحصت الصفحة السادسة بصريًا؛ ظهر نص MQ وRs/R0 واضحًا مع غياب R0 في بعض القنوات. لوحظ أن ترويسة الصفحة السادسة كانت ترث العنوان التاريخي `HazRob 5.0 / HR069`؛ لذا أُجري إصلاح إضافي لقالب ملحق V6 الجديد **دون أي تعديل لصفحات V5 السابقة**.

## إصلاح بصري إضافي قبل اعتماد حزمة الإصدار الأخيرة

في `SmartFieldSessionPdfWriter.java` commit `57ac2299eac1ed5664821663ad394258412fcfa5`، وُضع `v6ResearchChrome` خاص بالملحق فقط يعرض `HazRob Tank • ChemCore V6`، مع بقاء `chrome` التاريخي كما هو على صفحات V5 القديمة. ونُقل عنوان الصفحة البحثية 12 نقطة لأسفل لتجنب التداخل مع الترويسة. [Android compilation](https://github.com/HazRob/HazRob-Tank/actions/runs/40985588474) **SUCCESS**، و[اختبار إعادة التصدير المتكامل 40985626965](https://github.com/HazRob/HazRob-Tank/actions/runs/40985626965) **SUCCESS**: تحقق من أن صفحة V6 تحتوي `HazRob Tank • ChemCore V6` ولا تحتوي `HazRob 5.0`، وأن الترويسة التاريخية لا تزال موجودة فقط ضمن صفحات V5. اجتاز الاختبار كامل شروط الـ67 تأكيدًا و6 صفحات PDF و14 ورقة XLSX، وأثبت أن SHA-256 لبيانات الاعتماد لم يتغير.

**الحزمة النهائية بعد إصلاح الترويسة (المعتمدة):**
- [CI 40985626965](https://github.com/HazRob/HazRob-Tank/actions/runs/40985626965) ناجح، Artifact `12946528755`، ZIP SHA256 `97397a9084401e60cb8c6eddbb610199a1517b7f9572548dd06872b37fe59a0e`.
- PDF 6 صفحات SHA256 `6047500e30c58dc5d237d20806e0b201eed82670e1156ccf527d57535103517a`.
- XLSX 14 ورقة SHA256 `defa58c8d9a5cf8f629ce72d39d3ee08d26eb2faf22f62436475154ff09cff0e`.
- نسخة Library `/HazRob/MQ_Reference_Measurements_2026/HazRob_Tank_V6_N06E_Full_Approved_PDF_XLSX_Final_2026-10-30.zip` معرف `libfile_4f3f443035208191a64790a2f4a52b31`.
- فتحتُ الحزمة بعد تنزيلها، وتحققتُ مستقلًا من عدد PDF وCRC لـXLSX، ووجود `HazRob Tank` دون `HazRob 5.0` في الصفحة السادسة، مع بقاء الترويسة التاريخية في الصفحة الأولى فقط.
- الحزمة الأولى قبل إصلاح الترويسة (`12946766119`) مؤرشفة للمقارنة **وليست الحزمة النهائية**.

## الأعطال التي عولجت أثناء اختبار N06-E

1. [CI 40984257819](https://github.com/HazRob/HazRob-Tank/actions/runs/40984257819) فشل في بصمة الاعتماد: جدول `chemcore_decisions` لم يكن قد أنشئ بعد، لأن الجدول يُهيَّأ كسولًا عبر `ChemCoreDecisionRepository`، وليس `ExperimentsDb.onCreate`. عولج **بتهيئة الجدول الأصلي**، لا بصنع SHA غير قابل للتحقق (commit `74c5c397935e0761221237d8231e3ac1f8b92e28`).
2. [CI 40984736439](https://github.com/HazRob/HazRob-Tank/actions/runs/40984736439) اجتاز مرحلة التصدير لكنه رفض فحص Pixel في الركن الأبيض للصفحة. عولج بتحديد وجود حبر مرئي داخل متن الصفحات بدل شرط خاطئ بأن الركن الأبيض يجب أن يكون ملونًا (commit `dd7a2443b29d3a8a090062252bac32f778882a6e`).
3. **أول تشغيل مكتمل ناجح هو 40985046869**؛ لا نحتسب التشغيلين الفاشلين كدليل صلاحية.

## الوضع العلمي والقيود

- **N06-E: نجاح مؤكد لتصدير PDF/XLSX فعليين من SQLite V10 اختبارية معتمدة ببصمة SHA** عبر `FieldSessionReportExporter` على محاكي Android.
- لم يُشغّل التطبيق على هاتف المستخدم ولم تدخل حساسات الروبوت في الاختبار. ولا يُعَد ما نجح معايرة غاز أو تحقق G3.
- لم يُبنَ APK **إنتاجي** أو موقّع بمفتاح المستخدم؛ APK المحاكي **اختباري ومؤقت** فقط.
- يبقى كل قرار V5 سابق محفوظًا، ويظل ملحق V6 `UNKNOWN` وبدون ppm معتمد. لا يستبدل ملحق V6 المحرك التاريخي في الصفحات القديمة.
- **لا تعديل لمحرك الأغذية أو تسخين MQ أو حركة الروبوت**.
- حالة 80 تقنية: **78 ذات تنفيذ بحثي جزئي، 2 غير منقولتين، G3=0/80**.

**Next:** N06-F اختبارات حالات رفض/تلاعب الجهاز بعد مصدر SQLite، تقارير موقع متعدد الجلسات والملفات، التوافق مع APK المستخدم وتوقيعه، ثم اختبارات الجهاز الفعلية ومعايرة G3 لاحقًا. لا إعادة لملف منحنيات المصنع قبل إعادة إعطاء الأولوية له.

**Resume token:** `V6_CHECKPOINT23_N06E_FULL_APPROVED_ANDROID_REPORT_PASS_40985046869 / 6_PDF_PAGES_14_XLSX_SHEETS / 67_ANDROID_ASSERTIONS / ACTUAL_APPROVAL_SHA_UNCHANGED / 16_SYNTHETIC_SAMPLE_ROWS_EIGHT_MQ / ORIGINAL_V5_PRESERVED / V6_UNKNOWN_NO_PPM / MANUFACTURER_CURVE_EXPERIMENT_DEFERRED / N06E_V6_HEADER_RERUN_40985626965_SUCCESS_FINAL / G3_0 / NO_PRODUCTION_APK`.
