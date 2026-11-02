package com.hazrob.tank.chemcore.next;
import java.util.*;
public final class NextEngineRoutingPolicySmoke {
  private static int n;
  private static void yes(boolean ok,String reason){
    n++;if(!ok)throw new AssertionError("P07.3 "+n+": "+reason);
  }
  public static void main(String[] args){
    var next=NextEngineRoutingPolicy.choose(true,false);
    yes(next.target==NextEngineRoutingPolicy.Target.NEXT_RESEARCH,"Next selected explicitly");
    yes(next.eligibleForNewDecisionPersistence,"only Next results stored");
    yes(!next.ppmReportable&&!next.gasIdentityConfirmed&&!next.g3Qualified,
       "Next remains research-only");
    NextEngineRoutingPolicy.requireNext(next);
    var disabled=NextEngineRoutingPolicy.choose(false,false);
    yes(disabled.target==NextEngineRoutingPolicy.Target.DISABLED_REQUIRE_OPERATOR_ACTION,
       "disabled does not become old V6");
    yes(!disabled.eligibleForNewDecisionPersistence,"disabled cannot write fake Next");
    boolean blocked=false;try{NextEngineRoutingPolicy.requireNext(disabled);}
       catch(IllegalStateException e){blocked=true;}
    yes(blocked,"disabled route throws");
    var compare=NextEngineRoutingPolicy.choose(true,true);
    yes(compare.target==NextEngineRoutingPolicy.Target.V6_EXPLICIT_COMPARISON_ONLY,
       "V6 only explicit side by side research comparison");
    yes(!compare.eligibleForNewDecisionPersistence,"V6 not stored as Next");
    blocked=false;try{NextEngineRoutingPolicy.requireNext(compare);}
       catch(IllegalStateException e){blocked=true;}
    yes(blocked,"V6 route cannot pass Next storage");
    yes(compare.provenance.contains("SIDE_BY_SIDE"),"comparison correctly attributed");
    yes(next.provenance.contains("NO_TANK_G3"),"research limitation explicit");
    yes(NextEngineRoutingPolicy.choose(false,true).target==
       NextEngineRoutingPolicy.Target.V6_EXPLICIT_COMPARISON_ONLY,
       "explicit comparison never implicit fallback");
    System.out.println("CHEMCORE_NEXT_P07_3_NO_SILENT_FALLBACK_PASS assertions="+n+
       " source_of_truth_NEXT=true G3=false");
  }
}
