"""Group held-out scientific evaluation of the existing trained MLP."""
from pathlib import Path
import runpy

def grouped_mlp_benchmark(train_x, train_y, train_groups, test_x, test_y,
                          test_groups, sensor_ids, *, source_id):
    p=Path(__file__).resolve().parents[2]/"implementation_execution"/"batch03_model_baselines_v2.py"
    evaluate=runpy.run_path(str(p))["evaluate_method"]
    return evaluate("04.8",train_x,train_y,train_groups,test_x,test_y,
                    test_groups,sensor_ids,source_id=source_id)
