package com.hazrob.tank.chemcore.next;

import java.util.*;

/**
 * V8 P07.1: strict, Android-free typed boundary. It never learns R_air from
 * a remote source curve or copies an unverified firmware R0 into SOURCE_R0.
 * The original ADC/Rs remains auditable when inference must abstain.
 */
public final class NextTelemetryMapper {
  private NextTelemetryMapper(){}
  public static final String SCHEMA="hazrob.tank.v8.next.telemetry.boundary/1";
  public static final List<String> CHANNELS=List.of("mq2","mq3","mq4","mq5",
      "mq7","mq8","mq9","mq135");

  public static final class Channel {
    public final String model;
    public final Integer raw;
    public final Double voltageV,rsKohm,deviceAirReferenceKohm;
    public final boolean receivedOk,powered;
    public final long lastOkAgeMs;
    public final String sensorStatus,referenceSource,referenceDefinition,
       referenceDeviceId,referenceSession,referenceUtc;
    public final int referenceSamples,referenceWindows;
    public final boolean heaterProtocolQualified;
    public Channel(String model,Integer raw,Double volts,Double rs,Double rAir,
      boolean ok,boolean powered,long age,String status,String source,
      String definition,String boundDevice,String session,String utc,
      int nSamples,int nWindows,boolean heaterQualified){
      if(model==null||!CHANNELS.contains(model.toLowerCase(Locale.ROOT)))
        throw new IllegalArgumentException("one of 8 installed MQ models required");
      this.model=model.toLowerCase(Locale.ROOT);this.raw=raw;voltageV=volts;rsKohm=rs;
      deviceAirReferenceKohm=rAir;receivedOk=ok;this.powered=powered;
      lastOkAgeMs=age;sensorStatus=status==null?"":status;
      referenceSource=source==null?"":source;referenceDefinition=definition==null?"":definition;
      referenceDeviceId=boundDevice==null?"":boundDevice;
      referenceSession=session==null?"":session;referenceUtc=utc==null?"":utc;
      referenceSamples=nSamples;referenceWindows=nWindows;
      heaterProtocolQualified=heaterQualified;
    }
  }
  public static final class Input {
    public final String snapshotUid,deviceUid,hardwareRevision,firmwareBuild,stableSha256;
    public final CoreContracts.Mode mode;
    public final long monotonicMs;
    public final Double temperatureC,rhPercent;
    public final boolean relayConfirmedOn;
    public final List<Channel> channels;
    public Input(String snapshot,String device,String hardware,String firmware,String stableSha,
      CoreContracts.Mode mode,long mono,Double t,Double rh,boolean relayOn,List<Channel> channels){
      snapshotUid=required(snapshot,"snapshot");deviceUid=required(device,"physical device UID");
      hardwareRevision=required(hardware,"hardware");firmwareBuild=required(firmware,"firmware");
      stableSha256=required(stableSha,"Stable payload SHA256");
      if(!stableSha256.matches("[0-9a-fA-F]{64}"))
        throw new IllegalArgumentException("Verified Stable payload SHA-256 required; no fake manifest hash");
      this.mode=Objects.requireNonNull(mode);monotonicMs=mono;
      temperatureC=t;rhPercent=rh;relayConfirmedOn=relayOn;
      this.channels=List.copyOf(Objects.requireNonNull(channels));
    }
    private static String required(String s,String name){
      if(s==null||s.isBlank())throw new IllegalArgumentException(name+" required");
      return s.trim();
    }
  }
  public static final class Frozen {
    public final CoreContracts.DeviceProfile device;
    public final CoreContracts.Snapshot snapshot;
    public final String snapshotSha256;
    public final int validSourceMeasurements,rawOnlyChannels,excludedHeaterChannels,
      excludedUnverifiedReferenceChannels;
    private Frozen(CoreContracts.DeviceProfile device,CoreContracts.Snapshot snapshot,
                   int valid,int raw,int heaters,int refs){
      this.device=device;this.snapshot=snapshot;
      snapshotSha256=ChemCoreNext.snapshotHash(snapshot);
      validSourceMeasurements=valid;rawOnlyChannels=raw;excludedHeaterChannels=heaters;
      excludedUnverifiedReferenceChannels=refs;
    }
  }
  private static Double positiveOrNull(Double x){
    return x!=null&&Double.isFinite(x)&&x>0?x:null;
  }
  private static Double finiteOrNull(Double x){
    return x!=null&&Double.isFinite(x)?x:null;
  }
  private static boolean acceptableAir(String deviceId,Channel c){
    return positiveOrNull(c.deviceAirReferenceKohm)!=null
      &&"DEVICE_CLEAN_AIR_RS_MEAN".equals(c.referenceDefinition)
      &&("firmware_clean_air_r0".equals(c.referenceSource)
         ||"automatic_clean_air_field_r0".equals(c.referenceSource))
      &&deviceId.equals(c.referenceDeviceId)
      &&!c.referenceSession.isBlank()&&!c.referenceUtc.isBlank()
      &&c.referenceSamples>=10&&c.referenceWindows>=1;
  }
  public static Frozen freeze(Input in){
    if(in==null)throw new IllegalArgumentException("input missing");
    Map<String,Channel> byModel=new HashMap<>();
    for(Channel c:in.channels) {
      if(c==null||byModel.put(c.model,c)!=null)
        throw new IllegalArgumentException("duplicate/null sensor input");
    }
    List<CoreContracts.SensorInstance> inventory=new ArrayList<>();
    List<CoreContracts.Reading> readings=new ArrayList<>();
    int valid=0,raw=0,heater=0,references=0;
    for(String model:CHANNELS){
      final String uid=in.deviceUid+":"+model.toUpperCase(Locale.ROOT);
      boolean heaterOk=!(model.equals("mq7")||model.equals("mq9"));
      Channel c=byModel.get(model);
      if(c!=null)heaterOk=heaterOk&&c.heaterProtocolQualified;
      inventory.add(new CoreContracts.SensorInstance(uid,model.toUpperCase(Locale.ROOT),
        in.hardwareRevision,heaterOk?"REPORTED_READY_UNVALIDATED_G3":"CYCLIC_MQ_HEATER_UNQUALIFIED"));
      CoreContracts.SampleQuality quality;
      Double rs=c==null?null:positiveOrNull(c.rsKohm);
      boolean knownReference=c!=null&&acceptableAir(in.deviceUid,c);
      Double air=knownReference?positiveOrNull(c.deviceAirReferenceKohm):null;
      if(c==null)quality=CoreContracts.SampleQuality.NO_SAMPLE;
      else if(!in.relayConfirmedOn || !c.powered ||
           c.sensorStatus.equalsIgnoreCase("POWER_OFF"))
        quality=CoreContracts.SampleQuality.NO_SAMPLE;
      else if(c.sensorStatus.equalsIgnoreCase("ADC_UNAVAILABLE") ||
           c.sensorStatus.equalsIgnoreCase("MISSING_SOURCE") || !c.receivedOk || rs==null)
        quality=CoreContracts.SampleQuality.NO_SAMPLE;
      else if(c.lastOkAgeMs<0||c.lastOkAgeMs>6000 ||
              c.sensorStatus.equalsIgnoreCase("STALE"))
        quality=CoreContracts.SampleQuality.STALE;
      else if(c.sensorStatus.equalsIgnoreCase("SATURATED") ||
              c.sensorStatus.equalsIgnoreCase("RAIL_HIGH_OR_SATURATED"))
        quality=CoreContracts.SampleQuality.SATURATED_ADC;
      else if(!heaterOk)quality=CoreContracts.SampleQuality.BAD_HEATER_PHASE;
      else if(!knownReference)quality=CoreContracts.SampleQuality.MISSING_R_AIR;
      else quality=CoreContracts.SampleQuality.VALID;
      if(quality==CoreContracts.SampleQuality.VALID)valid++;
      else {
        if(rs!=null)raw++;
        if(quality==CoreContracts.SampleQuality.BAD_HEATER_PHASE)heater++;
        if(quality==CoreContracts.SampleQuality.MISSING_R_AIR)references++;
      }
      // Raw resistance may exist despite failed eligibility; device air is never
      // aliased to source R0. Never claim source concentration or G3.
      readings.add(new CoreContracts.Reading(uid,c==null?null:c.raw,
        c==null?null:finiteOrNull(c.voltageV),rs,air,null,
        air==null?CoreContracts.CalibrationReference.NOT_AVAILABLE:
                  CoreContracts.CalibrationReference.AIR_RS_MEAN,
        heaterOk?"SOURCE_HEATER_UNQUALIFIED_FOR_G3":"CYCLIC_HEATER_NOT_INSTALLED",quality));
    }
    var profile=new CoreContracts.DeviceProfile(in.deviceUid,in.hardwareRevision,
      in.firmwareBuild,inventory);
    var snapshot=new CoreContracts.Snapshot(in.snapshotUid,in.deviceUid,in.mode,
      in.monotonicMs,in.temperatureC,in.rhPercent,in.relayConfirmedOn,
      readings,in.stableSha256.toLowerCase(Locale.ROOT));
    return new Frozen(profile,snapshot,valid,raw,heater,references);
  }
}
