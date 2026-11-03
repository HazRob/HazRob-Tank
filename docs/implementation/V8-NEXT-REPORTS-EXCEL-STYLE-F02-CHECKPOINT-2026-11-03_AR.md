# HazRob Tank V8 — نقطة استئناف تحويل تقارير ChemCore Next وتنسيقات Excel

**تاريخ:** 2026-11-03 • **فرع العمل:** `feature/tank-v8-implementation`  
**قرار المستخدم:** جميع تقارير PDF التي كانت تعتمد على ChemCore القديم يجب أن تعتمد على **نتائج ChemCore Next للقياس المعتمد نفسه**. يجب أن تعتمد ملفات Excel التحليلية على Next أيضًا مع المحافظة على نظام التنسيق المرئي المعتمد، والبيانات الخام البيئية وADC/ADS وMQ ومعلومات القياس الأصلية. للموقع عدة جلسات؛ لكل جلسة **قياس واحد معتمد**.  
**التوافق الخلفي:** تبقى تقارير الجلسات التاريخية التي لا تملك سجل Next محفوظًا على بياناتها القديمة، ولا تُحوَّل بأثر رجعي إلى نتائج Next زائفة.

## A. مسارات الإنتاج المراجَعة
| الحالة | PDF | Excel | مصدر القرار |
|---|---|---|---|
| القياس الآني | `TankInstantReportPdf.writeNext` → `TankInstantV7PdfWriter.writeNext` | لا ملف Excel مستقل ثابت في هذا المسار ضمن هذا التدقيق | لقطة Next المحددة |
| الجلسة الميدانية الحديثة | `FieldSessionReportExporter.writePdf` → `SmartFieldSessionPdfWriter` + `V8NextChartRenderer` + `V8NextFieldResearchAppendixPdf` | `FieldSessionReportExporter.writeXlsx` → `V8NextFieldXlsxWriter` | `NextDecisionStore.latestForSession(primaryMeasurementSessionId)`، بصمة SHA والتحقق من الجلسة |
| الجلسة التاريخية | نفس الكاتب لكن branch تاريخي وملحق V6 السابق | `SmartFieldSessionXlsxWriter` القديم | المصدر التاريخي الأصلي، **لا ادعاء بأنه Next** |
| الموقع متعدد الجلسات | `SiteSummaryReportExporter` → `NextSiteEvidenceCharts` عندما يوجد Next | `SmartSiteXlsxWriter` → `V8NextSiteXlsxWriter` عندما يوجد Next | تقرير الموقع يقرأ قياس كل جلسة معتمدة مستقلة، ولا يجمع ppm |
| التقارير الفيزيائية والغذائية والانبعاثات | تبقى محركاتها المختصة؛ لا تُمزج بقاعدة الغازات ChemCore Next بلا سند | لا تُنقل بياناتها إلى محرك كيميائي آخر | مصادرها الأصلية |

## B. تغييرات التنفيذ الأخيرة
1. **المخططات العلمية:** 12 خانة PDF ثابتة الحجم ومرتبطة بمخرجات Next؛ استجابة MQ الزمنية مبنية على `t_ms/Rs` الأصلية، مع عدم مضاعفة `scan_cycle`.
2. **ملحق الجلسة الأخيرة:** كان التقرير الحديث ما زال يستدعي `V6FieldSessionResearchAppendix.analyzeVerified` للصفحة السادسة. الآن يُتجنب **تمامًا** عند وجود Next ويُستبدل بصفحة `V8NextFieldResearchAppendixPdf` المبنية فقط على `NextDecisionStore.Receipt`، واللقطة الأصلية، وجودة MQ، وUID المرشحين البحثيين، وجذور المصدر، وStable SHA. نفس مساحة 842×595 ونفس نظام الترويسة والتذييل؛ يبقى ملحق V6 للجلسات التاريخية.
3. **Excel للجلسة:** 20 ورقة وفق النسق الأصلي: القرارات والأدلة والجودة والمراجع وقياسات البيئة والحساسات والـADC/ADS والحمولات الأصلية لكل عينة. **لا يُعاد إدراج مصفوفة 144 القديمة أو احتمالات Posterior غير موجودة في Next**.
4. **Excel للموقع:** 6 أوراق، كل جذر مصدر وUID يحسب مرة لكل جلسة مستقلة، مع تحديد السجلات التاريخية بوصفها `LEGACY_ARCHIVE`.
5. **دورة الاعتماد:** اختبار Android إضافي يتحقق من رفض وجود قياسين بحالة مراجعة في الجلسة، ثم اعتماد قياس واحد والجلسة معًا ضمن معاملة SQLite واحدة ورفض التكرار؛ بيانات الاختبار تُلغى بمعاملة rollback.

## C. أدلة تأهيل مؤكدة
- Android Java compilation وG1: **PASS** <https://github.com/HazRob/HazRob-Tank/actions/runs/41567686809> عند `9588cc315`.
- Android 13 PDF and XLSX: **PASS** <https://github.com/HazRob/HazRob-Tank/actions/runs/41567633840> عند `d1be30b76`، بما يشمل صفحة Next السادسة بدون عبارة أو تشغيل V6؛ PDF جلسة 6 صفحات وPDF موقع 5 صفحات.
- XLSX theme: `SmartXlsxPackage.java` القديم لم يتغير، XML متوافق، RTL وفواصل ورؤوس وألوان التعبئة وخلايا الأعداد مثبتة؛ المصنفات **20 / 20 / 6 ورقات** للقديم وNext والموقع. فحص `test_v8_next_xlsx_style_and_lineage.py` نجح، كما تساوى SHA256 لشجرة التنسيق بين المصنفات الثلاثة (البادئة: `51459c5d1a66dbc07e`).
- Android13 fixtures **ليست** تجارب Gas G3 ولا معايرة جهاز Tank حقيقية ولا APK إصدار.

## D. البوابة التالية
- اختبار F02 الجديد بتغييرات `7ad818671`, `9b05a3290`, `009d5f029`, `80a6973be` قيد التشغيل في run <https://github.com/HazRob/HazRob-Tank/actions/runs/41568397468>. **لا توضع علامة نجاح قبل مراجعة النتيجة**. إذا تعثر، اقرأ `n06g_status.json` وسجل الـjob؛ أصلح سبب الاختبار أو مسار الإنتاج المثبت فقط.
- تدقيق الاستخدام الفعلي لزر الاعتماد وإظهار سبب الفشل وقراءات ADC على جهاز حقيقي، ثم بوابة F01/F02/F03 والتحليل G3 قبل إصدار APK.
- الإجراء عند انقطاع الاتصال: اقرأ هذا الملف و`docs/implementation/V8-REPORT-AND-APP-EXECUTION-TRACKER-2026-11-03.json` وأحدث commit في نفس الفرع، ثم `GitHub Actions`؛ لا تستنتج PASS من مجرد وجود شيفرة أو نتيجة تشغيل أقدم.

**قيد علمي:** يحتفظ Next بإيصال قرار مصدره الأصلي، لكن `CoreContracts.Result` لا يحوّل وحده فرضيات المنحنيات المرجعية إلى شهادة ppm/هوية/سلامة؛ يجب عرض القيم البحثية الصادرة من مصادرها ضمن حدود صحة المصدر دون اختلاق قيم مفقودة.


## E. استئناف 3 نوفمبر 2026 — إصلاح تعارض Next مع العقد القديم (F05)

- **الفشل المثبت:** تشغيل [F05 #41568863694](https://github.com/HazRob/HazRob-Tank/actions/runs/41568863694) على الالتزام `14a31796bd` فشل في الاختبار المتكامل بعد **128 تأكيدًا**، لأن `SmartFieldSessionPdfWriter.writePdf` كان يرفض `Bundle.analyticalContract == null`. هذه قيمة مقصودة وصحيحة لجلسة V8 التي تحمل قرار Next مؤرشفًا، وليست دليل فقد بيانات.
- **الإصلاح الإنتاجي:** الالتزام [`e5f152ed6b62ed13b2c515bc7106894d056fb5bc`](https://github.com/HazRob/HazRob-Tank/commit/e5f152ed6b62ed13b2c515bc7106894d056fb5bc) غيّر `SmartFieldSessionPdfWriter.java` لقراءة `NextDecisionStore.Receipt` قبل اختبار العقد القديم. إذا كان الإيصال موجودًا، تُستخدم `Bundle.primarySeries` الأصلية و`measurementSessionId` نفسه دون استخدام `ReportIntelligence` أو `CandidateHazardContext` أو `posterior` القديم. إذا لم يوجد Next، يحافظ المسار التاريخي على سلوكه وعقده الأصليين.
- **التجميع:** [Android compilation #41569572735](https://github.com/HazRob/HazRob-Tank/actions/runs/41569572735) **SUCCESS** على الالتزام المصحح. **الجرد:** [UI inventory #41569572577](https://github.com/HazRob/HazRob-Tank/actions/runs/41569572577) **SUCCESS** على الالتزام نفسه (جرد شيفرة لا تأكيد لمس جميع الأزرار).
- **اختبارات تحتاج متابعة:** [F05 #41569572553](https://github.com/HazRob/HazRob-Tank/actions/runs/41569572553) للتحقق من إنتاج Next PDF/20 XLSX والموقع/6 XLSX والتتبعية الأصلية، و[Android12 #41569572604](https://github.com/HazRob/HazRob-Tank/actions/runs/41569572604) لسلامة مسار Android12؛ **كانت RUNNING وقت تسجيل هذه النقطة** ولا تُعلَّم PASS قبل النتيجة الفعلية.
- **ثوابت لم تتغير:** ملفا `V7ReportLayoutKit.java` و`V7FieldChartLayout.java`، صفحات PDF ونسق Excel، مكتبة Stable، الفريموير؛ ولا APK مبني أو اعتماد Tank G3.
- **الاستئناف بعد الانقطاع:** تحقق من HEAD ثم نتيجتي F05/Android12 أعلاه. إذا فشل F05، اقرأ `n06g_status.json` وسطر الاستثناء الفعلي من job log، وعالج سبب الاختبار أو المسار المنتج فقط. لا تعيد استخدام عقد ChemCore V5/V6 في تقرير Next، ولا تنقل نتائج المصدر التجريبي إلى PPM معتمد. بعد اجتياز CI تابع F01 للتفاعل الحقيقي، F02 توقيت الريلاي + 60/60 وR0، F03 Stable والجودة، فحص RTL والتنسيقات على هاتف فعلي، وأخيرًا Tank G3.
