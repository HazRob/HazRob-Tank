package com.hazrob.tank.chemcore.next;

import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.*;

 /**
  * Deterministic research stress suite using the REAL ChemCoreNext.analyze()
  * of HazRob Tank V8, not a surrogate ranker.
  * All models, gas exposures, source qualification flags and R0 values are
  * SYNTHETIC. Source "qualifications" are deliberately injected only to
  * challenge post-gate ranking, not evidence of physical qualification.
  * This run measures software behaviour, NOT real gas identification accuracy.
  */
public final class NextV8QualQuantStressSuite {
  private NextV8QualQuantStressSuite(){}
  private record Gas(int uid, String name, String sensor, double a, double b,
      double minPpm, double maxPpm, double[] concentrations) {}
  private static final Gas[] G = {
    new Gas(101,"CH4","MQ4",1000.,-1.8,200.,10000.,new double[]{500,1000,2500,5000}),
    new Gas(102,"ETHANOL","MQ3",100.,-2.0,10.,500.,new double[]{20,50,100,200}),
    new Gas(103,"CO","MQ7",100.,-2.0,20.,1000.,new double[]{50,100,200,500}),
    new Gas(104,"H2S","MQ136",30.,-2.5,1.,200.,new double[]{2,10,30,90}),
    new Gas(105,"NH3","MQ137",40.,-2.3,5.,500.,new double[]{10,20,50,100}),
    new Gas(106,"ACETONE","MQ138",60.,-2.2,5.,500.,new double[]{15,30,100,200}),
    new Gas(107,"O3","MQ131_LOW",.08,1.6,.01,1.,new double[]{.025,.05,.1,.4})
  };
  private static final String[] MODES={"ideal","noise_2pct","noise_10pct",
     "humidity_ratio_plus15pct","source_r0_plus20pct","true_slope_plus15pct","cross_response"};
  private static final CoreContracts.DeviceProfile DEVICE;
  private static final List<ResearchCandidateGenerator.Material> CATALOG;
  private static final List<ChemCoreNext.CurveBinding> CURVES;
  private static final SourceCurveExpert.Qualification TEST_GATE =
    new SourceCurveExpert.Qualification(true,true,true,true);
  static {
    List<CoreContracts.SensorInstance> sensors=new ArrayList<>();
    List<ResearchCandidateGenerator.Material> materials=new ArrayList<>();
    List<ChemCoreNext.CurveBinding> curves=new ArrayList<>();
    for(int uid=1;uid<=736;uid++)
      materials.add(new ResearchCandidateGenerator.Material(uid,"catalog-uid-"+uid,
        uid>734?ResearchCandidateGenerator.MaterialKind.GENERAL_CLASS:
          ResearchCandidateGenerator.MaterialKind.MOLECULAR_CHEMICAL));
    for(Gas g:G) {
      sensors.add(new CoreContracts.SensorInstance("SIM-"+g.sensor,g.sensor,
          "SIM-PHYSICAL-SENSOR-SLOT","HEATER_TEST"));
      curves.add(new ChemCoreNext.CurveBinding(curve(g),"SIM-"+g.sensor,TEST_GATE));
    }
    DEVICE=new CoreContracts.DeviceProfile("SYNTHETIC-TANK-V8-TESTBED",
       "SIMULATED_NO_HARDWARE","SIMULATED_NO_FIRMWARE",sensors);
    CATALOG=List.copyOf(materials);CURVES=List.copyOf(curves);
  }
  private static SourceCurveExpert.Curve curve(Gas g) {
    return new SourceCurveExpert.Curve("SIM-CURVE-"+g.name,
      "SIM-INDEPENDENT-ROOT-"+g.name,g.sensor,g.name,(long)g.uid,
      SourceCurveExpert.Equation.PPM_EQUALS_A_TIMES_RATIO_POWER_B,
      g.a,g.b,null,g.minPpm,g.maxPpm,"HEATER_TEST");
  }
  private static int n=0, checks=0;
  private static void require(boolean v,String label) {
    checks++;if(!v)throw new IllegalStateException("SIM_BENCH_FAIL "+label);
  }
  private static final String HEADER="case_id,ground_truth,mode,true_ppm,expected_uid,"+
      "actual_status,rank_1_uid,ranked_count,ranked_uids,source_root_count,"+
      "true_gas_curve_decision,true_gas_research_ppm,absolute_error_ppm,relative_error_pct,"+
      "safety_alarm,reportable_ppm,physical_g3,diagnostics";
  private static final List<String> ROWS=new ArrayList<>();
  private static final Random RAND=new Random(20261105L);
  private static String str(Object x) {return x==null?"":x.toString();}
  private static String finite(Double x) {return x==null||!Double.isFinite(x)?"":String.format(Locale.ROOT,"%.8f",x);}
  private static final class Result {
    ChemCoreNext.Output out; SourceCurveExpert.Output quantified;
  }
  private static Result replay(Gas truth,Gas other,Double concentration,String mode,
      boolean relay,boolean sourceR0,boolean corrupt) {
    List<CoreContracts.Reading> readings=new ArrayList<>();
    for(Gas g:G) {
      double base=10.0+(g.uid-101)*2.0; // explicit synthetic source R0 kOhm
      double ratio=1.0;
      if(truth!=null && concentration!=null && g.uid==truth.uid)
        ratio=SourceCurveExpert.forwardSourceRatio(curve(g),concentration);
      if(other!=null && g.uid==other.uid)
        ratio=SourceCurveExpert.forwardSourceRatio(curve(g),other.concentrations[2]);
      if("cross_response".equals(mode) && truth!=null&&g.uid!=truth.uid&&g.uid!=otherUid(other))
        ratio=Math.pow(SourceCurveExpert.forwardSourceRatio(curve(truth),concentration),.10);
      if("true_slope_plus15pct".equals(mode) && truth!=null&&g.uid==truth.uid)
        ratio=Math.pow(ratio,1.15);
      if("humidity_ratio_plus15pct".equals(mode)) ratio*=1.15;
      if("noise_2pct".equals(mode)) ratio*=Math.exp(RAND.nextGaussian()*.02);
      if("noise_10pct".equals(mode)) ratio*=Math.exp(RAND.nextGaussian()*.10);
      double r0="source_r0_plus20pct".equals(mode)?base*1.20:base;
      CoreContracts.CalibrationReference ref=sourceR0?
        CoreContracts.CalibrationReference.SOURCE_R0:CoreContracts.CalibrationReference.AIR_RS_MEAN;
      readings.add(new CoreContracts.Reading("SIM-"+g.sensor,null,null,base*ratio,
        base,sourceR0?r0:null,ref,"HEATER_TEST",
        corrupt&&g.uid==101?CoreContracts.SampleQuality.STALE:
          CoreContracts.SampleQuality.VALID));
    }
    String id="SYNTHETIC-"+(++n);
    var cap=new CoreContracts.Snapshot(id,DEVICE.deviceUid,CoreContracts.Mode.LAB,
      n*1000L,25.,55.,relay,readings,"SYNTHETIC_STABLE_MANIFEST_NOT_QUALIFIED");
    var input=new ChemCoreNext.Input(DEVICE,cap,ChemCoreNext.snapshotHash(cap),CATALOG,
      CURVES,List.of(),10);
    var out=ChemCoreNext.analyze(input);
    require(!out.safetyAlarmAuthorized&&!out.ppmAuthorized&&!out.molecularIdentityConfirmed
        &&!out.g3Qualified,"engine must not assert field gas identity/ppm/alarm/G3");
    Result r=new Result();r.out=out;
    if(truth!=null) {
      Gas g=truth;
      r.quantified=SourceCurveExpert.evaluate(curve(g),
        DEVICE.installedSensors.stream().filter(s->s.model.equals(g.sensor)).findFirst().orElseThrow(),
        readings.stream().filter(x->x.sensorUid.equals("SIM-"+g.sensor)).findFirst().orElseThrow(),TEST_GATE);
    }
    return r;
  }
  private static int otherUid(Gas g) {return g==null?-1:g.uid;}
  private static void record(Gas truth,Gas other,Double ppm,String mode,boolean relay,
      boolean sourceR0,boolean corrupt) {
    Result r=replay(truth,other,ppm,mode,relay,sourceR0,corrupt);
    var out=r.out;
    var ranks=out.arbitration.eligibleRanked;
    StringJoiner ids=new StringJoiner("|");
    for(var x:ranks)ids.add(Long.toString(x.uid));
    int top=ranks.isEmpty()?0:(int)ranks.get(0).uid;
    Double est=r.quantified==null?r.quantified: r.quantified.researchOnlyPpm;
    Double absolute=est==null||ppm==null?null:Math.abs(est-ppm);
    Double rel=absolute==null||ppm<=0?null:100.0*absolute/ppm;
    String category=truth==null?(other==null?"CLEAN_AIR":"MIXTURE"):
       (other==null?truth.name:"MIXTURE");
    var metric=r.quantified;
    String quantDecision=metric==null?"NA":metric.decision.name();
    String diagnostics=String.join(";",out.sourceDiagnostics);
    diagnostics=diagnostics.replace(',',' ').replace('\n',' ');
    ROWS.add(String.join(",",Integer.toString(n),category,mode,finite(ppm),
       truth==null?"":Integer.toString(truth.uid),out.arbitration.status.name(),
       top==0?"":Integer.toString(top),Integer.toString(ranks.size()),ids.toString(),
       Integer.toString(out.graph.independentSourceRoots),quantDecision,finite(est),
       finite(absolute),finite(rel),Boolean.toString(out.safetyAlarmAuthorized),
       Boolean.toString(out.ppmAuthorized),Boolean.toString(out.g3Qualified),diagnostics));
  }
  public static void main(String[] args)throws Exception {
    require(args.length==1,"CSV output directory argument");
    for(Gas g:G)for(double ppm:g.concentrations)for(String mode:MODES)
      for(int replicate=0;replicate<12;replicate++)
        record(g,null,ppm,mode,true,true,false);
    for(int i=0;i<60;i++)record(null,null,null,"clean_air",true,true,false);
    for(int i=0;i<60;i++)record(G[0],G[4],G[0].concentrations[1],
       "two_gas_mixture",true,true,false);
    // Independent abstention / fail-closed controls.
    for(int i=0;i<10;i++)record(G[0],null,G[0].concentrations[1],
       "relay_off",false,true,false);
    for(int i=0;i<10;i++)record(G[0],null,G[0].concentrations[1],
       "no_source_r0",true,false,false);
    for(int i=0;i<10;i++)record(G[0],null,G[0].concentrations[1],
       "invalid_channel",true,true,true);
    Path root=Path.of(args[0]);Files.createDirectories(root);
    Path csv=root.resolve("next_v8_simulated_qual_quant_cases.csv");
    StringBuilder data=new StringBuilder(HEADER).append('\n');
    for(String s:ROWS)data.append(s).append('\n');
    Files.writeString(csv,data,StandardCharsets.UTF_8);
    System.out.println("NEXT_V8_REAL_ENGINE_SIMULATION_PASS cases="+ROWS.size()
       +" checks="+checks+" benchmark_only=true actual_Tank_G3=false");
    System.out.println("NEXT_V8_SIMULATION_ROWS="+ROWS.size());
  }
}
