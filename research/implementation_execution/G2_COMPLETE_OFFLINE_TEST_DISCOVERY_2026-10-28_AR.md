# G2 — توسيع تغطية اختبارات CI لجميع وحدات G2 البحثية (2026-10-28)

## نقطة الاستئناف ونطاق العمل
- فرع البحث فقط: `research/chemcore80-central-evidence-gap-analysis-20261021`.
- HEAD السابق: `74d13c87fbe5c9b84fc9ea23cffa65bb79260681`؛ 20/20 من تشغيلات CI السابقة ناجحة، وكانت بوابة G2 تشغل 233 اختباراً.
- تمت مراجعة فهرس المكتبة المركزية مباشرة من `HazRob/build-harness/library/reference-bank/external-research/master_catalog.json`؛ Git blob SHA1: `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f`، 80 مدخلاً و14 مجموعة بيانات خارجية.
- سجل الأدلة المستقلة: `G2_INDEPENDENT_EVIDENCE.json` يحوي `records: []`. مصفوفة المصادر المثبتة: 0/14 جاهزة للمراجعة و14/14 محجوبة. لا يُعدّ أي اختبار اصطناعي قياساً فيزيائياً.

## الفجوة المثبتة
كانت بوابة `.github/workflows/g2-signal-lineage-v4.yml` تشغّل خمس وحدات اختبار محددة بالأسماء فقط، ولا تستدعي ثلاث وحدات G2 موجودة بالفعل:
- `test_g2_mq01_fileday_benchmark.py` (10 اختبارات للمقارنة المقترنة وحجب اليوم والتبديل وإزالة الحساسات).
- `test_g2_zenodo_mq01_raw_inventory.py` (5 اختبارات للتحقق من البايتات وMD5/SHA256 وCRC وسلامة ZIP).
- `test_g2_mendeley_mq14_source_probe.py` (4 اختبارات للتحقق من بيانات المصدر وعدم منح تأهيل بلا قياسات).
وبالتالي كان بالإمكان إدخال خلل في هذه الوحدات من دون أن تكشفه بوابة G2 الأساسية.

## التنفيذ
- استبدال قائمة الوحدات اليدوية بأمر الاكتشاف الشامل:
  `python -m unittest discover -s research/implementation_execution -p 'test_g2_*.py' -v`.
- توسيع `compileall` ليشمل مجلد وحدات البحث بالكامل قبل الاختبار.
- إضافة `test_g2_workflow_test_coverage.py` باختبارين يتحققان من شمول اكتشاف الوحدات التسع ومن خطوة الترجمة ومسارات البيانات الخام.
- التزام تعديل سير العمل: `dac4b36415d4162a09a1213314747aa71f07cdc7`.
- التزام إضافة الاختبارات: `1608e7c0fc67a78b3ba03fafbbf92f7f749c9902`.

## إعادة الإنتاج ونتيجة CI
من جذر المستودع:
```sh
python -m compileall -q research/implementation_execution
python -m unittest discover -s research/implementation_execution -p 'test_g2_*.py' -v
python research/implementation_execution/g2_evidence_audit.py --output /tmp/g2_audit_results.json
```
- GitHub Actions: https://github.com/HazRob/HazRob-Tank/actions/runs/40715630169
- الوظيفة: `122912402190`.
- النتيجة: **254/254 اختباراً ناجحاً** (زيادة 21 اختباراً عن الـ233 السابقة: 19 اختباراً كان مستبعداً واختباران جديدان).
- تدقيق التقنيات الثمانين: `g2_pass_candidates=0`، `g2_blocked=80`. خرج تدقيق قابل لإعادة الإنتاج محفوظ ضمن artifact باسم `tank-v4-g2-signal-r0-gate`.
- لا تتضمن الاختبارات أي تأهيل علمي تلقائي؛ تبقى جميع النتائج `G2_BLOCKED` إلى حين توفر أدلة مستقلة مؤهلة.

## حدود النتائج
- تغيير سير العمل واختبار التغطية وتوثيق الجولة فقط على فرع البحث؛ لا تعديل لـChemCore Stable أو تطبيق Android أو ESP32/ESP32-CAM.
- نجاح CI يؤكد سلامة الاختبارات البرمجية المشمولة، لا صحة تعيين الغازات ولا معايرة ppm.
- يتعذر إكمال G2 علمياً من دون ملفات قياس خام مستقلة من تعرضات فيزيائية أصلية، ومراجع تركيز قابلة للتتبع، ومعرّفات أجهزة/جلسات/تعرضات، وتقسيمات train/calibration/blind test محجوبة، وتقييم مستقل.
- لا ترقية G2 أو G3، ولا تعديل لملفات الأدلة المستقلة.
