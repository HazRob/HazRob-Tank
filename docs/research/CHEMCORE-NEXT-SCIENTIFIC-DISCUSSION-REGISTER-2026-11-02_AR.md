# سجل المناقشات العلمية والقرارات — ChemCore Next / HazRob Tank V8
**تاريخ التأسيس:** 2026-11-02
**المستودع:** `HazRob/HazRob-Tank`
**الفرع:** `feature/tank-v8-implementation`
**الحالة:** `SOURCE_AUDIT_PARTIAL / SCIENTIFIC_DISCUSSION_OPEN / DESIGN_UNFROZEN`
**الغاية:** حفظ مصادر وأدلة وملاحظات التطوير، واستقبال الأفكار الجديدة للنقاش قبل اتخاذ قرار تنفيذي.

## 1. القرارات المعتمدة
| الرمز | القرار | الحالة |
|---|---|---|
| DEC-NEXT-001 | تخصص V8 لتطوير منظومة ChemCore V6 المستقلة إلى جيل باسم **ChemCore Next** | معتمد |
| DEC-NEXT-002 | مراجعة MQDataScience كاملة ومقارنة التقنيات والمعادلات مع V6 قبل اختيار وظائف النقل | معتمد |
| DEC-NEXT-003 | مراجعة MQSensorsLib (مكتبة R0 التاريخية) ومستودع MQSensorsLib_Docs لمعرفة مصادر منحنيات وأساليب المعايرة | معتمد |
| DEC-NEXT-004 | لا تعديل في محرك V6 المرجعي أو حذف أدلة سابقة؛ تُختبر أي إضافة إلى Next مستقلًا؛ أغذية النضارة خارج محرك الغاز | معتمد |
| DEC-NEXT-005 | تعليقات المستخدم القادمة تدخل **سجل المناقشات** أولًا، ولا تصبح متطلبات نهائية أو تغييرات تشغيلية إلا بعد المراجعة | معتمد |
| DEC-NEXT-006 | لا إعلان نجاح دقة الغاز أو PPM أو هويته أو صلاحية السلامة دون اختبارات G1/G2/G3 المناسبة | سياسة أدلة معتمدة |

## 2. مصادر أساسية مثبتة
### MQDataScience
- المصدر: https://github.com/abcdaaaaaaaaa/MQDataScience
- النسخة الجاري تدقيقها: `MQSpaceData 7.0.0`؛ المقارنة التاريخية مع `v6.0.0`.
- `DataScience/Requirements/PredictData.py` (blob `bacf95948714dbe76f09c57e3db14cbc60616580`): فهرس 76 نموذجًا للانحدار والتنبؤ، **ليس** 76 مصنف غازات.
- `DataScience/Requirements/ModelPredictData.py` (blob `efee443fa681cd8ad1f3a19b6d44de5a4a156853`): مسار إضافي ذي 66 اسم نموذج، يتداخل في 65 اسمًا مع الأول؛ الاتحاد 77 اسمًا (ليس برهانًا على استقلالية الخوارزميات).
- `DataScience/Requirements/MQInfo.py` (blob `c9519bcebd22c79fadeaa6b21d22d4fb821ba290`) و`src/SensorDefinitions.cpp` (blob `d432cae3f2d3043f6618488e44f054293ad5fa36`): 89 علاقة منحنى مصدر لـ21 نوع/وضع حساس؛ تطابقت معاملات a,b في مراجعة المصدر 89/89؛ 43 من النماذج الثمانية للدبابة تطابقت عدديًا مع `SourceCatalog.java` بـV6؛ بقي الالتباس الاسمي `NH4` مقابل `NH4/NH3`.
- `DataScience/3D_Surface/3DSurface.py` و`4D_Curve/4DCurve.py`: توابع شكلية لنمذجة التغير مع T/RH والزمن؛ لا تمنح هوية مركب معتمدة.
- فارق المصدر v6→v7: انتقال حساب `CalValue` من الاستيفاء الخطي إلى اللوغاريتمي في عدة ملفات، وتغيرت أربعة معاملات `calvalue` في MQInfo؛ دون اختبار مقابل قياس الدبابة، لا تُنقل القيم.
- ملف تتبع 76: `docs/research/CHEMCORE-NEXT-MQDATA-76-MODEL-GAP-2026-11-02.csv`. السجل التاريخي V6: 14 Android مسجلة (منها تقريبان) / 62 غير منقولة، وهو **ليس** عدد النماذج المُفعّلة في مسار تشغيل V6 ولا إثبات G2/G3.

### MQSensorsLib / معايرة R0
- المصدر: https://github.com/miguel5612/MQSensorsLib
- المرجع المتحقق: `master` commit `59a2e5852ec12f5da3cf508be68f5a9405314003`; `library.properties` يذكر `MQUnifiedsensor 3.0.5`. الترخيص MIT.
- أصل C++: `src/MQUnifiedsensor.cpp` blob `5b282c7c178f41478d2de2258d98b54ce269d407`, header blob `11637c2c0132cfc97c57b9482a057e0fa20dc382`.
- المرجع التوثيقي: https://github.com/miguel5612/MQSensorsLib_Docs
- دفتر نقطة المصدر الخام: https://github.com/miguel5612/MQSensorsLib_Docs/blob/master/Internal_design_documents/Experiments/Datasheets%20info.xlsx ؛ ودفاتر `MQ*_Regression.ipynb` متفرعة منه، راجعها جزئيًا من حيث بنية الشيفرة ولم تُعَد نتائجها التجريبية.
- نسب مصدر الهواء النظيف في أمثلة المكتبة: MQ2=9.83، MQ3=60، MQ4=4.4، MQ5=6.5، MQ7=27.5، MQ8=70، MQ9=9.6، MQ135=3.6؛ نسب `Rs_air/R0` بلا أبعاد، **ليست R0 بوحدة kΩ ولا ppm**.
- المعادلة `Rs=(Vcc*RL/Vout)-RL` و`R0_source=Rs_air/K_air`. يجب فصل `R_air` المرجعي لمراقبة تغير الهواء عن `R0_source` الخاص بمنحنى مصدر محدد.
- مزايا قابلة للاختبار: `externalADCUpdate`، `validateEcuation`، اتجاه `Rs/R0` أو `R0/Rs`، أنواع انحدار مختلفة، وأمثلة ADS1115 وMQ-135-ALL وMQ7/MQ9.
- تحذيرات: في `examples/MQ-Board/MQ-Board.ino` تبدو معايرات عدة قنوات مستدعية لكائن `MQ2` خطأً؛ وتختلف آليات التعويض البيئي بين المصدرين. MQ7/MQ9 يعتمدان على طور السخان وتعريف الدائرة، ولا يجوز افتراض توافق منحنى المصدر مع توصيل الدبابة.
- للمزيد: `docs/research/CHEMCORE-NEXT-MQSENSORSLIB-R0-CURVES-AUDIT-2026-11-02_AR.md`.
- لدى V6 جدول تدقيق إضافي: `docs/research/V6-CHEMCORE-MQUNIFIED-R0-RATIO-AUDIT.csv` يتضمن تعارضًا حسابيًا في سجلات خارجية قديمة لـMQ7/MQ8؛ لا تستخدم R0 لها كمعايرة Tank.

### ChemCore V6
- الفرع الأساسي المرجعي `feature/tank-v6-implementation`؛ الشيفرة في `android/app/src/main/java/com/hazrob/tank/chemcore/v6/`.
- سجل 80 تقنية V6: `docs/implementation/V6-CHEMCORE-80-TECHNIQUE-NEW-ENGINE-COVERAGE-2026-10-29.json`، حالة 78 تنفيذ جزئي، 2 غير منقولتين، 0/80 G3 ميداني.
- مؤشرات التكامل الواجب عزلها في Next: بناء مرشحين القياس الآني من ترتيب فرضيات مصادر بدل `Engine.Result.rankedCandidates`، واستمرار استدعاء استدلال تاريخي في مسار بعض الجلسات.
- لا تُساوى أرقام التشابه العلمي أو تقدير PPM المستنبط من منحنى بدقة جهاز معتمد.

## 3. ملاحظات تحليليّة وفرضيات للمناقشة — غير معتمدة بعد كمتطلبات نهائية
- **HYP-01 Dual baseline**: لكل حساس `R_air` و`R0_source` منفصلان مع `K_air` وهوية المصدر وR0 definition وعدم إعادة تسمية أحدهما صامتًا. يحتاج اختبار صلاحية عامل K للحساس الحقيقي وطور السخان.
- **HYP-02 Provenance-aware library**: تسجيل كل مصدر/منحنى ونوع وحدة ومدى PPM وهوية الغاز الأصلية ونسبته إلى دفتر مصنع؛ لا يعد تكرار المنحنى عبر مكتبتين دليلًا مستقلًا.
- **HYP-03 Parallel analyses**: 3D و4D والانحدار والبصمة أدلة بحثية منفصلة؛ تُدمج فقط عند توافق فيزيائي/تجريبي مثبت.
- **HYP-04 Unknown/mixtures**: حالة UNKNOWN والتحليل المشروط بوجود غاز واحد أو خليط، وعدم إجبار النظام على اختيار أحد أصناف المكتبة.
- **HYP-05 Evidence-quality gates**: قرار الاستدلال منفصل عن سلامة الأجهزة والإنذارات، مع G1/G2/G3 ومسار تحقق مستقل؛ والتقارير تحمل أسباب عدم اليقين وعدم الصلاحية.
- **HYP-06 76-model workbench**: اختيار نماذج مناسبة خارج تشغيل Android في البداية؛ منع تسرب تقسيم الزمن/التعرض وتضخم الأداء باختيار أفضل R² من 76 دون test محجوز.
- **HYP-07 immutable integration**: للجلسات والقياس الآني تقرير مشتق من نتيجة واحدة كاملة، وليس ترتيبًا مصدرًا أو نصًا ثابتًا.
- **HYP-08 heater validity**: تقييم MQ7/MQ9 مع طور السخان والجهد؛ تغيير الفريموير قرار مستقل يحتاج مراجعة تصميم الدارة.

## 4. مسائل مفتوحة قبل تجميد المواصفات
- ما ناتج ChemCore Next الذي يراد تحقيقه وتقييم دقته على نحو مستقل؟ كشف التغير؟ تحديد الهوية؟ تحليل خليط؟ PPM؟ أم جميعها بمستويات ثقة وبيانات مختلفة؟
- هل تُفعَّل 3D/4D في المسار الآني أم على السلسلة الزمنية في الجلسات فقط؟ وما معدل ومدة أخذ العينات الفعلي؟
- كيف تُعامل قيم R0 المقاسة سابقًا إذا لم يُعرف تعريفها أو ظروفها؟ لا تحويل صامت.
- ما إجراءات التجارب الميدانية المتاحة من غازات مرجعية وضبط اختلاط/تركيز ووسائل تهوية؟ لا تُجرى تجارب مواد قابلة للاشتعال/سامة في بيئة غير مجهزة.
- ما معيار الترقية لكل نموذج، وكيف نمنع تكرار أدلة المصدر والانحياز لقياسات المصنع؟
- ما مستوى الشفافية المطلوب في الواجهة: الأدلة البحثية، أسباب الرفض، نسب/مسافات التشابه، رسوم 3D/4D؟

## 5. سجل الأفكار والقرارات القادمة
**السجل مفتوح:** تدرج أفكار المالك الواحدة تلو الأخرى بالحقول `ID | Idea | Scientific basis | Decision | Required tests | Integration gate | Status`. لا يُفسّر تسجيل فكرة بأنها «اعتماد تقني» ولا يُعدّل محرك V6 أو يؤهل Next دون مراجعة.

## 6. وثائق التطوير المقترنة
- `docs/research/CHEMCORE-NEXT-MQDATASCIENCE-SCIENTIFIC-AUDIT-2026-11-02_AR.md`
- `docs/research/CHEMCORE-NEXT-MQDATA-76-MODEL-GAP-2026-11-02.csv`
- `research/chemcore_next/mqdata_v7_source_gas_curve_register_89.csv`
- `docs/research/CHEMCORE-NEXT-MQSENSORSLIB-R0-CURVES-AUDIT-2026-11-02_AR.md`
- `docs/architecture/CHEMCORE-NEXT-ADR-2026-11-02_AR.md`
- `docs/implementation/V8-IMPLEMENTATION-TRACKER-2026-11-02.json`

**آخر نقطة استئناف:** `CHEMCORE_NEXT_SOURCES_CAPTURED_DISCUSSION_OPEN; NO_NEW_MODEL_RUNTIME; NO_APK; TANK_G3=0`.


## 7. مقترحات مالك المشروع العلمية السبعة عشر — جلسة 2026-11-02
**الحالة الحاكمة:** OWNER_PROPOSALS_RECORDED / SCIENTIFIC_REVIEW_IN_PROGRESS / NO_FINAL_ADOPTION / NO_RUNTIME_CHANGE.
**إجراء:** تحفظ الأفكار بنصها المفاهيمي ولا تفسر هذه المراجعة بأنها اعتماد تنفيذي أو نتيجة اختبار. الترميز S-01..S-17 مطابق لترقيم صاحب المشروع.

| المعرف | مقترح المالك | التقدير العلمي/التحفظ | اختبار أو شرط التأهيل | الحالة |
| --- | --- | --- | --- | --- |
| S-01 | أولوية متفردة لاستجابة الحساس الموجه لغاز بعينه ولو اختلفت باقي النتائج | مهمة؛ حساسية المصدر للغاز لا تعني انتقائية مطلقة. لا تحجب الأدلة أحادية الحساس ولكن تسميها استجابة/ترشيحًا ما لم تؤهل الهوية | cross-sensitivity، مجال الكشف، التداخل، جودة المعايرة، تحقق Tank | للمناقشة |
| S-02 | إظهار العلاقات بين حساسات الغازات المتشابهة، مثل الدخان وCO وCO2 | لا يجوز جعل CO أو CO2 تحصيل حاصل من كشف الدخان، فهي متغيرات مختلفة؛ استخدم الارتباط المشروط والسياق لا التكافؤ | تجارب حرق/لا حرق وغازات منفردة وPM وCO وCO2 بمرجع مستقل | للمناقشة |
| S-03 | تشغيل كل من البصمات والمنحنيات واللوغاريتمات باستقلال ثم التحكيم | ملائم لتعدد الأدلة؛ لا يحسب المنحنى اللوغاريتمي ونفس مصدره مرتين كدليلين | مقارنات ablation، source independence، blind holdouts | للمناقشة |
| S-04 | مراجعة التجارب محليًا ومركزيًا وتوليد Stable بالتعلم الآلي والشبكات العصبية دون تعارض | احتفظ بالسجلات الخام والإصدارات؛ لا تعلم مباشر من قياس واحد أو رأي غير موثق | traceable provenance، drift، تجميعات مستقلة، حوكمة نسخ Stable | للمناقشة |
| S-05 | تطابق محرك Next عكسيًا مع التجارب والمختبر المركزي | نوصي بنموذج forward sensor response ثم inverse inference مع حالة خليط/مجهول | residual reconstruction، predictive checks، held-out experimental groups | للمناقشة |
| S-06 | استكمال جدول المركبات وحدودها الطبيعية والخطرة قبل التقرير | مطلوب؛ الحد الطبيعي ليس معروفًا لكل مركب، وبعض المواد لا تملك نطاقًا طبيعيًا ذا معنى؛ فصل background/LOD/exposure/TWA/STEL/ceiling/LEL | مصادر وتواريخ وصلاحية وحدات وناقلات قياس وقيم مفقودة واضحة | للمناقشة |
| S-07 | ذكاء حقيقي في التعامل مع الأدلة والاحتمالات المتقاطعة | نوصي بمحرك عوامل/أدلة واعتماد عدم اليقين مع UNKNOWN ومخاليط | calibrated likelihoods عند توافر بيانات ملائمة، FPR/FNR تحت تركيبات متقاطعة | للمناقشة |
| S-08 | توحيد مصادر القوائم وعلاقات كل طرق التحليل والتقارير | قاعدة معرفية واحدة versioned ذات معرفات CAS/سجل هوية وsensor/model/provenance IDs، ومخرجات تحليل غير قابلة للتحريف | schema migration، UI/report/session/instant parity | للمناقشة |
| S-09 | تحقق من أن مرجعية R0/الهواء النظيف المطبقة في V7 فعالة بالقياس الآني والجلسات | المصدر الحالي يؤكد CleanAirResponseGateV6 بنسبة انخفاض provisional 10% وشرط نموذجين؛ لا نثبت تكافؤ المسارين أو صحة تسمية الخصم | تدقيق V7 raw→Rs/R_air vs Rs/R0، حالات نظيف وغاز وmissing R0، exact parity replay | قيد التحقق |
| S-10 | غياب استجابة حساس الميثانول المتخصص ينفي ترشيحات الميثانول | لا يوجد ضمن الثمانية MQ2/3/4/5/7/8/9/135 حساس ميـثانول حصري مثبت. لا نفي إلا مع valid calibrated selective negative observation وLOD وغياب masking | قياس سلبي موثق مع مادة مرجعية وحالات تركيز دون LOD وعطل الحساس | للمناقشة |
| S-11 | مستخدم السلامة يصحح المرشحين قبل التقرير؛ وتستخدم التصحيحات لتعليم المختبر | ميزة قوية؛ السياق مثل مخزن إيثانول لا يثبت غياب ميثانول مجهول. لا حذف قياسات أو إنذارات مستقلة، بل annotate/contest، مع تقرير preliminary/final، وraw immutable | واجهة مراجعة provenance، فصل user assertion عن ground truth، audit trail، safety policy | للمناقشة |
| S-12 | احترام مدى MQ مع ابتكار توسيع لقدرات التحليل | زيادة التمييز يمكن تحقيقها ببصمات ومصفوفات وحساسات مضافة؛ لا تمدد نطاق LOD أو ترفع صفة اعتماد دون تحقق فيزيائي | detection/selectivity/range bench and cross-validation | للمناقشة |
| S-13 | ثمانية MQ الآن مع دعم توسع تركيبات الحساسات | واجهة sensor capability registry dynamic وليس hardcoded 8، وتسجيل الهوية الفريدة والطور والتحكم | missing/present/degraded/new-sensor regression | للمناقشة |
| S-14 | تحليل أحادي وثنائي وجماعي وتنافري ومصفوفي | مفيد، مع ضبط الارتباط والتكرار بين الدلائل من نفس القياس، وتعقيد المجموعات | pairwise ablation, matrix tests, diversity and condition-dependence | للمناقشة |
| S-15 | هرم احتمالات واسعة ثم تقليص واستثناء وتعزيز | مناسب مع إبقاء UNKNOWN والمخاليط وعدم الاستبعاد القاطع بقرينة ضعيفة | confusion matrix, open-set recall, traceable pruning reasons | للمناقشة |
| S-16 | feedback حتى في وضع تتبع غاز معروف | تسجيل true/false/unsure وسبب المستخدم وقراءة مرجعية إن توفرت؛ عدم اعتبار تأكيد المستخدم شهادة تجريبية | labeled-feedback quality and central moderation | للمناقشة |
| S-17 | مساهمات مجتمع المستخدمين عبر تبويب ونظام رفع ومراجعة قبل Stable | واقعي؛ يشبه جمع/وسم/تدريب البيانات لدى Bosch BME AI-Studio من حيث منهج التدريب، وليس إثباتًا أن Bosch تقدم نظام crowdsourcing متطابق | quarantine→triage→provenance→replicate→G2/G3→promote, privacy and consent | للمناقشة |

### 7.1 تصور طبقات ChemCore Next المستنبط من هذه المقترحات — افتراضي للنقاش
1. Hardware/Telemetry Quality: سلامة جهد ADC والريلاي والسخان والطور والتوقيت وملف المعايرة ونطاق القياس، والفصل بين فقد الإشارة والغياب الفيزيائي.
2. Dual Physical References: R_air للإزاحة المرجعية واستجابة تغير الهواء، وR0_source لمنحنيات المصنع وفق K_air المنسوب للمصدر؛ لا نساوي R0 والطرح الحسابي.
3. Sensor-specific Evidence: نتيجة مستقلة لكل حساس، ودلالة إيجابية وسلبية مشروطة بعلاقة الحساسية والانتقائية والـ LOD.
4. Independent Experts: source curve، empirical fingerprint، temporal/dynamics، 3D T/RH compensation، optionally ML؛ نسخ وإسناد وعدم دمج آلي لكل المعاملات.
5. Hypothesis Graph: ربط candidate gas أو mixture بالاستجابة المتوقعة للحساسات ونوع الجهاز/المواد والسياق وUNKNOWN وتعارض المخرجات، مع منع double counting.
6. Forward/Inverse Laboratory Check: إعادة إنتاج vector/trajectory المتوقع من الغازات المختارة، وإظهار residual وأدلة النفي والبدائل.
7. User Review & Safety: عرض الأدلة والمرشحين قبل إصدار تقرير تفسير نهائي، مع الاحتفاظ بالتقرير الآلي الأصلي وعدم حجب القياسات أو التنبيهات المؤهلة بقرار المستخدم.
8. Immutable Results + Central Feedback: حفظ raw observation وcandidate ledger وتعليقات المستخدم وسجلات تجارب المؤسسة؛ pipeline تأهيل منفصل للتحديث في Stable.
9. Multi-mode parity: آنـي وجلسات وكشف عام وتتبّع يستدعون واجهة قرار مشتركة، ويختلفون فقط في طول السلسلة الزمنية والوظائف المفعلة.

### 7.2 مشكلات الأولوية والتحفظات العلمية
- MQ3 حساس كحول وليس كاشفًا حصريًا للميثانول؛ وMQ137 الخاص بالأمونيا ليس جزءًا من الثمانية الحالية ولا هو انتقائي بالكامل للأمونيا.
- لا تُفسر استجابة الدخان أو MQ135 كقياس CO2 نوعي، ولا يُستنتج CO من CO2 أو بالعكس بلا حساس ومرجع خاص.
- لا يمكن احتساب اختلاف منحنيات مبنية على datasheet واحد كأدلة مستقلة.
- السجل يميز observation وgas hypothesis وvalidated identity وexposure alarm. حد NIOSH/OSHA الزمني لا يستبدل مباشرة حد تغير هواء لحظي.
- عند تعارض مراجعة المستخدم مع خطر فيزيائي مؤهل: يُحتفظ بالخطر ويظهر الاعتراض، ولا يجوز للمراجعة إبطال جهاز السلامة المؤهل أو حذف أصل القياس.
- حالة MQ7/MQ9 تحتاج مراجعة heater phase الفعلية ودائرة Tank؛ لا تعادل دقائق التسخين الروتينية التهيئة الأولية الطويلة.
- الثقة العددية أو احتمال الهوية يتطلب calibration على تجارب مستقلة؛ في البداية عرض مستويات أدلة وأسباب، لا نسب احتمالات اختراعية.

### 7.3 المراجع الرسمية المرتبطة بالنقاش
- Winsen MQ137 حساسية للأمونيا والأمينات: https://www.winsentech.com/Semiconductor_NH3_Sensor_MQ137.html
- Winsen MQ3B حساس كحول: https://www.winsentech.com/Semiconductor_Alcohol_Sensor_MQ-3B.html
- Winsen MQ135 غازات متنوعة: https://www.winsen-sensor.com/manual/mq135.html
- Winsen MQ-7B heater phases CO: https://www.winsen-sensor.com/sensors/co-sensor/mq-7b.html
- NIOSH direct-reading monitor evaluation: https://www.cdc.gov/niosh/docs/2012-162/default.html
- OSHA direct-reading instrument calibration guidance: https://www.osha.gov/publications/shib093013
- ISO 6145-1:2019 gas calibration mixtures: https://www.iso.org/standard/53598.html
- ISO/IEC 17025:2017 laboratory competence: https://www.iso.org/standard/66912.html
- Bosch BME AI-Studio train/evaluate: https://www.bosch-sensortec.com/software/bme/docs/overview/getting-started.html

**استنتاج أولي:** تقترح مناقشة Next فصل أولويات تخصص الحساس عن قوة إثبات هوية الغاز، وعزل رأي المستخدم عن الحقيقة التجريبية، وإعداد اختبارات تماثل ومصداقية بين الأوضاع. لا يوجد اعتماد نهائي لميزات Next من هذه الملاحظات، ولا اختبار G3 جديد، ولا تعديل للفريموير أو APK.
