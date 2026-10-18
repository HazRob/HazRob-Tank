"""Research-only single-hidden-layer MLP classifier; no external dependencies."""
from math import exp,isfinite,sqrt,tanh
from random import Random

def _matrix(rows, labels):
    x=[tuple(row) for row in rows]; y=list(labels)
    if len(x)<4 or len(x)!=len(y) or not x[0] or any(len(row)!=len(x[0]) for row in x):
        raise ValueError("invalid training matrix")
    if any(isinstance(v,bool) or not isinstance(v,(int,float)) or not isfinite(v) for row in x for v in row):
        raise ValueError("nonfinite features")
    if any(not isinstance(v,str) or not v for v in y) or len(set(y))<2:
        raise ValueError("need >=2 classes")
    return x,y,sorted(set(y))

def _softmax(logits):
    m=max(logits); a=[exp(v-m) for v in logits]; s=sum(a)
    return [v/s for v in a]

def fit_mlp(train_rows,train_labels,*,hidden=8,epochs=400,learning_rate=.05,l2=.001,seed=42):
    x,y,classes=_matrix(train_rows,train_labels)
    if any(not isinstance(v,int) or isinstance(v,bool) or v<1 for v in (hidden,epochs)):
        raise ValueError("invalid architecture or epochs")
    if not isinstance(seed,int) or isinstance(seed,bool):
        raise ValueError("invalid seed")
    if any(isinstance(v,bool) or not isinstance(v,(int,float)) or not isfinite(v) for v in (learning_rate,l2)) or learning_rate<=0 or l2<0:
        raise ValueError("invalid optimization hyperparameters")
    n=len(x);d=len(x[0]);k=len(classes)
    mean=[sum(row[j] for row in x)/n for j in range(d)]
    scale=[sqrt(sum((row[j]-mean[j])**2 for row in x)/n) for j in range(d)]
    scale=[v if v>0 else 1. for v in scale]
    z=[[(row[j]-mean[j])/scale[j] for j in range(d)] for row in x]
    rng=Random(seed)
    w1=[[rng.uniform(-.3,.3) for _ in range(d)] for _ in range(hidden)]
    b1=[0.]*hidden
    w2=[[rng.uniform(-.3,.3) for _ in range(hidden)] for _ in range(k)]
    b2=[0.]*k
    for _ in range(epochs):
        g1=[[0.]*d for _ in range(hidden)]; gb1=[0.]*hidden
        g2=[[0.]*hidden for _ in range(k)]; gb2=[0.]*k
        for row,label in zip(z,y):
            h=[tanh(sum(a*v for a,v in zip(w,row))+b) for w,b in zip(w1,b1)]
            p=_softmax([sum(a*v for a,v in zip(w,h))+b for w,b in zip(w2,b2)])
            delta=[p[c]-(1. if label==classes[c] else 0.) for c in range(k)]
            for c in range(k):
                gb2[c]+=delta[c]
                for j in range(hidden):g2[c][j]+=delta[c]*h[j]
            dh=[(1-h[j]**2)*sum(delta[c]*w2[c][j] for c in range(k)) for j in range(hidden)]
            for j in range(hidden):
                gb1[j]+=dh[j]
                for f in range(d):g1[j][f]+=dh[j]*row[f]
        for j in range(hidden):
            b1[j]-=learning_rate*gb1[j]/n
            for f in range(d):w1[j][f]-=learning_rate*(g1[j][f]/n+l2*w1[j][f])
        for c in range(k):
            b2[c]-=learning_rate*gb2[c]/n
            for j in range(hidden):w2[c][j]-=learning_rate*(g2[c][j]/n+l2*w2[c][j])
        if not all(isfinite(v) for arr in w1+w2+[b1,b2] for v in arr):
            raise ValueError("training diverged")
    return {"classes":classes,"mean":mean,"scale":scale,"w1":w1,"b1":b1,
            "w2":w2,"b2":b2,"features":d,"hidden":hidden,"seed":seed}

def predict_mlp(rows,model):
    out=[]
    for item in rows:
        row=tuple(item);d=model["features"]
        if len(row)!=d or any(isinstance(v,bool) or not isinstance(v,(int,float)) or not isfinite(v) for v in row):
            raise ValueError("invalid query")
        z=[(row[j]-model["mean"][j])/model["scale"][j] for j in range(d)]
        h=[tanh(sum(a*v for a,v in zip(w,z))+b) for w,b in zip(model["w1"],model["b1"])]
        p=_softmax([sum(a*v for a,v in zip(w,h))+b for w,b in zip(model["w2"],model["b2"])])
        best=max(range(len(p)),key=lambda j:p[j])
        out.append({"predicted_label":model["classes"][best],"softmax_scores":dict(zip(model["classes"],p))})
    return out
