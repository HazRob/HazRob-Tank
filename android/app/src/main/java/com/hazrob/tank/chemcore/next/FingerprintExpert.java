package com.hazrob.tank.chemcore.next;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ChemCore Next P04.2: source-space-aware multichannel fingerprint residuals.
 *
 * All observations remain research distances, NOT independent gas identities.
 * A 3-D temporal UCI prototype and an 8-D MQDataScience class signature cannot
 * be compared or assumed transferable to Tank simply because MQ appears in both.
 */
public final class FingerprintExpert {
    private FingerprintExpert() {}
    public static final String VERSION="chemcore-next-fingerprint-expert/0.1-g1";

    public enum Status {
        RESEARCH_RESIDUAL_ONLY, NOT_EVALUABLE, SPACE_MISMATCH,
        SENSOR_MODEL_MISMATCH, SOURCE_TRANSFORM_UNQUALIFIED,
        DEVICE_INSTANCE_NOT_MATCHED, INVALID_SOURCE_SCALES,
        INVALID_OBSERVATION, INSUFFICIENT_PHYSICAL_SUBSET,
        UNRECOGNIZED_FEATURE, NUMERICAL_FAILURE
    }

    private static String required(String s,String why) {
        if(s==null||s.trim().isEmpty())
            throw new IllegalArgumentException(why+" is required");
        return s.trim();
    }

    public static final class Signature {
        public final String id,sourceRoot,featureSpace,sensorModel;
        public final Long canonicalChemicalUid;
        public final int minimumComparableCoordinates;
        private final List<String> keys;
        private final List<Double> means, scales;

        public Signature(String id,String sourceRoot,String featureSpace,
                         String sensorModel,Long chemicalUid,
                         List<String> keys,List<Double> means,List<Double> scales,
                         int minFeatures) {
            this.id=required(id,"fingerprint ID");
            this.sourceRoot=required(sourceRoot,"independent source root");
            this.featureSpace=required(featureSpace,"feature space");
            this.sensorModel=required(sensorModel,"source sensor model");
            if(chemicalUid!=null&&chemicalUid<=0)
                throw new IllegalArgumentException("chemical UID must be positive");
            this.canonicalChemicalUid=chemicalUid;
            if(keys==null||means==null||scales==null||keys.size()!=means.size()
                ||keys.size()!=scales.size()||keys.isEmpty())
                throw new IllegalArgumentException("source prototype coordinates must align");
            if(minFeatures<1||minFeatures>keys.size())
                throw new IllegalArgumentException("invalid minimum comparable coordinates");
            Set<String> known=new HashSet<>();
            List<String> names=new ArrayList<>();
            for(String key:keys) {
                String clean=required(key,"feature key");
                if(!known.add(clean))
                    throw new IllegalArgumentException("duplicate physical/temporal feature key");
                names.add(clean);
            }
            for(Double mean:means)
                if(mean!=null&&!Double.isFinite(mean))
                    throw new IllegalArgumentException("nonfinite reference mean");
            // Preserve null/zero/negative *source uncertainty* for scientific
            // audit; evaluation refuses it rather than fabricating a positive SD.
            for(Double scale:scales)
                if(scale!=null&&!Double.isFinite(scale))
                    throw new IllegalArgumentException("nonfinite source scale");
            this.keys=Collections.unmodifiableList(names);
            this.means=Collections.unmodifiableList(new ArrayList<>(means));
            this.scales=Collections.unmodifiableList(new ArrayList<>(scales));
            this.minimumComparableCoordinates=minFeatures;
        }
        public int dimension(){return keys.size();}
        public List<String> featureKeys(){return keys;}
        public List<Double> referenceMeans(){return means;}
        public List<Double> sourceScales(){return scales;}
    }

    public static final class Observation {
        public final String sourceSpace,sensorModel,physicalObservationUid;
        public final boolean sampleQualityValid;
        public final Map<String,Double> availableCoordinates;
        public Observation(String sourceSpace,String sensorModel,String observationUid,
                           Map<String,Double> observed,boolean qualityValid) {
            this.sourceSpace=required(sourceSpace,"observation feature space");
            this.sensorModel=required(sensorModel,"observed sensor model");
            this.physicalObservationUid=required(observationUid,"observation UID");
            if(observed==null)throw new IllegalArgumentException("observation map required");
            Map<String,Double> copy=new HashMap<>();
            for(Map.Entry<String,Double> entry:observed.entrySet()) {
                String key=required(entry.getKey(),"observation key");
                Double value=entry.getValue();
                if(value==null||!Double.isFinite(value))
                    throw new IllegalArgumentException("observed coordinate must be finite");
                if(copy.put(key,value)!=null)
                    throw new IllegalArgumentException("duplicate feature key");
            }
            this.availableCoordinates=Collections.unmodifiableMap(copy);
            this.sampleQualityValid=qualityValid;
        }
    }

    public static final class Qualification {
        public final boolean sourceFeatureTransformVerified, physicalDeviceMatched;
        public Qualification(boolean sourceFeatureTransformVerified,
                             boolean physicalDeviceMatched) {
            this.sourceFeatureTransformVerified=sourceFeatureTransformVerified;
            this.physicalDeviceMatched=physicalDeviceMatched;
        }
        public static Qualification unverified(){return new Qualification(false,false);}
    }

    public static final class Output {
        public final Status status;
        public final String fingerprintId,sourceRoot,physicalObservationUid;
        public final Long canonicalChemicalUid;
        public final int matchedCoordinates,sourceCoordinates;
        public final Double researchNormalizedRmsDistance;
        public final boolean partialSubset;
        public final boolean gasIdentityConfirmed,ppmAuthorized,g3PhysicallyQualified;
        public final String reason;
        private Output(Signature f,Observation o,Status status,int matched,Double distance,String reason) {
            fingerprintId=f.id;sourceRoot=f.sourceRoot;
            canonicalChemicalUid=f.canonicalChemicalUid;
            physicalObservationUid=o==null?"":o.physicalObservationUid;
            this.status=status;matchedCoordinates=matched;sourceCoordinates=f.dimension();
            researchNormalizedRmsDistance=distance;
            partialSubset=matched<f.dimension();
            gasIdentityConfirmed=false;ppmAuthorized=false;g3PhysicallyQualified=false;
            this.reason=reason;
        }
    }

    public static Output compare(Signature reference,Observation current,Qualification q) {
        if(reference==null||q==null)
            throw new IllegalArgumentException("source reference and qualification required");
        if(current==null||!current.sampleQualityValid)
            return out(reference,current,Status.INVALID_OBSERVATION,0,null,
                       "observation is missing, stale or invalid");
        if(!reference.featureSpace.equals(current.sourceSpace))
            return out(reference,current,Status.SPACE_MISMATCH,0,null,
                       "different feature space; cross-space distances are invalid");
        if(!reference.sensorModel.equals(current.sensorModel))
            return out(reference,current,Status.SENSOR_MODEL_MISMATCH,0,null,
                       "sensor model does not match source feature basis");
        if(!q.sourceFeatureTransformVerified)
            return out(reference,current,Status.SOURCE_TRANSFORM_UNQUALIFIED,0,null,
                       "uncertain response transforms / temporal window definitions");
        if(!q.physicalDeviceMatched)
            return out(reference,current,Status.DEVICE_INSTANCE_NOT_MATCHED,0,null,
                       "no independently qualified instance/baseline transform");
        for(Double v:reference.means)
            if(v==null) return out(reference,current,Status.INVALID_SOURCE_SCALES,
                                  0,null,"reference contains absent coordinates");
        for(Double sd:reference.scales)
            if(sd==null||sd<=0||!Double.isFinite(sd))
                return out(reference,current,Status.INVALID_SOURCE_SCALES,0,null,
                           "source missing/nonpositive scale cannot be used as variance");
        Set<String> expected=new HashSet<>(reference.keys);
        for(String key:current.availableCoordinates.keySet())
            if(!expected.contains(key))
                return out(reference,current,Status.UNRECOGNIZED_FEATURE,0,null,
                           "observation contains an unsupported channel/feature");
        int seen=0;
        double total=0;
        for(int i=0;i<reference.keys.size();i++) {
            Double v=current.availableCoordinates.get(reference.keys.get(i));
            if(v==null)continue;
            double residual=(v-reference.means.get(i))/reference.scales.get(i);
            if(!Double.isFinite(residual)||!Double.isFinite(residual*residual))
                return out(reference,current,Status.NUMERICAL_FAILURE,seen,null,
                           "overflow in source standardized residual");
            total+=residual*residual;
            seen++;
        }
        if(seen<reference.minimumComparableCoordinates)
            return out(reference,current,Status.INSUFFICIENT_PHYSICAL_SUBSET,seen,null,
                       "one channel cannot replace an unqualified multichannel signature");
        if(!Double.isFinite(total))
            return out(reference,current,Status.NUMERICAL_FAILURE,seen,null,
                       "sum of squared reference residuals overflowed");
        return out(reference,current,Status.RESEARCH_RESIDUAL_ONLY,seen,
                   Math.sqrt(total/seen),"research distance only; not class probability");
    }
    private static Output out(Signature f,Observation o,Status status,int count,
                              Double value,String reason) {
        return new Output(f,o,status,count,value,reason);
    }
}
