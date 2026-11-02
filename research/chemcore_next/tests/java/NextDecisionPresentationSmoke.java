package com.hazrob.tank.chemcore.next;
import java.util.*;
public final class NextDecisionPresentationSmoke {
  private static int n;
  private static final String SHA="aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
  private static void yes(boolean b,String why){n++;if(!b)throw new AssertionError("P07.4 "+n+" "+why);}
  private static CoreContracts.Snapshot snapshot(){
    return new CoreContracts.Snapshot("snapshot-p074","tank-physical",
      CoreContracts.Mode.INSTANT,1500L,24.0,40.0,true,List.of(),SHA);
  }
  private static CoreContracts.Result result(CoreContracts.Snapshot s,int n){
    var candidates=new ArrayList<CoreContracts.Candidate>();
    for(int i=0;i<n;i++)candidates.add(new CoreContracts.Candidate(
      800L+i,"only source root "+i,List.of("source-original-"+i)));
    return new CoreContracts.Result(ChemCoreNext.VERSION,s,ChemCoreNext.snapshotHash(s),
      n==0?CoreContracts.DecisionStatus.UNKNOWN:
      n==1?CoreContracts.DecisionStatus.RESEARCH_CANDIDATES:
      CoreContracts.DecisionStatus.MIXTURE_UNRESOLVED,List.of(),candidates,
      List.of("NO_CHEMICAL_IDENTITY_OR_G3"));
  }
  private static void fail(Runnable op,String title){
    boolean caught=false;try{op.run();}catch(IllegalArgumentException e){caught=true;}
    yes(caught,title);
  }
  public static void main(String[] args){
    var s=snapshot();
    for(int amount:new int[]{0,1,5,10,12,80}){
      var v=NextDecisionPresentation.project(s,result(s,amount));
      yes(v.totalEligible==amount&&v.orderedCandidates.size()==amount,
        "does not pad or clip dynamic candidates "+amount);
      if(amount>0){
        yes(v.orderedCandidates.get(0).canonicalUid==800L,
          "original identity UID retained first "+amount);
        yes(v.orderedCandidates.get(amount-1).canonicalUid==800+amount-1,
          "original identity UID retained last "+amount);
        yes(v.orderedCandidates.get(amount-1).ordinal==amount,
          "ordinal assigned only after preserved order "+amount);
      }
      yes(!v.gasIdentityConfirmed&&!v.ppmReportable&&!v.fieldSafetyAlarmAllowed&&
          !v.physicallyValidatedG3,"no confidence/ppm/safety made by UI "+amount);
    }
    var v=NextDecisionPresentation.project(s,result(s,12));
    boolean immutable=false;try{v.orderedCandidates.clear();}
      catch(UnsupportedOperationException e){immutable=true;}
    yes(immutable,"presentation immutable");
    fail(()->NextDecisionPresentation.project(null,result(s,1)),"no missing capture");
    var alien=new CoreContracts.Snapshot("different","tank-physical",
      CoreContracts.Mode.INSTANT,1500L,24.0,40.0,true,List.of(),SHA);
    fail(()->NextDecisionPresentation.project(alien,result(s,1)),"no stale old decision");
    var duplicates=List.of(new CoreContracts.Candidate(900,"first",List.of("root1")),
      new CoreContracts.Candidate(900,"second",List.of("root2")));
    var dup=new CoreContracts.Result(ChemCoreNext.VERSION,s,
      ChemCoreNext.snapshotHash(s),CoreContracts.DecisionStatus.RESEARCH_CANDIDATES,
      List.of(),duplicates,List.of());
    fail(()->NextDecisionPresentation.project(s,dup),"no duplicate chemical uid");
    var unsupported=new CoreContracts.Result(ChemCoreNext.VERSION,s,
      ChemCoreNext.snapshotHash(s),CoreContracts.DecisionStatus.UNKNOWN,List.of(),
      List.of(new CoreContracts.Candidate(901,"fake",List.of("root"))),List.of());
    fail(()->NextDecisionPresentation.project(s,unsupported),"UNKNOWN must be empty");
    System.out.println("CHEMCORE_NEXT_P07_4_IMMUTABLE_DISPLAY_PASS assertions="+n+
      " counts=0_1_5_10_12_80 no_144_cap=true G3=false");
  }
}
