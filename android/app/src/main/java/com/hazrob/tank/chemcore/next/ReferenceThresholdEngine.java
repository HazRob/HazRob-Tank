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
  public static String level(double estimatedPpm,double referencePpm,boolean lfl,boolean natural){
    double r=ratio(estimatedPpm,referencePpm);
    if(!Double.isFinite(r))return "NOT_EVALUABLE";
    if(r>=1)return "AT_OR_ABOVE_REFERENCE";
    if(!natural&&r>=(lfl?PRECAUTIONARY_LFL:DEFAULT_EARLY_EXPOSURE))return "EARLY_WARNING";
    return "BELOW_ADVISORY";
  }
  public static Double ppmReference(double value,String units,boolean lfl){
    if(!Double.isFinite(value)||value<=0||units==null)return null;
    String u=units.trim().toLowerCase(java.util.Locale.ROOT).replace(" ","");
    if(u.equals("ppm")||u.equals("ppmv"))return value;
    if(lfl&&(u.equals("vol%")||u.equals("%vol")||u.equals("volume%")||
       u.equals("%v/v")||u.equals("vol.%")))return value*10000.0;
    return null;
  }
  public static double percent(double estimatedPpm,double referencePpm){
    return 100.0*ratio(estimatedPpm,referencePpm);
  }
}