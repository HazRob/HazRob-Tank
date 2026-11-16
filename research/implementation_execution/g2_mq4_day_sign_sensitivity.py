"""Research-only exact day-sign diagnostic from a pinned observational MQ4 summary.

A day is NOT presumed an independent physical exposure. These descriptive
binomial calculations are conditional thought experiments, never G2/G3 evidence.
"""
import argparse
import json
import math
from fractions import Fraction
from pathlib import Path

SOURCE = Path(__file__).with_name("G2_MQ4_FORWARD_DAY_ROBUSTNESS_2026-10-31.json")
EXPECTED_CHANNELS = ("MQ4_1", "MQ4_2", "MQ4_3")
EXPECTED_GAPS = ("0", "1", "2")
EXPECTED_MEMBER_SHA256 = "3c27c4f8b8e8aab54b6433acfa41de00890a9eefd19376259a2371f22a48ff2d"


def exact_positive_sign_tail(positive, negative):
    """P(X >= positive), X~Binomial(positive+negative, 1/2), ties excluded."""
    if type(positive) is not int or type(negative) is not int:
        raise ValueError("G2_SIGN_COUNTS_TYPE")
    if positive < 0 or negative < 0 or positive + negative == 0:
        raise ValueError("G2_SIGN_COUNTS_RANGE")
    n = positive + negative
    return Fraction(sum(math.comb(n, j) for j in range(positive, n + 1)), 2 ** n)


def evaluate(source):
    if not isinstance(source, dict) or source.get("schema") != "hazrob.g2.mq4-forward-day-robustness.v1":
        raise ValueError("G2_SIGN_SOURCE_SCHEMA")
    if source.get("raw_member_sha256") != EXPECTED_MEMBER_SHA256:
        raise ValueError("G2_SIGN_SOURCE_LINEAGE")
    flags = source.get("scientific_state")
    if not isinstance(flags, dict) or any(v is not False for v in flags.values()) or set(flags) != {
        "blinded_holdout_verified", "g2_qualified", "g3_tank_qualified",
        "physical_independence_verified", "reference_calibration_verified"
    }:
        raise ValueError("G2_SIGN_UNSUPPORTED_PROMOTION")
    days = source.get("common_test_days")
    if not isinstance(days, list) or len(days) != 6 or len(set(days)) != 6 or any(
        not isinstance(day, str) for day in days
    ):
        raise ValueError("G2_SIGN_DAY_SCOPE")
    if source.get("calendar_day_count") != 6 or source.get("gaps") != [0, 1, 2]:
        raise ValueError("G2_SIGN_DAY_SCOPE")
    channels = source.get("channels")
    if not isinstance(channels, dict) or set(channels) != set(EXPECTED_CHANNELS):
        raise ValueError("G2_SIGN_CHANNEL_SCOPE")
    alpha = Fraction(1, 20 * 80)  # family alpha=0.05, 80 predeclared claims
    cases = []
    for channel in EXPECTED_CHANNELS:
        gaps = channels[channel]
        if not isinstance(gaps, dict) or set(gaps) != set(EXPECTED_GAPS):
            raise ValueError("G2_SIGN_GAP_SCOPE")
        for gap in EXPECTED_GAPS:
            row = gaps[gap]
            if not isinstance(row, dict):
                raise ValueError("G2_SIGN_ROW")
            p, n = row.get("positive_skill_days"), row.get("negative_skill_days")
            if type(p) is not int or type(n) is not int or p < 0 or n < 0 or p + n != 6:
                raise ValueError("G2_SIGN_INCOMPLETE_DAY_COUNTS")
            if row.get("calendar_test_days") != 6 or row.get("zero_baseline_days") != 0:
                raise ValueError("G2_SIGN_INCOMPLETE_DAY_COUNTS")
            skill = row.get("mean_defined_day_skill")
            if type(skill) not in (int, float) or not math.isfinite(skill):
                raise ValueError("G2_SIGN_INVALID_SKILL")
            tail = exact_positive_sign_tail(p, n)
            cases.append({
                "channel": channel, "embargo_days": int(gap),
                "positive_days": p, "negative_days": n,
                "exact_one_sided_p": {"numerator": tail.numerator, "denominator": tail.denominator},
                "conditional_nominal_5pct_reject": tail <= Fraction(1, 20),
                "conditional_80_claim_bonferroni_reject": tail <= alpha,
                "mean_day_skill_negative": skill < 0,
                "physically_independent_days_verified": False,
                "g2_qualified": False
            })
    return {
        "schema": "hazrob.g2.mq4-day-sign-sensitivity.v1",
        "source_schema": source["schema"],
        "raw_member_sha256": EXPECTED_MEMBER_SHA256,
        "test_calendar_days": len(days),
        "source_observational_only": True,
        "claims_family_hypothetical": 80,
        "bonferroni_per_claim_alpha": {"numerator": 1, "denominator": 1600},
        "minimum_all_positive_independent_groups_for_80_claim_threshold": 11,
        "cases": cases,
        "independent_physical_exposure_records": 0,
        "g2_qualified": False,
        "g3_qualified": False,
        "limitations": (
            "The six calendar days are observational and may be dependent, "
            "not six validated independent blinded physical exposures. "
            "The exact sign tails are conditional sensitivity calculations, "
            "not valid inferential p-values without independence and "
            "prespecified claims. Reusing nine channel/embargo variants "
            "does not create new independent groups. No calibration, ppm "
            "accuracy, gas identity, G2 or G3 qualification is established."
        )
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, default=SOURCE)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    result = evaluate(json.loads(args.source.read_text(encoding="utf-8")))
    args.output.write_text(json.dumps(result, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print("Research-only day-sign diagnostic; G2/G3 NOT QUALIFIED")


if __name__ == "__main__":
    main()
