# نقطة استئناف V8 — نوفمبر 2026 (P07.2–P07.4)
**الفرع:** `feature/tank-v8-implementation` • **آخر حالة مفحوصة:** `90ef967a492dfe563fd04903245381fdd24a7cff`.
**قاعدة العمل:** لا تسجل `DONE_VERIFIED` قبل اجتياز اختبار القبول الفعلي الخاص بالمهمة؛ CI تجميع Java ليس فحص Android ولا فحص Tank.

## ما اكتمل وما لم يكتمل
- P07.1 **DONE_VERIFIED** في سجل المهام: Mapper ثماني القنوات وتطبيع R_air، اختبارات G1 وبرمجة Android. لا اختبار روبوت.
- P07.2 **DONE_VERIFIED (Android12 three tests PASS, run 41525071725)**: SQLite V15 additive/immutable، 10 اختبارات SQLite وتجمـيع Android PASS. التشغيل الأول على Android12 (run 41404801350) فشل عند `validateSigningDebug` لأن `source/signing/hazrob-prototype.jks` غير موجود. تعديل CI `33fa84324e2c603258e1d6792faabf0c1def88ab` جعل debug instrumentation يستخدم توقيع اختبار Android الافتراضي في بيئة مؤقتة. إعادة التشغيل السابقة `41525071725` قد نجحت بالفعل على Android 12 بثلاثة اختبارات Instrumentation (migration 14→15 والحفاظ على القديم، replay/duplicate، triggers للقرار غير القابل للتعديل). اختبار المطابقة الإضافي لـ12 مرشحًا أضيف لاحقًا ولم يُعتمد بعد.
- P07.3 **IN_PROGRESS**: مُنجز برمجيًا NextEngineRoutingPolicy وNextEngineRouter الذي يتحقق من **ملف** Stable ويفشل عند عدم وجوده، ومفتاح Settings البحثي مع حفظ ذري. G1 12 assertions run 41525622327 PASS، وتجميع Android run 41525715751 PASS. **لم يُربط بعد بمسار TankInstantAnalyzer أو جلسات الميدان؛ ولم تختبر شاشة الإعدادات على جهاز Android فعلي**.
- P07.4 **IN_PROGRESS**: NextDecisionPresentation/NextDecisionEnvelope متطابقان مع قرار Next الأصلي ويحتفظان بـ0/1/5/10/12/80 مرشحًا، 32 G1 checks PASS في 41525622327، Android compile PASS 41525715751. أُضيف فحص Instrumentation 12 مرشحًا للمساواة بين القرار المحفوظ ونسختي عرض الشاشة والتقرير، ولم ينجح تشغيله على Android12 بعد. ملفات PDF/XLSX لا تزال تحتوي مسارات legacy ويجب تحويلها ضمن P08/P09/P12.

## عناصر البوابة الشاملة التي طلبها المالك
نجح جرد مصدر العناصر [تشغيل 41525715765](https://github.com/HazRob/HazRob-Tank/actions/runs/41525715765): 114 وحدة مصدر، 24 وحدة تقارير، **516 موضعًا** لعناصر التحكم/الأحداث/الرسوم/الإجراءات، و**3612 حالة اختبار مطلوبة** عبر 7 سيناريوهات لكل موضع. الجرد source-indexed فقط **ولا يثبت صلاحية زر أو وظيفة أو مخطط أو تقرير على Android**. استعراض UI runtime واجب، وتبقى بوابة الإصدار محجوبة.

## أول إجراءات الاستئناف
1. P07.2 أُغلق على أساس run 41525071725. افحص [نتائج المحاكي 41525945844](https://github.com/HazRob/HazRob-Tank/actions/runs/41525945844) لاختبار المطابقة الإضافي، ولا تحسب الاختبار الجديد Pass مسبقًا.
2. تحقّق من أحدث G1 وAndroid compile لفرع HEAD بدل الاعتماد على green قديم إذا غُيّرت الشيفرة، وسجّل run URL وSHA في ملف tracker.
3. أكد استمرار نجاح P07.2 بعد أي تعديل على SQLite؛ لا يلزم إعادته إلا عند تغير الشيفرة/العقد الذي يؤثر فيه.
4. أكمل توصيل NextEngineRouter بـ`TankInstantAnalyzer` والقياس الميداني، ثم NextDecisionEnvelope للواجهات والتقارير قبل إغلاق P07.3/P07.4.
5. انتقل بعد ذلك إلى P08.1/P08.2 ثم P09 مع سيناريوهات فشل الجلسات، وابق على وضع `RESEARCH_ONLY` وG3=0/80.

**ممنوع:** إنشاء APK V8 بوصفه إصدارًا مؤهلًا أو إعلان إكمال أي مرحلة استنادًا إلى نجاح Java compilation وحده.


## استكمال في المحادثة الحالية — القياس الآني
- أُدمج `NextEngineRouter.analyzeAndPersist` داخل `TankInstantAnalyzer.analyze(Context,...)` وهو الآن مسار القياس الآني الافتراضي، مع إبقاء `analyzeV6Comparison` صريحًا للتشخيص فقط. لا تُصنع قائمة 144 و لا يُحقن source_R0 الافتراضي.
- تم ربط واجهة `MeasurementsTab` بمخرجات `NextDecisionEnvelope` البحثية، ومنع زر «فحص 144» من الترويج للاستدلال في وضع Next. **هذه مرحلة Wiring في الشيفرة، وليست إثبات Android runtime**.
- `NextAndroidAdapter` لا يعيد تفسير `adc` الخام بوصفه V؛ يحتاج الحقل المصرح `adc_voltage_v` أو `voltage_v`، وإلا يبقى voltage=null. لا يوجد اشتقاق Rs من ADC دون RL/Vcc.
- `P08.2` بدأت في الشيفرة فقط. المجموعة المتاحة من الخبراء المؤهلين لعتاد Tank لا تزال فارغة (G3=0)، وبالتالي `UNKNOWN` وقائمة خالية ممكنتان ومقصودتان؛ **هذه ليست تجربة نجاح للتعرف على مادة**.
- أُضيف اختبار بنيوي `test_v8_next_instant_source_routing.py` إلى بناء Android ويجب توثيق نتيجة workflow `41527354854` قبل ترقية الحالة.
- الأعمال الإلزامية التالية: حل فقد العينات/العد التنازلي 60+60، إنشاء source bindings موثقة، ربط الجلسات وصلاحية الريلاي، تحويل PDF/XLSX إلى قرار واحد وإتمام Android12 UI tests وbench Tank.

## أحدث قبول بناء للقياس الآني
- GitHub [Android compile 41527354854](https://github.com/HazRob/HazRob-Tank/actions/runs/41527354854) **SUCCESS**، متضمنًا 6 اختبارات بنيوية `test_v8_next_instant_source_routing.py` (كلها PASS). هذا لا يساوي نجاح قياس الهاتف الفعلي أو معايرة غاز.
- GitHub [Android12 SQLite 41525715760](https://github.com/HazRob/HazRob-Tank/actions/runs/41525715760) **SUCCESS**، 3 اختبارات (نسخة أخرى مستقلة عن إغلاق P07.2 السابق)، بينما اختبار المطابقة بعد إضافة 12 مرشحًا ما زال يحتاج نجاح تشغيل مع مطابقته.
- لا يزال P07.3/P07.4/P08.2 `IN_PROGRESS`: قياس Next موجود في `TankInstantAnalyzer` ويعيد `UNKNOWN` عند غياب مصادر مؤهلة؛ تقارير PDF/XLSX والجلسات لم تُحوّل بعد. نطاق G3 لا يزال صفرًا.
