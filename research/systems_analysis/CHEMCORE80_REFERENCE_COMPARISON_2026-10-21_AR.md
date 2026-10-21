# HazRob Tank — التحليل النظمي للتقنيات الثمانين في ضوء مكتبة المراجع الموحدة
**21 أكتوبر 2026 — دراسة تصميمية علمية، ليست تعديلاً على محرك ChemCore الإنتاجي.**

## مصادر الحقيقة

- **حصر التقنيات:** [PROGRESS_80.json](../PROGRESS_80.json) و[MASTER_PLAN.md](../MASTER_PLAN.md) في الفرع `main`؛ 80 معرفاً، اختبارات G1 تاريخية 1261، وG2 تمت مراجعتها 80/80، وG3 الحقيقي **0/80**.
- **المكتبة المرجعية:** [الفهرس المعتمد في منظمة HazRob](https://github.com/HazRob/build-harness/blob/main/library/reference-bank/INDEX.md)؛ **80 سجل مراجع** بعد إضافة 8 دراسات R0/Rs. المصادفة العددية لا تعني تطابق كل مرجع مع تقنية مستقلة.
- **مصفوفة المقارنة التفصيلية الآلية:** [CHEMCORE80_REFERENCE_GAP_MATRIX_2026-10-21.json](CHEMCORE80_REFERENCE_GAP_MATRIX_2026-10-21.json)؛ جميع المعرفات الثمانين مربوطة بمراجع علمية مختارة وقرار وتصميم بديل واختبار قبول.
- **بناء الخوارزميات البحثي السابق:** [V3 checkpoint draft PR#3](https://github.com/HazRob/HazRob-Tank/blob/research/chemcore80-v3-implementation-20261021/research/V3_IMPLEMENTATION_CHECKPOINT_2026-10-21_AR.md)؛ لم يندمج في `main`. حافظ على نتائج التدريب والمخاطر دون إعادة وصفها كإصدار تشغيلي.

## 1. خلاصة رقمية دقيقة

| القرار التصميمي | عدد التقنيات | ماذا يعني؟ |
|---|---:|---|
| **إعادة تصميم** | **26** | يلزم تعديل تمثيل المدخلات/المخرجات أو الفرضيات/المعايرة أو الاستدلال، وليس مجرد إضافة اختبار |
| **تطوير** | **33** | الخوارزمية أو دورها مناسب، لكن يلزم توسيع الميزات أو تصميم تحقق أقوى |
| **الإبقاء على النهج** | **14** | يوجد منطق صحيح أو baseline فعلي؛ لا يزال يلزم تحقق ميداني قبل التشغيل |
| **تأجيل** | **7** | فائدة إضافية ضعيفة الآن أو بيانات ميدانية/مكان/عتاد غير متوافرة |
| **الإجمالي** | **80** | لا توجد تقنية مُعتمدة علمياً على المصفوفة الفيزيائية الثمانية (G3=0/80) |

### توزيع القرارات على المراحل

| المرحلة | عددها | إعادة تصميم | تطوير | إبقاء | تأجيل |
|---|---:|---:|---:|---:|---:|
| 01 — القياس ومعايرة الحساسات | 7 | 4 | 3 | 0 | 0 |
| 02 — تجهيز الإشارة والبصمات | 8 | 2 | 4 | 2 | 0 |
| 03 — قابلية التمييز | 8 | 2 | 4 | 2 | 0 |
| 04 — التصنيف والتعرف | 10 | 3 | 2 | 5 | 0 |
| 05 — التقدير الكمي للغازات | 9 | 5 | 4 | 0 | 0 |
| 06 — دمج الأدلة وعدم اليقين | 9 | 3 | 5 | 1 | 0 |
| 07 — الانجراف والموثوقية | 10 | 1 | 7 | 1 | 1 |
| 08 — الشبكات والخلائط | 12 | 4 | 4 | 3 | 1 |
| 09 — الشم الروبوتي والموقع | 6 | 1 | 0 | 0 | 5 |
| 10 — الأدلة السلبية | 1 | 1 | 0 | 0 | 0 |

## 2. أهم إعادة التصميم الهندسي — مرجع ومنهج وسبب

### أ. كيمياء القياس والتعريف الصحيح لـR0: 01.1–01.6، 05.1–05.2

قيم R0 المنشورة من دراسات أجهزة MQ فردية مختلفة جداً: MQ9 يقترب في مصدرين (8.062 و8.21kΩ)، لكن MQ135 يوجد 23.35 و29.74 و110 و37kΩ حسب الجهاز وطريقة المعايرة؛ دراسة Dorcea 2018 فيها تعارض عددي MQ7/MQ8. لذلك: `R0` لا يُختار كرقم نموذجي لكل طراز، ولا تُنقل منحنيات MQDataScience إلى ppm مضمون، ولا يوحّد طور MQ7/MQ9 دون دليل التشغيل. يجب توقيع كل سجل `sensor_instance_id + heater_phase + RL + R0_definition + gas_standard + environment + time`.

المراجع: [Dorcea et al. 2018](https://doi.org/10.1109/SIITME.2018.8599253)، [تدقيق R0/Rs المركزي](https://github.com/HazRob/build-harness/blob/main/library/reference-bank/external-research/calibration_audits/MQ_R0_RS_PEER_COMPARISON_2026-10-21_AR.md)، `CN-MQ-01..04` و`TOOL-MQ-01`.

### ب. الفصل الحقيقي بين مكونات المخاليط: 03.8 و05.4 و05.5 و08.8–08.10

التقنيات الحالية تسمي unmixing/multi-analyte؛ هذا أوسع من مُصنف يقرر هل توجد مادتان. يجب فصل ثلاثة مسارات: **كشف وجود خليط**، **تحديد هويات المكونات**، ثم **تقدير ppm مستقل لكل مادة**. اختبار **قابلية التعيين identifiability** قبل أي تقدير كمي؛ إذا كانت استجابات غازين متقاربة على حساساتنا فلا يتيح أي نموذج استرجاع تركيزهما بثقة من مصفوفة غير كافية.

- **MCR-ALS**: تحلل مصفوفة الإشارات إلى مساهمات وتواقيع كامنة `X≈CSᵀ+E` مع قيود لا سلبية/عدد المكونات، بشرط مراجعة فرضية الخطية والتفاعل؛ مقال [Montoliu et al. 2010](https://doi.org/10.1016/j.snb.2009.12.051) استخدم بيانات تركيبية ثم اختبارات MOS حقيقية لخلائط CO/CH4 وسجل حدود الطريقة.
- **Nonnegative and sparse nonlinear inversion**: حل تركيزات `c_i>=0` باستخدام منحنى استجابة مشروط بالبيئة والسخان، بعقوبة تعقيد وجرد احتمالات متكافئة؛ مقارنة `SYS-07` و`SYS-08`.
- **Multioutput regression**: تعلّم تركيز كل عنصر من منحنيات موثقة وإبلاغ `MAE/RMSE` وcoverage لكل عنصر على حدة؛ `SYS-06` و`SYS-10`.
- **مجموعة اختبار UCI322**: 16 قناة TGS وملفان مستمران كل منهما 12 ساعة، نحو 4.18 مليون **صف زمني**، لا ملايين التعرضات المستقلة. أحد الملفين خليط Ethylene/CO والآخر Ethylene/CH4؛ مع `ppm` setpoint لكلا المكونين. لا تستعمل تجزئة عشوائية للصفوف كمصدر لقوة تعميم خارج التجربة.

### ج. QSPR/PubChem والاستدلال العكسي: 04.9 + 06 + 08

الفرق بين التنبؤ الأمامي `properties(molecule, sensor, concentration, T/RH) → response` والتنبؤ العكسي `unknown response → candidate ranking` جوهري؛ لا يكفي إعطاء واصفات الجزيء الصحيح للسجل المختبر، لأنه تسرب لهوية الاختبار. نبدأ داخل `04.9` باستخدام قناة MQ135 الخارجية في UCI1081، بتوليد جميع المرشحين (وليس المرشح الحقيقي فقط) والمقارنة بأسلوب train-only. المراجع `QSPR-REF-01..10`، ولا يوجد بعد إثبات أداء على Tank MQ8.

### د. بصمات الزمن والانتقائية: 01.4 و01.6 و02.4 و08.6 و08.11 و09.4

تحتاج المنظومة إلى حالة زمنية موحدة تدون حجم غرفة الاستشعار/تدفق المروحة/التوقف/غسيل الهواء/الذاكرة المتبقية ومرحلة السخان. `SYS-17` يعالج التأخر الزمني وفك الاستجابة المستشعرة، `CN-RSCH-01` يقدم ملامح الفرق بين المقاطع، و`SYS-11` يعدل تدفق العينة. تستخدم `SYS-18/19` مرشحات فيزيائية ومناخل، وهذا **تعديل للهواء قبل الحساس** لا مجرد مرشح رقمي؛ يتطلب عتاداً ودراسة اختراق/امتصاص هدف وتغيير RH ولا يجوز افتراضه ضمن Tank الحالي.

### هـ. جودة البصمة ودليلها: 02.5 و02.8 و04.10 و06.1 و06.9 و10.1

يجب منع خلط `PURE_COMPOUND` و`BINARY_MIXTURE_REFERENCED` و`COMPLEX_HEADSPACE` و`SOURCE_TRANSFER` و`SIMULATED_ONLY`. كما يجب أن يعترف Fusion بارتباط الأدلة الناتجة من منحنى المصنع نفسه وبصمة مشتقة منه؛ لا تُجمع درجات متطابقة المصدر كأنها أدلة مستقلة. يلزم الامتناع عن التعرف وعدم استبعاد غاز بمجرد ضعف استجابة حسّاس تداخلي.

## 3. هل نضيف تقنيات 81 فما بعد؟

**لا نزيد العدد الآن.** توجد أربعة مرشحات جديرة بقرار لاحق بعد تجربة مقارنة وليس إضافة أرقام شكلية:

| المرشح | المرجع داخل 80 | سبب عدم إضافته الآن | الشرط الذي يبرر جعله تقنية جديدة |
|---|---|---|---|
| الفصل الفيزيائي/الترشيح الانتقائي أو preconcentration | 01.6،08.11،09.4 | لا يوجد في Tank مرشح/صمام bypass مُختبر | وحدة فعلية + غازات معيارية قبل/بعد المرشح وبيانات انكسار/تشبع |
| الاستدلال الجزيئي العكسي المفتوح بواسطة PubChem/QSPR | 04.9،06.2،06.5 | لا يوجد تحقق مقارن مستقل على MQ8 | تحسن ملحوظ في top-k/open-set على جزيئات محجوبة دون leakage |
| محرك ميزانية عدم اليقين والمرجعية القياسية | 01.1،01.2،02.8،05.8 | وظيفة عابرة للتقنيات أكثر منها مصنفاً | سلسلة مستقلة مرجعية للمعايرة والقياس وLOD بمتطلبات قبول معلنة |
| فك الديناميكيات وتعقب تغير الخليط online | 02.4،08.6،08.9،09.5 | يمكن تطويره داخل المسارات الحالية | نموذج سببي متفوق بوضوح على البديل تحت تغير مكونات مرجعية مفاجئ |

أي إضافة جديدة تتطلب `ADR` باسم ورقم مستقل ومقارنةً إلى أبسط بديل ضمن الثمانين، وتكون `EXPERIMENTAL` قبل التشغيل. وجود ورقة أو براءة لا يجعل التقنية معتمدة ولا يبرر الترقيم.

## 4. الترتيب الصحيح لتجارب التحقق والاستثمار

| الدفعة | أولويات البحث | البيانات والتجربة | المخرج ومعيار قبول البحث |
|---|---|---|---|
| A — المرجعية والبنية | 01.1،01.2،01.3،01.4،01.6،02.5،02.8،04.10،05.1،05.8 | أجهزة Tank المستقبلية وR0/Rs المنشور للتحقق الحسابي الآن | عقود وحدات ومراجع؛ فحص phase وblank والمقاييس، ولا ppm معتمد |
| B — إمكان التعرف | 02.4،03.3،03.7،03.8،04.2،04.5،04.9،06.5،06.9،07.3 | UCI1081 MQ135-only، MultimodalGasData الفئات، بيانات MQ خميرة/نبيذ عند وصولها | Baseline vs fingerprints vs temporal vs molecular with grouped holdouts |
| C — المخاليط الكمية | 05.3،05.4،05.5،05.6،05.7،05.9،08.8،08.9،08.10،06.7 | UCI322: Ethylene/CO وEthylene/CH4؛ UCI308/309 كتحقق منهجي إضافي | فصل كل مكوّن ودقة التركيز مع عدم يقين وقيود تعيين |
| D — الانجراف والتعميم | 01.5،07.2،07.4–07.8،06.3،06.4،08.5 | UCI270، MOS-EXT-17/18، وجلسات MQ مستقبلية | أداء عبر أجهزة/جلسات/رطوبة، توثيق الانخفاض |
| E — الشم الروبوتي | 09.1–09.6،01.6،08.6،08.7 | Tank الحقيقي، إحداثيات/رياح/تدفق/مصدر مرجعي | بحث منفصل بعد توافر حقيقة مكانية، دون ادعاء من بيانات ثابتة |

## 5. تصنيف فردي لكل واحدة من التقنيات الثمانين

قرارات هذه المصفوفة توصيات **وظيفية**، وليست تحديثاً آلياً إلى حالات التنفيذ الحالية أو أحكاماً نهائية على الدقة:

### 01 — القياس ومعايرة الحساسات

| التقنية | القرار | مرجعان أو ثلاثة | التعديل البحثي المطلوب |
|---|---|---|---|
| `01.1` ADC to Rs | إعادة تصميم | `CN-MQ-01`, `R0-PEER-01` | Implement calibrated circuit-specific ADC-to-volts and Rs with RL, voltage dividers, ADS1115 gain, sample/heater timing; propagate component uncertainty. |
| `01.2` R0 calibration | إعادة تصميم | `R0-PEER-01`, `R0-PEER-02`, `R0-PEER-03`, `R0-PEER-04` | R0 per sensor instance, gas reference, heater phase, ambient and age; forbid borrowing from paper/model; retain questionable table values. |
| `01.3` Sensitivity curves | تطوير | `TOOL-MQ-01`, `CN-MQ-04`, `R0-PEER-01` | Source-tag manufacturer curves and alternative graph fits; normalize ppm and output-scale units; avoid extrapolation and correlated curve priors. |
| `01.4` Heater cycles | إعادة تصميم | `SYS-05`, `SYS-14`, `CN-RSCH-02` | Separate manufacturer-defined MQ7/MQ9 heater duty profiles from exploratory thermal excitation on approved hardware; forbid arbitrary PWM. |
| `01.5` Environmental correction | تطوير | `SYS-04`, `SYS-24`, `MQ-EXT-02` | Conditioned environmental compensation with BME humidity/temperature/pressure and out-of-support detector; do not remove chemical trend. |
| `01.6` Signal quality | إعادة تصميم | `SYS-23`, `SYS-11`, `JP-FIG-02` | Model intake/dead volume/fan duty/flush and sensor response delay as physical sampling state; QC contamination and carry-over. |
| `01.7` Drift monitoring | تطوير | `MOS-EXT-17`, `UCI-EXT-270` | Online device health diagnostics only, independent of training drift compensation in 07.5. |

### 02 — تجهيز الإشارة والبصمات

| التقنية | القرار | مرجعان أو ثلاثة | التعديل البحثي المطلوب |
|---|---|---|---|
| `02.1` Filtering | تطوير | `SYS-17`, `MQ-EXT-10` | Compare robust noise suppression to transient-preserving filters; fit filter solely on train data. |
| `02.2` Scaling | الإبقاء | `MQ-EXT-03`, `UCI-EXT-270` | Retain source-aware scaling and prohibit test-fold normalization leakage; unit-aware Rs vs ADC separation. |
| `02.3` Static features | الإبقاء | `MQ-EXT-11`, `SYS-13` | Retain ratios, peaks, slope and recovery features with source-specific units. |
| `02.4` Temporal features | تطوير | `CN-RSCH-01`, `SYS-17`, `MQ-EXT-10` | Add segmental average differentiation, phase-dependent response, kinetic slope and causal deconvolution features. |
| `02.5` Fingerprints | إعادة تصميم | `MQ-EXT-15`, `MQ-EXT-03`, `MQ-EXT-11` | Separate molecule/pure-reference fingerprints from mixture/food/odor class, source-transfer and device-specific features. |
| `02.6` 3D/4D features | تطوير | `TOOL-MQ-01`, `SYS-05` | Keep 3D/4D models as exploratory features and bounded simulation, not independent chemistry evidence. |
| `02.7` Feature selection | تطوير | `SYS-01`, `SYS-04` | Add train-only stability selection, nested sensor ranking and cost/energy-aware feature parsimony. |
| `02.8` Provenance schema | إعادة تصميم | `MQ-EXT-15`, `UCI-EXT-270`, `R0-PEER-01` | Formal source and label ontology: gas versus complex sample, ppb/ppm, Ri/R0/RL, calibration provenance, exposure group and train/test ancestry. |

### 03 — قابلية التمييز

| التقنية | القرار | مرجعان أو ثلاثة | التعديل البحثي المطلوب |
|---|---|---|---|
| `03.1` PCA | الإبقاء | `SYS-13`, `CN-PAT-01` | PCA for exploratory visualizable structure only; no standalone gas certainty. |
| `03.2` Similarity | تطوير | `MQ-EXT-03`, `MQ-EXT-11` | Density/shape-aware similarity with instrument response uncertainty and source group. |
| `03.3` Class overlap | تطوير | `SYS-02`, `UCI-EXT-270` | Quantify pure-gas and binary mixture overlaps under concentration and ambient shifts. |
| `03.4` LDA/PLS-DA | الإبقاء | `SYS-01`, `CN-PAT-01` | Retain genuinely trained LDA/PLS-DA external implementation; ensure cross-fold PLS components chosen train-only. |
| `03.5` Sensor importance | تطوير | `SYS-01`, `SYS-04` | Channel ablation and sensor-specific gain with hardware stability; no universal sensor rank across instruments. |
| `03.6` Sensor subsets | تطوير | `SYS-01`, `SYS-04` | Design minimum MQ subsets robustly across gases and concentration rather than all-eight assumption. |
| `03.7` Concentration confounding | إعادة تصميم | `MQ-EXT-15`, `SYS-10` | Joint concentration-invariant feature checks and leave-concentration-out stress; distinguish concentration change from identity change. |
| `03.8` Identifiability limits | إعادة تصميم | `SYS-06`, `SYS-07`, `SYS-09` | Measure mixture observability: response Jacobian rank, condition number and non-identifiable component combinations. |

### 04 — التصنيف والتعرف

| التقنية | القرار | مرجعان أو ثلاثة | التعديل البحثي المطلوب |
|---|---|---|---|
| `04.1` Curve matching | إعادة تصميم | `TOOL-MQ-01`, `R0-PEER-01` | Only curve-match within source-referenced calibration domain with heater, RL and R0 per device; treat unmatched as unknown. |
| `04.2` Fingerprint matching | تطوير | `MQ-EXT-03`, `MQ-EXT-11` | Use validated source-labelled fingerprints and report source-specific transfer domain. |
| `04.3` KNN | الإبقاء | `MQ-EXT-03`, `CN-RSCH-01` | KNN simple baseline; train-only feature extraction/scaling. |
| `04.4` Logistic regression | الإبقاء | `MQ-EXT-15`, `UCI-EXT-270` | Logistic baseline with calibration and group splits. |
| `04.5` SVM | الإبقاء | `SYS-01`, `CN-RSCH-02` | True linear/RBF SVM research implementation; tune nested, do not claim Tank transfer. |
| `04.6` Random Forest | تطوير | `SYS-04`, `SYS-13` | Random forests/ExtraTrees with class imbalance and uncertainty; separate concentration from identity target. |
| `04.7` XGBoost | الإبقاء | `SYS-01`, `SYS-13` | Real XGBoost reference path exists; compare learning curve and leakage hazards. |
| `04.8` MLP | الإبقاء | `CN-RSCH-01`, `CN-RSCH-04` | MLP/BP baseline must be actually trained and compared with penalized simpler methods. |
| `04.9` Physics+ML | إعادة تصميم | `QSPR-REF-03`, `QSPR-REF-04`, `QSPR-REF-10`, `SYS-07` | Forward molecule+sensor+environment QSPR with source-specific response curves, then candidate-wise inverse ranking; no true-label descriptors; physics regularization. |
| `04.10` Benchmarking | إعادة تصميم | `UCI-EXT-270`, `SYS-10`, `MQ-EXT-15` | Frozen benchmark registry: pure gas, mixtures, OOD, source and device; nested tuning, independent final test; record failed methods. |

### 05 — التقدير الكمي للغازات

| التقنية | القرار | مرجعان أو ثلاثة | التعديل البحثي المطلوب |
|---|---|---|---|
| `05.1` Single-sensor calibration | إعادة تصميم | `R0-PEER-01`, `R0-PEER-04`, `CN-MQ-04` | Device/gas-specific monotonic single-sensor concentration regression with traceable R0 definition; no paper R0 transfer. |
| `05.2` Multipoint calibration | إعادة تصميم | `MQ-EXT-15`, `SYS-04` | Multi-point and factorial temperature/humidity calibration with hysteresis, dilution uncertainty and concentration-specific error. |
| `05.3` Nonlinear regression | تطوير | `SYS-07`, `SYS-15` | Physical nonlinear log-sensitivity models, saturation and cross sensitivity with meaningful bounds. |
| `05.4` Multisensor regression | إعادة تصميم | `SYS-06`, `SYS-07`, `SYS-10` | True constrained multicomponent inverse regression with nonnegative concentrations and physically valid sensor response equations. |
| `05.5` PLS regression | تطوير | `SYS-09`, `SYS-11` | Real PLS/PLS2 and MCR-ALS latent profiles, rank and nonnegativity under explicit assumptions. |
| `05.6` Tree regression | تطوير | `SYS-04`, `SYS-15` | RF/ExtraTrees/CatBoost/SVR regression candidates with uncertainty and concentration/domain limits. |
| `05.7` Identity-conditional estimates | إعادة تصميم | `SYS-02`, `SYS-03` | Identity-conditioned concentration only when identity confidence passes scientific gate; allow unknown and mixtures. |
| `05.8` LOD/LOQ | إعادة تصميم | `R0-PEER-01`, `SYS-04` | Detection power/LOD/LOQ from blank and independently measured standards under humidity/noise, not manufacturer cartoon threshold. |
| `05.9` Unseen concentrations | تطوير | `MQ-EXT-15`, `SYS-10` | Concentration extrapolation stress and defined out-of-range rejection instead of unbounded ppm. |

### 06 — دمج الأدلة وعدم اليقين

| التقنية | القرار | مرجعان أو ثلاثة | التعديل البحثي المطلوب |
|---|---|---|---|
| `06.1` Evidence fusion | إعادة تصميم | `QSPR-REF-03`, `SYS-06` | Evidence fusion with dependence graph across same MQ curve, QSPR priors and ML; prohibit double counting correlated evidence. |
| `06.2` Bayesian inference | تطوير | `QSPR-REF-03`, `SYS-07` | Bayesian ranking over candidate gas and concentration with priors and unidentifiability flag. |
| `06.3` Probability calibration | تطوير | `SYS-04`, `UCI-EXT-270` | Probability calibration on calibration fold only; no on-the-fly confidence as measured ppm. |
| `06.4` Conformal prediction | تطوير | `MOS-EXT-17`, `UCI-EXT-270` | Conformal sets with group/shift qualifications; report loss of exchangeability across devices. |
| `06.5` Open-set recognition | تطوير | `MQ-EXT-15`, `QSPR-REF-03` | Open-set candidate rejection with class-sequestered training and unknown categories. |
| `06.6` OOD detection | تطوير | `UCI-EXT-270`, `MOS-EXT-17` | OOD checks spanning new chemical, concentration, drift and ambient/device. |
| `06.7` Evidence conflicts | إعادة تصميم | `SYS-07`, `QSPR-REF-04` | Evidence conflict accounting for cross-sensitivity and physically impossible concentration/mixture combinations. |
| `06.8` SHAP | الإبقاء | `QSPR-REF-05`, `SYS-04` | SHAP and descriptive model diagnostics only; do not label explanation as causal chemistry. |
| `06.9` Abstention | إعادة تصميم | `MQ-EXT-15`, `SYS-10` | Decision policy for unknown gas, unknown mixture, unsupported ppm and sensor fault; fail-safe separate from research identity. |

### 07 — الانجراف والموثوقية

| التقنية | القرار | مرجعان أو ثلاثة | التعديل البحثي المطلوب |
|---|---|---|---|
| `07.1` Cross-session tests | الإبقاء | `MQ-EXT-11`, `MQ-EXT-02` | Session-level validation distinct from time windows. |
| `07.2` Cross-device tests | تطوير | `MOS-EXT-18`, `R0-PEER-03` | Cross-device alignment with device-specific R0 and domain adaptation, not cross-device shared R0. |
| `07.3` Cross-concentration tests | تطوير | `MQ-EXT-15`, `SYS-10` | Train/holdout disjoint concentration ranges, not nearby windows. |
| `07.4` Drift detection | تطوير | `MOS-EXT-17`, `UCI-EXT-270` | Change-point drift detection conditional on environment and history. |
| `07.5` Drift compensation | تطوير | `SYS-24`, `UCI-EXT-270` | OSC/PLS drift correction constrained against removing chemistry signals. |
| `07.6` Domain adaptation | تطوير | `UCI-EXT-270`, `MOS-EXT-18` | Domain adaptation with unsupervised target normalization and no target-label leakage. |
| `07.7` Transfer learning | تطوير | `MOS-EXT-18`, `MQ-EXT-15` | Transfer learning from non-MQ allowed only method-wise, unless physically mapped MQ channel. |
| `07.8` Active learning/DoE | إعادة تصميم | `SYS-07`, `R0-PEER-02` | Use active design-of-experiments over gases, ppm, binary mixtures, temperature, RH, phase, session and device. |
| `07.9` Continual learning | تأجيل | `MOS-EXT-17`, `UCI-EXT-270` | No uncontrolled model self-update in deployed field app; require governance and fresh independent revalidation. |
| `07.10` Resource profiling | تطوير | `SYS-01`, `SYS-20` | Profile actual Android CPU, battery and stream latency vs algorithms; do not infer from desktop tests. |

### 08 — الشبكات والخلائط

| التقنية | القرار | مرجعان أو ثلاثة | التعديل البحثي المطلوب |
|---|---|---|---|
| `08.1` 1D-CNN | الإبقاء | `MQ-EXT-10`, `MQ-EXT-15` | Conv1d real external baseline already trained; test actual MQ temporal before deployment. |
| `08.2` TCN | الإبقاء | `SYS-17`, `MQ-EXT-10` | Causal TCN real external baseline; maintain temporal causality. |
| `08.3` LSTM/GRU | الإبقاء | `MQ-EXT-10`, `MQ-EXT-15` | GRU exists on main and LSTM on separate draft PR; treat both as research-only. |
| `08.4` GNN | تأجيل | `SYS-01`, `SYS-13` | GNN prototype underperforms simpler external baselines; do not prioritize without meaningful sensor graph and gains. |
| `08.5` Temporal ensemble | تطوير | `SYS-01`, `SYS-04` | Temporal ensemble must be diversity-aware with correlated model error and honest uncertainty. |
| `08.6` Simulation | تطوير | `SYS-17`, `SYS-05` | Source-calibrated kinetics and flow/heat time constants, no synthetic coefficient masquerading as measured. |
| `08.7` Sim-to-real | تطوير | `SYS-17`, `MOS-EXT-17` | Correct time alignment, units, sensor identity, baseline, impedance and domain shift before sim-real comparisons. |
| `08.8` Mixture detection | إعادة تصميم | `SYS-02`, `SYS-06`, `SYS-10` | Separate binary/ternary mixture detection and mixture-presence OOD from exact constituent identification. |
| `08.9` Unmixing | إعادة تصميم | `SYS-07`, `SYS-08`, `SYS-09` | Introduce model families MCR-ALS, nonnegative/sparse constrained inversion and nonlinear cross-sensitivity; explicitly test identifiability. |
| `08.10` Multi-analyte regression | إعادة تصميم | `SYS-06`, `SYS-10`, `CN-PAT-02` | Multi-output gas-specific ppm regression with mass-flow setpoints and uncertainty; classification not surrogate for concentration. |
| `08.11` Interference | إعادة تصميم | `SYS-18`, `SYS-19`, `SYS-23` | Separate chemical hardware filtering/preconcentration from computational interference model and purge; account breakthrough/humidity and target loss. |
| `08.12` Complex products | تطوير | `MQ-EXT-10`, `MQ-EXT-11` | Food/liquid/wood/composite fingerprints trained as classes only; avoid gas molecule labels. |

### 09 — الشم الروبوتي والموقع

| التقنية | القرار | مرجعان أو ثلاثة | التعديل البحثي المطلوب |
|---|---|---|---|
| `09.1` Robotic olfaction | تأجيل | `SYS-12`, `SYS-23` | Define robotic olfaction assumptions and recording pose/time/wind before claiming localization. |
| `09.2` Gas mapping | تأجيل | `SYS-12`, `SYS-10` | Build gas occupancy field when spatial reference route exists, not from stationary chambers. |
| `09.3` Source localization | تأجيل | `SYS-12`, `SYS-17` | Estimate source position only with transport dynamics, wind and reference source geometry. |
| `09.4` Active sampling | إعادة تصميم | `SYS-11`, `SYS-23` | Active sampling as controlled experiment: adjustable intake, clean-air purge, pauses and motion; maintain time-lag aware data model. |
| `09.5` Spatiotemporal fusion | تأجيل | `SYS-12`, `SYS-17` | Combine aligned robot pose plus time series after wind and inlet impulse response are available. |
| `09.6` Motion decisions | تأجيل | `SYS-12`, `SYS-23` | Motion-to-source safety and gas map decision policy requires validated source cues; movement safety remains independent. |

### 10 — الأدلة السلبية

| التقنية | القرار | مرجعان أو ثلاثة | التعديل البحثي المطلوب |
|---|---|---|---|
| `10.1` Negative Evidence & Sensor-Guided Exclusion | إعادة تصميم | `SYS-04`, `QSPR-REF-09` | Negative evidence only with calibrated detector sensitivity/false negative under concentration, mixture and humidity; forbid exclusion from non-response alone. |

## 6. بوابات لا يمكن تجاوزها

- **G1:** اجتياز كود بحثي لا يعني أن الخوارزمية الكيميائية فعالة على الجهاز؛ حتى شبكات UCI الحقيقية ليست Tank MQ8.
- **G2:** قد تتوفر بيانات MQ خام من حساس واحد أو مؤشرات نسبية/مواد غذائية لا تتضمن المعيار الكيميائي المطلوب؛ لا تُرفع تلقائياً إلى ppm.
- **G3:** لا تزال `0/80` تقنيات مثبتة على روبوت Tank الفعلي. تسجيل 80 مرجعاً ببليوغرافياً لا يعني 80 تجارب أو نجاحاً علمياً.
- **الإنتاج:** الفصل بين ChemCore في تطبيق Android، والبرامج البحثية، والمكتبة المركزية المرجعية، ونظام إنذار السلامة مستقل. لا تعديل على APK/ESP32/FW دون تصميم وتحقيق ومراجعة.
- **القابلية للإعادة:** كل تجربة تحمل مصدر البيانات/DOI/SHA256 إن جُلبت، نوع المستشعر، تعريف الغاز/الجلسة، الوحدات، heldout split، الشيفرة ونتائج الفشل.

## 7. نقطة استئناف بعد انقطاع الاتصال

1. افتح [المصفوفة الآلية](CHEMCORE80_REFERENCE_GAP_MATRIX_2026-10-21.json)؛ 80/80 معرفاً و179 رابطاً تقنياً لمراجع أصلية بأكوادها.
2. راجع `research/PROGRESS_80.json` و`research/RESUME.md` على `main`؛ لا ترفع حالة G3 أو تنسخ أرقام الاختبارات على غير جهازها.
3. افحص حالة PR#3 البحثي قبل أخذ ملفات LSTM/Physics+ML والمحاكاة؛ لا تعتمد نسخته دون CI ومراجعة تعارض الفروع.
4. ابدأ بحزمة A عشر تقنيات، مع توثيق مصدر كل قرار والاختبارات السلبية؛ ثم استكمل B/C/D/E بحسب أهلية البيانات.
5. لا تضف تقنية 81 أو تغيّر كود التطبيق التشغيلي إلا بعد نتائج تجريبية مستقلة وقرار ADR مقبول.
