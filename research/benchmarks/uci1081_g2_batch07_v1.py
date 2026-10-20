"""Fixed, exposure-disjoint exploratory G2-07 protocol; not Tank validation.

UCI1081 CC BY 4.0, DOI 10.24432/C5CK6F. All hyperparameters fixed before
reading held-out results. Three concentration holdouts. Within the remaining
60 exposures, replicate ordinal 4 reserves 12 calibration exposures, leaving
48 model exposures. Ordinal is a proxy, NOT a session/device identifier.
"""
import argparse,collections,json,math,statistics
from pathlib import Path
from uci1081_actual_modules_v1 import parse_archive,features,repo_module,SOURCE_SHA256,GASES,PPB
IDS=['04.5','04.7','04.8','06.1','06.2','06.4','06.6','06.8','06.9','08.12']

def partitions(rows,level):
    train=[i for i,r in enumerate(rows) if r['ppb']!=level and r['replicate_ordinal']!=4]
    cal=[i for i,r in enumerate(rows) if r['ppb']!=level and r['replicate_ordinal']==4]
    test=[i for i,r in enumerate(rows) if r['ppb']==level]
    if any(set(a)&set(b) for a,b in [(train,cal),(train,test),(cal,test)]):raise ValueError('overlapping exposures')
    if sorted(train+cal+test)!=list(range(len(rows))):raise ValueError('incomplete partition')
    if [len(train),len(cal),len(test)]!=[48,12,30]:raise ValueError('source inventory changed')
    return train,cal,test

def probability_rows(predictions,labels):
    return [[d['softmax_scores'][k] for k in labels] for d in predictions]

def fit_binary_likelihood(x,y,labels):
    threshold=statistics.median(r[0] for r in x)
    likelihood={k:(1+sum(v[0]>threshold for v,t in zip(x,y) if t==k))/(2+y.count(k)) for k in labels}
    return threshold,likelihood

def assess(rows):
    svm=repo_module('04.5','svm_v1.py');boost=repo_module('04.7','gradient_boosting_v1.py')
    mlp=repo_module('04.8','mlp_v1.py');soft=repo_module('04.4','logistic_regression_v1.py')
    fusion=repo_module('06.1','evidence_fusion_v1.py');bayes=repo_module('06.2','bayesian_inference_v1.py')
    conformal=repo_module('06.4','conformal_prediction_v1.py');ood=repo_module('06.6','ood_detection_v1.py')
    shap=repo_module('06.8','shap_v1.py');abstain=repo_module('06.9','abstention_v1.py')
    x=[features(r['trace'],'early_late_delta20') for r in rows];y=[r['gas'] for r in rows]
    folds=[]
    for level in PPB:
        train,cal,test=partitions(rows,level);xt=[x[i] for i in train];yt=[y[i] for i in train];xq=[x[i] for i in test]
        model=soft.fit_softmax(xt,yt,epochs=200,learning_rate=.1,l2=.01)
        labels=model['classes'];prior={k:yt.count(k)/len(yt) for k in labels}
        predictions=soft.predict_softmax(xq,model)
        sp=svm.decision_function(xq,svm.fit_linear_ovr_svm(xt,yt,epochs=500))
        bp=boost.predict_boosted_stumps(xq,boost.fit_boosted_stumps(xt,yt,rounds=30))
        mp=mlp.predict_mlp(xq,mlp.fit_mlp(xt,yt,hidden=8,epochs=400,seed=42))
        cp=soft.predict_softmax([x[i] for i in cal],model)
        cm=conformal.fit_split_conformal(probability_rows(cp,labels),[labels.index(y[i]) for i in cal],alpha=.1)
        sets=[conformal.predict_set(p,cm)['candidate_indices'] for p in probability_rows(predictions,labels)]
        coverage=conformal.evaluate_sets(sets,[labels.index(y[i]) for i in test])
        om=ood.fit(xt,margin=.1);flags=[ood.screen(om,q)['status']=='OUTSIDE_TRAINING_ENVELOPE' for q in xq]
        threshold,likelihood=fit_binary_likelihood(xt,yt,labels)
        bpred=[];fused=[];fusion_abstained=0;decisions=[]
        for j,i in enumerate(test):
            event=x[i][0]>threshold
            lk={k:v if event else 1-v for k,v in likelihood.items()}
            bpred.append(bayes.update_posterior(prior,[{'id':'one_binary_sensor_event','likelihoods':lk}])['ranking'][0])
            sources=[{'group':'same_exposure_same_training','probabilities':p[j]['softmax_scores']} for p in [predictions,mp]]
            fusion_abstained+=fusion.fuse_evidence(sources,prior=prior)['status']=='ABSTAIN'
            pooled=fusion.fuse_evidence(sources,prior=prior,minimum_sources=1)
            fused.append(pooled['ranking'][0])
            decisions.append(abstain.decide(predictions[j]['softmax_scores'],ood=flags[j]))
        # Explain a real measured query for a fixed 4-source-feature auxiliary
        # classifier, never relabel this as a full 20-feature explanation.
        sx=[v[:4] for v in xt];sm=soft.fit_softmax(sx,yt,epochs=200,learning_rate=.1,l2=.01)
        base=[statistics.mean(v[j] for v in sx) for j in range(4)]
        residuals=[]
        for i in test:
            q=x[i][:4];label=soft.predict_softmax([q],sm)[0]['predicted_label']
            def predict(z):return soft.predict_softmax([z],sm)[0]['softmax_scores'][label]
            residuals.append(abs(shap.explain(predict,base,q)['residual']))
        accepted=[j for j,d in enumerate(decisions) if d['candidate'] is not None]
        def correct(ps):return sum(p==y[i] for p,i in zip(ps,test))
        folds.append({'heldout_ppb':level,'train_ids':train,'calibration_ids':cal,'test_ids':test,
          '04.5_correct':correct([p['predicted_label'] for p in sp]),
          '04.7_boosted_stumps_NOT_XGBoost_correct':correct([p['predicted_label'] for p in bp]),
          '04.8_correct':correct([p['predicted_label'] for p in mp]),
          'softmax_baseline_correct':correct([p['predicted_label'] for p in predictions]),
          'majority_baseline_correct':correct([sorted(prior,key=lambda k:(-prior[k],k))[0]]*len(test)),
          '06.1_single_group_pool_correct':correct(fused),'06.1_default_gate_abstentions':fusion_abstained,
          '06.2_binary_event_bayes_correct':correct(bpred),
          '06.4':{**coverage,'empty_sets':sum(not s for s in sets),'threshold':cm['threshold']},
          '06.6_outside_envelope':sum(flags),
          '06.8_shapley_queries':len(residuals),'06.8_max_efficiency_residual':max(residuals),
          '06.9_accepted':len(accepted),'06.9_correct_accepted':sum(decisions[j]['candidate']==y[test[j]] for j in accepted)})
    if sorted(i for f in folds for i in f['test_ids'])!=list(range(90)):raise AssertionError('test exposure duplication')
    return {'source':'UCI1081','source_sha256':SOURCE_SHA256,'doi':'10.24432/C5CK6F','reviewed_ids':IDS,
     'external_numeric_ids':IDS[:-1],'blocked':{'08.12':'NO_WHOLE_PRODUCT_REFERENCE_LABELS'},'folds':folds,
     'unique_exposures':90,'Tank_G3_accepted':0,
     'limits':['No external sessions or devices; concentration folds are distribution shifts.',
       '04.7 remains residual boosted stumps, NOT XGBoost; 04.5 only linear OVR.',
       '06.1 pools dependent models in ONE group; default two-group gate abstains.',
       '06.2 uses one training-estimated binary-event likelihood, not posterior-as-likelihood.',
       '06.4 coverage descriptive only: concentration shift violates guaranteed exchangeability.',
       '06.6 flags known-gas concentration shifts, NOT unknown-gas sensitivity.',
       '06.8 exact baseline-dependent Shapley on first FOUR fixed source features, not causal.',
       'All scores uncalibrated; no Tank MQ8 ppm or alarm approval.']}

def main():
    p=argparse.ArgumentParser();p.add_argument('archive');p.add_argument('--output',default='g2_batch07.json');a=p.parse_args()
    r=assess(parse_archive(a.archive));Path(a.output).write_text(json.dumps(r,indent=2,allow_nan=False)+'\n')
    print(json.dumps(r,allow_nan=False))
if __name__=='__main__':main()
