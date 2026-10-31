# G2 — MQ4 segment lower bound and prior-day cross-boundary capacity (2026-10-31)

**Scope:** research-only consistency checks in `g2_mq4_cross_report_reconciliation.py`. These checks compare two pinned MQ4 summary reports; they do not reopen the original raw ZIP or certify exposure independence, model accuracy, gas identity, concentration, G2 or G3.

## Mathematical constraints

For `n` timestamped rows partitioned into `s` nonempty, strictly contiguous one-minute segments with lengths `L_i`, the number of valid lag-`k` pairs is `P_k = Σ_i max(0, L_i - k)`. Therefore **`max(0, n - s*k) <= P_k <= max(0, n - s + 1 - k)`**. The existing upper bound is retained and a necessary lower bound is added. This inequality does not reconstruct the original segment lengths and cannot establish physical independence.

For a cross-midnight pair at lag `k`, every counted later-day row has a distinct earlier-day partner. Consequently, the number of declared crossing pairs for that date cannot exceed either the later-day or the immediately preceding day's raw row count. The existing adjacency and later-day checks are retained; the previous-day capacity check is added. Nonadjacent day crossings still fail closed.

## Synthetic regression cases

1. A single contiguous segment of 181 rows cannot report 175 lag-5 pairs when 176 are necessary. The test preserves per-lag arithmetic while violating only the new segment lower bound among earlier lag-level checks.
2. A report cannot declare five lag-5 crossing pairs when its preceding calendar day declares only four source rows. The test specifically exercises the new preceding-day capacity check; downstream fold totals are not treated as independent evidence.

The committed pinned-summary test and deterministic committed-result byte replay remain unchanged. Expected offline suite count rises from 611 to **613**, pending CI confirmation. Any failure must be preserved and repaired before recording a passing result.

## Scientific boundary

No independent calibrated reference-gas exposures, authenticated physical sample groups, preregistered blind holdouts or external scientific review have been supplied. `G2_INDEPENDENT_EVIDENCE.json` remains the authority for actual submitted independent evidence. Neither these inequalities nor passing synthetic tests promote any of the 80 techniques. Stable ChemCore, Android, ESP32 firmware and `main` are out of scope.

## CI regression and correction

Initial commit `f58aa703d17cabe77289d90f1b06596ae67a9094` triggered [G2 CI 41154893354](https://github.com/HazRob/HazRob-Tank/actions/runs/41154893354): **613 tests, one failure**. The earlier empty-calendar-day negative fixture expected `FOLD_EMPTY_CALENDAR_DAY` but the new preceding-day capacity check raised first. This was diagnostic precedence, not a disagreement in pinned MQ4 data. The capacity check was moved after complete per-fold validation, preserving the original error classification. The preceding-day capacity fixture now keeps fold arithmetic and global row totals internally consistent while still violating the physical-day capacity constraint. Re-run CI; no pass is asserted until verified.

## Verified CI evidence

After repairing diagnostic precedence, [GitHub Actions run 41155009989](https://github.com/HazRob/HazRob-Tank/actions/runs/41155009989) on commit `ae10f849f1cc4b7b0ae639cffc402121078334a0`, job `124399757672`, passed **613/613** offline tests, compilation, 80-technique fail-closed audit, deterministic replay, and publication integrity gate. Research artifact ID: `13024852649`. The previously failed run `41154893354` is preserved for audit. This verification does not imply scientific G2 or G3 approval.
