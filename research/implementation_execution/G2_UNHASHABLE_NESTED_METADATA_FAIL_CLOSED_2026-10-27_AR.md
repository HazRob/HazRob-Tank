# G2 — منع تعطل تدقيق بيانات الإشارة والتجميع الفيزيائي ذات الأنواع غير القابلة للتجزئة

التاريخ: 2026-10-27. المشروع: HazRob Tank. النطاق: فرع البحث `research/chemcore80-central-evidence-gap-analysis-20261021` فقط. **لا تعديل على ChemCore Stable ولا ترقية علمية**.

## نقطة الاستئناف والمصادر

- نقطة الاستئناف: الالتزام `4343140c42410d7c97154ab711ee80f754e45875` ووثيقة `G2_MALFORMED_TEST_GROUP_FAIL_CLOSED_2026-10-27_AR.md`.
- أُعيد التحقق من مصدر مكتبة المراجع المركزية `HazRob/build-harness/library/reference-bank/external-research/master_catalog.json`: Git blob SHA `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f`، مطابق لفهرس مرشحي مصادر G2 المحلي. المصدر المركزي مكتبة مراجع، وليس شهادة صلاحية لقياسات Tank.
- سجل `G2_INDEPENDENT_EVIDENCE.json` لا يزال يحوي `records: []`. آخر تدقيق مكتمل قبل هذا التعديل أعاد `g2_pass_candidates=0` و`g2_blocked=80`؛ لا تُستنتج نتيجة تأهيل جديدة قبل تشغيل CI.
- مراجعة GitHub Actions على الالتزام السابق `4343140c` أظهرت نجاح التشغيلات الموثقة في الجولة السابقة.

## الثغرة المثبتة وتصحيحها

عند تمرير قيمة JSON صحيحة نحوياً لكنها من نوع `list` أو `dict` بدلاً من النص في `source_signal_units` أو `r0_mode` أو `heater_phase`، كانت اختبارات العضوية في `set` وعمليات `dict.get` قد تُطلق `TypeError: unhashable type`، ما يوقف تدقيق الدفعة كلها بدلاً من حجب السجل المعني. وبالمثل، يمكن لقيمة `grouping_key` غير النصية أن تتسبب في الاستثناء أثناء إعادة تشغيل الاتساق بين الملفات.

- أُضيفت حراسة نوعية صريحة قبل عمليات العضوية/البحث في `g2_signal_lineage_gate.py` مع الاحتفاظ برموز الرفض العلمية القائمة: `INVALID_SOURCE_SIGNAL_UNITS`، `R0_MODE_UNDOCUMENTED`، `R0_HEATER_PHASE_UNDOCUMENTED`، و`RS_OVER_R0_REQUIRES_RESISTANCE_INPUT_AND_DIMENSIONLESS_OUTPUT`.
- أُضيفت حراسة نوعية قبل `required_by_grouping_key.get` في `g2_cross_artifact_gate.py`، مع استمرار رفض `GROUPING_SOURCE_COLUMNS_NOT_PHYSICAL_FOR_KEY`.
- أُضيفت خمسة اختبارات انحدار، تتضمن حالات فرعية لقيم قائمة/قاموس في وحدات الإشارة ونمط R0 وطور التسخين ومعرّف التجميع الفيزيائي، وحالة نمط R0 غير النصي مع الإشارة الخام.
- هذه اختبارات ضوابط برمجية اصطناعية فقط؛ لا تختبر صحة تعريف الغاز أو تركيز ppm أو استقلال المراجع.

## الملفات والالتزامات

تغييرات الشيفرة والاختبارات الأربعة (كلها داخل `research/implementation_execution/`):

1. `g2_signal_lineage_gate.py` — الالتزام `f86dea3251a4120daa5fb12e8ef46f8ca40da04f`.
2. `g2_cross_artifact_gate.py` — الالتزام `41995e41b0731c5e38ceff5f5cbb323a8b557c16`.
3. `test_g2_signal_lineage_gate.py` — الالتزام `a441d7b8b261b3548eb7909cba9667b1a0dcf1b0`.
4. `test_g2_evidence_bundle_gate.py` — الالتزام `5d2360ed2e1671831df3af9624ebe08a3bac43fa`.

أثبتت مقارنة GitHub بين `4343140c` و`5d2360ed` أن جميع التغييرات الأربعة تقع داخل `research/implementation_execution/` ولا تمس ChemCore Stable.

## إعادة الإنتاج

من جذر المستودع، على الالتزام `5d2360ed2e1671831df3af9624ebe08a3bac43fa`:

```bash
cd research/implementation_execution
python -m unittest -v test_g2_signal_lineage_gate test_g2_evidence_audit test_g2_evidence_bundle_gate test_g2_cluster_bootstrap_gate test_g2_uci1081_signal_manifest
cd ../..
python research/implementation_execution/g2_evidence_audit.py --output /tmp/g2_audit_results.json
```

## حالة GitHub Actions عند التوثيق

- تشغيل بوابة G2 على الالتزام البرمجي الأخير: `40502987866`، كان **queued** عند آخر استعلام، ولم تُستنتج منه نتيجة نجاح أو فشل.
- كانت تشغيلات المسارات الأخرى للالتزام نفسه في قائمة الانتظار أيضاً؛ لا يوجد إخفاق مكتمل في هذه الجولة يمكن إصلاحه حتى الآن.
- عند توفر التشغيلات المكتملة يجب فحص عدد الاختبارات ونتائج جميع المسارات وإصلاح أي فشل قبل اعتبار ضوابط هذه الجولة متحققة في CI.

## الحالة العلمية والقيود

لا تزال `0/80` مرشحين مؤهلين بحسب سجل الأدلة المستقلة الحالي ونتيجة آخر تدقيق مكتمل، و`80/80` محجوبين. لم تُجمع قياسات Tank فيزيائية مستقلة، ولم تُمنح شهادة G2 أو G3. لا يمكن استكمال التأهيل العلمي إلا بعد توفير مصادر خام أصلية ذات مراجع تركيز فيزيائية مستقلة، ومعرّفات أجهزة/جلسات، ووحدات وإجراءات معايرة موثقة، وتقسيمات محجوبة قابلة لإعادة الإنتاج.
