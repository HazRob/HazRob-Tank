package com.servoc.app.field;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/**
 * V8 source-level protection for V7 repeated FAILED cycles.
 * Device timing, SQLite persistence, and live MQ samples require bench tests.
 */
public final class TankV8FieldCycleFailureDiagnosticsTest {
    private static String source(String name)throws Exception{
        return new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/"+name)),
                StandardCharsets.UTF_8);
    }

    @Test public void failedSessionsShowPersistedReasonNotOnlyFailedCode()throws Exception{
        String ui=source("ui/FieldSessionsTab.java");
        String repo=source("field/FieldCycleRepository.java");
        assertTrue(ui.contains("سبب الفشل المحفوظ: "));
        assertTrue(ui.contains("mostRecent.failReason"));
        assertTrue(repo.contains("v.put(\"fail_reason\",message)"));
        assertTrue(repo.contains("public String completionFailureReason("));
        assertTrue(repo.contains("INSUFFICIENT_MEASURED_EVIDENCE:rows="));
        assertTrue(repo.contains("distinct_mq_scan_cycles="));
        assertTrue(repo.contains("valid_environment_samples="));
        assertTrue(repo.contains("CHEMCORE_DECISION_NOT_PERSISTED_FOR_CAPTURE"));
        assertTrue(repo.contains("FIELD_CAPTURE_DURATION_SAMPLES_OR_TERMINAL_STATE_INVALID"));
    }

    @Test public void noSyntheticDataAndEvidenceApprovalRemainsStrict()throws Exception{
        String repo=source("field/FieldCycleRepository.java");
        String controller=source("measurement/FieldCycleController.java");
        assertTrue(repo.contains("gasScanCycles.size()>=10"));
        assertTrue(repo.contains("physicalN>=10"));
        assertTrue(repo.contains("FieldSessionApprovalPolicy.completedSuccessfully("));
        assertTrue(repo.contains("elapsedRealtime()-c.getLong(8)<60000L"));
        assertTrue(repo.contains("hasRequiredChemCoreDecision("));
        assertTrue(controller.contains("WARMUP_MS=60000L,SAMPLING_MS=60000L"));
        assertTrue(controller.contains("repo.completionFailureReason(cycleId,deviceId)"));
        assertTrue(controller.contains("now-relayVerifiedAtMs<2500L"));
        assertTrue(controller.contains("STALE_MS=6000L"));
        assertTrue(controller.contains("if(current==null){"));
        assertTrue(controller.contains("main.postDelayed(tick,500)"));
        assertTrue(controller.contains("UNKNOWN relay state"));
    }

    @Test public void missingChemCoreRatiosBecomeExplicitNonQuantitativeDecision()throws Exception{
        String analyzer=source("chemcore/CompletedSessionChemCoreAnalyzer.java");
        String recorder=source("measurement/TelemetryRecorder.java");
        assertTrue(analyzer.contains("if(ratios.isEmpty())"));
        assertTrue(analyzer.contains("ChemCoreDecision.Outcome.INSUFFICIENT_EVIDENCE"));
        assertTrue(analyzer.contains("ChemCoreDecision.EvidenceTier.M0_CATALOG_ONLY"));
        assertTrue(analyzer.contains("NO_ACCEPTED_DEVICE_R0_RATIO_FROM_DISTINCT_SCAN_CYCLES"));
        assertTrue(analyzer.contains("no_single_terminal_snapshot_fallback"));
        assertTrue(analyzer.contains("decisions.save(Math.max(0,tMs),unavailable)"));
        assertTrue(recorder.contains("field_chemcore_error_"));
        assertTrue(recorder.contains("android.util.Log.e(\"TankFieldCycle\""));
        assertTrue(repoHasSeparateEvidenceGate());
    }

    private static boolean repoHasSeparateEvidenceGate()throws Exception{
        String repo=source("field/FieldCycleRepository.java");
        return repo.contains("!hasMeasuredEvidence(w,ms,") &&
               repo.contains("!hasRequiredChemCoreDecision(ms,");
    }
}
