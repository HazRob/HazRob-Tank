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
