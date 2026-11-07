"""08.6: bounded first-order step-response MODEL only; no lab gas ppm claim."""
from math import exp,isfinite
def simulate_first_order(*,times_s,gas_reference_ppm,tau_s,starting_fraction=0.,
                         source_model_id,source_gas_label,heater_phase):
    values=list(times_s)
    if not source_model_id or not source_gas_label or heater_phase not in ("HIGH","LOW","CONSTANT"):
        raise ValueError("source gas and heater model required")
    numeric=[gas_reference_ppm,tau_s,starting_fraction]
    if any(isinstance(v,bool) or not isinstance(v,(int,float)) or not isfinite(v) for v in numeric):
        raise ValueError("finite model parameters required")
    if gas_reference_ppm<=0 or tau_s<=0 or not 0<=starting_fraction<=1:
        raise ValueError("physical model parameter invalid")
    if not values or any(isinstance(t,bool) or not isinstance(t,(int,float)) or not isfinite(t) or t<0 for t in values):
        raise ValueError("nonnegative observed times required")
    if any(b<=a for a,b in zip(values,values[1:])):
        raise ValueError("time must increase")
    out=[gas_reference_ppm*(1-(1-starting_fraction)*exp(-t/tau_s)) for t in values]
    return {"synthetic_chamber_reference_ppm":out,"source_model_id":source_model_id,
            "analytical_solution":"first_order_linear_step","actual_sensor_Rs_simulated":False,
            "is_real_exposure":False,"Tank_G3":False}
