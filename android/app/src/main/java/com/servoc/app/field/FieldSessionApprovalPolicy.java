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
        if(terminal.isEmpty()&&payload.isEmpty())return false;
        if(!terminal.isEmpty()&&!"ready".equalsIgnoreCase(terminal))return false;
        if(!payload.isEmpty()&&!"ready".equalsIgnoreCase(payload))return false;
        return "ready".equalsIgnoreCase(terminal)||"ready".equalsIgnoreCase(payload);
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
