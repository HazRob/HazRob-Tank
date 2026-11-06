package com.hazrob.tank.chemcore.next;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Exact production ChemCoreNext.analyze() research trace on source metadata.
 * Never writes to app, Stable, firmware or device. "Forced" references,
 * hypothetical observed Rs and synthetic UID are intentionally artificial.
 * This is NOT empirical gas-identification accuracy.
 */
public final class NextReferenceTraceReplay {
  private static int fingerprints=0, curves=0, exactFp=0, fpBadScales=0;
  private static int strictUnknown=0, strictQuarantine=0, strictSourceGate=0;
  private static int hypotheticalFpMatch=0, hypotheticalCurveMatch=0;
  private static int algebraicValid=0, algebraicUnavailable=0, r0Comparisons=0;
  private static int ambiguousTieFirstWrong=0;
  private static final String[] MODELS={"MQ2","MQ3","MQ4","MQ5","MQ7","MQ8","MQ9","MQ135"};
  private static final double[] AIR={92.1417313,25.5138416,37.8870316,158.0950317,
                                     62.5898705,36.5759659,56.4506149,113.6592255};
  private static double air(String m) {
    for(int i=0;i<MODELS.length;i++)if(MODELS[i].equals(m))return AIR[i];
    return 20.0; // synthetic-only source metadata device, never Tank physical claim
  }
  private static Double number(String v) { return v==null||v.isBlank()?null:Double.valueOf(v); }
  private static List<Double> numbers(String v) {
    var a=new ArrayList<Double>();
    for(String x:v.split(";",-1))a.add(number(x));return a;
  }
  private static String t(Object x) {
    return x==null?"":x.toString().replace('\t',' ').replace('\n',' ');
  }
  private static void emit(List<String> out,Object... f) {
    var x=new ArrayList<String>();for(Object v:f)x.add(t(v));
    out.add(String.join("\t",x));
  }
  private static CoreContracts.DeviceProfile device(String model){
    return new CoreContracts.DeviceProfile("SYNTHETIC_TRACE_DEVICE","REPLAY","NO_PHYSICAL_SENSOR",
        List.of(new CoreContracts.SensorInstance("TRACE-SENSOR",model,"SOURCE_ONLY","READY")));
  }
  private static CoreContracts.Snapshot snapshot(String model,double rs,double sourceR0){
    return new CoreContracts.Snapshot("TRACE-"+model,"SYNTHETIC_TRACE_DEVICE",CoreContracts.Mode.LAB,
        60000,23.0,40.0,true,
        List.of(new CoreContracts.Reading("TRACE-SENSOR",null,null,rs,air(model),sourceR0,
            CoreContracts.CalibrationReference.SOURCE_R0,"READY",CoreContracts.SampleQuality.VALID)),
        "RESEARCH_SYNTHETIC_TRACE");
  }
  private static ChemCoreNext.Output run(String model,double rs,double sourceR0,
      List<ChemCoreNext.CurveBinding> cv,List<ChemCoreNext.FingerprintBinding> fp,
      List<ResearchCandidateGenerator.Material> catalog){
    var d=device(model);var s=snapshot(model,rs,sourceR0);
    var in=new ChemCoreNext.Input(d,s,ChemCoreNext.snapshotHash(s),catalog,cv,fp,4);
    var x=ChemCoreNext.analyze(in);
    if(x.ppmAuthorized||x.g3Qualified||x.safetyAlarmAuthorized||x.molecularIdentityConfirmed)
      throw new AssertionError("research promoted physical certification");
    return x;
  }
  private static FingerprintExpert.Signature signature(String id,String root,String model,
      String space,Long uid,List<Double> mean,List<Double> sd){
    var keys=new ArrayList<String>();
    for(int i=0;i<mean.size();i++)keys.add("COORD_"+i);
    return new FingerprintExpert.Signature(id,root,space,model,uid,keys,mean,sd,1);
  }
  private static FingerprintExpert.Observation observation(String id,String model,String space,
      List<Double> mean){
    var coords=new HashMap<String,Double>();
    for(int i=0;i<mean.size();i++)if(mean.get(i)!=null)coords.put("COORD_"+i,mean.get(i));
    return new FingerprintExpert.Observation(space,model,"SOURCE_SELF_"+id,coords,true);
  }
  private static void fingerprint(List<String> result,String[] p,int index){
    fingerprints++;
    String id=p[1],root=p[2],model=p[3],space=p[4],label=p[5];
    var mean=numbers(p[6]);var sd=numbers(p[7]);
    var original=signature(id,root,model,space,null,mean,sd);
    var obs=observation(id,model,space,mean);
    var strictExpert=FingerprintExpert.compare(original,obs,FingerprintExpert.Qualification.unverified());
    var diagnosticExpert=FingerprintExpert.compare(original,obs,
        new FingerprintExpert.Qualification(true,true));
    if(diagnosticExpert.status==FingerprintExpert.Status.RESEARCH_RESIDUAL_ONLY){
      exactFp++;
      if(Math.abs(diagnosticExpert.researchNormalizedRmsDistance)>1e-12)
        throw new AssertionError("self fingerprint not zero: "+id);
    } else if(diagnosticExpert.status==FingerprintExpert.Status.INVALID_SOURCE_SCALES) fpBadScales++;
    var binding=new ChemCoreNext.FingerprintBinding(original,obs,
        FingerprintExpert.Qualification.unverified(),"TRACE-SENSOR",null,false);
    var strict=run(model,20.0,20.0,List.of(),List.of(binding),List.of());
    if(strict.arbitration.status!=CoreContracts.DecisionStatus.UNKNOWN)
      throw new AssertionError("unqualified source became eligible "+id);
    strictUnknown++;
    String firstStage=NextResearchSourceQuarantine.blocked(root,id,space)?
        "SOURCE_QUARANTINE":"FINGERPRINT_SOURCE_TRANSFORM_QUALIFICATION";
    if(firstStage.equals("SOURCE_QUARANTINE")) strictQuarantine++;
    else strictSourceGate++;
    String virtual="SKIPPED_UNQUALIFIED_OR_INVALID_SOURCE_SCALES";
    String ablation="NOT_RUN";
    if(diagnosticExpert.status==FingerprintExpert.Status.RESEARCH_RESIDUAL_ONLY){
      long uid=10000L+index;
      String testRoot="TRACE_SYNTHETIC_"+id.replaceAll("[^a-zA-Z0-9]","_");
      var clone=signature(id,testRoot,model,space,uid,mean,sd);
      var q=new FingerprintExpert.Qualification(true,true);
      var b=new ChemCoreNext.FingerprintBinding(clone,obs,q,"TRACE-SENSOR",0.25,true);
      var m=new ResearchCandidateGenerator.Material(uid,"SYNTHETIC_"+label,
          ResearchCandidateGenerator.MaterialKind.MOLECULAR_CHEMICAL);
      var x=run(model,20.0,20.0,List.of(),List.of(b),List.of(m));
      long top=x.arbitration.display.isEmpty()?0:x.arbitration.display.get(0).uid;
      virtual=x.arbitration.status+":"+top;
      if(top!=uid)throw new AssertionError("synthetic isolated self-match "+id);
      hypotheticalFpMatch++;
      var wrongId=1L;
      var bad=signature("DISTRACTOR_"+id,"TRACE_DISTRACTOR_"+id,model,space,wrongId,mean,sd);
      var bb=new ChemCoreNext.FingerprintBinding(bad,obs,q,"TRACE-SENSOR",0.25,true);
      var mm=new ResearchCandidateGenerator.Material(wrongId,"INDISTINGUISHABLE_DISTRACTOR",
          ResearchCandidateGenerator.MaterialKind.MOLECULAR_CHEMICAL);
      var tie=run(model,20.0,20.0,List.of(),List.of(b,bb),List.of(m,mm));
      long topTie=tie.arbitration.display.isEmpty()?0:tie.arbitration.display.get(0).uid;
      ablation=tie.arbitration.status+":"+topTie+":SAME_DISTANCE";
      if(topTie==wrongId)ambiguousTieFirstWrong++;
    }
    emit(result,"FINGERPRINT",id,label,model,root,firstStage,
        strictExpert.status,diagnosticExpert.status,
        diagnosticExpert.researchNormalizedRmsDistance,strict.arbitration.status,
        strict.arbitration.displayCount,virtual,ablation,
        "ORIGINAL_METADATA_NO_SYNTHETIC_SOURCE_QUALIFICATION");
  }
  private static MQSensorsLibExampleCatalog.Model example(String id){
    for(var m:MQSensorsLibExampleCatalog.all())if(m.id.equals(id))return m;
    return null;
  }
  private static void curve(List<String> result,List<String> r0,String[] p,int index){
    curves++;
    String id=p[1],root=p[2],model=p[3],label=p[4],equation=p[5];
    double a=Double.parseDouble(p[6]), b=Double.parseDouble(p[7]);
    Double scale=number(p[8]), min=number(p[9]), max=number(p[10]);
    var eq=equation.equals("PPM")?
       SourceCurveExpert.Equation.PPM_EQUALS_A_TIMES_RATIO_POWER_B:
       SourceCurveExpert.Equation.RATIO_EQUALS_A_TIMES_PPM_OVER_SCALE_POWER_B;
    var original=new SourceCurveExpert.Curve(id,root,model,label,null,eq,a,b,scale,min,max,"");
    var strictReading=snapshot(model,air(model),air(model)).readings.get(0);
    var direct=SourceCurveExpert.evaluate(original,device(model).installedSensors.get(0),
        strictReading,SourceCurveExpert.Qualification.unverified());
    var strict=run(model,air(model),air(model),
        List.of(new ChemCoreNext.CurveBinding(original,"TRACE-SENSOR",
            SourceCurveExpert.Qualification.unverified())),List.of(),List.of());
    if(strict.arbitration.status!=CoreContracts.DecisionStatus.UNKNOWN)
      throw new AssertionError("unqualified curve promoted "+id);
    strictUnknown++;strictSourceGate++;
    String algebraic="SOURCE_SCALE_UNAVAILABLE";
    if(eq==SourceCurveExpert.Equation.PPM_EQUALS_A_TIMES_RATIO_POWER_B || scale!=null) {
      double srcPpm=100.;
      double predicted=SourceCurveExpert.forwardSourceRatio(original,srcPpm);
      double inverse=SourceCurveExpert.inverseSourcePpm(original,predicted);
      if(Math.abs(inverse/srcPpm-1)>1e-9)
        throw new AssertionError("forward inverse parity "+id);
      algebraic="PASS_SELF_ROUNDTRIP";
      algebraicValid++;
    } else algebraicUnavailable++;
    String counterfactual="NOT_ELIGIBLE";
    var source=example(id);
    if(source!=null && source.installedOnTank){
      double K=source.cleanAirK, rAir=air(model);
      double refR0=rAir/K;
      double inputPpm=100.0;
      var artificial=new SourceCurveExpert.Curve(
          "TRACE_"+id,"TRACE_ISOLATED_"+id,model,label,20000L+index,eq,a,b,scale,
          1e-6,1e12,"READY");
      double ratio=SourceCurveExpert.forwardSourceRatio(artificial,inputPpm);
      double syntheticRs=ratio*refR0;
      var q=new SourceCurveExpert.Qualification(true,true,true,true);
      var xReading=snapshot(model,syntheticRs,refR0).readings.get(0);
      var trueResult=SourceCurveExpert.evaluate(artificial,device(model).installedSensors.get(0),
          xReading,q);
      var wrongReading=snapshot(model,syntheticRs,rAir).readings.get(0);
      var wrongResult=SourceCurveExpert.evaluate(artificial,device(model).installedSensors.get(0),
          wrongReading,q);
      if(trueResult.researchOnlyPpm==null||wrongResult.researchOnlyPpm==null||
         Math.abs(trueResult.researchOnlyPpm/inputPpm-1)>1e-9)
         throw new AssertionError("source R0 transform failed "+id);
      var material=new ResearchCandidateGenerator.Material(20000L+index,"SYNTHETIC_"+label,
          ResearchCandidateGenerator.MaterialKind.MOLECULAR_CHEMICAL);
      var correct=run(model,syntheticRs,refR0,
          List.of(new ChemCoreNext.CurveBinding(artificial,"TRACE-SENSOR",q)),
          List.of(),List.of(material));
      var wrong=run(model,syntheticRs,rAir,
          List.of(new ChemCoreNext.CurveBinding(artificial,"TRACE-SENSOR",q)),
          List.of(),List.of(material));
      long top=correct.arbitration.display.isEmpty()?0:correct.arbitration.display.get(0).uid;
      if(top!=20000L+index)throw new AssertionError("synthetic curve candidate "+id);
      hypotheticalCurveMatch++;
      counterfactual="CORRECT_AND_WRONG_R0_"+correct.arbitration.status+"_AND_"+wrong.arbitration.status;
      emit(r0,id,label,model,K,rAir,refR0,syntheticRs,inputPpm,
          trueResult.researchOnlyPpm,wrongResult.researchOnlyPpm,
          wrongResult.researchOnlyPpm/trueResult.researchOnlyPpm,
          correct.arbitration.status,wrong.arbitration.status,
          "SYNTHETIC_SAME_SOURCE_EQUATION_SELF_TEST_NOT_GAS_ACCURACY");
      r0Comparisons++;
    } else if(source!=null) counterfactual="SENSOR_NOT_INSTALLED";
    emit(result,"CURVE",id,label,model,root,"R0_SOURCE_QUALIFICATION",
        direct.decision,direct.abstention,algebraic,strict.arbitration.status,
        strict.arbitration.displayCount,counterfactual,"",
        "ORIGINAL_METADATA_UNKNOWN_OR_UNVERIFIED_SOURCE_RANGE_AND_UID");
  }
  private static void control(List<String> report){
    var a=new SourceCurveExpert.Curve("ALT-A","TRACE_SHARED_ROOT","MQ4","CH4",900001L,
        SourceCurveExpert.Equation.PPM_EQUALS_A_TIMES_RATIO_POWER_B,
        1012.7,-2.786,null,1e-6,1e12,"READY");
    var b=new SourceCurveExpert.Curve("ALT-B","TRACE_SHARED_ROOT","MQ4","H2",900002L,
        SourceCurveExpert.Equation.PPM_EQUALS_A_TIMES_RATIO_POWER_B,
        100.0,-1.0,null,1e-6,1e12,"READY");
    var q=new SourceCurveExpert.Qualification(true,true,true,true);
    var cv=List.of(new ChemCoreNext.CurveBinding(a,"TRACE-SENSOR",q),
        new ChemCoreNext.CurveBinding(b,"TRACE-SENSOR",q));
    var materials=List.of(
        new ResearchCandidateGenerator.Material(900001L,"SYNTHETIC_CH4",
          ResearchCandidateGenerator.MaterialKind.MOLECULAR_CHEMICAL),
        new ResearchCandidateGenerator.Material(900002L,"SYNTHETIC_H2",
          ResearchCandidateGenerator.MaterialKind.MOLECULAR_CHEMICAL));
    var out=run("MQ4",20.,20.,cv,List.of(),materials);
    if(out.arbitration.status!=CoreContracts.DecisionStatus.UNKNOWN ||
       out.graph.roots.size()!=1 ||!out.graph.roots.get(0).sharedHypothesisRoot)
       throw new AssertionError("one original-source family multi-labels must be ambiguous");
    emit(report,"CURVE_MULTILABEL_CONTROL","SYNTHETIC_SHARED_SOURCE","CH4_AND_H2","MQ4",
      "TRACE_SHARED_ROOT","EVIDENCE_ROOT_MULTI_IDENTITY",
      out.positiveModels,out.graph.roots.size(),"ONE_ROOT_TWO_UIDS",
      out.arbitration.status,out.arbitration.displayCount,"NONE",
      "AMBIGUOUS_AT_SOURCE_LEDGER","CONTROL_NOT_REAL_GAS");
  }
  public static void main(String[] args)throws Exception {
    if(args.length!=2)throw new IllegalArgumentException("source_cases.tsv outputdir");
    var output=Path.of(args[1]);Files.createDirectories(output);
    var traces=new ArrayList<String>();var r0=new ArrayList<String>();
    traces.add("kind\tid\tlabel\tmodel\troot\tfirst_block_stage\texpert_status\texpert_reason\texpert_value\tfinal_status\tfinal_candidate_count\tcounterfactual_isolation\tambiguous_same_distance_control\tnote");
    r0.add("id\tlabel\tmodel\tK_example\tr_air_kohm\tr0_source_kohm\tsynthetic_rs_kohm\tsynthetic_input_ppm\tcorrect_r0_ppm\twrong_r0_ppm\twrong_to_correct_ratio\tcorrect_candidate_status\twrong_candidate_status\tcaveat");
    int idx=0;
    for(String line:Files.readAllLines(Path.of(args[0]),StandardCharsets.UTF_8)){
      if(line.isBlank())continue;
      String[] cols=line.split("\t",-1);idx++;
      if("F".equals(cols[0]))fingerprint(traces,cols,idx);
      else if("C".equals(cols[0]))curve(traces,r0,cols,idx);
      else throw new IllegalStateException("bad kind");
    }
    control(traces);
    if(fingerprints!=24||curves!=109||r0Comparisons!=13 ||algebraicValid!=20)
       throw new AssertionError("expected counts: "+fingerprints+"/"+curves+"/r0 "+r0Comparisons+
          "/algebraic "+algebraicValid);
    Files.write(output.resolve("all_reference_stage_trace.tsv"),traces,StandardCharsets.UTF_8);
    Files.write(output.resolve("dual_r0_synthetic_counterfactual.tsv"),r0,StandardCharsets.UTF_8);
    var summary=String.format(Locale.ROOT,
      "TRACE_SOURCE_SELF_REPLAY_PASS fingerprints=%d curves=%d total=%d strict_unknown=%d "+
      "source_quarantine=%d first_source_gate=%d exact_distance_zero=%d invalid_fingerprint_scales=%d "+
      "isolation_fingerprint_match=%d isolation_curve_match=%d algebraic_roundtrips=%d "+
      "algebraic_unavailable=%d dual_r0=%d ambiguous_tie_arbiter_uid_first=%d "+
      "real_tank_G3=0 empirical_accuracy_unmeasured=true",
      fingerprints,curves,fingerprints+curves,strictUnknown,strictQuarantine,strictSourceGate,
      exactFp,fpBadScales,hypotheticalFpMatch,hypotheticalCurveMatch,
      algebraicValid,algebraicUnavailable,r0Comparisons,ambiguousTieFirstWrong);
    Files.writeString(output.resolve("summary.txt"),summary+"\n",StandardCharsets.UTF_8);
    System.out.println(summary);
  }
}
