#!/usr/bin/env python3
"""Synthetic blank-air negative control using owner's eight R_air reference means.
No fabricated fresh Tank measurement: exact/perturbed baseline is a G1 diagnostic.
"""
import argparse,csv,json,random,pathlib
p=argparse.ArgumentParser()
p.add_argument("--owner-air",required=True,type=pathlib.Path)
p.add_argument("--out",required=True,type=pathlib.Path)
x=p.parse_args()
air=json.loads(x.owner_air.read_text())["r_air_kohm"]
assert len(air)==8
modes=("R0_SOURCE_K_CONDITIONAL","DEVICE_R_AIR_AS_SOURCE_R0","FIXED_10K_FOR_ALL",
       "MQ2_K_FOR_ALL_WRONG","AIR_RS_MEAN_STRICT_UNQUALIFIED")
variant=("CLEAN_EXACT","CLEAN_NOISE_2PCT","CLEAN_NOISE_5PCT",
         "CLEAN_DRIFT_UP_10PCT","CLEAN_DRIFT_DOWN_10PCT")
rng=random.Random(20261106)
rows=[]
for typ in variant:
    for repeat in range(50):
        values={}
        for k,v in sorted(air.items()):
            mult=1.0
            if typ=="CLEAN_NOISE_2PCT":mult=max(0.2,1.0+rng.gauss(0,0.02))
            if typ=="CLEAN_NOISE_5PCT":mult=max(0.2,1.0+rng.gauss(0,0.05))
            if typ=="CLEAN_DRIFT_UP_10PCT":mult=1.1
            if typ=="CLEAN_DRIFT_DOWN_10PCT":mult=0.9
            values[k]=v*mult
        capture=";".join(f"{k}:{v:.14g}" for k,v in values.items())
        for mode in modes:
            rows.append(dict(case_id=f"BLANK_{len(rows):05d}",legacy=0,label="CLEAN_AIR_SYNTHETIC",
                ppm=0,environment=typ,repeat=repeat,r0mode=mode,
                measurements=capture,sensor_count=8,
                origin="MEASURED_R_AIR_MEANS_PLUS_SYNTHETIC_PERTURBATION_NOT_NEW_GAS_EXPOSURE"))
assert len(rows)==1250
with x.out.open("w",newline="",encoding="utf8") as f:
    w=csv.DictWriter(f,fieldnames=list(rows[0]),delimiter="\t");w.writeheader();w.writerows(rows)
print("STABLE_BLANK_AIR_G1_CONTROL_PREPARED cases=1250")
