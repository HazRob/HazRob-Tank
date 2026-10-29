# V6-003 — دراسة إضافة بصمات إحماء MQ ومكتبات Bosch BME688/BME690 إلى ChemCore Tank

**التاريخ:** 2026-10-29 · **حالة التصميم:** `SOURCE_REVIEWED / EXPERIMENTAL_FEATURE_TRACK_APPROVED_FOR_RESEARCH_ONLY` · **الفرع:** `feature/tank-v6-implementation`  
**النطاق:** بحث علمي + أداة تسجيل وميزات مبكرة منفصلة + مراجعة توفر المكتبات. **لا تغيير لسخان MQ ولا لمراحل R0 أو نتائج الاستدلال المعتمدة في التطبيق.**

## ملخص القرار الفني
الفكرة جديرة بالتجريب، لكن «قياس استجابة MQ خلال أول دقيقة بعد تشغيل السخان» يختلف عن «مسح BME688 متعدد درجات حرارة السخان» وعن «استجابة تعرض غازي بعد التسخين المستقر». لذلك يُطبّق ثلاث مراحل بحث مستقلة:

1. `POWER_ON_TRANSIENT`: لحظة إغلاق Relay تغذية MQ (t=0) حتى 60 ثانية، تشغيل السخان عند جهد المصنع **دون أي تحكم حراري مبتكر**؛ الناتج *fingerprint features research only*.
2. `STABLE_EXPOSURE_TRANSIENT`: بعد إتمام فترة الإحماء المعتمدة، رصد فترة تفاعل الغازات مع الحساسات عند إدخال عينة مرجعية أو تغيير التعرض؛ هذا يلتقط استجابة مبكرة **دون إعادة بدء السخان**.
3. `STABLE_ANALYTICS`: استمرار R0 وRs/R0 والقياسات العادية بعد الاستقرار؛ لا يتم إلغاؤها ولا تحويل early readings إلى ppm.

**ممنوع:** حذف warm-up لتحقيق startup analytics، أو حساب R0 من بداية الإحماء، أو تسميتها عينات مستقلة للغازات، أو استبدال دورة MQ7B/MQ9B المطلوبة إذا ثبت الطراز المادي، أو تطبيق ملف حرارة Bosch على MQ ذي كتلة حرارية مختلفة.

## المراجع الأولية التي تمت مراجعتها

| المرجع المباشر | ما يثبت | القيد |
|---|---|---|
| [Bosch BME688 introduction](https://www.bosch-sensortec.com/software/bme/docs/overview/introduction.html) | Micro-hotplate؛ **عشر نقاط حرارة** داخل ملف المسح، وقدرة مقارنة ملفات السخان | يتطلب شريحة BME688/BME690، ليس BME280 |
| [Bosch AI Studio software](https://www.bosch-sensortec.com/en/software-tools/software/bme688-and-bme690-software) | `BME AI-Studio 3.2.1 (Aug 2026)`، `BSEC 3.3.0.1 (Aug 2026)`، Dev Kit software, mobile app | BSEC binary closed/source-restricted license، لا يعمل كخوارزمية MQ مباشرة |
| [Bosch BME data workflow](https://www.bosch-sensortec.com/software/bme/docs) | Configure→Record→Import→Specimen→Train→Evaluate | منهج التوثيق قابل للاقتباس برمجياً؛ النماذج ليست مدربة على MQ |
| [Bosch mobile raw labels](https://www.bosch-sensortec.com/software/bme/docs/overview/mobile-app.html) | قراءات `gas resistance` بحسب heater step وT/RH/P والوسم | لا يعني أن تطبيق Bosch يعمل على أي ESP32+MQ |
| [Bosch specimen format](https://www.bosch-sensortec.com/software/bme/docs/technical-specification/bmespecimen.html) | `.bmespecimen` تنسيق JSON لـ`meta,data,sensors,heaterProfiles,cycles,specimenDataPoints` | حفظ أسماء/source schema/ترخيص دون ادعاء أن BSEC binary مفتوح |
| [Bosch recording guidance](https://www.bosch-sensortec.com/software/bme/docs/process-steps/record-data.html) | يوصي بمقارنة عينات عدة ظروف و≥50 دورة في كل حالة | الدقيقة الواحدة ليست كافية لتدريب Bosch لكافة ملفاتها |
| [Bosch stabilization guide](https://www.bosch-sensortec.com/software/bme/docs/overview/introduction.html) | حساس Bosch الجديد يحتاج استقرارًا أوليًا **24 ساعة على الأقل** | لا تصح المقارنة مع startup الأول كبيانات مدربة |
| [Bosch BME68x API BSD-3](https://github.com/boschsensortec/BME68x_SensorAPI) | Driver BME680/BME688 مفتوح | يتطلب وحدة حسّاس مختلفة |
| [Bosch Arduino BME68x](https://github.com/boschsensortec/Bosch-BME68x-Library) | تكامل Arduino لـBME680/BME688 | ليس محرك AI أو مكتبة MQ |
| [Bosch BME690 SensorAPI BSD-3](https://github.com/boschsensortec/BME690_SensorAPI) | Driver جديد BME690 | يحتاج BME690 نفسه |
| [Bosch BSEC2 Arduino](https://github.com/boschsensortec/Bosch-BSEC2-Library) | BSEC2 مع ESP32 وأمثلة؛ أعمدة binary/config تحت ترخيص Bosch | لا نسخ الثنائيات/إعادة توزيعها دون مراجعة شروط SLA |
| [Initial action period early correction](https://pmc.ncbi.nlm.nih.gov/articles/PMC6679561/) | transient عند الإشعال يمتد ثوان/دقائق، يتأثر بالمدة خارج التشغيل والبيئة، ويمكن تصحيحه في حساس TGS مُختبر | sensor-specific، **ليس** وصفة عامة لمصفوفة MQ |
| [Gas sensors power-on correction](https://www.sciencedirect.com/science/article/pii/S0925400516313144) | نموذج استجابة أولية وتصحيح لمنظومة MOS مع `R²` منشور مرتبط بإجراء مصدره | لا يمنح دقة هوية غاز Tank أو تشغيل دون إحماء |
| [EUROSENSORS 2026 early recognition poster](https://www.ama-science.org/proceedings/details/7205) | AUC-20s وderivative وrelative response قد تساعد في تصنيف غازات على بيانات حساسات معينة | poster غير كافٍ لنقل المعاملات/الدقة |
| [Temperature-modulated MOS study](https://www.sciencedirect.com/science/article/pii/S0925400512002341) | إثبات فائدة التحكم الحراري وقيود القصور الحراري | الورقة تتضمن pulse over-nominal؛ **ممنوع استنساخ ذلك على MQ دون تأهيل كهربائي/حراري** |
| [NIST temperature pulse transient](https://www.nist.gov/publications/thermally-assisted-transient-analysis-reducing-response-time-microhotplate-gas-sensors) | temperature-modulated microhotplate يمكن أن يحسن التمييز في الإعداد المختبري | لا ينقل ميكانيكيًا إلى MQ |
| [Winsen MQ-7B](https://www.winsen-sensor.com/sensors/co-sensor/mq-7b.html) و[MQ-9B](https://www.winsen-sensor.com/product/mq-9b.html) | **5 V 60s / 1.5 V 90s** هي دورة محددة إذا كان الطراز B، مع `preheat >48h` في MQ7B | لا يمكن تنفيذها على relay أحادي 5V دون عتاد خاص وتحديد variant |

**BME280 في Tank:** مرجع BME280 يقيس `T/RH/P` ولا يقيس مقاومة غاز. وظيفته في V6 تحسين ضبط البيئة/الموثوقية عند تحليل MQ، أما إجراء ملفات حرارة Bosch فيحتاج إضافة BME688 أو BME690 فعليًا.

## تحليل العتاد الموجود — من شيفرة V6

في `firmware/ESP32-Tank/Hardware.cpp`:
- `relayRaw(on)` يسجل `mqPowerAt=millis()`، و`mqPowerMs()` يعيد الزمن المنقضي من أول تشغيل.
- `dataJson()` يُخرج `relay.power_ms`، و`mqJson()` يتضمن `rs`,`ao`,`ratio`,`r0`,`ok`,`scan_cycle`,`sample_us`,`age_us`,`powered`.
- `Cfg::MQ_PERIOD=100 ms` يعني دورة تحديث مسح MQ داخل فيرموير، **وليس ضمان وصول 10 عينات/ثانية للهاتف** عبر HTTP.
- الثمانية MQ على ADS1115×2، relay واحد يغذي السخانات؛ لا يوجد التحكم الفردي بمستويات حرارة MQ ولا قياس مباشر لحرارة كل سخان.
- `Config.h` يبقي `R0_REFERENCE_GAS_VALIDATED=false` ولا يستنتج مرجعًا ماديًا من عينات startup.
- `TankAutoR0Math` في التطبيق يستخدم دورة 5min تسخين ثم 3min قياس/3min انتظار مكررة؛ **لا يتغير**.
- إذا انقطعت التغذية أو أُعيد تشغيل relay أثناء اللقطة: invalidate/reset startup exposure and provenance.

**النتيجة:** يمكن تجريب تحليل النوافذ الزمنية دون تعديل فيرموير الآن بشرط وصول timestamps حقيقية وعدد عينات كافٍ، لكن اختبارات أداء ديناميكي مرتفع قد تتطلب endpoint مستقلًا وحلقة ring-buffer بتسلسل/طابع microseconds؛ ينفذ ذلك **بعد** اختبار الضغط/الحمل والتحقق من safety/watchdog ومن دون حجب القيادة.

## تصميم الخبرة المقترح

**المستوى A — استخراج ميزات رياضية فقط (يمكن تنفيذه الآن):**
- عيّنة منظمة `device_id,sensor_instance,model,power_event_id,relay_power_ms,sample_us,raw_adc,ao_v,rs_original_unit,r0_state,heater_supply_state,T,RH,P,mq_ok,age_us,monotonic_time`.
- ميزات `log10(Rs(t)/Rs(t0))` (نسب من نفس الحساس **لا تعني Rs/R0**)، `AUC20`, `AUC60`, `early slope`, `late slope`, `rise/recovery` عند وجود مرحلة الغاز الصحيحة، `missing_fraction`, `max_sample_gap`,`shape curvature`.
- أي ملف ناقص أو timestamp غير متزايد/بلا مصدر power-event أو `MQ7/MQ9 HEATER_UNKNOWN` يحمل `REJECTED`/حالة QC وليس تنبؤًا بالغاز.
- لا فئة/غاز/PPM من هذه الميزات دون بيانات مبنية على تعرضات مرجعية مستقلة.

**المستوى B — تجربة غاز مرجعية مضبوطة لاحقًا:**
- ضبط وتسجيل `power_off_duration`, `last_gas_exposure`, `ambient_storage`, `RL/VC/heater`, `BME280 T/RH`، ومدة start transient لكل قناة.
- مقارنة أربع أذرع: `cold_clean_air`, `cold_gas_exposure`, `steady_clean_air`, `steady_gas_exposure`. الأخيرة لا تستلزم فصل تغذية السخان.
- كرر عبر أيام وأجهزة ونقاط تركيز مختلفة مع `independent_exposure` ومرجع تركيز حقيقي؛ احتفظ بفصل `G3` الخاص بالقياس المستقر.
- قارن `20s`,`30s`,`60s`,`full_stable`,`early+stable` عبر held-out day/concentration/device؛ تقارير `balanced accuracy`, `unknown rejection`, `coverage`, `no false-M4` مع bootstrap وفق exposure وليس timestamp.
- ينبغي إجراء دراسة مخاطر قبل تكرار تشغيل وإطفاء السخانات؛ لا يحدث toggling تلقائي بغرض الحصول على بصمات مع كل قياس.

**المستوى C — Bosch hardware track منفصل:**
- عند شراء BME688/BME690: تثبيت `SensorAPI` مع رقم المراجعة، معايرة/استقرار الجهاز وفق توصيات Bosch، جمع ملف heater-step مع معرفات الملفات والمراجع، تدريب BME AI-Studio/BSEC خارج Tank ثم استيراد خرج classifier أو raw profiles إلى ChemCore **بتسمية `BME68X_LIBRARY_PROVENANCE` مستقلة**. لا دمج بين BME sensor وبين BME280 القديم.
- يمكن استخدام مستورد `.bmespecimen` مستقبلاً بعد مراجعة license/data privacy. لا تتضمن هذه المهمة أي ملف RAW من Bosch، ولا تُوزع ثنائيات BSEC.
- في المدى الحالي مصدر Bosch الأجدى هو **نظام إدارة العينات وملفات التوثيق والتعلم والتقييم**، لا الاعتماد على مكتبة BSEC لتحليل MQ.

## بوابات القبول
`G-T01` حفظ الإحماء/R0 دون تغيير.  
`G-T02` timestamps monotonic محليًا وreset عند relay power-event change.  
`G-T03` عدم عرض `cold-start fingerprint` كهوية/PPM/G3.  
`G-T04` لا تدريب على مصدر gas غير موثق، ولا split للنوافذ من exposure واحد عبر التدريب والاختبار.  
`G-T05` تفصل النتائج `POWER_ON_TRANSIENT` عن `GAS_EXPOSURE_TRANSIENT` وعن `BOSCH_HEATER_SCAN`.  
`G-T06` ملف heater الآلي يحتاج هوية/مدى جهد/متحكم خاص وصحته الكهربائية والحرارية أولًا.  
`G-T07` دليل عدم التراجع/إيقاف الطوارئ وCI وAndroid runtime قبل تفعيل المكون.  
`G-T08` تستخدم T/RH/P من BME280 كمتغيرات سياق، وليس GAS measurements.

**المخرجات المخطط لها:** `research/chemcore_v6/startup_transient.py`، `test_startup_transient.py`، سيناريوهات `SIM-T01..T08`، `research/chemcore_v6/results/mq_startup_simulation.json` من GitHub CI، ثم `android/app/.../TankStartupTransientFeatureExtractor.java` تدريجيًا إذا نجحت المواصفات.  
**غير منجز ضمن هذه الوثيقة:** تجارب رطوبة/غازات فعلية، benchmark MQ early 60 seconds، تعديل firmware أو إضافة Bosch hardware أو APK.
