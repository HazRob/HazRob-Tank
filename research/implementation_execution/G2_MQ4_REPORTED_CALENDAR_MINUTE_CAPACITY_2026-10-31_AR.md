# G2 — تدقيق السعة الزمنية اليومية المعلنة لبيانات MQ4 (31 أكتوبر 2026)

**النطاق:** فحص اتساق بيانات وصفية منشورة مسبقاً، وليس إعادة قراءة ZIP الخام ولا اعتماد G2/G3.

يربط الفحص ثلاثة سجلات: `G2_MQ_LIBRARY_RAW_INTAKE_2026-10-30.json`، و`G2_MQ4_LODO_TWO_WAY_PURGE_RAW_REPLAY_2026-10-30.json`، و`G2_MQ4_STRICT_CONTIGUOUS_SEGMENTS_2026-10-30.json`. يتحقق من هوية المصدر وعدم تكرارها، وتطابق بصمتي SHA256 المعلنتين، وعدد الصفوف، ومجموع أطوال المقاطع المتصلة، وتواريخ البداية والنهاية.

بناءً على **ادعاء المصدر** أن الطوابع الزمنية فريدة وأن القياسات مجمعة بالدقيقة، فإن عدد الصفوف الممكن في أي يوم تقويمي لا يزيد على 1440. اليوم الأول يبدأ عند 11:06 (حده الأعلى 774 دقيقة)، واليوم الأخير ينتهي عند 12:02 (حده الأعلى 723 دقيقة). تُرفض السجلات التي تتجاوز هذه الحدود، أو تفقد ادعاء التفرد، أو تغير بصمة المصدر أو عدد المقاطع. النتيجة المرجعية المعلنة: 14,186 صفاً خلال 11 يوماً، بحد أقصى يومي 1440، وبواقع 774 و723 في اليومين الطرفيين.

**حد الاستدلال:** تفرد الطوابع الزمنية، ودقة التجميع بالدقيقة، وصحة التوقيتين الطرفيين ليست حقائق أعيد إثباتها هنا من ZIP الخام؛ هي بيانات منشأ يجب التحقق منها مستقلاً قبل ترقية الدليل. التحقق من حسابات السعة لا يثبت استقلال التعرضات أو معايرة مرجعية أو دقة التنبؤ أو صلاحية أي تقنية من التقنيات الثمانين.

إعادة الإنتاج من جذر المستودع:

```bash
python -m unittest discover -s research/implementation_execution -p 'test_g2_mq4_calendar_minute_capacity.py' -v
python research/implementation_execution/g2_mq4_calendar_minute_capacity.py \
  --intake research/implementation_execution/G2_MQ_LIBRARY_RAW_INTAKE_2026-10-30.json \
  --lodo research/implementation_execution/G2_MQ4_LODO_TWO_WAY_PURGE_RAW_REPLAY_2026-10-30.json \
  --segments research/implementation_execution/G2_MQ4_STRICT_CONTIGUOUS_SEGMENTS_2026-10-30.json \
  --output /tmp/mq4-calendar-minute-capacity.json
```

ملف النتيجة المثبت: `G2_MQ4_REPORTED_CALENDAR_MINUTE_CAPACITY_2026-10-31.json`. اختبارات الانحدار العشرة تشمل تكرار هوية المصدر، غياب ادعاء التفرد، اختلاف بصمة العضو، عدم محاذاة الدقيقة، تجاوز السعات اليومية والطرفية، واختلال مجموع المقاطع. **لا يُعلن نجاح CI حتى يثبت في GitHub Actions.** يظل `G2_INDEPENDENT_EVIDENCE.json` دون سجلات مستقلة، ولا تُعدّل ChemCore Stable أو Android أو الفريموير.
