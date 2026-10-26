"""Verify a G2 candidate's on-repository evidence bundle against its bytes.

This is an integrity/availability gate, not independent scientific review.
All paths are anchored under research/g2/evidence/<technique>/<dataset>/<run>/.
"""
import hashlib
import json
import csv
import re
from pathlib import Path

FILES = (
    "raw_manifest.json", "source_qualification.json", "signal_lineage.json",
    "group_manifest.csv", "split_manifest.json", "protocol_locked.json",
    "calibration_manifest.json", "predictions_blind.csv",
    "metrics_per_group.csv", "metrics_summary.json",
    "uncertainty_grouped.json", "reproduce.sh", "ci_run.json",
    "review_decision.json",
)
HEX64 = re.compile(r"^[0-9a-f]{64}$")
SAFE_SEGMENT = re.compile(r"^[A-Za-z0-9][A-Za-z0-9_.-]{0,79}$")


def _safe(value):
    return (isinstance(value, str) and bool(SAFE_SEGMENT.fullmatch(value))
            and value not in {".", ".."} and ".." not in value)


def audit_evidence_bundle(record, repo_root=Path(".")):
    """Fail closed on absent, unsafe, missing, or checksum-mismatched evidence.

    evidence_files is an exact filename-to-SHA256 map, not an arbitrary
    filesystem path map. The caller must still independently review the
    scientific meaning, grouping provenance, and reference traceability.
    """
    issues = []
    technique_id = record.get("id")
    dataset_id = record.get("dataset_id")
    run_id = record.get("run_id")
    if not all(_safe(v) for v in (technique_id, dataset_id, run_id)):
        return ["INVALID_EVIDENCE_BUNDLE_ID_OR_PATH"]
    manifest = record.get("evidence_files")
    if not isinstance(manifest, dict):
        return ["MISSING_EVIDENCE_BUNDLE"]
    missing = set(FILES) - set(manifest)
    extra = set(manifest) - set(FILES)
    if missing:
        issues.append("MISSING_EVIDENCE_FILES:" + ",".join(sorted(missing)))
    if extra:
        issues.append("UNEXPECTED_EVIDENCE_FILES:" + ",".join(sorted(extra)))
    base = (Path(repo_root).resolve() / "research" / "g2" / "evidence"
            / technique_id / dataset_id / run_id)
    root = Path(repo_root).resolve()
    for filename in FILES:
        digest = manifest.get(filename)
        if not isinstance(digest, str) or not HEX64.fullmatch(digest):
            issues.append("INVALID_EVIDENCE_SHA256:" + filename)
            continue
        path = base / filename
        try:
            resolved = path.resolve(strict=True)
            resolved.relative_to(root)
            if resolved != path.absolute() or path.is_symlink() or not resolved.is_file():
                raise ValueError("not a regular non-symlink file")
            sha = hashlib.sha256()
            with resolved.open("rb") as stream:
                for chunk in iter(lambda: stream.read(1024 * 1024), b""):
                    sha.update(chunk)
        except (OSError, ValueError):
            issues.append("EVIDENCE_FILE_UNAVAILABLE_OR_UNSAFE:" + filename)
            continue
        if sha.hexdigest() != digest:
            issues.append("EVIDENCE_FILE_SHA256_MISMATCH:" + filename)
            continue
        # Byte integrity alone is insufficient: reject unreadable/empty
        # evidence containers before they become review candidates.
        try:
            if filename.endswith(".json"):
                payload = json.loads(resolved.read_text(encoding="utf-8"))
                if not isinstance(payload, dict) or not payload:
                    raise ValueError("empty or non-object JSON")
            elif filename.endswith(".csv"):
                with resolved.open(newline="", encoding="utf-8") as stream:
                    rows = csv.reader(stream)
                    header = next(rows)
                    data = next(rows)
                    if not header or any(not c.strip() for c in header) or len(data) != len(header):
                        raise ValueError("invalid CSV header or first data row")
            elif filename == "reproduce.sh":
                if not resolved.read_text(encoding="utf-8").startswith("#!/"):
                    raise ValueError("missing executable script header")
        except (OSError, ValueError, UnicodeError, StopIteration, csv.Error):
            issues.append("INVALID_EVIDENCE_FILE_CONTENT:" + filename)
    return issues
