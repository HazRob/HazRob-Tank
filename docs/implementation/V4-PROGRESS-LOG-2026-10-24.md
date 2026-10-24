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
