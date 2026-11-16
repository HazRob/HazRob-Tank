"""08.3: check recurrent state isolation between exposure events.
This simple tanh recurrence is NOT an LSTM or GRU; trained GRU benchmark is separate.
"""
from math import isfinite,tanh
def isolated_recurrence(exposures, *, source_id,decay=.7,input_gain=.3):
    if not source_id or not isinstance(source_id,str):raise ValueError("source required")
    if any(isinstance(x,bool) or not isinstance(x,(int,float)) or not isfinite(x) for x in (decay,input_gain)):
        raise ValueError("finite weights required")
    if not 0<=decay<1 or input_gain<=0:raise ValueError("unstable recurrence weights")
    data=list(exposures)
    if not data:raise ValueError("independent exposure rows required")
    groups=set();outputs=[]
    for e in data:
        if not isinstance(e,dict) or e.get("source_id")!=source_id or not e.get("exposure_id") or not e.get("session_id"):
            raise ValueError("source/session/exposure identity missing")
        if e["exposure_id"] in groups:raise ValueError("duplicate exposure used as training event")
        groups.add(e["exposure_id"])
        seq=list(e.get("signal",[]))
        if not seq or any(isinstance(x,bool) or not isinstance(x,(int,float)) or not isfinite(x) for x in seq):
            raise ValueError("numeric signal expected")
        state=0.0  # critical: reset for EVERY independent event
        trace=[]
        for value in seq:
            state=tanh(decay*state+input_gain*value);trace.append(state)
        outputs.append({"exposure_id":e["exposure_id"],"trajectory":trace})
    return {"per_exposure":outputs,"hidden_state_reset":True,"actual_LSTM_or_GRU_trained":False,"Tank_G3":False}
