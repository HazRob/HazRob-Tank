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
