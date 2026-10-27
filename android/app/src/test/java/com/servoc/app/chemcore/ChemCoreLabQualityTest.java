package com.servoc.app.chemcore;

import android.content.SharedPreferences;
import com.servoc.app.calibration.ManualR0Calibration;
import org.json.JSONObject;
import org.junit.Test;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.Assert.*;

/** Tank V4 requires real device-specific accepted R0; firmware flags alone are not calibration. */
public class ChemCoreLabQualityTest {
    private static final String[] MQ={"mq2","mq4","mq5","mq8","mq3","mq7","mq9","mq135"};

    private static SharedPreferences memoryPreferences(){
        final Map<String,Object> storage=new HashMap<>();
        return (SharedPreferences)Proxy.newProxyInstance(
                SharedPreferences.class.getClassLoader(),new Class[]{SharedPreferences.class},(obj,method,args)->{
                    if("getString".equals(method.getName()))return storage.getOrDefault(args[0],args[1]);
                    if("edit".equals(method.getName())){
                        final Map<String,Object> pending=new HashMap<>();
                        return (SharedPreferences.Editor)Proxy.newProxyInstance(
                            SharedPreferences.Editor.class.getClassLoader(),new Class[]{SharedPreferences.Editor.class},(e,m,a)->{
                                if("putString".equals(m.getName())){pending.put((String)a[0],a[1]);return e;}
                                if("commit".equals(m.getName())){storage.putAll(pending);return true;}
                                if("remove".equals(m.getName())){storage.remove(a[0]);return e;}
                                throw new UnsupportedOperationException(m.getName());
                            });
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private static void acceptEight(SharedPreferences p,boolean firmware){
        Map<String,Double> r0=new LinkedHashMap<>();
        for(int i=0;i<MQ.length;i++)r0.put(MQ[i],20.0+i);
        assertTrue(firmware?
                ManualR0Calibration.saveFirmware(p,r0,"AA:BB:CC:DD:EE:FF"):
                ManualR0Calibration.saveAll(p,r0));
    }

    private static JSONObject diagnostic(boolean envOk,boolean referenceFlag)throws Exception{
        JSONObject d=new JSONObject().put("device_id","AA:BB:CC:DD:EE:FF");
        d.put("relay",new JSONObject().put("on",true).put("mask",1));
        d.put("adc",new JSONObject().put("ads1",true).put("ads2",true));
        JSONObject mq=new JSONObject();
        for(String key:MQ)mq.put(key,new JSONObject().put("ok",true).put("r0_calibrated",true)
                .put("r0_loaded",true).put("r0_source","unreviewed_firmware_reference")
                .put("r0_reference_validated",referenceFlag));
        d.put("mq",mq);
        JSONObject env=new JSONObject().put("ok",envOk).put("type","BME280")
                .put("status_code",envOk?"ok":"no_ack")
                .put("address",envOk?"0x76":"").put("chip_id",envOk?"0x60":"");
        if(envOk)env.put("temperature",24.5).put("humidity",42.0).put("pressure",874.2);
        d.put("env",env);return d;
    }

    @Test public void untrustedFirmwareClaimsDoNotUnlockFingerprintsOrPpm()throws Exception{
        try{
            ManualR0Calibration.load(null);
            ChemCoreLabQuality q=ChemCoreLabQuality.evaluate(diagnostic(true,true),0,0);
            assertFalse(q.configurationReady);assertFalse(q.captureReady);
            assertFalse(q.fingerprintNormalizationReady());
            assertEquals(0,q.r0NormalizedChannels);
            assertEquals(0,q.referenceValidatedChannels);
            assertFalse(q.anyReferenceGasCalibrationAvailable());
            assertTrue(q.blockers.toString().contains("r0_normalization_incomplete"));
        }finally{ManualR0Calibration.load(null);}
    }

    @Test public void acceptedCleanAirR0UnlocksFingerprintButNotGasCalibration()throws Exception{
        try{
            SharedPreferences prefs=memoryPreferences();ManualR0Calibration.load(prefs);
            acceptEight(prefs,false);
            ChemCoreLabQuality q=ChemCoreLabQuality.evaluate(diagnostic(true,false),0,0);
            assertTrue(q.configurationReady);assertTrue(q.captureReady);
            assertTrue(q.fingerprintNormalizationReady());
            assertEquals(8,q.r0NormalizedChannels);
            assertEquals(0,q.fieldR0Channels);assertEquals(0,q.referenceValidatedChannels);
            assertFalse(q.anyReferenceGasCalibrationAvailable());
            assertEquals("manual_device_r0",q.toJson().getJSONObject("r0_standard").getString("source"));
            assertFalse(q.toJson().getJSONObject("calibration_gate").getBoolean("field_r0_normalization_ready"));
        }finally{ManualR0Calibration.load(null);}
    }

    @Test public void missingEnvironmentRemainsAdvisoryAfterR0Acceptance()throws Exception{
        try{
            SharedPreferences prefs=memoryPreferences();ManualR0Calibration.load(prefs);
            acceptEight(prefs,false);
            ChemCoreLabQuality q=ChemCoreLabQuality.evaluate(diagnostic(false,false),0,0);
            assertTrue(q.configurationReady);assertTrue(q.captureReady);
            assertFalse(q.environmentReady);
            assertTrue(q.advisories.toString().contains("environmental_compensation_unavailable"));
        }finally{ManualR0Calibration.load(null);}
    }

    @Test public void importedFirmwareR0CannotQualifyDifferentRobot()throws Exception{
        try{
            SharedPreferences prefs=memoryPreferences();ManualR0Calibration.load(prefs);
            acceptEight(prefs,true);
            JSONObject foreign=diagnostic(true,false).put("device_id","11:22:33:44:55:66");
            ChemCoreLabQuality q=ChemCoreLabQuality.evaluate(foreign,0,0);
            assertFalse(q.configurationReady);assertFalse(q.fingerprintNormalizationReady());
            assertEquals(0,q.r0NormalizedChannels);
            assertTrue(q.advisories.toString().contains("esp32_r0_sync_pending"));
            assertEquals(0,q.toJson().getJSONObject("r0_standard")
                    .getJSONObject("per_sensor").length());
            assertEquals("unavailable",q.toJson().getJSONObject("r0_standard").getString("source"));
            ChemCoreLabQuality same=ChemCoreLabQuality.evaluate(diagnostic(true,false),0,0);
            assertTrue(same.configurationReady);assertTrue(same.fingerprintNormalizationReady());
        }finally{ManualR0Calibration.load(null);}
    }

    @Test public void manualR0BoundToOneTankIsRejectedOnAnother()throws Exception{
        try{
            SharedPreferences prefs=memoryPreferences();ManualR0Calibration.load(prefs);
            Map<String,Double> values=new LinkedHashMap<>();
            for(int i=0;i<MQ.length;i++)values.put(MQ[i],30.0+i);
            assertTrue(ManualR0Calibration.saveAllForDevice(prefs,values,"AA:BB:CC:DD:EE:FF"));
            ChemCoreLabQuality matching=ChemCoreLabQuality.evaluate(diagnostic(true,false),0,0);
            assertEquals(8,matching.r0NormalizedChannels);
            assertTrue(matching.configurationReady);
            JSONObject foreign=diagnostic(true,false).put("device_id","11:22:33:44:55:66");
            ChemCoreLabQuality wrong=ChemCoreLabQuality.evaluate(foreign,0,0);
            assertEquals(0,wrong.r0NormalizedChannels);
            assertFalse(wrong.configurationReady);
            JSONObject evidence=wrong.toJson().getJSONObject("r0_standard");
            assertEquals(0,evidence.getJSONObject("per_sensor").length());
            assertEquals("unavailable",evidence.getString("source"));
        }finally{ManualR0Calibration.load(null);}
    }

    @Test public void motionOrMissingAdcAlwaysBlocksCapture()throws Exception{
        try{
            SharedPreferences prefs=memoryPreferences();ManualR0Calibration.load(prefs);
            acceptEight(prefs,false);
            JSONObject diag=diagnostic(true,false);diag.getJSONObject("adc").put("ads2",false);
            ChemCoreLabQuality q=ChemCoreLabQuality.evaluate(diag,20,0);
            assertFalse(q.configurationReady);assertFalse(q.captureReady);
            assertTrue(q.blockers.contains("ads1115_0x49_not_ready"));
            assertTrue(q.blockers.contains("robot_not_stationary"));
        }finally{ManualR0Calibration.load(null);}
    }
}
