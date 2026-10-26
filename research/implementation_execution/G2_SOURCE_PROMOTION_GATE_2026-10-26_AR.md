# G2 V4 — بوابة ترقية الأدلة التجريبية للتقنيات الثمانين (2026-10-26)

**القرار الافتراضي:** `G2_BLOCKED`. لا تتحول نتيجة G1 البرمجية أو UCI الاستكشافية أو ورقة علمية أو رقم دقة منشور إلى `G2_PASS` تلقائياً. `G2_PASS_CANDIDATE_REQUIRES_REVIEW` تعني فقط اكتمال الحقول الأولية، وتستلزم مراجعة مصدر مستقل. G3 Tank الفيزيائي منفصل.

## 1. الحد الأدنى لمجموعة القياس المصدرية

1. Raw physical measurements محفوظة دون استبدالها بمخرجات محاكاة؛ المصدر/النسخة/الترخيص وSHA256 محسوب فعلياً من البايتات؛ زمن/تردد أخذ العينات، والوحدات والدارة.
2. لكل تعرض: `exposure_id` فريد، `gas_reference_id`، هوية الغاز ونقاوته، تركيز مرجعي ووحدته وعدم يقينه إن كانت التقنية تستنتج ppm، هوية الحساس/الجهاز، `session_id` أو `batch_id` أصلي، بداية ونهاية التعريض، تسخين/تهوية، T/RH، ظروف الصفر والخط الأساسي، سجل الفقد/التشبع.
3. لكل MQ: موديل + **رقم الحساس الفعلي**، Vc وRL ودارة ADC، نوع إشارة المصدر. إذا استخدم Rs/R0: `R0` مقاومة موثقة لنفس الحساس ونفس طور السخان مع غاز/تركيز مرجعي وملف مصدر؛ لا يجوز طرح R0 من ADC أو اعتبار طرح baseline معايرة R0.
4. الحد الأدنى **التشغيلي المبدئي للتخطيط وليس شهادة كفاية إحصائية**: 3 مستويات تركيز مرجعية إن كانت التقنية تدّعي اعتماداً على التركيز، و3 جلسات/دفعات في أيام منفصلة لكل شرط مرجعي مع ضوابط blank/recovery؛ الزيادة تحددها قدرة الدراسة والتحليل الإحصائي. للمخاليط يلزم مرجع مستقل لكل مكوّن. أي نقص يسجل في `source_qualification.json` ولا يعالج باختلاق معرفات.
5. G2 يقيّم صلاحية المنهج ضمن نطاق المصدر الموثق فقط؛ الادعاء بالعمل عبر أجهزة أو مواقع متعددة يحتاج أجهزة/مواقع مستقلة في holdout. G3 يحتاج قياسات فعلية على Tank MQ8 نفسه.

## 2. تجميع وتقسيم غير متسرب

- مجموعة الاستقلال الأساسية = جلسة/جهاز-جلسة/دفعة/حملة موثقة من المصدر، لا صف أو نافذة زمنية أو `replicate_ordinal` مستنتجة. كل نوافذ التعرض الواحد وكل المشتقات التابعة له تبقى في القسم نفسه.
- `train` و`calibration` و`blind_test` مجموعات منفصلة بالكامل على مستوى group IDs؛ يجب أن يحوي `split_manifest.json` المعرّفات الأصلية وبصمة بروتوكول مجمد قبل فتح blind test.
- في open-set: كل تعرضات الغاز المحجوب غائبة عن التدريب ومعايرة العتبة واختيار الميزات/الخوارزمية. لا يختار أفضل تكوين بعد النظر إلى نتائج unknown final test.
- scaler/imputer/feature selector/calibration fit على train فقط (ومعايرة known على calibration فقط). لا تكرار للمشاهدات كأنها مستقلة في فترات الثقة.
- عدم توفر معرفات جلسة/جهاز أصلية = `GROUPING_SOURCE_PROVENANCE_UNVERIFIED` = لا ترقية.

## 3. قواعد القبول المسبقة

**شروط صلبة مشتركة:** مصدر خام قابل للتحقق، وحدات موثقة، خط معالجة مضبوط، مرجع target مستقل، تجميع مثبت، holdout محجوب، صفر تسرب، metrics مقابل baseline، فترات ثقة تراعي المجموعات، مخرجات قابلة لإعادة التشغيل، وعدم وجود أي استنتاج safety/ppm خارج النطاق.

**معايير بحثية افتراضية مقترحة للتسجيل قبل التجربة، وليست معايير دولية أو شهادات سلامة:**
- تصنيف الغاز: `macro-F1` و`balanced_accuracy` لكل غاز ومجموعة، مع مقارنة baseline محدد مسبقاً؛ يجب أن يكون التحسن ذا دلالة على مستوى المجموعات لا الصفوف المتكررة.
- Open-set: `known_acceptance >= 90%`، `known_correct_and_accepted >= 80%`، `unknown_rejection >= 90%`، و95% group-aware lower confidence bound >= 80% لرفض المجهول؛ مع بيان false acceptance لكل غاز. هذه عتبات فرز بحثي محافظة، وليست كفاية لاعتماد أجهزة سلامة.
- تقدير تركيز: `MAE`, `RMSE`, bias, calibration slope/intercept لكل غاز ونطاق تركيز، وتحسن MAE عن baseline مستقل وفق CI grouped؛ حدود الخطأ النهائية تحدد مسبقاً بحسب عدم يقين المرجع ومتطلبات الاستخدام. لا ppm certification بلا سلسلة معايرة معيارية مستقلة.
- انجراف/مخاليط/عدم يقين: تُكتب metric/target/acceptance criterion ملائمة لكل تقنية **قبل** الاختبار؛ لا ينقل معيار التصنيف إلى الانحدار أو التعويض البيئي آلياً.

أي تجاوز للحدود المسبقة أو فشل holdout يمنع الترقية. مراجعة بشرية للمصدر والتجربة لازمة حتى عند اجتياز التدقيق الآلي.

## 4. ملفات الأدلة المطلوبة بالضبط

يُحفظ لكل تقنية/مصدر/تشغيل تحت `research/g2/evidence/<technique_id>/<dataset_id>/<run_id>/`:

| ملف إلزامي | المحتوى |
|---|---|
| `raw_manifest.json` | source URI/version/license, SHA256, archive inventory, acquisition timestamp, parser SHA |
| `source_qualification.json` | gas/reference traceability, concentration units/uncertainty, sensor/device/session provenance |
| `signal_lineage.json` | raw units, circuit/ADC/RL/Vc, feature formula, baseline/response windows, R0 mode and physical R0 source when used |
| `group_manifest.csv` | one exposure per row, original group IDs, gas, concentration, device, session, timestamps |
| `split_manifest.json` | train/calibration/test group IDs, no-overlap checks, frozen seed and split SHA |
| `protocol_locked.json` | versioned method, preprocessing, hyperparameters, baseline, metric targets, acceptance thresholds set before test |
| `calibration_manifest.json` | known-only threshold/fit and source reference; train/test nonreuse |
| `predictions_blind.csv` | exposure ID, true reference, prediction, rejection/abstention, fold/group ID |
| `metrics_per_group.csv` | counts/confusion/false accepts per independent group, gas, concentration |
| `metrics_summary.json` | baseline and model metrics, scope, failure flags, G2/G3 claims false by default |
| `uncertainty_grouped.json` | grouped bootstrap or justified cluster-aware intervals, effective group counts |
| `reproduce.sh` | exact source acquisition, checksum, test command and output SHA |
| `ci_run.json` | commit SHA, Actions run/job/artifact IDs, SHA256 of evaluation artifact, pass/fail |
| `review_decision.json` | reviewer, date, scope, blockers, decision; no automatic scientific promotion |

`G2_INDEPENDENT_EVIDENCE.json` يُحدّث فقط بعد اكتمال الملفات ومراجعتها؛ يحتفظ `g2_evidence_audit.py` بحالة fail-closed.

## 5. الحالة الراهنة

- UCI1081: raw SHA verified؛ **source signal units unknown**، `R0=NOT_USED`، و`replicate_ordinal` ليس مجموعة جلسة/جهاز مستقلة. نتائج 06.5 (حتى 13/90 رفض مجهول) لا تفي بمتطلبات G2.
- Run `40254297639`: 34 اختبارات provenance/audit ناجحة؛ `0/80` مرشحين و`80/80` محجوبين.
- Run `40254297596`: ستة baselines × ست طيات على 90 تعرضاً فعلياً من المصدر؛ **exploratory فقط**.
- `G3 Tank=0/80`؛ لا تعديل لـChemCore Stable.

**عائق مصدر البيانات:** لا يمكن للتشغيل البرمجي وحده توليد قياسات MQ8 المرجعية أو معرفات جلسات/أجهزة مفقودة في أرشيف UCI. يجب جمعها أو اقتناء مجموعة بديلة موثقة قبل ترقية تقنيات تعتمد عليها.
