package com.servoc.app.chemcore;

import org.json.JSONObject;
import java.util.Map;

/**
 * Expose the available research fingerprint computation as a SCREENING result
 * even when no validated open-set model exists. Identity promotion remains a
 * distinct later decision. Existing ChemCore ranking and PPM are untouched.
 */
public final class TankFingerprintScreening {
    public static final String KEY="research_fingerprint_screening";
    private TankFingerprintScreening(){}

    public static JSONObject evaluate(Map<String,Double> ratios){
        JSONObject out=new JSONObject();
        try{
            AcceptedFingerprintIdentityEngine.Decision d=
                AcceptedFingerprintIdentityEngine.evaluate(ratios);
            out.put("method","SOURCE_BACKED_MQ_ARRAY_FINGERPRINT");
            out.put("source",AcceptedFingerprintIdentityEngine.VERSION);
            out.put("calibrated_identity",false);
            out.put("calibrated_ppm",false);
            out.put("fingerprint_available",d.top!=null);
            out.put("research_match_passed_heuristic",d.accepted);
            out.put("note",d.reason);
            if(d.top!=null){
                int legacy=d.top.fingerprint.chemicalId;
                long uid=ChemCoreLegacyCandidateRegistry.uidForLegacyChemical(legacy);
                out.put("legacy_id",legacy);
                if(uid>0)out.put("chemical_uid",uid);
                out.put("label",d.top.fingerprint.label);
                out.put("contributors",d.top.contributors);
                if(Double.isFinite(d.top.distance))out.put("distance",d.top.distance);
                if(Double.isFinite(d.top.score))out.put("score",d.top.score);
            }
            if(d.runnerUp!=null){
                out.put("runner_up",d.runnerUp.fingerprint.label);
                if(Double.isFinite(d.runnerUp.distance))
                    out.put("runner_distance",d.runnerUp.distance);
            }
        }catch(Exception ex){
            try{out.put("method","SOURCE_BACKED_MQ_ARRAY_FINGERPRINT");
                out.put("fingerprint_available",false);
                out.put("status","EVALUATION_FAILED:"+ex.getClass().getSimpleName());}
            catch(Exception ignored){}
        }
        return out;
    }
}
