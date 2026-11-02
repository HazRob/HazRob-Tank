package com.hazrob.tank.foodfreshness.meat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Separate FOOD-ONLY diagnostic model fitted to Wijaya 2018 beef source
 * TS1..TS4 (8640 correlated minute rows), entirely outside gas ChemCore.
 *
 * Each source row: 7 MQ resistances -> centered log10(Rs) shape.
 * Four diagonal Gaussian class descriptions: arithmetic mean and population
 * variance per original class, fixed variance floor 0.01 in log shape units.
 * Score is a relative Gaussian cost, NOT posterior likelihood or food safety.
 *
 * Source cut-wise five-fold research mean balanced accuracy 62.93%; one cut
 * only 29.56%. No real Tank validation and no microbiological measurement.
 * Missing channels cause NOT_RUN, never impute invented training values.
 */
public final class BeefGaussianResearchEngine {
    public static final String MODEL="wijaya2018-ts1-ts4-gaussian-diagonal-log-shape/1";
    public static final String STUDY_DOI=BeefResearchCatalog.STUDY_DOI;
    public static final double SOURCE_FIVE_CUT_MEAN_BALANCED_ACCURACY=0.6293;
    public static final double SOURCE_TS5_BALANCED_ACCURACY=0.7034;
    public static final boolean TANK_VALIDATED=false;
    private static final String[] CLASSES={"excellent","good","acceptable","spoiled"};
    private static final int[] SOURCE_COUNTS={1320,2280,1680,3360};
    private static final double[][] MEANS={
        {-0.226490239769053,-0.070232515905940,0.227469319749900,0.074056141157433,0.306264440152348,-0.209530735436528,-0.101536409948163},
        {-0.367823829380562,0.095198392440081,0.084487258136658,-0.024839880158545,0.228797637966315,-0.038357333778447,0.022537754774500},
        {-0.175841379286928,0.154805255197871,-0.195076123133409,0.120470871563663,-0.106473326429264,0.119190284542975,0.082924417545091},
        {-0.156542364425359,0.148306943626627,-0.516927151654918,0.243784564667153,-0.033438439415393,0.185193772375209,0.129622674826682}
    };
    private static final double[][] VARIANCES={
        {0.081185046290213,0.008174313008986,0.011967361558992,0.096928568578506,0.006102170805909,0.020377049895381,0.017302416506657},
        {0.011928209994522,0.007197341931595,0.070682681285121,0.112014459784554,0.035843847128822,0.013915436992693,0.022826511564574},
        {0.019431217244504,0.007189536593080,0.093291305843764,0.195436049101462,0.016135607187613,0.030386082947045,0.012440269669894},
        {0.020061878024720,0.012127193500630,0.002353295714661,0.312412865497274,0.022349259520213,0.036477311327009,0.018956155126425}
    };
    private BeefGaussianResearchEngine(){}
    public static final class ClassScore {
        public final String classCode;
        public final double gaussianCost;
        public final int sourceRows;
        private ClassScore(String c,double cost,int rows){
            classCode=c;gaussianCost=cost;sourceRows=rows;
        }
    }
    public static final class Result {
        public final String status;
        public final List<ClassScore> ranked;
        public final int validChannels;
        public final boolean certifiedForFoodSafety=false,validatedOnTank=false;
        private Result(String s,List<ClassScore> r,int channels){
            status=s;ranked=Collections.unmodifiableList(new ArrayList<>(r));
            validChannels=channels;
        }
    }
    public static Result analyze(Map<String,Double> rs){
        double[] x=new double[7];int n=0;
        for(int i=0;i<7;i++){
            Double v=rs==null?null:rs.get(BeefResearchCatalog.SENSOR_ORDER.get(i));
            if(v==null||!Double.isFinite(v)||v<=0)continue;
            x[i]=Math.log10(v);n++;
        }
        if(n!=7)return new Result("NOT_RUN_REQUIRES_ALL_SEVEN_SOURCE_MATCHING_MQ",
                                 Collections.emptyList(),n);
        double avg=0;for(double v:x)avg+=v;avg/=7;
        for(int i=0;i<7;i++)x[i]-=avg;
        List<ClassScore> ranks=new ArrayList<>();
        for(int c=0;c<CLASSES.length;c++){
            double cost=0;
            for(int i=0;i<7;i++){
                double variance=Math.max(.01,VARIANCES[c][i]);
                double delta=x[i]-MEANS[c][i];
                cost+=delta*delta/variance+Math.log(variance);
            }
            if(Double.isFinite(cost))ranks.add(new ClassScore(CLASSES[c],cost,SOURCE_COUNTS[c]));
        }
        ranks.sort(Comparator.comparingDouble((ClassScore q)->q.gaussianCost)
                             .thenComparing(q->q.classCode));
        return new Result(ranks.isEmpty()?"NOT_EVALUABLE":"SOURCE_DIAGNOSTIC_ONLY",
                          ranks,n);
    }
    public static double[] classMean(String label){
        for(int i=0;i<CLASSES.length;i++)if(CLASSES[i].equals(label))
            return MEANS[i].clone();
        return new double[0];
    }
    public static double[] classVariance(String label){
        for(int i=0;i<CLASSES.length;i++)if(CLASSES[i].equals(label))
            return VARIANCES[i].clone();
        return new double[0];
    }
}
