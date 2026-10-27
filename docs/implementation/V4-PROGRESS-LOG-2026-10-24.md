# HazRob Tank V4 — سجل الإنجاز ونقطة الاستئناف

**أنشئ:** 2026-10-24  
**حالة التنفيذ عند الإنشاء:** PLAN_PREPARED / IMPLEMENTATION_BRANCH_ALREADY_EXISTS / LATEST_VISIBLE_CI_FAILED / DEVICE_NOT_VERIFIED  
**المستودع:** `HazRob/HazRob-Tank`  
**فرع التنفيذ الذي يجب مواصلة العمل عليه:** `feature/tank-v4-implementation`  
**فرع المتطلبات المرجعية:** `develop`  
**مرجع الخطة:** [V4-EXECUTION-PLAN-2026-10-24.md](V4-EXECUTION-PLAN-2026-10-24.md)  
**قائمة المتطلبات ذات المعرفات المرجعية:** [V4-UPDATE-BACKLOG-2026-10-24.md](../requirements/V4-UPDATE-BACKLOG-2026-10-24.md)

## Checkpoint 00 — 2026-10-24 — تنظيم المتطلبات فقط

### ما تم في هذه المهمة
- جرى **توحيد** متطلبات V4 الأصلية وجميع الإضافات المسجلة على فرع التنفيذ: **15 بنداً (V4-001 إلى V4-015)**. صُححت ازدواجية أرقام V4-011/012/013/014 عبر خريطة إحالات تحفظ مراجع commit القديمة ولا تفقد أي طلب مستخدم.
- تقرر ترتيب التنفيذ بحسب الاعتماد والخطر: **تدقيق البناء → إصلاح انهيار جلسات P0 → وضوح القوائم → شريط/تبويبات/إصدارات → عملية قياس آنية واحدة → ChemCore → مخططات → PDF → التحديث/رفع نتائج المطور → الملفات الثلاثة والقبول النهائي**.
- ثُبّت قرار **القياس الآني عملية واحدة بلا سجل لقطات** (V4-010 حاكم V4-006/007/011)، مع إبقاء جلسات المواقع وأرشيفها مستقلاً، وعدم حذف سجلات V3 القديمة أثناء الترقية.
- ثُبّتت قاعدة عدم تقييد تشغيل خوارزميات PPM المتاحة بسبب عدم وجود اعتماد مرجعي؛ يجب إظهار النواتج التقديرية بمصدرها.
- دراسة دمج `ChemCore` و`المرشحون` (V4-012) **غير موافق عليها لتنفيذ الدمج**؛ الاحتفاظ بهما منفصلين هو السلوك الافتراضي.
- وثائق الخطة وسجل الإنجاز موضوعة على `develop` ومكررة على فرع V4؛ **لم تُنفذ أي تغييرات برمجية في هذه المحادثة**.

### حالة المصدر الموجودة سلفاً قبل بدء الخطة
- كان هناك فعلاً فرع `feature/tank-v4-implementation` على GitHub، وآخر HEAD شوهد قبل إنشاء الوثائق: `22317a5aeac0242cfc2289bde39250ad6cc86d85` (تعديل تباين قائمة مخططات التقارير).
- عُثر على تغييرات مسبقة في `FieldSessionsTab` لحماية Spinner/SQLite، ومخططات ChemCore الحية وتوحيد بعض ألوان/حدود القوائم، مع اختبارات `TankV4SessionsCrashGuardTest` و`TankV4InstantChartsContractTest`. **لا تعِد تنفيذها عميانياً**؛ افحصها واختبرها وأصلحها حسب الفشل الحقيقي.
- عرض GitHub Actions أحدث تشغيل Android V4 رقم [40047526874](https://github.com/HazRob/HazRob-Tank/actions/runs/40047526874) بنتيجة **failure**، وكذلك عدة تشغيلات قبله. محاولة جلب سجل الوظيفة أعادت 404 من تخزين GitHub المؤقت، ولذلك **لم يُحدد سبب فشل الاختبار بعد**. يلزم إعادة تشغيل بناء يمكن قراءة سجلاته أو تشغيل Gradle في بيئة مناسبة؛ لا تخمّن العلة.
- **لم يُثبت** اختبار إصلاح سقوط `الجلسات` على الهاتف. ولم يُثبت إخراج V4 APK موقّع دائماً أو تفليش ESP32/CAM ولا ملفات إطلاق V4 مستقلة. كل ذلك `PENDING` وليس `PASS`.
- V3 الأصلي `feature/tank-v3-implementation` قائم وموثق؛ وكذلك `sohpany/SERVOC-Android` مستقل ولا ينبغي تغييره.

### خريطة تصحيح أرقام المتطلبات
- `V4-011` على فرع V4 القديم (ChemCore Stable) = **V4-013** في السجل الموحد.
- `V4-012` القديم (Echo/صوت/قوائم المطور) = **V4-014**.
- `V4-013` القديم (انهيار الجلسات) = **V4-015**.
- `V4-014` القديم (مخططات ChemCore) = **V4-011**، مع الحفاظ على 20 مخطط تقارير.
- `V4-012` المرجعي الجديد = **دراسة دمج تبويبي ChemCore والمرشحين**، تنفيذ الدمج يحتاج قرار المستخدم.

### حالة التقدم المبدئية

| الدفعة | البنود | الحالة | الدليل / العائق |
| --- | --- | --- | --- |
| 00 | كافة V4 | **PLAN_ONLY_DONE** | وثائق موحدة وخطة مفصلة؛ فرع V4 موجود مسبقاً |
| 01 | V4-015 | **SOURCE_PRESENT / CI_FAIL / DEVICE_PENDING** | اختبارات حماية crash موجودة، CI فشل، لا Logcat هاتف |
| 02 | V4-009/002/014 | **SOURCE_PRESENT / NOT_ACCEPTED** | تغييرات واجهة موجودة، تباين فعلي وEcho/Noise غير معتمدين |
| 03 | V4-003/004/005 | **AUDIT_REQUIRED** | يلزم فحص اختلاف الفهرسة والنسخ والشريط وحالة الاتصال |
| 04 | V4-010 | **AUDIT_REQUIRED** | آلة حالات قياس واحد بلا محفوظات هي شرط المستخدم |
| 05 | V4-013/007 | **AUDIT_REQUIRED** | نسخة Stable ومصدر نتيجة ChemCore لم يُعتمدا |
| 06 | V4-011 | **SOURCE_PRESENT / CI_FAIL** | قائمة مخططات ChemCore الحية موثقة بالشيفرة، نجاح CI غير مثبت |
| 07 | V4-006 | **NOT_ACCEPTED** | تقرير العملية الآنية لا بد أن يطابق النتائج والمرشحين |
| 08 | V4-008 | **BLOCKED_EXTERNAL + AUDIT_REQUIRED** | توقيع دائم ووجهة GitHub Tank/صلاحيات كتابة ورفع مطور آمن |
| 09 | V4-001 | **NOT_DELIVERED** | 2 مشروع ZIP منفصل + APK مستقل بعد بناء V4 |
| 10 | جميعها | **NOT_TESTED** | لا دليل هاتف/روبوت ولا شهادة إصدار مستقرة |
| قرار UX | V4-012 | **STUDY_DOCUMENTED / OWNER_APPROVAL_PENDING** | عدم تنفيذ الدمج افتراضياً |

## التعليمات الصريحة للدردشة الجديدة

1. اقرأ **هذه الوثيقة والخطة وقائمة المتطلبات** أولاً. راجع أحدث `HEAD` وGitHub Actions على `feature/tank-v4-implementation`، **لأن HEAD قد يكون تغيّر**. لا تعتمد SHA أعلاه بوصفه الأحدث دون تحقق.
2. لا تبدأ بتغليف ملفات أو إعلان APK حتى تعالج فشل البناء وتختبر سقوط الجلسات. افحص سجلات CI واختبارات Android/SQLite ولا تحذف اختبارات ناجحة أو متعثرة بدون بديل.
3. طبّق الدفعات بحسب جدول الخطة، موثقاً في هذه الوثيقة بعد كل دفعة ناجحة: commit المصدر وتاريخ الاختبار، رقم run، النتيجة الفعلية، الملفات، المتبقي، والقرار التالي.
4. عند كل اختلاف بين المتطلبات ومصدر V4، **تغلب أحدث تعليمات المالك** (قياس واحد بلا سجل، إصلاح التباين العالمي، رقم الإصدار في `الحالة` فقط) ولا تُحذف بيانات V3 عند التحديث.
5. **لا تُدمج شاشتا ChemCore والمرشحين** إلا بموافقة مستقلة، ولا تُنشر نتائج المستخدم في GitHub. تطوير ونشر ChemCore Stable يحتاج مراجعة مستقلة.
6. عند الاستعداد للإصدار افصل `ESP32-Tank` Arduino ZIP و`Cam-tank` Arduino ZIP عن APK، واكتب SHA-256 لكل ملف، وصرّح أي قبول هاتف/روبوت غير منفذ.
7. إذا حصل انقطاع قبل إكمال الدفعة اكتب `STOP POINT` فيها: آخر commit ناجح، ما عُدّل وما لم يُختبر، وسبب الفشل؛ لا تكتب `CI_PASS` دون رابط Actions ناجح.

## قالب دفعة التنفيذ المستقبلية (يُكرر دون حذف التاريخ)

```text
Batch:
Date UTC:
Canonical requirement IDs:
Before commit SHA:
Modified paths:
Implementation details:
Tests performed (commands / counts):
CI workflow run URL + result + tested source SHA:
APK / firmware outputs and SHA256 (if produced):
Data migration and security impact:
Remaining unverified behavior (Android device / hardware / signing):
Status (SOURCE_PRESENT / TEST_FAIL / CI_PASS / DEVICE_PENDING ...):
After commit SHA:
NEXT SAFE STEP:
```

**الخطوة التالية المحددة:** `BATCH 00 → BATCH 01`، مراجعة أحدث branch/CI فعلي واستخراج سبب الفشل، ثم إصلاح `V4-015` واختبارات ترحيل الجلسات حتى النجاح. **لا يوجد تنفيذ برمجي جديد في مهمة إعداد الخطة هذه.**

## Checkpoint 01 — 2026-10-24 — Implementation authorized, P0 session/SAF source fixes

**Authorization:** User expressly requested starting V4 implementation. **Current branch:** `feature/tank-v4-implementation`. **No merge into develop, no V4 release.**

| Batch | Requirement | Work actually committed | Verification state |
| --- | --- | --- | --- |
| 00 | all | Read canonical plan/backlog and compared feature against `develop`; re-inspected failed Actions run [40047526874](https://github.com/HazRob/HazRob-Tank/actions/runs/40047526874); reran its failed jobs. Rerun again resulted in a failed `build-tank-android` job with **no job steps available** and old logs unavailable (GitHub storage BlobNotFound). Exact root cause unknown. | **CI_FAIL / DIAGNOSIS_BLOCKED**; not evidence of Gradle compile error |
| 01 | V4-015 | Commit [1d019266](https://github.com/HazRob/HazRob-Tank/commit/1d0192667cb14d88b267c74039e6704c1c1a9442) binds Spinner selection to cached adapter identities instead of querying SQLite during callbacks; clears cache on DB error; reuses approved-session query; catches DB error during approval. | **SOURCE_PRESENT / DEVICE_NOT_TESTED**, root cause of original phone crash still unproven |
| 01 | V4-015 | Commit [4d8aadb0](https://github.com/HazRob/HazRob-Tank/commit/4d8aadb080703e60d50dec0e5248b0446ff8e233) updates source-based Spinner crash regression assertions. Commit [1a01b981](https://github.com/HazRob/HazRob-Tank/commit/1a01b98105fa5f338b21d5305aaa25064811ef73) adds real-SQLite grouped count test for empty, 650 + 25 field sessions. Commit [b57308e4](https://github.com/HazRob/HazRob-Tank/commit/b57308e4b44fc9d411132d60cff298e4600926a1) wires it into V4 CI before inherited-source checkout. | SQLite equivalent query manually exercised in Python: **PASS** (650 / 25 sessions, 973 northern links). New source-extracted CI test **NOT RUN in hosted workflow**; migration/device unverified |
| 07 preliminary | V4-006/V4-010 | Commit [c3126042](https://github.com/HazRob/HazRob-Tank/commit/c312604228d1520cdf043149dd75cabd25ddd506) fixes instant-report SAF lifecycle: previously stored instant-operation state only when an unrelated physical PDF picker was active; now independent. Commit [ceb597b5](https://github.com/HazRob/HazRob-Tank/commit/ceb597b5d5872273b8b842990434dbd5dbb65ce9) adds JUnit regression contract. | **SOURCE_PRESENT / JVM_CI_PENDING / DEVICE_PENDING**; does not prove complete single-operation lifecycle |
| 00 | plan authorization | Commit [e47068a1](https://github.com/HazRob/HazRob-Tank/commit/e47068a1be2ab95337ab55c5ef0a8ccfe439f77c) updates plan authorization to execute instead of planning-only. | DOC_UPDATED |

### Safety, data and release boundaries
- No database schema migrations or destructive deletions executed. Neither old V3 captures, approved site sessions, developer experiments nor research datasets were deleted.
- Original four-wheel `sohpany/SERVOC-Android` untouched; no ESP32 motor-control or camera flash applied.
- Source checks on six modified paths passed: Spinner ID maps, independent PDF saved state, source assertions, real-SQLite script path and CI hook found. Such checks do **not** constitute Android UI/JVM verification.
- Not yet run: Gradle tests, Android APK compile, original v9->current data migration tests, Logcat reproduction of sessions crash, offline live one-shot source-isolation, signing/upgrade, real phone and firmware tests.
- **Stop point:** the Actions rerun produces a `failure` without step details; obtain a functioning runner and an actual build/test log before claiming `CI_PASS`. Until then keep **V4 UNRELEASED**.
- **Next safe step:** execute `python3 tools/verify_v4_session_queries.py` and `python3 tools/verify_approved_report_sql.py` on a full repository checkout, then Gradle `:app:testDebugUnitTest :app:assembleDebug` using the pinned private baseline. Capture a device Logcat stack trace on opening `الجلسات` repeatedly, check migration with a backed-up older DB; continue batch 02 after P0 evidence.


## Checkpoint 02 — 2026-10-24 — Global contrast and completed instant result after disconnection

| Batch | Requirement | Changes | Status |
| --- | --- | --- | --- |
| 02 | V4-009 / V4-014 | [cd0f9155](https://github.com/HazRob/HazRob-Tank/commit/cd0f91559e6cf42aa770d5e69a48f12edf1b7fcb) makes the shared contrast guard resolve solid `GradientDrawable` backgrounds (with Android API guard) rather than assuming inherited white/gray. Text on outlined input surfaces, color-filled cards and warnings is checked against the actual fill. [7651e949](https://github.com/HazRob/HazRob-Tank/commit/7651e949560b4de52de551556ecc10eb8c8bc29f) adds regression source assertions. | **SOURCE_PRESENT / CI_PENDING / VISUAL_DEVICE_PENDING** |
| 04 (partial) | V4-010 / V4-006 | [3b1d09bb](https://github.com/HazRob/HazRob-Tank/commit/3b1d09bb7b21c857d909d585be4893ae62ddded6) fixes an offline UI bug: `LivePane.refresh()` previously overwrote the completed operation card/progress when telemetry disappeared. Now the frozen operation remains clearly completed and PDF action enabled after link loss, while running operations remain incomplete. [e7eb29ae](https://github.com/HazRob/HazRob-Tank/commit/e7eb29ae79e10afbc73cf208f0a07fa62e000c11) corrects Java text escapes in labels; [9083f084](https://github.com/HazRob/HazRob-Tank/commit/9083f084c0f4d5fb732305827a5094b5496eceba) adds source-regression checks. | **SOURCE_PRESENT / CI_PENDING / DEVICE_PENDING** |

**Still incomplete:** V4 single-operation temporal telemetry buffer, immutable capture-time R0 provenance, full state machine and field-session isolation audit; cross-screen candidate/plot/PDF evidence consistency; full light-theme dropdown/RTL audit; original `الجلسات` crash Logcat; real Gradle build. The offline fix preserves a result already in memory, **not** a previously nonexistent sample or durable restoration of the whole live workflow.

**STOP POINT:** Retain V4 work on the feature branch. Investigate GitHub Actions job failure (no steps/logs available from retried run); do not infer success from source tests. Next execute independent SQLite and JVM suites in a functioning Gradle environment before widening changes.

## Checkpoint 03 — 2026-10-24 — Owner's Echo, MQ controls, session stability and newest R0

**Owner authorization:** “ابدا بتنفيذ التعديلات”, covering the follow-up Echo, lower MQ toggle, sessions crash, and saved R0 / clean-air false alarm issues. Work remained on `feature/tank-v4-implementation` only; NOT merged/released. Traceable requirements: [V4-016..018](../requirements/V4-UPDATE-BACKLOG-2026-10-24.md), with sessions fix under V4-015.

### Concrete source commits

| Target | Commit(s) | Result / acceptance gap |
| --- | --- | --- |
| MQ power button V4-017 | `a0cdc46b` | Removed the `relayButton` upper-right `cameraBox` overlay and placed the same listener/button in bottom `actions` row. Portrait + landscape layout visual acceptance **PENDING**; preserved E-STOP. |
| Active R0 & false positive V4-018 | `a73db022`, `38627b60`, `f4638c88`, `e3cd4f5d` | `MainActivity` no longer initializes live gas alarms from old independent `health_baseline_*` database values. Operational baseline is Rs/R0=1 from active `ManualR0Calibration`, and changes are detected before `sensorHealth.accept`/alerts. Missing actual Rs cannot fall back to a legacy ratio. Historical recorded-R0 path retained. The original root cause is a **source-evidenced risk**, not device-proven diagnosis. |
| Remote + Direct R0 consistency V4-018 | `0e5ff49a` | MQTT canonical snapshot path now copies/re-normalizes all 8 active `mq` channels and `measurement.mq` via the latest phone R0 **before** CanonicalMeasurementNormalizer and all downstream consumers. Direct path already passed through `RobotProtocolV4.normalize` and `ManualR0Calibration.apply`. App restart loads last committed R0 from `SharedPreferences`; `saveAutomatic` uses synchronous `commit()`. Firmware R0 values still exist as provenance; no firmware calibration overwritten. |
| Echo firmware + UI V4-016 | `c8df2361`, `608ed75f`, `cd2434b9`, `750df7da` | Echo ISR ignores duplicate rise events; validates pulse window 110..23500 us; filters distance via 3-sample median; short raw valid pulses trigger immediate warning; timeout invalidation drops bogus distance; raw + filtered cm/status retained through HTTP Android path. `ESP32-Tank` source now labeled `0.1.4-v4-dev`. The 500 ms drive watchdog and warning-only role remain. **Hardware pulse/level-shifter tests pending.** |
| Sessions crash V4-015 | `adcf36ee`, `841bab79` (plus checkpoint 01) | Site/session dropdowns no longer rebind adapters on each refresh when bound IDs AND labels are unchanged. Existing SQLite grouped query, guarded refresh and in-tab non-destructive error handling retained. **No original on-device Logcat; the exact crash root cause is not verified.** |
| Regression coverage | `7b8e0443`, `26a2caa3`, `46413f3f`, `57035a83`, `13adb7e7` | Added JUnit source-contract checks and `tools/verify_v4_echo_r0_contract.py`; wired standalone Python verifier into V4 GitHub Actions before original-source checkout, alongside `verify_v4_session_queries.py`. |

### Actual verification and blockers

- GitHub source read-back after commits: **12 / 12 focused source-contract assertions true** (Echo bounds/median/near warning/warning-only, R0 durable and direct normalization, no ratio fallback, baseline refresh, lower MQ UI, adapter stability, verifier regex). This is **SOURCE_CONTRACT_PASS ONLY**.
- Inspected pinned original-source `SensorHealthEngine`, `LiveGasStateEngine`, `GasAlertCoordinator`, `CanonicalMeasurementNormalizer`, `AnalyticsSnapshot`, `TelemetryRecorder`, `RemoteConnection`: confirmed two distinct operational baseline paths and a remote-snapshot normalization bypass risk; historical telemetry stores captured channel JSON.
- No actual Python CLI verifier execution, JUnit, Gradle APK build, Arduino compile, UI screenshots, device Logcat, sensor-ruler comparison, or hardware-level acceptance was obtained in this checkpoint. Historical V4 Actions failure cannot be diagnosed from available logs; combined status and PR-filtered run lookup returned no new pass. Do not represent these source checks as `CI_PASS`.
- No database migrations, old session deletions, release signing, firmware flashing or changes to `sohpany/SERVOC-Android`.
- **Required before release:** Run Python/SQLite checks; Gradle `:app:testDebugUnitTest :app:assembleDebug`, run Arduino ESP32-Tank + Cam-tank compile; verify newest R0 across all 8 in Direct AND Remote, restart/reconnect and actual clean air including adequate sensor warmup; test calibration provenance in frozen instant/site report; inspect `الجلسات` crashes with actual Logcat and migrated v9 DB; check portrait/landscape MQ/E-STOP; physically measure Echo distances 20/40/90 cm with 5 V echo correctly level shifted. Firmware pins: TRIG GPIO23, ECHO GPIO39.
- **Delivery state:** `SOURCE_PRESENT / CI_UNKNOWN / DEVICE_NOT_TESTED / V4_UNRELEASED`. No APK or Arduino source ZIP generated or claimed.

### Next resume point
Continue from the V4 branch head after these commits and requirements. Verify CI executions with legible logs or run pinned-source Gradle in an authorized builder. Inspect timestamp/frozen provenance for single-operation ChemCore/candidate/PDF and field sessions. Do not merge V4 into `develop` or publish an APK as a tested release without compile and phone/robot gates.

## Checkpoint 04 — 2026-10-24 — Owner's non-M4 Stable PPM and instantaneous reference warnings (V4-019)

**Interpretation recorded:** the Tank robot produces **instantaneous source-backed ESTIMATES**, not a full-shift reference-certified occupational dosimeter. M4 is **not a prerequisite to calculate/display a bounded provisional ppm fit** from traceable M1/M2 research/manufacturer/literature relations, nor to show a labelled conditional warning against a source limit. M4 and full identity/reference validation remain prerequisites only for a separately labelled official/compliance-grade concentration assessment. A momentary comparison to TWA warns about a *hypothetical future sustained exposure*; no eight-hour mean is fabricated. LFL comparison is instantaneous. No absence of a warning asserts safety.

### Actual Tank-only implementation (UNRELEASED)

- **Stable content/provenance audit:** pinned embedded Stable builder `tools/build_hr069_embedded_stable.py` from `sohpany/SERVOC-Android` contains **652 identity records, 49 non-M4 response priors (43 research curves + 6 published MQ135 overlays), 0 M4 calibrations**; its `knowledge.response_priors.records` uses `LOG10_LINEAR` slope/intercept/sigma and bounded ppm domains. This inventory is a research input, **not** an on-device validation count.
- **Critical pre-change defect:** `TankInstantAnalyzer` previously called `ChemCoreLegacyCandidateRegistry.acceptedReferenceCandidates()` directly: the APK could install the Stable snapshot without the *active* library necessarily supplying the models to this particular one-shot solver. Source before commit lacked `ChemCoreLibraryStore` selection in the solver.
- **Active Stable adapter:** [`6dab9404`](https://github.com/HazRob/HazRob-Tank/commit/6dab9404fe8096cae869db796e028f9601820fc8) creates `TankStableScreeningModelRegistry`. It reads the **currently installed active Stable** via `ChemCoreLibraryStore`, bounds payload size, recomputes SHA256 and checks installed pointer/hash/schema/row counts, and converts eligible M1/M2 LOG10_LINEAR priors to `ChemCoreResponseModel` with original source ID, numerical domain, sigma and non-reportable status. It reconciles `legacy_id` with the **current** SQLite UID rather than blindly assuming `legacy_id+1`. Multiple Stable models for one MQ channel must use non-overlapping concentration domains; built-in models for stable-covered channels are replaced rather than counted twice. When unavailable/invalid, a reasoned `*_USE_BUILTIN` status accompanies the untouched original source-backed fallback, without claiming Stable models were imported.
- **Actual single-operation wiring:** [`050bba92`](https://github.com/HazRob/HazRob-Tank/commit/050bba92449312ced7076ce99c76b0e57c54c29e) selects the loaded models before `ChemCoreInferenceEngine.analyze`, adds `active_stable_reference_models` version/hash/status/import/reject counts to the frozen ChemCore output; [`59296f62`](https://github.com/HazRob/HazRob-Tank/commit/59296f6240f451e9b5981b7de9fc4fb4eb0835fa) passes Android context from the live measurement so the real installed snapshot is consulted. [`ec6b84f6`](https://github.com/HazRob/HazRob-Tank/commit/ec6b84f65301cf203dfde5f16e38ee9f1dc43a09) exposes `screening_ppm_estimate`, method, availability and `screening_m4_required=false`, separate from `ppm_reportable=false` for one-shot research.
- **Raw reference values, independent of M4:** [`f4cdc364`](https://github.com/HazRob/HazRob-Tank/commit/f4cdc36477011fe8cfedeb1159008322bb4d26d2) creates `TankExposureReferenceCatalog` extracting `ExposureRepository` OSHA/NIOSH/etc. values and original units/provenance for ranked chemistry, with a manually chosen-chemical API [`ba9850f7`](https://github.com/HazRob/HazRob-Tank/commit/ba9850f73efc0bece85024b050c0fd3dcacc6b9e). [`106de88f`](https://github.com/HazRob/HazRob-Tank/commit/106de88fc7d5ef94ebdab694d97116fef9657bab) freezes the inventory into the same instant analysis, [`05466d9a`](https://github.com/HazRob/HazRob-Tank/commit/05466d9acfeb219a5038d9c76912738210f35fea) displays the catalogue in both analysis and results subtabs, and [`b551ecd8`](https://github.com/HazRob/HazRob-Tank/commit/b551ecd89092ef0c754e30aaed62ab3986acb49c) adds a direct `حدود التعرض للمادة المحددة` button even before R0/measurement.
- **Instant conditional warning engine:** [`53a5c1e2`](https://github.com/HazRob/HazRob-Tank/commit/53a5c1e2f17d94d4e720d28ea68547971ca6772a) implements `TankScreeningExposureWarning`: without M4, a valid numerical candidate fit plus matching **source-identified and comparable units** may warn that continued exposure at an instantaneously estimated concentration above TWA/STEL could exceed the specified time average, while LFL/LEL and instant reference roles generate immediate conditional screening; 10% LFL is a separate early precaution, **not an ignition finding**. It does not silently compare mg/m³ with ppm, fabricate a TWA duration series, assert identity or legal compliance, or clear any safety flag. [`49d4a759`](https://github.com/HazRob/HazRob-Tank/commit/49d4a7593cf086c61db4413d34fde4bc4fae30e0) binds results into current operation; [`23637eb2`](https://github.com/HazRob/HazRob-Tank/commit/23637eb2bdc6f9a1df3104da37ff4c70c5e6860e) labels provisional comparisons separately.
- **User-facing results:** [`0ff7ead0`](https://github.com/HazRob/HazRob-Tank/commit/0ff7ead0a19bc96a231dfbe539dfc3ac40c6719d) displays conditional reference warnings in both ChemCore panes; [`1e65be13`](https://github.com/HazRob/HazRob-Tank/commit/1e65be13f9fadfaa1b0ce704ebc20b20659649f5) gives warning cards visible amber/red contrast while keeping them explicitly *hypothetical*. [`187e7bc0`](https://github.com/HazRob/HazRob-Tank/commit/187e7bc0aace135bfef2a68fadc3a4a84873dba5) shows calculated ppm for individual candidate rows. [`a6283de9`](https://github.com/HazRob/HazRob-Tank/commit/a6283de94b801a3afd05affa916c48d9e42c4135) updates `ChemCoreAnalysisPresenter.gasExposurePpm` so a **withheld regulatory** field never conceals separately available provisional reference warnings. [`d5d448bb`](https://github.com/HazRob/HazRob-Tank/commit/d5d448bb435679d077034fe0adca9d5897d3f4f5) fixes a caught duplicate final-view initialization in initial integration.
- **One-operation PDF:** [`759d016c`](https://github.com/HazRob/HazRob-Tank/commit/759d016cc37f46ec7551834e1d97cab275eaa123) includes raw reference inventory, [`34aa108e`](https://github.com/HazRob/HazRob-Tank/commit/34aa108e1686620df7744ea06f8cd4d3bd0eaa6d) conditionally adds an instantaneous TWA/LFL provisional warning page, and [`c8f6a78e`](https://github.com/HazRob/HazRob-Tank/commit/c8f6a78e122eed974a741458121cde0223a05f41) identifies the **actually loaded** Stable version/status/model count.
- **Regression checks:** [`bf407fe7`](https://github.com/HazRob/HazRob-Tank/commit/bf407fe71901202796bbae620f029f5f4b577baf) and [`48e0dd99`](https://github.com/HazRob/HazRob-Tank/commit/48e0dd99d647936540631e7bb97074054adf97e0) add six JVM unit scenarios: non-M4 instant 8h TWA, immediate LFL, 10% LFL precaution, no certified/false-safe verdict, rejected unconverted mass limit, and absent fit. [`01d1d48f`](https://github.com/HazRob/HazRob-Tank/commit/01d1d48ffc2b7c7d3b1d85a5e5439d6b44064dae) + [`90035504`](https://github.com/HazRob/HazRob-Tank/commit/9003550405d40cbf2c7c2db68901e463109414e9) add three direct M1/M2 logarithmic prior math and provenance unit scenarios. [`add7d2e5`](https://github.com/HazRob/HazRob-Tank/commit/add7d2e50c6f94dda58ca1f0e0d67f505e4eb8a9) adds standalone source contract `verify_v4_screening_stable_contract.py`; [`ae3a8c7a`](https://github.com/HazRob/HazRob-Tank/commit/ae3a8c7a958317b82194cb991e9629ae24463a31) wires it into V4 GitHub Actions ahead of Android build.

### Verification truth and scope

- **GitHub read-back/source-structure checks:** 13/13 key checks PASS for actual active Stable installer presence, runtime SHA/row validation, normalized UID, duplicate-sensor prevention, V4 live context wiring, non-M4 estimated ppm path, 2 warning panes (exactly two initializations), candidate ppm rows and unchanged strict one-shot reportability. **These are not JVM tests**.
- **NOT EXECUTED/NOT VERIFIED:** newly added JUnit (6+3) and hosted Python checks, Gradle compilation or APK/signing, real installed Stable import count, numerical performance/error on Tank's actual 8 MQ array, current R0/restart accuracy, device TWA/LFL warning screenshots, PDF rendering, physical known-gas studies. Historic Actions error persists with unavailable logs.
- **Important nuance:** `Stable 1.0.0-hr069r1` includes 49 explicit non-M4 response priors, **not 49 device-validated curves**; model imports are gated on current DB UID mapping and per-model content, so actual count may be less. Its 652 identity entries are **not 652 usable quantitative equations**. Distinct validated fingerprint artifacts are not proven present in the embedded payload. Other original HR069 built-in source-supported fingerprint/QSPR techniques remain fallback/parallel evidence; do not conflate them with a newly certified Tank Stable.
- **Original-HazRob carry-over limitation:** inherited `MqConditioningProfile` records a 48 h conditioning campaign for the original HR069 hardware; **Tank's physical 48 h conditioning and MQ7/MQ9 heater validity have not been established here**. Its use as a Tank qualification is pending controlled physical evidence. Source/prediction ppm is research/worker early warning ONLY.
- **Scope:** changes only in `HazRob/HazRob-Tank` feature branch; no edits to original `sohpany/SERVOC-Android`, public/library scientific reference bank, or frozen historical `sohpany/HazRob-Lab`; no unsupported claim that the original independent central regulatory `GasPpmExposureAssessment` gate was deleted. M4 remains solely for reportable regulatory output, not provisional calculation/warning.
- **Checkpoint state:** `SOURCE_PRESENT / SOURCE_STRUCTURE_PASS / JVM_TESTS_NOT_RUN / CI_UNKNOWN / DEVICE_PENDING / V4_UNRELEASED`. No standalone APK/Arduino ZIP generated or confirmed.

### Next continuation

1. Obtain a real V4 Gradle/Android 36 runner and execute JUnit including `TankStableScreeningModelRegistryTest` and `TankScreeningExposureWarningTest`; compile both Arduino sketches and check GitHub Actions logs.
2. On Tank Android device, verify active Stable version/hash/valid imported prior count and compare controlled reference-curve estimates against direct source-registry baseline; exercise 8 MQ R0 snapshots, fields and candidate matching.
3. Independently confirm Tank MQ initial conditioning, heater profile and actual errors via known reference gases; do not turn screening risk indications into safety/clearance certificates.
4. Record physical phone `الجلسات` Logcat, UI contrasting/portrait and Echo ruler measurements; package candidate artifacts only after gates pass.


## Checkpoint — 2026-10-27 — توسيع النطاق إلى جميع تبويبات المطور والمختبر

**حالة الخطوة:** توثيق متطلبات وفحص مصادر فقط (`PLAN_UPDATED`)، دون اعتماد أي وظيفة من التطبيق أو نجاح اختبار APK جديد.

- طلب المستخدم صراحة شمول **كامل وظائف** الأقسام الخمسة داخل مساحة المطور: التجارب، المختبر، R0، الاتصال، المكتبة، بما فيها التجارب المعتمدة، المقارنات العلمية، صادرات PDF/Excel، التحديث والنشر ومصدر البيانات. رُبطت جميع البنود بمعايير اختبار فردية.
- الملف المرجعي الجديد: [مصفوفة Developer + Laboratory](V4-DEVELOPER-LAB-COVERAGE-MATRIX-2026-10-27.md)، المعرّفات `V4-D01` إلى `V4-D09`.
- **ملاحظة مراجعة تستلزم التحقق:** `DeveloperTab.java` يُنشئ فئات `DeveloperExperimentsTab` و`AnalyticalLabTab` و`DeveloperConnectionDiagnosticsTab` و`CentralLibraryDashboardTab` التي لم تظهر في قائمة مصادر `ui` على الفرعين المفحوصين. لا بد من مراجعة بقية مصادر البناء ثم تشغيل Gradle لتحديد أثر ذلك، قبل اختبار المختبر عمليًا.
- **الخطوة التالية بعد الاستئناف:** مراجعة حالة الفرعين `feature/tank-v4-implementation` و`feature/tank-v4-mq-unified-r0` واستخراج آخر SHA، ثم فحص تعريفات فئات المطور الخمس ومسارات بنائها، ثم الاختبارات `V4-D01`، وبعدها `V4-D02..D09`. لا دمج ولا إصدار نهائي قبل نتائج فعلية.


## Checkpoint — 2026-10-27 — V4-D01 verified baseline and CI failures

- **Verified dependency:** Android source is an overlay atop pinned `sohpany/SERVOC-Android@6281d3eb7c1c961ce63b0b5e4ae0792874626720`, as documented by `tools/build_v4_codemagic.sh` and `build-tank-android-v4.yml`. All four apparently missing DeveloperTab dependencies were fetched successfully from that pinned source: `DeveloperExperimentsTab`, `AnalyticalLabTab`, `DeveloperConnectionDiagnosticsTab`, `CentralLibraryDashboardTab`. Thus the earlier absence from Tank's *overlay* directory is **not** a compile blocker; do not recreate them blindly.
- **CI evidence:** [Tank V4 APK run 40259706778](https://github.com/HazRob/HazRob-Tank/actions/runs/40259706778): Java and test-source compilation PASSED; `testDebugUnitTest`: **661 total / 15 failures**, therefore APK build blocked by tests, NOT missing developer classes. Log retrieved from job 121370816738, showing actual assertions.
- Failure clusters: historical R0 fallback/provenance assumptions (`ManualR0CalibrationTest`, `HR069R1EditableR0PersistenceTest`, `SensorHealthEngineTest`, etc.); weak evidence and absent-input ratio tests (`AcceptedR0NormalizerEvidenceTest`); one outdated source-string assertion (`HR069R1FieldUXRegressionsTest.siteAndSessionScopesAreHierarchical`); one report session assertion. **Do not restore unsafe synthetic R0 solely to satisfy outdated tests.** Classify and update tests only with supported current contract; preserve required behavior.
- Five developer destinations exist at pinned baseline; **functional tests remain unverified**, therefore V4-D01 source-discovery PASS / Gradle unit baseline FAIL. Next exact action: add firmware /r0 direct transport contract, then review developer R0 UI and strict telemetry provenance; run CI tests.


## Checkpoint — 2026-10-27 — V4-D04 firmware-owned R0 integration (source implemented, CI pending)

- Firmware branch `feature/tank-v4-mq-unified-r0`: `ea1f9ff` introduced persistent NVS R0 readback before "ready" and `committed` + `schema=tank-r0-fw-v4/1` in `/r0`; `dc6e38d` added controller MAC identity in both `/r0` and `/diag`. **Firmware CI runs 40422118259 and 40422378610 PASSED compilation**.
- Android branch `feature/tank-v4-implementation`:
  - `3b03ad6` added `RobotApi.calibrationStart/status/cancel`, with direct-mode support and explicit refusal of unsupported remote use.
  - `3d8ae08` added `FirmwareR0Contract` validating state/schema/`committed`, eight channels and ≥10 samples each. Draft values not eligible.
  - `e990c5a`, `bd70124` added phone-side mirrored firmware R0 provenance with device identity.
  - `4dc6190` replaced outdated 20-minute UI automatic acquisition with nonblocking firmware 300-second warmup + 60-second collection, explicit R0 re-sync, cancel and lifecycle release.
  - `f97b983`, `f6e0901` propagate device identity and reject cross-controller reuse of firmware calibration in new snapshots.
  - `d4b6817` added real `FirmwareR0ContractTest` cases: committed vs provisional, missing channel, count <10, invalid R0, unknown schema.
- **Verification status:** firmware compilation PASS; Android Gradle build **NOT PASS**. CI earlier 40422666783 compiled source but failed legacy R0/source-contract tests, including a three-window/20-minute test that conflicts with the approved firmware-controlled V4 design. Further tests and APK packaging are not certified.
- **Next exact action:** inspect latest Actions run for `d4b6817`, triage test failures without suppressing meaningful calibration/safety tests, update obsolete assertions against revised requirements, and rerun Gradle. Android APK remains UNRELEASED; device bench acceptance pending.


## Checkpoint — 2026-10-27 — Gradle regression repair and original-logo provenance

**Test progress, not yet release:**
- [CI 40424321760](https://github.com/HazRob/HazRob-Tank/actions/runs/40424321760): **665 tests, 2 failures**, after earlier **666 tests / 19 failures**. The two outstanding failures there were legacy branding expectations and obsolete instant-session export assertions. A recent build with their replacements is in progress. CI passes only when the latest revision succeeds.
- Relevant source corrections: `a26441e` requires controller ID also in direct AP sensor normalization; `c7aeba2` recognizes firmware clean-air R0 without claiming reference-gas ppm validation; `2afea0c` accepts that explicitly documented source in archived ChemCore; `dc9ad58` marks wrong-device R0; `eef7b32` changes completed session analysis to immutable recorded Rs/R0, and `af18ba0` adds that regression; `845a688` ensures lab-quality indicators do not accept unverified firmware R0 or legacy fallback as calibrated.
- CI tests revised **without bypassing them** where requirements changed: old 20-minute app R0 scheme -> firmware 300+60; false field R0 constants -> fail-closed; old sessions UI source string -> ID-bound session callbacks; single instant report -> frozen one-shot operation; calibrated phone R0/device provenance -> explicit validation. Relevant commits `0d15b81`, `282253e`, `285be1d`, `e1b11e0`, `d4379f5`, `aa1624e`, `981a5c5`, `b848ce0`, `4ba6087`, `f5d674f`.
- **Logo restored from authoritative source:** pinned `sohpany/SERVOC-Android@6281d3e...` has deterministic `tools/apply_hazrob_branding.py` which builds original `ic_hazrob_hr069_r1.png` (1024x1024 transparent). Tank `ConnectionGate` now references this original branding (`3d5a98f`); both Tank V4 build paths preserve original AndroidManifest icon and roundIcon (`7363382`, `0143866`); branding regression updated to explicitly check original logo (`001d4a1`). The Tank V3 chassis vector remains as an unused historical asset.
- Remaining mandatory gates: latest full Gradle results; Android APK signing/package verification, real phone launch/sessions/contrast and export tests; ChemCore grouped experiments; physical MQ8 calibration and NVS interruption test. No stable V4 release yet.


## Checkpoint — 2026-10-27 — OWNER BUILD HOLD / CONTINUE SOURCE WORK

**Owner directive received 2026-10-27:** continue the implementation plan, documentation, source review and code changes, but **DO NOT BUILD THE ANDROID APPLICATION / RUN GRADLE CI / PRODUCE APK** until the owner supplies further changes. This instruction takes priority over former automatic-build or release instructions.

**Enforcement completed:** `.github/workflows/tank-v4-android.yml` commit `0634bf5` and `.github/workflows/build-tank-android-v4.yml` commit `6a0f422` removed automatic `push` triggers on `feature/tank-v4-implementation`; both now support `workflow_dispatch` only. All other workflow push branches inspected: none target this V4 app branch. No workflows were manually dispatched after this directive. Last pre-hold Android CI runs [40424904867](https://github.com/HazRob/HazRob-Tank/actions/runs/40424904867) and [40424904988](https://github.com/HazRob/HazRob-Tank/actions/runs/40424904988): 665 tests, 2 failures, both `ChemCoreLabQualityTest` (missingEnvironmentIsAdvisoryNotFingerprintBlocker, cleanAirR0UnlocksFingerprintButNotReferenceGasCalibration). Their results are *pre-hold* evidence only.

**Allowed next steps:** review those two assertion failures without executing new builds; review/fix source-only sessions, exports, lab, developer tabs, R0, logo and contrast; add/update meaningful test source code and log each commit. **Prohibited until explicit later approval:** `gradle`, `assembleDebug`, GitHub Actions workflow dispatch, triggering any equivalent APK build elsewhere, signing/publishing an APK, hardware flashing.

**Resume rule:** On resumption, first inspect this checkpoint; do not treat the absence of new CI as a failure. Wait for owner's additional changes and subsequent authorization before manually starting build validation.


## Checkpoint — 2026-10-27 — Owner additions: full sessions/reports, laboratory R0, embedded Stable (SOURCE ONLY)

**Build hold remains active.** Changes are source-level only; NO Gradle, APK, GitHub build workflow dispatch or device qualification was run in this checkpoint.

### Completed source commits
- `1093947`: developer experiment R0 availability now verifies the current ESP32 identity for firmware-imported values, removes unused duplicate manual editor, and records each channel's observed Rs/R0/source/session/time/controller in immutable reference metadata; scientific M4/PPM is never implied by baseline R0.
- `ed984f3`: moves FieldSessionsTab repository read set (sites, sessions, measurement counts, approvals, schema check) to background executor. Maintains UI-thread-only rendering and coalesces refresh triggers.
- `1467fd2`: site creation, session start/close/approve now run off the main thread; discards stale site-selection query result and refreshes from latest choice.
- `813669d`: fixes broken `PYmkdir` shell heredoc delimiter in the manual V4 build script. Discovered via source inspection, NOT claimed tested.
- `98d676a`: manual R0 overwrite and firmware R0 re-import both require explicit confirmation when the other authority exists; reset UI truthfully says it clears the phone-only R0, not restores a synthetic fallback.
- `1894338`: ChemCoreLabQuality output rejects foreign Tank controller R0 even in per_sensor provenance JSON; current controller identity recorded.
- Prior `0cf7bc6` and `01b7d70`: report site/session spinner ID bindings with outlined light options and direct PDF/XLSX export routing from selected site/session.

### Source-level architectural findings
1. TelemetryRecorder (pinned original) creates USER `field_mq_cycle` measurement sessions and attaches them to the sole ACTIVE field session; developer controlled captures are deliberately excluded. Completion stores normalized telemetry samples and a ChemCore decision. User approval in FieldSessionRepository demands exactly one completed USER measurement with sampled rows and approved state. Cross-domain identity restrictions preserved.
2. FieldSessionReportExporter and SiteSummaryReportExporter resolve exact field/site scopes; SiteSummaryExportActivity exports PDF/XLSX using SAF and an off-main-thread writer. Report buttons must target the exact selected scope; path updated.
3. Original Android Gradle preBuild task generates offline `generated/chemcore/embedded_stable/{manifest.json,chemcore-stable-snapshot.json}`. Tank V4 preparation patches **both** manifest app-version boundaries to `4.0`; manual workflow asserts SHA-256/payload lengths, 652 molecular/chemical identities and no M4 calibrations. Source-only evidence **does not prove resulting APK assets** until an owner-authorized build and ZIP inspection.
4. Baseline bundled library is the pinned `1.0.0-hr069r1` screening **pre-release** snapshot (49 M1/M2 research response priors, no accepted M4) and **not** a guaranteed newer Tank-scientific Stable model version. A later explicit, verified higher-version Stable snapshot may supersede it. Never invent or auto-promote external research results.
5. Tested in past pre-hold CI only: 665 test cases / 2 failures. Recent source commits have not been compiled.

### Next source actions (no build)
- Add source-test assertions for nonblocking Sessions, correct report scope/format, firmware R0 controller binding and offline Stable manifest packaging; keep field devices and report formatting as device acceptance gates.
- Inspect DeveloperExperimentsTab/AnalyticalLabTab together for R0 conflict, measured-session provenance and multi-gas evidentiary limitations.
- Audit report and lab Java overrides for compilation blockers by static inspection only; finalize test suite changes without triggering Gradle.
- After the owner provides additional modifications AND explicitly authorizes the build, run Gradle CI/Android APK inspection and actual device tests. Until then UNRELEASED.


## Checkpoint — 2026-10-27 — Report SAF and source-only verification

**Owner build hold still enforced.** This checkpoint is source review / edits and **did not start GitHub Actions, Gradle, APK assembly, installation, or sensor qualification**.

### Completed changes (actual commits)
- `e580c4e`: fixed MainActivity field-session report dispatcher. Approval and session ID lookup are performed in background, picker opens on the UI thread, and a pending guard prevents two simultaneous Android SAF requests. Cancellation, missing session, invalid format, failure to open picker and activity disposal reset the request state; PDF/XLSX writing remains on a worker and uses the exact saved session ID.
- `1563128`: added Android test source asserting off-main report lookup, approved-only PDF/XLSX, session identifier preservation and clearing the pending SAF state.
- `0f966c4`: integrated six additional report SAF invariants into the build-free static source gate `tools/verify_v4_operator_lab_static.py`. This script remains configured for future **manual-only** V4 CI, not an automated build.
- Inspected latest DeveloperExperimentsTab and ChemCoreLabQuality; verified previous commits `5bb4128`/`fd15bcd` already enforce controller matching for BOTH firmware-sourced and manually device-bound R0. Did not duplicate an existing patch.
- At checkpoint: 30 targeted source-string assertions were checked against live GitHub contents and **30/30 matched**. **Scope**: static source presence only, not Java compilation, UI behavior, database query semantics, document formatting or APK packaging.

### Remaining acceptance tests — NOT PASSED
- Fresh + upgraded populated SQLite: site create/select, session start/close/approve, one completed valid USER measurement linked to exactly one field session, deletion, rapid scope switching; no ANR/uncaught exceptions.
- Site and session PDF/XLSX export on actual Android through SAF; validate file signature, page count/readability, right-to-left Arabic, reopened files, scoped grouping and no cross-site data.
- Firmware /r0 300+60 calibration against real sensors, power-loss NVS readback, MQTT vs direct identity, eight-channel valid samples, manual override audit and ChemCore frozen replay.
- Current actual bundled APK `assets/generated/chemcore/embedded_stable/`: verify library manifest's 4.0 compatibility and SHA256, snapshot parser in offline mode, 652 chemical identities and 0 accepted M4. **No APK currently assembled in this checkpoint.**
- Complete five developer tabs, logo, outlined black dropdowns, permission flows, crash regression on devices. CI baseline from pre-hold is historical (665 tests / 2 failures) and cannot validate these new changes.

### Next exact source-only action
Audit `ReportsTab`/session export, laboratory completed-session consumption, and Stable source package integrity for version mismatches or cross-device R0 contamination. Add meaningful test sources and checkpoints, without invoking any build or tool that triggers a build. Resume at the first open V4-D02/V4-D03/V4-D05 work item after checking repository HEAD.


## Checkpoint — 2026-10-27 — Source/UI audit completed; owner-authorized diagnostic build started
- Earlier NO-BUILD directive honored during source development. Owner later requested source/UI/menus/chart audit and proceeding to build if no source blocker remained.
- Source edits: `34f4c9c` corrected ChemCore status/result dark-on-dark cards, `1b6518f` replaced unusable PM fake chart controls with an accurate no-data state, `7c46777` invalidates stale report charts, `201f564` persists session report SAF identity across Activity recreation, `11f3022` adds screen/chart clarity unit tests, `b0e645e` labels known-ppm captures as exploratory, not automatically M4-approved.
- Previous R0 scientific evidence gates and at-least-10 real calibration samples remain intact; no physical accuracy claims.
- **Build authorization and scope:** workflow `68d89ff` is now scoped to a single marker path (no ordinary source commits trigger builds), owner authorization marker `abbb4e5` triggered [Actions run 40437121019](https://github.com/HazRob/HazRob-Tank/actions/runs/40437121019). Status when logged: in progress. Debug output is UNRELEASED and conditioned on all required Gradle tests passing. Source tests and UI visibility still require device verification.
- **Next exact action:** retrieve run 40437121019 job steps/logs; if any failure occurs, patch source and tests without suppressing relevant safety checks. A new source commit alone cannot retrigger the scoped build; subsequent controlled build requires another explicit authorization or a directly available manual dispatch mechanism.


## Checkpoint — 2026-10-27 — Pre-build readiness review after controlled CI failure

**User request:** report when V4 is completely ready for a new build. **No build is to be started merely because sources change.** This checkpoint is a source-only continuation.

### Latest completed CI evidence (not a release)
- Controlled GitHub Actions build [40437121019](https://github.com/HazRob/HazRob-Tank/actions/runs/40437121019), triggered by the prior documented owner authorization at commit `abbb4e5`, **FAILED**.
- Passed: operator/lab/Stable static contracts, grouped sessions SQL, Echo/R0/screening checks, pinned source checkout and overlay, embedded offline ChemCore Stable staging with SHA256 and V4 compatibility, firmware API4 contract, and real SQLite reporting query tests.
- Java **main** application compilation `:app:compileDebugJavaWithJavac` **PASSED**.
- Blocking defect occurred at `:app:compileDebugUnitTestJavaWithJavac`: two source-test helpers used Java-11 `Files.readString(Path,Charset)` under the Java source compatibility selected by the project. Result: test source compilation failed before running any JUnit assertions, and APK assemble/packaging was skipped.
- Corrections in source-only commits `1b82a68` / `f28d47d` replaced both helpers with Java-compatible `new String(Files.readAllBytes(...),StandardCharsets.UTF_8)`; `33cbe4b` aligned the chart contract test with existing stronger `released||chartGeneration!=request||selectedId!=experimentId` protection. **These fixes are NOT yet verified by another Gradle run**.
- A source contract pre-audit of UI charts inspected 21 expected markers and identified only the chart-test mismatch above; the source itself already had the stronger guard. Not an Android execution test.

### Current gate
**PREBUILD_STATUS = CONDITIONAL / NOT VERIFIED.** The main Java source compiled on the previous controlled commit, and the known test compilation blockers were patched. Complete unit-test pass, assembled APK integrity, launcher/login logo, stable assets actually inside APK, Android sessions ANR, report PDF/XLSX, and eight real MQ measurements remain unverified.

**Do not assert FULLY READY TO BUILD or RELEASED** based only on source inspection. Next exact action: finish static source/test consistency inspection and any necessary non-building edits; obtain explicit current user permission before rerunning Gradle; review complete unit-test output; only then notify readiness for package build and separately qualify real hardware.

### No-background guarantee
No automatic work or later notification is promised from this conversation alone. Any subsequent work/notification occurs only through a new conversation turn or an explicitly configured scheduled check.


### Source fixes and prevention after failed run 40437121019 — 2026-10-27
- JDK/source compatibility regression fixed in both V4 source-test helpers: commits `1b82a68` (FieldSessions test) and `f28d47d` (UI Chart test) now use `new String(Files.readAllBytes(...), StandardCharsets.UTF_8)`. This removes the two exact `Files.readString(Path,Charset)` compile errors from job 121968356375. No new Gradle run executed in response.
- `33cbe4b` corrected an obsolete chart assertion to test the stronger `released||chartGeneration!=request||selectedId!=experimentId` bitmap-release guard already present in the source. No safety assertion was skipped.
- `81d47fa` added `tools/verify_v4_prebuild_sources.py`: build-free guards for source overlays, Java source compatibility across Tank test overrides, sessions lifecycle, saved report display, firmware R0 and offline Stable packaging requirements.
- `2c36367` and `e660e58` added this source-only preflight to both **future** controlled V4 build paths; the push trigger remains restricted to the already-authorized marker path. No APK build was requested or run in this checkpoint.
- User specifically requested notification when the app becomes fully build-ready. A separate condition-based daily readiness check was configured to examine the repository and report only after sufficient evidence, without triggering any build or release. This does not replace outstanding Android tests and source review.

**CURRENT DECISION: NOT FULLY BUILD-READY / NO NEW BUILD STARTED.** The previous Java main-source compile passed. We have patched known test compile errors, but the 665+ JUnit tests and APK assembly have not yet run successfully on the resulting revision. Do not claim current code has passed Gradle or full device acceptance. Resume by inspecting new source-only gate and remaining tests, then request explicit owner permission before controlled diagnostic build.


## Checkpoint — 2026-10-27 — Prebuild readiness notification and source-only report ANR hardening

- **Latest diagnostic run reviewed:** GitHub Actions [40437121019](https://github.com/HazRob/HazRob-Tank/actions/runs/40437121019) **FAILED** on `compileDebugUnitTestJavaWithJavac`, two uses of `Files.readString(Path,Charset)` in `TankV4FieldSessionsAsyncAndExportContractTest` and `TankV4UiChartClarityContractTest`. Both are now `new String(Files.readAllBytes(...), UTF_8)` in branch source. No subsequent Gradle build invoked in this checkpoint.
- **Source P0 remedied:** `7da95c0` moves ReportsTab site/session SQLite loading, approved measurement report listing, selected archive experiment / ChemCore analysis and time-series queries off Android's main UI thread; old in-flight results are gated by generation and selected-record ID. FieldSessionsTab off-main-thread handling already present.
- **Future tests:** `6de131a` adds `TankV4ReportsAsyncContractTest`; `1203b52` updates `verify_v4_operator_lab_static.py`; `a2b2922` updates existing field-session report test for the new worker-thread SQL method.
- **Verification performed:** read 11 live files from current branch and checked 10 high-priority source invariants, **10/10 matched**. These are source-level assertions only; not Java compilation, Android instrumentation, PDF/XLSX generated file checks, or sensor accuracy validation.
- **Readiness ledger:** `docs/implementation/V4-ANDROID-PREBUILD-READINESS-GATE-2026-10-27.md`, latest `dba24b4`, records PB-01..PB-09; state `NOT READY_TO_BUILD` pending residual audits and verification. Owner has requested notification at readiness. A condition-watch notification has been configured to alert only on objective, explicitly logged `READY_TO_BUILD`, without starting a build.
- **Owner build restriction:** no Gradle, workflow dispatch, APK assembly or flash action in this checkpoint. A future authorized diagnostic build and real device acceptance are required separately.
- **Next exact task:** inspect test-source compatibility, matrix for all five developer screens, controls/branding, stable version/provenance and report scope migration. Reconcile planned tests and blockers. Only mark `READY_TO_BUILD` when no known source blocker remains; do not equate an untested patch to a successful test.


## Checkpoint — 2026-10-27 — Resumption, two-branch reconciliation and Tank-only research staging hardening

**Owner direction:** resume from the 27 October checkpoint, finish remaining source work accurately. This entry documents source changes only; no Gradle build, CI dispatch, Android installation, ESP32 flash, developer upload, or actual device measurement was performed in this checkpoint. Keep the separately documented build hold until an explicit new build authorization.

### Verified repository divergence
- Canonical Android development branch: `feature/tank-v4-implementation`; preceding head was `e4d4db6a3a944acd75402ed298f62004b9b7322f`.
- Firmware research branch: `feature/tank-v4-mq-unified-r0` (previous observed head `1d2bbc01308ab6ec59ad7007a10723395d7dfbab`).
- Byte comparison using GitHub blob IDs confirms firmware `Config.h`, `Hardware.cpp`, `Hardware.h` and `ESP32-Tank.ino` are already identical across both heads. **Do not cherry-pick or overwrite firmware merely because Git comparison includes its changes against a shared ancestor.**
- `DeveloperConnectionDiagnosticsTab` existed in the pinned original `sohpany/SERVOC-Android@6281d3e`; it was not absent from the assembled Java source. Previous successful main Java compilation in run `40437121019` confirms there was no missing developer-class compile blocker on that earlier candidate.

### Concrete implementation commits
| Commit | Change | Evidence scope |
| --- | --- | --- |
| `e3344759` | Added Tank-only overlay `github/GitHubReferencePublisher.java`, isolating publishing to `HazRob/HazRob-Tank` branch `research/tank-v4-review-staging` and path `research_data/tank_v4_scientific_review`. HTTP 201 is insufficient: require commit SHA and GitHub GET readback SHA-256 + byte-count match. | SOURCE_PRESENT only |
| `84471d9` | Raised bounded GitHub readback text cap to support multi-kilobyte captured reference payloads, avoiding the original 16 KiB truncation failure. | SOURCE_PRESENT only |
| `09ea5b7` | Corrected developer experiment text: no inherited app-version label; staging is Tank-only, operator confirmed and scientifically non-promoting. | SOURCE_PRESENT only |
| `d986966`, `ceab99f` | Added/adjusted Java source-contract checks for target repository/branch, approved lineage, no original-HazRob publishing and post-upload readback integrity. | TEST_SOURCE_ADDED / NOT_RUN |
| `2ae12ca` | Added light-color Tank override of inherited `DeveloperConnectionDiagnosticsTab`; changed white-on-dark labels/status/checkbox to legible dark-on-light. Captures `remember.isChecked()` on UI thread before the asynchronous WAN provisioning task. | SOURCE_PRESENT / DEVICE_PENDING |
| `4277e11` | Added Java source-contract checks for developer WAN text/background contrast and UI-to-worker checkbox state ownership. | TEST_SOURCE_ADDED / NOT_RUN |
| `7ff040c` | Wired publisher isolation, readback and WAN diagnostic source invariants into future source-only prebuild gate. | STATIC_GATE_UPDATED / NOT_RUN |

- New independent `research/tank-v4-review-staging` branch created from `main`. **No reference file was uploaded**, no PAT was supplied, and actual GitHub Contents-write permission, readback API round-trip, conflicts and retry handling remain untested.
- Direct live-GitHub readback compared 18 narrowly selected source/test markers: **18/18 matched**. This is a manual source-level structural verification, **not** execution of either JUnit test class, Python prebuild gate, Gradle or actual GitHub file upload.
- Existing `DeveloperExperimentsTab` still requires full review of persistence/retry/idempotence and encrypted credential lifecycle; creating the staging branch does not authorize automatic publication or scientific promotion.
- All changes are in Tank repository only; no files were changed in `sohpany/SERVOC-Android`, and no older SQLite user data were modified.

### Latest objective build / acceptance truth
- The last controlled Android Actions run `40437121019` failed in `compileDebugUnitTestJavaWithJavac` on two incompatible `Files.readString(Path,Charset)` test helpers. Main app Java compilation and earlier static/SQLite/embedded Stable staging steps succeeded on the *older* tested SHA. The two helpers were subsequently corrected to `Files.readAllBytes`; current changes and the new tests have not been compiled.
- No claim of `CI_PASS`, complete `UNIT_PASS`, `DEVICE_PASS`, developer GitHub publication acceptance, SHA verified *inside a new APK*, NVS power-loss safety or release-ready gas analysis.

**STOP POINT:** Current work is `SOURCE_UPDATED / STATIC_MARKERS_18_OF_18 / NEW_TESTS_NOT_RUN / NOT_READY_TO_BUILD / V4_UNRELEASED`. Resume from this branch's latest HEAD, review the two added contract tests and GitHub publisher against the pinned base, validate fresh + migrated SQLite and full developer-tab interactions. Obtain separate owner permission for a controlled Gradle diagnostic build; after passing tests, perform device/SAF/report/robot gates before claiming release readiness.
