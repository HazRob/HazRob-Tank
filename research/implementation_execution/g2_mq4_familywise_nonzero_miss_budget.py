"""Research-only exact binomial holdout budget allowing nonzero misses.

All quantities are hypothetical pre-registered group-level coverage claims.
Not physical exposure data, calibrated ppm, or G2/G3 qualification.
"""
import argparse
import json
from fractions import Fraction
from pathlib import Path

from g2_mq4_familywise_zero_miss_budget import (
    MAX_CLAIMS, MAX_GROUPS, _integer, _probability,
    min_familywise_zero_miss_groups,
)

MAX_MISSES = 20


def binomial_upper_tail_for_at_most_misses(n, misses, coverage):
    """Exact P_{p0}(successes >= n-misses) under p0=coverage."""
    n = _integer(n, 'N', 1, MAX_GROUPS)
    misses = _integer(misses, 'MISSES', 0, MAX_MISSES)
    if misses >= n:
        raise ValueError('G2_FAMILYWISE_MISSES_NOT_LESS_THAN_N')
    if type(coverage) is Fraction:
        if not 0 < coverage < 1:
            raise ValueError('G2_FAMILYWISE_TARGET')
        p = coverage
    else:
        p = _probability(coverage, 'TARGET')
    term = p ** n
    total = term
    for j in range(1, misses + 1):
        term *= Fraction(n - j + 1, j) * (1 - p) / p
        total += term
    return total


def min_familywise_miss_tolerant_groups(target_coverage, allowed_misses=0,
                                         family_confidence=0.95, claims=80):
    """Minimum n whose exact one-sided binomial test rejects p <= target.

    For up to k misses, P_{p0}(X >= n-k) <= (1-confidence)/claims.
    Bonferroni controls simultaneous claims, regardless of dependence
    BETWEEN claims; groups WITHIN each claim must be independent and
    representative, and the design/claim family must be fixed beforehand.
    """
    p = _probability(target_coverage, 'TARGET')
    conf = _probability(family_confidence, 'CONFIDENCE')
    count = _integer(claims, 'CLAIMS', 1, MAX_CLAIMS)
    k = _integer(allowed_misses, 'MISSES', 0, MAX_MISSES)
    alpha = (1 - conf) / count
    if binomial_upper_tail_for_at_most_misses(MAX_GROUPS, k, p) > alpha:
        raise ValueError('G2_FAMILYWISE_TOO_LARGE')
    lo, hi = k, MAX_GROUPS
    while lo + 1 < hi:
        mid = (lo + hi) // 2
        if binomial_upper_tail_for_at_most_misses(mid, k, p) <= alpha:
            hi = mid
        else:
            lo = mid
    return hi


def plan():
    levels = (0.9, 0.95)
    misses = (0, 1, 2, 5)
    results = {}
    for coverage in levels:
        rows = {}
        for k in misses:
            n = min_familywise_miss_tolerant_groups(coverage, k)
            rows[str(k)] = {
                'minimum_independent_locked_holdout_groups_per_claim': n,
                'maximum_predeclared_misses': k,
                'rejection_tail_at_minimum': float(
                    binomial_upper_tail_for_at_most_misses(n, k, coverage)),
                'scientifically_qualified': False,
            }
        results[str(coverage)] = rows
    return {
        'schema': 'hazrob.g2.mq4-familywise-nonzero-miss-budget.v1',
        'family_confidence_one_sided': 0.95,
        'number_of_predeclared_claims': 80,
        'bonferroni_per_claim_alpha': 0.000625,
        'target_coverage_levels': list(levels),
        'allowed_misses': list(misses),
        'results': results,
        'zero_miss_crosscheck_90': min_familywise_zero_miss_groups(0.9),
        'zero_miss_crosscheck_95': min_familywise_zero_miss_groups(0.95),
        'physical_independent_evidence_records': 0,
        'original_mq4_zip_replayed_this_iteration': False,
        'g2_qualified': False,
        'g3_qualified': False,
        'limitations': (
            'Exact conditional one-sided binomial and Bonferroni planning, not '
            'an empirical result. Requires fixed prespecified claims, blinded '
            'independent representative physical holdout groups within each '
            'claim and trustworthy reference truth. Dependence between claims '
            'is permitted for Bonferroni, but not pseudoreplication of rows '
            'within a claim. Calendar days are not automatically independent '
            'physical exposures. More than k misses fails the stated design; '
            'no valid scientific qualification follows from this calculation.'
        ),
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    args.output.write_text(json.dumps(plan(), indent=2, sort_keys=True) + '\n',
                           encoding='utf-8')
    print('Research-only miss-tolerant holdout planning; G2/G3 NOT QUALIFIED')


if __name__ == '__main__':
    main()
