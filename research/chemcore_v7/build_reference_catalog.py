#!/usr/bin/env python3
"""Build reproducible MQSpaceData v7 source catalog; never calibrates a Tank sensor.

Reads the ORIGINAL pinned MIT MQInfo.py. It deliberately does not infer device
R0, overwrite source units, or promote simulated gas concentrations to G3.
"""
from __future__ import annotations
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path

BASE = Path(__file__).resolve().parent
UPSTREAM = BASE / "mqspacedata_v7_upstream" / "MQInfo.py"
REVISION = "99c7262d8ce0fbd5f3aaee096cb0b469d2283637"
SENSORS = (
    "MQ2", "MQ3", "MQ4", "MQ5", "MQ6", "MQ7", "MQ8", "MQ9",
    "MQ131", "MQ135", "MQ136", "MQ137", "MQ138", "MQ214", "MQ216"
)
ALL_PARAMETERS = (
    "SensorName", "Air", "CalibrateAir", "CleanAir", "SensorRLCalRL",
    "MinAirPpm", "MaxAirPpm", "gas_params",
    "a_RH30", "b_RH30", "c_RH30",
    "a_RH33", "b_RH33", "c_RH33",
    "a_RH60", "b_RH60", "c_RH60",
    "a_RH85", "b_RH85", "c_RH85",
    "SensorMode", "FormulaCoefficient", "AirVals", "AirValsEqualGasVals",
    "CRMode", "FormulaMode",
)

def load():
    spec = importlib.util.spec_from_file_location("MQSpaceDataV7Pinned", UPSTREAM)
    assert spec and spec.loader
    m = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(m)
    return m

def extract():
    m = load()
    profiles = []
    for model in SENSORS:
        for field in ALL_PARAMETERS:
            setattr(m, field, 1 if field == "CleanAir" else None)
        getattr(m, model)()
        profile = {field: getattr(m, field, None) for field in ALL_PARAMETERS}
        # MQInfo may have missing calibration settings: preserve missing as null.
        # The source code legitimately includes gas-specific ppmvals.
        profile["model"] = model
        profile["sensor_count"] = len(profile.get("gas_params") or [])
        assert profile["SensorName"] is not None, model
        assert profile["SensorMode"] in ("Rs/Ro", "Ro/Rs", "Rs/Rs"), model
        assert profile["sensor_count"] > 0, model
        profiles.append(profile)
    return {
        "schema": "hazrob-chemcore-v7-mqspacedata-7-source-catalog/1",
        "source": {
            "repo": "abcdaaaaaaaaa/MQDataScience",
            "commit": REVISION,
            "version": "7.0.0",
            "license": "MIT",
            "file": "DataScience/Requirements/MQInfo.py",
            "source_sha256": hashlib.sha256(UPSTREAM.read_bytes()).hexdigest(),
        },
        "classification": "REFERENCE_EQUATIONS_ONLY_NOT_TANK_G3_CALIBRATION",
        "physical_tank_g3_validation": False,
        "profiles": profiles,
    }

def main():
    cli = argparse.ArgumentParser()
    cli.add_argument("--output", default=str(BASE / "mqspacedata_reference_catalog_v7.json"))
    args = cli.parse_args()
    data = extract()
    destination = Path(args.output)
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n",
                           encoding="utf-8")
    print("Reference catalog:", destination, "profiles:", len(data["profiles"]),
          "source gas curves:", sum(x["sensor_count"] for x in data["profiles"]))

if __name__ == "__main__":
    main()
