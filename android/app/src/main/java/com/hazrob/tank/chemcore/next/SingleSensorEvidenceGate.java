package com.hazrob.tank.chemcore.next;

/**
 * P02.4 — Single-physical-sensor specialist evidence and STRICT conditional
 * negative evidence; no arbitrary two-sensor suppression and no chemical
 * identification claim.
 *
 * Direction and observable magnitude are source qualifications, not gas labels.
 * Absence of a response cannot rule out a gas at unknown concentration. An
 * alleged negative requires a separate verified challenge/reference
 * concentration, a source-supported expectable signal above detection limit,
 * and actual valid acquisition with the correct heater and calibration.
 */
public final class SingleSensorEvidenceGate {
    private SingleSensorEvidenceGate() {}

    public enum Direction { RS_DECREASES, RS_INCREASES, EITHER }
    public enum Reason {
        POSITIVE_SINGLE_SPECIALIST_RESEARCH_ONLY,
        QUALIFIED_ABSENCE_IN_KNOWN_CHALLENGE,
        UNKNOWN_CHEMICAL_SOURCE_MAPPING, INVALID_OR_LIMITED_ACQUISITION,
        NO_AIR_REFERENCE, MODEL_SENSOR_OR_HEATER_MISMATCH, NO_DETECTION_LIMIT,
        SOURCE_NOT_QUALIFIED, RESPONSE_BELOW_DETECTION,
        RESPONSE_OPPOSITE_TO_SOURCE_DIRECTION,
        NO_INDEPENDENT_CHALLENGE, EXPECTED_RESPONSE_UNVERIFIED,
        EXPECTED_RESPONSE_BELOW_DETECTION, INTERFERING_SIGNAL_POSSIBLE
    }

    public static final class SensitivityModel {
        public final long canonicalChemicalUid;
        public final String physicalSensorUid, sensorModel, heaterPhase, sourceRoot;
        public final Direction expectedDirection;
        public final Double sourceDetectionLimitLogRatio;
        public final boolean sourceIdentityMapped, sourceSelectivityQualified;
        public SensitivityModel(long canonicalChemicalUid,String physicalSensorUid,
               String sensorModel,String heaterPhase,String sourceRoot,Direction direction,
               Double sourceDetectionLimitLogRatio,boolean sourceIdentityMapped,
               boolean sourceSelectivityQualified) {
            if(canonicalChemicalUid<=0||blank(physicalSensorUid)||blank(sensorModel)
                 ||blank(heaterPhase)||blank(sourceRoot)||direction==null)
                throw new IllegalArgumentException("source-qualified model identity required");
            this.canonicalChemicalUid=canonicalChemicalUid;
            this.physicalSensorUid=physicalSensorUid;this.sensorModel=sensorModel;
            this.heaterPhase=heaterPhase;this.sourceRoot=sourceRoot;
            this.expectedDirection=direction;
            if(sourceDetectionLimitLogRatio!=null &&
               (!Double.isFinite(sourceDetectionLimitLogRatio)||
                 sourceDetectionLimitLogRatio<=0))
                throw new IllegalArgumentException("source LOD must be finite positive");
            this.sourceDetectionLimitLogRatio=sourceDetectionLimitLogRatio;
            this.sourceIdentityMapped=sourceIdentityMapped;
            this.sourceSelectivityQualified=sourceSelectivityQualified;
        }
    }

    /** Hypothetical concentration must be supported by INDEPENDENT evidence. */
    public static final class Challenge {
        public final boolean independentReferenceVerified, expectedLogRatioVerified;
        public final Double referenceConcentrationPpm, expectedAbsLogResponse;
        public Challenge(boolean independentReferenceVerified,
              Double referenceConcentrationPpm, boolean expectedLogRatioVerified,
              Double expectedAbsLogResponse) {
            this.independentReferenceVerified=independentReferenceVerified;
            this.referenceConcentrationPpm=referenceConcentrationPpm;
            this.expectedLogRatioVerified=expectedLogRatioVerified;
            this.expectedAbsLogResponse=expectedAbsLogResponse;
        }
    }

    public static final class Observation {
        public final String physicalSensorUid,sensorModel,heaterPhase;
        public final Double measuredRsKohm,measuredAirRsKohm,baselineNoiseLogRatio;
        public final SensorQualityGate.Result acquisitionQuality;
        public Observation(String physicalSensorUid,String sensorModel,String heaterPhase,
               Double measuredRsKohm, Double measuredAirRsKohm,Double baselineNoiseLogRatio,
               SensorQualityGate.Result acquisitionQuality) {
            if(blank(physicalSensorUid)||blank(sensorModel)||blank(heaterPhase)||
                acquisitionQuality==null)
                throw new IllegalArgumentException("physical sensor and quality required");
            this.physicalSensorUid=physicalSensorUid;this.sensorModel=sensorModel;
            this.heaterPhase=heaterPhase;this.measuredRsKohm=measuredRsKohm;
            this.measuredAirRsKohm=measuredAirRsKohm;
            this.baselineNoiseLogRatio=baselineNoiseLogRatio;
            this.acquisitionQuality=acquisitionQuality;
        }
    }

    public static final class Decision {
        public final CoreContracts.EvidencePolarity polarity;
        public final Reason reason;
        public final Long canonicalChemicalUid;
        public final String physicalSensorUid,sourceRoot;
        public final Double measuredLogAirRatio,decisionThresholdLogRatio;
        /** Always false; research indication is not an identity/ppm certification. */
        public final boolean confirmedChemicalIdentity=false,certifiedPpm=false;
        private Decision(CoreContracts.EvidencePolarity polarity,Reason reason,
              SensitivityModel model,Double measured,Double threshold) {
            this.polarity=polarity;this.reason=reason;
            this.canonicalChemicalUid=model==null?null:model.canonicalChemicalUid;
            this.physicalSensorUid=model==null?"":model.physicalSensorUid;
            this.sourceRoot=model==null?"":model.sourceRoot;
            this.measuredLogAirRatio=measured;
            this.decisionThresholdLogRatio=threshold;
        }
    }

    public static Decision evaluate(SensitivityModel model,Observation obs,Challenge challenge) {
        if(model==null||obs==null)
            throw new IllegalArgumentException("model and physical reading required");
        if(!model.sourceIdentityMapped)
            return answer(CoreContracts.EvidencePolarity.NOT_EVALUABLE,
                Reason.UNKNOWN_CHEMICAL_SOURCE_MAPPING,model,null,null);
        if(!model.sensorModel.equalsIgnoreCase(obs.sensorModel)||
            !model.physicalSensorUid.equals(obs.physicalSensorUid)||
            !model.heaterPhase.equals(obs.heaterPhase))
            return answer(CoreContracts.EvidencePolarity.NOT_EVALUABLE,
                Reason.MODEL_SENSOR_OR_HEATER_MISMATCH,model,null,null);
        if(!obs.acquisitionQuality.sourceCurveEligible)
            return answer(CoreContracts.EvidencePolarity.NOT_EVALUABLE,
                Reason.INVALID_OR_LIMITED_ACQUISITION,model,null,null);
        if(!positive(obs.measuredRsKohm)||!positive(obs.measuredAirRsKohm))
            return answer(CoreContracts.EvidencePolarity.NOT_EVALUABLE,
                Reason.NO_AIR_REFERENCE,model,null,null);
        if(!positive(model.sourceDetectionLimitLogRatio)||
            !positive(obs.baselineNoiseLogRatio))
            return answer(CoreContracts.EvidencePolarity.NOT_EVALUABLE,
                Reason.NO_DETECTION_LIMIT,model,null,null);
        double signal=Math.log(obs.measuredRsKohm/obs.measuredAirRsKohm);
        double detection=Math.max(model.sourceDetectionLimitLogRatio,
                                   3.0*obs.baselineNoiseLogRatio);
        if(!Double.isFinite(signal)||!Double.isFinite(detection)||detection<=0)
            return answer(CoreContracts.EvidencePolarity.NOT_EVALUABLE,
                Reason.NO_DETECTION_LIMIT,model,null,null);
        // A source label with no qualified selectivity cannot create molecular identity.
        if(!model.sourceSelectivityQualified)
            return answer(CoreContracts.EvidencePolarity.AMBIGUOUS,
                Reason.SOURCE_NOT_QUALIFIED,model,signal,detection);
        boolean exceeds=Math.abs(signal)>=detection;
        boolean directionMatches=matches(signal,model.expectedDirection);
        if(exceeds&&directionMatches)
            return answer(CoreContracts.EvidencePolarity.POSITIVE,
                Reason.POSITIVE_SINGLE_SPECIALIST_RESEARCH_ONLY,model,signal,detection);
        if(exceeds)
            return answer(CoreContracts.EvidencePolarity.AMBIGUOUS,
                Reason.RESPONSE_OPPOSITE_TO_SOURCE_DIRECTION,model,signal,detection);
        // No/weak response is NOT negative chemical evidence by default.
        if(challenge==null||!challenge.independentReferenceVerified||
            !positive(challenge.referenceConcentrationPpm))
            return answer(CoreContracts.EvidencePolarity.AMBIGUOUS,
                Reason.NO_INDEPENDENT_CHALLENGE,model,signal,detection);
        if(!challenge.expectedLogRatioVerified||!positive(challenge.expectedAbsLogResponse))
            return answer(CoreContracts.EvidencePolarity.NOT_EVALUABLE,
                Reason.EXPECTED_RESPONSE_UNVERIFIED,model,signal,detection);
        // Conservative independent source-margin: expected >= 2x empirical LOD.
        if(challenge.expectedAbsLogResponse<2.0*detection)
            return answer(CoreContracts.EvidencePolarity.AMBIGUOUS,
                Reason.EXPECTED_RESPONSE_BELOW_DETECTION,model,signal,detection);
        return answer(CoreContracts.EvidencePolarity.CONDITIONAL_NEGATIVE,
            Reason.QUALIFIED_ABSENCE_IN_KNOWN_CHALLENGE,model,signal,detection);
    }

    private static boolean matches(double signal,Direction direction) {
        return direction==Direction.EITHER ||
            (direction==Direction.RS_DECREASES&&signal<0) ||
            (direction==Direction.RS_INCREASES&&signal>0);
    }
    private static boolean positive(Double value) {
        return value!=null&&Double.isFinite(value)&&value>0;
    }
    private static boolean blank(String s) {return s==null||s.trim().isEmpty();}
    private static Decision answer(CoreContracts.EvidencePolarity polarity,
          Reason reason,SensitivityModel model,Double signal,Double threshold) {
        return new Decision(polarity,reason,model,signal,threshold);
    }
}
