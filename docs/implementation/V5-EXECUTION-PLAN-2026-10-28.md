# HazRob Tank Android V5 — خطة التطوير الهندسية المعتمدة ونظام الاستئناف

**المرجع:** [متطلبات V5-001 ... V5-011](../requirements/V5-CHANGE-REGISTER-2026-10-28.md)  
**الفرع:** `feature/tank-v5-implementation`  
**الانطلاق:** `acf490c0ebda413c79ba59b5db085828177690a9` (نسخة V4 الموروثة وتوثيق تأسيس V5).  
**تاريخ التحديث:** 2026-10-28 (+03:00)، بعد ورود 11 تعديلًا من المالك.  
**حالة الخطة:** `PLAN_REGISTERED / SOURCE_AUDIT_PARTIAL / IMPLEMENTATION_NOT_STARTED / V5_APK_NOT_BUILT`.

## قواعد هندسية غير قابلة للتنازل

1. **مشروع مستقل:** لا تعديل `sohpany/SERVOC-Android` أو روبوت العربة أو مستودعات البحث الأصلية. لا يُدمج فرع V4 أو فرع MQ مع V5 دمجًا شاملًا؛ تُنقل التعديلات اللازمة فقط بعد مقارنة شاملة.
2. **الحفاظ على البيانات:** لا حذف لجداول مواقع/جلسات/معايرة V4 أو تغيير لمعنى معرّفاتها. ترحيل SQLite إضافي مع معاملة، نسخة اختبار مأخوذة من DB قديمة، وخطوة استرداد. ارتقاء التطبيق لا يشترط إزالة APK السابق؛ التوقيع والترقية لهما بوابة منفصلة.
3. **السلامة:** زمن/حالة الريلاي، E-STOP، منع حركة غير مقصودة، Echo/IR، R0 وصلاحية MQ7/MQ9 تخضع لاختبارات حقيقية. لا يعتبر نجاح محاكي أو Gradle كافيًا للسلامة الصناعية.
4. **ChemCore:** الحساب التقديري PPM مسموح عند توافر نموذج موثق وصالح، دون الادعاء أن كل نموذج مساوي لمعايرة ذات غاز مرجعي. يُفصل `ESTIMATED` عن `REPORTABLE` و`REGULATORY`.
5. **الإصدارات:** في جميع واجهات V5 يُعرض إصدار فعلي مستخرج من BuildConfig/Stable active manifest، وليس نصوص `V3/HR069/V4` قديمة. لا تتغير بيانات الماضي المخزنة لمجرد تحديث العناوين.
6. **عدم التكرار:** دمج شاشات ChemCore/المرشحين والمختبر/التجارب يكون بتوحيد Controller/Model/Repository، وليس تجاور نسختين أو حساب التحليل مرتين.
7. **دليل الإنجاز:** لكل مهمة Commit SHA واختبار تشغيل/نتيجة، وتحديث سجل الاستئناف؛ `SOURCE_PRESENT` لا يساوي `TESTED_PASS`، ولا إعلان ملف APK حتى وجود ملف فعلي صالح.

## نتائج الجرد الأولية (قراءة مباشرة من فرع V5)

| البند | الدليل الموجود فعلاً في الشيفرة | التغيير الهندسي اللازم |
| --- | --- | --- |
| V5-001 | `FieldSessionsTab` يضيف الموقع والجلسة فقط؛ `FieldSessionRepository.approveFieldSession` يرفض إذا `complete!=1`، والإغلاق/الاعتماد متعلقان بالجلسة كلها؛ `TankAcquisitionPolicy` يميز `INSTANT/DOCUMENTED` نظرياً. | إدخال دورة قياس مستقلة وقائمة المحاولات، قبول دورة منفردة، دعم عدة دورات في جلسة واحدة، ضمان التوقيت والربط والتقارير. |
| V5-002 | `MeasurementsTab` يُنشئ ثلاث شاشات تحليل `القياس الآني/ChemCore/المرشحون` ويستخدم `TankPpmEstimate` و`TankScreeningExposureWarning` لنتائج تقديرية وتنبيهات مشروطة. | حذف تكرار العرض والنتيجة؛ واجهة نتائج موحدة للـPPM التقديري والمرشحين وحدود التعرض مع شارات ☠/🔥 تراعي جودة الدليل. |
| V5-003 | نافذة المادة تستدعي `TankChemicalEvidence.inspect` الذي يقرأ الجداول المحلية أساسًا؛ `TankStableScreeningModelRegistry` يعالج نماذج Stable النشطة في مسار آخر. | مخزون capabilities موحد مصدره التشغيل الفعلي + cache ذي هوية المادة؛ اختبار Ethanol 64-17-5. |
| V5-004 | `ControlTab` يستخدم `HazardOverlayView` و`setGasDirection(angle,confidence)` بالفعل. | تحسين طبقة الرسم فقط مع فصلها عن أوامر الحركة وخريطة اتجاه التقدير. |
| V5-005 | `firmware/ESP32-Tank/Hardware.cpp` يحسب `Rs` و`R0` عبر معادلات مقاومة/عامل الهواء النظيف؛ تطوير `MQUnifiedsensor` مذكور في خطة الفيرموير، لكنه **ليس مثبتًا كاستدعاء لمكتبة خارجية في الشيفرة الحالية**. | جرد مكتبة MQUnifiedsensor وأمثلتها فعليًا ومقارنة معاملات منحنياتها بالمراجع، وتصميم مستورد آمن ومستوى دليل M1/M2 فقط قبل التحقق. |
| V5-006 | `SettingsTab.checkAnalysisUpdate` و`StatusTab.performUnifiedUpdate` موجودان؛ `CentralLibraryDashboardTab` يعرض `BuildConfig.CHEMCORE_LIBRARY_MANIFEST_URL` ويقول Fail Closed إن كان فارغًا. | اختبار قناة GitHub الحقيقية، عدم اعتبار عدم نشر manifest فحصًا ناجحًا، ثم تثبيت ذرّي مع rollback ومؤشر حالة يقرأ active manifest. |
| V5-007 | `DeveloperTab` يحوي `DeveloperR0Tab`؛ ينادي `calibrationStart(300,60)` ويقبل `FirmwareR0Contract.committedValues` بعد حفظ NVS في ESP32. | نقل ملكية الواجهة للإعدادات، معايرة ذات أزرار قبول/إعادة/إلغاء، وعدّاد واقعي وربط قرار القبول ببروتوكول firmware. |
| V5-008 | `DeveloperExperimentsTab` يجمع التجارب والمصادقة والمراجع؛ `AnalyticalLabTab` يضم `ChemCoreAnalysisTab` والمقاييس والتشخيص في وحدة ثانية. | UX موحّد بدورة مخبرية واحدة وصفحات فرعية غير متكررة مع shared state ولا حذف للموافقات أو سلاسل المصدر. |
| V5-009 | `ControlTab.applyLandscapeFullscreenChrome` يخفي بعض الأجزاء بنبش شجرة ViewParents، بينما أزرار التبويبات ينشئها `MainActivity.buildShell`. موقع Echo الآن `RIGHT|TOP`. | تصريح landscape layout مباشر يملكه MainActivity، badge `CENTER_HORIZONTAL|TOP` ومصفوفة تنافر لعناصر الفيديو/E-STOP. |
| V5-010 | `CentralLibraryDashboardTab` فيه أربعة أقسام فعلية: لوحة/توزيع/منصات/سجل؛ يحتوي بعض نصوص HR069 وأسماء مصدر `HazRob/build-harness` وmetadata اختيارية قد تكون فارغة. | فحص كل مصدر وAPI ونتيجة تحديث، وإظهار `UNAVAILABLE` عند فقد المصدر بدل بيانات وهمية، وإزالة تعابير إصدار موروثة من النصوص الحية. |
| V5-011 | `SettingsTab.directionReverseCheck` ينشئ `CheckBox` بنص داكن لكن بلا حدود خلفية محددة. | شكل حد وخلفية وحالة check/switch متباينة، مع الحفاظ على أثر عكس اتجاه القيادة. |

## مجموعات التنفيذ وترتيب الاعتماد

### P0 — تأسيس وتثبيت الحالة (قبل تغيير UX)
**V5-W00 — Baseline & reproducibility**
- [ ] تثبيت SHA الأساسي، جرد `BuildConfig.VERSION_NAME/VERSION_CODE` والتوقيع/الإصدار V4، توقيت API4، وفحص الأخطاء المفتوحة ومجلدات الكاميرا/الفيرموير.
- [ ] استخراج جرد overlay + Android original pinned؛ كل ملف يعتمد على مصدر موروث يُذكر صراحة.
- [ ] حصر schemas `field_sessions/field_session_measurements/measurement_sessions/telemetry_samples` ومنشأ ونسخة `ChemCore Stable`.
- [ ] بناء fixtures **ببيانات اصطناعية** لجلسة V4 قديمة فيها دورة واحدة وجلسة V5 فيها عدة دورات، وحالة مكتبة بدون إنترنت، و8 قنوات R0.
- **المخرج:** `V5-BASELINE-MANIFEST.json` (commit, SHA, branch, source origin, schema, APK package, data preservation risks), تشغيل فحوص مصدرية فقط في البداية.

### P1 — نموذج قياس الجلسات وR0 (V5-001 + V5-007) — الأعلى أولوية
**V5-W01 — عقد دورة الجلسة**
- [ ] تصميم `FieldMeasurementCycle` و`FieldCycleStateMachine` و`FieldCycleRepository` مع `cycle_id, field_session_id, attempt_no, sequence_no, device_id, modes, warmup_started_at, sample_started_at, sample_ended_at, decision_at, relay_on_verified, status, quality_summary, calibration_id, stable_version, evidence_sha`.
- [ ] ترحيل SQL **إضافي** يضمن FK وindexes وunique per field session sequence + تأريخ حالات دورة V4 القديمة دون العبث بالقياسات التاريخية.
- [ ] نقل اعتماد الدورة إلى عملية ذرية `approveCycle()` مع تثبيت سجل قياس `measurement_session_id` غير قابل للاستبدال؛ `retryCycle()` ينشئ محاولة جديدة، و`deleteCycle()` يعيد تدقيق الروابط ويرفض حذف الدليل المعتمد دون سياسة أرشفة/تأكيد.
- [ ] تغيير `approveFieldSession` بحيث يعتمد إغلاق الجلسة على **جميع الدورات المستهدفة** دون الشرط `complete!=1`، مع تعريف واضح لقبول الجلسة التي تحتوي بعض المحاولات المرفوضة والأخرى المعتمدة.
- [ ] سلسلة التقدم: `IDLE/REQUESTING_RELAY/WARMING/SAMPLING/READY_TO_REVIEW/APPROVED/RETRY_REQUESTED/FAILED/CANCELLED`. الأزرار فقط بعد `READY_TO_REVIEW`؛ مقاومة الضغط المتكرر وnavigation/rotation/restart.
- [ ] توقيت 60+60 باستخدام وقت monotonic والمرجع الفعلي لريلاي MQ وقراءات firmware، لا `CountDownTimer` وحده؛ تعطل الاتصال/الريلاي/القناة يوقف الاعتماد ويحفظ سبب الانقطاع. لا يوجد قياس جزيئات اصطناعي.
- [ ] حفظ بيانات الغازات والفيزيائية والجزيئات في نفس السجل مع **وحدات ومصادر/عدد العينات مستقلة**؛ إذا لم يتوفر PM يعرض `UNAVAILABLE` ويُسمح للمستخدم بإلغاء اختياره قبل البدء.
- [ ] رسوم وتقارير PDF/XLSX للجلسة والموقع: قسم لكل قياس معتمد + تواريخ ومحاولات وحالات + نتائج ChemCore المتجمدة، دون تجميع PPM حسابي غير قابل للمقارنة بين دورات مختلفة.
- **اختبارات:** SQL upgrade/fresh, simultaneous start, process kill, tap twice, relay off, no-ADS, PM absent, retry provenance, delete and export; موقع به 3 جلسات × 3 دورات.

**V5-W02 — معايرة R0 في الإعدادات**
- [ ] نقل الواجهة الحالية دون تكرار الـController إلى `SettingsTab`، وإزالة دخول R0 من `DeveloperTab` والتأكد من سلامة صلاحيات المستخدم.
- [ ] 300s warmup + 60s full capture، أرقام زمن/عينات الحساسات الثمانية وحالات `WARMUP/SAMPLING/REVIEW/FAILED` واضحة.
- [ ] زر `اعتماد` لا يُفعّل قبل تحقق كل 8 قنوات (>=10 عينات صحيحة) وسلامة الهوية وNVS/إعادة القراءة. `إعادة` تُعيد الدورة، `إلغاء` يوقفها دون تغيير السجل المعتمد.
- [ ] **Firmware protocol impact:** مراجعة نقل الالتزام من «نهاية 60s» إلى `candidate/pending` ثم `/r0 accept` مع إلغاء/rollback، واختبار توافق API4/‏V4، وعدم الإيحاء بأن المعايرة القديمة لن تتغير إذا كان firmware لا يدعم الحفظ المرحلي بعد.
- [ ] يمنع تشغيل معايرتين/تجربة/قياس في الوقت ذاته، ويدعم استرداد الحالة بعد إعادة فتح التطبيق والتراجع من manual R0 أو استيراده عند اختيار المستخدم.
- **اختبارات:** حالات 8 قنوات × فشل قناة واحدة، >=10 لكل قناة، uptime، انقطاع الكهرباء قبل/بعد NVS، معايرة جهاز B بدل A، reconnect، عدم تجاوز رقمي مستتر.

### P2 — المصدر العلمي وStable وPPM (V5-003 + V5-005 + V5-006 + V5-002)
**V5-W03 — تشخيص Ethanol والكشف عن إمكانات المادة**
- [ ] إنشاء `ChemicalCapabilitiesRepository` من القنوات `ACTIVE_STABLE + BUILTIN + LOCAL + VERIFIED` مع CAS/UID/legacy mapping (الاعتماد على UID وحده بعد التطبيع).
- [ ] اختبار `Ethanol 64-17-5`: وجودها في SQLite، manifest/asset في APK، تفعيل snapshot، كشف 49 non-M4 priors (إجمالي ليس بالضرورة لإيثانول)، توافر model sensor محدد.
- [ ] Dialog يعرض لكل مسار `نوع النموذج + إصدار المكتبة + مصدر النموذج + مجال ppm + confidence/uncertainty + سبب عدم التوفر`.
- **اختبارات:** Stable missing, hash invalid, old UID mapping, duplicate CAS, no Ethanol curve, newly activated Stable, stale UI cache, lifecycle.

**V5-W04 — دراسة وتدقيق منحنيات MQ**
- [ ] مراجعة `MQUnifiedsensor` والإصدارات/الرخصة وأمثلة معاملات `setA/setB` ونوع الانحدار `setRegressionMethod` ونقل ADC `externalADCUpdate` ومقاومات `RL` بالكيلو أوم، مع مطابقة مستشعرات Tank الفعلية.
- [ ] مراجعة منحنيات المصنّع Winsen للـMQ المختلفة وحالة MQ7/MQ9 heater، وملفات البحث الموجودة في بنك المراجع؛ رقم `R0` بحثي لا يساوي معايرة جهاز المستخدم.
- [ ] توليد `candidate_priors.json` **غير نشط** مع مصدر وحقوق وإصدار وصلاحية نطاق وتحويل `log10(ppm) → log10(Rs/R0)` وأخطاء digitization، وحظر التعارض/ازدواج النموذج.
- [ ] مقارنة المرجع في اختبارات مستقلة `known gas` و`grouped holdout` قبل أي ترقية، وعدم حذف Stable الموجودة.
- **المخرج:** تقرير تدقيق علمي لكل حساس، وفصل `RESEARCH_CANDIDATE` عن `ACCEPTED_STABLE`.

**V5-W05 — تحديثات GitHub/Stable**
- [ ] جرد مصدر `CHEMCORE_LIBRARY_MANIFEST_URL` الفعلي من إعدادات Gradle/التطبيق وقناة GitHub المصرح بها؛ إذا كان فارغًا يجب إظهار `NOT_CONFIGURED`.
- [ ] `check → download temp → verify metadata+SHA+model ranges → atomic swap → activate → verify readback → rollback on failure`.
- [ ] عرض `embedded version` مقابل `active version` و`source` و`last checked` و`last installed` و`status` في `StatusTab` للمستخدم، مع تفاصيل/rollback للمطور.
- [ ] بعد التحديث إعادة بناء فهارس ChemCore/قدرات المادة في worker، وإبطال cache، وعدم حذف قواعد المواقع أو R0؛ لا تنشيط وسط أي جلسة/معايرة.
- **اختبارات:** offline, URL absent, 404, 500, partial download, payload tamper, downgrade without consent, mismatched app version, concurrent measurement, restore previous library, fresh reinstall.

**V5-W06 — تحليل موحد ومقياس PPM وتحذيرات**
- [ ] إلغاء ازدواج الواجهة بين `AnalysisPane` و`AnalysisResultsPane` بتشارك `AnalysisViewModel` وحزمة قرار واحدة immutable؛ يحتفظ `القياس الآني` بوظيفته المنفصلة.
- [ ] عرض PPM الرقمي البحثي من المنحنيات الصالحة داخل نطاقها دون شرط M4، مع `Method/Source/Range/Uncertainty/EvidenceTier`. لا يُعد `M1/M2` شهادة.
- [ ] `HazardIndicators`: رموز `☠` و`🔥` عندما تتحقق حدود المقارنة ذات المصدر على مرشح محدد، و`☠🔥` معًا عند اجتماع الحالتين. تمييز `warning` عن `confirmed exceedance` و`10% LFL` عن `100% LFL` و`TWA` عن المقارنات اللحظية.
- [ ] توحيد نتيجة الشاشة/التتبع/تقرير PDF/XLSX مع UUID العملية؛ لا خلط بالتحليل السابق عند انتهاء دورة جديدة أو تبديل جهاز.
- **اختبارات:** synthetic math, no-M4 but valid prior, no sensor fit, boundary 0.1/1.0 LFL, TWA hypothetical, Ceiling, IDLH, ppm/vol%/mg/m³ mismatch, gas mixture, both icons, no false-safe.

### P3 — إعادة تنظيم واجهات المطور والقيادة (V5-008 + V5-010 + V5-004 + V5-009 + V5-011)
**V5-W07 — مختبر مطور موحد**
- [ ] إنشاء `DeveloperLabWorkspace` موحد (تجارب معتمدة/مقارنة نماذج/تحليل الجلسات/مراجعة ونشر)، ونقل وظائف `DeveloperExperimentsTab` و`AnalyticalLabTab` دون تعطيل الأدلة أو اعتماد المرجع.
- [ ] إزالة التكرار الفعلي في حالة القياس/قوائم النتائج، مع الاحتفاظ بأزرار PDF/XLSX، اعتماد المصدر وتوقيع/مراجعة غير تشغيلية؛ مُحدَّث Stable ينعكس فوراً.
- [ ] الإبقاء على صلاحية المطور لتعديل التجارب والمكتبة، وعدم إرجاع معايرة R0 إلى قائمة المطور.
- **اختبارات:** كل إجراء مطور قديم مُقارن بمسار V5 جديد واحد فقط، لا فرق في عدد سجلات التجارب/الملفات أو هوية المصدر.

**V5-W08 — مطور/المكتبة: أربع صفحات فعالة**
- [ ] بناء `LibraryDashboardAudit` لكل (لوحة/توزيع/منصات/سجل) مع بيانات حالة فعلية من active manifest, GitHub channel, telemetry الموثقة، وchangelog إصدار المكتبة.
- [ ] استبدال تسميات `V3/HR069` الموروثة حيث تعني إصدار التطبيق الحالي فقط؛ لا تنقيح تاريخ النتائج السابقة.
- [ ] `UNAVAILABLE/NOT_CONFIGURED/STALE` عند فقد البيانات بدلاً من الإفصاح عن فحص ناجح بلا استجابة. جميع عمليات الشبكة في executor وأخطاء واضحة قابلة لإعادة المحاولة.
- **اختبارات:** 4 صفحات × offline/online/invalid manifest/old metadata × back/forward navigation مع مصدر وناتج واضحين.

**V5-W09 — القيادة: السهم والاتجاه الأفقي**
- [ ] سهم 3D-like مرسوم في `HazardOverlayView` برأس/ظل/منظور وأخضر شبه شفاف، متناسب مع دقة الفيديو، مع confidence indicator؛ لا يوجه الروبوت آليًا.
- [ ] معالجة وضع landscape في مالك نافذة `MainActivity` صراحة، وليس بحثًا هشًا في `ViewParent`؛ إخفاء tab strip، وضع Echo أعلى المنتصف، وحساب مساحات خالية بين HUD والقيادة/E-STOP والكاميرا وزر MQ.
- [ ] دعم تغيير اتجاه الجهاز أثناء عرض الفيديو والتسجيل/التوقف بدون إعادة تشغيل محركات أو فقد E-STOP.
- **اختبارات:** portrait/landscape، RTL، أجهزة صغيرة/كبيرة، تسجيل، مؤشرات gas/flame/Echo، joystick/sliders، E-STOP visible/tappable، screenshot pixel bounds.

**V5-W10 — وضوح Checkbox إعدادات التوجيه**
- [ ] تطبيق Theme-aware outlined stateful background أو `MaterialCheckBox` إن كانت المكتبة مثبتة، لحقل معكوس/غير معكوس ومؤشر focus/disabled/checked/unchecked متباين.
- [ ] التأكد أن حفظ اتجاه التوجيه محلي ولا يرسل حركة، وأن تغييره يرسل neutral stop كما في V4.
- **اختبارات:** screenshot light/dark وRTL، تفعيل/إلغاء على أكثر من قياس كثافة.

### P4 — الدمج والتحقق والإصدار
**V5-W11 — تكامل End-to-End**
- [ ] SQLite migration + correlated report cycles; نموذج التشغيل Instant منفصل تماماً عن Field، حالة R0 مستقلة عن جلسة القياس.
- [ ] Stable runtime status + Ethanol dialog + candidate ppm/icon + exports + developer lab + driving landscape + role gate تحت شبكة Direct AP وoffline وremote المسموح.
- [ ] Gradle + JVM + Robolectric/instrumentation حيث يتوفر + Emulator + **هاتف فعلي** بقاعدة بيانات موروثة، مع Logcat ونصوص عربية وصور ومصفوفة نتائج.
- [ ] Arduino firmware tests بالتجميع لكل من 2.0.17 و3.3.11 في حال تعديل /r0 أو حالة الريلاي؛ اختبار bench لم تعدّه JVM.
- [ ] رفع `versionName=5.0` و`versionCode` بصورة تصاعدية، مراجعة `applicationId=com.hazrob.tank` والشهادة المستمرة للتحديث دون إزالة نسخة V4.
- [ ] فقط بعد النجاح: APK V5 مع SHA-256، تقرير الاختبارات والمخاطر المتبقية، تحديث README/سجل التعديلات. لا يلزم تعديل ملفات فيرموير إذا لم يتغير العقد.

## سجل المصادر العلمية الأولية

بدأ **جرد المصادر** للبند V5-005 بصورة محدودة وموثقة في
[V5-MQ-CURVE-SOURCE-AUDIT-2026-10-28.md](../research/V5-MQ-CURVE-SOURCE-AUDIT-2026-10-28.md).
عُثر في أمثلة مكتبة MQSensorsLib على معادلتين تجريبيتين لمؤشر "Alcohol"
لـMQ3 وMQ135؛ **لم تُستورد أي معاملات إلى Stable** ولم تُختبر على الدبابة.
تدل نشرة MQ7/MQ9 على ضرورة مراجعة دورة سخان CO قبل الاستخدام الكمي.
لا تستخدم نتائج هذا الجرد وحدها لدعم اعتماد مهني لرقم PPM.

## سياسة التقدم والحد الأدنى لدليل كل بند

| Status | معنى الحالة وما يُقبل من دليل |
| --- | --- |
| `REGISTERED` | طلب من المالك محفوظ، بلا تنفيذ |
| `SOURCE_AUDITED` | ملف/سطر/سلوك محدد ثبت في المصدر؛ لا يعني إصلاحه |
| `DESIGNED` | معمارية وحالات وعمليات DB ومعايير قبول معتمدة |
| `IN_PROGRESS` | تعديل قيد العمل، commit جزئي وتوثيق الفجوة |
| `CODE_COMPLETE` | ملفات التطبيق والتبعيات محفوظة في commit؛ الاختبارات لم تُثبت بعد |
| `UNIT_PASS` | رقم run أو أوامر tests ومخرجات 0 فشل |
| `INTEGRATION_PASS` | API/SQLite/Stable/SAF مترابطة ونتائج مسجلة |
| `DEVICE_PASS` | تجربة فعلية موثقة بالـLogcat/قياسات، لا استنتاج من emulator |
| `BLOCKED` | المانع والمدخل المفقود والخطوة التالية محددة |
| `RELEASE_ACCEPTED` | جميع بوابات الإصدار المطلوبة اجتازت وتمت مراجعتها |

## تعليمات الاستئناف بعد الانقطاع

1. `git checkout feature/tank-v5-implementation`، ثم اقرأ هذا الملف، سجل المتطلبات، ومصفوفة الاختبار؛ تحقق من HEAD عبر GitHub لا من ذاكرتنا.
2. ابدأ بـ **أول مهمة `IN_PROGRESS` غير موثقة النجاح**، ثم أول `SOURCE_AUDITED/DESIGNED`، وأخيرًا أول `REGISTERED` وفق الاعتمادات.
3. لا تعتمد نتيجة CI من التزام قديم إذا تغيّر كود V5 بعده. وثق `SHA / changed files / exact test / output / PASS|FAIL / next action`.
4. اختبارات الفيرموير/الغاز/الترحيل على الجهاز لا تُرقّى إلى نجاح بسبب فحص static أو Java unit tests فقط.
5. لا تُبنِ APK V5 قبل توثيق الاعتمادات/قابلية الاسترداد، وبقاء الميزات الأصلية التي لم يُطلب تغييرها. إنشاء المشروع لا يعني نشر إصدار.

**التزام نقطة التخطيط هذه:** `REQUIREMENTS_11_REGISTERED / SOURCE_SCOPES_IDENTIFIED / NO_V5_CODE_CHANGES / NO_V5_BUILD`.


## تحديث تنفيذ المرحلة الأولى — بعد تأسيس الخطة

- `W00`: Source baseline manifest أُنشئ، مع ترقية تطويرية لرقم DB من 9 إلى 10. يبقى build manifest والإصدار النهائي pending.
- `W01`: قاعدة البيانات ودورات القياس والاعتماد أُنشئت مصدرًا، لكن **runner/UI/60+60/تقارير multi-cycle غير مكتملة**. نموذج ترحيل SQLite الاصطناعي اجتاز بعد إصلاح عامل النص `||`.
- `W02`: R0 أصبح قابلًا للفتح من الإعدادات والاعتماد الصريح على مستوى الهاتف؛ قبول الفيرموير قبل NVS لم يُطبق بعد.
- `W03`: مصدر الإمكانات الموحّد مع Stable أُنشئ؛ فحص Ethanol الحي على الهاتف معلق.
- `W05`: حالة ومصدر التحديث وSHA تظهر للمستخدم؛ قناة manifest والتحديث الحي تحتاج فحصًا فعليًا.
- `W09/W10`: السهم وإخفاء القوائم وموضع Echo وخانات عكس التوجيه تم تعديلها مصدرًا، دون تصوير أو اختبار شاشة.
- `W06/W07`: دمج ChemCore/المرشحين والمختبر/التجارب ما زال غير منفذ؛ **لا يُبنى APK**.
- **المصدر الدائم:** [سجل إنجاز الدفعة الأولى](V5-PROGRESS-LOG-2026-10-28.md). لا يستند أي ادعاء `TESTED_PASS` إلى هذه المرحلة.
