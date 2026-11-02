package com.hazrob.tank.chemcore.next;

public final class ReferenceThresholdEngine {
  private ReferenceThresholdEngine() {}
  public static final double DEFAULT_EARLY_EXPOSURE=0.90;
  public static final double PRECAUTIONARY_LFL=0.10;
  public static double ratio(double estimatedPpm,double referencePpm){
    if(!Double.isFinite(estimatedPpm)||estimatedPpm<0||!Double.isFinite(referencePpm)||referencePpm<=0)
      return Double.NaN;
    return estimatedPpm/referencePpm;
  }
  public static double percent(double estimatedPpm,double referencePpm){
    return 100.0*ratio(estimatedPpm,referencePpm);
  }
}