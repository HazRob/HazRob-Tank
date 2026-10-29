# خطة التطوير العلمية والهندسية الحاكمة — ChemCore Tank V6

**المعرف:** HZT-V6-CHEMCORE-MP-001 · **الإصدار:** 1.0 · **التاريخ:** 2026-10-29 (Asia/Aden)
**المستودع:** `HazRob/HazRob-Tank` · **فرع التطوير:** `feature/tank-v6-implementation`
**الإطار:** V6-002 · **الحالة:** `DESIGN_BASELINE_DOCUMENTED / NO_CHEMCORE_IMPLEMENTATION / NO_V6_APK`
**المرجع الأساسي للتطبيق:** `feature/tank-v5-implementation@c132fc2f8eb7a54d5b3c96b4fdcd6090096d1a5b`.
**المحرك القديم المستعار أثناء البناء:** `sohpany/SERVOC-Android@6281d3eb7c1c961ce63b0b5e4ae0792874626720` للقراءة/الاقتباس الموثق فقط؛ ممنوع تعديل HazRob الأصلي.
**مراجع البحث:** `HazRob/HazRob-Tank:main/research/PROGRESS_80.json` blob `0919307d46b352a887f2fdbc5e94c752a74b16d0`؛ و`HazRob/build-harness:main/library/reference-bank/external-research/master_catalog.json` blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f`. يجب تحديث pin إذا استُخدمت نسخة لاحقة، لا نقل البيانات خلسة.

> **قرار الخطة:** بناء محرك استدلال بحثي قابل للاختبار والدمج في تطبيق Android باستخدام كل الموارد ذات الصلة؛ ليس اشتراطًا لتنفيذ كل 80 تقنية أو تشغيل كل 76 نموذجًا داخل الهاتف. تظل المعايرة الخاصة بالجهاز والأدلة الفيزيائية والتحقق العلمي شروطًا مستقلة، وG3 الفعلي موثق حاليًا بـ0/80. أي أرقام أداء مستقبلية تحتاج تجارب لا تنشأ من هذه الخطة.

## 1) متطلبات النظام وعدم القبول بالتراجع

1. **F01:** التشغيل دون إنترنت في الميدان مع Stable مثبتة محليًا ومتحقق من SHA-256، ولا حاجة لتطابق رقم إصدار Android والفيرموير؛ توافق الأوامر يعتمد على قدرات/حقول البروتوكول الفعلية فقط.
2. **F02:** دعم الحساسات الثمانية `MQ2,MQ3,MQ4,MQ5,MQ7,MQ8,MQ9,MQ135` بخريطة عتاد فعلية، وعدم افتراض صلاحية كل قناة أو بروتوكول تسخينها.
3. **F03:** تمييز `raw ADC -> V_RL -> Rs [ohm] -> R0 [ohm] -> Rs/R0 [dimensionless]`، والتحقق من `RL,Vc,ADC PGA,divider,heater` لكل قناة. R0 لكل sensor_instance ومعرّف معايرة وتعريف مرجع.
4. **F04:** عدم استبدال معايرة R0 بتاريخية، أو إعادة تحليل جلسة معتمدة بمرجع لاحق دون نسخة تحليل جديدة صريحة.
5. **F05:** بروتوكول جلسة موحد (baseline، exposure، recovery) مع تسلسل العينات والجودة وتوقيتها؛ القياس الآني والجلسات والمختبر يستعملون نفس `Engine API` لكن سياسات تحفظ مختلفة.
6. **F06:** مصدر منحنى/بصمة مستقل وموثق لكل حساس/غاز، وتحويل رياضي صحيح للوحدات ومعاملات المنحنى، دون خلط `alcohol class` بالإيثانول الجزيئي أو `liquid v/v` بغاز `ppm`.
7. **F07:** إنتاج فرضيات مرشحين لا هوية حتمية؛ رفض/امتناع عندما تفشل الأدلة، ومستويات منفصلة `UNKNOWN, SCREENING, IDENTIFIED_RESEARCH, MIXTURE_SUSPECTED, QUANTIFIED_M4_ONLY`.
8. **F08:** تركيز `screening_fit_ppm` اختياري وبوسم بحثي، أما `reportable_ppm` فلا يُملأ إلا بعد إثبات M4 مستقل لنفس الجهاز والغاز والبيئة ومجال التركيز.
9. **F09:** منع تضخيم الأدلة الناتج عن تعدد منحنيات الحساس نفسه، أو إعادة استخدام مصدر منحنى/بيانات التدريب داخل قنوات «مستقلة»؛ يجب احتساب المجموعات/التبعية.
10. **F10:** تشغيل Baseline V5 المرجعي جنبًا إلى جنب عبر نفس القياسات قبل السماح باستبداله، وعدم تدهور الجلسات والتقارير وSQLite وتوقيع التطبيق والأوامر والتحكم وإيقاف الطوارئ.
11. **F11:** واجهة حالة لكل نموذج من 76 و80: مصدر، جاهزية حوسبية، أهلية بيانات، تأهيل الجهاز، هل يعمل على Android أم المختبر؛ لا تغيير `Pending` شكلي.
12. **F12:** مخرجات قابلة للتفسير: قائمة المرشحين، مصدر/مساهمة كل قناة، نسبة تغطية، بواقي الملاءمة، أسباب الامتناع، مصدر المعايرة، حالة heater، رقم مكتبة Stable، نوع ppm، زمن الاستجابة.
13. **F13:** لا اعتماد قرارات أمنية/وقائية تلقائية من هوية/PPM بحثي، ولا ربط بالتحكم في حركة الروبوت دون تصميم سلامة مستقل.
14. **F14:** الاحتفاظ بتقارير PDF/Excel والمقارنات بنفس بيانات النسب وسياق البيانات الخام، مع اختبار رجعي لمصفوفات مواقع/جلسات V5.

## 2) تحليل النظام الحالي AS-IS

| المستوى الحالي | الدليل في المصدر | القرار |
|---|---|---|
| القراءة/المعايرة | `firmware/ESP32-Tank/Hardware.cpp`: ADS1115 وقيم RL/Vc ثابتة؛ `AcceptedR0Normalizer` | المحافظة على السياسة المرجعية وفحص العتاد فعليًا قبل أي تصحيح كهربائي |
| علاقات المصدر | `AcceptedReferenceResponseModel`, `TankStableScreeningModelRegistry`, `TankMultiSourceResponseModel` | لا حذف لمصادر مثبتة؛ افصل المصدر، المجال، المؤشرات ومعنى R0 |
| الملاءمة | `InverseMolecularSolver` بحث لوغاريتمي 181 نقطة ثم تحسين موضعي | استخدامه خط أساس B0، اختبار استقرار الحل وتفاوت المصادر وعدد القنوات |
| الاستدلال | `ChemCoreInferenceEngine`, `ChemCoreOpenSetGate`, `MixtureResidualGuard`, `AcceptedFingerprintIdentityEngine` من مرجع build مثبت | إثبات ربط التنفيذ الفعلي بتلك المصادر عند البناء؛ منع احتمال حضور زائف |
| Stable | 652 هوية، 49 prior response curves، نماذج معايرة Tank معتمدة 0 في السجل | تمييز فهرس الهوية عن دليل الاستجابة؛ تحقق byte/manifest للـ APK |
| النماذج الخارجية | MQDataScience 76: 14 Android native، 12 verified وفق سجل المصدر و2 approximation و62 Android port pending | حوسبة المختبر ليست تنفيذ Android؛ راجع 76 منفردة |
| تقنيات Tank | 80 research/techniques: ملفات README/code/tests؛ G1 تاريخي 1261 اختبارًا؛ G2: 49 عمليات خارجية،29 محجوبًا،2 تحليليًا فقط | سجل الدمج لكل ID؛ لا اعتبار G1/G2 صلاحية ميدانية |
| البيانات | 80 مرجعًا مركزيًا، لكن بعض الأرشيفات لم تُفتح خامًا ومجموعات legacy غير مفهرسة فرديًا | جرد فريد DOI/SHA/نوع المصدر وتحويل مشروط إلى مواد تدريب |

**مواطن القصور P0:** اختلاف تعريف R0 مع منحنى المصدر، RL والجهد الفعلي، سخان MQ7/MQ9 المجهول الهوية، مصادر `alcohol/CO2` الضعيفة، اختيار منحنى يتبدل عبر تركيز، الخوارزميات المستقلة المترابطة، `posterior` النسبي غير المُعاير، غياب بيانات مخاليط Tank والاختبارات G3. تقرير التفاصيل: `docs/research/V6-002-CHEMCORE-MANUFACTURER-R0-PPM-FINGERPRINT-GAP-AUDIT-2026-10-29.md`.

## 3) المعمارية المستقبلية TO-BE (Modular, versioned)

```text
ESP32 Tank / session replays / research adapters (separate provenance)
  └─ AcquisitionContract [sample/raw/ADC, hw-instance, time, heater, T/RH/P]
      └─ Electrical & R0 Validator [unit/rail/phase/RL/R0 definition]
          └─ Signal Quality + Exposure Windowing [baseline/exposure/recovery]
              └─ Feature Store [log-ratio, time, slope, T10/T90, recovery, mask]
                  ├─ Source Curves (factory + literature + Stable + physical)
                  ├─ Fingerprint & Similarity (reference coverage)
                  ├─ Classical Chemometrics / trained ML (if qualified)
                  ├─ Temporal / Environmental Models (if qualified)
                  └─ Unknown / Mixture / Interference Diagnostics
                       └─ EvidenceGraph + Independence-aware Fusion
                           └─ Uncertainty / OOD / Abstention / M4 Gate
                              ├─ Ranked candidates + reasons / NO forced identity
                              ├─ Research-only concentration range
                              └─ M4 when and only when separately certified evidence exists
                                    ├─ Android instant / field sessions / lab
                                    ├─ Immutable run & model manifests
                                    └─ PDF/XLSX + SQLite history
```

**مكونات البرمجية المقترحة (أسماء تصميمية، ليست ملفات موجودة):**
- `MeasurementFrameValidator`, `SensorInstanceCalibration`, `SourceCurveContract`, `ModelRegistryV6`;
- `SignalWindowFeatureExtractor`, `CandidateHypothesisEngine`, `EvidenceGraph`, `IndependenceAwareFusion`;
- `OODAbstentionPolicy`, `ConcentrationEvidenceGate`, `ChemCoreV6Facade`, `ResearchReplayHarness`.
واجهة Java صافية حيث يلزم داخل Android، وPython للنمذجة المختبرية *فقط*، مع تبادل `model.json` و`feature_schema.json` و`coefficients.bin` الموقع تحققُه محليًا أو في الحزمة، دون إضافة متطلبات Python/SciPy لخدمة Android.

## 4) عقد البيانات وأنواع الأدلة

**عقد `tank-chemcore-frame/1`**: `sample_id,acquisition_id,device_id,session_id,exposure_id,time_utc,monotonic_ms,sensor_instance_id,model,ads_address,channel,raw_adc,adc_pga_v,adc_reference_v,divider_gain,rl_ohm,vc_v,heater_voltage_v,heater_phase,rs_ohm,r0_ohm,r0_calibration_id,r0_definition_id,rs_r0,temperature_c,rh_pct,pressure_hpa,mq_relay_state,sample_qc`. عدم وجود معلومة لا يُعوّض بثابت افتراضي عند ادعاء صلاحية كمية.

**عقد `curve-evidence/1`**: `source_id,source_uri,doi,source_sha,license,manufacturer,model_variant,sensor_instance_scope,chemical_uid,CAS,chemical_label_semantics,fit_equation,parameter_units,r0_definition_id,heater_profile,temp_rh_range,ppm_range,uncertainty_origin,evidence_tier,independence_group,allowed_outputs`.

**عقد `inference-result/1`**: `analysis_id,input_sha,engine_sha,model_bundle_sha,stable_sha,measurement_profile,feature_schema,candidates[],source_residuals[],evidence_groups[],unknown_reason,mixture_state,research_ppm,reportable_ppm,evidence_tier,calibration_scope,uncertainty_kind,processing_ms`. **التسمية `posterior` في V5 relative ranking**؛ لا يجوز عرضها كاحتمال إحصائي معاير إلا عند وجود `probability_calibration_id` وبيانات validation مستقلة.

**التاريخ:** SQLite migration لا يعيد كتابة سجلات التجارب والجلسات القديمة؛ يبني نتيجة تحليل V6 جديدة مرتبطة بـ`parent_analysis_id`. الأثر الرجعي والإلغاء والـ rollback إلزامي. كل snapshot Stable برقم وSHA، ولا تحديث أثناء القياس.

## 5) رياضيات القياس والملاءمة والتمييز

1. `Rs=RL*(Vc/Vrl-1)` فقط بعد التحقق من كون `Vrl` هو جهد الحمل الفعلي لا الجهد بعد مقسم خارجي أو خرج محوّل غير معاير.
2. `r=Rs/R0` بلا وحدة. قد يكون `R0=Rs_air/k_air` عندما يطابق هذا تعريف المصدر، وقد يكون عند غاز وتركيز مرجعي؛ **ممنوع الخلط**.
3. `log10(r)=intercept+slope*log10(C_ppm)` أو `C=a*r^b`. تربط المخارج بنوع الغاز ومجال التركيز وتعريف R0 نفسه. لا استقراء خارج المجال المشروط.
4. لكل فرضية (g) احسب `c_hat_g=argmin_c Σ_i w_i ρ((log10(r_i)-f_i(g,c,T,RH,phase))/σ_i)`، بحيث لا يسمح `Σ` بإضافة الحساس نفسه عدة مرات باعتبار المنحنيات مصادر مستقلة؛ `ρ` robust loss يختار لاحقًا بعد الاختبار.
5. البصمة `log(Rs_exposure/Rs_baseline)` مع mask للقنوات الغائبة؛ زمن الاستجابة وشكله ودورة السخان جزء من البصمة متى توفرت بيانات مؤهلة. عدم تناظر مجالات التركيز بين غازين لا يعني هوية ثابتة.
6. OOD وUNKNOWN منفصلان عن `MIXTURE_SUSPECTED`؛ درجة عدم الاتساق لا تُثبت مكوّنات الخليط. لا تجميع خطي اعتباطي للمقاومات أو ppm من منحنيات الغازات النقية.
7. دمج الأدلة عبر provenance graph ومجموعة مستقلة (sensor instance, curve source family, original exposure, training source, correlation); قارن `single-sensor`, `2–3 sensors`, `full array` مع استبعاد/تدهور القنوات.
8. تقديرات ppm بحثية بشرط الصلاحية فقط؛ `M4` لا ينتج بسبب انخفاض residual أو اتفاق نماذج عديدة، بل يتطلب مرجع جهاز/غاز/تركيز حقيقي ومخرجات تحقق G3 مستقلة.

## 6) برنامج الاستخدام الأمثل للـ80 تقنية والـ76 نموذجًا

أ. **المسار الأساس الإلزامي:** 01.x (القياس والمعايرة)، 02.1–02.8 (تنظيف وميزات وبصمات)، 03.2/03.3/03.5/03.6/03.7/03.8 (تحديد قابلية التمييز)، 04.1/04.2/04.10 و05.1–05.9 (المنحنيات والتقدير إن وجدت مراجع)، 06.x (دمج/عدم يقين/امتناع).
ب. **مسار ML المقارن:** 03.1/03.4، 04.3–04.9، 07.1–07.9؛ اختيار نموذج بناء على بيانات مجمّعة، مقاومة overfit، تبعيات التنفيذ ومكسب الاختبار وليس اسم الخوارزمية.
ج. **المسار المتقدم المشروط بالبيانات:** 02.4/02.6، 08.1–08.7 للنماذج الزمنية والعصبية والمحاكاة والنقل؛ 08.8–08.12 للخلائط، 09.x للتموضع والاستشمام الروبوتي، 10.1 دليل النفي؛ لا تُرقى بلا بيانات ملائمة لكل ادعاء.
د. **MQDataScience 76:** 11 متعددة متغيرات بيئية، 38 انحدار/منحنيات، 14 زمنية، 13 تعلم آلي. الموجود على Android 14 (12 تطابقات مصدرية مذكورة و2 تقريبيان)، 62 `PORT PENDING`. تُقيّم الجدوى، ملاءمة المصدر واللوازم؛ لا يفرض نقل غير الضروري أو تسميته `Ready`.
هـ. **أداة العزل:** `technique-80-traceability.csv` و`mq76-port-and-usage-matrix.csv` في المسار البحثي لهذه الخطة، لكل سجل رقم ووظيفة وحالة أصلية ودور/قرار V6؛ يجب تحديث قرار `SELECTED/DEFERRED/REJECTED` بتوقيع المراجعة بعد benchmark.
و. **مصادر المعرفة:** مستقلة `manufacturer`, `peer literature`, `MQDataScience`, `approved-local`, `QSPR molecular`; PubChem ليس مصدر استجابة MQ. 80 مدخلًا ببليوغرافيًا لا تعني 80 ملفات خام؛ فهرس `reference-80-provenance-matrix.csv` يبيّن مستويات المصدر.

## 7) برنامج المحاكاة (Simulation) — مساران يجب فصلهما

**SIM-A — تحقق رياضي/كهربائي مضبوط (unit/physics simulation):**
- توليد `ADC → Vrl → Rs → R0 → ratio` بعقود ومرجعيات معروفة، والتحقق من معاملات `log` وinverse/forward لجميع المنحنيات المناسبة؛ فحص حالة الحدود `0,NaN,Inf,negative,rail,missing`.
- توليد قياسات افتراضية عند 5–8 نقاط ضمن المجال لكل مصدر، تماثل الدوال (roundtrip)، 1000 حالة محاكاة مولدة لكل فئة من العيوب كحجم تجريبي **مقترح** لا شاهد دقة.
- سيناريوهات `RL` الخطأ ±10/25/50%، `Vc` تغيرات، `R0` نصف/ضعف/10×، وحدات Ω/kΩ وppb/ppm، وعينة سخان MQ7/MQ9 غير مؤهلة؛ قيم الضغط وحدود الدائرة تُستمد من المصدر الموثق لا تُرسم هكذا تجريبيًا.
- تحليلات Monte Carlo بضوضاء مشتركة وعشوائية، حساسات مترابطة، saturation، فجوات زمن، تغير معدل أخذ العينات، وفشل BME؛ المعلمات غير المقاسة وسمها `ASSUMED_SYNTHETIC`.

**SIM-B — محاكاة أزمنة وبيئات وخلائط لاختبار الاستدلال:**
- نموذج ملاحظات `log(r_i(t)) = F_i(g,c,T,RH,heater)+h_i(t;τ_up,τ_down)+drift_i(day)+noise_i(t)+cross_sensitivity_i`؛ فقط معاملات ذات مصدر مؤهل تُوسم `SOURCE_CONSTRAINED`.
- نموذج مزج غير خطي مختلف عن جمع استجابات الغازات النقية، يستكشف unknown/interferent/blank، وتغاير الحرارة والرطوبة، وتأخر دخول الغاز وتطهير الحجرة.
- مصفوفة اختبار `single known / close competitors / unseen gas / mixture / blank / drift / humidity shift / heater invalid / missing 1–4 channels / eight-channel loss / invalid R0 / cross-source conflict / out-of-domain / burst noise / stale sample`.
- `S0` اختبار مواصفات فيزيائية، `S1` اختبار سليم بالمعادلات، `S2` حقن عيوب، `S3` مخاليط/مجهول، `S4` حساسية التحليل، `S5` replay مصادر باحثين، `S6` تجارب Tank الفيزيائية.
- 10 بذور منشورة كبداية للتحليل العددي، seeds ثابتة + repetitions؛ لا تدفع مؤشرات مولّدة إلى قائمة G3. التحقق الواقعي محجوز ببيانات منشورة/عينة جسمية لم تُستخدم في الملاءمة.

**مخرجات المحاكاة:** `simulation_config.yaml`, `synthetic_scenarios.jsonl`, `oracle_outputs.jsonl`, `v5_vs_v6_diff.csv`, `simulation_limitations.md`, `simulation_figures/`, `simulator_model_card.md`؛ كل سجل `synthetic=true` وأصل المعاملات.

## 8) أخذ عينات متعددة المصادر (real researchers' measurements)

البروتوكول المفصل في `docs/research/V6-CHEMCORE-DATASET-SAMPLING-PROTOCOL-2026-10-29_AR.md`؛ **أخذ عينات متنوع مشروط بفحص الملف الأصلي وترخيصه ومعرفات التعرض**:
- **UCI1081 (90 تعرضًا):** كل 90 في المعالجة الأولية؛ 6 غازات × 3 تراكيز (50/100/200 ppb) × 5 تكرارات، 10 قنوات منها MQ135 واحد؛ مقارنة `MQ135-only` مع المصفوفة المتاحة في المصدر، وحجب تركيز كامل دون تسرب.
- **UCI270 (13,910 قياسًا):** مؤشرات 128 من 16 TGS × عشرة دفعات زمنية؛ عينة تطوير طبقية من الغازات/الدفعات/مستويات التركيز عند توافرها؛ الاحتفاظ بالدفعة 10 كاملة للاختبار الزمني المغلق وعدم مضاعفة UCI224.
- **UCI308 (58 تعرضًا):** كل التعرضات الأصلية، 16 trace لكل تعرض؛ مقارنة temporal/signal؛ لا تحويل تراكيز سائلة إلى ppm غازي.
- **UCI309 (180 تعرضًا):** كل 30 وضع تدفق × 6 تكرارات؛ مخاليط ethylene/CO أو ethylene/CH4، TGS8 وتقديرات GC-MS **لمتوسط** ppm؛ حجب كل configuration عند الاختبار وليس قصاصات زمنية.
- **MQ-EXT-03:** 6,400 صف ADC منشورة في نسخة مرآة سبق فحصها؛ استعادة `recording/session` قبل أي تقييم؛ فئات `NoGas/Perfume/Smoke/Mixture` ليست هويات غاز نقي.
- **MQ-EXT-10/11/12/14** و`MOS-EXT-16/17/18`: مصادر تكميلية **HOLD_RAW_PENDING** إلى أن يكتمل جلب الملف الأصلي والتحقق من الوحدة والتكرار والمجموعة والترخيص؛ يمكن تنفيذ اختبارات features/multi-device/drift/mixtures حيث تكون مناسبة فقط، مع فصل انتقال العتاد.

**لا تعتبر 14,238 تعرضات خارجية 14,238 قياس Tank**، ولا يكفي خلط مجاميع التدريب والاختبار أو جمع نتائج الأداء بمقاييس مختلفة.

## 9) تصميم المقارنات والاختبارات الإحصائية

| Baseline | السلوك المقارن | اختبار القيمة الإضافية |
|---|---|---|
| B0 | ChemCore V5 المحدد SHA وبنفس المصدر وR0 | خط أساس محفوظ |
| B1 | curve-only inverse log-fit مع غاز معروف ضمن المصدر | صلاحية الرياضيات والمرجع |
| B2 | fingerprints static single/full-array | تمييز نمط لا ppm |
| B3 | temporal features + fingerprints | أهمية t10/t90/recovery مقابل B2 |
| B4 | PCA/LDA/PLS-DA, kNN/SVM/RF/XGB عند أهلية البيانات | accuracy تحت grouped holdout |
| B5 | independent source fusion + abstention | فائدتها على open-set/FPR والالتباس |
| B6 | joint concentration+identity | نسبة الخطأ بشرط عدم تمرير الهوية الحقيقية في التنبؤ |
| B7 | mixture/interference/unknown diagnostics | حساسية كشف الخلط ورفض الإسناد الخاطئ |
| B8 | robust/winsor/sigma or ML ablations | أثر إزالة كل مصدر/حساس/مرحلة على الأداء |

**خطة التقسيم:** grouping حسب exposure→run→day→device بحيث تبقى القراءات المتصلة والتسجيلات الأصلية والمخففة في قسم واحد. التدريب داخل `development` فقط؛ المعايرة الاحتمالية/الامتناع في `calibration` منفصل؛ الاختبار النهائي `sealed` ولا يتكرر اختياره بعد الاطلاع على أخطائه دون مجموعة جديدة. اختيار معامل أو ميزة يتم داخل nested/grouped CV، وتجنب `leave-one-concentration-out` الذي يجعل إعادة ضبط الميزات ترى بيانات الاختبار.

**المقاييس الملزمة:** macro-F1، balanced accuracy، top-1/top-3 conditional، coverage/rejection/accepted error، unknown false attribution وOOD AUROC/AUPRC حيث توفر negatives ذات مصداقية، per-gas/level `MAE, mean signed error, MARE` في المجال الموثق، interval coverage إن كانت فترات معايرة، residual and class confusion، استقرار drift، runtime p50/p95 والذاكرة واستهلاك البطارية/الحرارة. تقدير ثقة CI يجمع حسب *مجموعات التعرض المستقلة*، وليس timestamps. أخطاء ppm حول الصفر تستخدم مطلق ppm لا نسبة قسمة على صفر.

**اختبار التحسين:** قارن B0 وB6 على *نفس* حالات الاختبار، بالترتيب والتعريفات والانسحاب نفسها، مع paired difference و95% grouped bootstrap CI؛ لا يتم اختيار «أفضل خوارزمية» بعد رؤية sealed test وإعلانها معتمدة دون اختبار جديد.

## 10) مراحل التنفيذ المرحلية وبوابات الخروج

| المرحلة | حزمة العمل | مخرجات إلزامية | بوابة الإغلاق |
|---|---|---|---|
| P00 | تثبيت V5/Stable/firmware/research refs وتدقيق التطبيق ومخطط البيانات | `baseline_manifest.json`، حالة endpoints، مصدر كل ملف | التزام SHA، مقارنة branch، عدم فقد V5 |
| P01 | جرد المراجع والمنحنيات والمصادر والـ80 والـ76 | CSV matrices، 80×76 linkage، dedupe DOI/raw SHA | 100% فهرسها **بالحالة الحقيقية**، وليس ادعاء raw verified |
| P02 | تحليل مادي لعقد RL/R0/Vc/ADS/heater | `sensor_channel_contract.json` + electrical error analysis | الفحص البرمجي pass؛ أي مجال فيزيائي مجهول = `HARDWARE_PENDING` |
| P03 | تصميم وحدات API/provenance/evidence/DB | ADRs، typed schemas، compatibility and migration plan | schema tests وold-session replay بلا تغيير |
| P04 | محاكي رياضي ومحرك replay، إعداد مشاهد العيوب | SIM-A/B sources + oracle + reproducible seed manifest | roundtrip, invariants, synthetic negative tests؛ ليس G3 |
| P05 | استيراد المصادر وأخذ العينات والـholdouts | raw/source manifest، loader، samples/split locked | 100% raw SHA + licence + no leakage للمجموعات المقبولة |
| P06 | منحنيات/R0/دليل المصدر، مقارنة محرك V5 | curve compatibility audit وB0/B1 diff | رفض اختلاف الوحدات/المدى؛ no silent ref switch |
| P07 | features والبصمات والتعلم الآلي المختار | scored feature modules، B2/B3/B4 + parity tests | out-of-fold metrics، عدم تضخيم الدقة |
| P08 | evidence fusion/unknown/mixtures و`reportability` | B5/B6/B7/B8، failure catalog، model cards | unknown/mix refusal، M4 fail-closed دائمًا دون دليل |
| P09 | الربط داخل Android V6 وStable والمختبر والقياس الآني والجلسات | facade adapter، source-overlay، APK test artifacts | Gradle/CI + UI/DB/export + physical smoke وE-STOP |
| P10 | تحقق G3 الفعلي مع مراجع غاز | `tank-exposure-1.0` + G3 evidence package | شروط ملف G3 المُسبق؛ 0/80 حتى تتم تجارب حقيقية |
| P11 | تقييم الإصدار وتحزيمه ونشره بعد الإذن | acceptance report، APK SHA/signature، release notes | موافقة صريحة وإثبات عدم التراجع |

كل مرحلة توثّق `READY/BLOCKED/PASS/FAIL`، ولا يُعتبر نجاح Phase P09 وحده قبولًا لمرحلة P10.

## 11) بوابة G3 الفيزيائية المرجعية (لا تبدأ من simulation)

المصدر الحاكم: `HazRob_Tank_G3_Gate_2026-10-26.md` الموجود في مكتبة مشروع البحث، و`research/G3_TANK_MEASUREMENT_PROTOCOL_AR.md`. بروتوكول 26 أكتوبر **مقترح داخلي** ونسب القبول فيه ليست أرقامًا مفروضة من ISO/الشركة ويجب تثبيتها قبل بدء التعرضات:
- جرد الحساسات والثمانية المادية، المقاومة الفعلية RL ودائرة ADC/المقسم؛ ثلاثة اختبارات كهربائية مكررة لكل نقطة منخفضة/وسطى/عالية؛ مرجع غازي مستقل حقيقي.
- أول حملة: غازان موثوقان على الأقل، خمسة تراكيز موجبة لكل غاز، سبعة أيام مستقلة، ثلاث حقن/استرجاعات لكل تركيز في اليوم؛ **105 تعرضات لكل غاز، 210 لغازين** لا تشمل البلانكات/التداخل/المعايرة، ومنها **45 تعرضًا مغلقًا لكل غاز** خلال ثلاثة أيام اختبار.
- أول وآخر `blank` لكل جلسة وتنظيف/استرجاع بعد كل تعرض؛ 30 حالة تحدٍ مغلقة على الأقل تجمع غازين مجهولين خارج المكتبة وblanks؛ العينات المجهولة غير المعيارية لا تصلح لحساب حساسية كاشف unknown.
- cross-device يتطلب ثلاثة arrays مادية مستقلة إذا ادعينا التعميم؛ النظام الحالي لا يثبت ذلك.
- المقاييس الأولية المقترحة داخليًا: `lower95 balanced_accuracy >=0.80` و`per gas recall >=0.80`، و`unknown false-known upper95 <=0.10`، و`MARE upper95 <=0.20` و`|relative bias|<=0.10` **ضمن مجال فوق LOQ**؛ لا يجوز اعتمادها قبل تجميد metric/power/CI/claim، ولا يعني اجتيازها شهادة سلامة أو مطابقة OSHA.
- القياسات/الأدلة: `protocol.json,source_register.json,reference_register.csv,certificates/,raw/,raw_manifest.csv,exposures.jsonl,exposure_provenance.jsonl,events.csv,exclusions.csv,split_manifest.csv,techniques/<id>/spec.json,techniques/<id>/model_manifest.json,predictions.csv,metrics.json,uncertainty_budget.json,decision.md`، مع SHA لكل مصدر.
- 05.8 LOD/LOQ و10.1 الاستبعاد السلبي يحتاجان blank وlow-level مراجع إضافية، ولا يجوز افتراض غياب غاز لعدم استجابة MQ.

**معايير قياس مرجعية لا تعني شهادة:** ISO 6145-1:2019 ديناميكيات مخاليط المعايرة، ISO 6142-1:2015/ISO 6142-2:2024 تحضير المخاليط الوزني، **ISO 6143:2025** (وليس نسخة 2001 المسحوبة) طرق مقارنة المخاليط، ISO/IEC 17025:2017 أهلية المختبر، JCGM 100:2008 التعبير عن عدم اليقين، ISO 11843-2:2000 حدود الكشف في الحالة الخطية. يجب شراء النصوص كاملة عند تنفيذ اختبار يدّعي المطابقة، ولا تُستنتج متطلبات غير منشورة من ملخصات ISO.

## 12) تصميم الاختبارات المتعددة ذات الأولوية

**الحزمة T0 — العقود:** unit conversion, calibrated RL/VC, old R0 replay, heater-phase validity, firmware API4, Stable SHA.  
**T1 — المنحنيات:** 43 + 6 source priors، 39 legacy power laws إن كانت موجودة، exact manufacturer graph digitization؛ source/chemical semantics، roundtrip/order sensitivity/source discontinuity.  
**T2 — البصمات:** normalized 8-channel vectors، sparsity، stale/failed channels، static vs temporal signature، 3D/4D features وسبب فشلها.  
**T3 — learning:** 76-model registry vs actual 14 Android ports; training-only preprocessing and ports; G2 trained models source-specific (LDA/SVM/XGB/CNN/TCN/GRU/GNN) على بياناتها الخارجية لا يمنحها Tank validation.  
**T4 — fusion:** independence groups، shared training / source duplications، rare gases، unsuitable `single-contributor` loss، calibrated abstention، unknowns and mix contradictions.  
**T5 — بيئات ومقاومة:** T/RH/P, heater mode, drift days, missing 1–4 channels, baseline shift, sensor failure/lag, partial sessions.  
**T6 — Android:** offline, real API4 telemetry, R0 workflow، instant, field approved, full reports, SQLite, UI contrast, camera optional، developer tab and status.  
**T7 — سلامة:** no false M4, no unsourced occupational conclusions, retained E-STOP, lost-link motors, no app-version-to-firmware match.  
**T8 — reproducibility:** source hashes, scripts, CI evidence, open-source license records, seeds, per-gas and per-run metrics and rollback.

## 13) الأداء وميزانية الموارد

تقاس على هاتف Android 12 مرجعي مُسمّى وجهاز محاكاة ثان، مع تحديد RAM/CPU/OS/thermal state. الميزانيات التالية **أهداف تصميمية مبدئية وليست نتائج مقاسة**: الملاحظة في واجهة البث لا تعرقل سلايدر القيادة؛ تحليل نافذة آنية p95 < 1 ثانية عند المرشحين المؤهلين المعتادين، والتحليل النهائي الشامل خارج الخيط الرئيسي، وعدم وجود `ANR` أو تسرب ذاكرة في اختبار طويل، وحد Stable model bundle قابلة للرجوع. إن أخفق النموذج الثقيل، ينخفض إلى نسخة أخف موثقة وتظهر `DEGRADED`، لا يُعلن نجاحه افتراضًا. لا نقبل تخفيف safety أو إزالة بيانات provenance لتحقيق السرعة.

## 14) سجل المخاطر وقرارات التخفيف

| الخطر | أثره | المانع |
|---|---|---|
| R0 غير متوافق/وحدات خاطئة | ppm زائف | per-source R0 definition + refuse incompatible model |
| RL ثابت خطأ أو heater غير مطابق | انحراف شامل | عتاد مادي، range QC، قفل مسارات quant |
| تسرب التكرارات/الأجهزة | accuracy زائفة | dependency grouping + sealed holdout |
| double counting منحنى أو مصدر | ثقة زائفة | independence graph + source ablation |
| `Posterior` من softmax يُقرأ كاحتمال | ثقة تشغيلية كاذبة | labels `relative score` وcalibration required |
| تغيير Stable أثناء القياس | عدم قابليّة إعادة الإنتاج | snapshot immutable / rollback |
| غازات مخلوطة/غير موجودة | إجبار تسمية خاطئة | UNKNOWN/MIXTURE/abstention، negative controls |
| عدد النماذج > قدرة الهاتف | تعليق التطبيق | staged selection، performance budget، async analysis |
| نسخة Python مخبرية تسمى Android Ready | واجهة مضللة | dual registry with proof of executor |
| نتائج المصدر الخارجي تحل محل G3 | خطر سلامة | lab vs device claim gates، reviewer approval |
| فقد توافق جداول V5 والتقارير | بيانات تاريخية تالفة | migrations test/replay + backup/rollback |

## 15) إدارة المشروع والاستئناف

- الالتزامات فقط على فرع `feature/tank-v6-implementation`. لا بناء APK أو تعديل firmware ضمن وثيقة الخطة نفسها؛ التعديلات اللاحقة تجري بتعليمات معتمدة وببوابات.
- وثّق كل 10 تقنيات راجعتُها/نفّذتُها دفعة بتقرير مقارن، لا تدّعي أن إكمال عشر من المؤشرات وحده يثبت دقة المصفوفة.
- كل commit: `V6-002` + phase + source SHA + tests executed (PASS/FAIL/NOT_RUN) + new risks + next step، في `docs/implementation/V6-PROGRESS-LOG-2026-10-29.md`.
- أي ملف بناء/نتيجة: `artifact_sha256`, `source_rev`, `stable_rev`, `model_rev`, `samples_manifest_sha` و`evidence_gate`.
- **نقطة البدء بعد اعتماد الخطة:** P00 (hash/inventory, V5 replay) → P01 (80/76/80 trace) → P02 (متطلبات العتاد R0 لا يتوقف عليها SIM-A) → P03 → P04 → P05 → P06/P07/P08 → P09 → P10. يمكن تطوير البرمجيات مع بقاء G3 BLOCKED، لكن لا تتخطاه لقبول ppm.
- **الاستئناف بعد انقطاع المحادثة:** اقرأ هذه الخطة ثم سجلات V6 ثم مصفوفات التتبع وبروتوكول sampling، ثم قارن HEAD بالنسخ الموثقة؛ ابدأ من أحدث commit ثبت نجاحه وليس من خطة V5 القديمة.

## 16) روابط المصادر الأولية/القياسية

- Dataset official UCI: [1081](https://archive.ics.uci.edu/dataset/1081/gas+sensor+array+low-concentration), [270](https://archive.ics.uci.edu/dataset/270/gas+sensor+array+drift+dataset+at+different+concentrations), [308](https://archive.ics.uci.edu/dataset/308/gas+sensor+array+under+flow+modulation), [309](https://archive.ics.uci.edu/dataset/309/gas+sensor+array+exposed+to+turbulent+gas+mixtures.).
- [MQUnifiedsensor](https://github.com/miguel5612/MQSensorsLib), [MQDataScience](https://github.com/abcdaaaaaaaaa/MQDataScience); [Winsen](https://www.winsen-sensor.com/); [PubChem PUG REST](https://pubchem.ncbi.nlm.nih.gov/docs/pug-rest).
- [ISO 6145-1:2019](https://www.iso.org/standard/53598.html), [ISO 6143:2025](https://www.iso.org/standard/85579.html), [ISO/IEC 17025:2017](https://www.iso.org/standard/66912.html), [JCGM 100:2008](https://www.bipm.org/en/doi/10.59161/jcgm100-2008e).
- Internal authoritative: [Tank research progress](https://github.com/HazRob/HazRob-Tank/blob/main/research/PROGRESS_80.json), [central bibliography](https://github.com/HazRob/build-harness/blob/main/library/reference-bank/external-research/master_catalog.json), [baseline analysis](../research/V6-002-CHEMCORE-MANUFACTURER-R0-PPM-FINGERPRINT-GAP-AUDIT-2026-10-29.md).

## 17) ملاحظة ضبط قائمة السيناريوهات

يتضمن سجل `docs/research/V6-CHEMCORE-SIMULATION-SCENARIOS.csv` **24 سيناريو محددًا** من التحقق الكهربائي والمنحنيات إلى المحاكاة البيئية والخلائط والفحص الفيزيائي G3. هذا عدد سيناريوهات مخطط لها وليس تنفيذًا جديدًا؛ يجب توثيق النتيجة الفعلية لكل Scenario ID عند التنفيذ.
