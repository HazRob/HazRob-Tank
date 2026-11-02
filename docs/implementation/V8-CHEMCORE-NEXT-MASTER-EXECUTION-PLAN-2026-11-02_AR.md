# HazRob Tank V8 — الخطة التنفيذية الشاملة لـ ChemCore Next
**المرجع:** HZT8-NEXT-EXEC-001 | **التاريخ:** 2026-11-02 | **الحالة:** PLAN_DEFINED_NO_NEW_CORE_CODE | **الفرع:** feature/tank-v8-implementation
**نطاق المرحلة الحالية:** تنفيذ تدريجي واضح، بناء المحرك واختباره قبل دمجه في جميع مكونات V8. المرجعيات الدولية والتحضير للاعتماد تؤجل لمرحلة لاحقة؛ لا تؤجل سلامة الأجهزة وصحة القياس.
**السجل الملزم للحالة:** docs/implementation/V8-CHEMCORE-NEXT-EXECUTION-STATE-2026-11-02.json.
**الارتباط:** docs/implementation/V8-IMPLEMENTATION-TRACKER-2026-11-02.json؛ docs/research/CHEMCORE-NEXT-SCIENTIFIC-DISCUSSION-REGISTER-2026-11-02_AR.md (الأفكار العشرون)؛ docs/research/CHEMCORE-NEXT-TECHNIQUE-ENRICHMENT-PLAN-2026-11-02_AR.md (التقنيات الثمانون+الإضافية).
**القاعدة المرجعية:** ChemCore V6 يبقى ثابتًا للمقارنة، وV5/V7 للقراءة فقط، وكل العمل على V8. فريموير Tank وCam لا يعدّل إلا بدليل حاجة هندسية واختبار منفصل.

## 1. أولويات التنفيذ وقيود العمل
1. **المسار متسلسل:** تثبيت خط الأساس ← تأسيس نواة Next ← القياس/المعايرة ← المصادر والمركبات ← الخبراء المستقلون ← التحكيم ← الاختبارات العلمية ← تكامل تطبيق V8 ← اختبارات التطبيق ← APK.
2. **مهمة واحدة في كل مرة.** لكل مهمة مدخلات/مخرجات واختبار قبول وملف نتيجة وتبعيات وحالة وتاريخ وcommit. لا انتقال إلى مهمة لاحقة بمجرد إنشاء شيفرة دون تحقق.
3. بحث المزيد من الدراسات والأدوات يتقدم تدريجيًا ويُسجل في المكتبة؛ لا يشترط استيعاب العالم كله قبل بناء نسخة بحثية قابلة للاختبار.
4. لا ينبغي عدّ النسخة البحثية أداة تعريف نوعي أو تقدير ppm معتمد أو جهاز تحذير سلامة. G1 وG2 يمكن تنفيذهما حاسوبيًا، وG3 يتطلب Tank ومرجعًا فعليًا مناسبًا.
5. كل مصدر منحنى يحمل DOI/URL/version/sha ونوع القياس والوحدات ومدى التركيز وتعريف R0 وطور السخان وindependenceRootId. النتائج البحثية لا تتحول آليًا إلى نموذج Stable تشغيلي.
6. أي حساس غير مركب أو غير سليم لا يعطي نتيجة مصطنعة. غياب استجابة حساس متخصص قرينة سلبية مشروطة بالصلاحية وحد الكشف والظروف، لا نفي مطلق.
7. لا تعاد حسابات تقارير تاريخية بصمت عند ترقية المحرك. Next خلف feature flag، ويسجل version/model/Stable manifest مع كل قياس.
8. يستمر ريلاي الحساسات في دورة ناجحة إلى أن تنتهي العملية الفعلية وحفظ القراءة والتحقق، مع إطفاء آمن محدود عند fault/timeout أو فقد الاتصال؛ لا يُترك قيد التشغيل بلا نهاية.
9. معايرة R0 بحسب طلب المالك: **3 دقائق إحماء + 1 دقيقة جمع**. القياس الآني والجلسات: **1 دقيقة إحماء + 1 دقيقة جمع**. هذه ليست بديلًا تلقائيًا لدورة السخان الصحيحة أو الاستقرار الأولي.
10. لا تملأ قائمة المرشحين إلى عشرة بلا أدلة؛ العدد قابل لأن يكون صفرًا، مع الحفاظ على UNKNOWN، والبحث في **جميع مواد المكتبة الموحدة** مع قصر المرشحين على من لهم أدلة فعلية.

## 2. خط الأساس كما هو، لا كما نريده
- سجل برنامج V6 الثمانين: 78 تنفيذًا جزئيًا، 2 لم تنقلا (01.4 / 09.6)، G3 Tank = 0. عدد الأصول في مراجع سابقة: 49 منحنى/24 بصمة بحثية؛ لا يثبت عدد مرشحين فعالين أو هوية غاز.
- MQDataScience 7: 89 علاقة مصدر، 43 منها تخص طرازات MQ الثمانية؛ 76 نموذجًا رياضيًا للانحدار والتنبؤ لا 76 مصنف غازات.
- Android source: android/app/src/main/java/com/servoc/app/chemcore/TankInstantAnalyzer.java Git blob c3fb91f9c7a34645aaa034399e4befadcffbc169 يقيد mapping بحالة legacyId<144 ويأخذ أول ترتيب منحنيات المصدر للعرض. لا تصححه بتغيير UI فقط.
- ChemicalRepository.java يوفر unified catalog v14 وUIDs، لكن count الفعلي في قاعدة تشغيل V8 يحتاج قراءة SQLite أو اختبار، ولا تفترض 734 بدقة.
- Next سيبنى تحت android/app/src/main/java/com/hazrob/tank/chemcore/next/؛ V6 تحت .../chemcore/v6/، ومعالج الاستقبال TankV6TelemetryAdapter تحت .../tank/v6integration/.
- واجهات التطبيق بالمجلد android/app/src/main/java/com/servoc/app/ui؛ التقارير .../report؛ الجلسات .../field و.../measurement؛ الكيمياء .../chemical؛ تحديث Stable .../chemcore/library؛ أصول V6 .../assets/chemcore_v6.
- لا يوجد تنفيذ جديد للنواة أو APK V8 أو G3 في هذه الخطة عند إصدارها.

## 3. المراحل التنفيذية المنضبطة (P00→P13)

### P00 — تثبيت خط الأساس وجرد التطبيق قبل البناء
**المهام**
- P00.1 إعداد manifest مثبت للفرع والـ HEAD وSHA للمحرك والتطبيق والمكتبة والفريموير.
- P00.2 جرد 80 تقنية ملفًا ملفًا: موجودة/مستدعاة/غير مستدعاة/مخرجات/اختبارات فعلية ونواقص، بما فيها heater cycles وmotion decisions.
- P00.3 توثيق الرسم الفعلي لمرور البيانات: firmware→telemetry→R0/quality→V6→candidate→instant/session/report؛ كشف أي مسار legacy.
- P00.4 قراءة العدد الفعلي لمواد unified DB، canonical UID/CAS والربط legacy، ومصدر manifest Stable المعتمد، وأخطاء migration.
**ملفات الإثبات:** research/v8_implementation/results/next_p00_baseline.json ؛ next_p00_80_activation.csv ؛ next_p00_data_flow.md ؛ next_p00_catalog_count.json.
**القبول:** مصدر وإصدار كل مكون حرج موثق، لا تخمين عدد 734 ولا صلاحية 80 تقنية.

### P01 — تأسيس نواة مستقلة لـ ChemCore Next
- P01.1 إنشاء package next مستقل عن Android UI/SQLite، وله version وAPI ومدخلات/مخرجات موثقة.
- P01.2 تعريف SensorInstance, DeviceProfile, MeasurementSnapshot, EvidenceAsset, EvidenceNode, CandidateEvaluation, ChemCoreNextResult؛ snapshot/result immutable مع SHA ومصدر وتوقيت.
- P01.3 تسجيل الحساسات عبر capability registry قابل لإضافة مستشعرات مستقبلية دون منطق ثابت لعدد 8.
- P01.4 بناء deterministic replay + JSON schema/version migration + اختبار round-trip وincomplete inputs.
**ملفات الإثبات:** next_p01_schema.json، next_p01_replay.json، اختبارات وحدات next/.
**القبول:** نتائج replay ثابتة، الحقول الفيزيائية ووحداتها واضحة، UNKNOWN متاح دون تخمين.

### P02 — صلاحية القياس ومرجع الهواء وR0
- P02.1 محول مستقل raw ADC/ADS→voltage→Rs مع حفظ Vc/RL/نطاق ADC وحالة no sample/no power/saturation/stale.
- P02.2 الفصل بين R_air المرجعي وR0_source للمنحنيات ومعامل K ومعادلة اتجاه النسبة والتسخين؛ لا تحويل صامت لمعايرة تاريخية.
- P02.3 مصفوفة حالة كل حساس: noise, warmup, heater phase, environmental perturbation, drift, validity.
- P02.4 استخراج الاستجابة الأحادية للحساس، والدليل السلبي المؤهل، دون شرط وجود نموذجين قبل تسجيل كل الأدلة.
- P02.5 اختبارات clean air/one sensor/no R0/invalid/false positive/scan cycle duplicates.
**ملفات الإثبات:** next_p02_voltage_r0_tests.json، next_p02_clean_air_tests.json.
**القبول:** فصل الغياب الحقيقي عن fault، عدم اختلاق الغاز من الضوضاء، إظهار الاستجابة المنفردة كدليل بحثي.

### P03 — إثراء مكتبة الأدلة والمواد
- P03.1 بروتوكول بحث مصادر MQ ومشاريعها وتحليل تجربة كل مصدر، مع سجلات included/excluded ومصادر الخام والرخصة.
- P03.2 فهرس source curves/baselines: V6 49، MQDataScience 89، MQSensorsLib ودراسات أخرى؛ توحيد المعادلة واللوجاريتمات والوحدات ومنع تكرار نفس دليل المصنع.
- P03.3 فهرس البصمات الحقيقية والافتراضية والتجريبية والمخرجات 3D/4D؛ احتفظ بنوع كل مصدر.
- P03.4 ربط كل أصل بـ canonical chemical UID في جميع مواد unified catalog؛ المواد بلا أدلة تبقى ضمن البحث لا ضمن المرشحين.
- P03.5 SensorSelectivityMatrix حساس × غاز × تركيز × حرارة ورطوبة × تداخل × LOD/LOQ ومجال صلاحية، ودعم الحساسات المستقبلية.
- P03.6 استكمال خصائص جدول المركبات: النطاق الخلفي/الطبيعي حيث يوجد علميًا، حدود الكشف، حدود التعرض وأنواعها، LEL/UEL/GHS، unknown بدل قيم مختلقة.
**ملفات الإثبات:** next_p03_evidence_assets.csv، next_p03_model_mapping.csv، next_p03_sensor_selectivity.csv، next_p03_catalog_completeness.csv.
**القبول:** كل أصل له مصدر وهوية ووحدة ومجال؛ لا انتحال وجود مؤهل لغيره.

### P04 — الخبراء التحليليون المستقلون
- P04.1 CurveExpert: معادلات المصدر، fitting، inverse/forward، domain/outlier وحفظ كل فرضية لا تصنيف قطعي.
- P04.2 FingerprintExpert: similarity وأنماط الاستجابة الأحادية/الزوجية/الجماعية، ومصدر بصمة مخبرية مقابل نظرية.
- P04.3 TemporalExpert: features و4D على سلسلة زمنية حقيقية، ولا توقعات من عينة واحدة.
- P04.4 EnvironmentExpert: 3D تصحيح T/RH حين يتوفر نموذج مصدر متوافق مع فيزياء الإشارة.
- P04.5 MLResearchExpert: جرد 76 نموذجًا، تدريب وتقييم subset مفيد خارج APK أولًا، ومقارنة بخط أساس بسيط دون تسرب بيانات.
- P04.6 واجهة EvidenceNode موحدة لكل خبير، مع sourceRoot وثقة المصدر وصلاحية الاستخدام والاستثناءات، دون جمع الأرقام اعتباطًا.
**ملفات الإثبات:** next_p04_expert_regression.json، next_p04_independence.json، next_p04_model_gaps.csv.
**القبول:** لكل خبير اختبار مستقل، لا تقنيتان من أصل واحد تحسبان دليلين مستقلين.

### P05 — التحكيم والاستدلال الهرمي والتحقق العكسي
- P05.1 CandidateGenerator يعتمد جميع canonical material UIDs مع evidence eligibility.
- P05.2 منفذ دليل إيجابي وسلبي مشروط، يفرق NOT_EVALUABLE عن ACTUALLY_NEGATIVE.
- P05.3 Graph للارتباطات والمنافسة/التعزيز وعدم عدّ sensor model duplicated أو منحنى المصنع المكرر.
- P05.4 HierarchicalArbitrator: احتمال واسع → استبعاد مشروط → تعزيز → مقارنة تقاطعية → مرشح مبرر أو UNKNOWN/mixture.
- P05.5 ForwardInverseVerifier: هل المركب/الخليط يفسر القياس الحقيقي؟ سجل residual، وإذا كان نموذج الخليط غير مؤهل لا تستخدم جمعًا خطيًا مفترضًا.
- P05.6 ranked eligible candidates dynamic count 0/1/5/10/>10، max display فقط، no quota fill، no fake posterior percentage.
- P05.7 فصل نتائج الهوية/الكمية البحثية عن إشعارات السلامة المؤهلة؛ feedback المشغل لا يحذف الأصل.
**ملفات الإثبات:** next_p05_candidate_replay.json، next_p05_conflicts.json، next_p05_inverse.json.
**القبول:** قائمة غير ثابتة تتغير بتغير الأدلة، مرشح nonlegacy يظهر فقط إذا له نموذج فعلي؛ zero result علمي مقبول.

### P06 — اختبار المحرك قبل ربط Android
- P06.1 G1 اختبارات الأرقام والمعادلات والوحدات والحالات الحدية والواجهة والمصادر.
- P06.2 replay عينات V6 التاريخية وأصول MQ المنشورة والمقارنة بخط أساس/محرك V6، مع تفسير الاختلاف.
- P06.3 G2 بيانات قابلة للمقارنة بنوع مسألة الاستدلال؛ تقسيم exposure/day/device، اختبار held-out ثابت، no leakage؛ مقاييس دقة/خطأ/UNKNOWN مناسبة للبيانات.
- P06.4 قياس CPU/RAM/latency وملفات الموديلات اللازمة لأندرويد، مع failure injection.
- P06.5 حفظ model snapshot وmanifest وtest reports والإخفاقات، والتصنيف RESEARCH_ONLY لما لم يُؤهل.
**ملفات الإثبات:** next_p06_g1_report.json، next_p06_g2_report.json، next_p06_resource_profile.json، next_p06_model_manifest.json.
**بوابة توقف:** لا انتقال افتراضي إلى V8 Runtime دون نجاح G1 وتحليل G2 وحدود البيانات؛ يمكن feature flag بحثي بتصريح حالات التأهيل.

### P07 — ربط Next بالتطبيق (أول مرحلة V8 Runtime)
- P07.1 NextAndroidAdapter من raw telemetry إلى Snapshot، مواءمة R0 والجهد والوقت دون فقد.
- P07.2 SQLite schema للنتائج المحفوظة غير القابلة للتغيير، version/candidate UID/trace/source؛ ترحيل بيانات دون حذف.
- P07.3 feature flag Next/V6 + rollback ورفض البيانات غير المتوافقة، no hidden legacy reranking.
- P07.4 إثبات أن UI/PDF/XLSX لاحقًا ستقرأ نفس result ID، واختبارات startup/crash/restart/offline.
**ملفات الإثبات:** next_p07_adapter.json، next_p07_migration.json.

### P08 — القياس الآني والمرشحون
- P08.1 MeasurementsTab عداد حقيقي 60s إحماء +60s تسجيل، reject/success موثق لا مؤقت شكلي.
- P08.2 إصلاح TankInstantAnalyzer بحيث لا يعرض legacy144 ولا source_curve_hypotheses ترتيبًا نهائيًا، بل canonical ranked eligible list من Next.
- P08.3 ChemCoreAnalysisTab: تفسير المرشحين والأدلة الموجبة والسالبة ومستوى التحقق وUNKNOWN والرسوم فقط عندما تتوافر بيانات.
- P08.4 تقرير القياس الآني PDF من نتيجة واحدة موثقة، مع source evidence وreview state.
- P08.5 تتبع غاز محدد وتسجيل feedback له، ولو جاءت النتيجة عدم رصد.
**ملفات الإثبات:** next_p08_instant_flow.json، next_p08_0_to_10_candidates.json، next_p08_instant_pdf.json.

### P09 — إدارة المواقع والجلسات
- P09.1 FieldSessionsTab + FieldCycleController + FieldCycleRepository: تشغيل الريلاي المؤكد → 60+60 → جمع عينات حقيقية → flush/save/readback → إطفاء؛ منع infinite ON عند fault.
- P09.2 صلاحية العينات وتوثيق raw MQ/ADS إن كان firmware يدعمه، scan cycles، failures/retry/approval؛ لا قبول جلسة ناقصة.
- P09.3 CompletedSessionChemCoreAnalyzer Next مع تاريخ فعلي، واستدعاء temporal experts فقط عند توفر series؛ لا استدعاء قديم صامت.
- P09.4 ربط نتائج مواقع وجلسات متعددة والتتبع العام والقياس المختار بنفس canonical identity.
- P09.5 فصل حساسات PM/O2 والغرف الفيزيائية مع سياسة missing-sensor؛ لا تستخدم قياسات غير متوفرة.
**ملفات الإثبات:** next_p09_session_integrity.json، next_p09_relay_failures.json، next_p09_mode_parity.json.

### P10 — المختبر والمعايرة والتجارب
- P10.1 AnalyticalLabTab/ExperimentsTab/DeveloperExperimentsTab بروتوكول تجارب مع gas identity/reference ppm/unknown/simulation وRaw linkage.
- P10.2 DeveloperR0Tab/SettingsTab 3 دقائق إحماء +1 قياس مع حفظ R_air وR0_source وphase.
- P10.3 تجربة ومطابقة fingerprint/curve وتصحيح توقع المحرك مقابل النتائج المخبرية، ورفع مراجعة مدروسة لاحقًا.
- P10.4 تعديل مصادر التعلم دون ترقية Stable تلقائية، وسجل lab feedback قابل للمراجعة.
- P10.5 FoodFreshnessTab اللحوم/الفواكه وتقاريرها **منفصل** عن تحليل غازات ChemCore Next.
**ملفات الإثبات:** next_p10_lab_experiments.json، next_p10_r0_3plus1.json، next_p10_food_isolation.json.

### P11 — قاعدة المواد الموحدة وStable والتعلم من المستخدمين
- P11.1 chemical repository/central library/matrix/aliases/CAS/UID + قائمة مواد كاملة ومؤشرات توفر fingerprint/curve/ppm/M4، بدون نسخ متفرقة.
- P11.2 include verified Stable snapshot inside APK, version/provenance/inquiry update/rollback/offline via ChemCoreLibraryManager and UpdateClient؛ لا تنزيل APK عبر تلك الميزة.
- P11.3 UserReview على المرشحين والمتغيرات البيئية: original result immutable، reviewed result منفصل، source assertions لا تعتبر laboratory truth.
- P11.4 local FeedbackQueue + privacy/consent + central upload only after real backend/configured API؛ لا ادعاء رفع ناجح دون receipt.
- P11.5 CommunityContributionsTab: مقترحات مواد/منحنيات/بصمات ومصدر وتجربة، quarantine→scientific review→replication→candidate Stable.
**ملفات الإثبات:** next_p11_library_integrity.json، next_p11_update_flow.json، next_p11_feedback.json.

### P12 — التقارير والواجهة والاختبارات الشاملة
- P12.1 جميع PDF/XLSX instant/session/site/reports/physical/lab تأخذ نفس result، مع مصادر وتقارير الرسوم وتفاصيل العينة.
- P12.2 raw ADS مرفق في الجلسات Excel عند توفره، وN/A صريح عند غيابه.
- P12.3 ReportsTab/Charts: لا رسوم افتراضية عند بيانات مفقودة، ولا نص أبيض على خلفية بيضاء أو مربعات اختيار غير ظاهرة، ولا header مكرر.
- P12.4 ControlTab/StatusTab/SettingsTab/DeveloperTab/Diagnostics/echo/camera/drive: اختبارات عدم الانحدار، ولا تفسد وظائف الحركة.
- P12.5 Android CI, SQLite migration, low memory, rotation/crash, disconnected robot, PDF/XLSX data parity, review flags, multi session.
**ملفات الإثبات:** next_p12_end_to_end.json، next_p12_report_parity.json، next_p12_android_regression.json.

### P13 — التحقق على الجهاز وبناء نسخة APK V8
- P13.1 bench sensor/relay/ADC/ESP32 and Echo and lost link failsafe، مع تثبيت hardware profile.
- P13.2 تجارب G3 مؤهلة على Tank عندما تتوافر غازات مرجعية ومختبر وتجهيز آمن؛ إذا لم تتوافر تبقى نتائج الهوية وPPM RESEARCH_ONLY ولا ندعي نجاح G3.
- P13.3 بناء APK V8 تجريبي من GitHub Actions مع run ID وSHA وchecksums وlink artifact. Firmware ESP32/ESP32-CAM ZIP مستقل فقط إذا ظهرت حاجة مع سجل compile/flash.
- P13.4 مراجعة المستخدم للأخطاء وقياسات التوافق وتحسينها ثم build repeated؛ تسجيل نسخة Next وStable في واجهة التطبيق وREADME.
- P13.5 release decision: INTERNAL_RESEARCH أو BETA_RESEARCH، ومؤهّل ميدانيًا فقط عند الدليل الحقيقي، وحفظ rollback.
**ملفات الإثبات:** next_p13_bench.json، next_p13_g3.json، next_p13_build_manifest.json، next_p13_release_gate.json.

## 4. خريطة مكونات التطبيق من دون أي إغفال مقصود
- Core + Telemetry + Sensors + R0 + CleanAir + Experts + Graph + Stable + Catalog.
- MeasurementsTab، ChemCoreAnalysisTab، FieldSessionsTab، AnalyticalLabTab، DeveloperExperimentsTab، DeveloperR0Tab، ExperimentsTab، CentralLibraryDashboardTab، ChemicalExperimentMatrixDialog، SettingsTab، ReportsTab، FoodFreshnessTab المنفصل.
- Tracking، gas alerts، physical measurements، site/session approval، local storage, report PDF and Excel، raw ADS، GitHub update/query، feedback and contributions.
- ControlTab، StatusTab، developer diagnostics، robot network modes، camera and Echo: regression scope حتى إن لم تحتاج إعادة تطوير.
- README/VERSIONING وGitHub Actions وAPK وبنية rollback.
كل بند أعلاه يجب أن يربط في اختبارات P08–P13 بنتيجة حقيقية، لا يكفي ذكره في جدول.

## 5. نظام التتبع الإلزامي
**حالة المهمة:** NOT_STARTED / READY / IN_PROGRESS / BLOCKED / CODE_COMPLETE_UNTESTED / TEST_FAILED / TEST_PASSED / DONE / DEFERRED. مؤهل العلم G1/G2/G3 حقل منفصل عن حالة التنفيذ.
**كل مهمة تسجل:** id، phase، description، inputs، depends_on، deliverables، acceptance، state، commit_sha، test_status، action_run_id، actual_evidence_files، blockers، last_checkpoint، next_action، updated_at.
**بعد تنفيذ مهمة:** اكتب نتيجة الاختبار، ثم المستند/الملف الدال عليها، ثم commit، ثم تحديث state وtracker العام؛ لا تضف DONE إلا بنتيجة اختبار وإثبات.
**استئناف الانقطاع:** اقرأ plan ثم JSON state ثم V8 tracker، تحقق من GitHub HEAD ومن الـactual_evidence_files، استأنف active_task أو أول READY غير محجوبة؛ لا تعِد تنفيذ DONE ولا تتجاوز بوابة P06.
**نسب الإنجاز:** نسبة المهام DONE المثبتة فقط؛ لا تساوي وجود ملف برمجي جاهزية علمية.
**بداية المسار:** P00.1 تثبيت source SHA ثم P00.2؛ لا يوجد APK أو اختبار ميداني جديد ناتج عن إنشاء هذه الخطة.

## 6. وصف مختصر جاهز للاستئناف في محادثة جديدة
افتح مستودع HazRob/HazRob-Tank وفرع feature/tank-v8-implementation. اقرأ docs/implementation/V8-CHEMCORE-NEXT-MASTER-EXECUTION-PLAN-2026-11-02_AR.md وdocs/implementation/V8-CHEMCORE-NEXT-EXECUTION-STATE-2026-11-02.json ثم docs/implementation/V8-IMPLEMENTATION-TRACKER-2026-11-02.json. تحقق من HEAD ومن مخرجات evidence وActions. نفذ active_task بالترتيب، واكتب commit وtest/run evidence والتحديثات بعد كل خطوة. ChemCore V6 محفوظ، Next يُبنى ويُختبر أولاً، ثم V8. لا تفترض اكتمال G3 أو APK دون أدلة.
