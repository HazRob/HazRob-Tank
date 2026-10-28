# HazRob Tank V5 — محاولة إغلاق الخطة كاملة في دفعة واحدة

**التاريخ:** 2026-10-28 (+03:00)  
**الفرع:** `feature/tank-v5-implementation`  
**HEAD عند إعداد التقرير:** `659be35c79c7bfc1d1aa356027cf1c2a24b565a7`  
**تعليمات المالك:** استكمال جميع التعديلات والاختبارات، وإنتاج APK V5 فقط إذا اكتمل كل ذلك.  
**الحالة النهائية الصحيحة:** `RELEASE_GATE=BLOCKED`، `APK_NOT_BUILT`، `GRADLE_NOT_RUN`، `ESP32_NOT_FLASHED`. لا يوجد ملف `V5_RELEASE_GATE_APPROVED.md`، ويجب ألا يُنشأ دون أدلة قبول حقيقية.

## 1. الفحوص التي نفذت في هذه الدفعة بالفعل

1. **تدقيق payload حقيقي، لا مؤشرات نصية فقط:** فُتح ملف APK V4 الذي سلّمه المالك `HazRob-Tank-V4-debug.apk` محليًا واستُخرج `assets/generated/chemcore/embedded_stable/manifest.json` و`chemcore-stable-snapshot.json`. نجح SHA-256 لمحتوى 1,744,726 بايت، بإجمالي 652 هوية كيميائية، و49 prior منها 43 M1 و6 M2، وعدد نماذج M4 المعتمدة صفر.
2. **اختبار عددي فعلي لجميع المعادلات المنشورة في ملف المكتبة:** `49/49` معادلة LOG10_LINEAR اجتازت فحص المعاملات والمجال واسترجاع التركيز بالمعادلة العكسية في نقطة هندسية وسط مجال المصدر، خطأ نسبي دون `1e-9`. 8 حساسات MQ و13 مادة/فئة، و8 علاقات للإيثانول (legacy_id 103). لا تمثل دقة تجريبية على الروبوت. ملف التقرير المحلي `/mnt/data/HazRob_Tank_V5_Stable_Numerical_Audit.json`.
3. **اختبار SQLite فعلي في Python باستخدام نسخة مماثلة من SQL V5:** فحص ترحيل 4 روابط قديمة مع الحفاظ على 3 موافقات تاريخية، وإعادة تشغيل الترحيل دون ازدواج، وإضافة 3 دورات مستقلة داخل جلسة موجودة، ورفض ازدواج sequence واختيار صفر فئات. `PASS`. هذه fixture على SQLite في Python، **وليست** استدعاء `ExperimentsDb.onUpgrade` على Android. الملف المحلي `/mnt/data/HazRob_Tank_V5_SQLite_Migration_Fixture.py`.
4. مراجعة الشيفرة وحفظ إصلاحات حماية manifest+payload من الانتقال خارج HTTPS/GitHub/HazRob، والتحقق من عدم تجاوز مجلد النشر أو تمرير Query/Fragment/مسار مشفر، وإضافة `TankV5GithubSourceBoundaryTest` (اختبارات JUnit **مكتوبة وغير مشغّلة**).
5. تحديث فيرموير R0: منع بدء `/measure` أثناء مراجعة R0 المعلقة، ومنع قبول R0 أثناء القياس، وإلزام `warmSeconds=300` و`sampleSeconds=60` مع اشتراط **مرور 300 ثانية بالفعل منذ بدء المعايرة** حتى لو كان MQ قد سُخّن قبلها؛ تحديث اختبارات العقد. بقاء سجلَّي NVS المتناوبين وعدم مساس المفتاح القديم.
6. تحسين توافق Android API26: استبدال `List.copyOf` في `TankMultiSourceResponseModel` بعرض قائمة غير قابلة للتعديل متوافق مع API26.
7. تصحيح اختبار `HR069EmbeddedStableContractTest` ليتحقق من توافق Stable مع `ReleaseAudit.VERSION=5.0` بدل `4.0` الثابتة في الاختبار القديم.
8. تحديث `SiteSummaryReportExporter` لعرض PPM البحثية تحت `ppm/EST` بعلامة `~`، و`M4` فقط للمعايرة المهنية المتحققة؛ بيانات المصادر في XLSX منفصلة أساسًا، ولا متوسط تركيز عابر للجلسات. أُضيف اختبار مصدر `TankV5SessionSiteCycleReportContractTest`.

## 2. جرد بوابات الخطة V5-W00..W11

| المجموعة | ما أنجز في source | ما يمنع الإغلاق |
| --- | --- | --- |
| W00 Baseline | هوية المستودع ونسب النسخة القديمة والـSQLite v10 موثقة؛ APK V4 فعلي تدقق مكون Stable فيه | مفتاح توقيع V4، اختبار بيانات عميل فعلية، build runner |
| W01 دورة الجلسة | 60+60، حالات المحاولات، رابط المالك، أرشفة الحساسات، SHA شامل للقرار العلمي والتقارير | اختبار ESP32/Android فعلي، ضغط متكرر/انقطاع/خلفية، استرداد عند قتل التطبيق، 3 جلسات ×3 دورات |
| W02 R0 | 300+60، `PENDING→accept→NVS A/B→phone` ورفض/إلغاء/إعادة | Compile/flash لـESP32، قطع الطاقة، اختبار جميع حالات فشل NVS وثماني قنوات |
| W03 Ethanol | كتالوج Stable مشترَك ولوحة قدرات للمادة و8 مراجع Ethanol في بيانات V4 | تشغيل UID/SQLite/Stable على APK V5 وفحص البحث والواجهة |
| W04 منحنيات MQ | جرد المصدر 43+6 وتجربة عكسية عددية 49/49 ناجحة؛ عدم حجب المعادلات بمقياس M4 | مراجعة/توثيق كل ورقة/ترخيص وMQUnifiedsensor والخطأ التجريبي والـG3 الحقيقي |
| W05 Stable updates | GitHub-only URL+payload fail closed، rollback والإصدار والـSHA | لا قناة GitHub عامة منشورة مؤكدة، ولا اختبار install/download/rollback على Android |
| W06 unified ChemCore | واجهة واحدة مع fingerprint/PPM/تحذير المرشح، ومدخلات المصدر مشتركة | تفاعل الهاتف، 144 مرشحًا، حدود exposure/LFL على حالات قياس حقيقية |
| W07 Developer lab | مختبر/تجارب في workspace واحد وصلاحية PIN | تدقيق كل إجراءات المرجع/المقاييس وملفاتها من الواجهة الفعلية |
| W08 Developer library | أربع صفحات وقيم عدم التوفر وrollback ونسخة Stable | لا اختبارات الصفحات الأربع على الهاتف ولا مصدر إحصاءات المنصات العام |
| W09 Driving HUD | السهم الأخضر، Echo أعلى الوسط، إخفاء tab strip أفقيا | فحص التراكب وقطع التحكم عند التدوير وE-STOP وEcho/IR على الجهاز |
| W10 Checkboxes | مربعات عكس الحركة بأطر واضحة | اختبارات contrast/Touch وقيادة محركات حقيقية |
| W11 Release | Manifest/Workflow V5 يدوي مؤمَّن، وترقية SQLite إضافية في المصدر | Gradle JUnit، Android instrumentation، ESP32 hardware، توقيع APK، تثبيت وترقية وإعادة اختبار تقارير فعلية |

## 3. سبب عدم إنتاج APK رغم تصريح المالك المشروط

لا يوجد Gradle ولا Android SDK ولا Arduino CLI في بيئة تنفيذ هذه المحادثة، والتواصل المباشر مع GitHub من سطر الأوامر غير متاح. يمكن حفظ الشيفرة في المستودع عبر اتصال GitHub، لكن **لا يمكن إثبات تشغيل JVM/Android والفيرموير** بهذا الأسلوب. المالك لم يطلب ملف Debug تجريبي غير مكتمل، بل **ربط إذن بناء V5 باستكمال كامل الخطة والاختبارات**.

لا تُدخل `V5_RELEASE_GATE=PASS` أو `OWNER_BUILD_PERMISSION=CONDITIONAL_COMPLETION_GRANTED_2026-10-28` في ملف اعتماد جديد الآن. ذلك سيجعل workflow يبني APK رغم عدم استيفاء شرطه.

**المتبقي المطلوب للقرار الهندسي:** GitHub Action/JUnit حقيقي، compilation/bench لفريموير ESP32، smoke tests على الهاتف، تقارير PDF/XLSX من 3 جلسات×3 قياسات على الأقل، اختبار ترحيل/توقيع V4→V5، قناة Stable حية أو قرار صريح باعتبار تنزيلها اختياريًا مع استمرار عمل المكتبة المضمنة. هذه أدلة خارجية لا يمكن اختلاقها.

## 4. الاستئناف

ارجع إلى هذا الملف ثم `V5-RESUME-CHECKPOINT-2026-10-28.md`، وافحص HEAD الفعلي. لا تُعد أي علامة `SOURCE`/Python fixture بديلًا عن `DEVICE_PASS` أو `APK_VERIFIED`. صفة الإصدار الصحيحة حاليًا:

`V5_SOURCE_INTEGRATION_ADVANCED / REAL_STABLE_NUMERICAL_49_49_PASS / SQLITE_FIXTURE_PASS / JUNIT_NOT_RUN / ESP32_NOT_TESTED / APK_NOT_BUILT`.
