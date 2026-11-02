package com.hazrob.tank.chemcore.next;

import java.util.Locale;

/**
 * ChemCore Next P04.1 source-faithful algebraic curve expert.
 *
 * Mathematical inversion is G1 only. Hardware ppm estimates remain research
 * unless independent Tank reference/R0/heater/range evidence is collected and
 * G2/G3 qualification is established separately. This class NEVER activates
 * alarms, confirmed gas identity or field ppm output.
 *
 * MQDataScience form: Rs/R0 = a * (ppm/sourceScale)^b.
 * MQSensorsLib form: ppm = a * (Rs/R0)^b.
 * Coefficients from these forms are NOT interchangeable.
 */
public final class SourceCurveExpert {
    private SourceCurveExpert() {}

    public static final String VERSION = "chemcore-next-curve-expert/0.1-g1";
    public enum Equation {
        RATIO_EQUALS_A_TIMES_PPM_OVER_SCALE_POWER_B,
        PPM_EQUALS_A_TIMES_RATIO_POWER_B
    }
    public enum Decision {
        ALGEBRAIC_PARITY_ONLY,
        RESEARCH_SOURCE_QUANTITATIVE_ESTIMATE,
        NOT_EVALUABLE,
        OUTSIDE_SOURCE_DOMAIN
    }
    public enum AbstentionReason {
        NONE,
        READING_INVALID_OR_MISSING,
        SENSOR_MODEL_MISMATCH,
        SOURCE_R0_NOT_PROVEN_FOR_DEVICE,
        SOURCE_SCALE_UNVERIFIED,
        HEATER_PHASE_UNQUALIFIED,
        SOURCE_DOMAIN_UNVERIFIED,
        SOURCE_RATIO_UNAVAILABLE,
        NUMERIC_OUT_OF_DOMAIN,
        EXTRAPOLATION_BLOCKED
    }
    public static final class Curve {
        public final String id, sourceRoot, sensorModel, sourceGasLabel, requiredHeaterPhase;
        public final Long canonicalChemicalUid;
        public final Equation equation;
        public final double a, b;
        public final Double sourceScalePpm, sourcePpmMin, sourcePpmMax;

        public Curve(String id, String sourceRoot, String sensorModel,
                     String sourceGasLabel, Long canonicalChemicalUid,
                     Equation equation, double a, double b, Double sourceScalePpm,
                     Double sourcePpmMin, Double sourcePpmMax,
                     String requiredHeaterPhase) {
            this.id = required(id, "curve ID");
            this.sourceRoot = required(sourceRoot, "source root");
            this.sensorModel = required(sensorModel, "sensor model");
            this.sourceGasLabel = required(sourceGasLabel, "original source gas label");
            if (canonicalChemicalUid != null && canonicalChemicalUid <= 0)
                throw new IllegalArgumentException("invalid canonical UID");
            this.canonicalChemicalUid = canonicalChemicalUid;
            if (equation == null) throw new IllegalArgumentException("equation required");
            this.equation = equation;
            if (!Double.isFinite(a) || a <= 0 || !Double.isFinite(b) || b == 0)
                throw new IllegalArgumentException("finite a>0 and nonzero b required");
            this.a=a;this.b=b;
            validatePositive(sourceScalePpm, "source scale");
            if (equation == Equation.RATIO_EQUALS_A_TIMES_PPM_OVER_SCALE_POWER_B
                && sourceScalePpm == null) {
                // Unknown scales can be retained as metadata, but must never be
                // substituted with 1 or with V6's *assumed* scale at inference.
            }
            validatePositive(sourcePpmMin,"source ppm min");
            validatePositive(sourcePpmMax,"source ppm max");
            if (sourcePpmMin != null && sourcePpmMax != null &&
                sourcePpmMin >= sourcePpmMax)
                throw new IllegalArgumentException("source ppm interval must increase");
            this.sourceScalePpm=sourceScalePpm;
            this.sourcePpmMin=sourcePpmMin;
            this.sourcePpmMax=sourcePpmMax;
            this.requiredHeaterPhase=requiredHeaterPhase==null?"":requiredHeaterPhase;
        }
    }
    /**
     * Explicit provenance gates - these must be populated from actual validated
     * source and physical calibration records, not copied from an example.
     */
    public static final class Qualification {
        public final boolean scaleFromPrimarySourceVerified;
        public final boolean physicallyMatchedR0Definition;
        public final boolean sensorHeaterPhaseValidated;
        public final boolean sourceCalibrationRangeVerified;

        public Qualification(boolean scale,boolean r0,boolean heater,boolean domain) {
            scaleFromPrimarySourceVerified=scale;
            physicallyMatchedR0Definition=r0;
            sensorHeaterPhaseValidated=heater;
            sourceCalibrationRangeVerified=domain;
        }
        public static Qualification unverified() {
            return new Qualification(false,false,false,false);
        }
    }
    public static final class Output {
        public final String sourceCurveId, sourceRoot, sensorUid;
        public final Long canonicalChemicalUid;
        public final Double researchOnlyPpm;
        public final Decision decision;
        public final AbstentionReason abstention;
        public final boolean confirmedGas, fieldPpmAuthorized, safetyAlarmAuthorized, g3Qualified;
        private Output(Curve curve,String sensorUid,Double value,
                       Decision decision,AbstentionReason abstention) {
            sourceCurveId=curve.id;sourceRoot=curve.sourceRoot;
            this.sensorUid=sensorUid;
            canonicalChemicalUid=curve.canonicalChemicalUid;
            researchOnlyPpm=value;this.decision=decision;this.abstention=abstention;
            confirmedGas=false;
            fieldPpmAuthorized=false;
            safetyAlarmAuthorized=false;
            g3Qualified=false;
        }
    }
    private static String required(String s,String name) {
        if(s==null||s.trim().isEmpty())throw new IllegalArgumentException(name+" required");
        return s.trim();
    }
    private static void validatePositive(Double value,String field) {
        if(value != null && (!Double.isFinite(value)||value<=0))
            throw new IllegalArgumentException(field+" must be positive finite");
    }
    private static double result(double value) {
        if(!Double.isFinite(value)||value<=0)
            throw new IllegalArgumentException("source equation is undefined or exceeds numeric range");
        return value;
    }
    /** Research-only direct equation value; caller must supply original source scale. */
    public static double forwardSourceRatio(Curve curve,double ppm) {
        if(curve==null)throw new IllegalArgumentException("curve missing");
        result(ppm);
        if(curve.equation==Equation.RATIO_EQUALS_A_TIMES_PPM_OVER_SCALE_POWER_B) {
            if(curve.sourceScalePpm==null)throw new IllegalArgumentException("unverified/unknown source ppm scale");
            return result(curve.a*Math.pow(ppm/curve.sourceScalePpm,curve.b));
        }
        return result(Math.pow(ppm/curve.a,1.0/curve.b));
    }
    /** Algebraic-only ppm, not a measured/authorized concentration. */
    public static double inverseSourcePpm(Curve curve,double ratio) {
        if(curve==null)throw new IllegalArgumentException("curve missing");
        result(ratio);
        if(curve.equation==Equation.RATIO_EQUALS_A_TIMES_PPM_OVER_SCALE_POWER_B) {
            if(curve.sourceScalePpm==null)throw new IllegalArgumentException("unverified/unknown source ppm scale");
            return result(curve.sourceScalePpm*Math.pow(ratio/curve.a,1.0/curve.b));
        }
        return result(curve.a*Math.pow(ratio,curve.b));
    }
    /**
     * Fail-closed physical reading bridge, read-only; no result is a field
     * ppm until an independent qualification system certifies the device.
     */
    public static Output evaluate(Curve curve, CoreContracts.SensorInstance sensor,
                                  CoreContracts.Reading reading, Qualification q) {
        if(curve==null||q==null)throw new IllegalArgumentException("curve and provenance gate required");
        String uid=reading==null?"":reading.sensorUid;
        if(sensor==null||reading==null || reading.quality!=CoreContracts.SampleQuality.VALID
           || !sensor.uid.equals(reading.sensorUid))
            return reject(curve,uid,AbstentionReason.READING_INVALID_OR_MISSING);
        if(!curve.sensorModel.equals(sensor.model))
            return reject(curve,uid,AbstentionReason.SENSOR_MODEL_MISMATCH);
        if(reading.r0Definition!=CoreContracts.CalibrationReference.SOURCE_R0
           || !q.physicallyMatchedR0Definition)
            return reject(curve,uid,AbstentionReason.SOURCE_R0_NOT_PROVEN_FOR_DEVICE);
        if(curve.equation==Equation.RATIO_EQUALS_A_TIMES_PPM_OVER_SCALE_POWER_B
           && (!q.scaleFromPrimarySourceVerified||curve.sourceScalePpm==null))
            return reject(curve,uid,AbstentionReason.SOURCE_SCALE_UNVERIFIED);
        if(!q.sensorHeaterPhaseValidated || curve.requiredHeaterPhase.isEmpty()
           || !curve.requiredHeaterPhase.equals(reading.heaterPhase)
           || !curve.requiredHeaterPhase.equals(sensor.heaterProfile))
            return reject(curve,uid,AbstentionReason.HEATER_PHASE_UNQUALIFIED);
        if(!q.sourceCalibrationRangeVerified
           ||curve.sourcePpmMin==null||curve.sourcePpmMax==null)
            return reject(curve,uid,AbstentionReason.SOURCE_DOMAIN_UNVERIFIED);
        double ratio=reading.sourceRatioOrNaN();
        if(!Double.isFinite(ratio)||ratio<=0)
            return reject(curve,uid,AbstentionReason.SOURCE_RATIO_UNAVAILABLE);
        final double ppm;
        try {ppm=inverseSourcePpm(curve,ratio);}
        catch (IllegalArgumentException ex) {
            return reject(curve,uid,AbstentionReason.NUMERIC_OUT_OF_DOMAIN);
        }
        if(ppm<curve.sourcePpmMin || ppm>curve.sourcePpmMax)
            return new Output(curve,uid,null,Decision.OUTSIDE_SOURCE_DOMAIN,
                              AbstentionReason.EXTRAPOLATION_BLOCKED);
        return new Output(curve,uid,ppm,Decision.RESEARCH_SOURCE_QUANTITATIVE_ESTIMATE,
                          AbstentionReason.NONE);
    }
    private static Output reject(Curve curve,String uid,AbstentionReason reason) {
        return new Output(curve,uid,null,Decision.NOT_EVALUABLE,reason);
    }
}
