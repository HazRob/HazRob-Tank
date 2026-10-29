package com.servoc.app.chemcore;

/** JDK-only tests for a Tank-local, not-yet-connected V6 input evidence gate. */
public final class TankV6SourceCompatibilityTest {
    private static int checks=0;
    private static void check(boolean ok,String why){
        checks++;
        if(!ok)throw new AssertionError("FAIL "+checks+": "+why);
    }
    private static TankV6SourceCompatibility.Sample sample(
            TankV6SourceCompatibility.R0Definition d,boolean heater){
        return new TankV6SourceCompatibility.Sample(
          "tank1","MQ2-physical-1","MQ2",15000,10000,d,
          TankV6SourceCompatibility.RatioDirection.RS_OVER_R0,heater);
    }
    private static TankV6SourceCompatibility.Source model(
            TankV6SourceCompatibility.R0Definition d,
            String device,String sensorInstance,boolean m4){
        return new TankV6SourceCompatibility.Source(
          "manufacturer-source-curve-A","MQ2",device,sensorInstance,d,
          TankV6SourceCompatibility.RatioDirection.RS_OVER_R0,
          10,1000,m4,"physical-reference-claim-not-a-certificate");
    }
    private static String deny(TankV6SourceCompatibility.Sample s,
                               TankV6SourceCompatibility.Source m,
                               double ppm,boolean report){
        TankV6SourceCompatibility.Verdict v=
          TankV6SourceCompatibility.evaluate(s,m,ppm,report);
        check(!v.usable,"must deny "+v.reason);
        check(v.level==TankV6SourceCompatibility.Level.NONE,"denied level must be NONE");
        return v.reason;
    }
    public static void main(String[] args){
        TankV6SourceCompatibility.Sample s=sample(
           TankV6SourceCompatibility.R0Definition.CLEAN_AIR_RATIO,false);
        TankV6SourceCompatibility.Source m=model(
           TankV6SourceCompatibility.R0Definition.CLEAN_AIR_RATIO,null,null,false);
        TankV6SourceCompatibility.Verdict v=
           TankV6SourceCompatibility.evaluate(s,m,100,false);
        check(v.usable,"valid research source");
        check(v.level==TankV6SourceCompatibility.Level.RESEARCH_SCREENING,
              "not M4");
        check(Math.abs(v.log10RsOverR0-Math.log10(1.5))<1e-12,"ratio direction");
        check("INCOMPATIBLE_R0_REFERENCE".equals(deny(s,model(
           TankV6SourceCompatibility.R0Definition.REFERENCE_GAS_AT_DECLARED_PPM,
           null,null,false),100,false)), "no R0 convention interpolation");
        check("OUTSIDE_DECLARED_SOURCE_DOMAIN".equals(deny(s,m,1,false)),
              "domain low");
        check("OUTSIDE_DECLARED_SOURCE_DOMAIN".equals(deny(s,m,2000,false)),
              "domain high");
        check("INVALID_RS_OR_R0".equals(deny(
             new TankV6SourceCompatibility.Sample("tank1","MQ2-physical-1","MQ2",
                 Double.NaN,10000,TankV6SourceCompatibility.R0Definition.CLEAN_AIR_RATIO,
                 TankV6SourceCompatibility.RatioDirection.RS_OVER_R0,true),
             m,100,false)), "nan fails");
        check("DEVICE_SCOPE_MISMATCH".equals(deny(s,model(
          TankV6SourceCompatibility.R0Definition.CLEAN_AIR_RATIO,
          "another-device","MQ2-physical-1",true),100,false)),"device scope");
        check("SENSOR_INSTANCE_MISMATCH".equals(deny(s,model(
          TankV6SourceCompatibility.R0Definition.CLEAN_AIR_RATIO,
          "tank1","MQ2-physical-another",true),100,false)),"physical sensor scope");
        check("HEATER_PROTOCOL_UNVERIFIED".equals(deny(s,m,100,true)),
              "M4 heater gate");
        check("M4_REQUIRES_INDEPENDENT_G3_REGISTRY".equals(deny(
          sample(TankV6SourceCompatibility.R0Definition.CLEAN_AIR_RATIO,true),
          model(TankV6SourceCompatibility.R0Definition.CLEAN_AIR_RATIO,
                "tank1","MQ2-physical-1",true),100,true)), 
              "even claimed certificate flags cannot grant M4");
        check(Math.abs(TankV6SourceCompatibility.computeRsOhm(10000,5,2)-15000)<1e-10,
              "Rs electrical equation");
        check(Math.abs(TankV6SourceCompatibility.computeCleanAirR0Ohm(98300,9.83)-10000)<1e-9,
              "R0 clean air arithmetic");
        try{
          TankV6SourceCompatibility.computeRsOhm(10000,5,5);
          throw new AssertionError("ADC rail should fail");
        }catch(IllegalArgumentException expected){checks++;}
        try{
          TankV6SourceCompatibility.computeCleanAirR0Ohm(10000,0);
          throw new AssertionError("clean air zero should fail");
        }catch(IllegalArgumentException expected){checks++;}
        System.out.println("PASS TankV6SourceCompatibility JDK pure contract; assertions="+checks+
           "; no Android runtime integration or Tank G3 proof");
    }
}
