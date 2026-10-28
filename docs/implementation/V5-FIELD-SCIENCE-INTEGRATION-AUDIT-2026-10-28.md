# HazRob Tank V5 — دفعة E: اختبار تكامل الجلسات وChemCore والتقارير (دون بناء)

**تاريخ الإنجاز:** 2026-10-28 (+03:00)  
**الفرع:** `feature/tank-v5-implementation`  
**HEAD قبل كتابة هذا السجل:** `fab59c3bc39d51f6bd46bc6f1902d428d1f2891b`  
**المالك:** يطلب إتمام التعديلات واختبارها، ولا يسمح ببناء التطبيق/تشغيل Gradle أو إصدار APK حتى يأذن.  
**الحالة:** `SOURCE_INTEGRATION_FIXED / 25_OF_25_STATIC_CHECKS_PASS / NUMERICAL_FIXTURE_PASS / DEVICE_NOT_TESTED / NO_GRADLE_NO_APK`.

## 1. تدقيق المسار المرجعي

```text
Field Session selected by operator (immutable field_session_id)
   → FIELD_OPERATION reservation
   → ESP32 relay ON, identity confirmed
   → 60 s warmup with live link watchdog
   → FIELD_CAPTURE(owner_id = selected field_session_id)
   → TelemetryRecorder starts USER field_60s_cycle
   → FieldSessionRepository.linkMeasurement(owner_id,technical_id)
   → DB telemetry_samples, every poll tagged to technical_id
   → 60 s sampling + fail-closed stop
   → completed technical measurement with frozen recorded R0
   → per-channel median from >=10 DISTINCT hardware scan_cycle IDs
   → active SHA-checked Stable/reference curve + research fingerprint
   → ChemCore decision persisted with estimated PPM, evidence source,
     exposure reference inventory and conditional skull/flame warning
   → FieldCycleRepository.complete validates evidence, SHA, device & ownership
   → operator approval
   → Field Session PDF/XLSX + Site PDF/XLSX: approved cycles only
```

## 2. العيوب التي اكتُشفت وأُصلحت فعليًا

| الفجوة | الأصل | الحل المحفوظ |
| --- | --- | --- |
| خطر انتقال قياسات الجلسة إلى **آخر جلسة نشطة** بدل جلسة المشغل | `TelemetryRecorder.start` استخدم `fieldSessions.activeFieldSession()` | استحداث `FIELD_OWNER_SESSION` في `MeasurementSessionRegistry`؛ يتطلب `requestFieldCapture(fieldSessionId)` ويحفظ المعرف أثناء التسجيل؛ `FieldCycleController` يتأكد من `linked.fieldSessionId`، و`TelemetryRecorder` يستخدم المعرّف المحجوز فقط ويفشل إذا فُقد. |
| تحليل دورة جلسة مدتها دقيقة مبني على **آخر لقطة**، لا على عينات الدقيقة | `CompletedSessionChemCoreAnalyzer.observedRatios(terminalSnapshot)` | مسار `observedRatiosAcrossFieldSamples` يأخذ العينات المؤرشفة `ORDER BY t_ms,id`، ويحتفظ بنسبة `Rs/R0` المجمدة مع كل عينة، ويجمع **دورة مسح firmware واحدة فقط** مهما تعددت طلبات HTTP، ثم يستخدم Median لكل حساس إذا توفرت 10 دورات مستقلة. لا يسقط إلى لقطة نهائية عند فشل المسار. |
| نتائج PPM المرجعية والبصمة والحدود المشروطة موجودة في Instant لكنها قد تغيب من قرار الجلسة المخزن | `ChemCoreDecisionRepository.save` اقتصر على `r.toJson()` مع تقدير التعرض المنظم | حفظ `TankFingerprintScreening` و`TankExposureReferenceCatalog` و`TankScreeningExposureWarning` و`screening_ppm_estimate` مع نفس قرار ChemCore الخاص بالجولة؛ يبقى `GasPpmExposureAssessment` المستقل مقيدًا بالدليل المناسب، ولا يصف التقدير بأنه قياس مهني. |
| عمل إطفاء الريلاي وكتابة الفشل قد ينقطع عند إغلاق Activity | `MainActivity.onDestroy`: `fieldSessionsTab.release()` تضع التنظيف في workers ثم `workers.shutdownNow()` و`db.close()` فورًا | خُصص إغلاق منظم: `workers.shutdown()` مع انتظار محدود لا يتجاوز 5 ثوانٍ في thread مستقل قبل الإنهاء النهائي وإغلاق قاعدة البيانات وفك ربط Wi-Fi؛ عُزز `FieldCycleController.release` لفترات ARMING/RETRY قبل تحرير حجز العملية. |

## 3. النتيجة العلمية في تقارير الجلسات والموقع

- لكل حساس في دورة الـ60 ثانية تحسب `median(Rs/R0)` من دورات مسح firmware المستقلة، **لا** من آخر Snapshot؛ مع تجميد R0 عند الالتقاط حتى لا تعيد معايرة جديدة كتابة قياسات قديمة.
- تعمل مصادر المصنع والدراسات ومنحنيات Stable والقواعد البحثية والبصمات في الترشيح والتقدير بحسب صلاحية قيمها ومجالاتها، **دون اشتراط M4 لإجراء الحساب**.
- يحفظ القرار مستوى الدليل وبصمة المصدر والمرشحين وPPM التقديري والمراجع المهنية والتنبيهات **بصفة مشروطة**؛ لا تحوّل مقارنة TWA/STEL حسابية على دقيقة واحدة إلى إثبات تجاوز زمني.
- `FieldCycleRepository.complete` يرفض دورة غاز لم ينتج عنها قرار تحليل محفوظ، أو افتقرت إلى عينات حقيقية؛ تظل محاولتها في سجل التدقيق.
- التقارير تُصدّر قياسات `APPROVED/LEGACY_APPROVED` فقط للمحتوى التحليلي مع إظهار المحاولات الأخرى في سجل التدقيق.

## 4. الاختبارات المنفذة في هذه الدفعة فقط

| نوع التحقق | النتيجة | الحدود |
| --- | --- | --- |
| **25 مصدرًا/عقدًا للتكامل** | `25/25 PASS` | فحص مباشر لمحتوى ملفات GitHub، لا ترجمة Java |
| **حالة عددية مستقلة مكررة القراءة** | `PASS` | 60 قراءة HTTP × 12 دورة firmware فعلية، الناتج Median=0.65؛ حالة 60 HTTP لنفس دورة واحدة تُرفض بسبب أقل من 10 دورات. هذه محاكاة للقاعدة لا تشغيل Android. |
| اختبارات JUnit المصدرية المضافة | `WRITTEN / NOT_RUN` | `TankV5IntegrationOwnerAndEvidenceContractTest.java` يثبت شرط المالك/العينة/الإطفاء/التحذيرات |
| Android SQLite الحقيقي + الجهاز | `NOT_RUN` | يظل لازمًا اختبار ترحيل v9→v10 وحالات فشل الدورة وفتح تقرير PDF/Excel |
| ESP32 فعلي وفصل الطاقة/إنهاء العملية | `NOT_RUN` | الإغلاق المنظم يقلل خطر إيقاف مهمة التنظيف، لكنه لا يضمن إطفاء الريلاي عند قتل العملية بالقوة أو انقطاع طاقة الهاتف |

## 5. ما بقي معلقًا قبل إعلان الجاهزية للبناء

1. اختبارات ترجمة Java/Android وواجهات XML/Java الحقيقية، وفحوص JUnit، وتحليل compat مع أصل V4؛ لم يُنفّذ Gradle.
2. اختبار دورة `60+60` على روبوت ESP32 فعلي في الحالات `normal / lost-link / relay-off / double-tap / app background / Activity destroyed / process killed`.
3. التأكد من التوقيت والتكافؤ بين `FieldCycleRepository` وقاعدة DB فعلية لا نموذج SQLite مصطنع، ومن عرض كل دورة في تقارير الجلسة والموقع بدون التباس.
4. جرد بيانات Stable في APK النهائي (49 علاقة استجابة متوقعة ضمن المصدر) وفحص حساب Ethanol وبقية المصادر على الجهاز.
5. إغلاق `V5-007` لإدارة R0 `PENDING→APPROVE` على ESP32 قبل التخزين الدائم، واستكمال GitHub manifest الحقيقي، وتدقيق الوظائف الأربعة في مكتبة المطور.
6. اختبار سلامة بيانات المصدر وشهادات الاعتماد الفيزيائي، لا اختلاق G3/M4 من اختبارات مصدر.
7. لا يُشغّل `assemble` أو Actions APK حتى يأذن المالك صراحة.

**نقطة الاستئناف:** `V5-W01/W03: OWNER_PINNED + FIELD_MEDIAN_SAVED + REPORT_WARNING_CONTEXT_SAVED / NO_APK / NEXT: NON_BUILD_SOURCE_QA_AND_ANDROID_DEVICE_GATES`.
