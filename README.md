# HazRob Tank — آخر بناء للتطبيق والفريمويرين: V6 / Latest development builds V6

> **V7 development track is now open (2026-10-30):** [`feature/tank-v7-reports`](https://github.com/HazRob/HazRob-Tank/tree/feature/tank-v7-reports) is a **reports-first Android development branch**, forked from V6 source at `cf818a9e2ea1064da37f0d9fdf6beb0de4847250`. V7 is **not** the latest built APK, has **not** been released, and does **not** supersede the V6 firmware or approved results. Consult [V7 report baseline and design plan](docs/implementation/V7-REPORTS-DEVELOPMENT-START-2026-10-30_AR.md) and [V7 execution tracker](docs/implementation/V7-REPORTS-EXECUTION-TRACKER.json) before work. No report-template change may silently change measurement evidence, ChemCore decisions or field approval data.

> **V7 R01 PDF design milestone (2026-10-30):** The actual Android instant-gas report generator is implemented with the approved official right-header logo, fixed black/amber/gray A4 layout, 12pt+ bilingual typography, a bordered MQ table, a sensor evidence matrix and an exploratory radar. **Build + Android emulator PDF QA PASSED**: [Actions 41135056334](https://github.com/HazRob/HazRob-Tank/actions/runs/41135056334), source `c5b9f6e87b9883001699885f4f1cd6cdb6fb85ed`; PDF SHA256 `7a2d285543c58d5e5c536a07662ac48a378ceb84ab08cfd76e47adf5c54978e4`; QA artifact [HazRob-Tank-V7-R01-Original-Android-PDF-QA](https://github.com/HazRob/HazRob-Tank/actions/runs/41135056334). The fixture measurements are explicitly synthetic; the physical MQ G3 gate is unqualified. **No V7 release/upgrade APK has been produced.** [R01 implementation/QA log](docs/implementation/V7-R01-IMPLEMENTATION-QA-2026-10-30_AR.md) · [R02 next-report design proposal](docs/implementation/V7-R02-PHYSICAL-REPORT-ANALYSIS-PROPOSAL-2026-10-30_AR.md).


> **تحديث موثّق: 30 أكتوبر 2026.** أحدث تطبيق Android تم بناؤه، وأحدث فيرموير للتحكم ESP32-Tank، وأحدث فيرموير للكاميرا Cam-tank في هذا المشروع هي جميعًا **الإصدار السادس V6**. المقصود أحدث **حزم تطوير مجمّعة وقابلة للاختبار**، وليس اعتماد إصدار إنتاجي أو إثبات سلامة القياسات على الروبوت الحقيقي. الفرع المعتمد للتطوير: [`feature/tank-v6-implementation`](https://github.com/HazRob/HazRob-Tank/tree/feature/tank-v6-implementation). أما `main` فهو فرع المستودع الافتراضي وليس دليلًا على دمج جميع ملفات V6 إليه.

## سجل أحدث الإصدارات / Current version register

| المنتج | آخر إصدار V6 | ملفات المصدر / الحزمة | دليل البناء |
| --- | --- | --- | --- |
| **تطبيق الدبابة Android APK** | **V6** — `versionName=6.0`, `versionCode=6`, `applicationId=com.hazrob.tank`; Android 12 | [تحميل Artifact واستخراج `HazRob-Tank-V6-Diagnostic.apk`](https://github.com/HazRob/HazRob-Tank/actions/runs/41102976044/artifacts/12999965690) | [Actions 41102976044 — SUCCESS](https://github.com/HazRob/HazRob-Tank/actions/runs/41102976044)، عند `213f3bf89288fcdde76eebdbc7ee0d2973eeb139`؛ توقيع **Debug** وليس توقيع ترقية مثبتًا |
| **فيرموير ESP32-Tank** | **V6** — `ESP32-Tank 0.1.6-v6-quickr0-dev`؛ API 4 | [حزمة ESP32-Tank V6، مشروع Arduino + BIN](https://github.com/HazRob/HazRob-Tank/actions/runs/41093126933/artifacts/12995795016)؛ افتح `ESP32-Tank/ESP32-Tank.ino` | [Actions 41093126933 — SUCCESS](https://github.com/HazRob/HazRob-Tank/actions/runs/41093126933)، تجميع Arduino-ESP32 **3.3.11** للوحة `esp32:esp32:esp32` |
| **فيرموير Cam-tank (ESP32-CAM)** | **V6** — `Cam-tank 0.1.4-v6-candidate` | [حزمة Cam-tank V6، مشروع Arduino + BIN](https://github.com/HazRob/HazRob-Tank/actions/runs/41093126933/artifacts/12994304170)؛ افتح `Cam-tank/Cam-tank.ino` | [Actions 41093126933 — SUCCESS](https://github.com/HazRob/HazRob-Tank/actions/runs/41093126933)، تجميع Arduino-ESP32 **3.3.11** للوحة `esp32:esp32:esp32cam` / AI Thinker |

**مهم لتنزيل الملفات:** ملفات GitHub Actions من نوع Artifact تصل كأرشيف ZIP. حزمة التطبيق تحتوي **ملف APK مستقل** بعد فك الضغط، وليست تطبيقًا بصيغة ZIP. حزم الفريموير تحتوي ملفات مصدر Arduino، بما فيها `.ino` و`.h` و`.cpp`، بالإضافة إلى ملفات BIN وملف معلومات البناء. يجب فتح مشروع `.ino` من مجلده داخل Arduino IDE؛ **لا تستخدم Add .ZIP Library**.

## التغييرات المنجزة في مسار V6 / V6 change log

1. **Android وواجهات المشغّل:** هوية `HazRob Tank` مستقلة عن HazRob رباعي العجلات؛ الدخول إلى التطبيق لا يشترط تساوي نص رقم نسخة Android وفيرموير ESP32. جرى تطوير واجهات القياسات، المختبر، البحث الكيميائي، حالات المواد، المواقع والجلسات والتقارير، مع بقاء اختبارات الاستخدام الفعلية منفصلة عن نجاح التجميع.
2. **ChemCore V6:** إنشاء مسار استدلال مستقل `com.hazrob.tank.chemcore.v6` بدل تحويل نتائج محرك HazRob القديم إلى نتائج مؤكدة. تستخدم البصمات ومنحنيات MQ وميزات القياس والاستدلال المرجعي لأغراض بحثية؛ **لا يثبت ذلك هوية غاز أو ppm معتمدًا**. حالات الهواء النظيف، غياب R0 الموثوق، وعيوب القياس لا تطلق إنذارات غاز وهمية. يبقى تحذير Echo وإيقاف الحركة المستقلين عن بوابة ادعاء الغازات.
3. **معايرة R0 V6 موحّدة:** **180 ثانية تسخين + نافذة قياس واحدة 60 ثانية**، مع قبول منفصل وبيانات مصدر ومعالجة NVS؛ لا يُستخدم R0 منشور من دراسة أخرى كمعايرة لروبوت الدبابة. لا توجد قيمة R0 ميدانية مصطنعة داخل نسخة V6.
4. **المكتبة الكيميائية الموحدة SQLite V14:** **734 مادة بمعرّفات UID مستقلة** (منها 725 لها CAS مباشر أو رابط مرادف موثق و9 مسميات غير مفردة)، و**49 منحنى/مرجع MQ بحثي** و**25 ملاحظة R0/Rs خارجية** محفوظة مع سياق المصدر. القيم الناقصة تبقى `NULL`، والتصنيف المرجعي GHS لا يُرقّى تلقائيًا إلى إنذار تشغيلي.
5. **Stable دون اتصال:** تضمين Stable `1.0.0-hr069r1` بــ **652 هوية جزيئية** و49 سابقة استجابة بحثية و**صفر معايرات M4 مثبتة على الدبابة**، مع فحص SHA-256 داخل بناء APK؛ طبقة Stable لا تحل محل سجل المواد الموحد ذي 734 UID.
6. **تحديث سجلات المكتبة بالاستعلام، لا تنزيل المكتبة كاملة:** `ChemicalReferenceSyncV6` يستعلم عن فهرس السجلات المعدلة حسب UID/CAS وSHA-256 ويجلب عند الحاجة السجل الصغير المتغير (حتى 25 سجلًا في الطلب)، دون استبدال Stable الكامل في عملية التحديث المعتادة. تتحدث قوائم المواد وحالات **البصمة | منحنى MQ | PPM مرجعي | M4 مرجعي** مباشرة بعد التطبيق. قبول حالة `AVAILABLE` أو `QUALIFIED` المنشورة مسؤولية المكتبة المركزية؛ الهاتف يتحقق فقط من سلامة النقل والهوية والبنية، ولا يعيد التأهيل العلمي.
7. **إصلاحات آخر اللمسات:** سحب خاصية منشورة يُزيلها من جميع القوائم؛ السجل المنشور الخالي من الخصائص يسحب **القدرات الأربع كلها**؛ وجود مصدر إضافي بحالة `AVAILABLE` لا يُنزل حالة `QUALIFIED` لمصدر آخر. أضيفت اختبارات مصدر ومحاكي لهذه الحالات. [اختبار المصدر العلمي 41102007946 — SUCCESS](https://github.com/HazRob/HazRob-Tank/actions/runs/41102007946).
8. **المواقع والجلسات ونتائج الفحص:** تدقيق ترحيل قاعدة البيانات، اعتماد الجلسات والحذف، تقارير PDF/Excel، فصل الملاحق البحثية عن النتائج المؤهلة، وتصنيف GHS. وظيفة **نضارة الأغذية (اللحوم/الفواكه)** مستقلة عن ChemCore المخصص للغازات.
9. **فيرموير ESP32-Tank:** سيرفوان مستمران للجنزير، حد مهلة أمر القيادة **500 ms**، قياسات MQ عبر ADS1115، BME على I²C مستقل، Echo للتحذير فقط، وإجراءات R0 الجديدة. **Cam-tank:** مشروع كاميرا AI-Thinker مستقل، عرض MJPEG محلي؛ معدل الإطارات والاتصال البعيد لم يُثبتا بعد على الجهاز الحقيقي.

## حالة القبول وحدود V6 / Verification and release status

- **PASS برمجي:** بناء APK تجريبي ووجود Stable المدمجة وتوقيع APK التجريبي، وتجميع الفريمويرين، واختبارات مصدر علمي محددة. أحدث APK هو **V6 diagnostic**؛ لا يساوي `production release`.
- **ليس PASS ميدانيًا:** تفليش وحدتي ESP32 وESP32-CAM على العتاد، اتجاه المحركات وإيقاف الطوارئ وسلامة الاتصال، المعايرة بالغازات المرجعية، معدل FPS الفعلي، وتأكيد دقة هوية الغاز وppm. **G3 للروبوت الحقيقي = 0/80** حسب سجل المشروع.
- **توقيع Android:** شهادة V5 المثبتة لم يثبت تطابقها مع مفتاح APK التجريبي V6؛ قد يرفض Android التحديث المباشر. **لا تحذف التطبيق السابق قبل حفظ بياناته وجلساته**.
- **قناة نشر تحديثات المواد:** الفهرس `updates/chemcore-records/v6/index.json` الموجود بفرع التطوير بدأ فارغًا؛ لا يوجد إثبات نشر سجلات مؤهلة إنتاجية على `main`. لا يُشترط اختبار **تنزيل Stable الكامل عبر HTTPS** لإثبات طريقة **الاستعلام الجزئي** المتفق عليها؛ يلزم اختبار استعلام بيانات منشورة فعلًا عند توافرها.
- **بوابة الإنتاج:** `docs/implementation/V6-ANDROID-SOFTWARE-ACCEPTANCE.json` ما زالت `NO_RELEASE`؛ إذن تسليم نسخ التطوير لا يعني إغلاق جميع بوابات إصدار إنتاجي معتمد. لا تُرفع أعلام القبول دون أدلة.

**سجل الاستكمال:** [خطة قبول Android V6](https://github.com/HazRob/HazRob-Tank/blob/feature/tank-v6-implementation/docs/implementation/V6-ANDROID-COMPLETION-GATE-2026-10-30_AR.md) · [ملف القبول](https://github.com/HazRob/HazRob-Tank/blob/feature/tank-v6-implementation/docs/implementation/V6-ANDROID-SOFTWARE-ACCEPTANCE.json) · [تحديث السجلات الصغيرة](https://github.com/HazRob/HazRob-Tank/blob/feature/tank-v6-implementation/docs/implementation/V6-INCREMENTAL-CENTRAL-LIBRARY-SYNC-2026-10-30_AR.md) · [GitHub Actions](https://github.com/HazRob/HazRob-Tank/actions).

---

> **أرشيف الإصدارات السابقة:** الأقسام التالية تخص نقاط التقدم V2–V4 في وقت كتابتها. أرقام النسخ والعبارات من نوع "current" و"latest" فيها **تاريخية وليست الحالة الحالية**؛ سجل V6 أعلاه هو المرجع للأحدث.

## V4 engineering checkpoint — 2026-10-24 (UNRELEASED)

- **Working branch:** [feature/tank-v4-implementation](https://github.com/HazRob/HazRob-Tank/tree/feature/tank-v4-implementation). Owner has authorized implementation. Canonical [V4 requirements](docs/requirements/V4-UPDATE-BACKLOG-2026-10-24.md), [execution plan](docs/implementation/V4-EXECUTION-PLAN-2026-10-24.md), and [durable progress log](docs/implementation/V4-PROGRESS-LOG-2026-10-24.md) govern continuation.
- **Committed source changes:** identity-stable site/session Spinner callbacks and guarded approval, independent SAF state for one-shot PDF, real SQLite session-count regression added to V4 CI, GradientDrawable fill-aware contrast, preserved completed instant report after link loss. Source assertions are **not** full UI or device qualification.
- **Build status:** V4 GitHub Actions [run 40047526874](https://github.com/HazRob/HazRob-Tank/actions/runs/40047526874) failed; rerun also failed with no job steps returned and expired/unavailable job logs. **No confirmed V4 Gradle build, installable standalone APK, signed upgrade, firmware release, or acceptance-tested device result.**
- **Next mandatory gates:** working Actions/Gradle runner; JVM + real SQLite tests; user database migration/backup; handset Logcat for sessions crash and RTL/contrast; same-operation ChemCore/candidates/charts/PDF verification; stable signing and actual robot safety tests. Do not delete old V3 data or merge research-grade data into Stable. The original four-wheel HazRob remains untouched.

---

## V3 independent test artifacts (2026-10-20)

- Android debug APK (not a signed stable/upgrade release): `HazRob-Tank-V3-debug.apk`, built and tested by [CI 39490117129](https://github.com/HazRob/HazRob-Tank/actions/runs/39490117129). Its GitHub Actions artifact is ZIP-wrapped for archive download, but the extracted distributable is **the standalone .apk**. SHA256 `09d06f276ccc0758113491a5042dfec4587152a3c7479604b71433f82d3aa1c6`.
- Main ESP32 Tank Arduino sketch: `HazRob-Tank-V3-ESP32-Arduino.zip` — extract/open `ESP32-Tank/ESP32-Tank.ino`; pinned board core **2.0.17**; [full compile + bundled firmware binaries CI 39479359692](https://github.com/HazRob/HazRob-Tank/actions/runs/39479359692).
- Independent camera Arduino sketch: `HazRob-Tank-V3-ESP32-CAM-Arduino.zip` — extract/open `Cam-tank/Cam-tank.ino`; board **AI Thinker ESP32-CAM**, pinned core **3.3.11**; [compiled CI 39492657140](https://github.com/HazRob/HazRob-Tank/actions/runs/39492657140), source ZIP SHA256 `463d945dfece9c5b32015b5db5789ade0e35c012ce04bcf56146e2aa4248d08e`. Camera version `Cam-tank 0.1.3-v3-dev`.
- Every Arduino ZIP is a **sketch directory** (not a library). Extract it and open the matching `.ino` in Arduino IDE. All are **UNRELEASED candidates**: safe servo/hardware, camera streaming, end-to-end R0 and stable APK upgrade signing remain separate release qualification blockers; remote camera publishing is NAPT-gated and not available with the current 2.x main controller config.

# HazRob Tank — Independent Tracked Robot

> **Development handoff, 2026-10-18.** This README is the authoritative starting point for the next conversation and **v3 development**. Do not release another APK until the additional requested changes and acceptance tests are complete.

## V3 active implementation checkpoint — 2026-10-20 (NOT RELEASED)

- **Current work branch:** [`feature/tank-v3-implementation`](https://github.com/HazRob/HazRob-Tank/tree/feature/tank-v3-implementation). **No V3 release APK** has been published, the branch has not been merged into develop, and do not present any debug Actions artifact as a user-ready signed release.
- **Continue after interruption:** [V3 execution plan](docs/implementation/V3-EXECUTION-PLAN.md) and [chronological execution log](docs/implementation/V3-EXECUTION-LOG.md). Read these **before** making further edits; each records source commits, passing/failing CI, blockers, and per-requirement status.
- **Latest CI-tested Android V3 source:** `dda17d08bf2d2e82ca3c371cd686c9b85233cfae` — [run 39485877592](https://github.com/HazRob/HazRob-Tank/actions/runs/39485877592), **SUCCESS** for JVM regression, real SQLite approved-report scope, debug APK assembly and ChemCore snapshot checksum. All 20 chart selector evidence cases passed separately at [run 39485600871](https://github.com/HazRob/HazRob-Tank/actions/runs/39485600871). **Release signing skipped**, so the diagnostic APK is not a production update.
- **Implemented in V3 source (still unverified on real Tank):** offline mode with hard network block, 6 compact refresh overlays excluding drive, 2-up physical emoji cards, calibrated-only 90 dBA instantaneous red card display, 20-minute user clean-air R0 workflow and confirmed local save provenance/normalizer, dedicated site management vs approved reporting, approved-only field/site report data filtering, enlarged true-data report charts, frozen on-click physical PDF.
- **Tank V3 firmware:** `ESP32-Tank 0.1.3-v3-dev` has passed full Arduino-ESP32 2.0.17 build and been packaged as clearly **UNRELEASED** test .bin and Arduino files with SHA256, [run 39479359692](https://github.com/HazRob/HazRob-Tank/actions/runs/39479359692). The 500 ms neutral-stop and no autonomous reversing require real servo and link-loss trials before flashing/deployment.
- **Operator-facing Arabic acceptance matrix:** [V3 device test record](docs/implementation/V3-DEVICE-ACCEPTANCE-AR.md). All real-device outcomes remain NOT TESTED until witnessed and documented.
- **Outstanding release blockers:** stable Tank APK signing keystore/secrets, actual Android UI inspection (no emulator), complete site/session migration and safety tests, real 20-minute sensor calibration/ RL validation and ChemCore live activation, tracked robot motion watchdog/E-STOP/link-loss hardware tests, app installation/Play Protect diagnostics, plus still-pending backlog functionality.
- The original `sohpany/SERVOC-Android` remains read-only, pinned to `6281d3e`; no four-wheel code or deprecated simulator workflows should be introduced. Current V3 candidate `com.hazrob.tank` versionName `3.0`, versionCode `3` for CI.

## Project identity and separation

- Repository: **HazRob/HazRob-Tank**, working branch **develop**.
- Independent Android application ID: **`com.hazrob.tank`**; display name **HazRob Tank**.
- Independent tracked-robot firmware: **`firmware/ESP32-Tank/`**; Android overlays: **`android/app/src/main/java/`**; ChemCore stable data bundled at build time.
- The original four-wheel HazRob application in **`sohpany/SERVOC-Android`** must **not** be changed. Android baseline is pinned at commit **`6281d3eb7c1c961ce63b0b5e4ae0792874626720`** and copied into a separate build workspace. Changes belong in this Tank repository only.
- Never conflate HazRob Tank, HazRob Mini and the original HazRob. Firmware, Android app and ChemCore lifecycle must be handled independently.

## Verified build status versus development HEAD

| Component | Last known state | Verification |
| --- | --- | --- |
| Tank firmware | **`ESP32-Tank 0.1.1-dev`**, engineering revision **`TANK-SERVO-R1`**, **API 4** | Source in `firmware/ESP32-Tank/Config.h`; static API contract passed in earlier CI; **hardware validation pending** |
| Android version | **v2**, `versionName 2.0`, `versionCode 2`, package `com.hazrob.tank` | Generated from pinned Android baseline plus Tank overlays |
| Last **successful** APK build | Commit **`df92e9ccd2d7d8b69b83fff5515569d298734dc4`**, [Actions run 39084627573](https://github.com/HazRob/HazRob-Tank/actions/runs/39084627573) | Unit tests, APK assembly, signature verification and presence of embedded ChemCore assets passed. Artifact: `HazRob-Tank-v2-debug-apk`. **Debug-signed, not an upgrade-stable release** |
| Subsequent development | Commit **`ec8861da7a9c8141d4a6554cfb39a8b7cc5acaca` added offline entry; previous **`a2e8b4f0`** required persistent signing secrets | [Actions run 39086020081](https://github.com/HazRob/HazRob-Tank/actions/runs/39086020081) **failed**. **No validated APK containing offline entry** |
| V3 separate development branch | **V3 debug candidate assembled and firmware .bin compiled** | CI source verified; **NOT a signed/stable release**, no physical acceptance and no merge to `develop` |

**Do not claim that HEAD has passed CI simply because an earlier APK did.** Reinspect the latest Actions job logs and rerun the full pipeline after edits.

## Android v2 functionality implemented or prepared

1. Four measurement workspaces: chemical search/tracking, live ChemCore analysis, physical sensor measurements, and particle/PM view. The PM view must explicitly show unavailable hardware rather than fabricated concentrations.
2. Search and selection over the chemical library; ranked candidates and analysis results; session/site workflows, reports and developer/operator separation.
3. Light operator workspace with dark legible text, amber actions, Arabic/RTL layouts, scrolling lists and safety highlighting.
4. Chemical concentration and safety result fields turn **bold red** only for applicable, validated reference crossings; distinguish instantaneous comparison from time-integrated OSHA exposure. Do not invent PPM, LEL/LFL, chemical identity or confidence.
5. Physical reference comparison class: `android/app/src/main/java/com/servoc/app/measurements/PhysicalReferencePolicy.java`. It guards against unsupported instant comparisons. Current examples: WHO 2021 **PM2.5 15 µg/m³ / 24 h**, **PM10 45 µg/m³ / 24 h**, WHO UV index **≥3** when UV measurement is validated, indoor humidity guidance with explicit occupancy context; noise requires calibrated **dBA and duration**. This is **not** a completed regulatory standards engine. Physical tests: `android/app/src/test/java/com/servoc/app/measurements/PhysicalReferencePolicyTest.java`.
6. Clearing red/bold safety styling when readings disappear, become invalid or return to nonexceedance state.
7. **Pending release:** `android/app/src/main/java/com/servoc/app/auth/ConnectionGate.java` now adds **«الدخول دون اتصال • استعراض التطبيق»** on the entry screen. This should navigate for inspection **without** claiming a robot connection or enabling unsafe drive commands. Verify it on device and verify other screens in offline mode.
8. Entry screen still inherits the original HazRob branding/version footer; revise to **HazRob Tank v3** and verify independent branding in v3.

## ChemCore Stable in APK

- The build executes **`tools/build_hr069_embedded_stable.py`** from the pinned original Android source into Gradle's **`app/build/generated/hazrobAssets/generated/chemcore/embedded_stable/`**. **Do not** also generate it under `app/src/main/assets`: that caused a duplicate-assets failure.
- Manifest expected **`library_version = 1.0.0-hr069r1`**, **652 chemical identities**, **0 calibration records**; payload SHA-256 and length checked.
- APK verification checks `assets/generated/chemcore/embedded_stable/manifest.json` and `chemcore-stable-snapshot.json`.
- Tank `MainActivity` invokes **`ChemCoreEmbeddedStableInstaller.ensureInstalled(getApplicationContext())`** on startup. Embedded presence and build checks passed in the last successful APK, but **runtime installation, active-library status and analytical correctness still need on-device tests**. Embedded research priors are not a substitute for calibrated measurements.

## ESP32-Tank firmware contract

- Source of truth: `firmware/ESP32-Tank/ESP32-Tank.ino`, `Config.h`.
- API **4**, watchdog **500 ms**, endpoints include `/drive`, `/estop`, `/estop/reset`, `/relay`, `/measure`, `/data`, `/diag`, `/cfg`.
- Two rear continuous-rotation servos, eight MQ inputs through two ADS1115, BME environmental bus separate from ADS bus, Echo, microphone, IR/light and power diagnostics as implemented. Exact wiring and servo direction are provisional and require physical commissioning; see `Config.h`.
- No installed PM sensor: do not manufacture PM readings. Validate motor stop, watchdog, E-STOP, relay, sensor health and physical units with real hardware before deployment.

## Installation and signing — critical for v3

The v1 and successful v2 debug APKs used ephemeral GitHub Actions debug signing keys. Their signatures can differ, so Android may refuse an in-place upgrade. The user confirmed they are willing to remove **v1**; **do not** assume this solves signing for future updates.

Commit **`a2e8b4f0`** changed the v2 workflow to **require** a persistent Tank signing keystore through repository secrets:

- `TANK_KEYSTORE_BASE64`
- `TANK_KEY_ALIAS`
- `TANK_KEYSTORE_PASSWORD`
- `TANK_KEY_PASSWORD`

**These secrets must be configured securely by an authorized repository administrator.** Never commit the keystore, signing passwords or base64 keystore to the repository or a chat. Retain secure backups of the same keystore for all later releases. Current v2 pipeline may fail until these are provided. Keep application ID fixed and increment `versionCode` above 2 for v3 (e.g. 3, provided no higher version was already installed); use the **same signing certificate** for every future release. Test installing v3 over its signed predecessor without deleting data. Do not present the old debug-signed v2 as an upgrade-compatible baseline.

The user requested a **standalone APK download, not a ZIP**. GitHub Actions artifacts are ZIP archives. For v3, publish the signed APK as an **individual GitHub Release asset** or provide a separately verified standalone APK link. Do not falsely label an Actions artifact ZIP as a direct APK download.

## User-reported installation and entry issues

- Installing a new debug APK over v1 produced Android package conflict; removing v1 from Android Settings permitted installation.
- Play Protect displayed a **harmful app blocked** warning. **Cause is not established**; review signing, permissions (including `REQUEST_INSTALL_PACKAGES`), network behavior and package integrity; do not suggest bypassing protection as a routine installation procedure.
- User confirmed v2 launched, but its entry page lacked a **skip/offline entry button**. Code fix added in `ec8861da`, **not yet validated in an installable build**.

## v3 work plan / acceptance criteria

1. **Preserve the independent Tank repository and pinned baseline.** Begin from `develop`; inspect latest commits and failed workflow logs. Do not revert to older original HazRob application versions.
2. **Fix CI at HEAD**, confirm compilation and all applicable tests. The workflow excludes three legacy source-text tests that demand the original dark theme/old refresh implementation. Replace these with meaningful Tank-specific tests rather than treating exclusion as full UI validation.
3. **Finish persistent signing setup** (repository administrator), establish an upgrade-compatible baseline, increment version and correct v3 branding and entry footer.
4. **Test offline entry** on device: visible button, functional navigation, no false connection or telemetry, no drive commands without confirmed robot link.
5. **Complete physical standards coverage** by source, unit, calibration, exposure duration and context. Use WHO, OSHA, ASHRAE and other primary references where applicable; no universal limits for altitude, pressure, wind direction, luminous flux, etc. Add explicit averaging/validity provenance and tests. Do not call an instantaneous value a confirmed OSHA TWA exceedance.
6. **Review chemical/molecular safety emphasis** across all analysis panes, reports and alerts. Apply bold red only to valid threshold crossings, and reset styles on invalid/disconnected telemetry.
7. **Check ChemCore Stable runtime activation** and actual sensor/chemical analysis with controlled, calibrated data; maintain zero fabricated PM measurements.
8. **Validate firmware API4** against Android commands and on actual tracked hardware, especially emergency stop, drive neutral, watchdog, power supply and relay behavior.
9. **Build and sign v3**, verify signature/certificate fingerprint, package identity, versionCode, embedded ChemCore manifest/hash, tests and on-device upgrade. Publish **standalone APK** only after checks. Preserve prior app data across subsequent upgrades.
10. **Document every new change and test** in README and commit on `develop`; do not release prematurely.

## Instructions for a new ChatGPT conversation

> Continue independent **HazRob Tank v3** development in GitHub repository **`HazRob/HazRob-Tank`**, branch **`develop`**. **Read README.md and inspect the current repository and latest Actions results before editing.** Android base: `sohpany/SERVOC-Android@6281d3eb7c1c961ce63b0b5e4ae0792874626720` (read-only; never modify original). Firmware: `ESP32-Tank 0.1.1-dev`, API4. Last successful Android v2 debug APK: commit `df92e9cc`, Actions run `39084627573`. Later offline entry change `ec8861da` has **not** passed a full release build; persistent signing change `a2e8b4f0` requires GitHub secrets. ChemCore Stable `1.0.0-hr069r1` (652 identities) must remain embedded and verified. Preserve light Arabic RTL UI, real sensor data only, threshold provenance, emergency safety, separate project identity, and future in-place upgrade compatibility. **Do not release APK until requested v3 changes are finished and CI/device checks pass; provide a standalone signed APK, not an Actions ZIP.** Continue implementing user-requested changes with GitHub commits and update README.

