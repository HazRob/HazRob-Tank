package com.servoc.app.chemcore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * V6-002 source-curves explorer, derived byte-for-byte from pinned
 * mq_research_curves_43.json (git blob bd597ffe9ecaee6a0ccfddc5ec1b89c1cccc0ba1).
 *
 * Owner policy: use ALL numerically valid reference data without hard source,
 * heater, calibration, domain or label screening. Missing qualification becomes
 * an advisory, not a dropped hypothesis. Only undefined/non-finite arithmetic
 * prevents a numerical fit. This never turns an estimate into an actual Tank
 * physical calibration or a measured exposure.
 *
 * Not yet connected to V5 production inference or hazard/alarm logic.
 */
public final class SourceCurveRegistryV6 {
    private SourceCurveRegistryV6() {}
    public static final String SOURCE_ROOT = "MQDATASCIENCE_6_0_0";
    public static final String SOURCE_GIT_BLOB = "bd597ffe9ecaee6a0ccfddc5ec1b89c1cccc0ba1";
    public static final String SOURCE_COMMIT = "e88f876140587a708ee84a4d4d3acbd5720274ee";
    public static final String MATH_FORM = "RS_OVER_R0 = ratio_a * (ppm/output_scale)^ratio_b";

    public static final class Entry {
        public final int index;
        public final String sensorModel, gasLabel;
        public final long legacyId;
        public final double a,b,minPpm,maxPpm,outputScale;
        private Entry(int id,String sensor,String gas,long uid,double a,double b,
                      double low,double high,double scale) {
            index=id; sensorModel=sensor;gasLabel=gas;legacyId=uid;
            this.a=a;this.b=b;minPpm=low;maxPpm=high;outputScale=scale;
        }
        public String recordId(){return String.format(Locale.ROOT,"MQDATA43-%03d",index);}
        public boolean classOrMixedLabel() {
            return gasLabel.equalsIgnoreCase("alcohol") ||
                   gasLabel.equalsIgnoreCase("smoke") ||
                   gasLabel.equalsIgnoreCase("lpg") ||
                   gasLabel.equalsIgnoreCase("nh4/nh3");
        }
    }
    public enum NumericState { EXPLORATORY_FINITE, NUMERICALLY_UNDEFINED }
    public static final class Estimate {
        public final Entry source;
        public final NumericState state;
        public final double ppmResearch, observedRsOverR0;
        public final boolean outsideSourceDomain, sensorLabelMismatch;
        public final List<String> notes;
        private Estimate(Entry e,NumericState status,double ppm,double ratio,
                         boolean outside,boolean mismatch,List<String> warnings) {
            source=e;state=status;ppmResearch=ppm;observedRsOverR0=ratio;
            outsideSourceDomain=outside;sensorLabelMismatch=mismatch;
            notes=Collections.unmodifiableList(new ArrayList<>(warnings));
        }
        public boolean numericallyUsable(){return state==NumericState.EXPLORATORY_FINITE;}
        public boolean tankPhysicalCalibrationProven(){return false;}
    }

    private static final Entry[] ENTRIES = {
        new Entry(1, "MQ2", "LPG", 135, 17.6135, -0.4539, 200, 10000, 1),
        new Entry(2, "MQ2", "propane", 78, 19.5575, -0.461, 200, 10000, 1),
        new Entry(3, "MQ2", "H2", 75, 25.7473, -0.4731, 200, 10000, 1),
        new Entry(4, "MQ2", "alcohol", 103, 19.2641, -0.3604, 200, 10000, 1),
        new Entry(5, "MQ2", "CH4", 76, 20.7074, -0.36, 200, 10000, 1),
        new Entry(6, "MQ2", "smoke", 139, 26.4698, -0.3876, 200, 10000, 1),
        new Entry(7, "MQ2", "CO", 20, 28.024, -0.3182, 200, 10000, 1),
        new Entry(8, "MQ3", "Alcohol", 103, 0.5565, -0.6333, 5.15, 500, 50),
        new Entry(9, "MQ3", "Benzene", 34, 1.7957, -0.369, 5.15, 500, 50),
        new Entry(10, "MQ3", "Hexane", 84, 22.9337, -0.358, 5.15, 500, 50),
        new Entry(11, "MQ3", "LPG", 135, 27.8286, -0.2847, 5.15, 500, 50),
        new Entry(12, "MQ3", "CO", 20, 31.9559, -0.2325, 5.15, 500, 50),
        new Entry(13, "MQ3", "CH4", 76, 44.8389, -0.0476, 5.15, 500, 50),
        new Entry(14, "MQ4", "CH4", 76, 11.5517, -0.3541, 200, 10000, 1),
        new Entry(15, "MQ4", "LPG", 135, 14.1866, -0.3227, 200, 10000, 1),
        new Entry(16, "MQ4", "H2", 75, 9.6432, -0.1765, 200, 10000, 1),
        new Entry(17, "MQ4", "smoke", 139, 7.05, -0.1065, 200, 10000, 1),
        new Entry(18, "MQ4", "alcohol", 103, 5.7994, -0.0696, 200, 10000, 1),
        new Entry(19, "MQ4", "CO", 20, 5.49, -0.0495, 200, 10000, 1),
        new Entry(20, "MQ5", "LPG", 135, 6.1067, -0.4102, 200, 10000, 1),
        new Entry(21, "MQ5", "CH4", 76, 6.987, -0.3777, 200, 10000, 1),
        new Entry(22, "MQ5", "H2", 75, 7.4265, -0.2847, 200, 10000, 1),
        new Entry(23, "MQ5", "alcohol", 103, 10.8274, -0.2104, 200, 10000, 1),
        new Entry(24, "MQ5", "CO", 20, 8.2236, -0.1492, 200, 10000, 1),
        new Entry(25, "MQ7", "H2", 75, 21.1794, -0.7174, 50, 4000, 1),
        new Entry(26, "MQ7", "CO", 20, 20.89, -0.6568, 50, 4000, 1),
        new Entry(27, "MQ7", "LPG", 135, 14.9745, -0.1329, 50, 4000, 1),
        new Entry(28, "MQ7", "CH4", 76, 19.8094, -0.09, 50, 4000, 1),
        new Entry(29, "MQ7", "Alcohol", 103, 20.2524, -0.0655, 50, 4000, 1),
        new Entry(30, "MQ8", "H2", 75, 18391.5667, -1.4494, 200, 10000, 1),
        new Entry(31, "MQ8", "alcohol", 103, 468.8045, -0.5574, 200, 10000, 1),
        new Entry(32, "MQ8", "LPG", 135, 152.1255, -0.2922, 200, 10000, 1),
        new Entry(33, "MQ8", "CH4", 76, 117.9639, -0.1448, 200, 10000, 1),
        new Entry(34, "MQ8", "CO", 20, 126.605, -0.1173, 200, 10000, 1),
        new Entry(35, "MQ9", "CO", 20, 16.8797, -0.4445, 200, 1000, 1),
        new Entry(36, "MQ9", "LPG", 135, 24.918, -0.4695, 200, 10000, 1),
        new Entry(37, "MQ9", "CH4", 76, 21.8247, -0.3663, 200, 10000, 1),
        new Entry(38, "MQ135", "Acetone", 110, 2.7988, -0.2913, 10, 200, 1),
        new Entry(39, "MQ135", "Toluene", 95, 2.9788, -0.2892, 10, 200, 1),
        new Entry(40, "MQ135", "Alcohol", 103, 3.8623, -0.3126, 10, 200, 1),
        new Entry(41, "MQ135", "CO2", 23, 5.2278, -0.3585, 10, 200, 1),
        new Entry(42, "MQ135", "NH4/NH3", 21, 6.2968, -0.3892, 10, 200, 1),
        new Entry(43, "MQ135", "CO", 20, 5.0533, -0.2541, 10, 200, 1)
    };
    public static final List<Entry> ALL;
    static {
        List<Entry> copy=new ArrayList<>(ENTRIES.length);
        Collections.addAll(copy,ENTRIES);
        ALL=Collections.unmodifiableList(copy);
    }

    /** All coefficients are available even if reference R0/heater metadata is incomplete. */
    public static List<Entry> all(){return ALL;}

    public static List<Entry> forSensor(String model) {
        List<Entry> out=new ArrayList<>();
        for(Entry e:ALL)if(model!=null && e.sensorModel.equalsIgnoreCase(model.trim()))
            out.add(e);
        return Collections.unmodifiableList(out);
    }

    /**
     * Evaluate a research estimate for one candidate. The only hard rejection
     * is mathematically invalid input/result: zero/negative/nonfinite Rs, R0,
     * a, outputScale, zero slope, or overflow/underflow producing zero/infinity.
     * Source-domain, missing calibration definition, and heater caveats are
     * displayed but NEVER cause omission of an otherwise finite estimate.
     */
    public static Estimate explore(Entry entry,String observedSensorModel,
                                   double rs,double r0) {
        if(entry==null)throw new IllegalArgumentException("missing source curve");
        List<String> warnings=new ArrayList<>();
        warnings.add("SOURCE_R0_REFERENCE_NOT_INDEPENDENTLY_VERIFIED");
        warnings.add("SOURCE_HEATER_PROFILE_NOT_VERIFIED_ON_TANK");
        warnings.add("SOURCE_NOT_TANK_PHYSICAL_CALIBRATION");
        if(entry.outputScale!=1.0)warnings.add("NONUNITY_OUTPUT_SCALE_SOURCE_ASSUMPTION");
        if(entry.classOrMixedLabel())warnings.add("LABEL_IS_AGGREGATE_OR_AMBIGUOUS");
        boolean mismatch=observedSensorModel==null ||
                          !entry.sensorModel.equalsIgnoreCase(observedSensorModel.trim());
        if(mismatch)warnings.add("SENSOR_MODEL_MISMATCH_EXPLORATORY_ONLY");
        if(!validPositive(rs)||!validPositive(r0)||!validPositive(entry.a)||
           !Double.isFinite(entry.b)||entry.b==0||!validPositive(entry.outputScale)) {
            return new Estimate(entry,NumericState.NUMERICALLY_UNDEFINED,
                                Double.NaN,Double.NaN,false,mismatch,warnings);
        }
        double ratio=rs/r0;
        if(!validPositive(ratio))return new Estimate(entry,NumericState.NUMERICALLY_UNDEFINED,
                                Double.NaN,ratio,false,mismatch,warnings);
        double logPpm=Math.log(entry.outputScale)+
                       (Math.log(ratio)-Math.log(entry.a))/entry.b;
        double estimate=Math.exp(logPpm);
        if(!validPositive(estimate))return new Estimate(entry,NumericState.NUMERICALLY_UNDEFINED,
                                Double.NaN,ratio,false,mismatch,warnings);
        boolean outside=validPositive(entry.minPpm)&&validPositive(entry.maxPpm) &&
                        (estimate<entry.minPpm||estimate>entry.maxPpm);
        if(outside)warnings.add("EXTRAPOLATED_BEYOND_SOURCE_RANGE");
        return new Estimate(entry,NumericState.EXPLORATORY_FINITE,
                            estimate,ratio,outside,mismatch,warnings);
    }
    public static List<Estimate> exploreSensor(String sensor,double rs,double r0) {
        List<Estimate> results=new ArrayList<>();
        for(Entry e:forSensor(sensor))results.add(explore(e,sensor,rs,r0));
        return Collections.unmodifiableList(results);
    }
    private static boolean validPositive(double d){return Double.isFinite(d)&&d>0;}
}
