package com.servoc.app.ui;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Source contracts for field-scoped report/measurement routing; Android UI still needs field tests. */
public final class TankV4FieldReportPipelineContractTest {
    private static String read(String path)throws Exception{
        return new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/"+path)),StandardCharsets.UTF_8);
    }

    @Test public void chosenSiteAndSessionReportsExportTheirActualFormats()throws Exception{
        String ui=read("ui/FieldSessionsTab.java");
        assertTrue(ui.contains("SiteSummaryExportActivity.start(scroll.getContext(),selectedSiteId,\"pdf\")"));
        assertTrue(ui.contains("SiteSummaryExportActivity.start(scroll.getContext(),selectedSiteId,\"xlsx\")"));
        assertTrue(ui.contains("host.requestFieldSessionExport(\"pdf\",q.id)"));
        assertTrue(ui.contains("host.requestFieldSessionExport(\"xlsx\",q.id)"));
        assertTrue(ui.contains("host.openReportsForSite(q.siteId,q.id)"));
    }

    @Test public void reportsSelectionReusesIdsAndOutlinedDropdowns()throws Exception{
        String s=read("ui/ReportsTab.java");
        assertTrue(s.contains("boundSiteIds.equals(ids)"));
        assertTrue(s.contains("boundSessionIds.equals(ids)"));
        assertTrue(s.contains("if(rebindingScope||position"));
        assertTrue(s.contains("UiKit.outlinedSpinner(siteSelector)"));
        assertTrue(s.contains("UiKit.outlinedSpinner(fieldSelector)"));
        assertTrue(s.contains("UiKit.lightOptions(scroll.getContext(),labels.toArray("));
        assertTrue(s.contains("SiteSummaryExportActivity.start(scroll.getContext(),chosenSiteId,format)"));
        assertTrue(s.contains("host.requestFieldSessionExport(format,chosenSessionId)"));
    }

    @Test public void fieldMeasurementCannotSilentlyTurnIntoOrphan()throws Exception{
        String recorder=read("measurement/TelemetryRecorder.java");
        assertTrue(recorder.contains("MeasurementSessionRegistry.fieldCaptureOwnerSessionId()"));
        assertTrue(recorder.contains("!fieldSessions.linkMeasurement(active.id,sessionId)"));
        assertTrue(recorder.contains("throw new IllegalStateException("));
        assertTrue(recorder.contains("delete(\"measurement_sessions\",\"session_id=?\""));
        assertTrue(recorder.contains("MeasurementSessionRegistry.started(sessionId)"));
        String repo=read("field/FieldSessionRepository.java");
        assertTrue(repo.contains("FieldSessionPolicy.mayAttachMeasurement(kind,role)"));
        assertTrue(repo.contains("AND measurement_session_id IS NULL AND state IN (\'ARMED\',\'WARMUP\',\'SAMPLING\')"));
        assertTrue(repo.contains("approved cycles only") || repo.contains("Reports consume approved cycles only"));
        assertTrue(repo.contains("field_session_measurements\",\"field_measurement_cycles\",\"measurement_sessions\",\"telemetry_samples"));
    }

    @Test public void originalSessionReportsStayOutsideDeveloperScientificLibrary()throws Exception{
        String repo=read("field/FieldSessionRepository.java");
        assertTrue(repo.contains("STATUS_APPROVED"));
        assertTrue(repo.contains("FieldSessionApprovalPolicy.completedSuccessfully"));
        assertTrue(repo.contains("Refusing deletion: scientific capture uses this identity"));
        assertTrue(repo.contains("Refusing deletion: published or controlled reference"));
        String activity=read("MainActivity.java");
        assertTrue(activity.contains("FieldSessionReportExporter.writePdf(out,db,fieldId,chemicals)"));
        assertTrue(activity.contains("FieldSessionReportExporter.writeXlsx(out,db,fieldId,chemicals)"));
    }
}
