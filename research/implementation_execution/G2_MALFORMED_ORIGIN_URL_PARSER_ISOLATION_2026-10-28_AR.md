# G2 — عزل أخطاء تحليل روابط المصادر الخارجية (2026-10-28)

## نطاق التعديل
تحسين بحثي في فهرسة المصادر المركزية لمشروع HazRob Tank، وليس قياساً فيزيائياً أو ترقية علمية. التعديل محصور في فرع `research/chemcore80-central-evidence-gap-analysis-20261021` داخل `research/implementation_execution/`. لم تُعدّل ChemCore Stable أو ملفات Android/ESP32.

## الثغرة القابلة لإعادة الإنتاج
كان `audit_source_catalog` يبني عدّاد هويات المصادر من خلال `_canonical_source_origins` قبل تنفيذ فحص `_source_origin_blockers`. عند وجود عنوان URL يحوي قوس IPv6 غير مغلق مثل `https://[bad`، أو محارف تؤدي إلى خطأ NFKC في اسم المضيف، تُطلق `urllib.parse.urlsplit` الاستثناء `ValueError` وتوقف تدقيق جميع مصادر الفهرس، بدلاً من حجب السجل المعيب وحده. تجزئة المصدر الصحيحة لا تضمن صحة تركيب الرابط.

## الإصلاح
في `g2_reference_catalog_readiness.py` أصبح تحليل الرابط واسم المضيف داخل `_canonical_source_origin` محاطاً بالتقاط `ValueError`، وإرجاع `None` لهوية غير قابلة للتحليل. عندها تسجل `_source_origin_blockers` السبب `ORIGINAL_SOURCE_ORIGIN_MALFORMED`، ويستمر تصنيف بقية السجلات دون اعتبار العنوان المعيب أصلاً مستقلاً. يبقى التحقق من المنافذ والأصل والبيانات الخام إلزامياً.

## الاختبارات
أضيف اختباران في `test_g2_evidence_audit.py`: الأول يغطي ثلاثة عناوين غير سليمة في حقلي `primary_url` و`doi` (ست حالات)، ويتحقق من حجب السجل المتأثر دون تلويث حالة السجل المجاور؛ والثاني يثبت عدم تصنيف DOI صالح ورابط صحيح على أنهما مشوهان. جميع بيانات الاختبار اصطناعية ولا تُحتسب تجارب مستقلة.

إعادة الإنتاج:
```bash
cd research/implementation_execution
python -m unittest -v test_g2_evidence_audit
python -m unittest -v test_g2_signal_lineage_gate test_g2_evidence_audit test_g2_evidence_bundle_gate test_g2_cluster_bootstrap_gate test_g2_uci1081_signal_manifest
python g2_evidence_audit.py --output /tmp/g2_audit_results.json
```

التزام الشيفرة: `b8f96d0e8345d3838f68354e5a16842c913f9665`. التزام الاختبارات: `f86734d9fc1faa3e70866cbeab56fd39fa5da6b4`. تحقق CI: [GitHub Actions #40650146981](https://github.com/HazRob/HazRob-Tank/actions/runs/40650146981) — **226/226 اختباراً ناجحاً**؛ `g2_pass_candidates=0` و`g2_blocked=80`. ينبغي إعادة التحقق من اكتمال بقية تشغيلات CI بعد أي التزام توثيقي لاحق.

## حدود الاستنتاج
سجل الأدلة المستقلة `G2_INDEPENDENT_EVIDENCE.json` بقي `records: []`، ومصفوفة مصادر القياسات الخارجية المثبتة `0/14` مؤهلة لمراجعة المصدر و`14/14` محجوبة. سلامة تحليل الروابط لا تثبت هوية الغاز أو ppm أو استقلال التعرضات أو صلاحية معايرة Tank؛ لا يوجد اعتماد G2 أو G3.
