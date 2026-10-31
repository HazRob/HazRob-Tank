package com.servoc.app.report;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static org.junit.Assert.*;

/** Regression: ADS1115 raw conversion code must never be relabeled as voltage. */
public final class TankV7AdsRawExportContractTest {
    private static String src(String relative)throws Exception {
        return new String(Files.readAllBytes(
            Paths.get("src/main/java/com/servoc/app/"+relative)),StandardCharsets.UTF_8);
    }

    @Test public void exportsBothRawConversionCodeAndVoltageWithoutInference()throws Exception {
        String writer=src("report/SmartFieldSessionXlsxWriter.java");
        assertTrue(writer.contains("MQ الثمانية الخام"));
        assertTrue(writer.contains("MQ الأصل لكل حساس"));
        assertTrue(writer.contains("headings.add(hw.key+\" ADS raw code (4-conv mean)\")"));
        assertTrue(writer.contains("row.add(exactNumber(q,\"raw\"))"));
        assertTrue(writer.contains("row.add(exactNumber(q,\"adc\"))"));
        assertTrue(writer.contains("row.add(exactNumber(q,\"ao\"))"));
        assertTrue(writer.contains("exactNumber(nativeQ,\"raw\")"));
        assertTrue(writer.contains("originalJson(nativeQ)"));
        assertFalse(writer.contains("adcFromMq("));
        assertFalse(writer.contains("for(String key:new String[]{\"adc_raw\",\"raw_adc\",\"adc\""));
    }

    @Test public void rawFirmwarePayloadAndAcquisitionMetadataArePersistedTogether()throws Exception {
        String recorder=src("measurement/TelemetryRecorder.java");
        assertTrue(recorder.contains("state.put(\"firmware_mq\",new JSONObject(nativeMq.toString()))"));
        assertTrue(recorder.contains("state.put(\"firmware_adc\",new JSONObject(nativeAdc.toString()))"));
        assertTrue(recorder.contains("state.put(\"firmware_device_id\""));
        assertTrue(recorder.contains("state.put(\"firmware_build_revision\""));
        assertTrue(recorder.contains("v.put(\"mq_payload\",mq==null?\"{}\":mq.toString())"));
        assertTrue(recorder.contains("v.put(\"motion_payload\",state.toString())"));
    }

    @Test public void approvedReportScopeAndInvalidationArePreserved()throws Exception {
        String screen=src("ui/ReportsTab.java");
        String queries=src("data/ExperimentQueries.java");
        assertTrue(screen.contains("chosenSessionId=next;updateExportButtonLabels();refresh()"));
        assertTrue(screen.contains("approvedFieldReportsForScope("));
        assertTrue(queries.contains("AND fs.status='APPROVED' AND fs.site_id=?"));
        assertTrue(queries.contains("sql.append(\" AND fs.field_session_id=?\")"));
    }
}
