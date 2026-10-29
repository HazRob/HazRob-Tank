#!/usr/bin/env python3
"""Re-audit source-pinned historic G2 *reports*, not their large raw archives.

Historical GitHub Actions already processed real external UCI sources. V6 uses
their provenance as a reproducible benchmark registry, without claiming that
these reports independently re-execute UCI270/308/309 or validate Tank G3.
"""
import argparse
import json
from pathlib import Path
from source_audit import EvidenceError, git_blob_sha1

ROOT=Path(__file__).resolve().parent/"source_snapshots"/"g2_research"
PINS={
"uci270_g2_batch04_2026-10-19.json":"88d13ae28468472631085f1136147cdbae0f3d8a",
"uci308_g2_batch05_2026-10-20.json":"f3b8e9baa3b098c0eb969d52c5e9e95a06ccaf49",
"uci309_g2_batch06_2026-10-20.json":"94ac012cfdbc149077472819d1cb25ca4df31134",
"uci_trained_models_main_2026-10-21.json":"94aa5749add50884bd9658f2274377d1f8d46514",
}
EXPECTED_ORIGINAL_ARCHIVES={
"uci270":"98fe3a30981a222dd4518fbcc3dddd45d5c0ce9b03ef6dc6fe5cf7a04cfbff5e",
"uci308":"7b062960dbef1a9e8aefc62bc7dbac09bdadb8cbb40793cf2b8e122da1862f90",
"uci309":"5e9b707e7a44b3dcaf39b62bb46597403fcdd98ec89814b15e4876694db5f41e",
}
UCI1081_SOURCE_ARCHIVE="813ebb7d00ed9172114b68d27ab3213170d6c220b8b23907dce91e9838cd48ab"

def load_reports(root=ROOT):
    if set(f.name for f in root.iterdir() if f.is_file()) != set(PINS):
        raise EvidenceError("G2 result inventory differs from four pinned report files")
    reports={}
    for name,digest in PINS.items():
        b=(root/name).read_bytes()
        if git_blob_sha1(b)!=digest:
            raise EvidenceError("HISTORIC_G2_REPORT_SHA_MISMATCH:"+name)
        reports[name]=json.loads(b)
    return reports

def report(root=ROOT):
    r=load_reports(root)
    drift=r["uci270_g2_batch04_2026-10-19.json"]
    flow=r["uci308_g2_batch05_2026-10-20.json"]
    mix=r["uci309_g2_batch06_2026-10-20.json"]
    trained=r["uci_trained_models_main_2026-10-21.json"]
    if (drift["source_records"]!=13910 or drift["source_features_per_record"]!=128
        or drift["unique_source_batches"]!=10
        or sum(drift["batch_counts"])!=13910
        or drift["duplicate_with_UCI224"] is not True):
        raise EvidenceError("UCI270 count, batches or duplicate lineage changed")
    if (flow["original_exposures"]!=58 or flow["original_sensors"]!=16
        or flow["feature_rows"]!=58 or flow["verified_tank_calibration"] is not False):
        raise EvidenceError("UCI308 source exposure/unit contract mismatch")
    if (mix["source_experiment_count"]!=180 or mix["unique_mixture_configurations"]!=30
        or mix["repetitions_per_configuration"]!=6
        or mix["scientific_Tank_G3_validated"] is not False):
        raise EvidenceError("UCI309 source grouped exposure contract mismatch")
    if trained["research_only"] is not True or trained["tank_g3_validated"] is not False:
        raise EvidenceError("trained source evidence incorrectly promoted")
    archive_hashes={
        "uci270":drift["source_archive_sha256"],
        "uci308":flow["source_zip_sha256"],
        "uci309":mix["original_archive_sha256"],
    }
    if archive_hashes!=EXPECTED_ORIGINAL_ARCHIVES:
        raise EvidenceError("historic UCI original archive pins changed")
    return {
      "schema":"hazrob-tank-v6-g2-portfolio-audit/1",
      "record_type":"VERIFIED_HISTORY_REPORTS_NOT_NEW_RAW_RUNS",
      "original_report_git_blobs_verified":len(PINS),
      "distinct_external_exposure_counts":{"uci1081":90,"uci270":13910,"uci308":58,"uci309":180},
      "total_distinct_external_source_exposures":14238,
      "uci270_UCI224_same_dataset_mirror":True,
      "source_sensor_platforms_not_Tank_MQ_array":True,
      "flow_modulation_liquid_preparation_is_not_gas_phase_ppm":True,
      "uci309_mean_GCMS_ppm_is_not_per_timestamp_ppm":True,
      "unverified_raw_source_bytes_in_current_V6_task":["uci270","uci308","uci309"],
      "historical_original_archive_pins":archive_hashes,
      "historical_training_model_results_not_new_experiments":True,
      "Tank_G3_measurement_promotions":0,
      "M4_quantitative_claim":False,
    }

if __name__=="__main__":
    parser=argparse.ArgumentParser()
    parser.add_argument("--out",type=Path)
    args=parser.parse_args()
    result=report()
    if args.out:
        args.out.parent.mkdir(parents=True,exist_ok=True)
        args.out.write_text(json.dumps(result,indent=2,sort_keys=True)+"\n")
    print(json.dumps(result,indent=2,sort_keys=True))
