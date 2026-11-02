package com.hazrob.tank.chemcore.next;

import java.util.Collections;

public final class SingleSensorEvidenceGateSmoke {
    private static int n;
    private static void ok(boolean value,String reason){
        ++n;if(!value)throw new AssertionError("P02.4 test "+n+": "+reason);
    }
    private static void rejects(Runnable r,String reason){
        boolean threw=false;try{r.run();}catch(IllegalArgumentException e){threw=true;}
        ok(threw,reason);
    }
    private static final SensorQualityGate.Limits LIMITS=
        new SensorQualityGate.Limits(2000,60000,.2,.3,3);
    private static SensorQualityGate.Result quality(Boolean conversionReady,
                        SensorQualityGate.Transport transport,boolean ref) {
        return SensorQualityGate.evaluate("physical:MQ8","HEATER_READY",120000,
            Collections.singletonList(new SensorQualityGate.Sample(
                "physical:MQ8",119900,70000,2345,10234,8.0,
                transport,conversionReady,Boolean.TRUE,"HEATER_READY",24.0,47.0,ref)),LIMITS);
    }
    private static SingleSensorEvidenceGate.SensitivityModel model(
        boolean resolved,boolean qualified,String model,String phase){
        return new SingleSensorEvidenceGate.SensitivityModel(
            712L,"physical:MQ8",model,phase,"literature-curve-root-1",
            SingleSensorEvidenceGate.Direction.RS_DECREASES,.1,resolved,qualified);
    }
    private static SingleSensorEvidenceGate.Observation obs(Double rs,
                 SensorQualityGate.Result q){
        return new SingleSensorEvidenceGate.Observation(
            "physical:MQ8","MQ8","HEATER_READY",rs,10.0,.02,q);
    }
    private static SingleSensorEvidenceGate.Decision assess(
        SingleSensorEvidenceGate.SensitivityModel model,Double rs,
        SingleSensorEvidenceGate.Challenge challenge) {
        return SingleSensorEvidenceGate.evaluate(model,obs(rs,quality(
            true,SensorQualityGate.Transport.ACKED,true)),challenge);
    }
    private static void reason(SingleSensorEvidenceGate.Decision x,
        CoreContracts.EvidencePolarity p,SingleSensorEvidenceGate.Reason r){
        ok(x.polarity==p,"polarity expected "+p+" got "+x.polarity);
        ok(x.reason==r,"specific reason "+r+" got "+x.reason);
        ok(!x.confirmedChemicalIdentity&&!x.certifiedPpm,
            "single signal does not establish identified gas or ppm certification");
    }
    public static void main(String[] args){
        SingleSensorEvidenceGate.SensitivityModel specialist=model(true,true,"MQ8","HEATER_READY");
        SingleSensorEvidenceGate.Decision one=assess(specialist,8.0,null);
        reason(one,CoreContracts.EvidencePolarity.POSITIVE,
            SingleSensorEvidenceGate.Reason.POSITIVE_SINGLE_SPECIALIST_RESEARCH_ONLY);
        ok(one.canonicalChemicalUid==712L,"nonlegacy unified master UID supported");
        ok(one.sourceRoot.equals("literature-curve-root-1"),"source root preserved");
        ok(one.measuredLogAirRatio<0,"sensor resistance decrease direction recorded");
        ok(one.decisionThresholdLogRatio>=.1,"noise and sensitivity LOD threshold");
        ok(!one.confirmedChemicalIdentity,"positive single-sensor response is not identity");

        SingleSensorEvidenceGate.Decision noChallenge=assess(specialist,9.95,null);
        reason(noChallenge,CoreContracts.EvidencePolarity.AMBIGUOUS,
            SingleSensorEvidenceGate.Reason.NO_INDEPENDENT_CHALLENGE);
        SingleSensorEvidenceGate.Decision weakExpected=assess(specialist,9.95,
            new SingleSensorEvidenceGate.Challenge(true,150.0,true,.12));
        reason(weakExpected,CoreContracts.EvidencePolarity.AMBIGUOUS,
            SingleSensorEvidenceGate.Reason.EXPECTED_RESPONSE_BELOW_DETECTION);
        SingleSensorEvidenceGate.Decision notIndependent=assess(specialist,9.95,
            new SingleSensorEvidenceGate.Challenge(false,150.0,true,.5));
        reason(notIndependent,CoreContracts.EvidencePolarity.AMBIGUOUS,
            SingleSensorEvidenceGate.Reason.NO_INDEPENDENT_CHALLENGE);
        SingleSensorEvidenceGate.Decision unknownConcentration=assess(specialist,9.95,
            new SingleSensorEvidenceGate.Challenge(true,null,true,.5));
        reason(unknownConcentration,CoreContracts.EvidencePolarity.AMBIGUOUS,
            SingleSensorEvidenceGate.Reason.NO_INDEPENDENT_CHALLENGE);
        SingleSensorEvidenceGate.Decision missingCurve=assess(specialist,9.95,
            new SingleSensorEvidenceGate.Challenge(true,150.0,false,.5));
        reason(missingCurve,CoreContracts.EvidencePolarity.NOT_EVALUABLE,
            SingleSensorEvidenceGate.Reason.EXPECTED_RESPONSE_UNVERIFIED);
        SingleSensorEvidenceGate.Decision knownChallenge=assess(specialist,9.95,
            new SingleSensorEvidenceGate.Challenge(true,150.0,true,.5));
        reason(knownChallenge,CoreContracts.EvidencePolarity.CONDITIONAL_NEGATIVE,
            SingleSensorEvidenceGate.Reason.QUALIFIED_ABSENCE_IN_KNOWN_CHALLENGE);
        ok(knownChallenge.canonicalChemicalUid==712L,
            "negative stays tied to candidate source not world-wide gas absence");
        SingleSensorEvidenceGate.Decision opposite=assess(specialist,12.0,
            new SingleSensorEvidenceGate.Challenge(true,150.0,true,.5));
        reason(opposite,CoreContracts.EvidencePolarity.AMBIGUOUS,
            SingleSensorEvidenceGate.Reason.RESPONSE_OPPOSITE_TO_SOURCE_DIRECTION);
        SingleSensorEvidenceGate.Decision unmapped=assess(
            model(false,true,"MQ8","HEATER_READY"),8.0,null);
        reason(unmapped,CoreContracts.EvidencePolarity.NOT_EVALUABLE,
            SingleSensorEvidenceGate.Reason.UNKNOWN_CHEMICAL_SOURCE_MAPPING);
        SingleSensorEvidenceGate.Decision unqualified=assess(
            model(true,false,"MQ8","HEATER_READY"),8.0,null);
        reason(unqualified,CoreContracts.EvidencePolarity.AMBIGUOUS,
            SingleSensorEvidenceGate.Reason.SOURCE_NOT_QUALIFIED);
        SingleSensorEvidenceGate.Decision mismatch=assess(
            model(true,true,"MQ4","HEATER_READY"),8.0,null);
        reason(mismatch,CoreContracts.EvidencePolarity.NOT_EVALUABLE,
            SingleSensorEvidenceGate.Reason.MODEL_SENSOR_OR_HEATER_MISMATCH);
        SingleSensorEvidenceGate.Decision badHeater=assess(
            model(true,true,"MQ8","HEATER_LOW"),8.0,null);
        reason(badHeater,CoreContracts.EvidencePolarity.NOT_EVALUABLE,
            SingleSensorEvidenceGate.Reason.MODEL_SENSOR_OR_HEATER_MISMATCH);

        SingleSensorEvidenceGate.Decision noReady=SingleSensorEvidenceGate.evaluate(
            specialist,obs(8.0,quality(null,SensorQualityGate.Transport.ACKED,true)),null);
        reason(noReady,CoreContracts.EvidencePolarity.NOT_EVALUABLE,
            SingleSensorEvidenceGate.Reason.INVALID_OR_LIMITED_ACQUISITION);
        SingleSensorEvidenceGate.Decision nack=SingleSensorEvidenceGate.evaluate(
            specialist,obs(8.0,quality(true,SensorQualityGate.Transport.NACK,true)),null);
        reason(nack,CoreContracts.EvidencePolarity.NOT_EVALUABLE,
            SingleSensorEvidenceGate.Reason.INVALID_OR_LIMITED_ACQUISITION);
        SingleSensorEvidenceGate.Decision noR0=SingleSensorEvidenceGate.evaluate(
            specialist,obs(8.0,quality(true,SensorQualityGate.Transport.ACKED,false)),null);
        reason(noR0,CoreContracts.EvidencePolarity.NOT_EVALUABLE,
            SingleSensorEvidenceGate.Reason.INVALID_OR_LIMITED_ACQUISITION);
        SingleSensorEvidenceGate.Decision invalidRs=assess(specialist,null,null);
        reason(invalidRs,CoreContracts.EvidencePolarity.NOT_EVALUABLE,
            SingleSensorEvidenceGate.Reason.NO_AIR_REFERENCE);
        SingleSensorEvidenceGate.SensitivityModel unknownLod=
            new SingleSensorEvidenceGate.SensitivityModel(712L,"physical:MQ8",
                "MQ8","HEATER_READY","literature-curve-root-1",
                SingleSensorEvidenceGate.Direction.RS_DECREASES,
                null,true,true);
        reason(assess(unknownLod,8.0,null),CoreContracts.EvidencePolarity.NOT_EVALUABLE,
            SingleSensorEvidenceGate.Reason.NO_DETECTION_LIMIT);
        rejects(()->new SingleSensorEvidenceGate.SensitivityModel(0,"physical:MQ8",
            "MQ8","HEATER_READY","source",SingleSensorEvidenceGate.Direction.EITHER,
            .1,true,true),"invalid master UID rejected");
        rejects(()->new SingleSensorEvidenceGate.SensitivityModel(712,"physical:MQ8",
            "MQ8","HEATER_READY","source",SingleSensorEvidenceGate.Direction.EITHER,
            Double.NaN,true,true),"NaN sensitivity rejected");
        rejects(()->SingleSensorEvidenceGate.evaluate(null,
            obs(8.0,quality(true,SensorQualityGate.Transport.ACKED,true)),null),
            "null model rejected");

        System.out.println("CHEMCORE_NEXT_P02_SINGLE_SENSOR_EVIDENCE_PASS assertions="+n
            +" one_sensor_allowed=true"
            +" negative_requires_independent_reference=true"
            +" chemical_identification=false g3=false");
    }
}
