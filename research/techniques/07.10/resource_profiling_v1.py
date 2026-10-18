"""Research-only analytical memory/throughput budget, not measured profiling."""
from math import isfinite
def estimate(*,parameters,bytes_per_parameter=4,activations_bytes=0,operations_per_inference=0,frequency_hz=1):
    ints=(parameters,bytes_per_parameter,activations_bytes,operations_per_inference)
    if any(not isinstance(v,int) or isinstance(v,bool) or v<0 for v in ints) or parameters==0 or bytes_per_parameter==0 or not isinstance(frequency_hz,(int,float)) or isinstance(frequency_hz,bool) or not isfinite(frequency_hz) or frequency_hz<=0:raise ValueError("invalid budget")
    return {"weights_bytes":parameters*bytes_per_parameter,"total_estimated_bytes":parameters*bytes_per_parameter+activations_bytes,
            "operations_per_second":operations_per_inference*frequency_hz,
            "warning":"Excludes runtime allocator, stack, Wi-Fi/camera contention and power draw; measure on target hardware."}
