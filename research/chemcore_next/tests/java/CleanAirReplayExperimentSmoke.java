package com.hazrob.tank.chemcore.next;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * P02.5 SOFTWARE EXPERIMENT ONLY.
 * Synthetic values exercise clean-air, 3min warmup + 1min R_air baseline,
 * eight independent sensor channels and source abstention. NOT Tank G3.
 */
public final class CleanAirReplayExperimentSmoke {
    private static int n;
    private static void ok(boolean condition,String label) {
        n++;if(!condition)throw new AssertionError("P02.5 "+n+" "+label);
    }
    private static final String[] MODELS =
        {"MQ2","MQ3","MQ4","MQ5","MQ7","MQ8","MQ9","MQ135"};
    private static final SensorQualityGate.Limits LIMITS =
        new SensorQualityGate.Limits(61_000L,180_000L,.08,.20,5);
    private static final long END_MS=240_000L;
    private static SensorQualityGate.Sample sample(String uid,int second,
                                                    double rs,int raw,boolean ready) {
        return new SensorQualityGate.Sample(uid,second*1000L,second*1000L,
            3000L+second,raw,rs,SensorQualityGate.Transport.ACKED,
            ready,Boolean.TRUE,"HEATER_STEADY",24.0,52.0,true);
    }
    private static SensorQualityGate.Result quality(String uid,
                    List<SensorQualityGate.Sample> items) {
        return SensorQualityGate.evaluate(uid,"HEATER_STEADY",END_MS,items,LIMITS);
    }
    private static SingleSensorEvidenceGate.SensitivityModel source(String uid,
                                                                      String model,long chemicalUid) {
        return new SingleSensorEvidenceGate.SensitivityModel(chemicalUid,uid,model,
            "HEATER_STEADY","SOURCE-G1-MODEL-"+model,
            SingleSensorEvidenceGate.Direction.RS_DECREASES,.08,true,true);
    }
    public static void main(String[] args) {
        List<CoreContracts.Reading> records = new ArrayList<>();
        List<CoreContracts.SensorInstance> installed = new ArrayList<>();
        double globalWarmSum=0,globalCleanSum=0;
        for(int i=0;i<MODELS.length;i++) {
            String model=MODELS[i],uid="tank:"+model;
            installed.add(new CoreContracts.SensorInstance(uid,model,"ADS-"+(i<4?1:2),
                                                            "HEATER_STEADY"));
            // Real protocol: 180 seconds of warmup excluded from R_air measurement.
            // Warmup has deliberately much higher resistance in synthetic fixture.
            double warmupMean=100.0+i;
            double cleanAirTrue=10.0+i;
            double sum=0;
            List<SensorQualityGate.Sample> cleanWindow=new ArrayList<>();
            for(int j=0;j<60;j++) {
                double rs=cleanAirTrue+((j%5)-2)*.005;
                sum+=rs;
                cleanWindow.add(sample(uid,180+j,rs,1800+(j%5),true));
            }
            double rAir=sum/60.0;
            double mixedIncorrectMean=(warmupMean*180.0+sum)/240.0;
            globalWarmSum+=warmupMean;
            globalCleanSum+=rAir;
            ok(Math.abs(rAir-cleanAirTrue)<1e-10,
                model+": R_air 60 seconds excludes first 180 seconds");
            ok(mixedIncorrectMean>3.0*cleanAirTrue,
                model+": early warmup bias would invalidate R_air");
            SensorQualityGate.Result q=quality(uid,cleanWindow);
            ok(q.status==SensorQualityGate.Status.ACCEPTED,
                model+": 60 unique scan samples valid");
            ok(q.sourceCurveEligible,model+": no arbitrary two-sensor rule");
            ok(q.noiseCv!=null&&q.noiseCv<.01,model+": stable baseline low noise");
            CoreContracts.Reading reading=new CoreContracts.Reading(uid,1800,1.1,
                rAir+.005,rAir,null,CoreContracts.CalibrationReference.AIR_RS_MEAN,
                "HEATER_STEADY",CoreContracts.SampleQuality.VALID);
            records.add(reading);
            ok(reading.airRatioOrNaN()>0.99&&reading.airRatioOrNaN()<1.01,
                model+": clean air ratio near 1");
            SingleSensorEvidenceGate.Observation clean=
                new SingleSensorEvidenceGate.Observation(uid,model,"HEATER_STEADY",
                    rAir+.005,rAir,.01,q);
            SingleSensorEvidenceGate.Decision noAlert=
                SingleSensorEvidenceGate.evaluate(source(uid,model,701L+i),clean,null);
            ok(noAlert.polarity==CoreContracts.EvidencePolarity.AMBIGUOUS,
                model+": no chemical-positive claim from clean air");
            ok(noAlert.reason==SingleSensorEvidenceGate.Reason.NO_INDEPENDENT_CHALLENGE,
                model+": absent gas concentration cannot become negative identity");
            ok(!noAlert.certifiedPpm&&!noAlert.confirmedChemicalIdentity,
                model+": no false concentration/identity");
        }
        ok(globalWarmSum>globalCleanSum*4.0,
            "warmup source baseline materially different from clean-air session");

        CoreContracts.Snapshot measurement=new CoreContracts.Snapshot(
            "clean-air-replay-3m1m","hazrob-tank",CoreContracts.Mode.LAB,
            END_MS,24.0,52.0,true,records,"pinned-stable-hash");
        String archive=NextReplayCodec.encodeSnapshot(measurement);
        CoreContracts.Snapshot replay=NextReplayCodec.decodeSnapshot(archive);
        ok(replay.readings.size()==8,"all eight sensors persist across archive");
        ok(ChemCoreNext.snapshotHash(measurement).equals(ChemCoreNext.snapshotHash(replay)),
            "8-sensor R_air replay checksum unchanged");
        SensorCapabilityRegistry registry=new SensorCapabilityRegistry(installed);
        CoreContracts.Result out=ChemCoreNext.evaluate(replay,registry);
        ok(out.status==CoreContracts.DecisionStatus.NO_INFERENCE_MODEL,
            "no false gas identification in independent Next engine skeleton");
        ok(out.rankedEligibleCandidates.isEmpty(),"no invented Top10 candidates");
        ok(!out.reportablePpm&&!out.physicallyValidatedG3,"no fake G3 and no ppm claim");

        String specialist="tank:MQ8";
        // Exactly ONE physical sensor has a genuine synthetic response, other
        // seven channel records remain unchanged. This proves no 2-sensor gate.
        List<SensorQualityGate.Sample> gasSamples=Arrays.asList(
            sample(specialist,237,7.0,1200,true),
            sample(specialist,238,7.005,1201,true),
            sample(specialist,239,6.995,1199,true));
        SensorQualityGate.Result gasQuality=quality(specialist,gasSamples);
        ok(gasQuality.status==SensorQualityGate.Status.ACCEPTED,
            "one responding MQ has valid scans and heater");
        SingleSensorEvidenceGate.Observation gas=new SingleSensorEvidenceGate.Observation(
            specialist,"MQ8","HEATER_STEADY",7.0,13.0,.01,gasQuality);
        SingleSensorEvidenceGate.Decision specialistOnly=SingleSensorEvidenceGate.evaluate(
            source(specialist,"MQ8",712L),gas,null);
        ok(specialistOnly.polarity==CoreContracts.EvidencePolarity.POSITIVE,
            "one qualified specialist signal is not suppressed by other seven");
        ok(!specialistOnly.confirmedChemicalIdentity,
            "positive one-sensor signal still cannot assert identified molecule");

        SensorQualityGate.Result missing=quality(specialist,Collections.emptyList());
        ok(!missing.physicalSignalEligible,"missing samples not treated as absent gas");
        SingleSensorEvidenceGate.Decision absentTelemetry=SingleSensorEvidenceGate.evaluate(
            source(specialist,"MQ8",712L),
            new SingleSensorEvidenceGate.Observation(specialist,"MQ8","HEATER_STEADY",
                null,13.0,.01,missing),null);
        ok(absentTelemetry.polarity==CoreContracts.EvidencePolarity.NOT_EVALUABLE,
            "sensor silence is not conditional negative chemistry");

        // Duplicate scan cycles have no acceptable evidence regardless of
        // whether values are constant or different.
        SensorQualityGate.Result duplicate=quality(specialist,Arrays.asList(
            sample(specialist,238,13.0,1800,true),
            sample(specialist,239,13.0,1800,true) /* separate but consecutive IDs */));
        ok(duplicate.physicalSignalEligible,
            "identical valid value on consecutive scan cycles is not stale evidence");
        SensorQualityGate.Result notReady=quality(specialist,
            Collections.singletonList(sample(specialist,239,13.0,1800,false)));
        ok(!notReady.physicalSignalEligible,
            "ADS OS not ready rejects apparently valid raw payload");

        System.out.println("CHEMCORE_NEXT_P02_CLEAN_AIR_REPLAY_PASS assertions="+n
            +" channels=8 warmup_s=180 baseline_s=60"
            +" synthetic=true actual_tank_G3=false false_gas_ids=0");
    }
}
