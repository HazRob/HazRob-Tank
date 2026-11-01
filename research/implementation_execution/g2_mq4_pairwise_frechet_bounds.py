"""Research-only sharp marginal bounds for hypothetical MQ4 OR/AND ensembles."""
import argparse, hashlib, itertools, json
from fractions import Fraction
from pathlib import Path
ROOT=Path(__file__).resolve().parent
SOURCE=ROOT/"G2_MQ4_EXTERNAL_SYSTEM_SUMMARY_LIBRARY_SNAPSHOT_2026-10-30.json"
EVIDENCE=ROOT/"G2_INDEPENDENT_EVIDENCE.json"
SHA="0df1ed3776aa515973886bad18153498325a038040bdb08d8a0a70ab35f5a1ea"
NAMES=("TRAIN_MEDIAN_REFERENCE","RIDGE_MQ4_1","RIDGE_MQ4_3","RIDGE_MQ4_3_T_RH","HGB_MQ4_3_T_RH")
P,N=2346,11840
def rational(n,d):
    f=Fraction(n,d)
    return [f.numerator,f.denominator]
def bounds(a,b,p,n,mode):
    if mode not in ("OR","AND"): raise ValueError("BAD_MODE")
    if type(p) is not int or type(n) is not int or min(p,n)<=0 or any(type(v) is not int or v<0 for v in (*a,*b)):
        raise ValueError("BAD_COUNTS")
    if a[0]+a[1]!=p or b[0]+b[1]!=p or a[2]+a[3]!=n or b[2]+b[3]!=n:
        raise ValueError("BAD_DENOMINATORS")
    if mode=="OR":
        tl,th=max(a[0],b[0]),min(p,a[0]+b[0])
        fl,fh=max(a[2],b[2]),min(n,a[2]+b[2])
    else:
        tl,th=max(0,a[0]+b[0]-p),min(a[0],b[0])
        fl,fh=max(0,a[2]+b[2]-n),min(a[2],b[2])
    return dict(tp_range=[tl,th],fn_range=[p-th,p-tl],fp_range=[fl,fh],tn_range=[n-fh,n-fl],
                sensitivity_range=[rational(tl,p),rational(th,p)],
                false_positive_rate_range=[rational(fl,n),rational(fh,n)])
def project(raw,evidence_raw):
    if hashlib.sha256(raw).hexdigest()!=SHA: raise ValueError("SOURCE_SHA_MISMATCH")
    s=json.loads(raw); e=json.loads(evidence_raw)
    if (s.get("schema")!="hazrob-tank-v6-mq4-external-system-evaluation/1" or
        s.get("role")!="EXTERNAL_SOURCE_RESEARCH_PIPELINE_ONLY_NOT_JAVA_ANDROID_RUNTIME" or
        s.get("source_rows")!=14186 or s.get("days")!=11 or s.get("distinct_gases")!=1 or
        s.get("four_ppm_diagnostic",{}).get("not_safety_threshold") is not True or
        s.get("evidence_policy",{}).get("tank_g3_validated")!=0):
        raise ValueError("INVALID_SOURCE_SCOPE")
    if e.get("schema")!="hazrob.g2.independent-evidence.v1" or e.get("records")!=[]:
        raise ValueError("UNVERIFIED_INDEPENDENT_EVIDENCE")
    ms=s.get("models")
    if not isinstance(ms,list) or [m.get("model") for m in ms]!=list(NAMES):
        raise ValueError("INVALID_MODEL_SET")
    cc=[]
    for m in ms:
        a=tuple(m.get(k) for k in ("total_4ppm_true_positive","total_4ppm_false_negative",
            "total_4ppm_false_positive","total_4ppm_true_negative"))
        if any(type(v) is not int or v<0 for v in a): raise ValueError("INVALID_COUNTS")
        if a[0]+a[1]!=P or a[2]+a[3]!=N: raise ValueError("INVALID_DENOMINATORS")
        if m.get("original_source_test_predictions")!=P+N or m.get("test_days")!=11:
            raise ValueError("UNSUPPORTED_ROW_ALIGNMENT")
        cc.append(a)
    rows=[]
    for i,j in itertools.combinations(range(5),2):
        rows.append(dict(models=[NAMES[i],NAMES[j]],OR=bounds(cc[i],cc[j],P,N,"OR"),
                         AND=bounds(cc[i],cc[j],P,N,"AND")))
    return dict(schema="hazrob.g2.mq4-pairwise-frechet-bounds.v1",source_library_summary_sha256=SHA,
        source_rows=14186,source_days=11,source_gases=1,reference_positive_rows=P,
        reference_negative_rows=N,threshold_ppm=4,threshold_is_safety_limit=False,
        pairs=rows,pairwise_predictions_available=False,independence_assumed=False,
        joint_ensemble_performance_measured=False,prospective_or_tank_validation=False,
        independent_physical_evidence_records=0,g2_qualified=False,g3_qualified=False,
        limitations="Sharp algebraic bounds from source pooled marginals; pairwise row predictions unavailable. Source is retrospective (10/11 folds use future training dates), serial rows are not independent exposures. No Tank calibration, multi-gas identity, prospective or safety-grade validation.")
if __name__=="__main__":
    parser=argparse.ArgumentParser()
    parser.add_argument("--source",type=Path,default=SOURCE)
    parser.add_argument("--evidence",type=Path,default=EVIDENCE)
    parser.add_argument("--output",type=Path,required=True)
    args=parser.parse_args()
    args.output.write_text(json.dumps(project(args.source.read_bytes(),args.evidence.read_bytes()),sort_keys=True,indent=2)+"\n",encoding="utf-8")
    print("Source-only sharp bounds; G2/G3 blocked")
