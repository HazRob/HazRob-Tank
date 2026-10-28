# HazRob Tank V5 — نقطة الاستئناف التنفيذية (28 أكتوبر 2026)

**نوع نقطة الحفظ:** `REQUIREMENTS_AND_DESIGN_ONLY`  
**الهدف:** عدم فقد أيّ من التعديلات الإحدى عشرة، والعمل من نقطة محفوظة حتى لو انقطع الاتصال.  
**الفرع:** `feature/tank-v5-implementation`، مستودع `HazRob/HazRob-Tank`.

## آخر أعمال تم حفظها بالفعل

| التزام Git | نوع العمل | الدليل |
| --- | --- | --- |
| `ffc1cfbed4c603ae5f82d4e9422651d463a98433` | توثيق 11 متطلبًا `V5-001..011` | `docs/requirements/V5-CHANGE-REGISTER-2026-10-28.md` |
| `0becc27d5a769642c230df0607b216e943cf7a34` | خطة تنفيذ مفصلة `W00..W11` مع ترتيب الاعتماد والقيود | `docs/implementation/V5-EXECUTION-PLAN-2026-10-28.md` |
| `2791a75a7d7b6f369fe8ac16646714139a1b1fab` | مصفوفة قبول `V5-T001..V5-T040` مع معايير بوابات التسليم | `docs/implementation/V5-ACCEPTANCE-AND-RESUME-2026-10-28.md` |
| `4c6950747e9aeb2895e5439527c9c52270445e61` | جرد أولي لمصادر MQ/المصنع، أمثلة Alcohol غير نشطة | `docs/research/V5-MQ-CURVE-SOURCE-AUDIT-2026-10-28.md` |
| `d09936ae1db31c908ffe0621f9a9a44b53e34c12` | ربط تقرير البحث بالخطة | ملف خطة التنفيذ |

**النتيجة المثبتة:** 11/11 متطلبًا محفوظة، 40 سيناريو قبول محددة، 12 مجموعة عمل `W00..W11` موثقة. **لا توجد اختبارات V5 منفذة، ولا كود وظائف V5 جديد، ولا APK V5**.

## الجرد البرمجي المبدئي المدعوم بالمصدر
1. `FieldSessionRepository.approveFieldSession` ينص على `complete!=1`؛ دعم عدة دورات لكل جلسة يلزمه تغيير DB وسياسة الاعتماد ومسارات التقارير.
2. `MeasurementsTab` يعرض ChemCore والمرشحين منفصلين، لكن يوجد بالفعل `TankScreeningExposureWarning` للـPPM التقديري وبوابة تحذير مشروط؛ التوحيد يجب أن يعيد استخدام النتيجة لا يعيد تحليلها.
3. `TankChemicalEvidence.inspect` محصور غالبًا في جداول SQL المحلية؛ `TankStableScreeningModelRegistry` يحمل Stable على نحو منفصل، مما يبرر اختبار اتصال العرض بالمكتبة النشطة.
4. `DeveloperTab` يحوي معايرة R0 بينما `SettingsTab` لا يعرضها؛ مسار firmware الحالي `300+60` يكتب NVS قبل وجود زر موافقة مستخدم منفصل.
5. `StatusTab` و`SettingsTab` ينفذان مسار تحديث محتمل، لكنه يحتاج URL حقيقي واختبار فشل/نجاح/rollback. مطور/مكتبة أربع أقسام، منها بيانات منصات قد تكون غير متاحة.
6. `MainActivity.buildShell` يملك قائمة التبويبات، `ControlTab` يضع Echo أعلى اليمين، والسهم من `HazardOverlayView`.
7. `SettingsTab.directionReverseCheck` ينشئ أربعة CheckBoxes بلا إطار صريح.

## أول مهمة برمجية بعد موافقة البدء/استكمال المتطلبات
**`V5-W00`**: جرد مستقل للمصادر الموروثة ونسخ DB وAPI/Stable، تحديد إصدار التطبيق والتوقيع؛ ثم **`V5-W01`**: تصميم مخطط جداول الدورة الواحدة ومفتاح الاعتماد المعاملاتي وربطها بالمواقع والجلسات؛ ثم **`V5-W02`**: اتفاق تطبيق/firmware على قبول R0 المرحلي أو توضيح أن الاعتماد يتم على مستوى التطبيق فقط.

## تعليمة استئناف قصيرة
عند العودة: اقرأ هذا الملف ثم [سجل المتطلبات](../requirements/V5-CHANGE-REGISTER-2026-10-28.md) و[الخطة](V5-EXECUTION-PLAN-2026-10-28.md) و[مصفوفة القبول](V5-ACCEPTANCE-AND-RESUME-2026-10-28.md). تحقق من `feature/tank-v5-implementation` HEAD الفعلي، ولا تفترض نجاح إجراء لم يُسجل له commit ونتيجة اختبار. أضف كل بند جديد برقم يبدأ من `V5-012`، وألحق كل دفعة عمل ببيانات `commit SHA / files / tests / result / blockers / next action`.

`RELEASE_STATUS: V5_NOT_BUILT`.


## نقطة الاستئناف الأحدث — تنفيذ الدفعة A (تتقدم على نقطة التخطيط أعلاه)

**سجل الحقيقة:** [V5-PROGRESS-LOG-2026-10-28.md](V5-PROGRESS-LOG-2026-10-28.md).  
**آخر commit تنفيذي/توثيقي مرصود قبل تحديث هذه الصفحة:** `179e554ea21f544f9f9a2a9337381da144494de5`.  
**حالة البناء:** `NO_APK / NO_GRADLE / NO_ACTIONS / NO_HARDWARE_CERTIFICATION`.

- اكتملت **الدفعة المصدرية الأولى** وحُفظت في GitHub، وليس تطبيق V5 كاملاً.
- تعديل الدورات وR0 وStable وسهم القيادة والحالة وخانات الاختيار محفوظ؛ لم يبدأ بعد runner دورة الجلسة ولا دمج المرشحين والمختبر ولا إصدار V5.
- اختبار SQL النموذجي: **PASS** بعد معالجة `'LEGACY-'||measurement_session_id`؛ اختبار مصدر عبر GitHub **PASS**؛ JUnit Android وinstrumentation **NOT_RUN**.
- **الاستئناف التالي بالتحديد:** مراجعة `FieldCycleRepository` و`TelemetryRecorder` وربط cycle ذات 60 ثانية إحماء و60 ثانية قياس حقيقية؛ ثم تحديث FieldSessionsTab ببدء/إعادة/اعتماد الدورة ومصفوفة خيارات Gas/PM/Physical؛ ثم تصدير multi-cycle دون كسر V4.
- **قرار مالك المشروع الساري:** ممنوع بناء التطبيق حتى يعلن انتهاء التعديلات والاختبارات ويصدر أمر بناء صريح.
