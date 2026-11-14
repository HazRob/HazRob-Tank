"""Research-only exact one-sided zero-miss holdout feasibility; no G2/G3 promotion.

A distinct, *hypothetical* empirical coverage check, not the split-conformal
calibration-rank condition. Requires independently sampled exchangeable groups.
"""
import argparse
import json
import math
from fractions import Fraction
from pathlib import Path

from g2_mq4_prospective_acquisition_budget import finite_rank_minimum

MAX_GROUPS = 10000


def _probability(value, label):
    if type(value) not in (int, float) or not math.isfinite(value) or not 0 < value < 1:
        raise ValueError('G2_HOLDOUT_' + label)
    return Fraction(str(value))


def _integer(value, label, minimum):
    if type(value) is not int or value < minimum:
        raise ValueError('G2_HOLDOUT_' + label)
    return value


def min_zero_miss_groups(target_coverage, confidence=0.95):
    """Exact integer n satisfying target_coverage**n <= (1-confidence).

    Only if all n independent Bernoulli holdout groups are covered does the
    one-sided Clopper-Pearson lower confidence bound reach target_coverage.
    """
    target = _probability(target_coverage, 'TARGET')
    alpha = 1 - _probability(confidence, 'CONFIDENCE')
    estimate = math.ceil(math.log(float(alpha)) / math.log(float(target)))
    if estimate > MAX_GROUPS:
        raise ValueError('G2_HOLDOUT_TOO_LARGE')
    n = max(1, estimate)
    while n > 1 and target ** (n - 1) <= alpha:
        n -= 1
    while target ** n > alpha:
        n += 1
        if n > MAX_GROUPS:
            raise ValueError('G2_HOLDOUT_TOO_LARGE')
    return n


def zero_miss_lower_bound(groups, confidence=0.95):
    _integer(groups, 'GROUPS', 1)
    alpha = 1 - _probability(confidence, 'CONFIDENCE')
    return float(alpha) ** (1.0 / groups)


def plan(min_training_groups=3, illustrative_holdout_groups=6,
         gaps=(0, 1, 2), nominal_levels=(0.9, 0.95), confidence=0.95):
    _integer(min_training_groups, 'TRAINING', 2)
    _integer(illustrative_holdout_groups, 'GROUPS', 1)
    gaps, nominal_levels = tuple(gaps), tuple(nominal_levels)
    if not gaps or len(set(gaps)) != len(gaps):
        raise ValueError('G2_HOLDOUT_GAPS')
    for gap in gaps:
        _integer(gap, 'GAPS', 0)
    if not nominal_levels or len(set(nominal_levels)) != len(nominal_levels):
        raise ValueError('G2_HOLDOUT_LEVELS')
    _probability(confidence, 'CONFIDENCE')
    levels = {}
    for p in nominal_levels:
        level = str(float(_probability(p, 'TARGET')))
        cal = finite_rank_minimum(p)
        holdout = min_zero_miss_groups(p, confidence)
        levels[level] = {
            'minimum_calibration_groups_for_finite_conformal_rank_only': cal,
            'minimum_independent_holdout_groups_if_zero_misses': holdout,
            'illustrative_six_group_all_covered_lower_bound': zero_miss_lower_bound(
                illustrative_holdout_groups, confidence),
            'total_independent_groups_train_calibration_holdout': min_training_groups + cal + holdout,
            'contiguous_calendar_days_one_independent_group_per_day': {
                str(gap): min_training_groups + cal + holdout + gap for gap in gaps},
            'requires_zero_misses_and_independent_bernoulli_holdout': True,
            'scientifically_qualified': False,
        }
    return {
        'schema': 'hazrob.g2.mq4-zero-miss-holdout-budget.v1',
        'confidence_one_sided': confidence,
        'minimum_training_groups_illustrative': min_training_groups,
        'previous_illustrative_holdout_groups': illustrative_holdout_groups,
        'calendar_embargo_days': list(gaps),
        'nominal_levels': list(nominal_levels),
        'levels': levels,
        'independent_exposure_records_available': 0,
        'scientific_state': {
            'g2_qualified': False, 'g3_qualified': False,
            'physical_independence_verified': False,
            'traceable_reference_calibration_verified': False,
            'exchangeability_verified': False,
            'blinded_holdout_verified': False,
        },
        'limitations': ('Exact one-sided binomial zero-miss planning bound, not observed coverage, '
                        'power analysis, split-conformal coverage proof, or a G2/G3 certification. '
                        'Requires genuinely independent representative holdout exposure groups, '
                        'all covered, and valid fixed protocol. Any miss changes the bound. '
                        'Consecutive calendar days are not necessarily independent exposures. '
                        'Existing MQ4 observational days are not credited as physical groups.'),
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    args.output.write_text(json.dumps(plan(), indent=2, sort_keys=True) + '\n', encoding='utf-8')
    print('Research-only zero-miss holdout budget; G2/G3 NOT QUALIFIED')


if __name__ == '__main__':
    main()
