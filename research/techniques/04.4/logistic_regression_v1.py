"""Research-only multiclass softmax regression; train-only standardization."""
from math import exp,isfinite,log

def _validate(rows,labels):
    x=[tuple(row) for row in rows];y=list(labels)
    if len(x)<4 or len(x)!=len(y) or not x[0] or any(len(row)!=len(x[0]) for row in x):
        raise ValueError("invalid feature matrix")
    if any(isinstance(v,bool) or not isinstance(v,(int,float)) or not isfinite(v) for row in x for v in row):
        raise ValueError("features must be finite numeric")
    if any(not isinstance(v,str) or not v for v in y) or len(set(y))<2:
        raise ValueError("at least two labelled classes required")
    return x,y,sorted(set(y))

def fit_softmax(train_rows,train_labels,*,epochs=500,learning_rate=0.1,l2=0.01):
    """Full-batch gradient descent; intercept unpenalized; not calibrated."""
    x,y,classes=_validate(train_rows,train_labels)
    if not isinstance(epochs,int) or isinstance(epochs,bool) or not 1<=epochs<=100000:
        raise ValueError("invalid epochs")
    if any(isinstance(v,bool) or not isinstance(v,(int,float)) or not isfinite(v) for v in (learning_rate,l2)) or learning_rate<=0 or l2<0:
        raise ValueError("invalid hyperparameters")
    n=len(x);d=len(x[0]);k=len(classes)
    mean=[sum(row[j] for row in x)/n for j in range(d)]
    scale=[(sum((row[j]-mean[j])**2 for row in x)/n)**.5 for j in range(d)]
    scale=[s if s>0 else 1. for s in scale]
    z=[[1.]+[(row[j]-mean[j])/scale[j] for j in range(d)] for row in x]
    w=[[0.]*(d+1) for _ in classes]
    target=[classes.index(label) for label in y]
    for _ in range(epochs):
        grads=[[0.]*(d+1) for _ in classes]
        for row,yi in zip(z,target):
            logits=[sum(a*b for a,b in zip(weight,row)) for weight in w]
            offset=max(logits);e=[exp(v-offset) for v in logits];den=sum(e)
            for c in range(k):
                error=e[c]/den-(1. if c==yi else 0.)
                for j in range(d+1):grads[c][j]+=error*row[j]
        for c in range(k):
            for j in range(d+1):
                g=grads[c][j]/n+(l2*w[c][j] if j else 0.)
                w[c][j]-=learning_rate*g
                if not isfinite(w[c][j]):raise ValueError("optimization diverged")
    return {"classes":classes,"mean":mean,"scale":scale,"weights":w,"features":d,
            "epochs":epochs,"l2":l2,"warning":"Softmax scores are not independently calibrated probabilities."}

def predict_softmax(rows,model):
    out=[];d=model["features"]
    for item in rows:
        row=tuple(item)
        if len(row)!=d or any(isinstance(v,bool) or not isinstance(v,(int,float)) or not isfinite(v) for v in row):
            raise ValueError("invalid prediction row")
        z=[1.]+[(row[j]-model["mean"][j])/model["scale"][j] for j in range(d)]
        logits=[sum(a*b for a,b in zip(w,z)) for w in model["weights"]]
        offset=max(logits);e=[exp(v-offset) for v in logits];den=sum(e)
        scores=[v/den for v in e];idx=max(range(len(scores)),key=lambda i:scores[i])
        out.append({"predicted_label":model["classes"][idx],
                    "softmax_scores":dict(zip(model["classes"],scores))})
    return out
