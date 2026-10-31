# HazRob Tank V7 — الخريطة الموحدة لتصميم التقارير الستة (بلا اختبارات)
**ID:** HZT-V7-REPORT-UX-MAP-002 | **Date:** 2026-10-31  
**State:** `DESIGN_DEVELOPMENT_ONLY / QA_PAUSED_BY_OWNER`  
**Branch:** `feature/tank-v7-reports`  
**Basis:** owner's G01–G18 rev 1.2, all report-specific owner approvals, R03 source-binding gate, R04 design detail.

## 1. قاعدة العمل الحالية
تجميد الأعمال الاختبارية والنشاط التحققي/البنائي مؤقتًا والتركيز على **هندسة وتقسيم صفحات التقرير، المكونات المرئية، جداول العرض، المخططات وشرحها**. لا تشغيل QA أو Android Emulator أو إنشاء بيانات تجريبية أو تحويل حالة تصميم إلى حالة إنتاج. يظل تنشيط PDF/Android APK الإنتاجي مؤجلًا. لم تُلغَ الاختبارات من متطلبات القبول النهائية؛ **أُجِّلت فقط بناءً على توجيه المستخدم**.

## 2. المكتبة المرئية المشتركة
| Component | Geometry | Consumer | Mandatory behavior |
|---|---|---|---|
| `HEADER-01` | A4, 5–23mm y, 5mm margins | R01–R06 | الشعار الرسمي السداسي يمينًا، أسود، عنوان عربي 15pt #FFBE00؛ عنوان EN 12pt، وقت القياس الأصلي UTC يسارًا `-` إن غاب |
| `FOOTER-01` | y=194–205mm landscape أو 281–292mm portrait بحسب A4 | R01–R06 | رمادي #EBEBEB، «صفحة n من N» 14pt عريض، HazRob في اليمين |
| `TABLE-01` | dynamic rows, 2mm LR +1mm TB | All | حد أسود 0.7pt، amber header، نص >=12pt، RTL، auto-wrap وpage break |
| `CHART-02x02` | landscape: 2×2, 141.5×79.5mm, gutter 4mm | R03/R04 | كل رسم يجيب سؤالًا مستقلًا، محاور 12pt، لا نقاط وهمية |
| `CHART-BIG` | width 287mm, height depends on legends | R01/R03/R05/R06 | 3D/4D فقط بأبعاد حقيقية من المصدر؛ مساحات فارغة عند الغياب |
| `CANDIDATE-06` | landscape columns 35/58/40/29/55/70mm | R03/R04؛ جزء مختار من R01 | GHS المرجعي مقيد بهوية مادة مؤكدة في المصدر، CAS/UID كاملان، tier، ppm كما قدمه المحرك، mini-graph بلا حساب PDF |
| `LIMIT-REFERENCE` | repeated full-width records | R01/R03/R04/R02 where applicable | السلطة، القانون/الجهة، النوع، الفترة والوحدة والصيغة والمصدر/الإصدار؛ لا حكم تجاوز جديد |
| `PHYSICAL-15` | 3 columns × 5 groups or dynamically split | R02/R03/R04 | 15 مؤشراً كاملًا، الوحدة وsource-status؛ missing= `-`، لا WBGT/noise-derived false indicators |
| `PM-3` | independent PM1/2.5/10 traces | R02/R03/R04 | قياسات أصلية فقط وبفترة مكتوبة؛ لا extrapolated time series |
| `PROVENANCE` | full-width repeating ledger | R03/R04 | status, measurement ID, source timestamps, SHA-256 كاملة، جميع المحاولات مع فصل المعتمدة |
| `FOOD-MATRIX-18` | 2 portrait matrix pages target, 9 fruit families/page | R05 | صور 18 صنف/خليط مرجعية موثقة، نتائج الفئة المختارة فقط؛ بقية `-` |
| `BEEF-RESEARCH` | portrait multipage | R06 | 7 MQ، 4 classes، 3 distinct chart regions، TS5 4×4، دراسة 36h منفصلة عن 180s Tank |
| `EVIDENCE-BADGE` | neutral text, no colored status badge in PDF when unknown | R01–R06 | وسم `OBSERVED / DERIVED / REFERENCE / RESEARCH / UNAVAILABLE / QUALIFIED` لا يُغيّر دلالة المصدر |

**تصحيح تنسيقي:** مقاسات A4 في Android هي 595×842 أو 842×595pt تقريبًا. يقع تذييل الوضع الرأسي عند 281–292mm (هوامش 5mm؛ y الحسابية من أعلى 297mm)، ولا يعتمد التنفيذ على نقل إحداثيات الصفحة الأفقية حرفيًا. يعتمد دائمًا `V7ReportLayoutKit` على قياس الصفحة والنص الحقيقي.

## 3. تسلسل محتوى كل تقرير
| Report | User question | ثابت في التصميم | صفحات متابعة عند الحاجة | الفاصل العلمي |
|---|---|---|---|---|
| R01 Instant gas | ما الذي أظهرته **اللقطة الآنية**؟ | الكاشف/الوقت، قراءة MQ، `R0/Rs`، الأدلة، المرشحون، معايرة/حدود المرجع، radar/chemcore | جميع المرشحين وسجل التقنيات ذات مخرجات فعلية | لا ppm معتمد أو هوية يقينية من نمط غير مؤهل |
| R02 Physical | ما المؤشرات الفيزيائية المسجلة؟ | الـ15، Gauges وصفية/معتمدة من المصدر، جداول التراكيز الفيزيائية، التحذيرات المحسوبة خارجيًا | بيانات سلسلة حقيقية فقط والمراجع الصحيحة | لا حساب تجاوز أو اشتقاق ارتفاع/ضوضاء/WBGT مستقل |
| R03 Field session | ماذا حدث داخل **الجلسة المعتمدة** ودوراتها G/F/P؟ | التنفيذي، جميع المرشحين، 5 معارض مخططات + 3D/4D، الفيزيائي/PM، مراجع، تدقيق SHA | كل سجل وكل مرجع ومخطط فعلي من الأرشيف | مصدر الجلسة المعتمد حصريًا؛ لا snapshot حي |
| R04 Site | كيف تتغير النتائج **عبر الجلسات المستقلة للموقع**؟ | المقارنة، 144، جميع المواد، المرشحون/المراجع، G/F/P، دورات وتدقيق | كل جلسة ومادة ومرجع ودورة عبر صفحات | لا خلط ppm بين أزمنة أو مواقع ولا top-12 |
| R05 Fruit | إلى أي صنف/خليط ترتبط بصمة الفاكهة البحثية؟ | 18 family photos في صفحتين أساسيتين، نتائج المحدد فقط، مخططات بصمة ودراسة D1–D5 | كل المرجعيات والمراتب بلا قص | مستقل عن ChemCore الغاز، غير حاسم للسلامة الغذائية |
| R06 Beef | ما التطابق الوصفي مع أصناف الدراسة المرجعية؟ | 7 MQ، 4 class labels، fingerprint/180s/similarity، TS5 4×4، 36h وDOI | مصفوفات ونص مصدر أطول عند الحاجة | لا TVC فعلي، ولا اعتبار 36h مساوية لـTank 180s |

## 4. المخططات الدقيقة، لتجنب التكرار
- **R01:** fingerprint MQ، ratio/calibration evidence matrix، radar متعدد القنوات، ترتيب المرشحين. تُعرض العلاقات المتعددة داخل الرسم فقط عند وجود المصدر.
- **R02:** Gauge للمؤشر، time-series إن وُجدت، توزيع إحصائي فقط إن وردت مخرجاته، مرجع/حد متوافق من المحرك.
- **R03:** أربع لوحات 2×2 في كل معرض (R0/Rs، تعويض، مصدر/Observed، evidence، ranking، Pearson/kinetics، ChemCore)، صفحة كبيرة لـ3D/4D، PM مستقلة؛ جميعها مواضع محفوظة دون اختلاق.
- **R04:** Session timeline، session×family matrix، session outcomes، session×evidence، مصفوفة 144، candidate recurrence، physical×session، PM site history. **لا يُكرر رسم ترتيب المرشحين نفسه بأشكال مختلفة دون معنى إضافي**.
- **R05:** مختار family→MQ fingerprint؛ D1–D5 source pattern؛ صور العائلات المرجعية ليست صور عينات مفحوصة.
- **R06:** source/observed fingerprint (مصدر مختلف عن 180s raw)، Tank 180s signal (فارغ إن كانت السلسلة غير متاحة)، four-class similarity؛ 4×4 مصفوفة مصدر الدراسة لا نتائج Tank.

## 5. حالات التصميم والاحتياجات المفتوحة
| Report | Design-specific status at this checkpoint | Outstanding design requirement | Do not conflate with |
|---|---|---|---|
| R01 | Android report prototype exists, earlier QA on previous commits | تجميد ترتيب مخططات جميع المرشحين وضبط المساحات بحسب النص الكامل | Production release |
| R02 | 15-variable native implementation exists | تحديد عدد Gauges في الصفحة والتعامل مع المصادر الطويلة دون تصغير | Calibration validation |
| R03 | 11-page blank native layout and reference bridge exist | إنهاء توزيع الرسوم النهائية وعناوينها، ضبط كثافة جداول المرشحين | Actual field data bound |
| R04 | **detailed design spec added now** | اعتماد التوزيع النهائي للمصفوفات/المقارنات والملحقات بصريًا | Site PDF writer integrated |
| R05 | 18 family layout candidate exists | **18 licensed accurate photos**, including composite mixture pictures; photo identity/source list | Photo of Tank-inspected sample |
| R06 | native writer for 3 study sections exists | مراجعة فصل reference confusion/36h عن قياسات 180s في البنية البصرية | Food-safety validation |

## 6. أوامر الاستئناف
- **الآن:** أكمل فقط التصميم والتوثيق ومراجعة تخطيط الصفحات والنصوص المرجعية. **لا تُشغّل اختبارات** حتى يرفع المستخدم هذا الإيقاف.
- **لاحقًا بإذن مستقل:** رجوع إلى اختبارات Android الأصلية، التحقق من تسلسل المصدر، تعبئة الرسومات الفعلية، ثم توصيل R03/R04/R05 الإنتاجي وإصدار APK.
- تُحدَّث `V7-REPORTS-EXECUTION-TRACKER.json` بعد كل تغيير تصميم مع روابط commit. لا يُشغّل مسار QA لتوثيق التغييرات وحده.
