# G2 06.5 — حدود الثقة لنتائج UCI1081 (2026-10-26)

**Artifact source:** workflow run `40247645187`, artifact `12619196567`; archive SHA256 `9663f13673e635c0c0b0f49d4c87e0683dccfb74b338a858e04ee7c7a7461b04`.

| Method | Metric | Successes / observations | Point estimate | Wilson 95% |
|---|---|---:|---:|---:|
| Training threshold | Known accepted | 170/180 | 94.44% | 90.08–96.95% |
| Training threshold | Unknown rejected | 4/90 | 4.44% | 1.74–10.88% |
| Separate calibration | Known accepted | 90/90 | 100% | 95.91–100% |
| Separate calibration | Unknown rejected | 4/90 | 4.44% | 1.74–10.88% |

Formula: Wilson score interval, z=1.959963984540054, pooled per-observation.

**Important statistical qualification:** pooled Wilson assumes independent Bernoulli observations. Exposure replicates and folds may be correlated, so intervals are descriptive lower-level approximations, not cluster-aware uncertainty or G2 qualification. The known-test sets are of different sizes across methods; do not interpret the apparent gain as paired causal improvement.

**Decision:** no G2 qualification for technique 06.5; prioritize genuinely independent holdout, unknown-gas detection and multi-MQ sensors.
