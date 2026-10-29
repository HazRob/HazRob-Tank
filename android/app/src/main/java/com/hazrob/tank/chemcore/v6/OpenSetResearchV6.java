package com.hazrob.tank.chemcore.v6;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * N05-H source research open-set score threshold, fitted on an explicitly
 * separated calibration partition with KNOWN + UNKNOWN source groups.
 * An unknown TEST gas must NOT occur in train nor calibration source labels.
 *
 * Source-only, never a Tank safety-grade gas identity/ppm/LOD threshold.
 * Retain complete hypothesis scores even when final reporting abstains.
 */
public final class OpenSetResearchV6 {
    private OpenSetResearchV6(){}
    private static void require(boolean condition,String reason){
        if(!condition)throw new IllegalArgumentException(reason);
    }
    private static void score(double s) {
        require(Double.isFinite(s)&&s>=0&&s<=1,
            "finite source-relative maximum score in [0,1] required");
    }
    public static final class Threshold {
        public final double minimumScore,calibrationBalancedAcceptance,
                            calibrationKnownRetained,calibrationUnknownRejected;
        public final int calibrationKnownRows,calibrationUnknownRows;
        public final boolean sourceOnly,validatedForTank;
        private Threshold(double cutoff,double objective,double known,double unknown,
                          int nKnown,int nUnknown){
            minimumScore=cutoff;calibrationBalancedAcceptance=objective;
            calibrationKnownRetained=known;calibrationUnknownRejected=unknown;
            calibrationKnownRows=nKnown;calibrationUnknownRows=nUnknown;
            sourceOnly=true;validatedForTank=false;
        }
    }
    /**
     * Train nothing here: threshold search fits ONLY calibration scores.
     * Objective: 0.5*(fraction accepted known + fraction rejected unknown).
     * Candidate thresholds exhaust feasible score discontinuities plus
     * the all-reject threshold; ties favor stricter rejection. The objective
     * is a calibration research diagnostic, NOT an unknown-gas guarantee.
     */
    public static Threshold fitCalibrationThreshold(double[] knownMaximumScores,
                                                    double[] unknownMaximumScores){
        require(knownMaximumScores!=null&&unknownMaximumScores!=null&&
                knownMaximumScores.length>0&&unknownMaximumScores.length>0,
                "both known-source and genuinely independent unknown-source calibration required");
        double[] all=new double[knownMaximumScores.length+unknownMaximumScores.length];
        int m=0;
        for(double s:knownMaximumScores){score(s);all[m++]=s;}
        for(double s:unknownMaximumScores){score(s);all[m++]=s;}
        Arrays.sort(all);
        List<Double> candidates=new ArrayList<>();
        candidates.add(0.0); // all known/unknown accepted
        for(int i=0;i<all.length;i++){
            if(i==0||all[i]!=all[i-1])candidates.add(all[i]);
            if(i<all.length-1&&all[i+1]>all[i])
                candidates.add(all[i]+(all[i+1]-all[i])/2.0);
        }
        candidates.add(Math.nextUp(1.0)); // all rejected
        double chosen=Double.NaN,best=-1,retained=0,rejected=0;
        for(double candidate:candidates){
            int kept=0,denied=0;
            for(double s:knownMaximumScores)if(s>=candidate)kept++;
            for(double s:unknownMaximumScores)if(s<candidate)denied++;
            double known=kept/(double)knownMaximumScores.length;
            double unknown=denied/(double)unknownMaximumScores.length;
            double objective=.5*(known+unknown);
            if(objective>best+1e-12||
                (Math.abs(objective-best)<=1e-12&&candidate>chosen)){
                chosen=candidate;best=objective;retained=known;rejected=unknown;
            }
        }
        return new Threshold(chosen,best,retained,rejected,
                knownMaximumScores.length,unknownMaximumScores.length);
    }
    public static final class Decision {
        public final boolean unknown;
        public final String candidateLabel;
        public final String reportedLabel;
        public final double maximumRelativeScore,selectedSourceThreshold;
        public final boolean sourceCalibratedToTank;
        public final List<String> allCandidateLabels;
        public final double[] allCandidateRelativeScores;
        private Decision(boolean abstain,String candidate,double highest,double threshold,
                         List<String> labels,double[] scores){
            unknown=abstain;candidateLabel=candidate;
            reportedLabel=abstain?"UNKNOWN":candidate;
            maximumRelativeScore=highest;selectedSourceThreshold=threshold;
            allCandidateLabels=Collections.unmodifiableList(new ArrayList<>(labels));
            allCandidateRelativeScores=scores.clone();
            sourceCalibratedToTank=false;
        }
    }
    /** Preserve complete sorted class score vector. A finite research score
     * never becomes a certified sensor gas identity or a calibrated ppm.
     */
    public static Decision apply(Threshold sourceCalibration,
                                 List<ClassicalLearningV6.ClassScore> rankedScores){
        require(sourceCalibration!=null&&rankedScores!=null&&
                !rankedScores.isEmpty(),"calibration and complete source model rank required");
        List<String> labels=new ArrayList<>();
        double[] scores=new double[rankedScores.size()];
        for(int i=0;i<rankedScores.size();i++){
            ClassicalLearningV6.ClassScore s=rankedScores.get(i);
            require(s!=null&&s.label!=null&&!s.label.isEmpty(),
                    "source candidate label required");
            score(s.score);labels.add(s.label);scores[i]=s.score;
            for(int j=0;j<i;j++)require(!labels.get(j).equals(s.label),
                "duplicate source candidate");
        }
        double best=Double.NEGATIVE_INFINITY;
        String candidate=null;
        for(int i=0;i<scores.length;i++)if(scores[i]>best){
            best=scores[i];candidate=labels.get(i);
        }
        return new Decision(best<sourceCalibration.minimumScore,
            candidate,best,sourceCalibration.minimumScore,labels,scores);
    }
}
