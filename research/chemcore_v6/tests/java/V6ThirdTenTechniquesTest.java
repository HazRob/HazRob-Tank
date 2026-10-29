package com.hazrob.tank.chemcore.v6;

import java.util.Arrays;

/** V6-N03-C: isolated scientific unit tests. NOT Tank gas-device validation. */
public final class V6ThirdTenTechniquesTest {
    private static int checks=0;
    private static void ok(boolean x,String msg){
        checks++;if(!x)throw new AssertionError(msg);
    }
    private static void near(double a,double b,double delta,String msg){
        ok(Double.isFinite(a)&&Math.abs(a-b)<=delta,msg+": "+a+" vs "+b);
    }
    private static void throwsBad(Runnable r,String msg){
        boolean threw=false;try{r.run();}catch(IllegalArgumentException expected){threw=true;}
        ok(threw,msg);
    }
    public static void main(String[] args){
        // 04.10: macro recall/F1, explicit rejected predictions and unknown labels.
        var m=SourceValidationV6.benchmark(new String[]{"A","A","B","B"},
                new String[]{"A","UNKNOWN","A","B"},new String[]{"A","B"});
        ok(m.evaluated==4&&m.correct==2,"unknown prediction counted as miss");
        near(m.accuracy,.5,1e-12,"accuracy");
        near(m.balancedAccuracy,.5,1e-12,"macro recall");
        near(m.macroF1,(2.0/4+2.0/3)/2,1e-12,"macro F1");
        throwsBad(()->SourceValidationV6.benchmark(new String[]{"C"},
                  new String[]{"A"},new String[]{"A"}),"unknown true class rejected");

        // 06.2: source-conditional Bayes, no false device confidence.
        double[] bayes=SourceValidationV6.bayesianPosterior(
                new double[]{1e-250,1e-250},new double[]{1e-250,1e-250});
        near(bayes[0],.5,1e-12,"Bayes posterior");
        near(bayes[0]+bayes[1],1,1e-12,"normalized");
        throwsBad(()->SourceValidationV6.bayesianPosterior(
                  new double[]{.5,.5},new double[]{0,0}),"zero evidence undefined");

        // 06.3: temperature fit exclusively on calibration rows.
        var temp=SourceValidationV6.fitTemperature(
            new double[][]{{3,0},{0,3},{4,0},{0,4}},
            new int[]{0,1,0,1},new double[]{.5,1,2});
        near(temp.temperature,.5,1e-12,"best calibration NLL");
        ok(temp.calibrationRows==4,"all calibration rows");
        near(SourceValidationV6.softmaxTemperature(
                  new double[]{1e300,1e300},1e-300)[0],.5,1e-12,"stable softmax");
        throwsBad(()->SourceValidationV6.fitTemperature(
                  new double[][]{{1,2}},new int[]{2},new double[]{1}),
                  "calibration true class bounds");

        // 06.4: exact finite-sample conformal rank and conservative edge case.
        var set=SourceValidationV6.conformal(
            new double[]{.9,.8,.85,.7,.75},new double[]{.81,.1,.75},.25);
        near(set.nonconformityQuantile,.3,1e-12,"conformal quantile");
        ok(set.included[0]&&!set.included[1]&&set.included[2],"conformal set");
        var conservative=SourceValidationV6.conformal(
            new double[]{.9,.9},new double[]{0,1},.05);
        ok(Double.isInfinite(conservative.nonconformityQuantile),"n+1 rank");
        ok(conservative.included[0]&&conservative.included[1],"no fictitious coverage");
        throwsBad(()->SourceValidationV6.conformal(
                  new double[]{1.01},new double[]{.5},.1),"probability range");

        // 06.5: open-set threshold and margin; preserve numerical evidence.
        var tied=SourceValidationV6.openSet(new double[]{.7,.7,.1},.2,0);
        ok(tied.unknown&&tied.bestClassIndex==-1,"ambiguous tie");
        ok(!SourceValidationV6.openSet(new double[]{.8,.1},.2,.05).unknown,"clear best");
        ok(SourceValidationV6.openSet(new double[]{.1,.05},.2,0).unknown,"low score");

        // 06.6: OOD fit on source training only with explicit variance floor.
        var ood=SourceValidationV6.fitOod(new double[][]{{0,0},{2,0},{4,0}},.5);
        near(ood.mean[0],2,1e-12,"train-only mean");
        near(ood.variance[0],4,1e-12,"unbiased variance");
        near(ood.variance[1],.5,1e-12,"degenerate feature ridge");
        near(ood.squaredDistance(new double[]{2,0}),0,1e-12,"at source mean");
        ok(ood.squaredDistance(new double[]{20,0})>50,"OOD distance");
        throwsBad(()->ood.squaredDistance(new double[]{1}),"feature mismatch");

        // 06.9: abstention does NOT prune candidates.
        var denied=SourceValidationV6.abstain(new double[]{.5,.45},false,0,5,.4);
        ok(denied.abstained&&denied.reason.equals("UNVALIDATED_SOURCE"),"no G3 claim");
        ok(denied.unchangedScores.length==2,"all score hypotheses retained");
        ok(SourceValidationV6.abstain(new double[]{.5,.45},true,6,5,.4).abstained,
           "OOD abstention");
        ok(!SourceValidationV6.abstain(new double[]{.5,.45},true,1,5,.4).abstained,
           "conditional reportability");

        // 07.1/07.2/07.3: independent session, device and concentration holdout.
        var sessions=SourceValidationV6.sessionHoldout(
            new String[]{"S1","S1","S2","S2","S3"},"S2");
        ok(sessions.groupKind.equals("SESSION")&&sessions.testIndices.length==2,
           "session holdout");
        ok(Arrays.equals(sessions.trainingIndices,new int[]{0,1,4}),
           "no repeated-frame leakage");
        var device=SourceValidationV6.deviceHoldout(
            new String[]{"D1","D2","D1","D2"},"D1");
        ok(device.groupKind.equals("PHYSICAL_DEVICE"),"device type");
        ok(Arrays.equals(device.testIndices,new int[]{0,2}),"physical device held out");
        var concentration=SourceValidationV6.concentrationHoldout(
            new String[]{"50ppb","100ppb","50ppb","200ppb"},"50ppb");
        ok(concentration.groupKind.equals("SOURCE_CONCENTRATION"),"source level type");
        ok(Arrays.equals(concentration.testIndices,new int[]{0,2}),"whole level held out");
        throwsBad(()->SourceValidationV6.sessionHoldout(
                  new String[]{"only","only"},"only"),"train group required");
        throwsBad(()->SourceValidationV6.deviceHoldout(
                  new String[]{"d1","d2"},"d3"),"test group required");
        System.out.println("PASS V6ThirdTenTechniques: "+checks+
                           " JDK assertions; research only; Tank G3=0");
    }
}
