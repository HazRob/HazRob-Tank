"""Deterministic grouped bootstrap arithmetic for G2 classification accuracy.

Software integrity check only: it cannot establish original physical groups,
true gas identity, independent reference calibration or valid statistical power.
"""
import math
import random


def _linear_quantile(sorted_values, probability):
    position = (len(sorted_values) - 1) * probability
    lower = math.floor(position)
    upper = math.ceil(position)
    weight = position - lower
    return (1.0 - weight) * sorted_values[lower] + weight * sorted_values[upper]


def audit_group_bootstrap_accuracy(group_counts, group_correct, interval):
    """Recompute a pooled-accuracy cluster bootstrap, never a row bootstrap.

    Every draw resamples whole test groups with replacement. For each selected
    group, both its numerator and denominator are included. Sorted group IDs
    make the result invariant to CSV row ordering. Returns machine-readable
    blocking issues rather than a claim of scientific qualification.
    """
    if not isinstance(interval, dict) or interval.get("method") != "GROUP_BOOTSTRAP":
        return ["CLASSIFICATION_INTERVAL_METHOD_NOT_RECOMPUTABLE"]
    seed = interval.get("seed")
    resamples = interval.get("resamples")
    level = interval.get("confidence_level")
    if (isinstance(seed, bool) or not isinstance(seed, int) or not 0 <= seed < 2**32
            or isinstance(resamples, bool) or not isinstance(resamples, int)
            or not 1000 <= resamples <= 100000
            or isinstance(level, bool) or not isinstance(level, (float, int))
            or not math.isfinite(level) or level != 0.95
            or interval.get("resampling_unit") != "group_id"
            or interval.get("statistic") != "pooled_accuracy"
            or interval.get("quantile_method") != "linear"):
        return ["GROUP_BOOTSTRAP_PROTOCOL_INVALID"]
    ids = sorted(group_counts)
    if (len(ids) < 2 or set(group_correct) - set(ids) or
            any(isinstance(group_counts[g], bool) or not isinstance(group_counts[g], int)
                or group_counts[g] <= 0 or
                isinstance(group_correct.get(g, 0), bool)
                or not isinstance(group_correct.get(g, 0), int)
                or not 0 <= group_correct.get(g, 0) <= group_counts[g] for g in ids)):
        return ["GROUP_BOOTSTRAP_GROUP_COUNTS_INVALID"]
    counts = [group_counts[g] for g in ids]
    correct = [group_correct.get(g, 0) for g in ids]
    generator = random.Random(seed)
    samples = []
    for _ in range(resamples):
        indices = [generator.randrange(len(ids)) for _ in ids]
        numerator = sum(correct[i] for i in indices)
        denominator = sum(counts[i] for i in indices)
        samples.append(numerator / denominator)
    samples.sort()
    low = _linear_quantile(samples, (1 - level) / 2)
    high = _linear_quantile(samples, 1 - (1 - level) / 2)
    if any(isinstance(interval.get(key), bool) or
           not isinstance(interval.get(key), (int, float)) or
           not math.isfinite(interval[key]) or
           not math.isclose(interval[key], target, abs_tol=1e-12, rel_tol=1e-12)
           for key, target in (("low", low), ("high", high))):
        return ["GROUP_BOOTSTRAP_INTERVAL_RECOMPUTATION_MISMATCH"]
    return []
