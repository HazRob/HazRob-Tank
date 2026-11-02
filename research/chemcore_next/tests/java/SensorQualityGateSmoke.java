package com.hazrob.tank.chemcore.next;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class SensorQualityGateSmoke {
    private static int n;
    private static void ok(boolean v,String why) {
        ++n;
        if(!v)throw new AssertionError("P02.3 check "+n+" "+why);
    }
    private static void rejects(Runnable task,String why) {
        boolean thrown=false;
        try{task.run();}catch(IllegalArgumentException expected){thrown=true;}
        ok(thrown,why);
    }
    private static final long NOW=120000L;
    private static final SensorQualityGate.Limits LIMITS=
        new SensorQualityGate.Limits(2000,60000,0.20,0.30,3);
    private static SensorQualityGate.Sample sample(long time,long cycle,int raw,double rs) {
        return new SensorQualityGate.Sample("tank:MQ2",time,70000,cycle,raw,rs,
            SensorQualityGate.Transport.ACKED,Boolean.TRUE,Boolean.TRUE,
            "HEATER_STEADY",25.0,50.0,true);
    }
    private static SensorQualityGate.Result check(List<SensorQualityGate.Sample> samples) {
        return SensorQualityGate.evaluate("tank:MQ2","HEATER_STEADY",NOW,samples,LIMITS);
    }
    private static SensorQualityGate.Sample custom(long time,long cycle,Integer raw,double rs,
        SensorQualityGate.Transport transport,Boolean ready,Boolean power,String phase,
        long poweredFor,Double temp,Double rh,boolean hasReference) {
        return new SensorQualityGate.Sample("tank:MQ2",time,poweredFor,cycle,raw,rs,
            transport,ready,power,phase,temp,rh,hasReference);
    }
    private static void flagged(SensorQualityGate.Result r,SensorQualityGate.Flag f,boolean hard) {
        ok(r.has(f),"expected diagnostic "+f);
        if(hard) {
            ok(r.status==SensorQualityGate.Status.REJECTED,"fail closed "+f);
            ok(!r.physicalSignalEligible,"bad "+f+" cannot be used as physical signal");
            ok(!r.sourceCurveEligible,"bad "+f+" cannot be used for source curve");
        }
    }
    public static void main(String[] args) {
        SensorQualityGate.Result good=check(Arrays.asList(
            sample(119700,201,1500,10.0),sample(119800,202,1499,10.1),
            sample(119900,203,1501,9.9)));
        ok(good.status==SensorQualityGate.Status.ACCEPTED,"clean healthy acquisition accepted");
        ok(good.physicalSignalEligible,"healthy raw physical resistance allowed");
        ok(good.environmentalModelEligible,"T/RH present");
        ok(good.sourceCurveEligible,"source curve eligible only with full acquisition provenance");
        ok(good.flags.isEmpty(),"no invented defects");
        ok(good.noiseCv!=null&&good.noiseCv<0.02,"low stationary sample CV measured");
        ok(good.relativeWindowDrift!=null&&good.relativeWindowDrift<0.02,"low window drift");
        ok(good.observedSamples==3,"sample provenance count");

        flagged(check(Collections.emptyList()),SensorQualityGate.Flag.MISSING_SAMPLES,true);
        flagged(check(Collections.singletonList(sample(116000,201,1500,10))),
            SensorQualityGate.Flag.STALE_SAMPLE,true);
        flagged(check(Collections.singletonList(sample(121000,201,1500,10))),
            SensorQualityGate.Flag.FUTURE_SAMPLE_TIMESTAMP,true);
        flagged(check(Arrays.asList(sample(119800,201,1499,10),
            sample(119900,201,1500,10))),SensorQualityGate.Flag.DUPLICATE_SCAN_CYCLE,true);
        flagged(check(Arrays.asList(sample(119800,203,1499,10),
            sample(119900,202,1500,10))),SensorQualityGate.Flag.DUPLICATE_SCAN_CYCLE,true);
        flagged(check(Collections.singletonList(sample(119800,0,1500,10))),
            SensorQualityGate.Flag.INVALID_SCAN_CYCLE,true);
        flagged(check(Collections.singletonList(custom(119800,201,1500,10,
            SensorQualityGate.Transport.NACK,true,true,"HEATER_STEADY",
            70000,25.0,50.0,true))),SensorQualityGate.Flag.I2C_NACK,true);
        flagged(check(Collections.singletonList(custom(119800,201,1500,10,
            SensorQualityGate.Transport.TIMEOUT,true,true,"HEATER_STEADY",
            70000,25.0,50.0,true))),SensorQualityGate.Flag.I2C_TIMEOUT,true);
        flagged(check(Collections.singletonList(custom(119800,201,1500,10,
            SensorQualityGate.Transport.BUS_RECOVERING,true,true,"HEATER_STEADY",
            70000,25.0,50.0,true))),SensorQualityGate.Flag.I2C_BUS_RECOVERING,true);
        flagged(check(Collections.singletonList(custom(119800,201,1500,10,
            SensorQualityGate.Transport.ACKED,false,true,"HEATER_STEADY",
            70000,25.0,50.0,true))),SensorQualityGate.Flag.ADS_CONVERSION_NOT_READY,true);
        flagged(check(Collections.singletonList(custom(119800,201,1500,10,
            SensorQualityGate.Transport.ACKED,true,false,"HEATER_STEADY",
            70000,25.0,50.0,true))),SensorQualityGate.Flag.HEATER_OFF,true);
        flagged(check(Collections.singletonList(custom(119800,201,1500,10,
            SensorQualityGate.Transport.ACKED,true,true,"HEATER_STEADY",
            59000,25.0,50.0,true))),SensorQualityGate.Flag.HEATER_WARMING,true);
        flagged(check(Collections.singletonList(custom(119800,201,1500,10,
            SensorQualityGate.Transport.ACKED,true,true,"HEATER_LOW",
            70000,25.0,50.0,true))),SensorQualityGate.Flag.HEATER_PROFILE_MISMATCH,true);
        flagged(check(Collections.singletonList(sample(119800,201,32767,10))),
            SensorQualityGate.Flag.INVALID_RESISTANCE,true);
        flagged(check(Collections.singletonList(sample(119800,201,500,Double.NaN))),
            SensorQualityGate.Flag.INVALID_RESISTANCE,true);
        flagged(check(Collections.singletonList(new SensorQualityGate.Sample("tank:MQ3",
            119800,70000,201,1500,10.0,SensorQualityGate.Transport.ACKED,
            true,true,"HEATER_STEADY",25.0,50.0,true))),
            SensorQualityGate.Flag.SENSOR_UID_MISMATCH,true);
        flagged(check(Collections.singletonList(sample(-1,201,1500,10))),
            SensorQualityGate.Flag.MISSING_SAMPLE_TIMESTAMP,true);
        flagged(check(Arrays.asList(sample(119900,201,1500,10),
            sample(119800,202,1501,10))),SensorQualityGate.Flag.STALE_SAMPLE,true);

        SensorQualityGate.Result missingReady=check(Collections.singletonList(
            custom(119800,201,1500,10,SensorQualityGate.Transport.ACKED,
                null,true,"HEATER_STEADY",70000,25.0,50.0,true)));
        ok(missingReady.status==SensorQualityGate.Status.LIMITED,"no OS-ready telemetry is uncertainty");
        ok(missingReady.physicalSignalEligible,"missing OS evidence still records research raw");
        ok(!missingReady.sourceCurveEligible,"missing OS evidence blocks source model promotion");
        ok(missingReady.has(SensorQualityGate.Flag.ADS_READY_NOT_REPORTED),"exposes firmware gap");

        SensorQualityGate.Result missingI2c=check(Collections.singletonList(
            custom(119800,201,1500,10,SensorQualityGate.Transport.NOT_REPORTED,
                true,true,"HEATER_STEADY",70000,25.0,50.0,true)));
        ok(missingI2c.has(SensorQualityGate.Flag.I2C_STATUS_UNKNOWN),"I2C unknown documented");
        ok(missingI2c.status==SensorQualityGate.Status.LIMITED,"I2C missing not clean PASS");
        ok(!missingI2c.sourceCurveEligible,"missing I2C evidence prevents promoted source analysis");

        SensorQualityGate.Result missingEnv=check(Collections.singletonList(
            custom(119800,201,1500,10,SensorQualityGate.Transport.ACKED,
                true,true,"HEATER_STEADY",70000,null,null,true)));
        ok(missingEnv.has(SensorQualityGate.Flag.ENVIRONMENT_UNAVAILABLE),
            "missing BME channel documented");
        ok(missingEnv.physicalSignalEligible,"missing BME does not discard raw MQ resistance");
        ok(!missingEnv.environmentalModelEligible,"no 3D T/RH correction without BME");
        ok(missingEnv.sourceCurveEligible,
            "environment-free source curve can still use healthy raw if model permits");
        SensorQualityGate.Result invalidRh=check(Collections.singletonList(
            custom(119800,201,1500,10,SensorQualityGate.Transport.ACKED,
                true,true,"HEATER_STEADY",70000,25.0,120.0,true)));
        ok(invalidRh.has(SensorQualityGate.Flag.ENVIRONMENT_OUTSIDE_VALID_RANGE),
            "invalid RH reported");
        ok(!invalidRh.environmentalModelEligible,"120 RH cannot be used as humidity");

        SensorQualityGate.Result missingR0=check(Collections.singletonList(
            custom(119800,201,1500,10,SensorQualityGate.Transport.ACKED,
                true,true,"HEATER_STEADY",70000,25.0,50.0,false)));
        ok(missingR0.has(SensorQualityGate.Flag.REFERENCE_UNAVAILABLE),
            "missing clean air R0 provenance");
        ok(missingR0.physicalSignalEligible,"raw Rs preserved even without R0");
        ok(!missingR0.sourceCurveEligible,"source curve requires reference");

        SensorQualityGate.Result flat=check(Arrays.asList(
            sample(119700,501,1500,10),sample(119800,502,1500,10),
            sample(119900,503,1500,10)));
        ok(flat.has(SensorQualityGate.Flag.IDENTICAL_RAW_CODES_DIAGNOSTIC),
            "identical code prompts diagnostics only");
        ok(flat.physicalSignalEligible,"constant input is not a stale conversion proof");
        ok(flat.status==SensorQualityGate.Status.LIMITED,"flat codes research uncertainty");
        ok(!flat.has(SensorQualityGate.Flag.STALE_SAMPLE),
            "healthy increasing scan IDs distinguish repeated value from stale scan");

        SensorQualityGate.Result jump=check(Arrays.asList(
            sample(119700,301,1500,10),sample(119800,302,1800,10.5),
            sample(119900,303,3500,18)));
        ok(jump.has(SensorQualityGate.Flag.HIGH_NOISE),"large window volatility flagged");
        ok(jump.has(SensorQualityGate.Flag.TEMPORAL_DRIFT),"large window slope flagged");
        ok(jump.physicalSignalEligible,
            "real chemical exposure movement not automatically fabricated as ADC failure");
        ok(jump.status==SensorQualityGate.Status.LIMITED,
            "unstable window not silently model-qualified");
        ok(!jump.sourceCurveEligible,
            "unstable source model must not enter specialist adjudication unexamined");
        rejects(()->new SensorQualityGate.Limits(0,60000,.2,.3,3),"bad stale threshold");
        rejects(()->new SensorQualityGate.Limits(2000,60000,Double.NaN,.3,3),
            "bad CV limit");
        rejects(()->new SensorQualityGate.Sample("",119900,70000,10,1500,10,
            SensorQualityGate.Transport.ACKED,true,true,"STEADY",25.0,50.0,true),
            "empty sensor identity");
        rejects(()->SensorQualityGate.evaluate("tank:MQ2","",NOW,null,LIMITS),
            "null samples not allowed");

        System.out.println("CHEMCORE_NEXT_P02_SENSOR_QUALITY_PASS assertions="+n
            +" source_qualified=false device_G3=false firmware_unchanged=true");
    }
}
