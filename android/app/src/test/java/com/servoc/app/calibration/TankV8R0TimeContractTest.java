package com.servoc.app.calibration;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * V7 incident regression: V6 quick calibration is exactly 180 + 60 seconds.
 * The ESP32 can correctly report V5 idle schema before its FIRST V6 run,
 * then transition to tank-r0-fw-v6/2 after start; don't reject that transition.
 */
public final class TankV8R0TimeContractTest {
    private static JSONObject state(String schema,String stage,int warmup,int duration)
            throws Exception{
        JSONObject r=new JSONObject()
            .put("schema",schema)
            .put("state",stage)
            .put("committed","ready".equals(stage))
            .put("pending","pending".equals(stage))
            .put("device_id","AA:BB:CC:DD:EE:FF")
            .put("window_count",1)
            .put("r0_definition",FirmwareR0Contract.V6_QUICK_SCHEMA.equals(schema)?
                "DEVICE_CLEAN_AIR_RS_MEAN":"MQ_FACTOR_SCALED_R0_NOT_AIR_RS")
            .put("warmup_ms",warmup).put("duration_ms",duration)
            .put("elapsed_ms",0).put("error","");
        JSONObject channels=new JSONObject();
        for(String key:ManualR0Calibration.keys())
            channels.put(key,new JSONObject().put("samples",20).put("r0",42.5));
        return r.put("channels",channels);
    }

    @Test public void uncalibratedFirmwareV6MayReportV5SchemaWhenIdle()throws Exception{
        JSONObject idle=state(FirmwareR0Contract.SCHEMA,"idle",180000,60000);
        assertEquals("idle",FirmwareR0Contract.progress(idle).state);
        assertFalse(FirmwareR0Contract.progress(idle).committed);
    }

    @Test public void v6QuickMatchesEachPhaseExactly()throws Exception{
        for(String stage:new String[]{"warmup","sampling","pending","ready"}){
            int duration="warmup".equals(stage)?180000:60000;
            JSONObject json=state(FirmwareR0Contract.V6_QUICK_SCHEMA,
                stage,180000,duration);
            FirmwareR0Contract.Progress got=FirmwareR0Contract.progress(json);
            assertEquals(stage,got.state);
            assertEquals(1,got.windowCount);
            assertEquals(180000,got.warmupMs);
            assertEquals(duration,got.durationMs);
        }
        JSONObject pending=state(FirmwareR0Contract.V6_QUICK_SCHEMA,
            "pending",180000,60000);
        assertEquals(8,FirmwareR0Contract.pendingValues(pending).size());
        JSONObject ready=state(FirmwareR0Contract.V6_QUICK_SCHEMA,
            "ready",180000,60000);
        assertEquals(8,FirmwareR0Contract.committedValues(ready).size());
    }

    @Test public void stale300SecondTimingCannotPassAsV6Quick()throws Exception{
        JSONObject incompatible=state(FirmwareR0Contract.V6_QUICK_SCHEMA,
            "warmup",300000,300000);
        try{
            FirmwareR0Contract.progress(incompatible);
            fail("Never accept 300+60 as V6 quick 180+60");
        }catch(IllegalArgumentException ex){
            assertTrue(ex.getMessage().contains("warmup_ms=300000"));
            assertTrue(ex.getMessage().contains("180000"));
        }
        incompatible=state(FirmwareR0Contract.V6_QUICK_SCHEMA,
            "sampling",180000,180000);
        try{
            FirmwareR0Contract.progress(incompatible);
            fail("Never accept wrong 180-second sampling stage");
        }catch(IllegalArgumentException ex){
            assertTrue(ex.getMessage().contains("duration_ms=180000"));
        }
    }

    @Test public void unknownSchemaIsExplainedNotSilentlyRetried()throws Exception{
        JSONObject unknown=state("tank-r0-fw-v4/unknown","warmup",300000,60000);
        try{
            FirmwareR0Contract.progress(unknown);
            fail();
        }catch(IllegalArgumentException ex){
            assertTrue(ex.getMessage().contains("v4/unknown"));
        }
    }

    @Test public void uiUsesOnlySettingsControllerWithoutOldDeveloperClass()throws Exception{
        String screen=new String(java.nio.file.Files.readAllBytes(
                java.nio.file.Paths.get("src/main/java/com/servoc/app/ui/OperatorR0CalibrationScreen.java")),
            java.nio.charset.StandardCharsets.UTF_8);
        String settings=new String(java.nio.file.Files.readAllBytes(
                java.nio.file.Paths.get("src/main/java/com/servoc/app/ui/SettingsTab.java")),
            java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(screen.contains("calibrationStart(180,60)"));
        assertTrue(screen.contains("generation!=token"));
        assertTrue(screen.contains("FirmwareR0Contract.committedValues(verified)"));
        assertTrue(settings.contains("new OperatorR0CalibrationScreen("));
        assertFalse(settings.contains("new DeveloperR0Tab("));
    }
}
