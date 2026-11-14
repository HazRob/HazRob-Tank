"""05.6: source-traceable conditional ppm research study, not production calibration."""
from pathlib import Path
import runpy

def audit_source_calibration(train_records, heldout_records, *, metadata):
    engine=Path(__file__).resolve().parents[2]/"implementation_execution"/"source_conditional_regression_v2.py"
    evaluate=runpy.run_path(str(engine))["source_grouped_regression"]
    return evaluate("05.6",train_records,heldout_records,metadata=metadata)
