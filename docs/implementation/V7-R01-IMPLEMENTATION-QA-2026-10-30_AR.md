# V7-R01 — تقرير التنفيذ والتحقق من مولّد PDF الأصلي
**Date:** 2026-10-30 | **Report:** R01 Instant ChemCore Gas PDF | **Status:** `IMPLEMENTATION_COMPLETE_ANDROID_QA_PASS_OWNER_FINAL_VISUAL_ACCEPTANCE_PENDING`
**Base:** `feature/tank-v7-reports` | **Verified code commit:** `c5b9f6e87b9883001699885f4f1cd6cdb6fb85ed`
**CI success:** [Actions run 41135056334](https://github.com/HazRob/HazRob-Tank/actions/runs/41135056334)
**Artifact:** `HazRob-Tank-V7-R01-Original-Android-PDF-QA` (id `13016715647`)
**PDF SHA-256:** `7a2d285543c58d5e5c536a07662ac48a378ceb84ab08cfd76e47adf5c54978e4`

## 1. ما نفذ في مصدر Android
- `android/app/src/main/java/com/servoc/app/report/TankInstantV7PdfWriter.java`: مولّد A4 رأسي أصلي **Android PdfDocument/Canvas**، عربي/إنجليزي، رأس أسود، شعار السداسي الرسمي، لون `#FFBE00`، timestamp من العملية، تذييل رمادي، خلايا 2×1 مم، خط 12–15 نقطة، `StaticLayout` بتباعد أسطر 1.15.
- `android/app/src/main/java/com/servoc/app/report/TankInstantReportPdf.java`: تحويل تقارير `hazrob-tank-v6-android-telemetry-bridge/1` إلى مولّد V7 الجديد بدل مولّد V6 القديم؛ المنظومة الكيميائية نفسها لم تُعدّل.
- `android/app/src/main/java/com/servoc/app/MainActivity.java`: ربط `Context` مورد الشعار الرسمي عند تشغيل التطبيق لتمكين Drawables في PDF.
- `android/app/src/main/res/drawable/hazrob_official_report_mark.xml`: المصدر المتجهي المشتق من شعار المستخدم المرفوع (مورد أضيف سابقًا).
- **المخطط الأول:** `Evidence Matrix`: 8 حساسات × [Rs موجود/R0 موجود/engine-qualified R0/ratio returned] مع تمييز رمادي/داكن، دون اتخاذ حكم قياس جديد داخل PDF.
- **المخطط الثاني:** `Radar`: نسبة `rs_r0_ratio_research` التي أعادها المحرك، بوسم `Research only`، دون الإيحاء بأنها هوية غاز/ppm ميداني.
- جدول MQ مفصل مع 8 صفوف وأعمدة `Sensor/Rs/R0/Rs-R0/Status`، محاذاة RTL وعناوين جدول برتقالية سوداء بحدود.
- بطاقات عدد قنوات MQ، حالة ppm، الهوية التي أعادها المحرك؛ قسم قرار محرك ChemCore دون فلترة إضافية.
- صفحات إضافية تلقائية إذا كان `source_curve_hypotheses` يحتوي سجلات عديدة، وفق العدد الفعلي للسطور المعروضة.

## 2. QA من مصدر التطبيق ذاته
- **GitHub Actions 41135056334: `SUCCESS`**.
- **Android build/test APK** `assembleDebug`: `PASS`. هو APK اختبار مؤقت بتوقيع CI؛ **ليس إصدار تثبيت للمستخدم أو ترقية إنتاجية**.
- **Android emulator API33**: تطبيق Android الحقيقي أنشأ ملف `HazRob_Tank_V7_R01_Actual_Android_Report.pdf` بمولّد الإنتاج الجديد. `PdfRenderer` فتح الصفحة وأكد وجود محتوى.
- **تحقق PDF على المضيف**: `595×842 pt`، صفحة واحدة في حالة الاختبار، خط أصغر `12 pt` وأكبر `15 pt`، شعار رسمه Android، رأس أسود ولون أصفر برتقالي ظاهر، لا قص أو تراكب في الصورة النهائية بعد تصحيح موضع رادار.
- نسخة المعاينة الأصلية جرى فحصها بصريًا بعد استخراجها من CI. **التداخل السابق بين MQ2 ورأس الرادار** أزيل في SHA المصادق عليه أعلاه.
- **بيانات التحفيز:** صريحة أنها `SYNTHETIC QA ONLY`، ليست قراءات Tank الحقيقية، و`G3=0`، والهوية `UNKNOWN` والـppm غير مؤهلين.
- لا يُثبت نجاح حالة عرض واحدة تخطيطات الملاحق الطويلة، مكونات حدود GHS المستقبلية، الاعتماد الميداني أو قبول التصميم من المالك. تعد هذه اختبارات لاحقة قبل إصدار APK.

## 3. سجل إخفاقات CI المصححة
- `41133733032`: بناء Android `PASS`، واجهة المشغل CI استعملت `sh` مع `pipefail`، ففشل قبل تشغيل PDF. عولج بتحويل الاختبار إلى `bash script`.
- `41134124841`: بناء Android `PASS` وتوليد PDF، أخفق QA بسبب تحقق باسم ملف V6 رغم أن الملف باسم V7. أُصلح اسم التحقق.
- `41134485012`: **PASS** لتوليد/فحص PDF؛ مراجعة بصرية كشفت تداخلًا بين تسمية Radar ومحور MQ2، فجرى تعديل تموضع العنصر.
- `41135056334`: **PASS FINAL SOURCE / EMULATOR PDF / FONT / LOGO / DATA QA** في SHA `c5b9f6e`.

## 4. نقاط المتابعة
- حالة R01 **مكتمل تقنيًا لهذا التصميم الأولي، بانتظار قبول المالك للعرض النهائي**. لا تُرفع `production_release_authorized` ولا يُنشأ APK V7 تسليم.
- بحث الواجهة في R02: `docs/implementation/V7-R02-PHYSICAL-REPORT-ANALYSIS-PROPOSAL-2026-10-30_AR.md`، **اقتراح فقط** لا تعديل في `TankPhysicalSnapshotPdf.java`.
