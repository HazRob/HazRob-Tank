# G2 — رفض تجاوز المجال العددي في ملفات أدلة JSON الموثقة بالتجزئة

التاريخ: 2026-10-27 | المشروع: HazRob Tank | النطاق: فرع البحث فقط

## الحالة المرجعية
- نقطة البداية: `2eb5f4ebeaffee014d7336df73852dd8ff19ab24`.
- على نقطة البداية نجحت مسارات GitHub Actions الأحد عشر، ومنها G2 `40455219642` بعد نجاح 155 اختباراً، دون ترقية علمية.
- سجل `research/implementation_execution/G2_INDEPENDENT_EVIDENCE.json` ما زال يحتوي `records: []`. النتيجة المرجعية: 0/80 مرشحين و80/80 محجوبين.
- البيانات الخارجية (مثل UCI) ليست بديلاً عن مراجع قياس فيزيائية مؤهلة ومطابقة لمصفوفة Tank MQ8.

## المشكلة والتحسين
كان `g2_evidence_bundle_gate.py` يمنع المفاتيح المكررة وNaN/Infinity الحرفية في ملفات JSON، لكنه يستخدم `json.loads` دون `parse_float`. يسمح Python بتحويل العدد JSON الصحيح نحوياً `1e9999` أو `-1e9999` إلى قيم لا نهائية. عند تحديث SHA256 للملف، كان ممكناً قبول حاوية JSON تحتوي على قيمة عددية غير منتهية، حتى لو كان موضعها داخل حقل إضافي أو بنية متداخلة.

أُضيف محلل `finite_float` إلى قراءة ملفات JSON داخل الحزمة. يرفض المحلل أي قيمة عشرية تتحول إلى `inf` أو `-inf`، وتعيد البوابة رمز `INVALID_EVIDENCE_FILE_CONTENT:<filename>`. لا يتغير قبول الأعداد المنتهية.

أُضيف اختبار انحدار بمعطيات اصطناعية يعيد حساب SHA256 بعد إدراج أعداد `1e9999` و`-1e9999` داخل `raw_manifest.json` و`ci_run.json` و`uncertainty_grouped.json` (ست حالات فرعية). يختبر كل حالة رفض الحزمة ورفض ترقية سجل G2. ليست هذه الحالات أدلة غازية أو نتائج معايرة.

## إعادة الإنتاج
من جذر المستودع:
```bash
cd research/implementation_execution
python -m unittest -v test_g2_signal_lineage_gate test_g2_evidence_audit test_g2_evidence_bundle_gate test_g2_cluster_bootstrap_gate test_g2_uci1081_signal_manifest
cd ../..
python research/implementation_execution/g2_evidence_audit.py --output /tmp/g2_audit_results.json
```

## الملفات
- `research/implementation_execution/g2_evidence_bundle_gate.py`
- `research/implementation_execution/test_g2_evidence_bundle_gate.py`
- هذا التقرير.

التزامات الشيفرة والاختبار: `70623e349d614f30160799bf89359834c31f8c6e`، `e6348b530d52eb9a34f48d6a65d35813a90144ac`، `618ea87c72f9dbd52236f55d3334e1c82bba361e`.

## حدود الادعاء
- الفحص برمجي متعلق بسلامة الحاوية والتجزئة؛ لا يصادق على مصدر القياسات أو هوية الغاز أو ppm.
- لا تعديل على ChemCore Stable، ولا تغيير لحالة G2/G3.
- لا ترقية قبل قياسات مستقلة ذات مراجع فيزيائية ووحدات موثقة، وعزل مجموعات التدريب/المعايرة/الاختبار، ومراجعة مستقلة.
- نتائج CI الخاصة بالالتزام النهائي تُسجل بعد اكتمال GitHub Actions؛ لا تُفترض نتيجة النجاح من مجرد تشغيل المهمة.
