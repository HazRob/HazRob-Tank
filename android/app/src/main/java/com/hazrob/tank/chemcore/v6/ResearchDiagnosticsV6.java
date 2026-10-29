package com.hazrob.tank.chemcore.v6;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Clean-slate V6 source-data diagnostics, batch N03-A, five additional
 * techniques 02.7, 03.3, 03.5, 03.7 and 03.8.
 *
 * NOT a trained classifier, not a calibration, and not a safety gate:
 * all finite reference candidates are reported even when inseparable.
 */
public final class ResearchDiagnosticsV6 {
    private ResearchDiagnosticsV6(){}

    public static final class FeatureRank {
        public final int featureIndex,validRows;
        public final double mean,variance;
        FeatureRank(int index,int n,double mu,double var){
            featureIndex=index;validRows=n;mean=mu;variance=var;
        }
    }
    /** 02.7: unsupervised source-feature ranking (not a performance claim).
     * No feature removed for low variance; even a single observed value stays.
     */
    public static List<FeatureRank> featureVarianceRanking(double[][] matrix){
        if(matrix==null)return Collections.emptyList();
        int width=0;
        for(double[] row:matrix)if(row!=null)width=Math.max(width,row.length);
        List<FeatureRank> result=new ArrayList<>();
        for(int col=0;col<width;col++){
            double sum=0,sumSquares=0;int count=0;
            for(double[] row:matrix)if(row!=null && col<row.length &&
                                      Double.isFinite(row[col])){
                double x=row[col];sum+=x;sumSquares+=x*x;count++;
            }
            if(count==0)continue; // no mathematically defined variance
            double mean=sum/count;
            double sq=0;
            for(double[] row:matrix)if(row!=null && col<row.length &&
                                      Double.isFinite(row[col])){
                double diff=row[col]-mean;sq+=diff*diff;
            }
            double variance=sq/count;
            if(Double.isFinite(mean)&&Double.isFinite(variance))
                result.add(new FeatureRank(col,count,mean,variance));
        }
        result.sort(Comparator.comparingDouble((FeatureRank x)->x.variance)
                .reversed().thenComparingInt(x->x.featureIndex));
        return Collections.unmodifiableList(result);
    }

    public static final class PairDistance {
        public final String idA,idB,sourceClassA,sourceClassB,space;
        public final boolean sameClass;
        public final double distanceAtoB,distanceBtoA,symmetricMeanDistance;
        public final int sharedDimensions;
        private PairDistance(GasFingerprintCatalogV6.Signature a,
                             GasFingerprintCatalogV6.Signature b,
                             double dab,double dba,int overlap){
            idA=a.id;idB=b.id;sourceClassA=a.label;sourceClassB=b.label;
            space=a.space;sameClass=a.label.equals(b.label);
            distanceAtoB=dab;distanceBtoA=dba;
            symmetricMeanDistance=(dab/2+dba/2);sharedDimensions=overlap;
        }
    }
    private static FingerprintEngineV6.Hypothesis matchId(
            FingerprintEngineV6.Input input,String referenceId){
        for(FingerprintEngineV6.Hypothesis h:FingerprintEngineV6.match(input).hypotheses)
            if(h.fingerprintId.equals(referenceId))return h;
        return null;
    }
    /** 03.3: pairwise intra/inter-class source prototype overlaps.
     * Distances are descriptive, and only compared WITHIN SAME feature space.
     */
    public static List<PairDistance> classOverlap(String space){
        List<GasFingerprintCatalogV6.Signature> templates=
            GasFingerprintCatalogV6.inSpace(space);
        List<PairDistance> result=new ArrayList<>();
        for(int i=0;i<templates.size();i++){
            GasFingerprintCatalogV6.Signature a=templates.get(i);
            for(int j=i+1;j<templates.size();j++){
                GasFingerprintCatalogV6.Signature b=templates.get(j);
                FingerprintEngineV6.Hypothesis hAB=matchId(
                    new FingerprintEngineV6.Input(space,a.vector(),
                        "SOURCE_NATIVE_FEATURES","REFERENCE_PAIR"),b.id);
                FingerprintEngineV6.Hypothesis hBA=matchId(
                    new FingerprintEngineV6.Input(space,b.vector(),
                        "SOURCE_NATIVE_FEATURES","REFERENCE_PAIR"),a.id);
                if(hAB==null||hBA==null)continue;
                int overlap=Math.min(hAB.overlapDimensions,hBA.overlapDimensions);
                if(overlap>0&&Double.isFinite(hAB.distance)&&Double.isFinite(hBA.distance))
                    result.add(new PairDistance(a,b,hAB.distance,hBA.distance,overlap));
            }
        }
        result.sort(Comparator.comparingDouble((PairDistance p)->p.symmetricMeanDistance)
                .thenComparing(x->x.idA).thenComparing(x->x.idB));
        return Collections.unmodifiableList(result);
    }

    public static final class Importance {
        public final int featureIndex;
        public final double marginWithFeature,marginWithoutFeature,
                            signedMarginContribution;
        public final boolean numericallyDefined;
        private Importance(int index,double complete,double reduced){
            featureIndex=index;marginWithFeature=complete;marginWithoutFeature=reduced;
            signedMarginContribution=Double.isFinite(complete)&&Double.isFinite(reduced)
                ?complete-reduced:Double.NaN;
            numericallyDefined=Double.isFinite(signedMarginContribution);
        }
    }
    private static double margin(FingerprintEngineV6.Input x,
                                  String target,String rival){
        FingerprintEngineV6.Hypothesis a=matchId(x,target),b=matchId(x,rival);
        // Positive means target is closer than rival. Not a calibrated
        // classification confidence or feature-importance attribution.
        return a==null||b==null?Double.NaN:b.distance-a.distance;
    }
    /** 03.5: leave-one-coordinate-out change in target vs rival distance.
     * All finite coordinates returned; no arbitrary threshold discards a sensor.
     */
    public static List<Importance> leaveOneFeatureOut(
                  FingerprintEngineV6.Input input,String target,String rival){
        if(input==null)return Collections.emptyList();
        double[] base=input.values();
        double all=margin(input,target,rival);
        List<Importance> result=new ArrayList<>();
        for(int i=0;i<base.length;i++){
            if(!Double.isFinite(base[i]))continue;
            double[] missing=base.clone();missing[i]=Double.NaN;
            double reduced=margin(new FingerprintEngineV6.Input(
                input.space,missing,input.transformation,input.sourceContext),
                target,rival);
            result.add(new Importance(i,all,reduced));
        }
        result.sort(Comparator.comparingDouble((Importance a)->
                Double.isFinite(a.signedMarginContribution)?
                    -Math.abs(a.signedMarginContribution):Double.POSITIVE_INFINITY)
                .thenComparingInt(x->x.featureIndex));
        return Collections.unmodifiableList(result);
    }

    public static final class ConcentrationConfounding {
        public final String gas;
        public final double meanWithinGasAcrossLevels,meanBetweenGasesAtSameLevel;
        public final int intraPairs,interPairs;
        public final boolean independentlyValidatedDiscrimination;
        private ConcentrationConfounding(String label,double same,double other,
                                         int within,int between){
            gas=label;meanWithinGasAcrossLevels=same;
            meanBetweenGasesAtSameLevel=other;
            intraPairs=within;interPairs=between;
            independentlyValidatedDiscrimination=false;
        }
    }
    /**
     * 03.7: quantify source-prototype concentration confounding in UCI1081.
     * Never infer trained classifier accuracy from prototype geometry.
     * The 18 templates are 6 gases × 3 nominal reference levels (ppb).
     */
    public static List<ConcentrationConfounding> uciConcentrationConfounding(){
        List<GasFingerprintCatalogV6.Signature> uci=
            GasFingerprintCatalogV6.inSpace(FingerprintEngineV6.TEMPORAL);
        Map<String,List<GasFingerprintCatalogV6.Signature>> gases=new LinkedHashMap<>();
        for(GasFingerprintCatalogV6.Signature row:uci)
            gases.computeIfAbsent(row.label,k->new ArrayList<>()).add(row);
        List<ConcentrationConfounding> result=new ArrayList<>();
        for(Map.Entry<String,List<GasFingerprintCatalogV6.Signature>> group:gases.entrySet()){
            double intraSum=0,interSum=0;int intra=0,inter=0;
            for(GasFingerprintCatalogV6.Signature a:group.getValue()){
                FingerprintEngineV6.Input obs=new FingerprintEngineV6.Input(
                    FingerprintEngineV6.TEMPORAL,a.vector(),
                    "SOURCE_NATIVE_FEATURES","UCI_SOURCE_REFERENCE");
                for(GasFingerprintCatalogV6.Signature b:uci){
                    if(a.id.equals(b.id))continue;
                    FingerprintEngineV6.Hypothesis match=matchId(obs,b.id);
                    if(match==null||!Double.isFinite(match.distance))continue;
                    if(a.label.equals(b.label)){
                        if(a.sourceConcentrationPpm!=b.sourceConcentrationPpm){
                            intraSum+=match.distance;intra++;
                        }
                    }else if(a.sourceConcentrationPpm==b.sourceConcentrationPpm){
                        interSum+=match.distance;inter++;
                    }
                }
            }
            result.add(new ConcentrationConfounding(group.getKey(),
                intra>0?intraSum/intra:Double.NaN,
                inter>0?interSum/inter:Double.NaN,intra,inter));
        }
        return Collections.unmodifiableList(result);
    }

    public static final class Identifiability {
        public final String sensorInstance,sensorType;
        public final int alternativeSourceCurves,candidateChemicalIds,sourceRoots;
        public final List<Long> allCandidateIds;
        public final boolean uniqueIdentityEstablished;
        private Identifiability(String instance,String model,int n,int roots,
                                Set<Long> ids){
            sensorInstance=instance;sensorType=model;
            alternativeSourceCurves=n;candidateChemicalIds=ids.size();
            sourceRoots=roots;
            allCandidateIds=Collections.unmodifiableList(new ArrayList<>(ids));
            uniqueIdentityEstablished=false; // source alternatives != physical truth
        }
    }
    /** 03.8: keep ambiguity from each physical sensor explicit rather than
     * pretending an MQ response unique to one gas.
     */
    public static List<Identifiability> sourceIdentifiability(Engine.Result analysis){
        if(analysis==null)return Collections.emptyList();
        class Accumulator {
            final String instance,model;
            final Set<Long> ids=new LinkedHashSet<>();
            final Set<String> roots=new LinkedHashSet<>();
            int n=0;
            Accumulator(String i,String m){instance=i;model=m;}
            void add(Engine.SourceFit f){ids.add(f.chemicalId);roots.add(f.sourceRoot);n++;}
        }
        Map<String,Accumulator> groups=new LinkedHashMap<>();
        for(Engine.SourceFit r:analysis.allSourceFits){
            String key=r.sensorInstance+"::"+r.sensorType;
            groups.computeIfAbsent(key,k->new Accumulator(
                r.sensorInstance,r.sensorType)).add(r);
        }
        List<Identifiability> output=new ArrayList<>();
        for(Accumulator a:groups.values())
            output.add(new Identifiability(a.instance,a.model,a.n,a.roots.size(),a.ids));
        return Collections.unmodifiableList(output);
    }
}
