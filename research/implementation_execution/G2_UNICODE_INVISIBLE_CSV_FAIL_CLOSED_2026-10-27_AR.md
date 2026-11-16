# G2 — منع التباس هوية القياسات بسبب محارف Unicode غير المرئية في CSV

**التاريخ:** 2026-10-27. **المشروع:** HazRob Tank. **النطاق:** فرع البحث `research/chemcore80-central-evidence-gap-analysis-20261021` فقط.

## نقطة الاستئناف والمصادر
- رأس فرع البحث قبل هذه الجولة: `d3668b66e11bb8ed98d4921545b842f5e7919bf3`، وسبق توثيق رفض محارف ASCII C0/DEL في `G2_CSV_CONTROL_CHAR_FAIL_CLOSED_2026-10-27_AR.md`.
- تمت إعادة قراءة مكتبة المراجع المركزية مباشرة من `HazRob/build-harness/library/reference-bank/external-research/master_catalog.json`؛ Git blob SHA: `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f`، مطابق للقيمة المثبتة في `G2_CENTRAL_SOURCE_CANDIDATES_2026-10-26.json`.
- سجل الأدلة المستقلة `G2_INDEPENDENT_EVIDENCE.json`: `records: []` عند المراجعة. مصادر المراجع الخارجية لا تثبت تلقائياً صلاحية قياسات Tank.

## الخلل وخطورته
كانت البوابتان تتحققان من المحارف ASCII من U+0000 إلى U+001F وU+007F، لكنهما تقبلان محارف Unicode الخفية مثل NEL (U+0085)، Zero Width Space (U+200B)، Right-to-Left Override (U+202E)، BOM (U+FEFF)، Line Separator (U+2028)، وWord Joiner (U+2060). قد تحتوي حقول `group_id` و`gas_reference_id` و`predicted_reference` أو عناوين الأعمدة على هذه المحارف رغم صحة تجزئة SHA256؛ مما يخلق التباساً بين الهوية الظاهرة والبايتات المقروءة ويضعف مراجعة الفصل الفيزيائي بين مجموعات التدريب والمعايرة والاختبار.

## الإصلاح القابل لإعادة الإنتاج
- تحديث `g2_evidence_bundle_gate.py` و`g2_cross_artifact_gate.py` لرفض فئات Unicode `C*` (التحكم/التنسيق/البدائل/الخاص/غير المعرّف)، وكذلك `Zl` و`Zp`، في **كل عنوان وخلية CSV**. تبقى الأحرف العربية واللاتينية المشكّلة والمسافات العادية مقبولة.
- إضافة ثلاث حالات `unittest` في `test_g2_evidence_bundle_gate.py`، تتضمن اختبارات فرعية متعددة لمحارف Unicode الستة، وحقول العناوين والخلايا في الملفات الثلاثة `group_manifest.csv` و`predictions_blind.csv` و`metrics_per_group.csv`.
- تتعمد الاختبارات تعديل بايتات الملفات الاصطناعية ثم **إعادة حساب SHA256** وربط `evaluation_artifact_sha256` عند تعديل ملف التنبؤات. تتأكد الاختبارات من رفض بوابة سلامة الحزمة، ورفض قارئ إعادة التشغيل، وحجب نتيجة G2. هذه اختبارات برمجية اصطناعية فقط، وليست أدلة قياس غازات.

## إعادة التشغيل
```bash
cd research/implementation_execution
python -m unittest -v test_g2_signal_lineage_gate test_g2_evidence_audit test_g2_evidence_bundle_gate test_g2_cluster_bootstrap_gate test_g2_uci1081_signal_manifest
cd ../..
python research/implementation_execution/g2_evidence_audit.py --output /tmp/g2_audit_results.json
```

## نتائج التحقق
- الالتزامات البرمجية: `5abf157214c87141bf13332fe1d8cb11b6b64b8d` و`117382dbfcd556e103afaf9a6c2b07bfc0f8e827` و`af25a04a551435c096eae6c2015b3ca493280259`.
- نجحت **188/188** اختبارات في تشغيل GitHub Actions G2 رقم `40539426731` (الوظيفة `122313451426`) على الالتزام `af25a04a...`.
- نتيجة التدقيق: `g2_pass_candidates=0` و`g2_blocked=80`.
- مقارنة `d3668b66...af25a04a`: ثلاث ملفات متغيرة حصرياً داخل `research/implementation_execution/`. لا تعديل في ChemCore Stable.
- لا تُعتبر تشغيلات CI المعلّقة نجاحاً قبل ظهور نتيجة `completed/success`.

## القيود العلمية
هذا إصلاح سلامة هوية نصية ومنع التباس البيانات. لا يُثبت أصالة الحساس أو R0 أو تتبع معيار التركيز أو الاستقلال الفيزيائي أو معايرة ppm. لا توجد حزمة قياسات مستقلة مؤهلة في السجل، ولا يجوز منح اعتماد G2/G3 لأي تقنية. المطلوب لاحقاً: ملفات قياس أصلية موثقة، مرجع تركيز قابل للتتبع، معرّفات أجهزة وجلسات فعلية، وتقسيمات اختبار محجوبة مستقلة.
