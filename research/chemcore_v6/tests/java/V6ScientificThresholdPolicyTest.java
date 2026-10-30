package com.hazrob.tank.chemcore.v6;

/** Reproducible arithmetic policy checks, not proof of chemical measurement. */
public final class V6ScientificThresholdPolicyTest {
    static int asserts=0;
    static void check(boolean b,String why){asserts++;if(!b)throw new AssertionError(why);}
    static void close(double a,double b,String why){check(Math.abs(a-b)<1e-7,why+": "+a);}
    public static void main(String[] args){
        V6ScientificThresholdPolicy.Decision co2Below=
            V6ScientificThresholdPolicy.compare(4400,5000,"TWA",0.9);
        check(co2Below.state.equals("BELOW_ADVISORY"),"no threshold below 90%");
        V6ScientificThresholdPolicy.Decision co2Early=
            V6ScientificThresholdPolicy.compare(4500,5000,"TWA",0.9);
        check(co2Early.state.equals("EARLY_WARNING"),"90% PEL preco warning");
        close(co2Early.thresholdPpm,4500,"early OSHA CO2 example");
        V6ScientificThresholdPolicy.Decision co2At=
            V6ScientificThresholdPolicy.compare(5000,5000,"TWA",0.9);
        check(co2At.state.equals("AT_OR_ABOVE_REFERENCE"),"100% potential site hazard");
        check(co2At.sitePotentialHazard,"site flag");
        check(!co2At.regulatoryTwaExceeded,"instantaneous is not 8-hour TWA");
        check(!co2At.certifiedGasAlarm,"no G3 alarm");
        V6ScientificThresholdPolicy.Decision stel=
            V6ScientificThresholdPolicy.compare(27000,30000,"STEL",0.9);
        check(stel.state.equals("EARLY_WARNING"),"15 min STEL early");
        close(stel.thresholdPpm,27000,"NIOSH STEL 90%");
        V6ScientificThresholdPolicy.Decision flammable=
            V6ScientificThresholdPolicy.compare(1000,10000,"LFL",0.9);
        check(flammable.state.equals("LFL_PRECAUTIONARY"),"10 percent LFL is independent");
        close(flammable.thresholdPpm,1000,"10 percent LFL ppm");
        check(!flammable.certifiedGasAlarm,"even 10% LFL not certified");
        V6ScientificThresholdPolicy.Decision max=
            V6ScientificThresholdPolicy.compare(10000,10000,"LFL",0.9);
        check(max.state.equals("AT_OR_ABOVE_LFL"),"reached LFL");
        boolean bad=false;
        try{V6ScientificThresholdPolicy.compare(100,5000,"TWA",1.1);}
        catch(IllegalArgumentException e){bad=true;}
        check(bad,"bad 110% early setting rejected");
        bad=false;
        try{V6ScientificThresholdPolicy.compare(Double.NaN,5000,"TWA",0.9);}
        catch(IllegalArgumentException e){bad=true;}
        check(bad,"invalid ppm rejected");
        System.out.println("PASS V6ScientificThresholdPolicyTest "+asserts+" checks, G3 alarm FALSE");
    }
}
