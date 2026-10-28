package com.servoc.app.field;

import org.json.JSONObject;

/** Prevent incomplete/aborted telemetry captures being promoted to user approved reports. */
public final class FieldSessionApprovalPolicy {
    private FieldSessionApprovalPolicy(){}

    /**
     * Complete must be a real terminal READY/ready result.
     * Historical rows may have only payload; any explicit disagreement fails closed.
     */
    public static boolean completedSuccessfully(String analyticsJson,String payloadJson){
        String terminal=terminalState(analyticsJson);
        String payload=payloadState(payloadJson);
        // V5 documents a 60 s Android-controlled telemetry capture, not a
        // firmware /measure response. The original payload may legitimately be
        // "idle"; evidence is in monotonic duration, sample count and the
        // separate field_60s_capture marker. Do not falsify firmware state.
        if(isDocumentedFieldCapture(analyticsJson))
            return "ready".equalsIgnoreCase(terminal)&&fieldDurationAndSamplesValid(analyticsJson);
        if(terminal.isEmpty()&&payload.isEmpty())return false;
        if(!terminal.isEmpty()&&!"ready".equalsIgnoreCase(terminal))return false;
        if(!payload.isEmpty()&&!"ready".equalsIgnoreCase(payload))return false;
        return "ready".equalsIgnoreCase(terminal)||"ready".equalsIgnoreCase(payload);
    }
    private static boolean isDocumentedFieldCapture(String json){
        try{return new JSONObject(json).optBoolean("field_60s_capture",false);}
        catch(Exception e){return false;}
    }
    private static boolean fieldDurationAndSamplesValid(String json){
        try{
            JSONObject a=new JSONObject(json);
            long duration=a.optLong("duration_monotonic_ms",0);
            long count=a.optLong("sample_count",0);
            return duration>=60000L && count>=10 &&
                "UTC_EPOCH_MS_PLUS_ANDROID_ELAPSED_REALTIME".equals(
                    a.optString("clock_contract",""));
        }catch(Exception e){return false;}
    }

    private static String terminalState(String json){
        if(json==null||json.trim().isEmpty())return "";
        try{return new JSONObject(json).optString("terminal_state","").trim();}
        catch(Exception e){return "invalid";} // never approve malformed metadata
    }
    private static String payloadState(String json){
        if(json==null||json.trim().isEmpty())return "";
        try{
            JSONObject m=new JSONObject(json).optJSONObject("measurement");
            return m==null?"":m.optString("state","").trim();
        }catch(Exception e){return "invalid";}
    }
}
