# G2 V4 — سلامة مخطط CSV في حزمة الأدلة (2026-10-27، توقيت عدن)

## الحالة السابقة والمصدر
- مراجعة فرع البحث `research/chemcore80-central-evidence-gap-analysis-20261021` عند الالتزام `f10c5480560e3a04d743dccb9555809f9ec8eed7`.
- كان سجل `G2_INDEPENDENT_EVIDENCE.json` خالياً من الحزم المؤهلة، ونتيجة التأهيل التجريبي 0/80؛ لا تغير هذه الجولة هذا العدد.
- مراجعة تشغيلات GitHub Actions للالتزام السابق: 20 تشغيلًا ظاهرة بحالة success بتاريخ 2026-10-26، تشمل G2 source signal/R0 ودفعات V4. هذه نجاحات برمجية وليست إثباتاً لقياسات Tank.
- مرجعيات المراجعة: `g2_cross_artifact_gate.py`، `g2_evidence_bundle_gate.py`، `test_g2_evidence_bundle_gate.py`، و`G2_CLUSTER_BOOTSTRAP_RECOMPUTATION_GATE_2026-10-26_AR.md`.

## العيب المكتشف
`csv.DictReader` في Python يسمح بتكرار أسماء الأعمدة، فيُبقي آخر قيمة ويطمس الأولى، كما يحتفظ بخلايا زائدة في المفتاح `None` ويملأ الحقول الناقصة بقيمة `None`. لذلك يمكن لحزمة ذات SHA256 صحيح أن تمر عبر قراءة متساهلة رغم وجود غموض في `group_manifest.csv` أو `predictions_blind.csv` أو `metrics_per_group.csv`. هذا خطر على تفسير استقلال الجلسات، وحقيقة الاختبار المحجوب، وعدد النتائج الصحيحة.

## الإصلاح القابل لإعادة الإنتاج
- أصبحت `_read_csv` في `g2_cross_artifact_gate.py` ترفض: رأس ملف فارغ؛ اسم عمود فارغ أو محاط بمسافات؛ أسماء أعمدة مكررة؛ صفاً بعدد حقول أكبر أو أصغر من المخطط.
- يلتقط `audit_cross_artifact` هذا الخطأ ضمن `CROSS_ARTIFACT_READ_FAILED`، فتظل الحزمة `G2_BLOCKED` حتى مع تحديث تجزئات ملفاتها.
- أضيفت ثلاثة اختبارات انحدار في `test_g2_evidence_bundle_gate.py`: رؤوس مكررة في الملفات الثلاثة؛ حقول زائدة في الملفات الثلاثة؛ حقل أصل إضافي مفقود في صف واحد.
- هذه اختبارات بيانات اصطناعية لسلامة المحلل، وليست قياسات غاز أو دليلاً على صلاحية التصنيف.

## التزامات البحث
- `1651892657f278161954d5b986bfe631bd8f7e91`: تعديل قارئ CSV.
- `af268f89c07cf8b54b3dcd3ba2d8765356d490be`: إضافة الاختبارات.
- `f06ee9de7d5812aec72399dcd540a22f8a6051da`: تصحيح محارف السطر الجديد في عينات CSV.

## التحقق المطلوب
يُعاد التنفيذ في clean checkout باستخدام Python 3.11:
```bash
cd research/implementation_execution
python -m unittest -v test_g2_signal_lineage_gate test_g2_evidence_audit test_g2_evidence_bundle_gate test_g2_cluster_bootstrap_gate test_g2_uci1081_signal_manifest
cd ../..
python research/implementation_execution/g2_evidence_audit.py --output /tmp/g2_audit_results.json
```
مسار CI المستهدف: `.github/workflows/g2-signal-lineage-v4.yml`. عند إنشاء هذه الوثيقة كانت تشغيلات الالتزام `f06ee9de7` قيد الانتظار؛ يجب فحص النتيجة الفعلية وتوثيقها لاحقاً دون ادعاء نجاح مسبق.

## القيود العلمية
لا تزال حزم القياس المرجعي الفيزيائي المستقل لجهاز Tank ناقصة؛ لا يجوز ترقية أي تقنية G2 أو G3، ولا تعديل ChemCore Stable أو APK أو firmware. نجاح هذه البوابة يثبت فقط رفض ملفات CSV الملتبسة، ولا يثبت مصدر البيانات أو استقلال جلسات التعرض أو دقة ppm.
