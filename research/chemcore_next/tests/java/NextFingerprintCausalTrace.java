package com.hazrob.tank.chemcore.next;

import java.util.*;

 /** Read-only G1 diagnostic of the real V8 classes. No production changes.
  * Synthetic TRUE vs DISTRACTOR entries force evidence gates only to locate
  * where numerical fingerprint information stops influencing the ranking.
  * Real MQDS and RoboFusion examples are copied from their source-locked
  * stable reference inventory; never given fabricated molecular identities.
  */
public final class NextFingerprintCausalTrace {
  private static int checks;
  static void ok(boolean condition,String why){
    checks++;if(!condition)throw new AssertionError("CAUSE_TRACE_"+checks+" "+why);
  }
  static final FingerprintExpert.Qualification QUAL=new FingerprintExpert.Qualification(true,true);
  static final CoreContracts.DeviceProfile DEVICE=new CoreContracts.DeviceProfile(
     "SIM-DEVICE","SIM-MQ","SIM-FIRMWARE",List.of(
     new CoreContracts.SensorInstance("SIM:MQ3","MQ3","SIM","READY"),
     new CoreContracts.SensorInstance("SIM:MQ4","MQ4","SIM","READY")));
  static CoreContracts.Snapshot shot(String name,double r3,double r4){
    return new CoreContracts.Snapshot(name,DEVICE.deviceUid,CoreContracts.Mode.LAB,60000,25.,45.,true,List.of(
       new CoreContracts.Reading("SIM:MQ3",300,null,r3,10.,10.,
          CoreContracts.CalibrationReference.SOURCE_R0,"READY",CoreContracts.SampleQuality.VALID),
       new CoreContracts.Reading("SIM:MQ4",301,null,r4,10.,10.,
          CoreContracts.CalibrationReference.SOURCE_R0,"READY",CoreContracts.SampleQuality.VALID)
    ),"SIMULATED_STABLE_RESEARCH_NOT_RUNTIME");
  }
  static FingerprintExpert.Signature sig(String id,String root,String sensor,long uid,double mean){
    return new FingerprintExpert.Signature(id,root,"SIMULATED_COMMON_TWO_FEATURE_SPACE",
      sensor,uid,List.of("f0","f1"),List.of(mean,mean),List.of(1.,1.),2);
  }
  static ChemCoreNext.FingerprintBinding binding(FingerprintExpert.Signature s,String uid){
    var o=new FingerprintExpert.Observation("SIMULATED_COMMON_TWO_FEATURE_SPACE",s.sensorModel,
      "PRE_BOUND_OBSERVATION_NOT_SNAPSHOT",Map.of("f0",0.0,"f1",0.0),true);
    return new ChemCoreNext.FingerprintBinding(s,o,QUAL,uid,1.0,true);
  }
  static ChemCoreNext.Output run(String label,double rs3,double rs4,
        List<ChemCoreNext.FingerprintBinding> entries){
    var snap=shot(label,rs3,rs4);
    var materials=List.of(
        new ResearchCandidateGenerator.Material(100,"DISTRACTOR_LOWER_UID",
          ResearchCandidateGenerator.MaterialKind.MOLECULAR_CHEMICAL),
        new ResearchCandidateGenerator.Material(200,"IDENTICAL_TRUE_SIGNATURE",
          ResearchCandidateGenerator.MaterialKind.MOLECULAR_CHEMICAL));
    return ChemCoreNext.analyze(new ChemCoreNext.Input(DEVICE,snap,ChemCoreNext.snapshotHash(snap),
      materials,List.of(),entries,10));
  }
  static void report(String key,ChemCoreNext.Output o){
    StringJoiner j=new StringJoiner("|");
    for(var c:o.arbitration.eligibleRanked)j.add(c.uid+":"+c.independentPositiveRoots);
    System.out.println("TRACE "+key+" status="+o.arbitration.status+
       " ranked="+j+" raw_positive="+o.positiveModels+
       " roots="+o.graph.independentSourceRoots+
       " diag="+String.join("|",o.sourceDiagnostics));
  }
  public static void main(String[] args){
    var correct=sig("TRUE_SELF","STUDY_A","MQ4",200,0.0);
    var wrong=sig("DISTRACTOR","STUDY_B","MQ3",100,0.6);
    var trueB=binding(correct,"SIM:MQ4");
    var otherB=binding(wrong,"SIM:MQ3");
    var self=FingerprintExpert.compare(correct,trueB.observation,QUAL);
    var distract=FingerprintExpert.compare(wrong,otherB.observation,QUAL);
    ok(self.status==FingerprintExpert.Status.RESEARCH_RESIDUAL_ONLY&&self.researchNormalizedRmsDistance==0.0,
      "the identical reference must retain zero distance in compare");
    ok(Math.abs(distract.researchNormalizedRmsDistance-0.6)<1e-12,
      "other reference is 0.6 (worse) in the same source coordinate space");
    System.out.println("TRACE DIRECT_SELF_DISTANCE="+self.researchNormalizedRmsDistance+
      " DIRECT_DISTRACTOR_DISTANCE="+distract.researchNormalizedRmsDistance);
    var isolated=run("ISOLATED",10.,10.,List.of(trueB));
    report("isolated_true",isolated);
    ok(isolated.arbitration.eligibleRanked.size()==1&&isolated.arbitration.eligibleRanked.get(0).uid==200,
      "single identical fingerprint should be research candidate");
    var both=run("BOTH",10.,10.,List.of(trueB,otherB));
    report("both_competing",both);
    ok(both.arbitration.eligibleRanked.size()==2&&both.arbitration.eligibleRanked.get(0).uid==100,
      "the lower-UID less similar fingerprint incorrectly ranks first");
    var changed=run("CHANGED_RS",950.,0.055,List.of(trueB,otherB));
    report("modified_physical_reading_same_bound_observation",changed);
    ok(changed.arbitration.status==both.arbitration.status
       &&changed.arbitration.eligibleRanked.get(0).uid==both.arbitration.eligibleRanked.get(0).uid,
      "current physical Rs changed by orders of magnitude without changing bound observations: same ranks");
    var sameRoot=run("ONE_STUDY",10.,10.,
       List.of(binding(sig("TRUE","SAME_SOURCE","MQ4",200,0.0),"SIM:MQ4"),
               binding(sig("OTHER","SAME_SOURCE","MQ3",100,0.6),"SIM:MQ3")));
    report("same_source_multi_chemical",sameRoot);
    ok(sameRoot.graph.roots.size()==1
       &&sameRoot.graph.roots.get(0).alternativeChemicalUids.size()==2
       &&sameRoot.arbitration.eligibleRanked.isEmpty(),
      "source-root collapsing drops the identical true prototype when another gas in same study passes");
    var mqds=new FingerprintExpert.Signature("MQDS-ADC-1","MQDS_6400_SOURCE_ADC",
       "ADC_LOG_GEOMEAN_CLASS","MQ_ARRAY_8",null,
       List.of("0","1","2","3","4","5","6","7"),
       Arrays.asList(.14451613138166264,-.006557602534055202,null,
         -.018714246497049322,.034035113638107184,-.13017332874094964,null,-.023106067247713295),
       Arrays.asList(-.10078106448469856,-.10112804005375722,null,
         -.0240786770560068,-.11980312634069774,-.30558144410860255,null,-.07048066877423846),6);
    var actualObs=new FingerprintExpert.Observation("ADC_LOG_GEOMEAN_CLASS","MQ_ARRAY_8",
      "MQDS_SOURCE_SELF",Map.of("0",.14451613138166264,"1",-.006557602534055202,
        "3",-.018714246497049322,"4",.034035113638107184,
        "5",-.13017332874094964,"7",-.023106067247713295),true);
    var q=FingerprintExpert.compare(mqds,actualObs,QUAL);
    System.out.println("TRACE MQDS_ACTUAL_SELF="+q.status+" distance="+q.researchNormalizedRmsDistance+
        " uid="+mqds.canonicalChemicalUid+" reason="+q.reason);
    ok(q.status==FingerprintExpert.Status.INVALID_SOURCE_SCALES,
      "actual exact self-replay MQDS class must abstain due real absent/negative scales");
    var robo=new FingerprintExpert.Signature("ROBOFUSION-1","ROBOFUSION_H1_OPS",
      "OPERATIONAL_LOG1PLUS_CLR","MQ_ARRAY_6",null,
      List.of("0","1","2","3","4","5"),
      List.of(-.16218385913659927,.6895123557707743,-.4128213009720952,
        -.6769106258203069,.29794005163845416,-.23639028919702562),
      List.of(.8886916640838581,.5262006722158789,.2560133917667277,
        .41780831256948464,.39548286634707874,.5210604240378981),6);
    var obs=new FingerprintExpert.Observation("OPERATIONAL_LOG1PLUS_CLR",
      "MQ_ARRAY_6","ROBO_SOURCE_SELF",
      Map.of("0",-.16218385913659927,"1",.6895123557707743,
        "2",-.4128213009720952,"3",-.6769106258203069,
        "4",.29794005163845416,"5",-.23639028919702562),true);
    var qo=FingerprintExpert.compare(robo,obs,QUAL);
    System.out.println("TRACE ROBOFUSION_ACTUAL_SELF="+qo.status+
      " distance="+qo.researchNormalizedRmsDistance+" uid="+robo.canonicalChemicalUid);
    ok(qo.status==FingerprintExpert.Status.RESEARCH_RESIDUAL_ONLY
       &&qo.researchNormalizedRmsDistance==0.0&&robo.canonicalChemicalUid==null,
       "actual operational class self-match is zero but cannot become chemical identity");
    System.out.println("CHEMCORE_NEXT_FINGERPRINT_CAUSAL_TRACE_PASS checks="+checks+
      " changes_to_production_engine=0 validated_G3=false");
  }
}
