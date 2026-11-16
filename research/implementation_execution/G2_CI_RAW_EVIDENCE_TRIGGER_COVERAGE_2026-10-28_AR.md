# G2 — سد فجوة تشغيل CI عند تغير بايتات البيانات الخام أو سجل المعرفات (2026-10-28)

## الحالة السابقة والمصدر المرجعي
- فرع البحث: `research/chemcore80-central-evidence-gap-analysis-20261021`، نقطة الانطلاق `c83bfa559e5534fd206e4578b61a51fe3a678b21`؛ تشغيلات CI السابقة ناجحة.
- فُحصت مكتبة المراجع المركزية مباشرة في `HazRob/build-harness/library/reference-bank/external-research/master_catalog.json`، وثبت تطابق Git blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f`، 80 سجلاً و14 مجموعة بيانات خارجية.
- سجل الأدلة المستقلة `G2_INDEPENDENT_EVIDENCE.json` لا يزال `records: []`، ولا يوجد على الفرع الحالي مسار `research/g2/raw/` أو `research/g2/evidence/` بملفات قياس فعلية. مصفوفة المصادر 0/14 جاهزة للمراجعة، وجميع التقنيات 80/80 محجوبة علمياً.

## الفجوة القابلة لإعادة الإنتاج
كانت بوابة `.github/workflows/g2-signal-lineage-v4.yml` تستدعى عند تغيير `research/g2/evidence/**`، لكنها لا تستدعى عند تعديل بايتات الأرشيفات في `research/g2/raw/**` أو تعديل سجل المعرفات الأصلي `research/PROGRESS_80.json` أو مصفوفة الجاهزية المثبتة. وهذا يسمح بأن يتغير مدخل مهم للتدقيق من دون تشغيل البوابة آلياً على push/PR، حتى وإن كانت دوال SHA256 نفسها صحيحة.

## التنفيذ
- أُضيفت إلى مرشحات المسارات **لكل من push وpull_request** ثلاثة مسارات: `research/g2/raw/**`، `research/PROGRESS_80.json`، `research/implementation_execution/G2_CENTRAL_CATALOG_SOURCE_READINESS_2026-10-28.json`.
- احتُفظ بمسار `research/g2/evidence/**` السابق. لم تتغير شروط التأهيل العلمي، ولم تُمسّ شيفرة ChemCore Stable أو Android أو ESP32.
- أُضيف اختبار انحدار `test_raw_archive_registry_and_pinned_source_matrix_trigger_g2_ci` إلى `test_g2_evidence_audit.py` ليفحص وجود المسارات في قسمي push وpull_request، كي لا تُحذف بالخطأ.
- الالتزام الذري للشيفرة والاختبار وسير العمل: `d6e1c1a15823affe419f12d1b52160a3f9fd711d`. التغيير مقتصر على ملف اختبار داخل research وملف سير عمل **بحثي** في .github/workflows على **فرع البحث فقط**؛ لم يتغير الفرع الرئيسي.

## الاختبار ونتائج GitHub Actions
- [تشغيل بوابة G2 رقم 40704942493](https://github.com/HazRob/HazRob-Tank/actions/runs/40704942493)، الوظيفة `122874400293`: **233/233 اختباراً ناجحاً**؛ الاختبار الجديد ناجح؛ `g2_pass_candidates=0` و`g2_blocked=80`.
- جميع **21/21** تشغيلات CI المرتبطة بالالتزام `d6e1c1a15823affe419f12d1b52160a3f9fd711d` اكتملت بنجاح، ولا توجد إخفاقات متبقية.
- لإعادة الإنتاج: `python -m unittest -v test_g2_evidence_audit` من `research/implementation_execution`، أو `python -m unittest discover -s research/implementation_execution -p 'test_g2_*.py' -v` من جذر المستودع، ثم `python research/implementation_execution/g2_evidence_audit.py --output /tmp/g2_audit_results.json`.

## القيود والخطوة العلمية التالية
هذه معالجة **لفجوة جدولة الاختبارات** وليست تحققاً من دقة ppm أو تمييز الغازات أو استقلالية التعرضات. لا تكفي نتائج UCI أو الاختبارات الاصطناعية لتأهيل أي تقنية G2/G3. يلزم أرشيف خام مستقل لقياسات غازات فعلية، ومعايير تركيز مرجعية قابلة للتتبع، ومعرّفات أجهزة/حساسات/جلسات/تعرضات أصلية، وتقسيمات train/calibration/blind test مغلقة، مع مراجعة مستقلة قبل أي ترقية.
