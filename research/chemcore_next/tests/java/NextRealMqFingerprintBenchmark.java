package com.hazrob.tank.chemcore.next;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.*;

/**
 * Test the ACTUAL Next FingerprintExpert on original physical MQ records.
 * 1) MQ135: six chemically labelled gas vapours, 3 distinct concentration
 *    levels (50/100/200 ppb), five original exposures per combination;
 *    hold out an entire concentration level in each fold.
 * 2) MQ2/MQ3/MQ5/MQ6/MQ7/MQ8/MQ135: 6400 raw 10-bit ADC readings,
 *    four category recordings including Smoke. Test source-only 1/2/4/6/7
 *    channel subsets with contiguous-block holdouts.
 * 3) MQ4: three actually measured instances, 11 source days, independently
 *    Li-Cor-labelled methane ppm. Test 1/3 MQ4 and 3 MQ4 + T/RH under
 *    leave-entire-day-out, in 4 predeclared reference concentration bins.
 *
 * The owner Tank's 8 real R_air kOhm values are present as separate physical
 * source provenance; NEVER cross-calibrate external devices using Tank R_air.
 * This benchmark is not independent physical Tank G3 and cannot authorize ppm,
 * gas identity, safety alarms or posterior probability.
 */
public final class NextRealMqFingerprintBenchmark {
  private NextRealMqFingerprintBenchmark(){}
  private static final String[] UCI_GASES={"ethanol","acetone","toluene",
    "ethyl acetate","isopropanol","hexane"};
  private static final String[] ADC_CLASSES={"NoGas","Perfume","Smoke","Mixture"};
  private static final String[] METHANE_BINS={"CH4_lt4","CH4_4to6","CH4_6to8","CH4_ge8"};
  private static final String[] SOURCE_MQ7={"MQ2","MQ3","MQ5","MQ6","MQ7","MQ8","MQ135"};
  private static int validations;
  private static void check(boolean v,String reason) {
    validations++;if(!v)throw new IllegalStateException("NEXT_REAL_MQ_"+validations+": "+reason);
  }
  private static double number(String s) {
    double x=Double.parseDouble(s);if(!Double.isFinite(x))throw new IllegalArgumentException("nonfinite source "+s);
    return x;
  }
  private static int col(String[] header,String name) {
    for(int i=0;i<header.length;i++)if(header[i].equals(name))return i;
    throw new IllegalArgumentException("missing ORIGINAL source column "+name);
  }
  private static final class Row {
    final String category,group;final int concentration;final double[] v;
    Row(String category,String group,int concentration,double... features) {
      this.category=category;this.group=group;this.concentration=concentration;this.v=features;
    }
  }
  private static final class Stats {
    final String dataset,mode,unit,holdoutKind;
    final String[] classes;final int[][] cm;
    int total,correct,abstain,folds;
    Stats(String dataset,String mode,String units,String holdout,String[] labels) {
      this.dataset=dataset;this.mode=mode;unit=units;holdoutKind=holdout;
      classes=labels;cm=new int[labels.length][labels.length];
    }
    void record(String actual,String predicted) {
      total++;int t=Arrays.asList(classes).indexOf(actual);
      check(t>=0,"real source original category present");
      if(predicted==null){abstain++;return;}
      int p=Arrays.asList(classes).indexOf(predicted);
      check(p>=0,"prediction is source class not fabricated category");
      cm[t][p]++;if(t==p)correct++;
    }
    double accuracy(){return (double)correct/total;}
    double retainedAccuracy(){return total==abstain?Double.NaN:(double)correct/(total-abstain);}
    double balancedRecall() {
      double sum=0;for(int i=0;i<classes.length;i++){
        int observed=0;for(int x:cm[i])observed+=x;
        sum+=observed==0?0:(double)cm[i][i]/observed;
      }return sum/classes.length;
    }
    double recall(String name) {
      int i=Arrays.asList(classes).indexOf(name),n=0;
      for(int x:cm[i])n+=x;
      return n==0?Double.NaN:(double)cm[i][i]/n;
    }
    String json() {
      StringBuilder b=new StringBuilder();
      b.append("{\"dataset\":\"").append(dataset).append("\",\"mode\":\"")
       .append(mode).append("\",\"measurement_unit\":\"").append(unit)
       .append("\",\"holdout\":\"").append(holdoutKind)
       .append("\",\"folds\":").append(folds).append(",\"evaluated_source_rows\":").append(total)
       .append(",\"correct_rows\":").append(correct).append(",\"abstained_rows\":").append(abstain)
       .append(",\"accuracy_all_rows\":").append(fmt(accuracy()))
       .append(",\"accuracy_retained_rows\":").append(fmt(retainedAccuracy()))
       .append(",\"balanced_recall_all_source_groups\":").append(fmt(balancedRecall()))
       .append(",\"per_class_recall\":{");
      for(int j=0;j<classes.length;j++){
        if(j>0)b.append(',');
        b.append('"').append(classes[j]).append("\":").append(fmt(recall(classes[j])));
      }
      return b.append("},\"identity_verified_on_tank\":false,\"ppm_certified\":false}").toString();
    }
  }
  private static String fmt(double v) {
    return Double.isFinite(v)?String.format(Locale.US,"%.8f",v):"null";
  }
  private static final class Model {
    final Map<String,FingerprintExpert.Signature> signatures=new LinkedHashMap<>();
    final String root,space,sensor;final List<String> keys;
    Model(String sourceRoot,String sourceSpace,String sensorModel,List<String> coords) {
      root=sourceRoot;space=sourceSpace;sensor=sensorModel;keys=coords;
    }
    void train(List<Row> all,String[] labels,String withheld,String calib) {
      for(String label:labels) {
        int count=0;double[] mean=new double[keys.size()],variance=new double[keys.size()];
        for(Row r:all) {
          if(!r.category.equals(label)||r.group.equals(withheld)||r.group.equals(calib))continue;
          for(int k=0;k<mean.length;k++)mean[k]+=r.v[k];
          count++;
        }
        if(count<8)continue;
        for(int k=0;k<mean.length;k++)mean[k]/=count;
        for(Row r:all) {
          if(!r.category.equals(label)||r.group.equals(withheld)||r.group.equals(calib))continue;
          for(int k=0;k<variance.length;k++){
            double delta=r.v[k]-mean[k];variance[k]+=delta*delta;
          }
        }
        List<Double> means=new ArrayList<>(),scales=new ArrayList<>();
        for(int k=0;k<mean.length;k++){
          double sd=Math.sqrt(variance[k]/(count-1));
          means.add(mean[k]);scales.add(sd);
        }
        signatures.put(label,new FingerprintExpert.Signature(
          root+"|"+label+"|"+withheld,root,space,sensor,null,
          keys,means,scales,keys.size()));
      }
    }
    String predict(Row r) {
      Map<String,Double> coords=new LinkedHashMap<>();
      for(int k=0;k<keys.size();k++)coords.put(keys.get(k),r.v[k]);
      var obs=new FingerprintExpert.Observation(space,sensor,
        "original-source-"+root+"-"+r.group,coords,true);
      String best=null;double distance=Double.POSITIVE_INFINITY;
      for(var entry:signatures.entrySet()) {
        var output=FingerprintExpert.compare(entry.getValue(),obs,
          new FingerprintExpert.Qualification(true,true));
        check(!output.gasIdentityConfirmed&&!output.ppmAuthorized&&!output.g3PhysicallyQualified,
          "NEXT actual expert does not certify ppm or gas identity");
        if(output.status!=FingerprintExpert.Status.RESEARCH_RESIDUAL_ONLY)continue;
        if(output.researchNormalizedRmsDistance<distance){
          distance=output.researchNormalizedRmsDistance;best=entry.getKey();
        }
      }
      return best;
    }
    void denyCrossDevice(Row row){
      if(signatures.isEmpty())return;
      Map<String,Double> x=new LinkedHashMap<>();
      for(int k=0;k<keys.size();k++)x.put(keys.get(k),row.v[k]);
      var obs=new FingerprintExpert.Observation(space,sensor,"UNQUALIFIED_OWNER_TANK",x,true);
      var output=FingerprintExpert.compare(signatures.values().iterator().next(),obs,
        FingerprintExpert.Qualification.unverified());
      check(output.status==FingerprintExpert.Status.SOURCE_TRANSFORM_UNQUALIFIED,
        "external source shape never transfers to Tank without physical mapping");
    }
  }
  private static List<Row> sourceMq6400(Path file)throws Exception {
    List<Row> all=new ArrayList<>(),rows=all;
    int[] counts={0,0,0,0};
    try(BufferedReader br=Files.newBufferedReader(file,StandardCharsets.UTF_8)){
      String[] h=br.readLine().split(",",-1);
      check(h.length==9&&h[1].equals("MQ2")&&h[8].equals("Gas"),
        "seven physical MQ channel ADC source");
      String line;while((line=br.readLine())!=null) {
        String[] r=line.split(",",-1);check(r.length==9,"nine ADC source fields");
        int category=Arrays.asList(ADC_CLASSES).indexOf(r[8]);
        check(category>=0,"known SOURCE recording label");
        int serial=Integer.parseInt(r[0]);
        check(serial==counts[category],"within-source index starts 0 each class");
        counts[category]++;
        double[] values=new double[7];
        for(int k=0;k<7;k++){
          values[k]=number(r[k+1]);
          check(values[k]>=0&&values[k]<=1023,"original 10bit MQ ADC");
        }
        rows.add(new Row(r[8],"block"+(serial/100),-1,values));
      }
    }
    check(rows.size()==6400,"6400 actual MQ ADC rows");
    for(int x:counts)check(x==1600,"one original contiguous 1600-row class recording");
    return rows;
  }
  private static Stats adcMode(List<Row> all,int[] indices,String mode) {
    List<Row> transformed=new ArrayList<>(all.size());
    for(Row r:all){
      double[] v=new double[indices.length];
      for(int k=0;k<v.length;k++)v[k]=r.v[indices[k]];
      transformed.add(new Row(r.category,r.group,-1,v));
    }
    List<String> keys=new ArrayList<>();
    for(int i:indices)keys.add(SOURCE_MQ7[i]+"_SOURCE_ADC_10BIT");
    Stats stats=new Stats("MQ_EXT_03_6400_REAL_MQ_ADC",mode,
      "raw_10bit_ADC_no_ppm","contiguous_100row_block_not_independent_exposure",ADC_CLASSES);
    for(int fold=0;fold<16;fold++){
      String held="block"+fold,cal="block"+((fold+15)%16);
      Model model=new Model("MQ_EXT_03_SINGLE_SOURCE_RECORDING",
        "SOURCE_10BIT_ADC_NOT_TANK_ADS",mode,keys);
      model.train(transformed,ADC_CLASSES,held,cal);
      model.denyCrossDevice(transformed.get(0));
      for(Row r:transformed)if(r.group.equals(held))stats.record(r.category,model.predict(r));
      stats.folds++;
    }
    check(stats.total==6400,"all MQ ADC source rows tested once per channel subset");
    return stats;
  }
  private static List<Row> sourceUci(Path path)throws Exception{
    List<Row> result=new ArrayList<>();
    try(BufferedReader br=Files.newBufferedReader(path,StandardCharsets.UTF_8)){
      String h=br.readLine();
      check(h.startsWith("row_id,gas,ppb,f00,"),"verified original UCI source features");
      String line;while((line=br.readLine())!=null){
        String[] r=line.split(",",-1);
        check(r.length==33&&Integer.parseInt(r[0])==result.size(),"one row one exposure");
        int ppb=Integer.parseInt(r[2]);
        check(Arrays.asList(UCI_GASES).contains(r[1])&&(ppb==50||ppb==100||ppb==200),
          "six original gas identities x three concentrations ppb");
        result.add(new Row(r[1],"ppb"+ppb,ppb,number(r[7]),number(r[17]),number(r[27])));
      }
    }
    check(result.size()==90,"original 90 real MQ135-containing source exposures");
    for(String gas:UCI_GASES)for(int ppb:new int[]{50,100,200}){
      int n=0;for(Row r:result)if(r.category.equals(gas)&&r.concentration==ppb)n++;
      check(n==5,"five original physical exposures per gas & reference concentration");
    }
    return result;
  }
  private static Stats uciMode(List<Row> data,int[] cols,String mode){
    List<Row> rows=new ArrayList<>(data.size());
    for(Row r:data){
      double[] x=new double[cols.length];
      for(int k=0;k<cols.length;k++)x[k]=r.v[cols[k]];
      rows.add(new Row(r.category,r.group,r.concentration,x));
    }
    List<String> keys=new ArrayList<>();
    for(int c:cols)keys.add("MQ135_"+(c==0?"ENDPOINT":c==1?"EARLY":"MIDDLE")+"_BASELINE_RATIO_MINUS1");
    Stats stats=new Stats("UCI_1081_90_MQ135_REAL_GAS_PPB",mode,
      "ppb_true_source_concentration","leave_whole_concentration_level_out",UCI_GASES);
    for(int ppm:new int[]{50,100,200}){
      String held="ppb"+ppm;
      Model model=new Model("UCI1081_SINGLE_ORIGINAL_SOURCE",
        "MQ135_900POINT_SOURCE_NORMALIZED_WINDOWS","MQ135",keys);
      model.train(rows,UCI_GASES,held,"NO_CALIBRATION_GROUP");
      model.denyCrossDevice(rows.get(0));
      for(Row r:rows)if(r.group.equals(held))stats.record(r.category,model.predict(r));
      stats.folds++;
    }
    check(stats.total==90,"90 real original UCI MQ135 test exposures");
    return stats;
  }
  private static final class MethaneSource {
    final List<Row> all;
    final double min,max;
    final int[] referenceCounts;
    MethaneSource(List<Row> r,double a,double b,int[] counts){
      all=r;min=a;max=b;referenceCounts=counts;
    }
  }
  private static String methaneBin(double ppm) {
    return ppm<4?"CH4_lt4":ppm<6?"CH4_4to6":ppm<8?"CH4_6to8":"CH4_ge8";
  }
  private static MethaneSource sourceMethane(Path path)throws Exception {
    List<Row> out=new ArrayList<>();Set<String> days=new LinkedHashSet<>();
    double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;
    int[] bins=new int[4];
    try(BufferedReader reader=Files.newBufferedReader(path,StandardCharsets.UTF_8)){
      String[] h=reader.readLine().split(",",-1);
      int ti=col(h,"datetime"),ci=col(h,"licor_ch4"),rhi=col(h,"rh"),temp=col(h,"t");
      int[] si={col(h,"MQ4_1"),col(h,"MQ4_2"),col(h,"MQ4_3")};
      String line;while((line=reader.readLine())!=null){
        String[] r=line.split(",",-1);
        check(r.length==h.length,"original Minnesota source columns");
        String day=r[ti].substring(0,10);days.add(day);
        double ppm=number(r[ci])/1000.0,t=number(r[temp]),rh=number(r[rhi]);
        double[] x=new double[5];
        for(int k=0;k<3;k++) {
          double rsOhm=number(r[si[k]]);
          check(rsOhm>0,"actual MQ4 source resistance in Ohm");
          x[k]=Math.log(rsOhm);
        }
        x[3]=t;x[4]=rh;
        String gas=methaneBin(ppm);
        bins[Arrays.asList(METHANE_BINS).indexOf(gas)]++;
        min=Math.min(min,ppm);max=Math.max(max,ppm);
        out.add(new Row(gas,day,-1,x));
      }
    }
    check(out.size()==14186,"14186 original physically measured source rows");
    check(days.size()==11,"11 source days preserved");
    return new MethaneSource(out,min,max,bins);
  }
  private static Stats methaneMode(MethaneSource source,int[] channels,String mode){
    List<Row> rows=new ArrayList<>();
    for(Row r:source.all) {
      double[] v=new double[channels.length];
      for(int k=0;k<channels.length;k++)v[k]=r.v[channels[k]];
      rows.add(new Row(r.category,r.group,-1,v));
    }
    List<String> features=new ArrayList<>();String[] allNames={
      "MQ4_1_SOURCE_LOG_OHM","MQ4_2_SOURCE_LOG_OHM",
      "MQ4_3_SOURCE_LOG_OHM","SOURCE_T_C","SOURCE_RH_PCT"};
    for(int i:channels)features.add(allNames[i]);
    Stats result=new Stats("MINNESOTA_MQ4_LICOR_14186_REAL_SOURCE",mode,
      "reference_CH4_ppm_bins_not_Tank_ppm","leave_entire_source_day_out",METHANE_BINS);
    Set<String> groups=new LinkedHashSet<>();
    for(Row r:rows)groups.add(r.group);
    for(String withheld:groups){
      Model model=new Model("MQ_EXT_21_FURUTA_LICOR_ORIGINAL_SOURCE",
        "MINNESOTA_MQ4_SOURCE_LOGOHM_PLUS_OPTIONAL_ENV",mode,features);
      model.train(rows,METHANE_BINS,withheld,"NO_CALIBRATION_GROUP");
      model.denyCrossDevice(rows.get(0));
      for(Row row:rows)if(row.group.equals(withheld))result.record(row.category,model.predict(row));
      result.folds++;
    }
    check(result.total==14186,"all real Li-Cor reference rows scored once per model");
    return result;
  }
  private static void ownerGuard(Path baseline)throws Exception{
    String text=Files.readString(baseline,StandardCharsets.UTF_8);
    check(text.contains("DEVICE_CLEAN_AIR_RS_MEAN"),"Tank clean-air Rs definition preserved");
    for(String sensor:List.of("MQ2","MQ3","MQ4","MQ5","MQ7","MQ8","MQ9","MQ135")){
      check(text.contains("\""+sensor+"\": "),"each owner physical baseline preserved");
    }
    check(text.contains("\"physical_tank_g3_qualified\": false"),"physical G3 still 0");
  }
  public static void main(String[] args)throws Exception{
    check(args.length==5,"args UCI_features MQ6400_CSV original_MQ4_CSV Tank_owner_Rair_JSON output_dir");
    Path out=Path.of(args[4]);Files.createDirectories(out);
    ownerGuard(Path.of(args[3]));
    List<Row> uci=sourceUci(Path.of(args[0]));
    List<Row> adc=sourceMq6400(Path.of(args[1]));
    MethaneSource methane=sourceMethane(Path.of(args[2]));
    List<Stats> results=new ArrayList<>();
    results.add(uciMode(uci,new int[]{0},"MQ135_ENDPOINT_ONE_PHYSICAL_SENSOR"));
    results.add(uciMode(uci,new int[]{0,1,2},"MQ135_3_TEMPORAL_FEATURES_ONE_PHYSICAL_SENSOR"));
    results.add(adcMode(adc,new int[]{0},"MQ2_ONLY_ONE_SENSOR"));
    results.add(adcMode(adc,new int[]{1},"MQ3_ONLY_ONE_SENSOR"));
    results.add(adcMode(adc,new int[]{5},"MQ8_ONLY_ONE_SENSOR"));
    results.add(adcMode(adc,new int[]{0,1},"MQ2_MQ3_TWO_SENSORS"));
    results.add(adcMode(adc,new int[]{0,1,2,4},"MQ2_MQ3_MQ5_MQ7_FOUR"));
    results.add(adcMode(adc,new int[]{0,1,2,4,5,6},"SIX_TANK_OVERLAP_MQ"));
    results.add(adcMode(adc,new int[]{0,1,2,3,4,5,6},"SEVEN_SOURCE_MQ_INCLUDES_MQ6"));
    results.add(methaneMode(methane,new int[]{0},"SOURCE_MQ4_UNIT1_ONE_SENSOR"));
    results.add(methaneMode(methane,new int[]{0,1,2},"SOURCE_MQ4_THREE_PHYSICAL_SENSORS"));
    results.add(methaneMode(methane,new int[]{0,1,2,3,4},"SOURCE_MQ4_THREE_PLUS_T_RH"));
    StringBuilder report=new StringBuilder();
    report.append("{\n \"schema\":\"chemcore.next.original_measured_MQ_experts_experiment/1\",");
    report.append("\n \"core_expert\":\"com.hazrob.tank.chemcore.next.FingerprintExpert.compare\",");
    report.append("\n \"source_MQ_only\":true,\"all_test_rows_are_original_physical_source_measurements\":true,");
    report.append("\n \"training_test_source_record_separation\":true,\"owner_real_Rair_used_only_for_device_guard\":true,");
    report.append("\n \"cross_source_device_baselines_never_mixed\":true,");
    report.append("\n \"is_end_to_end_ChemCoreNext_arbitrator\":false,");
    report.append("\n \"source_classes_not_chemical_confirmation\":true,\"source_smoke_not_ppm\":true,");
    report.append("\n \"source_ppb_never_Tank_ppm\":true,\"source_methane_ppm_never_Tank_ppm\":true,");
    report.append("\n \"independent_MQ6400_source_exposures\":1,");
    report.append("\n \"G2_source_benchmark_not_G3\":true,");
    report.append("\n \"mq4_source\":{\"rows\":14186,\"physical_MQ4_instances\":3,\"days\":11,");
    report.append("\"reference_methane_min_ppm\":").append(fmt(methane.min));
    report.append(",\"reference_methane_max_ppm\":").append(fmt(methane.max));
    report.append(",\"original_source_rows_by_concentration_bin\":[");
    for(int j=0;j<4;j++){if(j>0)report.append(",");report.append(methane.referenceCounts[j]);}
    report.append("]},\n \"results\":[");
    for(int i=0;i<results.size();i++){
      Stats m=results.get(i);
      if(i>0)report.append(",");
      report.append("\n  ").append(m.json());
      System.out.println("NEXT_REAL_MQ_METRIC dataset="+m.dataset+" mode="+m.mode+
        " source_samples="+m.total+" folds="+m.folds+
        " accuracy_all="+fmt(m.accuracy())+
        " balanced_recall="+fmt(m.balancedRecall())+
        " abstained="+m.abstain+
        " smoke_recall="+(m.dataset.startsWith("MQ_EXT_03")?fmt(m.recall("Smoke")):"N/A"));
    }
    report.append("\n ],\"software_contract_checks\":").append(validations);
    report.append(",\"limitations\":[");
    report.append("\"The classifier performs source-domain normalized RMS fingerprint nearest-prototype, not full independent ChemCore Next arbitration\",");
    report.append("\"MQ6400 classes are one contiguous original recording each, so block holdouts are not independent exposures\",");
    report.append("\"MQ135 concentration holdout uses original MQ135 source temporal signal with known source exposure labels, not Tank\",");
    report.append("\"MQ4 day-grouped external Li-Cor ppm bins not sensor-independent Tank transfer\",");
    report.append("\"No per-channel Tank R_air drift/noise records nor full Tank G3 samples\"]}\n");
    Files.writeString(out.resolve("next_real_mq_fingerprint_benchmark.json"),
      report.toString(),StandardCharsets.UTF_8);
    check(results.size()==12,"12 sensor subset and label/concentration modes");
    System.out.println("CHEMCORE_NEXT_REAL_MQ_FINGERPRINT_BENCHMARK_PASS modes="+results.size()+
      " uci_exposures=90 mq6400_rows=6400 methane_source_rows=14186"+
      " owner_Rair_eight=8 g3=false ppm_tank=false");
  }
}
