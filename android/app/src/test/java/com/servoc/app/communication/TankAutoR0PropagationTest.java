package com.servoc.app.communication;

import android.content.SharedPreferences;
import com.servoc.app.calibration.AcceptedR0Normalizer;
import com.servoc.app.calibration.ManualR0Calibration;
import com.servoc.app.measurement.CanonicalMeasurementNormalizer;
import org.json.JSONObject;
import org.junit.Test;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.Assert.*;

/** Source-to-ChemCore contract: saved auto R0 wins over firmware values, even after restart. */
public final class TankAutoR0PropagationTest {
    private static SharedPreferences memoryPrefs(){
        final Map<String,String> stored=new HashMap<>();
        final SharedPreferences.Editor editor=(SharedPreferences.Editor)Proxy.newProxyInstance(
            SharedPreferences.Editor.class.getClassLoader(),new Class[]{SharedPreferences.Editor.class},
            (p,m,args)->{
                if("putString".equals(m.getName())){stored.put((String)args[0],(String)args[1]);return p;}
                if("remove".equals(m.getName())){stored.remove((String)args[0]);return p;}
                if("clear".equals(m.getName())){stored.clear();return p;}
                if("commit".equals(m.getName()))return true;
                if("apply".equals(m.getName()))return null;
                throw new UnsupportedOperationException(m.getName());
            });
        return (SharedPreferences)Proxy.newProxyInstance(
            SharedPreferences.class.getClassLoader(),new Class[]{SharedPreferences.class},
            (p,m,args)->{
                if("getString".equals(m.getName()))return stored.getOrDefault((String)args[0],(String)args[1]);
                if("contains".equals(m.getName()))return stored.containsKey(args[0]);
                if("edit".equals(m.getName()))return editor;
                throw new UnsupportedOperationException(m.getName());
            });
    }

    @Test public void savedAutomaticR0OverridesFirmwareAndSurvivesReload()throws Exception{
        SharedPreferences prefs=memoryPrefs();
        ManualR0Calibration.load(prefs);
        Map<String,Double> fresh=new LinkedHashMap<>();
        fresh.put("mq2",50.0);
        assertTrue(ManualR0Calibration.saveAutomatic(prefs,fresh));
        ManualR0Calibration.load(prefs);  // new Activity / reopened app
        JSONObject raw=new JSONObject(
            "{\"api\":4,\"adc\":{\"ads1\":true,\"ads2\":true},"+
            "\"relay\":{\"on\":true,\"mask\":1},\"env\":{},\"mq\":{"+
            "\"mq2\":{\"ok\":true,\"powered\":true,\"ads\":0,\"ch\":0,\"raw\":1000,\"adc\":1.0,\"ao\":1.0,\"rs\":100,"+
            "\"r0\":5,\"ratio\":20,\"r0_calibrated\":true,\"r0_source\":\"firmware_baseline\",\"age_us\":1000,\"scan_cycle\":6,\"sample_us\":12345}}}");
        JSONObject normalized=RobotProtocolV4.normalize(new JSONObject("{\"api\":4,\"adc\":{\"ads1\":true,\"ads2\":true}}"),raw,0,0);
        JSONObject mq=normalized.getJSONObject("mq").getJSONObject("mq2");
        assertEquals("automatic_clean_air_field_r0",mq.getString("r0_source"));
        assertEquals("USER_AUTO_FIELD_BASELINE",mq.getString("calibration_status"));
        assertEquals(50.0,mq.getDouble("r0"),1e-8);
        assertEquals(2.0,mq.getDouble("ratio"),1e-8);
        assertFalse(mq.getBoolean("r0_reference_validated"));
        assertFalse(mq.getBoolean("manual_calibration_confirmed"));
        assertTrue(mq.getBoolean("r0_provenance_complete"));
        JSONObject canonical=CanonicalMeasurementNormalizer.enrich(normalized,"DIRECT");
        JSONObject saved=canonical.getJSONObject("mq").getJSONObject("mq2");
        assertEquals(2.0,saved.getDouble("ratio"),1e-8);
        assertEquals(2.0,AcceptedR0Normalizer.ratio("mq2",saved),1e-8);
        assertEquals(2.0,AcceptedR0Normalizer.recordedRatio("mq2",new JSONObject(saved.toString())),1e-8);
        assertTrue(AcceptedR0Normalizer.usesRecordedDeviceR0("mq2",saved));
        // Change current R0 later. Old recorded ratio must remain unchanged.
        Map<String,Double> newer=new LinkedHashMap<>();newer.put("mq2",100.0);
        assertTrue(ManualR0Calibration.saveAutomatic(prefs,newer));
        assertEquals(1.0,AcceptedR0Normalizer.ratio("mq2",saved),1e-8);
        assertEquals(2.0,AcceptedR0Normalizer.recordedRatio("mq2",saved),1e-8);
    }
}
