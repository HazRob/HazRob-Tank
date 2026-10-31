package com.servoc.app.report;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static org.junit.Assert.*;

/** Source-contract of V7 Field Session XLSX only. No change to acquisition or UI. */
public final class TankV7AdsRawExportContractTest {
    private static String src(String relative)throws Exception{
        return new String(Files.readAllBytes(
            Paths.get("src/main/java/com/servoc/app/"+relative)),StandardCharsets.UTF_8);
    }

    @Test public void xlsxReportsEightOriginalMqAdcCodesAndVoltageSeparately()throws Exception{
        String writer=src("report/SmartFieldSessionXlsxWriter.java");
        assertTrue(writer.contains("MQ الثمانية الخام"));
        assertTrue(writer.contains("MQ الأصل لكل حساس"));
        assertTrue(writer.contains("headings.add(hw.key+\" ADS raw code (4-conv mean)\")"));
        assertTrue(writer.contains("row.add(exactNumber(q,\"raw\"))"));
        assertTrue(writer.contains("row.add(exactNumber(q,\"adc\"))"));
        assertTrue(writer.contains("row.add(exactNumber(q,\"ao\"))"));
        assertTrue(writer.contains("row.add(exactNumber(q,\"rs\"))"));
        assertTrue(writer.contains("row.add(exactNumber(q,\"r0\"))"));
        assertTrue(writer.contains("originalJson(q)"));
        assertFalse(writer.contains("adcFromMq("));
    }

    @Test public void xlsxPreservesStoredCalibrationAndTimingWithoutInference()throws Exception{
        String writer=src("report/SmartFieldSessionXlsxWriter.java");
        assertTrue(writer.contains("exactNumber(q,\"scan_cycle\")"));
        assertTrue(writer.contains("exactNumber(q,\"sample_us\")"));
        assertTrue(writer.contains("exactNumber(q,\"firmware_r0\")"));
        assertTrue(writer.contains("exactNumber(state,\"sample_utc_ms\")"));
        assertTrue(writer.contains("SELECT id,t_ms,env_payload,mq_payload,motion_payload"));
        assertTrue(writer.contains("WHERE session_id=? ORDER BY t_ms,id"));
    }
}
