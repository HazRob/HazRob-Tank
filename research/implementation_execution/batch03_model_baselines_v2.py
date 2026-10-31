"""Batch03: frozen baseline models with exposure-group disjointness.

Methods intentionally named after their ACTUAL implementations:
04.5 linear OVR SVM, 04.7 boosted stumps (NOT XGBoost).
"""
import importlib.util
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
_validation_spec=importlib.util.spec_from_file_location(
    "tank_v4_group_validator",Path(__file__).with_name("grouped_model_validation_v2.py"))
_validation=importlib.util.module_from_spec(_validation_spec)
_validation_spec.loader.exec_module(_validation)
score_classifier=_validation.score_classifier
SPECS={
 "04.3":("knn_v1.py","KNN","fit_knn","predict_knn"),
 "04.4":("logistic_regression_v1.py","SOFTMAX","fit_softmax","predict_softmax"),
 "04.5":("svm_v1.py","LINEAR_OVR_SVM","fit_linear_ovr_svm","decision_function"),
 "04.6":("random_forest_v1.py","RANDOM_FOREST","fit_forest","predict_forest"),
 "04.7":("gradient_boosting_v1.py","BOOSTED_STUMPS_NOT_XGBOOST","fit_boosted_stumps","predict_boosted_stumps"),
 "04.8":("mlp_v1.py","MLP_ONE_HIDDEN_LAYER","fit_mlp","predict_mlp")
}

def evaluate_method(technique_id, train, labels, groups, test, truth, heldout_groups,
                    sensor_ids, *, source_id):
    if technique_id not in SPECS:
        raise ValueError("unregistered research method")
    fname,label,fit_name,predict_name=SPECS[technique_id]
    path=ROOT/"techniques"/technique_id/fname
    spec=importlib.util.spec_from_file_location("tank_v1_"+technique_id.replace(".","_"),path)
    module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
    fit=getattr(module,fit_name);predict=getattr(module,predict_name)
    def trainer(x,y):
        if technique_id=="04.3":
            return fit(x,y,sensor_ids)
        if technique_id=="04.4":
            return fit(x,y,epochs=200)
        if technique_id=="04.5":
            return fit(x,y,epochs=200)
        if technique_id=="04.6":
            return fit(x,y,n_trees=15,max_depth=4,min_leaf=1,seed=42)
        if technique_id=="04.8":
            return fit(x,y,hidden=6,epochs=250,learning_rate=.08,seed=42)
        return fit(x,y,rounds=16,learning_rate=.25)
    def predictor(x,model):
        if technique_id=="04.3":
            return [predict(row,model,k=min(3,len(model["rows"])))["predicted_label"] for row in x]
        return [result["predicted_label"] for result in predict(x,model)]
    result=score_classifier(train,labels,groups,test,truth,heldout_groups,sensor_ids,
               source_id=source_id,model_name=label,trainer=trainer,predictor=predictor)
    result["technique_id"]=technique_id
    result["algorithm_upgraded_from_v1"]=False
    result["V4_grouped_source_contract"]=True
    result["hyperparameter_selection_with_heldout"]=False
    return result
