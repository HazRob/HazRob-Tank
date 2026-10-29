# HazRob Tank V6 — ChemCore checkpoint 02: lineage consensus, G2 records and SIM-B

**التاريخ:** 2026-10-29 · **المشروع:** `HazRob/HazRob-Tank` · **الفرع:** `feature/tank-v6-implementation` · **نطاق العمل:** `V6-002 P01/P04/P05/P08`.  
**الحالة:** `IMPLEMENTED_AND_CI_PASS_RESEARCH_COMPONENTS / NOT_WIRED_TO_ANDROID_DECISIONS / G3=0`.

## 1. لماذا جرى التطوير

محرك V5 يوفّر `InverseMolecularSolver` وتوثيق `ChemCoreEvidenceObservation` و`ChemCoreEvidenceFusion` ويختار مصدرًا واحدًا لكل حساس من مجموعة المنحنيات البديلة. يتطلب V6 فصل مفهومين:
- `physicalSensorBreadth`: عدد الحساسات الفيزيائية المستقلة، وليس عدد منحنيات المصنع/الدراسات.
- `independentSourceRoots`: أصول الأدلة البحثية، وليس عدد المشتقات من نفس المصدر أو البيانات.

**المكوّن الجديد:** `android/app/src/main/java/com/servoc/app/chemcore/TankV6EvidenceConsensus.java`، commit `0eb56670339d93057c27ec3a96f1e8affe39c76c`، يحصر مساهمة المرشح لكل حساس في قيمة واحدة ويكشف تعارض المنحنيات المستقلة عند `|match_A-match_B|>0.35`. يحسب `matchIndex` نسبيًا ولا يقدمه احتمالًا أو ثقة معايرة، ولا ينتج ppm. ينتج إحدى `INSUFFICIENT_EVIDENCE, SOURCE_CONFLICT, AMBIGUOUS, UNKNOWN_OR_OUTSIDE_RESEARCH_DOMAIN, RESEARCH_SCREENING`.

**تنبيه ضروري:** معاملات قبول `MIN_MATCH=0.55` و`MIN_MARGIN=0.10` و`MIN_SENSOR_BREADTH=2` **قيم تصميمية اختبارية فقط**. لا تثبت الحساسية/النوعية، ويمنع استخدامها كحدود هوية معتمدة دون بيانات مجمعة وتهيئة عتبة مستقلة.

اختبارات Java الخاصة به: `research/chemcore_v6/tests/java/TankV6EvidenceConsensusTest.java`، commit `50f70c124f1c03c01f303dba26e61b0539026a77`. **33 assertion PASS**، تتضمن: تكرار نفس منحنى الحساس لا يغير النتيجة؛ تعدد منحنيات حساس واحد لا يرفع breadth؛ تعارض المصدر يؤدي إلى الامتناع؛ هامش التعادل، البيانات غير الموثقة أو خارج المجال، عدم إمكان إسناد M4، وحتمية ترتيب المرشحين.

**لا يزال المكون مستقلًا عن `TankInstantAnalyzer` و`CompletedSessionChemCoreAnalyzer`.** دمجه في مسار تشغيل V6 يتطلب قرار ADR وتحليل مصدر root لكل نموذج وCI/Android regression. لا تُستبدل نتائج V5.

## 2. إعادة تدقيق سجلات G2 من UCI270/UCI308/UCI309

نُقلت نتائج مستقلة **على مستوى سجلات GitHub تاريخية، لا RAW datasets** مع مطابقة أصل الـgit blob:

| السجل التاريخي | Git blob | العد الأصلي المستقل حسب المصدر |
|---|---|---:|
| `uci270_g2_batch04_2026-10-19.json` | `88d13ae28468472631085f1136147cdbae0f3d8a` | 13,910 تعرضًا |
| `uci308_g2_batch05_2026-10-20.json` | `f3b8e9baa3b098c0eb969d52c5e9e95a06ccaf49` | 58 تعرضًا |
| `uci309_g2_batch06_2026-10-20.json` | `94ac012cfdbc149077472819d1cb25ca4df31134` | 180 تعرضًا |
| `uci_trained_models_main_2026-10-21.json` | `94aa5749add50884bd9658f2274377d1f8d46514` | نتائج نماذج على المجموعات نفسها **وليست تجارب إضافية** |

بالإضافة إلى UCI1081 (90 تعرضًا) يصبح الإجمالي التاريخي للمصادر الخارجية الأربع 14,238 تعرضًا. لا تحتسب UCI224 بوصفها مجموعة مختلفة عن UCI270.

تم تنفيذ `research/chemcore_v6/g2_portfolio_audit.py`، commit `f49a368418575e48932a90f1e4623c2fab858c4d`، و8 اختبارات المصدر `research/chemcore_v6/tests/test_g2_portfolio_audit.py`، commit `f54b0f2ec81d978735ef98ec4f2d2a7910a1807e`.

**أثناء التنفيذ:** أول تشغيل اختبارات كشف أن `scientific_Tank_G3_validated` في سجل UCI309 مكتوب `0` عدديًا وليس `false` منطقيًا، فتم التصحيح في `70bfaf86ee787cce5c1f738e08265a3996dfb26a` للسماح بالصفر الصريح فقط. تشغيل CI اللاحق ناجح. 
**حد القبول:** تظل الأرشيفات الخام لـ UCI270/308/309 غير معاد تشغيلها في V6 الحالي؛ لا تتحول نتيجة source-hash الصحيحة للسجلات إلى تأهيل بيانات أو G3.

## 3. محاكي SIM-B الفعلي

`research/chemcore_v6/temporal_simulation.py`، commit `c5a05072f84a72e756456a885e16374f08173bd7`، ينشئ منحنيات زمنية افتراضية 211 نقطة/حساس مع وسم `synthetic=true`، ومصدر ضوضاء مشتركة وفق seed ثابت. أضيفت 12 اختبارات Python `research/chemcore_v6/tests/test_temporal_simulation.py`، commit `6a9671703a19b13bab91726d1013fcd18ad68b55`.

**السيناريوهات الثمانية المنفذة (كلها SYNTHETIC ASSUMED وليست قياسات الباحثين أو Tank):**

| Scenario | الاختبار | نتيجة مرجعية من CI |
|---|---|---|
| SIM-B01 | رفع RH افتراضيًا 20 نقطة مئوية | تغير `log10(Rs/R0)` بـ +0.040000 |
| SIM-B02 | 30 يوم انجراف مفترض | تغير +0.021000 |
| SIM-B03 | ارتفاع/تعافٍ من نموذج حركي أولي | baseline 0، exposure -0.409176921، recovery -0.125404389 |
| SIM-B04 | طوابع زمنية معكوسة/مكررة | رفض الحالتين |
| SIM-B05 | MQ4 وMQ9 مفقودان | بقيت 6 قنوات فقط، دون استيفاء اصطناعي |
| SIM-B06 | ضوضاء مشتركة | معامل مشترك مفترض 0.80، seed 20261029 |
| SIM-B08 | تداخل غير جمعي مفترض | فرق `log10` +0.0764363 |
| SIM-B09 | تأثير متداخل مفترض | فرق `log10` +0.0957724 |

لا تدعم هذه الأرقام تقدير ppm أو المعايرة؛ الغرض كشف أخطاء pipeline ومعرفة حساسية النتيجة الافتراضية للافتراضات. لم ينفذ SIM-B07/SIM-B10..13 أو التحقق G3. يوجد **31 سيناريو** في الخطة؛ نفذ حتى هذه النقطة ثمانية SIM-B إضافة إلى تجارب SIM-A للمنحنيات وR0، مع بقاء معظم شروط الخطط المرجعية المفتوحة.

## 4. إثبات GitHub Actions

**أحدث نتيجة ناجحة:** [Run 40916864090](https://github.com/HazRob/HazRob-Tank/actions/runs/40916864090)، commit `3f81daa2913c3c679e1c8327d6fd188b183ca1f7`.
- Python `unittest`: **50/50 PASS**.
- Java/JDK: `TankV6SourceCompatibilityTest` **31 assertions PASS** و`TankV6EvidenceConsensusTest` **33 assertions PASS**.
- منحنيات 43×9: **387** اختبار roundtrip عددي ناجح، لا يثبت دقة الحساس.
- R0 افتراضي: **172** حالة، منها **94 خارج المجال**، ما أدى إلى رفض الاستقراء.
- المصدر: 14 أصل HR069 + 3 سجلات peer R0 + 4 سجلات G2 تاريخية مسجلة ببصمات Git. 
- CI artifact: `chemcore-v6-source-research-audit`، artifact ID `12915184303`. يحتوي ملخصات `source_audit.json`, `synthetic_r0_sensitivity.json`, `peer_r0_audit.json`, `g2_portfolio_audit.json`, `temporal_sim_b.json`، وليس ملفات RAW.

## 5. ما لم يكتمل بعد

- **P00:** اختبار مرجعي V5 على هاتف/روبوت وتحليل تقارير ومهاجرات قواعد البيانات (خارج هذا العمل).
- **P01/P05:** أرشيفات UCI270/308/309 الخام، مزيد من مصادر MQ وأرشيفات manufacturer، ترخيص كل مصدر وحجب الأيام/الأجهزة.
- **P02:** قياس RL/VC/ADC والسخان الفعلي لكل حساس Tank وتوثيق حالة معايرته الحقيقية.
- **P03:** إنشاء Facade يدعم contract وروابط الحساس والبيانات الخام ويحافظ على V5.
- **P06–P08:** اختيار البصمات/المنحنيات المؤهلة، وعكس متعدد الفرضيات، ودمج الدليل بعد التحقق في البيانات الحقيقية؛ P08 الحالي **prototype only**.
- **P09:** ربط Android فعليًا، بناء APK واختبارات جلسات وقياسات وتقارير وتحكم.
- **P10:** G3 الفعلي **0/80** ولا يرقى عبر UCI أو simulation.

## 6. إعادة التنفيذ والاستئناف

1. ابدأ من `feature/tank-v6-implementation` ثم اقرأ `docs/implementation/V6-CHEMCORE-EXECUTION-TRACKER.json`، هذا التقرير، والخطة `V6-CHEMCORE-SCIENTIFIC-MASTER-PLAN-2026-10-29_AR.md`.
2. تحقق من تشغيل `40916864090` ونتيجة 50 Python و64 Java assertions، ومن أرقام git blobs.
3. `python -m unittest discover -s research/chemcore_v6/tests -p 'test_*.py' -v` ثم `python research/chemcore_v6/temporal_simulation.py --out /tmp/sim-b.json` ثم `python research/chemcore_v6/g2_portfolio_audit.py --out /tmp/g2.json`.
4. لا تستبدل نتائج التطبيق القديم أو Stable بما سبق. الخطوة التالية P03/P06: تعريف ودمج بيانات provenance من `ChemCoreEvidenceObservation` إلى V6 بشكل guarded shadow مع مراجعة اختبارات Android، ثم تقييم آلية ترتيب المرشحين على datasets ذات حجب مستقل.
5. تحديث tracker والسجل بعد **كل commit/CI**؛ لا يُعد P08 أو P09 مغلقًا قبل الاختبارات الشاملة.

**checkpoint status:** `50_PYTHON_PASS / 64_JVM_ASSERTIONS_PASS / G2_HISTORIC_REPORTS_4_OF_4 / SIM_B_EIGHT_SCENARIOS / G3=0/80 / ANDROID_V6_INFERENCE_NOT_REPLACED`.
