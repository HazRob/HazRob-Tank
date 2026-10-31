## 2026-10-31 — G2 MQ4 zero-miss independent holdout coverage budget (research only; CI pending)

- Resumed from `815a620fe1cc94df56b762594d9082ac9f72f527`; G2 Actions run `41219577849` passed 724/724 offline tests and fail-closed audit/publication guard. All visible workflow runs for that checkpoint completed success.
- Added `g2_mq4_zero_miss_holdout_budget.py`, ten deterministic synthetic/fail-closed tests, pinned JSON and Arabic methodology. Local 10/10 tests passed against the same finite-rank function as the research baseline. No original source ZIP replay was performed this iteration.
- **Distinct from the finite split-conformal rank condition:** exact one-sided 95%-confidence Clopper–Pearson lower bound with zero observed misses requires 29 independent locked holdout groups to reach 90% coverage, or 59 for 95%. Six all-covered groups yield lower bound only ~60.70%. This is an optimistic zero-miss, independent Bernoulli planning bound, not measured coverage or sufficient G2 sample size.
- Illustrative combined rank+zero-miss design: 3 training + 9 calibration + 29 holdout = 41 physical groups (90%); 3 + 19 + 59 = 81 (95%). With one *verified* independent group per consecutive day and 0/1/2-day embargo, hypothetical durations 41/42/43 and 81/82/83 days. Calendar days from the observational source are not independently qualified groups.
- Changes are confined to `research/implementation_execution/` on the research branch. Check G2 Actions on this new HEAD before asserting 734/734 CI; 724/724 is the last independently verified baseline.
- Scientific state remains `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`, independent evidence `records=[]`. No G2/G3 promotion, no ChemCore Stable, Android, firmware, or main changes. Independent traceably calibrated physical exposures and pre-registered blinded grouped holdouts remain required.

---

## 2026-10-31 — G2 MQ4 prospective conformal acquisition rank budget (research only; CI pending)

- Resumed from research HEAD `604cd0191d2e5b3dd7daf42c172b82f79f7a64f9`; all 11 PR-associated workflows on that SHA completed success, including G2 `41214843068` (714 tests). Independent evidence `G2_INDEPENDENT_EVIDENCE.json` still has `records=[]`.
- Added `g2_mq4_prospective_acquisition_budget.py`, 10 deterministic synthetic/fail-closed tests, pinned `G2_MQ4_PROSPECTIVE_ACQUISITION_BUDGET_2026-10-31.json` and Arabic design document. Exact rational finite-rank bound avoids a near-unity confidence-level performance/precision issue. Local 10/10 tests PASS.
- This is a **necessary rank-only planning budget**, not physical exposure data or sufficient scientific sample size. At 90%/95% nominal levels, calibration rank needs 9/19 candidate exchangeable groups. With 3 training groups, 6 illustrative locked test groups, and calendar embargo 0/1/2 days, a **fresh** hypothetical one-independent-exposure-group-per-day sequence needs 18/19/20 or 28/29/30 consecutive calendar days respectively. Six holdout groups are not a validated coverage test size.
- Existing 11 source calendar days yield at most 2/1/0 candidate calibration days before the locked block; these days are **not** verified independent physical exposures. No source ZIP reanalysis was performed in this iteration: the planning inputs are taken from the previous pinned, SHA-verified source summary. The original ZIP remains outside Git.
- Research-only commits: implementation `e88ebe38`, tests `36f674b1`, pinned result `7c0294be`, Arabic documentation `0644cf50`, pinned-result regression `7d8afff6`, exact-rank fix `3282279e`, extreme-nominal test `80b20805`. All paths under `research/implementation_execution/`, research branch only.
- GitHub Actions on final code/test commit `80b208054d007a681c71ccf6a268c6096e38ca63` were queued at last check. Inspect its G2 run, fix failures if any, and update this checkpoint with verified CI before asserting 724 tests PASS. Previous 714/714 is verified, not a claim for this commit.
- Scientific state unchanged: `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`; no G2/G3 promotion. No ChemCore Stable, Android, firmware or main modifications. Independent traceably calibrated exposures and blinded grouped holdouts remain the fundamental blocker.

---

## 2026-10-31 — G2 MQ4 locked-day conformal finite-rank feasibility (714/714 CI, research only)

- Resumed from research HEAD `6a49bad08d054f8614e6e672d3db6e78a2f664cd`. All 20 visible preceding CI workflows completed success; G2 run `41210051442` passed 705 tests.
- Reopened original central Library ZIP `HazRob_MQ_Reference_Real_Measurements_2026-10-30.zip`; SHA256 archive `7112a370c7b21445f9e6ebb6d50e08059c352290ee6da23b0b735f5a9583afc9` and internal CSV `3c27c4f8b8e8aab54b6433acfa41de00890a9eefd19376259a2371f22a48ff2d` verified. Direct original CSV read: 14,186 rows over 11 calendar days (2021-07-21 to 2021-07-31).
- Added `g2_mq4_day_conformal_feasibility.py`, 9 synthetic/fail-closed tests, source-derived `G2_MQ4_DAY_CONFORMAL_FEASIBILITY_2026-10-31.json`, and Arabic methods `G2_MQ4_DAY_CONFORMAL_FEASIBILITY_2026-10-31_AR.md`. Research code/data commit `326b7839a63a13c11b584f3966f7b0e4e55dc6fc`.
- New mathematical blocker: a finite standard split-conformal rank at nominal 90% requires >=9 exchangeable calibration groups, at 95% >=19. The **locked** six-day forward test block, minimum 3 training days and 0/1/2-day embargo, leaves at most **2/1/0** prior calendar calibration days. Even optimistic rolling folds that reuse previous test days yield at most 7/6/5 by last test day; still insufficient for a finite 90% bound. Calendar days are not verified independent exposures/exchangeable groups; this is feasibility, not observed coverage or a certified interval.
- Local 9/9 tests passed. [G2 GitHub Actions 41214770224](https://github.com/HazRob/HazRob-Tank/actions/runs/41214770224) on research code commit passed **714/714** offline tests, 80-technique fail-closed audit, deterministic replay and publication guard (`g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`). Other workflows on code commit may still be queued; check final documentation HEAD CI independently.
- No independent exposure or traceably calibrated reference and no blinded group holdout: `G2_INDEPENDENT_EVIDENCE.json` still `records=[]`. No G2/G3 scientific promotion. No ChemCore Stable, Android, firmware or main changes. Original source ZIP remains in central Library, outside Git.
- Next: verify documentation HEAD CI and continue acquiring source-qualified physically independent exposure groups; more within-day minute rows cannot repair missing calibration groups.

---

## 2026-10-31 — G2 MQ4 three-channel forward benchmark and negative control (705/705 CI; research only)

- Resumed from verified research HEAD a50fe128a4be4f4b6d2fd89c1e3473aff37ffbe5; all 20 visible workflows on that checkpoint completed successfully. Reopened original 2,040,810-byte central Library ZIP and checked SHA256 archive 7112a370c7b21445f9e6ebb6d50e08059c352290ee6da23b0b735f5a9583afc9 and member 3c27c4f8b8e8aab54b6433acfa41de00890a9eefd19376259a2371f22a48ff2d.
- Added g2_mq4_multichannel_forward_control.py, 11 deterministic synthetic/fail-closed tests, pinned source-derived JSON, Arabic methods document. Compared training-standardized three-channel Ridge alpha=0.1 (fixed, not tuned on holdout) with each univariate OLS, training-only mean and a within-day half-rotation label placebo on identical 6 chronological held-out days / 7,889 rows and gaps 0/1/2. Original archive remains outside Git.
- Pooled Ridge RMSE gap 0/1/2: 1114.98/1158.16/1158.54 source ppb; MQ4_1 OLS 1123.90/1150.12/1150.24; training-mean 1319.04/1334.19/1351.99; label-rotation placebo Ridge 1328.08/1337.42/1437.16. Three-channel model improves slightly over MQ4_1 at gap 0 but worsens at gaps 1 and 2. Mean per-day Ridge skill is negative for all gaps; two of six days worse than training mean. Negative control is descriptive, not causal proof.
- Local 11/11 tests PASS. Code/test/results commit 974d0abfaf331b58441aa3177b40723f2d93febe; G2 Actions run 41209948423 on that commit PASS **705/705** offline tests, 80-technique fail-closed audit, byte-identical replay and publication guard. Other workflows may remain queued; check final documentation HEAD separately. All new files restricted to research/implementation_execution/ and research branch only.
- Scientific status unchanged: g2_pass_candidates=0, g2_blocked=80, source_blocked=14, independent evidence records=[]. No independent exposures, certified calibration, blinded grouped holdouts or Tank transfer; no G2/G3 promotion. No ChemCore Stable, Android, firmware or main changes.
- Next: check all final-HEAD CI; continue source-qualified validation, not additional claims from observational rows.

---

## 2026-10-31 — G2 MQ4 day-level sensitivity: negative macro-day skill (694/694 CI verified; no promotion)

- Resumed from research HEAD `e5a77097c5f761949f28539e500d00a09270a3f0`. All 11 visible PR workflows on that SHA completed success, including G2 run 41201348039 (684 tests).
- Reopened original central Library ZIP; archive SHA256 `7112a370c7b21445f9e6ebb6d50e08059c352290ee6da23b0b735f5a9583afc9`, member SHA256 `3c27c4f8b8e8aab54b6433acfa41de00890a9eefd19376259a2371f22a48ff2d`. Reproduced forward-only 11-day / 14,186-row source metrics, same six held-out days and 7,889 test rows across gaps 0/1/2 and MQ4_1/2/3.
- Added `g2_mq4_forward_day_robustness.py`, ten synthetic/fail-closed tests, pinned source-derived `G2_MQ4_FORWARD_DAY_ROBUSTNESS_2026-10-31.json` and Arabic methodology `G2_MQ4_FORWARD_DAY_ROBUSTNESS_2026-10-31_AR.md`. Local 10/10 tests passed. [G2 Actions 41205400829](https://github.com/HazRob/HazRob-Tank/actions/runs/41205400829) on implementation/test commit `b7b98bd250bb7a83de5fc98786acca40ee305e96`: **694/694 offline tests PASS**, 80-technique fail-closed audit, deterministic replay and publication guard PASS; other PR workflows on later documentation commits may still be queued.
- Important descriptive finding: row-pooled skill positive for all 9 channel/gap settings (+0.1943 to +0.2762), but **mean per-calendar-day skill negative in all 9** (−0.0803 to −0.6173); 1–2/6 held-out days worse than training-only mean, and max single-day share of model SSE up to 45.3%. Leave-one-test-day-out pooled skill ranges remain positive but are **not** confidence intervals, and days may be dependent. Do not infer robust physical ppm/gas accuracy.
- CI original ZIP not committed; GitHub CI uses synthetic fixtures and pinned summary, not independent physical validation. Scientific state `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`, independent evidence `records=[]`. No G2/G3 promotion, no Stable/Android/firmware/main modifications.
- Next: inspect latest research-branch CI; repair only research files if failed. Source-qualified independent physical exposures, traceable calibration and genuinely blinded group holdouts remain required.

---

## 2026-10-31 — G2 MQ4 chronological forward-only holdout (684/684 CI verified; no promotion)

- Resumed from verified research HEAD `03aa288bcdb0a39445c335e6426b18c3eca4b614`; its 11 PR-associated workflows completed success. Reopened central Library original ZIP, SHA256 `7112a370c7b21445f9e6ebb6d50e08059c352290ee6da23b0b735f5a9583afc9`, and internal CSV SHA256 `3c27c4f8b8e8aab54b6433acfa41de00890a9eefd19376259a2371f22a48ff2d`; 14,186 readings, 11 days, three MQ4 channels.
- Added `g2_mq4_forward_chaining_probe.py`: expanding **past-only** training days, 0/1/2 prior-calendar-day embargo, at least three training days, **identical six held-out days (26–31 July) across all gaps**, training-only mean baseline, per-fold coefficients and errors. This is a distinct observational forward-prediction stress test, not independent exposure evidence.
- Full source ZIP replay locally: 7,889 test rows across six days. Pooled RMSE ppb at gaps 0/1/2: MQ4_1 **1123.90 / 1150.12 / 1150.24**; MQ4_2 **1157.89 / 1197.61 / 1173.44**; MQ4_3 **1138.00 / 1164.34 / 1153.31**; training-mean baseline **1319.04 / 1334.19 / 1351.99**. Compare only across these same six test days; older LODO 11-day RMSE uses different test population.
- Nine new synthetic tests (including future-label non-leakage, gap boundaries, zero-variance and tampered archive hashes) + pinned compact source-derived metrics JSON + Arabic methodology. Local 9/9 PASS. The original ZIP is not in Git; CI uses synthetic tests, not the physical original source replay.
- Intermediate [G2 Actions 41201168815](https://github.com/HazRob/HazRob-Tank/actions/runs/41201168815) failed one of 684 tests because the test commit preceded the pinned JSON commit; **no validator weakened**. After the JSON commit, [G2 Actions 41201196210](https://github.com/HazRob/HazRob-Tank/actions/runs/41201196210) on `ffc54b8bf34b31ddbd3e48f5a3edf5d8e26840ea` **684/684 PASS**, 80-technique fail-closed audit, byte-identical replay and publication guard PASS. Documentation commit `945a5e5627abe410a7290ceecb6a0c9debc334d0` may have separate pending workflows; check by SHA.
- Scientific status remains `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`, independent evidence `records=[]`. No G2/G3 promotion, no ChemCore Stable, Android, firmware or main changes. All changes confined to `research/implementation_execution/` on research branch.
- Next: inspect CI on documentation checkpoint, then source-qualified new independent physical exposure evidence with traceable calibration and genuinely blinded grouped holdouts; until then preserve blocked scientific state.

---

## 2026-10-31 — G2 MQ4 adjacent-calendar-day embargo stress probe (research-only)

- Verified previous branch HEAD `4037770d76abc7cca35ce82ccb651a7ed213a87a` and previous G2 [Actions 41192600268](https://github.com/HazRob/HazRob-Tank/actions/runs/41192600268) success (667 tests). All 20 latest listed Actions on the checkpoint completed success.
- Re-read original 2,040,810-byte central Library ZIP and verified archive/member SHA256. Ran univariate OLS LODO with **0, ±1 and ±2 calendar-day embargo**, training-only mean baseline, per-day and pooled errors, on 14,186 source observations / 11 days / three MQ4 channels.
- Pooled RMSE (ppb of source methane reference), embargo 0 → ±1 → ±2: MQ4_1 **1248.26 → 1271.31 → 1282.18**; MQ4_2 **1287.28 → 1293.85 → 1315.16**; MQ4_3 **1266.99 → 1281.64 → 1294.19**. With ±2 days, 5/11, 4/11, 4/11 held-out days respectively are worse than each fold's training-mean baseline. Changing training composition changes baseline too; this is a sensitivity probe, not proof of temporal leakage.
- Added research-only `g2_mq4_neighbor_day_embargo_probe.py`, eight synthetic/fail-closed tests, pinned source-derived JSON and Arabic methodology. Original archive not committed; CI synthetic tests cannot replay source bytes. Local tests **8/8 PASS**. [G2 Actions 41196838949](https://github.com/HazRob/HazRob-Tank/actions/runs/41196838949), job `124524609863`, on implementation commit `2e2d26b9a6a93475c9c728eeaf56e17e775592f9`: **675/675 tests PASS**; all 80-technique fail-closed audit, deterministic replay, publication guard PASS (`g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`).
- No independent physically traceable exposure reference or blinded holdout. `G2_INDEPENDENT_EVIDENCE.json` remains `records=[]`; no G2/G3 promotion. No Stable, Android, firmware or main modifications.

---

## 2026-10-31 — G2 original MQ4 raw numeric LODO probe (667/667 CI verified; no promotion)

- Resumed from research checkpoint `ca8461f7e45d68d4663a3a7437da40957bd1ea04`. All 11 visible PR workflows on that SHA completed success; previous G2 implementation 661/661.
- Reopened original central Library ZIP `/HazRob/MQ_Reference_Measurements_2026/HazRob_MQ_Reference_Real_Measurements_2026-10-30.zip`; original archive and CSV SHA256 matched existing pinned raw replay. Directly analyzed **14,186 raw observations over 11 calendar days**, source methane `licor_ch4` (reported ppb) and three raw MQ4 channels. No original ZIP committed.
- Added `g2_mq4_grouped_numeric_probe.py` and six deterministic synthetic regressions. Calendar-day leave-one-day-out OLS is trained only on other days, compared to each fold's training-mean methane baseline; full raw results are reproducible with the documented CLI. Research-only summary JSON and Arabic methodology document pinned. Correlation sign is **positive on 2/11 days and negative on 9/11 days for each of the three MQ4 channels**. Pooled held-out RMSEs: MQ4_1 1248.26 ppb; MQ4_2 1287.28 ppb; MQ4_3 1266.99 ppb, versus training-mean baseline 1376.34 ppb. This is an observational source-data stress probe, **not** physically independent or blinded exposure validation, certified methane ppm, or Tank transfer evidence.
- [G2 Actions 41192600268](https://github.com/HazRob/HazRob-Tank/actions/runs/41192600268), job `124512246670`, on code/test commit `0f713d18ed19ddc7bcc55896e5e7ed68707e1486`: **667/667 tests PASS**, 80-technique fail-closed audit, deterministic replay and publication guard PASS; `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`. Synthetic tests in CI do not replay the external original ZIP.
- Changes restricted to research files under `research/implementation_execution/` on the research branch. No ChemCore Stable, Android, firmware or main modifications. Independent evidence `G2_INDEPENDENT_EVIDENCE.json` still `records=[]`; G2 remains incomplete. Future work requires independent traceably calibrated physical exposures and genuinely blinded grouped holdouts; do not promote techniques.

---

## 2026-10-31 — G2 MQ4 original ZIP raw-byte replay (652/652 CI verified; no promotion)

- Resumed from research checkpoint `30d04442eb846e7507e3e108759b6c732bdc9150`. The previous implementation CI [41180696031](https://github.com/HazRob/HazRob-Tank/actions/runs/41180696031) passed 641/641 tests; all 11 visible PR workflows on that checkpoint completed successfully.
- Located the original 2,040,810-byte source ZIP in the user's central Library `/HazRob/MQ_Reference_Measurements_2026/`; directly read the ZIP and original 4,787,918-byte CSV, verified archive SHA256 `7112a370c7b21445f9e6ebb6d50e08059c352290ee6da23b0b735f5a9583afc9` and member SHA256 `3c27c4f8b8e8aab54b6433acfa41de00890a9eefd19376259a2371f22a48ff2d`. Recomputed timestamps, 14,186 rows, 11 daily folds, 11 strict-minute segments, 6 lag-pair profiles and midnight crossings directly from original CSV bytes; all matched pinned summaries.
- Added research-only reproducible script `g2_mq4_original_zip_replay.py`, 11 synthetic/fail-closed regressions, pinned raw-derived JSON `G2_MQ4_ORIGINAL_ZIP_RAW_REPLAY_2026-10-31.json`, and Arabic methodology `G2_MQ4_ORIGINAL_ZIP_RAW_REPLAY_2026-10-31_AR.md`. The original ZIP remains in the central Library, not in the repository: CI validates synthetic regressions and pinned evidence boundaries, not the original ZIP replay.
- A transient [G2 CI failure 41184578679](https://github.com/HazRob/HazRob-Tank/actions/runs/41184578679) on the intermediate test commit occurred because the pinned result file had not yet been committed. After the result commit, [G2 CI 41184614587](https://github.com/HazRob/HazRob-Tank/actions/runs/41184614587) **652/652 PASS**, all 80-technique fail-closed audit, deterministic replay and publication guard PASS; `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`. Preserve failed intermediate run in audit history.
- This is raw-byte/timestamp reconciliation, NOT independent physical exposures, certified reference calibration, locked blinded group holdouts, gas identity performance or G2/G3 scientific approval. Independent evidence remains `records=[]`. No ChemCore Stable, Android, firmware or main changes. All new files under `research/implementation_execution/` on research branch only.

---

## 2026-10-31 — G2 MQ4 reported calendar-minute capacity cross-source audit (641/641 CI verified)

- Verified previous repaired implementation [G2 Actions 41176594975](https://github.com/HazRob/HazRob-Tank/actions/runs/41176594975): **631/631 tests PASS**, 80-technique fail-closed audit, deterministic replay and publication guard; all 20 workflows on previous documentation HEAD `456cc6af0c982b25ef17a99a69402f6784860184` completed successfully.
- Added `g2_mq4_calendar_minute_capacity.py`, ten regression tests, pinned result JSON and Arabic methodology. Cross-reconciles source-intake assertions, LODO daily fold counts and strict-minute segment totals; checks 1440 minute bins/day and the first/last partial-day caps (774/723) **conditional on reported unique minute timestamps**. Does NOT replay raw ZIP bytes or independently verify the asserted time granularity.
- **641/641 offline tests PASS** on repaired implementation commit `87ee4796c5b8936ebed29c4f4bd6f34c7ea6f892` in [G2 Actions 41180696031](https://github.com/HazRob/HazRob-Tank/actions/runs/41180696031), job `124477332383`. Full 80-technique fail-closed audit, deterministic replay and publication guard PASS. The initial test+documentation commit `6166ece797cd1e320cf0f1f997ec3819951c24b2` was followed by a diagnostic-precedence correction before verification. Existing pinned summary and Stable unchanged. Independent evidence remains empty; 0/80 G2 candidates, 80 blocked, 14 catalog sources blocked. Continue with original raw-byte verification and independently calibrated physical exposure evidence.

---

## 2026-10-31 — G2 per-day strict-minute lag capacity (631-test CI requeued)

- Resumed from `1ffe2a92b75d1c606c95a77080aed19ff003b489`. All 11 PR-associated workflows returned completed-success; the prior G2 implementation passed **627/627** in [Actions 41172130460](https://github.com/HazRob/HazRob-Tank/actions/runs/41172130460).
- Added the necessary per-day bound `same_day_context_rows(k) <= sum_d max(0, holdout_total(d)-k)` for strictly contiguous one-minute lag pairs, in `g2_mq4_cross_report_reconciliation.py` (code commit `40c40d10a124daecfcd068235dac784f693f7ebb`). This rejects arithmetic summaries that demand more within-day pairs than daily row counts can supply; no raw ZIP replay or physical calibration is claimed.
- Added four regressions and pinned-source check in test commit `e12132c983b68a8b3df4cda4b40b78dc986014a1`; Arabic methodology `G2_MQ4_SAME_DAY_STRICT_MINUTE_CAPACITY_2026-10-31_AR.md` (commit `935b438efeff4ddf27a1e73f0fda7e0d755b9bae`).
- Initial [G2 Actions 41176405189](https://github.com/HazRob/HazRob-Tank/actions/runs/41176405189) ran **631 tests, one failed**: a pre-existing midnight-profile regression was intercepted by the new stronger day-capacity check. Repaired the *fixture* to preserve the new day-capacity bound while still testing the original profile contradiction, without weakening either validator; repair commit `b971b180ef3118a31c1ca0a5f87a5be2f7c810d2`. New [G2 Actions 41176466982](https://github.com/HazRob/HazRob-Tank/actions/runs/41176466982) **queued at last check; 631/631 NOT YET VERIFIED**. Check this run, its job logs, all workflow failures and repair if necessary.
- **CI repair follow-up:** [G2 Actions 41176461508](https://github.com/HazRob/HazRob-Tank/actions/runs/41176461508) on `b971b180...` again failed one of 631 tests: the modified legacy fixture violated the already-established prior-day row capacity. No validator was disabled. Restored the legacy fixture and preserved its original midnight-profile diagnostic by executing the new same-day bound **after** the exact-midnight feasibility check (code `a3e5e188ebb7e9d03f9b31876bd9dcc8d1ad31aa`). Isolated the new bound in a separate integrated synthetic test with the earlier profile validator mocked only for that test (test commit `8018b67a373494672a3c784270aeba5fa71533ba`). Aligned the Arabic methodology in `f9b4c4559d7f8e3ae1ec6c1e947ae896fa51fa99`. [G2 Actions 41176594975](https://github.com/HazRob/HazRob-Tank/actions/runs/41176594975) queued at last check; **631 tests expected, not yet CI verified**. Preserve the two failed CI runs in the audit history.
- Verified compare `1ffe2a92...b971b180`: only three files under `research/implementation_execution/` changed. No ChemCore Stable, Android, firmware or main changes. Independent evidence remains `records=[]`; G2 candidates 0/80, blocked 80/80, source slots blocked 14/14 at last verified audit. No scientific promotion; independent reference exposures, traceable calibration, blinded group holdouts and external review remain missing.

---

## 2026-10-31 — G2 joint adjacent-midnight capacity gate (627/627 CI verified)

- Resumed from research checkpoint `65d200b897995a776c3526f58baa9f31726d9505`; its documented G2 implementation gate passed 624/624 in [Actions 41168123457](https://github.com/HazRob/HazRob-Tank/actions/runs/41168123457). GitHub connector returned 11 completed-success PR workflow runs for that documentation SHA; other workflows may exist outside the first page.
- Found a gap in independent per-midnight feasibility: individually possible crossing profiles can jointly demand more distinct head/tail rows than a shared calendar day contains. Added deterministic adjacent-boundary capacity DP to `g2_mq4_cross_report_reconciliation.py`; code commit `661e2c7778eb875d32c69f4722ef1a1ec8b17eb9`. New error `MQ4_CROSS_REPORT_CROSS_DAY_ADJACENT_CAPACITY` distinguishes this failure from per-boundary infeasibility.
- Added three regressions (six-row feasible shared day, five-row infeasible shared day, pinned MQ4 source summaries) in test commit `a5a27bf9885432ef10f63ee107fe592210965f3d`; Arabic methodology `G2_MQ4_JOINT_ADJACENT_MIDNIGHT_CAPACITY_2026-10-31_AR.md`. **627/627 tests PASS** in [G2 Actions 41172130460](https://github.com/HazRob/HazRob-Tank/actions/runs/41172130460), job `124452148377`, on test commit `a5a27bf9885432ef10f63ee107fe592210965f3d`. Byte-identical audit/triage replay and publication guard PASS; 0 G2 pass candidates, 80 blocked, 14 source slots blocked. Other workflows on later documentation commits must be checked independently. No original raw ZIP replay was performed.
- Changes confined to `research/implementation_execution/` on the research branch. ChemCore Stable, Android, firmware, main, and existing scientific qualification flags unchanged. Independent evidence `records=[]`, 0/80 G2 candidates, 80 blocked, 14 source slots blocked at the last verified audit. Physical independent exposures, traceable calibration, blinded holdouts, and external review still required.

---

## 2026-10-31 — G2 exact midnight profile feasibility gate (624/624 CI verified)

- Reviewed prior research HEAD `dd5e74af94ba66a9d45553c4171cb7903e9ec8dc`: 20/20 associated Actions succeeded; the prior code/test commit `ec1e2b5307a087da9e83faa927f6bc78d4a33bb0` passed **620/620** tests in [G2 Actions 41164080898](https://github.com/HazRob/HazRob-Tank/actions/runs/41164080898). No CI failures on the prior documentation HEAD.
- Added exact sparse-lag **single-midnight-profile feasibility** across all lag knots using bounded `(a,b)` witness search. Unlike the earlier 1-Lipschitz necessary condition, this rejects impossible profiles such as `[1,5,15,30,59,60]` that satisfy each lag's cap and adjacent difference bound. Source-pinned ten day boundaries have feasible `(120,120)` witnesses.
- Added four regressions (valid falling tail, impossible Lipschitz-valid profile, integrated tamper, pinned report) and Arabic methodology `G2_MQ4_EXACT_MIDNIGHT_PROFILE_FEASIBILITY_2026-10-31_AR.md`. **624/624 tests PASS** on implementation commit `f80b1a52300f58a0188ffc3eaddcb98d40ddf502` in [G2 Actions 41168123457](https://github.com/HazRob/HazRob-Tank/actions/runs/41168123457), job `124440011712`. Compile, 80-technique fail-closed audit, deterministic replay and publication guard PASS; `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`. Other workflow runs on the documentation checkpoint must be verified separately.
- Research-only change under `research/implementation_execution/` on research branch; no ChemCore Stable, Android, firmware or main changes. Original raw ZIP not replayed by this gate; no scientific promotion. Independent evidence `records=[]`, G2 0/80 qualified, 80 blocked, 14 source slots blocked.

---

## 2026-10-31 — G2 MQ4 midnight lag Lipschitz gate verified: 620/620

- Rechecked the previous checkpoint `e36e9932eeb8068a1b14bef079cd84d606e88403`: G2 Actions [41159859977](https://github.com/HazRob/HazRob-Tank/actions/runs/41159859977), job `124414801435`, **616/616 PASS**, full 80-technique audit, deterministic replay and publication guard; all 20/20 workflows on that SHA completed successfully.
- Added the necessary **per-midnight 1-Lipschitz bound** on cross-day lag-pair counts, derived from `C(k)=max(0,min(b,k)-max(0,k-a))` for the single strict-minute segment that can span a given midnight. The bound allows decreasing tails and rejects impossible jumps between sparse lag knots. This checks reported arithmetic, not source timestamps or exposure independence.
- Code `054223c2cc4a4daf7ac8e3ca12a1ba8f70e5d8c4`; four new synthetic/pinned/integration tests in `ec1e2b5307a087da9e83faa927f6bc78d4a33bb0`; Arabic methodology `G2_MQ4_CROSS_MIDNIGHT_LAG_LIPSCHITZ_2026-10-31_AR.md` committed as `71fd9266d18ceab1b295b3e9c58f0af221dc4d96`.
- **620/620 offline tests PASS** on code/test commit in [G2 Actions 41164080898](https://github.com/HazRob/HazRob-Tank/actions/runs/41164080898), job `124427740859`; compile, 80-technique fail-closed audit, deterministic replay and publication guard all PASS. Pinned MQ4 reconciliation output remains byte-identical and scientific qualification false. The later documentation-only commits may have separate Actions still pending; verify by SHA rather than assuming their status.
- Independent evidence remains `G2_INDEPENDENT_EVIDENCE.json: records=[]`, `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`. G2 not complete; missing independently measured and traceably calibrated physical exposures, locked blinded group holdouts and independent scientific review. Changes restricted to `research/implementation_execution/` on research branch; ChemCore Stable, Android, firmware and main untouched.

---

## 2026-10-31 — G2 MQ4 sparse-lag convexity research gate (CI verification pending)

- Resumed from the latest research checkpoint: repaired code commit `ae10f849f1cc4b7b0ae639cffc402121078334a0` passed G2 Actions [41155009989](https://github.com/HazRob/HazRob-Tank/actions/runs/41155009989), **613/613 tests**, full 80-technique fail-closed audit, deterministic replay and publication guard. Older repaired [41149752531](https://github.com/HazRob/HazRob-Tank/actions/runs/41149752531) also succeeded.
- Added a **necessary convexity inequality for sparse lag-pair counts** in `g2_mq4_cross_report_reconciliation.py`: for ordered lag knots, the average number of surviving strict-minute segments cannot increase at longer lags. Integer cross multiplication avoids floating-point tolerance. Integrated into `reconcile()` after existing per-lag diagnostics, without changing the pinned output schema or saved result.
- Added **three regressions** (valid two-segment construction, impossible nonconvex counts despite monotonicity and independent bounds, pinned MQ4 summary), and Arabic methodology `G2_MQ4_SPARSE_LAG_SEGMENT_CONVEXITY_2026-10-31_AR.md`. Code commit `85e3331049f6470ee434d5606d69e2c0d50af02f`, tests commit `7c833001b2e21cfdb34ad877539ce93eb035c275`. **616 offline tests expected, NOT yet CI verified**. Check latest Actions and repair any failures before declaring success.
- This is report-level mathematical consistency, not original raw ZIP replay, independent physical exposures, traceable calibration, blinded holdout, model performance or scientific G2/G3 approval. Evidence still `records=[]`; G2 0/80 candidates, 80 blocked, 14 source slots blocked. Changes confined to `research/implementation_execution/` on the research branch; no ChemCore Stable, Android, firmware or main changes.

---

## 2026-10-31 — G2 MQ4 necessary-bound research gate verified: 613/613

- Repaired implementation `ae10f849f1cc4b7b0ae639cffc402121078334a0` passed [GitHub Actions G2 run 41155009989](https://github.com/HazRob/HazRob-Tank/actions/runs/41155009989), job `124399757672`: **613/613 offline tests PASS**, compile PASS, full 80-technique fail-closed audit PASS, deterministic replay PASS, publication guard PASS. Research artifact `13024852649` saved. G2 `0/80` candidates, `80/80` blocked, source slots `14/14` blocked; independent evidence `records=[]`.
- New necessary lag-pair lower bound and prior-day cross-midnight capacity both tested. Initial failed run [41154893354](https://github.com/HazRob/HazRob-Tank/actions/runs/41154893354) remains visible; root cause was diagnostic precedence and was corrected, not suppressed. The committed pinned MQ4 reconciliation result remains unchanged.
- CI confirmation applies to the code/test commit above. The later documentation-only checkpoint may trigger additional Actions; verify their status separately. No physical independent exposures, traceable calibration, blinded holdout or scientific G2/G3 promotion. Only research files on the research branch changed; Stable/Android/firmware/main untouched.

---

## 2026-10-31 — G2 MQ4 CI diagnostic-precedence repair (613-test gate requeued)

- Initial [G2 CI 41154893354](https://github.com/HazRob/HazRob-Tank/actions/runs/41154893354) on `f58aa703d17cabe77289d90f1b06596ae67a9094` executed 613 tests with **one failure**: `test_reject_declared_calendar_day_with_zero_source_rows` expected `FOLD_EMPTY_CALENDAR_DAY`, but the newly inserted previous-day capacity check raised earlier. No pinned-source discrepancy was shown; test failure was diagnostic precedence.
- Repaired by deferring the previous-day capacity invariant until after existing fold validations. Updated the new previous-day capacity negative fixture to preserve fold and global row accounting; both invariants remain active. **613 tests expected; recheck CI before declaring success.**
- Research-only code/tests/methodology/RESUME, no Stable/Android/firmware/main changes or scientific promotion; G2 remains blocked without independent measured references and blinded physical holdouts.

---

## 2026-10-31 — G2 MQ4 necessary lower lag bound and preceding-day crossing capacity (613 tests expected; CI pending)

- Rechecked prior repaired G2 CI: run [41149778083](https://github.com/HazRob/HazRob-Tank/actions/runs/41149778083), job `124383281484`, **611/611 PASS**; all 20/20 workflow runs on research HEAD `059164fe7f579c083a9b33a09bd9e491ab44e8eb` completed successfully. G2 remained 0 candidates / 80 blocked / 14 source slots blocked; independent evidence `records=[]`.
- Added two **necessary** report-consistency checks in `g2_mq4_cross_report_reconciliation.py`: `P_k >= max(0,n-s*k)` for nonempty contiguous segments and cross-midnight pair count <= the preceding calendar day's declared raw rows. Existing upper bound, date adjacency, later-day capacity, and fail-closed scientific boundaries remain unchanged.
- Added two synthetic regressions to `test_g2_mq4_cross_report_reconciliation.py` and methodology `G2_MQ4_SEGMENT_LOWER_BOUND_PRIOR_DAY_CAP_2026-10-31_AR.md`. **613 offline tests expected, not verified until GitHub Actions completes.** The pinned reconciliation output is intentionally unchanged.
- All changes restricted to `research/implementation_execution/` on the research branch; no ChemCore Stable, Android, firmware, main, G2/G3 qualification or scientific claims changed. Check G2 CI for this commit, repair failures if any, and preserve negative findings. Physical independent reference exposures, traceable calibrations, blinded holdouts and independent review remain missing.

---

## 2026-10-31 — G2 MQ4 CI regression repair (611-test gate requeued)

- Initial new-code G2 workflow [41149599386](https://github.com/HazRob/HazRob-Tank/actions/runs/41149599386), job `124382691584`, executed **611 tests and failed 1**: legacy `test_reject_lag_pair_count_increasing_with_lag` expected `LAG_AVAILABLE_MONOTONIC`, but the newly inserted stronger `LAG_MAX_PAIRS_BY_SEGMENTS` check raised first. No underlying pinned-source discrepancy was identified; the test failed because of diagnostic precedence.
- Fixed by checking monotonicity first, then the stricter combinatorial bound, preserving both checks and the legacy diagnostic contract. Repair commit `d481dfa0c4afdf917937c7ec0e7a01f637ad847e`. The 611-test G2 workflow [41149752531](https://github.com/HazRob/HazRob-Tank/actions/runs/41149752531) was queued at the last inspection. **Do not claim a pass until the repair CI finishes**; inspect logs and repair further failures if any.
- Scientific state remains `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`, independent evidence `records=[]`. No Stable, Android, firmware, main or G2/G3 qualification changes.

---

## 2026-10-31 — G2 MQ4 necessary lag-pair and later-day row bounds (611 expected; CI verification pending)

- Reviewed the previous checkpoint `1a825ce8da3e7fe3b577e5089ba49d7ce0d08497`. Its G2 workflow [41144282084](https://github.com/HazRob/HazRob-Tank/actions/runs/41144282084), job `124365584523`, **completed successfully with 608/608 tests**, 80-technique fail-closed gate, deterministic replay and publication guard (`g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`). This confirms the *previous* checkpoint only.
- Identified a necessary combinatorial invariant absent from the MQ4 cross-report checker: with `n` rows, `s` nonempty strict-minute segments and lag `k`, contiguous pairs cannot exceed `max(0,n-s+1-k)`; mere monotonicity is insufficient. Also added later-day cross-context count <= later-day raw fold rows and a positive row count per declared calendar day.
- Research-only code commit `b9d765bcec8772ca8a63baa17b2cd4a2f94f5fa7`; tests `299a3f913ec815c34763d4adce203a8ccd0a82b6` corrected with internally consistent cross-day fixture in `d3b0c1b7fac2cc6de97746b173ad2ee5239281af`. Three new synthetic regressions; **611 tests expected, not yet verified**. Methodology: `G2_MQ4_LAG_COMBINATORIAL_DAY_ROW_BOUNDS_2026-10-31_AR.md` (commit `134df376eca8c9ac56456039e78b9b485ceba077`). Verify new G2 Actions and repair any failure; do not infer success from previous CI.
- Existing pinned reconciliation result schema/bytes unchanged; no new original raw ZIP replay, independently calibrated reference, physical exposure grouping or scientific qualification. `G2_INDEPENDENT_EVIDENCE.json` still `records=[]`; no G2/G3 promotions. All modifications restricted to `research/implementation_execution/` on the research branch; Stable, Android, firmware and main unchanged.

---

## 2026-10-31 — G2 MQ4 cross-midnight day adjacency and lag-one continuity (CI pending)

- Resumed from `6857c83a9330885ff5cd629ed277a16fcf7b3844`; verified **20/20 prior HEAD Actions successes** and preserved 605-test baseline. Research-only updates to `g2_mq4_cross_report_reconciliation.py` and `test_g2_mq4_cross_report_reconciliation.py` add fail-closed checks for **nonadjacent calendar-day crossings** and **positive longer-lag midnight crossing without a lag-one contiguous crossing**. Added **three synthetic regression cases** and Arabic methodology `G2_MQ4_CROSS_DAY_CONTIGUITY_2026-10-31_AR.md`. Expected test count is **608**, but **do not claim 608/608 PASS before G2 CI finishes**.
- Explicitly rejected an initially considered cross-day-count monotonicity rule: short contiguous segments can yield fewer long-lag pairs; removed that incorrect check and its test before the final checkpoint. The remaining checks are necessary report-consistency constraints, not raw replay or scientific qualification.
- Code/tests final commit `004bc4421a76317174e81d86d63c091152c2f2d7`; documentation correction `8c889fc7ca430867dced9eaff4fef2f32b0b916c`. The G2 Actions run `41144282084` and associated workflows were queued at last check. Recheck CI and repair any failures; validate 80-technique fail-closed gate, deterministic replay and publication guard before calling this checkpoint verified.
- `G2_INDEPENDENT_EVIDENCE.json` still contains `records=[]`; 0/80 scientific G2 candidates and 14/14 source slots blocked at last verified gate. No ChemCore Stable, Android, firmware or main changes. Missing independently measured physical exposures, traceable reference calibrations, blinded holdouts and external scientific review still block scientific promotion.

---

## 2026-10-31 — G2 MQ4 temporal and fold-complement consistency gate (605/605 CI verified)

- Resumed from `c369cff69598121143a67ec017adf0099b38d242`; independently verified **20/20 prior commit Actions successes**. Source catalog remains 14/14 blocked, independent evidence `records=[]`, scientific G2 0/80 candidates.
- On research-only implementation commit `105ea471d57be59f6c8ae2ec8d7e6714b3757c48`, hardened `g2_mq4_cross_report_reconciliation.py` to reject impossible ISO calendar days, float/Boolean metadata masquerading as integer counters, incorrect lag-1 pair/segment identity, non-monotonic lag availability, impossible per-day cross-midnight pair counts, and per-fold training/holdout complement violations that can cancel in aggregate totals. Added **eight synthetic regression tests** and Arabic methodology `G2_MQ4_CROSS_REPORT_TEMPORAL_INVARIANTS_2026-10-31_AR.md`.
- **Verified G2 CI:** [run 41138058771](https://github.com/HazRob/HazRob-Tank/actions/runs/41138058771), job `124345298723`: **605/605 tests PASS**, Python compile PASS, full 80-technique fail-closed audit PASS, byte-identical replay PASS, publication guard PASS (`g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`). **All 21/21 associated workflow runs on implementation commit succeeded**; no CI failures. The later documentation-only commit requires separate CI verification.
- Pinned MQ4 source summaries still reconcile without alteration. This verifies necessary arithmetic/temporal invariants only, **not original raw bytes**, model performance, physical independence, calibration, G2 or G3 qualification. All changed paths are in `research/implementation_execution/` on the research branch; no ChemCore Stable, Android, firmware or main modifications.

---

## 2026-10-31 — G2 MQ4 saved-result byte-replay gate (597/597 verified)

- Extended the pinned MQ4 cross-report regression suite with `test_committed_result_is_byte_identical_replay`, checking that the committed `G2_MQ4_CROSS_REPORT_RECONCILIATION_2026-10-31.json` matches fresh deterministic reconciliation of the two pinned source summaries byte for byte.
- **Verified G2 CI on test commit `dd26cdfd77411afc4c02989a85bf16cc023dcbde`**: [run 41131075501](https://github.com/HazRob/HazRob-Tank/actions/runs/41131075501), job `124321825635`: **597/597 tests PASS** (including committed-output replay), Python compilation, 80-technique fail-closed evidence audit, deterministic triage/audit replay, and publication guard all PASS; `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`. Documentation-only commits may have queued CI; verify separately before claiming all latest-commit runs complete.
- Output-only reconciliation confirms 14,186 rows, 11 days, 11 strict-minute segments, 12,981 eligible max-lag rows, and 1,200 excluded holdout contexts plus 1,200 purged training contexts. It does **not** replay the original ZIP, verify model accuracy or establish independent physical exposure groups. No G2/G3 promotion or Stable/Android/firmware/main changes.

---

## 2026-10-31 — G2 MQ4 pinned cross-report reconciliation (596/596 G2 CI verified)

- Reviewed research HEAD and prior G2 checkpoint: 583/583 G2 CI, 80 catalog techniques, 14 source slots, independent evidence `records=[]`. Verified latest pre-change research workflows on SHA `5ec1b81d26e2c1dd853cb4054a847c3f04c56b15` successful.
- Added `g2_mq4_cross_report_reconciliation.py` to fail closed on source-hash or schema mismatch, per-lag cross-day arithmetic, LODO holdout/train purge accounting, inconsistent fold totals, duplicate JSON keys, nonfinite JSON and unsupported scientific qualification. Added 13 tests in `test_g2_mq4_cross_report_reconciliation.py` (12 synthetic + one pinned repository-report test), Arabic methods `G2_MQ4_CROSS_REPORT_RECONCILIATION_2026-10-31_AR.md`, and pinned summary result `G2_MQ4_CROSS_REPORT_RECONCILIATION_2026-10-31.json`.
- **Verified implementation/test CI** on commit `da2e127032da4523d512ac58c94a765f2275f36d`: [G2 run 41130888748](https://github.com/HazRob/HazRob-Tank/actions/runs/41130888748), job `124321186798`: **596/596 tests PASS**, including pinned MQ4 cross-report test; Python compilation PASS; full 80-technique fail-closed audit PASS; deterministic audit/triage replay PASS; publication guard PASS (`g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`). Recheck documentation and output-only commit workflows independently; do not claim latest-commit CI success before completed.
- Report arithmetic reconciles 14,186 source rows, 11 calendar days, 11 contiguous minute segments, 12,981 max-lag timestamp-eligible rows and 1,200 two-way LODO exclusions/purges each. This is **cross-checking two existing reports**, not a fresh original-raw replay; shared upstream errors remain possible. Physical independence, Tank MQ8 hardware, reference calibration, model performance, G2/G3 approval remain unverified.
- Changes limited to research branch `research/chemcore80-central-evidence-gap-analysis-20261021`, `research/implementation_execution/` only; no Stable, Android, firmware or main changes. No G2/G3 scientific promotion.

---

## 2026-10-30 — G2 pinned MQ4 external benchmark claim reconciliation (583/583 G2 CI verified)

- Resumed from the latest 570-test research checkpoint and verified **11/11 GitHub Actions successes** on implementation SHA 070a119ad97e079ab78d7b47fbb8c869d0328772, including G2 source/R0 run 41115493429. Central scope remains 80 techniques, 14 external source slots, 0/14 review-ready; G2_INDEPENDENT_EVIDENCE.json still records=[].
- Independently materialized the **original user Library MQ4 ZIP** and V6 external benchmark summary; verified ZIP SHA256 7112a370c7b21445f9e6ebb6d50e08059c352290ee6da23b0b735f5a9583afc9, raw CSV SHA256 3c27c4f8b8e8aab54b6433acfa41de00890a9eefd19376259a2371f22a48ff2d, summary SHA256 0df1ed3776aa515973886bad18153498325a038040bdb08d8a0a70ab35f5a1ea. Recounted **14,186 original rows across 11 days, 2,346 LiCor CH4 reference positives at >=4000 ppb (4 ppm) and 11,840 negatives**. These are autocorrelated source rows, not independent physical exposures.
- Added g2_mq4_benchmark_claim_audit.py (strict pinned input hashes, raw LiCor source recount, declared confusion arithmetic/rates, scientific boundary), 13 synthetic and pinned-report regressions in test_g2_mq4_benchmark_claim_audit.py, byte-identical locally replayed report G2_MQ4_EXTERNAL_BENCHMARK_CLAIM_RECONCILIATION_2026-10-30.json (SHA256 c984723c246b219877cf9e0fc79357da205d032d1104aaaa18ccc4fa921130fb), and Arabic method G2_MQ4_EXTERNAL_BENCHMARK_CLAIM_RECONCILIATION_2026-10-30_AR.md. Local 12 synthetic tests PASS and original raw replay PASS; the additional pinned-report test passed in G2 CI. Reproducible source replay requires original Library ZIP and summary; GitHub CI does not contain those raw bytes and tests only synthetic fixtures plus pinned report integrity.
- The external HGB model summary's claimed 4-ppm sensitivity **22.72%** and claimed 90%-interval coverage **82.62%** are consistent with its declared confusion counts and reference class counts; **the original model predictions, MAE, and interval coverage were NOT independently recomputed**. The 4-ppm boundary is a research diagnostic, not an occupational alarm. No multi-gas identity, Tank MQ8 hardware, source-calibration traceability, or physical-group independence validated.
- Current code/test implementation SHA ef732fd7590859d695925a216474c412c56177a6; **G2 workflow run 41123534805, job 124296042925: 583/583 Python tests PASS, compilation PASS, full 80-technique fail-closed audit PASS, deterministic replay PASS, publication guard PASS (0 candidates / 80 blocked / 14 source-blocked)**. Associated documentation-checkpoint workflows were still queued at the last check; recheck those separately. All changes limited to research/implementation_execution on research branch, no Stable/Android/firmware/main changes, no G2/G3 promotion. Scientific G2 remains 0/80 candidates, 80 blocked, source readiness 0/14.

---

## 2026-10-30 — G2 strict all-test contamination coverage diagnostic (570/570 G2 CI verified)

- Resumed from `98cd852179710aa6fdbe11c544ca8b9d854d2add` and independently verified **20/20 Actions successes** including the G2 source/R0 gate. Central pinned inventory: 80 entries, 14 external datasets, **0/14 source-review-ready**; `G2_INDEPENDENT_EVIDENCE.json` remains `records=[]`. MQ raw archive intake is research-only, not independent qualified evidence.
- Added strict tri-state all-test clean coverage and two-clean-with-no-contamination indicators plus contaminated declared test-group count. Legacy descriptive two-test-component flag preserved. Known direct/transitive train/validation/calibration contamination is negative even when optional IDs are absent; absent IDs never establish a positive clean result. Device reuse alone is not contamination.
- Added 12 synthetic regressions and `G2_DECLARED_TEST_CONTAMINATION_COVERAGE_2026-10-30_AR.md`. **Verified implementation CI on commit `070a119ad97e079ab78d7b47fbb8c869d0328772`: [G2 run 41115493429](https://github.com/HazRob/HazRob-Tank/actions/runs/41115493429), job `124268518114`: 570/570 offline tests PASS, Python compile PASS, all 80 techniques fail-closed (`g2_pass_candidates=0`, `g2_blocked=80`), 14 source slots blocked, deterministic byte-identical audit/triage replay PASS, publication guard PASS. Artifact `13004978192`, ZIP SHA256 `3f53adc30f280f3277c5add1204a978094c42006de4ec92c43f7dbb8d3e46e6b`. At the time of this checkpoint, other batch07/batch08 workflows on the implementation commit were still in progress/queued; do not claim all associated workflows succeeded before checking. Verify documentation-only commit CI independently.
- Changes only under `research/implementation_execution/` on research branch. No Stable, Android, firmware, main, G2/G3 promotion. Independent traceable reference-gas exposures, preregistered blinded physical holdouts and external review remain unavailable.

---

## 2026-10-30 — G2 train/validation/test declared-identity overlap diagnostics (558/558 verified)

- Resumed from research HEAD `cf1e690adc7f2b7b5c229fab4bf24d023d8d3d3e`. Independently rechecked **20/20 workflow successes** on that commit, including the G2 source/R0 gate. Central readiness matrix remains 80 catalog entries, 14 external datasets, **0/14 source-review-ready**; `G2_INDEPENDENT_EVIDENCE.json` still has `records=[]`.
- Added tri-state `train_test_declared_identity_component_overlap`, `validation_test_declared_identity_component_overlap`, and `test_non_test_declared_identity_component_overlap` to `g2_declared_holdout_partition_audit.py`. These detect direct/transitive links through declared session, exposure batch, exposure and physical sample IDs; device reuse alone does not link groups. A detected link is true even with incomplete optional identity columns; no detected link is false only with all four identity fields and the relevant partition; otherwise null. Metadata is not authenticated physical evidence.
- Implementation commit `410c0eb57013b341af30f1307ba397606c63f582`; ten synthetic regressions commit `8b9685db24dbb9353efbf879f5f51f6742673202`; Arabic methodology `G2_TRAIN_VALIDATION_TEST_DECLARED_IDENTITY_CONNECTIVITY_2026-10-30_AR.md` commit `4941da5f3da925e276613b996d01a7cc4c086205`.
- **Verified G2 CI** on test commit: [run 41106367890](https://github.com/HazRob/HazRob-Tank/actions/runs/41106367890), job `124237139375`: **558/558 tests PASS**, compile PASS, full 80-technique fail-closed audit PASS, deterministic replay PASS, publication guard PASS (`g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`), artifact `13001930153`. Documentation-checkpoint and subsequent CI must be independently checked; no claim of complete latest-commit CI before it finishes.
- Only research implementation/test/methodology/RESUME paths on the research branch. No ChemCore Stable, Android, firmware or main changes. No G2/G3 promotion. Authenticated independent exposures, traceable physical reference concentration, preregistered blinded holdouts, and independent scientific review remain unavailable.

---

## 2026-10-30 — G2 calibration/test declared-identity connectivity (548/548 implementation CI)

- Resumed from research HEAD `a2e5afa347f284515c243018a86aafb25fdbbdb2`; independently rechecked its **20/20 GitHub Actions workflow successes** (including G2 source/R0 gate). Last verified G2 offline suite was **538/538**. Central source matrix remains **0/14 source-review-ready**, independent evidence `records=[]`, and scientific G2 candidates **0/80**.
- Implemented conservative `calibration_test_declared_identity_component_overlap` in `g2_declared_holdout_partition_audit.py`: catches direct/transitive declared calibration-to-test identity connections across sessions, batches, exposures, and physical samples; device reuse alone never creates a link. `false` requires full declared identity fields and an actual calibration partition; missing identity/calibration yields `null` unless a connection is already detected (`true`). These are declared metadata diagnostics, not authenticated physical independence.
- Implementation commit `c7dc34d17a8b125910911a27e6126084b491c860`; ten synthetic regression tests committed as `0f2b4f48dbfd157ad2c8b8a354f00f4eb2e5f6da`; Arabic methods `G2_CALIBRATION_TEST_DECLARED_IDENTITY_CONNECTIVITY_2026-10-30_AR.md` committed as `5685f24794019fd32e6e28d037aa5e6da63ebc14`.
- **Verified implementation/test CI:** [G2 Actions run 41098744594](https://github.com/HazRob/HazRob-Tank/actions/runs/41098744594), job `124211234478`, on test commit `0f2b4f48dbfd157ad2c8b8a354f00f4eb2e5f6da`: **548/548 tests PASS**, Python compile PASS, 80-technique fail-closed audit PASS, deterministic byte-identical replay PASS, and publication guard PASS (`g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`). Other workflow runs on later documentation commits must be rechecked separately; no unverified pass claimed.
- Changed files limited to research implementation/tests/methodology/RESUME in this research branch. No ChemCore Stable, Android, firmware or main edits; no G2/G3 promotion. Missing authenticated independent physical exposure evidence, traceable reference concentrations, preregistered blinded holdouts, and independent review remain scientific blockers.

---

## 2026-10-30 — G2 538-test implementation CI verification

- Implementation/tests checkpoint `98975929da42b1466268f7ddb574dcd6a0de1bce`: [G2 run 41088528851](https://github.com/HazRob/HazRob-Tank/actions/runs/41088528851), job `124175958627`: **538/538 offline tests PASS**, all new missing-identity regressions included, full 80-technique fail-closed audit PASS, deterministic byte-identical replay PASS, publication guard PASS; `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`.
- Associated workflows on implementation commit: 9/11 success, batch07/batch08 queued at last check; no observed failures. Documentation checkpoint `160597dd38cd8c18072a10fe3e4290e5a92ec37e` triggered 11 new workflows, queued at last check. **Do not claim complete documentation-checkpoint CI success until those runs complete.**
- Only research implementation paths changed; no independent physical evidence, no G2/G3 scientific approval, and no Stable/Android/firmware/main edits.

---

## 2026-10-30 — G2 missing optional identity provenance fail-closed diagnostic (CI pending)

- Resumed from verified checkpoint `0032f1d775a708487950fe73b662a462c4706ee6` (11/11 associated workflows successful); central source matrix remains 0/14 independently qualified and `G2_INDEPENDENT_EVIDENCE.json` remains `records=[]`.
- Fixed a research diagnostic overstatement: previously, two disconnected declared test components could return a positive structural-support flag even when `exposure_id` or `physical_sample_id` was absent. Such missing identities can conceal transitive connectivity. The descriptive component counts now explicitly carry upper-bound/incomplete-provenance flags; the positive two-cluster support result is `null` if either optional identity field is absent. Required session/batch absence continues to fail closed.
- Implementation commit `f3730b876bac2c9ebbebd4ef039afa83d0ade842`; eight synthetic regression tests in `test_g2_declared_identity_missing_fields_fail_closed.py`, commit `98975929da42b1466268f7ddb574dcd6a0de1bce`; Arabic methods `G2_DECLARED_TEST_IDENTITY_MISSING_FIELDS_FAIL_CLOSED_2026-10-30_AR.md`, commit `bfaa555755086ecd2f408164b770424f5cf379ef`.
- **CI pending verification on final documentation checkpoint.** Do not claim passing tests until GitHub Actions completes. Expected test count is 538 (prior 530 plus eight), but only CI can confirm. Full 80-technique G2 audit and publication guard must still be checked.
- Research branch and `research/implementation_execution/` only. No Stable/Android/firmware/main changes; no G2/G3 scientific promotion. Independent physical exposures, authenticated session/sample/device identifiers, traceable reference concentration, preregistered blinded group holdouts, and independent review remain outstanding.

---

## 2026-10-30 — G2 conservative effective declared test-identity cluster audit (530/530 G2 CI)

- Continued from verified research documentation checkpoint `93a1ee4f7ab2341b60ae62f15baef0eabbe43c8b` (11/11 associated workflows successful). Central source matrix still has 14 external source slots, 0 independently review-ready; `G2_INDEPENDENT_EVIDENCE.json` remains `records=[]`.
- Identified a methodological false-support scenario: two distinct declared test `group_id` values can be connected via session, batch, exposure or physical-sample IDs, including transitive connections through train/calibration groups. The previous two-group flag alone could overstate cluster-uncertainty support.
- Added conservative research-only union-find diagnostic to `g2_declared_holdout_partition_audit.py` that excludes device reuse, counts test-bearing and test-only identity components, and exposes cross-partition test components. Missing partition or session/batch provenance returns unverified/null, never a false independent-sample claim. Implementation commit `923cd88f7c99d2d9d4ab8407341a56160cfd55ae`.
- Added 13 synthetic regressions `test_g2_declared_identity_test_cluster_support.py`, commit `b5d8302ee84b947965b24ded78b8cebac5b7a382`. [G2 Actions run 41079690285](https://github.com/HazRob/HazRob-Tank/actions/runs/41079690285), job `124145181373`: **530/530 tests PASS**, deterministic audit/triage replay PASS, publication guard PASS, `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`.
- Arabic method note: `G2_DECLARED_EFFECTIVE_TEST_IDENTITY_CLUSTERS_2026-10-30_AR.md` committed as `0032f1d775a708487950fe73b662a462c4706ee6`. On latest CI check, G2 gate and nine associated workflows had succeeded; batch07 and batch08 remained queued. Recheck all workflows on final documentation checkpoint.
- All changed paths are in `research/implementation_execution/` on research branch only. No changes to ChemCore Stable, Android, firmware, or main. **No scientific G2 or G3 qualification**; actual independent calibrated exposures, authenticated physical provenance, preregistered blinded held-out experiments and independent review remain missing.

---

## 2026-10-30 — G2 calibration-partition and declared test-group support diagnostic (517/517 CI)

- Resumed from research HEAD `0adae7082e0d092737d9c601aded0bc8d98a0bfb`; prior 20/20 associated workflows successful. Reviewed central source-readiness matrix: 14 external source slots, 0 review-ready; `G2_INDEPENDENT_EVIDENCE.json` still `records=[]`. The Library raw-intake candidate is not independently qualified.
- Corrected a research holdout audit omission: explicit `calibration` partition is now accepted separately from `train`, `validation`, and `test`. Added deterministic declared row/group counts per partition, missing-calibration protocol-review flag, and a descriptive two-declared-test-group check for cluster-uncertainty feasibility. These are metadata diagnostics, **not** authenticated independent physical groups, and cannot grant G2/G3.
- Nine synthetic regression tests in `test_g2_declared_calibration_partition.py` cover separate calibration, session/sample cross-split overlap, absent calibration, one test group, missing train, row permutation and distinct validation/calibration.
- On implementation/test commit `5b3f6080d164477d9c67b1e9898f9d7280385573`, [G2 Actions run 41070601752](https://github.com/HazRob/HazRob-Tank/actions/runs/41070601752), job `124113346350`: **517/517 tests PASS**, full 80-technique fail-closed audit PASS, deterministic triage/evidence replay PASS, publication guard PASS. `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`. Check final documentation commit CI separately; do not infer from earlier runs.
- Arabic method note: `G2_DECLARED_CALIBRATION_PARTITION_COVERAGE_2026-10-30_AR.md`. Compare baseline to documentation commit `93a1ee4f7ab2341b60ae62f15baef0eabbe43c8b`: only three files changed, all within `research/implementation_execution/`. No ChemCore Stable, Android, firmware or main changes.
- **Next:** authenticated physical exposure/sample/session/device IDs, traceable reference-gas concentration calibration, preregistered train/calibration/test holdouts by independent physical group, blinded predictions, and independent scientific review. Software tests and manufacturer curves do not qualify any of 80 techniques. Continue research-only.

---

## 2026-10-30 — G2 declared train/test partition CI verification addendum

- Final method/test files on `5bdac3c6299c01c8f05b735d9397d1708060fff0`: [G2 run 41062584033](https://github.com/HazRob/HazRob-Tank/actions/runs/41062584033) **508/508 offline tests PASS**, deterministic evidence audit/replay and publication guard PASS, `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`.
- Documentation checkpoint `1043ebc46aa3310f1b28533d480025f4bcddc165`: **20/20 associated Actions workflows SUCCESS**, including [G2 run 41062687307](https://github.com/HazRob/HazRob-Tank/actions/runs/41062687307). Earlier failed synthetic assertions on intermediate commits remain in history; final tests repaired.
- Four changed files relative to `05a5aee1da249c790f59b2e8963f52f4ff2d0df4`, all under `research/implementation_execution/`: diagnostic, 21 synthetic tests, Arabic method note, RESUME. No Stable/Android/firmware/main edits; no scientific promotion or independent source data added. Verify CI again if this documentation-only commit triggers new workflows.

---

## 2026-10-30 — G2 declared train/test holdout partition diagnostic (CI verification pending)

- Resumed from research commit `05a5aee1da249c790f59b2e8963f52f4ff2d0df4`, whose previous G2 and associated workflows succeeded. Pinned central catalog: 80 technique entries, 14 external dataset slots, 0/14 source-review-ready; `G2_INDEPENDENT_EVIDENCE.json` still `records=[]`. No physical G2/G3 qualification.
- Added `g2_declared_holdout_partition_audit.py` (research only): explicit train/test/optional validation partitions; fail-closed when partition/session/batch or train/test coverage is absent; direct and transitive cross-partition group/session/batch connectivity; optional exposure and physical sample identity checks; device reuse does not by itself count as leakage; deterministic semantic input SHA256; no independent physical evidence claim.
- Added 21 synthetic regressions `test_g2_declared_holdout_partition_audit.py`, including NFKC critical header/optional ID aliases, CSV exact-width, missing metadata, direct/transitive cross-partition overlaps, and Arabic IDs. Arabic method note `G2_DECLARED_TRAIN_TEST_PARTITION_OVERLAP_2026-10-30_AR.md`.
- Initial G2 CI `41062215543` on test commit `4b56c98eaad0fb0a8dd9041ad91135369d95bded` failed 1/504 due to error-code ordering in a synthetic header-alias assertion (the source was correctly rejected by the shared provenance gate); fixed with early critical-header validation. Additional optional-ID fail-closed improvements added thereafter. Do not erase the failed run from audit history.
- Latest implementation/test commit `5bdac3c6299c01c8f05b735d9397d1708060fff0`; method-document checkpoint `0c10553a4508faf59a5545bb57e6dcab94a4d3bb`. Verify latest G2 Actions `g2-signal-lineage-v4.yml` and all associated workflows before declaring technical success. **No unverified CI pass claimed.**
- Only `research/implementation_execution/` changed, research branch only. ChemCore Stable, Android, ESP32 firmware, and main untouched. Independent calibrated exposures, authenticated session/sample/device provenance, preregistered blinded physical-group holdouts and independent review are still prerequisites for scientific G2; `g2_pass_candidates=0`, `g2_blocked=80`.

---

## 2026-10-30 — G2 transitive declared session/batch group connectivity (487/487 CI)

- Resumed from research HEAD `1930b0afba262ee0f1cd950623951ac153a6517c`; previous 20/20 CI workflows SUCCESS. Confirmed pinned central readiness inventory: 14 external source slots, `source_review_ready=0`; `G2_INDEPENDENT_EVIDENCE.json` still `records=[]`. No new independent physical evidence.
- Added research-only transitive connected-component diagnostic to `g2_declared_provenance_overlap_audit.py`. Groups are joined if they share declared `session_id` or `exposure_batch_id`; links can be transitive across both identifier types. Device reuse alone never creates an edge. If either session or batch column is missing, connectivity is explicitly unverified (`checked=false`, components `null`). Even disconnected declared components do NOT prove physical independence; multi-group components call for protocol review before split holdouts.
- Added ten synthetic regressions `test_g2_declared_provenance_components.py`, including cross-field transitive links, device-only reuse, missing provenance, row-order and duplicate invariance, Arabic identifiers and multiple components. Implementation commit `bfcd0f3483b06432781cc1a710fadc72835983d0`, tests commit `681ae8827e488ee02fe723b07fde11f1f8d75893`.
- [G2 Actions run 41055173880](https://github.com/HazRob/HazRob-Tank/actions/runs/41055173880), job `124060777385`: **487/487 PASS**, deterministic audit/triage replay PASS, publication guard PASS; `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`. No G2/G3 qualification.
- Arabic method note `G2_DECLARED_SESSION_BATCH_TRANSITIVE_CONNECTIVITY_2026-10-30_AR.md`, documentation commit `77b9424605dd9dc6d1a93f9441e4aa5af00a3015`. Compare from previous HEAD: only three research implementation files changed prior to this checkpoint; ChemCore Stable, Android, firmware and main untouched.
- Next scientific evidence blocker: authenticated physical sample/exposure/session/device IDs, traceable reference concentration calibration, preregistered blinded physical-group holdouts and independent review. No software-only promotion. Verify documentation-checkpoint CI and use this section to resume.

---

## 2026-10-30 — G2 exact repeated declared-provenance row diagnostics (477/477 CI)

- Resumed from research HEAD `070a4ffaf03bb8ad87d87a8feb54da0a313620d9`; verified all 20/20 previous branch-head workflows SUCCESS. Central pinned scope remains 80 techniques / 14 source slots; no independent source provenance `records=[]`, 0/14 qualified sources, 0/80 G2 candidates.
- Research-only extension to `g2_declared_provenance_overlap_audit.py`: count unique full declared rows, exact duplicate excess rows, repeated row patterns, and declared row counts per group. Do **not** equate rows or duplicate metadata with independent physical exposures; do not automatically reject legitimate repeated measurements. Preserve existing full-row semantic SHA256, row-order invariance, NFKC alias fail-closed checks, and source/nonpromotion flags.
- Seven synthetic regressions in `test_g2_declared_provenance_duplicate_rows.py`; implementation commit `417b6ff3dd1b87a6f2eaf0988c5a47178d2b12fa`, test fixture correction `0169255726fc57c14ab59d18906e083569ea6106`. [G2 Actions run 41047379807](https://github.com/HazRob/HazRob-Tank/actions/runs/41047379807), job `124034670324`: **477/477 PASS**, deterministic audit and triage replay PASS, publication guard PASS, 0 candidates / 80 blocked / 14 source-blocked. No G2/G3 qualification.
- Arabic methodology: `G2_DECLARED_PROVENANCE_EXACT_DUPLICATE_ROWS_2026-10-30_AR.md`, documentation commit `fb393dadb0b65796730620b2cd44fcea8663e030`. All changes limited to `research/implementation_execution/` on research branch; ChemCore Stable, Android, firmware, and main unchanged. Verify final documentation-checkpoint CI before declaring technical closure.
- Scientific blocker unchanged: authenticated physical sample/exposure/session/device IDs, traceable concentration-reference calibration and preregistered blinded physically grouped holdouts. No scientific G2/G3 promotion without independent evidence.

---

## 2026-10-30 — G2 declared provenance CSV exact-width fail-closed (470/470 CI)

- Resumed from research HEAD `b8bb8bf7983660ab9f2a1e710861ef3518ebc2cc`, prior G2 workflow successful; pinned 80 techniques, 14 source slots, independent evidence `records=[]`.
- Found a concrete parser integrity gap: `csv.DictReader` could silently pad missing noncritical trailing cells with `None` (or collect surplus cells under `None`) before the provenance semantic digest. Replaced with strict `csv.reader` plus per-record exact-width check and explicit header rejection; valid quoted commas, multiline fields and UTF-8 BOM remain supported.
- Research implementation commit `a133e59f26be93340e3bba2bd80021c9e8d5b686`, 11 synthetic regressions commit `13ab1f978323fe4242b91de67d65de00ca518286`. [G2 Actions run 41040381887](https://github.com/HazRob/HazRob-Tank/actions/runs/41040381887), job `124011426464`: **470/470 PASS**, deterministic audit/triage replay PASS, publication guard PASS, 0 candidates / 80 blocked / 14 source-blocked. Artifact `12969164262` SHA256 `aa2c21b11c2b00cd248aae8703eab256ac066027dace2d34a9f81abf54c51e1d`.
- Arabic methods: `G2_DECLARED_PROVENANCE_CSV_EXACT_WIDTH_FAIL_CLOSED_2026-10-30_AR.md`; documentation commit `f07f7e94e23b329900a837f457a7b7dc36635ed2`.
- All edits limited to `research/implementation_execution/` on research branch. ChemCore Stable, Android, firmware and main untouched; no G2/G3 promotion. Next: authenticated physical exposure/sample/session/device IDs, traceable concentration calibration and preregistered blinded physically grouped holdouts.

---

## 2026-10-30 — G2 declared provenance NFKC compatibility-alias guard (459/459 CI)

- Resumed from research HEAD `f1bbe4865dd0fd813f7dd3b2fcf7d63149230de3`; verified 20/20 previous workflow runs successful and independent evidence `records=[]`, source-ready 0/14, G2 candidates 0/80.
- Research-only enhancement to `g2_declared_provenance_overlap_audit.py`: fail closed on NFKC-equivalent but byte-distinct group/session/exposure-batch/device IDs; also reject compatibility-aliased critical CSV headers and distinct headers with the same NFKC spelling. Preserve distinct valid Arabic identifiers; do not silently normalize source identity.
- Eight new synthetic regressions in `test_g2_declared_provenance_nfkc_alias.py`. Implementation commits `df4a92af7485bdb54db32748cc22a2457516090e` and `53f3193ea6ed3a68bc1c39c0bca20011b16f511d`. [G2 Actions run 41032580256](https://github.com/HazRob/HazRob-Tank/actions/runs/41032580256), job `123985602747`: **459/459 PASS**, deterministic G2 audit/triage replay PASS, publication guard PASS; 80 blocked / 0 candidates / 14 source-blocked. No G2/G3 approval.
- Arabic methodology: `G2_DECLARED_PROVENANCE_NFKC_ALIAS_FAIL_CLOSED_2026-10-30_AR.md`. Changes confined to research implementation directory, research branch only. ChemCore Stable, Android, firmware, and main untouched.
- Next: obtain authenticated independent physical sample/exposure/session/device provenance, traceable concentration reference calibration, and preregistered blinded physically grouped holdouts. Until then, no scientific G2 qualification; G3 physical validation remains pending.

---

## 2026-10-30 — G2 declared session/batch/device cross-group overlap audit (451/451 CI)

- Resumed from research HEAD `aeb7c0baeeca684d49caa24a2a1871cd7bc7b0d8`, 11/11 previous GitHub Actions successful. Central pinned 80 techniques and 14 source slots unchanged; independent physical evidence still `records=[]`.
- New research-only `g2_declared_provenance_overlap_audit.py` audits optional `session_id`, `exposure_batch_id`, and `device_id` across declared `group_id` values in CSV. It reports deterministic cross-group identifier overlaps, a row-order-invariant full-row semantic SHA256, and explicit null/unverified when session/batch provenance is incomplete. Device reuse is **not** automatically leakage; same session/batch spanning groups requires independent protocol review, not automatic rejection.
- Added eight synthetic tests `test_g2_declared_provenance_overlap_audit.py`; local **8/8 PASS**. On implementation commit `9d8c170c4e3d7f3a49277b61665c36c8b0db2d17`, G2 [Actions run 41025818714](https://github.com/HazRob/HazRob-Tank/actions/runs/41025818714), job `123963558299`, **451/451 PASS**, deterministic G2 audit/triage replay PASS, publication guard PASS; `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`. Artifact `12963666738`, SHA256 `d503067f6835cff5afe9c36cb1e03efa8173cdb623c0a4ab9001a0ef787ee3e8`.
- Arabic scientific note: `G2_DECLARED_SESSION_BATCH_DEVICE_OVERLAP_2026-10-30_AR.md`, documentation commit `3d4b39d45061570a2a215ba75793b7331dda131e`. At last check, remaining non-G2 batch workflows for implementation/documentation commits were queued/in progress, without observed failures; recheck final HEAD CI.
- All changes restricted to `research/implementation_execution/` on research branch; no ChemCore Stable, Android, firmware, or main modifications. This diagnostic is NOT authenticated source provenance, physical independence, ppm calibration, G2 or G3 approval.
- Next: obtain authenticated physical exposure/sample/session/device IDs, independently calibrated concentration references, preregistered blinded physically grouped holdouts; only then run empirical G2 qualification. Source-ready 0/14 and G2 qualified 0/80 remain unchanged.

---

## 2026-10-30 — G2 declared physical sample ID / true-class ambiguity audit (443/443 CI)

- Resumed from `953e7650403b5c8dffddbc99e7148edcc0e217af`, whose prior 438-test G2 run and associated CI succeeded. Existing 80-technique G2 gate and 14 external-source slots remain pinned.
- Added research-only, order-invariant diagnostic to `g2_paired_prediction_replay.py` identifying declared `physical_sample_id` values mapped to multiple `true_reference` labels, and explicit `null` when the sample column is absent. Does not assume whether the cause is reused hardware, temporal exposure changes or incorrect IDs; does not reject valid multi-exposure scenarios.
- Added five synthetic tests in `test_g2_paired_sample_true_class_audit.py`. On implementation commit `e4bda196e36427f86d56c56e789cb152fb9d8038`, G2 Actions run [41020025863](https://github.com/HazRob/HazRob-Tank/actions/runs/41020025863), job `123944979200`, **443/443 PASS**, deterministic replay PASS, publication guard PASS, `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`. Prior baseline 438 tests.
- Documentation checkpoint `fd9c25b1a4b27618a5d9b622cf4200357818a67b`: **20/20 GitHub Actions runs SUCCESS**, including G2 [41020159216](https://github.com/HazRob/HazRob-Tank/actions/runs/41020159216). No failed CI jobs at verification time.
- Arabic methods: `G2_DECLARED_SAMPLE_MULTI_REFERENCE_CLASS_AMBIGUITY_2026-10-30_AR.md`. Comparison `953e765...e4bda196`: only two research files changed before documentation. No ChemCore Stable, Android, firmware or main modifications. No G2/G3 promotion.
- Next: obtain authenticated independent physical sample/exposure/session/device IDs, traceable concentration-reference calibration, and preregistered blinded physically grouped holdouts. Independent evidence `records=[]`; source-ready 0/14; G2 qualified 0/80.

---

## 2026-10-30 — G2 MQ4 strict-minute plateau/reference-span audit

- Resumed from `670a79adbac63bd6b8242387c169f48531b3bb8d`; previous 20/20 workflows SUCCESS, 432/432 tests, independent evidence `records=[]`, 0/14 source-qualified, 0/80 G2 candidates.
- Independently verified original Library ZIP SHA256 `7112a370c7b21445f9e6ebb6d50e08059c352290ee6da23b0b735f5a9583afc9` and raw CSV SHA256 `3c27c4f8b8e8aab54b6433acfa41de00890a9eefd19376259a2371f22a48ff2d`.
- New `g2_mq4_plateau_continuity_audit.py` uses existing numeric-integrity gate, splits constant-value runs on any non-60-second gap, assigns midnight-crossing equal pairs to the later day, and computes reference methane span within each constant channel run. Six new synthetic regressions and byte-pinned raw replay.
- Actual 14,186 rows / 10 gaps / 14,175 strict one-minute adjacent pairs. MQ4_1 / MQ4_2 / MQ4_3 strict equal pairs: 7 / 13 / 5,906; constant runs >=2: 7 / 13 / 3,430. MQ4_3: 25 constant runs with reference span >=100 ppb; max span 1201.7229 ppb; longest run 15. No previously counted equal pair crosses a non-minute gap in this source. These are descriptive channel/representation findings, NOT hardware fault, source calibration or independent exposure evidence.
- Pinned JSON `G2_MQ4_GAP_AWARE_PLATEAU_RAW_REPLAY_2026-10-30.json`, SHA256 `563c308f898cea911d8165de7b8fe96060902df88e76ac7fb2dbcb1021789f82`; Arabic methods `G2_MQ4_STRICT_MINUTE_PLATEAU_REFERENCE_SPAN_2026-10-30_AR.md`.
- Local six synthetic tests passed. Verify CI full discovery, deterministic replay, publication guard and workflow status on this checkpoint. Research-only changes; no ChemCore Stable, Android, firmware or main modifications. No G2/G3 promotion.
- CI verification on implementation commit `adc2b1dccefcea99fd02037706c932255110ed0f`: **21/21 workflow runs SUCCESS**; G2 run [41014170934](https://github.com/HazRob/HazRob-Tank/actions/runs/41014170934), job `123926596854`, **438/438 tests PASS** (six new), byte-identical audit/triage replay PASS, publication guard PASS, 0 G2 candidates / 80 blocked / 14 sources blocked. Artifact `12958444744` digest `sha256:43239653da54c039cb49b0c36b719c1587f8d4dedb7fef6e2baa2e4dff9fcb54`. This is software CI only, not source calibration or G2 qualification. Verify CI for this documentation-only checkpoint if required.
- Next independent evidence requirements: source calibration certificates, authenticated exposure/sample/device/session IDs, preregistered blinded physically grouped holdouts.

---

## 2026-10-30 — G2 pinned MQ4 numeric integrity and plateau audit

- Resumed from `531eb31897852f636f6aa0ff8528490c2bbff770`; previous G2 CI 20/20 successful. Research scope remains pinned 80 techniques/14 external source slots, 0/80 candidates, 0/14 source-qualified, independent evidence records=[].
- Independently materialized original user Library archive and SHA256-verified ZIP `7112a370c7b21445f9e6ebb6d50e08059c352290ee6da23b0b735f5a9583afc9` and raw CSV `3c27c4f8b8e8aab54b6433acfa41de00890a9eefd19376259a2371f22a48ff2d`. 14,186/14,186 rows passed finite/positive MQ4, nonnegative reference methane, valid RH and strict monotonic timestamp checks; 8 new synthetic tests passed locally.
- MQ4_1/MQ4_2/MQ4_3 distinct values: 10716/10691/**119**; identical adjacent pairs: 7/13/**5906** (41.64% of MQ4_3 consecutive pairs), longest MQ4_3 plateau 15. Possible different measurement discretization; hardware cause unknown.
- Added `g2_mq4_numeric_integrity_audit.py`, `test_g2_mq4_numeric_integrity_audit.py`, pinned raw replay `G2_MQ4_PINNED_NUMERIC_INTEGRITY_2026-10-30.json`, and Arabic methods note `G2_MQ4_NUMERIC_INTEGRITY_AND_PLATEAU_AUDIT_2026-10-30_AR.md`.
- CI verified: initial 431/431 PASS (run 41008372325, artifact 12956622017, ZIP SHA256 1b4decbdf5a11642ba634b4029526a2a190986ff26174c56b97b9c453161f299); byte-exact pinned JSON test added, then 432/432 PASS on ff957bd5708d2420347c539dc65a1305992036d2 (run 41008559543, artifact 12956805675, ZIP SHA256 43e719106e6279f244ca99587146bf6de1eba411678e15aaaaae024640d8afee). 11/11 associated workflows SUCCESS. Deterministic G2 replay and publication guard PASS, 0 candidates / 80 blocked / 14 source blocked. Raw JSON SHA256: 42dc075efcd420b45faf3754189880da5536386087cfa1995c830cde921975d0. Verify documentation-checkpoint CI on final HEAD. Numeric source integrity is NOT reference calibration traceability, physical exposure independence, model generalization or G2/G3 qualification. No ChemCore Stable, Android, firmware or main edits.
- Next: source calibration certificates, authenticated exposure/sample/session/device IDs, preregistered blinded physically grouped holdouts; test channel plateau impact only under research constraints.

---

## 2026-10-30 — G2 MQ4 hypothetical LODO two-way purge and max-lag eligibility

- Resumed from `c8ed513abf9bb760ece24e1b734720b6c26bd4c6`; verified 20/20 prior CI workflow successes, pinned 80 techniques / 14 external source slots. Independent evidence remains `records=[]`, 0/14 qualified sources, 0/80 G2 candidates.
- Extended research-only `g2_mq4_lodo_fold_audit.py`: exact longest-lag timestamp eligibility within strict 60-second contiguous segments; separate removal of eligible holdout rows using training context and purging of eligible training rows using holdout context; reconciled eligible and ineligible counts per hypothetical calendar-day fold. No actual training or physical independence claim.
- Added four synthetic regressions in `test_g2_mq4_lodo_fold_audit.py` covering longest-lag history, middle-day two-direction purge, a midnight gap, and explicit nonqualification. Expected synthetic outcomes: 181-minute/120-lag case has 61 eligible rows, 60 excluded and 1 retained on the later-day holdout; three-day/60-lag middle fold retains 1380 holdout rows and 0 training rows after purging. These are NOT real gas-validation metrics.
- Method note: `G2_MQ4_LODO_TWO_WAY_PURGE_AND_MAX_LAG_ELIGIBILITY_2026-10-30_AR.md`. Original MQ4 Library ZIP and member were independently SHA256-verified and replayed locally: 14,186 raw rows, 11 strict-minute segments, 12,981 timestamp-eligible rows with 120-minute max lag, 1,200 holdout-context exclusions plus 1,200 training-context purges across 11 hypothetical LODO folds. Reconciled eligibility via independent segment-length sum. Per-day machine-readable pinned summary: `G2_MQ4_LODO_TWO_WAY_PURGE_RAW_REPLAY_2026-10-30.json`. No independent exposure claims.
- CI must verify full offline test discovery, deterministic replay, and publication guard on the final HEAD; record final run/commit status after completion. No ChemCore Stable, Android, firmware, or main edits; no G2/G3 scientific approval.
- Next evidence blocker: independently traceable exposure/sample/session/device metadata, calibrated reference concentrations and preregistered blinded physically grouped holdouts.

---

## 2026-10-30 — G2 MQ4 hypothetical LODO fold-context and reference-leakage guard (419/419 CI)

- Resumed from research checkpoint `15bdd9994cd0ac8d8805dab56761e81ea2805ce7`; verified prior G2 CI and unchanged pinned 80 techniques / 14 external source slots. No independent exposure evidence promotion.
- Added research-only `g2_mq4_lodo_fold_audit.py` and eight synthetic tests `test_g2_mq4_lodo_fold_audit.py`; reference `licor_ch4` cannot be used as a predictor, and contiguous one-minute lag windows are mapped to hypothetical leave-one-calendar-day-out folds with train→holdout and holdout→train context separated and row unions deduplicated.
- Independently replayed SHA256-pinned original user Library MQ4 archive (`7112a370c7b21445f9e6ebb6d50e08059c352290ee6da23b0b735f5a9583afc9`), member (`3c27c4f8b8e8aab54b6433acfa41de00890a9eefd19376259a2371f22a48ff2d`): 14,186 rows, 11 dates, 11 strict-minute segments. Across all 11 hypothetical LODO folds, union of cross-fold lag context affected rows is **2,400** for lags 1/5/15/30/60/120 (each interior date 240; first/last 120). The 120-minute lag alone accounts for the union; do not sum lags as unique rows.
- Research files: `G2_MQ4_LODO_FOLD_CONTEXT_SUMMARY_2026-10-30.json` and `G2_MQ4_LODO_FOLD_CONTEXT_AND_REFERENCE_LEAKAGE_2026-10-30_AR.md`. Full locally regenerated JSON SHA256 `f18c25fb0647aa9ee84688e05977b173d542d80f8a65313e2434fa80fff04629`.
- [G2 Actions #40996790837](https://github.com/HazRob/HazRob-Tank/actions/runs/40996790837), job `123871672937`: **419/419 PASS**, deterministic audit/triage replay PASS, publication guard PASS; `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`. Artifact `12951278749`, ZIP SHA256 `6c412172faee7dc51cbc33884b0ea6d8a1d8699f40b72c6296a0626a0bf7b070`.
- Calendar-day LODO is hypothetical and not physically independent evidence. No actual model trained, no verified feature usage, no source calibration/physical grouping/blind labels. No G2/G3 scientific approval. All changes limited to `research/implementation_execution/` on research branch; ChemCore Stable, Android, firmware and `main` unchanged. Verify final documentation-checkpoint CI, then obtain independent physical exposure/session/device provenance, traceable references and preregistered blinded grouped holdouts.

---

## 2026-10-30 — G2 MQ4 multiscale lag-context embargo (411/411 CI)

- Resumed from research checkpoint `ff7106e21bdb5de6d5e45dd9f79e28faee143651`; verified preceding 20/20 CI successes and original pinned 80-technique/14-source scope. Independent evidence remains `records=[]`, source-ready 0/14, `g2_pass_candidates=0`, `g2_blocked=80`.
- SHA256-verified original Library archive `HazRob_MQ_Reference_Real_Measurements_2026-10-30.zip` (`7112a370c7b21445f9e6ebb6d50e08059c352290ee6da23b0b735f5a9583afc9`) and original raw member (`3c27c4f8b8e8aab54b6433acfa41de00890a9eefd19376259a2371f22a48ff2d`); 14,186 rows, 11 continuous one-minute segments. No source promotion.
- Added research-only `g2_mq4_multiscale_embargo_audit.py`, six synthetic tests, pinned result `G2_MQ4_MULTISCALE_DAY_CONTEXT_EMBARGO_2026-10-30.json`, and Arabic methods note `G2_MQ4_MULTISCALE_DAY_CONTEXT_EMBARGO_2026-10-30_AR.md`. Exact consecutive one-minute lag features 1/5/15/30/60/120 yield respectively 10/50/150/300/600/1200 cross-calendar-day context rows; valid lag-pair totals 14175/14131/14031/13881/13581/12981. All rows reconciled, byte-identical local raw replay. Conditional feature-context diagnostics, NOT actual train/test leakage proof or independent physical group evidence.
- [G2 Actions run 40991230087](https://github.com/HazRob/HazRob-Tank/actions/runs/40991230087) on `85db2fa4fd2fda0c34c0f70008a45e3dec99ae95`: **411/411 PASS**, deterministic audit/triage replay PASS, research publication guard PASS, 80 blocked, 0 candidates, 14 sources blocked. Artifact `12948688780`, SHA256 `93d761039335f6298cad0db634356cf6eacbb6f0199a78fc96c55d74f8e5d202`.
- Changes limited to `research/implementation_execution/` on research branch; no ChemCore Stable, Android, firmware or main modifications. No G2/G3 scientific approval. Pending: independent original exposure/sample/session/device provenance, traceable reference calibration, and preregistered blinded physically grouped holdouts. Verify documentation-checkpoint CI on final HEAD.

---

## 2026-10-30 — G2 MQ4 calendar-day lag60 feature-lineage / embargo diagnostic (405/405 CI)

- Resumed from `576bf612ddc3b828d7839385d816b1aa49457a07`; verified 20/20 preceding CI runs successful. Central pinned 80 techniques and 14 external source slots unchanged; independent evidence records=[].
- Independently SHA256-verified original user Library ZIP `HazRob_MQ_Reference_Real_Measurements_2026-10-30.zip` (archive `7112a370c7b21445f9e6ebb6d50e08059c352290ee6da23b0b735f5a9583afc9`, raw member `3c27c4f8b8e8aab54b6433acfa41de00890a9eefd19376259a2371f22a48ff2d`). Recomputed 14,186 rows, 11 continuous one-minute segments, 13,581 strict lag60 pairs. Of these, **600 pairs cross calendar-day boundaries**, 60 for each of 2021-07-22 through 2021-07-31; 600 distinct later-day rows would require previous-day context IF lag60 features were used.
- Research-only `g2_mq4_temporal_continuity_audit.py` now emits cross-day lag60 counts, per-day breakdown, later-day context count, and overlap flag. Added four synthetic regressions including 30s/90s discontinuity and two full midnight boundaries. Initial CI test failure was a short synthetic fixture (1501 vs required 1561 rows), fixed in `b81db0eefcb5581f809112f75808c2333c5e023e`.
- [G2 Actions run 40985855043](https://github.com/HazRob/HazRob-Tank/actions/runs/40985855043), job `123836580618`: **405/405 PASS**, byte-identical audit/triage replay PASS, publication guard PASS; `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`. Artifact `12947460368`, ZIP SHA256 `568b4e73acb921c771fbe1672bf77f82283d46ddbc5b4b8827e10f4df0d391f9`. All 21/21 CI runs on the corrected test commit succeeded.
- Research-only evidence: `G2_MQ4_CALENDAR_DAY_LAG60_EMBARGO_2026-10-30.json` and `G2_MQ4_DAY_BOUNDARY_LAG60_FEATURE_LEAKAGE_2026-10-30_AR.md`. No ChemCore Stable, Android, firmware, or main changes. This is a potential **feature-context** overlap diagnostic, NOT evidence that a trained model used lag60, that exposures are physically identical, or that G2/G3 is qualified.
- Next: verify CI on documentation checkpoint; obtain independent original exposure/sample/session/device provenance, traceable methane reference calibration, and preregistered blinded physically grouped holdouts before any scientific promotion.

---

## 2026-10-30 — G2 MQ4 strict contiguous 60-minute lag audit (401/401 CI)

- Resumed from `9cd049f0e2b4fe7c8eb959ec2bb02149b88e2c4b`; previous G2 and associated CI runs succeeded. Pinned G2 80-entry catalog and 14 external source slots unchanged; independent physical evidence remains absent.
- Fixed an endpoint-only 3600-second lag rule in `g2_mq4_temporal_continuity_audit.py`: now every intervening interval must be exactly 60 seconds. Segments split on any non-60-second gap; added segment lengths, count and cross-day span count. Three synthetic regressions include a compensating 30s+90s gap that must NOT count as a continuous 60-minute pair.
- Independently re-read original user Library ZIP `HazRob_MQ_Reference_Real_Measurements_2026-10-30.zip` (SHA256 `7112a370c7b21445f9e6ebb6d50e08059c352290ee6da23b0b735f5a9583afc9`), member SHA256 `3c27c4f8b8e8aab54b6433acfa41de00890a9eefd19376259a2371f22a48ff2d`. Actual 14,186 rows / 10 gaps / 11 continuous segments, lengths [1406,1565,1388,5,1454,2495,675,1367,995,1417,1419]; 9 segments span calendar days and 10 midnight-adjacent pairs. Strict lag60 pairs = 13,581, identical to prior count for THIS source; no change to earlier numerical interpretation.
- [G2 Actions run 40979161051](https://github.com/HazRob/HazRob-Tank/actions/runs/40979161051), job 123814563468: **401/401 PASS**, deterministic replay and publication guard PASS, `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`. Code `c627981b8a74f2677ec19b95ba5dea4f5f0e55e8`; tests `1ed6e62a05e323680c0eb6a2c3eca93a1ac645f5`.
- Evidence: `G2_MQ4_STRICT_CONTIGUOUS_SEGMENTS_2026-10-30.json` and `G2_MQ4_STRICT_LAG60_AND_SEGMENT_RECONCILIATION_2026-10-30_AR.md`. Research branch only; no ChemCore Stable, Android, firmware, or main edits. No G2/G3 scientific promotion.
- Next: verify all CI on documentation checkpoint; source-qualified physical exposure IDs, independent device/session provenance, traceable concentration reference calibration and preregistered blinded grouped holdouts remain required.

---

## 2026-10-30 — G2 original MQ4 temporal-dependence and catalog-revision audit

- Resumed from 8310a0118c656f94e8346fc243fa76328e388ed1 (20/20 prior CI successes). Central pinned 80-entry blob unchanged; independent evidence records=[]; 0/14 source-ready.
- SHA-verified original UMN MQ4 14,186-row Library ZIP: 14,175 one-minute pairs, 10 gaps, 10/10 contiguous midnight crossings; reference methane lag-1 correlation 0.996812 and MQ4 channel lag-1 correlations 0.997290–0.998305. Calendar day labels are not independent physical holdout evidence. Reference ppb and MQ4 ohm units preserved.
- Current build-harness catalog main blob 248e6006a3bac6f814a6e92d9d334c3d7fb4cf73 has 94 entries; first 80 identical to G2 pinned blob 1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f. Fourteen appended entries (11 MQ-EXT and 3 FOOD_ONLY), none measurement-qualified. Do not silently rebase locked G2 scope or mix food into gas ChemCore.
- Added research-only temporal audit code, 6 synthetic regressions, raw-result JSON, catalog-revision JSON and Arabic documentation G2_MQ4_TEMPORAL_CONTINUITY_AND_CATALOG_REVISION_2026-10-30_AR.md. CI verified on research commit 8f224ea7df8873c33fdc786c34571bbe19b21819: GitHub Actions G2 run 40973482997, job 123795729064, 398/398 tests PASS, deterministic audit/triage replay PASS, publication gate PASS, g2_pass_candidates=0, g2_blocked=80, source_blocked=14; artifact 12940599902 sha256:6515ab45dba0c2454b591009f021f2d1101e20013d75730e01006a0f04c84dea. All 21/21 associated push/PR workflows succeeded. No ChemCore Stable/Android/firmware/main edits or G2/G3 scientific promotion.

---

## 2026-10-30 — G2 declared-sample weighting and new MQ4 raw-library intake (392/392 CI)

- Resumed from `8f667942a4139bdc4d8f8b98d0c897b81faa4ff7`; verified prior 20/20 CI successes. Central catalog pinned Git blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` (80 references, 14 external datasets); central source readiness 0/14; `G2_INDEPENDENT_EVIDENCE.json` remains `records=[]`.
- Added optional equal-**declared-physical-sample** weighting to `g2_paired_prediction_replay.py` alongside row-weighted paired accuracy. It detects pseudoreplication-driven sign reversals and reconciles per-sample counts against per-group totals. When `physical_sample_id` is absent the diagnostic is `null`. No authentication of physical sample identity, no train/test independence claim, no promotion.
- Four new synthetic tests cover 9:1:1 repeated-reading sign reversal, unique-sample equality, Arabic IDs/order invariance, and absent metadata. A transient fixture newline-escaping failure was fixed in `7644188c60a9d5ab9920fd85a564c4fbc4e27b9d`; final G2 [Actions #40965959154](https://github.com/HazRob/HazRob-Tank/actions/runs/40965959154) job `123770402613`: **392/392 PASS**, byte-identical audit/triage replay PASS, publication guard PASS; `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`; artifact `12937683269` SHA256 `1f795bc9fb92992872e0d70a7105829fd4a18967923201ff348f03f5816cb84a`.
- Audited new user Library ZIP `/HazRob/MQ_Reference_Measurements_2026/HazRob_MQ_Reference_Real_Measurements_2026-10-30.zip` (archive SHA256 `7112a370c7b21445f9e6ebb6d50e08059c352290ee6da23b0b735f5a9583afc9`). UMN methane source DOI `10.13020/13xf-nw93` has 14,186 minute-averaged rows, 11 dates, three MQ4 ohm channels, reference `licor_ch4` in **ppb**, source SHA256 `3c27c4f8b8e8aab54b6433acfa41de00890a9eefd19376259a2371f22a48ff2d`. A separate MQ3/MQ4/MQ8 voltage log has 114 rows, 110/114 missing CO2 sentinel values. Both remain **unqualified research intake**; original pulse-level grouping and calibration traceability not yet independently audited. No silent addition to pinned central catalog.
- Documentation: `G2_DECLARED_SAMPLE_EQUAL_WEIGHT_PSEUDOREPLICATION_DIAGNOSTIC_2026-10-30_AR.md`, `G2_MQ_LIBRARY_RAW_INTAKE_2026-10-30_AR.md`, and machine-readable `G2_MQ_LIBRARY_RAW_INTAKE_2026-10-30.json`.
- Final documentation checkpoint `3d2c90a6b84447b7fbb90867e459c8f4c7b73e75`: **20/20 CI workflows SUCCESS**. Final [G2 Actions #40966310206](https://github.com/HazRob/HazRob-Tank/actions/runs/40966310206), job `123771610440`: **392/392 PASS**, deterministic replay and publication guard PASS; artifact `12937503736`, SHA256 `5633ba53178bb2a8266df7ee38d41cbba06dcea6909dde40a9aec2f916717cf7`.
- Only research branch `research/implementation_execution/` modified; ChemCore Stable, Android, firmware, and main untouched. Scientific G2/G3 **not** approved. Next: obtain original physical sample/session provenance and traceable references, preregister grouped blind holdout, then reassess source readiness.

---

## 2026-10-30 — G2 declared physical-sample cross-group leakage guard (388/388 CI)

- Resumed from `cb434a401a6c990fa2a2bde35d7a32df3dddb267`; verified 20/20 prior CI runs successful. Central catalog blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` (80 references / 14 external datasets), 0/14 source-qualified, independent evidence `records=[]`.
- Research-only paired replay now optionally accepts `physical_sample_id`: fail closed if a sample appears in more than one declared physical group or has missing/noncanonical IDs. Repeated exposure reads within one sample/group are counted separately from distinct samples. Missing sample metadata is reported as **not checked**, not a zero-leakage finding. Declared sample identity is NOT authenticated independent physical provenance.
- Code commit `24f0ef1a3aeb33a71bebf90aaecd5e9527e949ad`; five synthetic regression tests commit `0a746e5f60ebec4a854c90a07a3be3240d580652`.
- [G2 CI #40957596290](https://github.com/HazRob/HazRob-Tank/actions/runs/40957596290), job `123741611646`: **388/388 PASS**, deterministic byte-identical audit/triage replay PASS, research publication gate PASS; `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`. Artifact `12933719604`, SHA256 `5793245ecfb2052c014ec33107323bee04c310335b5b63bc6cd767da44c40bb6`.
- Evidence note: `G2_PHYSICAL_SAMPLE_CROSS_GROUP_LEAKAGE_GATE_2026-10-30_AR.md`. Only `research/implementation_execution/` on the research branch modified; no ChemCore Stable, Android, firmware or main changes. No G2/G3 scientific promotion.
- Next: verify documentation checkpoint CI; acquire independent original physical measurements, traceable reference concentrations, preregistered blinded holdouts, and independently authenticated device/session/sample/exposure provenance.

---

## 2026-10-29 — G2 paired true-reference class × physical-group discordance (383/383 CI)

- Resumed from `ff03433a2fc0c6cc10efe68e68ac71f4e51da458`, with 20/20 previous CI runs successful. Reverified central catalogue Git blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` (80 records / 14 external measurement datasets). Source-ready 0/14; independent evidence `records=[]`.
- Research-only `g2_paired_prediction_replay.py` now reconciles both-correct, both-wrong, model-only-correct, and baseline-only-correct for every observed true-reference-class × declared-physical-group cell, including exact class and group marginal checks. For each class, it reports equal-observed-group versus exposure-weighted paired accuracy deltas, group extrema, opposite-sign effects and weighting sign reversal. Missing cells are never imputed; physical group/label provenance is NOT independently authenticated.
- Code commit `a68aab2a7b82d160bbebf9a5fa8802b0a4329406`; four new synthetic regression tests commit `6754365a437d3b5cce89eecbbdc7fa7fe5c30a71`; corrected synthetic CSV newline escaping in `54c932fd372be2d5bf8169910e6ae8274aa55ed3` before CI.
- [G2 CI #40948094583](https://github.com/HazRob/HazRob-Tank/actions/runs/40948094583), job `123708659153`: **383/383 PASS**, deterministic byte-identical audit/triage replay PASS, research publication guard PASS; `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`. Artifact `12928873712`, ZIP SHA256 `75ab4bb65b6175fc97f516718985580c478c766ed883de40b2e2c9d4010285d1`.
- Evidence note: `G2_CLASS_BY_GROUP_PAIRED_DISCORDANCE_AND_WEIGHTING_2026-10-29_AR.md`. Research branch only; ChemCore Stable, Android, firmware and main unchanged. No scientific G2/G3 approval.
- Next: verify CI on final documentation checkpoint; obtain independently sourced original raw physical exposures with traceable reference concentrations, device/session/exposure provenance, preregistered blinded grouped holdouts and independently verified post-blind labels.

---

## 2026-10-29 — G2 true-reference class × physical-group support (379/379 CI)

- Resumed from `32daa568deca9d0d18e1f2acf37625745391441e`; verified 20/20 CI successes on that checkpoint. Central catalog blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` remains 80 records / 14 external datasets, source-ready 0/14, independent evidence `records=[]`.
- Research-only `g2_paired_prediction_replay.py` now cross-tabulates each true-reference class against each declared physical group, reports class group coverage and single-group-only classes, and reconciles class/group marginal counts with the original paired prediction rows. This exposes class-group confounding not visible in pooled or equal-class accuracy. Group independence and label authenticity remain unverified.
- Code commit `5c76feae6cc186d51c01e743d83b003a81f92456`; four new synthetic regression tests commit `011998bedcae5e31ea53f51c60e2182af05d01cb`.
- [G2 CI #40940130672](https://github.com/HazRob/HazRob-Tank/actions/runs/40940130672), job `123681007502`: **379/379 PASS**, byte-identical audit/triage replay PASS, publication gate PASS; `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`. Artifact `12926672056`, ZIP SHA256 `5d30d4abb159317eb1488634721c93be7f3d06d8453870f53b79ad46a008d5dc`.
- Evidence note: `G2_TRUE_REFERENCE_CLASS_BY_PHYSICAL_GROUP_SUPPORT_2026-10-29_AR.md`. Changes only in `research/implementation_execution/` on research branch; no ChemCore Stable, Android, firmware, or main changes. No scientific G2/G3 approval.
- Next: verify documentation checkpoint CI; acquire source-qualified independent physical exposures with traceable concentration standards, device/session/exposure provenance and preregistered blinded grouped holdouts.

---

## 2026-10-29 — G2 paired true-reference-class imbalance diagnostic (375/375 CI)

- Resumed from `4db7c8bd3f291c247066c67c20d201e20e764f20`; verified its 20/20 GitHub Actions success. Pinned central catalogue blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` (80 entries / 14 external datasets), source-ready 0/14, independent evidence `records=[]`.
- Research-only `g2_paired_prediction_replay.py` now recomputes per-true-reference-class paired outcomes and equal-class mean model-minus-baseline accuracy. This flags majority-class masking of minority-class regression; it is descriptive, NOT label authentication, independent physical validation, a significance test, or G2/G3 promotion.
- Three synthetic regression tests: exact per-class paired reconciliation, majority/minority weighting-direction reversal, row-order invariance with Arabic labels. Initial commit `195b18585c647f821e890aa4f6abe76405d3afbc`; corrected synthetic CSV fixture newline escapes in `72c738bb993f67487a37cb7b961dd9c8dfc24c68`.
- [G2 CI #40930981571](https://github.com/HazRob/HazRob-Tank/actions/runs/40930981571), job `123649012511`: **375/375 PASS**, byte-identical audit and triage replay PASS, publication gate PASS; `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`; artifact `12922332228`.
- Evidence note: `G2_PAIRED_TRUE_REFERENCE_CLASS_IMBALANCE_DIAGNOSTICS_2026-10-29_AR.md`. Research branch only; no ChemCore Stable, Android, firmware or `main` changes. No scientific G2/G3 approval.
- Next: verify CI on documentation checkpoint, then acquire original source-qualified independent physical exposures with traceable concentrations, device/session/exposure provenance and preregistered blind group holdouts.

---

## 2026-10-29 — G2 full CSV provenance semantic digest (372/372 CI)

- Resumed from `9c3e877c9d456bf55de3c802c5097136faa331c8`; verified prior 20/20 CI success. Central reference catalogue remains pinned at blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` (80 entries / 14 external measurement datasets); readiness 0/14, independent evidence `records=[]`.
- Research-only `g2_paired_prediction_replay.py` now emits `prediction_full_rows_semantic_sha256` and `metrics_full_rows_semantic_sha256`, binding ALL optional CSV provenance columns as well as scoring fields, invariant to row/column reordering. This closes a digest gap in which changing an optional device/session/reference field did not affect the earlier prediction-only semantic fingerprint. Hashes do not authenticate asserted metadata.
- Four new synthetic regression tests in `test_g2_paired_prediction_replay.py`. Code SHA `9a9173dadb98d826fa62da9b12778b55ffb9208e`; test SHA `468eba1742a21494061725e8d005938d2dd5b91d`.
- [G2 CI #40920322296](https://github.com/HazRob/HazRob-Tank/actions/runs/40920322296), job `123611470309`: **372/372 PASS**, byte-identical audit and triage replay PASS, research publication gate PASS; `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`. Artifact `12916709117`, ZIP SHA256 `ac2697aafbe0ebea634f188f4e8b7572de5db1ab19956b76b3bc241d999ffa67`.
- Evidence note: `G2_FULL_CSV_PROVENANCE_SEMANTIC_DIGEST_2026-10-29_AR.md`. Changes restricted to research branch `research/implementation_execution/`; no ChemCore Stable, Android, firmware or `main` edits. No scientific G2/G3 promotion.
- Next: verify CI for this documentation checkpoint; prioritize source-qualified original raw independent exposures with traceable reference concentrations and preregistered blind physical group holdouts. Avoid interpreting synthetic test coverage as gas calibration.

---

## 2026-10-29 — G2 per-group paired discordance and semantic prediction fingerprint (368/368 CI)

- Resumed from `8ec21b7a340b4a4b26439aa7180be69226d99071`, verified 11/11 previous CI success and pinned central catalog blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` (80 references, 14 external datasets). Readiness remains 0/14; independent evidence `records=[]`.
- Research-only `g2_paired_prediction_replay.py` now emits mutually exclusive paired correctness/discordance counts **for each declared physical group**, cross-checks group arithmetic, and computes row-order-invariant `prediction_rows_semantic_sha256` including true/model/baseline labels. This exposes opposite group-level effects hidden by a pooled tie and detects label changes even when accuracy and group assignment are unchanged.
- Four synthetic regression tests. Code SHA `aed622546089dda2178ec740f3d217dc44a5e37b`, initial tests SHA `9127e175a15b35e17c37d868eba193cd9706827e`. Intermediate G2 CI [#40911311715](https://github.com/HazRob/HazRob-Tank/actions/runs/40911311715) failed due to escaped newline strings in two new CSV fixtures; corrected in `e29ead32965c594bbc3d9e82202e5b150e826e5d`.
- [G2 Actions #40911567612](https://github.com/HazRob/HazRob-Tank/actions/runs/40911567612), job `123580609382`: **368/368 PASS**, byte-identical audit/triage replay and research publication gate PASS, `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`. Artifact `12913767097`, ZIP SHA256 `0208d0056c25ef8bb22bfbca437d3415c11262b7091b774b23822c631ac200da`. 11/11 workflow runs succeeded on test-fix commit.
- Evidence note: `G2_PAIRED_GROUP_DISCORDANCE_AND_SEMANTIC_PREDICTION_FINGERPRINT_2026-10-29_AR.md`. All changes within `research/implementation_execution/` on research branch. ChemCore Stable, Android, firmware untouched. No scientific G2/G3 promotion.
- Next: check CI on this documentation checkpoint, then seek source-qualified independent raw physical exposures with traceable concentration references and preregistered blinded grouped holdouts. Do not infer independent physical validation from synthetic arithmetic tests.

---

## 2026-10-29 — G2 paired post-blind exposure-row replay and cross-CSV reconciliation (364/364 CI)

- Resumed from `1ea14d32a9fc61afde0537573bade25b135a42eb`; checked previous G2 CI and pinned central reference catalog Git blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` (80 references, 14 external datasets). Pinned readiness remains 0/14; independent evidence `records=[]`.
- New research-only `g2_paired_prediction_replay.py` recomputes model and baseline correctness from the SAME post-blind `exposure_id` rows, cross-checks exact per-physical-group metrics, reports both-correct/both-wrong and discordant outcomes, and pins exact input CSV SHA256 plus sorted exposure-to-group assignment digest. Fails closed on duplicate exposure IDs, mismatched counts, invalid CSV and noncanonical physical IDs. It cannot establish physical provenance or blind release timing.
- Ten synthetic regression tests added in `test_g2_paired_prediction_replay.py`. Code commit `beb68455a96e20c1ee687bc746cb6b9e9d36cbba`, tests commit `8c09ce1bc4114f5519ce1c9245b20bb8b1b340ac`. [G2 CI #40900342477](https://github.com/HazRob/HazRob-Tank/actions/runs/40900342477), job `123540583951`: **364/364 PASS**, double replay and publication gate PASS; `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`. Artifact `12909641085`, SHA256 `857421a7996a76a03e16f01b52ad609b243b8956ec99e55d5e11fa3844feedfe`.
- Evidence note: `G2_PAIRED_BLIND_PREDICTION_ROW_REPLAY_2026-10-29_AR.md`. Research branch only; ChemCore Stable, Android and firmware unchanged. No scientific G2/G3 promotion.
- Next: check documentation-checkpoint CI; acquire independently sourced original raw physical exposures, traceable concentration standards, preregistered device/session/exposure-group holdouts and externally audited post-blind labels. Run the paired-row tool only on qualified evidence; synthetic tests are not gas measurements.

---

## 2026-10-29 — G2 physical-group weighting sensitivity and CSV header integrity (354/354 CI)

- Resumed from `13b94f2934f1fa0eab23f37ce7c1cfc1414f9459`, 20/20 previous CI successes. Rechecked central catalogue Git blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` (80 records / 14 external datasets), readiness 0/14 and independent evidence `records=[]`.
- Research-only `g2_paired_delta_diagnostic.py` now reports equal-group versus exposure-weighted delta, weighting direction disagreement, leave-one-group-out maximum shift and direction reversal count; no inferential significance or scientific promotion. Diagnostic CSV reader now rejects NFKC-unstable header aliases, including extra fields; valid Arabic header control preserved.
- Added six synthetic regression tests. An intermediate test-fixture newline escaping error caused CI failure at `943dc5a9`; corrected at `f832fadce0ea5c8d5cbe063c77462a36f7528228`. [G2 Actions #40891221189](https://github.com/HazRob/HazRob-Tank/actions/runs/40891221189), job `123508232777`: **354/354 PASS**, byte-identical replay PASS, publication guard PASS, `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`, artifact `12905330781`.
- Research branch only. No ChemCore Stable, Android, firmware changes or G2/G3 promotion. Evidence note: `G2_PAIRED_WEIGHTING_REVERSAL_AND_CSV_HEADER_GATE_2026-10-29_AR.md`.
- Next: verify documentation-checkpoint CI; acquire independently measured physical exposures with traceable concentration references, source hashes, device/session/exposure provenance, preregistered physical-group splits and blinded holdouts before scientific review.

---

## 2026-10-29 — G2 paired group heterogeneity and leave-one-out sensitivity (348/348 CI)

- Resumed from research HEAD `1e880b5bccef33b8cac1c947fe231ce8ac87abe6` after checking the prior 345-test G2 run and current GitHub Actions status. Rechecked central catalog `HazRob/build-harness/library/reference-bank/external-research/master_catalog.json`, blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` (80 records / 14 external datasets); independent evidence `records=[]`.
- Expanded research-only `g2_paired_delta_diagnostic.py` with model/baseline accuracies, counts and extrema of positive/negative/tied per-group effects, leave-one-physical-group-out pooled delta bounds, largest exposure-group fraction, and descriptive Kish effective group count. This exposes heterogeneity and group-weight concentration; it is NOT proof of physical independence, power, or G2/G3 qualification.
- Three synthetic regression tests added for mixed-sign unequal groups, 99% group dominance and deterministic group-order invariance. Code commit `27016f671f16da6a59d3a144135d5144c2048318`; test commit `69b839761ccef233da71d923062a97af2724b252`.
- [G2 Actions #40881408497](https://github.com/HazRob/HazRob-Tank/actions/runs/40881408497), job `123473588931`: **348/348 PASS**, byte-identical replay PASS, publication guard PASS; `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`. Artifact `12900526114`, ZIP SHA256 `f5fd172efeb8d027a18f9ae02ce049dbf254032a6e71549015d001f0f11fde98`.
- Research branch only; no ChemCore Stable, Android, firmware, or scientific G2/G3 promotion. See `G2_PAIRED_GROUP_HETEROGENEITY_LOO_DIAGNOSTICS_2026-10-29_AR.md`.
- Next: verify documentation checkpoint and any pending CI. Obtain source-qualified original physical exposures, traceable concentration standards, device/session/exposure provenance, preregistered group splits, and independent blinded holdouts before promotion.

---

## 2026-10-29 — G2 paired physical-group delta diagnostic (345/345 CI)

- Resumed from `1794f6b9fd9f3ed6e42ef38799870085cdfb82de`; checked prior successful G2 CI and central catalogue blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` (80 entries / 14 external datasets). Independent evidence `records=[]`; source readiness 0/14.
- Added research-only `g2_paired_delta_diagnostic.py` for deterministic paired cluster bootstrap of model-minus-baseline accuracy from the same physical test groups, plus CSV byte SHA256 lineage. Unlike marginal intervals or row resampling, paired resampling preserves within-group comparison. It is a diagnostic, not a new scientific promotion rule or a p-value.
- Added nine synthetic regression tests in `test_g2_paired_delta_diagnostic.py`: paired equality/improvement, deterministic group-order invariance, invalid or NFKC-aliased group IDs, invalid/missing counts, CSV schema and byte lineage, invalid protocol. Code SHA `029559d216737265e507b801727e7f55b6a7bc70`; test SHA `c0ce51b6c2b919667e543e252b7c0c42d975785f`.
- [G2 CI #40872313640](https://github.com/HazRob/HazRob-Tank/actions/runs/40872313640): **345/345 PASS**, byte-identical replay PASS, publication guard PASS; `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`; artifact `12895579575`, ZIP SHA256 `287b2d4029891f73ed4394794560de2771306e3278d05467176db4ef917f383e`.
- Research branch only. No ChemCore Stable, Android or firmware changes, and no G2/G3 scientific promotion. Documentation: `G2_PAIRED_PHYSICAL_GROUP_DELTA_DIAGNOSTIC_2026-10-29_AR.md`.
- Next: verify documentation-checkpoint CI; acquire independent original physical exposures, traceable concentration references, preregistered physical group splits and blind holdout before considering any G2 candidate.

---

## 2026-10-29 — G2 early NFKC CSV header gate in evidence bundle (336/336 CI)

- Resumed from `7d8b2b3bf3735b30bcf294dbb8f3991eac02c52c`; verified prior CI and central catalogue Git blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` (80 references, 14 external datasets). Pinned source readiness remains 0/14; independent evidence `records=[]`.
- Closed an early-bundle schema ambiguity: `g2_evidence_bundle_gate.py` now rejects NFKC-unstable CSV header names even when the evidence file has a matching SHA256. The later cross-artifact gate already rejected such aliases; this change makes the earlier bundle-level integrity result fail closed independently.
- Added three synthetic tests in `test_g2_bundle_csv_nfkc_gate.py`, including fullwidth/decomposed negatives and a valid Arabic column control. Code commit `bf028dd520973e6fe6f5ea3f02840f3539161e97`; test commit `6838a2a82cb5aeac51fca3c35ac7fc44262669f5`.
- [G2 Actions #40865031754](https://github.com/HazRob/HazRob-Tank/actions/runs/40865031754), job `123417510648`: **336/336 PASS**, byte-identical replay PASS, publication guard PASS; `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`; artifact `12892244820`, ZIP SHA256 `47e08878b80b14e145241b1551f95a6f0d4bb006b62be3c221282b0f4f022c47`.
- Research branch only. No ChemCore Stable, Android or firmware changes, and no scientific G2/G3 promotion. Documentation: `G2_BUNDLE_CSV_HEADER_NFKC_EARLY_GATE_2026-10-29_AR.md`.
- Next: verify documentation-checkpoint CI and acquire original independently measured exposures, traceable concentration standards, physical provenance, and preregistered grouped blind holdouts.

---

## 2026-10-29 — G2 NFKC evidence CSV header identity (333/333 CI)

- Resumed from `46dd80de1bc29d118aec72b8f24924b720355173` (prior G2 CI 327/327 PASS). Central catalog blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` contains 80 entries / 14 external datasets; 0/14 review-ready; independent evidence `records=[]`.
- Closed a distinct CSV schema alias gap: NFKC compatibility aliases of header names such as `ｅxposure_id` or `ｄevice_id` now fail closed with `CSV_HEADER_AMBIGUOUS` before any evidence-field interpretation. Six synthetic regression controls added; multilingual canonical headers preserved.
- Code/test commit `ff04494fc99377bf673dd3564c544b594a385466`; [G2 Actions #40857166203](https://github.com/HazRob/HazRob-Tank/actions/runs/40857166203), job `123390916550`: **333/333 PASS**, double replay and publication PASS, `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`; artifact `12889014481`, SHA256 `2b418c362804a37bb879c98f6a78010ef3f490b2a944202a128f0f1c1d0bcdbe`.
- Research branch only. No ChemCore Stable/Android/firmware modifications or scientific G2/G3 promotion. See `G2_CSV_HEADER_NFKC_ALIAS_FAIL_CLOSED_2026-10-29_AR.md`. Next: verify documentation CI and acquire independent physically referenced blind data.

---

## 2026-10-29 — G2 canonical exposure IDs across evidence CSVs (327/327 CI)

- Resumed from `237476c31ad55b11575413ed89b2550c773a8e38`; verified prior G2 CI success and central catalog blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` (80 references, 14 external datasets), pinned source readiness 0/14 and independent evidence `records=[]`.
- Fixed a distinct observation identity gap: NFC-stable Unicode compatibility aliases such as `e1` / `ｅ1` in `exposure_id` could evade exact duplicate and cross-CSV joins. `g2_cross_artifact_gate.py` now emits `NONCANONICAL_EXPOSURE_ID:<CSV_NAME>` for noncanonical exposure identifiers in group, prediction or optional per-group CSV columns; chemical labels and measured values remain untouched.
- Added four synthetic negative/positive regression tests in `test_g2_physical_source_nfkc_gate.py`. Code SHA `17fab3c5b7b4aa6aa55664848e305c33a6a2f114`; test SHA `9e979c3d9eb8a8b71dfc9b46b78051286afba7b4`. [G2 CI #40848756722](https://github.com/HazRob/HazRob-Tank/actions/runs/40848756722), job `123362676459`: **327/327 PASS**, double replay and publication PASS, `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`, artifact `12885787664` (ZIP SHA256 `b1c84f3d803d10816916fc2b1d734b477b24ebf7019e653c3b674c38f9910d0c`).
- Research branch only; no ChemCore Stable, Android or firmware changes. No G2/G3 scientific promotion. See `G2_CANONICAL_EXPOSURE_ID_CROSS_CSV_GATE_2026-10-29_AR.md`.
- Next: recheck final documentation CI, then acquire original independent physical exposures, traceable concentration references, device/session/exposure provenance, and preregistered grouped blind holdouts.

---

## 2026-10-29 — G2 NFKC physical source column identity (323/323 CI)

- Resumed from `f877276234d481a2db1ae20e2e697d9dd9cebc4d`, confirmed prior G2 CI success; central catalog blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` (80 references, 14 external datasets), pinned readiness 0/14, independent evidence `records=[]`.
- Closed a distinct physical holdout alias gap: NFC-valid compatibility characters in original `device_id`/`session_id`/`batch_id` CSV source columns could make one physical unit appear as different train/test units despite canonical `group_id` strings. `g2_cross_artifact_gate.py` now rejects NFKC-unstable physical source identities with `NONCANONICAL_PHYSICAL_SOURCE_ID`, while preserving ordinary multilingual labels and existing missing-value checks.
- Four synthetic regression tests in `test_g2_physical_source_nfkc_gate.py`. Code commit `be797fcf1a2cd653ed9d0df6f7b05a889d818389`, tests commit `ada445a5d227111060809607829972e036e7741f`. [G2 CI #40841945615](https://github.com/HazRob/HazRob-Tank/actions/runs/40841945615), job `123340162063`: **323/323 PASS**, `g2_pass_candidates=0`, `g2_blocked=80`, artifact `12883500714`. See `G2_NFKC_PHYSICAL_SOURCE_COLUMNS_FAIL_CLOSED_2026-10-29_AR.md`.
- Research branch only; no ChemCore Stable, Android or firmware edits. No scientific G2/G3 promotion. Next: recheck final documentation CI; acquire independent original physical exposure data, traceable concentration references, device/session/exposure provenance, and locked grouped blind holdouts.

---

## 2026-10-29 — G2 NFKC compatibility aliases for physical holdout IDs (319/319 CI)

- Resumed from `6af9fe99f45a0091f07cb71a80efc8d3bb3f17da`; previous G2 CI succeeded. Re-read central catalog `HazRob/build-harness/library/reference-bank/external-research/master_catalog.json`, blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` (80 entries, 14 external datasets). Independent evidence remains `records=[]`; source readiness 0/14.
- Found a holdout identity ambiguity: NFC-only validation accepted Unicode compatibility aliases (fullwidth Latin, circled/superscript digits, ligatures). `g2_evidence_audit.py` now requires NFKC-stable IDs for train/calibration/test partitions, failing closed with `NONCANONICAL_PHYSICAL_GROUP_ID`. Canonical ASCII, NFC Arabic and accented Latin controls remain accepted.
- Code SHA `2ec69c5cb3f5245d614c907bd4d5e66d86d54234`; tests SHA `cff33c767f4d8d534ceead8c52a720c4f0fa57ac`. [G2 CI #40834996094](https://github.com/HazRob/HazRob-Tank/actions/runs/40834996094), job `123317591771`: **319/319 PASS**, 80-technique audit 0 candidates / 80 blocked, double replay and publication guard PASS, artifact `12879788085`.
- Documentation: `G2_NFKC_PHYSICAL_GROUP_ALIAS_FAIL_CLOSED_2026-10-29_AR.md`. Research branch only; no ChemCore Stable, Android or firmware changes. No G2/G3 scientific promotion. The controls are synthetic, not independent gas measurements.
- Next: check final documentation checkpoint CI; obtain original independently measured raw exposures, traceable concentration references, device/session/exposure IDs and preregistered grouped blind holdouts.

---

## 2026-10-29 — G2 canonical physical group IDs at evidence-register entry (317/317 CI)

- Resumed from `775285129d00399f48dc7b81e85ccd6c09d602cb`; confirmed all 20 previous CI runs successful. Central catalogue current blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` (80 entries, 14 external measurement datasets); independent evidence `records: []`; pinned source readiness 0/14.
- Added early fail-closed `NONCANONICAL_PHYSICAL_GROUP_ID` in `g2_evidence_audit.py` for train/calibration/test physical group IDs. Rejects edge whitespace, invisible/control Unicode, non-ASCII spacing and non-NFC aliases while permitting NFC multilingual IDs and interior ASCII spaces. Added four synthetic regression tests; CSV-level checks remain unchanged.
- Research commit `ad5a0ea555f9a3712dbf09aaae8cb270353c4b9d`: [G2 CI #40828407547](https://github.com/HazRob/HazRob-Tank/actions/runs/40828407547), job `123296692377`: **317/317 PASS**, double replay PASS, publication guard PASS, artifact `12877612206` (ZIP SHA256 `ff75132605cf7a23c805656dcc2a9f0d0ab62dd0c78cbe114dabf55673678e7a`). All **21/21** associated CI runs succeeded. Audit: `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`.
- Changed files: research-only G2 Python gate, new test and `G2_CANONICAL_PHYSICAL_GROUP_ID_ENTRY_GATE_2026-10-29_AR.md`. No ChemCore Stable, Android or firmware modifications. No G2/G3 scientific promotion.
- Next: verify documentation checkpoint CI; obtain independently measured raw physical exposures with traceable concentration references, device/session/exposure IDs, and locked grouped blind holdouts.

---

## 2026-10-29 — G2 nonfinite exponent fail-closed publication (313/313 CI)

- Resumed from research HEAD `a327c32bcb8371392e82eb43cf6703bd79807db0`; central catalogue pinned blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` (80 entries, 14 datasets), source matrix blob `bd69304fc35175b90199c6cc6ad4a88fc10ae93f` (0/14 review-ready), independent evidence `records: []`.
- Fixed Python JSON overflow gap at publication boundary: `parse_constant` rejected NaN/Infinity but not `1e999` parsed as infinite float. Added finite-number parse hook and two regression tests covering invalid exponents/constants and a valid scientific-notation control.
- Code SHA `d8546fe733071c5c41401fe32ce71aa1c5489f27`; test SHA `60a92b7a2e93b305312e5d814578a161bf876373`. [G2 Actions #40823133117](https://github.com/HazRob/HazRob-Tank/actions/runs/40823133117), job `123279939990`: **313/313 PASS**, publication replay PASS, artifact `12875681362` (ZIP SHA256 `ddc9ca0edbc95c058e136cb4386bf6670b03818d2ca2a25512b0a3e9be03ee97`). Audit `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`.
- Research-only Python guard/tests and documentation; no Stable, Android or firmware changes. No scientific G2/G3 promotion. See `G2_PUBLICATION_NONFINITE_EXPONENT_FAIL_CLOSED_2026-10-29_AR.md`.
- Next: verify final documentation checkpoint CI. Acquire original independently measured physical exposures, traceable ppm references, device/session/exposure IDs and preregistered grouped blind holdouts.

---

## 2026-10-29 — G2 independent controlling-input publication replay (311/311 CI)

- Resumed from research HEAD `8edc69702a72d09742fb0c1dfb752085d93b7d81` (20/20 previous CI successes); verified central catalogue blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` (80 references/14 datasets), pinned source matrix `bd69304fc35175b90199c6cc6ad4a88fc10ae93f` (0/14 ready), independent evidence `records: []`.
- Closed a publication-boundary provenance gap: the publisher previously checked only the **shape** of four controlling input SHA256 fields. It now verifies exact bytes of all five inputs and independently rebuilds triage from controlling evidence; self-consistent forged audit/triage or altered ledger/evidence bytes fail closed. Six integration and negative-control tests added.
- Code/workflow commit `e3eb39c7dbd91faddc58c09679c1035472971fa9`: [G2 Actions #40817579479](https://github.com/HazRob/HazRob-Tank/actions/runs/40817579479), job `123262175764`: **311/311 PASS**, publication replay PASS; artifact `12872717811` (SHA256 `60327caa080a18cfb0b2a5a4a23c2f24257f6a4c8eecf51128e4d4f7758320f5`); **21/21** CI runs succeeded on code SHA. `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`.
- Research branch only: G2 workflow, publication gate, new test, plus this note and RESUME. No ChemCore Stable, Android or firmware edits; no G2/G3 scientific promotion. See `G2_PUBLICATION_CONTROLLING_INPUT_REPLAY_2026-10-29_AR.md`.
- Next: verify CI for this documentation checkpoint; acquire source-qualified independent physical raw exposures, traceable ppm references, device/session/exposure IDs and preregistered grouped blind holdouts.

---

## 2026-10-29 — G2 publication acquisition-action/caveat integrity (305/305 CI)

- Resumed from research HEAD `9ec4e2a3f058e3f231757f9329a85b875fa45d70` (20/20 previous CI successes). Reverified central catalogue blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` (80 entries/14 external datasets), pinned source matrix `bd69304fc35175b90199c6cc6ad4a88fc10ae93f` (0/14 ready) and independent evidence `records: []`.
- Fixed rehashed triage publication gap: acquisition instructions can no longer be altered/omitted while retaining valid blockers and counts; unknown source blockers and forged scientific caveat fail closed. All five input SHA256 fields are required and syntactically validated. Added five synthetic negative controls.
- Research code/test SHA `4df264c736a5a259319052d41ed72ccd3fdb3c35`: [G2 Actions #40811858097](https://github.com/HazRob/HazRob-Tank/actions/runs/40811858097), job `123243777899`: **305/305 PASS**; publication gate PASS; audit `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`; artifact `12870458175`, SHA256 `6ac8e8826c9d32e091d2338b7423eaa83ab75e856d76f4b90b5a79bfffcc5971`.
- Research branch only: `g2_acquisition_triage.py`, `g2_artifact_publish_gate.py`, `test_g2_artifact_publish_gate.py`, plus this evidence note and RESUME. No Stable/Android/firmware changes; no G2/G3 scientific promotion. See `G2_PUBLICATION_ACQUISITION_ACTION_INTEGRITY_2026-10-29_AR.md`.
- Next: verify CI on documentation checkpoint. Acquire independently measured original physical exposures, traceable concentration references, device/session/exposure IDs, and preregistered grouped blind holdouts before any scientific promotion.

---

## 2026-10-29 — G2 publication blocker-lineage and frequency consistency (300/300 CI)

- Resumed from research HEAD `bd3b925be7323fbe0c60ebb5039dbab8c87e41fc` after verifying prior G2 success (296/296) and final documentation run #40800046451. Re-read central `HazRob/build-harness/library/reference-bank/external-research/master_catalog.json`: Git blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f`, 80 entries including 14 external measurement datasets. Pinned source matrix `bd69304fc35175b90199c6cc6ad4a88fc10ae93f` remains 0/14 review-ready; independent evidence register `records: []`.
- Closed a publication-boundary gap: a rehashed triage report could change a technique's blocker labels while retaining identical technique IDs, status, and totals. The publication guard now checks each triage technique's blocker set against the corresponding audited `issues`, enforces source blocker/readiness consistency, and recomputes both blocker-frequency dictionaries. Missing, duplicate or malformed blocker entries fail closed.
- Added four negative-control regression tests for forged triage blocker, forged audit blocker, forged source blocker and forged blocker-frequency totals; synthetic fixtures are not experimental data.
- Research code/test commit `7e0f7c78cbba8198dc52942bfc71812a62d82780`: [G2 Actions #40806068181](https://github.com/HazRob/HazRob-Tank/actions/runs/40806068181), job `123225203201`: **300/300 PASS**. Audit `g2_pass_candidates=0`, `g2_blocked=80`; triage `source_blocked=14`. Publication guard PASS; artifact `12868456499`, SHA256 `5cfccd5c479c2874d51fa72cdc891c1c4d67093b406fd2d1e5cff7e36b861f4c`.
- Changed only two files under `research/implementation_execution/` in the research branch for code/tests; no ChemCore Stable, Android or firmware edits. No G2/G3 scientific promotion. See `G2_PUBLICATION_BLOCKER_LINEAGE_2026-10-29_AR.md`. Documentation checkpoint CI should be checked on next run.
- Next scientific requirement: acquire source-qualified independent physical raw exposures, traceable concentration references, device/session/exposure IDs, and pre-registered grouped blind holdouts; no claim of G2 completion before that evidence is audited.

---

## 2026-10-29 — G2 fail-closed publication guard (296/296 CI verified)

- Reviewed research HEAD 6885743dfe90c536fe671a648fc860f27888d724 and prior successful G2 run #40793543195 (289/289). Central catalog Git blob 1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f; 0/14 external sources review-ready, independent evidence records: [].
- Added g2_artifact_publish_gate.py, seven tests and a pre-upload integrity step. Fixed if: always()/if-no-files-found: ignore, which allowed incomplete G2 artifacts after failed CI; now success-only with missing files treated as errors.
- G2 CI run #40799799344, job 123204894955: 296/296 PASS; publish guard PASS; artifact 12865845365 (SHA256 de0928354a986342fb52ea1a6a7eae01d73336238676c11f22c2c819a56b57c9). Code SHA 98d7fc551b3533617d73d3b992c5d4ddc6168001. Research-only; no Stable, app or firmware edits. No G2/G3 promotion. Recheck documentation-only checkpoint CI next run.

---

## 2026-10-29 — G2 byte-exact double replay for audit and acquisition triage (289/289 CI)

- Resumed from research HEAD `5a2d18cf52e74c5c379378e82c91a99e3afe2692`; verified 20/20 previous CI successes. Rechecked central `HazRob/build-harness` catalog blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` (80 entries/14 external datasets); source readiness 0/14 and independent evidence `records: []`.
- Added `g2_replay_consistency.py` and four negative/positive-control tests; updated `.github/workflows/g2-signal-lineage-v4.yml` to independently rerun both audit and triage, fail closed on byte mismatches, and upload `g2_replay_integrity.json` with SHA256 and sizes.
- Code/workflow SHA `92edd4402aab54938244af755077433219abdd74`: [G2 Actions #40793543195](https://github.com/HazRob/HazRob-Tank/actions/runs/40793543195), job `123184342051`: **289/289 PASS**, both replay outputs byte-identical, `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`; artifact `12862363060` (SHA256 `3fb6bd09688b9ec3f759fc5e9a7dede495adeef9b168c7082b7fd08536650eed`).
- Documentation: `G2_BYTE_EXACT_DOUBLE_REPLAY_INTEGRITY_2026-10-29_AR.md`. Research branch only; no ChemCore Stable, Android or firmware changes. No G2/G3 promotion. Byte-identical replay is software reproducibility, not physical scientific validity.
- Next: recheck final documentation checkpoint CI; acquire independently measured physical exposures, traceable concentration standards, device/session/exposure IDs and locked grouped blind holdouts.

---

## 2026-10-29 — G2 exact-input SHA256 provenance for acquisition triage (285/285 CI)

- Continued from research HEAD `21392e3e654203815453d569d47c6b6e28598fe9`. Rechecked central `HazRob/build-harness` catalog blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` (80 entries/14 external datasets), pinned source matrix `bd69304fc35175b90199c6cc6ad4a88fc10ae93f` (0/14 review-ready), and independent evidence `records: []`.
- `g2_acquisition_triage.py` now emits SHA256 digests of the exact audit, source matrix, ledger, independent evidence and canonical registry input bytes; rejects inputs modified during replay. Added two regression tests (byte-preserving JSON semantics vs byte digest; mutation during replay).
- Code/test SHA `f0bf6f3b1fcc9f161879cb4527bbb48746ab5015`: [G2 Actions #40786907512](https://github.com/HazRob/HazRob-Tank/actions/runs/40786907512), job `123162275605`: **285/285 PASS**, `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`; artifact `12859467844` (SHA256 `a3831c2620851211b51b197108b8920045f7c9d2ea3d799611e6cb55c220526c`). Compare against prior HEAD: only two research implementation Python files changed.
- Documentation: `G2_TRIAGE_INPUT_BYTES_SHA256_LINEAGE_2026-10-29_AR.md`. No Stable/Android/firmware modifications; no G2/G3 promotion. Input hashes establish byte identity, not external physical authenticity.
- Next: recheck documentation checkpoint CI; original independently measured exposures, traceable ppm references, device/session/exposure IDs and locked grouped blind holdouts are still missing.

---

## 2026-10-29 — G2 audit replay binding for acquisition triage (283/283 CI)

- Continued from research HEAD `8709968e96b2928901ea5bc9caf8c48970ef50d0` (20/20 prior CI successful). Rechecked central `HazRob/build-harness` catalogue Git blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` (80 entries; 14 external datasets); pinned matrix `bd69304fc35175b90199c6cc6ad4a88fc10ae93f` remains 0/14 review-ready; independent evidence `records: []`.
- Fixed a reproducibility gap: triage previously accepted a forged, self-consistent audit with invented blockers or review candidates. `build_triage` now replays the canonical 80-technique evidence audit from the ledger and independent evidence register, reconciles the pinned source matrix, and requires exact equality before emitting triage. Added a positive control and two negative scenarios in one regression test.
- Code/test SHA `4618ffe49d04ed711746c207ca9a3e0f232ca894`: [G2 Actions #40779896071](https://github.com/HazRob/HazRob-Tank/actions/runs/40779896071), job `123138599806`, **283/283 PASS**, `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`; artifact `12856631682`. **21/21** workflows succeeded on code SHA.
- Documentation: `G2_TRIAGE_REPLAY_FROM_CONTROLLING_EVIDENCE_2026-10-29_AR.md`. Research branch only; no ChemCore Stable, Android or firmware edits; no G2/G3 scientific promotion.
- Next: recheck documentation-only checkpoint CI. Physically independent original gas exposures, traceable concentration references, device/session/exposure IDs and locked grouped blind holdouts remain absent.

---

## 2026-10-29 — G2 canonical 80-ID and pinned 14-source triage gate (282/282 CI)

- Resumed from research HEAD `a0e52bcce8f34d4c34d3136557b481d254d2decf`; verified 20/20 prior CI success. Re-read central `HazRob/build-harness` catalogue Git blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` (80 entries; 14 external sources); pinned derived matrix `bd69304fc35175b90199c6cc6ad4a88fc10ae93f`, 0/14 source-review ready; independent evidence `records: []`.
- Closed a triage integrity gap: a fabricated 80-by-14 input could substitute canonical technique/source identities or source blockers while retaining plausible counts. `g2_acquisition_triage.py` now binds to `research/PROGRESS_80.json`, the byte-pinned source matrix, status/blocker consistency, known acquisition actions and finite JSON numbers. CLI verifies the source-matrix Git blob before producing output. Added four regression tests with synthetic negative controls.
- Code/test SHA `244494f5d06399d92f3a7a8ac82c71be59f411de`: [G2 Actions #40771722696](https://github.com/HazRob/HazRob-Tank/actions/runs/40771722696), job `123110371111`: **282/282 PASS**, `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`; artifact `12852078288`. All **21/21** workflow runs on the code SHA succeeded.
- Documentation: `G2_TRIAGE_CANONICAL_SCOPE_PINNED_SOURCE_GATE_2026-10-29_AR.md`. Code/test changes confined to two files in `research/implementation_execution/` on research branch; no Stable, Android or firmware modifications. No G2/G3 promotion.
- Next: recheck CI for documentation checkpoint. Original independent physical exposures, traceable ppm references and locked grouped blind holdouts remain unavailable.

---

## 2026-10-28 — G2 deterministic acquisition triage (278/278 CI)

- Resumed from research HEAD `34b89d16163f4c8e58b6afece28b6d2a2482a470`; directly verified central `HazRob/build-harness` catalogue blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` (80 references, 14 external datasets). Independent evidence remains `records: []`; source review readiness 0/14.
- Added `g2_acquisition_triage.py` and `test_g2_acquisition_triage.py` to deterministically summarize the **actual** 80-technique audit and 14-source blocker matrix, including acquisition actions, while failing closed on forged counts, duplicate IDs, conflicting readiness, and duplicate JSON keys. Research G2 workflow now uploads `g2_acquisition_triage.json` alongside the existing audit artifact. This does not manufacture raw exposures or promote a technique.
- Code/workflow SHA `7449c25144d8298d3cd9a335dfa960073bedca0e`: [G2 Actions #40762104679](https://github.com/HazRob/HazRob-Tank/actions/runs/40762104679), job `123076456908`: **278/278 PASS**, `g2_pass_candidates=0`, `g2_blocked=80`, `source_blocked=14`; artifact ID `12847724274` successfully uploaded. Other CI runs on this commit were successful or queued at initial review; recheck all runs on the final documentation SHA next time.
- Documentation: `G2_DETERMINISTIC_ACQUISITION_TRIAGE_2026-10-28_AR.md`. Research branch only; no Stable, Android or firmware changes. Physical independent gas exposures, traceable reference concentrations and locked grouped blind holdouts are still absent. No G2/G3 approval.

---

## 2026-10-28 — G2 CI run metadata binding and replay (275/275 CI)

- Continued from research HEAD `6d456c74e75d02d6735bd572eee753b937ea1111` and verified central `HazRob/build-harness` catalogue blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` (80 records; 14 external datasets). Source readiness remains 0/14 and independent evidence register remains `records: []`.
- Fixed a reproducibility gap: the required, SHA256-verified `ci_run.json` was never semantically inspected. The cross-artifact gate now binds repository, run ID, head SHA, canonical run URL, G2 workflow path, event and successful conclusion to the technique record. Integer/boolean/float ID confusion is rejected. This is **offline consistency**, not authenticated GitHub provenance or physical scientific evidence.
- Added five regression tests (including rehashed forged run metadata and malformed JSON), updated the synthetic fixture, and corrected an initial test expectation to reflect the earlier JSON content gate. [G2 Actions #40754743557](https://github.com/HazRob/HazRob-Tank/actions/runs/40754743557), job `123050552473`: **275/275 PASS**, `g2_pass_candidates=0`, `g2_blocked=80`. All **21/21** associated CI runs succeeded on code/test SHA `5e6d5d0aae3cf7095821d01babe6f2e3683b4f8c`.
- Evidence document: `G2_CI_RUN_METADATA_BINDING_2026-10-28_AR.md`. Changes limited to research implementation files on research branch; no ChemCore Stable, Android or firmware changes. No G2/G3 scientific promotion. Recheck documentation checkpoint CI on next run. Physical independent raw exposures, traceable ppm references and locked grouped blind holdouts are still absent.

---

## 2026-10-28 — G2 central-source binding to technique review candidates (270/270 CI)

- Resumed at research HEAD `473a043874f1a7e1b5b1cedd312461dd4af72c08` (prior CI successful); pinned central catalogue `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` and public derived matrix `bd69304fc35175b90199c6cc6ad4a88fc10ae93f` (80 entries, 14 external datasets, 0/14 source-review ready); independent evidence register `records: []`.
- Added research-only `g2_catalog_link_gate.py`, nine negative-control tests, and main G2 audit reconciliation. A known central source cannot produce a technique review candidate while its source-readiness record is blocked; no source or technique can be promoted by this gate. Private central reference content was not copied to the public Tank repository.
- Documentation: `G2_CATALOG_SOURCE_TO_TECHNIQUE_BINDING_2026-10-28_AR.md`. Code/test commit `b8b4e219ee604b3b209367a662c593d9a565cd31`: [G2 Actions #40744308842](https://github.com/HazRob/HazRob-Tank/actions/runs/40744308842), job `123013896286`, **270/270 PASS**, `g2_pass_candidates=0`, `g2_blocked=80`; **21/21** associated push/PR CI runs succeeded. No ChemCore Stable, app or firmware changes. Physical independent exposures and traceable ppm standards are still unavailable.

---

## 2026-10-28 — G2 DOI/resolver cross-field consistency (261/261 CI)

- Continued from research HEAD `91e1ee0ab3c7f440202b9489e00f6e8d6141f8b4` (20/20 latest workflows succeeded); central catalogue blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` remains 80 entries/14 external sources. Independent evidence register `records: []`; source matrix 0/14 review-ready.
- Added fail-closed `ORIGINAL_SOURCE_DOI_RESOLVER_CONFLICT` for contradictory DOI field versus DOI-resolver landing URL; regular publisher URLs are unaffected. Added three real-byte synthetic regression controls in `test_g2_origin_percent_authority.py`.
- Documentation: `G2_CROSS_FIELD_DOI_RESOLVER_CONSISTENCY_2026-10-28_AR.md`. Code/documentation SHA `e6172f851baf980d9afc96891fccd6027731efb0`: [G2 Actions #40735621847](https://github.com/HazRob/HazRob-Tank/actions/runs/40735621847), job `122983228312`: **261/261 PASS**, `g2_pass_candidates=0`, `g2_blocked=80`; all **21/21** workflow runs succeeded. Only research implementation files changed; no Stable, app or firmware edits, no G2/G3 promotion. Recheck documentation-only commit CI; original independent physical exposure data, traceable reference ppm and locked grouped holdouts remain unavailable.

---

## 2026-10-28 — G2 complete offline test discovery (254/254 CI)

- Resumed from research HEAD `74d13c87fbe5c9b84fc9ea23cffa65bb79260681`; verified central `HazRob/build-harness` catalogue blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` (80 references, 14 external datasets), independent evidence `records: []`, and pinned source readiness 0/14.
- Fixed incomplete G2 CI test coverage: previously only five named test modules ran; three existing offline modules (MQ-EXT-01 file/day, Zenodo archive verification, Mendeley MQ-EXT-14 source metadata) were omitted. Workflow now compiles all research G2 modules and discovers every `test_g2_*.py` automatically. Added two regression tests to protect coverage.
- Workflow SHA `dac4b36415d4162a09a1213314747aa71f07cdc7`; test SHA `1608e7c0fc67a78b3ba03fafbbf92f7f749c9902`. [G2 Actions #40715630169](https://github.com/HazRob/HazRob-Tank/actions/runs/40715630169), job `122912402190`: **254/254 PASS** (19 previously omitted tests + 2 new tests), `g2_pass_candidates=0`, `g2_blocked=80`.
- Evidence: `G2_COMPLETE_OFFLINE_TEST_DISCOVERY_2026-10-28_AR.md`. Changes only on research branch, no ChemCore Stable, app or firmware edits, no G2/G3 scientific promotion.
- Next: recheck all CI workflows on documentation checkpoint; physically independent original gas exposures, traceable ppm references, device/session/exposure identifiers and locked grouped blind holdouts remain prerequisite to any G2 promotion.

---

## 2026-10-28 — G2 CI trigger coverage for raw archives and canonical registry (233/233 CI)

- Reviewed research HEAD `c83bfa559e5534fd206e4578b61a51fe3a678b21`, source catalogue Git blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` (80 records, 14 external datasets), independent evidence `records: []`, and pinned matrix 0/14 source-review ready.
- Fixed a reproducibility gap in research G2 workflow path filters: raw archives (`research/g2/raw/**`), canonical 80-technique registry (`research/PROGRESS_80.json`) and pinned source matrix now trigger the G2 audit for push and pull requests. Evidence bundle path was already covered.
- Added regression `test_raw_archive_registry_and_pinned_source_matrix_trigger_g2_ci`; code/workflow SHA `d6e1c1a15823affe419f12d1b52160a3f9fd711d`. [G2 Actions #40704942493](https://github.com/HazRob/HazRob-Tank/actions/runs/40704942493) job `122874400293`: **233/233 PASS**, `g2_pass_candidates=0`, `g2_blocked=80`. **21/21** associated workflow runs succeeded.
- Documentation: `G2_CI_RAW_EVIDENCE_TRIGGER_COVERAGE_2026-10-28_AR.md`. Only research G2 workflow and research test changed; no Stable, Android or firmware modifications; no G2/G3 scientific promotion.
- Next: recheck final documentation commit CI, then acquire physically independent, source-qualified raw gas exposures and traceable concentration references before considering G2 promotion.

---

## 2026-10-28 — G2 RFC3986 unreserved percent-encoded URL source aliases (232/232 CI)

- Resumed from research HEAD `5efbe572c6f091b37939b021e50d7a6b2416a3f1` (20/20 CI success), pinned central reference catalog Git blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` (80 entries; 14 external datasets), independent evidence `records: []`, and pinned source matrix 0/14 review-ready.
- Fixed `g2_reference_catalog_readiness.py`: normalize percent-encoded RFC3986 ASCII unreserved characters in generic HTTP(S) URL path and query while preserving reserved escape semantics. Prevents a duplicate source from bypassing the all-members source-identity blocker via `%7E` versus `~` or `%41` versus `A`; retains `%2F` versus `/` distinctions.
- Added `test_rfc3986_unreserved_percent_escapes_deduplicate_sources` with two real-byte synthetic files and recalculated SHA256, duplicate-class blocker and distinct-source controls. Code SHA `b63a3c78423167478415863680f3a0b220805b2c`, test SHA `010ecee8b5ab163d0fd96a3ba7262a7d2887cfe7`.
- [G2 Actions #40695601169](https://github.com/HazRob/HazRob-Tank/actions/runs/40695601169), job `122841409206`: **232/232 PASS**, `g2_pass_candidates=0`, `g2_blocked=80`. Comparison with baseline: two Python files changed, both under `research/implementation_execution/`. CI for other workflows and documentation commit should be rechecked.
- Documentation: `G2_RFC3986_UNRESERVED_PERCENT_ORIGIN_DEDUP_2026-10-28_AR.md`. No ChemCore Stable, Android or firmware edits; no G2/G3 scientific promotion. Synthetic fixtures are not physical measurements. Next: verify all CI, then obtain source-qualified independent physical raw exposures, traceable ppm reference, device/session/exposure identifiers, and locked blinded grouped holdouts.

---

## 2026-10-28 — G2 equivalent IPv6 textual source aliases (231/231 CI)

- Reviewed preceding research HEAD `ad4dbaa00d58ae965c1e6370f5a877bdfa5a07d7` (20/20 CI success), central reference catalogue Git blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` (80 records, 14 external datasets), independent G2 evidence `records: []`, and pinned external source matrix 0/14 review-ready.
- Fixed `g2_reference_catalog_readiness.py`: IPv6 literal addresses are canonicalized with `IPv6Address(...).compressed` before source-origin deduplication. Brackets, scheme, nondefault port, path and query are retained; distinct address+port authorities stay distinct.
- Added real-byte synthetic regression `test_equivalent_ipv6_spellings_are_one_source`, with recomputed SHA256, equivalent IPv6 spelling/IPv4-mapped controls, all-member duplicate blocking, and a distinct-host positive control. Code SHA `d3d284fcf178ddeb2fdc3a885d0a3bbc558b7dc3`; test SHA `2f1847b4631ff08d70b183a3b0140d2811ca7739`.
- [G2 Actions #40687760138](https://github.com/HazRob/HazRob-Tank/actions/runs/40687760138), job `122814413498`: **231/231 PASS**, `g2_pass_candidates=0`, `g2_blocked=80`; **21/21** workflow runs succeeded on test SHA. Compared with prior HEAD, only two files in `research/implementation_execution/` changed.
- Documentation: `G2_IPV6_EQUIVALENT_TEXTUAL_SOURCE_DEDUP_2026-10-28_AR.md`. No ChemCore Stable, Android or firmware edits; no G2/G3 scientific promotion. **Next:** recheck CI on final documentation/RESUME commit; acquire source-qualified original physical exposures, traceable concentration references, device/session/exposure IDs, and locked blind grouped holdouts. Synthetic regression data is not scientific evidence.

---

## 2026-10-28 — G2 IPv6 literal/port identity collision fix (230/230 CI)

- Rechecked research HEAD `d5ac5632959e94205c12e6334b467b3595503202`, central reference bank Git blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` (80 entries, 14 external datasets), and independent G2 evidence register `records: []`. Pinned catalogue matrix remains 0/14 source-review ready; no `research/g2/raw/` directory is present in the branch.
- Fixed a real IPv6 authority serialization collision: `https://[::1]:8080/data` and `https://[::1:8080]/data` previously normalized identically. Bracketed IPv6 host serialization now preserves distinct address+port identities and true default-port aliases.
- Added one real-byte synthetic regression with recomputed SHA256 and a duplicate-class control. Code/test SHA `471350d4227d66bdead9b35bd049fb477f84bd0d`.
- Verified [G2 Actions #40679098186](https://github.com/HazRob/HazRob-Tank/actions/runs/40679098186), job `122784922243`: **230/230 PASS**, `g2_pass_candidates=0`, `g2_blocked=80`; **21/21** workflows succeeded on code/test SHA. Compared to previous HEAD, only two files in `research/implementation_execution/` changed.
- Documentation: `G2_IPV6_LITERAL_PORT_IDENTITY_2026-10-28_AR.md`. No Stable, Android, or firmware edits, no G2/G3 scientific promotion. Recheck CI on this documentation commit. Independent physical raw exposure records, traceable reference ppm, and locked grouped holdouts are still missing.

---

## 2026-10-28 — G2 generic source URL scheme identity (229/229 G2 CI)

- Rechecked research HEAD `d1df93de574036581fa8b9ab22b7277735f27a87` (20/20 CI successful), pinned central source catalogue Git blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f`, independent evidence register `records: []`, and last pinned source matrix 0/14 review-ready.
- Fixed generic URL canonicalization to preserve `http://` versus `https://` as distinct origins while retaining scheme-specific default-port aliases and DOI resolver canonicalization.
- Added `test_generic_url_scheme_is_preserved_for_source_identity` with two real-byte synthetic archives and recomputed SHA256, covering source review and duplicate-group blocking. Documentation: `G2_HTTP_HTTPS_SOURCE_SCHEME_IDENTITY_2026-10-28_AR.md`.
- First G2 CI on `e918f9b796503252c1f1e6542ad394da1285fdfa` failed one of 229 tests because an older positive alias test assumed HTTP and HTTPS were equivalent. Corrected the legacy test in `93d97133cabafaa31ecbf31256c6f039f5a6d0c5`. Verified [G2 Actions #40671526473](https://github.com/HazRob/HazRob-Tank/actions/runs/40671526473), job `122759217384`: **229/229 PASS**, `g2_pass_candidates=0`, `g2_blocked=80`. All **21/21 workflow runs succeeded** on correction SHA. Compared to prior `d1df93de`, only five files in `research/implementation_execution/` changed. Recheck final documentation-commit CI next. No scientific promotion, no Stable/Android/firmware changes. Physical independent evidence and traceable ppm reference still absent.

---

## 2026-10-28 — G2 scheme-specific source URL port identity (228/228 G2 CI)

- Reviewed prior research HEAD `0635db25129159a522b9fa8619ada5ac9ad0a9f7` (20/20 CI successful), central reference-bank Git blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f`, and independent G2 evidence register `records: []`.
- Fixed generic URL identity normalization in `g2_reference_catalog_readiness.py`: only remove HTTPS port 443 or HTTP port 80; preserve scheme-nondefault ports to avoid false source deduplication.
- Added `test_nondefault_generic_url_ports_preserve_source_identity` using two real-byte synthetic archives and recomputed SHA256, covering distinct authorities and true default-port aliases. Code SHA `f22130d43c739f51948ddb591238a7086ae60f9a`, test SHA `0d22ca41cc412d58ce0040b252d5359b2c8b91de`.
- [G2 Actions #40663119310](https://github.com/HazRob/HazRob-Tank/actions/runs/40663119310), job `122730799682`: **228/228 tests PASS**, `g2_pass_candidates=0`, `g2_blocked=80`. All **21/21 workflows succeeded** on code/test SHA.
- Evidence report `G2_SCHEME_SPECIFIC_SOURCE_PORT_IDENTITY_2026-10-28_AR.md`. External catalogue matrix last pinned 0/14 source-review ready; physical independent evidence still absent. No scientific G2/G3 promotion and no Stable, Android or firmware edits.
- Next: verify final documentation-commit CI; acquire source-qualified independent raw physical exposures, traceable ppm references and blinded grouped holdouts. No synthetic controls may be promoted.

---
## 2026-10-28 — G2 DOI resolver authority and port fail-closed (227/227 G2 CI)

- Rechecked prior branch HEAD `76d823109eddb934948510103562584c0de95e23`, 20/20 successful workflows, central catalogue Git blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` from `HazRob/build-harness`, and independent evidence register `records: []`.
- Fixed DOI-resolver origin normalization: reject credential-bearing `doi.org`/`dx.doi.org` URLs and non-default resolver ports, including when the URL is carried by the `doi` field (and `doi:` prefixed URL). Invalid resolver authority returns `None` and blocks only the affected record; normal HTTP/HTTPS default ports remain valid.
- Added real-byte synthetic regression `test_doi_resolver_credentials_and_nondefault_ports_fail_closed` in `test_g2_evidence_audit.py`, covering both origin fields, 11 negative combinations and two valid aliases. Code/test commit `bb25fb83401bc9be7259fbee7dc90068497d2440`.
- Verified [G2 Actions #40656402533](https://github.com/HazRob/HazRob-Tank/actions/runs/40656402533), job `122708373891`: **227/227 PASS**, `g2_pass_candidates=0`, `g2_blocked=80`; **all 21/21 CI workflows on code SHA succeeded**.
- Evidence: `G2_DOI_RESOLVER_AUTHORITY_FAIL_CLOSED_2026-10-28_AR.md`. External catalog 0/14 source-review ready, independent physical G2 records absent. No Stable, Android or firmware changes, no G2/G3 scientific promotion. Recheck CI on this documentation commit before the next implementation.
- Next: acquire independently referenced raw physical exposures, traceable ppm references, device/session IDs, and blind grouped holdouts; no synthetic source may be promoted.

---

## 2026-10-28 — G2 malformed source URL parser isolation (226/226 G2 CI)

- Reviewed preceding `c6cc7c3fc2729a3d023c3c486c4cfadd2682008b` checkpoint, the central source catalog policy and independent G2 evidence register (`records: []`).
- Fixed `g2_reference_catalog_readiness.py` to catch `urlsplit` / hostname `ValueError` for malformed IPv6 brackets or NFKC-ambiguous URL hosts **before** the cross-source identity count. The malformed source receives `ORIGINAL_SOURCE_ORIGIN_MALFORMED` rather than crashing the entire catalogue audit.
- Added two regression tests in `test_g2_evidence_audit.py`: six malformed DOI/URL cases with isolation of the adjacent valid source, plus a valid DOI/URL control. Code SHA `b8f96d0e8345d3838f68354e5a16842c913f9665`; test SHA `f86734d9fc1faa3e70866cbeab56fd39fa5da6b4`.
- Verified [G2 CI #40650146981](https://github.com/HazRob/HazRob-Tank/actions/runs/40650146981), job `122688075186`: **226/226 tests PASS**, `g2_pass_candidates=0`, `g2_blocked=80`. Final recheck: **21/21 CI workflows succeeded on code/test SHA `f86734d9`**; **20/20 workflows succeeded on documentation checkpoint `de225c65587e816701608a4b51dfce243d9433f6`**; no failures. Recheck after this final RESUME-only update.
- Evidence document `G2_MALFORMED_ORIGIN_URL_PARSER_ISOLATION_2026-10-28_AR.md`. Initial two-commit diff vs preceding checkpoint: only `g2_reference_catalog_readiness.py` and `test_g2_evidence_audit.py` inside research implementation. 14/14 external catalogue sources previously blocked; no new independent physical exposures, no G2/G3 promotion and no ChemCore Stable, APK or firmware edits.
- Next: verify CI on latest documentation checkpoint, then continue acquisition of source-qualified independent physical measurements with traceable concentration, device/session/exposure identity, and blind grouped holdout.

---

## 2026-10-28 — G2 central catalogue authority and safety policy lock (224/224 G2 CI)

- Research-only `g2_reference_catalog_readiness.py` now requires the pinned reference-bank `schema_version`, `authority`, `scope` and five strict `True` non-certification policy flags. Prevents silently replacing the central source catalogue's declared authority or weakening the rule that raw data and gas labels do not certify Tank calibration, ppm or G3 when a new Git blob is re-pinned.
- Updated synthetic catalogue fixture and added regression `test_central_catalog_authority_and_noncertification_policy_are_locked` (positive case, altered/missing authority and scope, boolean type-confusion and missing policy negatives). Code/test SHA `a51ffe51b9b9e8fc96a7c8abc10e2cf37b80ebb3`; report `G2_CENTRAL_CATALOG_AUTHORITY_SAFETY_POLICY_LOCK_2026-10-28_AR.md`. **Verified G2 Actions:** [40643774255](https://github.com/HazRob/HazRob-Tank/actions/runs/40643774255), job `122667726887`: **224/224 PASS**, `g2_pass_candidates=0`, `g2_blocked=80`; **20/20 workflow runs succeeded** on documentation SHA `5312610ab6aa14ff91f2ecb58e19b8fb56e1268e`. Recheck CI on this new checkpoint commit.
- Central catalogue original Git blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` independently read from `HazRob/build-harness`. Independent G2 evidence register `records: []`; 0/14 external sources review-ready; no G2/G3 promotion. No ChemCore Stable, APK or firmware edits. Next: inspect G2 CI, repair failures, audit final branch-only diff, then continue independent physical evidence acquisition.

---

## 2026-10-28 — G2 source-origin provenance fail-closed (223/223 G2 CI)

- Research-only: source catalog now requires a syntactically valid DOI or HTTP(S) original landing URL; rejects malformed asserted DOI/URL, credentials, malformed ports, control characters, and DOI-resolver pages without a DOI. A valid DOI cannot mask a malformed landing URL.
- Added a real-byte synthetic regression fixture with positive DOI/URL cases and missing/malformed-origin negatives. Existing positive catalogue fixture now declares a valid landing URL. Evidence: `G2_SOURCE_ORIGIN_PROVENANCE_FAIL_CLOSED_2026-10-28_AR.md`.
- Initial code commit `a7f3ea113196b068db8107d48184c14b303e8414` failed G2 because the newline negative fixture used a literal backslash-n; corrected in `fefab773ff6ce6c21c25f5c869f5836e57be5d24`. Verified [G2 Actions 40638117397](https://github.com/HazRob/HazRob-Tank/actions/runs/40638117397), job `122649708083`: **223/223 PASS**, `g2_pass_candidates=0`, `g2_blocked=80`; **21/21 CI workflows on code SHA succeeded**. Compared to prior `445a778b`, four changed files all in `research/implementation_execution/`. Independent evidence register remains `records: []`, 14/14 external sources previously blocked. No ChemCore Stable, APK or firmware edits. Source identity is necessary but not physical provenance or concentration certification.

---

## 2026-10-28 — G2 DOI resolver alias normalization and URL path identity (222/222 CI)

- Last confirmed pre-change head `be3b16a21ec3c4375b6d082cf959678d63dc323b`: 220/220 G2 software tests and 20/20 final-commit workflows passed. Independent G2 register remains `records: []`, 0/80 G2 pass candidates, 80/80 blocked; external catalog matrix 0/14 ready.
- Research-only `g2_reference_catalog_readiness.py` now treats `doi.org`, `dx.doi.org`, `www.doi.org` DOI resolver aliases and percent-encoded DOI paths as the same source identity; preserves case-sensitive ordinary URL paths and query selectors while canonicalizing host/default port. Distinct source names still do not establish independent physical exposures.
- Two synthetic original-byte regression tests appended to `test_g2_evidence_bundle_gate.py`. Code/test SHA `75ff66a609b957d1bdc4d5e61aaa8c8717c852e3`; [G2 workflow #40631786246](https://github.com/HazRob/HazRob-Tank/actions/runs/40631786246) **222/222 PASS, 0 G2 candidates / 80 blocked; all 21/21 CI workflows on code SHA succeeded**. Evidence document: `G2_DOI_RESOLVER_ALIAS_SOURCE_IDENTITY_2026-10-28_AR.md`.
- Next: inspect workflow conclusions for the final documentation commit; investigate source-independent physical exposures and traceable reference concentrations; preserve the negative G2 gate. No ChemCore Stable, Tank APK, ESP32 or firmware edits; no G2/G3 scientific promotion without independently referenced physical measurements.

---

## 2026-10-28 — G2 cross-field DOI/landing URL source deduplication (220/220 CI)

- Research-only source-catalog audit now checks **both** DOI and landing-page identities per record; distinct DOIs sharing a landing page, or a DOI in one record repeated as resolver URL in another, block **all** source records. A DOI and its own URL in the same record are counted only once.
- Synthetic original-byte controls in `test_catalogue_doi_does_not_mask_duplicate_landing_url` verify positive distinct sources, two cross-field duplicate modes, order invariance, and no within-record self-duplication. Document: `G2_CROSS_FIELD_DOI_URL_SOURCE_DEDUP_2026-10-28_AR.md`.
- **Verified CI:** [G2 Actions 40626584311](https://github.com/HazRob/HazRob-Tank/actions/runs/40626584311), job `122612835706`: **220/220 tests PASS**, `g2_pass_candidates=0`, `g2_blocked=80`. All **21/21** workflows completed successfully on code SHA `8e1aee3d021a213b0be724a0d585052a7c7f29d0`. Central source catalogue blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` verified (80 entries, 14 datasets); independent G2 evidence register `records: []`; source matrix 0/14 review-ready. No G2/G3 scientific promotion or ChemCore Stable edits. Recheck CI after documentation commit.

---

## 2026-10-28 — G2 fail-closed for every duplicate source group member (219/219 G2 CI)

- Reviewed previous research checkpoint and central reference catalogue Git blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f`; independent G2 evidence register remains `records: []`.
- Research-only `g2_reference_catalog_readiness.py` now blocks **all** members of repeated normalized DOI/source-URL or raw SHA256 groups. Earlier implementation blocked only the lexically later entry, spuriously treating the first as source-review ready.
- Code/test commit `ad06177733a642ebd7bcf14e800cb16856ece3c5`; existing duplicate controls corrected, plus one 3-source regression checking all-member blocking, row-order invariance, and a rehashed raw mirror. G2 Actions [40620875383](https://github.com/HazRob/HazRob-Tank/actions/runs/40620875383), job `122594306304`: **219/219 PASS**, `g2_pass_candidates=0`, `g2_blocked=80`. Verified after completion: **21/21 CI workflow runs succeeded on code SHA** and **20/20 succeeded on documentation commit `a7ff679feab9742dd51cc48354e0ae88ec31cc6c`**; G2 run [40621058147](https://github.com/HazRob/HazRob-Tank/actions/runs/40621058147), job `122594919090`, **219/219 PASS**, 0 candidates and 80 blocked. Recheck CI after any later commit.
- Evidence document: `G2_ALL_DUPLICATE_SOURCE_MEMBERS_FAIL_CLOSED_2026-10-28_AR.md`. Pinned 14-source matrix still 0/14 ready. No independent physical G2 measurements, no G2/G3 scientific approval, no ChemCore Stable changes.
- Next: check CI on final commit; obtain original independent physical exposures, reference concentration traceability, device/session identifiers, and locked blind grouped holdout. DOI/raw-byte duplicates require source adjudication; distinct hashes are not proof of independence.

---

## 2026-10-28 — G2 original-dataset and raw-byte mirror deduplication (218/218 CI)

- Research-only source readiness now blocks duplicate external dataset origins (normalized DOI or source landing URL) and identical raw SHA256 archives across different catalogue IDs: `DUPLICATE_ORIGINAL_DATASET_ORIGIN`, `DUPLICATE_ORIGINAL_RAW_BYTES`. This prevents treating an archive mirror as a second independent physical experiment; distinct DOI/digest **does not prove** independent exposure.
- Code commit `85c961981783f30c32d09f2e7c06c689f10329d1`; two synthetic regression methods commit `c44b63c3fbb766bd4aa6c6611da2979bcb639b4a`. G2 Actions [40615130323](https://github.com/HazRob/HazRob-Tank/actions/runs/40615130323), job `122575715672`: **218/218 PASS**, `g2_pass_candidates=0`, `g2_blocked=80`. Verify other workflows on final documentation commit separately.
- Documentation: `G2_CATALOG_ORIGIN_AND_RAW_MIRROR_DEDUP_2026-10-28_AR.md`. Pinned central reference Git blob remains `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f`, 14 external datasets with 0 source-review ready; G2 evidence register still `records: []`. No ChemCore Stable, APK or firmware changes, no G2/G3 scientific approval.
- Next: check pending CI, obtain independently referenced original physical exposure records, verify device/session IDs and blind grouped holdout; do not count mirrors or synthetic fixtures as physical experiments.

---

## 2026-10-28 — G2 catalogue source readiness now requires original-byte replay (216/216 CI)

- Research-only code `g2_reference_catalog_readiness.py` no longer accepts all-true catalogue metadata plus a plausible SHA256 as sufficient for source-review readiness: it requires a present regular archive under `research/g2/raw/`, documented byte size, streamed SHA256 replay, and rejection of symlink/path escape/Git LFS pointer. This is archive integrity, **not** external provenance or physical concentration certification.
- Existing synthetic positive control now materializes raw bytes; two new regression methods cover absent source, size/digest mismatch, path escape (including a single backslash), symlink and LFS pointer. Final code/test commit `cd2d808c32d677a7df8053edc68fa73d7437afa9` (initial `3de1cdf4` followed by single-backslash correction).
- **Verified G2 Actions:** [40608458530](https://github.com/HazRob/HazRob-Tank/actions/runs/40608458530), job `122553776925`: **216/216 PASS**, `g2_pass_candidates=0`, `g2_blocked=80`. At last CI scan: 20/21 workflow runs on final code SHA succeeded, one batch07 run queued (not a failure or PASS). See `G2_CATALOG_RAW_BYTES_REPLAY_GATE_2026-10-28_AR.md`.
- Central reference catalogue Git blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` independently rechecked; 0/14 catalogued external datasets source-review ready; independent G2 evidence register still `records: []`. No ChemCore Stable, APK or firmware changes. **Next:** recheck pending CI, obtain original independently referenced physical source exposures and grouped blind holdouts. No G2/G3 approval.

---

## 2026-10-28 — G2 central catalog strict schema and complete category accounting (214/214 CI)

- Research-only `g2_reference_catalog_readiness.py`: validates exact complete `counts_by_type` against all 80 source entries (not just 14 external datasets); strict integer counts; ASCII canonical IDs/categories; rejects duplicate JSON keys, nonfinite JSON numbers, and float overflow even with matching blob SHA.
- Five new negative regression methods in `test_g2_evidence_bundle_gate.py`, including a corrected complete-category positive fixture. Code/test commit `cb47d3cb1d1259b9325bde4fea1e1b9ae358876f`. Verified G2 Actions [40601261833](https://github.com/HazRob/HazRob-Tank/actions/runs/40601261833), job `122529679909`: **214/214 PASS**, `g2_pass_candidates=0`, `g2_blocked=80`. Intermediate commit `d2a042d7` had a failed G2 run because the old positive fixture lacked non-dataset category accounting; corrected before PASS.
- Evidence: `G2_CATALOG_SCHEMA_CANONICAL_ACCOUNTING_2026-10-28_AR.md`. Pinned central catalog blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f`, 80 entries, 14 external datasets; source matrix remains 0/14 ready, 14/14 blocked; independent evidence register remains `records: []`. No G2/G3 scientific approval or ChemCore Stable edits.
- **Next:** recheck final documentation commit CI separately; prioritize independent original raw source bytes, physical concentration reference, device/session/exposure IDs and preregistered grouped holdout. No more synthetic test count should be treated as scientific progress.

---

## 2026-10-28 — G2 central 14-dataset provenance and source-readiness audit

- Research-only source `g2_reference_catalog_readiness.py` validates the exact central catalogue Git blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f`, 80 unique entries and 14 external measurement datasets. It rejects truthy qualification strings, raw mirror/inspection claims without original SHA256, unverified physical groups and unverified reference traceability. **This is catalogue triage, never G2 scientific qualification.**
- Machine matrix: `G2_CENTRAL_CATALOG_SOURCE_READINESS_2026-10-28.json`; Arabic methodology: `G2_CENTRAL_CATALOG_SOURCE_READINESS_2026-10-28_AR.md`. **0/14 source-review ready, 14/14 source-review blocked**. MQ-EXT-03 and MQ-EXT-15 have previously inspected/verified-elsewhere raw status but no independently qualified G2 physical-group/reference provenance in this catalogue; MQ-EXT-07 and MQ-EXT-09 require source access. Do not double-count UCI1081 or the MultimodalGasData mirror.
- Five new regression tests appended to `test_g2_evidence_bundle_gate.py`, including the pinned matrix. Test/CI status must be verified on the final commit; do not infer PASS from code writes.
- The previously documented independent G2 register remains `records: []`; last confirmed scientific audit `g2_pass_candidates=0`, `g2_blocked=80`. No G2/G3 approval or ChemCore Stable change.

---

## 2026-10-28 — G2 post-unblinding blind-truth integrity (204/204 CI)

- Research-only: `g2_cross_artifact_gate.py` checks `review_decision.json.blind_truth_release_sha256` against canonical blind reference labels **after unblinding**; never puts blind truth into the pre-unblinding protocol. Four synthetic regression tests added to `test_g2_evidence_bundle_gate.py`.
- Code/test commit `99abe3f67736baad593a8decb8e8f3d5f53d560c`; G2 Actions [40586663420](https://github.com/HazRob/HazRob-Tank/actions/runs/40586663420), job `122479787293`: **204/204 PASS**, `g2_pass_candidates=0`, `g2_blocked=80`. All 11 workflow runs on the then-final documentation commit `ffd7acde0509c673ea0cdd59a356c001ab551039` were subsequently verified successful, including G2 run [40586914683](https://github.com/HazRob/HazRob-Tank/actions/runs/40586914683), job `122480711077` (204/204 PASS, 0/80 candidates, 80/80 blocked), and the 23/23 batch07 jobs in run `40586914598`. Verify CI again on any later checkpoint commit.
- Evidence report `G2_POST_UNBLINDING_TRUTH_RELEASE_2026-10-28_AR.md`. Central source catalog Git blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` (80 entries, 14 external measurement datasets, none source-qualified true); independent G2 register `records: []`. This hash is an internal integrity commitment, not externally authenticated unblinding or physical calibration. No G2/G3 scientific approval or ChemCore Stable change.

---

## 2026-10-27 — G2 calibration reference-label commitment (200/200 CI)

- Research branch only. New `protocol_locked.json.calibration_labels_sha256` binds exposure/group/gas-reference labels of the calibration split independently of the physical holdout assignment and majority-train label locks. Rejects post-hoc calibration relabeling even when CSV SHA256 is refreshed.
- Code `g2_cross_artifact_gate.py` commit `2f78455d13bad40d195a9aa3d06f3732783d9425`; four synthetic controls in `test_g2_evidence_bundle_gate.py` commit `4473942658a890f2fa9cc4f335d66312bd4cc07a`. Document `G2_LOCKED_CALIBRATION_REFERENCE_LABELS_2026-10-27_AR.md`.
- Verified G2 CI run [40577570708](https://github.com/HazRob/HazRob-Tank/actions/runs/40577570708), job 122447989194: **200/200 PASS**; `g2_pass_candidates=0`, `g2_blocked=80`. Review all workflow conclusions on final documentation commit separately.
- Central catalog blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` reverified; external MQ catalog source blob `522a7013d5fab3ac6e6c2e4cbad1fc20fb99f837` reverified on its original research branch. No physical independent measured G2 records; no G2/G3 scientific approval or ChemCore Stable edits. Internal digest does **not** establish chronological preregistration.

---

## 2026-10-27 — G2 locked physical holdout allocation gate

- Research-only: require group_assignment_sha256 in protocol_locked.json to bind exposure IDs, physical source columns and group assignments without blind gas labels. Four synthetic regression controls in test_g2_evidence_bundle_gate.py.
- Documentation: G2_LOCKED_PHYSICAL_HOLDOUT_ASSIGNMENT_2026-10-27_AR.md. Verify CI on final commit before reporting success.
- No independent measured physical G2 evidence (records: []), 0/80 G2 candidates, 80/80 blocked; no G3 or ChemCore Stable changes. Digest matching alone cannot establish chronological preregistration.

---

## 2026-10-27 — G2 ASCII edge-whitespace identity gate (192/192 CI)

- **Research-only branch:** `research/chemcore80-central-evidence-gap-analysis-20261021`. Software commit `a0c70b6df42b412a84eefe4fd0db2537639d94e3`; code/test changes restricted to `research/implementation_execution/`. Documentation: `G2_ASCII_EDGE_WHITESPACE_IDENTITY_FAIL_CLOSED_2026-10-27_AR.md`.
- **Fix:** reject leading/trailing ASCII spaces in CSV header/cells in both evidence byte-integrity and cross-artifact replay gates; retain valid interior spaces. Prevent fake `majority_train` accuracy improvement through inconsistent normalization of train vs blind labels. Six rehashed negative controls in two new tests.
- **Verified CI:** [G2 run 40559691241](https://github.com/HazRob/HazRob-Tank/actions/runs/40559691241), job `122385061083`: **192/192 PASS**, `g2_pass_candidates=0`, `g2_blocked=80`. Other workflows on final doc commit require separate status check.
- **Central references:** `HazRob/build-harness/library/reference-bank/external-research/master_catalog.json` Git blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` reverified. Independent evidence register remains `records: []`.
- **Scientific limits:** 0/80 physically qualified G2, no G3, no ChemCore Stable change. Next: check final-commit CI, then source-qualified raw physical reference measurements and independently held-out groups; synthetic tests are never G2 scientific approval.

---

## 2026-10-27 — G2 canonical Unicode identity and CI checkpoint

- **Branch:** `research/chemcore80-central-evidence-gap-analysis-20261021`; software commits `825aa43e75ddbca0c66f6c3fefd8ccca663eb128` and `3edf52c7c893ec3aa63c859eb3a0099f1c4e4f91`. Research-only edits to three files under `research/implementation_execution/`; no ChemCore Stable changes.
- **G2 gate:** NFC normalization required for every CSV header/cell; reject Unicode nonstandard spaces (Zs except U+0020), C* and Zl/Zp. Two new synthetic regression tests, including rehashed CSV mutation controls. Details: `G2_UNICODE_NFC_SPACES_CANONICAL_IDENTITY_2026-10-27_AR.md`.
- **CI:** 190/190 tests passed, [G2 Actions run 40551023321](https://github.com/HazRob/HazRob-Tank/actions/runs/40551023321), job 122354475793. Parallel runs: 19/21 success, two batch07 runs queued at last check. Recheck before claiming all CI green.
- **Scientific status:** `g2_pass_candidates=0`, `g2_blocked=80`; independent evidence register `records: []`. Central source catalog pinned SHA `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` verified. No G2/G3 approval; physical reference and raw independent grouped holdout data remain necessary.

---

## 2026-10-26 — G1 independent CI remediation
- أعيد تشغيل 43 اختبار Python محلياً في `/mnt/data/hazrob_g1_validation`: `Ran 43 tests ... OK`؛ هذه تغطي 08.9 regression و09.5 و09.6 وست بوابات بحثية، **ولا تغطي الدفعة السابعة كاملة**.
- أضيف workflow مستقل `.github/workflows/chemcore80-g1-batch07-08.yml` (commit `68d7294b244beec389452032174418e9faf2cc6d`) يختبر الدفعة السابعة (22+7) والثامنة (14+11+11)، ويعمل عبر push أو workflow_dispatch إذا كان GitHub Actions متاحاً.
- استعلام runs المرتبطة بالالتزام الجديد لم يُرجع تشغيل PR حتى وقت التحقق؛ **لا يوجد إثبات CI PASS**. فشل workflow السابق pre-step ولا يجوز نسبة السبب جزماً إلى صلاحيات أو billing دون سجلات إدارية.
- معايير الإغلاق: PASS موثق من clean checkout لجميع الملفات، مراجعة source provenance، وتحديث ledger لكل تقنية. حتى ذلك الحين G1=60/80 وG3=0/80.

## 2026-10-26 — محاولة تنفيذ G1 مستقلة عن Actions
- فشل `git ls-remote https://github.com/HazRob/HazRob-Tank.git` في حاوية الاختبار بسبب `Could not resolve host: github.com`؛ تعذر clean checkout.
- تم تنفيذ 3 اختبارات Python محلية **PASS 3/3** على نسخة مستقلة أعيد بناء قلبها العددي من `08.9/nonnegative_rank_guard_v2.py`: الحل المعلوم، التباين في المقاييس، والرتبة المنهارة.
- هذه **ليست اختبارات ملف GitHub نفسه ولا اختبارات الدفعة السابعة الـ22**، ولا تغلق G1؛ لا يجوز تحويلها إلى CI_PASS.
- يظل G1=60/80 وG3=0/80. لاستكمال الإغلاق: تمكين clean checkout أو قناة نقل ملفات إلى الحاوية، ثم تشغيل `python -m unittest -v research/implementation_execution/test_batch07_v2.py` والاختبارات المحلية للمعرفات العشرة وملف انحدار 08.9، وبعد نجاحها تكرار الإجراء للدفعة الثامنة.

## 2026-10-26 — G1 بالترتيب: العودة إلى الدفعة السابعة
- بناءً على توجيه المستخدم، لا تُضاف تقنيات جديدة قبل إتمام اعتماد G1 للدفعة السابعة ثم الثامنة.
- أضيفت سبع حالات اختبار انحدار للتقنية 08.9: حل معلوم، ثبات أمام فرق القياس، الرتبة المنهارة، الحدود غير السالبة، NaN، تكرار القنوات، وعدم ادعاء ppm.
- الشيفرة والاختبارات محفوظة في `research/techniques/08.9/test_nonnegative_rank_guard_v2_regression.py`، commit `6b82d37a519b2388c385def9850406bd382a488e`.
- **حالة الاختبارات الجديدة: مكتوبة وغير منفذة محلياً أو عبر CI في هذه الجولة.** لا يجوز اعتبارها PASS.
- GitHub Actions run 40235160153 أُعيد تشغيله وفشل 11/11 بوظائف steps=[] و404 للسجلات؛ يجب حل runner/permissions أو توفير clean-checkout مستقل.
- يبقى G1 المعتمد 60/80 وG3=0/80؛ نقطة العمل التالية: تنفيذ الاختبارات الـ22 للدفعة 7 مع اختبارات 08.9 الجديدة على بيئة مستقلة ثم معالجة الإخفاقات قبل الدفعة 8.

# LATEST CHECKPOINT — 26 OCT 2026: BATCH08 08.10 + 10.1 LOCAL, G1 VERIFIED STILL 60/80

**Scientific snapshot**: 28/28 exact-source-code local Python unit tests passed across 08.9 (8), 08.10 (10), 10.1 (10); results and Git blob hashes archived in `LOCAL_AUDIT_2026-10-26_AR.md`. 08.10 source multioutput ridge tests are SYNTHETIC and its concentration predictions are explicitly RESEARCH_ONLY / `ppm_approved=False`. See `BATCH08_EVIDENCE_AR.md` and execution ledger. A passing local test of one module is not batch07/08 G1 success.

**Next**: independently reproduce ALL 10-ID Batch07 unit tests and 22 shared tests after restoring GitHub Actions job-start; GitHub jobs currently have `steps=[]` even for formerly passing workflows. Then complete Batch08 other eight IDs with meaningful source and holdout tests, not just superficial registration. Pinned central source references remain unchanged. **CI-certified G1 60/80 and physical Tank G3 0/80.** No production APK, ESP32, ChemCore Stable or original HR069 edits.

---

# ACTIVE RESUME POINT — 2026-10-26 / G1=60/80, PARTIAL VERIFIED LOCAL TESTS

**Most recent actual engineering work is NOT Batch01:** six V4 batches were verified in earlier CI; Batch07 has ten V2 source modules committed but all eleven CI jobs fail before any recorded steps, and Batch08 is partially begun for 10.1. Research branch: `research/chemcore80-central-evidence-gap-analysis-20261021`, [PR #6](https://github.com/HazRob/HazRob-Tank/pull/6).

1. **Primary evidence:** `research/implementation_execution/LOCAL_AUDIT_2026-10-26_AR.md` — exact Git blob SHA identities for new module/test files and **18/18 independently run local CPython checks**. This is **8 cases for 08.9 NNLS and 10 for 10.1 joint non-detection**, not the whole ten-ID Batch07 suite. The original Batch07 **22 shared tests** have NOT passed GitHub CI in this update.
2. **Scientific fixes:** `research/techniques/08.9/nonnegative_rank_guard_v2.py` now robustly handles fingerprint amplitudes `10^±200` and rejects collinear/ill-conditioned numeric mixtures. `research/techniques/10.1/joint_negative_evidence_v2.py` prevents unjustified multiplication of dependent MQ-sensor absence evidence and never hard-excludes gas; no independent calibration proof.
3. **Certified latest V4 G1 ledger** `EXECUTION_LEDGER_2026-10-21.json`: **60/80**, Batch07 10 code-committed but CI pending, Batch08 10.1 code-committed/local test pass; **G3 physical Tank 0/80**, no active safety ppm or mixture concentration approval. The 80/80 old G2 *source eligibility* should not be mistaken for V4 G1/G3.
4. **CI blocker after new commits:** [Actions #40235160153](https://github.com/HazRob/HazRob-Tank/actions/runs/40235160153) and later workflows show failure with `steps=[]` for even prior passing batch01–06; runner/organization pre-job failure suspected, exact cause not proven. Avoid reporting full batch pass until recorded steps/testing reappear.
5. **Next actual work:** Diagnose Actions runner/organization job-start failure or obtain full independently reproducible clean-checkout run; execute ALL Batch07 10-ID legacy tests plus 22 new source tests, repair findings, publish CI or complete local audit. Then develop 08.10–09.6 source-aware Batch08 engineering code and benchmark with real sensor data when available; run per-ID tests and preserve blocked outcomes.
6. **Research output to inspect:** `BATCH07_EVIDENCE_AR.md`, `BATCH07_SOURCE_READINESS_2026-10-23.json`, `BATCH08_EVIDENCE_AR.md`, `PLAN_80_V4.json`, `research/benchmarks/` (UCI external trained models). No merge into production Android/ESP32 or original HR069. No simulated G3.

**Do not delete earlier checkpoints**: they provide historical reproducibility and negative empirical UCI1081 06.5 results. Only the top dated checkpoint describes the current position.

---

# ACTIVE CHECKPOINT — 2026-10-23 / SIX V4 BATCHES COMPLETE (60/80 RESEARCH G1)

**Authoritative branch:** `research/chemcore80-central-evidence-gap-analysis-20261021`; **review request:** https://github.com/HazRob/HazRob-Tank/pull/6 (research only, not main merge). Always inspect current HEAD and CI before further edits.

- **Batch06 G1:** 10/10 IDs `06.9,07.1,07.2,07.3,07.4,07.5,07.6,07.7,07.8,07.9`, [200/200 Python tests PASS on GitHub Actions #39911351493](https://github.com/HazRob/HazRob-Tank/actions/runs/39911351493), tested commit `f565051f582d0f9703acf8dd3c2f65b1048b5dc9` (171 historical + 29 new grouped negative/positive tests). Failure history [#39911256560](https://github.com/HazRob/HazRob-Tank/actions/runs/39911256560) records and resolves a malformed set-bracket syntax error in 06.9.
- **Cumulative newly upgraded V4 software G1:** **60/80 (six ten-ID batches)**. Source/reference historical G2 reviewed 80/80 remains a different quantity. **New V4 external MQ/MOS measurement benchmark for Batch06 = 0; actual Tank physical gas-reference G3 = 0/80.**
- **Evidence docs:** `BATCH06_EVIDENCE_AR.md` and `BATCH06_SOURCE_READINESS_2026-10-23.json`. **Machine source-of-truth:** `EXECUTION_LEDGER_2026-10-21.json`; **per-technique scientific actions:** `PLAN_80_V4.json`; pinned reference catalogue Git blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f`.
- **Scientific limitations:** the new source-guard algorithms are tested on synthetic examples and not full physics-identification validation. UCI270 batch != device serial; matched-blank subtraction can erase target analytes; transfer can produce negative transfer; application auto-retraining, gas discharge and occupational ppm alarms remain disabled.
- **Latest known actual external negative finding:** 06.5 on UCI1081 single MQ135 source had unknown-gas rejection **4.44%** at known-gas acceptance 94.44% in leave-one-gas-out, with optimistic in-sample threshold; see `BATCH05_UCI1081_MQ135_NEGATIVE_FINDING_2026-10-23_AR.md`. Preserve this failure and do not generalize to Tank eight-MQ array.
- **NEXT BATCH07 10 IDs:** `07.10,08.1,08.2,08.3,08.4,08.5,08.6,08.7,08.8,08.9`. Prioritize actual CPU/power TinyML on production-like device with measured resources if available, source-aware CNN/TCN/GRU/GNN comparison and mixture identifiability/NNLS, not another uncalibrated accuracy claim. Do not duplicate existing trained UCI benchmark files; assess algorithm parity with technique modules and group-source eligibility.
- **Required continuation:** branch HEAD → updated ledger → Batch06 evidence & blockers → V4 plan source link → inspect Batch07 V1 and current UCI benchmarks → code/tests/CI → only then modify Batch07 ledger and prepend next checkpoint. Report per-ten batch completion.
- **Separation:** original HazRob HR069, production ChemCore Stable, Tank APK, ESP32, ESP32-CAM and robot hardware firmware remain unchanged. No implied production approval or background execution.

---

# ACTIVE CHECKPOINT — 2026-10-23 / V4 FIVE BATCHES COMPLETE (50/80 G1)

**Current authoritative research branch:** `research/chemcore80-central-evidence-gap-analysis-20261021`. Before continuing, resolve HEAD, read `EXECUTION_LEDGER_2026-10-21.json`, this checkpoint, and `PLAN_80_V4.json`. No implementation is considered merged into Tank production.

- **Batch01** 10 G1 IDs: [336 software tests](https://github.com/HazRob/HazRob-Tank/actions/runs/39545362103).
- **Batch02** 10 G1 IDs: [175 software tests](https://github.com/HazRob/HazRob-Tank/actions/runs/39547105861).
- **Batch03** 10 G1 IDs: [174 software tests](https://github.com/HazRob/HazRob-Tank/actions/runs/39550168060).
- **Batch04** 10 G1 IDs: [175 software tests](https://github.com/HazRob/HazRob-Tank/actions/runs/39904801822), source physical-session train/test leakage corrected in 04.10 and 05.1–05.6. Read `BATCH04_EVIDENCE_AR.md` and `BATCH04_SOURCE_READINESS_2026-10-21.json`.
- **Batch05** 10 G1 IDs: [182 software tests](https://github.com/HazRob/HazRob-Tank/actions/runs/39905840537), 158 historical + 24 new grouped scientific-contract tests. New modules `source_blank_limits_v2.py`, `source_concentration_holdout_v2.py`, `dependency_guard_v2.py`, `independence_bayes_v2.py`, `grouped_probability_calibration_v2.py`, `grouped_conformal_v2.py`, `open_set_group_guard_v2.py`, `source_ood_guard_v2.py`, `negative_evidence_guard_v2.py`, `shap_provenance_v2.py`. Read `BATCH05_EVIDENCE_AR.md` and `BATCH05_SOURCE_READINESS_2026-10-23.json`.

**Exact current totals:** **50/80 V4 source-aware software/G1 techniques**, **0 newly qualified Tank gas-reference G3/80**; new G2 experimentally grouped external sensor benchmarking not evidenced by the above CI. These checks do not make ppm, mixture separation, gas identity or occupational safety claims. Historical G2 source-eligibility 80/80 is distinct. Central reference catalogue pinned Git blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f`.

**NEXT BATCH06 ten fixed IDs:** `06.9,07.1,07.2,07.3,07.4,07.5,07.6,07.7,07.8,07.9`. Implement actual session/device/grouped chronological evaluation, reliability / drift, OOD and selective learning gates, with negative tests, CI, `BATCH06_EVIDENCE_AR.md`, ledger and resume update. Each technique must retain research-only warning if real source gas traceability missing.

**Known algorithm parity debt:** 04.5 linear SVM not RBF parity, 04.7 boosted-stump != XGBoost, 04.9 QSPR is source-derived ridge candidate rank without blind MQ chemical validation, 05.4 regression single target not multi-compound, 05.5 PLS1 not PLS2, 06.5 unknown threshold in-sample optimistic, 06.4 exchangeability unverified, source reference hashes/certification not independently physically checked. Do NOT mark these clinically or industrially validated.

**Separation:** Original HazRob HR069 app/firmware, Tank Android APK/ESP32 firmware, production ChemCore Stable all untouched. PR #6 research branch unmerged. To proceed, inspect current HEAD and CI, preserve negative outcomes. All development is synchronous in this turn only; no future background execution.

---

# ACTIVE RECOVERY — 2026-10-23 / BATCH04 VERIFIED, BATCH05 NEXT

**Branch:** `research/chemcore80-central-evidence-gap-analysis-20261021`. Last independently checked CI for the ten Batch04 IDs (04.8–05.7): [#39904801822](https://github.com/HazRob/HazRob-Tank/actions/runs/39904801822) on commit `76e5832ede1ac5a4d67700670d24cb7545d214ec`; **175/175 Python software tests PASS**, including 15 shared tests (3 new session-leakage negative cases) and 160 legacy. Structural plan checker also passed.

**Machine ledger** `EXECUTION_LEDGER_2026-10-21.json`: V4 research G1 **40/80 (4 ten-ID batches)**; `BATCH04_EVIDENCE_AR.md` records algorithm coverage and defects; `BATCH04_SOURCE_READINESS_2026-10-21.json` records missing experimental evidence. No new external physical G2 claim and Tank hardware G3 **0/80**.

**Scientific correction:** 04.10 and all six 05.1–05.6 estimators now require a `session_id`, reject training/testing on different exposure IDs from the same session, and retain source/R0/heater gates. Input `reference_trace_id` is still an *asserted* label, not independent certificate or true ppm verification. **04.9 QSPR is descriptor ridge candidate ranking only, 05.4 only a single analyte, 05.5 PLS1, 05.6 bagged trees single output**.

**Next exact official batch:** Batch05 `05.8,05.9,06.1,06.2,06.3,06.4,06.5,06.6,06.7,06.8`. Start with blank-based LOD/LOQ screening (05.8) and out-of-calibration guard (05.9), then dependence-aware evidence fusion, probability calibration and open-set controls (06.x); record CI and `BATCH05_EVIDENCE_AR.md`. Do not promote any LOD/LOQ or ppm into occupational safety without independently certified physical reference tests.

**Recovery order:** this checkpoint → `EXECUTION_LEDGER_2026-10-21.json` → `BATCH04_EVIDENCE_AR.md` → `PLAN_80_V4.json` → current GitHub CI. Old checkpoints below are historical, even if they say Batch04 has not begun.

---

# ACTIVE RECOVERY CHECKPOINT — V4 BATCH03 10/10 G1 SOURCE-CONTRACT PASS (2026-10-21)

**Next:** Batch04 (10 IDs) in this precise order `04.8,04.9,04.10,05.1,05.2,05.3,05.4,05.5,05.6,05.7`. Start with `04.8` (true ANN/MLP grouped evaluation, no source leakage), `04.9` (candidate-wise unknown gas/concentration QSPR/Physics+ML without test-label leak), `04.10` (independent source benchmark matrix); then 05.x conditional calibrated ppm logic where reference data exist.

- **Overall new V4 engineering G1:** **30/80**; Batch01 10/10 [336 tests](https://github.com/HazRob/HazRob-Tank/actions/runs/39545362103), Batch02 10/10 [175 tests](https://github.com/HazRob/HazRob-Tank/actions/runs/39547105861), Batch03 10/10 [174 tests](https://github.com/HazRob/HazRob-Tank/actions/runs/39550168060).
- **Batch03 code-tested commit:** `3fc4df06271e94d2a3d448f2ddae308a3684348c`. Successful plan G1 structural checker. **Note** per-directory 160 tests are historical; shared 14 are the V4 integrated new-method group-gate tests, not 174 new experimental gas exposures.
- **Primary evidence:** [BATCH03_EVIDENCE_AR.md](BATCH03_EVIDENCE_AR.md), [BATCH03_SOURCE_READINESS_2026-10-21.json](BATCH03_SOURCE_READINESS_2026-10-21.json). **Machine status:** [EXECUTION_LEDGER_2026-10-21.json](EXECUTION_LEDGER_2026-10-21.json). **80 detailed work plans:** [PLAN_80_V4.json](PLAN_80_V4.json).
- **Algorithm honesty:** `04.5` implements linear OVR SVM, RBF benchmark parity pending; `04.7` group-evaluates boosted decision stumps, **NOT XGBoost**. Research XGBoost models elsewhere in benchmarks are not thereby integrated. Mark scientific algorithm parity pending and do not deploy.
- **Actual raw external V4 grouped G2 assessment:** **not completed** from new code; source label/group eligibility unverified for several sources. Historical G2 old 80/80 source check is separate. **Actual MQ8 Tank physical G3 remains 0/80**. Original Android/ESP32/CAM and HR069 untouched.
- **Reference catalogue version pinned:** Git blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` in research V4 plan. Do not silently replace the evidence list with later mutable index; provenance must be versioned.
- **Restart:** read this checkpoint, inspect actual PR #6 branch head, then ledger + batch03 report + central source G2 blocked reasons. Before batch04 code, rerun `python research/implementation_execution/validate_plan_v4.py` and save CI. Before any scientific approval require independently measured gas concentration reference ppm, per-physical-instance R0, RL, heater phase, timestamps, separate sessions/devices with heldout testing.

---

# ACTIVE RECOVERY CHECKPOINT — V4 BATCH02 10/10 RESEARCH SOFTWARE PASS (2026-10-21)

**Authoritative next task:** V4 Batch03 ID `03.6` (sensor subset stability) through `04.7`. Its ten IDs according to canonical plan: `03.6,03.7,03.8,04.1,04.2,04.3,04.4,04.5,04.6,04.7`. Next file after development: `BATCH03_EVIDENCE_AR.md`; do not renumber techniques.

**Current PR/branch:** [research PR #6](https://github.com/HazRob/HazRob-Tank/pull/6), `research/chemcore80-central-evidence-gap-analysis-20261021`. Research branch only, not production and not yet merged into main.

**Verified new V4 software G1:**
- Batch01: 10/10 source-aware modules; [336/336 GitHub Actions #39545362103](https://github.com/HazRob/HazRob-Tank/actions/runs/39545362103).
- Batch02: 10/10 source-aware modules; **175/175** (153 directory tests + 22 shared new-method scientific tests), [GitHub Actions #39547105861](https://github.com/HazRob/HazRob-Tank/actions/runs/39547105861) on commit `cdcd6035adeaa06483e7a06c826c11621b5f98cf`.
- Combined **20/80 techniques newly advanced to V4 G1 engineering test gate** (does not indicate 20 real gas calibration validations). Remaining V4-batch upgrades 60.
- Historical G1 80/80 and historical G2 eligibility 80/80 remain unchanged; **new V4 G2 empirical validation has not been established by these unit tests. Tank G3 physical evidence stays 0/80**. Do not copy counts from initial G1/G2 across to new versions.

**Single-source execution status:** `EXECUTION_LEDGER_2026-10-21.json` (80 item states plus eight batches). Batch reports: `BATCH01_EVIDENCE_AR.md` and `BATCH02_EVIDENCE_AR.md`. New source blockers per ID: `BATCH02_SOURCE_READINESS_2026-10-21.json`.

**Reproducible restart sequence:**
1. Fetch actual current branch HEAD and PR #6 checks; record SHA, do not assume HEAD matches this documentation after concurrent edits.
2. Read `PLAN_80_V4.json` and this `EXECUTION_LEDGER_2026-10-21.json`, then inspect each ID currently not upgraded. Validate plan and status with `python research/implementation_execution/validate_plan_v4.py`.
3. Confirm two CI runs, preserve negative findings/failure histories; source catalogue must remain pinned to Git blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` until a deliberate version change.
4. Start Batch03 03.6 implementation, add versioned research code and tests, group-holdout source evidence when available, update ledger after CI, report only on 10-ID batch boundary.
5. Before any ChemCore runtime integration: unify duplicate method prototypes and API contracts; compare outcomes on independently labelled MQ exposures and Tank physical data. No changes to original HazRob, APK, ESP32 or Tank firmware without separate permission.

**IMPORTANT SCIENTIFIC GATES:** `MQ-EXT-03` 6400 ADC rows are not 6400 independent exposures, UCI sources are not Tank MQ8 calibrations, manufacturer graph curves and cross-device R0 are research priors. Blind gas identity, mixture component ppm, LOD/LOQ and toxicity decisions still require physically referenced calibration and G3 group-heldout trials.

---

# LATEST V4 SCIENTIFIC DEVELOPMENT CHECKPOINT — BATCH 01 G1 PASS (2026-10-21)

- **Current research PR:** https://github.com/HazRob/HazRob-Tank/pull/6, branch `research/chemcore80-central-evidence-gap-analysis-20261021`. This PR remains a **research review**, not automatically merged to `main`.
- **Full 80 technique V4 work orders documented** in `PLAN_80_V4_AR.md` and `PLAN_80_V4.json`; crosswalk pinned against central scientific references (source blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f`). Do NOT recompute historic G1/G2 totals.
- **NEW RESEARCH G1 upgrades 10/10 in BATCH 01**, IDs `01.1,01.2,01.3,01.4,01.5,01.6,01.7,02.1,02.2,02.3`: each has `*_v2.py` and `test_*_v2.py` in corresponding `research/techniques/<ID>/`; original V1 modules unchanged. New code isolates ADC/Rs topology, physical R0 source, log curve domain, measured heater phase, environment covariates, sample quality, physical drift, causal segments, grouped scaler and measured time kinetics.
- **Reproducible software gate:** [CI run #39545356321](https://github.com/HazRob/HazRob-Tank/actions/runs/39545356321) on code SHA `420908b38fee01e3c7c4ffc1a167b6254f86fff2` shows **10 independent jobs successful and 336 existing+new tests passed**; [V4 full first-batch plan gate #39545362103](https://github.com/HazRob/HazRob-Tank/actions/runs/39545362103) also PASS.
- **Failure audit:** Old `01.3` unit check compared binary floats for exact equality at the graph endpoint and rejected 500 nominal ppm due to ULP rounding; fixed strict domain with `isclose(rel_tol=1e-12)` and corrected test. New workflow initially missed `numpy` required by legacy 01.5; added dependency and reran until all tests passed. Both failed and successful CI histories remain reviewable.
- **NEW G2 NOT YET COMPLETED** for V2: meaningful external-source empirical grouped tests are still pending. Original G2 was for previous prototypes/reference review, cannot be promoted to V2 results by renaming. **Actual Tank MQ8 G3 remains 0/80.** No Android, ESP32, ESP32-CAM, original HazRob or production ChemCore modifications.
- **Authoritative status:** `EXECUTION_LEDGER_2026-10-21.json`: first ten have `G1_RESEARCH_TESTS_PASS_G2_G3_PENDING`, remaining 70 await V4 upgrades. Batch details: `BATCH01_EVIDENCE_AR.md`.
- **Next planned implementation batch 02 (10 IDs):** `02.4,02.5,02.6,02.7,02.8,03.1,03.2,03.3,03.4,03.5`; prioritize Chinese `CN-RSCH-01` segmental average differentiation in 02.4; independent group-aware fingerprint evidence, source provenance, identifiability, and physical sensor selection. G2 external source comparisons for batch01 can proceed in parallel once original raw files are qualified.
- **Recovery order:** check PR/branch HEAD → `PLAN_80_V4.json` → `EXECUTION_LEDGER_2026-10-21.json` → `BATCH01_EVIDENCE_AR.md` → run CI for any changed code → begin 02.4. Never claim any physical ppm safety performance from these 336 software tests.

---

# PRIMARY RECOVERY CHECKPOINT — V4 BATCH01 G1 ENGINEERING COMPLETE (2026-10-21)

**Next task:** start **V4 batch02**, ten IDs `02.4,02.5,02.6,02.7,02.8,03.1,03.2,03.3,03.4,03.5`; first work 02.4 dynamic segmental average differentiation vs existing temporal features and 02.5 source-qualified pure/complex fingerprints. **No production changes.**

- **Snapshot:** V4 source analysis `PLAN_80_V4.json` and [PLAN_80_V4_AR.md](PLAN_80_V4_AR.md) with 80 detailed work orders; source catalogue pinned blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f`.
- **Batch01:** IDs `01.1–01.7,02.1–02.3` **10/10 newly developed at software engineering/G1 level**, with [336/336 Python tests pass, Actions #39545362103](https://github.com/HazRob/HazRob-Tank/actions/runs/39545362103), tested commit `420908b38fee01e3c7c4ffc1a167b6254f86fff2`.
- **Authoritative ledger:** `EXECUTION_LEDGER_2026-10-21.json`; **audited per-ID table and known defects:** `BATCH01_EVIDENCE_AR.md`.
- **G2 scientific real dataset benchmarks newly added:** no claim at batch01. Historical G2 source-eligibility 80/80 remains intact; per-ID G2-V4 source data evaluation and reproducible holdings are still required. **Tank physical G3 0/80**. This is not a gas/ppm safety certification.
- **Parallel V2 research implementations:** 01.2, 01.3, 02.2 have alternative source-aware versions present in research branch; harmonize interfaces and test scientific equivalence before using either from actual ChemCore. Do not promote any version to HR069, Android Tank or MCU firmware without separate request and measured standards.
- **Required continuity steps:** (1) inspect current research branch HEAD and PR #6; (2) validate updated ledger with `python research/implementation_execution/validate_plan_v4.py`; (3) inspect batch01 CI and failed attempts for audit; (4) read V4 exact 02.4 work order and central sources `CN-RSCH-01,SYS-17,JP-FIG-02,MQ-EXT-10`; (5) create 02.4 new research code/tests, verify CI and write `BATCH02_EVIDENCE_AR.md`.
- **Scientific blocker:** No Tank-specific true reference gas exposures with measured RL/R0/heater/BME and separate sessions. No certified ppm, toxic gas sensing or G3 claims.
- **User reporting cadence:** summarize progress for user on completion of *each ten-technique batch*; preserve all per-ID commits and unresolved data requirements continuously.

---

# LATEST RESTART POINT — V4 BATCH01 RESEARCH IMPLEMENTATION (2026-10-21)

- **HEAD verified CI:** `18dbc262afa28aa2005dd03b190c12ca354126c1`, [GitHub Actions #39544492918](https://github.com/HazRob/HazRob-Tank/actions/runs/39544492918): **207/207 unit tests passed across IDs 01.1–02.3**.
- **New source-aware engineering prototypes:** IDs `01.1`, `01.2`, `01.3` — **3/10 newly developed in batch01**. `01.4`, `01.5`, `01.6`, `01.7`, `02.1`, `02.2`, `02.3` have only historical software prototypes despite passing historical tests.
- **Ledger source:** `research/implementation_execution/EXECUTION_LEDGER_2026-10-21.json`; **batch evidence:** `research/implementation_execution/BATCH01_EVIDENCE_AR.md`. Follow their updated statuses; an older paragraph below recording 01.1/01.2 as CODE_COMMITTED_CI_PENDING is now historical, not the current status.
- **Main G1/G2/G3 counts remain:** 80/80 historical G1; 80/80 external-source G2 eligibility; **0/80 Tank real-gas G3**, and no activated ChemCore/Android/ESP32 changes.
- **Next task:** technical scientific contracts and implemented evidence/test cases for **01.4 heater-state metadata**, then `01.5` environmental reference calibration, `01.6` chamber signal quality/flow and filter-state; proceed through remaining IDs to complete the first batch of 10, preserving 10-per-batch reporting.
- **Known merge concern:** alternative V2 prototypes of 01.2/01.3 are currently present; choose one audited canonical module per technique before any integration. Do not use source metadata SHA field as proof actual bytes were hashed.
- **Immutable scientific reference source:** pinned central catalogue blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f` used in V4 plan; refresh bibliography only with explicit source revision record. PR #6 is a research review, not an implicit production merge.

---

# HazRob Tank ChemCore V4 — recovery checkpoint (2026-10-21)

**Branch:** `research/chemcore80-central-evidence-gap-analysis-20261021` (**research-only**); canonical `main` untouched until PR review.

## Stable and immutable evidence
- `research/implementation_execution/PLAN_80_V4.json`: 80 named technique work orders, immutable baseline source catalog Git blob `1b3e0ce87f3625c0b6e5d514090abbfcb0911c7f`, eight original batches of 10, per-ID sources, testing and G3 criteria.
- `research/implementation_execution/PLAN_80_V4_AR.md`: full Arabic procedures for all 80.
- `research/systems_analysis/CHEMCORE80_REFERENCE_COMPARATIVE_SYSTEMS_ANALYSIS_2026-10-21_AR.md` and `TECH80_CENTRAL_REFERENCE_CROSSWALK_2026-10-21.json`: source-to-technique rationale, 23 redesign / 52 upgrades / 5 retained.
- `research/implementation_execution/EXECUTION_LEDGER_2026-10-21.json`: latest authoritative V4 execution statuses (NOT historical G1 or G2 record).

## Historical and new gates
Historical `research/PROGRESS_80.json`: 80/80 original G1 software prototype & G2 reference-eligibility reviewed (49 external data operations, 29 blocked, 2 theoretical), **Tank real MQ8 physical G3 = 0/80**. No operation in this research branch may upgrade G3 absent traceable actual Tank reference exposures. No APK, ESP32/ESP32-CAM, original HazRob, production ChemCore edits.

## Run order after interruption
1. Fetch newest branch HEAD and `EXECUTION_LEDGER_2026-10-21.json`; inspect PR and CI; do not assume any unfinished operation committed.
2. Review `PLAN_80_V4.json` and `PLAN_80_V4_AR.md`, then source catalog pinned SHA.
3. Continue earliest batch-01 ID with `PLANNED_NOT_EXECUTED` / `IMPLEMENTED_TESTS_PENDING`; do not reimplement committed validated code. Validate each stage with CI, record run URL/commit.
4. Update `BATCH01_EVIDENCE_AR.md` after each work item; publish cumulative recap only when all 10 complete, preserve blocked/negative findings.
5. Keep original historical `research/PROGRESS_80.json` and main untouched; this research branch is a proposal until PR merges.

## Physical blockers
- Measured Tank RL/Vc/ADC divider mapping, physical MQ instance IDs, heater HIGH/LOW cycle, R0 protocol, controlled blank and certified gas references, BME280 and actual session groups are required for G3.
- Publisher curves / cross-device R0 are priors not calibrations. Mock tests and Mendeley / UCI external arrays may help G1/G2 only.
