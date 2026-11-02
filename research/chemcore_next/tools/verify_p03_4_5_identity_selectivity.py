#!/usr/bin/env python3
"""ChemCore Next P03.4-5 authoritative source crosswalk and interference coverage.

Validates original 109 formula sources + SQLite-proven 49 legacy links,
44 direct CAS/name, 16 existing/general class UIDs. Builds NO physical
selectivity or detection limit; zero-valued source coverage never means
negative sensor response.
"""
import argparse
import collections
import csv
import json
import pathlib

ROOT = pathlib.Path(__file__).resolve().parents[3]
NEXT = ROOT / "research/chemcore_next"
SOURCE = NEXT / "source_curves/chemcore_next_p03_2_unified_109_formula_register.csv"
MAPPING = NEXT / "identity/chemcore_next_p03_4_verified_full_109_source_uid_crosswalk.csv"
MATRIX = NEXT / "selectivity/chemcore_next_p03_5_8sensor_21target_source_coverage_matrix.csv"
OVERLAP = NEXT / "selectivity/chemcore_next_p03_5_common_sensor_target_overlap_123.csv"
MODELS = ("MQ2", "MQ3", "MQ4", "MQ5", "MQ7", "MQ8", "MQ9", "MQ135")
CLASS_UIDS = {
    136: "LPG",
    140: "Smoke",
    142: "NOx",
    735: "Alcohols (general)",
    736: "Ammonium-containing materials (NH4+ source class)",
}
CLASS_NO_CAS = set(CLASS_UIDS)

def read(path):
    with path.open(encoding="utf8", newline="") as stream:
        return list(csv.DictReader(stream))

def verify():
    orig, links, matrix, overlapping = [read(p) for p in
        (SOURCE, MAPPING, MATRIX, OVERLAP)]
    assert len(orig) == len(links) == 109
    assert len(matrix) == 8 * 21 == 168
    assert len(overlapping) == 123
    original = {x["source_curve_uid"]: x for x in orig}
    mapped = {x["source_curve_uid"]: x for x in links}
    assert len(original) == len(mapped) == 109
    counts = collections.Counter()
    installed = collections.Counter()
    bypair = collections.defaultdict(list)
    identities = {}
    historical = 0
    for uid, r in mapped.items():
        o = original[uid]
        assert r["sensor_model"] == o["sensor_model"]
        assert r["source_original_label"] == o["source_gas_or_class_label"]
        assert r["evidence_source_root"] == o["source_root_id"]
        assert r["source_blob_sha"] == o["source_provenance_blob"]
        assert r["a"] == o["coefficient_a"] and r["b"] == o["coefficient_b"]
        assert r["installed_on_current_Tank"] == o["physical_sensor_present_on_tank"]
        assert r["source_original_scale_ppm"] == o["source_scale_ppm_known"]
        assert r["v6_legacy_scale_if_any"] == o["v6_assumed_scale"]
        assert r["source_reused_from_v6"] == o["v6_reused_curve_id"]
        assert 1 <= int(r["canonical_uid"]) <= 736
        assert r["source_is_independent_exposure"] == "NO"
        for k in ("effective_sensor_specific_ppm_model_qualified",
                  "tank_ppm_reporting_authorized",
                  "source_qualified_heater_phase", "safety_g3_validated"):
            assert r[k] == "NO"
        assert not r["LOD_ppm"] and not r["quantified_source_sensor_device_R0_kohm"]
        if r["canonical_cas"]:
            assert int(r["canonical_uid"]) not in CLASS_NO_CAS
        if int(r["canonical_uid"]) in CLASS_NO_CAS:
            assert r["canonical_name"] == CLASS_UIDS[int(r["canonical_uid"])]
            assert not r["canonical_cas"]
        kind = r["catalog_link_type"]
        counts[kind] += 1
        if kind == "LEGACY_UID_LOOKUP_SOURCE_ONLY":
            historical += 1
            assert r["legacy_reference_id"] and r["source_reused_from_v6"]
        else:
            assert not r["legacy_reference_id"]
            assert not r["source_reused_from_v6"]
        if kind == "GENERAL_CLASS_GHS_RESEARCH_UID_NOT_MOLECULAR":
            assert int(r["canonical_uid"]) in CLASS_NO_CAS
        if kind == "STRICT_CAS_AND_NAME_SOURCE_TARGET_LOOKUP":
            assert r["canonical_cas"], "CAS-resolved direct reference needed"
        identity = int(r["canonical_uid"])
        if identity in identities:
            assert identities[identity] == (r["canonical_name"], r["canonical_cas"])
        identities[identity] = (r["canonical_name"], r["canonical_cas"])
        if r["installed_on_current_Tank"] == "YES":
            assert r["sensor_model"] in MODELS
            bypair[r["sensor_model"], str(identity)].append(r)
            installed[r["sensor_model"]] += 1
    assert counts == {
        "LEGACY_UID_LOOKUP_SOURCE_ONLY": 49,
        "STRICT_CAS_AND_NAME_SOURCE_TARGET_LOOKUP": 44,
        "GENERAL_CLASS_GHS_RESEARCH_UID_NOT_MOLECULAR": 16,
    }, counts
    assert historical == 49 and sum(installed.values()) == 62
    assert len(identities) == 21
    assert installed == {
        "MQ2":8,"MQ3":7,"MQ4":7,"MQ5":6,
        "MQ7":6,"MQ8":6,"MQ9":4,"MQ135":18,
    }
    assert len({k for k in bypair}) == 47
    assert sum(1 for x in links if x["installed_on_current_Tank"] == "NO") == 47

    unique_pairs = set()
    for r in matrix:
        key = r["sensor_model"],r["target_master_uid"]
        assert r["sensor_model"] in MODELS
        assert int(r["target_master_uid"]) in identities
        assert key not in unique_pairs
        unique_pairs.add(key)
        entries = bypair.get(key, [])
        assert int(r["source_curves"]) == len(entries)
        assert int(r["source_formula_count"]) == len(entries)
        assert (r["target_name"],r["target_cas"]) == identities[int(r["target_master_uid"])]
        assert r["source_curve_ids"] == ";".join(x["source_curve_uid"] for x in entries)
        roots = {x["evidence_source_root"] for x in entries}
        assert int(r["independent_source_root_count_observed"]) == len(roots)
        for field in (
            "numeric_discrimination_established", "qualified_device_R0",
            "physical_Tank_G3", "sensor_output_may_be_zero_if_no_curve",
            "source_gas_identity_is_confirmed", "rankable_candidate_without_test",
        ):
            assert r[field] == "NO", (key,field)
        assert not r["measured_LOD_ppm"] and not r["LOQ_ppm"]
        if entries:
            assert r["source_evidence_status"] == "SOURCE_FORMULA_ONLY_NO_MEASURED_PERFORMANCE"
            assert r["cross_sensitivity_interference_status"] == "POTENTIAL_CROSS_SENSITIVITY_MUST_TEST"
        else:
            assert r["source_evidence_status"] == "NO_SOURCE_CURVE_NOT_NEGATIVE_EVIDENCE"
            assert r["cross_sensitivity_interference_status"] == "UNKNOWN_NO_NEGATIVE_EVIDENCE"
    assert len(unique_pairs) == 168
    assert sum(int(r["source_curves"]) for r in matrix) == 62
    assert sum(int(r["source_curves"]) > 0 for r in matrix) == 47
    assert sum(int(r["source_curves"]) == 0 for r in matrix) == 121

    shared_count = collections.Counter()
    pairs = set()
    for edge in overlapping:
        model = edge["sensor_model"]
        a, b = edge["source_target_uid_a"],edge["source_target_uid_b"]
        assert model in MODELS and int(a) < int(b)
        assert (model,a) in bypair and (model,b) in bypair
        assert edge["source_target_a"] == identities[int(a)][0]
        assert edge["source_target_b"] == identities[int(b)][0]
        assert int(edge["number_source_curves_for_a"]) == len(bypair[(model,a)])
        assert int(edge["number_source_curves_for_b"]) == len(bypair[(model,b)])
        assert edge["independently_measured_mixture_interference"] == "NO"
        assert edge["source_curve_coefficients_comparable"] == "NO"
        assert edge["physical_Tank_qualified"] == "NO"
        assert not edge["measured_selectivity_factor"]
        assert (model,a,b) not in pairs
        pairs.add((model,a,b))
        shared_count[model] += 1
    target_counts = collections.Counter(model for model, uid in bypair)
    for model in MODELS:
        n = target_counts[model]
        assert shared_count[model] == n*(n-1)//2
    assert len(pairs) == 123
    # Explicitly keep the original NH4 label versus historical ammonia
    # reference as two competing, unvalidated chemical interpretations.
    ammonia = mapped["MQDS-SRC-053"]
    assert ammonia["source_original_label"] == "NH4"
    assert ammonia["canonical_name"] == "Ammonia"
    assert ammonia["catalog_link_type"] == "LEGACY_UID_LOOKUP_SOURCE_ONLY"
    assert ammonia["source_identity_uncertainty"] == "AMBIGUOUS_CLASS_OR_ALIAS_NOT_UNIQUE_MOLECULE"

    return {
        "schema": "chemcore.next.p03.4_5.source_identity_selectivity/1",
        "status": "PASS_SOURCE_CATALOG_LINKS_AND_CONSERVATIVE_SELECTIVITY_MATRIX",
        "source_curves_total":109,
        "source_mapping_types":dict(counts),
        "source_target_catalog_unique_uids":len(identities),
        "historical_catalog_before_group_addition":734,
        "canonical_catalog_after_group_addition":736,
        "current_sensors":len(MODELS),
        "current_sensor_formula_links":sum(installed.values()),
        "future_sensor_formula_links":47,
        "current_sensor_target_pairs_with_sources":len(bypair),
        "current_sensor_target_pairs_no_curve":121,
        "current_sensor_total_coverage_pairs":168,
        "possible_same_sensor_overlap_target_pairs":len(pairs),
        "by_sensor_formula_count":dict(installed),
        "source_curve_LOD_ppm_qualified":0,
        "independent_interferent_experiments":0,
        "independent_real_Tank_G3_experiments":0,
        "chemcore_next_runtime_gas_candidates_activated":0,
        "scope": "Only research curves/model target coverage. No negative evidence, selectivity coefficient or actual LOD.",
    }

if __name__ == "__main__":
    parser=argparse.ArgumentParser()
    parser.add_argument("--out")
    args=parser.parse_args()
    report=verify()
    if args.out:
        out=pathlib.Path(args.out)
        out.parent.mkdir(parents=True,exist_ok=True)
        out.write_text(json.dumps(report,indent=2)+"\n",encoding="utf8")
    print(
        "CHEMCORE_NEXT_P03_4_5_SELECTIVITY_PASS source_formulas=109 "
        "master_uids=21 catalog=736 installed_curves=62 "
        "source_target_pairs=47 unsupported_pairs=121 overlap_pairs=123 "
        "LOD_qualified=0 Tank_G3=0")
