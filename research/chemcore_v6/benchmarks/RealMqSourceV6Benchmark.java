package com.hazrob.tank.chemcore.v6;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * MQ-ONLY source benchmark; never manufactures missing Tank channels.
 *
 * Study A: physically recorded 6400 source raw MQ ADC readings, 4 classes:
 * NoGas/Perfume/Smoke/Mixture (NOT molecular labels, NO ppm).
 * 5 blocked ordered segments/class, 20-row embargo around test segments;
 * source conditions and day/device grouping NOT independently supplied.
 *
 * Study B: ORIGINAL 90 UCI1081 exposures, MQ135 channel ONLY; six chemical
 * identities x 50/100/200 ppb. 3 WHOLE-CONCENTRATION holdouts, source
 * Softmax discrimination plus true-gas conditioned log-ratio concentration
 * inversion using reference anchors FIT ONLY ON TRAIN concentration levels.
 * Below source MQ detection limit, no certified Tank ppm or G3.
 */
public final class RealMqSourceV6Benchmark {
    private static final String[] CLASSES={"NoGas","Perfume","Smoke","Mixture"};
    private static final String[] GASES={"ethanol","acetone","toluene",
        "ethyl acetate","isopropanol","hexane"};
    private static final int[] LEVELS={50,100,200};
    private static final String[] MODES={"MQ135_only","TANK_OVERLAP_6_MQ","SOURCE_ALL_7_MQ"};
    private static final int EMBARGO=20;
    private static void req(boolean ok,String message){
        if(!ok)throw new IllegalArgumentException(message);
    }
    private static double number(String value){
        double x=Double.parseDouble(value);req(Double.isFinite(x),"nonfinite original source reading");
        return x;
    }
    private static final class Raw {
        final int serial,group,position;
        final String label;
        final double[] seven;
        Raw(int serial,int group,int pos,String label,double[] source){
            this.serial=serial;this.group=group;position=pos;this.label=label;seven=source;
        }
    }
    private static double[] select(Raw r,String mode){
        // Raw CSV columns: MQ2,MQ3,MQ5,MQ6,MQ7,MQ8,MQ135
        if("MQ135_only".equals(mode))return new double[]{r.seven[6]};
        if("TANK_OVERLAP_6_MQ".equals(mode))
            return new double[]{r.seven[0],r.seven[1],r.seven[2],
                r.seven[4],r.seven[5],r.seven[6]};
        return r.seven.clone();
    }
    private static List<Raw> readMQ(Path path)throws Exception{
        List<Raw> raw=new ArrayList<>();
        Map<String,Integer> counter=new HashMap<>();
        try(BufferedReader reader=Files.newBufferedReader(path,StandardCharsets.UTF_8)){
            String header=reader.readLine();
            req(header!=null&&header.replace(" ","").equals(
                "SerialNumber,MQ2,MQ3,MQ5,MQ6,MQ7,MQ8,MQ135,Gas"),
                "source raw ADC schema/source MQ models changed");
            String line;
            while((line=reader.readLine())!=null){
                String[] v=line.split(",",-1);
                req(v.length==9,"nine original ADC source fields");
                String label=v[8].trim();
                req(Arrays.asList(CLASSES).contains(label),"source label class");
                int position=counter.getOrDefault(label,0);
                counter.put(label,position+1);
                double[] x=new double[7];
                for(int j=0;j<7;j++)x[j]=number(v[j+1]);
                req(Integer.parseInt(v[0].trim())==position,"source serial must reset per gas class to 0..1599");
                raw.add(new Raw(raw.size(),position/320,position,label,x));
            }
        }
        req(raw.size()==6400,"exact source 6400 rows");
        for(String label:CLASSES)
            req(counter.getOrDefault(label,0)==1600,"1600 original rows per class");
        return raw;
    }
    private static void rawClassification(Path rawPath,BufferedWriter metric,
                 BufferedWriter predictions)throws Exception{
        List<Raw> all=readMQ(rawPath);
        metric.write("dataset,mode,fold,train_rows,test_rows,embargo_rows,accuracy,balanced_accuracy,macro_f1\n");
        predictions.write("dataset,mode,fold,source_row_index_zero_based,actual_class,predicted_class,source_raw_adc_no_ppm\n");
        for(String mode:MODES)for(int fold=0;fold<5;fold++){
            List<double[]> features=new ArrayList<>();
            List<String> labels=new ArrayList<>();
            List<Raw> test=new ArrayList<>();
            int held=0;
            for(Raw row:all){
                if(row.group==fold){test.add(row);continue;}
                int lower=fold*320,upper=lower+320;
                if(row.position>=lower-EMBARGO&&row.position<upper+EMBARGO){
                    held++;continue;
                }
                features.add(select(row,mode));labels.add(row.label);
            }
            req(test.size()==1280,"four source classes x 320 heldout rows");
            req(features.size()+held+test.size()==all.size(),"source row partition audit");
            req(features.size()>=4800,"source train population sufficient");
            var fitted=ClassicalLearningV6.fitSoftmax(
                new ClassicalLearningV6.Dataset(features.toArray(new double[0][]),
                                                 labels.toArray(new String[0])),
                600,.16,.003);
            req(fitted.sourceTrainingRows==features.size()&&
                fitted.classes.size()==4,"four source class training count");
            String[] actual=new String[test.size()],pred=new String[test.size()];
            for(int i=0;i<test.size();i++){
                Raw row=test.get(i);
                actual[i]=row.label;
                pred[i]=fitted.scores(select(row,mode)).get(0).label;
                predictions.write(String.format(Locale.ROOT,
                    "MQ6400,%s,%d,%d,%s,%s,true%n",
                    mode,fold,row.serial,row.label,pred[i]));
            }
            var evaluated=SourceValidationV6.benchmark(actual,pred,CLASSES);
            req(evaluated.evaluated==1280,"all source blocked test rows scored");
            String rec=String.format(Locale.ROOT,
                "MQ6400,%s,%d,%d,%d,%d,%.8f,%.8f,%.8f%n",
                mode,fold,features.size(),test.size(),held,
                evaluated.accuracy,evaluated.balancedAccuracy,evaluated.macroF1);
            metric.write(rec);
            System.out.print("REAL_MQ_RAW "+rec);
        }
        System.out.println("PASS MQ6400: 15 source blocked folds, 19200 predictions, raw source ADC only, NO ppm.");
    }
    private static final class Exposure {
        final int id,level;
        final String label;
        final double[] mq135;
        Exposure(int id,int concentration,String gas,double[] x){
            this.id=id;level=concentration;label=gas;mq135=x;
        }
    }
    private static List<Exposure> readOriginalUci(Path path)throws Exception{
        List<Exposure> rows=new ArrayList<>();
        try(BufferedReader reader=Files.newBufferedReader(path,StandardCharsets.UTF_8)){
            String header=reader.readLine();
            req(header!=null&&header.startsWith("row_id,gas,ppb,f00,"),
                "original source extractor schema changed");
            String line;
            while((line=reader.readLine())!=null){
                String[] v=line.split(",",-1);req(v.length==33,"30 source features");
                int id=Integer.parseInt(v[0]),level=Integer.parseInt(v[2]);
                req(id==rows.size()&&Arrays.asList(GASES).contains(v[1])&&
                    (level==50||level==100||level==200),"original row integrity");
                double[] x={number(v[7]),number(v[17]),number(v[27])};
                req(x[0]>-1,"MQ135 response baseline must be positive ratio");
                rows.add(new Exposure(id,level,v[1],x));
            }
        }
        req(rows.size()==90,"90 original source exposures");
        for(String gas:GASES)for(int level:LEVELS){
            int count=0;for(Exposure e:rows)
                if(e.label.equals(gas)&&e.level==level)count++;
            req(count==5,"five original sensor samples per gas-concentration group");
        }
        return rows;
    }
    private static void concentration(Path input,BufferedWriter classification,
        BufferedWriter heldoutPredictions,BufferedWriter concentrationRows)throws Exception{
        List<Exposure> all=readOriginalUci(input);
        classification.write("dataset,heldout_ppb,train_exposures,test_exposures,identity_accuracy,identity_balanced_accuracy,identity_macro_f1\n");
        heldoutPredictions.write("dataset,heldout_ppb,row_id,gas,original_concentration_ppb,predicted_gas,source_MQ135_endpoint_ratio\n");
        concentrationRows.write("dataset,heldout_ppb,row_id,gas,true_ppb,estimated_ppb,"+
            "absolute_error_ppb,relative_error_fraction,status,training_levels_ppb,estimation_is_oracle_gas\n");
        for(int heldout:LEVELS){
            List<Exposure> train=new ArrayList<>(),test=new ArrayList<>();
            for(Exposure e:all)(e.level==heldout?test:train).add(e);
            req(train.size()==60&&test.size()==30,"whole concentration-level holdout");
            double[][] x=new double[60][];String[] y=new String[60];
            for(int i=0;i<60;i++){x[i]=train.get(i).mq135;y[i]=train.get(i).label;}
            var fitted=ClassicalLearningV6.fitSoftmax(
                new ClassicalLearningV6.Dataset(x,y),800,.15,.005);
            String[] actual=new String[30],prediction=new String[30];
            int[] anchors=Arrays.stream(LEVELS).filter(v->v!=heldout).toArray();
            for(int i=0;i<30;i++){
                Exposure row=test.get(i);actual[i]=row.label;
                prediction[i]=fitted.scores(row.mq135).get(0).label;
                heldoutPredictions.write(String.format(Locale.ROOT,
                    "UCI1081_MQ135,%d,%d,%s,%d,%s,%.12f%n",
                    heldout,row.id,row.label,row.level,prediction[i],1+row.mq135[0]));
                double[] averages=new double[2];
                for(int k=0;k<2;k++){
                    for(Exposure e:train)if(e.label.equals(row.label)&&e.level==anchors[k])
                        averages[k]+=1+e.mq135[0];
                    averages[k]/=5;
                }
                String status="UNIDENTIFIABLE_SOURCE_ANCHORS";
                double estimated=Double.NaN,ae=Double.NaN,re=Double.NaN;
                if(Double.isFinite(averages[0])&&Double.isFinite(averages[1])&&
                    averages[0]>0&&averages[1]>0&&
                    Math.abs(Math.log(averages[1]/averages[0]))>1e-9){
                    try{
                        var fit=SourceNumericsV6.estimateUnseen(
                            new double[]{anchors[0],anchors[1]},
                            averages,1+row.mq135[0]);
                        estimated=fit.researchEstimatePpm;
                        if(Double.isFinite(estimated)&&estimated>0){
                            ae=Math.abs(estimated-row.level);
                            re=ae/row.level;
                            status=fit.extrapolated?
                                "EXTRAPOLATION_OUTSIDE_REFERENCE_DOMAIN":
                                "INTERPOLATION_INSIDE_REFERENCE_DOMAIN";
                        }else status="SOURCE_EXTRAPOLATION_UNBOUNDED";
                    }catch(IllegalArgumentException invalid){
                        status="SOURCE_RATIO_INVERSION_REFUSED";
                    }
                }
                concentrationRows.write(String.format(Locale.ROOT,
                    "UCI1081_MQ135,%d,%d,%s,%d,%s,%s,%s,%s,%d|%d,true%n",
                    heldout,row.id,row.label,row.level,
                    Double.isFinite(estimated)?String.format(Locale.ROOT,"%.12e",estimated):"",
                    Double.isFinite(ae)?String.format(Locale.ROOT,"%.12e",ae):"",
                    Double.isFinite(re)?String.format(Locale.ROOT,"%.12e",re):"",
                    status,anchors[0],anchors[1]));
            }
            var score=SourceValidationV6.benchmark(actual,prediction,GASES);
            String line=String.format(Locale.ROOT,
                "UCI1081_MQ135,%d,60,30,%.8f,%.8f,%.8f%n",
                heldout,score.accuracy,score.balancedAccuracy,score.macroF1);
            classification.write(line);
            System.out.print("REAL_MQ135_LEVEL "+line);
        }
        System.out.println("PASS MQ135: 6 actual source chemicals x 3 levels 50/100/200 ppb; "+
            "3 full concentration holdouts, 90 original heldout identity and source concentration errors.");
    }
    public static void main(String[] args)throws Exception{
        req(args.length==7,"Usage raw_6400.csv official_uci_90.csv raw_metrics.csv raw_predictions.csv mq135_classification.csv mq135_predictions.csv mq135_concentrations.csv");
        try(BufferedWriter a=Files.newBufferedWriter(Path.of(args[2]),StandardCharsets.UTF_8);
            BufferedWriter b=Files.newBufferedWriter(Path.of(args[3]),StandardCharsets.UTF_8);
            BufferedWriter c=Files.newBufferedWriter(Path.of(args[4]),StandardCharsets.UTF_8);
            BufferedWriter d=Files.newBufferedWriter(Path.of(args[5]),StandardCharsets.UTF_8);
            BufferedWriter e=Files.newBufferedWriter(Path.of(args[6]),StandardCharsets.UTF_8)){
            rawClassification(Path.of(args[0]),a,b);
            concentration(Path.of(args[1]),c,d,e);
        }
        System.out.println("PASS independent MQ model numerical source experiments; NO Tank G3, no food, no fabricated ppm.");
    }
}
