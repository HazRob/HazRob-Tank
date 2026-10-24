package com.servoc.app.ui;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Prevent hidden graphs when instant operation is independent of approved site reports. */
public final class TankV4InstantChartsContractTest {
    private String read(String p)throws Exception{
        return new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/"+p)),StandardCharsets.UTF_8);
    }
    @Test public void chartPickerVisibleWithinChemCoreAndPreservesTwentySiteReportTypes()throws Exception{
        String ui=read("ui/MeasurementsTab.java");
        String reports=read("ui/ReportsTab.java");
        assertTrue(ui.contains("قائمة اختيار مخططات ChemCore"));
        assertTrue(ui.contains("chartChooser.setAdapter(UiKit.lightOptions"));
        assertTrue(ui.contains("TankInstantCharts.available"));
        assertTrue(ui.contains("TankInstantCharts.render"));
        assertTrue(ui.contains("openLargeChart()"));
        assertTrue(reports.contains("private static final String[] CHARTS="));
        assertTrue(reports.contains("chartSelector"));
    }
    @Test public void graphValuesFromRealSingleCaptureAndNoFakeTimeSeries()throws Exception{
        String src=read("report/TankInstantCharts.java");
        assertTrue(src.contains("mq.optJSONObject(ch.key)"));
        assertTrue(src.contains("candidate_score_pct"));
        assertTrue(src.contains("Double.isFinite(v)"));
        assertFalse(src.contains("System.currentTimeMillis()"));
        assertFalse(src.contains("Math.random()"));
    }
}
