package com.hazrob.tank.chemcore.next;
import java.util.*;

/** P07.1 exact eight-MQ source input, untrusted R0 and fail-closed replay. */
public final class NextTelemetryMapperSmoke {
  private static int n;
  private static void yes(boolean b,String msg){n++;if(!b)throw new AssertionError("P07.1-"+n+" "+msg);}
  private static final String SHA="0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
  private static NextTelemetryMapper.Channel ch(String name,Double rs,Double air,
       String src,String def,String device,int samples,int windows,boolean heater) {
    return new NextTelemetryMapper.Channel(name,457,2.9,rs,air,true,true,50,
      "VALID",src,def,device,"FIRMWARE-V6-QUICK-20261031","2026-10-31T03:29:55Z",
      samples,windows,heater);
  }
  private static NextTelemetryMapper.Frozen run(List<NextTelemetryMapper.Channel> ch,
                                                 boolean relay) {
    return NextTelemetryMapper.freeze(new NextTelemetryMapper.Input(
      "source-original-001","tank-physical-001","doit-tank-2ADS","ESP32-Tank-V6",
      SHA,CoreContracts.Mode.FIELD_SESSION,723000,25.0,60.0,relay,ch));
  }
  private static CoreContracts.Reading get(NextTelemetryMapper.Frozen r,String model){
    return r.snapshot.readings.stream().filter(x->x.sensorUid.endsWith(":"+model))
      .findFirst().orElseThrow();
  }
  private static void bad(Runnable r,String why){
    boolean failed=false;try{r.run();}catch(IllegalArgumentException e){failed=true;}
    yes(failed,why);
  }
  public static void main(String[] args){
    var air=ch("mq2",92.1417313,92.1417313,"firmware_clean_air_r0",
        "DEVICE_CLEAN_AIR_RS_MEAN","tank-physical-001",437,1,true);
    var r=run(List.of(air),true);
    yes(r.snapshot.readings.size()==8&&r.device.installedSensors.size()==8,"eight installed, absent not fabricated");
    yes(r.validSourceMeasurements==1,"one valid source measured channel");
    yes(get(r,"MQ2").quality==CoreContracts.SampleQuality.VALID,"genuine same-device air");
    yes(Math.abs(get(r,"MQ2").airRatioOrNaN()-1)<1e-12,"measured air ratio unity");
    yes(Double.isNaN(get(r,"MQ2").sourceRatioOrNaN()),"device R_air never manufacturer R0");
    yes(get(r,"MQ4").quality==CoreContracts.SampleQuality.NO_SAMPLE,"no fake missing MQ4");
    yes(get(r,"MQ4").rawAdc==null,"absent raw remains null");
    yes(r.snapshotSha256.matches("[0-9a-f]{64}"),"stable canonical hash");
    yes(r.snapshotSha256.equals(ChemCoreNext.snapshotHash(r.snapshot)),"snap immutable reproducible");
    var unknown=run(List.of(ch("mq3",25.5,25.5,"manual_device_r0",
        "UNVERIFIED_MANUAL_OR_UNAVAILABLE","tank-physical-001",0,0,true)),true);
    yes(get(unknown,"MQ3").quality==CoreContracts.SampleQuality.MISSING_R_AIR,
      "manual numeric calibration is not independently verified air");
    yes(get(unknown,"MQ3").rsKohm==25.5&&get(unknown,"MQ3").rAirKohm==null,"retain raw Rs but abstain");
    var foreign=run(List.of(ch("mq2",92.0,92.0,"firmware_clean_air_r0",
        "DEVICE_CLEAN_AIR_RS_MEAN","other-tank",437,1,true)),true);
    yes(get(foreign,"MQ2").quality==CoreContracts.SampleQuality.MISSING_R_AIR,
      "reject other robot saved R_air");
    var v5=run(List.of(ch("mq2",92.0,92.0,"firmware_clean_air_r0",
        "LEGACY_MQ_FACTOR_SCALED_R0_NOT_AIR_RS","tank-physical-001",437,1,true)),true);
    yes(get(v5,"MQ2").quality==CoreContracts.SampleQuality.MISSING_R_AIR,
      "reject historical factory factor R0");
    var noWindow=run(List.of(ch("mq2",92.0,92.0,"firmware_clean_air_r0",
        "DEVICE_CLEAN_AIR_RS_MEAN","tank-physical-001",437,0,true)),true);
    yes(noWindow.validSourceMeasurements==0,"no measured window provenance");
    var off=run(List.of(air),false);
    yes(off.validSourceMeasurements==0&&get(off,"MQ2").quality==CoreContracts.SampleQuality.NO_SAMPLE,
      "relay off overrides stale previous valid readings");
    var mq7=run(List.of(ch("mq7",60.,60.,"firmware_clean_air_r0",
        "DEVICE_CLEAN_AIR_RS_MEAN","tank-physical-001",437,1,false)),true);
    yes(mq7.excludedHeaterChannels==1&&get(mq7,"MQ7").quality==CoreContracts.SampleQuality.BAD_HEATER_PHASE,
      "constant heater MQ7 cannot be chemically qualified");
    var mq9=run(List.of(ch("mq9",55.,55.,"firmware_clean_air_r0",
        "DEVICE_CLEAN_AIR_RS_MEAN","tank-physical-001",437,1,false)),true);
    yes(mq9.excludedHeaterChannels==1,"MQ9 cyclic heater not physically fitted");
    var stale=run(List.of(new NextTelemetryMapper.Channel("mq4",123,1.1,30.,30.,true,true,
      10000,"STALE","firmware_clean_air_r0","DEVICE_CLEAN_AIR_RS_MEAN",
      "tank-physical-001","r0x","utc",437,1,true)),true);
    yes(get(stale,"MQ4").quality==CoreContracts.SampleQuality.STALE,"stale telemetry blocked");
    var nan=run(List.of(ch("mq2",Double.NaN,92.,"firmware_clean_air_r0",
        "DEVICE_CLEAN_AIR_RS_MEAN","tank-physical-001",437,1,true)),true);
    yes(get(nan,"MQ2").rsKohm==null&&get(nan,"MQ2").quality==CoreContracts.SampleQuality.NO_SAMPLE,
      "invalid voltage/resistance cannot cause fake Rs");
    var one=run(List.of(ch("mq3",13.,26.,"firmware_clean_air_r0",
      "DEVICE_CLEAN_AIR_RS_MEAN","tank-physical-001",437,1,true)),true);
    yes(one.validSourceMeasurements==1&&Math.abs(get(one,"MQ3").airRatioOrNaN()-.5)<1e-12,
      "single source sensor remains measurable but not identified");
    bad(()->run(List.of(air,air),true),"duplicate channel fails");
    bad(()->NextTelemetryMapper.freeze(new NextTelemetryMapper.Input("id","dev","hw","fw",
      "not a real sha",CoreContracts.Mode.INSTANT,1L,null,null,true,List.of())),
      "fake stable sha rejected");
    boolean immutable=false;
    try{r.snapshot.readings.clear();}catch(UnsupportedOperationException ex){immutable=true;}
    yes(immutable,"snapshot cannot be modified after freeze");
    var no=ChemCoreNext.analyze(new ChemCoreNext.Input(r.device,r.snapshot,r.snapshotSha256,
      List.of(),List.of(),List.of(),10));
    yes(no.research.status==CoreContracts.DecisionStatus.UNKNOWN,"no source fingerprint gas given baseline only");
    yes(!no.g3Qualified&&!no.ppmAuthorized&&!no.safetyAlarmAuthorized,"no G3 or ppm promoted");
    System.out.println("CHEMCORE_NEXT_P07_1_TELEMETRY_MAPPER_PASS checks="+n+
      " owner_8mq_preserved=true genuine_rAir_only=true raw_only_retained=true G3=false");
  }
}
