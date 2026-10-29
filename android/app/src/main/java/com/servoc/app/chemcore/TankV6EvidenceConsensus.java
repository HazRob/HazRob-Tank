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
 * asserts a gas identity or creates reportable ppm. V6 inclusive policy retains
 * every mathematically valid observation, even with incomplete source metadata,
 * low match, one sensor, out-of-domain source, or conflicting research curves.
 */
public final class TankV6EvidenceConsensus {
    public static final String SCHEMA="tank-v6-evidence-consensus/2";
    public static final String SCORE_SEMANTICS="relative_pattern_match_index_not_probability";
    public static final double SOURCE_CONFLICT_DELTA=0.35; // advisory only
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
            // Only corrupt or mathematically undefined normalized numeric fields
            // are dropped. Missing source labels and out-of-domain evidence remain.
            return candidateUid>0 &&
                Double.isFinite(matchIndex) && matchIndex>=0 && matchIndex<=1 &&
                Double.isFinite(qualityWeight) && qualityWeight>0 && qualityWeight<=1;
        }
        private Evidence normalized() {
            return new Evidence(candidateUid,
                sensorInstance.isEmpty()?"sensor_unattributed":sensorInstance,
                originRootId.isEmpty()?"source_unattributed":originRootId,
                modelId.isEmpty()?"model_unattributed":modelId,
                matchIndex,qualityWeight,inSourceDomain);
        }
    }

    public static final class CandidateResult {
        public final long candidateUid;
        public final double matchIndex;
        public final int physicalSensorBreadth, independentSourceRoots;
        public final boolean conflictingSourceModels, hasOutsideSourceDomain, missingProvenance;
        public final List<String> sourceRoots, physicalSensors;
        private CandidateResult(long uid,double match,int sensors,int roots,boolean conflict,
                                boolean outside,boolean unattributed,
                                Set<String> lineage,Set<String> sensorIds){
            candidateUid=uid;matchIndex=match;physicalSensorBreadth=sensors;
            independentSourceRoots=roots;conflictingSourceModels=conflict;
            hasOutsideSourceDomain=outside;missingProvenance=unattributed;
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
     * then aggregate at most one term per physical sensor. Source disagreements,
     * absent source identifiers, single-channel evidence and low scores are visible
     * diagnostics, never exclusion rules. Score is not a calibrated probability.
     */
    public static Decision fuse(List<Evidence> observations){
        Map<Long,Map<String,Map<String,Evidence>>> byCandidate=new LinkedHashMap<>();
        int rejected=0,accepted=0;
        if(observations!=null)for(Evidence e:observations){
            if(e==null || !e.eligible()){rejected++;continue;}
            accepted++;
            Evidence observed=e.normalized();
            Map<String,Map<String,Evidence>> sensors=byCandidate.computeIfAbsent(observed.candidateUid,k->new LinkedHashMap<>());
            Map<String,Evidence> roots=sensors.computeIfAbsent(observed.sensorInstance,k->new LinkedHashMap<>());
            Evidence prior=roots.get(observed.originRootId);
            if(prior==null||preferred(observed,prior))roots.put(observed.originRootId,observed);
        }
        List<CandidateResult> candidates=new ArrayList<>();
        for(Map.Entry<Long,Map<String,Map<String,Evidence>>> candidate:byCandidate.entrySet()){
            double weightedSum=0,totalWeight=0;
            Set<String> lineage=new LinkedHashSet<>(),sensors=new LinkedHashSet<>();
            boolean conflict=false,outside=false,unattributed=false;
            for(Map.Entry<String,Map<String,Evidence>> channel:candidate.getValue().entrySet()){
                Evidence winner=null;double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;
                for(Evidence option:channel.getValue().values()){
                    if(winner==null||preferred(option,winner))winner=option;
                    min=Math.min(min,option.matchIndex);max=Math.max(max,option.matchIndex);
                    if(!option.inSourceDomain)outside=true;
                    if("source_unattributed".equals(option.originRootId)||
                       "sensor_unattributed".equals(option.sensorInstance)||
                       "model_unattributed".equals(option.modelId))unattributed=true;
                }
                if(winner==null)continue;
                if(channel.getValue().size()>1 && max-min>SOURCE_CONFLICT_DELTA)conflict=true;
                sensors.add(channel.getKey());lineage.add(winner.originRootId);
                weightedSum+=winner.matchIndex*winner.qualityWeight;
                totalWeight+=winner.qualityWeight;
            }
            if(totalWeight>0)candidates.add(new CandidateResult(candidate.getKey(),
                    weightedSum/totalWeight,sensors.size(),lineage.size(),
                    conflict,outside,unattributed,lineage,sensors));
        }
        candidates.sort(Comparator.comparingDouble((CandidateResult c)->c.matchIndex).reversed()
                .thenComparingLong(c->c.candidateUid));
        if(candidates.isEmpty())return new Decision(Outcome.INSUFFICIENT_EVIDENCE,
                "NO_SOURCE_QUALIFIED_EVIDENCE",candidates,rejected,accepted);
        // Do not suppress findings because of a research policy threshold.
        // Every finite normalized observation remains visible in candidate ranking.
        return new Decision(Outcome.RESEARCH_SCREENING,
            "ALL_NUMERICALLY_VALID_EVIDENCE_INCLUDED_WITH_ADVISORIES_NOT_CALIBRATED_IDENTITY",
            candidates,rejected,accepted);
    }
}
