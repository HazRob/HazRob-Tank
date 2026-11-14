# G2 — التحقق من طول البايتات الفعلي أثناء إعادة تشغيل أرشيف القياس ومعايرة R0

التاريخ: 2026-10-27. المشروع: HazRob Tank. النطاق: فرع البحث `research/chemcore80-central-evidence-gap-analysis-20261021` فقط. هذا فحص سلامة برمجية ولا يشكل اعتماداً علمياً أو معايرة ppm.

## نقطة الاستئناف والأدلة

- نقطة الاستئناف الموثقة: `fc6d8482c6678090b8d38912060b15cee28f3274`، بوابة إعادة تشغيل المصدر الخام لمعايرة R0.
- المرجع المركزي: `HazRob/build-harness/library/reference-bank/external-research/master_catalog.json`، Git blob SHA `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f`، أُعيد التحقق منه من GitHub في هذه الجولة؛ عدد سجلات الفهرس 80، ولا يحوّل وجود مرجع خارجي إلى قياس مستقل للدبابة.
- سجل الأدلة `research/implementation_execution/G2_INDEPENDENT_EVIDENCE.json` لا يزال `records: []`.
- CI السابق على الالتزام `fc6d8482`: تشغيلات GitHub Actions الأحد عشر `conclusion=success`.

## الفجوة المكتشفة

في `g2_raw_source_gate.py` و`g2_r0_source_gate.py` كان حجم الملف يُفحص باستخدام `stat()` **قبل** فتح الملف وقراءة البايتات. إذا تغير المصدر بين `stat()` و`open()` وأُعيد إعلان SHA256 للبايتات الأقصر، أمكن أن تطابق التجزئة البيانات المقروءة بينما يظل `raw_size_bytes` يصف طولاً آخر. هذا عدم اتساق قابل لإعادة الإنتاج، ولا ينبغي أن يمر إلى قائمة المراجعة.

## التصحيح والاختبارات

- عُدّل المساران لحساب `bytes_hashed` أثناء التحديث المتسلسل لـ SHA256، ومقارنته بحجم المصدر المعلن **بعد اكتمال القراءة**. يبقى فحص `stat()` الأولي للتنبيه المبكر، لكن لا يُعتمد عليه وحده.
- رمزا الرفض: `RAW_SOURCE_BYTE_SIZE_MISMATCH` و`R0_RAW_SOURCE_BYTE_SIZE_MISMATCH`، دون تغيير واجهة النتائج.
- أُضيفت أربعة اختبارات انحدار اصطناعية: تغير طول تيار R0 بعد `stat()` مع تجزئة مطابقة؛ تغير طول تيار القياس بعد `stat()` مع تجزئة مطابقة؛ رفض رابط رمزي لملف R0؛ وإعادة تشغيل مرجعي R0 لحساسين ثم كشف تغير بايتات الثاني.
- التغييرات البرمجية: `g2_raw_source_gate.py`، `g2_r0_source_gate.py`، `test_g2_evidence_bundle_gate.py` فقط، جميعها تحت `research/implementation_execution/`. الالتزام البرمجي: `cb7d05bfe0350a2f256933a636760610dbc0b172`.

## إعادة الإنتاج

من جذر المستودع:

```bash
cd research/implementation_execution
python -m unittest -v test_g2_signal_lineage_gate test_g2_evidence_audit test_g2_evidence_bundle_gate test_g2_cluster_bootstrap_gate test_g2_uci1081_signal_manifest
cd ../..
python research/implementation_execution/g2_evidence_audit.py --output /tmp/g2_audit_results.json
```

## نتائج CI المتحققة

- GitHub Actions — `Tank V4 G2 — source signal and R0 evidence gate`: التشغيل `40520723045`، الوظيفة `122246756442`، `conclusion=success`.
- سجل الوظيفة: **183/183 اختباراً ناجحاً**، بما فيها الاختبارات الأربعة الجديدة، دون فشل.
- تدقيق التقنيات الثمانين: `g2_pass_candidates=0` و`g2_blocked=80`.
- مقارنة الالتزامات `fc6d8482...cb7d05bf`: ثلاثة ملفات بحثية فقط، بلا تغيير ChemCore Stable.
- نتائج بقية المسارات على الالتزام البرمجي تُراجع منفصلة؛ التشغيل قيد الانتظار ليس نجاحاً ولا فشلاً.

## حدود الإثبات والعمل التالي

التحقق من البايتات والطول والتجزئة لا يثبت صحة الغاز المرجعي أو التركيز أو هوية الجهاز/الجلسة أو استقلال الاختبار المحجوب. لا تتوفر حتى الآن حزم قياسات Tank فيزيائية مؤهلة. لذلك لا تُمنح أي تقنية اعتماد G2 أو G3؛ تظل جميع التقنيات الثمانين محجوبة حتى تأهيل الأدلة الأصلية والتتبع المترولوجي.
