# ChemCore Next — مراجعة MQSensorsLib (MQUnifiedsensor) ومرجع معايرة R0
**التاريخ:** 2026-11-02 · **الحالة:** SOURCE-CODE-REVIEWED / PHYSICAL-VALIDATION-PENDING  
**المصدر:** https://github.com/miguel5612/MQSensorsLib  
**الفرع والمرجع الثابت:** `master` عند `59a2e5852ec12f5da3cf508be68f5a9405314003` (آخر commit المسترجع)  
**إصدار المكتبة المثبت في المصدر:** `MQUnifiedsensor 3.0.5` وفق `library.properties`. MIT.  
**المصدر المساند:** https://github.com/miguel5612/MQSensorsLib_Docs
**الأساس المحلي للمقارنة:** `docs/research/V6-CHEMCORE-MQUNIFIED-R0-RATIO-AUDIT.csv` في HazRob/HazRob-Tank V8.

## 1. أصول مؤكدة من الشيفرة
- `src/MQUnifiedsensor.h` (Git blob `11637c2c0132cfc97c57b9482a057e0fa20dc382`)؛ واجهات `setR0,setRL,setVCC,externalADCUpdate,setADC,calibrate,readSensor,readSensorR0Rs,validateEcuation,setRsR0RatioGetPPM,getRS`.
- `src/MQUnifiedsensor.cpp` (Git blob `5b282c7c178f41478d2de2258d98b54ce269d407`). حساب `Rs=(Vcc*RL/Vout)-RL`; `R0=Rs_air/K_air`; `ppm=a*(Rs/R0)^b` في طريقة الانحدار الأولى؛ الطريقة البديلة `10^((log10(ratio)-b)/a)`. `RL` بوحدة kΩ. كلاهما يحتاج تعريف اتجاه المنحنى ومجال المصدر والجهاز الفعلي.
- `examples/MQ135-ADS1115/MQ135-ADS1115.ino` (Git blob `2a4dc3a009387cf29f485decc57ecc18b0aba66c`) يمرر جهد ADS1115 فعلي عبر `externalADCUpdate`؛ مناسب للمقارنة بواجهة Tank، لكن مثال المصدر لا يثبت تكافؤ مقسم الجهد/الاتصال/التغذية على Tank.
- `examples/MQ-135-ALL/MQ-135-ALL.ino` (Git blob `3ffae99864cfc51c12941647290832dc39767bb2`) يحوي 6 معادلات منفصلة لـ CO/Alcohol/CO2/Toluene/NH4/Acetone؛ هذه **منحنيات أحادية المصدر/الحساس** لا بصمة متعددة الحساسات لغاز معروف عند تركيز معلوم.
- ملفات معايرة أو منحنيات MQ2, MQ3, MQ4, MQ5, MQ7, MQ8, MQ9, MQ135 و`MQ-4-ALL`, `MQ-9-ALL`, `Calibration`, `LinearVsExponential`, `MQ-135-CorrectionFactorDHT11`؛ تحتوي على أمثلة وطرق عددية لا إثبات Tank.
- `MQSensorsLib_Docs/Internal_design_documents/Experiments/MQ{2,3,4,5,6,7,8,9,131,135,303,309}_Regression.ipynb` تدل على ملفات انحدار عبر جداول نقاط مصنع. تم فحص دفاتر MQ2/MQ7/MQ8/MQ135؛ تستخدم ملف `Datasheets info.xlsx` وتصنع منحنيات/مخرجات للغازات المختلفة. مصدر الملف الأصلي: https://github.com/miguel5612/MQSensorsLib_Docs/blob/master/Internal_design_documents/Experiments/Datasheets%20info.xlsx ؛ لم ننجح في إعادة تشغيل دفاتر Jupyter وقراءة/تحليل Excel الأصلي رقميًا بعد.
- `MQSensorsLib_Docs` يوفر Datasheets وWPDigitalizer ودفاتر معايرة ومراجع. ينبغي فحص جميع الجداول الأصلية لاحقًا لاكتشاف بيانات إضافية، دون نسب تجارب حقيقية مستقلة إلى منحنيات مستخرجة من Datasheet.

## 2. معاملات الهواء النظيف الموثقة للمستشعرات الثمانية
| النموذج | `K_air=Rs_clean/R0_source` | 
| --- | ---: |
| MQ2 | 9.83 |
| MQ3 | 60 |
| MQ4 | 4.4 |
| MQ5 | 6.5 |
| MQ7 | 27.5 |
| MQ8 | 70 |
| MQ9 | 9.6 |
| MQ135 | 3.6 |

هذه النسب **ليست R0 الفيزيائي ولا ppm**؛ تعليقات `RS/R0=... ppm` في الأمثلة قد تكون مضللة كوحدات، ولا تعني هواء 9.83 ppm.
المعادلة:
`R_air=mean(Rs_air)`; `R0_source=R_air/K_air`; `q_air=Rs/R_air`; `q_source=q_air*K_air` (بشرط أن `K_air` صالح للموديل/مصدر المنحنى ودورة السخان).
**يجب حفظ كل من `R_air` و`R0_source` منفصلًا دون استبدال R0 المحفوظ تاريخيًا على الجهاز.**
`CleanAirResponseGateV6` و`TankV6TelemetryAdapter` يستخدمان مرجع هواء unity للكشف عن التغير؛ لذلك لا يجوز إدخال `R0_source` ذي معامل 9.83 في نفس حقل `R0=Rs_air_mean` دون وسم التعريف.

## 3. أدلة على أخطاء/عدم توافق بالمصدر
1. **خطأ واضح في `examples/MQ-Board/MQ-Board.ino` خطوط 130-137:** المتغيرات MQ3calcR0 إلى MQ9calcR0 تستدعي `MQ2.calibrate(...)` بدلاً من كائن الحساس المعني؛ المثال لا يصلح حاليًا لمعايرة مجموعة دون إصلاح.
2. `MQUnifiedsensor::calibrate` يتعامل مع `_sensor_volt` دون حماية كاملة من `Vout=0` أو قيمة غير واقعية؛ وسلوك `readSensor` يقص Rs السلبي للصفر ويحوّل overflow إلى `FLT_MAX`. يجب في Next إصدار حالة `INVALID_VOLTAGE`, `SATURATED`, `OUT_OF_DOMAIN` بدل تقديم 0 أو تركيز ضخم كمعلومة صالحة.
3. **اختلاف صيغ المعادلات بين المكتبتين:** MQSensorsLib تستعمل `ppm=A*(ratio)^B`، وMQDataScience/V6 تستعمل `ratio=a*(ppm/scale)^b`. القيم ليست معاملات بالمعنى نفسه وإن كان تحويلها ممكنًا رياضيًا: `B=1/b` و`A=scale*a^(-1/b)`. تُؤهل المقارنة بعد تعيين اتجاه النسبة وR0/النطاق؛ بعض المنحنيات قد تشترك في نقاط مصنع واحدة، فلا تُحسب أدلة مستقلة.
4. **تصحيح T/RH غير متجانس:** `readSensor` يضيف `correctionFactor` إلى نسبة المقاومة، بينما مثال `MQ-135-CorrectionFactorDHT11` يملك `getCorrectedResistance` يقسم المقاومة على دالة التصحيح؛ ومكتبة MQDataScience تستعمل نموذجًا ثالثًا `a(RH)+c(RH)exp(b(RH)*T)`. لا تصحيح قبل تعيين تعريف الإشارة ومعاملات/مرجع الرطوبة.
5. **MQ7/MQ9:** أمثلة تتناول دورة سخان ذات مرحلتي جهد 5 و1.4V وطول دورة 60+90 ثانية. صلاحية التطبيق الفعلي مشروطة باللوحة المنفذة فعلًا، ونوع الحساس ومنحنى المرحلة، لا بمجرد تسمية `MQ7` أو `MQ9`. استمرار التسخين 3 دقائق قبل القياس لا يعادل تسوية/تحميص حساس جديد ولا دورة heater سليمة تلقائيًا.
6. سجل V6 المقارن يذكر عدم اتساق حسابي في **سجلات جهاز خارجي 2018 لـMQ7 وMQ8** (`R0_reported` يختلف عن `Rs_air/ratioAir`)، فلا تستخدم هذه الأرقام في انتقال معايرة Tank دون التحقق من مصدر الخطأ.
7. **التباس NH4 وNH3:** اسم `NH4` موجود في MQSensorsLib وMQDataScience بينما V6 يحمل `NH4/NH3`. لا يصح اعتبار الأمونيوم الغازي والأمونيا نفس الهوية دون إعادة تأهيل مصدر المنحنى.

## 4. هل يحتوي المصدر على بصمات جديدة؟
- منحنيات `setA/setB` لكل غاز داخل MQ الواحد **ليست بذاتها بصمات كيميائية متعددة الحساسات**؛ وهي غالبًا مشتقة من الرسومات المنشورة، وقد تكون مكررة علميًا مع MQDataScience.
- يوجد مصدر نقاط منحنى مصنع من `Datasheets info.xlsx` وبنية دفاتر انحدار؛ هذه فرصة لبناء **بصمة نظرية افتراضية مشتركة من عدة منحنيات عند تركيز محدد**، لكن لا يجوز تسميتها بصمة Tank مقاسة أو ادعاء دقة تعرّف حتى تجارب متعددة الحساسات والمخاليط.
- القيمة المضافة العليا: تحويلات مصدرية قابلة للاختبار، تدقيق R0/ADC، نماذج منحنى بديلة، ووثائق المصدر الخام.

## 5. المعمارية المقترحة لإدماج الفوائد في ChemCore Next
- `ElectricalAdapter`: raw ADS/voltage/Vc/RL/ADC range/circuit topology/units/phase.
- `DualR0Profile`: `R_air`, `R0_source`, `K_air`, `K_source_id`, uncertainty and source lineage, captured device/temperature/humidity/heater profile.
- `CurveRegistry`: equations canonicalized to `forwardRatio(ppm)`, inverse with domain flags, original source parameters, manufacturer trace, independenceRootId and sample provenance.
- `EnvironmentModelRegistry`: source-specific additive/multiplicative correction types not interchangeable.
- `CandidateEvidence`: per sensor observation once; source curves from the same datasheet grouped under same independence root.
- `QualityGate`: saturation/noise/drift/heater/warmup/ADC validity; offline research falls back to UNKNOWN without alert.
- `CalibrationProfiles`: never silently overwrite a baseline or convert values without schema identity.
- Test matrix: clean-air, two gases, source curves, missing R0, unit and relay phase, stress and held-out real data.
  
**حدود المرحلة:** التدقيق المصدري الحالي يتضمن ملفات C++ وأمثلة متعددة وأربعة دفاتر Jupyter، وليس مراجعة كاملة لجميع دفاتر ومخططات المستودع التوثيقي أو نتائج اختبار الأجهزة. `NEXT_R0_AUDIT_IN_PROGRESS`، `G3_TANK=0`، دون تغيير الفريموير أو المحرك التشغيلي.
