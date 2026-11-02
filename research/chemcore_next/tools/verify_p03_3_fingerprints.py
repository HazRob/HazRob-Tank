#!/usr/bin/env python3
"""P03.3 static scientific fingerprint provenance validation, no G2/G3 claims."""
import argparse
import collections
import csv
import json
import math
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[3]
JAVA = ROOT / "android/app/src/main/java/com/hazrob/tank/chemcore/v6/GasFingerprintCatalogV6.java"
BASE = ROOT / "research/chemcore_next/fingerprints"
SOURCES = BASE / "chemcore_next_p03_3_source_fingerprints_24.json"
INVENTORY = BASE / "chemcore_next_p03_3_source_fingerprint_inventory_24.csv"
METHODS = BASE / "chemcore_next_p03_3_3d4d_methods.json"

REGEX = re.compile(
    r'new Signature\("([^"]+)",\s*"([^"]+)",\s*"([^"]+)",\s*"([^"]+)",'
    r'\s*(-?\d+)L,\s*([\w.\-]+),\s*(\d+),\s*'
    r'new double\[\]\{([^}]+)\},\s*new double\[\]\{([^}]+)\}\)'
)

def numeric(value):
    value = value.strip()
    if value == "Double.NaN":
        return None
    number = float(value)
    assert math.isfinite(number), "unexpected nonfinite prototype value"
    return number

def vector(text):
    return [numeric(part) for part in text.split(",")]

def equal(x, y):
    if x is None or y is None:
        return x is None and y is None
    return math.isclose(x, y, abs_tol=0, rel_tol=1e-13)

def validate():
    original = JAVA.read_text(encoding="utf-8")
    found = REGEX.findall(original)
    assert len(found) == 24, "24 original V6 signatures required"
    ledger = json.loads(SOURCES.read_text(encoding="utf-8"))
    records = ledger["records"]
    assert len(records) == 24 and ledger["record_count"] == 24
    with INVENTORY.open(encoding="utf-8", newline="") as fh:
        inventory = list(csv.DictReader(fh))
    assert len(inventory) == 24

    counts = collections.Counter()
    spaces = collections.defaultdict(set)
    invalid_weights = 0
    prototype_rows = 0
    for index, fields in enumerate(found):
        signature_id, root, feature_space, label, legacy, ppm, nrows, values, scales = fields
        saved = records[index]
        scalar = vector(values)
        uncertainty = vector(scales)
        assert saved["id"] == signature_id and saved["source_root_id"] == root
        assert saved["feature_space"] == feature_space
        assert saved["source_label"] == label
        assert saved["legacy_reference_id"] == (int(legacy) if int(legacy) >= 0 else None)
        assert saved["source_declared_ppm"] == (numeric(ppm))
        assert saved["source_count"] == int(nrows)
        assert saved["vector_dimension"] == len(scalar)
        assert saved["available_coordinates"] == sum(x is not None for x in scalar)
        assert len(scalar) == len(uncertainty)
        assert len(saved["vector"]) == len(scalar)
        assert len(saved["source_scale_raw"]) == len(uncertainty)
        assert all(equal(x,y) for x,y in zip(scalar,saved["vector"]))
        assert all(equal(x,y) for x,y in zip(uncertainty,saved["source_scale_raw"]))
        usable_scale = all(x is not None and x > 0 for x in uncertainty)
        assert saved["source_scale_positive_everywhere"] is usable_scale
        assert saved["canonical_uid"] is None
        assert saved["eligible_for_direct_tank_candidate"] is False
        assert saved["qualification"] == "RESEARCH_PROTOTYPE_NO_G3"
        assert inventory[index]["id"] == signature_id
        assert inventory[index]["feature_space"] == feature_space
        assert inventory[index]["canonical_uid"] == ""
        assert inventory[index]["eligible_for_direct_tank_candidate"] == "false"
        counts[root] += 1
        spaces[feature_space].add(len(scalar))
        if not usable_scale:
            invalid_weights += 1
        prototype_rows += int(nrows)

    assert counts == {
        "MQDS_6400_SOURCE_ADC": 4,
        "ROBOFUSION_H1_OPS": 2,
        "UCI1081_MQ135_TEMPORAL": 18,
    }
    assert dict(spaces) == {
        "ADC_LOG_GEOMEAN_CLASS": {8},
        "OPERATIONAL_LOG1PLUS_CLR": {6},
        "MQ135_BASELINE_NORMALIZED_TEMPORAL": {3},
    }
    assert invalid_weights == 4, "MQDS aggregate scale has missing or nonpositive values"
    assert all(r["source_count"] == 5 for r in records if r["source_root_id"] == "UCI1081_MQ135_TEMPORAL")
    assert all(r["legacy_reference_id"] is None for r in records if r["source_root_id"] != "UCI1081_MQ135_TEMPORAL")
    assert all(r["source_declared_ppm"] is not None for r in records if r["source_root_id"] == "UCI1081_MQ135_TEMPORAL")
    assert all(r["source_declared_ppm"] is None for r in records if r["source_root_id"] != "UCI1081_MQ135_TEMPORAL")

    method_info = json.loads(METHODS.read_text(encoding="utf-8"))
    m = method_info["methods"]
    assert len(m) == 3
    assert {v["id"] for v in m} == {
        "MQDATA-3D-T-RH", "MQDATA-4D-TEMPORAL", "MQDATA-76-REGRESSION"
    }
    assert all(v["not_an_independent_exposure"] and not v["ready_for_next_code"] for v in m)
    assert all(len(v["sha"]) == 40 for v in m)
    assert method_info["g3_qualified"] == 0

    return {
        "schema": "chemcore.next.p03.3.fingerprint_source_validation/1",
        "status": "PASS_SOURCE_PRESERVATION_ONLY",
        "fingerprints": len(records),
        "fingerprint_root_counts": dict(counts),
        "space_dimensions": {k: next(iter(v)) for k, v in spaces.items()},
        "uncertainty_vectors_not_usable": invalid_weights,
        "source_rows_sum_not_independent_exposures": prototype_rows,
        "external_3d_4d_regression_method_sources": len(m),
        "canonical_chemicals_mapped": 0,
        "real_tank_exposure_validation_G3": 0,
        "limitations": [
            "No raw Tank replay, no held-out G2, no G3",
            "UCI1081 single-channel temporal features are not Tank multichannel vectors",
            "Missing and negative source scale cannot become valid variance",
            "All 3D/4D sources are research methods, not physical evidence",
        ]
    }

if __name__ == "__main__":
    a = argparse.ArgumentParser()
    a.add_argument("--out")
    args = a.parse_args()
    result = validate()
    if args.out:
        destination = pathlib.Path(args.out)
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print("CHEMCORE_NEXT_P03_3_FINGERPRINT_PASS "
          "count=%d spaces=%d invalid_source_uncertainty=%d source_methods=%d g3=0" %
          (result["fingerprints"], len(result["space_dimensions"]),
           result["uncertainty_vectors_not_usable"],
           result["external_3d_4d_regression_method_sources"]))
