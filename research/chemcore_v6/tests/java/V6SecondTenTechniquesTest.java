package com.hazrob.tank.chemcore.v6;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Explicit scientific fixtures for V6-N03-B, NOT Tank gas-device validation. */
public final class V6SecondTenTechniquesTest {
    static int assertions=0;
    static void ok(boolean value,String label) {
        assertions++;if(!value)throw new AssertionError(assertions+": "+label);
    }
    static void near(double observed,double expected,double eps,String label){
        ok(Double.isFinite(observed)&&Math.abs(observed-expected)<=eps,
           label+": actual "+observed+", expected "+expected);
    }
    static String first(List<ClassicalLearningV6.ClassScore> results){
        return results.get(0).label;
    }
    public static void main(String[] args){
        // 01.5: user-provided environmental sensitivity is explicit.
        var env=CalibrationResearchV6.correctEnvironment(2.0,30,60,20,40,
                                                         0.01,0.002);
        ok(env.usable,"explicit temperature and RH coefficients");
        near(env.correctedRsOverR0,2/Math.pow(10,0.14),1e-12,
             "log10 environmental sensitivity");
        near(CalibrationResearchV6.correctEnvironment(
            3.2,25,50,25,50,0.8,0.9).correctedRsOverR0,3.2,1e-12,
            "reference environmental conditions should not modify ratio");
        ok(!CalibrationResearchV6.correctEnvironment(
            2,30,50,20,40,Double.NaN,0).usable,
            "missing source coefficient => no invented compensation");
        ok(!CalibrationResearchV6.correctEnvironment(
            -1,30,50,20,40,0,0).usable,"invalid source ratio excluded");

        // 05.1: one known ppm anchor + externally specified exponent.
        var anchor=new CalibrationResearchV6.Point(100,0.2);
        var one=CalibrationResearchV6.singlePoint(
            "MQ135","SOURCE_EXPERIMENT_A",anchor,-0.5);
        ok(one.fitted&&one.usedReferencePoints==1,"one-point a with known b");
        near(one.a,2,1e-12,"anchor coefficient a");
        near(one.estimatePpm(0.2),100,1e-7,"anchor inverse ppm");
        ok(!one.measuredOnTank,"external source not Tank calibration");
        var missingExponent=CalibrationResearchV6.singlePoint(
            "MQ135","LITERATURE",anchor,Double.NaN);
        ok(!missingExponent.fitted&&Double.isNaN(missingExponent.estimatePpm(0.2)),
           "one point without b cannot create a curve");

        // 05.2: actual multipoint slope regression, including bad numeric rows.
        List<CalibrationResearchV6.Point> points=new ArrayList<>();
        for(double ppm:new double[]{10,20,50,100,200})
            points.add(new CalibrationResearchV6.Point(ppm,2*Math.pow(ppm,-0.5)));
        points.add(new CalibrationResearchV6.Point(-1,0.4));
        points.add(new CalibrationResearchV6.Point(150,Double.NaN));
        var fit=CalibrationResearchV6.multipoint(
            "MQ135","INDEPENDENT_SOURCE_FIXTURE",points);
        ok(fit.fitted&&fit.totalReferencePoints==7&&fit.usedReferencePoints==5,
           "only numerically corrupt rows cannot be used in a log regression");
        near(fit.a,2,1e-11,"log10-law intercept");
        near(fit.b,-0.5,1e-12,"log10-law exponent");
        near(fit.rootMeanSquareLogResidual,0,1e-12,"exact synthetic source");
        near(fit.estimatePpm(2*Math.pow(50,-0.5)),50,1e-8,"independent inverse");
        near(fit.minimumReferencePpm,10,1e-12,"source lower range");
        near(fit.maximumReferencePpm,200,1e-12,"source upper range");
        var noSpread=CalibrationResearchV6.multipoint("MQ2","fixture",
            Arrays.asList(new CalibrationResearchV6.Point(50,0.8),
                          new CalibrationResearchV6.Point(50,0.7)));
        ok(!noSpread.fitted,"repeated concentration alone cannot fit exponent");

        // 05.7: keep every source-specific candidate estimate available.
        var response=Engine.analyze(Arrays.asList(
            new Engine.Channel("MQ135","device-mq135",42000,10000),
            new Engine.Channel("MQ2","device-mq2",15000,10000)));
        var conditional=CalibrationResearchV6.conditionalEstimates(response);
        ok(!conditional.isEmpty(),"conditional estimates remain source-conditional");
        int retained=0;
        for(var item:conditional){
            retained+=item.sourceFits;
            ok(Double.isFinite(item.medianResearchPpm)&&item.medianResearchPpm>0,
               "each source-group median computed");
            ok(item.minResearchPpm<=item.medianResearchPpm&&
               item.medianResearchPpm<=item.maxResearchPpm,
               "sorted min/median/max");
            ok(!item.validatedIdentity,"statistical estimates are not gas identity");
        }
        ok(retained==response.allSourceFits.size(),"no numeric fits hidden");
        ok(CalibrationResearchV6.conditionalEstimates(null).isEmpty(),"empty result safe");

        // 05.8: simple source-domain analytical blank/slope LOD and LOQ.
        var limit=CalibrationResearchV6.blankBasedLimits(
            new double[]{9,11,9,11},2.0);
        near(limit.blankSigmaSignal,Math.sqrt(4.0/3.0),1e-12,
            "unbiased blank sample standard deviation");
        near(limit.lodReferencePpm,3.3*Math.sqrt(4.0/3.0)/2.0,1e-12,
            "research LOD 3.3sigma/slope");
        near(limit.loqReferencePpm,10*Math.sqrt(4.0/3.0)/2.0,1e-12,
            "research LOQ 10sigma/slope");
        ok(!limit.physicallyVerified,"source blank cannot certify Tank");
        ok(!CalibrationResearchV6.blankBasedLimits(
            new double[]{1,1},0).numericallyDefined,"zero response slope undefined");
        ok(!CalibrationResearchV6.blankBasedLimits(
            new double[]{1},2).numericallyDefined,"one blank cannot infer sigma");

        // 03.1, 03.4, 04.3, 04.4: independent PCA and three classifier families.
        double[][] examples={
            {-3,-2},{-2,-3},{-4,-2},{-3,-4},
            {2,3},{3,2},{4,3},{3,4}
        };
        String[] names={"A","A","A","A","B","B","B","B"};
        var source=new ClassicalLearningV6.Dataset(examples,names);
        ok(source.validRows==8&&source.droppedNumericalRows==0,"eight source rows");
        var principal=ClassicalLearningV6.pca(examples,2);
        ok(principal.components.length==2&&principal.eigenvalues.length==2,
           "two actual PCA components");
        ok(principal.eigenvalues[0]>=principal.eigenvalues[1] &&
           principal.eigenvalues[0]>0,"PCA sorted principal variances");
        double projA=principal.transform(examples[0])[0];
        double projB=principal.transform(examples[7])[0];
        ok(Math.abs(projA-projB)>1,"source-separated PCA projection");
        near(principal.components[0][0]*principal.components[1][0]+
             principal.components[0][1]*principal.components[1][1],0,2e-3,
             "PCA axes nearly orthogonal");
        try{ClassicalLearningV6.pca(new double[][]{{1,2}},1);
            throw new AssertionError("one-row PCA accepted");}
        catch(IllegalArgumentException expected){assertions++;}

        var lda=ClassicalLearningV6.fitDiagonalLDA(source,0.05);
        ok(lda.classes.size()==2&&lda.sourceTrainingRows==8,
           "source class metadata captured");
        ok("A".equals(first(lda.score(new double[]{-3.4,-2.7}))),
           "diagonal LDA A");
        ok("B".equals(first(lda.score(new double[]{3.4,2.7}))),
           "diagonal LDA B");
        ok(lda.score(new double[]{1,1}).size()==2,
           "LDA lists all hypotheses, not only best class");

        var knn=ClassicalLearningV6.fitKnn(source);
        ok("A".equals(first(knn.rank(new double[]{-3.5,-2.0},3))),
           "KNN nearest A");
        ok("B".equals(first(knn.rank(new double[]{3.5,2.0},3))),
           "KNN nearest B");
        ok(knn.rank(new double[]{0,0},8).size()==2,"all class votes returned");
        near(knn.rank(new double[]{0,0},8).get(0).score,4,1e-10,
             "KNN source class votes");
        try{knn.rank(new double[]{1,2},9);
            throw new AssertionError("more neighbors than rows");}
        catch(IllegalArgumentException expected){assertions++;}

        var logistic=ClassicalLearningV6.fitSoftmax(source,500,0.15,0.005);
        ok(logistic.sourceTrainingRows==8&&logistic.trainingIterations==500,
           "actual source-gradient iterations");
        ok("A".equals(first(logistic.scores(new double[]{-3,-3}))),
           "multiclass softmax predicts A");
        ok("B".equals(first(logistic.scores(new double[]{3,3}))),
           "multiclass softmax predicts B");
        double probability=0;
        for(var score:logistic.scores(new double[]{0,0})) {
            ok(Double.isFinite(score.score)&&score.score>=0&&score.score<=1,
               "softmax math valid but not probability calibration");
            probability+=score.score;
        }
        near(probability,1,1e-12,"softmax sum to one");
        try{ClassicalLearningV6.fitSoftmax(source,200,0.2,-1);
            throw new AssertionError("invalid negative regularization accepted");}
        catch(IllegalArgumentException expected){assertions++;}

        // No leaking invalid numeric rows into source-training or scaler.
        var contaminated=new ClassicalLearningV6.Dataset(
            new double[][]{{-3,-3},{3,3},{Double.NaN,2}},
            new String[]{"A","B","A"});
        ok(contaminated.validRows==2&&contaminated.droppedNumericalRows==1,
           "invalid feature row reported and omitted");
        checkDatasetIsolation(contaminated);

        // 06.7: preserve all source estimate conflicts without exclusion.
        var conflicts=EvidenceConflictV6.compare(response);
        ok(conflicts.sourceCurveCount==response.allSourceFits.size(),
           "all curve hypotheses remain in evidence");
        ok(conflicts.sourceFamilies==2,"two distinct source families");
        ok(!conflicts.gasIdentityContradicted&&!conflicts.automaticallyDiscardedCandidates,
           "numerical disagreement not a hard identity rejection");
        ok(!conflicts.allComparablePairs.isEmpty(),"source curve pairs enumerated");
        int alternateMQ135=0;
        for(var p:conflicts.allComparablePairs){
            ok(p.log10PpmDifference>=0&&Double.isFinite(p.log10PpmDifference),
               "finite per-gas log ppm difference");
            if(p.differentSourceRoots&&p.samePhysicalSensor)alternateMQ135++;
        }
        ok(alternateMQ135>0,"same MQ135 compared across independent source families");
        ok(EvidenceConflictV6.compare(null).allComparablePairs.isEmpty(),
           "empty conflict registry creates no imaginary source");
        System.out.println("PASS V6SecondTenTechniques: "+assertions+
            " assertions, 10 additional independent research-stage techniques");
    }
    static void checkDatasetIsolation(ClassicalLearningV6.Dataset d) {
        ok(d.samples.length==2&&d.labels.length==2,"fit sees only finite training samples");
        ok(d.classes.size()==2,"both source classes retained");
    }
}
