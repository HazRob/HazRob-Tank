package com.servoc.app.field;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/**
 * Regression contracts for two integration failures found in V5.
 * These are source tests, NOT Android device/ESP32 validation.
 */
public final class TankV5IntegrationOwnerAndEvidenceContractTest {
    private static String src(String file)throws Exception{
        return new String(Files.readAllBytes(Paths.get(
            "src/main/java/com/servoc/app/"+file)),StandardCharsets.UTF_8);
    }
    @Test public void operatorSelectedFieldSessionMustOwnItsOwnCapture()throws Exception{
        String owner=src("measurement/MeasurementSessionRegistry.java");
        String runner=src("measurement/FieldCycleController.java");
        String recorder=src("measurement/TelemetryRecorder.java");
        assertTrue(owner.contains("requestFieldCapture(String fieldSessionId)"));
        assertTrue(owner.contains("FIELD_OWNER_SESSION.set(owner)"));
        assertTrue(owner.contains("fieldCaptureOwnerSessionId()"));
        assertTrue(runner.contains("requestFieldCapture(linked.fieldSessionId)"));
        assertTrue(runner.contains("linked.fieldSessionId.equals("));
        assertTrue(recorder.contains("String owner=MeasurementSessionRegistry.fieldCaptureOwnerSessionId()"));
        assertTrue(recorder.contains("fieldSessions.linkMeasurement(owner,sessionId)"));
        assertFalse(recorder.contains("fieldSessions.activeFieldSession()"));
    }
    @Test public void reportableFieldFitUsesSixtySecondDistinctScanSequence()throws Exception{
        String analyzer=src("chemcore/CompletedSessionChemCoreAnalyzer.java");
        String recorder=src("measurement/TelemetryRecorder.java");
        assertTrue(analyzer.contains("observedRatiosAcrossFieldSamples(db,id)"));
        assertTrue(analyzer.contains("SELECT mq_payload FROM telemetry_samples WHERE session_id=? ORDER BY t_ms,id"));
        assertTrue(analyzer.contains("putIfAbsent(scan,ratio)"));
        assertTrue(analyzer.contains("values.size()<10"));
        assertTrue(analyzer.contains("Collections.sort(values)"));
        assertTrue(analyzer.contains("60S_MEDIAN_DISTINCT_HARDWARE_SCAN_CYCLE"));
        assertTrue(recorder.contains("duration,fieldCaptureSession)"));
        assertTrue(analyzer.contains("if(ratios.isEmpty())return new Outcome(-1,0,0,null)"));
    }
    @Test public void fieldAndSiteDecisionsPersistSameIndependentScientificContext()throws Exception{
        String saved=src("chemcore/ChemCoreDecisionRepository.java");
        assertTrue(saved.contains("TankFingerprintScreening.KEY"));
        assertTrue(saved.contains("TankExposureReferenceCatalog.forAnalysis(db,payload)"));
        assertTrue(saved.contains("TankScreeningExposureWarning.evaluate(payload)"));
        assertTrue(saved.contains("TankPpmEstimate.from(payload)"));
        assertTrue(saved.contains("GasPpmExposureAssessment.evaluate(db,payload,tMs)"));
        assertTrue(saved.contains("screening_ppm_not_a_certified_measurement"));
    }
    @Test public void activityDestructionMustCancelArmingBeforeReleasingPhysicalReservation()throws Exception{
        String c=src("measurement/FieldCycleController.java");
        assertTrue(c.contains("ACTIVITY_DESTROYED_DURING_ARMING"));
        assertTrue(c.contains("ACTIVITY_DESTROYED_DURING_RETRY_ARMING"));
        assertTrue(c.contains("MeasurementSessionRegistry.endFieldOperation()"));
        assertTrue(c.contains("failWorker(cid"));
        String recorder=src("measurement/TelemetryRecorder.java");
        String main=src("MainActivity.java");
        assertTrue(recorder.contains("abortInterruptedFieldCapture(String reason)"));
        assertTrue(recorder.contains("metadata.put(\"terminal_state\",\"error\")"));
        assertTrue(recorder.contains("MeasurementSessionRegistry.resetActive(interrupted)"));
        assertTrue(main.contains("telemetryRecorder.abortInterruptedFieldCapture(\"ACTIVITY_DESTROYED\")"));
        assertTrue(main.indexOf("workers.awaitTermination(5000,TimeUnit.MILLISECONDS)")<
                   main.indexOf("telemetryRecorder.abortInterruptedFieldCapture"));
    }
}
