# HazRob Tank V9.1 — متطلبات دمج ChemCore Next R3 الشامل

**القرار:** V9-APP-R3-002 / Full Android pipeline integration  
**الفرع:** `feature/tank-v9-implementation`  
**الأساس البحثي:** `research/chemcore-next-r3-integrity-trace-20261106`  
**تاريخ التنفيذ:** 2026-11-06  
**حالة النشر:** APK اختبار مع بقاء التأهيل الميداني G3 وقرار الفريموير مستقلين.

## أ. العقد التنفيذي من استجابة ESP32 إلى كل المخرجات

| المرحلة | المكون التنفيذي | ما ينفَّذ فعلًا |
|---|---|---|
| 1 | `NextAndroidAdapter.freeze()` | نسخ قراءة `/data` الأصلية وتعريف وحدات Rs/ADC/R_air وحالة الريلاي، دون نقل R0 خاطئ |
| 2 | `NextEngineRouter.analyzeAndPersist()` | التحقق من SHA لمكتبة Stable المثبتة، وتكوين `ChemCoreNext.Input` من نفس اللقطة |
| 3 | `ChemCoreNext.analyze()` | تطبيق حواجز جودة المصدر، بصمات ومنحنيات الخبراء، فصل الأدلة حسب جذر المصدر، ترابط عدة حساسات، والامتناع عند تضارب أو مرجع غير مؤهل |
| 4 | `NextAnalysisTrace` | إنشاء حدث غير قابل للتعديل لكل مرحلة نفذت فعلًا، بما فيها أسباب الاستبعاد والمقاربات العددية البحثية |
| 5 | `NextDecisionStore.appendWithTrace()` | حفظ القرار وأصل اللقطة وجميع الأحداث في **معاملة SQLite واحدة** مع قراءة تحقق وSHA-256 |
| 6 | SQLite DB16 `next_research_trace_v16` | إضافة منفصلة إلى DB15؛ مفاتيح مرتبطة بالقرار وإسناد إلى hash اللقطة والمحرك، مع مشغلات منع UPDATE/DELETE |
| 7 | `NextDecisionEnvelope.fromReceipt()` | عقد موحد واحد من **سجل SQLite المقروء والتحقق منه**؛ ترتيب المرشحين وtrace SHA والأحداث نفسها لواجهة القياس والتصدير |
| 8 | `MeasurementsTab` | نصوص الواجهة العربية: عدد الأحداث والتقنيات، أسماء المصادر وحساساتها ونتائجها وأسباب الاستبعاد دون حجب المرشحين داخليًا |
| 9 | `TankInstantV7PdfWriter.writeNext()` | صفحات ملحقة لكل الأحداث الأصلية في PDF القياس الآني؛ بدون تغيير الصفحات السابقة |
| 10 | `SmartFieldSessionPdfWriter`, `V9NextR3TracePdfAnnex` | ملحق صفحات لكل أحداث الجلسة الميدانية المعتمدة |
| 11 | `SiteSummaryReportExporter` | صفحات R3 مفصولة حسب جلسات الموقع المرتبطة بدليل المصدر، دون دمج أصول الجلسات كأنها تعرض واحد |
| 12 | `V8NextFieldXlsxWriter` | 20 ورقة قائمة + ورقة جديدة لجميع أحداث R3، مع كود السبب والقيمة المصدرية والمعرّفات الأصلية |
| 13 | `V8NextSiteXlsxWriter` | 6 أوراق قائمة + ورقة جديدة تدمج أرشيف الأحداث **حسب كل جلسة مستقلة** لا حسب تراكيز مخترعة |
| 14 | تاريخ V8/V15 | عند غياب trace في قرار قديم، يُعرض `NOT_ARCHIVED_BEFORE_V9_R3`؛ لا يُعاد تحليل بيانات قديمة بغرض ملء سجل لم يُحفظ حينها |

## ب. حدود المعنى الكيميائي

- كل `source_diagnostic_value` معادلة مصدرية/بحثية، وليست قياس ppm مؤهلًا. لم تؤهل التجارب السابقة G2/G3 التراكيز الحقيقية.
- مرجع `R_air` المقاس على جهاز Tank لا يساوي بالضرورة `R0_source` الخاصة بالشركة أو الورقة البحثية؛ لا جسر تحويل تلقائي يصدّق الهوية.
- `NextResearchSourceBindings.abstentionOnlyCurves()` يحتفظ بالمرجع غير المؤهل في سجل الاستبعاد والاشتراطات العلمية، مع إبقاء التقديرات المشروطة منفصلة في `NextScientificDiagnostics`.
- تأكيد هوية المادة وppm والإنذار الكيميائي المهني يظل `false` حتى تتوفر معايرة ميدانية G3. ولا يمكن ادعاء دقة ≥90% من محاكاة نموذج على نفسه.
- لم تُعدَّل هوية المكتبة الموحدة أو بوابة تحديثها من GitHub ولا برنامج ESP32 `API4` ولا ESP32-CAM ولا قرارات PWM/servo/LED المفتوحة.

## ج. التوافق والترحيل

- `ReleaseAudit.VERSION=9.1`، `versionCode=10`، و`DB_VERSION=16`؛ لم يُحذف أي عمود قديم أو جدول قياسات أو مواقع أو معايرة.
- `NextDecisionStore.latestForSession()` يفضل إصدار R3 الجديد عند تعادل زمن اللقطة المحفوظة مع قرار V9/V8 قديم، دون تعديل القرار السابق.
- إدراج السجل `appendWithTrace` يتحقق من `INPUT` و`FINAL` وعدد الأحداث وبصمة SHA وسلامة الأصل؛ فشل كتابة أي جزء يمنع نجاح المعاملة المزدوجة.
- لا يختلف تعريف API4 أو GPIO في هذا التعديل، لذا **لا حاجة إلى فريموير جديد لمجرد تحديث المحرك**.
- توقيع APK الحالي تجريبي Debug مستقل؛ قبل تثبيته فوق إصدار سابق، تأكد من تطابق المفتاح أو انسخ بيانات الجلسات والمواقع وR0 احتياطيًا.

## د. بوابات الجودة

1. 50 تحققًا لوحدة المحرك R2+R3 الأصلية، دون تفعيل G3.
2. 57 اختبار عقد لـV8 لعدم تدهور الواجهات والجلسات والتقارير والمعايرة.
3. اختبارات `test_v9_r3_trace_contract.py`: تشغيل SQL المأخوذ حرفيًا من Java لتعريف DB15/DB16، التحقق من المحفوظات ومشغلات عدم التعديل، والمسار البرمجي الموحد للواجهة/PDF/XLSX.
4. بناء `compileDebugJavaWithJavac` + `assembleDebug` + `compileDebugAndroidTestJavaWithJavac` على المصدر الكامل للهاتف.
5. Android Emulator API31: `NextTraceV16InstrumentedTest` و`V9NextR3PdfAnnexInstrumentedTest` والاختبارات القديمة، بما فيها التجزئة وإعادة فتح قاعدة البيانات وعدم فقد جلسات R0. **نتيجة تشغيل المحاكي منفصلة عن مجرد نجاح ترجمة اختبار الجهاز**.
6. التحقق من `aapt` و`apksigner`، ومحتوى Stable وبصمة APK الفعلية قبل نشر الحزمة.

## هـ. فهرس إعادة الاستئناف

- بناء APK: [GitHub Actions — V9.1 R3 Full](https://github.com/HazRob/HazRob-Tank/actions/workflows/v9-chemcore-next-r3-apk.yml)
- اختبارات محاكي Android: [V9.1 R3 DB16 & PDF](https://github.com/HazRob/HazRob-Tank/actions/workflows/v9-next-r3-full-integration-android-device.yml)
- المسار التاريخي V9.0: [V9-APP-R3-001](V9-APP-R3-INTEGRATION-2026-11-06_AR.md)
- المتطلبات الميكانيكية والإلكترونية المفتوحة: [V9 Master](V9-MASTER-CHANGE-TRACK-2026-11-06_AR.md) و[Execution Tracker](V9-EXECUTION-TRACKER-2026-11-06.json).

**لا يُعَدّ نجـاح APK دليلًا على دقة كشف المواد أو على سلامة قيادة/تحكم عتاديين.**
