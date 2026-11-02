#!/usr/bin/env python3
"""P03.2 source-replay / attribution checks; research-only and no physical ppm/G3."""
import argparse
import collections
import csv
import json
import math
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[3]
DATA = ROOT / "research/chemcore_next/source_curves"
V6 = ROOT / "android/app/src/main/java/com/hazrob/tank/chemcore/v6"
MQDATA = ROOT / "research/chemcore_next/mqdata_v7_source_gas_curve_register_89.csv"
BASIC = DATA / "chemcore_next_p03_2_source_curve_ledger_95.csv"
EXAMPLES = DATA / "chemcore_next_p03_2_mqsensorslib_examples_14.csv"
UNIFIED = DATA / "chemcore_next_p03_2_unified_109_formula_register.csv"
COLUMNS = ("sensor_model", "source_gas_or_class_label", "source_curve_uid")
TANK = {"MQ2", "MQ3", "MQ4", "MQ5", "MQ7", "MQ8", "MQ9", "MQ135"}
CODE_RE = re.compile(
    r'new (?:SourceCatalog\.)?Curve\(\s*(\d+),\s*"([^"]+)",\s*(\d+)L,'
    r'\s*"([^"]+)",\s*([-\d.]+),\s*([-\d.]+),\s*([-\d.]+),\s*([-\d.]+),'
    r'\s*([-\d.]+)\s*\)'
)

def load_csv(path):
    with path.open(encoding="utf-8", newline="") as f:
        return list(csv.DictReader(f))

def assert_close(observed, expected, context):
    assert math.isfinite(observed) and math.isfinite(expected), context
    assert abs(observed - expected) <= 1e-9 * max(1.0, abs(expected)), (context, observed, expected)

def extract_java(path):
    result = []
    for g in CODE_RE.findall(path.read_text(encoding="utf-8")):
        (num, sensor, legacy, name, a, b, low, high, scale) = g
        result.append(dict(
            id=int(num), sensor=sensor, legacy=int(legacy), name=name,
            a=float(a), b=float(b), low=float(low),
            high=float(high), scale=float(scale)))
    return result

def verify():
    original = load_csv(MQDATA)
    reused = load_csv(BASIC)
    examples = load_csv(EXAMPLES)
    all_formulas = load_csv(UNIFIED)
    primary = extract_java(V6 / "SourceCatalog.java")
    alternate = extract_java(V6 / "AlternativeCurveCatalog.java")

    assert len(original) == 89, "MQDataScience 89 original source rows"
    assert len(primary) == 43 and len(alternate) == 6, "V6 43 + 6 curves"
    assert len(reused) == 95, "89 original + 6 alternative curves"
    assert len(examples) == 14, "eight MQ-Board and six MQ135 examples"
    assert len(all_formulas) == 109, "95 + 14 distinct numeric formula entries"
    assert len({r["source_curve_uid"] for r in all_formulas}) == 109, "unique curve identities"

    for i, row in enumerate(original):
        entry = reused[i]
        assert entry["curve_uid"] == "MQDS-SRC-%03d" % (i + 1)
        assert row["sensor"] == entry["sensor_model"]
        assert row["source_gas_or_class"] == entry["gas_label_original"]
        assert_close(float(row["source_a"]), float(entry["coefficient_a"]), "MQDS coefficient A")
        assert_close(float(row["source_b"]), float(entry["coefficient_b"]), "MQDS coefficient B")
        assert entry["source_blob_sha"] == row["source_blob_sha"]
        assert entry["source_line"] == row["source_line"]
        assert entry["source_ppm_min"] == row["source_specific_ppm_min_if_specified"]
        assert entry["source_ppm_max"] == row["source_specific_ppm_max_if_specified"]
        assert entry["source_ppm_scale"] == "", "unknown source scale cannot be made up"

    matched = set()
    for v in primary:
        rows = [r for r in reused[:89] if
                r["sensor_model"] == v["sensor"]
                and float(r["coefficient_a"]) == v["a"]
                and float(r["coefficient_b"]) == v["b"]]
        assert len(rows) == 1, "V6 curve must match exactly ONE MQDS source: %s" % v
        r = rows[0]
        matched.add(r["curve_uid"])
        assert r["v6_curve_id"] == "MQDATA43-%03d" % v["id"]
        assert int(r["v6_legacy_reference_id"]) == v["legacy"]
        assert_close(float(r["v6_engine_scale_assumed"]), v["scale"], "V6-only scale")
        assert_close(float(r["v6_engine_domain_min_assumed"]), v["low"], "V6 domain low")
        assert_close(float(r["v6_engine_domain_max_assumed"]), v["high"], "V6 domain high")
    assert len(matched) == 43
    assert all(r["source_duplicate_projections"] == "MQDATASCIENCE_ORIGINAL_AND_V6_REUSE"
               for r in reused[:89] if r["curve_uid"] in matched)
    assert sum(r["tank_installed_model"] == "true" for r in reused) == 49
    assert sum(r["tank_installed_model"] == "false" for r in reused) == 46

    for idx, v in enumerate(alternate):
        r = reused[89 + idx]
        assert r["curve_uid"] == "MQ135ALT6-%02d" % (v["id"] - 43)
        assert r["source_root_id"] == "UMBU_MQ135_2023_ALTERNATIVE_DIGITIZED"
        assert r["sensor_model"] == v["sensor"]
        assert r["gas_label_original"] == v["name"]
        assert_close(float(r["coefficient_a"]), v["a"], "alternative A")
        assert_close(float(r["coefficient_b"]), v["b"], "alternative B")
        assert r["canonical_chemical_uid"] == "", "source labels remain unresolved"
    assert set(r["curve_uid"] for r in all_formulas) == (
        {r["curve_uid"] for r in reused} |
        {r["curve_uid"] for r in examples}
    ), "unified records exactly union of verified primary and example layers"
    assert all(not (r["canonical_chemical_uid"] or r["candidate_eligibility"] != "NO" or
                    r["chemcore_next_model_activated"] != "NO")
               for r in all_formulas), "no false canonical chemicals or activation"
    assert all(r["manufacturer_root_proven_independent"] == "NO"
               and r["scientific_r0_reference_verified"] == "NO"
               and r["heater_phase_verified"] == "NO" for r in all_formulas)

    grouped = collections.Counter(r["source_root_id"] for r in all_formulas)
    assert grouped["MQDATASCIENCE_ORIGINAL_GAS_SENSOR_CURVE_FAMILY"] == 89
    assert grouped["UMBU_MQ135_2023_ALTERNATIVE_DIGITIZED"] == 6
    assert grouped["MQSL_MQ135_EXAMPLE_SAME_AS_ADS1115_EXAMPLE"] == 6
    assert grouped["MQSL_MQ_BOARD_8_MODEL_EXAMPLE"] == 8
    assert sum(r["physical_sensor_present_on_tank"] == "YES" for r in all_formulas) == 62
    assert sum(r["physical_sensor_present_on_tank"] == "NO" for r in all_formulas) == 47

    for row in examples:
        assert row["original_equation"] == "PPM_EQUALS_A_TIMES_RATIO_POWER_B"
        assert float(row["coefficient_a"]) > 0 and float(row["coefficient_b"]) != 0
        assert row["physical_r0_reference_qualified"] == "NO"
        assert row["canonical_chemical_uid"] == ""
        assert row["activation"] == "NO"
        assert row["qualification"] == "RESEARCH_EXAMPLE_ONLY"
        if row["sensor_model"] == "MQ135":
            assert row["numeric_duplicate_of_ads1115_example"] == "YES"
        if row["source_root_id"] == "MQSL_MQ_BOARD_8_MODEL_EXAMPLE":
            assert row["known_example_issue"] == "UPSTREAM_CALIBRATION_USES_MQ2_OBJECT_FOR_OTHERS"
    assert sum(r["sensor_model"] == "MQ135" for r in examples) == 6

    numeric_probes = 0
    for r in reused:
        a = float(r["coefficient_a"])
        b = float(r["coefficient_b"])
        assert a > 0 and b != 0
        # These synthetic algebraic probes never establish a physically valid ppm domain.
        scale = float(r["v6_engine_scale_assumed"]) if r["v6_engine_scale_assumed"] else 1.
        for ppm in (1., 10., 100., 1000.):
            ratio = a * (ppm / scale) ** b
            recovered = scale * (ratio / a) ** (1. / b)
            assert_close(recovered, ppm, "source inverse with explicitly selected scale")
            numeric_probes += 1

    for r in examples:
        a = float(r["coefficient_a"])
        b = float(r["coefficient_b"])
        for ratio in (0.1, 0.2, 0.5, 1.0, 2.0, 10.0):
            predicted = a * ratio ** b
            inverted_ratio = (predicted / a) ** (1. / b)
            assert_close(inverted_ratio, ratio, "MQSensorsLib example ppm power-law inversion")
            numeric_probes += 1

    # No R0 from a datasheet, example or a second repository is treated as measured Tank R0.
    anomalies = {
        "NH4_NH3_label_is_not_verified_identity": any(
            "NH4/NH3" in r["source_gas_or_class_label"] for r in all_formulas),
        "MQ135_MQSL_NH4_ambiguous": any(
            "NH4_UNRESOLVED_CLASS" == r["source_gas_or_class_label"] for r in all_formulas),
        "source_scale_unconfirmed": sum(not r["source_scale_ppm_known"] for r in all_formulas),
        "manufacturer_independence_not_verified": True,
    }
    summary = {
        "schema": "hazrob.chemcore.next.p03.2.provenance_ci/1",
        "status": "PASS_SOURCE_RECONCILIATION_AND_MATH_ONLY",
        "source_original_mqdata": len(original),
        "source_v6_same_root_reuse": len(primary),
        "source_v6_mq135_alternate": len(alternate),
        "mqsensorslib_examples_distinct_formulas": len(examples),
        "all_unique_source_formula_records": len(all_formulas),
        "tank_source_formula_records": 62,
        "future_sensor_formula_records": 47,
        "proven_independent_physical_exposure_count": 0,
        "canonical_chemical_mapping_count": 0,
        "chemcore_next_activated_curves": 0,
        "g3_qualified_curves": 0,
        "source_groups": dict(grouped),
        "numeric_inverse_probes": numeric_probes,
        "source_anomalies": anomalies,
        "test_scope": "source/arithmetical provenance; no gas detection, hardware R0 or G2/G3",
    }
    return summary

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", default=None)
    opt = parser.parse_args()
    summary = verify()
    if opt.out:
        path = pathlib.Path(opt.out)
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("CHEMCORE_NEXT_P03_2_SOURCE_CURVES_PASS "
          "rows=%d reused_v6=%d mqsensorslib=%d tested_inverse=%d "
          "g3=%d mapped_uid=%d" %
          (summary["all_unique_source_formula_records"],
           summary["source_v6_same_root_reuse"],
           summary["mqsensorslib_examples_distinct_formulas"],
           summary["numeric_inverse_probes"],
           summary["g3_qualified_curves"],
           summary["canonical_chemical_mapping_count"]))
