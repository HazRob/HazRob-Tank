package com.servoc.app.measurement;

import java.util.Arrays;
import java.util.Collections;

public final class TankV6StartupWindowTest {
    private static int count=0;
    private static void ok(boolean b,String msg){count++;if(!b)throw new AssertionError("test="+count+": "+msg);}
    private static TankV6StartupWindow.Reading r(String sensor,long cycle,long age,double rs) {
        return new TankV6StartupWindow.Reading(sensor,cycle,age,5000000+cycle*100000,rs,true);
    }
    private static TankV6StartupWindow.Batch a(TankV6StartupWindow w,long age,long cycle) {
        return w.accept(true,age,Arrays.asList(r("MQ2",cycle,20000,20000),r("mq135",cycle,30000,35000)));
    }
    public static void main(String[] args) {
        TankV6StartupWindow w=new TankV6StartupWindow();
        var initial=a(w,200,1);
        ok(initial.newlyPoweredOn,"relay power-on event");
        ok(initial.startedNearPowerOn,"starts within two seconds");
        ok(initial.eventSequence==1,"first event sequence");
        ok(initial.rows.size()==2,"two independent physical channels");
        ok(initial.rows.get(0).sampleSinceRelayMs==180,"sensor-age corrected time");
        ok(initial.rows.get(0).eventSequence==1,"event linked to record");
        ok(a(w,400,1).rows.isEmpty(),"same scan cycle not recorded twice");
        ok(a(w,500,2).rows.isEmpty(),"high frequency deduplicated per phone throttle");
        ok(a(w,1100,3).rows.size()==2,"one observation per sensor after at least 800 ms");
        ok(w.accept(true,1500,Arrays.asList(r("unknown",4,0,30))).rows.isEmpty(),"only known MQ channels");
        ok(w.accept(true,1800,Arrays.asList(
                new TankV6StartupWindow.Reading("mq2",5,0,5000000,Double.NaN,true)
        )).rows.isEmpty(),"invalid resistance rejected");
        ok(w.accept(true,1900,Arrays.asList(
                new TankV6StartupWindow.Reading("mq2",5,2000000,5000000,10000,true)
        )).rows.isEmpty(),"stale sensor not recorded");
        var end=w.accept(true,60001,Collections.emptyList());
        ok(end.completed,"completed at 60 s");
        ok(w.accept(true,61000,Arrays.asList(r("mq2",999,100,100))).rows.isEmpty(),
                "no long-term sensor collection");
        var off=w.accept(false,0,Collections.emptyList());
        ok(!off.newlyPoweredOn,"off does not create event");
        var second=a(w,100,1);
        ok(second.eventSequence==2,"new relay cycle");
        ok(second.rows.size()==2,"scan cycles can restart after relay");
        var late=new TankV6StartupWindow();
        var lateBatch=a(late,20000,1);
        ok(!lateBatch.startedNearPowerOn,"late app attach flagged incomplete");
        ok(lateBatch.rows.size()==2,"late read may be archived as partial");
        var reset=new TankV6StartupWindow();
        a(reset,3000,1);
        var resetEvent=a(reset,50,2);
        ok(resetEvent.eventSequence==2,"power_ms rollback starts new relay session");
        ok(resetEvent.newlyPoweredOn,"rollback start is observable event");
        var empty=new TankV6StartupWindow();
        ok(empty.accept(false,0,Collections.emptyList()).rows.isEmpty(),
                "no power means no samples");
        var bad=new TankV6StartupWindow();
        ok(bad.accept(true,-1,Arrays.asList(r("mq2",1,0,20))).rows.isEmpty(),
                "invalid relay power milliseconds rejected");
        ok(TankV6StartupWindow.WINDOW_MS==60000,"strict first minute only");
        ok(TankV6StartupWindow.SENSOR_MIN_SPACING_MS>=800,"phone conservative sampling");
        System.out.println("PASS TankV6StartupWindow: "+count+" assertions; no network or ESP32 changes");
    }
}
