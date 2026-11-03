# G2 — مراجعة تعارض حالة المصدر MQ-EXT-01 (2026-11-03)

النطاق: مقارنة حتمية **لمطالبات الفهرسة فقط** بين مصفوفتَي المصدر المثبتتين في فرع البحث، دون تعديل الفهرس المركزي الخاص أو ChemCore Stable أو منح اعتماد G2/G3.

**النتيجة:** تطابقت هوية الفهرس الأصلي Git blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` وتطابقت هويات المصادر الأربعة عشر. رُصد تعارض واحد: مصدر MQ-EXT-01 في مصفوفة الجاهزية القديمة يحمل `archive_listed_not_downloaded` و`SOURCE_DESCRIPTION_VERIFIED_RAW_PENDING`، بينما سجل المرشحين يذكر أرشيف Zenodo الأصلي بوصفه متحققاً عبر MD5 وSHA256، بالتجزئة `fde5a3836fe9ddb04fee90102b9c1a5816f48a78d37bf7125074e66a3f3b5f5a` وتشغيل CI رقم `40287072270`.

هذه **مطالبة موثقة بسجل سابق وليست إعادة فحص مستقلة للبايتات في هذه الجولة**. لا يجوز رفع `catalog_source_review_ready` أو تجاوز شرط إعادة تشغيل البايتات الأصلية، ولا تتضمن البيانات المتاحة مجموعات تعرض مستقلة أو مرجع تركيز أو معايرة Tank. أبقينا مصفوفة الجاهزية المثبتة دون تغيير حتى لا نكسر ربطها بالفهرس الأصلي.

التنفيذ: `g2_catalog_snapshot_drift.py` ينتج تقرير JSON حتمياً، ويرفض اختلاف هوية الفهرس أو مجموعة المعرفات، والبيانات المكررة، والتجزئات غير الصالحة، ومحاولات اعتبار المطالبة اعتماداً. أضيفت 7 اختبارات انحدار بينها اختبار على ملفات المستودع الحقيقية. التقرير: `G2_SOURCE_SNAPSHOT_DRIFT_2026-11-03.json`.

إعادة الإنتاج من جذر المستودع:

```bash
python -m unittest discover -s research/implementation_execution -p 'test_g2_catalog_snapshot_drift.py' -v
python research/implementation_execution/g2_catalog_snapshot_drift.py --output /tmp/g2_source_snapshot_drift.json
python research/implementation_execution/g2_evidence_audit.py --output /tmp/g2_evidence_audit.json
```

تظل حالة 80 تقنية محجوبة وفق آخر تدقيق علمي مثبت، والسجل المستقل `records=[]`. نجاح اختبارات CI البرمجية لا يعادل تأهيلاً علمياً. يلزم أولاً إعادة تحقق مصدر MQ-EXT-01 وتوثيق مجموعات التعرض المرجعية ثم اختبار محجوب مستقل.
