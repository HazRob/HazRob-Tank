# G2 — قفل هوية مكتبة المراجع المركزية وسياسة عدم الاعتماد (2026-10-28)

## النطاق
تحسين بحثي محصور في تدقيق أهلية مصادر القياسات الخارجية، ولا يمثل تجربة فيزيائية أو اعتماداً علمياً لأي تقنية من تقنيات ChemCore الثمانين. لم يُمس ChemCore Stable أو تطبيق Android أو الفريموير.

## الثغرة
كان `audit_source_catalog` يتحقق من العدد والمعرّفات والفئات وأهلية البيانات الخام، ولكنه لا يتحقق من حقول `schema_version` و`authority` و`scope` أو قواعد السلامة في `qualification_policy`. وبالتالي، عند إصدار فهرس جديد بتجزئة Git blob جديدة ومثبتة، يمكن تغيير سلطة الفهرس أو إسقاط حواجز تفسير البيانات دون رفض المخطط، مع استمرار نجاح اختبارات عدد المدخلات.

## المعالجة
أُضيفت قيم متوقعة صريحة مستمدة من الملف الأصلي في `HazRob/build-harness/library/reference-bank/external-research/master_catalog.json` (Git blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f`):
- `schema_version=hazrob-unified-reference-bank-1.0`.
- `authority=HazRob/build-harness/library/reference-bank/external-research/master_catalog.json`.
- `scope=SHARED_BIBLIOGRAPHY_AND_RESEARCH_DATA_DISCOVERY_FOR_ALL_HAZROB_PROJECTS`.
- يجب أن تكون قيم قواعد السلامة الخمس `True` المنطقية تحديداً: `raw_data_is_not_sensor_calibration` و`library_inclusion_does_not_promote_to_Tank_G3_or_HR069_M4` و`gas_label_not_equal_ppm_reference` و`raw_timeseries_not_independent_exposures` و`source_status_must_be_preserved`.

عند فقد أي حقل أو تغيره تُرفع `CENTRAL_CATALOG_AUTHORITY_OR_SAFETY_POLICY_MISMATCH`. لا تقبل قيم `1` أو `"true"` بديلاً عن `True`. يظل قفل Git blob الأصلي مستقلاً وسابقاً للتحقق من هذه السياسة.

## اختبارات الانحدار
أُضيفت قيم الهوية والسياسة إلى قالب الفهرس الاصطناعي الإيجابي، وأضيف `test_central_catalog_authority_and_noncertification_policy_are_locked` للتحقق من قبول المخطط الصحيح ورفض التغييرات في الهوية وقواعد السلامة والحقول المفقودة أو ذات الأنواع غير الصحيحة. الاختبارات الاصطناعية لا تُحتسب أدلة قياس.

## إعادة الإنتاج
```bash
cd research/implementation_execution
python -m unittest -v test_g2_evidence_bundle_gate
python -m unittest discover -p 'test_*.py'
```

الفرع: `research/chemcore80-central-evidence-gap-analysis-20261021`. الالتزام البرمجي: `a51ffe51b9b9e8fc96a7c8abc10e2cf37b80ebb3`. لا يُعد نجاح CI مثبتاً إلا بعد فحص تشغيل GitHub Actions على هذا الالتزام أو التزام توثيقي لاحق.

## حد الأدلة العلمية
سجل الأدلة المستقلة `G2_INDEPENDENT_EVIDENCE.json` ما زال `records: []` عند المراجعة، ومصفوفة المصادر المثبتة `0/14` جاهزة للمراجعة و`14/14` محجوبة. لا يمكن استنتاج ppm أو هوية الغاز أو استقلال التعرضات من قفل المخطط أو تجزئة المصدر. تبقى نتيجة التأهيل العلمي `0/80` مرشحين ناجحين و`80/80` محجوبين، إلى حين وصول قياسات أصلية مستقلة ذات مراجع تركيز ومعرّفات أجهزة وجلسات وتعرضات وتقسيم اختبار محجوب.
