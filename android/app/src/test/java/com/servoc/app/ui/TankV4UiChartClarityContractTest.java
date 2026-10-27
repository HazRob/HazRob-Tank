package com.servoc.app.ui;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Contract tests for contrast, selectable charts and honest missing-sensor UX.
 *  Pixel screenshots and accessibility testing remain separate Android device gates.
 */
public final class TankV4UiChartClarityContractTest {
    private static String src(String file)throws Exception{
        return new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/ui/"+file)),StandardCharsets.UTF_8);
    }

    @Test public void chemCoreUsesReadableLightOutlinedStatusAndResults()throws Exception{
        String code=src("ChemCoreAnalysisTab.java");
        assertTrue(code.contains("status.setBackground(UiKit.outline("));
        assertTrue(code.contains("signal.setBackground(UiKit.outline("));
        assertTrue(code.contains("resultText.setBackground(UiKit.outline("));
        assertTrue(code.contains("resultText.setTextColor(0xFF17212B)"));
        assertFalse(code.contains("resultText.setBackgroundColor(0xFF11171D)"));
    }

    @Test public void unavailableParticleSensorNeverOffersSyntheticPlots()throws Exception{
        String code=src("MeasurementsTab.java");
        assertTrue(code.contains("المخططات غير متاحة"));
        assertTrue(code.contains("لا توجد مستشعرات جزيئات نشطة"));
        assertFalse(code.contains("لا توجد عينات جزيئات مسجلة لعرض مخطط فعلي."));
        assertTrue(code.contains("report.setEnabled(false)"));
    }

    @Test public void archivedChartsDependOnRealDataAndRejectStaleRenders()throws Exception{
        String code=src("ReportsTab.java");
        assertTrue(code.contains("TankChartEligibility.hasData("));
        assertTrue(code.contains("chartGeneration++;selectedId=-1"));
        assertTrue(code.contains("if(chartGeneration!=request||selectedId!=experimentId)"));
        assertTrue(code.contains("UiKit.outlinedSpinner(chartSelector)"));
        assertTrue(code.contains("UiKit.lightOptions(scroll.getContext(),labels.toArray(new String[0]))"));
    }

    @Test public void allDeveloperSelectorsUseOutlinedBlackText()throws Exception{
        String exp=src("DeveloperExperimentsTab.java");
        assertTrue(exp.contains("UiKit.outlinedSpinner(referenceKindSpinner)"));
        assertTrue(exp.contains("UiKit.outlinedSpinner(modelSensorSpinner)"));
        String kit=src("UiKit.java");
        assertTrue(kit.contains("spinner.setBackground(outline("));
        assertTrue(kit.contains("t.setTextColor(Color.BLACK)"));
        assertTrue(kit.contains("t.setBackgroundColor(Color.WHITE)"));
    }
}
