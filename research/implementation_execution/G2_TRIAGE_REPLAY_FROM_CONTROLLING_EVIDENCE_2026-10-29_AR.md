# G2 — إعادة احتساب تدقيق الأدلة من السجل الأصلي قبل نشر تقرير اقتناء المصادر (29 أكتوبر 2026)

## نقطة الاستئناف
- فرع البحث: `research/chemcore80-central-evidence-gap-analysis-20261021`، الالتزام السابق `8709968e96b2928901ea5bc9caf8c48970ef50d0`.
- فهرس المراجع المركزي: `HazRob/build-harness/library/reference-bank/external-research/master_catalog.json`، Git blob SHA1 `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f`، 80 مرجعاً منها 14 مجموعة قياسات خارجية.
- المصفوفة المشتقة المثبتة: Git blob SHA1 `bd69304fc35175b90199c6cc6ad4a88fc10ae93f`؛ المصادر الجاهزة للمراجعة العلمية 0/14.
- سجل `G2_INDEPENDENT_EVIDENCE.json` لا يزال `records: []`، ولا توجد قياسات تعرض فيزيائية مستقلة مؤهلة أو تقسيمات محجوبة موثقة.

## المشكلة وأثرها
كان `g2_acquisition_triage.py` يراجع مخطط ملف نتائج التدقيق، ومجاميع 80 تقنية و14 مصدراً، وهوية المعرفات. لكنه كان يقبل ملف تدقيق مُعدّلاً يحتوي أسباب حجب مختلقة، أو يحوّل تقنية إلى `G2_PASS_CANDIDATE_REQUIRES_REVIEW` إذا عُدلت الحقول والمجاميع بصورة متسقة. لا يكفي تطابق عدد السجلات أو بصمات المصادر لإثبات أن تقرير الفرز يعكس إعادة تنفيذ بوابة الأدلة نفسها.

## الإصلاح
- أصبح مسار `build_triage` يعيد قراءة سجل التنفيذ `EXECUTION_LEDGER_2026-10-21.json` وسجل الأدلة المستقلة عبر قارئ JSON الصارم في `g2_evidence_audit.py`.
- يتحقق من نطاق معرفات التقنيات الثمانين الأصلي، ويعيد تشغيل `audit_all` ثم `reconcile_catalog_source_evidence` باستخدام المصفوفة المثبتة بالبايتات.
- يقارن التقرير المُدخل **بالكامل** بالنتيجة المعاد حسابها، بما في ذلك ترتيب سجلات التدقيق، الحالات، أسباب الحجب، والأعداد. عند أي اختلاف يتوقف مع `G2_TRIAGE_AUDIT_REPLAY_MISMATCH` ولا ينشر تقرير اقتناء مضللاً.
- أضيف معاملا CLI اختياريان `--ledger` و`--evidence`، وتبقى القيم الافتراضية هي السجلات الأصلية في فرع البحث.
- أضيف اختبار انحدار واحد مع حالتين سلبيتين (تغيير سبب الحجب؛ واختلاق مرشح مع تعديل الأعداد)، وحالة تحكم إيجابية لإعادة احتساب التدقيق الحقيقي.

## إعادة الإنتاج
```bash
python -m unittest discover -s research/implementation_execution -p 'test_g2_*.py' -v
python research/implementation_execution/g2_evidence_audit.py --output /tmp/g2_audit_results.json
python research/implementation_execution/g2_acquisition_triage.py \
  --audit /tmp/g2_audit_results.json \
  --source-matrix research/implementation_execution/G2_CENTRAL_CATALOG_SOURCE_READINESS_2026-10-28.json \
  --ledger research/implementation_execution/EXECUTION_LEDGER_2026-10-21.json \
  --evidence research/implementation_execution/G2_INDEPENDENT_EVIDENCE.json \
  --output /tmp/g2_acquisition_triage.json
```

## نتيجة الاختبارات والمخرجات
- التزام الشيفرة والاختبارات: `4618ffe49d04ed711746c207ca9a3e0f232ca894`.
- GitHub Actions G2: https://github.com/HazRob/HazRob-Tank/actions/runs/40779896071 ؛ الوظيفة `123138599806`؛ **283/283 اختباراً ناجحاً**، بما فيها اختبار إعادة الاحتساب الجديد.
- بوابة التدقيق: `g2_pass_candidates=0`، `g2_blocked=80`، `source_blocked=14`. تم رفع ملفي التدقيق والفرز ضمن artifact `12856631682`، وSHA256 لحزمة artifact `e9e1d8dd187f343abf67247b1fb1a53741fe63e1169ffbf843d3e76688060e4d`.
- جميع تشغيلات CI المرتبطة بالتزام الشيفرة **21/21 ناجحة**، دون إخفاقات متبقية. أثبتت مقارنة الالتزامات أن التغييرات اقتصرت على ملفي Python داخل `research/implementation_execution/`.

## الحدود العلمية
إعادة الاحتساب تربط تقرير الفرز بسجل الأدلة الموجود في المستودع، ولا تثبت أصالة هذا السجل خارجياً، ولا تنشئ قياسات مستقلة، ولا تحوّل مصدر البيانات إلى معيار معايرة. يظل تأهيل G2 غير مكتمل (0/80 مرشحاً)؛ ولا يوجد اعتماد G2 أو G3. استكمال التأهيل يتطلب الحصول على بايتات قياسات فيزيائية أصلية قابلة للتحقق، ومراجع تركيز قابلة للتتبع، ومعرفات أجهزة/جلسات/تعرضات، وتقسيمات اختبار محجوبة ومثبتة قبل كشف النتائج. لا تعديلات على ChemCore Stable أو تطبيق Android أو الفريموير.
