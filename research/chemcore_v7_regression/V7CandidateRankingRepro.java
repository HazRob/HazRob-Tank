package com.hazrob.tank.chemcore.v6;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Reproduce ranking behavior of the EXACT V7 ChemCore Engine and
 * TankInstantAnalyzer projection against distinct, entirely synthetic
 * per-channel ratios. These patterns are NOT validated gas exposures.
 *
 * Build uses V7 source pinned to b344b8ac1c651794fb600baae029efd0f2522ce8.
 * V7 UI displays chemical IDs in FIRST SOURCE CURVE occurrence order,
 * not from Engine.Result.rankedCandidates.
 */
public final class V7CandidateRankingRepro {
    private static final String[] SENSORS = {
        "MQ2","MQ3","MQ4","MQ5","MQ7","MQ8","MQ9","MQ135"
    };
    private static final String[] CASES = {
        "CLEAN_AIR_REFERENCE",
        "SYNTHETIC_MQ3_DOMINANT",
        "SYNTHETIC_MQ2_MQ5_DOMINANT",
        "SYNTHETIC_MQ4_DOMINANT",
        "SYNTHETIC_MQ7_MQ9_DOMINANT"
    };
    private static final double[][] RATIOS = {
        {1.00,1.00,1.00,1.00,1.00,1.00,1.00,1.00},
        {0.77,0.23,0.82,0.78,0.91,0.68,0.94,0.44},
        {0.19,0.72,0.52,0.27,0.84,0.63,0.42,0.78},
        {0.72,0.74,0.16,0.64,0.88,0.71,0.78,0.92},
        {0.71,0.80,0.83,0.89,0.19,0.46,0.22,0.81}
    };

    private static List<Engine.Channel> channels(double[] v){
        List<Engine.Channel> out=new ArrayList<>();
        for(int i=0;i<SENSORS.length;i++)
            out.add(new Engine.Channel(SENSORS[i],SENSORS[i]+"-device-1",
                100.0*v[i],100.0));
        return out;
    }
    private static List<Long> uiSourceFirstOrder(Engine.Result e){
        // Equivalent to TankInstantAnalyzer's LinkedHashMap first-seen
        // legacy IDs. Its SQLite legacy_id -> chemical_uid join does not
        // reorder; unmapped UIDs are simply omitted.
        Set<Long> ids=new LinkedHashSet<>();
        for(Engine.SourceFit fit:e.allSourceFits)
            if(fit.chemicalId>=0&&fit.chemicalId<144)
                ids.add(fit.chemicalId);
        return new ArrayList<>(ids);
    }
    private static List<Long> rankedEngineOrder(Engine.Result e){
        List<Long> ids=new ArrayList<>();
        for(Engine.Candidate c:e.rankedCandidates)ids.add(c.chemicalId);
        return ids;
    }
    private static void ok(boolean condition,String msg){
        if(!condition)throw new AssertionError(msg);
    }

    public static void main(String[] args){
        String expectedSourceOrder="";
        String firstEngineOrder="";
        boolean engineRankChanged=false,rmseChanged=false,gateAllowedAtLeastOne=false;
        double firstEngineRmse=Double.NaN;
        for(int n=0;n<CASES.length;n++){
            List<Engine.Channel> observations=channels(RATIOS[n]);
            Engine.Result analyzed=Engine.analyze(observations);
            CleanAirResponseGateV6.Decision gate=CleanAirResponseGateV6.assess(observations);
            List<Long> display=gate.showGasHypotheses?
                uiSourceFirstOrder(analyzed):Collections.emptyList();
            List<Long> sourceOrder=uiSourceFirstOrder(analyzed);
            List<Long> ranked=rankedEngineOrder(analyzed);
            String csv=sourceOrder.toString();
            if(n==0)expectedSourceOrder=csv;
            ok(expectedSourceOrder.equals(csv),
                "V7 source-first order should be constant for all 8 numerically valid MQ channels");
            ok(analyzed.allSourceFits.size()==49,
                "V7 should compute 43 base and 6 alternative curves for all 8 channels");
            ok(analyzed.rankedCandidates.size()==13,
                "V7 independent Engine should have 13 mathematical groups");
            ok(gate.showGasHypotheses==(n!=0),
                "The clean-air gate should suppress operator list only at clean-air baseline");
            if(n>0){
                ok(display.size()==13,"V7 projection must display all 13 legacy-id groups");
                gateAllowedAtLeastOne=true;
            }
            String rankStr=ranked.toString();
            if(n==1){
                firstEngineOrder=rankStr;
                firstEngineRmse=analyzed.rankedCandidates.get(0).jointResidualRmseLog10;
            }else if(n>1){
                if(!firstEngineOrder.equals(rankStr))engineRankChanged=true;
                if(Math.abs(firstEngineRmse-
                    analyzed.rankedCandidates.get(0).jointResidualRmseLog10)>1e-9)
                    rmseChanged=true;
            }
            System.out.println("CASE="+CASES[n]+
                " gate="+gate.state+
                " sourceFits="+analyzed.allSourceFits.size()+
                " operatorCandidates="+display.size()+
                " sourceFirstLegacyIDs="+display+
                " actualEngineResearchRanking="+ranked+
                " bestEngineRmse="+String.format(Locale.ROOT,"%.7g",
                    analyzed.rankedCandidates.get(0).jointResidualRmseLog10));
        }
        ok(gateAllowedAtLeastOne,"response fixtures must pass gate");
        ok(rmseChanged,"different MQ profiles must change internal numerical residuals");
        System.out.println("OBSERVATION: same V7 DISPLAY source order, different profile RMSE;"+
            " internal ranking_changed="+engineRankChanged+
            "; display never consumes rankedCandidates.");
        System.out.println("PASS: V7 source-first candidate order insensitive to snapshot"+
            " even while numerical engine evidence changes.");
    }
}
