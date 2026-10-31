package com.hazrob.tank.chemcore.v6;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Frozen V7 numerical engine + clean-air gate + source-first V7 instant projection.
 * All ratios, Rs, and R0 are SYNTHETIC. A provenanced device R0 is an
 * assumed input so the gate path can be exercised; it is NOT validated here.
 * This does not run the Android SQLite UID lookup or Android UI rendering.
 */
public final class V7ExtremeInstantReplay {
    static final String[] SENSORS={"MQ2","MQ3","MQ4","MQ5","MQ7","MQ8","MQ9","MQ135"};
    static final class Profile {
        final String id;
        final double[] rsOverR0;
        final int expectedOperator;
        Profile(String id,int n,double... ratios){
            if(ratios.length!=8)throw new IllegalArgumentException(id);
            this.id=id;rsOverR0=ratios;expectedOperator=n;
        }
    }
    private static List<Engine.Channel> getChannels(Profile v){
        List<Engine.Channel> data=new ArrayList<>();
        for(int i=0;i<8;i++){
            if(!Double.isFinite(v.rsOverR0[i]))continue; // a deliberately absent channel
            data.add(new Engine.Channel(SENSORS[i],SENSORS[i]+"-physical-1",
                v.rsOverR0[i]*100,100)); // fixed fictitious R0 = 100 kΩ
        }
        return data;
    }
    private static List<Long> sourceFirstOrder(Engine.Result result){
        Set<Long> seen=new LinkedHashSet<>();
        for(Engine.SourceFit row:result.allSourceFits){
            if(row.chemicalId>=0&&row.chemicalId<144)seen.add(row.chemicalId);
        }
        return new ArrayList<>(seen);
    }
    static String summary(Engine.Result result,int limit){
        StringBuilder s=new StringBuilder();
        int n=Math.min(limit,result.rankedCandidates.size());
        for(int i=0;i<n;i++){
            Engine.Candidate c=result.rankedCandidates.get(i);
            if(i>0)s.append(" | ");
            s.append(c.chemicalId).append(':').append(c.primaryLabel)
                .append("@").append(String.format(Locale.ROOT,"%.4f",c.jointResidualRmseLog10))
                .append("[n=").append(c.supportingSensorInstances).append(']');
        }
        return s.toString();
    }
    static void expect(boolean x,String error){if(!x)throw new AssertionError(error);}
    public static void main(String[] args){
        final List<Profile> tests=new ArrayList<>();
        // This matrix deliberately spans multiple orders of magnitude and
        // includes mirrored patterns, a single strong channel and Rs increases.
        tests.add(new Profile("REFERENCE_ALL_RATIO_1",0,
            1,1,1,1,1,1,1,1));
        tests.add(new Profile("ONE_MQ3_EXTREMELY_LOW",0,
            1,0.005,1,1,1,1,1,1));
        tests.add(new Profile("ALL_RATIO_HIGH_DRIFT",0,
            4.5,5.5,3.8,6.4,4.0,7.5,5.0,3.5));
        tests.add(new Profile("ALL_RATIO_VERY_LOW",13,
            0.01,0.03,0.06,0.02,0.08,0.015,0.045,0.025));
        tests.add(new Profile("EXTREME_MQ3_MQ135",13,
            1,0.003,1.8,1.5,1.4,1.3,1.2,0.007));
        tests.add(new Profile("EXTREME_MQ4_MQ5",13,
            1.6,1.1,0.004,0.009,2.2,2.5,1.9,1.7));
        tests.add(new Profile("EXTREME_MQ7_MQ9",13,
            3.5,2.6,4.5,2.0,0.006,3.0,0.012,2.9));
        tests.add(new Profile("ALTERNATING_LOWS_HIGHS",13,
            0.008,6.0,0.020,5.0,0.005,4.0,0.040,3.0));
        tests.add(new Profile("FOUR_PRESENT_TWO_RESPOND",0,
            Double.NaN,0.01,Double.NaN,0.02,Double.NaN,1.0,Double.NaN,1.0));
        tests.add(new Profile("FOUR_PRESENT_TWO_RESPOND_VALID",13,
            Double.NaN,0.01,Double.NaN,0.02,1,1,Double.NaN,Double.NaN));
        List<Long> constantOrder=null;
        Set<String> engineRankings=new LinkedHashSet<>();
        Set<String> scoreSequences=new LinkedHashSet<>();
        for(Profile test:tests){
            List<Engine.Channel> inputs=getChannels(test);
            Engine.Result engine=Engine.analyze(inputs);
            CleanAirResponseGateV6.Decision gate=CleanAirResponseGateV6.assess(inputs);
            List<Long> ranked=sourceFirstOrder(engine);
            List<Long> ui=gate.showGasHypotheses?ranked:Collections.<Long>emptyList();
            if(inputs.size()==8){
                expect(ranked.size()==13,test.id+": expected all 13 source IDs");
                expect(engine.allSourceFits.size()==49,test.id+": expected 49 curves");
                if(constantOrder==null)constantOrder=ranked;
                expect(constantOrder.equals(ranked),test.id+": V7 source-first projection changed");
            }
            if(test.expectedOperator==13)
                expect(ui.size()==13,test.id+": expected 13 operator candidates");
            else if(test.expectedOperator==0)
                expect(ui.size()==0,test.id+": expected gate to suppress operator list");
            else throw new AssertionError("fixture unsupported");
            if(!engine.rankedCandidates.isEmpty()){
                engineRankings.add(engine.rankedCandidates.toString());
                scoreSequences.add(summary(engine,3));
            }
            StringBuilder ratios=new StringBuilder();
            for(int i=0;i<8;i++)ratios.append(i==0?"":",")
                .append(Double.isNaN(test.rsOverR0[i])?"MISSING":
                    String.format(Locale.ROOT,"%.4f",test.rsOverR0[i]));
            System.out.println("CASE="+test.id+
                " ratios="+ratios+
                " ratioReady="+engine.ratioReadyChannels+
                " responders="+gate.respondingSensorModels+
                " opposite="+gate.risingOrOppositeModels+
                " gate="+gate.state+
                " show="+gate.showGasHypotheses+
                " sourceFits="+engine.allSourceFits.size()+
                " displayedCount="+ui.size()+
                " displayedLegacyIds="+ui+
                " numericalTop3="+summary(engine,3));
        }
        expect(scoreSequences.size()>3,"Expected varying numerical ranking/fit across disparate samples");
        System.out.println("VERDICT: extremal input changes numerical fit, but V7"+
            " UI projection is catalog-order only when gated open.");
        System.out.println("PASS: "+tests.size()+" extreme V7 numerical/screening fixtures"+
            "; NOT a live firmware/Android UI/chemical identification validation.");
    }
}
