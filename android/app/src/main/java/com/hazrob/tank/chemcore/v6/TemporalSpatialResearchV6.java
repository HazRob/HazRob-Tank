package com.hazrob.tank.chemcore.v6;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * V6-N03-E: ten independent, partial research primitives for temporal,
 * mixture and spatial gas-response evidence. These are NOT motion commands,
 * ppm calibration, automatic rescue/navigation or Tank G3 validation.
 * All values are in declared, consistent source feature spaces; food excluded.
 */
public final class TemporalSpatialResearchV6 {
    private TemporalSpatialResearchV6() {}
    private static final double EPS=1e-12;
    private static void require(boolean x,String why){
        if(!x)throw new IllegalArgumentException(why);
    }
    private static void v(double[] a){
        require(a!=null&&a.length>0,"nonempty source vector");
        for(double d:a)require(Double.isFinite(d),"nonfinite source value");
    }
    private static double dot(double[] a,double[] b){
        double s=0;for(int i=0;i<a.length;i++)s+=a[i]*b[i];return s;
    }
    private static double norm2(double[] a){
        return dot(a,a);
    }
    /** 08.5 temporal ensemble: each time window contributes once by
     * declared weighting, without pretending repeated windows independent.
     */
    public static double[] temporalEnsemble(double[][] scores,double[] weights){
        require(scores!=null&&weights!=null&&scores.length>0&&
                scores.length==weights.length,"source windows and weights aligned");
        v(scores[0]);int d=scores[0].length;double[] fused=new double[d];
        double sum=0;
        for(int i=0;i<scores.length;i++){
            v(scores[i]);require(scores[i].length==d,"common class axes required");
            require(Double.isFinite(weights[i])&&weights[i]>=0,
                    "nonnegative finite window weight");
            sum+=weights[i];
        }
        require(Double.isFinite(sum)&&sum>0,"positive ensemble weight");
        for(int i=0;i<scores.length;i++)for(int j=0;j<d;j++)
            fused[j]+=scores[i][j]*(weights[i]/sum);
        v(fused);return fused;
    }

    public static final class TransferAudit {
        public final int pairedRows,features;
        public final double[] meanSignedDifference,rootMeanSquaredDifference;
        private TransferAudit(int n,int d,double[] bias,double[] rmse){
            pairedRows=n;features=d;meanSignedDifference=bias;rootMeanSquaredDifference=rmse;
        }
    }
    /** 08.7 sim-to-real: DESCRIPTIVE paired residuals; paired target rows
     * must be real by caller provenance, never manufactured by this method.
     */
    public static TransferAudit simToRealAudit(double[][] simulated,double[][] measured){
        require(simulated!=null&&measured!=null&&simulated.length>1&&
                simulated.length==measured.length,"paired simulation/source observations");
        v(simulated[0]);int d=simulated[0].length,n=simulated.length;
        double[] bias=new double[d],rmse=new double[d];
        for(int i=0;i<n;i++){
            v(simulated[i]);v(measured[i]);
            require(simulated[i].length==d&&measured[i].length==d,"paired dimension");
            for(int j=0;j<d;j++){
                double e=measured[i][j]-simulated[i][j];
                bias[j]+=e/n;rmse[j]+=e*e/n;
            }
        }
        for(int j=0;j<d;j++)rmse[j]=Math.sqrt(rmse[j]);
        v(bias);v(rmse);return new TransferAudit(n,d,bias,rmse);
    }

    public static final class UnmixResult {
        public final double[] nonnegativeCoefficients;
        public final double residualSquared;
        public final int iterations,sourceProfiles;
        public final boolean chemicalMixtureConfirmed;
        private UnmixResult(double[] c,double residual,int its,int k){
            nonnegativeCoefficients=c;residualSquared=residual;iterations=its;
            sourceProfiles=k;chemicalMixtureConfirmed=false;
        }
    }
    /** 08.9: nonnegative multi-prototype coordinate descent (NNLS).
     * Strongly overlapping profiles may have nonunique coefficients.
     */
    public static UnmixResult unmix(double[] observed,double[][] profiles,
                                    int maxIterations,double stopTolerance){
        v(observed);
        require(profiles!=null&&profiles.length>=2&&maxIterations>0&&
                maxIterations<=100000&&Double.isFinite(stopTolerance)&&
                stopTolerance>=0,"source profiles and solver limits");
        int k=profiles.length,d=observed.length;
        for(double[] p:profiles){
            v(p);require(p.length==d&&norm2(p)>EPS,"aligned nonzero profiles");
        }
        double[] c=new double[k],prediction=new double[d];
        int iteration=0;
        for(;iteration<maxIterations;iteration++){
            double largest=0;
            for(int j=0;j<k;j++){
                double gradient=0;
                for(int q=0;q<d;q++)
                    gradient+=profiles[j][q]*(observed[q]-prediction[q]);
                double next=Math.max(0,c[j]+gradient/norm2(profiles[j]));
                double delta=next-c[j];
                c[j]=next;largest=Math.max(largest,Math.abs(delta));
                for(int q=0;q<d;q++)prediction[q]+=delta*profiles[j][q];
            }
            if(largest<=stopTolerance){iteration++;break;}
        }
        double residual=0;
        for(int q=0;q<d;q++){
            double e=observed[q]-prediction[q];residual+=e*e;
        }
        require(Double.isFinite(residual)&&residual>=0,"unmixing finite residual");
        return new UnmixResult(c,residual,iteration,k);
    }

    /** 08.10: multianalyte source regression with one selected feature per
     * target, trained only on provided reference rows. NOT mixture ppm.
     */
    public static final class MultiRegression {
        public final int trainingRows,targets,featureCount;
        public final double[] intercept,slope;
        public final int[] selectedFeature;
        private MultiRegression(int n,int d,int[] feature,double[] b,double[] w){
            trainingRows=n;targets=feature.length;featureCount=d;
            selectedFeature=feature;intercept=b;slope=w;
        }
        public double[] predict(double[] x){
            v(x);require(x.length==featureCount,"source feature vector dimensions");
            double[] out=new double[targets];
            for(int j=0;j<targets;j++)out[j]=intercept[j]+slope[j]*x[selectedFeature[j]];
            v(out);return out;
        }
    }
    public static MultiRegression fitIndependentTargets(double[][] x,double[][] y,
                                                       double ridge){
        require(x!=null&&y!=null&&x.length>=3&&x.length==y.length&&
                Double.isFinite(ridge)&&ridge>0,"training-only source regression");
        v(x[0]);v(y[0]);int d=x[0].length,t=y[0].length,n=x.length;
        for(int i=0;i<n;i++){v(x[i]);v(y[i]);require(x[i].length==d&&y[i].length==t,"training axes");}
        int[] best=new int[t];double[] intercept=new double[t],slope=new double[t];
        for(int j=0;j<t;j++){
            double meanY=0;for(int i=0;i<n;i++)meanY+=y[i][j]/n;
            double smallest=Double.POSITIVE_INFINITY;
            for(int feature=0;feature<d;feature++){
                double meanX=0;for(int i=0;i<n;i++)meanX+=x[i][feature]/n;
                double cov=0,var=0;
                for(int i=0;i<n;i++){
                    double z=x[i][feature]-meanX;
                    cov+=z*(y[i][j]-meanY);var+=z*z;
                }
                double b=cov/(var+ridge),a=meanY-b*meanX;
                double sse=0;
                for(int i=0;i<n;i++){double e=y[i][j]-(a+b*x[i][feature]);sse+=e*e;}
                if(sse<smallest){smallest=sse;best[j]=feature;intercept[j]=a;slope[j]=b;}
            }
        }
        return new MultiRegression(n,d,best,intercept,slope);
    }

    public static final class ComplexResponse {
        public final double[] additiveExpectation,observedResidual;
        public final double residualSquared;
        public final boolean identifiedProduct;
        private ComplexResponse(double[] expected,double[] residual,double s){
            additiveExpectation=expected;observedResidual=residual;residualSquared=s;
            identifiedProduct=false;
        }
    }
    /** 08.12: additive source-matrix interaction diagnostic, NOT a product
     * detector. Residual may represent nonlinear cross-sensitivity.
     */
    public static ComplexResponse complexResponse(double[][] sourceSignatures,
                                                  double[] assumedFractions,double[] observed){
        v(observed);require(sourceSignatures!=null&&assumedFractions!=null&&
                sourceSignatures.length==assumedFractions.length&&
                sourceSignatures.length>=2,"source product signature inputs");
        double[] expected=new double[observed.length];
        for(int i=0;i<sourceSignatures.length;i++){
            v(sourceSignatures[i]);require(sourceSignatures[i].length==observed.length&&
                Double.isFinite(assumedFractions[i])&&assumedFractions[i]>=0,
                "aligned nonnegative source fraction");
            for(int j=0;j<expected.length;j++)expected[j]+=
                assumedFractions[i]*sourceSignatures[i][j];
        }
        double[] residual=new double[observed.length];double s=0;
        for(int j=0;j<residual.length;j++){
            residual[j]=observed[j]-expected[j];s+=residual[j]*residual[j];
        }
        v(expected);v(residual);require(Double.isFinite(s),"finite source difference");
        return new ComplexResponse(expected,residual,s);
    }

    public static final class PoseReading {
        public final double x,y,signal;
        public final long timeMs;
        public final String measurementId;
        public PoseReading(String id,double x,double y,long t,double signal){
            require(id!=null&&!id.isEmpty()&&Double.isFinite(x)&&Double.isFinite(y)&&
                    t>=0&&Double.isFinite(signal),"valid user-supplied robot pose and sensor response");
            measurementId=id;this.x=x;this.y=y;timeMs=t;this.signal=signal;
        }
    }
    /** 09.1: immutable user-provided observation trace, monotonic times,
     * no wheel commands or inferred navigation/location accuracy.
     */
    public static List<PoseReading> olfactionTrace(List<PoseReading> readings){
        require(readings!=null&&!readings.isEmpty(),"spatial acquisition required");
        List<PoseReading> result=new ArrayList<>();
        long time=-1;
        for(PoseReading p:readings){
            require(p!=null&&p.timeMs>=time,"time-ordered pose observations");
            time=p.timeMs;result.add(p);
        }
        return Collections.unmodifiableList(result);
    }

    public static final class Estimate {
        public final double value,weightSum;
        public final int contributingObservations;
        private Estimate(double value,double weight,int n){
            this.value=value;weightSum=weight;contributingObservations=n;
        }
    }
    /** 09.2: inverse-distance spatial interpolation of SOURCE RESPONSE,
     * not airborne chemical concentration or a certified gas map.
     */
    public static Estimate idwMap(List<PoseReading> readings,double x,double y,
                                  double radius,double exponent) {
        require(readings!=null&&Double.isFinite(x)&&Double.isFinite(y)&&
                Double.isFinite(radius)&&radius>0&&
                Double.isFinite(exponent)&&exponent>0,"map geometry/parameters");
        double numerator=0,weight=0,atPoint=0;int n=0,exact=0;
        for(PoseReading p:readings){
            require(p!=null,"null original observation");
            double distance=Math.hypot(p.x-x,p.y-y);
            if(distance<=EPS){atPoint+=p.signal;exact++;continue;}
            if(distance>radius)continue;
            double w=1/Math.pow(distance,exponent);
            if(!Double.isFinite(w))continue;
            numerator+=w*p.signal;weight+=w;n++;
        }
        if(exact>0)return new Estimate(atPoint/exact,exact,exact);
        require(n>0&&weight>0&&Double.isFinite(numerator),"no nearby source observations");
        return new Estimate(numerator/weight,weight,n);
    }

    public static final class Localization {
        public final double x,y;
        public final String peakMeasurementId;
        public final int contributingObservations;
        public final boolean physicalSourceConfirmed;
        private Localization(double x,double y,String id,int n){
            this.x=x;this.y=y;peakMeasurementId=id;contributingObservations=n;
            physicalSourceConfirmed=false;
        }
    }
    /** 09.3: positive-response weighted centroid and peak observation.
     * A concentration gradient may be wind driven; source NOT established.
     */
    public static Localization sourceLocalization(List<PoseReading> readings,
                                                  double referenceSignal){
        require(readings!=null&&!readings.isEmpty()&&
                Double.isFinite(referenceSignal),"source localization inputs");
        double sum=0,x=0,y=0;int n=0;PoseReading best=null;
        for(PoseReading p:readings){
            require(p!=null,"missing pose observation");
            double w=Math.max(0,p.signal-referenceSignal);
            if(w<=0)continue;
            sum+=w;x+=w*p.x;y+=w*p.y;n++;
            if(best==null||p.signal>best.signal)best=p;
        }
        require(sum>0&&Double.isFinite(sum),"no positive response above explicit reference");
        return new Localization(x/sum,y/sum,best.measurementId,n);
    }

    public static final class SampleSite {
        public final double x,y,minSeparation;
        public final int candidateIndex;
        private SampleSite(double x,double y,double distance,int index){
            this.x=x;this.y=y;minSeparation=distance;candidateIndex=index;
        }
    }
    /** 09.4: purely geometric farthest-point next sampling proposal.
     * Not an autonomous driving/motion decision or safe route planner.
     */
    public static SampleSite proposeSampleSite(List<PoseReading> source,
                                               double[][] candidateLocations){
        require(source!=null&&!source.isEmpty()&&candidateLocations!=null&&
                candidateLocations.length>0,"known samples and candidate sites");
        SampleSite best=null;
        for(int k=0;k<candidateLocations.length;k++){
            double[] site=candidateLocations[k];
            require(site!=null&&site.length==2&&Double.isFinite(site[0])&&
                    Double.isFinite(site[1]),"finite 2D site coordinates");
            double min=Double.POSITIVE_INFINITY;
            for(PoseReading p:source){
                require(p!=null,"null source measurement");
                min=Math.min(min,Math.hypot(site[0]-p.x,site[1]-p.y));
            }
            if(best==null||min>best.minSeparation)
                best=new SampleSite(site[0],site[1],min,k);
        }
        return best;
    }

    /** 09.5: separable spatial-temporal Gaussian kernel averaging. Returned
     * value is source sensor response; no plume transport physics or ppm.
     */
    public static Estimate spatiotemporal(List<PoseReading> measurements,
                                         double x,double y,long timeMs,
                                         double spaceSigma,double timeSigmaMs){
        require(measurements!=null&&!measurements.isEmpty()&&
                Double.isFinite(x)&&Double.isFinite(y)&&timeMs>=0&&
                Double.isFinite(spaceSigma)&&spaceSigma>0&&
                Double.isFinite(timeSigmaMs)&&timeSigmaMs>0,"space-time query");
        double value=0,total=0;int used=0;
        for(PoseReading p:measurements){
            require(p!=null,"null time-series observation");
            double dx=(x-p.x)/spaceSigma,dy=(y-p.y)/spaceSigma;
            double dt=((double)timeMs-p.timeMs)/timeSigmaMs;
            double exponent=-.5*(dx*dx+dy*dy+dt*dt);
            double w=Math.exp(exponent);
            if(w==0)continue;
            total+=w;value+=w*p.signal;used++;
        }
        require(total>0&&Double.isFinite(value)&&Double.isFinite(total),
                "no numerically supported local space-time evidence");
        return new Estimate(value/total,total,used);
    }
}
