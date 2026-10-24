package com.servoc.app.ui;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Current operation is displayable and PDF-exportable after link loss; no old snapshot lookup. */
public final class TankV4LiveOfflineResultTest {
    @Test public void liveOfflineRefreshMustNotEraseFrozenCurrentOperation() throws Exception {
        String source=new String(Files.readAllBytes(Paths.get(
            "src/main/java/com/servoc/app/ui/MeasurementsTab.java")),StandardCharsets.UTF_8);
        String pane=source.substring(source.indexOf("private final class LivePane"),
            source.indexOf("private final class SensorCard"));
        assertTrue(pane.contains("TankInstantAnalyzer.Result completed=currentOperation"));
        assertTrue(pane.contains("التقرير متاح دون اتصال"));
        assertTrue(pane.contains("report.setEnabled(completed!=null)"));
        assertTrue(pane.contains("progress.setProgress(1000)"));
        assertFalse(pane.contains("حفظ لقطة فورية"));
        assertFalse(pane.contains("اللقطات الفورية المحلية"));
        assertTrue(pane.contains("host.requestInstantReportPdf(result.original,result.analysis,result.id,result.capturedAtMs)"));
    }
}
