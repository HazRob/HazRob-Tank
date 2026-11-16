# G2 — إلزام نموذج الأغلبية بجميع مجموعات التدريب المعلنة

التاريخ: 2026-10-27. المشروع: HazRob Tank. النطاق: فرع البحث `research/chemcore80-central-evidence-gap-analysis-20261021` فقط.

## حالة الاستئناف
- الالتزام المرجعي: `fa13fa77fe6f3ec31e5c6d4a25f3bcfa415d5584`؛ كانت مسارات GitHub Actions الأحد عشر ناجحة عليه.
- سجل `G2_INDEPENDENT_EVIDENCE.json` لا يزال يحوي `records: []`. البيانات الخارجية المرجعية ليست قياسات مستقلة على دبابة HazRob ولا تمنح اعتماد ppm.

## الفجوة والإصلاح
- كانت بوابة `majority_train` تسمح بأن تكون `baseline_model.fitted_group_ids` مجموعة جزئية من `train_group_ids`. هذا يسمح باستبعاد بعض مجموعات التدريب لإضعاف خط الأساس، مع بقاء تجزئات الملفات صحيحة.
- أصبح النموذج ملزماً باستخدام جميع معرّفات مجموعات التدريب المعلنة، دون اشتراط ترتيب محدد. عند عدم التطابق: `BASELINE_MAJORITY_TRAIN_PARTIAL_FIT`.
- لم تُغيّر قواعد فصل التدريب/المعايرة/الاختبار، أو تعريف G2 بوصفه تأهيلاً مشروطاً بأدلة مستقلة.

## اختبارات الانحدار
- اختبار سلبي `test_majority_train_rejects_partial_training_group_fit`: مجموعتا تدريب معلنتان، لكن النموذج يستخدم إحداهما فقط بعد إعادة تجزئة الملفات؛ النتيجة المطلوبة `G2_BLOCKED`.
- اختبار إيجابي `test_majority_train_full_training_group_fit_replays`: المجموعتان مستخدمتان، وتجري إعادة حساب SHA256 لتسميات التدريب؛ النتيجة `G2_PASS_CANDIDATE_REQUIRES_REVIEW` في بيانات اصطناعية فقط.
- الشيفرة: `research/implementation_execution/g2_cross_artifact_gate.py`؛ الاختبارات: `research/implementation_execution/test_g2_evidence_bundle_gate.py`.
- التزامات التنفيذ: `5cf089a76413f8007309b42f6e0ced362fc3e766` و`4bed446c9925c1d0610cdc4d5dcf6d4a911987c7`.

## إعادة الإنتاج
```bash
cd research/implementation_execution
python -m unittest -v test_g2_signal_lineage_gate test_g2_evidence_audit test_g2_evidence_bundle_gate test_g2_cluster_bootstrap_gate test_g2_uci1081_signal_manifest
cd ../..
python research/implementation_execution/g2_evidence_audit.py --output /tmp/g2_audit_results.json
```

## حدود الدليل
الاختبارات برمجية اصطناعية ولا تثبت الهوية الكيميائية أو معايرة الحساسات أو دقة التركيز. يتطلب اكتمال G2 قياسات خام مستقلة بمرجع فيزيائي ووحدات ومعرّفات أجهزة وجلسات وتقييم محجوب قابل للإعادة. لا تعديل على ChemCore Stable أو التطبيق أو الفريموير، ولا اعتماد G2 أو G3.


## نتيجة CI وإصلاح اختبار الانحدار
- كشف تشغيل G2 على الالتزام `4bed446c9925c1d0610cdc4d5dcf6d4a911987c7` إخفاقاً في الاختبار الإيجابي فقط: `SPLIT_MANIFEST_RECORD_MISMATCH`، لأن بيانات الاختبار الاصطناعية أضافت مجموعة تدريب إلى السجل والبروتوكول ولم تضفها إلى `split_manifest.json`. لم يكن إخفاقاً في بيانات فيزيائية أو في خوارزمية G2.
- صُحح الاختبار الاصطناعي بمزامنة `split_manifest.json` مع مجموعات التدريب. الالتزام الإصلاحي: `0b7a71023bc6561092023209e6fa548b454d1cd3`.
- نجح تشغيل G2 `40488033366` (الوظيفة `122134650528`) بنتيجة **167/167 اختباراً**، بما فيها الاختباران الجديدان، وأظهر التدقيق `g2_pass_candidates=0` و`g2_blocked=80`.
- فُحصت GitHub Actions على الالتزام الإصلاحي `0b7a7102`: **12/12 مساراً ناجحاً**، دون إخفاقات معلقة. تضم القائمة المسارات البحثية الثمانية، بوابة G2، والتحقق المستقل G1، ومساري المصدر الخارجي.
- مقارنة الالتزام المرجعي `fa13fa77` مع `0b7a7102` أظهرت تغيير ثلاثة ملفات فقط، كلها داخل `research/implementation_execution/`؛ لا تعديل على ChemCore Stable.
