# G2 — تشخيص أثر تكرار قراءات العينة الفيزيائية على المقارنة المقترنة (30 أكتوبر 2026)

## الأساس والمراجع
- نقطة الاستئناف المراجعة: `8f667942a4139bdc4d8f8b98d0c897b81faa4ff7`، مع نجاح 20/20 تشغيل CI سابقاً.
- فهرس المراجع المركزي المثبت: `HazRob/build-harness/library/reference-bank/external-research/master_catalog.json`، Git blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f`؛ 80 مرجعاً و14 مجموعة بيانات خارجية.
- حالة الأهلية: `G2_CENTRAL_CATALOG_SOURCE_READINESS_2026-10-28.json` يقرر 0/14 مؤهلة؛ سجل `G2_INDEPENDENT_EVIDENCE.json` بلا سجلات مستقلة. لا يجوز اعتبار بيانات UCI أو الأمثلة الاصطناعية تحققاً فيزيائياً للروبوت.

## الفجوة
كان `g2_paired_prediction_replay.py` يتحقق من أن `physical_sample_id` لا يتكرر بين المجموعات المعلنة، ويحصي القراءات المتكررة، لكنه كان يعرض فقط دقة/فارقاً موزوناً بعدد صفوف التعرض. إذا تكررت قراءة عينة واحدة كثيراً، فقد تهيمن على النتيجة المجمعة رغم عدم زيادة عدد العينات الفيزيائية المستقلة.

## التحسين
عند توفر `physical_sample_id` لكل صف، يعاد حساب فرق دقة النموذج وخط الأساس لكل عينة على حدة ثم يؤخذ **متوسط متساوي الوزن للعينات المعلنة**، بالإضافة إلى الفرق التقليدي الموزون بعدد القراءات. يتضمن الحقل `declared_physical_sample_equal_weight_diagnostic` النتائج الكلية وبحسب المجموعة، ويكشف انعكاس إشارة الفارق بين الطريقتين، ويحسب أقصى عدد قراءات للعينة. يُرفض أي تناقض في تجميع أعداد الصفوف أو الإجابات الصحيحة بالرمز `PAIRED_REPLAY_SAMPLE_RECONCILIATION_INVALID`. عندما يغيب عمود العينات تكون قيمة التشخيص `null`، ولا تُختلق أوزان أو استقلالية غير موثقة.

**حدود التفسير:** هذه أوزان وصفية للعينات *المعلنة* وليست تصحيحاً تلقائياً لفواصل الثقة، ولا تحققاً من هوية العينة أو تسرب التدريب إلى الاختبار. لا تغير قواعد ترقية G2/G3 ولا تنشئ قياسات غاز حقيقية.

## التعديل والاختبارات
- شيفرة البحث: `research/implementation_execution/g2_paired_prediction_replay.py`؛ الالتزام `da60163097ed6f8f8c579a3aa4bd14068d6b6b08`.
- أربعة اختبارات انحدار اصطناعية في `test_g2_paired_prediction_replay.py`: انعكاس الإشارة بسبب تسع قراءات لعينة واحدة مقابل عينتين أخريين؛ تطابق المتوسطين عند قراءة واحدة لكل عينة؛ ثبات النتائج عند عكس ترتيب الصفوف ومعرفات عربية؛ وغياب التشخيص عند عدم وجود معرف العينة.
- إصلاح تمثيل نهاية أسطر CSV في تجهيز الاختبارات: `7644188c60a9d5ab9920fd85a564c4fbc4e27b9d`.

## إعادة الإنتاج
```bash
python -m compileall -q research/implementation_execution
python -m unittest discover -s research/implementation_execution -p 'test_g2_*.py' -v
python research/implementation_execution/g2_evidence_audit.py --output /tmp/g2_audit_results.json
python research/implementation_execution/g2_acquisition_triage.py \
  --audit /tmp/g2_audit_results.json \
  --source-matrix research/implementation_execution/G2_CENTRAL_CATALOG_SOURCE_READINESS_2026-10-28.json \
  --output /tmp/g2_acquisition_triage.json
```

## ضوابط النشر العلمي
كل الملفات المعدلة ضمن `research/implementation_execution/` في فرع البحث فقط. لا تغيير في ChemCore Stable أو Android أو الفريموير أو الفرع `main`. لا اعتماد علمي لأي تقنية قبل تحقق مصادر القياس الفيزيائية الأصلية، والمعايير المرجعية القابلة للتتبع، واستقلال مجموعات الاختبار المحجوبة.
