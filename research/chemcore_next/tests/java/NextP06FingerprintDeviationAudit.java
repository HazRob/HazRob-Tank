package com.hazrob.tank.chemcore.next;
import com.hazrob.tank.chemcore.v6.ClassicalLearningV6;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Controlled stage-level diagnostic on ORIGINAL MQ135 physical UCI exposures.
 * All arms consume identical verified 90 x 3 measured-source window features,
 * identical whole concentration holdouts, identical training rows. Vary only
 * distance scale (class specific vs globally trained) and temporal feature
 * count (endpoint only vs 3). No heldout concentration leakage.
 * Absolute source signal rows are never treated as Tank R_air or SOURCE_R0.
 */
public final class NextP06FingerprintDeviationAudit {
 private NextP06FingerprintDeviationAudit(){}
 private static final String[] GASES={"ethanol","acetone","toluene","ethyl acetate","isopropanol","hexane"};
 private static int checks;
 private static void ok(boolean yes,String why){checks++;if(!yes)throw new AssertionError("P06-AUDIT "+checks+" "+why);}
 private static final class Row {
   int id,level,gas;double[] x;
   Row(int id,int level,int gas,double[] x){this.id=id;this.level=level;this.gas=gas;this.x=x;}
 }
 private static List<Row> load(Path path)throws Exception{
   List<Row> all=new ArrayList<>();
   try(var reader=Files.newBufferedReader(path,StandardCharsets.UTF_8)){
     String head=reader.readLine();
     ok(head!=null&&head.startsWith("row_id,gas,ppb,f00,"),"source features from verified original UCI protocol");
     String s;
     while((s=reader.readLine())!=null){
       String[] v=s.split(",",-1);
       ok(v.length==33,"30 actual source time-domain window features");
       int gas=Arrays.asList(GASES).indexOf(v[1]);
       int level=Integer.parseInt(v[2]);
       ok(gas>=0&&Arrays.asList(50,100,200).contains(level),"original gas and ppb units");
       int id=Integer.parseInt(v[0]);ok(id==all.size(),"row order anchored to original");
       double[] x={Double.parseDouble(v[7]),Double.parseDouble(v[17]),Double.parseDouble(v[27])};
       for(double y:x)ok(Double.isFinite(y)&&y>-1,"source ratio baseline valid");
       all.add(new Row(id,level,gas,x));
     }
   }
   ok(all.size()==90,"90 original physical exposures");
   return all;
 }
 private enum Arm { NEXT_CLASS_SD_3WINDOW, NEXT_POOLED_SD_3WINDOW,
   NEXT_CLASS_SD_ENDPOINT, NEXT_POOLED_SD_ENDPOINT }
 private static final class Prototype {
   final String gas;final FingerprintExpert.Signature signature;
   Prototype(String gas,FingerprintExpert.Signature f){this.gas=gas;signature=f;}
 }
 private static List<Prototype> train(List<Row> train,int withheld,Arm arm) {
    int d=arm.name().endsWith("ENDPOINT")?1:3;
    double[] pooled=new double[d],globalSd=new double[d];
    for(Row r:train)for(int k=0;k<d;k++)pooled[k]+=r.x[k];
    for(int k=0;k<d;k++)pooled[k]/=train.size();
    for(Row r:train)for(int k=0;k<d;k++){
      double e=r.x[k]-pooled[k];globalSd[k]+=e*e;
    }
    for(int k=0;k<d;k++)globalSd[k]=Math.sqrt(globalSd[k]/(train.size()-1));
    List<Prototype> models=new ArrayList<>();
    for(int species=0;species<GASES.length;species++){
      int n=0;double[] mean=new double[d],var=new double[d];
      for(Row r:train)if(r.gas==species){
        n++;for(int k=0;k<d;k++)mean[k]+=r.x[k];
      }
      ok(n==10,"two source concentration levels x five original replicates per gas");
      for(int k=0;k<d;k++)mean[k]/=n;
      for(Row r:train)if(r.gas==species)for(int k=0;k<d;k++){
        double e=r.x[k]-mean[k];var[k]+=e*e;
      }
      List<String> names=new ArrayList<>();
      List<Double> means=new ArrayList<>(),sd=new ArrayList<>();
      for(int k=0;k<d;k++){
        double denom=arm.name().contains("POOLED")?globalSd[k]:Math.sqrt(var[k]/(n-1.));
        ok(Double.isFinite(denom)&&denom>0,"physical source training variation required");
        names.add("UCI_MQ135_SOURCE_WINDOW_"+k);
        means.add(mean[k]);sd.add(denom);
      }
      var sign=new FingerprintExpert.Signature(
        "experiment-"+arm+"-"+GASES[species]+"-"+withheld,
        "ORIGINAL_UCI_1081_STUDY","UCI_3WINDOW_SOURCE", "MQ135",null,
        names,means,sd,d);
      models.add(new Prototype(GASES[species],sign));
    }
    return models;
 }
 private static int infer(List<Prototype> models,Row row) {
    Map<String,Double> vector=new LinkedHashMap<>();
    for(int k=0;k<models.get(0).signature.dimension();k++)
      vector.put("UCI_MQ135_SOURCE_WINDOW_"+k,row.x[k]);
    var obs=new FingerprintExpert.Observation("UCI_3WINDOW_SOURCE","MQ135",
       "source-exposure-"+row.id,vector,true);
    double best=Double.POSITIVE_INFINITY;int identity=-1;
    for(int c=0;c<models.size();c++){
      var res=FingerprintExpert.compare(models.get(c).signature,obs,
        new FingerprintExpert.Qualification(true,true));
      ok(res.status==FingerprintExpert.Status.RESEARCH_RESIDUAL_ONLY,
         "source-qualified research metric evaluated");
      if(res.researchNormalizedRmsDistance<best) {
        identity=c;best=res.researchNormalizedRmsDistance;
      }
    }
    return identity;
 }
 public static void main(String[] args)throws Exception{
    ok(args.length==2,"verified UCI features and output JSON");
    List<Row> data=load(Path.of(args[0]));
    String[] names={"V6_SOFTMAX",Arm.NEXT_CLASS_SD_3WINDOW.name(),
       Arm.NEXT_POOLED_SD_3WINDOW.name(),Arm.NEXT_CLASS_SD_ENDPOINT.name(),
       Arm.NEXT_POOLED_SD_ENDPOINT.name()};
    int[][] correct=new int[names.length][3],perGas=new int[names.length][6];
    int[][] errorsByGas=new int[names.length][6];
    int[] v6NextDisagree=new int[3],nextScaleDifferent=new int[3];
    for(int heldIdx=0;heldIdx<3;heldIdx++){
      int held=new int[]{50,100,200}[heldIdx];
      List<Row> train=new ArrayList<>(),test=new ArrayList<>();
      for(Row r:data)(r.level==held?test:train).add(r);
      ok(train.size()==60&&test.size()==30,"identical concentration-level holdout arms");
      double[][] X=new double[60][3];String[] y=new String[60];
      for(int i=0;i<60;i++){X[i]=train.get(i).x.clone();y[i]=GASES[train.get(i).gas];}
      var v6=ClassicalLearningV6.fitSoftmax(
          new ClassicalLearningV6.Dataset(X,y),800,.15,.005);
      List<List<Prototype>> models=new ArrayList<>();
      for(Arm a:Arm.values())models.add(train(train,held,a));
      for(Row row:test){
         int[] pred=new int[names.length];
         pred[0]=Arrays.asList(GASES).indexOf(v6.scores(row.x).get(0).label);
         for(int i=1;i<names.length;i++)pred[i]=infer(models.get(i-1),row);
         for(int i=0;i<names.length;i++){
            if(pred[i]==row.gas){correct[i][heldIdx]++;perGas[i][row.gas]++;}
            else errorsByGas[i][row.gas]++;
         }
         if(pred[0]!=pred[1])v6NextDisagree[heldIdx]++;
         if(pred[1]!=pred[2])nextScaleDifferent[heldIdx]++;
      }
    }
    StringBuilder out=new StringBuilder();
    out.append("{\"schema\":\"chemcore.next.P06_stage_diagnostic_UCI_MQ135/1\",");
    out.append("\"original_exposures\":90,\"sensor_model\":\"MQ135\",");
    out.append("\"same_source_early_middle_endpoint_features\":true,");
    out.append("\"train_holdout_source_groups_disjoint\":true,");
    out.append("\"whole_concentration_levels_ppb\":[50,100,200],");
    out.append("\"ablation_only_changes_training_scale_and_feature_count\":true,");
    out.append("\"research_classifiers\":[");
    for(int i=0;i<names.length;i++) {
      if(i>0)out.append(",");
      int total=0;for(int v:correct[i])total+=v;
      out.append("{\"model\":\"").append(names[i]).append("\",\"correct_total\":")
         .append(total).append(",\"accuracy\":")
         .append(String.format(Locale.US,"%.8f",total/90.)).append(",\"correct_by_concentration\":[");
      for(int j=0;j<3;j++){if(j>0)out.append(",");out.append(correct[i][j]);}
      out.append("],\"correct_by_gas\":[");
      for(int j=0;j<6;j++){if(j>0)out.append(",");out.append(perGas[i][j]);}
      out.append("],\"errors_by_gas\":[");
      for(int j=0;j<6;j++){if(j>0)out.append(",");out.append(errorsByGas[i][j]);}
      out.append("]}");
      System.out.println("NEXT_P06_DEVIATION_ABLATION model="+names[i]+
        " correct="+total+"/90 accuracy="+String.format(Locale.US,"%.5f",total/90.));
    }
    out.append("],\"gases\":[");
    for(int j=0;j<6;j++){if(j>0)out.append(",");out.append('"').append(GASES[j]).append('"');}
    out.append("],\"v6_vs_class_specific_next_disagreements_by_ppb\":[");
    for(int j=0;j<3;j++){if(j>0)out.append(",");out.append(v6NextDisagree[j]);}
    out.append("],\"next_class_specific_vs_pooled_scale_disagreements_by_ppb\":[");
    for(int j=0;j<3;j++){if(j>0)out.append(",");out.append(nextScaleDifferent[j]);}
    out.append("],\"mechanism\":\"shared input signal and windows, divergence occurs after training at prototype class-dependent variance/metric versus V6 global scaling and optimized softmax\",");
    out.append("\"cannot_prove_causality_without_ablation\":true,");
    out.append("\"physical_tank_Rair_injected\":false,\"gas_identity_certified\":false,");
    out.append("\"ppm_estimation_performed\":false,\"G3_tank\":false,");
    out.append("\"further_required\":\"independent multi-device calibration plus real Tank known-gas exposures and UNKNOWN gate evaluation\"}");
    Path target=Path.of(args[1]);Files.createDirectories(target.getParent());
    Files.writeString(target,out.toString()+"\n",StandardCharsets.UTF_8);
    System.out.println("CHEMCORE_NEXT_P06_DEVIATION_DIAG_PASS checks="+checks+
       " heldout_exposures=90 arms="+names.length+" source_gases=6 G3=0");
 }
}
