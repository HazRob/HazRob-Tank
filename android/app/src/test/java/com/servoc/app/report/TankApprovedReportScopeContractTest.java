package com.servoc.app.report;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static org.junit.Assert.*;

/** Protect against the first 100 draft captures starving approved reports. */
public final class TankApprovedReportScopeContractTest {
    @Test public void sqlFiltersApprovalsAndUserRoleBeforeLimit() throws Exception{
        String s=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/data/ExperimentQueries.java")),StandardCharsets.UTF_8);
        String fragment=s.substring(s.indexOf("public static List<Record> approvedFieldReports"),s.indexOf("/** Developer-only laboratory/reference"));
        int where=fragment.indexOf("WHERE m.operator_role='USER'");
        int approved=fragment.indexOf("fs.status='APPROVED'");
        int limit=fragment.indexOf("ORDER BY e.id DESC LIMIT");
        assertTrue(where>0&&approved>where&&limit>approved);
        assertTrue(fragment.contains("JOIN field_session_measurements"));
        assertTrue(fragment.contains("JOIN field_sessions"));
    }
    @Test public void sessionApprovalRequiresReadyTerminalCapture() throws Exception{
        String s=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/field/FieldSessionRepository.java")),StandardCharsets.UTF_8);
        assertTrue(s.contains("FieldSessionApprovalPolicy.completedSuccessfully"));
        assertTrue(s.contains("if(complete<1)return false"));
        assertTrue(s.contains("FieldCycleRepository.APPROVED.equals(cycleState)"));
        assertTrue(s.contains("FieldCycleRepository.SUPERSEDED.equals(cycleState)"));
        assertTrue(s.contains("SELECT 1 FROM telemetry_samples"));
    }
}
