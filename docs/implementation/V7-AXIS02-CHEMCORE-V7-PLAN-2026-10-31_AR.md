> **تنبيه قرار مالك المشروع — 31 أكتوبر 2026: SUPERSEDED / DEFERRED TO V8.** هذا المستند خطة تاريخية لتطوير ومحاكاة ChemCore، ولم يعد يمثل محور V7 الثاني. **المحور V7-02 الحالي هو التقاط الجلسات الميدانية وتصدير MQ/ADS الخام، يليه V7 APK.** قياسات R0 التجريبية، الغازيات الخمس، محاكاة ChemCore ومراجعة 3D/4D مؤجلة بالكامل إلى V8. انظر [البرنامج المعتمد الجديد](V7-REVISED-ROADMAP-ADS-CAPTURE-2026-10-31_AR.md). لا تنفذ بنود الاختبار الواردة أدناه باسم V7، ولا تعتبر نتائجها قد أُنجزت.  

# HazRob Tank V7 — المحور الثاني: خطة تطوير واختبارات محاكاة ChemCore V7
**Plan ID:** HZT-V7-A2-CHEMCORE-SIM-001  
**Date:** 2026-10-31  
**State:** `PLAN_READY / WAITING_REAL_TANK_R0_AND_SESSION_INPUTS`  
**Branch:** `feature/tank-v7-reports` · **APK:** `NOT_AUTHORIZED`  
**Owner assignment:** معايرة R0 وجمع جلسات العيّنات لدى المالك؛ تصميم **وتنفيذ وإعادة تشغيل المحاكاة** وتحليل الأخطاء وتطوير المحرك وربط النتائج بالتقارير هي مهمة المساعد.
**Inherited evidence:** `V6-CHEMCORE-SCIENTIFIC-MASTER-PLAN-2026-10-29_AR.md`, `V6-CHEMCORE-80-TECHNIQUE-NEW-ENGINE-COVERAGE-2026-10-29.json`, `V6-N08-CLEAN-AIR-RESPONSE-ALARM-BOUNDARY-2026-10-30_AR.md`, `V6-RESUME-R0-APP-CHECKPOINT-2026-10-30_AR.md`, `docs/research/V6-CHEMCORE-SIMULATION-SCENARIOS.csv`.

## A. حقيقة نقطة الانطلاق
- `ChemCoreTankV6` و`Engine` و`FingerprintEngineV6` و`SignalFeaturesV6` موجودة. مصدر V6 الأخير يقول 80 تقنية مسجلة: **78 جزئية و2 غير منقولتين بصورة كاملة، 0 مؤهلة على جهاز Tank الحقيقي**. ليس الرقم 80 عدداً من المنحنيات المستقلة.
- تتضمن V6 مراجع منحنيات غاز وبصمات بحثية، لكن **لا تعادل معايرة الجهاز نفسه ولا عينة غاز مرجعية معلومة ppm**. لا يصح القول بوجود محرك 3D/4D علمي فقط لأن `V7FieldChartLayout` يحتوي فراغ رسم 3D/4D.
- `V6-N08` أثبت مشكلة إيجابيات كاذبة في الهواء المرجعي مع `Rs/R0=1`. لذلك يجب اختبار الحالة النظيفة والامتناع وأخطاء الاستشعار قبل تحكيم هوية الغاز.
- `V6-RESUME-R0-APP-CHECKPOINT` يوثق **بروتوكول V6 الجاري: 180 ثانية warmup + 60 ثانية ONE sample** مع معاملة pending/committed. لا تنقل وصف `V5 300s+60s` أو فكرة `5+3+3 ×3` السابقة إلى نتائج V7 دون تأكيد الفيرموير والإجراء الفعلي. القياس المُرسَل يسجّل بروتوكوله الحقيقي ونتيجة الاعتماد NVS.
- لم يرفع المالك بعد ضمن هذا المحور ملفات R0/Gas sessions؛ لا نتائج محاكاة فعلية جديدة ولا أرقام دقة لـV7.

## B. مدخلات المالك — تعريف ملفات البيانات وعقد المصدر

### B1. الحزمة الأولى: R0 / المعايرة
أرسل تصدير التطبيق/الروبوت الخام إن أمكن (CSV/JSON/Excel) مع:
| Field | Requirement | Rule |
|---|---|---|
| `device_id`, `firmware_version`, `app_version` | required | الجهاز والنسخ الأصلية لتاريخ القياس |
| `calibration_id`, `started_utc`, `ended_utc`, `committed_utc` | required where present | لا افتراض أن القيم pending اعتمدت أو حفظت |
| `protocol_warmup_s`, `protocol_measure_s`, `protocol_version`, `sample_count` | required if recorded | الراجح V6 180+60؛ يُقبل التوثيق الفعلي المختلف مع تمييزه، لا إصلاح تعارضات بالتخمين |
| `mq_sensor_model`, `sensor_instance_id`, `r0_value`, `r0_unit`, `r0_definition_id` | one per each available MQ sensor | الوحدة والنوع وتعريف R0 والتزامن مع (Rs)؛ لا دمج `Ω` و`kΩ` دون تحويل موثق |
| `adc_path`, `rl_ohm`, `vc_v`, `divider_gain`, `heater_phase`, `heater_v` | if acquired | إذا غابت قد يصبح الاستدلال كميًا غير مؤهل |
| `temperature_c`, `rh_pct`, `pressure_hpa`, `baseline_conditions` | if acquired | لا اختلاق ظروف البيئة |
| `state`, `NVS_readback`, `raw_archive_sha256` | desired | إثبات pending/committed واستقرار أصل البيانات |
- يفضّل إرسال **كل قراءات المعايرة الزمنية** لا متوسط R0 فقط؛ يمكن استعمال المتوسط المعتمد لتحليل V6، والحفاظ على trace لدراسة التشتت.
- R0 `clean air` لا يُخْلَط بـR0 معرفة بغاز مرجعي في كتالوجات الشركات أو الدراسات؛ `r0_definition_id` بوابة إلزامية عند منحنى ppm.

### B2. الحزمة الثانية: خمس مجموعات تجريبية كما سمّاها المالك
| Label | Intended source | Notes for simulation |
|---|---|---|
| `ETHANOL` — إيثانول | عبوة/عينة معروفة الاسم | لا تحويل تركيز سائل إلى ppm هواء دون قياس مستقل |
| `LIGHTER_FUEL_BUTANE` — بيوتان الولاعات | منتج ولاعة محدد | تأكد من التركيب الفعلي بالملصق/المصدر؛ قد يتضمن إيزوبيوتان أو بروبان |
| `HOUSEHOLD_LPG` — غاز الطبخ المنزلي | مصدر منزلي محدد | خليط متغير من بوتان/بروبان وإضافات؛ ليس اسم جزيء نقي |
| `KEROSENE_VAPOR` — أبخرة الكيروسين | وقود كيروسين معلوم المصدر | خليط أبخرة هيدروكربونية، وليس «غاز كيروسين» أحادي الجزيء |
| `GASOLINE_VAPOR` — أبخرة البنزين الوقودي | وقود سيارات معلوم المصدر | خليط متغير؛ **ليس بنزين `benzene` C6H6 نقيًّا** |
- لكل واحدة: `exposure_id`, `session_id`, `source_label`, `source_label_confidence`, `material_name/brand/batch`, `reference_concentration` و`reference_concentration_unit` **إن قيسا مستقلًا**, `start/end_utc`, `device_id`, `calibration_id`, `T/RH`, `heater`, `sensor readings`, `sample_qc`, `baseline/exposure/recovery` إن كانت محفوظة.
- لا يلزم ملف ppm لإجراء **اختبار تمييز وصفي**؛ لكنه لازم مع منهج قياس مرجعي معتمد لقياس خطأ ppm. إذا كان تركيز البخار غير معروف، فإن `label_known=true` لا ينتج `ppm_truth`.
- تحفظ موجات `MQ2,MQ3,MQ4,MQ5,MQ7,MQ8,MQ9,MQ135` المتاحة فعليًا؛ لا تُعبّأ قناة مفقودة بصفر أو بحساس من دراسة أخرى.
- سجّل دورات منفصلة وكفاية المكررات الفعلية، لا تعامل عشرات قراءات الثانية الواحدة كمكررات تجريبية مستقلة.
- **السلامة:** المواد المختارة شديدة الاشتعال، وأبخرة البنزين الوقودي تحمل مخاطر سمية واشتعال. لا تتضمن هذه الخطة تعليمات صنع أبخرة/خلطات أو تفريغ غاز، ولا توصي بإجراء التجارب في المنزل أو بجوار مصادر إشعال. يتم الحصول على القياسات فقط في إعداد مختبري مناسب تُقيَّم فيه المخاطر ويُتحكم بالتهوية ومصادر الاشتعال وكشف LEL بوسائل مؤهلة؛ إن لم يتوافر ذلك، تستخدم datasets منشورة مؤهلة لا تعرّضًا مرتجلًا.

### B3. الأرشفة وقيود الخصوصية العلمية
- `raw` immutable؛ تحليل V7 يُنشئ `derived` منفصلًا مع `parent_sha256` وversion/commit/model/config.
- كل snapshot يحتفظ بوحداته الأصلية ومعناها، ويُمنع مطابقة `ppm` الناتج بالمصدر في حالة اختلاف تعريف R0 أو منحنى التسخين أو sensor batch.
- لا تتبع معرفات الملف بالاسم الشخصي؛ UUID للجلسة والجهاز يكفي.

## C. الجرد 3D / 4D قبل إضافة تقنية
**A2-03 checkpoint:** على GitHub branch المعتمد:
1. افحص `ChemCoreTankV6`, `SignalFeaturesV6`, `FingerprintEngineV6`، ملفات أصول `research/chemcore_v6`، المصدر Stable ومكتبة المراجع المركزية والـ80 تقنية. سجّل لكل ادعاء: `PRESENT_RUNTIME`, `PARTIAL`, `CHART_ONLY`, `SOURCE_ONLY`, `ABSENT`.
2. **3D** اسم رسم غير كافٍ: حدّد المحاور الفيزيائية الثلاثة ومعنى المقياس مثل `sensor channel / recorded time / normalized observed response` إذا توفر trace حقيقي؛ لا تستعمل عينة واحدة كسلسلة زمنية أو تفرض محور `ppm` دون إثبات.
3. **4D** يحتاج متغيرًا رابعًا محددًا وفترة تزامن/لون أو شرائح/توجيه عرض واضح؛ يمكن عرض `temperature` أو `humidity` فقط عند وجود المصدر وربط الزمن؛ لا تحويل تعريف R0 الأصلي للمنحنى صامتًا.
4. منحنيات ثلاثية/رباعية الأبعاد في Stable قد تكون أصولًا بحثية وصفيّة تحتاج `source_id`, `license`, `units`, `definition`, `dimensions`, `sample_period`, `sensor_model`, `chemical_uid`, `domain`, `hash`. الرسم العميق لا يساوي دليلًا إضافيًا على هوية الغاز، ويجب ألا يُضاعف وزن الحساس ذاته.
5. تقرير gap matrix ثم قرار إضافة حساب multi-dimensional فعلي، أو تفعيل الموجود، أو الاكتفاء بإضافة أصل Stable مرجعي مع إثبات توافق المصدر؛ الإغلاق يحتاج trace output قابل للإعادة.

## D. مسؤولية المساعد: حزمة محاكاة محكمة `SIM-A2-01 ... SIM-A2-14`
| ID | Replay/Simulation | Inputs | Required evidence |
|---|---|---|---|
| SIM-A2-01 | R0 ingestion parity | same approved raw R0 V6/V7 | unit/definition/NVS match, no silent revision |
| SIM-A2-02 | Clean-air negative control | genuine baseline; synthetic edge cases separately | false named candidates, false ppm/alarm gate; report abstention |
| SIM-A2-03 | Five known-class sessions replay | real Tank frozen MQ streams and source labels | confusion matrix per independent **session**, Top1/Top3, macro-F1, balanced accuracy, unknown/abstain |
| SIM-A2-04 | Channel ablation 8→7→3→1 | same session originals with software masking | coverage stability, not fabricated sensors |
| SIM-A2-05 | R0 ± perturbation sensitivity | real R0 with separately labeled synthetic perturbations | rank stability, odds/distance, ppm diagnostic only |
| SIM-A2-06 | T/RH/drift sensitivity | actual T/RH if present; hypothetical scenarios clearly labeled | domain, residual, no invented correction |
| SIM-A2-07 | Source curve compatibility | pinned Stable + manufacturer + central references | `r0_definition`, `Rs/R0`, source units and no extrapolation |
| SIM-A2-08 | 3D/4D replay | actual synchronized multichannel trace | axis/provenance, reduction vs raw, no extra claim |
| SIM-A2-09 | Time features | baseline/exposure/recovery if actually captured | rise/fall/t90/AUC with availability flags; avoid source-free timers |
| SIM-A2-10 | LPG/kerosene/gasoline ambiguity | separately labeled mixture/fuel source sessions | UNKNOWN/mixture behavior and cross-sensitivity; no forced pure molecule |
| SIM-A2-11 | Dependency/consensus | correlated channel priors and source duplicates | evidence independence; stable family-level consensus |
| SIM-A2-12 | Grouped holdout | complete sessions/days/batches/device group | split manifests and leakage audit |
| SIM-A2-13 | V6→V7 paired regression | fixed input/seed/baseline/stable hash | per-case delta, no silent loss of rows or false promotion |
| SIM-A2-14 | Reports contract replay | frozen raw and engine result, no production build while paused | source→engine→R01/R03/R04 field mapping / charts, `-` on absent |

### D1. اختبار التعميم الحقيقي
- لا تبدأ باستنتاج نسب الدقة من عدد قليل من تجارب معتمدة على نفس جلسة لكل غاز. `sample_n` هو عدد **exposures المستقلة** مع `n_timepoints` مستقل العرض.
- عند توافر مجموعات كافية: `train/development`, `calibration`, `sealed_test` على جلسات/أيام/دفعات مختلفة. يمكن تقييم `leave-one-session-out` و`leave-one-day-out` في حال توافر الأعداد المناسبة، وإلا يذكر `insufficient independent groups`.
- كل وصف للمهمة يحدد `known class set`, `unknown class set`, `mixture truth semantics`, `missing channel mask`, `source curve version`.
- أي نسبة أداء `Top1` أو ppm MAE يجب أن تُظهر أساس المقام وحالات الامتناع وخطأ الهوية/التسمية؛ لا تُسمى `chemical detection accuracy` إذا labels خلطات وعينات غير كمية.

### D2. مؤشرات القبول المقترحة وليست أرقامًا ناجحة سلفًا
- `ingestion`: توثيق كل ملف قبل أي تحويل، وحدات متسقة، رفض invalid/NaN والأمثلة غير المؤهلة للتفسير الكمي.
- `baseline`: عدم إطلاق حكم غاز مهني أو إنذار من مصدر clean air أو sample مجهولة المعايرة.
- `identity`: confusion matrix ومعدل الامتناع بوضوح، عرض الأخطاء والنتائج لكل فئة لا المتوسط فقط. عتبة رقمية لا تُعتمد دون عدد holdout مستقل كافٍ ومناقشة تكلفة الخطأ.
- `ppm`: `UNKNOWN` أو `RESEARCH_ONLY` إن لم توجد قيمة تركيز غازي مرجعية مادية ووحدة وتعريف R0 متوافق ونطاق مصدر صحيح؛ لا تزيين النتائج بإحصاء ppm MAE وهمي.
- `drift`: أي تحسين بعد تصحيح R0 يوثق على جلسات مستقلة، ويمنع ضبط نموذج على test محفوظ.
- `3D/4D`: وجود خرج حسابي مؤرخ قابل لإعادة الإنتاج وإحداثيات مصدرها واضح، لا مجرد template.
- `report`: لا تخفيض خطوط PDF <12pt، عدم قص UUID/CAS، لا حساب GHS أو OEL من PDF، جميع رسوم ChemCore من بيانات أصلية وإصدارات محددة.

## E. دفتر مخرجات كل دفعة محاكاة
عند وصول ملف:
1. `incoming/<batch_id>/manifest.json` مع SHA256، وحدات وتعريفات R0 والغاز والوقت.
2. `normalized/<batch_id>/frames.csv` أو `frames.jsonl`؛ يحتوي حقل `source_record_id`، وتحويلات موثقة فقط.
3. `splits/<batch_id>/groups.json`, `train_dev_test.json`، `leakage_audit.json`.
4. `runs/<batch_id>/v6_results.json` و`v7_results.json`, `model_manifest.json` و`config_hash`.
5. `reports/<batch_id>/performance.json`, `confusion_matrix.csv`, `candidate_rankings.csv`, `failures.csv`, `abstentions.csv`, `uncertainty.md`.
6. `reports/<batch_id>/source_3d4d_gap.csv`, `report_bindings_audit.json`, و`decision_log.md` مع نتيجة قبول أو `HOLD` لكل تعديل.

**لا توجد هذه الملفات الآن** لمدخلات Tank V7 لأن المالك لم يرسل قياسات المحور الثاني بعد. هذه أسماء تعاقدية متوقعة وليست ادعاء إنجاز.

## F. تسلسل التعديلات على المحرك
- `M0` جرد مصادر V6 والتحقق مما إذا كان 3D/4D تنفيذيًا أو مرئيًا فقط.
- `M1` استيراد R0 وتأهيل كل sensor-instance وربط تعريفه وسلامة NVS.
- `M2` ربط session replay دون تغيير القرارات الأساسية؛ الالتزام بقياسات المُدخل ومنع drift/temperature فواتح وهمية.
- `M3` نموذج تصنيف مفتوح المجموعة (open-set) وتفريق fuel mixtures عن pure analyte، بوابات abstention وconfidence calibration على independent groups.
- `M4` 3D/4D حساب/عرض من source-qualified series فقط؛ عدم احتساب derivatives من نفس الحساس كأدلة مستقلة.
- `M5` candidate ranking وquantitative hypothesis/uncertainty overlay للمراجع ذات مجال صالح؛ لا claims G3.
- `M6` توحيد مخارج ChemCore V7 بتعاقد JSON/manifest مع R01/R03/R04، وإبقاء R05/R06 مستقلة عن محرك الغاز.
- `M7` بعد أمر المالك بإعادة QA: اختبارات Android/PDF/CI وقياسات Tank مستقلة؛ إغلاق محور المحرك لا يؤدي آليًا إلى APK.

## G. نقطة الاستئناف الدقيقة
`NEXT = A2-03 static audit of existing multidimensional implementation/Stable assets; A2-01 remains WAITING_USER_R0; A2-02 remains WAITING_USER_FIVE_GAS_SESSIONS; simulation A2-04 cannot claim measured result until actual archive arrives.`

المستخدم طلب أن تكون مهمتك المحاكاة؛ **لا تطلب منه تنفيذ خوارزميات أو تحليل بيانات**. اطلب منه فقط ملفات R0 الأصلية والجلسات عندما تصبح متاحة. لا إصدار APK إلا بأمر صريح؛ لا تحديث Firmware تلقائي.
