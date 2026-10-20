"""G2-06 UCI309 real wind-tunnel signal audit for ten stage-08/09 techniques.

Authentic UCI309 180 exposures, 8 Figaro TGS sensors + T/RH, source
GC-MS mean concentration *levels* (not instantaneous ppm).
180 downsample files and 180 raw files refer to the SAME exposures.
This is research-only, not HazRob Tank MQ8 calibration or trained CNN/TCN/GNN.
"""
import argparse, collections, hashlib, io, json, math, re, statistics, zipfile
from pathlib import Path
from uci1081_actual_modules_v1 import repo_module

SHA256="5e9b707e7a44b3dcaf39b62bb46597403fcdd98ec89814b15e4876694db5f41e"
DOI="10.24432/C5JS5P"
SENSORS=("TGS2600","TGS2602_A","TGS2602_B","TGS2620_A",
         "TGS2612","TGS2620_B","TGS2611","TGS2610")
EXPERIMENT_REGEX=re.compile(r"(\d{3})_Et_([nLMH])_(CO|Me)_([nLMH])")
NOMINAL_PPM={"Et":{"n":0.,"L":31.,"M":46.,"H":96.},
             "CO":{"n":0.,"L":270.,"M":397.,"H":460.},
             "Me":{"n":0.,"L":51.,"M":115.,"H":131.}}
TECHNIQUES=("08.1","08.2","08.3","08.4","08.5","08.6","08.7","09.1","09.2","09.3")
GRU_WEIGHTS={"wz":1.0,"uz":0.1,"bz":-1.0,"wr":0.5,"ur":0.1,"br":0.0,
             "wh":0.8,"uh":0.25,"bh":0.0}


def inspect_zip(path):
    p=Path(path)
    if hashlib.sha256(p.read_bytes()).hexdigest()!=SHA256:
        raise ValueError("official UCI309 SHA256 differs from frozen archive")
    with zipfile.ZipFile(p) as z:
        names=[x for x in z.namelist() if not x.endswith("/")]
        raw={n.rsplit("/",1)[-1] for n in names
             if n.startswith("dataset_twosources_raw/")}
        down={n.rsplit("/",1)[-1] for n in names
              if n.startswith("dataset_twosources_downsampled/")}
        if len(names)!=360 or len(raw)!=180 or down!=raw:
            raise ValueError("expected 180 matched raw + downsample exposures")
        combos=collections.Counter()
        for name in sorted(down):
            match=EXPERIMENT_REGEX.fullmatch(name)
            if match is None:
                raise ValueError("unrecognized label: "+name)
            idx,eth,sec,level=match.groups()
            combos[(eth,sec,level)]+=1
        if len(combos)!=30 or set(combos.values())!={6}:
            raise ValueError("expected 30 source configurations ×6 replicates")
        return {"unique_exposures":180,"original_raw_files":180,
                "downsampled_views_of_same_exposures":180,
                "unique_configurations":30,"replicates_per_configuration":6,
                "source_sha256":SHA256}


def parse_downsample(path):
    inspect_zip(path)
    rows=[]
    with zipfile.ZipFile(path) as z:
        names=sorted(n for n in z.namelist()
                     if n.startswith("dataset_twosources_downsampled/") and not n.endswith("/"))
        for name in names:
            idx,eth,other,level=EXPERIMENT_REGEX.fullmatch(name.rsplit("/",1)[-1]).groups()
            data=[]
            with io.TextIOWrapper(z.open(name),encoding="utf-8-sig",newline="") as f:
                for line in f:
                    if not line.strip():continue
                    tokens=[x.strip() for x in line.split(",")]
                    if len(tokens)!=11:
                        raise ValueError("11 source columns required: "+name)
                    try: vals=[float(x) for x in tokens]
                    except ValueError as e:raise ValueError("non-numeric input "+name) from e
                    if any(not math.isfinite(x) for x in vals):
                        raise ValueError("nonfinite source observation "+name)
                    t,tmp,rh=vals[:3]
                    if not -40<=tmp<=90 or not 0<=rh<=100:
                        raise ValueError("invalid source T/RH")
                    if any(not 0<a<3110 for a in vals[3:]):
                        raise ValueError("out of range for documented UCI Rs transformation")
                    # Use 1Hz only for heavy feature extraction. Preserve source
                    # 10Hz recordings logically as one view of each exposure.
                    if abs(t-round(t))<.00001 and 5<=t<=299:
                        rs=[10*(3110-a)/a for a in vals[3:]] # measured source kOhm
                        data.append((round(t),tmp,rh,rs))
            if len(data)<250 or any(data[i][0]>=data[i+1][0] for i in range(len(data)-1)):
                raise ValueError("insufficient monotonic 1Hz points "+name)
            def values_between(lo,hi):
                return [item for item in data if lo<=item[0]<hi]
            base=values_between(10,50)
            early=values_between(80,120)
            late=values_between(180,220)
            recover=values_between(255,290)
            if any(len(v)<30 for v in [base,early,late,recover]):
                raise ValueError("incomplete labelled source times for "+name)
            b=[statistics.median(item[3][k] for item in base) for k in range(8)]
            if any(x<=0 for x in b):
                raise ValueError("invalid source resistance baseline")
            signal=[[item[3][k]/b[k]-1 for item in data] for k in range(8)]
            row={
                "file":name.rsplit("/",1)[-1],
                "experiment":int(idx),"ethylene_level":eth,"second_species":other,
                "second_level":level,"source_config":eth+"_"+other+"_"+level,
                "ethylene_mean_gcms_ppm":NOMINAL_PPM["Et"][eth],
                "second_mean_gcms_ppm":NOMINAL_PPM[other][level],
                "gas_presence_label":("ethylene+" if eth!="n" else "")+
                    (("CO" if other=="CO" else "methane") if level!="n" else ""),
                "n_valid_1hz_timepoints":len(data),"baseline_kohms":b,
                "early_signed_rs_fold_delta":[statistics.median(item[3][k] for item in early)/b[k]-1 for k in range(8)],
                "late_signed_rs_fold_delta":[statistics.median(item[3][k] for item in late)/b[k]-1 for k in range(8)],
                "temperature_c_baseline_median":statistics.median(item[1] for item in base),
                "relative_humidity_pct_baseline_median":statistics.median(item[2] for item in base),
                "times":[item[0] for item in data],
                "fold_deltas":signal
            }
            rows.append(row)
    if len(rows)!=180 or len(set(r["experiment"] for r in rows))!=180:
        raise ValueError("unexpected source experiment counts")
    counts=collections.Counter(r["source_config"] for r in rows)
    if len(counts)!=30 or set(counts.values())!={6}:
        raise ValueError("source configs/replicates inconsistent")
    return rows


def primitive_temporal_features(rows):
    cnn=repo_module("08.1","cnn_1d_v1.py")
    tcn=repo_module("08.2","tcn_v1.py")
    gru=repo_module("08.3","lstm_gru_v1.py")
    gnn=repo_module("08.4","gnn_v1.py")
    ev=repo_module("09.1","robotic_olfaction_v1.py")
    counts=collections.Counter()
    extracted=[]
    for r in rows:
        node_features=[]
        channel_events=0
        for x in r["fold_deltas"]:
            conv=cnn.convolve_valid(x,[-.5,0.,.5],activation="linear")
            cnn_feature=cnn.global_average_pool([abs(a) for a in conv])
            smooth=tcn.causal_conv(x,[.25,.5,.25],dilation=3)
            residual=tcn.residual_block(x,[.25,.5,.25],dilation=3)
            # Both have forward primitives only; nothing here trains weights.
            tcn_feature=statistics.median(smooth[-40:])
            h=0.
            for v in x:
                h=gru.gru_scalar_step(v,h,GRU_WEIGHTS)
            event=ev.encounters([abs(v) for v in x],baseline=0,threshold=.05)
            channel_events+=len(event["events"])
            node_features.append((cnn_feature,tcn_feature,h))
            counts["08.1_convolutions"]+=1
            counts["08.2_causal_and_residual"]+=1
            counts["08.3_fixed_weight_gru_traces"]+=1
            counts["09.1_thresholded_stationary_sensor_traces"]+=1
            if len(residual)!=len(x) or len(conv)!=len(x)-2 or not math.isfinite(h):
                raise AssertionError("primitive output shape/finite mismatch")
        # Neighbor topology is an arbitrary SOURCE CHANNEL ORDER, NOT
        # instrument position or observed sensor-to-sensor interaction.
        chain=[(i,i+1) for i in range(7)]
        neigh=gnn.aggregate(node_features,chain,include_self=True)
        counts["08.4_arbitrary_channel_graph_aggregation_exposures"]+=1
        extracted.append({"source_experiment":r["experiment"],
            "cnn_abs_rate_mean":statistics.mean(v[0] for v in node_features),
            "tcn_smoothed_late_mean":statistics.mean(v[1] for v in node_features),
            "fixed_weight_gru_last_mean":statistics.mean(v[2] for v in node_features),
            "graph_aggregated_means":[statistics.mean(z[j] for z in neigh) for j in range(3)],
            "09.1_numeric_event_count_all_8_channels":channel_events})
    return {"counts":dict(counts),
            "median_events_per_exposure":statistics.median(z["09.1_numeric_event_count_all_8_channels"]
                                                           for z in extracted),
            "median_abs_conv_gradient":statistics.median(z["cnn_abs_rate_mean"] for z in extracted),
            "mean_source_temperature_c":statistics.mean(r["temperature_c_baseline_median"] for r in rows),
            "mean_source_humidity_pct":statistics.mean(r["relative_humidity_pct_baseline_median"] for r in rows),
            "physical_caveats":["08.1 fixed 1D filter, NOT trained CNN",
                               "08.2 fixed dilated filter, NOT trained TCN",
                               "08.3 fixed scalar GRU forward weights, NOT trained GRU/LSTM",
                               "08.4 arbitrary channel-order edges, NOT a verified physical sensor graph",
                               "09.1 signal threshold events in stationary wind tunnel, NOT a mobile plume encounter detector"]}


def train_fold_probabilistic_ensemble(rows):
    # 24 nonzero secondary source configurations, 6 trials each -> 144.
    eligible=[r for r in rows if r["second_level"]!="n"]
    groups=sorted(set(r["source_config"] for r in eligible))
    if len(eligible)!=144 or len(groups)!=24:
        raise AssertionError("expected 144 true CO-or-methane exposures, 24 groups")
    scaler=repo_module("02.2","scaling_v1.py")
    knn=repo_module("04.3","knn_v1.py")
    ensemble=repo_module("08.5","temporal_ensemble_v1.py")
    fold_out=[]
    counts=collections.Counter()
    for config in groups:
        train=[r for r in eligible if r["source_config"]!=config]
        test=[r for r in eligible if r["source_config"]==config]
        if len(train)!=138 or len(test)!=6:
            raise AssertionError("leave source configuration out split broken")
        c_names=["CO","Me"]
        modelset={}
        for phase in ("early","late"):
            xfield=phase+"_signed_rs_fold_delta"
            xs=[r[xfield] for r in train]
            sc=scaler.fit_scaler(xs)
            sx=scaler.transform(xs,sc)
            mdl=knn.fit_knn(sx,[r["second_species"] for r in train],SENSORS)
            modelset[phase]=(sc,mdl)
        per_fold=collections.Counter()
        for r in test:
            probs=[]
            preds=[]
            for phase in ("early","late"):
                sc,mdl=modelset[phase]
                z=scaler.transform([r[phase+"_signed_rs_fold_delta"]],sc)[0]
                vote=knn.predict_knn(z,mdl,k=3,weighting="uniform")
                p=[vote["votes"].get(c,0.)/3. for c in c_names]
                if abs(sum(p)-1)>1e-10:raise AssertionError("probabilities not normalized")
                probs.append(p)
                preds.append(c_names[max(range(2),key=lambda k:p[k])])
            out=ensemble.ensemble(probs,weights=[1.,1.])
            pred=c_names[out["candidate_index"]]
            for mode,guess in (("early",preds[0]),("late",preds[1]),("ensemble",pred)):
                per_fold[mode+"_n"]+=1
                per_fold[mode+"_correct"]+=int(guess==r["second_species"])
        counts.update(per_fold)
        fold_out.append({"held_out_full_source_config":config,
                         "n_heldout_exposures":6,"n_train_exposures":138,
                         "early_correct":per_fold["early_correct"],
                         "late_correct":per_fold["late_correct"],
                         "ensemble_correct":per_fold["ensemble_correct"]})
    return {
        "source_response_task":"CO versus methane source type, when secondary source ACTIVE (no n-level rows)",
        "heldout_complete_mixture_configurations":24,
        "heldout_source_exposures":counts["ensemble_n"],
        "n_classes":2,"n_original_gas_configurations":24,
        "correct_early":counts["early_correct"],"correct_late":counts["late_correct"],
        "correct_ensemble":counts["ensemble_correct"],
        "accuracy_early":counts["early_correct"]/counts["early_n"],
        "accuracy_late":counts["late_correct"]/counts["late_n"],
        "accuracy_ensemble":counts["ensemble_correct"]/counts["ensemble_n"],
        "baseline_chance_balanced":.5,
        "folds":fold_out,
        "limitations":["All 6 replicates of a heldout gas-flow configuration excluded from training.",
                       "No independent physical device or atmospheric site held out.",
                       "08.5 averages two correlated KNN vote distributions; they are NOT calibrated probabilities.",
                       "Only CO vs methane source identity when secondary source is nonzero; not full multi-gas identification."]
    }


def toy_simulation_gap(rows):
    sim=repo_module("08.6","simulation_v1.py")
    gap=repo_module("08.7","sim_to_real_v1.py")
    syn=[]
    real=[]
    for r in rows:
        # This uses external GC-MS mean concentration as illustrative input
        # but sensitivity=0.001, tau=15s are ARBITRARY, NOT fitted to sensor!
        dosage=(r["ethylene_mean_gcms_ppm"]+r["second_mean_gcms_ppm"])
        vals=[0. if t<60 or t>=240 else dosage
              for t in range(300)]
        predicted=sim.simulate(vals,sensitivity=.001,baseline=1.,tau_seconds=15.)
        v=predicted["responses"]
        syn.append([statistics.median(v[190:220])-1.,
                    statistics.median(v[255:290])-1.])
        observed=r["fold_deltas"][0]
        times=r["times"]
        real.append([statistics.median(x for t,x in zip(times,observed) if 190<=t<220),
                     statistics.median(x for t,x in zip(times,observed) if 255<=t<290)])
    diff=gap.compare(syn,real)
    return {
      "08.6_toy_simulated_profiles":len(syn),
      "08.7_numeric_domain_gap_test_exposures":len(real),
      "mean_domain_gap_rmse_arbitrary_dimensionless":diff["mean_gap_rmse"],
      "assumed_tau_s":15,"arbitrary_sensitivity":.001,
      "source_sensor":"TGS2600 transformed Rs / local baseline",
      "status":"NUMERIC_ONLY_SYNTHETIC_MODEL_NOT_PHYSICALLY_CALIBRATED",
      "limitation":"GC-MS is mean mixed-gas exposure; predicted profile assumes square-wave concentration and unknown sensor gain; not a validated sim-to-real transfer metric."
    }


def run(archive):
    info=inspect_zip(archive)
    rows=parse_downsample(archive)
    numeric=primitive_temporal_features(rows)
    classify=train_fold_probabilistic_ensemble(rows)
    simulated=toy_simulation_gap(rows)
    return {
      "dataset":"UCI309 official original gas sensor array exposed to turbulent gas mixtures",
      "doi":DOI,"license":"CC BY 4.0; Jordi Fonollosa (2014)",
      "archive_sha256":SHA256,"source_validation":info,
      "n_unique_source_experiments":180,
      "source_sensors":list(SENSORS),
      "source_temperature_and_rh":True,
      "source_nominal_gcms_mean_ppm":NOMINAL_PPM,
      "ppm_warning":"GC-MS only measured mean concentration per source gas level, not instantaneous ppm in 300s stream.",
      "source_rs_transform":"10*(3110-A)/A kOhm as documented for UCI309; source Figaro circuit, NOT MQ-R0 Tank calibration.",
      "technique_ids":list(TECHNIQUES),
      "source_sensor_temporal_primitives":numeric,
      "08.5_heldout_source_configuration_classification":classify,
      "08.6_08.7_toy_simulation_vs_real_numeric_only":simulated,
      "blocked_geospatial":{
         "09.2":{"status":"BLOCKED_MISSING_SPATIAL_TRAJECTORY",
                 "missing":["x/y robot position per measurement","per-frame sensor location/orientation","wind field aligned to readings"]},
         "09.3":{"status":"BLOCKED_MISSING_MULTIPOSITION_LOCALIZATION_TRUTH",
                 "missing":["independent source coordinates along measured robot route","multiple positioned sensor visits","wind direction/velocity paired to sensor samples"]}
      },
      "per_technique_status":{
        **{z:"NUMERIC_SIGNAL_PRIMITIVE_ON_MEASURED_EXTERNAL_SENSOR_DATA" for z in ("08.1","08.2","08.3","08.4","08.5","09.1")},
        "08.6":"THEORETICAL_SIMULATION_USING_MEAN_GCMS_INPUT_NOT_VALIDATED",
        "08.7":"NUMERIC_ONLY_NONCALIBRATED_SIM_REAL_COMPARISON",
        "09.2":"BLOCKED_MISSING_SOURCE_XY_POSITION",
        "09.3":"BLOCKED_MISSING_SOURCE_LOCALIZATION_TRAJECTORY"
      },
      "caveats":["180 source exposure records; original 180 raw + 180 downsampled files are duplicated views of SAME exposures.",
                 "Only one external fixed Figaro sensor array and source wind tunnel, not Tank MQ8.",
                 "08.1,08.2,08.3,08.4 are fixed-weight mathematical primitives, NOT trained end-to-end neural architectures.",
                 "08.5 temporal average of correlated votes is not calibrated likelihood or safety evidence.",
                 "08.6 simulator and 08.7 numeric gap cannot be treated as physical validation.",
                 "09.1 threshold excursion is a stationary flow-tunnel event, NOT a robotic plume encounter.",
                 "09.2/09.3 have no time-aligned robot xy coordinates and must remain BLOCKED.",
                 "No technique qualifies Tank device scientific G3, occupational exposure or safety."]
    }


if __name__=="__main__":
  ap=argparse.ArgumentParser()
  ap.add_argument("original_zip");ap.add_argument("--output",default="uci309_g2_batch06_result.json")
  args=ap.parse_args()
  outcome=run(args.original_zip)
  Path(args.output).write_text(json.dumps(outcome,indent=2,ensure_ascii=False,allow_nan=False)+"\n",encoding="utf-8")
  print("UCI309 G2-06: source exposure records=",outcome["n_unique_source_experiments"])
  print("Temporal counts:",json.dumps(outcome["source_sensor_temporal_primitives"]["counts"]))
  cls=outcome["08.5_heldout_source_configuration_classification"]
  print("08.5 real source configuration holdout:",cls["correct_early"],cls["correct_late"],cls["correct_ensemble"],"/",cls["heldout_source_exposures"])
  print("08.6/08.7 simulated-vs-real (nonphysical):",outcome["08.6_08.7_toy_simulation_vs_real_numeric_only"]["mean_domain_gap_rmse_arbitrary_dimensionless"])
  print("09.2/09.3 blocked: NO per-sample moving robot coordinates")
