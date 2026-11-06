package com.hazrob.tank.chemcore.next;

import java.util.*;

/**
 * ChemCore Next research R2: fit one common source-domain concentration to
 * multi-MQ responses for every hypothesis within ONE correlated source root.
 *
 * This algorithm does NOT create additional evidence roots or a safety-grade
 * probability, does NOT translate manufacturer R0 into a device calibration,
 * and must receive already source-qualified measurements from SourceCurveExpert.
 * A one-channel candidate is retained in diagnostics, never called unique by
 * this multi-channel resolver. Thresholds here are G1 research heuristics,
 * deliberately reported for later independent G2 calibration.
 */
public final class SourceRootCoherence {
  private SourceRootCoherence(){}
  public static final String VERSION="next-source-root-coherence/0.3-g1-integrity-trace";
  public static final double RESEARCH_MAX_NORMALIZED_RMS=1.5;
  public static final double RESEARCH_MIN_SCORE_MARGIN=0.15;
  public static final double RESEARCH_COVERAGE_PENALTY=0.55;
  public static final double FALLBACK_LOG10_SIGMA=0.25;

  public static final class Sample {
    public final SourceCurveExpert.Curve curve;
    public final String physicalSensorUid;
    public final double sourceRatio,log10Sigma;
    public Sample(SourceCurveExpert.Curve c,String uid,double ratio,Double sigma){
      if(c==null||c.canonicalChemicalUid==null||uid==null||uid.isBlank()
          ||!Double.isFinite(ratio)||ratio<=0)
        throw new IllegalArgumentException("qualified source sample required");
      if(sigma!=null&&(!Double.isFinite(sigma)||sigma<=0))
        throw new IllegalArgumentException("source log10 sigma must be positive");
      curve=c;physicalSensorUid=uid;sourceRatio=ratio;
      log10Sigma=sigma==null?FALLBACK_LOG10_SIGMA:sigma;
    }
  }
  public static final class Match {
    public final long uid;
    public final int independentSensorChannels, availableRootChannels;
    public final double fittedPpm,normalizedRms,coveragePenalty,score;
    /** Leave-one-channel-out maximum standardized predictive residual.
        Null if fewer than three distinct physical channels are observed. */
    public final Double leaveOneChannelOutMaxResidual;
    public final List<String> sensorUids,sourceCurveIds;
    private Match(long id,Set<String> sensors,List<String> curves,int total,double ppm,
                  double rms,double penalty,Double looMax){
      uid=id;independentSensorChannels=sensors.size();
      availableRootChannels=total;fittedPpm=ppm;normalizedRms=rms;
      coveragePenalty=penalty;score=rms+penalty;
      leaveOneChannelOutMaxResidual=looMax;
      sensorUids=List.copyOf(sensors);sourceCurveIds=List.copyOf(curves);
    }
  }
  public static final class RootReport {
    public final String sourceRoot;
    public final List<Match> ranked;
    public final Long selectedResearchUid;
    public final String decisionReason;
    /** Unresolved alternatives in original source, including duplicate
      curve models for one physical sensor and material. */
    public final List<String> nonUniqueSourceModels;
    public final double runnerUpMargin;
    public final boolean g3Qualified,ppmCertified,identityConfirmed;
    private RootReport(String root,List<Match> ordered,Long selected,
                       String reason,double gap,List<String> duplicateAssets){
      sourceRoot=root;ranked=List.copyOf(ordered);selectedResearchUid=selected;
      decisionReason=reason;runnerUpMargin=gap;
      nonUniqueSourceModels=List.copyOf(duplicateAssets);
      g3Qualified=false;ppmCertified=false;identityConfirmed=false;
    }
  }
  private static final class Point {
    final Sample sample;
    final double alpha,beta,log10Observed,lo,hi;
    Point(Sample s){
      sample=s;var c=s.curve;
      if(c.sourcePpmMin==null||c.sourcePpmMax==null)
        throw new IllegalArgumentException("source curve range required");
      lo=Math.log10(c.sourcePpmMin);hi=Math.log10(c.sourcePpmMax);
      log10Observed=Math.log10(s.sourceRatio);
      if(c.equation==SourceCurveExpert.Equation.RATIO_EQUALS_A_TIMES_PPM_OVER_SCALE_POWER_B){
        if(c.sourceScalePpm==null)
          throw new IllegalArgumentException("source scale required");
        beta=c.b;
        alpha=Math.log10(c.a)-c.b*Math.log10(c.sourceScalePpm);
      }else{
        beta=1.0/c.b;
        alpha=-Math.log10(c.a)/c.b;
      }
    }
  }
  /** Identical mathematical source records are redundant aliases, not conflicts.
      Distinct source fits for one UID/channel remain unresolved research models. */
  private static boolean sameSourceEquation(Sample x,Sample y) {
    var a=x.curve;var b=y.curve;
    return a.equation==b.equation && Double.compare(a.a,b.a)==0
        && Double.compare(a.b,b.b)==0
        && Objects.equals(a.sourceScalePpm,b.sourceScalePpm)
        && Objects.equals(a.sourcePpmMin,b.sourcePpmMin)
        && Objects.equals(a.sourcePpmMax,b.sourcePpmMax)
        && a.requiredHeaterPhase.equals(b.requiredHeaterPhase)
        && Double.compare(x.log10Sigma,y.log10Sigma)==0
        && Double.compare(x.sourceRatio,y.sourceRatio)==0;
  }
  /** All source hypotheses, including low-coverage and ties, remain inspectable. */
  public static List<RootReport> evaluate(Collection<Sample> qualifiedSamples) {
    if(qualifiedSamples==null)throw new IllegalArgumentException("source samples required");
    var byRoot=new TreeMap<String,List<Sample>>();
    for(var s:qualifiedSamples){
      if(s==null)throw new IllegalArgumentException("null sample");
      byRoot.computeIfAbsent(s.curve.sourceRoot,k->new ArrayList<>()).add(s);
    }
    var reports=new ArrayList<RootReport>();
    for(var entry:byRoot.entrySet()){
      var samples=entry.getValue();
      var physicalSensors=new HashSet<String>();
      var byUid=new TreeMap<Long,List<Sample>>();
      for(var s:samples){
        physicalSensors.add(s.physicalSensorUid);
        byUid.computeIfAbsent(s.curve.canonicalChemicalUid,k->new ArrayList<>()).add(s);
      }
      var ranked=new ArrayList<Match>();
      var ambiguousDuplicates=new ArrayList<String>();
      for(var ce:byUid.entrySet()){
        // A second distinct source model on the SAME physical channel is not
        // an independent observation. In R2 a lexicographic ID silently
        // decided which curve was used. R3 preserves this as unresolved.
        var unique=new TreeMap<String,Sample>();
        boolean duplicateModels=false;
        for(var sample:ce.getValue()){
          Sample old=unique.putIfAbsent(sample.physicalSensorUid,sample);
          if(old!=null && !sameSourceEquation(old,sample)) {
            duplicateModels=true;
            ambiguousDuplicates.add("uid="+ce.getKey()+" sensor="+sample.physicalSensorUid+
                " curves="+old.curve.id+","+sample.curve.id);
          }
        }
        if(duplicateModels)continue;
        var points=new ArrayList<Point>();
        boolean valid=true;
        double lo=Double.NEGATIVE_INFINITY,hi=Double.POSITIVE_INFINITY;
        for(Sample s:unique.values()){
          try{
            Point p=new Point(s);points.add(p);
            lo=Math.max(lo,p.lo);hi=Math.min(hi,p.hi);
          }catch(IllegalArgumentException ex){valid=false;break;}
        }
        if(!valid||lo>hi||points.isEmpty())continue;
        double num=0,den=0;
        for(Point p:points){
          double w=1/(p.sample.log10Sigma*p.sample.log10Sigma);
          num+=w*p.beta*(p.log10Observed-p.alpha);
          den+=w*p.beta*p.beta;
        }
        if(!(den>0)||!Double.isFinite(den))continue;
        double x=Math.max(lo,Math.min(hi,num/den));
        if(!Double.isFinite(x))continue;
        double ppm=Math.pow(10,x);
        if(!Double.isFinite(ppm)||ppm<=0)continue;
        double sum=0;
        var ids=new ArrayList<String>();
        for(Point p:points){
          double z=(p.log10Observed-(p.alpha+p.beta*x))/p.sample.log10Sigma;
          sum+=z*z;ids.add(p.sample.curve.id);
        }
        double rms=Math.sqrt(sum/points.size());
        Double maxLooResidual=null;
        if(points.size()>=3){
          double worst=0;
          for(int held=0;held<points.size();held++){
            double numRest=0,denRest=0;
            for(int i=0;i<points.size();i++){
              if(i==held)continue;
              Point q=points.get(i);
              double w=1/(q.sample.log10Sigma*q.sample.log10Sigma);
              numRest+=w*q.beta*(q.log10Observed-q.alpha);
              denRest+=w*q.beta*q.beta;
            }
            if(!(denRest>0)||!Double.isFinite(denRest)){
              worst=Double.POSITIVE_INFINITY;break;
            }
            double xRest=Math.max(lo,Math.min(hi,numRest/denRest));
            Point q=points.get(held);
            worst=Math.max(worst,Math.abs(q.log10Observed-(q.alpha+q.beta*xRest))
                /q.sample.log10Sigma);
          }
          maxLooResidual=worst;
        }
        double penalty=RESEARCH_COVERAGE_PENALTY*
            (physicalSensors.size()-points.size())/Math.max(1,physicalSensors.size());
        ranked.add(new Match(ce.getKey(),unique.keySet(),ids,physicalSensors.size(),
            ppm,rms,penalty,maxLooResidual));
      }
      ranked.sort(Comparator.comparingDouble((Match m)->m.score)
          .thenComparing((Match m)->-m.independentSensorChannels)
          .thenComparingLong(m->m.uid));
      Long selected=null;
      String reason;
      double margin=ranked.size()<2?Double.POSITIVE_INFINITY:
        ranked.get(1).score-ranked.get(0).score;
      if(!ambiguousDuplicates.isEmpty())
        reason="DUPLICATE_CURVE_MODELS_SAME_UID_AND_PHYSICAL_SENSOR_UNRESOLVED";
      else if(byUid.size()<2)reason="ONLY_ONE_SOURCE_IDENTITY_NO_DISAMBIGUATION_NEEDED";
      else if(ranked.isEmpty())reason="NO_SHARED_CONCENTRATION_SOURCE_DOMAIN";
      else if(ranked.get(0).independentSensorChannels<2)
        reason="TOP_HYPOTHESIS_SINGLE_SENSOR_NOT_DISCRIMINATIVE";
      else if(ranked.get(0).normalizedRms>RESEARCH_MAX_NORMALIZED_RMS)
        reason="TOP_HYPOTHESIS_RESIDUAL_TOO_LARGE";
      else if(margin<RESEARCH_MIN_SCORE_MARGIN)
        reason="MULTISENSOR_SOURCE_RANKING_TIE";
      else{
        selected=ranked.get(0).uid;
        reason="RESEARCH_MULTISENSOR_CONSISTENT_SINGLE_SOURCE_ROOT";
      }
      reports.add(new RootReport(entry.getKey(),ranked,selected,reason,margin,ambiguousDuplicates));
    }
    return List.copyOf(reports);
  }
}
