package com.servoc.app.report;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Source contract only; PDF rendering/SAF and database upgrade require instrumentation tests. */
public final class TankV5SessionSiteCycleReportContractTest {
    private static String src(String path)throws Exception {
        return new String(Files.readAllBytes(Paths.get(
            "src/main/java/com/servoc/app/"+path)), StandardCharsets.UTF_8);
    }

    @Test public void onlyApprovedIndependentCyclesReachSharedSessionReportBundle()throws Exception {
        String source=src("field/FieldSessionRepository.java");
        String bundle=src("report/FieldSessionReportExporter.java");
        assertTrue(source.contains("AND c.state IN ('APPROVED','LEGACY_APPROVED') ORDER BY c.sequence_no"));
        assertTrue(bundle.contains("List<FieldCycleRepository.Cycle> cycleLedger=cycles.forSession(fs)"));
        assertTrue(bundle.contains("if(!cycle.approved())continue"));
        assertTrue(bundle.contains("approvedIds.equals(new HashSet<>(sessions))"));
        assertTrue(bundle.contains("Report integrity: approved cycles do not match session measurements"));
        assertTrue(bundle.contains("new Bundle(fs,site,field,new ArrayList<>(sessions),cycleLedger"));
    }

    @Test public void bothSessionFormatsEnumerateEveryCycleIncludingFailedAuditRows()throws Exception {
        String pdf=src("report/SmartFieldSessionPdfWriter.java");
        String xlsx=src("report/SmartFieldSessionXlsxWriter.java");
        assertTrue(pdf.contains("b.cycleLedger.size()+ROWS_PER_CYCLE_PAGE-1"));
        assertTrue(pdf.contains("cycleLedgerPage(p.getCanvas(),b,chunk)"));
        assertTrue(pdf.contains("cycleLedgerPage(Canvas c,FieldSessionReportExporter.Bundle b,int chunk)"));
        assertTrue(pdf.contains("v.approved()?"));
        assertTrue(xlsx.contains("new SmartXlsxPackage.Sheet(\"دورات القياس\",cycleLedger(db,b))"));
        assertTrue(xlsx.contains("for(FieldCycleRepository.Cycle c:b.cycleLedger)"));
        assertTrue(xlsx.contains("c.evidenceSha"));
        assertTrue(xlsx.contains("c.failReason"));
    }

    @Test public void siteIncludesApprovedSessionsAndAllCycleRowsWithoutPpmPooling()throws Exception {
        String site=src("report/SiteSummaryReportExporter.java");
        String xlsx=src("report/SmartSiteXlsxWriter.java");
        assertTrue(site.contains("repo.approvedSessionsForSite(id)"));
        assertTrue(site.contains("FieldSessionReportExporter.resolve(db,fs.id)"));
        assertTrue(site.contains("for(FieldCycleRepository.Cycle cycle:b.cycleLedger)"));
        assertTrue(site.contains("cycleMatrix(p.getCanvas(),b,chunk)"));
        assertTrue(site.contains("int total=3+matrixPages+cyclePages"));
        assertTrue(site.contains("screening_ppm_estimate"));
        assertTrue(site.contains("ppm/EST"));
        assertTrue(site.contains("~ppm = تقدير بحثي"));

        assertTrue(site.contains("for(CycleSummary entry:session.cycles)"));
        assertTrue(xlsx.contains("new SmartXlsxPackage.Sheet(\"دورات الموقع\",allFieldCycles(b))"));
        assertTrue(xlsx.contains("for(SiteSummaryReportExporter.SessionSummary session:b.sessions)"));
        assertTrue(xlsx.contains("for(SiteSummaryReportExporter.CycleSummary detail:session.cycles)"));
        assertTrue(xlsx.contains("No pooled PPM and no average PPM across sessions"));
        assertFalse(xlsx.contains("averagePpmAcrossSessions"));
    }

    @Test public void scientificRecurrenceUsesAllApprovedCyclesOncePerFieldSession()throws Exception {
        String planner=src("report/SiteReportIntelligence.java");
        assertTrue(planner.contains("for(SiteSummaryReportExporter.CycleSummary cycle:s.cycles)"));
        assertTrue(planner.contains("if(!cycle.cycle.approved()||!cycle.hasAnalysis)continue"));
        assertTrue(planner.contains("Map<Integer,Boolean> supported=new HashMap<>()"));
        assertTrue(planner.contains("if(Boolean.TRUE.equals(supported.get(index)))"));
        assertTrue(planner.contains("acc.supported++"));
        assertTrue(planner.contains("if(s.critical())critical++"));
    }
}
