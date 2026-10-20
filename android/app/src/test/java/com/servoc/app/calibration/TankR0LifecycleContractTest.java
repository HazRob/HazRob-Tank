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
        assertTrue(src("ui/DeveloperTab.java").contains("if(r0!=null)r0.release()"));
    }
    @Test public void cancellationPreventsLateSaveAndTimerLeak()throws Exception{
        String s=src("ui/DeveloperR0Tab.java");
        assertTrue(s.contains("private volatile boolean disposed=false"));
        assertTrue(s.contains("if(disposed)return; // Do not restart acquisition"));
        assertTrue(s.contains("clock.removeCallbacks(autoTick)"));
        assertTrue(s.contains("automaticSaveDialog.dismiss()"));
        assertTrue(s.contains("awaitingSave=false"));
        assertTrue(s.contains("scroll.setKeepScreenOn(false)"));
    }
}
