# G2 — اتساق DOI المعلن مع رابط حلّ DOI (2026-10-28)

## حالة الاستئناف
- فرع البحث فقط: `research/chemcore80-central-evidence-gap-analysis-20261021`، نقطة البداية `91e1ee0ab3c7f440202b9489e00f6e8d6141f8b4`.
- تم التحقق من فهرس المكتبة المركزية `HazRob/build-harness/library/reference-bank/external-research/master_catalog.json` بهوية Git blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f`، ويتضمن 80 مدخلاً منها 14 مصدر قياسات خارجية.
- سجل الأدلة المستقلة `G2_INDEPENDENT_EVIDENCE.json` ما زال `records: []`، ومصفوفة جاهزية المصادر المثبتة 0/14.
- آخر CI قبل التعديل: 20/20 مسار ناجح على `91e1ee0ab3c7f440202b9489e00f6e8d6141f8b4`، و258 اختباراً ناجحاً في تشغيل G2 رقم 40726260703 على الالتزام السابق.

## التحسين البحثي
في `g2_reference_catalog_readiness.py` أضيف شرط تعارض منشأ المصدر: إذا احتوى الصف على DOI صريح وعلى `primary_url` يشير إلى خدمة DOI resolver، يجب أن تُطابق الهوية القانونية المعيارية لكليهما. مثال `doi=10.5555/first` مع `https://doi.org/10.5555/second` ينتج الحاجز `ORIGINAL_SOURCE_DOI_RESOLVER_CONFLICT`. يحجب الصف المتعارض وحده ولا يعطل بقية المصادر. تُقبل صيغ `doi:` و`dx.doi.org` والترميز المئوي للشرطة المائلة في DOI عند تكافؤها. لا تُقارن روابط الناشر العادية بهذا الشرط لأن تطابق المسار النصي ليس شرطاً لهوية DOI.

## اختبارات قابلة للإعادة
أضيفت ثلاث حالات في `test_g2_origin_percent_authority.py`، وتستخدم ملفي بايتات اصطناعيين منفصلين مع SHA256 محسوب من محتواهما. تشمل تعارض DOI، وتطابق DOI مع بدائل resolver، وحالة رابط ناشر عادي. للتشغيل من جذر المستودع:

```sh
python -m compileall -q research/implementation_execution
python -m unittest discover -s research/implementation_execution -p 'test_g2_*.py' -v
python research/implementation_execution/g2_evidence_audit.py --output /tmp/g2_audit_results.json
```

## حدود الاستدلال
هذه حماية لاتساق فهرس المصدر وليست دليلاً على استقلال القياسات أو تركيز ppm أو معايرة حساسات Tank. لا يُعد مصدر مؤهلاً علمياً بسبب اجتياز فحص DOI. لا تغيير في ChemCore Stable أو تطبيق Android أو الفريموير. لم تُمنح أي ترقية G2/G3. نتيجة CI على التزام الشيفرة والاختبارات والتوثيق `e6172f851baf980d9afc96891fccd6027731efb0`: نجح 261/261 اختباراً في تشغيل G2 رقم [40735621847](https://github.com/HazRob/HazRob-Tank/actions/runs/40735621847)، الوظيفة `122983228312`، وظهر `g2_pass_candidates=0` و`g2_blocked=80`. اكتملت 21/21 تشغيلات CI على الالتزام نفسه بنجاح دون إخفاقات. يجب إعادة التحقق من CI على الالتزام التوثيقي النهائي، ولا يعني نجاح CI أي اعتماد علمي.
