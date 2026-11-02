package com.hazrob.tank.chemcore.next;

/**
 * P05.5 checks an EXTERNALLY PROPOSED source-domain ppm hypothesis against a
 * qualified source equation and an observed source-R0 ratio. It cannot infer
 * true concentration by circularly inverting the same ratio and claiming a
 * zero residual as proof of gas identity. Mixtures always abstain unless a
 * separate physically validated mixture forward model exists.
 */
public final class ForwardInverseVerifier {
  private ForwardInverseVerifier(){}
  public enum Status { RESEARCH_FORWARD_RESIDUAL_ONLY, INVALID_INPUT, SOURCE_NOT_APPLICABLE,
    OUTSIDE_SOURCE_DOMAIN, MIXTURE_MODEL_UNQUALIFIED, NUMERIC_FAILURE }
  public static final class Result {
    public final Status status;public final Double expectedRatio,observedRatio,
      logRatioResidual,relativeRatioResidual,referenceHypothesisPpm;
    public final String sourceRoot,reason;
    public final boolean independentGasIdentityVerified,certifiedPpm,alarmAuthorized,g3Qualified;
    private Result(SourceCurveExpert.Curve curve,Status status,Double expected,Double observed,
                   Double logResidual,Double relative,Double ppm,String reason){
      sourceRoot=curve==null?"":curve.sourceRoot;
      this.status=status;expectedRatio=expected;observedRatio=observed;
      logRatioResidual=logResidual;relativeRatioResidual=relative;
      referenceHypothesisPpm=ppm;this.reason=reason;
      independentGasIdentityVerified=false;certifiedPpm=false;alarmAuthorized=false;
      g3Qualified=false;
    }
  }
  private static Result no(SourceCurveExpert.Curve curve,Status s,String reason){
    return new Result(curve,s,null,null,null,null,null,reason);
  }
  public static Result verify(SourceCurveExpert.Curve curve,
       CoreContracts.SensorInstance sensor,CoreContracts.Reading observed,
       SourceCurveExpert.Qualification q,Double independentlyProposedPpm,
       boolean mixtureHypothesis) {
    if(curve==null||q==null)throw new IllegalArgumentException("curve/qualification needed");
    if(mixtureHypothesis)return no(curve,Status.MIXTURE_MODEL_UNQUALIFIED,
      "source single-gas power law cannot infer mixed-gas components");
    if(independentlyProposedPpm==null||!Double.isFinite(independentlyProposedPpm)
       ||independentlyProposedPpm<=0)
      return no(curve,Status.INVALID_INPUT,"independent proposal ppm required");
    if(curve.sourcePpmMin==null||curve.sourcePpmMax==null||
       independentlyProposedPpm<curve.sourcePpmMin||
       independentlyProposedPpm>curve.sourcePpmMax)
      return no(curve,Status.OUTSIDE_SOURCE_DOMAIN,"no source interpolation across excluded limits");
    // Use source expert's real physical applicability gates; never claim a
    // cross-device R_air matches the source R0.
    var applied=SourceCurveExpert.evaluate(curve,sensor,observed,q);
    if(applied.decision!=SourceCurveExpert.Decision.RESEARCH_SOURCE_QUANTITATIVE_ESTIMATE)
      return no(curve,Status.SOURCE_NOT_APPLICABLE,"source qualification: "+applied.abstention);
    double ratio=observed.sourceRatioOrNaN(),expected;
    try{expected=SourceCurveExpert.forwardSourceRatio(curve,independentlyProposedPpm);}
    catch(IllegalArgumentException ex){return no(curve,Status.NUMERIC_FAILURE,ex.getMessage());}
    double residual=Math.log(ratio/expected),relative=ratio/expected-1.0;
    if(!Double.isFinite(residual)||!Double.isFinite(relative))
      return no(curve,Status.NUMERIC_FAILURE,"numeric overflow in residual");
    return new Result(curve,Status.RESEARCH_FORWARD_RESIDUAL_ONLY,expected,ratio,
      residual,relative,independentlyProposedPpm,
      "source-domain difference only; zero residual does not independently prove chemical identity");
  }
}
