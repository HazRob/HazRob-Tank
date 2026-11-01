"""Fail-closed integrity audit for four pinned external MQ4 forward-day receipts.

This audits Git-tracked research receipts, not original ZIP replay, independent
Tank measurements, or G2/G3 scientific qualification.
"""
import argparse
import hashlib
import json
import math
from pathlib import Path

from g2_mq4_forward_day_stability import analyze
from g2_mq4_forward_day_ranking import build as rank_days
from g2_mq4_forward_day_support_strata import build as support_strata

ROOT = Path(__file__).resolve().parent
FILENAMES = {
    "source": "G2_MQ4_FORWARD_4PPM_2026-11-01.json",
    "stability": "G2_MQ4_FORWARD_DAY_STABILITY_2026-11-01.json",
    "ranking": "G2_MQ4_FORWARD_DAY_RANKING_2026-11-01.json",
    "support": "G2_MQ4_FORWARD_DAY_SUPPORT_STRATA_2026-11-01.json",
}
NO_APPROVAL = ("g2_qualified", "g3_qualified",
               "independent_tank_exposures", "blinded_prospective",
               "traceable_calibration")


def strict_json(raw):
    """Reject duplicate keys at every depth and nonfinite JSON numbers."""
    def unique(pairs):
        out = {}
        for key, value in pairs:
            if key in out:
                raise ValueError("MQ4_RECEIPT_DUPLICATE_KEY:" + key)
            out[key] = value
        return out

    def reject_constant(value):
        raise ValueError("MQ4_RECEIPT_NONFINITE:" + value)

    def finite_float(value):
        number = float(value)
        if not math.isfinite(number):
            raise ValueError("MQ4_RECEIPT_NONFINITE:" + value)
        return number

    try:
        obj = json.loads(raw.decode("utf-8"), object_pairs_hook=unique,
                         parse_constant=reject_constant, parse_float=finite_float)
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise ValueError("MQ4_RECEIPT_INVALID_JSON") from exc
    if not isinstance(obj, dict):
        raise ValueError("MQ4_RECEIPT_NOT_OBJECT")
    return obj


def audit(paths=None):
    paths = paths or {name: ROOT / filename for name, filename in FILENAMES.items()}
    if set(paths) != set(FILENAMES):
        raise ValueError("MQ4_RECEIPT_FILE_SET_MISMATCH")
    raw = {name: Path(paths[name]).read_bytes() for name in FILENAMES}
    obj = {name: strict_json(raw[name]) for name in FILENAMES}
    for name, item in obj.items():
        if any(item.get(flag) is not False for flag in NO_APPROVAL):
            raise ValueError("MQ4_RECEIPT_UNSUPPORTED_PROMOTION:" + name)
    if obj["source"].get("threshold_4ppm_is_safety_limit") is not False:
        raise ValueError("MQ4_RECEIPT_SAFETY_THRESHOLD_CLAIM")
    if analyze(obj["source"]) != obj["stability"]:
        raise ValueError("MQ4_RECEIPT_STABILITY_LINEAGE_MISMATCH")
    if rank_days(obj["source"], obj["stability"]) != obj["ranking"]:
        raise ValueError("MQ4_RECEIPT_RANKING_LINEAGE_MISMATCH")
    if support_strata(obj["source"], obj["stability"], obj["ranking"]) != obj["support"]:
        raise ValueError("MQ4_RECEIPT_SUPPORT_LINEAGE_MISMATCH")
    return {
        "schema": "hazrob.g2.mq4-receipt-lineage-ci.v1",
        "scope": "EXTERNAL_OBSERVATIONAL_RESEARCH_ONLY_NOT_G2_G3",
        "receipt_sha256": {name: hashlib.sha256(raw[name]).hexdigest()
                           for name in FILENAMES},
        "source_zip_sha256": obj["stability"]["source_zip_sha256"],
        "source_csv_sha256": obj["stability"]["source_csv_sha256"],
        "test_rows": obj["stability"]["rows"],
        "reference_positives": obj["stability"]["reference_positives"],
        "calendar_days": len(obj["stability"]["calendar_days"]),
        "g2_qualified": False,
        "g3_qualified": False,
        "independent_tank_exposures": False,
        "original_source_zip_replayed_in_ci": False,
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    result = audit()
    encoded = json.dumps(result, ensure_ascii=False, sort_keys=True,
                         separators=(",", ":"), allow_nan=False) + "\n"
    if args.output:
        args.output.write_text(encoded, encoding="utf-8")
    print(encoded, end="")


if __name__ == "__main__":
    main()
