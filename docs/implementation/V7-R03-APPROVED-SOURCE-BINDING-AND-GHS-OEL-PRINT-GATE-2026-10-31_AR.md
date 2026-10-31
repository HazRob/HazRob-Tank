# HazRob Tank V7 — R03 | تقدم الربط الأرشيفي وطبقة GHS/OEL وتحقق طباعة Android
**Document ID:** `V7-R03-SOURCE-BINDING-GATE-001`  
**Date:** 2026-10-31  
**Branch:** `feature/tank-v7-reports`  
**State:** `NATIVE_BLANK_REPORT_11_PAGE_QA_PASS / QUALIFIED_REFERENCE_BRIDGE_SOURCE_ADDED / NEW_SOURCE_REFERENCE_CI_PENDING`.  
**Approval boundary:** approved R03 specification, design first, missing values `-`, no invented or inferred plots or chemical/safety evidence.

## 1. نتائج الاختبار الفعلية (مكتملة)
- [GitHub Actions 41147792261](https://github.com/HazRob/HazRob-Tank/actions/runs/41147792261): **PASS** للهندسة البصرية الأصلية في Android: 3 صفحات A4 أفقية، شبكة `2×2`، منطقة `3D/4D` مع مساحتي تعويض، وعمود مرشح بمخطط PPM مصغر فارغ.
- [GitHub Actions 41148763993](https://github.com/HazRob/HazRob-Tank/actions/runs/41148763993): **PASS** لمولّد `V7FieldSessionPdfWriter.writeBlankDesignFixture` الحقيقي باستخدام Android `PdfDocument`، استخرج 11 صفحة A4 أفقية من محاكي Android، **جميع الخطوط ≥12pt**، وجميع أقسام الملخص والمرشحين و5 معارض المخططات والفيزيائي والجسيمات والتدقيق والأدلة موجودة دون قياسات خيالية. ملف PDF الأصلي `V7_R03_Full_Blank_Android_Report.pdf`، Artifact `13021339689`. هذه نتيجة **معاينة فارغة** وليست نتائج جلسة فعلية.
- **فحص بصري مستقل:** صور الصفحات 1–11، بدون فقد صفحات. كشف التواء/التفاف كلمات المحور الرأسي لضيق المساحة، فاستُبدل بمحور نصي مدوّر `-90°` عند `12pt` في `V7ReportLayoutKit.blankChart`. اجتاز [GitHub Actions 41148972994](https://github.com/HazRob/HazRob-Tank/actions/runs/41148972994) بعد هذا التصحيح.
- نسخة القالب بعد إضافة قسم مراجع OEL مؤهل قد تُنتج عددًا أكبر من 11 صفحة؛ **لا يدّعى نجاح نسختها الأحدث قبل CI النهائي**.

## 2. مسار المصدر الأرشيفي الجديد
1. `V7FieldReportSourceSnapshot.resolve(db,fieldId)` يستدعي `FieldSessionReportExporter.resolve`، الذي يتحقق أن الجلسة **معتمدة**، ويتحقق من ارتباط الجلسات المؤرشفة ودورات `G/F/P` وبصمات الدليل.
2. يُحفظ كل سجل دورة مع `sequence/attempt/state/measurementId/started/approved/full SHA-256`، ولا تعتبر المحاولات `FAILED/RETIRED` قياسات معتمدة.
3. **تم توسيع قائمة المرشحين لتشمل جميع جلسات القياس المعتمدة**، لا مرشحي جلسة واحدة فقط. ترتبط كل خانة بترقيم جلسة المصدر `S1/S2...` وترتيب مرشحها داخل جلسة المصدر، بلا دمج إحصائي بين مراتب غير متكافئة.
4. مصدر هوية المركب `chemical_uid` الموثوق به **منفصل** عن `id` الخاص بسجل المرشح؛ لا يجوز إظهار GHS لمرشح بسبب مساواة معرف داخلي غير مؤهل بمعرف مركب معروف.
5. المخططات الكيميائية تبقى **محاور وشبكات وعناوين فقط** أثناء مرحلة الربط دون بيانات سلسلة فعلية من المحرك. لا يُفسر الفراغ بأنه الهواء آمن أو المادة غير موجودة.

## 3. GHS المؤهلة وطبقة حدود التعرض — تصنيف مرجع لا إنذار
- `V7FieldSourceReferences.from(db,source)` يقرأ `GhsPictograms.allQualified(db)` فقط، وهي تتطلب `evidence_status=SOURCE_QUALIFIED` ومصدرًا علميًا مرتبطًا وكود `GHS01..GHS09` صالحًا.
- تربط GHS فقط بمعرف `chemical_uid` **الصريح** من المرشح، لا اعتمادًا على الاسم الظني أو احتمال الترشيح أو تشابه حروفه.
- `ExposureRepository.forChemical` أو `byCas` تعرض جميع مراجع `OSHA/NIOSH/ACGIH/...` المتاحة **دون دمج**، مع `authority/type/value/unit/duration_min/source_id/revision/notation` عند توفرها. الدلالة: هذا **مرجع مادة علمي/مهني** لا دليل على وجود المادة في عينة الروبوت.
- لا يقارن `PdfWriter` القيمة المرجعية بقراءة `PPM` لحظية، ولا يستنتج تجاوز TWA/STEL، ولا يصدر تحذير سلامة من تصنيف المادة وحده. **تبقى أشرطة PPM-vs-Limit فارغة إلى أن يتوفر تقييم محرك معتمد ومتكافئ زمنيًا**.
- يمكن طباعة ما يصل إلى **9 رموز GHS صحيحة** داخل عمود GHS 35mm بنظام `3×3` ضمن صف المرشح، دون حذف رموز علمية لفرض صفحة ثابتة. رموز غير متاحة = `-`.
- صفحات **Exposure Reference Annex** تتمدد وفق `max(1,ceil(N/3))`، وتعرض كل مصدر على حدة بخط ≥12pt. لا يتم تحويل عدم وجود مرجع إلى `0` أو قرار `SAFE`.

## 4. ملفات التعديل
- `android/app/src/main/java/com/servoc/app/report/V7FieldSessionPdfWriter.java` — عرض مرشحين وحزم مخططات، صفحات مرنة، قسم مراجع GHS/OEL، سجل كامل للدورات.
- `android/app/src/main/java/com/servoc/app/report/V7FieldReportSourceSnapshot.java` — عقد مصدر جميع الجلسات/المرشحين، و`qualifiedChemicalUid`.
- `android/app/src/main/java/com/servoc/app/report/V7FieldSourceReferences.java` — اتصال مراجع GHS/OEL بالمصادر المؤهلة.
- `android/app/src/main/java/com/servoc/app/report/V7ReportLayoutKit.java` — إصلاح المحور الرأسي المدور، ووحدات الصفحة والخط.
- `research/chemcore_v6/tests/android/V7R01PdfFixtureActivity.java` و`.github/workflows/v7-r01-android-pdf-qa.yml` — اختبار Android الفعلي، PNG/manifest/12pt لكل صفحة.

## 5. ما لم يكتمل ولا يجب الادعاء به
- **إخراج R03 الإنتاجي لم يُستبدل بعد**: `SmartFieldSessionPdfWriter.java` و`FieldSessionReportExporter.writePdf` ما زالا على المصدر الأصلي، اتقاءً لفقد المنحنيات الحقيقية أو نتائج جلسات في النسخة الانتقالية.
- لم تُربط بعد **السلاسل الزمنية الفعلية MQ/R0/Rs، التعويض العلمي، قرارات ChemCore، 3D/4D الحقيقية، والقياسات الفيزيائية/PM المؤرشفة** بعقد رسم المصدر مع حفظ شروط القابلية العلمية.
- لا توجد تجربة G3 مؤهلة للروبوت، ولا استنتاج هوية أو PPM مهني معتمد. مراجع GHS/OEL مطبوعة مستقلة من مصادرها فقط.
- لم تُجرَ بعد تجربة ربط مكتملة لجلسة ميدانية معتمدة تحتوي مرشحين عديدين وعدة دورات وصور GHS ومراجع OEL، لذلك **سلامة الربط الفعلي مفتوحة** ولو نجح اختبار القالب الفارغ.
- لم يُصدر APK V7، ولم يكتمل تكامل R04 النهائي وملفات صور R05.

## 6. بوابة استكمال العمل
1. تحقق من CI للـ`V7FieldSourceReferences` والعارض بعد إضافته، وأصلح أي compile أو خطأ أعمدة/صور.
2. اختبر PDF فارغًا مع 12pt وبعد نقل المحور الرأسي؛ راجع حدود النصوص والرسوم وملحق المرجع الجديد.
3. اختبر `all candidates` مع جلستين معتمدتين بأدلة مصدر مؤرشفة حقيقية، لا بتجميع أسماء افتراضية.
4. اربط الرسوم البيانية فقط عند وجود **معطيات مصدر قابلة للرسم** وبالفضاء/الوحدة/الوقت نفسه؛ لا ترسم تغيرًا زمنيًا من قيمة لحظية واحدة.
5. عندما يطابق التقرير الإنتاجي جميع أقسام R03 دون فقد البيانات، استبدل استدعاء `FieldSessionReportExporter.writePdf` بمولّد V7، ثم اختبار Android كامل وإذن المالك للاعتماد البصري.
