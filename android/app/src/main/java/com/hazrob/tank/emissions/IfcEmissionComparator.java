package com.hazrob.tank.emissions;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;

/** Mathematically compares every ChemCore Next source-model output only against
 * the appropriate IFC General EHS Guidelines (30 Apr 2007) Table 1.1.2.
 * This class DOES NOT change, censor, or re-rank the scientific source estimates.
 *
 * Applicability: combustion engines at 3-50 MWth, >500 h/y and capacity use
 * >30%, dry flue gas at 0 C / 1 atm with 15% dry O2 reference.
 * The source MQ telemetry does not establish those conditions, therefore a
 * comparison is NUMERIC/INDICATIVE and never a confirmed compliance finding.
 *
 * Source: https://www.ifc.org/content/dam/ifc/doc/2000/
 * 2007-general-ehs-guidelines-air-emissions-and-ambient-air-quality-en.pdf
 * (p.7, Table 1.1.2, engine rows).
 */
public final class IfcEmissionComparator {
    public static final String SCHEMA="hazrob.ifc.2007.table-1.1.2.numeric-comparison/1";
    public static final String SOURCE_URL="https://www.ifc.org/content/dam/ifc/doc/2000/"
        +"2007-general-ehs-guidelines-air-emissions-and-ambient-air-quality-en.pdf";
    public static final String TABLE="IFC EHS 2007 §1.1 Table 1.1.2, Engine (3-50 MWth)";
    public static final String NONE="UNSPECIFIED";
    public static final String GAS_SPARK="GAS_SPARK";
    public static final String GAS_DUAL="GAS_DUAL";
    public static final String GAS_COMPRESSION="GAS_COMPRESSION";
    public static final String LIQUID_BORE_LT400="LIQUID_BORE_LT400";
    public static final String LIQUID_BORE_GTE400="LIQUID_BORE_GTE400";
    public static final String[] PROFILES={NONE,GAS_SPARK,GAS_DUAL,GAS_COMPRESSION,
        LIQUID_BORE_LT400,LIQUID_BORE_GTE400};
    public static final String[] LABELS={
        "اختر فئة IFC للمحرك (غير محددة)",
        "غازي — إشعال شراري (200 NOx)",
        "غازي — وقود مزدوج (400 NOx)",
        "غازي — إشعال بالضغط (1600 NOx)",
        "وقود سائل — قطر الأسطوانة أقل من 400 ملم (1460 NOx)",
        "وقود سائل — قطر الأسطوانة 400 ملم أو أكثر (1850 NOx)"
    };
    private IfcEmissionComparator(){}
    private static boolean finite(double x){return Double.isFinite(x)&&x>=0;}
    public static int indexOf(String code){
        for(int i=0;i<PROFILES.length;i++)if(PROFILES[i].equals(code))return i;
        return 0;
    }
    public static String profile(int index){
        return PROFILES[Math.max(0,Math.min(index,PROFILES.length-1))];
    }
    public static double noxLimitMgNm3(String profile){
        if(GAS_SPARK.equals(profile))return 200;
        if(GAS_DUAL.equals(profile))return 400;
        if(GAS_COMPRESSION.equals(profile))return 1600;
        if(LIQUID_BORE_LT400.equals(profile))return 1460;
        if(LIQUID_BORE_GTE400.equals(profile))return 1850;
        return Double.NaN;
    }
    /** 1 mol ideal gas = 22.414 L at 273.15 K, 1 atm; NO2 molar mass 46.0055.
     * 1 ppmv NO2 = 46.0055 / 22.414 mg/Nm3. Does not compensate exhaust O2.
     */
    public static double no2PpmToMgNm3(double ppm){
        return ppm*46.0055/22.414;
    }
    private static boolean no2Basis(String gas){
        String label=gas==null?"":gas.trim().toLowerCase(Locale.ROOT);
        return label.equals("no2")||label.equals("nitrogen dioxide");
    }
    /** Results may contain many manufacturer/research source curves. Evaluate
     * all such curves, never average away contradictory source results.
     */
    public static JSONObject apply(JSONObject scan,String profile) throws org.json.JSONException {
        if(scan==null)throw new IllegalArgumentException("scan required");
        String selected=PROFILES[indexOf(profile)];
        scan.put("ifc_reference_schema",SCHEMA)
            .put("ifc_reference_table",TABLE)
            .put("ifc_reference_url",SOURCE_URL)
            .put("ifc_profile",selected)
            .put("ifc_reference_oxygen_percent",15)
            .put("ifc_reference_dry_gas",true)
            .put("ifc_reference_normal_celsius",0)
            .put("ifc_scope_checked",false)
            .put("ifc_comparison_is_regulatory_certification",false);
        JSONArray rows=scan.optJSONArray("pollutants");
        if(rows==null)return scan;
        for(int i=0;i<rows.length();i++){
            JSONObject row=rows.optJSONObject(i);if(row==null)continue;
            String pollutant=row.optString("pollutant","");
            double limit="NOX".equals(pollutant)?noxLimitMgNm3(selected):
                ("PM_TOTAL".equals(pollutant)&&selected.startsWith("LIQUID_")?50:Double.NaN);
            row.put("ifc_reference_url",SOURCE_URL)
               .put("ifc_profile",selected)
               .put("ifc_limit_value",finite(limit)?limit:JSONObject.NULL)
               .put("ifc_limit_unit",finite(limit)?"mg/Nm3":"N/A")
               .put("ifc_reference_o2_percent",15)
               .put("ifc_comparison_basis","RESEARCH_NUMERIC_ONLY_O2_UNVERIFIED")
               .put("ifc_numeric_exceeded",false)
               .put("ifc_numeric_comparable",false)
               .put("ifc_comparison_status",finite(limit)?
                   "NO_SAME_SPECIES_QUANTITATIVE_RESULT":"NO_IFC_NUMERIC_LIMIT_FOR_THIS_POLLUTANT");
            if("SOX".equals(pollutant)&&selected.startsWith("LIQUID_")){
                row.put("ifc_comparison_status","FUEL_SULFUR_LIMIT_NOT_EXHAUST_PPM");
                row.put("ifc_fuel_sulfur_mass_percent_reference",1.5);
            }
            JSONArray models=row.optJSONArray("next_source_hypotheses");
            int computed=0,above=0,below=0;
            if(models!=null)for(int j=0;j<models.length();j++){
                JSONObject model=models.optJSONObject(j);if(model==null)continue;
                model.put("ifc_reference_limit",finite(limit)?limit:JSONObject.NULL)
                     .put("ifc_reference_unit",finite(limit)?"mg/Nm3":"N/A")
                     .put("ifc_numeric_exceeded",false)
                     .put("ifc_numeric_comparable",false);
                double ppm=model.optDouble("research_estimate_ppm",Double.NaN);
                // ONLY verified species basis NO2 can mathematically compare
                // with IFC NOx expressed conventionally as NO2 equivalents.
                // Never convert 'smoke ppm' into stack PM mass.
                boolean sameSpecies="NOX".equals(pollutant)&&
                    no2Basis(model.optString("source_label",""));
                if(!finite(limit)||!finite(ppm)||!sameSpecies)continue;
                double mg=no2PpmToMgNm3(ppm);
                boolean exceeded=mg>limit;
                model.put("ifc_computed_mg_nm3",mg)
                     .put("ifc_numeric_comparable",true)
                     .put("ifc_numeric_exceeded",exceeded)
                     .put("ifc_reference_ratio",mg/limit)
                     .put("ifc_reference_percent",mg/limit*100)
                     .put("ifc_comparison_qualification",
                          "UNVERIFIED_DRY_15PCT_O2_AND_SOURCE_R0");
                computed++;
                if(exceeded)above++;else below++;
            }
            if(computed>0){
                row.put("ifc_numeric_comparable",true)
                   .put("ifc_numeric_exceeded",above>0)
                   .put("ifc_models_compared",computed)
                   .put("ifc_models_above",above)
                   .put("ifc_models_at_or_below",below)
                   .put("ifc_comparison_status",above==0?
                        "NUMERIC_AT_OR_BELOW_IFC":below==0?
                        "NUMERIC_ABOVE_IFC":"DISAGREEMENT_BETWEEN_SOURCE_MODELS");
            }
            // The original source ppm outputs are retained, and the standard
            // formal 'exceeded' field is reserved for verified stack tests.
            row.put("exceeded",false).put("ifc_comparable",false);
        }
        scan.put("ifc_assessment_state",NONE.equals(selected)?
            "IFC_PROFILE_NOT_SELECTED":"IFC_NUMERIC_SCREENING_ONLY");
        return scan;
    }
}
