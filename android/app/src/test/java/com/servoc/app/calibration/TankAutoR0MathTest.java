package com.servoc.app.calibration;

import org.junit.Test;
import static org.junit.Assert.*;

public final class TankAutoR0MathTest {
    @Test public void exactTimingIsTwentyMinutesWithTwoPoweredOnPauses(){
        assertEquals(-1,TankAutoR0Math.captureWindow(299999));
        assertEquals(0,TankAutoR0Math.captureWindow(300000));
        assertEquals(0,TankAutoR0Math.captureWindow(479999));
        assertEquals(-1,TankAutoR0Math.captureWindow(480000));
        assertEquals(1,TankAutoR0Math.captureWindow(660000));
        assertEquals(-1,TankAutoR0Math.captureWindow(840000));
        assertEquals(2,TankAutoR0Math.captureWindow(1020000));
        assertEquals(-1,TankAutoR0Math.captureWindow(1200000));
        assertEquals(1200000,TankAutoR0Math.TOTAL_MS);
    }
    @Test public void usesArithmeticAverageOfAllValidSamplesAcrossThreeWindows(){
        TankAutoR0Math m=new TankAutoR0Math();
        m.accept(0,0,100);m.accept(0,0,100);
        m.accept(0,1,200);m.accept(0,2,400);
        assertEquals(200.0,m.proposedR0(0),1e-7); // (100+100+200+400)/4
        assertTrue(Double.isNaN(m.proposedR0(1)));
        m.accept(1,0,0);m.accept(1,1,-2);m.accept(1,2,Double.NaN);
        assertEquals(0,m.validProposals().containsKey("mq3")?1:0);
    }
    @Test public void unevenWindowSampleCountsUsePooledArithmeticMean(){
        TankAutoR0Math m=new TankAutoR0Math();
        m.accept(0,0,100);
        m.accept(0,1,400);m.accept(0,1,500);
        m.accept(0,2,900);
        // Not geometric and not equal weighting of different-sized windows.
        assertEquals(475.0,m.proposedR0(0),1e-8);
        assertEquals(1,m.count(0,0));assertEquals(2,m.count(0,1));assertEquals(1,m.count(0,2));
    }
    @Test public void validChannelDoesNotRequireOtherSensors(){
        TankAutoR0Math m=new TankAutoR0Math();
        for(int w=0;w<3;w++)m.accept(7,w,12+w);
        assertEquals(1,m.validProposals().size());
        assertTrue(m.validProposals().containsKey("mq135"));
    }
}
