# G2 — بوابة منع تكرار العينة الفيزيائية بين مجموعات الاختبار (30 أكتوبر 2026)

## النطاق ونقطة الاستئناف
- الفرع: `research/chemcore80-central-evidence-gap-analysis-20261021`، ونقطة الانطلاق `cb434a401a6c990fa2a2bde35d7a32df3dddb267`.
- فهرس المراجع المركزي المثبت: `HazRob/build-harness/library/reference-bank/external-research/master_catalog.json`، Git blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f`؛ 80 مدخلاً، بينها 14 مجموعة بيانات خارجية.
- المصفوفة المثبتة: 0/14 مصدراً جاهزاً للمراجعة العلمية. سجل الأدلة المستقلة `G2_INDEPENDENT_EVIDENCE.json` يحتوي `records=[]`. هذه الأعداد لا تعني فشل أداء التقنيات بل عدم توافر الأدلة المؤهلة.

## المشكلة والضبط
قبل هذا التعديل كان `g2_paired_prediction_replay.py` يرفض تكرار `exposure_id` ويتحقق من `group_id`، لكنه لم يكتشف أن تعرضين مختلفين بالمعرّف قد ينتميان **للعينة الفيزيائية نفسها** الموزعة بين مجموعتين معلنتين مستقلتين. هذه الحالة قد تتيح تسرّباً بين مجموعات التقييم أو احتساب قراءات مكررة على أنها عينات مستقلة.

أُضيف عمود اختياري `physical_sample_id` في ملف `predictions_blind.csv`. عند وجود العمود:
1. يجب أن تكون **كل** قيمة غير فارغة ومعرّفاً معيارياً مقبولاً وفق مدقق المعرّفات الفيزيائية المستخدم أصلاً؛ يُرفض الإدخال غير المعياري برمز `PAIRED_REPLAY_PHYSICAL_SAMPLE_ID_INVALID`.
2. يُسمح بقراءات متعددة للعينة داخل **المجموعة نفسها**، وتُحصى بوصفها قراءات مكررة، لا عينات مستقلة إضافية.
3. يُرفض إسناد العينة نفسها إلى مجموعتين مختلفتين برمز `PAIRED_REPLAY_PHYSICAL_SAMPLE_GROUP_LEAKAGE`.
4. تُسجّل حقول `physical_sample_group_consistency_checked` و`distinct_declared_physical_samples` و`repeated_exposure_rows_within_declared_physical_samples` و`max_exposure_rows_per_declared_physical_sample`. عند غياب العمود تكون مؤشرات الأعداد `null`، وليس صفراً؛ فلا يُستنتج تحقق لم يحدث.

هذه بوابة **اتساق للمعرّفات المعلنة**، وليست مصادقة خارجية على هوية العينات أو استقلال المجموعات. لا يبرهن وجود العمود أن البيانات أصلية أو أن تقسيم التدريب/الاختبار كان محجوباً. يجب مراجعة سجلات المختبر، مصدر القياسات، وتوقيت كشف التسميات بصورة مستقلة.

## التعديلات والاختبارات
- الشيفرة: `research/implementation_execution/g2_paired_prediction_replay.py`، الالتزام `24f0ef1a3aeb33a71bebf90aaecd5e9527e949ad`.
- الاختبارات: `research/implementation_execution/test_g2_paired_prediction_replay.py`، الالتزام `0a746e5f60ebec4a854c90a07a3be3240d580652`.
- أضيفت خمسة اختبارات اصطناعية: تكرار قراءة العينة داخل مجموعة واحدة؛ رفض تكرارها بين مجموعتين؛ رفض القيمة الفارغة وغير المعيارية Unicode؛ الثبات أمام تغيير ترتيب الصفوف مع معرّفات عربية؛ والتمييز الصريح بين غياب بيانات العينة والتحقق منها.

## إعادة الإنتاج
```bash
python -m compileall -q research/implementation_execution
python -m unittest discover -s research/implementation_execution -p 'test_g2_*.py' -v
python research/implementation_execution/g2_evidence_audit.py --output /tmp/g2_audit_results.json
python research/implementation_execution/g2_acquisition_triage.py \
  --audit /tmp/g2_audit_results.json \
  --source-matrix research/implementation_execution/G2_CENTRAL_CATALOG_SOURCE_READINESS_2026-10-28.json \
  --output /tmp/g2_acquisition_triage.json
```

## نتائج CI المثبتة
- [GitHub Actions G2 #40957596290](https://github.com/HazRob/HazRob-Tank/actions/runs/40957596290)، الوظيفة `123741611646`: **388/388 PASS**، تجميع Python ناجح، تدقيق 80 تقنية ناجح، إعادة تشغيل الأدلة والفرز مطابقة بايتياً، وبوابة سلامة نشر الأثر ناجحة.
- الأثر البحثي `12933719604`، بصمة الحزمة `sha256:5793245ecfb2052c014ec33107323bee04c310335b5b63bc6cd767da44c40bb6`.
- نتائج البوابة: `g2_pass_candidates=0`، `g2_blocked=80`، `source_blocked=14`. لا ترقية علمية إلى G2 أو G3.
- جرى تعديل ملفات البحث فقط؛ لا تغيير في ChemCore Stable أو Android أو الفريموير أو `main`.

## شرط الاستكمال العلمي
الحصول على بيانات تعرض أصلية مستقلة ذات بصمات SHA256 ومراجع تركيز قابلة للتتبع، مع معرّفات فعلية للعينة/الجهاز/الجلسة والتقسيمات المحجوبة المثبتة مسبقاً، ثم مراجعة استقلال المجموعات والتحقق الخارجي من التسميات. لا تكفي نجاحات CI ولا الأمثلة الاصطناعية للاعتماد العلمي.
