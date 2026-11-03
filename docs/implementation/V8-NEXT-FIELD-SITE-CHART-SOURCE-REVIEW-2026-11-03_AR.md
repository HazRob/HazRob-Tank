# HazRob Tank V8 — مراجعة الربط العلمي لمخططات ChemCore Next في تقارير الجلسة والموقع

**نقطة استئناف قابلة للتحقق:** 2026-11-03، فرع `feature/tank-v8-implementation`.  
**النطاق مجمّد:** لا إعادة تصميم للقالب؛ لا تغيير أبعاد A4 أو عدد خانات الرسوم أو ترويسات V7 المشتركة. يُسمح بتصحيح معنى البيانات المعروضة ومصدرها.

## R03: تقرير الجلسة (جلسة واحدة = قياس معتمد واحد)
الكاتب الإنتاجي `SmartFieldSessionPdfWriter` يطلب `FieldSessionReportExporter.resolve`، والتحقق من قياس معتمد واحد وبصمة الأدلة `SHA-256`، ثم يقرأ `NextDecisionStore.latestForSession(primaryMeasurementSessionId)`. إذا وُجد Next يُفضَّل على معلومات ChemCore الأقدم؛ وإذا لم يوجد تبقى التقارير التاريخية الموثقة على مسارها السابق. الملخص والجدول والرسم والترويسة يجب أن تشير إلى **نفس قياس Next**، وليس إلى بيانات حية أو قياس آخر.

**خريطة خانات Next الاثنتي عشرة (أرقام الخانات 0–11):**

| خانة | معنى الرسم | مصدر البيانات الثابت/الأصلي | ما لا يجوز استنتاجه |
|---|---|---|---|
| 0 | عدد جذور الأدلة المستقلة لكل UID مرشح | `NextDecisionStore.Receipt.originalDecision.rankedEligibleCandidates[*].sourceRoots` | ليس احتمال هوية |
| 1 | جذور الدراسات/المصادر المسجلة | `originalDecision.evidence[*].sourceRootId` | ليس عدد مركبات مثبت |
| 2 | بصمة Rs/R_air للشوارد المؤهلة | `sourceSnapshot.readings[*].airRatioOrNaN()` | ليست Rs/R0 المصدرية إن غاب R0 |
| 3 | عدد أدلة المصدر المسجلة حسب الحساس الفعلي | `originalDecision.evidence[*].sensorUid` | ليس تركيزًا بالـppm |
| 4 | قطبية دليل المصدر + أو مشروط أو ملتبس | `originalDecision.evidence[*].polarity` | لا يُعد السالب غير المؤهل دليل نفي |
| 5 | حالة القرار وUID وبصمة المصدر | `Receipt.status/decisionUid/snapshotSha256` | لا يمنح G3 أو إنذارًا |
| 6 | الاستجابة الزمنية المقاسة لكل MQ مقابل الثواني | `SessionSeries.byId(db,measurementId).samples[*].tMs, mq[*].rs, scan_cycle` | ليست منحنيات مصدر/مصنع أو ppm؛ الطرح `ln(Rs(t)/Rs(t0))` |
| 7 | المقاومة المحفوظة للوحدة kΩ | `sourceSnapshot.readings[*].rsKohm` | لا يُعد تعيينًا لهوية مادة |
| 8 | شيفرات ADC الأصلية، عندما تكون متاحة | `sourceSnapshot.readings[*].rawAdc` | الخانة الفارغة ليست صفرًا |
| 9 | ربط المصدر والحساس في سجل الأدلة | `originalDecision.evidence[*].sourceRootId/sensorUid` | لا يُضاعف جذر الدراسة الواحد كدراسات مستقلة |
| 10 | درجة الحرارة والرطوبة مع الاستجابة | `SessionSeries` المؤرشف أو `sourceSnapshot` | لا يستنتج شروطًا لم تُقَس |
| 11 | نسبة القنوات السليمة على مستوى العينات الزمنية | `SessionSeries` و`ok,powered,rs,scan_cycle` مع استبعاد التكرار | ليست نسبة دقة كشف الغازات |

يستخدم R03 نفس `grid6()` المعتمد: 3 أعمدة × صفين في كل من صفحتي الأدلة والسياق. الرسم الزمني يتطلب عينتين صحيحتين على الأقل لكل حساس ولا يكرر `scan_cycle`. يظل الملحق التاريخي V6 مقروءًا دون إعادة تفسيره على أنه قرار Next.

## R04: تقرير الموقع (عدة جلسات، قياس واحد معتمد لكل جلسة)
- `SiteSummaryReportExporter` يقرأ جلسات الموقع المعتمدة فقط من `approvedSessionsForSite` ويستدعي `FieldSessionReportExporter.resolve` لكل واحدة، ولا يدمج سلاسل MQ أو متوسطات PPM.
- عندما تتوفر إيصالات Next يُستخدم `NextDecisionStore` لكل جلسة بشكل مستقل، ويتوقف استخدام قيم `top_posterior` و`screening_144` و`ppm` القديمة لتلك الجلسة.
- صفحات المقارنة نفسها تعرض الآن: تكرار جذور أدلة Next **بحساب مرة لكل جلسة**، تكرار UID البحثي لكل جلسة، توزيع حالات قرارات Next، وفهرس مصادر الجلسات. عند اجتماع جلسات تاريخية وجديدة تُوسم الرسوم بأنها خاصة بجلسات Next فقط؛ مصفوفة الجلسات الأساسية تستعرض جميع جلسات الموقع.
- للمرشحين من Next تُعرض `UID <number>` لا الاسم المؤكد، ويظل PPM/الإنذار/الأخطار غير المؤهلة فارغًا.
- لا يُعاد احتساب نتائج Next، ولا يُعدل `next_research_decisions` أو `telemetry_samples` أثناء الرسم.

## ملف Excel البحثي الموسع
`SmartFieldSessionXlsxWriter` يحتفظ بالأوراق الأصلية (14 أساس + 2 V6) ويضيف أربعة أوراق مستقلة عبر `V8FieldFullTelemetryXlsx`: سجل البيئة الكامل، ADC/ADS/MQ الأصلي، القنوات المساعدة والجزيئات والصوت والريلاي، وثلاث حمولات JSON لكل عينة زمنية. **20 ورقة** متوقعة عند وجود ملحق V6، بلا تغيير تنسيق PDF.

## الاختبارات ومسار استئناف التنفيذ
1. تجميع Java كاملاً و`G1` عبر `.github/workflows/v8-next-android-compile-gate.yml`؛ راجع آخر run والـSHA، لا تعتمد تشغيلًا أقدم على ملفات تغيرت.
2. Android 12 `V8NextTemporalChartInstrumentedTest`: يعكس تغير اتجاه `Rs(t)` في بكسلات الرسم، ويتحقق من عدم تضخيم التغطية عند تكرار `scan_cycle` وعدم رسم قناة `ok=false`. بالإضافة إلى `ReportChartDataParityInstrumentedTest` للتقارير الأخرى.
3. Android 13 `V8MultiSessionSiteFixtureActivity` عبر workflow F05: موقع به جلستان مستقلتان، إحداهما إيصال Next اختبار فقط محفوظ **قبل** بصمة الاعتماد، وإخراج PDF الجلسة والموقع وExcel 20 صفحة/ورقة (حسب كاتبي الإنتاج) مع رفض تعديل مصدر جلسة معتمدة.
4. **حالة الاختبارات الجديدة:** يلزم الرجوع إلى GitHub Actions للتحقق من اكتمال آخر الأعمال؛ لا توثّق PASS دون النتيجة. آخر نجاح مثبت قبل مسار الموقع Next: `41563156569` compile، و`41563067266` تشغيل F05 لأحد تعديلات الاختبار. تحديث الحالة مطلوب.
5. اختبار جهاز Tank الحقيقي ونطاق G3 وR0 والمعايرة/المعيار النوعي لاحق؛ لا يُنتج APK دون موافقة الإصدار.

**ملفات التعديل الأساسية:** `V8NextChartRenderer.java`، `NextSiteEvidenceCharts.java`، `SmartFieldSessionPdfWriter.java`، `SiteSummaryReportExporter.java`، `SiteReportIntelligence.java`، `V8FieldFullTelemetryXlsx.java`، واختبارات Android12/Android13 المقابلة.
