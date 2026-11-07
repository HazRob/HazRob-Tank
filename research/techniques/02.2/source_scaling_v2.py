"""02.2 — fitted train-only sensor scaling contract and leakage protection.

Numeric baselines are never a physical MQ calibration and never reveal ppm.
"""
from dataclasses import dataclass
from math import isfinite, sqrt

def numeric(value):
    if isinstance(value,bool) or not isinstance(value,(int,float)) or not isfinite(value):
        raise ValueError("finite measured features required")
    return float(value)

def _rows(rows, n):
    data=list(rows)
    if not data or any(len(x)!=n for x in data):
        raise ValueError("nonempty fixed channel count required")
    return [tuple(numeric(x) for x in row) for row in data]

def _groups(ids, n):
    a=list(ids)
    if len(a)!=n or any(not isinstance(x,str) or not x.strip() for x in a):
        raise ValueError("one independent exposure-group ID required per feature row")
    return a

@dataclass(frozen=True)
class FittedSourceScaler:
    source_id: str
    sensor_ids: tuple
    training_group_ids: tuple
    center: tuple
    scale: tuple
    method: str
    no_ppm_approved: bool=True

    def __post_init__(self):
        if not self.source_id or not isinstance(self.source_id,str):
            raise ValueError("source_id required")
        if not self.sensor_ids or len(set(self.sensor_ids))!=len(self.sensor_ids):
            raise ValueError("unique sensor channel names required")
        if not self.training_group_ids:
            raise ValueError("training exposure groups required")
        if self.method not in ("standard","center_only"):
            raise ValueError("unsupported scaler")
        if not self.no_ppm_approved:
            raise ValueError("scaler is never a ppm calibration")

def fit_train_scaler(source_id, sensor_ids, train_rows, train_group_ids, *, method="standard"):
    sensors=tuple(sensor_ids)
    if not sensors or len(set(sensors))!=len(sensors) or any(not isinstance(s,str) or not s.strip() for s in sensors):
        raise ValueError("invalid channel identity")
    matrix=_rows(train_rows,len(sensors))
    groups=_groups(train_group_ids,len(matrix))
    if method not in ("standard","center_only"):
        raise ValueError("unsupported method")
    center=tuple(sum(row[k] for row in matrix)/len(matrix) for k in range(len(sensors)))
    scales=[]
    for col in range(len(sensors)):
        variance=sum((row[col]-center[col])**2 for row in matrix)/len(matrix)
        scales.append(max(sqrt(variance),1e-12) if method=="standard" else 1.)
    return FittedSourceScaler(source_id=source_id,sensor_ids=sensors,
        training_group_ids=tuple(sorted(set(groups))),center=center,scale=tuple(scales),method=method)

def transform_heldout(fitted, rows, group_ids, *, sensor_ids, source_id):
    if not isinstance(fitted,FittedSourceScaler):
        raise TypeError("fitted training scaler required")
    if tuple(sensor_ids)!=fitted.sensor_ids or source_id!=fitted.source_id:
        raise ValueError("sensor channel order and dataset source must match fitted model")
    matrix=_rows(rows,len(fitted.sensor_ids))
    heldout=_groups(group_ids,len(matrix))
    if set(heldout)&set(fitted.training_group_ids):
        raise ValueError("leakage: training exposure group reappeared in held-out")
    return [[(x-fitted.center[j])/fitted.scale[j] for j,x in enumerate(row)] for row in matrix]
