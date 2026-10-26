# G2 — إلزام تطابق مفتاح التجميع مع أعمدة المصدر الفيزيائية (2026-10-26)

## نطاق العمل
- المستودع: `HazRob/HazRob-Tank`، فرع البحث `research/chemcore80-central-evidence-gap-analysis-20261021` فقط.
- متابعة بعد تدقيق `G2_BOOTSTRAP_INPUT_INTEGRITY_2026-10-26_AR.md` الذي ثبت فيه نجاح 106 اختبارات وحالة `0/80` مرشحين.
- التغيير محصور في `research/implementation_execution/g2_cross_artifact_gate.py` و`test_g2_evidence_bundle_gate.py`، إضافة إلى هذا التوثيق؛ لم يتغير ChemCore Stable أو قياسات الغاز الخام.

## الثغرة المكتشفة
كانت بوابة التجميع تتحقق من أن `grouping_source_field` يشير إلى أعمدة موجودة وغير فارغة، وأن معرف المجموعة يطابق صفوفها. لكنها **لم تتحقق من أن الأعمدة ذاتها تمثل الهوية الفيزيائية التي يزعمها `grouping_key`**. بالتالي، عندما تكون لكل مجموعة عينة واحدة، يمكن لمصدر `sample_id` فريد لكل صف أن يمر كأنه `device_session_id` مستقل؛ لا تكشف فحوص SHA256 أو فصل أسماء المجموعات وحدها هذا الخطأ.

## الإصلاح وشرط الرفض
أُضيف فحص `GROUPING_SOURCE_COLUMNS_NOT_PHYSICAL_FOR_KEY` عند عدم احتواء أعمدة المصدر على الحد الأدنى المطابق لنوع التجميع:
- `session_id` ← `session_id`.
- `device_session_id` ← `device_id` و`session_id`.
- `batch_id` ← `batch_id`.
- `campaign_id` ← `campaign_id`.
- `site_day_device` ← `site_id` و`day_id` و`device_id`.

يسمح بأعمدة إضافية، ولا يعد تسمية الأعمدة إثباتاً مستقلاً لصدق قيمها؛ يجب الرجوع إلى سجلات التجربة الأصلية. أي مصدر يستخدم أسماء أخرى يجب أن يوثق خريطة التحويل إلى أسماء أعمدة معيارية دون تصنيع معرفات.

## الاختبارات المضافة
1. **تحكم سلبي:** `grouping_key=device_session_id` مع `grouping_source_field=sample_id` وأربع عينات منفردة فريدة؛ يجب الحجب رغم إعادة حساب تجزئة CSV.
2. **تحكم سلبي:** `device_session_id` مع عمود `session_id` وحده؛ يجب الحجب.
3. **تحكم إيجابي برمجي:** `session_id` مع عمود `session_id` فعلي في الحزمة الاصطناعية؛ يبقى قابلاً للوصول إلى حالة مرشح للمراجعة فقط.
4. الاختبارات القديمة الخاصة بالتجميع والـbootstrap والبيانات المفقودة يجب أن تستمر بالنجاح.

أوامر إعادة الإنتاج:
```bash
cd research/implementation_execution
python -m unittest -v test_g2_signal_lineage_gate test_g2_evidence_audit test_g2_evidence_bundle_gate test_g2_cluster_bootstrap_gate test_g2_uci1081_signal_manifest
cd ../..
python research/implementation_execution/g2_evidence_audit.py --output /tmp/g2_audit_results.json
```

## حالة التحقق العلمي
لا تزال `G2_INDEPENDENT_EVIDENCE.json` تحتوي `records=[]`؛ **0/80 مرشحين و80/80 محجوبين**، إلى أن تتوفر ملفات قياس خام أصلية ومعايرة مرجعية مستقلة ومجموعات فيزيائية موثقة واختبار محجوب. نجاح اختبارات اصطناعية لا يثبت صلاحية ppm أو هوية الغاز ولا يرقّي G2/G3. يُمنع تعديل Stable أو منح اعتماد دون أدلة مستقلة.

## تتبع CI
- أساس المراجعة: تشغيل ناجح `40359691549` للالتزام `d73ea8a4`: https://github.com/HazRob/HazRob-Tank/actions/runs/40359691549
- التزامات التغيير: `46973801` (القاعدة)، `7488c44f` (الاختبارات)، `d365e48d` (تصحيح محارف CSV الاصطناعي).
- تشغيل التحقق على `d365e48d`: https://github.com/HazRob/HazRob-Tank/actions/runs/40368234561 (تحقق من النتيجة النهائية في Actions؛ كان في قائمة الانتظار وقت بدء كتابة التوثيق).

## إجراء لاحق
تحقق مستقل من أصالة معرفات الجلسة والجهاز في المصدر الخام، ومن قابلية إعادة إنتاج تقسيم train/calibration/test من المصدر قبل ترشيح أي تقنية. هذه البوابة تحقق اتساق التسمية، ولا تستطيع وحدها إثبات الاستقلال الفيزيائي.
