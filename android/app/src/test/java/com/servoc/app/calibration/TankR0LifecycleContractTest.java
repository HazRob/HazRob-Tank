package com.servoc.app.calibration;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** R0 screen navigation must never leave duplicate polling or auto-commit NVS. */
public final class TankR0LifecycleContractTest {
    private static String src(String path)throws Exception{
        return new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/"+path)),
            StandardCharsets.UTF_8);
    }
    @Test public void onlySettingsOwnsR0AndHostDestroysController()throws Exception{
        String settings=src("ui/SettingsTab.java");
        assertTrue(src("MainActivity.java").contains("if(settingsTab!=null)settingsTab.release()"));
        assertTrue(settings.contains("if(userCalibration!=null)userCalibration.release()"));
        assertTrue(settings.contains("if(userCalibration!=null)userCalibration.onHidden()"));
        assertTrue(settings.contains("userCalibration.onShown()"));
    }
    @Test public void uiPollLifecycleAndExplicitNvApproval()throws Exception{
        String s=src("ui/OperatorR0CalibrationScreen.java");
        assertTrue(s.contains("private boolean disposed=false,visible=false"));
        assertTrue(s.contains("main.removeCallbacksAndMessages(null)"));
        assertTrue(s.contains("main.removeCallbacks(poll)"));
        assertTrue(s.contains("generation!=token"));
        assertTrue(s.contains("host.robotApi().calibrationCancel()"));
        assertTrue(s.contains("FirmwareR0Contract.committedValues(verified)"));
        assertTrue(s.contains("ManualR0Calibration.saveFirmware("));
        assertFalse(s.contains("automaticSaveDialog"));
    }
}
