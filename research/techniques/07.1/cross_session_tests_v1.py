"""Research-only leave-one-session-out partitioning and summary."""
def folds(rows):
    data=list(rows)
    if not data or any(not isinstance(r,dict) or not isinstance(r.get("session"),str) or not r["session"] for r in data):raise ValueError("session-tagged rows required")
    sessions=sorted({r["session"] for r in data})
    if len(sessions)<2:raise ValueError("at least two sessions required")
    return [{"held_out_session":s,"train_indices":[i for i,r in enumerate(data) if r["session"]!=s],
             "test_indices":[i for i,r in enumerate(data) if r["session"]==s]} for s in sessions]
def summarize(session_results):
    rows=list(session_results)
    if not rows or any(not isinstance(x,dict) or not isinstance(x.get("n"),int) or x["n"]<=0 or not isinstance(x.get("correct"),int) or not 0<=x["correct"]<=x["n"] for x in rows):raise ValueError("valid session counts required")
    return {"n_sessions":len(rows),"micro_accuracy":sum(r["correct"] for r in rows)/sum(r["n"] for r in rows),
            "macro_session_accuracy":sum(r["correct"]/r["n"] for r in rows)/len(rows),
            "warning":"Descriptive only; model fitting and calibration must be train-fold-only."}
