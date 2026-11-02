# HEDAF — المنهجية العلمية الحاكمة لتطوير ChemCore Next
**المشروع:** HazRob Tank V8 — ChemCore Next
**التاريخ:** 2026-11-02 | **الإصدار الوثائقي:** 1.0-proposed | **الحالة:** PROPOSED_FOR_OWNER_APPROVAL — لا تُفسَّر كتابة الوثيقة بأنها اعتماد المستخدم النهائي.
**الفرع:** feature/tank-v8-implementation
**الأصل البرمجي المحفوظ:** ChemCore V6 مستقل؛ V5/V7 مراجع تاريخية فقط، ولا تُكتب عليها تغييرات.
**الموجهات:** الأفكار S-01 إلى S-20 في docs/research/CHEMCORE-NEXT-SCIENTIFIC-DISCUSSION-REGISTER-2026-11-02_AR.md.

## 1. الهدف والنطاق
HazRob Evidence-Driven Analytical Development Framework (**HEDAF**) إطار منهجي مقترح، **ليس معيارًا دوليًا قائمًا**. يفصل خمسة أهداف قابلة للقياس:
D1 كشف تغير الاستجابة عن مرجع الهواء، D2 ترشيح هوية مركب أو عائلة، D3 التمييز بين الغاز المفرد والخليط وUNKNOWN، D4 تركيز مرجعي ppm عندما يؤهل المرجع، D5 تقييم سلامة مستقل **بشروط صلاحية إضافية**. لا يسمح نجاح D1 بالادعاء بنجاح D2/D4/D5.

المنهج يشمل 20 مقترحًا للمالك، هندسة حساسات MQ الثمانية الحالية MQ2/3/4/5/7/8/9/135، والتوسع اللاحق إلى أجهزة انتقائية جديدة، وكل أنماط القياس (آني، جلسات، تتبع، تحليل عام، مختبر، تقارير، مساهمات). **نضارة الأغذية تبقى وحدة منفصلة ولا تخلط استدلالها بمكتبة الغازات.**

## 2. المبادئ غير القابلة للتفاوض
P01 **أصالة القياس:** raw ADC/voltage/resistance/time/scan_cycle/device_id محفوظة مع الوحدات ومصدرها. لا تتولد قراءات اصطناعية لحساس غير مركب أو تعاد عينات HTTP المتكررة كقياسات مستقلة.
P02 **تعريف R0 بلا لبس:** R_air=reference Rs in observed air، وR0_source=R_air/K_source عندما يكون تعريف K/source/heater متوافقًا. q_air=Rs/R_air وq_source=Rs/R0_source؛ لا تحويل لقيم R0 القديمة بلا توثيق.
P03 **جودة القياس قبل الاستدلال:** أسلاك/ADC/RL/Vc/relay/warmup/heater phase/saturation/staleness/timestamp/T/RH/time valid. توضع حالات INVALID/UNKNOWN لا zero أو ppm مختلق.
P04 **خصوصية الحساس مشروطة:** الحساسية والانتقائية وحد الكشف واختبارات التداخل لكل sensor-model/physical-instance/chemical/condition؛ استجابة متخصصة واحدة دليل يستحق التسجيل حتى دون استجابة اثنين، لكنها لا تثبت الهوية منفردة.
P05 **الدليل السلبي مشروط:** غياب استجابة يضعف المرشح فقط إذا كان expected positive under candidate concentration، والجهاز صحيح، وLOD وresponse window وبيئة التشغيل معروفة؛ عدم توفر نموذج استجابة لا يعني نفي الغاز.
P06 **الاستقلال الإحصائي:** عدة خوارزميات تستعمل نفس مصدر datasheet أو نفس القراءة الفيزيائية ليست أدلة مستقلة، ويُسجل independenceRootId.
P07 **تعدد الخبراء لا دمج أعمى:** curves, lab fingerprints, environmental 3D, temporal 4D, statistical/ML research operate separately and return interpretable claims; arbitration based on compatibility and independent support, not naive average or majority.
P08 **فرضيات مفتوحة:** UNKNOWN, OUT_OF_DOMAIN, INSUFFICIENT_EVIDENCE, MIXTURE_UNRESOLVED, CONTRADICTORY_EVIDENCE؛ لا ملء إجباري لقائمة المرشحين ولا اختراع ثقة.
P09 **نموذج أمامي وعكسي:** consistency check if a hypothesized gas/mixture at concentration can explain observed vector/trajectory; mixture response need empirical reference, do not presume sum of nonlinear sensitivities.
P10 **سياق الموقع وتغذية المستخدم منفصلان:** operator knowledge prior/constraint (not ground truth); corrections and negative labels retain immutable original evidence; comments never silence independent qualified safety alert.
P11 **حدود طبيعية مقابل تنظيمية:** background_air_range, detection_limit, quantification_limit, occupational OEL (TWA/STEL/ceiling), flammability LEL/UEL, hazard GHS each separate with units, time base, jurisdiction, provenance. Unavailable ≠ zero, and no unsupported “normal level”.
P12 **التعلم المركزي بإجراءات مراجعة:** no online self-training from raw operator feedback; quarantine→triage→replicate→train offline→holdout→G1/G2/G3 qualification→versioned stable promotion/rollback.
P13 **توسعية منظومة الأجهزة:** dynamic capability registry keyed immutable physical sensor ID and model; allow missing/new/degraded sensors; support O2/PM/other modules as separately scoped analytes.
P14 **الاستنتاج لا يساوي الإشارة إلى الخطر:** research concentration, identity prediction and certified safety performance separated in schema and UI; no occupational exposure assurance before measuring appropriate duration/reference.
P15 **التوثيق والاستعادة:** every phase commits source manifest, tests, unsuccessful cases, decisions, run IDs, artifacts and an incremental tracker; no undocumented claim of successful Android APK or hardware G3.

## 3. الكيانات والواجهات المرجعية — تصميم مبدئي
- SensorInstance: sensor_uid, model, board_revision, physical_location, circuit, Vc, RL, ADC_channel, heater_profile, burn_in_record, maintenance, calibration_id, capabilities[].
- MeasurementSnapshot (immutable): snapshot_id, session_id, device_id, timestamps_utc_monotonic, scan_cycle, mode, sampling_period, raw_adc, measured_volts, Rs_kohm, R_air_kohm, R0_source_kohm, r0_definition, T_C, RH_pct, heater_state, valid/fault codes, gas_relay_state, source_hash.
- EvidenceAsset: source_url/DOI/tag/blob/license, instrument model/conditions, chemical_uid, equation/curve/fingerprint/ML model, units, ratio_direction, applicable_range, reference_level, source_independence_root, maturity.
- EvidenceNode: hypothesis_id/canonical_chemical_uid or family, observed_sensor_instances, sign positive/negative/ambiguous, source_root, raw_sample_ref, expected_response, residual, applicability, caveats, quality_tier.
- CandidateEvaluation: canonical uid + CAS identity, direct sensor support, independent source support, disagreements, forward-residual, conditions, rank_basis, qualification, UNKNOWN_reasons, *not* invented posterior.
- ChemCoreNextResult: schema/version/model_hash/stable_manifest_hash/inputs_hash, status, all_evidence, ranked_eligible_candidates[], excluded_reasons[], preserved_unqualified_source_hypotheses[], raw_data_ref, operator_review_ref.
- UserReview: operator_id/pseudonym, site context claims, material inventory confidence and timestamp, selected candidate dispute, explanation/evidence attachments, actual reference device if any, consent, immutable revision history.
- StableRelease: parent_version, asset manifests, train/val/test groups and hashes, gates G1/G2/G3, qualified purposes, compatibility, signed checksum, rollback target.

## 4. تسلسل التحليل الهرمي
S0 Capture + quality check → S1 device-specific baseline and environmental context → S2 single-sensor evidence independently incl conditional negative → S3 parallel analytical experts (source curves/log inversion, experimental fingerprints, robust statistics, T/RH compensation 3D, temporal trajectories 4D, ML variants) → S4 canonical chemical hypotheses from **all eligible unified master UIDs** → S5 evidence correlation graph and calibrated/transparent arbitration → S6 forward reconstruction, open-set and mixture challenge → S7 context annotation + operator review **without altering scientific raw outcome** → S8 immutable decision → S9 source-aligned UI, PDF, XLSX, site summaries and central feedback. 
The sensor quality gate, inference claim gate and independent safety gate are separate layers.

**Mathematical skeleton:** manufacturer curve when applicable q_source=a(C/scale)^b; convert only with validated R0 definition. For forward model \hat{r}=F(g,c,T,RH,t,heater,device) and residual \mathcal{L}=d(r,\hat{r}), choose candidates through gated model comparison; no claim of unique inverse solution for non-selective MQ arrays. Missing expected signal contributes **weighted negative evidence only if reliable sensitivity/LOD and phase are known**. A chemical missing reference models is “NOT_EVALUABLE” not “ABSENT”.

## 5. نظام الترشيح وعداد النتائج
- Universe: all active canonical master chemicals (the current reported ~734 is a target for DB check, NOT experimentally confirmed count here).
- Candidate eligibility: model support + valid observed MQ signal + source applicability + independent evidence sufficient for *research hypothesis*. Classification distinct: signal change, tentative candidate, validated identity.
- Drop items with no evidence, impossible UID mapping, invalid sensors; preserve evidence in separate research/audit ledger.
- Display count **min(qualified_count, operator_max_N)**; no minimum N=10, 0 is a valid output with UNKNOWN. >N results remain inspectable as research data when eligible.
- Both positive single-sensor findings and qualified conditional nonresponses are shown with proper status. Multi-output rank must not be built from first occurrence in source_curve_hypotheses; use adjudicated rankedCandidates linked to master UID, not legacy 0..143.
- No confidence percentage without probability calibration and held-out reliability checks.

## 6. المنهجية البحثية للأبحاث والبيانات
Use PRISMA 2020 **as a reporting guide** for systematic/scoping evidence mapping: reproducible queries with databases, dates, included/excluded counts, extraction schema, bias/quality and traceability. Exhaustive capture of every MQ publication cannot be claimed; use continuously updated coverage register. Priority: manufacturer original datasheets, experimental papers with raw observations and known reference gas, independent device datasets, trustworthy repos, then simulated/digitized curves and worked examples. Record provenance separately; a datasheet converted through two source libraries is one source root, not two independent experiments.

## 7. التأهيل العلمي والبوابات
- E0 Source documented, license/provenance/units/applicability only.
- G1 Numerical/code: deterministic unit/metamorphic/source parity tests and error handling; acceptance all mandatory correctness cases pass, every failure tracked; exact tolerances predeclared per numerical method.
- G2 External model: real reference labels, session/exposure/device/day/batch grouped holdouts, time-forward split as necessary, immutable final test, no tuning after test. Metrics per task: change detection FPR/FNR, identity top-1/top-3/F1/open-set, calibration/ECE where supported, ppm bias/MAE/RMSE/uncertainty with known reference; confidence intervals grouped by exposure. Compare to V6 and simple baselines; preregister acceptance by application risk, use abstention rather than invented 95% accuracy.
- G3 Tank physical: design-of-experiments with blank + reference gases and concentration levels including claimed range and near LOD, independently repeated exposures/day/device/batch as feasible, separate calibration and blind samples, certified traceable calibration composition/reference analyzer, valid heater state and environment, mixtures/interferences, recovery and drift. Sample size/power based on expected variation; **initial planning minimum, not normative sufficiency**: >=3 independent exposure runs per test condition, >=3 nonzero concentration levels per analyte plus zero, >=3 days when feasible; these do not replace uncertainty and power analysis. No dangerous/flammable gas trial outside controlled suitable facilities.
- S Safety-application gate after G3: safety use-case and alarm range, applicable full IEC performance testing, independent reference, test of failure, alarm latency and false negative under known hazard; insufficient evidence → RESEARCH_ONLY; not a certified protective gas detector.

## 8. Standards and references (official/current as checked 2026-11-02)
- PRISMA 2020 https://www.prisma-statement.org/prisma-2020 (review reporting, not sensor certification).
- ISO/IEC 17025:2017 https://www.iso.org/standard/66912.html (competence of testing/calibration labs; not claiming accredited status).
- ISO 6145-1:2019 https://www.iso.org/standard/53598.html (dynamic reference gas preparation and metrological traceability).
- NIOSH 2012-162 https://www.cdc.gov/niosh/docs/2012-162/default.html (response, range, interferences, precision/uncertainty in direct-reading monitors).
- OSHA SHIB 11-26-2024 https://www.osha.gov/publications/shib093013 (portable monitor checks and calibration).
- IEC 60079-29-0:2025 https://webstore.iec.ch/en/publication/77173 (CURRENT general flammable, oxygen and toxic gas detector test requirements; replaces IEC 60079-29-1:2016 and specified prior standards; distinction FL/O2/TX scope).
- IEC 62990-1:2019 https://webstore.iec.ch/en/publication/28186 (toxic occupational HM context; safety monitoring SM superseded in relevant part by IEC 60079-29-0:2025; check version and scope).
- NIST AI RMF https://www.nist.gov/itl/ai-risk-management-framework (governance, validation, risk).
- Source repositories MQDataScience https://github.com/abcdaaaaaaaaa/MQDataScience ; MQSensorsLib https://github.com/miguel5612/MQSensorsLib and documentation https://github.com/miguel5612/MQSensorsLib_Docs .
- Relevant code snapshots: android/app/src/main/java/com/servoc/app/chemcore/TankInstantAnalyzer.java source blob c3fb91f9c7a34645aaa034399e4befadcffbc169; com.hazrob.tank.chemcore.v6.Engine; ChemCoreTankV6.

## 9. قبول المنهجية
Accept only when owner confirms HEDAF, detailed technique program and build plan; after confirmation freeze as v1.0 approved and assign ADR/decision IDs. Until then: PROPOSED, NO RUNTIME MODIFICATION, G3=0/80, NO APK. Any relaxation of safety evidence requires explicit new risk decision and cannot override applicable external regulation.
