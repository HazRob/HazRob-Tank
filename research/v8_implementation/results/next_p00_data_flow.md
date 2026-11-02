# P00.3 — تدقيق ثابت لمسارات البيانات الفعلية في HazRob Tank V8
**التاريخ:** 2026-11-02 | **الفرع:** feature/tank-v8-implementation
**نوع التحقيق:** Static source tracing, not an Android runtime replay or hardware validation.

## 1. القياس الآني
1. TankInstantAnalyzer.java (blob c3fb91f9c7a34645aaa034399e4befadcffbc169) يلتقط snapshot/valid Rs ويمرر إلى TankV6TelemetryAdapter.analyzeSnapshot(frozen) حول السطر 64.
2. TankV6TelemetryAdapter.java (blob cccd8cac3e78dca3cd3fee55dadd64381442fe4e) ينشئ Engine.Channel وبيئة T/RH ثم يستدعي ChemCoreTankV6.analyze عند السطر 63. المحول يستدعي CleanAirResponseGateV6.assess حول السطر 188/261. شرط MIN_DISTINCT_SENSOR_MODELS=2 وهامش drop 10% provisional؛ عند عدم تجاوزه يصفّر فرضيات source_curve_hypotheses لأغراض العرض، لكنه يحتفظ بالدفتر البحثي الخام source_curve_research_diagnostic_ledger.
3. ChemCoreTankV6.java (blob 41bf19235fc2d50298f7112cbbdd08898b900a4e) يرسل input إلى Engine.analyze وFingerprintEngineV6.match وEvidenceConflictV6.compare؛ كل مسار مصدر مستقل نظريًا ولكنه لا يعني G2/G3.
4. Engine.java (blob 4c63995cbdd8c214e922bed91496740f053742c9) ينتج rankedCandidates بترتيب اتساع الحساسات ثم residual، لا posterior احتمال معاير.
5. **الخلل:** TankInstantAnalyzer يأخذ source_curve_hypotheses لا rankedCandidates، وينشئ distinct LinkedHashMap بترتيب المصدر، ويحل المواد بشرط legacyId>=0&&legacyId<144؛ يحد ذلك نطاق العرض ولا يعكس ترتيب الخبراء النهائي. ليست هذه إثباتًا أن 734 مادة تمتلك جميعها أدلة MQ.
6. تقرير الآني: modules .../report/TankInstantReportPdf.java, TankInstantV7PdfWriter.java, V7InstantVisualAnnexWriter.java وغيرها. ينبغي التحقق لاحقًا من تطابق جميع الرسوم مع result_id وليس افتراضه.

## 2. الجلسات والقياسات الزمنية
1. FieldCycleController.java (blob d354c139810b83bea3e86e869ba16b2eb11bd90e) يتحقق من الجهاز والريلاي عند بدء cycle، ثم يتعامل مع 60s warmup + 60s recording ومسار اكتمال repo.complete ومن ثم mqPower(false). يوجد timeout بعد طلب الإنهاء، وتوجد أخطاء محفوظة في failWorker؛ نجاح repo.complete/readback لم يختبر هنا.
2. CompletedSessionChemCoreAnalyzer.java (blob 96cf770c813c3d2adc42af10e8fc2f97752d834c) يستخرج عينات telemetry_samples الفعلية وscan_cycle الفريد، ويستخدم AcceptedR0Normalizer.recordedRatio؛ عند الجلسة الحقلية لا يعود إلى لقطة طرفية واحدة. **هذا الكلاس يستدعي ChemCoreInferenceEngine.analyze التاريخي عند السطر 88**، وليس ChemCoreTankV6 عبر same result envelope. لديه call إلى ChemCoreDecisionRepository.save.
3. ChemCoreDecisionRepository.java (blob e3101c0a80f1d0a06748308d9a7a7e16a8412a0b) يسجل chemcore_decisions وحقول النموذج؛ يحتوي طرقًا تلحق معلومات لاحقة بـpayload. يلزم Next immutable result separate from later review/series append.
4. أصول التقارير الحقلية والمواقع تشمل FieldSessionReportExporter.java وV7FieldSessionPdfWriter.java وSmartFieldSessionXlsxWriter.java وSiteSummaryReportExporter.java؛ يجب اختبار parity فعلي مع V8.

## 3. قاعدة المواد وStable
- ChemicalRepository.java (blob c52813a7918a8a18eb824de8045202902020c2d1) يوفر count/search/byUid/byLegacyId من v14_unified_chemical_catalog، ويصف 144 legacy كعلم مصدر/أولوية وليس قائمة أخرى.
- ExperimentsDb.java (blob 2cd85975fbf188cb249b5484349521c7ea2dec03) ينشئ chemicals وscientific_sources وsensor_evidence ومخازن raw telemetry والجلسات، ويعرف view v14؛ العدد الفعلي يعتمد على seed/importer وتحديث الحالة على الجهاز.
- ChemicalSeedBootstrap.java (blob 5ffae18de4949af689b7ad775de552f6080db87d) يستورد generated/niosh/niosh_chemicals.json ويستدعي ChemCoreEmbeddedStableInstaller. عدد الصفوف النهائي وحالة النسخة المعتمدة يحتاجان اختبارًا منفصلًا P00.4.
- ChemCoreLibraryManager.java (blob c7e70d88312f39d30272125aa73158f397d2aff1) يدير Stable مضمّنة/remote وmanifest، وChemCoreLibraryUpdateClient.java (blob 5f13d8a109bb9816a5e18aa1fd13a267614510a6) يتضمن staged payload transfer؛ بحاجة لمراجعة توافقها مع طلب المستخدم «تحديث المحتوى عبر الاستعلام» في P11، وعدم الخلط بينها وبين تنزيل APK.

## 4. الأخطاء الجذرية المشخصة والتنفيذ المخطط
- F-001: Single valid specialist response masked by minimum 2 response models => Next P02.4/P05.2.
- F-002: Source order instead of actual adjudicated ranking => P05.6/P08.2.
- F-003: Legacy 144 display lookup => P03.4/P05.1/P08.2.
- F-004: Instant and field analysis use two different engines => P07.4/P09.3.
- F-005: R0 definition and source K not necessarily coherent => P02.2/P10.2.
- F-006: Approved Stable runtime and canonical DB count not confirmed => P00.4.
- F-007: Relay completion after SQLite capture not tested end-to-end => P09.1.
- F-008: Traceability between UI/rendered PDFs and scientific results untested => P12.1.

## 5. نقاط الاستئناف / حدود الإثبات
Static source-to-source call edges were verified for entrypoints but this does not establish that all screens or techniques execute the same path at runtime. Android instrumented tests for source/data/SQLite/UI to be carried out P06+P07–P12. This is sufficient to freeze a **static wiring gap report**, not to claim any runtime G1/G2/G3 pass or APK.
**المهمة التالية بعد توثيق هذا الفحص:** P00.4 catalog count/Stable manifest; بعدها تبدأ P01 نواة Next.
