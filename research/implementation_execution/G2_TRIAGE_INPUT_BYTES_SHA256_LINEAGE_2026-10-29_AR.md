# G2 — ربط تقرير اقتناء الأدلة بالبايتات الأصلية لمدخلات إعادة الاحتساب (29 أكتوبر 2026)

## نطاق التنفيذ
- المستودع: `HazRob/HazRob-Tank`؛ فرع البحث فقط: `research/chemcore80-central-evidence-gap-analysis-20261021`.
- نقطة الاستئناف: `21392e3e654203815453d569d47c6b6e28598fe9`.
- مكتبة المراجع المركزية: `HazRob/build-harness/library/reference-bank/external-research/master_catalog.json`، Git blob SHA1: `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f`؛ 80 مرجعاً منها 14 مصدر بيانات خارجية.
- مصفوفة الجاهزية المثبتة Git blob SHA1: `bd69304fc35175b90199c6cc6ad4a88fc10ae93f`؛ المؤهل للمراجعة 0/14؛ سجل الأدلة المستقلة `records: []`.

## الفجوة والإصلاح
كانت بوابة `g2_acquisition_triage.py` تعيد حساب نتائج التدقيق من السجل المتحكم، ولكن تقرير الفرز الناتج لم يتضمن تجزئات المدخلات الفعلية. وبذلك يصعب ربط artifact ببايتات الملف المعين الذي أُعيد تشغيله، حتى إذا كانت قيم JSON المنطقية متطابقة.

أضيفت مجموعة `input_sha256` إلى مخرج `build_triage` تضم تجزئة SHA256 **لبايتات الملفات كما هي**، لا JSON المعاد تسلسله: `audit`، `source_matrix`، `ledger`، `independent_evidence`، `canonical_registry`. تحسب التجزئات قبل إعادة التشغيل وبعده، وتُحجب النتيجة بخطأ `G2_TRIAGE_INPUT_MUTATED_DURING_REPLAY` إذا تغير أي مدخل أثناء العملية. يبقى التحقق من Git blob للمصفوفة المثبتة، وإعادة احتساب جميع تقنيات G2، والمطابقة الكاملة للتقرير شرطاً سابقاً للنشر.

## ضوابط الانحدار وإعادة الإنتاج
أضيف اختباران: (1) إثبات تطابق تجزئات المدخلات مع `hashlib.sha256(path.read_bytes())` وأن تغيير المسافات فقط في ملف التدقيق يغير بصمة الملف دون تغيير القرار العلمي؛ (2) تعديل ملف التدقيق أثناء إعادة التشغيل بصورة مضبوطة للتحقق من رفض النتيجة. البيانات الاصطناعية هنا **اختبارات سلامة برمجية فقط**.

```bash
python -m unittest discover -s research/implementation_execution -p 'test_g2_*.py' -v
python research/implementation_execution/g2_evidence_audit.py --output /tmp/g2_audit_results.json
python research/implementation_execution/g2_acquisition_triage.py \
  --audit /tmp/g2_audit_results.json \
  --source-matrix research/implementation_execution/G2_CENTRAL_CATALOG_SOURCE_READINESS_2026-10-28.json \
  --output /tmp/g2_acquisition_triage.json
```

## تحقق CI
- التزام التطوير: `b6d29651c48aced93b0dda0ebc9ab46c28490be4`، وتصحيح تمثيل نهاية السطر في اختبار الانحدار: `f0bf6f3b1fcc9f161879cb4527bbb48746ab5015`.
- GitHub Actions G2: https://github.com/HazRob/HazRob-Tank/actions/runs/40786907512 ؛ job `123162275605`؛ **285/285 PASS**؛ `g2_pass_candidates=0`، `g2_blocked=80`، `source_blocked=14`.
- Artifact: `12859467844`؛ SHA256 للحزمة `a3831c2620851211b51b197108b8920045f7c9d2ea3d799611e6cb55c220526c`.
- مقارنة التزام التطوير بنقطة الاستئناف: ملفا Python فقط ضمن `research/implementation_execution/`. لا تغييرات على ChemCore Stable أو التطبيق أو الفريموير.

## حدود الدليل
تجزئات SHA256 تثبت تطابق البايتات ولا تثبت صدق البيانات أو أصالتها الفيزيائية أو صحة القياس؛ كما أن فحص تغير المدخلات أثناء إعادة التشغيل ليس بديلاً عن سلسلة حيازة مستقلة أو توقيع موثوق. لا اعتماد G2 أو G3. ما تزال القياسات الفيزيائية المستقلة الأصلية، ومعايير التركيز القابلة للتتبع، ومعرّفات الأجهزة والجلسات والتعرضات، والتقسيمات المحجوبة المثبتة مسبقاً غير متوفرة. **الخطوة التالية:** فحص جميع تشغيلات CI على الالتزام التوثيقي ثم متابعة اقتناء الأدلة الخارجية دون ترقية علمية.
