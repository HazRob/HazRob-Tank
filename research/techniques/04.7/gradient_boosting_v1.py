"""Research-only multiclass gradient-boosted decision stumps; NOT XGBoost."""
from math import exp,isfinite

def _validate(rows, labels):
    x=[tuple(row) for row in rows]; y=list(labels)
    if len(x)<4 or len(x)!=len(y) or not x[0] or any(len(row)!=len(x[0]) for row in x):
        raise ValueError("invalid matrix")
    if any(isinstance(v,bool) or not isinstance(v,(int,float)) or not isfinite(v) for row in x for v in row):
        raise ValueError("nonfinite features")
    if any(not isinstance(v,str) or not v for v in y) or len(set(y))<2:
        raise ValueError("need at least two classes")
    return x,y,sorted(set(y))

def _softmax(logits):
    m=max(logits); e=[exp(v-m) for v in logits]; s=sum(e)
    return [v/s for v in e]

def _stump(x,residuals,l2):
    """Fit a squared-error regression stump to negative softmax gradients."""
    n=len(x); d=len(x[0])
    baseline=sum(residuals)/(n+l2)
    best=(sum((v-baseline)**2 for v in residuals),None,None,baseline,baseline)
    for j in range(d):
        values=sorted(set(row[j] for row in x))
        for a,b in zip(values,values[1:]):
            threshold=a+(b-a)/2
            left=[i for i,row in enumerate(x) if row[j]<=threshold]
            right=[i for i,row in enumerate(x) if row[j]>threshold]
            if not left or not right:continue
            lv=sum(residuals[i] for i in left)/(len(left)+l2)
            rv=sum(residuals[i] for i in right)/(len(right)+l2)
            loss=sum((residuals[i]-(lv if i in left else rv))**2 for i in range(n))
            candidate=(loss,j,threshold,lv,rv)
            if candidate[0]<best[0]-1e-12 or (abs(candidate[0]-best[0])<=1e-12 and best[1] is not None and candidate[1:3]<best[1:3]):best=candidate
    return {"feature":best[1],"threshold":best[2],"left":best[3],"right":best[4]}

def _apply(stump,row):
    return stump["left"] if stump["feature"] is None or row[stump["feature"]]<=stump["threshold"] else stump["right"]

def fit_boosted_stumps(train_rows,train_labels,*,rounds=30,learning_rate=.2,l2=1.):
    x,y,classes=_validate(train_rows,train_labels)
    if not isinstance(rounds,int) or isinstance(rounds,bool) or not 1<=rounds<=10000:
        raise ValueError("invalid rounds")
    if any(isinstance(v,bool) or not isinstance(v,(int,float)) or not isfinite(v) for v in (learning_rate,l2)) or learning_rate<=0 or l2<0:
        raise ValueError("invalid hyperparameters")
    n=len(x); k=len(classes); logits=[[0.]*k for _ in x]; trees=[]
    for _ in range(rounds):
        probabilities=[_softmax(z) for z in logits]
        stage=[]
        for c,label in enumerate(classes):
            residuals=[(1. if actual==label else 0.)-p[c] for actual,p in zip(y,probabilities)]
            stump=_stump(x,residuals,l2)
            for i,row in enumerate(x):
                logits[i][c]+=learning_rate*_apply(stump,row)
                if not isfinite(logits[i][c]):raise ValueError("optimization diverged")
            stage.append(stump)
        trees.append(stage)
    return {"classes":classes,"trees":trees,"learning_rate":learning_rate,"features":len(x[0]),
            "warning":"Approximate residual boosting; NOT XGBoost or calibrated probability."}

def predict_boosted_stumps(rows,model):
    results=[]
    for item in rows:
        row=tuple(item)
        if len(row)!=model["features"] or any(isinstance(v,bool) or not isinstance(v,(int,float)) or not isfinite(v) for v in row):
            raise ValueError("invalid query")
        logits=[0.]*len(model["classes"])
        for stage in model["trees"]:
            for c,stump in enumerate(stage):logits[c]+=model["learning_rate"]*_apply(stump,row)
        p=_softmax(logits); best=max(range(len(p)),key=lambda c:p[c])
        results.append({"predicted_label":model["classes"][best],
                        "softmax_scores":dict(zip(model["classes"],p))})
    return results
