package com.servoc.app.chemcore;

import java.util.Arrays;

/** JDK-only research gate tests: duplication, lineage, conflicts, open-set and provenance. */
public final class TankV6EvidenceConsensusTest {
    private static int assertions=0;
    private static void check(boolean good,String message){assertions++;if(!good)throw new AssertionError(message);}
    private static TankV6EvidenceConsensus.Evidence e(long gas,String sensor,String root,String model,double match){
        return new TankV6EvidenceConsensus.Evidence(gas,sensor,root,model,match,1.0,true);
    }
    private static TankV6EvidenceConsensus.Decision fuse(TankV6EvidenceConsensus.Evidence...e){
        return TankV6EvidenceConsensus.fuse(Arrays.asList(e));
    }
    private static void status(TankV6EvidenceConsensus.Decision d,TankV6EvidenceConsensus.Outcome expected){
        check(d.outcome==expected,"expected "+expected+" actual "+d.outcome+" reason "+d.reason);
        check(!d.deviceValidated && !d.reportablePpm,"no M4 or ppm authority");
    }
    public static void main(String[] args){
        var a=e(101,"MQ2-serial-1","paper-A","curve-A",.90);
        var b=e(101,"MQ8-serial-2","paper-A","curve-B",.85);
        var other=e(102,"MQ2-serial-1","paper-C","curve-C",.30);
        var other2=e(102,"MQ8-serial-2","paper-C","curve-D",.35);
        var d=fuse(a,b,other,other2);
        status(d,TankV6EvidenceConsensus.Outcome.RESEARCH_SCREENING);
        check(d.rankedCandidates.size()==2,"candidate count");
        check(d.rankedCandidates.get(0).candidateUid==101,"winner by score");
        check(d.rankedCandidates.get(0).physicalSensorBreadth==2,"physical breadth");
        check(d.rankedCandidates.get(0).independentSourceRoots==1,"single upstream source root despite two channels");
        check(Math.abs(d.rankedCandidates.get(0).matchIndex-.875)<1e-12,"weighted mean");
        var duplicated=fuse(a,a,a,b,other,other2);
        check(Math.abs(duplicated.rankedCandidates.get(0).matchIndex-d.rankedCandidates.get(0).matchIndex)<1e-12,
               "duplicating same source sensor cannot raise score");
        check(duplicated.rankedCandidates.get(0).physicalSensorBreadth==2,"duplicate remains 2 sensors");
        status(fuse(a),TankV6EvidenceConsensus.Outcome.RESEARCH_SCREENING);
        status(fuse(e(101,"MQ2","a","m1",.9),e(101,"MQ2","b","m2",.8)),
               TankV6EvidenceConsensus.Outcome.RESEARCH_SCREENING);
        d=fuse(a,b,e(101,"MQ2-serial-1","paper-Z","contrary",.10));
        status(d,TankV6EvidenceConsensus.Outcome.RESEARCH_SCREENING);
        check(d.rankedCandidates.get(0).conflictingSourceModels,"conflict explicitly reported");
        status(fuse(e(1,"a","root","a",.5),e(1,"b","root","b",.48)),
               TankV6EvidenceConsensus.Outcome.RESEARCH_SCREENING);
        status(fuse(e(1,"a","root","a",.7),e(1,"b","root","b",.7),
                    e(2,"a","root2","c",.65),e(2,"b","root2","d",.68)),
               TankV6EvidenceConsensus.Outcome.RESEARCH_SCREENING);
        d=fuse(new TankV6EvidenceConsensus.Evidence(1,"MQ2","","x",.9,1,true),
               new TankV6EvidenceConsensus.Evidence(1,"MQ8","","y",.9,1,true));
        status(d,TankV6EvidenceConsensus.Outcome.RESEARCH_SCREENING);
        check(d.rankedCandidates.get(0).missingProvenance,"missing source labels annotated");
        d=fuse(new TankV6EvidenceConsensus.Evidence(1,"MQ2","root","x",.9,0,true),
               new TankV6EvidenceConsensus.Evidence(1,"MQ8","root","y",.9,1,false));
        status(d,TankV6EvidenceConsensus.Outcome.RESEARCH_SCREENING);
        check(d.rankedCandidates.get(0).hasOutsideSourceDomain,
              "source range status is advisory, not a filter");
        check(d.rejectedObservations==1,"invalid numeric quality rejected");
        status(fuse(new TankV6EvidenceConsensus.Evidence(1,"MQ2","root","x",Double.NaN,1,true)),
               TankV6EvidenceConsensus.Outcome.INSUFFICIENT_EVIDENCE);
        d=fuse(e(101,"s1","A","m1",.9),e(101,"s2","B","m2",.9));
        status(d,TankV6EvidenceConsensus.Outcome.RESEARCH_SCREENING);
        check(d.rankedCandidates.get(0).independentSourceRoots==2,"two roots from two sensors");
        var shuffled=fuse(other2,b,other,a);
        check(shuffled.rankedCandidates.get(0).candidateUid==101,"permutation invariance");
        check(Math.abs(shuffled.rankedCandidates.get(0).matchIndex-.875)<1e-12,
              "permutation score invariance");
        try{d.rankedCandidates.clear();throw new AssertionError("mutable rankedCandidates");}
        catch(UnsupportedOperationException expected){assertions++;}
        check(TankV6EvidenceConsensus.SCORE_SEMANTICS.contains("not_probability"),"score never probability");
        check(d.rankedCandidates.get(0).candidateUid==101,"no heuristic threshold rejection");
        check(fuse(a).rankedCandidates.size()==1,"one sensor remains visible");
        check(fuse(e(10,"X","Y","Z",.01)).rankedCandidates.size()==1,
              "low score remains visible");
        check(fuse(new TankV6EvidenceConsensus.Evidence(7,"",null,null,.5,.5,false))
              .rankedCandidates.get(0).missingProvenance,
              "unattributed input still calculable");
        System.out.println("PASS V6 evidence consensus: "+assertions+" assertions; research-only, zero G3 claims");
    }
}
