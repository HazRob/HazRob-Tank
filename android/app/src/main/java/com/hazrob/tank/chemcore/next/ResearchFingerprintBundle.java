package com.hazrob.tank.chemcore.next;

import java.util.*;

/**
 * Source-constrained research bundle of diverse MQ fingerprints.
 *
 * A single physical source experiment may provide many sensor-channel,
 * time-window, concentration and algorithm variants.  All those variants are
 * correlated within the same source root and MUST contribute at most ONE
 * independent research evidence root.  "Bigger bundle" never means extra
 * independent votes, source-certified ppm or chemical identity.
 *
 * A variant distance cutoff must be learned on source CALIBRATION groups
 * independent of the source-training observations (not using held-out test
 * rows).  Missing cutoffs or physical-device matching => abstention. No
 * implicit 8-channel fill-ins when the original experiment measured fewer.
 */
public final class ResearchFingerprintBundle {
  private ResearchFingerprintBundle(){}
  public static final String VERSION="chemcore-next-diverse-fingerprint-bundle/0.1-g1";
  public enum Status {
    RESEARCH_SOURCE_BUNDLE_MATCH,NOT_EVALUABLE,PARTIAL_BUNDLE,
    SOURCE_DOMAIN_MISMATCH,NO_INDEPENDENT_CALIBRATION_THRESHOLD,
    INVALID_PHYSICAL_SENSOR,UNQUALIFIED_SOURCE_VARIANT,
    HIGH_SOURCE_RESIDUAL
  }
  public static final class Variant {
    public final FingerprintExpert.Signature sourceSignature;
    public final FingerprintExpert.Observation observed;
    public final FingerprintExpert.Qualification qualification;
    public final String sensorUid,featureFamily,techniqueId;
    public final Double independentlyCalibratedMaximumDistance;
    public final boolean sourceThresholdCalibrationVerified;
    public final Double originalSourceConcentrationPpb;
    public Variant(FingerprintExpert.Signature signature,
        FingerprintExpert.Observation observed,
        FingerprintExpert.Qualification sourceQualification,
        String sensorUid,String featureFamily,String techniqueId,
        Double calibratedMaxDistance,boolean thresholdVerified,
        Double sourceConcentrationPpb) {
      if(signature==null||observed==null||sourceQualification==null
          ||missing(sensorUid)||missing(featureFamily)||missing(techniqueId))
        throw new IllegalArgumentException("complete source variant fields required");
      if(calibratedMaxDistance!=null&&
         (!Double.isFinite(calibratedMaxDistance)||calibratedMaxDistance<=0))
        throw new IllegalArgumentException("source threshold must be positive finite");
      if(sourceConcentrationPpb!=null&&
         (!Double.isFinite(sourceConcentrationPpb)||sourceConcentrationPpb<=0))
        throw new IllegalArgumentException("source reference concentration invalid");
      this.sourceSignature=signature;this.observed=observed;
      qualification=sourceQualification;this.sensorUid=sensorUid;
      this.featureFamily=featureFamily;this.techniqueId=techniqueId;
      independentlyCalibratedMaximumDistance=calibratedMaxDistance;
      sourceThresholdCalibrationVerified=thresholdVerified;
      originalSourceConcentrationPpb=sourceConcentrationPpb;
    }
  }
  public static final class Bundle {
    public final String bundleUid,sourceRoot;
    public final long canonicalChemicalUid;
    public final List<Variant> variants;
    public final int minMatchedVariants,minDistinctSensorChannels,minDistinctFeatureFamilies;
    public Bundle(String uid,String root,long chemicalUid,List<Variant> variants,
                  int matchedMin,int channelsMin,int familiesMin) {
      if(missing(uid)||missing(root)||chemicalUid<=0||variants==null||variants.isEmpty()
         ||matchedMin<1||channelsMin<1||familiesMin<1||
         matchedMin>variants.size()||channelsMin>variants.size()||
         familiesMin>variants.size())
        throw new IllegalArgumentException("bundle requires explicit source/coverage");
      this.bundleUid=uid;sourceRoot=root;canonicalChemicalUid=chemicalUid;
      this.variants=List.copyOf(variants);
      minMatchedVariants=matchedMin;minDistinctSensorChannels=channelsMin;
      minDistinctFeatureFamilies=familiesMin;
      Set<String> variantsIds=new HashSet<>();
      for(Variant v:this.variants) {
        if(v==null||!root.equals(v.sourceSignature.sourceRoot)
           ||v.sourceSignature.canonicalChemicalUid==null
           ||v.sourceSignature.canonicalChemicalUid!=chemicalUid)
          throw new IllegalArgumentException("bundle source root and chemical UID must match every variant");
        if(!variantsIds.add(v.techniqueId))
          throw new IllegalArgumentException("duplicate technique in one source bundle");
      }
    }
  }
  public static final class Score {
    public final Status status;
    public final String bundleUid,sourceRoot;
    public final long canonicalChemicalUid;
    public final int sourceVariants,matchedVariants,distinctSensorChannels,
        distinctFeatureFamilies;
    public final List<String> matchedSensors,matchedFeatureFamilies,diagnostics;
    public final boolean isEligibleResearchHypothesis,gasIdentityConfirmed,
        ppmCertified,fieldSafetyAlarmAuthorized,physicalTankG3;
    private Score(Bundle b,Status status,int n,Set<String> sensors,Set<String> families,
                  List<String> diagnostics) {
      this.status=status;bundleUid=b.bundleUid;sourceRoot=b.sourceRoot;
      canonicalChemicalUid=b.canonicalChemicalUid;sourceVariants=b.variants.size();
      matchedVariants=n;distinctSensorChannels=sensors.size();
      distinctFeatureFamilies=families.size();
      matchedSensors=List.copyOf(sensors);matchedFeatureFamilies=List.copyOf(families);
      this.diagnostics=List.copyOf(diagnostics);
      isEligibleResearchHypothesis=status==Status.RESEARCH_SOURCE_BUNDLE_MATCH;
      gasIdentityConfirmed=false;ppmCertified=false;fieldSafetyAlarmAuthorized=false;
      physicalTankG3=false;
    }
  }
  private static boolean missing(String value){return value==null||value.isBlank();}

  /**
   * External benchmark adapter for original MQ 10-bit ADC-only recordings.
   * Never invent Rs, RL, R_air, R0, heater phase or certified ppm from an ADC.
   * This form cannot be used to perform any Tank physical or source-R0
   * verification; explicit source-root and source-hardware checks apply.
   */
  public static final class SourceAdcCapture {
    public final String sourceRoot,originalSourceRecordUid;
    public final Map<String,Integer> originalAdcBySensorUid;
    public final boolean originalSourceRecordVerified;
    public SourceAdcCapture(String root,String recordUid,Map<String,Integer> adc,
                            boolean physicallyRecordedSourceVerified) {
      if(missing(root)||missing(recordUid)||adc==null)
        throw new IllegalArgumentException("original ADC source provenance required");
      Map<String,Integer> copy=new TreeMap<>();
      for(var e:adc.entrySet()){
        if(missing(e.getKey())||e.getValue()==null||
           e.getValue()<0||e.getValue()>1023)
          throw new IllegalArgumentException("source 10bit ADC must be actual bounded reading");
        copy.put(e.getKey(),e.getValue());
      }
      sourceRoot=root;originalSourceRecordUid=recordUid;
      originalAdcBySensorUid=Collections.unmodifiableMap(copy);
      originalSourceRecordVerified=physicallyRecordedSourceVerified;
    }
  }
  public static Score evaluateSourceOnly(Bundle bundle,SourceAdcCapture capture){
    if(bundle==null||capture==null)throw new IllegalArgumentException("bundle and ADC source");
    if(!bundle.sourceRoot.equals(capture.sourceRoot)||!capture.originalSourceRecordVerified)
      return new Score(bundle,Status.SOURCE_DOMAIN_MISMATCH,
        0,new TreeSet<>(),new TreeSet<>(),List.of("UNQUALIFIED_SOURCE_HARDWARE_ROOT"));
    Map<String,Boolean> good=new HashMap<>();
    for(var v:bundle.variants)good.put(v.sensorUid,
      capture.originalAdcBySensorUid.containsKey(v.sensorUid));
    return scoreSourceVariants(bundle,good,true);
  }

  /** Bundle passed means *one* source-research hypothesis, never one per MQ. */
  public static Score evaluate(Bundle b,CoreContracts.DeviceProfile device,
                               CoreContracts.Snapshot snapshot) {
    if(b==null||device==null||snapshot==null)
      throw new IllegalArgumentException("bundle, device and capture required");
    if(!device.deviceUid.equals(snapshot.deviceUid))
      throw new IllegalArgumentException("device mismatched source capture");
    Map<String,CoreContracts.SensorInstance> sensors=new HashMap<>();
    for(var s:device.installedSensors)sensors.put(s.uid,s);
    Map<String,CoreContracts.Reading> readings=new HashMap<>();
    for(var r:snapshot.readings)readings.put(r.sensorUid,r);
    Map<String,Boolean> physicallyValid=new HashMap<>();
    for(Variant v:b.variants) {
      CoreContracts.SensorInstance instance=sensors.get(v.sensorUid);
      CoreContracts.Reading r=readings.get(v.sensorUid);
      physicallyValid.put(v.sensorUid,instance!=null&&r!=null
        &&r.quality==CoreContracts.SampleQuality.VALID
        &&instance.model.equals(v.sourceSignature.sensorModel));
    }
    return scoreSourceVariants(b,physicallyValid,snapshot.mqRelayOn);
  }
  private static Score scoreSourceVariants(Bundle b,Map<String,Boolean> physicallyValid,
                                           boolean powered) {
    Set<String> acceptedSensors=new TreeSet<>(),acceptedFamilies=new TreeSet<>();
    Set<String> acceptedVariantClusters=new HashSet<>();
    List<String> diag=new ArrayList<>();
    int matched=0;
    Status firstFailure=null;
    if(!powered)return new Score(b,Status.NOT_EVALUABLE,
      0,acceptedSensors,acceptedFamilies,List.of("MQ_RELAY_NOT_ON"));
    for(Variant v:b.variants){
      if(!Boolean.TRUE.equals(physicallyValid.get(v.sensorUid))) {
        if(firstFailure==null)firstFailure=Status.INVALID_PHYSICAL_SENSOR;
        diag.add(v.techniqueId+":MISSING_OR_INVALID_MQ_CHANNEL");
        continue;
      }
      if(!v.sourceThresholdCalibrationVerified ||
          v.independentlyCalibratedMaximumDistance==null) {
        if(firstFailure==null)firstFailure=Status.NO_INDEPENDENT_CALIBRATION_THRESHOLD;
        diag.add(v.techniqueId+":NO_SOURCE_GROUPED_DISTANCE_THRESHOLD");
        continue;
      }
      FingerprintExpert.Output out=FingerprintExpert.compare(
        v.sourceSignature,v.observed,v.qualification);
      if(out.status!=FingerprintExpert.Status.RESEARCH_RESIDUAL_ONLY) {
        if(firstFailure==null)firstFailure=Status.UNQUALIFIED_SOURCE_VARIANT;
        diag.add(v.techniqueId+":"+out.status);
        continue;
      }
      // Multiple concentration prototypes from the same physical channel and
      // family may be evaluated but are ONE family, not independent votes.
      final String familyKey=v.sensorUid+"|"+v.featureFamily;
      if(out.researchNormalizedRmsDistance>
          v.independentlyCalibratedMaximumDistance) {
        if(firstFailure==null)firstFailure=Status.HIGH_SOURCE_RESIDUAL;
        diag.add(v.techniqueId+":OUTSIDE_GROUPED_SOURCE_DISTANCE_THRESHOLD");
        continue;
      }
      if(!acceptedVariantClusters.add(familyKey)){
        diag.add(v.techniqueId+":CORRELATED_CONCENTRATION_TEMPLATE_NOT_EXTRA_VOTE");
        continue;
      }
      matched++;acceptedSensors.add(v.sensorUid);acceptedFamilies.add(v.featureFamily);
      diag.add(v.techniqueId+":SOURCE_CALIBRATED_FAMILY_MATCH");
    }
    final Status s;
    if(matched>=b.minMatchedVariants
       &&acceptedSensors.size()>=b.minDistinctSensorChannels
       &&acceptedFamilies.size()>=b.minDistinctFeatureFamilies)s=Status.RESEARCH_SOURCE_BUNDLE_MATCH;
    else if(matched>0)s=Status.PARTIAL_BUNDLE;
    else s=firstFailure==null?Status.NOT_EVALUABLE:firstFailure;
    return new Score(b,s,matched,acceptedSensors,acceptedFamilies,diag);
  }
}
