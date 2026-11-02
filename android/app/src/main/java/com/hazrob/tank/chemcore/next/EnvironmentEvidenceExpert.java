package com.hazrob.tank.chemcore.next;

/**
 * P04.4 source-locked environmental 3D research transform.
 *
 * Source physics: log(signal_at_T_RH) =
 * log(signal_at_reference_T_RH) + betaT*(T-Tref) + betaRH*(RH-RHref).
 * Hence correction to source reference is exp(-betaT*dT-betaRH*dRH).
 * Coefficients and domain MUST come from a specified, device-matched
 * experimental source. No default manufacturer correction is assumed.
 */
public final class EnvironmentEvidenceExpert {
    private EnvironmentEvidenceExpert() {}
    public static final String VERSION="chemcore-next-environment-expert/0.1-g1";
    public enum Status {
        RESEARCH_TRANSFORM_ONLY, NOT_EVALUABLE, SOURCE_UNQUALIFIED,
        SENSOR_OR_PHASE_MISMATCH, INCOMPLETE_ENVIRONMENT,
        OUTSIDE_SOURCE_DOMAIN, INVALID_SAMPLE, NUMERIC_FAILURE
    }
    public static final class SourceModel {
        public final String sourceRoot,sourceModelUid,sensorModel,heaterPhase;
        public final double referenceTemperatureC,referenceRelativeHumidityPercent;
        public final double sourceBetaPerDegreeC,sourceBetaPerRhPercent;
        public final double minTemperatureC,maxTemperatureC,minRhPercent,maxRhPercent;
        public SourceModel(String sourceRoot,String modelId,String sensorModel,String heater,
                           double tRef,double rhRef,double betaT,double betaRh,
                           double tMin,double tMax,double rhMin,double rhMax) {
            this.sourceRoot=required(sourceRoot);
            this.sourceModelUid=required(modelId);this.sensorModel=required(sensorModel);
            this.heaterPhase=required(heater);
            for(double v:new double[]{tRef,rhRef,betaT,betaRh,tMin,tMax,rhMin,rhMax})
                if(!Double.isFinite(v))throw new IllegalArgumentException("source numeric metadata must be finite");
            if(tMin>=tMax || rhMin<0 || rhMax>100 || rhMin>=rhMax
               || tRef<tMin || tRef>tMax || rhRef<rhMin || rhRef>rhMax)
                throw new IllegalArgumentException("reference point and domain must be source bounded");
            referenceTemperatureC=tRef;referenceRelativeHumidityPercent=rhRef;
            sourceBetaPerDegreeC=betaT;sourceBetaPerRhPercent=betaRh;
            minTemperatureC=tMin;maxTemperatureC=tMax;
            minRhPercent=rhMin;maxRhPercent=rhMax;
        }
        private static String required(String value){
            if(value==null||value.isBlank())throw new IllegalArgumentException("source metadata required");
            return value.trim();
        }
    }
    public static final class Qualification {
        public final boolean sourceCoefficientsVerified,sourceDomainVerified;
        public final boolean measuredEnvironmentalChannelsVerified,deviceResponseModelMatched;
        public Qualification(boolean coeff,boolean domain,boolean env,boolean device) {
            sourceCoefficientsVerified=coeff;sourceDomainVerified=domain;
            measuredEnvironmentalChannelsVerified=env;deviceResponseModelMatched=device;
        }
        public boolean valid(){return sourceCoefficientsVerified&&sourceDomainVerified
            &&measuredEnvironmentalChannelsVerified&&deviceResponseModelMatched;}
        public static Qualification unverified(){return new Qualification(false,false,false,false);}
    }
    public static final class Output {
        public final Status status;
        public final String sourceRoot,sourceModelUid,snapshotUid,sensorUid;
        public final double[] sourceThreeDimensionalVector; // air ratio,T C,RH %
        public final Double correctedReferenceAirRatio;
        public final boolean concentrationAuthorized,gasIdentityConfirmed,g3Qualified;
        public final String reason;
        private Output(Status s,SourceModel m,CoreContracts.Snapshot snap,
                       CoreContracts.SensorInstance sensor,double[] features,
                       Double corrected,String reason){
            status=s;sourceRoot=m.sourceRoot;sourceModelUid=m.sourceModelUid;
            snapshotUid=snap==null?"":snap.uid;sensorUid=sensor==null?"":sensor.uid;
            sourceThreeDimensionalVector=features==null?null:features.clone();
            correctedReferenceAirRatio=corrected;this.reason=reason;
            concentrationAuthorized=false;gasIdentityConfirmed=false;g3Qualified=false;
        }
        public double[] vectorCopy(){
            return sourceThreeDimensionalVector==null?null:sourceThreeDimensionalVector.clone();
        }
    }
    private static Output reject(Status why,SourceModel m,CoreContracts.Snapshot s,
                                 CoreContracts.SensorInstance sensor) {
        return new Output(why,m,s,sensor,null,null,"source-locked abstention: "+why);
    }
    public static Output evaluate(SourceModel m,Qualification q,CoreContracts.SensorInstance sensor,
                                  CoreContracts.Snapshot snapshot) {
        if(m==null || q==null)throw new IllegalArgumentException("model and qualification required");
        if(!q.valid())return reject(Status.SOURCE_UNQUALIFIED,m,snapshot,sensor);
        if(sensor==null||!m.sensorModel.equals(sensor.model)
           ||!m.heaterPhase.equals(sensor.heaterProfile))
            return reject(Status.SENSOR_OR_PHASE_MISMATCH,m,snapshot,sensor);
        if(snapshot==null||!snapshot.mqRelayOn)
            return reject(Status.INVALID_SAMPLE,m,snapshot,sensor);
        if(snapshot.temperatureC==null||snapshot.relativeHumidityPercent==null)
            return reject(Status.INCOMPLETE_ENVIRONMENT,m,snapshot,sensor);
        final double t=snapshot.temperatureC,rh=snapshot.relativeHumidityPercent;
        if(t<m.minTemperatureC||t>m.maxTemperatureC||rh<m.minRhPercent||rh>m.maxRhPercent)
            return reject(Status.OUTSIDE_SOURCE_DOMAIN,m,snapshot,sensor);
        CoreContracts.Reading current=null;
        for(CoreContracts.Reading r:snapshot.readings) {
            if(sensor.uid.equals(r.sensorUid)){current=r;break;}
        }
        if(current==null||current.quality!=CoreContracts.SampleQuality.VALID
           ||!m.heaterPhase.equals(current.heaterPhase))
            return reject(Status.INVALID_SAMPLE,m,snapshot,sensor);
        final double original=current.airRatioOrNaN();
        if(!Double.isFinite(original)||original<=0)
            return reject(Status.INVALID_SAMPLE,m,snapshot,sensor);
        final double logCorrection= -m.sourceBetaPerDegreeC*(t-m.referenceTemperatureC)
            -m.sourceBetaPerRhPercent*(rh-m.referenceRelativeHumidityPercent);
        if(!Double.isFinite(logCorrection))
            return reject(Status.NUMERIC_FAILURE,m,snapshot,sensor);
        final double corrected=original*Math.exp(logCorrection);
        if(!Double.isFinite(corrected)||corrected<=0)
            return reject(Status.NUMERIC_FAILURE,m,snapshot,sensor);
        return new Output(Status.RESEARCH_TRANSFORM_ONLY,m,snapshot,sensor,
            new double[]{original,t,rh},corrected,
            "3D source-space correction only; no ppm, gas identity or independent vote");
    }
}
