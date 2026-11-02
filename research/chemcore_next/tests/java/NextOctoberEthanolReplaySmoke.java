package com.hazrob.tank.chemcore.next;

import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Owner's October 01, 2026 ORIGINAL physical HazRob Experiment 4 replay.
 * CSV contains genuinely recovered, 0.001 kOhm-rounded Rs and historic R0
 * (the PDF independently documents experiment context and legacy ranking).
 * Reconstructs diagnostic Rs / historical R0, but NEVER asserts historic R0
 * equals R_air or source manufacturer R0. Original gas concentration unknown,
 * smoke interfered, and heater cycle was not source qualified.
 */
public final class NextOctoberEthanolReplaySmoke {
  private NextOctoberEthanolReplaySmoke(){}
  private static int assertions;
  private static void must(boolean ok,String why){
    assertions++;if(!ok)throw new IllegalStateException(
      "OCT2026_ETHANOL_G1_ASSERTION_"+assertions+" "+why);
  }
  private static final String[] MQ={"MQ2","MQ3","MQ4","MQ5","MQ7","MQ8","MQ9","MQ135"};
  private static final double[] REPORTED_RS={50.482,5.154,26.430,187.093,43.387,7.813,33.530,59.067};
  private static final double[] REPORTED_R0={91.734,20.686,65.270,251.965,160.601,54.889,69.990,213.239};
  private static final double[] CURRENT_OCT31_AIR={92.1417313,25.5138416,37.8870316,158.0950317,
                                                    62.5898705,36.5759659,56.4506149,113.6592255};
  private static final class Cell {
    final String sensor; final double rs,r0,ratio;
    Cell(String sensor,double rs,double r0){
      this.sensor=sensor;this.rs=rs;this.r0=r0;ratio=rs/r0;
    }
  }
  private static Map<String,Cell> load(Path p)throws Exception{
    Map<String,Cell> all=new LinkedHashMap<>();
    try(var reader=Files.newBufferedReader(p,StandardCharsets.UTF_8)){
      String header=reader.readLine();
      must(header.equals("sensor,reported_Rs_kohm_rounded,reported_historical_R0_kohm_rounded,original_R0_definition_verified,heater_phase_independently_verified"),
        "record has explicit R0 type and heater verification, not assumed");
      String line;
      while((line=reader.readLine())!=null){
        var x=line.split(",",-1);must(x.length==5,"five unambiguous raw fields");
        must("false".equals(x[3])&&"false".equals(x[4]),"R0 and heater remain unqualified");
        double rs=Double.parseDouble(x[1]),r0=Double.parseDouble(x[2]);
        must(Double.isFinite(rs)&&Double.isFinite(r0)&&rs>0&&r0>0,
             "physical measured source resistances positive");
        must(all.put(x[0],new Cell(x[0],rs,r0))==null,"unique actual physical MQ");
      }
    }
    must(all.size()==8,"8 real historic MQ channels");
    for(int i=0;i<MQ.length;i++) {
      var c=all.get(MQ[i]);
      must(c!=null,"original MQ "+MQ[i]);
      must(Math.abs(c.rs-REPORTED_RS[i])<1e-9,"October physical Rs rounded faithful "+MQ[i]);
      must(Math.abs(c.r0-REPORTED_R0[i])<1e-9,"October historical R0 rounded faithful "+MQ[i]);
      must(c.ratio>0&&c.ratio<1,"positive historic response ratio "+MQ[i]);
    }
    return all;
  }
  private static CoreContracts.DeviceProfile robot() {
    List<CoreContracts.SensorInstance> all=new ArrayList<>();
    for(String s:MQ) {
      String heater=s.equals("MQ7")||s.equals("MQ9")?
        "CONSTANT_5V_UNVERIFIED_CYCLIC":"HEATER_PHASE_UNVERIFIED";
      all.add(new CoreContracts.SensorInstance("OCT2026:"+s,s,
        "HAZROB_0_10_2_HR069",heater));
    }
    return new CoreContracts.DeviceProfile("ORIGINAL_HAZROB_WHEELED_OCT1",
       "HR069", "HazRob_0_10_2_API4",all);
  }
  private static CoreContracts.Snapshot capture(Map<String,Cell> measured,boolean sixOnly){
    List<CoreContracts.Reading> all=new ArrayList<>();
    for(var e:measured.entrySet()){
      String sensor=e.getKey();
      if(sixOnly&&(sensor.equals("MQ7")||sensor.equals("MQ9")))continue;
      // Valid measured Rs is preserved, but chemical/reference quality is
      // UNCERTAIN_REFERENCE because legacy R0 is not proved to mean R_air.
      all.add(new CoreContracts.Reading("OCT2026:"+sensor,null,null,e.getValue().rs,
        null,null,CoreContracts.CalibrationReference.NOT_AVAILABLE,
        sensor.equals("MQ7")||sensor.equals("MQ9")?
          "CONSTANT_5V_UNVERIFIED_CYCLIC":"HEATER_PHASE_UNVERIFIED",
        CoreContracts.SampleQuality.UNCERTAIN_REFERENCE));
    }
    return new CoreContracts.Snapshot("HAZROB_EXPERIMENT_4_2026_10_01",
        "ORIGINAL_HAZROB_WHEELED_OCT1",CoreContracts.Mode.LAB,149339,
        null,null,true,all,"ORIGINAL_HAZROB_0_10_2_RESEARCH_MANIFEST_UNVERIFIED");
  }
  private static ChemCoreNext.Output replay(Map<String,Cell> measured,boolean sixOnly){
    var curve=new SourceCurveExpert.Curve("MQDATASCIENCE_6_MQ3_ETHANOL",
       "MQDATASCIENCE_ORIGINAL_RESEARCH_CURVE","MQ3","Alcohol research only",103L,
       SourceCurveExpert.Equation.RATIO_EQUALS_A_TIMES_PPM_OVER_SCALE_POWER_B,
       0.5565,-0.6333,50.0,5.15,500.0,"SOURCE_HEATER_PROFILE_UNPROVEN");
    // Curve's mathematical form is known. Source calibration definition and
    // acquired instrument/heater matching are not. These flags are real false.
    var binding=new ChemCoreNext.CurveBinding(curve,"OCT2026:MQ3",
         SourceCurveExpert.Qualification.unverified());
    List<ResearchCandidateGenerator.Material> catalog=List.of(
       new ResearchCandidateGenerator.Material(103L,"Ethanol (legacy research mapping)",
           ResearchCandidateGenerator.MaterialKind.MOLECULAR_CHEMICAL),
       new ResearchCandidateGenerator.Material(76L,"Methane (legacy research mapping)",
           ResearchCandidateGenerator.MaterialKind.MOLECULAR_CHEMICAL));
    var snap=capture(measured,sixOnly);
    return ChemCoreNext.analyze(new ChemCoreNext.Input(robot(),snap,
       ChemCoreNext.snapshotHash(snap),catalog,List.of(binding),List.of(),10));
  }
  private static double hypotheticAlcoholPpm(String sensor,double ratio){
    // Same 7 ethanol cross-response source curves in pinned JSON
    // MQDataScience 6.0.0: Rs/R0_source = a*(ppm/source_scale)^b.
    // Ratio is the historical unqualified one => G1 algebra only.
    double a,b,scale,lo,hi;
    switch(sensor){
      case "MQ2": a=19.2641;b=-.3604;scale=1;lo=200;hi=10000;break;
      case "MQ3": a=.5565;b=-.6333;scale=50;lo=5.15;hi=500;break;
      case "MQ4": a=5.7994;b=-.0696;scale=1;lo=200;hi=10000;break;
      case "MQ5": a=10.8274;b=-.2104;scale=1;lo=200;hi=10000;break;
      case "MQ7": a=20.2524;b=-.0655;scale=1;lo=50;hi=4000;break;
      case "MQ8": a=468.8045;b=-.5574;scale=1;lo=200;hi=10000;break;
      case "MQ135": a=3.8623;b=-.3126;scale=1;lo=10;hi=200;break;
      default:throw new IllegalArgumentException("no original ethanol source curve for "+sensor);
    }
    var curve=new SourceCurveExpert.Curve("RESEARCH_ORIGINAL_"+sensor+"_ALCOHOL",
       "MQDATASCIENCE_ORIGINAL_RESEARCH_CURVE",sensor,"alcohol-source-not-specific",null,
       SourceCurveExpert.Equation.RATIO_EQUALS_A_TIMES_PPM_OVER_SCALE_POWER_B,
       a,b,scale,lo,hi,"SOURCE_HEATER_UNPROVEN");
    return SourceCurveExpert.inverseSourcePpm(curve,ratio);
  }
  private static String fmt(double x){return String.format(Locale.ROOT,"%.8f",x);}
  public static void main(String[] args)throws Exception{
    must(args.length==3,"CSV physical source, experiment metadata, output");
    String meta=Files.readString(Path.of(args[1]),StandardCharsets.UTF_8);
    must(meta.contains("\"mq_power_on_ms\": 149339"),"actual archived power-on interval");
    must(meta.contains("\"time_series_recorded\": false"),"no invented original time series");
    must(meta.contains("96% ethanol liquid"),"known exposure note not ppm gas");
    must(meta.contains("HazRob_Report_000004_20261001"),"original report title retained");
    must(meta.contains("FIELD-R0-3SESSION-20260913"),"historical 216 sample ref");
    Map<String,Cell> measured=load(Path.of(args[0]));
    var old=measured.get("MQ3");
    double predicted=hypotheticAlcoholPpm("MQ3",old.ratio);
    must(Math.abs(predicted-177.84732487397758)<1e-8,
       "the October MQ3 curve algebra is reproduced exactly from rounded originals");
    var legacyFull=replay(measured,false);
    var legacySix=replay(measured,true);
    must(legacyFull.research.status==CoreContracts.DecisionStatus.UNKNOWN,
       "8MQ legacy R0 unknown -> abstain on unqualified chemical inference");
    must(legacySix.research.status==CoreContracts.DecisionStatus.UNKNOWN,
       "6MQ without MQ7/9 preserves abstention without quantified reference");
    must(legacyFull.arbitration.eligibleCount==0&&legacySix.arbitration.eligibleCount==0,
       "zero actual source-qualified gas candidates");
    must(legacyFull.sourceDiagnostics.stream().anyMatch(s->s.contains("CURVE_NOT_ELIGIBLE")),
       "original experimental curve explicitly rejected as unqualified");
    must(legacySix.graph.independentSourceRoots==0,"no invented root votes");
    must(!legacyFull.ppmAuthorized&&!legacyFull.molecularIdentityConfirmed
        &&!legacyFull.safetyAlarmAuthorized&&!legacyFull.g3Qualified,
        "no source-based ppm or alarm certification");
    // Latest 2026-10-31 V6 tank R_air belongs to a DIFFERENT time/hardware.
    int crossDeviceDifferent=0;
    for(int i=0;i<MQ.length;i++){
      var c=measured.get(MQ[i]);double badRatio=c.rs/CURRENT_OCT31_AIR[i];
      if(Math.abs(badRatio-c.ratio)>0.01)crossDeviceDifferent++;
      must(CURRENT_OCT31_AIR[i]>0,"recent owner reference values consistent");
    }
    must(crossDeviceDifferent>=6,"cross-session R_air overwrite would change source response");
    // Comparing all ethanol-specific original source curve inversions shows
    // severe disagreement. Only MQ3 lies within its OWN declared source
    // concentration domain; this is neither independent identification nor
    // a real measured gas ppm.
    List<String> gasEvidence=new ArrayList<>();
    int outside=0,inSource=0;
    for(String sensor:new String[]{"MQ2","MQ3","MQ4","MQ5","MQ7","MQ8","MQ135"}){
      double est=hypotheticAlcoholPpm(sensor,measured.get(sensor).ratio);
      must(Double.isFinite(est)&&est>0,"research-only inverse finite "+sensor);
      double lo=sensor.equals("MQ3")?5.15:sensor.equals("MQ135")?10:sensor.equals("MQ7")?50:200;
      double hi=sensor.equals("MQ3")?500:sensor.equals("MQ135")?200:sensor.equals("MQ7")?4000:10000;
      boolean within=est>=lo&&est<=hi;
      if(within)inSource++;else outside++;
      if(sensor.equals("MQ3")||sensor.equals("MQ2")||sensor.equals("MQ5")||
         sensor.equals("MQ8")||sensor.equals("MQ135"))
        gasEvidence.add("\""+sensor+"\":{\"hypothetical_ppm_from_historical_ratio\":"+fmt(est)+
          ",\"inside_source_curve_interval\":"+within+"}");
    }
    must(inSource==1&&outside==6,"source curve disagreement: only MQ3 in nominal ppm range");
    StringBuilder json=new StringBuilder();
    json.append("{\n \"schema\":\"chemcore.next.october2026_ethanol_8mq_replay/1\",");
    json.append("\n \"actual_original_experiment\":\"HazRob 0.10.2 Experiment 4 on 2026-10-01\",");
    json.append("\n \"physical_October_MQ_channels\":8,\"rounded_source_values\":true,");
    json.append("\n \"legacy_R0_reference_definition_verified\":false,");
    json.append("\n \"known_exposure\":\"ethanol 96% liquid and cigarette smoke; unknown air-vapor ppm\",");
    json.append("\n \"old_screening_methane_rank\":1,\"old_screening_ethanol_rank\":5,");
    json.append("\n \"old_screening_methane_score\":71.625,\"old_screening_ethanol_score\":18.9,");
    json.append("\n \"MQ3_original_Rs_kohm\":5.154,\"MQ3_historic_R0_kohm\":20.686,");
    json.append("\n \"MQ3_ratio_from_rounded_values\":").append(fmt(old.ratio));
    json.append(",\n \"source_MQ3_ethanol_equation_replay_ppm_math_only\":").append(fmt(predicted));
    json.append(",\n \"published_curve_source\":\"MQDATASCIENCE_6_0_0 pinned original MQ3 Alcohol\",");
    json.append("\n \"research_curve_inversions\":{").append(String.join(",",gasEvidence)).append("},");
    json.append("\n \"alcohol_source_curves_nominally_within_range\":").append(inSource);
    json.append(",\"alcohol_source_curves_outside_nominal_range\":").append(outside);
    json.append(",\n \"historical_approx_118ppm_reproduced\":false,");
    json.append("\n \"historical_approx_118ppm_missing_per_device_literature_R0\":true,");
    json.append("\n \"source_heater_cycle_qualified\":false,\"real_independent_headspace_reference_ppm\":null,");
    json.append("\n \"next_original_8mq_status\":\"").append(legacyFull.research.status).append("\",");
    json.append("\n \"next_heater_filtered_6mq_status\":\"").append(legacySix.research.status).append("\",");
    json.append("\n \"next_original_8mq_candidates\":").append(legacyFull.arbitration.eligibleCount);
    json.append(",\"next_heater_filtered_6mq_candidates\":").append(legacySix.arbitration.eligibleCount);
    json.append(",\n \"oct31_V6_device_air_rebased_to_oct1_wheeled_original\":false,");
    json.append("\n \"cross_device_rebase_changes_ratio_by_more_than_0_01_channels\":").append(crossDeviceDifferent);
    json.append(",\n \"G1_software_pass\":true,\"G2_identity_qualified\":false,\"G3_actual_Tank_gas_challenge\":false,");
    json.append("\n \"source_signals_not_matched_to_qualified_reference\":true,");
    json.append("\n \"unit_scope\":\"ppm algebra diagnostic ONLY; no field ppm\",");
    json.append("\n \"analysis_scope\":\"original physical 8-MQ single-time snapshot; not complete ChemCore Next G2\",");
    json.append("\n \"corrective_actions\":[");
    json.append("\"preserve original Oct1 R0 and do not overwrite with Oct31 reference\",");
    json.append("\"add original multi-exposure training fingerprints and qualification gates\",");
    json.append("\"require independent controlled ethanol-vapor reference ppm and source-day holdout\",");
    json.append("\"measure smoke-only and ethanol-only controls, then mixture and clean air\",");
    json.append("\"record correct MQ7/MQ9 heater phase or exclude from source-response inference\"");
    json.append("],\"software_assertions\":").append(assertions).append("}\n");
    Path target=Path.of(args[2]);Files.createDirectories(target.getParent());
    Files.writeString(target,json.toString(),StandardCharsets.UTF_8);
    System.out.println("CHEMCORE_NEXT_OCT2026_REAL_ETHANOL_REPLAY_PASS source_channels=8"+
       " filtered_channels=6 old_ethanol_rank=5 original_methane_rank=1"+
       " mq3_algebra_ppm="+fmt(predicted)+" next_unknown=true source_curve_ood=6/7"+
       " assertions="+assertions+" G3=0");
  }
}
