#!/usr/bin/env python3
"""HazRob ChemCore V6 *external source* MQ4 full-chain reproducible evaluation.

Original 2022 Minnesota CH4 LiCor reference measured vs 3 actual MQ4 sensors.
Independent whole-DAY train/calibration/held-out test; one gas only.
This is a *research harness*, NOT the Android ChemCoreTankV6 runtime, not G3.
"""
import argparse, csv, hashlib, io, json, os, pathlib, zipfile
import numpy as np
import pandas as pd
from sklearn.linear_model import Ridge
from sklearn.ensemble import HistGradientBoostingRegressor
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
from sklearn.pipeline import make_pipeline
from sklearn.preprocessing import StandardScaler

PINNED_SHA={
  'mq4':'3c27c4f8b8e8aab54b6433acfa41de00890a9eefd19376259a2371f22a48ff2d',
  'mobile':'9ff45148718003878bca102c28d53df2708b87c7371e7d4ba986ddfd0ba98dc8'}
MQ=['MQ4_1','MQ4_2','MQ4_3']


def input_files(src):
    src=pathlib.Path(src)
    if src.is_file() and src.suffix.lower()=='.zip':
        with zipfile.ZipFile(src) as z:
            files={key:[n for n in z.namelist() if (('umn_10ppm' in n) if key=='mq4' else ('mobile_aq_mq3' in n)) and n.endswith('.csv')]
                   for key in PINNED_SHA}
            assert all(len(v)==1 for v in files.values()),files
            raw={key:z.read(v[0]) for key,v in files.items()}
    elif src.is_dir():
        raw={'mq4':(src/'10ppm_data.csv').read_bytes(), 'mobile':(src/'sensor_log.csv').read_bytes()}
    else:raise ValueError('provide original-source .zip or directory with both raw files')
    for key,blob in raw.items():
        digest=hashlib.sha256(blob).hexdigest()
        if digest!=PINNED_SHA[key]:raise ValueError(f'FAIL CLOSED {key} SHA256 {digest} not pinned source')
    return raw


def main():
    ap=argparse.ArgumentParser();ap.add_argument('--source',required=True);ap.add_argument('--out',required=True)
    a=ap.parse_args();out=pathlib.Path(a.out);out.mkdir(parents=True,exist_ok=True)
    raw=input_files(a.source)
    d=pd.read_csv(io.BytesIO(raw['mq4']))
    expected=['datetime','licor_ch4','licor_h2o','t','rh','2600_1','2600_2','2600_3','2602_1','2602_2','2602_3','2611C_1','2611C_2','2611C_3','2611E_1','2611E_2','2611E_3']+MQ
    assert list(d.columns)==expected and len(d)==14186
    d['datetime']=pd.to_datetime(d['datetime'],errors='raise')
    assert d['datetime'].is_monotonic_increasing and not d.datetime.duplicated().any()
    for col in ['licor_ch4','t','rh']+MQ:assert d[col].notna().all() and np.isfinite(d[col].to_numpy()).all()
    assert (d.licor_ch4>0).all() and (d[MQ]>0).all().all()
    d['day']=d.datetime.dt.strftime('%Y-%m-%d')
    days=sorted(d.day.unique().tolist());assert len(days)==11 and days[0]=='2021-07-21' and days[-1]=='2021-07-31'
    target=d.licor_ch4.to_numpy()/1000.0 # LiCor ppb => ppm exact numerical conversion
    # Model features are strictly resistance, temperature and RH. No target or LiCor water vapor.
    logR=np.log(d[MQ].to_numpy())
    X={
      'MQ4_1_only':logR[:,:1],
      'MQ4_3_instances':logR,
      'MQ4_3_plus_environment':np.column_stack([logR,d[['t','rh']].to_numpy()]),
    }
    # All hyperparameters preregistered, NO test-based tuning or model selection.
    methods=['TRAIN_MEDIAN_REFERENCE','RIDGE_MQ4_1','RIDGE_MQ4_3','RIDGE_MQ4_3_T_RH','HGB_MQ4_3_T_RH']
    pred_rows=[];fold_rows=[];models_research=[]
    for fold,day in enumerate(days):
        # Fixed rotation gives exactly one independent calibration day, nine training days.
        calday=days[(fold-1)%len(days)]
        train=(d.day!=day).to_numpy()&(d.day!=calday).to_numpy()
        cal=(d.day==calday).to_numpy()
        test=(d.day==day).to_numpy()
        assert train.sum()+cal.sum()+test.sum()==len(d)
        assert len(set(d.loc[train,'day'])&set(d.loc[cal,'day']))==0
        assert len(set(d.loc[test,'day'])&set(d.loc[cal|train,'day']))==0
        def reg(name):
            if name=='TRAIN_MEDIAN_REFERENCE':
                val=float(np.median(target[train]));return np.full(cal.sum(),val),np.full(test.sum(),val)
            if name=='RIDGE_MQ4_1':xs=X['MQ4_1_only'];est=make_pipeline(StandardScaler(),Ridge(alpha=50))
            elif name=='RIDGE_MQ4_3':xs=X['MQ4_3_instances'];est=make_pipeline(StandardScaler(),Ridge(alpha=50))
            elif name=='RIDGE_MQ4_3_T_RH':xs=X['MQ4_3_plus_environment'];est=make_pipeline(StandardScaler(),Ridge(alpha=50))
            elif name=='HGB_MQ4_3_T_RH':xs=X['MQ4_3_plus_environment'];est=HistGradientBoostingRegressor(max_iter=85,max_leaf_nodes=12,max_depth=4,learning_rate=.06,l2_regularization=10.,random_state=20261030)
            else:raise ValueError(name)
            est.fit(xs[train],target[train]);return est.predict(xs[cal]),est.predict(xs[test])
        for model in methods:
            pc,pt=reg(model)
            assert len(pc)==cal.sum() and len(pt)==test.sum()
            assert np.isfinite(pt).all() and np.isfinite(pc).all()
            # Cal-only split conformal absolute-error residual. No refit or use of test labels.
            resid=np.abs(target[cal]-pc)
            quantile_idx=min(len(resid)-1,int(np.ceil((len(resid)+1)*.9))-1)
            q=float(np.sort(resid)[quantile_idx]);assert q>=0
            truth=target[test]
            ae=np.abs(truth-pt)
            coverage=float(np.mean(ae<=q))
            known_high=truth>=4.0
            response_high=pt>=4.0
            tp=int(np.sum(known_high&response_high));fn=int(np.sum(known_high&~response_high))
            fp=int(np.sum(~known_high&response_high));tn=int(np.sum(~known_high&~response_high))
            fold_rows.append({'test_day':day,'calibration_day':calday,'model':model,
                'n_train':int(train.sum()),'n_cal':int(cal.sum()),'n_test':int(test.sum()),
                'test_reference_ppm_min':float(truth.min()),'test_reference_ppm_max':float(truth.max()),
                'mae_ppm':float(mean_absolute_error(truth,pt)),
                'rmse_ppm':float(np.sqrt(mean_squared_error(truth,pt))),
                'bias_ppm':float(np.mean(pt-truth)), 'p90_abs_error_ppm':float(np.quantile(ae,.9)),
                'r2':float(r2_score(truth,pt)),'calibration_90pct_halfwidth_ppm':q,
                'empirical_interval_coverage':coverage,
                'reference_high_4ppm':int(known_high.sum()),'tp4':tp,'fn4':fn,'fp4':fp,'tn4':tn})
            idx=np.flatnonzero(test)
            for i,k in enumerate(idx):
                pred_rows.append({'test_day':day,'calibration_day':calday,'model':model,
                   'source_row_id':int(k),'datetime':str(d.datetime.iloc[k]),
                   'reference_ch4_ppm':float(truth[i]),'estimated_ch4_ppm_research':float(pt[i]),
                   'absolute_error_ppm':float(ae[i]),'calibration_halfwidth_ppm':q,
                   'interval_contains_reference':bool(ae[i]<=q),
                   'above_4ppm_reference':bool(known_high[i]),'above_4ppm_research_estimate':bool(response_high[i]),
                   'reported_identity':'UNKNOWN_NO_MULTI_GAS_VALIDATION','tank_g3_validated':False})
            print('DAY_TEST',day,model,'MAE',round(float(ae.mean()),4),'90pct_interval_coverage',round(coverage,3))
    folds=pd.DataFrame(fold_rows);preds=pd.DataFrame(pred_rows)
    folds.to_csv(out/'mq4_day_blocked_folds.csv',index=False)
    preds.to_csv(out/'mq4_day_blocked_predictions.csv',index=False)
    summaries=[]
    for model,group in preds.groupby('model',sort=False):
        t=group.reference_ch4_ppm.to_numpy();p=group.estimated_ch4_ppm_research.to_numpy();ae=np.abs(t-p)
        fs=folds[folds.model==model]
        summaries.append({'model':model,'original_source_test_predictions':len(group),'test_days':len(fs),
            'pooled_mae_ppm':float(ae.mean()),'pooled_rmse_ppm':float(np.sqrt(np.mean((t-p)**2))),
            'pooled_bias_ppm':float(np.mean(p-t)), 'pooled_median_ae_ppm':float(np.median(ae)),
            'mae_mean_across_11_days_ppm':float(fs.mae_ppm.mean()),
            'mae_std_across_days_ppm':float(fs.mae_ppm.std(ddof=1)),
            'pooled_interval_coverage':float(group.interval_contains_reference.mean()),
            'mean_day_interval_coverage':float(fs.empirical_interval_coverage.mean()),
            'total_4ppm_false_negative':int(fs.fn4.sum()),'total_4ppm_false_positive':int(fs.fp4.sum()),
            'total_4ppm_true_positive':int(fs.tp4.sum()),'total_4ppm_true_negative':int(fs.tn4.sum()),
            'source_only':True,'tank_g3':False})
    pd.DataFrame(summaries).to_csv(out/'mq4_day_blocked_summary.csv',index=False)
    # Additional no-leakage test: simultaneous withheld DAY and unseen sensor UNIT.
    # Models trained on 2 MQ4 instances on nine OTHER days. Scaling trained solely there.
    # Test third entirely unseen MQ4 unit on the 11th withheld day, with exactly 1 independent cal day.
    transfer=[]
    for testday in days:
        calday=days[(days.index(testday)-1)%11]
        train_days=((d.day!=testday)&(d.day!=calday)).to_numpy()
        test=(d.day==testday).to_numpy();cal=(d.day==calday).to_numpy()
        for unseen in MQ:
            available=[x for x in MQ if x!=unseen]
            # Expand two source train units into separate 2-column rows: logRs + same-time T, RH.
            # No same timestamp or day allowed in target test; unseen unit is never in training.
            xx=[];yy=[]
            for sensor in available:
                xx.append(np.column_stack([np.log(d.loc[train_days,sensor].to_numpy()),d.loc[train_days,['t','rh']].to_numpy()]))
                yy.append(target[train_days])
            Xt=np.vstack(xx);yt=np.concatenate(yy)
            model=make_pipeline(StandardScaler(),Ridge(alpha=60)).fit(Xt,yt)
            Q=np.column_stack([np.log(d.loc[cal,unseen].to_numpy()),d.loc[cal,['t','rh']].to_numpy()])
            Z=np.column_stack([np.log(d.loc[test,unseen].to_numpy()),d.loc[test,['t','rh']].to_numpy()])
            # Cal day of unseen unit only used for residual uncertainty, not training fit.
            cp=model.predict(Q);yp=model.predict(Z)
            rq=np.sort(np.abs(target[cal]-cp));q=float(rq[min(len(rq)-1,int(np.ceil((len(rq)+1)*.9))-1)])
            transfer.append({'test_day':testday,'calibration_day':calday,'heldout_sensor_instance':unseen,
                 'training_instances':'|'.join(available),'training_days':9,
                 'training_independent_timepoints':int(train_days.sum()),
                 'training_model_rows_repeated_two_instances':int(len(yt)),
                 'source_day_rows_test':int(test.sum()),
                 'test_mae_ppm':float(mean_absolute_error(target[test],yp)),
                 'test_rmse_ppm':float(np.sqrt(mean_squared_error(target[test],yp))),
                 'test_interval90_coverage':float(np.mean(np.abs(target[test]-yp)<=q)),
                 'source_only':True,'tank_g3':False})
    pd.DataFrame(transfer).to_csv(out/'mq4_unseen_instance_plus_day_transfer.csv',index=False)
    # Source readout QA includes 114 unlabelled 0.1s-cadence readings. No chemistry inference.
    mob=pd.read_csv(io.BytesIO(raw['mobile']))
    assert len(mob)==114 and list(mob.columns)==['Timestamp (ms)','MQ-3 Voltage','MQ-4 Voltage','MQ-8 Voltage','CO2 (ppm)','Temperature (C)','Humidity (%)']
    times=pd.to_datetime(mob['Timestamp (ms)'],errors='raise')
    dt=times.diff().dt.total_seconds().dropna().to_numpy()
    assert (dt>0).all()
    mobile={'source_rows':len(mob),'sensor_models':['MQ3','MQ4','MQ8'],
        'signal_columns':['MQ-3 Voltage','MQ-4 Voltage','MQ-8 Voltage'],'gas_identity_labels_available':False,
        'individual_gas_reference_concentrations_available':False,
        'other_equipment_co2_ppm_not_mq_reading':True,'environment_sentinel_-1_means_missing':True,
        'co2_valid_entries':int((mob['CO2 (ppm)']>=0).sum()),
        'temperature_valid_entries':int((mob['Temperature (C)']>=0).sum()),
        'humidity_valid_entries':int((mob['Humidity (%)']>=0).sum()),
        'median_sampling_gap_seconds':float(np.median(dt)),
        'sensor_positive_finite_count':int(np.isfinite(mob[['MQ-3 Voltage','MQ-4 Voltage','MQ-8 Voltage']].to_numpy()).sum()),
        'sensor_finite_positive_required':bool((mob[['MQ-3 Voltage','MQ-4 Voltage','MQ-8 Voltage']]>0).all().all()),
        'gas_identity_inferred':False,'ppm_from_MQ_inferred':False,'status':'SOURCE_QC_ONLY_NO_CHEMICAL_GROUND_TRUTH'}
    (out/'mq_mobile_source_signal_qc.json').write_text(json.dumps(mobile,ensure_ascii=False,indent=2)+'\n')
    # Actual input schema fault-injection test for the independent research adapter.
    # Not an Android service or physical disconnected MQ4 wiring test.
    def require_original_three_mq4(sensor_map):
        if not isinstance(sensor_map,dict):raise ValueError('original sensor mapping required')
        for channel in MQ:
            if channel not in sensor_map:raise ValueError('missing original MQ4 channel '+channel)
            val=sensor_map[channel]
            if not isinstance(val,(int,float,np.number)) or not np.isfinite(val) or val<=0:
                raise ValueError('invalid original MQ4 source ohm for '+channel)
        return True
    clean_source=d.loc[0,MQ].to_dict()
    assert require_original_three_mq4(clean_source)
    injected={k:v for k,v in clean_source.items() if k!='MQ4_2'}
    rejected_missing=False
    try:require_original_three_mq4(injected)
    except ValueError:rejected_missing=True
    assert rejected_missing, 'missing source channel must fail closed, not silently substitute old engine'
    injected_nan=dict(clean_source);injected_nan['MQ4_3']=float('nan')
    rejected_nonfinite=False
    try:require_original_three_mq4(injected_nan)
    except ValueError:rejected_nonfinite=True
    assert rejected_nonfinite, 'nonfinite MQ4 channel must fail closed'
    fault={'test_type':'SOURCE_SCHEMA_FAULT_INJECTION_ACTUALLY_EXECUTED_NOT_HARDWARE_TEST',
        'source_record_index':int(d.index[0]),'sensor_channels_expected':MQ,
        'fault_channel':'MQ4_2','missing_channel_rejected_by_code':rejected_missing,
        'nonfinite_channel_rejected_by_code':rejected_nonfinite,
        'three_channel_inference_allowed_when_missing':False,
        'fallback_to_old_engine_allowed':False,'source_only':True,'tank_g3':False}
    (out/'mq4_fault_injection_gate.json').write_text(json.dumps(fault,indent=2)+'\n')
    # Robust result metadata: no training/test reused source row IDs per fold, no food profiles.
    manifest={'schema':'chemcore-v6-mq4-external-full-system-source-benchmark/1',
        'source':'Furuta et al University of Minnesota methane reference MQ4 original measured physical source',
        'source_url':'https://hdl.handle.net/11299/226714',
        'original_source_sha256':PINNED_SHA['mq4'],'mobile_original_sha256':PINNED_SHA['mobile'],
        'source_rows':len(d),'reference_gas':'CH4','distinct_reference_gases':1,
        'source_reference_instrument':'Li-Cor CH4', 'source_reference_unit':'ppb; benchmark converted to ppm by dividing by 1000',
        'source_reference_range_ppm':[float(target.min()),float(target.max())],
        'independent_MQ4_sensor_instances':len(MQ), 'original_source_days':days,
        'gas_sensor_feature_input_columns':MQ, 'environment_feature_input_columns':['t','rh'],
        'feature_resistance_preprocessing':'natural log raw Rs ohms; scaler trained solely on 9 training days; NO inferred R0',
        'date_split_protocol':'leave one entire test day out; previous calendar day is calibration-only; remaining nine days train',
        'test_fold_count':11, 'models_per_day':methods,'source_test_predictions':len(preds),
        'simultaneous_unseen_day_and_sensor_unit_folds':len(transfer),
        'interval_protocol':'90 percent split conformal absolute residual estimated calibration day; future test untouched',
        'threshold_4ppm':'source-only research diagnostic, LiCor truth benchmark, NOT device exposure or safety alarm',
        'original_mobile_source_qc':mobile,
        'regulatory_safety_certification':False,'unknown_gas_identity_evaluable':False,
        'multi_gas_discrimination_evaluable':False,
        'real_time_Android_ChemCoreV6_routing_evaluated':False,
        'eighty_techniques_end_to_end_benchmarked':False,
        'physical_Tank_G3_validated':0,'food_freshness_data_used':False,
        'caveats':['Only CH4 source and three MQ4 instances, NOT full HazRob Tank eight-MQ array.',
            'Pooled one-minute rows autocorrelated; 11 day groups, independent calibrations limited.',
            'LiCor reference concentrations 1.95-10 ppm; no extrapolated field ppm claims.',
            'Reference methane and weather features are not directly physically measured on Tank.',
            'R0 unprovided; do not invent or transfer a per-device Tank R0.',
            'No independent unknown gas/challenge negatives in source; identity UNKNOWN.',
            'HGB/Ridge are sklearn research harness, NOT Java application ChemCore V6 runtime.',
            'Training on source device does not prove Tank sensor validity.']}
    (out/'source_manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
    print('PASS FULL SYSTEM MQ4 source study days',len(days),'model-day-folds',len(fold_rows),'source predictions',len(preds),'sensor-transfer folds',len(transfer))
    print('SUMMARY\n'+pd.DataFrame(summaries).to_string(index=False))
    print('TRANSFER\n'+pd.DataFrame(transfer).groupby('heldout_sensor_instance')[['test_mae_ppm','test_interval90_coverage']].mean().to_string())
    print('PASS MOBILE source no-ppm QC',len(mob),'valid_env',mobile['temperature_valid_entries'])

if __name__=='__main__':main()