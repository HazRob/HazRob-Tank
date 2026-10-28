package com.servoc.app.chemcore;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** V5 packaging PRECONDITIONS. Does not generate or test an APK. */
public final class TankV5StableCompatibilitySourceTest {
    @Test public void generatorIsPreparedOnlyAfterReleaseAuditAndAppIdMatch()throws Exception{
        String script=new String(Files.readAllBytes(Paths.get(
            "../tools/prepare_v5_stable_source.py")),StandardCharsets.UTF_8);
        assertTrue(script.contains("version=\"5.0\""));
        assertTrue(script.contains("applicationId 'com.hazrob.tank'"));
        assertTrue(script.contains("versionName '5.0'"));
        assertTrue(script.contains("SERVO_DRIVE=true"));
        assertTrue(script.contains("calibration_count"));
        assertTrue(script.contains("payload_sha256"));
        assertFalse(script.contains("subprocess.run"));
        assertFalse(script.contains("gradle assemble"));
        String release=new String(Files.readAllBytes(Paths.get(
            "src/main/java/com/servoc/app/ReleaseAudit.java")),StandardCharsets.UTF_8);
        assertTrue(release.contains("VERSION=\"5.0\""));
    }
}
