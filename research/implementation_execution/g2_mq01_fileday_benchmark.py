"""Exploratory original MQ-EXT-01 file/day holdout; NOT gas identification or G2.

Ten single-produce classes have nominal D1..D5 file labels. File/day holdout
is used solely as a reproducible source-domain stress test. The source does
not establish physical independent sessions or calibrated resistance units.
"""
import argparse
import csv
import io
import json
import math
import re
import statistics
import tempfile
import zipfile
from collections import Counter, defaultdict
from pathlib import Path

from g2_zenodo_mq01_raw_inventory import FILENAME, download_verified

ALL_MQ = ["MQ2", "MQ3", "MQ4", "MQ5", "MQ6", "MQ7", "MQ8", "MQ9", "MQ135"]
TANK_MQ = ["MQ2", "MQ3", "MQ4", "MQ5", "MQ7", "MQ8", "MQ9", "MQ135"]
SINGLE = re.compile(r"^([^/]+) D([1-5])\.csv$", re.IGNORECASE)

def read_file_features(zf, name):
    values = {c: [] for c in ALL_MQ}
    with zf.open(name) as raw:
        with io.TextIOWrapper(raw, encoding="utf-8-sig", errors="strict", newline="") as fp:
            reader = csv.DictReader(fp)
            if not set(ALL_MQ).issubset(set(reader.fieldnames or [])):
                raise ValueError("MQ_COLUMNS_MISSING: " + name)
            n = 0
            for row in reader:
                n += 1
                for c in ALL_MQ:
                    value = float(row[c])
                    if not math.isfinite(value):
                        raise ValueError("NONFINITE_SOURCE_SIGNAL: " + name)
                    values[c].append(value)
    if n < 2:
        raise ValueError("TOO_FEW_SOURCE_READINGS: " + name)
    return {"rows": n, "medians": {c: statistics.median(values[c]) for c in ALL_MQ}}

def load_source(archive):
    records = []
    with zipfile.ZipFile(archive) as zf:
        for name in sorted(zf.namelist()):
            match = SINGLE.match(Path(name).name)
            if not match:
                continue
            fruit, day = match.groups()
            record = {"file": name, "label": fruit.strip().lower(),
                      "day": int(day), **read_file_features(zf, name)}
            records.append(record)
    labels = sorted({r["label"] for r in records})
    if len(records) != 50 or len(labels) != 10 or any(
            {r["day"] for r in records if r["label"] == label} != {1, 2, 3, 4, 5}
            for label in labels):
        raise ValueError("UNEXPECTED_SOURCE_FILE_DAY_GROUPING")
    return records

def predict_one(train, test, channels):
    # Fit mean and population std on training files only (never held-out day).
    means = {c: statistics.mean(r["medians"][c] for r in train) for c in channels}
    scales = {c: statistics.pstdev(r["medians"][c] for r in train) or 1.0 for c in channels}
    def dist(a, b):
        return sum(((a["medians"][c] - b["medians"][c]) / scales[c]) ** 2
                   for c in channels)
    return min(train, key=lambda r: (dist(r, test), r["file"]))["label"]

def evaluate(records, channels):
    predictions = []
    for day in range(1, 6):
        train = [r for r in records if r["day"] != day]
        test = [r for r in records if r["day"] == day]
        assert len(train) == 40 and len(test) == 10
        for item in test:
            prediction = predict_one(train, item, channels)
            predictions.append({"file": item["file"], "day": day,
                                "actual": item["label"], "predicted": prediction,
                                "correct": prediction == item["label"]})
    correct = sum(x["correct"] for x in predictions)
    return {"channels": channels, "n_test_files": len(predictions), "correct": correct,
            "accuracy": correct / len(predictions), "per_day": [
                {"day": day, "correct": sum(x["correct"] for x in predictions if x["day"] == day),
                 "total": 10} for day in range(1, 6)],
            "per_label": [
                {"label": label, "correct": sum(x["correct"] for x in predictions if x["actual"] == label),
                 "total": 5} for label in sorted({x["actual"] for x in predictions})],
            "predictions": predictions}

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--output-dir", required=True)
    args = ap.parse_args()
    output = Path(args.output_dir)
    output.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        archive = Path(tmp) / FILENAME
        source = download_verified(archive)
        records = load_source(archive)
    experiments = [evaluate(records, TANK_MQ), evaluate(records, ALL_MQ)]
    result = {"schema": "hazrob.g2.mq01.exploratory-fileday-v1",
              "source_archive_sha256": source["sha256"],
              "source_md5_verified": True,
              "task": "single-produce identity, not gas identity or ppm",
              "split": "hold out all Dn files from all 10 classes for each n=1..5",
              "grouping_verified_as_independent_physical_session": False,
              "source_signal_units_verified": False,
              "r0_mode": "NOT_USED",
              "preprocessing": "per-file channel medians; train-only z-score",
              "model": "1NN standardized Euclidean; no tuning",
              "baselines": {"majority_class_accuracy": 0.1},
              "experiments": experiments,
              "g2_qualified": False, "g3_tank": False, "safety_grade": False}
    (output / "source_file_features.json").write_text(json.dumps(records, indent=2) + "\n")
    (output / "fileday_results.json").write_text(json.dumps(result, indent=2) + "\n")
    print(json.dumps({"archive_sha256": source["sha256"],
                      "n_files": len(records),
                      "experiments": [{"channels": len(x["channels"]),
                                       "correct": x["correct"], "total": x["n_test_files"]}
                                      for x in experiments],
                      "g2_qualified": False}))

if __name__ == "__main__":
    main()
