package com.hazrob.tank.chemcore.v6;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Source-free statistical fitter for REPEATED OBSERVED clean-air Rs/R0.
 * A single mean R0, a factory curve or one instantaneous clean-air snapshot
 * cannot specify the expected noise or a natural clean-air band.
 *
 * All intervals are exploratory: empirical sensor reproducibility requires
 * repeats across days, humidity/temperature and heater phases. This class
 * cannot establish a chemical ppm threshold or certify safe atmosphere.
 */
public final class CleanAirReferenceStatisticsV14 {
    public static final String SCHEMA="hazrob-tank-v6-blank-rs-r0-v14/1";
    public static final int WINDOWS=3;
    public static final int MIN_SAMPLES_PER_WINDOW=10;
    private CleanAirReferenceStatisticsV14(){}
    private static boolean positive(double x){return Double.isFinite(x)&&x>0;}
    private static double median(List<Double> points){
        if(points.isEmpty())return Double.NaN;
        double[] ordered=new double[points.size()];
        for(int i=0;i<ordered.length;i++)ordered[i]=points.get(i);
        Arrays.sort(ordered);
        int n=ordered.length;
        return n%2==0?ordered[n/2-1]/2.0+ordered[n/2]/2.0:ordered[n/2];
    }
    public static final class Fit {
        public final String status;
        public final int totalSamples,windows,distinctDays;
        public final double referenceR0Kilohm,medianLogRatio,madLogRatio,
            lowerLogRatio,upperLogRatio;
        public final boolean numericalBandAvailable,validatedNormalAir;
        private Fit(String status,int count,int windows,int days,double r0,
                    double median,double mad,double lower,double upper){
            this.status=status;totalSamples=count;this.windows=windows;
            distinctDays=days;referenceR0Kilohm=r0;
            medianLogRatio=median;madLogRatio=mad;
            lowerLogRatio=lower;upperLogRatio=upper;
            numericalBandAvailable=Double.isFinite(lower)&&Double.isFinite(upper)&&lower<upper;
            validatedNormalAir=false;
        }
        public boolean inside(double rsKilohm){
            if(!numericalBandAvailable||!positive(rsKilohm))return false;
            double x=Math.log(rsKilohm/referenceR0Kilohm);
            return Double.isFinite(x)&&x>=lowerLogRatio&&x<=upperLogRatio;
        }
    }
    /**
     * Samples must belong to the SAME physical channel/device/R0 revision and
     * heater phase; caller documents environmental context independently.
     * The 3 accepted windows are equal-duration windows, not repetitions of
     * the same reading. No unqualified number is imputed for missing samples.
     */
    public static Fit fit(double referenceR0Kilohm,List<double[]> windowRs,
                          int distinctDays){
        if(!positive(referenceR0Kilohm)||windowRs==null||
            windowRs.size()!=WINDOWS||distinctDays<1)
            return new Fit("MISSING_REFERENCE_OR_WINDOWS",0,0,
                Math.max(0,distinctDays),referenceR0Kilohm,
                Double.NaN,Double.NaN,Double.NaN,Double.NaN);
        List<Double> logs=new ArrayList<>();
        for(double[] window:windowRs){
            if(window==null||window.length<MIN_SAMPLES_PER_WINDOW)
                return new Fit("INSUFFICIENT_WINDOW_REPETITIONS",logs.size(),WINDOWS,
                    distinctDays,referenceR0Kilohm,
                    Double.NaN,Double.NaN,Double.NaN,Double.NaN);
            int valid=0;
            for(double rs:window)if(positive(rs)){
                double x=Math.log(rs/referenceR0Kilohm);
                if(Double.isFinite(x)){logs.add(x);valid++;}
            }
            if(valid<MIN_SAMPLES_PER_WINDOW)
                return new Fit("INVALID_RESISTANCE_OR_MISSING_SAMPLES",logs.size(),WINDOWS,
                    distinctDays,referenceR0Kilohm,
                    Double.NaN,Double.NaN,Double.NaN,Double.NaN);
        }
        double center=median(logs);
        List<Double> deviation=new ArrayList<>();
        for(double x:logs)deviation.add(Math.abs(x-center));
        double mad=median(deviation);
        // Robust dispersion with 3 Gaussian-equivalent standard deviations,
        // widened to contain all observed blanks. No fixed 10% / 20% presumed
        // normal atmospheric range, and no manufacturer-gas formula is fitted.
        double sigma=mad*1.4826;
        double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;
        for(double x:logs){min=Math.min(min,x);max=Math.max(max,x);}
        double lower=Math.min(min,center-3*sigma);
        double upper=Math.max(max,center+3*sigma);
        if(!(Double.isFinite(lower)&&Double.isFinite(upper)&&lower<upper))
            return new Fit("UNRESOLVED_ZERO_DISPERSION",logs.size(),WINDOWS,
                distinctDays,referenceR0Kilohm,center,mad,Double.NaN,Double.NaN);
        String status=distinctDays<2?
            "WITHIN_RUN_ONLY_NOT_MULTI_DAY_NORMALITY":
            "RESEARCH_ENVELOPE_REQUIRES_T_RH_HEATER_AND_HOLDOUT";
        return new Fit(status,logs.size(),WINDOWS,distinctDays,
            referenceR0Kilohm,center,mad,lower,upper);
    }
}
