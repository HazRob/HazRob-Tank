# G2 — بوابة إعادة التشغيل الحرفية للنتائج ومطابقة SHA256 (29 أكتوبر 2026)

## نقطة الاستئناف والمصادر
- المستودع: `HazRob/HazRob-Tank`؛ الفرع البحثي فقط: `research/chemcore80-central-evidence-gap-analysis-20261021`.
- نقطة الاستئناف: `5a2d18cf52e74c5c379378e82c91a99e3afe2692`؛ تأكد نجاح تشغيلات CI السابقة 20/20.
- مكتبة المراجع المركزية في `HazRob/build-harness/library/reference-bank/external-research/master_catalog.json`؛ Git blob SHA1: `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f`، وتحتوي 80 مدخلاً منها 14 مجموعة بيانات خارجية.
- مصفوفة جاهزية المصادر مثبتة، والمراجعة المؤهلة 0/14. سجل الأدلة الفيزيائية المستقلة `records: []`.

## التحسين البحثي
كانت بوابة CI تولد تقرير التدقيق وتقرير اقتناء الأدلة مرة واحدة، ثم ترفع المخرجات دون اختبار فعلي لثبات **بايتات المخرجات عند إعادة تنفيذ العملية**. أُضيف ملف `g2_replay_consistency.py` للتحقق من تطابق ملفي التدقيق والفرز بعد تنفيذ مستقل ثانٍ لنفس الأوامر والمدخلات المثبتة. يفشل التشغيل مغلقاً إذا اختلفت البايتات، بما في ذلك المسافات أو ترتيب الحقول، مع `G2_REPLAY_BYTES_MISMATCH:audit` أو `G2_REPLAY_BYTES_MISMATCH:triage`. يصدر ملف `g2_replay_integrity.json` يحوي SHA256 والحجم والتطابق لكل مخرج، ويرفع مع artifact الأصلي.

أضيفت أربعة اختبارات: تطابق حتمي، اختلاف بايتات التدقيق، اختلاف تمثيل JSON المتكافئ منطقياً في تقرير الفرز، وتحقق من أن سير العمل يعيد تشغيل الأمرين ويصدر manifest. جميع الأمثلة الاصطناعية لا تُعد أدلة علمية أو تجارب غازات.

## إعادة الإنتاج
```bash
python -m unittest discover -s research/implementation_execution -p 'test_g2_*.py' -v
python research/implementation_execution/g2_evidence_audit.py --output /tmp/g2_audit_results.json
python research/implementation_execution/g2_acquisition_triage.py --audit /tmp/g2_audit_results.json --source-matrix research/implementation_execution/G2_CENTRAL_CATALOG_SOURCE_READINESS_2026-10-28.json --output /tmp/g2_acquisition_triage.json
python research/implementation_execution/g2_evidence_audit.py --output /tmp/g2_audit_results_replay.json
python research/implementation_execution/g2_acquisition_triage.py --audit /tmp/g2_audit_results_replay.json --source-matrix research/implementation_execution/G2_CENTRAL_CATALOG_SOURCE_READINESS_2026-10-28.json --output /tmp/g2_acquisition_triage_replay.json
python research/implementation_execution/g2_replay_consistency.py --audit-first /tmp/g2_audit_results.json --audit-second /tmp/g2_audit_results_replay.json --triage-first /tmp/g2_acquisition_triage.json --triage-second /tmp/g2_acquisition_triage_replay.json --output /tmp/g2_replay_integrity.json
```

## نتائج CI المؤكدة
- التزام التطوير: `92edd4402aab54938244af755077433219abdd74`.
- [تشغيل GitHub Actions G2 رقم 40793543195](https://github.com/HazRob/HazRob-Tank/actions/runs/40793543195)؛ job `123184342051`، **289/289 اختباراً ناجحاً**.
- إعادة التشغيل: نجح التحقق من تطابق بايتات ملفي التدقيق والفرز.
- نتائج التدقيق: `g2_pass_candidates=0`، `g2_blocked=80`، `source_blocked=14`.
- Artifact: `12862363060`؛ SHA256 لحزمة الرفع `3fb6bd09688b9ec3f759fc5e9a7dede495adeef9b168c7082b7fd08536650eed`.
- التعديلات محصورة في `research/implementation_execution/` وسير عمل G2 البحثي؛ لم يُعدّل ChemCore Stable أو تطبيق Android أو الفريموير.

## حدود الدليل
تطابق التشغيلين يثبت حتمية المخرجات البرمجية لهذه المدخلات ولا يثبت أصالة المصدر الفيزيائي أو استقلال التجارب أو معايرة ppm. لم يمنح أي اعتماد G2/G3. لاستكمال التأهيل العلمي لا تزال هناك حاجة إلى بيانات تعرض فيزيائية مستقلة، مراجع تركيز قابلة للتتبع، معرّفات الأجهزة والجلسات والتعرضات، وتقسيمات تدريب/اختبار محجوبة ومثبتة مسبقاً. يُعاد فحص CI على الالتزام التوثيقي التالي عند الاستئناف.
