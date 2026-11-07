"""06.2 — guard a research Bayesian likelihood update with physical lineage.

Declaring 'independent' in two classifier outputs from one recording does not
make the products conditionally independent. Real dependence validation remains.
"""
from bayesian_inference_v1 import update_posterior

def guarded_bayesian_update(prior, evidence, *, source_id):
    rows=list(evidence)
    if not source_id or not isinstance(source_id,str):
        raise ValueError("source id required")
    if not rows:
        return {"status":"ABSTAIN_NO_EVIDENCE","Tank_G3":False}
    identities=set(); physical=set(); clinical_groups=set()
    for row in rows:
        if not isinstance(row,dict) or row.get("source_id")!=source_id:
            raise ValueError("source mismatch")
        for key in ("id","session_id","sensor_array_id","independent_group","independence_audit_ref"):
            if not isinstance(row.get(key),str) or not row[key].strip():
                raise ValueError("independent evidence provenance required: "+key)
        phys=(row["session_id"],row["sensor_array_id"])
        if row["id"] in identities:
            raise ValueError("duplicate evidence id")
        identities.add(row["id"])
        if phys in physical or row["independent_group"] in clinical_groups:
            return {"status":"ABSTAIN_DEPENDENT_EVIDENCE",
                    "reason":"JOINT_LIKELIHOOD_REQUIRED","Tank_G3":False}
        physical.add(phys);clinical_groups.add(row["independent_group"])
    original=[{"id":r["id"],"conditional_independence_justified":True,
               "likelihoods":r.get("likelihoods")} for r in rows]
    result=update_posterior(prior,original)
    result["status"]="RESEARCH_BAYES_POSTERIOR_ASSUMPTION_ONLY"
    result["dependence_audit_refs"]=tuple(sorted({r["independence_audit_ref"] for r in rows}))
    result["probability_empirically_calibrated"]=False
    result["Tank_G3"]=False
    return result
