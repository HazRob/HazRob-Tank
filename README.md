# HazRob Tank

## أحدث إصدار مبني: التطبيق والفريمويران V6 (30 أكتوبر 2026)

> **تحديث موثّق: 30 أكتوبر 2026.** أحدث تطبيق Android تم بناؤه، وأحدث فيرموير للتحكم ESP32-Tank، وأحدث فيرموير للكاميرا Cam-tank في هذا المشروع هي جميعًا **الإصدار السادس V6**. المقصود أحدث **حزم تطوير مجمّعة وقابلة للاختبار**، وليس اعتماد إصدار إنتاجي أو إثبات سلامة القياسات على الروبوت الحقيقي. الفرع المعتمد للتطوير: [`feature/tank-v6-implementation`](https://github.com/HazRob/HazRob-Tank/tree/feature/tank-v6-implementation). أما `main` فهو فرع المستودع الافتراضي وليس دليلًا على دمج جميع ملفات V6 إليه.

## سجل أحدث الإصدارات / Current version register

| المنتج | آخر إصدار V6 | ملفات المصدر / الحزمة | دليل البناء |
| --- | --- | --- | --- |
| **تطبيق الدبابة Android APK** | **V6** — `versionName=6.0`, `versionCode=6`, `applicationId=com.hazrob.tank`; Android 12 | [تحميل Artifact واستخراج `HazRob-Tank-V6-Diagnostic.apk`](https://github.com/HazRob/HazRob-Tank/actions/runs/41102976044/artifacts/12999965690) | [Actions 41102976044 — SUCCESS](https://github.com/HazRob/HazRob-Tank/actions/runs/41102976044)، عند `213f3bf89288fcdde76eebdbc7ee0d2973eeb139`؛ توقيع **Debug** وليس توقيع ترقية مثبتًا |
| **فيرموير ESP32-Tank** | **V6** — `ESP32-Tank 0.1.6-v6-quickr0-dev`؛ API 4 | [حزمة ESP32-Tank V6، مشروع Arduino + BIN](https://github.com/HazRob/HazRob-Tank/actions/runs/41093126933/artifacts/12995795016)؛ افتح `ESP32-Tank/ESP32-Tank.ino` | [Actions 41093126933 — SUCCESS](https://github.com/HazRob/HazRob-Tank/actions/runs/41093126933)، تجميع Arduino-ESP32 **3.3.11** للوحة `esp32:esp32:esp32` |
| **فيرموير Cam-tank (ESP32-CAM)** | **V6** — `Cam-tank 0.1.4-v6-candidate` | [حزمة Cam-tank V6، مشروع Arduino + BIN](https://github.com/HazRob/HazRob-Tank/actions/runs/41093126933/artifacts/12994304170)؛ افتح `Cam-tank/Cam-tank.ino` | [Actions 41093126933 — SUCCESS](https://github.com/HazRob/HazRob-Tank/actions/runs/41093126933)، تجميع Arduino-ESP32 **3.3.11** للوحة `esp32:esp32:esp32cam` / AI Thinker |

**مهم لتنزيل الملفات:** ملفات GitHub Actions من نوع Artifact تصل كأرشيف ZIP. حزمة التطبيق تحتوي **ملف APK مستقل** بعد فك الضغط، وليست تطبيقًا بصيغة ZIP. حزم الفريموير تحتوي ملفات مصدر Arduino، بما فيها `.ino` و`.h` و`.cpp`، بالإضافة إلى ملفات BIN وملف معلومات البناء. يجب فتح مشروع `.ino` من مجلده داخل Arduino IDE؛ **لا تستخدم Add .ZIP Library**.

## التغييرات المنجزة في مسار V6 / V6 change log

1. **Android وواجهات المشغّل:** هوية `HazRob Tank` مستقلة عن HazRob رباعي العجلات؛ الدخول إلى التطبيق لا يشترط تساوي نص رقم نسخة Android وفيرموير ESP32. جرى تطوير واجهات القياسات، المختبر، البحث الكيميائي، حالات المواد، المواقع والجلسات والتقارير، مع بقاء اختبارات الاستخدام الفعلية منفصلة عن نجاح التجميع.
2. **ChemCore V6:** إنشاء مسار استدلال مستقل `com.hazrob.tank.chemcore.v6` بدل تحويل نتائج محرك HazRob القديم إلى نتائج مؤكدة. تستخدم البصمات ومنحنيات MQ وميزات القياس والاستدلال المرجعي لأغراض بحثية؛ **لا يثبت ذلك هوية غاز أو ppm معتمدًا**. حالات الهواء النظيف، غياب R0 الموثوق، وعيوب القياس لا تطلق إنذارات غاز وهمية. يبقى تحذير Echo وإيقاف الحركة المستقلين عن بوابة ادعاء الغازات.
3. **معايرة R0 V6 موحّدة:** **180 ثانية تسخين + نافذة قياس واحدة 60 ثانية**، مع قبول منفصل وبيانات مصدر ومعالجة NVS؛ لا يُستخدم R0 منشور من دراسة أخرى كمعايرة لروبوت الدبابة. لا توجد قيمة R0 ميدانية مصطنعة داخل نسخة V6.
4. **المكتبة الكيميائية الموحدة SQLite V14:** **734 مادة بمعرّفات UID مستقلة** (منها 725 لها CAS مباشر أو رابط مرادف موثق و9 مسميات غير مفردة)، و**49 منحنى/مرجع MQ بحثي** و**25 ملاحظة R0/Rs خارجية** محفوظة مع سياق المصدر. القيم الناقصة تبقى `NULL`، والتصنيف المرجعي GHS لا يُرقّى تلقائيًا إلى إنذار تشغيلي.
5. **Stable دون اتصال:** تضمين Stable `1.0.0-hr069r1` بــ **652 هوية جزيئية** و49 سابقة استجابة بحثية و**صفر معايرات M4 مثبتة على الدبابة**، مع فحص SHA-256 داخل بناء APK؛ طبقة Stable لا تحل محل سجل المواد الموحد ذي 734 UID.
6. **تحديث سجلات المكتبة بالاستعلام، لا تنزيل المكتبة كاملة:** `ChemicalReferenceSyncV6` يستعلم عن فهرس السجلات المعدلة حسب UID/CAS وSHA-256 ويجلب عند الحاجة السجل الصغير المتغير (حتى 25 سجلًا في الطلب)، دون استبدال Stable الكامل في عملية التحديث المعتادة. تتحدث قوائم المواد وحالات **البصمة | منحنى MQ | PPM مرجعي | M4 مرجعي** مباشرة بعد التطبيق. قبول حالة `AVAILABLE` أو `QUALIFIED` المنشورة مسؤولية المكتبة المركزية؛ الهاتف يتحقق فقط من سلامة النقل والهوية والبنية، ولا يعيد التأهيل العلمي.
7. **إصلاحات آخر اللمسات:** سحب خاصية منشورة يُزيلها من جميع القوائم؛ السجل المنشور الخالي من الخصائص يسحب **القدرات الأربع كلها**؛ وجود مصدر إضافي بحالة `AVAILABLE` لا يُنزل حالة `QUALIFIED` لمصدر آخر. أضيفت اختبارات مصدر ومحاكي لهذه الحالات. [اختبار المصدر العلمي 41102007946 — SUCCESS](https://github.com/HazRob/HazRob-Tank/actions/runs/41102007946).
8. **المواقع والجلسات ونتائج الفحص:** تدقيق ترحيل قاعدة البيانات، اعتماد الجلسات والحذف، تقارير PDF/Excel، فصل الملاحق البحثية عن النتائج المؤهلة، وتصنيف GHS. وظيفة **نضارة الأغذية (اللحوم/الفواكه)** مستقلة عن ChemCore المخصص للغازات.
9. **فيرموير ESP32-Tank:** سيرفوان مستمران للجنزير، حد مهلة أمر القيادة **500 ms**، قياسات MQ عبر ADS1115، BME على I²C مستقل، Echo للتحذير فقط، وإجراءات R0 الجديدة. **Cam-tank:** مشروع كاميرا AI-Thinker مستقل، عرض MJPEG محلي؛ معدل الإطارات والاتصال البعيد لم يُثبتا بعد على الجهاز الحقيقي.

## حالة القبول وحدود V6 / Verification and release status

- **PASS برمجي:** بناء APK تجريبي ووجود Stable المدمجة وتوقيع APK التجريبي، وتجميع الفريمويرين، واختبارات مصدر علمي محددة. أحدث APK هو **V6 diagnostic**؛ لا يساوي `production release`.
- **ليس PASS ميدانيًا:** تفليش وحدتي ESP32 وESP32-CAM على العتاد، اتجاه المحركات وإيقاف الطوارئ وسلامة الاتصال، المعايرة بالغازات المرجعية، معدل FPS الفعلي، وتأكيد دقة هوية الغاز وppm. **G3 للروبوت الحقيقي = 0/80** حسب سجل المشروع.
- **توقيع Android:** شهادة V5 المثبتة لم يثبت تطابقها مع مفتاح APK التجريبي V6؛ قد يرفض Android التحديث المباشر. **لا تحذف التطبيق السابق قبل حفظ بياناته وجلساته**.
- **قناة نشر تحديثات المواد:** الفهرس `updates/chemcore-records/v6/index.json` الموجود بفرع التطوير بدأ فارغًا؛ لا يوجد إثبات نشر سجلات مؤهلة إنتاجية على `main`. لا يُشترط اختبار **تنزيل Stable الكامل عبر HTTPS** لإثبات طريقة **الاستعلام الجزئي** المتفق عليها؛ يلزم اختبار استعلام بيانات منشورة فعلًا عند توافرها.
- **بوابة الإنتاج:** `docs/implementation/V6-ANDROID-SOFTWARE-ACCEPTANCE.json` ما زالت `NO_RELEASE`؛ إذن تسليم نسخ التطوير لا يعني إغلاق جميع بوابات إصدار إنتاجي معتمد. لا تُرفع أعلام القبول دون أدلة.

**سجل الاستكمال:** [خطة قبول Android V6](https://github.com/HazRob/HazRob-Tank/blob/feature/tank-v6-implementation/docs/implementation/V6-ANDROID-COMPLETION-GATE-2026-10-30_AR.md) · [ملف القبول](https://github.com/HazRob/HazRob-Tank/blob/feature/tank-v6-implementation/docs/implementation/V6-ANDROID-SOFTWARE-ACCEPTANCE.json) · [تحديث السجلات الصغيرة](https://github.com/HazRob/HazRob-Tank/blob/feature/tank-v6-implementation/docs/implementation/V6-INCREMENTAL-CENTRAL-LIBRARY-SYNC-2026-10-30_AR.md) · [GitHub Actions](https://github.com/HazRob/HazRob-Tank/actions).

---

---

HazRob Tank is an **independent tracked hazardous-gas research robot**. Its Android application, ESP32/ESP32-CAM firmware, research ChemCore, and repository are **separate** from the original four-wheel HazRob project.

## مكتبة المراجع المركزية الموحدة | Unified Reference Library

**المرجع المعتمد والموحد لجميع مشاريع HazRob هو مكتبة المنظمة، وليس نسخة جديدة منفصلة لكل روبوت.**

- **[الدخول إلى مكتبة المراجع المركزية — HazRob Organization](https://github.com/HazRob/build-harness/tree/main/library/reference-bank)**
- **[الفهرس العربي الشامل للمراجع](https://github.com/HazRob/build-harness/blob/main/library/reference-bank/INDEX.md)** — بيانات MQ وMOS والتقنيات والأبحاث والمنتجات والمراجع الجزيئية
- **[فهرس المصادر الآلي الموحد JSON](https://github.com/HazRob/build-harness/blob/main/library/reference-bank/external-research/master_catalog.json)** — المعرّفات وروابط البيانات وبيان صلاحيتها وسلسلة المصدر
- **[دليل سياسة المكتبة والحوكمة](https://github.com/HazRob/build-harness/blob/main/library/reference-bank/README.md)**
- **[فهرس المجموعات المرجعية التاريخية](https://github.com/HazRob/build-harness/blob/main/library/reference-bank/LEGACY_COLLECTIONS.md)** — روابط مراجع HR069 وTank والمكتبات القديمة دون ازدواج
- **[نقطة استئناف أبحاث وتجميع المراجع](https://github.com/HazRob/build-harness/blob/main/library/reference-bank/external-research/RESUME.md)**

يتضمن الفهرس المركزي حالياً **80 سجلاً مرجعياً** بتاريخ 2026-10-21: 56 سجلاً في الجمع الأولي، ومرجعَي UCI224/UCI270 اللذين استُكملا عند تدقيق المحادثة، و14 مصدراً صينياً/يابانياً إضافياً (مواصفات ومقالات وبراءات واختبارات Figaro)، و8 مراجع مقارنة R0/Rs. ليست هذه 80 تجربة مستقلة أو معايرات معتمدة. السجلات غير متساوية المستوى: بعضها قياسات فعلية فُحصت، وبعضها دراسات أو صفحات وصف لأرشيفات لم تُنزَّل بعد، وبعضها منتجات أو محاكاة. **لا يجوز عدّ السجلات الثمانين على أنها 80 تجربة مستقلة**.

### نطاق المكتبة وعلاقة Tank بها

- يُضاف كل مرجع علمي أو بيانات قياس مفتوحة إلى **الفهرس المركزي المشترك** بمعرّف دائم، حتى إن استخدم MQ واحداً أو حساسات MOS بديلة.
- ملفات المصدر أو القياسات الكبيرة قد تبقى في مصدرها أو تخزين أبحاث المشروع مع DOI وSHA256 وروابط واضحة؛ **التسجيل المرجعي الواحد يمنع ازدواج احتساب نفس البيانات**.
- كود التقنيات الثمانين وخطط التحقق الخاصة بـTank يظل تحت [`research/`](research/). [الخطة العلمية](research/MASTER_PLAN.md)، [سجل التقدم](research/PROGRESS_80.json)، [نقطة الاستئناف](research/RESUME.md) تخص Tank.
- تجميع المراجع لا يعني اعتماد طريقة تحليل، ولا معايرة أجهزة؛ استنتاج `ppm` أو سلامة أو G3 يتطلب قياسات مرجعية من الروبوت الفعلي وإجراء تحقق مستقل. **Tank G3=0/80** في آخر نقطة تدقيق بحثي مسجلة.
- يمنع نقل برامج أو معايرات المشروع الأصلي إلى Tank دون قرار نقل ومراجعة مستقلة. المكتبة التشغيلية الأصلية HR069 تبقى تحت `sohpany/SERVOC-Android:release/hazrob-0.10.2-rc2/research_data/hr069_analytical_library/`، وفق قواعد الحوكمة المنفصلة.

### المراجعات والدراسات الحديثة

[تدقيق MQ](https://github.com/HazRob/build-harness/blob/main/library/reference-bank/external-research/reviews/MQ_DATASETS_INITIAL_AUDIT_2026-10-21_AR.md) ·
[مكتبة MQDataScience وقواعد بيانات MQ الموسعة](https://github.com/HazRob/build-harness/blob/main/library/reference-bank/external-research/reviews/MQDATASCIENCE_AND_EXPANDED_DATA_2026-10-21_AR.md) ·
[الاستدلال الجزيئي العكسي QSPR/PubChem](https://github.com/HazRob/build-harness/blob/main/library/reference-bank/external-research/reviews/PUBCHEM_QSPR_INVERSE_MOLECULAR_2026-10-21_AR.md) ·
[الفصل الكمي للمخاليط والترشيح والأنظمة التحليلية](https://github.com/HazRob/build-harness/blob/main/library/reference-bank/external-research/reviews/ANALYTICAL_SYSTEMS_MIXTURE_UNMIXING_QUANT_FILTERS_2026-10-21_AR.md).

[دراسات صينية ويابانية عن MQ/MOS](https://github.com/HazRob/build-harness/blob/main/library/reference-bank/external-research/reviews/CHINESE_JAPANESE_MQ_MOS_RESEARCH_2026-10-21_AR.md).

[مقارنة R0 وRs بين دراسات متعددة لحساسات MQ](https://github.com/HazRob/build-harness/blob/main/library/reference-bank/external-research/calibration_audits/MQ_R0_RS_PEER_COMPARISON_2026-10-21_AR.md) — سجلات حساسات فردية وبيان القيم المنشورة غير المتسقة.

**قانون الاستئناف:** قبل أي تجارب أو إضافة مراجع جديدة، راجع `reference-bank/INDEX.md` والفهرس الآلي في مستودع المنظمة، ثم `research/RESUME.md` و`research/PROGRESS_80.json` في Tank. لا تستخدم فروع الأبحاث المؤقتة بوصفها مكتبات مرجعية منافسة للمكتبة المركزية.
