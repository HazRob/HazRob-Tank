# HazRob Tank V5 — الدفعة G: حماية NVS وتوثيق سلسلة القياس/التقرير

**التاريخ:** 2026-10-28 (+03:00)  
**المستودع:** `HazRob/HazRob-Tank`، الفرع `feature/tank-v5-implementation`  
**HEAD قبل كتابة السجل:** `76de04decb1e83579d19248d544bd796c40d4add`  
**القرار:** الإذن ببناء V5 **مشروط** بإنهاء جميع التعديلات وفحوص التشغيل؛ لا APK ولا Gradle/Actions build قبل إغلاق الشروط.

## 1. مراجعة الكتابة السابقة لمعايرة R0

كانت معايرة V4 تعتمد سجل `r0_v4` واحدًا، بينما بدأ V5 باستخدام قبول/رفض مرحلي دون تغيير مفتاح NVS. هذا يحمي من الكتابة قبل الموافقة، لكنه قد يعرض آخر قيم R0 لخطر الفقد عند الكتابة نفسها.

**تنفيذ الدفعة G:** أصبح تخزين R0 بعد الموافقة في سجلين مستقلين `r0_v5_a` و`r0_v5_b`، لكل منهما `R0_SLOT_MAGIC` و`generation` و`R0Record` و`checksum`. يكتب `saveR0Record` في **السجل غير الفعال فقط** ثم يعيد قراءته والتحقق من محتواه، ويختار `loadR0Record` أحدث سجل صالح. إذا فشلت قراءتهما يعود إلى `r0_v4` القديم **للقراءة فقط** دون محوه. لا تكتب المعايرة أثناء التجميع أو الحالة `CAL_PENDING`.

**القيود:** هذه سياسة مقاومة لفشل الكتابة في المصدر، وليست اختبارًا مثبتًا لسلوك NVS الفعلي عند فصل الطاقة. يجب تفليش الفيرموير وتجربة فشل/نجاح الكتابة، وتسلسل الاعتماد والإلغاء وإعادة التشغيل على ESP32. الرجوع إلى فيرموير V4 بعد اعتماد V5 قد يعيد استخدام R0 القديمة، لأنه لا يقرأ مفاتيح V5 الجديدة.

## 2. تصحيح بيانات الجزيئات الفيزيائية

اكتُشف أن `FieldCycleController` يتحقق من وجود `particles.installed/ok` قبل السماح بالدورة، لكن `TelemetryRecorder.sample` لم يكن يحفظ `particles` داخل `motion_payload` الذي يستعلم عنه `FieldCycleRepository.hasMeasuredEvidence`. كانت النتيجة أن دورة جزيئات فعلية يمكن أن تُرفض دائمًا لغياب الدليل المخزن.

أضيف الآن نسخ `snapshot.particles` عند توفره إلى `motion_payload.particles` دون تغيير البيانات أو اختلاق قيم لجهاز PM غائب. تظل بوابة 10 سجلات صالحة والتأكد من `installed=true` قبل البدء. **لا يوجد PM مثبت مؤكد في فيرموير Tank الحالي، ولذلك لا يجوز إعلان اختبار PM فعلي ناجح.**

## 3. منع تعديل نتائج ChemCore بعد اعتماد دورة قياس

كانت `evidence_sha256` لدورة القياس تشمل `measurement_sessions.analytics/payload` وعينات `telemetry_samples`، لكن **لا تشمل** صفوف `chemcore_decisions` التي تستخدمها تقارير الجلسة والموقع. كان ممكنًا مبدئيًا أن يتغير قرار التحليل بعد الاعتماد دون تغير بصمة العينات.

- أضيف إلى `FieldCycleRepository.evidenceDigest` كل صف `chemcore_decisions` المرتبط بنفس `measurement_session_id` بترتيب `t_ms,id`، مع `id` و`t_ms` و`payload`، وبترميز SHA256 length-prefixed متسق.
- بقي التحقق عند نقل `REVIEW_READY → APPROVED`، وأضيفت دالة `verifyApprovedEvidence(Cycle)` لإعادة الحساب على السجل المعتمد عند التصدير.
- `FieldSessionReportExporter.resolve` يستدعي الدالة قبل إنشاء Bundle؛ لذلك **مسارا PDF وExcel للجلسة** ومسارا **PDF وExcel للموقع** اللذان يستدعيان resolver نفسه يرفضان دوريًّا أي تعديل غير متسق بالدليل.
- تحفظ محاولات `FAILED / CANCELLED / SUPERSEDED / DELETED` في سجل التدقيق، لكنها لا تدخل الاستنتاج العلمي بوصفها دورة معتمدة.
- `LEGACY_APPROVED` التاريخية مستثناة من فرض بصمة V5 غير موجودة أصلاً، مع استمرار حواجز اعتماد V4 وحقول النطاق.

**لا يعتبر هذا إثباتًا ضد جميع أنواع العبث بقاعدة SQLite؛ هو منع لانزلاق سجل التقرير عن الدليل الموافق عليه داخل التطبيق.** يحتاج اختبارات تغيير payload الفعلي وإعادة التصدير وSAF.

## 4. اختبارات المصدر

- **22/22** فحصًا مصدرِيًا مباشرًا على مفاتيح NVS، checksum/النسخ المتناوب/الاعتماد، حفظ PM، ربط قرار ChemCore بالبصمة، وجود بوابة build معطلة.
- أضيفت اختبارات JUnit المصدرية `TankV5ScientificLineageAndParticlesContractTest`، ووسعت `TankV5PendingR0ProtocolTest` لاختبار ترتيب السجلين والتوافق القديم.
- حدثت `TankV5SessionSiteCycleReportContractTest` و`TankV4FieldReportPipelineContractTest` لتتحقق من الدالة الجديدة بدل النص القديم `activeFieldSession` أو موضع Repo الذي تغير.
- صحح `TankV5PendingR0ProtocolTest` مسار fixtures إلى `../firmware/ESP32-Tank/`، وحدثت `.github/workflows/build-tank-android-v5.yml` لتنسخ ملفات الفيرموير داخل `source/firmware` **عند تشغيل workflow لاحقًا فقط**.
- جميع هذه اختبارات **مصدر لم يتم تشغيل JUnit أو ترجمة C++/Java بواسطتها**. ليس لدينا بعد دليل Android lifecycle أو PDF rendered content على جهاز.

## 5. متطلبات الإغلاق المتبقية

1. ترجمة/تشغيل فيرموير V5 على ESP32 وتجربة 300+60s R0، وحالات فشل NVS، والانقطاع، والإلغاء والاعتماد ونسخ R0 القديمة.
2. اختبارات JUnit/Gradle Android واختبارات بيانات SQLite V4→V5 والترحيل الحقيقي.
3. دورة قياس 60+60 حقيقية ثم عدة دورات معتمدة لكل جلسة، وجلسات متعددة في الموقع، واختبار تقارير PDF/XLSX مع تعديلات مقصودة على دليل ChemCore بعد الاعتماد لرفض التصدير.
4. قناة GitHub Stable manifest عامة قابلة للتحقق، وتثبيت/rollback online/offline وإعادة تحميل المحركات، وظهور نسخ مكتبة Stable الفعلية.
5. مراجعة الوظائف الأربع لتبويب مكتبة المطور، الصور/التباين/العرض الأفقي/السهم وEcho والسيرفو، وأمان قطع الاتصال وريلاي MQ.
6. فحص مفتاح توقيع تحديثات Android واستمرارية ترقية الإصدار V4 للمستخدمين دون حذف البيانات.

**بوابة APK لا تزال مغلقة** ولم يُنشأ `docs/implementation/V5_RELEASE_GATE_APPROVED.md`. عند استئناف النقاش، راجع هذا الملف أولًا ثم `V5-R0-STAGED-INTEGRATION-2026-10-28.md`.

`CHECKPOINT_G=SOURCE_FIXES_COMMITTED / SOURCE_AUDIT_PASS / JUNIT_NOT_RUN / ESP32_NOT_FLASHED / APK_NOT_BUILT`.
