package com.hazrob.tank.chemcore.next;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * P04.3: time-resolved, source-locked research evidence from real snapshots.
 * Four features below share ONE physical series and ONE evidence root.
 * No interpolation, inferred time points, gas identification or calibrated ppm.
 */
public final class TemporalEvidenceExpert {
    private TemporalEvidenceExpert() {}
    public static final String VERSION = "chemcore-next-temporal-expert/0.1-g1";

    public enum Status {
        RESEARCH_FEATURES_ONLY, UNQUALIFIED_PROTOCOL, SENSOR_MISMATCH,
        INVALID_SERIES, TIMEBASE_OR_DEVICE_MISMATCH, QUALITY_OR_HEATER_INVALID,
        MISSING_OR_SHORT_WINDOW, WINDOW_GAP_TOO_LARGE, NUMERIC_FAILURE
    }

    public static final class Protocol {
        public final String sourceRoot, featureSpace, sensorModel, heaterPhase;
        public final long[][] windowsMs; // baseline, early, middle, late; half-open
        public final int minimumSamplesPerWindow;
        public final long maximumWithinWindowGapMs;
        public Protocol(String root, String space, String model, String phase, long[][] windows,
                        int minSamples, long maxGap) {
            sourceRoot=required(root); featureSpace=required(space);
            sensorModel=required(model); heaterPhase=required(phase);
            if(windows==null || windows.length!=4 || minSamples<2 || maxGap<=0)
                throw new IllegalArgumentException("four real windows, >=2 samples and positive maximum gap required");
            windowsMs=new long[4][2];
            for(int i=0;i<4;i++) {
                if(windows[i]==null || windows[i].length!=2
                    || windows[i][0]<0 || windows[i][1]<=windows[i][0]
                    || (i>0 && windows[i][0]<windows[i-1][1]))
                    throw new IllegalArgumentException("windows must be ordered, non-overlapping and finite");
                windowsMs[i]=windows[i].clone();
            }
            minimumSamplesPerWindow=minSamples;
            maximumWithinWindowGapMs=maxGap;
        }
        private static String required(String s){
            if(s==null || s.trim().isEmpty())throw new IllegalArgumentException("source provenance required");
            return s.trim();
        }
    }

    /** A caller must supply independently verified acquisition metadata. */
    public static final class Qualification {
        public final boolean sourceWindowDefinitionVerified, timebaseVerified,
            deviceSensorInstanceVerified, heaterScheduleVerified;
        public Qualification(boolean source, boolean time, boolean device, boolean heater) {
            sourceWindowDefinitionVerified=source; timebaseVerified=time;
            deviceSensorInstanceVerified=device; heaterScheduleVerified=heater;
        }
        public static Qualification unverified(){return new Qualification(false,false,false,false);}
        public boolean valid() {
            return sourceWindowDefinitionVerified && timebaseVerified
                && deviceSensorInstanceVerified && heaterScheduleVerified;
        }
    }

    public static final class Output {
        public final Status status;
        public final String sourceRoot, featureSpace, sensorUid, exposureUid;
        public final int[] samplesPerWindow;
        public final Double baselineRatio;
        public final double[] fourResearchFeatures;
        public final boolean gasIdentityConfirmed, ppmAuthorized, g3Qualified;
        public final String explanation;
        private Output(Status status,Protocol protocol,String uid,String exposure,int[] counts,
                       Double base,double[] features,String explanation) {
            this.status=status;sourceRoot=protocol.sourceRoot;featureSpace=protocol.featureSpace;
            sensorUid=uid==null?"":uid;exposureUid=exposure==null?"":exposure;
            samplesPerWindow=counts.clone();baselineRatio=base;
            fourResearchFeatures=features==null?null:features.clone();
            gasIdentityConfirmed=false;ppmAuthorized=false;g3Qualified=false;
            this.explanation=explanation;
        }
        public boolean hasFeatures(){return status==Status.RESEARCH_FEATURES_ONLY;}
        /** 3 window means plus a derived lag-like contrast: NOT four independent measurements. */
        public double[] featureCopy() {
            return fourResearchFeatures==null?null:fourResearchFeatures.clone();
        }
    }

    public static Output analyze(Protocol protocol, Qualification gate,
                                 CoreContracts.SensorInstance sensor, String deviceUid,
                                 String exposureUid, long captureStartMonotonicMs,
                                 List<CoreContracts.Snapshot> timeSeries) {
        if(protocol==null || gate==null)throw new IllegalArgumentException("protocol and gate required");
        String uid=sensor==null?"":sensor.uid;
        int[] counts=new int[4];
        if(!gate.valid())
            return fail(protocol,uid,exposureUid,counts,Status.UNQUALIFIED_PROTOCOL);
        if(sensor==null || !protocol.sensorModel.equals(sensor.model)
                || !protocol.heaterPhase.equals(sensor.heaterProfile))
            return fail(protocol,uid,exposureUid,counts,Status.SENSOR_MISMATCH);
        if(deviceUid==null || deviceUid.isBlank() || exposureUid==null || exposureUid.isBlank()
           || captureStartMonotonicMs<0 || timeSeries==null || timeSeries.isEmpty())
            return fail(protocol,uid,exposureUid,counts,Status.INVALID_SERIES);
        double[] sums=new double[4];
        long[] lastInWindow={-1,-1,-1,-1};
        long lastAbsolute=-1;
        String stableHash=null;
        Set<String> snapshotUids=new HashSet<>();
        for(CoreContracts.Snapshot s:timeSeries) {
            if(s==null || !deviceUid.equals(s.deviceUid)
               || s.monotonicMillis<captureStartMonotonicMs || s.monotonicMillis<=lastAbsolute
               || !snapshotUids.add(s.uid) || s.stableManifestHash.isBlank()
               || (stableHash!=null && !stableHash.equals(s.stableManifestHash)))
                return fail(protocol,uid,exposureUid,counts,Status.TIMEBASE_OR_DEVICE_MISMATCH);
            stableHash=s.stableManifestHash;
            lastAbsolute=s.monotonicMillis;
            CoreContracts.Reading found=null;
            for(CoreContracts.Reading r:s.readings)if(uid.equals(r.sensorUid))found=r;
            if(found==null || !s.mqRelayOn
               || found.quality!=CoreContracts.SampleQuality.VALID
               || !protocol.heaterPhase.equals(found.heaterPhase))
                return fail(protocol,uid,exposureUid,counts,Status.QUALITY_OR_HEATER_INVALID);
            double ratio=found.airRatioOrNaN();
            if(!Double.isFinite(ratio) || ratio<=0)
                return fail(protocol,uid,exposureUid,counts,Status.QUALITY_OR_HEATER_INVALID);
            long t=s.monotonicMillis-captureStartMonotonicMs;
            if(t<0) return fail(protocol,uid,exposureUid,counts,Status.TIMEBASE_OR_DEVICE_MISMATCH);
            for(int w=0;w<4;w++) {
                if(t>=protocol.windowsMs[w][0] && t<protocol.windowsMs[w][1]) {
                    if(lastInWindow[w]>=0 && t-lastInWindow[w]>protocol.maximumWithinWindowGapMs)
                        return fail(protocol,uid,exposureUid,counts,Status.WINDOW_GAP_TOO_LARGE);
                    lastInWindow[w]=t;counts[w]++;
                    sums[w]+=ratio;
                    if(!Double.isFinite(sums[w]))
                        return fail(protocol,uid,exposureUid,counts,Status.NUMERIC_FAILURE);
                    break;
                }
            }
        }
        for(int n:counts)
            if(n<protocol.minimumSamplesPerWindow)
                return fail(protocol,uid,exposureUid,counts,Status.MISSING_OR_SHORT_WINDOW);
        double base=sums[0]/counts[0];
        if(!Double.isFinite(base)||base<=0)
            return fail(protocol,uid,exposureUid,counts,Status.NUMERIC_FAILURE);
        double e=sums[1]/counts[1]/base-1.0;
        double m=sums[2]/counts[2]/base-1.0;
        double l=sums[3]/counts[3]/base-1.0;
        double[] features={e,m,l,l-e};
        for(double v:features)if(!Double.isFinite(v))
            return fail(protocol,uid,exposureUid,counts,Status.NUMERIC_FAILURE);
        return new Output(Status.RESEARCH_FEATURES_ONLY,protocol,uid,exposureUid,counts,
            base,features,"four descriptors from one source series; no recovered baseline, posterior, ppm or G3");
    }
    private static Output fail(Protocol p,String uid,String exposure,int[] counts,Status why) {
        return new Output(why,p,uid,exposure,counts,null,null,"fail closed: "+why);
    }
}
