package com.servoc.app;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Cross-layer source guard: HR069 hazard/interlock telemetry must survive Android normalization. */
public class SafetyTelemetryNormalizationContractTest {
    private static String source(String relative) throws Exception {
        Path p=Paths.get(relative);
        if(!Files.exists(p))p=Paths.get("app").resolve(relative);
        if(!Files.exists(p))p=Paths.get("..").resolve(relative);
        return new String(Files.readAllBytes(p),StandardCharsets.UTF_8);
    }

    @Test public void tankNormalizationDoesNotFabricateIrOrAutoStop()throws Exception{
        String p=source("app/src/main/java/com/servoc/app/communication/RobotProtocolV4.java");
        assertFalse(p.contains("out.put(\"ir\",ir("));
        assertFalse(p.contains("drive.put(\"safety_auto_stop\",safetyAutoStop)"));
        assertTrue(p.contains("proximity"));
    }
}
