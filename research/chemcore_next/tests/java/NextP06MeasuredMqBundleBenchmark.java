package com.hazrob.tank.chemcore.next;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.util.*;

/**
 * Real recorded source ADC, not simulated resistance: evaluate different
 * MQ sensor subsets and a two-family bundle (static ADC + measured adjacent
 * ADC difference) from the ORIGINAL pinned 6400-row MQ source.
 *
 * Grouped by 100 contiguous source samples; 1 calibration block and 1 test
 * block held out per class, 14 source-training blocks.  One original category
 * recording per class -> source temporal blocks NOT independent exposures.
 * Per-family source-distance cutoffs from separate calibration blocks.
 *
 * This is a preliminary source-only component benchmark, NOT physical Tank
 * calibration, molecular gas recognition or chemical ppm.
 */
public final class NextP06MeasuredMqBundleBenchmark {
  private NextP06MeasuredMqBundleBenchmark(){}
  private static final String ROOT="MQ-EXT-03_ORIGINAL_SINGLE_SOURCE_RECORDING";
  private static final String[] CLASSES={"NoGas","Perfume","Smoke","Mixture"};
  private static final String[] CH={"MQ2","MQ3","MQ5","MQ6","MQ7","MQ8","MQ135"};
  private static final int[][] MODES={{1},{0,1},{0,1,2,4},{0,1,2,4,5,6},{0,1,2,3,4,5,6}};
  private static final String[] NAMES={"ONE_MQ3","TWO_MQ2_MQ3","FOUR_MQ",
      "SIX_TANK_MODEL_OVERLAP","SEVEN_SOURCE_MQ"};
  private static final String[] FAMILY={"RAW_ADC","ACTUAL_ADJACENT_ADC_DELTA"};
  private static int checks;
  private static void ok(boolean yes,String reason){
    checks++;if(!yes)throw new AssertionError("NEXT-P06-BUNDLE-"+checks+" "+reason);
  }
  private static final class Row {
    final int c,serial,block,sourceIndex;final int[] raw;final double[] delta;
    Row(int c,int serial,int sourceIndex,int[] raw,double[] delta){
      this.c=c;this.serial=serial;this.block=serial/100;this.sourceIndex=sourceIndex;
      this.raw=raw;this.delta=delta;
    }
    boolean hasPreviousWithinBlock(){return serial%100!=0;}
    double feature(int sensor,int fam){return fam==0?raw[sensor]:delta[sensor];}
  }
  private static List<Row> load(Path path)throws Exception {
    List<Row> rows=new ArrayList<>();int[] previousSerial=new int[4],seen=new int[4];
    int[][] prior=new int[4][7];
    try(BufferedReader br=Files.newBufferedReader(path,StandardCharsets.UTF_8)){
      String header=br.readLine();
      ok(header.equals("Serial Number,MQ2,MQ3,MQ5,MQ6,MQ7,MQ8,MQ135,Gas"),
         "original seven physical MQ source channel schema");
      String line;
      while((line=br.readLine())!=null){
        String[] s=line.split(",",-1);ok(s.length==9,"nine original values");
        int cls=Arrays.asList(CLASSES).indexOf(s[8]);
        ok(cls>=0,"original source class rather than invented chemical");
        int serial=Integer.parseInt(s[0]);ok(serial==seen[cls]++,"source serial order");
        int[] raw=new int[7];double[] diff=new double[7];
        for(int j=0;j<7;j++){
          raw[j]=Integer.parseInt(s[j+1]);ok(raw[j]>=0&&raw[j]<=1023,"real original 10-bit ADC");
          diff[j]=serial==0||serial%100==0?Double.NaN:raw[j]-prior[cls][j];
          prior[cls][j]=raw[j];
        }
        rows.add(new Row(cls,serial,rows.size(),raw,diff));
      }
    }
    ok(rows.size()==6400,"exact 6400 original MQ source readings");
    for(int i=0;i<4;i++)ok(seen[i]==1600,"one contiguous original recording of each class");
    return rows;
  }
  private static final class Template {
    final int cls,sensor,family;final double mean,sd,limit;
    Template(int cls,int sensor,int family,double mean,double sd,double limit){
      this.cls=cls;this.sensor=sensor;this.family=family;this.mean=mean;this.sd=sd;
      this.limit=limit;
    }
  }
  private static Template fit(List<Row> rows,int cls,int s,int fam,int held,int cal){
    List<Double> training=new ArrayList<>(),calibration=new ArrayList<>();
    for(var row:rows){
      if(row.c!=cls||!row.hasPreviousWithinBlock())continue;
      double v=row.feature(s,fam);
      if(row.block==held)continue;
      if(row.block==cal)calibration.add(v);
      else training.add(v);
    }
    ok(training.size()==1386&&calibration.size()==99,
        "14x99 rows training and disjoint 99 source calibration samples");
    double mean=0,variance=0;for(double v:training)mean+=v;
    mean/=training.size();
    for(double v:training)variance+=Math.pow(v-mean,2);
    double sd=Math.sqrt(variance/(training.size()-1));
    // Zero or nearly-zero variability is not an evidentiary feature;
    // never fabricate positive variance or sensor response.
    if(sd<=1e-12||!Double.isFinite(sd))return null;
    double[] calD=new double[calibration.size()];
    for(int i=0;i<calibration.size();i++)calD[i]=Math.abs(calibration.get(i)-mean)/sd;
    Arrays.sort(calD);
    double threshold=calD[(int)Math.ceil(.95*calD.length)-1];
    if(!(threshold>0)&&calD.length>0)threshold=calD[calD.length-1];
    if(!(threshold>0)||!Double.isFinite(threshold))return null;
    return new Template(cls,s,fam,mean,sd,threshold);
  }
  private static ResearchFingerprintBundle.Bundle build(List<Template> trained,
       Row row,int cls,int[] sensors) {
    List<ResearchFingerprintBundle.Variant> entries=new ArrayList<>();
    for(Template t:trained){
      if(t==null||t.cls!=cls)continue;
      String sensor=CH[t.sensor];
      String coordinate=t.family==0?"SOURCE_ADC_ORIGINAL":"SOURCE_ADC_ADJACENT_DIFF";
      var sig=new FingerprintExpert.Signature(
          "original-"+cls+"-"+sensor+"-"+FAMILY[t.family],ROOT,
          coordinate,sensor,Long.valueOf(700+cls),
          List.of("source_feature"),List.of(t.mean),List.of(t.sd),1);
      var obs=new FingerprintExpert.Observation(coordinate,sensor,
        "SOURCE_READING_"+row.sourceIndex,Map.of("source_feature",row.feature(t.sensor,t.family)),true);
      entries.add(new ResearchFingerprintBundle.Variant(sig,obs,
        new FingerprintExpert.Qualification(true,true),
        "SOURCE:"+sensor,FAMILY[t.family],sig.id,t.limit,true,null));
    }
    if(entries.isEmpty())return null;
    int minSensors=Math.max(1,(int)Math.ceil(sensors.length*.6));
    int requiredFamilies=entries.stream().map(x->x.featureFamily).distinct().count()>1?2:1;
    int minMatched=Math.max(minSensors,(int)Math.ceil(entries.size()*.55));
    return new ResearchFingerprintBundle.Bundle("source-category-"+cls+"-portfolio",
      ROOT,700+cls,entries,minMatched,minSensors,requiredFamilies);
  }
  private static String fmt(double x){return String.format(Locale.US,"%.8f",x);}
  private static final class Metrics {
    final String mode;int tested,accepted,correct,sourceNoGas,sourceSmoke,smokeFound,unknown;
    final int[] perClass={0,0,0,0},perClassRight={0,0,0,0};
    int nTrainTemplates,folds;
    Metrics(String name){mode=name;}
    String json(){
      StringBuilder b=new StringBuilder();
      b.append("{\"mode\":\"").append(mode)
       .append("\",\"source_test_rows\":").append(tested)
       .append(",\"source_class_accepted\":").append(accepted)
       .append(",\"source_class_correct\":").append(correct)
       .append(",\"unknown_or_ambiguous\":").append(unknown)
       .append(",\"accuracy_all_test_rows\":").append(fmt((double)correct/tested))
       .append(",\"coverage_fraction\":").append(fmt((double)accepted/tested))
       .append(",\"correct_accepted_fraction\":").append(fmt((double)correct/tested))
       .append(",\"smoke_true_label_rows\":").append(sourceSmoke)
       .append(",\"smoke_correct_accepted\":").append(smokeFound)
       .append(",\"smoke_source_recall\":").append(fmt((double)smokeFound/sourceSmoke))
       .append(",\"source_class_counts\":[");
      for(int i=0;i<4;i++){if(i>0)b.append(",");b.append(perClass[i]);}
      b.append("],\"correct_class_counts\":[");
      for(int i=0;i<4;i++){if(i>0)b.append(",");b.append(perClassRight[i]);}
      return b.append("]}").toString();
    }
  }
  private static Metrics testMode(List<Row> rows,int[] channels,String mode){
    Metrics result=new Metrics(mode);
    int planned=0;
    for(int hold=0;hold<16;hold++){
      int cal=(hold+15)%16;
      List<Template> templates=new ArrayList<>();
      for(int cls=0;cls<4;cls++)for(int s:channels)for(int family=0;family<2;family++){
        Template t=fit(rows,cls,s,family,hold,cal);
        if(t!=null)templates.add(t);
      }
      result.nTrainTemplates+=templates.size();
      for(Row row:rows){
        if(row.block!=hold || !row.hasPreviousWithinBlock())continue;
        result.tested++;result.perClass[row.c]++;
        if(row.c==2)result.sourceSmoke++;
        if(row.c==0)result.sourceNoGas++;
        Map<String,Integer> captured=new LinkedHashMap<>();
        for(int s:channels)captured.put("SOURCE:"+CH[s],row.raw[s]);
        var original=new ResearchFingerprintBundle.SourceAdcCapture(ROOT,
          "original-"+row.sourceIndex,captured,true);
        int winner=-1,maxMatched=-1;
        boolean tie=false;
        for(int cls=0;cls<4;cls++){
          var bundle=build(templates,row,cls,channels);
          if(bundle==null)continue;
          var score=ResearchFingerprintBundle.evaluateSourceOnly(bundle,original);
          ok(!score.gasIdentityConfirmed&&!score.ppmCertified&&!score.physicalTankG3
              &&!score.fieldSafetyAlarmAuthorized,
              "no source-category fingerprint bundle promotes field identity or ppm");
          if(!score.isEligibleResearchHypothesis)continue;
          if(score.matchedVariants>maxMatched){
            winner=cls;maxMatched=score.matchedVariants;tie=false;
          }else if(score.matchedVariants==maxMatched)tie=true;
        }
        if(winner<0||tie)result.unknown++;
        else{
          result.accepted++;
          if(winner==row.c){
            result.correct++;result.perClassRight[row.c]++;
            if(row.c==2)result.smokeFound++;
          }
        }
      }
      result.folds++;
    }
    ok(result.folds==16&&result.tested==6336,"16 heldout source blocks per class, no cross-block delta");
    ok(result.accepted+result.unknown==result.tested,"all original rows classified or abstained");
    return result;
  }
  public static void main(String[] args)throws Exception{
    ok(args.length==2,"original source ADC and output JSON path");
    List<Row> data=load(Path.of(args[0]));
    List<Metrics> all=new ArrayList<>();
    for(int j=0;j<MODES.length;j++){
      Metrics m=testMode(data,MODES[j],NAMES[j]);
      all.add(m);
      System.out.println("NEXT_REAL_SOURCE_MQ_BUNDLE mode="+m.mode+" rows="+m.tested+
       " accepted="+m.accepted+" correct="+m.correct+" unknown="+m.unknown+
       " smoke_recall="+fmt((double)m.smokeFound/m.sourceSmoke));
    }
    StringBuilder out=new StringBuilder();
    out.append("{\"schema\":\"chemcore.next.measured_adc_bundles_source_only/1\",");
    out.append("\"source_root\":\"").append(ROOT).append("\",");
    out.append("\"source_original_rows\":6400,\"source_test_rows_per_mode\":6336,");
    out.append("\"source_original_recordings\":4,\"original_ADC_not_kohm\":true,");
    out.append("\"source_calibration_block_separate_from_training_and_test\":true,");
    out.append("\"original_measured_adjacent_samples_no_time_interpolation\":true,");
    out.append("\"source_classification_NOT_gas_identity\":true,");
    out.append("\"source_class_smoke_NO_ppm\":true,");
    out.append("\"source_exposure_independence_proven\":false,");
    out.append("\"source_domain_only_no_tank_Rair_substitution\":true,");
    out.append("\"not_full_ChemCoreNext_engine_G2\":true,");
    out.append("\"per_class_source_roots_are_same_physical_study\":true,");
    out.append("\"bundles\":[");
    for(int i=0;i<all.size();i++){
      if(i>0)out.append(",");
      out.append(all.get(i).json());
    }
    out.append("],\"limitations\":[");
    out.append("\"Original source categories only, one contiguous recording per class; block samples correlated\",");
    out.append("\"Positive-source-only calibration quantiles cannot establish unknown-gas rejection\",");
    out.append("\"Individual source feature families share one physical measurement and are not independent votes\",");
    out.append("\"No source-certified pure gas identity nor independent gas ppm labels\",");
    out.append("\"No Tank Rs, R_air or calibrated MQ source cross-hardware transform\",");
    out.append("\"Observed source-record adjacent time samples have order but no qualified absolute timestamps\"]}");
    Path target=Path.of(args[1]);Files.createDirectories(target.getParent());
    Files.writeString(target,out.toString()+"\n",StandardCharsets.UTF_8);
    System.out.println("CHEMCORE_NEXT_REAL_ADC_BUNDLE_STRESS_PASS source_rows=6400"+
      " tested_each_mode=6336 modes=5 checks="+checks+" G3=0");
  }
}
