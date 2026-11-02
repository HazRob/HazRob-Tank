package com.hazrob.tank.chemcore.next;
import java.util.*;
public final class NextFieldArchiveSmoke {
  static int n;static void yes(boolean b,String why){n++;if(!b)throw new AssertionError("V8-P09.2-"+n+" "+why);}
  static NextFieldArchive.Row row(long time,int cycle,double rs){
    return new NextFieldArchive.Row(time,"{\"temp\":23}","{\"mq2\":{\"rs\":"+rs+"}}",
      "{\"sample_seq\":"+cycle+"}",Map.of("mq2",new NextFieldArchive.Scan(cycle,rs,100L)));
  }
  public static void main(String[] a){
    List<NextFieldArchive.Row> valid=new ArrayList<>();
    for(int i=0;i<61;i++)valid.add(row(i*1000L,i,100.0+i));
    var full=NextFieldArchive.summarize(valid);
    yes(full.hasMeasuredEvidence(),"61 real physical scanned 60s");
    yes(full.coverageMs==60000L&&full.rawRows==61,"source genuine time coverage");
    yes(full.distinctGlobalGasScanCycles==61,"deduplicated physical scan IDs");
    yes(full.qualifiedChannels.size()==1,"no invented 7 other sensors");
    yes(full.qualifiedChannels.get("mq2").medianRsKohm==130,"median real Rs not last snapshot");
    yes(full.sourceSha256.matches("[0-9a-f]{64}"),"hashed entire source telemetry rows");
    yes(full.sourceSha256.equals(NextFieldArchive.summarize(valid).sourceSha256),"replay deterministic");
    // Some firmware snapshots omit last_ok_age_ms. Keep the archived Rs as
    // research evidence but mark it unqualified (age=-1), never invent 0ms.
    List<NextFieldArchive.Row> unknownAge=new ArrayList<>();
    for(int i=0;i<61;i++)unknownAge.add(new NextFieldArchive.Row(i*1000L,
      "{\"temp\":23}","{\"mq2\":{\"rs\":"+i+"}}",
      "{\"sample_seq\":"+i+"}",Map.of("mq2",
        new NextFieldArchive.Scan(i,80.0+i,-1L))));
    var rawUnknown=NextFieldArchive.summarize(unknownAge);
    yes(rawUnknown.hasMeasuredEvidence(),"independent archived scans preserved with unknown age");
    yes(rawUnknown.qualifiedChannels.get("mq2").lastAgeMs==-1,
      "unknown freshness is not asserted 0ms");

    List<NextFieldArchive.Row> duplicates=new ArrayList<>();
    for(int i=0;i<61;i++)duplicates.add(row(i*1000L,i/10,100+i));
    var dup=NextFieldArchive.summarize(duplicates);
    yes(!dup.hasMeasuredEvidence()&&dup.qualifiedChannels.isEmpty(),
      "61 repeated HTTP rows are just 7 physical scans");
    List<NextFieldArchive.Row> shortTime=new ArrayList<>();
    for(int i=0;i<45;i++)shortTime.add(row(i*1000L,i,10+i));
    yes(!NextFieldArchive.summarize(shortTime).hasMeasuredEvidence(),"time 44s not 60s");
    var tamper=new ArrayList<>(valid);tamper.set(30,new NextFieldArchive.Row(
      30000L,"{\"temp\":99}","{\"mq2\":{\"rs\":130.0}}","{\"sample_seq\":30}",
      Map.of("mq2",new NextFieldArchive.Scan(30,130.,100))));
    yes(!full.sourceSha256.equals(NextFieldArchive.summarize(tamper).sourceSha256),
      "raw provenance tamper changes hash even if model median equal");
    var missing=NextFieldArchive.summarize(List.of());
    yes(!missing.hasMeasuredEvidence(),"empty must fail");
    boolean rejects=false;try{NextFieldArchive.summarize(null);}catch(IllegalArgumentException ex){rejects=true;}
    yes(rejects,"null archive fails");
    boolean timeOrder=false;try{NextFieldArchive.summarize(List.of(row(1200,2,10),row(1000,1,11)));}
      catch(IllegalArgumentException ex){timeOrder=true;}
    yes(timeOrder,"unordered rows fail");
    System.out.println("CHEMCORE_NEXT_P09_2_FIELD_RAW_ARCHIVE_PASS assertions="+n+
      " original_60sec=true repeat_poll_not_evidence=true metadata_hash_stable=true G3=false");
  }
}
