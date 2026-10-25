# ChemCore Next — تدقيق معماري عابر للمستودعات (مرحلة 1)

**التاريخ:** 2026-10-26. **الحالة:** SOURCE_READ_AUDIT_IN_PROGRESS، وليست جرداً كاملاً لجميع الملفات والفروع. **النطاق:** قراءة فقط للمستودعات الخارجية؛ هذا الملف في Tank research فقط.

## المستودعات التي فُحصت فعلياً
| المستودع | المرجع المقروء | إثبات blob | الدور |
|---|---|---|---|
| `sohpany/SERVOC-Android` | `README.md` على `release/hazrob-0.10.2-rc2` | `243bec1e0aab6e3cbd7db7bb28ba4001385f7d04` | خط التطبيق/ESP32 الأصلي ومكتبة HR069 التشغيلية |
| `sohpany/SERVOC-Android` | `README.md` على `main` | `82d1a2c2a7db0be31ed18bb094fb76a7fe5797cf` | خط أقدم 0.8.0؛ لا يُعامل بوصفه RC2 |
| `sohpany/HazRob` | `README.md` على `main` | `c9161b3638d55c7a424182847c5106d6e8cc13c6` | وصف المشروع الشخصي، محتوى المحركات لم يُجرد |
| `sohpany/HazRob-Lab` | `README.md` على `main` | `f861c51ef267ee0e2637e5056db1d47bb399e4f7` | أرشيف مجمد منذ 2026-10-01؛ ممنوع التطوير أو النشر منه |
| `HazRob/HazRob-Tank` | `README.md` على `main` | `57d262c9c3c76b704a85d979d57a3977132db049` | روبوت مستقل؛ تقنيات ChemCore80 تحت research |
| `HazRob/HazRob-Tank` | `research/MASTER_PLAN.md` | `128830333f91e820f273549f67f47f66c1e28193` | تبعيات مراحل 01–10 والتقنيات الـ80 |
| `HazRob/build-harness` | `library/reference-bank/README.md` | `57962a961ed8d053abbe73317cf238eb6b38355a` | سلطة الفهرسة العلمية المشتركة |
| `HazRob/build-harness` | `library/reference-bank/INDEX.md` | `1e05fdd9aa4a4ccb01f18a1ddf046c12a15da9e4` | فهرس المراجع وأهلية مصادر MQ/MOS |
| `HazRob/build-harness` | `library/reference-bank/external-research/master_catalog.json` | `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` | 80 سجلًا مرجعيًا، **لا** 80 تجربة مستقلة |

## نتائج بنيوية مؤكدة
1. لا يجوز اعتماد `main` في SERVOC-Android بوصفه أحدث خط ChemCore؛ وثيقة فرع RC2 هي مرجع HR069 المعتمد هندسياً، وتصف إصداراً تشخيصياً غير معتمد ميدانياً.
2. مرجعية الببليوغرافيا المشتركة **وحيدة** في `HazRob/build-harness/library/reference-bank`؛ لا تُنسخ قاعدة رئيسية موازية إلى Tank.
3. بيانات وتجارب HR069 التشغيلية تبقى في `sohpany/SERVOC-Android:release/hazrob-0.10.2-rc2/research_data/hr069_analytical_library/`، مع منع تعديلها أو نقل معايراتها تلقائياً إلى Tank.
4. `HazRob-Lab` أرشيف تاريخي مجمد، لا بيئة إنتاج أو مرجع قبول.
5. `HazRob Tank` مستقل تماماً؛ تنقل الخوارزميات بعد مراجعة الرخص والعقود والاختبارات، وليس المعايرات والـppm.
6. 80 سجلًا في master_catalog تشمل 14 `EXTERNAL_MEASUREMENT_DATASET`، و4 `CROSS_PLATFORM_DATASET`، و24 `ANALYTICAL_METHOD_STUDY`، و8 `CALIBRATION_R0_RS_STUDY`، وغيرها. تعدد الأنواع يمنع استخدامها كلها بوصفها قياسات تدريب.
7. تسلسل ChemCore80 الرسمي: قياس 01 → معالجة 02 → تمييز 03 → تصنيف 04 وتقدير 05 مشروطان بالتوازي → دمج/لايقين 06؛ والتحقق 07 مستمر؛ الشبكات/المخاليط 08 بعد الأساسيات؛ الاستشعار الروبوتي 09 يحتاج مرجع حركة وتوقيت وتدفق.

## عقود التكامل المقترحة (قرار تصميم أولي لا تنفيذ)
- `MeasurementRecord`: device/sensor identity, ADC, supply, RL, heater phase, R0 provenance, timestamp, BME and session/source identifiers.
- `SignalQuality`: saturation, missingness, warmup, drift, sensor/ambient status, abstention reason.
- `FeatureSet`: static + temporal features with fitted-transform version and per-session lineage.
- `ModelEligibility`: device and gas domains, independent training/validation splits, calibration and source license.
- `CandidateEvidence`: gas hypothesis, score semantics, model and source hashes, correlated-evidence group, uncertainties.
- `Decision`: known/unknown/mixture/insufficient; ppm only with valid per-gas calibration; independent safety alarm.
- لا يمكن افتراض نقل نسب دقة UCI/MOS إلى MQ8 أو أن توافق عدة نماذج مستقلة إحصائياً.

## أعمال الجرد غير المنجزة
- استعراض أشجار الملفات والفروع المهمة لجميع المستودعات وتحديد محركات ChemCore Stable والـmodels والـschemas وأدوات المعايرة واحداً واحداً.
- جرد SQLite/JSON/CSV وأحجامها وإصداراتها وبصمات SHA وحقوق المصدر؛ فصل metadata عن raw measurements.
- بناء مصفوفة مقارنة واجهات Stable/HR069/Tank V4 وتقنيات 80 وتحديد الوظائف المكررة والناقصة.
- اختبار تجريبي مستقل على بيانات مكتبة المراجع الخام بعد إثبات صلاحية المصدر، مع grouped holdout.
- لا تغيير في G1/G2/G3 بسبب هذا التدقيق المعماري.

## قاعدة الاستئناف
تابع الجرد من هذا الملف؛ لا تكرر نتائج المرحلة 1 ولا تدّع اكتمال مسح لم يحدث. وثق لكل ملف: repository, branch, path, blob SHA, purpose, interface, data provenance, reuse decision.

## المرحلة 2 — جرد وظيفي مدعوم بنصوص الشيفرة/التوثيق

### مكونات HR069 المكتشفة من وثيقة RC2 ومراجعة التوافق
المصدر: `sohpany/SERVOC-Android:release/hazrob-0.10.2-rc2/README.md` blob `243bec1e0aab6e3cbd7db7bb28ba4001385f7d04`، و`docs/0.10.2/CR-20261008-006_DEV_REFERENCE_CHEMCORE_SYNC_AUDIT.md` blob `78d1063a7acc3fadbfd13e799abde6d663d3ce89`.

| الوحدة أو العقد | الوظيفة المؤكدة توثيقياً | قرار الاستفادة |
|---|---|---|
| `chemcore-features/1` | 17 خاصية لكل MQ × 8 = 136 خاصية؛ زمن الاستجابة، المساحة، الميل، الضجيج، SNR | **REUSE_DESIGN / VERIFY_SOURCE**: اعتماد تعريف الميزة والعينة ونسخها بعد تدقيق التنفيذ |
| ChemCore Stable `chemcore-stable-snapshot-v1` | قارئ `knowledge.response_priors` و`knowledge.model_artifacts`؛ فصل تحديث المكتبة عن APK | **ADAPTER_REQUIRED**: لا يُقرأ ملف تجارب الخام بوصفه Stable snapshot |
| ChemCore identity seed | 652 هوية؛ 615 سجل جزيئي و37 مادة/خليط/فئة غير محسومة جزيئياً | **REFERENCE_ONLY**: ليست 652 بصمة MQ مقاسة |
| AMCI | طلب تعريض إضافي أو تعافٍ أو تكرار أو تحقق مكاني عند عدم كفاية الأدلة | **REUSE_DESIGN**: قرار إعادة القياس منفصل عن ادعاء تحديد الغاز |
| `ManualR0Calibration` | قيم R0 المحلية القابلة للتعديل مع حفظ مصدر المعايرة؛ لا يستبدلها تحديث Stable | **ISOLATE_PER_DEVICE**: لا تنقل قيم R0 من الأصل إلى Tank |
| `ChemCoreSessionFeatureAdapter`, `SessionSeries`, `ChemCoreQuantitativeTimeSeries` | استعمال R0 المخزن وقت أخذ العينة لا قيمة R0 المعدلة لاحقاً | **REUSE_CONTRACT**: حفظ ثبات تفسير الجلسات التاريخية |
| `TelemetryRecorder`, `MeasurementSessionRegistry`, `ExperimentQueries.recentDeveloper`, `ReferenceLineageGuard.valid` | فصل USER/DEVELOPER والتحقق من سلسلة مصدر اللقطات المرجعية | **REVIEW_LINEAGE**: يمنع اعتماد قياسات المستخدم العادية كمراجع |
| `ReferenceBankExporter` | مخطط `hazrob-approved-experiment/3`؛ وثائق تاريخية تذكر `/1` | **SCHEMA_MIGRATION_REQUIRED** |
| `GitHubReferencePublisher` | هدف نشر قديم غير مطابق للسلطة التشغيلية؛ مسار staging جديد غير تشغيلي | **DO_NOT_REUSE_AS_PUBLISHER** |
| `ChemCoreLibraryManager` | HTTPS، حجم وSHA256، staging وrollback؛ manifest URL افتراضياً فارغ | **INTEGRATION_BLOCKED** حتى اكتمال بوابة القبول والنشر الموثق |
| `servoc.db` | قاعدة Android المحلية لا تُستبدل بتحديث Stable | **NO_DIRECT_COPY**: تحتاج جرد جداول/مخطط مستقل |

### الفجوات التحليلية المثبتة
- **فجوة النشر:** `hazrob-approved-experiment/3` لا يتحول آلياً إلى `chemcore-stable-snapshot-v1`. يلزم خط قبول علمي وتوحيد schema وبناء حزمة معتمدة.
- **فجوة الأدلة:** الهوية الكيميائية والخواص الجزيئية ليست بصمة استجابة MQ؛ يجب فصل identity catalogue عن measured fingerprint bank.
- **فجوة الذاكرة الزمنية:** أي إعادة حساب لجلسة قديمة يجب أن تستخدم R0 والظروف المخزنة في تلك الجلسة.
- **فجوة السلامة:** التقدير الكيميائي والتحذير الآني والسلامة التشغيلية ليست وظيفة واحدة؛ لا ينقل نجاح النموذج صلاحية ppm أو إنذار أمان.
- **فجوة فحص الملفات:** أسماء الوحدات السابقة موثقة في مراجعة المصدر، لكن لم تُفتح ملفات تنفيذها الأصلية واحداً واحداً في هذه المرحلة؛ لا تُسمى مراجعة شفرة كاملة.

### المصفوفة الأولية للدمج
| المكوّن | سلطة المصدر | اتجاه ChemCore Next |
|---|---|---|
| الببليوغرافيا والمراجع | `HazRob/build-harness/library/reference-bank` | وصل read-only بالمعرّفات والبصمات |
| نماذج Stable والأدلة التشغيلية | RC2 HR069 | تقييم وتوافق schema؛ **لا نقل تلقائي** |
| تقنيات Tank الـ80 | `HazRob-Tank/research` | orchestration research-only مع G1/G2 لكل تقنية |
| R0 والمعايرة | لكل جهاز وجلسة | per-device provenance، منع استيراد غير مؤهل |
| الاختبارات المرجعية | مجموعات بيانات خام مثبتة المصدر | split حسب الجلسة/الجهاز، metrics وablation، بدون ادعاء G3 |

**حالة المرحلة 2:** اكتمل جرد وظيفي **انتقائي موثق** لـ11 مكوّناً/عقداً في HR069؛ جرد أشجار الملفات والـSQLite وملفات النماذج الثنائية لم يكتمل. 
