package com.hazrob.tank.chemcore.next;

/** P02.1 nominal circuit mathematics and invalid-input gates (not a field calibration). */
public final class SignalPhysicsSmoke {
    private static int n=0;
    private static void check(boolean condition,String why) {
        n++;
        if(!condition)throw new AssertionError("SignalPhysics P02.1 "+n+" failed: "+why);
    }
    private static void rejects(Runnable task,String why) {
        boolean fail=false;
        try{task.run();}catch(IllegalArgumentException expected){fail=true;}
        check(fail,why);
    }
    private static SignalPhysics.Circuit physicalVerified(){
        // This is test-fixture hypothetical verification, NOT the physical Tank status.
        return new SignalPhysics.Circuit("test:MQ2",5.0,10.0,6.144,5.0,32768,
                                         1.0,true,true);
    }
    private static void status(SignalPhysics.Observation obs,SignalPhysics.Status expected,
                               String why){
        check(obs.status==expected,why+" got "+obs.status);
        if(expected!=SignalPhysics.Status.VALID){
            check(obs.rsKohm==null,why+" invalid sample cannot carry usable Rs");
            check(!obs.usableForAnalysis(),why+" invalid reading cannot enter chemistry");
        }
    }
    public static void main(String[] args){
        SignalPhysics.Circuit source=SignalPhysics.tankV6DeclaredNominal("tank:MQ2");
        check(!source.physicalWiringVerified,"actual Tank hardware proof not fabricated");
        check(!source.adcVoltageVerified,"firmware ADS nominal FSR not independent verification");
        check(source.adcCountsForFullScale==32768,"ADS positive signed 16-bit scaling");
        check(Math.abs(source.adcFullScaleV-6.144)<1e-12,"pinned firmware PGA");
        check(Math.abs(source.loadResistorKohm-10.0)<1e-12,"pinned firmware nominal load");

        status(SignalPhysics.fromAdcCode(source,10000),
            SignalPhysics.Status.ADC_NOT_CALIBRATED,
            "nominal declared voltage cannot be treated as field physical");
        SignalPhysics.Circuit verified=physicalVerified();
        status(SignalPhysics.fromAdcCode(verified,null),
            SignalPhysics.Status.NO_SAMPLE,"missing raw ADS");
        status(SignalPhysics.fromAdcCode(verified,-32768),
            SignalPhysics.Status.ADC_INVALID,"signed ADS acquisition failure");
        status(SignalPhysics.fromAdcCode(verified,65535),
            SignalPhysics.Status.ADC_INVALID,"overflow ADS sample");
        status(SignalPhysics.fromAdcCode(verified,32768),
            SignalPhysics.Status.ADC_SATURATED,"ADC positive full scale");
        SignalPhysics.Observation valid=SignalPhysics.fromKnownAdcVoltage(verified,16000,2.0);
        status(valid,SignalPhysics.Status.VALID,"actual defined Vout");
        check(Math.abs(valid.rsKohm-15.0)<1e-10,
            "Rs=RL*(Vcc/Vout-1) gives 15kohm at Vout2, Vcc5, RL10");
        check(Math.abs(valid.sensorOutputVoltageV-2.0)<1e-12,"Vout was not silently converted");
        check(valid.usableForAnalysis(),"verified physical input is usable as Rs only");

        SignalPhysics.Observation exactly2=SignalPhysics.fromAdcCode(verified,10000);
        status(exactly2,SignalPhysics.Status.VALID,"ADC code conversion");
        check(Math.abs(exactly2.measuredAdcVoltageV-1.875)<1e-12,
            "10000 ADS counts*6.144/32768 ->1.875V");
        check(Math.abs(exactly2.rsKohm-16.666666666666668)<1e-10,
            "known ADS voltage Rs conversion");

        status(SignalPhysics.fromKnownAdcVoltage(verified,123,null),
            SignalPhysics.Status.NO_SAMPLE,"no measured voltage");
        status(SignalPhysics.fromKnownAdcVoltage(verified,null,Double.NaN),
            SignalPhysics.Status.VOLTAGE_NOT_FINITE,"NaN ADC input");
        status(SignalPhysics.fromKnownAdcVoltage(verified,null,Double.POSITIVE_INFINITY),
            SignalPhysics.Status.VOLTAGE_NOT_FINITE,"infinity ADC input");
        status(SignalPhysics.fromKnownAdcVoltage(verified,1,-0.2),
            SignalPhysics.Status.VOLTAGE_ABOVE_ADC_SUPPLY,"negative Vadc");
        status(SignalPhysics.fromKnownAdcVoltage(verified,1,5.0),
            SignalPhysics.Status.VOLTAGE_ABOVE_ADC_SUPPLY,"ADS VDD not actual PGA FSR");
        status(SignalPhysics.fromKnownAdcVoltage(verified,1,0.0),
            SignalPhysics.Status.VOLTAGE_OUTSIDE_DIVIDER,"Vout=0 cannot become infinite Rs");
        SignalPhysics.Circuit differentGain=new SignalPhysics.Circuit(
            "other:MQ2",5.0,10.0,6.144,5.0,32768,2.0,true,true);
        status(SignalPhysics.fromKnownAdcVoltage(differentGain,100,3.0),
            SignalPhysics.Status.VOLTAGE_OUTSIDE_DIVIDER,
            "analogue gain maps above MQ sensor Vcc");

        SignalPhysics.Circuit unknownWiring=new SignalPhysics.Circuit(
            "unknown-MQ",5.0,10.0,6.144,5.0,32768,1.0,false,true);
        status(SignalPhysics.fromKnownAdcVoltage(unknownWiring,null,1.5),
            SignalPhysics.Status.SENSOR_CIRCUIT_NOT_VERIFIED,
            "electrical configuration not verified on device");
        SignalPhysics.Circuit uncalibrated=new SignalPhysics.Circuit(
            "unknown-MQ",5.0,10.0,6.144,5.0,32768,1.0,true,false);
        status(SignalPhysics.fromKnownAdcVoltage(uncalibrated,null,1.5),
            SignalPhysics.Status.ADC_NOT_CALIBRATED,
            "uncalibrated converter cannot produce valid Rs");
        SignalPhysics.Observation nonzero=SignalPhysics.fromKnownAdcVoltage(
            verified,400,0.000001);
        status(nonzero,SignalPhysics.Status.VALID,
            "tiny physical positive Vout yields finite resistance, not artificial 0");
        check(nonzero.rsKohm>1_000_000,"low Vout high but finite Rs retained; source OOD separate");
        rejects(()->new SignalPhysics.Circuit("sensor",5,-10,6.144,5,32768,1,true,true),
            "negative RL rejected");
        rejects(()->new SignalPhysics.Circuit("sensor",5,10,Double.NaN,5,32768,1,true,true),
            "missing ADC range rejected");
        rejects(()->new SignalPhysics.Circuit("sensor",5,10,6.144,5,1,1,true,true),
            "ADC count overflow model rejected");
        rejects(()->SignalPhysics.fromAdcCode(null,150),"null circuit rejected");

        System.out.println("CHEMCORE_NEXT_P02_SIGNAL_PHYSICS_PASS assertions="+n
            +" sample_source=firmware/ESP32-Tank/Hardware.cpp"
            +" operational_hardware_validation=false"
            +" ppm=false g3=false");
    }
}
