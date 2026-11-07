# G2 source-status reconciliation — 2026-11-04

Scope: research-only evidence consistency audit; NOT a G2/G3 qualification or change to ChemCore Stable.

## Verified inconsistency
The research branch currently contains two catalog-derived snapshots that disagree on MQ-EXT-01:

- `research/implementation_execution/G2_CENTRAL_SOURCE_CANDIDATES_2026-10-26.json` reports `ORIGINAL_ARCHIVE_SHA256_AND_PUBLISHER_MD5_VERIFIED`, archive SHA-256 `fde5a3836fe9ddb04fee90102b9c1a5816f48a78d37bf7125074e66a3f3b5f5a`, publisher MD5 `008b8ccb62d7a7d51ff9ffcfd698a1e5`, and CI run `40287072270`. It explicitly says `SOURCE_MEASURED_EXPLORATORY_ONLY_G2_BLOCKED`, with unverified physical grouping and signal units.
- `research/implementation_execution/G2_CENTRAL_CATALOG_SOURCE_READINESS_2026-10-28.json` reports `archive_listed_not_downloaded`, `SOURCE_DESCRIPTION_VERIFIED_RAW_PENDING`, and `ORIGINAL_RAW_SHA256_NOT_DOCUMENTED`.

These assertions cannot both describe the same source-acquisition state at the same time. The later-dated readiness snapshot must not silently override the more detailed earlier acquisition evidence, nor may the earlier claim be accepted without replaying its provenance. The conflict concerns *source acquisition status*, not scientific eligibility: both snapshots block G2 approval.

## Reproducible consistency check

From the repository root, with Python 3:

```python
import json
from pathlib import Path
p = Path("research/implementation_execution")
a = json.loads((p / "G2_CENTRAL_SOURCE_CANDIDATES_2026-10-26.json").read_text())
b = json.loads((p / "G2_CENTRAL_CATALOG_SOURCE_READINESS_2026-10-28.json").read_text())
x = next(v for v in a["candidates"] if v["id"] == "MQ-EXT-01")
y = next(v for v in b["datasets"] if v["id"] == "MQ-EXT-01")
assert x["raw_file_status"] == "ORIGINAL_ARCHIVE_SHA256_AND_PUBLISHER_MD5_VERIFIED"
assert y["raw_file_status"] == "SOURCE_DESCRIPTION_VERIFIED_RAW_PENDING"
assert x["measurement_qualification"] is False
assert y["measurement_qualification"] is False
print("SOURCE_STATUS_CONFLICT; G2_BLOCKED")
```

## Required resolution
1. Retrieve and verify the original bytes against the recorded SHA-256 and publisher checksum, with archived file name, retrieval timestamp, immutable URI and license.
2. Replay or inspect CI run 40287072270 and tie its input SHA to the catalog record; do not infer success from the run identifier alone.
3. Reconcile the two snapshots using a provenance-preserving supersession record; never rewrite historical observations.
4. Audit physical experiment/session grouping, units and reference concentrations, then run held-out group tests before considering any technique for G2.

No technique promotion, Stable change, or unverified source-ready claim is authorized by this note.
