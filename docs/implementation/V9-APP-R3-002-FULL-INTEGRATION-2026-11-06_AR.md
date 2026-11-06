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

## و. اختبارات الانحدار والقبول المستقل

- **البناء APK V9.1 + مصدر R3 + تصدير الجلسات/الموقع:** نجح تشغيل [42210814434](https://github.com/HazRob/HazRob-Tank/actions/runs/42210814434) على SHA `5c2f0bf4eb356fa2d8f6c16d4d52ebee86314708`؛ بصمة APK `e2a001c79ed52aff259bdd52c83836d4ba3f2c6a9c0252f2a5d4a436f94df0ce`. هذا إصدار تجريبي مؤقت إلى حين فحص اختبارات المحاكي واعتماد آخر تصحيح توافقي.
- **فحص SQLite/واجهة/PDF/XLSX البرمجي:** نجح 4/4 tests executing original Java SQL تعريفات DB15 وDB16 عبر Python SQLite مع مشغلات منع تعديل السجلات، دون المساس بمعايرة R0.
- **محاكي Android12 القديم:** كشف التشغيل [42209301111](https://github.com/HazRob/HazRob-Tank/actions/runs/42209301111) عن 3 أخطاء من 46 اختبارًا، جميعها `NoSuchMethodException` لاختبارات تعكس مُنشئ `NextDecisionStore.Receipt` التاريخي. أعيد تنفيذ مُنشئ التوافق التاريخي الذي يمرر إلى مُنشئ R3 الموسع دون تغيير إسناد الأرشيف في التزام `052bfeba81f4484484e2cdede4486adc6fcf2560`. **لا يُعد هذا الاختبار القديم ناجحًا**، وإعادة اختبار المحاكي على الالتزام المصحح مطلوبة.
- **تشغيل الاستعادة:** [GitHub Actions — V9.1 R3 Android Emulator](https://github.com/HazRob/HazRob-Tank/actions/runs/42211484163)؛ النتيجة النهائية تُؤخذ من صفحة Actions فقط.

## ز. نتائج البناء واختبارات التشغيل المؤكدة

- نجح بناء كامل التطبيق وإنتاج APK ‏9.1 مع DB16 وجميع أوراق Excel وملحقات PDF على [تشغيل GitHub Actions رقم 42212315578](https://github.com/HazRob/HazRob-Tank/actions/runs/42212315578)، والتزام الشيفرة `19d277e9f9579e087fa14e829bec8a68c04a5e91`.
- [تنزيل أحدث حزمة APK](https://github.com/HazRob/HazRob-Tank/actions/runs/42212315578/artifacts/13499681417)، باسم `HazRob-Tank-V9.1-ChemCore-Next-R3-FULL-TEST.apk`، بصمة SHA-256: `09fae1755e7bd86db3fa1407d482d9f5ea9a9832671f4e1ca7f09b8899913ac1`. توقيع Android Debug SHA-256: `44a30c08c218b042630ec23ef4a88f4b72b23794b8b047190b0998a44b306a12`.
- فحص المصدر والبناء: 50 تحققًا لمحرّك R2/R3، و57 اختبار عقد احتفاظ V8، و4 اختبارات SQLite/ربط جميع المخرجات؛ نجح تجميع `compileDebugAndroidTestJavaWithJavac` كذلك.
- **اجتازت اختبارات محاكي Android 12 ‏49/49 حالة** على [تشغيل 42211484163](https://github.com/HazRob/HazRob-Tank/actions/runs/42211484163) بعد إعادة توافق مُنشئ `NextDecisionStore.Receipt` التاريخي؛ لم يسجل هذا الاختبار أي فشل أو تخطٍ.
- بعد نجاح جولة المحاكي، أضيفت معالجة مشاركة Feedback العامة: **المعلومات الأصلية في SQLite باقية كاملة، لكن حزمة الملاحظات التي قد يوافق المستخدم على نشرها إلى GitHub تحذف معرفات الأجهزة ونصوص أسباب المصدر الحرة** مع الحفاظ على مراحل الاستدلال والأرقام المصدرية وبصمة Trace. وُضع اختبار محاكي جديد؛ [حالة تشغيل 42212307301](https://github.com/HazRob/HazRob-Tank/actions/runs/42212307301) مستقلة ولا يُزعم اجتيازها قبل انتهاء Actions.

**معيار القبول النهائي العلمي:** بناء البرمجيات وتحقق SQL/Android لا يعوضان معايرة G3 واختبارات تشغيل الحساسات والريلاي الفعلية؛ يظل G3 غير مؤهل، وكذلك ملحقات قيادة الذراع وPCA9685 والإنارة غير مقررة في هذا المسار.
