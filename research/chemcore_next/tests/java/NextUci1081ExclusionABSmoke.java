package com.hazrob.tank.chemcore.next;

import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

/**
 * Reproducible research comparison of the IDENTICAL actual ChemCoreNext
 * entrypoint before and after temporarily excluding UCI1081 source evidence.
 *
 * ALL vectors, qualifications, ratios, source-derived thresholds and unknown
 * cases are synthetic injection probes. No physical gas measurements or
 * externally validated R0 are contained here.
 */
public final class NextUci1081ExclusionABSmoke {
  private NextUci1081ExclusionABSmoke() {}
  private static final String UCI="UCI1081_ORIGINAL_SOURCE";
  private static final String MQ="MQ_LAB_INDEPENDENT_SOURCE_EXPERIMENT";
  private static final long METHANE=200L, ETHANOL=100L;
  private static final CoreContracts.DeviceProfile DEVICE=new CoreContracts.DeviceProfile(
    "SYNTHETIC_AB_DEVICE","SYNTHETIC","NO_REAL_FIRMWARE",
    List.of(
      new CoreContracts.SensorInstance("PHYSICAL-MQ4","MQ4","slot4","READY"),
      new CoreContracts.SensorInstance("PHYSICAL-MQ3","MQ3","slot3","READY")
    ));
  private static final List<ResearchCandidateGenerator.Material> MATERIALS=List.of(
    new ResearchCandidateGenerator.Material(METHANE,"METHANE",
      ResearchCandidateGenerator.MaterialKind.MOLECULAR_CHEMICAL),
    new ResearchCandidateGenerator.Material(ETHANOL,"ETHANOL",
      ResearchCandidateGenerator.MaterialKind.MOLECULAR_CHEMICAL)
  );
  private static CoreContracts.Snapshot snapshot(String scenario,boolean relay){
    return new CoreContracts.Snapshot("SIM-AB-"+scenario,DEVICE.deviceUid,
      CoreContracts.Mode.LAB,60000,23.0,42.0,relay,
      List.of(
        new CoreContracts.Reading("PHYSICAL-MQ4",null,null,12.0,12.0,12.0,
          CoreContracts.CalibrationReference.SOURCE_R0,"READY",
          CoreContracts.SampleQuality.VALID),
        new CoreContracts.Reading("PHYSICAL-MQ3",null,null,10.0,10.0,10.0,
          CoreContracts.CalibrationReference.SOURCE_R0,"READY",
          CoreContracts.SampleQuality.VALID)
      ),"SIMULATED_STABLE_ONLY");
  }
  private static ChemCoreNext.FingerprintBinding fp(String root,long uid,String model,String sensor,
       String caseName,boolean verified){
    var sig=new FingerprintExpert.Signature("SIGN-"+caseName,root,
      "SIMULATED_SOURCE_FEATURE_SPACE",model,uid,
      List.of("SOURCE_ONLY_F00"),List.of(0.20),List.of(0.10),1);
    var obs=new FingerprintExpert.Observation("SIMULATED_SOURCE_FEATURE_SPACE",
       model,"SIMULATED_CAPTURE_"+caseName,Map.of("SOURCE_ONLY_F00",0.205),true);
    return new ChemCoreNext.FingerprintBinding(sig,obs,
      new FingerprintExpert.Qualification(true,true),sensor,
      1.0,verified);
  }
  private static ChemCoreNext.CurveBinding curve(String root,long uid,String model,String sensor){
    return new ChemCoreNext.CurveBinding(
      new SourceCurveExpert.Curve("CURVE_"+root,root,model,"synthetic-ethanol",
        uid,SourceCurveExpert.Equation.PPM_EQUALS_A_TIMES_RATIO_POWER_B,
        100.0,-1.0,null,10.0,500.0,"READY"),
      sensor,new SourceCurveExpert.Qualification(true,true,true,true)
    );
  }
  private static ResearchFingerprintBundle.Bundle bundle(String root,long uid){
    var sign=new FingerprintExpert.Signature("BUNDLE_UCI_SIGN",
      root,"SIMULATED_SOURCE_FEATURE_SPACE","MQ3",uid,
      List.of("SOURCE_ONLY_F00"),List.of(0.20),List.of(0.10),1);
    var obs=new FingerprintExpert.Observation("SIMULATED_SOURCE_FEATURE_SPACE",
      "MQ3","SIMULATED_CAPTURE_UCI_BUNDLE",Map.of("SOURCE_ONLY_F00",0.205),true);
    var variant=new ResearchFingerprintBundle.Variant(sign,obs,
      new FingerprintExpert.Qualification(true,true),"PHYSICAL-MQ3",
      "SIMULATED_RAW","UCI1081_TECHNIQUE",1.0,true,100.0);
    return new ResearchFingerprintBundle.Bundle("UCI1081_BUNDLE",root,uid,
      List.of(variant),1,1,1);
  }
  private static String evaluate(String scenario,List<ChemCoreNext.FingerprintBinding> fps,
      List<ChemCoreNext.CurveBinding> curves,List<ResearchFingerprintBundle.Bundle> bundles,
      boolean relay){
    CoreContracts.Snapshot snap=snapshot(scenario,relay);
    var in=new ChemCoreNext.Input(DEVICE,snap,ChemCoreNext.snapshotHash(snap),
      MATERIALS,curves,fps,bundles,10);
    ChemCoreNext.Output r=ChemCoreNext.analyze(in);
    if(r.safetyAlarmAuthorized||r.ppmAuthorized||r.molecularIdentityConfirmed||r.g3Qualified)
      throw new AssertionError("Research test promoted a safety flag!");
    int top=r.arbitration.display.isEmpty()?0:(int)r.arbitration.display.get(0).uid;
    String d=String.join(";",r.sourceDiagnostics).replace(',',' ');
    return String.join(",",scenario,r.arbitration.status.name(),Integer.toString(top),
      Integer.toString(r.arbitration.eligibleCount),
      Integer.toString(r.graph.independentSourceRoots),
      Integer.toString(r.positiveModels),
      d);
  }
  public static void main(String[] args)throws Exception{
    if(args.length!=1)throw new IllegalArgumentException("CSV output path required");
    var fpUci=fp(UCI,ETHANOL,"MQ3","PHYSICAL-MQ3","UCI1081",true);
    var fpMq=fp(MQ,METHANE,"MQ4","PHYSICAL-MQ4","MQPHYSICAL",true);
    List<String> rows=new ArrayList<>();
    rows.add(evaluate("no_source",List.of(),List.of(),List.of(),true));
    rows.add(evaluate("mq_only",List.of(fpMq),List.of(),List.of(),true));
    rows.add(evaluate("uci_only",List.of(fpUci),List.of(),List.of(),true));
    rows.add(evaluate("uci_and_mq",List.of(fpUci,fpMq),List.of(),List.of(),true));
    rows.add(evaluate("uci_curve_only",List.of(),List.of(curve(UCI,ETHANOL,"MQ3","PHYSICAL-MQ3")),List.of(),true));
    rows.add(evaluate("uci_bundle_only",List.of(),List.of(),List.of(bundle(UCI,ETHANOL)),true));
    rows.add(evaluate("uci_relay_off",List.of(fpUci),List.of(),List.of(),false));
    rows.add(evaluate("mq_unverified_threshold",List.of(
      fp(MQ,METHANE,"MQ4","PHYSICAL-MQ4","UNVERIFIED",false)),List.of(),List.of(),true));
    rows.add(evaluate("uci_alias_source",List.of(
      fp("gas_sensor_array_low-concentration",ETHANOL,"MQ3","PHYSICAL-MQ3","UCI_ALIAS",true)),
      List.of(),List.of(),true));
    rows.add(evaluate("uci_and_mq_multiple_uci_variants",List.of(fpUci,fpUci,fpMq),
      List.of(),List.of(),true));
    Path out=Path.of(args[0]);Files.createDirectories(out.toAbsolutePath().getParent());
    String head="scenario,status,top_uid,eligible_count,source_root_count,positive_models,diagnostics";
    Files.writeString(out,head+"\n"+String.join("\n",rows)+"\n",StandardCharsets.UTF_8);
    System.out.println("NEXT_UCI_QUARANTINE_INFERENCE_AB_DONE cases="+rows.size()
      +" gates=false_user_facing_ppm_identity_g3");
  }
}
