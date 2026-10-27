package com.servoc.app.calibration;

import android.content.SharedPreferences;
import org.json.JSONObject;
import org.junit.Test;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.Assert.*;

/** Phone-side R0 samples must reflect verified firmware windows, never synthetic counts. */
public final class TankV4R0SamplePersistenceTest {
    private static SharedPreferences prefs(){
        final Map<String,String> values=new HashMap<>();
        return (SharedPreferences)Proxy.newProxyInstance(SharedPreferences.class.getClassLoader(),
            new Class[]{SharedPreferences.class},(p,m,a)->{
                if("getString".equals(m.getName()))
                    return values.getOrDefault((String)a[0],(String)a[1]);
                if("edit".equals(m.getName())){
                    final Map<String,String> pending=new HashMap<>();
                    final java.util.Set<String> removals=new java.util.HashSet<>();
                    return (SharedPreferences.Editor)Proxy.newProxyInstance(
                        SharedPreferences.Editor.class.getClassLoader(),
                        new Class[]{SharedPreferences.Editor.class},(e,method,args)->{
                            if("putString".equals(method.getName())){
                                pending.put((String)args[0],(String)args[1]);removals.remove(args[0]);return e;
                            }
                            if("remove".equals(method.getName())){
                                removals.add((String)args[0]);pending.remove(args[0]);return e;
                            }
                            if("commit".equals(method.getName())){
                                for(String key:removals)values.remove(key);
                                values.putAll(pending);return true;
                            }
                            throw new UnsupportedOperationException(method.getName());
                        });
                }
                throw new UnsupportedOperationException(m.getName());
            });
    }

    private static Map<String,Double> baselines(){
        Map<String,Double> m=new LinkedHashMap<>();int i=0;
        for(String key:ManualR0Calibration.keys())m.put(key,20.0+i++);
        return m;
    }
    private static Map<String,Integer> counts(int n){
        Map<String,Integer> m=new LinkedHashMap<>();
        for(String key:ManualR0Calibration.keys())m.put(key,n);
        return m;
    }

    @Test public void validatedSamplesSurviveReloadAndReachEvidence()throws Exception{
        SharedPreferences p=prefs();
        try{
            ManualR0Calibration.load(p);
            assertTrue(ManualR0Calibration.saveFirmware(p,baselines(),"AA:BB:CC:DD:EE:FF",counts(26)));
            ManualR0Calibration.load(p);
            assertEquals(26,ManualR0Calibration.getSamples("mq7"));
            assertEquals("AA:BB:CC:DD:EE:FF",ManualR0Calibration.getDeviceId("mq7"));
            JSONObject q=new JSONObject().put("rs",41.0).put("status","VALID");
            ManualR0Calibration.apply("mq7",q);
            assertEquals(26,q.getInt("r0_samples"));
            assertTrue(q.getBoolean("r0_provenance_complete"));
        }finally{ManualR0Calibration.load(null);}
    }

    @Test public void oneSensorBelowTenRejectsEntireFirmwareRevision(){
        SharedPreferences p=prefs();
        try{
            ManualR0Calibration.load(p);
            Map<String,Integer> invalid=counts(11);invalid.put("mq135",9);
            assertFalse(ManualR0Calibration.saveFirmware(p,baselines(),"AA:BB:CC:DD:EE:FF",invalid));
            assertTrue(Double.isNaN(ManualR0Calibration.get("mq2")));
            assertEquals(0,ManualR0Calibration.getSamples("mq135"));
        }finally{ManualR0Calibration.load(null);}
    }

    @Test public void manualOverrideCannotInheritObsoleteFirmwareSampleCounts(){
        SharedPreferences p=prefs();
        try{
            ManualR0Calibration.load(p);
            assertTrue(ManualR0Calibration.saveFirmware(p,baselines(),"AA:BB:CC:DD:EE:FF",counts(40)));
            assertTrue(ManualR0Calibration.saveForDevice(p,"mq7",55.0,"AA:BB:CC:DD:EE:FF"));
            ManualR0Calibration.load(p);
            assertEquals("manual_device_r0",ManualR0Calibration.getSource("mq7"));
            assertEquals(0,ManualR0Calibration.getSamples("mq7"));
            assertEquals(40,ManualR0Calibration.getSamples("mq9"));
        }finally{ManualR0Calibration.load(null);}
    }
}
