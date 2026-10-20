package com.servoc.app.calibration;

import java.util.LinkedHashMap;
import java.util.Map;

/** Exact 3 × 3min user clean-air sampling scheme. No drift-gating or automatic extensions. */
public final class TankAutoR0Math {
    public static final long TOTAL_MS=20L*60_000L;
    public static final int SENSOR_COUNT=8,WINDOWS=3;
    private static final String[] KEYS={"mq2","mq3","mq4","mq5","mq7","mq8","mq9","mq135"};
    private final double[][] sum=new double[SENSOR_COUNT][WINDOWS];
    private final int[][] counts=new int[SENSOR_COUNT][WINDOWS];

    /** 5-minute heat, then 3-minute capture / 3-minute powered-on rest / repeat. */
    public static int captureWindow(long elapsedMs) {
        if(elapsedMs>=300_000L&&elapsedMs<480_000L)return 0;
        if(elapsedMs>=660_000L&&elapsedMs<840_000L)return 1;
        if(elapsedMs>=1_020_000L&&elapsedMs<1_200_000L)return 2;
        return -1;
    }

    public static String sensorKey(int sensorIndex){return KEYS[sensorIndex];}

    public void accept(int sensor,int window,double rsKilohm){
        if(sensor<0||sensor>=SENSOR_COUNT||window<0||window>=WINDOWS
            ||!Double.isFinite(rsKilohm)||rsKilohm<=0)return;
        sum[sensor][window]+=rsKilohm;
        counts[sensor][window]++;
    }
    public int count(int sensor,int window){return counts[sensor][window];}
    /** Arithmetic average of every valid Rs reading over all three on-powered capture windows. */
    public double proposedR0(int sensor){
        if(sensor<0||sensor>=SENSOR_COUNT)return Double.NaN;
        double total=0.0;
        long sampleCount=0;
        for(int w=0;w<WINDOWS;w++){
            if(counts[sensor][w]==0)return Double.NaN;
            total+=sum[sensor][w];
            sampleCount+=counts[sensor][w];
        }
        double r=total/sampleCount;
        return Double.isFinite(r)&&r>0?r:Double.NaN;
    }
    public Map<String,Double> validProposals(){
        Map<String,Double> result=new LinkedHashMap<>();
        for(int i=0;i<SENSOR_COUNT;i++){
            double r=proposedR0(i);
            if(Double.isFinite(r)&&r>0)result.put(KEYS[i],r);
        }
        return result;
    }
}
