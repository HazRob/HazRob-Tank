package com.servoc.app.calibration;

import static org.junit.Assert.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * V8 F02 — Android runtime contract test for the single-window clean-air R0
 * protocol (180 seconds warmup + 60 seconds sampling). No actual Tank gas
 * reference, calibration, or NVS write occurs in this fixture.
 */
@RunWith(AndroidJUnit4.class)
public final class V8QuickR0FirmwareContractInstrumentedTest {
    private static JSONObject fixture(String state,boolean pending,boolean committed,
                                      int samples,int warmMs,int durationMs)throws Exception {
        JSONObject channels=new JSONObject();
        for(String name:ManualR0Calibration.keys())
            channels.put(name,new JSONObject().put("r0",83.25).put("samples",samples));
        return new JSONObject()
            .put("schema",FirmwareR0Contract.V6_QUICK_SCHEMA)
            .put("device_id","TEST-ESP32-R0-NO-HARDWARE")
            .put("state",state)
            .put("pending",pending)
            .put("committed",committed)
            .put("warmup_ms",warmMs)
            .put("duration_ms",durationMs)
            .put("elapsed_ms",state.equals("warmup")?15000:20000)
            .put("window_count",1)
            .put("window_index",1)
            .put("r0_definition","DEVICE_CLEAN_AIR_RS_MEAN")
            .put("protocol","V6_180S_WARM_60S_SINGLE_RS_AIR")
            .put("error","")
            .put("channels",channels);
    }
    private static void invalidSchema(JSONObject x)throws Exception {
        try{FirmwareR0Contract.progress(x);fail("quick calibration schema must fail closed");}
        catch(IllegalArgumentException expected){}
    }
    private static void invalidValues(JSONObject x,boolean pending)throws Exception {
        try{
            if(pending)FirmwareR0Contract.pendingValues(x);
            else FirmwareR0Contract.committedValues(x);
            fail("eight valid source channels are required");
        }catch(IllegalStateException expected){}
    }
    @Test public void warmupAndOneMinuteSampleAreNotCommitOrPending()throws Exception {
        JSONObject warm=fixture("warmup",false,false,0,180000,180000);
        FirmwareR0Contract.Progress a=FirmwareR0Contract.progress(warm);
        assertTrue(a.active());
        assertEquals(180000,a.warmupMs);
        assertEquals(180000,a.durationMs);
        assertEquals(1,a.windowCount);
        assertFalse(a.committed);
        assertFalse(a.pending);
        JSONObject measuring=fixture("sampling",false,false,8,180000,60000);
        FirmwareR0Contract.Progress b=FirmwareR0Contract.progress(measuring);
        assertTrue(b.active());
        assertEquals(60000,b.durationMs);
        invalidValues(measuring,true);
        invalidValues(measuring,false);
    }
    @Test public void eightChannelPendingRequiresExplicitFirmwareAccept()throws Exception {
        JSONObject pending=fixture("pending",true,false,10,180000,60000);
        assertEquals(8,FirmwareR0Contract.pendingValues(pending).size());
        invalidValues(pending,false);
        pending.getJSONObject("channels").getJSONObject("mq8").put("samples",9);
        invalidValues(pending,true);
    }
    @Test public void acceptedNvsReadbackRequiresAllEightChannels()throws Exception {
        JSONObject ready=fixture("ready",false,true,10,180000,60000);
        assertEquals(8,FirmwareR0Contract.committedValues(ready).size());
        invalidValues(ready,true);
        ready.getJSONObject("channels").getJSONObject("mq135").put("r0",0.0);
        invalidValues(ready,false);
        ready.getJSONObject("channels").remove("mq135");
        invalidSchema(ready);
    }
    @Test public void rejectDurationDefinitionWindowsAndForgedFlags()throws Exception {
        JSONObject bad=fixture("pending",true,false,11,180001,60000);
        invalidSchema(bad);
        bad=fixture("warmup",false,false,11,180000,179999);
        invalidSchema(bad);
        bad=fixture("pending",true,false,11,180000,60001);
        invalidSchema(bad);
        bad=fixture("pending",true,false,11,180000,60000);
        bad.put("window_count",3);invalidSchema(bad);
        bad.put("window_count",1).put("r0_definition","LEGACY_MQ_FACTOR_SCALED_R0_NOT_AIR_RS");
        invalidSchema(bad);
        bad=fixture("pending",false,true,11,180000,60000);
        invalidSchema(bad);
        bad=fixture("ready",false,false,11,180000,60000);
        invalidValues(bad,false);
    }
}
