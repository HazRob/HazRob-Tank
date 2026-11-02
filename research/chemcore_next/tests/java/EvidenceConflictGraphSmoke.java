package com.hazrob.tank.chemcore.next;
import java.util.*;
public final class EvidenceConflictGraphSmoke {
  private static int n;
  private static void yes(boolean ok,String why){
    n++;if(!ok)throw new AssertionError("P05.3 "+n+": "+why);
  }
  private static CoreContracts.EvidenceNode e(String r,String expert,String sensor,Long uid,
                                               CoreContracts.EvidencePolarity p) {
    return new CoreContracts.EvidenceNode(r,expert,sensor,uid,p,"source only");
  }
  private static EvidenceConflictGraph.Graph graph(CoreContracts.EvidenceNode... xs) {
    return EvidenceConflictGraph.construct(IndependentEvidenceLedger.consolidate("measured-source-exposure",Arrays.asList(xs)));
  }
  public static void main(String[] args){
    var pos=CoreContracts.EvidencePolarity.POSITIVE;
    var neg=CoreContracts.EvidencePolarity.CONDITIONAL_NEGATIVE;
    var x=graph(e("ORIGINAL-1","CURVE","MQ4",713L,pos),e("ORIGINAL-1","FINGERPRINT","MQ8",713L,pos));
    yes(x.rawNodes==2&&x.independentSourceRoots==1,"two correlated experts one root");
    yes(x.materials.get(713L).positiveRoots.size()==1,"same physical root not two votes");
    yes(x.edges.stream().anyMatch(v->v.kind==EvidenceConflictGraph.Link.SAME_SOURCE_CORRELATED),
        "correlation edge present");
    yes(!x.hasUnresolvedAlternatives&&!x.hasUnresolvedConflicts,"no false conflict");
    x=graph(e("ORIGINAL-1","CURVE","MQ4",713L,pos),e("ORIGINAL-2","CURVE","MQ8",713L,pos));
    yes(x.materials.get(713L).positiveRoots.size()==2,"independent roots counted");
    yes(x.edges.stream().anyMatch(v->v.kind==EvidenceConflictGraph.Link.CROSS_SOURCE_SUPPORT),
        "source corroboration explained");
    x=graph(e("ORIGINAL-1","CURVE","MQ4",713L,pos),e("CHALLENGE-2","SELECTIVITY","MQ8",713L,neg));
    yes(x.hasUnresolvedConflicts,"negative-positive contradiction retained");
    yes(x.materials.get(713L).crossSourceContradiction,"material conflict attached");
    yes(x.edges.stream().anyMatch(v->v.kind==EvidenceConflictGraph.Link.CROSS_SOURCE_CONTRADICTION),
        "conflict edge explicit");
    x=graph(e("ORIGINAL-1","CURVE","MQ4",713L,pos),e("ORIGINAL-1","CURVE","MQ4",714L,pos));
    yes(x.hasUnresolvedAlternatives,"common-source competing chemical identities");
    yes(x.materials.get(713L).positiveRoots.isEmpty(),"alternative not uniquely positive");
    yes(x.edges.stream().anyMatch(v->v.kind==EvidenceConflictGraph.Link.ALTERNATIVE_HYPOTHESIS),
        "source-root alternative edge");
    x=graph();
    yes(x.materials.isEmpty()&&x.independentSourceRoots==0,"zero evidence legal");
    yes(!x.g3Qualified&&!x.ppmAuthorized,"never promotes physical or ppm");
    System.out.println("CHEMCORE_NEXT_P05_3_CONFLICT_GRAPH_PASS assertions="+n+" g3=false");
  }
}
