package com.servoc.app.field;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Source contract only: does not simulate hardware or certify a 120-second run. */
public final class TankV5DocumentedCyclePipelineContractTest {
    private static String src(String file)throws Exception{
        return new String(Files.readAllBytes(Paths.get(
            "src/main/java/com/servoc/app/"+file)),StandardCharsets.UTF_8);
    }
    @Test public void operatorOwnsOneFieldCaptureAndItsTimer()throws Exception{
        String registry=src("measurement/MeasurementSessionRegistry.java");
        String controller=src("measurement/FieldCycleController.java");
        assertTrue(registry.contains("requestFieldCapture()"));
        assertTrue(registry.contains("requestFieldStop(String reason)"));
        assertTrue(registry.contains("FIELD_CAPTURE.compareAndSet(false,true)"));
        assertTrue(controller.contains("WARMUP_MS=60000L,SAMPLING_MS=60000L"));
        assertTrue(controller.contains("host.robotApi().mqPower(true)"));
        assertTrue(controller.contains("!relayOn(verified)"));
        assertTrue(controller.contains("repo.samplingStarted(cycleId,SystemClock.elapsedRealtime())"));
        assertTrue(controller.contains("linked.measurementId.equals(active)"));
        assertTrue(controller.contains("repo.complete(cycleId,deviceId)"));
        assertTrue(controller.contains("stopWithFailure("));
        assertTrue(controller.contains("host.robotApi().mqPower(false)"));
        assertFalse(controller.contains("Thread.sleep("));
        assertFalse(controller.contains("setConfig("));
    }
    @Test public void realPollsArePersistedAndNeverAssumeFirmwareReady()throws Exception{
        String recorder=src("measurement/TelemetryRecorder.java");
        String policy=src("field/FieldSessionApprovalPolicy.java");
        assertTrue(recorder.contains("fieldForced=MeasurementSessionRegistry.fieldCaptureRequested()"));
        assertTrue(recorder.contains("field_60s_cycle"));
        assertTrue(recorder.contains("instant_mq_cycle"));
        assertTrue(recorder.contains("fieldSessions.linkMeasurement(active.id,sessionId)"));
        assertTrue(recorder.contains("if(active==null||!fieldSessions.linkMeasurement"));
        assertTrue(recorder.contains("meta.put(\"field_60s_capture\",fieldCaptureSession)"));
        assertTrue(policy.contains("isDocumentedFieldCapture(analyticsJson)"));
        assertTrue(policy.contains("fieldDurationAndSamplesValid(analyticsJson)"));
        assertTrue(policy.contains("duration>=60000L && count>=10"));
    }
    @Test public void fieldApprovalsAndReportsExcludeSupersededAttempts()throws Exception{
        String cycles=src("field/FieldCycleRepository.java");
        String repo=src("field/FieldSessionRepository.java");
        String tab=src("ui/FieldSessionsTab.java");
        String xlsx=src("report/SmartFieldSessionXlsxWriter.java");
        assertTrue(cycles.contains("gasScanCycles.size()>=10"));
        assertTrue(cycles.contains("env.optLong(\"last_ok_age_ms\",Long.MAX_VALUE)<=5000L"));
        assertTrue(cycles.contains("evidenceDigest(w,ms,deviceId"));
        assertTrue(cycles.contains("ORDER BY t_ms,id"));
        assertTrue(repo.contains("FieldCycleRepository.SUPERSEDED.equals(cycleState)"));
        assertTrue(repo.contains("AND c.state IN ('APPROVED','LEGACY_APPROVED')"));
        assertTrue(repo.contains("if(complete<1)return false;"));
        assertTrue(tab.contains("gasCheck=measurementCheck(c,\"غازات\",true)"));
        assertTrue(tab.contains("particleCheck=measurementCheck(c,\"جزيئات\",false)"));
        assertTrue(tab.contains("physicalCheck=measurementCheck(c,\"فيزيائية\",true)"));
        assertTrue(tab.contains("cycleDriver.begin(id,opts)"));
        assertTrue(tab.contains("cycleDriver.retry(latestCycleId)"));
        assertTrue(tab.contains("cycleDriver.approve(latestCycleId)"));
        assertTrue(xlsx.contains("new SmartXlsxPackage.Sheet(\"دورات القياس\",cycleLedger(db,b))"));
        assertTrue(xlsx.contains("c.evidenceSha"));
    }
}
