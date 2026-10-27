# G2 — رفض مسافات ASCII الطرفية في هوية أدلة CSV
التاريخ: 2026-10-27. النطاق: فرع البحث `research/chemcore80-central-evidence-gap-analysis-20261021` فقط.

## فجوة الدليل
كان تدقيق CSV يرفض محارف Unicode الخفية والمسافات غير القياسية وصيغ Unicode غير المطابقة لـ NFC، لكنه لا يرفض مسافة ASCII العادية U+0020 عند بداية/نهاية خلية. في إعادة تشغيل `majority_train` تُزال المسافات الطرفية من تسميات التدريب عند حساب الأغلبية، بينما تُقارن تسميات الحقيقة العمياء حرفياً. لذلك يمكن للاحقة `"GAS_A "` أن تصنع فرقاً مصطنعاً في دقة خط الأساس مقابل تنبؤ يطابق النص الخام، حتى إذا أُعيد حساب SHA256 لجميع الملفات المتغيرة.

## التنفيذ
- `g2_evidence_bundle_gate.py`: رفض `value != value.strip()` في عناوين وخلايا CSV مع الاحتفاظ بالمسافات الداخلية في تسميات مثل `ethanol 50 ppm`.
- `g2_cross_artifact_gate.py`: تطبيق الشرط ذاته في مسار إعادة القراءة المستقل، لتفادي اختلاف حكم البوابتين.
- `test_g2_evidence_bundle_gate.py`: اختباران جديدان. الأول يفحص تسميات صحيحة ومتغيرات ذات مسافات طرفية؛ والثاني يعيد حساب SHA256 بعد ستة تحويرات في `group_manifest.csv` و`predictions_blind.csv` و`metrics_per_group.csv`، ويتطلب رفض بوابة سلامة الملف وإعادة التشغيل وتدقيق السجل النهائي.
- التغيير لا يحوّل أي بيانات اصطناعية إلى أدلة فيزيائية، ولا يثبت هوية غاز أو تركيز ppm.

## إعادة الإنتاج ونتيجة CI
من جذر المستودع في نسخة نظيفة من فرع البحث:
```sh
cd research/implementation_execution
python -m unittest -v test_g2_signal_lineage_gate test_g2_evidence_audit test_g2_evidence_bundle_gate test_g2_cluster_bootstrap_gate test_g2_uci1081_signal_manifest
cd ../..
python research/implementation_execution/g2_evidence_audit.py --output /tmp/g2_audit_results.json
```
أثبتت GitHub Actions في التشغيل [40559691241](https://github.com/HazRob/HazRob-Tank/actions/runs/40559691241) على الالتزام البرمجي `a0c70b6df42b412a84eefe4fd0db2537639d94e3` نجاح **192/192 اختباراً** في وظيفة `122385061083`، وتدقيق `g2_pass_candidates=0` و`g2_blocked=80`. تُراجع مسارات CI الأخرى على الالتزام نفسه بصورة مستقلة، ولا تُعد نتائجها ناجحة إلا عند ظهور `conclusion=success`.

## مراجعة المصادر وحدود الاعتماد
- مكتبة المراجع المركزية `HazRob/build-harness/library/reference-bank/external-research/master_catalog.json` مطابقة لتجزئة Git blob المثبتة `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f`.
- سجل `G2_INDEPENDENT_EVIDENCE.json` يحتوي `records: []`. يظل **0/80** مرشحين لمراجعة G2 و**80/80** محجوبين. لا اعتماد G2/G3 ولا ترقية إلى ChemCore Stable.
- عائق التأهيل العلمي المعروف: غياب قياسات مستقلة مؤهلة مرتبطة بمراجع تركيز فيزيائية وأجهزة وجلسات أصلية وتقسيمات حجب مستقلة، بصرف النظر عن نجاح الاختبارات الاصطناعية.
- لا تعديل للتطبيق أو الفريموير أو `ChemCore Stable`؛ لا دمج في `main`.
