package com.servoc.app.calibration;

import org.json.JSONObject;

/**
 * Only eight traceable measurements recorded by the same Tank ESP32 may enter
 * the developer reference pipeline. This is an evidence/provenance gate,
 * NOT a certificate of reference-gas accuracy or a ppm validation.
 */
public final class TankReferenceR0Gate {
    private TankReferenceR0Gate(){}

    public static boolean qualifiedEight(JSONObject channels,String controllerId){
        if(channels==null||controllerId==null||controllerId.trim().isEmpty())return false;
        for(String key:ManualR0Calibration.keys()){
            JSONObject q=channels.optJSONObject(key);
            if(q==null||!q.optBoolean("ok",q.optBoolean("valid",false)))return false;
            double rs=q.optDouble("rs",Double.NaN),r0=q.optDouble("r0",Double.NaN);
            if(!Double.isFinite(rs)||rs<=0||!Double.isFinite(r0)||r0<=0)return false;
            if(!q.optBoolean("r0_calibrated",false)
                    ||!q.optBoolean("r0_provenance_complete",false))return false;
            String source=q.optString("r0_source","");
            if(!"firmware_clean_air_r0".equals(source)
                    &&!"manual_device_r0".equals(source)
                    &&!"automatic_clean_air_field_r0".equals(source))return false;
            // A legacy unbound manual R0 can be studied as exploratory evidence,
            // never silently associated with another physical sensor array.
            if(!controllerId.equals(q.optString("r0_device_id","")))return false;
            // Same evidence contract as the V6 instantaneous clean-air
            // screen: source labels never substitute for measured windows.
            if("firmware_clean_air_r0".equals(source)||
                    "automatic_clean_air_field_r0".equals(source)){
                if(!"DEVICE_CLEAN_AIR_RS_MEAN".equals(
                        q.optString("r0_definition","")))return false;
                int windows=q.optInt("r0_window_count",0);
                int samples=q.optInt("r0_samples",0);
                if(!((windows==1&&samples>=10)||
                        (windows==3&&samples>=30)))return false;
            }
            if("manual_device_r0".equals(source)&&
                    !q.optBoolean("r0_clean_air_reference_independently_verified",false))
                return false;
        }
        return true;
    }
}
