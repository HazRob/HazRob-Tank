#!/usr/bin/env python3
"""Offline research runner for the ORIGINAL pinned MQSpaceData 76-model engine.

Source 4D columns use time, Per (%), Temp (C), Rh (%). No Tank physical R0
conversion is performed. Input is ONE complete, monotone research run.
Outputs are source-style forecast candidates, NOT device ppm or certified
chemical identity. Upstream internally uses a mixture of shuffled KFold and
TimeSeriesSplit; its reported CV R2 is not an independent external holdout.
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import json
import math
import sys
from pathlib import Path

BASE = Path(__file__).resolve().parent
VENDOR = BASE / "mqspacedata_v7_upstream"
REVISION = "99c7262d8ce0fbd5f3aaee096cb0b469d2283637"

def source_rows(path):
    source = Path(path)
    with source.open("r", encoding="utf-8-sig", newline="") as f:
        rows = list(csv.DictReader(f))
    if not (16 <= len(rows) <= 10000):
        raise ValueError("16..10000 actual sequential source rows required")
    expected = {"Time", "Per", "Temp", "Rh"}
    if not expected.issubset(rows[0]):
        raise ValueError("CSV must contain original MQSpaceData columns Time,Per,Temp,Rh")
    result = []
    prev = -math.inf
    for index, row in enumerate(rows):
        values = tuple(float(row[key]) for key in ("Time", "Per", "Temp", "Rh"))
        time, pct, temperature, humidity = values
        if not all(math.isfinite(v) for v in values):
            raise ValueError("Undefined source row at "+str(index))
        if not time > prev:
            raise ValueError("Time must increase strictly; never concatenate restarted sessions")
        prev = time
        if not (0 <= pct <= 100 and -10 <= temperature <= 50 and 0 <= humidity <= 100):
            raise ValueError("Outside documented upstream sensor%,T,RH source domains")
        result.append(values)
    return source, result

def run(path, horizon_seconds, output):
    if not (0 < horizon_seconds <= 3600):
        raise ValueError("Future horizon must be within (0,3600] seconds")
    source, observations = source_rows(path)
    try:
        import numpy as np
    except ImportError as exc:
        raise RuntimeError("Missing numpy. Install pinned upstream requirements only in an isolated LAB virtualenv") from exc
    sys.path.insert(0, str(VENDOR))
    try:
        import PredictData  # Original, unchanged 76-model source
    except ImportError as exc:
        raise RuntimeError("Upstream scientific Python dependencies unavailable; no models executed") from exc
    t = np.array([v[0] for v in observations], dtype=float)
    per = np.array([v[1] for v in observations], dtype=float)
    temp = np.array([v[2] for v in observations], dtype=float)
    rh = np.array([v[3] for v in observations], dtype=float)
    spacing = float(np.median(np.diff(t)))
    if not (math.isfinite(spacing) and spacing > 0):
        raise ValueError("Cannot infer future sampling period")
    n_future = min(200, max(1, int(math.ceil(horizon_seconds / spacing))))
    time_future = t[-1] + spacing * np.arange(1, n_future+1)
    lines = []
    # No independent test labels were supplied. Never promote upstream CV R2
    # (which includes shuffled folds for many models) as a future accuracy.
    for name, vals in (("Per", per), ("Temp", temp), ("Rh", rh)):
        upstream_r2, forecast, upstream_model = PredictData.get_best_fit(
            t, vals, time_future, temp=temp, rh=rh)
        forecast = np.asarray(forecast, dtype=float)
        if len(forecast) != n_future or not np.all(np.isfinite(forecast)):
            raise RuntimeError("Non-finite/wrong length forecast: "+name)
        lines.append({
            "source_axis": name,
            "upstream_model": upstream_model,
            "upstream_internal_cv_r2": upstream_r2,
            "independent_grouped_holdout_r2": None,
            "forecast": [float(x) for x in forecast],
        })
    payload = {
        "schema": "hazrob-chemcore-v7-mqspacedata-76-lab-forecast/1",
        "upstream_repository": "abcdaaaaaaaaa/MQDataScience",
        "upstream_commit": REVISION,
        "upstream_version": "7.0.0",
        "script": "DataScience/Requirements/PredictData.py",
        "input_file_sha256": hashlib.sha256(source.read_bytes()).hexdigest(),
        "source_rows": len(observations),
        "source_columns": ["Time","Per","Temp","Rh"],
        "time_forecast_s": [float(x) for x in time_future],
        "selected_models": lines,
        "research_only": True,
        "source_percent_is_not_tank_rs_r0": True,
        "device_r0_calibration_claim": False,
        "tank_g3_qualified": False,
        "reportable_tank_ppm": False,
        "warning": "Upstream model selection includes shuffled CV; independently grouped source holdouts and reference gas exposure required before promoting results."
    }
    output = Path(output)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(payload,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print("MQSpaceData v7 research forecast:", output, "source points",len(observations))

def main():
    cli = argparse.ArgumentParser()
    cli.add_argument("--csv", required=True,
        help="ONE source-native time series: Time,Per,Temp,Rh (not Tank Rs/R0)")
    cli.add_argument("--horizon-s",required=True,type=float)
    cli.add_argument("--output",required=True)
    args = cli.parse_args()
    run(args.csv,args.horizon_s,args.output)

if __name__ == "__main__":
    main()
