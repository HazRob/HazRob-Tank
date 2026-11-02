package com.hazrob.tank.chemcore.next;

/**
 * P02.1: electrically constrained Vout -> Rs conversion for real MQ divider,
 * adapted after review of the pinned Tank Hardware.cpp and MQSensorsLib.
 *
 * Tank source assumption: Rs = RL * ((MQ_VCC - Vout) / Vout);
 * ADS1115 bipolar PGA +/-6.144 V represents 32768 positive LSBs. The
 * effective Vout is NOT trustworthy until physical ADS VDD, channel wiring,
 * board attenuation and divider parameters are verified.
 *
 * This is resistance physics, not an identity/ppm or alarm algorithm.
 */
public final class SignalPhysics {
    private SignalPhysics() {}

    public enum Status {
        VALID,
        NO_SAMPLE,
        ADC_NOT_CALIBRATED,
        SENSOR_CIRCUIT_NOT_VERIFIED,
        ADC_INVALID,
        ADC_SATURATED,
        VOLTAGE_NOT_FINITE,
        VOLTAGE_OUTSIDE_DIVIDER,
        VOLTAGE_ABOVE_ADC_SUPPLY,
        RESISTANCE_INVALID
    }

    public static final class Circuit {
        public final String sensorUid;
        public final double vSensorSupply, loadResistorKohm, adcFullScaleV,
                            adcSupplyV, signalVoltageGain;
        public final int adcCountsForFullScale;
        public final boolean physicalWiringVerified, adcVoltageVerified;
        public Circuit(String sensorUid, double vSensorSupply, double loadResistorKohm,
                       double adcFullScaleV, double adcSupplyV, int adcCountsForFullScale,
                       double signalVoltageGain,
                       boolean physicalWiringVerified, boolean adcVoltageVerified) {
            if (sensorUid == null || sensorUid.trim().isEmpty())
                throw new IllegalArgumentException("sensor instance UID required");
            for(double v:new double[]{vSensorSupply,loadResistorKohm,adcFullScaleV,
                                      adcSupplyV,signalVoltageGain})
                if(!Double.isFinite(v)||v<=0)
                    throw new IllegalArgumentException("nonpositive or nonfinite circuit parameter");
            if(adcCountsForFullScale<2||adcCountsForFullScale>2147483646)
                throw new IllegalArgumentException("invalid ADC full-scale counts");
            this.sensorUid=sensorUid;
            this.vSensorSupply=vSensorSupply;
            this.loadResistorKohm=loadResistorKohm;
            this.adcFullScaleV=adcFullScaleV;
            this.adcSupplyV=adcSupplyV;
            this.adcCountsForFullScale=adcCountsForFullScale;
            this.signalVoltageGain=signalVoltageGain;
            this.physicalWiringVerified=physicalWiringVerified;
            this.adcVoltageVerified=adcVoltageVerified;
        }
    }

    public static final class Observation {
        public final Status status;
        public final String sensorUid, reason;
        public final Integer rawAdc;
        public final Double measuredAdcVoltageV, sensorOutputVoltageV, rsKohm;
        private Observation(Circuit c, Status status, String reason, Integer raw,
                            Double adcVoltage, Double output, Double rs) {
            this.sensorUid=c.sensorUid;
            this.status=status;
            this.reason=reason;
            this.rawAdc=raw;
            this.measuredAdcVoltageV=adcVoltage;
            this.sensorOutputVoltageV=output;
            this.rsKohm=rs;
        }
        public boolean usableForAnalysis() {return status==Status.VALID && rsKohm!=null;}
    }
    private static Observation invalid(Circuit c, Status status, String reason,
                                       Integer raw, Double adcV, Double outputV) {
        return new Observation(c,status,reason,raw,adcV,outputV,null);
    }

    /** ADS1115 unsigned positive codes 0..32767, bipolar PGA LSB=FS/32768. */
    public static Observation fromAdcCode(Circuit c, Integer raw) {
        if(c==null)throw new IllegalArgumentException("circuit required");
        if(raw==null)return invalid(c,Status.NO_SAMPLE,"missing ADS sample",null,null,null);
        if(raw<0||raw>c.adcCountsForFullScale)
            return invalid(c,Status.ADC_INVALID,"ADS code outside unsigned positive range",raw,null,null);
        if(raw>=c.adcCountsForFullScale-1)
            return invalid(c,Status.ADC_SATURATED,"ADC positive full-scale code saturated",raw,null,null);
        double adcV=raw*(c.adcFullScaleV/c.adcCountsForFullScale);
        if(!c.adcVoltageVerified)
            return invalid(c,Status.ADC_NOT_CALIBRATED,
                "ADC reference/gain/supply has not been verified on the built board",
                raw,adcV,null);
        if(!c.physicalWiringVerified)
            return invalid(c,Status.SENSOR_CIRCUIT_NOT_VERIFIED,
                "physical MQ Vcc/RL/voltage divider/wiring not verified",raw,adcV,null);
        return fromKnownAdcVoltage(c,raw,adcV);
    }

    /**
     * The Vout voltage has been physically observed (or read by a qualified
     * ADC). This avoids the inaccurate shortcut Vout = raw*3.3/4095 for ESP32.
     */
    public static Observation fromKnownAdcVoltage(Circuit c,Integer raw,Double observedAdcV) {
        if(c==null)throw new IllegalArgumentException("circuit required");
        if(raw!=null && (raw<0 || raw>c.adcCountsForFullScale))
            return invalid(c,Status.ADC_INVALID,"invalid raw ADC code paired with voltage",raw,observedAdcV,null);
        if(raw!=null && raw>=c.adcCountsForFullScale-1)
            return invalid(c,Status.ADC_SATURATED,"saturated raw ADC code paired with voltage",raw,observedAdcV,null);
        if(observedAdcV==null)return invalid(c,Status.NO_SAMPLE,"no measured Vout",raw,null,null);
        if(!Double.isFinite(observedAdcV))
            return invalid(c,Status.VOLTAGE_NOT_FINITE,"Vout is NaN/infinite",raw,null,null);
        if(!c.adcVoltageVerified)
            return invalid(c,Status.ADC_NOT_CALIBRATED,"ADC voltage was not reference-verified",
                raw,observedAdcV,null);
        if(!c.physicalWiringVerified)
            return invalid(c,Status.SENSOR_CIRCUIT_NOT_VERIFIED,"MQ circuit not verified",
                raw,observedAdcV,null);
        if(observedAdcV<0||observedAdcV>=c.adcSupplyV)
            return invalid(c,Status.VOLTAGE_ABOVE_ADC_SUPPLY,
                "ADS input violates 0<=Vadc<ADC-VDD",raw,observedAdcV,null);
        double output=observedAdcV*c.signalVoltageGain;
        if(!Double.isFinite(output)||output<=0||output>=c.vSensorSupply)
            return invalid(c,Status.VOLTAGE_OUTSIDE_DIVIDER,
                "MQ divider requires 0<Vout<sensor Vcc",raw,observedAdcV,output);
        double rs=c.loadResistorKohm*(c.vSensorSupply/output-1.0);
        if(!Double.isFinite(rs)||rs<=0)
            return invalid(c,Status.RESISTANCE_INVALID,
                "computed Rs is not a finite positive resistance",raw,observedAdcV,output);
        return new Observation(c,Status.VALID,"resistance only; no gas identity or ppm",raw,
                               observedAdcV,output,rs);
    }

    /**
     * Firmware's declared nominal constants: ADS FSR=6.144V, supply=5V,
     * analog MQ supply=5V, RL=10 kOhm. FOR AUDIT ONLY: not field verified.
     * Operational readings must not use this profile as physically qualified.
     */
    public static Circuit tankV6DeclaredNominal(String sensorUid) {
        return new Circuit(sensorUid,5.0,10.0,6.144,5.0,32768,1.0,false,false);
    }
}
