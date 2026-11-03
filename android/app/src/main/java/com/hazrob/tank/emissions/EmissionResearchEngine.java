package com.hazrob.tank.emissions;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Map;

/**
 * Independent EMISSIONS feature boundary.
 * This is deliberately a completed measurement of MQ source signals, NOT a
 * fabricated quantitative exhaust result. The owner will choose the future
 * inference engine and source-validated IFC reference mapping separately.
 * The gas ChemCore Next and Stable catalogs are not altered or coupled here.
 */
public final class EmissionResearchEngine {
    public static final String VERSION="emissions-research-capture/1";
    public static final String INFERENCE="PENDING_ENGINE_SELECTION";
    private EmissionResearchEngine(){}
    private static boolean valid(Double v){return v!=null&&Double.isFinite(v)&&v>0;}
    private static JSONObject pollutant(String id,String label,String sensor,
                                        Map<String,Double> avg,Map<String,Integer> counts){
        JSONObject row=new JSONObject();
        try{
            row.put("pollutant",id);
            row.put("label",label);
            row.put("value_ppm",JSONObject.NULL);
            row.put("limit",JSONObject.NULL);
            row.put("limit_unit","-");
            row.put("source","IFC EHS: specific applicable reference not yet selected");
            row.put("ifc_comparable",false);
            row.put("exceeded",false);
            row.put("status","NOT_CALIBRATED");
            row.put("unit","ppm");
            // Historical source responses are collected as RESEARCH INDICATORS ONLY.
            // Never interpret MQ cross-sensitivity as a SOx/NOx/CO2 measurement.
            if(sensor!=null&&valid(avg.get(sensor))){
                row.put("mq_sensor",sensor);
                row.put("mq_average_rs",avg.get(sensor));
                row.put("mq_samples",counts.containsKey(sensor)?counts.get(sensor):0);
                row.put("status","MQ_SIGNAL_ONLY");
            }
        }catch(Exception e){throw new IllegalStateException(e);}
        return row;
    }
    public static JSONObject complete(long engineId,String engineName,long startMs,
                                      long elapsedMs,boolean includeParticles,int frames,
                                      Map<String,Double> averages,Map<String,Integer> counts){
        if(engineId<=0||engineName==null||engineName.trim().isEmpty())
            throw new IllegalArgumentException("SELECT_ENGINE_FIRST");
        JSONObject result=new JSONObject();
        JSONArray entries=new JSONArray();
        entries.put(pollutant("SOX","SOx / SO₂",null,averages,counts));
        entries.put(pollutant("NOX","NOx / NO₂",null,averages,counts));
        entries.put(pollutant("CH4","الميثان CH₄","MQ4",averages,counts));
        entries.put(pollutant("CO2","ثاني أكسيد الكربون CO₂",null,averages,counts));
        entries.put(pollutant("SMOKE","الدخان Smoke","MQ2",averages,counts));
        if(includeParticles){
            entries.put(pollutant("PM1","PM₁",null,averages,counts));
            entries.put(pollutant("PM25","PM₂.₅",null,averages,counts));
            entries.put(pollutant("PM10","PM₁₀",null,averages,counts));
        }
        try{
            result.put("schema",VERSION).put("mode",INFERENCE)
                .put("engine_id",engineId).put("engine_name",engineName)
                .put("measured_at_ms",System.currentTimeMillis())
                .put("started_at_ms",startMs).put("duration_ms",elapsedMs)
                .put("frames",frames).put("include_particles",includeParticles)
                .put("source_state","UNQUALIFIED_MQ_SCREENING_ONLY")
                .put("pollutants",entries);
            JSONObject raw=new JSONObject();
            for(Map.Entry<String,Double> e:averages.entrySet())
                if(valid(e.getValue()))raw.put(e.getKey(),e.getValue());
            result.put("mq_average_rs",raw);
            result.put("ifc_assessment_state","NOT_EVALUABLE_PENDING_SENSORS_AND_SOURCE_REFERENCES");
        }catch(Exception e){throw new IllegalStateException(e);}
        return result;
    }
}
