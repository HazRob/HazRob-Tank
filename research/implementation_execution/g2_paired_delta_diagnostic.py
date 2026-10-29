"""Research-only paired cluster bootstrap for model-minus-baseline accuracy.

Resample the same independent test-group indices for model and baseline.
A marginal accuracy CI or row bootstrap cannot substitute for a paired
cluster-level comparison. This is arithmetic, not G2/G3 qualification.
"""
import argparse
import csv
import hashlib
import io
import json
import math
import random
import unicodedata
from pathlib import Path

from g2_cluster_bootstrap_gate import _linear_quantile


def _canonical_group_id(value):
    return (isinstance(value, str) and bool(value.strip()) and
            value == value.strip() and
            unicodedata.normalize("NFKC", value) == value and
            not any(unicodedata.category(c).startswith("C") or
                    unicodedata.category(c) in ("Zl", "Zp") or
                    (unicodedata.category(c) == "Zs" and c != " ")
                    for c in value))


def paired_cluster_accuracy_delta(group_counts, model_correct, baseline_correct,
                                  *, seed=20261029, resamples=2000,
                                  confidence_level=0.95):
    """Return a deterministic paired group bootstrap diagnostic.

    The inputs are per-independent-physical-test-group integer counts. All
    three dictionaries must have identical group keys; no row-level or
    unpaired resampling is performed. No significance or certification claim
    follows from the interval without an independently verified design.
    """
    if (type(seed) is not int or not 0 <= seed < 2**32 or
            type(resamples) is not int or not 1000 <= resamples <= 100000 or
            type(confidence_level) not in (int, float) or
            not math.isfinite(confidence_level) or
            confidence_level != 0.95):
        raise ValueError("PAIRED_DELTA_PROTOCOL_INVALID")
    if (not isinstance(group_counts, dict) or
            not isinstance(model_correct, dict) or
            not isinstance(baseline_correct, dict) or
            len(group_counts) < 2 or
            set(group_counts) != set(model_correct) or
            set(group_counts) != set(baseline_correct) or
            any(not _canonical_group_id(g) for g in group_counts)):
        raise ValueError("PAIRED_DELTA_PHYSICAL_GROUPS_INVALID")
    ids = sorted(group_counts)
    for gid in ids:
        n, model, baseline = (group_counts[gid], model_correct[gid],
                              baseline_correct[gid])
        if (type(n) is not int or n < 1 or
                type(model) is not int or not 0 <= model <= n or
                type(baseline) is not int or not 0 <= baseline <= n):
            raise ValueError("PAIRED_DELTA_COUNTS_INVALID")
    counts = [group_counts[g] for g in ids]
    differences = [model_correct[g] - baseline_correct[g] for g in ids]
    total = sum(counts)
    observed = sum(differences) / total
    # Report how much the pooled estimate depends on group size and any
    # single physical test group. These diagnostics do not certify that the
    # declared groups are independent or that the holdout was blinded.
    model_accuracy = sum(model_correct[g] for g in ids) / total
    baseline_accuracy = sum(baseline_correct[g] for g in ids) / total
    group_deltas = [d / n for d, n in zip(differences, counts)]
    leave_one_group_out = [
        (sum(differences) - d) / (total - n)
        for d, n in zip(differences, counts)
    ]
    effective_group_count = total * total / sum(n * n for n in counts)
    # Equal-group and exposure-weighted estimands answer different questions.
    # A direction reversal is a weighting sensitivity warning, not evidence
    # of model superiority or a formal Simpson's-paradox test.
    equal_group_mean_delta = math.fsum(group_deltas) / len(group_deltas)
    weighting_direction_disagreement = (observed * equal_group_mean_delta < 0)
    max_loo_shift = max(abs(value - observed) for value in leave_one_group_out)
    loo_direction_reversals = sum(observed * value < 0 for value in leave_one_group_out)
    rng = random.Random(seed)
    draws = []
    for _ in range(resamples):
        chosen = [rng.randrange(len(ids)) for _ in ids]
        draws.append(sum(differences[i] for i in chosen) /
                     sum(counts[i] for i in chosen))
    nonpositive_fraction = sum(v <= 0 for v in draws) / resamples
    draws.sort()
    tail = (1.0 - confidence_level) / 2.0
    return {
        "schema": "hazrob.g2.paired-cluster-delta-diagnostic.v1",
        "scope": "RESEARCH_ARITHMETIC_ONLY_NOT_G2_OR_G3_APPROVAL",
        "method": "PAIRED_GROUP_BOOTSTRAP",
        "resampling_unit": "independent_test_group_id",
        "statistic": "pooled_model_accuracy_minus_pooled_baseline_accuracy",
        "groups": len(ids),
        "test_exposures": total,
        "seed": seed,
        "resamples": resamples,
        "confidence_level": confidence_level,
        "quantile_method": "linear",
        "observed_delta": observed,
        "model_accuracy": model_accuracy,
        "equal_physical_group_mean_delta": equal_group_mean_delta,
        "exposure_vs_equal_group_delta_gap": observed - equal_group_mean_delta,
        "exposure_vs_equal_group_direction_disagreement": weighting_direction_disagreement,
        "leave_one_group_out_max_absolute_shift": max_loo_shift,
        "leave_one_group_out_direction_reversal_count": loo_direction_reversals,
        "baseline_accuracy": baseline_accuracy,
        "group_positive_delta_count": sum(d > 0 for d in differences),
        "group_negative_delta_count": sum(d < 0 for d in differences),
        "group_tied_delta_count": sum(d == 0 for d in differences),
        "group_delta_min": min(group_deltas),
        "group_delta_max": max(group_deltas),
        "leave_one_group_out_delta_min": min(leave_one_group_out),
        "leave_one_group_out_delta_max": max(leave_one_group_out),
        "largest_group_exposure_fraction": max(counts) / total,
        "kish_effective_group_count_descriptive_only": effective_group_count,
        "interval_low": _linear_quantile(draws, tail),
        "interval_high": _linear_quantile(draws, 1.0 - tail),
        "bootstrap_nonpositive_fraction_not_p_value": nonpositive_fraction,
        "caveat": "Group independence, blind labels, physical traceability and statistical power require external evidence. Not a hypothesis-test p-value or scientific approval.",
    }


def replay_per_group_csv(path, *, seed=20261029, resamples=2000):
    """Replay per-group accuracy deltas from an existing G2 metrics CSV.

    The SHA256 identifies the exact CSV bytes, not their physical authenticity.
    This helper is deliberately separate from the scientific promotion gate.
    """
    path = Path(path)
    raw = path.read_bytes()
    with io.StringIO(raw.decode("utf-8"), newline="") as stream:
        reader = csv.DictReader(stream, strict=True)
        header = reader.fieldnames
        required = {"group_id", "sample_count", "correct_count", "baseline_correct_count"}
        if (not header or len(set(header)) != len(header) or
                any(not _canonical_group_id(name) for name in header) or
                not required.issubset(set(header))):
            raise ValueError("PAIRED_DELTA_CSV_HEADER_INVALID")
        counts, model, baseline = {}, {}, {}
        for row in reader:
            if (None in row or any(value is None for value in row.values()) or
                    not _canonical_group_id(row["group_id"]) or
                    row["group_id"] in counts):
                raise ValueError("PAIRED_DELTA_CSV_ROW_INVALID")
            gid = row["group_id"]
            try:
                numbers = [int(row[field]) for field in
                           ("sample_count", "correct_count", "baseline_correct_count")]
            except (ValueError, TypeError) as exc:
                raise ValueError("PAIRED_DELTA_CSV_ROW_INVALID") from exc
            if any(str(value) != row[field] for field, value in zip(
                    ("sample_count", "correct_count", "baseline_correct_count"), numbers)):
                raise ValueError("PAIRED_DELTA_CSV_ROW_INVALID")
            counts[gid], model[gid], baseline[gid] = numbers
    result = paired_cluster_accuracy_delta(counts, model, baseline,
                                           seed=seed, resamples=resamples)
    result["source_csv_sha256"] = hashlib.sha256(raw).hexdigest()
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--per-group-csv", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--seed", type=int, default=20261029)
    parser.add_argument("--resamples", type=int, default=2000)
    args = parser.parse_args()
    result = replay_per_group_csv(args.per_group_csv,
                                  seed=args.seed, resamples=args.resamples)
    args.output.write_text(json.dumps(result, sort_keys=True, indent=2) + "\n",
                           encoding="utf-8")
    print("Paired group delta arithmetic complete; NOT scientific G2/G3 approval")


if __name__ == "__main__":
    main()
