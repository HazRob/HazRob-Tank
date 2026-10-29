package com.hazrob.tank.chemcore.v6;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** N05-F: real ORIGINAL UCI1081 sealed concentration folds, NEW Java N03-F
 * SVM/stump forest/second-order boosted stumps/MLP, one-vs-rest six classes.
 * Research source-discrimination only; NEVER substitute for Tank G3.
 */
public final class Uci1081N03FJavaBenchmark {
    private Uci1081N03FJavaBenchmark(){}
    private static final String[] GASES={"ethanol","acetone","toluene",
        "ethyl acetate","isopropanol","hexane"};
    private static final int[] LEVELS={50,100,200};
    private static final String[] MODES={"MQ135_only","full_source_10_channels"};
    private static final String[] ALGORITHMS={
        "V6_LinearHingeSVM_OVR","V6_BootstrapStumpForest_OVR",
        "V6_NewtonBoostStumps_OVR","V6_OneHiddenMLP_OVR"
    };
    private static void require(boolean b,String message){
        if(!b)throw new IllegalArgumentException(message);
    }
    private static final class Record {
        final int id,ppb;
        final String gas;
        final double[] features;
        Record(int row,String gas,int level,double[] source){
            id=row;this.gas=gas;ppb=level;features=source;
        }
    }
    private static List<Record> load(Path csv)throws Exception {
        List<Record> records=new ArrayList<>();
        try(BufferedReader in=Files.newBufferedReader(csv,StandardCharsets.UTF_8)){
            String header=in.readLine();
            require(header!=null&&header.startsWith("row_id,gas,ppb,f00,"),
                "invalid verified UCI feature header");
            String line;
            while((line=in.readLine())!=null){
                String[] fields=line.split(",",-1);
                require(fields.length==33,"expected 33 source feature CSV columns");
                int row=Integer.parseInt(fields[0]),level=Integer.parseInt(fields[2]);
                require(row==records.size(),"source row order/integrity");
                require(Arrays.asList(GASES).contains(fields[1]),"unknown gas label");
                require(level==50||level==100||level==200,"unknown source level");
                double[] features=new double[30];
                for(int j=0;j<30;j++){
                    features[j]=Double.parseDouble(fields[j+3]);
                    require(Double.isFinite(features[j]),"missing source features");
                }
                records.add(new Record(row,fields[1],level,features));
            }
        }
        require(records.size()==90,"exactly 90 original source exposures");
        for(String gas:GASES)for(int level:LEVELS){
            int count=0;
            for(Record r:records)if(r.gas.equals(gas)&&r.ppb==level)count++;
            require(count==5,"exactly five original source replicates per group");
        }
        return records;
    }
    private static double[] select(Record r,String mode){
        if(mode.equals("MQ135_only"))
            return new double[]{r.features[4],r.features[14],r.features[24]};
        return r.features.clone();
    }
    private interface OvrModel {double score(double[] x);}
    private static OvrModel fit(String name,double[][] x,int[] labels,int seed){
        if(name.equals("V6_LinearHingeSVM_OVR")){
            var m=SupervisedLearningResearchV6.fitLinearSvm(x,labels,150,.04,.02);
            return m::margin;
        }
        if(name.equals("V6_BootstrapStumpForest_OVR")){
            var m=SupervisedLearningResearchV6.fitBootstrapStumps(x,labels,79,seed);
            return m::positiveVoteFraction;
        }
        if(name.equals("V6_NewtonBoostStumps_OVR")){
            var m=SupervisedLearningResearchV6.fitNewtonBoost(x,labels,12,.2,1);
            return m::sourceScore;
        }
        if(name.equals("V6_OneHiddenMLP_OVR")){
            var m=SupervisedLearningResearchV6.fitMlp(x,labels,4,800,.15,seed);
            return m::sourceScore;
        }
        throw new IllegalArgumentException("unknown V6 N03-F research model");
    }
    private static String[] evaluateModel(String algorithm,double[][] trainX,
                       String[] trainLabels,double[][] heldoutX,int seed){
        int n=trainX.length;
        require(n==60&&heldoutX.length==30,"60/30 source split");
        OvrModel[] models=new OvrModel[GASES.length];
        for(int c=0;c<GASES.length;c++){
            int[] y=new int[n];int positive=0;
            for(int i=0;i<n;i++){
                if(trainLabels[i].equals(GASES[c])){y[i]=1;positive++;}
            }
            require(positive==10,"one-vs-rest source class has ten train replicates");
            models[c]=fit(algorithm,trainX,y,seed+c*7919);
        }
        String[] predictions=new String[heldoutX.length];
        for(int i=0;i<heldoutX.length;i++){
            double best=-Double.MAX_VALUE;int chosen=-1;
            for(int c=0;c<models.length;c++){
                double score=models[c].score(heldoutX[i]);
                require(Double.isFinite(score),"nonfinite source model score");
                if(chosen<0||score>best){best=score;chosen=c;}
            }
            predictions[i]=GASES[chosen];
        }
        return predictions;
    }
    public static void main(String[] args)throws Exception{
        require(args.length==3,"expected verified features, predictions and fold metric paths");
        List<Record> source=load(Path.of(args[0]));
        try(BufferedWriter predictions=Files.newBufferedWriter(Path.of(args[1]),StandardCharsets.UTF_8);
            BufferedWriter metrics=Files.newBufferedWriter(Path.of(args[2]),StandardCharsets.UTF_8)){
            predictions.write("mode,algorithm,heldout_ppb,source_row_id,actual,predicted\n");
            metrics.write("mode,algorithm,heldout_ppb,training_exposures,test_exposures,accuracy,balanced_accuracy,macro_f1\n");
            for(String mode:MODES)for(int level:LEVELS){
                List<Record> train=new ArrayList<>(),test=new ArrayList<>();
                for(Record row:source)(row.ppb==level?test:train).add(row);
                require(train.size()==60&&test.size()==30,"entire source-concentration holdout");
                double[][] trainX=new double[60][],heldoutX=new double[30][];
                String[] trainY=new String[60],testY=new String[30];
                for(int i=0;i<60;i++){
                    trainX[i]=select(train.get(i),mode);trainY[i]=train.get(i).gas;
                }
                for(int i=0;i<30;i++){
                    heldoutX[i]=select(test.get(i),mode);testY[i]=test.get(i).gas;
                }
                for(String algorithm:ALGORITHMS){
                    String[] predicted=evaluateModel(algorithm,trainX,trainY,heldoutX,
                                                   20261029+level);
                    var score=SourceValidationV6.benchmark(testY,predicted,GASES);
                    require(score.evaluated==30,"all original heldout rows");
                    String row=String.format(Locale.ROOT,
                      "%s,%s,%d,60,30,%.10f,%.10f,%.10f%n",
                      mode,algorithm,level,score.accuracy,score.balancedAccuracy,score.macroF1);
                    metrics.write(row);System.out.print("N05_F_ORIGINAL "+row);
                    for(int i=0;i<30;i++)
                        predictions.write(mode+","+algorithm+","+level+","+
                            test.get(i).id+","+testY[i]+","+predicted[i]+"\n");
                }
            }
        }
        System.out.println("PASS N05-F original UCI source: 90 exposures, 24 classifier/fold"+
            " evaluations, 720 heldout predictions. External research ONLY; Tank G3=0.");
    }
}
