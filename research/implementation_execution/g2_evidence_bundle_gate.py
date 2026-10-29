"""Verify a G2 candidate's on-repository evidence bundle against its bytes.

This is an integrity/availability gate, not independent scientific review.
All paths are anchored under research/g2/evidence/<technique>/<dataset>/<run>/.
"""
import hashlib
import json
import csv
import math
import unicodedata
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


def _csv_has_control(value):
    """Reject visually ambiguous Unicode in evidence CSV headers and cells.

    SHA256 proves byte identity, not canonical identity of a physical group
    or gas label. Require NFC (so equivalent composed/decomposed identifiers
    cannot be mixed), reject Unicode controls/formatting/separators and all
    non-ASCII-space Zs characters (e.g. NBSP, thin and ideographic spaces).
    Ordinary ASCII spaces and NFC multilingual text remain valid.
    """
    # Edge whitespace can create visually identical but distinct gas/group IDs.
    # In particular, majority_train strips training labels while blind truth
    # comparison uses exact CSV strings; an edge space could fake an improvement.
    # Interior ASCII spaces (e.g. "ethanol 50 ppm") remain valid.
    if value != value.strip():
        return True
    if unicodedata.normalize("NFC", value) != value:
        return True
    return any(unicodedata.category(ch).startswith("C") or
               unicodedata.category(ch) in ("Zl", "Zp") or
               (unicodedata.category(ch) == "Zs" and ch != " ")
               for ch in value)


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
    # A syntactically valid protocol digest is not evidence that the locked
    # protocol file is the one declared by the candidate. Bind the digest to
    # the byte-verified file; the later loop independently verifies its bytes.
    if record.get("protocol_sha256") != manifest.get("protocol_locked.json"):
        issues.append("PROTOCOL_DIGEST_NOT_BOUND_TO_LOCKED_FILE")
    # The declared evaluation digest must identify the exact blind-prediction
    # bytes included in this evidence bundle, not an unverified external hash.
    # File-byte validation below independently checks that digest.
    if record.get("evaluation_artifact_sha256") != manifest.get("predictions_blind.csv"):
        issues.append("EVALUATION_DIGEST_NOT_BOUND_TO_BLIND_PREDICTIONS")
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
                # Strict JSON: duplicate keys and nonfinite values can make
                # provenance/metrics ambiguous despite a matching SHA256.
                def unique_object(pairs):
                    result = {}
                    for key, value in pairs:
                        if key in result:
                            raise ValueError("duplicate JSON key: " + key)
                        result[key] = value
                    return result

                def reject_constant(value):
                    raise ValueError("nonfinite JSON constant: " + value)

                def finite_float(token):
                    # Python's JSON decoder accepts 1e9999 as infinity even
                    # when parse_constant rejects literal Infinity/NaN.
                    value = float(token)
                    if not math.isfinite(value):
                        raise ValueError("nonfinite JSON number: " + token)
                    return value

                def finite_int(token):
                    # Huge JSON integers may overflow float conversion in
                    # downstream metrics and must not crash the entire audit.
                    try:
                        value = int(token)
                        if not math.isfinite(float(value)):
                            raise OverflowError("integer outside finite numeric domain")
                    except (ValueError, OverflowError) as exc:
                        raise ValueError("out-of-range JSON integer") from exc
                    return value

                payload = json.loads(
                    resolved.read_text(encoding="utf-8"),
                    object_pairs_hook=unique_object,
                    parse_constant=reject_constant,
                    parse_float=finite_float,
                    parse_int=finite_int,
                )
                if not isinstance(payload, dict) or not payload:
                    raise ValueError("empty or non-object JSON")
            elif filename.endswith(".csv"):
                # Validate every record, not only the first data row: a
                # malformed blind prediction or group metric later in the
                # file must not pass the integrity gate.
                with resolved.open(newline="", encoding="utf-8") as stream:
                    rows = csv.reader(stream, strict=True)
                    header = next(rows)
                    if (not header or any(not c.strip() or _csv_has_control(c) or
                              unicodedata.normalize("NFKC", c) != c
                              for c in header)
                            or len(set(header)) != len(header)):
                        raise ValueError("invalid or duplicate CSV headers")
                    count = 0
                    for data in rows:
                        if len(data) != len(header) or any(_csv_has_control(c) for c in data):
                            raise ValueError("inconsistent CSV row or control character")
                        count += 1
                    if count == 0:
                        raise ValueError("CSV contains no data rows")
            elif filename == "reproduce.sh":
                if not resolved.read_text(encoding="utf-8").startswith("#!/"):
                    raise ValueError("missing executable script header")
        except (OSError, ValueError, UnicodeError, StopIteration, csv.Error):
            issues.append("INVALID_EVIDENCE_FILE_CONTENT:" + filename)
    return issues
