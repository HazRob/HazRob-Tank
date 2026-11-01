"""Original-source MQ4 forward-only 4ppm benchmark; research, NOT Tank G2/G3."""
import argparse,hashlib,io,json,math,zipfile
from datetime import date
from pathlib import Path

ZIP_SHA='7112a370c7b21445f9e6ebb6d50e08059c352290ee6da23b0b735f5a9583afc9'
CSV_SHA='3c27c4f8b8e8aab54b6433acfa41de00890a9eefd19376259a2371f22a48ff2d'
MEMBER='raw/MQ-ACQ-2026-06_umn_10ppm_data_csv.csv'
MODELS=['TRAIN_MEDIAN_REFERENCE','RIDGE_MQ4_1','RIDGE_MQ4_3','RIDGE_MQ4_3_T_RH','HGB_MQ4_3_T_RH']
DAYS=[f'2021-07-{n:02d}' for n in range(21,32)]


def plan(days=DAYS):
    if len(days)<5 or days!=sorted(set(days)) or any((date.fromisoformat(b)-date.fromisoformat(a)).days!=1 for a,b in zip(days,days[1:])):
        raise ValueError('NONCHRONOLOGICAL_DAYS')
    return [{'train':days[:i-1],'cal':days[i-1],'test':days[i]} for i in range(4,len(days))]


def score(truth,pred,q):
    if len(truth)==0 or len(truth)!=len(pred) or not math.isfinite(q) or q<0:
        raise ValueError('INVALID_SCORE')
    if not all(math.isfinite(float(x)) for x in list(truth)+list(pred)):
        raise ValueError('NONFINITE_SCORE')
    tp=sum(t>=4 and p>=4 for t,p in zip(truth,pred))
    fn=sum(t>=4 and p<4 for t,p in zip(truth,pred))
    fp=sum(t<4 and p>=4 for t,p in zip(truth,pred))
    tn=len(truth)-tp-fn-fp
    ae=[abs(t-p) for t,p in zip(truth,pred)]
    return {'n':len(truth),'pos':tp+fn,'tp':tp,'fn':fn,'fp':fp,'tn':tn,
            'mae':sum(ae)/len(ae),'coverage':sum(x<=q for x in ae)/len(ae)}


def evaluate(archive):
    if hashlib.sha256(archive).hexdigest()!=ZIP_SHA:raise ValueError('ZIP_SHA')
    with zipfile.ZipFile(io.BytesIO(archive)) as z:
        if z.namelist().count(MEMBER)!=1:raise ValueError('CSV_MEMBER')
        raw=z.read(MEMBER)
    if hashlib.sha256(raw).hexdigest()!=CSV_SHA:raise ValueError('CSV_SHA')
    import numpy as np
    import pandas as pd
    from sklearn.linear_model import Ridge
    from sklearn.ensemble import HistGradientBoostingRegressor
    from sklearn.pipeline import make_pipeline
    from sklearn.preprocessing import StandardScaler
    d=pd.read_csv(io.BytesIO(raw))
    if len(d)!=14186 or not {'datetime','licor_ch4','t','rh','MQ4_1','MQ4_2','MQ4_3'}.issubset(d):raise ValueError('SCHEMA')
    dt=pd.to_datetime(d.datetime,errors='raise')
    if not dt.is_monotonic_increasing or dt.duplicated().any():raise ValueError('TIME')
    if not np.isfinite(d[['licor_ch4','t','rh','MQ4_1','MQ4_2','MQ4_3']].to_numpy()).all():raise ValueError('NONFINITE')
    if not (d[['licor_ch4','MQ4_1','MQ4_2','MQ4_3']]>0).all().all():raise ValueError('NONPOSITIVE')
    day=dt.dt.strftime('%Y-%m-%d').to_numpy()
    if sorted(set(day))!=DAYS:raise ValueError('DAYS')
    y=d.licor_ch4.to_numpy()/1000
    log=np.log(d[['MQ4_1','MQ4_2','MQ4_3']].to_numpy())
    env=np.column_stack((log,d[['t','rh']].to_numpy()))
    x={'RIDGE_MQ4_1':log[:,:1],'RIDGE_MQ4_3':log,'RIDGE_MQ4_3_T_RH':env,'HGB_MQ4_3_T_RH':env}
    folds=plan()
    result={'schema':'hazrob.g2.mq4-forward-4ppm-source.v1','zip_sha256':ZIP_SHA,'csv_sha256':CSV_SHA,
            'source_rows':len(d),'test_days':[f['test'] for f in folds],
            'positive_by_test_day':{f['test']:int(sum(y[day==f['test']]>=4)) for f in folds},
            'protocol':'expand past train days; immediately previous full day calibration; fixed models; no future fit',
            'threshold_4ppm_is_safety_limit':False,'source_only_not_tank':True,
            'independent_tank_exposures':False,'blinded_prospective':False,'traceable_calibration':False,
            'g2_qualified':False,'g3_qualified':False,'models':{}}
    for name in MODELS:
        rows=[]
        for f in folds:
            tr=np.isin(day,f['train']);cal=day==f['cal'];te=day==f['test']
            if not (tr.any() and cal.any() and te.any() and not (tr&cal).any()):raise ValueError('SPLIT')
            if name=='TRAIN_MEDIAN_REFERENCE':
                v=float(np.median(y[tr]));pc=np.full(cal.sum(),v);pt=np.full(te.sum(),v)
            else:
                model=(make_pipeline(StandardScaler(),Ridge(alpha=50)) if name.startswith('RIDGE') else
                    HistGradientBoostingRegressor(max_iter=85,max_leaf_nodes=12,max_depth=4,learning_rate=.06,l2_regularization=10.,random_state=20261030))
                model.fit(x[name][tr],y[tr]);pc=model.predict(x[name][cal]);pt=model.predict(x[name][te])
            resid=np.sort(np.abs(y[cal]-pc));k=math.ceil((len(resid)+1)*.9)
            if k>len(resid):raise ValueError('CALIBRATION_SUPPORT')
            metrics=score(y[te].tolist(),pt.tolist(),float(resid[k-1]))
            rows.append({'day':f['test'],'train_days':len(f['train']),**metrics})
        n=sum(r['n'] for r in rows)
        agg={k:sum(r[k] for r in rows) for k in ['tp','fn','fp','tn','pos']}
        agg.update(n=n,mae=sum(r['mae']*r['n'] for r in rows)/n,
                   sensitivity=agg['tp']/agg['pos'] if agg['pos'] else None,
                   specificity=agg['tn']/(agg['tn']+agg['fp']) if agg['tn']+agg['fp'] else None)
        result['models'][name]={'aggregate':agg,'days':rows}
    return result


def compact(r):
    return {**{k:v for k,v in r.items() if k!='models'},
            'test_rows_by_day':{f['day']:f['n'] for f in r['models'][MODELS[0]]['days']},
            'models':{name:{'aggregate':v['aggregate'],
                            'days':[{k:f[k] for k in ('day','pos','fn','fp','mae')} for f in v['days']]}
                      for name,v in r['models'].items()}}


def main():
    p=argparse.ArgumentParser();p.add_argument('--archive',required=True,type=Path);p.add_argument('--out',required=True,type=Path);p.add_argument('--compact',action='store_true')
    a=p.parse_args();r=evaluate(a.archive.read_bytes())
    if a.compact:r=compact(r)
    a.out.write_text(json.dumps(r,sort_keys=True,ensure_ascii=False,separators=(',',':'),allow_nan=False)+'\n',encoding='utf-8')
    print('PASS original MQ4 7-day forward benchmark; G2/G3 BLOCKED')


if __name__=='__main__':main()
