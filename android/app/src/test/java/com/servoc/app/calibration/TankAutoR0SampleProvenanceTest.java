package com.servoc.app.calibration;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Test;
import static org.junit.Assert.*;

/** The ESP32 V6, not phone view redraws, owns R0 capture. */
public final class TankAutoR0SampleProvenanceTest {
    private String read(String p)throws Exception{
        return new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/"+p)),
            StandardCharsets.UTF_8);
    }
    @Test public void appDoesNotTreatPollsAsIndependentMqSamples()throws Exception{
        String ui=read("ui/OperatorR0CalibrationScreen.java");
        assertTrue(ui.contains("host.robotApi().calibrationStatus()"));
        assertTrue(ui.contains("FirmwareR0Contract.committedValues(verified)"));
        assertFalse(ui.contains("autoSampler.accept("));
        assertFalse(ui.contains("TankAutoR0Math.captureWindow("));
    }
    @Test public void firmwareControlsExact180Plus60AndR0RemainsStaged()throws Exception{
        String ui=read("ui/OperatorR0CalibrationScreen.java");
        assertTrue(ui.contains("host.robotApi().mqPower(true)"));
        assertTrue(ui.contains("host.robotApi().calibrationStart(180,60)"));
        assertFalse(ui.contains("host.robotApi().mqPower(false)"));
        String contract=read("calibration/FirmwareR0Contract.java");
        assertTrue(contract.contains("p.counts.get(key)<(p.windowCount==3?30:10)"));
        assertTrue(contract.contains("pendingValues(JSONObject response)"));
        assertTrue(contract.contains("V6_QUICK_SCHEMA"));
    }
}
