package com.hazrob.tank.chemcore.v6;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Operator-facing baseline-response gate, independent of research math.
 * This is NOT a toxic gas threshold and is NOT a calibrated safety alarm.
 *
 * With predominantly reducing-gas negative-slope MQ curves, an increase in
 * normalized conductance manifests as a fall of Rs/R0. Rs/R0 near unity means
 * same-air/reference-equivalent, NOT a positively identified clean atmosphere.
 * Larger upward Rs changes are classified as unexplained drift/direction,
 * not a confirmed gas exposure.
 *
 * DEFAULT_DROP=10% is an explicitly provisional engineering deadband used
 * only to avoid obvious baseline false positives in demonstration tests.
 * Before deployment replace with per-sensor repeatability/noise/T-RH/phase
 * baselines, performance verification and validated gas-specific thresholds.
 * No code here authorizes an exposure alarm.
 */
public final class CleanAirResponseGateV6 {
    public static final String SCHEMA="hazrob-v6-clean-air-response-gate/1";
    public static final double DEFAULT_DROP_FRACTION=0.10;
    public static final int MIN_DISTINCT_SENSOR_MODELS=2;
    private CleanAirResponseGateV6(){}

    public static final class Decision {
        public final String state;
        public final int rawValidChannels,ratioReadyChannels;
        public final int respondingSensorModels,risingOrOppositeModels;
        public final double thresholdDropFraction;
        public final boolean showGasHypotheses,operatorAlarmAllowed;
        public final List<String> responseModels;
        private Decision(String state,int raw,int ratio,int responders,int opposite,
                         double threshold,Set<String> models){
            this.state=state;rawValidChannels=raw;ratioReadyChannels=ratio;
            respondingSensorModels=responders;risingOrOppositeModels=opposite;
            thresholdDropFraction=threshold;
            showGasHypotheses=responders>=MIN_DISTINCT_SENSOR_MODELS;
            // G3 or independent verified gas concentration thresholds are
            // NOT present. Even a large Rs change cannot authorize alerts.
            operatorAlarmAllowed=false;
            responseModels=Collections.unmodifiableList(new ArrayList<>(models));
        }
    }

    public static Decision assess(List<Engine.Channel> input){
        return assess(input,DEFAULT_DROP_FRACTION);
    }
    public static Decision assess(List<Engine.Channel> input,double dropFraction){
        if(!Double.isFinite(dropFraction)||dropFraction<=0||dropFraction>=1)
            throw new IllegalArgumentException("Positive bounded response fraction required");
        Set<String> seenRaw=new LinkedHashSet<>(),seenRatio=new LinkedHashSet<>();
        Set<String> positive=new LinkedHashSet<>(),opposite=new LinkedHashSet<>();
        if(input!=null)for(Engine.Channel c:input){
            if(c==null||!c.rawResistanceValid()||c.sensorType.isEmpty())continue;
            String unique=c.sensorInstance+"::"+c.sensorType;
            seenRaw.add(unique);
            if(!c.normalizedRatioAvailable())continue;
            seenRatio.add(unique);
            double ratio=c.rs/c.r0;
            if(ratio<=1.0-dropFraction)positive.add(c.sensorType);
            if(ratio>=1.0+dropFraction)opposite.add(c.sensorType);
        }
        String state;
        if(seenRaw.isEmpty())state="NO_VALID_MQ_SIGNAL";
        else if(seenRatio.isEmpty())state="NO_R0_FOR_RESPONSE_COMPARISON";
        else if(positive.size()>=MIN_DISTINCT_SENSOR_MODELS)
            state="PROVISIONAL_RESPONSE_RESEARCH_ONLY";
        else if(!opposite.isEmpty())state="UNATTRIBUTED_DRIFT_OR_OPPOSITE_DIRECTION";
        else if(!positive.isEmpty())state="SINGLE_SENSOR_CHANGE_UNCONFIRMED";
        else state="WITHIN_CLEAN_AIR_REFERENCE_BAND";
        return new Decision(state,seenRaw.size(),seenRatio.size(),
            positive.size(),opposite.size(),dropFraction,positive);
    }
}
