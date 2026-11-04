package com.hazrob.tank.chemcore.next;

import java.util.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.io.*;

/** This Java17 audit invokes exact V8 SourceCurveExpert and ChemCoreNext.analyze.
 * The source-curve a,b and V6 assumed scale are original library data, but
 * the physical reading, R0, source qualification flags and sample are MADE UP
 * strictly to stress software inference and self-retrieval. No gas accuracy!
 */
public final class NextStableSourceCurveFullEntryAudit {
  private static final class Row {
    int curveIndex,oldUid,step; String curveId,sensor,gas;
    double a,b,scale,min,max,ppm,ratio;
  }
  private static final List<Row> rows=new ArrayList<>();
  private static final Map<String,List<Row>> sensorRows=new LinkedHashMap<>();
  private static List<ResearchCandidateGenerator.Material> materialList;
  private static CoreContracts.DeviceProfile device;
  private static final SourceCurveExpert.Qualification TEST_ONLY=
     new SourceCurveExpert.Qualification(true,true,true,true);
  private static final List<String> OUTPUT=new ArrayList<>();

  private static void readCSV(Path path)throws IOException{
    List<String> lines=Files.readAllLines(path,StandardCharsets.UTF_8);
    String[] fields=lines.get(0).split(",",-1);
    Map<String,Integer> ix=new HashMap<>();
    for(int i=0;i<fields.length;i++)ix.put(fields[i],i);
    for(String f:lines.subList(1,lines.size())){
      if(f.isBlank())continue;
      String[] x=f.split(",",-1);
      Row y=new Row();
      y.curveId=x[ix.get("curve_id")];
      y.curveIndex=Integer.parseInt(y.curveId.substring(y.curveId.length()-3));
      y.sensor=x[ix.get("sensor")];
      y.gas=x[ix.get("gas")];
      y.oldUid=Integer.parseInt(x[ix.get("original_legacy_id")]);
      y.step=Integer.parseInt(x[ix.get("step")]);
      y.a=Double.parseDouble(x[ix.get("source_coeff_a")]);
      y.b=Double.parseDouble(x[ix.get("source_coeff_b")]);
      y.scale=Double.parseDouble(x[ix.get("source_scale_assumed")]);
      y.min=Double.parseDouble(x[ix.get("source_min_ppm")]);
      y.max=Double.parseDouble(x[ix.get("source_max_ppm")]);
      y.ppm=Double.parseDouble(x[ix.get("true_ppm")]);
      y.ratio=Double.parseDouble(x[ix.get("synthetic_ratio")]);
      rows.add(y);
      if(y.step==0)sensorRows.computeIfAbsent(y.sensor,k->new ArrayList<>()).add(y);
    }
    Set<Integer> seen=new HashSet<>();
    List<ResearchCandidateGenerator.Material> items=new ArrayList<>();
    for(Row c:rows)if(seen.add(c.oldUid))items.add(
      new ResearchCandidateGenerator.Material(c.oldUid,"LEGACY-REPLAY-"+c.oldUid,
        ResearchCandidateGenerator.MaterialKind.MOLECULAR_CHEMICAL));
    materialList=List.copyOf(items);
    List<CoreContracts.SensorInstance> sensors=new ArrayList<>();
    for(String model:sensorRows.keySet())sensors.add(
      new CoreContracts.SensorInstance("SIM-"+model,model,"SIM_BOARD","SIM_HEATER"));
    device=new CoreContracts.DeviceProfile("SIM_STABLE_TESTBENCH","SYNTHETIC","NONE",sensors);
  }
  private static SourceCurveExpert.Curve curve(Row c){
    return new SourceCurveExpert.Curve(c.curveId,"MQDATASCIENCE_SINGLE_ROOT",
       c.sensor,c.gas,(long)c.oldUid,
       SourceCurveExpert.Equation.RATIO_EQUALS_A_TIMES_PPM_OVER_SCALE_POWER_B,
       c.a,c.b,c.scale,c.min,c.max,"SIM_HEATER");
  }
  private static final class Inference {
    int top,n,positive;String status;
  }
  private static Inference predict(Row tested,boolean all,boolean qualified,boolean cleanAir) {
    List<CoreContracts.Reading> readings=new ArrayList<>();
    for(String sensor:sensorRows.keySet()){
      double ratio=cleanAir?1.0:((tested!=null && sensor.equals(tested.sensor))?tested.ratio:1.0);
      readings.add(new CoreContracts.Reading("SIM-"+sensor,null,null,10.0*ratio,
        10.0,10.0,CoreContracts.CalibrationReference.SOURCE_R0,
        "SIM_HEATER",CoreContracts.SampleQuality.VALID));
    }
    String snapshotId=(tested==null?"CLEAN":tested.curveId+"-"+tested.step)
                        +(all?"-ALL":"-ISOLATED")+(qualified?"-Q":"-NQ");
    var snap=new CoreContracts.Snapshot(snapshotId,device.deviceUid,CoreContracts.Mode.LAB,
       50000L,24.,48.,true,readings,"SYNTHETIC_SOURCE_MANIFEST");
    List<ChemCoreNext.CurveBinding> bindings=new ArrayList<>();
    if(all)for(List<Row> a:sensorRows.values())for(Row c:a)bindings.add(
      new ChemCoreNext.CurveBinding(curve(c),"SIM-"+c.sensor,
        qualified?TEST_ONLY:SourceCurveExpert.Qualification.unverified()));
    else if(tested!=null)bindings.add(new ChemCoreNext.CurveBinding(
       curve(tested),"SIM-"+tested.sensor,
       qualified?TEST_ONLY:SourceCurveExpert.Qualification.unverified()));
    var inp=new ChemCoreNext.Input(device,snap,ChemCoreNext.snapshotHash(snap),
       materialList,bindings,List.of(),100);
    var out=ChemCoreNext.analyze(inp);
    if(out.molecularIdentityConfirmed||out.ppmAuthorized||out.g3Qualified||out.safetyAlarmAuthorized)
       throw new IllegalStateException("Unqualified simulated research must not certify");
    Inference r=new Inference();r.n=out.arbitration.eligibleCount;
    r.top=out.arbitration.eligibleRanked.isEmpty()?0:(int)out.arbitration.eligibleRanked.get(0).uid;
    r.status=out.arbitration.status.name();r.positive=out.positiveModels;
    return r;
  }
  private static void add(String kind,Row c,Inference i){
    OUTPUT.add(String.join(",",kind,c==null?"AIR":c.curveId,
      c==null?"":c.sensor,c==null?"":c.gas,c==null?"":Double.toString(c.ppm),
      c==null?"":Integer.toString(c.oldUid),
      Integer.toString(i.top),Integer.toString(i.n),Integer.toString(i.positive),
      i.status,Boolean.toString(c!=null&&i.top==c.oldUid),
      "ALL_PHYSICAL_AND_QUALIFICATION_FLAGS_SYNTHETIC"));
  }
  public static void main(String[] a)throws Exception{
    if(a.length!=2)throw new IllegalArgumentException("Usage source_math.csv output.csv");
    readCSV(Path.of(a[0]));
    if(rows.size()!=301||sensorRows.values().stream().mapToInt(List::size).sum()!=43)
       throw new IllegalArgumentException("Expected all 43 sources x 7 concentration levels");
    OUTPUT.add("scenario,source_curve_id,sensor,gas,true_source_ppm,expected_legacy_uid,top1_legacy_uid,eligible_count,positive_models,status,top1_equal_source_uid,qualification");
    int isolatedCorrect=0,combinedCorrect=0,qualRejected=0;
    for(Row c:rows){
      var isolated=predict(c,false,true,false);add("ONE_SOURCE_INJECTED",c,isolated);
      if(isolated.top==c.oldUid)isolatedCorrect++;
      var combined=predict(c,true,true,false);add("ALL_43_SOURCES_INJECTED",c,combined);
      if(combined.top==c.oldUid)combinedCorrect++;
      var rejected=predict(c,true,false,false);add("ALL_SOURCE_GATES_UNVERIFIED",c,rejected);
      if(rejected.n==0)qualRejected++;
      double forward=SourceCurveExpert.forwardSourceRatio(curve(c),c.ppm);
      double inverse=SourceCurveExpert.inverseSourcePpm(curve(c),forward);
      double err=100.0*Math.abs(inverse-c.ppm)/c.ppm;
      if(err>1e-9||Math.abs(forward/c.ratio-1)>1e-9)
        throw new IllegalStateException("source numerical inversion mismatch "+c.curveId);
      var sensor=device.installedSensors.stream().filter(s->s.model.equals(c.sensor)).findFirst().orElseThrow();
      var reading=new CoreContracts.Reading("SIM-"+c.sensor,null,null,10.0*forward,10.,10.,
       CoreContracts.CalibrationReference.SOURCE_R0,"SIM_HEATER",CoreContracts.SampleQuality.VALID);
      var research=SourceCurveExpert.evaluate(curve(c),sensor,reading,TEST_ONLY);
      if(research.decision!=SourceCurveExpert.Decision.RESEARCH_SOURCE_QUANTITATIVE_ESTIMATE)
        throw new IllegalStateException("not source research result "+c.curveId+" "+research.decision);
      if(research.fieldPpmAuthorized||research.confirmedGas)
        throw new IllegalStateException("G1 source algebra must not certify field ppm");
    }
    int airCandidates=0;
    for(String model:sensorRows.keySet()){
      Row first=sensorRows.get(model).get(0);
      var clean=predict(first,true,true,true);
      add("CLEAN_AIR_REFERENCE_ALL_43",first,clean);
      if(clean.n>0)airCandidates++;
    }
    Path dst=Path.of(a[1]);Files.createDirectories(dst.getParent());
    Files.writeString(dst,String.join("\n",OUTPUT)+"\n");
    System.out.println("NEXT_STABLE_CURVE_ACTUAL_V8_CORE_AUDIT_PASS"+
      " source_curve_samples="+rows.size()+" isolated_top1="+isolatedCorrect+
      " all43_top1="+combinedCorrect+" unverified_rejected="+qualRejected+
      " clean_air_sensor_profiles_with_false_candidates="+airCandidates+
      " no_real_Tank_accuracy=true");
  }
}
