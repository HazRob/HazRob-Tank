package com.hazrob.tank.emissions;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;

/**
 * IFC General EHS, Air Emissions and Ambient Air Quality, Table 1.1.2 p.7
 * (April 30 2007). A pure reference matching operation for reciprocating
 * ENGINES, NOT boilers/turbines or ambient-air-quality reference limits.
 * https://www.ifc.org/content/dam/ifc/doc/2000/2007-general-ehs-guidelines-en.pdf
 *
 * Applies within a verified 3-50 MWth facility operating >500 h/year and
 * utilization >30%, with stack results at dry standard 0 C, 1 atm, 15% O2.
 * Only values explicitly tagged to this same measurement basis can be
 * numerically matched; no accidental source ppm to mg/Nm3 conversion.
 */
public final class EmissionIfcComparator {
    public static final String SOURCE="IFC General EHS Air Emissions Table 1.1.2 (2007), page 7";
    public static final String URL="https://www.ifc.org/content/dam/ifc/doc/2000/2007-general-ehs-guidelines-en.pdf";
    public static final String PROFILE_NONE="NOT_CONFIGURED";
    public static final String[] PROFILE_CODES={
        PROFILE_NONE,"LIQUID_LT400","LIQUID_GE400",
        "GAS_SPARK","GAS_DUAL","GAS_COMPRESSION"
    };
    public static final String[] PROFILE_LABELS={
        "اختر فئة المحرك للمقارنة",
        "سائل (ديزل) — قطر الأسطوانة أقل من 400 مم",
        "سائل (ديزل) — قطر الأسطوانة 400 مم فأكثر",
        "غاز — إشعال شراري",
        "غاز — وقود مزدوج",
        "غاز — إشعال بالانضغاط"
    };
    private EmissionIfcComparator(){}
    public static boolean knownProfile(String profile){
        if(profile==null)return false;
        for(String p:PROFILE_CODES)if(p.equals(profile))return true;
        return false;
    }
    private static boolean liquid(String p){return p!=null&&p.startsWith("LIQUID_");}
    private static boolean gas(String p){return p!=null&&p.startsWith("GAS_");}
    private static void reference(JSONObject pollutant,Double threshold,String unit,String reason)
      throws org.json.JSONException {
        pollutant.put("ifc_reference_source",SOURCE);
        pollutant.put("ifc_reference_url",URL);
        pollutant.put("ifc_reference_basis","DRY_STACK_GAS_0C_1ATM_15PCT_O2");
        pollutant.put("ifc_reference_limit",threshold==null?JSONObject.NULL:threshold);
        pollutant.put("ifc_reference_unit",threshold==null?"-":unit);
        pollutant.put("ifc_reference_state",reason);
        pollutant.put("ifc_reference_scope",
            "ENGINES_3_TO_50_MWTH_GT500_HOURS_PER_YEAR_GT30_PERCENT_UTILIZATION");
        pollutant.put("ifc_scope_verified",false); // profile category alone cannot prove scope
        pollutant.put("limit",threshold==null?JSONObject.NULL:threshold);
        pollutant.put("limit_unit",threshold==null?"-":unit);
        pollutant.put("source",SOURCE);
    }
    /**
     * The conditional numeric comparison is only evaluated for a source
     * value whose unit AND dry O2 normalization match the IFC table.
     * The screen can show source numerical ppm for CH4/CO2/SMOKE regardless.
     */
    public static JSONObject evaluate(JSONObject frozen,String profile)
      throws org.json.JSONException {
        if(frozen==null||!knownProfile(profile))
            throw new IllegalArgumentException("Invalid IFC result/profile");
        JSONArray rows=frozen.optJSONArray("pollutants");
        if(rows==null)throw new IllegalArgumentException("Missing emission rows");
        frozen.put("ifc_profile",profile);
        frozen.put("ifc_reference",SOURCE).put("ifc_reference_url",URL);
        frozen.put("ifc_applicability","VERIFY_FACILITY_3_TO_50_MWTH_OPERATING_500H_30PCT");
        int matched=0,numeric=0,above=0;
        for(int i=0;i<rows.length();i++){
            JSONObject row=rows.optJSONObject(i);if(row==null)continue;
            String chemical=row.optString("pollutant","");
            Double threshold=null;
            String unit="mg/Nm3";
            String state="NO_DIRECT_IFC_ENGINE_STACK_LIMIT";
            if(PROFILE_NONE.equals(profile)){
                state="SELECT_ENGINE_FUEL_AND_BORE_OR_IGNITION";
            }else if("NOX".equals(chemical)){
                switch(profile){
                    case "LIQUID_LT400":threshold=1460.0;break;
                    case "LIQUID_GE400":threshold=1850.0;break;
                    case "GAS_SPARK":threshold=200.0;break;
                    case "GAS_DUAL":threshold=400.0;break;
                    case "GAS_COMPRESSION":threshold=1600.0;break;
                    default:break;
                }
                state="IFC_NOX_ENGINE_GUIDELINE";
            }else if("PM_TOTAL".equals(chemical)&&liquid(profile)){
                threshold=50.0;
                state="IFC_TOTAL_PM_ENGINE_GUIDELINE";
            }else if("SOX".equals(chemical)&&liquid(profile)){
                // IFC table gives FUEL SULFUR, not an exhaust SO2 ppm limit.
                threshold=1.5;unit="percent_sulfur_in_fuel";
                state="FUEL_SULFUR_CONTENT_NOT_EXHAUST_SO2";
            }else if("PM1".equals(chemical)||"PM25".equals(chemical)||
                      "PM10".equals(chemical)){
                state="IFC_ENGINE_LIMIT_IS_TOTAL_PM_NOT_SIZE_RESOLVED_AMBIENT_PM";
            }
            reference(row,threshold,unit,state);
            row.put("ifc_comparable",false).put("exceeded",false);
            row.put("ifc_ratio_percent",JSONObject.NULL);
            row.put("ifc_comparison_state",threshold==null?
                "NO_APPLICABLE_NUMERICAL_LIMIT":"REFERENCE_ONLY_MEASUREMENT_BASIS_NOT_MET");
            if(threshold==null)continue;
            matched++;
            // Only explicitly normalized NOx/total PM results are comparable
            // against the stack emissions mass concentration limit.
            boolean numericBasis="mg/Nm3".equals(unit)&&
                row.optString("measured_basis","").equals(
                    "DRY_STACK_GAS_0C_1ATM_15PCT_O2")&&
                ("NOX_AS_NO2".equals(row.optString("species_basis"))||
                 "PM_TOTAL".equals(chemical));
            double measured=row.optDouble("measured_mg_nm3",Double.NaN);
            if(numericBasis&&Double.isFinite(measured)&&measured>=0){
                numeric++;
                double pct=100*measured/threshold;
                row.put("ifc_ratio_percent",pct);
                row.put("ifc_comparable",true);
                row.put("exceeded",measured>threshold);
                row.put("ifc_comparison_state",measured>threshold?
                    "NUMERIC_ABOVE_REFERENCE":"NUMERIC_AT_OR_BELOW_REFERENCE");
                if(measured>threshold)above++;
            }
            if("FUEL_SULFUR_CONTENT_NOT_EXHAUST_SO2".equals(state)){
                double fuelSulfur=row.optDouble("verified_fuel_sulfur_percent",Double.NaN);
                if(Double.isFinite(fuelSulfur)&&fuelSulfur>=0){
                    numeric++;
                    row.put("ifc_ratio_percent",100*fuelSulfur/threshold);
                    row.put("ifc_comparable",true);
                    row.put("exceeded",fuelSulfur>threshold);
                    row.put("ifc_comparison_state",fuelSulfur>threshold?
                        "FUEL_SULFUR_ABOVE_IFC_FUEL_REFERENCE":
                        "FUEL_SULFUR_AT_OR_BELOW_IFC_FUEL_REFERENCE");
                    if(fuelSulfur>threshold)above++;
                }
            }
        }
        frozen.put("ifc_matched_reference_count",matched);
        frozen.put("ifc_numeric_comparison_count",numeric);
        frozen.put("ifc_numeric_above_reference_count",above);
        frozen.put("ifc_assessment_state",numeric>0?
            "NUMERIC_IFC_REFERENCE_COMPARISONS_NOT_FINAL_COMPLIANCE":
            "IFC_REFERENCE_ATTACHED_NO_SAME_BASIS_MEASUREMENT");
        return frozen;
    }
}
