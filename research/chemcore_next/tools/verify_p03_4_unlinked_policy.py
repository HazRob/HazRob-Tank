#!/usr/bin/env python3
"""Conservative NIOSH-CAS source identity *metadata* policy audit, not gas detection."""
import argparse
import collections
import csv
import json
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[3]
BASE = ROOT / "research/chemcore_next"
POLICY = BASE / "identity/chemcore_next_p03_4_unlinked_60_source_identity_policy.csv"
ORIGINAL = BASE / "source_curves/chemcore_next_p03_2_unified_109_formula_register.csv"
LINK = BASE / "identity/chemcore_next_p03_4_initial_historical_uid_crosswalk_109.csv"
TANK_SENSORS = {"MQ2", "MQ3", "MQ4", "MQ5", "MQ7", "MQ8", "MQ9", "MQ135"}
NIOSH_CAS_INDEX = "https://www.cdc.gov/niosh/npg/npgdcas.html"

def rows(path):
    with path.open(encoding="utf8",newline="") as f:
        return list(csv.DictReader(f))

def cas_valid(cas):
    if re.fullmatch(r"\d{2,7}-\d{2}-\d", cas) is None: return False
    digits=cas.replace("-","")
    return sum(i*int(x) for i,x in enumerate(reversed(digits[:-1]),1))%10 == int(digits[-1])

def verify():
    policy=rows(POLICY)
    original=rows(ORIGINAL)
    links=rows(LINK)
    assert len(policy)==60 and len(original)==len(links)==109
    by_source={r["source_curve_uid"]:r for r in original}
    by_link={r["source_curve_uid"]:r for r in links}
    assert len(by_source)==len(by_link)==109
    assert sum(not r["verified_master_uid"] for r in links)==60
    uid=set()
    states=collections.Counter()
    installed=collections.Counter()
    labels=collections.Counter()
    for r in policy:
        source_uid=r["source_curve_uid"]
        assert source_uid not in uid
        uid.add(source_uid)
        src=by_source[source_uid]
        unresolved=by_link[source_uid]
        assert unresolved["verified_master_uid"]==""
        assert unresolved["source_sensor"]==r["sensor_model"]==src["sensor_model"]
        assert unresolved["source_label"]==r["source_label"]==src["source_gas_or_class_label"]
        assert r["source_root_id"]==unresolved["source_root_id"]==src["source_root_id"]
        assert r["source_blob_sha"]==unresolved["source_blob_sha"]==src["source_provenance_blob"]
        assert not src["v6_reused_curve_id"], "new record must be source not V6 link"
        status=r["chemical_identity_policy"]
        states[status]+=1
        labels[r["source_label"]]+=1
        if r["sensor_model"] in TANK_SENSORS:installed[status]+=1
        if status=="CAS_SPECIES_EXACT_SOURCE_LABEL":
            assert cas_valid(r["expected_cas"]), r
            assert r["expected_chemical_name"]
            if r["source_label"]=="Ethanol":
                assert r["expected_cas"]=="64-17-5"
                assert r["expected_chemical_name"]=="Ethyl alcohol"
                assert r["authority_url"]=="https://www.cdc.gov/niosh/npg/npgd0262.html"
            else:
                assert r["authority_url"]==NIOSH_CAS_INDEX
        elif status=="CLASS_MIXTURE_OR_NON_SPECIFIC_NOT_MOLECULE":
            assert r["source_label"].lower() in {"lpg","smoke","alcohol","nox"}
            assert not r["expected_cas"] and not r["authority_url"]
        else:
            assert status=="NH4_ION_SOURCE_LABEL_UNVERIFIED"
            assert r["source_label"] in {"NH4","NH4_UNRESOLVED_CLASS"}
            assert not r["expected_cas"] and not r["authority_url"]
    assert states=={
        "CAS_SPECIES_EXACT_SOURCE_LABEL":44,
        "CLASS_MIXTURE_OR_NON_SPECIFIC_NOT_MOLECULE":14,
        "NH4_ION_SOURCE_LABEL_UNVERIFIED":2,
    }
    assert sum(installed.values())==13
    assert installed["CAS_SPECIES_EXACT_SOURCE_LABEL"]==7
    assert installed["CLASS_MIXTURE_OR_NON_SPECIFIC_NOT_MOLECULE"]==5
    assert installed["NH4_ION_SOURCE_LABEL_UNVERIFIED"]==1
    assert len(labels)==25
    return {
        "schema":"chemcore.next.p03.4.cas_alias_policy_source_check/1",
        "status":"SOURCE_POLICY_AND_CAS_CHECK_DIGITS_PASS",
        "rows":60,
        "unique_source_labels":len(labels),
        "policy_statuses":dict(states),
        "installed_sensor_extra_rows":dict(installed),
        "checksum_verified_cas_rows":44,
        "physical_gas_identifications":0,
        "chemcore_next_curve_activations":0,
        "next":"Run physical 734 SQLite exact CAS/name resolution and retain ambiguous/missing as abstentions",
    }

if __name__=="__main__":
    parser=argparse.ArgumentParser()
    parser.add_argument("--out")
    opt=parser.parse_args()
    report=verify()
    if opt.out:
        target=pathlib.Path(opt.out)
        target.parent.mkdir(parents=True,exist_ok=True)
        target.write_text(json.dumps(report,indent=2)+"\n",encoding="utf8")
    print("CHEMCORE_NEXT_P03_4_SOURCE_CAS_POLICY_PASS "
          "rows=60 exact_source_species=44 generic=14 nh4=2 installed=13 "
          "cas_checksum_pass=44 source_identity_approved=false")
