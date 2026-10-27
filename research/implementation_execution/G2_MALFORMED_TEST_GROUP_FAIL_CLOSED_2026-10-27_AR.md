# G2 — منع تعطل تدقيق المجموعات المحجوبة عند وجود معرّفات غير صالحة

التاريخ: 2026-10-27. المشروع: HazRob Tank. النطاق: فرع البحث `research/chemcore80-central-evidence-gap-analysis-20261021` حصرياً؛ لا تعديل على ChemCore Stable أو تطبيق الإنتاج.

## الاستئناف والمصادر
- استُؤنف العمل من الالتزام `8ad5b52f8c5e0bc655655af52b4fa5a8c52e5e27`، مع مراجعة وثيقة `G2_MAJORITY_FULL_TRAINING_COVERAGE_GATE_2026-10-27_AR.md` وشيفرة بوابات الأدلة وسجل `G2_INDEPENDENT_EVIDENCE.json`.
- تم التحقق من مكتبة المراجع المركزية `HazRob/build-harness/library/reference-bank/external-research/master_catalog.json`: تجزئة Git blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f`، وهي ذات التجزئة المسجلة في `G2_CENTRAL_SOURCE_CANDIDATES_2026-10-26.json`. يحتوي السجل المحلي على 20 مرشح مصدر، ولا يُعادل أي منها قياساً فيزيائياً مؤهلاً على Tank.
- سجل الأدلة المستقلة `G2_INDEPENDENT_EVIDENCE.json` ما زال `records: []`. لا يمكن استنتاج تأهيل علمي من اختبارات CI الاصطناعية أو مجموعات UCI الخارجية.

## الثغرة والإصلاح
- كانت `audit_cross_artifact` تنفذ `set(record.get("test_group_ids", []))` وتستخدم عناصر المجموعة ذاتها كمفاتيح في عدادات bootstrap. القيم JSON الصحيحة نحوياً، مثل قائمة تحتوي قاموساً أو قائمة متداخلة، قد تتسبب في `TypeError: unhashable type` بدلاً من رفض منضبط؛ والقيمة العددية المفردة قد تتسبب في `TypeError: not iterable`.
- أُزيل الاعتماد على `test_group_ids` غير المتحقق من نوعها في مسارَي تغطية التنبؤات وحساب bootstrap، واستُخدمت معرّفات الاختبار النصية التي جمعها `expected_groups` فقط. يبقى رفض السجل غير السليم إلزامياً في تدقيق الحقول ومطابقة التقسيمات.
- أُضيف اختبار انحدار واحد بثلاث حالات فرعية: قاموس داخل قائمة الاختبار، قائمة متداخلة، وقيمة عددية مفردة. تتحقق الحالات من `SPLIT_MANIFEST_RECORD_MISMATCH` وحالة `G2_BLOCKED` دون استثناء غير مضبوط.

## الملفات والتزام التنفيذ
- الالتزام البرمجي: `3947f34514fd0acd82e478a62328918cc5dbb5d1`.
- الملفات المعدلة حصراً: `research/implementation_execution/g2_cross_artifact_gate.py` و`research/implementation_execution/test_g2_evidence_bundle_gate.py`.
- مقارنة GitHub بين `8ad5b52f` و`3947f345`: ملفان بحثيان فقط؛ لم يُمس ChemCore Stable.

## إعادة الإنتاج
من جذر المستودع على الالتزام `3947f345`:
```bash
cd research/implementation_execution
python -m unittest -v test_g2_signal_lineage_gate test_g2_evidence_audit test_g2_evidence_bundle_gate test_g2_cluster_bootstrap_gate test_g2_uci1081_signal_manifest
cd ../..
python research/implementation_execution/g2_evidence_audit.py --output /tmp/g2_audit_results.json
```

## نتائج CI الموثقة
- تشغيل GitHub Actions G2 رقم `40495584905`، الوظيفة `122160066950`: **168/168 PASS**، وظهر اختبار `test_malformed_test_group_ids_fail_closed_without_typeerror` في السجل.
- نتيجة تدقيق G2: `g2_pass_candidates=0` و`g2_blocked=80`.
- جميع تشغيلات GitHub Actions الناتجة عن الالتزام البرمجي اكتملت بنجاح: **21/21 تشغيلًا، عبر 12 مساراً مميزاً**؛ شملت مساري batch07 وbatch08 ووظائفهما.
- نجاح CI يثبت سلامة ضوابط البرمجيات فقط، ولا يثبت صحة تعيين الغاز أو التركيز ppm أو استقلال المراجع الفيزيائية.

## المتبقي للتأهيل العلمي
تتطلب ترقية أي تقنية إلى مراجعة G2 ملفات قياس خام أصلية قابلة لإعادة التشغيل، ومراجع تركيز فيزيائية مستقلة، ووحدات إشارة موثقة، ومعرفات أجهزة وجلسات أصلية، وفصلاً فعلياً لمجموعات التدريب والمعايرة والاختبار المحجوب. لا يوجد حالياً دليل يسمح بمنح اعتماد G2 أو G3 لأي تقنية.
