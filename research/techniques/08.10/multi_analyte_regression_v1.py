"""Research-only nonnegative multianalyte coordinate-descent regression."""
from math import isfinite
def estimate(observed,response_columns,iterations=100,step_size=0.01):
    y=list(observed);columns=[list(c) for c in response_columns]
    if not y or not columns or not isinstance(iterations,int) or iterations<1 or not isinstance(step_size,(int,float)) or not isfinite(step_size) or step_size<=0:raise ValueError("invalid model")
    n=len(y)
    if any(len(c)!=n for c in columns) or any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) for v in y+[v for c in columns for v in c]):raise ValueError("finite aligned vectors required")
    weights=[0.]*len(columns)
    for _ in range(iterations):
        for j,col in enumerate(columns):
            pred=[sum(weights[k]*columns[k][i] for k in range(len(columns))) for i in range(n)]
            grad=2*sum((pred[i]-y[i])*col[i] for i in range(n))
            weights[j]=max(0.,weights[j]-step_size*grad)
    return {"coefficients":weights,"warning":"Uncalibrated optimization coefficients are NOT validated ppm concentrations; check rank, conditioning, units and nonlinearities."}
