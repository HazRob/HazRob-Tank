"""G2 research-only: Bonferroni simultaneous zero-miss holdout planning.

Not physical evidence, observed coverage, or G2/G3 qualification.
"""
import argparse
import json
import math
from fractions import Fraction
from pathlib import Path
from g2_mq4_prospective_acquisition_budget import finite_rank_minimum

MAX_GROUPS = 10000
MAX_CLAIMS = 10000


def _probability(value, label):
    if type(value) not in (int, float) or not math.isfinite(value) or not 0 < value < 1:
        raise ValueError('G2_FAMILYWISE_' + label)
    return Fraction(str(value))


def _integer(value, label, minimum, maximum):
    if type(value) is not int or not minimum <= value <= maximum:
        raise ValueError('G2_FAMILYWISE_' + label)
    return value


def min_familywise_zero_miss_groups(target_coverage, family_confidence=0.95, claims=80):
    """Exact minimal n with p**n <= (1-family_confidence)/claims.

    Bonferroni allows dependent claims but requires independent representative
    physical holdout groups within each predeclared claim, with zero misses.
    """
    target = _probability(target_coverage, 'TARGET')
    confidence = _probability(family_confidence, 'CONFIDENCE')
    count = _integer(claims, 'CLAIMS', 1, MAX_CLAIMS)
    alpha_per_claim = (1 - confidence) / count
    if target ** MAX_GROUPS > alpha_per_claim:
        raise ValueError('G2_FAMILYWISE_TOO_LARGE')
    lo, hi = 0, MAX_GROUPS
    while lo + 1 < hi:
        mid = (lo + hi) // 2
        if target ** mid <= alpha_per_claim:
            hi = mid
        else:
            lo = mid
    return hi


def plan(claims=80, family_confidence=0.95, nominal_levels=(0.9, 0.95),
         training_groups=3, illustrative_holdout_groups=6, gaps=(0, 1, 2)):
    count = _integer(claims, 'CLAIMS', 1, MAX_CLAIMS)
    confidence = _probability(family_confidence, 'CONFIDENCE')
    _integer(training_groups, 'TRAINING', 2, MAX_GROUPS)
    _integer(illustrative_holdout_groups, 'HOLDOUT', 1, MAX_GROUPS)
    levels, gaps = tuple(nominal_levels), tuple(gaps)
    if not levels or len(set(levels)) != len(levels):
        raise ValueError('G2_FAMILYWISE_LEVELS')
    if not gaps or len(set(gaps)) != len(gaps):
        raise ValueError('G2_FAMILYWISE_GAPS')
    for gap in gaps:
        _integer(gap, 'GAP', 0, MAX_GROUPS)
    results = {}
    for level in levels:
        target = _probability(level, 'TARGET')
        n = min_familywise_zero_miss_groups(level, family_confidence, count)
        cal = finite_rank_minimum(level)
        total = training_groups + cal + n
        results[str(float(target))] = {
            'finite_split_conformal_calibration_rank_groups_only': cal,
            'independent_zero_miss_holdout_groups_per_claim': n,
            'previous_six_group_simultaneous_lower_bound_if_all_covered':
                float((1 - confidence) / count) ** (1 / illustrative_holdout_groups),
            'illustrative_train_calibration_holdout_groups': total,
            'illustrative_days_one_verified_independent_group_per_day':
                {str(gap): total + gap for gap in gaps},
            'all_claims_scientifically_qualified': False,
        }
    return {
        'schema': 'hazrob.g2.mq4-familywise-zero-miss-budget.v1',
        'number_of_predeclared_coverage_claims': count,
        'family_confidence': family_confidence,
        'per_claim_alpha_bonferroni': float((1 - confidence) / count),
        'nominal_coverage_levels': list(levels),
        'illustrative_training_groups': training_groups,
        'illustrative_existing_holdout_groups': illustrative_holdout_groups,
        'calendar_embargo_days': list(gaps),
        'levels': results,
        'independent_physical_holdout_records_available': 0,
        'g2_qualified': False,
        'g3_qualified': False,
        'limitations': (
            'Conditional design only: 80 separately predeclared coverage claims, '
            'Bonferroni family-wise one-sided confidence, zero misses per claim, '
            'independent representative Bernoulli physical holdout groups within each claim, '
            'fixed protocol and genuine blinded evaluation. Bonferroni does not require '
            'independence across techniques; the same verified physical groups may be '
            'reused across techniques when every claim is valid on them. '
            'These are necessary numerical zero-miss bounds for the stated protocol, '
            'not observed performance, a sufficient G2 sample size, power analysis, '
            'or evidence of independent exposures. Calendar days are not inherently '
            'independent groups. No multiple-claim correction is necessary when '
            'only one claim is prespecified; do not assert 80 simultaneous claims '
            'unless all are preregistered and scientifically meaningful.'),
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    args.output.write_text(json.dumps(plan(), indent=2, sort_keys=True) + '\n', encoding='utf-8')
    print('Research-only family-wise planning; G2/G3 NOT QUALIFIED')


if __name__ == '__main__':
    main()
