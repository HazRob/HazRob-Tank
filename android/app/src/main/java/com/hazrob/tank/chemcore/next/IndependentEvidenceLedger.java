package com.hazrob.tank.chemcore.next;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * P04.6: qualitative evidence graph, never an uncalibrated posterior.
 * Multiple curves, experts, channels or competing gas labels backed by the
 * same sourceRootId are ONE source family within one physical exposure.
 *
 * NOT_EVALUABLE is not negative evidence. Conflicting or multiple possible
 * identities from one source are AMBIGUOUS and not independently voted.
 */
public final class IndependentEvidenceLedger {
    private IndependentEvidenceLedger() {}
    public static final String VERSION="chemcore-next-evidence-ledger/0.1-g1";

    public static final class RootGroup {
        public final String sourceRootId, exposureUid;
        public final List<String> expertIds, sensorUids;
        public final List<Long> alternativeChemicalUids;
        public final CoreContracts.EvidencePolarity effectivePolarity;
        public final Long uniqueChemicalUid;
        public final boolean sharedHypothesisRoot, conflictingPolarity, independentlyValidatedG3;
        private RootGroup(String root,String exposure,Set<String> experts,Set<String> sensors,
                          Set<Long> chemicals,CoreContracts.EvidencePolarity p,
                          boolean conflict) {
            sourceRootId=root; exposureUid=exposure;
            expertIds=Collections.unmodifiableList(new ArrayList<>(experts));
            sensorUids=Collections.unmodifiableList(new ArrayList<>(sensors));
            alternativeChemicalUids=Collections.unmodifiableList(new ArrayList<>(chemicals));
            effectivePolarity=p;
            uniqueChemicalUid=chemicals.size()==1?chemicals.iterator().next():null;
            sharedHypothesisRoot=chemicals.size()>1;
            conflictingPolarity=conflict;
            independentlyValidatedG3=false;
        }
        public boolean positiveResearchHypothesis() {
            return effectivePolarity==CoreContracts.EvidencePolarity.POSITIVE
                && uniqueChemicalUid!=null && !sharedHypothesisRoot;
        }
    }

    public static final class Summary {
        public final String exposureUid;
        public final int rawExpertOutputs, independentSourceRoots;
        public final List<RootGroup> roots;
        public final List<CoreContracts.EvidenceNode> consolidatedNodes;
        public final boolean confirmedGas,ppmAuthorized,physicalG3Verified;
        private Summary(String exposure,List<RootGroup> groups,List<CoreContracts.EvidenceNode> nodes,
                        int n) {
            exposureUid=exposure;rawExpertOutputs=n;
            independentSourceRoots=groups.size();
            roots=Collections.unmodifiableList(groups);
            consolidatedNodes=Collections.unmodifiableList(nodes);
            confirmedGas=false;ppmAuthorized=false;physicalG3Verified=false;
        }
    }

    private static final class Bucket {
        final Set<String> experts=new LinkedHashSet<>(), sensors=new LinkedHashSet<>();
        final Set<Long> uids=new LinkedHashSet<>();
        boolean positive,negative,ambiguous,notEvaluable;
    }

    public static Summary consolidate(String exposureUid,List<CoreContracts.EvidenceNode> raw) {
        if(exposureUid==null||exposureUid.isBlank()||raw==null)
            throw new IllegalArgumentException("one real exposure and raw evidence list required");
        Map<String,Bucket> bySource=new LinkedHashMap<>();
        for(CoreContracts.EvidenceNode node:raw) {
            if(node==null)throw new IllegalArgumentException("null source evidence forbidden");
            Bucket b=bySource.computeIfAbsent(node.sourceRootId,k->new Bucket());
            b.experts.add(node.expertId);b.sensors.add(node.sensorUid);
            if(node.canonicalChemicalUid!=null)b.uids.add(node.canonicalChemicalUid);
            switch(node.polarity) {
                case POSITIVE: b.positive=true;break;
                case CONDITIONAL_NEGATIVE:b.negative=true;break;
                case AMBIGUOUS:b.ambiguous=true;break;
                case NOT_EVALUABLE:b.notEvaluable=true;break;
                default:throw new IllegalArgumentException("unknown polarity");
            }
        }
        List<RootGroup> groups=new ArrayList<>();
        List<CoreContracts.EvidenceNode> nodes=new ArrayList<>();
        for(Map.Entry<String,Bucket> e:bySource.entrySet()){
            String root=e.getKey();Bucket b=e.getValue();
            boolean conflict=(b.positive&&b.negative);
            CoreContracts.EvidencePolarity polarity;
            if(conflict||b.ambiguous||b.uids.size()>1)polarity=CoreContracts.EvidencePolarity.AMBIGUOUS;
            else if(b.positive)polarity=CoreContracts.EvidencePolarity.POSITIVE;
            else if(b.negative)polarity=CoreContracts.EvidencePolarity.CONDITIONAL_NEGATIVE;
            else polarity=CoreContracts.EvidencePolarity.NOT_EVALUABLE;
            RootGroup g=new RootGroup(root,exposureUid,b.experts,b.sensors,b.uids,polarity,conflict);
            groups.add(g);
            // All experts in this root collapsed to a traceable, single research node.
            Long uid=b.uids.size()==1?g.uniqueChemicalUid:null;
            nodes.add(new CoreContracts.EvidenceNode(root,"NEXT_P04_SOURCE_ROOT_FUSION",
                b.sensors.iterator().next(),uid,polarity,
                "research root grouped; experts="+b.experts.size()+" sensors="+b.sensors.size()
                    +" alternatives="+b.uids.size()+"; G3=false"));
        }
        return new Summary(exposureUid,groups,nodes,raw.size());
    }
}
