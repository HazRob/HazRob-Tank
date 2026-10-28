# HazRob Tank V5 — تدقيق ربط دورات القياس بتقارير الجلسات والمواقع

**التاريخ:** 2026-10-28 (+03:00)  
**طلب المالك:** مراجعة وتأكيد الربط الصحيح بين دورات القياس الميداني وتقارير الجلسة والموقع بكل جلساته.  
**الفرع:** `feature/tank-v5-implementation`  
**HEAD عند إعداد الوثيقة:** `df18a34da35fa337011fafb225dee3d7e3933af6`  
**الحالة:** `SOURCE_AUDIT_PASS / SQLITE_FIXTURE_PASS / PDF_XLSX_DEVICE_PENDING / NO_GRADLE_OR_APK`.

## مخطط مسار البيانات الإلزامي

```text
Site(site_id)
  └── FieldSession(field_session_id, site_id, APPROVED)
        ├── FieldMeasurementCycle(cycle_id, sequence_no, attempt_no, state, device_id, SHA)
        │     ├── Approved → measurement_session_id (only this enters analytical reports)
        │     └── Failed/Superseded/Cancelled/Deleted → audit ledger only
        │
        ├── Session Smart PDF & XLSX
        │     ├── Strict approved-cycle linkage set verification
        │     ├── Existing independent analytical content
        │     └── V5 paginated cycle audit PDF / full Excel cycle ledger
        │
        └── Site summary (approved Field Sessions only)
              ├── Site Smart PDF + paginated per-cycle matrix
              ├── Site Smart XLSX + كل دورات الموقع sheet
              └── 144 recurrence scans every APPROVED child cycle;
                  each material contributes at most once per parent session
```

## أخطاء التصميم التي تم تحديدها وتصحيحها

| الحالة قبل التصحيح | العيب | الإصلاح |
| --- | --- | --- |
| `FieldSessionReportExporter.resolve` يختار أفضل قياس تحليلي فقط | لا توجد بوابة تحقق أن قائمة الدورات المعتمدة تطابق القياسات المدرجة في التقرير | `cycleLedger` في Bundle؛ التحقق من `measurement_session_id` لكل دورة معتمدة، تطابق مجموعتي المعرفات، منع الازدواج والتصدير الصامت لبيانات ناقصة |
| `SmartFieldSessionPdfWriter` يختصر النتائج وقد لا يعرض كل محاولات القياس | يمكن ألا تظهر المحاولات أو دورة رقم 8+ في PDF | صفحات ملحق متغيرة العدد: رقم الدورة، المحاولة، الحالة، الخيارات، التوقيت، معرف القياس، مختصر SHA |
| `SmartFieldSessionXlsxWriter` | لا سجل تدقيق مرتبط بنفس snapshot التقرير أو احتمال قراءة متغيرة مستقلة | Sheet `دورات القياس` من `b.cycleLedger` نفسه؛ يحتفظ ببصمة SHA كاملة وأسباب الفشل والتاريخ |
| `SiteSummaryReportExporter` | لا يُظهر صفًا تفصيليًا لكل دورة في كل جلسة | PDF يضيف صفحات `cycleMatrix` لجميع دورات جميع الجلسات المعتمدة، مع حساب تلقائي لعدد الصفحات |
| `SmartSiteXlsxWriter` | جدول جلسات الموقع يحصي الدورات دون عرض تفاصيلها والتحليلات المستقلة | Sheet `دورات الموقع` يضم جميع المحاولات، IDs وSHA وOutcome عند توافره، وestimated vs reportable ppm، دون دمج متوسطات |
| `SiteReportIntelligence.plan` | يقرأ `primaryDecision` فقط لمصفوفة 144، فيغفل دليل دورة ثانية | قراءة `screening_144` من كل دورة معتمدة، ثم اتحاد المواد **داخل الجلسة الواحدة** قبل حساب `supportedSessions` للموقع |
| `SiteSummaryReportExporter.SessionSummary` | خطر أو LFL في دورة غير رئيسية قد لا يظهر على مستوى الموقع | فحص المخاطر في دورات الجلسة المعتمدة كلها، مع الاحتفاظ بالبوابة الكمية الصالحة للإنذار وعدم تحويل EST إلى تعرض مؤكد |
| `SiteSummaryReportExporter.resolve` | لا تحقق صريح من المعرفات المكررة/دورة أو جلسة من موقع مختلف | بوابة `siteId` لكل جلسة، فحص bundle.site وbundle.fieldSession، منع التكرار والاختلاط بين المواقع |
| اختبارات V4 | بعض العقود ما زالت تتوقع بالضبط دورة واحدة | عدلت اختبارات المصدر لتشترط `complete>=1` مع حالة اعتماد صريحة لكل دورة واستبعاد `SUPERSEDED` |

## قائمة تغطية التقرير

| المستوى | PDF | Excel | النطاق |
| --- | --- | --- | --- |
| تقرير الجلسة | صفحات الرسوم الأساسية + صفحات ملحق دورات القياس حسب العدد | ورقة دورات كاملة إلى جانب أوراق التحليل والتتبع | الجلسة المعتمدة المحددة فقط، الدورات المعتمدة للتحليل، غير المعتمدة للتدقيق |
| تقرير الموقع | مصفوفة الجلسات + صفحات ملحق دورات كل الجلسات المعتمدة | ورقة `دورات الموقع` تتضمن sessionId/cycleId وعلاقة المصدر | جلسات الموقع المعتمدة فقط، دون جلسات مواقع أخرى |
| التجميع العلمي | دعم مصفوفة 144 على مستوى كل دورة ثم deduplicate by Field Session | تكرار المواد على مستوى عدد الجلسات المستقلة | لا تتضخم قيم التكرار عند تكرار القياس في نفس الجلسة |
| السلامة | تحذيرات من أي دورة معتمدة فقط إذا اجتازت البوابة | نتيجة سياقية مع توضيح أن EST لا يثبت تجاوزًا | لا تُدمج القيم الفيزيائية أو PPM في متوسط عام غير مبرر |

## الاختبارات المنفذة فعليًا في هذه الدفعة

1. **24/24 فحص مصدر مؤشرات** تربط `FieldCycleRepository` و`FieldSessionReportExporter` و`SmartFieldSessionPdfWriter` و`SmartFieldSessionXlsxWriter` و`SiteSummaryReportExporter` و`SmartSiteXlsxWriter` و`SiteReportIntelligence`. كلها `PASS` بالمطابقة المصدرية، وليس تنفيذ JUnit.
2. **12/12 ملف Java** من مسار التقارير/قواعد الاعتماد اجتازت فحصًا بنيويًا للأقواس والسلاسل والتعليقات. هذه ليست `javac` ولا تؤكد الأنواع أو الاعتمادات.
3. **10/10 اختبارات fixture SQLite من بيئة Python مستقلة**: موقعان، 4 جلسات منها 3 معتمدة؛ للموقع المختبر جلستان معتمدتان و4 قياسات معتمدة و6 سجلات دورة تدقيق، بما فيها `SUPERSEDED` و`DELETED`. النتيجة المستخرجة `f11: m11,m12,m14` و`f12: m21`. الموقع الآخر والجلسة غير المعتمدة مستبعدان. تكرار المواد `{64:2,65:1,71:2}` محسوب **لكل جلسة لا لكل دورة**. هذا نموذج SQL اصطناعي، ليس `ExperimentsDb` Android على جهاز.
4. أضيف `TankV5SessionSiteCycleReportContractTest.java` لتقييد ربط PDF/XLSX ومصفوفة الموقع ودورات التحليل المتعددة عند تشغيل JUnit مستقبلًا. **لم يتم تشغيل هذا الاختبار** تنفيذياً لأن المستخدم حظر Gradle/بناء التطبيق.

## حدود التحقق وبوابات الإغلاق

- `NOT_RUN`: Android Gradle/JVM tests، تثبيت/ترقية قاعدة SQLite V4 الحقيقية، PDF render وفحص الصفحات واللغة العربية والخطوط، Excel unzip/ZIP integrity/LibreOffice أو قارئ الهاتف، SAF `ACTION_CREATE_DOCUMENT` على الجهاز، فقد الاتصال والتزامن وذاتية relay.
- `NOT_RUN`: الوصول إلى النتائج الفيزيائية الحقيقية على ESP32، عينات غاز ذات R0 صحيحة، واعتماد PPM أو استنتاج امتثال مهني. لا يجوز افتراض السلامة من مرور اختبار منطقي.
- شيفرة V5 للتحليل والمرشحين والمختبر/التجارب وتحديثات GitHub لا تزال فيها مهام غير مكتملة. **ممنوع بناء APK حتى إذن المستخدم الصريح اللاحق.**

## نقطة الاستئناف الدقيقة

- شيفرة ربط قياسات الجلسات والتقارير موجودة في `feature/tank-v5-implementation`.
- التالي: مراجعة `V5-001` بالحالات الطرفية (توقف Activity أثناء sampling، النقر المزدوج، 60+60، تسجيل عينات الغاز والفيزيائية، جهاز PM غير متوفر)، ثم `V5-002` توحيد تحليل ChemCore/المرشحين، و`V5-007` R0، وبقية خطط W03..W11.
- لا يُستنتج `DEVICE_PASS` من هذه الوثيقة؛ بوابة `SITE_REPORT_RELEASE_READY` تظل `BLOCKED_PENDING_ANDROID_TESTS`.

**العقد النهائي:** `REPORT_SCOPE=APPROVED_CYCLES_ONLY / AUDIT_SCOPE=ALL_CYCLES / SCIENCE_SITE_RECURRENCE=PER_PARENT_SESSION / BUILD=NOT_RUN`.
