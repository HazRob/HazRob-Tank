# G2 — حجب نشر مخرجات CI الجزئية والتحقق النهائي من الأدلة (2026-10-29)

نقطة الاستئناف: البحث عند 6885743dfe90c536fe671a648fc860f27888d724. التشغيل السابق G2 #40793543195 ناجح (289/289). مكتبة المراجع المركزية HazRob/build-harness/library/reference-bank/external-research/master_catalog.json ذات Git blob SHA1 1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f؛ 80 مرجعاً بينها 14 مصدر بيانات خارجي. سجل الأدلة المستقلة records: [] وجاهزية المصادر 0/14.

## العيب
كان GitHub Actions يستخدم if: always() وif-no-files-found: ignore لرفع artifact حتى لو فشل تدقيق G2 أو فشل إنتاج إحدى النتائج. وقد ينشأ ملف جزئي يبدو صالحاً رغم فشل CI.

## المعالجة
أضيفت بوابة g2_artifact_publish_gate.py التي تعيد قراءة بايتات audit وtriage وreplay integrity قبل الرفع، وتتحقق من SHA256 والأحجام ومطابقة تجزئة التدقيق في triage واتساق نطاق 80 تقنية و14 مصدراً والحالات والعدادات. يقتصر رفع artifact على if: success() مع if-no-files-found: error. أضيفت سبعة اختبارات إيجابية وسلبية، تشمل التلاعب بالبايتات بعد إعادة التشغيل، وتغيير العدادات مع إعادة حساب التجزئات، وتغيير ارتباط المدخلات، وغياب ملف من manifest، وضبط سياسة الرفع.

## إعادة الإنتاج
python -m unittest discover -s research/implementation_execution -p 'test_g2_*.py' -v

python research/implementation_execution/g2_artifact_publish_gate.py --audit /tmp/g2_audit_results.json --triage /tmp/g2_acquisition_triage.json --manifest /tmp/g2_replay_integrity.json

## حدود الإثبات
التحقق البرمجي لا يثبت أصالة بيانات غازات فعلية أو معايرة ppm أو استقلال تقسيمات الاختبار؛ لا يمنح G2/G3. التعديلات في فرع البحث حصرياً، ولا تغيير على ChemCore Stable أو تطبيق Android أو الفريموير. تُلحق نتيجة CI بعد اكتمال التشغيل.
