package com.hazrob.tank.chemcore.v6;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 06.7: scientific evidence disagreement diagnostics for NEW Tank V6.
 * This is NOT an exclusion gate: all source equations and their candidate
 * hypotheses remain present even when curves give wildly different ppm.
 * ADC/temporal fingerprint scores never get subtracted from ppm estimates.
 */
public final class EvidenceConflictV6 {
    private EvidenceConflictV6(){}

    public static final class Pair {
        public final long chemicalReferenceId;
        public final String sourceCurveA,sourceCurveB,rootA,rootB,sensorA,sensorB;
        public final double log10PpmDifference,concentrationRatioMaxMin;
        public final boolean differentSourceRoots,samePhysicalSensor;
        private Pair(Engine.SourceFit a,Engine.SourceFit b){
            chemicalReferenceId=a.chemicalId;
            sourceCurveA=a.id;sourceCurveB=b.id;
            rootA=a.sourceRoot;rootB=b.sourceRoot;
            sensorA=a.sensorInstance;sensorB=b.sensorInstance;
            differentSourceRoots=!rootA.equals(rootB);
            samePhysicalSensor=sensorA.equals(sensorB);
            log10PpmDifference=Math.abs(Math.log10(a.fitPpm)-Math.log10(b.fitPpm));
            concentrationRatioMaxMin=Math.pow(10,log10PpmDifference);
        }
    }
    public static final class Result {
        public final List<Pair> allComparablePairs;
        public final int sourceCurveCount,sourceFamilies;
        public final boolean gasIdentityContradicted,automaticallyDiscardedCandidates;
        private Result(List<Pair> pairs,int curves,int roots){
            allComparablePairs=Collections.unmodifiableList(new ArrayList<>(pairs));
            sourceCurveCount=curves;sourceFamilies=roots;
            gasIdentityContradicted=false;automaticallyDiscardedCandidates=false;
        }
    }
    /** Enumerate every finite within-chemical estimate pair.
     * Different devices or physical methods can make ppm disagreements large;
     * a ratio is descriptive, not a boolean "identity false" decision.
     */
    public static Result compare(Engine.Result curves){
        if(curves==null)return new Result(Collections.emptyList(),0,0);
        Map<Long,List<Engine.SourceFit>> byChemical=new LinkedHashMap<>();
        java.util.Set<String> rootIds=new java.util.LinkedHashSet<>();
        for(Engine.SourceFit f:curves.allSourceFits){
            if(f.chemicalId<=0||!Double.isFinite(f.fitPpm)||f.fitPpm<=0)continue;
            rootIds.add(f.sourceRoot);
            byChemical.computeIfAbsent(f.chemicalId,k->new ArrayList<>()).add(f);
        }
        List<Pair> pairs=new ArrayList<>();
        for(List<Engine.SourceFit> group:byChemical.values())
            for(int i=0;i<group.size();i++)
                for(int j=i+1;j<group.size();j++)
                    pairs.add(new Pair(group.get(i),group.get(j)));
        pairs.sort(Comparator.comparingDouble((Pair p)->p.log10PpmDifference)
                              .reversed().thenComparingLong(p->p.chemicalReferenceId)
                              .thenComparing(p->p.sourceCurveA)
                              .thenComparing(p->p.sourceCurveB));
        return new Result(pairs,curves.allSourceFits.size(),rootIds.size());
    }
}
