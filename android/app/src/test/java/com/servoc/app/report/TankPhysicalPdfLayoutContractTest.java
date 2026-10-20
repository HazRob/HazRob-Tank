package com.servoc.app.report;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/** Pure source contract; a real Android PDF viewer inspection is still mandatory. */
public final class TankPhysicalPdfLayoutContractTest {
    private String source()throws Exception{
        return new String(Files.readAllBytes(Paths.get(
            "src/main/java/com/servoc/app/report/TankPhysicalSnapshotPdf.java")),StandardCharsets.UTF_8);
    }
    @Test public void rendersArabicThroughBidiLayoutAndLeavesNumericColumnSeparate()throws Exception{
        String s=source();
        assertTrue(s.contains("StaticLayout.Builder.obtain"));
        assertTrue(s.contains("TextDirectionHeuristics.RTL"));
        assertTrue(s.contains("Layout.Alignment.ALIGN_NORMAL"));
        assertTrue(s.contains("setEllipsize(TextUtils.TruncateAt.END)"));
        assertTrue(s.contains("rtlField(c,PhysicalCatalog.NAMES_AR[i]"));
        assertFalse(s.contains("c.drawText(s,560,y,p)")); // no raw Arabic Canvas line
    }
    @Test public void frozenPhysicalReportExplicitlyExcludesPmAndUncalibratedNoiseThresholds()throws Exception{
        String s=source();
        assertTrue(s.contains("if(i==9||i==10||i==11)continue"));
        assertTrue(s.contains("calibrated_dba"));
        assertTrue(s.contains("rms_v"));
        assertTrue(s.contains("p2p_v"));
        assertTrue(s.contains("لقطة قياس فورية"));
        assertTrue(s.contains("غير معاير"));
    }
}
