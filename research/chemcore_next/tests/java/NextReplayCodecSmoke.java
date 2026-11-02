package com.hazrob.tank.chemcore.next;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** P01.4 no-device deterministic scientific replay acceptance test. */
public final class NextReplayCodecSmoke {
    private static int count;
    private static void yes(boolean condition,String message) {
        count++;
        if(!condition) throw new AssertionError("TEST "+count+" FAILED: "+message);
    }
    private static void rejected(Runnable action,String message) {
        boolean failed=false;
        try{action.run();}catch(IllegalArgumentException expected){failed=true;}
        yes(failed,message);
    }
    private static CoreContracts.Reading valid(String sensor,double rs) {
        return new CoreContracts.Reading(sensor,1234,1.1234567890123,rs,10.0,2.0,
                CoreContracts.CalibrationReference.SOURCE_R0,"HEATER_LOW",
                CoreContracts.SampleQuality.VALID);
    }
    private static CoreContracts.Snapshot snap(List<CoreContracts.Reading> data) {
        return new CoreContracts.Snapshot("measurement-42","hazrob-tank",CoreContracts.Mode.FIELD_SESSION,
                120_010L,22.123456789,48.998877,true,data,"stableManifestSHA-immutable");
    }
    public static void main(String[] args) {
        List<CoreContracts.Reading> readings=new ArrayList<>();
        readings.add(valid("mq2-A",7.1234567890123));
        readings.add(valid("mq7-B",9.9999999999));
        CoreContracts.Snapshot original=snap(readings);
        String first=NextReplayCodec.encodeSnapshot(original);
        CoreContracts.Snapshot decoded=NextReplayCodec.decodeSnapshot(first);
        yes(first.startsWith(NextReplayCodec.ENVELOPE),"versioned envelope");
        yes(first.equals(NextReplayCodec.encodeSnapshot(decoded)),"exact byte roundtrip");
        yes(ChemCoreNext.snapshotHash(original).equals(ChemCoreNext.snapshotHash(decoded)),
            "scientific snapshot hash after replay");
        yes(decoded.readings.size()==2,"all physical channels preserved");
        yes(decoded.mode==CoreContracts.Mode.FIELD_SESSION,"session mode preserved");
        yes(decoded.monotonicMillis==120010L,"monotonic timestamp preserved");
        yes(Double.doubleToRawLongBits(decoded.temperatureC)==
            Double.doubleToRawLongBits(original.temperatureC),"full precision Celsius");
        yes(Double.doubleToRawLongBits(decoded.relativeHumidityPercent)==
            Double.doubleToRawLongBits(original.relativeHumidityPercent),"full precision RH");
        yes(decoded.readings.get(0).sensorUid.equals("mq2-A"),"sensor uid preserved");
        yes(decoded.readings.get(0).rawAdc==1234,"raw ADC preserved");
        yes(Double.doubleToRawLongBits(decoded.readings.get(0).rsKohm)==
            Double.doubleToRawLongBits(original.readings.get(0).rsKohm),"full Rs precision");
        yes(decoded.readings.get(0).r0Definition==CoreContracts.CalibrationReference.SOURCE_R0,
            "calibration reference orientation preserved");
        yes(decoded.stableManifestHash.equals(original.stableManifestHash),
            "stable version manifest preserved");
        yes(decoded.readings.get(1).heaterPhase.equals("HEATER_LOW"),
            "heater phase preserved");
        yes(decoded.mqRelayOn,"recorded relay state preserved");

        CoreContracts.Reading noSample=new CoreContracts.Reading("mq4-x",null,null,null,null,
            null,CoreContracts.CalibrationReference.NOT_AVAILABLE,"UNKNOWN",
            CoreContracts.SampleQuality.NO_SAMPLE);
        CoreContracts.Snapshot missing=snap(Collections.singletonList(noSample));
        CoreContracts.Snapshot missingReplay=NextReplayCodec.decodeSnapshot(
            NextReplayCodec.encodeSnapshot(missing));
        yes(missingReplay.readings.get(0).rawAdc==null,"absent ADC preserved as absent");
        yes(missingReplay.readings.get(0).rsKohm==null,"missing Rs is not zero");
        yes(missingReplay.readings.get(0).quality==CoreContracts.SampleQuality.NO_SAMPLE,
            "missing reading has explicit quality");
        yes(NextReplayCodec.encodeSnapshot(snap(Collections.emptyList()))
            .equals(NextReplayCodec.encodeSnapshot(
                NextReplayCodec.decodeSnapshot(NextReplayCodec.encodeSnapshot(
                    snap(Collections.emptyList()))))),"zero-readings valid archive");

        CoreContracts.Result result=new CoreContracts.Result(ChemCoreNext.VERSION,original,
            ChemCoreNext.snapshotHash(original),CoreContracts.DecisionStatus.RESEARCH_CANDIDATES,
            Arrays.asList(new CoreContracts.EvidenceNode("source-1","curve-expert","mq2-A",
                501L,CoreContracts.EvidencePolarity.POSITIVE,"source formula research only"),
                new CoreContracts.EvidenceNode("source-1","curve-v2","mq7-B",
                null,CoreContracts.EvidencePolarity.NOT_EVALUABLE,"no matched model")),
            Collections.singletonList(new CoreContracts.Candidate(501L,"research candidate; not known",
                Arrays.asList("source-1"))),Arrays.asList("RESEARCH_ONLY"));
        String encodedResult=NextReplayCodec.encodeResult(result);
        CoreContracts.Result decodedResult=NextReplayCodec.decodeResult(encodedResult,original);
        yes(NextReplayCodec.encodeResult(decodedResult).equals(encodedResult),
            "exact research decision roundtrip");
        yes(decodedResult.rankedEligibleCandidates.get(0).canonicalChemicalUid==501L,
            "nonlegacy canonical UID preserved");
        yes(decodedResult.evidence.size()==2,"two evidence records saved");
        yes(decodedResult.evidence.get(1).polarity==CoreContracts.EvidencePolarity.NOT_EVALUABLE,
            "not evaluable not translated to negative");
        yes(decodedResult.evidence.get(0).sourceRootId.equals(decodedResult.evidence.get(1).sourceRootId),
            "same source root not turned into independent votes");
        yes(!decodedResult.physicallyValidatedG3 && !decodedResult.reportablePpm,
            "scientific replay cannot fabricate G3 or quantitative certification");
        yes(decodedResult.problems.contains("RESEARCH_ONLY"),"reasons preserved");

        rejected(()->NextReplayCodec.decodeSnapshot(first.replace("HCNEXT-REPLAY/1:",
            "HCNEXT-REPLAY/2:")),"unknown envelope version fail closed");
        rejected(()->NextReplayCodec.decodeResult(first,original),"snapshot cannot be decoded as result");
        rejected(()->NextReplayCodec.decodeSnapshot(encodedResult),"result cannot be decoded as snapshot");
        rejected(()->NextReplayCodec.decodeSnapshot(first.substring(0,first.length()-1)+"*"),
            "bad base64 rejected");
        rejected(()->NextReplayCodec.decodeSnapshot(first.replaceFirst("[0-9a-f]{64}","0000000000000000000000000000000000000000000000000000000000000000")),
            "SHA integrity mismatch detected");
        rejected(()->NextReplayCodec.decodeResult(encodedResult,
            snap(Collections.singletonList(valid("mq2-A",7.0)))),
            "result cannot be attached to changed raw MQ snapshot");
        rejected(()->NextReplayCodec.decodeResult(encodedResult,
            new CoreContracts.Snapshot("measurement-42","hazrob-tank",
                CoreContracts.Mode.FIELD_SESSION,120010L,22.123456789,48.998877,
                true,readings,"other-stable-manifest")),"result cannot change Stable version");
        rejected(()->NextReplayCodec.decodeSnapshot("random bad replay"),
            "random string cannot become input evidence");
        rejected(()->NextReplayCodec.decodeSnapshot(first+first),
            "replay concatenation rejected");

        List<CoreContracts.Reading> nine=new ArrayList<>();
        for(int i=0;i<9;i++)nine.add(valid("mq-"+i,8.0+i/10.0));
        String nineEncoded=NextReplayCodec.encodeSnapshot(snap(nine));
        yes(NextReplayCodec.decodeSnapshot(nineEncoded).readings.size()==9,
            "future sensors >8 preserve all physical readings");

        System.out.println("CHEMCORE_NEXT_P01_REPLAY_CODEC_PASS assertions="+count
            +" schema="+CoreContracts.SCHEMA+" envelope="+NextReplayCodec.ENVELOPE
            +" g3=false ppm=false");
    }
}
