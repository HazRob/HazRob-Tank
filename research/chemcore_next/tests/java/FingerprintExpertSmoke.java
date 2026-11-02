package com.hazrob.tank.chemcore.next;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Source-only fingerprint identity + physical-subset and source-space G1 tests. */
public final class FingerprintExpertSmoke {
    private static int n=0;
    static void ok(boolean yes,String why) {
        n++;
        if(!yes) throw new AssertionError("P04.2 #"+n+": "+why);
    }
    static void equal(double a,double b,String why) {
        ok(Double.isFinite(a)&&Math.abs(a-b)<1e-11,why);
    }
    static void rejects(Runnable task,String why) {
        boolean rejected=false;
        try{task.run();}catch(IllegalArgumentException wanted){rejected=true;}
        ok(rejected,why);
    }
    static List<String> names(int count) {
        List<String> names=new ArrayList<>();
        for(int i=0;i<count;i++)names.add("sensor-uid-"+(i+1));
        return names;
    }
    static List<Double> numbers(int count,double add) {
        List<Double> numbers=new ArrayList<>();
        for(int i=0;i<count;i++)numbers.add((double)i+add);
        return numbers;
    }
    static FingerprintExpert.Signature signature(int dimension,int min) {
        return new FingerprintExpert.Signature("test-signature","MQ_ARRAY_ORIGINAL_ROOT",
            "EIGHT_SENSOR_LOG_RESPONSE","MQ_ARRAY_8",136L,names(dimension),
            numbers(dimension,0),numbers(dimension,1.0),min);
    }
    static FingerprintExpert.Observation snapshot(Map<String,Double> obs,
                                                 String space,String model,boolean valid) {
        return new FingerprintExpert.Observation(space,model,"sensor-window-physical-1",obs,valid);
    }
    static Map<String,Double> values(int count,double add) {
        Map<String,Double> result=new LinkedHashMap<>();
        for(int i=0;i<count;i++)result.put("sensor-uid-"+(i+1),(double)i+add);
        return result;
    }
    public static void main(String[] ignored) {
        FingerprintExpert.Qualification verified=new FingerprintExpert.Qualification(true,true);
        FingerprintExpert.Signature single=signature(1,1);
        FingerprintExpert.Output one=FingerprintExpert.compare(single,snapshot(
            values(1,0),"EIGHT_SENSOR_LOG_RESPONSE","MQ_ARRAY_8",true),verified);
        ok(one.status==FingerprintExpert.Status.RESEARCH_RESIDUAL_ONLY,
           "one physical channel permitted for explicitly 1-D prototype");
        equal(one.researchNormalizedRmsDistance,0.0,"single channel exact center");
        ok(one.matchedCoordinates==1&&!one.partialSubset,"single full signature");

        FingerprintExpert.Signature pair=signature(2,2);
        FingerprintExpert.Output two=FingerprintExpert.compare(pair,snapshot(
            values(2,1),"EIGHT_SENSOR_LOG_RESPONSE","MQ_ARRAY_8",true),verified);
        ok(two.status==FingerprintExpert.Status.RESEARCH_RESIDUAL_ONLY,
           "two-channel research fingerprint");
        double expected=Math.sqrt((1.0+0.25)/2.0);
        equal(two.researchNormalizedRmsDistance,expected,"weighted two sensor residual");

        FingerprintExpert.Signature eight=signature(8,3);
        FingerprintExpert.Output full=FingerprintExpert.compare(eight,snapshot(
            values(8,1),"EIGHT_SENSOR_LOG_RESPONSE","MQ_ARRAY_8",true),verified);
        ok(full.matchedCoordinates==8&&!full.partialSubset,"full eight-source profile");
        ok(full.researchNormalizedRmsDistance>0,"research residual not pseudo ppm");

        Map<String,Double> partial=values(4,0);
        FingerprintExpert.Output four=FingerprintExpert.compare(eight,snapshot(partial,
            "EIGHT_SENSOR_LOG_RESPONSE","MQ_ARRAY_8",true),verified);
        ok(four.status==FingerprintExpert.Status.RESEARCH_RESIDUAL_ONLY,
           "minimum three coordinates allow an explicitly partial profile");
        ok(four.partialSubset&&four.matchedCoordinates==4&&four.sourceCoordinates==8,
           "partial subset always disclosed");
        equal(four.researchNormalizedRmsDistance,0.0,"four exact source coordinates");
        FingerprintExpert.Output deniedPair=FingerprintExpert.compare(eight,snapshot(
            values(2,0),"EIGHT_SENSOR_LOG_RESPONSE","MQ_ARRAY_8",true),verified);
        ok(deniedPair.status==FingerprintExpert.Status.INSUFFICIENT_PHYSICAL_SUBSET,
           "two channels cannot stand in for minimum three");
        ok(deniedPair.researchNormalizedRmsDistance==null,"no fake numeric distance");
        FingerprintExpert.Output deniedSingle=FingerprintExpert.compare(eight,snapshot(
            values(1,0),"EIGHT_SENSOR_LOG_RESPONSE","MQ_ARRAY_8",true),verified);
        ok(deniedSingle.status==FingerprintExpert.Status.INSUFFICIENT_PHYSICAL_SUBSET,
           "one channel is not an 8MQ signature");

        FingerprintExpert.Output wrongSpace=FingerprintExpert.compare(eight,snapshot(
            values(8,0),"OPERATIONAL_LOG1PLUS_CLR","MQ_ARRAY_8",true),verified);
        ok(wrongSpace.status==FingerprintExpert.Status.SPACE_MISMATCH,
           "6D operational and 8D ADC features never mixed");
        FingerprintExpert.Output wrongSensor=FingerprintExpert.compare(eight,snapshot(
            values(8,0),"EIGHT_SENSOR_LOG_RESPONSE","UCI_MQ135",true),verified);
        ok(wrongSensor.status==FingerprintExpert.Status.SENSOR_MODEL_MISMATCH,
           "external UCI MQ135 is not 8-channel Tank");
        FingerprintExpert.Output unqualified=FingerprintExpert.compare(eight,snapshot(
            values(8,0),"EIGHT_SENSOR_LOG_RESPONSE","MQ_ARRAY_8",true),
            FingerprintExpert.Qualification.unverified());
        ok(unqualified.status==FingerprintExpert.Status.SOURCE_TRANSFORM_UNQUALIFIED,
           "data transformation source not verified");
        FingerprintExpert.Output missingDevice=FingerprintExpert.compare(eight,snapshot(
            values(8,0),"EIGHT_SENSOR_LOG_RESPONSE","MQ_ARRAY_8",true),
            new FingerprintExpert.Qualification(true,false));
        ok(missingDevice.status==FingerprintExpert.Status.DEVICE_INSTANCE_NOT_MATCHED,
           "same source model does not physically certify Tank source");
        FingerprintExpert.Output stale=FingerprintExpert.compare(eight,snapshot(
            values(8,0),"EIGHT_SENSOR_LOG_RESPONSE","MQ_ARRAY_8",false),verified);
        ok(stale.status==FingerprintExpert.Status.INVALID_OBSERVATION,
           "stale ADS sample invalidates source fingerprint evidence");

        List<Double> missingScales=new ArrayList<>(numbers(8,1));
        missingScales.set(0,null);missingScales.set(3,0.0);
        FingerprintExpert.Signature missing=new FingerprintExpert.Signature(
            "MQDS_INVALID_SCALE_SOURCE","MQDS_6400_SOURCE_ADC",
            "EIGHT_SENSOR_LOG_RESPONSE","MQ_ARRAY_8",null,names(8),
            numbers(8,0),missingScales,3);
        ok(FingerprintExpert.compare(missing,snapshot(
            values(8,0),"EIGHT_SENSOR_LOG_RESPONSE","MQ_ARRAY_8",true),verified)
           .status==FingerprintExpert.Status.INVALID_SOURCE_SCALES,
           "missing source uncertainty must not be replaced by synthetic positive weights");
        List<Double> negativeScale=new ArrayList<>(numbers(8,1));
        negativeScale.set(7,-2.0);
        FingerprintExpert.Signature negative=new FingerprintExpert.Signature(
            "NEGATIVE_MQDS_SOURCE","same-original-root",
            "EIGHT_SENSOR_LOG_RESPONSE","MQ_ARRAY_8",null,names(8),
            numbers(8,0),negativeScale,1);
        ok(FingerprintExpert.compare(negative,snapshot(
            values(1,0),"EIGHT_SENSOR_LOG_RESPONSE","MQ_ARRAY_8",true),verified)
           .status==FingerprintExpert.Status.INVALID_SOURCE_SCALES,
           "negative scale remains invalid even when excluded feature is not observed");
        Map<String,Double> extra=values(8,0);
        extra.put("not-in-source",42.0);
        ok(FingerprintExpert.compare(eight,snapshot(
            extra,"EIGHT_SENSOR_LOG_RESPONSE","MQ_ARRAY_8",true),verified)
           .status==FingerprintExpert.Status.UNRECOGNIZED_FEATURE,
           "unknown input feature cannot silently affect G1 research model");

        FingerprintExpert.Signature temporal=new FingerprintExpert.Signature(
            "UCI-TEMPORAL","UCI1081_MQ135_TEMPORAL",
            "UCI1081_MQ135_BASELINE_NORMALIZED_TEMPORAL","UCI_MQ135",null,
            Arrays.asList("window0","window1","window2"),
            Arrays.asList(0.4,0.3,0.2),Arrays.asList(0.1,0.1,0.2),3);
        Map<String,Double> temporalObserved=new HashMap<>();
        temporalObserved.put("window0",0.5);temporalObserved.put("window1",0.3);
        temporalObserved.put("window2",0.0);
        FingerprintExpert.Output temporalResult=FingerprintExpert.compare(temporal,
            snapshot(temporalObserved,"UCI1081_MQ135_BASELINE_NORMALIZED_TEMPORAL",
                "UCI_MQ135",true),verified);
        ok(temporalResult.status==FingerprintExpert.Status.RESEARCH_RESIDUAL_ONLY,
           "3 temporal windows properly share one sensor and one source feature definition");
        equal(temporalResult.researchNormalizedRmsDistance,
              Math.sqrt((1.0+0.0+1.0)/3.0),"temporal scaled residual");

        ok(!full.gasIdentityConfirmed&&!full.ppmAuthorized&&!full.g3PhysicallyQualified,
           "distance never asserts gas identity, concentration or G3");
        ok(!temporalResult.gasIdentityConfirmed,
           "UCI external label cannot certify Tank gas candidate");
        ok(eight.sourceRoot.equals(full.sourceRoot),"source root always retained");
        ok(full.canonicalChemicalUid==136L,"canonical UID is metadata only");
        rejects(()->new FingerprintExpert.Signature(
            "dup","root","space","MQ2",null,
            Arrays.asList("MQ2","MQ2"),Arrays.asList(0.,1.),
            Arrays.asList(1.,1.),1),"duplicate physical feature identity blocked");
        rejects(()->new FingerprintExpert.Signature(
            "bad","root","space","MQ2",null,
            Arrays.asList("MQ2"),Arrays.asList(0.,1.),
            Arrays.asList(1.,1.),1),"source dimensions must agree");
        rejects(()->new FingerprintExpert.Signature(
            "bad","root","space","MQ2",null,
            Arrays.asList("MQ2"),Arrays.asList(0.),
            Arrays.asList(1.),2),"cannot require more than source feature count");
        rejects(()->new FingerprintExpert.Observation(
            "space","MQ2","UID",Map.of("invalid",Double.NaN),true),
            "nonfinite feature values forbidden");
        rejects(()->new FingerprintExpert.Signature(
            "bad","root","space","MQ2",null,
            Arrays.asList("MQ2"),Arrays.asList(Double.NaN),
            Arrays.asList(1.),1),"NaN reference mean forbidden");
        Map<String,Double> mutable=values(1,0);
        FingerprintExpert.Observation safe=snapshot(mutable,
            "EIGHT_SENSOR_LOG_RESPONSE","MQ_ARRAY_8",true);
        mutable.put("sensor-uid-1",99.0);
        equal(safe.availableCoordinates.get("sensor-uid-1"),0.,
              "immutable copy protects measurement replay after map mutation");
        boolean immutable=false;
        try{safe.availableCoordinates.put("x",1.0);}
        catch(UnsupportedOperationException expectedImmutable){immutable=true;}
        ok(immutable,"observed feature map is read-only");
        System.out.println("CHEMCORE_NEXT_P04_2_FINGERPRINT_PASS assertions="+n+
            " feature_spaces_guarded=true subsets_1_2_8=true G3=false");
    }
}
