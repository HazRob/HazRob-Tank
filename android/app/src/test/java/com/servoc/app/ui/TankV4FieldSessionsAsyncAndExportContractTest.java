package com.servoc.app.ui;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/**
 * V4 source-contract regression; instrumentation on a populated migrated SQLite DB
 * remains mandatory before release. This checks intended thread/scope wiring only.
 */
public final class TankV4FieldSessionsAsyncAndExportContractTest {
    private static String src(String path)throws Exception{
        return Files.readString(Paths.get("src/main/java/com/servoc/app/"+path),StandardCharsets.UTF_8);
    }

    @Test public void sessionDbQueriesAreDispatchedOffMainThread()throws Exception{
        String code=src("ui/FieldSessionsTab.java");
        assertTrue(code.contains("host.io().execute(()->{"));
        assertTrue(code.contains("repo.verifySessionSchema()"));
        assertTrue(code.contains("new SessionData(active,ss,id,children,approved,counts)"));
        assertTrue(code.contains("main.post(()->{"));
        assertTrue(code.contains("renderSessionUi(result)"));
        assertFalse(code.contains("void renderSessionUi(){"));
        assertTrue(code.contains("refreshQueued=true"));
        assertTrue(code.contains("!requestedSite.equals(selectedSiteId)"));
        assertFalse(code.contains("selectingScope||refreshing||"));
        assertTrue(code.contains("if(refreshing){refreshQueued=true;return;}"));
    }

    @Test public void fieldWritesRunOnExecutorNotBlockingUiCallbacks()throws Exception{
        String code=src("ui/FieldSessionsTab.java");
        assertTrue(code.contains("final String id=repo.createSite(name"));
        assertTrue(code.contains("final String id=repo.startFieldSession(site,name)"));
        assertTrue(code.contains("boolean ok=repo.closeFieldSession(a.id)"));
        assertTrue(code.contains("boolean ok=repo.approveFieldSession(q.id)"));
        assertTrue(code.contains("main.post(this::showSessionLoadError)"));
    }

    @Test public void reportPickerChecksApprovalOffMainAndNeverOverlaps()throws Exception{
        String activity=src("MainActivity.java");
        assertTrue(activity.contains("if(pendingFieldSessionPicker)"));
        assertTrue(activity.contains("workers.execute(()->{"));
        assertTrue(activity.contains("selected=new FieldSessionRepository(db).fieldSessionById(fs)"));
        assertTrue(activity.contains("ui.post(()->{"));
        assertTrue(activity.contains("if(!selectedRow.approved())"));
        assertTrue(activity.contains("pendingFieldSessionId=fs"));
        assertTrue(activity.contains("startActivityForResult(i,pdf?EXPORT_FIELD_PDF:EXPORT_FIELD_XLSX)"));
        assertTrue(activity.contains("pendingFieldSessionPicker=false;pendingFieldSessionId=\"\";"));
        assertTrue(activity.contains("FieldSessionReportExporter.writePdf(out,db,fieldId,chemicals)"));
        assertTrue(activity.contains("FieldSessionReportExporter.writeXlsx(out,db,fieldId,chemicals)"));
    }

    @Test public void pendingSessionRefreshesCannotOutliveTheActivity()throws Exception{
        String sessions=src("ui/FieldSessionsTab.java");
        String host=src("MainActivity.java");
        assertTrue(sessions.contains("private volatile boolean disposed=false"));
        assertTrue(sessions.contains("main.removeCallbacksAndMessages(null)"));
        assertTrue(sessions.contains("if(disposed){refreshing=false;return;}"));
        assertTrue(host.contains("if(fieldSessionsTab!=null)fieldSessionsTab.release()"));
    }

    @Test public void exactSiteAndSessionExportFormatsArePreserved()throws Exception{
        String code=src("ui/FieldSessionsTab.java");
        assertTrue(code.contains("SiteSummaryExportActivity.start(scroll.getContext(),selectedSiteId,\"pdf\")"));
        assertTrue(code.contains("SiteSummaryExportActivity.start(scroll.getContext(),selectedSiteId,\"xlsx\")"));
        assertTrue(code.contains("host.requestFieldSessionExport(\"pdf\",q.id)"));
        assertTrue(code.contains("host.requestFieldSessionExport(\"xlsx\",q.id)"));
        String reports=src("ui/ReportsTab.java");
        assertTrue(reports.contains("fieldSessions.approvedSessionsForSite(chosenSiteId)"));
        assertTrue(reports.contains("UiKit.lightOptions(scroll.getContext(),labels.toArray(new String[0]))"));
        assertTrue(reports.contains("if(rebindingScope||position!=fieldSelector.getSelectedItemPosition())return"));
        assertTrue(reports.contains("if(chosenSessionId.isEmpty())SiteSummaryExportActivity.start("));
        assertTrue(reports.contains("else host.requestFieldSessionExport(format,chosenSessionId)"));
    }
}
