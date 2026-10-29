package com.servoc.app.chemcore;

/** Bounded source-curve numerical checks. Nothing here proves a physical PPM value. */
public final class TankV6CurveKernelTest {
    private static int assertions=0;
    private static void check(boolean b,String message){assertions++;if(!b)throw new AssertionError(message);}
    private static TankV6SourceCompatibility.Source source(
            TankV6SourceCompatibility.R0Definition definition,
            TankV6SourceCompatibility.RatioDirection direction,
            String device) {
        return new TankV6SourceCompatibility.Source(
            "mq-source-curve","MQ3",device,"MQ3-unit-1",
            definition,direction,50,5000,true,"metadata-not-physical-proof");
    }
    private static TankV6SourceCompatibility.Sample sample(
            double ratio,TankV6SourceCompatibility.R0Definition def,String device){
        return new TankV6SourceCompatibility.Sample(
            device,"MQ3-unit-1","MQ3",ratio*10000,10000,def,
            TankV6SourceCompatibility.RatioDirection.RS_OVER_R0,false);
    }
    public static void main(String[] args){
        var def=TankV6SourceCompatibility.R0Definition.CLEAN_AIR_RATIO;
        var dir=TankV6SourceCompatibility.RatioDirection.RS_OVER_R0;
        var curve=new TankV6CurveKernel.Curve("source-MQ3-alcohol-50x",
                   source(def,dir,"tank-a"),2.0,-0.5,50);
        double ratio=TankV6CurveKernel.forwardResearchRatio(curve,200);
        check(Math.abs(ratio-1)<1e-12,"MQ3 output scale=50 applies correctly");
        var fit=TankV6CurveKernel.inverseResearchFit(sample(ratio,def,"tank-a"),curve);
        check(fit.state==TankV6CurveKernel.State.RESEARCH_SCREENING,"valid research fit");
        check(Math.abs(fit.researchFitPpm-200)<1e-8,"inverse matches source formula");
        check(!fit.reportablePpm&&!fit.deviceValidated,"no G3 despite forged source M4 flag");
        check("mq-source-curve".equals(fit.sourceId),"provenance retained");
        var mismatch=TankV6CurveKernel.inverseResearchFit(
            sample(1,TankV6SourceCompatibility.R0Definition.REFERENCE_GAS_AT_DECLARED_PPM,"tank-a"),curve);
        check(mismatch.state==TankV6CurveKernel.State.SOURCE_R0_MISMATCH,"R0 definition incompatible");
        var domain=TankV6CurveKernel.inverseResearchFit(sample(.001,def,"tank-a"),curve);
        check(domain.state==TankV6CurveKernel.State.OUTSIDE_SOURCE_RANGE,"no extrapolation");
        var wrongDevice=TankV6CurveKernel.inverseResearchFit(sample(1,def,"wrong"),curve);
        check(wrongDevice.state==TankV6CurveKernel.State.SOURCE_NOT_COMPATIBLE,"no source cross-device elevation");
        var inverse=new TankV6CurveKernel.Curve("inverse-source",
            source(def,TankV6SourceCompatibility.RatioDirection.R0_OVER_RS,"tank-a"),
            2.0,-.5,50);
        double sourceRatio=TankV6CurveKernel.forwardResearchRatio(inverse,200);
        var inverseFit=TankV6CurveKernel.inverseResearchFit(sample(1/sourceRatio,def,"tank-a"),inverse);
        check(inverseFit.state==TankV6CurveKernel.State.RESEARCH_SCREENING,"inverse ratio direction respected");
        check(Math.abs(inverseFit.researchFitPpm-200)<1e-8,"proper ratio inversion");
        var invalid=new TankV6CurveKernel.Curve("invalid",source(def,dir,"tank-a"),0,-.5,50);
        check(TankV6CurveKernel.inverseResearchFit(sample(1,def,"tank-a"),invalid).state==
            TankV6CurveKernel.State.INVALID_SOURCE_CURVE,"a=0 rejected");
        var invalidSlope=new TankV6CurveKernel.Curve("slope0",source(def,dir,"tank-a"),2,0,50);
        check(TankV6CurveKernel.inverseResearchFit(sample(1,def,"tank-a"),invalidSlope).state==
            TankV6CurveKernel.State.INVALID_SOURCE_CURVE,"slope=0 rejected");
        try{TankV6CurveKernel.forwardResearchRatio(curve,10);
            throw new AssertionError("range must fail");
        }catch(IllegalArgumentException ok){assertions++;}
        try{TankV6CurveKernel.forwardResearchRatio(curve,Double.NaN);
            throw new AssertionError("NaN must fail");
        }catch(IllegalArgumentException ok){assertions++;}
        check(TankV6CurveKernel.inverseResearchFit(null,curve).state==
              TankV6CurveKernel.State.INVALID_SAMPLE,"null sample rejected");
        System.out.println("PASS TankV6CurveKernel: "+assertions+
          " JDK assertions; source math only, no device M4 or ppm certificate");
    }
}
