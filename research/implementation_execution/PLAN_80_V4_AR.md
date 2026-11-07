# الخطة التنفيذية V4 لتطوير تقنيات ChemCore الثمانين — HazRob Tank

**تاريخ التثبيت:** 21 أكتوبر 2026. **مستودع العمل:** `HazRob/HazRob-Tank`, فرع `research/chemcore80-central-evidence-gap-analysis-20261021`. **النطاق:** البحوث فقط، لا تحديث APK أو FW أو محرك ChemCore الإنتاجي.

## 1. الوضع المرجعي عند بدء الخطة

- الـ80 تقنية هي المعرّفات الرسمية في `research/PROGRESS_80.json`. G1 اجتازت **80/80** اختبارات النموذج البرمجي السابقة؛ G2 مراجعة بيانات **80/80** (49 تنفيذات عددية/وصفية على بيانات خارجية بعضها proxy، 29 محجوبة، 2 نظرية). **G3 لقياسات Tank الفيزيائية = 0/80**؛ اختباراتنا الجديدة لا تغير هذا الرقم.
- فهرس المراجع المركزية: [HazRob/build-harness / reference-bank](https://github.com/HazRob/build-harness/tree/main/library/reference-bank)؛ لقطة مصدر المراجعة: `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f`، 80 سجلاً من فئات دراسات/براءات/مصانع/بيانات/محاكاة (ليست 80 تجربة).
- [مصفوفة الفجوات والتقنيات](../systems_analysis/TECH80_CENTRAL_REFERENCE_CROSSWALK_2026-10-21.json): **23 إعادة تصميم**، **52 تطويراً**، **5 إبقاء واختبار**. الأرقام تخطيط هندسي وليس استنتاج أداء من اختبارات.
- [الملف التنفيذي الآلي الكامل](PLAN_80_V4.json) يتضمن لكل تقنية الإدخال/المخرجات، المرجع IDs، اسم المسار البرمجي، الاختبارات والبوابات G0-G3، الحالة والشروط الناقصة.

## 2. بوابات العمل وتسجيل النجاح

| البوابة | معنى مرورها | البينة المطلوبة |
|---|---|---|
| G0 تحليل علمي | سؤال/معادلة، وحدات، مصادر وأخطاء وإشكالات صلاحية | سجل معرف مصدر ونسخة SHA وأثر القرار |
| G1 تنفيذ قابل للإعادة | كود مع اختبارات طبيعية وحدّية وخاطئة وعلى checkout نظيف | commit وCI URL ونتيجة test counts |
| G2 بيانات خارجية | ملفات أصلية ورخصة، وحدات حقيقية، تقسيم exposure/session، عدم تسرب | manifest/source SHA256/أهلية، نتائج مقارنة |
| G3 تجربة Tank | قياس فعلي موثق من MQ8، R0/RL/Heater/ADC/BME، مرجع ppm معروف وتقسيم محجوب | سجلات مختبرية مستقلة، تقديرات خطأ وثقة؛ **غير متوفرة الآن** |
| نشر إنتاجي | مراجعة مقياس وسلامة مستقلة وأمر صريح منفصل | نسخة مستقلة وتعامل rollback؛ خارج هذه الخطة |

كل `BLOCKED` و`FAIL` يدوَّن مع السبب حتى في حال اجتياز CI؛ لا تعتمد تقنية على synthetic أو رسوم الشركة لإثبات ppm أو سلامة.

## 3. ترتيب التنفيذ وثماني دفعات ذات عشرة معرفات

تعتمد الخطة الدفعات الرسمية التاريخية نفسها لتجنب فقد الترابط مع `PROGRESS_80.json`. **التنفيذ الجديد منفصل عن إنجاز بوابات G1/G2 القديمة**.

### الدفعة 01 — 01.1، 01.2، 01.3، 01.4، 01.5، 01.6، 01.7، 02.1، 02.2، 02.3

| المعرّف | نوع التغيير | الأولوية | العمل التنفيذي المطلوب | المراجع الرئيسة | معيار التحقق |
|---|---|---|---|---|---|
| `01.1` | تطوير | P0 | تثبيت RL/Vc/ADC والمقسم ونطاق الجهد والتحويل الأمثل وتوحيد Ω قبل أي Rs؛ اختبارات دوائر حساسات منفردة | `CN-MQ-01`، `CN-MQ-04`، `R0-PEER-01`، `TOOL-MQ-02` | physical Ohm/vout consistency, calibration traceability and fault-injection; if no physical reference, NEEDS_DATA |
| `01.2` | إعادة تصميم | P0 | R0 لكل جهاز/طور سخان/غاز مرجعي بوحدات ونسخ معايرة؛ استبعاد المتوسطات الخارجية وتدقيق تعارضات MQ7 وMQ8 | `R0-PEER-01`، `R0-PEER-02`، `R0-PEER-03`، `R0-PEER-04` | physical Ohm/vout consistency, calibration traceability and fault-injection; if no physical reference, NEEDS_DATA |
| `01.3` | إعادة تصميم | P0 | منحنى مع تعريف R0 والنطاق والغاز والطراز والاعتماد على الهواء؛ 43 منحنى رسوم مصانع ليست 43 معايرة حقيقية | `TOOL-MQ-01`، `CN-MQ-01`، `CN-MQ-02`، `CN-MQ-03` | physical Ohm/vout consistency, calibration traceability and fault-injection; if no physical reference, NEEDS_DATA |
| `01.4` | تطوير | P0 | تمييز تسجيل HIGH/LOW من تشغيل السخان؛ رفض PWM تجريبي غير موثق لـMQ7/MQ9 وتجهيز تجارب آمنة وفق المصنع | `CN-MQ-01`، `SYS-05`، `SYS-14`، `SYS-20` | physical Ohm/vout consistency, calibration traceability and fault-injection; if no physical reference, NEEDS_DATA |
| `01.5` | تطوير | P0 | تعويض T/RH مشروط بكل طراز وغاز/تركيز مع منع تسرب بيانات الجهاز؛ فصل الرطوبة عن الانجراف | `MQ-EXT-02`، `MQ-EXT-05`، `SYS-04`، `SYS-24` | physical Ohm/vout consistency, calibration traceability and fault-injection; if no physical reference, NEEDS_DATA |
| `01.6` | إعادة تصميم | P0 | تحويل جودة الإشارة إلى نموذج تدفق/مكوث/تطهير/انحياز سحب الهواء وحساس معيب وطبقة مرشح فيزيائي مشروطة | `SYS-11`، `SYS-18`، `SYS-22`، `SYS-23` | physical Ohm/vout consistency, calibration traceability and fault-injection; if no physical reference, NEEDS_DATA |
| `01.7` | تطوير | P1 | فصل استقرار مقاومة الهواء والانجراف المستمر والتعافي من أثر التعرض مع تسجيل صحة كل حساس | `UCI-EXT-270`، `MOS-EXT-17`، `JP-FIG-02` | physical Ohm/vout consistency, calibration traceability and fault-injection; if no physical reference, NEEDS_DATA |
| `02.1` | تطوير | P1 | فلتر سببي لحظي بأثر زمن معلن يحفظ قمم الإشارة وعدم استقرار السخان ولا ينظر إلى المستقبل | `MQ-EXT-10`، `MQ-EXT-02`، `SYS-17` | source/session-grouped split, missing/saturated channel tests, features-only train fitting |
| `02.2` | تطوير | P1 | توحيد قياس واعتماد وحدات Rs/R0 وADC حسب المصدر وتحجيم fitted على train فقط | `MQ-EXT-03`، `MQ-EXT-15`، `SYS-11` | source/session-grouped split, missing/saturated channel tests, features-only train fitting |
| `02.3` | تطوير | P1 | ميز peak/baseline/recovery وقناع missing وحالة المضخة والطور لكل سجل لا لكل صف | `MQ-EXT-10`، `MQ-EXT-11`، `JP-FIG-02` | source/session-grouped split, missing/saturated channel tests, features-only train fitting |

**تعريف إغلاق الدفعة:** عشر ملفات/تقنيات معرفات موزعة بالمرجع، وإثبات كود+اختبار أو سبب `NEEDS_DATA`، والتزامات وCI مصادق عليها وملف `BATCH01_EVIDENCE_AR.md`. لا ترقية G3 بلا قياسات Tank.

### الدفعة 02 — 02.4، 02.5، 02.6، 02.7، 02.8، 03.1، 03.2، 03.3، 03.4، 03.5

| المعرّف | نوع التغيير | الأولوية | العمل التنفيذي المطلوب | المراجع الرئيسة | معيار التحقق |
|---|---|---|---|---|---|
| `02.4` | تطوير | P0 | إضافة Segmental Average Differentiation الصينية وdynamic-deconvolution كتجارب مسجلة مقابل خصائص t10/t90/AUC الحالية | `CN-RSCH-01`، `SYS-17`، `JP-FIG-02`، `MQ-EXT-10` | source/session-grouped split, missing/saturated channel tests, features-only train fitting |
| `02.5` | إعادة تصميم | P0 | تصنيف fingerprint إلى مادة نقية/خليط/منتج/نشاط ثم محاذاة القنوات وقياس التكرار والاستقلال الفعلي | `MQ-EXT-03`، `MQ-EXT-10`، `MQ-EXT-11`، `MQ-EXT-15` | source/session-grouped split, missing/saturated channel tests, features-only train fitting |
| `02.6` | تطوير | P1 | 3D/4D ليست برهاناً بمجرد تصوير منحنيات؛ استخراج response–time–RH–concentration بتجارب متعددة العوامل | `TOOL-MQ-01`، `MQ-EXT-10`، `MOS-EXT-16` | source/session-grouped split, missing/saturated channel tests, features-only train fitting |
| `02.7` | تطوير | P1 | Nested grouped feature-selection وأثر إسقاط الحساسات لاختيار مصفوفة قوية بدلاً من معيار accuracy واحد | `SYS-01`، `SYS-04`، `CN-RSCH-02` | source/session-grouped split, missing/saturated channel tests, features-only train fitting |
| `02.8` | إعادة تصميم | P0 | فرض بيانات provenance source/licence/DOI/CAS وحقيقة ppm وR0/RL/طور/جلسة ونوع vector والفصل بين source_raw وderived | `MQ-EXT-03`، `MQ-EXT-15`، `UCI-EXT-270`، `R0-PEER-01` | source/session-grouped split, missing/saturated channel tests, features-only train fitting |
| `03.1` | إبقاء واختبار | P1 | PCA على مجموعات مختبرة وتفسير loadings مع قياس تأثير قيود الجهاز والبلانك | `CN-PAT-01`، `SYS-06`، `MQ-EXT-11` | class overlap & confusion, leave-gas/concentration-out, rank/condition or stability where applicable |
| `03.2` | تطوير | P1 | تشابه كيميائي مشروط باستجابة المصدر والظروف وترتيب الحساسات بدل similarity ثابت عبر المصفوفات | `MQ-EXT-03`، `MQ-EXT-11`، `QSPR-REF-09` | class overlap & confusion, leave-gas/concentration-out, rank/condition or stability where applicable |
| `03.3` | تطوير | P0 | قياس degeneracy والقدرة على الفصل بين عائلات كيميائية متشابهة عند تراكيز مجهولة | `QSPR-REF-09`، `SYS-07`، `MQ-EXT-15` | class overlap & confusion, leave-gas/concentration-out, rank/condition or stability where applicable |
| `03.4` | تطوير | P1 | LDA/PLS-DA مدربان فعلياً؛ nested holdout للمركب والجهاز والتركيز ومقارنة baseline لا مجرد embedding | `CN-PAT-01`، `SYS-06`، `MQ-EXT-15` | class overlap & confusion, leave-gas/concentration-out, rank/condition or stability where applicable |
| `03.5` | تطوير | P1 | أهمية كل حساس عبر drop-column على جلسات مستقلة وليس تفسير SHAP وحيد | `SYS-01`، `SYS-04`، `CN-RSCH-02` | class overlap & confusion, leave-gas/concentration-out, rank/condition or stability where applicable |

**تعريف إغلاق الدفعة:** عشر ملفات/تقنيات معرفات موزعة بالمرجع، وإثبات كود+اختبار أو سبب `NEEDS_DATA`، والتزامات وCI مصادق عليها وملف `BATCH02_EVIDENCE_AR.md`. لا ترقية G3 بلا قياسات Tank.

### الدفعة 03 — 03.6، 03.7، 03.8، 04.1، 04.2، 04.3، 04.4، 04.5، 04.6، 04.7

| المعرّف | نوع التغيير | الأولوية | العمل التنفيذي المطلوب | المراجع الرئيسة | معيار التحقق |
|---|---|---|---|---|---|
| `03.6` | تطوير | P1 | اختيار أقل عدد حساسات مع ثبات النتائج تحت الرطوبة/التركيز وزمن الاستجابة | `SYS-01`، `SYS-04`، `MQ-EXT-11` | class overlap & confusion, leave-gas/concentration-out, rank/condition or stability where applicable |
| `03.7` | تطوير | P0 | اختبار الخلط بين هوية الغاز وشدة التعرض بفصل تركيزات المرجع وحجبها والتمييز بين liquid v/v وgas ppm | `MQ-EXT-10`، `MQ-EXT-15`، `UCI-EXT-270` | class overlap & confusion, leave-gas/concentration-out, rank/condition or stability where applicable |
| `03.8` | إعادة تصميم | P0 | إضافة تشخيص رتبة Jacobian/condition number وبصمة الالتباس قبل التصنيف أو الفصل الكمي وإخراج NON_IDENTIFIABLE | `SYS-07`، `SYS-08`، `SYS-09`، `QSPR-REF-09` | class overlap & confusion, leave-gas/concentration-out, rank/condition or stability where applicable |
| `04.1` | تطوير | P1 | منحنيات وحساسات من نفس المصنع مع نقاط تحقق فيزيائي؛ لا تصنيف اعتماداً على ملاءمة ستة منحنيات منشأها الرسم نفسه | `TOOL-MQ-01`، `CN-MQ-01`، `R0-PEER-01` | grouped macro-F1, per-class confusion, calibration/abstention, no true-test-gas leakage |
| `04.2` | تطوير | P1 | فصل بصمات الحالات المركبة عن الغاز النقي؛ دعم masked channels/unknown/valid-domain | `MQ-EXT-03`، `MQ-EXT-11`، `MQ-EXT-15` | grouped macro-F1, per-class confusion, calibration/abstention, no true-test-gas leakage |
| `04.3` | إبقاء واختبار | P2 | KNN baseline ثابت مع group splits وضبط k على التدريب فقط | `SYS-06`، `MQ-EXT-03` | grouped macro-F1, per-class confusion, calibration/abstention, no true-test-gas leakage |
| `04.4` | إبقاء واختبار | P2 | Logistic baseline وتحقق probability calibration مع حجب تركيزات مماثلة | `MQ-EXT-15`، `UCI-EXT-270` | grouped macro-F1, per-class confusion, calibration/abstention, no true-test-gas leakage |
| `04.5` | تطوير | P1 | SVM موجود في benchmark؛ قابلية نقل نفس المعالجة إلى technique module قبل اعتماده، مع مقارنة خطي/RBF | `SYS-01`، `CN-RSCH-02`، `MQ-EXT-15` | grouped macro-F1, per-class confusion, calibration/abstention, no true-test-gas leakage |
| `04.6` | تطوير | P1 | RF وExtraTrees ومقارنة أهمية القنوات مع عدم تكرار نفس UCI322 كمصدر جديد | `SYS-13`، `SYS-04` | grouped macro-F1, per-class confusion, calibration/abstention, no true-test-gas leakage |
| `04.7` | تطوير | P1 | XGBoost حقيقي في benchmark؛ تحقق source-heldout ونسخ النموذج وإخفاقاته قبل نقله إلى تنفيذ معتمد | `SYS-01`، `MQ-EXT-15` | grouped macro-F1, per-class confusion, calibration/abstention, no true-test-gas leakage |

**تعريف إغلاق الدفعة:** عشر ملفات/تقنيات معرفات موزعة بالمرجع، وإثبات كود+اختبار أو سبب `NEEDS_DATA`، والتزامات وCI مصادق عليها وملف `BATCH03_EVIDENCE_AR.md`. لا ترقية G3 بلا قياسات Tank.

### الدفعة 04 — 04.8، 04.9، 04.10، 05.1، 05.2، 05.3، 05.4، 05.5، 05.6، 05.7

| المعرّف | نوع التغيير | الأولوية | العمل التنفيذي المطلوب | المراجع الرئيسة | معيار التحقق |
|---|---|---|---|---|---|
| `04.8` | تطوير | P1 | BP/RBF وMLP متعدد المخارج مع تتبع leakage ومصدر المنشأة والتحديات من الصين | `CN-RSCH-01`، `CN-RSCH-03`، `CN-RSCH-04` | grouped macro-F1, per-class confusion, calibration/abstention, no true-test-gas leakage |
| `04.9` | إعادة تصميم | P0 | Physics+ML غير مكتمل للاستدلال المجهول: forward molecular candidates/Rs-response/unknown c + QSPR PubChem وجسر أجهزة المرجع | `QSPR-REF-01`، `QSPR-REF-03`، `QSPR-REF-04`، `QSPR-REF-05` | grouped macro-F1, per-class confusion, calibration/abstention, no true-test-gas leakage |
| `04.10` | تطوير | P0 | بروتوكول benchmark مشترك لبيانات المصدر والبلانك والجلسة/الجهاز والتراكيز مع بيانات عدم الدلالة الإحصائية | `MQ-EXT-15`، `UCI-EXT-270`، `SYS-10`، `SYS-11` | grouped macro-F1, per-class confusion, calibration/abstention, no true-test-gas leakage |
| `05.1` | إعادة تصميم | P0 | Single-sensor ppm مشروط بالغاز/R0 ومعايرة مستقلة؛ يمنع تحويل ADC أو رسوم المصنع إلى حد تعرض | `CN-MQ-01`، `CN-MQ-04`، `R0-PEER-01`، `TOOL-MQ-02` | per-component RMSE/MAE/bias across held-out ranges, LOQ/blank and confidence/error intervals |
| `05.2` | تطوير | P0 | معايرة متعددة نقاط بمرجع ppb/ppm معلوم وفصل الصفر/البلانك وإعادة الإنتاج بين أيام/الأجهزة | `MQ-EXT-15`، `UCI-EXT-270`، `MOS-EXT-16` | per-component RMSE/MAE/bias across held-out ranges, LOQ/blank and confidence/error intervals |
| `05.3` | تطوير | P1 | مقارنة nonlinear inverse curve/log cross terms ومعايير max residual/valid domain/ill-conditioning | `SYS-07`، `SYS-08`، `TOOL-MQ-01` | per-component RMSE/MAE/bias across held-out ranges, LOQ/blank and confidence/error intervals |
| `05.4` | إعادة تصميم | P0 | Joint regression تقدّر ppm لكل مركب بمشاهدات خلائط مرجعية، لا coefficients غير معايرة أو نقاط سائلة | `SYS-06`، `SYS-07`، `SYS-10`، `CN-RSCH-04` | per-component RMSE/MAE/bias across held-out ranges, LOQ/blank and confidence/error intervals |
| `05.5` | تطوير | P1 | PLS/PLS2 مع validation لكل مكون وfeature scaling على التدريب فقط | `SYS-06`، `SYS-11`، `CN-PAT-01` | per-component RMSE/MAE/bias across held-out ranges, LOQ/blank and confidence/error intervals |
| `05.6` | تطوير | P1 | Multioutput RF/XGBoost مع quantile intervals وتقييم linearly vs nonlinear interactions | `SYS-04`، `SYS-15` | per-component RMSE/MAE/bias across held-out ranges, LOQ/blank and confidence/error intervals |
| `05.7` | إعادة تصميم | P0 | تقدير التركيز بعد احتمال الهوية/الخليط، مع marginalization على احتمالات المرشحين بدل تمرير true gas label | `QSPR-REF-03`، `SYS-03`، `SYS-07` | per-component RMSE/MAE/bias across held-out ranges, LOQ/blank and confidence/error intervals |

**تعريف إغلاق الدفعة:** عشر ملفات/تقنيات معرفات موزعة بالمرجع، وإثبات كود+اختبار أو سبب `NEEDS_DATA`، والتزامات وCI مصادق عليها وملف `BATCH04_EVIDENCE_AR.md`. لا ترقية G3 بلا قياسات Tank.

### الدفعة 05 — 05.8، 05.9، 06.1، 06.2، 06.3، 06.4، 06.5، 06.6، 06.7، 06.8

| المعرّف | نوع التغيير | الأولوية | العمل التنفيذي المطلوب | المراجع الرئيسة | معيار التحقق |
|---|---|---|---|---|---|
| `05.8` | إعادة تصميم | P0 | LOD/LOQ عتبات blank/reference detection وuncertainty لا أدلة UCI source threshold المبدئية | `MQ-EXT-15`، `R0-PEER-01`، `CN-MQ-01` | per-component RMSE/MAE/bias across held-out ranges, LOQ/blank and confidence/error intervals |
| `05.9` | تطوير | P1 | حجب تراكيز كاملة واختبار الاستقراء وتقرير OUT_OF_CALIBRATION بدلاً من رقم ppm زائف | `UCI-EXT-270`، `MQ-EXT-15` | per-component RMSE/MAE/bias across held-out ranges, LOQ/blank and confidence/error intervals |
| `06.1` | إعادة تصميم | P0 | Evidence fusion مع نموذج ترابط الحساسات والنماذج لمنع مضاعفة evidence ذات المصدر الواحد | `SYS-06`، `QSPR-REF-03`، `TOOL-MQ-01` | Brier/NLL/ECE, risk-coverage, OOD false acceptance, dependence ablation |
| `06.2` | تطوير | P1 | Bayesian joint likelihood calibrated by environmental condition and confounders؛ prior لا يصبح evidence مضاعف | `QSPR-REF-03`، `SYS-07` | Brier/NLL/ECE, risk-coverage, OOD false acceptance, dependence ablation |
| `06.3` | تطوير | P1 | Platt/isotonic/temperature scaling على مجموعة calibration منفصلة وECE/Brier/NLL | `MQ-EXT-15`، `UCI-EXT-270` | Brier/NLL/ECE, risk-coverage, OOD false acceptance, dependence ablation |
| `06.4` | تطوير | P1 | Conformal set مع توضيح exchangeability coverage under drift وعدم ضمان التغطية الميدانية | `UCI-EXT-270`، `MOS-EXT-17` | Brier/NLL/ECE, risk-coverage, OOD false acceptance, dependence ablation |
| `06.5` | إعادة تصميم | P0 | Reject gas unseen, overlapping chemistry and novel complex products؛ calibrate threshold on gas-heldout only | `QSPR-REF-03`، `QSPR-REF-09`، `MQ-EXT-11` | Brier/NLL/ECE, risk-coverage, OOD false acceptance, dependence ablation |
| `06.6` | تطوير | P1 | كشف OOD/unknown physical fault separate; heldout temp,concentration,device and gas | `UCI-EXT-270`، `MOS-EXT-17`، `MQ-EXT-15` | Brier/NLL/ECE, risk-coverage, OOD false acceptance, dependence ablation |
| `06.7` | تطوير | P0 | conflicts when concentration/temperature/heater invalid; evidence negative is not independent zero detection | `R0-PEER-01`، `SYS-04`، `JP-FIG-02` | Brier/NLL/ECE, risk-coverage, OOD false acceptance, dependence ablation |
| `06.8` | إبقاء واختبار | P2 | SHAP interpretability audit only, not scientific proof; reference sampling and correlated features caveat | `QSPR-REF-05`، `QSPR-REF-07` | Brier/NLL/ECE, risk-coverage, OOD false acceptance, dependence ablation |

**تعريف إغلاق الدفعة:** عشر ملفات/تقنيات معرفات موزعة بالمرجع، وإثبات كود+اختبار أو سبب `NEEDS_DATA`، والتزامات وCI مصادق عليها وملف `BATCH05_EVIDENCE_AR.md`. لا ترقية G3 بلا قياسات Tank.

### الدفعة 06 — 06.9، 07.1، 07.2، 07.3، 07.4، 07.5، 07.6، 07.7، 07.8، 07.9

| المعرّف | نوع التغيير | الأولوية | العمل التنفيذي المطلوب | المراجع الرئيسة | معيار التحقق |
|---|---|---|---|---|---|
| `06.9` | تطوير | P0 | abstention on conditional risk, coverage, missing RL/R0, and failure/out-of-domain cases | `SYS-07`، `QSPR-REF-09` | Brier/NLL/ECE, risk-coverage, OOD false acceptance, dependence ablation |
| `07.1` | إبقاء واختبار | P1 | Group whole session / day, avoid random split of temporal rows across training-test | `MQ-EXT-02`، `MQ-EXT-10`، `MQ-EXT-11` | chronological/leave-device/session-out, calibration frozen, report degradation and negative transfer |
| `07.2` | إعادة تصميم | P0 | cross-device checks need physical sensor_instance_id; distinct experimental unit not batch identity or duplicate file | `MQ-EXT-12`، `MOS-EXT-18`، `UCI-EXT-270` | chronological/leave-device/session-out, calibration frozen, report degradation and negative transfer |
| `07.3` | تطوير | P1 | leave concentration groups whole + low concentration below detection interpretability | `MQ-EXT-15`، `UCI-EXT-270` | chronological/leave-device/session-out, calibration frozen, report degradation and negative transfer |
| `07.4` | تطوير | P1 | online drift alert with blank/reference and delayed response monitor, not re-training during hazardous exposure | `MOS-EXT-17`، `UCI-EXT-270` | chronological/leave-device/session-out, calibration frozen, report degradation and negative transfer |
| `07.5` | تطوير | P1 | OSC/orthogonal correction and physical reference drift; evaluate negative transfer in chronological batches | `SYS-24`، `UCI-EXT-270`، `MOS-EXT-17` | chronological/leave-device/session-out, calibration frozen, report degradation and negative transfer |
| `07.6` | تطوير | P1 | domain adaptation conditional on MQ vs TGS and humidity/device, never blind transfer of quantitative R0 | `MOS-EXT-18`، `UCI-EXT-270` | chronological/leave-device/session-out, calibration frozen, report degradation and negative transfer |
| `07.7` | تطوير | P1 | transfer learning with sample efficiency and leave-device/source evaluation; report negative transfer | `MOS-EXT-18`، `JP-FIG-01` | chronological/leave-device/session-out, calibration frozen, report degradation and negative transfer |
| `07.8` | إعادة تصميم | P1 | active learning/DoE as experiment-loop selecting gas, concentration, humidity, run and standard result before model update | `CN-RSCH-02`، `SYS-04`، `MQ-EXT-15` | chronological/leave-device/session-out, calibration frozen, report degradation and negative transfer |
| `07.9` | تطوير | P2 | continual model only after external verification + drift audit; lock baseline and allow rollback | `MOS-EXT-17`، `UCI-EXT-270` | chronological/leave-device/session-out, calibration frozen, report degradation and negative transfer |

**تعريف إغلاق الدفعة:** عشر ملفات/تقنيات معرفات موزعة بالمرجع، وإثبات كود+اختبار أو سبب `NEEDS_DATA`، والتزامات وCI مصادق عليها وملف `BATCH06_EVIDENCE_AR.md`. لا ترقية G3 بلا قياسات Tank.

### الدفعة 07 — 07.10، 08.1، 08.2، 08.3، 08.4، 08.5، 08.6، 08.7، 08.8، 08.9

| المعرّف | نوع التغيير | الأولوية | العمل التنفيذي المطلوب | المراجع الرئيسة | معيار التحقق |
|---|---|---|---|---|---|
| `07.10` | تطوير | P1 | benchmark CPU/memory/power on actual Android and MCU; NN training stays lab; TinyML only when measurable | `SYS-20`، `TOOL-MQ-01` | chronological/leave-device/session-out, calibration frozen, report degradation and negative transfer |
| `08.1` | تطوير | P1 | CNN Conv1d trained benchmark -> versioned module parity; feature attribution and group holdout | `SYS-01`، `MQ-EXT-10` | per component ppm error, identity/mix confusion, rank, source group holds and complexity |
| `08.2` | تطوير | P1 | TCN strictly causal, with future/purge masks and heldout experimental sessions | `SYS-17`، `MQ-EXT-10` | per component ppm error, identity/mix confusion, rank, source group holds and complexity |
| `08.3` | تطوير | P1 | GRU/LSTM separately with correct masks and time series; avoid tuning on final UCI benchmark | `SYS-01`، `CN-PAT-03`، `MQ-EXT-12` | per component ppm error, identity/mix confusion, rank, source group holds and complexity |
| `08.4` | تطوير | P2 | GNN learned topology must improve independent heldout results over identity graph; otherwise deprioritize | `SYS-01`، `MOS-EXT-18` | per component ppm error, identity/mix confusion, rank, source group holds and complexity |
| `08.5` | تطوير | P1 | temporal ensemble prevent reuse of correlated model outputs as independent confidence | `SYS-17`، `CN-RSCH-01` | per component ppm error, identity/mix confusion, rank, source group holds and complexity |
| `08.6` | إعادة تصميم | P1 | first-order adsorption/kinetic simulator conditioned on humidity/heater/chamber airflow; synthetic tests ≠ validation | `SYS-17`، `SYS-23`، `TOOL-MQ-01` | per component ppm error, identity/mix confusion, rank, source group holds and complexity |
| `08.7` | إعادة تصميم | P1 | simulate-to-real requires matched Rs/R0, heater trace, chamber, gas reference, and source provenance | `SYS-17`، `SYS-05`، `MOS-EXT-16` | per component ppm error, identity/mix confusion, rank, source group holds and complexity |
| `08.8` | إعادة تصميم | P0 | simple fingerprint residual is ambiguity flag, not mixture proof; compare against unknown gas, faults and drift with heldout mixtures | `SYS-02`، `SYS-11`، `SYS-12`، `SYS-13` | per component ppm error, identity/mix confusion, rank, source group holds and complexity |
| `08.9` | إعادة تصميم | P0 | replace two-component fixed linear grid with identifiability-gated NNLS, nonlinear coupled response, MCR-ALS, sparse priors, confidence; not guaranteed physical separation | `SYS-06`، `SYS-07`، `SYS-08`، `SYS-09` | per component ppm error, identity/mix confusion, rank, source group holds and complexity |

**تعريف إغلاق الدفعة:** عشر ملفات/تقنيات معرفات موزعة بالمرجع، وإثبات كود+اختبار أو سبب `NEEDS_DATA`، والتزامات وCI مصادق عليها وملف `BATCH07_EVIDENCE_AR.md`. لا ترقية G3 بلا قياسات Tank.

### الدفعة 08 — 08.10، 08.11، 08.12، 09.1، 09.2، 09.3، 09.4، 09.5، 09.6، 10.1

| المعرّف | نوع التغيير | الأولوية | العمل التنفيذي المطلوب | المراجع الرئيسة | معيار التحقق |
|---|---|---|---|---|---|
| `08.10` | إعادة تصميم | P0 | multi-analyte ppm with multioutput ML and per-analyte calibration/reference chemistry, abstain on non-identifiable composition | `SYS-06`، `SYS-10`، `SYS-15`، `CN-RSCH-04` | per component ppm error, identity/mix confusion, rank, source group holds and complexity |
| `08.11` | إعادة تصميم | P0 | interference vs adsorption/filter breakthrough/compound selectivity in controlled chamber; risk physical filter removes target gas | `SYS-18`، `SYS-19`، `SYS-23`، `SYS-04` | per component ppm error, identity/mix confusion, rank, source group holds and complexity |
| `08.12` | تطوير | P1 | complex food/wood/vegetable headspace must be classified as product not pure gas and cannot borrow ppm | `MQ-EXT-01`، `MQ-EXT-02`، `MQ-EXT-11`، `MQ-EXT-13` | per component ppm error, identity/mix confusion, rank, source group holds and complexity |
| `09.1` | تطوير | P2 | robotic olfaction requires physical gas plume and time/location context; no claim chemical identity from pose alone | `SYS-12`، `SYS-23` | distance/localization error, information gain per energy/time, false plume hotspots |
| `09.2` | تطوير | P2 | gas mapping with sensor response latency, baseline and uncertainty; pressure/flow bias | `SYS-12`، `SYS-17` | distance/localization error, information gain per energy/time, false plume hotspots |
| `09.3` | تطوير | P2 | source localization by temporal/plume cues only with wind/reference position and independent known point source | `SYS-12`، `SYS-23` | distance/localization error, information gain per energy/time, false plume hotspots |
| `09.4` | إعادة تصميم | P1 | active sampling should include pump on/off, controlled purge, residence time and cost/risk, not simple waypoint rankings | `SYS-11`، `SYS-23`، `CN-RSCH-02` | distance/localization error, information gain per energy/time, false plume hotspots |
| `09.5` | تطوير | P1 | spatiotemporal fusion + deconvolution of sensor lag without confusing recovery tail with fresh gas | `SYS-17`، `SYS-12` | distance/localization error, information gain per energy/time, false plume hotspots |
| `09.6` | تطوير | P2 | motion decisions require confidence gate, fail safe and separate obstacle control from chemical unverified inferences | `SYS-23`، `SYS-12` | distance/localization error, information gain per energy/time, false plume hotspots |
| `10.1` | إعادة تصميم | P0 | negative evidence: no independent sensor likelihood multiplication unless correlation tested; no absolute exclusion without calibration | `QSPR-REF-09`، `R0-PEER-01`، `SYS-07` | false exclusions/coverage under heldout unknown gas/environment/device |

**تعريف إغلاق الدفعة:** عشر ملفات/تقنيات معرفات موزعة بالمرجع، وإثبات كود+اختبار أو سبب `NEEDS_DATA`، والتزامات وCI مصادق عليها وملف `BATCH08_EVIDENCE_AR.md`. لا ترقية G3 بلا قياسات Tank.

## 4. طريقة تطوير المسارات العلمية

**العقد الفيزيائي (01):** حساب Rs من الجهد/المقاومة مع وحدة Ω وتحقق RL/mux/divider؛ R0 خاص بالحساس الفردي وطور السخان؛ لا تستعمل متوسطات R0 لدراسات الآخرين. أما MQ7/MQ9 فتحمل وسماً لطور التسخين HIGH/LOW وزمن الاستقرار؛ لا يستنتج الملف جهد التشغيل من الاسم.

**الإشارات والهوية (02–04):** مقارنات على بصمات زمنية حقيقية مع فصل البيانات المشتقة عن التعرضات الفعلية. مستخلص زمني صيني `Segmental Average Differentiation` يجب مقارنته بخط أساس t10/t90 الموجود تحت 02.4. QSPR/PubChem في 04.9 يختبر المرشحين فرداً فرداً، دون أن يتلقى النموذج واصف المادة الصحيحة من test label.

**المخاليط والكم (05/08):** لا يتحول معامل حل خطي للمتجه إلى ppm. أولاً تحقق من الرتبة والتداخل في 03.8، ثم تعلم NNLS / cross-sensitivity nonlinear / MCR-ALS / PLS2 أو مقارنة multioutput على مخاليط تمتلك ppm مستقلاً لكل مكوّن وبلانكاً. نتيجة `NOT_IDENTIFIABLE` ضرورية عند نقص المرجعيات.

**الثقة والمرونة (06/07/10):** لا تجمع أدلة 8 حساسات مترابطة كاحتمالات مستقلة، ولا تُدخل حالات OOD في خليط معلوم، ولا تستخدم عدم الكشف كاستبعاد قطعي قبل تعلم detection power مشروطاً بالغاز والتركيز والرطوبة وزمن الاستجابة.

**حركة الروبوت (09):** قياس وقت مكوث الغاز وغسل الحجرة وموقع الاستشعار وزمن تأخر الحساس وتسجيل حالة مروحة السحب، ومقارنة مصدر مكاني مستقل قبل السماح بوضعيات أخذ عينات نشطة؛ لا تعديل لوحدات التحكم في الروبوت بهذا العمل.

## 5. الدفعة الأولى: ترتيب عملي وقيود

**ابتداءً من 01.1 → 01.2 → 01.3 → 01.4 → 01.5 → 01.6 → 01.7 → 02.1 → 02.2 → 02.3**. نصمم أولاً منطق سلامة المدخلات الذي يحتاجه الجميع، ثم نتبعه بتجارب مصدر مستقلة. نبدأ الآن 01.1/01.2 بفحص حسابات `Rs` والاشتراطات الإلزامية لوحدة R0، مع كتابة اختبارات وتصنيف حالتها. تطوير البقية يبقى `PENDING` حتى تنجح اختبارات كل منها، ولا نعلن اكتمال الدفعة الأولى استباقاً.

**المصادر المهمة للدفعة:** رسوم ومراجع Hanwei MQ2/MQ4/MQ5/MQ8، سجلات R0 الثمانية في المكتبة، ملف `R0_RS_CROSS_STUDY_2026-10-21`، وسجلات MQ7/MQ9 الطورية، وتجارب MQ الخارجية التي لم تغط متطلبات MQ8/Tank.

## 6. ملفات استئناف العمل

- `PLAN_80_V4_AR.md` هذه الخطة البشرية و`PLAN_80_V4.json` خطة الـ80 الآلية.
- `EXECUTION_LEDGER_2026-10-21.json`: سجّل لكل تقنية المرحلة المنفذة، ملفات الاختبار، SHA وCI وسبب العائق.
- `BATCH01_EVIDENCE_AR.md`: تقرير الدفعة الأولى، حالات PASS/BLOCKED/FAIL بالأدلة؛ يبدأ بـ01.1/01.2.
- `RESUME.md`: **أحدث نقطة استئناف في أعلى الملف**؛ يذكر الفرع، SHA، الاختبار الأخير، والخطوة التالية المحددة.
- المرجع الأصلي: `research/RESUME.md` و`research/PROGRESS_80.json` يبقيان تاريخ G1/G2، ولا نعيد كتابة الأرقام دون أدلة.

**نقطة التوقف المشروعة:** عندما لا يوجد مرجع غاز معاير من Tank اكتب `G3_BLOCKED_MISSING_TANK_PHYSICAL_DATA` بدل تعويضه ببيانات TGS أو محاكاة أو رسوم شركة. أصل المسألة تطوير علمي لا مجرد رفع نجاح CI.
