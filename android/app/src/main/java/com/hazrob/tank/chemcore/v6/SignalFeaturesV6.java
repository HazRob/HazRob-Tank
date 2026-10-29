package com.hazrob.tank.chemcore.v6;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Clean-slate numerical signal techniques:
 * 02.1 filtering, 02.2 scaling, 02.3 static features, 02.4 temporal features.
 *
 * Input can represent a POST-WARMUP reading/normal measurement series.
 * This class DOES NOT turn MQ power-on warmup into a chemical classifier, does
 * not control a heater, and never produces physical ppm or a certified identity.
 * Missing/nonfinite/negative inputs are excluded only from undefined arithmetic;
 * the number of such samples is explicitly reported.
 */
public final class SignalFeaturesV6 {
    public static final String SCHEMA="hazrob-tank-v6-signal-math/1";
    private SignalFeaturesV6(){}

    public static final class Sample {
        public final long timeMs;
        public final double rawRs;
        public Sample(long timestampMs,double resistance){timeMs=timestampMs;rawRs=resistance;}
    }
    public static final class Summary {
        public final int total,used,invalid;
        public final double arithmeticMeanRs,medianRs,standardDeviationRs;
        public final double log10SlopePerSecond,relativeLastFirst,integratedLog10DeltaSeconds;
        public final long firstTimeMs,lastTimeMs;
        public final boolean calibratedConcentration;
        private Summary(int submitted,int size,double mean,double median,double std,
                        double slope,double delta,double auc,long first,long last){
            total=submitted;used=size;invalid=submitted-size;
            arithmeticMeanRs=mean;medianRs=median;standardDeviationRs=std;
            log10SlopePerSecond=slope;relativeLastFirst=delta;
            integratedLog10DeltaSeconds=auc;firstTimeMs=first;lastTimeMs=last;
            calibratedConcentration=false;
        }
    }
    private static boolean positive(double v){return v>0&&Double.isFinite(v);}

    public static double median(double[] values){
        if(values==null)return Double.NaN;
        List<Double> finite=new ArrayList<>();
        for(double x:values)if(Double.isFinite(x))finite.add(x);
        if(finite.isEmpty())return Double.NaN;
        Collections.sort(finite);
        int n=finite.size();
        return (n&1)==1 ? finite.get(n/2)
                          : finite.get(n/2-1)/2+finite.get(n/2)/2;
    }

    /**
     * Centered trailing median filter, output 1 value for each input.
     * NaN markers remain NaN if a window has no finite number.
     */
    public static double[] trailingMedian(double[] series,int windowSize){
        if(series==null)return new double[0];
        if(windowSize<1)throw new IllegalArgumentException("windowSize must be positive");
        double[] out=new double[series.length];
        for(int i=0;i<series.length;i++){
            int lo=Math.max(0,i-windowSize+1);
            out[i]=median(Arrays.copyOfRange(series,lo,i+1));
        }
        return out;
    }

    public static double[] log10GeometricShape(double[] series){
        if(series==null)return new double[0];
        double[] shape=new double[series.length];
        Arrays.fill(shape,Double.NaN);
        double mean=0;int count=0;
        for(double x:series)if(positive(x)){mean+=Math.log10(x);count++;}
        if(count==0)return shape;
        mean/=count;
        for(int i=0;i<series.length;i++)
            if(positive(series[i]))shape[i]=Math.log10(series[i])-mean;
        return shape;
    }

    public static Summary summarize(List<Sample> input){
        int n=input==null?0:input.size();
        List<Sample> clean=new ArrayList<>();
        if(input!=null)for(Sample s:input)
            if(s!=null && s.timeMs>=0 && positive(s.rawRs))
                clean.add(s);
        clean.sort(Comparator.comparingLong(s->s.timeMs));
        if(clean.isEmpty())
            return new Summary(n,0,Double.NaN,Double.NaN,Double.NaN,
                               Double.NaN,Double.NaN,Double.NaN,-1,-1);
        double sum=0;
        double[] samples=new double[clean.size()];
        for(int i=0;i<clean.size();i++){
            samples[i]=clean.get(i).rawRs;
            sum+=samples[i];
        }
        double mean=sum/samples.length,med=median(samples),variance=0;
        for(double r:samples){double e=r-mean;variance+=e*e;}
        double std=Math.sqrt(variance/samples.length);

        double sumT=0,sumL=0;
        final long initial=clean.get(0).timeMs,last=clean.get(clean.size()-1).timeMs;
        for(Sample s:clean){sumT+=(s.timeMs-initial)/1000.0;sumL+=Math.log10(s.rawRs);}
        double meanT=sumT/clean.size(),meanL=sumL/clean.size();
        double numerator=0,denominator=0,auc=0;
        for(int i=0;i<clean.size();i++){
            Sample s=clean.get(i);
            double t=(s.timeMs-initial)/1000.0, log=Math.log10(s.rawRs);
            numerator+=(t-meanT)*(log-meanL);
            denominator+=(t-meanT)*(t-meanT);
            if(i>0){
                Sample previous=clean.get(i-1);
                double dt=(s.timeMs-previous.timeMs)/1000.0;
                auc+=0.5*dt*(Math.log10(previous.rawRs/samples[0])+
                             Math.log10(s.rawRs/samples[0]));
            }
        }
        double slope=denominator>0?numerator/denominator:Double.NaN;
        double delta=(samples[samples.length-1]-samples[0])/samples[0];
        return new Summary(n,clean.size(),mean,med,std,slope,delta,
                           auc,initial,last);
    }
}
