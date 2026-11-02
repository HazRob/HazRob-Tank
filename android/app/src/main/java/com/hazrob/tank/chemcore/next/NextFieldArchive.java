package com.hazrob.tank.chemcore.next;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * V8 P09.2 original 60-second MQ series: stable source hash and per-channel
 * median of DISTINCT physical scan cycles. No repeated 200ms Android polls
 * become independent observations. This is NOT a learned chemical fingerprint.
 */
public final class NextFieldArchive {
  private NextFieldArchive(){}
  public static final String SCHEMA="hazrob.tank.v8.field.raw_mq_archive/1";
  private static final List<String> INSTALLED=NextTelemetryMapper.CHANNELS;
  public static final class Scan {
    public final long cycle;public final double rsKohm;public final long ageMs;
    public Scan(long cycle,double rsKohm,long ageMs){
      this.cycle=cycle;this.rsKohm=rsKohm;this.ageMs=ageMs;
    }
  }
  public static final class Row {
    public final long timestampMs;
    public final String envJson,mqJson,motionJson;
    public final Map<String,Scan> scans;
    public Row(long t,String env,String mq,String motion,Map<String,Scan> scans){
      if(t<0||env==null||mq==null||motion==null||scans==null)
        throw new IllegalArgumentException("original archived row required");
      timestampMs=t;envJson=env;mqJson=mq;motionJson=motion;
      this.scans=Map.copyOf(scans);
    }
  }
  public static final class ChannelSummary {
    public final String model;
    public final int distinctCycles;
    public final double medianRsKohm;
    public final long lastAgeMs;
    private ChannelSummary(String model,int count,double median,long lastAge){
      this.model=model;distinctCycles=count;medianRsKohm=median;lastAgeMs=lastAge;
    }
  }
  public static final class Summary {
    public final String sourceSha256;
    public final int rawRows;
    public final long earliestTms,latestTms,coverageMs;
    public final int distinctGlobalGasScanCycles;
    public final Map<String,ChannelSummary> qualifiedChannels;
    public final String status;
    private Summary(String sha,int rows,long first,long last,int n,Map<String,ChannelSummary> found) {
      sourceSha256=sha;rawRows=rows;earliestTms=first;latestTms=last;
      coverageMs=rows==0?0:last-first;distinctGlobalGasScanCycles=n;
      qualifiedChannels=Collections.unmodifiableMap(new LinkedHashMap<>(found));
      status=rows>0 && coverageMs>=58000L && n>=10 && !found.isEmpty()?
        "SOURCE_ARCHIVE_ELIGIBLE_FOR_RESEARCH_UNKNOWN_ONLY":"INSUFFICIENT_REAL_FIELD_ARCHIVE";
    }
    public boolean hasMeasuredEvidence(){return status.startsWith("SOURCE_ARCHIVE_ELIGIBLE");}
  }
  private static void feed(MessageDigest d,String s) {
    byte[] bytes=s.getBytes(StandardCharsets.UTF_8);
    int n=bytes.length;
    d.update((byte)(n>>>24));d.update((byte)(n>>>16));
    d.update((byte)(n>>>8));d.update((byte)n);d.update(bytes);
  }
  public static Summary summarize(List<Row> rows) {
    if(rows==null)throw new IllegalArgumentException("saved field rows required");
    try{
      MessageDigest d=MessageDigest.getInstance("SHA-256");
      Map<String,Map<Long,Scan>> unique=new LinkedHashMap<>();
      for(String s:INSTALLED)unique.put(s,new LinkedHashMap<>());
      Set<Long> globalCycles=new HashSet<>();
      long first=Long.MAX_VALUE,last=0,previous=-1;
      for(Row r:rows){
        if(r==null||r.timestampMs<previous)
          throw new IllegalArgumentException("archived rows must retain original time order");
        previous=r.timestampMs;first=Math.min(first,r.timestampMs);
        last=Math.max(last,r.timestampMs);
        feed(d,Long.toString(r.timestampMs));feed(d,r.envJson);
        feed(d,r.mqJson);feed(d,r.motionJson);
        for(var entry:r.scans.entrySet()){
          String model=entry.getKey();
          if(!unique.containsKey(model))
            throw new IllegalArgumentException("unknown physically recorded sensor "+model);
          Scan p=entry.getValue();
          if(p==null||p.cycle<0||!Double.isFinite(p.rsKohm)||p.rsKohm<=0||
              p.ageMs<0||p.ageMs>6000L)continue;
          unique.get(model).putIfAbsent(p.cycle,p);
          globalCycles.add(p.cycle);
        }
      }
      LinkedHashMap<String,ChannelSummary> sensors=new LinkedHashMap<>();
      for(var entry:unique.entrySet()){
        if(entry.getValue().size()<10)continue;
        List<Double> r=new ArrayList<>();
        long mostRecentAge=Long.MAX_VALUE;
        for(Scan scan:entry.getValue().values()){
          r.add(scan.rsKohm);
          mostRecentAge=Math.min(mostRecentAge,scan.ageMs);
        }
        Collections.sort(r);int n=r.size();
        double median=n%2==0?(r.get(n/2-1)+r.get(n/2))/2:r.get(n/2);
        sensors.put(entry.getKey(),new ChannelSummary(entry.getKey(),n,median,mostRecentAge));
      }
      StringBuilder hex=new StringBuilder(64);
      for(byte x:d.digest())hex.append(String.format(Locale.ROOT,"%02x",x&255));
      return new Summary(hex.toString(),rows.size(),rows.isEmpty()?0:first,
        last,globalCycles.size(),sensors);
    }catch(java.security.NoSuchAlgorithmException ex){
      throw new IllegalStateException("SHA256 engine unavailable",ex);
    }
  }
}
