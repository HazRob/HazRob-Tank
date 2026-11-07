"""Frozen MQ group-aware scaler. Research source and channel order stay locked.

Fit on exposure/session GROUPS only, with each group weighted equally; raw
temporal rows are not independent events. Nothing is fitted on held-out data.
"""
from dataclasses import dataclass
from math import isfinite,sqrt


def check_num(v):
    if isinstance(v,bool) or not isinstance(v,(int,float)) or not isfinite(v):
        raise ValueError("nonfinite/bool value in scaled MQ features")
    return float(v)


@dataclass(frozen=True)
class FrozenGroupScaler:
    source_id: str
    sensor_order: tuple
    trained_group_ids: tuple
    centers: tuple
    scales: tuple
    def __post_init__(self):
        if not isinstance(self.source_id,str) or not self.source_id.strip():
            raise ValueError("source_id required")
        if not self.sensor_order or len(set(self.sensor_order))!=len(self.sensor_order):
            raise ValueError("missing/duplicate physical channel")
        if len(self.centers)!=len(self.scales) or len(self.centers)!=len(self.sensor_order):
            raise ValueError("channel dimension mismatch")
        if len(set(self.trained_group_ids))<2:
            raise ValueError("at least two independent training groups required")
        for s in self.scales:
            if check_num(s)<=0:raise ValueError("invalid scale")


def fit_group_scaler(source_id, sensor_order, measurements):
    order=tuple(sensor_order)
    if not order or len(set(order))!=len(order) or any(not isinstance(x,str) or not x.strip() for x in order):
        raise ValueError("explicit unique physical sensor order required")
    groups={}
    for m in measurements:
        if not isinstance(m,dict) or not isinstance(m.get("group_id"),str) or not m["group_id"].strip():
            raise ValueError("independent group metadata missing")
        data=m.get("values")
        if not isinstance(data,dict) or set(data)!=set(order):
            raise ValueError("channel count/order incompatible")
        groups.setdefault(m["group_id"],[]).append([check_num(data[k]) for k in order])
    if len(groups)<2:
        raise ValueError("fewer than two independent experiments")
    group_means=[]
    for rows in groups.values():
        group_means.append([sum(x[j] for x in rows)/len(rows) for j in range(len(order))])
    means=[sum(x[j] for x in group_means)/len(group_means) for j in range(len(order))]
    std=[sqrt(sum((x[j]-means[j])**2 for x in group_means)/len(group_means)) for j in range(len(order))]
    return FrozenGroupScaler(source_id,order,tuple(sorted(groups)),tuple(means),
                             tuple(s if s>0 else 1.0 for s in std))


def transform_heldout(model,records,*,source_id):
    if source_id!=model.source_id:
        raise ValueError("fitted scaler cannot transfer silently across datasets")
    records=list(records)
    if not records:raise ValueError("no heldout records")
    out=[]
    for row in records:
        if not isinstance(row,dict) or row.get("group_id") in model.trained_group_ids:
            raise ValueError("training group leakage")
        if not isinstance(row.get("group_id"),str) or not row["group_id"].strip():
            raise ValueError("missing heldout group")
        values=row.get("values")
        if not isinstance(values,dict) or set(values)!=set(model.sensor_order):
            raise ValueError("missing or unexpected sensor instance")
        out.append({"group_id":row["group_id"],
                    "values":{key:(check_num(values[key])-model.centers[j])/model.scales[j]
                              for j,key in enumerate(model.sensor_order)},
                    "scaler_source_id":model.source_id,
                    "physical_sensor_order":model.sensor_order,
                    "ppm_approved":False})
    return out
