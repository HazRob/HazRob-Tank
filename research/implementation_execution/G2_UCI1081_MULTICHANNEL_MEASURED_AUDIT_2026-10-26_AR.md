# G2 / 06.5 — تجربة UCI1081 متعددة القنوات، ومراجعة R0 (2026-10-26)

## مصدر تجريبي قابل لإعادة الإنتاج

- GitHub Actions run: `40254297596` — **success**، المصدر UCI1081 DOI `10.24432/C5CK6F`.
- Source archive SHA256: `813ebb7d00ed9172114b68d27ab3213170d6c220b8b23907dce91e9838cd48ab` (تحقق منه CI قبل التحليل).
- Results artifact `12621533193`، ZIP SHA256 `61f8509377062f18546f9a9826f984a66361b5db028b10964e1b844776777008`.
- Result file: `uci1081_g2_multichannel_open_set_results.json`، **36 fold/config rows** و6 summaries.
- Code: `research/benchmarks/uci1081_g2_multichannel_open_set.py`; eight synthetic negative tests passed.
- Training: 45 exposures؛ known-only calibration: 15؛ known final test: 15؛ unknown final test: 15، لكل طية من ست غازات. **لا تدخل عينة مجهولة في ضبط العتبة.**
- كل تكوين اختير مسبقاً (3 مجموعات قنوات × مقياسان للمسافة)، ولم تُضبط المعلمات على نتائج الغاز المجهول.

## النتائج الفعلية (أعداد صحيحة من Artifact)

| Source channels | Distance | Known accepted / 90 evaluations | Correct known & accepted / 90 evaluations | Unknown rejected / 90 unique exposures |
|---|---|---:|---:|---:|
| MQ135 | Euclidean | 90 (100%) | 43 (47.78%) | 4 (4.44%) |
| MQ135 | Manhattan | 90 (100%) | 43 (47.78%) | 4 (4.44%) |
| MQ135 + MQ137 + MQ138 | Euclidean | 85 (94.44%) | 46 (51.11%) | 9 (10.00%) |
| MQ135 + MQ137 + MQ138 | Manhattan | 85 (94.44%) | 42 (46.67%) | 13 (14.44%) |
| Source array 10 channels | Euclidean | 85 (94.44%) | 56 (62.22%) | 10 (11.11%) |
| Source array 10 channels | Manhattan | 90 (100%) | 55 (61.11%) | 8 (8.89%) |

**لا اختيار فائز بعد رؤية اختبار المجهول.** أفضل نسبة رفض مجهول مشاهدة هنا هي 13/90 فقط، ولا تحقق شرط قبول منهجي. إضافة قنوات قد تزيد رفض بعض الغازات وتحسن تصنيف المعروف، لكن النتائج غير مستقرة حسب الغاز/المقياس؛ ولا تثبت صلاحية Tank.

**تحذير التكرار:** المقام `90 known evaluations` ليس 90 تعرضاً مستقلاً: تعرضات replicate ordinal=4 المعروفة (18 تعرضاً فريداً) يعاد تقييمها عبر خمس طيات؛ أما حالات المجهول الـ90 فتمثل 90 تعرضاً فريداً موزعة على ستة غازات، لكن لا تتوفر session/device IDs للتحقق من الاستقلال. لا يجوز استخدام Wilson 90 independent known trials أو وصفها بدليل متعدد الأجهزة.

## تدقيق المعالجة: R0 لم تُطرح

- من `uci1081_actual_modules_v1.py`: `median(trace[780:900]) - median(trace[180:300])`، لكل قناة مصدر.
- من `uci1081_g2_multichannel_open_set.py`: استخدام **late baseline delta**؛ `r0_mode=NOT_USED`، `r0_subtracted=false`.
- مصدر وحدات الإشارة **غير مثبت** (لا يثبت أنها ADC أو فولت أو مقاومة)، فلا يجوز افتراض Rs، أو حساب Rs/R0، أو ppm من هذه الأرقام.
- Provenance: `research/implementation_execution/G2_UCI1081_SIGNAL_R0_PROVENANCE_2026-10-26.json`.

## نتيجة بوابة G2 والاختبارات

- Run `40254297639`: **34/34** tests PASS، مع audit لجميع IDs؛ `g2_pass_candidates=0`، `g2_blocked=80`.
- Audit artifact `12622051293`، ZIP SHA256 `33a8e1555ac9969227f5c96936f416e586d42a66e4a6d735e2aff20f4ef34528`.
- عُدلت بوابة `g2_evidence_audit.py` لتمنع اعتبار `replicate_ordinal` تجميعاً مستقلاً، أو الخلط بين baseline delta وR0، أو قبول SHA غير مثبت من البايتات.
- جميع workflows العشرة المرتبطة بـcommit `280507ef4db27ecc49cdd54f37cac3e8abe3f30f` كانت **success**، منها الـG1 batches وG2 source tests؛ **لا تعني نجاح G2 علمياً**.

## الحاجز العلمي والخطوات اللاحقة

لا يمكن ترقية تقنية 06.5 أو أي تقنية أخرى اعتماداً على هذه المجموعة وحدها: يجب وجود raw مستقل ومرجع غاز موثق، ومصدر وحدات/دارة الإشارة، وتوثيق مجموعة تعرضات على مستوى جلسة/دفعة/جهاز، وتقسيم train/calibration/test مع holdout محجوب، ومقارنة baseline، ومقاييس لكل غاز وفترات عدم يقين مع مراعاة الترابط. بالنسبة إلى Rs/R0 يجب توثيق R0 لكل حساس فعلي مع ظروف السخان ومرجع المعايرة وSHA للمصدر؛ **لا يُطرح R0 من ADC**. هذه شروط مراجعة، ولا تجعل التجربة أداة سلامة أو معايرة ppm.

**حالة المشروع:** G1 البرمجي ناجح، G2 الجديد المؤهل **0/80**، G3 Tank الفيزيائي **0/80**. لم يُعدل ChemCore Stable.
