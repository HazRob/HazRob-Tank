package com.hazrob.tank.chemcore.v6;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * V6-N03-C: ten research-stage validation techniques; no Android dependencies.
 * All fitted parameters are derived from TRAINING/CALIBRATION sets only.
 * Scores are NOT certified gas-identity probabilities. Every numerically valid
 * source hypothesis remains available even if reporting abstains.
 * No food-freshness or previous-engine dependency. Tank G3 is not established.
 */
public final class SourceValidationV6 {
    private SourceValidationV6(){}
    private static boolean finite(double x){return Double.isFinite(x);}
    private static void require(boolean condition,String message){
        if(!condition)throw new IllegalArgumentException(message);
    }
    private static void vector(double[] p){
        require(p!=null&&p.length>0,"nonempty numeric vector required");
        for(double v:p)require(finite(v),"nonfinite evidence");
    }
    public static final class Metrics {
        public final int evaluated,correct;
        public final double accuracy,balancedAccuracy,macroF1;
        public final Map<String,Integer> support;
        private Metrics(int n,int c,double a,double b,double f,Map<String,Integer> s){
            evaluated=n;correct=c;accuracy=a;balancedAccuracy=b;macroF1=f;
            support=Collections.unmodifiableMap(s);
        }
    }
    /** 04.10: metrics from independently held-out labels; abstentions count as errors. */
    public static Metrics benchmark(String[] truth,String[] predictions,String[] knownClasses){
        require(truth!=null&&predictions!=null&&knownClasses!=null&&
            truth.length==predictions.length&&truth.length>0&&knownClasses.length>0,
            "heldout truths/predictions/classes required");
        Map<String,Integer> index=new LinkedHashMap<>();
        for(String c:knownClasses){
            require(c!=null&&!c.isEmpty()&&!index.containsKey(c),"unique class names");
            index.put(c,index.size());
        }
        int k=index.size(),n=truth.length,correct=0;
        int[] tp=new int[k],pred=new int[k],actual=new int[k];
        for(int i=0;i<n;i++){
            Integer t=index.get(truth[i]);
            require(t!=null,"unknown true class in evaluation");
            actual[t]++;
            Integer p=index.get(predictions[i]);
            if(p!=null){pred[p]++;if(p.equals(t)){tp[t]++;correct++;}}
        }
        double recall=0,f1=0;int active=0;
        Map<String,Integer> support=new LinkedHashMap<>();
        for(Map.Entry<String,Integer> e:index.entrySet()){
            int i=e.getValue();support.put(e.getKey(),actual[i]);
            if(actual[i]>0){
                active++;
                recall+=tp[i]/(double)actual[i];
                f1+=2.0*tp[i]/(actual[i]+pred[i]);
            }
        }
        return new Metrics(n,correct,correct/(double)n,recall/active,f1/active,support);
    }
    /** 06.2: source-conditional Bayes normalization; not device posterior. */
    public static double[] bayesianPosterior(double[] prior,double[] likelihood){
        vector(prior);vector(likelihood);
        require(prior.length==likelihood.length,"Bayes dimensions");
        // Log-sum-exp prevents underflow of perfectly valid small source likelihoods.
        double[] logWeight=new double[prior.length];
        double largest=Double.NEGATIVE_INFINITY;
        for(int i=0;i<logWeight.length;i++){
            require(prior[i]>=0&&likelihood[i]>=0,"negative probability/likelihood");
            logWeight[i]=prior[i]==0||likelihood[i]==0?Double.NEGATIVE_INFINITY:
                Math.log(prior[i])+Math.log(likelihood[i]);
            largest=Math.max(largest,logWeight[i]);
        }
        require(finite(largest),"zero Bayes evidence");
        double[] w=new double[prior.length];double sum=0;
        for(int i=0;i<w.length;i++){
            w[i]=Math.exp(logWeight[i]-largest);sum+=w[i];
        }
        require(finite(sum)&&sum>0,"invalid Bayes normalization");
        for(int i=0;i<w.length;i++)w[i]/=sum;
        return w;
    }
    public static double[] softmaxTemperature(double[] logits,double temperature){
        vector(logits);require(finite(temperature)&&temperature>0,"positive temperature");
        // Subtract raw max BEFORE division: z/temperature can overflow for
        // finite z and small positive temperature even when posterior is defined.
        double max=Double.NEGATIVE_INFINITY;
        for(double z:logits)max=Math.max(max,z);
        double[] p=new double[logits.length];double sum=0;
        for(int i=0;i<p.length;i++){p[i]=Math.exp((logits[i]-max)/temperature);sum+=p[i];}
        require(finite(sum)&&sum>0,"invalid softmax normalization");
        for(int i=0;i<p.length;i++)p[i]/=sum;
        return p;
    }
    public static final class TemperatureFit {
        public final double temperature,calibrationMeanNll;
        public final int calibrationRows;
        private TemperatureFit(double t,double loss,int rows){
            temperature=t;calibrationMeanNll=loss;calibrationRows=rows;
        }
    }
    /** 06.3: choose temperature only from an independent calibration partition. */
    public static TemperatureFit fitTemperature(double[][] calibrationLogits,int[] trueIndices,
                                                double[] candidateTemperatures){
        require(calibrationLogits!=null&&trueIndices!=null&&candidateTemperatures!=null&&
            calibrationLogits.length>0&&calibrationLogits.length==trueIndices.length&&
            candidateTemperatures.length>0,"independent calibration set required");
        double best=Double.POSITIVE_INFINITY,chosen=Double.NaN;
        int width=-1;
        for(int i=0;i<calibrationLogits.length;i++){
            vector(calibrationLogits[i]);
            if(width<0)width=calibrationLogits[i].length;
            require(width==calibrationLogits[i].length&&trueIndices[i]>=0&&
                    trueIndices[i]<width,"calibration class index/dimension");
        }
        for(double t:candidateTemperatures){
            require(finite(t)&&t>0,"positive calibration candidate");
            double loss=0;
            for(int i=0;i<trueIndices.length;i++){
                double p=softmaxTemperature(calibrationLogits[i],t)[trueIndices[i]];
                loss-=Math.log(Math.max(p,Double.MIN_NORMAL));
            }
            loss/=trueIndices.length;
            if(loss<best){best=loss;chosen=t;}
        }
        return new TemperatureFit(chosen,best,trueIndices.length);
    }
    public static final class ConformalSet {
        public final boolean[] included;
        public final double nonconformityQuantile;
        public final int calibrationRows;
        private ConformalSet(boolean[] x,double q,int n){
            included=x;nonconformityQuantile=q;calibrationRows=n;
        }
    }
    /** 06.4: split-conformal marginal set, assuming exchangeability.
     * For rank n+1, conservative quantile is infinity: include ALL labels.
     * Class-probability inputs are mathematical scores and need calibration.
     */
    public static ConformalSet conformal(double[] calibrationTrueClassProbability,
                                         double[] currentClassProbability,double alpha){
        vector(calibrationTrueClassProbability);vector(currentClassProbability);
        require(finite(alpha)&&alpha>0&&alpha<1,"0<alpha<1");
        double[] errors=new double[calibrationTrueClassProbability.length];
        for(int i=0;i<errors.length;i++){
            double p=calibrationTrueClassProbability[i];
            require(p>=0&&p<=1,"calibration probability range");errors[i]=1-p;
        }
        Arrays.sort(errors);
        int rank=(int)Math.ceil((errors.length+1)*(1-alpha));
        double q=rank>errors.length?Double.POSITIVE_INFINITY:errors[rank-1];
        boolean[] included=new boolean[currentClassProbability.length];
        for(int j=0;j<included.length;j++){
            double p=currentClassProbability[j];
            require(p>=0&&p<=1,"current probability range");
            included[j]=1-p<=q;
        }
        return new ConformalSet(included,q,errors.length);
    }
    public static final class OpenSetResult {
        public final boolean unknown;
        public final int bestClassIndex;
        public final double bestScore,secondScore;
        private OpenSetResult(boolean unknown,int idx,double best,double second){
            this.unknown=unknown;bestClassIndex=idx;bestScore=best;secondScore=second;
        }
    }
    /** 06.5: explicit threshold/margin; NO certified gas identity. */
    public static OpenSetResult openSet(double[] scores,double minBest,double minMargin){
        vector(scores);require(finite(minBest)&&finite(minMargin)&&minMargin>=0,
                               "explicit threshold/margin");
        int best=-1;double top=Double.NEGATIVE_INFINITY,next=Double.NEGATIVE_INFINITY;
        for(int i=0;i<scores.length;i++){
            if(scores[i]>top){next=top;top=scores[i];best=i;}
            else next=Math.max(next,scores[i]);
        }
        boolean unknown=top<minBest||(scores.length>1&&top-next<=minMargin);
        return new OpenSetResult(unknown,unknown?-1:best,top,next);
    }
    public static final class OodModel {
        public final double[] mean,variance;
        public final int trainingRows;
        private OodModel(double[] m,double[] v,int n){
            mean=m;variance=v;trainingRows=n;
        }
        public double squaredDistance(double[] x){
            vector(x);require(x.length==mean.length,"OOD dimension");
            double total=0;
            for(int j=0;j<x.length;j++){double d=x[j]-mean[j];total+=d*d/variance[j];}
            return total;
        }
    }
    /** 06.6: diagonal Mahalanobis score fitted on known-source training only.
     * The variance floor is explicit and no OOD rejection threshold is learned.
     */
    public static OodModel fitOod(double[][] training,double varianceFloor){
        require(training!=null&&training.length>=2&&finite(varianceFloor)&&varianceFloor>0,
                "two source training rows and positive variance floor required");
        vector(training[0]);int d=training[0].length;
        double[] mean=new double[d],var=new double[d];
        for(double[] row:training){
            vector(row);require(row.length==d,"OOD dimension mismatch");
            for(int j=0;j<d;j++)mean[j]+=row[j];
        }
        for(int j=0;j<d;j++)mean[j]/=training.length;
        for(double[] row:training)for(int j=0;j<d;j++){
            double e=row[j]-mean[j];var[j]+=e*e;
        }
        for(int j=0;j<d;j++)var[j]=Math.max(varianceFloor,var[j]/(training.length-1));
        return new OodModel(mean,var,training.length);
    }
    public static final class Abstention {
        public final boolean abstained;
        public final String reason;
        public final double[] unchangedScores;
        private Abstention(boolean a,String reason,double[] s){
            abstained=a;this.reason=reason;unchangedScores=s;
        }
    }
    /** 06.9: abstain from reporting, never remove source candidate scores. */
    public static Abstention abstain(double[] scores,boolean sourceCalibrated,
                                    double observedOod,double maxOod,double minTop){
        vector(scores);
        require(finite(observedOod)&&observedOod>=0&&finite(maxOod)&&maxOod>=0&&
                finite(minTop),"abstention threshold inputs");
        double best=Double.NEGATIVE_INFINITY;for(double s:scores)best=Math.max(best,s);
        String reason=!sourceCalibrated?"UNVALIDATED_SOURCE":
                      observedOod>maxOod?"OUT_OF_DISTRIBUTION":
                      best<minTop?"INSUFFICIENT_RELATIVE_SCORE":"NO_ABSTENTION";
        return new Abstention(!"NO_ABSTENTION".equals(reason),reason,scores.clone());
    }
    public static final class Holdout {
        public final String groupKind,heldoutGroup;
        public final int[] trainingIndices,testIndices;
        private Holdout(String type,String group,int[] train,int[] test){
            groupKind=type;heldoutGroup=group;trainingIndices=train;testIndices=test;
        }
    }
    private static Holdout holdout(String kind,String[] groups,String withheld){
        require(groups!=null&&groups.length>=2&&withheld!=null&&!withheld.isEmpty(),
                "independent grouping required");
        List<Integer> train=new ArrayList<>(),test=new ArrayList<>();
        for(int i=0;i<groups.length;i++){
            require(groups[i]!=null&&!groups[i].isEmpty(),"missing group ID");
            (withheld.equals(groups[i])?test:train).add(i);
        }
        require(!test.isEmpty()&&!train.isEmpty(),"nonempty sealed test and training groups");
        return new Holdout(kind,withheld,toArray(train),toArray(test));
    }
    private static int[] toArray(List<Integer> a){
        int[] r=new int[a.size()];for(int i=0;i<r.length;i++)r[i]=a.get(i);return r;
    }
    /** 07.1: same source session/repeated frames never split across partitions. */
    public static Holdout sessionHoldout(String[] sessionIds,String withheldSession){
        return holdout("SESSION",sessionIds,withheldSession);
    }
    /** 07.2: leave one independently identified physical device out. */
    public static Holdout deviceHoldout(String[] deviceIds,String withheldDevice){
        return holdout("PHYSICAL_DEVICE",deviceIds,withheldDevice);
    }
    /** 07.3: leave one entire source concentration level out. */
    public static Holdout concentrationHoldout(String[] levelIds,String withheldLevel){
        return holdout("SOURCE_CONCENTRATION",levelIds,withheldLevel);
    }
}
