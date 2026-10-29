package com.hazrob.tank.chemcore.v6;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Independent source-labeled classical ML primitives for ChemCore Tank V6.
 * 03.1 PCA; 03.4 diagonal LDA (NOT PLS-DA); 04.3 KNN;
 * 04.4 multiclass softmax logistic regression.
 *
 * Train ONLY on complete labeled source rows; holdout data must be kept out
 * of fit(), scaler statistics and model selection. Inputs are already in ONE
 * scientifically consistent feature space. NO food features or previous engine.
 * Scores reflect math of source data, not calibrated gas identity.
 */
public final class ClassicalLearningV6 {
    private ClassicalLearningV6(){}
    private static final double EPS=1e-12;
    private static boolean finite(double x){return Double.isFinite(x);}
    private static double dot(double[] a,double[] b) {
        double sum=0;for(int j=0;j<a.length;j++)sum+=a[j]*b[j];return sum;
    }
    public static final class Dataset {
        public final double[][] samples;
        public final String[] labels;
        public final int originalRows,validRows,droppedNumericalRows,dimension;
        public final List<String> classes;
        public Dataset(double[][] input,String[] inputLabels) {
            if(input==null||inputLabels==null||input.length!=inputLabels.length)
                throw new IllegalArgumentException("matched source features/labels required");
            originalRows=input.length;
            int d=-1,skipped=0;
            List<double[]> good=new ArrayList<>();
            List<String> named=new ArrayList<>();
            for(int i=0;i<input.length;i++){
                double[] row=input[i];String label=inputLabels[i];
                if(row==null||row.length==0||label==null||label.trim().isEmpty()){
                    skipped++;continue;
                }
                if(d<0)d=row.length;
                if(row.length!=d)throw new IllegalArgumentException(
                    "source feature dimensions inconsistent: no imputation");
                boolean valid=true;
                for(double x:row)if(!finite(x)){valid=false;break;}
                if(!valid){skipped++;continue;}
                good.add(row.clone());named.add(label);
            }
            validRows=good.size();droppedNumericalRows=skipped;dimension=d<0?0:d;
            samples=good.toArray(new double[0][]);
            labels=named.toArray(new String[0]);
            classes=Collections.unmodifiableList(new ArrayList<>(
                    new java.util.LinkedHashSet<>(named)));
        }
    }
    public static final class Standardizer {
        public final double[] mean,scale;
        private Standardizer(double[] m,double[] s){mean=m.clone();scale=s.clone();}
        public double[] transform(double[] vector) {
            if(vector==null||vector.length!=mean.length)
                throw new IllegalArgumentException("feature dimension mismatch");
            double[] x=new double[vector.length];
            for(int j=0;j<x.length;j++){
                if(!finite(vector[j]))throw new IllegalArgumentException("nonfinite feature");
                x[j]=(vector[j]-mean[j])/scale[j];
            }
            return x;
        }
        public static Standardizer fit(double[][] x) {
            if(x==null||x.length==0)throw new IllegalArgumentException("empty source training");
            int d=x[0].length;double[] m=new double[d],v=new double[d];
            for(double[] row:x){
                if(row.length!=d)throw new IllegalArgumentException("dimension mismatch");
                for(int j=0;j<d;j++){
                    if(!finite(row[j]))throw new IllegalArgumentException("bad training feature");
                    m[j]+=row[j];
                }
            }
            for(int j=0;j<d;j++)m[j]/=x.length;
            for(double[] row:x)for(int j=0;j<d;j++){
                double z=row[j]-m[j];v[j]+=z*z;
            }
            for(int j=0;j<d;j++){
                v[j]=Math.sqrt(v[j]/x.length);
                if(!(v[j]>EPS))v[j]=1.0; // do not invent variability
            }
            return new Standardizer(m,v);
        }
    }
    public static final class PCA {
        public final double[] mean,eigenvalues;
        public final double[][] components;
        public final int sourceTrainingRows;
        private PCA(double[] center,double[][] basis,double[] values,int n){
            mean=center;components=basis;eigenvalues=values;sourceTrainingRows=n;
        }
        public double[] transform(double[] observation){
            if(observation==null||observation.length!=mean.length)
                throw new IllegalArgumentException("PCA dimensions");
            double[] centered=new double[mean.length];
            for(int j=0;j<centered.length;j++){
                if(!finite(observation[j]))throw new IllegalArgumentException("bad PCA input");
                centered[j]=observation[j]-mean[j];
            }
            double[] projected=new double[components.length];
            for(int k=0;k<components.length;k++)projected[k]=dot(components[k],centered);
            return projected;
        }
    }
    /** 03.1: covariance PCA by deterministic orthogonal power iteration.
     * No test data used in training; zero-eigenvalue axes are retained as
     * zero coordinates, rather than silently labelling noise as evidence.
     */
    public static PCA pca(double[][] sourceTraining,int requestedComponents){
        if(sourceTraining==null||sourceTraining.length<2||requestedComponents<1)
            throw new IllegalArgumentException("PCA training rows and components required");
        int d=sourceTraining[0].length;
        if(d==0||requestedComponents>d)throw new IllegalArgumentException("PCA dimension");
        double[] mean=new double[d];
        for(double[] row:sourceTraining){
            if(row==null||row.length!=d)throw new IllegalArgumentException("PCA feature shape");
            for(int j=0;j<d;j++){
                if(!finite(row[j]))throw new IllegalArgumentException("PCA nonfinite");
                mean[j]+=row[j];
            }
        }
        for(int j=0;j<d;j++)mean[j]/=sourceTraining.length;
        double[][] covariance=new double[d][d];
        for(double[] row:sourceTraining)for(int i=0;i<d;i++)
            for(int j=0;j<d;j++)
                covariance[i][j]+=(row[i]-mean[i])*(row[j]-mean[j])/
                                  (sourceTraining.length-1.0);
        double[][] axes=new double[requestedComponents][d];
        double[] eigen=new double[requestedComponents];
        for(int component=0;component<requestedComponents;component++){
            double[] v=new double[d];
            v[component%d]=1;
            for(int iter=0;iter<400;iter++){
                double[] next=new double[d];
                for(int i=0;i<d;i++)for(int j=0;j<d;j++)
                    next[i]+=covariance[i][j]*v[j];
                for(int k=0;k<component;k++){
                    double proj=dot(next,axes[k]);
                    for(int j=0;j<d;j++)next[j]-=proj*axes[k][j];
                }
                double norm=Math.sqrt(dot(next,next));
                if(!(norm>EPS))break;
                for(int j=0;j<d;j++)v[j]=next[j]/norm;
            }
            double norm=Math.sqrt(dot(v,v));
            if(norm>EPS)for(int j=0;j<d;j++)v[j]/=norm;
            double lambda=0;
            for(int i=0;i<d;i++)for(int j=0;j<d;j++)
                lambda+=v[i]*covariance[i][j]*v[j];
            eigen[component]=Math.max(0,lambda);
            axes[component]=v;
        }
        return new PCA(mean,axes,eigen,sourceTraining.length);
    }

    public static final class ClassScore {
        public final String label;
        public final double score;
        public final int matchedSourceTrainingRows;
        private ClassScore(String id,double s,int n) {
            label=id;score=s;matchedSourceTrainingRows=n;
        }
    }
    public static final class DiagonalLDA {
        public final List<String> classes;
        public final int sourceTrainingRows;
        private final double[][] means;
        private final double[] pooledVariance;
        private final int[] counts;
        private DiagonalLDA(List<String> c,int n,double[][] m,double[] v,int[] numbers){
            classes=c;sourceTrainingRows=n;means=m;pooledVariance=v;counts=numbers;
        }
        public List<ClassScore> score(double[] observation){
            if(observation==null||observation.length!=pooledVariance.length)
                throw new IllegalArgumentException("LDA input dimension");
            List<ClassScore> result=new ArrayList<>();
            for(int k=0;k<classes.size();k++){
                double distance=0;
                for(int j=0;j<observation.length;j++){
                    if(!finite(observation[j]))throw new IllegalArgumentException("LDA nonfinite");
                    double z=observation[j]-means[k][j];
                    distance+=z*z/pooledVariance[j];
                }
                result.add(new ClassScore(classes.get(k),-0.5*distance,counts[k]));
            }
            result.sort(Comparator.comparingDouble((ClassScore x)->x.score)
                                  .reversed().thenComparing(x->x.label));
            return Collections.unmodifiableList(result);
        }
    }
    /** 03.4: diagonal shared-covariance LDA, equal class priors.
     * This is NOT an implementation of PLS-DA.
     */
    public static DiagonalLDA fitDiagonalLDA(Dataset data,double ridge){
        if(data==null||data.validRows<2||data.classes.size()<2||
           !finite(ridge)||ridge<=0)throw new IllegalArgumentException("LDA training");
        int k=data.classes.size(),d=data.dimension;
        double[][] means=new double[k][d];int[] counts=new int[k];
        for(int i=0;i<data.validRows;i++){
            int idx=data.classes.indexOf(data.labels[i]);counts[idx]++;
            for(int j=0;j<d;j++)means[idx][j]+=data.samples[i][j];
        }
        for(int c=0;c<k;c++)for(int j=0;j<d;j++)means[c][j]/=counts[c];
        double[] variance=new double[d];
        for(int i=0;i<data.validRows;i++){
            int c=data.classes.indexOf(data.labels[i]);
            for(int j=0;j<d;j++){
                double e=data.samples[i][j]-means[c][j];variance[j]+=e*e;
            }
        }
        for(int j=0;j<d;j++)variance[j]=variance[j]/data.validRows+ridge;
        return new DiagonalLDA(data.classes,data.validRows,means,variance,counts);
    }

    public static final class KNearest {
        private final Dataset source;
        private final Standardizer standardizer;
        private final double[][] scaled;
        private KNearest(Dataset training){
            source=training;standardizer=Standardizer.fit(training.samples);
            scaled=new double[training.validRows][];
            for(int i=0;i<scaled.length;i++)
                scaled[i]=standardizer.transform(training.samples[i]);
        }
        public List<ClassScore> rank(double[] observation,int k){
            if(k<1||k>source.validRows)
                throw new IllegalArgumentException("k must be within training rows");
            double[] x=standardizer.transform(observation);
            List<Integer> indices=new ArrayList<>();
            for(int i=0;i<scaled.length;i++)indices.add(i);
            indices.sort(Comparator.comparingDouble((Integer idx)->{
                double d=0;for(int j=0;j<x.length;j++){
                    double e=x[j]-scaled[idx][j];d+=e*e;
                }
                return d;
            }).thenComparingInt(i->i));
            Map<String,Integer> votes=new LinkedHashMap<>();
            for(String label:source.classes)votes.put(label,0);
            for(int i=0;i<k;i++)
                votes.put(source.labels[indices.get(i)],votes.get(source.labels[indices.get(i)])+1);
            List<ClassScore> output=new ArrayList<>();
            for(Map.Entry<String,Integer> item:votes.entrySet())
                output.add(new ClassScore(item.getKey(),item.getValue(),k));
            output.sort(Comparator.comparingDouble((ClassScore s)->s.score).reversed()
                                  .thenComparing(s->s.label));
            return Collections.unmodifiableList(output);
        }
    }
    /** 04.3: Euclidean KNN in TRAINING-standardized space, all tied labels returned. */
    public static KNearest fitKnn(Dataset training){
        if(training==null||training.validRows<1)
            throw new IllegalArgumentException("KNN requires training rows");
        return new KNearest(training);
    }

    public static final class SoftmaxModel {
        public final List<String> classes;
        public final int sourceTrainingRows,trainingIterations;
        public final double regularization;
        private final Standardizer transform;
        private final double[][] coefficients;
        private SoftmaxModel(Dataset dataset,Standardizer scaler,double[][] weights,
                             int iterations,double lambda){
            classes=dataset.classes;sourceTrainingRows=dataset.validRows;
            transform=scaler;coefficients=weights;
            trainingIterations=iterations;regularization=lambda;
        }
        public List<ClassScore> scores(double[] input){
            double[] x=transform.transform(input);
            double[] logits=new double[classes.size()];double max=Double.NEGATIVE_INFINITY;
            for(int c=0;c<logits.length;c++){
                double value=coefficients[c][x.length];
                for(int j=0;j<x.length;j++)value+=coefficients[c][j]*x[j];
                logits[c]=value;max=Math.max(max,value);
            }
            double normalizer=0;
            for(int c=0;c<logits.length;c++){
                logits[c]=Math.exp(logits[c]-max);normalizer+=logits[c];
            }
            List<ClassScore> result=new ArrayList<>();
            for(int c=0;c<logits.length;c++)
                result.add(new ClassScore(classes.get(c),logits[c]/normalizer,
                                          sourceTrainingRows));
            result.sort(Comparator.comparingDouble((ClassScore v)->v.score)
                                  .reversed().thenComparing(v->v.label));
            return Collections.unmodifiableList(result);
        }
    }
    /** 04.4: actual batch-gradient multiclass logistic softmax with L2 penalty.
     * Probabilities are SOFTMAX SCORES of source-trained model, NOT calibrated
     * probabilities on Tank and never used as a certified safety threshold.
     */
    public static SoftmaxModel fitSoftmax(Dataset training,int iterations,
                                          double step,double l2){
        if(training==null||training.validRows<2||training.classes.size()<2||
           iterations<1||!Double.isFinite(step)||step<=0||step>1||
           !Double.isFinite(l2)||l2<0)
            throw new IllegalArgumentException("source softmax training specification");
        int classes=training.classes.size(),d=training.dimension,n=training.validRows;
        Standardizer scaler=Standardizer.fit(training.samples);
        double[][] x=new double[n][];
        for(int i=0;i<n;i++)x[i]=scaler.transform(training.samples[i]);
        double[][] w=new double[classes][d+1];
        for(int it=0;it<iterations;it++){
            double[][] gradient=new double[classes][d+1];
            for(int i=0;i<n;i++){
                double[] logits=new double[classes];double max=Double.NEGATIVE_INFINITY;
                for(int k=0;k<classes;k++){
                    double z=w[k][d];for(int j=0;j<d;j++)z+=w[k][j]*x[i][j];
                    logits[k]=z;max=Math.max(max,z);
                }
                double denominator=0;
                for(int k=0;k<classes;k++){
                    logits[k]=Math.exp(logits[k]-max);denominator+=logits[k];
                }
                for(int k=0;k<classes;k++){
                    double error=logits[k]/denominator-
                        (training.classes.get(k).equals(training.labels[i])?1.0:0.0);
                    for(int j=0;j<d;j++)gradient[k][j]+=error*x[i][j];
                    gradient[k][d]+=error;
                }
            }
            for(int k=0;k<classes;k++){
                for(int j=0;j<d;j++)
                    w[k][j]-=step*(gradient[k][j]/n+l2*w[k][j]);
                w[k][d]-=step*gradient[k][d]/n;
            }
        }
        return new SoftmaxModel(training,scaler,w,iterations,l2);
    }
}
