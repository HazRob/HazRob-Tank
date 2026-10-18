"""Research-only multiclass temperature scaling on held-out calibration logits."""
from math import exp,log,isfinite

def _valid(logits, labels):
    rows=list(logits);y=list(labels)
    if not rows or len(rows)!=len(y):raise ValueError("aligned nonempty rows required")
    n=len(rows[0])
    if n<2 or any(len(row)!=n or any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) for v in row) for row in rows):
        raise ValueError("finite logits of fixed class count required")
    if any(not isinstance(k,int) or isinstance(k,bool) or not 0<=k<n for k in y):
        raise ValueError("integer class indices required")
    return rows,y,n

def softmax(logits, temperature=1.0):
    if not isinstance(temperature,(int,float)) or isinstance(temperature,bool) or not isfinite(temperature) or temperature<=0:
        raise ValueError("temperature must be finite positive")
    if len(logits)<2 or any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) for v in logits):
        raise ValueError("finite logits required")
    z=[v/temperature for v in logits];m=max(z);w=[exp(v-m) for v in z];s=sum(w)
    return [v/s for v in w]

def negative_log_likelihood(logits, labels, temperature):
    rows,y,_=_valid(logits,labels)
    total=0.
    for row,k in zip(rows,y):
        z=[v/temperature for v in row];m=max(z)
        total+=m+log(sum(exp(v-m) for v in z))-z[k]
    return total/len(rows)

def fit_temperature(logits,labels, *, candidates=None):
    """Fit T using disjoint held-out calibration set, NOT model-training or test set."""
    rows,y,n=_valid(logits,labels)
    if len(set(y))<2:raise ValueError("calibration set must contain >=2 observed classes")
    ts=list(candidates) if candidates is not None else [0.25,0.5,0.75,1.,1.5,2.,3.,5.,10.]
    if not ts or any(not isinstance(t,(int,float)) or isinstance(t,bool) or not isfinite(t) or t<=0 for t in ts):
        raise ValueError("positive candidate temperatures required")
    scores=[(negative_log_likelihood(rows,y,t),t) for t in ts]
    _,best=min(scores)
    return {"temperature":best,"classes":n,"n_calibration":len(rows),
            "calibration_nll":negative_log_likelihood(rows,y,best),
            "warning":"Discrete-grid temperature scaling only; held-out reliability and unknown-gas testing required."}

def apply_temperature(logits,model):
    if len(logits)!=model["classes"]:raise ValueError("class order/count mismatch")
    return softmax(logits,model["temperature"])

def expected_calibration_error(probabilities,labels,*,bins=10):
    rows=list(probabilities);y=list(labels)
    if not rows or len(rows)!=len(y) or not isinstance(bins,int) or isinstance(bins,bool) or bins<2:
        raise ValueError("invalid ECE inputs")
    n=len(rows[0]);counts=[0]*bins;correct=[0]*bins;confidence=[0.]*bins
    for p,k in zip(rows,y):
        if len(p)!=n or n<2 or any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) or v<0 or v>1 for v in p) or abs(sum(p)-1)>1e-6 or not isinstance(k,int) or isinstance(k,bool) or not 0<=k<n:
            raise ValueError("invalid probability row/label")
        top=max(range(n),key=lambda i:p[i]);conf=p[top];b=min(bins-1,int(conf*bins))
        counts[b]+=1;correct[b]+=int(top==k);confidence[b]+=conf
    ece=sum(abs(correct[b]/counts[b]-confidence[b]/counts[b])*counts[b]/len(rows) for b in range(bins) if counts[b])
    return {"ece":ece,"n":len(rows),"bins":bins,"warning":"ECE is bin-dependent; report held-out NLL, Brier, reliability and classwise performance too."}
