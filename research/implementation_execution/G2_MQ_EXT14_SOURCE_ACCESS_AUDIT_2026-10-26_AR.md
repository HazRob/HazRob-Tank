# G2 — تدقيق الوصول إلى المصدر الأصلي MQ-EXT-14 (2026-10-26)

## الهدف
التحقق من إمكان الوصول إلى ملفات البيانات الخام لمجموعة Mendeley المنشورة `78y8kv6hz7`، التي يصفها ناشرها بأنها استجابات ثمانية حساسات لأبخرة الإيثانول وحمض الأسيتيك والهكسانال والبنتانون بتركيزات مختلفة. **لا يثبت وصف الصفحة أن الحساسات من عائلة MQ، ولا يثبت مرجع ppm أو استقلال الجلسات.**

- المصدر: https://data.mendeley.com/datasets/78y8kv6hz7/1
- DOI: `10.17632/78y8kv6hz7.1`
- دليل API الرسمي: https://data.mendeley.com/api/docs/

## التنفيذ الموثق
- الشيفرة: `research/implementation_execution/g2_mendeley_mq14_source_probe.py`.
- اختبارات الضوابط السلبية: `test_g2_mendeley_mq14_source_probe.py` — **4/4 PASS**.
- CI: https://github.com/HazRob/HazRob-Tank/actions/runs/40279463016 — **success** لسلامة مسار الاستكشاف، **وليس نجاحاً في تنزيل المصدر**.
- الالتزام المنفذ: `421dfe8669901e15c33ef520a7c6352586b1353a`.
- artifact `12631786887`، SHA256: `66e4ea2f4225eb65675744c13ebca23efa4b0396768e83d598f96891dd05c960`.
- استجابة `GET https://api.data.mendeley.com/datasets/78y8kv6hz7?version=1`: **HTTP 401**.
- استجابة `GET https://api.data.mendeley.com/datasets/78y8kv6hz7/snapshot?version=1`: **HTTP 401**.
- الحالة: `SOURCE_METADATA_ACCESS_BLOCKED`. لم تُنزّل ملفات خام، ولم يُحسب SHA256 لأي ملف مصدر، ولم تُجر تجربة على MQ-EXT-14.

## العائق والإجراء التالي
الواجهات الرسمية المختبرة تتطلب وصولاً موثقاً في بيئة CI الحالية. لا يجوز استبدال بيانات المصدر ببيانات محاكاة أو إسناد نموذج MQ أو تركيز ppm اعتماداً على الوصف. الخيارات: قناة تنزيل عامة موثقة تسمح بالتحقق من البايتات، أو إذن API مناسب، أو استبعاد المجموعة مؤقتاً والانتقال إلى مصادر Zenodo/UCI المتاحة.

**G2: لا ترقية. G3 Tank: لا ترقية. ChemCore Stable: لم يُعدل.**
