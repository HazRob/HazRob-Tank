# G2 — تدقيق سلامة مدخلات Bootstrap العنقودي (2026-10-26)

## النطاق
- المستودع: `HazRob/HazRob-Tank`؛ فرع البحث `research/chemcore80-central-evidence-gap-analysis-20261021`.
- مراجعة نقطة الاستكمال السابقة `ca24ea9f85e50b9942bff43b1996cf103ef6273a`، وتدقيق G2 ذي 100 اختبار ناجح و`0/80` مرشحين.
- تعديل بحثي فقط؛ لا تغيير في `ChemCore Stable` أو ملفات Android/ESP32 أو معايرة Tank.

## الثغرة المحاسبية التي أُغلقت
كانت `audit_group_bootstrap_accuracy` تستخدم `group_correct.get(group_id, 0)`؛ فإذا سقط بسط مجموعة من بيانات الاختبار أمكن افتراض صفر إجابات صحيحة بصمت، مع إعادة حساب مجال ثقة متسق مع مدخل ناقص. كما أن المعرفات المختلطة الأنواع أو المدخلات غير القاموسية قد تتسبب في استثناء بدل نتيجة رفض قابلة للتدقيق.

أصبحت البوابة الآن:
1. تتطلب تطابقاً تاماً بين مجموعتي مفاتيح `group_counts` و`group_correct`؛ لا بسط مفقود ولا زائد.
2. تتحقق من نوع المدخلات قبل الفرز، وتقبل معرفات مجموعات نصية غير فارغة فقط.
3. ترفض أعداد العينات غير الصحيحة، والقيم المنطقية، والأعداد السالبة، والبسط الأكبر من المقام.
4. تُرجع `GROUP_BOOTSTRAP_GROUP_COUNTS_INVALID` في الحالات غير الصالحة بدلاً من الانهيار أو ملء قيمة مفقودة بصفر.

## الاختبارات والنتائج
- ملف اختبارات جديد: `research/implementation_execution/test_g2_cluster_bootstrap_gate.py`، **ست حالات اختبار رئيسية** تشمل مدخلين سليمين وغير سليمين، وsubtests لمعرفات وأعداد مختلفة.
- ضُمت الاختبارات صراحة إلى `.github/workflows/g2-signal-lineage-v4.yml`.
- تحقق GitHub Actions ناجح للالتزام `21bf296f5faca39b46deb7697360a10a187ecdf0`:
  https://github.com/HazRob/HazRob-Tank/actions/runs/40359461406
- سجلات التشغيل: **106 اختبارات ناجحة**، وخطوة تدقيق G2 `g2_pass_candidates=0` و`g2_blocked=80`، مع رفع ملف `g2_audit_results.json` ضمن الأثر `tank-v4-g2-signal-r0-gate`.
- التزامات التنفيذ: `ff585d72732d8748f0c6f7c30fb3e7deee5fd2c1` (الإصلاح)، `0437d825377749f0346bc597e934b7e8747b7148` (الاختبارات)، `21bf296f5faca39b46deb7697360a10a187ecdf0` (CI).

## إعادة الإنتاج
```bash
cd research/implementation_execution
python -m unittest -v test_g2_signal_lineage_gate test_g2_evidence_audit test_g2_evidence_bundle_gate test_g2_cluster_bootstrap_gate test_g2_uci1081_signal_manifest
cd ../..
python research/implementation_execution/g2_evidence_audit.py --output /tmp/g2_audit_results.json
```

## حدود الدليل والإجراءات القادمة
- الاختبارات اصطناعية للتحقق من صحة الشيفرة، **وليست** تجارب غاز أو تحققاً إحصائياً من استقلال مجموعات التعرض.
- لا تتغير نتائج المصادر الخارجية السابقة، ولا تصبح قيمة `0/80` اعتماداً سلبياً للمنهج؛ إنها حالة غياب حزم إثبات مستقلة صالحة.
- يلزم استكمال مصادر خام موثقة وحدات الإشارة والهوية الفيزيائية للجلسة والجهاز وتركيزاً مرجعياً مستقلاً، ثم إجراء فصل مجموعات وتجربة محجوبة مسبقة البروتوكول قبل ترشيح أي تقنية G2.
