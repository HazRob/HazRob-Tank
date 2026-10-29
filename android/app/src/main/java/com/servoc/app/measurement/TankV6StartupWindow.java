package com.servoc.app.measurement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Phone-side collector for already-published Tank MQ telemetry.
 * Does not issue any robot requests, switch heater relays, change the R0
 * calibration or evaluate gas identity/ppm. Pure Java for independent JDK tests.
 *
 * Only scan-cycle-tagged sensor observations are used, one observation/channel/
 * >=800 ms, bounded to the first 60 s of a relay-on episode. The actual rate
 * is bounded by whatever snapshots the existing Android poll obtains.
 */
public final class TankV6StartupWindow {
    public static final long WINDOW_MS=60000;
    public static final long SENSOR_MIN_SPACING_MS=800;
    public static final long MAX_ACCEPTED_SENSOR_AGE_US=1500000;
    public static final long MAX_RESTART_SKEW_MS=2000;
    private static final String[] ALLOWED={"mq2","mq3","mq4","mq5","mq7","mq8","mq9","mq135"};

    public static final class Reading {
        public final String sensorKey;
        public final long scanCycle, sensorAgeUs, sensorSampleUs;
        public final double resistance;
        public final boolean valid;
        public Reading(String sensor,long cycle,long ageUs,long sampleUs,double rs,boolean ok) {
            sensorKey=sensor==null?"":sensor.toLowerCase(java.util.Locale.ROOT);
            scanCycle=cycle;sensorAgeUs=ageUs;sensorSampleUs=sampleUs;
            resistance=rs;valid=ok;
        }
    }
    public static final class Row {
        public final long eventSequence,relayPowerMs,sampleSinceRelayMs,scanCycle,sampleUs;
        public final String sensorKey;
        public final double resistance;
        public final boolean captureBeginsNearPowerOn;
        Row(long event,long elapsed,long sensorElapsed,Reading r,boolean near) {
            eventSequence=event;relayPowerMs=elapsed;sampleSinceRelayMs=sensorElapsed;
            scanCycle=r.scanCycle;sampleUs=r.sensorSampleUs;sensorKey=r.sensorKey;
            resistance=r.resistance;captureBeginsNearPowerOn=near;
        }
    }
    public static final class Batch {
        public final long eventSequence;
        public final boolean newlyPoweredOn,completed,startedNearPowerOn;
        public final List<Row> rows;
        Batch(long sequence,boolean newEvent,boolean done,boolean near,List<Row> result) {
            eventSequence=sequence;newlyPoweredOn=newEvent;completed=done;
            startedNearPowerOn=near;
            rows=Collections.unmodifiableList(result);
        }
    }
    private long sequence=0, lastPowerMs=-1;
    private boolean powered=false,nearStart=false,completed=false;
    private final Map<String,Long> lastSampleTime=new HashMap<>(),lastCycle=new HashMap<>();

    public synchronized Batch accept(boolean relayOn,long powerMs,List<Reading> readings){
        boolean began=false;
        if(!relayOn || powerMs<0) {
            boolean ended=powered && !completed;
            powered=false;completed=false;lastPowerMs=-1;
            lastSampleTime.clear();lastCycle.clear();
            return new Batch(sequence,false,ended,false,Collections.<Row>emptyList());
        }
        boolean restarted=!powered || (lastPowerMs>=0 && powerMs+MAX_RESTART_SKEW_MS<lastPowerMs);
        if(restarted){
            sequence++;powered=true;completed=false;nearStart=powerMs<=2000;
            lastSampleTime.clear();lastCycle.clear();began=true;
        }
        lastPowerMs=powerMs;
        if(completed || powerMs>WINDOW_MS){
            boolean justFinished=!completed;completed=true;
            return new Batch(sequence,began,justFinished,nearStart,Collections.<Row>emptyList());
        }
        List<Row> rows=new ArrayList<>();
        if(readings!=null)for(Reading r:readings) {
            if(r==null || !validSensor(r.sensorKey) || !r.valid ||
                    r.scanCycle<=0 || r.sensorSampleUs<=0 ||
                    r.sensorAgeUs<0 || r.sensorAgeUs>MAX_ACCEPTED_SENSOR_AGE_US ||
                    !Double.isFinite(r.resistance) || r.resistance<=0)continue;
            long sampleMs=powerMs-r.sensorAgeUs/1000;
            if(sampleMs<0 || sampleMs>WINDOW_MS)continue;
            Long priorCycle=lastCycle.get(r.sensorKey);
            if(priorCycle!=null && priorCycle==r.scanCycle)continue;
            Long priorMs=lastSampleTime.get(r.sensorKey);
            if(priorMs!=null && sampleMs-priorMs<SENSOR_MIN_SPACING_MS)continue;
            lastCycle.put(r.sensorKey,r.scanCycle);
            lastSampleTime.put(r.sensorKey,sampleMs);
            rows.add(new Row(sequence,powerMs,sampleMs,r,nearStart));
        }
        return new Batch(sequence,began,false,nearStart,rows);
    }

    private static boolean validSensor(String key) {
        for(String k:ALLOWED)if(k.equals(key))return true;
        return false;
    }
    public synchronized long currentSequence(){return sequence;}
}
