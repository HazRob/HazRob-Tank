package com.hazrob.tank.chemcore.next;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * P06.1/P06.4 research-core G1: exact owner Tank 8 sensor clean-air fixture,
 * defensive invalid states, immutable complete graph/arbitration/result,
 * dynamic master catalog and Android-compatible Java17 CPU/heap profile.
 * Does NOT invent gas exposures from the R_air means or report gas ppm.
 */
public final class NextP06IntegratedCoreSmoke {
  private NextP06IntegratedCoreSmoke(){}
  private static int checks;
  private static void require(boolean yes,String why){
    checks++;if(!yes)throw new AssertionError("P06 "+checks+": "+why);
  }
  private static final String AIR_JSON="research/chemcore_next/device_calibration/tank_8mq_v6_quick_clean_air_2026-10-31T032955Z.json";
  private static final LinkedHashMap<String,Double> AIR=new LinkedHashMap<>();
  private static void loadBaseline()throws Exception {
    String s=Files.readString(Path.of(AIR_JSON),StandardCharsets.UTF_8);
    int begin=s.indexOf("\"r_air_kohm_by_sensor\""),stop=s.indexOf("}",begin);
    require(begin>=0&&stop>begin,"owner source physical reference section present");
    String body=s.substring(begin,stop);
    String[] labels={"MQ2","MQ3","MQ4","MQ5","MQ7","MQ8","MQ9","MQ135"};
    for(String label:labels){
      var m=java.util.regex.Pattern.compile("\""+label+"\"\\s*:\\s*([0-9]+\\.[0-9]+)").matcher(body);
      require(m.find(),"real Tank source baseline "+label);
      double value=Double.parseDouble(m.group(1));
      require(value>0&&Double.isFinite(value),"physically measured mean positive "+label);
      AIR.put(label,value);
    }
    require(AIR.size()==8,"all eight physical sensor models");
  }
  private static CoreContracts.EvidenceNode node(String root,long uid,CoreContracts.EvidencePolarity p){
    return new CoreContracts.EvidenceNode(root,"SOURCE_RESEARCH","Tank:MQ4",uid,p,
      "synthetic test polarity NOT a physical Tank gas challenge");
  }
  private static List<ResearchCandidateGenerator.Material> catalog(){
    List<ResearchCandidateGenerator.Material> result=new ArrayList<>();
    for(long id=1;id<=736;id++)
      result.add(new ResearchCandidateGenerator.Material(id,"master-chemical-uid-"+id,
        id>734?ResearchCandidateGenerator.MaterialKind.GENERAL_CLASS:
          ResearchCandidateGenerator.MaterialKind.MOLECULAR_CHEMICAL));
    return List.copyOf(result);
  }
  private static NextResearchDecisionPipeline.Decision evaluate(List<ResearchCandidateGenerator.Material> master,
      CoreContracts.EvidenceNode... evidence){
    var summary=IndependentEvidenceLedger.consolidate("snapshot-replay-physical-source-reference",
      List.of(evidence));
    var refs=new ArrayList<ResearchCandidateGenerator.AssetLink>();
    for(var root:summary.roots)if(root.positiveResearchHypothesis())
      refs.add(new ResearchCandidateGenerator.AssetLink(root.sourceRootId,"SOURCE-LOCKED-"+root.sourceRootId,
        root.uniqueChemicalUid,true));
    var proposals=ResearchCandidateGenerator.generate(master,refs,summary,10);
    return NextResearchDecisionPipeline.evaluate(EvidenceConflictGraph.construct(summary),
       proposals,10);
  }
  private static void invalid(Runnable r){
    boolean failed=false;try{r.run();}catch(IllegalArgumentException x){failed=true;}
    require(failed,"input failed closed");
  }
  private static long heap(){
    Runtime r=Runtime.getRuntime();return r.totalMemory()-r.freeMemory();
  }
  private static String num(double x){return String.format(Locale.ROOT,"%.6f",x);}
  public static void main(String[] args)throws Exception{
    require(args.length==1,"output directory mandatory");
    loadBaseline();
    List<CoreContracts.Reading> base=new ArrayList<>();
    for(var e:AIR.entrySet()){
      String model=e.getKey();double rAir=e.getValue();
      CoreContracts.Reading r=new CoreContracts.Reading("Tank:physical:"+model,null,null,
        rAir,rAir,null,CoreContracts.CalibrationReference.AIR_RS_MEAN,
        "UNKNOWN_HEATER_PROFILE",CoreContracts.SampleQuality.VALID);
      base.add(r);
      require(Math.abs(r.airRatioOrNaN()-1)<1e-12,model+" baseline real ratio unity");
      require(Double.isNaN(r.sourceRatioOrNaN()),model+" no manufacturer R0 imported");
      // Synthetic 5 percent and 10 percent - no claim of a measured gas.
      for(double fraction:new double[]{.95,.9,1.01}){
        var c=R0ReferenceMath.compare(new R0ReferenceMath.Reference(
           "Tank:physical:"+model,model,"HEATER_PROFILE_UNVERIFIED",
           R0ReferenceMath.FIRMWARE_DEVICE_AIR,rAir),rAir*fraction,null);
        require(Math.abs(c.rsOverAir-fraction)<1e-12,"relative Rs arithmetic only "+model);
        require(!c.sourceCurveUseQualified,"never source-certified from R_air "+model);
      }
    }
    var capture=new CoreContracts.Snapshot("owner-clean-air-2026-10-31",
       "Tank-owner-unverified-hardware",CoreContracts.Mode.LAB,0,null,null,true,
       base,"stable-manifest-UNVERIFIED");
    require(capture.readings.size()==8,"8 physical readings in immutable snapshot");
    boolean immutable=false;try{capture.readings.clear();}
       catch(UnsupportedOperationException x){immutable=true;}
    require(immutable,"snapshot immutable");
    List<ResearchCandidateGenerator.Material> fullCatalog=catalog();
    var unknown=evaluate(fullCatalog);
    require(unknown.status==CoreContracts.DecisionStatus.UNKNOWN,
      "unexposed real clean-air snapshot must produce UNKNOWN not gases");
    require(unknown.eligibleCount==0,"no fabricated source supports");
    var falseNegative=evaluate(fullCatalog,
       node("non-independent-unknown-source",700,CoreContracts.EvidencePolarity.NOT_EVALUABLE));
    require(falseNegative.status==CoreContracts.DecisionStatus.UNKNOWN,
      "missing source sensitivity is not a qualified negative");
    var one=evaluate(fullCatalog,node("root-A",700,CoreContracts.EvidencePolarity.POSITIVE));
    require(one.eligibleCount==1&&one.status==CoreContracts.DecisionStatus.RESEARCH_CANDIDATES,
      "synthetic G1 source-only material UID 700 correctly routed");
    var conflict=evaluate(fullCatalog,node("root-A",700,CoreContracts.EvidencePolarity.POSITIVE),
        node("root-B",700,CoreContracts.EvidencePolarity.CONDITIONAL_NEGATIVE));
    require(conflict.status==CoreContracts.DecisionStatus.UNKNOWN,
      "independently attributed contradiction makes unknown");
    var mixture=evaluate(fullCatalog,node("root-A",700,CoreContracts.EvidencePolarity.POSITIVE),
        node("root-B",701,CoreContracts.EvidencePolarity.POSITIVE));
    require(mixture.status==CoreContracts.DecisionStatus.MIXTURE_UNRESOLVED
        &&!mixture.mixtureConfirmed,"two independent hypotheses cannot prove mixture");
    invalid(()->new CoreContracts.Reading("sensor",null,null,Double.NaN,10.,null,
        CoreContracts.CalibrationReference.AIR_RS_MEAN,"READY",CoreContracts.SampleQuality.VALID));
    invalid(()->new CoreContracts.Reading("sensor",null,null,10.,null,null,
        CoreContracts.CalibrationReference.AIR_RS_MEAN,"READY",CoreContracts.SampleQuality.VALID));
    invalid(()->node("",700,CoreContracts.EvidencePolarity.POSITIVE));
    var oneGraph=EvidenceConflictGraph.construct(IndependentEvidenceLedger.consolidate(
       "source-snapshot",List.of(node("root-A",700,CoreContracts.EvidencePolarity.POSITIVE))));
    var oneResult=NextResearchDecisionPipeline.result(capture,"sha256-owner-snapshot-replay",
        oneGraph,one);
    require(oneResult.rankedEligibleCandidates.size()==1
        &&oneResult.rankedEligibleCandidates.get(0).canonicalChemicalUid==700,
        "research result has canonical nonlegacy UID");
    require(!oneResult.physicallyValidatedG3&&!oneResult.reportablePpm,
        "no physical Tank gas identity or calibrated ppm");
    // Warm up; profile only the core graph/arbitration path, not model training or Android.
    for(int i=0;i<150;i++)evaluate(fullCatalog,node("root-"+i%4,700,
       CoreContracts.EvidencePolarity.POSITIVE));
    final int N=1500;
    long[] lat=new long[N];long heapBefore=heap();
    for(int i=0;i<N;i++){
      long start=System.nanoTime();
      var d=evaluate(fullCatalog,node("SOURCE-A",700,CoreContracts.EvidencePolarity.POSITIVE),
        node("SOURCE-B",700,CoreContracts.EvidencePolarity.POSITIVE));
      lat[i]=System.nanoTime()-start;
      if(i==0)require(d.eligibleCount==1&&d.eligibleRanked.get(0).independentPositiveRoots==2,
        "two independent roots not merged into one or padded");
    }
    long heapAfter=heap();
    Arrays.sort(lat);
    double median=lat[N/2]/1e6,p95=lat[(int)Math.ceil(.95*N)-1]/1e6;
    double p99=lat[(int)Math.ceil(.99*N)-1]/1e6;
    require(median>=0&&p95>=median&&p99>=p95,"monotone measured latency quantiles");
    Path dir=Path.of(args[0]);Files.createDirectories(dir);
    String payload="{\n  \"schema\":\"chemcore.next.p06_JVM_resource_profile/1\",\n"+
      "  \"scenario\":\"736_material_catalog_two_source_roots_research_only\",\n"+
      "  \"measurements\":"+N+",\"warmups\":150,\n"+
      "  \"jdk\":\""+System.getProperty("java.version")+"\",\n"+
      "  \"available_cores\":"+Runtime.getRuntime().availableProcessors()+",\n"+
      "  \"jvm_heap_limit_bytes\":"+Runtime.getRuntime().maxMemory()+",\n"+
      "  \"heap_used_before_bytes\":"+heapBefore+",\"heap_used_after_bytes\":"+heapAfter+",\n"+
      "  \"latency_median_ms\":"+num(median)+",\"latency_p95_ms\":"+num(p95)+
      ",\"latency_p99_ms\":"+num(p99)+",\n"+
      "  \"software_checks\":"+checks+",\n"+
      "  \"benchmarks_android_device\":false,\"tests_actual_gas_exposure\":false,\n"+
      "  \"tests_full_next_engine_inference\":true,\"ppm_authorized\":false,\"g3\":false\n"+
      "}\n";
    Files.writeString(dir.resolve("p06_resource_profile.json"),payload,StandardCharsets.UTF_8);
    System.out.println("CHEMCORE_NEXT_P06_1_4_INTEGRATED_G1_PASS checks="+checks+
       " samples="+N+" median_ms="+num(median)+" p95_ms="+num(p95)+" g3=false");
  }
}
