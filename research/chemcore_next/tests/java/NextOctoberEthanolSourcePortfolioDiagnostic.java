package com.hazrob.tank.chemcore.next;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Original 2026-10-01 HazRob 8-MQ snapshot through actual ChemCoreNext Java
 * source-curve expert and independent orchestrator.
 *
 * PRIMARY RESULT: UNKNOWN (historical R0 definition not verified, heater
 * cycles unverified, ethanol + tobacco smoke co-exposure).
 *
 * RESEARCH-ONLY SHADOW DIAGNOSTIC: mathematically invert original
 * MQDataScience 6.0.0 source curves under the explicit FALSE ASSUMPTION that
 * historical R0 coincides with the source-equation R0.  Count per-source
 * inverse estimates inside source nominal domain and 2x concentration
 * agreement clusters. These are NOT chemical detection, posterior, or ppm.
 */
public final class NextOctoberEthanolSourcePortfolioDiagnostic {
  private NextOctoberEthanolSourcePortfolioDiagnostic(){}
  private static final String SOURCE="MQDATASCIENCE_6_0_0_SINGLE_ROOT";
  private static final String[] SENSOR={"MQ2","MQ3","MQ4","MQ5","MQ7","MQ8","MQ9","MQ135"};
  private static int checks;
  private static void check(boolean good,String msg) {
    checks++;if(!good)throw new IllegalStateException("OCT_ETHANOL_PORTFOLIO_G1_"+checks+": "+msg);
  }
  private static final class Physical {
    String sensor;double rs,r0,ratio;
    Physical(String sensor,double rs,double r0){
      this.sensor=sensor;this.rs=rs;this.r0=r0;ratio=rs/r0;
    }
  }
  private static final class SourceModel {
    String sensor,label;int legacyId;double min,max;
    SourceCurveExpert.Curve curve;
    SourceModel(String sensor,int uid,String label,double a,double b,double scale,double min,double max) {
      this.sensor=sensor;legacyId=uid;this.label=label;this.min=min;this.max=max;
      curve=new SourceCurveExpert.Curve("MQDATASCIENCE_"+sensor+"_MODEL_"+uid,
        SOURCE,sensor,label,null,
        SourceCurveExpert.Equation.RATIO_EQUALS_A_TIMES_PPM_OVER_SCALE_POWER_B,
        a,b,scale,min,max,"UNVERIFIED_SOURCE_HEATER");
    }
  }
  private static final class Point {
    SourceModel source;double hypotheticalPpm;boolean inDomain;
    Point(SourceModel source,double ppm) {
      this.source=source;hypotheticalPpm=ppm;
      inDomain=ppm>=source.min&&ppm<=source.max;
    }
  }
  private static final class Candidate {
    int id,inDomain,available,largestFactorTwoCluster;
    String label;
    List<Point> sensors=new ArrayList<>();
    List<String> clusterSensorModels=new ArrayList<>();
    Candidate(int id,String label){this.id=id;this.label=label;}
    void complete(){
      available=sensors.size();
      List<Point> in=new ArrayList<>();
      for(Point p:sensors)if(p.inDomain)in.add(p);
      inDomain=in.size();
      in.sort(Comparator.comparingDouble(p->p.hypotheticalPpm));
      int best=0,begin=0;
      for(int end=0;end<in.size();end++){
        while(begin<=end&&in.get(end).hypotheticalPpm/in.get(begin).hypotheticalPpm>2.)
          begin++;
        if(end-begin+1>best){
          best=end-begin+1;
          clusterSensorModels=new ArrayList<>();
          for(int j=begin;j<=end;j++)clusterSensorModels.add(in.get(j).source.sensor);
        }
      }
      largestFactorTwoCluster=best;
    }
  }
  private static Map<String,Physical> loadPhysical(Path file)throws Exception {
    Map<String,Physical> p=new LinkedHashMap<>();
    try(BufferedReader br=Files.newBufferedReader(file,StandardCharsets.UTF_8)){
      check(br.readLine().equals(
        "sensor,reported_Rs_kohm_rounded,reported_historical_R0_kohm_rounded,original_R0_definition_verified,heater_phase_independently_verified"),
        "historic R0 definition and heater fields");
      String s;
      while((s=br.readLine())!=null){
        String[] x=s.split(",",-1);
        check(x.length==5&&x[3].equals("false")&&x[4].equals("false"),
          "real original source, not chemically qualified");
        double rs=Double.parseDouble(x[1]),r0=Double.parseDouble(x[2]);
        check(Double.isFinite(rs)&&Double.isFinite(r0)&&rs>0&&r0>0,
          "source resistances physically positive");
        check(p.put(x[0],new Physical(x[0],rs,r0))==null,"one physical reading per MQ");
      }
    }
    check(p.size()==8,"all original eight MQ channels");
    for(String sensor:SENSOR)check(p.containsKey(sensor),"original sensor "+sensor);
    return p;
  }
  private static List<SourceModel> loadModels(Path path)throws Exception {
    List<SourceModel> c=new ArrayList<>();
    try(BufferedReader br=Files.newBufferedReader(path,StandardCharsets.UTF_8)){
      check(br.readLine().equals("sensor\tlegacy_id\tgas_label\tratio_a\tratio_b\toutput_scale\tmin_ppm\tmax_ppm"),
        "source curves schema output exact");
      String line;
      while((line=br.readLine())!=null){
        String[] x=line.split("\t",-1);check(x.length==8,"source curve original columns");
        c.add(new SourceModel(x[0],Integer.parseInt(x[1]),x[2],
          Double.parseDouble(x[3]),Double.parseDouble(x[4]),Double.parseDouble(x[5]),
          Double.parseDouble(x[6]),Double.parseDouble(x[7])));
      }
    }
    check(c.size()==43,"43 original source curves, no invented trained fingerprints");
    return c;
  }
  private static boolean included(String model,boolean filtered){
    return !filtered||(!model.equals("MQ7")&&!model.equals("MQ9"));
  }
  private static String quote(String s){
    return "\""+s.replace("\\","\\\\").replace("\"","\\\"")+"\"";
  }
  private static String fmt(double x){
    if(!Double.isFinite(x))return "null";
    return String.format(Locale.ROOT,"%.10g",x);
  }
  private static List<Candidate> rank(Map<String,Physical> originals,List<SourceModel> curves,
                                     boolean heaterFiltered) {
    Map<Integer,Candidate> byId=new LinkedHashMap<>();
    for(SourceModel model:curves){
      if(!included(model.sensor,heaterFiltered))continue;
      Physical raw=originals.get(model.sensor);
      if(raw==null)throw new IllegalStateException("source/physical matching channel absent");
      Candidate cand=byId.computeIfAbsent(model.legacyId,
        k->new Candidate(model.legacyId,model.label));
      double estimate=SourceCurveExpert.inverseSourcePpm(model.curve,raw.ratio);
      check(Double.isFinite(estimate)&&estimate>0,
        "actual next source expert inverse algebra finite");
      cand.sensors.add(new Point(model,estimate));
    }
    List<Candidate> ranked=new ArrayList<>(byId.values());
    for(Candidate cand:ranked)cand.complete();
    // This is explicitly NOT a scientific identity or source-weighted
    // posterior; just number of MODELled per-channel ppm inside each source
    // curve interval and 2x agreement cluster for a fully hypothetical R0.
    ranked.sort(Comparator.comparingInt((Candidate z)->z.largestFactorTwoCluster).reversed()
      .thenComparing(Comparator.comparingInt((Candidate z)->z.inDomain).reversed())
      .thenComparing(Comparator.comparingInt((Candidate z)->z.available).reversed())
      .thenComparingInt(z->z.id));
    return ranked;
  }
  private static ChemCoreNext.Output runActualNext(Map<String,Physical> physical,
                                                   List<SourceModel> models,boolean six) {
    List<CoreContracts.SensorInstance> sensors=new ArrayList<>();
    List<CoreContracts.Reading> readings=new ArrayList<>();
    List<ChemCoreNext.CurveBinding> bindings=new ArrayList<>();
    for(Physical x:physical.values()){
      if(!included(x.sensor,six))continue;
      sensors.add(new CoreContracts.SensorInstance("HAZROB-WHEELED:"+x.sensor,
        x.sensor,"HR069_ORIGINAL","HEATER_UNVERIFIED"));
      // Unknown historical R0 means we CANNOT simply label x.r0 as
      // R_air or SOURCE_R0.  Preserve measured Rs for input provenance.
      readings.add(new CoreContracts.Reading("HAZROB-WHEELED:"+x.sensor,
        null,null,x.rs,null,null,CoreContracts.CalibrationReference.NOT_AVAILABLE,
        "HEATER_UNVERIFIED",CoreContracts.SampleQuality.UNCERTAIN_REFERENCE));
    }
    for(SourceModel source:models) {
      if(!included(source.sensor,six))continue;
      bindings.add(new ChemCoreNext.CurveBinding(source.curve,
          "HAZROB-WHEELED:"+source.sensor,SourceCurveExpert.Qualification.unverified()));
    }
    var device=new CoreContracts.DeviceProfile("OCT-01-HR069-WHEELED",
      "HR069","HazRob_0.10.2",sensors);
    var capture=new CoreContracts.Snapshot("OCT-2026-ETHANOL-ORIGINAL",
      "OCT-01-HR069-WHEELED",CoreContracts.Mode.LAB,149339L,
      null,null,true,readings,"historical-legacy-R0-undefined");
    return ChemCoreNext.analyze(new ChemCoreNext.Input(
      device,capture,ChemCoreNext.snapshotHash(capture),
      List.of(),bindings,List.of(),10));
  }
  private static String resultJson(List<Candidate> ranked,boolean six,
                                   ChemCoreNext.Output gate) {
    StringBuilder s=new StringBuilder();
    s.append("{\"configuration\":").append(quote(six?"six_MQ_excluding_MQ7_MQ9":"all_eight_original_MQ"));
    s.append(",\"original_MQ_channels\":").append(six?6:8);
    s.append(",\"next_engine_decision\":").append(quote(gate.research.status.name()));
    s.append(",\"engine_eligible_candidates\":").append(gate.arbitration.eligibleCount);
    s.append(",\"source_model_count\":").append(gate.modelsInspected);
    s.append(",\"shadow_ranking_HYPOTHETICAL_R0_SOURCE_ONLY\":[");
    for(int i=0;i<ranked.size();i++){
      Candidate c=ranked.get(i);
      if(i>0)s.append(",");
      s.append("{\"rank\":").append(i+1).append(",\"legacy_id\":").append(c.id);
      s.append(",\"source_label\":").append(quote(c.label));
      s.append(",\"original_curve_channels\":").append(c.available);
      s.append(",\"hypothetical_model_curves_within_nominal_ppm_range\":").append(c.inDomain);
      s.append(",\"hypothetical_factor_two_cluster_channel_count\":").append(c.largestFactorTwoCluster);
      s.append(",\"hypothetical_cluster_sensor_models\":[");
      for(int j=0;j<c.clusterSensorModels.size();j++){
        if(j>0)s.append(",");s.append(quote(c.clusterSensorModels.get(j)));
      }
      s.append("],\"per_original_sensor_curve_inverse\":[");
      for(int j=0;j<c.sensors.size();j++){
        Point pt=c.sensors.get(j);
        if(j>0)s.append(",");
        s.append("{\"sensor\":").append(quote(pt.source.sensor));
        s.append(",\"hypothetical_ppm_algebra_only\":").append(fmt(pt.hypotheticalPpm));
        s.append(",\"source_nominal_domain_min_ppm\":").append(fmt(pt.source.min));
        s.append(",\"source_nominal_domain_max_ppm\":").append(fmt(pt.source.max));
        s.append(",\"inside_source_nominal_domain\":").append(pt.inDomain);
        s.append("}");
      }
      s.append("]}");
    }
    s.append("]}");
    return s.toString();
  }
  public static void main(String[] args)throws Exception {
    check(args.length==3,"physical CSV, original source curves TSV, report JSON");
    Map<String,Physical> actual=loadPhysical(Path.of(args[0]));
    List<SourceModel> originalModels=loadModels(Path.of(args[1]));
    var originalEight=runActualNext(actual,originalModels,false);
    var filteredSix=runActualNext(actual,originalModels,true);
    check(originalEight.research.status==CoreContracts.DecisionStatus.UNKNOWN,
      "actual Next eight MQ abstains from unverified R0 and gas identity");
    check(filteredSix.research.status==CoreContracts.DecisionStatus.UNKNOWN,
      "actual Next six MQ abstains from unverified R0");
    check(originalEight.arbitration.eligibleCount==0&&filteredSix.arbitration.eligibleCount==0,
      "no false Next engine canonical candidates");
    check(!originalEight.molecularIdentityConfirmed&&!originalEight.ppmAuthorized
      &&!originalEight.safetyAlarmAuthorized&&!originalEight.g3Qualified,
      "source cross device cannot establish physical identity, ppm or safety");
    List<Candidate> rankedEight=rank(actual,originalModels,false);
    List<Candidate> rankedSix=rank(actual,originalModels,true);
    check(rankedEight.size()==13&&rankedSix.size()==13,
      "complete 13 distinct source labels, no artificial top-10 quota");
    check(rankedEight.get(0).id==75&&rankedSix.get(0).id==75,
      "research-only inversion domain consistency favours H2, NOT known ethanol");
    Candidate alcoholEight=null,alcoholSix=null,methaneEight=null;
    for(Candidate c:rankedEight){
      if(c.id==103)alcoholEight=c;if(c.id==76)methaneEight=c;
    }
    for(Candidate c:rankedSix)if(c.id==103)alcoholSix=c;
    check(alcoholEight!=null&&alcoholSix!=null&&methaneEight!=null,
      "october ethanol, methane and source candidates retained");
    check(alcoholEight.inDomain==1&&alcoholEight.available==7,
      "ethanol ONLY MQ3 curve nominally in range in 8-sensor source diagnostic");
    check(alcoholSix.inDomain==1&&alcoholSix.available==6,
      "ethanol ONLY MQ3 curve nominally in range in filtered 6-sensor diagnostic");
    check(rankedEight.get(0).largestFactorTwoCluster==3
       &&rankedSix.get(0).largestFactorTwoCluster==3,
       "three correlated H2 research curves agree by factor 2, yet false physical identity");
    StringBuilder doc=new StringBuilder();
    doc.append("{\"schema\":\"chemcore.next.original_october_ethanol_source_portfolio/1\",");
    doc.append("\"original_experiment_id\":\"HazRob Experiment 4 Oct 1 2026\",");
    doc.append("\"source_8mq_original_rounded_real_Rs_and_historical_R0\":true,");
    doc.append("\"historical_R0_definition_verified\":false,");
    doc.append("\"original_exposure_ethanol_96_percent_liquid_with_cigarette_smoke\":true,");
    doc.append("\"original_headspace_ethanol_ppm_measured\":false,");
    doc.append("\"source_fingerprint_reference\":\"MQDataScience 6.0.0 original 43 curves\",");
    doc.append("\"source_curves_are_valid_for_owner_wheeled_R0\":false,");
    doc.append("\"source_fingerprint_algorithm\":\"ChemCoreNext.analyze + SourceCurveExpert.inverseSourcePpm exploratory curve-domain agreement\",");
    doc.append("\"research_shadow_ranking_is_NOT_ChemCoreNext_identity_decision\":true,");
    doc.append("\"shadow_factor_two_window_is_predeclared_not_calibrated\":true,");
    doc.append("\"source_root_independence_count\":1,");
    doc.append("\"physical_G3\":false,\"gas_ppm_reportable\":false,\"chemical_identity_confirmed\":false,");
    doc.append("\"source_experimental_arms\":[");
    doc.append(resultJson(rankedEight,false,originalEight)).append(",");
    doc.append(resultJson(rankedSix,true,filteredSix));
    doc.append("],\"conclusion\":\"At original unverified R0 both engine arms abstain. H2 looks most internally consistent only under unqualified source-R0 substitution, even though user provided ethanol plus cigarette smoke. This is demonstrated source-reference/model transfer bias, not true H2 detection.\",");
    doc.append("\"G1_assertions\":").append(checks).append("}\n");
    Path output=Path.of(args[2]);Files.createDirectories(output.getParent());
    Files.writeString(output,doc.toString(),StandardCharsets.UTF_8);
    System.out.println("CHEMCORE_NEXT_OCT_ETHANOL_PORTFOLIO_DIAGNOSTIC_PASS"+
      " real_original_sensors=8 six_sensor_arm=true"+
      " full_engine=UNKNOWN filtered_engine=UNKNOWN"+
      " hypothetical_H2_top_two_arms=true"+
      " ethanol_source_range_count_full=1_of_7"+
      " ethanol_source_range_count_filtered=1_of_6"+
      " assertion_count="+checks+" G3=0");
  }
}
