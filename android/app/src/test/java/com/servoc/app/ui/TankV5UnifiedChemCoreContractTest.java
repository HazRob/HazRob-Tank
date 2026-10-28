package com.servoc.app.ui;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** UI source assertions. Android rendering and accessibility still require device tests. */
public final class TankV5UnifiedChemCoreContractTest {
    private String code()throws Exception {
        return new String(Files.readAllBytes(Paths.get(
            "src/main/java/com/servoc/app/ui/MeasurementsTab.java")), StandardCharsets.UTF_8);
    }
    @Test public void oneAnalysisWorkspaceWithIndependentInstantMode()throws Exception {
        String s=code();
        assertTrue(s.contains("String[] sectionNames={\"⏱ القياس الآني\",\"🧪 ChemCore والمرشحون\"}"));
        assertTrue(s.contains("analysisTabs=new Button[2]"));
        assertFalse(s.contains("\"🏅 المرشحون\""));
        assertTrue(s.contains("View target=analysisSection==0?live.view:analysisResults.view"));
        assertTrue(s.contains("analysisResults.box.addView(analysis.box"));
        assertTrue(s.contains("else{analysisResults.refresh();analysis.refresh();}"));
        assertFalse(s.contains("showAnalysisSection(2)"));
        assertTrue(s.contains("currentOperation.analysis"));
    }
    @Test public void sameAnalysisSuppliesCandidateWarningsAndCharts()throws Exception{
        String s=code();
        assertTrue(s.contains("TankCandidateHazardBadges.forCandidate(p,uid)"));
        assertTrue(s.contains("TankScreeningExposureWarning.summary(p,8)"));
        assertTrue(s.contains("TankPpmEstimate.from(p)"));
        assertTrue(s.contains("TankInstantCharts.render"));
        assertTrue(s.contains("host.requestInstantReportPdf(result.original,result.analysis,result.id,result.capturedAtMs)"));
        assertTrue(s.contains("trackingTargetId()"));
    }
}
