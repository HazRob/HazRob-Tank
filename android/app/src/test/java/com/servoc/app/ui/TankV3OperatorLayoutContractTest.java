package com.servoc.app.ui;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/**
 * Source-level V3 UI contracts, no emulator and no fake screenshots.
 * Real device visual approval remains separately required.
 */
public final class TankV3OperatorLayoutContractTest {
    private String src(String file) throws Exception {
        return new String(Files.readAllBytes(Paths.get("app/src/main/java/com/servoc/app/"+file)),StandardCharsets.UTF_8);
    }
    @Test public void refreshNeverAddsAButtonToDrivingTab() throws Exception {
        String main=src("MainActivity.java");
        assertTrue(main.contains("tabViews=new View[]{controlTab.view(),withRefresh("));
        assertTrue(main.contains("private View withRefresh(View content,int topMarginDp)"));
        assertTrue(main.contains("Gravity.TOP|Gravity.LEFT"));
        assertFalse(main.contains("withRefresh(controlTab.view())"));
        assertFalse(main.contains("تحديث البيانات الحية"));
    }
    @Test public void physicalMeasurementCardsAreTwoUpAndPmIsNotMixed() throws Exception {
        String s=src("ui/MeasurementsTab.java");
        assertTrue(s.contains("visibleCards.size();i+=2"));
        assertTrue(s.contains("box.addView(row,UiKit.full(c))"));
        assertTrue(s.contains("if(i==9||i==10||i==11)"));
        assertTrue(s.contains("private View buildParticlesLanding()"));
        assertTrue(s.contains("ALERT_BG=0xFFC62828"));
        assertTrue(s.contains("int color=alert?Color.WHITE:TEXT"));
        assertTrue(s.contains("calibrated&&Double.isFinite(dba)"));
        assertTrue(s.contains("cardAlert(5,dba>90.0)"));
    }
    @Test public void legacyIrCannotPretendToBeAnInstalledTankSensor() throws Exception {
        String main=src("MainActivity.java");
        assertFalse(main.contains("cliff_warning"));
        assertFalse(main.contains("خطر سقوط"));
        assertTrue(main.contains("proximity.optBoolean(\"warning\""));
    }
    @Test public void reportsUseRealChartsAndSeparateEnlargedViewer() throws Exception {
        String reports=src("ui/ReportsTab.java");
        assertTrue(reports.contains("showChartDialog()"));
        assertTrue(reports.contains("chartView.getDrawable()==null"));
        assertTrue(reports.contains("new AlertDialog.Builder(c)"));
        assertTrue(reports.contains("renderSelectedChart"));
    }
}
