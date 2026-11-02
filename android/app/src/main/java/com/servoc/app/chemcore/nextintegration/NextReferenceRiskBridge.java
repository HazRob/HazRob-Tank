package com.servoc.app.chemcore.nextintegration;

import com.hazrob.tank.chemcore.next.ReferenceThresholdEngine;
import com.servoc.app.chemcore.TankExposureReferenceCatalog;
import com.servoc.app.chemical.GhsPictograms;
import com.servoc.app.data.ExperimentsDb;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

/** Reference-linked conditional comparisons for each original source ppm fit.
 * No comparison changes the original ChemCore Next identity decision.
 */
public final class NextReferenceRiskBridge {
  public static final String SCHEMA="hazrob.tank.v8.next.reference_screening/1";
  private NextReferenceRiskBridge() {}

  public static JSONObject evaluate(ExperimentsDb db,JSONObject scientific)
       throws org.json.JSONException {
    if(db==null||scientific==null)throw new IllegalArgumentException("source/DB required");
    JSONObject result=new JSONObject();
    result.put("schema",SCHEMA);
    result.put("engine","ChemCore Next / ReferenceThresholdEngine");
    result.put("conditional_estimates_only",true);
    result.put("actual_occupational_exposure_proven",false);
    result.put("lfl_or_gas_identity_physically_proven",false);
    result.put("time_weighted_exposure_evaluated",false);
    JSONArray rows=new JSONArray(),classification=new JSONArray();
    result.put("comparisons",rows);
    result.put("material_ghs_references",classification);
    JSONArray source=scientific.optJSONArray("all_reference_models");
    Map<Long,JSONObject> inventory=new HashMap<>();
    int withPpm=0,notComparable=0,above=0,early=0;
    if(source!=null)for(int i=0;i<source.length();i++){
      JSONObject fit=source.optJSONObject(i);
      if(fit==null)continue;
      long uid=fit.optLong("chemical_uid",-1);
      double ppm=fit.optDouble("estimated_ppm",Double.NaN);
      if(uid<=0||!Double.isFinite(ppm)||ppm<0){notComparable++;continue;}
      withPpm++;
      JSONObject item=inventory.get(uid);
      if(item==null){
        JSONObject full=TankExposureReferenceCatalog.forSelectedChemical(
           db,uid,fit.optString("gas_or_class","—"));
        JSONArray candidates=full.optJSONArray("candidates");
        item=candidates!=null&&candidates.length()>0?candidates.optJSONObject(0):new JSONObject();
        inventory.put(uid,item);
        JSONArray pictograms=GhsPictograms.asJson(GhsPictograms.forChemical(db,uid));
        classification.put(new JSONObject()
           .put("chemical_uid",uid)
           .put("reference_material_label",fit.optString("gas_or_class","—"))
           .put("ghs_pictograms",pictograms)
           .put("ghs_scope","HAZARD_CLASSIFICATION_OF_REFERENCE_MATERIAL_NOT_DETECTION"));
      }
      JSONArray limits=item.optJSONArray("limits");
      if(limits!=null)for(int j=0;j<limits.length();j++){
        JSONObject limit=limits.optJSONObject(j);if(limit==null)continue;
        String type=limit.optString("limit_type","").toUpperCase(Locale.ROOT);
        boolean lfl=type.contains("LFL")||type.contains("LEL")||
          type.contains("LOWER_FLAMMABLE")||type.contains("LOWER_EXPLOSIVE");
        double value=limit.optDouble("value",Double.NaN);
        Double limitPpm=ReferenceThresholdEngine.ppmReference(
          value,limit.optString("unit",""),lfl);
        if(limitPpm==null)continue;
        String role=lfl?"LFL":type.contains("STEL")?"STEL":
          type.contains("TWA")||type.contains("PEL")||type.contains("REL")?"TWA":
          type.contains("IDLH")?"IDLH":
          type.contains("CEILING")?"CEILING":"OTHER";
        JSONObject row=comparison(fit,ppm,limitPpm,lfl,false);
        row.put("reference_role",role);
        row.put("authority",limit.optString("authority",""));
        row.put("reference_type",type);
        row.put("reference_value_original",value);
        row.put("reference_units_original",limit.optString("unit",""));
        row.put("source_id",limit.optString("source_id",""));
        row.put("reference_revision",limit.optString("revision",""));
        row.put("reference_duration_min",limit.opt("duration_min"));
        row.put("time_weighted_exceedance_proven",false);
        row.put("warning_kind",("TWA".equals(role)||"STEL".equals(role))?
          "HYPOTHETICAL_IF_SUSTAINED":"CONDITIONAL_CONCENTRATION_SCREENING");
        rows.put(row);
        if("AT_OR_ABOVE_REFERENCE".equals(row.optString("level")))above++;
        if("EARLY_WARNING".equals(row.optString("level")))early++;
      }
      JSONArray background=item.optJSONArray("background_ranges");
      if(background!=null)for(int j=0;j<background.length();j++){
        JSONObject normal=background.optJSONObject(j);if(normal==null)continue;
        double maximum=normal.optDouble("max",Double.NaN);
        Double maxPpm=ReferenceThresholdEngine.ppmReference(
          maximum,normal.optString("unit",""),false);
        if(maxPpm==null)continue;
        JSONObject row=comparison(fit,ppm,maxPpm,false,true);
        row.put("reference_role","BACKGROUND_MAX");
        row.put("environment_type",normal.optString("environment_type",""));
        row.put("source_id",normal.optString("source_id",""));
        row.put("qualification_status",normal.optString("qualification_status",""));
        row.put("location_scope",normal.optString("location_scope",""));
        row.put("reference_period",normal.optString("reference_period",""));
        row.put("warning_kind","CONDITIONAL_BACKGROUND_EXCEEDANCE");
        rows.put(row);
        if("AT_OR_ABOVE_REFERENCE".equals(row.optString("level")))above++;
      }
    }
    result.put("source_ppm_fit_count",withPpm);
    result.put("reference_comparison_count",rows.length());
    result.put("at_or_above_count",above);
    result.put("early_count",early);
    result.put("not_comparable_source_models",notComparable);
    result.put("status",rows.length()==0?"NO_SUITABLE_SAME_UNIT_REFERENCES":"COMPARISONS_AVAILABLE");
    return result;
  }
  private static JSONObject comparison(JSONObject fit,double ppm,double limit,
             boolean lfl,boolean natural) throws org.json.JSONException {
    JSONObject row=new JSONObject();
    row.put("source_curve_id",fit.optString("source_curve_id",""));
    row.put("source_root",fit.optString("source_root",""));
    row.put("chemical_uid",fit.optLong("chemical_uid",-1));
    row.put("material_label",fit.optString("gas_or_class","—"));
    row.put("sensor_model",fit.optString("sensor_model",""));
    row.put("estimated_ppm",ppm);
    row.put("reference_ppm",limit);
    row.put("ratio_to_reference",ReferenceThresholdEngine.ratio(ppm,limit));
    row.put("percent_of_reference",ReferenceThresholdEngine.percent(ppm,limit));
    row.put("level",ReferenceThresholdEngine.level(ppm,limit,lfl,natural));
    row.put("advisory_fraction",natural?1.0:lfl?
        ReferenceThresholdEngine.PRECAUTIONARY_LFL:ReferenceThresholdEngine.DEFAULT_EARLY_EXPOSURE);
    row.put("conditional",true);
    row.put("source_reference_r0_equivalence_verified",false);
    row.put("regulatory_exceedance_proven",false);
    if(lfl)row.put("percent_lfl",ReferenceThresholdEngine.percent(ppm,limit));
    row.put("chart_ratio_percent",ReferenceThresholdEngine.percent(ppm,limit));
    return row;
  }
}
