package com.hazrob.tank.chemcore.next;

import java.util.List;

public final class ConditionalEvidenceRouterSmoke {
    private static int count;
    private static void ok(boolean b,String why){
        count++;if(!b)throw new AssertionError("P05.2 #"+count+": "+why);
    }
    private static SensorQualityGate.Result quality(boolean ready){
        return SensorQualityGate.evaluate("physical:MQ8","HEATER_READY",120000,
            List.of(new SensorQualityGate.Sample("physical:MQ8",119900,70000,2345,10234,8.0,
                SensorQualityGate.Transport.ACKED,ready,true,"HEATER_READY",24.0,47.0,true)),
            new SensorQualityGate.Limits(2000,60000,.2,.3,3));
    }
    private static SingleSensorEvidenceGate.SensitivityModel model(boolean hasLod){
        return new SingleSensorEvidenceGate.SensitivityModel(712,"physical:MQ8","MQ8",
            "HEATER_READY","ORIGINAL-MQ8-SELECTIVITY-ROOT",
            SingleSensorEvidenceGate.Direction.RS_DECREASES,
            hasLod?.1:null,true,true);
    }
    private static SingleSensorEvidenceGate.Observation observation(double rs,boolean ready){
        return new SingleSensorEvidenceGate.Observation("physical:MQ8","MQ8",
            "HEATER_READY",rs,10.,.02,quality(ready));
    }
    private static SingleSensorEvidenceGate.Challenge challenge(boolean independent,
                                                                boolean sourceExpectation){
        return new SingleSensorEvidenceGate.Challenge(independent,150.,sourceExpectation,.5);
    }
    private static ConditionalEvidenceRouter.ChallengeProvenance proof(String source,
                                                                      boolean verified){
        return new ConditionalEvidenceRouter.ChallengeProvenance(source,"test-reference-run",verified);
    }
    public static void main(String[] args){
        var q=ConditionalEvidenceRouter.evaluate(model(true),observation(8,true),null,null);
        ok(q.evidence.polarity==CoreContracts.EvidencePolarity.POSITIVE,"physical response research positive");
        ok(!q.gasIdentityConfirmed&&!q.ppmAuthorized&&!q.fieldAlarmAuthorized,"no qualification");
        var noChallenge=ConditionalEvidenceRouter.evaluate(model(true),
            observation(9.95,true),null,null);
        ok(noChallenge.evidence.polarity==CoreContracts.EvidencePolarity.AMBIGUOUS,
            "no physical challenge never negative");
        var missingProof=ConditionalEvidenceRouter.evaluate(model(true),
            observation(9.95,true),challenge(true,true),null);
        ok(missingProof.evidence.polarity==CoreContracts.EvidencePolarity.NOT_EVALUABLE,
            "math source challenge without provenance is NOT_EVALUABLE");
        var repeatedSource=ConditionalEvidenceRouter.evaluate(model(true),
            observation(9.95,true),challenge(true,true),
            proof("ORIGINAL-MQ8-SELECTIVITY-ROOT",true));
        ok(repeatedSource.evidence.polarity==CoreContracts.EvidencePolarity.NOT_EVALUABLE,
            "same source cannot self-certify negative");
        var unverified=ConditionalEvidenceRouter.evaluate(model(true),
            observation(9.95,true),challenge(true,true),proof("INDEPENDENT-LICOR",false));
        ok(unverified.evidence.polarity==CoreContracts.EvidencePolarity.NOT_EVALUABLE,
            "unverified challenge provenance cannot exclude");
        var qualified=ConditionalEvidenceRouter.evaluate(model(true),
            observation(9.95,true),challenge(true,true),proof("INDEPENDENT-LICOR",true));
        ok(qualified.evidence.polarity==CoreContracts.EvidencePolarity.CONDITIONAL_NEGATIVE,
            "independently evidenced known challenge allows conditional negative");
        ok(qualified.negativeChallengeProvenanceVerified,
            "separate challenge source maintained outside main source root");
        ok(qualified.challengeSourceRoot.equals("INDEPENDENT-LICOR"),"reference source trace");
        ok(!qualified.g3Qualified,"synthetic mathematical challenge not physical Tank qualification");
        var missingExpected=ConditionalEvidenceRouter.evaluate(model(true),
            observation(9.95,true),challenge(true,false),proof("INDEPENDENT-LICOR",true));
        ok(missingExpected.evidence.polarity==CoreContracts.EvidencePolarity.NOT_EVALUABLE,
            "expected physical response source unverified");
        var signalLowLod=ConditionalEvidenceRouter.evaluate(model(false),
            observation(9.95,true),challenge(true,true),proof("INDEPENDENT-LICOR",true));
        ok(signalLowLod.evidence.polarity==CoreContracts.EvidencePolarity.NOT_EVALUABLE,
            "no detection limit is not an absence");
        var stale=ConditionalEvidenceRouter.evaluate(model(true),observation(8,false),
            challenge(true,true),proof("INDEPENDENT-LICOR",true));
        ok(stale.evidence.polarity==CoreContracts.EvidencePolarity.NOT_EVALUABLE,
            "invalid acquisition blocks positive/negative");
        var shared=IndependentEvidenceLedger.consolidate("exposure",List.of(
            qualified.evidence,missingProof.evidence));
        ok(shared.independentSourceRoots==1,"two hypotheses from one source not two votes");
        ok(shared.roots.get(0).effectivePolarity==CoreContracts.EvidencePolarity.CONDITIONAL_NEGATIVE,
            "non-evaluable source does not strengthen/decrease conditional negative");
        System.out.println("CHEMCORE_NEXT_P05_2_POLARITY_PASS assertions="+count+
            " independent_challenge_required=true no_global_absence_claim=true G3=0");
    }
}
