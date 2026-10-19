"""Immutable UCI 270 gas drift archive adapter. Research only.

Each libsvm-like row: gas_class;known_ppmv 1:feature1 ... 128:feature128.
The 128 attributes are PRECOMPUTED time-series descriptors from 16 gas
sensors, NOT raw waveforms or MQ Tank measurements. Ten temporal batches are
not interchangeable with ten different devices.
"""
import collections
import hashlib
import io
import math
import re
import zipfile
from pathlib import Path

ZIP_SHA256="98fe3a30981a222dd4518fbcc3dddd45d5c0ce9b03ef6dc6fe5cf7a04cfbff5e"
DOI="10.24432/C5MK6M"
LABELS={1:"ethanol",2:"ethylene",3:"ammonia",4:"acetaldehyde",5:"acetone",6:"toluene"}
N_FEATURES=128
BATCH_COUNTS={1:445,2:1244,3:1586,4:161,5:197,6:2300,7:3613,8:294,9:470,10:3600}

def parse_original(path):
    path=Path(path)
    if hashlib.sha256(path.read_bytes()).hexdigest()!=ZIP_SHA256:
        raise ValueError("unrecognized or modified primary source ZIP")
    rows=[]
    sparse=collections.Counter()
    with zipfile.ZipFile(path) as z:
        files=z.namelist()
        expected={f"batch{i}.dat" for i in range(1,11)}
        if set(files)!=expected:
            raise ValueError("unexpected UCI270 file inventory")
        for batch in range(1,11):
            with io.TextIOWrapper(z.open(f"batch{batch}.dat"),encoding="utf-8-sig") as stream:
                n=0
                for line in stream:
                    line=line.strip()
                    if not line:continue
                    tokens=line.split()
                    h=tokens[0].split(";")
                    if len(h)!=2: raise ValueError("invalid label/ppmv row header")
                    label=int(h[0])
                    concentration=float(h[1])
                    if label not in LABELS or not math.isfinite(concentration) or concentration<=0:
                        raise ValueError("invalid gas and ppm")
                    vec=[0.]*N_FEATURES
                    seen=set()
                    for token in tokens[1:]:
                        p=token.split(":")
                        if len(p)!=2:raise ValueError("invalid feature token")
                        k=int(p[0])
                        v=float(p[1])
                        if not 1<=k<=N_FEATURES or k in seen or not math.isfinite(v):
                            raise ValueError("invalid/duplicate feature index or finite feature")
                        vec[k-1]=v;seen.add(k)
                    sparse[len(seen)]+=1
                    if not seen:raise ValueError("empty feature set")
                    rows.append({"batch":batch,"within_batch_row":n,
                                 "source_row":len(rows),"class_id":label,
                                 "gas":LABELS[label],"ppm":concentration,
                                 "features":vec})
                    n+=1
                if n!=BATCH_COUNTS[batch]:
                    raise ValueError(f"unexpected batch {batch} count {n}")
    if len(rows)!=13910:
        raise AssertionError("expected 13,910 actual exposure records")
    return rows,{"total_exposure_records":len(rows),
                 "batch_sizes":{str(k):v for k,v in BATCH_COUNTS.items()},
                 "sparse_feature_counts":dict(sorted(sparse.items())),
                 "source_sha256":ZIP_SHA256,
                 "distinct_labels":sorted({r["gas"] for r in rows}),
                 "concentration_range_ppm":[min(r["ppm"] for r in rows),
                                            max(r["ppm"] for r in rows)],
                 "dataset_doi":DOI,
                 "n_derived_features_per_exposure":N_FEATURES,
                 "gas_label_mapping":LABELS,
                 "source_type":"precomputed 128-feature responses; 16 TGS sensor array",
                 "batch_warning":"Ten chronological batches do not imply ten independent devices/sessions."}

def select_sensor_features(row):
    """Choose one normalized DR-like source descriptor per each of 16 sensors.

    Feature index 2,10,...122 (zero-based 1,9,...121): supplied
    normalized-resistance-change descriptor. Do not identify this as MQ Rs/R0.
    """
    return [row["features"][8*i+1] for i in range(16)]
