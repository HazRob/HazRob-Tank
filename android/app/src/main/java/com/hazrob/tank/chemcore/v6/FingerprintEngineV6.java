package com.hazrob.tank.chemcore.v6;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * New independent V6 fingerprint matcher. NEVER compares incompatible feature
 * spaces or treats a shape score as a probability, ppm or physical calibration.
 *
 * All source signatures of the selected feature space are evaluated whenever
 * the input has >=1 compatible finite coordinate. Insufficient provenance,
 * few sensors, zero margin and conflicting classes are NOTES, not exclusions.
 * Actual invalid arithmetic cannot produce an invented numeric distance.
 */
public final class FingerprintEngineV6 {
    public static final String SCHEMA="hazrob-tank-v6-fingerprint-research/1";
    public static final String ADC="ADC_LOG_GEOMEAN_CLASS";
    public static final String OPERATIONAL="OPERATIONAL_LOG1PLUS_CLR";
    public static final String TEMPORAL="MQ135_BASELINE_NORMALIZED_TEMPORAL";
    public static final List<String> MQ_ORDER=Collections.unmodifiableList(
        Arrays.asList("MQ2","MQ3","MQ4","MQ5","MQ7","MQ8","MQ9","MQ135"));
    public static final List<String> OPERATIONAL_ORDER=Collections.unmodifiableList(
        Arrays.asList("MQ2","MQ4","MQ5","MQ7","MQ8","MQ135"));
    private FingerprintEngineV6(){}

    public static final class Input {
        public final String space,transformation,sourceContext;
        private final double[] features;
        public Input(String featureSpace,double[] sourceSpaceFeatures,
                     String transform,String context){
            space=featureSpace==null?"":featureSpace;
            features=sourceSpaceFeatures==null?new double[0]:sourceSpaceFeatures.clone();
            transformation=transform==null?"UNSPECIFIED":transform;
            sourceContext=context==null?"UNKNOWN_SOURCE":context;
        }
        public double[] values(){return features.clone();}
    }

    public static final class Hypothesis {
        public final String fingerprintId,sourceRoot,featureSpace,label;
        public final long chemicalId;
        public final double distance,similarityIndex,sourceConcentrationPpm;
        public final int overlapDimensions,sourcePrototypeRows,prototypeDimensions;
        public final List<String> notes;
        private Hypothesis(GasFingerprintCatalogV6.Signature s,
                           double distance,int shared,int finiteSource,
                           List<String> warnings){
            fingerprintId=s.id;sourceRoot=s.sourceRoot;featureSpace=s.space;
            label=s.label;chemicalId=s.chemicalId;
            sourceConcentrationPpm=s.sourceConcentrationPpm;
            sourcePrototypeRows=s.sourceRows;
            this.distance=distance;
            similarityIndex=100.0/(1.0+distance);
            overlapDimensions=shared;prototypeDimensions=finiteSource;
            notes=Collections.unmodifiableList(new ArrayList<>(warnings));
        }
    }

    public static final class Result {
        public final String space;
        public final List<Hypothesis> hypotheses;
        public final int sourceTemplatesInSpace,numericallyUnusableTemplates;
        public final boolean calibratedIdentity,reportablePpm,sourceDeviceEquivalence;
        private Result(String featureSpace,List<Hypothesis> all,int sourceN,int invalidN){
            space=featureSpace;
            hypotheses=Collections.unmodifiableList(new ArrayList<>(all));
            sourceTemplatesInSpace=sourceN;numericallyUnusableTemplates=invalidN;
            calibratedIdentity=false;reportablePpm=false;
            sourceDeviceEquivalence=false;
        }
    }

    private static boolean finite(double x){return Double.isFinite(x);}
    private static List<Integer> overlap(double[] observed,double[] reference){
        List<Integer> dimensions=new ArrayList<>();
        for(int i=0;i<Math.min(observed.length,reference.length);i++){
            if(finite(observed[i]) && finite(reference[i]))
                dimensions.add(i);
        }
        return dimensions;
    }
    private static double mean(double[] a,List<Integer> dims){
        double sum=0;for(int i:dims)sum+=a[i];return sum/dims.size();
    }

    /**
     * Feature-space input is ALREADY transformed. For cross-sensor CLR shapes,
     * observed/reference means are removed on their COMMON finite coordinates,
     * so a missing MQ4/MQ9 or partial data does not invent a channel.
     * For UCI temporal features the source SD is a per-coordinate scale;
     * neither the source concentration nor the fit score is a Tank ppm estimate.
     */
    public static Result match(Input observed){
        if(observed==null)return new Result("",Collections.emptyList(),0,0);
        List<GasFingerprintCatalogV6.Signature> sources=GasFingerprintCatalogV6.inSpace(observed.space);
        List<Hypothesis> scores=new ArrayList<>();
        int unusable=0;
        for(GasFingerprintCatalogV6.Signature source:sources){
            double[] reference=source.vector();
            double[] input=observed.features;
            List<Integer> dims=overlap(input,reference);
            if(dims.isEmpty()){unusable++;continue;}
            boolean isTemporal=TEMPORAL.equals(observed.space);
            double centerInput=isTemporal?0:mean(input,dims);
            double centerReference=isTemporal?0:mean(reference,dims);
            double[] aux=source.uncertainty();
            double residual=0;
            for(int index:dims){
                double error=(input[index]-centerInput)-(reference[index]-centerReference);
                if(isTemporal){
                    // Feature 0 and 1 are almost identical source features, while
                    // slope has units per-second. This normalizes dimensions
                    // without treating prototype SD as actual observation uncertainty.
                    double floor=index==2?0.00002:0.02;
                    double scale=index<aux.length&&finite(aux[index])
                        ?Math.max(floor,Math.abs(aux[index])):floor;
                    error/=scale;
                }else if(OPERATIONAL.equals(observed.space)){
                    // Robust inter-session prototype scatter in source vector.
                    double scale=index<aux.length&&finite(aux[index])
                        ?Math.max(0.05,Math.abs(aux[index])):0.05;
                    error/=scale;
                }
                residual+=error*error;
            }
            double rmse=Math.sqrt(residual/dims.size());
            if(!finite(rmse)){unusable++;continue;}
            int srcDims=0;
            for(double v:reference)if(finite(v))srcDims++;
            List<String> warnings=new ArrayList<>();
            warnings.add("SOURCE_INSTRUMENT_TRANSFER_UNVERIFIED");
            warnings.add("NOT_PHYSICAL_TANK_GAS_CALIBRATION");
            if(dims.size()<srcDims)warnings.add("PARTIAL_SENSOR_OR_FEATURE_OVERLAP");
            if(dims.size()==1)warnings.add("ONE_DIMENSION_ONLY_UNINFORMATIVE_SHAPE");
            if(!TEMPORAL.equals(observed.space) && 
                    !"SOURCE_NATIVE_FEATURES".equals(observed.transformation))
                warnings.add("RAW_SOURCE_TRANSFORMATION_NOT_CONFIRMED_EQUIVALENT");
            if(TEMPORAL.equals(observed.space))
                warnings.add("UCI_50_TO_200_PPB_SOURCE_CONTEXT_NOT_DEVICE_PPM");
            if(source.chemicalId<=0)
                warnings.add("OPERATIONAL_CLASS_NOT_A_MOLECULAR_IDENTITY");
            scores.add(new Hypothesis(source,rmse,dims.size(),srcDims,warnings));
        }
        scores.sort(Comparator.comparingDouble((Hypothesis h)->h.distance)
                              .thenComparing(h->h.fingerprintId));
        return new Result(observed.space,scores,sources.size(),unusable);
    }

    private static Input transform(Map<String,Double> sourceReadings,
                                   List<String> sourceOrder,boolean logOnePlus,
                                   String space,String context){
        double[] transformed=new double[sourceOrder.size()];
        Arrays.fill(transformed,Double.NaN);
        for(int i=0;i<sourceOrder.size();i++){
            Double value=sourceReadings==null?null:
                         sourceReadings.get(sourceOrder.get(i));
            if(value==null||!finite(value)||
               (logOnePlus?value<0:value<=0))continue;
            transformed[i]=logOnePlus?Math.log10(1.0+value):Math.log10(value);
        }
        // Do not center here: matcher centers only common channels against
        // each template. Important for sparse arrays with missing MQ4/MQ9.
        return new Input(space,transformed,
            logOnePlus?"LOG10_ONE_PLUS_RAW":"LOG10_RAW_ADC",context);
    }

    /** ADC-only transfer: requires actual source-like ADC values, not Rs/ohms. */
    public static Input fromAdc(Map<String,Double> adcBySensor,String sourceContext){
        return transform(adcBySensor,MQ_ORDER,false,ADC,sourceContext);
    }
    /** Operational raw source indices; not automatically equivalent to Tank ADC. */
    public static Input fromOperational(Map<String,Double> raw,String sourceContext){
        return transform(raw,OPERATIONAL_ORDER,true,OPERATIONAL,sourceContext);
    }
    /** Explicit feature vector from >=900 s UCI-style baseline/exposure windows. */
    public static Input fromMq135Temporal(double endpointOverBaseline,
                                           double endpointDeltaFraction,
                                           double endpointSlopePerSecond,
                                           String context){
        return new Input(TEMPORAL,new double[]{
            endpointOverBaseline,endpointDeltaFraction,endpointSlopePerSecond},
            "UCI_BASELINE_NORMALIZED_900S_WINDOWS",context);
    }
}
