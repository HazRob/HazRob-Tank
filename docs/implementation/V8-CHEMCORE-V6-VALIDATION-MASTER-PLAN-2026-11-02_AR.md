# HazRob Tank V8 — الخطة العلمية لتقييم وتطوير ChemCore V6
تاريخ القرار: 2026-11-02 (Asia/Aden)
القرار: APPROVED_BY_OWNER — RETAIN_AND_IMPROVE_CHEMCORE_V6
الفرع التنفيذي الوحيد: feature/tank-v8-implementation
مصدر التطبيق المرجعي دون تغيير: feature/tank-v7-reports / b344b8ac1c651794fb600baae029efd0f2522ce8
سجل الإنجاز: docs/implementation/V8-IMPLEMENTATION-TRACKER-2026-11-02.json
حالة الخطة عند التأسيس: PLAN_AUTHORIZED, SCIENTIFIC_AUDIT_NOT_YET_EXECUTED, NO_CODE_MODIFICATIONS, NO_APK, NO_HARDWARE_TEST.

## 0 — قرار الاتجاه العلمي
1. **استمرار ChemCore V6 الجديد، وعدم استبداله بمحرك V5 أو HR069.** يحتفظ بالمصدر، والتقنيات، والنتائج غير الناجحة، والأدلة المجمعة. لا تنقل أي خوارزمية سابقة إلى التشغيل دون تبرير واختبار مستقل، ويمكن استخدام المحرك السابق «خط أساس للمقارنة» لا بديلاً.
2. إصدار **V8 هو التطبيق الحاضن** لتحسينات محرك ChemCore V6؛ تسمية المحرك الأساسية V6 لا تتغير من تلقاء نفسها عند تعديل التطبيق. توثّق version وrevision لكل نسخة تحليلية داخل V8.
3. تشخيص مشكلة ثبات مرشحي V7 ثبت برمجيًا، لكنه لا يثبت فشل محرك ChemCore V6: قائمة Android بُنيت من ترتيب أول ظهور لمواد منحنيات المصدر وليس من ترتيب المعادلات؛ فرز Engine الداخلي أيضًا يقدم اتساع التغطية الحسية على RMSE. لا نعالج العرض فقط دون اختبار القاعدة التحليلية.
4. تسجيل القياسات/حفظ الجلسات، إظهار العدادات، Echo، جدول المواد، Stable، والتقارير تبقى مسارات تطبيق V8 الأصلية (R02,R03,R05,R06,R07,R08) بعد تطويعها لناتج ChemCore V6 المطور.
5. **التمييز بين G1 (صحة حسابية) وG2 (بيانات خارجية ذات مصادر وتقسيم صحيح) وG3 (تحقق ميداني على Tank)**: نجاح G1/G2 لا يبرر هوية غاز مؤكدة ولا ppm أو قرار OSHA/NIOSH من مصفوفة MQ وحدها. سياسة UNKNOWN / abstain مطلوبة، وتبقى G3=0/80 حتى تجارب مؤهلة مستقلة.

## 1 — سجل الأدلة المتحقق منها قبل اعتماد المسار
- V7 عند b344b8ac: TankInstantAnalyzer→TankV6TelemetryAdapter→ChemCoreTankV6 وEngine؛ source_curve_hypotheses→LinkedHashMap→rank=distinct.size()+1؛ لا يستهلك ترتيب Engine.Result.rankedCandidates في قائمة المستخدم. المرجع: android/app/src/main/java/com/servoc/app/chemcore/TankInstantAnalyzer.java.
- اختبارات V7 التاريخية (غير مفعلة كـ CI في V8 بعد إعادة التصفير): Actions 41171598687 (5 ملفات نسب اصطناعية)، Actions 41173692574 (10 متجهات متباعدة). تؤكد عدم تغير ترتيب القائمة الظاهرة حين تُعرض فرضيات المصدر الـ13، بينما تغيرت بواقيها وبعض ترتيبات الحساب.
- مستودع MQ Data Science / MQSpaceData: الشيفرة المسجلة في Git history عند db0efc3e4c6736c1983fc82b5304095ebd44737f ضمن research/chemcore_v7/mqspacedata_v7_upstream: 3DSurface.py و4DCurve.py وMQInfo.py وPredictData.py وreadppm.py وLICENSE.upstream (MIT). دليل 3D يذكر surfaces لحرارة ورطوبة ونسبة حساس ومعادلات غاز مصدرية. دليل 4D يذكر توقعات زمنية لحقول Time,Per,Temp,Rh واختيار أفضل نموذج من 76 نماذج. يجب تدقيق المصدر والترخيص وإعادة اختبار الأرقام قبل نقل أي معادلة أو تدريب إلى Android.
- القياسات التي أنتجها المستخدم للهواء المرجعي ومواد مختلفة لا تتضمن في سجل التجارب الحالي سلسلة خام كاملة موحدة التوقيت مع جهاز قياس مرجعي؛ لا نستنتج منها نسبة دقة غاز.

## 2 — مراحل التطوير العلمية ذات أولوية

### C6-00 — جرد تقنيات ChemCore V6 واختبار التوصيل الفعلي
- استخراج قائمة وحدات com.hazrob.tank.chemcore.v6 ومصفوفة البرنامج الـ80؛ تصنيف كل تقنية: ABSENT, IMPLEMENTED_NOT_CALLED, CALLED_UNVALIDATED, G1_PASS, G2_PASS, G3_PASS.
- تتبع كل تقنية Input→Core→Coordinator→Android→Candidate→Report، والـsource_id والدليل المستعمل؛ عدم مساواة «class موجودة» مع «وظيفة فعّالة».
- قياس التغطية الحقيقية لـ fingerprint models، منحنيات مصدر، تنعيم/إحصاء، تصنيف، mixtures, temporal, open-set, reliability/uncertainty، نقاط تصحيح الحرارة والرطوبة، duplicate sensor evidence.
- تسليم: research/v8_implementation/results/chemcore_v6_technique_activation_inventory.csv + wiring_graph.json.

### C6-01 — فحص مصدر MQ Data Science ومصفوفة الفجوات
- تثبيت upstream origin/revision/license وhash لكل ملف. مناقشة اختلاف sensor percentage (Per) وRs/R0 ووحدات ppm والوظائف العكسية: يمنع التحويل دون معايرة أو معادلة مثبتة.
- جرد التقنيات: 3D Rs/R0/Per, temperature, RH, source-gas equation surfaces; 4D time-series predictive modelling, 76 candidate statistical models, model selection, temperature/humidity correction, extrapolation clamp, report/visualization helpers, source gas coverage، ونوع cross-validation.
- جدول مطابقة «الفكرة، المعادلة، المدخلات، المخرجات، المصدر، موجود في V6؟، اختلاف طريقة التمثيل، حد القياس، اختبارات وحدات، فائدة متوقعة، كلفة CPU/RAM، G2/G3».
- إجراء اختبارات تحكم: بلا تسرب من المستقبل، TimeSeriesSplit مع purge عند الحاجة، حجز days/devices/exposures، مقارنة بموديل بسيط last-value/linear، تجنب اختيار best-of-76 من مجموعة الاختبار نفسها.
- 3D سطح تصوري من معادلات الشركة لا يمثل تدريبًا على جهاز Tank، و4D forecasting لا يمثل استدلال هوية الغاز تلقائيًا.
- تسليم: mq_data_science_gap_matrix.csv + mq_data_science_equation_audit.json + original_source_manifest.json.

### C6-02 — إثبات صحة النواة الرياضية G1
- اختبار جميع معادلات المنحنيات على حدود المجال، NaN/صفر/سالب، الوحدات، Rs/R0 inversion، R0 validity، التحويل log10(ppm)، حرارة/RH، حساسات MQ7/MQ9 heater cycle، الحساسات الغائبة والتعارضات.
- Run deterministic differential tests بين SourceCatalog ومرجع المعادلات الأصلية مع tolerances موثقة وعدم استبدال المخفقين بـ sentinel 0.
- اختبار نماذج المقارنة متعددة الحساسات وعدم مضاعفة نفس الحسّاس الفيزيائي في دمج المصادر؛ توثيق residual وsensor coverage كل على حدة.
- بوابة مخرجات: لا تنبؤ كيميائي قاطع دون sufficient independent evidence؛ تشغيل UNKNOWN/OOD gate على الهواء المرجعي والاستجابات غير الممثلة.
- تسليم: g1_numerical_accuracy.json وfailed_cases.json.

### C6-03 — تقييم موضوعي G2 واختبارات التعميم
- استرجاع جميع datasets ذات أصالة مثبتة والبيانات الخام إن توفرت: أبحاث MQ، UCI إن كانت تربط نفس مسألة المقارنة؛ فصل عينات source/virtual vs physically observed. UCI غير المماثل لمصفوفة Tank ليس مرجعًا لإثبات دقة Tank.
- بروفايلات اختبار: هواء مرجعي، إثانول، LPG، غاز ولاعة، CH4، CO، مركبات غير ممثلة، مخاليط، R0 drift، humidity/temperature shifts، missing sensors، exposure recovery؛ لا تنسب أسماء غاز لعينات اصطناعية.
- Grouped train/validation/untouched test split على مستوى session/exposure/day/device/batch، وليس تقسيم صفوف نفس التعرض عشوائيًا. منع leakage من نفس الفرد/الدورة.
- مقاييس: Top1,Top3,macro-F1, balanced accuracy, confusion matrix, false positive on clean air, UNKNOWN precision/recall, OOD/AUROC إن أمكن، calibration/reliability plot، CI by bootstrap على مستوى المجموعة، PPM error فقط إذا عُرفت التركيزات وثبتت ملاءمة المعايرة.
- قارن كل تعديل بـ baseline ChemCore V6 الأصلي وbaseline بسيط وبديل المحرك القديم **للمقارنة فقط**، واحفظ كل seed وSHA واختبار فشل.
- تسليم: g2_grouped_holdout_benchmark.json, model_comparisons.csv, failure_analysis.md.

### C6-04 — تطوير التقنيات الناقصة بناءً على الدليل
- ترتيب الفجوات بمزيج العائد الحقيقي، مخاطر overfit، جودة المرجع، الاعتمادية في Android. ابدأ بالتحسينات التي تغلق تباعد UI عن ranked outputs، جودة عينات MQ، معايرة R0، تصحيح الحرارة والرطوبة عند وجود نموذج مؤهل.
- دمج 3D/4D بصورة **وحدات علمية اختيارية منفصلة** مع unit tests؛ قد تكون مفيدة للتنبؤ بالزمن أو تفسير ظروف البيئة، وليس أساسًا وحيدًا لتحديد الغاز.
- لكل تقنية: baseline → code/tests → G1 → G2 holdout → تفعيل مشروط في التطبيق مع feature flag وprovenance؛ rollback جاهز. لا يوجد تلقيح مباشر لكل 76 نموذجًا داخل الهاتف بلا قياس الذاكرة والسرعة والقدرة.
- تسليم: technique_change_ledger.json وintegration_gate.json.

### C6-05 — إصلاح توصيل مرشحي القياس الآني في V8
- اختيار قائمة operator candidates من نتائج ChemCore V6 **المرتبة بناء على جودة أدلة حالية** بعد تصحيح منطق الترتيب والتحقق G1/G2، وليس من raw source_curve_hypotheses. تفصل المكتبة/المصادر عن الفرضيات.
- مسار واحد immutable: snapshot(UTC+monotonic,scan_cycle,quality.valid,Rs,R0,ratio,T,RH) → ChemCore V6 versioned inference → ranked candidates+residual+source+UNKNOWN reason → UI/PDF/XLSX؛ مع R0/device binding وchecksum.
- ألوان البطاقات تستمد من تصنيف جودة الدليل المعرّف ولا تعني GHS خطرًا مؤكدًا. لا تُعرض درجات posterior البحثية كنسب احتمال وجود الغاز.
- اختبارات تطابق رتب وأسماء وUID ودرجات بين الواجهة وdecision JSON وتقارير اللحظة والجلسة، وتغيرها أو قبول UNKNOWN عندما تتغير المدخلات.

### C6-06 — البوابة الميدانية G3 على Tank (لا إعلان جاهزية قبلها)
- تأهيل مصدر غاز ذو هوية/تركيز معروف مع قياس مرجعي مستقل، هواء مرجعي، معايرة متحكم بها، time series والتعافي وفترة احتراق sensors، توثيق درجة حرارة ورطوبة وheater power، إعادة التكرار على أيام ودُفعات وأجهزة مختلفة.
- بنية ملفات evidence manifest/session, calibration, raw MQ8, reference gas, sensor state, chain-of-custody, laboratory protocol, results and holdouts; قواعد قبول مسبقة ومراجعة مستقلة.
- **حتى اجتيازها يبقى البحث غير صالح لقرارات التعرض والاشتعال أو ID/ppm معتمدة**.

## 3 — أعمال التطبيق V8 المرافقة (متطلبات المستخدم محفوظة)
R02 — Field sessions: استمرار تشغيل ريلاي MQ إلى أن يكتمل تسجيل القراءة وحفظ SQLite والتحقق من دليلها؛ أسباب failure مفصلة. يسمح safety shutdown عند الفشل الكهربائي/فقد الاتصال، ولا يُحتفظ بالسخانات إلى مالانهاية.
R03 — Instant: عداد واضح لـ60s تسخين ثم 60s تسجيل فعلي، ثم flush/analysis/verify/success، مع أسباب رفض.
R05 — قائمة 734 مادة: هوية ومصادر وحدود ومخاطر وأعمدة fingerprint/curve/ppm/M4، Unknown للمفقود؛ مصدر مؤهل لكل قيمة.
R06 — كل PDF/XLSX والرسوم: نفس immutable ChemCore V6 result JSON للمرشحين دون إعادة ترتيب داخل renderer، مع اختبار فتح وعرض الصفحات.
R07 — Echo GPIO23/39 والـISR وno_falling_edge: تحقق كهربائي من 5V إلى <=3.3V، بروتوكول نبضة، اختبارات مسافات فعلية.
R08 — أحدث Stable موقّع/مؤهل ومتوافق مع V8؛ النسخة البحثية non-runtime لا تُفعّل تلقائيًا؛ fallback صالح والتحقق بالـSHA.
G09 — اختبارات Android/SQLite migration/التقارير/المكتبة/Echo وتجارب Tank قبل إصدار APK.

## 4 — ترتيب التنفيذ والمسؤولية العلمية
الدفعة الأولى (تدقيق وبدون تبديل محرك): C6-00 + C6-01 + C6-02، واستخراج baseline 5/10-case replay وتشخيص بوابة العرض.
الدفعة الثانية: C6-03 ثم تصنيف الفجوات وقبول/رفض تقنيات C6-04.
الدفعة الثالثة: C6-05 مع R02/R03/R05/R07/R08 ثم R06.
الدفعة الرابعة: C6-06 ثم G09؛ قد يُصدر build تجريبي محصور لا يدعي تحقق G3 فقط بعد اجتياز بوابة البرمجيات إذا أذن المالك.
كل خطوة تحمل: task_id, branch, source_commit, dataset_hash, inputs, expected/actual, test_cmd/CI_run, pass/fail, limitation, next_action. سجل المتابعة JSON هو مرجع الاستئناف.

## 5 — شروط عدم القفز والاستئناف
- توقف تطوير ChemCore القديم واستعادته في مسار V8. تُحتفظ بشيفراته كمرجع مقارنة منفصل فقط.
- لا تقاس دقة نظام الغاز من صور الواجهة، ولا من عدد التقنيات أو المنحنيات، ولا من تقييم Cross Validation غير مستقل أو اختيار الأفضل من الاختبار.
- تجارب 3D/4D من MQ Data Science المصدرية **غير مؤهلة بعد للـTank**؛ يلزم اختبار المعادلات والمجال ووحدات الإدخال قبل الترويج.
- بعد كل تجربة/التزام حدّث docs/implementation/V8-IMPLEMENTATION-TRACKER-2026-11-02.json قبل العمل التالي؛ إن تعطل التفاعل يُستأنف منه لا من الذاكرة.
