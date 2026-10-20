"""G2-06: honest research audit of ten temporal/robotic technique IDs on UCI308.

UCI308 authentic external 16-channel dR signals, 58 exposures x 16 x 7500.
NO Tank MQ8 measurements, no verified plume encounters, no robot positions,
no wind, and no calibrated gas-phase ppm or neural network weights.
"""
import argparse
import collections
import csv
import gzip
import io
import json
import math
import statistics
import zipfile
from pathlib import Path

from uci1081_actual_modules_v1 import repo_module
from uci308_g2_batch05_v1 import (
    SHA256, SENSORS, validate_original, parse_features
)

IDS=("08.1","08.2","08.3","08.4","08.5","08.6","08.7",
     "09.1","09.2","09.3")
FOUR_LABELS=("acetone","ethanol","mixture","air")
GRU_WEIGHTS={"wz":0.1,"uz":0.1,"bz":0.0,
             "wr":0.1,"ur":0.1,"br":0.0,
             "wh":0.1,"uh":0.1,"bh":0.0}

def read_all_raw(path, feature_rows):
    """Validate every original value; retain 1 Hz 16-channel traces in RAM."""
    validate_original(path)
    master={int(r["exp"]):r for r in feature_rows}
    ret=collections.defaultdict(dict)
    n=0
    with zipfile.ZipFile(path) as z:
        with io.TextIOWrapper(gzip.GzipFile(fileobj=z.open("rawdata.csv.gz")),
                              encoding="utf-8-sig",newline="") as file:
            reader=csv.DictReader(file)
            if len(reader.fieldnames)!=7509 or "dR_t7500" not in reader.fieldnames:
                raise ValueError("unexpected original time series schema")
            for row in reader:
                if None in row or None in row.values():
                    raise ValueError("malformed original raw signal record")
                exp=int(row["exp"]);sensor=int(row["sensor"])
                if exp not in master or sensor not in range(1,17):
                    raise ValueError("unexpected exp/sensor")
                if sensor in ret[exp]:
                    raise ValueError("duplicate exposure sensor")
                record=master[exp]
                if any(row[k]!=record[k] for k in ("batch","gas","lab")):
                    raise ValueError("raw/summary annotation mismatch")
                signal=[]
                for t in range(1,7501):
                    v=float(row["dR_t%d"%t])
                    if not math.isfinite(v):
                        raise ValueError("nonfinite raw dR")
                    if (t-1)%25==0:
                        signal.append(v)
                if len(signal)!=300:
                    raise ValueError("expected 300 downsampled points per trace")
                ret[exp][sensor]=signal
                n+=1
    if n!=928 or set(ret)!=set(master) or any(set(d)!=set(range(1,17))
                                          for d in ret.values()):
        raise ValueError("source must have 58 exposures x 16 sensors")
    return ret,n

def signal_primitives(raw,rows):
    cnn=repo_module("08.1","cnn_1d_v1.py")
    tcn=repo_module("08.2","tcn_v1.py")
    gru=repo_module("08.3","lstm_gru_v1.py")
    graph=repo_module("08.4","gnn_v1.py")
    simulator=repo_module("08.6","simulation_v1.py")
    gap=repo_module("08.7","sim_to_real_v1.py")
    olfaction=repo_module("09.1","robotic_olfaction_v1.py")
    # Sensor index-neighborhood is invented for a NUMERIC graph check, not
    # a known physical/sensor-crosstalk adjacency!
    edges=[(i,i+1) for i in range(15)]
    cnn_vals=[];tcn_vals=[];gru_vals=[];graph_vals=[]
    gaps=[];events=0;nonempty=0
    simulated_exposures=[0.]*30+[1.]*270
    prototype=simulator.simulate(simulated_exposures,0.1,
                                 baseline=0.0,tau_seconds=5.,
                                 step_seconds=1.)["responses"]
    if len(prototype)!=300:
        raise AssertionError("simulation shape")
    for row in rows:
        exp=row["exp"]
        # Input data are real source dR signals but neither physical log Rs
        # nor neural-model training inputs.
        nodes=graph.aggregate([[v] for v in row["max_dR"]],edges,True)
        graph_vals.append(statistics.mean(v[0] for v in nodes))
        channel_outputs=[]
        for sensor in range(1,17):
            trace=raw[exp][sensor]
            base=statistics.median(trace[:30])
            centered=[v-base for v in trace]
            norm=max(1.,max(abs(v) for v in centered))
            standardized=[v/norm for v in centered]
            a=cnn.convolve_valid(standardized,[.25,.5,.25],
                                 activation="linear")
            cnn_vals.append(cnn.global_average_pool(a))
            b=tcn.causal_conv(standardized,[.5,.5],dilation=4)
            tcn_vals.append(b[-1])
            state=0.
            for v in standardized:
                state=gru.gru_scalar_step(v,state,GRU_WEIGHTS)
            if not math.isfinite(state):
                raise ValueError("unstable deterministic GRU forward")
            gru_vals.append(state)
            channel_outputs.append(standardized)
        # Synthetic first-order output compared to real S1 standardized dR:
        # dimensional similarity only, not any calibrated simulator.
        result=gap.compare(
            [prototype,prototype],
            [channel_outputs[0],
             channel_outputs[0]])
        gaps.append(result["mean_gap_rmse"])
        # 09.1 threshold flags within original dR S1, but test rig is
        # stationary; absence of robot pose/wind means NOT robotic olfaction.
        trace=raw[exp][1]
        base=statistics.median(trace[:30])
        events_here=olfaction.encounters(trace,base,threshold=.05)["events"]
        events+=len(events_here)
        nonempty+=int(bool(events_here))
    if len(cnn_vals)!=928 or len(tcn_vals)!=928 or len(gru_vals)!=928:
        raise AssertionError("each sensor signal must be processed once")
    if len(graph_vals)!=58 or len(gaps)!=58:
        raise AssertionError("each authentic exposure must be processed once")
    def metrics(v):
        if any(not math.isfinite(x) for x in v):
            raise ValueError("unexpected nonfinite numeric output")
        return {"n":len(v),"median":statistics.median(v),
                "minimum":min(v),"maximum":max(v)}
    return {
        "08.1_numeric_convolution":{"status":"EXTERNAL_SIGNAL_FORWARD_ONLY_NOT_TRAINED_CNN",
                                    **metrics(cnn_vals)},
        "08.2_numeric_causal_convolution":{"status":"EXTERNAL_SIGNAL_FORWARD_ONLY_NOT_TRAINED_TCN",
                                           **metrics(tcn_vals)},
        "08.3_numeric_GRU_cell":{"status":"EXTERNAL_SIGNAL_FORWARD_ONLY_NOT_TRAINED_GRU_OR_LSTM",
                                 **metrics(gru_vals)},
        "08.4_numeric_graph_aggregation":{"status":"ARBITRARY_SENSOR_INDEX_GRAPH_NOT_LEARNED_OR_PHYSICAL",
                                         "fixed_edges":edges,**metrics(graph_vals)},
        "08.6_simulator":{"status":"SYNTHETIC_ONLY_NOT_MEASURED_OR_CALIBRATED",
                          "simulated_points":len(prototype),
                          "fictional_input_zero_then_unit_step":True},
        "08.7_sim_vs_real_gap":{"status":"NUMERIC_ONLY_INCOMPATIBLE_PHYSICAL_INPUTS",
                               "simulated_unitless_vs_measured_dR":True,
                               **metrics(gaps)},
        "09.1_numeric_events":{"status":"EXTERNAL_STATIONARY_TEST_RIG_NOT_ROBOTIC_PLUME",
                               "real_sensor_traces_examined":58,
                               "n_with_event":nonempty,
                               "n_events":events,
                               "fixed_source_dR_threshold":.05}
    }

def temporal_ensemble_by_batch(rows,raw):
    """Four-category experimental label CV, train on whole source batches only.

    08.5 combines posterior rows from 3 disjoint temporal windows.
    Logistic 04.4 is fitted ONLY on training-batch windows; a toy trained
    auxiliary classifier, not a CNN/TCN/GRU model.
    """
    ensemble=repo_module("08.5","temporal_ensemble_v1.py")
    softmax=repo_module("04.4","logistic_regression_v1.py")
    windows=((0,100),(100,200),(200,300))
    rows_by_exp={}
    for row in rows:
        exp=row["exp"];items=[]
        for start,end in windows:
            items.append([statistics.mean(raw[exp][sensor][start:end])
                          for sensor in range(1,17)])
        rows_by_exp[exp]=items
    batches=sorted({r["batch"] for r in rows})
    outputs=[]
    for heldout in batches:
        train=[r for r in rows if r["batch"]!=heldout]
        test=[r for r in rows if r["batch"]==heldout]
        train_x=[v for r in train for v in rows_by_exp[r["exp"]]]
        train_y=[r["gas"] for r in train for _ in windows]
        if set(train_y)!=set(FOUR_LABELS):
            raise ValueError("heldout batch training lacks 4 source labels")
        model=softmax.fit_softmax(train_x,train_y,epochs=150,
                                  learning_rate=.07,l2=.02)
        labels=model["classes"]
        count_early=0;count_late=0;count_ens=0
        for r in test:
            # Match posterior columns explicitly to the model class order.
            score_dict=softmax.predict_softmax(rows_by_exp[r["exp"]],model)
            proba=[[d["softmax_scores"][label] for label in labels]
                   for d in score_dict]
            ens=ensemble.ensemble(proba,weights=[1.,1.,1.])
            guess=labels[ens["candidate_index"]]
            count_ens+=int(guess==r["gas"])
            count_early+=int(labels[max(range(4),key=lambda j:proba[0][j])]==r["gas"])
            count_late+=int(labels[max(range(4),key=lambda j:proba[-1][j])]==r["gas"])
        outputs.append({"heldout_batch":heldout,"training_exposures":len(train),
                        "test_exposures":len(test),
                        "correct_early_window":count_early,
                        "correct_late_window":count_late,
                        "correct_ensemble":count_ens})
    tested=sum(v["test_exposures"] for v in outputs)
    if tested!=len(rows):raise AssertionError("all source experiments must be tested once")
    return {"status":"TRAINED_AUXILIARY_SOFTMAX_PLUS_08.5_TEMPORAL_AGGREGATION_NOT_CALIBRATED",
            "folds":outputs,"n_heldout_source_exposures":tested,
            "n_correct_early_window":sum(v["correct_early_window"] for v in outputs),
            "n_correct_late_window":sum(v["correct_late_window"] for v in outputs),
            "n_correct_ensemble":sum(v["correct_ensemble"] for v in outputs),
            "source_categories":list(FOUR_LABELS),
            "warning":"Windows are from same exposure; averaged predictions are correlated, not independent evidence."}

def run(source):
    summary=parse_features(source)
    raw,n=read_all_raw(source,summary)
    primitives=signal_primitives(raw,summary)
    ensemble=temporal_ensemble_by_batch(summary,raw)
    blocked={
      "09.2":"BLOCKED_NO_ROBOT_POSE_XY_OR_DISTANCE_SPATIAL_CELLS",
      "09.3":"BLOCKED_NO_ROBOT_POSITION_WIND_OR_VERIFIED_LEAK_SOURCE"
    }
    if len(IDS)!=10 or len(set(IDS))!=10:
        raise AssertionError("must review exactly ten G2 IDs")
    if set(IDS)!=set(blocked)|{"08.1","08.2","08.3","08.4","08.5",
                                "08.6","08.7","09.1"}:
        raise AssertionError("technique inventory mismatch")
    return {
      "source":"UCI308 authentic flow-modulated laboratory exposure data",
      "doi":"10.24432/C5BG7G",
      "source_sha256":SHA256,
      "measured_exposures":58,"original_sensors":16,
      "original_measurements_per_sensor":7500,
      "verified_original_scalar_points":n*7500,
      "numerical_downsample_hz":1,
      "source_dR_unit":"NOT calibrated log Rs/R0, NOT Tank MQ8",
      "source_gas_physical_concentration_ppm":None,
      "techniques_reviewed":list(IDS),
      "signal_or_numeric_ids":["08.1","08.2","08.3","08.4","08.5",
                               "08.7","09.1"],
      "synthetic_only_ids":["08.6"],
      "blocked_ids":list(blocked),
      "physical_blocks":blocked,
      "signal_forward_audit":primitives,
      "08.5_time_batch_heldout":ensemble,
      "notes":[
        "08.1,08.2,08.3,08.4 only execute fixed numerical forwards. There is NO trained CNN, TCN, GRU, or GNN.",
        "08.6 produces hypothetical concentrations unrelated to actual source gas ppm; not a physically calibrated simulator.",
        "08.7 compares incompatible simulation and dR measurements; numerical gap only, NOT sim-to-real transfer evidence.",
        "09.1 thresholds source test-rig time pulses; this is NOT a robotic plume encounter or leak localization.",
        "09.2 and 09.3 cannot run on UCI308 without inventing robot pose, spatial coordinates or wind.",
        "08.5 uses independently learned auxiliary 04.4 on source training batches; source-time-batch folds, not independent devices.",
        "No HazRob Tank 8xMQ reference concentration/R0/BME/heater/gas safety evidence; G3=0."
      ]
    }

def main():
    p=argparse.ArgumentParser()
    p.add_argument("original_uci308_zip")
    p.add_argument("--output",default="uci308_g2_batch06_results.json")
    args=p.parse_args()
    result=run(args.original_uci308_zip)
    Path(args.output).write_text(json.dumps(result,ensure_ascii=False,
                                            allow_nan=False,indent=2)+"\n",
                                encoding="utf-8")
    print("G2-06 verified",result["measured_exposures"],"real exposures,",
          result["verified_original_scalar_points"],"raw numerical readings")
    for k,v in result["signal_forward_audit"].items():
        print(k,json.dumps(v))
    print("08.5",json.dumps(result["08.5_time_batch_heldout"]))
    print("BLOCKED",result["physical_blocks"])
if __name__=="__main__":
    main()
