#!/usr/bin/env python3
"""Stable 1.0.0-hr069r1 49-prior replay matrix; data are SOURCE-MODEL SYNTHETIC.
No prior ethanol experiment, no physical gas calibration, no Tank G3.
The 49 priors are recreated by the SHA-pinned ORIGINAL Stable generator.
R0 from MQSensorsLib K is a CONDITIONAL EXAMPLE, not a factory absolute value.
"""
import argparse,csv,json,math,pathlib,random
from collections import Counter
MODELS=("MQ2","MQ3","MQ4","MQ5","MQ7","MQ8","MQ9","MQ135")
K={"MQ2":9.83,"MQ3":60.,"MQ4":4.4,"MQ5":6.5,"MQ7":27.5,
   "MQ8":70.,"MQ9":9.6,"MQ135":3.6}
LEVELS=[.05,.1,.2,1,5,10,25,50,100,200,500,1000,3000,5000,10000]
R0_MODES=("R0_SOURCE_K_CONDITIONAL","DEVICE_R_AIR_AS_SOURCE_R0","FIXED_10K_FOR_ALL",
          "MQ2_K_FOR_ALL_WRONG","AIR_RS_MEAN_STRICT_UNQUALIFIED")
VARIANTS=("IDEAL","RANDOM_NOISE_2PCT","RANDOM_NOISE_10PCT",
          "DRIFT_RS_UP_20PCT","DRIFT_RS_DOWN_20PCT","ENV_GAIN_UP_15PCT",
          "ENV_GAIN_DOWN_15PCT","DROP_ONE_PRESENT_SENSOR")
def write(out,rows,cols):
    with out.open("w",newline="",encoding="utf-8") as h:
        w=csv.DictWriter(h,fieldnames=cols,delimiter="\t");w.writeheader();w.writerows(rows)
def main():
    p=argparse.ArgumentParser()
    p.add_argument("--priors",required=True,type=pathlib.Path)
    p.add_argument("--owner-air",required=True,type=pathlib.Path)
    p.add_argument("--out",required=True,type=pathlib.Path)
    args=p.parse_args()
    args.out.mkdir(parents=True,exist_ok=True)
    payload=json.loads(args.priors.read_text())
    assert payload["schema"]=="hazrob.chemcore-response-priors.v1"
    models=payload["records"]
    assert len(models)==49 and payload["source_curve_count"]==43 and payload["preferred_literature_overlay_count"]==6
    assert all(m["form"]=="LOG10_LINEAR" and not m["device_validated"] for m in models)
    rair=json.loads(args.owner_air.read_text())["r_air_kohm_by_sensor"]
    assert set(rair)==set(MODELS)
    by_gas={}
    priorRows=[]
    for i,m in enumerate(models):
        model=m["sensor_key"].upper()
        assert model in MODELS
        gas=int(m["legacy_id"])
        by_gas.setdefault(gas,[]).append(m)
        priorRows.append(dict(index=i+1,legacy=gas,gas=m["label"],sensor=model,
              root=m["source_id"],asset=m["model_version"]+"_"+str(i+1),a=10**m["intercept"],
              b=m["slope"],lo=m["min_ppm"],hi=m["max_ppm"],sigma=m["sigma_log10"],
              evidence_tier=m["evidence_tier"]))
    assert len(by_gas)==13
    write(args.out/"stable_49_original_priors.tsv",priorRows,
          ["index","legacy","gas","sensor","root","asset","a","b","lo","hi","sigma","evidence_tier"])
    experiment=[]
    rng=random.Random(20261106)
    source_seen=Counter()
    for legacy,rows in sorted(by_gas.items()):
        for ppm in LEVELS:
            active=[m for m in rows if m["min_ppm"]<=ppm<=m["max_ppm"]]
            if not active: continue
            # One highest-evidence model/channel at a concentration, not double-count
            # M1/M2 for same sensor. Absent model -> missing channel, not simulated no-response.
            select={}
            for m in active:
                sensor=m["sensor_key"].upper()
                if sensor not in select or (m["evidence_tier"]=="M2" and select[sensor]["evidence_tier"]!="M2"):
                    select[sensor]=m
            for variant in VARIANTS:
                for repeat in range(3):
                    trueRs={}
                    for sensor,m in sorted(select.items()):
                        ratio=10**(m["intercept"]+m["slope"]*math.log10(ppm))
                        # No ambient physics is assumed: ENV_GAIN is a uniform artificial sensitivity.
                        r0_source=rair[sensor]/K[sensor]
                        modifier=1.
                        if variant=="RANDOM_NOISE_2PCT":modifier=1+rng.gauss(0,.02)
                        if variant=="RANDOM_NOISE_10PCT":modifier=1+rng.gauss(0,.10)
                        if variant=="DRIFT_RS_UP_20PCT":modifier=1.20
                        if variant=="DRIFT_RS_DOWN_20PCT":modifier=.80
                        if variant=="ENV_GAIN_UP_15PCT":modifier=1.15
                        if variant=="ENV_GAIN_DOWN_15PCT":modifier=.85
                        if modifier<=0:modifier=.05
                        trueRs[sensor]=ratio*r0_source*modifier
                    if variant=="DROP_ONE_PRESENT_SENSOR" and trueRs:
                        drop=sorted(trueRs)[repeat%len(trueRs)]
                        trueRs.pop(drop)
                    if not trueRs:continue
                    for mode in R0_MODES:
                        serialized=";".join(k+":"+format(v,".14g") for k,v in sorted(trueRs.items()))
                        source_seen[legacy]+=1
                        experiment.append(dict(case_id="STABLE_%05d"%len(experiment),
                           legacy=legacy,label=rows[0]["label"],ppm=ppm,
                           environment=variant,repeat=repeat,r0mode=mode,
                           measurements=serialized,sensor_count=len(trueRs),
                           origin="STABLE_SOURCE_MODEL_SYNTHETIC_NOT_INDEPENDENT"))
    assert experiment and len(priorRows)==49
    write(args.out/"stable_scenarios.tsv",experiment,
       ["case_id","legacy","label","ppm","environment","repeat","r0mode",
        "measurements","sensor_count","origin"])
    stats={
      "schema":"hazrob.next.stable49.source_synthetic_matrix/1",
      "source_stable":"1.0.0-hr069r1",
      "stable_model_count":len(models),"material_families":len(by_gas),
      "source_concentration_points":LEVELS,
      "r0_modes":R0_MODES,"perturbations":VARIANTS,
      "replicates_per_setting":3,
      "cases":len(experiment),"source_ground_truth":"MODEL_GENERATED_NOT_MEASURED",
      "owner_clean_air_reference_is_R_air":True,
      "no_actual_tank_exposures":True,
      "no_old_ethanol_experiment":True,"no_evidence_of_90_percent_real_accuracy":True,
      "remarks":"Only channels with source gas curves are emitted; absent channels never fabricated. K is a conditional MQSensorsLib example on different source curves."
    }
    (args.out/"stable_matrix_manifest.json").write_text(json.dumps(stats,ensure_ascii=False,indent=2)+"\n")
    print("STABLE_SOURCE_MATRIX_PREPARED",json.dumps(stats,sort_keys=True))
if __name__=="__main__":main()
