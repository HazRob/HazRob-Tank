package com.servoc.app.field;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Source-level regression; not a substitute for real Android field/site PDF and Excel tests. */
public final class TankV5ScientificLineageAndParticlesContractTest {
    private static String src(String f)throws Exception{
        return new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/"+f)),
                StandardCharsets.UTF_8);
    }
    @Test public void optionalParticlePayloadIsStoredExactlyWhenPresent()throws Exception{
        String capture=src("measurement/TelemetryRecorder.java");
        assertTrue(capture.contains("JSONObject particles=snapshot.optJSONObject(\"particles\")"));
        assertTrue(capture.contains("state.put(\"particles\",new JSONObject(particles.toString()))"));
        String approve=src("field/FieldCycleRepository.java");
        assertTrue(approve.contains("JSONObject pm=aux.optJSONObject(\"particles\")"));
        assertTrue(approve.contains("pm.optBoolean(\"ok\",false)"));
        assertTrue(approve.contains("particleN>=10"));
        String controller=src("measurement/FieldCycleController.java");
        assertTrue(controller.contains("pm.optBoolean(\"installed\",false)"));
    }

    @Test public void approvedCycleEvidenceHashBindsBothRawAndChemCoreDecisions()throws Exception{
        String repo=src("field/FieldCycleRepository.java");
        assertTrue(repo.contains("SELECT t_ms,env_payload,mq_payload,motion_payload FROM telemetry_samples"));
        assertTrue(repo.contains("SELECT id,t_ms,payload FROM chemcore_decisions"));
        assertTrue(repo.contains("add(d,nz(decisions.getString(2)))"));
        assertTrue(repo.contains("hash.equalsIgnoreCase(evidenceDigest(w,ms,device,metadata,payload))"));
        assertTrue(repo.contains("hasRequiredChemCoreDecision(ms,c.getInt(9)!=0)"));
        assertTrue(repo.contains("public boolean verifyApprovedEvidence(Cycle cycle)"));
        assertTrue(repo.contains("cycle.evidenceSha.equalsIgnoreCase("));
    }
    @Test public void approvedOnlyReportsPreserveRejectedAttemptLedger()throws Exception{
        String repo=src("field/FieldSessionRepository.java");
        assertTrue(repo.contains("c.state IN ('APPROVED','LEGACY_APPROVED')"));
        String fieldReport=src("report/FieldSessionReportExporter.java");
        String siteReport=src("report/SiteSummaryReportExporter.java");
        assertTrue(fieldReport.contains("List<FieldCycleRepository.Cycle> cycleLedger=cycles.forSession(fs)"));
        assertTrue(fieldReport.contains("if(!cycles.verifyApprovedEvidence(cycle))"));
        assertTrue(siteReport.contains("for(FieldCycleRepository.Cycle cycle:b.cycleLedger)"));
    }
}
