package com.hazrob.tank.chemcore.next;

import java.util.Arrays;

/**
 * ChemCore Next original-source temporal fingerprints — independent research code.
 *
 * Uses the exact UCI1081 900-point trace protocol. Source segment means are
 * normalized by pre-exposure baseline (0..299), NOT MQ sensor R0 and NOT ppm.
 * The extra transient/lag fields are research descriptors, not validated gas ID.
 *
 * 02.3/02.4/02.5 partial-port improvement: source-consistent temporal features,
 * independent of legacy V6, no secret input of gas label to extraction.
 */
public final class TemporalSourceFingerprint {
    private TemporalSourceFingerprint() {}

    public static final String SCHEMA = "chemcore.next.source.uci1081.3window/1";
    public static final String SOURCE_ROOT = "MQ-EXT-15_UCI1081_ORIGINAL";
    public static final String MEASUREMENT_PROFILE = "UCI1081_900_POINTS_PER_SOURCE_CHANNEL";
    public static final int REQUIRED_POINTS = 900;

    public static final class Features {
        public final double baseline, early, middle, endpoint;
        public final double earlyResponseFraction, endpointMinusEarly, recoveryNotObserved;
        public final int originalLength;
        public final boolean gasIdentityConfirmed, ppmCalibrationQualified;
        private Features(double baseline, double e, double m, double end, int length) {
            this.baseline = baseline;
            this.early = e;
            this.middle = m;
            this.endpoint = end;
            this.earlyResponseFraction = Math.abs(end) > 1e-10 ? e / end : Double.NaN;
            this.endpointMinusEarly = end - e;
            this.recoveryNotObserved = Double.NaN;
            this.originalLength = length;
            this.gasIdentityConfirmed = false;
            this.ppmCalibrationQualified = false;
        }
        public double[] exactSourceWindows() {
            return new double[]{endpoint, early, middle};
        }
        public boolean canReportMolecularGasOrPpm() {
            return false;
        }
    }

    /** Original source window ratios: (mean_window / mean_air_0_to_299) - 1. */
    public static Features extractOriginalUciWindowProtocol(double[] signal) {
        if (signal == null || signal.length != REQUIRED_POINTS)
            throw new IllegalArgumentException("original UCI window requires 900 source points");
        for (int i=0;i<signal.length;i++) {
            if (!Double.isFinite(signal[i]))
                throw new IllegalArgumentException("non-finite source trace at position "+i);
        }
        double baseline = mean(signal,0,300);
        if (!Double.isFinite(baseline) || Math.abs(baseline)<1e-9)
            throw new IllegalArgumentException("zero/unqualified baseline");
        double early = mean(signal,300,360)/baseline-1.0;
        double middle = mean(signal,600,660)/baseline-1.0;
        double end = mean(signal,840,900)/baseline-1.0;
        if (!Double.isFinite(early)||!Double.isFinite(middle)||!Double.isFinite(end))
            throw new IllegalArgumentException("invalid source normalized signal");
        return new Features(baseline,early,middle,end,signal.length);
    }

    public static double mean(double[] data,int from,int toExclusive) {
        if (data==null||from<0||toExclusive>data.length||from>=toExclusive)
            throw new IllegalArgumentException("invalid source averaging window");
        double sum=0;
        for(int i=from;i<toExclusive;i++)sum+=data[i];
        return sum/(toExclusive-from);
    }

    /**
     * Simple constant-size temporal signature from a TRAINING group only.
     * Source scale is absolute/robust median deviation, not sensor accuracy.
     */
    public static final class Prototype {
        public final String sourceRoot, sensorModel, category;
        public final int sourceTrainingExposures;
        public final double[] median, medianAbsDeviation;
        public final boolean hasPhysicalTankG3Evidence, isApprovedForGasInference;
        public Prototype(String sourceRoot,String sensorModel,String category,
                         double[][] trainingSourceFeatures) {
            if (blank(sourceRoot)||blank(sensorModel)||blank(category)||
                    trainingSourceFeatures==null||trainingSourceFeatures.length<2)
                throw new IllegalArgumentException("source prototype provenance and training rows required");
            this.sourceRoot=sourceRoot;
            this.sensorModel=sensorModel;
            this.category=category;
            sourceTrainingExposures=trainingSourceFeatures.length;
            median=new double[3];
            medianAbsDeviation=new double[3];
            for(int j=0;j<3;j++){
                double[] all=new double[trainingSourceFeatures.length];
                for(int i=0;i<all.length;i++){
                    if(trainingSourceFeatures[i]==null||trainingSourceFeatures[i].length!=3||
                            !Double.isFinite(trainingSourceFeatures[i][j]))
                        throw new IllegalArgumentException("source profile requires exactly 3 finite dimensions");
                    all[i]=trainingSourceFeatures[i][j];
                }
                median[j]=median(all);
                for(int i=0;i<all.length;i++)all[i]=Math.abs(all[i]-median[j]);
                medianAbsDeviation[j]=median(all);
            }
            hasPhysicalTankG3Evidence=false;
            isApprovedForGasInference=false;
        }
        public boolean sourceCompatible(String root,String sensor,int dimensions) {
            return sourceRoot.equals(root)&&sensorModel.equals(sensor)&&dimensions==3;
        }
        public double researchDistance(double[] sourceFeatures,String root,String sensor) {
            if (!sourceCompatible(root,sensor,sourceFeatures==null ? -1 : sourceFeatures.length))
                return Double.NaN;
            double sum=0;
            for(int j=0;j<3;j++) {
                if (!Double.isFinite(sourceFeatures[j]))return Double.NaN;
                // Floor is a numeric robustness parameter only, NOT source LOD.
                double denom=Math.max(1e-6,1.4826*medianAbsDeviation[j]);
                double x=(sourceFeatures[j]-median[j])/denom;
                sum+=x*x;
            }
            return Math.sqrt(sum);
        }
    }
    private static boolean blank(String s){return s==null||s.trim().isEmpty();}
    public static double median(double[] values) {
        if(values==null||values.length==0)throw new IllegalArgumentException("empty array");
        double[] sorted=values.clone();
        for(double v:sorted)if(!Double.isFinite(v))throw new IllegalArgumentException("non-finite median");
        Arrays.sort(sorted);
        int m=sorted.length/2;
        return (sorted.length&1)==1 ? sorted[m] : 0.5*sorted[m-1]+0.5*sorted[m];
    }
}
