package com.servoc.app.ui;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Source-level guard; on-device light/dark/RTL screenshots remain an independent gate. */
public final class TankV4OutlinedContrastTest {
    @Test public void outlinedFieldsAndDropdownsHaveVisibleBordersAndDarkPopupText() throws Exception {
        String s=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/ui/UiKit.java")),StandardCharsets.UTF_8);
        assertTrue(s.contains("e.setBackground(outline(c,HAZ_SURFACE,0xFF8994A3))"));
        assertTrue(s.contains("spinner.setBackground(outline(spinner.getContext(),HAZ_SURFACE,0xFF8994A3))"));
        assertTrue(s.contains("t.setTextColor(Color.BLACK)"));
        assertTrue(s.contains("t.setBackgroundColor(Color.WHITE)"));
    }
    @Test public void runtimeGuardResolvesGradientFillBeforeChangingSafetyText() throws Exception {
        String s=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/ui/UiKit.java")),StandardCharsets.UTF_8);
        assertTrue(s.contains("private static int solidDrawableColor("));
        assertTrue(s.contains("drawable instanceof GradientDrawable"));
        assertTrue(s.contains("((GradientDrawable)drawable).getColor()"));
        assertTrue(s.contains("int color=solidDrawableColor(cur.getBackground(),Color.TRANSPARENT)"));
        assertTrue(s.contains("solidDrawableColor(v.getBackground(),Color.TRANSPARENT)"));
        assertTrue(s.contains("if(contrastRatio(current,background)<MIN_TEXT_CONTRAST)"));
    }
}
