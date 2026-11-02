package com.hazrob.tank.chemcore.next;
import java.util.*;
public final class ChemCoreNextOrchestratorSmoke {
  private static int n;
  private static void check(boolean ok,String why){
    n++;if(!ok)throw new AssertionError("NEXT integrated "+n+": "+why);
  }
  private static CoreContracts.DeviceProfile device(){
    return new CoreContracts.DeviceProfile("physical-Tank-1","board-v6","v6-quick",
       List.of(new CoreContracts.SensorInstance("tank-mq4","MQ4","slot","READY")));
  }
  private static CoreContracts.Snapshot capture(double rs,boolean powered){
    return new CoreContracts.Snapshot("capture-001","physical-Tank-1",CoreContracts.Mode.LAB,
       120000L,25.,55.,powered,List.of(new CoreContracts.Reading(
       "tank-mq4",12222,2.0,rs,37.8870316,null,
       CoreContracts.CalibrationReference.AIR_RS_MEAN,"READY",
       CoreContracts.SampleQuality.VALID)),"stable-manifest-original-unverified");
  }
  private static List<ResearchCandidateGenerator.Material> catalog(){
    List<ResearchCandidateGenerator.Material> all=new ArrayList<>();
    for(long id=1;id<=736;id++)all.add(new ResearchCandidateGenerator.Material(id,
      "canonical-"+id,id>734?ResearchCandidateGenerator.MaterialKind.GENERAL_CLASS:
        ResearchCandidateGenerator.MaterialKind.MOLECULAR_CHEMICAL));
    return all;
  }
  private static SourceCurveExpert.Curve curve(){
    return new SourceCurveExpert.Curve("source-mq4","SOURCE_ORIGINAL_MQ4","MQ4",
      "methane",200L,SourceCurveExpert.Equation.RATIO_EQUALS_A_TIMES_PPM_OVER_SCALE_POWER_B,
       1.5,-.5,100.,10.,5000.,"READY");
  }
  private static ChemCoreNext.Output run(double rs,boolean relay,
       List<ChemCoreNext.CurveBinding> c,List<ChemCoreNext.FingerprintBinding> f){
    return ChemCoreNext.analyze(new ChemCoreNext.Input(device(),capture(rs,relay),
        "sha-owner",catalog(),c,f,10));
  }
  public static void main(String[] args){
    var no=run(37.8870316,true,List.of(),List.of());
    check(no.research.status==CoreContracts.DecisionStatus.UNKNOWN,"real owner's R_air alone no source gases");
    check(no.research.rankedEligibleCandidates.isEmpty(),"no source no candidates");
    var cb=new ChemCoreNext.CurveBinding(curve(),"tank-mq4",
         SourceCurveExpert.Qualification.unverified());
    var blocked=run(37.8870316,true,List.of(cb),List.of());
    check(blocked.research.status==CoreContracts.DecisionStatus.UNKNOWN,
       "do not use source R0 with owner's independent R_air");
    check(blocked.sourceDiagnostics.get(0).contains("CURVE_NOT_ELIGIBLE"),
       "source calibration blocked with explicit reason");
    var relay=run(37.8870316,false,List.of(cb),List.of());
    check(relay.research.status==CoreContracts.DecisionStatus.UNKNOWN,
       "relay off forbids physically active source inference");
    var sig=new FingerprintExpert.Signature("original-u","SOURCE-PROTOTYPE",
       "SOURCE_COMPATIBLE_FEATURE_SPACE","MQ4",200L,
       List.of("feature"),List.of(.1),List.of(.05),1);
    var obs=new FingerprintExpert.Observation("SOURCE_COMPATIBLE_FEATURE_SPACE",
       "MQ4","actual-source-exposure",Map.of("feature",.11),true);
    var fb=new ChemCoreNext.FingerprintBinding(sig,obs,
       FingerprintExpert.Qualification.unverified(),"tank-mq4",1.,false);
    var unqualified=run(37.8870316,true,List.of(),List.of(fb));
    check(unqualified.research.status==CoreContracts.DecisionStatus.UNKNOWN,
       "unverified source fingerprint threshold and physical transform abstain");
    var verifiedExternalOnly=new ChemCoreNext.FingerprintBinding(sig,obs,
       new FingerprintExpert.Qualification(true,true),"tank-mq4",1.,true);
    var positive=run(37.8870316,true,List.of(),List.of(verifiedExternalOnly));
    check(positive.research.status==CoreContracts.DecisionStatus.RESEARCH_CANDIDATES,
       "strict source-qualified reference research hypothesis");
    check(positive.research.rankedEligibleCandidates.get(0).canonicalChemicalUid==200L,
       "nonlegacy canonical UID enters graph");
    check(positive.graph.independentSourceRoots==1,"no inflation of root count");
    check(positive.positiveModels==1,"source verified research model accepted");
    check(!positive.safetyAlarmAuthorized&&!positive.ppmAuthorized
       &&!positive.molecularIdentityConfirmed&&!positive.g3Qualified,
       "never converts research model to certified safety");
    // Same original source appears twice: only one root counted.
    positive=run(37.8870316,true,List.of(),List.of(verifiedExternalOnly,verifiedExternalOnly));
    check(positive.graph.independentSourceRoots==1,"duplicate fingerprint same root one root");
    var high=new ChemCoreNext.FingerprintBinding(sig,
       new FingerprintExpert.Observation("SOURCE_COMPATIBLE_FEATURE_SPACE","MQ4",
          "exposure-X",Map.of("feature",9.0),true),
       new FingerprintExpert.Qualification(true,true),"tank-mq4",1.,true);
    var abstain=run(37.8870316,true,List.of(),List.of(high));
    check(abstain.research.status==CoreContracts.DecisionStatus.UNKNOWN,
       "outlier source threshold cannot force chemical absence");
    check(abstain.sourceDiagnostics.get(0).contains("HIGH_RESIDUAL_UNKNOWN"),
       "separate high residual uncertainty");
    check(!no.research.physicallyValidatedG3&&!positive.research.reportablePpm,
       "software-only synthetic source qualification never physical G3");
    boolean fails=false;try{ChemCoreNext.analyze(null);}
       catch(IllegalArgumentException e){fails=true;}
    check(fails,"missing source snapshot fails closed");
    System.out.println("CHEMCORE_NEXT_P06_ORCHESTRATOR_PASS assertions="+n+
       " single_entrypoint=true source_experts=true graph=true arbiter=true G3=false");
  }
}
