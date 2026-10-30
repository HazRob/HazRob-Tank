# ChemCore Tank V6 — Checkpoint 21: Android PdfRenderer evidence for actual N06-C field research appendix

**التاريخ:** 2026-10-30  
**الفرع:** `feature/tank-v6-implementation`  
**مرجع N06-C:** [Checkpoint19](V6-CHEMCORE-NEW-ENGINE-CHECKPOINT-19-N06C-REAL-REPORT-OUTPUTS-2026-10-30_AR.md) و[Checkpoint20 SQLite migration](V6-CHEMCORE-NEW-ENGINE-CHECKPOINT-20-N06D-SQLITE-MIGRATION-2026-10-30_AR.md).

## 1. إثبات التنفيذ الميداني للعرض على محاكي Android (وليس الروبوت)

**النتيجة الحاكمة:** [GitHub Actions 40978812570](https://github.com/HazRob/HazRob-Tank/actions/runs/40978812570) **SUCCESS**. بدأ Workflow بنسخة أساس Android `sohpany/SERVOC-Android@6281d3eb7c1c961ce63b0b5e4ae0792874626720` ووضع مصادر HazRob Tank V6 في مساحة CI **مؤقتة**، وأضاف Activity الاختبار من `research/chemcore_v6/tests/android/V6ResearchPdfSmokeFixtureActivity.java` إلى AndroidManifest المؤقت. لم تدخل Activity في برنامج الإنتاج، ولم يُنشر توقيع APK مؤقت على المستخدم.

1. بُني APK debug **مؤقت للاختبار فقط** بمفتاح ينتجه CI آنيًا وتُتلف حيازته بانتهاء التشغيل؛ لا يصلح للترقية فوق التطبيق المثبت سابقًا.
2. شُغّل محاكي Android API 35 مع صلاحية Linux `/dev/kvm`، وتم تثبيت التطبيق المؤقت وتشغيل Activity.
3. استخدمت Activity طريقة `SmartFieldSessionPdfWriter.v6ResearchAppendixPage(...)` الأصلية نفسها عبر reflection، مع **7 دورات جلسات اختبارية مصطنعة**، ولكل دورة 8 طرازات MQ؛ ست قنوات بنتيجة بحثية غير معتمدة وقناتان بأقل من 10 scan cycles.
4. أُنشئ PDF فعلي من Skia Android `PdfDocument` بمقاس **842×595 pt (A4 أفقي)**، و**3 صفحات** في ناتج واحد (**3+3+1** دورات). فُتح الملف الناتج فعليًا باستخدام `PdfRenderer` في Android وأُنتجت ثلاث صور **842×595 PNG**. تم التحقق من أن كل صفحة ليست بيضاء/فارغة وأن `status=PASS`.
5. نُزِّلت الملفات من المحاكي وتحُققت بصمات SHA-256 وملفات PNG والـPDF بصريًا في بيئة المراجعة المحلية، بالإضافة إلى `pdfinfo` و`pdftotext`. نصوص العربية الأساسية ظهرت من دون مربعات بديلة، لكن نصوص التفاصيل صغيرة (نحو 6.9pt)، وبعض المتتاليات المختلطة Rs/R0/رموز لاتينية متلاصقة في استخراج النص؛ **القراءة البصرية والتموضع النهائي ما زالا بحاجة تحسينًا قبل طرح التقرير النهائي**.

## 2. ملفات الدليل (كلها مولدة حقيقة في محاكي Android)

| الملف | الحجم | SHA256 |
|---|---:|---|
| `v6_pdf_fixture.pdf` | 178,744 بايت | `9416101a1ad4560e6dae2cee20b4cadda11479bb2f2ea374c506de404132cbbc` |
| `v6_pdf_fixture_page_1.png` | 100,407 بايت | `2127a124ab7d7fe51627a6517f3ffcfa6681ffb9d9d6c8e1407bb5863b18224c` |
| `v6_pdf_fixture_page_2.png` | 100,509 بايت | `6b797abc5fa877d66ac38179c15cc07678c3fa9825c3c96` |
| `v6_pdf_fixture_page_3.png` | 48,468 بايت | `a469156a62649360a64fabc98b4024f42013a6175104845a31f77fb03c199eb2` |

**أرشيف Artifact CI** `12944101188`، ZIP SHA256 `467627e50babd08768ae6832fa3ba5359cc9aae28ca9190599d55ab5683188b1`. يحتوي ملفَي `v6_pdf_status.json` و`v6_pdf_emulator_verified_manifest.json` بالإضافة إلى PDF والصور الثلاث.

**نسخة محفوظة في مكتبة المستخدم:** `/HazRob/MQ_Reference_Measurements_2026/HazRob_Tank_V6_N06C_Android_PDF_Raster_Proof_2026-10-30.zip`، معرف `libfile_27a5671b735081919b189397e31b7b77`.

## 3. الحوادث والحلول الموثقة

- [40975702846](https://github.com/HazRob/HazRob-Tank/actions/runs/40975702846): فشل بناء APK المؤقت بسبب غياب مفتاح توقيع source؛ حُلّ بإنشاء مفتاح تجريبي قابل للإتلاف داخل CI فقط، وليس مفتاح إصدار.
- [40975895004](https://github.com/HazRob/HazRob-Tank/actions/runs/40975895004): فشل تشغيل المحاكي لا ملف PDF؛ غابت صلاحية `/dev/kvm` واستغرق الإقلاع 16 دقيقة وانتهى بفشل ADB input. حُلّ عبر تهيئة صلاحية `/dev/kvm` في CI.
- [40978108804](https://github.com/HazRob/HazRob-Tank/actions/runs/40978108804): أقلع المحاكي في نحو 30 ثانية، لكن `script: set -euo pipefail` فشل لأن runner يستخدم `/bin/sh`.
- [40978449015](https://github.com/HazRob/HazRob-Tank/actions/runs/40978449015): وصل إلى تثبيت Activity وتشغيلها، لكن runner ينفذ أسطر YAML script منفردة مما كسر حلقة `for`.
- **الحل النهائي:** `research/chemcore_v6/tests/android/run_v6_pdf_emulator_smoke.sh` (commit `0edfa814578484ab32780c067ca1c3ece51180a4`)، ويشغّله workflow `chemcore-v6-n06c-emulator-pdf-smoke-ci.yml` بسطر واحد `bash ...` (commit `bc344d4859a76257d3cad23da9f6399223be152e`)، ثم **40978812570 SUCCESS** مع جميع الملفات.

## 4. ما تم إثباته وما لم يتم إثباته

**تم:** توليد صفحات PDF المعتمدة برمجيًا كملحق بحثي من شيفرة الإنتاج، باستخدام Android PdfDocument، وإعادة فتحها ورسمها بواسطة Android PdfRenderer، وفحص الصور/الأحجام/الأدلة. كذلك أُثبت في N06-C أن Excel يصدر OOXML/ZIP حقيقيًا من مصدر V6 نفسه حتى 100 دورة.

**لم يتم:** تمرير **تقرير جلسة معتمدة كاملة** من `FieldSessionReportExporter` مرورًا بقاعدة SQLite حقيقية للمستخدم إلى PDF ثم Excel، ولا اختبار الـPDF على هاتف المستخدم، ولا اختبار كل رسوم ومخططات V5 الموجودة. ما زالت سلامة بعض أحجام الخط وقراءة Rs/R0 المختلطة تتطلب تعديلًا وتحسينًا. لا اعتماد ppm أو تحديد غاز على الروبوت، ولا اجتياز فيزيائي G3، ولا نسخة APK موقّعة للإصدار والترقية.

**الحالة:** 78/80 تنفيذ تقنيات بحثية جزئي، تقنيتان غير منقولتين؛ **Tank G3=0/80**. نضارة الأغذية مستقلة. لم يتغير Firmware للمحركات أو تسخين MQ.

**Resume token:** `V6_CHECKPOINT21_N06C_REAL_ANDROID_PDF_RENDERER_PASS / CI_40978812570 / PDF_A4_842x595_3_PAGES / 3_ANDROID_PNGS_VISUAL_INSPECTED / 7_SYNTHETIC_CYCLES_8_MQ_MODELS / FINAL_TEXT_SIZE_POLISH_NEEDED / FULL_APPROVED_SESSION_EXPORT_NOT_END_TO_END / NO_RELEASE_APK / 78_PARTIAL_2_UNPORTED_G3_0`.
