package com.servoc.app.calibration;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** Scientific reference-gas captures cannot borrow R0 from another robot. */
public final class TankReferenceR0GateTest {
    private static final String ID="AA:BB:CC:DD:EE:FF";

    private static JSONObject captured(String source)throws Exception{
        JSONObject all=new JSONObject();
        for(String key:ManualR0Calibration.keys()){
            JSONObject q=new JSONObject();
            q.put("ok",true).put("rs",20.0).put("r0",10.0)
                .put("r0_calibrated",true)
                .put("r0_provenance_complete",true)
                .put("r0_source",source)
                .put("r0_device_id",ID);
            all.put(key,q);
        }
        return all;
    }

    @Test public void allEightDeviceBoundSourcesAreEligibleForReferenceDrafts()throws Exception{
        for(String source:new String[]{"firmware_clean_air_r0","manual_device_r0","automatic_clean_air_field_r0"}){
            assertTrue(source,TankReferenceR0Gate.qualifiedEight(captured(source),ID));
        }
    }

    @Test public void missingControllerOrForeignR0NeverQualifies()throws Exception{
        for(String source:new String[]{"firmware_clean_air_r0","manual_device_r0"}){
            JSONObject channels=captured(source);
            assertFalse(TankReferenceR0Gate.qualifiedEight(channels,""));
            assertFalse(TankReferenceR0Gate.qualifiedEight(channels,"11:22:33:44:55:66"));
            channels.getJSONObject("mq7").remove("r0_device_id");
            assertFalse(TankReferenceR0Gate.qualifiedEight(channels,ID));
        }
    }

    @Test public void oneMissingOrStaleSensorRejectsReference()throws Exception{
        JSONObject channels=captured("firmware_clean_air_r0");
        channels.remove("mq135");
        assertFalse(TankReferenceR0Gate.qualifiedEight(channels,ID));
        channels=captured("manual_device_r0");
        channels.getJSONObject("mq9").put("ok",false);
        assertFalse(TankReferenceR0Gate.qualifiedEight(channels,ID));
        channels=captured("firmware_clean_air_r0");
        channels.getJSONObject("mq2").put("r0_provenance_complete",false);
        assertFalse(TankReferenceR0Gate.qualifiedEight(channels,ID));
    }

    @Test public void syntheticR0OrInvalidRsDoNotProduceScientificReferences()throws Exception{
        JSONObject channels=captured("manual_device_r0");
        channels.getJSONObject("mq4").put("r0_source","firmware_default");
        assertFalse(TankReferenceR0Gate.qualifiedEight(channels,ID));
        channels=captured("manual_device_r0");
        channels.getJSONObject("mq8").put("rs",0.0);
        assertFalse(TankReferenceR0Gate.qualifiedEight(channels,ID));
    }
}
