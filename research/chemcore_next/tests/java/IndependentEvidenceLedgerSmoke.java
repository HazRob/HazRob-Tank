package com.hazrob.tank.chemcore.next;

import java.util.ArrayList;
import java.util.List;

public final class IndependentEvidenceLedgerSmoke {
    private static int count;
    private static void check(boolean b,String why) {
        count++;if(!b)throw new AssertionError("P04.6 #"+count+": "+why);
    }
    private static CoreContracts.EvidenceNode n(String root,String expert,String sensor,Long uid,
                                                 CoreContracts.EvidencePolarity p){
        return new CoreContracts.EvidenceNode(root,expert,sensor,uid,p,"research");
    }
    private static IndependentEvidenceLedger.Summary c(CoreContracts.EvidenceNode... inputs) {
        return IndependentEvidenceLedger.consolidate("exposure-MQ-001",List.of(inputs));
    }
    private static void reject(Runnable r){
        boolean rejected=false;try{r.run();}catch(IllegalArgumentException x){rejected=true;}
        check(rejected,"invalid source grouping rejected");
    }
    public static void main(String[] args){
        CoreContracts.EvidenceNode a=n("DATASHEET-ONE","CURVE","MQ4",76L,
            CoreContracts.EvidencePolarity.POSITIVE);
        CoreContracts.EvidenceNode b=n("DATASHEET-ONE","FINGERPRINT","MQ8",76L,
            CoreContracts.EvidencePolarity.POSITIVE);
        IndependentEvidenceLedger.Summary group=c(a,b);
        check(group.rawExpertOutputs==2&&group.independentSourceRoots==1,
            "two experts from one original root equal one source");
        check(group.roots.get(0).effectivePolarity==CoreContracts.EvidencePolarity.POSITIVE,
            "consensus within one source does not count as two votes");
        check(group.roots.get(0).positiveResearchHypothesis(),"one research hypothesis");
        check(group.roots.get(0).sensorUids.size()==2,"two actual physical sensors recorded");
        check(group.consolidatedNodes.size()==1,"single normalized evidence node");
        check(!group.confirmedGas&&!group.ppmAuthorized&&!group.physicalG3Verified,
            "source-only evidence never promotes gas or ppm");

        CoreContracts.EvidenceNode independent=n("EXPERIMENT-TWO","CURVE","MQ8",76L,
            CoreContracts.EvidencePolarity.POSITIVE);
        check(c(a,b,independent).independentSourceRoots==2,
            "independent source root restores source count");
        CoreContracts.EvidenceNode otherGas=n("DATASHEET-ONE","CURVE","MQ4",200L,
            CoreContracts.EvidencePolarity.POSITIVE);
        group=c(a,otherGas);
        check(group.independentSourceRoots==1&&group.roots.get(0).sharedHypothesisRoot,
            "NH3/NH4 or source alias must not multiply roots");
        check(group.roots.get(0).uniqueChemicalUid==null,
            "conflicting chemical interpretations require ambiguity");
        check(group.roots.get(0).effectivePolarity==CoreContracts.EvidencePolarity.AMBIGUOUS,
            "same source, two UIDs cannot be two confirmations");
        check(!group.roots.get(0).positiveResearchHypothesis(),
            "shared source is not a verified unique molecular identity");
        CoreContracts.EvidenceNode negative=n("DATASHEET-ONE","SELECTIVITY","MQ8",76L,
            CoreContracts.EvidencePolarity.CONDITIONAL_NEGATIVE);
        group=c(a,negative);
        check(group.roots.get(0).conflictingPolarity,"contradictory polarities disclosed");
        check(group.roots.get(0).effectivePolarity==CoreContracts.EvidencePolarity.AMBIGUOUS,
            "positive and negative never cancel numerically");
        CoreContracts.EvidenceNode none=n("DATASHEET-ONE","CURVE","MQ4",76L,
            CoreContracts.EvidencePolarity.NOT_EVALUABLE);
        group=c(none);
        check(group.roots.get(0).effectivePolarity==CoreContracts.EvidencePolarity.NOT_EVALUABLE,
            "missing sensor not negative");
        group=c(a,none);
        check(group.roots.get(0).effectivePolarity==CoreContracts.EvidencePolarity.POSITIVE,
            "not evaluable neither deletes nor strengthens actual evidence");
        group=c(n("DATASHEET-ONE","CURVE","MQ4",null,
            CoreContracts.EvidencePolarity.AMBIGUOUS));
        check(group.roots.get(0).effectivePolarity==CoreContracts.EvidencePolarity.AMBIGUOUS,
            "unmapped source cannot become candidate");
        check(c().independentSourceRoots==0,"zero roots is legal");
        reject(()->IndependentEvidenceLedger.consolidate(null,List.of(a)));
        reject(()->IndependentEvidenceLedger.consolidate("group",null));
        List<CoreContracts.EvidenceNode> mutable=new ArrayList<>();
        mutable.add(a);mutable.add(null);
        reject(()->IndependentEvidenceLedger.consolidate("group",mutable));
        boolean immutable=false;try{c(a).roots.clear();}catch(UnsupportedOperationException e){immutable=true;}
        check(immutable,"immutable grouped summary");
        System.out.println("CHEMCORE_NEXT_P04_6_ROOT_FUSION_PASS assertions="+count+
            " source_dedup=true no_unqualified_posterior=true G3=0");
    }
}
