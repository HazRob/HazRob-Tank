package com.hazrob.tank.chemcore.v6;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Independent, provenance-first research calculations for V6.
 * 01.5 environmental model; 05.1 single-sensor calibration;
 * 05.2 multipoint log-log calibration; 05.7 identity-conditional estimates;
 * 05.8 signal-domain LOD/LOQ.
 *
 * No fitted source coefficients are generated from the current measurement.
 * Physical reference concentrations must come from an external experiment.
 */
public final class CalibrationResearchV6 {
    private CalibrationResearchV6(){}
    private static boolean positive(double x) {return Double.isFinite(x)&&x>0;}

    public static final class EnvironmentalCorrection {
        public final double originalRsOverR0,correctedRsOverR0;
        public final double referenceTemperatureC,referenceHumidityPct;
        public final boolean usable;
        public final String interpretation;
        private EnvironmentalCorrection(double original,double correction,
                                        double t,double rh,boolean valid) {
            originalRsOverR0=original;correctedRsOverR0=correction;
            referenceTemperatureC=t;referenceHumidityPct=rh;usable=valid;
            interpretation=valid?"USER_SUPPLIED_SENSITIVITY_NOT_TANK_VALIDATED":
                                "MISSING_COEFFICIENT_OR_UNDEFINED_ARITHMETIC";
        }
    }
    /** 01.5 log10 corrected ratio = log10 observed ratio
     * - beta_T*(T-Tref)-beta_RH*(RH-RHref).
     * Coefficients MUST be explicitly sourced; neither zero nor a manufacturer's
     * compensation is silently assumed.
     */
    public static EnvironmentalCorrection correctEnvironment(
        double ratio,double temperatureC,double humidityPct,
        double referenceT,double referenceRh,double betaLog10PerC,
        double betaLog10PerRhPercent) {
        boolean ok=positive(ratio)&&Double.isFinite(temperatureC)&&
            Double.isFinite(humidityPct)&&Double.isFinite(referenceT)&&
            Double.isFinite(referenceRh)&&Double.isFinite(betaLog10PerC)&&
            Double.isFinite(betaLog10PerRhPercent);
        double corrected=ok?Math.pow(10,Math.log10(ratio)
            -betaLog10PerC*(temperatureC-referenceT)
            -betaLog10PerRhPercent*(humidityPct-referenceRh)):Double.NaN;
        if(!positive(corrected)){ok=false;corrected=Double.NaN;}
        return new EnvironmentalCorrection(ratio,corrected,referenceT,referenceRh,ok);
    }

    public static final class Point {
        public final double referencePpm, observedRsOverR0;
        public Point(double ppm,double ratio) {referencePpm=ppm;observedRsOverR0=ratio;}
        public boolean valid() {return positive(referencePpm)&&positive(observedRsOverR0);}
    }
    public static final class CurveFit {
        public final String sensorModel, sourceExperimentId;
        public final double a,b,rootMeanSquareLogResidual,minimumReferencePpm,
                            maximumReferencePpm;
        public final int totalReferencePoints,usedReferencePoints;
        public final boolean fitted,measuredOnTank;
        public final String formula;
        private CurveFit(String sensor,String source,int all,int usable,
                         double a,double b,double rmse,double min,double max) {
            sensorModel=sensor;sourceExperimentId=source;
            totalReferencePoints=all;usedReferencePoints=usable;
            this.a=a;this.b=b;rootMeanSquareLogResidual=rmse;
            minimumReferencePpm=min;maximumReferencePpm=max;
            fitted=positive(a)&&Double.isFinite(b)&&b!=0;
            measuredOnTank=false;
            formula="Rs/R0=a*ppm^b; source-dependent log10 fit";
        }
        public double estimatePpm(double observedRatio) {
            if(!fitted||!positive(observedRatio))return Double.NaN;
            double ppm=Math.pow(10,(Math.log10(observedRatio)-Math.log10(a))/b);
            return positive(ppm)?ppm:Double.NaN;
        }
    }
    /** 05.1: single-sensor response to ONE external known concentration is a
     * one-point anchor, NOT a fitted sensitivity exponent. Requires a supplied b.
     * Does not infer a concentration from a one-point source without b.
     */
    public static CurveFit singlePoint(String sensor,String source,Point anchor,
                                        double externallySpecifiedExponent) {
        if(anchor==null||!anchor.valid()||!Double.isFinite(externallySpecifiedExponent)||
           externallySpecifiedExponent==0)
            return new CurveFit(sensor,source,anchor==null?0:1,0,
                 Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN);
        double a=anchor.observedRsOverR0/Math.pow(anchor.referencePpm,
                                                    externallySpecifiedExponent);
        if(!positive(a))a=Double.NaN;
        return new CurveFit(sensor,source,1,1,a,externallySpecifiedExponent,
                            0,anchor.referencePpm,anchor.referencePpm);
    }

    /** 05.2: least-squares log10(Rs/R0) = log10(a) + b log10(ppm).
     * Rejects only invalid numerical points; a nonzero reference concentration
     * spread is mathematically needed to estimate b.
     */
    public static CurveFit multipoint(String sensor,String source,List<Point> points) {
        int n=points==null?0:points.size();
        List<Point> valid=new ArrayList<>();
        if(points!=null)for(Point p:points)if(p!=null&&p.valid())valid.add(p);
        int m=valid.size();
        if(m<2)return new CurveFit(sensor,source,n,m,Double.NaN,Double.NaN,
                                    Double.NaN,Double.NaN,Double.NaN);
        double sx=0,sy=0,min=Double.POSITIVE_INFINITY,max=0;
        for(Point p:valid) {
            sx+=Math.log10(p.referencePpm);sy+=Math.log10(p.observedRsOverR0);
            min=Math.min(min,p.referencePpm);max=Math.max(max,p.referencePpm);
        }
        double meanX=sx/m,meanY=sy/m,num=0,den=0;
        for(Point p:valid) {
            double dx=Math.log10(p.referencePpm)-meanX;
            num+=dx*(Math.log10(p.observedRsOverR0)-meanY);
            den+=dx*dx;
        }
        if(!positive(den))return new CurveFit(sensor,source,n,m,
                      Double.NaN,Double.NaN,Double.NaN,min,max);
        double b=num/den,a=Math.pow(10,meanY-b*meanX);
        double sum=0;
        for(Point p:valid) {
            double error=Math.log10(p.observedRsOverR0)-
                  (Math.log10(a)+b*Math.log10(p.referencePpm));
            sum+=error*error;
        }
        double rmse=Math.sqrt(sum/m);
        return new CurveFit(sensor,source,n,m,a,b,rmse,min,max);
    }

    /** 05.7: preserve every candidate-specific result. No pseudo identity
     * probability and no unconditional "one gas must exist" assignment.
     */
    public static final class ConditionalEstimate {
        public final long chemicalReferenceId;
        public final int sourceFits;
        public final List<Double> referenceCurvePpm;
        public final double medianResearchPpm,minResearchPpm,maxResearchPpm;
        public final boolean validatedIdentity;
        private ConditionalEstimate(long id,List<Double> values) {
            chemicalReferenceId=id;sourceFits=values.size();
            List<Double> a=new ArrayList<>(values);Collections.sort(a);
            referenceCurvePpm=Collections.unmodifiableList(a);
            minResearchPpm=a.isEmpty()?Double.NaN:a.get(0);
            maxResearchPpm=a.isEmpty()?Double.NaN:a.get(a.size()-1);
            medianResearchPpm=a.isEmpty()?Double.NaN:(a.size()%2==0?
                a.get(a.size()/2-1)/2+a.get(a.size()/2)/2:a.get(a.size()/2));
            validatedIdentity=false;
        }
    }
    public static List<ConditionalEstimate> conditionalEstimates(Engine.Result result) {
        if(result==null)return Collections.emptyList();
        java.util.Map<Long,List<Double>> fits=new java.util.LinkedHashMap<>();
        for(Engine.SourceFit fit:result.allSourceFits)
            if(fit.chemicalId>0&&positive(fit.fitPpm))
                fits.computeIfAbsent(fit.chemicalId,k->new ArrayList<>()).add(fit.fitPpm);
        List<ConditionalEstimate> output=new ArrayList<>();
        for(java.util.Map.Entry<Long,List<Double>> e:fits.entrySet())
            output.add(new ConditionalEstimate(e.getKey(),e.getValue()));
        return Collections.unmodifiableList(output);
    }

    /** 05.8: conventional source-specified blank signal noise / sensitivity
     * proxy. The returned concentration figures are research detection
     * estimates, not IUPAC/lab-certified LOD/LOQ.
     */
    public static final class DetectionLimit {
        public final double lodReferencePpm,loqReferencePpm,blankSigmaSignal,
                            slopeSignalPerPpm;
        public final int blankN;
        public final boolean numericallyDefined,physicallyVerified;
        private DetectionLimit(double lod,double loq,int n,double sigma,double slope) {
            lodReferencePpm=lod;loqReferencePpm=loq;blankN=n;
            blankSigmaSignal=sigma;slopeSignalPerPpm=slope;
            numericallyDefined=Double.isFinite(lod)&&Double.isFinite(loq);
            physicallyVerified=false;
        }
    }
    public static DetectionLimit blankBasedLimits(double[] blankResponse,
                                                   double sourceSlopeSignalPerPpm) {
        int n=0;double mean=0,m2=0;
        if(blankResponse!=null)for(double v:blankResponse)if(Double.isFinite(v)) {
            n++;double d=v-mean;mean+=d/n;m2+=d*(v-mean);
        }
        double sigma=n>=2?Math.sqrt(m2/(n-1)):Double.NaN;
        double sensitivity=Math.abs(sourceSlopeSignalPerPpm);
        boolean ok=Double.isFinite(sigma)&&positive(sensitivity);
        return new DetectionLimit(ok?3.3*sigma/sensitivity:Double.NaN,
            ok?10*sigma/sensitivity:Double.NaN,n,sigma,sourceSlopeSignalPerPpm);
    }
}
