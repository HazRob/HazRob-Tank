#!/usr/bin/env python3
"""V6 N05-G: SHA-verified original UCI1081 time-major 30-bin sequence extraction.
The source stores 10 contiguous 900-sample sensor blocks per exposure.
Keep ALL 30 bins of each ORIGINAL exposure together in grouped holdouts.
No trained smoothing, food headspace or Tank ppm conversion.
"""
import argparse
import csv
import io
import json
import math
from pathlib import Path
from collections import Counter
import sys
sys.path.insert(0,str(Path(__file__).resolve().parent))
from extract_uci1081_verified import verified_bytes,sha,ZIP_SHA,CSV_SHA,GASES,LEVELS,CHANNELS

def main():
    p=argparse.ArgumentParser()
    p.add_argument("--archive")
    p.add_argument("--out",required=True)
    p.add_argument("--manifest",required=True)
    args=p.parse_args()
    source=verified_bytes(args.archive)
    rows=list(csv.reader(io.StringIO(source.decode("utf-8-sig"))))
    if len(rows)!=90:raise ValueError("original source 90 exposure rows required")
    counts=Counter()
    path=Path(args.out);path.parent.mkdir(parents=True,exist_ok=True)
    with path.open("w",newline="",encoding="utf-8") as f:
        w=csv.writer(f,lineterminator="\n")
        w.writerow(["row_id","gas","ppb"]+[f"t{time:02d}_s{channel:02d}" for time in range(30) for channel in range(10)])
        for idx,row in enumerate(rows):
            if len(row)!=9002:raise ValueError("source signal shape changed")
            gas=row[0].strip().lower()
            label=row[1].strip().lower()
            if gas not in GASES or not label.endswith("ppb"):raise ValueError("source label")
            ppb=int(label[:-3])
            if ppb not in LEVELS:raise ValueError("source concentration")
            samples=[float(v) for v in row[2:]]
            if not all(math.isfinite(x) for x in samples):raise ValueError("nonfinite raw UCI source")
            blocks=[samples[channel*900:(channel+1)*900] for channel in range(10)]
            baselines=[sum(block[:300])/300 for block in blocks]
            if any(abs(x)<1e-9 for x in baselines):raise ValueError("zero source baseline")
            values=[]
            for time in range(30):
                for ch in range(10):
                    source_bin=sum(blocks[ch][time*30:(time+1)*30])/30
                    values.append(source_bin/baselines[ch]-1)
            if not all(math.isfinite(v) for v in values):raise ValueError("undefined temporal sequence")
            w.writerow([idx,gas,ppb]+[format(v,".17g") for v in values])
            counts[(gas,ppb)]+=1
    if len(counts)!=18 or set(counts.values())!={5}:
        raise ValueError("original 6 gases x 3 concentration levels x 5 replicates required")
    manifest={
      "schema":"chemcore-v6-n05g-original-uci1081-temporal-sequences/1",
      "source_url":"https://archive.ics.uci.edu/dataset/1081/gas+sensor+array+low-concentration",
      "original_archive_sha256":ZIP_SHA,"original_csv_sha256":CSV_SHA,
      "derived_temporal_csv_sha256":sha(path.read_bytes()),
      "original_exposure_rows":90,
      "feature_layout":"30 consecutive NONOVERLAPPING 30-second bins x 10 original source sensors, time-major",
      "source_layout":"ten 900-sample contiguous channel blocks per 90 exposure rows",
      "normalization":"each 30-second mean / SAME exposure's first 300-second raw baseline - 1",
      "no_trained_feature_preprocessing":True,
      "source_sensor_names":list(CHANNELS),
      "evaluation":"whole concentration held out, 60 exposure training rows and 30 exposure test rows",
      "source_day_device_holdout_proven":False,"tank_physical_g3":False,"calibrated_ppm":False,
      "graph_physical_adjacency_proven":False,
      "temporal_startup_modeling":"not part of this study; first 300-second source baseline only"
    }
    Path(args.manifest).write_text(json.dumps(manifest,indent=2)+"\n",encoding="utf-8")
    print("PASS UCI N05-G verified archive and CSV SHA256")
    print("PASS 90 original exposure sequences, 30 bins x 10 source sensor channels")
    print("SOURCE_TEMPORAL_SHA256="+manifest["derived_temporal_csv_sha256"])
if __name__=="__main__":main()
