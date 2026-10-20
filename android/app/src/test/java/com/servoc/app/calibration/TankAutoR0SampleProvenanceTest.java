package com.servoc.app.calibration;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Test;
import static org.junit.Assert.*;

/** The firmware streams scan_cycle; do not mistake repeated Android refreshes for fresh Rs. */
public final class TankAutoR0SampleProvenanceTest {
    @Test public void eachAcceptedR0ReadingRequiresMonotonicallyNewMqScan() throws Exception {
        String source=new String(Files.readAllBytes(Paths.get(
            "src/main/java/com/servoc/app/ui/DeveloperR0Tab.java")),StandardCharsets.UTF_8);
        assertTrue(source.contains("long cycle=sample.optLong(\"scan_cycle\",-1L);"));
        assertTrue(source.contains("if(cycle<=0||cycle<=lastCycles[i])continue;"));
        assertTrue(source.contains("lastCycles[i]=cycle;"));
        assertTrue(source.indexOf("lastCycles[i]=cycle;")<
            source.indexOf("autoSampler.accept(i,window,rs);"));
    }
    @Test public void approvedTwentyMinuteCycleHasNoExtraWarmupOrPowerOff() throws Exception {
        String source=new String(Files.readAllBytes(Paths.get(
            "src/main/java/com/servoc/app/ui/DeveloperR0Tab.java")),StandardCharsets.UTF_8);
        assertTrue(source.contains("host.robotApi().mqPower(true)"));
        assertTrue(source.contains("TankAutoR0Math.TOTAL_MS"));
        assertFalse(source.contains("host.robotApi().mqPower(false)"));
    }
}
