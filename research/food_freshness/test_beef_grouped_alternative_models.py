"""Food-only source robustness: five held-out beef cuts and alternate MQ models.
Research diagnostics, NOT a validation of HazRob Tank or food-edibility detection.
Never tune or select a final classifier using the same holdout as its test.
"""
import csv,math,statistics,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
SOURCE=ROOT/"research/food_freshness/meat/source_original"
SENSORS=("MQ2","MQ3","MQ4","MQ5","MQ8","MQ9","MQ135")
LABELS=("excellent","good","acceptable","spoiled")
def read_cut(k):
    out=[]
    with (SOURCE/("TS%d.csv"%k)).open(newline="") as handle:
        for row in csv.DictReader(handle):
            raw=[float(row[k]) for k in SENSORS]
            if not all(x>0 and math.isfinite(x) for x in raw):continue
            logs=[math.log10(x) for x in raw]
            mid=sum(logs)/len(logs)
            out.append((row["class"].lower(),[x-mid for x in logs]))
    assert len(out)==2160
    return out
def train(source,method):
    classes={}
    for label in LABELS:
        values=[v for code,v in source if code==label]
        if not values:continue
        center=[(statistics.median(col) if method=="median" else statistics.mean(col))
                for col in zip(*values)]
        var=[statistics.mean((v[i]-center[i])**2 for v in values) for i in range(7)]
        classes[label]=(center,var)
    return classes
def predict(x,centers,method):
    def cost(row):
        label,(c,v)=row
        if method=="gaussian_global":
            return sum((x[i]-c[i])**2 for i in range(7))
        if method=="gaussian_diagonal":
            # Fixed ridge prevents numerical blow-ups; no per-test-fold tuning.
            return sum((x[i]-c[i])**2/max(0.01,v[i])+
                       math.log(max(0.01,v[i])) for i in range(7))
        return sum((x[i]-c[i])**2 for i in range(7))
    return min(centers.items(),key=cost)[0]
def score(train_set,test,method):
    centers=train(train_set,method)
    matrix={l:{p:0 for p in LABELS} for l in LABELS}
    for actual,x in test:
        matrix[actual][predict(x,centers,method)]+=1
    n=sum(sum(row.values()) for row in matrix.values())
    acc=sum(matrix[c][c] for c in LABELS)/n
    balance=sum(matrix[c][c]/sum(matrix[c].values()) for c in LABELS)/4
    return acc,balance
class BeefGroupedExperimentalModels(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.cuts={i:read_cut(i) for i in range(1,6)}
    def test_all_study_cuts_hold_out_entire_cut(self):
        results={m:[] for m in ("median","mean","gaussian_diagonal","gaussian_global")}
        for held in range(1,6):
            training=[item for k,rows in self.cuts.items() if k!=held for item in rows]
            test=self.cuts[held]
            self.assertEqual(len(training),8640)
            self.assertEqual(len(test),2160)
            for model in results:
                res=score(training,test,model)
                self.assertTrue(all(0<=value<=1 and math.isfinite(value) for value in res))
                results[model].append(res)
        for model,rows in results.items():
            print("BEEF GROUP HELDOUT SOURCE %s: per_cut=%s mean_acc=%.4f mean_bal_acc=%.4f"%
                  (model,[(round(a,4),round(b,4)) for a,b in rows],
                   statistics.mean(a for a,b in rows),
                   statistics.mean(b for a,b in rows)))
        # Four-class source domain is fragile even within original apparatus.
        # Do NOT assert that a model beats the published independent sample benchmark.
if __name__=="__main__":unittest.main()
