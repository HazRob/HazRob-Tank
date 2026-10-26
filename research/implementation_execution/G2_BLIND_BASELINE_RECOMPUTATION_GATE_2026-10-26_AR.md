# G2 — بوابة إعادة احتساب أداء النموذج المرجعي من التنبؤات المحجوبة (2026-10-26)

## النطاق
المشروع: HazRob Tank — تأهيل ChemCore80 التجريبي G2. التغييرات محصورة في فرع `research/chemcore80-central-evidence-gap-analysis-20261021` ضمن أدوات البحث. لم تُعدّل ChemCore Stable أو شيفرة التطبيق/الفريموير أو المعايرة.

## الخلل المحدد
كانت `g2_cross_artifact_gate.py` تعيد حساب `classification_accuracy` للنموذج المختبر من صفوف `predictions_blind.csv`، لكنها تكتفي بمطابقة `baseline_metric_value` بين سجل الأدلة و`metrics_summary.json`. لذلك كان ممكناً تقديم رقم baseline غير مستند إلى صفوف الاختبار، مع تجزئات SHA256 متطابقة، ثم الادعاء بتحسن غير قابل لإعادة الإنتاج.

## الإصلاح
- جعل `baseline_predicted_reference` عموداً إلزامياً في `predictions_blind.csv` عند استعمال `metric_name=classification_accuracy`.
- إعادة حساب `baseline_metric_value` من **التعرضات المحجوبة نفسها** التي يُحسب منها `metric_value`، وبالمقارنة مع `gas_reference_id` في `group_manifest.csv`.
- رفض السجل برمز `BLIND_BASELINE_ACCURACY_RECOMPUTATION_MISMATCH` إذا اختلف الرقم المحسوب عن المعلن، أو `ACCURACY_RECOMPUTATION_FIELDS_MISSING` إذا غاب العمود.
- إضافة ثلاثة اختبارات سلبية: تضخيم baseline في السجل وملف الملخص معاً؛ حذف تنبؤات baseline؛ وتغيير تنبؤات baseline مع تحديث SHA256.
- تحديث حالة اختبار `BLIND_PREDICTION_TRUTH_MISMATCH` لتظل قابلة للفحص بعد توسيع مخطط CSV.

## حدود الإثبات
الاختبارات تستخدم حزم ملفات **اصطناعية** للتحقق من سلامة بوابة التدقيق فقط. لا تثبت أن نموذج baseline دُرّب بصورة مستقلة أو أن `blind_test` كان محجوباً فعلاً، ولا تثبت صحة قياسات MQ أو ppm. يلزم تثبيت بروتوكول baseline مسبقاً والتحقق من استقلال المصدر الخام والمجموعات قبل أي مراجعة علمية. `G2_INDEPENDENT_EVIDENCE.json` يحتوي حالياً على `records: []`؛ **0/80 مرشحين، 80/80 محجوبين**.

## إعادة الإنتاج
```bash
cd research/implementation_execution
python -m unittest -v test_g2_evidence_bundle_gate test_g2_evidence_audit test_g2_signal_lineage_gate
cd ../..
python research/implementation_execution/g2_evidence_audit.py --output /tmp/g2_audit_results.json
```

## سجل التنفيذ
- `4b1b9e3b058f2feb00393da881fd53e2bb9a5cab`: إعادة احتساب baseline.
- `2a1622bdacd8d7363495f4ac703288f121b758d6`: اختبارات سلبية جديدة.
- `555e134b48b32d08dfdda2479672732abb70eb9a`: تصحيح ترميز أسطر CSV في عينات الاختبار.
- `51e8be2f35e737ec23091fa94721ee6bbe41d275`: إصلاح اختبار الحقيقة المرجعية بعد تحديث المخطط.
- **CI VERIFIED:** نجح تشغيل GitHub Actions رقم `40322282979` على الالتزام البرمجي `51e8be2f35e737ec23091fa94721ee6bbe41d275`: اجتازت اختبارات بوابة G2 عدد **86/86**، وسجل التدقيق `g2_pass_candidates=0` و`g2_blocked=80`. رابط التشغيل: https://github.com/HazRob/HazRob-Tank/actions/runs/40322282979 .\n- نجح أيضاً تشغيل بوابة G2 رقم `40322290220` على الالتزام البرمجي نفسه. التزام هذا الملف توثيقي فقط؛ يلزم فحص CI الخاص به عند اكتماله.
