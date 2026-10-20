"""G3-preparatory, G2 external array baseline for *genuine* LDA/PLS-DA, SVM, XGBoost.

Uses official UCI1081 90 real lab exposures; 3 concentration-level holdouts.
This is research-only source-domain predictive scoring, not Tank MQ8 calibration.
No hyperparameter selection on outer test data. Versions recorded.
"""
import argparse
import json
from pathlib import Path

from uci1081_actual_modules_v1 import GASES,parse_archive,features,splits
from sklearn.preprocessing import StandardScaler,LabelEncoder
from sklearn.discriminant_analysis import LinearDiscriminantAnalysis
from sklearn.cross_decomposition import PLSRegression
from sklearn.svm import SVC
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import confusion_matrix,accuracy_score,f1_score,balanced_accuracy_score
from xgboost import XGBClassifier
import numpy as np
import sklearn
import xgboost

METHODS=("majority","softmax_ref","lda_multiclass","plsda_multicomponent",
         "svm_linear","svm_rbf","xgboost_multiclass")

def fit_predict(method,x_train,y_train,x_test):
    """All label encoding / scaling is fitted *only* on train."""
    classes=np.unique(np.asarray(y_train,dtype=str))
    if len(classes)<2:raise ValueError("requires >1 known gas")
    if method=="majority":
        freq={c:y_train.count(c) for c in classes}
        best=min(classes,key=lambda c:(-freq[c],c))
        return [best]*len(x_test)
    encoder=LabelEncoder().fit(y_train)
    y=encoder.transform(y_train)
    scaler=StandardScaler().fit(x_train)
    xt=scaler.transform(x_train)
    xv=scaler.transform(x_test)
    if method=="softmax_ref":
        estimator=LogisticRegression(C=1.,max_iter=1200,random_state=19)
        estimator.fit(xt,y);z=estimator.predict(xv)
    elif method=="lda_multiclass":
        estimator=LinearDiscriminantAnalysis(solver="lsqr",shrinkage="auto")
        estimator.fit(xt,y);z=estimator.predict(xv)
    elif method=="plsda_multicomponent":
        n_comp=min(3,len(classes)-1,xt.shape[1],len(xt)-1)
        if n_comp<2:raise ValueError("requires >=2 PLS components")
        target=np.eye(len(classes))[y]
        estimator=PLSRegression(n_components=n_comp,scale=False,max_iter=500)
        estimator.fit(xt,target)
        z=np.argmax(estimator.predict(xv),axis=1)
    elif method in ("svm_linear","svm_rbf"):
        estimator=SVC(kernel="linear" if method=="svm_linear" else "rbf",
                      C=1.,gamma="scale",random_state=19)
        estimator.fit(xt,y);z=estimator.predict(xv)
    elif method=="xgboost_multiclass":
        estimator=XGBClassifier(n_estimators=35,max_depth=2,learning_rate=.1,
            subsample=1.,colsample_bytree=1.,reg_lambda=1.,
            objective="multi:softprob",num_class=len(classes),
            tree_method="hist",n_jobs=1,random_state=19)
        estimator.fit(xt,y);z=estimator.predict(xv)
    else:raise ValueError("unknown method")
    return encoder.inverse_transform(np.asarray(z,dtype=int)).tolist()

def run(source):
    rows=parse_archive(source)
    x=[features(r["trace"],"early_late_delta20") for r in rows]
    labels=[r["gas"] for r in rows]
    if len(x)!=90 or len(x[0])!=20:raise ValueError("bad sensor representation")
    folds=splits(rows,"leave_one_concentration_ppb")
    result={}
    for method in METHODS:
        true=[];pred=[];per_fold=[]
        for f in folds:
            ti,vi=f["train"],f["test"]
            ypred=fit_predict(method,[x[i] for i in ti],[labels[i] for i in ti],
                              [x[i] for i in vi])
            if len(ypred)!=len(vi):raise AssertionError("prediction mismatch")
            ytrue=[labels[i] for i in vi]
            true.extend(ytrue);pred.extend(ypred)
            per_fold.append({
                "heldout_ppb":f["heldout"],"n_train":len(ti),"n_test":len(vi),
                "correct":sum(a==b for a,b in zip(ytrue,ypred)),
                "accuracy":accuracy_score(ytrue,ypred)})
        if len(true)!=90:raise AssertionError("not all exposures tested")
        result[method]={
            "n_test_exposures":90,"correct":sum(a==b for a,b in zip(true,pred)),
            "accuracy":float(accuracy_score(true,pred)),
            "macro_f1":float(f1_score(true,pred,labels=GASES,average="macro",zero_division=0)),
            "balanced_accuracy":float(balanced_accuracy_score(true,pred)),
            "confusion_labels":list(GASES),
            "confusion":confusion_matrix(true,pred,labels=GASES).tolist(),
            "folds":per_fold
        }
    return {
      "dataset":"UCI1081 official CC BY 4.0 / DOI 10.24432/C5CK6F",
      "n_real_external_exposures":90,"sensor_schema":"10 source channels, NOT Tank MQ8",
      "features":"early/late signal delta fixed a priori, electrical units unverified",
      "protocol":"three leave-one-ppb-level-out folds; 60 training and 30 test exposures/fold; scaling training-only; fixed algorithms",
      "methods":list(METHODS),
      "versions":{"scikit_learn":sklearn.__version__,"xgboost":xgboost.__version__,
                  "numpy":np.__version__},
      "g3_tank_verified":False,
      "limitations":[
          "No device/session independence established; no Tank MQ8 resistances/R0/BME.",
          "All models trained on same 90 external exposures with small n; classification only.",
          "The 7 rows are configurations, not independent experiments.",
          "LDA and PLS-DA are real multiclass fitted estimators; SVM is true linear/RBF kernel SVC.",
          "XGBoost uses the actual library, not earlier 04.7 residual-boosted-stump prototype.",
          "These reported scores are exploratory and must not be used for real gas alarms or ppm.",
          "No hyperparameters tuned on heldout concentration folds; benchmark choices fixed before run."
      ],
      "results":result
    }

def main():
    p=argparse.ArgumentParser()
    p.add_argument("source_zip")
    p.add_argument("--output",required=True)
    a=p.parse_args()
    data=run(a.source_zip)
    Path(a.output).write_text(json.dumps(data,ensure_ascii=False,indent=2,allow_nan=False)+"\n",encoding="utf-8")
    for method,result in data["results"].items():
        print(method,result["correct"],"/",result["n_test_exposures"],
              "macro_f1",round(result["macro_f1"],4),
              "fold_correct",[f["correct"] for f in result["folds"]])
if __name__=="__main__":main()
