"""Source-only, leave-one-study-day-out fruit reference benchmark.
Source unit: 45 reference day profiles from 9 families, NOT 45 specimens or
independent Tank food exposures. The same original samples contribute repeated days.
No gas ChemCore import, no food edibility/ripeness/TVC claim.
"""
import json,math,unittest,re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
SOURCE=ROOT/"research/food_freshness/source_snapshots/food_freshness_headspace_fingerprints_v1.json"
def parse():
    j=json.loads(SOURCE.read_text(encoding="utf-8"))
    out=[]
    for label,v in j["sessions"]:
        m=re.search(r"\s+D([1-9][0-9]*)$",label,re.I)
        family=re.sub(r"\s+D[1-9][0-9]*$","",label,flags=re.I).strip().lower()
        if family=="mushroom":continue
        out.append(dict(label=label,vector=v,family=family,day=int(m.group(1)) if m else 0))
    return out
def distance(a,b,channels=range(8)):
    x=[math.log10(a["vector"][i]) for i in channels]
    y=[math.log10(b["vector"][i]) for i in channels]
    ax=sum(x)/len(x);ay=sum(y)/len(y)
    return math.sqrt(sum(((p-ax)-(q-ay))**2 for p,q in zip(x,y))/len(x))
def benchmark():
    records=parse()
    results=[]
    for q in (p for p in records if p["day"]):
        same=[p for p in records if p is not q and p["day"] and p["family"]==q["family"]]
        global_matches=[p for p in records if p is not q]
        neighbor=min(same,key=lambda p:(distance(q,p),p["label"]))
        other=min(global_matches,key=lambda p:(distance(q,p),p["label"]))
        results.append(dict(
            reference=q["label"],same_family_neighbor=neighbor["label"],
            day_error=abs(q["day"]-neighbor["day"]),
            identified_family=other["family"]==q["family"]))
    return records,results
class FruitLeaveDayOut(unittest.TestCase):
    def test_source_only_diagnostic_not_taxonomic_claim(self):
        records,rows=benchmark()
        self.assertEqual(len(records),54)
        self.assertEqual(len(rows),45)
        self.assertEqual(len(set(x["family"] for x in records if x["day"])),9)
        self.assertEqual(sum(x["day_error"]<=1 for x in rows),41)
        self.assertEqual(sum(x["day_error"] for x in rows),49)
        self.assertEqual(sum(x["identified_family"] for x in rows),5)
        print("FOOD SOURCE ONLY: known-fruit neighbor within one source day 41/45 (91.11%);")
        print("unknown-fruit identity among all families only 5/45 (11.11%);")
        print("therefore require declared food type; NEVER infer edibility or actual age")
    def test_scale_invariance_matches_source_model(self):
        rows=parse()
        for src in rows:
            scaled=dict(vector=[x*1000 for x in src["vector"]])
            self.assertAlmostEqual(distance(src,scaled),0,places=12)
if __name__=="__main__":unittest.main()
