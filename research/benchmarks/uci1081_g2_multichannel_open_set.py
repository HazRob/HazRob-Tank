"""Pre-registered UCI1081 exploratory open-set comparison, no Tank G2/G3 claims.

Source trace -> median(late injection) - median(pre-injection), not R0.
Known-only calibration of a nearest-centroid distance threshold; unknown class
is NEVER used for threshold selection. Replicate ordinal is not a verified
physical session/device group, and source units are not established.
"""
import argparse
import json
import math
from pathlib import Path
from statistics import mean, pstdev
from uci1081_actual_modules_v1 import parse_archive, features, GASES, SENSORS, SOURCE_SHA256

SENSOR_SETS = {
    "MQ135_ONLY": ("MQ135",),
    "MQ135_MQ137_MQ138": ("MQ135", "MQ137", "MQ138"),
    "ALL_SOURCE_10": tuple(SENSORS),
}
DISTANCES = ("EUCLIDEAN", "MANHATTAN")

def _distance(x,y,method):
    diffs=[abs(a-b) for a,b in zip(x,y)]
    return math.sqrt(sum(d*d for d in diffs)) if method=="EUCLIDEAN" else sum(diffs)

def evaluate_one(train,cal,known,unknown,indices,method):
    """Evaluate one predeclared feature layout without unknown-label tuning."""
    groups=[train,cal,known,unknown]
    if any(not g for g in groups): raise ValueError("four nonempty partitions required")
    ids=[r["exposure_id"] for g in groups for r in g]
    if len(ids)!=len(set(ids)): raise ValueError("exposure reused across partitions")
    known_labels={r["gas"] for r in train}
    if len(known_labels)<2 or not {r["gas"] for r in cal+known}.issubset(known_labels):
        raise ValueError("known validation/test labels not covered by train")
    if {r["gas"] for r in unknown}&known_labels:
        raise ValueError("unknown gas present in training")
    if method not in DISTANCES or not indices or len(set(indices))!=len(indices):
        raise ValueError("invalid pre-registered metric or channel indices")
    if any(not isinstance(i,int) or isinstance(i,bool) or not 0<=i<len(SENSORS) for i in indices):
        raise ValueError("invalid channel index")
    if any(len(r["features"])!=len(SENSORS) or any(not math.isfinite(v) for v in r["features"]) for g in groups for r in g):
        raise ValueError("nonfinite or mismatched source feature vector")
    mu=[mean(r["features"][i] for r in train) for i in indices]
    sd=[max(pstdev(r["features"][i] for r in train),1e-9) for i in indices]
    def z(r):return tuple((r["features"][i]-a)/b for i,a,b in zip(indices,mu,sd))
    centers={gas:tuple(mean(z(r)[j] for r in train if r["gas"]==gas) for j in range(len(indices))) for gas in sorted(known_labels)}
    def predict(r):
        v=z(r)
        gas,d=min(((gas,_distance(v,center,method)) for gas,center in centers.items()),key=lambda a:(a[1],a[0]))
        return gas,d
    # n=15; ceil((n+1)*0.9)=15, so max known calibration distance.
    alpha=0.1
    n=len(cal)
    rank=math.ceil((n+1)*(1-alpha))
    if rank>n:raise ValueError("insufficient calibration exposures for alpha")
    threshold=sorted(predict(r)[1] for r in cal)[rank-1]
    known_scored=[(r,predict(r)) for r in known]
    unknown_scored=[(r,predict(r)) for r in unknown]
    ka=sum(d<=threshold for _,(_,d) in known_scored)
    kr=sum(gas==r["gas"] and d<=threshold for r,(gas,d) in known_scored)
    ur=sum(d>threshold for _,(_,d) in unknown_scored)
    return {"n_train":len(train),"n_cal":len(cal),"n_known_test":len(known),"n_unknown_test":len(unknown),
            "known_accept":ka,"known_correct_and_accepted":kr,"unknown_reject":ur,
            "known_accept_rate":ka/len(known),"known_correct_and_accepted_rate":kr/len(known),
            "unknown_reject_rate":ur/len(unknown),"threshold":threshold}

def make_rows(archive):
    raw=parse_archive(archive)
    rows=[]
    for r in raw:
        v=features(r["trace"],"early_late_delta20")
        rows.append({"exposure_id":f"UCI1081-row-{r['source_row']}","gas":r["gas"],
                     "ppb":r["ppb"],"replicate_ordinal":r["replicate_ordinal"],
                     "features":tuple(v[len(SENSORS):])})
    if len(rows)!=90 or len({r["exposure_id"] for r in rows})!=90:
        raise ValueError("expected 90 unique physical exposures")
    return rows

def run(archive):
    rows=make_rows(archive)
    results=[]
    for unseen in GASES:
        train=[r for r in rows if r["gas"]!=unseen and r["replicate_ordinal"]<=2]
        cal=[r for r in rows if r["gas"]!=unseen and r["replicate_ordinal"]==3]
        known=[r for r in rows if r["gas"]!=unseen and r["replicate_ordinal"]==4]
        unknown=[r for r in rows if r["gas"]==unseen]
        if (len(train),len(cal),len(known),len(unknown))!=(45,15,15,15):
            raise ValueError("incorrect known/calibration/unknown source partitions")
        for sensor_set,names in SENSOR_SETS.items():
            indices=tuple(SENSORS.index(s) for s in names)
            for method in DISTANCES:
                score=evaluate_one(train,cal,known,unknown,indices,method)
                results.append({"heldout_gas":unseen,"sensor_set":sensor_set,"distance":method,**score})
    aggregate=[]
    for sensor_set in SENSOR_SETS:
        for method in DISTANCES:
            subset=[r for r in results if r["sensor_set"]==sensor_set and r["distance"]==method]
            assert len(subset)==6
            aggregate.append({"sensor_set":sensor_set,"distance":method,
                              "known_accept":sum(r["known_accept"] for r in subset),
                              "known_correct_and_accepted":sum(r["known_correct_and_accepted"] for r in subset),
                              "unknown_reject":sum(r["unknown_reject"] for r in subset),
                              "known_total":sum(r["n_known_test"] for r in subset),
                              "unknown_total":sum(r["n_unknown_test"] for r in subset),
                              "known_accept_rate":mean(r["known_accept_rate"] for r in subset),
                              "known_correct_and_accepted_rate":mean(r["known_correct_and_accepted_rate"] for r in subset),
                              "unknown_reject_rate":mean(r["unknown_reject_rate"] for r in subset)})
    return {"schema":"hazrob.g2.uci1081.predeclared-multichannel-open-set.v1",
            "source":"UCI1081 DOI 10.24432/C5CK6F","source_zip_sha256":SOURCE_SHA256,
            "n_exposures":90,"n_folds":6,"n_predeclared_configurations":6,
            "feature":"median(t780:900) - median(t180:300) for each source channel; NOT Rs/R0",
            "source_signal_units":"UNVERIFIED","r0_mode":"NOT_USED","r0_subtracted":False,
            "split":"known train replicate ordinals 0-2; known calibration 3; known test 4; unknown heldout gas all 0-4",
            "split_warning":"Replicate ordinal inferred, not verified independent physical session/device",
            "threshold_rule":"ceil((n_cal+1)*0.9) order statistic of known-only calibration distances; no unknown tuning",
            "G2_qualified":False,"Tank_G3":False,"ppm_calibrated":False,"safety_grade":False,
            "per_fold":results,"aggregate":aggregate}

if __name__=="__main__":
    ap=argparse.ArgumentParser();ap.add_argument("original_zip");ap.add_argument("--output",default="uci1081_multichannel_open_set_results.json")
    args=ap.parse_args();out=run(args.original_zip)
    Path(args.output).write_text(json.dumps(out,indent=2,allow_nan=False)+"\n")
    for r in out["aggregate"]:
        print(f"{r['sensor_set']}/{r['distance']} known_accept={r['known_accept']}/90 correct_accepted={r['known_correct_and_accepted']}/90 unknown_reject={r['unknown_reject']}/90")
