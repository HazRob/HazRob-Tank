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

## 18) إضافة معتمدة إلى نطاق الخطة: مكتبة معايرة R0 والمنحنيات والبصمات التاريخية

**مستند المصدر الملزم:** `../research/V6-CHEMCORE-R0-CURVES-FINGERPRINTS-INGESTION-PLAN-2026-10-29_AR.md`. اعتمد باعتباره جزءًا أساسيًا من P01/P02/P04/P05/P06/P07/P08/P09، وليس دراسة خلفية مؤجلة.

**المصفوفات الجديدة:**
- `../research/V6-CHEMCORE-MQUNIFIED-R0-RATIO-AUDIT.csv`: 8 عوامل `Rs_air/R0` الخاصة بأمثلة MQUnifiedsensor، مقارنة رياضيات R0 من تجربة 2018 ومعرفة عدم تطابق MQ7 وMQ8.
- `../research/V6-CHEMCORE-MQDATA-CURVES-43-COMPATIBILITY.csv`: 43 منحنى مع معاملات المصدر ومجاله و`output_scale`؛ **ستة** MQ3 بها `output_scale=50` ويلزم توثيق تحويلها.
- `../research/V6-CHEMCORE-HISTORICAL-LIBRARY-14-ASSET-MANIFEST.csv`: 14 أصلًا بحثيًا تاريخيًا محفوظًا بأرقام Git blob أصلية، تشمل منحنيات وبصمات ونماذج ودراسات.

**مبدأ الاستفادة:** حفظ بصمات 11 مادة + فئتين تشغيليتين (13 عائلة مصدرية مشتقة)، و59 جلسة headspace غير جزيئية، و4 فئات MQ ADC (6400 *صف* ليس 6400 تعرضًا)، و18 موجزًا غاز/تركيز من UCI1081، والاستفادة من نماذج 76 مع إبقاء `Pending` متى لم تُنقل فعليًا. 39 منحنى GasEngine والتصنيفات القديمة مصدر مقارن إضافي؛ لا إضافة مباشرة لعدد 49 Stable لأنها قد تتداخل معها. استُخدمت ملفات `HazRob/build-harness/library/research/hr069/current-manifest.json` كمصدر 14 أصلًا مع الالتزام المرجعي القديم `33ee17dbbd291ded7154f5030612d90f6e79f81b`.

**مطلب عقد V6:** أضف `ratio_direction` و`r0_definition_id` و`source_graph_revision` و`reference_compound_semantics` لكل منحنى؛ اقفل التحليل الكمي إذا افتقد المصدر التوافق، لكن احتفظ به كبحثي مع سبب. `MQUnifiedsensor.readSensorR0Rs()` يستخدم `R0/Rs` بعكس `readSensor()`، وهو سبب لمنع الخلط. المصدر الخارجي لا يعوض R0 على جهاز Tank.

**المخرجات الملزمة لمحاكاة هذه الإضافة:** `r0_cross_source_compatibility.csv`, `curve_unit_roundtrip.json`, `ppm_sensitivity_to_r0.csv`, `response_prior_dependent_fingerprints.jsonl`, `fingerprint_overlap_mask_test.json`, `duplicate_lineage_report.json` مع `source_hash` وأرقام المنحنيات. ما سبق مجرد أسماء مخرجات مطلوبة وليست نتائج مُنجزة.

## 19) مراجعة سيناريوهات المصادر الملحقة
أضيفت **7 حالات محاكاة متخصصة** `SIM-R01..SIM-R07` إلى أصل **24** سيناريو، فأصبح سجل `../research/V6-CHEMCORE-SIMULATION-SCENARIOS.csv` يحتوي **31 سيناريو مخططًا**. يشمل المخطط الأصلي حالة تحقق فيزيائي منفصلة `REAL-G301`، لذا **30 سيناريو برمجيًا/بحثيًا + واحد G3 ميداني**. لم تُنفّذ هذه السيناريوهات؛ السجل الآن متطلبات اختبار لا نتائج. التتبع في `V6-CHEMCORE-EXECUTION-TRACKER.json`.

## 20) V6-003 — نطاق مؤجل

بقرار المستخدم في 2026-10-29، يُؤجل تطوير تحليل بصمات الإحماء الأولى لحساسات MQ وتقنيات Bosch متعددة التسخين إلى ما بعد إكمال الخطة الأساسية. لا تغيير على فيرموير الروبوت أو مسار R0 أو التسجيل في الهاتف الآن. تُحفظ الدراسة العلمية في `../research/V6-003-BOSCH-BME68X-MQ-STARTUP-TRANSIENT-FEASIBILITY-2026-10-29_AR.md`. عند إعادة فتحها لاحقًا يكون التخزين على الهاتف دون طلبات جديدة من ESP32.

## 21) تعديل سياسة الاستخدام البحثي — 2026-10-29

**بقرار المستخدم:** يُستخدم كل منحنى/بصمة/نموذج صالح حسابيًا في التجريب وترتيب المرشحين دون اشتراط اكتمال التوثيق المخبري أو تعريف R0 أو مجال المصدر أو عدد الحساسات أو مستوى تطابق معين. تُحوّل القيود الميدانية السابقة إلى **ملاحظات تفسيرية** لا إلى استبعاد من الحساب البحثي. لا يسمح باختلاق رقم من `NaN/Infinity` أو صفر/سالب في المقامات أو معادلة غير معرفة. لا يصبح تقدير المصدر المعروض على الشاشة قياسًا مُعايرًا بمجرد حسابه.

**المرجع المحدّث الملزم لهذه السياسة:** `../research/V6-002-INCLUSIVE-ANALYTICS-POLICY-2026-10-29_AR.md`. يسود هذا القرار على أي نص سابق في هذه الخطة ينص على حجب المنحنيات والأدلة البحثية **لمجرد** نقص مصدرها، عدد القنوات، تدقيق السخان أو عتبة الترجيح. تبقى بوابة G3 في سجلها العلمي باعتبارها مستوى إثبات مستقلًا، لا عائقًا لحساب النتائج البحثية وعرضها. تقنية الإحماء V6-003 مؤجلة كما تقرر.

## 22) القرار المعماري الأحدث: ChemCore Tank V6 جديد كليًا — supersedes previous engine integration

**توجيه المستخدم بتاريخ 2026-10-29 17:46Z:** نحن بصدد بناء ChemCore جديد كليًا على إصدار الدبابة السادس؛ يجب عدم الاستناد إلى أي محرك ChemCore سابق في بنية المحرك الجديد، أو اعتباره ملحقًا لنواته. هذا التوجيه **أحدث وأعلى أولوية من أي فقرة سابقة في هذه الخطة** حول مقارنة أداء نواة ChemCore الجديدة أو ربطها كـoverlay لنواة أقدم.

**معتمد من الآن:** `docs/architecture/CHEMCORE-TANK-V6-CLEAN-SLATE-ADR-2026-10-29_AR.md`، والمسار البرمجي الجديد `android/app/src/main/java/com/hazrob/tank/chemcore/v6/`. يقتصر الاقتباس من العمل السابق على مجموعات **المصادر العلمية الخام والمنحنيات والبصمات والقياسات** لا على الأصناف أو مديري الاستدلال أو سياسات الترجيح الموروثة. تبقى أدوات المستخدم في Android قائمة مؤقتًا إلى أن تنضج منظومة V6 الجديدة، دون أن تعتمد عليها النواة الجديدة.

**أول ما نُفّذ:** مصدر 43 منحنى MQ، نواة استدلال مستقلة `Engine.java`، نموذج إدخال يحفظ Rs الخام حتى عند نقص R0، حسابات source fit وjoint log-domain fit لكل مرشح، 13 مجموعة مرشحين من المصدر، ومخرجات غير مقيدة إلا بالفساد الحسابي. تحقق CI `40925694019` بنجاح؛ **59 JVM assertion و3 Python coefficient/dependency assertions**. بناء APK V6 وتفعيل واجهات المحرك ما زالا غير منجزين.

**تستأنف الخطة ابتداءً من مراحل V6-N01..N07 الجديدة في المتتبع** لا من محاولة تكييف محلل آخر. تقنية MQ startup/Bosch مؤجلة. `G3=0/80` يمثل مستوى الإثبات الميداني، ولا يمنع إنشاء وترتيب الفرضيات البحثية.


## تطبيق مستقل موازٍ — فحص نضارة الأغذية (اللحوم / الفواكه)

بقرار المستخدم في 2026-10-29، **نضارة الأغذية وظيفة مستقلة عن ChemCore V6 للغازات** داخل تبويب القياسات بجوار «الجزيئات». تُعرض قائمة منسدلة لاختيار «اللحوم» أو «الفواكه»، ويُستخدم لكل خيار مرجع علمي ومحرك ومخطط وتقرير PDF خاص به؛ دون إقحام أي منحنيات أو درجات طعام في هوية أو كيمياء أو PPM للغازات.

**المنجز:** مسار الفواكه يعتمد 54 بصمة فواكه من بنك يحفظ 59 ملفاً أصليًا، ومسار اللحوم البقرية يستخدم 10,800 قراءة من دراسة Wijaya 2018 في خمس سلاسل، ويفصل فئات `excellent/good/acceptable/spoiled` بحثيًا باستخدام سبعة طرازات MQ مشتركة؛ ضَعف أداء `TS5` (دقة متوازنة 38.9%) يمنع عرض النتيجة باعتبارها تأكيدًا لسلامة اللحم. كلا المسارين عمليات فردية من الهاتف بقراءات MQ الحالية، مع رصد T/RH من BME280 للحوم، وتقرير PDF واحد عند نهاية الفحص. التفاصيل والمراجع والاختبارات: `TANK-FOOD-FRESHNESS-TWO-MODES-CHECKPOINT-2026-10-29_AR.md`.

**إثبات الشيفرة:** اختبار عزل/مصدر طعام `40932744483` ناجح، 11 اختبارات Python، 316 تحققًا لمحرك الفواكه و30 لمحرك اللحوم؛ تجميع Java Android `40932621388` ناجح. **ما يزال اختبار الهاتف والفحص الحقيقي وتحقق رسوم PDF وإصدار APK غير منجز**. لا تغيير Firmware.

## 23) نقطة الاستئناف 06 المعتمدة — التنفيذ الفعلي والأرقام المصححة

حتى 2026-10-29، أصبح لدى **المحرك المستقل الجديد** 24 تقنية من أصل 80 **ذات تنفيذ جزئي واختبار برمجي**، وبقي 56 تقنية لم تُنقل، مع `Tank G3=0/80`. شملت آخر دفعة عشر تقنيات: `01.1, 01.2, 01.6, 01.7, 02.6, 02.7, 03.3, 03.5, 03.7, 03.8`، في `MeasurementDiagnosticsV6.java` و`ResearchDiagnosticsV6.java`. وثّقت النتائج والأرقام والملفات في `V6-CHEMCORE-NEW-ENGINE-CHECKPOINT-06-2026-10-29_AR.md`، وهو المرجع الأحدث لهذه المرحلة. CI النواة `40938234785` ناجح (392 تحققًا للدفعة، 1,782 تحققًا تراكميًا)، واختبار المصدر `40939360159` ناجح (69 Python). البناء على Android حتى الآن **compile-only** دون APK.

**إلغاء مرجع benchmark قديم:** نتائج UCI1081 الرقمية الأولى كانت خاطئة بسبب ترتيب صفوف المصدر `reshape(900,10)`؛ ثبت حسابيًا أن الصيغة الصحيحة `reshape(10,900).T`، ومن ثم لا يجوز استخدام القيم السابقة. النتائج المرجعية الصحيحة بعد حجب كل مستوى تركيز: MQ135 فقط Logistic 28.89% وLDA 34.44%؛ مصفوفة المصدر الكاملة (10 حساسات) Logistic 71.11% وLDA 78.89%. لا تعني هذه الأرقام اختبار Tank أو معايرته؛ راجع `research/chemcore_v6/results/uci1081_corrected_sensor_blocked_benchmark_2026-10-29.json`.

**الخطوة العملية التالية:** عشر تقنيات إضافية من القائمة غير المنقولة، وربطها بالمسار العلمي لنواة V6 دون الاستدلال القديم، ثم مقارنة مخرجاتها ببيانات أصلية محجوبة ودمج الواجهة والتقارير. تبقى محركات اللحوم والفواكه منفصلة تمامًا. وأي بند قديم في هذه الخطة يخالف أرقام checkpoint 06 أو ترتيب UCI1081 الجديد يُعتبر متجاوزًا.

## 24) أحدث حالة معتمدة — Checkpoint 07: تنفيذ الدفعة N03-B

**الأحدث حتى 2026-10-29:** ملف `V6-CHEMCORE-NEW-ENGINE-CHECKPOINT-07-2026-10-29_AR.md` هو مرجع التنفيذ الأحدث لهذه المهمة، ويتقدم على أي أرقام سابقة في هذه الوثيقة. أصبح لدينا **34 تقنية ذات تنفيذ جزئي داخل نواة الغاز المستقلة، و46 لم تُنقل، و0 اعتماد فيزيائي G3**. آخر دفعة عشر تقنيات هي `01.5,03.1,03.4,04.3,04.4,05.1,05.2,05.7,05.8,06.7` في `CalibrationResearchV6`, `ClassicalLearningV6`, `EvidenceConflictV6`. خصوصًا `03.4` يغطي diagonal LDA فقط **دون PLS-DA**، وsoftmax لا يعني احتمال غاز مُعايرًا.

[CI النواة 40941942329](https://github.com/HazRob/HazRob-Tank/actions/runs/40941942329) ناجح مع **1,886 Java assertions**، وتجميع كامل تطبيق Android Java في [40941913288](https://github.com/HazRob/HazRob-Tank/actions/runs/40941913288) ناجح **دون إنشاء APK**. أضيف إلى JSON الخاص بمحرك V6 الجديد قسم `source_curve_disagreements` لحفظ اختلافات المصادر دون حذف المرشحين. **لم يُجرَ بعد** حجب مجموعات بيانات المصدر عبر نماذج V6 الجديدة، ولم تُحوّل واجهة القياسات/المختبر إلى المحرك الجديد، ولم يُعتمد ppm على جهاز Tank. جميع مراجع الأغذية خارج هذا المحرك. تحليل أول دقيقة من إحماء MQ/Bosch ما زال مؤجلًا.

**الاستئناف العملي:** اختر عشر تقنيات جديدة من `V6-CHEMCORE-80-TECHNIQUE-NEW-ENGINE-COVERAGE-2026-10-29.json` غير المنقولة؛ نفّذ واختبر ثم حدّث الأرقام بعد نجاح CI، بالتوازي مع تقييم PCA/LDA/KNN/Logistic الجديدة على بيانات UCI1081 الأصلية بتقسيم صحيح، ثم دمج المخرجات في UI والتقارير دون استدعاء المحرك السابق.

## 25) أحدث حالة موثقة — Checkpoint 08 (N03-C / 2026-10-29)

**يتقدم هذا القسم على الحالة 34/80 في القسم 24.** بعد CI ناجح في [40949107155](https://github.com/HazRob/HazRob-Tank/actions/runs/40949107155) أصبح **44/80 تنفيذًا بحثيًا جزئيًا، 36/80 لم تُنقل، 0/80 G3**. أضيفت التقنية 04.10 و06.2 و06.3 و06.4 و06.5 و06.6 و06.9 و07.1 و07.2 و07.3 في SourceValidationV6.java واختبار V6ThirdTenTechniquesTest.java (38 تأكيدًا إضافيًا، الإجمالي **1924 تأكيد JDK**). تجميع Java لتطبيق Android ناجح في [40948928576](https://github.com/HazRob/HazRob-Tank/actions/runs/40948928576) على تعديل النواة؛ **لم يصدر APK ولم تُعتمد هوية أو ppm على جهاز حقيقي**.

المصدر التنفيذي/بوابات النتيجة: [Checkpoint 08](V6-CHEMCORE-NEW-ENGINE-CHECKPOINT-08-2026-10-29_AR.md). بقيت إحصاءات UCI1081 الصحيحة السابقة تخص sklearn **فقط**، ولم تُختبر نماذج Java الجديدة على حجب مستويات التركيز الأصلية بعد. بقيت الأغذية منفصلة والإحماء المؤقت مؤجلًا. الخطوة العملية التالية **V6-N05**: تقييم المصدر الصحيح للموديلات الجديدة؛ ثم V6-N03-D (عشر تقنيات جديدة من 36 غير المنقولة) وV6-N06 ربط الواجهة والتقارير، دون خرق G3.


**تصحيح رقمي لاحق للدفعة ذاتها:** Bayes log-sum-exp وsoftmax stable extreme finite values ورفض التعادل التام دون حدّ هامش؛ [CI 40949747063](https://github.com/HazRob/HazRob-Tank/actions/runs/40949747063) **SUCCESS** مع بقاء **1924 تأكيدًا**، و[CI المراجع 40949746723](https://github.com/HazRob/HazRob-Tank/actions/runs/40949746723) **SUCCESS**. لا زيادة عدد التقنيات بسبب التصحيح.


## 26) آخر نقطة استئناف — Checkpoint 09 (N05 + N03-D، 2026-10-29)

**هذا القسم يلغي أرقام 44/36 الأقدم بوصفها أحدث حالة.** بعد نجاح [CI 40952792580](https://github.com/HazRob/HazRob-Tank/actions/runs/40952792580) أصبحت تغطية نواة الغاز الجديدة **54 تقنية ذات تنفيذ بحثي جزئي / 26 لم تُنقل / 0 معتمدة على Tank G3**، بمجموع **1968 تحقق JDK ناجح**. أضيفت تقنيات 05.5، 05.6، 05.9، 07.4، 07.5، 07.8، 07.10، 08.6، 08.8، 08.11 في SourceNumericsV6.java، مع 44 تحققًا جديدًا. تجميع Android Java ناجح في [40952676989](https://github.com/HazRob/HazRob-Tank/actions/runs/40952676989)، وهو تجميع فقط وليس APK.

**N05 أصبح مُنفَّذًا على بيانات UCI1081 الأصلية** في [40952186046](https://github.com/HazRob/HazRob-Tank/actions/runs/40952186046) بعد تحقق SHA لكل من ZIP وCSV، وحجب التركيز 50 أو 100 أو 200 ppb بالكامل في كل قسم. 24 تقييمًا / 720 تنبؤًا؛ متوسط balanced accuracy لخوارزمية Softmax الجديدة 38.89% مع MQ135 فقط و76.67% مع مصفوفة UCI ذات 10 حساسات. هذه أرقام Java الجديدة لا sklearn القديم، ولا تثبت أي معايرة Tank أو صلاحية هوية غاز مجهول. توجد النتائج الموثقة في research/chemcore_v6/results/uci1081_v6_n05_java_original_source_results_2026-10-29.json، وحزمة النتائج الأصلية محفوظة في المكتبة.

التفاصيل العلمية والروابط والحدود: [Checkpoint 09](V6-CHEMCORE-NEW-ENGINE-CHECKPOINT-09-2026-10-29_AR.md). الخطوة التالية N03-E (عشر تقنيات بحثية من 26) ثم N05-open-set مع مجهولات وتقييم مُنفصل للتدريب/المعايرة/الاختبار، ثم ربط الواجهات والتقارير N06. تبقى الأغذية مستقلة تمامًا وتحليل إحماء MQ/Bosch مؤجلًا، دون APK أو تحقق G3.


## 27) أحدث حالة — Checkpoint 10 / N03-E (2026-10-29)

**الأرقام الحاكمة:** **64/80 تقنية بتنفيذ بحثي جزئي، 16/80 لم تُنقل، 0/80 اعتماد فيزيائي G3**. الدفعة الخامسة أضافت التقنيات 08.5، 08.7، 08.9، 08.10، 08.12، 09.1، 09.2، 09.3، 09.4، 09.5 في الملف المستقل TemporalSpatialResearchV6.java، مع 47 تحقق Java جديد؛ [CI 40954841693](https://github.com/HazRob/HazRob-Tank/actions/runs/40954841693) ناجح بإجمالي **2015 تحقق JDK**. [تجميع Android 40954696816](https://github.com/HazRob/HazRob-Tank/actions/runs/40954696816) ناجح من نوع compile-only، [عزل الأغذية 40954696500](https://github.com/HazRob/HazRob-Tank/actions/runs/40954696500) ناجح. **لا APK V6 ولا اختبارات جهاز Tank ولا استنتاج ppm ميداني**.

أُضيفت عمليات الخرائط والاستشعار المكاني للبحث فقط؛ تُطبق على إشارات مصدر مُدخلة ومواقع معلومة ولا تُمكّن القيادة أو تحديد مصدر تسرب مُثبت. نتائج UCI1081 الأصلية في Checkpoint 09 تظل نتائج بيانات مصدر بحثي لا G3.

[ملف الاستئناف Checkpoint 10](V6-CHEMCORE-NEW-ENGINE-CHECKPOINT-10-2026-10-29_AR.md) هو المرجع الأحدث ويتقدم على أرقام Checkpoint 09 و08. التالي N03-F ثم N05-open-set بمجهولات مستقلة ثم N06 واجهة القياس والتقارير دون المحرك السابق. تبقى أول دقيقة من إحماء MQ/Bosch مؤجلة، وجميع دراسات الأغذية خارج محرك الغازات.


## 28) آخر استئناف حاكم — Checkpoint 11 / N03-F + N05-F (2026-10-29)

**تقدم تقنيات محرك الغازات الجديد:** **74 تنفيذًا بحثيًا جزئيًا، ست تقنيات غير منقولة، صفر اعتماد فعلي G3**. أُضيفت 10 وظائف رياضية إلى `SupervisedLearningResearchV6.java` و`AdaptationEvidenceResearchV6.java`: 04.5 و04.6 و04.7 و04.8 و04.9 و06.8 و07.6 و07.7 و07.9 و10.1. اجتازت [CI 40956582213](https://github.com/HazRob/HazRob-Tank/actions/runs/40956582213) بإجمالي **2073 تحقق Java**، بزيادة 58 عن Checkpoint 10. تجميع Java الخاص بـ Android [40956403097](https://github.com/HazRob/HazRob-Tank/actions/runs/40956403097) ناجح؛ عزل الأغذية [40956403090](https://github.com/HazRob/HazRob-Tank/actions/runs/40956403090) ناجح. لا APK ولا تجربة ميدانية.

**التحقق الإضافي من UCI1081 الأصلي:** [N05-F CI 40956838753](https://github.com/HazRob/HazRob-Tank/actions/runs/40956838753) ناجح: 90 تعرّضًا حقيقيًا من المصدر الأصلي، 3 تقسيمات بحجب مستوى تركيز كامل، 4 خوارزميات Java جديدة بصيغة OVR × خيار MQ135 أو 10 حساسات = 24 تقييمًا، و720 تنبؤًا. متوسط balanced accuracy للمصفوفة الكاملة: SVM 38.89%، bootstrap stump forest 41.11%، Newton boosted stumps 42.22%، MLP 42.22%. كلها أقل من Java Softmax المرجعي السابق 76.67% لنفس مجموعة المصدر/التقسيمات، ولكن بإعدادات نمذجة مختلفة. هذه نتائج مصدر خارجي لا يجوز نسبتها إلى Tank أو استنتاج ppm منها.

**الأدلة السلبية 10.1:** العلامة السلبية لعائلة كيميائية مشروطة ببيانات اعتماد مستقلة لحساس Tank، وتشغيله وسلامة حد الكشف ومشاهدات مستقرة وإسناد المصدر؛ الواجهة **لا تمسح المرشحين ولا المرشح UNKNOWN**، كما أن إدخال المستخدم `calibrated=true` لا يُثبت صحة المعايرة بذاته. لا ترقية أو استبعاد آلي فعلي دون G3.

مرجع الاستئناف الأحدث [Checkpoint 11](V6-CHEMCORE-NEW-ENGINE-CHECKPOINT-11-2026-10-29_AR.md). الستة المتبقية: 01.4 (إحماء مؤجل)، 08.1، 08.2، 08.3، 08.4 (تعلّم متسلسلات وشبكات)، 09.6 (قرار الحركة المؤمَّن). التالي N03-G للتقنيات العصبية الممكنة بحثيًا، ثم N05-open-set بمجهولات مستقلة، ثم N06 لإسناد التقارير إلى محرك V6 الجديد فقط، ثم G3. المحرك الغذائي مستقل تمامًا.


## 29) أحدث استئناف حاكم — Checkpoint 12 / N03-G + N05-G (2026-10-29)

**الحالة المؤكدة:** **78/80 تقنية بتطبيق رياضي بحثي جزئي، 2/80 غير منقولتين، 0/80 اعتماد فيزيائي على Tank G3**. أُنجزت أربع بنى معالجة متسلسلات مستقلة محدودة (08.1 CNN، 08.2 TCN، 08.3 GRU، 08.4 GNN) في `TemporalNeuralResearchV6.java`. أوزان مستخلصات الميزات الزمنية/البوابات/انتشار الرسم البياني **ثابتة**، والمدرَّب فقط رأس تصنيف لوجستي؛ ليست شبكات مُدرّبة end-to-end. اجتاز [CI 40958218641](https://github.com/HazRob/HazRob-Tank/actions/runs/40958218641) بـ **2120 تحقق Java**، منها 47 جديدة، واجتاز تجميع Android برمجيًا [40957697411](https://github.com/HazRob/HazRob-Tank/actions/runs/40957697411) والفصل الغذائي [40957697278](https://github.com/HazRob/HazRob-Tank/actions/runs/40957697278) دون إصدار APK.

[اختبار N05-G على UCI1081 الأصلي 40958081282](https://github.com/HazRob/HazRob-Tank/actions/runs/40958081282) ناجح بعد فحص SHA؛ 90 تعرضًا، 30 نافذة زمنية لكل تعرض × 10 حساسات أصلية، مع حجب مستوى التركيز 50/100/200 ppb بالكامل، **18 تقييمًا و540 تنبؤًا**. متوسط balanced accuracy للمصفوفة الأصلية الكاملة: CNN 65.56% وTCN 53.33% وGRU 81.11%، مقابل GRU بقناة MQ135 فقط 34.44%. لا يجوز تعميم النتيجة على أجهزة Tank أو دقة ppm أو الغازات المجهولة؛ GNN لم يُختبر على بيانات UCI لأن الرسم البياني الفيزيائي غير موثق.

**المتبقي غير المنقول فقط:** 01.4 دورات إحماء MQ/Bosch (مؤجلة بقرار المستخدم)، و09.6 قرارات حركة الروبوت (مقيدة بتحققات السلامة الميدانية). لا نقل شكلي لهاتين التقنيتين. الخطوة التالية إعطاء الأولوية لـ N05-open-set بمجهولات مستقلة وN06 لتوجيه القياسات الآنية والجلسات والتقارير إلى محرك V6 الجديد وحده؛ لا مساس بمحرك الأغذية.

**مرجع الحالة الأحدث:** [Checkpoint 12](V6-CHEMCORE-NEW-ENGINE-CHECKPOINT-12-2026-10-29_AR.md). أرقام Checkpoint 11 أو أقدم أصبحت تاريخية وليست الحالة الراهنة.


## 30) آخر حالة علمية معتمدة — Checkpoint 13 / N05-H (2026-10-29)

**78/80 تقنية بتنفيذ بحثي جزئي، 2/80 غير منقولتين، 0/80 تحقق G3.** أجريت تجربة Open-set على UCI1081 الأصلي بإسناد محكم للتعرّضات، واختيار فئات غازات مجهولة منفصلة للمعايرة والاختبار لكل تقسيم، دون مشاركة أي غاز مجهول للاختبار في التدريب/المعايرة. **36 تقسيمًا** (18 لكل نمط حساسات)، **972 قرارًا على اختبارات محجوبة**، **3240 إسنادًا لصفوف المصدر**، وملف CSV يحتفظ بكل الفرضيات الأربع ودرجاتها؛ نجاح التشغيل [40959780208](https://github.com/HazRob/HazRob-Tank/actions/runs/40959780208). الإجمالي البرمجي للنواة **2148 تأكيد Java ناجحًا** بعد 28 اختبارًا إضافيًا في [40959501615](https://github.com/HazRob/HazRob-Tank/actions/runs/40959501615). تجميع Android Java فقط [40959245040](https://github.com/HazRob/HazRob-Tank/actions/runs/40959245040) ناجح، وعزل الأغذية [40959244989](https://github.com/HazRob/HazRob-Tank/actions/runs/40959244989) ناجح.

**النتيجة الحاكمة: بوابة Open-set غير مجتازة علميًا للاستخدام الميداني.** قُبل 96 من 270 تعرّضًا مجهولًا خطأً في MQ135 المنفرد (35.56%)، و151 من 270 تعرّضًا مجهولًا خطأً في مصفوفة UCI ذات 10 حساسات (55.93%). التعرّفات الصحيحة للمعروف بعد تطبيق الامتناع بلغت 67/216 (31.02%) و114/216 (52.78%) على الترتيب. لا توثيق للاستقلال بين أجهزة/أيام المصدر، ولا يقابل الجهاز المرجعي عتاد Tank، ولا يجوز اعتبار العتبة مستندًا لغاز الهوية أو ppm في الروبوت. أبقِ جميع فرضيات المصدر الصالحة متاحة، واسمح بإشارة UNKNOWN دون حذف البيانات.

التوثيق والمقاييس وملف النتائج المثبت داخل GitHub في [Checkpoint 13](V6-CHEMCORE-NEW-ENGINE-CHECKPOINT-13-2026-10-29_AR.md)، والنتائج الأصلية نُسخت إلى Library. التالي N06-Prep/ربط بحثي آمن لا يصدر هوية غاز معتمدة، ثم N05-I بتحقق مستقل جديد؛ يبقى G3 حاكمًا لأي اعتماد ميداني، وإحماء MQ/Bosch مؤجلًا، وقرارات الحركة دون تشغيل.


## 31) أحدث نتيجة علمية حاكمة — Checkpoint 14: اختبارات MQ التجريبية (2026-10-29)

بناءً على طلب إعادة الاختبار على قراءات حساسات **MQ فعلية لمواد وتراكيز مختلفة**، نُفِّذ الاختبار [CI 40962025285](https://github.com/HazRob/HazRob-Tank/actions/runs/40962025285) بنجاح على **مصدرين أصليين مُتحققين من SHA256**. قُيِّدت نتائج كل مصدر بمستوى تأهيله الحقيقي:

1. **6,400 قراءة ADC أصلية من طرازات MQ** لتمييز NoGas وPerfume وSmoke وMixture (ليست غازات نقية ولا يوجد ppm): خمسة قطاعات حجب لكل فئة مع حاجز 20 صفًا مجاورًا، وعدم تركيب قيم MQ4/MQ9 المفقودة. دقة balanced accuracy لـ MQ135 وحده **66.84%**، ولستة MQ مشتركة مع Tank **74.66%**، ولمصفوفة المصدر السباعية **74.92%**. تصنيف NoGas/Perfume ضعيف؛ لا استنتاج لهوية مادة كيميائية.
2. **90 تعرّضًا أصليًا من MQ135 وحده** لست غازات نقية عند **50/100/200 ppb**؛ الحجب الكامل لمستوى التركيز. متوسط دقة التعرف المتوازنة **38.89%**. تقدير التركيز مع إعطاء اسم الغاز الحقيقي للخوارزمية **مسبقًا** أعطى وسيط خطأ مطلق **54.99 ppb**، مع **60/90** تقدير استقرائي خارج مجال الاستجابة المرجعية؛ ومن ثم لا يجوز اعتبار القيم صالحة لقياسات ppm ميدانية. الوسط الحسابي للخطأ المطلق ارتفع إلى نحو **3075 ppb** بسبب استقراء شديد غير مستقر.

المرجع الكامل، مصفوفة الالتباس، جدول كل مادة/تركيز، ملف التنبؤات، توثيق SHA، وحدود التأهيل محفوظ في [Checkpoint 14](V6-CHEMCORE-NEW-ENGINE-CHECKPOINT-14-REAL-MQ-2026-10-29_AR.md) و`research/chemcore_v6/results/real_mq_gas_identification_concentration_holdout_2026-10-29.json`، وحزمة ZIP من نتائج CI محفوظة في المكتبة.

**الحدود الحاكمة:** تجربة المصدر متعددة MQ لا تتضمن تراكيز ppm أصلًا؛ مجموعة UCI1081 عند ppb تضم فقط MQ135 من حساسات Tank، وتركيزاتها أدنى من مجال الكشف الموثوق. دراستا WANG 2022 وMEI 2025 مناسبتان لإعادة الاختبار على MQ متعددة مع تراكيز 10–100 ppm لكن البيانات الرقمية الأولية **غير محفوظة محليًا** حسب جرد المصادر. لا تركيب بيانات ولا إدراج منحنيات المصنع المحسوبة بوصفها قياسات مستقلة.

**الحالة التقنية لم تتغير بسبب هذه التجربة:** 78/80 تقنية بتنفيذ بحثي جزئي، 2 غير منقولتين، 0/80 G3، ولا اعتماد ppm أو هوية ميدانية. اختبار Open-set N05-H لم يجتز بوابة الأمان. محرك الأغذية منفصل.
