# G2 — قفل تسميات المرجع لمجموعة المعايرة (2026-10-27)

## نطاق التنفيذ
فرع البحث حصراً: `research/chemcore80-central-evidence-gap-analysis-20261021`. لم تُعدّل ChemCore Stable أو Android/ESP32 أو الفرع الرئيسي. تعديل البرمجيات: `g2_cross_artifact_gate.py` (commit `2f78455d13bad40d195a9aa3d06f3732783d9425`)؛ الاختبارات: `test_g2_evidence_bundle_gate.py` (commit `4473942658a890f2fa9cc4f335d66312bd4cc07a`).

## المشكلة المثبتة
كان `group_assignment_sha256` يقفل توزيع التعرضات والهويات الفيزيائية، لكنه يستبعد تسميات الغازات قصداً، وكان `baseline_model.training_labels_sha256` يقفل تسميات التدريب فقط. بالتالي يمكن تغيير `gas_reference_id` لمجموعة المعايرة في `group_manifest.csv` وإعادة حساب SHA256 لملف CSV دون تغيير قفل التوزيع أو قفل تسميات التدريب. قد يؤدي ذلك إلى إخفاء التعديل اللاحق لتسميات معايرة العتبات أو اختيار النماذج.

## المعالجة القابلة لإعادة التشغيل
أُضيف الحقل الإلزامي `protocol_locked.json.calibration_labels_sha256`. يعاد بناء صفوف مجموعة المعايرة فقط من `group_manifest.csv`، كل صف `[exposure_id,group_id,gas_reference_id]`، مرتبة حسب `(exposure_id,group_id)`، ثم تُسلسل JSON باستخدام `ensure_ascii=False` و`separators=(",",":")` وتُحسب SHA256 لبايتات UTF-8. تُرفض القيم الفارغة والمفقودة، أو الحقل غير الصحيح، أو عدم التطابق، بالرموز `CALIBRATION_LABELS_UNAVAILABLE` و`CALIBRATION_LABEL_DIGEST_NOT_LOCKED` و`CALIBRATION_LABEL_DIGEST_MISMATCH`.

التحقق يتم **بعد** بوابة التجزئة لملفات الحزمة الأربعة عشر، وهو مستقل عن قفل توزيع التعرضات وعن قفل تسميات التدريب. لا يُعاد قفل أي تسمية في الاختبارات السلبية.

## الاختبارات
أضيفت أربع حالات اصطناعية: حالة سليمة، تغيير تسمية المعايرة مع إعادة تجزئة CSV، حذف قفل التسميات مع إعادة تجزئة البروتوكول، وتفريغ تسمية المعايرة مع تجزئة صحيحة. نجحت **200/200** حالة Python في GitHub Actions على الالتزام `4473942658a890f2fa9cc4f335d66312bd4cc07a`، تشغيل G2 رقم `40577570708`، وظيفة `122447989194`. أخرج التدقيق `g2_pass_candidates=0` و`g2_blocked=80`. رابط التشغيل: https://github.com/HazRob/HazRob-Tank/actions/runs/40577570708 .

لإعادة التشغيل من جذر مستودع نظيف:
```bash
cd research/implementation_execution
python -m unittest -v test_g2_signal_lineage_gate test_g2_evidence_audit test_g2_evidence_bundle_gate test_g2_cluster_bootstrap_gate test_g2_uci1081_signal_manifest
cd ../..
python research/implementation_execution/g2_evidence_audit.py --output /tmp/g2_audit_results.json
```
ينبغي أن تكون الحصيلة 200 اختبار ناجحاً و0 مرشحين و80 محجوبين عند استخدام نفس الالتزام.

## مصادر الأدلة وحالتها
تأكد تطابق ملف المراجع المركزي `HazRob/build-harness/library/reference-bank/external-research/master_catalog.json` مع Git blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f`. يحتوي الفهرس على 80 مرجعاً رئيسياً، منها 14 مصدر بيانات قياس خارجي، لكن سجلات هذه المصادر لا تقدم تأهيلاً مستقلاً مثبتاً لمجموعات المعايرة والاختبار المطلوبة. تأكدت أيضاً مطابقة مصدر جرد MQ الخارجي في فرع `research/mq-open-measurements-audit-20261021` مع blob `522a7013d5fab3ac6e6c2e4cbad1fc20fb99f837`. سجل G2 الفعلي `G2_INDEPENDENT_EVIDENCE.json` لا يزال `records: []`.

## حدود الاستدلال
تجزئة القفل تثبت **الاتساق الداخلي فقط**، ولا تثبت أن البروتوكول سُجل قبل كشف تسميات الاختبار، أو أن الغاز المرجعي معتمد، أو أن السجلات الخام مرتبطة فعلاً بالتعرضات، أو أن الوحدات والتركيزات دقيقة. الحالات الاصطناعية ليست أدلة فيزيائية ولا تؤهل ppm أو هوية الغاز أو قرار سلامة. لا توجد ترقية G2/G3 ولا تعديل لـ ChemCore Stable.

## المتبقي
التحقق المستقل زمنياً من تسجيل البروتوكول، توفير أرشيفات قياسات خام أصلية قابلة للتجزئة وإعادة التشغيل، شهادات وتركيزات مرجعية قابلة للتتبع، هوية الجهاز والحساس والجلسة والتعرض، واختبار محجوب فعلي مع تدقيق مخرجات النموذج. لا تُنقل أي تقنية من G2_BLOCKED دون ذلك.
