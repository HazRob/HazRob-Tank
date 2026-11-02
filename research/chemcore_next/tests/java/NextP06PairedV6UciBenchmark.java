package com.hazrob.tank.chemcore.next;
import com.hazrob.tank.chemcore.v6.ClassicalLearningV6;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * P06.2 exact paired-source V6 softmax vs Next fingerprint expert comparison.
 * Both use the SAME 90 original physical MQ135 UCI source exposures, SAME
 * 3 source temporal features, SAME withheld whole concentration level, no
 * training-time access to heldout samples. Scores are comparative research
 * source classification, NOT Tank molecular identification or ppm.
 */
public final class NextP06PairedV6UciBenchmark {
  private static final String[] CLASSES={"ethanol","acetone","toluene",
      "ethyl acetate","isopropanol","hexane"};
  private static int checks;
  private static void ok(boolean x,String why) {
    checks++;if(!x)throw new AssertionError("P06 paired "+checks+": "+why);
  }
  private static final class Exposure {
    final int row,ppb;final String gas;final double[] mq135;
    Exposure(int row,int ppb,String gas,double[] f){this.row=row;this.ppb=ppb;this.gas=gas;mq135=f;}
  }
  private static List<Exposure> originalFeatures(Path file)throws Exception{
    List<Exposure> all=new ArrayList<>();
    try(var br=Files.newBufferedReader(file,StandardCharsets.UTF_8)){
      ok(br.readLine().startsWith("row_id,gas,ppb,f00,"),"original UCI feature contract");
      String line;while((line=br.readLine())!=null){
        String[] r=line.split(",",-1);ok(r.length==33,"30 source features");
        int id=Integer.parseInt(r[0]),ppb=Integer.parseInt(r[2]);
        ok(id==all.size()&&Arrays.asList(CLASSES).contains(r[1]),"original row/gas");
        ok(ppb==50||ppb==100||ppb==200,"original ppb units");
        all.add(new Exposure(id,ppb,r[1],new double[]{
          Double.parseDouble(r[7]),Double.parseDouble(r[17]),Double.parseDouble(r[27])}));
      }
    }
    ok(all.size()==90,"original source 90 exposures");
    return all;
  }
  private static String nextPrediction(Map<String,FingerprintExpert.Signature> models,Exposure row){
    Map<String,Double> obs=new LinkedHashMap<>();
    obs.put("MQ135_ENDPOINT",row.mq135[0]);obs.put("MQ135_EARLY",row.mq135[1]);
    obs.put("MQ135_MIDDLE",row.mq135[2]);
    var reading=new FingerprintExpert.Observation("UCI1081_SOURCE_TEMPORAL",
        "MQ135","original-uci-row-"+row.row,obs,true);
    double bestDistance=Double.POSITIVE_INFINITY;String best=null;
    for(var e:models.entrySet()){
      var result=FingerprintExpert.compare(e.getValue(),reading,
          new FingerprintExpert.Qualification(true,true));
      ok(!result.gasIdentityConfirmed&&!result.ppmAuthorized&&!result.g3PhysicallyQualified,
          "Next source-trained fingerprint not field-verified");
      if(result.status==FingerprintExpert.Status.RESEARCH_RESIDUAL_ONLY
          &&result.researchNormalizedRmsDistance<bestDistance){
        best=e.getKey();bestDistance=result.researchNormalizedRmsDistance;
      }
    }
    return best;
  }
  private static Map<String,FingerprintExpert.Signature> nextTrain(List<Exposure> training,
                                                                     int withheld){
    Map<String,FingerprintExpert.Signature> sourceModels=new LinkedHashMap<>();
    for(String species:CLASSES){
      List<Exposure> samples=new ArrayList<>();
      for(Exposure row:training)if(row.gas.equals(species))samples.add(row);
      ok(samples.size()==10,"two source levels x five independent exposure records");
      double[] mean=new double[3],var=new double[3];
      for(var r:samples)for(int i=0;i<3;i++)mean[i]+=r.mq135[i];
      for(int i=0;i<3;i++)mean[i]/=samples.size();
      for(var r:samples)for(int i=0;i<3;i++){
        double diff=r.mq135[i]-mean[i];var[i]+=diff*diff;
      }
      List<Double> means=new ArrayList<>(),scales=new ArrayList<>();
      for(int i=0;i<3;i++){means.add(mean[i]);scales.add(Math.sqrt(var[i]/9.));}
      sourceModels.put(species,new FingerprintExpert.Signature(
          "source-fingerprint-"+species+"-"+withheld,"UCI1081_ORIGINAL_SOURCE",
          "UCI1081_SOURCE_TEMPORAL","MQ135",null,
          List.of("MQ135_ENDPOINT","MQ135_EARLY","MQ135_MIDDLE"),means,scales,3));
    }
    return sourceModels;
  }
  public static void main(String[] args)throws Exception{
    ok(args.length==2,"input original verified UCI features and output path");
    List<Exposure> all=originalFeatures(Path.of(args[0]));
    int total=0,v6Correct=0,nextCorrect=0,agree=0,disagree=0,bothCorrect=0;
    StringBuilder foldJson=new StringBuilder("[");
    for(int held:new int[]{50,100,200}){
      List<Exposure> train=new ArrayList<>(),test=new ArrayList<>();
      for(var r:all)(r.ppb==held?test:train).add(r);
      ok(train.size()==60&&test.size()==30,"whole concentration heldout");
      double[][] xs=new double[train.size()][];String[] labels=new String[train.size()];
      for(int i=0;i<train.size();i++){
        xs[i]=train.get(i).mq135.clone();labels[i]=train.get(i).gas;
      }
      // Original same-source V6 classifier. V6 parameters from its pinned
      // research source benchmark; no thresholds optimized on heldout records.
      var v6=ClassicalLearningV6.fitSoftmax(
        new ClassicalLearningV6.Dataset(xs,labels),800,.15,.005);
      var next=nextTrain(train,held);
      int v=0,n=0,match=0,both=0;
      for(var r:test){
        String v6Gas=v6.scores(r.mq135).get(0).label;
        String nextGas=nextPrediction(next,r);
        ok(nextGas!=null,"source-qualified expert had comparable 3-dimensional prototype");
        if(v6Gas.equals(r.gas)){v++;v6Correct++;}
        if(nextGas.equals(r.gas)){n++;nextCorrect++;}
        if(v6Gas.equals(nextGas)){match++;agree++;}else disagree++;
        if(v6Gas.equals(r.gas)&&nextGas.equals(r.gas)){both++;bothCorrect++;}
        total++;
      }
      if(foldJson.length()>1)foldJson.append(",");
      foldJson.append("{\"heldout_source_ppb\":").append(held)
          .append(",\"source_test_exposures\":30,\"v6_correct\":").append(v)
          .append(",\"next_correct\":").append(n)
          .append(",\"same_prediction\":").append(match)
          .append(",\"both_correct\":").append(both)
          .append(",\"v6_accuracy\":").append(v/30.0)
          .append(",\"next_accuracy\":").append(n/30.0).append("}");
      System.out.printf(Locale.ROOT,
        "NEXT_P06_PAIRED_UCI heldout_ppb=%d next=%d/30 v6=%d/30 same_prediction=%d/30%n",
        held,n,v,match);
    }
    foldJson.append("]");
    ok(total==90&&agree+disagree==90,"all original source test rows assessed exactly once");
    String json="{\n\"schema\":\"chemcore.next.p06_paired_v6_source_experiment/1\",\n"+
      "\"original_source\":\"UCI1081 ORIGINAL SHA256 verified by prior workflow\",\n"+
      "\"only_real_MQ135_features\":true,\"physical_source_exposures\":90,\n"+
      "\"source_concentrations_ppb\":[50,100,200],\"candidate_gases\":6,\n"+
      "\"same_samples_same_concentration_holdouts\":true,\n"+
      "\"v6_algorithm\":\"ClassicalLearningV6.fitSoftmax 800 .15 .005\",\n"+
      "\"next_algorithm\":\"FingerprintExpert source-standardized Euclidean minimum RMS\",\n"+
      "\"source_test_rows\":90,\"v6_correct\":"+v6Correct+
      ",\"next_correct\":"+nextCorrect+
      ",\"v6_accuracy\":"+v6Correct/90.0+
      ",\"next_accuracy\":"+nextCorrect/90.0+
      ",\"same_prediction_rows\":"+agree+",\"different_prediction_rows\":"+disagree+
      ",\"both_correct_rows\":"+bothCorrect+
      ",\"per_source_concentration_fold\":"+foldJson+
      ",\"expert_only_not_complete_engine\":true,\n"+
      "\"g3_tank\":false,\"ppm_authorized\":false,\"safety_classification_authorized\":false,\n"+
      "\"concentration_not_regressed\":true,\n"+
      "\"limitations\":[\"Only source MQ135 measurements from a separate hardware unit\","
      +"\"V6 classifier and Next fingerprint have different hypothesis classes, not whole system runtime\","
      +"\"No source device/day independent grouping exists for UCI1081\","
      +"\"Both methods always classify within six research reference labels; this is closed set, not unknown rejection\"]}\n";
    Path out=Path.of(args[1]);Files.createDirectories(out.getParent());
    Files.writeString(out,json,StandardCharsets.UTF_8);
    System.out.println("CHEMCORE_NEXT_P06_2_PAIRED_SOURCE_REPLAY_PASS"+
      " real_exposures=90 next_correct="+nextCorrect+" v6_correct="+v6Correct+
      " different="+disagree+" checks="+checks+" g3=false");
  }
}
