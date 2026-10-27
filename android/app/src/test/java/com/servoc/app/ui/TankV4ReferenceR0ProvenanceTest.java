package com.servoc.app.ui;

import com.servoc.app.calibration.ManualR0Calibration;
import org.json.JSONObject;
import org.junit.Test;
import java.lang.reflect.Method;
import static org.junit.Assert.*;

/**
 * Laboratory draft acceptance must use a complete, immutable eight-MQ reading from
 * the same robot. Neither controller-bound manual nor firmware R0 can cross devices.
 * Actual ESP32 calibration and model accuracy require independent bench evidence.
 */
public final class TankV4ReferenceR0ProvenanceTest {
    private static JSONObject capture(String source,String deviceId)throws Exception{
        JSONObject channels=new JSONObject();
        for(String key:ManualR0Calibration.keys()){
            JSONObject q=new JSONObject();
            q.put("ok",true);q.put("valid",true);
            q.put("rs",20.0);q.put("r0",10.0);q.put("ratio",2.0);
            q.put("r0_calibrated",true);
            q.put("r0_provenance_complete",true);
            q.put("r0_source",source);q.put("r0_device_id",deviceId);
            channels.put(key,q);
        }
        return channels;
    }

    private static boolean qualified(JSONObject channels,String controller)throws Exception{
        Method method=DeveloperExperimentsTab.class.getDeclaredMethod(
                "qualifiedReferenceChannels",JSONObject.class,String.class);
        method.setAccessible(true);
        return (Boolean)method.invoke(null,channels,controller);
    }

    @Test public void sameControllerAcceptedForRecordedFirmwareCalibration()throws Exception{
        assertTrue(qualified(capture("firmware_clean_air_r0","AA:BB:CC:DD:EE:FF"),
                "AA:BB:CC:DD:EE:FF"));
    }

    @Test public void foreignControllerRejectedForFirmwareAndManualCalibration()throws Exception{
        for(String source:new String[]{"firmware_clean_air_r0","manual_device_r0"}){
            assertFalse(qualified(capture(source,"11:22:33:44:55:66"),
                    "AA:BB:CC:DD:EE:FF"));
        }
    }

    @Test public void firmwareRevisionWithoutControllerCannotBeApproved()throws Exception{
        assertFalse(qualified(capture("firmware_clean_air_r0",""),
                "AA:BB:CC:DD:EE:FF"));
    }

    @Test public void missingSensorOrProvenanceBlocksScientificReference()throws Exception{
        JSONObject x=capture("manual_device_r0","AA:BB:CC:DD:EE:FF");
        x.remove("mq135");assertFalse(qualified(x,"AA:BB:CC:DD:EE:FF"));
        x=capture("manual_device_r0","AA:BB:CC:DD:EE:FF");
        x.getJSONObject("mq7").put("r0_provenance_complete",false);
        assertFalse(qualified(x,"AA:BB:CC:DD:EE:FF"));
        x.getJSONObject("mq7").put("r0_provenance_complete",true);
        x.getJSONObject("mq7").put("rs",0);
        assertFalse(qualified(x,"AA:BB:CC:DD:EE:FF"));
    }
}
