#!/usr/bin/env python3
"""HazRob Tank V8 / P11.1: research-only R_air versus MQSensorsLib R0 algebra.

The eight R_air values were measured on Tank (2026-11-02), in kOhm.
Coefficients and K are pinned to MQSensorsLib upstream source blobs:
 MQ-135-ALL.ino: 3ffae99864cfc51c12941647290832dc39767bb2
 MQ-Board.ino: ce78fce7d9f2af0465924d9bd317da1f7ba486b8
 MQUnifiedsensor.cpp: 5b282c7c178f41478d2de2258d98b54ce269d407

No real gas Rs series is included; test Rs is hypothetical as R_air*q.
This checks mathematical equivalence, NOT source-K validity, gas identity,
reference calibration, PPM accuracy, field alarms, or Tank G3.
"""
import json
import math
import random

AIR_KOHM = {
    "MQ2": 92.1417313, "MQ3": 25.5138416, "MQ4": 37.8870316,
    "MQ5": 158.0950317, "MQ7": 62.5898705, "MQ8": 36.5759659,
    "MQ9": 56.4506149, "MQ135": 113.6592255,
}
# FROM MQSensorsLib examples, NOT manufacturer-universal constants.
SOURCE_EXAMPLE_K = {
    "MQ2": 9.83, "MQ3": 60.0, "MQ4": 4.4, "MQ5": 6.5,
    "MQ7": 27.5, "MQ8": 70.0, "MQ9": 9.6, "MQ135": 3.6,
}
# Original source equation: ppm=A*(Rs/R0_example)^B; source labels are NOT
# confirmed chemical identities. The NH4 label is deliberately unresolved.
CURVES = [
    ("MQ135","CO",605.18,-3.937), ("MQ135","Alcohol",77.255,-3.18),
    ("MQ135","CO2",110.47,-2.862), ("MQ135","Toluene",44.947,-3.445),
    ("MQ135","NH4_UNRESOLVED_CLASS",102.2,-2.473),
    ("MQ135","Acetone",34.668,-3.369),
    ("MQ2","LPG",574.25,-2.222), ("MQ3","Alcohol",0.3934,-1.504),
    ("MQ4","CH4",1012.7,-2.786), ("MQ5","Alcohol",97124.0,-4.918),
    ("MQ7","CO",99.042,-1.518), ("MQ8","H2",976.97,-0.688),
    ("MQ9","LPG",1000.5,-2.186),
]

def main():
    assert len(CURVES)==13 and len(AIR_KOHM)==len(SOURCE_EXAMPLE_K)==8
    rng=random.Random(20261103)
    checks=0
    max_relative=0.0
    comparison=[]
    for model, gas, a, b in CURVES:
        rair=AIR_KOHM[model]
        k=SOURCE_EXAMPLE_K[model]
        assert all(math.isfinite(v) and v>0 for v in (rair,k,a))
        assert math.isfinite(b) and b<0
        calculated_r0_example=rair/k
        # 12 fixed + 100 fixed-seed random values per source curve:
        # compares (i) R0 physical division, (ii) use R_air and q*K,
        # (iii) use R_air and coefficient A'=A*K^B.
        ratios=[0.01,0.05,0.1,0.2,0.3,0.5,0.8,1.0,1.25,2.0,5.0,10.0]
        ratios += [10**rng.uniform(-2.0,1.0) for _ in range(100)]
        for q in ratios:
            rs=rair*q
            derived=a*(rs/calculated_r0_example)**b
            source_ratio=a*(k*rs/rair)**b
            transformed_coeff=(a*k**b)*(rs/rair)**b
            assert all(math.isfinite(p) and p>0 for p in
                       (derived,source_ratio,transformed_coeff))
            assert math.isclose(derived,source_ratio,rel_tol=3e-14)
            assert math.isclose(derived,transformed_coeff,rel_tol=3e-14)
            max_relative=max(max_relative,
                abs(derived-source_ratio)/derived,
                abs(derived-transformed_coeff)/derived)
            checks+=2
        # No mutation of original R_air: source R0 exists only in a derived view.
        assert AIR_KOHM[model]==rair and calculated_r0_example!=rair
        checks+=1
        example_ppm=a*(0.5*k)**b
        wrong_no_conversion=a*0.5**b
        comparison.append({
            "model":model,"source_label":gas,"source_example_K":k,
            "tank_measured_R_air_kohm":rair,
            "derived_R0_example_kohm_research_only":calculated_r0_example,
            "hypothetical_Rs_over_air":0.5,
            "source_model_ppm_research_only":example_ppm,
            "wrong_naive_over_correct_factor":wrong_no_conversion/example_ppm,
            "clean_air_model_nonzero_ppm":a*k**b,
            "K_plus_10_percent_model_ppm_change_pct":100*(1.1**b-1),
            "K_minus_10_percent_model_ppm_change_pct":100*(0.9**b-1),
        })
    assert checks==2925 and max_relative<3e-14
    assert all(x["clean_air_model_nonzero_ppm"]>0 for x in comparison)
    # Safety regression: the previous air-normalized response gate may compare
    # Rs/R_air with a 10% relative drop threshold (0.90). If a converted
    # source R0 is written over R_air, this original gate can miss changes.
    # This is a hypothetical gate-behavior test, not an alarm accuracy claim.
    hypothetical_air_drop=0.5
    gate_cutoff=0.9
    safety_checks=0
    for model,k in SOURCE_EXAMPLE_K.items():
        air_ratio=hypothetical_air_drop
        source_ratio=hypothetical_air_drop*k
        assert air_ratio<gate_cutoff
        assert not source_ratio<gate_cutoff
        safety_checks+=2
    report={
        "schema":"hazrob.tank.v8.p11_1.r0_reference_equivalence_g1/1",
        "scope":"MATH_AND_SOURCE_EXAMPLE_ONLY_NO_HARDWARE_OR_G3",
        "physical_Rs_measurements_used":False,
        "curve_count":len(CURVES), "measured_Tank_air_reference_channels":8,
        "numeric_assertions_passed":checks,
        "hypothetical_air_normalization_gate_assertions_passed":safety_checks,
        "hypothetical_50pct_air_drop_missed_if_source_ratio_used":len(SOURCE_EXAMPLE_K),
        "max_relative_equivalence_error":max_relative,
        "naive_multiplier_range":[min(c["wrong_naive_over_correct_factor"] for c in comparison),
                                  max(c["wrong_naive_over_correct_factor"] for c in comparison)],
        "source_K_is_manufacturer_universal":False,
        "all_source_gas_identities_and_ppm_qualified":False,
        "models":comparison,
    }
    print(json.dumps(report,ensure_ascii=False,indent=2))

if __name__=="__main__":
    main()
