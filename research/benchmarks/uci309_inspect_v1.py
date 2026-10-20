"""Source inspection only: verify 180 UCI309 raw and 180 downsample files."""
import argparse,collections,hashlib,io,json,re,zipfile
from pathlib import Path
def inspect(path):
    p=Path(path);sha=hashlib.sha256(p.read_bytes()).hexdigest()
    with zipfile.ZipFile(p) as z:
        names=[n for n in z.namelist() if not n.endswith("/")]
        counts=collections.Counter(n.split("/")[0] for n in names)
        preview=[]
        for name in [next((n for n in names if "downsample" in n.lower()),None),
                     next((n for n in names if "_raw/" in n.lower()),None)]:
            if name:
                with z.open(name) as f:
                    lines=[]
                    for _ in range(5):
                        b=f.readline()
                        if not b:break
                        lines.append(b.decode("utf-8-sig",errors="replace").strip()[:500])
                    preview.append({"name":name,"size":z.getinfo(name).file_size,"first_lines":lines})
        return {"dataset":"UCI309","sha256":sha,"n_files":len(names),"top_level_counts":dict(counts),
                "first_names":names[:12],"last_names":names[-12:],
                "samples":preview,"size_zip_bytes":p.stat().st_size}
if __name__=="__main__":
  import argparse
  a=argparse.ArgumentParser();a.add_argument("path");args=a.parse_args()
  print(json.dumps(inspect(args.path),indent=2,ensure_ascii=False))
