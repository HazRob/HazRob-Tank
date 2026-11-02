# HazRob Tank V8 — خطة تكامل التطبيق بالكامل مع ChemCore Next ومعالجة أعطال V7
**المعرّف:** HZT8-NEXT-ANDROID-INTEGRATION-001  
**التاريخ:** 2026-11-02  
**الفرع المستهدف:** \`feature/tank-v8-implementation\`  
**الحالة:** \`FORENSIC_REVIEW_AND_PLAN_ONLY\` — لا تعني اكتمال أي إصلاح في Android أو تأهيل G3.  
**الاستقلال:** ChemCore Next محرك منفصل (P00–P06)، وهذه **خطة تنفيذ Android مستقلة لاحقة** وفق P07–P13؛ إن إعدادها لا يغيّر بوابة المحرك أو يصرّح إصدار APK، ولا يعدّ تشغيل أدوات Next البحثية داخل التطبيق مؤهلًا لقرار سلامة.  
**المراجع الحاكمة:** \`V8-CHEMCORE-NEXT-MASTER-EXECUTION-PLAN-2026-11-02_AR.md\`، و\`V8-CHEMCORE-NEXT-EXECUTION-STATE-2026-11-02.json\`، و\`CHEMCORE-NEXT-INDEPENDENT-PROJECT-SCOPE-2026-11-02_AR.md\`.  
**معيار التحقق:** G1 اختبار برمجي؛ G2 تأهيل بيانات/مصادر مع حجب مجموعات مستقلة؛ G3 تجارب Tank فعلية مع غاز مرجعي وتركيز مستقل. لا ترقية تلقائية.

## 1. محضر مراجعة ما هو قائم بالفعل

### حقائق مثبتة من الشيفرة الحالية — لا يجوز وصفها بأنها أسباب ميدانية قاطعة دون إعادة إنتاج

| ID | درجة الإثبات | الموضع | الدليل التقني | الأثر المحتمل والإصلاح |
|---|---|---|---|---|
| F01 | **مؤكد شيفريًا** | \`measurement/FieldCycleController.java:307–339\` | بعد 60 ثانية أخذ عينات تُطلب \`requestFieldStop("ready")\`؛ ينتظر البرنامج علم مسجل القياسات ثم يستدعي \`repo.complete\`؛ يوجد timeout ثابت 12 ثانية. | أي تأخر في تفريغ المسجل/تحليل ChemCore/قراءة DB قد يحول الدورة إلى FAILED رغم توفر عينات؛ اعتماد ack موثّق منفصل عن توقف الالتقاط واستكمال المعالجة. |
| F02 | **مؤكد شيفريًا** | \`measurement/TelemetryRecorder.java:124–150\` | تنهي الدالة جلسة \`measurement_sessions\` ثم تحاول \`CompletedSessionChemCoreAnalyzer\`؛ الاستثناء داخل \`catch(Exception ignored)\` ولا ينتقل سبب الفشل إلى دورة القياس. | يظل شرط القرار محفوظًا ناقصًا بلا سبب تشخيصي؛ يلزم حدث نتيجة \`analysis_status\` دائم \`SUCCESS/INSUFFICIENT_EVIDENCE/FAILED\` مع \`error_code\` وعدم ابتلاع الأخطاء. |
| F03 | **مؤكد شيفريًا** | \`field/FieldCycleRepository.java:184–215,419+\` | \`complete\` يشترط ended_ts، samples، 60s، \`hasMeasuredEvidence\`، و\`hasRequiredChemCoreDecision\`؛ الدالة تعيد Boolean فقط. | فشل متطلبات متعددة لا يظهر للمستخدم أي شرط فشل؛ إعادة واجهة \`CompletionValidationResult\` بأكواد أسباب، وعزل مرحلة التسجيل عن التحليل. |
| F04 | **مؤكد شيفريًا** | \`chemcore/TankInstantAnalyzer.java:64–118\` | المخرجات من \`ChemCoreTankV6\` و\`source_curve_hypotheses\`، والربط \`legacyId<144\`، مع dedup حسب UID لكن ترتيب الإدراج مصدره قائمة المنحنيات. | القائمة قد تكون شبه ثابتة أو تفقد مواد خارج legacy؛ استبدال المسار بـ \`ChemCoreNextResult.eligibleRankedCandidates\` المرتبطة بالـ UID الموحد دون فرض عدد 10 أو 144. |
| F05 | **مؤكد شيفريًا** | \`ui/MeasurementsTab.java:858–960\` | بطاقة «فحص 144»، حد عرض 12، \`selectedCandidateId:int\`، وواجهة \`renderResearchOnlyV6\`. | إزالة فحص 144 من ترتيب Next (يمكن الاحتفاظ بأرشيف Legacy للبحث)، وحفظ \`long UID\` وعزل selection عن session revision، واستعمال paging للمرشحين الفعليين. |
| F06 | **مؤكد شيفريًا** | \`ui/ChemCoreAnalysisTab.java:137–210\` | المسار المخبري ما زال ينشئ \`ChemCoreTankV6.Result\` وينقل ميزات مبنية على نسب session ومراحل Recovery. | لا يصح استبدال هذا التحليل جزئيًا دون إعادة ربط source/phase/feature eligibility وحفظ نسخة الأدلة؛ Next adapter أو \`UNKNOWN\` مفسّر. |
| F07 | **مؤكد شيفريًا** | \`ui/SettingsTab.java:75–141\` و\`ui/DeveloperR0Tab.java\` | إنشاء واجهة R0 الديناميكية من الإعدادات مع try/catch عند الفشل؛ العقد يحدد 180s +60s للمعايرة، و60s+60s للقياس. | معرض لمسار فشل فتح المعايرة وتعطيل واجهة الإعدادات؛ شاشة R0 مستقلة مضبوطة lifecycle تُفتح بملاحة واضحة، ولا تتعدد حالات R0 بين Developer/Settings. |
| F08 | **مؤكد شيفريًا** | \`field/FieldSessionRepository.java:236–245\` و\`ui/FieldSessionsTab.java:244–314\` | كشف جداول SQLite المطلوبة، وحماية العرض من الانهيار مع رسالة عامة للخلل. | العرض متين جزئيًا لكن لا يوجد تشخيص تفصيلي لحالة migration على واجهة المسؤول؛ فحوص migration/read-only repair/backup واسترجاع دون حذف. |
| F09 | **مؤكد شيفريًا** | \`report/FieldSessionReportExporter.java:46–68\` و\`ui/ReportsTab.java\` | التصدير يطابق القائمة المعتمدة ويعيد التحقق من evidence digest؛ الشاشات تحتوي صفوف المخططات القديمة بما فيها «فحص 144». | منع إصلاح UI وحده؛ تحديث عقد التقرير والرسوم ليقرأ المصدر المحفوظ نفسه ويحفظ flags ومصدر الامتناع، مع فحص PDF/XLSX الحقيقيين. |
| F10 | **مؤكد شيفريًا** | \`measurement/TelemetryRecorder.java:29–50,81–150\` | التقاط 200ms في الجلسة الميدانية و250ms في المسار المعتاد؛ قد تتكرر نفس دورة MQ بالاستطلاع. | يجب حصر العينات المؤهلة في \`scan_cycle\` فريد لكل حساس، مع التوقيت والحرارة والأصل والكفاية، وليس عدد طلبات HTTP. |
| F11 | **مؤكد شيفريًا** | \`chemcore/CompletedSessionChemCoreAnalyzer.java:126–161\` | تجميع median لمجموعات \`scan_cycle\` المتميزة، وحد أدنى 10 للغاز، مع \`Tank V6\`. | الحفاظ على ميزة إزالة التكرار، واستبدال محرك الاستدلال فقط مع الاحتفاظ بقيم R0 المحفوظة مع العينة؛ لا إعادة حساب قديم بمعايرة جديدة. |
| F12 | **مؤكد شيفريًا** | \`ui/FoodFreshnessTab.java\` | منظومة مستقلة للفواكه واللحوم مع نتائج وتقارير خاصة. | تبقى مستقلة عن نتائج الغاز ChemCore Next، ويمكن إعادة استعمال طبقة الجمع الخام فقط بعد إثبات الفصل. |

**المحددات:** لا يوجد سجل crash/stacktrace أو إعادة إنتاج جهاز V7 في هذه المراجعة. F01–F12 تشير إلى **مسارات كود وفجوات تعاقدية مثبتة**، وليست نسبة إرجاع كل حادثة فشل ميدانية إلى سبب محدد. يجب إغلاق السبب الجذري بإعادة إنتاج instrumented وsnapshot/audit/log.

## 2. المعمارية المرجعية الوحيدة للتطبيق

\`\`\`text
ESP32 Tank (API4; لا موافقة نسخ إلزامية، لكن capability/contract must validate)
    + ESP32-CAM [مسار صورة مستقل]
     │ /diag + /data + relay/mq power + measurement/cycle IDs
     ▼
TelemetryTransport → SourceClock/MonotonicTimestamp → CanonicalRawReading
     │    الجهاز | firmware/source-definition | sensor UID | scan_cycle | ADC/V/Rs
     │    heater phase | temperature/RH | BME | physical/particle availability
     ▼
RawCaptureStore (SQLite transaction + append-only + checksum)
     ├─ CalibrationLedger: current R_air; historic legacy R0; optional qualified R0_source
     ├─ SignalQuality (missing/invalid/stale/powered/noise/cycle dedup)
     ├─ MeasurementCoordinator [SINGLE OWNER FIELD/INSTANT/LAB/CALIBRATION/FOOD]
     └─ CanonicalMeasurementSnapshot immutable + source/calibration manifest/SHA
               ▼
        NextAndroidAdapter / Research feature flag
               ▼
      ChemCore Next P01–P06 result:
      source curves + fingerprints + temporal + environment + ML research
         → independence/correlation graph → arbitration → UNKNOWN or eligible list
         → separate quantitatively qualified source result / safety domain
               ▼
    ResultStore (atomic persistence; analysis status; source_root; version)
               │ resultId + snapshotId + evidenceSha + modelManifestSha
        ┌──────┼─────────┬────────────┬──────────────┬────────────┐
        ▼      ▼         ▼            ▼              ▼            ▼
   Instant    Candidate  Field       Site          Lab/Dev      PDF/XLSX
   UI+timer   dynamic    Session     Aggregation   & experiments visual reports
   all read EXACT persisted result and immutable snapshot by IDs
\`\`\`

**عقود عدم التداخل:** مصدر الحقيقة ليس \`host.latestDiagnostics()\` عند عرض التقرير؛ يستخدم \`captureId\` و\`resultId\` المحفوظين فقط. المؤقت لا يعلن نجاحًا ولا يطفئ الريلاي قبل إثبات حفظ العينات والتحليل أو إرساء فشل مُسجّل؛ السلامة الكهربائية تتطلب مؤقت إيقاف أقصى منفصلًا مع تسجيل سبب انتهاء غير مكتمل. لا يرتب أي مكوّن UI بيانات Legacy مباشرة. لا يوجد مصدر ثانٍ لقائمة المرشحين.

### عقود البيانات المقترحة (تعريفات مستقلة عن التنفيذ)
- \`AcquisitionCommand\`: \`operationId\`, \`ownerMode\` (\`INSTANT/FIELD/LAB/CALIBRATION/FOOD\`), \`ownerSessionId\`, \`deviceId\`, \`requestedFamilies\`, \`warmupMs\`, \`samplingMs\`, \`startedElapsedMs\`, \`deadlineMs\`.
- \`SampleFrame\`: \`operationId\`, \`sampleId\`, \`UTC\`, \`monotonicElapsed\`, \`sensorUid\`, \`scanCycleId\`, \`adcRaw\`, \`voltage\`, \`rsKohm\`, \`rAirKohm\`, \`r0SourceKohm\` إن كان مؤهلًا، \`definition\`, \`heaterPhase\`, \`quality\`, \`T/RH\`, \`payloadSha\`. \`scanCycleId\` اختياري عند العدم مع \`NOT_EVALUABLE\` واضح، لا يختلق.
- \`StoredCapture\`: immutable frame IDs + \`captureId\` + \`deviceId\` + \`calibrationId\` + \`manifestSha\` + \`selectedFamilies\` + counts by unique scan/sensor + \`captureSha\`.
- \`AnalysisResult\`: \`resultId\` + \`captureId\` + \`engineKind\` + \`engineVersion\` + \`modelManifestSha\` + \`decisionStatus\` + \`eligibleCandidates[]\` + \`excluded/unknownEvidence[]\` + \`perSensorResiduals[]\` + \`sourceRoots[]\` + \`ppmAuthorization\` + \`safetyAuthorization\` + \`createdAt\` + \`analysisFailureCode\`.
- \`ReportEnvelope\`: \`reportId\` + source \`captureId,resultId,siteId,fieldSessionId,approvedCycleIds\` + \`sourceDigest\` + formatting version + exported \`fileSha\`; تسلسل موافقة منفصل.
- \`SystemStatus\`: \`OFFLINE/NO_SENSORS/WAIT_HEATER/CAPTURING/FINALIZING/UNKNOWN/RESEARCH_ONLY/QUALIFIED\`; لا تخلط NETWORK_CONNECTED مع SENSOR_VALID أو GAS_DETECTED.
- \`ChemCore Next\` و\`ChemCore V6\` لا يتبادلان كائن \`Candidate\` القديم: واجهة Adapter صريحة، حالة fallback موثقة، وVersion-sealed archival reads. \`SOURCE_ONLY\` لا يتحول إلى \`G3\` عند التسلسل.
- **تحديث \`Stable\`**: staging/validate/source manifest/atomically activate/rollback، وتحديث أعلام البصمة والمنحنى وPPM وM4 لجميع المواد المرتبطة (UID) دون إعادة كتابة نتائج تاريخية أو معايرة R0. \`Stable\` مكتبة بيانات لا عقد أجهزة.

## 3. دورة الجلسة الحتمية وإغلاق مشكلة الفشل

**دورة جديدة محددة بالحالات التالية** (state machine persisted in SQLite):  
\`CREATED → DEVICE_VERIFIED → RELAY_REQUESTED → RELAY_CONFIRMED → WARMUP(60s) → CAPTURE_REQUESTED → CAPTURE_ACKED → SAMPLING(60s from first valid stored sample) → STOP_REQUESTED → RECORDER_FINALIZED → RAW_FLUSHED → READBACK_VERIFIED → ANALYSIS_PERSISTED/ANALYSIS_INSUFFICIENT → REVIEW_READY → APPROVED\`.  

في جميع المراحل: \`FAILED(code)/INTERRUPTED/RETRY_NEW_ATTEMPT\` مسارات نهائية موثقة، مع watchdog/relay-off مؤكد. الحالات الانتقالية لا تعتمد على Handler/Activity كمالك وحيد؛ تستند إلى \`operationId\` ودورة persistent وعقد ACK. **المشكلة ليست فقط أن ينتهي العداد؛ يجب أن تنتهي البيانات.**

1. **حجز واحد:** Atomic acquisition coordinator؛ يمنع تزامن R0/Instant/Field/Lab/Food، ويفصل camera/drive telemetry عن امتلاك دورة MQ. فحص \`deviceId\`, \`relayStatus\`, \`firmware\` capability قبل البدء.
2. **تشغيل الحساسات:** إرسال الأمر، ثم قراءة ACK + قراءة حالة ESP32 الفعلية + TTL؛ لا تستنتج النجاح من HTTP 200. قاعدة إنذارات فقد الاتصال تعمل باستقلال حتى عند اشتغال الريلاي.
3. **الإحماء:** جلسة/Instant 60 ثانية *تشغيلية مشروطة* بالجاهزية الحقيقية؛ R0 التلقائية 180+60 ثانية. لا يُفترض أن الإحماء 60s يغني عن متطلبات دورة سخان MQ7/MQ9 أو عن زمن التشغيل الأولي. عرض countdown من monotonic clock مع TTL.
4. **تسجيل العينة:** طلب \`captureStart\` ثم انتظار فتح \`measurement_sessions\` وربطه فورًا بـ\`field_measurement_cycles\`؛ بداية نافذة 60s من أول scan موثق بعد ACK لا من مجرد الضغط. يحتفظ كل poll بالـ \`scan_cycle\` ويحذف التكرار حسابيًا لا من الخام.
5. **الإيقاف:** \`stop requested\` يرسل إلى recorder واحد، ثم \`RecorderFinalized\` typed ack بعد INSERT/UPDATE transaction وقراءة \`ended_ts\` وchecksum وعدّ العينات المتميزة. لا تعتمد على \`FIELD_CAPTURE==false\` كمساوٍ لحفظ القراءات.
6. **التحليل:** تحليل \`StoredCapture\` على خلفية التطبيق؛ timeout مستقل ومبرر، ونتيجة صريحة: \`SUCCESS\` أو \`INSUFFICIENT_EVIDENCE\` (صالح كحالة محفوظة، لا مرشح مختلق) أو \`FAILED\`. من الخطأ اشتراط نجاح تعريف الغاز لقبول **حفظ قياس مكتمل**؛ معيار جودة الاكتساب منفصل عن جودة الاستنتاج.
7. **الاعتماد:** الحفظ والتحليل يتضمنان readback ثم \`REVIEW_READY\`؛ أزرار \`اعتماد/إعادة/رفض\` لا تظهر في وضع \`FINALIZING\`. الموافقة تحمل \`approvedAt\` وchecksum وسياق المستخدم، وإعادة القياس تنشئ attempt جديدًا ولا تمحو السابق.
8. **الريلاي:** طبيعيًا يُطفأ بعد \`RAW_FLUSHED+READBACK_VERIFIED\` وحسم حالة التحليل أو تسجيل فشله؛ وضع timeout/physical emergency يؤدي إلى فصل آمن مع \`POWER_STOP_FORCED_BEFORE_FINALIZATION\` وسجل خسارة البيانات، ولا يصور الدورة على أنها ناجحة.
9. **الاسترجاع:** عند process death/reconnect أو إغلاق Activity: enumerate unresolved operations؛ لا \`READY\` افتراضي ولا حذف سجل سابق؛ استرجاع session إن كان محفوظًا، وإلا \`INTERRUPTED\` مع السبب، إطفاء الريلاي وفق سياسة fail-safe.
10. **منع السباقات:** serial executor للحالة، token monotonic \`generation\`, CAS state transition وidempotency \`operationId\`, circuit breaker لخسارة الاتصال، لا polling tasks متراكبة. النقل والـ DB بالـ IO، UI observers على main thread فقط.
11. **رسائل الفشل:** \`DEVICE_ID_CHANGED, RELAY_NOT_ACKED, CAPTURE_START_TIMEOUT, NO_FRESH_SCAN_CYCLE, STALE_TELEMETRY, FW_MEASUREMENT_BUSY, MISSING_RAW, DB_WRITE_FAIL, READBACK_HASH_MISMATCH, ANALYSIS_FAILED, INSUFFICIENT_EVIDENCE, POWER_STOP_FORCED, SESSION_MIGRATION_INCOMPLETE\`. الرسالة للمستخدم بالعربية + code قابل للتتبع في سجل التشخيص.
12. **التصحيح الخاص:** لا ينبغي لـ\`repo.complete\` أن يرد Boolean فقط؛ واجهة \`CompletionValidationResult\` قائمة من شروط محددة و\`failingCondition\`. لا تبتلع \`TelemetryRecorder.finish\` فشل تحليل الغاز ولا تبقي الجلسة في حالة غير قابلة للاعتماد.

## 4. مصفوفة الربط بين أجزاء التطبيق — تعديل مع قبول

| الوحدة/الواجهة | ملفات الربط الأولى | عقد Next | معيار القبول |
|---|---|---|---|
| الاتصال/تحكم الروبوت/الكاميرا | \`HazRobHost\`, \`RobotApi\`, connectivity diagnostics | حفظ deviceId/health/ACK، كاميرا مستقلة | فصل camera failure عن فشل الغاز؛ القيادة لا تتأثر بتحليل PDF |
| بطاقة المستشعرات والقياسات الفيزيائية | \`MeasurementsTab\`, physical policies | sensor capability/quality + real BME/mic/Echo | غياب PM/O2 لا يُعرض صفراً؛ Echo warning ليس قياس غاز |
| القياس الآني | \`MeasurementsTab\`, \`TankInstantAnalyzer\` | one-shot \`StoredCapture\` ثم \`AnalysisResult\` | عداد 60+60 حقيقي + نجاح القراءة + cancel/retry + resultId محفوظ |
| المرشحون الديناميكيون | \`TankInstantAnalyzer\`, \`MeasurementsTab.AnalysisResultsPane\` | \`eligibleRankedCandidates\`, canonical \`long UID\` | 0/1/5/10/أكثر + مادة UID>144 + تغير مصدر القراءة → تغير النتيجة، دون list padding أو legacy reorder |
| محرك Next الموحّد | \`ChemCoreNext\`, \`ResearchFingerprintBundle\` | \`NextAndroidAdapter\`, feature flags | كل expert مشروط، source-root dedup، mismatch → UNKNOWN، لا افتراض G3 |
| الجلسات/المواقع | \`FieldSessionsTab\`, \`FieldCycleController\`, \`FieldCycleRepository\` | persistent operation + source result | انقطاع أثناء التسخين/التسجيل/التحليل؛ قبول صحيح وسبب فشل تشخيصي |
| قاعدة العينات | \`TelemetryRecorder\`, \`MeasurementSessionRegistry\`, \`ExperimentsDb\` | \`Frame/StoredCapture\` | scan_cycle dedup، atomic write/readback، partial/failure flags، offline/restart |
| نتائج الجلسة | \`CompletedSessionChemCoreAnalyzer\`, \`ChemCoreDecisionRepository\` | result binding via \`captureId\` | لا اختلاف بين final decision والتقرير وUI؛ تحليل ضعيف ≠ فشل حفظ |
| المختبر والتجارب | \`ChemCoreAnalysisTab\`, \`DeveloperExperimentsTab\` | separate lab protocol/source exposure/feedback | baseline, exposure, recovery، 3D/4D من series حقيقية فقط، لا استبدال قياس مرجعي |
| معايرة R0 | \`SettingsTab\`, \`DeveloperR0Tab\`, \`ManualR0Calibration\` | device \`R_air\` vs source \`R0\` وsource K | شاشة واحدة مستقلة؛ R0 تلقائي 180+60؛ إدخال يدوي مميز؛ استيراد/حفظ/استرجاع دون crash |
| المواد والبحث والتتبع | \`ChemicalRepository\`, status repos, unified UIDs | single master catalog/Stable capability | لا مرشح legacy مصطنع؛ search all canonical + حالة fingerprint/curve/PPM/M4 محدثة |
| المخططات والبيانات | \`ReportsTab\`, charts code | stored raw time series, labels + eligibility | plot actual stored values only، no temporal chart from one reading |
| التقارير | six V7 report writers, \`FieldSessionReportExporter\` | \`ReportEnvelope\` same \`resultId\` | PDF/XLSX/archive/render parity، citations/provenance، Arabic RTL، all approved sessions |
| نظارة الأغذية | \`FoodFreshnessTab\` + fruit/meat engines | separate food reference and result type | لا دمج مع كتالوج غازات Next أو اسم مرشح غاز كحكم جودة لحم/فاكهة |
| المطور/التشخيص | \`DeveloperTab\`, diagnostics, R0, experiments | source-level logs, replay, gated simulation | عرض cause codes وشواهد الفشل؛ simulation لا تُخلط مع field |
| الأمان والتحذير | \`GasAlertCoordinator\`, safety controls | independent threshold/sensor-qualified domain | لا تحذير مهني GHS/OEL/LEL من UNKNOWN أو research ppm، إنذارات الجهاز الآمنة مستقلة |

### المرشحون — شروط حاسمة
- \`candidateCount\` يساوي عدد الهوية المستحقة **من الأدلة الحالية**. لا يساوي عدد المنحنيات ولا عدد المواد المتاحة ولا عشرة.
- تتجدد القائمة عند **تغير \`resultId\`/مقتطف evidence generation**، لا عند مجرد redraw؛ كل مصدر مرشح يحتفظ بـ\`sourceRoot\`, \`sensorSet\`, \`concentrationRange\`, \`decisionReason\`, \`research/confirmed\`.
- فصل ثلاثة مفاهيم في UI: **بحث في قاعدة المواد** (جميع المواد)، **مرشحو القراءة** (قد يساوون صفرًا)، **مادة مختارة للتتبع اليدوي** (اختيار المستخدم ليس إثبات رصد). عرض \`UNKNOWN\` صراحة.
- السماح \`long canonical UID\` بلا تحويل \`int\`، استعمال recycler/paging ديناميكي وبناء adapters على ID ثابت، وإبطال cache حين تتغير calibration/source library/measurement.
- \`source_root\` غير مستقل إذا نشأت البصمات من نفس سجل الدراسة؛ تنوع عدد الحساسات/تقنيات 3D/4D لا يعطي أصواتًا مستقلة إضافية.

## 5. عقد التقارير الستة وربط كل رقم بدليل واحد
| الرمز | التقرير الحالي | مصدر البيانات الملزم | إثبات الجودة |
|---|---|---|---|
| R01 | التقرير الآني للغازات \`TankInstantV7PdfWriter\` | immutable instant \`resultId+captureId\` | candidates/UNKNOWN/evidence/version/R0 provenance + no fake time-series |
| R02 | التقرير الفيزيائي \`V7PhysicalReportPdfWriter\` | BME/mic/installed physical sensors | missing vs zero vs threshold وبيانات ووقت ومصدر كل وحدة |
| R03 | تقرير الجلسة الميدانية \`V7FieldSessionPdfWriter\` | approved cycle readback + source hash + analysis | كل دورة معتمدة مرئية، sample count/quality، fail ledger لا يُحذف |
| R04 | تقرير ملخص الموقع \`V7SiteVisualPdfWriter\` | جميع جلسات الموقع المعتمدة | تجميع دون استنتاج هوية غاز بين الجلسات، counts/units صحيحة |
| R05 | تقرير نظارة الفواكه \`V7FruitReportPdfWriter\` | fruit engine/source mode | مراجع الفواكه + صور أصلية متاحة فعلاً + مستقل عن gas Next |
| R06 | تقرير نظارة اللحوم \`V7BeefResearchPdfWriter\` | meat engine/source mode | مراجع اللحوم، classes/uncertainty، لا تعميم خارج الأصل |
| XLSX | تصدير الجلسة والموقع | نفس approved capture + \`scan_cycle\` | أوراق raw MQ/ADS إذا متوفرة فعليًا، readings/warmup/decision/gas evidence/source hash، ومطابقة PDF |

**قاعدة التقرير:** لا يُسمح لمولد PDF/XLSX بإعادة تشغيل محرك آخر أو استعمال \`latestDiagnostics\` عند التصدير؛ كل قيمة من \`ReportEnvelope\`. عدم توفر raw ADS يعرض \`NOT_CAPTURED_BY_FIRMWARE\` بدل قيم مستنتجة. تدقيق صفحة بصفحة عبر Android native PDF render/emulator، واختبارات Arabic RTL/contrast/landscape/tables/embedded charts/large candidate lists/long location names/Unicode.

## 6. خطة التنفيذ المتسلسلة وربطها بمراحل P07–P13

> **الشرط السابق:** يظل P06 مستقلًا. حالة G3 الحالية صفر؛ لا يترتب على P06 البحثي تشغيل Next كجهاز تعريف أو سلامة. بدء Android research integration لا يعني قبول النموذج علميًا.

### P07 — عقد البيانات والربط والتحويل (أربع حزم)
- **P07.1** مخطط \`NextAndroidAdapter\` من API4/ADS raw → canonical snapshot؛ schema/provenance/units/calibration; مقارنة byte-for-byte مع المصدر؛ لا يتطلب مصادقة نسخة فريموير قديمة.
- **P07.2** \`ExperimentsDb\` migration additive؛ جداول \`acquisition_operations, raw_frames, calibration_history, analysis_results, result_candidates, source_evidence, report_envelopes\` أو تعزيز القائم دون تكرار بلا ضرورة. اختبار V6/V7 historical backup→V8، قيود FK وunique operationId.
- **P07.3** feature flag \`V6_ARCHIVED, NEXT_RESEARCH, NEXT_QUALIFIED_ONLY\`؛ عدم fallback الصامت من Next إلى V6. أي فشل يبحث عنه عبر \`error_code\`; rollback لا يغيّر أرشيف القياسات.
- **P07.4** repository/observable result لواجهات التطبيق والتقارير؛ اختبار اثنين متزامنين/عملية قتل/إغلاق شاشة/rollback/read-only replay.

### P08 — القياس الآني والمرشحون (خمس حزم)
- **P08.1** تشغيل 60s إحماء و60s تسجيل مع UI timer مصدره state DB؛ capture complete ACK ثم status. الحالات غير الصالحة تُفسَّر، لا نجاح كاذب.
- **P08.2** التخلص من \`legacyId<144\` في المسار النشط؛ generator يحترم كامل canonical UIDs وأدلة source roots؛ عدد قابل للتغير 0/1/5/10/12/40، \`UNKNOWN\`.
- **P08.3** صفحة المرشحين تعرض signal quality, proven/non-proven chemistry, evidence conflicts, fingerprint bundles, source confidence **غير إحصائي**، وper-sensor contribution.
- **P08.4** report R01 من نفس \`resultId\` وتجميد المصدر، مع chart eligibility.
- **P08.5** اختيار مادة وتتبعها لمدة جلسة أو عدة جلسات مع حفظ target UID وfeedback منفصل عن ناتج الكشف.

### P09 — الجلسات والمواقع ونظام القياس (خمس حزم؛ أعلى خطورة)
- **P09.1** استبدال تسلسل الإنهاء المزدوج بعقد persisted state/ACK/atomic raw flush/readback؛ تعديلات \`FieldCycleController/TelemetryRecorder/FieldCycleRepository\`.
- **P09.2** full validation typed \`CompletionValidationResult\`، ومالك واحد لعملية القياس؛ \`retry\` immutable + \`failure codes\`؛ fail-safe power-off.
- **P09.3** تشغيل Next على session saved full series، feature extraction temporal إن وجدت بيانات كافية، وإرجاع \`INSUFFICIENT_EVIDENCE\` بدل فشل حفظ القياس.
- **P09.4** site/session aggregation وتطابق archived decision/report، مرجع source snapshot ثابت لكل دورة.
- **P09.5** gas/physical/particle availability masks: 2/3 اختيارات افتراضية غاز+فيزيائي؛ PM/O2 غير مركبة غير صالحة ولا تمثل صفر.

### P10 — المعايرة والمختبر والتجارب (خمس حزم)
- **P10.1** اعادة ربط \`ChemCoreAnalysisTab\` بالـ Next research sources؛ أدوات lab لها \`exposureKnown\`/blank/candidate labels دون ترقية مرجع غير معروف.
- **P10.2** إلغاء تكرار controller R0؛ شاشة calibration واحدة مستقلة lifecycle، \`R_air\` مدته 180+60 مثبتة، manual R0 إدخال مستقل عن timer، فرق R0_source/K يُعرض.
- **P10.3** معالجة source sensor heaters MQ7/MQ9 and signal quality؛ Lab diagnostics/source residual و 3D/4D المشتقة من سلسلة زمنية فعلية.
- **P10.4** تجربة أكتوبر الإيثانول والدخان، CH4 Li-Cor، UCI MQ135، raw ADC smoke — replay مع نفس المصدر وتسجيل الانحراف/unknown عبر Next research.
- **P10.5** developer/test simulations مميزة بوضوح عن التجارب الفيزيائية؛ جلسات الباحث/المستخدم معزولة، لا تلويث قاعدة المراجع.

### P11 — قاعدة المواد وStable (خمس حزم)
- **P11.1** master chemicals single UID+CAS+aliases؛ تحديث كافة القوائم بما فيها lab/candidate/tracking.
- **P11.2** Stable read/query update manifest/provenance, staging، rollback، offline default، لا تعديل مُعرفات تاريخية.
- **P11.3** update coverage flags fingerprint/curve/PPM/M4 and source mapping for all catalog rows بعد atomic activation.
- **P11.4** GHS/OEL/PEL/LEL/UEL/natural range مصدر مفصول وتاريخ/وحدات، ولا استنتاج تجاوز من research-only ppm.
- **P11.5** اختبارات DB row migration/duplicates/UID>144/class smoke-LPG/no CAS وأخطاء الإنترنت وعدم فقد calibration.

### P12 — التقارير والقبول الوظيفي (خمس حزم)
- **P12.1** R01/R02 exact source binding and real native PDF render.
- **P12.2** R03 + XLSX multi-session raw/ADS and tamper/readback.
- **P12.3** R04 site aggregation + export checksums.
- **P12.4** R05/R06 food freshness distinct route/source and print QA.
- **P12.5** integration regression: menus, Android 12, process death, UI non-blocking, database, field/candidate/instant/report consistency, accessibility.

### P13 — الجهاز الحقيقي والبناء (خمس حزم)
- **P13.1** جهاز Tank: firmware API/relay/R0/ADS scan-cycle/PM actual/camera/network fault injection؛ لا تحوّل 60s إلى فرضية معدل عينات وهمية.
- **P13.2** مجموعة تجارب غاز حقيقية G3 منفصلة بمعايرة مرجعية (ethanol/smoke alone and mixtures, CH4, blank) وT/RH ومعايرة مصدر لكل جهاز؛ التقنيات غير المؤهلة تظل research only.
- **P13.3** Android build debug + release signed reproducibility and SHA، permissions/Android 12/ANR/memory/CPU.
- **P13.4** staged internal RC8 QA من بيانات V7 محفوظة، تقارير أصلية، منع regressions؛ لا APK مع عطل حرج مفتوح.
- **P13.5** release candidate handoff فقط عند تحقق جميع البوابات؛ README/commit/schema/manifest + rollback + evidence dossier. يظل إصدار إنتاجي معتمد لغاز/ppm محظورًا دون G3.

## 7. بوابات قبول صارمة — اختبارات لا يكفي فيها «فتح الشاشة»

**GATE-A / Source & schema:** baseline versions/source SHA + fixture device provenance؛ 0 type coercion؛ لا legacy UID mapping خاطئ؛ schema migration preserves V6 and V7 records with source hashes.  
**GATE-B / Acquisition:** 60s/60s real, R0 180s/60s; no false ready, relay state attested, 10+ independently distinguishable firmware scan cycles per requested gas where raw exists; selected physical/particle have valid measured records; UI timer not source of truth.  
**GATE-C / Fail-safe:** 15 fault-injection cases + device reconnect, relay stuck, SQLite writes blocked, Android lifecycle, duplicate events, ANALYSIS_FAILED, loss network, restart; never falsely APPROVED, never leave relay indefinitely on.  
**GATE-D / Candidate:** 0/1/5/10/12 and UID>144 + NO SOURCE, signal/noise, known/source-independent holdings, stale results, source update without relabel historical data.  
**GATE-E / UI consistency:** saved \`resultId+snapshotId+sourceManifest\` **identical** across Instant, FieldSessions, Reports, Lab and export; no static cached candidate list.  
**GATE-F / Reports:** six actual V7-style native PDFs plus site/session XLSX, source-to-page/data parity, Arabic RTL, original chart data, missing sensor and research-only disclaimers, sha integrity.  
**GATE-G / Release:** Android build & emulator real tests; all GATE-A..F passing; firmware tests if changed; source CI green; G3 separate; signed RC8 distribution only after acceptance.

**اختبارات الإخفاق المحددة:** بدء بدون اتصال؛ تبديل device_id خلال WARMUP؛ توقف الريلاي منتصف SAMPLING؛ فقد telemetry 7s؛ response مضاعف بنفس scan_cycle؛ رجوع الوقت؛ استمرار CAPTURE_ACK بدون أول sample؛ إطفاء الهاتف في WARMUP؛ kill process أثناء finalization؛ DB full أثناء INSERT؛ SQLITE migration ناقص؛ clean air response <noise؛ فقد R_air؛ heater MQ7/9 غير مطابق؛ source manifest تالف؛ Stable update rollback؛ duplicate approval/retry؛ 0 و13 مرشحًا؛ legacyId متوافق مع uid مختلف؛ تغيير اختيار الموقع بعد lazy refresh؛ صادرات متعددة بعد rotate/background؛ PDF برقم جلسة آخر؛ فشل source evidence hash؛ food capture reference not found؛ noninstalled PM selection؛ اختيار التتبع اليدوي لـ UID>INT_MAX.

### اختبارات مستخدم نهائية يجب تنفيذها على الجهاز الفعلي
1. إنشاء موقع وفتح جلسة، اختيار «غازات + فيزيائية» تلقائيًا؛ تنفيذ 60+60، تسجيل 60s raw متكرر مع unique scan IDs؛ مراجعة ثم اعتماد؛ إعادة فتح التطبيق؛ قراءة نفس \`captureId\`؛ إصدار PDF وExcel مطابقين.
2. تكرار مع مقاطعة الاتصال بعد 40s وتحقق أن الدورة \`FAILED\` بسبب محدد ويمكن إعادة قياسها **دون محو الأصل**.
3. تنفيذ Instant → قائمة المرشحين → اختيار UID خارج 144 → تتبع يدوي → تقرير فوري → عودة؛ تحرك القائمة حسب snapshot/quality، لا قائمة محفوظة مسبقًا.
4. تغيير R0 في صفحة مخصصة، ثم قراءة تقرير قديم: قيمه ومصدره ثابتان؛ قراءة جديدة لها calibrationId مختلف. محاكاة heater mismatch وclean-air noise يؤدي إلى \`UNKNOWN/NOT_EVALUABLE\`، لا ppm معتمد.
5. توليد التقارير الستة الخاصة بالتطبيق **كما يكتبها Native Android** والتأكد من التطابق البصري والبياني؛ الغذاء لا يستخدم ChemCore الغاز.
6. إعادة تشغيل جهاز/تطبيق مع فقد الطاقة أثناء جلسة ومراقبة \`relay off\`, recovery state, audit log, report exclusion.

## 8. تتبع التنفيذ والإغلاق
لكل مهمة: \`task_id, status, source_files, commit_sha, workflow_run_id, test_fixtures_sha, test_result_artifact, reviewer_acceptance, blocker, next_action\`.  
تحوّل \`PLANNED → CODE_COMPLETE → TESTED_G1 → INTEGRATION_PASSED → DEVICE_PASSED → ACCEPTED\` فقط عبر دليل تشغيل حقيقي. لا نقبل كلمة «تم» نتيجةً لبناء الشيفرة وحدها.  
عند كل دفعة تغييرات: تشغيل local unit + connected GitHub Actions، ومقارنة واجهات الملفات وتقرير DB/QA، ثم تحديث tracker وحفظ حد الإصدار.  
**لا نغيّر الآن V6 Stable ولا firmware ولا تاريخ R0 ولا APK ولا نرقّي G3**. الخطة مُعدة للاستئناف، والبدء البرمجي يظل مهمة مستقلة بعد اكتمال/تحديد قيود بوابة P06 العلمية.
