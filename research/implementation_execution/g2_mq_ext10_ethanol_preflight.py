"""Fail-closed MQ-EXT-10 original-source ethanol preflight (research only).

Published liquid solution % v/v is NOT measured ethanol headspace ppm.
No raw archive is bundled; no G2/G3 promotion can result from this tool.
"""
import argparse
import hashlib
import io
import json
import math
import re
from decimal import Decimal
from pathlib import Path

SCHEMA = "hazrob.g2.mq-ext10-ethanol-preflight.v1"
SOURCE_ID = "MQ-EXT-10"
DOI = "10.17632/vpc887d53s.3"
LIQUID_PERCENT_BY_CODE = {"C1": "1", "C2": "2.5", "C3": "5", "C4": "10", "C5": "15", "C6": "20"}
SENSOR_CHANNELS = ["MQ3_A", "MQ4_A", "MQ6_A", "MQ3_B", "MQ4_B", "MQ6_B"]
NAME_RE = re.compile(r"^Ea[-_]?C([1-6])[-_]?R([0-9]{2,3})$", re.IGNORECASE)
NUMERIC_TOKEN_RE = re.compile(r"[+-]?(?:[0-9]+(?:\.[0-9]*)?|\.[0-9]+)(?:[eE][+-]?[0-9]{1,5})?", re.ASCII)
MAX_NUMERIC_TOKEN_LENGTH = 128
MAX_SOURCE_BYTES_PER_FILE = 4_000_000


def require(condition, code):
    if not condition:
        raise ValueError("MQ_EXT10_" + code)


def parse_ethanol_filename(stem):
    require(isinstance(stem, str), "INVALID_FILENAME")
    match = NAME_RE.fullmatch(stem)
    require(match is not None, "INVALID_FILENAME")
    code = "C" + match.group(1)
    repetition = int(match.group(2))
    require(repetition > 0, "INVALID_REPETITION")
    return code, repetition


def canonical_exact_decimal(token):
    """Lossless numeric identity; Decimal.normalize() rounds to context precision."""
    require(len(token) <= MAX_NUMERIC_TOKEN_LENGTH, "NUMERIC_TOKEN_TOO_LONG")
    require(NUMERIC_TOKEN_RE.fullmatch(token) is not None, "INVALID_NUMERIC_TOKEN")
    value = Decimal(token)
    require(value.is_finite(), "NONFINITE_ROW")
    parts = value.as_tuple()
    digits = list(parts.digits)
    if not any(digits):
        return "0"
    exponent = parts.exponent
    while digits[-1] == 0:
        digits.pop()
        exponent += 1
    return ("-" if parts.sign else "+") + "".join(map(str, digits)) + "e" + str(exponent)


def parse_numeric_rows(data, expected_rows=3330):
    """Accept only eight finite numeric fields per row; header is not assumed."""
    require(type(expected_rows) is int and expected_rows > 0, "EXPECTED_ROWS")
    try:
        decoded = data.decode("utf-8-sig")
    except UnicodeError as exc:
        raise ValueError("MQ_EXT10_SOURCE_ENCODING_UNVERIFIED") from exc
    rows = 0
    numeric_digest = hashlib.sha256()
    minima = [float("inf")] * 8
    maxima = [float("-inf")] * 8
    for line in io.StringIO(decoded):
        line = line.strip()
        require(bool(line), "BLANK_DATA_ROW")
        parts = ([x.strip() for x in line.split(",")] if "," in line
                 else [x.strip() for x in line.split(";")] if ";" in line
                 else line.split())
        require(len(parts) == 8, "COLUMN_COUNT_NOT_EIGHT")
        try:
            values = [float(x) for x in parts]
        except ValueError as exc:
            raise ValueError("MQ_EXT10_NONNUMERIC_ROW") from exc
        require(all(math.isfinite(x) for x in values), "NONFINITE_ROW")
        require(all(x > 0 for x in values[2:]), "NONPOSITIVE_RESISTANCE")
        canonical = [canonical_exact_decimal(t) for t in parts]
        numeric_digest.update(("\x1f".join(canonical) + "\n").encode("ascii"))
        for i, x in enumerate(values):
            minima[i] = min(minima[i], x)
            maxima[i] = max(maxima[i], x)
        rows += 1
        require(rows <= expected_rows, "TOO_MANY_ROWS")
    require(rows == expected_rows, "ROW_COUNT_MISMATCH")
    return {"rows": rows, "ranges": [[minima[i], maxima[i]] for i in range(8)],
            "numeric_trace_sha256": numeric_digest.hexdigest()}


def inspect_file(path, expected_rows=3330):
    path = Path(path)
    require(path.is_file() and not path.is_symlink(), "NOT_REGULAR_FILE")
    require(path.stat().st_size <= MAX_SOURCE_BYTES_PER_FILE, "FILE_TOO_LARGE")
    code, repetition = parse_ethanol_filename(path.stem)
    data = path.read_bytes()
    summary = parse_numeric_rows(data, expected_rows)
    return {"filename": path.name, "concentration_code": code,
            "nominal_liquid_ethanol_percent_vv": LIQUID_PERCENT_BY_CODE[code],
            "repetition": repetition, "sha256": hashlib.sha256(data).hexdigest(),
            "rows": summary["rows"], "ranges": summary["ranges"],
            "numeric_trace_sha256": summary["numeric_trace_sha256"]}


def run(raw_root=None):
    """Optional raw scan never grants G2; physical provenance still required."""
    result = {
        "schema": SCHEMA, "source_id": SOURCE_ID, "source_version_doi": DOI,
        "source_page": "https://data.mendeley.com/datasets/vpc887d53s/3",
        "source_metadata_kind": "PUBLISHED_DATASET_DESCRIPTION_NOT_RAW_VERIFICATION",
        "reported_ethanol_measurements": 65,
        "reported_wine_measurements": 235,
        "reported_rows_per_measurement": 3330,
        "reported_sampling_hz": "18.5", "reported_duration_seconds": 180,
        "channels": ["relative_humidity_percent", "temperature_celsius"] +
                    [f"{name}_resistance_kohm" for name in SENSOR_CHANNELS],
        "nominal_liquid_ethanol_percent_vv_by_code": LIQUID_PERCENT_BY_CODE,
        "headspace_ethanol_ppm_measured": False,
        "liquid_percent_vv_is_gas_ppm": False,
        "r0_per_sensor_traceably_calibrated": False,
        "independent_exposure_groups_verified": False,
        "independent_tank_exposures": False,
        "g2_qualified": False, "g3_qualified": False,
        "source_status": "ORIGINAL_RAW_NOT_PRESENT_IN_THIS_RUN",
        "files_scanned": 0, "rows_scanned": 0, "file_manifest": [],
        "blockers": ["ORIGINAL_RAW_SHA256_NOT_VERIFIED", "PHYSICAL_GROUP_INDEPENDENCE_NOT_VERIFIED",
                     "HEADSPACE_GAS_PPM_REFERENCE_NOT_MEASURED", "R0_CALIBRATION_NOT_VERIFIED"],
    }
    if raw_root is None:
        return result
    root = Path(raw_root)
    require(root.is_dir() and not root.is_symlink(), "RAW_ROOT_NOT_DIRECTORY")
    entries = sorted(root.iterdir())
    require(all(p.is_file() and not p.is_symlink() for p in entries),
            "NONREGULAR_OR_NESTED_SOURCE_ENTRY")
    files = entries
    require(len(files) > 0, "NO_ETHANOL_FILES")
    require(len(files) <= 65, "MORE_FILES_THAN_REPORTED_MEASUREMENTS")
    require(all(p.suffix.lower() in {".txt", ".csv", ".dat"} for p in files),
            "UNSUPPORTED_EXTENSION")
    seen = set()
    seen_bytes = set()
    seen_numeric_traces = set()
    manifest = []
    for p in files:
        row = inspect_file(p)
        key = (row["concentration_code"], row["repetition"])
        require(key not in seen, "DUPLICATE_CONCENTRATION_REPETITION")
        require(row["sha256"] not in seen_bytes, "DUPLICATE_MEASUREMENT_BYTES")
        require(row["numeric_trace_sha256"] not in seen_numeric_traces,
                "DUPLICATE_MEASUREMENT_NUMERIC_TRACE")
        seen.add(key)
        seen_bytes.add(row["sha256"])
        seen_numeric_traces.add(row["numeric_trace_sha256"])
        manifest.append(row)
    result["source_status"] = ("RAW_FILES_SCANNED_UNQUALIFIED" if len(files) == 65
                               else "PARTIAL_RAW_FILES_SCANNED_UNQUALIFIED")
    result["files_scanned"] = len(files)
    result["rows_scanned"] = sum(x["rows"] for x in manifest)
    result["file_manifest"] = manifest
    result["blockers"] = [
        "ORIGINAL_ARCHIVE_BYTE_PROVENANCE_NOT_VERIFIED",
        "PHYSICAL_GROUP_INDEPENDENCE_NOT_VERIFIED",
        "HEADSPACE_GAS_PPM_REFERENCE_NOT_MEASURED",
        "R0_CALIBRATION_NOT_VERIFIED",
    ]
    if len(files) != 65:
        result["blockers"].insert(0, "INCOMPLETE_REPORTED_ETHANOL_MEASUREMENTS")
    return result


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--raw-root", type=Path, help="Optional local Ethanol folder, never required by CI")
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    encoded = json.dumps(run(args.raw_root), ensure_ascii=False, sort_keys=True,
                         separators=(",", ":"), allow_nan=False) + "\n"
    if args.output:
        args.output.write_text(encoded, encoding="utf-8")
    else:
        print(encoded, end="")


if __name__ == "__main__":
    main()
