"""G2-05: UCI308 authentic binary acetone/ethanol mixture signal audit.

Research-only; DOI 10.24432/C5BG7G, CC BY 4.0, Ziyatdinov & Fonollosa.
Use measured dR signals and source feature columns, NEVER interpret prepared
liquid volume percent as headspace gas ppm. UCI308 uses 16 source sensors,
not Tank 8-MQ array. Source hash locked to official UCI archive.

Four numeric source-value technique checks (02.6,08.8,08.9,08.10), six
scientific-compatibility blocks (02.8,05.4,05.5,05.6,05.7,08.11).
"""
import argparse,collections,csv,gzip,hashlib,io,json,math,statistics,zipfile
from pathlib import Path
from uci1081_actual_modules_v1 import repo_module

SHA256="7b062960dbef1a9e8aefc62bc7dbac09bdadb8cbb40793cf2b8e122da1862f90"
TECHNIQUES=("02.6","02.8","05.4","05.5","05.6","05.7","08.8","08.9","08.10","08.11")
PURE=("acetone","ethanol")
SENSORS=tuple("S%d"%i for i in range(1,17))
LIQUID_DOSES={0.,0.1,0.3,1.}
VALID_LABELS={"mixture","acetone","ethanol","air"}

def _csvreader(stream):
    return csv.DictReader(stream)

def validate_original(path):
    if hashlib.sha256(Path(path).read_bytes()).hexdigest()!=SHA256:
        raise ValueError("UCI308 source archive SHA mismatch")
    with zipfile.ZipFile(path) as z:
        if set(z.namelist())!={"features.csv","rawdata.csv.gz"}:
            raise ValueError("unexpected archive members")

def parse_features(path):
    validate_original(path)
    with zipfile.ZipFile(path) as z:
        with io.TextIOWrapper(z.open("features.csv"),encoding="utf-8-sig",newline="") as stream:
            reader=_csvreader(stream)
            if len(reader.fieldnames)!=439 or any("S%d_max"%i not in reader.fieldnames for i in range(1,17)):
                raise ValueError("unexpected 432-feature UCI308 schema")
            rows=[]
            for line in reader:
                if None in line or None in line.values():
                    raise ValueError("short or malformed feature row")
                try:
                    exp=int(line["exp"])
                    vector=[float(line["S%d_max"%i]) for i in range(1,17)]
                    ace=float(line["ace_conc"]);eth=float(line["eth_conc"])
                except (TypeError,ValueError) as e:
                    raise ValueError("invalid source feature or dose") from e
                if (len(vector)!=16 or any(not math.isfinite(v) for v in vector)
                    or ace not in LIQUID_DOSES or eth not in LIQUID_DOSES):
                    raise ValueError("nonfinite source max signal or unsupported liquid dose")
                gas=line["gas"].strip().lower()
                batch=line["batch"].strip()
                lab=line["lab"].strip()
                if gas not in VALID_LABELS or not batch or not lab:
                    raise ValueError("invalid UCI308 annotations")
                if gas=="air" and (ace!=0 or eth!=0):raise ValueError("air with nonzero preparation")
                if gas=="mixture" and not (ace>0 and eth>0):raise ValueError("mixture lacking second analyte")
                if gas=="acetone" and not (ace>0 and eth==0):raise ValueError("acetone mislabeled")
                if gas=="ethanol" and not (eth>0 and ace==0):raise ValueError("ethanol mislabeled")
                rows.append({"exp":exp,"batch":batch,"gas":gas,"lab":lab,
                             "ace_liquid_vol_percent":ace,"eth_liquid_vol_percent":eth,
                             "max_dR":vector})
    if len(rows)!=58 or len(set(r["exp"] for r in rows))!=58:
        raise ValueError("expected 58 independent exposure records")
    if len(set(r["lab"] for r in rows))!=12:
        raise ValueError("expected 12 UCI classes")
    if len(set(r["batch"] for r in rows))!=5:
        raise ValueError("expected five UCI batch labels")
    return rows

def numeric_raw_trajectories(path,features):
    """Audit all 928 real traces, downsample 16 selected series per sample.

    02.6 expects calibrated log Rs fold-changes. Source dR is NOT verified
    log(Rs_exposure/Rs_baseline); operation below NUMERIC-ONLY/noncompliant
    with chemical feature contract. No fabricated physical variables.
    """
    curve=repo_module("02.6","multidimensional_features_v1.py")
    expected={r["exp"]:r for r in features}
    raw=collections.defaultdict(dict)
    n_lines=0
    with zipfile.ZipFile(path) as z:
        with io.TextIOWrapper(gzip.GzipFile(fileobj=z.open("rawdata.csv.gz")),
                              encoding="utf-8-sig",newline="") as stream:
            rdr=_csvreader(stream)
            if len(rdr.fieldnames)!=7509 or "dR_t7500" not in rdr.fieldnames:
                raise ValueError("unexpected 7500-point UCI308 raw schema")
            for line in rdr:
                if None in line or None in line.values():
                    raise ValueError("invalid UCI raw source line")
                exp=int(line["exp"]); sensor=int(line["sensor"])
                if exp not in expected or not 1<=sensor<=16:
                    raise ValueError("unknown experiment/sensor")
                if sensor in raw[exp]:
                    raise ValueError("duplicate experimental sensor trace")
                master=expected[exp]
                if line["batch"]!=master["batch"] or line["gas"]!=master["gas"] or line["lab"]!=master["lab"]:
                    raise ValueError("raw/feature metadata disagreement")
                # Verify all real 7,500 values; extract only 300 equally-spaced
                # time-aligned points to bound analysis memory and CPU.
                signal=[]
                for t in range(1,7501):
                    v=float(line["dR_t%d"%t])
                    if not math.isfinite(v):
                        raise ValueError("nonfinite raw sensor trace")
                    if sensor in (1,2) and (t-1)%25==0:
                        signal.append(v)
                if sensor in (1,2):
                    if len(signal)!=300:raise ValueError("expected 300 downsampled timepoints")
                    raw[exp][sensor]=signal
                else:
                    raw[exp][sensor]=None
                n_lines+=1
    if n_lines!=58*16 or any(len(d)!=16 for d in raw.values()):
        raise ValueError("expected 58 times 16 raw sensor traces")
    lengths=[]
    for exp,d in sorted(raw.items()):
        time=[i*1. for i in range(300)]
        tr=curve.build_trajectory(time,[d[1],d[2]],("source_S1_dR","source_S2_dR"))
        if tr["dimensions"]!=3 or len(tr["points"])!=300:raise ValueError("trajectory not aligned")
        length=curve.trajectory_path_length(tr["points"],scales=[300,1,1])
        if not math.isfinite(length):raise ValueError("nonfinite numerical path length")
        lengths.append(length)
    return {
        "02.6_status":"NUMERIC_ONLY_NOT_VALID_LOG_RS_CHEMICAL_INPUT",
        "validated_raw_sensor_series":n_lines,
        "raw_points_per_sensor":7500,
        "source_signal_feature":"source dR values, NOT verified calibrated log-Rs ratio",
        "downsampled_to_hz":1.,
        "numeric_3d_exposure_trajectories":len(lengths),
        "numeric_path_length_median_arb_scaled":statistics.median(lengths),
        "real_4d_environmental_covariate":"UNAVAILABLE"
    }

def train_endmembers(train):
    pure={gas:[r for r in train if r["gas"]==gas] for gas in PURE}
    if any(len(pure[g])<2 for g in PURE):
        raise ValueError("insufficient pure-source training exposures per class")
    # Training-only feature scaling, fitted without liquid concentration labels.
    scale=[statistics.pstdev([r["max_dR"][k] for r in train]) for k in range(16)]
    scale=[v if v>1e-10 else 1. for v in scale]
    signatures={gas:[
        statistics.mean(row["max_dR"][j]/scale[j] for row in pure[gas])
        for j in range(16)] for gas in PURE}
    return scale,signatures

def score_folds(rows):
    det=repo_module("08.8","mixture_detection_v1.py")
    unm=repo_module("08.9","unmixing_v1.py")
    reg=repo_module("08.10","multi_analyte_regression_v1.py")
    batches=sorted({r["batch"] for r in rows})
    results=[]
    totals=collections.Counter()
    for batch in batches:
        train=[r for r in rows if r["batch"]!=batch]
        test=[r for r in rows if r["batch"]==batch]
        scale,sig=train_endmembers(train)
        evaluate=[r for r in test if r["gas"] in (*PURE,"mixture")]
        count=collections.Counter()
        for r in evaluate:
            vector=[v/scale[j] for j,v in enumerate(r["max_dR"])]
            # No train/test leakage; threshold predeclared as fixed and UNCALIBRATED.
            classified=det.assess(vector,sig,threshold=0.5)
            suspect=classified["status"]=="UNEXPLAINED_RESPONSE"
            actual=r["gas"]=="mixture"
            count["n"]+=1;count["mixture_samples"]+=int(actual)
            count["pure_samples"]+=int(not actual)
            count["mix_true_positive"]+=int(actual and suspect)
            count["pure_false_positive"]+=int(not actual and suspect)
            count["mixture_residual_sum"]+=classified["relative_residual"] if actual else 0
            if actual:
                # 08.9 and 08.10 are linear numerical decompositions of
                # 16-channel real measured signals. Their coefficients are NOT
                # gas ppm, gas molar mixing fractions or liquid dose estimates.
                a=unm.unmix_two(vector,sig["acetone"],sig["ethanol"],step=0.1)
                b=reg.estimate(vector,
                               [sig["acetone"],sig["ethanol"]],
                               iterations=500,step_size=.005)
                if any(not math.isfinite(x) for x in
                       [a["coefficient_a"],a["coefficient_b"],b["coefficients"][0],b["coefficients"][1]]):
                    raise ValueError("nonfinite unmixing estimate")
                count["unmix_n"]+=1
                count["grid_residual_sum"]+=a["squared_error"]
                count["descent_residual_sum"]+=sum(
                   (vector[i]-b["coefficients"][0]*sig["acetone"][i]-
                    b["coefficients"][1]*sig["ethanol"][i])**2 for i in range(16))
                count["grid_a_sum"]+=a["coefficient_a"]
                count["grid_b_sum"]+=a["coefficient_b"]
                count["fit_a_sum"]+=b["coefficients"][0]
                count["fit_b_sum"]+=b["coefficients"][1]
        results.append({"held_out_batch":batch,"n_training_exposures":len(train),
            "n_testing_exposures":len(test),"n_scored_pure_or_mixture":count["n"],
            "n_true_mixture":count["mixture_samples"],"n_true_pure":count["pure_samples"],
            "08.8_mix_correctly_flagged":count["mix_true_positive"],
            "08.8_pure_false_flags":count["pure_false_positive"],
            "08.9_08.10_mixture_numeric_fits":count["unmix_n"],
            "08.9_mean_squared_fit_error":count["grid_residual_sum"]/count["unmix_n"] if count["unmix_n"] else None,
            "08.10_mean_squared_fit_error":count["descent_residual_sum"]/count["unmix_n"] if count["unmix_n"] else None,
            "08.9_mean_coefficients":([count["grid_a_sum"]/count["unmix_n"],count["grid_b_sum"]/count["unmix_n"]] if count["unmix_n"] else None),
            "08.10_mean_coefficients":([count["fit_a_sum"]/count["unmix_n"],count["fit_b_sum"]/count["unmix_n"]] if count["unmix_n"] else None)})
        totals.update(count)
    if totals["unmix_n"]==0 or totals["pure_samples"]==0:
        raise ValueError("no actual mixture and pure source samples")
    return {"folds":results,"n_scored_exposures":totals["n"],
       "08.8_mix_sensitivity":totals["mix_true_positive"]/totals["mixture_samples"],
       "08.8_pure_specificity":1-totals["pure_false_positive"]/totals["pure_samples"],
       "08.8_tp_mix":totals["mix_true_positive"],"08.8_total_mix":totals["mixture_samples"],
       "08.8_fp_pure":totals["pure_false_positive"],"08.8_total_pure":totals["pure_samples"],
       "08.9_08.10_n_numerical_mixture_fits":totals["unmix_n"],
       "fixed_threshold":0.5,
       "interpretation":"Two-source signal-residual study; neither mixture suspicion nor coefficients calibrate gas ppm or prove gas identity."}

def run(path):
    rows=parse_features(path)
    raw=numeric_raw_trajectories(path,rows)
    diagnostics=score_folds(rows)
    classes=collections.Counter(r["gas"] for r in rows)
    batch_counts=collections.Counter(r["batch"] for r in rows)
    blocks={
       "02.8":"BLOCKED: no verified device identity/firmware/calibration/heater/timestamp for canonical provenance; batch label alone is not those fields.",
       "05.4":"BLOCKED: nonpositive/unknown electrical responses cannot be asserted calibrated Rs/R0; liquid doses not headspace ppm.",
       "05.5":"BLOCKED: calibrated gas-specific Rs/R0 and gas concentration ppm missing.",
       "05.6":"BLOCKED: same missing Rs/R0 and gas ppm; tree fitting with liquid dose labels would misrepresent gas concentration.",
       "05.7":"BLOCKED: no independently calibrated gas identity probabilities AND no certified ppm calibrators.",
       "08.11":"BLOCKED: no experimentally paired interferent/control exposure at controlled same background, humidity and gas concentrations.",
    }
    if set(TECHNIQUES)!={"02.6","08.8","08.9","08.10",*blocks}:
        raise AssertionError("must review ten distinct technique IDs")
    return {
       "scientific_scope":"external-source numeric evidence only; zero Tank G3",
       "source":"UCI308 flow-modulated gas sensor array",
       "source_doi":"10.24432/C5BG7G",
       "license":"CC BY 4.0; Ziyatdinov & Fonollosa",
       "source_archive_sha256":SHA256,
       "source_experiments":len(rows),"source_sensors":16,"source_raw_rows":928,
       "source_raw_timepoints_per_sensor":7500,"source_rate_hz":25,
       "classes":dict(classes),"batch_counts":dict(batch_counts),
       "prepared_liquid_vol_percent_values":sorted(LIQUID_DOSES),
       "measured_gas_ppm":None,
       "numerical_checks":{"02.6":raw,"08.8_08.9_08.10":diagnostics},
       "blocked_physical_techniques":blocks,
       "reviewed_techniques":list(TECHNIQUES),
       "conclusions":[
          "Raw source dR values are not established as log(Rs/R0), so 02.6 3D numeric execution does NOT meet its chemical semantic input contract.",
          "G2 heldout-day mixture-suspicion and linear unmixing are exploratory results on a different 16-sensor array.",
          "Source acetone/ethanol concentration labels are liquid dilution vol.% in water, not headspace gas ppm.",
          "No independent Tank MQ8 gas calibration / independent external gas validation.",
          "Unknown-gas detection, true ppm multi-analyte estimation and paired interferent effect remain scientifically untested.",
       ]
    }

if __name__=="__main__":
    p=argparse.ArgumentParser()
    p.add_argument("official_uci308_zip")
    p.add_argument("--output",default="uci308_g2_batch05_results.json")
    args=p.parse_args()
    report=run(args.official_uci308_zip)
    Path(args.output).write_text(json.dumps(report,ensure_ascii=False,allow_nan=False,indent=2)+"\n",encoding="utf-8")
    print("G2-05 source",report["source_experiments"],"exposures /",report["source_raw_rows"],"sensor traces; blocked",len(report["blocked_physical_techniques"]))
    print("RAW",json.dumps(report["numerical_checks"]["02.6"]))
    print("MIXTURE",json.dumps(report["numerical_checks"]["08.8_08.9_08.10"]))
