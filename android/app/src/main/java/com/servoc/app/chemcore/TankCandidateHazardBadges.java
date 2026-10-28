package com.servoc.app.chemcore;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Candidate-specific conditional symbols sourced from the EXACT analysis
 * payload. Never infers a chemical identity, LFL, ppm or exposure threshold.
 * A stale candidate cannot inherit another candidate's warning.
 *
 * Because source ppm is not physically qualified, symbols are explicitly
 * advisory; TWA/STEL comparisons do NOT prove a time-weighted exceedance.
 */
public final class TankCandidateHazardBadges {
    private TankCandidateHazardBadges(){}

    public static String forCandidate(JSONObject analysis,long chemicalUid){
        if(analysis==null||chemicalUid<0)return "";
        JSONObject screening=analysis.optJSONObject(TankScreeningExposureWarning.KEY);
        JSONArray warnings=screening==null?null:screening.optJSONArray("warnings");
        if(warnings==null)return "";
        boolean exposure=false,lfl=false,approach=false,timeBased=false;
        for(int i=0;i<warnings.length();i++){
            JSONObject w=warnings.optJSONObject(i);
            if(w==null||w.optLong("chemical_uid",-1)!=chemicalUid)continue;
            // Never re-label failed or uncertified estimates as confirmed.
            if(!w.optBoolean("conditional",true))continue;
            double ratio=w.optDouble("ratio_to_reference",Double.NaN);
            if(!Double.isFinite(ratio)||ratio<0)continue;
            String role=w.optString("reference_role","");
            if("LFL".equals(role)){
                if(ratio>=1.0)lfl=true;
                else if(ratio>=0.10)approach=true;
            }else if(ratio>=1.0&&("IDLH".equals(role)||
                    "CEILING".equals(role)||"TWA".equals(role)||"STEL".equals(role))){
                exposure=true;
                if("TWA".equals(role)||"STEL".equals(role))timeBased=true;
            }
        }
        if(!exposure&&!lfl&&!approach)return "";
        StringBuilder b=new StringBuilder("  ");
        if(exposure)b.append("☠");
        if(lfl)b.append("🔥");
        if(exposure||lfl){
            b.append(" [تقديري/مشروط]");
            if(timeBased)b.append(" [التعرض المستمر افتراضي]");
        }
        if(approach&&!lfl)b.append(" ⚠ اقتراب LFL 10%");
        return b.toString();
    }
}
