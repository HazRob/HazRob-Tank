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
import random
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
    if (not channels or len(set(channels)) != len(channels) or
            any(c not in ALL_MQ for c in channels)):
        raise ValueError('INVALID_CHANNEL_SELECTION')
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

def permutation_negative_control(records, channels, n_permutations=1999, seed=20261026):
    """Conditional *descriptive* null, shuffling class labels within filename-day.

    1NN nearest-file indices are computed from the measured features and
    training-day files only; labels are never used to choose neighbours.
    This cannot establish independent physical sessions or a valid p-value
    for generalization beyond the archive.
    """
    if not isinstance(n_permutations, int) or n_permutations < 1:
        raise ValueError("INVALID_PERMUTATION_COUNT")
    by_day = {day: sorted((r for r in records if r["day"] == day),
                          key=lambda r: r["file"]) for day in range(1, 6)}
    if any(len(items) != 10 or len({r["label"] for r in items}) != 10
           for items in by_day.values()):
        raise ValueError("UNBALANCED_FILEDAY_PERMUTATION_STRATA")
    if len({r["file"] for r in records}) != len(records):
        raise ValueError("DUPLICATE_SOURCE_FILE_ID")
    neighbours = {}
    for day, heldout in by_day.items():
        train = [r for r in records if r["day"] != day]
        means = {c: statistics.mean(r["medians"][c] for r in train) for c in channels}
        scales = {c: statistics.pstdev(r["medians"][c] for r in train) or 1.0 for c in channels}
        for test in heldout:
            closest = min(train, key=lambda r: (
                sum(((r["medians"][c] - test["medians"][c]) / scales[c]) ** 2
                    for c in channels), r["file"]))
            neighbours[test["file"]] = closest["file"]
    labels = {r["file"]: r["label"] for r in records}
    observed = sum(labels[f] == labels[n] for f, n in neighbours.items())
    rng = random.Random(seed)
    null_counts = []
    for _ in range(n_permutations):
        permuted = {}
        for day, items in by_day.items():
            values = [r["label"] for r in items]
            rng.shuffle(values)
            permuted.update({r["file"]: value for r, value in zip(items, values)})
        null_counts.append(sum(permuted[f] == permuted[n] for f, n in neighbours.items()))
    ordered = sorted(null_counts)
    return {"source_files": len(records), "observed_correct": observed,
            "permutations": n_permutations, "seed": seed,
            "null_mean_correct": sum(null_counts) / n_permutations,
            "null_median_correct": ordered[n_permutations // 2],
            "null_p95_correct": ordered[int(0.95 * (n_permutations - 1))],
            "null_ge_observed": sum(x >= observed for x in null_counts),
            "conditional_tail_fraction_plus_one": (
                1 + sum(x >= observed for x in null_counts)) / (n_permutations + 1),
            "inference_scope": "DESCRIPTIVE_FILEDAY_LABEL_RANDOMIZATION_ONLY",
            "independent_session_significance_claim": False}


def paired_channel_comparison(first, second):
    """Report paired 8-vs-9 correctness, not two independent proportions."""
    left = {r["file"]: r["correct"] for r in first["predictions"]}
    right = {r["file"]: r["correct"] for r in second["predictions"]}
    if left.keys() != right.keys():
        raise ValueError("UNMATCHED_PAIRED_TEST_FILES")
    return {"both_correct": sum(left[k] and right[k] for k in left),
            "only_first_correct": sum(left[k] and not right[k] for k in left),
            "only_second_correct": sum(right[k] and not left[k] for k in left),
            "both_wrong": sum(not left[k] and not right[k] for k in left),
            "comparison": "PAIRED_DESCRIPTIVE_NOT_INDEPENDENT_SAMPLES"}


def prespecified_channel_sets():
    """Fixed source-domain ablations; no held-out-driven channel selection."""
    sets = [
        {"id": "TANK8", "channels": list(TANK_MQ)},
        {"id": "TANK6_EXCLUDE_MQ7_MQ9",
         "channels": [c for c in TANK_MQ if c not in ("MQ7", "MQ9")]},
        {"id": "MQ7_MQ9_ONLY", "channels": ["MQ7", "MQ9"]},
        {"id": "SOURCE9", "channels": list(ALL_MQ)},
    ]
    sets.extend({"id": "DROP_" + c,
                 "channels": [x for x in TANK_MQ if x != c]} for c in TANK_MQ)
    assert len(sets) == 12 and len({x["id"] for x in sets}) == 12
    return sets


def ablation_comparison(experiments):
    """Paired file-level contrasts; not independent-session inference."""
    by_id = {x["config_id"]: x for x in experiments}
    baseline = by_id["TANK8"]
    return [
        {"removed_sensor": c,
         "tank8_correct": baseline["correct"],
         "without_correct": by_id["DROP_" + c]["correct"],
         "delta_correct": by_id["DROP_" + c]["correct"] - baseline["correct"],
         "paired": paired_channel_comparison(baseline, by_id["DROP_" + c])}
        for c in TANK_MQ
    ]


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
    experiments = [dict(config_id=spec["id"], **evaluate(records, spec["channels"]))
                   for spec in prespecified_channel_sets()]
    controls = [permutation_negative_control(records, channels)
                for channels in (TANK_MQ, ALL_MQ)]
    paired = paired_channel_comparison(experiments[0], experiments[3])
    ablations = ablation_comparison(experiments)
    assert controls[0]["observed_correct"] == experiments[0]["correct"]
    assert controls[1]["observed_correct"] == experiments[3]["correct"]
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
              "permutation_controls": controls,
              "paired_channel_comparison": paired,
              "prespecified_sensor_ablations": ablations,
              "ablation_scope": "DESCRIPTIVE_SOURCE_FILE_ONLY_NOT_GAS_OR_SAFETY",
              "model_selected_using_heldout_scores": False,
              "g2_qualified": False, "g3_tank": False, "safety_grade": False}
    (output / "source_file_features.json").write_text(json.dumps(records, indent=2) + "\n")
    (output / "fileday_results.json").write_text(json.dumps(result, indent=2) + "\n")
    print(json.dumps({"archive_sha256": source["sha256"],
                      "n_files": len(records),
                      "experiments": [{"channels": len(x["channels"]),
                                       "correct": x["correct"], "total": x["n_test_files"]}
                                      for x in experiments],
                      "permutation_controls": [{"observed_correct": x["observed_correct"],
                                                "null_p95_correct": x["null_p95_correct"],
                                                "conditional_tail_fraction_plus_one": x["conditional_tail_fraction_plus_one"]}
                                               for x in controls],
                      "paired": paired,
                      "g2_qualified": False}))

if __name__ == "__main__":
    main()
