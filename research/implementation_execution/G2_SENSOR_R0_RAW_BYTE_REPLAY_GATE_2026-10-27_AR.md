# G2 — إعادة التحقق من بايتات مصدر معايرة R0 لكل حساس فعلي

التاريخ: 2026-10-27. المشروع: HazRob Tank. النطاق: فرع البحث `research/chemcore80-central-evidence-gap-analysis-20261021` فقط. لا تعديل على ChemCore Stable أو التطبيق أو الفريموير، ولا اعتماد علمي تلقائي.

## مراجعة حالة الاستئناف والمراجع

- نقطة الاستئناف: الالتزام `f928136378b552ee75e17a45d9008f32b70fffb8`، والتقرير السابق `G2_UNHASHABLE_NESTED_METADATA_FAIL_CLOSED_2026-10-27_AR.md`.
- مكتبة المراجع المركزية: `HazRob/build-harness/library/reference-bank/external-research/master_catalog.json`، Git blob SHA `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f`؛ أُعيد فحص SHA مباشرة من GitHub وهو مطابق للفهرس المحلي. لا يعني وجود مرجع منشور أنه دليل معايرة Tank.
- سجل `G2_INDEPENDENT_EVIDENCE.json` لا يزال `records: []`. بيانات UCI والبيانات الخارجية لا تعوّض قياسات Tank أو شهادات المرجع الفيزيائي.

## الفجوة العلمية-البرمجية المثبتة

كانت `audit_signal_lineage` تشترط في مرجع `R0` قيمة `raw_sha256` وعلامة `source_bytes_verified=true`، لكنها لا تعيد حساب SHA256 من ملف مصدر معايرة R0. بوابة البيانات الخام العامة `g2_raw_source_gate.py` تتحقق من أرشيف القياسات الرئيس، وليس بالضرورة من **أرشيف المعايرة المنفصل لكل حساس**. بالتالي كان ممكناً تقديم ملف قياسات أصلي صحيح مع ادعاء R0 بلا ملف معايرة أصلي.

## التصحيح

- أُضيفت `g2_r0_source_gate.py` للتحقق من بايتات أرشيف R0 لكل مرجع مستخدم في `RS_OVER_R0`، سواء `r0_reference` أو `r0_references` متعددة الحساسات.
- أصبحت حقول `raw_local_path` و`raw_size_bytes` و`raw_sha256` إلزامية لكل مصدر R0. يُقبل الملف العادي فقط تحت `research/g2/raw/`، وتُرفض المسارات النسبية غير الآمنة، الروابط الرمزية، الملفات الغائبة، اختلاف الحجم أو SHA256، ومؤشرات Git LFS غير المحمّلة.
- رُبط التحقق بـ `g2_evidence_audit.audit_record` بعد نجاح فحص ملفات الحزمة، مع الاحتفاظ بالفصل بين سلامة البايتات وصحة المرجع الكيميائي.
- جُهّزت عينة الاختبار الاصطناعية بملف R0 خام مستقل عن ملف القياسات، وأُضيفت **ستة اختبارات انحدار**: نجاح الإعادة، غياب الأرشيف مع علامة verified، تغيير البايتات، traversal، مؤشر LFS، وغياب الحجم.

## التنفيذ وإعادة الإنتاج

الالتزام البرمجي: `d2bb5968836427a9bd681551cc229e359a99661f`.

الملفات المعدلة حصرياً داخل `research/implementation_execution/`:
`g2_r0_source_gate.py` (جديد)، `g2_evidence_audit.py`، `test_g2_evidence_bundle_gate.py`.

من جذر المستودع:

```bash
cd research/implementation_execution
python -m unittest -v test_g2_signal_lineage_gate test_g2_evidence_audit test_g2_evidence_bundle_gate test_g2_cluster_bootstrap_gate test_g2_uci1081_signal_manifest
cd ../..
python research/implementation_execution/g2_evidence_audit.py --output /tmp/g2_audit_results.json
```

## نتائج CI المحققة

- تشغيل بوابة G2: `40511485875`، الوظيفة `122214255692`، الحالة **SUCCESS**، **179/179 اختباراً ناجحاً** وفق سجل الوظيفة، مع نجاح اختبارات إعادة تشغيل R0 الستة.
- نتيجة التدقيق على التقنيات الثمانين: `g2_pass_candidates=0` و`g2_blocked=80`، ومخرجات التدقيق في artifact رقم `12737506920`.
- مقارنة `f9281363...d2bb5968`: ثلاثة ملفات فقط، كلها داخل `research/implementation_execution/`. لم يُمس ChemCore Stable.
- حالة بقية CI: راجع تشغيلات الالتزام نفسه؛ أي تشغيل غير مكتمل لا يُعتبر ناجحاً قبل ظهور `conclusion=success`.

## حدود الإثبات

إعادة حساب تجزئة ملف R0 تثبت اتساق الأرشيف مع الادعاء المسجل فقط؛ لا تثبت أن الغاز المرجعي صحيح أو أن تركيزه مضبوط أو أن R0 قيس للحساس الفعلي تحت شروط التسخين المحددة. يلزم التحقق المستقل من المعيار الفيزيائي وسلسلة المعايرة، ووحدات Rs/R0، وأجهزة/جلسات المصدر، والتقسيمات المحجوبة. الحالة العلمية تبقى **0/80 مؤهلاً و80/80 محجوباً**؛ لا ترقية إلى G2 أو G3.
