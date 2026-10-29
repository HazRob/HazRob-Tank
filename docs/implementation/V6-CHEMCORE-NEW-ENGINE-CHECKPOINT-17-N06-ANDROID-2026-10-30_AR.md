# ChemCore Tank V6 — Checkpoint 17: N06 Android instant / PDF research-only integration

**التاريخ:** 2026-10-30 | **الفرع:** `feature/tank-v6-implementation`  
**سابقًا:** [Checkpoint 16 — MQ4 Java real-source validation](V6-CHEMCORE-NEW-ENGINE-CHECKPOINT-16-MQ4-JAVA-2026-10-30_AR.md).  
**الحالة العلمية لا تزال:** **78/80 PARTIAL source research, 2/80 NOT YET PORTED, Tank physical G3 0/80**.

## 1. المنفذ فعليًا في مسار القياس الآني

- أصبح `com.servoc.app.chemcore.TankInstantAnalyzer` يستدعي مباشرة `TankV6TelemetryAdapter.analyzeSnapshot(frozen)` ثم `toJson()`؛ ينتقل الطلب إلى `ChemCoreTankV6.analyze` **دون fallback** إلى محرك الغاز القديم أو بصمات الأغذية. التعديل السابق موجود أصلًا في الفرع منذ N06 الأولي؛ لم يتم الادعاء بإعادة بنائه في هذه الدفعة.
- عُدّل `android/app/src/main/java/com/hazrob/tank/v6integration/TankV6TelemetryAdapter.java`، commit `dddbed53a6cecca14abb49c450923a89ae9d7c61`، ليستبعد القنوات التي علّمها ESP32 `ok/valid=false` أو `powered=false` صراحة أو `Rs<=0`/غير محدود. يُحتفَظ بالـRs الأصلي الموجب المحدود إذا لم يتوفر R0، لكن **لا** يحسب له نسبة أو منحنى أو هوية كيميائية.
- يَتضمّن خرج JSON سجلًا مستقلًا `original_mq_channel_evidence` يجمع طراز/معرف الحساس وقيم Rs/R0 كما وردت دون افتراضات، ويصرّح `tank_g3_reference_calibration_validated=false`، و`reportable_ppm=false`.
- واجهة `MeasurementsTab.java` commit `b61ffa205558caa269b85b5a60cce8d3f2e807c8` تعرض نتائج V6 في فرع خاص قبل استدعاء `TankPpmEstimate.from` أو حساب posterior السابق. تعرض `UNKNOWN`، وتصنّف جميع منحنيات المصدر فرضيات بحثية غير مثبتة؛ لا تسجل إنذارات تعرض مهنية ولا تنشئ تلقائيًا تتبعًا لغاز من تلك الفرضيات.
- تقرير `TankInstantReportPdf.java` commit `6efce502ac8e7a8d25605408f9510797667f92ca` يحوي مسار PDF مخصصًا عندما تكون `schema=hazrob-tank-v6-android-telemetry-bridge/1`، يسرد القياسات الأصلية و`R0` غير المتوفر، و`UNKNOWN`، وفرضيات المصدر من دون نسب Posterior أو ppm معتمد أو تحذيرات تجاوز معايير العمل. مسار V5 السابق يبقى منفصلًا ولم يُحذف.

**حدود التنفيذ:** لا يجري ذلك تحويل PDF على هاتف/محاكي Android، ولم يُختبر سلوك الشاشة بالإدخال المباشر على هاتف. إنما جرى تجميع الشيفرة عبر Gradle واختبار الشيفرة الهيكلية واختبار محول Android JSON فعليًا على JDK.

## 2. أدلة CI وتجميع Android

- [Android compile-only CI 40969942752](https://github.com/HazRob/HazRob-Tank/actions/runs/40969942752) **SUCCESS**: `:app:compileDebugJavaWithJavac` على طبقة Tank فوق أصل تطبيق Android المثبّت بإصدار المصدر المحدد، ثم **سبعة** اختبارات `research/chemcore_v6/tests/test_v6_android_instant_research_contract.py` **PASS**.
- تسجيل تعديل الـCI في `.github/workflows/chemcore-v6-android-compile.yml` commit `9fe87d1d486d6e5144af611881e831a31e7e5b3e`، يشمل `TankInstantReportPdf.java` ومسارات التحقق.
- الاختبارات السبعة **تحقق بنيوي/static** لفصل المحرك/عدم الرجوع إلى القديم/بوابة صلاحية القنوات/عدم اختلاق R0/تفرع واجهة V6 قبل Posterior/تقرير PDF البحثي/بقاء G3 صفرًا. هي ليست اختبارات end-to-end لجهاز أو PDF مرئي.

## 3. تشغيل محول Android JSON على **أصل القياسات الفعلية**

- المصدر نفسه من Furuta/Minnesota، ملف `10ppm_data.csv` SHA256 `3c27c4f8b8e8aab54b6433acfa41de00890a9eefd19376259a2371f22a48ff2d`.
- مصدر قياسات **14,186 سجلًا زمنيًا × ثلاث وحدات MQ4 حقيقية = 42,558 قراءة مقاومة Ω**. محرك Tank يتضمن وحدة MQ4 واحدة، لذلك **مررنا الوحدات الثلاث بالتتابع وليس بالتزامن**؛ نُقلت وحدة Ω إلى kΩ في حدود واجهة JSON فقط دون اختراع قيم R0. لم تصل إشارة Li-Cor CH4 إلى مدخل محرك الغاز؛ بقيت مرجعًا خارجيًا مستقلًا.
- `research/chemcore_v6/tests/java/MinnesotaMq4AndroidBridgeTest.java` commit `231f54b67f0cfe198832f0591c1eef16d1d3fdc5`: شغّل `TankV6TelemetryAdapter.analyzeSnapshot` و`TankV6TelemetryAdapter.toJson` مع المنسق الجديد على أصل المدخلات؛ **638,380 تأكيدًا ناجحًا**، منها قيم فعلية ورفض قنوات invalid/unpowered/negative، مع تمرير قياس R0 صناعي خاص بالاختبار السلبي فقط للتأكد أنه لا يرقّي المخرجات إلى قياس رسمي.
- [CI 40970539124](https://github.com/HazRob/HazRob-Tank/actions/runs/40970539124) **SUCCESS** مع حزمتي المصدر و`org.json` الأصلية. نسخة مكتبة `org.json json-20240303.jar` مثبتة SHA256 `3cf6cd6892e32e2b4c1c39e0f52f5248a2f5b37646fdfbb79a66b46b618414ed`. تشغيل أول فاشل [40970372808](https://github.com/HazRob/HazRob-Tank/actions/runs/40970372808) سببه طلب ملف `.sha256` غير متاح من Maven؛ عولج باسترجاع `.sha1` ثم تحقق SHA256 مثبت قبل التشغيل، والنسخة الأولية [40970448667](https://github.com/HazRob/HazRob-Tank/actions/runs/40970448667) نجحت.
- نتائج مرور البيانات: **42,558** قراءة Rs الأصلية باقية، قناة MQ4 واحدة في كل طلب، **0** R0 مفترض، **0** curve fits أو هوية غاز عند غياب R0، **0** ppm قابل للإبلاغ أو تحقق G3. بيانات الأغذية خارج المسار. هذا **تشغيل فعلي لمحول Android JSON على JDK**، وليس تشغيل التطبيق على هاتف Android.
- Workflow المُثبّت `.github/workflows/chemcore-v6-n06a-mq4-android-json-replay.yml` commit `b734f41d7c137f2ce6cda1c5cc28f266f31471e7`.
- محفوظ JSON حاكم: `research/chemcore_v6/results/tank_v6_n06a_android_mq4_original_json_bridge_2026-10-30.json` commit `2a06726c352ac18890cf1faf8b8ab74a7b9f9488`.
- Artifact CI ID **12939574004**، ZIP SHA256 `3e1c85fb206d12f982045c2d0007ab7b1b5f038c451d30d39232d30e16f0eef2` يحتوي `v6_android_json_bridge_original_mq4_audit.csv` و`v6_adapter_mq4_manifest.json`.
- نسخة شخصية للمكتبة `/HazRob/MQ_Reference_Measurements_2026/HazRob_Tank_V6_N06A_MQ4_Android_JSON_Original_Evidence_2026-10-30.zip`، `libfile_00bbb73d77ac819184c1d2c1aacc1ce4`.

## 4. النطاقات غير المنجزة وخطة استئناف N06

| بند | الحالة | البوابة المطلوبة |
|---|---|---|
| قياس آني جديد V6-only (شيفرة) | **تم ربطه وتجميعه** | اختبار تشغيل هاتف فعلي على اتصال/انقطاع/استعادة ورصد تعدد الحساسات |
| محول Android JSON على القياسات الأصلية | **نجح في JDK** | اختبار تطابق بيانات ESP32 من الروبوت نفسه |
| شاشة V6 البحثية | **تم فرعها/تجميعها** | اختبارات UI مرئية وتشغيل اختيار المرشحين والتتبع اليدوي |
| تقرير PDF V6 الآني | **شيفرة وتجميع** | فتح PDF على هاتف، صفحات RTL وUnicode وفحصها والتحقق من التخزين والمشاركة |
| إدارة المواقع والجلسات المعتمدة | **لم يُنقل مسار ChemCore الخاص بها بعد** | adapter V6 مخصص للجلسات بمدخلات مثبتة وقراءة سجلات تاريخية؛ اختبارات SQLite migration/approval |
| تقارير الموقع/الجلسة PDF/Excel | **لم تُختبر مع V6** | تغيير تدريجي مع rollback للبيانات التاريخية وعدم fallback للاستدلال القديم داخل V6 |
| الرؤية الفيزيائية وG3 | **0/80** | R0 فعلي، غازات مرجعية، أجهزة/أيام/تراكيز مستقلة |
| إصدار V6 APK وتوقيعه | **لم يُنشأ** | اجتياز اختبارات التكامل ثم مسار بناء موقّع متوافق مع الترقيات |

لا تغيير لمحرك الغذاء المستقل أو تسخين الحساسات المؤجل أو تحكم حركة الروبوت. لا تُرفع تقنيات إلى G3 أو Stable من اختبارات JDK.

**Resume token:** `V6_CHECKPOINT17_N06A / INSTANT_V6_ONLY_ANDROID_COMPILES / PDF_V6_RESEARCH_BRANCH_COMPILES / 7_STATIC_TESTS_PASS / 14186_SOURCE_MQ4_MINUTES / 42558_PHYSICAL_RESISTANCES_REPLAYED_SEQUENTIALLY / 638380_ADAPTER_ASSERTIONS / CI_40969942752_AND_40970539124_SUCCESS / NO_G3 / 78_PARTIAL_2_UNPORTED / SESSION_EXPORT_ANDROID_DEVICE_UNTESTED / NO_V6_APK`.
