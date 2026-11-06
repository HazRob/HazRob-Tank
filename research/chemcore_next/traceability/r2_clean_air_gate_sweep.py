#!/usr/bin/env python3
"""Ablation: evaluate clean-air evidence gate across 10,380 synthetic Stable
source curves plus 1,250 owner R_air-anchored negative controls.
This is only a diagnostic sweep, NOT validated air-noise acceptance or hardware
alarm logic. Fail open for absent baseline; never call a simulated control physical FPR.
"""
import csv,json,math,pathlib,argparse,collections
THRESHOLDS=(0.,.01,.02,.03,.05,.08,.10,.15,.20,.30)
def load(p):
    with p.open(encoding="utf8",newline="") as f:return list(csv.DictReader(f,delimiter="\t"))
def main():
    p=argparse.ArgumentParser()
    p.add_argument("--positive-cases",required=True,type=pathlib.Path)
    p.add_argument("--positive-trace",required=True,type=pathlib.Path)
    p.add_argument("--blank-cases",required=True,type=pathlib.Path)
    p.add_argument("--blank-trace",required=True,type=pathlib.Path)
    p.add_argument("--owner-air",required=True,type=pathlib.Path)
    p.add_argument("--out",required=True,type=pathlib.Path)
    a=p.parse_args()
    owners=json.loads(a.owner_air.read_text())["r_air_kohm_by_sensor"]
    def build(inputs,traces):
        cases=load(inputs);by={r["case_id"]:r for r in load(traces)}
        assert len(cases)==len(by)
        out=[]
        for r in cases:
            t=by[r["case_id"]]
            channels=[x.split(":") for x in r["measurements"].split(";") if x]
            deviations=[abs(math.log10(float(v)/owners[k])) for k,v in channels]
            delta=max(deviations) if deviations else 0
            out.append((r,t,delta))
        return out
    positive=build(a.positive_cases,a.positive_trace)
    blanks=build(a.blank_cases,a.blank_trace)
    assert len(positive)==10380 and len(blanks)==1250
    summaries=[]
    for mode in sorted({r["r0mode"] for r,_,_ in positive}):
        for t in THRESHOLDS:
            pp=[(r,x,delta) for r,x,delta in positive if r["r0mode"]==mode]
            bb=[(r,x,delta) for r,x,delta in blanks if r["r0mode"]==mode]
            def eval(samples,blank):
                emit=[(r,x,delta) for r,x,delta in samples if delta>t and int(x["synthetic_qualified_rank1"])!=0]
                correct=0 if blank else sum(int(x["synthetic_qualified_rank1"])==100000+int(r["legacy"]) for r,x,_ in emit)
                return dict(total=len(samples), emitted=len(emit),correct=correct,
                    emitted_fraction=round(len(emit)/len(samples),6),
                    matched_fraction=round(correct/len(samples),6) if not blank else None,
                    predicted_precision=round(correct/len(emit),6) if emit else None,
                    baseline_suppressed_count=sum(delta<=t for _,_,delta in samples))
            summaries.append(dict(r0mode=mode,threshold_abs_log10_rsr_air=t,
                source_modelled=eval(pp,False),synthetic_blank=eval(bb,True)))
    a.out.mkdir(parents=True,exist_ok=True)
    out={"schema":"chemcore.next.r2.clean_air_uncertainty_ablation/1",
         "original_gas_cases":len(positive),"original_blank_cases":len(blanks),
         "gate_definition":"all available source-channel Rs are inside |log10(Rs/R_air)|<=threshold",
         "source_noise_variance_from_owner_device":None,
         "thresholds_are_research_sweep_only":True,
         "no_empirical_acceptance":True,
         "treat_unknown_oxidizers_differently_in_future":True,
         "measurements_not_real_exposure":True,"results":summaries}
    (a.out/"r2_clean_air_gate_sweep.json").write_text(json.dumps(out,indent=2)+"\n")
    print("NEXT_R2_CLEAN_AIR_GATE_SWEEP_PASS",json.dumps({
        "scenarios":len(summaries),
        "source_r0_mode":[r for r in summaries if r["r0mode"]=="R0_SOURCE_K_CONDITIONAL" and
              r["threshold_abs_log10_rsr_air"] in (0,.02,.05,.1,.15)]},sort_keys=True))
if __name__=="__main__":main()
