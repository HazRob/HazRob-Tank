"""Research-only prospective MQ4 split-conformal rank-feasibility budget.

Necessary group counts, NOT sufficient scientific sample sizes, exchangeability,
empirical coverage, calibration validity, or G2/G3 promotion.
"""
import argparse
import json
import math
from pathlib import Path


def _positive_integer(value, label, minimum=0):
    if type(value) is not int or value < minimum:
        raise ValueError('G2_ACQUISITION_' + label)
    return value


def finite_rank_minimum(nominal):
    if type(nominal) not in (int, float) or not math.isfinite(nominal) or not 0 < nominal < 1:
        raise ValueError('G2_ACQUISITION_NOMINAL')
    n = 1
    while math.ceil((n + 1) * nominal - 1e-12) > n:
        n += 1
    return n


def plan(min_training_groups=3, locked_test_groups=6, gaps=(0, 1, 2),
         nominal_levels=(0.9, 0.95), existing_candidate_days=11):
    _positive_integer(min_training_groups, 'TRAINING', 2)
    _positive_integer(locked_test_groups, 'HOLDOUT', 1)
    _positive_integer(existing_candidate_days, 'DAYS', 0)
    gaps = tuple(gaps)
    nominal_levels = tuple(nominal_levels)
    if not gaps or len(set(gaps)) != len(gaps):
        raise ValueError('G2_ACQUISITION_GAPS')
    for gap in gaps:
        _positive_integer(gap, 'GAPS')
    if not nominal_levels or len(set(nominal_levels)) != len(nominal_levels):
        raise ValueError('G2_ACQUISITION_NOMINAL')
    if existing_candidate_days and existing_candidate_days < min_training_groups + locked_test_groups:
        raise ValueError('G2_ACQUISITION_DAYS')
    minimum = {str(float(p)): finite_rank_minimum(p) for p in nominal_levels}
    results = {}
    for gap in gaps:
        # Existing days are candidate calendar days only, not verified exposures.
        candidate_past = max(0, existing_candidate_days - locked_test_groups - gap)
        candidate_cal = max(0, candidate_past - min_training_groups)
        results[str(gap)] = {
            'candidate_calendar_calibration_days_not_verified_exposures': candidate_cal,
            'nominal_rank_budgets': {
                level: {
                    'minimum_exchangeable_calibration_groups': count,
                    'shortfall_if_existing_calendar_days_were_valid_groups': max(0, count - candidate_cal),
                    'prospective_contiguous_calendar_days_if_one_independent_group_per_day':
                        min_training_groups + count + gap + locked_test_groups,
                    'prospective_total_independent_exposure_groups_excluding_embargo':
                        min_training_groups + count + locked_test_groups,
                    'rank_feasible_only_not_scientifically_qualified': True,
                }
                for level, count in minimum.items()
            },
        }
    return {
        'schema': 'hazrob.g2.mq4-prospective-acquisition-rank-budget.v1',
        'min_training_groups': min_training_groups,
        'locked_test_groups': locked_test_groups,
        'existing_candidate_calendar_days': existing_candidate_days,
        'minimum_group_counts': minimum,
        'gaps_calendar_days': list(gaps),
        'gap_results': results,
        'independent_exposure_records_available': 0,
        'scientific_state': {'g2_qualified': False, 'g3_qualified': False,
                             'exchangeability_verified': False,
                             'traceable_reference_calibration_verified': False,
                             'blinded_holdout_verified': False},
        'limitations': ('Necessary finite-rank counts only. Historical days are not verified independent '
                        'exposure groups; calendar gaps are not additional independent groups. '
                        'The prospective count assumes exactly one eligible independent exposure group '
                        'per consecutive calendar day. Even meeting counts does not prove exchangeability, '
                        'empirical coverage, calibrated gas identity, transfer to Tank, or G2/G3 eligibility. '
                        'Six holdout groups are an illustrative locked block, not a validated coverage sample size.'),
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--train', type=int, default=3)
    parser.add_argument('--test', type=int, default=6)
    parser.add_argument('--existing-days', type=int, default=11)
    parser.add_argument('--gaps', nargs='+', type=int, default=[0, 1, 2])
    args = parser.parse_args()
    result = plan(args.train, args.test, args.gaps, existing_candidate_days=args.existing_days)
    args.output.write_text(json.dumps(result, sort_keys=True, indent=2) + '\n', encoding='utf-8')
    print('Necessary rank budget only: G2/G3 NOT QUALIFIED')


if __name__ == '__main__':
    main()
