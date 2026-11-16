# G2 — فحص مجال دقة التصنيف وفترات الثقة (2026-10-26)

## نطاق التغيير
- المستودع: `HazRob/HazRob-Tank`.
- الفرع البحثي: `research/chemcore80-central-evidence-gap-analysis-20261021`.
- الملفات البرمجية: `research/implementation_execution/g2_evidence_audit.py` و`test_g2_evidence_audit.py`.
- لم يتغير ChemCore Stable ولا قواعد بيانات الإنتاج ولا عتبات الكشف الفعلية.

## نتيجة مراجعة الحالة السابقة
في التزام `44e8263d935e16cbe31633dd9afb5a78f3697151` كانت بوابة منع تسرب الجلسات الفيزيائية تعمل، وكانت السجلات `G2_INDEPENDENT_EVIDENCE.json` خالية من حزم مستقلة قابلة للتأهيل؛ **0/80 مرشح مراجعة، 80/80 محجوب**. نجاح CI السابق لا يعادل التحقق الفيزيائي.

## العيب والحل
كانت البوابة ترفض القيم غير المنتهية وتتحقق من شمول فترة الثقة للقيمة المعلنة، لكنها لا ترفض قيمة دقة تصنيف أو حد ثقة سالباً أو أكبر من 1 عندما يتطابق هذا الادعاء بين الملفات الموقعة بالتجزئة. أضيف فحص fail-closed للقيم `metric_value` و`baseline_metric_value` و`confidence_interval.low/high` **فقط عندما يكون `metric_name=classification_accuracy`**. رموز الرفض: `CLASSIFICATION_ACCURACY_OUT_OF_RANGE:<field>` و`CLASSIFICATION_INTERVAL_OUT_OF_RANGE:<field>`. لا يفرض مجال [0,1] على مقاييس مثل RMSE التي قد تتجاوز الواحد.

## الاختبارات القابلة لإعادة الإنتاج
أضيفت ثلاثة اختبارات وحدة تغطي خمسة سيناريوهات: دقة النموذج >1، دقة المرجع <0، حد ثقة أدنى <0، حد أعلى >1، وعدم تطبيق قيد دقة التصنيف على RMSE. تُستعمل بيانات اصطناعية للاختبار البرمجي فقط.

```bash
cd research/implementation_execution
python -m unittest -v test_g2_signal_lineage_gate test_g2_evidence_audit test_g2_evidence_bundle_gate test_g2_uci1081_signal_manifest
cd ../..
python research/implementation_execution/g2_evidence_audit.py --output /tmp/g2_audit_results.json
```

## إثبات CI
- الالتزام البرمجي: `094f801a9c871620510233366b8be938c30f512c`.
- GitHub Actions: https://github.com/HazRob/HazRob-Tank/actions/runs/40340229908
- النتيجة: **success**، `Ran 93 tests`، `OK`، وبوابة السجل **0/80 مرشح مراجعة و80/80 محجوب**.
- الأثر المرفوع: `tank-v4-g2-signal-r0-gate`، artifact ID `12658823333`.

## حدود الإثبات
فحص المجال يثبت اتساقاً رياضياً لازماً ولا يثبت صحة حدود الثقة أو طريقة حسابها أو استقلال الجلسات فيزيائياً. لا توجد بيانات غاز Tank مرجعية مستقلة جديدة. لا يجوز منح G2 أو G3 أو شهادة ppm أو سلامة مهنية بناء على هذا التعديل.

## متابعة
الخطوة التالية: تدقيق إعادة إنتاج فترة الثقة العنقودية من توقعات كل جلسة باستخدام بذرة وخوارزمية مثبتتين، ثم استكمال جمع أدلة خام بوحدات معروفة ومرجع تركيز مستقل وتقسيم جلسات أصلي.
