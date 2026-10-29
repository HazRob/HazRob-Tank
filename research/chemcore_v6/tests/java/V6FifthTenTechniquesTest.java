package com.hazrob.tank.chemcore.v6;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** N03-E: research-only numerical regression and negative-case assertions. */
public final class V6FifthTenTechniquesTest {
    private static int assertions;
    private static void ok(boolean value,String message){
        assertions++;if(!value)throw new AssertionError(assertions+": "+message);
    }
    private static void near(double actual,double expected,double tolerance,String message){
        ok(Double.isFinite(actual)&&Math.abs(actual-expected)<=tolerance,
                message+" actual="+actual+" expected="+expected);
    }
    private static void rejects(Runnable test,String message){
        boolean failed=false;
        try{test.run();}catch(IllegalArgumentException expected){failed=true;}
        ok(failed,message);
    }
    private static TemporalSpatialResearchV6.PoseReading p(
            String id,double x,double y,long time,double response){
        return new TemporalSpatialResearchV6.PoseReading(id,x,y,time,response);
    }
    public static void main(String[] args){
        // 08.5: window weighting, no fake sample independence / confidence.
        double[] fused=TemporalSpatialResearchV6.temporalEnsemble(
            new double[][]{{.2,.8},{.8,.2}},new double[]{1,3});
        near(fused[0],.65,1e-12,"temporal window 0");
        near(fused[1],.35,1e-12,"temporal window 1");
        rejects(()->TemporalSpatialResearchV6.temporalEnsemble(
            new double[][]{{.2,.8},{.8,.2}},new double[]{0,0}),
            "no invented weight from zero mass");
        rejects(()->TemporalSpatialResearchV6.temporalEnsemble(
            new double[][]{{.2,.8},{.8}},new double[]{1,1}),
            "heterogeneous class-score dimensions rejected");

        // 08.7: independently paired domain audit, not Tank transfer validation.
        var audit=TemporalSpatialResearchV6.simToRealAudit(
            new double[][]{{1,2},{3,4}},new double[][]{{2,0},{4,2}});
        ok(audit.pairedRows==2&&audit.features==2,"paired original samples");
        near(audit.meanSignedDifference[0],1,1e-12,"transfer bias");
        near(audit.meanSignedDifference[1],-2,1e-12,"second channel bias");
        near(audit.rootMeanSquaredDifference[0],1,1e-12,"transfer RMSE");
        rejects(()->TemporalSpatialResearchV6.simToRealAudit(
            new double[][]{{1},{2}},new double[][]{{1}}),
            "unpaired samples not fabricated");

        // 08.9: actual NNLS for 3 independent source patterns.
        var mixed=TemporalSpatialResearchV6.unmix(
            new double[]{2,3,4},new double[][]{
                {1,0,0},{0,1,0},{0,0,1}},100,1e-12);
        near(mixed.nonnegativeCoefficients[0],2,1e-12,"unmix coefficient a");
        near(mixed.nonnegativeCoefficients[1],3,1e-12,"unmix coefficient b");
        near(mixed.nonnegativeCoefficients[2],4,1e-12,"unmix coefficient c");
        near(mixed.residualSquared,0,1e-12,"independent normalized source mix");
        ok(!mixed.chemicalMixtureConfirmed,"NNLS not physical mixture identity");
        var boundary=TemporalSpatialResearchV6.unmix(
            new double[]{3,0},new double[][]{{1,0},{0,1}},100,1e-12);
        near(boundary.nonnegativeCoefficients[1],0,1e-12,
            "nonnegative boundary source");
        rejects(()->TemporalSpatialResearchV6.unmix(
            new double[]{2,3},new double[][]{{0,0},{1,1}},10,.1),
            "zero source fingerprint cannot be fit");

        // 08.10: two targets with different single relevant training features.
        double[][] xs={{0,3},{1,2},{2,1},{3,0},{4,-1}};
        double[][] ys={{1,6},{3,4},{5,2},{7,0},{9,-2}};
        var multi=TemporalSpatialResearchV6.fitIndependentTargets(xs,ys,1e-9);
        ok(multi.targets==2&&multi.trainingRows==5&&multi.featureCount==2,
            "source multi-target regression dimensions");
        double[] outputs=multi.predict(new double[]{2.5,.5});
        near(outputs[0],6,1e-7,"first response training fit");
        near(outputs[1],1,1e-7,"second response training fit");
        rejects(()->multi.predict(new double[]{1}),
            "invalid target feature space dimensions");
        rejects(()->TemporalSpatialResearchV6.fitIndependentTargets(xs,ys,-1),
            "negative ridge not silently accepted");

        // 08.12: simple additive assumed product; doesn't establish chemistry.
        var complex=TemporalSpatialResearchV6.complexResponse(
            new double[][]{{1,0},{0,2}},new double[]{2,3},new double[]{2,7});
        near(complex.additiveExpectation[0],2,1e-12,"additive source 1");
        near(complex.additiveExpectation[1],6,1e-12,"additive source 2");
        near(complex.observedResidual[1],1,1e-12,"nonlinear response residual");
        near(complex.residualSquared,1,1e-12,"complex additive error");
        ok(!complex.identifiedProduct,"not a product ID");
        rejects(()->TemporalSpatialResearchV6.complexResponse(
            new double[][]{{1,0},{0,2}},new double[]{-1,3},new double[]{2,7}),
            "negative unverified source fraction forbidden");

        // 09.1: strictly given spatial observations, no fabricated motion.
        var trace=TemporalSpatialResearchV6.olfactionTrace(Arrays.asList(
            p("A",0,0,0,1),p("B",2,0,500,2),p("C",0,2,1000,4)));
        ok(trace.size()==3,"three pose observations retained");
        ok(trace.get(2).measurementId.equals("C"),"source IDs stable");
        boolean immutable=false;
        try {trace.add(p("D",1,1,1500,3));}
        catch(UnsupportedOperationException expected){immutable=true;}
        ok(immutable,"immutable trace should reject edits");
        rejects(()->TemporalSpatialResearchV6.olfactionTrace(Arrays.asList(
            p("A",0,0,10,1),p("B",1,1,9,2))),
            "nonmonotonic source time rejected");

        // 09.2: IDW same-point observation and symmetric positions.
        var origin=TemporalSpatialResearchV6.idwMap(trace,0,0,4,2);
        near(origin.value,1,1e-12,"exact point should use source observation");
        var between=TemporalSpatialResearchV6.idwMap(
            Arrays.asList(p("Q",0,0,0,2),p("W",2,0,0,6)),1,0,5,2);
        near(between.value,4,1e-12,"symmetric source IDW");
        ok(between.contributingObservations==2,"map source count");
        rejects(()->TemporalSpatialResearchV6.idwMap(trace,100,100,1,2),
            "no source support, no fake map value");

        // 09.3: centroid + observed peak, never physical gas-source proof.
        var localized=TemporalSpatialResearchV6.sourceLocalization(trace,0);
        near(localized.x,4.0/7,1e-12,"weighted x");
        near(localized.y,8.0/7,1e-12,"weighted y");
        ok(localized.peakMeasurementId.equals("C"),"strongest original response");
        ok(!localized.physicalSourceConfirmed,"centroid isn't source certification");
        rejects(()->TemporalSpatialResearchV6.sourceLocalization(trace,10),
            "no positive response above blank/reference");

        // 09.4: geometric sample proposal only, without any drive command.
        var site=TemporalSpatialResearchV6.proposeSampleSite(
            Arrays.asList(p("O",0,0,0,1)),
            new double[][]{{1,0},{5,0},{2,2}});
        ok(site.candidateIndex==1,"farthest sampled-grid candidate");
        near(site.minSeparation,5,1e-12,"geometric source-distance coverage");
        rejects(()->TemporalSpatialResearchV6.proposeSampleSite(
            trace,new double[][]{{Double.NaN,1}}),"invalid proposed 2D site");

        // 09.5: Gaussian spatial/temporal kernel with explicit scales.
        var st=TemporalSpatialResearchV6.spatiotemporal(
            Arrays.asList(p("T0",0,0,500,2),p("T1",0,0,500,6)),
            0,0,500,1,100);
        near(st.value,4,1e-12,"identical space/time observation mean");
        ok(st.contributingObservations==2,"two source windows in spatiotemporal fit");
        rejects(()->TemporalSpatialResearchV6.spatiotemporal(
            trace,0,0,0,0,100),"zero spatial bandwidth cannot fabricate model");
        rejects(()->TemporalSpatialResearchV6.spatiotemporal(
            Arrays.asList(p("P",1000,1000,10000,2)),
            0,0,0,.1,.1),"underflow with no supporting source returns no estimate");

        System.out.println("PASS V6FifthTenTechniques: "+assertions+
            " assertions, 10 research-stage temporal/spatial algorithms, Tank G3=0");
    }
}
