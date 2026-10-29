package com.hazrob.tank.chemcore.v6;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * N03-D additional ten independent research-stage numerical primitives.
 * 05.5 PLS1 (one latent dimension); 05.6 CART regression stump; 05.9
 * cross-concentration interpolation; 07.4 descriptive drift; 07.5 baseline
 * transfer; 07.8 entropy sample ordering; 07.10 latency distribution;
 * 08.6 reproducible source perturbation; 08.8 nonnegative two-component fit;
 * 08.11 interference residual under a specified target.
 *
 * No old ChemCore, Android or food source. No concentration or safety claim
 * is supported until independent physical Tank G3 validation.
 */
public final class SourceNumericsV6 {
    private SourceNumericsV6(){}
    private static final double EPS=1e-12;
    private static void check(boolean x,String reason) {
        if(!x)throw new IllegalArgumentException(reason);
    }
    private static void vector(double[] a) {
        check(a!=null&&a.length>0,"nonempty vector");
        for(double x:a)check(Double.isFinite(x),"nonfinite source number");
    }
    private static void positive(double[] a) {
        vector(a);for(double x:a)check(x>0,"positive source number required");
    }
    private static double dot(double[] a,double[] b) {
        double s=0;for(int i=0;i<a.length;i++)s+=a[i]*b[i];return s;
    }
    private static double[][] matrix(double[][] x,int minRows) {
        check(x!=null&&x.length>=minRows,"insufficient source rows");
        vector(x[0]);int d=x[0].length;
        for(double[] row:x){vector(row);check(row.length==d,"matrix dimension mismatch");}
        return x;
    }

    /** 05.5: mathematically defined one-latent-factor partial least squares
     * regression. Not a multicomponent PLS model or PLS-DA classifier.
     */
    public static final class Pls1 {
        public final double[] sourceMean,axis;
        public final double responseMean,responseSlope;
        public final int trainingRows;
        private Pls1(double[] mean,double[] w,double yMean,double slope,int n){
            sourceMean=mean;axis=w;responseMean=yMean;responseSlope=slope;trainingRows=n;
        }
        public double predict(double[] x) {
            vector(x);check(x.length==axis.length,"PLS source feature dimension");
            double t=0;for(int j=0;j<x.length;j++)t+=(x[j]-sourceMean[j])*axis[j];
            return responseMean+responseSlope*t;
        }
    }
    public static Pls1 fitPls1(double[][] features,double[] concentrations){
        matrix(features,3);vector(concentrations);
        check(features.length==concentrations.length,"PLS aligned source labels");
        int n=features.length,d=features[0].length;double[] mean=new double[d],w=new double[d];
        double ymean=0;
        for(int i=0;i<n;i++){
            ymean+=concentrations[i];
            for(int j=0;j<d;j++)mean[j]+=features[i][j];
        }
        ymean/=n;
        for(int j=0;j<d;j++)mean[j]/=n;
        for(int i=0;i<n;i++)for(int j=0;j<d;j++)
            w[j]+=(features[i][j]-mean[j])*(concentrations[i]-ymean);
        double norm=Math.sqrt(dot(w,w));
        check(norm>EPS&&Double.isFinite(norm),"unidentifiable PLS covariance");
        for(int j=0;j<d;j++)w[j]/=norm;
        double tt=0,ty=0;
        for(int i=0;i<n;i++){
            double t=0;
            for(int j=0;j<d;j++)t+=(features[i][j]-mean[j])*w[j];
            tt+=t*t;ty+=t*(concentrations[i]-ymean);
        }
        check(tt>EPS,"degenerate PLS latent scores");
        return new Pls1(mean,w,ymean,ty/tt,n);
    }

    /** 05.6: deterministic one-split CART regression prototype; training only.
     * Full tree/forest performance MUST NOT be inferred from this stump.
     */
    public static final class RegressionStump {
        public final int splitFeature,trainingRows;
        public final double threshold,leftMean,rightMean,trainSse;
        private RegressionStump(int feature,int n,double cut,double left,double right,double sse){
            splitFeature=feature;trainingRows=n;threshold=cut;
            leftMean=left;rightMean=right;trainSse=sse;
        }
        public double predict(double[] features){
            vector(features);check(splitFeature<features.length,"stump input dimension");
            return features[splitFeature]<=threshold?leftMean:rightMean;
        }
    }
    public static RegressionStump fitStump(double[][] features,double[] targets){
        matrix(features,4);vector(targets);
        check(targets.length==features.length,"tree labels");
        int n=targets.length,d=features[0].length;
        RegressionStump best=null;double bestSse=Double.POSITIVE_INFINITY;
        for(int feature=0;feature<d;feature++){
            final int c=feature;
            Integer[] idx=new Integer[n];
            for(int i=0;i<n;i++)idx[i]=i;
            Arrays.sort(idx,Comparator.comparingDouble((Integer i)->features[i][c]).thenComparingInt(i->i));
            for(int cut=2;cut<=n-2;cut++){
                double low=features[idx[cut-1]][c],high=features[idx[cut]][c];
                if(!(high>low))continue;
                double left=0,right=0;
                for(int i=0;i<cut;i++)left+=targets[idx[i]];
                for(int i=cut;i<n;i++)right+=targets[idx[i]];
                left/=cut;right/=(n-cut);
                double sse=0;
                for(int i=0;i<cut;i++){double e=targets[idx[i]]-left;sse+=e*e;}
                for(int i=cut;i<n;i++){double e=targets[idx[i]]-right;sse+=e*e;}
                if(sse<bestSse) {
                    bestSse=sse;
                    best=new RegressionStump(c,n,low+(high-low)/2,left,right,sse);
                }
            }
        }
        check(best!=null,"all source predictors constant: no identifiable tree split");
        return best;
    }

    /** 05.9: positive, strictly monotone source ratio and concentration
     * anchors; interpolation/extrapolation is explicitly flagged.
     */
    public static final class UnseenLevel {
        public final double researchEstimatePpm;
        public final boolean extrapolated;
        public final double lowerSourcePpm,upperSourcePpm;
        private UnseenLevel(double value,boolean outer,double lo,double hi){
            researchEstimatePpm=value;extrapolated=outer;
            lowerSourcePpm=lo;upperSourcePpm=hi;
        }
    }
    public static UnseenLevel estimateUnseen(double[] concentrations,double[] ratios,double observedRatio){
        positive(concentrations);positive(ratios);
        check(concentrations.length==ratios.length&&ratios.length>=2&&
              Double.isFinite(observedRatio)&&observedRatio>0,"source ratio/ppm anchors");
        int n=concentrations.length;boolean ascending=ratios[n-1]>ratios[0];
        check(ratios[n-1]!=ratios[0],"zero sensitivity");
        for(int i=1;i<n;i++){
            check(concentrations[i]>concentrations[i-1],"strict source concentration ordering");
            check(ascending?ratios[i]>ratios[i-1]:ratios[i]<ratios[i-1],
                  "nonmonotone source sensitivity: reject interpolation");
        }
        int bracket=0;boolean outside=false;
        if(ascending){
            if(observedRatio<ratios[0]){bracket=0;outside=true;}
            else if(observedRatio>ratios[n-1]){bracket=n-2;outside=true;}
            else while(bracket<n-2&&observedRatio>ratios[bracket+1])bracket++;
        }else{
            if(observedRatio>ratios[0]){bracket=0;outside=true;}
            else if(observedRatio<ratios[n-1]){bracket=n-2;outside=true;}
            else while(bracket<n-2&&observedRatio<ratios[bracket+1])bracket++;
        }
        double y0=Math.log(ratios[bracket]),y1=Math.log(ratios[bracket+1]);
        double t=(Math.log(observedRatio)-y0)/(y1-y0);
        double result=Math.exp(Math.log(concentrations[bracket])+
                    t*(Math.log(concentrations[bracket+1])-Math.log(concentrations[bracket])));
        check(Double.isFinite(result)&&result>0,"undefined interpolated research ppm");
        return new UnseenLevel(result,outside,concentrations[0],concentrations[n-1]);
    }

    /** 07.4: time-index trend of source baseline log-responses only.
     * An observed slope is not proof of chemical sensor drift.
     */
    public static final class DriftTrend {
        public final int observationCount;
        public final double slopePerObservation,endpointDelta;
        private DriftTrend(int n,double slope,double delta) {
            observationCount=n;slopePerObservation=slope;endpointDelta=delta;
        }
    }
    public static DriftTrend driftTrend(double[] baselineSequence){
        vector(baselineSequence);check(baselineSequence.length>=3,"three source baselines");
        int n=baselineSequence.length;double xmean=(n-1)/2.0,mean=0;
        for(double y:baselineSequence)mean+=y;
        mean/=n;
        double numerator=0,denominator=0;
        for(int i=0;i<n;i++){
            numerator+=(i-xmean)*(baselineSequence[i]-mean);
            denominator+=(i-xmean)*(i-xmean);
        }
        return new DriftTrend(n,numerator/denominator,baselineSequence[n-1]-baselineSequence[0]);
    }
    /** 07.5: source-baseline ratio transfer with explicit measured anchors. */
    public static double transferBaseline(double observedRatio,
                                          double measuredBlankRatio,double sourceBlankRatio) {
        check(Double.isFinite(observedRatio)&&observedRatio>0&&
              Double.isFinite(measuredBlankRatio)&&measuredBlankRatio>0&&
              Double.isFinite(sourceBlankRatio)&&sourceBlankRatio>0,"three physical/source anchors");
        double corrected=observedRatio*sourceBlankRatio/measuredBlankRatio;
        check(Double.isFinite(corrected)&&corrected>0,"baseline transfer overflow/underflow");
        return corrected;
    }

    /** 07.8: entropy ranking from positive source-relative class scores.
     * NOT an uncertainty-calibrated or automated experimental DoE policy.
     */
    public static final class ActiveCandidate {
        public final int originalIndex;
        public final double normalizedEntropy;
        private ActiveCandidate(int row,double h){originalIndex=row;normalizedEntropy=h;}
    }
    public static List<ActiveCandidate> rankSourceAmbiguity(double[][] scoreRows){
        check(scoreRows!=null,"score rows required");
        List<ActiveCandidate> result=new ArrayList<>();
        for(int i=0;i<scoreRows.length;i++){
            double[] scores=scoreRows[i];vector(scores);
            check(scores.length>=2,"at least two hypotheses");
            double sum=0;
            for(double s:scores){check(s>=0,"nonnegative source scores");sum+=s;}
            check(Double.isFinite(sum)&&sum>0,"positive scores required");
            double entropy=0;
            for(double s:scores)if(s>0){
                double p=s/sum;entropy-=p*Math.log(p);
            }
            result.add(new ActiveCandidate(i,entropy/Math.log(scores.length)));
        }
        result.sort(Comparator.comparingDouble((ActiveCandidate x)->x.normalizedEntropy)
                              .reversed().thenComparingInt(x->x.originalIndex));
        return Collections.unmodifiableList(result);
    }

    /** 07.10: descriptive latency percentiles from caller-captured nanos.
     * No Android resource budget can be claimed without phone measurements.
     */
    public static final class LatencyStats {
        public final int samples;
        public final double p50Ms,p95Ms,maxMs;
        private LatencyStats(int n,double a,double b,double c){
            samples=n;p50Ms=a;p95Ms=b;maxMs=c;
        }
    }
    public static LatencyStats latency(long[] nanos) {
        check(nanos!=null&&nanos.length>0,"latency samples");
        long[] copy=nanos.clone();
        for(long n:copy)check(n>=0,"negative elapsed monotonic time");
        Arrays.sort(copy);
        return new LatencyStats(copy.length,quantile(copy,.5)/1e6,
                quantile(copy,.95)/1e6,copy[copy.length-1]/1e6);
    }
    private static double quantile(long[] v,double q) {
        double ix=q*(v.length-1);
        int l=(int)Math.floor(ix),h=(int)Math.ceil(ix);
        return v[l]+(v[h]-v[l])*(ix-l);
    }

    /** 08.6: deterministic additive Gaussian perturbation of source features.
     * This is NOT a physically validated sensor/noise or gas-generation model.
     */
    public static double[][] simulateSource(double[] sourceFeatures,double absoluteSigma,
                                            int sampleCount,long seed) {
        vector(sourceFeatures);
        check(Double.isFinite(absoluteSigma)&&absoluteSigma>=0&&sampleCount>0&&
              sampleCount<=10000,"simulator parameters");
        Random generator=new Random(seed);
        double[][] simulated=new double[sampleCount][sourceFeatures.length];
        for(int i=0;i<sampleCount;i++)for(int j=0;j<sourceFeatures.length;j++){
            simulated[i][j]=sourceFeatures[j]+absoluteSigma*generator.nextGaussian();
            check(Double.isFinite(simulated[i][j]),"simulation numeric overflow");
        }
        return simulated;
    }

    /** 08.8: nonnegative two-source least squares fit, including boundary
     * single-component solutions. An improved residual does NOT prove a mixture.
     */
    public static final class PairFit {
        public final double firstScale,secondScale,residualSquared,bestSingleResidualSquared;
        public final boolean mixtureIdentityValidated;
        private PairFit(double a,double b,double r,double single){
            firstScale=a;secondScale=b;residualSquared=r;
            bestSingleResidualSquared=single;mixtureIdentityValidated=false;
        }
    }
    private static double residual(double[] observed,double[] a,double[] b,
                                   double ka,double kb){
        double s=0;
        for(int i=0;i<observed.length;i++){
            double e=observed[i]-ka*a[i]-kb*b[i];s+=e*e;
        }
        return s;
    }
    public static PairFit sourcePairFit(double[] observed,double[] first,double[] second){
        vector(observed);vector(first);vector(second);
        check(observed.length==first.length&&first.length==second.length&&
              observed.length>=2,"aligned source feature space required");
        double aa=dot(first,first),bb=dot(second,second),ab=dot(first,second);
        check(aa>EPS&&bb>EPS,"nonzero source prototypes");
        double ay=dot(first,observed),by=dot(second,observed);
        double one=Math.max(0,ay/aa),two=Math.max(0,by/bb);
        double singleA=residual(observed,first,second,one,0);
        double singleB=residual(observed,first,second,0,two);
        double best=Math.min(singleA,singleB);
        double bestA=singleA<=singleB?one:0,bestB=singleA<=singleB?0:two;
        double determinant=aa*bb-ab*ab;
        if(determinant>EPS){
            double a=(ay*bb-by*ab)/determinant;
            double b=(by*aa-ay*ab)/determinant;
            if(a>=0&&b>=0){
                double candidate=residual(observed,first,second,a,b);
                if(candidate<best){best=candidate;bestA=a;bestB=b;}
            }
        }
        return new PairFit(bestA,bestB,best,Math.min(singleA,singleB));
    }
    /** 08.11: conditional residual after adding a nonnegative interfering
     * source vector to a FIXED user-specified target fingerprint.
     */
    public static final class InterferenceFit {
        public final double assumedInterfererScale,residualSquared,baselineResidualSquared;
        public final boolean interfererIdentityValidated;
        private InterferenceFit(double scale,double residual,double baseline){
            assumedInterfererScale=scale;residualSquared=residual;
            baselineResidualSquared=baseline;interfererIdentityValidated=false;
        }
    }
    public static InterferenceFit interferenceResidual(double[] observed,
                                                       double[] specifiedTarget,
                                                       double[] candidateInterference) {
        vector(observed);vector(specifiedTarget);vector(candidateInterference);
        check(observed.length==specifiedTarget.length&&
              observed.length==candidateInterference.length,"aligned channel spaces");
        double strength=dot(candidateInterference,candidateInterference);
        check(strength>EPS,"interferer source direction required");
        double correlation=0,baseline=0;
        for(int i=0;i<observed.length;i++){
            double e=observed[i]-specifiedTarget[i];
            correlation+=e*candidateInterference[i];baseline+=e*e;
        }
        double scale=Math.max(0,correlation/strength);
        double fit=residual(observed,specifiedTarget,candidateInterference,1,scale);
        return new InterferenceFit(scale,fit,baseline);
    }
}
