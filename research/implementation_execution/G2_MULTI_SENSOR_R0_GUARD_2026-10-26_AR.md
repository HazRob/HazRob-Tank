# G2 — تدقيق R0 متعدد الحساسات وقفل مصدر المعايرة (26 أكتوبر 2026)

## النطاق
HazRob Tank، الفرع البحثي `research/chemcore80-central-evidence-gap-analysis-20261021` فقط. هذه مراجعة **سلامة بيانات ومعايرة وصفية**، وليست قياساً جديداً أو اعتماداً لـG2/G3. لم تُعدل ChemCore Stable.

## الخلل المكتشف
في `g2_signal_lineage_gate.py` كان مسار `RS_OVER_R0` يقبل مصفوفة `sensor_ids` متعددة بينما يتحقق من مرجع `r0_reference` واحد فقط. قد يؤدي ذلك إلى قبول سجل وصف بيانات يدعي تطبيع عدة حساسات بمقاومة مرجعية لحساس واحد. كذلك كان اسم الطراز `MQ135` مقبولاً كهوية عينية فعلية للحساس، مع أنه ليس رقماً تعريفياً للوحدة المقاسة.

## المعالجة
- حظر استعمال اسم طراز MQ وحده بديلاً عن `sensor_instance_id` عند تطبيع `RS_OVER_R0`.
- عند وجود أكثر من حساس، إلزام `r0_references` وهي خريطة تربط **كل معرف عيني** بمرجعه؛ حظر المراجع الناقصة والمتقاطعة أو تعريف `r0_reference` و`r0_references` معاً.
- الاحتفاظ بتوافق الصيغة المفردة `r0_reference` مع قناة واحدة.
- اختبارات تحكم سلبية وإيجابية جديدة في `test_g2_signal_lineage_gate.py`. نجاح حالة البيانات الوصفية يعني فقط صحة شكل السجل، ولا يثبت صحة قياس R0 أو مرجع الغاز.

## إعادة الإنتاج
```bash
cd research/implementation_execution
python -m unittest -v test_g2_signal_lineage_gate test_g2_evidence_audit test_g2_uci1081_signal_manifest
cd ../..
python research/implementation_execution/g2_evidence_audit.py --output /tmp/g2_audit_results.json
```
تستدعي `.github/workflows/g2-signal-lineage-v4.yml` الاختبارات نفسها وتتحقق من بقاء 0 مرشحاً و80 تقنية محجوبة عندما لا توجد أدلة تجريبية مستقلة.

## حدود النتيجة
لا يحتوي `G2_INDEPENDENT_EVIDENCE.json` حالياً على سجل قياس مؤهل. UCI1081 لا توفر وحدات المصدر ومعرفات جلسات/أجهزة مستقلة معتمدة لهذه البوابة. لا يمكن لهذه المعالجة البرمجية توليد بيانات مفقودة أو تحويل اختبارات CI إلى دليل معايرة فيزيائية. تبقى جميع التقنيات في G2 بلا ترقية إلى أن تستوفى متطلبات `G2_SOURCE_PROMOTION_GATE_2026-10-26_AR.md`.
