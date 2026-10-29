package com.hazrob.tank.chemcore.v6;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Four new independent V6 signal techniques, synthetic post-warmup fixtures. */
public final class SignalFeaturesV6Test {
    private static int assertions=0;
    private static void ok(boolean b,String msg){
        assertions++;if(!b)throw new AssertionError(assertions+": "+msg);
    }
    private static void close(double x,double y,double eps,String why){
        ok(Double.isFinite(x)&&Math.abs(x-y)<=eps,why+" actual="+x+" expected="+y);
    }
    public static void main(String[] unused){
        double[] f=SignalFeaturesV6.trailingMedian(
            new double[]{1,100,2,3,4,Double.NaN,5},3);
        ok(f.length==7,"one filter output per input");
        close(f[0],1,1e-12,"first");
        close(f[1],50.5,1e-12,"second");
        close(f[2],2,1e-12,"third robust");
        close(f[3],3,1e-12,"fourth robust");
        close(f[5],3.5,1e-12,"nonfinite ignored within bounded window");
        ok(Double.isNaN(SignalFeaturesV6.median(new double[]{Double.NaN})),
           "corrupt-only median has no fabricated result");
        close(SignalFeaturesV6.median(new double[]{1,2,3,4}),2.5,1e-12,
              "median even");
        try{SignalFeaturesV6.trailingMedian(new double[]{1},0);
            throw new AssertionError("accepted invalid filter window");
        }catch(IllegalArgumentException expected){assertions++;}
        double[] shape=SignalFeaturesV6.log10GeometricShape(
            new double[]{100,1000,10000,Double.NaN,-5,0});
        ok(shape.length==6,"preserves channel positions");
        close(shape[0],-1,1e-12,"first normalized log");
        close(shape[1],0,1e-12,"second normalized log");
        close(shape[2],1,1e-12,"third normalized log");
        ok(Double.isNaN(shape[3])&&Double.isNaN(shape[4])&&Double.isNaN(shape[5]),
           "mathematically invalid channels not imputed");
        double[] missing=SignalFeaturesV6.log10GeometricShape(new double[]{0,-1,Double.NaN});
        ok(Arrays.stream(missing).allMatch(Double::isNaN),"no finite reference shape is invented");
        List<SignalFeaturesV6.Sample> observations=Arrays.asList(
            new SignalFeaturesV6.Sample(3000,200),
            new SignalFeaturesV6.Sample(0,100),
            new SignalFeaturesV6.Sample(2000,200),
            new SignalFeaturesV6.Sample(1000,100)
        );
        var summary=SignalFeaturesV6.summarize(observations);
        ok(summary.used==4 && summary.invalid==0,"all four real numeric samples");
        close(summary.arithmeticMeanRs,150,1e-9,"mean");
        close(summary.medianRs,150,1e-9,"median");
        close(summary.standardDeviationRs,50,1e-9,"std");
        close(summary.relativeLastFirst,1.0,1e-9,"final increase");
        close(summary.log10SlopePerSecond,Math.log10(2)*0.4,1e-7,
              "least squares log ratio slope");
        ok(summary.firstTimeMs==0&&summary.lastTimeMs==3000,"time sorted not arbitrary");
        ok(!summary.calibratedConcentration,"not gas ppm");
        double integral=Math.log10(2)*(0.5+1.0);
        close(summary.integratedLog10DeltaSeconds,integral,1e-9,
              "trapezoidal measured AUC");
        List<SignalFeaturesV6.Sample> partiallyCorrupt=new ArrayList<>(observations);
        partiallyCorrupt.add(new SignalFeaturesV6.Sample(-1,555));
        partiallyCorrupt.add(new SignalFeaturesV6.Sample(5000,Double.NaN));
        partiallyCorrupt.add(new SignalFeaturesV6.Sample(9000,-5));
        var result=SignalFeaturesV6.summarize(partiallyCorrupt);
        ok(result.invalid==3&&result.used==4,"only unusable arithmetic excluded");
        close(result.arithmeticMeanRs,150,1e-9,"valid source data preserved");
        var single=SignalFeaturesV6.summarize(Collections.singletonList(
            new SignalFeaturesV6.Sample(10,42)));
        ok(single.used==1,"single sample retained for static features");
        ok(Double.isNaN(single.log10SlopePerSecond),"cannot invent a time slope");
        var empty=SignalFeaturesV6.summarize(null);
        ok(empty.used==0&&empty.total==0,"missing trace supported");
        ok(Double.isNaN(empty.medianRs)&&Double.isNaN(empty.relativeLastFirst),
           "no artificial sample statistics");
        System.out.println("PASS SignalFeaturesV6: "+assertions+
            " assertions, four independent numeric techniques, no heater changes");
    }
}
