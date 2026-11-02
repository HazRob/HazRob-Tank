# ChemCore Next — تصنيف المخاطر للفئات العامة داخل الجدول الموحد نفسه

**قرار المستخدم (2026-11-02):** لا تُنشئ مكتبة ثانية أو جدول هويات آخر. احتفظ بكل من LPG والدخان وأكاسيد النيتروجين والكحولات العامة وفئة المواد المحتوية على الأمونيوم داخل مرجع المواد نفسه.

## التصميم المعتمد
- **جدول الهوية الوحيد:** `chemicals`؛ ومرآته `v14_unified_chemical_catalog`. آخر عدّ مؤكد قبل التعديل: 734 هوية فعلية. بعد تشغيل الإضافة على قاعدة تفتقر للفئتين الجديدتين، يتوقع 736 سجلًا. **لا تعتمد رقم 736 حتى ينجح اختبار SQLite**.
- **ثلاث هويات موجودة بأرقامها التاريخية دون استنساخ:** LPG (legacy 135 / UID 136)، Smoke (legacy 139 / UID 140)، NOx (legacy 141 / UID 142). حُفظت `legacy_id` والمراجع واللقطات السابقة.
- **فئتان عامتان تُضافان فقط إذا لم تكونا موجودتين:** `Alcohols (general)` = كحولات (فئة عامة)، و`Ammonium-containing materials (NH4+ source class)` = مواد تحتوي على الأمونيوم (فئة مصادر). لا يُنسب CAS أو صيغة جزيئية أو GHS معتمد لأي منهما دون دليل.
- **جدول المخاطر الموجود مسبقًا:** `chemical_hazard_classifications`؛ يُربط عبر `chemical_uid` إلى الصف نفسه في `chemicals` ويُوثق `source_id/source_url/evidence_status/notes`. يُستخدم `scientific_sources` الموجود، ولا تنشأ أي جداول أو views إضافية.
- التصنيف `SOURCE_QUALIFIED` مخصص لحقائق الفئة المدعومة مباشرة. والتصنيف `PROVISIONAL` يعني احتمال خطر يعتمد على المكون أو الصيغة أو التركيز، **ليس ملصق SDS نهائيًا**. تُبقي `GhsPictograms.allQualified()` الحالية تصنيفات PROVISIONAL خارج الرموز المؤكدة والإنذارات.
- مسار القراءة البحثية `ChemCoreNextGeneralHazardBootstrap.forMasterCatalog(db, uid)` يسترجع حقائق الفئة ومصادرها وحالتها من **جدول المخاطر الموحد نفسه**؛ لا يعتمد أي مكتبة جانبية.

## مصفوفة المخاطر المدخلة في جدول التصنيفات الموحد

| الفئة | الرمز | حالة الدليل | القاعدة |
|---|---|---|---|
| LPG | GHS02 لهب | **SOURCE_QUALIFIED** | موثّق من NIOSH كغاز وقود قابل للاشتعال (خليط) |
| NOx | GHS03 مؤكسد | **PROVISIONAL** | يختلف بحسب أكسيد النيتروجين الفعلي وتركيب الخليط. NO₂ يمكن أن يسرّع الاحتراق، لكن لا يعمَّم على NOx |
| NOx | GHS06 سمية حادة | **PROVISIONAL** | خطر استنشاق NO₂ لا يعني أن كل NOx في الفئة نفسها |
| NOx | GHS05 تآكل | **PROVISIONAL** | خطر احتمالي للمكونات المتفاعلة مثل NO₂، غير مؤهل كتصنيف لكل خليط |
| Smoke | GHS07 مهيج | **PROVISIONAL** | الدخان هباء معقد، وتتغير مكوناته حسب مصدر الاحتراق |
| Alcohols (general) | GHS02 قابل للاشتعال | **PROVISIONAL** | ثبوت اشتعال الإيثانول لا يثبت اشتعال كل مواد عائلة الكحولات |
| Ammonium-containing materials | GHS07 مهيج | **PROVISIONAL** | بعض الأملاح/المحاليل قد تطلق NH₃ وفق الظروف؛ NH4⁺ **ليس غازًا** |

**قرار إضافي:** عدم إضافة GHS04 لـLPG المنفلت في الهواء، لأن رمز الغاز تحت الضغط يعتمد على حالة الحاوية والمنتج وتطبيق معايير التعبئة. لم نُسجّله خطأً تحت تصنيف HARMFUL.

## المراجع الأصلية
1. NIOSH L.P.G. — https://www.cdc.gov/niosh/npg/npgd0679.html (مخلوط وقودي قابل للاشتعال، CAS خليط 68476-85-7 في المرجع، لكنه لا يُفرض كـCAS جزيئي على صف الـlegacy).
2. NIOSH Nitrogen dioxide — https://www.cdc.gov/niosh/npg/npgd0454.html (تأثيرات الجهاز التنفسي وتسريع الاحتراق).
3. NIOSH Ammonia — https://www.cdc.gov/niosh/npg/npgd0028.html (NH₃ لا NH₄⁺).
4. NIOSH Ethyl alcohol — https://www.cdc.gov/niosh/npg/npgd0262.html (مثال فقط على كحول قابل للاشتعال).
5. OSHA HCS physical hazards Appendix B — https://www.osha.gov/hazcom/appendix-b .
6. OSHA pictograms and labels Appendix C — https://www.osha.gov/hazcom/appendix-c .
7. OSHA health hazards and mixture classification Appendix A — https://www.osha.gov/hazcom/appendix-a .

## حدود الأهلية
1. وجود مادة وفئتها في الجدول **لا يثبت اكتشافها بواسطة MQ**، ولا يسمح بوضع ppm أو رمز GHS على إنذار لحساس غير مؤهل.
2. استخدم مخطط علامة PROVISIONAL في العرض البحثي فقط، مع المصدر والملاحظة؛ لا تدمج هذه الرموز بالنتائج المؤكدة دون SDS أو تصنيف خليط مستند إلى بياناته.
3. كل المعادلات الست عشرة العامة غير المحسومة سابقًا يمكن أن تشير إلى إحدى **خمس هويات مرجعية عامة**، لكن ذلك لا يميز الغاز الفعلي ولا يخلق دليل MQ مستقلًا.
4. اختبار قبول المرحلة: SQLite قبل/بعد، ثبات UID التاريخي، 2 إدخال جديد فقط عند الغياب، 7 حقائق GHS مع حالتها، اختبار تكرار ensure، وعدم وجود جداول إضافية.
5. لا تغيير في أصل ChemCore V6 أو إنشاء APK V8؛ العمل ضمن ChemCore Next العلمي المستقل.

**ملفات الشيفرة:** `android/app/src/main/java/com/servoc/app/chemical/ChemCoreNextGeneralIdentityBootstrap.java` و`ChemCoreNextGeneralHazardBootstrap.java`، واختبار `ChemCoreNextUnifiedCatalogGeneralHazardTest` و`ChemCoreNextGeneralSourceAliasTest`.

**التحقق النهائي الفيزيائي:** غير منفذ. **تأهيل G3:** 0.
