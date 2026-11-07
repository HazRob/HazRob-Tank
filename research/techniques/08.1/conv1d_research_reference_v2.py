"""08.1: explicit valid 1D convolution inference kernel, no training/accuracy claim.

Real trainable CNN exists separately in research/benchmarks/uci1081_trainable_neural_v1.py.
This kernel verifies source channel/time conventions only.
"""
from math import isfinite
def _matrix(x):
    data=[list(z) for z in x]
    if len(data)<3 or not data[0]:raise ValueError("minimum sequence length and channels")
    n=len(data[0])
    if any(len(y)!=n or any(isinstance(v,bool) or not isinstance(v,(int,float)) or not isfinite(v) for v in y) for y in data):
        raise ValueError("finite fixed channels required")
    return data,n

def conv1d_valid(x,kernel,bias=0.,*,sensor_ids,source_id,exposure_id):
    data,n=_matrix(x)
    if not source_id or not exposure_id or len(sensor_ids)!=n or len(set(sensor_ids))!=n:
        raise ValueError("source, exposure and exact channels required")
    k=[list(z) for z in kernel]
    if not k or len(k)>len(data) or any(len(z)!=n for z in k):raise ValueError("kernel time/channel dimensions")
    if isinstance(bias,bool) or not isinstance(bias,(int,float)) or not isfinite(bias):
        raise ValueError("invalid bias")
    if any(isinstance(y,bool) or not isinstance(y,(int,float)) or not isfinite(y) for row in k for y in row):
        raise ValueError("nonfinite weights")
    out=[]
    for pos in range(len(data)-len(k)+1):
        out.append(max(0.,bias+sum(data[pos+j][channel]*k[j][channel]
                                   for j in range(len(k)) for channel in range(n))))
    return {"conv_relu":out,"trained":False,"gas_identity_verified":False,
            "source_id":source_id,"exposure_id":exposure_id,"Tank_G3":False}
