package com.hazrob.tank.chemcore.next;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Executes ORIGINAL ChemCoreNext.analyze using EXACT Stable 49-prior SOURCE
 * model-generated sensor vectors (not gas measurements). No source or device
 * calibration is certified; only hypothetical gate bypass is used to isolate
 * ranking effects. Every scenario retains physical K/R_air assumption text.
 */
public final class NextStable49MatrixReplay {
  private record Prior(int idx,long uid,String label,String sensor,String root,
      String id,double a,double b,double lo,double hi) {}
  private static final Map<String,Double> AIR=Map.of(
      "MQ2",92.1417313,"MQ3",25.5138416,"MQ4",37.8870316,
      "MQ5",158.0950317,"MQ7",62.5898705,"MQ8",36.5759659,
      "MQ9",56.4506149,"MQ135",113.6592255);
  private static final Map<String,Double> K=Map.of(
      "MQ2",9.83,"MQ3",60.0,"MQ4",4.4,
      "MQ5",6.5,"MQ7",27.5,"MQ8",70.,
      "MQ9",9.6,"MQ135",3.6);
  private static final String HEATER="SYNTHETIC_HEATER_UNVERIFIED";
  private static final String DEVICE="STABLE49_VIRTUAL_TANK_NO_PHYSICAL_GAS";
  private static final SourceCurveExpert.Qualification SYNTHETIC_GATE=
      new SourceCurveExpert.Qualification(true,true,true,true);
  private static final List<String> MODELS=List.of("MQ2","MQ3","MQ4","MQ5","MQ7","MQ8","MQ9","MQ135");
  private static String[] fields(String s){return s.split("\t",-1);}
  private static List<Prior> priors(Path file)throws Exception {
    var lines=Files.readAllLines(file,StandardCharsets.UTF_8);
    if(lines.size()!=50)throw new IllegalStateException("STABLE 49 source priors required");
    var out=new ArrayList<Prior>();
    for(String line:lines.subList(1,lines.size())) {
      String[] x=fields(line);
      out.add(new Prior(Integer.parseInt(x[0]),100000L+Long.parseLong(x[1]),
          x[2],x[3],x[4],x[5],Double.parseDouble(x[6]),Double.parseDouble(x[7]),
          Double.parseDouble(x[8]),Double.parseDouble(x[9])));
    }
    return out;
  }
  private static String t(Object x){return x==null?"":x.toString().replace('\t',' ');}
  private static void emit(List<String> rows,Object...values) {
    var a=new ArrayList<String>();for(Object v:values)a.add(t(v));
    rows.add(String.join("\t",a));
  }
  private static final CoreContracts.DeviceProfile PROFILE=buildProfile();
  private static CoreContracts.DeviceProfile buildProfile(){
    var sensors=new ArrayList<CoreContracts.SensorInstance>();
    for(String model:MODELS)sensors.add(new CoreContracts.SensorInstance(
        "SIM-"+model,model,"NO_REAL_ADC_OR_RL",HEATER));
    return new CoreContracts.DeviceProfile(DEVICE,"SYNTHETIC","NOT_FIRMWARE",sensors);
  }
  private static Map<String,Double> measurements(String s){
    var out=new LinkedHashMap<String,Double>();
    for(String tok:s.split(";")){
      var x=tok.split(":");if(x.length!=2)throw new IllegalArgumentException("invalid token "+tok);
      double rs=Double.parseDouble(x[1]);if(!(rs>0&&Double.isFinite(rs)))throw new IllegalArgumentException("Rs");
      out.put(x[0],rs);
    }
    return out;
  }
  private static CoreContracts.Snapshot snapshot(String id,String mode,Map<String,Double> rs){
    var readings=new ArrayList<CoreContracts.Reading>();
    for(var x:rs.entrySet()){
      String sensor=x.getKey();double ref=AIR.get(sensor);
      boolean strict=mode.equals("AIR_RS_MEAN_STRICT_UNQUALIFIED");
      double denominator=switch(mode) {
        case "R0_SOURCE_K_CONDITIONAL" -> ref/K.get(sensor);
        case "DEVICE_R_AIR_AS_SOURCE_R0" -> ref;
        case "FIXED_10K_FOR_ALL" -> 10.;
        case "MQ2_K_FOR_ALL_WRONG" -> ref/K.get("MQ2");
        case "AIR_RS_MEAN_STRICT_UNQUALIFIED" -> ref;
        default -> throw new IllegalArgumentException("R0 mode "+mode);
      };
      readings.add(new CoreContracts.Reading("SIM-"+sensor,null,null,x.getValue(),ref,
          strict?null:denominator,
          strict?CoreContracts.CalibrationReference.AIR_RS_MEAN:CoreContracts.CalibrationReference.SOURCE_R0,
          HEATER,CoreContracts.SampleQuality.VALID));
    }
    return new CoreContracts.Snapshot(id,DEVICE,CoreContracts.Mode.LAB,60000,
        24.,45.,true,readings,"STABLE_1_0_0_HR069R1_SYNTHETIC_NOT_DEVICE_PROVEN");
  }
  public static void main(String[] args)throws Exception{
    if(args.length!=3)throw new IllegalArgumentException("priors cases out.tsv");
    List<Prior> source=priors(Path.of(args[0]));
    var catalog=new ArrayList<ResearchCandidateGenerator.Material>();
    var seen=new HashSet<Long>();
    for(Prior p:source)if(seen.add(p.uid))
      catalog.add(new ResearchCandidateGenerator.Material(p.uid,p.label,
          p.uid==100135L||p.uid==100139L?
          ResearchCandidateGenerator.MaterialKind.GENERAL_CLASS:
          ResearchCandidateGenerator.MaterialKind.MOLECULAR_CHEMICAL));
    if(catalog.size()!=13)throw new IllegalStateException("13 families expected "+catalog.size());
    var blockedBindings=new ArrayList<ChemCoreNext.CurveBinding>();
    var testBindings=new ArrayList<ChemCoreNext.CurveBinding>();
    for(Prior p:source) {
      // Stable prior log10(ratio)=intercept + b*log10(ppm)
      // therefore EXACT equivalent ratio=a*ppm^b, using reparameterized 1ppm
      // mathematical coordinate; NOT claiming original source physical scale.
      var curve=new SourceCurveExpert.Curve("STABLE49_"+p.id+"_"+p.idx,
          p.root,p.sensor,p.label,p.uid,
          SourceCurveExpert.Equation.RATIO_EQUALS_A_TIMES_PPM_OVER_SCALE_POWER_B,
          p.a,p.b,1.,p.lo,p.hi,HEATER);
      blockedBindings.add(new ChemCoreNext.CurveBinding(curve,"SIM-"+p.sensor,
          SourceCurveExpert.Qualification.unverified()));
      testBindings.add(new ChemCoreNext.CurveBinding(curve,"SIM-"+p.sensor,SYNTHETIC_GATE));
    }
    var lines=Files.readAllLines(Path.of(args[1]),StandardCharsets.UTF_8);
    var out=new ArrayList<String>();
    emit(out,"case_id","expected_legacy","material","source_ppm","perturbation",
      "replicate","R0_mode","observed_channels","strict_status","strict_rank1",
      "synthetic_qualified_status","synthetic_qualified_rank1",
      "eligible_candidates","source_roots","positive_curve_models",
      "expected_material_present","first_deviation_reason","note");
    int n=0,strictUnknown=0,simCandidate=0,simCorrect=0,ambiguous=0,modelsPositive=0;
    for(String line:lines.subList(1,lines.size())){
      String[] v=fields(line);
      String id=v[0],mode=v[6];
      long expected=100000L+Long.parseLong(v[1]);
      var shot=snapshot(id,mode,measurements(v[7]));
      var strict=ChemCoreNext.analyze(new ChemCoreNext.Input(PROFILE,shot,
          ChemCoreNext.snapshotHash(shot),catalog,blockedBindings,List.of(),6));
      var research=ChemCoreNext.analyze(new ChemCoreNext.Input(PROFILE,shot,
          ChemCoreNext.snapshotHash(shot),catalog,testBindings,List.of(),6));
      if(strict.ppmAuthorized||strict.g3Qualified||research.ppmAuthorized||research.g3Qualified
          ||research.safetyAlarmAuthorized||research.molecularIdentityConfirmed)
        throw new AssertionError("false certification");
      long strictTop=strict.arbitration.display.isEmpty()?0:strict.arbitration.display.get(0).uid;
      long simTop=research.arbitration.display.isEmpty()?0:research.arbitration.display.get(0).uid;
      if(strict.arbitration.status==CoreContracts.DecisionStatus.UNKNOWN)strictUnknown++;
      if(simTop>0)simCandidate++;
      if(simTop==expected)simCorrect++;
      if(research.graph.hasUnresolvedAlternatives)ambiguous++;
      modelsPositive+=research.positiveModels;
      boolean present=research.arbitration.eligibleRanked.stream().anyMatch(x->x.uid==expected);
      String cause;
      if(mode.equals("AIR_RS_MEAN_STRICT_UNQUALIFIED"))cause="SOURCE_R0_DEFINITION_NOT_MATCHED";
      else if(research.positiveModels==0)cause="SOURCE_CURVES_OUT_OF_DOMAIN_OR_MISSING";
      else if(research.graph.hasUnresolvedAlternatives)cause="SAME_SOURCE_MULTIPLE_IDENTITIES_AMBIGUOUS";
      else if(!present)cause="EXPECTED_IDENTITY_INELIGIBLE_AFTER_EVIDENCE_FUSION";
      else if(simTop!=expected)cause="ARBITER_RANKING_OR_COMPETING_SOURCE_COUNT";
      else cause="SYNTHETIC_SOURCE_SELF_CONSISTENT";
      emit(out,id,v[1],v[2],v[3],v[4],v[5],mode,v[8],
          strict.arbitration.status,strictTop,research.arbitration.status,simTop,
          research.arbitration.eligibleCount,research.graph.independentSourceRoots,
          research.positiveModels,present,cause,
          "NO_REAL_GAS; ALL_SOURCE_QUALIFICATIONS_INJECTED_SYNTHETICALLY");
      n++;
    }
    if(n<1000)throw new AssertionError("matrix must be diverse "+n);
    Files.write(Path.of(args[2]),out,StandardCharsets.UTF_8);
    System.out.println("STABLE49_ACTUAL_CHEMCORE_NEXT_MATRIX_PASS cases="+n+
        " original_source_models="+source.size()+" strict_unknown="+strictUnknown+
        " synthetic_ranked="+simCandidate+" synthetic_top1="+simCorrect+
        " ambiguous_source_cases="+ambiguous+
        " synthetic_positive_models="+modelsPositive+
        " physical_accuracy_claim=false");
  }
}
