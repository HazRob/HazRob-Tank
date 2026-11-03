package com.hazrob.tank.emissions;

import com.servoc.app.chemcore.TankInstantAnalyzer;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;

/**
 * Read-only emissions projection of an ACTUAL completed ChemCore Next
 * immutable decision + its separate, provenance-labelled source curve ledger.
 * This never modifies ChemCore Next, its rankings, source models, or Stable.
 *
 * Gas identity and actual exhaust concentration remain UNCONFIRMED.  Curves
 * are competing research hypotheses only and never an IFC compliance value.
 */
public final class EmissionNextBridge {
    public static final String SCHEMA="hazrob-tank-emissions-next-projection/1";
    private EmissionNextBridge(){}

    private static boolean applies(String pollutant,String sourceLabel){
        String label=sourceLabel==null?"":sourceLabel.trim().toLowerCase(Locale.ROOT);
        switch(pollutant){
            case "CH4": return label.equals("ch4")||label.equals("methane");
            case "CO2": return label.equals("co2")||label.equals("carbon dioxide");
            case "SMOKE": return label.equals("smoke");
            // The current source curve bank does not contain validated NOx/SOx
            // response models. Never substitute sulfides for oxides of sulfur.
            default: return false;
        }
    }
    /** Only preserve *source* numerical ppm fits as conditional estimates.
     * They are NOT measured concentrations, NOx/SOx evidence or IFC exceedances.
     */
    public static JSONObject attach(JSONObject emissions,TankInstantAnalyzer.Result analyzed)
        throws org.json.JSONException {
        if(emissions==null||analyzed==null||analyzed.analysis==null)
            throw new IllegalArgumentException("actual Next analysis required");
        JSONObject next=analyzed.analysis;
        if(!"hazrob.tank.v8.next.decision.presentation/1".equals(next.optString("schema")))
            throw new IllegalArgumentException("NOT_A_NEXT_DECISION");
        JSONArray rows=emissions.optJSONArray("pollutants");
        if(rows==null)throw new IllegalArgumentException("emission matrix missing");
        JSONArray curves=next.optJSONArray("source_curve_hypotheses");
        int linked=0,finite=0;
        for(int i=0;i<rows.length();i++){
            JSONObject row=rows.getJSONObject(i);
            String key=row.optString("pollutant","");
            JSONArray hypotheses=new JSONArray();
            if(curves!=null)for(int c=0;c<curves.length();c++){
                JSONObject fit=curves.optJSONObject(c);
                if(fit==null||!applies(key,fit.optString("gas_or_class","")))continue;
                double ppm=fit.optDouble("estimated_ppm",Double.NaN);
                JSONObject hypothesis=new JSONObject()
                    .put("source_curve_id",fit.optString("source_curve_id",""))
                    .put("source_root",fit.optString("source_root",""))
                    .put("source_label",fit.optString("gas_or_class",""))
                    .put("mq_sensor",fit.optString("sensor_model",""))
                    .put("research_estimate_ppm",
                         Double.isFinite(ppm)&&ppm>0?ppm:JSONObject.NULL)
                    .put("status",fit.optString("status","NOT_RUN"))
                    .put("extrapolated",fit.optBoolean("extrapolated",false))
                    .put("device_r0_source_r0_equivalent",false)
                    .put("ifc_comparable",false)
                    .put("gas_identity_confirmed",false);
                hypotheses.put(hypothesis);
                linked++;
                if(Double.isFinite(ppm)&&ppm>0)finite++;
            }
            row.put("next_source_hypotheses",hypotheses);
            row.put("next_source_hypotheses_count",hypotheses.length());
            row.put("next_research_ppm_hypotheses",
                hypotheses.length()>0?"CONDITIONAL_ONLY":"NO_MODEL_AVAILABLE");
            // Keep certified exhaust ppm empty regardless of research inverse
            // curve numerical values. Only a later qualified inference can
            // populate a measured concentration and comparable IFC limit.
            row.put("value_ppm",JSONObject.NULL);
            row.put("ifc_comparable",false);
            row.put("exceeded",false);
            if(hypotheses.length()>0)row.put("status",
                finiteFor(hypotheses)>0?"NEXT_SOURCE_HYPOTHESES":"NEXT_MODELS_NOT_EVALUABLE");
        }
        emissions.put("analysis_mode","CHEMCORE_NEXT_SCOPED_EMISSIONS");
        emissions.put("mode","CHEMCORE_NEXT_SCOPED_EMISSIONS");
        emissions.put("next_projection_schema",SCHEMA);
        emissions.put("next_decision_uid",next.optString("decision_uid",""));
        emissions.put("next_session_id",next.optString("session_id",""));
        emissions.put("next_snapshot_sha256",next.optString("snapshot_sha256",""));
        emissions.put("stable_manifest_sha256",next.optString("stable_manifest_sha256",""));
        emissions.put("next_model_version",next.optString("model_version",""));
        emissions.put("next_decision_status",next.optString("decision_status","UNKNOWN"));
        emissions.put("next_candidate_count",next.optInt("candidate_count",0));
        emissions.put("next_analysis",new JSONObject(next.toString()));
        emissions.put("next_source_hypotheses",linked);
        emissions.put("next_finite_ppm_hypotheses",finite);
        emissions.put("next_status","SUCCESS_RESEARCH_ONLY");
        emissions.put("ifc_assessment_state","NOT_EVALUABLE_NO_QUALIFIED_EXHAUST_CONCENTRATIONS");
        return emissions;
    }
    private static int finiteFor(JSONArray list){
        int count=0;
        for(int i=0;i<list.length();i++){
            JSONObject q=list.optJSONObject(i);
            if(q!=null&&!q.isNull("research_estimate_ppm")&&
               Double.isFinite(q.optDouble("research_estimate_ppm",Double.NaN)))count++;
        }
        return count;
    }
    public static JSONObject unavailable(JSONObject emissions,String reason)
        throws org.json.JSONException{
        if(emissions==null)throw new IllegalArgumentException("measurement required");
        emissions.put("analysis_mode","CHEMCORE_NEXT_SCOPED_EMISSIONS");
        emissions.put("mode","CHEMCORE_NEXT_SCOPED_EMISSIONS");
        emissions.put("next_projection_schema",SCHEMA);
        emissions.put("next_status","NOT_RUN");
        emissions.put("next_unavailable_reason",reason==null?"UNKNOWN":reason);
        emissions.put("ifc_assessment_state","NOT_EVALUABLE_NEXT_NOT_RUN");
        return emissions;
    }
}
