# HazRob Tank — Checkpoint 19 / ChemCore V6 N06-C: XLSX OOXML actual output + Android PDF emulator QA

**التاريخ:** 2026-10-30  
**الفرع:** `feature/tank-v6-implementation`  
**مرجع الاستئناف السابق:** [Checkpoint18 — approved gas PDF/XLSX source appendices](V6-CHEMCORE-NEW-ENGINE-CHECKPOINT-18-N06B-APPROVED-REPORTS-2026-10-30_AR.md)

## 1. تنفيذ فعلي لتوليد XLSX — لا مجرد فحص نص الشيفرة

أُنشئ المكوّن `android/app/src/main/java/com/servoc/app/report/V6ResearchXlsxSheets.java`، commits `3ae031a64053b0394da1c8555315bfa4c303bd02`، `bdda0f2fbfba7b93b83817b5df08c430c64be1c3`. وهو **مستخدم الآن فعليًا في** `SmartFieldSessionXlsxWriter.java` (commit `2128c9d382314277c68a647b5ccbe985e36ba114`). هذا يمنع وجود نسخة منفصلة من الشيفرة مخصّصة للاختبار: نفس مولد الأوراق الذي يستدعيه Android هو الذي يخضع لاختبار JDK/OXML.

- تدقيق سابق لتوليد الورقتين: `schema=hazrob-tank-v6-approved-field-readonly/1`، وتطابق `field_session_id` مع حزمة الجلسة المعتمدة، `reported_identity=UNKNOWN`، عدم ترخيص ppm أو G3، عدم تعديل سجلات V5، وتطابق عدد دورات الغاز المعتمدة مع الحاوية المصدرية. تُرفض المخالفة قبل إنتاج الملف.
- عند عدم وجود دورة غاز معتمدة لا تُنشأ ورقة V6 وهمية. عند وجودها تُضاف ورقتان مستقلتان **دون حذف أو تعديل الأوراق السابقة**: `V6 بحثي غازات` و`V6 مصادر وتعارضات`.
- يستخدم مكوّن الأصل المبني والمثبّت `sohpany/SERVOC-Android@6281d3eb7c1c961ce63b0b5e4ae0792874626720`، `SmartXlsxPackage.java`، الذي يُنشئ الحزمة الحقيقية `xlsx` عبر ZIP وOOXML.

**اختبارات إنتاج الملفات الفعلية:** [GitHub CI 40975202131](https://github.com/HazRob/HazRob-Tank/actions/runs/40975202131) **SUCCESS**. تجميع JDK17 للمولد الإنتاجي والـpackager الأصلي، توليد:
 
| عينة اختبارية مصطنعة — ليس مصدرًا ميدانيًا | دورات الغاز | فرضيات منحنيات المصادر | حجم XLSX | عدد صفوف ورقة MQ | عدد صفوف فرضيات المصادر |
|---|---:|---:|---:|---:|---:|
| `V6_RESEARCH_XLSX_TEST_FIXTURE_3_CYCLES.xlsx` | 3 | 42 | 9,415 bytes | 42 | 48 |
| `V6_RESEARCH_XLSX_STRESS_FIXTURE_100_CYCLES.xlsx` | 100 | 2,800 | 189,126 bytes | 818 | 2,806 |

شملت عملية التحقق: CRC لجميع ملفات ZIP، تحليل XML لكل عناصر `[Content_Types]`، وملفات العلاقات و`workbook` والـworksheet، وأسماء الأوراق الثلاث العربية، ظهور سجل V5 التجريبي السابق، `UNKNOWN`، المعرّفات المرجعية، هروب رموز XML الخاصة، غياب NaN/NULL النصية في الخلايا، وإثبات أحجام وأرقام الصفوف. ورُفض الاختبار الذي حاول إدخال حزمة جلسة أجنبية أو ترقية ppm أو G3 إلى حالة اعتماد.

**بصمات الملفات:**
- ثلاث دورات: SHA256 `a706501ee3f1d4bcc13e3a9f743cfe45714bbb14111f0df54d94a5421ca91267`.
- مئة دورة: SHA256 `00d4728ba909e9da6ab714a303fa3c219e66ced584e4504123fa3c192dd86346`.
- Artifact ID `12941419764` ZIP SHA256 `94529671fab197ebb1f4a34df5094c7a6ed8aa3f07438466626c6ec0d7b35e27`، مرجع النتائج `research/chemcore_v6/results/n06c_real_xlsx_ooxml_2026-10-30.json`.
- حزمة الاختبارات محفوظة في **مكتبة الملفات الشخصية**: `/HazRob/MQ_Reference_Measurements_2026/HazRob_Tank_V6_N06C_Real_Excel_Fixtures_and_Checks_2026-10-30.zip`، معرف `libfile_cd53eb0a8694819184dab8b1d17ac3b4`.

## 2. تعزيز سلامة اعتماد الجلسة خلال إنتاج التقارير

أُضيف إلى `V6FieldSessionResearchAppendix.analyzeVerified` التحقق **مرة قبل** القراءة المشتقة لكل دورة معتمدة **ومرة بعدها** باستدعاء `FieldCycleRepository.verifyApprovedEvidence(cycle)`، حتى لا تُحتسب قراءات SQLite تغيّرت أثناء إنتاج الملحق برغم أن `FieldSessionReportExporter.resolve` تحقق منها في البداية. التعديل في commit `1ec5c2b01c98192a58c80b0e352ab8d611a027e9` واختبار العقد في `0378f5db11a9176e376db7350048aa7c55308899`.

[Android Java compilation + 14 checks 40975800422](https://github.com/HazRob/HazRob-Tank/actions/runs/40975800422) **SUCCESS**، و[science/source regression CI 40975800321](https://github.com/HazRob/HazRob-Tank/actions/runs/40975800321) **SUCCESS** على نفس مصدر الشيفرة الأخير. هذه مؤشرات استمرارية البناء، وليست إثبات تشغيل PDF/XLSX على هاتف فعلي.

## 3. اختبار PDF على محاكي Android — مسار الاختبار

أُنشئ نشاط اختبار مؤقت **غير داخل تطبيق الإنتاج** `research/chemcore_v6/tests/android/V6ResearchPdfSmokeFixtureActivity.java`، commit `91fbbf314e9d07f794017032dc56ed8ab46bc332`، والذي يستدعي **الطريقة الأصلية لصفحة ملحق V6** داخل `SmartFieldSessionPdfWriter` عبر reflection، ويولد ملف PDF من بيانات مصطنعة **7 دورات × 8 قنوات MQ** (3 صفحات)، ثم يعيد قراءته ورسم الصور على Android باستخدام `PdfRenderer` ويحسب وجود محتوى غير فارغ لكل صفحة.

Workflow `.github/workflows/chemcore-v6-n06c-emulator-pdf-smoke-ci.yml` يعمل على نسخة source مؤقتة، مع Activity تُضاف إلى manifest المؤقت فقط. لا يتسرب كود الاختبار إلى تطبيق Android الفعلي. لإنجاز بناء تطبيق الاختبار المؤقت، يستخدم مفتاح توقيع **مُنشأ داخل CI ويُتلف بانتهاء التشغيل**؛ **لا يُستخدم مطلقًا لتوقيع إصدار V6 أو ترقية APK مستخدم**. سجل التشغيل الأول [40975702846](https://github.com/HazRob/HazRob-Tank/actions/runs/40975702846) **FAILED** بسبب غياب keystore الأصلي، وعولج في commit `980cef87067e9f33f3793785f87430578f31be3d`. 

**حالة إثبات PDF:** تشغيل المحاكي [40975895004](https://github.com/HazRob/HazRob-Tank/actions/runs/40975895004) قيد التحقق النهائي وقت تسجيل هذا الجزء. لا يُعتمد نجاحه أو ملفه إلا بعد `SUCCESS` مع إثبات صفحات PDF وPNG الثلاث في Artifact.

## 4. القيود العلمية والسياسية

- ملف XLSX **فحص فعلي للمولد الإنتاجي باستخدام بيانات اختبارية مصطنعة**، وليس تصديرًا من قاعدة بيانات حقيقية لجلسة مصادق عليها على جهاز Tank. لا يغني عن اختبار قراءة جداول الجهاز ومصادقة المستخدِم بعد ترحيل V9/V10.
- تشغيل PDF على المحاكي يعرض **صفحة V6 فقط** باستخدام renderer الأصلي؛ لا يثبت سلامة جميع صفحات التقرير متعدد المخططات أو مسار الاعتماد الكامل في Android.
- **لا موافقة G3 ولا عتبة إنذار غازي ولا نسبة تركيز ppm قابلة للإبلاغ**. بيانات المصادر MQ4 الحقيقية في Checkpoint16/17 لم تغيّر سياسة التأهيل.
- لا تغيير لمشروع HazRob الأصلي، ولا لبرنامج تسخين MQ المؤجل، ولا لحركة الروبوت، ولا دمج لمحرك نضارة الغذاء في غازات ChemCore.
- **حالة التقنيات:** 78/80 تنفيذ بحثي جزئي، 2/80 غير منقولتين؛ Tank physical G3=0/80.
- **لا يوجد APK V6 إنتاجي أو موقع بتوقيع المستخدم**؛ أي APK ينتجه Workflow المحاكي مؤقت للاختبار وحده ولا يوزّع.

## 5. استئناف N06-D

بعد اختبار ملفات XLSX وPDF على fixtures، المرحلة اللاحقة هي اختبار **ترحيل SQLite V9/V10 إلى V6 دون كتابة في السجلات المعتمدة**، ثم اختبار شامل `FieldSessionReportExporter` مع قاعدة بيانات جلسة أصلية متاحة، وفحص PDF متعدد الصفحات، واختبار قبول ورفض الجلسات على هاتف Android واتصال ESP32 الحقيقي.

**Resume token:** `V6_CHECKPOINT19 / N06C_REAL_XLSX_OOXML_PRODUCTION_SHEETS_PASS_40975202131 / 3_AND_100_CYCLES / 42_AND_2800_SOURCE_FITS / 14_ANDROID_CONTRACTS_PASS / APPROVED_SHA_REVERIFIED_BEFORE_AFTER / PDF_EMULATOR_40975895004_PENDING / NO_TANK_G3 / 78_PARTIAL_2_UNPORTED / NO_PRODUCTION_APK`.
