package com.hazrob.tank.chemcore.next;

import java.util.*;

/** Deterministic R3 integrity, duplicate-curve and complete execution-trace regression. */
public final class NextIntegrityTraceR3Regression {
  private static int checks=0;
  private static void check(boolean valid,String why) {
    checks++;
    if(!valid)throw new AssertionError("R3_TRACE_INTEGRITY_FAILURE "+why);
  }
  private static final String ROOT="SOURCE_ONE_G1_ONLY";
  private static final String PHASE="SIMULATED_HEATER_NOT_PHYSICALLY_VERIFIED";
  private static final SourceCurveExpert.Qualification SIMULATED_Q=
      new SourceCurveExpert.Qualification(true,true,true,true);
  private static SourceCurveExpert.Curve curve(String id,long uid,String model,double a){
    return new SourceCurveExpert.Curve(id,ROOT,model,"ARTIFICIAL_UID_"+uid,uid,
      SourceCurveExpert.Equation.RATIO_EQUALS_A_TIMES_PPM_OVER_SCALE_POWER_B,
      a,-0.4,1.0,200.0,10000.0,PHASE);
  }
  private static final SourceCurveExpert.Curve A2=curve("A2",101,"MQ2",10.);
  private static final SourceCurveExpert.Curve A4=curve("A4",101,"MQ4",12.);
  private static final SourceCurveExpert.Curve B2=curve("B2",102,"MQ2",10.7);
  private static final SourceCurveExpert.Curve B4=curve("B4",102,"MQ4",10.8);
  private static final SourceCurveExpert.Curve DUP2=curve("DUP2",101,"MQ2",15.);
  private static final CoreContracts.DeviceProfile DEVICE=new CoreContracts.DeviceProfile(
    "R3_SAMPLE_DEVICE","R3_G1","VIRTUAL",
    List.of(new CoreContracts.SensorInstance("S2","MQ2","ADC_UNKNOWN",PHASE),
            new CoreContracts.SensorInstance("S4","MQ4","ADC_UNKNOWN",PHASE)));
  private static CoreContracts.Snapshot shot(double p2,double p4){
    return new CoreContracts.Snapshot("SYNTHETIC_R3_SAMPLE","R3_SAMPLE_DEVICE",
      CoreContracts.Mode.LAB,60000L,24.,42.,true,
      List.of(new CoreContracts.Reading("S2",null,null,p2*10.,12.,10.,
                     CoreContracts.CalibrationReference.SOURCE_R0,PHASE,CoreContracts.SampleQuality.VALID),
              new CoreContracts.Reading("S4",null,null,p4*10.,14.,10.,
                     CoreContracts.CalibrationReference.SOURCE_R0,PHASE,CoreContracts.SampleQuality.VALID)),
      "RESEARCH_STABLE_REFERENCE_NOT_TANK");
  }
  private static List<ResearchCandidateGenerator.Material> catalog(){
    return List.of(
      new ResearchCandidateGenerator.Material(101L,"RESEARCH_GAS_A",
        ResearchCandidateGenerator.MaterialKind.MOLECULAR_CHEMICAL),
      new ResearchCandidateGenerator.Material(102L,"RESEARCH_GAS_B",
        ResearchCandidateGenerator.MaterialKind.MOLECULAR_CHEMICAL));
  }
  private static ChemCoreNext.Input input(SourceCurveExpert.Curve[] curves,
      boolean qualified,String digest,int displayLimit){
    double r2=SourceCurveExpert.forwardSourceRatio(A2,1000.);
    double r4=SourceCurveExpert.forwardSourceRatio(A4,1000.);
    var s=shot(r2,r4);var bindings=new ArrayList<ChemCoreNext.CurveBinding>();
    for(var c:curves)
      bindings.add(new ChemCoreNext.CurveBinding(c,c.sensorModel.equals("MQ2")?"S2":"S4",
          qualified?SIMULATED_Q:SourceCurveExpert.Qualification.unverified(),0.10));
    return new ChemCoreNext.Input(DEVICE,s,digest==null?ChemCoreNext.snapshotHash(s):digest,
      catalog(),bindings,List.of(),displayLimit);
  }
  private static void throwsIntegrity(ChemCoreNext.Input input) {
    boolean rejected=false;
    try{ChemCoreNext.analyze(input);}
    catch(IllegalArgumentException e) {
      rejected=e.getMessage().startsWith("SNAPSHOT_HASH_MISMATCH");
    }
    check(rejected,"tampered or stale snapshot hash accepted");
  }
  public static void main(String[] args){
    var base=ChemCoreNext.analyze(input(new SourceCurveExpert.Curve[]{A2,A4,B2,B4},
        true,null,10));
    check(base.sourceCoherence.size()==1,"coherent one source root");
    check(base.sourceCoherence.get(0).ranked.size()==2,"all source alternatives preserved");
    check(base.sourceCoherence.get(0).selectedResearchUid!=null
       &&base.sourceCoherence.get(0).selectedResearchUid==101L,"expected source A selected");
    check(base.executionTrace.stream().filter(e->e.stage.equals("CURVE")).count()==4,
        "trace all four source curves");
    check(base.executionTrace.stream().filter(e->e.stage.equals("COHERENCE_HYPOTHESIS")).count()==2,
        "trace both source hypotheses");
    check(base.executionTrace.stream().anyMatch(e->e.stage.equals("LEDGER")),"ledger traced");
    check(base.executionTrace.stream().anyMatch(e->e.stage.equals("ARBITRATION")),"arbitration traced");
    check(base.executionTrace.stream().anyMatch(e->e.stage.equals("FINAL")
        &&e.status.equals("RESEARCH_CANDIDATES")),"final research status traced");
    boolean immutable=false;
    try{base.executionTrace.add(new NextAnalysisTrace.Event("TAMPER","","","",null,"BAD","",null));}
    catch(UnsupportedOperationException e){immutable=true;}
    check(immutable,"trace must be immutable");
    check(base.executionTrace.stream().filter(e->e.stage.equals("CURVE"))
        .allMatch(e->e.sourceOnlyDiagnosticValue!=null),"retain all source ppm diagnostics");
    check(!base.g3Qualified&&!base.molecularIdentityConfirmed&&!base.safetyAlarmAuthorized
        &&!base.ppmAuthorized,"G3 and hazardous outputs falsely promoted");
    var noCap=ChemCoreNext.analyze(input(new SourceCurveExpert.Curve[]{A2,A4,B2,B4},
        true,null,0));
    check(noCap.arbitration.displayCount==0 &&
        !noCap.arbitration.eligibleRanked.isEmpty() &&
        noCap.executionTrace.stream().filter(e->e.stage.equals("CURVE")).count()==4,
        "UI cap must not truncate computation or trace");
    var dupl=ChemCoreNext.analyze(input(new SourceCurveExpert.Curve[]{A2,A4,B2,B4,DUP2},
        true,null,10));
    var root=dupl.sourceCoherence.get(0);
    check(root.selectedResearchUid==null,"duplicate sensor model must not pick by lexical ID");
    check(root.decisionReason.equals("DUPLICATE_CURVE_MODELS_SAME_UID_AND_PHYSICAL_SENSOR_UNRESOLVED"),
        "duplicate reason visible");
    check(root.nonUniqueSourceModels.stream().anyMatch(x->x.contains("DUP2")),
        "duplicate model IDs retained");
    check(dupl.executionTrace.stream().filter(e->e.stage.equals("CURVE")).count()==5,
        "duplicate diagnostic kept, not hidden");
    check(dupl.arbitration.status==CoreContracts.DecisionStatus.UNKNOWN,
        "duplicate source cannot create confirmed hypothesis");
    var strict=ChemCoreNext.analyze(input(new SourceCurveExpert.Curve[]{A2,A4,B2,B4},
        false,null,10));
    check(strict.arbitration.status==CoreContracts.DecisionStatus.UNKNOWN,
        "unqualified fail closed");
    check(strict.executionTrace.stream().filter(e->e.stage.equals("CURVE"))
        .allMatch(e->e.reason.equals("SOURCE_R0_NOT_PROVEN_FOR_DEVICE")),
        "specific calibration refusal traced");
    throwsIntegrity(input(new SourceCurveExpert.Curve[]{A2,A4,B2,B4},true,"BAD_HASH",10));
    throwsIntegrity(input(new SourceCurveExpert.Curve[]{A2,A4,B2,B4},false,
         ChemCoreNext.snapshotHash(shot(2.,3.)),10));
    check(!base.sourceCoherence.get(0).ranked.get(0).sourceCurveIds.isEmpty(),
        "fitted hypothesis sources visible");
    System.out.println("NEXT_R3_TRACE_INTEGRITY_REGRESSION_PASS checks="+checks+
        " no_hardware_measured=true no_Tank_G3=true no_90pct_accuracy_claim=true");
  }
}
