# HazRob Tank — V7 خطة التطوير الرئيسية متعددة المحاور
**Plan ID:** HZT-V7-MASTER-2026-10-31-01  
**Date:** 2026-10-31 | **Repository:** `HazRob/HazRob-Tank` | **Working branch:** `feature/tank-v7-reports`  
**Owner decision:** إغلاق محور تصميم PDF عند مستوى **إنجاز التصميم**، ثم تنظيم ChemCore V7 على محور مستقل. إنتاج APK محظور حتى توجيه صريح.

## 0. نظام إدارة المحاور
1. كل محور له رقم تعريفي، خطة عمل، مدخلات ومخرجات، بوابات قبول، سجل مراحل، نقطة استئناف وcommit evidence.
2. `DESIGN_COMPLETE` ≠ `SOURCE_INTEGRATED` ≠ `TESTED` ≠ `DEVICE_VALIDATED` ≠ `RELEASED`. يجب عدم دمج هذه الحالات.
3. وثائق محور مكتمل تظل مرجعية وغير قابلة لإعادة توصيف الإنجاز على أنه منتج نهائي دون أدلة.
4. تستمر مسؤولية المستخدم عن جمع قياسات معايرة R0 وجلسات الغازات وإرسالها؛ مسؤولية المساعد فحص البيانات علميًا وإجراء **المحاكاة وإعادة تشغيل جلسات القياس (replay)** والمقارنة والتحليل وتطوير ChemCore V7 والربط البرمجي المعتمد لاحقًا.
5. يمنع بناء APK أو تحديث firmwares أو نشر نسخة مستخدم أثناء هذه الخطة قبل أمر جديد. اختبارات المحاكاة البرمجية المخصصة للمحور الثاني تُنفذ **عند وصول البيانات وتفويض بدء دفعة الاختبارات**؛ إيقاف QA/Android CI الذي صدر في المحور الأول لا يُرفع ضمنيًا. لا يُطلق أي workflow من تحديث هذه الخطة.
6. تطوير ChemCore خاص بدبابة HazRob، مستقل عن برنامج HazRob ذي العجلات وعن مسار تقييم الفواكه واللحوم؛ لا تُدرج منحنيات الأغذية في محرك الغاز.
7. محور لاحق بعد الثاني: `AXIS-03 / FUTURE_UNSPECIFIED`. وردت عبارة «مسار تطوير v3» من المالك؛ تُحفظ كما هي دون افتراض أنها تعني V7 أو توصيف فني لم يحدده.

## 1. سجل المحاور
| المحور | التسمية | الحالة عند هذا التحديث | حدود الإنجاز | المرجع |
|---|---|---|---|---|
| AXIS-01 | V7 Android PDF: R01–R06 | `DESIGN_COMPLETE_OWNER_DECLARED / INTEGRATION_PENDING` | الستة صُممت وتوثقت، أضيفت Writers/ملحقات وأساسات المصدر؛ لم تُعتمد جميعها عبر rendering بعد الإيقاف، R05 صور 18 غير محزّمة، R03/R04 وملاحق التقرير تحتاج ربطًا إنتاجيًا | `V7-R01-R06-VISUAL-ART-DIRECTION-2026-10-31_AR.md`; tracker |
| AXIS-02 | ChemCore V7: baseline R0 / 5 fuel-gas classes / 3D/4D / simulations / report bridge | `PLANNED_WAITING_OWNER_DATA` | مدخلات المصدر قبل اختبار محاكاة مبنية على جهاز Tank، مع تحليل مصادر رقمية V6/Stable | `V7-AXIS02-CHEMCORE-V7-PLAN-2026-10-31_AR.md` |
| AXIS-03 | Future development workstream; owner mentioned `v3` | `UNSCOPED` | لا قرارات تنفيذ أو تغيير نسخة | define later |

## 2. AXIS-01 — سجل التسليم وحقيقة الحالة
- R01 gas instant: `TankInstantV7PdfWriter` + `V7InstantVisualAnnexWriter`، المرشحون/القنوات والحدود المرجعية.
- R02 physical: `V7PhysicalReportPdfWriter` + `V7PhysicalGaugeVisualWriter`، قياسات `PhysicalCatalog` الخمسة عشر.
- R03 approved session: `V7FieldSessionPdfWriter`، صفحاته وآثار G/F/P ودورات المصدر والمعارض 3D/4D وGHS/OEL والـSHA.
- R04 site: `V7SiteVisualPdfWriter`، اثنا عشر قسم تصميمي ومصفوفة 144 من مواضع العرض، **ليست قيمًا مستشعرة أو عددًا حقيقيًا لمركبات محددة**.
- R05 fruits: `V7FruitReportPdfWriter`، صور الـ18 في عمود يميني RTL؛ **أصول الصور الأصلية المرخصة لم تحزّم**، ويظل الفصل عن ChemCore gas إلزاميًا.
- R06 beef: `V7BeefResearchPdfWriter`، 7 MQ، 4 فئات للدراسة، TS5 المراجع، قياس Tank الـ180s منفصل عن دراسة خارجية 36h.
- يظل `release.apk_built=false` و`G3=0/80`. مخرجات التصميم **ليست شهادة كشف غازات أو اعتماد صلاحية غذائية**.
- الربط الفعلي بنتائج ChemCore V7 ينقل إلى AXIS-02 task A2-05، مع صيانة كامل شروط التصميم من AXIS-01، ولا يعاد فتح تصميمه كموضوع مستقل إلا بطلب المالك.

## 3. AXIS-02 — سلسلة المهام التي قررها المالك
| WBS | المهمة | مسؤول البيانات / المنفذ | المخرج |
|---|---|---|---|
| A2-01 | استقبال R0 المعتمدة لكل حساس MQ، وملف raw والسياق الزمني والقياسي | المالك يقيس ويرفع؛ المساعد يتحقق من الاستيراد | R0 provenance register + normalized dataset |
| A2-02 | استلام جلسات فئات الإيثانول، بيوتان الولاعات، LPG المنزلي، بخار الكيروسين، بخار وقود البنزين | المالك يجري **فحوصًا آمنة في بيئة مختصة**؛ المساعد يراجع البيانات | frozen measurement/label sessions، baseline/exposure/recovery |
| A2-03 | جرد تنفيذ 3D/4D في ChemCore V6 والمكتبة المرجعية، تحديد ما إذا كانت الحسابات موجودة فعلًا أو مجرد charts slots؛ إدراج بيانات Stable بعد فحص مصدرها وترخيصها | المساعد | 3D/4D evidence coverage + source qualification and design decisions |
| A2-04 | محاكاة replay باستخدام **R0 الحقيقية والبيانات الأصلية**، اختبارات الهوية المعروفة، التعويض، الخطأ، drift، negative/unknown/mixture، grouped holdout | المساعد | inputs/results/metrics reproducible + ChemCore V7 deltas |
| A2-05 | ربط **مخرجات المحرك فقط** بتقارير R01–R06 ذات الصلة (غازات R01/R03/R04، فيزيائي R02 وفق مصدره؛ دون خلط المحرك بتقارير الأغذية) | المساعد | report source binding contract / future integration QA |
| A2-06 | بعد محاكاة كافية: مراجعة أدلة Tank مستقلة ومعايرة كميّة ومعايير G3 قبل أي claim ppm / alarm | owner experimental evidence + assistant analysis | G3 decision ledger, not automatic certification |

## 4. بوابات المحاكاة والقبول
- **G0 DATA**: وجود `sensor_instance_id`, `device_id`, `R0 calibration ID`, `Rs` بوحدته الأصلية و/أو `raw_adc`, `RL`, `Vc`, `divider` إذا لزم تحويل كهربائي، وقت UTC والمتتابع، وسياق الحرارة والرطوبة وحالة heater وsensor model. حقول غير موثقة تُوسم ولا تُستكمل بافتراضات.
- **G1 LABEL**: مادة الهدف من هوية عبوة/مرجع موثق، وتحديد “إيثانول” أو "بيوتان" أو “LPG” أو “kerosene vapor” أو “gasoline vapor” بوصفها هوية معلومة المصدر لا **هوية مثبتة بقراءات MQ**. LPG وبخار الوقود خليط متغير التركيب؛ لا يوصف بأنه غاز نقي. إن لم يُقَس تركيز مرجعي، فالتسمية النوعية لا تعطي ppm.
- **G2 SPLIT**: لا تقسيم عشوائي لعينات زمنية متجاورة؛ حجز جلسات/أيام/دفعات تعرض كاملة ومتميزة والتوثيق قبل الضبط. رفض التسرب عبر R0 shared baseline أو overlapping windows أو المصدر البحثي المكرر.
- **G3 REPLAY**: تشغيل baseline V6 وcandidate V7 على نفس snapshot والـR0؛ نشر confusion matrix والنجاحات والأخطاء والامتناع وOOD، ترتيب المرشحين، حساسية `R0`, stability/latency حيث يتوفر الدليل.
- **G4 SCIENCE**: لا ترويج لهوية غاز مؤكدة أو ppm معتمد/حد مهني أو إنذار مهني أو خلائط M4 من المحاكاة وحدها. المقاييس تُنسب إلى بيانات Tank المؤهلة وفترة الاختبار المحددة. الأرقام من مصدر خارجي تبقى `EXTERNAL_RESEARCH`.
- **G5 REPORT**: مصدر التاريخ هو original capture UTC؛ لوحات R03/R04 تعمل من archived approved snapshots، المصدر والحالة والوحدة ونسخة Stable موجودة؛ لا حساب كيميائي جديد داخل PDF.
- **RELEASE**: غلق محور ChemCore والتكامل لاحقًا **لا يفوض** بناء APK تلقائيًا.

## 5. استئناف العمل
1. اقرأ `README.md` ثم هذا الملف ثم `V7-AXIS02-CHEMCORE-V7-PLAN-2026-10-31_AR.md` و`V7-AXIS02-EXECUTION-TRACKER.json`.
2. افحص commits/branch للحفاظ على الاستقلالية؛ ابدأ بحصر محرك 3D/4D ومخزون Stable وطبقات الأدلة وألا تكرر التحويلات.
3. لا تصف المحور الثاني `SIMULATED` حتى تبدأ اختبارات فعلية قابلة للتكرار مع مدخلات محددة وتسجيل نتائجها. لا تقل إن بيانات R0 وصلت أو أن جلسات الغاز نُفذت ما لم ترفق فعليًا.
4. راجع أي عودة لتشغيل Android CI أو بناء APK مع أمر المالك الصريح. `V7-REPORTS-EXECUTION-TRACKER.json` هو سجل المحور الأول التاريخي؛ هذا الملف وسجل AXIS-02 هما المرجع الأشمل للمرحلة الجديدة.
