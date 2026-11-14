# G2 UCI1081 — تحليل مخرجات القياس الفعلي (2026-10-26)

## أصل النتائج
- GitHub Actions run: https://github.com/HazRob/HazRob-Tank/actions/runs/40247645187
- Artifact ID: `12619196567`، digest `sha256:9663f13673e635c0c0b0f49d4c87e0683dccfb74b338a858e04ee7c7a7461b04`.
- File: `uci1081_mq135_open_set_v4_results.json`، مع ملخص مستقل.
- Original UCI1081 archive SHA256: `813ebb7d00ed9172114b68d27ab3213170d6c220b8b23907dce91e9838cd48ab`.
- **90** تجربة، MQ135 فقط؛ مصدر خارجي، ليس مجموعة Tank ذات 8 MQ.
- Signal: median(last 120s injection) - pre-injection baseline؛ وحدات مشابهة ADC، ليست Rs/R0 أو ppm.

## بروتوكول
Six leave-one-gas-out folds; train replicates 0–2 for known classes; known final test replicates 3–4; all 15 replicates of held-out gas treated unknown. No independent source session/device identifiers available. Calibration of threshold from held-out known calibration subset also reported.

## النتائج التي أعيد فحصها من Artifact
- Known acceptance macro: **94.44%**.
- Unknown gas rejection macro: **4.44%** (poor).
- With separate known calibration, known acceptance: **100%**; unknown rejection remains **4.44%**.
- Training-derived threshold is optimistic and not suitable for claim of generalized OOD performance.

## قرار G2
**EXTERNAL_EXPLORATORY_BENCHMARK_COMPLETED, OPEN_SET_PERFORMANCE_FAILS_QUALIFICATION.** This is real external source evidence for 06.5 methodology, **not G2 empirical qualification** for Tank or broad open-set claims. No certified concentration or safety use.

## الخطوات التالية
- Add independently grouped session/device exposures or equivalent externally verifiable holdout data.
- Compare alternative baselines and calibrations on genuinely untouched groups.
- Repeat on multichannel MQ datasets with trustworthy gas labels and report false negative unknowns.
