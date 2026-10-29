# HazRob Tank V6-002 — تنفيذ الدفعة الأولى ونقطة الاستئناف

**الوقت:** 2026-10-29 (Asia/Aden) · **المستودع:** `HazRob/HazRob-Tank` · **الفرع:** `feature/tank-v6-implementation`. هذه الوثيقة تلخص تنفيذًا فعليًا، مع فصل الاختبارات المنفذة عن الأعمال المتبقية.

## 1. خط أساس التنفيذ المحمي
- **V5 frozen parent:** `c132fc2f8eb7a54d5b3c96b4fdcd6090096d1a5b`.
- **Inherited read-only analytical source:** `sohpany/SERVOC-Android@6281d3eb7c1c961ce63b0b5e4ae0792874626720`.
- **Worktree/source branch:** `feature/tank-v6-implementation`. لم يتم دمج هذا الفرع في V5 ولم يتغير firmware، ولا نُشرت APK V6.
- **Snapshot manifest:** `research/chemcore_v6/source_manifest.json`، commit `269bc84d7bd7771ad22f29a1732737d8c3ec1a7e`.

## 2. مراجعة المصادر وتثبيتها (P01 — جزئي)
- جلب ونسخ **14/14 أصلًا تاريخيًا علميًا** من `HazRob/build-harness/library/research/hr069/` إلى `research/chemcore_v6/source_snapshots/hr069/`. قارنت sha1 Git blob لكل ملف مع `current-manifest.json` الأصلي، و**14/14 مطابق** و0 ملفات إضافية.
- جلب **3/3 ملفات** مراجعة R0 منشورة، مع pin ثابت لكل ملف إلى `research/chemcore_v6/source_snapshots/r0_peers/`:
  - `MQ_R0_RS_CROSS_STUDY_2026-10-21.json` blob `03198302d478c9f54cf84a79b5bdec1b600612ca`;
  - `MQ_R0_ADDITIONAL_5_SOURCES_2026-10-21.json` blob `34b3cbc64cbc881444fbeefa54fda92448cbedfb`;
  - `mq_r0_rs_peer_sources_2026-10-21.json` blob `8565d6f2d9c27bf459d3f63f4350b4b19d9ecf3b`.
- فهرست 80 تقنية/76 نموذجًا/80 مرجعًا وسجلات 43 منحنى/8 MQ calibrator factors في مصفوفات V6 السابقة؛ فحص metadata فقط لبعض أرشيفات المراجع، **لا ادعاء تدقيق raw لجميع 80**.

## 3. أعمال البرمجة المنجزة
- `research/chemcore_v6/source_audit.py`، commit `b43223ef4a4492479f11e5aaef65c9a722d9c3b7`: التحقق من سلامة الأصول بصمات Git، معادلة المقاومة الكهربائية بوحدات أوم، `R0=Rs_air/k_air` المشروط، اتجاه `Rs/R0` مقابل `R0/Rs`، رفض اختلاف تعريف R0 أو مجال المنحنى، قراءة 43 منحنى بحثيًا، التحقق من بصمة ستة حساسات مشتركة دون اختلاق MQ4/MQ9، وتوليد counterfactual R0 أرقامًا بحثية فقط.
- `research/chemcore_v6/tests/test_source_audit.py`، commit `10d85334eca3cedf2a6d482dd4f2bf5d3760cb64`: **23 اختبارًا**. أول CI فشل بسبب خطأ rounding عند endpoint المصدر؛ وثق وأصلح في `c4277c5e6e10849ea3b0b64e128693332bf760f2` دون تغيير الحدود/المصدر.
- `research/chemcore_v6/peer_r0_audit.py` commit `889ee27944a36d0f81e7fed5a96716af0bdd2bdd`: حساب (Rs_{air}/ratioAir) من بيانات الدراسة المنشورة؛ ترصد تعارض قيم MQ7 وMQ8 مع حسابها.
- `research/chemcore_v6/tests/test_peer_r0_audit.py` commit `d57240300ccd7a14f02ba3bb5e627208915d0080`: **7 اختبارات** إضافية لصحة المصدر ورفض أي تغيير غير موثق.
- `.github/workflows/chemcore-v6-scientific-ci.yml` commit `989ffdf8b7b71baa378994e1072b21f372a83db0`: CI على branch V6 فقط، Python 3.11، 30 unit tests، 3 JSON audit artifacts محفوظة.

## 4. نتائج مثبتة بتشغيل GitHub CI

- **Run:** [40911664273](https://github.com/HazRob/HazRob-Tank/actions/runs/40911664273) · **SHA:** `989ffdf8b7b71baa378994e1072b21f372a83db0` · **Conclusion:** `success`.
- **Unit tests:** **30/30 PASS**. Source verification **14/14**, 3 peer-paper sources pinned.
- **Curve roundtrips:** 43 relations × 9 concentrations each = **387 successful forward/inverse numerical checks**؛ max relative error `3.311559369998569e-15`. هذا هو self-consistency لنفس الدالة، وليس خطأ PPM في المختبر.
- **R0 counterfactuals:** 43 × 4 multipliers = **172 synthetic cases**؛ **94** ينتج عنها تركيز خارج نطاق المصدر، وتم رفض الاستقراء بصورة صريحة (not measured error).
- **Peer-paper checks:** 2018 study nine models including 8 Tank model names؛ discrepancy at **MQ7 and MQ8**، وعدم نقل R0 من جهاز آخر.
- **Artifact:** `chemcore-v6-source-research-audit`، ID `12914830258`، ZIP SHA256 `be3e9e4c97ca4302cbab5bd63609235854902864fa4c19b0293b1dc2dcef8939`.
- **Source code:** `research/chemcore_v6/source_audit.py`, `peer_r0_audit.py`, test files. No Tank gas/certified sensor experiment was performed.

## 5. أول تجربة فعلية على بيانات باحثين: UCI1081 (P05 — جزئي)

**تشغيل محلي حقيقي في هذه المهمة** باستخدام ZIP UCI1081 الخام من مكتبة الملفات:
- ZIP SHA256 `813ebb7d00ed9172114b68d27ab3210d6c220b8b23907dce91e9838cd48ab`; `gsalc.csv` SHA256 `d79afe6494903b988de68d4a8ab133167151a1bb620ba413e5cefd15003d07b3`.
- **90 تعرضًا**، 6 غازات ×3 مستويات (50/100/200 ppb) ×5 تكرارات، 10 قنوات (MQ135 فقط مشترك مع Tank).
- الميزات: استجابة source voltage 3 نوافذ زمنية مقسومة على baseline، بدون اختلاق Rs أو R0.
- اختبار `leave-one-entire-ppm-level-out` — **المصدر بوحدة ppb**، 30 تعرضًا محجوبًا ×3 طيات؛ كل fold يعمل training pipeline scaler داخل جزء التدريب فقط.
- النتائج balanced accuracy المتوسط: MQ135/LogReg **56.67%**؛ MQ135/LDA **50%**؛ 10 قنوات/LogReg **60.00%**؛ 10 قنوات/LDA **62.22%**. التفاصيل والأخطاء في `docs/research/V6-CHEMCORE-UCI1081-REAL-DATA-PILOT-2026-10-29_AR.md`.
- شيفرة إعادة التنفيذ `research/chemcore_v6/benchmarks/uci1081_v6_benchmark.py`، commit `0345ac430ecb7455554937d2127b6b0055f15f7a`. **لم يُعاد تشغيل التدريب عبر GitHub CI لأن الأرشيف الخام غير مدرج في المستودع**. التشغيل الفعلي تم محليًا، وملف النتائج الكاملة متاح من مرفق المحادثة. كانت المعالجة مع scikit-learn 1.8.0.

**لا يمكن** الاستنتاج أن دقة Tank V6 هي أي من هذه النسب؛ كلها تجارب على مصدر خارج الجهاز لم تُثبَت فيه independence/day/device وطبقة الغاز ppm لجهازنا.

## 6. حالة بوابات المراحل في هذه النقطة

| المرحلة | الحالة المنفذة | المتبقي |
|---|---|---|
| P00 | source baseline pins and branch comparison done | exact V5 device replay / APK behavioural tests |
| P01 | 14 HR069 +3 peer raw files pin/verify, 80/76/80 cataloged | all remaining raw source archive qualification/dedup |
| P02 | digital R0 contracts; peer arithmetic checks | physical RL, ADS, source variant, R0 session/T/RH, heater |
| P03 | planned schema only | actual Android/SQLite facade and migration tests |
| P04 | basic forward/inverse + sensitivity SIM-A run (387/172) | remainder of 31 SIM scenarios, environment/time/mix |
| P05 | one original external 90-exposure UCI1081 pilot | UCI270/308/309, other eligible MQ/biophysical raw datasets, cross-device/day splits |
| P06–P11 | not started | code model selection/fusion/Android G3/release |

## 7. تعليمات الاستئناف الدقيقة

1. اقرأ هذا checkpoint و`docs/implementation/V6-CHEMCORE-EXECUTION-TRACKER.json` والخطة الرئيسية.
2. احصل على branch `feature/tank-v6-implementation`، تحقق أن source manifest يحتوي 14 ملفًا + 3 peer snapshots ونتيجة CI `40911664273`.
3. شغّل `python -m unittest discover -s research/chemcore_v6/tests -v`؛ ثم `python research/chemcore_v6/source_audit.py --output ... --counterfactual-output ...`؛ ثم `python research/chemcore_v6/peer_r0_audit.py`.
4. UCI1081: استخدم ملف source ZIP **بنفس SHA** مع `python research/chemcore_v6/benchmarks/uci1081_v6_benchmark.py --archive <path_to_original.zip> --out <report.json>`؛ لا تستخدم CSV بعد تحويله إلى بيانات أخرى دون ثبات schema.
5. **الخطوة التالية:** تكملة P01/QC عبر المصادر المتاحة، بدء مصدر UCI270/308/309 مع حفظ SHA والتقسيم وحقول provenance؛ ثم تطوير baseline V5 replay وAndroid facade بعد وثيقة ADR.
6. أوثق `commit SHA`، `run ID`، `actual test PASS/FAIL` ومخرجات البيانات عند كل خطوة، ولا تغيّر حالة G3 تلقائيًا.

**Final research checkpoint:** `SOURCE_14_OF_14_VERIFIED / PEER_R0_3_OF_3 / RESEARCH_CI_30_OF_30_PASS / CURVE_ROUNDTRIP_387 / SYNTH_R0_172 / REAL_EXTERNAL_UCI1081_90 / G3_TANK_0_OF_80 / ANDROID_CHEMCORE_V6_NOT_INTEGRATED / APK_V6_NOT_BUILT`.

## 8. إضافة تنفيذية لاحقة: بوابة Android Java لتوافق R0 والمصدر

- **شيفرة فعلية:** `android/app/src/main/java/com/servoc/app/chemcore/TankV6SourceCompatibility.java`, commit `714541f37eb6e29f562bb3433f547b19089e9ef5`, ثم تصحيح fail-closed `1ca5a8c4df23b327b98e62cab7ccfbf8f6cd57a1`. تُعرّف عقد `sensor model/device instance`, `R0 definition`, `ratio direction`, `RL/VC/ADC`, `ppm source domain`, `heater flag` وترفض source غير المتوافق قبل إنتاج حساب كمي. **لا يمنح أي مصدر M4** بالاعتماد على flags دون سجل تحقق G3 مستقل.
- **اختبار JVM مستقل:** `research/chemcore_v6/tests/java/TankV6SourceCompatibilityTest.java`, commit `74f81ef2bb73a97f9a05a7cc61b809b6c963eead`, **31 assertion passed**.
- **CI حديث:** [Run 40912404109](https://github.com/HazRob/HazRob-Tank/actions/runs/40912404109) عند SHA `42732b2cd51b3e1d0567cda686d0291f8b75a909` — **success**: Python 30/30, Java compile & 31 assertions, source curve 387, synthetic R0 172, source peers validated؛ artifact ID `12914686568`, digest `2056f5cb2f995ea0226f5225c65652352df57f63a6626d5de2ecd9b6cb613d06`.
- **حدود الإنجاز:** ملف Java مصدر فعلي على فرع V6، لكنه **غير موصل بالاستدلال الفعلي** في التطبيق ولم يُشغل Gradle Android ولا build APK. لا ترفع حالة P03 إلى `PASS` قبل تكامل البيانات وSQLite والجلسات والتقارير والتجارب.
- **استئناف:** الخطوة التالية P03: تصميم وربط `SourceCurveRegistryV6` بمخرجات V5 على read-only sidecar، ثم اختبارات مقارنة قبل السماح باستبدال أي source selector.
