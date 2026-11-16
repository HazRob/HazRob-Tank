# MQ-EXT-10: paired folder/ZIP source-byte parity — 2026-11-02

**Scope:** G2 research-source intake integrity only. No original publisher archive or Tank exposure was obtained during this change. The reported ethanol C1–C6 labels refer to liquid solution **% v/v**, not independently measured headspace ethanol ppm.

## Integrity contract

`g2_mq_ext10_folder_zip_parity.py` requires both an ethanol folder and the publisher ZIP in the same invocation, or neither for a truthful no-raw CI receipt. It independently invokes the existing folder and ZIP parsers, indexes samples by (concentration code, repetition), and fails closed if membership differs. For matching samples it requires identical raw **SHA256**, numerical-trace SHA256, eight-channel parsed ranges, row count, liquid labels and repetitions. It also reconciles six-level counts, published 65-sample count and level coverage. The canonical paired manifest digest is deterministic under file order changes. No ZIP extraction occurs.

**Run with original files, when available:**

```bash
python research/implementation_execution/g2_mq_ext10_folder_zip_parity.py \
  --raw-root /path/to/Ethanol \
  --raw-zip /path/to/original-publisher-v3.zip \
  --output /path/to/paired-source-receipt.json
```

**No-raw CI invocation:** `python research/implementation_execution/g2_mq_ext10_folder_zip_parity.py --output /tmp/paired.json`. It emits `PAIRED_ORIGINAL_RAW_NOT_AVAILABLE`, not an invented passing parity result.

Seven deterministic synthetic tests exercise no-input replay, missing paired input, byte-matched paired paths, membership mismatch, altered bytes, numerically equivalent but byte-distinct input, and digest replay. CI compiles the module, discovers its tests and uploads the no-raw receipt.

## Evidence limitations

Agreement between a folder and ZIP proves consistency of two ingestion paths for supplied bytes, **not** that those bytes originated from the publisher. A computed archive SHA256 is not publisher-authenticated without an independently trusted checksum or equivalent provenance. Neither source consistency nor 65 files establishes sample-group independence, gas-phase concentration, traceable MQ R0 calibration, Tank hardware validation or scientific accuracy. All G2/G3 promotion flags remain false. No ChemCore Stable, Android, firmware or main-branch changes are authorized by this work.
