# HazRob Tank V8 — P11.2: تنفيذ المرجع المزدوج في ChemCore Next
**تاريخ المرحلة:** 2026-11-03  
**الفرع:** `feature/tank-v8-implementation`  
**نوع الإثبات:** G1 رياضي + تدقيق أصول المصادر + تجميع Android Java 17؛ لا تجارب Tank بغازات مرجعية ولا اعتماد G3.

## قرار التصميم المعتمد
يبقى مرجع الجهاز الأصلي `R_air=mean(Rs_clean_air)` محفوظًا دون أي تغيير أو إعادة كتابة في القيم الدائمة. **لا يُستبدل مرجع الهواء النظيف في التطبيق أو الفيرموير بمرجع منحنى خارجي.** يُشتق لكل معادلة MQSensorsLib وبصفة منفصلة مرجع بحثي:
```text
R0_source_research = R_air / K_source
q_air = Rs / R_air
q_source = K_source * q_air
ppm_source_research = A_source * (q_source ^ B_source)
```
وهو مكافئ رياضيًا لـ `A_source*(Rs/R0_source_research)^B_source`. **لا يُطبّق معامل K الخاص بـMQSensorsLib على الـ49 منحنى MQDataScience/البدائل**؛ لتلك العلاقات معادلة مختلفة `Rs/R0 = a*(ppm/scale)^b`، ومرجع R0 ومقياس مصدرها لم تتم مطابقتهما على الجهاز. تطبيق K عالميًا على جميع الأصول مرفوض.

الأصل المثبت:
- `miguel5612/MQSensorsLib`، commit `59a2e5852ec12f5da3cf508be68f5a9405314003`، مكتبة `MQUnifiedsensor` 3.0.5؛ تعريف `calibrate` يستعمل `Rs_air/K`، و`readSensor` ينفّذ في `regressionMethod=1` صيغة `ppm=A*(Rs/R0)^B`.
- 6 معادلات MQ135: blob `3ffae99864cfc51c12941647290832dc39767bb2`.
- 8 معادلات MQ-Board: blob `ce78fce7d9f2af0465924d9bd317da1f7ba486b8`. منها 7 في MQ مثبتة على Tank وواحدة MQ6 غير مثبتة.
- أصل MQ-Board يخطئ في عدة استدعاءات معايرة MQ3..MQ9 باستخدام `MQ2.calibrate`، **لم يُنقل هذا الخطأ**. مثال MQ135 يطبع `CO2+400`، بينما الحساب البحثي هنا يحتفظ بالمعادلة الخام، دون إضافة تعويض 400 ppm اعتباطيًا.

## ملفات التعديل الوظيفي
1. `android/app/src/main/java/com/hazrob/tank/chemcore/next/MQSensorsLibExampleCatalog.java`: نموذج ثابت بـ14 معادلة ومعامل `K` والسطر والملف والـblob وأصل الأدلة. 13 منها `installedModels()`؛ MQ6 مرجع مؤرشف فقط. **ليس مكتبة جديدة للمواد الكيميائية**.
2. `android/app/src/main/java/com/hazrob/tank/chemcore/next/R0ReferenceMath.java`: طريقة `researchTransform` حسابية مع صفة `SOURCE_K_RESEARCH_ONLY`؛ ترفض R_air الغائب أو غير الصحيح، وRs الصفري/NaN، والنموذج غير المطابق، والسجل القديم ذي `LEGACY_SCALED_R0`. لا تغيّر `R_air` ولا تدّعي تحقق طور السخان؛ تظل `compare` بوابة المطابقة الفيزيائية المشددة المنفصلة.
3. `android/app/src/main/java/com/servoc/app/chemcore/nextintegration/NextResearchSourceBindings.java`: ربط 13 معادلة إلى مسار `ChemCoreNext` بعد الـ49 السابقة، بصيغتها الأصلية `PPM_EQUALS_A_TIMES_RATIO_POWER_B`، مع `Qualification.unverified()` وهوية الكيميائية `null`. **62 أصلًا بحثيًا متصلًا بهدف تسجيل الاستبعاد وإظهار الفرضيات، لا 62 دليل تعرف معتمدًا**. تبقى الـ47 معادلة لحساسات غير مثبتة في سجل الأبحاث.
4. `android/app/src/main/java/com/servoc/app/chemcore/nextintegration/NextScientificDiagnostics.java`: عند القياس الآني يحسب نسب `Rs/R_air` و`Rs/R0_source_research` ونتيجة ppm المصدرية البحثية في سجل مستقل؛ يأخذ `Rs,R_air` من **اللقطة المجمّدة المستخدمة في قرار Next نفسه**، ويتحقق من `device_id` وحالة `SampleQuality.VALID` و`AIR_RS_MEAN` والريلاي؛ لا يقرأ `mq.r0` غير الموثوق في الفيرموير لتلك المعادلات. يكتب مع كل نتيجة `source_example_clean_air_K`، `source_r0_research_kohm`، `source_blob/source_line`، ورايات عدم التحقق من السخان وG3 ونطاق المصدر. ترقية schema إلى `hazrob.tank.v8.scientific_source_diagnostics/2`.
5. `android/app/src/main/java/com/servoc/app/chemcore/nextintegration/NextFieldArchiveBridge.java`: بعد أرشفة 60 ثانية أصلية وحفظ قرار Next، يحسب نفس التشخيص من وسيط المقاومة لمراحل المسح المؤهلة ومن لقطة Next المجمّدة، ويحفظه في SQLite meta بالمفتاح `v8_field_next_dual_reference_<session_id>` مع `source_archive_sha256` و`next_decision_uid`. هذه بيانات بحثية مستقلة عن قرار اعتماد الجلسة، ولا تُغيّر السجل الخام أو حالة الريلاي أو قالب PDF.
6. لا تغيير لعتبات الإنذار، أو مخطط PDF V7، أو الخوارزمية المخصصة للأغذية، أو نموذج الاعتماد، أو جداول المركبات الموحدة، أو إصدار الفيرموير، أو أي قيمة R0 دائمة.

## التحقق المنجز
- اختبار `research/chemcore_next/experiments/v8_r0_reference_transform_g1.py`: **2,925** تحققًا رياضيًا من التكافؤ بين نقل المرجع وتحويل النسبة/المعامل، باستخدام 13 معادلة وثمانية مراجع `R_air` مسجلة من Tank مع `Rs` **افتراضي**. كذلك **16** تأكيدًا على خطر إدخال النسبة المرجعية المحوّلة في بوابة الهواء النظيف. لا توجد سلسلة `Rs` لغاز معروف ضمن هذه التجربة.
- `research/chemcore_next/tests/java/DualR0SourceExampleSmoke.java`: **991** تحققًا في Java17: نماذج المصدر الـ14، وجود الثمانية الفعلية، تكافؤ النسب، عدم كتابة R_air، رفض قيم Rs الصفرية/NaN، رفض K الخاص بموديل آخر، رفض R0 القديم، وعدم السماح بإعلان G3 لمجرد وجود `labVerifiedForThisDevice=true` في الدالة البحثية.
- `research/v8_implementation/tests/test_v8_next_dual_reference_source.py`: فحوص مطابقة معاملات الـ14 مقابل سجل الـ109 المركزي، وجود الحساس، واشتراط المرجع المجمد ورايات الامتناع وسجل الجلسة.
- GitHub Actions [P11 G1 RUN 41583931823](https://github.com/HazRob/HazRob-Tank/actions/runs/41583931823): **SUCCESS** عند `fe6eeb2e6`؛ 991 Java17 + 2,925 Python + 7 اختبارات بنيوية.
- [Android compile RUN 41583899922](https://github.com/HazRob/HazRob-Tank/actions/runs/41583899922): **SUCCESS** عند `e2c66e409`؛ بناء مصدر Android Java17 مع Next وواجهة التطبيق (تجميع فقط، ليس APK ولا اختبار جهاز). تشغيل بعد تعديل الاختبار فقط لا يغير Java.
- تشغيلات F05/Android12 لعرض التقارير قد تكون مستقلة أو قيد التشغيل؛ لا يجوز مساواتها بهذا الاختبار الحسابي.

## شروط السلامة والتأهيل المتبقية
- `K` من أمثلة المصدر **ليس بالضرورة معايرة مصنعية معتمدة لجهازنا نفسه**، ولا إثباتًا لملاءمة جهد السخان أو الحرارة والرطوبة أو حمل المقاومة/ADC.
- حساسات MQ7 وMQ9 ذات دورات سخان متخصصة تظل غير مؤهلة لإخراج PPM بحثي من اللقطة إذا كانت `SampleQuality` غير صالحة. لا تجاوز لهذه البوابة بتحويل R0.
- جميع معادلات MQSensorsLib الـ13 تبقى `chemical_uid=null` في الجسر البحثي حتى لا تتحول تسمية الغاز من مثال برمجي إلى هوية مادة مؤكدة أو تنبيه تعرض/اشتعال.
- قبل إطلاق PPM تشغيلي يلزم تعيين مرجع R0 المصدر الفعلي وظروف السخان ومجال التركيز وأسلوب المعايرة، ثم اختبارات متعددة التراكيز بغازات معروفة، وحجوزات مستقلة واختبار G3 الحقيقي. `Tank G3 = 0/80` حتى الآن.
- يجب اختبار الاسترجاع الفعلي من SQLite بعد انقطاع التطبيق، واستهلاك سجل الجلسة في Excel الموسّع، وصحة شاشة الهاتف الحقيقي ضمن بوابات الإصدار اللاحقة. هذه المرحلة تربط البيانات وتوثقها، ولا تدعي أن تقرير Excel النهائي قد أعيد بناؤه الآن.

## نقطة الاستئناف
الخطوة التالية: تأكيد آخر تشغيل G1 وAndroid compile بعد آخر تعديل، ثم مراجعة قراءة أرشيف الحقول في التقارير الموسعة دون تغيير هندسة PDF، وإكمال G2/G3 الفيزيائي عند توافر قياسات الغاز المرجعي. **تجنب تكرار F01/F02 دون تغيير مرتبط.**
