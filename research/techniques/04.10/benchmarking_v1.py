"""Research-only grouped holdout benchmark metrics; no trained models or real data."""
from collections import Counter
from math import isfinite
def grouped_split(records, *, holdout_groups):
    """Explicit session/device group holdout; no row-wise random leakage."""
    if not isinstance(holdout_groups,(set,list,tuple)) or not holdout_groups:
        raise ValueError("holdout groups required")
    hold=set(holdout_groups)
    if len(hold)!=len(holdout_groups):raise ValueError("duplicate holdout groups")
    rows=list(records)
    if not rows or any(not isinstance(r,dict) or "group" not in r or "label" not in r for r in rows):
        raise ValueError("records require group and label")
    train=[r for r in rows if r["group"] not in hold]
    test=[r for r in rows if r["group"] in hold]
    if not train or not test:raise ValueError("both train and holdout must be nonempty")
    if {r["group"] for r in train}&{r["group"] for r in test}:
        raise ValueError("group leakage")
    return train,test

def classification_metrics(true_labels,predicted_labels,*,abstain_token="ABSTAIN"):
    """Accuracy, coverage, selective accuracy, per-class precision/recall/F1 and confusion counts."""
    y=list(true_labels);p=list(predicted_labels)
    if not y or len(y)!=len(p) or not isinstance(abstain_token,str) or not abstain_token:
        raise ValueError("invalid labels")
    if any(not isinstance(v,str) or not v for v in y+p) or abstain_token in y:
        raise ValueError("invalid label or abstain collision")
    classes=sorted(set(y)|({v for v in p if v!=abstain_token}))
    matrix={actual:{pred:0 for pred in classes+[abstain_token]} for actual in classes}
    for actual,pred in zip(y,p):matrix[actual][pred]+=1
    n=len(y);covered=sum(v!=abstain_token for v in p)
    correct=sum(a==b for a,b in zip(y,p))
    per_class={}
    for c in classes:
        tp=matrix[c][c]
        fp=sum(matrix[other][c] for other in classes if other!=c)
        fn=sum(matrix[c][other] for other in classes+[abstain_token] if other!=c)
        precision=tp/(tp+fp) if tp+fp else None
        recall=tp/(tp+fn) if tp+fn else None
        f1=2*precision*recall/(precision+recall) if precision is not None and recall is not None and precision+recall else (0. if precision is not None and recall is not None else None)
        per_class[c]={"support":sum(matrix[c].values()),"precision":precision,"recall":recall,"f1":f1}
    supported=[per_class[c] for c in classes if per_class[c]["support"]]
    balanced_recall=sum(z["recall"] for z in supported)/len(supported)
    return {"n":n,"accuracy_with_abstention_as_error":correct/n,
            "coverage":covered/n,"selective_accuracy":correct/covered if covered else None,
            "balanced_recall":balanced_recall,"per_class":per_class,"confusion":matrix,
            "warning":"Descriptive metrics only; no uncertainty interval or external validation."}

def compare_predictions(true_labels, predictions_by_model):
    """Compare already-generated predictions on exactly the same held-out labels."""
    if not isinstance(predictions_by_model,dict) or not predictions_by_model:
        raise ValueError("model predictions required")
    result={}
    for name,pred in sorted(predictions_by_model.items()):
        if not isinstance(name,str) or not name:raise ValueError("invalid model name")
        result[name]=classification_metrics(true_labels,pred)
    return result
