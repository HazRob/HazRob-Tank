package com.servoc.app.ui;

import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

/** Structural regression for heavy saved-report reads on the Android main thread. */
public final class TankV4ReportsAsyncContractTest {
    private static String source()throws Exception{
        return new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/ui/ReportsTab.java")),StandardCharsets.UTF_8);
    }

    @Test public void reportsScopeAndHistoryRunAsynchronousQueries()throws Exception{
        String s=source();
        assertTrue(s.contains("class ScopeData"));
        assertTrue(s.contains("host.io().execute(()->{"));
        assertTrue(s.contains("List<FieldSessionRepository.Site> sites=fieldSessions.sites()"));
        assertTrue(s.contains("fieldSessions.approvedSessionsForSite(effectiveSite)"));
        assertTrue(s.contains("ExperimentQueries.approvedFieldReportsForScope("));
        assertTrue(s.contains("host.database(),effectiveSite,effectiveSession,100)"));
        assertTrue(s.contains("chosenSessionId=next;updateExportButtonLabels();refresh()"));
        assertTrue(s.contains("for(FieldSessionRepository.FieldSession item:sessions)"));
        assertTrue(s.contains("ScopeData data=new ScopeData(siteId,sessionId,sites,sessions,records)"));
        assertTrue(s.contains("if(released||reportGeneration!=request)return"));
        assertTrue(s.contains("if(!chosenSiteId.equals(siteId)||!chosenSessionId.equals(sessionId))"));
        assertTrue(s.contains("refreshSiteScope(data)"));
        assertTrue(s.contains("renderHistory(data.records)"));
        assertFalse(s.contains("private void refreshSiteScope(){"));
        assertFalse(s.contains("private void refreshSessionScope(){"));
    }

    @Test public void savedChemCoreAnalysisIsReadOnlyOnWorkerAndBoundToSelectedId()throws Exception{
        String s=source();
        assertTrue(s.contains("ExperimentQueries.Record row=ExperimentQueries.byId(host.database(),id)"));
        assertTrue(s.contains("decisions.latestPayloadForSession(sid)"));
        assertTrue(s.contains("SessionSeries.forExperiment(host.database(),row)"));
        assertTrue(s.contains("fieldSessions.fieldSessionIdForMeasurement(sid)"));
        assertTrue(s.contains("if(released||reportGeneration!=request||selectedId!=id)return"));
        assertTrue(s.contains("showSelectedReady(row,decision,series,fieldId)"));
        assertTrue(s.contains("reportGeneration++"));
        assertTrue(s.contains("chartGeneration++"));
        assertTrue(s.contains("private void showSelectedReady("));
    }

    @Test public void scopedSiteExportAndSessionExportPreserveTheirDifferentFormats()throws Exception{
        String s=source();
        assertTrue(s.contains("if(chosenSessionId.isEmpty())SiteSummaryExportActivity.start("));
        assertTrue(s.contains("else host.requestFieldSessionExport(format,chosenSessionId)"));
        assertTrue(s.contains("UiKit.outlinedSpinner(siteSelector)"));
        assertTrue(s.contains("UiKit.outlinedSpinner(fieldSelector)"));
    }
}
