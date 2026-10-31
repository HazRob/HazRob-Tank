"""08.2: causal dilated temporal convolution primitive, not trainable TCN."""
from math import isfinite

def causal_dilated_filter(sequence,kernel,*,dilation=1,source_id,exposure_id):
    values=list(sequence);k=list(kernel)
    if not source_id or not exposure_id or not values or not k:
        raise ValueError("source, exposure and non-empty kernel required")
    if isinstance(dilation,bool) or not isinstance(dilation,int) or dilation<1:
        raise ValueError("dilation integer >=1")
    if any(isinstance(v,bool) or not isinstance(v,(int,float)) or not isfinite(v) for v in values+k):
        raise ValueError("finite signal and weights required")
    # Uses current and previous signals only. Out[0:t] cannot see x[t+1:].
    outputs=[sum(w*values[t-j*dilation] for j,w in enumerate(k) if t-j*dilation>=0)
             for t in range(len(values))]
    return {"filtered_signal":outputs,"causal":True,
            "receptive_field_samples":1+(len(k)-1)*dilation,
            "neural_weights_trained":False,
            "source_id":source_id,"exposure_id":exposure_id,"Tank_G3":False}
