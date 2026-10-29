package com.hazrob.tank.foodfreshness.meat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Single-session, independent FOOD-MEAT study similarity; no gas ChemCore.
 *
 * Recreates a portable seven-sensor projection of Wijaya et al. 2018 beef
 * dataset with per-cut held-out evaluation. An operator's three-minute scan
 * is NOT a replication of the paper's 36-hour time course, and the studied
 * meat type is beef only. Quality classes refer to source study labels,
 * not verified bacterial counts of the current food sample.
 */
public final class BeefResearchEngine {
    public static final String SCHEMA="hazrob-tank-food-beef-research/1";
    private BeefResearchEngine(){}
    public static final class Match {
        public final String classCode;
        public final double distance,descriptiveSimilarity;
        public final int trainingRows,overlap;
        Match(String code,double dist,int n,int sensors){
            classCode=code;distance=dist;trainingRows=n;overlap=sensors;
            descriptiveSimilarity=100.0/(1+dist);
        }
    }
    public static final class Result {
        public final List<Match> rankedClasses;
        public final int validChannels;
        public final boolean directMicrobiologyMeasured,foodSafetyCertified,
                             modelValidatedForCurrentTank;
        public final double studyHeldoutBalancedAccuracy;
        public final List<String> caveats;
        private Result(List<Match> ranked,int channels) {
            rankedClasses=Collections.unmodifiableList(new ArrayList<>(ranked));
            validChannels=channels;
            directMicrobiologyMeasured=false;
            foodSafetyCertified=false;
            modelValidatedForCurrentTank=false;
            studyHeldoutBalancedAccuracy=BeefResearchCatalog.HELDOUT_BALANCED_ACCURACY;
            caveats=Collections.unmodifiableList(Arrays.asList(
                "BEEF_ONLY_SOURCE_STUDY",
                "THREE_MINUTE_SESSION_NOT_36_HOUR_PAPER_PROTOCOL",
                "SOURCE_DEVICE_TO_TANK_TRANSFER_NOT_VALIDATED",
                "HELDOUT_BALANCED_ACCURACY_BELOW_0_40",
                "CLASS_SIMILARITY_NOT_MICROBIAL_COUNT_OR_EDIBILITY"
            ));
        }
    }
    /**
     * Input keys are physical MQ models and values are observed Rs resistances
     * in consistent positive units. Source data used Ω; centering log10 Rs
     * removes common gain factor (not independent channel response differences).
     */
    public static Result analyze(Map<String,Double> resistance) {
        List<Integer> validIdx=new ArrayList<>();
        List<Double> observed=new ArrayList<>();
        for(int i=0;i<BeefResearchCatalog.SENSOR_ORDER.size();i++){
            Double r=resistance==null?null:
                resistance.get(BeefResearchCatalog.SENSOR_ORDER.get(i));
            if(r==null||!Double.isFinite(r)||r<=0)continue;
            validIdx.add(i);
            observed.add(Math.log10(r));
        }
        List<Match> results=new ArrayList<>();
        if(!observed.isEmpty()){
            double mean=0;for(double v:observed)mean+=v;mean/=observed.size();
            for(BeefResearchCatalog.Template source:BeefResearchCatalog.classes()){
                double[] template=source.logShape();
                double targetMean=0;
                for(int i:validIdx)targetMean+=template[i];
                targetMean/=validIdx.size();
                double error=0;
                for(int j=0;j<validIdx.size();j++){
                    double d=(observed.get(j)-mean)-(template[validIdx.get(j)]-targetMean);
                    error+=d*d;
                }
                double dist=Math.sqrt(error/observed.size());
                if(Double.isFinite(dist))
                    results.add(new Match(source.label,dist,source.originalTrainingRows,observed.size()));
            }
        }
        results.sort(Comparator.comparingDouble((Match x)->x.distance)
                               .thenComparing(x->x.classCode));
        return new Result(results,observed.size());
    }
}
