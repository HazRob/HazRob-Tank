# G1 V4 إغلاق برمجي وبدء G2 — 2026-10-26

## نتيجة G1
- اعتماد **80/80** فقط في نطاق G1 V4 (source-software CI): الدفعات 01–06 لها تشغيلات CI ناجحة، والدفعتان 07–08 اختبرتا في [run 40247142083](https://github.com/HazRob/HazRob-Tank/actions/runs/40247142083) بنتيجة **22/22 jobs success**، بما فيها جميع تقنيات الدفعتين والبوابات المشتركة.
- إصلاحا 09.5 في الاختبار والمصدر عند الالتزامين 7d4a524e و23ba43f5.
- G1 لا يثبت الدقة الكيميائية أو ppm أو الأداء على عينات غاز حقيقية. G3 Tank = **0/80**.

## G2 V4 — شروط قبول تجريبية لكل تقنية
1. مصدر البيانات: dataset خام أو مقياس مرجعي، ترخيص، رابط وإصدار، SHA256، تعيين نوع الحساس ووحدة القياس، R0/Rs والتوقيت وظروف البيئة؛ دون أي اختلاق للحقول المفقودة.
2. تقسيمات مانعة للتسرب: group holdout حسب الجلسة والجهاز والغاز والتركيز والتاريخ حسب ما يناسب التقنية؛ عزل بيانات الضبط والاختبار، وتكرارات مستقلة؛ أي انعدام للـmetadata يسجل BLOCKED.
3. مقاييس مناسبة: classification macro-F1/confusion/OOD AUROC وunknown rejection؛ regression MAE/RMSE وCI وcoverage؛ mixture identifiability، domain shift، drift، latency/pose alignment حسب التقنية.
4. مقارنات baseline، confidence intervals، negative controls، ablations، وقياس تدهور الأداء على بيانات خارج التدريب؛ تثبيت البذور والإصدارات والـartifacts.
5. حظر الترقية: لا تُمنح G2_PASS للـsynthetic-only أو الـin-sample validation، ولا تدعى ppm أو gas identity أو صلاحية قيادة آلية دون دليل مستقل. تمييز source eligibility القديم 80/80 عن G2 التجريبي الجديد.

## الأولويات الأولى لـG2
- 06.5 UCI1081 MQ135: إعادة benchmark على البيانات الأصلية وتوثيق فشل/نجاح unknown rejection، مع حفظ النتائج السلبية وعدم إخفائها.
- 04.5 و04.7: تحقيق parity لخوارزميات RBF وXGBoost أو وضع BLOCKED_ALGORITHM_PARITY.
- 08.12: grouped holdout فعلي بدلاً من مجرد عدّ المجموعات.
- 09.1 و09.3 و09.5: إثبات causal alignment والتأخر الزمني بقياسات مستقلة؛ عدم استنتاج source localization من threshold crossings.
- 08.11 و09.4 و09.6: انتقائية حقيقية وحظر أي استنتاج حركي من خرائط hotspot وصفية.

## القرار
G1 V4 software CI = 80/80؛ **G2 V4 empirical qualification remains unverified**؛ G3 Tank physical = 0/80. يجب بناء مصفوفة evidence per technique ونتائج تشغيل المصدر الحقيقي قبل تحديث G2.
