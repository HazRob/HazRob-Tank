"""Research-only ChemCore Next evidence gate; no gas identification or ppm estimation."""
from dataclasses import dataclass
from math import isfinite
from typing import FrozenSet, Tuple


@dataclass(frozen=True)
class Evidence:
    source_id: str
    source_sha: str
    session_id: str
    device_id: str
    sensor_domain: str
    model_id: str
    independent_group: str
    score: float
    validated: bool
    feature_names: Tuple[str, ...]


@dataclass(frozen=True)
class Decision:
    eligible: bool
    reason: str
    accepted_groups: int


def evaluate(evidence: Tuple[Evidence, ...], expected_domain: str,
             minimum_independent_groups: int = 2) -> Decision:
    """Conservative pre-fusion eligibility, not confidence calibration."""
    if not isinstance(minimum_independent_groups, int) or isinstance(minimum_independent_groups, bool) or minimum_independent_groups < 2:
        raise ValueError("minimum_independent_groups must be >= 2")
    if not expected_domain or not expected_domain.strip():
        return Decision(False, "MISSING_EXPECTED_DOMAIN", 0)
    if not evidence:
        return Decision(False, "NO_EVIDENCE", 0)
    groups: FrozenSet[str] = frozenset()
    for e in evidence:
        if not all((e.source_id, e.source_sha, e.session_id, e.device_id,
                    e.sensor_domain, e.model_id, e.independent_group)):
            return Decision(False, "MISSING_PROVENANCE", len(groups))
        if e.sensor_domain != expected_domain:
            return Decision(False, "DOMAIN_MISMATCH", len(groups))
        if not e.validated:
            return Decision(False, "UNVALIDATED_EVIDENCE", len(groups))
        if not isinstance(e.score, (int, float)) or isinstance(e.score, bool) or not isfinite(e.score) or not 0 <= e.score <= 1:
            return Decision(False, "INVALID_SCORE", len(groups))
        if not e.feature_names or any(not name for name in e.feature_names):
            return Decision(False, "MISSING_FEATURE_SCHEMA", len(groups))
        groups = groups | {e.independent_group}
    if len(groups) < minimum_independent_groups:
        return Decision(False, "INSUFFICIENT_INDEPENDENT_GROUPS", len(groups))
    return Decision(True, "ELIGIBLE_FOR_FURTHER_ANALYSIS", len(groups))
