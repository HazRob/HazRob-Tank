# G2 — منع مرور مقاييس لا يمكن إعادة حسابها من الأدلة المحجوبة (2026-10-26)

## السياق والنطاق
- المشروع: HazRob Tank، تأهيل G2 للتقنيات الثمانين.
- فرع العمل الوحيد: `research/chemcore80-central-evidence-gap-analysis-20261021`.
- متابعة بوابة `G2_CLUSTER_BOOTSTRAP_RECOMPUTATION_GATE_2026-10-26_AR.md`.
- لا تعديل على ChemCore Stable أو تطبيق Android أو البرامج الثابتة أو البيانات الخام.

## الثغرة المؤكدة
في `g2_cross_artifact_gate.py` تُعاد حسابات `classification_accuracy` من `predictions_blind.csv` مع إعادة حساب `baseline` وفترة الثقة المجمّعة. لكن عند تغيير `metric_name` إلى `rmse` أو `macro_f1` أو `balanced_accuracy` أو اسم مخصص، كانت البوابة تتحقق فقط من تطابق الملخصات وتجزئات الملفات دون إعادة حساب المقياس من القياسات والتنبؤات. لذلك كان يمكن نظرياً الوصول إلى `G2_PASS_CANDIDATE_REQUIRES_REVIEW` بمقياس غير قابل للتحقق حسابياً، رغم عدم حصول أي تقنية على هذا الوضع فعلياً.

## الإصلاح البرمجي
- أُضيف مانع `METRIC_FAMILY_NOT_RECOMPUTABLE` لكل `metric_name` غير `classification_accuracy` ضمن بوابة الاتساق بين ملفات الأدلة.
- لا تُفسَّر النتيجة على أنها بطلان مقاييس RMSE/F1 علمياً؛ بل منع قبولها قبل بناء تدقيق خاص يحسبها من عينات الاختبار المحجوب، مع عدم اليقين المناسب.
- أضيف اختبار سلبي مكرر لأربعة أسماء مقاييس، مع إنشاء حزمة اصطناعية مكتملة (14 ملفاً) وتحديث SHA256 للملخص بحيث يثبت الاختبار أن مجرد تطابق التجزئات لا يكفي.
- اختبار الحزمة السليمة لمقياس `classification_accuracy` يبقى مرشحاً للمراجعة فقط، وليس اعتماداً علمياً.

## قابلية إعادة الإنتاج
```bash
cd research/implementation_execution
python -m unittest -v test_g2_evidence_bundle_gate test_g2_evidence_audit test_g2_cluster_bootstrap_gate
cd ../..
python research/implementation_execution/g2_evidence_audit.py --output /tmp/g2_audit_results.json
```
تغطي GitHub Actions `.github/workflows/g2-signal-lineage-v4.yml` اختبارات الأدلة والبوابة الكاملة للثمانين.

## سجل التنفيذ والتحقق
- تعديل البوابة: `39922bff8422cb307882fcf668dc12223294d1b3`.
- إضافة الاختبارات: `158841c8f3ee211ef5a4183f84e51816febd0078`.
- نجاح CI على الالتزام الثاني: GitHub Actions run `40376495245`، رابط: https://github.com/HazRob/HazRob-Tank/actions/runs/40376495245 .
- هذه الاختبارات اصطناعية للتحقق من سلامة البرمجيات، وليست قياسات غاز مستقلة.

## الحالة العلمية وخطوات المتابعة
لا تزال `G2_INDEPENDENT_EVIDENCE.json` تحتوي `records=[]`؛ النتيجة `0/80` مؤهلة و`80/80` محجوبة. لا يمكن ترقية أي تقنية دون مصدر غاز مرجعي مستقل ومعرفات جلسات أصلية ووحدات إشارة مثبتة واختبار محجوب. لإتاحة المقاييس الأخرى يلزم تحديد نوع الهدف، وتوثيق المدخلات الأصلية، وإعادة حساب كل مقياس وفترته من بيانات الاختبار مع اختبارات حالات سلبية مناسبة، ثم مراجعة علمية مستقلة.
