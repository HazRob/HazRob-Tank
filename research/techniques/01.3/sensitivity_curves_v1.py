"""Research-only MQ sensitivity curve interpolation. No manufacturer curves bundled."""
from math import isfinite, log, exp

def _check_points(points):
    if len(points)<2: raise ValueError("at least two points required")
    out=[]
    for ppm, ratio in points:
        if not isfinite(ppm) or not isfinite(ratio) or ppm<=0 or ratio<=0:
            raise ValueError("concentration and Rs/R0 must be positive finite")
        out.append((float(ppm),float(ratio)))
    if any(out[i][0]>=out[i+1][0] for i in range(len(out)-1)):
        raise ValueError("concentrations must be strictly ascending")
    return out

def ratio_at_ppm(points, ppm, *, allow_extrapolation=False):
    """Piecewise log-log interpolation; outside range rejected by default."""
    p=_check_points(points)
    if not isfinite(ppm) or ppm<=0: raise ValueError("invalid concentration")
    if not allow_extrapolation and not p[0][0]<=ppm<=p[-1][0]:
        raise ValueError("outside calibrated concentration range")
    for x,y in p:
        if ppm==x: return y
    j=0
    while j<len(p)-2 and ppm>p[j+1][0]: j+=1
    x0,y0=p[j];x1,y1=p[j+1]
    t=log(ppm/x0)/log(x1/x0)
    return exp(log(y0)+t*log(y1/y0))

def ppm_from_ratio(points, ratio, *, allow_extrapolation=False):
    """Invert strictly monotonic sensitivity curve; no mixtures or identity inference."""
    p=_check_points(points)
    ys=[y for _,y in p]
    if not (all(ys[i]<ys[i+1] for i in range(len(ys)-1)) or
            all(ys[i]>ys[i+1] for i in range(len(ys)-1))):
        raise ValueError("inverse requires strictly monotonic ratios")
    if not isfinite(ratio) or ratio<=0: raise ValueError("invalid ratio")
    low,high=min(ys),max(ys)
    if not allow_extrapolation and not low<=ratio<=high:
        raise ValueError("outside calibrated ratio range")
    for x,y in p:
        if ratio==y: return x
    # When explicitly extrapolating, select the endpoint *adjacent* to the
    # observation. A search over in-range segments alone silently picks the
    # final segment for both tails, which is wrong for non-power-law curves.
    if ratio<low or ratio>high:
        before_first=(ys[0]>ys[-1] and ratio>ys[0]) or (ys[0]<ys[-1] and ratio<ys[0])
        j=0 if before_first else len(p)-2
    else:
        j=0
        while j<len(p)-2 and not min(ys[j],ys[j+1])<=ratio<=max(ys[j],ys[j+1]):
            j+=1
    x0,y0=p[j];x1,y1=p[j+1]
    t=log(ratio/y0)/log(y1/y0)
    return exp(log(x0)+t*log(x1/x0))
