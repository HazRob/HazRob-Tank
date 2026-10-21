"""Hold out independent exposure groups before any classifier training."""
from math import isfinite

def validate_split(train, train_labels, train_groups, test, test_labels, test_groups, sensor_ids, source_id):
    a=[tuple(x) for x in train]; b=[tuple(x) for x in test]
    y=list(train_labels);z=list(test_labels)
    g=list(train_groups);h=list(test_groups);s=tuple(sensor_ids)
    if not source_id or not s or len(set(s))!=len(s):
        raise ValueError("source and unique sensor IDs required")
    if min(len(a),len(b))<2 or len(a)!=len(y) or len(a)!=len(g) or len(b)!=len(z) or len(b)!=len(h):
        raise ValueError("incomplete source rows")
    if any(len(r)!=len(s) for r in a+b):
        raise ValueError("sensor width changed")
    if any(isinstance(v,bool) or not isinstance(v,(int,float)) or not isfinite(v) for r in a+b for v in r):
        raise ValueError("invalid numeric observations")
    if any(not isinstance(v,str) or not v for v in y+z+g+h):
        raise ValueError("missing class or group labels")
    if len(set(y))<2 or set(z)-set(y) or len(set(g))<3 or len(set(h))<2:
        raise ValueError("insufficient grouped class support")
    if set(g)&set(h):
        raise ValueError("train/test group leakage")
    return a,y,b,z,s

def score_classifier(train, labels, groups, test, test_labels, test_groups, sensor_ids,
                     *, source_id, model_name, trainer, predictor):
    a,y,b,z,s=validate_split(train,labels,groups,test,test_labels,test_groups,sensor_ids,source_id)
    model=trainer(a,y)
    predicted=list(predictor(b,model))
    if len(predicted)!=len(z) or set(predicted)-set(y):
        raise ValueError("predictor output not comparable")
    classes=sorted(set(y))
    confusion={c:{d:0 for d in classes} for c in classes}
    for actual,pred in zip(z,predicted):
        confusion[actual][pred]+=1
    f1=[]
    for c in classes:
        tp=confusion[c][c];fp=sum(confusion[k][c] for k in classes if k!=c)
        fn=sum(confusion[c].values())-tp
        f1.append(2*tp/(2*tp+fp+fn) if 2*tp+fp+fn else 0.)
    return {"source_id":source_id,"method":model_name,"sensor_ids":s,
            "heldout_n":len(z),"heldout_accuracy":sum(p==t for p,t in zip(predicted,z))/len(z),
            "heldout_macro_f1":sum(f1)/len(f1),"confusion":confusion,
            "experimental_source_validated":False,"Tank_G3":False,
            "note":"Grouped result only; actual independent exposures require external source verification"}
