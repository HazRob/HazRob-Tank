"""V4 06.5 genuine UCI1081 MQ135-only known-vs-unseen-source gas stress test.

UCI1081 90 physically measured lab exposures; one source MQ135 channel only,
not Tank MQ8 and not a source-verified independent session/device holdout.
Source file is fetched by CI, SHA256 checked by canonical archive parser.
This benchmark does NOT calibrate gas identity, concentration or safety.
"""
import argparse
import json
from pathlib import Path
import importlib.util

from uci1081_actual_modules_v1 import (
    parse_archive,features,GASES,SENSORS,SOURCE_SHA256
)

ROOT=Path(__file__).resolve().parents[1]
def load_open_set():
    path=ROOT/"techniques/06.5/open_set_group_guard_v2.py"
    spec=importlib.util.spec_from_file_location("v4_real_mq135_open_set",path)
    m=importlib.util.module_from_spec(spec)
    import sys
    sys.path.insert(0,str(path.parent))
    spec.loader.exec_module(m)
    return m.evaluate_heldout_unknowns

def make_rows(raw):
    index=SENSORS.index("MQ135")
    out=[]
    for row in raw:
        signal=features(row["trace"],"early_late_delta20")
        # Source MQ135 channel only, late injection-baseline delta; NOT Rs/R0.
        value=signal[index+len(SENSORS)]
        out.append({"exposure_id":"UCI1081-row-"+str(row["source_row"]),
                    "gas_label":row["gas"],"source_id":"UCI1081",
                    "replicate_ordinal":row["replicate_ordinal"],
                    "ppb":row["ppb"],"scope":"PURE_GAS",
                    "features":(value,)})
    if len(out)!=90 or len({v["exposure_id"] for v in out})!=90:
        raise ValueError("source not 90 unique physical exposure rows")
    return out

def run(source_zip):
    source=parse_archive(source_zip)
    rows=make_rows(source)
    test=load_open_set()
    path=ROOT/"techniques/06.5/calibrated_open_set_source_v2.py"
    spec=importlib.util.spec_from_file_location("v4_calibrated_source_open_set",path)
    calibrated_module=importlib.util.module_from_spec(spec)
    import sys
    sys.path.insert(0,str(path.parent))
    spec.loader.exec_module(calibrated_module)
    results=[]
    for unseen in GASES:
        train=[r for r in rows if r["gas_label"]!=unseen and r["replicate_ordinal"]<=2]
        known=[r for r in rows if r["gas_label"]!=unseen and r["replicate_ordinal"]>=3]
        unknown=[r for r in rows if r["gas_label"]==unseen]
        if (len(train),len(known),len(unknown))!=(45,30,15):
            raise ValueError("wrong 5 gas × 3 ppm × replicate ordinal source split")
        x=test(train,known,unknown,source_id="UCI1081",sensor_ids=("MQ135",))
        calibration=[r for r in rows if r["gas_label"]!=unseen and r["replicate_ordinal"]==3]
        final_known=[r for r in rows if r["gas_label"]!=unseen and r["replicate_ordinal"]==4]
        if (len(calibration),len(final_known))!=(15,15):
            raise ValueError("calibration/final known test must be 15 source exposures each")
        same_split_baseline=test(train,final_known,unknown,source_id="UCI1081",sensor_ids=("MQ135",))
        source_calibrated=calibrated_module.calibrated_open_set_source(
            train,calibration,final_known,unknown,source_id="UCI1081",sensor_ids=("MQ135",),alpha=.1)
        results.append({"heldout_gas":unseen,
            "same_split_training_threshold_known_accept":same_split_baseline["known_accept_rate"],
            "same_split_training_threshold_unknown_reject":same_split_baseline["unknown_reject_rate"],
            "calibrated_threshold_known_accept":source_calibrated["known_accept_rate"],
            "calibrated_threshold_unknown_reject":source_calibrated["unknown_reject_rate"],
            "n_known_calibration":len(calibration),"n_known_final_test":len(final_known),
            "calibration_quantile_alpha":.1,"calibrated_threshold":source_calibrated["threshold"],
            "n_known_train":len(train),
                        "n_known_test":len(known),"n_unknown_test":len(unknown),
                        "known_accept_rate":x["known_accept_rate"],
                        "unknown_reject_rate":x["unknown_reject_rate"],
                        "method_status":x["model_status"]})
    return {"schema_version":"hazrob.tank.v4.uci1081.mq135.open-set-external.v1",
            "source":"UCI1081 DOI 10.24432/C5CK6F",
            "verified_source_zip_sha256":SOURCE_SHA256,
            "source_sample_count":90,"source_mq_channel":"MQ135",
            "sensor_schema":"SINGLE EXTERNAL MQ135 (not Tank 8 MQ)",
            "signal":"late 120-s injection median minus pre-injection baseline, source ADC-like units",
            "protocol":"leave one gas out, known gas replicate ordinals 0-2 train / 3-4 known test, all 15 of heldout gas as unknown",
            "source_true_session_device_ids_available":False,
            "G2_external_exploratory_measured":True,
            "G2_new_source_physical_validated":False,
            "G3_Tank":False,
            "publication_accuracy_claim":"NONE; small-six-gas source-domain research stress test",
            "per_unseen_gas":results,
            "macro_known_accept_rate":sum(x["known_accept_rate"] for x in results)/6,
            "macro_unknown_reject_rate":sum(x["unknown_reject_rate"] for x in results)/6,
            "same_split_training_threshold_macro_known_accept":sum(x["same_split_training_threshold_known_accept"] for x in results)/6,
            "same_split_training_threshold_macro_unknown_reject":sum(x["same_split_training_threshold_unknown_reject"] for x in results)/6,
            "independent_known_calibration_macro_known_accept":sum(x["calibrated_threshold_known_accept"] for x in results)/6,
            "independent_known_calibration_macro_unknown_reject":sum(x["calibrated_threshold_unknown_reject"] for x in results)/6,
            "warning":"Training-threshold open-set model is optimistic; source 50-200ppb below typical MQ nominal range; no certified ppm or safety."}

if __name__=="__main__":
    ap=argparse.ArgumentParser()
    ap.add_argument("original_zip")
    ap.add_argument("--output",default="uci1081_mq135_open_set_v4_results.json")
    args=ap.parse_args()
    result=run(args.original_zip)
    Path(args.output).write_text(json.dumps(result,indent=2)+"\n")
    print(json.dumps({k:v for k,v in result.items() if k!="per_unseen_gas"},indent=2))
