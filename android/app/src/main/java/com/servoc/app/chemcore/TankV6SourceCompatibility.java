package com.servoc.app.chemcore;

/**
 * HazRob Tank V6 source-to-R0 compatibility gate. No Android dependencies.
 *
 * This is a conservative preflight contract for future ChemCore V6 source
 * registry integration, NOT a new calibrated ppm engine. It is deliberately
 * not wired to the V5 production ranking path until integration tests pass.
 */
public final class TankV6SourceCompatibility {
    private TankV6SourceCompatibility(){}

    public enum R0Definition {
        CLEAN_AIR_RATIO,
        REFERENCE_GAS_AT_DECLARED_PPM,
        RS_AIR_DIRECT
    }
    public enum RatioDirection { RS_OVER_R0, R0_OVER_RS }
    public enum Level { NONE, RESEARCH_SCREENING, DEVICE_VALIDATED_M4 }

    public static final class Sample {
        public final String deviceId, sensorInstance, sensorModel;
        public final double rsOhm, r0Ohm;
        public final R0Definition r0Definition;
        public final RatioDirection ratioDirection;
        public final boolean heaterValidated;
        public Sample(String device, String instance, String model, double rs, double r0,
                      R0Definition definition, RatioDirection direction, boolean heaterValid) {
            deviceId=device; sensorInstance=instance; sensorModel=model;
            rsOhm=rs; r0Ohm=r0; r0Definition=definition;
            ratioDirection=direction; heaterValidated=heaterValid;
        }
    }

    public static final class Source {
        public final String id, sensorModel, deviceScope, sensorInstanceScope;
        public final R0Definition r0Definition;
        public final RatioDirection ratioDirection;
        public final double minPpm, maxPpm;
        public final boolean deviceValidated;
        public final String calibrationId;
        public Source(String sourceId, String sensor, String device, String instance,
                      R0Definition definition, RatioDirection direction,
                      double min, double max, boolean validated, String calibration) {
            id=sourceId; sensorModel=sensor; deviceScope=device; sensorInstanceScope=instance;
            r0Definition=definition; ratioDirection=direction; minPpm=min; maxPpm=max;
            deviceValidated=validated; calibrationId=calibration;
        }
    }

    public static final class Verdict {
        public final boolean usable;
        public final Level level;
        public final String reason;
        public final double log10RsOverR0;
        private Verdict(boolean accepted, Level acceptedLevel, String code, double ratio){
            usable=accepted; level=acceptedLevel; reason=code; log10RsOverR0=ratio;
        }
    }

    private static boolean finitePositive(double v) {
        return Double.isFinite(v) && v>0;
    }
    private static boolean hasValue(String v) {
        return v!=null && !v.trim().isEmpty();
    }
    private static Verdict deny(String reason) {
        return new Verdict(false, Level.NONE, reason, Double.NaN);
    }

    /**
     * Caller supplies ppm only for checking the original source curve domain;
     * it MUST NOT manufacture ppm from Rs. Importing a peer device reference
     * requires its source R0 definition, never guesses a clean-air reference.
     */
    public static Verdict evaluate(Sample sample, Source source, double proposedPpm,
                                   boolean reportablePpmRequested) {
        if(sample==null || source==null) return deny("MISSING_SAMPLE_OR_SOURCE");
        if(!hasValue(source.id) || !hasValue(source.sensorModel))
            return deny("SOURCE_PROVENANCE_MISSING");
        if(!hasValue(sample.sensorModel) || !source.sensorModel.equalsIgnoreCase(sample.sensorModel))
            return deny("SENSOR_MODEL_MISMATCH");
        if(source.deviceScope!=null && !source.deviceScope.equals(sample.deviceId))
            return deny("DEVICE_SCOPE_MISMATCH");
        if(source.sensorInstanceScope!=null &&
           !source.sensorInstanceScope.equals(sample.sensorInstance))
            return deny("SENSOR_INSTANCE_MISMATCH");
        if(sample.r0Definition==null || source.r0Definition==null ||
           sample.r0Definition!=source.r0Definition)
            return deny("INCOMPATIBLE_R0_REFERENCE");
        if(sample.ratioDirection==null || source.ratioDirection==null)
            return deny("UNKNOWN_RATIO_DIRECTION");
        if(!finitePositive(sample.rsOhm) || !finitePositive(sample.r0Ohm))
            return deny("INVALID_RS_OR_R0");
        if(!finitePositive(proposedPpm) || !finitePositive(source.minPpm) ||
           !finitePositive(source.maxPpm) || source.maxPpm<=source.minPpm ||
           proposedPpm<source.minPpm || proposedPpm>source.maxPpm)
            return deny("OUTSIDE_DECLARED_SOURCE_DOMAIN");
        // Ratio convention is normalized to Rs/R0 once; no double inversion.
        double ratio=sample.rsOhm/sample.r0Ohm;
        double logRatio=Math.log10(ratio);
        if(!finitePositive(ratio) || !Double.isFinite(logRatio))
            return deny("INVALID_NORMALIZED_RESPONSE");
        if(reportablePpmRequested) {
            if(!sample.heaterValidated)
                return deny("HEATER_PROTOCOL_UNVERIFIED");
            if(!source.deviceValidated || !hasValue(source.calibrationId) ||
               !hasValue(source.deviceScope) || !hasValue(source.sensorInstanceScope))
                return deny("M4_DEVICE_REFERENCE_MISSING");
            return new Verdict(true, Level.DEVICE_VALIDATED_M4, "M4_SOURCE_CONTRACT_ONLY",
                               logRatio);
        }
        return new Verdict(true, Level.RESEARCH_SCREENING, "SOURCE_COMPATIBLE_RESEARCH",
                           logRatio);
    }

    public static double computeRsOhm(double rlOhm,double vcVolts,double loadVolts){
        if(!finitePositive(rlOhm) || !finitePositive(vcVolts) ||
           !finitePositive(loadVolts) || loadVolts>=vcVolts)
            throw new IllegalArgumentException("INVALID_ELECTRICAL_CONTRACT");
        double rs=rlOhm*(vcVolts/loadVolts-1d);
        if(!finitePositive(rs))throw new IllegalArgumentException("INVALID_RS");
        return rs;
    }

    public static double computeCleanAirR0Ohm(double rsAirOhm,double cleanAirRatio){
        if(!finitePositive(rsAirOhm) || !finitePositive(cleanAirRatio))
            throw new IllegalArgumentException("INVALID_CLEAN_AIR_REFERENCE");
        return rsAirOhm/cleanAirRatio;
    }
}
