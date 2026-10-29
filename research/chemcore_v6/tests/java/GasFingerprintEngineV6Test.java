package com.hazrob.tank.chemcore.v6;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Synthetic/native-prototype algebraic tests: not physical sensor validation. */
public final class GasFingerprintEngineV6Test {
    private static int checks=0;
    private static void ok(boolean cond,String reason){
        checks++;
        if(!cond)throw new AssertionError("assertion "+checks+": "+reason);
    }
    public static void main(String[] args){
        ok(GasFingerprintCatalogV6.all().size()==24,"24 gas-only numeric reference prototypes");
        ok(GasFingerprintCatalogV6.inSpace(FingerprintEngineV6.ADC).size()==4,"four ADC classes");
        ok(GasFingerprintCatalogV6.inSpace(FingerprintEngineV6.OPERATIONAL).size()==2,"two operational classes");
        ok(GasFingerprintCatalogV6.inSpace(FingerprintEngineV6.TEMPORAL).size()==18,"18 UCI temporal profiles");
        ok(FingerprintEngineV6.MQ_ORDER.size()==8,"eight original Tank MQ model types");
        ok(!FingerprintEngineV6.MQ_ORDER.contains("MUSHROOM"),"food labels never included");
        int count=0;
        for(GasFingerprintCatalogV6.Signature source:GasFingerprintCatalogV6.all()){
            FingerprintEngineV6.Input nativeInput=new FingerprintEngineV6.Input(
                source.space,source.vector(),"SOURCE_NATIVE_FEATURES",source.sourceRoot);
            FingerprintEngineV6.Result ranked=FingerprintEngineV6.match(nativeInput);
            int expected=GasFingerprintCatalogV6.inSpace(source.space).size();
            ok(ranked.hypotheses.size()==expected,"only same-feature-space candidates returned");
            ok(ranked.sourceTemplatesInSpace==expected,"space template inventory retained");
            ok(ranked.numericallyUnusableTemplates==0,"native profiles are mathematically valid");
            ok(!ranked.calibratedIdentity&&!ranked.reportablePpm&&!ranked.sourceDeviceEquivalence,
               "reference self-test not a Tank ppm or identity validation");
            FingerprintEngineV6.Hypothesis same=null;
            for(FingerprintEngineV6.Hypothesis h:ranked.hypotheses){
                ok(h.featureSpace.equals(source.space),"no ADC/operational/temporal mixing");
                ok(Double.isFinite(h.distance)&&h.distance>=0,"finite ranked distance");
                ok(Double.isFinite(h.similarityIndex)&&h.similarityIndex>0
                   &&h.similarityIndex<=100,"bounded descriptive score");
                if(h.fingerprintId.equals(source.id))same=h;
            }
            ok(same!=null,"reference not silently excluded");
            ok(same.distance<1e-9,"source template self distance zero");
            ok(Math.abs(same.similarityIndex-100)<1e-7,"own prototype shape 100 index");
            if(source.space.equals(FingerprintEngineV6.ADC))
                ok(same.overlapDimensions==6,"missing MQ4/MQ9 not fabricated");
            if(source.space.equals(FingerprintEngineV6.OPERATIONAL))
                ok(same.overlapDimensions==6,"operational source has six channels");
            if(source.space.equals(FingerprintEngineV6.TEMPORAL))
                ok(same.overlapDimensions==3,"one-sensor three temporal features");
            count++;
        }
        Map<String,Double> adc=new LinkedHashMap<>();
        adc.put("MQ2",620d);adc.put("MQ3",430d);
        adc.put("MQ5",320d);adc.put("MQ7",250d);
        adc.put("MQ8",380d);adc.put("MQ135",405d);
        var snapshot=FingerprintEngineV6.match(
            FingerprintEngineV6.fromAdc(adc,"SOURCE_LIKE_ADC_0_TO_1023_FIXTURE"));
        ok(snapshot.hypotheses.size()==4,"ADC source-like snapshot returns four classes");
        ok(snapshot.hypotheses.get(0).overlapDimensions==6,"six actual ADC MQ readings used");
        ok(snapshot.hypotheses.get(0).notes.contains(
            "RAW_SOURCE_TRANSFORMATION_NOT_CONFIRMED_EQUIVALENT"),
            "ADC source transfer limitation remains visible");
        adc.clear();
        adc.put("MQ2",150.0);
        var sparse=FingerprintEngineV6.match(
            FingerprintEngineV6.fromAdc(adc,"SPARSE_FIXTURE"));
        ok(sparse.hypotheses.size()==4,"single finite channel not threshold-excluded");
        ok(sparse.hypotheses.get(0).overlapDimensions==1,"sparse overlap explicitly reported");
        ok(sparse.hypotheses.get(0).notes.contains(
            "ONE_DIMENSION_ONLY_UNINFORMATIVE_SHAPE"),
            "single-channel cross-sensor class shape is uninformative");
        Map<String,Double> robo=new LinkedHashMap<>();
        for(String sensor:FingerprintEngineV6.OPERATIONAL_ORDER)robo.put(sensor,100.0);
        var operational=FingerprintEngineV6.match(
            FingerprintEngineV6.fromOperational(robo,"SOURCE_LIKE_RAW_FIXTURE"));
        ok(operational.hypotheses.size()==2,"operational source classes remain separate");
        ok(operational.hypotheses.stream().anyMatch(h->
            h.notes.contains("OPERATIONAL_CLASS_NOT_A_MOLECULAR_IDENTITY")),
            "source Hazard_1 cannot become a pure gas identity");
        var uci=FingerprintEngineV6.match(
            FingerprintEngineV6.fromMq135Temporal(0.95,-0.05,-0.0001,
                "SYNTHETIC_900S_FEATURES"));
        ok(uci.hypotheses.size()==18,"all source UCI ppm-level profiles available");
        ok(uci.hypotheses.get(0).notes.contains(
            "UCI_50_TO_200_PPB_SOURCE_CONTEXT_NOT_DEVICE_PPM"),
            "original ppb exposure not claimed as Tank reference ppm");
        var malformed=FingerprintEngineV6.match(new FingerprintEngineV6.Input(
            FingerprintEngineV6.ADC,new double[]{Double.NaN,Double.POSITIVE_INFINITY},
            "SOURCE_NATIVE_FEATURES","BAD_NUMERIC"));
        ok(malformed.hypotheses.isEmpty(),"no fabricated fit with all nonfinite coordinates");
        ok(malformed.numericallyUnusableTemplates==4,
           "all mathematically impossible prototype comparisons counted");
        ok(FingerprintEngineV6.match(null).hypotheses.isEmpty(),"null input safe");
        ok(FingerprintEngineV6.match(new FingerprintEngineV6.Input(
            "UNKNOWN_FEATURE_SPACE",new double[]{1.0},"SOURCE_NATIVE_FEATURES",""))
           .hypotheses.isEmpty(),"no invented cross-space model");
        var returned=GasFingerprintCatalogV6.all().get(0).vector();
        double sourceValue=returned[0];
        returned[0]=123456789;
        ok(GasFingerprintCatalogV6.all().get(0).vector()[0]==sourceValue,
           "source fingerprint coefficient arrays immutable to caller");
        System.out.println("PASS GasFingerprintEngineV6: "+checks+
            " assertions; "+count+" source signatures self-verified in native feature spaces");
    }
}
