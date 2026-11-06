package com.hazrob.tank.chemcore.next;

import java.util.*;

/** R2 deterministic actual-analyzer regression: one root, many chemical labels. */
public final class NextCoherenceR2Regression {
  private static int checks=0;
  private static void require(boolean ok,String cause){
    checks++;
    if(!ok)throw new AssertionError("COHERENCE_R2_FAIL "+cause);
  }
  private static final String ROOT="RESEARCH_SINGLE_SOURCE_NOT_INDEPENDENT_ROOTS";
  private static final String HEATER="SIMULATED_VERIFIED_FOR_G1_ONLY";
  private static final SourceCurveExpert.Qualification TEST_ONLY_Q=
      new SourceCurveExpert.Qualification(true,true,true,true);
  private static SourceCurveExpert.Curve curve(String id,long uid,String sensor,double a,double b){
    return new SourceCurveExpert.Curve(id,ROOT,sensor,"SYNTHETIC_UID_"+uid,uid,
        SourceCurveExpert.Equation.RATIO_EQUALS_A_TIMES_PPM_OVER_SCALE_POWER_B,
        a,b,1.,200.,10000.,HEATER);
  }
  private static final SourceCurveExpert.Curve A2=curve("A2",101,"MQ2",10.,-0.35);
  private static final SourceCurveExpert.Curve A4=curve("A4",101,"MQ4",12.,-0.35);
  private static final SourceCurveExpert.Curve B2=curve("B2",102,"MQ2",10.1,-0.35);
  private static final SourceCurveExpert.Curve B4=curve("B4",102,"MQ4",11.,-0.35);
  private static final SourceCurveExpert.Curve T2=curve("T2",102,"MQ2",10.,-0.35);
  private static final SourceCurveExpert.Curve T4=curve("T4",102,"MQ4",12.,-0.35);

  private static final CoreContracts.DeviceProfile PROFILE=new CoreContracts.DeviceProfile(
      "R2_DEVICE_G1_ONLY","G1_SIMULATOR","NO_TANK_HARDWARE",
      List.of(new CoreContracts.SensorInstance("SIM-MQ2","MQ2","NO_ADC",HEATER),
              new CoreContracts.SensorInstance("SIM-MQ4","MQ4","NO_ADC",HEATER)));
  private static ChemCoreNext.Output run(SourceCurveExpert.Curve[] curves,
           double ratio2,double ratio4,boolean strict){
    var shot=new CoreContracts.Snapshot("R2_"+ratio2+"_"+ratio4,"R2_DEVICE_G1_ONLY",
        CoreContracts.Mode.LAB,60000,25.,45.,true,
        List.of(new CoreContracts.Reading("SIM-MQ2",null,null,ratio2*10,12.,10.,
                    CoreContracts.CalibrationReference.SOURCE_R0,HEATER,CoreContracts.SampleQuality.VALID),
                new CoreContracts.Reading("SIM-MQ4",null,null,ratio4*10,14.,10.,
                    CoreContracts.CalibrationReference.SOURCE_R0,HEATER,CoreContracts.SampleQuality.VALID)),
        "SYNTHETIC_SOURCE_REFERENCE_WITHOUT_G3");
    List<ChemCoreNext.CurveBinding> bindings=new ArrayList<>();
    for(var curve:curves) bindings.add(new ChemCoreNext.CurveBinding(curve,
        curve.sensorModel.equals("MQ2")?"SIM-MQ2":"SIM-MQ4",
        strict?SourceCurveExpert.Qualification.unverified():TEST_ONLY_Q,0.05));
    var material=List.of(
      new ResearchCandidateGenerator.Material(101,"SYNTHETIC_A",
          ResearchCandidateGenerator.MaterialKind.MOLECULAR_CHEMICAL),
      new ResearchCandidateGenerator.Material(102,"SYNTHETIC_B",
          ResearchCandidateGenerator.MaterialKind.MOLECULAR_CHEMICAL));
    var out=ChemCoreNext.analyze(new ChemCoreNext.Input(PROFILE,shot,
        ChemCoreNext.snapshotHash(shot),material,bindings,List.of(),6));
    require(!out.ppmAuthorized&&!out.safetyAlarmAuthorized&&!out.g3Qualified
       &&!out.molecularIdentityConfirmed,"false G3 or identity");
    return out;
  }
  private static long top(ChemCoreNext.Output out){
    return out.arbitration.eligibleRanked.isEmpty()?0:
        out.arbitration.eligibleRanked.get(0).uid;
  }
  public static void main(String[] args){
    double ppm=1000.;
    double a2=SourceCurveExpert.forwardSourceRatio(A2,ppm);
    double a4=SourceCurveExpert.forwardSourceRatio(A4,ppm);
    var original=run(new SourceCurveExpert.Curve[]{A2,A4,B2,B4},a2,a4,false);
    require(original.sourceCoherence.size()==1,"root group count");
    require(original.sourceCoherence.get(0).ranked.size()==2,"all alternate candidates visible");
    require(original.sourceCoherence.get(0).selectedResearchUid!=null
        &&original.sourceCoherence.get(0).selectedResearchUid==101L,"source multisensor A");
    require(top(original)==101L,"actual Next arbitration A");
    require(original.arbitration.status==CoreContracts.DecisionStatus.RESEARCH_CANDIDATES,
        "actual Next A is research-only");
    var reverse=run(new SourceCurveExpert.Curve[]{A2,A4,B2,B4},
        SourceCurveExpert.forwardSourceRatio(B2,ppm),
        SourceCurveExpert.forwardSourceRatio(B4,ppm),false);
    require(reverse.sourceCoherence.get(0).selectedResearchUid!=null
        &&reverse.sourceCoherence.get(0).selectedResearchUid==102L,"B source multichannel");
    require(top(reverse)==102L,"actual Next arbitration B");
    var noisy=run(new SourceCurveExpert.Curve[]{A2,A4,B2,B4},a2*1.02,a4*.98,false);
    require(!noisy.sourceCoherence.isEmpty(),"noise diagnostics accessible");
    var tie=run(new SourceCurveExpert.Curve[]{A2,A4,T2,T4},a2,a4,false);
    require(tie.sourceCoherence.get(0).selectedResearchUid==null,"tie remains ambiguous");
    require(tie.arbitration.status==CoreContracts.DecisionStatus.UNKNOWN,"same root tie abstention");
    var oneChannel=run(new SourceCurveExpert.Curve[]{A2,B2},a2,a4,false);
    require(oneChannel.sourceCoherence.get(0).selectedResearchUid==null,"one channel never disambiguates");
    require(oneChannel.arbitration.status==CoreContracts.DecisionStatus.UNKNOWN,
        "one-channel ambiguity remains unknown");
    var strict=run(new SourceCurveExpert.Curve[]{A2,A4,B2,B4},a2,a4,true);
    require(strict.sourceCoherence.isEmpty(),"no fake source qualification");
    require(strict.arbitration.status==CoreContracts.DecisionStatus.UNKNOWN,"unverified gate holds");
    var singleton=run(new SourceCurveExpert.Curve[]{A2,A4},a2,a4,false);
    require(singleton.sourceCoherence.get(0).selectedResearchUid==null,
        "no forced same-source refinement when no competition");
    require(top(singleton)==101L,"single already-evidenced root remains research hypothesis");
    System.out.println("NEXT_COHERENCE_R2_REGRESSION_PASS checks="+checks+
        " tests=7 ambiguous_ties_preserved=true all_hypotheses_visible=true"+
        " g3=0 field_ppm=0 independent_gas_accuracy_unmeasured=true");
  }
}
