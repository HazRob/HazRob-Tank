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
        if(analyzed==null||analyzed.analysis==null)
            throw new IllegalArgumentException("actual Next analysis required");
        return attachAnalysis(emissions,analyzed.analysis);
    }
    /** Also permits testing the read-only source projection with frozen Next receipts. */
    public static JSONObject attachAnalysis(JSONObject emissions,JSONObject next)
        throws org.json.JSONException {
        if(emissions==null||next==null)
            throw new IllegalArgumentException("actual Next decision required");
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
                    .put("source_class",fit.optString("source_class","SOURCE_NOT_CATEGORIZED"))
                    .put("source_label",fit.optString("gas_or_class",""))
                    .put("mq_sensor",fit.optString("sensor_model",""))
                    .put("research_estimate_ppm",
                         Double.isFinite(ppm)&&ppm>0?ppm:JSONObject.NULL)
                    .put("source_equivalent_unit","SMOKE".equals(key)?
                        "ppm source-equivalent (not PM mass)":"ppm (source model)")
                    .put("status",fit.optString("status","NOT_RUN"))
                    .put("extrapolated",fit.optBoolean("extrapolated",false))
                    .put("reference_min_ppm",fit.opt("reference_min_ppm"))
                    .put("reference_max_ppm",fit.opt("reference_max_ppm"))
                    .put("source_equation",fit.optString("source_equation",""))
                    .put("algorithm",fit.optString("algorithm",""))
                    .put("model_a",fit.opt("a"))
                    .put("model_b",fit.opt("b"))
                    .put("reference_scale_ppm",fit.opt("reference_scale_ppm"))
                    .put("observed_rs_r0",fit.opt("observed_rs_r0"))
                    .put("r0_definition",fit.optString("r0_definition",""))
                    .put("source_r0_equivalence_verified",
                        fit.optBoolean("source_r0_equivalence_verified",false))
                    .put("caveat",fit.optString("caveat","DEVICE_R0_TRANSFER_NOT_VERIFIED"))
                    .put("ifc_comparable",false)
                    .put("gas_identity_confirmed",false);
                hypotheses.put(hypothesis);
                linked++;
                if(Double.isFinite(ppm)&&ppm>0)finite++;
            }
            // Every finite manufacturer/research source model is displayed;
            // NO arbitrary 1-model/3-model cap, no out-of-domain suppression.
            // Separate the analytical numbers from certified exhaust ppm.
            JSONArray numeric=new JSONArray();
            for(int h=0;h<hypotheses.length();h++){
                JSONObject source=hypotheses.optJSONObject(h);
                if(source!=null&&!source.isNull("research_estimate_ppm"))
                    numeric.put(new JSONObject(source.toString()));
            }
            row.put("next_source_hypotheses",hypotheses);
            row.put("next_source_hypotheses_count",hypotheses.length());
            row.put("research_numeric_models",numeric);
            row.put("research_numeric_models_count",numeric.length());
            row.put("research_values_visible",true);
            row.put("next_research_ppm_hypotheses",
                hypotheses.length()>0?"SOURCE_MODELS_DISPLAYED":"NO_MODEL_AVAILABLE");
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
        emissions.put("numerical_output_policy",
            "DISPLAY_ALL_FINITE_SOURCE_CURVE_ESTIMATES_WITH_STATUS_AND_PROVENANCE");
        emissions.put("source_models_suppressed_due_to_g3",false);
        emissions.put("source_extrapolations_suppressed",false);
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
