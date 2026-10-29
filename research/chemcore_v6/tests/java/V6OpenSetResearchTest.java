package com.hazrob.tank.chemcore.v6;

import java.util.Arrays;

/** N05-H fail-closed calibration and hypothesis-preservation unit tests. */
public final class V6OpenSetResearchTest {
    private static int n=0;
    private static void ok(boolean condition,String why){
        n++;if(!condition)throw new AssertionError("N05-H assertion "+n+": "+why);
    }
    private static void near(double value,double expected,double tol,String why){
        ok(Double.isFinite(value)&&Math.abs(value-expected)<=tol,
            why+" observed="+value+" expected="+expected);
    }
    private static void rejects(Runnable r,String why){
        boolean rejected=false;
        try{r.run();}catch(IllegalArgumentException x){rejected=true;}
        ok(rejected,why);
    }
    public static void main(String[] ignored){
        var t=OpenSetResearchV6.fitCalibrationThreshold(
            new double[]{.9,.8,.7},new double[]{.1,.2,.3});
        near(t.calibrationBalancedAcceptance,1,1e-12,"perfect source calibration balanced separation");
        near(t.calibrationKnownRetained,1,1e-12,"known reference retained");
        near(t.calibrationUnknownRejected,1,1e-12,"unseen calibration negative rejected");
        ok(t.minimumScore>=.7&&t.minimumScore<=.8,
           "threshold selected ONLY inside source calibration interval");
        ok(t.calibrationKnownRows==3&&t.calibrationUnknownRows==3,
           "source provenance count");
        ok(t.sourceOnly&&!t.validatedForTank,"no physical Tank promotion");
        var t2=OpenSetResearchV6.fitCalibrationThreshold(
            new double[]{.7,.5},new double[]{.2,.6});
        ok(t2.minimumScore>0,"fitted research threshold not fixed fake constant");
        ok(t2.calibrationBalancedAcceptance>=.5,"calibration retains minimax candidates");
        var allEqual=OpenSetResearchV6.fitCalibrationThreshold(
            new double[]{.5,.5},new double[]{.5,.5});
        near(allEqual.calibrationBalancedAcceptance,.5,1e-12,
            "indistinguishable known/unknown cannot be separated");
        ok(allEqual.minimumScore>.5,
           "strict abstention on ties when equally non-informative");
        rejects(()->OpenSetResearchV6.fitCalibrationThreshold(
            new double[]{},new double[]{.1}),"no known calibration examples");
        rejects(()->OpenSetResearchV6.fitCalibrationThreshold(
            new double[]{.1},new double[]{}),"no independent unknown-negative examples");
        rejects(()->OpenSetResearchV6.fitCalibrationThreshold(
            new double[]{Double.NaN},new double[]{.1}),"invalid calibration score");
        rejects(()->OpenSetResearchV6.fitCalibrationThreshold(
            new double[]{.4},new double[]{Double.POSITIVE_INFINITY}),
            "invalid nonfinite unknown score");
        rejects(()->OpenSetResearchV6.fitCalibrationThreshold(
            new double[]{1.1},new double[]{.1}),"impossible positive source score");
        double[][] features={{-2},{-1},{-.5},{.5},{1},{2}};
        String[] labels={"A","A","A","B","B","B"};
        var model=ClassicalLearningV6.fitSoftmax(
            new ClassicalLearningV6.Dataset(features,labels),200,.2,.01);
        var positive=model.scores(new double[]{2});
        var negative=model.scores(new double[]{-2});
        ok(positive.size()==2&&negative.size()==2,"complete two class scores");
        ok(positive.get(0).label.equals("B"),"source learned candidate B");
        ok(negative.get(0).label.equals("A"),"source learned candidate A");
        var allowAll=OpenSetResearchV6.fitCalibrationThreshold(
            new double[]{.9},new double[]{.1});
        var d=OpenSetResearchV6.apply(allowAll,positive);
        ok(d.allCandidateLabels.size()==2&&d.allCandidateRelativeScores.length==2,
           "all candidate hypotheses remain in ledger");
        ok(d.candidateLabel.equals("B"),"raw source candidate unaffected by rejection gate");
        ok(d.reportedLabel.equals(d.candidateLabel)||d.reportedLabel.equals("UNKNOWN"),
           "reporting is recognized or abstention only");
        near(d.maximumRelativeScore,positive.get(0).score,1e-12,
             "unmodified source maximum score");
        ok(!d.sourceCalibratedToTank,"SOURCE_ONLY scoring never certified for Tank");
        var deny=OpenSetResearchV6.fitCalibrationThreshold(
            new double[]{1},new double[]{0});
        // Model score cannot reach one exactly for these finite source inputs.
        var blocked=OpenSetResearchV6.apply(deny,positive);
        ok(blocked.unknown,"strict calibration source threshold must abstain");
        ok(blocked.reportedLabel.equals("UNKNOWN"),"candidate withheld from reported identity");
        ok(blocked.candidateLabel.equals("B")&&
           blocked.allCandidateRelativeScores.length==2,
           "abstention does not delete any source candidate");
        rejects(()->OpenSetResearchV6.apply(null,positive),
           "must supply source calibration before reporting");
        rejects(()->OpenSetResearchV6.apply(t,Arrays.asList()),
           "cannot manufacture a missing classifier score");
        System.out.println("PASS V6OpenSetResearchTest: "+n+
           " JDK assertions, source calibration separated and ALL candidates preserved; G3=0.");
    }
}
