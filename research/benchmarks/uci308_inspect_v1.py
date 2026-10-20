"""Inspect official UCI308 archive without treating liquid percentages as ppm."""
import argparse,csv,gzip,hashlib,io,json,zipfile
from collections import Counter
from pathlib import Path
def read_csv(z,path,limit=None):
    if path.endswith(".gz"):
        stream=io.TextIOWrapper(gzip.GzipFile(fileobj=z.open(path)),encoding="utf-8-sig",newline="")
    else:
        stream=io.TextIOWrapper(z.open(path),encoding="utf-8-sig",newline="")
    with stream:
        reader=csv.reader(stream)
        header=next(reader)
        rows=[]
        for i,row in enumerate(reader):
            if limit is None or i<limit:
                rows.append(row)
            if limit is not None and i>=limit:
                break
    return header,rows
def inspect(path):
    p=Path(path)
    with zipfile.ZipFile(p) as z:
        names=z.namelist()
        print("ARCHIVE",p.name,"SHA256",hashlib.sha256(p.read_bytes()).hexdigest())
        print("FILES",json.dumps(names))
        out={"source_sha256":hashlib.sha256(p.read_bytes()).hexdigest(),"files":names}
        for name in names:
            if name.endswith(".csv") or name.endswith(".csv.gz"):
                header,rows=read_csv(z,name,limit=20)
                print("FILE",name,"COLUMNS",len(header),"ROWS_SAMPLED",len(rows))
                print("HEADERS",json.dumps(header[:24]))
                print("HEAD_FIRST",json.dumps(rows[0][:24] if rows else []))
                print("HEAD_SECOND",json.dumps(rows[1][:24] if len(rows)>1 else []))
                keys=[k for k in ("exp","batch","sensor","sample","ace_conc","eth_conc","lab","gas","col") if k in header]
                print("KEYS",json.dumps([{k:row[header.index(k)] for k in keys} for row in rows[:8]]))
                out[name]={"columns":len(header),"metadata_fields":keys,"row_length_sample":[len(v) for v in rows],"header_first":header[:24],"first_keys":[{k:r[header.index(k)] for k in keys} for r in rows[:8]]}
    return out
if __name__=="__main__":
    p=argparse.ArgumentParser();p.add_argument("archive");p.add_argument("--output",default="uci308_inspect.json")
    args=p.parse_args()
    Path(args.output).write_text(json.dumps(inspect(args.archive),indent=2,ensure_ascii=False)+"\n",encoding="utf-8")
