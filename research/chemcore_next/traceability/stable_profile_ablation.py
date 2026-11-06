#!/usr/bin/env python3
"""Comparative RESEARCH-ONLY ranker on pinned Stable 49 source-modelled inputs.
Not ChemCoreNext production code. Source data are mathematically self-generated,
not independent gas measurements. No model trained or qualified here.
Tests whether source-root ambiguity can be reduced by multichannel
concentration-coherent matching WITHOUT counting correlated source roots twice.
"""
import csv,json,math,pathlib,argparse,statistics,collections
def read(path):
    with path.open(encoding="utf8",newline="") as h:return list(csv.DictReader(h,delimiter="\t"))
K={"MQ2":9.83,"MQ3":60.,"MQ4":4.4,"MQ5":6.5,"MQ7":27.5,
   "MQ8":70.,"MQ9":9.6,"MQ135":3.6}
AIR={"MQ2":92.1417313,"MQ3":25.5138416,"MQ4":37.8870316,"MQ5":158.0950317,
     "MQ7":62.5898705,"MQ8":36.5759659,"MQ9":56.4506149,"MQ135":113.6592255}
METHODS=("RMS_TWO_CHANNEL_MIN","RMS_OVERLAP_PENALIZED","FULL_OBS_CHANNEL_COVERAGE")
THRESHOLDS=(0.0,0.15,0.35)
def denominator(m,mode):
    if mode=="R0_SOURCE_K_CONDITIONAL":return AIR[m]/K[m]
    if mode=="DEVICE_R_AIR_AS_SOURCE_R0":return AIR[m]
    if mode=="FIXED_10K_FOR_ALL":return 10.
    if mode=="MQ2_K_FOR_ALL_WRONG":return AIR[m]/K["MQ2"]
    if mode=="AIR_RS_MEAN_STRICT_UNQUALIFIED":return None
    raise ValueError(mode)
def make_candidates(priors):
    by=collections.defaultdict(list)
    for v in priors:
        by[int(v["legacy"])].append(dict(model=v["sensor"],a=float(v["a"]),b=float(v["b"]),
                    lo=float(v["lo"]),hi=float(v["hi"]),sigma=float(v["sigma"]),
                    tier=v["evidence_tier"]))
    grid=set([.05,.1,.2,1,5,10,25,50,100,200,500,1000,3000,5000,10000])
    lo=.05;hi=10000.
    grid.update(10**(math.log10(lo)+(math.log10(hi)-math.log10(lo))*i/140) for i in range(141))
    profiles={}
    for uid,arr in by.items():
        vlist=[]
        for ppm in sorted(grid):
            active={}
            for m in arr:
                if m["lo"]<=ppm<=m["hi"]:
                    old=active.get(m["model"])
                    if old is None or (m["tier"]=="M2" and old["tier"]!="M2"):
                        active[m["model"]]=m
            if not active:continue
            predicted={key:(math.log10(m["a"])+m["b"]*math.log10(ppm),m["sigma"]) for key,m in active.items()}
            vlist.append((ppm,predicted))
        profiles[uid]=vlist
    assert len(profiles)==13
    return profiles
def evaluate(row,models):
    raw={}
    for token in row["measurements"].split(";"):
        model,rs=token.split(":")
        d=denominator(model,row["r0mode"])
        if d is None:return {m:(0,0,0,0,0) for m in METHODS}
        raw[model]=math.log10(float(rs)/d)
    n=len(raw)
    candidates=[]
    for uid,profiles in models.items():
        best=None
        for ppm,src in profiles:
            shared=[key for key in raw if key in src]
            if len(shared)<2:continue
            norm=sum(((raw[key]-src[key][0])/src[key][1])**2 for key in shared)
            rms=math.sqrt(norm/len(shared))
            if best is None or rms<best[0]:best=(rms,len(shared),ppm)
        if best is not None:candidates.append((uid,*best))
    results={}
    for method in METHODS:
        scores=[]
        for uid,rms,overlap,ppm in candidates:
            if method=="FULL_OBS_CHANNEL_COVERAGE" and overlap<n:continue
            penalty=(0.55*(n-overlap)/n if method=="RMS_OVERLAP_PENALIZED" else 0.)
            scores.append((rms+penalty,uid,ppm,overlap,rms))
        scores.sort(key=lambda v:(v[0],-v[3],v[1]))
        if not scores:
            results[method]=(0,0,0,0,0)
            continue
        best=scores[0]
        margin=(scores[1][0]-best[0]) if len(scores)>1 else float("inf")
        results[method]=(best[1],best[0],margin,best[2],best[3])
    return results
def main():
    p=argparse.ArgumentParser();p.add_argument("--priors",type=pathlib.Path,required=True)
    p.add_argument("--cases",type=pathlib.Path,required=True)
    p.add_argument("--out",type=pathlib.Path,required=True);args=p.parse_args()
    args.out.mkdir(parents=True,exist_ok=True)
    profiles=make_candidates(read(args.priors))
    rows=read(args.cases);assert len(rows)>1000
    results=[]
    for i,row in enumerate(rows):
        candidates=evaluate(row,profiles)
        for method,(uid,score,margin,pred_ppm,overlap) in candidates.items():
            for limit in THRESHOLDS:
                # fixed research thresholds for sensitivity analysis only, not tuned to labels
                accepted=(uid!=0 and score<=1.5 and margin>=limit)
                results.append(dict(case_id=row["case_id"],source_label=row["label"],
                    ground_truth_legacy=row["legacy"],true_source_ppm=row["ppm"],
                    perturbation=row["environment"],r0_mode=row["r0mode"],method=method,
                    margin_threshold=limit,ranked_uid=uid,fit_error=score if uid else None,
                    margin=margin if math.isfinite(margin) else None,matching_channels=overlap,
                    accepted=accepted,correct=accepted and uid==int(row["legacy"]),
                    predicted_source_ppm=pred_ppm if uid else None))
    out=args.out/"stable49_candidate_profile_ablation.csv"
    with out.open("w",encoding="utf8",newline="") as f:
        w=csv.DictWriter(f,fieldnames=list(results[0]));w.writeheader();w.writerows(results)
    summary={}
    for method in METHODS:
        summary[method]={}
        for limit in THRESHOLDS:
            group=[r for r in results if r["method"]==method and r["margin_threshold"]==limit]
            valid=[r for r in group if r["r0_mode"]!="AIR_RS_MEAN_STRICT_UNQUALIFIED"]
            bymode={}
            for mode in sorted(set(r["r0_mode"] for r in valid)):
                v=[r for r in valid if r["r0_mode"]==mode]
                chosen=[r for r in v if r["accepted"]]
                correct=sum(r["correct"] for r in chosen)
                bymode[mode]=dict(n=len(v),coverage=round(len(chosen)/len(v),6),
                     correct_all=round(correct/len(v),6),
                     precision_accepted=(round(correct/len(chosen),6) if chosen else None),
                     abstentions=len(v)-len(chosen),
                     source_model_self_generated=True)
            summary[method][str(limit)]=bymode
    payload={"schema":"chemcore.next.stable49.multi_sensor_profile_ablation/1",
             "test_cases":len(rows),"method_types":METHODS,"thresholds":THRESHOLDS,
             "results":summary,
             "interpretation":"self-generated synthetic Stable reference profiles; precision here is NOT empirical; no 90% qualification; proposed scorer is NOT part of actual Next code",
             "source_meta":"49 original non-M4 priors; original source-root correlation preserved in separate real engine test"}
    (args.out/"stable49_ablation_summary.json").write_text(json.dumps(payload,ensure_ascii=False,indent=2)+"\n")
    print("STABLE49_RESEARCH_RANKING_ABLATION_PASS",json.dumps({
        "cases":len(rows),"method_count":len(METHODS),"result_rows":len(results),
        "source_reference_r0_mode":{m:summary[m]["0.15"].get("R0_SOURCE_K_CONDITIONAL") for m in METHODS}},
        sort_keys=True))
if __name__=="__main__":main()
