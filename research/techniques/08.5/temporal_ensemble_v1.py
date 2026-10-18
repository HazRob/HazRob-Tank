"""Research-only weighted temporal probability aggregation."""
from math import isfinite
def ensemble(probability_rows,weights=None):
    rows=[list(row) for row in probability_rows]
    if not rows or len(rows[0])<2:raise ValueError("multiclass rows required")
    n=len(rows[0])
    if any(len(row)!=n or any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) or v<0 for v in row) or abs(sum(row)-1)>1e-6 for row in rows):raise ValueError("normalized rows required")
    w=[1.]*len(rows) if weights is None else list(weights)
    if len(w)!=len(rows) or any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) or v<0 for v in w) or sum(w)<=0:raise ValueError("positive total weight required")
    p=[sum(w[i]*rows[i][j] for i in range(len(rows)))/sum(w) for j in range(n)]
    return {"probabilities":p,"candidate_index":max(range(n),key=lambda j:p[j]),"warning":"Correlated temporal frames are not independent evidence; aggregation is not calibration."}
