# G2 V4 — إعادة حساب دقة التصنيف من التنبؤات المحجوبة (2026-10-26)

## النطاق
- المستودع: `HazRob/HazRob-Tank`؛ الفرع البحثي `research/chemcore80-central-evidence-gap-analysis-20261021`.
- التعديل محصور في `research/implementation_execution/`، ولا يمس ChemCore Stable أو التطبيق أو الفريموير.
- المرجعية: `G2_SOURCE_PROMOTION_GATE_2026-10-26_AR.md`، و`G2_CROSS_ARTIFACT_CONSISTENCY_GATE_2026-10-26_AR.md`.

## المشكلة التي كشفتها المراجعة
قبل هذا الإصلاح كان بإمكان حزمة ذات SHA256 صحيح أن تقدم `metric_value` متطابقة بين سجل الأدلة و`metrics_summary.json` دون إثبات أنها حُسبت من `predictions_blind.csv`. كما لم يكن `correct_count` لكل مجموعة أو صدق التسمية المرجعية في التنبؤات متحققاً منه. سلامة التجزئة واتساق ملفين لا يكفيان لصحة حساب المقياس.

## التحسين
عند `metric_name=classification_accuracy` تشترط `g2_cross_artifact_gate.py` الآن:
1. `group_manifest.csv`: أعمدة `exposure_id,group_id,gas_reference_id`.
2. `predictions_blind.csv`: أعمدة `exposure_id,group_id,true_reference,predicted_reference`.
3. `metrics_per_group.csv`: أعمدة `group_id,sample_count,correct_count`.
4. تطابق `true_reference` لكل تعرض مع `gas_reference_id` في سجل التعرضات.
5. إعادة حساب الدقة = عدد التنبؤات المطابقة / عدد التنبؤات المحجوبة، ومقارنتها بالقيمة المسجلة بدقة عددية صارمة.
6. إعادة حساب `correct_count` لكل مجموعة، مع رفض السالب أو القيم الأكبر من حجم المجموعة.

رموز الرفض: `ACCURACY_RECOMPUTATION_FIELDS_MISSING`، `BLIND_PREDICTION_TRUTH_MISMATCH`، `BLIND_ACCURACY_RECOMPUTATION_MISMATCH`، `PER_GROUP_CORRECT_COUNT_MISMATCH`.

## اختبارات قابلة للإعادة
أُضيفت أربعة ضوابط سلبية في `test_g2_evidence_bundle_gate.py`: رفع دقة إجمالية اصطناعية إلى 100% مع تحديث JSON والتجزئات؛ تزوير الحقيقة المرجعية؛ تضخيم عدد الصحيح في مجموعة؛ حذف أعمدة الحقيقة/التنبؤ. عُدلت العينة الاصطناعية الإيجابية إلى تعرضين اختباريين أحدهما صحيح والآخر خاطئ (1/2 = 50%)؛ هذه بيانات اختبار برمجي **وليست قياسات غاز**.

```bash
cd research/implementation_execution
python -m unittest -v test_g2_signal_lineage_gate test_g2_evidence_audit test_g2_evidence_bundle_gate test_g2_uci1081_signal_manifest
cd ../..
python research/implementation_execution/g2_evidence_audit.py --output /tmp/g2_audit_results.json
```

## حالة التحقق وحدود الاستنتاج
- الالتزامات: `0f1a445d` (حساب الدقة)، `2e290052` (الضوابط)، `ef013629` (ضبط العينة)، `27d4b80d` (تصحيح صيغة CSV).
- **CI VERIFIED:** نجح تشغيل GitHub Actions رقم `40315089183` على الالتزام البرمجي `27d4b80d95f9e136bc966428cd19a182a42468d7` بتاريخ 2026-10-26، بما في ذلك تنفيذ الضوابط السلبية، وتدقيق نطاق 80 تقنية، ورفع ملف نتائج البوابة `tank-v4-g2-signal-r0-gate`. الرابط: https://github.com/HazRob/HazRob-Tank/actions/runs/40315089183 .
- السجل المركزي `G2_INDEPENDENT_EVIDENCE.json` ما يزال `records=[]`؛ الحالة العلمية **0/80 مرشحاً و80/80 محجوباً**.
- التحقق الحسابي هنا يخص `classification_accuracy` فقط؛ `macro-F1` و`balanced_accuracy` وppm تتطلب إعادة حساب منفصلة ذات تعريف مسبق، ولم تُعتمد ضمن هذا التغيير.
- تطابق التنبؤات مع سجل التعرضات لا يثبت أن `gas_reference_id` صحيح فيزيائياً أو أن المجموعات مستقلة؛ يتطلب ذلك تدقيق مصدر وتجارب مستقلة.
