package com.hazrob.tank.chemcore.v6;

import java.util.Arrays;
import java.util.Random;

/**
 * N03-F first five source-trained mathematical learning prototypes.
 * Research only: input features/labels must be prepared independently and
 * any source holdout must not enter fit or hyperparameter selection.
 * NO device ppm, gas identification certification, food or prior engine.
 */
public final class SupervisedLearningResearchV6 {
    private SupervisedLearningResearchV6(){}
    private static final double EPS=1e-12;
    private static void check(boolean ok,String why){
        if(!ok)throw new IllegalArgumentException(why);
    }
    private static void vec(double[] x){
        check(x!=null&&x.length>0,"source vector required");
        for(double a:x)check(Double.isFinite(a),"nonfinite source feature");
    }
    private static int matrix(double[][] x,int minRows){
        check(x!=null&&x.length>=minRows,"insufficient source training rows");
        vec(x[0]);int d=x[0].length;
        for(double[] r:x){vec(r);check(r.length==d,"inconsistent source feature axes");}
        return d;
    }
    private static double dot(double[] a,double[] b){
        double sum=0;for(int j=0;j<a.length;j++)sum+=a[j]*b[j];return sum;
    }
    private static double sigmoid(double z){
        return z>=0?1/(1+Math.exp(-z)):Math.exp(z)/(1+Math.exp(z));
    }
    private static int[] labels(int[] y,int n){
        check(y!=null&&y.length==n,"matched binary source labels");
        int zero=0,one=0;
        for(int k:y){check(k==0||k==1,"0/1 source labels only");if(k==0)zero++;else one++;}
        check(zero>0&&one>0,"both source classes required");
        return y;
    }

    /** 04.5: deterministic linear binary soft-margin hinge-loss SVM SGD
     * with L2 weight penalty; not a kernel SVM or probability estimator.
     */
    public static final class LinearSvm {
        public final double[] weight;
        public final double intercept;
        public final int sourceTrainingRows,epochs;
        private LinearSvm(double[] w,double b,int n,int epochs){
            weight=w.clone();intercept=b;sourceTrainingRows=n;this.epochs=epochs;
        }
        public double margin(double[] x){
            vec(x);check(x.length==weight.length,"SVM feature dimension");
            return dot(weight,x)+intercept;
        }
        public int predict(double[] x){return margin(x)>=0?1:0;}
    }
    public static LinearSvm fitLinearSvm(double[][] x,int[] y,int epochs,
                                         double learningRate,double lambda){
        int d=matrix(x,4),n=x.length;labels(y,n);
        check(epochs>0&&epochs<=20000&&Double.isFinite(learningRate)&&
              learningRate>0&&learningRate<=1&&Double.isFinite(lambda)&&lambda>0,
              "SVM optimizer configuration");
        double[] w=new double[d];double b=0;
        for(int epoch=0;epoch<epochs;epoch++){
            double lr=learningRate/(1+.02*epoch);
            for(int i=0;i<n;i++){
                int sign=y[i]==1?1:-1;
                double margin=sign*(dot(w,x[i])+b);
                for(int j=0;j<d;j++)
                    w[j]-=lr*(lambda*w[j]-(margin<1?sign*x[i][j]:0));
                if(margin<1)b+=lr*sign;
            }
        }
        vec(w);check(Double.isFinite(b),"SVM diverged: inspect source scaling");
        return new LinearSvm(w,b,n,epochs);
    }

    /** 04.6: bootstrap forest of one-level CART classification trees
     * with seeded feature subsampling. NOT a deep RandomForest.
     */
    public static final class StumpForest {
        private final int[] features,left,right;
        private final double[] thresholds;
        public final int trees,sourceTrainingRows,featureCount;
        private StumpForest(int[] features,double[] limits,int[] a,int[] b,int n,int d){
            this.features=features;thresholds=limits;left=a;right=b;
            trees=features.length;sourceTrainingRows=n;featureCount=d;
        }
        public double positiveVoteFraction(double[] x){
            vec(x);check(x.length==featureCount,"forest source axis mismatch");
            int votes=0;
            for(int i=0;i<trees;i++)votes+=x[features[i]]<=thresholds[i]?left[i]:right[i];
            return (double)votes/trees; // relative votes, NOT calibrated probability
        }
        public int predict(double[] x){return positiveVoteFraction(x)>=.5?1:0;}
    }
    public static StumpForest fitBootstrapStumps(double[][] x,int[] y,int trees,long seed){
        int d=matrix(x,4),n=x.length;labels(y,n);
        check(trees>0&&trees<=10000,"forest size bounds");
        Random random=new Random(seed);
        int[] features=new int[trees],a=new int[trees],b=new int[trees];
        double[] thresholds=new double[trees];
        for(int t=0;t<trees;t++){
            int feature=random.nextInt(d);features[t]=feature;
            int[] sample=new int[n];
            for(int i=0;i<n;i++)sample[i]=random.nextInt(n);
            double bestLoss=Double.POSITIVE_INFINITY;
            double bestThreshold=Double.POSITIVE_INFINITY;
            int bestA=0,bestB=0;
            int positives=0;
            for(int idx:sample)positives+=y[idx];
            int overall=positives*2>=n?1:0;
            bestA=overall;bestB=overall;
            for(int c=0;c<n;c++){
                double cut=x[sample[c]][feature];
                int na=0,nb=0,ya=0,yb=0;
                for(int idx:sample){
                    if(x[idx][feature]<=cut){na++;ya+=y[idx];}
                    else{nb++;yb+=y[idx];}
                }
                if(na==0||nb==0)continue;
                int leftLabel=ya*2>=na?1:0,rightLabel=yb*2>=nb?1:0;
                double loss=Math.min(ya,na-ya)+Math.min(yb,nb-yb);
                if(loss<bestLoss){
                    bestLoss=loss;bestThreshold=cut;bestA=leftLabel;bestB=rightLabel;
                }
            }
            thresholds[t]=bestThreshold;a[t]=bestA;b[t]=bestB;
        }
        return new StumpForest(features,thresholds,a,b,n,d);
    }

    /** 04.7: gradient/Hessian Newton boosting with logistic Bernoulli loss
     * and 1-split trees. A deliberately bounded XGBoost-*style* subset;
     * NOT XGBoost library, histogram, missing-value or full-depth trees.
     */
    public static final class NewtonBoostStumps {
        public final int sourceTrainingRows,rounds,featureCount;
        private final int[] feature;
        private final double[] cutoff,leftWeight,rightWeight;
        private final double initialLogit,step;
        private NewtonBoostStumps(int n,int d,int[] feature,double[] cutoff,
            double[] a,double[] b,double init,double lr){
            sourceTrainingRows=n;featureCount=d;rounds=feature.length;
            this.feature=feature;this.cutoff=cutoff;leftWeight=a;rightWeight=b;
            initialLogit=init;step=lr;
        }
        public double sourceScore(double[] x){
            vec(x);check(x.length==featureCount,"boost feature dimension");
            double z=initialLogit;
            for(int i=0;i<rounds;i++)
                z+=step*(x[feature[i]]<=cutoff[i]?leftWeight[i]:rightWeight[i]);
            return sigmoid(z); // source score; uncalibrated
        }
    }
    public static NewtonBoostStumps fitNewtonBoost(double[][] x,int[] y,
                     int rounds,double learningRate,double ridge){
        int d=matrix(x,4),n=x.length;labels(y,n);
        check(rounds>0&&rounds<=1000&&Double.isFinite(learningRate)&&
              learningRate>0&&learningRate<=1&&Double.isFinite(ridge)&&ridge>0,
              "Newton boosted-stump controls");
        int count=0;for(int a:y)count+=a;
        double init=Math.log((count+.5)/(n-count+.5));
        double[] z=new double[n];Arrays.fill(z,init);
        int[] features=new int[rounds];double[] cuts=new double[rounds],
             left=new double[rounds],right=new double[rounds];
        for(int stage=0;stage<rounds;stage++){
            double[] g=new double[n],h=new double[n];
            for(int i=0;i<n;i++){
                double p=sigmoid(z[i]);g[i]=y[i]-p;h[i]=Math.max(1e-8,p*(1-p));
            }
            double bestGain=-Double.MAX_VALUE;
            int bestFeature=0;double bestCut=0,bestLeft=0,bestRight=0;
            for(int feature=0;feature<d;feature++){
                for(int c=0;c<n;c++){
                    double cut=x[c][feature],ga=0,gb=0,ha=0,hb=0;
                    int na=0,nb=0;
                    for(int i=0;i<n;i++){
                        if(x[i][feature]<=cut){ga+=g[i];ha+=h[i];na++;}
                        else{gb+=g[i];hb+=h[i];nb++;}
                    }
                    if(na==0||nb==0)continue;
                    double gain=ga*ga/(ha+ridge)+gb*gb/(hb+ridge);
                    if(gain>bestGain){
                        bestGain=gain;bestFeature=feature;bestCut=cut;
                        bestLeft=ga/(ha+ridge);bestRight=gb/(hb+ridge);
                    }
                }
            }
            check(bestGain>-Double.MAX_VALUE,"no source split for gradient boosting");
            features[stage]=bestFeature;cuts[stage]=bestCut;
            left[stage]=bestLeft;right[stage]=bestRight;
            for(int i=0;i<n;i++)
                z[i]+=learningRate*(x[i][bestFeature]<=bestCut?bestLeft:bestRight);
        }
        return new NewtonBoostStumps(n,d,features,cuts,left,right,init,learningRate);
    }

    /** 04.8: genuine one-hidden-layer tanh binary MLP trained by batch
     * backpropagation with seeded weights; no deep gas-ID claim.
     */
    public static final class OneHiddenMlp {
        public final int sourceTrainingRows,featureCount,hiddenUnits,epochs;
        private final double[][] w;
        private final double[] a,v;
        private final double b;
        private OneHiddenMlp(int n,int d,int hidden,int epochs,double[][] w,
                             double[] a,double[] v,double b){
            sourceTrainingRows=n;featureCount=d;hiddenUnits=hidden;this.epochs=epochs;
            this.w=w;this.a=a;this.v=v;this.b=b;
        }
        public double sourceScore(double[] x){
            vec(x);check(x.length==featureCount,"MLP input axes");
            double logit=b;
            for(int j=0;j<hiddenUnits;j++)logit+=
                v[j]*Math.tanh(dot(w[j],x)+a[j]);
            return sigmoid(logit);
        }
    }
    public static OneHiddenMlp fitMlp(double[][] x,int[] y,int hidden,
               int epochs,double learningRate,long seed){
        int d=matrix(x,4),n=x.length;labels(y,n);
        check(hidden>0&&hidden<=256&&epochs>0&&epochs<=20000&&
              Double.isFinite(learningRate)&&learningRate>0&&learningRate<=1,
              "MLP architecture or optimization");
        Random random=new Random(seed);
        double[][] w=new double[hidden][d];double[] a=new double[hidden],
                   v=new double[hidden];
        for(int j=0;j<hidden;j++){
            for(int k=0;k<d;k++)w[j][k]=random.nextGaussian()*.25;
            v[j]=random.nextGaussian()*.25;
        }
        double b=0;
        for(int epoch=0;epoch<epochs;epoch++){
            double[][] dw=new double[hidden][d];
            double[] da=new double[hidden],dv=new double[hidden];
            double db=0;
            for(int i=0;i<n;i++){
                double[] h=new double[hidden];double logit=b;
                for(int j=0;j<hidden;j++){
                    h[j]=Math.tanh(dot(w[j],x[i])+a[j]);
                    logit+=v[j]*h[j];
                }
                double error=sigmoid(logit)-y[i];db+=error;
                for(int j=0;j<hidden;j++){
                    dv[j]+=error*h[j];
                    double back=error*v[j]*(1-h[j]*h[j]);
                    da[j]+=back;
                    for(int k=0;k<d;k++)dw[j][k]+=back*x[i][k];
                }
            }
            for(int j=0;j<hidden;j++){
                v[j]-=learningRate*dv[j]/n;
                a[j]-=learningRate*da[j]/n;
                for(int k=0;k<d;k++)w[j][k]-=learningRate*dw[j][k]/n;
            }
            b-=learningRate*db/n;
        }
        vec(v);vec(a);check(Double.isFinite(b),"MLP diverged");
        for(double[] row:w)vec(row);
        return new OneHiddenMlp(n,d,hidden,epochs,w,a,v,b);
    }

    /** 04.9: fixed externally justified source-curve physics estimate plus
     * train-only 1D residual correction in log response. Numerical proof
     * cannot turn an unverified sensor curve into physical calibration.
     */
    public static final class PhysicsResidual {
        public final int sourceTrainingRows;
        public final double anchoredSlope,anchoredIntercept,residualIntercept,residualSlope;
        private PhysicsResidual(int n,double slope,double intercept,double a,double b){
            sourceTrainingRows=n;anchoredSlope=slope;anchoredIntercept=intercept;
            residualIntercept=a;residualSlope=b;
        }
        public double researchLogConcentration(double positiveRatio){
            check(Double.isFinite(positiveRatio)&&positiveRatio>0,"valid source Rs/R0");
            double u=Math.log(positiveRatio);
            return anchoredIntercept+anchoredSlope*u+residualIntercept+residualSlope*u;
        }
        public double researchConcentration(double positiveRatio){
            double value=Math.exp(researchLogConcentration(positiveRatio));
            check(Double.isFinite(value)&&value>0,"unrepresentable source output");
            return value; // source units only, not Tank ppm without calibration
        }
    }
    public static PhysicsResidual fitPhysicsResidual(double[] sourceRsR0,
        double[] sourceReferenceConcentration,double fixedCurveSlope,
        double fixedCurveIntercept,double ridge){
        vec(sourceRsR0);vec(sourceReferenceConcentration);
        check(sourceRsR0.length==sourceReferenceConcentration.length&&
              sourceRsR0.length>=3&&Double.isFinite(fixedCurveSlope)&&
              Double.isFinite(fixedCurveIntercept)&&Double.isFinite(ridge)&&ridge>0,
              "source-referenced physical prior");
        int n=sourceRsR0.length;double avgX=0,avgY=0;
        double[] x=new double[n],res=new double[n];
        for(int i=0;i<n;i++){
            check(sourceRsR0[i]>0&&sourceReferenceConcentration[i]>0,
                  "positive source ratio and concentration references");
            x[i]=Math.log(sourceRsR0[i]);
            res[i]=Math.log(sourceReferenceConcentration[i])-
                   (fixedCurveIntercept+fixedCurveSlope*x[i]);
            avgX+=x[i]/n;avgY+=res[i]/n;
        }
        double covariance=0,variance=0;
        for(int i=0;i<n;i++){
            double dx=x[i]-avgX;
            covariance+=dx*(res[i]-avgY);variance+=dx*dx;
        }
        check(variance>EPS,"unidentifiable source residual correction");
        double slope=covariance/(variance+ridge);
        double intercept=avgY-slope*avgX;
        return new PhysicsResidual(n,fixedCurveSlope,fixedCurveIntercept,intercept,slope);
    }
}
