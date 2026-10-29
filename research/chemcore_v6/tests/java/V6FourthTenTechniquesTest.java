package com.hazrob.tank.chemcore.v6;

import java.util.Arrays;
import java.util.List;

/** N03-D, ten additional numerical research primitives, NOT physical Tank G3. */
public final class V6FourthTenTechniquesTest {
    private static int assertions=0;
    private static void ok(boolean x,String label){
        assertions++;if(!x)throw new AssertionError(assertions+": "+label);
    }
    private static void near(double a,double b,double tolerance,String label){
        ok(Double.isFinite(a)&&Math.abs(a-b)<=tolerance,
           label+" actual="+a+" expected="+b);
    }
    private static void rejected(Runnable f,String label){
        boolean failed=false;
        try{f.run();}catch(IllegalArgumentException expected){failed=true;}
        ok(failed,label);
    }
    public static void main(String[] args){
        // 05.5: PLS1 one latent factor, fit only from source-labelled rows.
        double[][] xx={{1,0},{2,0},{3,0},{4,0},{5,0}};
        double[] yy={3,5,7,9,11};
        var pls=SourceNumericsV6.fitPls1(xx,yy);
        near(pls.predict(new double[]{3.5,0}),8,1e-10,"PLS one-factor linear prediction");
        ok(pls.trainingRows==5,"PLS training provenance");
        rejected(()->SourceNumericsV6.fitPls1(
            new double[][]{{1,1},{2,2},{3,3}},new double[]{3,3,3}),
            "unidentifiable PLS constant targets");
        rejected(()->pls.predict(new double[]{1}),"PLS dimension contract");

        // 05.6: an actual regression stump with min 2 original rows per leaf.
        var stump=SourceNumericsV6.fitStump(
            new double[][]{{0},{1},{2},{3},{4},{5}},
            new double[]{0,0,0,10,10,10});
        near(stump.threshold,2.5,1e-12,"CART split threshold");
        near(stump.predict(new double[]{1}),0,1e-12,"CART left source mean");
        near(stump.predict(new double[]{5}),10,1e-12,"CART right source mean");
        near(stump.trainSse,0,1e-12,"CART training SSE");
        rejected(()->SourceNumericsV6.fitStump(
            new double[][]{{1},{1},{1},{1}},new double[]{1,2,3,4}),
            "degenerate source split");

        // 05.9: source-level inverse interpolation in log response space.
        var unseen=SourceNumericsV6.estimateUnseen(
            new double[]{10,100,1000},new double[]{1,.1,.01},Math.sqrt(.1));
        near(unseen.researchEstimatePpm,Math.sqrt(1000),1e-9,
             "between 10 and 100 ppm anchor interpolation");
        ok(!unseen.extrapolated,"inside reference bounds");
        var extrap=SourceNumericsV6.estimateUnseen(
            new double[]{10,100,1000},new double[]{1,.1,.01},.001);
        ok(extrap.extrapolated,"reference-domain extrapolation flagged");
        ok(extrap.researchEstimatePpm>1000,"extrapolation has numeric estimate");
        rejected(()->SourceNumericsV6.estimateUnseen(
            new double[]{10,100,1000},new double[]{1,.2,.3},.1),
            "nonmonotone empirical response refused");

        // 07.4: research-source trend is not asserted to be physical drift.
        var drift=SourceNumericsV6.driftTrend(new double[]{1,1.2,1.4,1.6,1.8});
        near(drift.slopePerObservation,.2,1e-12,"least squares trend");
        near(drift.endpointDelta,.8,1e-12,"endpoint observed delta");
        ok(drift.observationCount==5,"source observations retained");
        rejected(()->SourceNumericsV6.driftTrend(new double[]{2,3}),
            "minimum distinct baseline observations");

        // 07.5: baseline transfer requires all explicit source references.
        near(SourceNumericsV6.transferBaseline(.8,2,1),.4,1e-12,
             "normalized source-baseline transfer");
        rejected(()->SourceNumericsV6.transferBaseline(.8,0,1),
            "zero measured baseline cannot be divided");
        rejected(()->SourceNumericsV6.transferBaseline(.8,2,Double.NaN),
            "unknown source reference is NOT invented");

        // 07.8: uncalibrated score entropy is only an experimental ordering.
        var active=SourceNumericsV6.rankSourceAmbiguity(new double[][]{
            {.99,.01},{.5,.5},{.7,.3}
        });
        ok(active.size()==3&&active.get(0).originalIndex==1,
           "most ambiguous score row first");
        near(active.get(0).normalizedEntropy,1,1e-12,"equal score entropy");
        ok(active.get(2).originalIndex==0,"sharp source score last");
        rejected(()->SourceNumericsV6.rankSourceAmbiguity(
            new double[][]{{0,0}}),"all zero class evidence cannot normalize");

        // 07.10: explicit monotonic-clock samples, no Android profile claim.
        var latency=SourceNumericsV6.latency(new long[]{3000000,1000000,2000000});
        near(latency.p50Ms,2,1e-12,"p50 actual sample math");
        near(latency.p95Ms,2.9,1e-12,"p95 linear percentile");
        near(latency.maxMs,3,1e-12,"observed maximum");
        rejected(()->SourceNumericsV6.latency(new long[]{-1}),
            "negative elapsed-time sample invalid");

        // 08.6: reproducible numerical source perturbation, not fake exposure.
        double[][] replica=SourceNumericsV6.simulateSource(new double[]{1,2},.1,4,12345L);
        double[][] replay=SourceNumericsV6.simulateSource(new double[]{1,2},.1,4,12345L);
        ok(replica.length==4&&Arrays.deepEquals(replica,replay),
           "deterministic source simulation under fixed seed");
        var noNoise=SourceNumericsV6.simulateSource(new double[]{1,2},0,2,9L);
        near(noNoise[0][0],1,1e-12,"zero-noise exact source");
        rejected(()->SourceNumericsV6.simulateSource(new double[]{1},-1,3,10),
            "negative simulation sigma");

        // 08.8: two nonnegative source prototypes; more fit does NOT prove mix.
        var pair=SourceNumericsV6.sourcePairFit(
            new double[]{2,3},new double[]{1,0},new double[]{0,1});
        near(pair.firstScale,2,1e-12,"first nonnegative coefficient");
        near(pair.secondScale,3,1e-12,"second nonnegative coefficient");
        near(pair.residualSquared,0,1e-12,"two-prototype residual");
        near(pair.bestSingleResidualSquared,4,1e-12,
             "best single prototype cannot explain entire response");
        ok(!pair.mixtureIdentityValidated,"residual improvement not proof");
        var single=SourceNumericsV6.sourcePairFit(
            new double[]{3,0},new double[]{1,0},new double[]{0,1});
        near(single.secondScale,0,1e-12,"boundary solution single source");
        rejected(()->SourceNumericsV6.sourcePairFit(
            new double[]{1,2},new double[]{0,0},new double[]{1,1}),
            "zero source prototype");

        // 08.11: interference residual under a specified known target.
        var interference=SourceNumericsV6.interferenceResidual(
            new double[]{1,3},new double[]{1,0},new double[]{0,1});
        near(interference.assumedInterfererScale,3,1e-12,
             "conditional interfering source scale");
        near(interference.residualSquared,0,1e-12,"conditional residual");
        near(interference.baselineResidualSquared,9,1e-12,
             "target-only discrepancy");
        ok(!interference.interfererIdentityValidated,"no physical interference identity");
        var forbiddenNegative=SourceNumericsV6.interferenceResidual(
            new double[]{1,-3},new double[]{1,0},new double[]{0,1});
        near(forbiddenNegative.assumedInterfererScale,0,1e-12,
             "negative unconstrained interference scale is clipped to zero");
        System.out.println("PASS V6FourthTenTechniques: "+assertions+
            " JDK assertions; 10 partial research algorithms; Tank G3=0");
    }
}
