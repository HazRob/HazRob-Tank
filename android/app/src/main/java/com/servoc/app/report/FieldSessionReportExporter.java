package com.servoc.app.report;

import com.servoc.app.chemical.ChemicalRepository;
import com.servoc.app.chemcore.ChemCoreDecisionRepository;
import com.servoc.app.data.ExperimentQueries;
import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.data.SessionSeries;
import com.servoc.app.field.FieldSessionRepository;
import com.servoc.app.field.FieldCycleRepository;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONObject;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/** Stable Field Session report facade. PDF is compact; XLSX remains the detailed auditable workbook. */
public final class FieldSessionReportExporter {
    private FieldSessionReportExporter(){}
    public static final int PAGE_WIDTH=842,PAGE_HEIGHT=595,MIN_PAGES=4,MAX_PAGES=5; // Analytical core only; V5 cycle appendices increase actual PDF pages.

    public static final class Bundle {
        public final String fieldSessionId;
        public final FieldSessionRepository.Site site;
        public final FieldSessionRepository.FieldSession fieldSession;
        public final List<String> measurementSessionIds;
        /** All approved/retried/deleted cycle ledger rows, in sequence order. */
        public final List<FieldCycleRepository.Cycle> cycleLedger;
        public final List<ExperimentQueries.Record> records;
        public final String primaryMeasurementSessionId;
        public final SessionSeries.Series primarySeries;
        public final JSONObject primaryDecision;
        /** Shared ChemCore -> SmartReport contract consumed by both PDF and XLSX paths. */
        public final SmartReportAnalyticalContract analyticalContract;
        Bundle(String fieldSessionId,FieldSessionRepository.Site site,FieldSessionRepository.FieldSession fieldSession,
               List<String> measurementSessionIds,List<FieldCycleRepository.Cycle> cycleLedger,List<ExperimentQueries.Record> records,String primaryMeasurementSessionId,
               SessionSeries.Series primarySeries,JSONObject primaryDecision,SmartReportAnalyticalContract analyticalContract){
            this.fieldSessionId=fieldSessionId;this.site=site;this.fieldSession=fieldSession;this.measurementSessionIds=measurementSessionIds;
            this.cycleLedger=java.util.Collections.unmodifiableList(new ArrayList<>(cycleLedger));this.records=records;this.primaryMeasurementSessionId=primaryMeasurementSessionId;this.primarySeries=primarySeries;this.primaryDecision=primaryDecision;
            this.analyticalContract=analyticalContract;
        }
    }

    public static Bundle resolve(ExperimentsDb db,String fieldSessionId){
        if(db==null)throw new IllegalArgumentException("db required");String fs=required(fieldSessionId);
        FieldSessionRepository repo=new FieldSessionRepository(db);if(!repo.isApproved(fs))throw new IllegalStateException("يجب اعتماد الجلسة قبل إصدار التقرير");FieldSessionRepository.Site site=null;FieldSessionRepository.FieldSession field=null;
        for(FieldSessionRepository.Site s:repo.sites()){for(FieldSessionRepository.FieldSession q:repo.sessionsForSite(s.id))if(fs.equals(q.id)){site=s;field=q;break;}if(field!=null)break;}
        if(field==null||site==null)throw new IllegalArgumentException("field session not found");
        FieldCycleRepository cycles=new FieldCycleRepository(db);
        List<FieldCycleRepository.Cycle> cycleLedger=cycles.forSession(fs);
        List<String> sessions=repo.measurementSessionIds(fs);
        // A report must never quietly omit a valid approved cycle, duplicate an
        // acquisition, or include an ID from another Field Session.
        Set<String> approvedIds=new HashSet<>();
        for(FieldCycleRepository.Cycle cycle:cycleLedger){
            if(!cycle.approved())continue;
            if(!fs.equals(cycle.fieldSessionId)||cycle.measurementId.isEmpty()||
                !approvedIds.add(cycle.measurementId))
                throw new IllegalStateException("Approved measurement ledger is inconsistent");
            if(!cycles.verifyApprovedEvidence(cycle))
                throw new IllegalStateException("Approved cycle evidence or ChemCore result changed: "+cycle.id);
        }
        if(approvedIds.isEmpty()||approvedIds.size()!=sessions.size()||
            !approvedIds.equals(new HashSet<>(sessions)))
            throw new IllegalStateException("Report integrity: approved cycles do not match session measurements");
        List<ExperimentQueries.Record> records=new ArrayList<>();
        for(String sid:sessions)records.addAll(ExperimentQueries.bySessionId(db,sid));
        ChemCoreDecisionRepository decisions=new ChemCoreDecisionRepository(db);

        // Validate every candidate measurement session and select the same strongest analytical session
        // used by the smart PDF scoring policy. XLSX therefore cannot silently bind to a different
        // "latest" session while the PDF displays a stronger earlier session.
        String primary="";SessionSeries.Series series=null;JSONObject decision=null;int bestScore=Integer.MIN_VALUE;
        for(String sid:sessions){
            if(sid==null||sid.trim().isEmpty())continue;
            String id=sid.trim();
            SessionSeries.Series candidateSeries=SessionSeries.byId(db,id);
            JSONObject candidateDecision=decisions.latestPayloadForSession(id);
            SmartReportAnalyticalContract.bind(candidateSeries,candidateDecision);
            int candidateRecords=ExperimentQueries.bySessionId(db,id).size();
            int score=SmartReportPrimarySelector.score(candidateSeries,candidateDecision,candidateRecords);
            if(primary.isEmpty()||score>=bestScore){
                bestScore=score;primary=id;series=candidateSeries;decision=candidateDecision;
            }
        }
        SmartReportAnalyticalContract contract=SmartReportAnalyticalContract.bind(series,decision);
        return new Bundle(fs,site,field,new ArrayList<>(sessions),cycleLedger,records,primary,series,decision,contract);
    }

    public static void writePdf(OutputStream out,ExperimentsDb db,String fieldSessionId,ChemicalRepository chemicals)throws Exception{
        // SmartFieldSessionPdfWriter resolves the bundle and therefore cannot bypass the shared contract guard.
        SmartFieldSessionPdfWriter.writePdf(out,db,fieldSessionId,chemicals);
    }
    public static void writeXlsx(OutputStream out,ExperimentsDb db,String fieldSessionId,ChemicalRepository chemicals)throws Exception{
        Bundle b=resolve(db,fieldSessionId);
        // A valid Field Session is exportable even before a derived experiment or
        // ChemCore decision has been saved. Missing analysis is shown as missing,
        // while independent raw telemetry, session identity and QA remain auditable.
        // Do not silently deny Excel to an operator because chemistry is incomplete.
        // XLSX receives the same already-validated Bundle/contract used by the field-report system.
        SmartFieldSessionXlsxWriter.write(out,db,b,chemicals);
    }
    private static String required(String s){String v=s==null?"":s.trim();if(v.isEmpty())throw new IllegalArgumentException("field session required");return v;}
}
