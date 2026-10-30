# HazRob Tank V6 — Checkpoint 20 / N06-D: ترحيل SQLite الأصلي وحماية اعتماد V9 القديم

**التاريخ:** 2026-10-30  
**الفرع:** `feature/tank-v6-implementation`  
**المرحلة السابقة:** [Checkpoint 19 — Excel OOXML حقيقي وPDF اختباري](V6-CHEMCORE-NEW-ENGINE-CHECKPOINT-19-N06C-REAL-REPORT-OUTPUTS-2026-10-30_AR.md)

## 1. نتائج فعلية — استخدام SQL المصدر الأصلي نفسه

أنشئ اختبار `research/chemcore_v6/tests/test_v6_n06d_actual_v9_v10_sqlite_migration.py` في commit `daaba1147c0d4f2735288101338692d24517965a` ثم طُوِّر في `006c6ed115171b9b99127e90e77c9869b4738142`. **لا يعيد كتابة SQL الأصلية أو يخمّن تعريف الجداول.** يستخرج تعليمات `db.execSQL` حرفيًا من ثلاث طرق موجودة في Android `ExperimentsDb.java`:
- `createV9UserFieldTables` لبنية المواقع والجلسات والروابط التاريخية.
- `createV10FieldCycleTables` للجدول الجديد `field_measurement_cycles` وفهارسه.
- `importV9FieldCycleLinks` لنقل الروابط القديمة بقواعد `LEGACY_APPROVED` و`LEGACY_IMPORTED` الموثقة.

كما يستخرج جداول `measurement_sessions` و`telemetry_samples` من `createV6Tables` الفعلية. يضع البيانات الاختبارية في قاعدة SQLite حقيقية إصدارها `PRAGMA user_version=9` ثم ينفذ SQL الترحيل الأصلي وصولًا إلى `user_version=10` ويعيد تطبيق الاستيراد للتحقق من idempotency.

**النتيجة الناجحة:** [GitHub CI 40978064395](https://github.com/HazRob/HazRob-Tank/actions/runs/40978064395) **SUCCESS** في تشغيل مصدر SQL واختبار Python SQLite وتدقيق سجل الأدلة. تم التأكد من أن قاعدة V9 قبل الترحيل وبعده تضم **6 جلسات قياس و84 صف telemetry أصلي** لم تتغير محتوياتها، وبقاء مواقعها وروابط الجلسات وحالتها الأصلية. أنشأ V10 **6 دورات مستوردة**، منها **4 `LEGACY_APPROVED` و2 `LEGACY_IMPORTED`**، ولم يمنح أيًا منها `evidence_sha256` مصطنعًا. اجتاز `PRAGMA integrity_check=ok` و`PRAGMA foreign_key_check` وأعاد تشغيل الاستيراد دون تكرار.

**ملفات أصلية ثنائية قابلة للتحقق:** 
- `v9_before_approved_fixture.sqlite3` (**73,728 بايت**).
- `v10_after_migration_fixture.sqlite3` (**98,304 بايت**).
- `n06d_v9_v10_sqlite_source_migration_2026-10-30.json` (بصمات SHA لجميع جداول البيانات الأصلية، SQL المستخرج، وملفَي SQLite).

أُعيد فتح الملفين بعد تنزيل GitHub Artifact إلى بيئة التدقيق، وتأكدنا مباشرة من `user_version` قبل 9 وبعد 10، وثبات 84 عينة، ووجود حالتي الدورة الست دون SHA، وسلامة SQLite.

**سجل الأرشفة:** artifact `12943401616`، ZIP digest `sha256:111bc964d05c4e78cddec4549e6609c4ffb9e9321cf4a48b488de0ba7a447bfe`، محفوظ في مكتبة المستخدم `/HazRob/MQ_Reference_Measurements_2026/HazRob_Tank_V6_N06D_Actual_SQLite_V9_V10_Migration_2026-10-30.zip`، ID `libfile_09bdd257d9e48191b737007220c742af`.

## 2. اكتشاف علمي مهم في اعتماد البيانات القديمة

دالة `FieldCycleRepository.verifyApprovedEvidence(cycle)` في المصدر الحالي **تُرجِع true مباشرة** لدورة `LEGACY_APPROVED` بحكم سياسة V9 التاريخية، **وليس** بسبب تحقق بصمة SHA. ذلك يتيح استمرار التقارير القديمة، ولكنه لا يجوز أن يمنح تحليل ChemCore V6 ثقة بأدلة خارج سجلها الأصلي.

لهذا تم تعديل `android/app/src/main/java/com/servoc/app/report/V6FieldSessionResearchAppendix.java` في commit `bb77ab0c25dbca4d1368d547430bd4b80c39e08b` ليعمل كما يلي:
- تُحفظ دورة `LEGACY_APPROVED` في قاعدة البيانات وفي تقرير V5 التاريخي بنفس الحالة؛ **لا تُحذف ولا تُعدل ولا تُعتمد مجددًا**.
- لا يعاد تشغيلها في ملحق V6 البحثي **لعدم وجود `evidence_sha256` المعتمد**، وتُحسب ضمن `legacy_approved_gas_cycles_without_sha_skipped` مع سبب رفض الترقية.
- تُحلّل دورات V10 الحديثة فقط بعد التحقق من SHA الأصلي **قبل** تحليل V6 و**بعده**؛ ويظل الملحق للقراءة فقط، ولا يستخدم محرك V5 بديلًا عند فشل V6.
- المعيار الحاكم: `LEGACY_APPROVED` **لا يعني** `G3` أو أدلة MQ كيميائية مؤهّلة.

اختبار الانفصال في `research/chemcore_v6/tests/test_v6_approved_session_report_contract.py` commit `4a889b3a31dc472963dc96cf67c52984ae2cdd92`، ونجح تجميع Android إلى جانب اختبارات المصدر بعد التعديل (أحدث الاختبارات في [Actions Android V6](https://github.com/HazRob/HazRob-Tank/actions/workflows/chemcore-v6-android-compile.yml)).

## 3. PDF — إصلاح عائق بيئة المحاكي، دون ادعاء نجاح غير مثبت

عُرف أن [التشغيل الأول 40975702846](https://github.com/HazRob/HazRob-Tank/actions/runs/40975702846) فشل بسبب غياب مفتاح التوقيع الأولي عن CI، وعولج بمفتاح مؤقت خاص **بتطبيق الاختبار فقط**. [التشغيل التالي 40975895004](https://github.com/HazRob/HazRob-Tank/actions/runs/40975895004) بنى تطبيق الاختبار ووصل إلى تهيئة المحاكي لكنه فشل لأن `/dev/kvm` لم يكن متاحًا للمستخدم، فاستغرق الإقلاع نحو 16 دقيقة وانتهى الأمر بخطأ `adb input keyevent 82: Broken pipe` قبل تنفيذ تطبيق الاختبار. **ذلك ليس فشلًا مثبتًا في PDF ولا نجاحًا له.**

عُدّل workflow مؤقت المحاكي لتفعيل صلاحية `/dev/kvm` في Github runner إن كان متاحًا، commit `0d222c52705d87726e1e95751419daa3fe792268`. [محاولة المحاكي المعاد تشغيلها 40978108804](https://github.com/HazRob/HazRob-Tank/actions/runs/40978108804) لا تُعتبر ناجحة إلا عند `SUCCESS` وإثبات PDF وPNG لكل صفحة في Artifact. أي APK يُبنى هناك اختبار مؤقت بمفتاح لا يصلح لترقية المستخدم أو النشر، ولا يمثل إصدار V6.

## 4. حدّ الاستنتاج والمرحلة التالية

هذا **اختبار SQL مطابق للمصدر وبملفات SQLite حقيقية اصطناعية**، وليس تشغيل `SQLiteOpenHelper.onUpgrade` على بيانات هاتف المستخدم. لا يجري إدخال ملفات المستخدم الحقيقية أو تعديلها أو إصدار APK؛ ولا يختبر V6 على روبوت فعلي.

المرحلة القادمة: استخراج تقرير شامل لقاعدة بيانات **Android فعلية** أو عينة مؤرشفة من `servoc.db` مع الحماية من التغييرات، والتأكد من ملحق PDF بعد تشغيل Android emulator، وتجربة حاوية تصدير كامل على جهاز محاكي ثم اختبار عملي للأطراف والحساسات. لا تترتب ترقية G3 أو اعتماد ppm على هذه الاختبارات.

**الحالة العلمية:** 78/80 تقنية بتنفيذ بحثي جزئي، تقنيتان غير منقولتين، **Tank G3=0/80**، ولا دمج مع وحدة نضارة الأغذية، ولا تغيير للسيرفو أو بروتوكول التسخين المؤجل.

**Resume token:** `V6_CHECKPOINT20_N06D_ORIGINAL_V9_V10_SOURCE_SQL_PASS / SQLITE_V9_AND_V10_BYTES_ARCHIVED / 6_MEASUREMENT_SESSIONS_84_SOURCE_SAMPLES / LEGACY_4_APPROVED_2_IMPORTED / NO_FABRICATED_SHA / V6_SKIP_LEGACY_WITHOUT_HASH / ANDROID_COMPILE_PASS / PDF_EMULATOR_KVM_RETRY_40978108804 / G3_0_80 / NO_RELEASE_APK`.


## ملحق نتيجة PDF النهائية بعد الترحيل (30 أكتوبر 2026)

نتيجة التشغيل [40978108804](https://github.com/HazRob/HazRob-Tank/actions/runs/40978108804) كانت **FAILURE** لأسباب تنفيذ YAML script بصدفة `sh`. والتشغيل [40978449015](https://github.com/HazRob/HazRob-Tank/actions/runs/40978449015) كان **FAILURE** بسبب تنفيذ كل سطر على حدة من أداة المحاكي (فشلت حلقة `for`). عولج ذلك بملف `research/chemcore_v6/tests/android/run_v6_pdf_emulator_smoke.sh` يُشغَّل كسطر واحد داخل action. **التشغيل النهائي [40978812570](https://github.com/HazRob/HazRob-Tank/actions/runs/40978812570) SUCCESS:** إنتاج PDF حقيقي 3 صفحات على Android و3 صور PNG، والتحقق المستقل من SHA والرسم. [Checkpoint21 كامل](V6-CHEMCORE-NEW-ENGINE-CHECKPOINT-21-N06C-ANDROID-PDF-RENDERER-2026-10-30_AR.md). Archive Library `libfile_27a5671b735081919b189397e31b7b77`. ما يزال تقرير الجلسة الكاملة عبر SQLite الفعلية والتطبيق على هاتف المستخدم غير مختبر، وG3=0/80.
