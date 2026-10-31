# مصفوفة تتبع ChemCore Next — جرد مستودعات 2026-10-26

## منهج الجرد المنفذ
استخدمنا `GET /repos/{owner}/{repo}/git/trees/{ref}?recursive=1`، وجميع الاستجابات `truncated=false`. فُحصت خمسة مستودعات بإجمالي **1319 ملفاً**؛ طابق مرشح الاسم/المسار **782 ملفاً**. قائمة 782 path + blob SHA + bytes موجودة في [JSON](CHEMCORE_CROSS_REPO_FILE_INVENTORY_2026-10-26.json). المطابقة ليست حكمًا على الوظيفة أو السلامة أو جودة الشيفرة. SERVOC شجرة commit `6281d3eb7c1c961ce63b0b5e4ae0792874626720`، والبقية `main` بتاريخ القراءة. لم تُفحص كل الفروع التاريخية أو الملفات غير النصية بقراءة المحتوى.

| Repo | الملفات | المطابقة |
|---|---:|---:|
| sohpany/SERVOC-Android | 776 | 338 |
| sohpany/HazRob | 2 | 0 |
| sohpany/HazRob-Lab | 19 | 15 |
| HazRob/HazRob-Tank | 372 | 346 |
| HazRob/build-harness | 150 | 83 |

## مصفوفة تتبع تنفيذية — فُتح المصدر فعلياً لست وحدات
| وحدة HR069 الأصلية | Git blob | API/العقد الذي فُحص | نظير Tank/التقنيات | قرار الدمج |
|---|---|---|---|---|
| `ChemCoreRuntimeCoordinator.java` | `2998b70ab19656f8cbb8dfda08f8e4a3fa629dd6` | `begin`, `attachMeasurementSession`, `advance`, `abort`, `finish`, `observeMotion`, `snapshot`, `statusJson` | 09.4–09.6، إدارة الجلسات | REUSE_CONTRACT_ONLY: واجهة منسق جديد مستقلة |
| `ChemCoreInferenceEngine.java` | `d0aba24dc4a1448981adaf80c0263d33bc0d1e7f` | `analyze`, `analyzeLegacyManufacturerPriors`, `hasReferenceBackedMultisensorSupport` | 04.*,05.*,06.* | PORT_AFTER_TESTS: فصل ranking عن ppm |
| `ChemCoreEvidenceFusion.java` | `8faea685d03e7133d41a11ca1aadbf82f1d0c865` | `fuse`, `fuseForChemical`, `supportWeight`, `isM4`, `isReference` | 06.1,06.7,10.1 | REUSE_DESIGN: فحص الاعتماد بين الأدلة والـtiers |
| `ChemCoreFeaturePipeline.java` | `33cf66ab653df490c281c8f9ea20641285961447` | `extract(timeSeconds,ratios)`, `featureName` | 02.3–02.6 | PRIORITY_PARITY_TEST: 17×8 ونسخة schema |
| `ChemCoreOpenSetGate.java` | `b44e1d4055739d22843bc8992199ef6c8775e8cc` | `fit`, `fromValidatedThreshold`, `evaluate`, `normalizedEntropy` | 06.5–06.6,06.9 | RETRAIN_AND_CALIBRATE: منع نسخ threshold غير مؤهل |
| `library/ChemCoreLibraryManager.java` | `5e40b23ab3802c900e6166f3229d9ab474f6ebd3` | `check`, `install`, `rollback`, `activePayload` | library release adapter | REUSE_CONTRACT_ONLY: فصل المصدر والموافقة وتحديث Stable |

**مهم:** فحص API لا يساوي تحليل جميع أسطر التنفيذ أو تشغيل الاختبارات. هذه مصفوفة source-inspected للواجهات الست فقط، وليست شهادة تكافؤ.

## مجموعات الملفات المكتشفة في شجرة SERVOC
- `app/src/main/java/com/servoc/app/chemcore/`: وحدات inference، evidence، fingerprint، physics، QSPR، mixture، open-set، runtime، library.
- `app/src/main/java/com/servoc/app/analytics/`: `RidgeConcentrationModel`, `TemporalFeatureExtractor`, `AnalysisDecision`.
- `app/src/main/java/com/servoc/app/fingerprint/`: `FingerprintClassifier`, `FingerprintTrainer`, `ResearchIdentificationFingerprintRegistry`.
- `app/src/main/assets/generated/chemcore/`: `manifest.json`, `molecular_descriptors.json` (2,642,719 bytes), `unresolved_curation.json`; هذه descriptors لا تعني قياسات حساسات.
- `app/src/test/java/com/servoc/app/chemcore/`: اختبارات وحدة لمراحل fusion, feature, unknown, mixture, QSPR, library وغيرها.
- `research_data/hr069_analytical_library/`: يجب تفكيك ملفات هذا المجلد حسب الـschema والأهلية وليس نسخاً مباشراً إلى Tank.

## شجرة Tank
- `research/techniques/` يحتوي **80 معرف تقنية فريداً** و**270 ملفاً** من نوع README/Python/test وغيرها، على `main`.
- `research/benchmarks/` يحتوي adapters، trained models، نتائج UCI متعددة؛ يُربط كل benchmark بمعرف المصدر والـsplit والـSHA.
- لا يُفسر وجود ملف `test_*.py` بوصفه اختباراً ناجحاً على فرع V4؛ يجب إثبات نتائج CI على نفس blob.

## شجرة HazRob-Lab المجمدة
19 ملفاً، منها `calibration/priors/identification_fingerprint_priors_v1.json`, `mq_research_curves_43.json`, `datasets/external/derived/*`, `datasets/external/registry.json`, `models/mq76/catalog.json`، و7 وثائق بحثية. **ARCHIVE_ONLY**؛ لا تُرقى إلى بيانات معايرة Tank.

## شجرة build-harness
150 ملفاً، منها `library/reference-bank/`، `library/chemcore/stable/reference-evidence/snapshots/2026-10-21/`، فهارس citations وcurated وhr069-research وworkflows. التفريق بين فهرس أدلة ومحتوى runtime شرط إلزامي.

## التوصيات التنفيذية ذات الأولوية
1. إنشاء ملف manifest موحد لكل `MeasurementRecord`, `FeatureSet`, `EvidenceRecord`, `ModelArtifact`, `DecisionEnvelope` مع `source_repo`, `commit`, `blob_sha`, `schema_version`, `sensor_domain`, `validation_tier`.
2. اختبار parity لمخرجات `ChemCoreFeaturePipeline.extract` مقابل Tank `02.3–02.6` على بيانات اختبار متطابقة، مع توثيق الوحدات والفقد والـR0.
3. مقارنة منطق `ChemCoreEvidenceFusion` مع `06.1` و`10.1` ومنع مضاعفة أدلة المصدر الواحد.
4. تدقيق `OpenSetGate` مع `06.5/06.6/06.9` وإعادة تقدير العتبات على split مستقل؛ لا نقل مباشر للعتبات.
5. فحص مخطط قاعدة Android وStable reader وجرد الجداول والـmigrations من المصدر، دون افتراض أن كل `.json` تدريب.
6. منع نقل القيم الفعلية لـR0 أو ادعاءات ppm من الأصل إلى Tank؛ G3 صفر حتى قياسات Tank المعايرة.

## حدود الإثبات
تم جرد **أسماء وبصمات الملفات** على الأشجار الخمس بالكامل، وفحص **واجهات ست وحدات Java** من المصدر، وقراءة وثائق/سجلات تقدم متعددة. لم نفتح 1319 محتوى ملف أو نحلل 782 ملفاً تحليلاً سطرياً أو نختبر تكامل المحرك. هذا **جرد شامل للأشجار المحددة**، وليس تدقيقاً شاملاً للسلوك البرمجي والبيانات الثنائية.
