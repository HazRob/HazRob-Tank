# مراجعة مرجعية سريعة — ChemCore Next — 2026-11-02

**النطاق:** مراجعة مستقلة لسجل HazRob المركزي قبل استكمال محور GHS، دون تعديل قاعدة المركبات أو استبدال ChemCore V6.

## المرجع المركزي الحاكم

- الفهرس الأصلي: https://github.com/HazRob/build-harness/blob/main/library/reference-bank/INDEX.md
- البيانات الآلية: https://github.com/HazRob/build-harness/blob/main/library/reference-bank/external-research/master_catalog.json
- Git blob SHA: 248e6006a3bac6f814a6e92d9d334c3d7fb4cf73
- المجموع: **94 سجلًا مرجعيًا** بتاريخ 2026-10-30. ليست 94 تجربة مستقلة أو 94 منحنى غاز مؤهلًا.
- الاستحواذ: https://github.com/HazRob/build-harness/blob/main/library/reference-bank/external-research/reviews/MQ_MEASUREMENT_ACQUISITION_2026-10-30_AR.md

## الفرص مرتبة حسب الأولوية

| الأولوية | معرف المصدر | القيمة الدقيقة | ما يجب إدخاله في تقييم ChemCore Next | القيد |
|---|---|---|---|---|
| عاجلة | MQ-EXT-21 | Furuta 2022 MQ4 + Li-Cor methane: 14,186 صفًا، 3 وحدات، 11 يومًا؛ ميثان مرجعي 1945–9998 ppb، Rs بالأوم وT/RH | التباين بين الوحدات، تصحيح البيئة، grouped day/instance holdout، معايرة عدم اليقين ورفض الإنذارات غير الموثوقة | بيانات خارجية، R0 الروبوت غير معلوم، G3 غير مكتمل |
| عاجلة | MQ-EXT-15 | UCI1081: 90 تعرضًا، MQ135 ضمن 10 قنوات، ستة أبخرة × 50/100/200 ppb ×5 مكررات | open-set, leave-one-gas/concentration-out، التمييز بين المرشحين | ppb لا ppm؛ نموذج خارجي غير مطابق لثمانية حساسات Tank |
| عالية | MQ-EXT-03 | 6400 صف ADC خام بست حساسات MQ، أربع فئات NoGas/Perfume/Smoke/Mixture | فحص فئات المخاليط العامة، عدم تأكيد CAS من فئة Smoke، امتناع التصنيف | مرايا المصدر لا تمثل تجارب إضافية، ليس غازًا مرجعيًا خالصًا |
| عالية | MQ-EXT-24 | 114 صف جهد MQ3/MQ4/MQ8؛ تناظرات بيئية وCO2 من جهاز منفصل | جودة الإشارة، معالجة النقص، وسوم القنوات وعدم المبالغة في أدلة ppm | نحو 15 ثانية فقط، من دون تركيز غاز معيّن موثق |
| عالية | SYS-06/07/08 | KPCA/KNN/MVRVM + sparse nonlinear least squares + نموذج تداخل SnO2 | فصل نوع الخليط عن تقدير تركيز مكوناته، اختبار identifiability | مصادر منهجية غالباً بغير raw أصلية؛ غير مثبتة على MQ Tank |
| عالية | SYS-17/23/24 | استرداد الإشارات البطيئة، ثلاث مراحل sampling/purge، تعويض drift | تطوير التحليل الزمني 4D، جودة الجلسة، ذاكرة الحساس | ليس الفصل الكيميائي نفسه؛ أجهزة MOS غير MQ غالباً |
| متوسطة | MQ-EXT-26/27 + SYS-01 | تجارب 12 VOC على MQ2–MQ9؛ إيثانول/بروبانول ثنائية؛ channel ablation | فصل الخلطات، أهمية كل قناة، اختيار ميزات قابلة للنقل | ملفات تعرضات الغاز الخام ليست محفوظة/مؤهلة محليًا |
| متوسطة | R0-PEER-01 وCN-MQ-01..04 وQSPR-REF-10 | تعريف R0/R_air/RL ومرجع المصنع وPubChem | معايرة source K وحل هوية CAS/المادة المعروفة فقط | PubChem لا يوفر منحنيات MQ، وR0 ليس قيمة موحدة لجميع الوحدات |

## نتائج حقيقية سابقة يجب إدخالها في تصميم البوابات

تم تحليل MQ-EXT-21 بحثيًا في ChemCore Tank V6:
- HGB ثلاث MQ4 + T/RH: MAE = 0.8021 ppm مقابل baseline من دون MQ = 0.8767 ppm؛ فرق محدود.
- تغطية فاصل تنبؤ اسمي 90% تحققت بنسبة 82.62% فقط.
- عند حد بحثي CH4>=4 ppm: كشف 533 من 2346 حالة، أي حساسية 22.7% تقريبًا، و1044 إيجابية خاطئة؛ لا يصلح كنظام إنذار.
- في 33 اختبار انتقال جهاز/يوم محجوب: متوسط MAE = 1.1561 ppm، مع حالات فشل تامة لاسترجاع حالات فوق 4 ppm بوحدتين محجوبتين في اختبار Java آخر.
- تلك أرقام تجربة خارجية سابقة **وليست أداء ChemCore Next ولا معايرة أو مواصفة Tank**.

## حقائق تمنع تكرار المصادر أو المبالغة

- 94 سجلًا تشمل 19 مجموعة قياسات خارجية، 30 دراسة طرق تحليلية، 9 QSPR، 8 دراسة R0/Rs، وغيرها؛ لا تُجمع كتعرضات.
- مجموعتا MQ-EXT-21 وMQ-EXT-24 حملتا بايتات خام مع SHA256، لكن تحفظ في Library شخصية وGitHub Actions artifacts؛ لا توجد CSV خام مكشوفة ضمن مجلد الفهرس المركزي نفسه.
- 43 منحنى V6 أعيد استخدامها في MQDataScience، فلا تحسب كدليل ثانٍ، و109 سجلات معادلات ChemCore Next ليست 109 تجارب.
- UCI224 وUCI270 وجهان لمصدر مترابط؛ SYS-11/UCI308 في بيانات G2 القديمة، لا تُكرر.
- ثلاثة ملفات MQ-FOOD-21..23 للأغذية فقط، لا تربط بتركيزات غاز نقي.
- بعض مراجع Mendeley ما زالت غير قابلة لتنزيل raw بسبب HTTP401؛ لا تضع RAW_VERIFIED افتراضًا.
- MQ-EXT-16 (45 مستشعرًا ومرجع SIFT-MS) جذابة لاختبار الإسناد المرجعي لكنها cross-platform، وraw لم يفحص بعد.
- مقارنة R0 المنشورة في Dorcea 2018 تضم عدم اتساق حسابي في MQ7 وMQ8: المذكور 6.944 و1.708 kΩ مقابل 2.944 و0.641 kΩ عند حساب Rs_air/K؛ لا ينقل كلا الرقمين كمرجع جهاز.

## قرارات البحث التالية

1. لا تنشئ سجلًا مركزيًا موازيًا؛ استعمل ID وsource root ونسخة الملف وlicense.
2. اجعل MQ4 device/day holdout وcoverage وmiss rates اختبار أساس إلزاميًا لـChemCore Next G2؛ قارن في المصدر نفسه ولا تُعيد عد الصفوف.
3. كرر UCI1081 فقط إن كان هناك بروتوكول تقسيم/فرضية مختلفة؛ لا تسوّق الاختبار على أنه 8-channel Tank benchmark.
4. انقل من SYS-06/07/08 منهجيات mixtures بعد تحقق identifiability؛ ومن SYS-17/23/24 ميزات الاستجابة والعودة إلى clean air.
5. لا تنشئ تقنية 81 ولا تعرض منحنيات ناتجة عن محاكاة على أنها تعرضات جديدة.
6. التحقق الفيزيائي للجهاز ما زال G3=0/80، وبنك المراجع لا يؤهل GHS أو ppm أو إنذارًا ميدانيًا من تلقاء ذاته.
7. لا تغيير في ChemCore Next runtime ولا جدول المركبات الموحد ولا APK أو الفريموير بهذه المراجعة.

## مراجع التقرير

- https://github.com/HazRob/build-harness/blob/main/library/reference-bank/external-research/reviews/ANALYTICAL_SYSTEMS_MIXTURE_UNMIXING_QUANT_FILTERS_2026-10-21_AR.md
- https://github.com/HazRob/build-harness/blob/main/library/reference-bank/external-research/reviews/MQDATASCIENCE_AND_EXPANDED_DATA_2026-10-21_AR.md
- https://github.com/HazRob/build-harness/blob/main/library/reference-bank/external-research/calibration/MQ_R0_RS_CROSS_STUDY_AUDIT_2026-10-21_AR.md
- https://github.com/HazRob/build-harness/blob/main/library/reference-bank/external-research/reviews/MQ_MEASUREMENT_ACQUISITION_2026-10-30_AR.md

**الحالة:** QUICK_REVIEW_ONLY؛ لا مهام G2/G3 جديدة مكتملة، لا تغيير بنيوي أو تشغيل جديد.
