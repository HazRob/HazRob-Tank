"""Reproduce beef 2018 study-class research templates from preserved raw CSV.

The source classes come from the dataset, not a device-measured TVC. An
evaluation on TS5 (one held-out cut) is NOT Tank model validation.
"""
import csv
import hashlib
import json
import math
import statistics
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
MEAT=ROOT/"research/food_freshness/meat"
BASE=MEAT/"source_original"
PROTOTYPES=MEAT/"wijaya2018_5series_7mq_class_prototypes.json"
MODELS=("MQ2","MQ3","MQ4","MQ5","MQ8","MQ9","MQ135")
LABELS=("excellent","good","acceptable","spoiled")

def read(path):
    with path.open(newline="") as f:
        raw=list(csv.DictReader(f))
    assert len(raw)==2160
    result=[]
    for row in raw:
        v=[float(row[k]) for k in MODELS]
        if any(not math.isfinite(x) or x<=0 for x in v):
            continue
        logs=[math.log10(x) for x in v]
        mean=sum(logs)/len(logs)
        result.append((row["class"].lower(),[x-mean for x in logs],float(row["TVC"])))
    return result

def sha1_gitblob(data):
    return hashlib.sha1(b"blob "+str(len(data)).encode()+b"\0"+data).hexdigest()

class BeefStudyModelReproduction(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.catalog=json.loads(PROTOTYPES.read_text(encoding="utf-8"))
        cls.rows={f"TS{i}.csv":read(BASE/f"TS{i}.csv") for i in range(1,6)}

    def test_raw_file_count_and_source_sha_are_pinned(self):
        self.assertEqual(set(self.rows),set(x["name"] for x in self.catalog["provenance"]["mirror_files"]))
        for entry in self.catalog["provenance"]["mirror_files"]:
            path=BASE/entry["name"]
            self.assertEqual(sha1_gitblob(path.read_bytes()),entry["git_blob_sha1"])
            self.assertEqual(len(self.rows[entry["name"]]),entry["rows"])
        self.assertEqual(sum(map(len,self.rows.values())),10800)

    def test_all_training_class_medians_reproduce_precisely(self):
        train=sum((self.rows[f"TS{i}.csv"] for i in range(1,5)),[])
        assert len(train)==8640
        for source in self.catalog["prototypes"]:
            data=[(v,tvc) for label,v,tvc in train if label==source["label"]]
            self.assertEqual(source["row_count"],len(data))
            for index,expected in enumerate(source["median_log_shape"]):
                self.assertAlmostEqual(statistics.median(x[index] for x,_ in data),expected,places=12)
            self.assertAlmostEqual(statistics.median(tvc for _,tvc in data),
                                   source["tvc_median"],places=12)

    def test_frozen_heldout_cut_5_accuracy_not_exaggerated(self):
        profiles=self.catalog["prototypes"]
        confusion={a:{b:0 for b in LABELS} for a in LABELS}
        for actual,v,_ in self.rows["TS5.csv"]:
            predicted=min(profiles,key=lambda p:math.sqrt(sum(
                (x-y)**2 for x,y in zip(v,p["median_log_shape"]))/len(v)))["label"]
            confusion[actual][predicted]+=1
        expected=self.catalog["holdout_results"]
        self.assertEqual(confusion,expected["confusion"])
        n=sum(sum(row.values()) for row in confusion.values())
        accuracy=sum(confusion[c][c] for c in LABELS)/n
        recalls=[confusion[c][c]/sum(confusion[c].values()) for c in LABELS]
        balance=sum(recalls)/len(recalls)
        self.assertAlmostEqual(accuracy,expected["accuracy"],places=12)
        self.assertAlmostEqual(balance,expected["balanced_accuracy"],places=12)
        self.assertLess(balance,0.4)
        self.assertFalse(self.catalog["claims"]["food_model_separate_from_gas_chemcore"] is False)
        self.assertFalse(self.catalog["claims"]["meat_microbiological_safety_certified"])

if __name__=="__main__":
    unittest.main()
