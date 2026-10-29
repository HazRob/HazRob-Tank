# ChemCore Tank V6 — نقطة استئناف 07: الدفعة العلمية الثانية (10 تقنيات جديدة)

**التاريخ:** 2026-10-29 · **الفرع:** `feature/tank-v6-implementation` · **النطاق:** محرك ChemCore الجديد المستقل للغازات فقط، `com.hazrob.tank.chemcore.v6`. يُمنع إلحاق منهج استدلال V6 بمحرك سابق أو إضافة أي بصمة أغذية. تقنية بدء الإحماء/Bosch مؤجلة بقرار المستخدم.

## 1. النتيجة المعتمدة

**34/80 تقنية لها تنفيذ جزئي في نواة V6؛ 46 تقنية لم تُنقل؛ تقنيات معتمدة ميدانيًا على Tank G3 = 0/80.** 10 تقنيات جديدة أُضيفت في هذه الدفعة، ونجح اختبارها. لا يعني `PARTIAL` اكتمال الدمج بالواجهات أو تحقيق معيار أداء ميداني.

| الرمز | التقنية | التنفيذ الجديد وحدوده |
|---|---|---|
| `01.5` | Environmental correction | `CalibrationResearchV6.correctEnvironment`: تطبيق نموذج `log10(Rs/R0)` مع `betaT` و`betaRH` مُمرَّرين صراحةً من مصدر الدراسة؛ عند غياب أي معامل لا تُختلق قيمة تعويض |
| `03.1` | PCA | `ClassicalLearningV6.pca`: تركيب مصفوفة التغاير من عينات التدريب فقط، واستخراج المحاور بواسطة power iteration والتعامد؛ لا تعادل التصنيف ولا تستخدم بيانات الاختبار |
| `03.4` | LDA/PLS-DA | `ClassicalLearningV6.fitDiagonalLDA`: تنفيذ **LDA قطري** مع تغاير مجمّع من التدريب، انتظام ridge واحتمالات أولية متساوية. **PLS-DA غير منفذة بعد**؛ حالة التقنية جزئية |
| `04.3` | KNN | `ClassicalLearningV6.fitKnn`: تصحيح المقاييس بمعايير مجموعة التدريب، ثم مسافات إقليدية وأصوات `k` مع الإبقاء على جميع الأصناف |
| `04.4` | Logistic regression | `ClassicalLearningV6.fitSoftmax`: تدريب `multiclass softmax` بالتدرّج الدفعي وL2، مع استخدام مقاييس التدريب فقط؛ درجات softmax **غير معايرة احتماليًا** على Tank |
| `05.1` | Single-sensor calibration | `singlePoint`: نقطة تركيز مرجعي معلومة للحساس الواحد **مع أسّ حساسية خارجي معلوم**؛ لا يمكن اشتقاق أسّ منحنى من نقطة منفردة |
| `05.2` | Multipoint calibration | `multipoint`: انحدار `log10(Rs/R0)` على `log10(ppm)` لعينات مصدرية متعددة، ومعاملات `a,b` وبواقي RMSE ومجال تراكيز المصدر |
| `05.7` | Identity-conditional estimates | `conditionalEstimates`: لكل مادة مرجعية جميع منحنيات التركيز ومجال أدنى/وسيط/أعلى دون افتراض إثبات الهوية |
| `05.8` | LOD/LOQ | `blankBasedLimits`: انحراف معياري تجريبي للـblank + حساسية مرجعية لإخراج `3.3 sigma/slope`, `10 sigma/slope`، **تقدير بحثي لا اعتماد مختبر/معيار دولي** |
| `06.7` | Evidence conflicts | `EvidenceConflictV6.compare`: إظهار كل زوج من منحنيات المادة نفسها، اختلافات `log10(PPM)` ونسب التركيز، مع تحديد مصدرَي المنحنى والحساس الفيزيائي دون حجب الفرضيات |

## 2. الملفات المنفذة

- `android/app/src/main/java/com/hazrob/tank/chemcore/v6/CalibrationResearchV6.java` — commit `db6c3b2fd64ed81ff4c524dfc3a18758965c65dc`.
- `android/app/src/main/java/com/hazrob/tank/chemcore/v6/ClassicalLearningV6.java` — commit `93e46d90dec3afabb29643325acc86f4fb8c20e3`.
- `android/app/src/main/java/com/hazrob/tank/chemcore/v6/EvidenceConflictV6.java` — commit `3da5d747b3a27f49f410f170a9a09951ee3dbaa4`.
- `research/chemcore_v6/tests/java/V6SecondTenTechniquesTest.java` — commit `d2b7d602b06ac02de6d68aa6398c91f679ffc5f5`.
- `ChemCoreTankV6.java` — ربط اختلاف المنحنيات في `Result.sourceCurveDisagreements`، commit `c2ef02e32a217f735e9817297980c54ed36ef631`.
- `TankV6TelemetryAdapter.java` — تصدير جميع المقارنات عبر `source_curve_disagreements` ضمن JSON المستقل V6، commit `4eb63a24ef85c8f8c2e691b67f4dfb436eb5d8f2`.
- `ChemCoreTankV6CoordinatorTest.java` — تحققات إضافية من عدم الحجب عند اختلاف المصدر، commit `1605ed0e12cb3b9eaf4003af0ca30b2f3bd98693`.

### التحقق البرمجي الفعلي

| مجال التحقق | حالة |
|---|---:|
| [CI النواة 40941942329](https://github.com/HazRob/HazRob-Tank/actions/runs/40941942329) | **success** |
| دقة نقل المنحنيات وقياسات Response | 60 تأكيدًا |
| مطابقة بصمات الغازات في فضاءات المصدر المستقلة | 1,245 تأكيدًا |
| منسق الأدلة بعد ربط تعارض المصادر | 58 تأكيدًا |
| تقنيات معالجة الإشارة السابقة | 30 تأكيدًا |
| الدفعة الأولى من عشر تقنيات | 392 تأكيدًا |
| **هذه الدفعة: عشر تقنيات إضافية** | **101 تأكيد** |
| **مجموع تأكيدات Java في التشغيل** | **1,886 نجاحًا** |
| [تجميع Android 40941913288](https://github.com/HazRob/HazRob-Tank/actions/runs/40941913288) | **success — compile-only** |
| [CI المراجع 40941942306](https://github.com/HazRob/HazRob-Tank/actions/runs/40941942306) | **success** |
| التحقق على هاتف Android 12 وESP32/غاز مرجعي | **لم يُنفذ** |
| APK V6 قابل للتثبيت في هذه الدفعة | **لم يُبنَ** |

التجارب العددية هنا **اختبارات وحدة على بيانات اصطناعية واضحة المواصفات**؛ نجاح اختبار ذاتي لمعادلة معايرة أو train/evaluate على الأمثلة نفسها لا يثبت دقة التعرف على الغازات في الواقع. لا توجد بيانات تدريب فعلية ميدانية لـTank ولا نتائج ميكروبية/غذائية في هذا المحرك.

## 3. مصدر UCI1081 المصحح

**تظل نتائج checkpoint 06 هي المرجع العلمي الوحيد لأداء UCI1081:** صُحح ترتيب المصدر الخام إلى `reshape(10,900).T`، ونسخت الدرجات السابقة الباطلة. مخرجات `uci1081_corrected_sensor_blocked_benchmark_2026-10-29.json` تحفظ النتائج المصححة. لم تُعد هذه الدفعة تدريب أصناف Java الجديدة على التعرضات التسعين؛ لذلك لا تُنسب إليها دقة 28.89–78.89% (الأرقام خاصة بنموذجي sklearn وتكوين المصدر السابق). ينبغي إجراء تحقق مستقل للنماذج الجديدة على تقسيم تعرضات حقيقي وحجب تركيز كامل قبل المقارنة.

## 4. المطلوب بعد هذه الدفعة

1. **V6-N03-C:** الدفعة التالية من عشر تقنيات لم تُنقل، حسب جدول التتبع الأصلي، مع تنفيذ رياضي فعلي وJDK tests واختيار مصادر مستقلة. يجب تمييز تقنية LDA/PLS-DA الحالية: LDA الجزئي فقط، والتخطيط لـPLS-DA لاحقًا.
2. **V6-N05:** إعادة اختبار نماذج PCA/KNN/LDA/softmax Java الجديدة على سمات UCI1081 الصحيحة وholdouts مُحكمة، واختبار اختلاف عدد الحساسات أو تهيئة الجهاز.
3. **V6-N06:** إتاحة نواة V6 الجديدة وحدها في واجهة التحليل ومحرّك تقارير الجلسات والقياس الآني؛ لا تستخدم شيفرة استدلال المحرك السابق، ولا تدمج أي مصدر بيانات خاص بالأغذية.
4. **V6-N07:** تحقق هاتف/تقارير/جلسات، ثم APK V6 وتجربة مرجعية فيزيائية تحقق G3. لا تسمح ادعاءات ppm أو هوية غاز مؤكدة قبل ذلك.

**نقطة الاستئناف:** `V6-N03-B_DONE_JDK_PASS / 34_PARTIAL / 46_NOT_PORTED / 1886_ASSERTIONS / ANDROID_COMPILE_PASS / FOOD_ISOLATED / NO_APK / G3=0`.
