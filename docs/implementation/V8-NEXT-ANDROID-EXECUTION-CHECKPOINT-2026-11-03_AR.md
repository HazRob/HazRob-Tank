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
