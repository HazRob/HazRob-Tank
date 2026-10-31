package com.servoc.app.ui;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** V8 source contracts supersede the V5 migrated DeveloperR0Tab screen. */
public final class TankV5OperatorR0ContractTest {
    private static String src(String p)throws Exception{
        return new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/"+p)),
            StandardCharsets.UTF_8);
    }
    @Test public void operatorR0IsRebuiltInSettingsAndNotDeveloper()throws Exception{
        String settings=src("ui/SettingsTab.java"),dev=src("ui/DeveloperTab.java");
        String screen=src("ui/OperatorR0CalibrationScreen.java");
        assertTrue(settings.contains("new OperatorR0CalibrationScreen(scroll.getContext(),host)"));
        assertTrue(settings.contains("userCalibration.onShown()"));
        assertTrue(settings.contains("userCalibration.onHidden()"));
        assertTrue(settings.contains("userCalibration.release()"));
        assertFalse(dev.contains("new OperatorR0CalibrationScreen("));
        assertFalse(settings.contains("new DeveloperR0Tab("));
        assertFalse(Files.exists(Paths.get("src/main/java/com/servoc/app/ui/DeveloperR0Tab.java")));
        assertTrue(screen.contains("main.removeCallbacks(poll)"));
        assertTrue(screen.contains("generation++"));
        assertTrue(screen.contains("onHidden()"));
    }
    @Test public void approvalVerifiesFirmwareAndSavedPhoneWithoutFabrication()throws Exception{
        String screen=src("ui/OperatorR0CalibrationScreen.java");
        assertTrue(screen.contains("calibrationStart(180,60)"));
        assertTrue(screen.contains("FirmwareR0Contract.pendingValues("));
        assertTrue(screen.contains("FirmwareR0Contract.committedValues(verified)"));
        assertTrue(screen.contains("host.robotApi().calibrationAccept()"));
        assertTrue(screen.contains("host.robotApi().calibrationStatus()"));
        assertTrue(screen.contains("ManualR0Calibration.saveFirmware("));
        assertTrue(screen.contains("requestCancel()"));
        assertTrue(screen.contains("هوية ESP32"));
        assertFalse(screen.contains("host.robotApi().mqPower(false)"));
    }
}
