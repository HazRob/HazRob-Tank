# G2 — مقارنة مصادر القياس مع متطلبات ChemCore V4 (2026-10-26)

**مصدر المعلومات:** `HazRob/build-harness/library/reference-bank/external-research/master_catalog.json`، blob SHA `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f`. هذا تقييم ملاءمة metadata، وليس إعادة تشغيل benchmarks على raw data.

| Dataset | MQ مؤكدة | الإشارة | بيانات مرجعية | استقلال التجارب | الملاءمة |
|---|---|---|---|---|---|
| MQ-EXT-15 / UCI1081 | MQ135 | 10 قنوات زمنية 1 Hz؛ 300s baseline و600s injection | غاز وتركيز ppb منشور؛ ليس معايرة Tank | NOT_AUDITED | temporal fingerprints / classification / open-set؛ الأولوية 1 |
| MQ-EXT-03 / MultimodalGasData | MQ2 MQ3 MQ5 MQ7 MQ8 MQ135 | 10-bit ADC | تصنيفات منشورة، ليس ppm مستقلاً | NOT_AUDITED | cross-sensor fingerprints / classification؛ نسخة mirror لم تطابق الأصل بايتياً |
| MQ-EXT-10 / Wine Spoilage | MQ3 MQ4 | Rs kΩ زمني + T/RH | سلسلة محاليل إيثانول؛ لا يجوز اعتبارها ppm في الهواء | NOT_AUDITED | Rs-domain response / environmental adjustment؛ الخام غير مفحوص |
| MQ-EXT-14 / four vapors | أنواع الحساسات غير مثبتة في catalog | جهد كهربائي | labels/conditions يلزم تدقيقها | NOT_AUDITED | مقارنة انتقائية فقط بعد فحص الملفات |
| UCI-EXT-270 / drift | لم يثبت أنها MQ؛ 16 MOS | 128 features + concentration + batch | تراكيز مرجعية ضمن المجموعة | batch metadata موجودة | drift / grouped generalization / regression؛ لا يُنقل R0 إلى MQ |

## استنتاجات مقارنة
1. **Rs مقابل ADC:** لا يمكن تحويل ADC إلى Rs بدون جهد التغذية وRL ونموذج الدارة؛ ولا يمكن تحويل Rs إلى Rs/R0 بدون R0 متسق مع حالة التسخين والمعايرة.
2. **التعرف على الغاز مقابل التركيز:** UCI1081 توفر غازات وتراكيز ppb منشورة؛ MultimodalGasData تدعم labels أكثر من دقة ppm؛ Wine Spoilage ليست مرجع ppm في الهواء.
3. **الاستقلال:** فهرس المكتبة يضع استقلال التجارب NOT_AUDITED للمصادر MQ الثلاثة؛ يجب حجز مجموعات تعرض/جلسات مستقلة قبل حساب G2.
4. **انجراف الأجهزة:** UCI270 أفضل benchmark متعدد دفعات لاختبار خوارزميات drift، لكنه لا يثبت تعويض انجراف حساسات MQ الثمانية في Tank.
5. **المخاليط:** لا توجد في هذه الخمسة بيانات كافية مثبتة لتقدير تراكيز مكونات مخلوط مستقل في Tank؛ تُستخدم UCI308/322 كاختبارات MOS منفصلة عند تحقق raw.

## خطة تنفيذ مقيدة بالدليل
- استرجاع ملفات raw من مصادر سبق اقتناؤها؛ تسجيل sha256 وبنية الملفات.
- تدقيق identifiers للمجموعة والجلسة والجهاز والتاريخ؛ إذا غابت، لا يُسمح بــgroup holdout مزعوم.
- baseline موحد: nearest-centroid / ridge أو classifier مناسب؛ split grouped؛ error bars؛ holdout untouched؛ report per-technique.
- إبقاء G2 الجديد BLOCKED لكل تقنية حتى يوجد artifact مستقل من تشغيل فعلي، لا نتيجة منشورة غير معاد إنتاجها.
