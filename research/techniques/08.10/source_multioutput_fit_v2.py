"""08.10 source-conditional multi-analyte ridge baseline, research only.

A regression on reference-labeled gas mixtures is not proof of independent
traceable ppm: the data steward must verify physical calibrant provenance.
"""
from math import isfinite
import numpy as np


def _label(s, name):
    if not isinstance(s, str) or not s.strip():
        raise ValueError(name + ' must be nonempty')
    return s


def _numbers(row, keys, name, nonnegative=False):
    if not isinstance(row, dict) or set(row) != set(keys):
        raise ValueError(name + ' keys must match source declaration')
    out = []
    for key in keys:
        v = row[key]
        if isinstance(v, bool) or not isinstance(v, (int, float)) or not isfinite(v) or (nonnegative and v < 0):
            raise ValueError(name + ' requires finite physical values')
        out.append(float(v))
    return out


def fit_multianalyte_source(train_rows, *, source_id, sensor_ids, gas_labels,
                            sensor_array_id, heater_phase, r0_reference_id,
                            ridge=1e-8, max_condition=1e4):
    """Fit a multioutput linear model, rejecting ill-conditioned sources."""
    for name, value in (('source_id', source_id), ('sensor_array_id', sensor_array_id),
                        ('r0_reference_id', r0_reference_id)):
        _label(value, name)
    if heater_phase not in ('HIGH', 'LOW', 'CONSTANT'):
        raise ValueError('known heater phase required')
    sensors = tuple(sensor_ids)
    gases = tuple(gas_labels)
    if (len(sensors) < 2 or len(gases) < 2 or len(set(sensors)) != len(sensors)
            or len(set(gases)) != len(gases)):
        raise ValueError('two or more unique sensors and gas outputs needed')
    for tag in sensors + gases:
        _label(tag, 'sensor or gas')
    if (isinstance(ridge, bool) or not isinstance(ridge, (int,float)) or
        not isfinite(ridge) or ridge < 0 or
        isinstance(max_condition, bool) or not isinstance(max_condition,(int,float)) or
        not isfinite(max_condition) or max_condition <= 1):
        raise ValueError('invalid regularization/condition parameter')
    rows=list(train_rows)
    if len(rows) < max(6, len(sensors)+3):
        raise ValueError('insufficient independent training exposures')
    sessions=set(); exposures=set(); X=[]; Y=[]
    for row in rows:
        if not isinstance(row,dict) or any(row.get(k)!=v for k,v in (
                ('source_id', source_id),('sensor_array_id', sensor_array_id),
                ('heater_phase', heater_phase),('r0_reference_id', r0_reference_id))):
            raise ValueError('incompatible source/device/heater/R0')
        session=_label(row.get('physical_session_id'),'physical_session_id')
        exp=_label(row.get('exposure_id'),'exposure_id')
        _label(row.get('reference_trace_id'),'reference_trace_id')
        if exp in exposures:
            raise ValueError('duplicated exposure source')
        sessions.add(session); exposures.add(exp)
        X.append(_numbers(row.get('features'),sensors,'sensor features'))
        Y.append(_numbers(row.get('concentration_ppm_by_gas'),gases,
                          'independently claimed reference labels',True))
    if len(sessions)<3:
        raise ValueError('must have >=3 independently labelled physical sessions')
    x=np.asarray(X,dtype=float); y=np.asarray(Y,dtype=float)
    x_mean=x.mean(axis=0); x_sd=x.std(axis=0)
    if np.any(x_sd <= 1e-12):
        raise ValueError('constant channel cannot separate gases')
    z=(x-x_mean)/x_sd
    singular=np.linalg.svd(z, compute_uv=False)
    if singular[-1] <= 1e-12 or singular[0]/singular[-1] > max_condition:
        raise ValueError('NOT_IDENTIFIABLE_SOURCE_COLLINEAR')
    y_mean=y.mean(axis=0)
    weights=np.linalg.solve(z.T@z + ridge*np.eye(len(sensors)),z.T@(y-y_mean))
    if not np.all(np.isfinite(weights)):
        raise ValueError('model coefficients out of range')
    return {'source_id':source_id,'sensor_array_id':sensor_array_id,
        'heater_phase':heater_phase,'r0_reference_id':r0_reference_id,
        'sensor_ids':list(sensors),'gas_labels':list(gases),
        'train_sessions':sorted(sessions),'train_exposures':sorted(exposures),
        'x_mean':x_mean.tolist(),'x_sd':x_sd.tolist(),
        'source_feature_min':x.min(axis=0).tolist(),
        'source_feature_max':x.max(axis=0).tolist(),
        'y_mean':y_mean.tolist(),'multioutput_coefficients':weights.tolist(),
        'source_condition_number':float(singular[0]/singular[-1]),
        'training_residual_MAE_optimistic_ppm':np.mean(abs(z@weights+y_mean-y),axis=0).tolist(),
        'independent_reference_certificate_verified':False,
        'Tank_G3':False,'ppm_approved':False,
        'status':'MULTIOUTPUT_FITTED_UNCALIBRATED_SOURCE_REFERENCE'}


def predict_source_heldout(model, features, *, source_id, sensor_array_id,
                           heater_phase, r0_reference_id, physical_session_id,
                           exposure_id):
    if any(model[k] != v for k,v in (
            ('source_id',source_id),('sensor_array_id',sensor_array_id),
            ('heater_phase',heater_phase),('r0_reference_id',r0_reference_id))):
        raise ValueError('source/model/R0/heater mismatch')
    _label(physical_session_id,'physical_session_id');_label(exposure_id,'exposure_id')
    if physical_session_id in model['train_sessions'] or exposure_id in model['train_exposures']:
        raise ValueError('source leakage: held-out exposure/session overlaps train')
    xs=_numbers(features,model['sensor_ids'],'heldout features')
    if any(a < lo or a > hi for a,lo,hi in zip(xs,model['source_feature_min'],model['source_feature_max'])):
        return {'status':'ABSTAIN_OUT_OF_SOURCE_RANGE','gas_ppm_approved':False,
                'estimated_component_ppm_research_only':None,'Tank_G3':False}
    values=(np.asarray(xs)-model['x_mean'])/model['x_sd']
    predicted=values@np.asarray(model['multioutput_coefficients'])+model['y_mean']
    if not np.all(np.isfinite(predicted)) or np.any(predicted < 0):
        return {'status':'ABSTAIN_UNPHYSICAL_LINEAR_PREDICTION','gas_ppm_approved':False,
                'estimated_component_ppm_research_only':None,'Tank_G3':False}
    return {'status':'UNCALIBRATED_MULTI_ANALYTE_SOURCE_PREDICTION',
            'estimated_component_ppm_research_only':dict(zip(model['gas_labels'],predicted.tolist())),
            'gas_ppm_approved':False, 'independent_reference_certificate_verified':False,
            'Tank_G3':False}
