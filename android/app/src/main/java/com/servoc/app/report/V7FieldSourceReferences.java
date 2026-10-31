package com.servoc.app.report;

import com.servoc.app.chemical.GhsPictograms;
import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.exposure.ExposureRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Source-only, legally conservative GHS and occupational reference adapter.
 * GHS refers to the candidate MATERIAL's independently documented hazard
 * classification; it never establishes that the Tank detected that chemical.
 *
 * No numeric ppm/limit ratio or occupational exceedance is calculated.
 * All independently sourced limits remain separate, including reference
 * duration, jurisdiction and revision.
 */
public final class V7FieldSourceReferences {
    public static final class Ref {
        public final V7FieldReportSourceSnapshot.Candidate candidate;
        public final ExposureRepository.Limit limit;
        Ref(V7FieldReportSourceSnapshot.Candidate c,ExposureRepository.Limit l){
            candidate=c;limit=l;
        }
    }
    private final IdentityHashMap<V7FieldReportSourceSnapshot.Candidate,List<String>>
        pictograms=new IdentityHashMap<>();
    public final List<Ref> limits;
    public final String verificationState;
    private V7FieldSourceReferences(String status,List<Ref> rs){
        verificationState=status;
        limits=Collections.unmodifiableList(rs);
    }
    public static V7FieldSourceReferences noData(){
        return new V7FieldSourceReferences("NO SOURCE FIXTURE",new ArrayList<Ref>());
    }
    private static long uid(String candidate){
        if(candidate==null||candidate.trim().isEmpty())return -1;
        try{
            long v=Long.parseLong(candidate.trim());
            return v>0?v:-1;
        }catch(NumberFormatException ex){return -1;}
    }
    public static V7FieldSourceReferences from(
            ExperimentsDb db,V7FieldReportSourceSnapshot s){
        if(db==null||s==null)throw new IllegalArgumentException("Verified source required");
        List<Ref> all=new ArrayList<>();
        V7FieldSourceReferences display=
            new V7FieldSourceReferences("QUALIFIED MATERIAL REFERENCES ONLY",all);
        Map<Long,List<String>> verifiedGhs=GhsPictograms.allQualified(db);
        ExposureRepository exposure=new ExposureRepository(db);
        for(V7FieldReportSourceSnapshot.Candidate c:s.rawCandidates){
            long chemicalUid=c.qualifiedChemicalUid;
            List<String> codes=verifiedGhs.get(chemicalUid);
            display.pictograms.put(c,codes==null?Collections.<String>emptyList():
                Collections.unmodifiableList(new ArrayList<>(codes)));
            List<ExposureRepository.Limit> refs=chemicalUid>0?
                exposure.forChemical(chemicalUid):
                "-".equals(c.cas)?Collections.<ExposureRepository.Limit>emptyList():
                    exposure.byCas(c.cas);
            for(ExposureRepository.Limit reference:refs)
                if(reference!=null)all.add(new Ref(c,reference));
        }
        // Reference data are only read/ordered, not interpreted as an alarm.
        return display;
    }
    public List<String> pictograms(
            V7FieldReportSourceSnapshot.Candidate candidate){
        List<String> found=pictograms.get(candidate);
        return found==null?Collections.<String>emptyList():found;
    }
}
