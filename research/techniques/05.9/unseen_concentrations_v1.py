"""Research-only evaluation of concentration-level holdouts (no model fitting)."""
from math import log10, sqrt, isfinite

def _positive(x):
    return isinstance(x,(int,float)) and not isinstance(x,bool) and isfinite(x) and x>0

def split_by_concentration(records, held_out_ppm):
    """Records: dictionaries with ppm and optional session/device/sensors.
    Hold out entire concentration levels, never individual repeated rows.
    """
    rows=list(records);held=set(held_out_ppm)
    if not rows or not held or any(not _positive(v) for v in held):
        raise ValueError("nonempty records and positive held-out concentrations required")
    if any(not isinstance(row,dict) or not _positive(row.get("ppm")) for row in rows):
        raise ValueError("invalid reference ppm")
    levels={row["ppm"] for row in rows}
    if not held.issubset(levels):raise ValueError("held-out levels must exist")
    train=[dict(row) for row in rows if row["ppm"] not in held]
    test=[dict(row) for row in rows if row["ppm"] in held]
    if not train or not test or len({row["ppm"] for row in train})<2:
        raise ValueError("at least two distinct training concentration levels required")
    train_levels={row["ppm"] for row in train}
    assert train_levels.isdisjoint({row["ppm"] for row in test})
    low=min(train_levels);high=max(train_levels)
    return {"train":train,"test":test,"train_levels":sorted(train_levels),
            "test_levels":sorted(held),"train_range_ppm":[low,high],
            "test_region":{v:("INTERPOLATION" if low<v<high else "EXTRAPOLATION")
                           for v in held}}

def score_predictions(reference_ppm,predicted_ppm,*,train_range_ppm):
    """Descriptive held-out metrics, separating abstentions and in/out-range targets."""
    truth=list(reference_ppm);pred=list(predicted_ppm)
    if not truth or len(truth)!=len(pred) or any(not _positive(v) for v in truth):
        raise ValueError("aligned positive references required")
    lo,hi=train_range_ppm
    if not _positive(lo) or not _positive(hi) or lo>=hi:
        raise ValueError("valid training concentration range required")
    groups={"INTERPOLATION":[],"EXTRAPOLATION":[]}
    for actual,guess in zip(truth,pred):
        if guess is not None and not _positive(guess):
            raise ValueError("predictions must be positive finite ppm or None")
        region="INTERPOLATION" if lo<actual<hi else "EXTRAPOLATION"
        groups[region].append((actual,guess))
    def metrics(rows):
        accepted=[(a,p) for a,p in rows if p is not None]
        n=len(rows);m=len(accepted)
        return {"n":n,"n_estimated":m,"n_abstained":n-m,
                "coverage":m/n if n else None,
                "mae_ppm":sum(abs(a-p) for a,p in accepted)/m if m else None,
                "rmse_ppm":sqrt(sum((a-p)**2 for a,p in accepted)/m) if m else None,
                "mean_abs_log10_error":sum(abs(log10(a)-log10(p)) for a,p in accepted)/m if m else None}
    return {"overall":metrics(list(zip(truth,pred))),
            "interpolation":metrics(groups["INTERPOLATION"]),
            "extrapolation":metrics(groups["EXTRAPOLATION"]),
            "warning":"Descriptive only; compare identical held-out levels, sessions and devices. Coverage and error must be reported together."}
