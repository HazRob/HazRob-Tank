# G2 — مصفوفة حصر نواقص الأدلة وإجراءات اقتنائها (28 أكتوبر 2026)

## خط الأساس المراجع
- نقطة الاستئناف: `34b89d16163f4c8e58b6afece28b6d2a2482a470`، مع نجاح CI السابق وعدم وجود اعتماد علمي.
- أُعيدت قراءة فهرس `HazRob/build-harness/library/reference-bank/external-research/master_catalog.json` من المصدر، وتأكد Git blob SHA1 `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f`؛ يضم 80 مدخلاً و14 مجموعة قياسات خارجية.
- مصفوفة المراجع المثبتة: 0/14 جاهزة للمراجعة، وسجل `G2_INDEPENDENT_EVIDENCE.json` يحوي `records: []`. هذه البيانات ليست قياسات فيزيائية مستقلة لروبوت Tank.

## التنفيذ
أُضيفت أداة `g2_acquisition_triage.py` لتحويل نتيجة تدقيق التقنيات الثمانين ومصفوفة جاهزية المصادر الأربعة عشر إلى تقرير JSON محدد البنية، مرتب وثابت وقابل لإعادة الإنتاج. تتضمن النتيجة معرّفات التقنيات والمصادر، حواجز كل منها، إحصاءً بالحواجز المتكررة، وخطوات اقتناء الدليل (البايتات الأصلية وتجزئاتها، مجموعات الأجهزة والجلسات والتعرضات، مرجع التركيز القابل للتتبع، وتوثيق بروتوكول القياس). لا تمنح الأداة أي ترقية علمية ولا تشتق ppm من أسماء الغازات.

التحقق يفشل عند تكرار المعرّفات، أو تناقض المجاميع، أو وجود حالة جاهزية متعارضة مع الحواجز، أو تكرار مفاتيح JSON. أُضيفت ثلاثة اختبارات آلية مع حالات سلبية. عُدّل سير عمل `.github/workflows/g2-signal-lineage-v4.yml` لإنتاج `g2_acquisition_triage.json` من ملف التدقيق الفعلي، وإرفاقه إلى جانب `g2_audit_results.json` ضمن artifact `tank-v4-g2-signal-r0-gate`. لا تُنسخ محتويات المكتبة المركزية الخاصة إلى المستودع العام.

## إعادة الإنتاج
```sh
python -m unittest discover -s research/implementation_execution -p 'test_g2_*.py' -v
python research/implementation_execution/g2_evidence_audit.py --output /tmp/g2_audit_results.json
python research/implementation_execution/g2_acquisition_triage.py \
  --audit /tmp/g2_audit_results.json \
  --source-matrix research/implementation_execution/G2_CENTRAL_CATALOG_SOURCE_READINESS_2026-10-28.json \
  --output /tmp/g2_acquisition_triage.json
```

## تحقق CI والحدود العلمية
- التزام سير العمل `7449c25144d8298d3cd9a335dfa960073bedca0e`، [تشغيل G2 رقم 40762104679](https://github.com/HazRob/HazRob-Tank/actions/runs/40762104679)، الوظيفة `123076456908`: **278/278** اختباراً ناجحاً، وتدقيق `g2_blocked=80` و`g2_pass_candidates=0`، ومصفوفة الاقتناء `source_blocked=14`. نجحت خطوة توليد JSON ورفع artifact `12847724274`.
- هذا تحسين تنظيمي قابل للتكرار لأعمال اقتناء الأدلة، لا قياس غاز جديد ولا تأهيل مستقل لأي تقنية. لا يوجد دليل مستقل يجيز اعتماد G2 أو G3.
- اقتصرت التغييرات على ملفات البحث وسير عمل G2 البحثي في الفرع `research/chemcore80-central-evidence-gap-analysis-20261021`؛ لم يُمس ChemCore Stable أو تطبيق Android أو الفريموير.
- يبقى الاستكمال العلمي مشروطاً ببيانات تعرض أصلية، مرجع ppm قابل للتتبع، هوية الجهاز والجلسة والتعرض، تقسيم مجموعات محجوب قبل الاطلاع على النتائج، ومراجعة علمية مستقلة.
