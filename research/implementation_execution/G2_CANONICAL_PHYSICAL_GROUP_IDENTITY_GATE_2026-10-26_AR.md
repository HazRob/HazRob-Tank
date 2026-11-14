# G2 — منع التحايل على استقلال الجلسات بإضافة أعمدة مصدر غير فيزيائية (2026-10-26)

## النطاق
- المشروع: `HazRob/HazRob-Tank`، الفرع البحثي `research/chemcore80-central-evidence-gap-analysis-20261021` حصراً.
- الملفات البرمجية: `research/implementation_execution/g2_cross_artifact_gate.py` و`test_g2_evidence_bundle_gate.py`.
- لا تغيير لـChemCore Stable أو Android أو firmware أو مكتبة المراجع المركزية.
- بيانات الاختبار الاصطناعية **ليست** قياسات غازات فعلية.

## المشكلة القابلة لإعادة الإنتاج
في بوابة `audit_cross_artifact` كان تحقق استقلال المجموعات يستخدم **جميع** الأعمدة المصرح بها في `grouping_source_field` لتكوين مفتاح الهوية الفيزيائية. عندما تكون `grouping_key=device_session_id`، يستطيع سجل أن يصرّح `device_id,session_id,sample_id`؛ وعند تكرار الجلسة نفسها في التدريب والاختبار مع تغيير `sample_id`، يظهر المفتاح المركب مختلفاً ويختفي تسرب الجلسة من فحص `PHYSICAL_GROUP_ALIAS_ACROSS_SPLITS`. تجزئات SHA256 الصحيحة لا تمنع هذا الخطأ المنطقي.

## الإصلاح
- تثبيت **مفتاح هوية فيزيائية معياري** بحسب `grouping_key`:
  - `device_session_id`: `device_id + session_id`
  - `session_id`: `session_id`
  - `site_day_device`: `site_id + day_id + device_id`
  - `batch_id`: `batch_id`
  - `campaign_id`: `campaign_id`
- الإبقاء على الأعمدة الإضافية المصرح بها لأغراض provenance، والتحقق من عدم خلوها، **لكن دون استعمالها لإثبات استقلال جلسة جديدة**.
- يستمر رفض الأعمدة الفيزيائية الأساسية الناقصة أو غير المعروفة، واستمرار حظر تكرار المجموعة الفيزيائية عبر أقسام train/calibration/test.

## الاختبارات
- تحكم سلبي جديد: الجلسة `d1/s1` نفسها في train وblind test، مع `sample_id` مختلف؛ النتيجة المطلوبة `PHYSICAL_GROUP_ALIAS_ACROSS_SPLITS` و`G2_BLOCKED`.
- تحكم إيجابي جديد: أربع جلسات متميزة بمفاتيح فيزيائية صحيحة، مع `sample_id` إضافي؛ تظل حزمة fixture الاصطناعية مرشح مراجعة برمجية، **لا تأهيلاً علمياً**.
- GitHub Actions `40385319420` على commit `6f6b60a007e2eb9a544f9e7b475be68ebfef30c8`: **112 اختباراً ناجحاً**، `g2_pass_candidates=0`، `g2_blocked=80`.
- سجل التشغيل: https://github.com/HazRob/HazRob-Tank/actions/runs/40385319420
- التزامات التغيير: `72d8e06880a1d1bcc47fdccabc94776061227189` (الإصلاح) و`6f6b60a007e2eb9a544f9e7b475be68ebfef30c8` (الاختبارات).

## إعادة الإنتاج
```bash
cd research/implementation_execution
python -m unittest -v test_g2_evidence_bundle_gate test_g2_evidence_audit test_g2_cluster_bootstrap_gate test_g2_signal_lineage_gate test_g2_uci1081_signal_manifest
cd ../..
python research/implementation_execution/g2_evidence_audit.py --output /tmp/g2_audit_results.json
```

## حدود الحجية والقرارات
نجاح فحص الهوية لا يثبت أن أعمدة `device_id` و`session_id` مستخرجة من جهاز حقيقي دون تلاعب؛ يتطلب ذلك بيانات مصدر أصلية وتوثيقاً مستقلاً. كما لا يثبت عدد المجموعات كفاية القدرة الإحصائية، أو صحة تركيز ppm أو R0 أو هوية الغاز. حالة G2 الفعلية باقية **0/80 مرشحاً، 80/80 محجوباً**؛ G3 غير معتمد. يستمر البحث عن مجموعات بيانات خام ذات معرفات جهاز/جلسة أصلية ومرجع فيزيائي مستقل، دون أي ترقية تلقائية.
