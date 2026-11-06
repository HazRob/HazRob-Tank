package com.hazrob.tank.chemcore.next;

import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Single Java17 core entry point independent of Android/V6.
 * P06 integration: immutable capture -> compatible source experts ->
 * source-root ledger -> scientific candidate generator -> arbitration ->
 * immutable research result. Expert outputs without a separately qualified
 * source decision threshold remain diagnostics and NEVER become "gas found".
 * Does not pretend the 80 techniques were all activated or physically G3 tested.
 */
public final class ChemCoreNext {
  private ChemCoreNext() {}
  public static final String VERSION="chemcore-next-research-core/0.3-g1-source-trace";

  public static final class CurveBinding {
    public final SourceCurveExpert.Curve curve;
    public final String physicalSensorUid;
    public final SourceCurveExpert.Qualification qualification;
    /** Optional source-provenance log10 response sigma; NOT physical Tank accuracy. */
    public final Double sourceLog10Sigma;
    public CurveBinding(SourceCurveExpert.Curve model,String sensorUid,
                        SourceCurveExpert.Qualification q){
      this(model,sensorUid,q,null);
    }
    public CurveBinding(SourceCurveExpert.Curve model,String sensorUid,
                        SourceCurveExpert.Qualification q,Double sigma){
      if(model==null||sensorUid==null||sensorUid.isBlank()||q==null)
        throw new IllegalArgumentException("source curve binding provenance required");
      if(sigma!=null&&(!Double.isFinite(sigma)||sigma<=0))
        throw new IllegalArgumentException("source sigma must be finite positive");
      curve=model;physicalSensorUid=sensorUid;qualification=q;sourceLog10Sigma=sigma;
    }
  }
  public static final class FingerprintBinding {
    public final FingerprintExpert.Signature reference;
    public final FingerprintExpert.Observation observation;
    public final FingerprintExpert.Qualification qualification;
    public final String physicalSensorUid;
    /** Threshold trained on source independent calibration observations. */
    public final Double maxSourceNormalizedRmsDistance;
    public final boolean independentThresholdCalibrationVerified;
    public FingerprintBinding(FingerprintExpert.Signature s,
         FingerprintExpert.Observation x,FingerprintExpert.Qualification q,
         String sensorUid,Double sourceDistance,boolean thresholdVerified){
      if(s==null||x==null||q==null||sensorUid==null||sensorUid.isBlank())
        throw new IllegalArgumentException("fingerprint source and sensor binding required");
      if(sourceDistance!=null&&(!Double.isFinite(sourceDistance)||sourceDistance<=0))
        throw new IllegalArgumentException("source distance must be positive");
      reference=s;observation=x;qualification=q;physicalSensorUid=sensorUid;
      maxSourceNormalizedRmsDistance=sourceDistance;
      independentThresholdCalibrationVerified=thresholdVerified;
    }
  }
  public static final class Input {
    public final CoreContracts.DeviceProfile device;
    public final CoreContracts.Snapshot current;
    public final String sourceSnapshotHash;
    public final List<ResearchCandidateGenerator.Material> catalog;
    public final List<CurveBinding> curves;
    public final List<FingerprintBinding> fingerprints;
    public final List<ResearchFingerprintBundle.Bundle> bundles;
    public final int displayLimit;
    public Input(CoreContracts.DeviceProfile device,CoreContracts.Snapshot current,
                 String snapshotHash,
                 List<ResearchCandidateGenerator.Material> catalog,
                 List<CurveBinding> curves,List<FingerprintBinding> fingerprints,
                 int maxDisplay){
      this(device,current,snapshotHash,catalog,curves,fingerprints,List.of(),maxDisplay);
    }
    public Input(CoreContracts.DeviceProfile device,CoreContracts.Snapshot current,
                 String snapshotHash,
                 List<ResearchCandidateGenerator.Material> catalog,
                 List<CurveBinding> curves,List<FingerprintBinding> fingerprints,
                 List<ResearchFingerprintBundle.Bundle> bundledFingerprints,
                 int maxDisplay){
      if(device==null||current==null||snapshotHash==null||snapshotHash.isBlank()
          ||catalog==null||curves==null||fingerprints==null||bundledFingerprints==null||maxDisplay<0)
        throw new IllegalArgumentException("complete immutable source context required");
      this.device=device;this.current=current;sourceSnapshotHash=snapshotHash;
      this.catalog=List.copyOf(catalog);this.curves=List.copyOf(curves);
      this.fingerprints=List.copyOf(fingerprints);
      this.bundles=List.copyOf(bundledFingerprints);displayLimit=maxDisplay;
    }
  }
  public static final class Output {
    public final CoreContracts.Result research;
    public final NextResearchDecisionPipeline.Decision arbitration;
    public final EvidenceConflictGraph.Graph graph;
    /** Unbounded per-source hypothesis rankings incl. rejected/tied models. */
    public final List<SourceRootCoherence.RootReport> sourceCoherence;
    /** Every called expert/decision stage; retains original labels and rejects. */
    public final List<NextAnalysisTrace.Event> executionTrace;
    public final List<String> sourceDiagnostics;
    public final int modelsInspected,positiveModels;
    public final boolean safetyAlarmAuthorized,ppmAuthorized,molecularIdentityConfirmed,g3Qualified;
    private Output(CoreContracts.Result result,NextResearchDecisionPipeline.Decision d,
                   EvidenceConflictGraph.Graph g,List<SourceRootCoherence.RootReport> rootFits,
                   List<NextAnalysisTrace.Event> events,
                   List<String> diagnostics,int inspected,int positive){
      research=result;arbitration=d;graph=g;sourceCoherence=List.copyOf(rootFits);
      executionTrace=NextAnalysisTrace.immutable(events);
      sourceDiagnostics=List.copyOf(diagnostics);
      modelsInspected=inspected;positiveModels=positive;
      safetyAlarmAuthorized=false;ppmAuthorized=false;molecularIdentityConfirmed=false;g3Qualified=false;
    }
  }
  public static Output analyze(Input input){
    if(input==null)throw new IllegalArgumentException("Next input missing");
    if(!input.device.deviceUid.equals(input.current.deviceUid))
      throw new IllegalArgumentException("snapshot device does not match physical profile");
    if(input.current.stableManifestHash.isBlank())
      throw new IllegalArgumentException("unversioned Stable source snapshot");
    // Reject stale or falsely attributed measurements before any inference.
    // The digest is an integrity fingerprint, not a cryptographic signature.
    if(!snapshotHash(input.current).equals(input.sourceSnapshotHash))
      throw new IllegalArgumentException("SNAPSHOT_HASH_MISMATCH source capture changed");
    Map<String,CoreContracts.SensorInstance> instances=new HashMap<>();
    for(var s:input.device.installedSensors)instances.put(s.uid,s);
    Map<String,CoreContracts.Reading> readings=new HashMap<>();
    for(var r:input.current.readings){
      if(!instances.containsKey(r.sensorUid))
        throw new IllegalArgumentException("reading not present on physical device");
      readings.put(r.sensorUid,r);
    }
    List<CoreContracts.EvidenceNode> raw=new ArrayList<>();
    List<ResearchCandidateGenerator.AssetLink> assets=new ArrayList<>();
    List<SourceRootCoherence.Sample> sourceCurveSamples=new ArrayList<>();
    List<String> reasons=new ArrayList<>();
    List<NextAnalysisTrace.Event> trace=new ArrayList<>();
    trace.add(new NextAnalysisTrace.Event("INPUT","CAPTURE",input.current.stableManifestHash,
        input.current.deviceUid,null,"HASH_VERIFIED",
        "SHA256 matches immutable snapshot; not a cryptographic signature",null));
    int count=0,positives=0;
    for(var b:input.curves){
      count++;
      if(NextResearchSourceQuarantine.blocked(b.curve.sourceRoot,b.curve.id)){
        reasons.add("SOURCE_TEMPORARILY_EXCLUDED_UCI1081_CURVE "+b.curve.id);
        trace.add(new NextAnalysisTrace.Event("CURVE",b.curve.id,b.curve.sourceRoot,
          b.physicalSensorUid,b.curve.canonicalChemicalUid,"QUARANTINED","UCI1081",null));
        continue;
      }
      var sensor=instances.get(b.physicalSensorUid);
      var reading=readings.get(b.physicalSensorUid);
      if(!input.current.mqRelayOn){
        reasons.add("SOURCE_CURVE_BLOCKED_RELAY_OFF "+b.curve.id);
        trace.add(new NextAnalysisTrace.Event("CURVE",b.curve.id,b.curve.sourceRoot,
          b.physicalSensorUid,b.curve.canonicalChemicalUid,"RELAY_OFF","NO_READING",null));
        continue;
      }
      var out=SourceCurveExpert.evaluate(b.curve,sensor,reading,b.qualification);
      trace.add(new NextAnalysisTrace.Event("CURVE",b.curve.id,b.curve.sourceRoot,
        b.physicalSensorUid,b.curve.canonicalChemicalUid,out.decision.name(),
        out.abstention.name(),out.researchOnlyPpm));
      // A valid single-source curve is a research HYPOTHESIS ONLY, not molecular identity.
      if(out.decision==SourceCurveExpert.Decision.RESEARCH_SOURCE_QUANTITATIVE_ESTIMATE
         &&b.curve.canonicalChemicalUid!=null){
        raw.add(new CoreContracts.EvidenceNode(b.curve.sourceRoot,"SOURCE_CURVE",
          b.physicalSensorUid,b.curve.canonicalChemicalUid,
          CoreContracts.EvidencePolarity.POSITIVE,
          "source single-gas curve research hypothesis; quantitative result non-reportable"));
        assets.add(new ResearchCandidateGenerator.AssetLink(
          b.curve.sourceRoot,b.curve.id,b.curve.canonicalChemicalUid,true));
        sourceCurveSamples.add(new SourceRootCoherence.Sample(
          b.curve,b.physicalSensorUid,reading.sourceRatioOrNaN(),b.sourceLog10Sigma));
        positives++;
      }else {
        reasons.add("CURVE_NOT_ELIGIBLE "+b.curve.id+" "+out.abstention);
      }
    }
    for(var b:input.fingerprints){
      count++;
      if(NextResearchSourceQuarantine.blocked(b.reference.sourceRoot,
           b.reference.id,b.reference.featureSpace)){
        reasons.add("SOURCE_TEMPORARILY_EXCLUDED_UCI1081_FINGERPRINT "+b.reference.id);
        trace.add(new NextAnalysisTrace.Event("FINGERPRINT",b.reference.id,
          b.reference.sourceRoot,b.physicalSensorUid,b.reference.canonicalChemicalUid,
          "QUARANTINED","UCI1081",null));
        continue;
      }
      if(!input.current.mqRelayOn || !instances.containsKey(b.physicalSensorUid)
         ||!readings.containsKey(b.physicalSensorUid)
         ||readings.get(b.physicalSensorUid).quality!=CoreContracts.SampleQuality.VALID) {
        reasons.add("FINGERPRINT_PHYSICAL_SENSOR_NOT_ELIGIBLE "+b.reference.id);
        trace.add(new NextAnalysisTrace.Event("FINGERPRINT",b.reference.id,
          b.reference.sourceRoot,b.physicalSensorUid,b.reference.canonicalChemicalUid,
          "NOT_EVALUABLE","INVALID_OR_MISSING_PHYSICAL_SENSOR",null));
        continue;
      }
      var output=FingerprintExpert.compare(b.reference,b.observation,b.qualification);
      trace.add(new NextAnalysisTrace.Event("FINGERPRINT",b.reference.id,
        b.reference.sourceRoot,b.physicalSensorUid,b.reference.canonicalChemicalUid,
        output.status.name(),!b.independentThresholdCalibrationVerified?
        "SOURCE_THRESHOLD_NOT_INDEPENDENTLY_VERIFIED":"COMPARE_COMPLETE",
        output.researchNormalizedRmsDistance!=null &&
        Double.isFinite(output.researchNormalizedRmsDistance)?
        output.researchNormalizedRmsDistance:null));
      if(output.status!=FingerprintExpert.Status.RESEARCH_RESIDUAL_ONLY
         ||b.reference.canonicalChemicalUid==null
         ||!b.independentThresholdCalibrationVerified
         ||b.maxSourceNormalizedRmsDistance==null){
        reasons.add("FINGERPRINT_SOURCE_UNQUALIFIED "+b.reference.id+" "+output.status);
        continue;
      }
      if(output.researchNormalizedRmsDistance<=b.maxSourceNormalizedRmsDistance){
        raw.add(new CoreContracts.EvidenceNode(b.reference.sourceRoot,"SOURCE_FINGERPRINT",
          b.physicalSensorUid,b.reference.canonicalChemicalUid,
          CoreContracts.EvidencePolarity.POSITIVE,
          "source-trained threshold passed; research-only; NOT molecular identity"));
        assets.add(new ResearchCandidateGenerator.AssetLink(
          b.reference.sourceRoot,b.reference.id,b.reference.canonicalChemicalUid,true));
        positives++;
      }else {
        // A high residual is NOT negative evidence; it might be an
        // unseen mixture or an environmental response outside source.
        reasons.add("FINGERPRINT_HIGH_RESIDUAL_UNKNOWN "+b.reference.id);
      }
    }
    // Bundle variants share one original source root even if many sensor/time/
    // concentration templates match. Two studies may contribute two roots,
    // never one vote per MQ channel or per algorithm.
    for(var bundle:input.bundles) {
      count+=bundle.variants.size();
      boolean quarantined=NextResearchSourceQuarantine.blocked(bundle.sourceRoot,bundle.bundleUid);
      for(var v:bundle.variants)
        quarantined |= NextResearchSourceQuarantine.blocked(v.sourceSignature.sourceRoot,
            v.sourceSignature.id,v.sourceSignature.featureSpace,v.techniqueId);
      if(quarantined){
        reasons.add("SOURCE_TEMPORARILY_EXCLUDED_UCI1081_BUNDLE "+bundle.bundleUid);
        trace.add(new NextAnalysisTrace.Event("BUNDLE",bundle.bundleUid,
          bundle.sourceRoot,"",bundle.canonicalChemicalUid,
          "QUARANTINED","UCI1081",null));
        continue;
      }
      var score=ResearchFingerprintBundle.evaluate(bundle,input.device,input.current);
      trace.add(new NextAnalysisTrace.Event("BUNDLE",bundle.bundleUid,
        bundle.sourceRoot,"",bundle.canonicalChemicalUid,score.status.name(),
        "matched_variants="+score.matchedVariants+
        " sensor_channels="+score.distinctSensorChannels+
        " feature_families="+score.distinctFeatureFamilies,null));
      reasons.add("SOURCE_BUNDLE "+bundle.bundleUid+" "+score.status+
        " physical_MQ="+score.distinctSensorChannels+
        " feature_families="+score.distinctFeatureFamilies+
        " matched_variants="+score.matchedVariants);
      if(score.isEligibleResearchHypothesis && bundle.canonicalChemicalUid==null)
        reasons.add("SOURCE_CATEGORY_UNMAPPED_NO_CAS "+bundle.bundleUid);
      if(score.isEligibleResearchHypothesis && bundle.canonicalChemicalUid!=null) {
        raw.add(new CoreContracts.EvidenceNode(bundle.sourceRoot,
          "SOURCE_DIVERSE_FINGERPRINT_BUNDLE",bundle.variants.get(0).sensorUid,
          bundle.canonicalChemicalUid,CoreContracts.EvidencePolarity.POSITIVE,
          "ONE correlated source root across varied MQ fingerprints; research only"));
        assets.add(new ResearchCandidateGenerator.AssetLink(
          bundle.sourceRoot,bundle.bundleUid,bundle.canonicalChemicalUid,true));
        positives++;
      }
    }
    // R2: resolve physically cross-compared channels at ONE source-domain
    // concentration. No extra source root, no extra votes, and no G3 promotion.
    // Only positive, source-qualified curve evaluations reach this resolver.
    var sourceCoherence=SourceRootCoherence.evaluate(sourceCurveSamples);
    for(var fit:sourceCoherence){
      trace.add(new NextAnalysisTrace.Event("COHERENCE",fit.sourceRoot,fit.sourceRoot,
          "",fit.selectedResearchUid,fit.decisionReason,
          "margin="+fit.runnerUpMargin+
          " unresolved_models="+fit.nonUniqueSourceModels.size(),null));
      reasons.add("SOURCE_COHERENCE root="+fit.sourceRoot+
          " alternatives="+fit.ranked.size()+" reason="+fit.decisionReason+
          " margin="+fit.runnerUpMargin);
      for(var candidate:fit.ranked) {
        trace.add(new NextAnalysisTrace.Event("COHERENCE_HYPOTHESIS",
            candidate.sourceCurveIds.isEmpty()?fit.sourceRoot:candidate.sourceCurveIds.get(0),
            fit.sourceRoot,candidate.sensorUids.isEmpty()?"":candidate.sensorUids.get(0),
            candidate.uid,"SCORED",
            "channels="+candidate.independentSensorChannels+
            " ppm_source_only="+candidate.fittedPpm+
            " loo_max="+candidate.leaveOneChannelOutMaxResidual,candidate.score));
        reasons.add("SOURCE_COHERENCE_HYPOTHESIS root="+fit.sourceRoot+
            " uid="+candidate.uid+" score="+candidate.score+
            " rms="+candidate.normalizedRms+" ppm_source_only="+candidate.fittedPpm+
            " channels="+candidate.independentSensorChannels);
      }
      if(fit.selectedResearchUid==null)continue;
      boolean nonCurveAtSameRoot=false;
      for(var node:raw)
        if(node.sourceRootId.equals(fit.sourceRoot)&&!"SOURCE_CURVE".equals(node.expertId))
          nonCurveAtSameRoot=true;
      if(nonCurveAtSameRoot) {
        reasons.add("COHERENCE_NOT_APPLIED_MIXED_EXPERT_FAMILY "+fit.sourceRoot);
        continue;
      }
      SourceRootCoherence.Match winner=null;
      for(var candidate:fit.ranked)
        if(candidate.uid==fit.selectedResearchUid){winner=candidate;break;}
      if(winner==null||winner.sourceCurveIds.isEmpty()||winner.sensorUids.size()<2)
        throw new IllegalStateException("invalid source-coherence winner");
      raw.removeIf(node->node.sourceRootId.equals(fit.sourceRoot)
          &&"SOURCE_CURVE".equals(node.expertId));
      assets.removeIf(asset->asset.sourceRoot.equals(fit.sourceRoot));
      raw.add(new CoreContracts.EvidenceNode(fit.sourceRoot,
          "SOURCE_MULTISENSOR_CONCENTRATION_COHERENCE",winner.sensorUids.get(0),
          winner.uid,CoreContracts.EvidencePolarity.POSITIVE,
          "One source-root research hypothesis; competing identities retained in coherence trace"));
      assets.add(new ResearchCandidateGenerator.AssetLink(
          fit.sourceRoot,winner.sourceCurveIds.get(0),winner.uid,true));
      reasons.add("SOURCE_COHERENCE_REFINED_RESEARCH_ONLY root="+fit.sourceRoot+
          " uid="+winner.uid+" matched_sensors="+winner.independentSensorChannels);
    }
    var ledger=IndependentEvidenceLedger.consolidate(input.current.uid,raw);
    var graph=EvidenceConflictGraph.construct(ledger);
    for(var root:ledger.roots)
      trace.add(new NextAnalysisTrace.Event("LEDGER",root.sourceRootId,
          root.sourceRootId,root.sensorUids.isEmpty()?"":root.sensorUids.get(0),
          root.uniqueChemicalUid,root.effectivePolarity.name(),
          "alternatives="+root.alternativeChemicalUids+
          " distinct_channels="+root.sensorUids.size(),null));
    var generated=ResearchCandidateGenerator.generate(input.catalog,assets,ledger,
       input.displayLimit);
    var coherenceScores=new HashMap<Long,Double>();
    for(var root:sourceCoherence)if(root.selectedResearchUid!=null)
      for(var match:root.ranked)if(match.uid==root.selectedResearchUid)
        coherenceScores.merge(match.uid,match.score,Math::min);
    var arbitration=NextResearchDecisionPipeline.evaluate(
        graph,generated,input.displayLimit,coherenceScores);
    for(var candidate:arbitration.eligibleRanked)
      trace.add(new NextAnalysisTrace.Event("ARBITRATION",Long.toString(candidate.uid),
          String.join(";",candidate.roots),"",candidate.uid,"RANKED_RESEARCH_ONLY",
          "source_roots="+candidate.independentPositiveRoots+
          " score="+candidate.sourceConcentrationCoherenceScore,null));
    trace.add(new NextAnalysisTrace.Event("FINAL","STATUS","", "",null,
        arbitration.status.name(),String.join(" | ",arbitration.reasons),null));
    var result=NextResearchDecisionPipeline.result(input.current,input.sourceSnapshotHash,
       graph,arbitration);
    return new Output(result,arbitration,graph,sourceCoherence,trace,reasons,count,positives);
  }
  // P01 compatibility: immutable sensor input validation and canonical replay SHA-256.
    public static CoreContracts.Result evaluate(CoreContracts.Snapshot snapshot,
                                                SensorCapabilityRegistry registry) {
        if (snapshot == null || registry == null) throw new IllegalArgumentException("snapshot and registry required");
        List<String> problems = new ArrayList<>();
        for (CoreContracts.Reading r : snapshot.readings) {
            if (registry.byUid(r.sensorUid) == null) problems.add("UNREGISTERED_SENSOR:" + r.sensorUid);
            if (r.quality != CoreContracts.SampleQuality.VALID)
                problems.add("INVALID_SENSOR_SIGNAL:" + r.sensorUid + ":" + r.quality);
        }
        if (snapshot.readings.isEmpty()) problems.add("NO_READINGS");
        if (snapshot.readings.size() > 0 && !snapshot.mqRelayOn)
            problems.add("RELAY_OFF_WHILE_MQ_READING_PRESENT");
        return new CoreContracts.Result(VERSION, snapshot, snapshotHash(snapshot),
            problems.isEmpty() ? CoreContracts.DecisionStatus.NO_INFERENCE_MODEL
                               : CoreContracts.DecisionStatus.INVALID_INPUT,
            Collections.emptyList(), Collections.emptyList(), problems);
    }

    /**
     * Stable, source-only numeric signature. Not an authenticated signature.
     * Uses explicit units, presence markers, and preserves physical channel order.
     */
    public static String snapshotHash(CoreContracts.Snapshot s) {
        if (s == null) throw new IllegalArgumentException("snapshot required");
        StringBuilder b = new StringBuilder("NEXT-P01|");
        append(b, CoreContracts.SCHEMA);
        append(b, s.uid); append(b, s.deviceUid); append(b, s.mode.name());
        append(b, Long.toString(s.monotonicMillis));
        append(b, numeric(s.temperatureC)); append(b, numeric(s.relativeHumidityPercent));
        append(b, Boolean.toString(s.mqRelayOn)); append(b, s.stableManifestHash);
        for (CoreContracts.Reading r : s.readings) {
            append(b, r.sensorUid); append(b, r.rawAdc == null ? "N" : r.rawAdc.toString());
            append(b, numeric(r.voltageV)); append(b, numeric(r.rsKohm));
            append(b, numeric(r.rAirKohm)); append(b, numeric(r.r0SourceKohm));
            append(b, r.r0Definition.name()); append(b, r.heaterPhase); append(b, r.quality.name());
        }
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(
                b.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte v : bytes) hex.append(String.format(Locale.ROOT, "%02x", v & 255));
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
    private static String numeric(Double n) {
        return n == null ? "N" : Long.toHexString(Double.doubleToLongBits(n));
    }
    private static void append(StringBuilder b, String value) {
        b.append(value.length()).append(':').append(value).append('|');
    }
}
