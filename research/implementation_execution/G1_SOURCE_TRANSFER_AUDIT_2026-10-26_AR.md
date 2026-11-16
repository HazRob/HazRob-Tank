# سجل نقل شيفرة G1 والتحقق من التطابق — 2026-10-26

## الإجراء 1: اختبار قناة النقل
- المستودع: `HazRob/HazRob-Tank`، الفرع: `research/chemcore80-central-evidence-gap-analysis-20261021`.
- قراءة `research/techniques/07.10/cpython_research_profile_v2.py` عبر موصل GitHub `fetch_file`.
- نقل المحتوى إلى `/mnt/data/hazrob_source_transfer/research/techniques/07.10/cpython_research_profile_v2.py`.
- حساب بصمة Git blob (SHA-1 على `blob <bytes>\\0<content>`) في Python؛ النتيجة `39bd34a8f92c6ac77ac02e93a96441cb94c7d368` تطابق GitHub.
- اختباران محليان مباشران نجحا: عقد CPython ورفض ادعاء قياس ESP32. **2/2 PASS** لهذا الملف فقط.
- الأدلة المحلية: `TRANSFER_AUDIT.json` و`RUN_LOG.txt` ضمن حزمة `HazRob_G1_verified_transfer_step01.zip` المتاحة في المحادثة، وليست ملفاً مدفوعاً إلى المستودع.

## الإجراء 2: قيود ومتابعة
- ملفات الدفعة السابعة السابقة المعاد بناؤها محلياً ليست بالضرورة متطابقة مع GitHub؛ لا تستخدم نتائجها كدليل اعتماد.
- بقية ملفات Batch07/08 **لم تنقل بعد ببصمات مطابقة**؛ حالة G1 الرسمية **60/80**.
- GitHub Actions سابقاً أعاد `steps=[]` و404 للسجلات؛ لا يوجد CI PASS حديث.
- المطلوب تالياً: نقل باقي الملفات مع Git blob equality لكل ملف، ثم تنفيذ مجموعة الاختبارات الأصلية على الملفات المتطابقة، وتحديث ledger بعد التحقق. G3 الفيزيائي 0/80.

## الإجراء 3: مراجعة التغطية وإصلاح CI (2026-10-26)
- اكتُشف نقص مثبت في workflow المستقل: لم تكن اختبارات 08.10 و10.1 مدرجة.
- أُضيف أمر unittest discover لملف `research/techniques/08.10/test_source_multioutput_fit_v2.py` وآخر لـ `research/techniques/10.1/test_joint_negative_evidence_v2.py`، commit `c6c1bcbd40bd2a5e7c2fa48e656e53575bf13f9f`.
- أُعيد تنفيذ 22 اختباراً محلياً للدفعة السابعة على `/mnt/data/g1_batch07`: `Ran 22 tests ... OK`. **نسخة محلية معاد بناؤها**؛ بصمات Git blob لمعظم الملفات لا تطابق الأصل، لذا لا ترفع G1.
- الاختبارات الجديدة في workflow **لم يثبت تنفيذها عبر Actions**؛ لا يجوز اعتبار إصلاح تغطية CI مساوياً لنجاحه.
- الخطوة التالية: نقل بقية ملفات GitHub الأصلية حرفياً إلى البيئة التنفيذية مع فحص SHA-1 blob لكل ملف، ثم اختبار 22+7+14+11+11+10+10 ومراجعة السجل.
