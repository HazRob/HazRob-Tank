"""07.10: reproducible CPython benchmark, NOT Android/ESP32 CPU, RAM, power."""
from time import perf_counter_ns
from statistics import median
from tracemalloc import start, stop, get_traced_memory, is_tracing

def profile_python_call(fn, data, *, repeats=7,device_label="CPYTHON_LOCAL"):
    if not callable(fn) or isinstance(repeats,bool) or not isinstance(repeats,int) or not 3<=repeats<=500:
        raise ValueError("callable and >=3 repetitions required")
    if device_label!="CPYTHON_LOCAL":
        raise ValueError("no claimed ESP32/Android benchmark without a real hardware profiler")
    # Warmup is part of protocol; input must be immutable or caller cloned.
    fn(data)
    times=[]; peaks=[]
    for _ in range(repeats):
        if is_tracing():raise RuntimeError("other tracemalloc sampler already active")
        start()
        try:
            t0=perf_counter_ns()
            fn(data)
            t1=perf_counter_ns()
            _,peak=get_traced_memory()
        finally:
            stop()
        times.append(t1-t0);peaks.append(peak)
    return {"median_wall_ns":median(times),"max_traced_python_bytes":max(peaks),
            "runtime":"CPython","hardware_power_watts":None,
            "sensor_inference_latency_verified":False,
            "Tank_G3":False,"warning":"unloaded CPython software overhead, not embedded real-time guarantee"}
