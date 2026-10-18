"""Research-only log-Rs environmental normalization; coefficients must be trained."""
from math import exp,log,isfinite

def normalize_rs(rs_ohms, temperature_c, relative_humidity_pct, pressure_hpa,
                 *, reference=(25.0,50.0,1013.25), coefficients=(0.0,0.0,0.0)):
    """log(Rs_ref)=log(Rs_obs)-sum(beta_i*(environment_i-reference_i)).
    Zero coefficients imply no correction, NOT validated independence."""
    values=(rs_ohms,temperature_c,relative_humidity_pct,pressure_hpa,*reference,*coefficients)
    if not all(isfinite(float(x)) for x in values): raise ValueError("non-finite input")
    if rs_ohms<=0 or not 0<=relative_humidity_pct<=100 or pressure_hpa<=0:
        raise ValueError("invalid observation")
    if len(reference)!=3 or len(coefficients)!=3: raise ValueError("three reference values and coefficients required")
    t0,h0,p0=reference
    if not 0<=h0<=100 or p0<=0: raise ValueError("invalid reference environment")
    delta=(temperature_c-t0,relative_humidity_pct-h0,pressure_hpa-p0)
    correction=sum(beta*d for beta,d in zip(coefficients,delta))
    if abs(correction)>50: raise ValueError("correction outside research numerical guardrail")
    return exp(log(rs_ohms)-correction)

def fit_coefficients(rows, *, reference=(25.0,50.0,1013.25)):
    """OLS with numpy; rows contain rs_ohms, temperature_c, humidity_pct, pressure_hpa,
    reference_rs_ohms from SAME gas, concentration, heater phase and sensor.
    Caller must enforce grouping and fit on training data only."""
    import numpy as np
    if len(rows)<5: raise ValueError("insufficient calibration rows")
    x=[];y=[]
    for rs,t,h,p,target_rs in rows:
        if not all(isfinite(float(v)) for v in (rs,t,h,p,target_rs)) or rs<=0 or target_rs<=0 or not 0<=h<=100 or p<=0:
            raise ValueError("invalid row")
        x.append([t-reference[0],h-reference[1],p-reference[2]])
        y.append(log(rs/target_rs))
    a=np.asarray(x,dtype=float);b=np.asarray(y,dtype=float)
    if np.linalg.matrix_rank(a)<3: raise ValueError("environmental design is rank-deficient")
    beta=np.linalg.lstsq(a,b,rcond=None)[0]
    return tuple(float(v) for v in beta)
