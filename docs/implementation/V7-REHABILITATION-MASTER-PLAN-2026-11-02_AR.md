# HazRob Tank V7 — خطة إعادة تأهيل المنظومة التحليلية والجلسات
التاريخ: 2026-11-02 (Asia/Aden)
نقطة الأساس: V7 commit b344b8ac1c651794fb600baae029efd0f2522ce8; tree a3bb5353eec46c7b0b32fbbb1624d3165e4d4cdc.
فرع التنفيذ المستقل: feature/tank-v7-rehabilitation
مؤشر التنفيذ: docs/implementation/V7-REHABILITATION-TRACKER-2026-11-02.json
الحالة الأولية: SOURCE_DIAGNOSED / PLAN_DOCUMENTED / NO_APP_CODE_MODIFIED / NO_APK / NO_FIRMWARE_FLASH.

## ضوابط
- كل التعديلات المستقبلية في فرع التأهيل فقط. V7 الأصلي وV6/V5 ومستودع HazRob الأصلي دون تعديل. لا تغيير لقيم R0 على الأجهزة أو NVS أو التوصيلات أثناء تدقيق الشيفرة.
- الاستعادة المقصودة: مسار ChemCore السابق المستخدم في V5، لا مجرد تغيير ترتيب قائمة V6. لا يدعي هذا المسار دقة مثبتة أو تحققًا ميدانيًا G3.
- مدخلات قياس حقيقية وموقوتة، وتمييز قاطع بين إنجاز التسجيل وبين اعتماد الاستدلال الكيميائي، وبين البحث التجريبي وبين إنذار سلامة مهني.
- عدم ملء البيانات مجهولة المصدر بأرقام مصطنعة أو تصنيف «آمن». الحقول المفقودة توثّق UNKNOWN.
- لا إصدار APK قبل اكتمال اختبارات التكامل والتقارير، ولا تنسب المحاكاة إلى قياس الغاز الفعلي.

## المرحلة A — التشخيص الموثق عند تأسيس الخطة

### R01 — استرجاع ChemCore القديم
- V5 file: android/app/src/main/java/com/servoc/app/chemcore/TankInstantAnalyzer.java ،blob a24b8e7e433e24565adb400cc3f605cfd02ac3df ،فرع feature/tank-v5-implementation. يستدعي ChemCoreInferenceEngine.analyze مع TankStableRuntimeCatalog.responseModels، وTankFingerprintScreening، والنتيجة التراثية AnalyticsSnapshot، مع حالة تحليل أدلة غير م4.
- V7 file: نفس المسار،blob c3fb91f9c7a34645aaa034399e4befadcffbc169؛ يستدعي TankV6TelemetryAdapter.analyzeSnapshot ويستخرج قائمة المرشحين من ledger منحنيات المصدر. فئات ChemCoreInferenceEngine وInverseMolecularSolver وCompletedSessionChemCoreAnalyzer لا تزال في طبقات V7؛ لذا يوجد اختلاف بين المسار الآني ومسار بعض الجلسات.
- الإجراء: استعادة V5 engine routing مع تكييف API/SQLite V14 ومكتبة Stable وسجل provenance، وتوحيد إخراج analysis envelope في المسارات الآنية والجلسات والمختبر. يُترك V6 بوصفه مسارًا بحثيًا مؤرشفًا غير مؤثر على ranking الرئيسي، من دون تدمير بياناته أو نسخه.
- معيار القبول: parity على لقطات V5 محفوظة ثم حالات V7 نفسها، مع تحقق UID والملاءمة والرفض UNKNOWN؛ posterior هو relative ranking weight وليس احتمالًا معايرًا.

### R02 — الجلسات والريلاي
- FieldCycleController الحالي 60s warmup +60s capture؛ يطلب requestFieldStop("ready") ثم ينتظر تحرر حجز التسجيل ويختبر repo.complete، وبعد النجاح يطلب mqPower(false). في failWorker يمكنه أيضًا فصل الريلاي عند الفشل.
- FieldCycleRepository.complete يعيد false دون تحديد أي شرط أخفق، ويتطلب نهاية recording و60 ثانية وعينات مخزنة و10 scan_cycle مستقلة للغاز و10 عينات env للفيزيائيات المختارة، وقرار ChemCore للجلسات الغازية.
- العيب: رفض لسبب معين يُعرض كـFAILED دون fail_reason التفصيلي؛ وشرط عدم ظهور relay في snapshot مفقود قد يُفسر فقد HTTP عابرًا على أنه OFF.
- التنفيذ: آلة حالات ARM → WARMUP → CAPTURE → FLUSH → VERIFY_PERSISTED → POWER_OFF → REVIEW_READY. التأكد من commit readback وended_ts وعدد العينات ونسخة قرار التحليل والبصمة قبل فصل الريلاي في المسار الناجح، ثم التحقق من حالة OFF. في حالات الخطر/فقد الاتصال يجوز الفصل الآمن العاجل مع كود SAFETY_SHUTDOWN؛ لا تترك تسخين الحساسات إلى أجل غير محدود.
- أعطال محفوظة ومقروءة: TELEMETRY_STALE, RELAY_OFF_DURING_CAPTURE, DEVICE_ID_MISMATCH, INSUFFICIENT_MQ_CYCLES, INSUFFICIENT_ENV_SAMPLES, MEASUREMENT_NOT_CLOSED, STORAGE_ERROR, COMMIT_READBACK_FAILED, ANALYSIS_PERSISTENCE_ERROR, CHEMCORE_INSUFFICIENT_EVIDENCE, SAFETY_SHUTDOWN. لا تعلن «نجاح قياس» إذا فشل الحفظ؛ وفي المقابل لا تُعد UNKNOWN الكيميائية فشلًا في حفظ بيانات صحيحة.
- اختبار: قطع الاتصال مرة واحدة وخلال أكثر من 6s، power-off مفاجئ، غياب 10 scans، نقص BME، DB write fail، إعادة المحاولة، وقبول ثم تصدير.

### R03 — القياس الآني وعدّاد الوقت
- الوضع الحالي يبدأ/ينتهي يدويًا، ويحلل لقطة latestDiagnostics الختامية؛ شريط تقدم 0/500/1000 ليس مؤقتًا لمرحلتين.
- مطلوب POWER_VERIFY → WARMUP(60s) → RECORD(60s) → FINALIZE → ANALYZE → SUCCESS/FAILED. العدّاد التنازلي الحقيقي لكل مرحلة وفق elapsedRealtime؛ عدد العينات والدورات الفريدة وتاريخ أول/آخر مسح مع تقرير الحالة.
- التسجيل لمدة 60s من عينات حقيقية، لا إنشاء زمن افتراضي من snapshot واحدة. فحص freshness/scan_cycle/quality.valid/R0 provenance، مع ربط operation_id/hash والنتيجة نفسها بكل من الشاشة والتقرير. لا توجد لقطة قديمة ترث نتيجة operation جديدة.
- معايرة R0 180s+60s منفصلة تمامًا، ولا يعاد ضبطها أو تغيير مصدرها بسبب مؤقت القياس الآني.

### R04 — ثبات المرشحين وجودة الربط
- عيب مثبت: ترتيب TankInstantAnalyzer V7 مأخوذ من أول ظهور للمادة في source_curve_hypotheses داخل LinkedHashMap ويستخدم rank=distinct.size()+1؛ لا يستعمل Engine.Result.rankedCandidates أو jointResidualRmseLog10.
- الاختبار التاريخي V7 الأصلي خمس حالات مختلفة: Actions 41171598687. اختبار عشرة متجهات بعيدة: Actions 41173692574. بقي ترتيب 13 مادة ثابتًا كلما سمحت بوابة الاستجابة بعرض الجميع، رغم تغير البواقي وترتيب المحرك الداخلي. هذه نتائج محاكاة فقط.
- واجهة renderResearchOnlyV6 بطاقات بيضاء ثابتة لا تستدعي candidateColor. كما أن بعض المستهلكين يفضلون ok الأصلي حتى عندما يضع CanonicalMeasurementNormalizer quality.valid=false لقراءة STALE.
- المطلوب: قاعدة واحدة للمصدر التحليلي engine_candidates → analysis payload immutable → UI/PDF/XLSX. دفتر منحنيات المصدر source_curve_hypotheses مستقل للشفافية البحثية ولا يصنع ranking. إظهار score/residual/coverage/source/unknown_reason، وألوان أدلة علمية لا دلالات خطورة.
- اختبار: الهواء عند baseline، عشر متجهات متباعدة، حالة ok=true+quality.valid=false، R0 من جهاز آخر، missing MQ، scan_cycle مكرر، فساد وحدات، تعرضات غاز معروفة ومجموعة holdout. يجب أن يتغير ترتيب المرشحين أو يصدر UNKNOWN بحسب الأدلة؛ لا تفرض نتيجة مادة من بيانات اصطناعية.

### R05 — استكمال قاعدة المركبات
- مصدر Android: ExperimentsDb.DB_VERSION=14، القائمة الواحدة v14_unified_chemical_catalog. توثيق V13 سجل 734 مادة مع 144 legacy indices مدمجة، و82 حقلًا؛ يجب عد حقول V14 فعليًا وقياس الترحيل. 213/734 LFL و179/734 UFL و1432 limit records حسب checkpoint N09B؛ غالبية GHS/normal ranges غير موثقة.
- الاستكمال يعني لكل uid: أسماء/CAS/مرادفات/صيغة/فئة، قدرة MQ2 MQ3 MQ4 MQ5 MQ7 MQ8 MQ9 MQ135، availability fingerprint/curve/ppm/M4، الطبيعي وسياقه، OSHA/NIOSH limit ووحدته وفترته، LFL/UFL، GHS، أثر صحي وSDS ومصدر وتاريخ مراجعة وحالة مجهول.
- حقل مجهول يبقى NULL/UNKNOWN، لا صفر ولا false. لا استنتاج ppm من PEL أو حدود هواء طبيعي؛ لا ادعاء خطر بناء على رمز GHS لمادة مجرد مرشحة.
- التسليم: chemical_completeness_audit.csv، reference_audit.json، fresh DB + migrated DB 734 unique UID، اختبارات مصدرية للحقول ومطابقة CAS.

### R06 — تقارير التحليل
- V7: TankInstantReportPdf يحيل V6 payload إلى TankInstantV7PdfWriter. الجلسات/المواقع تستخدم FieldSessionReportExporter والقرارات المؤرشفة. بعض كتّاب V7 مخصصة للتخطيط البصري دون كل الرسوم المصدرية.
- المطلوب ضمان أن كل تقرير غازي (instant PDF، field PDF/XLSX، site PDF/XLSX، controlled lab) يستخدم EXACT candidate order/score/UNKNOWN والـR0 والـsampling provenance من سجل واحد غير قابل لإعادة التفسير. تقارير Physical/Food تبقى مستقلة في نوع الدليل.
- القبول: اختبار مقارنة JSON↔UI↔PDF↔XLSX، إعداد خمس عمليات متباينة بما فيها هواء مرجعي، غياب بيانات، معدل العينات، تقسيم الصفحات RTL وظهور الرسوم/الرموز؛ اختبار الاعتماد وتغيير الدليل بعده (رفض التصدير). لا يُعاد ترتيب المواد داخل renderer.

### R07 — Echo
- Config.h: TRIG GPIO23، ECHO GPIO39 (مدخل 3.3V لا يجوز 5V مباشرة)، ULTRA_PERIOD 150ms، ULTRA_TIMEOUT_US 25000. Hardware.cpp يستخدم ISR نبضات، ويصدر no_falling_edge عندما يرى rise دون fall ضمن المهلة. ControlTab لا يعرض مسافة ما لم proximity.valid=true.
- مطلوب تشخيص source/timing + hardware: pin/voltage divider + GND + signal integrity + scope capture أو logic analyzer لـTRIG 10µs وECHO، counters no_rise/no_fall/echo_high/invalid_pulse، وأهداف مرجعية 10 و20 و50 و100 سم مع اختبار عدم وجود هدف.
- لا تُنشأ مسافة وهمية ولا تُفعّل الحركة/التوقف الآلي بسبب Echo؛ هو تحذيري، والدفع رباعي العجلات مشكلة منفصلة لا تغيّر مخطط GPIO ضمن تشخيص Echo.

### R08 — Stable
- النسخة التشغيلية المضمنة الموثقة في V7: 1.0.0-hr069r1، 652 هوية و49 research prior، لا M4/G3. prepare_v7_stable_source.py يضبط مجال إصدار 7.0.
- updates/chemcore-stable/README.md يذكر أن runtime production manifest على main غير منشور. مستودع HazRob/build-harness commit 031f5642ecbba1fab5b4be06d94413211f3f2559 يحتوي لقطة مصادر بحثية بتاريخ 2026-10-21 مصرح بأنها runtime_activation=false؛ لا يجوز تحويلها صامتًا إلى تحديث تشغيلي.
- تحقق من أي نسخة Stable أحدث مُعتمدة للمستهلك Android مع manifest/SHA256/schema/sampling compatibility. إن لم توجد نسخة معتمدة أحدث، أبقِ النسخة 1.0.0 مثبتة في التطبيق، واظهر الحالة الصحيحة دون الادعاء بأحدثية إصدار غير منشور. اختبارات offline/rollback/manifest tamper والتوافق V7.

## المرحلة B — ترتيب تنفيذ المراحل
G00: قفل source manifest للنسخ V5,V7,Stable وفحص migration؛ بدون تغيير التطبيق.
R01: استرجاع المسار القديم مع ملفات اختبارات parity.
R04: تصحيح استهلاك engine candidates وجودة القنوات وربط الواجهة.
R02 + R03: وحدات state machine، تسخين وتسجيل فعلي، حفظ معاد الاعتماد، وتشخيص أخطاء.
R07: فحص Echo عتادي وبرمجي بحسب الحالة no_falling_edge.
R05: توسعة التغطية المرجعية من مصادر مؤهلة، audit تدريجي لكل UID.
R08: تضمين أحدث Stable approved والتحقق offline.
R06: إعادة ربط كل التقارير بعد تثبيت عقود التحليل والجلسات.
G09: full source/Gradle/JUnit/SQLite/device/PDF/XLSX regression، ثم gate APK لاحقًا.

## بوابات إثبات الأداء
- اختبارات JUnit/Android/SQLite على نسخ جديدة ومهاجرة؛ لا تعتمد الاختبارات النصية وحدها.
- الاختبار البرمجي يستعمل inputs مأخوذة من V5 وV7 ويقارن outputs hash/JSON وقرارات UNKNOWN ويثبت عدم وصول بيانات تالفة.
- الاختبارات الفيزيائية لتمييز الغازات تحتاج هوية وتركيز معروفين وأجهزة مرجعية، replicate holdouts مستقلة لكل تعرض/يوم/جهاز، sensitivity/specificity/confusion matrix/abstention مع توثيق ظروف البيئة. G3 0/80 حتى تكتمل هذه الشواهد.
- لا ادعاء بدقة هوية «مطلقة»؛ المطلوب مطابقة تقارير التطبيق للقرارات المؤهلة بدقة كاملة وتوثيق حدود عدم اليقين.
- إصدار APK ممنوع قبل نجاح gate G09 وإكمال الاختبارات المطلوبة، لا وعد برابط بناء مسبق.

## نتائج أولية محفوظة وأدلة مطلوبة
المنجز الآن: مراجعة مسارات الشيفرة، تحديد V5 blob، إعادة قراءة نتائج محاكاة V7 المؤرشفة، تدقيق وحدات الجلسات والتقارير وEcho ومصدر Stable. لم يُنفّذ نقل المحرك ولا إصلاح التطبيق ولا اختبار ميداني جديد.
ملفات الأدلة المستقبلية داخل research/v7_rehab/results:
source_manifest.json; legacy_engine_parity.json; field_cycle_failure_matrix.json; instant_60_60_timing.json; candidate_replay_10case.json; chemical_completeness_audit.csv; report_parity_checks.json; echo_bench_matrix.csv; stable_payload_integrity.json.
سجل الإصدار النهائي بعد اكتمال جميع الاختبارات: docs/implementation/V7-REHABILITATION-RELEASE-GATE.md.

## آلية الاستئناف
1. اقرأ هذا الملف ثم V7-REHABILITATION-TRACKER-2026-11-02.json.
2. تحقق من branch HEAD والـcommit الأخير ومن ملفات الأدلة؛ لا تكرر اختبارًا فاشلًا باعتباره ناجحًا.
3. اعمل على أصغر خطوة NEXT في tracker، واكتب source_sha/evidence_file/test_run/observed_result.
4. حدّث status من NOT_STARTED إلى IN_PROGRESS إلى VERIFIED فقط عند وجود دليل اختبار، وإلا BLOCKED/BENCH_REQUIRED.
5. ممنوع تعديل V5/V6/V7 المرجعي أو قيم R0 على الروبوت، أو نشر Stable بحثي غير مفعل.
