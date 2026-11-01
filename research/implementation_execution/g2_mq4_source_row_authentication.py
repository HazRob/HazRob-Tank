"""SHA-pinned original MQ4 CSV row authentication for submitted predictions.

Research-only. This requires the original archive and independently submitted
same-row predictions. It never infers physical exposure independence or G2/G3.
"""
import argparse
import csv
import hashlib
import io
import math
import json
import zipfile
from datetime import datetime
from pathlib import Path

from g2_mq4_original_zip_replay import MEMBER, derive
from g2_mq4_paired_prediction_intake import audit
from g2_mq4_pairwise_frechet_bounds import EVIDENCE, SOURCE, strict_json
from g2_mq4_three_way_source_reconciliation import ARCHIVE_SHA, MEMBER_SHA, REPLAY

SOURCE_ID = "MQ-ACQ-2026-06"
PINNED_MEMBER_BYTES = 4787918
MAX_MEMBER_BYTES = 8_000_000


def sha256(raw):
    return hashlib.sha256(raw).hexdigest()


def canonical_row_sha256(columns, values):
    """Hash UTF-8 JSON of ordered parsed CSV fields (not literal CSV line bytes).

    Exact serialization: compact JSON list [column_names, cell_strings],
    ensure_ascii=False, UTF-8; independent of CSV quoting and newline style.
    """
    payload = json.dumps([columns, values], ensure_ascii=False,
                         separators=(",", ":")).encode("utf-8")
    return sha256(payload)


def authenticate_rows(csv_bytes, submitted_rows, *, require_full=True):
    """Match every declared source-row ID, day and canonical digest to CSV bytes.

    The synthetic-test mode is deliberately partial. Only the production
    caller may assert a full 14,186-row match against SHA-pinned ZIP bytes.
    """
    if not isinstance(submitted_rows, list) or not submitted_rows:
        raise ValueError("NO_SUBMITTED_ROWS")
    try:
        reader = csv.reader(io.StringIO(csv_bytes.decode("utf-8-sig"), newline=""),
                            strict=True)
        columns = next(reader)
        if len(columns) != 20 or len(set(columns)) != 20 or columns[0] != "datetime":
            raise ValueError("INVALID_SOURCE_HEADER")
        expected = {}
        for index, values in enumerate(reader, 1):
            if len(values) != len(columns):
                raise ValueError("SOURCE_COLUMN_COUNT_MISMATCH")
            dt = datetime.strptime(values[0], "%Y-%m-%d %H:%M:%S")
            if dt.strftime("%Y-%m-%d %H:%M:%S") != values[0]:
                raise ValueError("INVALID_SOURCE_TIMESTAMP")
            row_id = f"{SOURCE_ID}:{index:06d}"
            expected[row_id] = (dt.date().isoformat(),
                                canonical_row_sha256(columns, values))
    except (UnicodeError, csv.Error, StopIteration) as exc:
        raise ValueError("INVALID_SOURCE_CSV") from exc
    if not expected:
        raise ValueError("EMPTY_SOURCE")
    if require_full and len(submitted_rows) != len(expected):
        raise ValueError("SOURCE_ROW_COVERAGE_MISMATCH")
    seen = set()
    for row in submitted_rows:
        if not isinstance(row, dict):
            raise ValueError("INVALID_SUBMITTED_ROW")
        rid = row.get("row_id")
        if not isinstance(rid, str) or rid not in expected:
            raise ValueError("SOURCE_ROW_ID_MISMATCH")
        if rid in seen:
            raise ValueError("DUPLICATE_SUBMITTED_SOURCE_ROW")
        seen.add(rid)
        day, digest = expected[rid]
        if row.get("test_day") != day or row.get("fold_test_day") != day:
            raise ValueError("SOURCE_ROW_DAY_MISMATCH")
        if row.get("raw_row_sha256") != digest:
            raise ValueError("SOURCE_ROW_DIGEST_MISMATCH")
    if require_full and len(seen) != len(expected):
        raise ValueError("SOURCE_ROW_COVERAGE_MISMATCH")
    return {
        "source_rows": len(expected),
        "authenticated_submitted_rows": len(seen),
        "complete_source_row_coverage": require_full,
        "row_digest_method": "sha256-utf8-json-ordered-csv-fields-v1",
        "model_predictions_independently_reproduced": False,
        "physical_exposure_independence_verified": False,
        "g2_qualified": False,
        "g3_qualified": False,
    }


def authenticate_reference_labels(csv_bytes, submitted_rows, *, require_full=True):
    """Recompute every submitted 4-ppm truth label from source LiCor ppb.

    Mirrors the pinned Library benchmark: target = licor_ch4 / 1000.0,
    known_high = target >= 4.0. This authenticates source labels only;
    it does NOT reproduce any model predictions or qualify G2/G3.
    """
    if not isinstance(submitted_rows, list) or not submitted_rows:
        raise ValueError("NO_SUBMITTED_ROWS")
    try:
        reader = csv.DictReader(
            io.StringIO(csv_bytes.decode("utf-8-sig"), newline=""), strict=True)
        columns = reader.fieldnames
        if not columns or len(columns) != 20 or len(set(columns)) != 20 \
                or columns[0] != "datetime" or "licor_ch4" not in columns:
            raise ValueError("INVALID_SOURCE_HEADER")
        expected = {}
        for index, record in enumerate(reader, 1):
            if None in record or any(v is None for v in record.values()):
                raise ValueError("SOURCE_COLUMN_COUNT_MISMATCH")
            ppb = float(record["licor_ch4"])
            if not math.isfinite(ppb) or ppb <= 0:
                raise ValueError("INVALID_SOURCE_REFERENCE_PPB")
            expected[f"{SOURCE_ID}:{index:06d}"] = ppb / 1000.0 >= 4.0
    except (UnicodeError, csv.Error, OverflowError) as exc:
        raise ValueError("INVALID_SOURCE_CSV") from exc
    except (TypeError, ValueError) as exc:
        if isinstance(exc, ValueError) and str(exc) in (
                "INVALID_SOURCE_HEADER", "SOURCE_COLUMN_COUNT_MISMATCH",
                "INVALID_SOURCE_REFERENCE_PPB"):
            raise
        raise ValueError("INVALID_SOURCE_REFERENCE_PPB") from exc
    if require_full and len(submitted_rows) != len(expected):
        raise ValueError("SOURCE_ROW_COVERAGE_MISMATCH")
    if len(expected) == 14186 and sum(expected.values()) != 2346:
        raise ValueError("SOURCE_REFERENCE_MARGINAL_MISMATCH")
    seen = set()
    for row in submitted_rows:
        if not isinstance(row, dict):
            raise ValueError("INVALID_SUBMITTED_ROW")
        rid = row.get("row_id")
        if not isinstance(rid, str) or rid not in expected:
            raise ValueError("SOURCE_ROW_ID_MISMATCH")
        if rid in seen:
            raise ValueError("DUPLICATE_SUBMITTED_SOURCE_ROW")
        seen.add(rid)
        if type(row.get("reference_positive_4ppm")) is not bool:
            raise ValueError("INVALID_SUBMITTED_REFERENCE_LABEL")
        if row["reference_positive_4ppm"] != expected[rid]:
            raise ValueError("SOURCE_REFERENCE_LABEL_MISMATCH")
    if require_full and len(seen) != len(expected):
        raise ValueError("SOURCE_ROW_COVERAGE_MISMATCH")
    return {
        "source_rows": len(expected),
        "reference_positive_rows": sum(expected.values()),
        "authenticated_reference_labels": len(seen),
        "complete_reference_label_coverage": require_full,
        "reference_threshold_ppm": 4.0,
        "reference_source_unit": "LiCor ppb",
        "model_predictions_independently_reproduced": False,
        "g2_qualified": False,
        "g3_qualified": False,
    }


def validate_replay_metadata(replay):
    """Fail closed on historical raw-replay provenance and scientific status.

    Source bytes and derived statistics are checked separately by the caller.
    A self-declared successful replay is NOT independent physical evidence.
    """
    if not isinstance(replay, dict) or replay.get("schema") != (
            "hazrob.g2.mq4-original-zip-raw-replay.v1"):
        raise ValueError("REPLAY_SCHEMA_MISMATCH")
    if replay.get("source_id") != SOURCE_ID or (
            replay.get("archive_sha256") != ARCHIVE_SHA or
            replay.get("raw_member_sha256") != MEMBER_SHA):
        raise ValueError("REPLAY_LINEAGE_MISMATCH")
    if type(replay.get("raw_member_bytes")) is not int or (
            replay["raw_member_bytes"] != PINNED_MEMBER_BYTES):
        raise ValueError("REPLAY_MEMBER_SIZE_MISMATCH")
    if replay.get("raw_replayed_in_this_execution") is not True or (
            replay.get("summary_fields_verified") is not True):
        raise ValueError("REPLAY_COMPLETENESS_MISMATCH")
    for key in ("g2_qualified", "g3_tank_qualified",
                "physical_exposure_independence_verified",
                "reference_calibration_certificate_verified",
                "blinded_holdout_verified"):
        if replay.get(key) is not False:
            raise ValueError("REPLAY_UNSUPPORTED_SCIENTIFIC_PROMOTION")
    if not isinstance(replay.get("derived"), dict):
        raise ValueError("REPLAY_DERIVED_MISSING")


def verify_original_archive(archive_bytes, replay_raw):
    """Verify source archive/member bytes and independently re-derive report."""
    if sha256(archive_bytes) != ARCHIVE_SHA:
        raise ValueError("ARCHIVE_SHA_MISMATCH")
    replay = strict_json(replay_raw)
    validate_replay_metadata(replay)
    try:
        with zipfile.ZipFile(io.BytesIO(archive_bytes)) as z:
            names = z.namelist()
            if len(names) != len(set(names)) or names.count(MEMBER) != 1:
                raise ValueError("ZIP_MEMBER_AMBIGUITY")
            info = z.getinfo(MEMBER)
            if info.file_size != PINNED_MEMBER_BYTES or info.file_size > MAX_MEMBER_BYTES:
                raise ValueError("ZIP_MEMBER_SIZE_MISMATCH")
            raw = z.read(MEMBER)
    except (zipfile.BadZipFile, RuntimeError, OSError) as exc:
        raise ValueError("INVALID_SOURCE_ARCHIVE") from exc
    if len(raw) != PINNED_MEMBER_BYTES or sha256(raw) != MEMBER_SHA:
        raise ValueError("SOURCE_MEMBER_SHA_MISMATCH")
    if derive(raw) != replay.get("derived"):
        raise ValueError("SOURCE_REPLAY_DERIVATION_MISMATCH")
    return raw


def authenticate_paired_submission(archive_bytes, submission_raw,
                                   source_raw, evidence_raw, replay_raw):
    """Fail closed unless all source rows and original benchmark marginals match."""
    submitted_audit = audit(submission_raw, source_raw, evidence_raw, require_full=True)
    raw = verify_original_archive(archive_bytes, replay_raw)
    submitted = strict_json(submission_raw)
    row_audit = authenticate_rows(raw, submitted["rows"], require_full=True)
    label_audit = authenticate_reference_labels(raw, submitted["rows"], require_full=True)
    if row_audit["source_rows"] != 14186:
        raise ValueError("PINNED_SOURCE_ROW_COUNT_MISMATCH")
    return {
        "schema": "hazrob.g2.mq4-source-row-authentication.v1",
        "source_id": SOURCE_ID,
        "source_archive_sha256": ARCHIVE_SHA,
        "source_member_sha256": MEMBER_SHA,
        "submitted_rows": row_audit["authenticated_submitted_rows"],
        "source_row_digest_method": row_audit["row_digest_method"],
        "submitted_rows_matched_to_sha_pinned_archive": True,
        "reference_labels_matched_to_sha_pinned_archive": True,
        "authenticated_reference_labels": label_audit["authenticated_reference_labels"],
        "source_reference_positive_rows": label_audit["reference_positive_rows"],
        "source_benchmark_marginals_reconciled": submitted_audit["full_source_marginals_match"],
        "model_predictions_independently_reproduced": False,
        "blinded_independent_holdouts_verified": False,
        "physical_independent_exposure_evidence": False,
        "paired_inferential_test_authorized": False,
        "g2_qualified": False,
        "g3_qualified": False,
    }


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--archive", type=Path, required=True,
                    help="Original SHA-pinned source ZIP, never downloaded by CI")
    ap.add_argument("--submission", type=Path, required=True,
                    help="Full 14,186-row externally submitted predictions JSON")
    ap.add_argument("--source", type=Path, default=SOURCE)
    ap.add_argument("--evidence", type=Path, default=EVIDENCE)
    ap.add_argument("--replay", type=Path, default=REPLAY)
    ap.add_argument("--output", type=Path, required=True)
    a = ap.parse_args()
    result = authenticate_paired_submission(
        a.archive.read_bytes(), a.submission.read_bytes(),
        a.source.read_bytes(), a.evidence.read_bytes(), a.replay.read_bytes())
    a.output.write_text(json.dumps(result, sort_keys=True, indent=2) + "\n",
                        encoding="utf-8")
    print("Source row hashes matched; G2/G3 and paired inference remain blocked")


if __name__ == "__main__":
    main()
