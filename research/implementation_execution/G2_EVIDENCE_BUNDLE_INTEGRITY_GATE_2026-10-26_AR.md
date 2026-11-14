# G2 — إغلاق ثغرة ترقية سجلات الأدلة دون ملفات قابلة للتحقق (2026-10-26)

## نطاق التعديل
- المستودع: `HazRob/HazRob-Tank`؛ الفرع `research/chemcore80-central-evidence-gap-analysis-20261021`.
- التعديل محصور في `research/implementation_execution/` ومسار CI البحثي `.github/workflows/g2-signal-lineage-v4.yml`.
- لم تُعدّل شيفرة ChemCore Stable أو التطبيق أو الفريموير أو بيانات المعايرة التشغيلية.

## المشكلة المؤكدة في البوابة السابقة
كان `g2_evidence_audit.py` يقبل ترشيح `G2_PASS_CANDIDATE_REQUIRES_REVIEW` اعتماداً على حقول وصفية في سجل JSON، من دون التحقق من وجود ملفات الأدلة الأربعة عشر التي تشترطها وثيقة `G2_SOURCE_PROMOTION_GATE_2026-10-26_AR.md`، أو مطابقة SHA256 لبايتاتها. وكان مسار CI لا يُعاد تشغيله عند تعديل سجل الأدلة أو إضافة ملفات `research/g2/evidence/**`، ما يضعف كشف تغييرات الأدلة.

## ما نُفذ
1. إضافة `g2_evidence_bundle_gate.py`: يلزم `dataset_id` و`run_id` صالحين، وقائمة الملفات الأربعة عشر الكاملة، وتجزئة SHA256 صحيحة لكل ملف، ووجوده فعلياً في مسار المصدر/التقنية/التشغيل داخل المستودع. تُرفض الملفات المفقودة، والتجزئات غير الصحيحة، والمسارات غير الآمنة والروابط الرمزية.
2. إضافة فحص بنية الملفات: JSON كائن غير فارغ، CSV له رأس وصف بيانات متسقان، و`reproduce.sh` يبدأ بسطر تشغيل. **هذا فحص شكل وصحة بايتات وليس تحققاً من صحة النتائج العلمية أو محتوى القياسات المرجعية.**
3. ربط بوابة الملفات بـ`audit_record`، بحيث لا يمكن لحقول metadata وحدها إنشاء مرشح مراجعة.
4. إضافة `test_g2_evidence_bundle_gate.py`: ضوابط اصطناعية لحزمة كاملة، وتلاعب بالبايتات، وملف مفقود، وملف ناقص/زائد في القائمة، واجتياز مسار `../`، ورابط رمزي، وتجزئة غير صحيحة، وJSON غير صالح مع SHA صحيح.
5. تحديث اختبارات البوابة السابقة بحيث تعتبر السجل الوصفي وحده `G2_BLOCKED`.
6. تحديث GitHub Actions ليشغّل اختبارات البوابة الجديدة، ويُعاد تشغيله عند تغيّر `G2_INDEPENDENT_EVIDENCE.json` أو `EXECUTION_LEDGER_2026-10-21.json` أو ملفات الأدلة.

## حالة البيانات العلمية
- لا تزال `G2_INDEPENDENT_EVIDENCE.json` تحوي `records: []`.
- لا يوجد بهذه الجولة اختبار غاز جديد أو معايرة Tank فعلية. لا يجوز تحويل اختبار صحة ملفات أو اختبارات fixtures إلى دليل تجريبي.
- G2 الجديد: **0/80 مرشحين، 80/80 محجوبين** إلى أن تتوفر قياسات خام مستقلة ذات provenance أصلي، ووحدات وإشارة ومرجع وتركيز وholdout مثبت.
- G3 الفيزيائي لـTank: **0/80**.
- نجاح CI عند اكتماله يثبت صحة بوابة البرمجيات فقط؛ لا يثبت صلاحية تقنية أو ppm.

## إعادة الإنتاج
من جذر المستودع:
```bash
python -m compileall -q research/implementation_execution/g2_evidence_bundle_gate.py research/implementation_execution/g2_evidence_audit.py
(cd research/implementation_execution && python -m unittest -v test_g2_signal_lineage_gate test_g2_evidence_audit test_g2_evidence_bundle_gate test_g2_uci1081_signal_manifest)
python research/implementation_execution/g2_evidence_audit.py --output /tmp/g2_audit_results.json
```

## قيود باقية
حتى الحزمة التي تجتاز فحص الملفات والتجزئات لا تثبت استقلال جلسات التعرض أو صحة المرجع الفيزيائي؛ لذا تظل `G2_PASS_CANDIDATE_REQUIRES_REVIEW` مرشح مراجعة فقط، ولا تتحول تلقائياً إلى اعتماد. كما أن البيانات الحالية تفتقر إلى عناصر المصدر المطلوبة للتأهيل؛ لا يمكن توليدها من CI.
