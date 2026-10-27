# HazRob Tank V5 — فحص أولي لمصادر MQUnifiedsensor ومنحنياتها

**الحالة:** `SOURCE_IDENTIFIED / RESEARCH_CANDIDATES_ONLY / NOT_IMPORTED_INTO_STABLE / NOT_DEVICE_VALIDATED`.  
**السياق:** `V5-005` مع `V5-002` و`V5-003`.  
**التاريخ:** 2026-10-28.  
**المبدأ:** العثور على معاملات منشورة ليس دليلاً على معايرة Tank أو أن "Alcohol" في أمثلة المكتبة يحدد الإيثانول بشكل انتقائي.

## مصادر مباشرة تم التعرف إليها

| المصدر | مرجع القراءة | ما يبرره علمياً | الحد |
| --- | --- | --- | --- |
| `MQSensorsLib` | https://github.com/miguel5612/MQSensorsLib | مكتبة موحّدة لمحولات MQ، أمثلة وعناصر API، ترخيص MIT مذكور في المستودع | لا تُقدِّم شهادة دقة Tank |
| Header `MQUnifiedsensor.h` | https://github.com/miguel5612/MQSensorsLib/blob/master/src/MQUnifiedsensor.h | `externalADCUpdate(volt)`, `setRL(kΩ)`, `setA`, `setB`, `setRegressionMethod`, `calibrate(cleanAirRatio)` | اعتماد النسخة المثبتة والتحقق من API، لا نسخ افتراضي |
| مثال `MQ-3.ino` | https://github.com/miguel5612/MQSensorsLib/blob/master/examples/MQ-3/MQ-3.ino | صف `Alcohol: a=0.3934, b=-1.504` ضمن نموذج `ppm = a × (Rs/R0)^b` البحثي؛ للمراجعة/استخراج provenance فقط | نوع الكحول واللوحة وRL والمنحنى الأصلي ومجال الصلاحية يجب توثيقها قبل أي استخدام |
| مثال `MQ-135-ALL.ino` | https://github.com/miguel5612/MQSensorsLib/blob/master/examples/MQ-135-ALL/MQ-135-ALL.ino | مثال `Alcohol: a=77.255, b=-3.18`، ويعرض غازات أخرى متقاطعة | ليس دليلًا على تخصيص النوع الكيميائي أو دقة ppm في روبوت الدبابة |
| دليل MQ135 الأصلي Winsen | https://www.winsen-sensor.com/manual/mq135.html | شروط القياس و`Rs=(Vc/Vrl−1)×RL` وعوامل الحرارة/الرطوبة وتهيئة >48 ساعة ومجالات الحساسية | المنحنيات نموذجية لمستشعر وظروف محددة؛ استنساخها بلا ظروف/معيار يغيّر معناها |
| مواصفات MQ-7B من Winsen | https://www.winsen-sensor.com/sensors/co-sensor/mq-7b.html | نظام التسخين `5V/1.5V` و`60s/90s` للـCO | تغذية سخان 5V ثابتة في اللوحة لا تثبت صلاحية قياسات CO ذات دورة المصنع |
| MQ-9B من Winsen | https://www.winsen-sensor.com/product/mq-9b.html | تبديل 5V/1.5V بنمط حراري وقياس CO/CH4 ضمن شروط معينة | لا يسوغ نقل معاملات MQ9B إلى MQ9 دون توثيق تطابق النسخة |
| دليل OSHA للمراقبة المهنية | https://www.osha.gov/otm/section-2-health-hazards/chapter-3 | يميز قياس %LEL ومعايرة الغاز الخاص من قياسات حدود التعرض | لا يجيز استنتاج امتثال مهني أو اكتمال السلامة من MQ غير معايرة |

## مقارنة أولية بالشيفرة الحالية

- `firmware/ESP32-Tank/Hardware.cpp` يستخدم معادلة المقاومة المستندة إلى `Cfg::RL`، ويحسب معايرة الهواء النظيف `Rs/cleanAirRatio`؛ **لم يثبت من فحص هذا الملف وجود استدعاء مباشر إلى `MQUnifiedsensor.h`**. الخطة القديمة ذكرت تكامل المكتبة كهدف، ولا يجوز الإشارة إليه كتنفيذ مؤكد.
- يُستخدم ADS1115 الخارجي، لذلك عند تنفيذ مكتبة MQ مستقبلًا يجب أن يستعمل الجهد المقاس فعليًا `externalADCUpdate` ولا تُنسخ أمثلة `analogRead` ذات ADC 10-bit إلى ADS1115 16-bit.
- حزمة Stable المضمنة V4 أظهرت 652 هوية و49 `response_priors` و0 نموذج M4 موثق تجريبيًا من Tank (آخر تحقق مصدر/بناء)؛ **لا يثبت هذا أن الإيثانول يستفيد من جميع المنحنيات**.

## بطاقة تقييم نموذج قبل استيراده

```yaml
candidate_id: string
sensor_model_exact: MQ3 | MQ135 | ...
target_gas_name: string
cas: string_or_null
source_url: string
source_commit_or_version: string
license: string
curve_origin: manufacturer | literature | digitized | example
a: positive_number_or_null
b: number_or_null
equation: "ppm = a * (Rs/R0)^b"
rs_r0_definition: string
r0_reference_condition: string
rl_kohm: number_or_null
heater_protocol: string
vcc_v: number_or_null
temperature_c: number_or_null
relative_humidity_pct: number_or_null
min_ppm: number_or_null
max_ppm: number_or_null
validity_notes: string
digitization_error: number_or_null
evidence_tier: M1 | M2
device_validated: false
tank_g3_grouped_holdout: NOT_RUN
promotion_status: RESEARCH_CANDIDATE
```

## مهام البحث اللاحقة (غير منفذة)

1. تثبيت SHA للمكتبة وإصدارها والترخيص وقراءة أمثلة MQ2/MQ3/MQ4/MQ5/MQ7/MQ8/MQ9/MQ135 كلها، لا مثالَي الكحول فقط.
2. مقارنة المعاملات مع الرسوم الأصلية ودراسة نوع الغاز ومرجع `R0`، وعدم نقل قيم `clean air` إذا كانت معادلة الغاز المعياري مختلفة.
3. استخراج منحنيات `reference_priors` في ملف منفصل مع مصدر/نطاق/ملاحظات وسياسة duplicate sensors؛ عدم تعديل `active Stable` أثناء الدراسة.
4. اختبار نماذج المركب الواحد، المخاليط، open-set وEthanol في محرك التحليل بمحاكيات من مصدر مرجعي، ثم مراجعة تجارب Tank الحقيقية `G3` قبل الاعتماد.
5. تصنيف نتيجتي MQ3/MQ135 المذكورتين أعلاه مثالاً **مرشحًا للفحص فقط**، وليس نموذجًا أضيف إلى المكتبة أو تقدير Ethanol صحيحًا.

**المخرج المنشود في خطوة V5-W04:** `mq_source_inventory.csv`، `candidate_priors.json`، `model_provenance.json`، `curve_validation_results.json`، `scientific_review_decisions.md`. لا تُنتج هذه الملفات قبل الحصول على بيانات المصدر والاختبار الفعلي.
