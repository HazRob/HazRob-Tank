# G2 — منع تسرب الجلسات عبر الأسماء البديلة لمجموعات القياس (2026-10-26)

## نطاق التعديل
مستودع HazRob-Tank، فرع `research/chemcore80-central-evidence-gap-analysis-20261021` فقط. لم تُمس ChemCore Stable ولا معايرة Tank ولا بيانات الإنتاج. يخص التعديل بوابة المراجعة البرمجية لأدلة G2، وليس اعتماد الغاز أو ppm.

## العيب المكتشف
كانت `g2_cross_artifact_gate.py` تتحقق من انفصال أسماء `group_id` بين `train/calibration/test`، لكنها لا تربط تلك الأسماء بالأعمدة الأصلية التي يدّعي سجل الدليل أنها تحدد الوحدة الفيزيائية المستقلة. مثال: `train_group_id=A` و`test_group_id=B` قد يشيران إلى `device_id=d1, session_id=s1` نفسه. عندئذ لا يكشف فحص اختلاف أسماء المجموعات تسرب الجلسة الفيزيائية.

## الإصلاح البرمجي
- تحليل `grouping_source_field` من سجل الأدلة كأسماء أعمدة مصدرية، مثل `device_id,session_id`.
- اشتراط وجود هذه الأعمدة فعلياً في `group_manifest.csv` وعدم خلو قيمها لكل تعرض.
- التحقق من علاقة واحد-لواحد بين `group_id` والزوج/المجموعة الفيزيائية الأصلية؛ رفض إعادة تسمية الجلسة الفيزيائية نفسها باسم مجموعة آخر، ورفض إسناد مجموعة واحدة إلى أكثر من وحدة فيزيائية.
- الرموز: `GROUPING_SOURCE_COLUMNS_INVALID`، `GROUPING_SOURCE_COLUMNS_MISSING`، `GROUPING_SOURCE_VALUE_MISSING`، `GROUP_ID_MAPS_TO_MULTIPLE_PHYSICAL_UNITS`، `PHYSICAL_GROUP_ALIAS_ACROSS_SPLITS`.
- أربع حالات سلبية جديدة وبيانات اصطناعية موجبة بعد إضافة `device_id,session_id`، لا تعتبر بيانات غاز حقيقية.

## حدود الإثبات
تطابق قيم الأعمدة في الملفات لا يثبت أصالتها؛ يلزم مطابقتها مع البيانات الخام ودفاتر المختبر ومعرفات الجهاز والجلسة من المصدر المستقل. لا يمنع هذا الفحص كل أنواع التسرب (مثل القياسات المرتبطة بيوم/حملة واحدة إذا كان تعريف الاستقلال خاطئاً)، ولا يثبت صلاحية R0 أو ppm. حالة `G2_PASS_CANDIDATE_REQUIRES_REVIEW` مراجعة أولية فقط، وليست اعتماداً علمياً.

## إعادة الإنتاج
```bash
cd research/implementation_execution
python -m unittest -v test_g2_evidence_bundle_gate test_g2_evidence_audit test_g2_signal_lineage_gate
cd ../..
python research/implementation_execution/g2_evidence_audit.py --output /tmp/g2_audit_results.json
```

## خط الأساس والنتيجة
قبل التعديل: `G2_INDEPENDENT_EVIDENCE.json` لا يحوي أي سجل قياس مؤهل، 0/80 مرشح و80/80 محجوب. الاختبارات البرمجية والـCI تثبت صحة قواعد التحقق فقط، ولا تولد بيانات فيزيائية مفقودة.

## الملفات المتغيرة
- `research/implementation_execution/g2_cross_artifact_gate.py`
- `research/implementation_execution/test_g2_evidence_bundle_gate.py`
- هذا التقرير.
