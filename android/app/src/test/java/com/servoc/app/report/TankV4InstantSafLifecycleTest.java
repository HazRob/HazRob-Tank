package com.servoc.app.report;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Prevents loss of the frozen one-shot analysis while Android's SAF picker recreates the Activity. */
public final class TankV4InstantSafLifecycleTest {
    private String main() throws Exception {
        return new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/MainActivity.java")),
            StandardCharsets.UTF_8);
    }
    @Test public void instantExportStateIsIndependentOfPhysicalPicker() throws Exception {
        String src=main();
        String save=src.substring(src.indexOf("@Override protected void onSaveInstanceState"),
            src.indexOf("@Override protected void onDestroy"));
        int physical=save.indexOf("if(pendingPhysicalDocumentRequest)");
        int instant=save.indexOf("if(!pendingInstantId.isEmpty())");
        assertTrue("physical snapshot save must exist",physical>=0);
        assertTrue("instant operation must save independently",instant>physical);
        assertTrue("physical block must close before instant block",
            save.substring(physical,instant).contains("tank_v3_physical_export_utc"));
        assertFalse("instant operation must not be saved inside physical-only branch",
            save.substring(physical,instant).contains("tank_v4_instant_telemetry"));
        assertTrue(save.substring(instant).contains("out.putString(\"tank_v4_instant_telemetry\""));
        assertTrue(save.substring(instant).contains("out.putString(\"tank_v4_instant_analysis\""));
        assertTrue(save.substring(instant).contains("out.putString(\"tank_v4_instant_id\""));
        assertTrue(save.substring(instant).contains("out.putLong(\"tank_v4_instant_time\""));
    }
    @Test public void reconstructedActivityReadsFrozenIdentityAndReportUsesIt() throws Exception {
        String src=main();
        assertTrue(src.contains("pendingInstantId=b.getString(\"tank_v4_instant_id\",\"\")"));
        assertTrue(src.contains("pendingInstantTelemetry=b.getString(\"tank_v4_instant_telemetry\",\"{}\")"));
        assertTrue(src.contains("pendingInstantAnalysis=b.getString(\"tank_v4_instant_analysis\",\"{}\")"));
        assertTrue(src.contains("final String frozen=pendingInstantTelemetry,analysis=pendingInstantAnalysis,"));
        assertTrue(src.contains("opId=pendingInstantId"));
        assertTrue(src.contains("TankInstantReportPdf.write(out,new JSONObject(frozen)"));
    }
}
