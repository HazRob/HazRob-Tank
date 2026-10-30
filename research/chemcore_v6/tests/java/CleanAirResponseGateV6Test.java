package com.hazrob.tank.chemcore.v6;

import java.util.Arrays;
import java.util.List;
import java.util.Collections;

/** Tests of source research-vs-operator display boundary, NOT G3. */
public final class CleanAirResponseGateV6Test {
    private static int checks;
    private static void require(boolean b,String s){
        checks++;if(!b)throw new AssertionError("gate "+checks+": "+s);
    }
    private static List<Engine.Channel> channels(double a,double b,double c){
        return Arrays.asList(new Engine.Channel("MQ2","MQ2-1",100*a,100),
            new Engine.Channel("MQ4","MQ4-1",90*b,90),
            new Engine.Channel("MQ135","MQ135-1",110*c,110));
    }
    public static void main(String[] args){
        var clean=CleanAirResponseGateV6.assess(channels(1,1,1));
        require(!clean.showGasHypotheses&&!clean.operatorAlarmAllowed,"exact air");
        require(clean.state.equals("WITHIN_CLEAN_AIR_REFERENCE_BAND"),"baseline state");
        require(clean.ratioReadyChannels==3,"readings are retained");
        for(double f:new double[]{0.98,0.99,1.0,1.01,1.02,1.08,0.92}){
            var d=CleanAirResponseGateV6.assess(channels(f,f,f));
            require(!d.showGasHypotheses,"small normal drift "+f);
            require(!d.operatorAlarmAllowed,"no alarm "+f);
        }
        var singles=CleanAirResponseGateV6.assess(channels(0.70,1,1));
        require(!singles.showGasHypotheses,"one sensor cannot assert chemical candidate");
        require(singles.state.equals("SINGLE_SENSOR_CHANGE_UNCONFIRMED"),"single sensor diagnosis");
        var two=CleanAirResponseGateV6.assess(channels(0.80,0.77,1));
        require(two.showGasHypotheses&&two.respondingSensorModels==2,"two robust decreases research screen");
        require(!two.operatorAlarmAllowed,"even two sensor response cannot issue gas safety alarm");
        var reverse=CleanAirResponseGateV6.assess(channels(1.35,1.30,1.20));
        require(!reverse.showGasHypotheses,"opposite direction not gas signal");
        require(reverse.state.equals("UNATTRIBUTED_DRIFT_OR_OPPOSITE_DIRECTION"),"drift state");
        var noR0=CleanAirResponseGateV6.assess(Collections.singletonList(
           new Engine.Channel("MQ2","NO-R0",100,Double.NaN)));
        require(!noR0.showGasHypotheses&&noR0.rawValidChannels==1,"retained uncalibrated reading");
        require(noR0.state.equals("NO_R0_FOR_RESPONSE_COMPARISON"),"no fake normalization");
        var none=CleanAirResponseGateV6.assess(Collections.emptyList());
        require(none.state.equals("NO_VALID_MQ_SIGNAL"),"no measurement");
        boolean invalid=false;
        try{CleanAirResponseGateV6.assess(channels(1,1,1),-0.05);}
        catch(IllegalArgumentException e){invalid=true;}
        require(invalid,"invalid profile blocked");
        System.out.println("PASS,CleanAirResponseGateV6Test,assertions="+checks);
    }
}
