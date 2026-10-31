package com.servoc.app.calibration;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Test;
import static org.junit.Assert.*;

/** V4: firmware owns scan timing and collects each ADC cycle once; phone only imports verified R0. */
public final class TankAutoR0SampleProvenanceTest {
    private String read(String path)throws Exception{
        return new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/"+path)),StandardCharsets.UTF_8);
    }
    @Test public void appNeverCountsScreenRefreshesAsMQSamples()throws Exception{
        String ui=read("ui/DeveloperR0Tab.java");
        assertTrue(ui.contains("host.robotApi().calibrationStatus()"));
        assertTrue(ui.contains("FirmwareR0Contract.committedValues(response)"));
        assertFalse(ui.contains("autoSampler.accept("));
        assertFalse(ui.contains("TankAutoR0Math.captureWindow("));
        assertFalse(ui.contains("lastCycles[i]"));
    }
    @Test public void firmwareControls300Plus60AndPhoneDoesNotPowerOffSensorsAfterCapture()throws Exception{
        String ui=read("ui/DeveloperR0Tab.java");
        assertTrue(ui.contains("host.robotApi().mqPower(true)"));
        assertTrue(ui.contains("host.robotApi().calibrationStart(300,60)"));
        assertFalse(ui.contains("host.robotApi().mqPower(false)"));
        String contract=read("calibration/FirmwareR0Contract.java");
        assertTrue(contract.contains("p.counts.get(key)<10"));
        assertTrue(contract.contains("!p.committed"));
        assertTrue(contract.contains("SCHEMA=\"tank-r0-fw-v5/2\""));
        assertTrue(contract.contains("pendingValues(JSONObject response)"));
    }
}
