"""Research-only leave-one-concentration-level-out splits."""
from math import isfinite
def leave_one_level_out(rows):
    data=list(rows)
    if not data:raise ValueError("rows required")
    for r in data:
        if not isinstance(r,dict) or not isinstance(r.get("gas"),str) or not r["gas"] or not isinstance(r.get("ppm"),(int,float)) or isinstance(r["ppm"],bool) or not isfinite(r["ppm"]) or r["ppm"]<=0:
            raise ValueError("gas and positive known ppm required")
    levels=sorted({float(r["ppm"]) for r in data})
    if len(levels)<2:raise ValueError("two or more ppm levels required")
    return [{"held_out_ppm":p,"train_indices":[i for i,r in enumerate(data) if float(r["ppm"])!=p],
             "test_indices":[i for i,r in enumerate(data) if float(r["ppm"])==p],
             "extrapolation":p==levels[0] or p==levels[-1]} for p in levels]
