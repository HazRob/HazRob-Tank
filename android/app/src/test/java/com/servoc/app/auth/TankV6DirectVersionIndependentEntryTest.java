package com.servoc.app.auth;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.*;

/** V6-001: Direct entry is transport discovery, never app/firmware release matching.
 * This JVM regression is not a substitute for live HazRob AP, motor safety or UI testing.
 */
public final class TankV6DirectVersionIndependentEntryTest {
    @Test public void knownTankFirmwareBannersAreIndependentOfAndroidAppVersion(){
        assertTrue(TankFirmwareCompatibility.looksLikeTank("ESP32-Tank 0.1.4-v4-dev"));
        assertTrue(TankFirmwareCompatibility.looksLikeTank("ESP32-Tank 0.1.5-v5-dev"));
        assertTrue(TankFirmwareCompatibility.looksLikeTank("ESP32-Tank 0.2.0-v6-dev"));
        assertFalse(TankFirmwareCompatibility.looksLikeTank("HazRob-CAM 0.1.5-v5-dev"));
        assertFalse(TankFirmwareCompatibility.looksLikeTank("Unexpected device"));
        assertFalse(TankFirmwareCompatibility.looksLikeTank(null));
    }

    @Test public void directGateNeverChecksAndroidVersionAgainstFirmwareBanner()throws Exception{
        String s=new String(Files.readAllBytes(Paths.get(
            "src/main/java/com/servoc/app/auth/ConnectionGate.java")),StandardCharsets.UTF_8);
        assertTrue(s.contains("TankFirmwareCompatibility.looksLikeTank(banner)"));
        assertFalse(s.contains("isExpectedFirmwareBanner("));
        assertFalse(s.contains("ReleaseAudit.VERSION"));
        assertTrue(s.contains("if(explicitlyRemote)"));
        assertFalse(s.contains("boolean switchNeeded=true"));
        assertTrue(s.contains("WifiRoute.waitForRobotWifi(a,30000)"));
        assertTrue(s.contains("listener.onReady(Mode.DIRECT)"));
        assertTrue(s.contains("listener.onReady(Mode.OFFLINE)"));
    }
}
