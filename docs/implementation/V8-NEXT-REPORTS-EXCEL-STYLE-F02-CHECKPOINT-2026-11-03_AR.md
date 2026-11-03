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
