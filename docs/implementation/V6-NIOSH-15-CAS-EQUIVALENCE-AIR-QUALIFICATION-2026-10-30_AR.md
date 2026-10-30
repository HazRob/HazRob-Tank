# HazRob Tank V6 — ربط 15 مرادفًا كيميائيًا مع CAS/NIOSH والتأهيل الصارم للهواء الطبيعي
**التاريخ:** 2026-10-30
**المستودع:** HazRob/HazRob-Tank / `feature/tank-v6-implementation`
**قرار الإنتاج:** `NO_RELEASE`.

## 1. التمييز بين السجل وهوية المادة

الجدول الأساسي **734 UID** جميعها فريدة كمعرّفات، لكنه يحتوي على أسماء مترادفة لبعض المواد نفسها. عدم تكرار UID لا يثبت عدم تكرار المادة. لإزالة الالتباس دون فقدان أي تجربة أو جلسة قديمة، أُنشئ جدول `chemical_identity_equivalence_v6` الذي يسجل علاقة "اسم تاريخي ↔ اسم NIOSH معتمد" ذات أصل محدد، ولا يزيل صفًا من `chemicals` أو يغيّر FK ولا ينقل حدود التعرض إلى تجربة قديمة.

### علاقات تم التحقق منها على SQLite Android 12

| UID القديم | اسم HazRob التاريخي | UID المرجعي | الاسم المرجعي NIOSH | CAS | المصدر |
|---:|---|---:|---|---|---|
| 9 | Germane | 615 | Germanium tetrahydride | 7782-65-2 | [NIOSH](https://www.cdc.gov/niosh/npg/npgd0300.html) |
| 63 | Chloromethane | 519 | Methyl chloride | 74-87-3 | [NIOSH](https://www.cdc.gov/niosh/npg/npgd0403.html) |
| 64 | Dichloromethane | 557 | Methylene chloride | 75-09-2 | [NIOSH](https://www.cdc.gov/niosh/npg/npgd0414.html) |
| 67 | 1,2-Dichloroethane | 181 | Ethylene dichloride | 107-06-2 | [NIOSH](https://www.cdc.gov/niosh/npg/npgd0271.html) |
| 71 | Chloroethane | 552 | Ethyl chloride | 75-00-3 | [NIOSH](https://www.cdc.gov/niosh/npg/npgd0267.html) |
| 97 | Ethylbenzene | 149 | Ethyl benzene | 100-41-4 | [NIOSH](https://www.cdc.gov/niosh/npg/npgd0264.html) |
| 103 | Methanol | 503 | Methyl alcohol | 67-56-1 | [NIOSH](https://www.cdc.gov/niosh/npg/npgd0397.html) |
| 104 | Ethanol | 499 | Ethyl alcohol | 64-17-5 | [NIOSH](https://www.cdc.gov/niosh/npg/npgd0262.html) |
| 105 | 1-Propanol | 514 | n-Propyl alcohol | 71-23-8 | [NIOSH](https://www.cdc.gov/niosh/npg/npgd0533.html) |
| 106 | Isopropanol | 504 | Isopropyl alcohol | 67-63-0 | [NIOSH](https://www.cdc.gov/niosh/npg/npgd0359.html) |
| 107 | 1-Butanol | 515 | n-Butyl alcohol | 71-36-3 | [NIOSH](https://www.cdc.gov/niosh/npg/npgd0076.html) |
| 108 | 2-Butanol | 631 | sec-Butyl alcohol | 78-92-2 | [NIOSH](https://www.cdc.gov/niosh/npg/npgd0077.html) |
| 109 | Isobutanol | 629 | Isobutyl alcohol | 78-83-1 | [NIOSH](https://www.cdc.gov/niosh/npg/npgd0352.html) |
| 110 | tert-Butanol | 572 | tert-Butyl alcohol | 75-65-0 | [NIOSH](https://www.cdc.gov/niosh/npg/npgd0078.html) |
| 123 | 1,4-Dioxane | 280 | Dioxane | 123-91-1 | [NIOSH](https://www.cdc.gov/niosh/npg/npgd0237.html) |

تُرفض علاقة التطابق تلقائيًا إذا كان CAS غير صالح حسابيًا، أو لم يطابق الاسم الأصلي/الاسم المرجعي النص المثبت، أو تعدد مصدر CAS، أو لم يوجد رابط `chemical_sources` الرسمي لـ`NIOSH_NPG`. في حالة نجاحها تُعرض في `ChemicalRepository.scientificProfile(uid)` وواجهة تفاصيل المادة باعتبارها دليل هوية لا دليل كشف غاز.

- **التغطية:** 671 مادة لديها CAS مخزن مباشرة، و15 سجلًا آخر رُبط باسم ذي CAS مطابق وموثق، أي **686 سجلًا له CAS مباشر أو دليل ارتباط بمصدر CAS**، مع بقاء **48 سجلًا** غير محسوم المصدر/الانطباق. منها 9 تسميات لخلائط/فئات (`LPG,CNG,Natural Gas,Town Gas,Smoke,TVOC,NOx,LEL,R410A`) لا يصح اختراع CAS واحد لها؛ ويظل **39 سجلًا فرديًا** يحتاج فحصًا مرجعيًا.
- ليس الرقم 686 عدد مواد كيميائية متميزة؛ فهو تغطية CAS على مستوى السجلات قبل حل بقية المرادفات. **734−15=719 سجلاً بعد تجاهل المرادفات الـ15 المعروفة فقط**، ولا يدّعي أنه العدد النهائي للكيانات الفريدة.
- المصدر الرسمي لـCAS/الترادف هو NIOSH Pocket Guide الأصلي؛ لا يُستنتج CAS من تشابه الصيغ وحده، ولا تُدمج "Aniline (and homologs)" مع Aniline المفرد تلقائيًا.

**الدليل الآلي الفعلي:** Android 12 API31 [41041368422](https://github.com/HazRob/HazRob-Tank/actions/runs/41041368422) = PASS، 15/15 رابطة، 0 مرفوضة، والاستيراد الثاني 0 تغييرات، 130 فحصًا، مع ثبات العدد 734. الاختبار التصنيفي [41041411111](https://github.com/HazRob/HazRob-Tank/actions/runs/41041411111) PASS؛ والتجميع [41041572382](https://github.com/HazRob/HazRob-Tank/actions/runs/41041572382) PASS.

## 2. تصحيح عدم التباس وصف الهواء الطبيعي مع قيمة سلامة مهنية

كُشف في `v14_unified_chemical_catalog` أن اختبار `background_range_count>0` قد يمنح وصف `SOURCE_QUALIFIED_ENVIRONMENTAL_RANGE` لنطاق الأكسجين في رطوبة جوية نموذجية مصدره `CONTEXT_SPECIFIC_DESCRIPTIVE_NOT_SAFETY`. جرى تعديل SQL ليقبل هذا التصنيف **فقط** إذا كان يوجد `chemical_background_ranges` بوسم `qualification_status='SOURCE_QUALIFIED'` ومع حد أدنى وأقصى معتبرين.

- نطاق NOAA للأكسجين 20.11 إلى 20.947 بالمئة تركيز حجمي هو **بيان وصفي مشروط**، وليس حدًا للصحة المهنية أو تصريحًا بأن الغاز طبيعي/آمن في أي موقع.
- حدود OSHA لنقص الأكسجين أو زيادته منفصلة عن وصف الهواء الطبيعي، كما أن ارتفاع نسب MQ أو هبوطها لا يحول المصدر إلى شهادة تركيز.
- نظرًا لأن قاعدة البيانات غير مزودة بحدود طبيعية ذات مصدر مؤهل بالمعنى الصارم، فنتيجة العرض الطبيعي الآن `NO_QUALIFIED_NATURAL_RANGE`، لا صفر ولا `SAFE`.
- تم تحديث اختبار ترحيل V13→V14 ليدخل نطاقًا وصفيًا مرفوضًا ثم نطاقًا `SOURCE_QUALIFIED` افتراضيًا للتأكد من الانتقال الصحيح؛ واستكمال نموذج V12 المصدر التجريبي بحقل `qualification_status` الموجود في SQLite الحقيقي.
- الاختبار الرقمي لعملية الترحيل [41042184182](https://github.com/HazRob/HazRob-Tank/actions/runs/41042184182) PASS، و[41042184234](https://github.com/HazRob/HazRob-Tank/actions/runs/41042184234) scientific source PASS. اختبار Android للمخزون الكيميائي يجري مراجعته بتاريخ التقرير ويُسجل برقم التشغيل الذي يجتاز فعلًا.

## 3. الحدود والمهام التالية

- البيانات الجزيئية: 668 صيغة و610 كتلة مولية/734 بعد إسقاط PubChem المصدر مع بقاء الأصل.
- لا يزال بحث المواد عبر كل واجهات التطبيق والمرشحين/القياس وتوحيد العرض النهائي للمرادفات يحتاج فحصًا؛ ربط هوية لا يعني بعد إزالة كل صف مترادف من كل قائمة. حقل CAS الأصلي للسجل التاريخي لا يُملأ بقيمة ثانوية عشوائية.
- بقي اختبار نشر مصدر Stable الحقيقي على GitHub وتحميله والتحقق من التوقيع وترقية APK Android 12 وفحص تبويبات الجلسات الواقعية.
- محرك ChemCore الفعلي G3=0/80. **لا ترخيص إنذار غاز مهني أو ppm مؤكد**.
- لا يطلب إذن APK + firmware V6 حتى إغلاق A01–A14، ثم يُعرض ملخص قرار الإغلاق ويطلب إذن المالك الصريح.

## الاستئناف

راجع `docs/implementation/V6-ANDROID-SOFTWARE-ACCEPTANCE.json` مع هذا الملف وملف `V6-CATALOG-ENRICHMENT-AND-RELEASE-CHECKPOINT-2026-10-30_AR.md`. لا تعتبر الاختبارات على commits أقدم دليلاً لملفات جديدة دون فحص أثر التعديلات. حالة الإصدار يجب أن تبقى `NO_RELEASE`.

## تحديث توسعة المرادفات الرسمية — نقاط التحقق التاريخية 21 ثم 27

- ثُبتت الدفعة الثانية بستة أسماء ترادفية رسمية: `Methanethiol ↔ Methyl mercaptan` (CAS 74-93-1)، `Ethanethiol ↔ Ethyl mercaptan` (75-08-1)، `1,2-Dichloropropane ↔ Propylene dichloride` (78-87-5)، `MEK ↔ 2-Butanone` (78-93-3)، `Diethyl Ether ↔ Ethyl ether` (60-29-7)، `R22 ↔ Chlorodifluoromethane` (75-45-6). تحققت على محاكي Android 12 في [CI 41044188586](https://github.com/HazRob/HazRob-Tank/actions/runs/41044188586) PASS: **21 علاقة/0 مرفوضة**، **692 سجلًا له CAS مباشر أو رابط**، **33 فرديًا مجهول CAS**، و9 تسميات غير مفردة.
- أُدرجت الدفعة الثالثة ذات الستة أدلة NIOSH الأصلية: `Silane ↔ Silicon tetrahydride` (7803-62-5)، `n-Octane ↔ Octane` (111-65-9)، `n-Nonane ↔ Nonane` (111-84-2)، `Mesitylene ↔ 1,3,5-Trimethylbenzene` (108-67-8)، `MIBK ↔ Hexone` (108-10-1)، `Propyl Acetate ↔ n-Propyl acetate` (109-60-4). لها صفوف موثقة بمصدر CAS في أرشيف NIOSH الأصلي المضمن، ولا تُخترع قيم مقاومة أو إنذار. مصدر الشيفرة [commit 137d8aadd474d2f30b2710310a92afb3efe3df53](https://github.com/HazRob/HazRob-Tank/commit/137d8aadd474d2f30b2710310a92afb3efe3df53).
- النتائج المتوقع التحقق منها في اختبار Android N09 الأخير: **27 مرادفًا، 698 CAS مباشر/مرتبط، 36 بدون CAS منها 9 فئات و27 مادة فردية**. إلى حين اكتمال [N09 CI 41044802357](https://github.com/HazRob/HazRob-Tank/actions/runs/41044802357) لا تُدرج النتيجة بوصفها قبولًا على الجهاز. اجتاز تجميع Android [41044748497](https://github.com/HazRob/HazRob-Tank/actions/runs/41044748497) وCI المصدر العلمي [41044830613](https://github.com/HazRob/HazRob-Tank/actions/runs/41044830613) بنجاح.
- تبقى أي قضية أسماء لم تتوفر لها مرجعية CAS أصلية أو خليط حقيقي من مصدر مستقل `UNKNOWN`، مع الحفاظ على 734 UID، دون تغيير معرّفات تقارير الجلسات.

### نتيجة الاختبار النهائية للدفعة الثالثة (بعد تدوين التوقعات)

- [اختبار Android 12 الفعلي `41044802357`](https://github.com/HazRob/HazRob-Tank/actions/runs/41044802357) = **PASS** على commit `c9c60ca73ea2997ab27c2f79060bd30842bc0556`، 27/27 علاقة NIOSH سليمة، 0 مرفوضة، إعادة الحفظ `inserted=0`، 734 UID بلا تكرار، CAS مباشر 671، CAS مستند إلى مرادف NIOSH 27، **المجموع 698**، المتبقي 36 (9 غير فردية + 27 مواد فردية). 668 صيغة جزيئية و610 كتل مولية، وحقل الأسماء العربية لم يُدعَ أنه مُستكمل.
- [CI البرمجيات العلمية `41044830613`](https://github.com/HazRob/HazRob-Tank/actions/runs/41044830613) = **PASS**؛ [تجميع Android `41044748497`](https://github.com/HazRob/HazRob-Tank/actions/runs/41044748497) = **PASS** (دون APK إنتاج).
- الملفات الثلاثة الناتجة من اختبار المحاكي محفوظة في artifacts المرتبطة برقم التشغيل: `v6_chemical_catalog_coverage.csv` (734 سجلًا مع حقول CAS المربوطة)، `n09_scientific_risk_db.json`، `n09_ghs_report.pdf`.
- تم تحديث سجل القبول البرمجي الرئيسي دون رفع أعلام القبول أو إذن الإنتاج. الحالة باقية `NO_RELEASE` لأن الفحص النهائي للواجهة وتحديث Stable والتوقيع الفيزيائي/الميداني لم يغلق.
