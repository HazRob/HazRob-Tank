package com.servoc.app.calibration;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** No 20-minute R0 acquisition or save dialog may outlive its Android Activity. */
public final class TankR0LifecycleContractTest {
    private static String src(String path)throws Exception{
        return new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/"+path)),StandardCharsets.UTF_8);
    }
    @Test public void hostDestroyDisposesDeveloperAutomaticR0()throws Exception{
        assertTrue(src("MainActivity.java").contains("if(developerTab!=null)developerTab.release()"));
        assertTrue(src("ui/SettingsTab.java").contains("if(userCalibration!=null)userCalibration.release()"));
    }
    @Test public void cancellationAndActivityDestroyCannotAccidentallyApplyLateR0()throws Exception{
        String s=src("ui/DeveloperR0Tab.java");
        assertTrue(s.contains("private volatile boolean disposed=false"));
        assertTrue(s.contains("if(disposed||!automaticActive)return"));
        assertTrue(s.contains("clock.removeCallbacks(autoTick)"));
        assertTrue(s.contains("scroll.setKeepScreenOn(false)"));
        assertTrue(s.contains("host.robotApi().calibrationCancel()"));
        assertTrue(s.contains("FirmwareR0Contract.committedValues(response)"));
        assertTrue(s.contains("ManualR0Calibration.saveFirmware("));
        assertFalse(s.contains("automaticSaveDialog"));
        assertFalse(s.contains("awaitingSave"));
    }

}
