# HazRob Tank — تحقق تأثير محولات مستويات I2C على قراءات ADS1115
**التاريخ:** 2026-11-02 | **الفرع:** feature/tank-v8-implementation
**الحالة:** SOURCE_AND_STATIC_FIRMWARE_VERIFIED / PHYSICAL_BENCH_NOT_DONE
**الموضوع:** تأكيد المستخدم: كل ADS1115 متصلة عبر وحدة تحويل مستويات I2C من جهة 5V إلى جهة ESP32 3.3V. هل تتغير نتائج ADC الرقمية؟

## 1. القرار الهندسي
- مبدئيًا **لا**: عندما يكون المحول ثنائي الاتجاه متوافقًا مع I2C (open-drain) وتتحقق مستويات المنطق وتوقيت الإشارات، لا تتغير بتات نتيجة ADC ولا دقتها الرقمية. المحول يغير الجهد العالي HIGH على SDA/SCL، ولا يغير الإشارة التناظرية الداخلة إلى ADS1115 أو مكسب PGA أو مقاومة Rs المقاسة.
- هذا الحكم مشروط **بسلامة الناقل**. محول المستويات والأسلاك ومقاومات pull-up تؤثر على rise-time وclock timing وACK/NACK واحتمال فشل أو تأخر القراءة، وخاصة عند 400 kHz. البيانات الصحيحة لا تنحرف رقميًا بصورة تدريجية بسبب المحول، ولكن قد تضيع/تتأخر أو تُقرأ عينات قديمة أو غير موثوقة عند وجود مشكلة اتصال.
- التحويل لا يجعل مستشعرًا غير معاير مؤهلًا ولا يمنح الجهاز G3. الجهاز لم يُفحص عمليًا أثناء هذه المراجعة.

## 2. خريطة المسارين
**تناظري:** MQ AO → ADS1115 AIN0..AIN3 → conversion 16-bit/PGA/reference. هذا المسار لا يمر عبر وحدة تحويل مستويات I2C.
**رقمي:** ADS1115 SDA/SCL (جهة HV، pullups إلى 5V) ↔ bidirectional I2C level shifter ↔ SDA/SCL ESP32 (جهة LV، pullups إلى 3.3V).
**قدرة التغذية:** تأكيد المستخدم أن ADC تعمل بجهد 5V هو معلومة تركيب، لا قياس DMM مستقل. 3.3V في I2C لا يعني أن ADS VDD=3.3V. يجب قياس VDD فعليًا قبل التأهيل.
**تفصيل تنفيذي:** يجب التأكد من نوع المحول، هل MOSFET/BSS138 أو IC bidirectional فعلي. مقسم مقاومات أحادي الاتجاه على SDA لا يكفي لضمان ACK ثنائي الاتجاه. لا يغني ذلك عن الأرضي المرجعي الصحيح.

## 3. تدقيق الشيفرة الفعلية
**firmware/ESP32-Tank/Config.h** Git blob e4746fbd3049e5ae82b3f3ba976e75a2c749a5ac:
- GPIO21 SDA وGPIO22 SCL؛ ADC1 address 0x48، ADC2 address 0x49؛ منفصلان عن EnvWire للحساسات البيئية.
- إعداد ADS_I2C_HZ = 400000، ADS_SUPPLY_V=5.0، ADS_FULL_SCALE_V=6.144، MQ_VCC=5.0، RL=10 kΩ، ADC_SAMPLES=4، معدل 860SPS.
**firmware/ESP32-Tank/Hardware.cpp** Git blob d28f529089d1546d641e1cc0eade9874defb5ffe:
- يبدأ Single-shot conversion عبر تكوين ADS1115 حسب MUX، PGA ±6.144، و860SPS.
- ينتظر delayMicroseconds(1300) ثم يقرأ conversion register؛ لا يختبر OS bit=1 للتحقق من اكتمال التحويل في كل مرة.
- عندما تخفق معاملة I2C أو لا تعود البيانات الكافية، يرجع رمز فشل؛ وإذا فشلت العينات كلها يُسجّل عدم الجاهزية. تُحسب متوسطات الأربع قراءات؛ قد توجد حالات sample stale/aliasing غير مختبرة.
- اعتبار التوقيت الثابت سببًا للقراءات الثابتة هو **فرضية تحتاج فحصًا** لا نتيجة مثبتة، ولا تُنسب تلقائيًا لوحدة تحويل المستويات.

## 4. خصائص المحول وتوقيت الناقل
- يدعم ADS1115 I2C Fast mode حتى 400 kHz؛ الحد الأقصى لزمن ارتفاع SDA/SCL عند هذا المعدل t_r = 300 ns، في حين Standard 100 kHz يسمح حتى نحو 1000 ns. 400 kHz هو حد أعلى وليس إثباتًا أن التركيب مع المحول فعليًا يلتزم بمتطلبات timing.
- محول NXP AN10441 MOSFET ثنائي الاتجاه مناسب نظريًا لـ100/400 kHz عند تحقق pullups/capacitance.
- حسب TI SLVA689A: t_r ≈ 0.8473 × R_pullup × C_bus. مثال حسابي غير مقاس: R=4.7kΩ وC=100pF يعطي t_r≈398ns، وهو أكبر من حد Fast-mode 300ns؛ لا نستنتج أن قيمة R أو C هذه موجودة في الروبوت.
- مع وحدتي ADS1115 ومحولي مستويات منفصلين على ناقل ESP32 واحد، قد تتوازى مقاومات pullup على الجانب المنخفض وتتغير capacitance والسحب؛ تحقق من فعالية الجهتين وسلامة VOL دون اختراع تكوين الأسلاك الفعلي.
- نطاق ADS PGA ±6.144V يحدد التدرج الرياضي، **ولا يعني** أن AIN يمكن أن تتجاوز VDD التناظرية (≈5V بحسب التركيب)؛ محول المستوى لا يحمي مدخل AIN التناظري.
- صيغة المقاومة المصدرية الحالية تعتمد على V_AIN مقاسة داخل ADS وV_MQ/RL ولا تتضمن معامل تصحيح للـ5V↔3.3V الرقمي؛ لا تعديل R0 أو Rs بسبب وجود level shifter.

## 5. اختبار الدبابة الفعلي — بروتوكول HW-I2C-01..05
**HW-I2C-01 قياسات التغذية:** قِس ADS VDD، ومجهود HV/LV للمحول، وجانب ESP32 HIGH في SDA/SCL، وتأكّد أنه لا يرتفع إلى 5V. افحص الأرضي والتركيب. لا تضع 5V مباشرة على GPIO الخاصة بـESP32.
**HW-I2C-02 الأوسيلوسكوب/Logic Analyzer:** تحقق عند 400kHz من high/low وrise/fall وACK/NACK وSCL clock على الجهتين، مع الوحدتين معًا. افحص pullups المكافئة وتوزيع مقاوماتهما.
**HW-I2C-03 المقارنة الضابطة 100/400kHz:** في نسخة تشخيصية مؤقتة، أبقِ نفس الإدخال التناظري الآمن الثابت (مثلاً 1.0V أو 2.0V عندما يكون متوافقًا مع دائرة ADC ومتطلبات التغذية)، واقرأ على الأقل 1000 تحويل لكل قناة ولكل ADS مع تسجيل raw/mean/std/invalid/NACK/timeout/age. قارن 100kHz بـ400kHz دون تغيير أي عامل آخر. لا يُفصل MQ ولا يُدخل مصدر قياسي إلا عند ضمان عدم تعارضهما كهربائيًا.
**HW-I2C-04 اكتمال التحويل:** بعد كتابة config single-shot، اقرأ OS bit حتى ready=1 ضمن deadline، ثم اقرأ conversion register؛ قارن النتائج بالانتظار الثابت 1300μs وتحقق من الاستجابة لتغير الجهد التناظري.
**HW-I2C-05 تشويش الحمل:** أعِد الاختبار عند تشغيل الريلاي والجهد والحركة، وانتبه إلى انتقال التشويش عبر الأرضي أو مصدر تغذية AO مقابل أخطاء I2C الرقمية.

**مقاييس النجاح:** لا 5V على MCU SDA/SCL، تحقيق rise/fall وVOL المناسب، معدل NACK/timeout متدنٍ وقابل للرصد، تطابق متوسط raw عند 100/400 ضمن تشتت ADC المتوقع، وعدم وجود عينات stale بانتقال MUX أو تغير الجهد. لا يمكن منح PASS نهائي قبل وجود raw bench evidence.

## 6. قرار التنفيذ في ChemCore Next
- اترك ADS_I2C_HZ=400kHz **مؤقتًا دون تغيير** حتى الاختبار؛ لا ننقل معدل الناقل إلى 100kHz أو نغيّر معاملات R0 بناء على فرضية.
- أضف في P02.3/P13 فحوص I2C_NACK، I2C_TIMEOUT، ADC_CONVERSION_NOT_READY، STALE_CONVERSION، ADC_SATURATION، مع توثيق نوع شريحة level shifter وقراءات التغذية ووسم عدم التحقق.
- إذا نجح 100kHz وفشل 400kHz يُستقصى عن pullups/capacitance/rise-time/level shifter قبل تعديل المنطق الكيميائي.
- بيانات التحليل على G1 لا تبرهن سلامة النظام الفيزيائي.

## 7. المصادر الأولية التي فُحصت
1. Texas Instruments, ADS1115 datasheet Rev E, 2024 (timing, data rate, OS-bit, AIN limits): https://www.ti.com/lit/ds/symlink/ads1115.pdf
2. NXP, AN10441 (2007), bidirectional level shifting for I2C: https://cdn-shop.adafruit.com/datasheets/AN10441.pdf
3. Texas Instruments, SLVA689A (October 2026), I2C pull-up resistor calculation, rise-time versus R and C: https://www.ti.com/lit/an/slva689/slva689.pdf
4. Pinned firmware Config.h and Hardware.cpp as identified above.
**Evidence classification:** manufacturer source + static firmware review; hardware timings not measured, no bench acceptance, no firmware modifications in this task.
