"""08.9: two-component NNLS candidate with explicit identifiability gate.
Returns dimensionless mixture coefficients only, NEVER gas-specific ppm.
"""
from math import isfinite,sqrt

def nnls_two_component(y,a,b,*,source_id,sensor_ids,
                       reference_r0_id,heater_phase,max_condition=1e5):
    if not source_id or not reference_r0_id or heater_phase not in ("HIGH","LOW","CONSTANT"):
        raise ValueError("source R0/heater context mandatory")
    ys=list(y);aa=list(a);bb=list(b)
    n=len(ys)
    if n<2 or len(aa)!=n or len(bb)!=n or len(sensor_ids)!=n or len(set(sensor_ids))!=n:
        raise ValueError("independent MQ channels and two source fingerprints required")
    if any(isinstance(v,bool) or not isinstance(v,(int,float)) or not isfinite(v) for v in ys+aa+bb):
        raise ValueError("finite numeric features required")
    if isinstance(max_condition,bool) or not isinstance(max_condition,(int,float)) or not isfinite(max_condition) or max_condition<=1:
        raise ValueError("invalid identifiability threshold")
    dot=lambda x,z:sum(u*v for u,v in zip(x,z))
    A=dot(aa,aa);B=dot(bb,bb);AB=dot(aa,bb)
    det=A*B-AB*AB
    if A<=0 or B<=0 or det<=0 or det<=1e-12*A*B:
        return {"status":"NOT_IDENTIFIABLE_SOURCE_COLLINEAR","coefficients":None,
                "gas_ppm":None,"Tank_G3":False}
    tr=A+B;dis=sqrt(max(0.,(A-B)**2+4*AB*AB))
    eigmax=(tr+dis)/2
    eigmin=det/eigmax
    cond=sqrt(eigmax/eigmin)
    if cond>max_condition:
        return {"status":"NOT_IDENTIFIABLE_ILL_CONDITIONED","condition_number":cond,
                "coefficients":None,"gas_ppm":None,"Tank_G3":False}
    ay=dot(aa,ys);by=dot(bb,ys)
    ca=(ay*B-by*AB)/det;cb=(by*A-ay*AB)/det
    options=[(0.,0.),(max(0.,ay/A),0.),(0.,max(0.,by/B))]
    if ca>=0 and cb>=0:options.append((ca,cb))
    residual=lambda u,v:sum((yy-u*x-v*z)**2 for yy,x,z in zip(ys,aa,bb))
    x,z=min(options,key=lambda values:residual(*values))
    return {"status":"NUMERICAL_NNLS_SOURCE_PRIOR_ONLY",
            "coefficients":{"a":x,"b":z},"residual_rmse":sqrt(residual(x,z)/n),
            "condition_number":cond,"gas_ppm":None,"linear_additive_physics_validated":False,
            "Tank_G3":False}
