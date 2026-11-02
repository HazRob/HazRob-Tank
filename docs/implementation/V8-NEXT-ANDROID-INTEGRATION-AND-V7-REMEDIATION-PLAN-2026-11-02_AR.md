# HazRob Tank V8 — الخطة الشاملة لتكامل ChemCore Next ومعالجة عيوب V7
**معرّف:** HZT8-NEXT-APP-V7-R02 | **تاريخ القرار:** 2026-11-02 | **التكليف الصريح:** 2026-11-02T06:44:21Z | **الفرع:** `feature/tank-v8-implementation`
**الحالة عند تحرير هذه الوثيقة:** مراجعة بنيوية وخطة تنفيذ فقط، **لم يبدأ تنفيذ مهام التطبيق P07–P13 ولم يُبن APK V8**. هذه الوثيقة لا تعيد توصيف نجاح اختبارات ChemCore Next بأنه تحقق ميداني.
**السجل الملزم للتنفيذ:** [V8-NEXT-ANDROID-INTEGRATION-TRACKER-2026-11-02.json](./V8-NEXT-ANDROID-INTEGRATION-TRACKER-2026-11-02.json).
**المراجع المستقلة:** سجل Core `V8-CHEMCORE-NEXT-EXECUTION-STATE-2026-11-02.json` وخطة Next P00–P13؛ التطبيق V7 للقراءة فقط. ChemCore Next مستقل عن V6 وعن تطوير التطبيق وعن الفريموير.

## 1. القرار وحدود النطاق
1. طلب المالك الآن تنفيذ خطة تكامل محرك ChemCore Next ضمن **تطبيق HazRob Tank V8** بعد مراجعة شاملة لأعطال V7. يلغي هذا القرار فقط تأجيل **التكامل التطبيقي P07–P13** المبيّن في نطاق Next التاريخي؛ ولا يغيّر استقلال شيفرة النواة العلمية ولا نتائج تجاربها.
2. **تؤجل تجارب المواد والمقارنات G2/G3** حاليًا. يسمح بتحضير وربط وتشغيل **وضع بحثي موسوم بوضوح** واختبارات G1 التكاملية، ولا يسمح بتسمية غاز مؤكد أو ppm معتمد أو إنذار غاز نوعي كأنه متحقق بواسطة MQ. يُحتفظ بعناصر G2 الناقصة في backlog البحثي، لا تزور نتائجها.
3. لا تعديل تلقائي لفيرموير `ESP32-Tank` و`Cam-tank` أو الأصل رباعي العجلات. لا استخدام لقيم معايرة 1 أكتوبر للروبوت القديم كبديل لـTank V6 يوم 31 أكتوبر. `R_air` التشغيلي ليس `R0_source` الذي تقتضيه منحنيات المصدر؛ كل قيمة وحدة kΩ وتاريخ ومعرف جهاز وطور سخان.
4. تبقى بيانات V5–V7 والقرارات القديمة وتقاريرها قابلة للقراءة كما أُصدرت، ويستخدم V6 **مرجع مقارنة معلنًا أو rollback مسمى**. ممنوع مسار fallback صامت يُنشئ مرشحين أو ppm من V6 بينما تعرض الواجهة اسم Next.
5. لا تكتمل مهمة بمجرد commit؛ لا بد من نجاح اختبار مثبت برقم workflow run، وملف دليل، وتحديث حالة JSON. **العداد الحالي لبرنامج النواة مستقل**، ولا يجب جمع 34 مهام التطبيق مع 37 مهمة النواة كمعدل نجاح مزيف.

## 2. ما أثبتته مراجعة الشيفرة الحالية — لا افتراضات
| الرمز | العيب / موضع الخطر | الدليل البرمجي | خطة العلاج |
|---|---|---|---|
| D01 | **ثبات المرشحين وترتيب غير علمي** | `chemcore/TankInstantAnalyzer.java`: تجميع `source_curve_hypotheses` في `LinkedHashMap` مع `rank=distinct.size()+1`، وربط `legacyId<144` | القائمة وحدها من `NextDecisionEnvelope.rankedEligibleCandidates` وUID مكتبة كاملة؛ source ledger تبويب منفصل لا يعيد ترتيب |
| D02 | **جلسة تصل إلى نهاية التوقيت ولا تُقبل** | `field/FieldCycleRepository.java:185–215`: انتهاء التسجيل + صلاحية samples + `hasRequiredChemCoreDecision` كلها شروط قبل `REVIEW_READY` | handshake محفوظ بوضوح: sealed samples → Next → persisted UNKNOWN/decision → readback/checksum → REVIEW؛ منع سباق tasks |
| D03 | **فشل تسجيل متقطع** | `measurement/FieldCycleController.java:253–339`: freshness 6s، بدء/flush انتظار 12s، مؤقت مرتبط بالـActivity، تنفيذ `host.io()` غير متزامن | process-resilient ownership وACK للـsampling وpersist؛ timeouts مرحلية قابلة للتشخيص، دون إرخاء شرط 60s |
| D04 | **عينة مرئية غير كافية علميًا** | `FieldCycleRepository.hasMeasuredEvidence`: ≥10 scan cycles MQ فريدة، ≥10 physical/particle rows، جلسة مستخدم مكتملة | توثيق سبب الرفض لكل modality: no scan_cycle, insufficient duration, stale, powered_off, no decision, no DB receipt |
| D05 | **تعدد مسارات الاستدلال** | `CompletedSessionChemCoreAnalyzer` ما زال يستدعي `ChemCoreInferenceEngine`؛ `ChemCoreAnalysisTab` يستدعي V6؛ `TankInstantAnalyzer` يستدعي V6 | Router/Adapter واحد لـNext مع snapshot ثابت، إخراج واحد لكل session/instant/lab ومسارات V6 legacy متعقبة |
| D06 | **عطل معايرة R0 في الإعدادات** | `SettingsTab.openUserCalibration()` ينشئ `DeveloperR0Tab` متأخرًا ويقبض استثناء، و`ManualR0Calibration` يخزن مصادر متعددة وتعريفات تاريخية | اختبارات Activity lifecycle/Android12 + firmware progress 180+60 + save8 atomically + ضبط المصدر الحقيقي، إظهار stack trace إلى سجل تشخيص |
| D07 | **مخرجات واجهات وتقارير قد تختلف** | `MeasurementsTab` يعرض مصفوفة 144 و`posterior`؛ تقارير PDF وXLSX منشأة من أكثر من writer | immutable decision ID واحد وsnapshot SHA، لا حساب هوية/ppm/GHS من renderer ولا تغيير الترتيب |
| D08 | **SQLite ومكتبة Stable** | `ExperimentsDb.DB_VERSION=14`، مكتبة chemical uid موحدة، Stable بإصدار/ SHA؛ لا Next decision store مرسخ | migration additive إلى 15 عند التنفيذ، عدم فقد القديم، تأهيل source mapping وchecksum لكل قرار/إصدار |
| D09 | **الجهاز والاتصال** | الريلاي وEcho والكاميرا وBME مرئية في وحدات قائمة؛ أعطال المالك موثقة ولكن السبب الفيزيائي غير مثبت بكل حالة | اختبار bench/Android وESP32 logs والتعافي بلا تعارض مع السلامة؛ عدم ادعاء إصلاح حتى replay/hardware |
| D10 | **النواة العلمية ليست G3** | سجل ChemCore Next الحالي يحصي الاختبارات العلمية لكنه لا يثبت G3 على Tank | TEST MODE فقط بوسم RESEARCH_ONLY؛ لا تأهيل ppm/هوية/قرارات تعرض/اشتعال |

**تنبيه:** D01/D02/D03 عيوب/مخاطر مدعومة بمسار الشيفرة؛ سبب الفشل الميداني لمالك الروبوت **يحتاج سجل تجربة لإسناده**. لا يجوز استنتاج أن فشل جميع الجلسات ناتج من سبب واحد.

## 3. هندسة تدفق بيانات واحدة (SSOT)
```text
ESP32-Tank /diag + /data  (frame ID / uptime / device ID / relay ACK)
        ↓  raw sealed capture [MQ raw/ADC/ADS/Rs, scan_cycle, T/RH, gas power, time]
TelemetryAcquisitionService (background owner; monotonic clock, state machine)
        ↓  persistence staging + unit/quality + validated R_air ref / source R0 separated
NextAndroidAdapter → immutable Next Snapshot (schema + sha256 + source provenance)
        ↓  router with explicit engine tag: NEXT_RESEARCH | V6_COMPARISON_ONLY
ChemCoreNext.analyze → source-matched curves / fingerprint bundles / temporal if real series
        ↓  independent evidence ledger → root conflict graph → arbitration
NextDecisionEnvelope  [UNKNOWN or research hypotheses; NEVER certified ppm/G3]
        ↓  transactional append-only NextDecisionStore → read-back+hash
        ├── Instant/MeasurementsTab + Candidates/Tracking
        ├── FieldSessionsTab / site rollups / approved attempt
        ├── Lab / Developer / Evidence Matrix / feedback
        ├── Reports PDF / XLSX / charts / print
        └── Audit trail, source/Stable manifest, research review
```
**قاعدة عامة:** العرض والتصدير لا يستدعيان inference ثانية. كل جهة عرض تعتمد `decision_uid` و`capture_hash` و`ordered_candidate_uids` نفسيهما. تتطابق قائمة المرشحين الصفرية مع `UNKNOWN` دون حشو، ولا تظهر تسمية «تعرفنا على الغاز» دون G3.

## 4. بروتوكولات العمل الفيزيائي
**معايرة الهواء المرجعي R_air:** 180s إحماء +60s جمع من حساسات الروبوت الحقيقي، مع تاريخ وجهاز و(R_s) ومتوسط/ضجيج وheater phase، commit كامل للقنوات المؤهلة أو لا يعتمد التحديث. قيم (R0_source) وأدلة K من المصنع مستقلة.

**الآني والميداني:** 60s تسخين بعد تأكيد الريلاي +60s **تسجيل فعلي**. لا تساوي مجرد تأخير 120s بقياس صالح. إظهار Warmup, Sampling, Finalizing, Analyzing, Saved/Failed وعداد واضح. عند غياب إشارة حقيقية لا نجاح صوري.

**دورة الجلسة:** `IDLE→ARMED→RELAY_ACK→WARMUP→CAPTURE_ACK→SAMPLING→STOP_ACK→FLUSH→QUALITY_GATE→NEXT_DECISION_SAVED→READBACK→REVIEW_READY→APPROVED`. حالات موازية `FAILED(code)`,`INTERRUPTED`,`SUPERSEDED` مع ربط idempotency key. تعطيل الريلاي **بعد الحفظ والتحقق الطبيعي**، أو فور إيقاف الأمان لدى فقد اتصال/فشل/مهلة قصوى؛ وأي تعذر تأكيد إطفائه يولد إنذار fault منفصلًا.

**القيم الخام:** لا اشتقاق Rs من ADC دون Vcc/RL/مرجع جهد متحقق، ولا PM/O2 إذا غير مركبة، ولا استجابة 4D من snapshot واحد. أية حساسات غير سليمة مسجلة `NOT_EVALUABLE` وليس `NEGATIVE`، ونتائج الهوية ليست مصدر إنذار سلامة معتمد.

## 5. 34 مهمة تطبيقية قابلة للتنفيذ — حسب الأولوية وتبعية P07–P13
المرجع الآلي لكل مهمة في JSON يضم: الحالة، الأولوية، الملفات المستهدفة، اختبارات القبول، ملف الإثبات، commit/CI. جميعها الآن `NOT_STARTED`.

### P07 — البنية الأساسية والقرار الموحد
| المهمة | الأولوية | العمل | قبول إلزامي مختصر |
|---|---|---|---|
| P07.1 | P0 | NextAndroidAdapter: تجميد التليمترية وتحويلها | G1 سلوك null/NaN/انقطاع الطاقة/8 و1 حساس؛ snapshot SHA ثابت؛ لا نقل R_air القديم أو R0_source |
| P07.2 | P0 | SQLite V15 ترحيل محافظ وسجل قرارات غير قابل لإعادة الكتابة | اختبار إنشاء v15 وترحيل v14 مع المحافظة على التاريخ والمرجع؛ atomic commit, unique session+revision, readback/hash, ممنوع duplicate |
| P07.3 | P0 | FeatureFlag: تفعيل Next البحثي مع rollback صريح | Next RESEARCH_ONLY افتراضيًا في البناء التجريبي؛ V6 كمسار مقارنة/تشخيص معلن وليس ترتيبًا مخفيًا؛ كل قرار يوثق engine_id |
| P07.4 | P0 | DecisionEnvelope واحد للواجهات والتقارير | نفس decision_uid/hash/ordered UIDs وUNKNOWN بجميع المستهلكين؛ round-trip واختبار العبث/إعادة التشغيل |


### P08 — القياس الآني والمرشحون
| المهمة | الأولوية | العمل | قبول إلزامي مختصر |
|---|---|---|---|
| P08.1 | P1 | آلة حالات القياس الآني وعدادات 60+60 حقيقية | 0–60 إحماء ثم 60–120 جمع فِعلي؛ عداد إحماء/قياس/حفظ/تحليل؛ منع نجاح قبل ≥60s و≥10 scan cycles مع أسباب فشل |
| P08.2 | P0 | إزالة legacy144 والمرشحين الثابتين من المسار التشغيلي | لا ترتيب source_curve_hypotheses ولا rank من first appearance؛ 0/1/5/10/12 مرشح مؤهل من UID كامل؛ الترتيب يتغير مع اختلاف الأدلة؛ لا حشو |
| P08.3 | P1 | عرض Next وUNKNOWN والأدلة/البصمات والأسباب | تبويبا ChemCore والمرشحون، تنويهات G1/G2/G3، عدد الحساسات والطرق/الأصول، positive/negative/unqualified، لا posterior مزيف |
| P08.4 | P1 | تقرير PDF آني من decision وحيد | مساواة UID/rank/snapshot_hash مع الشاشة؛ PDF A4/Arabic 0–N؛ UNKNOWN لا ppm أو GHS ككشف؛ اختبار فتح PDF |
| P08.5 | P1 | التتبع المقصود والتصحيح دون اعتماد هوية | اختيار canonical UID ومراقبة اتجاه Rs وتغير الزمن حتى عند UNKNOWN؛ المستخدم لا يؤهل الغاز ولا يمحو decision الأصلي |


### P09 — الجلسات والمواقع والتسجيل
| المهمة | الأولوية | العمل | قبول إلزامي مختصر |
|---|---|---|---|
| P09.1 | P0 | إصلاح ملكية الريلاي والجلسات عبر durable state machine | مسار START→RELAY_CONFIRMED→60s→CAPTURE→60s→FLUSH→NEXT→DB_READBACK→REVIEW→SAFE_OFF؛ فقد اتصال/تعطل/Activity destroy لا يترك دورة أو relay معلقة |
| P09.2 | P0 | تثبيت عينات MQ/ADS وأسباب فشل قابلة للتشخيص | ≥10 scan cycles متميزة، قياس وقت monotonic، 60 ثانية فعلية، hash على raw وmetadata، FAILED_REASON enum؛ ADS إن كان المصدر موجودًا فقط |
| P09.3 | P0 | ربط تحليل Next بنتيجة الجلسة قبل انتقالها إلى REVIEW | تمرير 60s series وخبراء الزمن الحقيقيين ثم حفظ decision أو UNKNOWN/INSUFFICIENT؛ إغلاق/قراءة عكسية داخل بوابة complete؛ لا انتظار قرار لن يُنشأ |
| P09.4 | P1 | مواقع وجلسات ومحاولات متعددة واستئناف آمن | استرداد العمليات بعد process death وتمييز retry/superseded؛ الاعتماد فقط بعد evidence check؛ التنقل بين المواقع لا يخلط نتائجها |
| P09.5 | P1 | عدم اختلاق بيانات PM/O2 والقياسات الفيزيائية | خيارات gas/particles/physical تختبر منفصلة ومختلطة؛ غير موجود→UNAVAILABLE لا صفر ولا fake report؛ اختبارات selected-mode 8 cases |


### P10 — المختبر والمعايرة
| المهمة | الأولوية | العمل | قبول إلزامي مختصر |
|---|---|---|---|
| P10.1 | P1 | المختبر: تجربة موثقة ومرجع وتركيز ومصفوفة خام | للتجربة خصائص المصدر، نوع خليط، ppm مرجعي إن وُجد، calibration campaign، evidence root؛ محاكاة منفصلة وواضحة |
| P10.2 | P0 | إصلاح مسار R_air/R0 في الإعدادات والمطور | 180s+60s فقط للمعايرة؛ حفظ 8 قيم atomically وfirmware session/device/provenance؛ فتح/عودة/إلغاء/استيراد/حفظ وrestart Android12 بلا crash |
| P10.3 | P1 | سجلات مقارنة التجربة بالبصمات والمصادر دون ترقية | مرجع المصدر الأصلي ونطاقه وR0_source والدخان/المخاليط؛ تقرير اختلاف النتائج؛ لا تغيير active model من تجربة غير مؤهلة |
| P10.4 | P1 | مراجعة المختبر المركزي وسجل الاعتماد العلمي | feedback append-only، provenance/consent/reviewer؛ انتقال G1→G2→G3 بعناصر الأدلة المحددة دون اعتماد تلقائي |
| P10.5 | P1 | استقلال نضارة الأغذية عن محرك الغازات | اختبار اللحوم والفواكه وتقاريرها لا تستدعي Next الغازي ولا تغير قرار التعرض الغازي أو قائمة المرشحين |


### P11 — المكتبة وStable والمراجعات
| المهمة | الأولوية | العمل | قبول إلزامي مختصر |
|---|---|---|---|
| P11.1 | P0 | مصدر هوية واحد من مكتبة المواد الموحدة | كل المرشحين والتتبع وتقارير المختبر بمفتاح chemical.uid؛ 734 molecular/2 generic حسب manifest المثبت لا افتراض على الجهاز؛ مواد لا نموذج لها تظهر لا تتعرف |
| P11.2 | P1 | Stable مدمج وتحديثات استعلام/تفعيل آمن وrollback | تعطل الشبكة لا يفقد النسخة المدمجة؛ SHA/schema/app compatibility قبل التفعيل؛ التحديث لا يغير النتيجة القديمة؛ مقارنة 49 source priors وDB provenance |
| P11.3 | P1 | مراجعة وتصحيح المشغل مع حفظ القرار الأصلي | جدول مراجعات منفصل append-only؛ لا overwrite لنتيجة محفوظة، الإسناد إلى snapshot_uid والمصدر والمالك |
| P11.4 | P1 | قائمة إرسال اختياري للمختبر بدون إرسال وهمي | local PENDING→SENT فقط بعد receipt من backend معتمد؛ لا سرية/ملفات خام ترفع دون إذن؛ offline retry idempotent |
| P11.5 | P2 | تبويب المساهمات العلمية للمواد والمنحنيات | المقترحات quarantined، المصدر والترخيص وR0 والقياسات ومرجع الهوية؛ لا إدراج في Stable حتى لجنة ومراجعة Evidence Gate |


### P12 — التقارير والانحدارات
| المهمة | الأولوية | العمل | قبول إلزامي مختصر |
|---|---|---|---|
| P12.1 | P0 | توحيد كل تقارير PDF وXLSX على قرار Next الأصلي | تقرير آنية/جلسة/موقع/مختبر/مقارنة، نفس decision_uid/snapshot_sha/UID/rank/status؛ لا إعادة استدلال داخل exporter |
| P12.2 | P1 | حقول ADS raw الفيزيائية في جلسات Excel إن وُجدت | تحقق raw ADC/ADS من source payload وإظهار MISSING صريح؛ توافق Excel; test no fabricated columns |
| P12.3 | P1 | إصلاح الواجهات والألوان والتنقل والقوائم والرسوم | Android 12: الخطوط/التباين/حقول بيضاء على بيضاء/المؤقت/الرسم عند null؛ rotation/theme/background لا تعطل الجلسة |
| P12.4 | P1 | عدم رجوع العيوب غير الكيميائية في V7 | AP/remote/camera/motor/stop on loss, HC-SR04/voltage level/echo timeout, BME/mic/flame; regression بدون تعديل firmware إلا عند ثبوت الحاجة |
| P12.5 | P0 | اختبارات تكامل Android وSQLite والتقارير والفشل | اختبارات SQLite 14→15، force-stop/network loss, 0–N rank, duplicate tap, ANR, PDF/XLSX open, runtime flags; لا قبول مع فشل CI |


### P13 — اختبارات الجهاز وبناء APK
| المهمة | الأولوية | العمل | قبول إلزامي مختصر |
|---|---|---|---|
| P13.1 | P1 | اختبار منصة Tank الحقيقي للريلاي والحساسات وإيقاف الأمان | 8 MQ/2 ADS/relay/scan timestamp/BME/Echo/communication، فقد الاتصال وtimeout وحفظ/عودة؛ تسجيل ملفات raw ومخطط أسلاك |
| P13.2 | P2 | G3 منفصل مع غازات مرجعية ومختبر آمن | 0/80 حاليًا؛ لا تأهيل ميداني حتى قياسات gas reference مستقل ومعايرة ومجموعات تعرّض مستقلة؛ ليس شرط بناء APK بحثي مشروط |
| P13.3 | P1 | بناء APK V8 تجريبي قابل للاختبار مع حدود علمية | source pinned, Android Java17 SDK36 Gradle9.5, versionCode جديد، appId com.hazrob.tank، sign/sha/artifact، APK research-only؛ إصدار عام مقيد بـG3 |
| P13.4 | P1 | تحليل ملاحظات مالك الروبوت وإصلاح عيوب النسخة | استخراج logs وعينات وscreen recordings وfailure IDs، reproducible→fix→regression→new test APK دون حذف قاعدة المستخدم |
| P13.5 | P1 | README وبوابة الإصدار/الرجوع وختم manifest | توثيق sha engine/Android/Stable/APK/firmware، known limitations، backup/restore، منع claim G3 والتمييز TEST/RELEASE |


## 6. قاعدة المفاضلة والمرشحين
- ليس المطلوب 10 مرشحين في كل مرة: العدد `0/1/5/10/12/...` وفق **الأدلة الحقيقية**، مع اختيار حد العرض فقط وليس فرض حد للمكتبة.
- `source_curve_hypotheses`, `exposure_reference_limits`, `catalog_matches`, `rankedEligibleCandidates` كيانات مختلفة. الواجهة والقوائم والتقارير تستخدم **الأخير فقط** للمرشحين، وتظهر تاريخ كل فرضية وسيناريو التداخل.
- لا مساواة بين UID وlegacy ID أو فهرس صف للـ144 أو مُعرّف «الدخان» العام. يستخدم chemical.uid فقط إن تم تثبيت الربط، وإلا SOURCE_CLASS_UNMAPPED مع بحث مادة غير مؤهلة.
- الترتيب المتشابه يجب أن يتغير عند تغير `scan_cycle` والقراءات المؤهلة، لكنه قد يبقى منطقيًا إذا كان القرار UNKNOWN أو لم تتغير البصمات؛ الاختبار يثبت **مسببات الترتيب** لا يُجبره على الحركة بلا أدلة.
- لا يُظهر `posterior` الخام كنسبة احتمال «وجود الغاز» ما لم يُعاير ويثبت. تعرض residual/coverage/domain/status/source-root بشكل موثق. فشل المطابقة ينتج `UNKNOWN` وسببًا، لا انتحار UI.
- عرض رمز GHS لا يعني رصد الغاز؛ تفصل تصنيفات أخطار المادة العامة عن استنتاج هوية وتركيز ومقارنة عتبات، وتبقى قرارات تعرض العامل منفصلة.

## 7. اختبار العيوب قبل الإصلاح وبعده — مصفوفة بوابة قبول
| رمز | السيناريو | معيار النجاح |
|---|---|---|
| S01 | جلسة Gas + Physical، 60+60، ريلاي يعمل ويتواصل | نهاية محددة/حفظ سلسلة وقرار/REVIEW_READY ثم OFF |
| S02 | فصل اتصال عند 10/59/60/119/121 ثانية | FAILED_REASON صحيح؛ إطفاء أمان/إشارة انعدام تأكيد؛ لا جلسة APPROVED |
| S03 | تأخر إنشاء `measurement_session_id` أو flush | لا سباق `repo.complete`؛ wait receipt أو فشل محدد |
| S04 | فقد/تكرار scan_cycle، 9 مقابل 10 دورات | رفض 9؛ قبول 10 فقط مع اكتمال 60s ومتطلبات الجودة |
| S05 | توقف Activity/process وإعادة الدخول | استرداد durable record وإظهار INTERRUPTED/retry، لا بقاء حالة busy |
| S06 | Gas فقط/Physical فقط/Particles غير مركبة | الاختيارات تعالج على حدة؛ غير متاح واضح، بلا قيمة اصطناعية |
| S07 | R0 180+60، فتح/إغلاق Settings، حفظ/استيراد | صفر crash؛ سجل 8 قنوات/تاريخ/جهاز؛ نجاح persist/reload |
| S08 | مدخلات MQ متباينة/متطابقة | مرشحو Next يطابقون decision؛ 0/1/5/10/12؛ لا legacy144 |
| S09 | نظافة الهواء/حساس واحد/no R0/no source | UNKNOWN أو hypothesis تشخيصية؛ لا ppm أو G3 |
| S10 | تغيير Stable أو R0 أثناء/بعد جلسة | Freeze snapshot؛ تقرير الجلسة القديم مطابق SHA/UID/rank |
| S11 | PDF/XLSX آنية وجلسة وموقع ومختبر | فتح وعرض، RTL وترقيم الصفحة، قرار واحد، ولا تعديل source JSON |
| S12 | عدم اتصال/SQLite full/corrupt/disconnect network | سبب واضح وrollback وحفظ أصلي؛ لا نجاح مفترض |
| S13 | AP/remote/camera/Echo/drive/BME/mic/Flame | لا regressions؛ device bench عند الحاجة |
| S14 | Android 12، شاشة بيضاء/تدوير/تكرار taps | استقرار UI وتباين وactions idempotent |
| S15 | مكتبة 734 جزيئية + أصناف عامة وlegacy aliases | UID صحيح عبر قوائم المواد/التتبع/التقارير |
| S16 | مواد بلا بصمات ومواد ذات عدة منحنيات من أصل واحد | غير موجود/غير مؤهل صريح؛ أصل واحد لا ينشئ أصواتًا مستقلة |

**إثبات الفشل والتصحيح:** لكل Sxx حفظ `fixture`, `original_snapshot_hash`, `firmware_revision`, `steps`, `expected`, `observed`, `root_cause`, `patch_sha`, `CI_run`, `Android/device_run`, `owner_acceptance`.

## 8. ترتيب دفعات التنفيذ والبوابات
1. **A0 (P0):** تثبيت قرارات/SQLite/Router/Adapter، ثم P09.1–P09.3. لا نبدأ بتجميل الواجهة قبل إصلاح مصدر البيانات وملكية التسجيل. سيبقى قسم P06.3/P06.5 العلمي غير المنجز موسومًا pending؛ لا يوقف بناء رابط بحثي لكنه يمنع الادعاءات المؤهلة.
2. **A1:** إصلاح المرشحين وعداد الآني، حل R0 في Settings، تأسيس UID الواحد (P08/P10.2/P11.1)، مع اختبارات 0/1/5/10/12.
3. **A2:** المختبر والمواقع والتتبع وStable والمراجعات وفصل Food Freshness؛ تجميع evidence origin في كل عملية.
4. **A3:** كل التقارير PDF/XLSX والرسوم وتجارب SQLite/Android failure injection، وتحقيق من device bench للريلاي وEcho/BME/الاتصال.
5. **A4:** بناء **APK V8 اختباري بحثي فقط** بعد اجتياز P0 وCI وAPK signature/package/Stable SHA، ثم حل ملاحظات المستخدم؛ **إصدار سلامة/قياس غازات عام ممنوع حتى G3**.

**بوابات الانتقال:**
- `Gate A`: كل P0 + انتقال 14→15 بدون فقد + source Next result واحد + replay synthetic وowner clean-air.
- `Gate B`: دورة 60+60 وreadback وUNKNOWN وfailure reasons مع اختبارات ريلاي، ليست مراجعة UI فقط.
- `Gate C`: PDF/XLSX parity مع `decision_uid` وGHS/ppm محفوظة بمستوى الأدلة.
- `Gate D`: Android compile، اختبارات وحدة/Instrumentation وإعادة تشغيل، ثم APK تجريبي صالح للتحميل على Android 12.
- `Gate E`: بحث G3 ميداني عند توفر أجهزة قياس ومعايير تأهيل؛ ليس شرط إنتاج نسخة تجريبية محددة بأنها غير معتمدة.

## 9. بناء APK وقرار النسخة
- CI على فرع `feature/tank-v8-implementation`، overlay المصدر المعتمد `sohpany/SERVOC-Android` مثبت عند SHA معروف، Java17/Gradle 9.5/SDK36؛ التحقق من Gradle source/signing ثم artifact SHA256 و`com.hazrob.tank` و`versionName=8.0-test` ورقم إصدار أعلى من V7.
- لا تتسبب نسخة الاختبار في إزالة قاعدة التطبيق المثبتة؛ افحص signing lineage وإمكانية الترقية قبل التثبيت وإلا احفظ نسخة احتياطية. لا تعدل نظام firmware أو الشبكة دون إثبات واجهة غير متوافقة.
- يقوم `README` بربط APK SHA ومحرك Next / Stable / firmware revisions والمشكلات المعروفة؛ لا تقرير «مكتمل» بدون مسار artifact + تشغيل اختبارات.

## 10. آلية الاستئناف
- **المصدر الحاكم:** `docs/implementation/V8-NEXT-ANDROID-INTEGRATION-TRACKER-2026-11-02.json`؛ الحالة لكل مهمة `NOT_STARTED/IN_PROGRESS/FAILED/BLOCKED/DONE_VERIFIED`.
- يبدأ P07.1 عند أول تعديل؛ يحدّث السجل بعد نجاح كل مهمة بأمر الاختبار، run URL، SHA، والنتيجة؛ لا تقفز من مخرجات G1 إلى G2/G3.
- عند تعطل الاتصال: اقرأ هذا الملف وCore state الحالي والفرع وCI، ثم أكمل آخر مهمة غير مؤهلة، دون العودة إلى V7 أو إنشاء مصدر مرشحين موازٍ.
