package com.servoc.app.chemcore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Research-only, provenance-aware multi-candidate evidence consensus (V6 P08 prototype).
 *
 * An input score is an explicit [0,1] diagnostic MATCH, not an empirical probability.
 * Evidence from the same physical sensor is counted once per candidate; distinct source
 * curves sharing a sensor may introduce a conflict, but never an extra measurement.
 * Source lineage groups and sensor breadth are separately reported. This class never
 * asserts a gas identity or creates reportable ppm. Current acceptance thresholds
 * are conservative engineering screens, NOT validated sensitivity.
 */
public final class TankV6EvidenceConsensus {
    public static final String SCHEMA="tank-v6-evidence-consensus/1";
    public static final String SCORE_SEMANTICS="relative_pattern_match_index_not_probability";
    public static final double MIN_MATCH=0.55, MIN_MARGIN=0.10, SOURCE_CONFLICT_DELTA=0.35;
    public static final int MIN_SENSOR_BREADTH=2;
    private TankV6EvidenceConsensus(){}

    public enum Outcome { INSUFFICIENT_EVIDENCE, SOURCE_CONFLICT, AMBIGUOUS,
                          UNKNOWN_OR_OUTSIDE_RESEARCH_DOMAIN, RESEARCH_SCREENING }

    public static final class Evidence {
        public final long candidateUid;
        public final String sensorInstance, originRootId, modelId;
        public final double matchIndex, qualityWeight;
        public final boolean inSourceDomain;
        public Evidence(long candidate, String sensor, String root, String model,
                        double match, double quality, boolean inDomain) {
            candidateUid=candidate; sensorInstance=norm(sensor); originRootId=norm(root);
            modelId=norm(model); matchIndex=match; qualityWeight=quality;
            inSourceDomain=inDomain;
        }
        private boolean eligible() {
            return candidateUid>0 && !sensorInstance.isEmpty() && !originRootId.isEmpty()
                && !modelId.isEmpty() && inSourceDomain
                && Double.isFinite(matchIndex) && matchIndex>=0 && matchIndex<=1
                && Double.isFinite(qualityWeight) && qualityWeight>0 && qualityWeight<=1;
        }
    }

    public static final class CandidateResult {
        public final long candidateUid;
        public final double matchIndex;
        public final int physicalSensorBreadth, independentSourceRoots;
        public final boolean conflictingSourceModels;
        public final List<String> sourceRoots, physicalSensors;
        private CandidateResult(long uid,double match,int sensors,int roots,boolean conflict,
                                Set<String> lineage,Set<String> sensorIds){
            candidateUid=uid;matchIndex=match;physicalSensorBreadth=sensors;
            independentSourceRoots=roots;conflictingSourceModels=conflict;
            sourceRoots=Collections.unmodifiableList(new ArrayList<>(lineage));
            physicalSensors=Collections.unmodifiableList(new ArrayList<>(sensorIds));
        }
    }

    public static final class Decision {
        public final Outcome outcome;
        public final String reason,scoreSemantics;
        public final List<CandidateResult> rankedCandidates;
        public final int rejectedObservations, acceptedObservations, uniqueCandidateCount;
        public final boolean deviceValidated, reportablePpm;
        private Decision(Outcome status,String reason,List<CandidateResult> rankings,int rejected,int accepted){
            this.outcome=status;this.reason=reason;scoreSemantics=SCORE_SEMANTICS;
            rankedCandidates=Collections.unmodifiableList(new ArrayList<>(rankings));
            rejectedObservations=rejected;acceptedObservations=accepted;
            uniqueCandidateCount=rankings.size();deviceValidated=false;reportablePpm=false;
        }
    }

    private static String norm(String s){return s==null?"":s.trim().toLowerCase(Locale.ROOT);}

    /** Deterministic comparison between models of the same physical sensor. */
    private static boolean preferred(Evidence proposed,Evidence existing){
        double a=proposed.qualityWeight*proposed.matchIndex;
        double b=existing.qualityWeight*existing.matchIndex;
        if(a!=b)return a>b;
        if(proposed.qualityWeight!=existing.qualityWeight)return proposed.qualityWeight>existing.qualityWeight;
        int root=proposed.originRootId.compareTo(existing.originRootId);
        if(root!=0)return root<0;
        return proposed.modelId.compareTo(existing.modelId)<0;
    }

    /**
     * Deduplicate (candidate, physical sensor, scientific root) model variants,
     * then aggregate at most one term per physical sensor. Source disagreements
     * force abstention of the top candidate. Score is NOT a calibrated probability.
     */
    public static Decision fuse(List<Evidence> observations){
        Map<Long,Map<String,Map<String,Evidence>>> byCandidate=new LinkedHashMap<>();
        int rejected=0,accepted=0;
        if(observations!=null)for(Evidence e:observations){
            if(e==null || !e.eligible()){rejected++;continue;}
            accepted++;
            Map<String,Map<String,Evidence>> sensors=byCandidate.computeIfAbsent(e.candidateUid,k->new LinkedHashMap<>());
            Map<String,Evidence> roots=sensors.computeIfAbsent(e.sensorInstance,k->new LinkedHashMap<>());
            Evidence prior=roots.get(e.originRootId);
            if(prior==null||preferred(e,prior))roots.put(e.originRootId,e);
        }
        List<CandidateResult> candidates=new ArrayList<>();
        for(Map.Entry<Long,Map<String,Map<String,Evidence>>> candidate:byCandidate.entrySet()){
            double weightedSum=0,totalWeight=0;
            Set<String> lineage=new LinkedHashSet<>(),sensors=new LinkedHashSet<>();
            boolean conflict=false;
            for(Map.Entry<String,Map<String,Evidence>> channel:candidate.getValue().entrySet()){
                Evidence winner=null;double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;
                for(Evidence option:channel.getValue().values()){
                    if(winner==null||preferred(option,winner))winner=option;
                    min=Math.min(min,option.matchIndex);max=Math.max(max,option.matchIndex);
                }
                if(winner==null)continue;
                if(channel.getValue().size()>1 && max-min>SOURCE_CONFLICT_DELTA)conflict=true;
                sensors.add(channel.getKey());lineage.add(winner.originRootId);
                weightedSum+=winner.matchIndex*winner.qualityWeight;
                totalWeight+=winner.qualityWeight;
            }
            if(totalWeight>0)candidates.add(new CandidateResult(candidate.getKey(),
                    weightedSum/totalWeight,sensors.size(),lineage.size(),conflict,lineage,sensors));
        }
        candidates.sort(Comparator.comparingDouble((CandidateResult c)->c.matchIndex).reversed()
                .thenComparingLong(c->c.candidateUid));
        if(candidates.isEmpty())return new Decision(Outcome.INSUFFICIENT_EVIDENCE,
                "NO_SOURCE_QUALIFIED_EVIDENCE",candidates,rejected,accepted);
        CandidateResult top=candidates.get(0);
        if(top.physicalSensorBreadth<MIN_SENSOR_BREADTH)
            return new Decision(Outcome.INSUFFICIENT_EVIDENCE,"LESS_THAN_TWO_PHYSICAL_SENSORS",candidates,rejected,accepted);
        if(top.conflictingSourceModels)
            return new Decision(Outcome.SOURCE_CONFLICT,"CONTRADICTORY_SOURCE_CURVES",candidates,rejected,accepted);
        if(top.matchIndex<MIN_MATCH)
            return new Decision(Outcome.UNKNOWN_OR_OUTSIDE_RESEARCH_DOMAIN,
                "ALL_CANDIDATES_BELOW_RESEARCH_MATCH_SCREEN",candidates,rejected,accepted);
        if(candidates.size()>1 && top.matchIndex-candidates.get(1).matchIndex<MIN_MARGIN)
            return new Decision(Outcome.AMBIGUOUS,"LOW_MARGIN_BETWEEN_TOP_CANDIDATES",candidates,rejected,accepted);
        return new Decision(Outcome.RESEARCH_SCREENING,"RESEARCH_ONLY_NOT_CALIBRATED_IDENTITY",candidates,rejected,accepted);
    }
}
