package com.hazrob.tank.chemcore.next;

/**
 * P02.2: explicit dual clean-air reference for Tank V6 firmware and MQ source curves.
 * A sensor's measured R_air and manufacturer curve R0_source are NOT the same value.
 *
 * Source reference: MQSensorsLib.calibrate(kCleanAir) -> Rs_air/kCleanAir.
 * The source K is an instrument/model/condition coefficient, NOT gas concentration.
 * This module never changes saved firmware R0 and never fabricates calibration.
 */
public final class R0ReferenceMath {
    private R0ReferenceMath() {}

    public static final String FIRMWARE_DEVICE_AIR =
        "DEVICE_CLEAN_AIR_RS_MEAN";
    public static final String FIRMWARE_LEGACY_SCALED =
        "MQ_FACTOR_SCALED_R0_NOT_AIR_RS";

    public enum ReferenceKind {
        DEVICE_CLEAN_AIR, LEGACY_SCALED_R0, UNKNOWN
    }
    public enum Status {
        AIR_REFERENCE_AVAILABLE, LEGACY_ONLY_NO_AIR_REFERENCE,
        INVALID_REFERENCE, SOURCE_K_RESEARCH_ONLY, SOURCE_MATCH_QUALIFIED,
        SOURCE_MODEL_MISMATCH, SOURCE_HEATER_MISMATCH, SOURCE_K_NOT_SUPPLIED
    }

    public static final class SourceK {
        public final String sourceId, sensorModel, heaterProfile;
        public final double cleanAirRatioK;
        public final boolean labVerifiedForThisDevice;
        public SourceK(String sourceId,String sensorModel,String heaterProfile,
                       double cleanAirRatioK,boolean labVerifiedForThisDevice) {
            if(empty(sourceId)||empty(sensorModel)||empty(heaterProfile))
                throw new IllegalArgumentException("source identity/model/heater required");
            if(!Double.isFinite(cleanAirRatioK)||cleanAirRatioK<=0)
                throw new IllegalArgumentException("K must be finite positive ratio");
            this.sourceId=sourceId;
            this.sensorModel=sensorModel;
            this.heaterProfile=heaterProfile;
            this.cleanAirRatioK=cleanAirRatioK;
            this.labVerifiedForThisDevice=labVerifiedForThisDevice;
        }
    }

    public static final class Reference {
        public final String physicalSensorUid,sensorModel,heaterProfile,firmwareDefinition;
        public final ReferenceKind kind;
        public final Double rAirKohm, legacyR0SourceKohm;
        public Reference(String physicalSensorUid,String sensorModel,String heaterProfile,
                         String firmwareDefinition,Double valueKohm) {
            if(empty(physicalSensorUid)||empty(sensorModel)||empty(heaterProfile))
                throw new IllegalArgumentException("device sensor UID/model/heater required");
            this.physicalSensorUid=physicalSensorUid;
            this.sensorModel=sensorModel;
            this.heaterProfile=heaterProfile;
            this.firmwareDefinition=firmwareDefinition==null?"":firmwareDefinition;
            this.kind=FIRMWARE_DEVICE_AIR.equals(firmwareDefinition)?
                ReferenceKind.DEVICE_CLEAN_AIR:FIRMWARE_LEGACY_SCALED.equals(firmwareDefinition)?
                ReferenceKind.LEGACY_SCALED_R0:ReferenceKind.UNKNOWN;
            Double valid=valueKohm!=null&&Double.isFinite(valueKohm)&&valueKohm>0?
                valueKohm:null;
            this.rAirKohm=this.kind==ReferenceKind.DEVICE_CLEAN_AIR?valid:null;
            this.legacyR0SourceKohm=this.kind==ReferenceKind.LEGACY_SCALED_R0?valid:null;
        }
    }

    public static final class Comparison {
        public final Status status;
        public final Double measuredRsKohm, measuredAirRsKohm, sourceR0Kohm,
                            rsOverAir, rsOverSourceR0;
        public final boolean sourceCurveUseQualified;
        public final String sourceRoot, reason;
        private Comparison(Status status,Double rs,Double air,Double source,
                           Double qAir,Double qSource,boolean accepted,
                           String root,String reason) {
            this.status=status;this.measuredRsKohm=rs;this.measuredAirRsKohm=air;
            this.sourceR0Kohm=source;this.rsOverAir=qAir;this.rsOverSourceR0=qSource;
            this.sourceCurveUseQualified=accepted;this.sourceRoot=root;this.reason=reason;
        }
    }

    public static Comparison compare(Reference ref,Double observedRsKohm,SourceK source) {
        if(ref==null)throw new IllegalArgumentException("reference record required");
        String root=source==null?"":source.sourceId;
        if(observedRsKohm==null||!Double.isFinite(observedRsKohm)||observedRsKohm<=0)
            return new Comparison(Status.INVALID_REFERENCE,null,ref.rAirKohm,null,
                                  null,null,false,root,"invalid observed Rs");
        if(ref.kind==ReferenceKind.UNKNOWN)
            return new Comparison(Status.INVALID_REFERENCE,observedRsKohm,null,null,
                                  null,null,false,root,"unknown firmware R0 definition");
        if(ref.kind==ReferenceKind.LEGACY_SCALED_R0) {
            // Historical values are not clean-air Rs means; never quietly multiply by K.
            Double q=ref.legacyR0SourceKohm==null?null:
                observedRsKohm/ref.legacyR0SourceKohm;
            return new Comparison(Status.LEGACY_ONLY_NO_AIR_REFERENCE,observedRsKohm,
                null,ref.legacyR0SourceKohm,null,q,false,root,
                "legacy source-scaled R0 is NOT a measured R_air; source compatibility unproven");
        }
        if(ref.rAirKohm==null)
            return new Comparison(Status.INVALID_REFERENCE,observedRsKohm,null,null,
                                  null,null,false,root,"clean-air resistance not available");
        double qAir=observedRsKohm/ref.rAirKohm;
        if(!Double.isFinite(qAir)||qAir<=0)
            return new Comparison(Status.INVALID_REFERENCE,observedRsKohm,ref.rAirKohm,
                                  null,null,null,false,root,"invalid air ratio");
        if(source==null)
            return new Comparison(Status.SOURCE_K_NOT_SUPPLIED,observedRsKohm,ref.rAirKohm,
                null,qAir,null,false,root,"air comparison available without manufacturer K");
        if(!ref.sensorModel.equalsIgnoreCase(source.sensorModel))
            return new Comparison(Status.SOURCE_MODEL_MISMATCH,observedRsKohm,ref.rAirKohm,
                null,qAir,null,false,root,"source K is for a different sensor model");
        if(!ref.heaterProfile.equals(source.heaterProfile))
            return new Comparison(Status.SOURCE_HEATER_MISMATCH,observedRsKohm,ref.rAirKohm,
                null,qAir,null,false,root,"manufacturer heater profile not matched");
        double sourceR0=ref.rAirKohm/source.cleanAirRatioK;
        double qSource=observedRsKohm/sourceR0;
        if(!Double.isFinite(sourceR0)||sourceR0<=0||!Double.isFinite(qSource)||qSource<=0)
            return new Comparison(Status.INVALID_REFERENCE,observedRsKohm,ref.rAirKohm,
                null,qAir,null,false,root,"source R0 transformation overflow/underflow");
        boolean qualified=source.labVerifiedForThisDevice;
        return new Comparison(qualified?Status.SOURCE_MATCH_QUALIFIED:
            Status.SOURCE_K_RESEARCH_ONLY,observedRsKohm,ref.rAirKohm,sourceR0,
            qAir,qSource,qualified,root,
            qualified?"laboratory-qualified source relationship":"manufacturer K research only; no Tank G3");
    }

    /**
     * Conditional mathematical transport ONLY. This method intentionally
     * does not compare heater profiles or assert source laboratory K: a
     * source example without matched heater evidence must still be inspectable
     * without being misrepresented as a certified gas measurement.
     *
     * Rs/R0_source = K*(Rs/R_air). Return values are never eligible to alter
     * the device R_air baseline, candidate ranking, G3 or a safety alarm.
     * The strict compare() method remains the qualification boundary.
     */
    public static Comparison researchTransform(Reference physical,Double observedRsKohm,
                                               SourceK source) {
        if(physical==null)throw new IllegalArgumentException("device reference required");
        String root=source==null?"":source.sourceId;
        if(physical.kind!=ReferenceKind.DEVICE_CLEAN_AIR ||
           physical.rAirKohm==null)
            return new Comparison(Status.INVALID_REFERENCE,observedRsKohm,null,
                null,null,null,false,root,
                "research transform requires a genuine device clean-air mean");
        if(observedRsKohm==null||!Double.isFinite(observedRsKohm)||observedRsKohm<=0)
            return new Comparison(Status.INVALID_REFERENCE,null,physical.rAirKohm,
                null,null,null,false,root,"positive sensor Rs required");
        double qAir=observedRsKohm/physical.rAirKohm;
        if(!Double.isFinite(qAir)||qAir<=0)
            return new Comparison(Status.INVALID_REFERENCE,observedRsKohm,
                physical.rAirKohm,null,null,null,false,root,"invalid air response");
        if(source==null)
            return new Comparison(Status.SOURCE_K_NOT_SUPPLIED,observedRsKohm,
                physical.rAirKohm,null,qAir,null,false,root,"source K not supplied");
        if(!physical.sensorModel.equalsIgnoreCase(source.sensorModel))
            return new Comparison(Status.SOURCE_MODEL_MISMATCH,observedRsKohm,
                physical.rAirKohm,null,qAir,null,false,root,
                "cannot transfer K across distinct sensor models");
        double sourceR0=physical.rAirKohm/source.cleanAirRatioK;
        double qSource=qAir*source.cleanAirRatioK;
        if(!Double.isFinite(sourceR0)||sourceR0<=0||
           !Double.isFinite(qSource)||qSource<=0)
            return new Comparison(Status.INVALID_REFERENCE,observedRsKohm,
                physical.rAirKohm,null,qAir,null,false,root,
                "source reference conversion not finite");
        return new Comparison(Status.SOURCE_K_RESEARCH_ONLY,observedRsKohm,
            physical.rAirKohm,sourceR0,qAir,qSource,false,root,
            "CONDITIONAL_MATH_ONLY_K_HEATER_DOMAIN_AND_G3_NOT_VERIFIED");
    }

    private static boolean empty(String s){return s==null||s.trim().isEmpty();}
}
