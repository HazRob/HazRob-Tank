# HazRob Tank V7 — حزمة المواصفات المرئية الموحدة لجميع تقارير PDF
**Document ID:** HZT-V7-VISUAL-FREEZE-CANDIDATE-001  
**Date:** 2026-10-31 | **Branch:** `feature/tank-v7-reports`  
**Stage:** `R01_R06_VISUAL_SOURCE_DESIGN_IN_PROGRESS / TESTS_PAUSED / APK_NOT_AUTHORIZED`  
**Source of truth:** G01–G18 from `HZT-V7-RPT-STD-001 rev1.2`, report-specific owner decisions, `V7-R01-R06-UNIFIED-REPORT-VISUAL-DESIGN-MAP-2026-10-31_AR.md`.

## 1. التعليمات الحاكمة
- **تعليمات المالك:** استكمال التصميم المرئي لجميع التقارير R01–R06؛ عدم إصدار APK إلا بأمر لاحق صريح، وإيقاف الاختبارات في هذه المرحلة.
- لا تغيير في firmware أو chemCore أو خوارزميات الأغذية أو بيانات القياسات؛ لا إدخال قيم أو منحنيات تخمينية.
- تستمر مسارات الإنتاج `TankInstantReportPdf` و`TankPhysicalSnapshotPdf` و`SmartFieldSessionPdfWriter` و`SiteSummaryReportExporter` و`FoodFreshnessPdf` و`BeefResearchPdf` دون أي تبديل سببه قوالب التصميم الجديدة.
- المعاينات التي ستصدر من `writeBlankDesign` هي منشآت **Android PdfDocument** حقيقية عند تشغيلها مستقبلًا، لكنها **لم تُشغّل/تُراجع** الآن وليست تقارير تحليلية ولا مخرجات جهاز حقيقي.
- منع سريان `on: push` الاختباري لوركفلو `v7-r01-android-pdf-qa.yml` أثناء مرحلة التصميم. التحفيز اليدوي `workflow_dispatch` متاح فقط بعد إعادة السماح بالاختبارات.

## 2. نظام الصفحة الثابت
| Component | Portrait A4 | Landscape A4 | Color / type |
|---|---|---|---|
| Body | 210×297mm | 297×210mm | White `#FFFFFF` |
| Outer margin | 5mm | 5mm | No content outside |
| Black header | x=5..205; y=5..23mm | x=5..292; y=5..23mm | Official hexagonal HazRob mark at right; Arabic 15pt amber `#FFBE00`, English 12pt; original capture time on left |
| Content | y=27..277mm nominal | y=27..190mm nominal | all body text >=12pt |
| Footer | y≈281..292mm | y=194..205mm | `#EBEBEB`; `صفحة n من N` 14pt Bold black on left; identity on right |
| Tables | Flexible multiline height | Flexible multiline height | 2mm horizontal, 1mm vertical padding; `#FFBE00` black-bold heading; 0.7pt black outline; RTL-aware |
| Captions/axes | >=12pt | >=12pt | Not clipped, not collapsed to unreadable sizes |
| Missing data | `-` | `-` | Never 0, false SAFE, false absence-of-gas |
| GHS/OEL | Source-reference-only labeling | Source-reference-only labeling | No TWA/STEL comparison or occupational compliance computed in PDF |
| Real graphs | Only from original source/engine channels | Only from original source/engine channels | When source absent: axes, title, unit and blank grid; NO invented numbers/curves/colors |

## 3. ملفات Java المرئية حسب التقرير

### R01 — القياس الغازي الآني
- **الموجود من قبل:** `TankInstantV7PdfWriter.java`؛ صفحة قراءة MQ مع `Rs/R0`، مصفوفة أدلة، Radar متعدد القنوات من المصدر، الحالة والمرجع، صفحات source hypotheses المتتابعة.
- **الجديد في حزمة التصميم:** `V7InstantVisualAnnexWriter.java`؛ ملحق Portrait من صفحتين بصريتين: الأولى سجل مرشحين مع CAS/UID/طبقة الدليل/رمز GHS المرجعي ومخطط مصغر فارغ؛ الثانية مراجع OSHA/NIOSH/ACGIH متعددة بمعلومات القانون/المدة/الوحدة/المصدر ومخطط مقارنة فارغ.
- **الامتداد الإنتاجي اللاحق:** الصفوف تتكرر بلا `top-N`; لا يعتمد مولّد R01 الجديد على تحويل رتبة المرشح إلى هوية مؤكدة؛ تتوافق حالة PPM مع محرك ChemCore فقط.
- **الحالة:** مواصفة مرئية ومكوّن additive، لا وصل للملحق بإنتاج R01 أو QA جديد.

### R02 — القياسات الفيزيائية
- **الموجود من قبل:** `V7PhysicalReportPdfWriter.java` لجميع مؤشرات PhysicalCatalog الخمسة عشر، قيم ووحدات، بيانات مشتقة *فقط من المحرك*، إحصاء/مخططات محجوزة دون سلسلة مُصطنعة.
- **الجديد:** `V7PhysicalGaugeVisualWriter.java`؛ 15 Gauge موزعة تلقائيًا بمعدل أربعة مواضع/صفحة Portrait (آخر صفحة قد تحتوي أقل). كل بطاقة اسم عربي/إنجليزي ووحدة ومصدر وموضع عداد نصف دائري رمادي بلا إبرة أو حدود خطر أو خطوط قراءة لا وجود لها.
- **عند الدمج:** يمكن جعل البطاقة Gauge وصفية من قيمة أصلية، لكن تقسيم حدود المناطق/الألوان مرهون بحالة واضحة من محرك مختص ووحدة وفترة مقارنة حقيقية.
- **الحالة:** اكتمال هيكل العرض والعدادات المرئي على مستوى الشيفرة فقط؛ لا تشغيل QA.

### R03 — الجلسة الميدانية المعتمدة
- **الموجود من قبل:** `V7FieldSessionPdfWriter.java` مع تصميم A4 أفقي 11 صفحة تقريبًا عندما تكون جميع الرسومات فارغة، ملخص أرشيفي، مرشحون، GHS/OEL مستقلان، معارض 2×2، 3D/4D + T/RH، الفيزيائي والـPM والدورات وSHA كاملة.
- **أهم تفاصيل الفنون:** معاينة شبكة الأربعة 141.5×79.5mm وفاصل 4mm؛ فصل صفحه التعويض البصري عن البصمات؛ جدول المرشحين 6 أعمدة RTL `35/58/40/29/55/70mm`; صفحة مراجع لكل سلطة/نوع/مدة؛ ألوان SOURCE/RESEARCH واضحة دون ادعاء رصد.
- **الحالة:** مولّد تصميم أصلي قائم؛ لا يُبدَّل `SmartFieldSessionPdfWriter` حتى تكتمل عقود الرسم من السجلات المعتمدة لاحقًا. تحوّل التاريخ في رأس نموذج V7 إلى `sourceCaptureStartedUtc` المؤرشف، لا وقت الطباعة. تغييرات مصدرية حديثة لم تُختبر بأمر المالك.

### R04 — التقرير التجميعي للموقع
- **الجديد:** `V7SiteVisualPdfWriter.java`، تصميم أصلي باستخدام Android PdfDocument، باثني عشر فصلاً مستقلًا و12 صفحة حجز حاليًا **وليست سقفًا لعدد صفحات التقرير النهائي**.
- **S01–S12:** تنفيذي الموقع، جلسات الموقع كلها، أربعة مخططات مقارنة، شبكة 144 (12×12)، فهرس المواد الكامل، جميع المرشحين مع GHS/OEL/PPM mini, ملحق جميع المراجع، 15 مؤشراً فيزيائياً، PM1/PM2.5/PM10، التكرار والتغير، جميع محاولات الدورات G/F/P مع SHA-256 كاملة، حدود الاستنتاجات/المراجع.
- **قواعد مميزة:** لا `top-12` أو `top-15` كحد عرض نهائي، لا اختصار UUID/SHA، لا PPM متوسط اعتباطي بين جلسات مستقلة، لا زمن جلسة إداري بديل عن بداية ونهاية القياس المؤرشف.
- **الحالة:** ترتيب هندسي مُقترح موثق وقابل لرسم Android، غير موصول بـ `SiteSummaryReportExporter.writePdf`. تفصيل ترتيب الصفحات لم يحصل على اعتماد بصري نهائي.

### R05 — نضارة الفواكه والمخاليط
- **الموجود من قبل:** `V7FruitReportPdfWriter.java`، صفحتان رأسيتان أساسيتان لعائلات الفواكه الـ18 (9/صفحة) وملحق كامل بالمراتب المصدرية، Chart MQ fingerprint وD1–D5 خاليان دون معطيات.
- **تصحيح جوهري مرئي الآن:** عمود صورة الصنف **انتقل إلى العمود الأول يمينًا** (RTL) على امتداد جميع الصفوف؛ بقية الأعمدة تسير من اليمين لليسار مع الحفاظ على المجموع `200mm`.
- **تحسين:** عرض `captureTimestampUtc` من سجل القياس الحقيقي عند تمريره لاحقًا للكاتب بواسطة overload؛ الاستدعاء القديم يبقى متوافقًا ويستخدم `-` عند غيابه، ولا يستبدل زمن التصدير بزمن قياس وهمي.
- **حزمة الصور:** `V7-R05-PHOTO-ASSET-CATALOG-2026-10-31.json` تحتوي 18 اسم ملف ومسارًا وتخطيطًا للمخاليط، مع التزام أن يكون كل مصدر صورة حقيقية بحقوق نشر/ترخيص واضحة، ومرجعها وSHA عند قبولها.
- **الاحتياج المعلَّق:** الصور الـ18 **غير مرفقة بعد**؛ لم أختلق صورًا أو أضع صورًا بلا توثيق حقوق. تبقى `FoodFreshnessPdf.save` غير موصولة بالكاتب الجديد.

### R06 — نضارة اللحوم البقرية البحثية
- **الموجود من قبل:** `V7BeefResearchPdfWriter.java`، 3 صفحات Portrait مرنة كمبدأ، 7 حساسات MQ، أربع فئات مرجعية، ثلاثة مخططات مختلفة، مصفوفة التحقق البحثية TS5 ذات 4×4، 36 ساعة مصدر الدراسة منفصلة عن 180 ثانية الروبوت وDOI الصحيح في العرض.
- **تحسين:** overload تاريخ الالتقاط الأصلي `captureTimestampUtc` ليظهر في الترويسة على صفحات التقرير، و`-` حين لا يرسله مسار المصدر؛ لا استبدال تلقائي بوقت التصدير.
- **ضوابط:** `excellent/good/acceptable/spoiled` فئات من دراسة مرجعية، لا حكم سلامة غذائية/عدد ميكروبي TVC فعلي لعينة دبابة.
- **الحالة:** الكاتب المرئي الإنتاجي موجود من المرحلة السابقة؛ تغيير التاريخ لم يُختبر بعد.

## 4. مصفوفة فصل المخططات (لا تكرار دون سؤال مستقل)
| Report | Plot | السؤال العلمي | Source fields later | Null presentation |
|---|---|---|---|---|
| R01 | MQ fingerprint/Rs:R0 | شكل استجابة القنوات الفعلية | raw+engine MQ | blank |
| R01 | Evidence matrix & radar | كم قناة تمتلك معلومة مفسرة؟ | independent engine evidence | `-`/blank |
| R01 | Candidate/OEL mini | ما المرجع المصاحب لكل فرضية؟ | candidate + qualified refs | blank |
| R02 | Neutral gauge | كم القيمة المرصودة وما حالة صلاحيتها؟ | `PhysicalCatalog` + engine state | empty arc |
| R02 | Trend or distribution | ما تغير القيمة خلال فترة مسجلة؟ | timestamped original series only | empty axes |
| R03 | Cycle R0/Rs vs time | كيف تغيرت المقاومة في دورات مؤرشفة؟ | original approved records | empty axes |
| R03 | 3D/4D + compensation | أثر الأبعاد الأخرى والتعويض على بصمة المصدر | actual engine structures | empty axes |
| R03 | G/F/P evidence | هل توجد بيانات فعلية لكل فئة مختارة؟ | archived FieldCycle selections | `-` |
| R04 | Session timeline | متى وقعت جلسات الموقع فعليًا؟ | archived capture timestamps | empty |
| R04 | 144 material recurrence | ما المواد التي تكررت أدلتها عبر الجلسات؟ | qualified recurrence source | 144 `-` cells |
| R04 | Per-session physical/PM | هل تغير المؤشر بين جلسات مستقلة؟ | source-aligned G/F/P data | empty |
| R05 | 8-channel fingerprint | إلى أي مرجع غذائي وصفي تشبه العينة؟ | independent food engine | blank |
| R05 | D1–D5 source | ماذا يقول مرجع أيام الدراسة فقط؟ | original reference bank | blank |
| R06 | 7-MQ shape | ما شكل البصمة مقابل مرجع الدراسة؟ | actual source + sample channels | blank |
| R06 | 180-second trace | كيف تغيرت الحساسات خلال القياس الفعلي؟ | real time series not current API | blank |
| R06 | Four-class similarity | ما ترتيب التشابه الوصفي المحسوب؟ | BeefResearchEngine.Result | `-` |
| R06 | 4×4 source confusion | ما أدلة تحقق الدراسة المنشورة؟ | cited static TS5 data | source study label, not field validation |

## 5. حدود هذا الإنجاز
- تم **تطوير التصميم في الملفات المصدرية** وليس إخراج ملفات PDF أو PNG فعلية جديدة. لا ينبغي اعتبارها «معاينات معتمدة بصريًا» حتى يستأنف المستخدم الاختبارات.
- **لم ينتج APK V7**، ولم يتغير إصدار Firmware أو ChemCore، ولم يُحوّل مولّد R04 الجديد إلى إنتاج.
- صور R05 الـ18 لم تُحصل/تُرخّص وتُحزّم. تظل فجوة مرئية مانعة لإغلاق صورة R05 النهائية.
- لم يُشغَّل أي Android test/Gradle/CI يدوي في هذه المرحلة؛ تم تعطيل التشغيل التلقائي لوركفلو V7 R01–R06 المرئي.
- تحرير هذه المواصفة لا يعني تصديق نتائج G3 أو إنشاء فاعلية ميدانية للتعرف على الغازات أو سلامة الغذاء.

## 6. استئناف العمل لاحقًا
استمرار الترتيب: `VISUAL DESIGN SOURCE -> AUTHENTIC 18 PHOTOS WITH LICENSES -> OWNER VISUAL SIGNOFF -> [OWNER REENABLES QA] -> ANDROID PDF RENDERS -> SOURCE BINDING -> RELEASE AUTHORIZATION -> APK`.  
**حتى أمر جديد:** لا APK، لا Gradle، لا QA، لا تحديث في قائمة الالتزام العلمي سوى التوثيق البصري.
