# G2 — تدقيق أهلية المصادر الخام الأربعة عشر في المكتبة المركزية (2026-10-28)

## الهدف والنطاق
تدقيق قابل لإعادة الإنتاج لمصادر **EXTERNAL_MEASUREMENT_DATASET** الموجودة في فهرس المراجع المركزي، دون الخلط بين (1) وجود بيانات أو مقالة علمية، (2) تأهيل مصدر القياس ومجموعاته الفيزيائية، (3) نجاح تقنية ChemCore على مجموعة اختبار محجوبة، (4) اعتماد Tank G3. التعديل في فرع البحث فقط؛ لا تغيير في ChemCore Stable أو Android أو Firmware.

## المرجع المثبت
المصدر: `HazRob/build-harness/library/reference-bank/external-research/master_catalog.json`، هوية Git blob: `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f`. تمت قراءة الملف الأصلي من المستودع والتحقق من 80 مدخلاً و14 مدخلاً من فئة القياسات الخارجية، وعدم تكرار معرّفات المدخلات. هوية Git blob تعرّف المحتوى، ولا تثبت بذاتها صحة التجربة الفيزيائية أو استقلال المرجع.

## نتيجة الجرد
**14/14 مصادر محجوبة عن الانتقال إلى مراجعة مصدر G2؛ 0/14 جاهزة وفق بيانات الفهرس الحالية.** جميع المصادر الأربعة عشر تحمل `experiment_independence=NOT_AUDITED`، ولا يحمل أي منها `measurement_qualification=true` من النوع المنطقي الصريح. القيم النصية مثل `claim_not_verified` و`reported_ppb_not_tank_calibration` **ليست** قيمة `true`، ويُمنع تحويلها إلى مؤهل بسبب truthiness البرمجية.

مصدران فقط يحملان حالة `RAW_INSPECTED_OR_PREVIOUSLY_VERIFIED` في الفهرس: `MQ-EXT-03` و`MQ-EXT-15`. هذه الحالة ليست بديلاً عن إثبات تجزئة SHA256 لبايتات المصدر الأصلي ومطابقة نسخة المرآة للمصدر الأصلي؛ `MQ-EXT-03` مرآة GitHub لمصدر Mendeley ولم تثبت مساواة البايتات، و`MQ-EXT-15` موجود مسبقاً في اختبارات UCI1081، ويُمنع احتسابه مصدراً تجريبياً مستقلاً إضافياً. المصدران `MQ-EXT-07` و`MQ-EXT-09` يحتاجان وصولاً أو طلباً من جهة المصدر. بقية المصادر تحتاج استكمال الوصول/فحص الأرشيف الأصلي. لا يجوز مساواة عدد صفوف البيانات بعدد الجلسات أو التعرضات الفيزيائية المستقلة.

## التنفيذ
- `g2_reference_catalog_readiness.py`: يتحقق من هوية Git blob لبايتات الفهرس، ومن العدد والمخطط وتفرد المعرّفات، ويُصدر مصفوفة أسباب حجب لكل مصدر. لا يصف أي مصدر بأنه جاهز إلا مع أدلة وصفية صريحة: `measurement_qualification is True`، `experiment_independence=PHYSICAL_GROUPS_VERIFIED`، `raw_file_status=RAW_BYTES_SHA256_VERIFIED`، تجزئة SHA256 أصلية موثقة، و`physical_reference_traceability is True`. وحتى عند تحققها لا تتجاوز النتيجة **SOURCE_CATALOG_ONLY_NOT_TECHNIQUE_APPROVAL**.
- `G2_CENTRAL_CATALOG_SOURCE_READINESS_2026-10-28.json`: المصفوفة الفعلية المستخرجة من المدخلات الأربعة عشر، مع هوية المصدر الأصلية وأسباب الحجب.
- خمس حالات اختبار اصطناعية/توثيقية جديدة في `test_g2_evidence_bundle_gate.py`: رفض النصوص التي تشبه القيم المنطقية، رفض الخلط بين فحص المرآة وتوثيق بايتات المصدر الأصلي، فصل جاهزية الفهرس عن اعتماد G2/G3، رفض المعرفات المكررة/تغير العدد/تجزئة غير مطابقة، والتحقق من ثبات مصفوفة 14 مصدراً المحفوظة.

## إعادة الإنتاج
من نسخة محلية تحتوي مستودعَي `HazRob/build-harness` و`HazRob/HazRob-Tank`، مع وجود Git blob المثبت:

```bash
git -C build-harness cat-file blob 1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f > /tmp/hazrob_master_catalog.json
python HazRob-Tank/research/implementation_execution/g2_reference_catalog_readiness.py \
  --catalog /tmp/hazrob_master_catalog.json \
  --output /tmp/g2_catalog_readiness.json
diff -u HazRob-Tank/research/implementation_execution/G2_CENTRAL_CATALOG_SOURCE_READINESS_2026-10-28.json /tmp/g2_catalog_readiness.json
cd HazRob-Tank/research/implementation_execution
python -m unittest -v test_g2_evidence_bundle_gate
```

لا تُستخدم ملفات أو بيانات اصطناعية لإثبات تركيز ppm أو هوية الغاز. ولا يُمنح G2 حتى مع نجاح هذه الاختبارات؛ فالحزمة المستقلة تحتاج ملفات القياس الأصلية الموثقة، مرجع تركيز مستقل، تعريف الجهاز والجلسة والتعرض، تقسيم مجموعات فيزيائية محجوبة، تثبيت بروتوكول وتنبؤات قبل كشف الحقيقة، وإعادة حساب المقاييس واللايقين ثم مراجعة مستقلة.

## وضع التنفيذ
أُضيفت الشيفرة والاختبارات ومصفوفة المصدر في فرع `research/chemcore80-central-evidence-gap-analysis-20261021` فقط. لا تُعلن نتيجة CI إلا بعد فحص تشغيل GitHub Actions على الالتزام النهائي. يبقى آخر وضع علمي موثق `g2_pass_candidates=0` و`g2_blocked=80`، وسجل الأدلة المستقلة `records: []`، إلى أن يثبت خلافه بتدقيق جديد. لا اعتماد G2/G3.

## تحقق CI المثبت
على الالتزام البحثي `b97e5224eca6b014302f7f1fa64eedfd08fc7f3b` نجحت جميع تشغيلات GitHub Actions **20/20** دون إخفاق مكتمل أو تشغيل معلّق عند الفحص. نجحت بوابة G2 في التشغيل `40594939850`، الوظيفة `122508351355`، بعد **209/209 اختبار Python ناجح**؛ وأعاد التدقيق الفعلي `g2_pass_candidates=0` و`g2_blocked=80`. مرجع التشغيل: https://github.com/HazRob/HazRob-Tank/actions/runs/40594939850 . هذه نتائج اختبارات برمجية وتدقيق مصدر، لا اعتماد علمي. أي التزام لاحق يستلزم إعادة التحقق من CI على SHA الجديد.
