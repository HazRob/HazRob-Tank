# HazRob Tank

HazRob Tank is an **independent tracked hazardous-gas research robot**. Its Android application, ESP32/ESP32-CAM firmware, research ChemCore, and repository are **separate** from the original four-wheel HazRob project.

## مكتبة المراجع المركزية الموحدة | Unified Reference Library

**المرجع المعتمد والموحد لجميع مشاريع HazRob هو مكتبة المنظمة، وليس نسخة جديدة منفصلة لكل روبوت.**

- **[الدخول إلى مكتبة المراجع المركزية — HazRob Organization](https://github.com/HazRob/build-harness/tree/main/library/reference-bank)**
- **[الفهرس العربي الشامل للمراجع](https://github.com/HazRob/build-harness/blob/main/library/reference-bank/INDEX.md)** — بيانات MQ وMOS والتقنيات والأبحاث والمنتجات والمراجع الجزيئية
- **[فهرس المصادر الآلي الموحد JSON](https://github.com/HazRob/build-harness/blob/main/library/reference-bank/external-research/master_catalog.json)** — المعرّفات وروابط البيانات وبيان صلاحيتها وسلسلة المصدر
- **[دليل سياسة المكتبة والحوكمة](https://github.com/HazRob/build-harness/blob/main/library/reference-bank/README.md)**
- **[فهرس المجموعات المرجعية التاريخية](https://github.com/HazRob/build-harness/blob/main/library/reference-bank/LEGACY_COLLECTIONS.md)** — روابط مراجع HR069 وTank والمكتبات القديمة دون ازدواج
- **[نقطة استئناف أبحاث وتجميع المراجع](https://github.com/HazRob/build-harness/blob/main/library/reference-bank/external-research/RESUME.md)**

يتضمن الفهرس المركزي حالياً **72 سجلاً مرجعياً** بتاريخ 2026-10-21: 56 سجلاً في الجمع الأولي، ومرجعَي UCI224/UCI270 اللذين استُكملا عند تدقيق المحادثة، و14 مصدراً صينياً/يابانياً إضافياً (مواصفات ومقالات وبراءات واختبارات Figaro)، مع خمس مراجعات بحثية محفوظة. ليست هذه 72 تجربة مستقلة أو معايرات معتمدة. السجلات غير متساوية المستوى: بعضها قياسات فعلية فُحصت، وبعضها دراسات أو صفحات وصف لأرشيفات لم تُنزَّل بعد، وبعضها منتجات أو محاكاة. **لا يجوز عدّ 56 سجلاً على أنها 56 تجربة مستقلة**.

### نطاق المكتبة وعلاقة Tank بها

- يُضاف كل مرجع علمي أو بيانات قياس مفتوحة إلى **الفهرس المركزي المشترك** بمعرّف دائم، حتى إن استخدم MQ واحداً أو حساسات MOS بديلة.
- ملفات المصدر أو القياسات الكبيرة قد تبقى في مصدرها أو تخزين أبحاث المشروع مع DOI وSHA256 وروابط واضحة؛ **التسجيل المرجعي الواحد يمنع ازدواج احتساب نفس البيانات**.
- كود التقنيات الثمانين وخطط التحقق الخاصة بـTank يظل تحت [`research/`](research/). [الخطة العلمية](research/MASTER_PLAN.md)، [سجل التقدم](research/PROGRESS_80.json)، [نقطة الاستئناف](research/RESUME.md) تخص Tank.
- تجميع المراجع لا يعني اعتماد طريقة تحليل، ولا معايرة أجهزة؛ استنتاج `ppm` أو سلامة أو G3 يتطلب قياسات مرجعية من الروبوت الفعلي وإجراء تحقق مستقل. **Tank G3=0/80** في آخر نقطة تدقيق بحثي مسجلة.
- يمنع نقل برامج أو معايرات المشروع الأصلي إلى Tank دون قرار نقل ومراجعة مستقلة. المكتبة التشغيلية الأصلية HR069 تبقى تحت `sohpany/SERVOC-Android:release/hazrob-0.10.2-rc2/research_data/hr069_analytical_library/`، وفق قواعد الحوكمة المنفصلة.

### المراجعات والدراسات الحديثة

[تدقيق MQ](https://github.com/HazRob/build-harness/blob/main/library/reference-bank/external-research/reviews/MQ_DATASETS_INITIAL_AUDIT_2026-10-21_AR.md) ·
[مكتبة MQDataScience وقواعد بيانات MQ الموسعة](https://github.com/HazRob/build-harness/blob/main/library/reference-bank/external-research/reviews/MQDATASCIENCE_AND_EXPANDED_DATA_2026-10-21_AR.md) ·
[الاستدلال الجزيئي العكسي QSPR/PubChem](https://github.com/HazRob/build-harness/blob/main/library/reference-bank/external-research/reviews/PUBCHEM_QSPR_INVERSE_MOLECULAR_2026-10-21_AR.md) ·
[الفصل الكمي للمخاليط والترشيح والأنظمة التحليلية](https://github.com/HazRob/build-harness/blob/main/library/reference-bank/external-research/reviews/ANALYTICAL_SYSTEMS_MIXTURE_UNMIXING_QUANT_FILTERS_2026-10-21_AR.md).

[دراسات صينية ويابانية عن MQ/MOS](https://github.com/HazRob/build-harness/blob/main/library/reference-bank/external-research/reviews/CHINESE_JAPANESE_MQ_MOS_RESEARCH_2026-10-21_AR.md).

**قانون الاستئناف:** قبل أي تجارب أو إضافة مراجع جديدة، راجع `reference-bank/INDEX.md` والفهرس الآلي في مستودع المنظمة، ثم `research/RESUME.md` و`research/PROGRESS_80.json` في Tank. لا تستخدم فروع الأبحاث المؤقتة بوصفها مكتبات مرجعية منافسة للمكتبة المركزية.
