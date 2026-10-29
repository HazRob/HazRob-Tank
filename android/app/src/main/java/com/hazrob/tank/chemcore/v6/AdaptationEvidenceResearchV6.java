package com.hazrob.tank.chemcore.v6;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * N03-F five source-evidence research primitives: 06.8/07.6/07.7/07.9/10.1.
 * Caller MUST provide independently sourced training and calibration provenance.
 * No physical Tank gas/ppm validation; no legacy engine or food inputs.
 */
public final class AdaptationEvidenceResearchV6 {
    private AdaptationEvidenceResearchV6(){}
    private static final double EPS=1e-12;
    private static void check(boolean value,String reason){
        if(!value)throw new IllegalArgumentException(reason);
    }
    private static void vec(double[] row) {
        check(row!=null&&row.length>0,"nonempty source feature vector");
        for(double x:row)check(Double.isFinite(x),"finite source feature required");
    }
    private static int matrix(double[][] x,int minRows) {
        check(x!=null&&x.length>=minRows,"source reference rows");
        vec(x[0]);int d=x[0].length;
        for(double[] row:x){vec(row);check(row.length==d,"source feature axes mismatch");}
        return d;
    }
    private static double dot(double[] a,double[] b){
        double s=0;for(int j=0;j<a.length;j++)s+=a[j]*b[j];return s;
    }

    /** 06.8: exact interventional Shapley attribution over FIXED reference
     * baseline for <=12 features; conditional dependence is NOT modeled.
     * This is not full SHAP library TreeExplainer or causal importance.
     */
    public interface ScalarSourceModel {double score(double[] features);}
    public static final class ShapleyAudit {
        public final double[] contributions;
        public final double baselineScore,observationScore;
        public final boolean causalExplanationValidated;
        private ShapleyAudit(double[] a,double b,double o) {
            contributions=a;baselineScore=b;observationScore=o;
            causalExplanationValidated=false;
        }
    }
    public static ShapleyAudit fixedReferenceShapley(ScalarSourceModel model,
                                                      double[] reference,double[] observation){
        check(model!=null,"source function required");vec(reference);vec(observation);
        check(reference.length==observation.length&&observation.length<=12,
              "exact reference SHAP dimension capped at 12");
        int d=reference.length,sets=1<<d;
        double[] subsetScore=new double[sets];
        for(int mask=0;mask<sets;mask++){
            double[] x=reference.clone();
            for(int j=0;j<d;j++)if((mask&(1<<j))!=0)x[j]=observation[j];
            double score=model.score(x);
            check(Double.isFinite(score),"source function returned undefined score");
            subsetScore[mask]=score;
        }
        double[] attribution=new double[d];
        for(int mask=0;mask<sets;mask++){
            int s=Integer.bitCount(mask);
            if(s==d)continue;
            double weight=1.0/d/binomial(d-1,s);
            for(int j=0;j<d;j++)if((mask&(1<<j))==0)
                attribution[j]+=weight*
                    (subsetScore[mask|(1<<j)]-subsetScore[mask]);
        }
        return new ShapleyAudit(attribution,subsetScore[0],subsetScore[sets-1]);
    }
    private static double binomial(int n,int k) {
        double value=1;
        for(int i=1;i<=k;i++)value=value*(n-k+i)/i;
        return value;
    }

    /** 07.6: diagonal CORAL-style mean/variance alignment of TARGET source
     * coordinates into a separately supplied REFERENCE source domain.
     * Paired domain labels/physical compatibility are not demonstrated.
     */
    public static final class DiagonalDomainTransfer {
        public final int referenceRows,targetRows,featureCount;
        public final double[] referenceMean,targetMean,referenceStd,targetStd;
        private DiagonalDomainTransfer(int n,int m,double[] a,double[] b,
                                        double[] as,double[] bs){
            referenceRows=n;targetRows=m;featureCount=a.length;
            referenceMean=a;targetMean=b;referenceStd=as;targetStd=bs;
        }
        public double[] targetIntoReference(double[] targetVector){
            vec(targetVector);check(targetVector.length==featureCount,"domain dimensions");
            double[] out=new double[featureCount];
            for(int j=0;j<featureCount;j++)
                out[j]=(targetVector[j]-targetMean[j])*referenceStd[j]/targetStd[j]+
                        referenceMean[j];
            vec(out);return out;
        }
    }
    private static double[][] moments(double[][] x) {
        int d=x[0].length;double[][] out=new double[2][d];
        for(double[] row:x)for(int j=0;j<d;j++)out[0][j]+=row[j]/x.length;
        for(double[] row:x)for(int j=0;j<d;j++){
            double z=row[j]-out[0][j];out[1][j]+=z*z/x.length;
        }
        for(int j=0;j<d;j++)out[1][j]=Math.sqrt(out[1][j]);
        return out;
    }
    public static DiagonalDomainTransfer fitDiagonalDomainTransfer(
                       double[][] referenceTraining,double[][] targetCalibration){
        int d=matrix(referenceTraining,2);
        check(matrix(targetCalibration,2)==d,"domain matching feature axes");
        double[][] a=moments(referenceTraining),b=moments(targetCalibration);
        for(int j=0;j<d;j++)
            check(a[1][j]>EPS&&b[1][j]>EPS,
                  "cannot invent adaptation variance for constant domain channel");
        return new DiagonalDomainTransfer(referenceTraining.length,targetCalibration.length,
                                         a[0],b[0],a[1],b[1]);
    }

    /** 07.7: transfer learning by fitting source-labelled least squares
     * penalized toward externally trained prior weights (including bias).
     * Small matrix solved by pivoted Gauss-Jordan; not neural fine tuning.
     */
    public static final class RidgeTransfer {
        public final double[] coefficients; // last coefficient is intercept
        public final int fineTuneSourceRows,featureCount;
        private RidgeTransfer(double[] w,int n,int d){
            coefficients=w;fineTuneSourceRows=n;featureCount=d;
        }
        public double predict(double[] x) {
            vec(x);check(x.length==featureCount,"transfer source axes");
            double y=coefficients[featureCount];
            for(int j=0;j<featureCount;j++)y+=coefficients[j]*x[j];
            return y;
        }
    }
    public static RidgeTransfer fineTuneRidge(double[][] x,double[] labels,
                                              double[] pretrained,double lambda) {
        int d=matrix(x,2),n=x.length;vec(labels);vec(pretrained);
        check(labels.length==n&&pretrained.length==d+1&&
            Double.isFinite(lambda)&&lambda>0,"source transfer weights and labels");
        int size=d+1;
        double[][] a=new double[size][size+1];
        for(int i=0;i<size;i++){
            a[i][i]+=lambda;a[i][size]+=lambda*pretrained[i];
        }
        for(int i=0;i<n;i++){
            for(int j=0;j<size;j++){
                double zj=j==d?1:x[i][j];
                a[j][size]+=zj*labels[i];
                for(int k=0;k<size;k++)
                    a[j][k]+=zj*(k==d?1:x[i][k]);
            }
        }
        for(int k=0;k<size;k++){
            int pivot=k;
            for(int i=k+1;i<size;i++)
                if(Math.abs(a[i][k])>Math.abs(a[pivot][k]))pivot=i;
            check(Math.abs(a[pivot][k])>EPS,"singular source transfer fit");
            double[] tmp=a[k];a[k]=a[pivot];a[pivot]=tmp;
            double divisor=a[k][k];
            for(int j=k;j<=size;j++)a[k][j]/=divisor;
            for(int i=0;i<size;i++)if(i!=k){
                double factor=a[i][k];
                for(int j=k;j<=size;j++)a[i][j]-=factor*a[k][j];
            }
        }
        double[] w=new double[size];
        for(int j=0;j<size;j++)w[j]=a[j][size];
        vec(w);return new RidgeTransfer(w,n,d);
    }

    /** 07.9: auditable one-sample elastic consolidation step.
     * This does NOT claim resistance to catastrophic forgetting or authorise
     * unsupervised model promotion on an operational Tank.
     */
    public static final class ConsolidationStep {
        public final double[] updatedWeights;
        public final double squaredErrorBefore,squaredErrorAfter;
        public final boolean safeToPromoteWithoutHoldout;
        private ConsolidationStep(double[] weights,double before,double after){
            updatedWeights=weights;squaredErrorBefore=before;squaredErrorAfter=after;
            safeToPromoteWithoutHoldout=false;
        }
    }
    public static ConsolidationStep consolidate(double[] currentWeights,
                                                double[] frozenReference,
                                                double[] importance,
                                                double[] newLabeledFeatures,
                                                double target,double learningRate,
                                                double stabilization) {
        vec(currentWeights);vec(frozenReference);vec(importance);vec(newLabeledFeatures);
        int d=currentWeights.length;
        check(frozenReference.length==d&&importance.length==d&&
              newLabeledFeatures.length==d&&Double.isFinite(target)&&
              Double.isFinite(learningRate)&&learningRate>0&&learningRate<=1&&
              Double.isFinite(stabilization)&&stabilization>=0,
              "continual learning source metadata");
        double before=dot(currentWeights,newLabeledFeatures)-target;
        double[] updated=currentWeights.clone();
        for(int j=0;j<d;j++){
            check(importance[j]>=0,"nonnegative externally estimated importance");
            double penalty=stabilization*importance[j]*
                           (currentWeights[j]-frozenReference[j]);
            updated[j]-=learningRate*(before*newLabeledFeatures[j]+penalty);
        }
        vec(updated);
        double after=dot(updated,newLabeledFeatures)-target;
        return new ConsolidationStep(updated,before*before,after*after);
    }

    /** 10.1: transparent negative evidence ledger. NEVER delete hypotheses.
     * Negative detection can only flag a specific candidate if caller provides
     * source-qualified, Tank-calibrated, sensor-operational evidence at its
     * applicable detection limit, plus minimum stability observations.
     * All candidate identities and unknown remain available in the result.
     */
    public static final class NegativeSensorObservation {
        public final String sensorId,provenanceId;
        public final String negativeForFamily;
        public final boolean tankCalibrated,operational,belowCertifiedDetectionLimit;
        public final int independentStableMeasurements;
        public NegativeSensorObservation(String id,String provenance,String family,
             boolean calibrated,boolean operational,boolean belowLimit,int count){
            check(id!=null&&!id.trim().isEmpty()&&provenance!=null&&
                  !provenance.trim().isEmpty()&&family!=null&&!family.trim().isEmpty()&&
                  count>=0,"qualified sensor evidence metadata required");
            sensorId=id;provenanceId=provenance;negativeForFamily=family;
            tankCalibrated=calibrated;this.operational=operational;
            belowCertifiedDetectionLimit=belowLimit;
            independentStableMeasurements=count;
        }
    }
    public static final class Hypothesis {
        public final String name,family;
        public Hypothesis(String name,String family){
            check(name!=null&&!name.trim().isEmpty()&&family!=null&&
                  !family.trim().isEmpty(),"chemical family metadata");
            this.name=name;this.family=family;
        }
    }
    public static final class NegativeAssessment {
        public final Hypothesis hypothesis;
        public final boolean qualifiedNegativeEvidence;
        public final List<String> supportingSensorIds,sourceProvenanceIds;
        private NegativeAssessment(Hypothesis h,List<String> sensors,List<String> sources){
            hypothesis=h;supportingSensorIds=Collections.unmodifiableList(sensors);
            sourceProvenanceIds=Collections.unmodifiableList(sources);
            qualifiedNegativeEvidence=!sensors.isEmpty();
        }
    }
    public static List<NegativeAssessment> assessNegativeEvidence(
                    List<Hypothesis> candidates,
                    List<NegativeSensorObservation> sensorEvidence,
                    int minimumStableMeasurements){
        check(candidates!=null&&sensorEvidence!=null&&
              minimumStableMeasurements>0,"negative gate source inputs");
        List<NegativeAssessment> result=new ArrayList<>();
        for(Hypothesis h:candidates){
            check(h!=null,"source candidate missing");
            List<String> sensors=new ArrayList<>(),provenance=new ArrayList<>();
            for(NegativeSensorObservation evidence:sensorEvidence){
                check(evidence!=null,"null sensor observation");
                if(evidence.negativeForFamily.equals(h.family)&&
                   evidence.tankCalibrated&&evidence.operational&&
                   evidence.belowCertifiedDetectionLimit&&
                   evidence.independentStableMeasurements>=minimumStableMeasurements){
                    sensors.add(evidence.sensorId);
                    provenance.add(evidence.provenanceId);
                }
            }
            result.add(new NegativeAssessment(h,sensors,provenance));
        }
        return Collections.unmodifiableList(result);
    }
}
