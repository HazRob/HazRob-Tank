package com.hazrob.tank.chemcore.next;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * P02.3: independent single-sensor acquisition quality adjudication.
 * No arbitrary gas identity, safety alarm, or quantitative ppm claims.
 *
 * Timestamp and scan IDs come from actual firmware packets; a static/identical
 * ADC value is not itself proof of an old conversion. OS-ready and I2C status
 * are separate physical checks. Noise and drift are diagnostics only: genuine
 * exposure changes can also cause them.
 */
public final class SensorQualityGate {
    private SensorQualityGate() {}

    public enum Transport {
        ACKED, NACK, TIMEOUT, BUS_RECOVERING, NOT_REPORTED
    }
    public enum Status {
        ACCEPTED, LIMITED, REJECTED
    }
    public enum Flag {
        MISSING_SAMPLES, SENSOR_UID_MISMATCH, INVALID_RESISTANCE,
        I2C_NACK, I2C_TIMEOUT, I2C_BUS_RECOVERING, I2C_STATUS_UNKNOWN,
        ADS_CONVERSION_NOT_READY, ADS_READY_NOT_REPORTED,
        MISSING_SAMPLE_TIMESTAMP, FUTURE_SAMPLE_TIMESTAMP, STALE_SAMPLE,
        DUPLICATE_SCAN_CYCLE, INVALID_SCAN_CYCLE,
        HEATER_OFF, HEATER_WARMING, HEATER_PROFILE_MISMATCH,
        ENVIRONMENT_UNAVAILABLE, ENVIRONMENT_OUTSIDE_VALID_RANGE,
        REFERENCE_UNAVAILABLE, HIGH_NOISE, TEMPORAL_DRIFT,
        IDENTICAL_RAW_CODES_DIAGNOSTIC
    }

    public static final class Limits {
        public final long staleAfterMs, requiredWarmupMs;
        public final double maxNoiseCoefficientOfVariation, driftFractionForDiagnostic;
        public final int minSamplesForDiagnostics;
        public Limits(long staleAfterMs, long requiredWarmupMs,
                      double maxNoiseCoefficientOfVariation,
                      double driftFractionForDiagnostic, int minSamplesForDiagnostics) {
            if(staleAfterMs<=0||requiredWarmupMs<0||!Double.isFinite(maxNoiseCoefficientOfVariation)
                ||maxNoiseCoefficientOfVariation<=0||!Double.isFinite(driftFractionForDiagnostic)
                ||driftFractionForDiagnostic<=0||minSamplesForDiagnostics<2)
                throw new IllegalArgumentException("invalid research quality gate configuration");
            this.staleAfterMs=staleAfterMs;
            this.requiredWarmupMs=requiredWarmupMs;
            this.maxNoiseCoefficientOfVariation=maxNoiseCoefficientOfVariation;
            this.driftFractionForDiagnostic=driftFractionForDiagnostic;
            this.minSamplesForDiagnostics=minSamplesForDiagnostics;
        }
    }
    public static final class Sample {
        public final String sensorUid, heaterPhase;
        public final long receivedAtMs, devicePowerOnDurationMs, scanCycle;
        public final Double rsKohm, temperatureC, relativeHumidityPercent;
        public final Integer rawAdc;
        public final Transport transport;
        public final Boolean adcConversionReady, heaterPowered;
        public final boolean referenceAvailable;
        public Sample(String sensorUid, long receivedAtMs, long devicePowerOnDurationMs,
                      long scanCycle, Integer rawAdc, Double rsKohm, Transport transport,
                      Boolean adcConversionReady, Boolean heaterPowered, String heaterPhase,
                      Double temperatureC, Double relativeHumidityPercent,
                      boolean referenceAvailable) {
            if(sensorUid==null||sensorUid.trim().isEmpty())
                throw new IllegalArgumentException("sensor UID required");
            if(devicePowerOnDurationMs<0)
                throw new IllegalArgumentException("negative device power-on duration");
            this.sensorUid=sensorUid;this.receivedAtMs=receivedAtMs;
            this.devicePowerOnDurationMs=devicePowerOnDurationMs;
            this.scanCycle=scanCycle;this.rawAdc=rawAdc;this.rsKohm=rsKohm;
            this.transport=transport==null?Transport.NOT_REPORTED:transport;
            this.adcConversionReady=adcConversionReady;this.heaterPowered=heaterPowered;
            this.heaterPhase=heaterPhase==null?"":heaterPhase;
            this.temperatureC=temperatureC;this.relativeHumidityPercent=relativeHumidityPercent;
            this.referenceAvailable=referenceAvailable;
        }
    }
    public static final class Result {
        public final Status status;
        public final boolean physicalSignalEligible, environmentalModelEligible,
                             sourceCurveEligible;
        public final List<Flag> flags;
        public final Double noiseCv, relativeWindowDrift;
        public final int observedSamples;
        private Result(Status status, boolean rawEligible, boolean envEligible,
                       boolean curveEligible, EnumSet<Flag> flags,
                       Double cv, Double drift, int observedSamples) {
            this.status=status;this.physicalSignalEligible=rawEligible;
            this.environmentalModelEligible=envEligible;
            this.sourceCurveEligible=curveEligible;
            this.flags=Collections.unmodifiableList(new ArrayList<>(flags));
            this.noiseCv=cv;this.relativeWindowDrift=drift;
            this.observedSamples=observedSamples;
        }
        public boolean has(Flag flag) {return flags.contains(flag);}
    }

    public static Result evaluate(String physicalSensorUid, String expectedHeaterPhase,
                                  long nowMs, List<Sample> orderedSamples, Limits limits) {
        if(physicalSensorUid==null||physicalSensorUid.trim().isEmpty()||limits==null)
            throw new IllegalArgumentException("sensor identity and limits required");
        if(nowMs<0)throw new IllegalArgumentException("negative time");
        if(orderedSamples==null)throw new IllegalArgumentException("samples required");
        if(orderedSamples.size()>4096)throw new IllegalArgumentException("too many samples in one evaluation");
        EnumSet<Flag> flags=EnumSet.noneOf(Flag.class);
        if(orderedSamples.isEmpty())flags.add(Flag.MISSING_SAMPLES);
        boolean reject=orderedSamples.isEmpty(),limited=false,env=true,ref=true;
        long previousCycle=Long.MIN_VALUE,previousTimestamp=Long.MIN_VALUE;
        int repeated=0,previousRaw=Integer.MIN_VALUE;
        double sum=0,mean=0,m2=0,first=Double.NaN,last=Double.NaN;
        int validN=0;
        boolean seenDifferentRaw=false;
        for(Sample s:orderedSamples) {
            if(s==null)throw new IllegalArgumentException("null sample");
            if(!physicalSensorUid.equals(s.sensorUid)) {
                flags.add(Flag.SENSOR_UID_MISMATCH);reject=true;
            }
            if(s.rsKohm==null||!Double.isFinite(s.rsKohm)||s.rsKohm<=0
                    ||s.rawAdc==null||s.rawAdc<0||s.rawAdc>=32767) {
                flags.add(Flag.INVALID_RESISTANCE);reject=true;
            }
            switch(s.transport) {
                case NACK: flags.add(Flag.I2C_NACK);reject=true;break;
                case TIMEOUT: flags.add(Flag.I2C_TIMEOUT);reject=true;break;
                case BUS_RECOVERING: flags.add(Flag.I2C_BUS_RECOVERING);reject=true;break;
                case NOT_REPORTED: flags.add(Flag.I2C_STATUS_UNKNOWN);limited=true;break;
                default:break;
            }
            if(Boolean.FALSE.equals(s.adcConversionReady)) {
                flags.add(Flag.ADS_CONVERSION_NOT_READY);reject=true;
            } else if(s.adcConversionReady==null) {
                flags.add(Flag.ADS_READY_NOT_REPORTED);limited=true;
            }
            if(s.receivedAtMs<0) {
                flags.add(Flag.MISSING_SAMPLE_TIMESTAMP);reject=true;
            } else if(s.receivedAtMs>nowMs) {
                flags.add(Flag.FUTURE_SAMPLE_TIMESTAMP);reject=true;
            } else if(nowMs-s.receivedAtMs>limits.staleAfterMs) {
                flags.add(Flag.STALE_SAMPLE);reject=true;
            }
            if(previousTimestamp!=Long.MIN_VALUE&&s.receivedAtMs<previousTimestamp) {
                flags.add(Flag.STALE_SAMPLE);reject=true;
            }
            previousTimestamp=s.receivedAtMs;
            if(s.scanCycle<=0) {
                flags.add(Flag.INVALID_SCAN_CYCLE);reject=true;
            } else if(previousCycle!=Long.MIN_VALUE&&s.scanCycle<=previousCycle) {
                flags.add(Flag.DUPLICATE_SCAN_CYCLE);reject=true;
            }
            previousCycle=s.scanCycle;
            if(Boolean.FALSE.equals(s.heaterPowered)) {
                flags.add(Flag.HEATER_OFF);reject=true;
            } else if(s.heaterPowered==null) {
                flags.add(Flag.HEATER_WARMING);limited=true;
            }
            if(s.devicePowerOnDurationMs<limits.requiredWarmupMs) {
                flags.add(Flag.HEATER_WARMING);reject=true;
            }
            if(expectedHeaterPhase!=null&&!expectedHeaterPhase.isEmpty()
                &&!expectedHeaterPhase.equals(s.heaterPhase)) {
                flags.add(Flag.HEATER_PROFILE_MISMATCH);reject=true;
            }
            if(s.temperatureC==null||s.relativeHumidityPercent==null
                ||!Double.isFinite(s.temperatureC)||!Double.isFinite(s.relativeHumidityPercent)) {
                flags.add(Flag.ENVIRONMENT_UNAVAILABLE);env=false;
            } else if(s.relativeHumidityPercent<0||s.relativeHumidityPercent>100) {
                flags.add(Flag.ENVIRONMENT_OUTSIDE_VALID_RANGE);env=false;
            }
            if(!s.referenceAvailable){flags.add(Flag.REFERENCE_UNAVAILABLE);ref=false;}
            if(s.rawAdc!=null) {
                if(previousRaw!=Integer.MIN_VALUE) {
                    if(s.rawAdc==previousRaw)repeated++;
                    else seenDifferentRaw=true;
                }
                previousRaw=s.rawAdc;
            }
            if(s.rsKohm!=null&&Double.isFinite(s.rsKohm)&&s.rsKohm>0) {
                if(validN==0)first=s.rsKohm;
                last=s.rsKohm;
                validN++;
                double d=s.rsKohm-mean;mean+=d/validN;m2+=d*(s.rsKohm-mean);
                sum+=s.rsKohm;
            }
        }
        Double cv=null,drift=null;
        if(validN>=limits.minSamplesForDiagnostics && mean>0) {
            cv=Math.sqrt(Math.max(0,m2/validN))/mean;
            drift=Math.abs(last-first)/first;
            if(cv>limits.maxNoiseCoefficientOfVariation) {
                flags.add(Flag.HIGH_NOISE);limited=true;
            }
            if(drift>limits.driftFractionForDiagnostic) {
                flags.add(Flag.TEMPORAL_DRIFT);limited=true;
            }
        }
        if(orderedSamples.size()>=limits.minSamplesForDiagnostics
            &&repeated>=orderedSamples.size()-1&&!seenDifferentRaw) {
            // Constant input is possible; a repeated bit pattern alone is NOT stale evidence.
            flags.add(Flag.IDENTICAL_RAW_CODES_DIAGNOSTIC);limited=true;
        }
        boolean physical=!reject;
        boolean envEligible=physical&&env;
        boolean sourceEligible=physical&&ref
            &&!flags.contains(Flag.I2C_STATUS_UNKNOWN)
            &&!flags.contains(Flag.ADS_READY_NOT_REPORTED)
            &&!flags.contains(Flag.HEATER_WARMING)
            &&!flags.contains(Flag.HIGH_NOISE)
            &&!flags.contains(Flag.TEMPORAL_DRIFT);
        Status status=reject?Status.REJECTED:(limited||!env||!ref?Status.LIMITED:Status.ACCEPTED);
        return new Result(status,physical,envEligible,sourceEligible,flags,cv,drift,orderedSamples.size());
    }
}
