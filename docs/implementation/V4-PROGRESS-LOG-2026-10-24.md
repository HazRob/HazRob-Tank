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

