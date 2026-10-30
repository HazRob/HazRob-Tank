# HazRob Tank V6 — إكمال أرقام CAS والكتل المولية للمواد الفردية بمراجع NIST SRD69
**تاريخ التحرير:** 2026-10-30  
**فرع التطوير:** `feature/tank-v6-implementation`  
**حالة الإنتاج:** `NO_RELEASE`

## ملخص التدقيق

تحتفظ قاعدة `SQLite V14` بـ **734 سجل UID**. قبل هذه الدفعة كان **671** سجلًا يملك CAS مباشرًا و**27** سجلًا تاريخيًا يملك CAS مطابقًا عبر جدول مرادفات NIOSH الموثق، بإجمالي **698** سجلًا ذي CAS مباشر أو مرتبط بمصدر. كانت هناك 27 مادة فردية لا تملك CAS، وتسع تسميات لا تمثل مادة نقية واحدة (LPG/CNG/Natural Gas/Town Gas/Smoke/TVOC/NOx/LEL/R410A).

تم التحقق من **الـ27 مادة الفردية كلها** مقابل صفحات الهوية الأساسية في `NIST Chemistry WebBook, Standard Reference Database 69` وحُفظ رقم CAS والكتلة المولية ومعرّف السجل العلمي ورابط NIST لكل مادة. كل قيمة CAS تخضع لخوارزمية رقم التحقق، ومطابقة UID والاسم والصيغة و`legacy_id`، وعدم الاصطدام بمرجع غير مؤهل.

### سجل مصدر كل إضافة

| UID | اسم المادة الأصلي | الصيغة | CAS المسند | الكتلة المولية g/mol | رابط المصدر |
|---:|---|---|---|---:|---|
| 38 | Carbonyl Sulfide | COS | 463-58-1 | 60.075 | [NIST SRD69](https://webbook.nist.gov/cgi/cbook.cgi?ID=463-58-1) |
| 41 | Dimethyl Sulfide | C2H6S | 75-18-3 | 62.134 | [NIST SRD69](https://webbook.nist.gov/cgi/cbook.cgi?ID=75-18-3) |
| 42 | Dimethyl Disulfide | C2H6S2 | 624-92-0 | 94.199 | [NIST SRD69](https://webbook.nist.gov/cgi/cbook.cgi?ID=624-92-0) |
| 45 | Tungsten Hexafluoride | WF6 | 7783-82-6 | 297.83 | [NIST SRD69](https://webbook.nist.gov/cgi/cbook.cgi?ID=7783-82-6) |
| 46 | Silicon Tetrafluoride | SiF4 | 7783-61-1 | 104.0791 | [NIST SRD69](https://webbook.nist.gov/cgi/cbook.cgi?ID=7783-61-1) |
| 47 | Titanium Tetrachloride | TiCl4 | 7550-45-0 | 189.679 | [NIST SRD69](https://webbook.nist.gov/cgi/cbook.cgi?ID=7550-45-0) |
| 53 | Aniline | C6H7N | 62-53-3 | 93.1265 | [NIST SRD69](https://webbook.nist.gov/cgi/cbook.cgi?ID=62-53-3) |
| 57 | Propionaldehyde | C3H6O | 123-38-6 | 58.0791 | [NIST SRD69](https://webbook.nist.gov/cgi/cbook.cgi?ID=123-38-6) |
| 58 | Butyraldehyde | C4H8O | 123-72-8 | 72.1057 | [NIST SRD69](https://webbook.nist.gov/cgi/cbook.cgi?ID=123-72-8) |
| 76 | Hydrogen | H2 | 1333-74-0 | 2.01588 | [NIST SRD69](https://webbook.nist.gov/cgi/cbook.cgi?ID=1333-74-0) |
| 78 | Ethane | C2H6 | 74-84-0 | 30.0690 | [NIST SRD69](https://webbook.nist.gov/cgi/cbook.cgi?ID=74-84-0) |
| 83 | Isopentane | C5H12 | 78-78-4 | 72.1488 | [NIST SRD69](https://webbook.nist.gov/cgi/cbook.cgi?ID=78-78-4) |
| 84 | Neopentane | C5H12 | 463-82-1 | 72.1488 | [NIST SRD69](https://webbook.nist.gov/cgi/cbook.cgi?ID=463-82-1) |
| 89 | n-Decane | C10H22 | 124-18-5 | 142.2817 | [NIST SRD69](https://webbook.nist.gov/cgi/cbook.cgi?ID=124-18-5) |
| 90 | Ethylene | C2H4 | 74-85-1 | 28.0532 | [NIST SRD69](https://webbook.nist.gov/cgi/cbook.cgi?ID=74-85-1) |
| 91 | Propylene | C3H6 | 115-07-1 | 42.0797 | [NIST SRD69](https://webbook.nist.gov/cgi/cbook.cgi?ID=115-07-1) |
| 93 | Cyclopropane | C3H6 | 75-19-4 | 42.0797 | [NIST SRD69](https://webbook.nist.gov/cgi/cbook.cgi?ID=75-19-4) |
| 121 | MTBE | C5H12O | 1634-04-4 | 88.1482 | [NIST SRD69](https://webbook.nist.gov/cgi/cbook.cgi?ID=1634-04-4) |
| 124 | Difluoromethane / R32 | CH2F2 | 75-10-5 | 52.0234 | [NIST SRD69](https://webbook.nist.gov/cgi/cbook.cgi?ID=75-10-5) |
| 126 | R134a | C2H2F4 | 811-97-2 | 102.0309 | [NIST SRD69](https://webbook.nist.gov/cgi/cbook.cgi?ID=811-97-2) |
| 127 | R125 | C2HF5 | 354-33-6 | 120.0214 | [NIST SRD69](https://webbook.nist.gov/cgi/cbook.cgi?ID=354-33-6) |
| 128 | R143a | C2H3F3 | 420-46-2 | 84.0404 | [NIST SRD69](https://webbook.nist.gov/cgi/cbook.cgi?ID=420-46-2) |
| 129 | R152a | C2H4F2 | 75-37-6 | 66.0500 | [NIST SRD69](https://webbook.nist.gov/cgi/cbook.cgi?ID=75-37-6) |
| 131 | R1234yf | C3H2F4 | 754-12-1 | 114.0416 | [NIST SRD69](https://webbook.nist.gov/cgi/cbook.cgi?ID=754-12-1) |
| 132 | R1234ze(E) | C3H2F4 | 29118-24-9 | 114.0416 | [NIST SRD69](https://webbook.nist.gov/cgi/cbook.cgi?ID=29118-24-9) |
| 133 | Carbon Tetrafluoride | CF4 | 75-73-0 | 88.0043 | [NIST SRD69](https://webbook.nist.gov/cgi/cbook.cgi?ID=75-73-0) |
| 134 | Hexafluoroethane | C2F6 | 76-16-4 | 138.0118 | [NIST SRD69](https://webbook.nist.gov/cgi/cbook.cgi?ID=76-16-4) |

**استثناء مدروس:** السجل التاريخي `UID=53 Aniline` هو مادة الأنيلين المفردة ذات CAS **62-53-3**؛ بينما سجل NIOSH الأصلي `UID=486 Aniline (and homologs)` أوسع نطاقًا ويستعمل نفس CAS للمادة الأساسية في مجموعة التعرض. يسمح فحص NIST بإسناد CAS للأنيلين المفرد بعد التأكد من السجل المرجعي الآخر، **دون دمج أو تعديل معرّفي UID أو نسخ حدود التعرض الخاصة بالمجموعة إلى سجل الأنيلين الفردي**. لا يجري تعميم الاستثناء على أي زوج آخر.

## آلية التكامل

- `ChemicalNistIdentityCompletionV6.ensure` يعمل بعد استيراد NIOSH الأساسي وقبل استيراد حدود OSHA/IDLH، بعملية SQLite واحدة، ويعدّل **فقط** `chemicals.cas` أو `chemicals.molar_mass` إذا كان الحقل فارغًا.
- يخزن رقم CAS مع `source_id=NIST_WEBBOOK` في `chemical_identifiers`، ورابط NIST لكل UID في `chemical_sources`.
- عند إعادة تشغيل التطبيق، لا يعيد ملء القيم أو تغييرها، ويعتمد على سجل مصدر NIST الثابت حتى إذا وسعت استيرادات OSHA/NIOSH نص `record_source` بعد التشغيل الأول.
- ينقل `ChemicalRepository.scientificProfile(uid)` مرجع NIST عبر `nist_direct_cas_source`، ويوضح تقرير المادة أنه **دليل هوية وكتلة جزيئية، وليس قياسًا للتركيز أو معايرة R0**.
- تبقى التسع التسميات المتعددة المكونات أو الفئوية بلا CAS مفرد، ويوثق `chemical_class` سبب عدم انطباق CAS.

## مخرجات التغطية المستهدفة بعد اجتياز اختبار Android الفعلي

إذا أقر الاختبار الواقعي على Android 12 جميع القيود:
- CAS مباشر: **698/734**.
- CAS مرتبط موثق بمرادف NIOSH: **27/734**.
- مجموع السجلات ذات CAS مباشر أو مرتبط: **725/734**.
- تسميات لا ينطبق عليها CAS واحد: **9/734**.
- مواد فردية متبقية بلا CAS مصدر مؤهل: **0**.
- صيغ جزيئية: **668/734** (الصيغ غير المنطبقة على مجموعات/خلائط تبقى فارغة).
- كتل مولية: **637/734** بعد إضافة 27 مرجع NIST إلى 610 سابقة.

**لا تعتبر الأعداد المستهدفة أعلاه اعتمادًا نهائيًا حتى ظهور `PASS` من محاكي Android API31 V14 وجرد CSV الفعلي.** لا يعني اكتمال CAS اكتمال حدود التعرض أو GHS أو معايرة MQ أو G3. حكم الغاز الميداني يبقى غير معتمد إلى حين الأدلة الفيزيائية.

## مسار الاختبارات والاستئناف

الاختبارات المصدرية: `test_v6_nist_cas_complete.py`، والفعلية `V6ScientificRiskDbFixtureActivity.java` و`run_n09_scientific_risk_db.sh` التي تتحقق من 27 إدخالًا، عدم الكتابة في السجلات القديمة، إعادة تشغيل الاستيراد دون تغييرات، عدم خلط المصادِر، و734 UID ثابتًا.

بعد إثبات `PASS`، حدّث `docs/implementation/V6-ANDROID-SOFTWARE-ACCEPTANCE.json` ثم واصل القبول A01–A14. لا تطلب موافقة إنتاج APK/فيرموير ما لم تستوف الخطة كلها ويوثق القرار بإذن المالك.
