package com.servoc.app.ui;

import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

/** Regression contracts for sessions-tab crash and large-site refresh. */
public final class TankV4SessionsCrashGuardTest {
    private String src(String path)throws Exception{
        return new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/"+path)),StandardCharsets.UTF_8);
    }
    @Test public void sessionsIsSecondAfterMeasurementsWithMatchingViewAndTabLabels()throws Exception {
        String s=src("MainActivity.java");
        assertTrue(s.contains("String[] names={\"القيادة\",\"القياسات\",\"الجلسات\""));
        assertTrue(s.contains("tabViews=new View[]{controlTab.view(),withRefresh(measurementsTab.view()"));
        assertTrue(s.contains("withRefresh(fieldSessionsTab.view()"));
        assertTrue(s.contains("if(index==2)fieldSessionsTab.refresh()"));
        assertTrue(s.contains("else if(selectedTab==1)measurementsTab.refresh()"));
    }
    @Test public void databaseExceptionOrReentrantAdapterCannotTerminateActivity()throws Exception{
        String s=src("ui/FieldSessionsTab.java");
        assertTrue(s.contains("if(refreshing){refreshQueued=true;return;}"));
        assertTrue(s.contains("repo.verifySessionSchema();"));
        assertTrue(s.contains("catch(RuntimeException ex)"));
        assertTrue(s.contains("showSessionLoadError();"));
        assertTrue(s.contains("if(selectingScope||pos!=siteDropdown.getSelectedItemPosition())return;"));
        assertTrue(s.contains("if(selectingScope||selectedSiteId.isEmpty()||pos!=sessionDropdown.getSelectedItemPosition())return;"));
        assertTrue(s.contains("siteOptionIds.get(pos)"));
        assertTrue(s.contains("sessionOptionIds.get(pos)"));
        assertTrue(s.contains("siteOptionIds.clear();sessionOptionIds.clear();"));
        assertTrue(s.contains("siteReportPdf.setEnabled(false)"));
        assertTrue(s.contains("deleteSite.setEnabled(false)"));
    }
    @Test public void reportRefreshUsesSingleAggregatedQuery()throws Exception{
        String s=src("field/FieldSessionRepository.java");
        assertTrue(s.contains("measurementCountsForSite(String siteId)"));
        assertTrue(s.contains("GROUP BY f.field_session_id"));
        assertTrue(s.contains("LEFT JOIN field_session_measurements"));
        String ui=src("ui/FieldSessionsTab.java");
        assertTrue(ui.contains("repo.measurementCountsForSite(id)"));
        assertFalse(ui.contains("for(FieldSessionRepository.FieldSession q:children)measurementCount+=repo.measurementSessionIds("));
    }
}
