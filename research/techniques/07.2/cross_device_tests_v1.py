"""Research-only leave-one-device-out evaluation splits."""
def leave_one_device_out(rows):
    data=list(rows)
    if not data or any(not isinstance(r,dict) or not isinstance(r.get("device"),str) or not r["device"] for r in data):raise ValueError("device-tagged rows required")
    devices=sorted({r["device"] for r in data})
    if len(devices)<2:raise ValueError("at least two devices required")
    return [{"device":d,"train_indices":[i for i,r in enumerate(data) if r["device"]!=d],
             "test_indices":[i for i,r in enumerate(data) if r["device"]==d]} for d in devices]
def summarize(results):
    data=list(results)
    if not data or any(not isinstance(r,dict) or not isinstance(r.get("correct"),int) or isinstance(r["correct"],bool) or not isinstance(r.get("total"),int) or isinstance(r["total"],bool) or r["total"]<=0 or not 0<=r["correct"]<=r["total"] for r in data):raise ValueError("invalid per-device counts")
    return {"macro_accuracy":sum(r["correct"]/r["total"] for r in data)/len(data),
            "micro_accuracy":sum(r["correct"] for r in data)/sum(r["total"] for r in data),
            "n_devices":len(data)}
