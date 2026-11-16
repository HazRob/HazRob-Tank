"""Research-only exact disagreement bounds from pinned MQ4 marginal confusion matrices.

No paired prediction rows exist here. Bounds are hypothetical conditional on a
common labelled row universe; never a measured McNemar test or G2 evidence.
"""
import argparse
import itertools
import json
from pathlib import Path

from g2_mq4_pairwise_frechet_bounds import (
    EVIDENCE, N, NAMES, P, SOURCE, project, strict_json,
)


def disagreement_bounds(a_errors, b_errors, population):
    """Sharp range of binary error disagreement counts on common rows."""
    if any(type(x) is not int for x in (a_errors, b_errors, population)):
        raise ValueError("BAD_INTEGER_COUNTS")
    if population <= 0 or not (0 <= a_errors <= population and 0 <= b_errors <= population):
        raise ValueError("BAD_ERROR_COUNTS")
    low = abs(a_errors - b_errors)
    high = min(a_errors + b_errors, 2 * population - a_errors - b_errors)
    if (high - low) % 2:
        raise AssertionError("DISCORDANCE_PARITY")
    return [low, high]


def derive(source_raw, evidence_raw):
    # Verify SHA256, scope, model identities, denominators and strict records=[].
    project(source_raw, evidence_raw)
    source = strict_json(source_raw)
    models = source["models"]
    pairs = []
    for i, j in itertools.combinations(range(len(NAMES)), 2):
        a, b = models[i], models[j]
        afn, bfn = a["total_4ppm_false_negative"], b["total_4ppm_false_negative"]
        afp, bfp = a["total_4ppm_false_positive"], b["total_4ppm_false_positive"]
        positive = disagreement_bounds(afn, bfn, P)
        negative = disagreement_bounds(afp, bfp, N)
        total = [positive[0] + negative[0], positive[1] + negative[1]]
        delta = (bfn + bfp) - (afn + afp)
        # B-only error = A correct, B wrong; A-only error = A wrong, B correct.
        b_only = [(d + delta) // 2 for d in total]
        a_only = [(d - delta) // 2 for d in total]
        if any((d + delta) % 2 for d in total) or min(*b_only, *a_only) < 0:
            raise AssertionError("INVALID_PAIRING_BOUNDS")
        pairs.append({
            "models": [NAMES[i], NAMES[j]],
            "positive_row_disagreement_range": positive,
            "negative_row_disagreement_range": negative,
            "total_disagreement_range": total,
            "a_wrong_b_correct_range": a_only,
            "a_correct_b_wrong_range": b_only,
            "b_minus_a_total_errors": delta,
            "paired_predictions_available": False,
            "paired_statistical_test_permitted": False,
        })
    return {
        "schema": "hazrob.g2.mq4-paired-discordance-identifiability.v1",
        "source_library_summary_sha256": "0df1ed3776aa515973886bad18153498325a038040bdb08d8a0a70ab35f5a1ea",
        "positive_rows": P, "negative_rows": N, "threshold_ppm": 4,
        "threshold_is_safety_limit": False,
        "common_row_identity_verified": False,
        "conditional_same_row_universe": True,
        "pairwise_prediction_rows_available": False,
        "mcnemar_test_performed": False,
        "independent_physical_evidence_records": 0,
        "g2_qualified": False, "g3_qualified": False,
        "pairs": pairs,
        "limitations": "Sharp hypothetical binary-error disagreement bounds from pinned pooled marginals only. Equal counts and 11 reported days do not prove paired row identities; bounds require a common labelled row universe. No observed discordances, no McNemar p-values, no independent exposures. Retrospective 10/11 folds train on future dates; 4 ppm is not an occupational safety limit."
    }


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--source", type=Path, default=SOURCE)
    ap.add_argument("--evidence", type=Path, default=EVIDENCE)
    ap.add_argument("--output", type=Path, required=True)
    args = ap.parse_args()
    result = derive(args.source.read_bytes(), args.evidence.read_bytes())
    args.output.write_text(json.dumps(result, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print("Hypothetical paired-error bounds only; G2/G3 blocked")


if __name__ == "__main__":
    main()
