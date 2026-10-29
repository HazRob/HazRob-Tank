package com.hazrob.tank.chemcore.v6;

import java.util.Arrays;
import java.util.List;

/** N03-F: 10 source-only mathematical prototypes, positive and negative cases. */
public final class V6SixthTenTechniquesTest {
    private static int tests=0;
    private static void ok(boolean b,String msg){
        tests++;if(!b)throw new AssertionError(tests+": "+msg);
    }
    private static void near(double a,double b,double tolerance,String msg){
        ok(Double.isFinite(a)&&Math.abs(a-b)<=tolerance,
           msg+" actual "+a+" expected "+b);
    }
    private static void reject(Runnable f,String msg){
        boolean caught=false;try{f.run();}catch(IllegalArgumentException e){caught=true;}
        ok(caught,msg);
    }
    public static void main(String[] args) {
        // 04.5 actual SGD linear hinge-loss SVM, source-only.
        double[][] x={{-3,-3},{-2,-2},{-1,-1},{-1.5,-1.5},
                      {1,1},{1.5,1.5},{2,2},{3,3}};
        int[] y={0,0,0,0,1,1,1,1};
        var svm=SupervisedLearningResearchV6.fitLinearSvm(x,y,120,.04,.02);
        ok(svm.sourceTrainingRows==8&&svm.epochs==120,"SVM provenance");
        ok(svm.margin(new double[]{-2,-2})<0,"SVM negative support");
        ok(svm.margin(new double[]{2,2})>0,"SVM positive support");
        ok(svm.predict(new double[]{-4,-4})==0,"SVM inference negative");
        ok(svm.predict(new double[]{4,4})==1,"SVM inference positive");
        reject(()->SupervisedLearningResearchV6.fitLinearSvm(x,y,0,.1,.01),
            "SVM must have actual optimization");
        reject(()->svm.margin(new double[]{1}),"SVM axes must match");

        // 04.6 seeded bootstrap classification stump forest.
        var forest=SupervisedLearningResearchV6.fitBootstrapStumps(x,y,101,5L);
        var replay=SupervisedLearningResearchV6.fitBootstrapStumps(x,y,101,5L);
        near(forest.positiveVoteFraction(new double[]{2,2}),
             replay.positiveVoteFraction(new double[]{2,2}),0,"seed repeatability");
        ok(forest.sourceTrainingRows==8&&forest.trees==101,"bootstrap trees");
        ok(forest.predict(new double[]{3,3})==1,"forest positive");
        ok(forest.predict(new double[]{-3,-3})==0,"forest negative");
        ok(forest.positiveVoteFraction(new double[]{2,2})<=1 &&
           forest.positiveVoteFraction(new double[]{2,2})>=0,
           "uncalibrated voting fraction");
        reject(()->SupervisedLearningResearchV6.fitBootstrapStumps(x,y,0,5),
            "forest must contain source-fitted trees");

        // 04.7 actual gradient/Hessian boosting binary logistic objective.
        var boost=SupervisedLearningResearchV6.fitNewtonBoost(x,y,8,.4,1);
        ok(boost.rounds==8&&boost.featureCount==2,"second order stump stages");
        ok(boost.sourceScore(new double[]{3,3})>.7,"boost positive score");
        ok(boost.sourceScore(new double[]{-3,-3})<.3,"boost negative score");
        reject(()->SupervisedLearningResearchV6.fitNewtonBoost(x,y,10,0,1),
            "boost requires valid shrinkage rate");

        // 04.8 batch-backprop tanh MLP, reproducible seed.
        var mlp=SupervisedLearningResearchV6.fitMlp(x,y,4,1000,.2,123L);
        var mlpReplay=SupervisedLearningResearchV6.fitMlp(x,y,4,1000,.2,123L);
        ok(mlp.sourceTrainingRows==8&&mlp.hiddenUnits==4,"MLP trained hidden layer");
        ok(mlp.sourceScore(new double[]{2,2})>.75,"nonlinear trained source MLP positive");
        ok(mlp.sourceScore(new double[]{-2,-2})<.25,"nonlinear trained source MLP negative");
        near(mlp.sourceScore(new double[]{1,1}),
             mlpReplay.sourceScore(new double[]{1,1}),0,
             "seeded MLP deterministic parameters");
        reject(()->mlp.sourceScore(new double[]{1}),"MLP source shape guard");
        reject(()->SupervisedLearningResearchV6.fitMlp(x,y,0,100,.1,1),
            "actual hidden layer required");

        // 04.9 physical curve must be provided; residual fitted on source.
        double[] ratios={1,2,4,8};
        double[] refs={Math.exp(1)*1,Math.exp(1)*4,
                       Math.exp(1)*16,Math.exp(1)*64};
        var corrected=SupervisedLearningResearchV6.fitPhysicsResidual(
            ratios,refs,1,1,1e-9);
        near(corrected.researchConcentration(3),Math.exp(1)*9,1e-7,
             "source-specific log residual hybrid");
        ok(corrected.sourceTrainingRows==4,"physical curve paired references");
        reject(()->corrected.researchConcentration(0),
            "no fabricated physical ratio if denominator zero");
        reject(()->SupervisedLearningResearchV6.fitPhysicsResidual(
                new double[]{1,1,1},new double[]{2,3,4},1,0,1),
            "constant source curve cannot identify residual slope");

        // 06.8 exact finite fixed-background interventional Shapley.
        var shap=AdaptationEvidenceResearchV6.fixedReferenceShapley(
            f->2*f[0]+3*f[1],new double[]{0,0},new double[]{2,4});
        near(shap.contributions[0],4,1e-12,"exact source Shapley first");
        near(shap.contributions[1],12,1e-12,"exact source Shapley second");
        near(shap.observationScore-shap.baselineScore,
             shap.contributions[0]+shap.contributions[1],1e-12,
             "Shapley efficiency identity");
        ok(!shap.causalExplanationValidated,"not causal SHAP interpretation");
        var interaction=AdaptationEvidenceResearchV6.fixedReferenceShapley(
            f->f[0]*f[1],new double[]{0,0},new double[]{2,4});
        near(interaction.contributions[0],4,1e-12,"interaction split first");
        near(interaction.contributions[1],4,1e-12,"interaction split second");
        reject(()->AdaptationEvidenceResearchV6.fixedReferenceShapley(
            f->Double.NaN,new double[]{0},new double[]{1}),
            "undefined explanation not silently accepted");

        // 07.6 diagonal domain moment transfer using separate source datasets.
        var domains=AdaptationEvidenceResearchV6.fitDiagonalDomainTransfer(
            new double[][]{{0,0},{2,4},{4,8}},
            new double[][]{{10,100},{12,104},{14,108}});
        var aligned=domains.targetIntoReference(new double[]{11,102});
        near(aligned[0],1,1e-12,"target-domain first axis");
        near(aligned[1],2,1e-12,"target-domain second axis");
        ok(domains.referenceRows==3&&domains.targetRows==3,"domain provenance count");
        reject(()->AdaptationEvidenceResearchV6.fitDiagonalDomainTransfer(
            new double[][]{{1,0},{1,2}},new double[][]{{4,6},{5,7}}),
            "zero reference variance cannot be manufactured");

        // 07.7 prior-constrained closed-form ridge fine-tuning.
        var transfer=AdaptationEvidenceResearchV6.fineTuneRidge(
            new double[][]{{0},{1},{2},{3}},new double[]{1,3,5,7},
            new double[]{0,0},1e-8);
        near(transfer.predict(new double[]{4}),9,1e-6,"prior-centred source fine tuning");
        ok(transfer.fineTuneSourceRows==4&&transfer.featureCount==1,
            "actual source fine-tune observations");
        reject(()->AdaptationEvidenceResearchV6.fineTuneRidge(
            new double[][]{{0},{1}},new double[]{1,2},
            new double[]{1},.1),"pretrained weights must include bias");

        // 07.9 incremental elastic regularizer step: no automatic promotion.
        var consolidated=AdaptationEvidenceResearchV6.consolidate(
            new double[]{0,0},new double[]{0,0},new double[]{2,3},
            new double[]{1,0},1,.2,1);
        near(consolidated.updatedWeights[0],.2,1e-12,"incremental gradient update");
        near(consolidated.updatedWeights[1],0,1e-12,"unobserved coefficient frozen");
        ok(consolidated.squaredErrorAfter<consolidated.squaredErrorBefore,
            "single-step regression training signal");
        ok(!consolidated.safeToPromoteWithoutHoldout,
            "continued learning not automatically validated");
        var elastic=AdaptationEvidenceResearchV6.consolidate(
            new double[]{1,0},new double[]{0,0},new double[]{2,1},
            new double[]{0,1},0,.1,1);
        near(elastic.updatedWeights[0],.8,1e-12,
            "explicit consolidation penalty toward frozen prior");
        reject(()->AdaptationEvidenceResearchV6.consolidate(
            new double[]{1},new double[]{1},new double[]{-1},
            new double[]{1},1,.1,1),"negative importance invalid");

        // 10.1 source-qualified negative evidence: never delete a candidate.
        var candidates=Arrays.asList(
            new AdaptationEvidenceResearchV6.Hypothesis("hydrogen","hydrogen"),
            new AdaptationEvidenceResearchV6.Hypothesis("ethanol","alcohol"),
            new AdaptationEvidenceResearchV6.Hypothesis("UNKNOWN","unknown")
        );
        var notCalibrated=Arrays.asList(
            new AdaptationEvidenceResearchV6.NegativeSensorObservation(
                "MQ8","lab-unverified-17","hydrogen",false,true,true,5));
        var first=AdaptationEvidenceResearchV6.assessNegativeEvidence(
            candidates,notCalibrated,3);
        ok(first.size()==3,"all candidates incl UNKNOWN preserved");
        ok(!first.get(0).qualifiedNegativeEvidence,
            "unvalidated Tank MQ8 zero cannot exclude hydrogen");
        ok(!first.get(2).qualifiedNegativeEvidence,
            "UNKNOWN hypothesis never disappears");
        var qualified=Arrays.asList(
            new AdaptationEvidenceResearchV6.NegativeSensorObservation(
                "MQ8","tank-g3-hypothetical-only","hydrogen",true,true,true,5),
            new AdaptationEvidenceResearchV6.NegativeSensorObservation(
                "MQ7","low-data","carbon-oxide",true,true,true,1));
        var third=AdaptationEvidenceResearchV6.assessNegativeEvidence(
            candidates,qualified,3);
        ok(third.size()==3&&third.get(0).qualifiedNegativeEvidence,
           "qualified hypothetical source observation flags family only");
        ok(third.get(0).supportingSensorIds.size()==1&&
           third.get(0).sourceProvenanceIds.get(0).equals("tank-g3-hypothetical-only"),
           "audit sensor provenance remains attached");
        ok(!third.get(1).qualifiedNegativeEvidence,
            "alcohol not rejected by unrelated sensor family");
        ok(!third.get(2).qualifiedNegativeEvidence,
            "open-set candidate retained under negative result");
        var tooShort=AdaptationEvidenceResearchV6.assessNegativeEvidence(
            candidates,qualified,7);
        ok(!tooShort.get(0).qualifiedNegativeEvidence,
            "insufficient independent stable measurements cannot gate");
        var offline=Arrays.asList(new AdaptationEvidenceResearchV6.NegativeSensorObservation(
            "MQ8","offline","hydrogen",true,false,true,12));
        ok(!AdaptationEvidenceResearchV6.assessNegativeEvidence(
            candidates,offline,3).get(0).qualifiedNegativeEvidence,
            "disconnected sensor cannot exclude gas family");
        var noLimit=Arrays.asList(new AdaptationEvidenceResearchV6.NegativeSensorObservation(
            "MQ8","nonqualified-LOD","hydrogen",true,true,false,12));
        ok(!AdaptationEvidenceResearchV6.assessNegativeEvidence(
            candidates,noLimit,3).get(0).qualifiedNegativeEvidence,
            "unknown calibrated detection-limit status forbids exclusion");
        reject(()->AdaptationEvidenceResearchV6.assessNegativeEvidence(
            candidates,qualified,0),"stable validation count must be positive");
        System.out.println("PASS V6SixthTenTechniques: "+tests+
            " JDK assertions, 10 partial source-only research methods, 0/80 Tank G3");
    }
}
