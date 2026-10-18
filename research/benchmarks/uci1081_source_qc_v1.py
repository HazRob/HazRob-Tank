"""Read-only QC of user-provided UCI1081 gsalc.csv ZIP.

Schema/numerical QC only, no chemical identification or hardware calibration.
Does NOT include or publish third-party raw data. Python standard library only.
"""
import argparse
import collections
import csv
import hashlib
import io
import json
import math
import zipfile
from pathlib import Path

def audit(path):
    path=Path(path)
    counts=collections.Counter()
    lengths=collections.Counter()
    row_hashes=set()
    numeric=nonnumeric=nonfinite=0
    lo,hi=float("inf"),-float("inf")
    with zipfile.ZipFile(path) as z:
        with z.open("gsalc.csv") as binary:
            reader=csv.reader(io.TextIOWrapper(binary,encoding="utf-8"))
            for row in reader:
                lengths[len(row)]+=1
                if len(row)!=9002:
                    continue
                counts[tuple(x.strip() for x in row[:2])]+=1
                row_hashes.add(hashlib.sha256("\x00".join(row).encode()).hexdigest())
                for raw in row[2:]:
                    try:
                        v=float(raw)
                    except (ValueError,TypeError):
                        nonnumeric+=1
                        continue
                    if not math.isfinite(v):
                        nonfinite+=1
                        continue
                    numeric+=1
                    lo=min(lo,v)
                    hi=max(hi,v)
    return {
        "source_sha256":hashlib.sha256(path.read_bytes()).hexdigest(),
        "total_rows":sum(lengths.values()),
        "row_lengths":dict(sorted(lengths.items())),
        "unique_full_rows":len(row_hashes),
        "finite_numeric_values":numeric,
        "nonnumeric":nonnumeric,
        "nonfinite":nonfinite,
        "observed_min":lo if numeric else None,
        "observed_max":hi if numeric else None,
        "gas_concentration_counts":{
            f"{gas} | {conc}":n for (gas,conc),n in sorted(counts.items())
        },
        "scientific_warning":"Channel/time order and calibration metadata are not independently established by this CSV."
    }

if __name__=="__main__":
    p=argparse.ArgumentParser()
    p.add_argument("source_zip",help="original local archive; do not commit raw data")
    args=p.parse_args()
    print(json.dumps(audit(args.source_zip),ensure_ascii=False,indent=2))
