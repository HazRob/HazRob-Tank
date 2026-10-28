package com.servoc.app.ui;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Static UX geometry contracts; screenshot, motion and orientation device tests remain pending. */
public final class TankV5TrackingAndLandscapeContractTest {
    static String src(String path)throws Exception{
        return new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/"+path)),
                StandardCharsets.UTF_8);
    }
    @Test public void gasTrackingIsLargeGreenTranslucentAndNonDriving()throws Exception{
        String hud=src("vision/HazardOverlayView.java");
        assertTrue(hud.contains("side*0.38f"));
        assertTrue(hud.contains("0xA800D65A"));
        assertTrue(hud.contains("0x809BFF9A"));
        assertTrue(hud.contains("c.rotate((float)Math.toDegrees(gasAngleRad))"));
        assertTrue(hud.contains("setClickable(false)"));
        assertTrue(hud.contains("confidence>=0.18"));
        assertFalse(hud.contains("robotApi()"));
        assertFalse(hud.contains("setDriveCommand("));
    }
    @Test public void landscapeNavigationAndEchoBelongToCorrectViewOwners()throws Exception{
        String host=src("MainActivity.java"),drive=src("ui/ControlTab.java");
        assertTrue(host.contains("mainTabStrip=tabsScroll"));
        assertTrue(host.contains("getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE"));
        assertTrue(host.contains("mainTabStrip.setVisibility(fullscreenDrive?View.GONE:View.VISIBLE)"));
        assertTrue(drive.contains("Gravity.CENTER_HORIZONTAL|Gravity.TOP"));
        assertTrue(drive.contains("distanceBadge.setMaxWidth("));
        assertTrue(drive.contains("landscape?6:10"));
        assertFalse(drive.contains("if(p instanceof LinearLayout)"));
        assertTrue(drive.contains("stop=UiKit.button(c,\"E-STOP\")"));
        assertTrue(drive.contains("cameraBox.addView(actions,ap)"));
    }
}
