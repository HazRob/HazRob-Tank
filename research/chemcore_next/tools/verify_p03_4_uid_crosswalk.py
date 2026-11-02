#!/usr/bin/env python3
"""P03.4 verify a historical SQLite source-UID crosswalk, not gas identity."""
import argparse
import csv
import json
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[3]
BASE = ROOT / "research/chemcore_next/identity"
UNIFIED = ROOT / "research/chemcore_next/source_curves/chemcore_next_p03_2_unified_109_formula_register.csv"
HIST = BASE / "chemcore_next_p03_4_sqlite_legacy_source_49.csv"
LOOKUP = BASE / "chemcore_next_p03_4_initial_historical_uid_crosswalk_109.csv"
AMBIGUOUS = re.compile(
    r"^(?:lpg|smoke|alcohol|nh4|nh4/nh3|ethanol/alcohol|nh4_unresolved_class)$", re.I)

def load(path):
    with path.open(encoding="utf8", newline="") as f:
        return list(csv.DictReader(f))

def audit():
    original, historical, mapped = load(UNIFIED), load(HIST), load(LOOKUP)
    assert len(original) == len(mapped) == 109
    assert len(historical) == 49
    by_original = {r["source_curve_uid"]: r for r in original}
    by_hist = {r["v6_curve_id"]: r for r in historical}
    assert len(by_original) == 109 and len(by_hist) == 49
    seen = set()
    linked = 0
    unlinked = 0
    ambiguous = 0
    canonical = set()
    for row in mapped:
        uid = row["source_curve_uid"]
        assert uid not in seen, "source record duplicated"
        seen.add(uid)
        orig = by_original[uid]
        assert row["source_sensor"] == orig["sensor_model"]
        assert row["source_label"] == orig["source_gas_or_class_label"]
        assert row["original_v6_curve_id"] == orig["v6_reused_curve_id"]
        assert row["v6_legacy_reference_index"] == orig["v6_legacy_index_not_canonical_uid"]
        assert row["candidate_eligibility"] == "NO"
        assert row["source_independence"] == "NOT_PROVEN_INDEPENDENT_EXPOSURE"
        assert row["source_root_id"] == orig["source_root_id"]
        assert row["source_blob_sha"] == orig["source_provenance_blob"]
        label_is_ambiguous = AMBIGUOUS.fullmatch(row["source_label"]) is not None
        if row["original_v6_curve_id"]:
            linked += 1
            hist = by_hist[row["original_v6_curve_id"]]
            assert row["historical_reference_state"] == "SQLITE_LEGACY_TO_MASTER_UID_READBACK_VERIFIED"
            assert row["verified_master_uid"] == hist["sqlite_master_uid"]
            assert row["verified_master_name"] == hist["master_name"]
            assert row["verified_master_cas"] == hist["master_cas"]
            if row["source_label"] != hist["source_label"]:
                assert row["original_v6_curve_id"] == "MQDATA43-042", "unexpected source label drift"
                assert row["source_label"] == "NH4" and hist["source_label"] == "NH4/NH3"
                assert row["chemical_identity_state"] == "AMBIGUOUS_CLASS_OR_ALIAS_NOT_UNIQUE_MOLECULE"
                assert "UPSTREAM_LABEL_CONFLICT" in row["investigation_note"]
            assert row["source_sensor"] == hist["sensor"]
            assert row["v6_legacy_reference_index"] == hist["legacy_reference_id"]
            assert 0 <= int(row["v6_legacy_reference_index"]) < 144
            assert 0 < int(row["verified_master_uid"]) <= 734
            canonical.add(int(row["verified_master_uid"]))
            if label_is_ambiguous:
                ambiguous += 1
                assert row["chemical_identity_state"] == "AMBIGUOUS_CLASS_OR_ALIAS_NOT_UNIQUE_MOLECULE"
            elif not row["verified_master_cas"]:
                assert row["chemical_identity_state"] == "HISTORICAL_LABEL_WITHOUT_UNIQUE_CAS"
            else:
                assert row["chemical_identity_state"] == "HISTORICAL_UID_WITH_CAS_RESEARCH_ONLY"
        else:
            unlinked += 1
            assert row["verified_master_uid"] == ""
            assert row["verified_master_cas"] == ""
            assert row["verified_master_name"] == ""
            assert row["historical_reference_state"] == "NOT_MAPPED_BY_UNAMBIGUOUS_SOURCE"
            assert row["chemical_identity_state"] == "NOT_RESOLVED_SOURCE_ALIAS"
    assert len(seen) == 109
    assert linked == 49 and unlinked == 60, (linked, unlinked)
    assert len(canonical) == 13
    assert ambiguous == 18, "Source LPG/smoke/alcohol/NH4-NH3 deliberately flagged"
    return {
        "schema": "chemcore.next.p03.4.historical_uid_link_ci/1",
        "status": "PASS_49_VERIFIED_LEGACY_LINKS_60_CONSERVATIVELY_UNRESOLVED",
        "unified_formula_source_rows": 109,
        "historically_linked_curves": linked,
        "unresolved_formula_rows": unlinked,
        "distinct_sqlite_master_uids": len(canonical),
        "ambiguous_historical_class_alias_curves": ambiguous,
        "eligible_gas_identity_approvals": 0,
        "qualified_tank_g3": 0,
        "catalog_master_uids": 734,
        "real_database_ci_evidence": "https://github.com/HazRob/HazRob-Tank/actions/runs/41378951072",
        "scope": "Only historical source-reference index resolution through actual seeded 734-row SQLite; no assumed exact matches for MQSensorsLib or future-sensor source rows.",
    }

if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--out")
    args = ap.parse_args()
    result = audit()
    if args.out:
        p = pathlib.Path(args.out)
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print("CHEMCORE_NEXT_P03_4_UID_CROSSWALK_PASS "
          "source_rows=109 mapped=%d unresolved=%d distinct_uids=%d "
          "ambiguous=%d gas_certifications=0" %
          (result["historically_linked_curves"],
           result["unresolved_formula_rows"],
           result["distinct_sqlite_master_uids"],
           result["ambiguous_historical_class_alias_curves"]))
