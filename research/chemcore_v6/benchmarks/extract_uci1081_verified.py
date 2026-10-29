#!/usr/bin/env python3
"""Verified extraction of ORIGINAL UCI1081 to fixed 90x30 numerical source features.

No invented sessions/devices, no Tank Rs/R0, no previous ChemCore and no food data.
Expected physical layout: ten contiguous 900-point channel blocks per CSV row.
"""
import argparse
import csv
import hashlib
import io
import json
import math
import urllib.request
import zipfile
from collections import Counter
from pathlib import Path

URL = "https://archive.ics.uci.edu/static/public/1081/gas%2Bsensor%2Barray%2Blow-concentration.zip"
ZIP_SHA = "813ebb7d00ed9172114b68d27ab3213170d6c220b8b23907dce91e9838cd48ab"
CSV_SHA = "d79afe6494903b988de68d4a8ab133167151a1bb620ba413e5cefd15003d07b3"
GASES = ("ethanol","acetone","toluene","ethyl acetate","isopropanol","hexane")
LEVELS = (50,100,200)
CHANNELS = ("TGS2603","TGS2630","TGS813","TGS822","MQ135","MQ137",
            "MQ138","2M012","VOCS-P","2SH12")

def sha(data):
    return hashlib.sha256(data).hexdigest()

def verified_bytes(archive):
    if archive is not None:
        raw = Path(archive).read_bytes()
    else:
        req = urllib.request.Request(URL, headers={"User-Agent":"HazRobTankV6-Research/1.0"})
        with urllib.request.urlopen(req, timeout=50) as response:
            raw = response.read()
    checksum = sha(raw)
    if checksum != ZIP_SHA:
        raise ValueError("FAIL-CLOSED wrong ORIGINAL UCI1081 archive SHA256: " + checksum)
    with zipfile.ZipFile(io.BytesIO(raw)) as f:
        if f.namelist() != ["gsalc.csv"]:
            raise ValueError("Unexpected source archive layout")
        data = f.read("gsalc.csv")
    if sha(data) != CSV_SHA:
        raise ValueError("FAIL-CLOSED original UCI1081 gsalc.csv SHA256 mismatch")
    return data

def extract(data):
    rows = list(csv.reader(io.StringIO(data.decode("utf-8-sig"))))
    if len(rows) != 90:
        raise ValueError("Source must have exactly 90 original exposures")
    out = []
    freq = Counter()
    parity = []
    for rowid, row in enumerate(rows):
        if len(row) != 9002:
            raise ValueError("Expected 9000 signal readings + gas + concentration")
        gas = row[0].strip().lower()
        level_text = row[1].strip().lower()
        if gas not in GASES or not level_text.endswith("ppb"):
            raise ValueError("Unexpected original UCI1081 gas/unit")
        level = int(level_text[:-3])
        if level not in LEVELS:
            raise ValueError("Unexpected source concentration")
        values = [float(v) for v in row[2:]]
        if not all(math.isfinite(v) for v in values):
            raise ValueError("Undefined original source signal")
        endpoints, early, middle = [], [], []
        for channel in range(10):
            signal = values[channel * 900:(channel + 1) * 900]
            baseline = sum(signal[:300]) / 300
            if abs(baseline) < 1e-9:
                raise ValueError("Unusable original zero baseline")
            endpoint = sum(signal[840:900]) / 60
            first = sum(signal[300:360]) / 60
            mid = sum(signal[600:660]) / 60
            endpoints.append(endpoint/baseline - 1)
            early.append(first/baseline - 1)
            middle.append(mid/baseline - 1)
            if gas == "acetone" and level == 50 and channel == 4:
                parity.append(endpoint/baseline)
        out.append([rowid,gas,level] + endpoints + early + middle)
        freq[(gas,level)] += 1
    if len(freq) != 18 or set(freq.values()) != {5}:
        raise ValueError("Original source 6 x 3 x 5 group integrity failed")
    if len(parity) != 5 or abs(sum(parity)/5 - 0.9471726656594102) > 1e-9:
        raise ValueError("MQ135 source layout parity to independently pinned reference failed")
    return out

def main():
    p=argparse.ArgumentParser()
    p.add_argument("--archive",help="Optional original zip if already downloaded")
    p.add_argument("--out",required=True)
    p.add_argument("--manifest",required=True)
    args=p.parse_args()
    rows=extract(verified_bytes(args.archive))
    path=Path(args.out)
    path.parent.mkdir(parents=True,exist_ok=True)
    with path.open("w",newline="",encoding="utf-8") as f:
        w=csv.writer(f,lineterminator="\n")
        w.writerow(["row_id","gas","ppb"]+["f%02d"%i for i in range(30)])
        for r in rows:
            w.writerow(r[:3]+[format(x,".17g") for x in r[3:]])
    manifest={
      "schema":"chemcore-v6-n05-uci1081-verified-extraction/1",
      "origin":"UCI Machine Learning Repository, Gas sensor array low-concentration, DOI 10.24432/C5CK6F, CC BY 4.0",
      "source_url":URL,"original_archive_sha256":ZIP_SHA,"original_csv_sha256":CSV_SHA,
      "features_csv_sha256":sha(path.read_bytes()),
      "original_exposures":90,"gases":list(GASES),"concentration_levels_ppb":list(LEVELS),
      "source_sensor_order":list(CHANNELS),
      "source_signal_order":"10 contiguous channel blocks, 900 samples per block",
      "feature_schema":"30 features: 10 endpoint (840:900), 10 early (300:360), 10 middle (600:660), each window/baseline(0:300)-1",
      "parity":"acetone 50ppb MQ135 first source replicates mean endpoint/baseline 0.9471726656594102",
      "split_policy":"leave-one-whole-source-concentration-level-out, 60 training and 30 heldout exposures per fold",
      "device_day_independence_proven":False,
      "chemically_calibrated_rs_r0":False,
      "tank_physical_g3":False
    }
    Path(args.manifest).write_text(json.dumps(manifest,indent=2)+"\n",encoding="utf-8")
    print("PASS VERIFIED_UCI1081_SOURCE archive_sha256="+ZIP_SHA)
    print("PASS VERIFIED_UCI1081_SOURCE csv_sha256="+CSV_SHA)
    print("PASS VERIFIED_UCI1081_SOURCE features_sha256="+manifest["features_csv_sha256"])
    print("PASS source 90 original rows; 10 sensor blocks; 6 gases; 3 levels; five replicates per combination")
if __name__=="__main__":
    main()
