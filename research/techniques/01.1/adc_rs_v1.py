"""Research-only voltage divider MQ Rs model; RL at ground, Rs at Vc."""
from math import isfinite

def rs_from_vrl(vrl_volts, vc_volts, rl_ohms):
    if not all(isfinite(x) for x in (vrl_volts, vc_volts, rl_ohms)):
        raise ValueError("non-finite input")
    if vc_volts <= 0 or rl_ohms <= 0 or not 0 < vrl_volts < vc_volts:
        raise ValueError("invalid divider or rail reading")
    return rl_ohms * (vc_volts / vrl_volts - 1.0)

def adc_to_voltage(raw_code, full_scale_code, full_scale_volts, divider_gain=1.0):
    if not isinstance(raw_code, int) or not isinstance(full_scale_code, int):
        raise TypeError("ADC codes must be integers")
    if full_scale_code <= 0 or not 0 <= raw_code <= full_scale_code:
        raise ValueError("invalid ADC code")
    if not all(isfinite(x) for x in (full_scale_volts, divider_gain)):
        raise ValueError("non-finite parameter")
    if full_scale_volts <= 0 or divider_gain < 1:
        raise ValueError("invalid scale or divider gain")
    return raw_code / full_scale_code * full_scale_volts * divider_gain

def rs_from_adc(raw_code, full_scale_code, full_scale_volts, divider_gain, vc_volts, rl_ohms):
    return rs_from_vrl(adc_to_voltage(raw_code, full_scale_code, full_scale_volts, divider_gain), vc_volts, rl_ohms)
