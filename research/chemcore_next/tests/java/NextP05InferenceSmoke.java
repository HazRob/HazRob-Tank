package com.hazrob.tank.chemcore.next;
import java.util.*;
public final class NextP05InferenceSmoke {
 private static int n;
 private static void yes(boolean b,String reason){n++;if(!b)throw new AssertionError("P05.4-P05.7 "+n+" "+reason);}
 private static CoreContracts.EvidenceNode e(String root,long uid,CoreContracts.EvidencePolarity p) {
   return new CoreContracts.EvidenceNode(root,"SOURCE_CURVE","physical:MQ4",uid,p,"research only");
 }
 private static ResearchCandidateGenerator.Output proposals(IndependentEvidenceLedger.Summary ledger,
                                                              int nCandidate){
   List<ResearchCandidateGenerator.Material> m=new ArrayList<>();
   List<ResearchCandidateGenerator.AssetLink> links=new ArrayList<>();
   for(int i=0;i<nCandidate;i++){
      long uid=200+i; m.add(new ResearchCandidateGenerator.Material(uid,"material-"+uid,
         ResearchCandidateGenerator.MaterialKind.MOLECULAR_CHEMICAL));
      for(var root:ledger.roots)if(root.uniqueChemicalUid!=null&&root.uniqueChemicalUid==uid)
          links.add(new ResearchCandidateGenerator.AssetLink(root.sourceRootId,"CURVE-"+i,
             uid,true));
   }
   return ResearchCandidateGenerator.generate(m,links,ledger,10);
 }
 private static NextResearchDecisionPipeline.Decision eval(int nCandidate,int cap,
                                                            CoreContracts.EvidenceNode... nodes){
   var ledger=IndependentEvidenceLedger.consolidate("SOURCE_GAS_EXPOSURE",List.of(nodes));
   return NextResearchDecisionPipeline.evaluate(EvidenceConflictGraph.construct(ledger),
       proposals(ledger,nCandidate),cap);
 }
 private static CoreContracts.SensorInstance sensor(){
   return new CoreContracts.SensorInstance("device:mq4","MQ4","board-main","SOURCE_HEATER_READY");
 }
 private static CoreContracts.Reading reading(double ratio){
   return new CoreContracts.Reading("device:mq4",14532,2.5,ratio*10.0,10.0,10.0,
     CoreContracts.CalibrationReference.SOURCE_R0,"SOURCE_HEATER_READY",
     CoreContracts.SampleQuality.VALID);
 }
 private static SourceCurveExpert.Curve curve(){
   return new SourceCurveExpert.Curve("hypothesis","original-MQ4-source","MQ4",
      "methane",200L,SourceCurveExpert.Equation.RATIO_EQUALS_A_TIMES_PPM_OVER_SCALE_POWER_B,
      1.5,-.5,100.0,10.0,5000.0,"SOURCE_HEATER_READY");
 }
 public static void main(String[] args){
   var pos=CoreContracts.EvidencePolarity.POSITIVE;
   var neg=CoreContracts.EvidencePolarity.CONDITIONAL_NEGATIVE;
   var none=eval(736,10);
   yes(none.status==CoreContracts.DecisionStatus.UNKNOWN&&none.eligibleCount==0,
       "zero actual source evidence means UNKNOWN");
   var one=eval(736,10,e("A",200,pos),e("A",200,pos));
   yes(one.status==CoreContracts.DecisionStatus.RESEARCH_CANDIDATES,"single source candidate");
   yes(one.eligibleCount==1&&one.eligibleRanked.get(0).independentPositiveRoots==1,
       "same source never duplicated");
   yes(one.display.size()==1,"no top ten filling");
   var independent=eval(736,10,e("A",200,pos),e("B",200,pos));
   yes(independent.eligibleRanked.get(0).independentPositiveRoots==2,
       "two qualified independent source roots");
   var mixture=eval(736,10,e("A",200,pos),e("B",201,pos));
   yes(mixture.status==CoreContracts.DecisionStatus.MIXTURE_UNRESOLVED,
       "two alternative source hypotheses never confirmed as physical mixture");
   yes(!mixture.mixtureConfirmed&&mixture.unknownPossible,"mixture is not a confirmation");
   var conflict=eval(736,10,e("A",200,pos),e("B",200,neg));
   yes(conflict.status==CoreContracts.DecisionStatus.UNKNOWN,"conflict abstains");
   yes(conflict.blockedContradictoryCount==1,"conflicting material documented");
   var alternatives=eval(736,10,e("A",200,pos),e("A",201,pos));
   yes(alternatives.status==CoreContracts.DecisionStatus.UNKNOWN,
       "two identities sharing same source root not eligible as separate gases");
   var list=new ArrayList<CoreContracts.EvidenceNode>();
   for(int i=0;i<12;i++)list.add(e("INDEPENDENT-"+i,200+i,pos));
   var r=eval(736,10,list.toArray(new CoreContracts.EvidenceNode[0]));
   yes(r.eligibleCount==12&&r.displayCount==10,"dynamic 12 eligible not padded");
   yes(r.eligibleRanked.get(11).uid==211L,"nonlegacy UID no 144 cap");
   yes(r.status==CoreContracts.DecisionStatus.MIXTURE_UNRESOLVED,"source-only possible mixture");
   yes(!r.gasIdentityConfirmed&&!r.ppmCertified&&!r.fieldSafetyAlarmEnabled
       &&!r.physicalG3Qualified,"research output cannot set alarms");
   for(int count:new int[]{0,1,5,10,12}) {
       var nodes=new ArrayList<CoreContracts.EvidenceNode>();
       for(int i=0;i<count;i++)nodes.add(e("R"+i,200+i,pos));
       var q=eval(736,20,nodes.toArray(new CoreContracts.EvidenceNode[0]));
       yes(q.eligibleCount==count,"exact eligible dynamic size "+count);
   }
   var refCurve=curve();
   var goodQ=new SourceCurveExpert.Qualification(true,true,true,true);
   double proposedPpm=100.0, ratio=SourceCurveExpert.forwardSourceRatio(refCurve,proposedPpm);
   var forward=ForwardInverseVerifier.verify(refCurve,sensor(),reading(ratio),goodQ,
      proposedPpm,false);
   yes(forward.status==ForwardInverseVerifier.Status.RESEARCH_FORWARD_RESIDUAL_ONLY,
       "forward projected source residual computed");
   yes(Math.abs(forward.logRatioResidual)<1e-12,"equal source-proposed hypothesis zero log residual");
   forward=ForwardInverseVerifier.verify(refCurve,sensor(),reading(ratio*.8),goodQ,
      proposedPpm,false);
   yes(Math.abs(forward.logRatioResidual-Math.log(.8))<1e-12,
       "measured perturbation residual is reproducible");
   forward=ForwardInverseVerifier.verify(refCurve,sensor(),reading(ratio),goodQ,
      6000.0,false);
   yes(forward.status==ForwardInverseVerifier.Status.OUTSIDE_SOURCE_DOMAIN,
       "outside declared ppm source domain abstains");
   forward=ForwardInverseVerifier.verify(refCurve,sensor(),reading(ratio),goodQ,
      100.0,true);
   yes(forward.status==ForwardInverseVerifier.Status.MIXTURE_MODEL_UNQUALIFIED,
       "no invented mixture signal superposition");
   forward=ForwardInverseVerifier.verify(refCurve,sensor(),reading(ratio),
       SourceCurveExpert.Qualification.unverified(),100.0,false);
   yes(forward.status==ForwardInverseVerifier.Status.SOURCE_NOT_APPLICABLE,
       "foreign device unqualified R0 cannot invert");
   yes(!forward.alarmAuthorized&&!forward.certifiedPpm&&!forward.g3Qualified,
       "research-only source forward inversion never certified");
   var fb=new NextResearchDecisionPipeline.Feedback("research-user","feedback-1",
       "snapshot-001","suspicious experimental pattern");
   List<NextResearchDecisionPipeline.Feedback> appended=
       NextResearchDecisionPipeline.appendFeedback(List.of(),fb);
   yes(appended.size()==1,"append-only one feedback");
   yes(appended.get(0)==fb,"raw feedback preserved");
   boolean immutable=false;try{appended.clear();}catch(UnsupportedOperationException x){immutable=true;}
   yes(immutable,"operator feedback ledger immutable");
   boolean duplicate=false;
   try{NextResearchDecisionPipeline.appendFeedback(appended,fb);}
   catch(IllegalArgumentException x){duplicate=true;}
   yes(duplicate,"duplicated feedback ID rejected");
   var snapshot=new CoreContracts.Snapshot("snapshot-001","device-001",
       CoreContracts.Mode.LAB,20000L,null,null,true,List.of(reading(1.0)),"stable-research-hash");
   var ledger=IndependentEvidenceLedger.consolidate("SOURCE_GAS_EXPOSURE",List.of(e("A",200,pos)));
   var result=NextResearchDecisionPipeline.result(snapshot,"snapshot-sha",
       EvidenceConflictGraph.construct(ledger),one);
   yes(result.status==CoreContracts.DecisionStatus.RESEARCH_CANDIDATES,
       "immutable scientific result status");
   yes(!result.reportablePpm&&!result.physicallyValidatedG3,
       "immutable result cannot authorize safety");
   System.out.println("CHEMCORE_NEXT_P05_4_7_INFERENCE_PASS assertions="+n+
       " source_conflicts=true unknown=true dynamic=true forward_inverse=true safety=false G3=0");
 }
}
