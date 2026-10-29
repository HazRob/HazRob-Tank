package com.hazrob.tank.chemcore.v6;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Independent ChemCore Tank V6 numerical physics and diagnostics, batch N03-A.
 *
 * Research techniques: 01.1 ADC to Rs, 01.2 air R0, 01.6 signal quality,
 * 01.7 drift monitoring, 02.6 3D/4D feature representation.
 *
 * Input/source validity is noted rather than imposing eligibility thresholds.
 * Only impossible arithmetic has a missing numeric result. No ESP32 heater
 * changes, no manufacturer G3 calibration, no predecessor ChemCore imports.
 */
public final class MeasurementDiagnosticsV6 {
    public static final String SCHEMA="tank-chemcore-v6-physics-diagnostics/1";
    private MeasurementDiagnosticsV6(){}
    private static boolean positive(double x){return Double.isFinite(x)&&x>0;}
    private static double median(List<Double> source) {
        if(source.isEmpty())return Double.NaN;
        List<Double> values=new ArrayList<>(source);
        Collections.sort(values);
        int n=values.size();
        return (n%2==0)?values.get(n/2-1)/2+values.get(n/2)/2:values.get(n/2);
    }

    public static final class AdcResistance {
        public final double vAdc,vSensor,rsOhm;
        public final boolean numericallyDefined;
        public final String wiringEquation,notice;
        private AdcResistance(double atAdc,double atSensor,double resistance,boolean ok){
            vAdc=atAdc;vSensor=atSensor;rsOhm=resistance;numericallyDefined=ok;
            wiringEquation="V_SENSOR=ADC/MAX*V_REF*DIVIDER_GAIN; Rs=RL*(VC/V_SENSOR-1)";
            notice=ok?"ELECTRICAL_DIVIDER_INFERENCE_NOT_INDEPENDENT_GAS_CALIBRATION":
                       "UNDEFINED_DIVIDER_ARITHMETIC";
        }
    }
    /**
     * Assumes Rs on high side and RL low side with sensor output tapped at RL.
     * dividerGain = physical V_SENSOR / ADC_PIN_VOLTAGE, not its reciprocal.
     * Same equations apply to the ESP32 analog reference, with input scale
     * requiring independent hardware identification.
     */
    public static AdcResistance adcToRs(double adc,double adcMaximum,double adcReferenceV,
                                       double dividerGain,double sensorSupplyV,double rlOhm){
        if(!Double.isFinite(adc)||!positive(adcMaximum)||!positive(adcReferenceV)||
           !positive(dividerGain)||!positive(sensorSupplyV)||!positive(rlOhm)||
           adc<0||adc>adcMaximum)
            return new AdcResistance(Double.NaN,Double.NaN,Double.NaN,false);
        double pinV=adc/adcMaximum*adcReferenceV;
        double sensorV=pinV*dividerGain;
        if(!positive(sensorV)||sensorV>=sensorSupplyV)
            return new AdcResistance(pinV,sensorV,Double.NaN,false);
        double rs=rlOhm*(sensorSupplyV/sensorV-1);
        return new AdcResistance(pinV,sensorV,positive(rs)?rs:Double.NaN,positive(rs));
    }

    public static final class AirR0 {
        public final int received,finiteAirSamples,invalidSamples;
        public final double medianAirRs,sourceAirRatio,r0Ohm;
        public final boolean numericallyDefined,physicalDeviceCalibrationProven;
        private AirR0(int n,int good,double sampleMedian,double factor,double r0){
            received=n;finiteAirSamples=good;invalidSamples=n-good;
            medianAirRs=sampleMedian;sourceAirRatio=factor;r0Ohm=r0;
            numericallyDefined=positive(r0);
            physicalDeviceCalibrationProven=false;
        }
    }
    /**
     * R0 = median(Rs measured in clean air) / (Rs_air/R0) given a SOURCE
     * factor with correctly declared ratio direction. Does not infer that
     * air was clean or sensor burn-in/thermostatic state was physically proven.
     */
    public static AirR0 cleanAirR0(double[] airRs,double rsAirOverR0){
        List<Double> usable=new ArrayList<>();
        if(airRs!=null)for(double x:airRs)if(positive(x))usable.add(x);
        double sample=median(usable);
        double r0=positive(sample)&&positive(rsAirOverR0)?sample/rsAirOverR0:Double.NaN;
        if(!positive(r0))r0=Double.NaN;
        return new AirR0(airRs==null?0:airRs.length,usable.size(),
                         sample,rsAirOverR0,r0);
    }

    public static final class Quality {
        public final int total,validResistance,invalidResistance,duplicateTimestamps;
        public final double medianResistance,robustMad,coefficientOfVariation,
                            timeSpanSeconds;
        public final List<String> notes;
        private Quality(int n,int valid,int dup,double median,double mad,double cv,
                        double span,List<String> notes){
            total=n;validResistance=valid;invalidResistance=n-valid;
            duplicateTimestamps=dup;medianResistance=median;robustMad=mad;
            coefficientOfVariation=cv;timeSpanSeconds=span;
            this.notes=Collections.unmodifiableList(new ArrayList<>(notes));
        }
    }
    /** 01.6: descriptive signal quality, no artificial quality acceptance gate. */
    public static Quality signalQuality(List<SignalFeaturesV6.Sample> readings){
        List<SignalFeaturesV6.Sample> clean=new ArrayList<>();
        int n=readings==null?0:readings.size();
        if(readings!=null)for(SignalFeaturesV6.Sample v:readings)
            if(v!=null&&v.timeMs>=0&&positive(v.rawRs))clean.add(v);
        clean.sort(Comparator.comparingLong(s->s.timeMs));
        int duplicates=0;
        List<Double> values=new ArrayList<>();
        double sum=0;
        for(int i=0;i<clean.size();i++){
            SignalFeaturesV6.Sample v=clean.get(i);
            if(i>0&&v.timeMs==clean.get(i-1).timeMs)duplicates++;
            values.add(v.rawRs);sum+=v.rawRs;
        }
        double med=median(values);
        List<Double> deviations=new ArrayList<>();
        double squared=0,mean=values.isEmpty()?Double.NaN:sum/values.size();
        for(double v:values){
            deviations.add(Math.abs(v-med));
            squared+=(v-mean)*(v-mean);
        }
        double mad=median(deviations);
        double cv=positive(mean)?Math.sqrt(squared/values.size())/mean:Double.NaN;
        double span=clean.isEmpty()?Double.NaN:
            (clean.get(clean.size()-1).timeMs-clean.get(0).timeMs)/1000.0;
        List<String> warnings=new ArrayList<>();
        if(n>clean.size())warnings.add("INVALID_NUMERIC_READINGS_NOT_USED_IN_STATISTICS");
        if(duplicates>0)warnings.add("DUPLICATE_SAMPLE_TIMESTAMPS");
        if(clean.size()==1)warnings.add("SINGLE_READING_NO_TEMPORAL_QUALITY_INFERENCE");
        if(clean.isEmpty())warnings.add("NO_VALID_NUMERIC_READINGS");
        return new Quality(n,clean.size(),duplicates,med,mad,cv,span,warnings);
    }

    public static final class Drift {
        public final double earlyMedianRs,lateMedianRs,
                            relativeDifference,log10Difference;
        public final int firstWindowCount,lastWindowCount;
        public final boolean deviceDriftProven;
        private Drift(double start,double end,int n0,int n1){
            earlyMedianRs=start;lateMedianRs=end;
            firstWindowCount=n0;lastWindowCount=n1;
            relativeDifference=positive(start)&&positive(end)?end/start-1:Double.NaN;
            log10Difference=positive(start)&&positive(end)?Math.log10(end/start):Double.NaN;
            deviceDriftProven=false;
        }
    }
    /**
     * 01.7: drift indicator from two chronological nonoverlapping windows.
     * Nonstationary environment/gas composition can produce the same signal;
     * this is not a confirmed hardware sensor aging/drift diagnosis.
     */
    public static Drift windowDrift(List<SignalFeaturesV6.Sample> samples,int window){
        if(window<1)throw new IllegalArgumentException("window must be positive");
        List<SignalFeaturesV6.Sample> valid=new ArrayList<>();
        if(samples!=null)for(SignalFeaturesV6.Sample s:samples)
            if(s!=null&&s.timeMs>=0&&positive(s.rawRs))valid.add(s);
        valid.sort(Comparator.comparingLong(s->s.timeMs));
        int n=valid.size(),take=Math.min(window,n/2);
        List<Double> early=new ArrayList<>(),late=new ArrayList<>();
        for(int i=0;i<take;i++){
            early.add(valid.get(i).rawRs);late.add(valid.get(n-take+i).rawRs);
        }
        return new Drift(median(early),median(late),take,take);
    }

    public static final class ContextVector {
        public final String[] names;
        public final double[] values;
        public final int usableDimensions;
        public final boolean quantitativeGasCalibration;
        private ContextVector(String[] fields,double[] vals){
            names=fields.clone();values=vals.clone();
            int n=0;for(double d:vals)if(Double.isFinite(d))n++;
            usableDimensions=n;quantitativeGasCalibration=false;
        }
        public double[] copyValues(){return values.clone();}
    }
    /** 02.6: canonical 3D/4D feature coordinates from time/environment/MQ.
     * Missing R0 leaves log10 ratio absent, but preserves T, RH, t coordinates.
     * Units and axes are explicit, no physical quantities merged unscaled.
     */
    public static ContextVector physical4d(double rs,double r0,double tC,
                                           double rhPct,double elapsedSeconds){
        double ratio=positive(rs)&&positive(r0)?rs/r0:Double.NaN;
        double y=positive(ratio)?Math.log10(ratio):Double.NaN;
        return new ContextVector(
            new String[]{"log10_rs_over_r0","temperature_c","humidity_percent","elapsed_seconds"},
            new double[]{y,Double.isFinite(tC)?tC:Double.NaN,
                Double.isFinite(rhPct)?rhPct:Double.NaN,
                Double.isFinite(elapsedSeconds)&&elapsedSeconds>=0?
                    elapsedSeconds:Double.NaN});
    }
}
