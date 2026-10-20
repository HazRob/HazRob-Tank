"""Final ten-ID source eligibility audit; 05.8 numeric blank screening only.

Requirements are for the CURRENT modules and validated intended endpoints,
not a claim that outside datasets can never support related research.
"""
import argparse,json,statistics
from pathlib import Path
from uci1081_actual_modules_v1 import parse_archive,SOURCE_SHA256,repo_module,PPB,SENSORS
IDS=['04.1','04.9','05.2','05.3','05.8','06.7','09.4','09.5','09.6','10.1']
BLOCKS={
 '04.1':['matched MQ sensor curves with documented R0 protocol','independently controlled concentration and Rs/R0'],
 '04.9':['matched MQ curves and R0','candidate-blind complete physics-plus-ML implementation'],
 '05.2':['verified MQ Rs/R0 and at least three calibration levels with independent validation'],
 '05.3':['at least four distinct positive single-gas levels','MQ Rs/R0 provenance'],
 '06.7':['gas/concentration/condition-specific calibrated sensor detection power'],
 '09.4':['measured waypoint coordinates with calibrated uncertainty/risk','actual sampling outcomes'],
 '09.5':['time-synchronized robot pose and gas response','sensor transport lag and spatial ground truth'],
 '09.6':['synchronized obstacle/cliff/localization/communication telemetry and fault outcomes'],
 '10.1':['held-out sensor detection and non-detection likelihoods','validated multi-sensor dependence model']}

def baseline_summary(row,sensor):
    trace=row['trace'][sensor]
    return statistics.median(trace[180:300]),statistics.median(trace[780:900])

def screening(rows):
    m=repo_module('05.8','lod_loq_v1.py');results=[]
    for level in PPB:
        tr=[r for r in rows if r['ppb']!=level];te=[r for r in rows if r['ppb']==level]
        if (len(tr),len(te))!=(60,30):raise ValueError('source exposure inventory')
        for j,sensor in enumerate(SENSORS):
            train=[baseline_summary(r,j) for r in tr]
            direction='increasing' if statistics.mean(b-a for a,b in train)>=0 else 'decreasing'
            try:model=m.fit_blank_limits([a for a,b in train],sensor_id=sensor,gas_label='pooled_source_exposures_NOT_specific_gas',signal_unit='source_signal_units_unspecified',direction=direction)
            except ValueError as exc:
                # No invented sigma or forced floor for constant real baselines.
                if 'zero blank variance' not in str(exc):raise
                results.append({'heldout_ppb':level,'sensor':sensor,'status':'UNESTIMABLE_ZERO_BASELINE_VARIANCE'});continue
            counts={'blank_flagged':0,'exposure_flagged':0,'quantifiable':0}
            for row in te:
                a,b=baseline_summary(row,j)
                da=m.classify_signal(a,model);db=m.classify_signal(b,model)
                counts['blank_flagged']+=da['status']!='BELOW_SCREENING_LOD'
                counts['exposure_flagged']+=db['status']!='BELOW_SCREENING_LOD'
                counts['quantifiable']+=da['quantifiable'] or db['quantifiable']
            assert counts['quantifiable']==0
            results.append({'heldout_ppb':level,'sensor':sensor,'status':'DESCRIPTIVE_BASELINE_PROXY_ONLY','train_exposures':60,'test_exposures':30,'direction':direction,'sigma':model['blank_sd'],'threshold_source_units':model['lod_signal'],**counts})
    return results

def run(source):
    rows=parse_archive(source)
    return {'reviewed_ids':IDS,'blocked':BLOCKS,'external_numeric_ids':['05.8'],
      'source_sha256':SOURCE_SHA256,'source':'UCI1081','05.8':screening(rows),'Tank_G3_accepted':0,
      'restrictions':['One baseline summary per exposure, not serial samples counted as independent blanks.',
        'Baseline is a source-protocol proxy; no independently certified blank gas, no independent day/device IDs.',
        'Thresholds remain source units. Not formal LOD/LOQ, gas identity, concentration, or certified false alarm rate.',
        'UCI309 DOES provide Rs conversion and GCMS-estimated mean ppm, but TGS sensors are not Tank MQ sensors.',
        'UCI309 has only 3 positive levels per gas: 05.3 requires >=4. Do not count duplicate replicates as levels.',
        'UCI308 dR=(R-Rbaseline)/Rbaseline; 1+dR is a baseline ratio, not manufacturer MQ calibration R0.',
        'Physical blockers concern present inputs and intended Tank endpoints; reopen when matching data exists.']}
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('archive');p.add_argument('--output',default='g2_batch08.json');a=p.parse_args()
    result=run(a.archive);Path(a.output).write_text(json.dumps(result,indent=2,allow_nan=False)+'\n');print(json.dumps(result))
