package com.servoc.app.report;

import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.chemcore.ChemCoreDecisionRepository;
import com.servoc.app.field.FieldCycleRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable, source-traceable R03 field-session PDF input contract.
 * Resolves only the approved archived field session and its independent
 * completed measurement sessions; does not calculate candidate ranking,
 * concentration, exposure status, sensor compensation or food analysis.
 */
public final class V7FieldReportSourceSnapshot {
    public static final class Cycle {
        public final int sequence,attempt;
        public final String state,startedUtc,approvedUtc,measurementId,evidenceSha256;
        public final boolean approved,gas,physical,particles;
        private Cycle(FieldCycleRepository.Cycle c) {
            this.sequence=c.sequence;this.attempt=c.attempt;
            state=blank(c.state);startedUtc=blank(c.started);
            approvedUtc=blank(c.approved);
            measurementId=blank(c.measurementId);
            evidenceSha256=blank(c.evidenceSha);
            approved=c.approved();
            gas=c.modes!=null&&c.modes.gas;
            physical=c.modes!=null&&c.modes.physical;
            particles=c.modes!=null&&c.modes.particles;
        }
    }
    public static final class Candidate {
        public final int sourceIndex, measurementOrdinal;
        public final String measurementSessionId,label,cas,uid,evidenceTier,sourceJson;
        private Candidate(int index,int ordinal,String sessionId,JSONObject entry){
            sourceIndex=index;
            measurementOrdinal=ordinal;
            measurementSessionId=blank(sessionId);
            label=blank(entry.optString("label",entry.optString("name","")));
            cas=blank(entry.optString("cas",""));
            uid=blank(entry.optString("uid",entry.optString("id","")));
            evidenceTier=blank(entry.optString("evidence_tier",""));
            sourceJson=entry.toString();
        }
    }
    public final String siteId,siteName,fieldSessionId,fieldSessionName,
                        approvalStatus,primaryMeasurementSessionId;
    public final List<String> measurementSessionIds;
    public final List<Cycle> auditCycles;
    public final List<Candidate> rawCandidates;
    public final String primarySourceDecisionJson;
    public final boolean approvedGas,approvedPhysical,approvedParticles;
    public final int approvedCycles;
    public final String contractEvidence;

    private V7FieldReportSourceSnapshot(ExperimentsDb db,FieldSessionReportExporter.Bundle b) {
        if(b==null||b.analyticalContract==null)
            throw new IllegalArgumentException("Archived approved contract required");
        siteId=blank(b.site.id);siteName=blank(b.site.name);
        fieldSessionId=blank(b.fieldSession.id);
        fieldSessionName=blank(b.fieldSession.name);
        approvalStatus=blank(b.fieldSession.status);
        primaryMeasurementSessionId=blank(b.primaryMeasurementSessionId);
        List<String> ids=new ArrayList<>();
        for(String id:b.measurementSessionIds)ids.add(blank(id));
        measurementSessionIds=Collections.unmodifiableList(ids);
        List<Cycle> audits=new ArrayList<>();
        int app=0;boolean g=false,p=false,f=false;
        for(FieldCycleRepository.Cycle c:b.cycleLedger){
            Cycle current=new Cycle(c);
            audits.add(current);
            if(current.approved){
                app++;
                g|=current.gas;p|=current.particles;f|=current.physical;
            }
        }
        auditCycles=Collections.unmodifiableList(audits);
        approvedCycles=app;approvedGas=g;approvedParticles=p;approvedPhysical=f;
        JSONObject decision=b.analyticalContract.decision();
        primarySourceDecisionJson=decision==null?"{}":decision.toString();
        List<Candidate> all=new ArrayList<>();
        // Preserve candidates from ALL approved archived measurement sessions,
        // never just the single best-scoring primary session. Do not fuse
        // their ranks or derive chemical identity in the PDF data contract.
        ChemCoreDecisionRepository decisions=new ChemCoreDecisionRepository(db);
        for(int ord=0;ord<measurementSessionIds.size();ord++){
            String sid=measurementSessionIds.get(ord);
            JSONObject payload=sid.equals(primaryMeasurementSessionId)?
                decision:decisions.latestPayloadForSession(sid);
            JSONArray candidates=payload==null?null:payload.optJSONArray("candidates");
            if(candidates==null)continue;
            for(int i=0;i<candidates.length();i++){
                JSONObject entry=candidates.optJSONObject(i);
                if(entry!=null)all.add(new Candidate(i,ord+1,sid,entry));
            }
        }
        rawCandidates=Collections.unmodifiableList(all);
        contractEvidence="ARCHIVED_APPROVED_FIELD_SESSION_ONLY; RAW_SOURCE_UNCHANGED";
    }
    private static String blank(String s){
        return s==null||s.trim().isEmpty()?"-":s;
    }
    public static V7FieldReportSourceSnapshot resolve(ExperimentsDb db,String fieldId){
        FieldSessionReportExporter.Bundle b=FieldSessionReportExporter.resolve(db,fieldId);
        return new V7FieldReportSourceSnapshot(db,b);
    }
}
