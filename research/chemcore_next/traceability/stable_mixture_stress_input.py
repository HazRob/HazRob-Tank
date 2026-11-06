#!/usr/bin/env python3
"""Generate nonphysical two-model source response blends for adversarial
mixture-abstention software stress. Geometric interpolation of two Rs curves
is not a physical gas-mixing law and must NEVER train a chemical classifier.
"""
import argparse,csv,json,math,pathlib,random,itertools,collections
GASES=(20,75,76,103,135,139)
GRID=(200.,500.,1000.,3000.,5000.)
WEIGHTS=(.25,.5,.75)
MODES=("R0_SOURCE_K_CONDITIONAL","DEVICE_R_AIR_AS_SOURCE_R0",
       "FIXED_10K_FOR_ALL","MQ2_K_FOR_ALL_WRONG")
K={"MQ2":9.83,"MQ3":60.,"MQ4":4.4,"MQ5":6.5,"MQ7":27.5,
   "MQ8":70.,"MQ9":9.6,"MQ135":3.6}
def main():
    p=argparse.ArgumentParser()
    p.add_argument("--priors",type=pathlib.Path,required=True)
    p.add_argument("--owner-air",type=pathlib.Path,required=True)
    p.add_argument("--out",type=pathlib.Path,required=True)
    a=p.parse_args()
    with a.priors.open(encoding="utf8",newline="") as f:
        rows=list(csv.DictReader(f,delimiter="\t"))
    assert len(rows)==49
    air=json.loads(a.owner_air.read_text())["r_air_kohm_by_sensor"]
    by=collections.defaultdict(list)
    for m in rows:by[int(m["legacy"])].append(m)
    rng=random.Random(20261106)
    scenarios=[];truth=[]
    for gasA,gasB in itertools.combinations(GASES,2):
        for cA,cB in itertools.product(GRID,repeat=2):
            groups=[]
            for gas,ppm in ((gasA,cA),(gasB,cB)):
                candidates={}
                for m in by[gas]:
                    if float(m["lo"])<=ppm<=float(m["hi"]):
                        key=m["sensor"]
                        if key not in candidates or (m["evidence_tier"]=="M2" and
                              candidates[key]["evidence_tier"]!="M2"):
                            candidates[key]=m
                groups.append(candidates)
            overlap=sorted(set(groups[0])&set(groups[1]))
            if len(overlap)<2:continue
            for weight,noise,repeat in itertools.product(WEIGHTS,(0.,.03,.10),range(2)):
                measurements={}
                for sensor in overlap:
                    ma=groups[0][sensor];mb=groups[1][sensor]
                    ra=float(ma["a"])*cA**float(ma["b"])
                    rb=float(mb["a"])*cB**float(mb["b"])
                    # Geometric interpolation is adversarial numeric stress only.
                    modeled=math.exp(weight*math.log(ra)+(1-weight)*math.log(rb))
                    perturb=max(.05,1+rng.gauss(0,noise))
                    measurements[sensor]=air[sensor]/K[sensor]*modeled*perturb
                capture=";".join(f"{sensor}:{value:.14g}" for sensor,value in measurements.items())
                for mode in MODES:
                    case_id=f"BLEND_{len(scenarios):07d}"
                    scenarios.append(dict(case_id=case_id,legacy=0,
                       label=f"UNPHYSICAL_SOURCE_BLEND_{gasA}_{gasB}",
                       ppm=0,environment=f"SOURCE_BLEND_{weight}_NOISE_{noise}",
                       repeat=repeat,r0mode=mode,measurements=capture,
                       sensor_count=len(overlap),
                       origin="MODELLED_GEOMETRIC_INTERPOLATION_NOT_CHEMICAL_MIXTURE"))
                    truth.append(dict(case_id=case_id,first_source_legacy=gasA,
                        second_source_legacy=gasB,first_source_concentration_ppm=cA,
                        second_source_concentration_ppm=cB,blend_weight_first=weight,
                        ratio_noise_sd=noise,physical_mixture_ground_truth=False,
                        no_real_mixture_ppm=True,overlapping_sensor_count=len(overlap)))
    assert len(scenarios)>1000,len(scenarios)
    a.out.mkdir(parents=True,exist_ok=True)
    with (a.out/"stable_blend_scenarios.tsv").open("w",newline="",encoding="utf8") as f:
        w=csv.DictWriter(f,fieldnames=list(scenarios[0]),delimiter="\t");w.writeheader();w.writerows(scenarios)
    with (a.out/"stable_blend_provenance.tsv").open("w",newline="",encoding="utf8") as f:
        w=csv.DictWriter(f,fieldnames=list(truth[0]),delimiter="\t");w.writeheader();w.writerows(truth)
    manifest={"schema":"chemcore.next.stable49.nonphysical_mixture_numeric_stress/1",
        "cases":len(scenarios),"distinct_model_pairs":len(set((x["first_source_legacy"],x["second_source_legacy"]) for x in truth)),
        "noise_sd":[0,.03,.10],"mixing_law":"GEOMETRIC_INTERPOLATION_NOT_REAL_GAS_MIXTURE",
        "r0_modes":MODES,"physical_exposure_count":0,"not_g3":True}
    (a.out/"stable_blend_manifest.json").write_text(json.dumps(manifest,indent=2)+"\n")
    print("STABLE_MODEL_BLEND_SOURCE_STRESS_PREPARED",json.dumps(manifest,sort_keys=True))
if __name__=="__main__":main()
