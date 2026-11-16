# G2 — رفض الأعداد غير المنتهية الناتجة من الأسّ في مدخلات النشر (29 أكتوبر 2026)

## نقطة الاستئناف والنطاق
- الفرع البحثي فقط: `research/chemcore80-central-evidence-gap-analysis-20261021`، الرأس قبل التعديل `a327c32bcb8371392e82eb43cf6703bd79807db0`.
- فهرس المراجع المركزي `HazRob/build-harness/library/reference-bank/external-research/master_catalog.json`: Git blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f`، 80 مدخلاً و14 مجموعة بيانات خارجية.
- مصفوفة المصادر المثبتة: blob `bd69304fc35175b90199c6cc6ad4a88fc10ae93f`، الجاهزية 0/14. سجل الأدلة المستقلة `G2_INDEPENDENT_EVIDENCE.json`: `records: []`.
- آخر نقطة استئناف موثقة قبل التعديل: 311/311 اختباراً ناجحاً في CI، من دون ترقية علمية.

## الخلل المعالج
قارئ JSON الصارم في `g2_artifact_publish_gate.py` كان يرفض الثوابت `NaN` و`Infinity` عبر `parse_constant`، لكنه لم يرفض الأسس العشرية المتجاوزة لمجال float مثل `1e999`، لأن محلل Python الافتراضي يحولها إلى `inf` دون استدعاء `parse_constant`. وقد يؤدي ذلك إلى قبول قيمة غير منتهية في كائن JSON ضمن حدود النشر، رغم منعها في قارئات بحثية أخرى. لا يشير هذا إلى تعرضات فيزيائية أو نتائج قياس فعلية.

## الإصلاح والاختبارات
- أضيف `parse_float=finite_float` مع `math.isfinite`، ويرفع `G2_PUBLISH_NONFINITE` عند تجاوز المجال.
- أضيف اختباران: رفض `1e999` و`-1e999` و`NaN` و`±Infinity`؛ والتحقق من استمرار قبول `1.25e-3` الصحيح.
- الالتزام البرمجي: `d8546fe733071c5c41401fe32ce71aa1c5489f27`. التزام الاختبارات: `60a92b7a2e93b305312e5d814578a161bf876373`.
- تشغيل G2: https://github.com/HazRob/HazRob-Tank/actions/runs/40823133117 ؛ وظيفة `123279939990`: **313/313 PASS**، ونجاح إعادة التشغيل المطابق بايتياً وبوابة نشر الأدلة، ورفع artifact `12875681362` (SHA256 للـZIP `ddc9ca0edbc95c058e136cb4386bf6670b03818d2ca2a25512b0a3e9be03ee97`).
- نتائج التدقيق: `g2_pass_candidates=0`، `g2_blocked=80`، `source_blocked=14`. نجاح G2 البرمجي لا يثبت صلاحية قياسات الغازات.

## إعادة الإنتاج
```bash
python -m unittest discover -s research/implementation_execution -p 'test_g2_*.py' -v
python research/implementation_execution/g2_evidence_audit.py --output /tmp/g2_audit_results.json
```
يمكن مراجعة سجل تشغيل GitHub Actions أعلاه للتحقق من بوابة النشر وإعادة التشغيل الكاملين.

## الحدود والقرار العلمي
لا تعديل لـChemCore Stable أو تطبيق Android أو الفريموير. لا اعتماد G2/G3، ولا استنتاجات دقة ppm أو تحديد غاز اعتماداً على الاختبارات الاصطناعية. يستلزم التقدم العلمي بيانات تعرض أصلية مستقلة، مراجع تركيز قابلة للتتبع، هويات أجهزة وجلسات وتعرضات، وتقسيمات اختبار محجوبة مثبتة مسبقاً.
