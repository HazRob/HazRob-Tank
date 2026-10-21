"""04.9 inverse hypothesis ranking: iterate candidates, never expose test-gas ID.

A fitted QSPR forward model can only rank known provided candidates; not
confirm molecule identity, independent ppm or multi-gas unmixing.
"""
from math import isfinite,log10,sqrt

def inverse_molecular_hypotheses(model, observed_ratios, candidate_descriptors,
                                ppm_grid, *, source_id, heater_phase, r0_definition_id,
                                heldout_exposure_id=None):
    if not isinstance(model,dict) or model.get("status")!="FITTED_SOURCE_FORWARD_QSPR_UNCALIBRATED":
        raise ValueError("trained source model required")
    if (model["source_id"],model["heater_phase"],model["r0_definition_id"])!=(source_id,heater_phase,r0_definition_id):
        raise ValueError("model/source/heater/R0 definition mismatch")
    if heldout_exposure_id is not None and heldout_exposure_id in model["training_exposure_ids"]:
        raise ValueError("test exposure reused during QSPR forward training")
    ids=model["sensor_ids"]
    if set(observed_ratios)!=set(ids):
        raise ValueError("sensor response channel mismatch")
    if any(isinstance(v,bool) or not isinstance(v,(int,float)) or not isfinite(v) or v<=0
           for v in observed_ratios.values()):
        raise ValueError("all observed Rs/R0 must be positive")
    if not isinstance(candidate_descriptors,dict) or not candidate_descriptors:
        raise ValueError("candidate properties required")
    ppm_values=list(ppm_grid)
    if not ppm_values or any(isinstance(v,bool) or not isinstance(v,(int,float)) or not isfinite(v) or v<=0 for v in ppm_values):
        raise ValueError("positive candidate concentrations required")
    low,high=model["concentration_range_ppm"]
    if any(c<low or c>high for c in ppm_values):
        raise ValueError("extrapolation past measured source ppm not allowed")
    targets=[log10(observed_ratios[s]) for s in ids]
    ranked=[];excluded=[]
    for gas,desc in sorted(candidate_descriptors.items()):
        if not isinstance(gas,str) or not gas or not isinstance(desc,dict) or set(desc)!=set(model["descriptor_names"]):
            raise ValueError("candidate identifier and descriptors required")
        values=[desc[name] for name in model["descriptor_names"]]
        if any(isinstance(v,bool) or not isinstance(v,(int,float)) or not isfinite(v) for v in values):
            raise ValueError("nonfinite candidate descriptors")
        if any(not model["descriptor_ranges"][name][0]<=v<=model["descriptor_ranges"][name][1]
               for name,v in zip(model["descriptor_names"],values)):
            excluded.append(gas);continue
        options=[]
        for ppm in ppm_values:
            raw=[log10(ppm)]+values
            features=[1.]+[(v-m)/sd for v,m,sd in zip(raw,model["normalization_mean"],model["normalization_scale"])]
            predicted=[sum(a*b for a,b in zip(model["weights"][s],features)) for s in ids]
            err=sqrt(sum((x-y)**2 for x,y in zip(targets,predicted))/len(ids))
            options.append((err,ppm))
        distance,best_ppm=min(options)
        ranked.append({"candidate":gas,"log10_ratio_rmse":distance,
                       "hypothetical_ppm":best_ppm,"source_extrapolation":False})
    ranked.sort(key=lambda v:(v["log10_ratio_rmse"],v["candidate"]))
    return {"ranking":ranked,"excluded_out_of_domain":excluded,
            "decision":"CANDIDATE_RANKING_ONLY" if ranked else "ABSTAIN_OUT_OF_DOMAIN",
            "Gas_Identity_Verified":False,"PPM_Validated":False,"Tank_G3":False,
            "warning":"Dataset-trained QSPR association only; PubChem descriptors do not prove chemical identity."}
