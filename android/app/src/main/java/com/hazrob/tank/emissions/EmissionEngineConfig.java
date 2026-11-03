package com.hazrob.tank.emissions;

import org.json.JSONObject;

/** Immutable description of ONE engine and the facility for IFC reference selection.
 * Optional fields stay absent/null: zero is never a placeholder for unknown.
 * Supports small generators outside IFC Table 1.1.2 without inventing a limit.
 */
public final class EmissionEngineConfig {
    public static final String SCHEMA="hazrob.tank.emissions.engine.definition/1";
    public static final String LIQUID="LIQUID", GAS="GAS", UNKNOWN="UNKNOWN";
    public static final String SPARK="SPARK", DUAL="DUAL", COMPRESSION="COMPRESSION";
    public static final String UNSPECIFIED="UNSPECIFIED";
    public final String fuel,ignition;
    public final Double boreMm,facilityMwth,hoursPerYear,utilizationPercent;
    public final Double fuelSulfurPercent,oxygenPercent;
    public final boolean verifiedDryStandard;
    private EmissionEngineConfig(String fuel,String ignition,Double bore,
        Double mwth,Double hours,Double utilization,Double sulfur,Double oxygen,
        boolean dry){
        this.fuel=fuel;this.ignition=ignition;this.boreMm=bore;
        facilityMwth=mwth;hoursPerYear=hours;utilizationPercent=utilization;
        fuelSulfurPercent=sulfur;oxygenPercent=oxygen;verifiedDryStandard=dry;
    }
    public static EmissionEngineConfig of(String fuel,String ignition,
        Double bore,Double mwth,Double hours,Double utilization,
        Double sulfur,Double oxygen,boolean dry){
        if(!LIQUID.equals(fuel)&&!GAS.equals(fuel)&&!UNKNOWN.equals(fuel))
            throw new IllegalArgumentException("FUEL_NOT_SUPPORTED");
        if(!SPARK.equals(ignition)&&!DUAL.equals(ignition)&&
            !COMPRESSION.equals(ignition)&&!UNSPECIFIED.equals(ignition))
            throw new IllegalArgumentException("IGNITION_NOT_SUPPORTED");
        if(bore!=null&&(!Double.isFinite(bore)||bore<=0||bore>1500))
            throw new IllegalArgumentException("BORE_MM_INVALID");
        if(mwth!=null&&(!Double.isFinite(mwth)||mwth<=0||mwth>100000))
            throw new IllegalArgumentException("FACILITY_MWTH_INVALID");
        if(hours!=null&&(!Double.isFinite(hours)||hours<0||hours>8784))
            throw new IllegalArgumentException("ANNUAL_HOURS_INVALID");
        if(utilization!=null&&(!Double.isFinite(utilization)||utilization<0||utilization>100))
            throw new IllegalArgumentException("UTILIZATION_INVALID");
        if(sulfur!=null&&(!Double.isFinite(sulfur)||sulfur<0||sulfur>100))
            throw new IllegalArgumentException("FUEL_SULFUR_INVALID");
        if(oxygen!=null&&(!Double.isFinite(oxygen)||oxygen<0||oxygen>=21))
            throw new IllegalArgumentException("OXYGEN_INVALID");
        return new EmissionEngineConfig(fuel,ignition,bore,mwth,hours,utilization,
            sulfur,oxygen,dry);
    }
    public static EmissionEngineConfig empty(){
        return of(UNKNOWN,UNSPECIFIED,null,null,null,null,null,null,false);
    }
    public static EmissionEngineConfig fromLegacyProfile(String p){
        if(p==null)return empty();
        switch(p){
            case "LIQUID_LT400":return of(LIQUID,COMPRESSION,399.0,null,null,null,null,null,false);
            case "LIQUID_GE400":return of(LIQUID,COMPRESSION,400.0,null,null,null,null,null,false);
            case "GAS_SPARK":return of(GAS,SPARK,null,null,null,null,null,null,false);
            case "GAS_DUAL":return of(GAS,DUAL,null,null,null,null,null,null,false);
            case "GAS_COMPRESSION":return of(GAS,COMPRESSION,null,null,null,null,null,null,false);
            default:return empty();
        }
    }
    public String profile(){
        if(LIQUID.equals(fuel))return boreMm==null?
            EmissionIfcComparator.PROFILE_NONE:
            (boreMm<400?"LIQUID_LT400":"LIQUID_GE400");
        if(GAS.equals(fuel)){
            if(SPARK.equals(ignition))return "GAS_SPARK";
            if(DUAL.equals(ignition))return "GAS_DUAL";
            if(COMPRESSION.equals(ignition))return "GAS_COMPRESSION";
        }
        return EmissionIfcComparator.PROFILE_NONE;
    }
    public String applicability(){
        if(facilityMwth!=null&&(facilityMwth<3||facilityMwth>50))
            return "OUTSIDE_IFC_TABLE_1_1_2_FACILITY_CAPACITY";
        if(hoursPerYear!=null&&hoursPerYear<=500)
            return "OUTSIDE_IFC_TABLE_1_1_2_HOURS";
        if(utilizationPercent!=null&&utilizationPercent<=30)
            return "OUTSIDE_IFC_TABLE_1_1_2_UTILIZATION";
        if(facilityMwth==null||hoursPerYear==null||utilizationPercent==null)
            return "IFC_SCOPE_UNDETERMINED_MISSING_FACILITY_DATA";
        return "WITHIN_IFC_TABLE_1_1_2_SCOPE_BY_ENTERED_VALUES";
    }
    public boolean scopeSatisfied(){
        return "WITHIN_IFC_TABLE_1_1_2_SCOPE_BY_ENTERED_VALUES".equals(applicability());
    }
    private static void put(JSONObject dst,String key,Double value)throws org.json.JSONException{
        dst.put(key,value==null?JSONObject.NULL:value);
    }
    public JSONObject toJson()throws org.json.JSONException{
        JSONObject j=new JSONObject();
        j.put("schema",SCHEMA).put("fuel",fuel).put("ignition",ignition);
        put(j,"bore_mm",boreMm);put(j,"facility_mwth",facilityMwth);
        put(j,"hours_per_year",hoursPerYear);
        put(j,"utilization_percent",utilizationPercent);
        put(j,"fuel_sulfur_percent",fuelSulfurPercent);
        put(j,"oxygen_percent",oxygenPercent);
        j.put("verified_dry_standard",verifiedDryStandard);
        j.put("ifc_profile",profile()).put("ifc_applicability",applicability());
        return j;
    }
    private static Double numeric(JSONObject j,String key){
        if(j==null||!j.has(key)||j.isNull(key))return null;
        double x=j.optDouble(key,Double.NaN);return Double.isFinite(x)?x:null;
    }
    public static EmissionEngineConfig fromJson(String raw){
        if(raw==null||raw.trim().isEmpty())return empty();
        try{
            JSONObject j=new JSONObject(raw);
            return of(j.optString("fuel",UNKNOWN),j.optString("ignition",UNSPECIFIED),
                numeric(j,"bore_mm"),numeric(j,"facility_mwth"),
                numeric(j,"hours_per_year"),numeric(j,"utilization_percent"),
                numeric(j,"fuel_sulfur_percent"),numeric(j,"oxygen_percent"),
                j.optBoolean("verified_dry_standard",false));
        }catch(Exception e){return empty();}
    }
}
