package com.hazrob.tank.chemcore.next;

import java.util.*;

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
  public static final String VERSION="chemcore-next-research-core/0.1-g1";

  public static final class CurveBinding {
    public final SourceCurveExpert.Curve curve;
    public final String physicalSensorUid;
    public final SourceCurveExpert.Qualification qualification;
    public CurveBinding(SourceCurveExpert.Curve model,String sensorUid,
                        SourceCurveExpert.Qualification q){
      if(model==null||sensorUid==null||sensorUid.isBlank()||q==null)
        throw new IllegalArgumentException("source curve binding provenance required");
      curve=model;physicalSensorUid=sensorUid;qualification=q;
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
    public final int displayLimit;
    public Input(CoreContracts.DeviceProfile device,CoreContracts.Snapshot current,
                 String snapshotHash,
                 List<ResearchCandidateGenerator.Material> catalog,
                 List<CurveBinding> curves,List<FingerprintBinding> fingerprints,
                 int maxDisplay){
      if(device==null||current==null||snapshotHash==null||snapshotHash.isBlank()
          ||catalog==null||curves==null||fingerprints==null||maxDisplay<0)
        throw new IllegalArgumentException("complete immutable source context required");
      this.device=device;this.current=current;sourceSnapshotHash=snapshotHash;
      this.catalog=List.copyOf(catalog);this.curves=List.copyOf(curves);
      this.fingerprints=List.copyOf(fingerprints);displayLimit=maxDisplay;
    }
  }
  public static final class Output {
    public final CoreContracts.Result research;
    public final NextResearchDecisionPipeline.Decision arbitration;
    public final EvidenceConflictGraph.Graph graph;
    public final List<String> sourceDiagnostics;
    public final int modelsInspected,positiveModels;
    public final boolean safetyAlarmAuthorized,ppmAuthorized,molecularIdentityConfirmed,g3Qualified;
    private Output(CoreContracts.Result result,NextResearchDecisionPipeline.Decision d,
                   EvidenceConflictGraph.Graph g,List<String> diagnostics,int inspected,int positive){
      research=result;arbitration=d;graph=g;sourceDiagnostics=List.copyOf(diagnostics);
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
    List<String> reasons=new ArrayList<>();
    int count=0,positives=0;
    for(var b:input.curves){
      count++;
      var sensor=instances.get(b.physicalSensorUid);
      var reading=readings.get(b.physicalSensorUid);
      if(!input.current.mqRelayOn){
        reasons.add("SOURCE_CURVE_BLOCKED_RELAY_OFF "+b.curve.id);continue;
      }
      var out=SourceCurveExpert.evaluate(b.curve,sensor,reading,b.qualification);
      // A valid single-source curve is a research HYPOTHESIS ONLY, not molecular identity.
      if(out.decision==SourceCurveExpert.Decision.RESEARCH_SOURCE_QUANTITATIVE_ESTIMATE
         &&b.curve.canonicalChemicalUid!=null){
        raw.add(new CoreContracts.EvidenceNode(b.curve.sourceRoot,"SOURCE_CURVE",
          b.physicalSensorUid,b.curve.canonicalChemicalUid,
          CoreContracts.EvidencePolarity.POSITIVE,
          "source single-gas curve research hypothesis; quantitative result non-reportable"));
        assets.add(new ResearchCandidateGenerator.AssetLink(
          b.curve.sourceRoot,b.curve.id,b.curve.canonicalChemicalUid,true));
        positives++;
      }else {
        reasons.add("CURVE_NOT_ELIGIBLE "+b.curve.id+" "+out.abstention);
      }
    }
    for(var b:input.fingerprints){
      count++;
      if(!input.current.mqRelayOn || !instances.containsKey(b.physicalSensorUid)
         ||!readings.containsKey(b.physicalSensorUid)
         ||readings.get(b.physicalSensorUid).quality!=CoreContracts.SampleQuality.VALID) {
        reasons.add("FINGERPRINT_PHYSICAL_SENSOR_NOT_ELIGIBLE "+b.reference.id);continue;
      }
      var output=FingerprintExpert.compare(b.reference,b.observation,b.qualification);
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
    var ledger=IndependentEvidenceLedger.consolidate(input.current.uid,raw);
    var graph=EvidenceConflictGraph.construct(ledger);
    var generated=ResearchCandidateGenerator.generate(input.catalog,assets,ledger,
       input.displayLimit);
    var arbitration=NextResearchDecisionPipeline.evaluate(graph,generated,input.displayLimit);
    var result=NextResearchDecisionPipeline.result(input.current,input.sourceSnapshotHash,
       graph,arbitration);
    return new Output(result,arbitration,graph,reasons,count,positives);
  }
}
