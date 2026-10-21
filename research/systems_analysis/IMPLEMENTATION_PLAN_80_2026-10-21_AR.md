# خطة تنفيذ تطوير التقنيات الثمانين وفق مكتبة المراجع المركزية
**2026-10-21، HazRob Tank فقط — خطة تشغيل بحثية قابلة للاستئناف**

هذه خطة مراجعة وتنفيذ واقعية لكل تقنية، ولا تفترض نجاحاً تجريبياً من قراءة بحث علمي أو اجتياز اختبارات برمجية. لا تعديل على APK أو ESP32 أو ChemCore الأصلي. جميع الأعمال هنا في `research/systems_analysis/`. البيانات المرجعية موجودة [في مكتبة المنظمة](https://github.com/HazRob/build-harness/tree/main/library/reference-bank/).

## ما تحقق وما لم يتحقق

- **80/80** نماذج بحثية أولية G1 مع **1261 اختباراً تاريخياً**؛ و**80/80** مراجعة G2 للأهلية: 49 عمليات خارجية رقمية/وصفية، 29 محجوبة، 2 محاكاة فقط.
- **14,238 تعرضاً خارجياً معروفاً** في UCI1081/UCI270/UCI308/UCI309. لا يوجد بعد تعريض مرجعي من مصفوفة MQ8 الفعلية، لذا **G3=0/80**.
- المقارنة مع **80 سجل مصادر علمية** أنتجت 26 إعادة تصميم و33 تطويراً و14 إبقاءً و7 تأجيلاً؛ العدد 80 للمراجع مصادفة وليس 80 بيانات قياس مستقل.

## آلية التنفيذ والاستئناف

1. تُنفّذ **ثماني دفعات، عشر تقنيات في كل دفعة**. كل دفعة تُجمّد قبل التنفيذ مع رابط للمصدر والخوارزمية والاختبار ومفتاح العينة والقيود.
2. لكل تقنية من الثمانين: ID، ومرجع من المكتبة المركزية، وما الذي يجب تعديله، والمدخل/المخرج والوحدة، وحالات الفشل، وقاعدة منع تسرب التدريب/الاختبار، ومخرج اختبار برمجي مثبت بـCI.
3. يُحفظ التقرير والـJSON في `research/systems_analysis/`؛ بعد كل عمل تُحدَّث حالة التقنية مع commit SHA ورابط CI وحقيقة `G3=0` إذا لم توجد تجربة حقيقية.
4. الأدلة الخارجية لا تبرر تقديم R0 عامة لكل حساس، ولا تقدير ppm موثوق، ولا إنذار سلامة أو هوية غاز مؤكدة.
5. فرع V3 البحثي السابق [PR #3](https://github.com/HazRob/HazRob-Tank/pull/3) يحتوي إضافات LSTM وPhysics+ML والمحاكاة، فلا يُدمج دون مراجعة فروق الملفات مع `main` وقيود المصدر.

## الدفعات الثماني

| دفعة | المعرفات العشرة | حالة التقدم |
|---|---|---|
| 1 | `01.1`، `01.2`، `01.3`، `01.4`، `01.6`، `02.5`، `02.8`، `04.10`، `05.1`، `05.8` | PLANNED |
| 2 | `01.5`، `01.7`، `02.1`، `02.2`، `02.3`، `02.4`، `02.6`، `02.7`، `03.1`، `03.2` | PLANNED |
| 3 | `03.3`، `03.4`، `03.5`، `03.6`، `03.7`، `03.8`، `04.1`، `04.2`، `04.3`، `04.4` | PLANNED |
| 4 | `04.5`، `04.6`، `04.7`، `04.8`، `04.9`، `05.2`، `05.3`، `05.4`، `05.5`، `05.6` | PLANNED |
| 5 | `05.7`، `05.9`، `06.1`، `06.2`، `06.3`، `06.4`، `06.5`، `06.6`، `06.7`، `06.8` | PLANNED |
| 6 | `06.9`، `07.1`، `07.2`، `07.3`، `07.4`، `07.5`، `07.6`، `07.7`، `07.8`، `07.9` | PLANNED |
| 7 | `07.10`، `08.1`، `08.2`، `08.3`، `08.4`، `08.5`، `08.6`، `08.7`، `08.8`، `08.9` | PLANNED |
| 8 | `08.10`، `08.11`، `08.12`، `09.1`، `09.2`، `09.3`، `09.4`، `09.5`، `09.6`، `10.1` | PLANNED |

## بروتوكول الاختبارات والحدود

| البوابة | ما يجب إثباته |
|---|---|
| G0 | المرجع الأصلي وDOI/URL، نوع الحساس وRL/R0 والسخان والبيئة وصنف الوسم والبيانات المفقودة |
| G1 | شيفرة فعلية واختبارات صحيحة وخاطئة وحدّية على commit SHA معروف، وأثر CI؛ لا تعادل تحقق غاز |
| G2 | ملف تعريضات حقيقية مؤهل لمدخلات التقنية وتراكيز/جلسات موثقة حيث يلزم؛ لا مساواة UCI/TGS ببيانات MQ8 |
| G3 | تجربة MQ8 Tank مع حقيقة غاز/تركيز مستقلة وجلسة/جهاز محجوب واختبار عدم اليقين؛ **0/80 حالياً** |

## أولويات قابلة للاختبار

**الدفعة 1 = أساس القياس والمنهجية:** ADC→Rs؛ R0؛ منحنيات؛ طور السخان؛ جودة العينة والتدفق؛ تعريف البصمة؛ هوية المصدر؛ benchmark محجوب بالجلسة؛ انحدار الغاز المعروف؛ حدود الكشف المسندة لفراغات مستقلة. أول إنجاز فيها عقد برامج مشترك *غير تشغيلي*، مع 10 واجهات مع اختبارات وحدية وسلبية.

**بقية الدفعات:** تتدرج من تعويض البيئة والتوقيت والتمييز، إلى التصنيف والتقدير الكمي، فدمج الأدلة والتحقق والانجراف، ثم النماذج المتقدمة والمخاليط والشم الروبوتي.

## تفاصيل العمل الموثق لكل تقنية

### الدفعة 1 — عشرة معرفات

| المعرف | القرار | التنفيذ/التعديل المطلوب | مراجع المصدر | معيار الإثبات قبل الإغلاق |
|---|---|---|---|---|
| `01.1` ADC to Rs | إعادة تصميم | Implement calibrated circuit-specific ADC-to-volts and Rs with RL, voltage dividers, ADS1115 gain, sample/heater timing; propagate component uncertainty. | `CN-MQ-01`, `R0-PEER-01` | Require device wiring and raw ADC/voltage/RL trace. |
| `01.2` R0 calibration | إعادة تصميم | R0 per sensor instance, gas reference, heater phase, ambient and age; forbid borrowing from paper/model; retain questionable table values. | `R0-PEER-01`, `R0-PEER-02`, `R0-PEER-03`, `R0-PEER-04` | Replicated standards and traceable blank, heater-cycle metadata. |
| `01.3` Sensitivity curves | تطوير | Source-tag manufacturer curves and alternative graph fits; normalize ppm and output-scale units; avoid extrapolation and correlated curve priors. | `TOOL-MQ-01`, `CN-MQ-04`, `R0-PEER-01` | Source chart digitization audit and independent pure-gas points. |
| `01.4` Heater cycles | إعادة تصميم | Separate manufacturer-defined MQ7/MQ9 heater duty profiles from exploratory thermal excitation on approved hardware; forbid arbitrary PWM. | `SYS-05`, `SYS-14`, `CN-RSCH-02` | Electrical manufacturer compliance and per-phase Rs traces. |
| `01.6` Signal quality | إعادة تصميم | Model intake/dead volume/fan duty/flush and sensor response delay as physical sampling state; QC contamination and carry-over. | `SYS-23`, `SYS-11`, `JP-FIG-02` | Measured chamber airflow/washout/retained-sample tests. |
| `02.5` Fingerprints | إعادة تصميم | Separate molecule/pure-reference fingerprints from mixture/food/odor class, source-transfer and device-specific features. | `MQ-EXT-15`, `MQ-EXT-03`, `MQ-EXT-11` | Enforce class semantics and leave source/session out. |
| `02.8` Provenance schema | إعادة تصميم | Formal source and label ontology: gas versus complex sample, ppb/ppm, Ri/R0/RL, calibration provenance, exposure group and train/test ancestry. | `MQ-EXT-15`, `UCI-EXT-270`, `R0-PEER-01` | Automated schema and audit-of-raw-file checks. |
| `04.10` Benchmarking | إعادة تصميم | Frozen benchmark registry: pure gas, mixtures, OOD, source and device; nested tuning, independent final test; record failed methods. | `UCI-EXT-270`, `SYS-10`, `MQ-EXT-15` | No windows of same exposure in both train/test. |
| `05.1` Single-sensor calibration | إعادة تصميم | Device/gas-specific monotonic single-sensor concentration regression with traceable R0 definition; no paper R0 transfer. | `R0-PEER-01`, `R0-PEER-04`, `CN-MQ-04` | Certified target gas concentrations over range. |
| `05.8` LOD/LOQ | إعادة تصميم | Detection power/LOD/LOQ from blank and independently measured standards under humidity/noise, not manufacturer cartoon threshold. | `R0-PEER-01`, `SYS-04` | Calibration blank distribution and false-negative confidence. |

### الدفعة 2 — عشرة معرفات

| المعرف | القرار | التنفيذ/التعديل المطلوب | مراجع المصدر | معيار الإثبات قبل الإغلاق |
|---|---|---|---|---|
| `01.5` Environmental correction | تطوير | Conditioned environmental compensation with BME humidity/temperature/pressure and out-of-support detector; do not remove chemical trend. | `SYS-04`, `SYS-24`, `MQ-EXT-02` | Factorial humidity/temperature reference exposures. |
| `01.7` Drift monitoring | تطوير | Online device health diagnostics only, independent of training drift compensation in 07.5. | `MOS-EXT-17`, `UCI-EXT-270` | Longitudinal blank and reference checks on same devices. |
| `02.1` Filtering | تطوير | Compare robust noise suppression to transient-preserving filters; fit filter solely on train data. | `SYS-17`, `MQ-EXT-10` | Show preserved onset times/area and group-holdout effect. |
| `02.2` Scaling | إبقاء وتحقق | Retain source-aware scaling and prohibit test-fold normalization leakage; unit-aware Rs vs ADC separation. | `MQ-EXT-03`, `UCI-EXT-270` | Per-source baseline and grouped split tests. |
| `02.3` Static features | إبقاء وتحقق | Retain ratios, peaks, slope and recovery features with source-specific units. | `MQ-EXT-11`, `SYS-13` | Whole recorded event held out. |
| `02.4` Temporal features | تطوير | Add segmental average differentiation, phase-dependent response, kinetic slope and causal deconvolution features. | `CN-RSCH-01`, `SYS-17`, `MQ-EXT-10` | Time-shift ablation, independent full trials. |
| `02.6` 3D/4D features | تطوير | Keep 3D/4D models as exploratory features and bounded simulation, not independent chemistry evidence. | `TOOL-MQ-01`, `SYS-05` | No fabricated measurements; compare on matched actual traces. |
| `02.7` Feature selection | تطوير | Add train-only stability selection, nested sensor ranking and cost/energy-aware feature parsimony. | `SYS-01`, `SYS-04` | Nested validation with whole sessions/devices. |
| `03.1` PCA | إبقاء وتحقق | PCA for exploratory visualizable structure only; no standalone gas certainty. | `SYS-13`, `CN-PAT-01` | Train-only fit and heldout projection. |
| `03.2` Similarity | تطوير | Density/shape-aware similarity with instrument response uncertainty and source group. | `MQ-EXT-03`, `MQ-EXT-11` | Candidate-ranking calibration after grouped holdout. |

### الدفعة 3 — عشرة معرفات

| المعرف | القرار | التنفيذ/التعديل المطلوب | مراجع المصدر | معيار الإثبات قبل الإغلاق |
|---|---|---|---|---|
| `03.3` Class overlap | تطوير | Quantify pure-gas and binary mixture overlaps under concentration and ambient shifts. | `SYS-02`, `UCI-EXT-270` | Report indistinguishable regions and abstention. |
| `03.4` LDA/PLS-DA | إبقاء وتحقق | Retain genuinely trained LDA/PLS-DA external implementation; ensure cross-fold PLS components chosen train-only. | `SYS-01`, `CN-PAT-01` | Compare complete heldout protocol to documented baseline. |
| `03.5` Sensor importance | تطوير | Channel ablation and sensor-specific gain with hardware stability; no universal sensor rank across instruments. | `SYS-01`, `SYS-04` | Repeated nested ablation, cost-accuracy trade-off. |
| `03.6` Sensor subsets | تطوير | Design minimum MQ subsets robustly across gases and concentration rather than all-eight assumption. | `SYS-01`, `SYS-04` | Evaluate source subsets across heldout sessions and gas levels. |
| `03.7` Concentration confounding | إعادة تصميم | Joint concentration-invariant feature checks and leave-concentration-out stress; distinguish concentration change from identity change. | `MQ-EXT-15`, `SYS-10` | Locked unseen concentration ranges and per-gas confusion. |
| `03.8` Identifiability limits | إعادة تصميم | Measure mixture observability: response Jacobian rank, condition number and non-identifiable component combinations. | `SYS-06`, `SYS-07`, `SYS-09` | Show when composition cannot be uniquely inferred with given sensors. |
| `04.1` Curve matching | إعادة تصميم | Only curve-match within source-referenced calibration domain with heater, RL and R0 per device; treat unmatched as unknown. | `TOOL-MQ-01`, `R0-PEER-01` | Independent standard reference and uncertainty. |
| `04.2` Fingerprint matching | تطوير | Use validated source-labelled fingerprints and report source-specific transfer domain. | `MQ-EXT-03`, `MQ-EXT-11` | Grouped cross-source OOD and top-k. |
| `04.3` KNN | إبقاء وتحقق | KNN simple baseline; train-only feature extraction/scaling. | `MQ-EXT-03`, `CN-RSCH-01` | Paired heldout vs richer models. |
| `04.4` Logistic regression | إبقاء وتحقق | Logistic baseline with calibration and group splits. | `MQ-EXT-15`, `UCI-EXT-270` | Reference comparisons and uncertainty. |

### الدفعة 4 — عشرة معرفات

| المعرف | القرار | التنفيذ/التعديل المطلوب | مراجع المصدر | معيار الإثبات قبل الإغلاق |
|---|---|---|---|---|
| `04.5` SVM | إبقاء وتحقق | True linear/RBF SVM research implementation; tune nested, do not claim Tank transfer. | `SYS-01`, `CN-RSCH-02` | Grouped source validation. |
| `04.6` Random Forest | تطوير | Random forests/ExtraTrees with class imbalance and uncertainty; separate concentration from identity target. | `SYS-04`, `SYS-13` | Nested source and concentration groups. |
| `04.7` XGBoost | إبقاء وتحقق | Real XGBoost reference path exists; compare learning curve and leakage hazards. | `SYS-01`, `SYS-13` | Grouped holdout and freeze hyperparameters. |
| `04.8` MLP | إبقاء وتحقق | MLP/BP baseline must be actually trained and compared with penalized simpler methods. | `CN-RSCH-01`, `CN-RSCH-04` | Independent gas class trials. |
| `04.9` Physics+ML | إعادة تصميم | Forward molecule+sensor+environment QSPR with source-specific response curves, then candidate-wise inverse ranking; no true-label descriptors; physics regularization. | `QSPR-REF-03`, `QSPR-REF-04`, `QSPR-REF-10`, `SYS-07` | MQ135-only source test and heldout-molecule challenge; calibrated candidate error. |
| `05.2` Multipoint calibration | إعادة تصميم | Multi-point and factorial temperature/humidity calibration with hysteresis, dilution uncertainty and concentration-specific error. | `MQ-EXT-15`, `SYS-04` | Replicate standards, uncertainty budget, environmental conditions. |
| `05.3` Nonlinear regression | تطوير | Physical nonlinear log-sensitivity models, saturation and cross sensitivity with meaningful bounds. | `SYS-07`, `SYS-15` | Independent multi-concentration mixtures. |
| `05.4` Multisensor regression | إعادة تصميم | True constrained multicomponent inverse regression with nonnegative concentrations and physically valid sensor response equations. | `SYS-06`, `SYS-07`, `SYS-10` | Known each-component ppm; report per-gas RMSE/MAE and ambiguity. |
| `05.5` PLS regression | تطوير | Real PLS/PLS2 and MCR-ALS latent profiles, rank and nonnegativity under explicit assumptions. | `SYS-09`, `SYS-11` | Unseen mixture compositions and independent time batches. |
| `05.6` Tree regression | تطوير | RF/ExtraTrees/CatBoost/SVR regression candidates with uncertainty and concentration/domain limits. | `SYS-04`, `SYS-15` | Paired per-component evaluation, no leakage. |

### الدفعة 5 — عشرة معرفات

| المعرف | القرار | التنفيذ/التعديل المطلوب | مراجع المصدر | معيار الإثبات قبل الإغلاق |
|---|---|---|---|---|
| `05.7` Identity-conditional estimates | إعادة تصميم | Identity-conditioned concentration only when identity confidence passes scientific gate; allow unknown and mixtures. | `SYS-02`, `SYS-03` | Quantification error conditional on label quality. |
| `05.9` Unseen concentrations | تطوير | Concentration extrapolation stress and defined out-of-range rejection instead of unbounded ppm. | `MQ-EXT-15`, `SYS-10` | Heldout concentration strata, out-of-domain alarm. |
| `06.1` Evidence fusion | إعادة تصميم | Evidence fusion with dependence graph across same MQ curve, QSPR priors and ML; prohibit double counting correlated evidence. | `QSPR-REF-03`, `SYS-06` | Ablation + reliability diagram and source-separated error. |
| `06.2` Bayesian inference | تطوير | Bayesian ranking over candidate gas and concentration with priors and unidentifiability flag. | `QSPR-REF-03`, `SYS-07` | Posterior calibration and coverage on unknown gas. |
| `06.3` Probability calibration | تطوير | Probability calibration on calibration fold only; no on-the-fly confidence as measured ppm. | `SYS-04`, `UCI-EXT-270` | Expected calibration error on session holdout. |
| `06.4` Conformal prediction | تطوير | Conformal sets with group/shift qualifications; report loss of exchangeability across devices. | `MOS-EXT-17`, `UCI-EXT-270` | Coverage per device/domain, empty set counted as abstention. |
| `06.5` Open-set recognition | تطوير | Open-set candidate rejection with class-sequestered training and unknown categories. | `MQ-EXT-15`, `QSPR-REF-03` | Heldout chemical species, not random rows only. |
| `06.6` OOD detection | تطوير | OOD checks spanning new chemical, concentration, drift and ambient/device. | `UCI-EXT-270`, `MOS-EXT-17` | Distinct OOD axes and false-safe rates. |
| `06.7` Evidence conflicts | إعادة تصميم | Evidence conflict accounting for cross-sensitivity and physically impossible concentration/mixture combinations. | `SYS-07`, `QSPR-REF-04` | Disagreement with mixture standards and abstain. |
| `06.8` SHAP | إبقاء وتحقق | SHAP and descriptive model diagnostics only; do not label explanation as causal chemistry. | `QSPR-REF-05`, `SYS-04` | Feature importance stability and false explanatory claims. |

### الدفعة 6 — عشرة معرفات

| المعرف | القرار | التنفيذ/التعديل المطلوب | مراجع المصدر | معيار الإثبات قبل الإغلاق |
|---|---|---|---|---|
| `06.9` Abstention | إعادة تصميم | Decision policy for unknown gas, unknown mixture, unsupported ppm and sensor fault; fail-safe separate from research identity. | `MQ-EXT-15`, `SYS-10` | Thresholds from decision costs, grouped false-negative confidence. |
| `07.1` Cross-session tests | إبقاء وتحقق | Session-level validation distinct from time windows. | `MQ-EXT-11`, `MQ-EXT-02` | Entire exposure and recording groups separated. |
| `07.2` Cross-device tests | تطوير | Cross-device alignment with device-specific R0 and domain adaptation, not cross-device shared R0. | `MOS-EXT-18`, `R0-PEER-03` | Hold out physical sensor modules. |
| `07.3` Cross-concentration tests | تطوير | Train/holdout disjoint concentration ranges, not nearby windows. | `MQ-EXT-15`, `SYS-10` | Report errors at unseen ppm and power dependence. |
| `07.4` Drift detection | تطوير | Change-point drift detection conditional on environment and history. | `MOS-EXT-17`, `UCI-EXT-270` | Longitudinal independent days. |
| `07.5` Drift compensation | تطوير | OSC/PLS drift correction constrained against removing chemistry signals. | `SYS-24`, `UCI-EXT-270` | Before/after heldout calibration and interferents. |
| `07.6` Domain adaptation | تطوير | Domain adaptation with unsupervised target normalization and no target-label leakage. | `UCI-EXT-270`, `MOS-EXT-18` | Test later batches and unseen instruments. |
| `07.7` Transfer learning | تطوير | Transfer learning from non-MQ allowed only method-wise, unless physically mapped MQ channel. | `MOS-EXT-18`, `MQ-EXT-15` | Zero-shot vs few-shot device transfer claims separated. |
| `07.8` Active learning/DoE | إعادة تصميم | Use active design-of-experiments over gases, ppm, binary mixtures, temperature, RH, phase, session and device. | `SYS-07`, `R0-PEER-02` | Pre-registered factorial/space filling and random effects. |
| `07.9` Continual learning | تأجيل مشروط | No uncontrolled model self-update in deployed field app; require governance and fresh independent revalidation. | `MOS-EXT-17`, `UCI-EXT-270` | Locked model and separate training signoff. |

### الدفعة 7 — عشرة معرفات

| المعرف | القرار | التنفيذ/التعديل المطلوب | مراجع المصدر | معيار الإثبات قبل الإغلاق |
|---|---|---|---|---|
| `07.10` Resource profiling | تطوير | Profile actual Android CPU, battery and stream latency vs algorithms; do not infer from desktop tests. | `SYS-01`, `SYS-20` | Device runtime profiling, memory and thermal test. |
| `08.1` 1D-CNN | إبقاء وتحقق | Conv1d real external baseline already trained; test actual MQ temporal before deployment. | `MQ-EXT-10`, `MQ-EXT-15` | Session/device-heldout; compare simpler feature models. |
| `08.2` TCN | إبقاء وتحقق | Causal TCN real external baseline; maintain temporal causality. | `SYS-17`, `MQ-EXT-10` | Streaming prefix invariance on heldout sessions. |
| `08.3` LSTM/GRU | إبقاء وتحقق | GRU exists on main and LSTM on separate draft PR; treat both as research-only. | `MQ-EXT-10`, `MQ-EXT-15` | Read PR #3, LSTM external 59/90 and GRU 69/90; Tank unknown. |
| `08.4` GNN | تأجيل مشروط | GNN prototype underperforms simpler external baselines; do not prioritize without meaningful sensor graph and gains. | `SYS-01`, `SYS-13` | Controlled graph ablations on sufficient Tank data. |
| `08.5` Temporal ensemble | تطوير | Temporal ensemble must be diversity-aware with correlated model error and honest uncertainty. | `SYS-01`, `SYS-04` | Pairwise error dependence, no naive vote confidence. |
| `08.6` Simulation | تطوير | Source-calibrated kinetics and flow/heat time constants, no synthetic coefficient masquerading as measured. | `SYS-17`, `SYS-05` | Fit actual bounded response/recovery curves; PR #3 prototype not merged. |
| `08.7` Sim-to-real | تطوير | Correct time alignment, units, sensor identity, baseline, impedance and domain shift before sim-real comparisons. | `SYS-17`, `MOS-EXT-17` | Paired physical apparatus and source trace hashes. |
| `08.8` Mixture detection | إعادة تصميم | Separate binary/ternary mixture detection and mixture-presence OOD from exact constituent identification. | `SYS-02`, `SYS-06`, `SYS-10` | Whole mixed runs, unseen ratios and absent compounds. |
| `08.9` Unmixing | إعادة تصميم | Introduce model families MCR-ALS, nonnegative/sparse constrained inversion and nonlinear cross-sensitivity; explicitly test identifiability. | `SYS-07`, `SYS-08`, `SYS-09` | Pure-gas and measured binary mixture ground-truth with abundance per component. |

### الدفعة 8 — عشرة معرفات

| المعرف | القرار | التنفيذ/التعديل المطلوب | مراجع المصدر | معيار الإثبات قبل الإغلاق |
|---|---|---|---|---|
| `08.10` Multi-analyte regression | إعادة تصميم | Multi-output gas-specific ppm regression with mass-flow setpoints and uncertainty; classification not surrogate for concentration. | `SYS-06`, `SYS-10`, `CN-PAT-02` | Each constituent ppm error on heldout mixtures and external sessions. |
| `08.11` Interference | إعادة تصميم | Separate chemical hardware filtering/preconcentration from computational interference model and purge; account breakthrough/humidity and target loss. | `SYS-18`, `SYS-19`, `SYS-23` | Filter-before/after and bleed-through under standards; no ad hoc heated addon. |
| `08.12` Complex products | تطوير | Food/liquid/wood/composite fingerprints trained as classes only; avoid gas molecule labels. | `MQ-EXT-10`, `MQ-EXT-11` | Holdout physical specimen and sample origin. |
| `09.1` Robotic olfaction | تأجيل مشروط | Define robotic olfaction assumptions and recording pose/time/wind before claiming localization. | `SYS-12`, `SYS-23` | Time synchronized pose+wind+gas data. |
| `09.2` Gas mapping | تأجيل مشروط | Build gas occupancy field when spatial reference route exists, not from stationary chambers. | `SYS-12`, `SYS-10` | Ground truth release positions and wind. |
| `09.3` Source localization | تأجيل مشروط | Estimate source position only with transport dynamics, wind and reference source geometry. | `SYS-12`, `SYS-17` | Cross-plume trials with measured flow. |
| `09.4` Active sampling | إعادة تصميم | Active sampling as controlled experiment: adjustable intake, clean-air purge, pauses and motion; maintain time-lag aware data model. | `SYS-11`, `SYS-23` | Measured sample delay, pump/fan phases and normalized chamber mixing. |
| `09.5` Spatiotemporal fusion | تأجيل مشروط | Combine aligned robot pose plus time series after wind and inlet impulse response are available. | `SYS-12`, `SYS-17` | Independent field plume campaigns. |
| `09.6` Motion decisions | تأجيل مشروط | Motion-to-source safety and gas map decision policy requires validated source cues; movement safety remains independent. | `SYS-12`, `SYS-23` | Safe navigation + independently verified plume orientation. |
| `10.1` Negative Evidence & Sensor-Guided Exclusion | إعادة تصميم | Negative evidence only with calibrated detector sensitivity/false negative under concentration, mixture and humidity; forbid exclusion from non-response alone. | `SYS-04`, `QSPR-REF-09` | Species-conditional detection power, unknown-class caution. |

## الاستئناف في حالة انقطاع الاتصال

افتح بهذا الترتيب: [الخطة الآلية](IMPLEMENTATION_PLAN_80_2026-10-21.json) → [مصفوفة المراجع](CHEMCORE80_REFERENCE_GAP_MATRIX_2026-10-21.json) → [سجل حالة التنفيذ](IMPLEMENTATION_STATUS_80_2026-10-21.json) → [نقطة الاستئناف](RESUME_IMPLEMENTATION_80_2026-10-21.md) → تقارير الدفعات المرقمة → رابط CI وcommit لكل إنجاز.

**عندما لا تتوفر بيانات غاز فيزيائية مناسبة، لا تُغلق G3 ولا يُغيّر وضع التقنية إلى RESEARCH_VALIDATED.**
