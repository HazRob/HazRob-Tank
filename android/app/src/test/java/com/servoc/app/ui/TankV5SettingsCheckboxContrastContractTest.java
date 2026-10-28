package com.servoc.app.ui;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Readability source contract only; device light/dark and RTL screenshots pending. */
public final class TankV5SettingsCheckboxContrastContractTest {
    @Test public void fourReverseDirectionCheckboxesAreOutlinedAndRetainStopSemantics()throws Exception{
        String s=new String(Files.readAllBytes(Paths.get(
            "src/main/java/com/servoc/app/ui/SettingsTab.java")),StandardCharsets.UTF_8);
        assertTrue(s.contains("b.setBackground(UiKit.outline(c,0xFFF7F8FA,0xFF60758C))"));
        assertTrue(s.contains("b.setButtonTintList(android.content.res.ColorStateList.valueOf(0xFF145C7A))"));
        assertTrue(s.contains("b.setMinHeight(UiKit.dp(c,48))"));
        assertTrue(s.contains("host.setDriveCommand(0,0)"));
        for(String k:new String[]{"PREF_REVERSE_WHEEL","PREF_REVERSE_JOYSTICK",
                "PREF_REVERSE_BUTTONS","PREF_REVERSE_PIVOT_ALL"})
            assertTrue(s.contains("DriveDirectionOption."+k));
    }
}
