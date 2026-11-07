# G2 — ضبط هوية مسارات ZIP لمصدر MQ-EXT-10 (3 نوفمبر 2026)

## نطاق المراجعة
- فرع البحث: `research/chemcore80-central-evidence-gap-analysis-20261021`.
- نقطة البداية: `9112ff1c9bbce3776b4736bc79cc9b7b00bdbd10`، وقد تحققت 21 عملية GitHub Actions ناجحة مرتبطة بهذا الالتزام، بما فيها بوابتا G2.
- الملفات المعدلة: `g2_mq_ext10_archive_intake.py`، `test_g2_mq_ext10_archive_intake.py`، هذه الوثيقة وسجل الاستئناف فقط.

## المشكلة
كان فاحص الأرشيف يقارن أسماء الأعضاء باستخدام `casefold()` دون تطبيع Unicode NFKC. يمكن لمسارين متكافئين ظاهرياً مثل `AQ_Wine...` و`ＡＱ_Wine...` أن يُعاملا كعضوين مختلفين. كذلك يمكن وضع قياسات داخل مجلد `Ｅｔｈａｎｏｌ` أو `ethanol` فلا تُحسب ضمن ملفات `Ethanol`، حتى لو اكتملت حصيلة 65 ملفاً في مجلد آخر. كما أن محارف التنسيق الخفية في أسماء الأعضاء كانت مقبولة.

## الإصلاح
1. تطبيق NFKC + casefold على **مفتاح اكتشاف تكرار المسار فقط**، دون تغيير الاسم الأصلي أو SHA256.
2. رفض أسماء مجلدات `Ethanol` المكافئة بعد NFKC/casefold لكنها غير مطابقة نصياً للاسم المعتمد، بما فيها إدخالات المجلدات نفسها، باستخدام `ARCHIVE_ETHANOL_FOLDER_ALIAS`.
3. رفض محارف Unicode ذات الفئة العامة `C*` في المسارات باستخدام `ARCHIVE_UNSAFE_PATH`، مع إبقاء الأسماء متعددة اللغات العادية مقبولة.
4. إضافة ستة اختبارات اصطناعية حتمية: ازدواج مسارات NFKC، مجلد كامل العرض، اختلاف حالة الأحرف، إدخال مجلد مستعار، محرف تنسيق خفي، وضابط إيجابي لاسم NFC متعدد اللغات.

## إعادة الإنتاج
```bash
python -m unittest discover -s research/implementation_execution -p 'test_g2_mq_ext10_archive_intake.py' -v
python -m unittest discover -s research/implementation_execution -p 'test_g2_*.py' -v
python research/implementation_execution/g2_evidence_audit.py --output /tmp/g2_audit_results.json
```

## حدود الدليل
هذه اختبارات ZIP اصطناعية لفحص سلامة المصدر فقط. لا يتوفر الأرشيف الأصلي المُصادق عليه ولا بصمة ناشر مستقلة، ولا تمثل النسب المئوية لسائل الإيثانول قياسات ppm في الهواء. لا تؤهل النتائج أي تقنية علمياً. سجل الأدلة المستقلة `records=[]`، والحالة السابقة `g2_pass_candidates=0` و`g2_blocked=80` و`source_blocked=14`. يجب التحقق من CI على الالتزام النهائي قبل إعلان نجاح التغيير. لم تُمس ChemCore Stable أو Android أو الفريموير أو main.
