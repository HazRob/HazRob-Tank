package com.servoc.app;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** HR069-R1: E-STOP/watchdog independent, IR/Echo auto-stop is explicit/persistent. */
public class SafetyAutoStopOptionContractTest {
    private static String source(String path)throws Exception{
        Path p=Paths.get(path);if(!Files.exists(p))p=Paths.get("app").resolve(path);
        if(!Files.exists(p))p=Paths.get("..").resolve(path);
        return new String(Files.readAllBytes(p),StandardCharsets.UTF_8);
    }

    @Test public void tankRemovesObstacleAutoStopUiAndApi()throws Exception{
        String api=source("app/src/main/java/com/servoc/app/communication/RobotApi.java");
        String settings=source("app/src/main/java/com/servoc/app/ui/SettingsTab.java");
        assertFalse(api.contains("safetyAutoStopStatus()"));
        assertFalse(api.contains("setSafetyAutoStop(boolean enabled)"));
        assertFalse(settings.contains("applySafetyAutoStop(boolean enabled)"));
        assertFalse(settings.contains("safetyAutoStop=new CheckBox"));
    }

    @Test public void emergencyStopAndWatchdogStayIndependent()throws Exception{
        String api=source("app/src/main/java/com/servoc/app/communication/RobotApi.java");
        assertTrue(api.contains("/estop"));
        assertTrue(api.contains("/motion/stop"));
    }
}
