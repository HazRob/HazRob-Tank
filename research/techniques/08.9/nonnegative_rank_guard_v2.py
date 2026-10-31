"""08.9 Two-source NNLS screening with scale-robust identifiability checks.

Only fits numeric evidence. It neither identifies actual gases nor outputs ppm.
"""
from math import fsum, hypot, isfinite, sqrt


def nnls_two_component(y, a, b, *, source_id, sensor_ids, reference_r0_id,
                       heater_phase, max_condition=1e5):
    if not all(isinstance(s, str) and s.strip() for s in (source_id, reference_r0_id)):
        raise ValueError("source and R0 provenance required")
    if heater_phase not in ("HIGH", "LOW", "CONSTANT"):
        raise ValueError("fixed heater phase required")
    yy, aa, bb = list(y), list(a), list(b)
    sensor_ids = list(sensor_ids)
    n = len(yy)
    if (n < 2 or len(aa) != n or len(bb) != n or len(sensor_ids) != n or
        any(not isinstance(s, str) or not s.strip() for s in sensor_ids) or
        len(set(sensor_ids)) != n):
        raise ValueError("matched unique sensor channels required")
    if any(isinstance(v, bool) or not isinstance(v, (int, float)) or not isfinite(v)
           for v in yy + aa + bb):
        raise ValueError("finite numeric source arrays required")
    if (isinstance(max_condition, bool) or not isinstance(max_condition, (int, float)) or
        not isfinite(max_condition) or max_condition <= 1):
        raise ValueError("invalid condition limit")
    an, bn = hypot(*aa), hypot(*bb)
    if not isfinite(an) or not isfinite(bn):
        raise ValueError("out-of-range source fingerprint norm")
    if an == 0 or bn == 0:
        return {"status": "NOT_IDENTIFIABLE_SOURCE_COLLINEAR", "coefficients": None,
                "gas_ppm": None, "Tank_G3": False}
    ua, ub = [v/an for v in aa], [v/bn for v in bb]
    rho = fsum(v*w for v, w in zip(ua, ub))
    rho = max(-1., min(1., rho))
    determinant_unit = max(0., (1.-rho)*(1.+rho))
    if determinant_unit <= 1e-12:
        return {"status": "NOT_IDENTIFIABLE_SOURCE_COLLINEAR", "coefficients": None,
                "gas_ppm": None, "Tank_G3": False}
    scale = max(an, bn)
    A, B = (an/scale)**2, (bn/scale)**2
    AB = (an/scale)*(bn/scale)*rho
    maximum_eigenvalue = ((A+B)+hypot(A-B, 2*AB))/2
    minimum_eigenvalue = (A*B*determinant_unit)/maximum_eigenvalue
    if minimum_eigenvalue <= 0:
        return {"status": "NOT_IDENTIFIABLE_ILL_CONDITIONED", "coefficients": None,
                "gas_ppm": None, "Tank_G3": False}
    condition = sqrt(maximum_eigenvalue/minimum_eigenvalue)
    if condition > max_condition:
        return {"status": "NOT_IDENTIFIABLE_ILL_CONDITIONED", "condition_number": condition,
                "coefficients": None, "gas_ppm": None, "Tank_G3": False}
    au, bu = fsum(v*w for v,w in zip(ua, yy)), fsum(v*w for v,w in zip(ub, yy))
    # Dimensionless unit-column solution, then return coefficients in source units.
    candidates = [(0., 0.), (max(0., au/an), 0.), (0., max(0., bu/bn))]
    ca, cb = ((au-rho*bu)/determinant_unit/an,
              (bu-rho*au)/determinant_unit/bn)
    if ca >= 0 and cb >= 0:
        candidates.append((ca, cb))
    def rmse(u,v):
        residual = hypot(*(target-u*x-v*z for target,x,z in zip(yy, aa, bb))) / sqrt(n)
        return residual
    candidate_results = [(rmse(u,v),u,v) for u,v in candidates]
    if any(not all(isfinite(q) for q in item) for item in candidate_results):
        raise ValueError("NNLS numeric range exceeded")
    loss, x, z = min(candidate_results)
    return {"status": "NUMERICAL_NNLS_SOURCE_PRIOR_ONLY", "coefficients": {"a": x,"b": z},
            "residual_rmse": loss, "condition_number": condition,
            "gas_ppm": None, "linear_additive_physics_validated": False,
            "Tank_G3": False}
