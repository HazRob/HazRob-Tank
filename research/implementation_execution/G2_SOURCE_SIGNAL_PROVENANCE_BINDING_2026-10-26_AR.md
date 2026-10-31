# G2 — ربط تأهيل المصدر وسلسلة الإشارة بالأدلة المحفوظة (2026-10-26)

## النطاق
المشروع: `HazRob/HazRob-Tank`، فرع البحث `research/chemcore80-central-evidence-gap-analysis-20261021` حصراً. لا تعديل لـChemCore Stable أو التطبيق أو البرامج الثابتة أو البيانات الخام. هذا فحص اتساق برمجي، لا تحقق كيميائي.

## الثغرة
كانت بوابة الحزمة تتحقق من SHA256 وسلامة بنية `source_qualification.json` و`signal_lineage.json`، لكن بوابة الاتساق لم تكن تقارن محتوياتهما بالسجل الرئيسي. لذلك يمكن نظرياً تقديم حزمة ذات تجزئات صحيحة ولكن ملف تأهيل مصدر يصف مصدراً اصطناعياً بينما السجل يزعم `INDEPENDENT_MEASURED`، أو ملف سلسلة إشارة يخالف `R0` و`Rs/R0` المعلنين. التطابق في التجزئات وحده لا يمنع التناقض الداخلي.

## التعديل
في `research/implementation_execution/g2_cross_artifact_gate.py`:
- قراءة الملفين بعد نجاح بوابة التجزئات.
- مقارنة `source_kind`, `dataset_uri`, `raw_sha256`, `raw_sha256_verified_from_bytes`, `source_raw_data_accessible` بالسجل.
- مقارنة `signal_processing`, `measurement_units`, `sensor_metadata` بالسجل.
- الحجب بالرمزين `SOURCE_QUALIFICATION_RECORD_MISMATCH` و`SIGNAL_LINEAGE_RECORD_MISMATCH` عند النقص أو الاختلاف.
- التحقق من قيم المصدر الخام ما يزال إثبات اتساق بين سجلات؛ لا يثبت بنفسه الحصول على القياس الفيزيائي الأصلي، ولا يغني عن مراجعة المرجع المستقل.

## الاختبارات
حُدّث مولد الحزمة الاصطناعية في `test_g2_evidence_bundle_gate.py` ليحتوي وثائق متسقة. أضيفت ثلاثة اختبارات (أربعة سيناريوهات) تشمل تبديل `source_kind` إلى `SYNTHETIC`، تغيير نمط R0 داخل ملف السلسلة، واستبدال أي من الملفين بمحتوى شكلي، **مع إعادة حساب SHA256 لكل ملف متلاعب به**. يجب أن تبقى الحزمة سليمة تجزئياً وأن تُحجب من بوابة الاتساق.

## إعادة الإنتاج
```bash
cd research/implementation_execution
python -m unittest -v test_g2_signal_lineage_gate test_g2_evidence_audit test_g2_evidence_bundle_gate test_g2_cluster_bootstrap_gate test_g2_uci1081_signal_manifest
cd ../..
python research/implementation_execution/g2_evidence_audit.py --output /tmp/g2_audit_results.json
```
التحقق الآلي: `.github/workflows/g2-signal-lineage-v4.yml`. لا تُعد الاختبارات الاصطناعية أدلة G2 علمية.

## الحالة
آخر التزام برمجي: `9cd2a8de3181f9b7426d6095ad03d129e8b43c5e`. نجح GitHub Actions على الالتزام البرمجي `9cd2a8de`، التشغيل [40395360002](https://github.com/HazRob/HazRob-Tank/actions/runs/40395360002): **115/115 اختباراً ناجحاً**، واجتاز تدقيق G2 الكامل: `g2_pass_candidates=0`, `g2_blocked=80`. أخفقت تشغيلات انتقالية على الالتزام الوسيط `40bf7a43` قبل تحديث الحزمة الاصطناعية؛ أصلحها الالتزام `9cd2a8de`. كان سجل `G2_INDEPENDENT_EVIDENCE.json` خالياً من السجلات المستقلة عند الفحص؛ التأهيل العلمي لا يزال **0/80**، وكل التقنيات محجوبة. لا ترقية لـG2/G3 أو تعديل لـStable.

## الخطوة التالية
مراجعة CI وتصحيح أي إخفاق. ثم تدقيق الربط المستقل لملفات `protocol_locked.json` و`review_decision.json` والأصل الخام الفعلي ومصدر معيار الغاز، مع اختبار مقاييس الأداء حسب نوعها دون منح ترقية تلقائية.
