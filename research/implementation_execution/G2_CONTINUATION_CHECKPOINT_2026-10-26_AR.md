# استكمال G2 — نقطة تنفيذ موثقة 2026-10-26

## المرجعية الملزمة
- الفرع: `research/chemcore80-central-evidence-gap-analysis-20261021`؛ النطاق `research/**` فقط.
- خطة `PLAN_80_V4.json`؛ سجل `EXECUTION_LEDGER_2026-10-21.json`؛ بنك المراجع المثبت Git blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f`.
- ثماني دفعات × 10 تقنيات؛ لا تغيير في المواصفات أو التطبيقات التشغيلية.
- G1 V4 المعتمد 60/80؛ G2 التاريخي مراجعة أهلية 80/80، وليس اجتياز benchmark تجريبي؛ G3 الفيزيائي 0/80.

## التنفيذ الحالي: تدقيق مصدر الدفعة السابعة قبل الاستدلال
- جرى فتح `BATCH07_SOURCE_READINESS_2026-10-23.json` من GitHub (blob `147c110be7333fbad1354a729bdb64fe39d1a908`).
- 07.10: قياس زمن/ذاكرة CPython بديل بحثي، **لا** قياس كمون/قدرة على ESP32/Android الحقيقي؛ `G2_NEEDS_HARDWARE_BENCHMARK`.
- 08.1: مرشح Conv1d أمامي، **ليس** شبكة CNN مدربة ومقيمة على مجموعات اختبار مستقلة؛ `G2_NEEDS_GROUPED_TRAINED_BENCHMARK`.
- 08.2: مرشح سببي متسع (TCN primitive)، **ليس** نموذج TCN مدرباً مثبت الأداء؛ `G2_NEEDS_GROUPED_TRAINED_BENCHMARK`.
- يجب مراجعة بقية المعرفات 08.3–08.9 فردياً وفق source readiness، وتسجيل المصدر ونوع الحساس ووحدات القياس وtrain/test grouping والقيود العلمية، دون تعميم نتيجة UCI على Tank.
- لم تُنفذ في هذه النقطة تجارب بيانات خام جديدة أو اختبارات G2؛ هذا **تدقيق توثيقي فعلي** للمصادر والقيود، وليس إغلاق G2.
- أي تقنية تفتقد raw independent source أو معايرة أو heldout grouping تبقى `NEEDS_DATA` / `BLOCKED`؛ لا تُرفع إلى PASS.
- استمرار G1: سجل `G1_SOURCE_TRANSFER_AUDIT_2026-10-26_AR.md`، نقل ملفات أصلية مع Git blob SHA، اختبارات نظيفة؛ لا دمج للإنتاج.
