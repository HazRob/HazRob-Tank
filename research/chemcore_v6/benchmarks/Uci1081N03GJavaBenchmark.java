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

/**
 * N05-G independent source-domain temporal sequence holdout.
 * Real original UCI1081 90 exposure rows; 30 bins/exposure are NOT
 * treated as independent experiments. Hold out 50/100/200 ppb complete
 * levels, 60 source exposure training, 30 heldout exposure test.
 *
 * CNN/TCN/GRU encoders are frozen and ONLY logistic readouts are trained.
 * GNN omitted: original UCI does not document physical sensor graph edges.
 * This benchmark neither validates Tank G3 nor learns physical ppm.
 */
public final class Uci1081N03GJavaBenchmark {
    private Uci1081N03GJavaBenchmark(){}
    private static final String[] GASES={"ethanol","acetone","toluene",
                                         "ethyl acetate","isopropanol","hexane"};
    private static final int[] LEVELS={50,100,200};
    private static final String[] MODES={"MQ135_only","full_source_10_channels"};
    private static final TemporalNeuralResearchV6.Kind[] METHODS={
        TemporalNeuralResearchV6.Kind.CNN1D_FROZEN_FILTERS,
        TemporalNeuralResearchV6.Kind.TCN_CAUSAL_FROZEN_FILTERS,
        TemporalNeuralResearchV6.Kind.GRU_FROZEN_GATES
    };
    private static void require(boolean b,String why){
        if(!b)throw new IllegalArgumentException(why);
    }
    private static final class SourceExposure {
        final int id,level;
        final String gas;
        final double[][] seq;
        SourceExposure(int id,String gas,int ppb,double[][] matrix){
            this.id=id;this.gas=gas;level=ppb;seq=matrix;
        }
    }
    private static List<SourceExposure> read(Path path)throws Exception {
        List<SourceExposure> out=new ArrayList<>();
        try(BufferedReader in=Files.newBufferedReader(path,StandardCharsets.UTF_8)){
            String header=in.readLine();
            require(header!=null&&header.startsWith("row_id,gas,ppb,t00_s00,"),
                "verified original temporal feature schema");
            String line;
            while((line=in.readLine())!=null){
                String[] cells=line.split(",",-1);
                require(cells.length==303,"exactly 30x10 signal values");
                int row=Integer.parseInt(cells[0]),ppb=Integer.parseInt(cells[2]);
                require(row==out.size(),"exposure source row order");
                require(Arrays.asList(GASES).contains(cells[1]),"original gas class");
                require(ppb==50||ppb==100||ppb==200,"source level");
                double[][] seq=new double[30][10];
                for(int t=0;t<30;t++)for(int s=0;s<10;s++){
                    seq[t][s]=Double.parseDouble(cells[3+t*10+s]);
                    require(Double.isFinite(seq[t][s]),"nonfinite original sequence");
                }
                out.add(new SourceExposure(row,cells[1],ppb,seq));
            }
        }
        require(out.size()==90,"90 UCI original exposure sequences");
        for(String gas:GASES)for(int level:LEVELS){
            int found=0;
            for(SourceExposure r:out)if(r.gas.equals(gas)&&r.level==level)found++;
            require(found==5,"UCI five original replicates per gas and level");
        }
        return out;
    }
    private static double[][] choose(double[][] input,String mode){
        if(mode.equals("full_source_10_channels"))return input;
        double[][] one=new double[input.length][1];
        for(int t=0;t<input.length;t++)one[t][0]=input[t][4];
        return one;
    }
    private static String[] classify(TemporalNeuralResearchV6.Kind method,
                                   double[][][] train,String[] labels,
                                   double[][][] test){
        int n=train.length;
        require(n==60&&test.length==30,"exact group partition 60/30");
        TemporalNeuralResearchV6.Model[] models=
            new TemporalNeuralResearchV6.Model[GASES.length];
        for(int c=0;c<GASES.length;c++){
            int[] binary=new int[n],pos= new int[]{0};
            for(int i=0;i<n;i++)if(labels[i].equals(GASES[c])){
                binary[i]=1;pos[0]++;
            }
            require(pos[0]==10,"train originals only ten positives per gas");
            models[c]=TemporalNeuralResearchV6.fitBinary(
                    method,train,binary,null,550,.12,.002);
            require(!models[c].encoderWeightsTrained &&
                    !models[c].probabilitiesCalibrated &&
                    !models[c].physicalTankGasIdentityValidated,
                    "source research claims boundary");
        }
        String[] result=new String[test.length];
        for(int i=0;i<test.length;i++){
            double best=Double.NEGATIVE_INFINITY;int winner=-1;
            for(int c=0;c<GASES.length;c++){
                double score=models[c].sourceRelativeScore(test[i]);
                require(Double.isFinite(score),"unbounded result");
                if(winner<0||score>best){best=score;winner=c;}
            }
            result[i]=GASES[winner];
        }
        return result;
    }
    public static void main(String[] args)throws Exception {
        require(args.length==3,"required source temporal CSV, predictions, metrics");
        List<SourceExposure> source=read(Path.of(args[0]));
        try(BufferedWriter predicted=Files.newBufferedWriter(Path.of(args[1]),StandardCharsets.UTF_8);
            BufferedWriter scored=Files.newBufferedWriter(Path.of(args[2]),StandardCharsets.UTF_8)){
            predicted.write("mode,method,heldout_ppb,source_row_id,actual,predicted\n");
            scored.write("mode,method,heldout_ppb,train_original_exposures,test_original_exposures,accuracy,balanced_accuracy,macro_f1\n");
            for(String mode:MODES)for(int level:LEVELS){
                List<SourceExposure> train=new ArrayList<>(),test=new ArrayList<>();
                for(SourceExposure r:source)(r.level==level?test:train).add(r);
                require(train.size()==60&&test.size()==30,"whole concentration source split");
                double[][][] fit=new double[60][][],holdout=new double[30][][];
                String[] labels=new String[60],truth=new String[30];
                for(int i=0;i<60;i++){
                    fit[i]=choose(train.get(i).seq,mode);
                    labels[i]=train.get(i).gas;
                }
                for(int i=0;i<30;i++){
                    holdout[i]=choose(test.get(i).seq,mode);
                    truth[i]=test.get(i).gas;
                }
                for(TemporalNeuralResearchV6.Kind method:METHODS){
                    String[] guess=classify(method,fit,labels,holdout);
                    var report=SourceValidationV6.benchmark(truth,guess,GASES);
                    require(report.evaluated==30,"30 original heldout exposures");
                    String stat=String.format(Locale.ROOT,
                        "%s,%s,%d,60,30,%.10f,%.10f,%.10f%n",
                        mode,method.name(),level,report.accuracy,
                        report.balancedAccuracy,report.macroF1);
                    scored.write(stat);
                    System.out.print("N05G_ORIGINAL_SOURCE "+stat);
                    for(int i=0;i<30;i++)
                        predicted.write(mode+","+method.name()+","+level+","+
                            test.get(i).id+","+truth[i]+","+guess[i]+"\n");
                }
            }
        }
        System.out.println("PASS N05-G 90 ORIGINAL UCI1081 source exposures, "+
            "18 complete concentration holdouts, 540 predictions, GNN withheld "+
            "pending source graph evidence; G3=0/80.");
    }
}
