"""Research-only quadratic log-response regression for a known gas and MQ sensor."""
from math import log10,isfinite,sqrt
def pos(v):
    return isinstance(v,(int,float)) and not isinstance(v,bool) and isfinite(v) and v>0
def solve(a,b):
    n=len(b);a=[list(row)+[b[i]] for i,row in enumerate(a)]
    for j in range(n):
        pivot=max(range(j,n),key=lambda i:abs(a[i][j]))
        if abs(a[pivot][j])<1e-12:raise ValueError("singular calibration design")
        a[j],a[pivot]=a[pivot],a[j]
        z=a[j][j]
        for k in range(j,n+1):a[j][k]/=z
        for i in range(n):
            if i==j:continue
            z=a[i][j]
            for k in range(j,n+1):a[i][k]-=z*a[j][k]
    return [a[i][n] for i in range(n)]
def fit_nonlinear(points,*,gas_label,sensor_id,r0_ohms,ridge=0.):
    if not isinstance(gas_label,str) or not gas_label or not isinstance(sensor_id,str) or not sensor_id or not pos(r0_ohms):
        raise ValueError("gas, sensor, positive R0 required")
    if not isinstance(ridge,(int,float)) or isinstance(ridge,bool) or not isfinite(ridge) or ridge<0:
        raise ValueError("invalid regularization")
    p=[tuple(v) for v in points]
    if len(p)<4 or any(len(v)!=2 or not all(pos(z) for z in v) for v in p):
        raise ValueError("at least four positive reference pairs")
    if len(set(ppm for ppm,_ in p))<4:raise ValueError("four distinct concentrations")
    xs=[log10(ppm) for ppm,_ in p];ys=[log10(rs/r0_ohms) for _,rs in p]
    center=sum(xs)/len(xs);scale=sqrt(sum((x-center)**2 for x in xs)/len(xs))
    if scale<1e-12:raise ValueError("degenerate concentrations")
    basis=[[1,(x-center)/scale,((x-center)/scale)**2] for x in xs]
    gram=[[sum(v[i]*v[j] for v in basis)+(ridge if i==j and i>0 else 0) for j in range(3)] for i in range(3)]
    rhs=[sum(v[i]*y for v,y in zip(basis,ys)) for i in range(3)]
    coef=solve(gram,rhs)
    lo=min(xs);hi=max(xs)
    def derivative(x):
        z=(x-center)/scale
        return (coef[1]+2*coef[2]*z)/scale
    dl=derivative(lo);dh=derivative(hi)
    if abs(dl)<1e-10 or abs(dh)<1e-10 or dl*dh<=0:
        raise ValueError("nonmonotonic/flat fitted curve; inverse ambiguous")
    error=[y-sum(c*v for c,v in zip(coef,row)) for y,row in zip(ys,basis)]
    return {"gas_label":gas_label,"sensor_id":sensor_id,"r0_ohms":r0_ohms,
            "coef":coef,"center":center,"scale":scale,"log_min":lo,"log_max":hi,
            "direction":1 if dl>0 else -1,"in_sample_log_rmse":sqrt(sum(v*v for v in error)/len(error)),
            "n_points":len(p),"ridge":ridge}
def _logratio(logppm,model):
    z=(logppm-model["center"])/model["scale"]
    a,b,c=model["coef"];return a+b*z+c*z*z
def expected_rs(ppm,model):
    if not pos(ppm):raise ValueError("positive ppm required")
    x=log10(ppm)
    if x<model["log_min"]-1e-12 or x>model["log_max"]+1e-12:
        return {"rs_ohms":None,"status":"OUTSIDE_CALIBRATED_RANGE"}
    return {"rs_ohms":model["r0_ohms"]*10**_logratio(x,model),"status":"WITHIN_CALIBRATED_RANGE"}
def estimate_ppm(rs_ohms,model):
    if not pos(rs_ohms):raise ValueError("positive Rs required")
    target=log10(rs_ohms/model["r0_ohms"])
    lo=model["log_min"];hi=model["log_max"]
    a=_logratio(lo,model);b=_logratio(hi,model)
    if target<min(a,b)-1e-10 or target>max(a,b)+1e-10:
        return {"ppm":None,"status":"OUTSIDE_CALIBRATED_RANGE"}
    for _ in range(80):
        mid=(lo+hi)/2
        if (_logratio(mid,model)<target)==(model["direction"]>0):lo=mid
        else:hi=mid
    return {"ppm":10**((lo+hi)/2),"status":"WITHIN_CALIBRATED_RANGE",
            "warning":"Known gas only; no independent calibration accuracy claim."}
