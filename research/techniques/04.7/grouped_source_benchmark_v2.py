"""04.7 V4 — source-qualified grouped evaluation of the versioned V1 baseline.

This is a new leakage-safe experimental wrapper, not production ChemCore.
"""
from pathlib import Path
import importlib.util

def benchmark_source(train_rows,train_labels,train_exposure_ids,
                     test_rows,test_labels,test_exposure_ids,
                     sensor_ids,*,source_id):
    path=Path(__file__).resolve().parents[2]/"implementation_execution"/"batch03_model_baselines_v2.py"
    spec=importlib.util.spec_from_file_location("tank_batch03_baselines_04_7",path)
    module=importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module.evaluate_method("04.7",train_rows,train_labels,train_exposure_ids,
        test_rows,test_labels,test_exposure_ids,sensor_ids,source_id=source_id)
