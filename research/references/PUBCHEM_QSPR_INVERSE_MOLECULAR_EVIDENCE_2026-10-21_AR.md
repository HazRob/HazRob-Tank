# HazRob Tank — مراجعة QSPR/PubChem والتحليل الجزيئي العكسي
**التاريخ:** 2026-10-21، توقيت اليمن. **النوع:** مراجعة علمية بحثية موثقة / ليست دمجاً إنتاجياً.  
**الفرع:** `research/mq-open-measurements-audit-20261021`، **المراجعة:** PR #4 (مسودة غير مدمجة).

## 1) النتيجة المباشرة وما لم يثبت بعد
**نعم:** توجد دراسات ربطت **الواصفات الجزيئية** باستجابات الأنف الإلكتروني، ودراسات MOS بحثية بنت نماذج ML تتنبأ باستجابة حساسات أكسيد المعدن لمركبات VOC باستخدام خصائص الجزيئات ومواصفات المواد. كما نشر فريق JPL تجربة تحليل عكسي لاستجابة غير معروفة باستخدام QSAR/QSPR مع ترشيح هوية `SF6` **احتمالياً**.

**لكن:** لم نعثر في هذا المسح على دراسة تُثبت أن `PubChem -> descriptors -> inverse chemistry -> unknown gas` مُعايرة وتجريبياً **على MQ2/MQ3/MQ4/MQ5/MQ7/MQ8/MQ9/MQ135 التجارية**. لا يصح نقل نتائج بوليمرات JPL أو أكاسيد معدنية محضّرة مختبرياً إلى MQ مباشرة.

**PubChem مصدر معلومات مركبات** (`CID`, `SMILES`, `InChIKey`, `MolecularWeight`, `XLogP`, `TPSA`, `HBondDonorCount`, `HBondAcceptorCount`, `Complexity`) وليس قاعدة فعلية لقراءات MQ أو معاملات `Rs/R0` أو استجابة السخان. لا نُخترع قياسات مقاومة من خصائص الجزيء.

## 2) الأبحاث الأساسية ومناهجها

| ID | المرجع الأصلي | الحساسات / التصميم | نتيجة ومدى الإثبات |
|---|---|---|---|
| QSPR-REF-01 | Shevade et al., *Developing sensor activity relationships for the JPL electronic nose sensors using molecular modeling and QSAR techniques*, IEEE Sensors 2005, DOI:10.1109/ICSENS.2005.1597683؛ https://www.researchgate.net/publication/4224523_Developing_sensor_activity_relationships_for_the_JPL_electronic_nose_sensors_using_molecular_modeling_and_QSAR_techniques | **مصفوفة حساسات بوليمر–كربون JPL** وليس MQ؛ بيانات استجابة متعددة الجزيئات عند ppm، `y=A1*c+A2*c²` ثم استخدام `A1` هدفاً لوصف QSAR؛ سمات جزيئية+طاقة تفاعل فيلم–جزيء باستخدام نماذج حسابية؛ GFA لاختيار الواصفات | يوجد تطبيق عملي تدريبي/اختباري على أنف إلكتروني محدد؛ دليل على طريقة **forward response prediction** لا على دقة MQ |
| QSPR-REF-02 | AIChE JPL 2006 *Sensor Material Selection and Response Modeling…*, https://proceedings.aiche.org/conferences/aiche-annual-meeting/2006/proceeding/paper/545b-sensor-material-selection-and-response-modeling-jpl-electronic-nose-using-molecular-modeling | حساسات بوليمرية؛ QSAR/GFA + `QSPR` + طاقة الارتباط وHansen cohesion descriptors وmolar volume | دليل تصميم فيزيائي ومعادلات ومقارنة مواد حساسة، **ليس تسلسلاً علمياً من PubChem إلى MQ8** |
| QSPR-REF-03 | Shevade et al. *Characterization of Unknown Events Observed by the Third Generation JPL Electronic Nose Using Sensor Response Models*، 2010؛ https://www.researchgate.net/publication/268580400_Characterization_of_Unknown_Events_Observed_by_the_Third_Generation_JPL_Electronic_Nose_Using_Sensor_Response_Models | رصد أنف JPL على ISS أحداثاً خارج قائمة المواد؛ استخدم نماذج QSAR + molecular dynamics ثم قارن descriptors (dipole، H-bond، molar refractivity، molar volume، solubility) بمكتبة مركبات SMAC | إحدى أبرز حالات **inverse candidate ranking**، والمادة المرشحة `SF6` **محتملة** وليست تعريفاً كيميائياً مستقلاً مؤكداً |
| QSPR-REF-04 | Guo et al., 2024, *Machine-learning-enabled exploitation of gas-sensing descriptors: five pristine metal oxides*, DOI:10.1016/j.cej.2024.152280، https://www.sciencedirect.com/science/article/pii/S1385894724037677 | خمسة أكاسيد `SnO2/ZnO/In2O3/NiO/Cr2O3` وثمانية VOC، خصائص مادة + جزيئات + عوامل التجربة → حساسية MOS | **تجارب MOS فعلية** مع نموذج تنبؤي؛ أكثر صلة بآلية MQ من البوليمر لكنه لا يقيس MQ تجارياً |
| QSPR-REF-05 | Su et al., 2025, *ML for discovery of descriptors for gas-sensing: doped metal oxides*, DOI:10.1016/j.talanta.2025.127594، https://www.sciencedirect.com/science/article/pii/S0039914025000803 | **872 مدخلاً تجريبياً من الأدبيات** و34 خاصية لمواد معدنية مطعمة، ANN محسنة بخوارزمية وراثية | مقاربة لتنقيب قواعد الاستجابة وعلاقة المادة الحساسة/الغاز؛ ليست اختبار Unknown بالضرورة |
| QSPR-REF-06 | *Physicochemical and statistical characterization … resonator sensors with carbonaceous films*, 2022، https://www.sciencedirect.com/science/article/pii/S221171562200145X | QSPR + LSER لتوقع سعة الامتصاص/تقسيم الغاز بين فيلم الاستشعار والهواء، مع واصفات بنيوية/طاقة | بحث فيزيائي مقارن للاستجابة، **المقياس تردد رنين مختلف عن مقاومة MQ** |
| QSPR-REF-07 | Chen et al., 2026, *Towards the design of artificial sensing materials via quantum-informed explainable AI*, https://link.springer.com/article/10.1186/s13321-026-01232-3 | `MORE-ML`: واصفات إلكترونية كمومية + بيانات تفاعل جزيئات روائح جسم مع المستقبل؛ CatBoost وSHAP | استشراف مواد/روابط حسية عبر ML قابل للتفسير؛ **ليس MQ**، ولا تدل بيانات التفاعل الحسابية وحدها على معايرة الغاز |
| QSPR-REF-08 | Debnath et al., 2023, *Comparing molecular representations, e-nose signals, and other featurization…*, DOI:10.1371/journal.pone.0289881، https://journals.plos.org/plosone/article?id=10.1371/journal.pone.0289881 | مقارنة SMILES وواصفات الجزيئات والطيف/الإشارات الحسية من **QCM electronic nose** لـ114 مركب رائحة | إثبات فائدة المقارنة بين تمثيلات جزيئية/حسية لمهام **رائحة مدركة**، وليست تجربة تنبؤ Rs لمستشعر MQ |
| QSPR-REF-09 | *Bio-Benchmarking of Electronic Nose Sensors*, https://pmc.ncbi.nlm.nih.gov/articles/PMC2712691/ | دراسة مقارنة أنوف أكاسيد المعادن وسجلات الروائح والمعايير البنيوية | قيد: مصفوفة MOS تميز الفئات الوظيفية بصورة أفضل من مواد متشابهة داخل الفئة؛ منع الثقة المفرطة |
| QSPR-REF-10 | **NCBI PubChem PUG-REST** رسمي، https://pubchem.ncbi.nlm.nih.gov/docs/pug-rest (محدث 2026-09-15) | `CID`, `InChIKey`, `SMILES`, MW, XLogP, TPSA, H-bond descriptors, structure search | **API لوصفات الجزيء فقط**؛ لا يتضمن استجابات MQ الفيزيائية ولا بيانات heater/R0 ودرجات حرارة القياس |

## 3) الفرق بين التنبؤ الأمامي والعكسي
1. **Forward QSPR:** (d_g = \mathrm{PubChem}(g)) ثم (\widehat{S}_{i,g}=f_\theta(d_g,m_i,T,H,V_h,c,\mathrm{history})) حيث (S) استجابة مقاومة حقيقية موثقة، (m_i) مواصفات الحساس الفيزيائي؛ النموذج يُدرب على **قراءات MQ/مرجع معلوم** لا على PubChem فقط.
2. **Inverse candidate inference:** من `observed time-aligned Rs/R0` نُنشئ تنبؤاً لكل `g` في مكتبة مرجعية محددة ونحل في حدود المعايرة `min_c distance(observed, S_hat(g,c,..))`. تُعرض عدة غازات محتملة وثقة/امتناع، **لا جزيء مجهول مؤكد**.
3. **Multi-analyte mixtures:** مع عدة غازات `g1..gk` يصبح التنبؤ (S_i=f_i(g_1,c_1,...,g_k,c_k,...)). تعلّم الاستجابة النقية لا يحدد التفاعل في الخليط. لا يجوز افتراض `Rs/R0` خطياً قابلاً للجمع على جميع الغازات. نحتاج عينات خلائط موسومة بمكونات وتركيزات مرجعية.
4. **Chemical-class priors:** يمكن استخدام المعالم الجزيئية (كالمجموعات الوظيفية والقطبية) في ترتيب سابق ضعيف أو اقتراح مجموعات مرشحة، مع عدم احتساب المعلومة نفسها مرة ثانية كدليل مستقل إذا كان نموذج الحساسية قد استعملها.

## 4) تجربة HazRob Tank المقترحة قبل أي دمج
الدفعة الأولى: التحقق من **MQ135-only** على **UCI1081** مع 6 غازات و3 مستويات ppb (90 تعرضاً)، لأن هذا المصدر مؤهل فعلاً لقياسات قناة MQ135 في مصفوفة خارجية؛ تضاف له أوصاف PubChem الثابتة لا على شكل ميزات صفوف مرتبطة باسم الغاز الحقيقي في الاختبار.

نقارن خمسة نماذج على **نفس تقسيم تركيزات المصدر المحجوبة بالكامل**:
- `B0`: fingerprint/classifier باستخدام MQ135 وحده دون الواصفات.
- `B1`: multi-channel baseline كما في أبحاث G2؛ **سقف مرجعي مختلف في العتاد**، لا أساس MQ8.
- `F0`: تدريب forward regression من `[known compound descriptor, c, sensor metadata]` إلى استجابة MQ135؛ عند الاختبار يتنبأ النموذج **بجميع المرشحين** وليس label الحقيقي.
- `I0`: معايرة المسافة بين MQ135 المقاس والاستجابات المتوقعة للغازات الستة، مع joint optimization للتركيز في مجال ppm/ppb المصدر فقط؛ مقارنة B0 دون معلومات اختبار مسربة.
- `I1`: دمج بصمات MQ135 والفيزياء/QSPR مع نموذج calibrated candidate ranking وتقييم `top-k`, `open set`, التباس الغازات المتشابهة و`abstention`.

**يجب الإبلاغ صراحة أن `UCI1081` قياسات 10 قنوات من جهاز خارجي، مع قناة MQ135 واحدة، ولا تقدم `RL/R0` وأطوار تسخين Tank الثمانية.** هذا اختبار feasibility لخوارزمية عامة وليس معايرة الروبوت.

**منع تسرب خطير:** لا تعطِ `PubChem descriptors` المرتبطة بتسمية الغاز الصحيحة للسجل المختبَر كمدخل للمصنف؛ فهذا يزوّده بالإجابة. يجب بدلاً من ذلك حساب الاستجابة المتوقعة لكل مرشح على حدّة ثم مطابقة القياس المجهول.

**التحقق الإضافي:** 
- `Leave-One-Molecule-Out` عند تطوير نموذج يعمم على جزيئات جديدة، مع تمييزه عن اختبار مستويات تركيز جديدة لنفس ستة جزيئات؛ عدد 6 مركبات قليل للغاية للادعاء بتعميم QSPR.
- `Leave-One-Session/Device-Out` عند توافر بيانات MQ إضافية؛ لا يجوز تكرار نفس الجزيء في train/test وادعاء generalization لغاز غير معروف.
- اختبار أثر حذف كل descriptor ودرجة collinearity، calibration error، NLL، التغطية، ومعدلات الامتناع وفصل الغازات المتشابهة وظيفياً.
- أي نتائج سلبية توثق؛ ربما **لا تضيف QSPR شيئاً** بعد استخدام بصمة MQ135، وهذا نتيجة صالحة.

## 5) نموذج توثيق PubChem
- مفتاح الهوية: `PubChem CID` مع `InChIKey` و`SMILES` وختم تاريخ ونسخة البيانات.
- أمثلة API المدعومة رسمياً:
  `https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/name/ethanol/property/MolecularWeight,XLogP,TPSA,HBondDonorCount,HBondAcceptorCount,InChIKey/JSON`
  `https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/name/acetone/property/MolecularWeight,XLogP,TPSA,HBondDonorCount,HBondAcceptorCount,InChIKey/JSON`
  البيانات المقاسة تجريبياً مثل ضغط البخار ودرجة الغليان هي مصادر annotations/مراجع ذات وحدات ودرجة حرارة، **لا تفترض أنها حقول PUG-REST مركبات قياسية أو ثابتة بلا سياق**.
- نظام بيانات `molecule -> descriptors -> gas_sensor_observation -> reference_source -> experiment_group`، مع فصل `QSPR_PREDICTED` عن `DEVICE_MEASURED`.

## 6) توصية لهيكلة المشروع
1. ربط هذا المسار بالتقنيات القائمة: `04.9 Physics+ML`, `02.5 Fingerprints`, `03.3 Identifiability`, `05.x concentration`, `06.1 evidence fusion`, `06.5 open-set`, `08.7 sim-to-real`, `08.8 mixtures`, `08.9 unmixing`, `10.1 negative evidence`.
2. **لا ننشئ التقنية 81 الآن.** نبدأ كتجربة هندسية داخل 04.9 ونسجل `ADR` إذا أثبت الاختبار فائدة منهجية مستقلة لا تغطيها 80 تقنية؛ يجب تحضير مواصفات قابلة للاختبار قبل تغيير عددها.
3. يُجلب PubChem كمرجع إلى المختبر/مكتبة المعرفة أولاً، ويُنشر للنظام الميداني فقط نموذج مختصر/مرشح معتمد بعد اختبارات مستقلة. لا يعتمد الروبوت على اتصال مباشر بخدمة PubChem أثناء الإنذار.
4. قاعدة الاستنتاج: **PubChem يقول شيئاً عن الجزيء، MQ يقول شيئاً عن استجابة خليط تحت شروط عتاد، والربط بينهما نموذج تجريبي يجب إثباته.**

## 7) حالة الإنجاز
- `REFERENCE_EVIDENCE_AUDITED`: نعم (مصادر الويب أعلاه).
- `PUBCHEM_DESCRIPTORS_FETCHED_FOR_6_GASES`: **لا** في هذه المراجعة.
- `FORWARD_QSPR_TRAINED_ON_REAL_MQ`: **لا** في هذا العمل.
- `INVERSE_QSPR_TESTED_ON_MQ135_UCI`: **لا** في هذا العمل.
- `TANK_8_MQ_SCIENTIFIC_G3`: **0/80**.
- لا تعديل APK/FW/ChemCore الإنتاجي.
