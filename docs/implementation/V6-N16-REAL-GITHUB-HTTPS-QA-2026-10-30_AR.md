# HazRob Tank V6 — مسار اختبار التحديث الشبكي الحقيقي N16

**التاريخ:** 2026-10-30، **الفرع:** `feature/tank-v6-implementation`، **حالة إصدار APK/الفيرموير:** `NO_RELEASE`.

## لماذا هذا الاختبار؟
اختبار N15 السابق ناجح بـ30 فحصًا على Android 12 لحالة التثبيت والتراجع داخل ذاكرة التطبيق، لكنه لا يختبر الـHTTPS الحقيقي. أنشئ N16 لاختبار `ChemCoreLibraryManager.check` ثم `install` على جهاز محاكى باستخدام تنزيل HTTPS فعلي عبر `raw.githubusercontent.com`.

### حواجز عدم نشر مصدر غير معتمد
- لا يرفع أي ملف إلى قناة `main` للإنتاج. كل تشغيل ينشئ فرع QA فريدًا: `qa/v6-stable-online-<GitHub Actions run id>` ويمسحه بخطوة `if: always()` بعد تجميع الدليل.
- المصدر هو نفس ملف Stable المثبت SHA256: `94f41ba754a1c239122b82081a91e9656012503cebda0eae6473ed8a4e45d7cf` بحجم 1744726 بايت، يحوي 652 مرجع هوية و49 منحنى/سابقة استجابة بحثية و0 تحقق M4؛ يرفع إصدار **اختبار فقط** `1.0.1-hr069r1` دون تغيير بيانات المصدر.
- خطوة `curl` على GitHub Actions تثبت وجود manifest+payload عن طريق HTTPS الحقيقي وتطابق بصمتهما، تليها عملية تنزيل ثانية من محاكي Android 12 مع SHA-256 وفحص الدلالة والمصدر وعدم إعادة التثبيت دون إصدار جديد.
- حظر انتحال مصدر GitHub الخارجي، وفصل مصدر Stable عن قاعدة بيانات التجارب، وعدم تحميل تطبيق APK للتسليم.

### سجل التنفيذ
- [N16 تشغيل 41094322532](https://github.com/HazRob/HazRob-Tank/actions/runs/41094322532): **FAILED** في مسار تسمية ملف Activity قبل أن يُنشأ الفرع؛ تم إصلاحه.
- [N16 تشغيل 41094506296](https://github.com/HazRob/HazRob-Tank/actions/runs/41094506296): وصلت ملفات المصدر إلى raw.githubusercontent.com وأُكدت بصمات manifest+payload من runner، لكن **FAILED** على محاكي Android برسالة `UnknownHostException: Unable to resolve host raw.githubusercontent.com`. السبب الظاهر DNS/شبكة المحاكي، لا رفض SHA256.
- أضيف خيار DNS صريح `-dns-server 168.63.129.16,1.1.1.1` للبيئة، وشُغِّل [N16 مجددًا 41094999163](https://github.com/HazRob/HazRob-Tank/actions/runs/41094999163). **نتيجته معلقة وقت تدوين هذا السجل**؛ لا يُسجّل PASS حتى ظهور JSON من الجهاز وتوافق checksum.

## بوابات ما بعد الاختبار
في حالة PASS، يسجل `actual_https_download_from_public_QA_branch_tested=true` و`actual_https_download_from_public_main_tested=false`، ولا يعتبر ذلك إذنًا لنشر Stable الإنتاجية قبل مراجعة المصدر. في حالة تعذر DNS للمحاكي، يسجل الاختبار **Blocked by emulator networking** دون تغيير كود العميل لمصدر أقل ثقة.

التوقيع نفسه عقبة مستقلة: V5 APK يحمل شهادة `97F0E315…`، والمفتاح V4 المسجل مختلف، لذا لا يدّعى التحديث الموضعي للـAPK دون مفتاح V5 أو مسار تصدير/استيراد مدقق. اختبارات ESP32/Cam-tank تجميع برمجي فقط؛ لا يُنسب لها تحقق ميداني.

التفويض المشروط الصريح للمستخدم (16:44:50Z): عند إغلاق كل بوابات القبول وإمكان إنتاج نسخ موثوقة، إصدار **APK V6 + Firmware ESP32-Tank وCam-tank**. لم تُفعّل أعلام `OWNER_APPROVED_RELEASE` بعد لأن شرط اكتمال الخطة لم يتحقق.
