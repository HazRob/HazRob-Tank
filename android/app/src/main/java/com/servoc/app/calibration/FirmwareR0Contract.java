package com.servoc.app.calibration;

import org.json.JSONObject;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Validates firmware-committed V4 R0 readings; warmup/sample previews are not accepted. */
public final class FirmwareR0Contract {
    public static final String SCHEMA="tank-r0-fw-v4/1";
    private FirmwareR0Contract(){}

    public static final class Progress {
        public final String state,error;
        public final int elapsedMs,durationMs,warmupMs;
        public final boolean committed;
        public final Map<String,Integer> counts;
        private Progress(String state,String error,int elapsedMs,int durationMs,int warmupMs,
                         boolean committed,Map<String,Integer> counts){
            this.state=state;this.error=error;this.elapsedMs=elapsedMs;this.durationMs=durationMs;
            this.warmupMs=warmupMs;this.committed=committed;
            this.counts=Collections.unmodifiableMap(counts);
        }
        public boolean active(){return "warmup".equals(state)||"sampling".equals(state);}
    }

    public static Progress progress(JSONObject response){
        if(response==null||!SCHEMA.equals(response.optString("schema","")))
            throw new IllegalArgumentException("Unsupported firmware R0 API schema");
        String state=response.optString("state","");
        if(!("idle".equals(state)||"warmup".equals(state)||"sampling".equals(state)
             ||"ready".equals(state)||"error".equals(state)||"cancelled".equals(state)))
            throw new IllegalArgumentException("Unknown R0 state");
        Map<String,Integer> counts=new LinkedHashMap<>();
        JSONObject channels=response.optJSONObject("channels");
        if(channels==null)throw new IllegalArgumentException("Missing R0 channels");
        for(String key:ManualR0Calibration.keys()){
            JSONObject q=channels.optJSONObject(key);
            if(q==null)throw new IllegalArgumentException("Missing R0 channel: "+key);
            int n=q.optInt("samples",-1);
            if(n<0)throw new IllegalArgumentException("Invalid R0 count: "+key);
            counts.put(key,n);
        }
        boolean committed=response.optBoolean("committed",false);
        if(committed&&!"ready".equals(state))
            throw new IllegalArgumentException("R0 committed flag contradicts state");
        return new Progress(state,response.optString("error",""),
                Math.max(0,response.optInt("elapsed_ms",0)),Math.max(0,response.optInt("duration_ms",0)),
                Math.max(0,response.optInt("warmup_ms",0)),committed,counts);
    }

    /**
     * Only returns R0 values after the ESP32 has written and read back its complete NVS
     * record. All eight sensor channels must pass the >=10 fresh-sample gate.
     */
    public static Map<String,Double> committedValues(JSONObject response){
        Progress p=progress(response);
        if(!"ready".equals(p.state)||!p.committed||!p.error.isEmpty())
            throw new IllegalStateException("No verified firmware R0 commit");
        JSONObject channels=response.optJSONObject("channels");
        Map<String,Double> values=new LinkedHashMap<>();
        for(String key:ManualR0Calibration.keys()){
            JSONObject q=channels.optJSONObject(key);
            double value=q.optDouble("r0",Double.NaN);
            if(p.counts.get(key)<10||!Double.isFinite(value)||value<=0)
                throw new IllegalStateException("Invalid firmware R0: "+key);
            values.put(key,value);
        }
        return Collections.unmodifiableMap(values);
    }
}
