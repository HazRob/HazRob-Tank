# بروتوكول الاستيراد وأخذ العينات من قواعد الباحثين — ChemCore Tank V6
**ID:** V6-002-DSP-001 | **version:** 1.0 | **date:** 2026-10-29 | **state:** `PROTOCOL_ONLY / NO_NEW_DATA_DOWNLOADED_OR_TESTED`.

هذا البروتوكول **ليس** ملف نتائج، ولا يفيد بأن تنزيل المصادر المعلّقة قد حدث. يحظر استخدام أي مجموعة لا تُعرف وحدة إشارة الحساس فيها لاستخراج `Rs/R0` أو ppm بصورة مصطنعة. العينة المستقلة هي `exposure/experiment` وليست scalar ADC أو ثانية تسجيل.

## أ — سجل البيانات ومراحل القبول

1. **D0 discover:** فهرس `HazRob/build-harness/library/reference-bank/external-research/master_catalog.json`، حذف DOI/author mirror duplicates، وتثبيت source ID/version وlicense.
2. **D1 raw qualification:** تنزيل من المصدر الأصلي إذا تسمح الرخصة، حفظ raw byte-for-byte مع `SHA256,size,filename,URL,DOI,downloaded_utc,license,authors`؛ المصدر الذي حالة `SOURCE_DESCRIPTION_VERIFIED_RAW_PENDING` يظل `HOLD` حتى تنفذ هذه الخطوة.
3. **D2 semantics:** إثبات `sensor_model,instance,unit,reference_gas,gas_phase_ppm/ppb (if independently measured),carrier,exposure_group,replicate,day,session,configuration,time_s,T,RH`. فرق `prepared liquid % v/v` عن `gas-phase ppm` غير قابل للتحويل بلا قياس أو اتزان موثق.
4. **D3 grouping:** الحفاظ على المشاهد (raw,downsampled,feature export) تحت `parent_exposure_id` واحد؛ جلسات/أيام/أجهزة متطابقة أو حجرة دون reset تُوصل في dependency graph.
5. **D4 selection:** `train/dev -> calibration -> sealed_test` بدون تداخل، مع `seed=20261029` وحفظ قائمة IDs الحقيقية لكل فصل؛ لا إعادة اختيار test بالنظر إلى الأداء.
6. **D5 analysis:** run model baseline B0..B8 تحت نطاق الإشارات الفعلية فقط؛ إخراج uncertainty ومعنى الدور البحثي لكل مجموعة.
7. **D6 evidence:** report كل الاستبعادات، license compliance، علامات `CROSS_PLATFORM_ONLY,RAW_PENDING,LABEL_QUALITY`. صفر نتائج مزعومة إذا لم تصل ملفات خام.

## ب — خطة عينات مرجعية متنوعة ومحددة مسبقًا

| ID | حجم مصدر الباحث | اختيار العينة عند التحقق من RAW | المهمة العلمية | أهم قيد/فصل |
|---|---|---|---|---|
| UCI1081 / MQ-EXT-15 | 90 تعرضًا حقيقيًا، 6 غازات×3 مستويات 50/100/200 **ppb**×5 مكررات | **جميع 90**. المعيار: leave-one-concentration-out 3 طيات كاختبار ضغط؛ مجموعة تطوير مستقلة للموديلات/الضبط. قارن MQ135 (قناة 5) مع 10 قنوات | fingerprints/static/temporal، تمييز 6 غازات منخفضة التركيز | مستوى ppb أقل من الحساسية الاسمية لكثير من MOS؛ ليس معايرة Tank، ولا دمج قنوات TGS إلى MQ |
| UCI270 (يحل محل مضاعفة UCI224) | 13,910 تعرضًا، 16 حسّاسًا TGS، 128 feature، 10 batches عبر الزمن، ست فئات | للتجريب السريع **حد أقصى 30 exposure/غاز/دفعة/فئة تركيز** عند التوفر (reservoir deterministic داخل كل stratum)، مع الإبقاء على الدفعة 10 كاملة للاختبار؛ التدريب في batches 1–6 والتحقق/معايرة batches 7–8، وحجز batches 9–10 مغلقة حيث يصلح تكوين الفئات؛ گزارش per-batch | drift، domain adaptation، unseen concentrations، مجموعة baseline واسعة | فئات لا تظهر في كل دفعة (تولوين مفقود في بعض batches)، لا نملأ المفقود بأمثلة اصطناعية؛ يجب تقويم CI حسب batch |
| UCI308 | 58 تعرضًا أصلية ذات 16 trace/تعرض وتردد موثق | **جميع 58** بعد مطابقة original trace وfeature-file IDs. Group by whole exposure/flow condition؛ hold out group عند كفاية groups | temporal/filter/modulation، extraction، خطأ التكرار | إذا كان الوسم ناتج إعداد محلول لا `ppm` عند الحساس، لا ننشئ ppm |
| UCI309 | 180 تعرضًا، 30 configuration ×6 replicates، 8 TGS، 300s لكل تسجيل | **جميع 180**؛ مثال outer grouped evaluation `leave-one-configuration-out`، وسياسة held-out source pairs تحدد قبل الاختبار. time windows تتبع نفس exposure | mixture/interference، أثر تغير النسبة والاضطراب والزمن، baseline→plume→recovery | concentrations من GC-MS **mean at location** (لا تمثل ppm لحظيًا لكل نقطة زمنية)؛ source raw/downsampled لا تضاعف التعرضات |
| MQ-EXT-03 MultimodalGasData | 6400 ADC samples في mirror سبق فحصه؛ الأصل يحتاج byte comparison | إذا توافرت `session_ids`: اختيار 5 exposures مستقلة أو كل exposures إن كانت قليلة لكل label؛ وإذا لا تتوافر، QC/visualization فقط `NO_INDEPENDENT_TRAIN_TEST` | nonpure odor class، robustness وfeature/no-gas | labels NoGas/Perfume/Smoke/Mixture **ليست جزيئات خالصة**؛ لا نقول 6400 exposure |
| MQ-EXT-10 | 6 حساسات MQ3/MQ4/MQ6، 18.5 Hz، 180 ثانية، Rs kΩ في وصف الناشر | raw pending، بعد التحقق ابدأ بحد أقصى 12 جلسة مستقلة لكل class/level مع حفظ الزجاجة/التعرض كاملاً، split bottle/session | مقارنة Rs، تذبذب baseline، T/RH، زمن الاستجابة | 1,2.5,5,10,15,20% هي نسب **محلول إيثانول سائل**؛ ليست ppm غاز |
| MQ-EXT-11 | 309 قطعة أخشاب موصوفة، 16 قناة بينها سبعة من MQ الثمانية | raw pending، stratified by wood species ثم select ≤15 *pieces* لكل species للتطوير، واترك pieces/جلسات مختلفة للاختبار عند وجود metadata | fingerprint generalization والتعدد | species wood classes، لا `gas identity` ولا benchmark PPM |
| MQ-EXT-12 | 6×MQ3، 4 تجارب نوعية بحسب وصف الناشر | raw pending؛ جميع 4 التجارب فقط، تحليل وصفي/leave-one-experiment-out عند جواز ذلك | تفاوت الوحدات من نوع MQ واحد | n=4 ضعيف؛ لا تدريب deep learning أو claim generalization |
| MQ-EXT-14 | استجابة 8 sensors لـ4 مواد مسماة؛ التفاصيل غير متحققة | raw pending؛ بعد إثبات أنواع الحساسات ووحدات التركيز اختر ≥3 *exposures* مستقلة/مادة إن كانت موجودة؛ وإلا `NEEDS_DATA` | صلاحية انتقال المنحنيات والمقارنات | اسم 8 sensors لا يعني أن MQ8 Tank فعليًا حاضرة |
| MOS-EXT-16 | 45 قناة متنوعة وتزامن SIFT-MS موصوف | raw pending، stratify by VOC campaign/T/RH with held-out campaign | robust regression and environmental correction | لا تنقل coefficients إلى MQ |
| MOS-EXT-17 | 62 MOS، 700 حالة، نحو 12 شهرًا في وصف المصدر | raw pending، حجب الفترة الزمنية المتأخرة بالكامل | drift/model stability | اختلاف العتاد وملفات 141MB يحولها إلى مرحلة لاحقة |
| MOS-EXT-18 | 3 أجهزة SGP40/900 mixture conditions موصوفة | raw pending، holdout entire device and mixture condition after verifying grouping | mixture transfer and device adaptation | لا تثبت MQ8 array performance |

**تفسير اختيار 30 أو 12 أو 15:** سقوف للحمل التجريبي الأولي، ليست حصة واجبة أو دليل كفاية إحصائية. الرقم الحقيقي يعتمد على stratification وتوفر ملفات الخام، ويُسجل التوزيع المنفذ والخلايا الخالية قبل أي تدريب. لا تسحب عينات من `sealed_test` لمجرد موازنة العينة التدريبية.

## ج — تصميم المقارنات بحسب طبيعة المصدر

**UCI1081**: `E0` صنّف باستخدام MQ135 فقط، `E1` جميع القنوات الـ10، `E2` مفرد + temporal features، `E3` one withheld concentration، `E4` abstention؛ لا تمرر اسم الغاز الحقيقي كميزة QSPR أثناء اختبار هوية مجهولة. لا يمكن جعل 90 exposure دليلًا على 90 غازًا مستقلًا زمانيًا إذا جمعها يوم/دفعة غير موثقة؛ اذكر القيد.

**UCI270**: أربع سياسات `train early -> test late`, `leave-one-batch-out`, `leave-one-concentration-level-out` عند توافر عينة كافية, `source-author baseline`. لا تقارن accuracy من نموذج علم عدد القنوات/الفئات/الأيام مختلف دون توحيد المهمة.

**UCI308**: timing features مقابل baseline، منع تسرب دورة تدفق واحدة إذا ارتبطت بسجل آخر؛ لا تقيس ppm إلا بمعيار غاز موجود أصلًا.

**UCI309**: خصائص high/low time windows، مقارنة pure vs binary mixtures حيث توفر configurations الصفرية، كشف CO/CH4 مع ethylene، تقسيم whole configuration، وتسجيل أن GCMS يعطي mean gas concentration نهائيًا لا trace لحظي.

**MQ**: استبعاد source rows ذات وحدات غير ثابتة من `forward-ppm`، والسماح لها باختبارات `signal preprocessing` أو `qualitative classification` مع label مضبوط. لا تطابق `MQ135` الباحث مع MQ135 المركب دون cross-device transfer gate.

## د — عينة الإحصاء/مخاطر التسرب

- سجل كل `root_exposure_id`, `independence_group`, `source_group_id`, `raw_sha`, `time_window_parent`, `device_id`, `session_id`, `day_id`, `preparation_batch`, `gas_label`, `level_original_unit`, `label_confidence`, `sampling_weight`, `split_role`.
- اختيار `seed=20261029`، ثم `sampling_algorithm_version` ثابت (مثلاً stable sort by SHA256(stratum+parent_exposure_id), pick k)، **وليس** الاعتماد على ترتيب ملفات نظام التشغيل.
- تحقق آلة: لا تداخل `exposure_id` أو `connected dependency component` بين أي قسمين؛ لا اختلاف وحدات بين sample وmodel؛ عدم تدريب scaler/feature selector/threshold/OOD/calibration على sealed_test.
- كل source يسجل full original counts، eligible counts، exclusions، independent groups، split imbalance و`effective_sample_size`؛ عدم وجود session metadata يحد من claim ويستدعي وضع `GROUPING_UNVERIFIED`.
- Monte Carlo/synthetic ليست عينة من بيانات الباحثين حتى لو استُعملت منحنيات الباحثين لإنشائها.

## هـ — الملفَّات التي يجب أن يصدرها التنفيذ لا التخطيط

```
research/chemcore_v6/
  manifests/sources.json                    # source DOI, SHA256, license, raw-state
  manifests/source_inventory.csv
  manifests/sampling_manifest.csv            # exact selected exposure IDs + group + seed
  manifests/split_manifest.csv               # development/calibration/sealed
  manifests/raw_integrity_audit.json
  manifests/label_and_unit_audit.csv
  manifests/duplicate_dependency_graph.json
  manifests/blocked_sources.csv
  adapters/<dataset_id>_adapter.py
  tests/test_adapter_<dataset_id>.py
  results/<dataset_id>/b0_v5_baseline.json
  results/<dataset_id>/ablation_matrix.csv
  results/<dataset_id>/predictions.csv
  results/<dataset_id>/metrics_and_ci.json
  results/<dataset_id>/failure_cases.md
```

**الأصول الكبيرة تحفظ خارج git إذا تطلب الترخيص والحجم** مع مراجع immutable وSHA؛ لا تنشر أجهزة أو هوية عامل أو معرّفات حساسة بالخطأ. ملفات النتائج وحدها لا تكفي دون lineage.

## و — بوابات وقف/قبول التحليل

- `RAW_PENDING` أو `LICENSE_UNKNOWN`: لا benchmark عددي على source.
- `UNITS_UNVERIFIED`: allowed class/temporal QC فقط إن كانت أصلاً كافية؛ `PPM_BLOCKED`.
- `GROUPING_UNVERIFIED`: لا claim تعميم ولا independent accuracy؛ exploratory only.
- `EXTERNAL_ARRAY_ONLY`: يسمح باختبار الخوارزمية؛ لا `Tank device calibrated`.
- `SOURCE_QUALIFIED`: root bytes + label + unit + grouping + hash + license + detector family.
- `BENCHMARK_PASS`: split frozen, paired baseline & statistical audit، أخطاء لكل فئة، negative outcomes مثبتة.
- `G3_TANK`: لا تتغير من الصفر حتى تصل سجلات تعرضات Tank بمعيار غاز مستقل حسب بروتوكول 26 أكتوبر.

## ز — مصادر خارجية أصيلة

[UCI1081](https://archive.ics.uci.edu/dataset/1081/gas+sensor+array+low-concentration)؛ [UCI270](https://archive.ics.uci.edu/dataset/270/gas+sensor+array+drift+dataset+at+different+concentrations)؛ [UCI308](https://archive.ics.uci.edu/dataset/308/gas+sensor+array+under+flow+modulation)؛ [UCI309](https://archive.ics.uci.edu/dataset/309/gas+sensor+array+exposed+to+turbulent+gas+mixtures.)؛ [MQ-EXT-10](https://data.mendeley.com/datasets/vpc887d53s/3)؛ [MQ-EXT-11](https://data.mendeley.com/datasets/gvwr8hmv2t/2)؛ [MQ-EXT-12](https://data.mendeley.com/datasets/brwptm8wjb/1)؛ [MQ-EXT-14](https://data.mendeley.com/datasets/78y8kv6hz7/1)؛ [MOS-EXT-16](https://data.mendeley.com/datasets/9v3nywn7hy/1)؛ [MOS-EXT-17](https://zenodo.org/records/15681119)؛ [MOS-EXT-18](https://zenodo.org/records/6821340).

**Current state:** `SAMPLING_DESIGN_COMPLETE; NEW_FILES_DOWNLOADED=0; NEW_SAMPLED_EXPOSURES=0; BENCHMARKS_EXECUTED=0`.
